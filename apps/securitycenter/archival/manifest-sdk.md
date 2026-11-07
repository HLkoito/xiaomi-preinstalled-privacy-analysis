> **Historical analysis.** This archive includes observations that remain unverified or were later corrected. See the [final report](../REPORT.md) and [independent review](../REVIEW.md) for the reviewed findings on upload scope, consent, local FBO delegation, emergency SMS encryption, and provider authorization. Source references retain their original decompilation paths. The publication includes the selected files under evidence/.

# MIUI Security Center — Manifest / Permissions / Component Exposure / Signature / Third Party SDK / Code Structure

> Target: `MIUISecurityCenter-10.4.9-250402.0.1.apk`
> SHA-256 `fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd`, 66,917,411 B, ZIP entry 9231
> Scope of responsibilities: Manifest, permissions, component exposure, signatures, SDK manifest, code structure and obfuscation, multi-process. **does not contain the** business algorithm (others are responsible).
> Marking discipline: **[Observed]**=shipped artifact direct fact; **[Inferred]**=reasonable inference based on observation; **[Unknown]**=insufficient evidence.

---

## 0. Baseline correction (must read first)

These lines in `_BASELINE.md` point to **, the product of** and another APK, and cannot be used as evidence for this purpose:

| baseline statement | actual situation |
|---|---|
| `_analysis\apktool_sc\` = apktool decoding (including AndroidManifest.xml) | **is wrong**. `apktool_sc\` only has `res\` + 3 dex, no manifest |
| Implicitly available `_analysis\apktool\` / `_analysis\src\` / `_analysis\report\` | **is all the product** of `com.miui.guardprovider` 2.0.3-20231124.0 (apktool.yml: `apkFileName: GuardProvider-2.0.3-20231124.0.apk`, package `com.miui.guardprovider`, sharedUserId None, class 3242) |
| `_analysis\avlsdk_extract\` | Also from GuardProvider (`assets/avlsdk`), there is no avlsdk**in the assets of** and APK |
| `_analysis\dex_strings_all.txt` | dex from GuardProvider (there are `Ltmsdk/...`, `com/miui/guardprovider/engine/tencent/TencentEngine`), **is not this APK** |

**Reproducible evidence sources used in this report** (all have been generated and can be reviewed):

| File | Notes |
|---|---|
| `_analysis\tools\axml2xml.js` | Self-written AXML decoder (Node, no third-party dependencies), decodes binary `apk_full\AndroidManifest.xml` into text |
| `_analysis\apk_full\AndroidManifest.decoded.xml` | The complete manifest text after **decoding, line 5137** - all `L###` line numbers in this report refer to this file |
| `_analysis\tools\manifest.events.json` | AXML event for each element + decoded line number |
| `_analysis\tools\manifest_out\manifest.structured.json` | Structured list (components/permissions/meta-data full) |
| `_analysis\tools\classes.classlist.json` | **25009 class** directly enumerated from `class_defs` of 3 dex (non-jadx statistics) |
| `_analysis\tools\{dex_classes,pkg_hist,top_roots,perm_classify,gen_tables2}.js` | Statistics script |

Reproduction command:
```powershell
node _analysis\tools\axml2xml.js _analysis\apk_full\AndroidManifest.xml _analysis\apk_full\AndroidManifest.decoded.xml _analysis\tools\manifest.events.json
node _analysis\tools\manifest_analyze.js _analysis\tools\manifest.events.json _analysis\tools\manifest_out
node _analysis\tools\dex_classes.js _analysis\apk_full\classes.dex _analysis\apk_full\classes2.dex _analysis\apk_full\classes3.dex
```

---

## 1. Application identity and application-level attributes

### 1.1 `<manifest>`（decoded.xml L2–L10）

| Properties | value | Judgment |
|---|---|---|
| `package` | `com.miui.securitycenter` | [Observed] |
| `android:versionCode` | `40001049` | [Observed] |
| `android:versionName` | `10.4.9-250402.0.1` | [Observed] |
| **`android:sharedUserId`** | **`android.uid.system`** | [Observed]**The most critical line in the whole article** |
| `android:compileSdkVersion` / `Codename` | `34` / `14` | [Observed] |
| `platformBuildVersionCode` / `Name` | `34` / `14` | [Observed] |
| `android:targetSandboxVersion` | **does not exist** | [Observed]→ Take the default value 1 (not 2, that is, strong sandbox isolation of Android 10 is not enabled) |

`<uses-sdk>`（L11）：`minSdkVersion=23`、`targetSdkVersion=34`。[Observed]

> The meaning of **`sharedUserId="android.uid.system"`** [inference, based on AOSP semantics]: This APK shares **UID with `system_server`, `com.android.systemui`, `com.android.settings`, etc. 1000**. Any code path executed as system UID directly owns the SELinux domain of the system and all `signature|privileged` permissions; `android:permission` can only block the "initiator", but cannot block "the capabilities that this process has after it is exploited." This is the APK with all exported components risking the **amplifier**.

### 1.2 `<application>`（decoded.xml L469–L481）

| Properties | value | Judgment |
|---|---|---|
| `android:name` | `com.miui.securitycenter.Application` | [Observed] |
| `android:label` / `icon` | `@0x7f12020f` / `@0x7f08037a` | [Observed] |
| `android:backupAgent` | `com.miui.antispam.service.backup.SecurityCenterBackupAgent` | [Observed] |
| **`android:allowBackup`** | **does not exist** | [Observed]→ The default is **true**; with the custom backupAgent above, it is equal to **application data (interception rules/configuration library) can be exported** by the backup channel, and the process identity is system |
| `android:debuggable` | does not exist | [Observed]→ false（release） |
| `android:requestLegacyExternalStorage` | `true` | [Observed]This flag is invalid under targetSdk 34 and is only a historical legacy |
| `android:extractNativeLibs` | `false` | [Observed]so map directly from APK (volume for startup speed) |
| `android:appComponentFactory` | `androidx.core.app.CoreComponentFactory` | [Observed]AndroidX component factory |
| `android:networkSecurityConfig` | `@0x7f15003c` | [Observed] Plain text traffic policy at `res/xml/network_security_config.xml` (unsolved in arsc, see §9) |
| `android:resizeableActivity` | `false` | [Observed] |
| `android:hardwareAccelerated` / `uiOptions` / `supportsRtl` | `true` / `0x1` / `true` | [Observed] |
| `android:largeHeap` / `usesCleartextTraffic` / `fullBackupContent` / `dataExtractionRules` / `localeConfig` | None exist | [Observed] |

---

## 2. Permissions

### 2.1 Custom permissions declared by this APK: 50 (decoded.xml L51–L458, interleaved with uses-permission)

Protection level distribution [Observed]: **`signatureOrSystem (0x3)` = 43**, `signature (0x2)` = 3, `normal (0x0)` = 2, **does not declare protectionLevel (=normal) = 2**.

