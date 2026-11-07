> **Historical analysis.** This archive includes observations that remain unverified or were later corrected. See the [final report](../REPORT.md) and [independent review](../REVIEW.md) for the reviewed findings on upload scope, consent, local FBO delegation, emergency SMS encryption, and provider authorization. Source references retain their original decompilation paths. The publication includes the selected files under evidence/.

# MIUI Security Center—Permission Center/Privacy Business Module Reverse Report

- Target: `MIUISecurityCenter-10.4.9-250402.0.1.apk` (`com.miui.securitycenter`, versionCode 40001049)
- SHA-256: `fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd` (see `_BASELINE.md`)
- Analysis method: **pure static**. Full amount of jadx 10624 `.java` + apktool decoded `AndroidManifest.xml` / `res/values/strings.xml`.
- Annotation convention: **observed** = code/manifest fact that can be directly quoted in the shipped artifact; **inferred** = inferred from the code structure but no runtime evidence; **unknown** = statically undeterminable.
- Evidence format: `relative path:line number` (the path is relative to `_analysis\jadxout\sources\` or `_analysis\apktool_sc\`).

---

## 0. Summary: Ten key conclusions

1. **`miui.security.IPermissionManager`** does not exist in this APK (full tree grep `IPermissionManager` zero hits - **observes**). There are three real boundaries between this App and the permissions database:
   - `ContentResolver.call("content://com.lbe.security.miui.permmgr", "<methodCode>", ...)` - provided by `com.lbe.security.miui`**(LBE permission manager) pre-installed on** (`com/miui/permission/PermissionContract.java:10,15`; `com/miui/permission/PermissionManager.java:302,437`);
   - `miui.security.SecurityManager` (`"security"` system service of system_server, framework class, non-original APK code) - **, the only true source of** in application lock, privacy application, privacy input method, cross-terminal and other states;
   - `AppOpsManager` hides API + MIUI private op (`10000–10053`) - permission "validation" layer.
2. Writing permission **is double writing**: first write MIUI's own permmgr (`setMode` method=9 / `setApplicationPermission` method=6), and then use reflection to adjust AOSP `IPackageManager.grantRuntimePermission` / `revokeRuntimePermission` / `updatePermissionFlags` Keep the frame consistent (`g4/b.java:320-417`). **observes**.
3. All switch preference settings do not go to the App private SP, but go to `callPreference`**of**`content://com.miui.securitycenter.remoteprovider`, and the entities are `SharedPreferences("remote_provider_preferences")` (`r4/a.java`, `com/miui/common/persistence/RemoteProvider.java`). **observes**.
4. Privacy consent (CTA) is an independent **`ue` package SDK**, POST to `data.sec.miui.com/privacy/{agree,revoke,get,latestVersion}`, with a self-signed `sign` header (hard-coded key `2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9`, MD5+Base64). The international version directly short-circuits and returns to `-4`. **observes**.
5. **privacy thumbnail blur is not `com.miui.blur` implemented in**. `com.miui.blur` is the backdrop background blur SDK (RenderScript + `MiuiCompositionSamplingListener` + SurfaceControl); the real switch for privacy blur is `Settings.System."miui_recents_privacy_thumbnail_blur"` (comma separated list of package names), which is read and rendered by `com.miui.home` (desktop). **observes**.
6. Privacy risk detection (`PrivacyRiskDetectionActivity`) has a total of **8 detection items** (id 11/12/13/14/15/16/22 + fixed 3 recommendation cards), and the basis for determination is "Whether the **permission is "always allowed" (action==3) application count**" + DeviceAdmin/Notification Monitoring/UsageStats Count + Cloud Risk Application List **observes**.
7. There are exported components of **that have been declared but the code of** is missing: `com.miui.permcenter.untrustwarn.UnknownSourceWarnActivity`, `com.miui.permcenter.privacymanager.XSpaceWarningDialogActivity` - the list has it, DEX has no corresponding class. **observes (missing category)**, whether it is still pulled up by the system → **unknown**.
8. The password/pattern of the application lock **is never stored in this App**: the verification is `SecurityManager.checkAccessControlPassword(type, pwd)`, the setting is `setAccessControlPassword(pwd, type)`, and all is system_server. This APK is only responsible for the UI and "release after unlocking" (`addAccessControlPass`). **observes**.
9. Cross-end permission (`acrossterminal`) writes data in **`/data/misc/user/<userId>/securitycenter/across_terminal.xml`** (XML v1, grouped by Xiaomi account). **observes**.
10. Upload surface (privacy related): `flash.sec.miui.com/user/{cat,ra}` will report the list of visible application package names on the **desktop (ra also has versionCode)**+ device model/MIUI version, `biz_id="rivacy_risk_check"` (the first letter r is missing in the spelling). AES/ECB/PKCS5 + GZIP + Base64, key=`SHA1(ConfigManager.getSecurityHttpKey())`. **observes**.

---
## 1. Module map

| package | Number of classes | Responsibilities |
|---|---|---|
| `com.miui.permcenter` | 162 | Permission center main UI: permission-by-permission/application-by-application authorization, auto-start, privacy detection, thumbnail blur, privacy input method, shutdown password, cross-terminal permissions, behavior records, installation verification, capsule |
| `com.miui.permission` | 16 | **permissions SDK**: `PermissionManager` (115 `PERM_ID_*`), `PermissionContract` (method code), `StoragePolicyContract`, `RequiredPermissionsUtil` |
| `com.miui.privacyapps` | 10 | Privacy application (hidden application space) UI + `PrivacyAppsProvider` (`app.hide`) |
| `com.miui.applicationlock` | 34 | App Lock: Unlock UI (pattern/number/mixed password/fingerprint/face), block notification content, retrieve password |
| `com.miui.blur` | 18 | backdrop background blur SDK (non-privacy blur) |
| `com.miui.appmanager` | 22 | Application management: batch uninstall, clear cache/data, notification switch, usage statistics |
| `com.miui.devicepermission` | 7 | Cross-device (screen/camera/microphone sharing) permission editing and authorization pop-up window |
| `com.miui.combinepermission` | 4 | Combined permission one-time authorization pop-up window (CTA) |
| `com.miui.idprovider` | 11 | OAID/Device Identification Reading and Authorization (`IdProvider` + Authorization Activity) |
| `com.miui.appcompatibility` | 1 | Application compatibility exception prompt (only 1 Activity) |

The components related to the above package in the manifest have a total of **107 string hits on** (count of `AndroidManifest.xml` grep `com\.miui\.(permcenter|permission|privacyapps|applicationlock|blur|appmanager|devicepermission|combinepermission|idprovider|appcompatibility)`; about 120 lines of entities parsed by `<activity|service|provider|receiver|activity-alias>`, including duplicate intent-filter expansion). `com.miui.blur` **does not have any manifest component** (pure code base).

---

## 2. List of exported components and pullers (key points)

> Source: `_analysis\apktool_sc\AndroidManifest.xml` (parsed and exported using XML, `exported` / `permission` are all taken from the original properties of the manifest).

### 2.1 High-value export entrance

| components | exported | Protect permissions | intent action (puller) |
|---|---|---|---|
| `permcenter.permissions.SystemAppPermissionDialogActivity` | true | None | `miui.intent.action.SYSTEM_PERMISSION_DECLARE` / `_NEW` / `CTA_DECLARE` / `_NO_OPTIONAL` — **When a third-party app requests permission for the first time, the system/permission manager pulls up** |
| `permcenter.permissions.PermissionsEditorActivity` | true | None | `miui.intent.action.APP_PERM_EDITOR`, `miui.intent.action.APP_PERM_EDITOR_PRIVATE` — Settings/Desktop long press "Application Information → Permissions" |
| `permcenter.permissions.PermissionAppsEditorActivity` | true | None | `miui.intent.action.PERMISSION_APP_EDITOR_ACTIVITY` — View application list by permissions |
| `permcenter.permissions.AppPermissionsUseActivity` | true | `com.miui.securitycenter.permission.PERM_USE_INFO` | `miui.intent.action.APP_PERMISSION_USE` — Permission usage prompt (click on status bar) |
| `permcenter.permissions.AppPermissionsTabActivity` | true | None | No filter (explicit Intent), parameter `select_navi_item` |
| `permcenter.autostart.AutoStartManagementActivity` | true | None | `miui.intent.action.OP_AUTO_START` — Settings→Application→Auto-start |
| `permcenter.detection.PrivacyRiskDetectionActivity` | true | None | `miui.intent.action.PRIVACY_RISK_DETECTION_ACTIVITY` — Security Center/Setup Privacy Check |
| `permcenter.privacyblur.PrivacyThumbnailBlurSettings` | true | None | `com.miui.securitycenter.action.PRIVACY_THUMBNAIL_BLUR_SETTING` |
| `permcenter.settings.OtherPermissionsActivity` | true | `...permission.SYSTEM_PERMISSION_DECLARE` | No filter ("Other Permissions" aggregate page) |
| `permcenter.settings.PrivacyInputModeActivity` | true | None | `miui.intent.action.PRIVACY_INPUT_MODE_ACTIVITY` |
| `permcenter.settings.ShutDownPasswordActivity` | true | `...SYSTEM_PERMISSION_DECLARE` | `miui.intent.action.SHUT_DOWN_PASSWORD_ACTIVITY` |
| `permcenter.settings.InvisibleModeActivity` | true | None | `com.miui.securitycenter.action.INVISIBLE_SETTING` |
| `permcenter.privacycenter.PrivacySafetyActivity` | true | None | `miui.intent.action.PRIVACY_SETTINGS` / `SECURITY_CENTER_SETTINGS` / `PRIVACY_SAFETY` |
| `permcenter.privacycenter.usage.AppPermissionUsageActivity` | true | None | `miui.intent.action.APP_PRIVACY_DETAIL` (+ alias `privacymanager.behaviorrecord.PrivacyDetailActivity`) |
| `permcenter.privacycenter.usage.PermissionUsageRecordActivity` | true | None | `miui.intent.action.APP_BEHAVIRO_RECORD` |
| `privacyapps.ui.PrivacyAppsManageActivity` | true | None | `miui.intent.action.MANAGE_PRIVACY_APPS` |
| `privacyapps.ui.PrivacyAppsHelper` | true | `miui.permission.USE_INTERNAL_GENERAL_API` | `miui.intent.action.PRIVACY_APPS_HELPER` (externalOnly’s entry springboard) |
| `applicationlock.FirstUseAppLockActivity` | true | None | `com.miui.securitycenter.action.LOCK_APP` |
| `applicationlock.ConfirmAccessControl` | true | None | `miui.intent.action.CHECK_ACCESS_CONTROL` — **system pulls up** when the protected App is in the foreground, carrying `android.app.extra.PROTECTED_APP_TOKEN` (IBinder) |
| `applicationlock.LockChooseAccessControl` | true | None | `com.miui.securitycenter.action.LOCKCHOOSEACCESSCONTROL` |
| `applicationlock.TransitionHelper` | true | None | `com.miui.securitycenter.action.TRANSITION` |
| `idprovider.ui.GrantPermissionActivity` | true | None | `miui.intent.action.REQUEST_PERMISSIONS_FOR_OAID` — App reads OAID and pulls it up when intercepted |
| `idprovider.ui.OAIDSettingsActivity` / `OAIDAppSettingsActivity` / `legacy.OAIDAppsActivity` | true | None | `miui.intent.action.OAID_APPS` |
| `combinepermission.grantpermission.CombinePermissionActivity` | true | `...SYSTEM_PERMISSION_DECLARE` | `miui.intent.action.REQUEST_PERMISSIONS_COMBINE`、`miui.intent.action.CTA_REQUEST_PERMISSIONS_COMBINE` |
| `devicepermission.grantpermission.GrantDevicePermissionActivity` | true | `...SYSTEM_PERMISSION_DECLARE` | `miui.intent.action.REQUEST_DEVICE_PERMISSIONS` |
| `devicepermission.editpermission.DevicePermissionEditorActivity` / `DevicePermissionSettingsActivity` | true | `...SYSTEM_PERMISSION_DECLARE` | `miui.intent.action.EDITOR_DEVICE_PERMISSIONS` / `DEVICE_PERMISSION_SETTINGS` |
| `appmanager.AppManagerMainActivity` | true | None | `miui.intent.action.APP_MANAGER` |
| `appmanager.ApplicationsDetailsActivity` | true | None | `miui.intent.action.APP_MANAGER_APPLICATION_DETAIL` |
| `permcenter.install.AdbInstallActivity` | true | None | `com.miui.securitycenter.intent.action.INSTALL_PACKAGE` (ADB/package installer installation confirmation) |
| `permcenter.MainAcitivty` (original spelling) | true | None | `miui.intent.action.LICENSE_MANAGER` (open source license) |
| `permcenter.AccessibilityServiceActivity` | true | `miui.permission.ACCESSIBILITY_SERVICE` | `miui.intent.action.ACCESSIBILITY_SERVICE` |
| `permcenter.NotificationVerifyActivity` | true | `miui.permission.NOTIFICATION_VERIFY` | `miui.intent.action.NOTIFICATION_VERIFY` |
| `permcenter.WLANVerificationActivity` | true | None | `miui.intent.action.OPEN_WIFI_VERIFY` |
| `permcenter.ctaverify.SendSmsVerificationActivity` | true | `android.permission.BROADCAST_SMS` | `miui.intent.action.SEND_SMS_VERIFY` |
| `permcenter.DeviceOwnerChangedReceiver` | true | None | `android.app.action.DEVICE_OWNER_CHANGED` |
| `permcenter.install.PackageVerificationReceiver` | true | `android.permission.INSTALL_PACKAGES` | `com.miui.action.PACKAGE_NEEDS_VERIFICATION` — **package installer pre-installation verification callback** |
| `permcenter.root.RootUpdateReceiver` | true | None | `com.android.updater.action.ACQUIRED_ROOT_SUCCESSED` |
| `permcenter.privacymanager.widget.BehaviorWidget` | true | None | AppWidget (behavior recording widget) |

### 2.2 Export Provider (external data plane)

| Provider | authority | Protect permissions | Notes |
|---|---|---|---|
| `privacyapps.PrivacyAppsProvider` | `com.miui.securitycenter.app.hide` | `...SYSTEM_PERMISSION_DECLARE` | Only implements `call("isPrivacyApp")` → `SecurityManager.isPrivacyApp(pkg,userId)` (`com/miui/privacyapps/PrivacyAppsProvider.java:39-56`) |
| `permcenter.permissions.acrossterminal.AcrossTerminalPermissionContentProvider` | `com.miui.permissions.acrossterminal` | `...SYSTEM_PERMISSION_DECLARE` | Cross-end permissions to read and write (see §4.9) |
| `permcenter.privacycenter.PrivacyCenterProvider` | `com.miui.permcenter.privacycenter` | None | `setCTAStatus` / `queryShowCTA` / `queryCTAStatus` / `privacy_provider_get_using`（`PrivacyCenterProvider.java:38-80`） |
| `permcenter.privacymanager.behaviorrecord.PackageBehaviorContentProvider` | `com.miui.securitycenter.appbehaviors` | `...permission.READ_APP_BEHAVIORS` | Application behavior record query |
| `idprovider.IdProvider` | `com.miui.idprovider` | None | OAID/Device Identification (see §4.15) |
| `com.miui.sec.external.InstallProvider` | `com.miui.sec.INSTALL_PACKAGE` | None | Installation package information |

> **observed**: The vast majority of `permcenter` pages are exported without permission protection (`exported=true` and no `android:permission`), which means that any third-party app **can directly pull up the specific page** of the permission center and pass in `extra_pkgname` and other parameters to create the UI Deception/Information Detection. There are only a few with `permission` protection (`PERM_USE_INFO`, `SYSTEM_PERMISSION_DECLARE`, `READ_APP_BEHAVIORS`).

---

## 3. Core data source and write link (global)

### 3.1 Overview of the three links

```
[UI switch]
   │
   ├─(A) Permission model layer → PermissionManager(com.miui.permission)
   │        └─ ContentResolver.call("content://[<userId>@]com.lbe.security.miui.permmgr", "<code>", arg, bundle)
   │              └─ com.lbe.security.miui (pre-installed LBE permission manager, in addition to this APK) → Internal re-operation AppOpsService
   │
   ├─(B) AppOps effective layer → AppOpsManager.setMode/setUidMode/startWatchingActive (hide API + MIUI private op)
   │
   └─(C) Framework consistency → reflection IPackageManager.grantRuntimePermission / revokeRuntimePermission / updatePermissionFlags
```

### 3.2 (A) method code table of permmgr provider (**observation**, `com/miui/permission/PermissionContract.java:44-61`)

| constant | value | call point |
|---|---|---|
| `GET_ALL_PERMISSION_GROUPS` | 3 | `PermissionManager.java:355` `getAllPermissionGroups()` |
| `GET_ALL_PERMISSIONS` | 4 | `PermissionManager.java:364` |
| `UPDATE_DATA` | 5 | `PermissionManager.java:523` |
| `SET_APPLICATION_PERMISSION` | 6 | `PermissionManager.java:437,451` (asynchronous `ConcurrentUtils.execute`) |
| `GET_EFFECTIVE_PERMISSIONS` | 7 | `PermissionManager.java:374` |
| `WRITE_WAKE_PATH_WHITE_LIST` | 8 | (Wakeup path whitelist, outside this module) |
| `SET_MODE` | 9 | `PermissionManager.java:302` `setModeNow()` |
| `GET_NOT_RECORD_LIST` / `ADD_NOT_RECORD_LIST` | 10 / 11 | Behavior record exclusion list |
| `GET_PERMSSION_FOR_ID` | 12 | `PermissionManager.java:381` |
| `GET_USING_LOCATION_LIST` | 13 | Target active list |
| `SEND_WAKEPATH_PERMISSION_RECORD` / `SEND_PERMISSION_RECORD` | 14 / 15 | Behavior record reporting |
| `READ_CLIPBOARD_TIP_SETTING` | 16 | Clipboard prompt |
| `GET_PERMSSION_STATE` | 17 | — |
| `SET_APP_BEHAVIOR_RECORD_ENABLE` | 18 | `PermissionManager.java:395` |
| `GET_PERMISSION_FOR_NAME_DESC` | 19 | — |

Multi-user addressing: `content://<userId>@com.lbe.security.miui.permmgr` (`permcenter/autostart/AutoStartManagementActivity.java:414`) - URI authority prefix `<uid>@` is the multi-user writing method of MIUI ContentProvider (compare `"110@"+authority` of `r4/a.java`).

### 3.3 (B) MIUI private AppOps op code (**observation**, `android/app/AppOpsManagerCompat.java`)

`MIUI_OP_START = 10000`, `MIUI_OP_END = 10053`; AOSP op 0–133. Key items related to privacy/permissions:

| constant | value | Purpose |
|---|---|---|
| `OP_AUTO_START` | 10008 | **self-starts** (paired with `PERM_ID_AUTOSTART=16384`) |
| `OP_BOOT_COMPLETED` | 10007 | Start up |
| `OP_BACKGROUND_START_ACTIVITY` | 10021 | Background pop-up interface |
| `OP_SHOW_WHEN_LOCKED` | 10020 | Lock screen display |
| `OP_GET_INSTALLED_APPS` | 10022 | Read the list of installed applications |
| `OP_GET_OAID` / `OP_GET_OAID_USER` | 10037 / 10038 | **OAID Read (no permission required / user authorization required)** |
| `OP_GET_ANONYMOUS_ID` | 10024 | Anonymous device ID |
| `OP_GET_UDEVICE_ID` | 10025 | UDeviceID (old) |
| `OP_DEVICE_ID` | 10050 | Device identification |
| `OP_READ_CLIPBOARD` / `OP_WRITE_CLIPBOARD` | 29 / 30 | clipboard |
| `OP_CAN_BE_RECORDED` | 10041 | Can **be screen-recorded/screen-casted (privacy masking core)** |
| `OP_MEDIA_PROJECTION` | 10042 | cast screen |
| `OP_SERVICE_FOREGROUND` | 10023 | Front desk service |
| `OP_BACKGROUND_LOCATION` | 10027 | Background positioning |
| `OP_ACCESS_GALLERY` | 10034 | Photo album access |
| `OP_ACCESS_SOCIALITY` | 10035 | social relations |
| `OP_SHAKE` | 10044 | Shake it (advertisement jump) |
| `OP_READ_NOTE` / `OP_READ_RECORD` | 10052 / 10051 | Notes/recordings |
| `MODE_ALLOWED / IGNORED / ERRORED / DEFAULT / FOREGROUND / ASK` | 0 / 1 / 2 / 3 / 4 / 5 | **`MODE_ASK=5` is MIUI extension** ("each query"), AOSP has no such value |

Encapsulation layer:
- `com/miui/permcenter/compact/AppOpsUtilsCompat.java` — Reflection `checkOpNoThrow/setMode/setUidMode/startWatchingActive/startWatchingNoted/permissionToOpCode/strOpToOp`; `setModeWithXSpace()` will **and also write a copy of** (`AppOpsUtilsCompat.java` in `if (b2.c(context,str)) setMode(..., z1.j(999, z1.b(uid)), ...)`) to XSpace user 999.
- `switchOpToMiuiFlaresPermission()` — Map appops ops back to MIUI `PERM_ID`: `0/1/41/42 → 32(LOCATION)`, `26 → 4096(CAMERA)`, `27 → 131072(MIC)`, `10042 → 1`.

### 3.4 (C) Double writing demonstration (**observation**, `g4/b.java`)

- Authorization (`action==1`, i.e. "allow"): `PermissionManager.setModeNow(op, uid, pkg, 1, true)` → then `tg.a.i/j(...)` = `IPackageManager.grantRuntimePermission` + `updatePermissionFlags(state 3/2)` (`g4/b.java:382-404`)
- Rejection: `setModeNow(op, uid, pkg, i11==3 ? 0 : 4, true)` → `tg.a.f(...)` = `revokeRuntimePermission` + `updatePermissionFlags(1,1)` (`g4/b.java:406-417`)
- Special branches of storage permissions: `targetSdk<33` goes to `PermissionManagerCompat.setApplicationPermissionWithVirtual(..., PERM_ID_EXTERNAL_STORAGE, ...)` (`g4/b.java:334`); `targetSdk>=33 && permissions==-3` goes to "Media Visual Permission Group" one by one `setUidMode` (`g4/b.java:345-357`).
- **virtual permission**: `PermissionManagerCompat.setApplicationPermissionWithVirtual()` (`com/miui/permcenter/compact/PermissionManagerCompat.java:13-19`) writes an additional `virtualMap` mapping permission when `action==7` (`ACTION_VIRTUAL`), and the action is changed to 1 (deny). `virtualMap` (`PermissionManager.java:176-182`) = `READSMS→REAL_READ_SMS`, `READCONTACT→REAL_READ_CONTACTS`, `CALENDAR→REAL_READ_CALENDAR`, `READCALLLOG→REAL_READ_CALL_LOG`, `PHONEINFO→REAL_READ_PHONE_STATE` (only `overMiuiTwelve()` and SDK≥28).

---

## 4. Detailed explanation of modules

### 4.1 `permcenter.permissions` — Authorization main interface (category 24)

**function list (observation)**

| Function | class |
|---|---|
| Per-app permissions editor (Tab: Permissions/Other Permissions) | `AppPermissionsTabActivity`、`AppPermissionsFragment`、`NewPermissionsEditorFragment` |
| View the application list by permission ("Camera → Which applications can be used") | `PermissionAppsEditorActivity`、`PermissionAppsModifyFragment`、`PermissionAppsModifyActivity` |
| System permission declaration pop-up window (first request from a third-party app) | `SystemAppPermissionDialogActivity` |
| Permission usage prompt (status bar "XX is using positioning") | `AppPermissionsUseActivity`、`AppPermissionItemActivity` |
| Second permission | `SecondPermissionAppsActivity/Fragment` |
| Cross-end permissions | `acrossterminal/*`（§4.9） |
| Preference control encapsulation | `RadioButtonPreference`, `ValuePreference`, `PermTipsPreference`, `ButtonPanel`, etc. |

**link (observation)**

`AppPermissionsTabActivity` (`select_navi_item` page selection, `AppPermissionsTabActivity.java:263`)
→ `AppPermissionsFragment` / `SystemPermissionsEditorFragment` Read data: `PermissionManager.getAllPermissions(GROUP_*)`, `getAllPermissionGroups()`, `getPermissionForId()` (method 3/4/12)
→ Display three states (allow/frontend only/ask/deny) from `AppPermissionInfo.getPermissionToAction()`, action value is calculated by 8 masks from `PermissionManager.calculatePermissionAction()` (`PermissionManager.java:211-238`), `ACTION_*`: `0=DEFAULT,1=REJECT,2=PROMPT,3=ACCEPT,4=BLOCK,5=NONBLOCK,6=FOREGROUND,7=VIRTUAL` (`PermissionManager.java:24-31`)
→ User clicks → `g4.b.l()` (permission group)/`g4.b.m()` (single permission) → double write (§3.4).

**External Intent Contract (Observation)**

| entrance | parameters | Return |
|---|---|---|
| `APP_PERM_EDITOR` / `APP_PERM_EDITOR_PRIVATE` | `extra_pkgname`（`PermissionsEditorActivity.java:53`） | — |
| `APP_PERMISSION_USE` (`PERM_USE_INFO` permission required) | `extra_pkgname`（`AppPermissionsUseActivity.java:99`） | `setResult(2308)`（`AppPermissionsUseActivity.java:169`） |
| `PERMISSION_APP_EDITOR_ACTIVITY` | `extra_permission_id`、`extra_permission_name`、`group_id`（`PermissionAppsEditorActivity.java:1216,1254-1258,1389,1412`） | `permission_name` / `group_id` / `permission_id` / `permission_action` / `extra_permission_info` |
| `SYSTEM_PERMISSION_DECLARE*` | `miui.intent.extra.REQUEST_PERMISSIONS_NAMES`、`..._NAMES_DESC`、`...KEY_REQUEST_PERMISSIONS_STATE`、`...PACKAGE_NAME`、`show_locked`（`SystemAppPermissionDialogActivity.java:1041-1049,1133`） | Field with the same name + `REQUEST_PERMISSIONS_RESULTS` |

> `SystemAppPermissionDialogActivity` is the actual implementation of the MIUI private permission pop-up window (54 KB, supports both `CTA_DECLARE` and `_NO_OPTIONAL`), and writes the "permission description" back to the provider's `miui_permission_description_info` table (`SystemAppPermissionDialogActivity.java:1127,1196`, field `locale/description/pkgName/permissionId/processState`, see `PermissionContract.java:113-119`).

**data**: no local DB; all go to permmgr provider (remote). Describe cache table `miui_permission_description_info`.

---

### 4.2 `permcenter.autostart` - Autostart Management (Category 4)

- **Function**: Application-level "Allow self-start" switch + search + grouping.
- **write link (observation)**: `AutoStartManagementActivity:403-423`
  - When `z1.r()` (currently in user 0): If the target userId ∉ {0, 999}, directly `ContentResolver.call("content://<userId>@com.lbe.security.miui.permmgr", "6", null, bundle)`, `EXTRA_PERMISSION=16384(PERM_ID_AUTOSTART)`, `EXTRA_ACTION`, `EXTRA_PACKAGE=[pkg]`, `extra_flags=0`, `EXTRA_USER_ID`;
  - Otherwise `permissionManager.setApplicationPermission(16384L, action, userId, pkg)`.
  - After writing, the `f` thread delays 400 ms before doing the follow-up (`AutoStartManagementActivity.java:424-428`).
- **data**: permmgr (remote); no local table.
- **AIDL/system boundary**: `content://com.lbe.security.miui.permmgr` (method 6); supporting appop `OP_AUTO_START=10008` is dropped from the library by `com.lbe.security.miui`.
- **is launched by**: `miui.intent.action.OP_AUTO_START` (Settings→Application→Auto-start); the recommended card for `PrivacyRiskDetectionActivity` also jumps here (`PrivacyRiskDetectionActivity.java:398-400`).

---

### 4.3 `permcenter.detection` — Privacy risk detection (key projects)

**A. Detection item list (observation; item id comes from `detection/model/d.java` + `PrivacyRiskDetectionActivity.java:752-771`)**

| id | meaning (string resource) | Data source | Judgment basis |
|---|---|---|---|
| **11** | `privacy_risk_sensitive_permissions_tip` Sensitive permissions | `ub.b`(task) + `mc.c.w()` | The number of applications in the sensitive permission set "**always allows (action==3)**" > 0: `32(LOCATION)`, `8/READCONTACT`, `16/READCALLLOG`, `131072(CLIPBOARD)`, `PERM_ID_EXTERNAL_STORAGE`; SDK≥T added `-3` (media vision), `-2` (media hearing) |
| **12** | `privacy_risk_dangerous_permissions_tip` Dangerous permissions | `ub.b` → `mc.c.w()` homologous | Summary count > 0 (the "Danger Permissions" card on the UI is displayed separately, the actual data source is the same as 11, see `PrivacyRiskDetectionActivity.java:237-258`) |
| **13** | `privacy_risk_install_app_tip` Risk Application | `ub.d` → `xc.h.a()` Desktop application list → `ac.c.d(list)` Upload `flash.sec.miui.com/user/ra` → `RiskAppInfoBean.getRiskList()` | **Find the intersection of the package name set** returned by the cloud and the local desktop application |
| **14** | `privacy_risk_result_protection_tip` Thumbnail blur/screen blocked | `ub.c` | `SystemProperties.getBoolean("persist.sys.protect_image", false)` (`ub/c.java`) → If true, it means "screen occlusion" mode, otherwise "blur" mode (`detection/model/d.java:97-110`) |
| **15** | `privacy_risk_result_file_tip` file | `d.e(context, b...)` container | Subitems need to be inserted externally. Currently, `N0()` passes `r.get(18), r.get(17)` (neither is set) → **is actually always null, and is filtered by `removeAll(null)` to** (`PrivacyRiskDetectionActivity.java:760,766`). |
| **16** | `privacy_risk_result_photo_delete` Photo Deletion Protection | `ub.a` + `vf.n.a(ctx)` | Prompt (`PrivacyRiskDetectionActivity.java:545-551`) when `df.h.J(df.h.m(Application.A()))` (`ub/a.java`) is true and `vf.n.a()` is true |
| **22** | `find_leak_risk` input method leakage risk | `ub.e` → `ec.c.f25249a` + `ec.c.j()` (**locally cached cloud classification JSON**) | In the app → category mapping returned from the cloud, **is not in the user's "closed" list** and if `fc.c.b(category)` is true, an alarm will be issued (`ub/e.java`), and one-click input method switch will be jumped (`PrivacyRiskDetectionActivity.java:487-489`) |
| 21 | Fixed recommendation card (self-start/junk cleanup/uninstall) | `detection/model/c.java` | Non-detection items, fixed display (`PrivacyRiskDetectionActivity.java:767-769`) |

**B. Auxiliary count of dangerous permissions (observation, `ub/b.java`)**

- DeviceAdmin quantity: `DevicePolicyManager.getActiveAdmins().size()`
- Number of notification listeners: `queryIntentServices(NotificationListenerService)` + `NotificationManager.isNotificationListenerAccessGranted()`, excluding itself
- **UsageStats Application quantity**: Reflection `IPackageManager.getAppOpPermissionPackages("android.permission.PACKAGE_USAGE_STATS", 0)` → Determine mode==0 or `AppOpsManager.getOpsForPackage(uid, pkg, {43})` one by one (mode==3 and `checkPermission` passes) → Then filter `isPackageAvailable` and `getApplicationEnabledSetting != 2` (`ub/b.java:52-160`)

**C. UI/Jump (observation)**

`i.a()` (`PrivacyRiskDetectionActivity.java:388-492`) is assigned by item id:
- `11/12` → `PrivacyPermissionsSetActivity`（`privacyType=10/11`），`startActivityForResult(100/101)`
- `13` → `RiskAppDeleteActivity.i0(..., 102)` (uninstall risky applications in batches)
- `14` → `PrivacyThumbnailBlurSettings.v0(this, 103)`
- `16` → `jg.a.e(this, true)`
- `22` → `InputMethodManager.showInputMethodPicker()`
- `21` → `miui.intent.action.OP_AUTO_START` / `GARBAGE_CLEANUP` / `GARBAGE_UNINSTALL_APPS`

The result is backfilled: `onActivityResult` → `Q0(code)` maps 100..104 back to 11..15, and then `T0()` refreshes the script (`PrivacyRiskDetectionActivity.java:809-888,1095`).

**D. Pulled by**: `miui.intent.action.PRIVACY_RISK_DETECTION_ACTIVITY` (exported, no permission). Security Center Home Page/Settings "Privacy Protection".

---

### 4.4 `permcenter.privacyblur` — Privacy thumbnail blur (key project)

**Conclusion: There is a "switch" here, not an "algorithm".**

- **switch drop point (observation)**: `Settings.System."miui_recents_privacy_thumbnail_blur"`, the value is **comma-separated package name string** (delimiter `aa.f23523b = ","`), read and written by `ec/c.java:74-116`; the empty string means none.
  - `PrivacyThumbnailBlurSettings.x0()` (`privacyblur/PrivacyThumbnailBlurSettings.java:334-338`) → `ec.c.d(ctx,pkg,checked)` Write System Settings + `ec.c.f(pkg,checked)` Maintain "User Explicitly Closed" list `miui_recents_privacy_thumbnail_blur_close` (go to remoteprovider preference).
- **Valid party (inference → strong evidence)**: The reading party is **`com.miui.home` (MIUI desktop)**, not this App:
  - `ec.c.r()` reads `metaData "recents_blur_v2_enabled"` (`ec/c.java:231-240`) of `com.miui.home`
  - `ec.c.u()` reads `Settings.Secure."miui_home_recents_blur_mode"` ∈ {`recents_blur_mode_dim`, `recents_blur_mode_blur`} (`ec/c.java:262-277`)
  - `ec.c.s()` detects whether `com.android.settings` declares `com.android.settings.action.PRIVACY_THUMBNAIL_BLUR_SETTING` (`ec/c.java:242-246`)
  - The `"recents_blur_v2_enabled"` / `"recents_blur_mode_dim"` constant (`PrivacyThumbnailBlurSettings.java:67-76`) appearing in `PrivacyThumbnailBlurSettings` indicates that this page is just "writing settings for desktop viewing".
  - Therefore, **`PRIVACY_THUMBNAIL_BLUR_SETTING` blurs what**: **task snapshot thumbnail** of the application in desktop multitasking (Recents); in V2 mode, there are two presentations: "dim" or "blur".
- **"Screen Occlusion" variant (observation)**: When `SystemProperties.getBoolean("persist.sys.protect_image", false)` (`ub/c.java`) is true, the UI title switches to `privacy_thumbnail_shield_title`, and the task description label of this Activity is also changed (`PrivacyThumbnailBlurSettings.java:306-310, 400-422`) - that is, "screen sharing is occluded instead of blurred".
- **Cloud automatically turns on (observation)**: `ec.c.w(ctx,z)` (30 days throttling, `pref_key_last_upload_privacy_thumbnail_blur_time`) → `ac.c.e(ac.c.f(apps))` POST `https://flash.sec.miui.com/user/cat` → Return to `{pkg: classification}` JSON Save `privacy_thumbnail_blur_category` → Applications `ec.c.d(ctx,pkg,true)` that are not on the closure list and are classified as sensitive are automatically opened (`ec/c.java:279-315`).
- **temporarily protects** during the sharing period: `ec.c.b()` / `ec.c.c()` maintains the `blur_screen_share_protection` list, and `ec.c.x(ctx)` removes the temporary item (`ec/c.java:59-90,317-326`) when the sharing ends.
- Where is the **algorithm?**: Not in this APK. `com.miui.blur` is another set of **and** (§4.14). This app does not contain any pixel processing code.

---

### 4.5 `permcenter.provision` — Additional Privacy Notice (Category 6, Kotlin)

- `PrivacyExtraProvisionActivity extends miuix.provision.ProvisionBaseActivity`（`provision/PrivacyExtraProvisionActivity.java:30`），`ProvisionViewModel`（`provision/c.java`）+ `ProvisionAdapter`（`provision/b.java`）。
- **function (observation)**: Before entering certain functions for the first time, "the permissions applied for by the application × the privacy policy used" are listed, and the user needs to confirm each item. Copywriting key: `privacy_extra_provision_require_permission`, `system_permission_declare_privacy_policy` (`provision/b.java:145,172`).
- **Privacy Policy Jump to**: `startHtmlViewer` = `Intent.ACTION_VIEW` (`provision/c.java:214-221`); toast `privacy_provison_no_network` when there is no network.
- **process**:`android:process="com.miui.securitycenter.remote"` (manifest).
- **data**: no independent persistence; the confirmation result is written back to the caller (permission pop-up process).

---

### 4.6 `permcenter.capsule` — Smart Capsule/Screen Sharing Protection (Category 3)

| class | Function |
|---|---|
| `ScreenShareProtectionActivity` | Screen sharing risk pop-ups (high-risk applications detected during screencasting) |
| `FlaresCapsuleOpenActivity` | Capsule click landing page (`MIUISafety-Flares` log field) |
| `b`（13.5 KB） | Capsule data/reporting |

**ScreenShareProtectionActivity link (observation)**
1. Pulled up by the system with `Intent`, read `IS_SCREEN_SHARE_HIGH_RISK_APP` (bool), `android.intent.extra.PACKAGE_NAME`, `SHARED_PACKAGE_NAME` (`capsule/ScreenShareProtectionActivity.java:96-101`).
2. The user selects "Stop Sharing" → Reflect `MediaProjectionManager.stopActiveProjection()` (`:60-64`).
3. The user selects "Remove high-risk applications" → `AppOpsUtilsCompat.setModeWithXSpace(..., OP_CAN_BE_RECORDED=10041, mode=0)` (`:65-69`) → broadcast `com.miui.action.remove_screen_share_high_risk_app` (with permissions `miui.permission.READ_AND_WIRTE_PERMISSION_MANAGER`, `:72-74`).
4. The Activity `exported=false` is started by the capsule service/system UI in the `com.miui.securitycenter.remote` process (**infers**: no filter, can only be started explicitly).

---

### 4.7 `permcenter.settings` — Privacy Settings Aggregation (Category 26)

| sub function | class | Key implementation (observation) |
|---|---|---|
| **Other permissions** (Overview of "Other Permissions" across applications) | `OtherPermissionsActivity`(export, `SYSTEM_PERMISSION_DECLARE` required) + `OtherPermissionsFragment`(46 KB) | Aggregate non-runtime permissions (self-start/background pop-up/lock screen display/notification, etc.), writing still goes `g4.b` |
| **Privacy input mode** | `PrivacyInputModeActivity` + `PrivacyInputModeFragment` | See below |
| **Shutdown password** | `ShutDownPasswordActivity` + `ShutDownPasswordFragment` | See below |
| **Stealth mode** | `InvisibleModeActivity` + `service.InvisibleModeService` | See §4.10 |
| **capsule set** | `FlaresSettingsActivity` | Pure PreferenceFragment |
| **permission installation prompt** | `InstalledPermissionDialog` (process `remote`) | — |
| **QS Tile springboard** | `TileTransitActivity` (requires `BIND_QUICK_SETTINGS_TILE`) | — |
| **Privacy Notice** | `PrivacyProvisionActivity` | — |

**Privacy input mode (observation)**
- Setting keys (`Settings.Secure`): `miui_privacy_input_pkg_name` (currently effective IME package name), `miui_privacy_input_enabled`, `miui_privacy_input_supported`; the allowed IME whitelist comes from `R.array.input_package_names` (`c/e/f/g/h/i/j` of `x4/k1.java`).
- Interaction: `PrivacyInputModeFragment.j0()` binds `action="miui.intent.action.PREPARE_PRIVACY_INPUT_MODE"`, `setPackage (current default input method package name)` (`:153-163`); sends `what=1` (carrying `download_voice_package`) through `Messenger`, and receives `what=2` (`result`) (`:117-182`).
- Switch item: `CheckBoxPreference key="key_privacy_input_mode"` (`:323`).
- The copy claims capabilities: local processing of data, disabling sensitive permissions, stopping collection, offline voice input (`R.string.data_local_processing_and_storage`, etc., `:191-227`) - **is a UI copy, and whether it will be implemented depends on the IME implementation (unknown)**.
- **AIDL boundary**: None AIDL, only `Messenger` (`android.os.Messenger`) IPC to the input method process.

**Shutdown password (observation)**
- `ShutDownPasswordFragment` uses `miui.security.SecurityManager.getAccessControlPasswordType()` (the same model as `SettingLockFragment.java:1159`) - that is, **and App Lock share the same password storage** (system_server). `ShutDownPasswordActivity` requires `SYSTEM_PERMISSION_DECLARE` protection.
- Details (storage key/validity period) → **Unsolved Mystery** (This module did not find the independent SP/DB).

---

### 4.8 `permcenter.untrustwarn` — Unknown source installation alarm (key project)

**observation conclusion: This package does not exist in DEX.**

- Listing statement: `<activity android:exported="false" android:name="com.miui.permcenter.untrustwarn.UnknownSourceWarnActivity"/>` (`AndroidManifest.xml`), **without intent-filter**.
- Code: There is no `untrustwarn` directory under `_analysis\jadxout\sources\com\miui\permcenter\`; grep `UnknownSourceWarn` / `untrustwarn` / `UNKNOWN_SOURCE_VERIFY` in the whole tree has zero hits.
- Similar dangling statement: `com.miui.permcenter.privacymanager.XSpaceWarningDialogActivity` (also missing category).
- The actual "unknown source installation" of **is intercepted in `permcenter.install`** (see below) and has nothing to do with `untrustwarn`.
- Verdict: Manifest remains/components have been removed but the manifest has not been cleaned up. Whether **is still pulled up by a certain path in the system → Unknown** (static cannot be denied).

Installation verification link for **`permcenter.install` (observation, `install/PackageVerificationReceiver.java`)**

```
System package installer (com.miui.packageinstaller / com.android.packageinstaller)
   └─ Broadcast com.miui.action.PACKAGE_NEEDS_VERIFICATION (protected with permission=android.permission.INSTALL_PACKAGES)
        └─ PackageVerificationReceiver.onReceive()
             ├─ Get extras: "installerPackage", IBinder "observer" (IMessenger), session id
             ├─ IMessenger.Stub.asInterface(observer)  (:219)
             ├─ Whitelist installer: {com.miui.packageinstaller, com.google.android.packageinstaller, com.android.packageinstaller} (:40-42)
             ├─ Google/Play Scene independent broadcast com.miui.global.packageinstaller.action.verifypackage
             │     With permission com.miui.securitycenter.permission.GLOBAL_PACKAGEINSTALLER (:47-53)
             └─ Conclusion receipt: IMessenger.send(msg), "verify package X, finish: true/false" (:116-127,168-208)
                  · Signature verification failed → false ("can't install, signature verify fail")
                  · webview installer whitelist deny → false
                  · pass → true
```

- `AdbInstallActivity` (`com.miui.securitycenter.intent.action.INSTALL_PACKAGE`, exported) handles **ADB. Install** confirmation pop-up window, read `android.content.pm.extra.SESSION_ID` and `IBinder "observer"` (`install/AdbInstallActivity.java:176-188`), layout `pm_adb_install_alert`.
- Related switches: `security_adb_install_enable` (`RemoteProvider.h()` specializes in process-level authentication, only allows this package or `com.miui.klo.bugreport` to write, `com/miui/common/persistence/RemoteProvider.java:174-190`).
- `AdbInputApplyActivity` (`com.miui.securitycenter.action.ADB_INPUT_APPLY`) - ADB input authorization.

---

### 4.9 `permcenter.acrossterminal` — Cross-end permissions (type 3 + provider)

- **storage (observation)**: `/data/misc/user/<userId>/securitycenter/across_terminal.xml`, directory `/data/misc/user/<userId>/securitycenter/` (`permissions/acrossterminal/b.java:28-31`). XML structure:
  ```xml
  <runtime-permissions version="1">
    <account AccountId="<Xiaomi account name>">
      <package TerminalId=".." TerminalName=".." TerminalType="..">
        <permission name=".." action=".." onetimeFlag=".."/>
  ```
  (`:56-102` writes; `:104-168` reads; account source `we.e.c(context)` + account change callback `b.f18331e`, `:39-54,170-173`)
- **Provider method (observation, `AcrossTerminalPermissionContentProvider.call()`)**

| method | parameters | Return |
|---|---|---|
| `get_all_terminal` | — | `extra_data` = All terminals under this account |
| `get_all_permission` | `terminalId` | Permission table for this terminal |
| `check_permission` | `terminalId`、`permissionName`、`terminalName`、`onetimeFlag` | `action` (int); if it is 2 (PROMPT), record a prompt `kc.q.G(ctx).D(perm, terminalId)` |
| `update_terminal_name` | `terminalId`、`terminalName` | — |
| `update_terminal_permission` | `terminalId`、`permissionName`、`terminalName`、`terminalType`、`action`、`onetimeFlag`、`override_onetime` | — |
| `temp_close_capsule` | bundle | → `z4.d.j(ctx,bundle)` (temporarily close capsule) |
| `notify_terminal_privacy` | bundle | → `kc.q.G(ctx).U(bundle)` (notify terminal privacy status) |

- **permission name maps**: `dc/d.java` (`dc.d.a(permissionName)`). The cross-end permission is **string permission name** instead of `long PERM_ID`.
- The **service depends on**: `kc.q` = cross-continuity service singleton. **infers that** corresponds to `com.xiaomi.continuity` (`com.xiaomi.continuity.InitializationProvider` is in the manifest), and registers `com.miui.continuity.netbus.*` in the manifest.
- **pulled by**: no filter, `exported=true` + `SYSTEM_PERMISSION_DECLARE` protection → Only system components (settings, cross-terminal services) with this permission can be called.

---

### 4.10 `permcenter.service.InvisibleModeService` — Stealth Mode (Observation)

**This is not "input privacy", but "camera/microphone/location indicator light stealth"**.

- `onStartCommand` → `b()`（`service/InvisibleModeService.java:97-120`）
- Affects the op set: `{0, 1, 41, 42, 27, 26}` = `COARSE_LOCATION, FINE_LOCATION, MONITOR_LOCATION, MONITOR_HIGH_POWER_LOCATION, RECORD_AUDIO, CAMERA` (`:37`)
- Target package:
  - `27 (RECORD_AUDIO)` → `com.miui.voicetrigger`
  - `26 (CAMERA)` → `com.miui.face`
  - Others → `com.android.camera` (only when SDK==31 and `IS_SUPPORT_CDD`) or `null` (`:51-53`)
- Method: Reflect `AppOpsManager.setUserRestrictionForUser(int code, boolean, IBinder token, String[]/PackageTagsList, int userId)` (`:63-96`), and do it again for users `0` and `999` (XSpace) (`z1.r()` judgment)
- Hide status bar icon: `StatusBarManager.setIconVisibility("stealth", bool)` (`:41-59`)
- Broadcast: `com.miui.action.sync_status_bar` (authority `miui.permission.READ_AND_WIRTE_PERMISSION_MANAGER`)
- Local switch judgment: `o.a.b(this)`; capability switch `o.f18016m`
- **data**: no persistence (replay according to current settings each time you enter).

---

### 4.11 `permcenter` other sub-packages

| subpackage | Number of classes | Description (observation) |
|---|---|---|
| `privacymanager` | 12 | Interception permission pop-ups (`SpecialPermissionInterceptActivity`, `InterceptPermissionFragment`, `InterceptMIUIFragment`, `InterceptBaseFragment`, `DeviceManagerApplyActivity`, `AppBackgroundManagerActivity`), behavioral widget `widget/BehaviorWidget`+`widget/a` (17.7 KB). `miui.intent.action.SPECIAL_PERMISSIO_NINTERCEPT` (original spelling, missing `N`), `miui.intent.action.MANAGER_BACKGROUND`, `com.miui.securitycenter.action.DEVICE_MANAGER_APPLY` |
| `privacycenter` | 5 + usage 4 | `PrivacySafetyActivity`（`PRIVACY_SETTINGS`/`SECURITY_CENTER_SETTINGS`/`PRIVACY_SAFETY`）、`PrivacyFragment`(25 KB)、`SecurityFragment`(25.9 KB)、`PrivacyCenterProvider`（`setCTAStatus`/`queryShowCTA`/`queryCTAStatus`/`privacy_provider_get_using`）。`usage/*`：`AppPermissionUsageActivity`(59 KB)、`PermissionUsageRecordActivity`(52 KB)、`PermissionUsageTaskActivity`、`RecentBehaviorPreference`(14 KB) |
| `monitor` | 3 | `AppUsagePersistService` (`miui.intent.action.persist_behavior`), `MaliciousBehaviorTrackService`, `a`(25.5 KB)/`b`(13.9 KB). Read `PermissionContract.RECORD_URI` (`monitor/a.java:349`: `permissionId,pkgName,calleePkg,endTime,count,op` + `calleePkg=='null'` + `_id DESC`); threshold key `record_threshold` (`monitor/b.java:59`); cloud control `get_cloud,behavior:...,include_system:...,record_threshold:...` |
| `isolate` | 2 | `SandboxManagerService` (12.3 KB, `exported=false`, process `remote`) + `a` (9 KB). Application sandbox/isolation (`miui.security` side) |
| `install` | 8 | See §4.8; also `PackageManagerActivity` (17.9 KB), `RiskAppAuthActivity`/`History`, `AdbInstallVerifyActivity`, `UnknownSourceVerifyActivity` (`com.miui.securitycenter.action.UNKNOWN_SOURCE_VERIFY`, **Note: This is the "Unknown Source Verification" page, not `untrustwarn`**) |
| `root` | 5 | `RootApplyActivity`、`RootManagementActivity`、`NotSupportRootActivity`、`RootUpdateReceiver`(`com.android.updater.action.ACQUIRED_ROOT_SUCCESSED`) |
| `wakepath` | 3 | Wake-up path management (`WakePathManagerActivity`/`ListFragment`/`DetailFragment`), corresponding to permmgr method 8 `WRITE_WAKE_PATH_WHITE_LIST` |
| `compact` | 8 | Compatible layer: `AppOpsUtilsCompat`, `PermissionManagerCompat`, `MiuiSettingsCompat`, `SystemPropertiesCompat`, `IntentCompat` (`getIBinderExtra` reflective), `ReflectUtilHelper` |
| `model` / `utils` / `widget` / `detection` | — | Data beans (`LocalAppInfoBean`, `AppPermissionInfo`), `PlaceHolderPreference`, `ContentPressPreference` |
| Top Miscellaneous `a…l.java`/`n.java`(57 KB)/`q.java`(29 KB)/`l.java`(33 KB) | — | Obfuscated tools/controllers (`n` includes `y(ctx,boolean,List<Long>,xc.c)` permission batch query, `r()`, etc.) |

---

### 4.12 `applicationlock` — Application Lock (Key Special Project)

**A. Function list (observation)**

| Function | class |
|---|---|
| First time user guide | `FirstUseAppLockActivity`（`com.miui.securitycenter.action.LOCK_APP`） |
| App lock list/switch | `AppLockManageFragment`(51 KB)、`PrivacyAndAppLockManageActivity` |
| Set lock type (pattern/number/mixed password/fingerprint/face) | `ChooseLockTypeActivity`、`ChooseAccessControl`(42.5 KB)、`SettingLockFragment`(47.6 KB)、`SettingLockActivity` |
| Unlock interface | `ConfirmAccessControl`(108.7 KB), `ConfirmAccountActivity` (forgot password) |
| Notification content blocked | `MaskNotificationActivity` |
| Recommended to lock | `ApplockRecommendActivity` |
| Privacy app unlock | `PrivacyAppsConfirmAccessControl` |
| control | `LockPatternView`(31.8 KB), `MiuiKeyBoardView`(25.2 KB), `MiuiNumericInputView`, `NumberPasswordEditText`, `PhoneGLSurfaceView` (face preview), `PasswordUnlockMediator`, `BindAccountDialog` |

**B. Unlock the main link (observation)**

```
The system (AMS/AppLock interception) detects that a protected app enters the foreground
  └─ Explicitly start applicationlock.ConfirmAccessControl
       · Intent with IBinder "android.app.extra.PROTECTED_APP_TOKEN" (ConfirmAccessControl.java:1991-1993)
       · Also supports miui.intent.action.CHECK_ACCESS_CONTROL (exported)
  └─ onCreate:
       securityManager = (SecurityManager) getSystemService("security")   (:1368)
       mPasswordType   = securityManager.getAccessControlPasswordType()   (:1373)  // "numeric"/"mixed"/"pattern"
       Corresponding unlock control according to type inflate (widget/e subclass: q=number, LockPatternView-based=pattern, mixed password EditText) (:1384-1401)
       securityManager.exemptTemporarily(...)                            (:999)
  └─ onResume:
       · If passwordType changes → finish() (:2485-2489)
       · needFinishAccessControl(activityToken) == true → finish() (:2520-2530) // The protected App is no longer in the foreground
       · Countdown/fingerprint disabled state processing (:2532-2549)
  └─ User input:
       Numeric password → widget/q.A(pwd) → securityManager.checkAccessControlPassword("numeric", pwd) (widget/q.java:129-136)
       Pattern/Mixing → Respective Controls → SecurityManager Validation (Same Origin)
       Fingerprint → Fingerprint callback in AppLockManageFragment → c3.f.b(code, ctx) (AppLockManageFragment.java:522)
       Face → callback chain onFaceAuthenticated/onFaceLocked/onFaceHelp/onFaceAuthFailed/onFaceStart (AppLockManageFragment.java:575-627)
  └─ Verification successful → c3.f released: securityManager.addAccessControlPass(pkg) (c3/f.java:250)
                and removeAccessControlPass clean (c3/f.java:391-396)
```

**C. Safety boundary (observation)**

- **password/pattern never enters this APK**: setting = `SecurityManager.setAccessControlPassword(pwd,type)` (`applicationlock/ChooseAccessControl.java:509`; clear = `c3/f.java:547` and pass `(null,null)`), verification = `checkAccessControlPassword(type,pwd)`. → Hash and compare in system_server.
- The **release voucher is the "passed note"** of system_server: `addAccessControlPass(pkg)` / `checkAccessControlPass(pkg)` / `checkAccessControlPassAsUser(pkg,user)` (`c3/f.java:435,478`). Even if the App process is compromised, it cannot create tickets "out of thin air" on its own (the tickets are only in the memory of SecurityManager).
- **Masking and Limiting**: `ChooseAccessControl.java:502` Reflection `setUserRestriction(op, bool, IBinder)` (behavior during limit setting).
- **notification content blocks**: `MaskNotificationActivity` + `SecurityManager.getApplicationMaskNotificationEnabledAsUser(pkg,userId)` / `setApplicationMaskNotificationEnabledForUser(pkg,bool,userId)` (`applicationlock/f.java:230`, list homogeneous call point). When using privacy applications, you can also use `INotificationManager.setNotificationsEnabledForPackage` to directly turn off notifications (`se/e.java:96-118`).
- **forgot the password**: `ConfirmAccountActivity` is bound to `IXiaomiAccountService.Stub.asInterface(iBinder)` (`ConfirmAccountActivity.java:174-177`); after Xiaomi account verification is passed, `forgetPrivacyPassword` and write `gb_xiaomi_id_md5_key` (Base64, `c3/w.java:60-80`). → The recovery path depends on the Xiaomi account. **does not have a local backdoor** (within the observation range).
- **side effects**: `AppLockManageFragment` record `cancel_fingerprint_verify_times`/`cancel_fingerprint_guide_times` (`:178,208,426,431`) is used to suppress boot pop-ups.

**D. Data and persistence (observation)**

- Application lock password/pattern, protected package name list, notification mask switch: **are all in `SecurityManager`** of system_server (this APK has no corresponding SP/DB).
- There are only statistics/boot keys locally (using remoteprovider): `applock_alarm_count`, `applock_LockModePrompt`, `applock_notifycation_clicked`, `applock_verify_and_activate_fingerprint_<userId>`, `gb_xiaomi_id_md5_key` (`c3/f.java:513,612,665,138-153,214-222`).
- `applock_unlock_mode`, `applock_countDownTimer_deadline` write `Settings.Secure` (`c3/f.java:535,624`).

---

### 4.13 `privacyapps` — Privacy application/privacy space (key projects)

**A. Function and link (observation)**

- The implementation location of **storage isolation is**: the true source of data is `miui.security.SecurityManager`:
  - `isPrivacyApp(pkg, userId)`、`getAllPrivacyApps(userId)`、`setPrivacyApp(pkg, userId, isPrivacy)`（`se/a.java:47-66`、`se/c.java:65-97`、`se/e.java:120`）
  - Traverse `UserManager.getUserProfiles()` → **according to Android multi-user profile and isolate** (not a self-built VirtualUser)
- **switch and status (`Settings.Secure`)** (`se/a.java`)
  - `is_privacy_apps_enable` (master switch, `:44-46`)
  - `is_privacy_apps_password_enable` (whether to use password to protect privacy space, `:33-35`; `-1` = not initialized)
  - `privacy_apps_shield_message_enable` (message blocking, `:29-31`)
  - The first boot mark goes to remoteprovider: `is_first_setting_privacyapp`
- **Associated actions when privacy applications are enabled (`se/e.java`)**: `g(ctx,pkg,userId,isPrivacy)`
  1. `SecurityManager.setPrivacyApp(pkg,userId,isPrivacy)`
  2. `ContentResolver.notifyChange(content://com.miui.securitycenter.provider/update_privacyapps_icon)` (`pe/a.java`) — Notification desktop refresh icon
  3. Broadcast `com.miui.gamebooster.PPRIVACYAPP` (with `pkgName`/`isPrivacy`/`userId`), permission `com.miui.dock.permission.DOCK_EVENT` (`:54-60`)
- **Cleanup when joining the private space (`se/e.java:104-176`)**
  - Removed from recent tasks: `ActivityManager.getRecentTasks()` matches `persistentId` → `AppManageUtils.q0(persistentId)` (`:130-150`)
  - Kill process: `AppManageUtils.m(activityManager, pkg, userId)` (`:170`)
  - Turn off notifications for this app: Reflection `INotificationManager.setNotificationsEnabledForPackage(pkg, uid, false)` (`:96-118`)
  - All executed in `ef.z.d().b(...)` (background single thread)
- **whitelist recycling**: `se/c.java`’s `recoverWhiteApps()` versus `{com.android.deskclock, com.miui.securitymanager}`’s forced removal from the privacy space and restoration of notifications (`:33-34,58-92`)
- **`app.hide` Entrance disguise (observation)**: `PrivacyAppsProvider` authority = `com.miui.securitycenter.app.hide`, but `call()` **only supports `isPrivacyApp`**, and other methods directly return null (`privacyapps/PrivacyAppsProvider.java:39-56`). → The name is "hide", but it actually means "query whether a certain package is in the private space", which is used by the desktop/system to determine whether to hide icons and tasks. **does not have the real data reading and writing interface of**.
- **entrance**: `PrivacyAppsHelper` (requires `miui.permission.USE_INTERNAL_GENERAL_API`) first `se.a.c()` judgment (with password and enabled) → jump to `PrivacyAppsConfirmAccessControl` verification → `setResult` and then enter `PrivacyAppsActivity` (`privacyapps/ui/PrivacyAppsHelper.java:30-46`).
- `PrivacyAppsActivity` will reuse the application lock (`privacyapps/ui/PrivacyAppsActivity.java:157-160`) with `ConfirmAccessControl` / `LockChooseAccessControl`.

**B. Whether there is file encryption / independent user**: **did not observe**. Privacy space = application collection under the same userId + hidden processing of desktop/task/notification; **not seen** per-app encryption, `VirtualUser`, independent mounting namespace code. → If MIUI really has storage isolation, its implementation is not in this APK (**is unknown/on the system side**).

**C. Data**: `SecurityManager` (system_server) + `Settings.Secure` three keys + `update_privacyapps_icon` URI as refresh signal. No DB.

---

### 4.14 `com.miui.blur` — Bokeh SDK (clarification)

- Catalog `com/miui/blur/sdk/backdrop/`, category 18; **pure graphics SDK, not related to privacy**.
- Capability switch: `BlurManager.f12315a = (SDK==30) && exists android.view.MiuiCompositionSamplingListener && ro.miui.backdrop_sampling_enabled && ro.miui.ui.version.code>=11` (`blur/sdk/backdrop/BlurManager.java:84`); SDK≥31 go `f12316b` Branch (`:85`).
- Data path: `MiuiCompositionSamplingListener.register(listener, 0, SurfaceControl, 16.0f, periodNs)` (`p.java:388-392`) → SurfaceFlinger synthetic sampling → `GraphicBuffer` → RenderScript `ScriptIntrinsicBlur` (`p.java:174-176`) + `Allocation` (`p.java:127-129`).
- SurfaceControl is obtained by reflection `ViewRootImpl.getSurfaceControl()` (`b.java:57-61`, `u.java:41-47`, `p.java:388`).
- The relationship between **and the "Privacy Ambiguity" of this report: only the names are similar.** permission/privacy module does not reference `com.miui.blur` (`permcenter`/`privacyapps`/`applicationlock` have no import).

---

### 4.15 `idprovider` — OAID / Equipment identification (key projects)

**A. OAID Authorization pop-up window (observation, `idprovider/ui/GrantPermissionActivity`)**

1. Trigger: When the App call `AppOpsManager.noteOp(OP_GET_OAID_USER=10038)` is judged as "asking required" by MIUI, the system pulls up `miui.intent.action.REQUEST_PERMISSIONS_FOR_OAID` (exported, without permission).
2. `onCreate`：
   - `d.g()` feature switch is not satisfied → finish (`:118`)
   - `getCallingPackage()` is empty → finish (`:119-124`)
   - `getLaunchedFromUid()` reflects the caller uid, which is 0 → finish (`:132-137`)
   - `d.f36228a.b(this)` (Policy allows pop-ups) otherwise directly reject and return the result (`:143-147`)
   - `AppOpsUtilsCompat.checkOpNoThrow(appOps, callingPkg, uid, OP_GET_OAID_USER)`: When the **value ≠ 5 (MODE_ASK), the result** will be returned directly (allowed/denied, no more disturbing) (`:149-157`)
3. Pop-up window copy `permission_oaid_desc` (including application name, `:169-171`), button allow/deny (`:173`)
4. The result is written back (`:69-99`):
   ```
   setResult(RESULT_OK, {"REQUEST_PERMISSIONS_RESULTS": 0=allow / 1=deny})
   AppOpsUtilsCompat.setMode(appOps, callingPkg, uid, OP_GET_OAID=10037,  allow?1:0)
   AppOpsUtilsCompat.setMode(appOps, callingPkg, uid, OP_GET_OAID_USER=10038, allow?1:0)
   ```
   → Note: `z10 = (i10==1)` means "clicked the reject button", the code is written `!z10 ? 1 : 0`, that is, **allow=1, reject=0** (contrary to AOSP's 0=ALLOWED/1=IGNORED, which is lbe/MIUI Lateral semantics, **is observed but the semantics is questionable, and the annotation is inferred**).
5. Telemetry events: `1127.35.0.1.35763` (pop-up window display), `1127.35.0.1.35764` (click, including `click_content=允许/拒绝`) (`:66,183-186`)

**B. `IdProvider` (31 KB, authority `com.miui.idprovider`, process `remote`)**: The read side of OAID/AAID/VAID. Capability switch: `AppOpsUtilsCompat.isSupportOAIDApps()` = `!International version && 10037 < MIUI_OP_END && (miuiVersion>11 || Development version)` (`permcenter/compact/AppOpsUtilsCompat.java`).
**C. Setting page**: `OAIDSettingsActivity`, `OAIDAppSettingsActivity` (managed by application), legacy version `OAIDAppsActivity`/`OAIDSettingsLegacy` (`miui.intent.action.OAID_APPS`).
**D. Data**: The OAID value and authorization table are in the `com.miui.idprovider` process itself (in `IdProvider`); there is no DB on this APK side.
**E. Unsolved mystery**: OAID generation algorithm (whether random UUID + reset strategy), complete method list of `IdProvider` (not expanded method by method).

---

### 4.16 `appmanager` — Application Management (Key Special Projects)

**A. Function list (observation)**

| Function | realize |
|---|---|
| Application List (All/Users/System) | `AppManagerMainActivity`（`miui.intent.action.APP_MANAGER`）、`fragment/ManageFragment`(76 KB)、`fragment/AppFragment` |
| Application details (including permissions/storage/notifications/networking) | `ApplicationsDetailsActivity`（`miui.intent.action.APP_MANAGER_APPLICATION_DETAIL`）、`fragment/ApplicationsDetailsFragment`(135 KB) |
| Storage details | `AMAppStorageDetailsActivity`、`fragment/AMStorageDetailsFragment` |
| Settings page (including "disable/enable") | `AppManagerSettings`、`fragment/AMSettingsFragment` |
| Application information | `AMAppInfomationActivity`、`fragment/AMAppInformationFragment` |

**B. Key Actions and System Calls (Observation, `appmanager/AppManageUtils.java`, 60.7 KB)**

| action | call |
|---|---|
| **batch uninstall** | `deletePackageAsUser`: SDK>25 use `(String, int, IPackageDeleteObserver, int, int)`, otherwise `(String, IPackageDeleteObserver, int, int)` (`:930-955`) |
| **Clear cache** | `IPackageDataObserver.Stub`（`ClearCacheObserver`，`:124`）+ `deleteApplicationCacheFiles`（`:861`） |
| **clear data** | `ClearUserDataObserver`（`:141`）+ `clearApplicationUserData`（`:880`） |
| **Deactivate/Enable** | `PackageManager.setApplicationEnabledSetting(pkg, state, flags)` (`fragment/AMSettingsFragment.java:161` enabled; `fragment/ApplicationsDetailsFragment.java:720` disabled) |
| **Notification switch** | `INotificationManager.areNotificationsEnabledForPackage` / `setNotificationsEnabledForPackage`（`:778,1319`） |
| **WiFi scan trigger** (used to refresh the network status of the list) | `IWifiManager.startScan` (3 heavy loads, `:430,446,461`) |
| **window/task** | `IWindowManager`（`:675`）、`removeTask(persistentId)`（`q0`，`:1099`） |
| **usage statistics** | `UsageStatsManager.queryUsageStatsAsUser(4, from, to, userId)` reflection (`:985-1008`) |
| **privacy application linkage** | `h0(ctx,userId)` Check whether `getAllPrivacyApps` is available (`:906-916`); `z0(pkg,userId,bool)` Change notification switch (`:1317`) |
| **XSpace detects** | `MiuiSettings$Secure.getBoolean("xspace_enabled")`（`:957-964`） |

**C. Connection** with "Permission Management Portal": Click "Permissions" in the application details → pull up `miui.intent.action.APP_PERM_EDITOR` (`extra_pkgname`). Batch uninstallation still uses `IPackageManager`, which has no shared status with the permission center.
**D. Data**: no own DB; `r4.a` (remoteprovider) stores UI tags such as `am_update_clicked` (`:926-928`).

---

### 4.17 `devicepermission` — Device permissions (cross-device sharing permissions)

- Scenario: Authorization/editing of device-level permissions such as `com.miui.permission.SCREEN` (cross-device "screen" permissions).
- Components: `GrantDevicePermissionActivity` (`miui.intent.action.REQUEST_DEVICE_PERMISSIONS`), `DevicePermissionEditorActivity` (`EDITOR_DEVICE_PERMISSIONS`), `DevicePermissionSettingsActivity` (`DEVICE_PERMISSION_SETTINGS`), `DevicePermissionModifyActivity`; the latter three require `...SYSTEM_PERMISSION_DECLARE`.
- Source of permission items: mapping table of `dc/d.java:29-77` (`com.miui.permission.SCREEN` → `R.string.connected_devices_screen*`).
- Write: `grantpermission/a.java` (8.1 KB) + `editpermission/DevicePermissionModifyFragment.java` (16.9 KB) → take the same `g4.b` / `PermissionManager` link; `DevicePermissionSettingsActivity` will also `b0(this,"com.miui.permission.SCREEN", true)` (`DevicePermissionSettingsActivity.java:48`).

---

### 4.18 `combinepermission` — Combined permission authorization pop-up window (CTA)

- Components: `CombinePermissionActivity` (`REQUEST_PERMISSIONS_COMBINE`/`CTA_REQUEST_PERMISSIONS_COMBINE`, `SYSTEM_PERMISSION_DECLARE`, `noHistory=true` required).
- Input parameters (`CombinePermissionActivity.java:164-177`): `miui.intent.extra.REQUEST_PERMISSIONS_NAMES`, `..._NAMES_DESC`, `...KEY_REQUEST_PERMISSIONS_STATE`, `...PERMISSIONS_DIALOG_DESC`, `...PACKAGE_NAME`.
- Output parameters (`:130-143`): names with the same name + `miui.intent.extra.REQUEST_PERMISSIONS_RESULTS` (int[]).
- Submission process (`combinepermission/grantpermission/a.java:371-390`): Traverse `PermissionPreferenceFragment.d0()` to obtain `LinkedHashMap<permissionId, action>`. When `m.d(id)` (whether it is a permission group) is established, go to `g4.b.l(...)`, otherwise go to `g4.b.m(...)` → double write (§3.4).
- Characteristic switch constant: `f12476n = "miui.intent.action.CTA_REQUEST_PERMISSIONS_COMBINE"` (`:32`) - used to distinguish "normal combination" from "CTA scenario".

---

### 4.19 `appcompatibility` - Application Compatibility Tip (Category 1)

- `AppExcepitonTipsActivity` (original spelling), action `com.miui.appcompatibility.LaunchDialog.launcher` / `.appstore`, `Theme.Light.NoTitle.Transparent`.
- Function: When the app crashes/is incompatible, a prompt pops up and guides the upgrade (Google Play/App Store). No coupling with permissions/privacy.

---

### 4.20 `com.miui.permission` — Permissions SDK (Category 16, global cornerstone)

| class | Key content |
|---|---|
| `PermissionManager` | 115 `PERM_ID_*` (long bit mask), `GROUP_*`, `ACTION_*`, `TYPE_*`, `virtualMap`, `calculatePermission*/calculatePermissionCount/supportAlwaysAllow/supportCloseBehaviorRecord`, 8 provider method packages |
| `PermissionContract` | authority/method code/table structure (§3.2, §3.3) |
| `StoragePolicyContract` | `com.lbe.security.miui.storagepolicy`, `SPLIT_PACKAGE_OP="@"`, `SPLIT_MULTI_PATH` (for `ec.c.z()` to spell multi-value string) |
| `RequiredPermissionsUtil` | MIUI extended permission string: `PERMISSION_GET_INSTALLED_APPS`, `PERMISSION_SHAKE`, `PERMISSION_READ_RECORD`, `PERMISSION_READ_NOTE`, etc. (used for `permissionToOpCode` special judgment) |
| `StaticGroup` | Group whitelist (clipboard/call contacts/text messages MMS/storage) |
| `AnalyticNameUtil` | Permissions → Bury name |
| `support/util/*` | `ReflectUtil`、`SdkLevel`、`MiuiCommonUtil`、`SystemPropertiesCompat`、`ConcurrentUtils` |

---

## 5. AIDL / System service boundary summary (must be pre-installed + evidence of system permissions)

> **Important correction**: `miui.security.IPermissionManager` / `AppOpsService` AIDL **assumed in the assignment book does not exist** in this APK. The true boundaries are as follows (all observations):

| # | border | form | Evidence |
|---|---|---|---|
| 1 | `com.lbe.security.miui` (LBE permission manager) **ContentProvider** | `ContentResolver.call(Uri, methodCode, arg, bundle)`；URI `content://[<userId>@]com.lbe.security.miui.permmgr` | `com/miui/permission/PermissionContract.java:10,15`；`com/miui/permission/PermissionManager.java:302,356,364,374,381,395,437`；`permcenter/autostart/AutoStartManagementActivity.java:414` |
| 2 | `miui.security.SecurityManager` (`"security"` service of system_server, **framework class is not this APK**) | Direct forced transfer call (non-AIDL generated code): `getSystemService("security")` | `applicationlock/ConfirmAccessControl.java:1368`; `c3/f.java:250,391-396,435,478,547`; `se/e.java:120`; `se/a.java:47-66`; `privacyapps/PrivacyAppsProvider.java:79`; `permcenter/service/InvisibleModeService.java` (indirect) |
| 3 | `ISecurityManager$Stub` (reflection direct acquisition) | `Class.forName("miui.security.ISecurityManager$Stub").asInterface(ServiceManager.getService("security"))` → `getCurrentUserId()` | `m2/j.java:17` (the only place where **explicitly calls** through Binder’s security) |
| 4 | `AppOpsManager` Hide API | Reflection `checkOp/checkOpNoThrow/noteOpNoThrow/setMode/setUidMode/startWatchingActive/startWatchingNoted/permissionToOpCode/strOpToOp/unsafeCheckOpRawNoThrow/opToSwitch/opToPermission` | `com/miui/permcenter/compact/AppOpsUtilsCompat.java`、`android/app/AppOpsManagerCompat.java`、`ub/b.java:112-160` |
| 5 | `AppOpsManager.setUserRestrictionForUser` | Reflection (signature changes with SDK: SDK≤30 `String[]`, SDK>30 `PackageTagsList`) | `permcenter/service/InvisibleModeService.java:63-96`；`applicationlock/ChooseAccessControl.java:502` |
| 6 | `IPackageManager` | Reflection `asInterface` → `getAppOpPermissionPackages` / `isPackageAvailable` / `getApplicationEnabledSetting` / `deletePackageAsUser` / `grantRuntimePermission` / `revokeRuntimePermission` / `updatePermissionFlags` | `ub/b.java:52-160`；`appmanager/AppManageUtils.java:930-955,1109`；`se/e.java:70-90`；`tg.a.*` |
| 7 | `INotificationManager` | Reflection `areNotificationsEnabledForPackage` / `setNotificationsEnabledForPackage` | `appmanager/AppManageUtils.java:778,1319`；`se/e.java:68-116` |
| 8 | `IWindowManager` | reflection | `appmanager/AppManageUtils.java:675` |
| 9 | `IWifiManager` | Reflection `startScan` (3 overloads) | `appmanager/AppManageUtils.java:430,446,461` |
| 10 | `IUsbManager` | Reflection (default USB configuration cleanup / `clearDefaults`) | `appmanager/fragment/ApplicationsDetailsFragment.java:1076`；`AppManageUtils.i()`（`clearDefaults`） |
| 11 | `StatusBarManager.setIconVisibility("stealth")` | reflection | `permcenter/service/InvisibleModeService.java:41-59` |
| 12 | `MediaProjectionManager.stopActiveProjection` | reflection | `permcenter/capsule/ScreenShareProtectionActivity.java:60-64` |
| 13 | **original APK own AIDL**: `ISecurityCenterNotificationListener` | `.Stub.asInterface()` (the server is elsewhere, this APK is the client) | `permcenter/i.java:144` |
| 14 | **own APK own AIDL**: `IMessenger` (`android.os.IMessenger`) | Package installer callback channel `IBinder "observer"` | `permcenter/install/AdbInstallActivity.java:178-182`；`install/PackageVerificationReceiver.java:217-221` |
| 15 | `IXiaomiAccountService` | `.Stub.asInterface()` (forgot password/account binding) | `applicationlock/ConfirmAccountActivity.java:174-177` |
| 16 | `IPackageDataObserver` (AIDL Stub implementation) | Clear cache/clear data callback | `appmanager/AppManageUtils.java:124,141` |
| 17 | `IPackageDeleteObserver` | Uninstall callback | `appmanager/AppManageUtils.java:930-955` |
| 18 | `android.permission.PermissionManager` (internal `mPermissionManager` field) | reflection fetch field | `g4/b.java:116` |
| 19 | `android.miui.AppOpsUtils.isXOptMode` | reflective static | `AppOpsUtilsCompat.java` `isXOptMode()` |
| 20 | `android.app.AppOpsManagerInjector.isAutoStartRestriction` | reflective static | `AppOpsManagerCompat.java` `isAutoStartRestriction()` |

Evidence that **"must be pre-installed"**:
- The authority of `content://com.lbe.security.miui.permmgr` belongs to another package **`com.lbe.security.miui`** (the literal value of `PermissionContract.java:10` + `PermissionManager.java:511` to `getPackageInfo`) → This App depends on this pre-installed package to work. **observes**.
- `android:sharedUserId` in the list needs to be checked (**is not checked and** is not checked, see §9).
- A large number of `permission="com.miui.securitycenter.permission.*"` self-signed permissions + `miui.permission.USE_INTERNAL_GENERAL_API` to protect own components → Typical system application signature-level permission model.

---

## 6. Data and persistence summary

| Type | location | content | Evidence |
|---|---|---|---|
| **SharedPreferences** | `remote_provider_preferences` | All `r4.a.*` switches (behavior recording, thumbnail fuzzy classification and timestamp, auto-start buried points, application lock boot count, `security_adb_install_enable`, etc.) | `com/miui/common/persistence/RemoteProvider.java` `onCreate()`；`r4/a.java` |
| SP | `device_owner_shared_preferences` | DeviceOwner change history | `permcenter/DeviceOwnerChangedReceiver.java:102` |
| SP | `privacy_sdk` | Privacy CTA Throttle time (`<policy>_privacy_query_time` / `_privacy_update_time`) | `ue/i.java:24` |
| SP | `credential_service_config` | `is_credential_configured` | `com/miui/securitycenter/Application.java:542,547` |
| **file** | `files/privacypolicy/<policyName>/{privacy_version, privacy_update, privacy_temp_update_version, privacy_agree_error}` | Privacy policy version and failure scene (plain text JSON/string) | `ue/a.java:63-77,132-144`（`d()/g()/i()`） |
| **file** | `/data/misc/user/<userId>/securitycenter/across_terminal.xml` | Cross-terminal permissions (XML v1) | `permissions/acrossterminal/b.java:28-31` |
| **Settings.System** | `miui_recents_privacy_thumbnail_blur` | List of privacy-obfuscated package names (comma separated) | `ec/c.java:70,81,100,115,213,325,334` |
| Settings.Secure | `privacy_thumbnail_blur_history`, `miui_recents_privacy_thumbnail_blur` (copy), `miui_home_recents_blur_mode` | Fuzzy boot/mode | `ec/c.java:49,217,228,255,276` |
| Settings.Secure | `is_privacy_apps_enable`、`is_privacy_apps_password_enable`、`privacy_apps_shield_message_enable`、`miui_privacy_input_{pkg_name,enabled,supported}`、`PERMISSION_USE_WARNING`、`applock_unlock_mode`、`applock_countDownTimer_deadline` | Privacy space/input method/permission prompt/app lock | `se/a.java:29-46`；`x4/k1.java`；`mc/c.java:229,503`；`c3/f.java:535,624` |
| Settings.Global | `key_app_behavior_record_enable` | Behavior recording master switch | `PermissionManager.java:390`；`PermissionContract.java:12` |
| **Remote Provider** | `content://com.lbe.security.miui.permmgr` (+ table `active`/`miui_permission_record_info`/`miui_permission_description_info`) | Permission status matrix (userAccept/userReject/userPrompt/userForeground/suggest*/forcedBits/permMask), permission usage records, permission description in multiple languages | `PermissionContract.java:19-42,150-162` |
| Remote Provider | `content://com.lbe.security.miui.storagepolicy` | storage strategy | `StoragePolicyContract.java:12,34` |
| **system_server** | `SecurityManager` | App lock password/type, protected package, notification mask, private app collection, `exemptTemporarily` ticket | §4.12/§4.13 |
| In-process | `privacy_thumbnail_blur_category` (JSON saved by remoteprovider) | App returned from the cloud→category mapping | `ec/c.java:209,53-57` |

