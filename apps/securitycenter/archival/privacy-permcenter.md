> **历史分析存档，非最终结论。** 本文保留原始分析的技术内容，可能包含尚未验证或已被纠正的表述。隐私数据上传范围、同意条件、FBO 本地委托、报警短信加密、Provider 鉴权等，以 [最终报告](../REPORT.md) 与 [独立复核](../REVIEW.md) 为准。文中源码定位保留为原反编译相对路径；只有 evidence/ 中列出的精选文件随包提供，未打包完整源码树。

# MIUI 安全中心 — 权限中心 / 隐私业务模块逆向报告

- 目标：`MIUISecurityCenter-10.4.9-250402.0.1.apk`（`com.miui.securitycenter`，versionCode 40001049）
- SHA-256：`fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd`（见 `_BASELINE.md`）
- 分析方式：**纯静态**。全量 jadx 10624 个 `.java` + apktool 解码的 `AndroidManifest.xml` / `res/values/strings.xml`。
- 标注约定：**观测** = shipped artifact 中可直接引用的代码/清单事实；**推断** = 由代码结构推得但无运行时证据；**未知** = 静态无法判定。
- 证据格式：`相对路径:行号`（路径相对 `_analysis\jadxout\sources\` 或 `_analysis\apktool_sc\`）。

---

## 0. 摘要：十条关键结论

1. **`miui.security.IPermissionManager` 在本 APK 中不存在**（全树 grep `IPermissionManager` 零命中——**观测**）。本 App 与权限数据库之间的真实边界是三条：
   - `ContentResolver.call("content://com.lbe.security.miui.permmgr", "<methodCode>", ...)` — 由**预装的 `com.lbe.security.miui`**（LBE 权限管家）提供（`com/miui/permission/PermissionContract.java:10,15`；`com/miui/permission/PermissionManager.java:302,437`）；
   - `miui.security.SecurityManager`（system_server 的 `"security"` 系统服务，框架类，非本 APK 代码）——应用锁、隐私应用、隐私输入法、跨端等状态的**唯一真源**；
   - `AppOpsManager` 隐藏 API + MIUI 私有 op（`10000–10053`）——权限"生效"层。
2. 权限**写入是双写**：先写 MIUI 自己的 permmgr（`setMode` method=9 / `setApplicationPermission` method=6），再用反射调 AOSP `IPackageManager.grantRuntimePermission` / `revokeRuntimePermission` / `updatePermissionFlags` 保持框架一致（`g4/b.java:320-417`）。**观测**。
3. 所有开关型偏好设置不落 App 私有 SP，而是走 **`content://com.miui.securitycenter.remoteprovider` 的 `callPreference`**，实体是 `SharedPreferences("remote_provider_preferences")`（`r4/a.java`、`com/miui/common/persistence/RemoteProvider.java`）。**观测**。
4. 隐私同意（CTA）是一条独立的 **`ue` 包 SDK**，POST 到 `data.sec.miui.com/privacy/{agree,revoke,get,latestVersion}`，带自签 `sign` 头（硬编码 key `2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9`，MD5+Base64）。国际版直接短路返回 `-4`。**观测**。
5. **隐私缩略图模糊不是 `com.miui.blur` 实现的**。`com.miui.blur` 是 backdrop 背景虚化 SDK（RenderScript + `MiuiCompositionSamplingListener` + SurfaceControl）；隐私模糊的真正开关是 `Settings.System."miui_recents_privacy_thumbnail_blur"`（逗号分隔包名列表），由 `com.miui.home`（桌面）读取渲染。**观测**。
6. 隐私风险检测（`PrivacyRiskDetectionActivity`）共 **8 类检测项**（id 11/12/13/14/15/16/22 + 固定 3 张推荐卡），判定依据是"**权限是否为「始终允许」(action==3) 的应用计数**" + DeviceAdmin/通知监听/UsageStats 计数 + 云端风险应用名单。**观测**。
7. 存在**已声明但代码缺失**的导出组件：`com.miui.permcenter.untrustwarn.UnknownSourceWarnActivity`、`com.miui.permcenter.privacymanager.XSpaceWarningDialogActivity` — 清单有、DEX 无对应类。**观测（缺类）**，是否仍被系统拉起 → **未知**。
8. 应用锁的密码/图案**从不落盘在本 App**：校验是 `SecurityManager.checkAccessControlPassword(type, pwd)`，设置在 `setAccessControlPassword(pwd, type)`，全部在 system_server。本 APK 只负责 UI 与"解锁后放行"（`addAccessControlPass`）。**观测**。
9. 跨端权限（`acrossterminal`）把数据写在 **`/data/misc/user/<userId>/securitycenter/across_terminal.xml`**（XML v1，按小米账号分组）。**观测**。
10. 上传面（隐私相关）：`flash.sec.miui.com/user/{cat,ra}` 会上报**桌面可见应用包名清单（ra 还带 versionCode）**+ 设备型号/MIUI 版本，`biz_id="rivacy_risk_check"`（拼写缺首字母 r）。AES/ECB/PKCS5 + GZIP + Base64，key=`SHA1(ConfigManager.getSecurityHttpKey())`。**观测**。

---
## 1. 模块地图

| 包 | 类数 | 职责 |
|---|---|---|
| `com.miui.permcenter` | 162 | 权限中心主 UI：逐权限/逐应用授权、自启动、隐私检测、缩略图模糊、隐私输入法、关机密码、跨端权限、行为记录、安装校验、胶囊 |
| `com.miui.permission` | 16 | **权限 SDK**：`PermissionManager`（115 个 `PERM_ID_*`）、`PermissionContract`（method 码）、`StoragePolicyContract`、`RequiredPermissionsUtil` |
| `com.miui.privacyapps` | 10 | 隐私应用（隐藏应用空间）UI + `PrivacyAppsProvider`（`app.hide`） |
| `com.miui.applicationlock` | 34 | 应用锁：解锁 UI（图案/数字/混合密码/指纹/人脸）、通知内容遮挡、找回密码 |
| `com.miui.blur` | 18 | backdrop 背景虚化 SDK（非隐私模糊） |
| `com.miui.appmanager` | 22 | 应用管理：批量卸载、清缓存/数据、通知开关、用量统计 |
| `com.miui.devicepermission` | 7 | 跨设备（屏幕/摄像头/麦克风共享）权限编辑与授权弹窗 |
| `com.miui.combinepermission` | 4 | 组合权限一次性授权弹窗（CTA） |
| `com.miui.idprovider` | 11 | OAID/设备标识读取与授权（`IdProvider` + 授权 Activity） |
| `com.miui.appcompatibility` | 1 | 应用兼容性异常提示（仅 1 个 Activity） |

清单中与上述包相关的组件共 **107 处字符串命中**（对 `AndroidManifest.xml` grep `com\.miui\.(permcenter|permission|privacyapps|applicationlock|blur|appmanager|devicepermission|combinepermission|idprovider|appcompatibility)` 的计数；按 `<activity|service|provider|receiver|activity-alias>` 解析后的实体约 120 行，含重复 intent-filter 展开）。`com.miui.blur` **无任何清单组件**（纯代码库）。

---

## 2. 导出组件与拉起者一览（重点）

> 来源：`_analysis\apktool_sc\AndroidManifest.xml`（用 XML 解析导出，`exported` / `permission` 均取自清单原始属性）。

### 2.1 高价值导出入口

| 组件 | exported | 保护权限 | intent action（拉起者） |
|---|---|---|---|
| `permcenter.permissions.SystemAppPermissionDialogActivity` | true | 无 | `miui.intent.action.SYSTEM_PERMISSION_DECLARE` / `_NEW` / `CTA_DECLARE` / `_NO_OPTIONAL` — **第三方 App 首次请求权限时由系统/权限管家拉起** |
| `permcenter.permissions.PermissionsEditorActivity` | true | 无 | `miui.intent.action.APP_PERM_EDITOR`、`miui.intent.action.APP_PERM_EDITOR_PRIVATE` — 设置/桌面长按"应用信息→权限" |
| `permcenter.permissions.PermissionAppsEditorActivity` | true | 无 | `miui.intent.action.PERMISSION_APP_EDITOR_ACTIVITY` — 按权限查看应用列表 |
| `permcenter.permissions.AppPermissionsUseActivity` | true | `com.miui.securitycenter.permission.PERM_USE_INFO` | `miui.intent.action.APP_PERMISSION_USE` — 权限使用提示（状态栏点击） |
| `permcenter.permissions.AppPermissionsTabActivity` | true | 无 | 无 filter（显式 Intent），参数 `select_navi_item` |
| `permcenter.autostart.AutoStartManagementActivity` | true | 无 | `miui.intent.action.OP_AUTO_START` — 设置→应用→自启动 |
| `permcenter.detection.PrivacyRiskDetectionActivity` | true | 无 | `miui.intent.action.PRIVACY_RISK_DETECTION_ACTIVITY` — 安全中心/设置隐私检查 |
| `permcenter.privacyblur.PrivacyThumbnailBlurSettings` | true | 无 | `com.miui.securitycenter.action.PRIVACY_THUMBNAIL_BLUR_SETTING` |
| `permcenter.settings.OtherPermissionsActivity` | true | `...permission.SYSTEM_PERMISSION_DECLARE` | 无 filter（"其他权限"聚合页） |
| `permcenter.settings.PrivacyInputModeActivity` | true | 无 | `miui.intent.action.PRIVACY_INPUT_MODE_ACTIVITY` |
| `permcenter.settings.ShutDownPasswordActivity` | true | `...SYSTEM_PERMISSION_DECLARE` | `miui.intent.action.SHUT_DOWN_PASSWORD_ACTIVITY` |
| `permcenter.settings.InvisibleModeActivity` | true | 无 | `com.miui.securitycenter.action.INVISIBLE_SETTING` |
| `permcenter.privacycenter.PrivacySafetyActivity` | true | 无 | `miui.intent.action.PRIVACY_SETTINGS` / `SECURITY_CENTER_SETTINGS` / `PRIVACY_SAFETY` |
| `permcenter.privacycenter.usage.AppPermissionUsageActivity` | true | 无 | `miui.intent.action.APP_PRIVACY_DETAIL`（+ 别名 `privacymanager.behaviorrecord.PrivacyDetailActivity`） |
| `permcenter.privacycenter.usage.PermissionUsageRecordActivity` | true | 无 | `miui.intent.action.APP_BEHAVIRO_RECORD` |
| `privacyapps.ui.PrivacyAppsManageActivity` | true | 无 | `miui.intent.action.MANAGE_PRIVACY_APPS` |
| `privacyapps.ui.PrivacyAppsHelper` | true | `miui.permission.USE_INTERNAL_GENERAL_API` | `miui.intent.action.PRIVACY_APPS_HELPER`（externalOnly 的入口跳板） |
| `applicationlock.FirstUseAppLockActivity` | true | 无 | `com.miui.securitycenter.action.LOCK_APP` |
| `applicationlock.ConfirmAccessControl` | true | 无 | `miui.intent.action.CHECK_ACCESS_CONTROL` — **系统在受保护 App 前台时拉起**，携带 `android.app.extra.PROTECTED_APP_TOKEN`（IBinder） |
| `applicationlock.LockChooseAccessControl` | true | 无 | `com.miui.securitycenter.action.LOCKCHOOSEACCESSCONTROL` |
| `applicationlock.TransitionHelper` | true | 无 | `com.miui.securitycenter.action.TRANSITION` |
| `idprovider.ui.GrantPermissionActivity` | true | 无 | `miui.intent.action.REQUEST_PERMISSIONS_FOR_OAID` — App 读 OAID 被拦截时拉起 |
| `idprovider.ui.OAIDSettingsActivity` / `OAIDAppSettingsActivity` / `legacy.OAIDAppsActivity` | true | 无 | `miui.intent.action.OAID_APPS` |
| `combinepermission.grantpermission.CombinePermissionActivity` | true | `...SYSTEM_PERMISSION_DECLARE` | `miui.intent.action.REQUEST_PERMISSIONS_COMBINE`、`miui.intent.action.CTA_REQUEST_PERMISSIONS_COMBINE` |
| `devicepermission.grantpermission.GrantDevicePermissionActivity` | true | `...SYSTEM_PERMISSION_DECLARE` | `miui.intent.action.REQUEST_DEVICE_PERMISSIONS` |
| `devicepermission.editpermission.DevicePermissionEditorActivity` / `DevicePermissionSettingsActivity` | true | `...SYSTEM_PERMISSION_DECLARE` | `miui.intent.action.EDITOR_DEVICE_PERMISSIONS` / `DEVICE_PERMISSION_SETTINGS` |
| `appmanager.AppManagerMainActivity` | true | 无 | `miui.intent.action.APP_MANAGER` |
| `appmanager.ApplicationsDetailsActivity` | true | 无 | `miui.intent.action.APP_MANAGER_APPLICATION_DETAIL` |
| `permcenter.install.AdbInstallActivity` | true | 无 | `com.miui.securitycenter.intent.action.INSTALL_PACKAGE`（ADB/包安装器安装确认） |
| `permcenter.MainAcitivty`（原文拼写如此） | true | 无 | `miui.intent.action.LICENSE_MANAGER`（开源许可） |
| `permcenter.AccessibilityServiceActivity` | true | `miui.permission.ACCESSIBILITY_SERVICE` | `miui.intent.action.ACCESSIBILITY_SERVICE` |
| `permcenter.NotificationVerifyActivity` | true | `miui.permission.NOTIFICATION_VERIFY` | `miui.intent.action.NOTIFICATION_VERIFY` |
| `permcenter.WLANVerificationActivity` | true | 无 | `miui.intent.action.OPEN_WIFI_VERIFY` |
| `permcenter.ctaverify.SendSmsVerificationActivity` | true | `android.permission.BROADCAST_SMS` | `miui.intent.action.SEND_SMS_VERIFY` |
| `permcenter.DeviceOwnerChangedReceiver` | true | 无 | `android.app.action.DEVICE_OWNER_CHANGED` |
| `permcenter.install.PackageVerificationReceiver` | true | `android.permission.INSTALL_PACKAGES` | `com.miui.action.PACKAGE_NEEDS_VERIFICATION` — **包安装器安装前校验回调** |
| `permcenter.root.RootUpdateReceiver` | true | 无 | `com.android.updater.action.ACQUIRED_ROOT_SUCCESSED` |
| `permcenter.privacymanager.widget.BehaviorWidget` | true | 无 | AppWidget（行为记录小组件） |

