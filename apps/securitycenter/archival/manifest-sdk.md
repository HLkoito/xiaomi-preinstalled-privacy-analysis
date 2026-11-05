> **历史分析存档，非最终结论。** 本文保留原始分析的技术内容，可能包含尚未验证或已被纠正的表述。隐私数据上传范围、同意条件、FBO 本地委托、报警短信加密、Provider 鉴权等，以 [最终报告](../REPORT.md) 与 [独立复核](../REVIEW.md) 为准。文中源码定位保留为原反编译相对路径；只有 evidence/ 中列出的精选文件随包提供，未打包完整源码树。

# MIUI 安全中心 — Manifest / 权限 / 组件暴露面 / 签名 / 第三方 SDK / 代码结构

> 目标：`MIUISecurityCenter-10.4.9-250402.0.1.apk`
> SHA-256 `fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd`，66,917,411 B，ZIP 条目 9231
> 责任范围：清单、权限、组件暴露面、签名、SDK 清单、代码结构与混淆、多进程。**不含**业务算法（他人负责）。
> 标注纪律：**【观测】**=shipped artifact 直接事实；**【推断】**=基于观测的合理推论；**【未知】**=证据不足。

---

## 0. 基线纠错（必须先看）

`_BASELINE.md` 里这几行指向**另一个 APK 的产物**，不能当本目标的证据用：

| 基线说法 | 实际情况 |
|---|---|
| `_analysis\apktool_sc\` = apktool 解码（含 AndroidManifest.xml） | **错**。`apktool_sc\` 只有 `res\` + 3 个 dex，没有 manifest |
| 隐含可用 `_analysis\apktool\` / `_analysis\src\` / `_analysis\report\` | **全是 `com.miui.guardprovider` 2.0.3-20231124.0 的产物**（apktool.yml: `apkFileName: GuardProvider-2.0.3-20231124.0.apk`，package `com.miui.guardprovider`，sharedUserId 无，3242 类） |
| `_analysis\avlsdk_extract\` | 同样来自 GuardProvider（`assets/avlsdk`），**本 APK 的 assets 里没有 avlsdk** |
| `_analysis\dex_strings_all.txt` | 来自 GuardProvider 的 dex（里面有 `Ltmsdk/...`、`com/miui/guardprovider/engine/tencent/TencentEngine`），**不是本 APK** |

**本报告使用的可复现证据源**（均已生成，可复核）：

| 文件 | 说明 |
|---|---|
| `_analysis\tools\axml2xml.js` | 自写 AXML 解码器（Node，无第三方依赖），把二进制 `apk_full\AndroidManifest.xml` 解成文本 |
| `_analysis\apk_full\AndroidManifest.decoded.xml` | **解码后的完整 manifest 文本，5137 行** —— 本报告所有 `L###` 行号都指这个文件 |
| `_analysis\tools\manifest.events.json` | 每个元素的 AXML 事件 + 解码后行号 |
| `_analysis\tools\manifest_out\manifest.structured.json` | 结构化清单（组件/权限/meta-data 全量） |
| `_analysis\tools\classes.classlist.json` | 从 3 个 dex 的 `class_defs` 直接枚举的 **25009 个类**（非 jadx 统计） |
| `_analysis\tools\{dex_classes,pkg_hist,top_roots,perm_classify,gen_tables2}.js` | 统计脚本 |

复现命令：
```powershell
node _analysis\tools\axml2xml.js _analysis\apk_full\AndroidManifest.xml _analysis\apk_full\AndroidManifest.decoded.xml _analysis\tools\manifest.events.json
node _analysis\tools\manifest_analyze.js _analysis\tools\manifest.events.json _analysis\tools\manifest_out
node _analysis\tools\dex_classes.js _analysis\apk_full\classes.dex _analysis\apk_full\classes2.dex _analysis\apk_full\classes3.dex
```

---

## 1. 应用身份与 application 级属性

### 1.1 `<manifest>`（decoded.xml L2–L10）

| 属性 | 值 | 判定 |
|---|---|---|
| `package` | `com.miui.securitycenter` | 【观测】 |
| `android:versionCode` | `40001049` | 【观测】 |
| `android:versionName` | `10.4.9-250402.0.1` | 【观测】 |
| **`android:sharedUserId`** | **`android.uid.system`** | 【观测】**全篇最关键一条** |
| `android:compileSdkVersion` / `Codename` | `34` / `14` | 【观测】 |
| `platformBuildVersionCode` / `Name` | `34` / `14` | 【观测】 |
| `android:targetSandboxVersion` | **不存在** | 【观测】→ 取默认值 1（非 2，即未启用 Android 10 的强沙箱隔离） |

`<uses-sdk>`（L11）：`minSdkVersion=23`、`targetSdkVersion=34`。【观测】

> **`sharedUserId="android.uid.system"` 的含义**【推断，基于 AOSP 语义】：本 APK 与 `system_server`、`com.android.systemui`、`com.android.settings` 等共享 **UID 1000**。凡是以 system UID 身份执行到的代码路径，都直接拥有 system 的 SELinux 域与全部 `signature|privileged` 权限；`android:permission` 只能拦"发起方"，拦不住"本进程被利用后自身拥有的能力"。这是本 APK 所有 exported 组件风险的**放大器**。

### 1.2 `<application>`（decoded.xml L469–L481）

| 属性 | 值 | 判定 |
|---|---|---|
| `android:name` | `com.miui.securitycenter.Application` | 【观测】 |
| `android:label` / `icon` | `@0x7f12020f` / `@0x7f08037a` | 【观测】 |
| `android:backupAgent` | `com.miui.antispam.service.backup.SecurityCenterBackupAgent` | 【观测】 |
| **`android:allowBackup`** | **不存在** | 【观测】→ 默认 **true**；配合上面的自定义 backupAgent，等于**应用数据（拦截规则/配置库）可被 backup 通道导出**，且进程身份是 system |
| `android:debuggable` | 不存在 | 【观测】→ false（release） |
| `android:requestLegacyExternalStorage` | `true` | 【观测】targetSdk 34 下该标志已失效，仅历史遗留 |
| `android:extractNativeLibs` | `false` | 【观测】so 直接从 APK 映射（体积换启动速度） |
| `android:appComponentFactory` | `androidx.core.app.CoreComponentFactory` | 【观测】AndroidX 组件工厂 |
| `android:networkSecurityConfig` | `@0x7f15003c` | 【观测】明文流量策略在 `res/xml/network_security_config.xml`（arsc 内未解，见 §9） |
| `android:resizeableActivity` | `false` | 【观测】 |
| `android:hardwareAccelerated` / `uiOptions` / `supportsRtl` | `true` / `0x1` / `true` | 【观测】 |
| `android:largeHeap` / `usesCleartextTraffic` / `fullBackupContent` / `dataExtractionRules` / `localeConfig` | 均不存在 | 【观测】 |

---

## 2. 权限

### 2.1 本 APK 声明的自定义权限：50 个（decoded.xml L51–L458，与 uses-permission 交错）

保护级别分布【观测】：**`signatureOrSystem (0x3)` = 43**、`signature (0x2)` = 3、`normal (0x0)` = 2、**未声明 protectionLevel（=normal）= 2**。