> **did not find any `SQLiteOpenHelper` / `openOrCreateDatabase` in** and APK (zero hits in grep within the module range) - the permission data is all on the external package or system side. **observes**.

---

## 7. Cloud interface summary

### 7.1 Privacy Policy Agree (CTA SDK, `ue` package)

| endpoint | method | trigger process | Request body (`JSONObject`, all fields are String) | Evidence |
|---|---|---|---|---|
| `https://data.sec.miui.com/privacy/agree/v1` | POST | The user "agrees" to a privacy policy / enters the consent page for the first time | `pkg, policyName, idContent, miuiVersion, apkVersion, policyVersion, language, region, timestamp` | `ue/d.java:27-50` |
| `https://data.sec.miui.com/privacy/revoke/v1` | POST | User "Withdraw Consent" | Same as above + **`idStatus="1"`** | `ue/g.java:22-63` (`idStatus` see `:31`) |
| `https://data.sec.miui.com/privacy/get/v1` | POST | Requires "Pull agreed policy details/latest content" | `pkg, policyName, timestamp` | `ue/h.java:44-90` |
| `https://data.sec.miui.com/privacy/latestVersion` | POST | Query the latest version number of the strategy (do version comparison) | `pkg, policyName, timestamp` | `ue/f.java:33-58` |