### 2.2 导出 Provider（对外数据面）

| Provider | authority | 保护权限 | 说明 |
|---|---|---|---|
| `privacyapps.PrivacyAppsProvider` | `com.miui.securitycenter.app.hide` | `...SYSTEM_PERMISSION_DECLARE` | 仅实现 `call("isPrivacyApp")` → `SecurityManager.isPrivacyApp(pkg,userId)`（`com/miui/privacyapps/PrivacyAppsProvider.java:39-56`） |
| `permcenter.permissions.acrossterminal.AcrossTerminalPermissionContentProvider` | `com.miui.permissions.acrossterminal` | `...SYSTEM_PERMISSION_DECLARE` | 跨端权限读写（见 §4.9） |
| `permcenter.privacycenter.PrivacyCenterProvider` | `com.miui.permcenter.privacycenter` | 无 | `setCTAStatus` / `queryShowCTA` / `queryCTAStatus` / `privacy_provider_get_using`（`PrivacyCenterProvider.java:38-80`） |
| `permcenter.privacymanager.behaviorrecord.PackageBehaviorContentProvider` | `com.miui.securitycenter.appbehaviors` | `...permission.READ_APP_BEHAVIORS` | 应用行为记录查询 |
| `idprovider.IdProvider` | `com.miui.idprovider` | 无 | OAID/设备标识（见 §4.15） |
| `com.miui.sec.external.InstallProvider` | `com.miui.sec.INSTALL_PACKAGE` | 无 | 安装包信息 |

> **观测**：绝大多数 `permcenter` 页面导出于无权限保护（`exported=true` 且无 `android:permission`），这意味着**任何第三方 App 都能直接拉起权限中心的具体页面**并传入 `extra_pkgname` 等参数，做 UI 欺骗/信息探测。带 `permission` 保护的只有少数（`PERM_USE_INFO`、`SYSTEM_PERMISSION_DECLARE`、`READ_APP_BEHAVIORS`）。

---

## 3. 核心数据源与写入链路（全局）

### 3.1 三条链路总览

```
[UI 开关]
   │
   ├─(A) 权限模型层 → PermissionManager(com.miui.permission)
   │        └─ ContentResolver.call("content://[<userId>@]com.lbe.security.miui.permmgr", "<code>", arg, bundle)
   │              └─ com.lbe.security.miui（预装 LBE 权限管家，本 APK 之外）→ 内部再操作 AppOpsService
   │
   ├─(B) AppOps 生效层 → AppOpsManager.setMode/setUidMode/startWatchingActive（隐藏 API + MIUI 私有 op）
   │
   └─(C) 框架一致性 → 反射 IPackageManager.grantRuntimePermission / revokeRuntimePermission / updatePermissionFlags
```

### 3.2 (A) permmgr provider 的 method 码表（**观测**，`com/miui/permission/PermissionContract.java:44-61`）

| 常量 | 值 | 调用点 |
|---|---|---|
| `GET_ALL_PERMISSION_GROUPS` | 3 | `PermissionManager.java:355` `getAllPermissionGroups()` |
| `GET_ALL_PERMISSIONS` | 4 | `PermissionManager.java:364` |
| `UPDATE_DATA` | 5 | `PermissionManager.java:523` |
| `SET_APPLICATION_PERMISSION` | 6 | `PermissionManager.java:437,451`（异步 `ConcurrentUtils.execute`） |
| `GET_EFFECTIVE_PERMISSIONS` | 7 | `PermissionManager.java:374` |
| `WRITE_WAKE_PATH_WHITE_LIST` | 8 | （唤醒路径白名单，本模块外） |
| `SET_MODE` | 9 | `PermissionManager.java:302` `setModeNow()` |
| `GET_NOT_RECORD_LIST` / `ADD_NOT_RECORD_LIST` | 10 / 11 | 行为记录排除名单 |
| `GET_PERMSSION_FOR_ID` | 12 | `PermissionManager.java:381` |
| `GET_USING_LOCATION_LIST` | 13 | 定位使用中列表 |
| `SEND_WAKEPATH_PERMISSION_RECORD` / `SEND_PERMISSION_RECORD` | 14 / 15 | 行为记录上报 |
| `READ_CLIPBOARD_TIP_SETTING` | 16 | 剪贴板提示 |
| `GET_PERMSSION_STATE` | 17 | — |
| `SET_APP_BEHAVIOR_RECORD_ENABLE` | 18 | `PermissionManager.java:395` |
| `GET_PERMISSION_FOR_NAME_DESC` | 19 | — |

多用户寻址：`content://<userId>@com.lbe.security.miui.permmgr`（`permcenter/autostart/AutoStartManagementActivity.java:414`）——URI authority 前缀 `<uid>@` 是 MIUI ContentProvider 的多用户写法（对照 `r4/a.java` 的 `"110@"+authority`）。

### 3.3 (B) MIUI 私有 AppOps op 码（**观测**，`android/app/AppOpsManagerCompat.java`）

`MIUI_OP_START = 10000`，`MIUI_OP_END = 10053`；AOSP op 0–133。隐私/权限相关的关键项：

| 常量 | 值 | 用途 |
|---|---|---|
| `OP_AUTO_START` | 10008 | **自启动**（与 `PERM_ID_AUTOSTART=16384` 配对） |
| `OP_BOOT_COMPLETED` | 10007 | 开机启动 |
| `OP_BACKGROUND_START_ACTIVITY` | 10021 | 后台弹出界面 |
| `OP_SHOW_WHEN_LOCKED` | 10020 | 锁屏显示 |
| `OP_GET_INSTALLED_APPS` | 10022 | 读取已安装应用列表 |
| `OP_GET_OAID` / `OP_GET_OAID_USER` | 10037 / 10038 | **OAID 读取（无需权限版 / 需用户授权版）** |
| `OP_GET_ANONYMOUS_ID` | 10024 | 匿名设备 ID |
| `OP_GET_UDEVICE_ID` | 10025 | UDeviceID（旧） |
| `OP_DEVICE_ID` | 10050 | 设备标识 |
| `OP_READ_CLIPBOARD` / `OP_WRITE_CLIPBOARD` | 29 / 30 | 剪贴板 |
| `OP_CAN_BE_RECORDED` | 10041 | **能否被录屏/投屏采集（隐私遮蔽核心）** |
| `OP_MEDIA_PROJECTION` | 10042 | 投屏 |
| `OP_SERVICE_FOREGROUND` | 10023 | 前台服务 |
| `OP_BACKGROUND_LOCATION` | 10027 | 后台定位 |
| `OP_ACCESS_GALLERY` | 10034 | 相册访问 |
| `OP_ACCESS_SOCIALITY` | 10035 | 社交关系 |
| `OP_SHAKE` | 10044 | 摇一摇（广告跳转） |
| `OP_READ_NOTE` / `OP_READ_RECORD` | 10052 / 10051 | 笔记 / 录音 |
| `MODE_ALLOWED / IGNORED / ERRORED / DEFAULT / FOREGROUND / ASK` | 0 / 1 / 2 / 3 / 4 / 5 | **`MODE_ASK=5` 是 MIUI 扩展**（"每次询问"），AOSP 无此值 |

封装层：
- `com/miui/permcenter/compact/AppOpsUtilsCompat.java` — 反射 `checkOpNoThrow/setMode/setUidMode/startWatchingActive/startWatchingNoted/permissionToOpCode/strOpToOp`；`setModeWithXSpace()` 会**同时给 XSpace 用户 999 写一份**（`AppOpsUtilsCompat.java` 中 `if (b2.c(context,str)) setMode(..., z1.j(999, z1.b(uid)), ...)`）。
- `switchOpToMiuiFlaresPermission()` — 把 appops op 映射回 MIUI `PERM_ID`：`0/1/41/42 → 32(LOCATION)`、`26 → 4096(CAMERA)`、`27 → 131072(MIC)`、`10042 → 1`。

### 3.4 (C) 双写实证（**观测**，`g4/b.java`）

- 授权（`action==1`，即"允许"）：`PermissionManager.setModeNow(op, uid, pkg, 1, true)` → 再 `tg.a.i/j(...)` = `IPackageManager.grantRuntimePermission` + `updatePermissionFlags(state 3/2)`（`g4/b.java:382-404`）
- 拒绝：`setModeNow(op, uid, pkg, i11==3 ? 0 : 4, true)` → `tg.a.f(...)` = `revokeRuntimePermission` + `updatePermissionFlags(1,1)`（`g4/b.java:406-417`）
- 存储权限特殊分支：`targetSdk<33` 走 `PermissionManagerCompat.setApplicationPermissionWithVirtual(..., PERM_ID_EXTERNAL_STORAGE, ...)`（`g4/b.java:334`）；`targetSdk>=33 && 权限==-3` 走"媒体视觉权限组"逐个 `setUidMode`（`g4/b.java:345-357`）。
- **虚拟权限**：`PermissionManagerCompat.setApplicationPermissionWithVirtual()`（`com/miui/permcenter/compact/PermissionManagerCompat.java:13-19`）在 `action==7`（`ACTION_VIRTUAL`）时额外写一条 `virtualMap` 映射权限，动作改为 1（拒绝）。`virtualMap`（`PermissionManager.java:176-182`）= `READSMS→REAL_READ_SMS`、`READCONTACT→REAL_READ_CONTACTS`、`CALENDAR→REAL_READ_CALENDAR`、`READCALLLOG→REAL_READ_CALL_LOG`、`PHONEINFO→REAL_READ_PHONE_STATE`（仅 `overMiuiTwelve()` 且 SDK≥28）。

---

## 4. 分模块详解

### 4.1 `permcenter.permissions` — 授权主界面（24 类）

**功能清单（观测）**

| 功能 | 类 |
|---|---|
| 逐应用权限编辑器（Tab：权限/其他权限） | `AppPermissionsTabActivity`、`AppPermissionsFragment`、`NewPermissionsEditorFragment` |
| 逐权限查看应用列表（"相机 → 哪些应用能用"） | `PermissionAppsEditorActivity`、`PermissionAppsModifyFragment`、`PermissionAppsModifyActivity` |
| 系统权限声明弹窗（第三方 App 首次请求） | `SystemAppPermissionDialogActivity` |
| 权限使用提示（状态栏"XX 正在使用定位"） | `AppPermissionsUseActivity`、`AppPermissionItemActivity` |
| 二次权限（Second permission） | `SecondPermissionAppsActivity/Fragment` |
| 跨端权限 | `acrossterminal/*`（§4.9） |
| 偏好控件封装 | `RadioButtonPreference`、`ValuePreference`、`PermTipsPreference`、`ButtonPanel` 等 |

**链路（观测）**

