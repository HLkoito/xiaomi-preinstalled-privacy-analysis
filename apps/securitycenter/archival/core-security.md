> **Historical analysis.** This archive includes observations that remain unverified or were later corrected. See the [final report](../REPORT.md) and [independent review](../REVIEW.md) for the reviewed findings on upload scope, consent, local FBO delegation, emergency SMS encryption, and provider authorization. Source references retain their original decompilation paths. The publication includes the selected files under evidence/.

# MIUI Security Center - Reverse Analysis of "Security and Interception" Business Module

> Target: `MIUISecurityCenter-10.4.9-250402.0.1.apk` (SHA-256 `fc63de0e…78cfd`)
> Analyst: Team Lead `lead`’s teammate **core-security**
> Evidence baseline: `_analysis/jadxout/sources/` (10624.java) + `_analysis/apktool_sc/` (Manifest/res) + `_analysis/apk_full/assets/`
> Annotation convention: **[Observed]**= shipped artifact directly readable fact; **[Inferred]**= forced by code structure, not dynamically verified; **[Unknown]**= cannot be determined from existing materials

---

## 0. A quick overview of the global structure

### 0.1 Process Model[Observed]

There are 3 processes in `AndroidManifest.xml` (statistical command: `Select-String android:process` → 100/6/3 times):

| process | Purpose | Number of key components |
|---|---|---|
| Main process `com.miui.securitycenter` | All UI (293 Activities, the vast majority), securityscan homepage | — |
| `com.miui.securitycenter:ui` | 6 components (UI isolation such as `of.e.r()`) | 6 |
| `com.miui.securitycenter.remote` | **resident background process**: antivirus/antispam/luckymoney/policeassist/tvm/antifraud/RemoteProvider/NotificationService/RemoteService All here | 100 |
| `com.miui.securitycenter.bootaware` | DirectBoot (before FBE is unlocked) available: `com.miui.simlock.*` | 3 |

[Observed]`com.miui.securitycenter.Application#attachBaseContext` uses `x4.f1.u(context)` to get the current process name, and assigns 3 completely different startup paths accordingly (`Application.java:683-689`, `710-842`):

| process name | Take the branch | Things to do (excerpt) |
|---|---|---|
| `com.miui.securitycenter` (main) | `onCreate:717-729` | `AnalyticsUtil.initMiStats`、`registerActivityLifecycleCallbacks`、`TmBinderCacher.initForUIProcess` |
| `com.miui.securitycenter:ui` | `onCreate:730-734` | Only do `initMiStats` + `of.e.r()` |
| `com.miui.securitycenter.remote` | `onCreate:735-841` | **initializes all background capabilities** (see below) |

[Observed] Initialization list of remote process `onCreate` (`Application.java:735-841`) - This is the panoramic view of the "resident daemon process" of APK:

```
735  if ("com.miui.securitycenter.remote".equals(this.f20261f)) {
739 v(); // delete /MIUI/debug_log/com.miui.privacy
742 L(); // Enable super power saving Tile
743      w.E();
744      l.a(this);
745 B(this); //Register miui.intent.action.FIREWALL_UPDATED
746 C(this); // xf.b whitelist/blacklist initialization
747      G(this);   // monthreport + push
749-751  ConnectivityChangeJobService2.s / c3.d.g / c3.d.c
758      v.m(this, new mf.a(), CONNECTIVITY_CHANGE + action_update_sc_network_allow)
764      v.m(this, new i3.b(), ACTION_USER_PRESENT)
767      AppOpsUtilsCompat.autoOptmize(this)
769      k.a().b(this)
788-800 df.h.c/E + registerContentObserver(default_input_method) //Input method security
801 j.d(this, "application") // Application installation monitoring
802 d0(this) // Notification permission is synchronized to PermissionManager(32768L)
808 Z() // BehaviorWidget switch
809      ef.a0.a(this)
816 Looper.myQueue().addIdleHandler(→ K()) // ★ Initialize "heavy" business when idle
820-831  ...
```

`K()` (`Application.java:361-410`) runs in **IdleHandler**, and the sequence is the "heavyweight business list" ([Observed]):

```
364 VirusAutoUpdateJobService.d(this); // Virus database updated daily Job(99)
367  M(this);      → VideoToolBoxService
368 LuckyMoneyHelper.init(this); // ★ Red envelope assistant
369-373 v.w(FirewallService / TrafficManageService / MiuiVpnManageService)
375-377 WarningCenterDisasterService (not international version and push enabled)
384-391 EarthquakeWarningService (when earthquake warning is turned on)
```

### 0.2 External capabilities declared by APK (components within the scope of this document) [Observed]

| components | Type | exported | permission | process | Manifest line |
|---|---|---|---|---|---|
| `com.miui.antivirus.service.GuardService` | service | false | — | remote | 835 |
| `com.miui.antivirus.service.VirusAutoUpdateJobService` | service(Job) | false | BIND_JOB_SERVICE | remote | 870 |
| `com.miui.antispam.service.AntiSpamService` | service | **true** | `com.miui.antispam.permission.DATA_MONTH_REPORT` | remote | 1193 |
| `com.miui.antispam.db.AntiSpamProvider` | provider `antispam` | **true** | `com.miui.antispam.permission.CONTENTPROVIDER` | remote | 1194 |
| `com.miui.riskcontrol.SensitiveWordDetectProvider` | provider `com.miui.riskcontrol` | **true** | `com.miui.security.permission.DETECT_TEXT` | (main) | 1733 |
| `com.miui.luckymoney.service.LuckyMoneyMonitorService` | service | false | — | remote | 1126 |
| `com.miui.luckymoney.provider.LuckyMoneyProvider` | provider `com.miui.luckymoney.provider` | **true** | `miui.permission.EXTRA_NETWORK` | remote | 1127 |
| `com.miui.warningcenter.policeassist.PaService` | service | false | — | remote | 475 |
| `com.miui.tvm.service.TvmManagerService` | service | **true**（enabled=false） | `miui.permission.TVM` | (main) | 476 |
| `com.miui.antifraud.AntiFraudReceiver` | receiver | **true** | `android.permission.MODIFY_PHONE_STATE` | remote | 433 |
| `com.miui.antifraud.PayActivityMonitorService` | service | false | — | remote | 432 |
| `com.miui.securitycenter.service.RemoteService` | service | false | — | remote | 1724 |
| `com.miui.securitycenter.service.NotificationService` | service | **true** | `…permission.NOTIFICATION_SERVICE` | remote | 1725 |
| `com.miui.securitycenter.receiver.BootReceiver` | receiver | false | — | remote | 890 |
| `com.miui.common.persistence.RemoteProvider` | provider `com.miui.securitycenter.remoteprovider` | **true** | `…permission.ACCESS_SECURITY_CENTER_PROVIDER` | remote | (flatten table) |
| `com.xiaomi.security.xsof.XSOFProvider` | provider | **true** | — | remote | 1758 |
| `com.xiaomi.security.xsof.MiSafetyDetectService` | service | **true** | — | remote | 1759 |

External Action entrance (`<intent-filter>`, for system/desktop/other apps to call) [Observed]:
`miui.intent.action.ANTI_VIRUS`、`miui.intent.action.ANTI_VIRUS_WHITELIST`、`com.miui.securitycenter.action.ANTIVIRUS_SETTINGS`、`miui.intent.action.SAFE_PAY_MONITOR_SETTINGS`、`miui.intent.action.SET_FIREWALL`、`miui.intent.action.CALL_FIREWALL`、`miui.intent.action.VIEW_BLACKLIST`、`miui.intent.action.ADD_FIREWALL`、`miui.intent.action.REMOVE_BLACKLIST`、`com.miui.antispam.action.ANTISPAM_SETTINGS`、`miui.intent.action.HB_MAIN_ACTIVITY`、`miui.intent.action.LUCKY_MORE_SETTING`、`miui.intent.action.LUCKY_ALARM_SETTING`、`miui.intent.action.WARNINGCENTER_POLICE_ASSIST`、`miui.intent.action.START_TVM`。

### 0.3 Cross-process preferences (throughout all modules) [Observed]

`r4.a` (`r4/a.java`) **is not** ordinary SharedPreferences, but read and write across processes through ContentProvider `call()`:

```java
// r4/a.java:19-45
private static Bundle a(String str, Bundle bundle) {
    ...
    return f34090a.call(uriBuild, "callPreference", str, bundle);
}

// r4/a.java:151-158  SET
Bundle b = ...; b.putInt("type", 1); b.putString("key", str); b.putBoolean("p", z10);
a("SET", b);
f34090a.notifyChange(Uri.withAppendedPath(Uri.parse("content://com.miui.securitycenter.remoteprovider"), str), null, false);
```

Server `com.miui.common.persistence.RemoteProvider` (`RemoteProvider.java:264`):

```java
public boolean onCreate() {
    this.f12718a = getContext().getSharedPreferences("remote_provider_preferences", 0);
    if (!i()) return true;    // i() = !containsKey("key_default_preferences_saved")
    j();                      // First time: Move in all PreferenceManager.getDefaultSharedPreferences
    return true;
}
```

- [Observed] There is only one preference file that is truly shared between **modules: `remote_provider_preferences`** (in the `com.miui.securitycenter.remote` process). Most of the switches, timestamps, and package name lists of `Application` / `antivirus` / `antispam` / `securityscan` use it.
- [Observed]`type` 0=int / 1=boolean / 2=String / 3=float / 4=long / 5=StringSet; supports `GET` / `SET` / `CONTAIN_KEY` three methods (`RemoteProvider.java:40-178`).
- [Observed] There is a mandatory security check: when writing `security_adb_install_enable=true`, use `SecurityManager#getPackageNameByPid` to reversely check the caller. Only this package or `com.miui.klo.bugreport` (`RemoteProvider.java:180-194`) is allowed.
- [Observed] The same Provider also has three methods `isFeatureSupport` / `getFeatureStatus` / `setFeatureStatus` (`RemoteProvider.java:232-240`).

---

## 1. `com.miui.securitycenter`——Main Application/Resident Service/Boot Broadcast

### 1.1 Function list

| Function | Entry class | Core classes·methods | Data | Cloud | Evidence |
|---|---|---|---|---|---|
| Application process dispatch (3 processes, 3 sets of initialization) | `Application#onCreate` | `Application#attachBaseContext / onCreate` | `remote_provider_preferences` | — | `Application.java:683-842`[Observed] |
| Resident front desk service (keep alive) | Manifest:1724 | `RemoteService#onCreate / onStartCommand` | `Notification` channel `securitycenter_resident_notification_hide` | — | `RemoteService.java:57-117`[Observed] |
| Status bar "memory/cleanup" resident notification | Manifest:1725 | `NotificationService`（23.5 KB） | `t.m()` total memory | — | `NotificationService.java:47-90`[Observed] |
| Boot broadcast | Manifest:890 | `BootReceiver#onReceive` | — | — | `BootReceiver.java:80-118`[Observed] |
| Cloud data update (shrink the screen to clean the anti-kill list) | Manifest:824 | `CloudDataUpdateService#a` | `pref ver` + `items` | `POST https://api.sec.miui.com/trashCleaner/screenLock` | `CloudDataUpdateService.java:30-60`[Observed] |
| Game automatic scan job | Manifest:823 | `AutoScanGameJobService`（7.6 KB） | — | — | — |
| Privacy/Credentials Provider | Manifest:1758/1759, flatten table | `XSOFProvider#call`、`MiSafetyDetectService` | — | — | `XSOFProvider.java:25-32`[Observed] |
| Widget | `WidgetProvider`（16.7 KB） | — | — | — | File name[Observed] |

### 1.2 `RemoteService`: "Pin" the remote process into the foreground [Observed]

```java
// RemoteService.java:38-49
private void a() {
    Object objH = f.h(Class.forName("android.app.ActivityManagerNative"), "getDefault", null, new Object[0]);
    if (Build.VERSION.SDK_INT >= 26) {
        f.d(objH, "setProcessImportant", new Class[]{IBinder.class, Integer.TYPE, Boolean.TYPE, String.class},
            this.f20418b, Integer.valueOf(Process.myPid()), Boolean.TRUE, StatConstants.Channel.SECURITYCENTER);
    } else {
        f.d(objH, "setProcessForeground", ...);   // Reflection call hidden API
    }
}
```

`cmd` branch of `onStartCommand` (`RemoteService.java:95-115`):

| cmd | behavior |
|---|---|
| `recommend_app_installed` | `e.p(this)` (recommended for application management) |
| `app_lock` | `e.j(intent, this)` (Application Lock) |
| `competitive_app_installed` | `e.n(this, "000015")` |
| `app_installed_scan` | `e.m(this)` |
| `show_virus_notification` | `n.f20925f.a().t(this, (ArrayList) intent.getSerializableExtra("key_virus_pkg_list"))` ← **Virus Result Notification Portal** |
| `hide_virus_notification` | `n.f20925f.a().f(this)` |

`dump()` can also output build configuration (`FLAVOR : cnPhone` / `IS_FLAVOR_REGION_CN : true`) and `PackageVerificationReceiver` status - `RemoteService.java:51-71`[Observed].