Public details (observations):
- **HTTP header**: `sign` (`MD5( Base64( key + sortedParams_k=v... + key ) )` in uppercase, key hard-coded `2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9`), `timestamp`, `source: sdk` (`ue/b.java:246-267`, `:136-153`)
- **Content-Type**：`application/json; charset=UTF-8`（`ue/b.java:74-82`）
- **return value convention**: `code==200 && msg=="success"` is successful; `data.latestPolicyVersion` (agree)/`data.version` (get)/`data` (latestVersion) file
- **error code**: `-2` network failure, `-3` analysis/service failure, `-4` **international version short circuit**, `-5` 24h throttling hit, `-6` Still need to update after agreeing, `-7` query is the latest (`ue/e.java:20-90`)
- **caller (observation)**: Security center main process `MainFragment.java:397,672`, `SettingsFragment.java:201`, `SettingsPrivacyActivity.java:196`, `BaseAcquireCtaActivity.java:28`, `eg/o.java:100`; and earthquake warning `earthquakewarning/*`, charging protection `electricrisk/*`, Xunyou `l7/b.java`, disaster warning `WarningCenterDisasterActivity.java:352`
- **international version alternative endpoint** (not in the `ue` package, but belongs to another flow): `api-flow-intl.10046.xiaomimobile.com/privacy/{agree,withdraw_agree,query_agree_info}` (`findings/dex_urls.txt`)
- **Privacy Policy Webpage**: `https://privacy.mi.com/security/`, `/security-pad/`, `/security-pad-global/`, `/all/`, `/earthquake_monitor/`, `/emergency_location/`, `/AIalert/%s`