`AppPermissionsTabActivity`（`select_navi_item` 选页，`AppPermissionsTabActivity.java:263`）
→ `AppPermissionsFragment` / `SystemPermissionsEditorFragment` 读数据：`PermissionManager.getAllPermissions(GROUP_*)`、`getAllPermissionGroups()`、`getPermissionForId()`（method 3/4/12）
→ 展示三态（允许 / 仅前台 / 询问 / 拒绝）来自 `AppPermissionInfo.getPermissionToAction()`，动作值由 `PermissionManager.calculatePermissionAction()`（`PermissionManager.java:211-238`）按 8 个 mask 计算，`ACTION_*`：`0=DEFAULT,1=REJECT,2=PROMPT,3=ACCEPT,4=BLOCK,5=NONBLOCK,6=FOREGROUND,7=VIRTUAL`（`PermissionManager.java:24-31`）
→ 用户点击 → `g4.b.l()`（权限组）/ `g4.b.m()`（单权限）→ 双写（§3.4）。

**外部 Intent 契约（观测）**

| 入口 | 参数 | 返回 |
|---|---|---|
| `APP_PERM_EDITOR` / `APP_PERM_EDITOR_PRIVATE` | `extra_pkgname`（`PermissionsEditorActivity.java:53`） | — |
| `APP_PERMISSION_USE`（需 `PERM_USE_INFO` 权限） | `extra_pkgname`（`AppPermissionsUseActivity.java:99`） | `setResult(2308)`（`AppPermissionsUseActivity.java:169`） |
| `PERMISSION_APP_EDITOR_ACTIVITY` | `extra_permission_id`、`extra_permission_name`、`group_id`（`PermissionAppsEditorActivity.java:1216,1254-1258,1389,1412`） | `permission_name` / `group_id` / `permission_id` / `permission_action` / `extra_permission_info` |
| `SYSTEM_PERMISSION_DECLARE*` | `miui.intent.extra.REQUEST_PERMISSIONS_NAMES`、`..._NAMES_DESC`、`...KEY_REQUEST_PERMISSIONS_STATE`、`...PACKAGE_NAME`、`show_locked`（`SystemAppPermissionDialogActivity.java:1041-1049,1133`） | 同名字段 + `REQUEST_PERMISSIONS_RESULTS` |

> `SystemAppPermissionDialogActivity` 是 MIUI 私有权限弹窗的实际实现（54 KB，同时支持 `CTA_DECLARE` 与 `_NO_OPTIONAL`），并把"权限描述"写回 provider 的 `miui_permission_description_info` 表（`SystemAppPermissionDialogActivity.java:1127,1196`，字段 `locale/description/pkgName/permissionId/processState`，见 `PermissionContract.java:113-119`）。

**数据**：无本地 DB；全部走 permmgr provider（远程）。描述缓存表 `miui_permission_description_info`。

---

### 4.2 `permcenter.autostart` — 自启动管理（4 类）

- **功能**：应用级"允许自启动"开关 + 搜索 + 分组。
- **写入链路（观测）**：`AutoStartManagementActivity:403-423`
  - `z1.r()`（当前在用户 0）时：若目标 userId ∉ {0, 999}，直接 `ContentResolver.call("content://<userId>@com.lbe.security.miui.permmgr", "6", null, bundle)`，`EXTRA_PERMISSION=16384(PERM_ID_AUTOSTART)`、`EXTRA_ACTION`、`EXTRA_PACKAGE=[pkg]`、`extra_flags=0`、`EXTRA_USER_ID`；
  - 否则 `permissionManager.setApplicationPermission(16384L, action, userId, pkg)`。
  - 写完后 `f` 线程延迟 400 ms 再做后续（`AutoStartManagementActivity.java:424-428`）。
- **数据**：permmgr（远程）；无本地表。
- **AIDL/系统边界**：`content://com.lbe.security.miui.permmgr`（method 6）；配套 appop `OP_AUTO_START=10008` 由 `com.lbe.security.miui` 侧落库。
- **拉起者**：`miui.intent.action.OP_AUTO_START`（设置→应用→自启动）；`PrivacyRiskDetectionActivity` 的推荐卡也跳这里（`PrivacyRiskDetectionActivity.java:398-400`）。

---

### 4.3 `permcenter.detection` — 隐私风险检测（重点专项）

**A. 检测项清单（观测；item id 来自 `detection/model/d.java` + `PrivacyRiskDetectionActivity.java:752-771`）**

| id | 含义（字符串资源） | 数据来源 | 判定依据 |
|---|---|---|---|
| **11** | `privacy_risk_sensitive_permissions_tip` 敏感权限 | `ub.b`(task) + `mc.c.w()` | 敏感权限集合中"**始终允许(action==3)**"的应用个数 > 0：`32(LOCATION)`、`8/READCONTACT`、`16/READCALLLOG`、`131072(CLIPBOARD)`、`PERM_ID_EXTERNAL_STORAGE`；SDK≥T 追加 `-3`（媒体视觉）、`-2`（媒体听觉） |
| **12** | `privacy_risk_dangerous_permissions_tip` 危险权限 | `ub.b` → `mc.c.w()` 同源 | 汇总计数 > 0（UI 上"危险权限"卡单独展示，实际数据源与 11 相同，见 `PrivacyRiskDetectionActivity.java:237-258`） |
| **13** | `privacy_risk_install_app_tip` 风险应用 | `ub.d` → `xc.h.a()` 桌面应用列表 → `ac.c.d(list)` 上传 `flash.sec.miui.com/user/ra` → `RiskAppInfoBean.getRiskList()` | **云端返回的包名集合** 与本地桌面应用求交集 |
| **14** | `privacy_risk_result_protection_tip` 缩略图模糊/屏幕遮挡 | `ub.c` | `SystemProperties.getBoolean("persist.sys.protect_image", false)`（`ub/c.java`）→ 若 true 表示"屏幕遮挡"模式，否则"模糊"模式（`detection/model/d.java:97-110`） |
| **15** | `privacy_risk_result_file_tip` 文件 | `d.e(context, b...)` 容器 | 需要外部塞入子项，当前 `N0()` 传的是 `r.get(18), r.get(17)`（均未被设置）→ **实际恒为 null，被 `removeAll(null)` 过滤**（`PrivacyRiskDetectionActivity.java:760,766`） |
| **16** | `privacy_risk_result_photo_delete` 照片删除保护 | `ub.a` + `vf.n.a(ctx)` | `df.h.J(df.h.m(Application.A()))`（`ub/a.java`）为 true 且 `vf.n.a()` 为 true 时提示（`PrivacyRiskDetectionActivity.java:545-551`） |
| **22** | `find_leak_risk` 输入法泄漏风险 | `ub.e` → `ec.c.f25249a` + `ec.c.j()`（**本地缓存的云端分类 JSON**） | 云端返回的 app→分类映射中，**不在用户"已关闭"名单**且 `fc.c.b(分类)` 为真的项存在即报警（`ub/e.java`），一键跳输入法切换器（`PrivacyRiskDetectionActivity.java:487-489`） |
| 21 | 固定推荐卡（自启动/垃圾清理/卸载） | `detection/model/c.java` | 非检测项，固定展示（`PrivacyRiskDetectionActivity.java:767-769`） |

**B. 危险权限的辅助计数（观测，`ub/b.java`）**

- DeviceAdmin 数量：`DevicePolicyManager.getActiveAdmins().size()`
- 通知监听器数量：`queryIntentServices(NotificationListenerService)` + `NotificationManager.isNotificationListenerAccessGranted()`，排除自身
- **UsageStats 应用数量**：反射 `IPackageManager.getAppOpPermissionPackages("android.permission.PACKAGE_USAGE_STATS", 0)` → 逐个 `AppOpsManager.getOpsForPackage(uid, pkg, {43})` 判定 mode==0 或 (mode==3 且 `checkPermission` 通过) → 再过滤 `isPackageAvailable` 且 `getApplicationEnabledSetting != 2`（`ub/b.java:52-160`）

**C. UI/跳转（观测）**

`i.a()`（`PrivacyRiskDetectionActivity.java:388-492`）按 item id 分派：
- `11/12` → `PrivacyPermissionsSetActivity`（`privacyType=10/11`），`startActivityForResult(100/101)`
- `13` → `RiskAppDeleteActivity.i0(..., 102)`（批量卸载风险应用）
- `14` → `PrivacyThumbnailBlurSettings.v0(this, 103)`
- `16` → `jg.a.e(this, true)`
- `22` → `InputMethodManager.showInputMethodPicker()`
- `21` → `miui.intent.action.OP_AUTO_START` / `GARBAGE_CLEANUP` / `GARBAGE_UNINSTALL_APPS`

结果回填：`onActivityResult` → `Q0(code)` 把 100..104 映射回 11..15，再 `T0()` 刷新脚本（`PrivacyRiskDetectionActivity.java:809-888,1095`）。

**D. 拉起者**：`miui.intent.action.PRIVACY_RISK_DETECTION_ACTIVITY`（exported，无权限）。安全中心首页/设置"隐私保护"。

---

### 4.4 `permcenter.privacyblur` — 隐私缩略图模糊（重点专项）

**结论：这里有"开关"，没有"算法"。**

- **开关落点（观测）**：`Settings.System."miui_recents_privacy_thumbnail_blur"`，值为 **逗号分隔的包名串**（分隔符 `aa.f23523b = ","`），由 `ec/c.java:74-116` 读写；空串表示无。
  - `PrivacyThumbnailBlurSettings.x0()`（`privacyblur/PrivacyThumbnailBlurSettings.java:334-338`）→ `ec.c.d(ctx,pkg,checked)` 写 System 设置 + `ec.c.f(pkg,checked)` 维护"用户显式关闭"名单 `miui_recents_privacy_thumbnail_blur_close`（走 remoteprovider 偏好）。
- **生效方（推断→强证据）**：读取方是**`com.miui.home`（MIUI 桌面）**，而不是本 App：
  - `ec.c.r()` 读 `com.miui.home` 的 `metaData "recents_blur_v2_enabled"`（`ec/c.java:231-240`）
  - `ec.c.u()` 读 `Settings.Secure."miui_home_recents_blur_mode"` ∈ {`recents_blur_mode_dim`, `recents_blur_mode_blur`}（`ec/c.java:262-277`）
  - `ec.c.s()` 探测 `com.android.settings` 是否声明 `com.android.settings.action.PRIVACY_THUMBNAIL_BLUR_SETTING`（`ec/c.java:242-246`）
  - `PrivacyThumbnailBlurSettings` 里出现的 `"recents_blur_v2_enabled"` / `"recents_blur_mode_dim"` 常量（`PrivacyThumbnailBlurSettings.java:67-76`）说明本页只是"写设置给桌面看"。
  - 因此 **`PRIVACY_THUMBNAIL_BLUR_SETTING` 模糊什么**：桌面多任务（Recents）里该应用的**任务快照缩略图**；V2 模式下为"变暗(dim)"或"模糊(blur)"两种呈现。
- **"屏幕遮挡"变体（观测）**：`SystemProperties.getBoolean("persist.sys.protect_image", false)`（`ub/c.java`）为 true 时 UI 标题切到 `privacy_thumbnail_shield_title`，并把本 Activity 的任务描述标签也改掉（`PrivacyThumbnailBlurSettings.java:306-310, 400-422`）——即"屏幕共享时遮挡而非模糊"。
- **云端自动开启（观测）**：`ec.c.w(ctx,z)`（30 天节流，`pref_key_last_upload_privacy_thumbnail_blur_time`）→ `ac.c.e(ac.c.f(apps))` POST `https://flash.sec.miui.com/user/cat` → 返回 `{pkg: 分类}` JSON 存 `privacy_thumbnail_blur_category` → 对不在关闭名单且分类判定为敏感的应用 `ec.c.d(ctx,pkg,true)` 自动打开（`ec/c.java:279-315`）。
- **共享期临时保护**：`ec.c.b()` / `ec.c.c()` 维护 `blur_screen_share_protection` 列表，`ec.c.x(ctx)` 在共享结束时把临时项移除（`ec/c.java:59-90,317-326`）。
- **算法在哪**：不在本 APK。`com.miui.blur` 是**另一套**（§4.14）。本 App 无任何像素处理代码。

---

### 4.5 `permcenter.provision` — 隐私额外告知（6 类，Kotlin）

- `PrivacyExtraProvisionActivity extends miuix.provision.ProvisionBaseActivity`（`provision/PrivacyExtraProvisionActivity.java:30`），`ProvisionViewModel`（`provision/c.java`）+ `ProvisionAdapter`（`provision/b.java`）。
- **功能（观测）**：在首次进入某些功能前，列出"该应用申请的权限 × 使用的隐私政策"，需用户逐项确认。文案 key：`privacy_extra_provision_require_permission`、`system_permission_declare_privacy_policy`（`provision/b.java:145,172`）。
- **隐私政策跳转**：`startHtmlViewer` = `Intent.ACTION_VIEW`（`provision/c.java:214-221`）；无网络时 toast `privacy_provison_no_network`。
- **进程**：`android:process="com.miui.securitycenter.remote"`（清单）。
- **数据**：无独立持久化；确认结果回写调用方（权限弹窗流程）。