### 1.3 `BootReceiver`: What does it do when booting up [Observed]

`BootReceiver.java:81-118` sequence execution:

1. `z1.e() != z1.g(context)` → Reset some switches when crossing users/users (`x.W/x.E/t.R("com.android.settings",0,false)`)
2. `r1.a(context)`, `j.d(context)`, `c3.f.m0/C0` (statistics)
3. **`SecurityManager#startServiceAsUser(AntiSpamService, CURRENT_OR_SELF)`** ← Reflection pull up interception service
4. `TrafficManageService` start
5. `fe.f.b(0L)`, `b(context)=jd.a.b/c`, `d(context)` (`PowerSaveService` issued `TRY_CLOSE_SAVE_MODE`), `c(context)=g.l`, `a()` (first start timestamp)
6. `e.r(context)` and `e(context)`:
   - `e()` → `SecurityManager#updateLauncherPackageNames` (reflection)
7. When `t.h()==9`, write notification switches to `PermissionManager.setApplicationPermission(32768L, …)` (`BootReceiver.java:31,105-115`) one by one for `{com.miui.player, com.miui.fm, com.android.soundrecorder, com.android.browser, com.miui.voiceassist, com.mipay.wallet}`
8. `com.miui.common.base.asyn.a.a(new a(context))` → `le.b.a/b` (non-international version)

### 1.4 Persistence

| carrier | Name | content | Evidence |
|---|---|---|---|
| SharedPreferences | `remote_provider_preferences` | Full APK cross-process shared preferences (including all safety switches) | `RemoteProvider.java:264`[Observed] |
| SharedPreferences | `credential_service_config` | `is_credential_configured` | `Application.java:542-547`[Observed] |
| SharedPreferences | `SmsEnginePreference` | SMS engine version/time (see §4) | `j2/a.java:316`[Observed] |
| Settings.Secure | `security_adb_install_enable`、`accessibility_button_targets`、`power_supersave_tile_enabled`、`credential_service*`、`default_input_method` | System Level Switch | Many places[Observed] |
| File | `files/scanitem.json_v{0,1}`、`files/resource.zip`、`files/SmsEngineResource/` | See §2/§4 | `ModelFactory.java:88-95`、`j2/a.java:177`[Observed] |
| Provider | `content://com.miui.securitycenter.remoteprovider` | See 0.3 | [Observed] |

---

## 2. `com.miui.securityscan` ——Home page "One-click physical examination"

(133 .java, 987.6 KB; `MainFragment.java` single file 153 KB)

### 2.1 Function list

| Function | entrance | Core classes·methods | key data | Cloud | Evidence |
|---|---|---|---|---|---|
| Home + one-click physical examination | `MainEntryActivity` (**Manifest:238**, `enabled=false`, export; intent-filter 4 groups: `android.intent.action.MAIN`, `mi.quicksearch.intent.action.RETRIEVE`, `miui.intent.action.SECURITY_CENTER`, `VIEW securitycenter://home:8888/mainActivity` — Manifest:239-256) → Actual UI `com.miui.securityscan.MainActivity` (**Manifest:546**) | `MainFragment`（153 KB） | `assets/scanitem.json` | None | [Observed] |
| Scanning schedule | `k.n(context)` singleton | `com.miui.securityscan.scanner.k#w/z/v` | — | — | `scanner/k.java:840-960`[Observed] |
| Score | Same as above | `scanner.ScoreManager#o/q/i` | — | — | `ScoreManager.java:167-220,445-509`[Observed] |
| Scheduled/background scan | Manifest:1590 `ScanJobService` | `ScanJobService.c(context)` | — | — | `Application.java:178`[Observed] |
| Physical examination items (two groups) | `ModelFactory#produceManualGroupModel / produceSystemGroupModel` | `AbsModel#scan/optimize` | `scanitem.json` | — | `ModelFactory.java:213-229`[Observed] |
| First aid kit (another checklist) | `ModelFactory#produceFirstAidKitGroupModel` | Same as above | `assets/firstaid_scanitem.json` (Group 5: Performance/Internet/Operation/ConsumePower/Other) | — | `ModelFactory.java:174-202`[Observed] |
| Desktop shortcuts (cleaning/network diagnostics/game acceleration) | `shortcut.ShortcutActivity`（11.1 KB） | `shortcut.d#q/v` | Enumeration `shortcut.d.b`: `CLEANMASTER(2)/NETWORK_DIAGNOSTICS(12)/GAME_TURBO(14)` + more | — | `shortcut/d.java:102-116`[Observed] |
| Picture privacy protection (album deletion interception) | `fileobserver.ImageProtectService`（23.8 KB）/ `DelImgInterceptActivity`（22.9 KB） | — | — | — | File name[Observed] |
| Settings / Whitelist / About | `ui/settings/*` | `SettingsFragment`（27.2 KB）、`WhiteListActivity` | — | — | [Observed] |
| Results page | `OptimizeAndResultActivity`（50.3 KB） | — | — | — | [Observed] |

### 2.2 Full list of "one-click physical examination" inspection items - directly from `assets/scanitem.json`

`scanitem.json` has three top keys: `manual` (28 groups), `system` (8 groups), and `version`. Each item has `key`/`name` (fully qualified name of the implementation class)/`version` (applicable channel alpha/development/stable)/`score` (point deduction weight).
[Observed] Loading logic (`ModelFactory.java:159-172`): `Class.forName(name).getConstructor(String.class, Integer.class).newInstance(key, score)` - **reflection instantiates**; `version` must contain the return value of `x4.a0.h()`, otherwise it will be skipped.

**manual (manual items, 28 groups; candidates for "Selective Display" in the same group, `GroupModel#scan` takes the first non-SAFE item)**

| # | key | weight | Implementation class | Candidates within the group |
|---|---|---|---|---|
| 0 | `VIRUS` | 0 | `model.system.VirusScanModel` | Single |
| 1 | `CLEANER_SHORTCUT` | 3 | `model.manualitem.CleanerShortcutModel` | Single |
| 2 | `DARK_MODEL` | 0 | `model.manualitem.DarkModel` | Single |
| 3 | `NETWORK_SWITCH` | 5 | `…NetworkSwitchModel` | 7 items in the same group: `VIRUS_CLOUDSCAN(0)`, `CLEANER_CLOUDSCAN(0)`, `GARBAGE_LIB(0)`, `FLOW_NOTIFICATION(0)`, `TELECOM_OPERAOTR(0)`, `AUTO_UPDATE(2)` |
| 4 | `GARBAGE_CLEAN` | 5 | `…GarbageCleanModel` | Single |
| 5 | `MI_SIM_LOCK` | 5 | `…MiSimLockModel` | Single |
| 6 | `DEFAULT_LAUNCHER` | 0 | `…defaultapp.DefaultLauncherModel` | 8 items in the same group: `DEFAULT_DIAL/MESSAGING/BROWSER/CAMERA/GALLERY/MUSIC/EMAIL` (all 0) |
| 7 | `DATA_FLOW` | 3 | `…DataPackageModel` | Single |
| 8 | `KEY_SYNC` | 3 | `…KeySyncModel` | Single |
| 9 | `PAYMENT_RISK` | 3 | `…PaymentRiskModel` | Single |
| 10 | `SAFE_MODE` | 3 | `…SafeModeModel` | Single |
| 11 | `FIND_DEVICE` | 3 | `…FindDeviceModel` | Single |
| 12 | `FLOW_VERIFY` | 0 | `…PackageVerifyModel` | Single |
| 13 | `FLOW_VERIFY_FAILED` | 0 | `…PackageVerifyFailedModel` | Single |
| 14 | `CLOUD_SERVICE` | 2 | `…CloudServiceModel` | Single |
| 15 | `MIFI_INSURANCE` | 0 | `…MifiInsuranceModel` | Single |
| 16 | `MIUI_UPDATE` | 5 | `…MiuiUpdaterModel` | Single |
| 17 | `FIRST_AID_KIT` | 0 | `…FirstAidModel` | Single |
| 18 | `PERMISSION_ROOT` | 0 | `…PermissionRootModel` | Single |
| 19 | `APP_UPDATE` | 0 | `…AppUpdateModel` | Single |
| 20 | `PERMANENT_NOTIFICATIONBAR` | 0 | `…SecurityNotificationModel` | Single |
| 21 | `FLOW_PURCHASE` | 0 | `…DataPurchaseModel` | Single |
| 22 | `CLOUD_SPACE` | 0 | `…CloudSpaceModel` | Single |
| 23 | `FLOW_RANK` | 0 | `…FlowRankModel` | Single |
| 24 | `MI_INPUTMETHOD` | 5 | `…MiInputMethodModel` | Single |
| 25 | `CONSUME_POWER_RANK` | 0 | `…ConsumePowerRankModel` | Single |
| 26 | `APP_LOCK` | 0 | `…AppLockModel` | Single |
| 27 | `SHUT_DOWN_PASSWORD` | 5 | `…ShutDownPasswordModel` | Single |

**system (system item, 8 groups)**

| # | key | weight | Implementation class |
|---|---|---|---|
| 0 | `VIRUS_LIB` | 5 | `model.system.VirusDbModel` |
| 1 | `SYSTEM_AUTO_DOWNLOAD` | 0 | `model.system.AutoDownloadModel` |
| 2 | `INSTALL_MONITOR` | 5 | `model.system.InstallMonitorModel` |
| 3 | `DEV_MODE` | 2 | `model.system.DevModeModel` |
| 4 | `USB` | 3 | `model.system.UsbModel` |
| 5 | `MMS` | 5 | `model.system.MmsModel` |
| 6 | `CHAIN_START` | 0 | `model.system.ChainStartModel` |
| 7 | `GMS_OFF` | 0 | `model.system.GmsModel` |

> [Observed]Another 6 model classes exist in the source code but **does not appear in scanitem.json**: `AppLockModel` (in), `AutoUpdateModel` (in), `DefaultAppModel`, `FlowRankModel` (in), `MiAccountModel`, `MiRoamingModel`, `PaymentRiskModel` (in)… Specific ones that did not appear: `model/manualitem/DefaultAppModel.java`, `MiAccountModel.java`, `MiRoamingModel.java`, `FirstAidModel` (in). See `model/ModelConstants.java` (0–66, including unenabled `MODEL_LIVE_TALK=25`, `MODEL_REALNAME_VERIFY=37`, etc.) for complete index constants. [Observation/Unknown] Those who are not in JSON can be regarded as **. The current version does not enable**, and the risk weight shall be based on JSON.

### 2.3 Scan orchestration: 5 concurrent pipelines [Observed]

`scanner/k.java` (the class name is confused as `k`, the log TAG is `SecurityManager`):

```java
// scanner/k.java:940-956
public void w(InterfaceC0264k cb, rf.i iVar, rf.c cVar, m mVar) {   // startScanAndOptimize
    this.f20833a = false;
    this.f20842j.a();                                     // Optimize queue reset
    this.f20847o.clear(); this.f20845m.a();
    this.f20847o.addAll(Arrays.asList(zf.d.values()));    // Optimization item enumeration
    this.f20839g.E();                                     // ScoreManager.clear
    A(false, iVar);   // 1) Manual item scanning → scanner/c.e(...)
    x(false, mVar);   // 2) Automatic item scanning → scanner/m.o(...)
    C(false, cVar, mVar, cb);  // 3) System application scan (or incremental scan)
    B(false, cVar, mVar);      // 4) Memory scan → scanner/d.n(...)
    y(mVar);          // 5) Cache scan → CacheCheckManager.c(...)
}
// scanner/k.java:829-838 - Step 3 branch
private void C(...) {
    if (o.f20935o.a().E()) {                 // E()=read key_is_guard_support
        bVar.a().O(new o(..., "incremental_scan_bg"));   // GuardProvider incremental scan
    } else {
        this.f20835c.l(z10, new o(..., "pre_scan"), "HOMEPAGE_SCAN", z10 ? 6 : 7);
    }
}
```

The role of each sub-scanner [observation, declaration by class and call point]:

| File | role | Trigger method |
|---|---|---|
| `scanner/c.java`(3 KB) | manual group scanner | `e(callback)`; call it in `A()` in `k` |
| `scanner/m.java`(20.8 KB) | Automatic items + system application scan + optimization | `o(...)`、`l(...)`、`k(...)`、`n(...)` |
| `scanner/d.java`(15.6 KB) | Memory scan/optimization | `n(callback)`, `m(list, callback)` |
| `scanner/CacheCheckManager.java`(5 KB) | Cache expiration check | `c(callback, callback)` |
| `scanner/o.java`(28.5 KB，Kotlin) | **VirusScanManager** (virus scan coordinator, see §3) | `r()/s()/N()/O()` |
| `scanner/n.java`(15 KB) | Virus result notification (`n.f20925f`) | `t(context, list)`、`f(context)` |
| `scanner/IncrementalScanObserver.java` | incremental scan observer | `R0/r8/s8/t8` |