### 7.2 Privacy risk detection/fuzzy thumbnail recommendation (`ac` package)

| endpoint | method | trigger | Request | response usage | Evidence |
|---|---|---|---|---|---|
| `https://flash.sec.miui.com/user/ra` | POST | Privacy risk detection "risk application" item (`ub.d`) and fuzzy automatic activation (`ec.c.c()` call chain) | `{"apps":{pkg:versionCode}, "type":.., "timestamp":.., "os":"MIUI_<incremental>", "device":<ro.product.device>, "biz_id":"rivacy_risk_check"}` | Return to `{pkg: ...}` collection → Intersect with local desktop application → Risk application list | `ac/c.java:37,43-57,77-90` |
| `https://flash.sec.miui.com/user/cat` | POST | Thumbnail blurry "Recommended by Month" (`ec.c.w()`, 30 days throttling) | `{"apps":[pkg,...], same metafield as above}` | Return to `data` (JSON: pkg→category) → Save `privacy_thumbnail_blur_category` → Automatically turn on blur | `ac/c.java:36,60-71,93-107`；`ec/c.java:279-315` |

Transport layer (observation): `AES/ECB/PKCS5Padding` (16 bytes `SecretKeySpec` generated by key = `SHA1(ConfigManager.getSecurityHttpKey())`) → Base64 → **GZIP** → POST, `Content-Type: text/xml` (`ac/a.java:63-76`, `ac/c.java:118-160`). Access control: `x.z()`.
> **Privacy Meaning**: This interface will upload the list of application package names **(`/user/ra` also includes the version number) visible on the desktop of user** to the Xiaomi server.