| # | Custom permissions | protectionLevel | The components protected by this APK | Line number |
|---|---|---|---|---|
| 1 | `com.miui.securitycenter.permission.READ_APP_BEHAVIORS` | signatureOrSystem (0x3) | provider:PackageBehaviorContentProvider | 51 |
| 2 | `miui.permission.NOTIFICATION_VERIFY` | signatureOrSystem (0x3) | activity:NotificationVerifyActivity | 55 |
| 3 | `miui.permission.ACCESSIBILITY_SERVICE` | signatureOrSystem (0x3) | activity:AccessibilityServiceActivity | 57 |
| 4 | `miui.permission.APP_PREDICT_VERIFY` | signatureOrSystem (0x3) | service:AiService | 59 |
| 5 | `com.miui.autotask.permission.CONTENTPROVIDER` | signatureOrSystem (0x3) | provider:AutoTaskProvider | 61 |
| 6 | `com.xiaomi.permission.miuibatteryintelligence` | signatureOrSystem (0x3) | (none) | 63 |
| 7 | `com.miui.security.manager.SHORTCUT` | signatureOrSystem (0x3) | (none) | 77 |
| 8 | `miui.permission.TVM` | **undeclared → normal** | service:TvmManagerService | 79 |
| 9 | `miui.securitycenter.permission.ANALYTICS` | signatureOrSystem (0x3) | (none) | 81 |
| 10 | `miui.permission.MEMORY_CHECK` | signatureOrSystem (0x3) | service:MemoryCheckService | 83 |
| 11 | `com.miui.securitycenter.permission.SWITCH_DEV_MODE` | signatureOrSystem (0x3) | (none) | 85 |
| 12 | `com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER` | signatureOrSystem (0x3) | activity:ThirdDesktopAlertActivity<br>service:GameBoosterService<br>service:VideoToolBoxService<br>service:DockWindowManagerService<br>service:NotificationLinkService<br>provider:SecurityCenterProvider<br>provider:RemoteProvider<br>provider:ThirdMonitorProvider | 87 |
| 13 | `com.miui.monthreport.permission.DATA_MONTH_REPORT` | signatureOrSystem (0x3) | provider:MonthReportProvider | 89 |
| 14 | `com.miui.securitycenter.permission.NOTIFICATION_SERVICE` | signatureOrSystem (0x3) | service:NotificationService | 91 |
| 15 | `com.miui.powercenter.permission.POWER_DATA` | signatureOrSystem (0x3) | provider:PowerCenterProvider<br>provider:AutoTaskProvider | 93 |
| 16 | `com.miui.securitrycenter.permission.EXTERNAL_ANLYTICS` | signature (0x2) | (none) | 95 |
| 17 | `miui.permission.EXTRA_NETWORK` | signatureOrSystem (0x3) | provider:NetworkAssistantProvider<br>provider:LuckyMoneyProvider | 97 |
| 18 | `com.miui.powercenter.permission.POWER_COMMAND` | signatureOrSystem (0x3) | provider:PowerSaverProvider | 99 |
| 19 | `com.miui.securitycenter.permission.RECEIVE_TRACK_EVERYWEEK` | signatureOrSystem (0x3) | (none) | 101 |
| 20 | `com.miui.securitycenter.permission.CONTROL_VPN` | signatureOrSystem (0x3) | (none) | 103 |
| 21 | `com.miui.antispam.permission.CONTENTPROVIDER` | signatureOrSystem (0x3) | provider:AntiSpamProvider | 105 |
| 22 | `com.miui.antispam.permission.DATA_MONTH_REPORT` | signatureOrSystem (0x3) | service:AntiSpamService<br>service:CloudThirdDesktopService | 107 |
| 23 | `com.miui.securitycenter.permission.MEDIA_SERVER_KILL` | normal (0x0) | (none) | 109 |
| 24 | `com.miui.securitycenter.permission.GAMEBOOSTER_PROVIDER` | signatureOrSystem (0x3) | provider:GameBoosterProvider | 111 |
| 25 | `com.miui.securitycenter.permission.PERM_USE_INFO` | **undeclared → normal** | activity:AppPermissionsUseActivity | 113 |
| 26 | `com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE` | signatureOrSystem (0x3) | activity:ShutDownPasswordActivity<br>activity:SimLockStartActivity<br>activity:DevicePermissionSettingsActivity<br>activity:DevicePermissionEditorActivity<br>activity:GrantDevicePermissionActivity<br>activity:OtherPermissionsActivity<br>activity:CombinePermissionActivity<br>provider:AcrossTerminalPermissionContentProvider<br>provider:PrivacyAppsProvider | 115 |
| 27 | `com.miui.securitycenter.permission.REMOTE_ANALYTICS` | signatureOrSystem (0x3) | receiver:RemoteAnalyticsReceiver | 117 |
| 28 | `com.miui.securitycenter.permission.TOP_GAME_LIST` | signatureOrSystem (0x3) | (none) | 119 |
| 29 | `com.miui.securitycenter.permission.GB_SHOW_WINDOW` | signatureOrSystem (0x3) | (none) | 121 |
| 30 | `com.miui.securitycenter.permission.RECEIVE_TRACK_DAY` | signatureOrSystem (0x3) | (none) | 123 |
| 31 | `com.miui.securitycenter.permission.GB_CREATE_VOICE` | signatureOrSystem (0x3) | (none) | 125 |
| 32 | `com.miui.zman.permission.SECURITY_SHARE` | signatureOrSystem (0x3) | activity:PrivacyProtectSettingsActivity<br>activity:ShareSettingActivity<br>activity:PrivacyProtectTipSettingsActivity<br>provider:ZmanProvider | 127 |
| 33 | `com.miui.cleanmaster.permission.UPDATE_SHORTCUT` | signatureOrSystem (0x3) | (none) | 129 |
| 34 | `com.miui.cleaner.permission.UPDATE_CLEANER_SHORTCUT` | signatureOrSystem (0x3) | (none) | 131 |
| 35 | `com.miui.cleaner.permission.REMOTE_NOTIFICATION` | signatureOrSystem (0x3) | (none) | 133 |
| 36 | `com.miui.securitycenter.permission.CLEANER_MANAGE` | signatureOrSystem (0x3) | service:CleanerHandleService | 135 |
| 37 | `com.miui.securitycenter.permission.WIDGET_PROVIDER` | signatureOrSystem (0x3) | provider:WidgetProvider | 137 |
| 38 | `com.miui.dock.permission.DOCK_EVENT` | signatureOrSystem (0x3) | (none) | 139 |
| 39 | `com.miui.dock.permission.STATUS_CHANGE` | signatureOrSystem (0x3) | (none) | 141 |
| 40 | `com.miui.gamebooster.permission.PANNEL_OPEN` | signatureOrSystem (0x3) | (none) | 407 |
| 41 | `com.miui.securitycenter.POWER_CENTER_COMMON_PERMISSION` | signatureOrSystem (0x3) | (none) | 409 |
| 42 | `com.xiaomi.joyose.permission.predownload` | signatureOrSystem (0x3) | (none) | 413 |
| 43 | `com.miui.globalsatisfaction.permission.PROVIDER` | signatureOrSystem (0x3) | provider:GsProvider | 415 |
| 44 | `com.miui.gamebooster.access.ACTIVITY` | signatureOrSystem (0x3) | activity:SettingsActivity<br>activity:SelectGameLandActivity | 417 |
| 45 | `miui.permission.REMOTE_AUTH` | signatureOrSystem (0x3) | (none) | 437 |
| 46 | `miui.permission.TRUST_SERVICE_INTERNAL` | signatureOrSystem (0x3) | (none) | 439 |
| 47 | `com.miui.security.permission.DETECT_TEXT` | signatureOrSystem (0x3) | provider:SensitiveWordDetectProvider | 443 |
| 48 | `com.xiaomi.security.permission.ACCESS_XSOF` | normal (0x0) | (none) | 449 |
| 49 | `com.miui.securitycenter.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | signature (0x2) | (none) | 451 |
| 50 | `com.miui.securitycenter.permission.MIPUSH_RECEIVE` | signature (0x2) | (none) | 457 |

The two "undeclared protectionLevel" permissions of **are real gaps** [observation + inference]:
- `miui.permission.TVM` (L79, no protectionLevel → **normal**) protection export service `com.miui.tvm.service.TvmManagerService` (action `miui.intent.action.START_TVM`, L1264). **Note**: This service is also `android:enabled="false"`, so the current version cannot actually be started; this protection level gap is the potential **of** (it will take effect immediately if it is enabled by `PackageManager.setComponentEnabledSetting` on the system side).
- `com.miui.securitycenter.permission.PERM_USE_INFO` (L113, → **normal**) protected export Activity `com.miui.permcenter.permissions.AppPermissionsUseActivity` (action `miui.intent.action.APP_PERMISSION_USE`).
- In addition, `com.miui.securitycenter.permission.MEDIA_SERVER_KILL` (L109) and `com.xiaomi.security.permission.ACCESS_XSOF` (L449) explicitly write `normal`.
- [Inferred]`normal` level means **and any third-party App can declare it in the manifest and get** immediately. Therefore, for any component that uses these four permissions as the only protection, its "permission protection" is actually not established - the hits are `TvmManagerService` (L1264, and `enabled=false`) and `AppPermissionsUseActivity` (L3795); whether it can still be exploited needs to be combined with whether the caller verification is done inside the component (not verified in this round).

### 2.2 `uses-permission`: 147 items, all without `maxSdkVersion` limit

`uses-permission-sdk-23` = 0。[Observed]

Classification statistics [Observed]: **AOSP Normal 30 / Dangerous (runtime) 17 / AOSP Signature · Privilege 28 / MIUI · Vendor proprietary 72**.

Statistics: A AOSP Normal = 30, D Dangerous (runtime) = 17, C AOSP Signed/Privileged = 28, E MIUI/Vendor Private = 72, Total 147

| # | Permission | Classification | maxSdk | Line number |
|---|---|---|---|---|
| 1 | `android.permission.ACCESS_DOWNLOAD_MANAGER` | A AOSP Normal | - | 209 |
| 2 | `android.permission.ACCESS_NETWORK_STATE` | A AOSP Normal | - | 213 |
| 3 | `android.permission.ACCESS_WIFI_STATE` | A AOSP Normal | - | 215 |
| 4 | `android.permission.ACCESS_WIMAX_STATE` | A AOSP Normal | - | 291 |
| 5 | `android.permission.AUTHENTICATE_ACCOUNTS` | A AOSP Normal | - | 351 |
| 6 | `android.permission.BLUETOOTH` | A AOSP Normal | - | 375 |
| 7 | `android.permission.BLUETOOTH_ADMIN` | A AOSP Normal | - | 377 |
| 8 | `android.permission.CHANGE_WIFI_STATE` | A AOSP Normal | - | 217 |
| 9 | `android.permission.CHANGE_WIMAX_STATE` | A AOSP Normal | - | 293 |
| 10 | `android.permission.CONFIGURE_WIFI_DISPLAY` | A AOSP Normal | - | 319 |
| 11 | `android.permission.DISABLE_KEYGUARD` | A AOSP Normal | - | 333 |
| 12 | `android.permission.DOWNLOAD_WITHOUT_NOTIFICATION` | A AOSP Normal | - | 355 |
| 13 | `android.permission.EXPAND_STATUS_BAR` | A AOSP Normal | - | 287 |
| 14 | `android.permission.FOREGROUND_SERVICE` | A AOSP Normal | - | 381 |
| 15 | `android.permission.FOREGROUND_SERVICE_DATA_SYNC` | A AOSP Normal | - | 441 |
| 16 | `android.permission.GET_PACKAGE_SIZE` | A AOSP Normal | - | 249 |
| 17 | `android.permission.INTERNET` | A AOSP Normal | - | 219 |
| 18 | `android.permission.KILL_BACKGROUND_PROCESSES` | A AOSP Normal | - | 243 |
| 19 | `android.permission.MANAGE_ACCOUNTS` | A AOSP Normal | - | 309 |
| 20 | `android.permission.READ_SYNC_SETTINGS` | A AOSP Normal | - | 255 |
| 21 | `android.permission.READ_USER_DICTIONARY` | A AOSP Normal | - | 225 |
| 22 | `android.permission.RECEIVE_BOOT_COMPLETED` | A AOSP Normal | - | 251 |
| 23 | `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | A AOSP Normal | - | 435 |
| 24 | `android.permission.SCHEDULE_EXACT_ALARM` | A AOSP Normal | - | 19 |
| 25 | `android.permission.USE_CREDENTIALS` | A AOSP Normal | - | 311 |
| 26 | `android.permission.VIBRATE` | A AOSP Normal | - | 447 |
| 27 | `android.permission.WAKE_LOCK` | A AOSP Normal | - | 307 |
| 28 | `android.permission.WRITE_SYNC_SETTINGS` | A AOSP Normal | - | 349 |
| 29 | `android.permission.WRITE_USER_DICTIONARY` | A AOSP Normal | - | 227 |
| 30 | `com.android.launcher.permission.INSTALL_SHORTCUT` | A AOSP Normal | - | 259 |
| 31 | `android.permission.ACCESS_COARSE_LOCATION` | D Danger (runtime) | - | 25 |
| 32 | `android.permission.ACCESS_FINE_LOCATION` | D Danger (runtime) | - | 33 |
| 33 | `android.permission.BLUETOOTH_CONNECT` | D Danger (runtime) | - | 53 |
| 34 | `android.permission.BLUETOOTH_SCAN` | D Danger (runtime) | - | 47 |
| 35 | `android.permission.BODY_SENSORS` | D Danger (runtime) | - | 37 |
| 36 | `android.permission.CALL_PHONE` | D Danger (runtime) | - | 205 |
| 37 | `android.permission.GET_ACCOUNTS` | D Danger (runtime) | - | 39 |
| 38 | `android.permission.POST_NOTIFICATIONS` | D Danger (runtime) | - | 29 |
| 39 | `android.permission.READ_CALL_LOG` | D Danger (runtime) | - | 357 |
| 40 | `android.permission.READ_CONTACTS` | D Danger (runtime) | - | 211 |
| 41 | `android.permission.READ_EXTERNAL_STORAGE` | D Danger (runtime) | - | 23 |
| 42 | `android.permission.READ_PHONE_STATE` | D Danger (runtime) | - | 35 |
| 43 | `android.permission.READ_SMS` | D Danger (runtime) | - | 43 |
| 44 | `android.permission.RECORD_AUDIO` | D Danger (runtime) | - | 41 |
| 45 | `android.permission.SEND_SMS` | D Danger (runtime) | - | 301 |
| 46 | `android.permission.WRITE_CALL_LOG` | D Danger (runtime) | - | 359 |
| 47 | `android.permission.WRITE_EXTERNAL_STORAGE` | D Danger (runtime) | - | 21 |
| 48 | `android.permission.ACCESS_CACHE_FILESYSTEM` | C AOSP signature/privilege | - | 271 |
| 49 | `android.permission.BACKUP` | C AOSP signature/privilege | - | 235 |
| 50 | `android.permission.BATTERY_STATS` | C AOSP signature/privilege | - | 233 |
| 51 | `android.permission.CLEAR_APP_CACHE` | C AOSP signature/privilege | - | 245 |
| 52 | `android.permission.CLEAR_APP_USER_DATA` | C AOSP signature/privilege | - | 221 |
| 53 | `android.permission.CONTROL_VPN` | C AOSP signature/privilege | - | 313 |
| 54 | `android.permission.DELETE_CACHE_FILES` | C AOSP signature/privilege | - | 247 |
| 55 | `android.permission.DEVICE_POWER` | C AOSP signature/privilege | - | 201 |
| 56 | `android.permission.FORCE_STOP_PACKAGES` | C AOSP signature/privilege | - | 229 |
| 57 | `android.permission.GET_APP_OPS_STATS` | C AOSP signature/privilege | - | 15 |
| 58 | `android.permission.GET_TASKS` | C AOSP signature/privilege | - | 281 |
| 59 | `android.permission.MANAGE_NETWORK_POLICY` | C AOSP signature/privilege | - | 295 |
| 60 | `android.permission.MANAGE_USB` | C AOSP signature/privilege | - | 237 |
| 61 | `android.permission.MASTER_CLEAR` | C AOSP signature/privilege | - | 207 |
| 62 | `android.permission.MODIFY_NETWORK_ACCOUNTING` | C AOSP signature/privilege | - | 299 |
| 63 | `android.permission.MODIFY_PHONE_STATE` | C AOSP signature/privilege | - | 223 |
| 64 | `android.permission.MOUNT_UNMOUNT_FILESYSTEMS` | C AOSP signature/privilege | - | 203 |
| 65 | `android.permission.PACKAGE_USAGE_STATS` | C AOSP signature/privilege | - | 231 |
| 66 | `android.permission.PACKAGE_VERIFICATION_AGENT` | C AOSP signature/privilege | - | 339 |
| 67 | `android.permission.READ_NETWORK_USAGE_HISTORY` | C AOSP signature/privilege | - | 297 |
| 68 | `android.permission.READ_SECURE_SETTINGS` | C AOSP signature/privilege | - | 303 |
| 69 | `android.permission.READ_SETTINGS` | C AOSP signature/privilege | - | 305 |
| 70 | `android.permission.REAL_GET_TASKS` | C AOSP signature/privilege | - | 335 |
| 71 | `android.permission.REBOOT` | C AOSP signature/privilege | - | 239 |
| 72 | `android.permission.SYSTEM_ALERT_WINDOW` | C AOSP signature/privilege | - | 331 |
| 73 | `android.permission.UPDATE_APP_OPS_STATS` | C AOSP signature/privilege | - | 253 |
| 74 | `android.permission.WRITE_APN_SETTINGS` | C AOSP signature/privilege | - | 321 |
| 75 | `android.permission.WRITE_SETTINGS` | C AOSP signature/privilege | - | 199 |
| 76 | `com.android.permission.GET_INSTALLED_APPS` | E MIUI/manufacturer’s proprietary | - | 45 |
| 77 | `com.android.permission.WN_CHECKER` | E MIUI/manufacturer’s proprietary | - | 49 |
| 78 | `com.android.settings.permission.POWER_MODE_CHANGE` | E MIUI/manufacturer’s proprietary | - | 337 |
| 79 | `com.android.settings.permission.PULL_CLOUD_DATA` | E MIUI/manufacturer’s proprietary | - | 267 |
| 80 | `com.android.settings.permission.SWITCH_DEV_MODE` | E MIUI/manufacturer’s proprietary | - | 265 |
| 81 | `com.android.systemui.permission.NOTIFICATION` | E MIUI/manufacturer’s proprietary | - | 31 |
| 82 | `com.android.updater.permission.ACCESS_UPDATESERVICE` | E MIUI/manufacturer’s proprietary | - | 283 |
| 83 | `com.android.updater.permission.RECEIVE_BROADCAST` | E MIUI/manufacturer’s proprietary | - | 285 |
| 84 | `com.cleanmaster.permission.sdk.clean` | E MIUI/manufacturer’s proprietary | - | 241 |
| 85 | `com.mi.health.provider.device.read` | E MIUI/manufacturer’s proprietary | - | 399 |
| 86 | `com.mipay.wallet.permission.READ_IDENTITY_STATUS` | E MIUI/manufacturer’s proprietary | - | 329 |
| 87 | `com.miui.accessibility.permission.ACCESS_SI` | E MIUI/manufacturer’s proprietary | - | 431 |
| 88 | `com.miui.calendar.permission.READ_CALENDAR` | E MIUI/manufacturer’s proprietary | - | 387 |
| 89 | `com.miui.cleanmaster.permission.Clean_Master` | E MIUI/manufacturer’s proprietary | - | 327 |
| 90 | `com.miui.dock.permission.DOCK_EVENT` | E MIUI/manufacturer’s proprietary | - | 401 |
| 91 | `com.miui.gamebooster.access.ACTIVITY` | E MIUI/manufacturer’s proprietary | - | 421 |
| 92 | `com.miui.gamebooster.permission.PARENT_VERIFY` | E MIUI/manufacturer’s proprietary | - | 419 |
| 93 | `com.miui.gameturbo.permission.ACTIVE_MANAGER` | E MIUI/manufacturer’s proprietary | - | 385 |
| 94 | `com.miui.guardprovider.permission.Guard_Provider` | E MIUI/manufacturer’s proprietary | - | 325 |
| 95 | `com.miui.home.launcher.permission.UNINSTALL_SHORTCUT` | E MIUI/manufacturer’s proprietary | - | 261 |
| 96 | `com.miui.monthreport.permission.DATA_MONTH_REPORT` | E MIUI/manufacturer’s proprietary | - | 279 |
| 97 | `com.miui.permission.FIREWALL` | E MIUI/manufacturer’s proprietary | - | 341 |
| 98 | `com.miui.permission.MANAGE_BOOT_TIME` | E MIUI/manufacturer’s proprietary | - | 273 |
| 99 | `com.miui.permission.MANAGE_SMS_INTERCEPT` | E MIUI/manufacturer’s proprietary | - | 315 |
| 100 | `com.miui.permission.REGION_SAMPLING` | E MIUI/manufacturer’s proprietary | - | 455 |
| 101 | `com.miui.powerkeeper.permission.POWER_NOTIFY` | E MIUI/manufacturer’s proprietary | - | 73 |
| 102 | `com.miui.powerkeeper.permission.REQUEST_WAKE` | E MIUI/manufacturer’s proprietary | - | 395 |
| 103 | `com.miui.security.manager.SHORTCUT` | E MIUI/manufacturer’s proprietary | - | 69 |
| 104 | `com.miui.security.permission.installOtherApp` | E MIUI/manufacturer’s proprietary | - | 75 |
| 105 | `com.miui.securityadd.permission.GAMEBOOSTER_PROVIDER` | E MIUI/manufacturer’s proprietary | - | 363 |
| 106 | `com.miui.securityadd.permission.KID_MODE_ACCESS` | E MIUI/manufacturer’s proprietary | - | 379 |
| 107 | `com.miui.securityadd.permission.SUPERPOWER_RECEIVE` | E MIUI/manufacturer’s proprietary | - | 373 |
| 108 | `com.miui.securitycenter.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | E MIUI/manufacturer’s proprietary | - | 453 |
| 109 | `com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER` | E MIUI/manufacturer’s proprietary | - | 275 |
| 110 | `com.miui.securitycenter.permission.GAMEBOOSTER_PROVIDER` | E MIUI/manufacturer’s proprietary | - | 361 |
| 111 | `com.miui.securitycenter.permission.GLOBAL_PACKAGEINSTALLER` | E MIUI/manufacturer’s proprietary | - | 367 |
| 112 | `com.miui.securitycenter.permission.MIPUSH_RECEIVE` | E MIUI/manufacturer’s proprietary | - | 459 |
| 113 | `com.miui.securitycenter.permission.PERM_USE_INFO` | E MIUI/manufacturer’s proprietary | - | 369 |
| 114 | `com.miui.systemAdSolution.adSwitch.PROVIDER` | E MIUI/manufacturer’s proprietary | - | 343 |
| 115 | `com.miui.voiceassist.QUERY_ENABLE` | E MIUI/manufacturer’s proprietary | - | 371 |
| 116 | `com.miui.yellowpage.permission.ACCESS_YELLOWPAGE` | E MIUI/manufacturer’s proprietary | - | 317 |
| 117 | `com.qualcomm.permission.IZAT` | E MIUI/manufacturer’s proprietary | - | 13 |
| 118 | `com.xiaomi.aicr.permission.OPEN_CAPABILITY_INTERFACE` | E MIUI/manufacturer’s proprietary | - | 433 |
| 119 | `com.xiaomi.calendar.permission.READ_CALENDAR` | E MIUI/manufacturer’s proprietary | - | 389 |
| 120 | `com.xiaomi.dist.permission.DIST_HARDWARE_SERVICE` | E MIUI/manufacturer’s proprietary | - | 425 |
| 121 | `com.xiaomi.gamecenter.for3thd.migame.xcashier` | E MIUI/manufacturer’s proprietary | - | 345 |
| 122 | `com.xiaomi.macro.permission` | E MIUI/manufacturer’s proprietary | - | 391 |
| 123 | `com.xiaomi.permission.ACCESS_MMS_NETWORK_STATE` | E MIUI/manufacturer’s proprietary | - | 27 |
| 124 | `com.xiaomi.permission.ACCESS_SECURITY_DEVICE_CREDENTIAL` | E MIUI/manufacturer’s proprietary | - | 365 |
| 125 | `com.xiaomi.permission.BIND_CONTINUITY_SERVICE_INTERNAL` | E MIUI/manufacturer’s proprietary | - | 405 |
| 126 | `com.xiaomi.permission.CLOUD_MANAGER` | E MIUI/manufacturer’s proprietary | - | 289 |
| 127 | `com.xiaomi.permission.miuibatteryintelligence` | E MIUI/manufacturer’s proprietary | - | 71 |
| 128 | `com.xiaomi.permission.USE_KEYBAG` | E MIUI/manufacturer’s proprietary | - | 393 |
| 129 | `com.xiaomi.smarthome.READ_PHONE_SCENE_CONDITION` | E MIUI/manufacturer’s proprietary | - | 65 |
| 130 | `com.xiaomi.smarthome.SYNC_PHONE_SCENE_CONDITION` | E MIUI/manufacturer’s proprietary | - | 67 |
| 131 | `com.xiaomi.vtcamera.permission.ACTIVITY_PERMISSION_DECLARE` | E MIUI/manufacturer’s proprietary | - | 423 |
| 132 | `com.xiaomi.vtcamera.permission.SERVICE_PERMISSION_DECLARE` | E MIUI/manufacturer’s proprietary | - | 427 |
| 133 | `com.xiaomi.wearable.provider.device.read` | E MIUI/manufacturer’s proprietary | - | 397 |
| 134 | `com.xiaomi.xmsf.permission.MIPUSH_RECEIVE` | E MIUI/manufacturer’s proprietary | - | 347 |
| 135 | `com.xiaomi.xmsf.permission.USE_XMSF_TRAFFIC` | E MIUI/manufacturer’s proprietary | - | 383 |
| 136 | `miui.packageinstaller.permission.ACTION_INFO` | E MIUI/manufacturer’s proprietary | - | 403 |
| 137 | `miui.permission.ACCESS_AUDIOMONITOR` | E MIUI/manufacturer’s proprietary | - | 429 |
| 138 | `miui.permission.ACCESS_BLE_SETTINGS` | E MIUI/manufacturer’s proprietary | - | 353 |
| 139 | `miui.permission.ACCESS_MILINK_CAST` | E MIUI/manufacturer’s proprietary | - | 461 |
| 140 | `miui.permission.BACKUP_PROXY` | E MIUI/manufacturer’s proprietary | - | 263 |
| 141 | `miui.permission.FILE_PROXY` | E MIUI/manufacturer’s proprietary | - | 257 |
| 142 | `miui.permission.powerkeeper.HIDDEN_MODE_PROVIDER` | E MIUI/manufacturer’s proprietary | - | 323 |
| 143 | `miui.permission.READ_AND_WIRTE_PERMISSION_MANAGER` | E MIUI/manufacturer’s proprietary | - | 277 |
| 144 | `miui.permission.restore_brightness` | E MIUI/manufacturer’s proprietary | - | 411 |
| 145 | `miui.permission.SHELL` | E MIUI/manufacturer’s proprietary | - | 269 |
| 146 | `miui.permission.USE_INTERNAL_GENERAL_API` | E MIUI/manufacturer’s proprietary | - | 17 |
| 147 | `xiaomi.permission.aivision.MI_AI_TRANSLATE` | E MIUI/manufacturer’s proprietary | - | 445 |

### 2.3 High-risk ability sets (this is the direct answer to "why does it have to be pre-installed")

**(a) AOSP signature/privilege level permission 28** - normal applications cannot get it even if they apply dynamically. Only `signature|privileged` + falling in `/system/priv-app` or platform signature will be granted [observation permission name, inferred grant conditions]:

| ability cluster | Involves permissions | what can be done |
|---|---|---|
| SMS dual communication | `READ_SMS` + `SEND_SMS` | Read all text messages (including verification code) + send text messages on behalf of others - combined with `com.miui.permission.MANAGE_SMS_INTERCEPT` (interception rule management), it means "full rights to text message content" |
| Full call rights | `READ_CALL_LOG` + `WRITE_CALL_LOG` + `MODIFY_PHONE_STATE` + `CALL_PHONE` | Read and write call records, change phone status (`MODIFY_PHONE_STATE` is signature|privileged） |
| Device wipe/reboot | **`MASTER_CLEAR`**、`REBOOT`、`DEVICE_POWER` | Factory reset, restart, direct power control |
| Package and process life and death | `FORCE_STOP_PACKAGES`、`CLEAR_APP_USER_DATA`、`CLEAR_APP_CACHE`、`DELETE_CACHE_FILES`、`KILL_BACKGROUND_PROCESSES`、`REAL_GET_TASKS`、`GET_TASKS` | Forcefully stop any application and clear any application data |
| Installation/verification chain | **`PACKAGE_VERIFICATION_AGENT`** | Become a packet verification agent, **blocks/releases the entire system APK and installs** (`PackageVerificationReceiver` in the manifest has a receiver for this action, L2109 / action L2110) |
| USB | **`MANAGE_USB`** | Manage USB device/authorization |
| Application usage and AppOps | `PACKAGE_USAGE_STATS`、`GET_APP_OPS_STATS`、`UPDATE_APP_OPS_STATS`、`BATTERY_STATS` | Read the full usage record, **rewrite the AppOps authorization status of any application**, read the entire system power consumption |
| backup | `BACKUP` | Trigger system backup/restore channel |
| Storage/File System | `MOUNT_UNMOUNT_FILESYSTEMS`、`ACCESS_CACHE_FILESYSTEM`、`MANAGE_EXTERNAL_STORAGE` | Mount the file system and access the cache partition (this APK has used system paths such as `/data/system/dropbox`, `/data/app`, etc., see reports from others) |
| network governance | `MANAGE_NETWORK_POLICY`、`READ_NETWORK_USAGE_HISTORY`、`MODIFY_NETWORK_ACCOUNTING`、`WRITE_APN_SETTINGS`、`CONTROL_VPN` | Change network policy/APN, read full traffic history, **takes over VPN** |
| settings | `WRITE_SETTINGS`、`READ_SECURE_SETTINGS`、`READ_SETTINGS`、`SYSTEM_ALERT_WINDOW` | Change system settings, read secure settings, global floating window |

**(b) 72 MIUI/manufacturer private permission** - only MIUI platform signature or pre-installed system application can hold [observation permission name, inferred grant conditions]. Representative high-risk items:

| Permission | Semantics (inferred by naming) | holder component |
|---|---|---|
| `miui.permission.SHELL` | MIUI shell channel | — |
| `miui.permission.READ_AND_WIRTE_PERMISSION_MANAGER` | Read and write **permission manager** internal data | — |
| `miui.permission.FILE_PROXY` / `miui.permission.BACKUP_PROXY` | File/Backup Agent Channel | — |
| `com.miui.permission.FIREWALL` | firewall rules | `NetworkAssistantProvider` etc. |
| `com.miui.permission.MANAGE_SMS_INTERCEPT` | SMS interception rule management | `AntiSpamProvider` |
| `com.miui.security.permission.installOtherApp` | Install other applications | — |
| `com.miui.securitycenter.permission.GLOBAL_PACKAGEINSTALLER` | global installer | — |
| `miui.packageinstaller.permission.ACTION_INFO` | Installer internal information | — |
| `com.android.settings.permission.SWITCH_DEV_MODE` / `PULL_CLOUD_DATA` | Switch to developer mode/pull cloud configuration | — |
| `com.android.updater.permission.ACCESS_UPDATESERVICE` / `RECEIVE_BROADCAST` | OTA update service | `UpdaterReceiver` |
| `com.miui.monthreport.permission.DATA_MONTH_REPORT` | monthly report data | 3 providers + 2 services reuse it |
| `com.xiaomi.xmsf.permission.MIPUSH_RECEIVE` | Receive push notifications via XMSF | `XiaomiPushReceiver`、`PushMessageHandler` |
| `com.xiaomi.permission.CLOUD_MANAGER` | Cloud synchronization management | `SettingsCloudBackupService` |
| `com.miui.permission.MANAGE_BOOT_TIME` | Change boot time record | — |

**(c) Conclusion**: `sharedUserId=android.uid.system` + 28 AOSP `signature|privileged` + 72 vendor private = this package **It is impossible to install** as a normal application. It must follow the `/system/priv-app/MIUISecurityCenter` or equivalent preinstallation path, and must hold the MIUI platform key signature (see §3). This also explains why it can directly read and write `/data/system/*`.

---

## 3. Signature

REA `inspect_apk_signature`，evidence_id **`ev_774c8c1faf38e245d0c008a0979e7abc3958ee7dd6e00b48ac3bc5de819cb354`**，`authority=shipped-artifact`，`confidence=observed`。

### 3.1 Scheme and Consistency[Observed]

| item | value |
|---|---|
| v1 (JAR) | present，`META-INF/PLATFORM.SF` + `META-INF/PLATFORM.RSA` |
| v2 | present, 1 signer, digest algorithm `sha256` (id 259), `matches_apk: true` |
| v3 | present, 1 signer, `min_sdk=24` `max_sdk=2147483647`, `rotation.present=false` (**no key rotation lineage**) |
| v3.1 | **absent** |
| source stamp | **absent** |
| The strongest solution | v3 |
| `schemes_agree_on_signer` / `v1_signers_match_block_signers` / `self_consistent` | Full `true` |
| `uninterpreted_bytes` | v2 signed_data contains 4 B (not enough to determine deformity) |

⚠️ **REA explicitly declares**: `matches_apk` only means "declared digest == current byte digest", **is not signature verification**; the certificate chain, revocation, and authenticity are not verified [observation limitation]. No independent `apksigner verify` was made in this round.

### 3.2 Certificate[Observed]

```
subject : C=CN, ST=Beijing, L=Beijing, O=Xiaomi, OU=MIUI, CN=MIUI, emailAddress=[VENDOR_EMAIL_REMOVED]
issuer: Same as above (self-signed)
SHA-256 : c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025
serial  : E552A8ECB9011B7C
Validity period: 2011-12-06T03:26:26Z → 2039-04-23T03:26:26Z
Public key: RSA (292 B public key encoding; v1 signature algorithm OID 1.2.840.113549.1.1.1, digest 2.16.840.1.101.3.4.2.1 = SHA-256)
```

Is **a platform signature?** —— Yes, but **is not AOSP universal test platform signature** (`CN=Android, O=Android, C=US`), but **Xiaomi MIUI platform key** (`O=Xiaomi / OU=MIUI / CN=MIUI`, issued in 2011, valid until 2039, self-signed, consistent with all characteristics of "vendor long-term platform key") [observation of certificate fields + inference of "platform key" identity]. This directly explains: The `signature` and `signatureOrSystem` permissions declared/used by this APK can only be granted to each other by applications signed by **with the same MIUI platform key**.

### 3.3 v1 coverage[Observed]

| item | value |
|---|---|
| ZIP Total Entries | **9231** |
| `Name:` entry in `MANIFEST.MF` | **9228** (each with one `SHA-256-Digest`, 9228 in total) |
| Difference 3 items | `META-INF/MANIFEST.MF`, `META-INF/PLATFORM.SF`, `META-INF/PLATFORM.RSA` itself (not included according to JAR specification) |
| `PLATFORM.SF` | `Signature-Version: 1.0`, `SHA-256-Digest-Manifest` 1 piece, `X-Android-APK-Signed: 2, 3` |
| Additional signature file | **does not have**. There is only `MANIFEST.MF / PLATFORM.SF / PLATFORM.RSA / com\android\build\gradle\app-metadata.properties / services\*` under `META-INF\`, **does not have** `CERT.RSA`, there is no second group `.SF/.RSA`, and there is no `.version` series library tag (compared to GuardProvider, which has 40+ `*.version`) |

[Inferred]`X-Android-APK-Signed: 2, 3` means that the v1 block declares "there is also v2/v3", which is consistent with the measured v2/v3 present and belongs to normal packaging (not a sign of "forging v1 after stripping v2/v3").

---

## 4. Manifest full structure

### 4.1 Component list[Observed]

| kind | total | exported=true | exported and **has no permission to protect** | exported and has permissions |
|---|---|---|---|---|
| activity | 293 | 127 | 106 | 21 |
| activity-alias | 2 | 2 | 2 | 0 |
| service | 67 | 21 | 2 | 19 |
| receiver | 22 | 16 | 8 | 8 |
| **provider** | **40** | **28** | **10** | **18** |
| **Total** | **424** | **194** | **128** | **66** |

Supplement[Observed]:
- `android:exported` There are 4 unset components **, and all of them have no intent-filter** → In fact, none of them can be pulled up externally (provider defaults to false when targetSdk≥17, and the rest default to false because there is no filter).
- Therefore, **194 exported all declare** explicitly, and there is no hidden side of "default export".
- There are a total of 6 components of **`android:enabled="false"`, 5 of which are exported**: `MainEntryActivity` (no permission, old entry), `BubblesNotificationListenerService`, `TvmManagerService`, `SuperPowerTileService`, `PerformanceModeTileService`, `PerformanceWildModeTileService`. The disabled component `PackageManager` will refuse to be pulled up, so the truly available **exported = 188**; only `MainEntryActivity` in the A.1–A.5 table is "disabled", and the rest 127 Each zero-permission exported component is the exposed surface of **'s currently valid**.

### 4.2 Complete component list

#### A. exported but **has no permission to protect** (can be pulled up externally, and the component runs as android.uid.system)

##### A.1 provider (10)

| # | components | action / filter | process | authorities | L |
|---|---|---|---|---|---|
| 1 | `com.miui.autotask.provider.AutoTaskServiceProvider` | - | SC.remote | com.miui.autotask.provider.auto_task_service | 1225 |
| 2 | `com.miui.permcenter.privacycenter.PrivacyCenterProvider` | - | SC.remote | com.miui.permcenter.privacycenter | 1358 |
| 3 | `com.miui.securitycenter.provider.DeviceCredentialProvider` | - | SC.remote | com.miui.securitycenter.credentialprovider | 2480 |
| 4 | `com.miui.idprovider.IdProvider` | - | SC.remote | com.miui.idprovider | 2507 |
| 5 | `com.miui.sec.external.InstallProvider` | - | (main process) | com.miui.sec.INSTALL_PACKAGE | 3806 |
| 6 | `com.miui.securitycenter.provider.SecSettingsSearchProvider` | miui.intent.action.SETTINGS_SEARCH_PROVIDER | (main process) | com.miui.securitycenter.provider.SecSettingsSearchProvider | 3813 |
| 7 | `com.miui.securityadd.input.InputProvider` | - | SC.remote | com.miui.input.provider | 4076 |
| 8 | `com.miui.warningcenter.provider.WarningCenterProvider` | - | SC.remote | com.miui.warningcenter.provider | 4147 |
| 9 | `com.miui.securitycenter.SystemAdContentProvider` | - | SC.remote | com.miui.securitycenter.splashAd | 4491 |
| 10 | `com.xiaomi.security.xsof.XSOFProvider` | - | SC.remote | com.xiaomi.security.xsof.provider | 4989 |

##### A.2 service (2)

| # | components | action / filter | process | authorities | L |
|---|---|---|---|---|---|
| 1 | `com.miui.networkassistant.vpn.miui.MiuiVpnManageService` | com.miui.networkassistant.vpn.MIUI_VPN_MANAGE_SERVICE | SC.remote | - | 2974 |
| 2 | `com.xiaomi.security.xsof.MiSafetyDetectService` | com.xiaomi.security.xsof.SAFETY_DETECT_SERVICE | SC.remote | - | 4994 |

##### A.3 receiver (8)

| # | components | action / filter | process | authorities | L |
|---|---|---|---|---|---|
| 1 | `com.miui.apppredict.widget.AppPredictWidget2x4` | android.appwidget.action.APPWIDGET_UPDATE<br>miui.appwidget.action.APPWIDGET_UPDATE<br>com.miui.action.UPDATE_PREDICT_LIST | SC.remote | - | 736 |
| 2 | `com.miui.simlock.BootCompleteReceiver` | android.intent.action.LOCKED_BOOT_COMPLETED | SC.bootaware | - | 1061 |
| 3 | `com.miui.autotask.common.PolarisGeofenceReceiver` | - | SC.remote | - | 1412 |
| 4 | `com.miui.permcenter.privacymanager.widget.BehaviorWidget` | android.appwidget.action.APPWIDGET_UPDATE<br>miui.appwidget.action.APPWIDGET_UPDATE | SC.remote | - | 2119 |
| 5 | `com.miui.permcenter.DeviceOwnerChangedReceiver` | android.app.action.DEVICE_OWNER_CHANGED | (main process) | - | 2135 |
| 6 | `com.miui.permcenter.root.RootUpdateReceiver` | com.android.updater.action.ACQUIRED_ROOT_SUCCESSED | (main process) | - | 2409 |
| 7 | `com.miui.powercenter.provider.DateResetReceiver` | android.intent.action.TIME_SET<br>android.intent.action.TIMEZONE_CHANGED<br>android.intent.action.LOCALE_CHANGED | SC.remote | - | 2418 |
| 8 | `com.miui.securityadd.input.ProvisionCompleteReceiver` | android.provision.action.PROVISION_COMPLETE | SC.remote | - | 4081 |

##### A.4 activity (106)

| # | components | action / filter | process | authorities | L |
|---|---|---|---|---|---|
| 1 | `com.miui.securityscan.MainEntryActivity` | android.intent.action.MAIN<br>mi.quicksearch.intent.action.RETRIEVE<br>miui.intent.action.SECURITY_CENTER<br>android.intent.action.VIEW | (main process) | - | 500 |
| 2 | `com.miui.permcenter.autostart.AutoStartManagementActivity` | miui.intent.action.OP_AUTO_START | (main process) | - | 554 |
| 3 | `com.miui.earthquakewarning.ui.EarthquakeWarningMainActivity` | miui.intent.action.EARTHQUAKE_WARNING_MAIN | (main process) | - | 594 |
| 4 | `com.miui.electricrisk.ElectricRiskMainActivity` | miui.intent.action.ELECTRIC_RISK | (main process) | - | 622 |
| 5 | `com.miui.electricrisk.ElectricProtectPhoneActivity` | - | (main process) | - | 636 |
| 6 | `com.miui.electricrisk.AiGuardPosterActivity` | com.android.settings.action.AI_ENHANCED_SETTINGS | (main process) | - | 643 |
| 7 | `com.miui.permcenter.settings.PrivacyInputModeActivity` | miui.intent.action.PRIVACY_INPUT_MODE_ACTIVITY | (main process) | - | 694 |
| 8 | `com.miui.autotask.activity.TaskManagerActivity` | com.miui.autotask.AUTO_TASK_MANAGER_ACTIVITY | (main process) | - | 850 |
| 9 | `com.miui.autotask.activity.BluetoothSelectActivity` | - | (main process) | - | 938 |
| 10 | `com.miui.autotask.activity.WlanSelectActivity` | - | (main process) | - | 946 |
| 11 | `com.miui.simlock.activity.SimLockChooseActivity` | miui.intent.action.SIM_LOCK_CHOOSE | (main process) | - | 967 |
| 12 | `com.miui.idprovider.ui.legacy.OAIDAppsActivity` | miui.intent.action.OAID_APPS | (main process) | - | 1089 |
| 13 | `com.miui.idprovider.ui.legacy.OAIDSettingsActivityLegacy` | - | (main process) | - | 1103 |
| 14 | `com.miui.idprovider.ui.OAIDAppSettingsActivity` | - | (main process) | - | 1111 |
| 15 | `com.miui.idprovider.ui.OAIDSettingsActivity` | - | (main process) | - | 1119 |
| 16 | `com.miui.permcenter.privacycenter.usage.PermissionUsageRecordActivity` | miui.intent.action.APP_BEHAVIRO_RECORD | (main process) | - | 1316 |
| 17 | `com.miui.permcenter.privacycenter.usage.AppPermissionUsageActivity` | miui.intent.action.APP_PRIVACY_DETAIL | (main process) | - | 1341 |
| 18 | `com.miui.permcenter.settings.InvisibleModeActivity` | com.miui.securitycenter.action.INVISIBLE_SETTING | (main process) | - | 1367 |
| 19 | `com.miui.warningcenter.policeassist.PaSettingActivity` | miui.intent.action.WARNINGCENTER_POLICE_ASSIST | (main process) | - | 1396 |
| 20 | `com.miui.securityscan.MainActivity` | android.intent.action.MAIN<br>mi.quicksearch.intent.action.RETRIEVE<br>miui.intent.action.SECURITY_CENTER<br>android.intent.action.VIEW | (main process) | - | 1461 |
| 21 | `com.miui.securityscan.ui.settings.SettingsActivity` | miui.intent.action.APP_SETTINGS<br>com.miui.securitycenter.action.SECURITYCENTER_SETTINGS | (main process) | - | 1508 |
| 22 | `com.miui.firstaidkit.FirstAidKitActivity` | com.miui.securitycenter.action.FIRST_AID_KIT | (main process) | - | 1535 |
| 23 | `com.miui.antivirus.activity.MainActivity` | miui.intent.action.ANTI_VIRUS | (main process) | - | 1581 |
| 24 | `com.miui.antivirus.activity.SettingsActivity` | com.miui.securitycenter.action.ANTIVIRUS_SETTINGS | (main process) | - | 1595 |
| 25 | `com.miui.antivirus.whitelist.WhiteListActivity` | miui.intent.action.ANTI_VIRUS_WHITELIST | (main process) | - | 1610 |
| 26 | `com.miui.permcenter.MainAcitivty` | miui.intent.action.LICENSE_MANAGER | (main process) | - | 1648 |
| 27 | `com.miui.permcenter.permissions.AppPermissionsTabActivity` | - | (main process) | - | 1668 |
| 28 | `com.miui.permcenter.permissions.PermissionsEditorActivity` | miui.intent.action.APP_PERM_EDITOR_PRIVATE<br>miui.intent.action.APP_PERM_EDITOR | (main process) | - | 1675 |
| 29 | `com.miui.permcenter.permissions.PermissionAppsEditorActivity` | miui.intent.action.PERMISSION_APP_EDITOR_ACTIVITY | (main process) | - | 1695 |
| 30 | `com.miui.devicepermission.editpermission.DevicePermissionModifyActivity` | - | (main process) | - | 1725 |
| 31 | `com.miui.powercenter.PowerMainActivity` | miui.intent.action.POWER_MANAGER<br>miui.intent.action.POWER_BATTERY<br>android.intent.action.POWER_USAGE_SUMMARY | (main process) | - | 1877 |
| 32 | `com.miui.powercenter.PowerCenter` | miui.intent.action.POWER_SCAN | (main process) | - | 1903 |
| 33 | `com.miui.powercenter.bootshutdown.PowerShutdownOnTime` | miui.powercenter.intent.action.BOOT_SHUTDOWN_ONTIME | (main process) | - | 1917 |
| 34 | `com.miui.powercenter.legacypowerrank.PowerDetailActivity` | android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | (main process) | - | 1931 |
| 35 | `com.miui.powercenter.PowerSettings` | com.miui.securitycenter.action.POWER_SETTINGS<br>com.miui.securitycenter.action.FIRMWARE_UPDATE | (main process) | - | 1988 |
| 36 | `com.miui.powercenter.savemode.PowerSaveActivity` | com.miui.powercenter.POWER_SAVE | (main process) | - | 2004 |
| 37 | `com.miui.powercenter.wirelesscharge.WirelessChargingDialogActivity` | miui.intent.action.ACTIVITY_REVERSE_WIRELESS_CHG_DIALOG | (main process) | - | 2038 |
| 38 | `com.miui.powercenter.autotask.AutoTaskManageActivity` | com.miui.powercenter.AUTO_TASK<br>miui.intent.action.AUTO_TASK | (main process) | - | 2052 |
| 39 | `com.miui.permcenter.install.AdbInstallActivity` | com.miui.securitycenter.intent.action.INSTALL_PACKAGE | (main process) | - | 2091 |
| 40 | `com.miui.antivirus.activity.MonitoredAppSettingsActivity` | miui.intent.action.SAFE_PAY_MONITOR_SETTINGS | (main process) | - | 2171 |
| 41 | `com.miui.optimizemanage.OptimizemanageMainActivity` | miui.intent.action.OPTIMIZE_MANAGE | (main process) | - | 2200 |
| 42 | `com.miui.googlebase.ui.GmsCoreSettings` | miui.intent.action.APP_SETTINGS | (main process) | - | 2236 |
| 43 | `com.miui.applicationlock.LockChooseAccessControl` | com.miui.securitycenter.action.LOCKCHOOSEACCESSCONTROL | (main process) | - | 2558 |
| 44 | `com.miui.applicationlock.ConfirmAccessControl` | miui.intent.action.CHECK_ACCESS_CONTROL | (main process) | - | 2576 |
| 45 | `com.miui.applicationlock.FirstUseAppLockActivity` | com.miui.securitycenter.action.LOCK_APP | (main process) | - | 2611 |
| 46 | `com.miui.applicationlock.TransitionHelper` | com.miui.securitycenter.action.TRANSITION | (main process) | - | 2653 |
| 47 | `com.miui.networkassistant.ui.NetworkAssistantActivity` | miui.intent.action.NETWORKASSISTANT_ENTRANCE | (main process) | - | 2670 |
| 48 | `com.miui.networkassistant.ui.activity.FirewallActivity` | miui.intent.action.NETWORKASSISTANT_FIREWALL | (main process) | - | 2684 |
| 49 | `com.miui.networkassistant.ui.GetChargeTrafficActivity` | - | (main process) | - | 2698 |
| 50 | `com.miui.networkassistant.ui.order.orderlist.BhOrderListActivity` | android.intent.action.VIEW | (main process) | - | 2706 |
| 51 | `com.miui.networkassistant.ui.revert.RevertResultActivity` | - | (main process) | - | 2724 |
| 52 | `com.miui.networkassistant.ui.order.orderdetail.BhNormalOrderDetailActivity` | - | (main process) | - | 2731 |
| 53 | `com.miui.networkassistant.ui.GetPayStatusActivity` | android.intent.action.VIEW | (main process) | - | 2739 |
| 54 | `com.miui.networkassistant.ui.base.UniversalFragmentActivity` | miui.intent.action.NETWORKASSISTANT_UNIVERSAL_FRAGMENT_ACTIVITY<br>miui.intent.action.NETWORKASSISTANT_APP_DETAIL<br>android.intent.action.VIEW<br>miui.intent.action.NETWORKASSISTANT_TC_DIAGNOSTIC<br>miui.intent.action.NETWORKASSISTANT_SMS_REPORT<br>miui.intent.action.BG_NETWORK_CONTROL | (main process) | - | 2759 |
| 55 | `com.miui.networkassistant.ui.base.UniversalPreferenceActivity` | miui.intent.action.NETWORKASSISTANT_UNIVERSAL_PREFERENCE_ACTIVITY<br>miui.intent.action.NETWORKASSISTANT_SETTINGS<br>miui.intent.action.NETWORKASSISTANT_MONTH_PACKAGE_SETTING<br>miui.intent.action.NETWORKASSISTANT_AUTO_TRAFFIC_CORRECTION_SETTING<br>miui.intent.action.NETWORKASSISTANT_STATUS_BAR_SETTING<br>miui.intent.action.NETWORKASSISTANT_OPERATOR_SETTING<br>miui.intent.action.NETWORKASSISTANT_LIMIT_SETTING<br>miui.intent.action.NETWORKASSISTANT_SET_PACKAGE_GUIDE | (main process) | - | 2806 |
| 56 | `com.miui.networkassistant.ui.activity.TrafficSortedActivity` | miui.intent.action.NETWORKASSISTANT_TRAFFIC_SORTED | (main process) | - | 2868 |
| 57 | `com.miui.networkassistant.ui.activity.InternationalRoamingSettingActivity` | - | (main process) | - | 2917 |
| 58 | `com.miui.networkassistant.ui.activity.TetheringStatsSettingActivity` | - | (main process) | - | 2926 |
| 59 | `com.miui.wakepath.ui.ConfirmStartActivity` | android.app.action.CHECK_ALLOW_START_ACTIVITY | (main process) | - | 3029 |
| 60 | `com.miui.luckymoney.ui.activity.LuckySettingActivity` | miui.intent.action.HB_MAIN_ACTIVITY<br>miui.intent.action.LUCKY_SETTING_ACTIVITY<br>android.intent.action.VIEW | (main process) | - | 3048 |
| 61 | `com.miui.luckymoney.ui.activity.SecondarySettingActivity` | miui.intent.action.LUCKY_MORE_SETTING | (main process) | - | 3092 |
| 62 | `com.miui.luckymoney.ui.activity.RemoveLuckyMoneyActivity` | miui.intent.action.LUCKY_ALARM_SETTING | (main process) | - | 3106 |
| 63 | `com.miui.appmanager.AppManagerMainActivity` | miui.intent.action.APP_MANAGER | (main process) | - | 3142 |
| 64 | `com.miui.appmanager.ApplicationsDetailsActivity` | miui.intent.action.APP_MANAGER_APPLICATION_DETAIL | (main process) | - | 3157 |
| 65 | `com.miui.appmanager.AppManagerSettings` | - | (main process) | - | 3171 |
| 66 | `com.miui.antispam.ui.activity.MainActivity` | miui.intent.action.SET_FIREWALL<br>android.intent.action.VIEW | (main process) | - | 3196 |
| 67 | `com.miui.antispam.ui.activity.AddAntiSpamActivity` | miui.intent.action.ADD_FIREWALL | (main process) | - | 3232 |
| 68 | `com.miui.antispam.ui.activity.RemoveBlacklistActivity` | miui.intent.action.REMOVE_BLACKLIST | (main process) | - | 3246 |
| 69 | `com.miui.antispam.ui.activity.AntiSpamNewSettingsActivity` | com.miui.antispam.action.ANTISPAM_SETTINGS | (main process) | - | 3259 |
| 70 | `com.miui.antispam.ui.activity.CallInterceptSettingsActivity` | miui.intent.action.CALL_FIREWALL | (main process) | - | 3280 |
| 71 | `com.miui.antispam.ui.activity.BlackListActivity` | miui.intent.action.VIEW_BLACKLIST | (main process) | - | 3323 |
| 72 | `com.miui.antispam.ui.activity.MarkNumGuideActivity` | - | (main process) | - | 3345 |
| 73 | `com.miui.cleanmaster.CleanerInstallActivity` | com.miui.cleanmaster.InstallAndLunchCleanMaster<br>com.miui.cleaner.InstallAndLunchCleaner | (main process) | - | 3363 |
| 74 | `com.miui.cleanmaster.LowMemoryIntentDispatchActivity` | com.miui.securitycenter.LunchCleanMaster<br>com.miui.securitycenter.LunchCleaner<br>com.miui.securitycenter.action.START_LOW_MEMORY_CLEAN | (main process) | - | 3381 |
| 75 | `com.miui.appcompatibility.AppExcepitonTipsActivity` | com.miui.appcompatibility.LaunchDialog.launcher<br>com.miui.appcompatibility.LaunchDialog.appstore | (main process) | - | 3406 |
| 76 | `com.miui.gamebooster.ui.WindowCallActivity` | com.miui.gamebooster.action.ACCESS_WINDOWCALLACTIVITY | (main process) | - | 3423 |
| 77 | `com.miui.gamebooster.ui.WelcomActivity` | com.miui.gamebooster.action.ACCESS_MAINACTIVITY<br>com.miui.gamebooster.action.MI_PUSH_GAMEBOOSTER_HOT | (main process) | - | 3492 |
| 78 | `com.miui.gamebooster.ui.GameBoosterMainActivity` | - | (main process) | - | 3513 |
| 79 | `com.miui.gamebooster.ui.WonderfulMomentActivity` | com.miui.securitycenter.action.WONDERFUL_MOMENT | (main process) | - | 3591 |
| 80 | `com.miui.gamebooster.ui.QuickReplySettingsActivity` | miui.intent.action.QUICK_REPLY_SETTINGS | (main process) | - | 3679 |
| 81 | `com.miui.gamebooster.shoulderkey.ShoulderKeyLightEffectActivity` | com.miui.gamebooster.action.ACCESS_LIGHTEFFECT | (main process) | - | 3702 |
| 82 | `com.miui.privacyapps.ui.PrivacyAppsManageActivity` | miui.intent.action.MANAGE_PRIVACY_APPS | (main process) | - | 3737 |
| 83 | `com.miui.permcenter.permissions.SystemAppPermissionDialogActivity` | miui.intent.action.SYSTEM_PERMISSION_DECLARE<br>miui.intent.action.SYSTEM_PERMISSION_DECLARE_NEW<br>miui.intent.action.CTA_DECLARE<br>miui.intent.action.SYSTEM_PERMISSION_DECLARE_NO_OPTIONAL<br>miui.intent.action.SYSTEM_PERMISSION_DECLARE<br>miui.intent.action.SYSTEM_PERMISSION_DECLARE_NEW<br>miui.intent.action.CTA_DECLARE<br>miui.intent.action.SYSTEM_PERMISSION_DECLARE_NO_OPTIONAL | (main process) | - | 3850 |
| 84 | `com.miui.superpower.SuperPowerSettings` | com.miui.powercenter.SUPERPOWER_SAVE<br>com.miui.powercenter.SUPERPOWER_SAVE_NEW | (main process) | - | 3909 |
| 85 | `com.miui.earthquakewarning.ui.EarthquakeWarningEmergencyEditActivity` | - | (main process) | - | 3998 |
| 86 | `com.miui.earthquakewarning.ui.EarthquakeWarningMonitorActivity` | - | (main process) | - | 4052 |
| 87 | `com.miui.warningcenter.WarningCenterMainActivity` | miui.intent.action.WARNINGCENTER_MAIN | (main process) | - | 4108 |
| 88 | `com.miui.warningcenter.mijia.WarningCenterMijiaActivity` | miui.intent.action.WARNINGCENTER_MIJIA | (main process) | - | 4121 |
| 89 | `com.miui.permcenter.settings.PrivacyProvisionActivity` | - | (main process) | - | 4155 |
| 90 | `com.miui.permcenter.privacycenter.PrivacySafetyActivity` | miui.intent.action.PRIVACY_SETTINGS<br>miui.intent.action.SECURITY_CENTER_SETTINGS<br>miui.intent.action.PRIVACY_SAFETY | (main process) | - | 4162 |
| 91 | `com.miui.optimizecenter.storage.StorageActivity` | com.miui.cleanmaster.action.STORAGE_MANAGE<br>com.miui.securitycenter.action.STORAGE_MANAGE | (main process) | - | 4194 |
| 92 | `com.miui.zman.ui.ShareActivity` | miui.intent.action.XMAN_SHARE_MANAGER | (main process) | - | 4292 |
| 93 | `com.miui.zman.ui.LoadingActivity` | - | (main process) | - | 4304 |
| 94 | `com.miui.warningcenter.disasterwarning.WarningCenterDisasterActivity` | miui.intent.action.WARNINGCENTER_DISASTER | (main process) | - | 4370 |
| 95 | `com.miui.permcenter.detection.PrivacyRiskDetectionActivity` | miui.intent.action.PRIVACY_RISK_DETECTION_ACTIVITY | (main process) | - | 4444 |
| 96 | `com.miui.permcenter.privacyblur.PrivacyThumbnailBlurSettings` | com.miui.securitycenter.action.PRIVACY_THUMBNAIL_BLUR_SETTING | (main process) | - | 4467 |
| 97 | `com.miui.gamebooster.beauty.BeautySettingsActivity` | com.miui.gamebooster.action.ACCESS_FRONT_ASSISTANT | (main process) | - | 4553 |
| 98 | `com.miui.powercenter.charge.ChargeFeatureActivity` | miui.intent.action.CHARGE_FEATURE_MANAGER<br>miui.intent.action.POWER_WIRELESS_REVERSE_LIST<br>com.miui.powercenter.FAST_CHARGE | (main process) | - | 4587 |
| 99 | `com.miui.dock.settings.DockSettingsActivity` | com.miui.dock.ACTION_SETTINGS<br>com.miui.dock.ACTION_DOCK_SETTINGS<br>com.miui.gamebooster.action.ACCESS_BEAUTY<br>com.miui.gamebooster.action.ACCESS_CONVERSATION<br>com.miui.gamebooster.action.VIDEOBOX_SETTINGS<br>com.miui.gamebooster.action.VIDEOBOX_SETTINGS_ALL | (main process) | - | 4612 |
| 100 | `com.miui.permcenter.WLANVerificationActivity` | miui.intent.action.OPEN_WIFI_VERIFY | (main process) | - | 4700 |
| 101 | `com.miui.gamebooster.gamemode.GameModeSettingsActivity` | com.miui.gamebooster.action.ACCESS_BIGWINDOW | (main process) | - | 4715 |
| 102 | `com.xiaomi.mipush.sdk.NotificationClickedActivity` | - | (main process) | - | 4729 |
| 103 | `com.miui.gamebooster.beauty.PrivacyCameraGlobalSettingsActivity` | - | (main process) | - | 4806 |
| 104 | `com.miui.idprovider.ui.GrantPermissionActivity` | miui.intent.action.REQUEST_PERMISSIONS_FOR_OAID | (main process) | - | 4858 |
| 105 | `com.miui.bubbles.settings.BubbleAppManageActivity` | - | :ui | - | 4940 |
| 106 | `com.miui.bubbles.settings.BubbleSettingsActivity` | com.miui.bubbles.BUBBLES_SETTINGS | :ui | - | 4951 |

##### A.5 activity-alias (2)

| # | components | action / filter | process | authorities | L |
|---|---|---|---|---|---|
| 1 | `com.miui.permcenter.privacymanager.behaviorrecord.PrivacyDetailActivity` | - | (main process) | - | 1352 |
| 2 | `com.miui.permcenter.settings.PrivacySettingsActivity` | - | (main process) | - | 4177 |

#### B. exported and **has permission to protect**

##### B.1 provider (18)

| # | components | Protect permissions | action / filter | process | L |
|---|---|---|---|---|---|
| 1 | `com.miui.autotask.provider.AutoTaskProvider` | com.miui.autotask.permission.CONTENTPROVIDER | - | SC.remote | 1219 |
| 2 | `com.miui.permcenter.privacymanager.behaviorrecord.PackageBehaviorContentProvider` | com.miui.securitycenter.permission.READ_APP_BEHAVIORS | - | (main process) | 1380 |
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
| 13 | `com.miui.zman.ZmanProvider` | com.miui.zman.permission.SECURITY_SHARE | - | (main process) | 4363 |
| 14 | `com.miui.securitycenter.WidgetProvider` | com.miui.securitycenter.permission.WIDGET_PROVIDER | - | SC.remote | 4518 |
| 15 | `com.miui.globalsatisfaction.provider.GsProvider` | com.miui.globalsatisfaction.permission.PROVIDER | miui.intent.action.SETTINGS_NOTIFICATION_PROVIDER | SC.remote | 4740 |
| 16 | `com.miui.permcenter.permissions.acrossterminal.AcrossTerminalPermissionContentProvider` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | - | SC.remote | 4757 |
| 17 | `com.miui.privacyapps.PrivacyAppsProvider` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | - | (main process) | 4925 |
| 18 | `com.miui.riskcontrol.SensitiveWordDetectProvider` | com.miui.security.permission.DETECT_TEXT | - | (main process) | 4933 |

##### B.2 service (19)

| # | components | Protect permissions | action / filter | process | L |
|---|---|---|---|---|---|
| 1 | `com.miui.apppredict.service.AiService` | miui.permission.APP_PREDICT_VERIFY | - | SC.remote | 725 |
| 2 | `com.miui.bubbles.services.BubblesNotificationListenerService` | android.permission.BIND_NOTIFICATION_LISTENER_SERVICE | android.service.notification.NotificationListenerService | SC.remote | 1178 |
| 3 | `com.miui.tvm.service.TvmManagerService` | miui.permission.TVM | miui.intent.action.START_TVM | (main process) | 1264 |
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
| 19 | `com.xiaomi.mipush.sdk.PushMessageHandler` | com.xiaomi.xmsf.permission.MIPUSH_RECEIVE | - | (main process) | 5089 |

##### B.3 receiver (8)

| # | components | Protect permissions | action / filter | process | L |
|---|---|---|---|---|---|
| 1 | `com.miui.antifraud.AntiFraudReceiver` | android.permission.MODIFY_PHONE_STATE | android.intent.action.PHONE_STATE<br>miui.action.mms.SCAN_RISKY_URL | SC.remote | 1130 |
| 2 | `com.miui.push.XiaomiPushReceiver` | com.xiaomi.xmsf.permission.MIPUSH_RECEIVE | com.xiaomi.mipush.RECEIVE_MESSAGE<br>com.xiaomi.mipush.MESSAGE_ARRIVED<br>com.xiaomi.mipush.ERROR | SC.remote | 1278 |
| 3 | `com.miui.permcenter.install.PackageVerificationReceiver` | android.permission.INSTALL_PACKAGES | com.miui.action.PACKAGE_NEEDS_VERIFICATION | SC.remote | 2109 |
| 4 | `com.miui.securitycenter.receiver.LauncherLoadingFinishedReceiver` | miui.permission.USE_INTERNAL_GENERAL_API | com.miui.home.intent.action.LOADING_FINISHED | SC.remote | 2446 |
| 5 | `com.miui.antivirus.receiver.UpdaterReceiver` | com.android.updater.permission.RECEIVE_BROADCAST | com.android.updater.action.UPDATE_SUCCESSED | SC.remote | 2456 |
| 6 | `com.miui.securityscan.cards.DownloadInstallReceiver` | miui.permission.USE_INTERNAL_GENERAL_API | com.xiaomi.market.DOWNLOAD_INSTALL_RESULT | (main process) | 2999 |
| 7 | `com.miui.analytics.RemoteAnalyticsReceiver` | com.miui.securitycenter.permission.REMOTE_ANALYTICS | com.miui.security.remote.analytics | SC.remote | 3940 |
| 8 | `androidx.profileinstaller.ProfileInstallReceiver` | android.permission.DUMP | androidx.profileinstaller.action.INSTALL_PROFILE<br>androidx.profileinstaller.action.SKIP_FILE<br>androidx.profileinstaller.action.SAVE_PROFILE<br>androidx.profileinstaller.action.BENCHMARK_OPERATION | (main process) | 5105 |

##### B.4 activity (21)

| # | components | Protect permissions | action / filter | process | L |
|---|---|---|---|---|---|
| 1 | `com.miui.permcenter.settings.ShutDownPasswordActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | miui.intent.action.SHUT_DOWN_PASSWORD_ACTIVITY | (main process) | 708 |
| 2 | `com.miui.simlock.activity.SimLockStartActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | miui.intent.action.SIM_LOCK_START | (main process) | 982 |
| 3 | `com.miui.permcenter.ctaverify.SendSmsVerificationActivity` | android.permission.BROADCAST_SMS | miui.intent.action.SEND_SMS_VERIFY | (main process) | 1148 |
| 4 | `com.miui.permcenter.NotificationVerifyActivity` | miui.permission.NOTIFICATION_VERIFY | miui.intent.action.NOTIFICATION_VERIFY | (main process) | 1192 |
| 5 | `com.miui.permcenter.AccessibilityServiceActivity` | miui.permission.ACCESSIBILITY_SERVICE | miui.intent.action.ACCESSIBILITY_SERVICE | (main process) | 1424 |
| 6 | `com.miui.devicepermission.editpermission.DevicePermissionSettingsActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | miui.intent.action.DEVICE_PERMISSION_SETTINGS | (main process) | 1711 |
| 7 | `com.miui.devicepermission.editpermission.DevicePermissionEditorActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | miui.intent.action.EDITOR_DEVICE_PERMISSIONS | (main process) | 1734 |
| 8 | `com.miui.devicepermission.grantpermission.GrantDevicePermissionActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | miui.intent.action.REQUEST_DEVICE_PERMISSIONS | (main process) | 1749 |
| 9 | `com.miui.permcenter.SettingsAcitivty` | android.permission.CHANGE_COMPONENT_ENABLED_STATE | com.miui.securitycenter.action.PERMISSION_SETTINGS | (main process) | 1856 |
| 10 | `com.miui.gamebooster.ui.SettingsActivity` | com.miui.gamebooster.access.ACTIVITY | com.miui.gamebooster.action.GLOBAL_SETTINGS<br>android.intent.action.VIEW | (main process) | 3626 |
| 11 | `com.miui.gamebooster.ui.SelectGameLandActivity` | com.miui.gamebooster.access.ACTIVITY | com.miui.gamebooster.action.SELECT_GAME | (main process) | 3654 |
| 12 | `com.miui.privacyapps.ui.PrivacyAppsHelper` | miui.permission.USE_INTERNAL_GENERAL_API | miui.intent.action.PRIVACY_APPS_HELPER | (main process) | 3773 |
| 13 | `com.miui.permcenter.permissions.AppPermissionsUseActivity` | com.miui.securitycenter.permission.PERM_USE_INFO | miui.intent.action.APP_PERMISSION_USE | (main process) | 3795 |
| 14 | `com.miui.securitycenter.activity.ThirdDesktopAlertActivity` | com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER | - | (main process) | 3829 |
| 15 | `com.miui.optimizecenter.storage.FboResultActivity` | miui.permission.USE_INTERNAL_GENERAL_API | miui.intent.action.FBO_RESULT | (main process) | 4215 |
| 16 | `com.miui.zman.ui.PrivacyProtectSettingsActivity` | com.miui.zman.permission.SECURITY_SHARE | miui.intent.action.ZMAN_PRIVACY_PROTECT_SETTING | (main process) | 4316 |
| 17 | `com.miui.zman.ui.ShareSettingActivity` | com.miui.zman.permission.SECURITY_SHARE | miui.intent.action.ZMAN_SECURITY_SHARE_SETTING | (main process) | 4330 |
| 18 | `com.miui.zman.ui.PrivacyProtectTipSettingsActivity` | com.miui.zman.permission.SECURITY_SHARE | miui.intent.action.ZMAN_PRIVACY_PROTECT_TIP_SETTING | (main process) | 4343 |
| 19 | `com.miui.permcenter.settings.TileTransitActivity` | android.permission.BIND_QUICK_SETTINGS_TILE | android.service.quicksettings.action.QS_TILE_PREFERENCES | (main process) | 4500 |
| 20 | `com.miui.permcenter.settings.OtherPermissionsActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | - | (main process) | 4579 |
| 21 | `com.miui.combinepermission.grantpermission.CombinePermissionActivity` | com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE | miui.intent.action.REQUEST_PERMISSIONS_COMBINE<br>miui.intent.action.CTA_REQUEST_PERMISSIONS_COMBINE | (main process) | 4875 |

#### C. Non-exported but important components (extract)

| kind | components | process | Notes | L |
|---|---|---|---|---|
| activity | `com.miui.gamebooster.ui.GameBoosterRealMainActivity` | (main process) | Internal boot only | 3536 |
| provider | `com.miui.apppredict.db.WidgetBlackListContentProvider` | (main process) | authority=com.miui.securitycenter.widget.black.list.provider | 730 |
| provider | `com.miui.powercenter.autotask.AutoTaskProvider` | SC.remote | authority=com.miui.powercenter.autotask | 2501 |
| provider | `com.miui.powercenter.batteryhistory.BatteryHistoryProvider` | (main process) | authority=com.miui.powercenter.batteryhistory | 2512 |
| provider | `androidx.core.content.FileProvider` | SC.remote | authority=com.miui.networkassistant.fileprovider grantUri=true | 2992 |
| provider | `com.miui.earthquakewarning.db.EarthquakeContentProvider` | SC.remote | authority=com.miui.earthquakewarning.EarthquakeContentProvider | 4039 |
| provider | `com.miui.earthquakewarning.db.EarthquakeContentProvider2` | SC.remote | authority=com.miui.earthquakewarning.EarthquakeContentProvider2 | 4045 |
| provider | `com.miui.zman.ui.ZmanFileProvider` | (main process) | authority=com.miui.securitycenter.zman.fileProvider grantUri=true | 4355 |
| provider | `com.miui.warningcenter.disasterwarning.db.DisasterContentProvider` | SC.remote | authority=com.miui.warningcenter.DisasterContentProvider | 4421 |
| provider | `com.miui.warningcenter.disasterwarning.db.DisasterAreaProvider` | SC.remote | authority=com.miui.warningcenter.DisasterAreaProvider | 4479 |
| provider | `com.miui.warningcenter.disasterwarning.db.DisasterSubscribeProvider` | SC.remote | authority=com.miui.warningcenter.DisasterSubscribeProvider | 4485 |
| provider | `androidx.startup.InitializationProvider` | (main process) | authority=com.miui.securitycenter.androidx-startup | 5067 |
| provider | `com.xiaomi.continuity.InitializationProvider` | (main process) | authority=com.miui.securitycenter.continuity-startup | 5134 |

### 4.3 `<provider>` authorities full list (40, no duplicate authorities) [Observed]

| authority | components | exported | Protect permissions | grantUri | process |
|---|---|---|---|---|---|
| `com.miui.securitycenter.widget.black.list.provider` | `com.miui.apppredict.db.WidgetBlackListContentProvider` | false | — | — | Lord |
| `com.miui.autotask.provider.auto_task` | `com.miui.autotask.provider.AutoTaskProvider` | true | `com.miui.autotask.permission.CONTENTPROVIDER` (0x3) | — | remote |
| `com.miui.autotask.provider.auto_task_service` | `AutoTaskServiceProvider` | **true** | **No** | — | remote |
| `com.miui.permcenter.privacycenter` | `PrivacyCenterProvider` | **true** | **No** | — | remote |
| `com.miui.securitycenter.appbehaviors` | `PackageBehaviorContentProvider` | true | `...permission.READ_APP_BEHAVIORS` (0x3) | — | Lord |
| `com.miui.monthreport` | `MonthReportProvider` | true | `com.miui.monthreport.permission.DATA_MONTH_REPORT` | — | remote |
| `com.miui.securitycenter.provider` | `SecurityCenterProvider` | true | `...ACCESS_SECURITY_CENTER_PROVIDER` (0x3) | — | remote |
| `com.miui.securitycenter.credentialprovider` | **`DeviceCredentialProvider`** | **true** | **No** | — | remote |
| `com.miui.securitycenter.remoteprovider` | `RemoteProvider` | true | `...ACCESS_SECURITY_CENTER_PROVIDER` | — | remote |
| `com.miui.powercenter.provider` | `PowerCenterProvider` | true | `com.miui.powercenter.permission.POWER_DATA` | — | remote |
| `com.miui.powercenter.autotask` | `AutoTaskProvider`(powercenter) | false | — | — | remote |
| **`com.miui.idprovider`** | `IdProvider`（OAID） | **true** | **No** | — | remote |
| `com.miui.powercenter.batteryhistory` | `BatteryHistoryProvider` | false | — | — | Lord |
| `com.miui.networkassistant.provider` | `NetworkAssistantProvider` | true | `miui.permission.EXTRA_NETWORK` (0x3) | — | remote |
| `com.miui.networkassistant.fileprovider` | `androidx.core.content.FileProvider` | false | — | **true** | remote |
| `com.miui.powercenter.powersaver` | `PowerSaverProvider` | true | `...permission.POWER_COMMAND` | — | remote |
| `com.miui.luckymoney.provider` | `LuckyMoneyProvider` | true | `miui.permission.EXTRA_NETWORK` | — | remote |
| **`antispam`** | `AntiSpamProvider` | true | `com.miui.antispam.permission.CONTENTPROVIDER` (0x3) | — | remote |
| `com.miui.securitycenter.gamebooster` | `GameBoosterProvider` | true | `...GAMEBOOSTER_PROVIDER` (0x3) | — | remote |
| **`com.miui.sec.INSTALL_PACKAGE`** | **`com.miui.sec.external.InstallProvider`** | **true** | **No** | — | Lord |
| `com.miui.securitycenter.provider.SecSettingsSearchProvider` | Same name | **true** | **No** | **true** | Lord |
| `com.miui.sec.THIRD_DESKTOP` | `ThirdMonitorProvider` | true | `...ACCESS_SECURITY_CENTER_PROVIDER` | — | remote |
| `com.miui.earthquakewarning.EarthquakeContentProvider` | Same name | false | — | — | remote |
| `com.miui.earthquakewarning.EarthquakeContentProvider2` | Same name 2 | false | — | — | remote |
| **`com.miui.input.provider`** | `com.miui.securityadd.input.InputProvider` | **true** | **No** | — | remote |
| `com.miui.warningcenter.provider` | `WarningCenterProvider` | **true** | **No** | — | remote |
| `com.miui.securitycenter.zman.fileProvider` | `ZmanFileProvider` | false | — | **true** | Lord |
| `com.miui.securitycenter.zman.provider` | `ZmanProvider` | true | `com.miui.zman.permission.SECURITY_SHARE` | — | Lord |
| `com.miui.warningcenter.DisasterContentProvider` | Same name | false | — | — | remote |
| `com.miui.warningcenter.DisasterAreaProvider` | Same name | false | — | — | remote |
| `com.miui.warningcenter.DisasterSubscribeProvider` | Same name | false | — | — | remote |
| `com.miui.securitycenter.splashAd` | `SystemAdContentProvider` | **true** | **No** | — | remote |
| `com.miui.securitycenter.widgetProvider` | `WidgetProvider` | true | `...WIDGET_PROVIDER` (0x3) | — | remote |
| **`com.xiaomi.account.provider.XiaomiAccountSettingsNotificationProvider`** | `com.miui.globalsatisfaction.provider.GsProvider` | true | `com.miui.globalsatisfaction.permission.PROVIDER` (0x3) | **true** | remote |
| `com.miui.permissions.acrossterminal` | `AcrossTerminalPermissionContentProvider` | true | `...SYSTEM_PERMISSION_DECLARE` (0x3) | — | remote |
| `com.miui.securitycenter.app.hide` | `PrivacyAppsProvider` | true | `...SYSTEM_PERMISSION_DECLARE` | — | Lord |
| `com.miui.riskcontrol` | `SensitiveWordDetectProvider` | true | `com.miui.security.permission.DETECT_TEXT` (0x3) | — | Lord |
| **`com.xiaomi.security.xsof.provider`** | `XSOFProvider` | **true** | **No** | — | remote |
| `com.miui.securitycenter.androidx-startup` | `androidx.startup.InitializationProvider` | false | — | — | Lord |
| `com.miui.securitycenter.continuity-startup` | `com.xiaomi.continuity.InitializationProvider` | false | — | — | Lord |

**has authorized URI with only 4** (decoded.xml L2992 / L3813 / L4355 / L4740): `androidx.core.content.FileProvider` (networkassistant.fileprovider, exported=false), `SecSettingsSearchProvider` (**exported=true and no permission**), `ZmanFileProvider` (exported=false), `GsProvider` (exported=true, with 0x3 permissions). **`SecSettingsSearchProvider` is the only provider with "exported + no permissions + grantUriPermissions=true", which is the point that needs the most verification in this round (see §8/§9).**

**authority namespace impersonates** [Observed]: `com.xiaomi.account.provider.XiaomiAccountSettingsNotificationProvider`, `com.miui.sec.INSTALL_PACKAGE`, `com.miui.input.provider`, `com.miui.permissions.acrossterminal`, `com.xiaomi.security.xsof.provider` all fall under the package name prefix **of other system applications**. [Inferred] This is the "authoritative naming convention" within MIUI (hanging capabilities under the name of the conceptual host), not Squid; if external applications make trust judgments based on authority, they will misjudge the source.

### 4.4 `<meta-data>`: 82 items at application level + 60 items at component level

None of **is appid/appkey/secret** of SDK” - in contrast to GuardProvider’s `AVL_OPENSDK_APPKEY=D5DFB878...`. [Observed]

In the application-level meta-data, **has the information** (the rest are the function switches/version numbers of this APK):

| key | value | meaning (inference) | L |
|---|---|---|---|
| `sc_version` | `1de6cc0dd33cc321d8ca9e9ad4143829e5f3a187` | Build commit (40 hex, SHA-1 form) | 4651 |
| `flavor_region` | `cn` | Chinese version channel | 4649 |
| `miui_private_label` | `PRIVATE_V150` | MIUI private branch label | 4657 |
| `os_private_label` | `PRIVATE_OS020` | OS private branch tags | 4659 |
| `market_sdk_version` | `3.2.26` | Xiaomi App Store SDK version | 5002 |
| `MIPUSH_SDK_VERSION_CODE` / `_NAME` | `50708` / `5_7_8-C` | MiPush version | 5081 / 5083 |
| `com.xiaomi.continuity.sdk.VERSION_CODE` / `_NAME` | `3001561` / `3.1.561.10.1201105` | Interoperability SDK version | 5127 / 5129 |
| `com.xiaomi.security.xsof.service.version` / `required.client.version` | `1` / `1` | XSOF interface contract version | 4981 / 4983 |
| `game_turbo_api_version` / `auto_task_api_version` / `miuiWidgetVersion` / `support_game_turbo_func_flags` | `2` / `1` / `4` / `3` | Negotiate versions with system-side capabilities | 574/580/1452/2528 |
| `app_hide_enable` / `disable_notifications_when_app_hide` | `true` / `false` | Hide app switch | 482/484 |
| `miui.support_full_scan` / `securitycenter.support.mix.scan` | `true` / `true` | Scan mode | 490 / 1448 |
| `miui.supportMultiUserPermission` / `supportCombinePermission` / `miui.supportSmartAutoStart` | `true`×3 | Multi-user/combined permissions/intelligent self-start | 572/576/1241 |
| `security.miui.support.text.detect` | `true` | Sensitive text detection switch (corresponding to `SensitiveWordDetectProvider`) | 4927 |
| `xunyou_voice_remind` | `1` | Xunyou accelerator linkage | 4655 |
| `miui.focusNotificationVersion` / `miui.lessLocationTip` / `miui.dynamic_dpi_relaunch` | `1`/`1`/`true` | System feature negotiation | 4661/4671/4653 |
| `com.miui.maml.sdk` + 20+ miuix component library versions (`miuixpreference`, `appcompat`, `bottomsheet`, `folme`, `haptic`, `pinyin`...) | version number | MIUIX UI library version list | 5004–5125 |

Component-level meta-data are all MAML cards/widgets/accessible configurations (`miuiWidget*`, `android.appwidget.provider`, `android.accessibilityservice`, `android.support.FILE_PROVIDER_PATHS`, `androidx.startup`), **without key**. [Observed]

### 4.5 `<uses-library>` (8, all `required=false`) [Observed]

L582 `vendor.xiaomi.hardware.misecurity-V1.0-java`、L1434 `micloud-sdk`、L1436 `android-support-v7-recyclerview.jar`、L1438 `protobuf.jar`、L1440 `org.apache.http.legacy`、L1442 `com.miui.system`、L5077 `androidx.window.extensions`、L5079 `androidx.window.sidecar`。

### 4.6 `<queries>`（package visibility）[Observed]

decoded.xml **L463–L468**, only 2 items:
```xml
<queries>
    <package android:name="com.xiaomi.migameservice">
    <package android:name="com.xiaomi.mi_connect_service">
</queries>
```
Note: There is no **`android.permission.QUERY_ALL_PACKAGES` in the manifest for**. [Inferred] It relies on `sharedUserId=android.uid.system` to naturally see all packages, and `<queries>` is just a formal declaration for targetSdk 30+.

### 4.7 `<protected-broadcast>`: 28 items (decoded.xml L143–L197) [Observed]

`com.miui.action.NETWORK_POLICY_UPDATE`、`com.miui.action.REFRESH_DATA_USAGE_DAILY`、`com.miui.action.DATA_USAGE_AUTO_CORRECTION`、`action_update_sc_network_allow`、`action_broadcast_tc_diagnostic_status`、`action_broadcast_cancel_notification`、`action_broadcast_tc_sms_report_status`、`sms_receiver_action`、`action_broadcast_allow_app_firewall`、`com.miui.securitycenter.action.UPDATE_NOTIFICATION`、`com.miui.securitycenter.action.CLEAR_MEMORY`、`com.miui.securitycenter.action.TRACK_EVERY_WEEK`、`com.miui.antispam.SMS_AUTO_UPDATE`、`com.miui.action.appcompatibility.update`、`com.miui.securitycenter.action.TRACK_EVERY_DAY`、`miui.intent.action.config_changed`、`config_changed_flag`、`show_float_window`、`remove_float_window`、`shake_send_sticker`、`show_float_window_button`、`action_refresh_data_usage_daily`、`com.miui.luckymoney.ACTION_UPDATE_TIPS_CONFIG`、`com.miui.luckymoney.ACTION_UPDATE_ALARM_CONFIG`、`com.miui.luckymoney.ACTION_LUCKY_ALARM`、`com.miui.cleanmaster.action.UPDATE_SHORTCUT`、`com.xiaomi.security.SUPER_POWER_MODE_EXIT`、`com.miui.securitycenter.extreme.endurance.shutdown`。

The semantics of [Inferred]`protected-broadcast` is "can only be sent by the same UID or system" - it turns these broadcasts **into internal signals** that only the system can forge, which is a rare positive reinforcement in this design.

---

## 5. Third-party/self-developed SDK list (accurate statistics based on dex `class_defs`)

Data source: `_analysis\tools\classes.classlist.json` (25009 `class_defs`), script `pkg_hist.js` / `gen_tables2.js`.
DEX notation: `D1`=`classes.dex`, `D2`=`classes2.dex`, `D3`=`classes3.dex`.

| # | top package | Number of classes | DEX distribution | Belong | The effect of one sentence |
|---|---|---|---|---|---|
| 1 | `com.google.android.exoplayer2` | 2031 | D1:2031 | Third-party open source | ExoPlayer 2 — Media playback (game video/screen recording playback) |
| 2 | `com.google.common` | 1995 | D1:1995 | Third-party open source | Guava（collect/util/base/graph/cache/hash/io/reflect/primitives/math/eventbus/net） |
| 3 | `com.google.android.material` | 884 | D1:884 | Third-party open source | Material Components |
| 4 | `com.google.gson` | 217 | D2:217 | Third-party open source | Gson JSON |
| 5 | `com.google.protobuf` | 246 | D2:246 | Third-party open source | protobuf-java (corresponds to assets/protobuf/*.proto) |
| 6 | `com.google.android.flexbox` | 37 | D1:37 | Third-party open source | FlexboxLayout |
| 7 | `androidx` | 2167 | D1:2167 | Third-party open source | AndroidX（core/appcompat/recyclerview/fragment/lifecycle/preference/constraintlayout/transition/annotation/window/emoji2/room/profileinstaller…） |
| 8 | `miuix` | 1504 | D3:1504 | Xiaomi self-developed framework | MIUIX self-developed UI framework (appcompat/animation/preference/popupwidget/bottomsheet…) |
| 9 | `miui.cloud` | 105 | D3:105 | Third party SDK | Xiaomi Cloud Service SDK (uses-library micloud-sdk) |
| 10 | `com.xiaomi.continuity` | 486 | D3:486 | Third party SDK | Xiaomi Interoperability SDK (libmicontinuity_sdk.so; META-INF version 3.1.561.10.1201105 / 3001561) |
| 11 | `com.xiaomi.onetrack` | 251 | D3:251 | Third party SDK | ONETrack buried points/statistics SDK (pi.b = DexAnalytics, DexClassLoader can load external APK internal com.miui.analytics.Analytics) |
| 12 | `com.xiaomi.push` | 166 | D3:166 | Third party SDK | MiPush push client (libtcp.so; meta MIPUSH_SDK_VERSION_NAME=5_7_8-C / 50708) |
| 13 | `com.xiaomi.mipush` | 82 | D3:82 | Third party SDK | MiPush message/SDK channel |
| 14 | `com.miui.maml` | 626 | D2:626 | Xiaomi self-developed framework | MAML dynamic card/theme rendering framework (assets/maml, com.miui.maml.sdk=true) |
| 15 | `com.miui.hybrid` | 64 | D2:64 | Xiaomi self-developed framework | H5 mixing container |
| 16 | `com.market.sdk` | 73 | D2:73 | Third party SDK | Xiaomi App Store SDK (meta market_sdk_version=3.2.26) |
| 17 | `com.market.pm` | 6 | D2:6 | Third party SDK | Store package management linkage |
| 18 | `com.milink.api` | 67 | D2:67 | Third party SDK | Xiaomi mutual transmission MiLink API interface layer |
| 19 | `com.milink.sdk` | 58 | D2:58 | Third party SDK | MiLink SDK implementation |
| 20 | `com.xiaomi.accountsdk` | 61 | D3:61 | Third party SDK | Xiaomi account SDK |
| 21 | `com.xiaomi.passport` | 22 | D3:22 | Third party SDK | Passport login |
| 22 | `com.xiaomi.analytics` | 18 | D3:18 | Third party SDK | Xiaomi Statistics SDK |
| 23 | `com.miui.analytics` | 20 | D2:20 | Xiaomi SDK | Xiaomi Statistics (DexAnalytics dynamic loading target) |
| 24 | `com.qti` | 139 | D3:139 | Third party SDK | Qualcomm IZAT / GNSS / FLP positioning (debugreport/geofence/wwandb/wifidb…) |
| 25 | `com.xiaomi.gnss` | 43 | D3:43 | Third party SDK | GNSS positioning enhancement |
| 26 | `org.tensorflow` | 14 | D3:14 | Third-party open source | TensorFlow Lite Java bindings (libtensorflowlite_jni.so) |
| 27 | `com.airbnb.lottie` | 52 | D1:52 | Third-party open source | Lottie Animation |
| 28 | `com.davemorrissey.labs` | 20 | D1:20 | Third-party open source | subsampling-scale-image-view Large image browsing |
| 29 | `xcrash` | 28 | D3:28 | Third-party open source | iQiyi xCrash crash capture (libxcrash.so / libxcrash_dumper.so) |
| 30 | `okhttp3` | 2 | D3:2 | Third-party open source (residual) | OkHttp (only 2 remaining classes: EverythingIsNonNull, PublicSuffixDatabase) |
| 31 | `kotlin` | 87 | D3:87 | Third-party open source | Kotlin stdlib（KGP 1.8.22，kotlin-tooling-metadata.json） |
| 32 | `kotlinx.coroutines` | 139 | D3:139 | Third-party open source | coroutine library |
| 33 | `vendor.xiaomi` | 45 | D3:45 | System interface binding | HIDL/AIDL generation interface (uses-library vendor.xiaomi.hardware.misecurity-V1.0-java) |
| 34 | `com.xiaomi.security` | 27 | D3:27 | Xiaomi SDK | Xiaomi Security Public Library (XSOF) |
| 35 | `com.xiaomi.bspsecurity` | 4 | D3:4 | Xiaomi SDK | BSP Security |
| 36 | `com.xiaomi.trustservice` | 4 | D3:4 | Xiaomi SDK | Trusted service |
| 37 | `com.xiaomi.micloudsdk` | 14 | D3:14 | Third party SDK | Xiaomi Cloud SDK |
| 38 | `com.xiaomi.micloudkeybag` | 9 | D3:9 | Third party SDK | Cloud KeyBag |
| 39 | `com.xiaomi.joyose` | 30 | D3:30 | Third party SDK | System Performance Scheduling Joyose |
| 40 | `com.xiaomi.ad` | 9 | D3:9 | Third party SDK | Xiaomi advertising |
| 41 | `com.miui.msa` | 1 | D2:1 | Xiaomi Advertising SDK | MSA advertising logo |
| 42 | `com.miui.systemAdSolution` | 12 | D3:12 | Xiaomi Advertising SDK | System advertising solution (open screen ad provider) |
| 43 | `com.miui.blur` | 29 | D2:29 | Xiaomi self-research library | Gaussian blur library |
| 44 | `com.miui.fastplayer` | 8 | D2:8 | Xiaomi self-research library | Quick player (libfastplayer.so) |
| 45 | `com.miui.mlkit` | 5 | D2:5 | Xiaomi self-developed | ML Kit |
| 46 | `com.miui.sdk` | 22 | D3:22 | Xiaomi self-developed | MIUI Public SDK |
| 47 | `com.miui.support` | 10 | D3:10 | Xiaomi self-developed | MIUI compatible support |
| 48 | `com.xiaomi.ai` | 17 | D3:17 | Third party SDK | Xiaomi AI |
| 49 | `com.xiaomi.aicr` | 11 | D3:11 | Third party SDK | AI capability open interface |
| 50 | `com.xiaomi.aiasst` | 11 | D3:11 | Third party SDK | Xiaoai assistant ability |
| 51 | `com.xiaomi.venus` | 6 | D3:6 | Third party SDK | Venus Performance Capabilities |
| 52 | `com.xiaomi.NetworkBoost` | 6 | D3:6 | Third party SDK | network acceleration |
| 53 | `com.xiaomi.gamecenter` | 8 | D3:8 | third party | game center |
| 54 | `com.xiaomi.migameservice` | 8 | D3:8 | third party | Xiaomi Game Service (<queries> declares visibility) |
| 55 | `com.xiaomi.rcs` | 4 | D3:4 | third party | RCS Converged Communications |
| 56 | `com.xiaomi.dist` | 0 |  | Third party SDK | Distributed hardware (DIST_HARDWARE_SERVICE permission) |
| 57 | `com.xiaomi.vtcamera` | 0 |  | Third party SDK | Video calling camera capabilities |
| 58 | `com.xiaomi.wearable` | 0 |  | third party | Wearable device provider (permission statement) |
| 59 | `com.milink.contants` | 1 | D2:1 | Third party SDK | MiLink constants |
| 60 | `com.milink.service` | 1 | D2:1 | Third party SDK | MiLink Services |
| 61 | `android` | 371 | D1:371 | AOSP shadow class | The framework class is typed into DEX (class 371, see §6.4) |
| 62 | `com.android.bluetooth` | 8 | D1:8 | AOSP Quote | Bluetooth configuration reference |
| 63 | `com.android.provision` | 8 | D1:8 | AOSP Quote | Boot configuration reference |
| 64 | `com.android.systemui` | 6 | D1:6 | AOSP Quote | SystemUI notification interface reference |
| 65 | `com.android.providers` | 4 | D1:4 | AOSP Quote | System provider reference |
| 66 | `com.android.internal` | 1 | D1:1 | AOSP Quote | Internal API reference |
| 67 | `com.qualcomm` | 0 |  | third party citations | Qualcomm permissions related references |
| 68 | `com.mobile.businesshall` | 1 | D3:1 | Three-party jump | Operator business hall jump |

**Key negative conclusion**[Observed]:
- **None** `retrofit2`, `dagger`/`hilt`, `glide`, `rxjava`/`io.reactivex`, `com.squareup.*`, `firebase`, `crashlytics`, `umeng`, `bugly`, `adjust`(class), `appsFlyer`, `unity3d`, `flutter`, `react-native` class definition.
- `okhttp3` has only 2 classes left (`okhttp3.internal.annotations.EverythingIsNonNull`, `okhttp3.internal.publicsuffix.PublicSuffixDatabase`) + resource `okhttp3/internal/publicsuffix/publicsuffixes.gz`; **does not have any okio class**. [Unknown] If it does rely on OkHttp, its main body is in 637 obfuscated short packets and has not been located in this round.
- The `com.facebook.*` class is 0; but `assets\placement_id` records `123=fb:196567740764356_...` (Facebook ad slot ID) - the **ad slot configuration exists but the SDK code is not in** ([Inferred] ad rendering takes the system-level MSA/`com.miui.systemAdSolution` channel).

---

## 6. Code structure and obfuscation

### 6.1 Scale[Observed]

| indicator | value |
|---|---|
| DEX number | 3 (`classes.dex` 8,142,528 B / `classes2.dex` 8,835,532 B / `classes3.dex` 7,543,548 B, total 24.5 MB) |
| `class_defs` Total | **25,009** |
| Top level `.java` produced by jadx | **10,624** (≈ number of top-level classes; the difference is internal classes/anonymous classes) |
| Number of top-level packages | **649** |
| Named roots (12) | `com` 14469, `androidx` 2167, `miuix` 1504, `android` 371, `miui` 171, `kotlinx` 139, `kotlin` 87, `org` 45. `vendor` 45. `xcrash` 28. `okhttp3` 2. `java` 1 → **Total 19,029 categories (76.1%)** |
| obfuscated short packet (1–3 letters + numbers) | **637 roots, 5,980 categories (23.9%)** |
| Confusion short package Top | `aj` 352, `a8` 129, `mk` 124, `a4` 99, `x4` 98, `v3` 84, `c3` 52, `zm` 52, `r3` 51. `ci` 51. `t5` 49. `s` 46. `de` 46. `ok` 45. `z2` 44 … Among them, there are <5 types of roots **407** |

### 6.2 Conclusion: This is **R8 partially renamed**, not a reinforced shell

| criterion | Observed | Judgment |
|---|---|---|
| Whether to reinforce the shell | `lib/` Download 18 .so (×2 ABI) **are all business/algorithm libraries**: `libc++_shared`, `libfastplayer`, `libjni_load_serinum`, `libjni_sound_effect(_2_0)`, `liblib-riskcontrol`, `libmicontinuity_sdk` , `libmiffmpeg`, `libMNN`/`libmnncore`/`libMNNTrain`/`libMNN_Express`, `libnap`, `libsec-common-utils`, `libtcp`, `libtensorflowlite_jni`, `libxcrash(_dumper)`. **No** `libjiagu`, `libDexHelper`, `libshell`, `libmobisec`, `libexec.so`, `libprotectClass` | **is not a shell**[Observed] |
| Is dex readable? | The three dexes are all plain text, and jadx is fully decompiled successfully (`DONE`, errors=53 / warns=354, only the upper limit of decompilation is `JadxOverflowException`) | **No encryption/None VMP**[Observed] |
| Is `Application` replaced with a shell? | `<application android:name="com.miui.securitycenter.Application">`, directly business type; no `StubApp`/`SecShellApplication` etc. | **is not a shell**[Observed] |
| dynamic loading | There is only 1 `DexClassLoader` in all APK: `pi/b.java` (`pi.b`, log tag `DexAnalytics`) | See §6.3 |
| degree of confusion | 637 roots were renamed to 1–3 letters; but `com.miui.networkassistant`/`gamebooster`/`permcenter`/`powercenter` **completely retains the original name** | **partially confuses**: the package name of its own business module `-keep` is changed, and the third-party/internal library is renamed by R8 [observation + inference] |

> In other words: **is not protected by the shell**, and normal static analysis can get all the business logic. Obfuscation only makes it difficult for 23.9% of classes (mainly repackaged libraries) to be attributed by package name.

### 6.3 The only dynamic loading point[Observed]

`_analysis\jadxout\sources\pi\b.java`（`classes3.dex`）：
```java
this.f33226b = new DexClassLoader(apkPath, context.getDir("dex",0).getAbsolutePath(), optimizedDir, ClassLoader.getSystemClassLoader());
this.f33226b.loadClass("com.miui.analytics.Analytics").getDeclaredMethod("trackEvent", String.class).invoke(null, str);
```
Constructor: `ni/c.java: return new pi.b(this.f32067a, D(), E());`

**meaning**: This is the "DexAnalytics" variant of `com.xiaomi.onetrack` (ONETrack) - when the statistics implementation exists as **standalone APK**, `com.miui.analytics.Analytics` is loaded from the external APK path and called reflectively (version taken from `getPackageArchiveInfo(apkPath, 1)`). [Observed code + inferred use] **is not the loader of the** shell; but it is a real "reflective loading code from external APK" channel, and the path to the loading target is provided by `D()` of `ni.c` ([Unknown] The source of this path is not traced).

### 6.4 DEX subcontracting strategy[Observed]

| DEX | Number of classes | Main content |
|---|---|---|
| `classes.dex` | 8,189 | **pure third-party UI/media base**: `com.google.android.exoplayer2` 2031, `com.google.common` (Guava) 1995, `com.google.android.material` 884, `androidx` 2167, `com.airbnb.lottie` 52, `com.davemorrissey.labs` 20. **`android.*` shadow class 371**, 46 confusing short packets |
| `classes2.dex` | 8,924 | **business entity**: `com` 6292 (`com.miui.maml` 626, `com.miui.networkassistant` 1071, `com.miui.gamebooster` 936, `com.miui.permcenter` 500, `com.miui.powercenter` 496, gson 217, protobuf 246, `com.milink.*` 125, `com.market.sdk` 73, `com.miui.hybrid` 64...) + **285 confused short packets** |
| `classes3.dex` | 7,896 | **frame/SDK layer**: `miuix` 1504, `com` 3131 (`com.xiaomi.continuity` 486, `com.xiaomi.onetrack` 251, `com.xiaomi.push` 166, `com.miui.warningcenter` 249, `com.qti` 139, `com.xiaomi.mipush` 82...), `miui.cloud` 171, `kotlinx.coroutines` 139, `kotlin` 87, `vendor.xiaomi` 45, `org.*` 45, `xcrash` 28. `ok` 45. **290 confusing short packets** |

[Inferred] This is not subcontracting by "function", but the default 64K method number segmentation + class number sequence **of**R8/D8: dex1 is full of Google heavy dependencies, dex2 is the business, and dex3 is the framework/SDK. There is no intentional feature-split in the project.

### 6.5 What exactly is `tmsdkobf` (to clarify the misleading in the baseline)

- **`tmsdkobf` / `Ltmsdk/*` are not included in this APK. **[Observed] The hit number of `Ltmsdk/` in the three dex string tables of APK =**0**, and the class definition =**0**.
- Hit source: `_analysis\dex\classes.dex` (dex**of**GuardProvider, 685 strings containing `tmsdk` and 108 strings containing `miui/guardprovider` among 22623 strings), and its string export `_analysis\dex_strings_all.txt` (515 KB, 702 lines of hits). The three dex of this APK are actually measured `tmsdk` = **0/0/0**. [Observation, command `_analysis\tools\scan_tmsdk.js`]
- It refers to **, Tencent Mobile Secure (TMS, Tencent Mobile Secure) SDK**, the package name is `tmsdk.*`, and the obfuscated variant is often named `tmsdkobf`; the content contains `tmsdk/common/tcc/*` (TCC Engine: `DeepCleanEngine`, `QSdcardScanner`, `VideoMetaRunner`, `TrafficSmsParser`), `tmsdk/common/module/qscanner/*` (quick scan), `tmsdk/bg/creator/*`, `tmsdk/fg/creator/*` (front and back manager creator), `tmsdk/common/roach/nest/PowerNest` (keep alive), `tmsdk/common/NumMarker` (number tag). **These are the AV/scrubber engine dependencies of GuardProvider.**
- The relationship between this APK and it: `com.miui.securitycenter` is just that **delegates scanning to the external `com.miui.guardprovider`** through `content://guard` (`com.miui.guardprovider.permission.Guard_Provider`) (`uses-permission com.miui.guardprovider.permission.Guard_Provider` L325 in manifest; `com/miui/guardprovider` in dex) Cited at 24+6). **Tencent TMS/AVL code is not included in this APK.**[Observed]

### 6.6 `android.*` shadow class (packaging exception, worth remembering)

[Observed]**371 `android.*` class **(`android.app.Notification$MessagingStyle`, `android.app.TaskStackListener`, `android.content.LocusId`, `android.graphics.BlendMode`, `android.app.usage.StorageStats`...) are defined in `classes.dex`, among which**359 Each band is not empty `class_data`** (that is, it contains methods/fields, not empty piles).
[Inferred] When building, a certain `framework.jar` / `miui-framework` is regarded as a normal dependency of **(implementation rather than compileOnly)** is entered into dex. Classes with the same name will be parsed first by bootclasspath during runtime, which usually has no behavioral impact, but is a packaging hygiene issue + volume waste.
[Unknown] Whether the specific contents of these classes are copies of AOSP as they are, they are not compared one by one.

---

## 7. Multi-process

`android:process` only uses 3 custom values + default main process [Observed]:

| process | full process name | Number of components | bear |
|---|---|---|---|
| main process | `com.miui.securitycenter` | **315** | All 293 Activities (except 8), `com.miui.sec.external.InstallProvider`, `SecSettingsSearchProvider`, `PackageBehaviorContentProvider`, `PrivacyAppsProvider`, `SensitiveWordDetectProvider`, `ZmanProvider`/`ZmanFileProvider`, `androidx.startup.InitializationProvider`, `ProfileInstallReceiver`, etc. |
| **`com.miui.securitycenter.remote`** | Same name | **100** | **carries almost all Services and most Provider**: `XMPushService`/`XMJobService`, `GuardService`, `AntiSpamService` , `TrafficManageService`, `FirewallService`, `MiuiVpnManageService`, `GameBoosterService`, `VideoToolBoxService`, `NotificationListener`, `BubblesNotificationListenerService`, `SuperPowerTileService`, `PerformanceMode(Wild)TileService`, `SandboxManagerService`, `InvisibleModeService`, `DockMonitorService`, `BeautyService`, `FrameRateDataService`, `FreeformWindowService`, `RemoteService`, `NotificationService`, `MiSafetyDetectService`, and `SecurityCenterProvider`/`RemoteProvider`/`DeviceCredentialProvider`/`IdProvider`/`AntiSpamProvider`(`antispam`)/`WarningCenterProvider`/`XSOFProvider`… All 17 `*ContentProvider` |
| **`com.miui.securitycenter.bootaware`** | Same name | **3** | `directBootAware` group: `com.miui.simlock.SimLockMonitorService`, `com.miui.simlock.BootCompleteReceiver` (`LOCKED_BOOT_COMPLETED`), `com.miui.simlock.activity.SuccessDialogActivity` - **can run before** user unlocks (direct boot), used for SIM card lock |
| **`:ui`** | `com.miui.securitycenter:ui` | **6** | The UI side of bubble notification and game acceleration: `BubblesService`, `BubbleRemoteService`, `BubbleSettingsActivity`, `BubbleAppManageActivity`, `DockWindowManagerService`, `GameBoosterTelecomManager`, `AntiMsgAccessibilityService` (running without obstacles on `:ui`, indicating that its UI injection scope is deliberately isolated) |

[Inferred] The `remote` process assumes all the responsibilities of "resident background + cross-process provider" and is the main bearer of **memory/permission isolation**; the main process only places the foreground UI and a few providers that must be the main process.

---

## 8. Risk list (exposure perspective, sorted by severity)

> Prerequisite: All listed components run as **system UID** (§1.1). Therefore, "exported components without permission protection" are not "ordinary information leakage", but "**Any local App can use the system identity to perform a system-level action**".

| # | Severity | components | facts | why dangerous |
|---|---|---|---|---|
| 1 | **high** | `provider com.miui.sec.external.InstallProvider`（authority `com.miui.sec.INSTALL_PACKAGE`，L3806） | exported=true, **without any permission / readPermission / writePermission**, without `grantUriPermissions` | The name and authority directly point to the "installation channel"; the provider directly returns the cursor/calls the internal installation logic. Any App can be query/insert. If it can drive the installation process, it is equivalent to "packaging as system". [Unknown] The specific verification of `query/insert/call` requires reading the source code of `com.miui.sec.external.InstallProvider` for confirmation (not read this round) |
| 2 | **high** | `provider com.miui.securitycenter.provider.DeviceCredentialProvider`（`com.miui.securitycenter.credentialprovider`，L2480） | exported=true, **has no permission** | Device credentials (PIN/password verification) are usually high-value oracles; exporting without permission means that they can be called by any App as the "password verification/credential query" interface |
| 3 | **high** | `provider com.miui.securitycenter.provider.SecSettingsSearchProvider`（L3813） | exported=true, **without permission**, **grantUriPermissions=true**, with `<intent-filter action="miui.intent.action.SETTINGS_SEARCH_PROVIDER">` | The only provider that is "exported + has no permissions + can grant URI"; it also exposes the search interface and temporary URI authorization capabilities (potentially granting internal files URI to third parties) |
| 4 | **high** | `service com.miui.networkassistant.vpn.miui.MiuiVpnManageService`（action `com.miui.networkassistant.vpn.MIUI_VPN_MANAGE_SERVICE`，L2974） | exported=true, **has no permission** | VPN management service has no permission to export = Any App can request to create/configure VPN (as system), which is a traffic hijacking surface |
| 5 | **medium high** | `activity com.miui.permcenter.permissions.SystemAppPermissionDialogActivity`（L3850） | exported=true, **does not have permission to**, receives `miui.intent.action.SYSTEM_PERMISSION_DECLARE` / `_NEW` / `CTA_DECLARE` / `_NO_OPTIONAL`, and intent-filter is bound to `content:`, `file:`, `image/*` | The **system permission statement/privacy agreement pop-up window** can be popped up by any App → Typical "fake system authorization prompt" phishing surface (users will regard the system UID pop-up window as a system prompt) |
| 6 | **medium high** | `provider com.miui.idprovider.IdProvider`（`com.miui.idprovider`，L2507） | exported=true, **has no permission** | OAID/device identification reading interface; no permission to export = any app can directly get OAID and possible associated identifications |
| 7 | **medium high** | `activity com.miui.networkassistant.ui.base.UniversalFragmentActivity`（L2759） | exported=true, **does not have permission to**, and receives multiple actions of `com.miui.datatraffic:` scheme + `VIEW` + `NETWORKASSISTANT_*` | The name is "UniversalFragment", usually according to `:android:show_fragment` in extra to instantiate fragment → **Fragment injection surface** (any registered Fragment can be instantiated in the system process) |
| 8 | **medium high** | `activity com.miui.permcenter.install.AdbInstallActivity`（L2091） | exported=true, **does not have permission to**, receives `android.intent.action.INSTALL_PACKAGE` + `file:` + `application/vnd.android.package-archive` | Give an intent of `file://`+APK MIME to the installation UI of the system identity; cooperate with `AdbInputApplyActivity`/`AdbInstallVerifyActivity` to form the "ADB installation" link, which is a common starting point for the privilege escalation chain |
| 9 | **in** | `provider com.xiaomi.security.xsof.XSOFProvider`（`com.xiaomi.security.xsof.provider`，L4989）+ `service com.xiaomi.security.xsof.MiSafetyDetectService`（L4994） | Both exported=true, **has no authority** | XSOF is the "Xiaomi Security Capability Open Framework"; the service action `com.xiaomi.security.xsof.SAFETY_DETECT_SERVICE` can be pulled up by any App for security detection (may return the device risk level) |
| 10 | **in** | `provider com.miui.warningcenter.provider.WarningCenterProvider`（L4147）、`AutoTaskServiceProvider`（L1225）、`PrivacyCenterProvider`（L1358）、`InputProvider`（L4076）、`SystemAdContentProvider`（L4491） | All exported=true, **has no permission** | The complete data surface of early warnings/automatic tasks/privacy center/input method configuration/open-screen advertisements is exposed to any App |
| 11 | **in** | 2 normal level permissions (`miui.permission.TVM` L79, `...PERM_USE_INFO` L113) | protectionLevel missing → normal | The actual protection of `AppPermissionsUseActivity` (near L3858) claimed to be protected by authority has failed; `TvmManagerService` is a potential gap because `enabled=false` is currently unreachable at the same time. |
| 12 | **in** | `android:allowBackup` Default (=true) + Custom `backupAgent` | Observed | System UID application data (interception rules/privacy configuration) can go through the backup channel |
| 13 | **low-medium** | 106 exported and unauthorized activities (see §4.2-A.5 for complete details) | Observed | A large number of `miui.intent.action.*` internal entrances are visible to the outside world; viewed individually, most of them are UI jumps, but they are all running in the system process. |
| 14 | **low** | 8 exported Receivers without permission: `BootCompleteReceiver`(LOCKED_BOOT_COMPLETED), `DateResetReceiver`(TIME_SET/TIMEZONE_CHANGED/LOCALE_CHANGED), `DeviceOwnerChangedReceiver`, `RootUpdateReceiver`, `ProvisionCompleteReceiver`, `PolarisGeofenceReceiver`, `AppPredictWidget2x4`, `BehaviorWidget` | Observed | Both are "system broadcast triggers", and third parties cannot directly forge some actions; however, `DateResetReceiver` receives `TIME_SET`, and it is worth noting the permission of `MANAGE_BOOT_TIME` |

**forward reinforcement**[Observed]: `android:exported` all explicitly declared (no default export); 28 `protected-broadcast`; provider authority has no duplication; 18 of 40 providers have `signatureOrSystem` authority.

---

## 9. Unsolved mysteries (leaved to follow-up/requires dynamic verification)

1. The actual check of **`com.miui.sec.external.InstallProvider` is**: the manifest layer is completely naked, but the code layer may have `Binder.getCallingUid()` checks/`enforceCallingPermission`. You need to read `com.miui.sec.external.InstallProvider` (`classes*.dex`, the specific file has not been located by jadx) to confirm whether there is a UID whitelist. [Unknown]
2. `query/insert/call` semantics and caller verification of **`DeviceCredentialProvider` / `IdProvider` / `XSOFProvider` / `SecSettingsSearchProvider`**: Same as above, the manifest can only give "exposure", not "whether it can really be exploited". [Unknown]
3. Which paths `grantUriPermissions=true` of **`SecSettingsSearchProvider` can actually grant to**: depends on its internal `openFile`/`getType` implementation, unread. [Unknown]
4. **`network_security_config` content** (`@0x7f15003c`): There is no `res/xml/` of apktool in this directory (`apktool_sc` lacks manifest and xml, `apktool` belongs to GuardProvider), you need to solve `resources.arsc` to get it `res/xml/network_security_config.xml`, and then determine whether `cleartextTrafficPermitted` is configured with pinning. [Unknown]
5. Where does the external APK path of **`pi.b` (DexAnalytics) come from**: `ni.c.D()` is not read; it is necessary to confirm whether it is "pointing to `com.miui.analytics` independent APK" or a path that can be replaced (if the latter is any code loading surface). [Unknown]
6. **23.9% The attribution of obfuscated classes (5980 / 637 roots)**: This round is only counted by package name, without reference analysis. Which roots belong to okhttp/okio/other repackaged libraries require `trace_android_references` or string/resource reference reverse positioning. [Unknown]
7. **`android.*` 371 shadow classes **: It is necessary to confirm whether it is an original copy of AOSP and whether there is any tampered framework behavior (if it is a selective copy, the risk level will increase significantly). [Unknown]
8. **signature authenticity**: REA only performs self-consistent verification of "declaration summary == current byte", **does not perform cryptographic verification of signature**. To assert "not repackaged", `apksigner verify --print-certs` needs to be independent and compared with the MIUI platform certificate fingerprint. [Unknown]
9. Intranet DB configuration **in**`application.properties`:
   ```
   #spring.datasource.url=jdbc:mysql://[ADDRESS_REMOVED]:3306/miui_sec_api?useUnicode=true&characterEncoding=utf8
   #spring.datasource.username=root
   #spring.datasource.password=sec
   ```
   It has been commented, but **is shipped with APK and**. It is a build hygiene/information leakage issue (internal network IP + weak password pattern), not an exploitable vulnerability; whether there are other build legacy files with valid credentials has not been fully investigated. [Observation content + inferred properties]
10. **`<queries>` only declares 2 packages.** and `QUERY_ALL_PACKAGES` are missing and coexisting: they depend on the implicit visibility of system UID. If APK is downgraded to a non-system installation, it will immediately lose the package visibility - that is, the hard constraint of "pre-installed dependencies". [Inferred]

---

## 10. One page quick overview

- Identity: `com.miui.securitycenter` 10.4.9-250402.0.1 (40001049), **`sharedUserId=android.uid.system`**, minSdk 23/targetSdk 34.
- Signature: v1+v2+v3 (no v3.1/no rotation/no source stamp), certificate `O=Xiaomi, OU=MIUI, CN=MIUI`, SHA-256 `c9009d01…9025`, 2011→2039 self-signed RSA —— **MIUI Platform key**. v1 covers 9228/9231 entries.
- Components: **424** (293 act + 2 alias + 67 svc + 22 rcv + 40 prov); **exported 194** (5 of them `enabled=false`, effective 188), **128 zero permission protection** (127 currently valid).
- Permissions: Custom **50** (43×signatureOrSystem, 3×signature, 2×normal, **2×missing → normal**); `uses-permission` **147** (AOSP normal 30/danger 17 / AOSP signature·privilege 28 / manufacturer’s private 72).
- SDK: ExoPlayer2 2031, Guava 1995, Material 884, AndroidX 2167, MIUIX 1504, MAML 626, Continuity 486, ONETrack 251, MiPush 248, protobuf 246, Gson 217, Qualcomm 139, xCrash 28, TFLite 14…; **does not have retrofit/dagger/glide/rxjava/firebase/umeng**; `tmsdk*` **is not in this package**.
- Structure: 25009 `class_defs` / 649 top-level roots / **637 obfuscated short containing 5980 categories (23.9%)**; 3 dex plaintext, jadx full success; **without shell and no reinforcement**; the only dynamic loading point `pi.b`(DexAnalytics).
- Process: main process(315) + `:remote`(100) + `:bootaware`(3) + `:ui`(6).