---

### 4.6 `permcenter.capsule` — 灵动胶囊 / 屏幕共享保护（3 类）

| 类 | 功能 |
|---|---|
| `ScreenShareProtectionActivity` | 屏幕共享风险弹窗（投屏中检测到高风险应用） |
| `FlaresCapsuleOpenActivity` | 胶囊点击落地页（`MIUISafety-Flares` 日志域） |
| `b`（13.5 KB） | 胶囊数据/上报 |

**ScreenShareProtectionActivity 链路（观测）**
1. 由系统以 `Intent` 拉起，读 `IS_SCREEN_SHARE_HIGH_RISK_APP`(bool)、`android.intent.extra.PACKAGE_NAME`、`SHARED_PACKAGE_NAME`（`capsule/ScreenShareProtectionActivity.java:96-101`）。
2. 用户选"停止共享" → 反射 `MediaProjectionManager.stopActiveProjection()`（`:60-64`）。
3. 用户选"移除高风险应用" → `AppOpsUtilsCompat.setModeWithXSpace(..., OP_CAN_BE_RECORDED=10041, mode=0)`（`:65-69`）→ 广播 `com.miui.action.remove_screen_share_high_risk_app`（带权限 `miui.permission.READ_AND_WIRTE_PERMISSION_MANAGER`，`:72-74`）。
4. 该 Activity `exported=false`，由 `com.miui.securitycenter.remote` 进程内的胶囊服务/系统 UI 拉起（**推断**：无 filter，只能显式启动）。

---

### 4.7 `permcenter.settings` — 隐私设置聚合（26 类）

| 子功能 | 类 | 关键实现（观测） |
|---|---|---|
| **其他权限**（跨应用"其他权限"总览） | `OtherPermissionsActivity`(导出,需 `SYSTEM_PERMISSION_DECLARE`) + `OtherPermissionsFragment`(46 KB) | 聚合非运行时权限（自启动/后台弹出/锁屏显示/通知等），写入仍走 `g4.b` |
| **隐私输入模式** | `PrivacyInputModeActivity` + `PrivacyInputModeFragment` | 见下 |
| **关机密码** | `ShutDownPasswordActivity` + `ShutDownPasswordFragment` | 见下 |
| **隐形模式** | `InvisibleModeActivity` + `service.InvisibleModeService` | 见 §4.10 |
| **胶囊设置** | `FlaresSettingsActivity` | 纯 PreferenceFragment |
| **权限安装提示** | `InstalledPermissionDialog`（进程 `remote`） | — |
| **QS Tile 跳板** | `TileTransitActivity`（需 `BIND_QUICK_SETTINGS_TILE`） | — |
| **隐私告知** | `PrivacyProvisionActivity` | — |

**隐私输入模式（观测）**
- 设置键（`Settings.Secure`）：`miui_privacy_input_pkg_name`（当前生效 IME 包名）、`miui_privacy_input_enabled`、`miui_privacy_input_supported`；允许的 IME 白名单来自 `R.array.input_package_names`（`x4/k1.java` 的 `c/e/f/g/h/i/j`）。
- 交互：`PrivacyInputModeFragment.j0()` 绑定 `action="miui.intent.action.PREPARE_PRIVACY_INPUT_MODE"`，`setPackage(当前默认输入法包名)`（`:153-163`）；通过 `Messenger` 发 `what=1`（携带 `download_voice_package`），收 `what=2`（`result`）（`:117-182`）。
- 开关项：`CheckBoxPreference key="key_privacy_input_mode"`（`:323`）。
- 文案宣称能力：数据本地处理、禁用敏感权限、停止收集、离线语音输入（`R.string.data_local_processing_and_storage` 等，`:191-227`）——**属 UI 文案，是否落地取决于该 IME 实现（未知）**。
- **AIDL 边界**：无 AIDL，仅 `Messenger`（`android.os.Messenger`）IPC 到输入法进程。

**关机密码（观测）**
- `ShutDownPasswordFragment` 使用 `miui.security.SecurityManager.getAccessControlPasswordType()`（`SettingLockFragment.java:1159` 同款判断）——即**与应用锁共用同一套密码存储**（system_server）。`ShutDownPasswordActivity` 需 `SYSTEM_PERMISSION_DECLARE` 保护。
- 细节（存储键/有效期）→ **未解之谜**（本模块未发现独立 SP/DB）。

---

### 4.8 `permcenter.untrustwarn` — 未知来源安装告警（重点专项）

**观测结论：该包在 DEX 中不存在。**

- 清单声明：`<activity android:exported="false" android:name="com.miui.permcenter.untrustwarn.UnknownSourceWarnActivity"/>`（`AndroidManifest.xml`），**无 intent-filter**。
- 代码：`_analysis\jadxout\sources\com\miui\permcenter\` 下无 `untrustwarn` 目录；全树 grep `UnknownSourceWarn` / `untrustwarn` / `UNKNOWN_SOURCE_VERIFY` 均零命中。
- 同类悬空声明：`com.miui.permcenter.privacymanager.XSpaceWarningDialogActivity`（也缺类）。
- **实际的"未知来源安装"拦截在 `permcenter.install`**（见下），与 `untrustwarn` 无关。
- 判定：清单残留 / 组件已被移除但清单未清理。**是否仍由系统某路径拉起 → 未知**（静态无法否定）。

**`permcenter.install` 的安装校验链路（观测，`install/PackageVerificationReceiver.java`）**

```
系统包安装器 (com.miui.packageinstaller / com.android.packageinstaller)
   └─ 广播 com.miui.action.PACKAGE_NEEDS_VERIFICATION（带 permission=android.permission.INSTALL_PACKAGES 保护）
        └─ PackageVerificationReceiver.onReceive()
             ├─ 取 extras: "installerPackage"、IBinder "observer"(IMessenger)、session id
             ├─ IMessenger.Stub.asInterface(observer)  (:219)
             ├─ 白名单 installer: {com.miui.packageinstaller, com.google.android.packageinstaller, com.android.packageinstaller}  (:40-42)
             ├─ Google/Play 场景单独广播 com.miui.global.packageinstaller.action.verifypackage
             │     带权限 com.miui.securitycenter.permission.GLOBAL_PACKAGEINSTALLER  (:47-53)
             └─ 结论回执: IMessenger.send(msg)，"verify package X, finish: true/false"  (:116-127,168-208)
                  · 签名校验失败 → false（"can't install, signature verify fail"）
                  · webview installer 白名单拒绝 → false
                  · 通过 → true