### 7.3 Other related

- `https://data.sec.miui.com/data/upload`, `/adv/tail`, `/data/chargeV2`, `/adv/game_ad/report` — burying points/advertising (non-permission module)
- `https://api.miui.security.xiaomi.com/*` — Security center master API (not the core of this module)
- Cloud control feature switch: `RemoteProvider.call("isFeatureSupport"/"getFeatureStatus"/"setFeatureStatus", ...)` → `com.miui.common.d`

---

## 8. Key projects: direct answers

### Q1 `permcenter` Each sub-contains one sentence

| subpackage | a word |
|---|---|
| `permissions` | Per-application/per-permission authorization UI + system permission declaration pop-up window + permission usage prompt; write to lbe permmgr and AOSP PackageManager via `g4.b` |
| `autostart` | Self-start switch, write `PERM_ID_AUTOSTART(16384)` (permmgr method 6, multi-user with `<uid>@` prefix) |
| `detection` | Privacy risk detection: 8 types of inspection items (sensitive/dangerous permission counts, cloud risk applications, thumbnail blur, photo deletion protection, input method leakage risk) + 3 fixed recommendation cards |
| `privacyblur` | Only write `Settings.System.miui_recents_privacy_thumbnail_blur` package name whitelist; render in `com.miui.home` |
| `provision` | Additional privacy notice (permissions × privacy policy itemized confirmation, Kotlin + `miuix.provision`) |
| `capsule` | Smart capsule landing page + "Stop sharing/Cancel recording" pop-up window for high-risk screen sharing applications (`OP_CAN_BE_RECORDED`) |
| `settings` | Privacy settings aggregation: other permissions, privacy input mode, shutdown password, stealth mode, capsule settings, QS Tile springboard |
| `untrustwarn` | **empty package** (the manifest declares `UnknownSourceWarnActivity`, DEX without class); the true unknown source is checked in `install` |
| `acrossterminal` | Cross-end permissions, fall into `/data/misc/user/<uid>/securitycenter/across_terminal.xml`, provider `com.miui.permissions.acrossterminal` |
| `service` | `InvisibleModeService`: Do `setUserRestrictionForUser` for camera/microphone/positioning + hide status bar stealth icon |