| # | 自定义权限 | protectionLevel | 本 APK 中受其保护的组件 | 行号 |
|---|---|---|---|---|
| 1 | `com.miui.securitycenter.permission.READ_APP_BEHAVIORS` | signatureOrSystem (0x3) | provider:PackageBehaviorContentProvider | 51 |
| 2 | `miui.permission.NOTIFICATION_VERIFY` | signatureOrSystem (0x3) | activity:NotificationVerifyActivity | 55 |
| 3 | `miui.permission.ACCESSIBILITY_SERVICE` | signatureOrSystem (0x3) | activity:AccessibilityServiceActivity | 57 |
| 4 | `miui.permission.APP_PREDICT_VERIFY` | signatureOrSystem (0x3) | service:AiService | 59 |
| 5 | `com.miui.autotask.permission.CONTENTPROVIDER` | signatureOrSystem (0x3) | provider:AutoTaskProvider | 61 |
| 6 | `com.xiaomi.permission.miuibatteryintelligence` | signatureOrSystem (0x3) | （无） | 63 |
| 7 | `com.miui.security.manager.SHORTCUT` | signatureOrSystem (0x3) | （无） | 77 |
| 8 | `miui.permission.TVM` | **未声明 → normal** | service:TvmManagerService | 79 |
| 9 | `miui.securitycenter.permission.ANALYTICS` | signatureOrSystem (0x3) | （无） | 81 |
| 10 | `miui.permission.MEMORY_CHECK` | signatureOrSystem (0x3) | service:MemoryCheckService | 83 |
| 11 | `com.miui.securitycenter.permission.SWITCH_DEV_MODE` | signatureOrSystem (0x3) | （无） | 85 |
| 12 | `com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER` | signatureOrSystem (0x3) | activity:ThirdDesktopAlertActivity<br>service:GameBoosterService<br>service:VideoToolBoxService<br>service:DockWindowManagerService<br>service:NotificationLinkService<br>provider:SecurityCenterProvider<br>provider:RemoteProvider<br>provider:ThirdMonitorProvider | 87 |
| 13 | `com.miui.monthreport.permission.DATA_MONTH_REPORT` | signatureOrSystem (0x3) | provider:MonthReportProvider | 89 |
| 14 | `com.miui.securitycenter.permission.NOTIFICATION_SERVICE` | signatureOrSystem (0x3) | service:NotificationService | 91 |
| 15 | `com.miui.powercenter.permission.POWER_DATA` | signatureOrSystem (0x3) | provider:PowerCenterProvider<br>provider:AutoTaskProvider | 93 |
| 16 | `com.miui.securitrycenter.permission.EXTERNAL_ANLYTICS` | signature (0x2) | （无） | 95 |
| 17 | `miui.permission.EXTRA_NETWORK` | signatureOrSystem (0x3) | provider:NetworkAssistantProvider<br>provider:LuckyMoneyProvider | 97 |
| 18 | `com.miui.powercenter.permission.POWER_COMMAND` | signatureOrSystem (0x3) | provider:PowerSaverProvider | 99 |
| 19 | `com.miui.securitycenter.permission.RECEIVE_TRACK_EVERYWEEK` | signatureOrSystem (0x3) | （无） | 101 |
| 20 | `com.miui.securitycenter.permission.CONTROL_VPN` | signatureOrSystem (0x3) | （无） | 103 |
| 21 | `com.miui.antispam.permission.CONTENTPROVIDER` | signatureOrSystem (0x3) | provider:AntiSpamProvider | 105 |
| 22 | `com.miui.antispam.permission.DATA_MONTH_REPORT` | signatureOrSystem (0x3) | service:AntiSpamService<br>service:CloudThirdDesktopService | 107 |
| 23 | `com.miui.securitycenter.permission.MEDIA_SERVER_KILL` | normal (0x0) | （无） | 109 |
| 24 | `com.miui.securitycenter.permission.GAMEBOOSTER_PROVIDER` | signatureOrSystem (0x3) | provider:GameBoosterProvider | 111 |
| 25 | `com.miui.securitycenter.permission.PERM_USE_INFO` | **未声明 → normal** | activity:AppPermissionsUseActivity | 113 |
| 26 | `com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE` | signatureOrSystem (0x3) | activity:ShutDownPasswordActivity<br>activity:SimLockStartActivity<br>activity:DevicePermissionSettingsActivity<br>activity:DevicePermissionEditorActivity<br>activity:GrantDevicePermissionActivity<br>activity:OtherPermissionsActivity<br>activity:CombinePermissionActivity<br>provider:AcrossTerminalPermissionContentProvider<br>provider:PrivacyAppsProvider | 115 |
| 27 | `com.miui.securitycenter.permission.REMOTE_ANALYTICS` | signatureOrSystem (0x3) | receiver:RemoteAnalyticsReceiver | 117 |
| 28 | `com.miui.securitycenter.permission.TOP_GAME_LIST` | signatureOrSystem (0x3) | （无） | 119 |
| 29 | `com.miui.securitycenter.permission.GB_SHOW_WINDOW` | signatureOrSystem (0x3) | （无） | 121 |
| 30 | `com.miui.securitycenter.permission.RECEIVE_TRACK_DAY` | signatureOrSystem (0x3) | （无） | 123 |
| 31 | `com.miui.securitycenter.permission.GB_CREATE_VOICE` | signatureOrSystem (0x3) | （无） | 125 |
| 32 | `com.miui.zman.permission.SECURITY_SHARE` | signatureOrSystem (0x3) | activity:PrivacyProtectSettingsActivity<br>activity:ShareSettingActivity<br>activity:PrivacyProtectTipSettingsActivity<br>provider:ZmanProvider | 127 |
| 33 | `com.miui.cleanmaster.permission.UPDATE_SHORTCUT` | signatureOrSystem (0x3) | （无） | 129 |
| 34 | `com.miui.cleaner.permission.UPDATE_CLEANER_SHORTCUT` | signatureOrSystem (0x3) | （无） | 131 |
| 35 | `com.miui.cleaner.permission.REMOTE_NOTIFICATION` | signatureOrSystem (0x3) | （无） | 133 |
| 36 | `com.miui.securitycenter.permission.CLEANER_MANAGE` | signatureOrSystem (0x3) | service:CleanerHandleService | 135 |
| 37 | `com.miui.securitycenter.permission.WIDGET_PROVIDER` | signatureOrSystem (0x3) | provider:WidgetProvider | 137 |
| 38 | `com.miui.dock.permission.DOCK_EVENT` | signatureOrSystem (0x3) | （无） | 139 |
| 39 | `com.miui.dock.permission.STATUS_CHANGE` | signatureOrSystem (0x3) | （无） | 141 |
| 40 | `com.miui.gamebooster.permission.PANNEL_OPEN` | signatureOrSystem (0x3) | （无） | 407 |
| 41 | `com.miui.securitycenter.POWER_CENTER_COMMON_PERMISSION` | signatureOrSystem (0x3) | （无） | 409 |
| 42 | `com.xiaomi.joyose.permission.predownload` | signatureOrSystem (0x3) | （无） | 413 |
| 43 | `com.miui.globalsatisfaction.permission.PROVIDER` | signatureOrSystem (0x3) | provider:GsProvider | 415 |
| 44 | `com.miui.gamebooster.access.ACTIVITY` | signatureOrSystem (0x3) | activity:SettingsActivity<br>activity:SelectGameLandActivity | 417 |
| 45 | `miui.permission.REMOTE_AUTH` | signatureOrSystem (0x3) | （无） | 437 |
| 46 | `miui.permission.TRUST_SERVICE_INTERNAL` | signatureOrSystem (0x3) | （无） | 439 |
| 47 | `com.miui.security.permission.DETECT_TEXT` | signatureOrSystem (0x3) | provider:SensitiveWordDetectProvider | 443 |
| 48 | `com.xiaomi.security.permission.ACCESS_XSOF` | normal (0x0) | （无） | 449 |
| 49 | `com.miui.securitycenter.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | signature (0x2) | （无） | 451 |
| 50 | `com.miui.securitycenter.permission.MIPUSH_RECEIVE` | signature (0x2) | （无） | 457 |

**两个"未声明 protectionLevel"的权限是真实缺口**【观测 + 推断】：
- `miui.permission.TVM`（L79，无 protectionLevel → **normal**）保护导出服务 `com.miui.tvm.service.TvmManagerService`（action `miui.intent.action.START_TVM`，L1264）。**注意**：该 service 同时是 `android:enabled="false"`，所以当前版本实际不可启动；这个保护级别缺口是**潜在的**（若系统侧用 `PackageManager.setComponentEnabledSetting` 将其启用则立即生效）。
- `com.miui.securitycenter.permission.PERM_USE_INFO`（L113，→ **normal**）保护导出 Activity `com.miui.permcenter.permissions.AppPermissionsUseActivity`（action `miui.intent.action.APP_PERMISSION_USE`）。
- 另有 `com.miui.securitycenter.permission.MEDIA_SERVER_KILL`（L109）与 `com.xiaomi.security.permission.ACCESS_XSOF`（L449）显式写 `normal`。
- 【推断】`normal` 级别意味着**任何第三方 App 都能在 manifest 里声明并即时获得**。因此凡是以这 4 个权限作为唯一防护的组件，其 "permission 保护" 事实上不成立 —— 命中的是 `TvmManagerService`(L1264，且 `enabled=false`) 与 `AppPermissionsUseActivity`(L3795)；是否还能被利用，需再结合组件内部是否做调用方校验（本轮未验证）。

### 2.2 `uses-permission`：147 条，全部无 `maxSdkVersion` 限制

`uses-permission-sdk-23` = 0。【观测】

分类统计【观测】：**AOSP 普通 30 / 危险（运行时）17 / AOSP 签名·特权 28 / MIUI·厂商私有 72**。

统计：A AOSP 普通 = 30，D 危险(运行时) = 17，C AOSP 签名/特权 = 28，E MIUI/厂商私有 = 72，合计 147

| # | 权限 | 分类 | maxSdk | 行号 |
|---|---|---|---|---|
| 1 | `android.permission.ACCESS_DOWNLOAD_MANAGER` | A AOSP 普通 | - | 209 |
| 2 | `android.permission.ACCESS_NETWORK_STATE` | A AOSP 普通 | - | 213 |
| 3 | `android.permission.ACCESS_WIFI_STATE` | A AOSP 普通 | - | 215 |
| 4 | `android.permission.ACCESS_WIMAX_STATE` | A AOSP 普通 | - | 291 |
| 5 | `android.permission.AUTHENTICATE_ACCOUNTS` | A AOSP 普通 | - | 351 |
| 6 | `android.permission.BLUETOOTH` | A AOSP 普通 | - | 375 |
| 7 | `android.permission.BLUETOOTH_ADMIN` | A AOSP 普通 | - | 377 |
| 8 | `android.permission.CHANGE_WIFI_STATE` | A AOSP 普通 | - | 217 |
| 9 | `android.permission.CHANGE_WIMAX_STATE` | A AOSP 普通 | - | 293 |
| 10 | `android.permission.CONFIGURE_WIFI_DISPLAY` | A AOSP 普通 | - | 319 |
| 11 | `android.permission.DISABLE_KEYGUARD` | A AOSP 普通 | - | 333 |
| 12 | `android.permission.DOWNLOAD_WITHOUT_NOTIFICATION` | A AOSP 普通 | - | 355 |
| 13 | `android.permission.EXPAND_STATUS_BAR` | A AOSP 普通 | - | 287 |
| 14 | `android.permission.FOREGROUND_SERVICE` | A AOSP 普通 | - | 381 |
| 15 | `android.permission.FOREGROUND_SERVICE_DATA_SYNC` | A AOSP 普通 | - | 441 |
| 16 | `android.permission.GET_PACKAGE_SIZE` | A AOSP 普通 | - | 249 |
| 17 | `android.permission.INTERNET` | A AOSP 普通 | - | 219 |
| 18 | `android.permission.KILL_BACKGROUND_PROCESSES` | A AOSP 普通 | - | 243 |
| 19 | `android.permission.MANAGE_ACCOUNTS` | A AOSP 普通 | - | 309 |
| 20 | `android.permission.READ_SYNC_SETTINGS` | A AOSP 普通 | - | 255 |
| 21 | `android.permission.READ_USER_DICTIONARY` | A AOSP 普通 | - | 225 |
| 22 | `android.permission.RECEIVE_BOOT_COMPLETED` | A AOSP 普通 | - | 251 |
| 23 | `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | A AOSP 普通 | - | 435 |
| 24 | `android.permission.SCHEDULE_EXACT_ALARM` | A AOSP 普通 | - | 19 |
| 25 | `android.permission.USE_CREDENTIALS` | A AOSP 普通 | - | 311 |
| 26 | `android.permission.VIBRATE` | A AOSP 普通 | - | 447 |
| 27 | `android.permission.WAKE_LOCK` | A AOSP 普通 | - | 307 |
| 28 | `android.permission.WRITE_SYNC_SETTINGS` | A AOSP 普通 | - | 349 |
| 29 | `android.permission.WRITE_USER_DICTIONARY` | A AOSP 普通 | - | 227 |
| 30 | `com.android.launcher.permission.INSTALL_SHORTCUT` | A AOSP 普通 | - | 259 |
| 31 | `android.permission.ACCESS_COARSE_LOCATION` | D 危险(运行时) | - | 25 |
| 32 | `android.permission.ACCESS_FINE_LOCATION` | D 危险(运行时) | - | 33 |
| 33 | `android.permission.BLUETOOTH_CONNECT` | D 危险(运行时) | - | 53 |
| 34 | `android.permission.BLUETOOTH_SCAN` | D 危险(运行时) | - | 47 |
| 35 | `android.permission.BODY_SENSORS` | D 危险(运行时) | - | 37 |
| 36 | `android.permission.CALL_PHONE` | D 危险(运行时) | - | 205 |
| 37 | `android.permission.GET_ACCOUNTS` | D 危险(运行时) | - | 39 |
| 38 | `android.permission.POST_NOTIFICATIONS` | D 危险(运行时) | - | 29 |
| 39 | `android.permission.READ_CALL_LOG` | D 危险(运行时) | - | 357 |
| 40 | `android.permission.READ_CONTACTS` | D 危险(运行时) | - | 211 |
| 41 | `android.permission.READ_EXTERNAL_STORAGE` | D 危险(运行时) | - | 23 |
| 42 | `android.permission.READ_PHONE_STATE` | D 危险(运行时) | - | 35 |
| 43 | `android.permission.READ_SMS` | D 危险(运行时) | - | 43 |
| 44 | `android.permission.RECORD_AUDIO` | D 危险(运行时) | - | 41 |
| 45 | `android.permission.SEND_SMS` | D 危险(运行时) | - | 301 |
| 46 | `android.permission.WRITE_CALL_LOG` | D 危险(运行时) | - | 359 |
| 47 | `android.permission.WRITE_EXTERNAL_STORAGE` | D 危险(运行时) | - | 21 |
| 48 | `android.permission.ACCESS_CACHE_FILESYSTEM` | C AOSP 签名/特权 | - | 271 |
| 49 | `android.permission.BACKUP` | C AOSP 签名/特权 | - | 235 |
| 50 | `android.permission.BATTERY_STATS` | C AOSP 签名/特权 | - | 233 |
| 51 | `android.permission.CLEAR_APP_CACHE` | C AOSP 签名/特权 | - | 245 |
| 52 | `android.permission.CLEAR_APP_USER_DATA` | C AOSP 签名/特权 | - | 221 |
| 53 | `android.permission.CONTROL_VPN` | C AOSP 签名/特权 | - | 313 |
| 54 | `android.permission.DELETE_CACHE_FILES` | C AOSP 签名/特权 | - | 247 |
| 55 | `android.permission.DEVICE_POWER` | C AOSP 签名/特权 | - | 201 |
| 56 | `android.permission.FORCE_STOP_PACKAGES` | C AOSP 签名/特权 | - | 229 |
| 57 | `android.permission.GET_APP_OPS_STATS` | C AOSP 签名/特权 | - | 15 |
| 58 | `android.permission.GET_TASKS` | C AOSP 签名/特权 | - | 281 |
| 59 | `android.permission.MANAGE_NETWORK_POLICY` | C AOSP 签名/特权 | - | 295 |
| 60 | `android.permission.MANAGE_USB` | C AOSP 签名/特权 | - | 237 |
| 61 | `android.permission.MASTER_CLEAR` | C AOSP 签名/特权 | - | 207 |
| 62 | `android.permission.MODIFY_NETWORK_ACCOUNTING` | C AOSP 签名/特权 | - | 299 |
| 63 | `android.permission.MODIFY_PHONE_STATE` | C AOSP 签名/特权 | - | 223 |
| 64 | `android.permission.MOUNT_UNMOUNT_FILESYSTEMS` | C AOSP 签名/特权 | - | 203 |
| 65 | `android.permission.PACKAGE_USAGE_STATS` | C AOSP 签名/特权 | - | 231 |
| 66 | `android.permission.PACKAGE_VERIFICATION_AGENT` | C AOSP 签名/特权 | - | 339 |
| 67 | `android.permission.READ_NETWORK_USAGE_HISTORY` | C AOSP 签名/特权 | - | 297 |
| 68 | `android.permission.READ_SECURE_SETTINGS` | C AOSP 签名/特权 | - | 303 |
| 69 | `android.permission.READ_SETTINGS` | C AOSP 签名/特权 | - | 305 |
| 70 | `android.permission.REAL_GET_TASKS` | C AOSP 签名/特权 | - | 335 |
| 71 | `android.permission.REBOOT` | C AOSP 签名/特权 | - | 239 |
| 72 | `android.permission.SYSTEM_ALERT_WINDOW` | C AOSP 签名/特权 | - | 331 |
| 73 | `android.permission.UPDATE_APP_OPS_STATS` | C AOSP 签名/特权 | - | 253 |
| 74 | `android.permission.WRITE_APN_SETTINGS` | C AOSP 签名/特权 | - | 321 |
| 75 | `android.permission.WRITE_SETTINGS` | C AOSP 签名/特权 | - | 199 |
| 76 | `com.android.permission.GET_INSTALLED_APPS` | E MIUI/厂商私有 | - | 45 |
| 77 | `com.android.permission.WN_CHECKER` | E MIUI/厂商私有 | - | 49 |
| 78 | `com.android.settings.permission.POWER_MODE_CHANGE` | E MIUI/厂商私有 | - | 337 |
| 79 | `com.android.settings.permission.PULL_CLOUD_DATA` | E MIUI/厂商私有 | - | 267 |
| 80 | `com.android.settings.permission.SWITCH_DEV_MODE` | E MIUI/厂商私有 | - | 265 |
| 81 | `com.android.systemui.permission.NOTIFICATION` | E MIUI/厂商私有 | - | 31 |
| 82 | `com.android.updater.permission.ACCESS_UPDATESERVICE` | E MIUI/厂商私有 | - | 283 |
| 83 | `com.android.updater.permission.RECEIVE_BROADCAST` | E MIUI/厂商私有 | - | 285 |
| 84 | `com.cleanmaster.permission.sdk.clean` | E MIUI/厂商私有 | - | 241 |
| 85 | `com.mi.health.provider.device.read` | E MIUI/厂商私有 | - | 399 |
| 86 | `com.mipay.wallet.permission.READ_IDENTITY_STATUS` | E MIUI/厂商私有 | - | 329 |
| 87 | `com.miui.accessibility.permission.ACCESS_SI` | E MIUI/厂商私有 | - | 431 |
| 88 | `com.miui.calendar.permission.READ_CALENDAR` | E MIUI/厂商私有 | - | 387 |
| 89 | `com.miui.cleanmaster.permission.Clean_Master` | E MIUI/厂商私有 | - | 327 |
| 90 | `com.miui.dock.permission.DOCK_EVENT` | E MIUI/厂商私有 | - | 401 |
| 91 | `com.miui.gamebooster.access.ACTIVITY` | E MIUI/厂商私有 | - | 421 |
| 92 | `com.miui.gamebooster.permission.PARENT_VERIFY` | E MIUI/厂商私有 | - | 419 |
| 93 | `com.miui.gameturbo.permission.ACTIVE_MANAGER` | E MIUI/厂商私有 | - | 385 |
| 94 | `com.miui.guardprovider.permission.Guard_Provider` | E MIUI/厂商私有 | - | 325 |
| 95 | `com.miui.home.launcher.permission.UNINSTALL_SHORTCUT` | E MIUI/厂商私有 | - | 261 |
| 96 | `com.miui.monthreport.permission.DATA_MONTH_REPORT` | E MIUI/厂商私有 | - | 279 |
| 97 | `com.miui.permission.FIREWALL` | E MIUI/厂商私有 | - | 341 |
| 98 | `com.miui.permission.MANAGE_BOOT_TIME` | E MIUI/厂商私有 | - | 273 |
| 99 | `com.miui.permission.MANAGE_SMS_INTERCEPT` | E MIUI/厂商私有 | - | 315 |
| 100 | `com.miui.permission.REGION_SAMPLING` | E MIUI/厂商私有 | - | 455 |
| 101 | `com.miui.powerkeeper.permission.POWER_NOTIFY` | E MIUI/厂商私有 | - | 73 |
| 102 | `com.miui.powerkeeper.permission.REQUEST_WAKE` | E MIUI/厂商私有 | - | 395 |
| 103 | `com.miui.security.manager.SHORTCUT` | E MIUI/厂商私有 | - | 69 |
| 104 | `com.miui.security.permission.installOtherApp` | E MIUI/厂商私有 | - | 75 |
| 105 | `com.miui.securityadd.permission.GAMEBOOSTER_PROVIDER` | E MIUI/厂商私有 | - | 363 |
| 106 | `com.miui.securityadd.permission.KID_MODE_ACCESS` | E MIUI/厂商私有 | - | 379 |
| 107 | `com.miui.securityadd.permission.SUPERPOWER_RECEIVE` | E MIUI/厂商私有 | - | 373 |
| 108 | `com.miui.securitycenter.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | E MIUI/厂商私有 | - | 453 |
| 109 | `com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER` | E MIUI/厂商私有 | - | 275 |
| 110 | `com.miui.securitycenter.permission.GAMEBOOSTER_PROVIDER` | E MIUI/厂商私有 | - | 361 |
| 111 | `com.miui.securitycenter.permission.GLOBAL_PACKAGEINSTALLER` | E MIUI/厂商私有 | - | 367 |
| 112 | `com.miui.securitycenter.permission.MIPUSH_RECEIVE` | E MIUI/厂商私有 | - | 459 |
| 113 | `com.miui.securitycenter.permission.PERM_USE_INFO` | E MIUI/厂商私有 | - | 369 |
| 114 | `com.miui.systemAdSolution.adSwitch.PROVIDER` | E MIUI/厂商私有 | - | 343 |
| 115 | `com.miui.voiceassist.QUERY_ENABLE` | E MIUI/厂商私有 | - | 371 |
| 116 | `com.miui.yellowpage.permission.ACCESS_YELLOWPAGE` | E MIUI/厂商私有 | - | 317 |
| 117 | `com.qualcomm.permission.IZAT` | E MIUI/厂商私有 | - | 13 |
| 118 | `com.xiaomi.aicr.permission.OPEN_CAPABILITY_INTERFACE` | E MIUI/厂商私有 | - | 433 |
| 119 | `com.xiaomi.calendar.permission.READ_CALENDAR` | E MIUI/厂商私有 | - | 389 |
| 120 | `com.xiaomi.dist.permission.DIST_HARDWARE_SERVICE` | E MIUI/厂商私有 | - | 425 |
| 121 | `com.xiaomi.gamecenter.for3thd.migame.xcashier` | E MIUI/厂商私有 | - | 345 |
| 122 | `com.xiaomi.macro.permission` | E MIUI/厂商私有 | - | 391 |
| 123 | `com.xiaomi.permission.ACCESS_MMS_NETWORK_STATE` | E MIUI/厂商私有 | - | 27 |
| 124 | `com.xiaomi.permission.ACCESS_SECURITY_DEVICE_CREDENTIAL` | E MIUI/厂商私有 | - | 365 |
| 125 | `com.xiaomi.permission.BIND_CONTINUITY_SERVICE_INTERNAL` | E MIUI/厂商私有 | - | 405 |
| 126 | `com.xiaomi.permission.CLOUD_MANAGER` | E MIUI/厂商私有 | - | 289 |
| 127 | `com.xiaomi.permission.miuibatteryintelligence` | E MIUI/厂商私有 | - | 71 |
| 128 | `com.xiaomi.permission.USE_KEYBAG` | E MIUI/厂商私有 | - | 393 |
| 129 | `com.xiaomi.smarthome.READ_PHONE_SCENE_CONDITION` | E MIUI/厂商私有 | - | 65 |
| 130 | `com.xiaomi.smarthome.SYNC_PHONE_SCENE_CONDITION` | E MIUI/厂商私有 | - | 67 |
| 131 | `com.xiaomi.vtcamera.permission.ACTIVITY_PERMISSION_DECLARE` | E MIUI/厂商私有 | - | 423 |
| 132 | `com.xiaomi.vtcamera.permission.SERVICE_PERMISSION_DECLARE` | E MIUI/厂商私有 | - | 427 |
| 133 | `com.xiaomi.wearable.provider.device.read` | E MIUI/厂商私有 | - | 397 |
| 134 | `com.xiaomi.xmsf.permission.MIPUSH_RECEIVE` | E MIUI/厂商私有 | - | 347 |
| 135 | `com.xiaomi.xmsf.permission.USE_XMSF_TRAFFIC` | E MIUI/厂商私有 | - | 383 |
| 136 | `miui.packageinstaller.permission.ACTION_INFO` | E MIUI/厂商私有 | - | 403 |
| 137 | `miui.permission.ACCESS_AUDIOMONITOR` | E MIUI/厂商私有 | - | 429 |
| 138 | `miui.permission.ACCESS_BLE_SETTINGS` | E MIUI/厂商私有 | - | 353 |
| 139 | `miui.permission.ACCESS_MILINK_CAST` | E MIUI/厂商私有 | - | 461 |
| 140 | `miui.permission.BACKUP_PROXY` | E MIUI/厂商私有 | - | 263 |
| 141 | `miui.permission.FILE_PROXY` | E MIUI/厂商私有 | - | 257 |
| 142 | `miui.permission.powerkeeper.HIDDEN_MODE_PROVIDER` | E MIUI/厂商私有 | - | 323 |
| 143 | `miui.permission.READ_AND_WIRTE_PERMISSION_MANAGER` | E MIUI/厂商私有 | - | 277 |
| 144 | `miui.permission.restore_brightness` | E MIUI/厂商私有 | - | 411 |
| 145 | `miui.permission.SHELL` | E MIUI/厂商私有 | - | 269 |
| 146 | `miui.permission.USE_INTERNAL_GENERAL_API` | E MIUI/厂商私有 | - | 17 |
| 147 | `xiaomi.permission.aivision.MI_AI_TRANSLATE` | E MIUI/厂商私有 | - | 445 |

### 2.3 高危能力组合（这是"它为什么必须预装"的直接答案）

**（a）AOSP 签名/特权级权限 28 个**——正常应用即使动态申请也拿不到，只有 `signature|privileged` + 落在 `/system/priv-app` 或平台签名才授予【观测权限名，推断授予条件】：

| 能力簇 | 涉及权限 | 能做到什么 |
|---|---|---|
| 短信双通 | `READ_SMS` + `SEND_SMS` | 读全部短信（含验证码）+ 代发短信 —— 与 `com.miui.permission.MANAGE_SMS_INTERCEPT`（拦截规则管理）组合即"短信内容全权" |
| 通话全权 | `READ_CALL_LOG` + `WRITE_CALL_LOG` + `MODIFY_PHONE_STATE` + `CALL_PHONE` | 读写通话记录、改电话状态（`MODIFY_PHONE_STATE` 是 signature|privileged） |
| 设备抹除/重启 | **`MASTER_CLEAR`**、`REBOOT`、`DEVICE_POWER` | 恢复出厂、重启、直接控电源 |
| 包与进程生杀 | `FORCE_STOP_PACKAGES`、`CLEAR_APP_USER_DATA`、`CLEAR_APP_CACHE`、`DELETE_CACHE_FILES`、`KILL_BACKGROUND_PROCESSES`、`REAL_GET_TASKS`、`GET_TASKS` | 强停任意应用、清任意应用数据 |
| 安装/校验链 | **`PACKAGE_VERIFICATION_AGENT`** | 成为包校验代理，**拦下/放行全系统 APK 安装**（manifest 里 `PackageVerificationReceiver` 对该 action 有接收器，L2109 / action L2110） |
| USB | **`MANAGE_USB`** | 管理 USB 设备/授权 |
| 应用使用与 AppOps | `PACKAGE_USAGE_STATS`、`GET_APP_OPS_STATS`、`UPDATE_APP_OPS_STATS`、`BATTERY_STATS` | 读全量使用记录、**改写任意应用的 AppOps 授权状态**、读全系统耗电 |
| 备份 | `BACKUP` | 触发系统备份/恢复通道 |
| 存储/文件系统 | `MOUNT_UNMOUNT_FILESYSTEMS`、`ACCESS_CACHE_FILESYSTEM`、`MANAGE_EXTERNAL_STORAGE` | 挂载文件系统、访问 cache 分区（本 APK 已用 `/data/system/dropbox`、`/data/app` 等 system 路径，见他人报告） |
| 网络治理 | `MANAGE_NETWORK_POLICY`、`READ_NETWORK_USAGE_HISTORY`、`MODIFY_NETWORK_ACCOUNTING`、`WRITE_APN_SETTINGS`、`CONTROL_VPN` | 改网络策略/APN、读全量流量历史、**接管 VPN** |
| 设置 | `WRITE_SETTINGS`、`READ_SECURE_SETTINGS`、`READ_SETTINGS`、`SYSTEM_ALERT_WINDOW` | 改系统设置、读 secure settings、全局悬浮窗 |

**（b）72 个 MIUI/厂商私有权限**——只有 MIUI 平台签名或预装系统应用可持有【观测权限名，推断授予条件】。代表性高危项：

| 权限 | 语义（按命名推断） | 持有者组件 |
|---|---|---|
| `miui.permission.SHELL` | MIUI shell 通道 | — |
| `miui.permission.READ_AND_WIRTE_PERMISSION_MANAGER` | 读写**权限管理器**内部数据 | — |
| `miui.permission.FILE_PROXY` / `miui.permission.BACKUP_PROXY` | 文件/备份代理通道 | — |
| `com.miui.permission.FIREWALL` | 防火墙规则 | `NetworkAssistantProvider` 等 |
| `com.miui.permission.MANAGE_SMS_INTERCEPT` | 短信拦截规则管理 | `AntiSpamProvider` |
| `com.miui.security.permission.installOtherApp` | 代装其它应用 | — |
| `com.miui.securitycenter.permission.GLOBAL_PACKAGEINSTALLER` | 全局安装器 | — |
| `miui.packageinstaller.permission.ACTION_INFO` | 安装器内部信息 | — |
| `com.android.settings.permission.SWITCH_DEV_MODE` / `PULL_CLOUD_DATA` | 切开发者模式 / 拉云端配置 | — |
| `com.android.updater.permission.ACCESS_UPDATESERVICE` / `RECEIVE_BROADCAST` | OTA 更新服务 | `UpdaterReceiver` |
| `com.miui.monthreport.permission.DATA_MONTH_REPORT` | 月度报告数据 | 3 个 provider + 2 个 service 复用它 |
| `com.xiaomi.xmsf.permission.MIPUSH_RECEIVE` | 通过 XMSF 收推送 | `XiaomiPushReceiver`、`PushMessageHandler` |
| `com.xiaomi.permission.CLOUD_MANAGER` | 云同步管理 | `SettingsCloudBackupService` |
| `com.miui.permission.MANAGE_BOOT_TIME` | 改开机时间记录 | — |

**（c）结论**：`sharedUserId=android.uid.system` + 28 个 AOSP `signature|privileged` + 72 个厂商私有 = 这个包 **不可能以普通应用身份安装**。它必须走 `/system/priv-app/MIUISecurityCenter` 或同等预装路径，并且必须持有 MIUI 平台密钥签名（见 §3）。这也解释了它为什么能直接读写 `/data/system/*`。

---

## 3. 签名

REA `inspect_apk_signature`，evidence_id **`ev_774c8c1faf38e245d0c008a0979e7abc3958ee7dd6e00b48ac3bc5de819cb354`**，`authority=shipped-artifact`，`confidence=observed`。

### 3.1 方案与一致性【观测】

| 项 | 值 |
|---|---|
| v1 (JAR) | present，`META-INF/PLATFORM.SF` + `META-INF/PLATFORM.RSA` |
| v2 | present，1 signer，digest 算法 `sha256`（id 259），`matches_apk: true` |
| v3 | present，1 signer，`min_sdk=24` `max_sdk=2147483647`，`rotation.present=false`（**无密钥轮换 lineage**） |
| v3.1 | **absent** |
| source stamp | **absent** |
| 最强方案 | v3 |
| `schemes_agree_on_signer` / `v1_signers_match_block_signers` / `self_consistent` | 全 `true` |
| `uninterpreted_bytes` | v2 signed_data 内 4 B（不足以判定畸形） |

⚠️ **REA 明确声明**：`matches_apk` 只表示"声明的摘要 == 当前字节的摘要"，**不是签名验证**；证书链、撤销、真实性均未校验【观测 limitation】。本轮未做独立 `apksigner verify`。

### 3.2 证书【观测】

```
subject : C=CN, ST=Beijing, L=Beijing, O=Xiaomi, OU=MIUI, CN=MIUI, emailAddress=[VENDOR_EMAIL_REMOVED]
issuer  : 同上（自签名）
SHA-256 : c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025
serial  : E552A8ECB9011B7C
有效期  : 2011-12-06T03:26:26Z  →  2039-04-23T03:26:26Z
公钥    : RSA（292 B 公钥编码；v1 签名算法 OID 1.2.840.113549.1.1.1，digest 2.16.840.1.101.3.4.2.1 = SHA-256）
```

**是否为平台签名？** —— 是，但**不是 AOSP 通用测试平台签名**（`CN=Android, O=Android, C=US`），而是**小米 MIUI 平台密钥**（`O=Xiaomi / OU=MIUI / CN=MIUI`，2011 年签发、有效期到 2039 年、自签名，符合"厂商长期平台密钥"的全部特征）【观测证书字段 + 推断"平台密钥"身份】。这直接解释了：本 APK 声明/使用的 `signature` 与 `signatureOrSystem` 权限，只有**同一 MIUI 平台密钥**签名的应用才能互相授予。

### 3.3 v1 覆盖范围【观测】

| 项 | 值 |
|---|---|
| ZIP 总条目 | **9231** |
| `MANIFEST.MF` 中 `Name:` 条目 | **9228**（每条配一个 `SHA-256-Digest`，共 9228 条） |
| 差额 3 条 | `META-INF/MANIFEST.MF`、`META-INF/PLATFORM.SF`、`META-INF/PLATFORM.RSA` 本身（按 JAR 规范不计入） |
| `PLATFORM.SF` | `Signature-Version: 1.0`、`SHA-256-Digest-Manifest` 1 条、`X-Android-APK-Signed: 2, 3` |
| 额外签名文件 | **无**。`META-INF\` 下只有 `MANIFEST.MF / PLATFORM.SF / PLATFORM.RSA / com\android\build\gradle\app-metadata.properties / services\*`，**没有** `CERT.RSA`、没有第二组 `.SF/.RSA`、没有 `.version` 系列库标记（对比 GuardProvider 有 40+ 个 `*.version`） |

【推断】`X-Android-APK-Signed: 2, 3` 表示 v1 块声明"另外还有 v2/v3"，与实测 v2/v3 present 一致，属于正常打包（不是"剥离 v2/v3 后伪造 v1"的迹象）。

---

## 4. Manifest 全量结构

### 4.1 组件总表【观测】

| kind | 总数 | exported=true | exported 且**无权限保护** | exported 且有权限 |
|---|---|---|---|---|
| activity | 293 | 127 | 106 | 21 |
| activity-alias | 2 | 2 | 2 | 0 |
| service | 67 | 21 | 2 | 19 |
| receiver | 22 | 16 | 8 | 8 |
| **provider** | **40** | **28** | **10** | **18** |
| **合计** | **424** | **194** | **128** | **66** |

补充【观测】：
- `android:exported` 未设置的组件 **4 个，且全部没有 intent-filter** → 事实上都不可被外部拉起（provider 在 targetSdk≥17 默认 false，其余默认 false 因无 filter）。
- 因此 **194 个 exported 全部是显式声明**，不存在"默认导出"的隐蔽面。
- **`android:enabled="false"` 的组件共 6 个，其中 5 个是 exported**：`MainEntryActivity`（无权限，旧入口）、`BubblesNotificationListenerService`、`TvmManagerService`、`SuperPowerTileService`、`PerformanceModeTileService`、`PerformanceWildModeTileService`。被禁用的组件 `PackageManager` 会拒绝拉起，所以**真正可用的 exported = 188**；A.1–A.5 表中只有 `MainEntryActivity` 属于"已禁用"，其余 127 个零权限导出组件都是**当前有效**的暴露面。

### 4.2 完整组件清单

#### A. exported 但**无 permission 保护**（外部可任意拉起，组件以 android.uid.system 运行）

##### A.1 provider (10)

| # | 组件 | action / filter | 进程 | authorities | L |
|---|---|---|---|---|---|
| 1 | `com.miui.autotask.provider.AutoTaskServiceProvider` | - | SC.remote | com.miui.autotask.provider.auto_task_service | 1225 |
| 2 | `com.miui.permcenter.privacycenter.PrivacyCenterProvider` | - | SC.remote | com.miui.permcenter.privacycenter | 1358 |
| 3 | `com.miui.securitycenter.provider.DeviceCredentialProvider` | - | SC.remote | com.miui.securitycenter.credentialprovider | 2480 |
| 4 | `com.miui.idprovider.IdProvider` | - | SC.remote | com.miui.idprovider | 2507 |
| 5 | `com.miui.sec.external.InstallProvider` | - | (主进程) | com.miui.sec.INSTALL_PACKAGE | 3806 |
| 6 | `com.miui.securitycenter.provider.SecSettingsSearchProvider` | miui.intent.action.SETTINGS_SEARCH_PROVIDER | (主进程) | com.miui.securitycenter.provider.SecSettingsSearchProvider | 3813 |
| 7 | `com.miui.securityadd.input.InputProvider` | - | SC.remote | com.miui.input.provider | 4076 |
| 8 | `com.miui.warningcenter.provider.WarningCenterProvider` | - | SC.remote | com.miui.warningcenter.provider | 4147 |
| 9 | `com.miui.securitycenter.SystemAdContentProvider` | - | SC.remote | com.miui.securitycenter.splashAd | 4491 |
| 10 | `com.xiaomi.security.xsof.XSOFProvider` | - | SC.remote | com.xiaomi.security.xsof.provider | 4989 |

##### A.2 service (2)

| # | 组件 | action / filter | 进程 | authorities | L |
|---|---|---|---|---|---|
| 1 | `com.miui.networkassistant.vpn.miui.MiuiVpnManageService` | com.miui.networkassistant.vpn.MIUI_VPN_MANAGE_SERVICE | SC.remote | - | 2974 |
| 2 | `com.xiaomi.security.xsof.MiSafetyDetectService` | com.xiaomi.security.xsof.SAFETY_DETECT_SERVICE | SC.remote | - | 4994 |

##### A.3 receiver (8)

| # | 组件 | action / filter | 进程 | authorities | L |
|---|---|---|---|---|---|
| 1 | `com.miui.apppredict.widget.AppPredictWidget2x4` | android.appwidget.action.APPWIDGET_UPDATE<br>miui.appwidget.action.APPWIDGET_UPDATE<br>com.miui.action.UPDATE_PREDICT_LIST | SC.remote | - | 736 |
| 2 | `com.miui.simlock.BootCompleteReceiver` | android.intent.action.LOCKED_BOOT_COMPLETED | SC.bootaware | - | 1061 |
| 3 | `com.miui.autotask.common.PolarisGeofenceReceiver` | - | SC.remote | - | 1412 |
| 4 | `com.miui.permcenter.privacymanager.widget.BehaviorWidget` | android.appwidget.action.APPWIDGET_UPDATE<br>miui.appwidget.action.APPWIDGET_UPDATE | SC.remote | - | 2119 |
| 5 | `com.miui.permcenter.DeviceOwnerChangedReceiver` | android.app.action.DEVICE_OWNER_CHANGED | (主进程) | - | 2135 |
| 6 | `com.miui.permcenter.root.RootUpdateReceiver` | com.android.updater.action.ACQUIRED_ROOT_SUCCESSED | (主进程) | - | 2409 |
| 7 | `com.miui.powercenter.provider.DateResetReceiver` | android.intent.action.TIME_SET<br>android.intent.action.TIMEZONE_CHANGED<br>android.intent.action.LOCALE_CHANGED | SC.remote | - | 2418 |
| 8 | `com.miui.securityadd.input.ProvisionCompleteReceiver` | android.provision.action.PROVISION_COMPLETE | SC.remote | - | 4081 |

##### A.4 activity (106)

| # | 组件 | action / filter | 进程 | authorities | L |
|---|---|---|---|---|---|
| 1 | `com.miui.securityscan.MainEntryActivity` | android.intent.action.MAIN<br>mi.quicksearch.intent.action.RETRIEVE<br>miui.intent.action.SECURITY_CENTER<br>android.intent.action.VIEW | (主进程) | - | 500 |
| 2 | `com.miui.permcenter.autostart.AutoStartManagementActivity` | miui.intent.action.OP_AUTO_START | (主进程) | - | 554 |
| 3 | `com.miui.earthquakewarning.ui.EarthquakeWarningMainActivity` | miui.intent.action.EARTHQUAKE_WARNING_MAIN | (主进程) | - | 594 |
| 4 | `com.miui.electricrisk.ElectricRiskMainActivity` | miui.intent.action.ELECTRIC_RISK | (主进程) | - | 622 |
| 5 | `com.miui.electricrisk.ElectricProtectPhoneActivity` | - | (主进程) | - | 636 |
| 6 | `com.miui.electricrisk.AiGuardPosterActivity` | com.android.settings.action.AI_ENHANCED_SETTINGS | (主进程) | - | 643 |
| 7 | `com.miui.permcenter.settings.PrivacyInputModeActivity` | miui.intent.action.PRIVACY_INPUT_MODE_ACTIVITY | (主进程) | - | 694 |
| 8 | `com.miui.autotask.activity.TaskManagerActivity` | com.miui.autotask.AUTO_TASK_MANAGER_ACTIVITY | (主进程) | - | 850 |
| 9 | `com.miui.autotask.activity.BluetoothSelectActivity` | - | (主进程) | - | 938 |
| 10 | `com.miui.autotask.activity.WlanSelectActivity` | - | (主进程) | - | 946 |
| 11 | `com.miui.simlock.activity.SimLockChooseActivity` | miui.intent.action.SIM_LOCK_CHOOSE | (主进程) | - | 967 |
| 12 | `com.miui.idprovider.ui.legacy.OAIDAppsActivity` | miui.intent.action.OAID_APPS | (主进程) | - | 1089 |
| 13 | `com.miui.idprovider.ui.legacy.OAIDSettingsActivityLegacy` | - | (主进程) | - | 1103 |
| 14 | `com.miui.idprovider.ui.OAIDAppSettingsActivity` | - | (主进程) | - | 1111 |
| 15 | `com.miui.idprovider.ui.OAIDSettingsActivity` | - | (主进程) | - | 1119 |
| 16 | `com.miui.permcenter.privacycenter.usage.PermissionUsageRecordActivity` | miui.intent.action.APP_BEHAVIRO_RECORD | (主进程) | - | 1316 |
| 17 | `com.miui.permcenter.privacycenter.usage.AppPermissionUsageActivity` | miui.intent.action.APP_PRIVACY_DETAIL | (主进程) | - | 1341 |
| 18 | `com.miui.permcenter.settings.InvisibleModeActivity` | com.miui.securitycenter.action.INVISIBLE_SETTING | (主进程) | - | 1367 |
| 19 | `com.miui.warningcenter.policeassist.PaSettingActivity` | miui.intent.action.WARNINGCENTER_POLICE_ASSIST | (主进程) | - | 1396 |
| 20 | `com.miui.securityscan.MainActivity` | android.intent.action.MAIN<br>mi.quicksearch.intent.action.RETRIEVE<br>miui.intent.action.SECURITY_CENTER<br>android.intent.action.VIEW | (主进程) | - | 1461 |
| 21 | `com.miui.securityscan.ui.settings.SettingsActivity` | miui.intent.action.APP_SETTINGS<br>com.miui.securitycenter.action.SECURITYCENTER_SETTINGS | (主进程) | - | 1508 |
| 22 | `com.miui.firstaidkit.FirstAidKitActivity` | com.miui.securitycenter.action.FIRST_AID_KIT | (主进程) | - | 1535 |
| 23 | `com.miui.antivirus.activity.MainActivity` | miui.intent.action.ANTI_VIRUS | (主进程) | - | 1581 |
| 24 | `com.miui.antivirus.activity.SettingsActivity` | com.miui.securitycenter.action.ANTIVIRUS_SETTINGS | (主进程) | - | 1595 |
| 25 | `com.miui.antivirus.whitelist.WhiteListActivity` | miui.intent.action.ANTI_VIRUS_WHITELIST | (主进程) | - | 1610 |
| 26 | `com.miui.permcenter.MainAcitivty` | miui.intent.action.LICENSE_MANAGER | (主进程) | - | 1648 |
| 27 | `com.miui.permcenter.permissions.AppPermissionsTabActivity` | - | (主进程) | - | 1668 |
| 28 | `com.miui.permcenter.permissions.PermissionsEditorActivity` | miui.intent.action.APP_PERM_EDITOR_PRIVATE<br>miui.intent.action.APP_PERM_EDITOR | (主进程) | - | 1675 |
| 29 | `com.miui.permcenter.permissions.PermissionAppsEditorActivity` | miui.intent.action.PERMISSION_APP_EDITOR_ACTIVITY | (主进程) | - | 1695 |
| 30 | `com.miui.devicepermission.editpermission.DevicePermissionModifyActivity` | - | (主进程) | - | 1725 |
| 31 | `com.miui.powercenter.PowerMainActivity` | miui.intent.action.POWER_MANAGER<br>miui.intent.action.POWER_BATTERY<br>android.intent.action.POWER_USAGE_SUMMARY | (主进程) | - | 1877 |
| 32 | `com.miui.powercenter.PowerCenter` | miui.intent.action.POWER_SCAN | (主进程) | - | 1903 |
| 33 | `com.miui.powercenter.bootshutdown.PowerShutdownOnTime` | miui.powercenter.intent.action.BOOT_SHUTDOWN_ONTIME | (主进程) | - | 1917 |
| 34 | `com.miui.powercenter.legacypowerrank.PowerDetailActivity` | android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | (主进程) | - | 1931 |
| 35 | `com.miui.powercenter.PowerSettings` | com.miui.securitycenter.action.POWER_SETTINGS<br>com.miui.securitycenter.action.FIRMWARE_UPDATE | (主进程) | - | 1988 |
| 36 | `com.miui.powercenter.savemode.PowerSaveActivity` | com.miui.powercenter.POWER_SAVE | (主进程) | - | 2004 |
| 37 | `com.miui.powercenter.wirelesscharge.WirelessChargingDialogActivity` | miui.intent.action.ACTIVITY_REVERSE_WIRELESS_CHG_DIALOG | (主进程) | - | 2038 |
| 38 | `com.miui.powercenter.autotask.AutoTaskManageActivity` | com.miui.powercenter.AUTO_TASK<br>miui.intent.action.AUTO_TASK | (主进程) | - | 2052 |
| 39 | `com.miui.permcenter.install.AdbInstallActivity` | com.miui.securitycenter.intent.action.INSTALL_PACKAGE | (主进程) | - | 2091 |
| 40 | `com.miui.antivirus.activity.MonitoredAppSettingsActivity` | miui.intent.action.SAFE_PAY_MONITOR_SETTINGS | (主进程) | - | 2171 |
| 41 | `com.miui.optimizemanage.OptimizemanageMainActivity` | miui.intent.action.OPTIMIZE_MANAGE | (主进程) | - | 2200 |
| 42 | `com.miui.googlebase.ui.GmsCoreSettings` | miui.intent.action.APP_SETTINGS | (主进程) | - | 2236 |
| 43 | `com.miui.applicationlock.LockChooseAccessControl` | com.miui.securitycenter.action.LOCKCHOOSEACCESSCONTROL | (主进程) | - | 2558 |
| 44 | `com.miui.applicationlock.ConfirmAccessControl` | miui.intent.action.CHECK_ACCESS_CONTROL | (主进程) | - | 2576 |
| 45 | `com.miui.applicationlock.FirstUseAppLockActivity` | com.miui.securitycenter.action.LOCK_APP | (主进程) | - | 2611 |
| 46 | `com.miui.applicationlock.TransitionHelper` | com.miui.securitycenter.action.TRANSITION | (主进程) | - | 2653 |
| 47 | `com.miui.networkassistant.ui.NetworkAssistantActivity` | miui.intent.action.NETWORKASSISTANT_ENTRANCE | (主进程) | - | 2670 |
| 48 | `com.miui.networkassistant.ui.activity.FirewallActivity` | miui.intent.action.NETWORKASSISTANT_FIREWALL | (主进程) | - | 2684 |
| 49 | `com.miui.networkassistant.ui.GetChargeTrafficActivity` | - | (主进程) | - | 2698 |
| 50 | `com.miui.networkassistant.ui.order.orderlist.BhOrderListActivity` | android.intent.action.VIEW | (主进程) | - | 2706 |
| 51 | `com.miui.networkassistant.ui.revert.RevertResultActivity` | - | (主进程) | - | 2724 |
| 52 | `com.miui.networkassistant.ui.order.orderdetail.BhNormalOrderDetailActivity` | - | (主进程) | - | 2731 |
| 53 | `com.miui.networkassistant.ui.GetPayStatusActivity` | android.intent.action.VIEW | (主进程) | - | 2739 |
| 54 | `com.miui.networkassistant.ui.base.UniversalFragmentActivity` | miui.intent.action.NETWORKASSISTANT_UNIVERSAL_FRAGMENT_ACTIVITY<br>miui.intent.action.NETWORKASSISTANT_APP_DETAIL<br>android.intent.action.VIEW<br>miui.intent.action.NETWORKASSISTANT_TC_DIAGNOSTIC<br>miui.intent.action.NETWORKASSISTANT_SMS_REPORT<br>miui.intent.action.BG_NETWORK_CONTROL | (主进程) | - | 2759 |
| 55 | `com.miui.networkassistant.ui.base.UniversalPreferenceActivity` | miui.intent.action.NETWORKASSISTANT_UNIVERSAL_PREFERENCE_ACTIVITY<br>miui.intent.action.NETWORKASSISTANT_SETTINGS<br>miui.intent.action.NETWORKASSISTANT_MONTH_PACKAGE_SETTING<br>miui.intent.action.NETWORKASSISTANT_AUTO_TRAFFIC_CORRECTION_SETTING<br>miui.intent.action.NETWORKASSISTANT_STATUS_BAR_SETTING<br>miui.intent.action.NETWORKASSISTANT_OPERATOR_SETTING<br>miui.intent.action.NETWORKASSISTANT_LIMIT_SETTING<br>miui.intent.action.NETWORKASSISTANT_SET_PACKAGE_GUIDE | (主进程) | - | 2806 |
| 56 | `com.miui.networkassistant.ui.activity.TrafficSortedActivity` | miui.intent.action.NETWORKASSISTANT_TRAFFIC_SORTED | (主进程) | - | 2868 |
| 57 | `com.miui.networkassistant.ui.activity.InternationalRoamingSettingActivity` | - | (主进程) | - | 2917 |
| 58 | `com.miui.networkassistant.ui.activity.TetheringStatsSettingActivity` | - | (主进程) | - | 2926 |
| 59 | `com.miui.wakepath.ui.ConfirmStartActivity` | android.app.action.CHECK_ALLOW_START_ACTIVITY | (主进程) | - | 3029 |
| 60 | `com.miui.luckymoney.ui.activity.LuckySettingActivity` | miui.intent.action.HB_MAIN_ACTIVITY<br>miui.intent.action.LUCKY_SETTING_ACTIVITY<br>android.intent.action.VIEW | (主进程) | - | 3048 |
| 61 | `com.miui.luckymoney.ui.activity.SecondarySettingActivity` | miui.intent.action.LUCKY_MORE_SETTING | (主进程) | - | 3092 |
| 62 | `com.miui.luckymoney.ui.activity.RemoveLuckyMoneyActivity` | miui.intent.action.LUCKY_ALARM_SETTING | (主进程) | - | 3106 |
| 63 | `com.miui.appmanager.AppManagerMainActivity` | miui.intent.action.APP_MANAGER | (主进程) | - | 3142 |
| 64 | `com.miui.appmanager.ApplicationsDetailsActivity` | miui.intent.action.APP_MANAGER_APPLICATION_DETAIL | (主进程) | - | 3157 |
| 65 | `com.miui.appmanager.AppManagerSettings` | - | (主进程) | - | 3171 |
| 66 | `com.miui.antispam.ui.activity.MainActivity` | miui.intent.action.SET_FIREWALL<br>android.intent.action.VIEW | (主进程) | - | 3196 |
| 67 | `com.miui.antispam.ui.activity.AddAntiSpamActivity` | miui.intent.action.ADD_FIREWALL | (主进程) | - | 3232 |
| 68 | `com.miui.antispam.ui.activity.RemoveBlacklistActivity` | miui.intent.action.REMOVE_BLACKLIST | (主进程) | - | 3246 |
| 69 | `com.miui.antispam.ui.activity.AntiSpamNewSettingsActivity` | com.miui.antispam.action.ANTISPAM_SETTINGS | (主进程) | - | 3259 |
| 70 | `com.miui.antispam.ui.activity.CallInterceptSettingsActivity` | miui.intent.action.CALL_FIREWALL | (主进程) | - | 3280 |
| 71 | `com.miui.antispam.ui.activity.BlackListActivity` | miui.intent.action.VIEW_BLACKLIST | (主进程) | - | 3323 |
| 72 | `com.miui.antispam.ui.activity.MarkNumGuideActivity` | - | (主进程) | - | 3345 |
| 73 | `com.miui.cleanmaster.CleanerInstallActivity` | com.miui.cleanmaster.InstallAndLunchCleanMaster<br>com.miui.cleaner.InstallAndLunchCleaner | (主进程) | - | 3363 |
| 74 | `com.miui.cleanmaster.LowMemoryIntentDispatchActivity` | com.miui.securitycenter.LunchCleanMaster<br>com.miui.securitycenter.LunchCleaner<br>com.miui.securitycenter.action.START_LOW_MEMORY_CLEAN | (主进程) | - | 3381 |
| 75 | `com.miui.appcompatibility.AppExcepitonTipsActivity` | com.miui.appcompatibility.LaunchDialog.launcher<br>com.miui.appcompatibility.LaunchDialog.appstore | (主进程) | - | 3406 |
| 76 | `com.miui.gamebooster.ui.WindowCallActivity` | com.miui.gamebooster.action.ACCESS_WINDOWCALLACTIVITY | (主进程) | - | 3423 |
| 77 | `com.miui.gamebooster.ui.WelcomActivity` | com.miui.gamebooster.action.ACCESS_MAINACTIVITY<br>com.miui.gamebooster.action.MI_PUSH_GAMEBOOSTER_HOT | (主进程) | - | 3492 |
| 78 | `com.miui.gamebooster.ui.GameBoosterMainActivity` | - | (主进程) | - | 3513 |
| 79 | `com.miui.gamebooster.ui.WonderfulMomentActivity` | com.miui.securitycenter.action.WONDERFUL_MOMENT | (主进程) | - | 3591 |
| 80 | `com.miui.gamebooster.ui.QuickReplySettingsActivity` | miui.intent.action.QUICK_REPLY_SETTINGS | (主进程) | - | 3679 |
| 81 | `com.miui.gamebooster.shoulderkey.ShoulderKeyLightEffectActivity` | com.miui.gamebooster.action.ACCESS_LIGHTEFFECT | (主进程) | - | 3702 |
| 82 | `com.miui.privacyapps.ui.PrivacyAppsManageActivity` | miui.intent.action.MANAGE_PRIVACY_APPS | (主进程) | - | 3737 |
| 83 | `com.miui.permcenter.permissions.SystemAppPermissionDialogActivity` | miui.intent.action.SYSTEM_PERMISSION_DECLARE<br>miui.intent.action.SYSTEM_PERMISSION_DECLARE_NEW<br>miui.intent.action.CTA_DECLARE<br>miui.intent.action.SYSTEM_PERMISSION_DECLARE_NO_OPTIONAL<br>miui.intent.action.SYSTEM_PERMISSION_DECLARE<br>miui.intent.action.SYSTEM_PERMISSION_DECLARE_NEW<br>miui.intent.action.CTA_DECLARE<br>miui.intent.action.SYSTEM_PERMISSION_DECLARE_NO_OPTIONAL | (主进程) | - | 3850 |
| 84 | `com.miui.superpower.SuperPowerSettings` | com.miui.powercenter.SUPERPOWER_SAVE<br>com.miui.powercenter.SUPERPOWER_SAVE_NEW | (主进程) | - | 3909 |
| 85 | `com.miui.earthquakewarning.ui.EarthquakeWarningEmergencyEditActivity` | - | (主进程) | - | 3998 |
| 86 | `com.miui.earthquakewarning.ui.EarthquakeWarningMonitorActivity` | - | (主进程) | - | 4052 |
| 87 | `com.miui.warningcenter.WarningCenterMainActivity` | miui.intent.action.WARNINGCENTER_MAIN | (主进程) | - | 4108 |
| 88 | `com.miui.warningcenter.mijia.WarningCenterMijiaActivity` | miui.intent.action.WARNINGCENTER_MIJIA | (主进程) | - | 4121 |
| 89 | `com.miui.permcenter.settings.PrivacyProvisionActivity` | - | (主进程) | - | 4155 |
| 90 | `com.miui.permcenter.privacycenter.PrivacySafetyActivity` | miui.intent.action.PRIVACY_SETTINGS<br>miui.intent.action.SECURITY_CENTER_SETTINGS<br>miui.intent.action.PRIVACY_SAFETY | (主进程) | - | 4162 |
| 91 | `com.miui.optimizecenter.storage.StorageActivity` | com.miui.cleanmaster.action.STORAGE_MANAGE<br>com.miui.securitycenter.action.STORAGE_MANAGE | (主进程) | - | 4194 |
| 92 | `com.miui.zman.ui.ShareActivity` | miui.intent.action.XMAN_SHARE_MANAGER | (主进程) | - | 4292 |
| 93 | `com.miui.zman.ui.LoadingActivity` | - | (主进程) | - | 4304 |
| 94 | `com.miui.warningcenter.disasterwarning.WarningCenterDisasterActivity` | miui.intent.action.WARNINGCENTER_DISASTER | (主进程) | - | 4370 |
| 95 | `com.miui.permcenter.detection.PrivacyRiskDetectionActivity` | miui.intent.action.PRIVACY_RISK_DETECTION_ACTIVITY | (主进程) | - | 4444 |
| 96 | `com.miui.permcenter.privacyblur.PrivacyThumbnailBlurSettings` | com.miui.securitycenter.action.PRIVACY_THUMBNAIL_BLUR_SETTING | (主进程) | - | 4467 |
| 97 | `com.miui.gamebooster.beauty.BeautySettingsActivity` | com.miui.gamebooster.action.ACCESS_FRONT_ASSISTANT | (主进程) | - | 4553 |
| 98 | `com.miui.powercenter.charge.ChargeFeatureActivity` | miui.intent.action.CHARGE_FEATURE_MANAGER<br>miui.intent.action.POWER_WIRELESS_REVERSE_LIST<br>com.miui.powercenter.FAST_CHARGE | (主进程) | - | 4587 |
| 99 | `com.miui.dock.settings.DockSettingsActivity` | com.miui.dock.ACTION_SETTINGS<br>com.miui.dock.ACTION_DOCK_SETTINGS<br>com.miui.gamebooster.action.ACCESS_BEAUTY<br>com.miui.gamebooster.action.ACCESS_CONVERSATION<br>com.miui.gamebooster.action.VIDEOBOX_SETTINGS<br>com.miui.gamebooster.action.VIDEOBOX_SETTINGS_ALL | (主进程) | - | 4612 |
| 100 | `com.miui.permcenter.WLANVerificationActivity` | miui.intent.action.OPEN_WIFI_VERIFY | (主进程) | - | 4700 |
| 101 | `com.miui.gamebooster.gamemode.GameModeSettingsActivity` | com.miui.gamebooster.action.ACCESS_BIGWINDOW | (主进程) | - | 4715 |
| 102 | `com.xiaomi.mipush.sdk.NotificationClickedActivity` | - | (主进程) | - | 4729 |
| 103 | `com.miui.gamebooster.beauty.PrivacyCameraGlobalSettingsActivity` | - | (主进程) | - | 4806 |
| 104 | `com.miui.idprovider.ui.GrantPermissionActivity` | miui.intent.action.REQUEST_PERMISSIONS_FOR_OAID | (主进程) | - | 4858 |
| 105 | `com.miui.bubbles.settings.BubbleAppManageActivity` | - | :ui | - | 4940 |
| 106 | `com.miui.bubbles.settings.BubbleSettingsActivity` | com.miui.bubbles.BUBBLES_SETTINGS | :ui | - | 4951 |

##### A.5 activity-alias (2)

| # | 组件 | action / filter | 进程 | authorities | L |
|---|---|---|---|---|---|
| 1 | `com.miui.permcenter.privacymanager.behaviorrecord.PrivacyDetailActivity` | - | (主进程) | - | 1352 |
| 2 | `com.miui.permcenter.settings.PrivacySettingsActivity` | - | (主进程) | - | 4177 |

#### B. exported 且**有 permission 保护**

##### B.1 provider (18)

| # | 组件 | 保护权限 | action / filter | 进程 | L |
|---|---|---|---|---|---|
| 1 | `com.miui.autotask.provider.AutoTaskProvider` | com.miui.autotask.permission.CONTENTPROVIDER | - | SC.remote | 1219 |
| 2 | `com.miui.permcenter.privacymanager.behaviorrecord.PackageBehaviorContentProvider` | com.miui.securitycenter.permission.READ_APP_BEHAVIORS | - | (主进程) | 1380 |
| 3 | `com.miui.monthreport.MonthReportProvider` | com.miui.monthreport.permission.DATA_MONTH_REPORT | - | SC.remote | 2467 |
| 4 | `com.miui.securitycenter.provider.SecurityCenterProvider` | com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER | - | SC.remote | 2474 |
| 5 | `com.miui.common.persistence.RemoteProvider` | com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER | - | SC.remote | 2487 |
| 6 | `com.miui.powercenter.provider.PowerCenterProvider` | com.miui.powercenter.permission.POWER_DATA | - | SC.remote | 2494 |
| 7 | `com.miui.networkassistant.provider.NetworkAssistantProvider` | miui.permission.EXTRA_NETWORK | - | SC.remote | 2985 |
| 8 | `com.miui.powercenter.powersaver.PowerSaverProvider` | com.miui.powercenter.permission.POWER_COMMAND | - | SC.remote | 3018 |
| 9 | `com.miui.luckymoney.provider.LuckyMoneyProvider` | miui.permission.EXTRA_NETWORK | - | SC.remote | 3134 |
| 10 | `com.miui.antispam.db.AntiSpamProvider` | com.miui.antispam.permission.CONTENTPROVIDER | - | SC.remote | 3358 |
| 11 | `com.miui.gamebooster.provider.GameBoosterProvider` | com.miui.securitycenter.permission.GAMEBOOSTER_PROVIDER | - | SC.remote | 3715 |
| 12 | `com.miui.securitycenter.provider.ThirdMonitorProvider` | com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER | - | SC.remote | 3841 |
| 13 | `com.miui.zman.ZmanProvider` | com.miui.zman.permission.SECURITY_SHARE | - | (主进程) | 4363 |
| 14 | `com.miui.securitycenter.WidgetProvider` | com.miui.securitycenter.permission.WIDGET_PROVIDER | - | SC.remote | 4518 |
| 15 | `com.miui.globalsatisfaction.provider.GsProvider` | com.miui.globalsatisfaction.permission.PROVIDER | miui.intent.action.SETTINGS_NOTIFICATION_PROVIDER | SC.remote | 4740 |
| 16 | `com.miui.permcenter.permissions.acrossterminal.AcrossTerminalPermissionContentProvider` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | - | SC.remote | 4757 |
| 17 | `com.miui.privacyapps.PrivacyAppsProvider` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | - | (主进程) | 4925 |
| 18 | `com.miui.riskcontrol.SensitiveWordDetectProvider` | com.miui.security.permission.DETECT_TEXT | - | (主进程) | 4933 |

##### B.2 service (19)

| # | 组件 | 保护权限 | action / filter | 进程 | L |
|---|---|---|---|---|---|
| 1 | `com.miui.apppredict.service.AiService` | miui.permission.APP_PREDICT_VERIFY | - | SC.remote | 725 |
| 2 | `com.miui.bubbles.services.BubblesNotificationListenerService` | android.permission.BIND_NOTIFICATION_LISTENER_SERVICE | android.service.notification.NotificationListenerService | SC.remote | 1178 |
| 3 | `com.miui.tvm.service.TvmManagerService` | miui.permission.TVM | miui.intent.action.START_TVM | (主进程) | 1264 |
| 4 | `com.miui.securitycenter.memory.MemoryCheckService` | miui.permission.MEMORY_CHECK | miui.intent.action.MEMORY_CHECK_SERVICE | SC.remote | 2253 |
| 5 | `com.miui.securitycenter.cloudbackup.SettingsCloudBackupService` | com.xiaomi.permission.CLOUD_MANAGER | miui.action.CLOUD_BACKUP_SETTINGS<br>miui.action.CLOUD_RESTORE_SETTINGS | SC.remote | 2280 |
| 6 | `com.miui.gamebooster.service.GameBoosterService` | com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER | com.miui.gamebooster.service.GameBoosterServices | SC.remote | 2323 |
| 7 | `com.miui.gamebooster.service.VideoToolBoxService` | com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER | com.miui.gamebooster.videobox.services.VideoToolBoxService | SC.remote | 2347 |
| 8 | `com.miui.gamebooster.service.DockWindowManagerService` | com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER | com.miui.gamebooster.service.GameBoxService | :ui | 2357 |
| 9 | `com.miui.gamebooster.gbservices.AntiMsgAccessibilityService` | android.permission.BIND_ACCESSIBILITY_SERVICE | android.accessibilityservice.AccessibilityService | :ui | 2373 |
| 10 | `com.miui.gamebooster.service.NotificationListener` | android.permission.BIND_NOTIFICATION_LISTENER_SERVICE | - | SC.remote | 2386 |
| 11 | `com.miui.securitycenter.service.NotificationLinkService` | com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER | miui.action.service.NOTIFICTION_LINKAGE | SC.remote | 2398 |
| 12 | `com.miui.antispam.service.AntiSpamService` | com.miui.antispam.permission.DATA_MONTH_REPORT | - | SC.remote | 3351 |
| 13 | `com.miui.securitycenter.service.CloudThirdDesktopService` | com.miui.antispam.permission.DATA_MONTH_REPORT | - | SC.remote | 3823 |
| 14 | `com.miui.superpower.notification.SuperPowerTileService` | android.permission.BIND_QUICK_SETTINGS_TILE | android.service.quicksettings.action.QS_TILE | SC.remote | 3930 |
| 15 | `com.miui.cleanmaster.CleanerHandleService` | com.miui.securitycenter.permission.CLEANER_MANAGE | miui.intent.action.CREATE_CM_SHORTCUT<br>miui.intent.action.CREATE_CLEANER_SHORTCUT | SC.remote | 3969 |
| 16 | `com.miui.powercenter.powersaver.PerformanceModeTileService` | android.permission.BIND_QUICK_SETTINGS_TILE | android.service.quicksettings.action.QS_TILE | SC.remote | 4825 |
| 17 | `com.miui.powercenter.powersaver.PerformanceWildModeTileService` | android.permission.BIND_QUICK_SETTINGS_TILE | android.service.quicksettings.action.QS_TILE | SC.remote | 4838 |
| 18 | `com.miui.securitycenter.service.NotificationService` | com.miui.securitycenter.permission.NOTIFICATION_SERVICE | com.miui.securitycenter.action.NOTIFICATION_SERVICE | SC.remote | 4913 |
| 19 | `com.xiaomi.mipush.sdk.PushMessageHandler` | com.xiaomi.xmsf.permission.MIPUSH_RECEIVE | - | (主进程) | 5089 |

##### B.3 receiver (8)

| # | 组件 | 保护权限 | action / filter | 进程 | L |
|---|---|---|---|---|---|
| 1 | `com.miui.antifraud.AntiFraudReceiver` | android.permission.MODIFY_PHONE_STATE | android.intent.action.PHONE_STATE<br>miui.action.mms.SCAN_RISKY_URL | SC.remote | 1130 |
| 2 | `com.miui.push.XiaomiPushReceiver` | com.xiaomi.xmsf.permission.MIPUSH_RECEIVE | com.xiaomi.mipush.RECEIVE_MESSAGE<br>com.xiaomi.mipush.MESSAGE_ARRIVED<br>com.xiaomi.mipush.ERROR | SC.remote | 1278 |
| 3 | `com.miui.permcenter.install.PackageVerificationReceiver` | android.permission.INSTALL_PACKAGES | com.miui.action.PACKAGE_NEEDS_VERIFICATION | SC.remote | 2109 |
| 4 | `com.miui.securitycenter.receiver.LauncherLoadingFinishedReceiver` | miui.permission.USE_INTERNAL_GENERAL_API | com.miui.home.intent.action.LOADING_FINISHED | SC.remote | 2446 |
| 5 | `com.miui.antivirus.receiver.UpdaterReceiver` | com.android.updater.permission.RECEIVE_BROADCAST | com.android.updater.action.UPDATE_SUCCESSED | SC.remote | 2456 |
| 6 | `com.miui.securityscan.cards.DownloadInstallReceiver` | miui.permission.USE_INTERNAL_GENERAL_API | com.xiaomi.market.DOWNLOAD_INSTALL_RESULT | (主进程) | 2999 |
| 7 | `com.miui.analytics.RemoteAnalyticsReceiver` | com.miui.securitycenter.permission.REMOTE_ANALYTICS | com.miui.security.remote.analytics | SC.remote | 3940 |
| 8 | `androidx.profileinstaller.ProfileInstallReceiver` | android.permission.DUMP | androidx.profileinstaller.action.INSTALL_PROFILE<br>androidx.profileinstaller.action.SKIP_FILE<br>androidx.profileinstaller.action.SAVE_PROFILE<br>androidx.profileinstaller.action.BENCHMARK_OPERATION | (主进程) | 5105 |

##### B.4 activity (21)

| # | 组件 | 保护权限 | action / filter | 进程 | L |
|---|---|---|---|---|---|
| 1 | `com.miui.permcenter.settings.ShutDownPasswordActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | miui.intent.action.SHUT_DOWN_PASSWORD_ACTIVITY | (主进程) | 708 |
| 2 | `com.miui.simlock.activity.SimLockStartActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | miui.intent.action.SIM_LOCK_START | (主进程) | 982 |
| 3 | `com.miui.permcenter.ctaverify.SendSmsVerificationActivity` | android.permission.BROADCAST_SMS | miui.intent.action.SEND_SMS_VERIFY | (主进程) | 1148 |
| 4 | `com.miui.permcenter.NotificationVerifyActivity` | miui.permission.NOTIFICATION_VERIFY | miui.intent.action.NOTIFICATION_VERIFY | (主进程) | 1192 |
| 5 | `com.miui.permcenter.AccessibilityServiceActivity` | miui.permission.ACCESSIBILITY_SERVICE | miui.intent.action.ACCESSIBILITY_SERVICE | (主进程) | 1424 |
| 6 | `com.miui.devicepermission.editpermission.DevicePermissionSettingsActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | miui.intent.action.DEVICE_PERMISSION_SETTINGS | (主进程) | 1711 |
| 7 | `com.miui.devicepermission.editpermission.DevicePermissionEditorActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | miui.intent.action.EDITOR_DEVICE_PERMISSIONS | (主进程) | 1734 |
| 8 | `com.miui.devicepermission.grantpermission.GrantDevicePermissionActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | miui.intent.action.REQUEST_DEVICE_PERMISSIONS | (主进程) | 1749 |
| 9 | `com.miui.permcenter.SettingsAcitivty` | android.permission.CHANGE_COMPONENT_ENABLED_STATE | com.miui.securitycenter.action.PERMISSION_SETTINGS | (主进程) | 1856 |
| 10 | `com.miui.gamebooster.ui.SettingsActivity` | com.miui.gamebooster.access.ACTIVITY | com.miui.gamebooster.action.GLOBAL_SETTINGS<br>android.intent.action.VIEW | (主进程) | 3626 |
| 11 | `com.miui.gamebooster.ui.SelectGameLandActivity` | com.miui.gamebooster.access.ACTIVITY | com.miui.gamebooster.action.SELECT_GAME | (主进程) | 3654 |
| 12 | `com.miui.privacyapps.ui.PrivacyAppsHelper` | miui.permission.USE_INTERNAL_GENERAL_API | miui.intent.action.PRIVACY_APPS_HELPER | (主进程) | 3773 |
| 13 | `com.miui.permcenter.permissions.AppPermissionsUseActivity` | com.miui.securitycenter.permission.PERM_USE_INFO | miui.intent.action.APP_PERMISSION_USE | (主进程) | 3795 |
| 14 | `com.miui.securitycenter.activity.ThirdDesktopAlertActivity` | com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER | - | (主进程) | 3829 |
| 15 | `com.miui.optimizecenter.storage.FboResultActivity` | miui.permission.USE_INTERNAL_GENERAL_API | miui.intent.action.FBO_RESULT | (主进程) | 4215 |
| 16 | `com.miui.zman.ui.PrivacyProtectSettingsActivity` | com.miui.zman.permission.SECURITY_SHARE | miui.intent.action.ZMAN_PRIVACY_PROTECT_SETTING | (主进程) | 4316 |
| 17 | `com.miui.zman.ui.ShareSettingActivity` | com.miui.zman.permission.SECURITY_SHARE | miui.intent.action.ZMAN_SECURITY_SHARE_SETTING | (主进程) | 4330 |
| 18 | `com.miui.zman.ui.PrivacyProtectTipSettingsActivity` | com.miui.zman.permission.SECURITY_SHARE | miui.intent.action.ZMAN_PRIVACY_PROTECT_TIP_SETTING | (主进程) | 4343 |
| 19 | `com.miui.permcenter.settings.TileTransitActivity` | android.permission.BIND_QUICK_SETTINGS_TILE | android.service.quicksettings.action.QS_TILE_PREFERENCES | (主进程) | 4500 |
| 20 | `com.miui.permcenter.settings.OtherPermissionsActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | - | (主进程) | 4579 |
| 21 | `com.miui.combinepermission.grantpermission.CombinePermissionActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | miui.intent.action.REQUEST_PERMISSIONS_COMBINE<br>miui.intent.action.CTA_REQUEST_PERMISSIONS_COMBINE | (主进程) | 4875 |

#### C. 非导出但重要的组件（摘选）

| kind | 组件 | 进程 | 说明 | L |
|---|---|---|---|---|
| activity | `com.miui.gamebooster.ui.GameBoosterRealMainActivity` | (主进程) | 仅内部启动 | 3536 |
| provider | `com.miui.apppredict.db.WidgetBlackListContentProvider` | (主进程) | authority=com.miui.securitycenter.widget.black.list.provider | 730 |
| provider | `com.miui.powercenter.autotask.AutoTaskProvider` | SC.remote | authority=com.miui.powercenter.autotask | 2501 |
| provider | `com.miui.powercenter.batteryhistory.BatteryHistoryProvider` | (主进程) | authority=com.miui.powercenter.batteryhistory | 2512 |
| provider | `androidx.core.content.FileProvider` | SC.remote | authority=com.miui.networkassistant.fileprovider grantUri=true | 2992 |
| provider | `com.miui.earthquakewarning.db.EarthquakeContentProvider` | SC.remote | authority=com.miui.earthquakewarning.EarthquakeContentProvider | 4039 |
| provider | `com.miui.earthquakewarning.db.EarthquakeContentProvider2` | SC.remote | authority=com.miui.earthquakewarning.EarthquakeContentProvider2 | 4045 |
| provider | `com.miui.zman.ui.ZmanFileProvider` | (主进程) | authority=com.miui.securitycenter.zman.fileProvider grantUri=true | 4355 |
| provider | `com.miui.warningcenter.disasterwarning.db.DisasterContentProvider` | SC.remote | authority=com.miui.warningcenter.DisasterContentProvider | 4421 |
| provider | `com.miui.warningcenter.disasterwarning.db.DisasterAreaProvider` | SC.remote | authority=com.miui.warningcenter.DisasterAreaProvider | 4479 |
| provider | `com.miui.warningcenter.disasterwarning.db.DisasterSubscribeProvider` | SC.remote | authority=com.miui.warningcenter.DisasterSubscribeProvider | 4485 |
| provider | `androidx.startup.InitializationProvider` | (主进程) | authority=com.miui.securitycenter.androidx-startup | 5067 |
| provider | `com.xiaomi.continuity.InitializationProvider` | (主进程) | authority=com.miui.securitycenter.continuity-startup | 5134 |

### 4.3 `<provider>` authorities 全表（40 个，无重复 authority）【观测】

| authority | 组件 | exported | 保护权限 | grantUri | 进程 |
|---|---|---|---|---|---|
| `com.miui.securitycenter.widget.black.list.provider` | `com.miui.apppredict.db.WidgetBlackListContentProvider` | false | — | — | 主 |
| `com.miui.autotask.provider.auto_task` | `com.miui.autotask.provider.AutoTaskProvider` | true | `com.miui.autotask.permission.CONTENTPROVIDER` (0x3) | — | remote |
| `com.miui.autotask.provider.auto_task_service` | `AutoTaskServiceProvider` | **true** | **无** | — | remote |
| `com.miui.permcenter.privacycenter` | `PrivacyCenterProvider` | **true** | **无** | — | remote |
| `com.miui.securitycenter.appbehaviors` | `PackageBehaviorContentProvider` | true | `...permission.READ_APP_BEHAVIORS` (0x3) | — | 主 |
| `com.miui.monthreport` | `MonthReportProvider` | true | `com.miui.monthreport.permission.DATA_MONTH_REPORT` | — | remote |
| `com.miui.securitycenter.provider` | `SecurityCenterProvider` | true | `...ACCESS_SECURITY_CENTER_PROVIDER` (0x3) | — | remote |
| `com.miui.securitycenter.credentialprovider` | **`DeviceCredentialProvider`** | **true** | **无** | — | remote |
| `com.miui.securitycenter.remoteprovider` | `RemoteProvider` | true | `...ACCESS_SECURITY_CENTER_PROVIDER` | — | remote |
| `com.miui.powercenter.provider` | `PowerCenterProvider` | true | `com.miui.powercenter.permission.POWER_DATA` | — | remote |
| `com.miui.powercenter.autotask` | `AutoTaskProvider`(powercenter) | false | — | — | remote |
| **`com.miui.idprovider`** | `IdProvider`（OAID） | **true** | **无** | — | remote |
| `com.miui.powercenter.batteryhistory` | `BatteryHistoryProvider` | false | — | — | 主 |
| `com.miui.networkassistant.provider` | `NetworkAssistantProvider` | true | `miui.permission.EXTRA_NETWORK` (0x3) | — | remote |
| `com.miui.networkassistant.fileprovider` | `androidx.core.content.FileProvider` | false | — | **true** | remote |
| `com.miui.powercenter.powersaver` | `PowerSaverProvider` | true | `...permission.POWER_COMMAND` | — | remote |
| `com.miui.luckymoney.provider` | `LuckyMoneyProvider` | true | `miui.permission.EXTRA_NETWORK` | — | remote |
| **`antispam`** | `AntiSpamProvider` | true | `com.miui.antispam.permission.CONTENTPROVIDER` (0x3) | — | remote |
| `com.miui.securitycenter.gamebooster` | `GameBoosterProvider` | true | `...GAMEBOOSTER_PROVIDER` (0x3) | — | remote |
| **`com.miui.sec.INSTALL_PACKAGE`** | **`com.miui.sec.external.InstallProvider`** | **true** | **无** | — | 主 |
| `com.miui.securitycenter.provider.SecSettingsSearchProvider` | 同名 | **true** | **无** | **true** | 主 |
| `com.miui.sec.THIRD_DESKTOP` | `ThirdMonitorProvider` | true | `...ACCESS_SECURITY_CENTER_PROVIDER` | — | remote |
| `com.miui.earthquakewarning.EarthquakeContentProvider` | 同名 | false | — | — | remote |
| `com.miui.earthquakewarning.EarthquakeContentProvider2` | 同名2 | false | — | — | remote |
| **`com.miui.input.provider`** | `com.miui.securityadd.input.InputProvider` | **true** | **无** | — | remote |
| `com.miui.warningcenter.provider` | `WarningCenterProvider` | **true** | **无** | — | remote |
| `com.miui.securitycenter.zman.fileProvider` | `ZmanFileProvider` | false | — | **true** | 主 |
| `com.miui.securitycenter.zman.provider` | `ZmanProvider` | true | `com.miui.zman.permission.SECURITY_SHARE` | — | 主 |
| `com.miui.warningcenter.DisasterContentProvider` | 同名 | false | — | — | remote |
| `com.miui.warningcenter.DisasterAreaProvider` | 同名 | false | — | — | remote |
| `com.miui.warningcenter.DisasterSubscribeProvider` | 同名 | false | — | — | remote |
| `com.miui.securitycenter.splashAd` | `SystemAdContentProvider` | **true** | **无** | — | remote |
| `com.miui.securitycenter.widgetProvider` | `WidgetProvider` | true | `...WIDGET_PROVIDER` (0x3) | — | remote |
| **`com.xiaomi.account.provider.XiaomiAccountSettingsNotificationProvider`** | `com.miui.globalsatisfaction.provider.GsProvider` | true | `com.miui.globalsatisfaction.permission.PROVIDER` (0x3) | **true** | remote |
| `com.miui.permissions.acrossterminal` | `AcrossTerminalPermissionContentProvider` | true | `...SYSTEM_PERMISSION_DECLARE` (0x3) | — | remote |
| `com.miui.securitycenter.app.hide` | `PrivacyAppsProvider` | true | `...SYSTEM_PERMISSION_DECLARE` | — | 主 |
| `com.miui.riskcontrol` | `SensitiveWordDetectProvider` | true | `com.miui.security.permission.DETECT_TEXT` (0x3) | — | 主 |
| **`com.xiaomi.security.xsof.provider`** | `XSOFProvider` | **true** | **无** | — | remote |
| `com.miui.securitycenter.androidx-startup` | `androidx.startup.InitializationProvider` | false | — | — | 主 |
| `com.miui.securitycenter.continuity-startup` | `com.xiaomi.continuity.InitializationProvider` | false | — | — | 主 |

**授权过 URI 的只有 4 个**（decoded.xml L2992 / L3813 / L4355 / L4740）：`androidx.core.content.FileProvider`（networkassistant.fileprovider，exported=false）、`SecSettingsSearchProvider`（**exported=true 且无权限**）、`ZmanFileProvider`（exported=false）、`GsProvider`（exported=true，有 0x3 权限）。**`SecSettingsSearchProvider` 是唯一"exported + 无权限 + grantUriPermissions=true"的 provider，是本轮最需要实证的一个点（见 §8/§9）。**

**authority 命名空间冒用**【观测】：`com.xiaomi.account.provider.XiaomiAccountSettingsNotificationProvider`、`com.miui.sec.INSTALL_PACKAGE`、`com.miui.input.provider`、`com.miui.permissions.acrossterminal`、`com.xiaomi.security.xsof.provider` 都落在**别的系统应用的包名前缀**下。【推断】这是 MIUI 内部的"权威命名惯例"（把能力挂到概念宿主名下），不是 squid；但凡外部应用按 authority 做信任判断，就会误判来源。

### 4.4 `<meta-data>`：application 级 82 条 + 组件级 60 条

**没有一条是 SDK 的 appid/appkey/secret**”——与 GuardProvider 的 `AVL_OPENSDK_APPKEY=D5DFB878...` 形成对比。【观测】

application 级 meta-data 中**有信息量的**（其余是本 APK 的功能开关/版本号）：

| key | value | 含义（推断） | L |
|---|---|---|---|
| `sc_version` | `1de6cc0dd33cc321d8ca9e9ad4143829e5f3a187` | 构建 commit（40 hex，SHA-1 形态） | 4651 |
| `flavor_region` | `cn` | 中国版渠道 | 4649 |
| `miui_private_label` | `PRIVATE_V150` | MIUI 私有分支标签 | 4657 |
| `os_private_label` | `PRIVATE_OS020` | OS 私有分支标签 | 4659 |
| `market_sdk_version` | `3.2.26` | 小米应用商店 SDK 版本 | 5002 |
| `MIPUSH_SDK_VERSION_CODE` / `_NAME` | `50708` / `5_7_8-C` | MiPush 版本 | 5081 / 5083 |
| `com.xiaomi.continuity.sdk.VERSION_CODE` / `_NAME` | `3001561` / `3.1.561.10.1201105` | 互联互通 SDK 版本 | 5127 / 5129 |
| `com.xiaomi.security.xsof.service.version` / `required.client.version` | `1` / `1` | XSOF 接口契约版本 | 4981 / 4983 |
| `game_turbo_api_version` / `auto_task_api_version` / `miuiWidgetVersion` / `support_game_turbo_func_flags` | `2` / `1` / `4` / `3` | 与系统侧的能力协商版本 | 574/580/1452/2528 |
| `app_hide_enable` / `disable_notifications_when_app_hide` | `true` / `false` | 隐藏应用开关 | 482/484 |
| `miui.support_full_scan` / `securitycenter.support.mix.scan` | `true` / `true` | 扫描模式 | 490 / 1448 |
| `miui.supportMultiUserPermission` / `supportCombinePermission` / `miui.supportSmartAutoStart` | `true`×3 | 多用户/组合权限/智能自启 | 572/576/1241 |
| `security.miui.support.text.detect` | `true` | 敏感文本检测开关（对应 `SensitiveWordDetectProvider`） | 4927 |
| `xunyou_voice_remind` | `1` | 迅游加速器联动 | 4655 |
| `miui.focusNotificationVersion` / `miui.lessLocationTip` / `miui.dynamic_dpi_relaunch` | `1`/`1`/`true` | 系统特性协商 | 4661/4671/4653 |
| `com.miui.maml.sdk` + 20+ 条 miuix·组件库版本（`miuixpreference`、`appcompat`、`bottomsheet`、`folme`、`haptic`、`pinyin`…） | 版本号 | MIUIX UI 库版本清单 | 5004–5125 |

组件级 meta-data 全部是 MAML 卡片/小组件/无障碍配置（`miuiWidget*`、`android.appwidget.provider`、`android.accessibilityservice`、`android.support.FILE_PROVIDER_PATHS`、`androidx.startup`），**无密钥**。【观测】

### 4.5 `<uses-library>`（8，全部 `required=false`）【观测】

L582 `vendor.xiaomi.hardware.misecurity-V1.0-java`、L1434 `micloud-sdk`、L1436 `android-support-v7-recyclerview.jar`、L1438 `protobuf.jar`、L1440 `org.apache.http.legacy`、L1442 `com.miui.system`、L5077 `androidx.window.extensions`、L5079 `androidx.window.sidecar`。

### 4.6 `<queries>`（package visibility）【观测】

decoded.xml **L463–L468**，只有 2 条：
```xml
<queries>
    <package android:name="com.xiaomi.migameservice">
    <package android:name="com.xiaomi.mi_connect_service">
</queries>
```
注意：manifest 中**没有** `android.permission.QUERY_ALL_PACKAGES`。【推断】它靠 `sharedUserId=android.uid.system` 天然可见全部包，`<queries>` 只是给 targetSdk 30+ 的形式化声明。

### 4.7 `<protected-broadcast>`：28 条（decoded.xml L143–L197）【观测】

`com.miui.action.NETWORK_POLICY_UPDATE`、`com.miui.action.REFRESH_DATA_USAGE_DAILY`、`com.miui.action.DATA_USAGE_AUTO_CORRECTION`、`action_update_sc_network_allow`、`action_broadcast_tc_diagnostic_status`、`action_broadcast_cancel_notification`、`action_broadcast_tc_sms_report_status`、`sms_receiver_action`、`action_broadcast_allow_app_firewall`、`com.miui.securitycenter.action.UPDATE_NOTIFICATION`、`com.miui.securitycenter.action.CLEAR_MEMORY`、`com.miui.securitycenter.action.TRACK_EVERY_WEEK`、`com.miui.antispam.SMS_AUTO_UPDATE`、`com.miui.action.appcompatibility.update`、`com.miui.securitycenter.action.TRACK_EVERY_DAY`、`miui.intent.action.config_changed`、`config_changed_flag`、`show_float_window`、`remove_float_window`、`shake_send_sticker`、`show_float_window_button`、`action_refresh_data_usage_daily`、`com.miui.luckymoney.ACTION_UPDATE_TIPS_CONFIG`、`com.miui.luckymoney.ACTION_UPDATE_ALARM_CONFIG`、`com.miui.luckymoney.ACTION_LUCKY_ALARM`、`com.miui.cleanmaster.action.UPDATE_SHORTCUT`、`com.xiaomi.security.SUPER_POWER_MODE_EXIT`、`com.miui.securitycenter.extreme.endurance.shutdown`。

【推断】`protected-broadcast` 的语义是"只有同 UID 或系统可发"——它把这些广播**变成了只有系统能伪造的内部信号**，是这套设计里少见的正向加固。

---

## 5. 第三方 / 自研 SDK 清单（按 dex `class_defs` 精确统计）

数据源：`_analysis\tools\classes.classlist.json`（25009 个 `class_defs`），脚本 `pkg_hist.js` / `gen_tables2.js`。
DEX 记法：`D1`=`classes.dex`，`D2`=`classes2.dex`，`D3`=`classes3.dex`。

| # | 顶层包 | 类数 | DEX 分布 | 归属 | 一句话作用 |
|---|---|---|---|---|---|
| 1 | `com.google.android.exoplayer2` | 2031 | D1:2031 | 第三方开源 | ExoPlayer 2 — 媒体播放（游戏视频/录屏回放） |
| 2 | `com.google.common` | 1995 | D1:1995 | 第三方开源 | Guava（collect/util/base/graph/cache/hash/io/reflect/primitives/math/eventbus/net） |
| 3 | `com.google.android.material` | 884 | D1:884 | 第三方开源 | Material Components |
| 4 | `com.google.gson` | 217 | D2:217 | 第三方开源 | Gson JSON |
| 5 | `com.google.protobuf` | 246 | D2:246 | 第三方开源 | protobuf-java（对应 assets/protobuf/*.proto） |
| 6 | `com.google.android.flexbox` | 37 | D1:37 | 第三方开源 | FlexboxLayout |
| 7 | `androidx` | 2167 | D1:2167 | 第三方开源 | AndroidX（core/appcompat/recyclerview/fragment/lifecycle/preference/constraintlayout/transition/annotation/window/emoji2/room/profileinstaller…） |
| 8 | `miuix` | 1504 | D3:1504 | 小米自研框架 | MIUIX 自研 UI 框架（appcompat/animation/preference/popupwidget/bottomsheet…） |
| 9 | `miui.cloud` | 105 | D3:105 | 第三方 SDK | 小米云服务 SDK（uses-library micloud-sdk） |
| 10 | `com.xiaomi.continuity` | 486 | D3:486 | 第三方 SDK | 小米互联互通 SDK（libmicontinuity_sdk.so；META-INF 版本 3.1.561.10.1201105 / 3001561） |
| 11 | `com.xiaomi.onetrack` | 251 | D3:251 | 第三方 SDK | ONETrack 埋点/统计 SDK（pi.b = DexAnalytics，可 DexClassLoader 加载外部 APK 内 com.miui.analytics.Analytics） |
| 12 | `com.xiaomi.push` | 166 | D3:166 | 第三方 SDK | MiPush 推送客户端（libtcp.so；meta MIPUSH_SDK_VERSION_NAME=5_7_8-C / 50708） |
| 13 | `com.xiaomi.mipush` | 82 | D3:82 | 第三方 SDK | MiPush 消息/SDK 通道 |
| 14 | `com.miui.maml` | 626 | D2:626 | 小米自研框架 | MAML 动态卡片/主题渲染框架（assets/maml、com.miui.maml.sdk=true） |
| 15 | `com.miui.hybrid` | 64 | D2:64 | 小米自研框架 | H5 混合容器 |
| 16 | `com.market.sdk` | 73 | D2:73 | 第三方 SDK | 小米应用商店 SDK（meta market_sdk_version=3.2.26） |
| 17 | `com.market.pm` | 6 | D2:6 | 第三方 SDK | 商店包管理联动 |
| 18 | `com.milink.api` | 67 | D2:67 | 第三方 SDK | 小米互传 MiLink API 接口层 |
| 19 | `com.milink.sdk` | 58 | D2:58 | 第三方 SDK | MiLink SDK 实现 |
| 20 | `com.xiaomi.accountsdk` | 61 | D3:61 | 第三方 SDK | 小米账号 SDK |
| 21 | `com.xiaomi.passport` | 22 | D3:22 | 第三方 SDK | Passport 登录 |
| 22 | `com.xiaomi.analytics` | 18 | D3:18 | 第三方 SDK | 小米统计 SDK |
| 23 | `com.miui.analytics` | 20 | D2:20 | 小米 SDK | 小米统计（DexAnalytics 动态加载目标） |
| 24 | `com.qti` | 139 | D3:139 | 第三方 SDK | Qualcomm IZAT / GNSS / FLP 定位（debugreport/geofence/wwandb/wifidb…） |
| 25 | `com.xiaomi.gnss` | 43 | D3:43 | 第三方 SDK | GNSS 定位增强 |
| 26 | `org.tensorflow` | 14 | D3:14 | 第三方开源 | TensorFlow Lite Java 绑定（libtensorflowlite_jni.so） |
| 27 | `com.airbnb.lottie` | 52 | D1:52 | 第三方开源 | Lottie 动画 |
| 28 | `com.davemorrissey.labs` | 20 | D1:20 | 第三方开源 | subsampling-scale-image-view 大图浏览 |
| 29 | `xcrash` | 28 | D3:28 | 第三方开源 | 爱奇艺 xCrash 崩溃捕获（libxcrash.so / libxcrash_dumper.so） |
| 30 | `okhttp3` | 2 | D3:2 | 第三方开源（残留） | OkHttp（仅剩 2 个残留类：EverythingIsNonNull、PublicSuffixDatabase） |
| 31 | `kotlin` | 87 | D3:87 | 第三方开源 | Kotlin stdlib（KGP 1.8.22，kotlin-tooling-metadata.json） |
| 32 | `kotlinx.coroutines` | 139 | D3:139 | 第三方开源 | 协程库 |
| 33 | `vendor.xiaomi` | 45 | D3:45 | 系统接口绑定 | HIDL/AIDL 生成接口（uses-library vendor.xiaomi.hardware.misecurity-V1.0-java） |
| 34 | `com.xiaomi.security` | 27 | D3:27 | 小米 SDK | 小米安全公共库（XSOF） |
| 35 | `com.xiaomi.bspsecurity` | 4 | D3:4 | 小米 SDK | BSP 安全 |
| 36 | `com.xiaomi.trustservice` | 4 | D3:4 | 小米 SDK | 可信服务 |
| 37 | `com.xiaomi.micloudsdk` | 14 | D3:14 | 第三方 SDK | 小米云 SDK |
| 38 | `com.xiaomi.micloudkeybag` | 9 | D3:9 | 第三方 SDK | 云 KeyBag |
| 39 | `com.xiaomi.joyose` | 30 | D3:30 | 第三方 SDK | 系统性能调度 Joyose |
| 40 | `com.xiaomi.ad` | 9 | D3:9 | 第三方 SDK | 小米广告 |
| 41 | `com.miui.msa` | 1 | D2:1 | 小米广告 SDK | MSA 广告标识 |
| 42 | `com.miui.systemAdSolution` | 12 | D3:12 | 小米广告 SDK | 系统广告方案（开屏 ad provider） |
| 43 | `com.miui.blur` | 29 | D2:29 | 小米自研库 | 高斯模糊库 |
| 44 | `com.miui.fastplayer` | 8 | D2:8 | 小米自研库 | 快速播放器（libfastplayer.so） |
| 45 | `com.miui.mlkit` | 5 | D2:5 | 小米自研 | ML Kit |
| 46 | `com.miui.sdk` | 22 | D3:22 | 小米自研 | MIUI 公共 SDK |
| 47 | `com.miui.support` | 10 | D3:10 | 小米自研 | MIUI 兼容支持 |
| 48 | `com.xiaomi.ai` | 17 | D3:17 | 第三方 SDK | 小米 AI |
| 49 | `com.xiaomi.aicr` | 11 | D3:11 | 第三方 SDK | AI 能力开放接口 |
| 50 | `com.xiaomi.aiasst` | 11 | D3:11 | 第三方 SDK | 小爱助理能力 |
| 51 | `com.xiaomi.venus` | 6 | D3:6 | 第三方 SDK | Venus 性能能力 |
| 52 | `com.xiaomi.NetworkBoost` | 6 | D3:6 | 第三方 SDK | 网络加速 |
| 53 | `com.xiaomi.gamecenter` | 8 | D3:8 | 第三方 | 游戏中心 |
| 54 | `com.xiaomi.migameservice` | 8 | D3:8 | 第三方 | 小米游戏服务（<queries> 声明可见性） |
| 55 | `com.xiaomi.rcs` | 4 | D3:4 | 第三方 | RCS 融合通信 |
| 56 | `com.xiaomi.dist` | 0 |  | 第三方 SDK | 分布式硬件（DIST_HARDWARE_SERVICE 权限） |
| 57 | `com.xiaomi.vtcamera` | 0 |  | 第三方 SDK | 视频通话相机能力 |
| 58 | `com.xiaomi.wearable` | 0 |  | 第三方 | 穿戴设备 provider（权限声明） |
| 59 | `com.milink.contants` | 1 | D2:1 | 第三方 SDK | MiLink 常量 |
| 60 | `com.milink.service` | 1 | D2:1 | 第三方 SDK | MiLink 服务 |
| 61 | `android` | 371 | D1:371 | AOSP 影子类 | framework 类被一并打入 DEX（371 类，见 §6.4） |
| 62 | `com.android.bluetooth` | 8 | D1:8 | AOSP 引用 | 蓝牙配置引用 |
| 63 | `com.android.provision` | 8 | D1:8 | AOSP 引用 | 开机配置引用 |
| 64 | `com.android.systemui` | 6 | D1:6 | AOSP 引用 | SystemUI 通知接口引用 |
| 65 | `com.android.providers` | 4 | D1:4 | AOSP 引用 | 系统 provider 引用 |
| 66 | `com.android.internal` | 1 | D1:1 | AOSP 引用 | 内部 API 引用 |
| 67 | `com.qualcomm` | 0 |  | 第三方引用 | Qualcomm 权限相关引用 |
| 68 | `com.mobile.businesshall` | 1 | D3:1 | 三方跳转 | 运营商营业厅跳转 |

**关键否定结论**【观测】：
- **无** `retrofit2`、`dagger`/`hilt`、`glide`、`rxjava`/`io.reactivex`、`com.squareup.*`、`firebase`、`crashlytics`、`umeng`、`bugly`、`adjust`(类)、`appsFlyer`、`unity3d`、`flutter`、`react-native` 的类定义。
- `okhttp3` 只剩 2 个类（`okhttp3.internal.annotations.EverythingIsNonNull`、`okhttp3.internal.publicsuffix.PublicSuffixDatabase`）+ 资源 `okhttp3/internal/publicsuffix/publicsuffixes.gz`；**没有任何 okio 类**。【未知】若确实依赖 OkHttp，其主体在 637 个混淆短包内，本轮未定位。
- `com.facebook.*` 类为 0；但 `assets\placement_id` 记录 `123=fb:196567740764356_...`（Facebook 广告位 ID）——**广告位配置存在但 SDK 代码不在**（【推断】广告渲染走系统级 MSA/`com.miui.systemAdSolution` 通道）。

---

## 6. 代码结构与混淆

### 6.1 规模【观测】

| 指标 | 值 |
|---|---|
| DEX 数 | 3（`classes.dex` 8,142,528 B / `classes2.dex` 8,835,532 B / `classes3.dex` 7,543,548 B，合计 24.5 MB） |
| `class_defs` 总数 | **25,009** |
| jadx 产出的顶层 `.java` | **10,624**（≈ 顶层类数；差额是内部类/匿名类） |
| 顶层包根数 | **649** |
| 具名根（12 个） | `com` 14469、`androidx` 2167、`miuix` 1504、`android` 371、`miui` 171、`kotlinx` 139、`kotlin` 87、`org` 45、`vendor` 45、`xcrash` 28、`okhttp3` 2、`java` 1 → **合计 19,029 类（76.1%）** |
| 混淆短包（1–3 字母+数字） | **637 个根，5,980 类（23.9%）** |
| 混淆短包 Top | `aj` 352、`a8` 129、`mk` 124、`a4` 99、`x4` 98、`v3` 84、`c3` 52、`zm` 52、`r3` 51、`ci` 51、`t5` 49、`s` 46、`de` 46、`ok` 45、`z2` 44 … 其中 <5 类的根有 **407 个** |

### 6.2 结论：这是**R8 局部重命名**，不是加固壳

| 判据 | 观测 | 判定 |
|---|---|---|
| 是否加固壳 | `lib/` 下 18 个 .so（×2 ABI）**全部是业务/算法库**：`libc++_shared`、`libfastplayer`、`libjni_load_serinum`、`libjni_sound_effect(_2_0)`、`liblib-riskcontrol`、`libmicontinuity_sdk`、`libmiffmpeg`、`libMNN`/`libmnncore`/`libMNNTrain`/`libMNN_Express`、`libnap`、`libsec-common-utils`、`libtcp`、`libtensorflowlite_jni`、`libxcrash(_dumper)`。**没有** `libjiagu`、`libDexHelper`、`libshell`、`libmobisec`、`libexec.so`、`libprotectClass` | **不是壳**【观测】 |
| dex 是否可读 | 3 个 dex 都是明文，jadx 全量反编译成功（`DONE`，errors=53 / warns=354，只有 `JadxOverflowException` 这类反编译上限） | **无加密/无 VMP**【观测】 |
| `Application` 是否被替换成壳 | `<application android:name="com.miui.securitycenter.Application">`，直接是业务类；无 `StubApp`/`SecShellApplication` 之类 | **不是壳**【观测】 |
| 动态加载 | 全 APK 只有 1 处 `DexClassLoader`：`pi/b.java`（`pi.b`，日志 tag `DexAnalytics`） | 见 §6.3 |
| 混淆程度 | 637 个根被重命名为 1–3 字母；但 `com.miui.networkassistant`/`gamebooster`/`permcenter`/`powercenter` **完全保留原名** | **部分混淆**：自身业务模块 `-keep` 了包名，第三方/内部库被 R8 改名【观测 + 推断】 |

> 也就是说：**它没被壳保护**，正常静态分析可以拿到全部业务逻辑。混淆只让 23.9% 的类（主要是被 repackage 的库）难以按包名归属。

### 6.3 唯一的动态加载点【观测】

`_analysis\jadxout\sources\pi\b.java`（`classes3.dex`）：
```java
this.f33226b = new DexClassLoader(apkPath, context.getDir("dex",0).getAbsolutePath(), optimizedDir, ClassLoader.getSystemClassLoader());
this.f33226b.loadClass("com.miui.analytics.Analytics").getDeclaredMethod("trackEvent", String.class).invoke(null, str);
```
构造方：`ni/c.java: return new pi.b(this.f32067a, D(), E());`

**含义**：这是 `com.xiaomi.onetrack`（ONETrack）的 "DexAnalytics" 变体 —— 当统计实现以**独立 APK**形式存在时，从外部 APK 路径加载 `com.miui.analytics.Analytics` 并反射调用（版本取自 `getPackageArchiveInfo(apkPath, 1)`）。【观测代码 + 推断用途】**不是**壳的加载器；但它是一个真实的"从外部 APK 反射加载代码"通道，加载目标的路径由 `ni.c` 的 `D()` 提供（【未知】该路径来源未追）。

### 6.4 DEX 分包策略【观测】

| DEX | 类数 | 主要内容 |
|---|---|---|
| `classes.dex` | 8,189 | **纯第三方 UI/媒体底座**：`com.google.android.exoplayer2` 2031、`com.google.common`(Guava) 1995、`com.google.android.material` 884、`androidx` 2167、`com.airbnb.lottie` 52、`com.davemorrissey.labs` 20、**`android.*` 影子类 371**、46 个混淆短包 |
| `classes2.dex` | 8,924 | **业务主体**：`com` 6292（`com.miui.maml` 626、`com.miui.networkassistant` 1071、`com.miui.gamebooster` 936、`com.miui.permcenter` 500、`com.miui.powercenter` 496、gson 217、protobuf 246、`com.milink.*` 125、`com.market.sdk` 73、`com.miui.hybrid` 64…）+ **285 个混淆短包** |
| `classes3.dex` | 7,896 | **框架/SDK 层**：`miuix` 1504、`com` 3131（`com.xiaomi.continuity` 486、`com.xiaomi.onetrack` 251、`com.xiaomi.push` 166、`com.miui.warningcenter` 249、`com.qti` 139、`com.xiaomi.mipush` 82…）、`miui.cloud` 171、`kotlinx.coroutines` 139、`kotlin` 87、`vendor.xiaomi` 45、`org.*` 45、`xcrash` 28、`ok` 45、**290 个混淆短包** |

【推断】这不是按"功能"分包，而是 **R8/D8 的默认 64K 方法数切分 + 类序号顺序**：dex1 吃满了 Google 重依赖，dex2 是业务，dex3 是框架/SDK。工程上没有做有意的 feature-split。

### 6.5 `tmsdkobf` 到底是什么（澄清基线里的误导）

- **`tmsdkobf` / `Ltmsdk/*` 不在本 APK 里。**【观测】本 APK 三个 dex 的字符串表中 `Ltmsdk/` 命中数 = **0**，类定义 = **0**。
- 命中来源：`_analysis\dex\classes.dex`（**GuardProvider 的 dex**，22623 条字符串里含 `tmsdk` 的 685 条、含 `miui/guardprovider` 的 108 条），以及其字符串导出 `_analysis\dex_strings_all.txt`（515 KB，702 行命中）。本 APK 的三个 dex 实测 `tmsdk` = **0/0/0**。【观测，命令 `_analysis\tools\scan_tmsdk.js`】
- 它指的是**腾讯移动安全（TMS, Tencent Mobile Secure）SDK**，包名 `tmsdk.*`，混淆变体常被命名为 `tmsdkobf`；内容含 `tmsdk/common/tcc/*`（TCC 引擎：`DeepCleanEngine`、`QSdcardScanner`、`VideoMetaRunner`、`TrafficSmsParser`）、`tmsdk/common/module/qscanner/*`（快速扫描）、`tmsdk/bg/creator/*`、`tmsdk/fg/creator/*`（前后台 Manager 创建器）、`tmsdk/common/roach/nest/PowerNest`（保活）、`tmsdk/common/NumMarker`（号码标记）。**这些是 GuardProvider 的 AV/清理引擎依赖。**
- 本 APK 与它的关系：`com.miui.securitycenter` 只是**通过 `content://guard`（`com.miui.guardprovider.permission.Guard_Provider`）把扫描委托给外部 `com.miui.guardprovider`**（manifest 里 `uses-permission com.miui.guardprovider.permission.Guard_Provider` L325；dex 中 `com/miui/guardprovider` 引用 24+6 处）。**腾讯 TMS/AVL 代码不在本 APK 内。**【观测】

### 6.6 `android.*` 影子类（打包异常，值得记一笔）

【观测】`classes.dex` 里定义了 **371 个 `android.*` 类**（`android.app.Notification$MessagingStyle`、`android.app.TaskStackListener`、`android.content.LocusId`、`android.graphics.BlendMode`、`android.app.usage.StorageStats`…），其中 **359 个带非空 `class_data`**（即含方法/字段，不是空桩）。
【推断】构建时把某个 `framework.jar` / `miui-framework` 当作**普通依赖（implementation 而非 compileOnly）**打进了 dex。运行时同名类会被 bootclasspath 优先解析，通常无行为影响，但属于打包卫生问题 + 体积浪费。
【未知】这些类的具体内容是否为 AOSP 原样拷贝，未逐个比对。

---

## 7. 多进程

`android:process` 只用了 3 个自定义值 + 默认主进程【观测】：

| 进程 | 完整进程名 | 组件数 | 承载 |
|---|---|---|---|
| 主进程 | `com.miui.securitycenter` | **315** | 全部 293 个 Activity（除 8 个）、`com.miui.sec.external.InstallProvider`、`SecSettingsSearchProvider`、`PackageBehaviorContentProvider`、`PrivacyAppsProvider`、`SensitiveWordDetectProvider`、`ZmanProvider`/`ZmanFileProvider`、`androidx.startup.InitializationProvider`、`ProfileInstallReceiver` 等 |
| **`com.miui.securitycenter.remote`** | 同名 | **100** | **承载了几乎所有 Service 与绝大多数 Provider**：`XMPushService`/`XMJobService`、`GuardService`、`AntiSpamService`、`TrafficManageService`、`FirewallService`、`MiuiVpnManageService`、`GameBoosterService`、`VideoToolBoxService`、`NotificationListener`、`BubblesNotificationListenerService`、`SuperPowerTileService`、`PerformanceMode(Wild)TileService`、`SandboxManagerService`、`InvisibleModeService`、`DockMonitorService`、`BeautyService`、`FrameRateDataService`、`FreeformWindowService`、`RemoteService`、`NotificationService`、`MiSafetyDetectService`，以及 `SecurityCenterProvider`/`RemoteProvider`/`DeviceCredentialProvider`/`IdProvider`/`AntiSpamProvider`(`antispam`)/`WarningCenterProvider`/`XSOFProvider`… 全部 17 个 `*ContentProvider` |
| **`com.miui.securitycenter.bootaware`** | 同名 | **3** | `directBootAware` 组：`com.miui.simlock.SimLockMonitorService`、`com.miui.simlock.BootCompleteReceiver`（`LOCKED_BOOT_COMPLETED`）、`com.miui.simlock.activity.SuccessDialogActivity` —— 在**用户解锁前（direct boot）**就能运行，用于 SIM 卡锁 |
| **`:ui`** | `com.miui.securitycenter:ui` | **6** | 气泡通知与游戏加速的 UI 侧：`BubblesService`、`BubbleRemoteService`、`BubbleSettingsActivity`、`BubbleAppManageActivity`、`DockWindowManagerService`、`GameBoosterTelecomManager`、`AntiMsgAccessibilityService`（无障碍跑在 `:ui`，说明其 UI 注入作用域被刻意隔离） |

【推断】`remote` 进程承担了全部"常驻后台 + 跨进程 provider"的职责，是**内存/权限隔离的主要承载者**；主进程只放前台 UI 与少数必须主进程的 provider。

---

## 8. 风险清单（暴露面视角，按严重度排序）

> 前提：所有 listed 组件都以 **system UID** 运行（§1.1）。所以"无权限保护的 exported 组件"不是"普通信息泄露"，而是"**任何本地 App 都能借 system 身份执行一段系统级动作**"。

| # | 严重度 | 组件 | 事实 | 为什么危险 |
|---|---|---|---|---|
| 1 | **高** | `provider com.miui.sec.external.InstallProvider`（authority `com.miui.sec.INSTALL_PACKAGE`，L3806） | exported=true，**无任何 permission / readPermission / writePermission**，无 `grantUriPermissions` | 名字与 authority 直指"安装通道"；provider 直接返回 cursor/调用内部安装逻辑。任何 App 都能 query/insert，若能驱动安装流程即等于"以 system 身份装包"。【未知】其 `query/insert/call` 的具体校验需读 `com.miui.sec.external.InstallProvider` 源码确认（本轮未读） |
| 2 | **高** | `provider com.miui.securitycenter.provider.DeviceCredentialProvider`（`com.miui.securitycenter.credentialprovider`，L2480） | exported=true，**无权限** | 设备凭据（PIN/密码校验）通常是高价值 oracle；无权限导出意味着可被任意 App 当作"密码校验/凭据查询"接口调用 |
| 3 | **高** | `provider com.miui.securitycenter.provider.SecSettingsSearchProvider`（L3813） | exported=true，**无权限**，**grantUriPermissions=true**，带 `<intent-filter action="miui.intent.action.SETTINGS_SEARCH_PROVIDER">` | 唯一"exported + 无权限 + 可授 URI"的 provider；同时暴露搜索接口与临时 URI 授权能力（潜在可把内部文件 URI 授给第三方） |
| 4 | **高** | `service com.miui.networkassistant.vpn.miui.MiuiVpnManageService`（action `com.miui.networkassistant.vpn.MIUI_VPN_MANAGE_SERVICE`，L2974） | exported=true，**无权限** | VPN 管理服务无权限导出 = 任意 App 可请求建立/配置 VPN（以 system 身份），属可流量劫持面 |
| 5 | **中高** | `activity com.miui.permcenter.permissions.SystemAppPermissionDialogActivity`（L3850） | exported=true，**无权限**，接收 `miui.intent.action.SYSTEM_PERMISSION_DECLARE` / `_NEW` / `CTA_DECLARE` / `_NO_OPTIONAL`，且 intent-filter 绑 `content:`、`file:`、`image/*` | **系统权限声明/隐私协议弹窗**可被任意 App 弹出 → 典型"伪造系统授权提示"钓鱼面（用户会把 system UID 的弹窗当成系统提示） |
| 6 | **中高** | `provider com.miui.idprovider.IdProvider`（`com.miui.idprovider`，L2507） | exported=true，**无权限** | OAID/设备标识读取接口；无权限导出 = 任意 App 直接拿 OAID 及可能的关联标识 |
| 7 | **中高** | `activity com.miui.networkassistant.ui.base.UniversalFragmentActivity`（L2759） | exported=true，**无权限**，接收 `com.miui.datatraffic:` scheme + `VIEW` + `NETWORKASSISTANT_*` 多个 action | 名字即 "UniversalFragment"，通常按 extra 里的 `:android:show_fragment` 实例化 fragment → **Fragment 注入面**（可在 system 进程内实例化任意已注册 Fragment） |
| 8 | **中高** | `activity com.miui.permcenter.install.AdbInstallActivity`（L2091） | exported=true，**无权限**，接收 `android.intent.action.INSTALL_PACKAGE` + `file:` + `application/vnd.android.package-archive` | 把一个 `file://`+APK MIME 的 intent 交给 system 身份的安装 UI；配合 `AdbInputApplyActivity`/`AdbInstallVerifyActivity` 构成"ADB 安装"链路，是提权链常见起点 |
| 9 | **中** | `provider com.xiaomi.security.xsof.XSOFProvider`（`com.xiaomi.security.xsof.provider`，L4989）+ `service com.xiaomi.security.xsof.MiSafetyDetectService`（L4994） | 均 exported=true，**无权限** | XSOF 是"小米安全能力开放框架"；服务 action `com.xiaomi.security.xsof.SAFETY_DETECT_SERVICE` 可被任意 App 拉起做安全检测（可能返回设备风险等级） |
| 10 | **中** | `provider com.miui.warningcenter.provider.WarningCenterProvider`（L4147）、`AutoTaskServiceProvider`（L1225）、`PrivacyCenterProvider`（L1358）、`InputProvider`（L4076）、`SystemAdContentProvider`（L4491） | 全部 exported=true、**无权限** | 预警/自动任务/隐私中心/输入法配置/开屏广告 的完整数据面暴露给任意 App |
| 11 | **中** | 2 个 normal 级权限（`miui.permission.TVM` L79、`...PERM_USE_INFO` L113） | protectionLevel 缺失 → normal | 声称有权限保护的 `AppPermissionsUseActivity`（L3858 附近）实际防护失效；`TvmManagerService` 因同时 `enabled=false` 目前不可达，属潜在缺口 |
| 12 | **中** | `android:allowBackup` 缺省（=true）+ 自定义 `backupAgent` | 观测 | system UID 应用的数据（拦截规则/隐私配置）可走 backup 通道 |
| 13 | **低-中** | 106 个 exported 且无权限的 Activity（完整见 §4.2-A.5） | 观测 | 大量 `miui.intent.action.*` 内部入口对外可见；单独看多为 UI 跳转，但都运行在 system 进程 |
| 14 | **低** | 8 个 exported 无权限 Receiver：`BootCompleteReceiver`(LOCKED_BOOT_COMPLETED)、`DateResetReceiver`(TIME_SET/TIMEZONE_CHANGED/LOCALE_CHANGED)、`DeviceOwnerChangedReceiver`、`RootUpdateReceiver`、`ProvisionCompleteReceiver`、`PolarisGeofenceReceiver`、`AppPredictWidget2x4`、`BehaviorWidget` | 观测 | 均为"系统广播触发"，第三方无法直接伪造部分 action；但 `DateResetReceiver` 接收 `TIME_SET`，配合 `MANAGE_BOOT_TIME` 权限值得注意 |

**正向加固**【观测】：`android:exported` 全部显式声明（无默认导出）；28 条 `protected-broadcast`；provider authority 无重复；40 个 provider 中 18 个带 `signatureOrSystem` 权限。

---

## 9. 未解之谜（交给后续/需要动态验证）

1. **`com.miui.sec.external.InstallProvider` 的实际校验**：manifest 层完全裸奔，但代码层可能有 `Binder.getCallingUid()` 检查/`enforceCallingPermission`。需要读 `com.miui.sec.external.InstallProvider`（`classes*.dex`，未 jadx 定位到具体文件）确认是否有 UID 白名单。【未知】
2. **`DeviceCredentialProvider` / `IdProvider` / `XSOFProvider` / `SecSettingsSearchProvider` 的 `query/insert/call` 语义与调用方校验**：同上，manifest 只能给出"暴露"，不能给出"是否真能被利用"。【未知】
3. **`SecSettingsSearchProvider` 的 `grantUriPermissions=true` 实际能授出哪些路径**：取决于其内部 `openFile`/`getType` 实现，未读。【未知】
4. **`network_security_config` 内容**（`@0x7f15003c`）：本目录没有 apktool 的 `res/xml/`（`apktool_sc` 缺 manifest 与 xml，`apktool` 是 GuardProvider 的），需要解 `resources.arsc` 才能拿到 `res/xml/network_security_config.xml`，进而判断是否 `cleartextTrafficPermitted` 与 pinning 配置。【未知】
5. **`pi.b`(DexAnalytics) 的外部 APK 路径从哪来**：`ni.c.D()` 未读；需要确认是"指向 `com.miui.analytics` 独立 APK"还是可被替换的路径（若是后者则是任意代码加载面）。【未知】
6. **23.9% 混淆类（5980 个 / 637 个根）的归属**：本轮只按包名统计，未做引用分析。哪些根属于 okhttp/okio/其它被 repackage 的库，需要 `trace_android_references` 或字符串/资源引用反向定位。【未知】
7. **`android.*` 371 个影子类**：需确认是否为 AOSP 原样拷贝、是否有被篡改的 framework 行为（若是选择性拷贝，风险等级会显著上升）。【未知】
8. **签名真实性**：REA 只做了"声明的摘要 == 当前字节"的自洽校验，**没有做密码学验签**。若要断言"未被二次打包"，需要独立 `apksigner verify --print-certs` 并与 MIUI 平台证书指纹比对。【未知】
9. **`application.properties` 里的内网 DB 配置**：
   ```
   #spring.datasource.url=jdbc:mysql://[ADDRESS_REMOVED]:3306/miui_sec_api?useUnicode=true&characterEncoding=utf8
   #spring.datasource.username=root
   #spring.datasource.password=sec
   ```
   已注释，但**随 APK 出厂**。属于构建卫生/信息泄露问题（内网 IP + 弱口令样式），不是可利用漏洞；是否有其它构建遗留文件带有效凭据未全查。【观测内容 + 推断性质】
10. **`<queries>` 只声明 2 个包** 与 `QUERY_ALL_PACKAGES` 缺失并存：依赖 system UID 的隐式可见性，若该 APK 被降级为非 system 安装会立刻失去包可见性——即"预装依赖"的硬约束。【推断】

---

## 10. 一页速览

- 身份：`com.miui.securitycenter` 10.4.9-250402.0.1 (40001049)，**`sharedUserId=android.uid.system`**，minSdk 23 / targetSdk 34。
- 签名：v1+v2+v3（无 v3.1/无轮换/无 source stamp），证书 `O=Xiaomi, OU=MIUI, CN=MIUI`，SHA-256 `c9009d01…9025`，2011→2039 自签名 RSA —— **MIUI 平台密钥**。v1 覆盖 9228/9231 条目。
- 组件：**424**（293 act + 2 alias + 67 svc + 22 rcv + 40 prov）；**exported 194**（其中 5 个 `enabled=false`，有效 188），**128 个零权限保护**（127 个当前有效）。
- 权限：自定义 **50**（43×signatureOrSystem、3×signature、2×normal、**2×漏写→normal**）；`uses-permission` **147**（AOSP 普通 30 / 危险 17 / AOSP 签名·特权 28 / 厂商私有 72）。
- SDK：ExoPlayer2 2031、Guava 1995、Material 884、AndroidX 2167、MIUIX 1504、MAML 626、Continuity 486、ONETrack 251、MiPush 248、protobuf 246、Gson 217、Qualcomm 139、xCrash 28、TFLite 14…；**无 retrofit/dagger/glide/rxjava/firebase/umeng**；`tmsdk*` **不在本包**。
- 结构：25009 个 `class_defs` / 649 个顶层根 / **637 个混淆短包含 5980 类（23.9%）**；3 dex 明文、jadx 全量成功；**无壳无加固**；唯一动态加载点 `pi.b`(DexAnalytics)。
- 进程：主进程(315) + `:remote`(100) + `:bootaware`(3) + `:ui`(6)。