Anti-reentrancy/cancellation: `k#m()` sets two threads `f20833a=true` and `interrupt()`, while `o.q()` cancels incremental scanning (`k.java:882-901`).

### 2.4 Scoring algorithm (`ScoreManager#o` / `#q` / `#i`) [Observed]

**deduction system** (`ScoreManager.java:167-220`):

```
deduct = Σ(score of system models with State==DANGER) // System items
        + n( g() ) // Memory deduction
        + f() // Cache stale points deduction
        +  Σ(score of manual models with State==DANGER) // Manual items
deduct = clamp(deduct, 0, 100)
if (!z() && deduct < 41) deduct = 41; // ★ As long as there is "any risk", at least 41 points will be deducted
```

- `n(memoryBytes)` (`ScoreManager.java:141-165`): `ratio = used/total*100`, `≥50→15`, `≥40→10`, `≥30→8`, `≥20→5`; and it returns 0 directly if it is <300 s since the last cache check.
- `f()` (`ScoreManager.java:112-132`): `>3 days→15` since the last scan, `>DEFAULT_UPDATE_FREQUENCY→10`, `>24 h→8`, `>12 h→5`, `>6 h→3`, otherwise 0.
- `z()` (`ScoreManager.java:565-567`) = `w()<=0 && f20792c.isEmpty()`, that is, "no risk items and no virus list".
- **final score `q()` = `100 - deduct`** (`ScoreManager.java:501-509`); `i()` is another path (`100 - x.m(context)`) for incremental refresh (`ScoreManager.java:445-456`).
- Points will be deducted and preferences written back: `x.c0(Application.A(), deduct)` (`ScoreManager.java:215`).

**shows**[Observed]:
- The real score control is `MainFragment.S0` (type `wf.a` interface, `MainFragment.java:154`), which is rendered by model by five implementations: `MainContentFrame` / `MainContentFrameFold` / `MainContentFrameLite` / `MainContentFramePad` / `ScanContentFramePad`:
  ```java
  // MainFragment.java:1566-1567
  mainFragment.S0.setScoreText(this.f20575g.intValue());
  mainFragment.S0.setResultScoreText(this.f20575g.intValue());
  // MainContentFrame.java:587-589
  public void setScoreText(int i10) { this.f21107b.setScore(i10); }
  // MainContentFrame.java:576-578
  public void setResultScoreText(int i10) { this.f21109d.setScore(i10); }
  ```
- There are also `MainFragment.f20481n` (`NativeInterstitialAdLayout`, `MainFragment.java:239`) **also with fraction**: `f20481n.a(ScoreManager.j().q())` (`:1695`) → Set the fraction within the advertising card and press `score >= this.f21208d` in `shape_interstitial_ad_blue` / `shape_interstitial_ad_yellow` Switch the background between (`NativeInterstitialAdLayout.java:35-46`).
- RBI: `qf.c.T(ScoreManager.j().q())` (`MainFragment.java:2248`).

`ScoreManager` is also responsible for virus list aggregation: `c(com.miui.antivirus.model.i)` / `u()` / `R()` Count `RISK_APP` separately from ordinary risks and write `o2.h.I/J` (`ScoreManager.java:344-369`).

### 2.5 Single implementation example

| item | realize | code |
|---|---|---|
| `VIRUS` (virus item) | Direct reading of ScoreManager is "clean"; jump to `miui.intent.action.ANTI_VIRUS` and bring `key_auto_to_result_page=true` + `key_risk_list` during optimization | `model/system/VirusScanModel.java:49-63`[Observed] |
| `VIRUS_CLOUDSCAN` | Only determine whether the preference switch `preference_key_open_virus_cloud_scan` is on; if it is not on, DANGER, `optimize()` directly write true | `model/manualitem/VirusCloudScanModel.java:56-69`[Observed] |
| `PAYMENT_RISK` | `o2.h.g() == w2.r.SAFE`; the results are synchronized into the SECURITY card pool of `zf.f` | `model/manualitem/PaymentRiskModel.java:51-62`[Observed] |

---

## 3. `com.miui.antivirus` - Virus Scan

(82 .java, 660.7 KB)

### 3.1 Function list

| Function | entrance | Core classes·methods | Data | Cloud/AIDL | Evidence |
|---|---|---|---|---|---|
| Virus scanning main interface | `activity.MainActivity` (96.5 KB, Manifest: 252 segments, action `miui.intent.action.ANTI_VIRUS`) | `MainActivity` + `result/n.java`(44.8 KB) + `ui/MainContentFrame` | — | AIDL | [Observed] |
| Virus result details | `activity.VirusDetailActivity` | — | `key_risk_list` | — | Manifest |
| Virus settings | `activity.SettingsActivity`（45.2 KB，action `com.miui.securitycenter.action.ANTIVIRUS_SETTINGS`） | `SettingsFragment` | See 3.4 | — | [Observed] |
| whitelist | `whitelist.WhiteListActivity`（22.2 KB，action `miui.intent.action.ANTI_VIRUS_WHITELIST`） | `whitelist/d.java`(11 KB) | `r4.a` `key_safepay_sign_exception` etc. | — | Manifest[Observed] |
| **Resident Security Service** | Manifest:835 `GuardService` (32.1 KB, remote process) | See 3.3 | — | AIDL + ProcessManager | [Observed] |
| Virus database updated daily | Manifest:870 `VirusAutoUpdateJobService`（8.4 KB） | `onStartJob` | Job id **99**, period 86400000 ms | AIDL + URL | `VirusAutoUpdateJobService.java:197-229`[Observed] |
| Payment security detection (SafePay) | `activity.MonitoredAppSettingsActivity`（20.2 KB，action `miui.intent.action.SAFE_PAY_MONITOR_SETTINGS`） | `GuardService` + `w2.t` | `assets/safepay_*` | — | [Observed] |
| Website security detection | `activity.WebsiteSecurityCheckActivity` | `h2.f`(URLFilterManager) | `assets/white_url.json` | `com.miui.guardprovider.action.urlscan` | [Observed] |
| Signature exception/installation interception | `activity.SignExceptionActivity`、`receiver.UpdaterReceiver` | — | — | — | Manifest |
| Danger prompt dialog box | `activity.DangerousAlertActivity`、`VirusMonitorDialogActivity`、`service.DialogService`（`com.miui.safepay.SHOW_WARNING_DIALOG`） | — | — | — | Manifest |

### 3.2 Scanning engine: This APK is just the client, the engine is in `com.miui.guardprovider`[Observed]

**key findings 1: There is no virus signature database file in this APK.**
There are no `.db`/`.dat`/signature library class files in the full list of `_analysis/apk_full/assets/` (the only SQLite is `garbage_clean.db`, which is a cleanup module); there are no engine libraries such as `libavl*`/`libscan*` among the 18 .so files under `lib/`. → **local signature library format** does not exist in this APK and belongs to GuardProvider APK. [Observed]

Key discovery of **2: The virus killing body is not included in this APK.**

1. `p9.a` (`GpBinderManager`) binding external service fixed Action/Package:

```java
// p9/a.java:136-140
private void h() {
    Intent intent = new Intent("com.miui.guardprovider.action.antivirusservice");
    intent.setPackage("com.miui.guardprovider");
    this.f33092a.bindService(intent, this.f33096e, 1);
}
```

2. The interface is described by the AIDL stub `com.miui.guardprovider.aidl.IAntiVirusServer` in this APK (26.9 KB generated code), and the exposed method (`IAntiVirusServer.java:561-607`[Observed]):

```
int v0(String[] paths, IVirusObserver cb, boolean flag) // Batch scan files
int  y6(String[] paths, IVirusObserver cb, boolean flag, int mode)
int q1(IVirusObserver cb) int D(IVirusObserver cb) // Full/Cancel
boolean r7(String) / X3(String) // Single package/single path query
String n0(String) / P6(String) // Engine/library version
String[] O() / Q0() // Engine list / support capabilities
List<String> m6(List) / x0(List) // Batch query (whitelist/result)
int C2/K/c5/O3(String url, IWifiDetectObserver, …) // ★ Wi-Fi security detection
boolean K0() int h1() void W/L5/O1/j2(int|String,…) // Status and control
```

3. The `IAntiVirusServer` copy also declares the Wi-Fi detection sub-interface `IWifiDetectObserver` (`aidl/IWifiDetectObserver.java`), indicating that the GuardProvider also provides Wi-Fi risk detection.

4. Both Provider URI also point to GuardProvider[Observed]:
   - `content://guard/support_incremental` (`scanner/o.java:653`) - whether to support incremental scanning
   - `content://guard/network_set` (`o2/b.java:85-88`) - write network status

5. **scan object**[Observed]`VirusScanManager#y(Context)` (`scanner/o.java:710-769`):

```java
for (PackageInfo packageInfo : i4.a.k(context).j()) {
    ApplicationInfo applicationInfo = packageInfo.applicationInfo;
    if ((applicationInfo.flags & 1) == 0) {       // Only scan non-system applications (FLAG_SYSTEM==0)
        … iVar.q(applicationInfo.sourceDir);      // Record APK path
        map.put(applicationInfo.sourceDir, iVar);
    }
}
// Append: Orphaned .apk in external storage
Cursor cursorQuery = contentResolver.query(MediaStore.Files.getContentUri("external"),
        new String[]{"_data", "date_modified"}, "_data LIKE '%.apk'", null, null);
```

→ ** scanning objects = `sourceDir` with third-party APK installed + all `*.apk` files ** in external storage. Memory scan/process scan code path not seen. [Observed]`o2.b.c` Two enumeration values ​​also confirm: `INSTALLED_APP` / `UNINSTALLED_APK` (`o2/b.java:59-62`).

6. Result model [Observed] `o2.b.d` four states: `SAFE / RISK / VIRUS / RISK_APP` (`o2/b.java:64-69`).

7. **is not used. `libsec-common-utils` is used for killing.**[Observed]: All APK. Only `com.miui.securitykey.ConfigManager` loads it (`ConfigManager.java:42`). The purpose is to obtain the key and has nothing to do with killing (see §7).

### 3.3 `GuardService`: Payment security + input method + Wi-Fi resident guardian [Observed]

`GuardService.java` structure:

| mechanism | code |
|---|---|
| Register **process-level activity switch monitoring** (reflection `miui.process.ProcessManager`) | `bh.e.f(Class.forName("miui.process.ProcessManager"), "registerActivityChangeListener", new Class[]{List, List, Class.forName(IActivityChangeListener.DESCRIPTOR)}, o2.h.k(this), o2.h.j(this), this.f10637h)` — `GuardService.java:390` |
| Listening collection comes from SafePay rules (app package + activity) | `f10651v = o2.h.k(guardService)` (`key_safepay_monitored_apps_list`), `f10652w = o2.h.j(...)` (`key_safepay_monitored_activity_list`), `f10653x = t.p(guardService)` (`type==0` in `safepay_target_activity`), `f10654y = t.m(guardService)` (`safepay_calling_activity`) - `GuardService.java:385-388` |
| Enter/leave the payment page → Status bar safe state | `t.P(this, 0, "com.miui.app.ExtraStatusBarManager.action_status_safepay", false, new Bundle())` — `GuardService.java:452`、`617` |
| Payment warning popup | `com.miui.safepay.SHOW_WARNING_DIALOG` / `HIDE_WARNING_DIALOG` — `GuardService.java:598`、`629` |
| **Input method anti-hijacking**: Switch back to the system input method when it is detected that the current default input method is a third party | `S()` — `GuardService.java:700-735`: `Settings.Secure.putString(cr,"default_input_method", system input method.getId())`, and save the original value in `f10655z` for `O()` to restore |
| Restricting the backend via AppOps | `setUserRestriction(24, z10, binder, new String[]{F, "com.android.pcmode", "com.miui.handwriting"})` — `GuardService.java:675` |
| Wi-Fi risk detection | Register `WIFI_STATE_CHANGE / CONFIGURED_NETWORKS_CHANGE / supplicant.*`, hand it over to `f10644o` (`o2.i`) and `w2.t`; the result is written `key_safepay_wifi_scan_result` — `GuardService.java:638-648`, `o2/h.java:123` |
| Game joint operation judgment (jointly operated games are tied to the game cashier when paying) | `e extends AsyncTask` → `POST https://flash.sec.miui.com/game/union`, field `pkg`, response `isUnionGame`/`frequency` — `GuardService.java:330-352` |
| onStartCommand command | `action_register_foreground_notification`(4) / `action_unregister_foreground_notification`(5) / `action_pay_safe_dialog_click_ignore` / `action_unshelf_warning_dialog_click_ignore` (with `prePackageName`/`preClassName`/`curPackageName`/`curClassName`) / `action_anti_fraud_auto_scan_apps` / `action_anti_fraud_auto_scan_single_app` (with `curPackageName`)—`GuardService.java:775-797` |
| onCreate injection | `o2.g.J(this)` (monitor), `i.c(this)` (Wi-Fi), `p9.a.j(this)` (Guard binding), `w2.b.b()` (anti-fraud) - `GuardService.java:749-764` |

### 3.4 Virus database update: two independent channels [Observed]