### Q2 What items does `PrivacyRiskDetectionActivity` detect and the basis for judgment?
See §4.3 table. Core judgment formula:
- Item 11/12 = `count{app | permissionToAction[PERM_ID] == 3 (always allowed)}`, PERM_ID ∈ {32 location, 8/READCONTACT, 16/READCALLLOG, 131072 clipboard, 35184372088832 external storage, -3 Media Vision (T+), -2 Media Hearing (T+)} (`mc/c.java:358-379`, `mc/c.java:110-121`, `PrivacyRiskDetectionActivity.java:237-258`)
- Item 13 = Cloud risk package name collection ∩ Local desktop application (`ub/d.java:52-72`)
- Item 14 = `persist.sys.protect_image`
- Item 16 = `df.h.J(df.h.m(ctx))` && `vf.n.a(ctx)`
- Item 22 = An item exists in the cloud app classification map, is not in the user shutdown list, and `fc.c.b(category)` is true
- The number of auxiliary items (`ub/b`) DeviceAdmin/Notification Listener/UsageStats is only used as a supplementary count for the "Dangerous Permissions" card

### Q3 `UnknownSourceWarnActivity` interception logic
Unable to answer - class **does not exist** (§4.8). Replacements: `PackageVerificationReceiver` (pre-installation signature/source verification + `IMessenger` receipt), `AdbInstallActivity`/`AdbInstallVerifyActivity` (ADB installation confirmation), `UnknownSourceVerifyActivity` (`com.miui.securitycenter.action.UNKNOWN_SOURCE_VERIFY`, UI page).