```

- `AdbInstallActivity`（`com.miui.securitycenter.intent.action.INSTALL_PACKAGE`，exported）处理 **ADB 安装**确认弹窗，读 `android.content.pm.extra.SESSION_ID` 与 `IBinder "observer"`（`install/AdbInstallActivity.java:176-188`），布局 `pm_adb_install_alert`。
- 相关开关：`security_adb_install_enable`（`RemoteProvider.h()` 专门做进程级鉴权，只允许本包或 `com.miui.klo.bugreport` 写，`com/miui/common/persistence/RemoteProvider.java:174-190`）。
- `AdbInputApplyActivity`（`com.miui.securitycenter.action.ADB_INPUT_APPLY`）— ADB 输入授权。

---

### 4.9 `permcenter.acrossterminal` — 跨端权限（3 类 + provider）

- **存储（观测）**：`/data/misc/user/<userId>/securitycenter/across_terminal.xml`，目录 `/data/misc/user/<userId>/securitycenter/`（`permissions/acrossterminal/b.java:28-31`）。XML 结构：
  ```xml
  <runtime-permissions version="1">
    <account AccountId="<小米账号 name>">
      <package TerminalId=".." TerminalName=".." TerminalType="..">
        <permission name=".." action=".." onetimeFlag=".."/>
  ```
  （`:56-102` 写；`:104-168` 读；账号来源 `we.e.c(context)` + 账号变更回调 `b.f18331e`，`:39-54,170-173`）
- **Provider 方法（观测，`AcrossTerminalPermissionContentProvider.call()`）**

| method | 参数 | 返回 |
|---|---|---|
| `get_all_terminal` | — | `extra_data` = 该账号下所有终端 |
| `get_all_permission` | `terminalId` | 该终端的权限表 |
| `check_permission` | `terminalId`、`permissionName`、`terminalName`、`onetimeFlag` | `action`（int）；若为 2(PROMPT) 则记录一次提示 `kc.q.G(ctx).D(perm, terminalId)` |
| `update_terminal_name` | `terminalId`、`terminalName` | — |
| `update_terminal_permission` | `terminalId`、`permissionName`、`terminalName`、`terminalType`、`action`、`onetimeFlag`、`override_onetime` | — |
| `temp_close_capsule` | bundle | → `z4.d.j(ctx,bundle)`（临时关闭胶囊） |
| `notify_terminal_privacy` | bundle | → `kc.q.G(ctx).U(bundle)`（通知终端隐私状态） |

- **权限名映射**：`dc/d.java`（`dc.d.a(permissionName)`），跨端权限是**字符串权限名**而非 `long PERM_ID`。
- **服务依赖**：`kc.q` = 跨端（continuity）服务单例，**推断**对应 `com.xiaomi.continuity`（清单中有 `com.xiaomi.continuity.InitializationProvider`），并在 manifest 里注册了 `com.miui.continuity.netbus.*`。
- **拉起者**：无 filter，`exported=true` + `SYSTEM_PERMISSION_DECLARE` 保护 → 只有持该权限的系统组件（设置、跨端服务）能调用。

---

### 4.10 `permcenter.service.InvisibleModeService` — 隐形模式（观测）

**这不是"输入隐私"，而是"相机/麦克风/定位指示灯隐身"**。

- `onStartCommand` → `b()`（`service/InvisibleModeService.java:97-120`）
- 影响 op 集合：`{0, 1, 41, 42, 27, 26}` = `COARSE_LOCATION, FINE_LOCATION, MONITOR_LOCATION, MONITOR_HIGH_POWER_LOCATION, RECORD_AUDIO, CAMERA`（`:37`）
- 目标包：
  - `27 (RECORD_AUDIO)` → `com.miui.voicetrigger`
  - `26 (CAMERA)` → `com.miui.face`
  - 其它 → `com.android.camera`（仅 SDK==31 且 `IS_SUPPORT_CDD` 时）或 `null`（`:51-53`）
- 手段：反射 `AppOpsManager.setUserRestrictionForUser(int code, boolean, IBinder token, String[]/PackageTagsList, int userId)`（`:63-96`），并对用户 `0` 与 `999`（XSpace）各做一遍（`z1.r()` 判定）
- 隐藏状态栏图标：`StatusBarManager.setIconVisibility("stealth", bool)`（`:41-59`）
- 广播：`com.miui.action.sync_status_bar`（权限 `miui.permission.READ_AND_WIRTE_PERMISSION_MANAGER`）
- 本地开关判断：`o.a.b(this)`；能力开关 `o.f18016m`
- **数据**：无持久化（每次进入按当前设置重放）。

---

### 4.11 `permcenter` 其他子包

| 子包 | 类数 | 说明（观测） |
|---|---|---|
| `privacymanager` | 12 | 拦截型权限弹窗（`SpecialPermissionInterceptActivity`、`InterceptPermissionFragment`、`InterceptMIUIFragment`、`InterceptBaseFragment`、`DeviceManagerApplyActivity`、`AppBackgroundManagerActivity`）、行为小组件 `widget/BehaviorWidget`+`widget/a`(17.7 KB)。`miui.intent.action.SPECIAL_PERMISSIO_NINTERCEPT`（原文拼写，缺 `N`）、`miui.intent.action.MANAGER_BACKGROUND`、`com.miui.securitycenter.action.DEVICE_MANAGER_APPLY` |
| `privacycenter` | 5 + usage 4 | `PrivacySafetyActivity`（`PRIVACY_SETTINGS`/`SECURITY_CENTER_SETTINGS`/`PRIVACY_SAFETY`）、`PrivacyFragment`(25 KB)、`SecurityFragment`(25.9 KB)、`PrivacyCenterProvider`（`setCTAStatus`/`queryShowCTA`/`queryCTAStatus`/`privacy_provider_get_using`）。`usage/*`：`AppPermissionUsageActivity`(59 KB)、`PermissionUsageRecordActivity`(52 KB)、`PermissionUsageTaskActivity`、`RecentBehaviorPreference`(14 KB) |
| `monitor` | 3 | `AppUsagePersistService`（`miui.intent.action.persist_behavior`）、`MaliciousBehaviorTrackService`、`a`(25.5 KB)/`b`(13.9 KB)。读 `PermissionContract.RECORD_URI`（`monitor/a.java:349`：`permissionId,pkgName,calleePkg,endTime,count,op` + `calleePkg=='null'` + `_id DESC`）；阈值 key `record_threshold`（`monitor/b.java:59`）；云控 `get_cloud,behavior:...,include_system:...,record_threshold:...` |
| `isolate` | 2 | `SandboxManagerService`（12.3 KB，`exported=false`，进程 `remote`）+ `a`(9 KB)。应用沙箱/隔离（`miui.security` 侧） |
| `install` | 8 | 见 §4.8；另有 `PackageManagerActivity`(17.9 KB)、`RiskAppAuthActivity`/`History`、`AdbInstallVerifyActivity`、`UnknownSourceVerifyActivity`（`com.miui.securitycenter.action.UNKNOWN_SOURCE_VERIFY`，**注意：这是"未知来源校验"页，不是 `untrustwarn`**） |
| `root` | 5 | `RootApplyActivity`、`RootManagementActivity`、`NotSupportRootActivity`、`RootUpdateReceiver`(`com.android.updater.action.ACQUIRED_ROOT_SUCCESSED`) |
| `wakepath` | 3 | 唤醒路径管理（`WakePathManagerActivity`/`ListFragment`/`DetailFragment`），与 permmgr method 8 `WRITE_WAKE_PATH_WHITE_LIST` 对应 |
| `compact` | 8 | 兼容层：`AppOpsUtilsCompat`、`PermissionManagerCompat`、`MiuiSettingsCompat`、`SystemPropertiesCompat`、`IntentCompat`（`getIBinderExtra` 反射）、`ReflectUtilHelper` |
| `model` / `utils` / `widget` / `detection` | — | 数据 bean（`LocalAppInfoBean`、`AppPermissionInfo`）、`PlaceHolderPreference`、`ContentPressPreference` |
| 顶层杂类 `a…l.java`/`n.java`(57 KB)/`q.java`(29 KB)/`l.java`(33 KB) | — | 混淆后的工具/控制器（`n` 含 `y(ctx,boolean,List<Long>,xc.c)` 权限批量查询、`r()` 等） |

---

### 4.12 `applicationlock` — 应用锁（重点专项）

**A. 功能清单（观测）**

| 功能 | 类 |
|---|---|
| 首次使用引导 | `FirstUseAppLockActivity`（`com.miui.securitycenter.action.LOCK_APP`） |
| 应用锁列表/开关 | `AppLockManageFragment`(51 KB)、`PrivacyAndAppLockManageActivity` |
| 设置锁类型（图案/数字/混合密码/指纹/人脸） | `ChooseLockTypeActivity`、`ChooseAccessControl`(42.5 KB)、`SettingLockFragment`(47.6 KB)、`SettingLockActivity` |
| 解锁界面 | `ConfirmAccessControl`(108.7 KB)、`ConfirmAccountActivity`（忘记密码） |
| 通知内容遮挡 | `MaskNotificationActivity` |
| 推荐加锁 | `ApplockRecommendActivity` |
| 隐私应用解锁 | `PrivacyAppsConfirmAccessControl` |
| 控件 | `LockPatternView`(31.8 KB)、`MiuiKeyBoardView`(25.2 KB)、`MiuiNumericInputView`、`NumberPasswordEditText`、`PhoneGLSurfaceView`（人脸预览）、`PasswordUnlockMediator`、`BindAccountDialog` |

**B. 解锁主链路（观测）**

```
系统（AMS/AppLock 拦截）检测到受保护 App 进入前台
  └─ 显式启动 applicationlock.ConfirmAccessControl
       · Intent 带 IBinder "android.app.extra.PROTECTED_APP_TOKEN"      (ConfirmAccessControl.java:1991-1993)
       · 也支持 miui.intent.action.CHECK_ACCESS_CONTROL（exported）
  └─ onCreate:
       securityManager = (SecurityManager) getSystemService("security")   (:1368)
       mPasswordType   = securityManager.getAccessControlPasswordType()   (:1373)  // "numeric"/"mixed"/"pattern"
       根据类型 inflate 对应解锁控件（widget/e 子类：q=数字、LockPatternView-based=图案、混合密码 EditText）(:1384-1401)
       securityManager.exemptTemporarily(...)                            (:999)
  └─ onResume:
       · 若 passwordType 变化 → finish()                                 (:2485-2489)
       · needFinishAccessControl(activityToken) == true → finish()        (:2520-2530)  // 受保护 App 已不在前台
       · 倒计时/指纹禁用态处理                                            (:2532-2549)
  └─ 用户输入:
       数字密码 → widget/q.A(pwd) → securityManager.checkAccessControlPassword("numeric", pwd)  (widget/q.java:129-136)
       图案/混合 → 各自控件 → SecurityManager 校验（同源）
       指纹 → AppLockManageFragment 内 Fingerprint 回调 → c3.f.b(code, ctx)   (AppLockManageFragment.java:522)
       人脸 → 回调链 onFaceAuthenticated/onFaceLocked/onFaceHelp/onFaceAuthFailed/onFaceStart  (AppLockManageFragment.java:575-627)
  └─ 校验成功 → c3.f 放行: securityManager.addAccessControlPass(pkg)        (c3/f.java:250)
                并 removeAccessControlPass 清理           (c3/f.java:391-396)
```

**C. 安全边界（观测）**

- **密码/图案从不落盘在本 APK**：设置=`SecurityManager.setAccessControlPassword(pwd,type)`（`applicationlock/ChooseAccessControl.java:509`；清空=`c3/f.java:547` 传 `(null,null)`），校验=`checkAccessControlPassword(type,pwd)`。→ 哈希与比对在 system_server。
- **放行凭证是 system_server 的"通过票据"**：`addAccessControlPass(pkg)` / `checkAccessControlPass(pkg)` / `checkAccessControlPassAsUser(pkg,user)`（`c3/f.java:435,478`）。App 进程被攻破也无法自行"凭空"造票据（票据只在 SecurityManager 内存里）。
- **遮罩与限制**：`ChooseAccessControl.java:502` 反射 `setUserRestriction(op, bool, IBinder)`（限制设置期间的行为）。
- **通知内容遮挡**：`MaskNotificationActivity` + `SecurityManager.getApplicationMaskNotificationEnabledAsUser(pkg,userId)` / `setApplicationMaskNotificationEnabledForUser(pkg,bool,userId)`（`applicationlock/f.java:230`、清单同族调用点）。隐私应用时还会用 `INotificationManager.setNotificationsEnabledForPackage` 直接关通知（`se/e.java:96-118`）。
- **忘记密码**：`ConfirmAccountActivity` 绑定 `IXiaomiAccountService.Stub.asInterface(iBinder)`（`ConfirmAccountActivity.java:174-177`）；小米账号验证通过后 `forgetPrivacyPassword` 并写 `gb_xiaomi_id_md5_key`（Base64，`c3/w.java:60-80`）。→ 恢复路径依赖小米账号，**本地无后门**（观测范围内）。
- **副作用**：`AppLockManageFragment` 记录 `cancel_fingerprint_verify_times`/`cancel_fingerprint_guide_times`（`:178,208,426,431`）用于抑制引导弹窗。

**D. 数据与持久化（观测）**

- 应用锁密码/图案、受保护包名列表、通知遮挡开关：**全部在 system_server 的 `SecurityManager`**（本 APK 无对应 SP/DB）。
- 本地仅有统计/引导类键（走 remoteprovider）：`applock_alarm_count`、`applock_LockModePrompt`、`applock_notifycation_clicked`、`applock_verify_and_activate_fingerprint_<userId>`、`gb_xiaomi_id_md5_key`（`c3/f.java:513,612,665,138-153,214-222`）。
- `applock_unlock_mode`、`applock_countDownTimer_deadline` 写 `Settings.Secure`（`c3/f.java:535,624`）。

---

### 4.13 `privacyapps` — 隐私应用 / 隐私空间（重点专项）

**A. 功能与链路（观测）**

- **存储隔离的实现位置**：数据真源是 `miui.security.SecurityManager`：
  - `isPrivacyApp(pkg, userId)`、`getAllPrivacyApps(userId)`、`setPrivacyApp(pkg, userId, isPrivacy)`（`se/a.java:47-66`、`se/c.java:65-97`、`se/e.java:120`）
  - 遍历 `UserManager.getUserProfiles()` → **按 Android 多用户 profile 隔离**（不是自建 VirtualUser）
- **开关与状态（`Settings.Secure`）**（`se/a.java`）
  - `is_privacy_apps_enable`（总开关，`:44-46`）
  - `is_privacy_apps_password_enable`（是否用密码保护隐私空间，`:33-35`；`-1` = 未初始化）
  - `privacy_apps_shield_message_enable`（消息遮挡，`:29-31`）
  - 首次引导标记走 remoteprovider：`is_first_setting_privacyapp`
- **启用隐私应用时的连带动作（`se/e.java`）**：`g(ctx,pkg,userId,isPrivacy)`
  1. `SecurityManager.setPrivacyApp(pkg,userId,isPrivacy)`
  2. `ContentResolver.notifyChange(content://com.miui.securitycenter.provider/update_privacyapps_icon)`（`pe/a.java`）— 通知桌面刷新图标
  3. 广播 `com.miui.gamebooster.PPRIVACYAPP`（带 `pkgName`/`isPrivacy`/`userId`），权限 `com.miui.dock.permission.DOCK_EVENT`（`:54-60`）
- **加入隐私空间时的清理（`se/e.java:104-176`）**
  - 从最近任务移除：`ActivityManager.getRecentTasks()` 匹配 `persistentId` → `AppManageUtils.q0(persistentId)`（`:130-150`）
  - 杀进程：`AppManageUtils.m(activityManager, pkg, userId)`（`:170`）
  - 关闭该应用通知：反射 `INotificationManager.setNotificationsEnabledForPackage(pkg, uid, false)`（`:96-118`）
  - 全部在 `ef.z.d().b(...)`（后台单线程）执行
- **白名单回收**：`se/c.java` 的 `recoverWhiteApps()` 对 `{com.android.deskclock, com.miui.securitymanager}` 强制移出隐私空间并恢复通知（`:33-34,58-92`）
- **`app.hide` 入口伪装（观测）**：`PrivacyAppsProvider` authority = `com.miui.securitycenter.app.hide`，但 `call()` **只支持 `isPrivacyApp`**，别的 method 直接返回 null（`privacyapps/PrivacyAppsProvider.java:39-56`）。→ 名字是"hide"，实际是"查询某包是否在隐私空间"，供桌面/系统判断是否隐藏图标与任务。**没有**真正的数据读写接口。
- **入口**：`PrivacyAppsHelper`（需 `miui.permission.USE_INTERNAL_GENERAL_API`）先 `se.a.c()` 判断（有密码且已启用）→ 跳 `PrivacyAppsConfirmAccessControl` 验证 → `setResult` 后再进 `PrivacyAppsActivity`（`privacyapps/ui/PrivacyAppsHelper.java:30-46`）。
- `PrivacyAppsActivity` 会 `ConfirmAccessControl` / `LockChooseAccessControl` 复用应用锁（`privacyapps/ui/PrivacyAppsActivity.java:157-160`）。

**B. 是否存在文件加密 / 独立用户**：**未观测到**。隐私空间 = 同一 userId 下的应用集合 + 桌面/任务/通知的隐藏处理；**未见** per-app 加密、`VirtualUser`、独立挂载命名空间代码。→ 若 MIUI 真有存储隔离，其实现不在本 APK（**未知/在系统侧**）。

**C. 数据**：`SecurityManager`（system_server）+ `Settings.Secure` 三个键 + `update_privacyapps_icon` URI 作为刷新信号。无 DB。

---

### 4.14 `com.miui.blur` — 背景虚化 SDK（澄清）

- 目录 `com/miui/blur/sdk/backdrop/`，18 类；**纯图形 SDK，与隐私无关**。
- 能力开关：`BlurManager.f12315a = (SDK==30) && 存在 android.view.MiuiCompositionSamplingListener && ro.miui.backdrop_sampling_enabled && ro.miui.ui.version.code>=11`（`blur/sdk/backdrop/BlurManager.java:84`）；SDK≥31 走 `f12316b` 分支（`:85`）。
- 数据通路：`MiuiCompositionSamplingListener.register(listener, 0, SurfaceControl, 16.0f, periodNs)`（`p.java:388-392`）→ SurfaceFlinger 合成采样 → `GraphicBuffer` → RenderScript `ScriptIntrinsicBlur`（`p.java:174-176`）+ `Allocation`（`p.java:127-129`）。
- SurfaceControl 取得靠反射 `ViewRootImpl.getSurfaceControl()`（`b.java:57-61`、`u.java:41-47`、`p.java:388`）。
- **与本报告"隐私模糊"的关系：仅名称相近。** 权限/隐私模块未引用 `com.miui.blur`（`permcenter`/`privacyapps`/`applicationlock` 均无 import）。

---

### 4.15 `idprovider` — OAID / 设备标识（重点专项）

**A. OAID 授权弹窗（观测，`idprovider/ui/GrantPermissionActivity`）**

1. 触发：App 调 `AppOpsManager.noteOp(OP_GET_OAID_USER=10038)` 被 MIUI 判为"需询问"时，系统拉起 `miui.intent.action.REQUEST_PERMISSIONS_FOR_OAID`（exported，无权限）。
2. `onCreate`：
   - `d.g()` 特性开关不满足 → finish（`:118`）
   - `getCallingPackage()` 为空 → finish（`:119-124`）
   - `getLaunchedFromUid()` 反射取调用方 uid，为 0 → finish（`:132-137`）
   - `d.f36228a.b(this)`（策略允许弹窗）否则直接拒绝并回结果（`:143-147`）
   - `AppOpsUtilsCompat.checkOpNoThrow(appOps, callingPkg, uid, OP_GET_OAID_USER)`：**值 ≠ 5(MODE_ASK) 时直接回结果**（已允许/已拒绝不再打扰）（`:149-157`）
3. 弹窗文案 `permission_oaid_desc`（含应用名，`:169-171`），按钮允许/拒绝（`:173`）
4. 结果写回（`:69-99`）：
   ```
   setResult(RESULT_OK, {"REQUEST_PERMISSIONS_RESULTS": 0=允许 / 1=拒绝})
   AppOpsUtilsCompat.setMode(appOps, callingPkg, uid, OP_GET_OAID=10037,  allow?1:0)
   AppOpsUtilsCompat.setMode(appOps, callingPkg, uid, OP_GET_OAID_USER=10038, allow?1:0)
   ```
   → 注意：`z10 = (i10==1)` 表示"点了拒绝按钮"，代码写 `!z10 ? 1 : 0`，即**允许=1、拒绝=0**（与 AOSP 的 0=ALLOWED/1=IGNORED 相反，是 lbe/MIUI 侧语义，**观测但语义存疑，标注推断**）。
5. 埋点：`1127.35.0.1.35763`（弹窗展示）、`1127.35.0.1.35764`（点击，含 `click_content=允许/拒绝`）（`:66,183-186`）

**B. `IdProvider`（31 KB，authority `com.miui.idprovider`，进程 `remote`）**：OAID/AAID/VAID 的读取面。能力开关：`AppOpsUtilsCompat.isSupportOAIDApps()` = `!国际版 && 10037 < MIUI_OP_END && (miuiVersion>11 || 开发版)`（`permcenter/compact/AppOpsUtilsCompat.java`）。
**C. 设置页**：`OAIDSettingsActivity`、`OAIDAppSettingsActivity`（按应用管理）、legacy 版 `OAIDAppsActivity`/`OAIDSettingsLegacy`（`miui.intent.action.OAID_APPS`）。
**D. 数据**：OAID 值与授权表在 `com.miui.idprovider` 进程自身（`IdProvider` 内）；本 APK 侧无 DB。
**E. 未解之谜**：OAID 生成算法（是否随机 UUID + 重置策略）、`IdProvider` 的完整 method 列表（未逐方法展开）。

---

### 4.16 `appmanager` — 应用管理（重点专项）

**A. 功能清单（观测）**

| 功能 | 实现 |
|---|---|
| 应用列表（全部/用户/系统） | `AppManagerMainActivity`（`miui.intent.action.APP_MANAGER`）、`fragment/ManageFragment`(76 KB)、`fragment/AppFragment` |
| 应用详情（含权限/存储/通知/联网） | `ApplicationsDetailsActivity`（`miui.intent.action.APP_MANAGER_APPLICATION_DETAIL`）、`fragment/ApplicationsDetailsFragment`(135 KB) |
| 存储详情 | `AMAppStorageDetailsActivity`、`fragment/AMStorageDetailsFragment` |
| 设置页（含"停用/启用"） | `AppManagerSettings`、`fragment/AMSettingsFragment` |
| 应用信息 | `AMAppInfomationActivity`、`fragment/AMAppInformationFragment` |

**B. 关键动作与系统调用（观测，`appmanager/AppManageUtils.java`，60.7 KB）**

| 动作 | 调用 |
|---|---|
| **批量卸载** | `deletePackageAsUser`：SDK>25 用 `(String, int, IPackageDeleteObserver, int, int)`，否则 `(String, IPackageDeleteObserver, int, int)`（`:930-955`） |
| **清缓存** | `IPackageDataObserver.Stub`（`ClearCacheObserver`，`:124`）+ `deleteApplicationCacheFiles`（`:861`） |
| **清数据** | `ClearUserDataObserver`（`:141`）+ `clearApplicationUserData`（`:880`） |
| **停用/启用** | `PackageManager.setApplicationEnabledSetting(pkg, state, flags)`（`fragment/AMSettingsFragment.java:161` 启用；`fragment/ApplicationsDetailsFragment.java:720` 停用） |
| **通知开关** | `INotificationManager.areNotificationsEnabledForPackage` / `setNotificationsEnabledForPackage`（`:778,1319`） |
| **WiFi 扫描触发**（用于刷新列表的联网状态） | `IWifiManager.startScan`（3 个重载，`:430,446,461`） |
| **窗口/任务** | `IWindowManager`（`:675`）、`removeTask(persistentId)`（`q0`，`:1099`） |
| **用量统计** | `UsageStatsManager.queryUsageStatsAsUser(4, from, to, userId)` 反射（`:985-1008`） |
| **隐私应用联动** | `h0(ctx,userId)` 检查 `getAllPrivacyApps` 可用（`:906-916`）；`z0(pkg,userId,bool)` 改通知开关（`:1317`） |
| **XSpace 检测** | `MiuiSettings$Secure.getBoolean("xspace_enabled")`（`:957-964`） |

**C. 与"权限管理入口"的连接**：应用详情里点"权限"→ 拉起 `miui.intent.action.APP_PERM_EDITOR`（`extra_pkgname`）。批量卸载走的仍是 `IPackageManager`，与权限中心无共享状态。
**D. 数据**：无自有 DB；`r4.a`(remoteprovider) 存 `am_update_clicked` 等 UI 标记（`:926-928`）。

---

### 4.17 `devicepermission` — 设备权限（跨设备共享类权限）

- 场景：`com.miui.permission.SCREEN`（跨设备"屏幕"权限）等设备级权限的授权/编辑。
- 组件：`GrantDevicePermissionActivity`（`miui.intent.action.REQUEST_DEVICE_PERMISSIONS`）、`DevicePermissionEditorActivity`（`EDITOR_DEVICE_PERMISSIONS`）、`DevicePermissionSettingsActivity`（`DEVICE_PERMISSION_SETTINGS`）、`DevicePermissionModifyActivity`；后三者均需 `...SYSTEM_PERMISSION_DECLARE`。
- 权限项来源：`dc/d.java:29-77` 的映射表（`com.miui.permission.SCREEN` → `R.string.connected_devices_screen*`）。
- 写入：`grantpermission/a.java`（8.1 KB）+ `editpermission/DevicePermissionModifyFragment.java`（16.9 KB）→ 走同一 `g4.b` / `PermissionManager` 链路；`DevicePermissionSettingsActivity` 还会 `b0(this,"com.miui.permission.SCREEN", true)`（`DevicePermissionSettingsActivity.java:48`）。

---

### 4.18 `combinepermission` — 组合权限授权弹窗（CTA）

- 组件：`CombinePermissionActivity`（`REQUEST_PERMISSIONS_COMBINE` / `CTA_REQUEST_PERMISSIONS_COMBINE`，需 `SYSTEM_PERMISSION_DECLARE`，`noHistory=true`）。
- 入参（`CombinePermissionActivity.java:164-177`）：`miui.intent.extra.REQUEST_PERMISSIONS_NAMES`、`..._NAMES_DESC`、`...KEY_REQUEST_PERMISSIONS_STATE`、`...PERMISSIONS_DIALOG_DESC`、`...PACKAGE_NAME`。
- 出参（`:130-143`）：同名 names + `miui.intent.extra.REQUEST_PERMISSIONS_RESULTS`（int[]）。
- 提交流程（`combinepermission/grantpermission/a.java:371-390`）：遍历 `PermissionPreferenceFragment.d0()` 得到的 `LinkedHashMap<permissionId, action>`，当 `m.d(id)`（是否为权限组）成立走 `g4.b.l(...)`，否则走 `g4.b.m(...)` → 双写（§3.4）。
- 特征开关常量：`f12476n = "miui.intent.action.CTA_REQUEST_PERMISSIONS_COMBINE"`（`:32`）——用于区分"普通组合"与"CTA 场景"。

---

### 4.19 `appcompatibility` — 应用兼容性提示（1 类）

- `AppExcepitonTipsActivity`（原文拼写），action `com.miui.appcompatibility.LaunchDialog.launcher` / `.appstore`，`Theme.Light.NoTitle.Transparent`。
- 功能：应用闪退/不兼容时弹提示并引导升级（Google Play / 应用商店）。与权限/隐私无耦合。

---

### 4.20 `com.miui.permission` — 权限 SDK（16 类，全局基石）

| 类 | 关键内容 |
|---|---|
| `PermissionManager` | 115 个 `PERM_ID_*`（long 位掩码）、`GROUP_*`、`ACTION_*`、`TYPE_*`、`virtualMap`、`calculatePermission*/calculatePermissionCount/supportAlwaysAllow/supportCloseBehaviorRecord`、8 个 provider method 封装 |
| `PermissionContract` | authority/method 码/表结构（§3.2、§3.3） |
| `StoragePolicyContract` | `com.lbe.security.miui.storagepolicy`，`SPLIT_PACKAGE_OP="@"`、`SPLIT_MULTI_PATH`（用于 `ec.c.z()` 拼多值串） |
| `RequiredPermissionsUtil` | MIUI 扩展权限字符串：`PERMISSION_GET_INSTALLED_APPS`、`PERMISSION_SHAKE`、`PERMISSION_READ_RECORD`、`PERMISSION_READ_NOTE` 等（用于 `permissionToOpCode` 特判） |
| `StaticGroup` | 分组白名单（剪贴板/通话联系人/短信MMS/存储） |
| `AnalyticNameUtil` | 权限 → 埋点名 |
| `support/util/*` | `ReflectUtil`、`SdkLevel`、`MiuiCommonUtil`、`SystemPropertiesCompat`、`ConcurrentUtils` |

---

## 5. AIDL / 系统服务边界汇总（必须预装 + 系统权限的证据）

> **重要更正**：任务书中假设的 `miui.security.IPermissionManager` / `AppOpsService` AIDL **在本 APK 中不存在**。真实边界如下（全部为观测）：

| # | 边界 | 形态 | 证据 |
|---|---|---|---|
| 1 | `com.lbe.security.miui`（LBE 权限管家）**ContentProvider** | `ContentResolver.call(Uri, methodCode, arg, bundle)`；URI `content://[<userId>@]com.lbe.security.miui.permmgr` | `com/miui/permission/PermissionContract.java:10,15`；`com/miui/permission/PermissionManager.java:302,356,364,374,381,395,437`；`permcenter/autostart/AutoStartManagementActivity.java:414` |
| 2 | `miui.security.SecurityManager`（system_server 的 `"security"` 服务，**框架类非本 APK**） | 直接强转调用（非 AIDL 生成代码）：`getSystemService("security")` | `applicationlock/ConfirmAccessControl.java:1368`；`c3/f.java:250,391-396,435,478,547`；`se/e.java:120`；`se/a.java:47-66`；`privacyapps/PrivacyAppsProvider.java:79`；`permcenter/service/InvisibleModeService.java`（间接） |
| 3 | `ISecurityManager$Stub`（反射直取） | `Class.forName("miui.security.ISecurityManager$Stub").asInterface(ServiceManager.getService("security"))` → `getCurrentUserId()` | `m2/j.java:17`（**唯一一处显式走 Binder 的 security 调用**） |
| 4 | `AppOpsManager` 隐藏 API | 反射 `checkOp/checkOpNoThrow/noteOpNoThrow/setMode/setUidMode/startWatchingActive/startWatchingNoted/permissionToOpCode/strOpToOp/unsafeCheckOpRawNoThrow/opToSwitch/opToPermission` | `com/miui/permcenter/compact/AppOpsUtilsCompat.java`、`android/app/AppOpsManagerCompat.java`、`ub/b.java:112-160` |
| 5 | `AppOpsManager.setUserRestrictionForUser` | 反射（签名随 SDK 变：SDK≤30 `String[]`，SDK>30 `PackageTagsList`） | `permcenter/service/InvisibleModeService.java:63-96`；`applicationlock/ChooseAccessControl.java:502` |
| 6 | `IPackageManager` | 反射 `asInterface` → `getAppOpPermissionPackages` / `isPackageAvailable` / `getApplicationEnabledSetting` / `deletePackageAsUser` / `grantRuntimePermission` / `revokeRuntimePermission` / `updatePermissionFlags` | `ub/b.java:52-160`；`appmanager/AppManageUtils.java:930-955,1109`；`se/e.java:70-90`；`tg.a.*` |
| 7 | `INotificationManager` | 反射 `areNotificationsEnabledForPackage` / `setNotificationsEnabledForPackage` | `appmanager/AppManageUtils.java:778,1319`；`se/e.java:68-116` |
| 8 | `IWindowManager` | 反射 | `appmanager/AppManageUtils.java:675` |
| 9 | `IWifiManager` | 反射 `startScan`（3 重载） | `appmanager/AppManageUtils.java:430,446,461` |
| 10 | `IUsbManager` | 反射（默认 USB 配置清理 / `clearDefaults`） | `appmanager/fragment/ApplicationsDetailsFragment.java:1076`；`AppManageUtils.i()`（`clearDefaults`） |
| 11 | `StatusBarManager.setIconVisibility("stealth")` | 反射 | `permcenter/service/InvisibleModeService.java:41-59` |
| 12 | `MediaProjectionManager.stopActiveProjection` | 反射 | `permcenter/capsule/ScreenShareProtectionActivity.java:60-64` |
| 13 | **本 APK 自有 AIDL**：`ISecurityCenterNotificationListener` | `.Stub.asInterface()`（服务端在别处，本 APK 为客户端） | `permcenter/i.java:144` |
| 14 | **本 APK 自有 AIDL**：`IMessenger`（`android.os.IMessenger`） | 包安装器回调通道 `IBinder "observer"` | `permcenter/install/AdbInstallActivity.java:178-182`；`install/PackageVerificationReceiver.java:217-221` |
| 15 | `IXiaomiAccountService` | `.Stub.asInterface()`（忘记密码/账号绑定） | `applicationlock/ConfirmAccountActivity.java:174-177` |
| 16 | `IPackageDataObserver`（AIDL Stub 实现） | 清缓存/清数据回调 | `appmanager/AppManageUtils.java:124,141` |
| 17 | `IPackageDeleteObserver` | 卸载回调 | `appmanager/AppManageUtils.java:930-955` |
| 18 | `android.permission.PermissionManager`（内部 `mPermissionManager` 字段） | 反射取字段 | `g4/b.java:116` |
| 19 | `android.miui.AppOpsUtils.isXOptMode` | 反射静态 | `AppOpsUtilsCompat.java` `isXOptMode()` |
| 20 | `android.app.AppOpsManagerInjector.isAutoStartRestriction` | 反射静态 | `AppOpsManagerCompat.java` `isAutoStartRestriction()` |

**"必须预装"的证据**：
- `content://com.lbe.security.miui.permmgr` 的 authority 属于**另一个包 `com.lbe.security.miui`**（`PermissionContract.java:10` 的字面量 + `PermissionManager.java:511` 对它 `getPackageInfo`）→ 本 App 依赖该预装包才能工作。**观测**。
- 清单里 `android:sharedUserId` 需核对（**未核**，见 §9）。
- 大量 `permission="com.miui.securitycenter.permission.*"` 自签权限 + `miui.permission.USE_INTERNAL_GENERAL_API` 保护自家组件 → 典型的系统应用签名级权限模型。

---

## 6. 数据与持久化汇总

| 类型 | 位置 | 内容 | 证据 |
|---|---|---|---|
| **SharedPreferences** | `remote_provider_preferences` | 所有 `r4.a.*` 开关（行为记录、缩略图模糊分类与时间戳、自启动埋点、应用锁引导计数、`security_adb_install_enable` 等） | `com/miui/common/persistence/RemoteProvider.java` `onCreate()`；`r4/a.java` |
| SP | `device_owner_shared_preferences` | DeviceOwner 变更记录 | `permcenter/DeviceOwnerChangedReceiver.java:102` |
| SP | `privacy_sdk` | 隐私 CTA 节流时间（`<policy>_privacy_query_time` / `_privacy_update_time`） | `ue/i.java:24` |
| SP | `credential_service_config` | `is_credential_configured` | `com/miui/securitycenter/Application.java:542,547` |
| **文件** | `files/privacypolicy/<policyName>/{privacy_version, privacy_update, privacy_temp_update_version, privacy_agree_error}` | 隐私政策版本与失败现场（明文 JSON/字符串） | `ue/a.java:63-77,132-144`（`d()/g()/i()`） |
| **文件** | `/data/misc/user/<userId>/securitycenter/across_terminal.xml` | 跨端权限（XML v1） | `permissions/acrossterminal/b.java:28-31` |
| **Settings.System** | `miui_recents_privacy_thumbnail_blur` | 隐私模糊包名列表（逗号分隔） | `ec/c.java:70,81,100,115,213,325,334` |
| Settings.Secure | `privacy_thumbnail_blur_history`、`miui_recents_privacy_thumbnail_blur`(副本)、`miui_home_recents_blur_mode` | 模糊引导/模式 | `ec/c.java:49,217,228,255,276` |
| Settings.Secure | `is_privacy_apps_enable`、`is_privacy_apps_password_enable`、`privacy_apps_shield_message_enable`、`miui_privacy_input_{pkg_name,enabled,supported}`、`PERMISSION_USE_WARNING`、`applock_unlock_mode`、`applock_countDownTimer_deadline` | 隐私空间/输入法/权限提示/应用锁 | `se/a.java:29-46`；`x4/k1.java`；`mc/c.java:229,503`；`c3/f.java:535,624` |
| Settings.Global | `key_app_behavior_record_enable` | 行为记录总开关 | `PermissionManager.java:390`；`PermissionContract.java:12` |
| **远程 Provider** | `content://com.lbe.security.miui.permmgr`（+ 表 `active` / `miui_permission_record_info` / `miui_permission_description_info`） | 权限状态矩阵（userAccept/userReject/userPrompt/userForeground/suggest*/forcedBits/permMask）、权限使用记录、权限描述多语言 | `PermissionContract.java:19-42,150-162` |
| 远程 Provider | `content://com.lbe.security.miui.storagepolicy` | 存储策略 | `StoragePolicyContract.java:12,34` |
| **system_server** | `SecurityManager` | 应用锁密码/类型、受保护包、通知遮挡、隐私应用集合、`exemptTemporarily` 票据 | §4.12/§4.13 |
| 进程内 | `privacy_thumbnail_blur_category`（remoteprovider 存的 JSON） | 云端返回的 app→分类映射 | `ec/c.java:209,53-57` |