`VirusAutoUpdateJobService#onStartJob`（`VirusAutoUpdateJobService.java:209-229`）：

```java
d dVar = new d(jobParameters);
j2.a.e(this).d(new a(dVar));      // ① Update SMS rule resource.zip (see §4.4)
if (t.G(this)) { /* FBE */ } else {
    b(dVar);                       // ② Go to IAntiVirusServer to update the virus database (VirusObserver.A(UpdateInfo))
    e();                           // ③ updateAvlURLRules → h2.f#f() → IURLScanServer.b0("AVL")
}
me.c.g().m(dVar);
this.f10674a.postDelayed(dVar, 12000L);   // 12 s timeout
```

Update result field [Observed]: `UpdateInfo{ updateResult(0/2/3), engineName }`, successfully write `preference_key_database_auto_update_time_%s` (`key_database_auto_update_*` of `VirusAutoUpdateJobService.java:86-109`, `SettingsActivity`).

Job registration (`VirusAutoUpdateJobService.java:197-201`): `JobInfo.Builder(99, …).setPeriodic(86400000L).setPersisted(true).setRequiredNetworkType(2)`, i.e. **24 h, durable, non-metered network only**; controlled by `persist.sys.miui_optimization` / `ro.miui.cts`.

### 3.5 Persistence (antivirus related keys, all in `remote_provider_preferences`) [Observed]

| key | meaning | Evidence |
|---|---|---|
| `key_is_guard_support` | Does GuardProvider support incremental scanning? | `scanner/o.java:554-578` |
| `key_full_scan_time` | Last full scan time | `scanner/o.java:570,663,697` |
| `key_test_type` | AB Testing: New/Old Scanning Strategies (0/1) | `scanner/o.java:661,691-694` |
| `key_database_auto_update_enabled_%s` / `key_database_auto_update_time_%s` | Engine library automatic update switch/time (`%s`=engine name or `all`) | `res/values/strings.xml` |
| `key_pay_safety_status` | Payment security status (`w2.r` enum int) | `o2/h.java:53-55,157` |
| `key_safepay_monitored_apps_list` / `key_safepay_monitored_activity_list` | SafePay listening collection | `o2/h.java:69-75,168-179` |
| `key_safepay_sign_exception` | Signature anomaly whitelist | `o2/h.java:103,187` |
| `key_safepay_wifi_scan_result` | Wi-Fi scan conclusion string | `o2/h.java:123,191` |
| `key_safepay_input_method_state` / `key_safepay_auto_scan_state` | Input method/auto scan switch | `o2/h.java:87-99` |
| `key_anti_fraud_packages` | Anti-fraud scanned application collection | `o2/h.java:127` |
| `key_latest_virus_scan_date` | Latest virus scan time (also write `Settings.Secure`) | `o2/h.java:66-67` |
| `key_last_scan_risk_count` / `key_last_scan_virus_count` | Last risk/virus count | `o2/h.java:58-62` |
| `key_free_wifi_alert` / `key_first_enter_antivirus_v3` / `key_risk_application_warning_enable` | Prompt switch | `o2/h.java:41-45,183,199` |
| `preference_key_open_virus_cloud_scan` | **cloud killing switch** (string resource) | `o2/h.java:23`、`VirusCloudScanModel.java:32` |
| `key_delte_gameunion_file_frequency` | Game intermodal file cleaning frequency | `o2/h.java:141` |
| SharedPreferences `data_config` | Results page/wireframe animation configuration | `antivirus/result/ScanResultFrame.java:223`、`result/c0.java:123` |

> **is unknown to**: Which interface is used for "cloud checking" after `preference_key_open_virus_cloud_scan` is turned on? In this APK code, **does not find** to upload/query URL; `IAntiVirusServer` does not have a cloud checking method. **infers that**: The GuardProvider (another APK) is connected to the network by itself. This APK is only responsible for switches and prompts.

---

## 4. `com.miui.antispam` - Harassment interception

(72 .java, 618.7 KB)

### 4.1 Determine the overall calling direction of the link [observation + inference]

This APK **does not intercept calls/text messages** by itself; the system (MIUI framework/Telephony) passes the ContentProvider counter-checking strategy when calling/receiving text messages:

```
System Telephony
   ↓ ContentResolver.query(content://antispam/sms_judge) ← 3 parameters: [number, text, simId]
   ↓ ContentResolver.query(content://antispam/call_judge) ← 5 parameters: [number, callType, is_forward_call,
   ↓                                                              is_repeated_normal_call, is_repeated_blocked_call]
com.miui.antispam.db.AntiSpamProvider   (Manifest:1194, exported + permission CONTENTPROVIDER)
   ↓ f9555b = b2.b.c(context) [PolicyManager singleton]
b2.b#b(cc) ← Incoming call b2.b#e(cc) ← SMS
   ↓ Run policies one by one in h2.e enumeration ordinal order (TreeMap is sorted by f26798a)
com.miui.antispam.policy.* (21; URLFilterPolicy is linked to the last link of the SMS chain)
   ↓ Hit means break, return blockType (int)
SMS: ExtraTelephony.getRealBlockType(i10) → if ≥3: m2.f.a(ctx, number, realBlockType, keyword) Write interception log
```

Evidence: `AntiSpamProvider.java:62-91` (URI table), `:106-124` (`b()`=getCallBlockType / `c()`=getSMSBlockType → `this.f9555b.b/e(cVar)`), `:828-884` (`case 9` / `case 10` Real implementation, `MatrixCursor` listed `smsBlockType` / `callBlockType`), `b2/b.java:81-169` (PolicyManager main loop + `ExtraTelephony.getRealBlockType` + `f.a`).

> **Note** (see §4.6-C): `url_judge`(13) / `service_num_judge`(14) / `call_transfer_intercept_judge`(21) Three URI are registered in `UriMatcher`, but `query()` is gone The `default` branch directly returns null - **This version does not implement**.

### 4.2 21 strategies, in order of priority [Observed] (`h2/e.java:113-159`)

| ord | enumeration | Applicable | Implementation class |
|---|---|---|---|
| 0 | `EMPTY_NUMBER_POLICY` | CALL | `EmptyNumberPolicy` |
| 1 | `STRONG_CLOUD_WHITELIST_POLICY` | BOTH | `StrongCloudWhiteListPolicy` |
| 2 | `STRONG_CLOUD_BLACKLIST_POLICY` | BOTH | `StrongCloudBlackListPolicy` |
| 3 | `WHITELIST_POLICY` | BOTH | `WhiteListPolicy` |
| 4 | `BLACKLIST_POLICY` | BOTH | `BlackListPolicy` |
| 5 | `VAGUE_WHITELIST_POLICY` | BOTH | `VagueWhiteListPolicy` |
| 6 | `BLACK_PREFIX_POLICY` | BOTH | `BlackPrefixPolicy` |
| 7 | `BLACK_ADDRESS_POLICY` | BOTH | `BlackAddressPolicy` (including "number segment/location" blacklist) |
| 8 | `SERVICE_SMS_POLICY` | SMS | `ServiceSmsPolicy` (106 service number SMS) |
| 9 | `KEYWORDS_WHITELIST_POLICY` | SMS | `KeywordsWhiteListPolicy` |
| 10 | `CLOUD_WHITELIST_POLICY` | BOTH | `CloudWhiteListPolicy` |
| 11 | `CALL_ALL_NUMBER_POICY` | CALL | `CallAllNumberPolicy` |
| 12 | `STRANGER_POLICY` | BOTH | `StrangerPolicy` (stranger) |
| 13 | `OVERSEA_POLICY` | CALL | `OverseaPolicy` |
| 14 | `KEYWORDS_BLACKLIST_POLICY` | SMS | `KeywordsBlackListPolicy` |
| 15 | `CLOUD_BLACKLIST_POLICY` | BOTH | `CloudBlackListPolicy` |
| 16 | `CLOUD_WHITE_KEYWORDS_POLICY` | SMS | `CloudWhiteKeywordsPolicy` |
| 17 | `CLOUD_BLACK_KEYWORDS_POLICY` | SMS | `CloudBlackKeywordsPolicy` |
| 18 | `SMART_SMS_FILTER_POLICY` | SMS | `SmartSmsFilterPolicy` ← **connected to TFLite engine** |
| 19 | `CALL_TRANSFER_POLICY` | CALL | `CallTransferPolicy` (Call transfer interception) |
| 20 | `REPORTED_NUMBER_POLICY` | CALL | `ReportedNumberPolicy` (reported number) |

The strategy base class `com.miui.antispam.policy.a` (`policy/a.java:10-44`) holds `mJudge` (`h2.b`) and `mPolicyManager`; the return value `a.C0122a{ boolean a; int b; String c; }`, `a=true` means "processed/terminated chain".

An extra step at the end of `b2.b#e()` (`b2/b.java:140-144`): After the SMS is determined, go to `com.miui.antispam.policy.b#d(blockType, cVar)` (**URLFilterPolicy**, the class name is not in the enumeration, and is mounted separately).

### 4.3 `assets/pattern` - How rules are loaded and matched [Observed]

**loading path**: `assets/pattern` (GBK/UTF-8 mixed with JSON) → `lj.c.a(Context)` is read as a string (`files/pattern` is preferred, otherwise assets, `lj/c.java:42-57`) → `nj.d#d(String)` is parsed.

```java
// nj/d.java:61-85
JSONObject jSONObject = new JSONObject(new JSONTokener(str));
String string = jSONObject.getString("version");          // "1.2"
JSONArray jSONArray = jSONObject.getJSONArray("features");
for (…) {
    c cVar = new c(this.f32120a);
    if (cVar.c(jSONArray.getJSONObject(i10))) {
        this.f32120a.put(cVar.a(), cVar);                  // name → feature
        this.f32121b.add(cVar);
    }
}
```

Schema**(`nj/c.java:51-84`) of** single feature:

```json
{
  "name": "Fei_xin",
  "sms_type": 1,                 // bit mask, ultimately ORed together
  "assist": true,                // true=auxiliary features (not included in scoring)
  "address": { "type":[1,2,3,4,5,6], "pattern":["^12520"], "neg_pattern":[…], "or_pattern":[…] },
  "body":    { "pattern":[ …all must match… ], "neg_pattern":[ …exclude if any match… ], "or_pattern":[ …any match… ], "body_len":["0-15"] },
  "combi":   [ "featA&featB", … ]    // Combination Features: All hits in any row are counted as hits.
}
```

Matching algorithm[Observed]:
- `nj.e#a(String)` (`nj/e.java:70-97`): `pattern` is AND (all `find()` hits), `neg_pattern` is excluded (any hit is false), `or_pattern` is OR.
- `nj.c#e(lj.b)` (`nj/c.java:90-123`): address and body are both satisfied, and then press `combi` for recursive evaluation.
- `nj.d#e(lj.b)` (`nj/d.java:87-95`): **traverses all non-assist features, sets the hit `sms_type` or** to get `featureBits`, and writes back `bVar.n(featureBits)` (`lj/e.java:20-32`).
- Bit and weight conversion [Observed] `nj.d#b(int)` (bits 12..23 → weight 1..12) and `nj.d#a(int)` (bits 4..15 → 12..1), used to offset the model output: `lj/e#g(double[], b)` shifts logits (`lj/e.java:49-64`) according to `(iA-iB)*(p0-p1)/4`.

### 4.4 New generation SMS engine (TFLite, issued by `asset/resource.zip`) [Observed]

`h2.a` (`SmsEngineHandler`, line 339):

| link | code |
|---|---|
| Unzip the rules package | `j(context, dir)`: `ZipInputStream(context.openFileInput("resource.zip"))` → `files/SmsEngineResource/`, with path crossing protection (`getCanonicalPath().startsWith(str)`) — `h2/a.java:98-181` |
| structure check | `l(context)`: Compare file sizes entry by entry. If inconsistent, re-interpret - `h2/a.java:184-224` |
| Modeling | `da.a.b(dir)` → `com.miui.sms...` three-piece set (`da/a.java:31-57`) |
| judge text message | `h(sender, body)`: `mProxy.c(sender, body)` → whether it is important; `d(body)` → whether it is intercepted. Return 0=Do not block/Important, 2=Intercept — `h2/a.java:234-331` |
| trigger update | `j2.a` (`SmsEngineUpdateManager`): `POST https://api.sec.miui.com/GuardProviderV1/GetResourceZipFile`, body `{app_version, ver:"2.1.2"}`, response `{code:200, data.fileUrl}`; after downloading, **MD5 must be equal to `6206c9f17a64246498b52e117d0cff1e`**, place `files/resource.zip`, then `ContentResolver.call(content://antispam, "initSmsEngine")` — `j2/a.java:261-276, 166-198` |

TFLite components (`da` / `aa` / `ca` / `ba` / `z9` packages):