### Q4 `applicationlock` implementation and security boundaries
See §4.12. Three sentences: **UI In this App, the password and ticket are in system_server**; successful unlocking depends on `SecurityManager.addAccessControlPass(pkg)`; if you forget the password, use the Xiaomi account (`IXiaomiAccountService`) instead of the local backdoor.

### Q5 `privacyapps` storage isolation and entrance camouflage
- Isolation = **Android multi-user profile + `SecurityManager`’s privacy application collection + hidden processing of desktop/task/notification**; no file encryption or independent mounting is seen.
- `com.miui.securitycenter.app.hide` This authority **does not provide the hide capability**, but only provides `isPrivacyApp` query (§4.13 A).

### Q6 `blur` — `PRIVACY_THUMBNAIL_BLUR_SETTING` What is the specific blur and where is the algorithm?
- Blurred object: **task thumbnail** in Desktop Recents (if the package name is in the whitelist, it will be blurred/occluded); V2 supports two modes: "dim" and "blur".
- Algorithm: **is not APK**. This App only writes `Settings.System."miui_recents_privacy_thumbnail_blur"`; `com.miui.blur` is irrelevant to the background blur SDK (RenderScript `ScriptIntrinsicBlur` + SurfaceFlinger synthesis sampling).

### Q7 `idprovider` — OAID Reading and Authorization
See §4.15. Key points: `OP_GET_OAID(10037)` / `OP_GET_OAID_USER(10038)`; pull up `REQUEST_PERMISSIONS_FOR_OAID` when `MODE_ASK=5`; the result is written back to the mode of the two ops and `setResult`.