> **未发现**本 APK 内任何 `SQLiteOpenHelper` / `openOrCreateDatabase`（模块范围内 grep 零命中）—— 权限数据全在外部包或系统侧。**观测**。

---

## 7. 云端接口汇总

### 7.1 隐私政策同意（CTA SDK，`ue` 包）

| 端点 | 方法 | 触发流程 | 请求体（`JSONObject`，字段全部为 String） | 证据 |
|---|---|---|---|---|
| `https://data.sec.miui.com/privacy/agree/v1` | POST | 用户"同意"某隐私政策 / 首次进入需同意页 | `pkg, policyName, idContent, miuiVersion, apkVersion, policyVersion, language, region, timestamp` | `ue/d.java:27-50` |
| `https://data.sec.miui.com/privacy/revoke/v1` | POST | 用户"撤回同意" | 同上 + **`idStatus="1"`** | `ue/g.java:22-63`（`idStatus` 见 `:31`） |
| `https://data.sec.miui.com/privacy/get/v1` | POST | 需要"拉取已同意的策略详情/最新内容" | `pkg, policyName, timestamp` | `ue/h.java:44-90` |
| `https://data.sec.miui.com/privacy/latestVersion` | POST | 查询策略最新版本号（做版本比对） | `pkg, policyName, timestamp` | `ue/f.java:33-58` |