| class | Purpose | constant |
|---|---|---|
| `ca.a` | Vocabulary → 128-dimensional embedding: files `v.txt` (`token\tfloat`), `f8020b=128` | `ca/a.java:19-46` |
| `aa.c` | **UnlawRecognizer**: `1.bin`, threshold `0.4` | `aa/c.java:11-29` |
| `aa.a` | **ImportanRecognizer**: `2.bin`, threshold `0.6` | `aa/a.java:11-29` |
| `aa.b` / `ba.b` / `ba.a` | Rule table by sender (key="0" is wildcard) | `aa/b.java:15-46` |
| `z9.a` | Pre-screened regex: including Chinese, pure numbers, pure ASCII without spaces → skip directly | `z9/a.java:9-15` |
| `lj.g` + `mj.a` | **Thesaurus/word segmentation of the old engine**: `assets/dict` → `mj.a.h(InputStream)`, CRF Formula 4 state decoding (`mj/a.java:56-80`) | — |

> [Observed] `assets/model` (680 KB) and `assets/rec_model_20230425.mnn` **is not referenced by** in this module code; `lj.g` only uses `dict`, and `aa.*` only uses `1.bin/2.bin/v.txt` in `resource.zip`.
> [Unknown] The actual consumer of `assets/model` / `.mnn` (**infers**: clean/recommend/`com.miui.mlkit` path, not this module).

### 4.5 Number database/yellow pages/white list

| data source | Reading method | Evidence |
|---|---|---|
| **system phone book/blacklist** (real storage location) | `ContentResolver.query(ExtraTelephony.Phonelist.CONTENT_URI, {type,state,number,sim_id}, "sync_dirty<>1")` | `h2/b.java:140-145`[Observed] |
| **system keyword library** | `ContentResolver.query(ExtraTelephony.Keyword.CONTENT_URI, {type,data,sim_id})` | `h2/b.java:84-90`[Observed] |
| SMS Whitelist (Yellow Pages) | `YellowPageContract.AntispamWhiteList.CONTNET_URI`, `number LIKE 'xxx%'`, `106` start with white | `SmartSmsFilterPolicy.java:27-51`[Observed] |
| Local service number list | `assets/service_num.json` (Priority `files/service_num.json`, can be covered by cloud `MiuiSettingsCompat`) | `m2/e.java:55`[Observed] |
| URL whitelist | `assets/white_url.json` (`{"content":[…]}`), can be covered by cloud `MiuiSettings antispam_white_url/content` | `m2/e.java`[Observed] |
| Cloud number library | `POST https://api.sec.miui.com/harassIntercept/intercepts`, body `{dataVersion}`; response `version` + `v[{status:new|updated|deleted, type:1|2, uuid, p, attributes}]` (type=1 number, attributes two digits = category + level; type=2 keyword, attributes single digit) | `k2/a.java:51-130`[Observed] |

After the cloud number is logged into the database, it will be written `b2.b#h/i` (`f7520b.E/G`, `F`) and synchronized to the memory index; when it is fully reset, it will be `delete from phone_list where cloudUid not null` / `keyword …` (`k2/a.java:45-49`).

### 4.6 URL interception (`URLFilterPolicy`, `policy/b.java`) [Observed]