### Q8 `appmanager` — Batch uninstall/freeze/permission entry
See §4.16. Batch uninstall = `IPackageManager.deletePackageAsUser`; "Freeze/Deactivate" = `PackageManager.setApplicationEnabledSetting`; Permission entry = Jump `APP_PERM_EDITOR`; Permission statistics are not in this package (in `privacycenter/usage`).

---

## 9. Unsolved mysteries/need dynamic or cross-image verification

1. **`dynamic`: `com.lbe.security.miui` internally implements** (the real person of `IPermissionManager`, the AppOps write entry table, and the actual writer of `miui_permission_record_info`) - this APK is just a client, and the LBE package needs to be analyzed or system_server.
2. **`dynamic`: `miui.security.SecurityManager`'s method table** (`checkAccessControlPassword`'s hash algorithm, `addAccessControlPass` ticket validity, `exemptTemporarily` semantics) - the framework code is not in this APK.
3. **`unknown`: `untrustwarn.UnknownSourceWarnActivity` / `privacymanager.XSpaceWarningDialogActivity` is still pulled up by a certain path in the system to**. Manifest exists but no class; static cannot negate "System side will still explicitly start → ActivityNotFound".
4. **`unknown`: `android:sharedUserId` and signature** - not verified (`inspect_apk_signature` not run / manifest root node attribute not read).
5. **`unknown`: Real storage isolation** for privacy space. This APK side only has "application collection + hidden processing"; if MIUI performs data isolation on privacy applications, the implementation should be in the system_server / `com.miui.home` / storage layer.
6. **`unknown`: The complete method table of `IdProvider` (31 KB) and the OAID generation algorithm** (whether `UUID.randomUUID()`, resettable, AAID/VAID processing).
7. **`unknown`: `OAID` authorizes writing the semantics of mode** (`allow → 1`, `deny → 0`, opposite to AOSP `MODE_ALLOWED=0`). It may be a non-standard mode table of lbe's custom op, which needs to be dynamically verified.
8. **`unknown`: The storage** of `ShutDownPasswordFragment` (suspected to reuse `SecurityManager.setAccessControlPassword`, but no explicit call point is seen).
9. **`partial`: `permcenter/q.java`(29 KB), `permcenter/l.java`(33 KB), `permcenter/n.java`(57 KB)` and other obfuscation tool classes are not expanded one by one** (`n` has been confirmed to contain `y(ctx,boolean,List<Long>,xc.c)` Permission batch query).
10. **`partial`: `appmanager/fragment/ApplicationsDetailsFragment`(135 KB)/`ManageFragment`(76 KB), `permcenter/permissions/PermissionAppsModifyFragment`(88 KB), `settings/OtherPermissionsFragment`(46 KB) only sampled the entrance and key calls, and did not cover** method by method.

---

## 10. Evidence Index (by Topic)

**permission data source / contract**
- `com/miui/permission/PermissionContract.java:10,15,16,17`（authority / URI）
- `com/miui/permission/PermissionContract.java:44-61` (method code)
- `com/miui/permission/PermissionContract.java:19-42` (`active` list)
- `com/miui/permission/PermissionContract.java:150-162` (`miui_permission_record_info` list)
- `com/miui/permission/PermissionManager.java:24-31,43-68,69-136`（ACTION/GROUP/PERM_ID）
- `com/miui/permission/PermissionManager.java:170-191` (virtualMap / static initialization)
- `com/miui/permission/PermissionManager.java:293-306,436-455,522-524` (three write entries)
- `com/miui/permission/StoragePolicyContract.java:12,21,29,34`
- `com/miui/permcenter/compact/AppOpsUtilsCompat.java` (full text)
- `android/app/AppOpsManagerCompat.java` (full text, op table)

**write / double write**
- `g4/b.java:320-367` (group permission write + store special branch)
- `g4/b.java:369-417` (single permission write + grant/revoke reflection)
- `com/miui/permcenter/compact/PermissionManagerCompat.java:13-19` (virtual permission)
- `permcenter/autostart/AutoStartManagementActivity.java:403-428`

**Privacy risk detection**
- `permcenter/detection/PrivacyRiskDetectionActivity.java:237-258,712-721,752-773,834-888,388-492`
- `permcenter/detection/model/d.java:60-125` (item structure and copy key)
- `ub/b.java:52-160`、`ub/c.java`、`ub/d.java:52-72`、`ub/e.java`、`ub/a.java`
- `mc/c.java:110-121,358-379,388-500`
- `uc/b.java:20-40`

**Privacy blur**
- `ec/c.java` (full text, including `Settings.System.miui_recents_privacy_thumbnail_blur`)
- `permcenter/privacyblur/PrivacyThumbnailBlurSettings.java:67-76,306-310,334-338,400-422`
- `permcenter/privacyblur/a.java`（adapter）
- `ub/c.java`（`persist.sys.protect_image`）

**Application Lock**
- `applicationlock/ConfirmAccessControl.java:1368,1373,1991-1993,2485-2530,999`
- `applicationlock/widget/q.java:118-136`
- `c3/f.java:250,391-396,435,478,547,138-153,214-222,513,535,612,624,665`
- `c3/d.java:88-96,155-165,236-240`
- `applicationlock/ChooseAccessControl.java:502,509`
- `applicationlock/ConfirmAccountActivity.java:174-177`
- `c3/w.java:60-80`
- `applicationlock/f.java:230`

**Privacy application**
- `privacyapps/PrivacyAppsProvider.java:39-56,79`
- `se/a.java:29-66`、`se/c.java:33-97`、`se/e.java:54-176`
- `pe/a.java:7`
- `privacyapps/ui/PrivacyAppsHelper.java:30-46`、`privacyapps/ui/PrivacyAppsActivity.java:135-160,192-334`

**OAID**
- `idprovider/ui/GrantPermissionActivity.java:66-99,118-186`
- `permcenter/compact/AppOpsUtilsCompat.java`（`isSupportOAIDApps`）

**cross-port**
- `permcenter/permissions/acrossterminal/b.java:28-31,56-102,104-168`
- `permcenter/permissions/acrossterminal/AcrossTerminalPermissionContentProvider.java` (`call()` full text)
- `dc/d.java:29-77`

**installation verification / unknown source**
- `permcenter/install/PackageVerificationReceiver.java:40-53,116-127,168-208,213-221`
- `permcenter/install/AdbInstallActivity.java:176-188`
- `com/miui/common/persistence/RemoteProvider.java:174-190`

**Stealth mode/Privacy input/Screen protection**
- `permcenter/service/InvisibleModeService.java:37,41-59,63-96,97-120`
- `permcenter/settings/PrivacyInputModeFragment.java:117-182,323,353`
- `x4/k1.java` (privacy input setting key)
- `permcenter/capsule/ScreenShareProtectionActivity.java:60-78,96-101`

**blur**
- `com/miui/blur/sdk/backdrop/BlurManager.java:84-85,116-140`
- `com/miui/blur/sdk/backdrop/p.java:127-129,174-176,388-392`
- `com/miui/blur/sdk/backdrop/BlurDrawInfo.java`

**Cloud**
- `ue/b.java:74-82,136-153,246-267` (HTTP + signature)
- `ue/d.java`、`ue/g.java`、`ue/f.java`、`ue/h.java`、`ue/e.java:20-90`、`ue/i.java`、`ue/a.java`、`ue/c.java`
- `ac/a.java:63-76`、`ac/c.java:36-37,43-107,118-160`
- `findings/dex_urls.txt`（`privacy.mi.com/*`、`api-flow-intl.*`）

**App manages**
- `appmanager/AppManageUtils.java:124,141,861,880,906-916,930-955,985-1008,1099,1109,1317`
- `appmanager/fragment/AMSettingsFragment.java:161`、`fragment/ApplicationsDetailsFragment.java:720`

**List**
- `_analysis/apktool_sc/AndroidManifest.xml` (`exported`/`permission`/`intent-filter` of all `activity/service/provider/receiver/activity-alias`; see §2.2 Note for `activity-alias` targets)