公共细节（观测）：
- **HTTP 头**：`sign`（`MD5( Base64( key + sortedParams_k=v... + key ) )` 大写，key 硬编码 `2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9`）、`timestamp`、`source: sdk`（`ue/b.java:246-267`、`:136-153`）
- **Content-Type**：`application/json; charset=UTF-8`（`ue/b.java:74-82`）
- **返回值约定**：`code==200 && msg=="success"` 为成功；`data.latestPolicyVersion`（agree）/ `data.version`（get）/ `data`（latestVersion）落文件
- **错误码**：`-2` 网络失败、`-3` 解析/业务失败、`-4` **国际版短路**、`-5` 24h 节流命中、`-6` 同意后仍需更新、`-7` 查询已是最新（`ue/e.java:20-90`）
- **调用者（观测）**：安全中心主流程 `MainFragment.java:397,672`、`SettingsFragment.java:201`、`SettingsPrivacyActivity.java:196`、`BaseAcquireCtaActivity.java:28`、`eg/o.java:100`；以及地震预警 `earthquakewarning/*`、充电保护 `electricrisk/*`、迅游 `l7/b.java`、灾害预警 `WarningCenterDisasterActivity.java:352`
- **国际版替代端点**（不在 `ue` 包，属另一套 flow）：`api-flow-intl.10046.xiaomimobile.com/privacy/{agree,withdraw_agree,query_agree_info}`（`findings/dex_urls.txt`）
- **隐私政策网页**：`https://privacy.mi.com/security/`、`/security-pad/`、`/security-pad-global/`、`/all/`、`/earthquake_monitor/`、`/emergency_location/`、`/AIalert/%s`

### 7.2 隐私风险检测 / 缩略图模糊推荐（`ac` 包）

| 端点 | 方法 | 触发 | 请求 | 响应用途 | 证据 |
|---|---|---|---|---|---|
| `https://flash.sec.miui.com/user/ra` | POST | 隐私风险检测"风险应用"项（`ub.d`）与模糊自动开启（`ec.c.c()` 调用链） | `{"apps":{pkg:versionCode}, "type":.., "timestamp":.., "os":"MIUI_<incremental>", "device":<ro.product.device>, "biz_id":"rivacy_risk_check"}` | 返回 `{pkg: ...}` 集合 → 与本地桌面应用求交集 → 风险应用列表 | `ac/c.java:37,43-57,77-90` |
| `https://flash.sec.miui.com/user/cat` | POST | 缩略图模糊"按月推荐"（`ec.c.w()`，30 天节流） | `{"apps":[pkg,...], 同上元字段}` | 返回 `data`（JSON：pkg→分类）→ 存 `privacy_thumbnail_blur_category` → 自动打开模糊 | `ac/c.java:36,60-71,93-107`；`ec/c.java:279-315` |

传输层（观测）：`AES/ECB/PKCS5Padding`（key = `SHA1(ConfigManager.getSecurityHttpKey())` 生成的 16 字节 `SecretKeySpec`）→ Base64 → **GZIP** → POST，`Content-Type: text/xml`（`ac/a.java:63-76`、`ac/c.java:118-160`）。门禁：`x.z()`。
> **隐私含义**：此接口会把**用户桌面可见的应用包名清单**（`/user/ra` 还含版本号）上传到小米服务器。

### 7.3 其他相关

- `https://data.sec.miui.com/data/upload`、`/adv/tail`、`/data/chargeV2`、`/adv/game_ad/report` — 埋点/广告（非权限模块）
- `https://api.miui.security.xiaomi.com/*` — 安全中心主 API（非本模块核心）
- 云控特征开关：`RemoteProvider.call("isFeatureSupport"/"getFeatureStatus"/"setFeatureStatus", ...)` → `com.miui.common.d`

---

## 8. 重点专项：直接回答

### Q1 `permcenter` 各子包一句话