**Let’s first explain the two auxiliary functions that are easy to confuse (the semantics of the whole tree have been checked):**
- `m2.i.f(ctx, str)` → `m2.e.b(ctx, str)` (`m2/i.java:147`, `m2/e.java:47-70`) = **determines whether `str` is in the "service number list"** (cloud `antispam_service_number/content`, otherwise `assets/service_num.json`'s `content` array). **is not a** URL table.
- `m2.i.b(str)` (`m2/i.java:93-129`) = use regular `f29607a` to extract all URL**from the text**; `m2.i.a(str)` = normalized domain name; `m2.i.c(str)` = `\d{7,15}` to determine whether the text contains 7–15 consecutive digits (suspected account number/card number); `m2.i.d(ctx)` = read `white_url.json` whitelist set.

**A. URL determination in the SMS main chain (actual effective path)** —— `policy/b.java#d(int blockType, h2.c sms)`

```java
// policy/b.java:220-267
public a d(int i10, c cVar) {
    if (Build.IS_INTERNATIONAL_BUILD) return null;
    ArrayList<String> urls = m2.i.b(cVar.f26766g);            // URL in the text
    if (urls.isEmpty()) return null;
    if (!m2.i.f(this.f9563a, cVar.f26761b)) {                 // ★ The sender is not a service account
        return new a(true, e(i10));                           // → Directly determine risk: blockType |= 128
    }
    if (i10 >= 3) return null;                                // If it has been judged to be at a more serious level, it will no longer be superimposed.
    f fVar = new f(this.f9563a);                              // GuardProvider IURLScanServer
    for (String s : urls) {
        if (fVar.d(s) == 1) { c2.a.t("black"); return new a(true, 8); }   // AVL determines black → blockType=8
    }
    if (m2.i.c(cVar.f26766g)) { c2.a.t("number"); return new a(true, e(i10)); }  // Contains 7-15 digits
    HashSet<String> whitelist = m2.i.d(this.f9563a);
    for (String s : norm(urls)) {
        if (!whitelist.contains(s)) { c2.a.t("non_white_black_single_url"/"…multi_url"); return new a(true, e(i10)); }
    }
    c2.a.t("white"); return null;                             // All hits whitelist → Release
}
```
Among them `e(int i) = i | 128` (`policy/b.java:168-170`). Call point: `b2/b.java:140-144` (the last link of the SMS chain, `this.f7521c.d(i10, cVar)`).

**B. `c(String oriUrl, String redirectUrl)`** (`policy/b.java:172-218`) - independent URL determination entry, the first parameter of the signature is **sender number** (used for service number release):

```java
public int c(String str /*sender number*/, String str2 /*URL*/) {
    boolean zF = m2.i.f(this.f9563a, str);                    // Is the sender a service account?
    String strA = a(str2, new i("antispam_urlfilterpolicy")); // Follow 30x to get the final URL (UA antispam/1.0.0)
    int iD = new f(this.f9563a).d(str2);                      // ① GuardProvider AVL url scan
    if (iD != 1) iD = b(str2);                                // ② Cloud detection (original)
    if (iD != 1) iD = b(strA);                                // ③ Cloud detection (after the jump)
    if (!zF) { return iD==1 ? 2 : (iD==2 ? 0 : -1); }
    if (iD == 1) return 2;
    HashSet<String> whitelist = m2.i.d(context);              // white_url
    if (whitelist.contains(norm(str2))) return 0;
    if (!e2.b.i()) return -1;                                 // No Internet access → Unknown
    return whitelist.contains(norm(strA)) ? 0 : 1;
}
```
`b2.b#f(str,str2)` is its packaging (`b2/b.java:164-169`); **[Unknown] The call point `b2.b#f` was not found in this APK** - inferred from the system side (MIUI framework browser/SMS URL detection) is triggered through other channels, or this version is not wired yet.

**C. Three "declared but not implemented" Providers URI**[Observed]:
Within `switch` of `AntiSpamProvider.query()`, `case 13 (url_judge)` / `case 14 (service_num_judge)` / `case 21 (call_transfer_intercept_judge)` all fall into `default` → `Log.e(...,"Unknown query URI")` → `return null` (`AntiSpamProvider.java:904-906`). Only `case 9 (sms_judge)` and `case 10 (call_judge)` are actually implemented.

Cloud URL detection interface (`policy/b.java:135-166`):

```
POST https://url-detect.engine.mi.com/url/detect
form: url=<url>&ts=<sec>&c=2&sign=md5(sort splicing + "2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9")
resp: {"code":200, "data":{"urlCategoryId": N}}
      N==-1 → 0 (unknown) N==0 → 2 (danger) N==1|2 → 1 (safe)
UA: antispam/1.0.0 Timeout: connect/read 5 s each
```

### 4.7 Database structure[Observed]

`e2.a` (`AntiSpamDB extends SQLiteOpenHelper`): **db name `AntiSpam`, version 10** (`e2/a.java:117`). Create table SQL all from string resources (`res/values/strings.xml`, `e2/a.java:130-148`):

| table | Table creation statement (original text) |
|---|---|
| `fwlog` | `CREATE TABLE fwlog( _id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT DEFAULT NULL, date INTEGER NOT NULL DEFAULT 0, type INTEGER NOT NULL DEFAULT 0, reason INTEGER NOT NULL DEFAULT 0, data1 TEXT DEFAULT NULL, data2 TEXT DEFAULT NULL, read INTEGER NOT NULL DEFAULT 0, simid TEXT DEFAULT -1, mode INTEGER NOT NULL DEFAULT 0, callType INTEGER NOT NULL DEFAULT 0 );` |
| `phone_list` | `CREATE TABLE phone_list( _id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT DEFAULT NULL, display_number TEXT DEFAULT NULL, notes TEXT DEFAULT NULL, state INTEGER NOT NULL DEFAULT 0, isdisplay INTEGER NOT NULL DEFAULT 1, type INTEGER NOT NULL DEFAULT 1, cloudUid TEXT DEFAULT NULL, sync_dirty INTEGER DEFAULT 0, e_tag TEXT DEFAULT NULL, record_id TEXT DEFAULT NULL, location INTEGER DEFAULT 0, sim_id INTEGER NOT NULL DEFAULT 1 );` |
| `keyword` | `CREATE TABLE keyword( _id INTEGER PRIMARY KEY AUTOINCREMENT, data TEXT DEFAULT NULL, type INTEGER NOT NULL DEFAULT 1, cloudUid TEXT DEFAULT NULL, sim_id INTEGER NOT NULL DEFAULT 1 );` |
| `mode` | `CREATE TABLE mode( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT DEFAULT NULL, state TEXT DEFAULT NULL );` |
| `sim` | `CREATE TABLE sim( _id INTEGER PRIMARY KEY AUTOINCREMENT, sim_id TEXT NOT NULL, call_wait TEXT DEFAULT -1, name TEXT DEFAULT NULL, backsound_mode INTEGER DEFAULT 0 );` |
| `reportSms` | `CREATE TABLE reportSms( id TEXT NOT NULL );` |
| `reportSmsPending` | `CREATE TABLE reportSmsPending( id TEXT NOT NULL, address TEXT, body TEXT, date TEXT );` |

> [Observed] The **authoritative copy of `phone_list`/`keyword`/`fwlog` is actually in the system MIUI Provider** (`miui.provider.ExtraTelephony.*`). The `AntiSpam` library of this APK is mainly responsible for Cloud synchronization/reporting/dual-SIM mode/ringback tone and other scenarios; in `onUpgrade`, bulkInsert the call record of `ExtraTelephony.FirewallLog` twice into `CallLog.Calls` and then clear it (`e2/a.java:42-76`).

### 4.8 Backup/synchronization protobuf[Observed]

- Schema：`assets/protobuf/antispam.proto`（`package antispam; java_package = "com.miui.antispam.service.backup"; java_outer_classname = "AntiSpamProtos"; optimize_for = LITE_RUNTIME`）
  - `AntiSpam{ repeated BlacklistEntry blacklist=1; repeated WhitelistEntry whitelist=2; repeated KeywordEntry keyword=3; repeated PolicyEntry policy=4; repeated SwitchEntry switch=5; repeated NotificationEntry notification=6; AutoUpdateLibrary autoUpdateLibrary=7; SimsShareSettings simsShareSettings=8; ReportedNumberGuide reportedNumberGuide=9; }`
  - Each entry uniformly carries `guid=1, luid=2` (synchronization identification), and the entry fields are: `number/notes/state/simId` (black), `number/notes/state/isDisplay/simId` (white), `keyword/simId/type` (keyword), `name/state/simId` (policy), `state/simId` (switch/notification).
  - `ReportedNumberGuide{ bool FraudState=3; bool AgentState=4; bool SellState=5; }` ← Category III mark number (fraud/intermediary/promotion).
- `syncroot.proto`：`SyncRoot{ optional antispam.AntiSpam antispam = 8; }`
- Generated code: `com.miui.antispam.service.backup.a..n` (Lite protobuf, `c extends GeneratedMessageLite` is `AntiSpam`, 9 `List` + 3 singleton fields, `c/c.java:19-56`).
- Point of use: `SecurityCenterBackupAgent extends miui.app.backup.FullBackupAgent` (`SecurityCenterBackupAgent.java:17`):
  - `onFullBackup`: Read the 5-category list (`this.f9576a.i()/f()/j()/g()/h()`) from DB and pour it into `c.D()` builder, `n.g().k(build).build().writeTo(fos)` (`:92-171`)
  - `onDataRestore`: `n.j(stream)` → One by one `this.f9576a.c/a/d/b/r(...)` write back (`:30-89`)
  - `getVersion()` returns 1 fixedly.
- **infers**: `guid`/`luid` are the global/local IDs of MiCloud synchronization; the synchronization channel itself is not implemented in this APK (`miui.cloud` framework side).

### 4.9 antispam preference (almost all in `Settings.Secure`, non-SP files) [Observed]

`m2.a` is a Settings.Secure wrapper (`bm.a.*` = `Settings.Secure`/`Settings.System`), and `e2.b` does dual-card dispatch on it:

| key | Type | meaning |
|---|---|---|
| `fraud_num_state` / `fraud_num_state_sim_2` | int | Scam number blocking (0=on) |
| `agent_num_state(_sim_2)` | int | Intermediary number |
| `sell_num_state(_sim_2)` | int | Promotion number |
| `harass_num_state(_sim_2)` | int | harassment number |
| `show_notification_type(_sim_2)` | int | notification style |
| `sms_classifier_auto_update` | bool | SMS classifier automatic update (`m2/a.java:96`) |
| `sms_classifier_update_time` | long | Last updated |
| `service_sms_mode` / `stranger_sms_mode` / `mms_mode` | int | Service account/stranger/MMS interception mode (default 1/1/2) |
| `call_all_number_mode(_2)` | int | Block all numbers |
| `backsound_index(_sim_2)` | int | Ringback tone |
| `is_call_transfer_blocked(_sim_2)` | bool | call forwarding interception |
| `is_repeated_marked_number_permit(_sim_2)` | bool | Duplicate marking allowed |
| `virtual_sim_slot_id` / `has_new_antispam` | int/bool | Virtual Card Slot/New Message |
| `unread_mms_count` / `unread_phone_count` | int | unread |
| `clouds_data_version_new` | long | Local version of cloud number database (`e2/b.java:36-38,116-118`) |
| `reported_number_settings_reset` | bool | Reporting settings reset |

---

## 5. `com.miui.antifraud` + `com.miui.riskcontrol`

### 5.1 antifraud (5 classes)

| Function | entrance | core | Data | Cloud | Evidence |
|---|---|---|---|---|---|
| Call/SMS fraud warning | `AntiFraudReceiver` (Manifest:433, exported, requires `MODIFY_PHONE_STATE`) → only handles `android.intent.action.PHONE_STATE` and `miui.action.mms.SCAN_RISKY_URL` | `d.k().p(intent)` / `d.k().q()` | `d` class state machine | — | `AntiFraudReceiver.java:20-40`[Observed] |
| Transfer/remittance page monitoring | `PayActivityMonitorService`（Manifest:432，remote） | See below | `assets/anti_fraud_pay_activity` | — | `PayActivityMonitorService.java`[Observed] |

Front door of `AntiFraudReceiver` (`AntiFraudReceiver.java:21-31`) [Observed]:
```java
if (intent == null || Build.IS_INTERNATIONAL_BUILD || !tm.isVoiceCapable()) return;
if (t.h() < 13) { q0.g(TAG,"return,because MIUI Version below 13"); return; }
if (!x.h()) { Log.i(TAG,"sms call pay warn not open"); return; }   // Switch: SMS/call payment alert
```

`PayActivityMonitorService`[Observed]（`PayActivityMonitorService.java:85-127`）：
```java
InputStream is = new File(filesDir + "/anti_fraud_pay_activity").exists()
        ? openFileInput("anti_fraud_pay_activity") : getAssets().open("anti_fraud_pay_activity");
JSONArray arr = new JSONArray(cl.e.i(is));   // [{package, activity:[...]}]
bh.e.f("PayActivityMonitorService", Class.forName("miui.process.ProcessManager"),
       "registerActivityChangeListener",
       new Class[]{List.class, List.class, Class.forName(IActivityChangeListener.DESCRIPTOR)},
       v.Q(pkgSet), v.Q(activitySet), this.f9521e);
```
Enter the hit Activity (delay 500 ms) → `d.k().r(true)` + `d.k().o()`; leave the same package Activity → `d.k().r(false)` + `d.k().j()` (`PayActivityMonitorService.java:57-76`).

`assets/anti_fraud_pay_activity` content [Observed]: WeChat `com.tencent.mm.plugin.remittance.ui.RemittanceUI`, Alipay 4 `com.alipay.mobile.transferapp.ui.*`, QQ `…qwallet.transaction.impl.TransactionActivity` - **is exactly the "transfer/remittance confirmation page"**.

> The specific determination rules of [Unknown]`d` (13.9 KB), whether to report to the server, and the reporting fields - need to be read separately; this document only covers the entrance and page monitoring. **unknown**: `x.h()` corresponding preference key and "reported content".

### 5.2 riskcontrol (1 class, 79 lines)

**`SensitiveWordDetectProvider`** - unique component, Manifest:1733 (`authorities="com.miui.riskcontrol"`, **exported=true**, `permission=com.miui.security.permission.DETECT_TEXT`) [Observed]:

```java
// SensitiveWordDetectProvider.java:19-46
static {
    Uri.withAppendedPath(Uri.parse("content://com.miui.riskcontrol"), "censorResult");
    f20241a.add("com.android.camera");
    f20241a.add("com.miui.mediaeditor");      // ★ Hard-coded whitelist: only serves cameras and media editors
    System.loadLibrary("lib-riskcontrol");    // → /lib/arm64-v8a/liblib-riskcontrol.so
}
private native boolean hasSensitiveWords(String str);
private native void setAssetManager(AssetManager assetManager);

public final Bundle call(String str, String str2, Bundle bundle) {
    if (!f20241a.contains(getCallingPackage())) { Log.i("SWDP", getCallingPackage()+"not in whiteList!"); return null; }
    if (!"METHOD_SENSITIVE_WORD_DETECT".equals(str)) return null;
    setAssetManager(getContext().getAssets());          // ★ Hand over assets to native
    Bundle b = new Bundle();
    b.putBoolean("hasSensitiveWords", hasSensitiveWords(str2));
    return b;
}
```

- [Observed] **protocol**: The caller `ContentResolver.call(Uri.parse("content://com.miui.riskcontrol"), "METHOD_SENSITIVE_WORD_DETECT", text to be detected, null)`, returns `Bundle.getBoolean("hasSensitiveWords")`.
- [Observed]**`assets/dict` / `censor_source` Are** used here: `setAssetManager(getAssets())` Hand over the entire assets to native, the Java layer **of this APK does not have any** `open("dict")`/`open("censor_source")` call (whole tree grep only `lj/g.java:32` reads `dict`, which is a short message segmentation). Therefore, whether **`censor_source` (2688 B, high entropy cannot be read) and `dict` is read by `liblib-riskcontrol.so` belongs to the native side behavior - [Unknown], and `open_binary` needs to analyze the string of `lib/arm64-v8a/liblib-riskcontrol.so`/PLT to make a conclusion.**
- [Observed] The return value of `Uri.withAppendedPath(...)` in Provider is discarded, and `UriMatcher` in `onCreate` is just `new` and discarded - **is a pure placeholder code**, and the actual entry is only `call()`.
- [Inferred] Purpose: Camera/media editor conducts sensitive word/text review in the **offline** scene (such as taking photos/screenshots of the text after OCR), without being connected to the Internet, and will not be reported. **Unknown**: Whether it will be reported after `hasSensitiveWords` hits.

---

## 6. `com.miui.luckymoney`——Red envelope assistant

(54 .java, 231.8 KB)

### 6.1 Function list

| Function | entrance | core | Data | Cloud | Evidence |
|---|---|---|---|---|---|
| Red envelope assistant setting page | `ui/activity/LuckySettingActivity`（17 KB，Manifest:1095，action `miui.intent.action.HB_MAIN_ACTIVITY`，`singleTask`） | — | `CommonConfig` | `webapi/WebApiAccessHelper` | Manifest[Observed] |
| Secondary settings | `SecondarySettingActivity`（18.5 KB，action `miui.intent.action.LUCKY_MORE_SETTING`） | — | — | — | Manifest[Observed] |
| Quickly open whitelist management | `FastOpenListActivity`（18.8 KB） | `config/FastOpenConfig` | — | — | [Observed] |
| Reminder settings | `RemoveLuckyMoneyActivity`（action `miui.intent.action.LUCKY_ALARM_SETTING`） | — | — | — | Manifest[Observed] |
| Boot page | `GuideActivity` | — | — | — | [Observed] |
| **red envelope monitoring service** | Manifest:1126 `service/LuckyMoneyMonitorService` (20.4 KB, remote process) | See 6.2 | — | — | [Observed] |
| Cross-process query | Manifest:1127 `provider/LuckyMoneyProvider`（`miui.permission.EXTRA_NETWORK`） | — | — | — | Manifest[Observed] |
| Cloud control | `service/CloudControlReceiver`（3 KB） | — | — | — | [Observed] |
| Silent/Do Not Disturb linkage | `config/DoNotDisturbConstants`、`utils/SettingsUtil#isQuietModeEnable` | — | — | — | [Observed] |

### 6.2 Implementation method: **is not an accessibility service, but a "notification listening" (borrowed from GameBooster's notification agent)**[Observed]

```java
// LuckyMoneyMonitorService.java:312（onCreate）
bindService(new Intent(this, (Class<?>) NotificationListener.class), this.mNotificationListenerConn, 1);
// :73-82
public void onServiceConnected(ComponentName cn, IBinder b) {
    mNoticationListenerBinder = ISecurityCenterNotificationListener.Stub.asInterface(b);
    mNoticationListenerBinder.Z2(mNotificationListenerCallback);   // Register callback
}
```

- The listening agent class is in `com.miui.gamebooster.service.NotificationListener` / `ISecurityCenterNotificationListener` (**is the same as the GameBooster module of** in APK, not the system `NotificationListenerService`).
- Callback `onNotificationPostedCallBack(StatusBarNotification)` (`:58-72`): Filter `userId` (current user or 999/0) and hand it to `onNotificationPosted`.
- Message distribution (`:332-403`):
  - `AppConstants.Package.PACKAGE_NAME_MM` (WeChat) → `new WechatMessage(...)` → `isHongbao()` → Go to `mWeixinPipeline`
  - `PACKAGE_NAME_QQ` → `new QQMessage(...)` → `QQGroupCollector.collect(...)` → `isHongbao()` → Go `mQQPipeline`; `type == -1` go `processTypeUnknownNotification`
  - Do not remind during a call: `PhoneStateMonitor.isPhoneBusy(ctx)` (`:358`, `:393`).

### 6.3 "Auto grab" implementation (`Pipeline#process`) [Observed]

```java
// Pipeline.java:203-251
if (!appMessage.isHongbao()) return false;
if (notifyType == null || notifyType == NONE) { /* do not notify */ }
else if (!justForGroupMessage() || appMessage.isGroupMessage()) {
    if (!SettingsUtil.isQuietModeEnable(ctx)) ScreenUtil.powerOnScreen(ctx);      // ★Light up the screen
    if (z10 || !checkFastOpenMode()) {
        MessageView v = obtianMessageView(appMessage);  v.show(appMessage);       // Floating window/lock screen card
        if (!z10 && ScreenUtil.isScreenLocked(ctx)) { keyguardPipelineHistory…; lastLockScreenMessage = appMessage; }
    } else {
        new PendingIntentRunnable(appMessage.getAction()).run();                 // ★★ Automatically trigger red envelope PendingIntent
        commonConfig.setWarningLuckyMoneyCount(+1);
        MiStatUtil.recordLuckyMoneyFastOpen(...);
    }
    if (!quiet && needPlaySource()) a.c(ctx);                                     // Play tone
}
```

Key prerequisites of `Pop` `checkFastOpenMode()` (`Pipeline.java:52-64`):
```java
if (ScreenUtil.isScreenLocked(ctx) || !CommonConfig.getInstance(ctx).isFastOpenEnable()) return false;
String fg = PackageUtil.getForegroundApp(ctx);
if (shellAppMap.containsKey(fg)) fg = shellAppMap.get(fg);   // com.android.thememanager→themestore, securitycenter→securitymanager
return !FastOpenConfig.getInstance(ctx).isRestrict(fg);      // The foreground application is not in the "Quick Open Blacklist"
```

`PendingIntentRunnable` (`ui/view/PendingIntentRunnable.java:13-23`) is `pendingIntent.send()`.

→ **Conclusion [Observed]**: The so-called "automatic grabbing of red envelopes" = when the **screen is not locked + the user has turned on "Quick Open" + the current foreground application is not in the restricted list**, **directly triggers the WeChat/QQ notification that comes with it `contentIntent`** is equivalent to the user clicking on the notification; it does not simulate clicks, does not operate the UI level, and does not rely on accessibility. In the lock screen state, change it to "lock screen card + replay after unlocking" (`keyguardPipelineHistory` + `ScreenUtil.KeyguardUnlockedListener`, `Pipeline.java:126-149`).

Other mechanisms [Observed]: `PhoneStateListener` in-call deduplication (`:143-158`), `BroadcastReceiver` monitoring `ACTION_CONFIG_CHANGED` / `ACTION_SCREEN_OFF` / package installation (`:89-137`), `HeadsUpView`/`LockScreenView` (`ui/view/*`).

> [Unknown]`com.miui.luckymoney.provider.LuckyMoneyProvider` The specific protocol of `call()`/`query()` exposed; the interface address of `webapi/WebApiAccessHelper` (no obvious red envelope meaning entries were hit in `dex_urls.txt`). **infers**: Cloud Control only issues "switch/copy/restriction list".

---

## 7. `com.miui.policeassist` + `com.miui.warningcenter.policeassist` - Alarm auxiliary

(policeassist 20 classes 61.9 KB; actual principal in `warningcenter/policeassist`)

### 7.1 Function list

| Function | entrance | Core classes·methods | Data | Cloud | Evidence |
|---|---|---|---|---|---|
| settings page | `warningcenter.policeassist.PaSettingActivity`（20 KB，Manifest:520，exported，action `miui.intent.action.WARNINGCENTER_POLICE_ASSIST`） | — | `Settings.Secure` | Agreement/Privacy URL | Manifest[Observed] |
| Boot page | `policeassist.ui.PoliceassistGuideActivity` / `PoliceassistGuideLawFragment` | — | — | — | [Observed] |
| **Report location during emergency call** | Manifest:475 `warningcenter.policeassist.PaService`（23 KB，remote） | See 7.2 | Locate + de-geocode addresses | 3 channels | [Observed] |
| Floating window reminder | `PaFloatWindowManager` / `PaLocationFloatingView` / `PaPrivacyFloatingView` | Hang on InCallUI | — | — | [Observed] |
| Report to EPS (National Emergency Platform) | `com.miui.policeassist.EPSManager` | `postEPS(...)` | `EPSRequestBean` | `https://eps.caict.ac.cn/eps/collect` | `EPSManager.java:97-143`[Observed] |
| SMS channel | `PaCommon.SMS_ADDRESS = "106903590009"` | — | — | SMS | `PaCommon.java:23-24`[Observed] |

### 7.2 Process[Observed]

1. **triggers**: `PaService` registers `PhoneStateListener` (`PaService.java:75-98`); `onCallStateChanged` uses `TelephonyManager.getCallState()` to determine the status. If the conditions are not met when hanging up (`callState==0`), the boot notification will pop up and end with `mTransType` (`NETWORK`→`sendLastRequest()+stopServiceTimer()`, otherwise `stopSelf()`).
2. **number**: `GetMyPhoneNumberTask` → `PaUtils.getMyPhoneNumber(service)`; with local number → `TransType.NETWORK` and `startRequestLoop()`; **without local number → `TransType.SMS`** (`PaService.java:131-152`).
3. **locates**: `startLocation(mCallNumber)` (`:468`); `onLocationSuccess(Location)` after success; `onLocationFailed(String)` after failure.
4. **address**: `TransDataTask#doInBackground` Use `Geocoder.getFromLocation(lat,lng,1).getAddressLine(0)` (`PaService.java:182-187`) - **reverse geocoding to turn the coordinates into text address**.
5. **Channel A: Network** (`PaCommon.java:17-18`)
   - `NET_ADDRESS = https://gateway-papa.cloud.weizhi.com/papa/upload`
   - `NET_XIAOMI_CHANNEL = "3016"`，`NET_XIAOMI_CHANNEL_NAME = "xiaomi"`
   - Encryption and decryption materials: `AES_ENCRYPT_KEY = "HPLvLMJnR6vvuS9VNkpj7DYb"`, `RSA_ENCRYPT_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCsPsxtHUmeUiBfvdGqBIJQUloI52iMczf/5glfBgjo3NCV5tMq4bV4mUV4g6/tEkbh+0N2xcavZWnDVNpCDrYFQcVupICPwSdS3wZyoWogt9RdoLs7lnQ4l7OPWNjweMqDE2b8gacmA+aebDgSb3wqT8iNIh+yjvgwpq9s88u+cQIDAQAB"（`AesCbcUtils.java` implementation)
6. **channel B: EPS national platform** (`EPSManager.java`)
   - `EPS_NET_ADDRESS = "https://eps.caict.ac.cn/eps/collect"`，`EPS_PRIVATE_KEY = "97782f2e2e75445180d664fb5705a8a5"`，`EPS_XIAOMI_CHANNEL = "17"`
   - Request body (`EPSRequestBean` full field, `EPSRequestBean.java:5-19`): `source/caller/called/device/method/type/lng/lat/hAccuracy/vAccuracy/altitude/address/time/extra/sign`
   - Signature algorithm (`EPSManager.java:35-47`): Sequentially spell `TreeMap` into `k=v&…` and then append `key=EPS_PRIVATE_KEY`. The whole string is made into **SM3** and converted to hex (`sm3/SM3Digest.java` + `Sm3Utils`); `caller` is also SM3 hex (`:52`)
   - `called` coding: `"0"+method+lng+lat` form splicing (`:53-57`, `LOCATION_EXCEPTION="0"`/`LOCATION_GPS="1"`/`LOCATION_NET="2"`)
   - Retry 3 times, interval 1000 ms (`:103-120`)
7. **switch**: `Settings.Secure` `com_miui_warningcenter_eps_status` (EPS) and `com_miui_warningcenter_pa_status` (overall); `getEPSPoliceAssistToggle` requires `==1` (`EPSManager.java:82-84`, `PaCommon.java:10-11`).
8. **floating window**: `miui.process.IForegroundInfoListener` monitors the frontend package name, and only retains the floating window (`PaService.java:99-112`) when the frontend is InCallUI (`isInCallUI(pkg)`).

### 7.3 Privacy impact (based on code facts, no value judgment)

| facts | influence | Evidence |
|---|---|---|
| Automatically obtain accurate latitude and longitude when call ends/emergency call | The location accuracy depends on `Location.getAccuracy()`/`getVerticalAccuracyMeters()`, and **is reported to the third-party domain name** | `PaService.java:182`、`EPSManager.java:59-70`[Observed] |
| Reverse geocoding to obtain the "text address" and report it | More direct address inference than bare coordinates | `PaService.java:182-187`[Observed] |
| The reporting target is `weizhi.com` ("location" pinyin, non-Xiaomi domain name) + `caict.ac.cn` (China Academy of Information and Communications Technology) | Data export/third-party sharing | `PaCommon.java:6,17`[Observed] |
| The hard-coded AES password and RSA public key are in the assets sibling code | Transmission encryption strength is limited by hardcoded keys | `PaCommon.java:5,22`[Observed] |
| When there is no local number, it is downgraded to **. Plain text message** is sent to `106903590009`. | The address is transmitted via the operator's SMS channel | `PaService.java:141-152`、`PaCommon.java:23`[Observed] |
| Requires `IForegroundInfoListener` (process-level activity visibility) and floating window permissions | Ability to permanently sense front-end applications | `PaService.java:99-112`[Observed] |
| The default switch is "off" (`Settings.Secure.getInt(..., 0)`, turn `-1` off) | Requires user to explicitly enable it | `EPSManager.java:82-84,122-124`[Observed] |

---

## 8. `com.miui.tvm` - TrustZone/TEE virtual machine; `com.miui.securitykey` - local key

### 8.1 tvm (13 classes, 68.1 KB)

| Function | entrance | core | Data | Evidence |
|---|---|---|---|---|
| TVM management service (AIDL, `miui.permission.TVM`) | Manifest:476 `com.miui.tvm.service.TvmManagerService` (**`enabled=false`**, dynamically enabled by `Application#w()`) | `ITvmManagerImpl`(16.8 KB) / `ITvmManager`/`ITvmManagerCallback` | — | [Observed] |
| TVM system image download/installation | `TvmManager#b(url)` | `DownloadManager` + `setFileSize(140509184)`(≈134 MB) | — | `TvmManager.java:105-134`[Observed] |
| state machine | `TvmManager#q(ctx)` → `fh.c#x(ctx)` | 0=Not installed, 1=Upgradeable, **2=Downloading**, 3=Ready, 4=TVM running, 6=Conditions not met, 7=Mirror damaged | `vendor.*` system properties | `fh/c.java:756-783`[Observed] |
| status bar/dialog box | `ui/TvmDialogActivity`（`AlertDialog.Theme.DayNight`） | `TvmManager#y(ctx,status)` | — | `TvmManager.java:98-103`[Observed] |
| TrustedUI call | `fh.c#p()` → `IMiTrustedUIService` | — | — | `fh/c.java:627-629`[Observed] |
| Mirror installation | `IMTService`（`vendor.xiaomi.hardware.aidl.mtdservice.IMTService/default`） | `TvmManager#i()` | — | `TvmManager.java:89-91`[Observed] |
| whitelist | `assets/tvm_whitelist.json` | `fh.c#E(ctx)` | — | `fh/c.java:109-112`[Observed] |

**Dynamic enablement logic** [Observed] (`Application.java:660-668`)
```java
private void w() {
    if (Build.IS_INTERNATIONAL_BUILD) return;
    String strC  = x4.v1.c("vendor.xiaomi.trustedvm.version", null);
    String strC2 = x4.v1.c("ro.vendor.mitee_tui.support", null);
    getPackageManager().setComponentEnabledSetting(
        new ComponentName(this, "com.miui.tvm.service.TvmManagerService"),
        (TextUtils.isEmpty(strC) && TextUtils.isEmpty(strC2)) ? 2 : 1, 1);   // 2=DISABLED 1=ENABLED
}
```

System attribute consumption point (`fh/c.java:785-793`):
```java
public static String y()  { return v1.c("vendor.xiaomi.trustedvm.version", null); }
public static boolean z() { return "RUNNING".equalsIgnoreCase(v1.c("vendor.qvirtmgr.trustedvm.status", null)); }
```

> [Inferred]TVM = Xiaomi's "trusted virtual machine" running on TEE/virtualization layer provides the security center with trusted UI (enter password/biometric confirmation) and trusted environment capabilities; this APK only does "image version comparison + download and installation + status display".
> [Unknown] The actual loading/verification process of the image (in the `IMTService` implementation on the vendor side); there is no corresponding .so in the `lib` directory, indicating pure AIDL calling.

### 8.2 securitykey (4 classes, 4.1 KB) - **only uses `libsec-common-utils` to get the key**

`com.miui.securitykey.ConfigManager`（`ConfigManager.java`）[Observed]：

```java
static { System.loadLibrary("sec-common-utils"); }   // :42
private static native String getDecodeSecretKey();   // getDecodeKey()
private static native String getEncodeSecretKey();   // getEncodeKey()
private static native String getHttpKey();           // getSecurityHttpKey()
private static native String getNetworkAssistantKey(); // getNetworkAssistantEncryptKey()
private native String getRubbishSecurityKey(Context, boolean);  // getRubbishKey(ctx[,true])
private static native String getUuidKey(Context, Boolean);      // getUuidAesKey → UuidAesKey{key,iv}
```

- `getUuidAesKey` cuts the `"<key>-<iv>"` returned by native into 2 segments (`ConfigManager.java:83-105`) according to `-` for use by AES.
- [Observed]**This is the only use** of `libsec-common-utils` in the whole APK (there are only 6 places of `System.loadLibrary` in the whole tree: `sec-common-utils` / `jni_load_serinum` / `tcp` / `lib-riskcontrol` / `fastplayer`+`miffmpeg` / `tensorflowlite_jni`).
- [Unknown] The derivation of the key ontology in native (requires `open_binary` analysis `lib/arm64-v8a/libsec-common-utils.so`).

---

## 9. `com.xiaomi.security` (10 categories)

| Function | class | Notes | Evidence |
|---|---|---|---|
| XSOF Capability Negotiation | `xsof/XSOFProvider`（Manifest:1758, exported） | `call("getServiceVersion")` and the caller holds `com.xiaomi.security.permission.ACCESS_XSOF`, return `version_name="1.0"`, `version_code=1` | `XSOFProvider.java:25-32`[Observed] |
| Security testing services | `xsof/MiSafetyDetectService`（Manifest:1759, exported, remote） | `MiSafetyDetectImpl extends IMiSafetyDetectServer.Stub`; Each API call is first verified by `Binder.getCallingPid()` → package name → `gj.e.a(ctx, pkg, "com.xiaomi.security.permission.ACCESS_XSOF")`, and then verified that the caller is consistent with the package name declared by itself; error codes such as `1006`, `AnalyticsListener.EVENT_VIDEO_INPUT_FORMAT_CHANGED`(1004) | `MiSafetyDetectService.java:52-90`[Observed] |
| Device Credentials | `devicecredential/*`（`ISecurityDeviceCredentialManager` 6.1 KB） | Credential management AIDL (matched with `credential_service` system settings, see `Application#a0`) | [Observed] |

> [Unknown]XSOF The semantic number (obfuscated names such as `C3`/`d8`) and return value enumeration of each API need to be read from `hj.*`, `ij.*` and other support classes; this time it is not expanded.

---

## 9.5 Attachment: `com.miui.msa` / `com.miui.sec` / `com.miui.support` (junction with main scan)

| class | facts | Relationship to this module | Evidence |
|---|---|---|---|
| `com.miui.msa.util.MsaUtils` | Just do one thing: return the MSA (Xiaomi advertisement SDK) package name - if the international version is installed, then `com.miui.msa.global`, otherwise `com.miui.systemAdSolution` | [Inferred] Ad attribution; `assets/placement_id` (`123=fb:196567740764356_196569107430886`) is the Facebook Audience Network ad slot ID | `MsaUtils.java:9-38`[Observed] |
| `com.miui.sec.external.InstallProvider` | `call()` is only forwarded to super when `IS_INTERNATIONAL_BUILD && SDK>=25`, otherwise null is returned; `insert/query/update/delete` is all empty | **empty shell Provider** (compatible with international version installer) | `InstallProvider.java:12-60`[Observed] |
| `com.miui.support.provider.MiuiSettingsCompat$SettingsCloudData` | **unified cloud configuration reading port**: `content://com.android.settings.cloud.CloudSettings/cloud_all_data` (and `/single`, `/notify`); `CloudData` is `Parcelable`, internal lazy analysis `JSONObject` | ★ Used by antispam to cover `white_url.json` (`MiuiSettingsCompat.getCloudDataString(cr,"antispam_white_url","content",null)`, `m2/e.java`) and `service_num.json`; the same applies to other modules (alarm/traffic) | `MiuiSettingsCompat$SettingsCloudData.java:15-54`[Observed] |
| `com.miui.support.cardview.*` / `drawable.*` | Card shadow/stroke self-drawing (`CardDrawable` 7.8 KB, `CardStateDrawable` 13.4 KB) | Pure UI, nothing to do with scanning | [Observed] |

> [Observed] Advertising related: Homepage `MainFragment.f20481n` is `NativeInterstitialAdLayout` (interstitial native advertising card), which will receive safety scores and change colors according to thresholds (§2.4); `securitycenter/ad/view/*` (`AdDownloadView`/`AdImageView`/`RoundImageView`/`RoundResizeFrameLayout`) is an advertising material rendering component.
> [Unknown] The server pull interface of advertisement SDK does not appear in the entry of this module of `dex_urls.txt` (`adv.sec.miui.com/*` belongs to other modules such as GameBooster/theme).

---

## 10. Cloud interface summary (within the scope of this document)

| Purpose | Method with URL | Request fields | response fields | Evidence |
|---|---|---|---|---|
| **SMS Rule package update** | POST `https://api.sec.miui.com/GuardProviderV1/GetResourceZipFile` | `app_version`, `ver="2.1.2"`; header with fixed appKey `7htr5238-a8cf-3k79-ec73-75382145ns5c` | `code`, `data.fileUrl`; including MD5, `6206c9f17a64246498b52e117d0cff1e` is required | `j2/a.java:261-276`、`:166-173`[Observed] |
| **cloud number database incremental synchronization** | POST `https://api.sec.miui.com/harassIntercept/intercepts` | `dataVersion`；appKey `5cdd8678-cddf-4269-ab73-48387445bba5` | `version`、`v[{status,type,uuid,p,attributes}]` | `k2/a.java:65-130`[Observed] |
| **cloud URL detection (fishing determination)** | POST `https://url-detect.engine.mi.com/url/detect` | `url`、`ts`、`c=2`、`sign=md5(…+"2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9")`；UA `antispam/1.0.0` | `code`、`data.urlCategoryId`（-1/0/1/2） | `policy/b.java:135-166`[Observed] |
| Reducing the screen to clear the anti-kill list | POST `https://api.sec.miui.com/trashCleaner/screenLock` | `param=Base64({"ver":n,"isDiff":true})`；appKey `5cdd8678-cddf-4269-ab73-48387445bba6` | `version`、`items[]` | `CloudDataUpdateService.java:30-60`[Observed] |
| Game joint transport judgment | POST `https://flash.sec.miui.com/game/union` | `pkg`（form） | `isUnionGame`, `frequency` (seconds, default 259200) | `GuardService.java:330-352`[Observed] |
| **Alarm Assist-Third Party** | POST `https://gateway-papa.cloud.weizhi.com/papa/upload` | AES/RSA encrypted payload (see §7.2) | — | `PaCommon.java:17-22`[Observed] |
| **Alarm Assist-Institute of Information and Communications Technology EPS** | POST `https://eps.caict.ac.cn/eps/collect` | `EPSRequestBean` 14 fields + SM3 `sign` | `status==200` considers it successful | `EPSManager.java:97-143`[Observed] |
| Privacy Policy/Agreement | `https://privacy.mi.com/emergency_location/`、`https://terms.miui.com/doc/alarmassistance.html?lang=` | — | — | `PaCommon.java:20-21`[Observed] |
| **virus cloud killing** | (**URL was not found in** in this APK) | — | — | [Unknown] |

Other related `api.sec.miui.com` paths not expanded this time (from `dex_urls.txt`, belonging to other modules): `/data/check` (monthly report), `/v1/traffic/report`, `/universalWhitelist/checkUserId`, `/common/whiteList/*` , `/health/v1/getOptimizationFile2`, `/earthquake/*`, `/redPacket/reportStatus`, `/LBS/geoconv/admin`, `/mnntrain/taskdata`, `/12379/warning/getRegionInfo`.

---

## 11. Persistence master table (within the scope of this document)

| Type | Name | Belong | content |
|---|---|---|---|
| SP | `remote_provider_preferences` | Full module (remote process) | See 0.3 + §3.5 + §4 |
| SP | `SmsEnginePreference` | antispam(SMS engine) | `j2/a.java:316` |
| SP | `data_config` | antivirus results page | `result/ScanResultFrame.java:223` |
| SP | `credential_service_config` | securitycenter | `Application.java:542` |
| SP | `sp_apppredict`, `autoTaskSp`, `gb_gamead_data_config`, etc. | Non-this module (other teammates) | — |
| **SQLite** | `AntiSpam`(v10)：`fwlog`/`phone_list`/`keyword`/`mode`/`sim`/`reportSms`/`reportSmsPending` | antispam | §4.7 |
| **SQLite(asset)** | `assets/garbage_clean.db`：`t_cache`/`t_ad`/`t_residual` | Clean up (other modules, baseline has been recorded) | — |
| File | `files/scanitem.json_v{0,1}` | securityscan | The physical examination list distributed by the cloud covers assets |
| File | `files/resource.zip` → `files/SmsEngineResource/{1.bin,2.bin,v.txt,…}` | antispam | §4.4 |
| File | `files/anti_fraud_pay_activity` | antifraud | Overwrite assets files with the same name |
| File | `files/service_num.json`, `files/white_url.json`, `files/dict`/`pattern`/`phish` (`lj.c#b` gives priority to `openFileInput`) | antispam | `lj/c.java:42-57`、`m2/e.java` |
| File | `Download/resource.zip.tmp` | antispam download relay | `j2/a.java:43,166` |
| Provider | `content://com.miui.securitycenter.remoteprovider` (`remote_provider_preferences` + `whitelist` table) | overall situation | `RemoteProvider.java:34-38` |
| Provider | `content://antispam/{sms_judge,call_judge,url_judge,service_num_judge,call_transfer_intercept_judge,phone_list,log,log_sms,logconversation,keyword,mode,sim,unsynced_count,synced_count,allow_repeat,account,markednumber,cloudantispam,category,report_sms,report_sms_pending}` | antispam | `AntiSpamProvider.java:59-92` |
| Provider | `content://guard/{support_incremental,network_set}` | External GuardProvider | §3.2 |
| Provider | `content://com.miui.riskcontrol` (`call` only) | riskcontrol | §5.2 |
| Provider | `content://com.miui.luckymoney.provider` | luckymoney | Manifest:1127 |
| Provider | `content://com.xiaomi.security.xsof.provider/XSOF` | XSOF | `XSOFProvider.java:21` |
| Settings.Secure | `fraud_num_state*`/`agent_num_state*`/`sell_num_state*`/`harass_num_state*`/`sms_classifier_*`/`*_mode`/`backsound_index*`/`is_call_transfer_blocked*`/`show_notification_type*`/`com_miui_warningcenter_(eps|pa)_status`/`key_latest_virus_scan_date`/`security_adb_install_enable`/`default_input_method`/`accessibility_button_targets` | Each module | See sections |
| System properties (read) | `vendor.xiaomi.trustedvm.version`、`vendor.qvirtmgr.trustedvm.status`、`ro.vendor.mitee_tui.support`、`persist.sys.miui_optimization`、`ro.miui.cts`、`ro.mi.os.version.code` | tvm/job scheduling | [Observed] |

---

## 12. Unsolved mysteries (requires follow-up special projects)

1. The real channel for **virus cloud scanning**: `preference_key_open_virus_cloud_scan` Which interface should you use after opening it? This APK has no cloud search method for URL and `IAntiVirusServer` → it is very likely that `com.miui.guardprovider` APK will be connected to the Internet by itself. You need to get the GuardProvider to make a conclusion.
2. Consumer **of**`assets/dict` / `assets/censor_source`: The Java layer only has `lj/g.java:32` reading `dict` (SMS word segmentation); `censor_source` has no Java reference. Whether it is read by `liblib-riskcontrol.so` through `setAssetManager` - it must be natively analyzed.
3. **`assets/model`(680 KB) and `rec_model_20230425.mnn`(810 KB)**: This module has zero references. The consumer is unknown.
4. Determination and reporting of **`com.miui.antifraud.d` (13.9 KB)**: Only the entrance (PHONE_STATE / SCAN_RISKY_URL) and page monitoring are confirmed, but the complete rules and reporting payload are not seen.
5. **`liblib-riskcontrol.so` Internal**: the source of the sensitive word list, whether it is implemented, and whether there is a remote dictionary.
6. **TVM image installation/verification**: `IMTService` is vendor implementation, this APK is only AIDL client.
7. **`assets/pattern` encoding**: The file is stored in GBK/mixed encoding, `lj.c` uses `new InputStreamReader(stream)` (**platform default encoding**, not explicitly specified), the Chinese pattern may be misread in some locales - This may be a potential flaw of "the rule fails on some devices" (**infers**).
8. `censorResult` URI**of**`SensitiveWordDetectProvider`: only placeholders, no actual query implementation (`query()` always returns null).

---

## Appendix A: Single piece of evidence index (review on demand)

| Topic | file:line |
|---|---|
| process dispatch | `com/miui/securitycenter/Application.java:683-842` |
| IdleHandler heavy business | `Application.java:820-825` → `K()` `:361-410` |
| TVM component dynamic switch | `Application.java:660-668` |
| Keep alive (reflection setProcessImportant) | `com/miui/securitycenter/service/RemoteService.java:38-49` |
| Boot broadcast | `com/miui/securitycenter/receiver/BootReceiver.java:80-118` |
| Cross-process preferences | `r4/a.java:19-200` + `com/miui/common/persistence/RemoteProvider.java:40-284` |
| Physical examination checklist | `apk_full/assets/scanitem.json` + `securityscan/model/ModelFactory.java:53-229` |
| Scan layout | `securityscan/scanner/k.java:808-960` |
| Score | `securityscan/scanner/ScoreManager.java:112-220,445-509` |
| Virus scan target enumeration | `securityscan/scanner/o.java:710-769` |
| GuardProvider binding | `p9/a.java:136-192` + `com/miui/guardprovider/aidl/IAntiVirusServer.java:561-607` |
| Virus database job | `antivirus/service/VirusAutoUpdateJobService.java:197-236` |
| GuardService main logic | `antivirus/service/GuardService.java:385-410,638-800` |
| SafePay Rule Assets | `apk_full/assets/safepay_calling_activity`、`safepay_target_activity`、`safepay_target_package`、`safepay_imm_whitelist` |
| Interception strategy chain | `h2/e.java:113-159` + `b2/b.java:81-169` |
| System number/keyword library reading | `h2/b.java:84-156` |
| pattern parsing | `nj/d.java:61-95`、`nj/c.java:51-123`、`nj/e.java:70-97` |
| SMS TFLite engine | `da/a.java:31-117`、`aa/a.java`、`aa/c.java`、`ca/a.java`、`h2/a.java:234-331` |
| Rule package download/verification | `j2/a.java:133-276` |
| Cloud number library | `k2/a.java:45-130` |
| URL intercept | `com/miui/antispam/policy/b.java:135-267` |
| antispam DB | `e2/a.java:116-148` + `res/values/strings.xml`(`create_table_*`) |
| protobuf backup | `apk_full/assets/protobuf/{antispam,syncroot}.proto` + `com/miui/antispam/service/backup/SecurityCenterBackupAgent.java:30-171` |
| Anti-fraud entrance | `com/miui/antifraud/AntiFraudReceiver.java:20-40`、`PayActivityMonitorService.java:85-127` |
| Sensitive word Provider | `com/miui/riskcontrol/SensitiveWordDetectProvider.java:19-79` |
| Red envelope monitoring | `com/miui/luckymoney/service/LuckyMoneyMonitorService.java:58-100,293-403` |
| Red envelope "Open quickly and click automatically" | `com/miui/luckymoney/controller/Pipeline.java:52-64,203-251` + `ui/view/PendingIntentRunnable.java:13-23` |
| Alarm assistance | `com/miui/warningcenter/policeassist/PaService.java:75-200,434-520`、`PaCommon.java`、`com/miui/policeassist/EPSManager.java` |
| TVM | `com/miui/tvm/TvmManager.java`、`fh/c.java:756-793` |
| local key | `com/miui/securitykey/ConfigManager.java:41-107` |
| XSOF | `com/xiaomi/security/xsof/XSOFProvider.java`、`MiSafetyDetectService.java:52-90` |