| 子包 | 一句话 |
|---|---|
| `permissions` | 逐应用/逐权限授权 UI + 系统权限声明弹窗 + 权限使用提示；写入经 `g4.b` 双写到 lbe permmgr 与 AOSP PackageManager |
| `autostart` | 自启动开关，写 `PERM_ID_AUTOSTART(16384)`（permmgr method 6，多用户带 `<uid>@` 前缀） |
| `detection` | 隐私风险检测：8 类检查项（敏感/危险权限计数、云端风险应用、缩略图模糊、照片删除保护、输入法泄漏风险）+ 3 张固定推荐卡 |
| `privacyblur` | 只写 `Settings.System.miui_recents_privacy_thumbnail_blur` 包名白名单；渲染在 `com.miui.home` |
| `provision` | 隐私额外告知（权限×隐私政策逐项确认，Kotlin + `miuix.provision`） |
| `capsule` | 灵动胶囊落地页 + 屏幕共享高风险应用的"停止共享/取消可录制"弹窗（`OP_CAN_BE_RECORDED`） |
| `settings` | 隐私设置聚合：其他权限、隐私输入模式、关机密码、隐形模式、胶囊设置、QS Tile 跳板 |
| `untrustwarn` | **空包**（清单声明了 `UnknownSourceWarnActivity`，DEX 无类）；真实未知来源校验在 `install` |
| `acrossterminal` | 跨端权限，落 `/data/misc/user/<uid>/securitycenter/across_terminal.xml`，provider `com.miui.permissions.acrossterminal` |
| `service` | `InvisibleModeService`：对相机/麦克风/定位做 `setUserRestrictionForUser` + 隐藏状态栏 stealth 图标 |

### Q2 `PrivacyRiskDetectionActivity` 检测哪些项、判定依据
见 §4.3 表格。核心判定式：
- 项 11/12 = `count{app | permissionToAction[PERM_ID] == 3(始终允许)}`，PERM_ID ∈ {32 定位, 8/READCONTACT, 16/READCALLLOG, 131072 剪贴板, 35184372088832 外部存储, -3 媒体视觉(T+), -2 媒体听觉(T+)}（`mc/c.java:358-379`、`mc/c.java:110-121`、`PrivacyRiskDetectionActivity.java:237-258`）
- 项 13 = 云端风险包名集合 ∩ 本地桌面应用（`ub/d.java:52-72`）
- 项 14 = `persist.sys.protect_image`
- 项 16 = `df.h.J(df.h.m(ctx))` && `vf.n.a(ctx)`
- 项 22 = 云端 app 分类映射中、不在用户关闭名单、且 `fc.c.b(分类)` 为真的项存在
- 辅助项（`ub/b`）DeviceAdmin/通知监听/UsageStats 数量仅作"危险权限"卡的补充计数

### Q3 `UnknownSourceWarnActivity` 拦截逻辑
无法回答——**类不存在**（§4.8）。替代品：`PackageVerificationReceiver`（安装前签名/来源校验 + `IMessenger` 回执）、`AdbInstallActivity`/`AdbInstallVerifyActivity`（ADB 安装确认）、`UnknownSourceVerifyActivity`（`com.miui.securitycenter.action.UNKNOWN_SOURCE_VERIFY`，UI 页）。

### Q4 `applicationlock` 实现与安全边界
见 §4.12。三句话：**UI 在本 App，密码与票据在 system_server**；解锁成功靠 `SecurityManager.addAccessControlPass(pkg)`；忘记密码走小米账号（`IXiaomiAccountService`）而非本地后门。

### Q5 `privacyapps` 存储隔离与入口伪装
- 隔离 = **Android 多用户 profile + `SecurityManager` 的隐私应用集合 + 桌面/任务/通知的隐藏处理**；未见文件加密或独立挂载。
- `com.miui.securitycenter.app.hide` 这个 authority **不提供 hide 能力**，只提供 `isPrivacyApp` 查询（§4.13 A）。

### Q6 `blur` — `PRIVACY_THUMBNAIL_BLUR_SETTING` 具体模糊什么、算法在哪
- 模糊对象：**桌面 Recents 里的任务缩略图**（该包名在白名单内则模糊/遮挡）；V2 支持 "dim" 与 "blur" 两种模式。
- 算法：**不在本 APK**。本 App 只写 `Settings.System."miui_recents_privacy_thumbnail_blur"`；`com.miui.blur` 是无关的 backdrop 虚化 SDK（RenderScript `ScriptIntrinsicBlur` + SurfaceFlinger 合成采样）。

### Q7 `idprovider` — OAID 读取与授权
见 §4.15。要点：`OP_GET_OAID(10037)` / `OP_GET_OAID_USER(10038)`；`MODE_ASK=5` 时拉起 `REQUEST_PERMISSIONS_FOR_OAID`；结果写回两个 op 的 mode 并 `setResult`。

### Q8 `appmanager` — 批量卸载/冻结/权限入口
见 §4.16。批量卸载 = `IPackageManager.deletePackageAsUser`；"冻结/停用" = `PackageManager.setApplicationEnabledSetting`；权限入口 = 跳 `APP_PERM_EDITOR`；权限统计不在本包（在 `privacycenter/usage`）。

---

## 9. 未解之谜 / 需要动态或跨镜像验证

1. **`dynamic`：`com.lbe.security.miui` 内部实现**（`IPermissionManager` 真身、AppOps 写入落库表、`miui_permission_record_info` 的实际写入者）——本 APK 只是客户端，需分析 LBE 包或 system_server。
2. **`dynamic`：`miui.security.SecurityManager` 的方法表**（`checkAccessControlPassword` 的哈希算法、`addAccessControlPass` 票据有效期、`exemptTemporarily` 语义）——框架代码不在本 APK。
3. **`unknown`：`untrustwarn.UnknownSourceWarnActivity` / `privacymanager.XSpaceWarningDialogActivity` 是否仍被系统某路径拉起**。清单存在但无类；静态无法否定"系统侧仍会显式启动 → ActivityNotFound"。
4. **`unknown`：`android:sharedUserId` 与签名**——未核验（未跑 `inspect_apk_signature` / 未读 manifest 根节点属性）。
5. **`unknown`：隐私空间的真实存储隔离**。本 APK 侧只有"应用集合 + 隐藏处理"；若 MIUI 对隐私应用做数据隔离，实现应在 system_server / `com.miui.home` / storage 层。
6. **`unknown`：`IdProvider`（31 KB）的完整 method 表与 OAID 生成算法**（是否 `UUID.randomUUID()`、是否可重置、AAID/VAID 处理）。
7. **`unknown`：`OAID` 授权写 mode 的语义**（`allow → 1`、`deny → 0`，与 AOSP `MODE_ALLOWED=0` 相反）。可能是 lbe 自定义 op 的非标准 mode 表，需动态验证。
8. **`unknown`：`ShutDownPasswordFragment` 的存储**（疑似复用 `SecurityManager.setAccessControlPassword`，但未见显式调用点）。
9. **`partial`：`permcenter/q.java`(29 KB)、`permcenter/l.java`(33 KB)、`permcenter/n.java`(57 KB)` 等混淆工具类未逐个展开**（`n` 已确认含 `y(ctx,boolean,List<Long>,xc.c)` 权限批量查询）。
10. **`partial`：`appmanager/fragment/ApplicationsDetailsFragment`(135 KB) / `ManageFragment`(76 KB)、`permcenter/permissions/PermissionAppsModifyFragment`(88 KB)、`settings/OtherPermissionsFragment`(46 KB) 只做了入口与关键调用的抽样，未逐方法覆盖**。

---

## 10. 证据索引（按主题）

**权限数据源 / 契约**
- `com/miui/permission/PermissionContract.java:10,15,16,17`（authority / URI）
- `com/miui/permission/PermissionContract.java:44-61`（method 码）
- `com/miui/permission/PermissionContract.java:19-42`（`active` 表列）
- `com/miui/permission/PermissionContract.java:150-162`（`miui_permission_record_info` 表列）
- `com/miui/permission/PermissionManager.java:24-31,43-68,69-136`（ACTION/GROUP/PERM_ID）
- `com/miui/permission/PermissionManager.java:170-191`（virtualMap / 静态初始化）
- `com/miui/permission/PermissionManager.java:293-306,436-455,522-524`（三个 write 入口）
- `com/miui/permission/StoragePolicyContract.java:12,21,29,34`
- `com/miui/permcenter/compact/AppOpsUtilsCompat.java`（全文）
- `android/app/AppOpsManagerCompat.java`（全文，op 表）

**写入 / 双写**
- `g4/b.java:320-367`（组权限写入 + 存储特殊分支）
- `g4/b.java:369-417`（单权限写入 + grant/revoke 反射）
- `com/miui/permcenter/compact/PermissionManagerCompat.java:13-19`（虚拟权限）
- `permcenter/autostart/AutoStartManagementActivity.java:403-428`

**隐私风险检测**
- `permcenter/detection/PrivacyRiskDetectionActivity.java:237-258,712-721,752-773,834-888,388-492`
- `permcenter/detection/model/d.java:60-125`（item 构造与文案 key）
- `ub/b.java:52-160`、`ub/c.java`、`ub/d.java:52-72`、`ub/e.java`、`ub/a.java`
- `mc/c.java:110-121,358-379,388-500`
- `uc/b.java:20-40`

**隐私模糊**
- `ec/c.java`（全文，含 `Settings.System.miui_recents_privacy_thumbnail_blur`）
- `permcenter/privacyblur/PrivacyThumbnailBlurSettings.java:67-76,306-310,334-338,400-422`
- `permcenter/privacyblur/a.java`（adapter）
- `ub/c.java`（`persist.sys.protect_image`）

**应用锁**
- `applicationlock/ConfirmAccessControl.java:1368,1373,1991-1993,2485-2530,999`
- `applicationlock/widget/q.java:118-136`
- `c3/f.java:250,391-396,435,478,547,138-153,214-222,513,535,612,624,665`
- `c3/d.java:88-96,155-165,236-240`
- `applicationlock/ChooseAccessControl.java:502,509`
- `applicationlock/ConfirmAccountActivity.java:174-177`
- `c3/w.java:60-80`
- `applicationlock/f.java:230`

**隐私应用**
- `privacyapps/PrivacyAppsProvider.java:39-56,79`
- `se/a.java:29-66`、`se/c.java:33-97`、`se/e.java:54-176`
- `pe/a.java:7`
- `privacyapps/ui/PrivacyAppsHelper.java:30-46`、`privacyapps/ui/PrivacyAppsActivity.java:135-160,192-334`

**OAID**
- `idprovider/ui/GrantPermissionActivity.java:66-99,118-186`
- `permcenter/compact/AppOpsUtilsCompat.java`（`isSupportOAIDApps`）

**跨端**
- `permcenter/permissions/acrossterminal/b.java:28-31,56-102,104-168`
- `permcenter/permissions/acrossterminal/AcrossTerminalPermissionContentProvider.java`（`call()` 全文）
- `dc/d.java:29-77`

**安装校验 / 未知来源**
- `permcenter/install/PackageVerificationReceiver.java:40-53,116-127,168-208,213-221`
- `permcenter/install/AdbInstallActivity.java:176-188`
- `com/miui/common/persistence/RemoteProvider.java:174-190`

**隐形模式 / 隐私输入 / 屏享保护**
- `permcenter/service/InvisibleModeService.java:37,41-59,63-96,97-120`
- `permcenter/settings/PrivacyInputModeFragment.java:117-182,323,353`
- `x4/k1.java`（privacy input 设置键）
- `permcenter/capsule/ScreenShareProtectionActivity.java:60-78,96-101`

**blur**
- `com/miui/blur/sdk/backdrop/BlurManager.java:84-85,116-140`
- `com/miui/blur/sdk/backdrop/p.java:127-129,174-176,388-392`
- `com/miui/blur/sdk/backdrop/BlurDrawInfo.java`

**云端**
- `ue/b.java:74-82,136-153,246-267`（HTTP + 签名）
- `ue/d.java`、`ue/g.java`、`ue/f.java`、`ue/h.java`、`ue/e.java:20-90`、`ue/i.java`、`ue/a.java`、`ue/c.java`
- `ac/a.java:63-76`、`ac/c.java:36-37,43-107,118-160`
- `findings/dex_urls.txt`（`privacy.mi.com/*`、`api-flow-intl.*`）

**App 管理**
- `appmanager/AppManageUtils.java:124,141,861,880,906-916,930-955,985-1008,1099,1109,1317`
- `appmanager/fragment/AMSettingsFragment.java:161`、`fragment/ApplicationsDetailsFragment.java:720`

**清单**
- `_analysis/apktool_sc/AndroidManifest.xml`（全部 `activity/service/provider/receiver/activity-alias` 的 `exported`/`permission`/`intent-filter`；`activity-alias` 目标见 §2.2 注）
