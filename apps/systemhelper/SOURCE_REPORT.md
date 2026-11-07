> **Historical analysis with subsequent corrections.** Read the [final review](REPORT.md) first. The review narrows or corrects earlier claims about MD5-derived keys, full installed-app inventories, the reachability of privacy notices, one-second upload intervals, timestamps, and confirmed replay. Some OEM branches remain outside the follow-up review. Local identity paths and actual user data are excluded.

# SystemHelper (com.mobiletools.systemhelper) complete reverse analysis report

> Target: `[LOCAL_PATH_REMOVED]`
> Analysis method: REA (MCP, v6.8.0) Android/JADX static analysis + apktool 3.0.3 full decompilation + four-way parallel in-depth analysis (Lead summary and cross-check)
> Authoritative agreement: **[Observed]**=The bytecode/resource is directly visible; **[Inferred]**=Inferred from the calling relationship or string semantics; **[Unknown]**=There is no evidence in this sample.

---

## 0. One sentence conclusion

This is a **pre-installed (/system/priv-app) China Unicom terminal information collection/registration component** (display name "SystemHelper", internal self-proclaimed "operator service"): resident in the name of "Device Management (DM)", after booting through the AlarmManager timing (4 minutes first start / 24 hours regular / 7 days application list), **dual card IMEI, IMSI, ICCID, MSISDN, current cell CellID/LAC, VoLTE and 5G switch status, model manufacturer version, storage and memory images, and application usage and traffic statistics in the last 7 days** is serialized into JSON, using `AES/MD5/RSA` After triple processing, **in plain text HTTP** POST to `http://dm.wo.com.cn:18080/registerv3` (China Unicom DM platform, port 18080), protocol version `SpecVersion=3.0`, `RegVersion=1.0`.

The engineering implementation quality and security strength are very low: built-in **RSA-1024 self-signed "CN=UNICOM" certificate** (valid until 2119), AES key is **'s own plain text MD5** The AIDL service **that is derived (equal to not encrypted), exported and** has no permission protection can be rewritten by any third-party application. The reporting address/reporting period, the privacy notification string **zero reference** in the code, and the desktop icon is the **system Android robot**.

---

## 1. Sample identity and analysis methods

### 1.1 Sample identity[Observed]

| item | value |
|---|---|
| file name | `SystemHelper-1.2-20230925-U.apk` |
| SHA-256 | `efa1af0d0be75985701fd0eca79a90f0be0bb9fd2735245915200ec1a3e9657f` |
| size | 911,821 bytes |
| package name | `com.mobiletools.systemhelper` |
| versionName / Code | `1.2-20230925-U` / `3` |
| minSdk / targetSdk / compileSdk | 24 / **28** / 33 |
| sharedUserId | `com.mobiletools.systemhelper.uid`，`sharedUserMaxSdkVersion=32` |
| application class | `com.mobiletools.systemhelper.control.DMApplication` |
| Others | `usesCleartextTraffic=true`、`allowBackup=false`、`extractNativeLibs=false`、`appComponentFactory=android.support.v4.app.CoreComponentFactory` |

### 1.2 Signature[Observed]

- Only **v2** scheme (no v1/v3/v3.1), 1 signer, `self_consistent=true`; no key rotation pedigree, no source stamp.
- Signing certificate SHA-256: `874b8ca2297f53a373b1b935c134cb0eb1ff7381e54491b88c643a6b9c46db77`
- The **signature has not been cryptographically verified by** (REA is clear: `signature_verification=not_performed`). `self_consistent` only means "the statement summary is consistent with the current byte" and does not constitute a proof of authenticity.
- There is no evidence in this sample to prove the identity of the signer **and "China Unicom Official"** (see §11).

### 1.3 Analysis tool chain and evidence

| link | Tools/Operations | Evidence ID (REA) |
|---|---|---|
| packages/components/permissions | `inspect_android_package` | `ev_d49242be02b911af209753b080d8a8ea43ddf1342c2017f8f52f07c1f14ade1c` |
| Decoded Manifest (4380 characters full text) | `inspect_analysis_view{kandroid,facet:manifest}` | `ev_ae62a3b5e3f0a068aa535829c6ab72d400ba6b63dce66b6bdb14e0ac8082e8b7` |
| List of 409 items in the package | `inspect_artifact` + inventory view | `ev_c1904f4e2b323b3a64d21db0f4e1e8df2255f95998339357169de424d68bdd33` / `ev_39fad11288bc176284d5be5e43aacd9ebc5ebf210c33e6f4373e0af6dfd43445` |
| Signature structure | `inspect_apk_signature` | `ev_8001f625fd02e3bace98a316edbbbf5fc4d9926c7807d5f0bdfd69d90e97c415` |
| Full list of 476 top-level categories | `search_android_classes` | `ev_0235e39e76b9d6b6307467e688dddea1a11fe9d0d1d07729b1d3fe46076a57f1` |
| Resources and own strings | `decode_android_resources`（apktool 3.0.3） | `ev_9d7c3abf81048f4137b6fa92c5847ac8309c75f91741b840f83cdde591432cdf` |
| Report thread Java source code | `inspect_android_method d0.k.run` | `ev_408dcf228ff95090d81dc7686f653e63aec9b70366ceea9e59bc914871e29a85` |
| Global configuration fields (28 static fields) | `inspect_android_class f0.f` | `ev_15bbb4403c5e6a0319d2c0b14030a062de71f838d038a0e0e6daabc7076e1474` |
| The rest of the conclusions | apktool full smali (783 files) + cross-check | See section `File:line number` |

**decompilation products**: `analysis\SystemHelper\apktool\` (smali/res/assets/original/unknown), `all_const_strings.txt` (1880 deduplicated constants), `method_index.txt` (complete method signature index of 387 non-library classes). See `findings\` for the four sub-reports.

### 1.4 Coverage and Limitations

- Coverage: `com.mobiletools.*` 15 classes, `e0/*` 85, `d0/*` 15, `f0/*` 15, several tool/constant classes; **own code 100% Check** category by category (library codes: `android.support.*`, `androidx.*`, `android.arch.*`, a total of 390+ files are AppCompat/ConstraintLayout/life cycle library, only identified but not expanded).
- **did not execute**: APK has never been run (no adb/simulator involved), all behaviors are static inferences; there is no real machine verification.
- The `res/**/*.xml` Chinese decoded by apktool is garbled (due to round-trip encoding by the tool). The original Chinese text of **is subject to the decoding results of REA `decode_android_resources` and `resources.arsc`**.
- String search boundary: `all_const_strings.txt` only covers the `const-string` instruction; `static final String` compile-time constants and splicing strings may be missing (the `f0/f` static field has been used to fill in key constants).

---

## 2. Application composition

### 2.1 Manifest full text highlights [Observed]

```
<permission android:name="com.mobiletools.systemhelper.permission" android:protectionLevel="signature"/>
<uses-permission INTERNET, RECEIVE_BOOT_COMPLETED,
  ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION,
  READ_EXTERNAL_STORAGE, ACCESS_NETWORK_STATE,
  READ_PHONE_STATE, READ_PRIVILEGED_PHONE_STATE,
  PACKAGE_USAGE_STATS, QUERY_ALL_PACKAGES, SCHEDULE_EXACT_ALARM/>
```
- Declared signature-level custom permissions `com.mobiletools.systemhelper.permission` - **and zero use of** in the entire project (neither `uses-permission` nor `android:permission` added to any component), which is a dead statement.
- `READ_PRIVILEGED_PHONE_STATE` and `PACKAGE_USAGE_STATS` are not available for normal applications and must be granted by the system (see §2.5).

### 2.2 Component List[Observed]

| Type | Name | exported | filter action |
|---|---|---|---|
| activity | `Activity.TestActivity` | **true** | `com.mobiletools.systemhelper.activity.testactivity` |
| receiver | `control.OnBootReceiver` | **true** | `BOOT_COMPLETED`、`QUICKBOOT_POWERON`（`priority=2147483647`） |
| receiver | `control.SIMStateReceiver` | **true** | `SIM_STATE_CHANGED` |
| receiver | `control.ConnectStateReceiver` | **true** | `CONNECTIVITY_CHANGE` |
| receiver | `control.ScheduleRegisterReceiver` | false | — |
| receiver | `control.AppInfoRegisterReceiver` | false | — |
| service | `service.RegisterHelperService` | **true** | `com.mobiletools.systemhelper.service.IRegisterHelperService` |
| service | `service.RegisterIntentService` | false | — |
| service | `service.RegisterJobService` | false（`android:permission=BIND_JOB_SERVICE`） | — |

### 2.3 Package contents[Observed]

- Single `classes.dex` (979,860 B), **476 top-level classes**, 783 smali files; **has no `lib/` native library** (pure Java/Kotlin-free).
- There are 408 resources (most of them are library resources, and the `res/` file name is shrunk by R8 into `-A.png`, `0H.9.png`, etc.).
- **’s only natural asset**: `assets/pub.cer` (596 B, see §6.2).
- `META-INF/com/android/build/gradle/app-metadata.properties`：`appMetadataVersion=1.1`、`androidGradlePluginVersion=7.4.1`。
- `original/AndroidManifest.xml` = original binary manifest (8120 bytes); `unknown/` = 22 `META-INF/*.version`.

### 2.4 Permission usage comparison [observation + inference]

| Permission | Practical use (evidence) |
|---|---|
| INTERNET | Report (`f0/g`), get public IP (`d0/h`, not enabled in this version) |
| RECEIVE_BOOT_COMPLETED | Self-starting at boot (`OnBootReceiver`) |
| ACCESS_FINE/COARSE/BACKGROUND_LOCATION | Positioning: `LocationManager.requestLocationUpdates` of `d0/f$a` (this version is closed by the switch) + part of OEM reader takes `getLastKnownLocation` (library code residual path) |
| READ_PHONE_STATE | IMEI/IMSI/ICCID/MSISDN/cell (`e0/r`, OEM reader) |
| READ_PRIVILEGED_PHONE_STATE | Same as above, used to read without `READ_PHONE_STATE` runtime dialog box (requires system grant) |
| PACKAGE_USAGE_STATS | `UsageStatsManager.queryUsageStats` application usage statistics for `d0/i` |
| QUERY_ALL_PACKAGES | `d0/i` Traverse installed applications (required for Android 11+) |
| ACCESS_NETWORK_STATE | `f0/l.l()`/`o()` Determine the network type (Mobile/WiFi/Disconnected) |
| READ_EXTERNAL_STORAGE | The actual read path is not seen (`f0`/`e0` does not appear `getExternalStorage*` read) → suspected redundancy [Inferred] |
| SCHEDULE_EXACT_ALARM | `f0/a` of `setExactAndAllowWhileIdle` |

### 2.5 privapp whitelist (key design evidence) [Observed]

`res/xml/motosystemhelper.xml`：
```xml
<permissions>
  <privapp-permissions package="com.mobiletools.systemhelper">
    <permission name="android.permission.PACKAGE_USAGE_STATS"/>
    <permission name="android.permission.READ_PRIVILEGED_PHONE_STATE"/>
  </privapp-permissions>
</permissions>
```
This file **is not referenced by Manifest** (no references such as `android:networkSecurityConfig`/`android:permission`), and is a whitelist for the ROM integrator to put into `/system/etc/permissions/`. **Conclusion: The design and deployment form of this APK is the operator-customized firmware component** pre-installed into `/system/priv-app/`, rather than being distributed by an application store.
(Contrast: `res/xml/network_security_config.xml` exists but is also not referenced - the one that really takes effect is `usesCleartextTraffic=true`.)

### 2.6 Resources, icons and "disguise"[Observed]

| item | content |
|---|---|
| `app_name` | `SystemHelper` (English, `values-zh-rCN` **does not cover**) |
| `setting_advice_title` | `Operator Service (SystemHelper)` |
| `setting_advice_content` | `The operator may collect data such as your device information through WLAN or mobile data network. Turning it on is deemed to be consent to networking, access to application lists and positioning. ` |
| Own layout | Only `activity_test.xml`: ConstraintLayout → ScrollView → TextView (id `tv_device_info`, default copy "Device Info"); **does not have any switch/setting control** |
| Topic | `AppTheme` = Android Studio default template (`#3f51b5/#303f9f/#ff4081`) |
| icon | 5 sets of density `ic_launcher`(+`round`), pattern **green Android robot (bugdroid)**, no brand/operator semantics |
| string reference | **`setting_advice_title` / `setting_advice_content` has zero reference to** in smali and res, and the privacy notice is never reachable |

---

## 3. Overall architecture

### 3.1 Stratification[Observed]

```
com.mobiletools.systemhelper.* ← Shell: Application / 4 Receiver / 3 Service / 1 Activity
        │  (trigger)
        ▼
e0/c2 DeviceManager: RegType arrangement + vendor Reader selection + reporting trigger
e0/r DeviceInfoReaderBase: Collection template (VoLTE/5G/cell/operator/dual card writing)
e0/* 8 API Bridges + 33 OEM Readers (Samsung/Xiaomi/OPPO/vivo/HUAWEI/...)
        │  (write data)
        ▼
d0/g DeviceInfo DTO (26 String + 3 JSON containers + 2 int) → JSON serialization
        │
        ├─ d0/e PhoneStateListener (mask 0x100500)
        ├─ d0/f  TelephonyCallback（API≥31；SignalStrengths/CellInfo/DisplayInfo）
        ├─ d0/h IP collection thread (GET/public/ip) - This version is closed by the switch
        ├─ d0/i UsageStats + NetworkStats thread (7-day window)
        ├─ d0/j signal history loader (works within the constructor, no run())
        └─ d0/k is the only reporting thread
        ▼
f0/g    HTTPS/HTTP POST（Content-Type: application/encrypted-json）
f0/c MD5 / AES-128-ECB / Base64 package; f0/k RSA-1024 (assets/pub.cer)
f0/d SQLite systemhelper (failure retry queue, ≤100 items)
f0/l SharedPreferences (8 types of keys)
f0/a AlarmManager package (precise timing of 4 receivers)
f0/f global static configuration (28 domains, including default URL and three cycles)
```

Support library (identification conclusion, not application logic): `f/g/h` = ConstraintLayout solver; `q` = ArrayMap/SimpleArrayMap; `r/x/y/t` = AppCompat (MenuInflater/WindowCallback/AppCompatActivity); `l/o/p/k` = TypefaceCompat/FontResources/PrecomputedText;`s/m/i/w/v/j` = accessibility/DrawableCompat/ResourceTools;`b` = LinkedHashMap;`a` = TaskExecutor;`u/a..u/j` = **AppCompat's R Classes** (attr/id/layout/string/styleable…), **are not protocol constants** (early speculation has been corrected on this point).

### 3.2 End-to-end data flow (time series) [observation + inference]

```
BOOT_COMPLETED
  └─ OnBootReceiver.onReceive
       ├─ Read pref preference_enable_log → f0/f.a
       ├─ SDK≥30 → e0/r.k(ctx) (Create TelephonyCallback listener)
       └─ action==BOOT/QUICKBOOT
            ├─ f0/a.b(ctx, 240000ms) → Enter OnBootReceiver again after 4 minutes (delayed first boot)
            ├─ f0/a.d(ctx, scheduleInterval=24h) → ScheduleRegisterReceiver self-renewal
            └─ f0/a.a(ctx, appInfoInterval=7d) → AppInfoRegisterReceiver self-renewal

OnBootReceiver 4 minutes later (non-boot action branch)
  ├─ SIM_STATE==READY and SDK≥30 and f0/f.t≥0 → count down; unknown network type → f0/a.b(ctx, 3000ms) retry (≤3 times)
  ├─ e0/c2.i() creates MODEL→manufacturer static table
  ├─ reader = e0/c2.h(ctx) manufacturer branch → OEM Reader (miss → e0/r)
  └─ reader ∈ {z0,c0,e0} ? → startService(RegisterIntentService, RegType=F/B) (first bind the manufacturer AIDL)
                            : d0/g.b(ctx, "F"/"B") → Direct collection + reporting

ScheduleRegisterReceiver (every 24h) → d0/g.b(ctx,"S")
AppInfoRegisterReceiver (every 7d) → d0/g.b(ctx,"A")
SIMStateReceiver(SIM changes) → d0/g.b(ctx,"T")
ConnectStateReceiver (network change) → only writes logs (no collection, no reporting, see §4.4)

d0/g.b/a/c(ctx[,type]) → e0/c2.g(ctx,type)
  ├─ "T": Compare ICCID with pref iccid1/iccid2, unchanged → record "SIM not changed, not post!" and terminate
  ├─ "A": device_provisioned==1? Starting from d0/i (Application Statistics): Starting from d0/k
  ├─ "R": Starting from d0/j (installing signal history), **does not report** (this package has no sender → dead path)
  └─ "B"/"F"/"S"/Others: starting from d0/k

d0/k.run()
  ├─ f0/f.g (default true=allow networking/collection)
  │    └─ It is: d0/g.d() → MD5 → AES+sign → URL → POST → 200/416 is considered successful → clear the history table
  │    └─ No: Write the d0/g.d() result to systemhelper.history (≤100 entries), log "User Permission Closed!"
```

### 3.3 Global static configuration `f0/f` (28 fields, non-final, can be rewritten arbitrarily) [Observed]

| domain | initial value | Semantics | Is there a writing point |
|---|---|---|---|
| `w` | `http://dm.wo.com.cn:18080/registerv3` | Report address default value | No (pref can be overridden) |
| `x` | `http://dm.wo.com.cn:18080/public/ip` | Public IP echo service | No |
| `y` | `0x5265C00`=86,400,000 | scheduleInterval default 24h | No |
| `z` | `0x240C8400`=604,800,000 | appInfoInterval default 7d | No |
| `t` | `2` | Initial value of network wait retry count | Yes (OnBootReceiver decrements) |
| `g` | `true` | **networking/acquisition main switch** (d0/k judgment) | No (initialization only) |
| `a` | `false` | Log switch | Yes (pref/AIDL s()) |
| `b` | `false` | IP acquisition switch | **No → Die** |
| `c` | `false` | VoLTE write system switch | **No → Die** |
| `e` | `false` | signal information switch | **No → Die** |
| `f` | `null` | Context | Yes (d0/g factory) |
| `h/i` | `null` | TelephonyManager (Slot 1/Slot 2) | Yes |
| `j/k` | `null` | `d0/e` PhoneStateListener ×2 | Yes |
| `l/m` | `null` | `d0/f` TelephonyCallback ×2 | Yes |
| `n..s` | `""` | NetworkTypeSlot1/2, DataSlot cache, etc. | Yes |
| `A` | `null` | nubia `b0/a` Binder Agent | Yes |
| `B` | `null` | Gree `c0/a` Binder Agent | Yes |
| `u/v` | `false` | Other branch flags | Yes |

> Note: The `b/c/e` three switches **have no `sput` writing point** (only initialization 0), so in this version **IP collection, signal strength collection, VoLTE forced opening of the three links are all ineffective** - but the code and data path are completely retained, and can be activated by rewriting the static domain once.

---

## 4. Components and external triggering surfaces

### 4.1 `DMApplication`[Observed]
Only `Log("DMApplication","onCreate--->")`; when `SDK≥30`, call `e0/r.k(this)` to register Telephony monitoring. No other initialization.

### 4.2 `OnBootReceiver` (13 KB, core-scheduled) [Observed]

1. Read `preference_enable_log` → `f0/f.a`.
2. Record action; `SDK≥30` → `e0/r.k(ctx)`.
3. **action ∈ {BOOT_COMPLETED, QUICKBOOT_POWERON}**：
   - Log `boot delayed for 4 minutes-->`; `f0/a.b(ctx, 0x3A980=240000ms)` hangs for 4 minutes and then re-enters itself;
   - `f0/a.d(ctx, f0/l.f(ctx)=scheduleInterval)`；
   - `f0/a.a(ctx, f0/l.b(ctx)=appInfoInterval)`;Return.
4. **Other actions (including null, including self-awakening)**:
   - If `SIM_STATE==5(READY)`, `SDK≥30`, `f0/f.t≥0` → `f0/f.t--`; if `f0/f.n`(NetworkTypeSlot1) and `f0/f.o` are both empty → `f0/a.b(ctx, 0xBB8=3000ms)` wait another 1 second and return;
   - `e0/c2.i()`；`reader=e0/c2.h(ctx)`；
   - Read pref `firstPowerOn`: empty → `RegType="F"`, otherwise `"B"`;
   - `reader ∈ {e0/z0, e0/c0, e0/e0}` → `startService(RegisterIntentService, RegType=F|B)`; otherwise → `d0/g.b(ctx, "F"|"B")`;
   - Write back pref `firstPowerOn`.

> **risk**: The receiver `exported=true` and **do not verify the source** except for the action string; any application sending any action can repeatedly trigger "immediate collection + reporting" (see §10.4).

### 4.3 `SIMStateReceiver`[Observed]
`SIM_STATE_CHANGED` → Static method `a()` (reset) + `b(Context)`: `f0/a.c(ctx, 0x57E40=360000ms=6 minutes)` will automatically trigger once "wait for SIM to be ready before reporting" (`RegType="T"`). That is, after SIM changes, it will be re-registered on demand after a delay of 6 minutes.

### 4.4 `ConnectStateReceiver`[Observed]
- Static registration (Manifest, `CONNECTIVITY_CHANGE`) + call `b(ctx)` from `OnBootReceiver` to dynamically register again (in `b()`, `unregister` is the old one, and then `f0/f.v=true` is used to suppress the "sticky callback at the registration moment").
- `onReceive` only does one thing: use `f0/l.l(ctx)` to determine whether it is currently connected to the Internet, print `NetWork connected` / `NetWork disconnected`, **does not collect, and does not report**.
- `a()` is the logout method (`unregisterReceiver` + clear `f0/f.v`).

### 4.5 `ScheduleRegisterReceiver`[Observed]
Self-wakeup every 24h: re-hang the 24h alarm (self-renewal) + trigger a regular collection report (`RegType="S"`).

### 4.6 `AppInfoRegisterReceiver`[Observed]
Self-wake up every 7 days: re-hang the alarm for 7 days + trigger an application list collection (`RegType="A"` → starting from `d0/i` statistics UsageStats/traffic).

### 4.7 `TestActivity`（exported）[Observed]
```
onCreate:
  setContentView(activity_test)
  f0/f.a = pref preference_enable_log
  SDK≥30 → e0/r.k(ctx)
  Check READ_PHONE_STATE + ACCESS_COARSE_LOCATION + ACCESS_NETWORK_STATE
    All granted → B()
    Otherwise → requestPermissions([...], 1)
onRequestPermissionsResult(1): All three granted → B(); otherwise finish()
B():
  tv = findViewById(tv_device_info)
  e0/c2.i()
  r = e0/c2.h(ctx)
  r ∈ {e0/z0, e0/c0, e0/e0} ? startService(RegisterIntentService) : tv.setText(d0/g.c(ctx))
```
That is: the **interface is a TextView**; `d0/g.c(ctx)` returns a plain text summary (`d0/g.e()`) that spells out the fields of `d0/g` for "test/self-test" display.
> Risk: Any application can launch this Activity and trigger three system permission pop-ups (it is not subject to background startup restrictions under targetSdk 28).

### 4.8 `RegisterHelperService` + `IRegisterHelperService` (**highest risk component**) [Observed]

- `exported=true`, **without `android:permission`**, with action filter → Any application can get `bindService` Binder.
- AIDL descriptor: `com.mobiletools.systemhelper.service.IRegisterHelperService`; `onTransact` only does `enforceInterface` (only interface name is verified, **does not perform caller authentication/Uid verification/signature verification**).
- The real semantics of 5 transactions (`RegisterHelperService$a.smali:34-144`):

| affairs | method | actual behavior |
|---|---|---|
| 1 | `r(String s)` | `f0/l.t(ctx,s)` → write pref **`serverAddress` = s** (**can redirect the entire telemetry to any server**) |
| 2 | `i(int n)` | `scheduleInterval=n`; `appInfoInterval=n*8/5`; `signalInfoInterval=n*9/5` (**can change 24h to 1s, forming a flow/power amplification**) |
| 3 | `u()` | `f0/l.a()` → Delete `/data/data/com.mobiletools.systemhelper/shared_prefs/com_mobiletools_systemhelper_preference.xml` (Configure DoS) |
| 4 | `s(boolean z)` | `f0/f.a=z` + write pref `preference_enable_log` (remote switch log) |
| 5 | `o()→boolean` | Read pref `preference_enable_log` |

In `onCreate`, Telephony monitoring is only registered when `SDK≥30`; `onUnbind` always returns `true` (next time bind will go to `onRebind`, but it is not implemented → the actual behavior is based on the system default).

### 4.9 `RegisterIntentService` (OEM private Binder bootloader) [Observed]

- Only when the reader belongs to `e0/z0` (nubia), `e0/c0`/`e0/e0` (Gree), external `startService`; `bindService` on demand in `onCreate`:
  - `com.android.phone/com.android.phone.nubia.NBExtTelephonyService` (`BIND_AUTO_CREATE`) → Agent deposit `f0/f.A`
  - `com.gree.telephonymanager/com.gree.telephonymanager.GreeTelephonyManagerService` → Agent deposit `f0/f.B`
- `onHandleIntent` waits for the agent to be ready by spinning **with `Thread.sleep(5)`** without timeout, then `d0/g.b(ctx, RegType)`.
- `onDestroy` **does not unbind** (connection leak).
- The inner class `$a`/`$b` is two `ServiceConnection`.

### 4.10 `RegisterJobService` (**dead component**) [Observed]
`BIND_JOB_SERVICE` is declared, but **in the whole project has no `JobScheduler`/`JobInfo` referencing** → will never be scheduled; `onStartJob` has no substantial logic.

---

## 5. Panorama of collection capabilities (function list)

### 5.1 Composition of `e0` layer (role division of 85 classes) [Observed]

| role | Quantity | member | Notes |
|---|---|---|---|
| infrastructure | 5 | `e0/r` (base class), `e0/c2` + `c2$a`/`c2$b`/`c2$c` | Template + orchestrator + two callbacks |
| API compatible bridge | 20 | a..l1 side 12: `b,d,e,f,g,h,i0,j,k,l,l0` (and similar single method classes); m..z1 side 8: `m,n,o,p,q,v0,x,z` | Each only contains one API≥29 new members (`getDomain`/`getTransportType`/`getCellIdentity`/`CellIdentityTdscdma.getCid,getLac`/`SubscriptionInfo.getMncString`/`SubscriptionManager.getSlotIndex`/`getActiveDataSubscriptionId`/`getImei(int)`/`getCellIdentityNr` etc.), is the product of cross-version compatibility of "compileSdk 33 / targetSdk 28" |
| reflection tool | 1 | `e0/d2` | `getXXX(int)`→`getXXX(long)` fallback, `long[]`/`int[]` dual resolution of `getSubId` |
| OEM collector | 59 | See §5.2 and Appendix D (48 of them appear in the brand distribution table of `c2.h()`, and the rest are variants without distribution entry) | One set of templates + one copy for each manufacturer, covering Samsung/Xiaomi/OPPO/vivo/HUAWEI/Lenovo/ZTE/Coolpad/meizu/nubia/realme/OnePlus/TCL/Hisense/KONKA/lephone/gionee/Changhong/WIKO/PTAC/TDTECH/Liantong/K-Touch/JingLing/Shown/LETV/Bee/BestSonny/kingsun/mfox/Xiaolajiao/ChuiZi/SINTAVE/OYSIN/BROR/NOAIN/YinKe/HZTL/INTKI/PPTV/Onefive/sprd, etc. |

### 5.2 Manufacturer distribution and model whitelist [Observed]

- `e0/c2.i()` Lazy loading static `Map a`: **`Build.MODEL → Manufacturer name `**, total **304** (302 hardcoded + 2 `ro.product.manufacturer` direct fetch), line number 1370–3391. Brand distribution: Xiaomi 117, OPPO 92, vivo 20, realme 13, Hisense 7, PTAC 7, samsung 6, Lenovo/ZTE/Liantong 4 each, nubia/OnePlus/WIKO/TDTECH 3 each, meizu 2, and the remaining 14 brands 1 each.
- `e0/c2.h(Context)`: `SDK≥30` first `e0/r.k(ctx)`; take `a.get(Build.MODEL)`, then use **60 to branch `String.equals`** `new` to get the corresponding Reader; miss → `e0/r` and type `getReader error!! manufacture==null!`. The results are cached to `e0/c2.b` (never recalculated).
- Key mapping (60 branches of `e0/c2.h()`, `Build.MANUFACTURER` string exact `equals`, line number `e0/c2.smali`): `samsung→e0/q1`, `Xiaomi→e0/y1`, `OPPO→e0/c1`, `BBK/vivo→e0/u1`, `HUAWEI→e0/h0`, `nubia→e0/z0`, `meizu→e0/u0`, `motorola→e0/w0` , `Lenovo→e0/s0` , `ZTE→e0/b2` , `Coolpad→e0/y` , `TCL→e0/s1` , `realme→e0/m1` , `OnePlus→e0/f1` , `Hisense→e0/g0` , `KONKA→e0/o0` , `kingsun→e0/n0` , `lephone→e0/q0` , `gionee→e0/d0` , `LETV→e0/r0` , `sprd→e0/c` , `CHANGHONG→e0/v` , `TDTECH→e0/t1` , `PTAC→e0/k1` , `WIKO→e0/v1` , `EWPE→e0/c0` , `TOSOT→e0/e0` , `Liantong/H10→e0/t0` , `YIK→e0/z1` , `xiaodu→e0/w1` , `HZTL→e0/j0` , `Shown→e0/n1` , `OYSIN→e0/e1` , `QIKU→e0/l1` , `PHICOMM→e0/g1` , `PHILIPS→e0/h1` , `PROTRULY→e0/j1` , `Haipainoble→e0/f0` , `NOAIN→e0/y0` , `SOP→e0/p1` , `Xiaolajiao→e0/x1` , `5G HYUN→e0/a1` , `INTKI→e0/k0` , `PPTV→e0/i1` , `Onefive→e0/b1` , `BROR→e0/a` ;Miss → `e0/r` and hit `getReader error!! manufacture==null!` (see `findings/device-core.md §6.3` for complete 60 branches and line numbers).
- Conclusion: "Whether **collects" is completely determined by `Build.MODEL`**, and has nothing to do with permission status or user consent. You can switch/trigger different collectors by changing the model (even non-China Unicom models can run the deepest collection branch of China Unicom models).

### 5.3 Summary list of collected data items (data items → source → drop point) [observation + inference]

| # | data item | Source (API/Properties/File) | `d0/g` field | Report key | Notes |
|---|---|---|---|---|---|
| 1 | Manufacturer | `c2.a.get(Build.MODEL)` / `Build.MANUFACTURER` | `a` | `Manuf` | |
| 2 | Model | `Build.MODEL` | `b` | `Model` | |
| 3 | Hardware version | `ro.*` manufacturer attributes (`ro.board.hardware_version`, `ro.build.hardware.version`, `ro.huawei.*`, `ro.vivo.*`…) | `c` | `HWVersion` | Keys vary from manufacturer to manufacturer |
| 4 | Software version | `ro.build.display.id`/`ro.build.PDA`/`ro.tcl.*`/`ro.miui.cust_hardware`… | `d` | `SWVersion` | |
| 5 | OS name | Constant `"Android"` (rewritten for YunOS models) | `e` | `OS` | |
| 6 | OS version | `Build.VERSION.RELEASE`（`ro.build.version.external`） | `f` | `OSVersion` | |
| 7 | **IMEI1** | `getImei()/getDeviceId()` (reflective), `gsm.mtk.imei1`, `ril.imei.sim1`, `gsm.rgk.imei1`, `gsm.yunos.imei0`, `persist.sys.fota_deviceid1`, MTK `ITelephonyEx.getSvlteImei(0)`, nubia/Gree AIDL, `miui.telephony.*` | `g` | `IMEI1` | 6 independent methods |
| 8 | **ICCID1** | `getSimSerialNumber()`、`SubscriptionInfo.getIccId()` | `h` | `ICCID1` | |
| 9 | **IMSI1** | `getSubscriberId()`、`miui.*ForSubscription` | `i` | `IMSI1` | |
| 10 | **MSISDN1 (mobile phone number)** | `getLine1Number()`、`getMsisdn()`、`SubscriptionInfo.getNumber()` | `j` | `MSISDN1` | |
| 11 | Operator 1 | `getNetworkOperator()`/`getSimOperator` (**complete MCC+MNC**, such as `46001`) | `k` | `MNC1` | Key name "does not deserve its name" |
| 12 | Network type 1 (NCLS) | `TelephonyManager.getNetworkType()/getVoiceNetworkType()` + mapping table | `l` | `NCLS1` | See §5.4 |
| 13 | **cell ID1** | `getCellLocation()`、`getAllCellInfo()`、`getNetworkRegistrationInfoList()`（domain=PS, transport=WWAN）、`CellIdentity{Gsm,Wcdma,Tdscdma,Cdma,Lte,Nr}`、pref `CellID0/1` | `m` | `CellID1` | For NR `NCI`(e0/e) + `TAC`(e0/f) |
| 14 | **LAC1/TAC1** | Same as above | `n` | `LAC1` | |
| 15 | **VoLTE switch 1** | `ImsMmTelManager.isAdvancedCallingSettingEnabled()`、`TelephonyManager.isVolteAvailable`、`Settings.Global`、ColorOS `isOemEnhanced4gLteModeSettingEnabledByUser`、vivo `API_TAG_isVolteEnable`、ZTE `DmUnicomAbsTelephonyManager.getVoLTEState` | `o` | `VolteEnabled1` | The **base class will turn on the switch first and then read** (see §10.2) |
| 16 | **5G switch 1** | `Settings.Global["preferred_network_mode"+subId] ∈ [23,33]`、`isFiveGCapable`、`ro.nubia.nr.support`、`persist.radio.vivo.fiveg` | `p` | `5GEnabled1` | The semantics is "user's preferred network mode", not "whether 5G is supported" |
| 17–26 | Slot 2 Same item | Same as above, `slot=1` path | `r..A` | `IMEI2/ICCID2/IMSI2/MSISDN2/MNC2/NCLS2/CellID2/LAC2/VolteEnabled2/5GEnabled2` | |
| 27 | Data card slot | 4 algorithms (`getPhoneId(defaultDataSubId)+1`, `getDefaultDataSubscriptionId()+1`, `==0?"1":"2"`, hard-coded `"1"`) | `B` | `DataSlot` | 0/1 base mix |
| 28 | Access type | `ConnectivityManager`：Mobile / WiFi / Disconnected | `C` | `AccType` | |
| 29 | IPv4 / IPv6 | Server `GET /public/ip`; first non-loopback/linklocal Inet6Address | `D/E` | `IPv4/IPv6` | **switch `f0/f.b` constant false → never collect** |
| 30 | RAM/ROM/available | `/proc/meminfo`、`/proc/meminfo_memsize`、`StatFs`、`ActivityManager.MemoryInfo` | `F` | `SubType{RAM,ROM,FREE}` | Read without permission |
| 31 | Device type | Hard-coded `"000"`, individual manufacturers write `"007001029"` (Baidu coding)/`"000004"`/`"000005"`/model name | `G` | `DeviceType` | Unclear semantics and contaminated |
| 32 | Registration type | `F/B/S/A/T/R` | `H` | `RegType` | |
| 33 | Protocol version | Constant `"3.0"` | — | `SpecVersion` | |
| 34 | client version | `PackageManager.getPackageInfo().versionName` | `J` | `RegVersion` | |
| 35 | time | `SimpleDateFormat("yyyy-MM-dd HH:mm:ss")` | `K` | `Time` | |
| 36 | **application usage list** | `UsageStatsManager.queryUsageStats(now-7d, now)` + `NetworkStatsManager` | `L` | `AppInfo` | See §5.5 |
| 37 | **Signal History** | `f0/m` (signalhelper.signal meter reading) | `N` | `SignalInfo` | The table has no writing method → constant empty/`"null"`; and the switch `f0/f.e` is constant false → the key is omitted |
| 38 | **failure queue** | `f0/d` (systemhelper.history table) | `M` | `History` | Array of historical plaintext messages that failed to be reported |

> Not collected (explicitly excluded): `Build.SERIAL`, `Settings.Secure.ANDROID_ID`, OAID/GAID, WiFi MAC/BSSID, Bluetooth address, address book, text messages, call records, photo album, microphone, camera, clipboard, package name + full version of installed applications (only visible statistics of UsageStats), accurate GPS (this version of the path is closed).

### 5.4 NCLS network type mapping (expanded by `packed-switch`) [Observed]

| `getNetworkType()` | NCLS |
|---|---|
| 1, 2, 16 | GSM |
| 3, 8, 9, 10, 15 | WCDMA / TD-SCDMA (by MCC: 46000/46002/46007 → TD-SCDMA, the rest → WCDMA) |
| 4, 5, 6, 7, 12, 14 | CDMA（1xRTT/EvDo） |
| 13, 19 | LTE |
| 17 | TD-SCDMA |
| 20 | **NR（5G）** |
| 11, 18 | unknown |

### 5.5 `d0/i`: Application usage and traffic collection [Observed]

- Time window: `now - 7 days` (`f0/f.z` = 604,800,000 ms) → `now`, through `UsageStatsManager.queryUsageStats`.
- Filter/sort: `getTotalTimeInForeground()` **in descending order**; front desk duration `ms/60000` rounded to minutes (`long→int` truncated).
- Output: `JSONArray` → `d0/g.L` → `AppInfo`. Each item `f0/b`: package name, foreground minutes, last usage time, traffic (`NetworkStatsManager` queryDetailsForUid: mobile data KB=`/1024`, WiFi total MB=`/1048576`).
- Dependency: `PACKAGE_USAGE_STATS` (requires user authorization in "Usage Access", or directly granted by privapp whitelist) + `QUERY_ALL_PACKAGES`.

### 5.6 Location and Cell[Observed]

- `d0/g.f(CellLocation)` / `g(CellLocation)`: parse GSM/CDMA and two `CellLocation`, write `CellID1/LAC1` (slot 1) and `CellID2/LAC2` (slot 2), and print `gsm location cellId1 = ...` and other logs.
- When API≥29, use `getNetworkRegistrationInfoList()` to filter `domain=PS, transport=WWAN`, and then take `CellIdentityNr` (`NCI`+`TAC`), `CellIdentityLte`, `CellIdentityWcdma`, etc.; `e0/p,q` bridge provides `CellIdentityTdscdma.getCid/getLac`.
- Bottom line: pref `CellID0/1`, `LAC0/1` (historical value).
- **exact GPS**: `d0/f$a` (`LocationManager.requestLocationUpdates`) with `getLastKnownLocation` path to library code exists, but `d0/f.onSignalStrengthsChanged`/full link is closed by `f0/f.e` (no write point) → This version **does not report the latitude and longitude**.

---

## 6. Reporting protocol (DM register v3)

### 6.1 Target address[Observed]

```
GET http://dm.wo.com.cn:18080/public/ip ← Public IP echo (not enabled in this version)
POST http://dm.wo.com.cn:18080/registerv3?ver=3.0&model=<URLEnc(Model)>&manuf=<URLEnc(Manuf)>&sign=<URLEnc(Base64(RSA(MD5)))>
```
- The default value comes from `f0/f.w`; the actual value `f0/l.g(ctx)` reads pref `serverAddress` (can be overwritten by §4.8 Transaction 1).
- **plain text HTTP, non-standard port 18080, none TLS** (`usesCleartextTraffic=true`, `network_security_config.xml` are not referenced).
- `dm.wo.com.cn` = the device management domain name (`wo`) of China Unicom's "Wo" system; `ver=3.0` and `SpecVersion=3.0` correspond to the "register v3" specification.

### 6.2 Encrypted chain (`d0/k.smali:143-260`, checked instruction by instruction) [Observed]

```
plaintext = d0/g.d() // JSON string
K = MD5(plaintext) // 16 bytes, also serves as AES key
sign      = Base64( RSA/ECB/PKCS1Padding(pub.cer, K) ) // 1024-bit
body = AES/ECB/PKCS5Padding(K, plaintext) // Raw bytes (not Base64)
URL       = serverAddress + "?ver=3.0&model=..&manuf=..&sign=" + URLEncode(sign)
POST body=body, Content-Type: application/encrypted-json, Content-Length=len(body)
```
- `assets/pub.cer`: X.509 DER, **self-signed**, `Subject=Issuer=CN=UNICOM, OU=UNICOM, O=UNICOM, L=BJ, S=BJ, C=CN`, Serial `528B9360`, validity period **2019-05-24 → 2119-04-30**, signature algorithm `sha256RSA`, **RSA 1024-bit** with SKI extension only; DER SHA-256 `810c0122ba6af7a4b6a5921fdbbbf9197fb2e0b2e82de0c5aa40d28f1b7c6501`.
- `key` **in the third parameter `f0/g.c(url, body, key)` is completely unused** (only URL is used for signature).
- **cryptography evaluation**: key = MD5 (plain text) → Anyone who gets `body` can use MD5 (body) to decrypt AES. "Encryption" is only equivalent to confusion; `sign` has no timestamp/random number/serial number → **can infinitely replay**; RSA-1024 can be decomposed by national-level computing power.

### 6.3 HTTP details[Observed]

| item | value |
|---|---|
| method | `POST` |
| head | `Content-Type: application/encrypted-json`、`Content-Length` |
| timeout | connect 10,000 ms、read 15,000 ms |
| Successful judgment | `200` → Success; **`416`(0x1A0) is also regarded as success** |
| Try again | **No retry, no backoff** (dropped after one failure, wait for the next 24h cycle) |
| Log | `RegisterThread` TAG: `register postString: <clear text>`, `server responseCode-->`, `register success!`/`register fail!` |

### 6.4 Failure handling and dropout [Observed]

- Failure: `d0/g.M = null` is followed by writing the plain text JSON into SQLite `systemhelper.history(id INTEGER PRIMARY KEY AUTOINCREMENT, jsonString TEXT)`; the upper limit of the **table is 100** (`count>=0x64` is deleted according to the "jsonString value of the earliest row". Note that it is deleted by value rather than by id. More with the same value will be deleted).
- Success: `DELETE FROM history` (full clear); at the same time, the history will be merged into the text `History` array in the next report (SDK≥28 uses 20 MB `CursorWindow` to read).
- Therefore, the data during the offline period of **will be accumulated and brought out to** at once (≤100 items) after the network is restored, forming a "supplementary transmission" capability.

### 6.5 Complete message structure (`d0/g.d()`, `d0/g.smali:335-981`) [Observed]

```json
{
  "DeviceInfo": {
    "Manuf": "...", "Model": "...", "HWVersion": "...", "SWVersion": "...",
    "OS": "Android", "OSVersion": "...",
    "IMEI1": "...", "ICCID1": "...", "IMSI1": "...", "MSISDN1": "...",
    "MNC1": "46001", "NCLS1": "LTE", "CellID1": "...", "LAC1": "...",
    "VolteEnabled1": "1", "5GEnabled1": "0",
    "IMEI2": "...", "ICCID2": "...", "IMSI2": "...", "MSISDN2": "...",
    "MNC2": "...", "NCLS2": "...", "CellID2": "...", "LAC2": "...",
    "VolteEnabled2": "...", "5GEnabled2": "...",
    "DataSlot": "1", "AccType": "Mobile",
    "SubType": {"RAM":"8G","ROM":"128G","FREE":"50G"},
    "DeviceType": "000", "RegType": "S", "SpecVersion": "3.0",
    "RegVersion": "1.2-20230925-U", "Time": "2026-11-05 02:00:00",
    "IPv4": "...", "IPv6": "..."          // Only when f0/f.b is true (this version is always false)
  },
  "AppInfo":  [ {…}, … ],                  // Only RegType=A / when there is data
  "SignalInfo": "…",                       // Only f0/f.e is true (this version is always false)
  "History":  [ "…", "…" ]                 // Only when there is a failed cache
}
```
- The 26 `DeviceInfo` single value keys are **constant output** (if missing, fill in `"Unknown"` or `""`), and there are 4 condition keys.
- Empty IMSI will be automatically cleared to `null` by `e0/c2.n()`; before `d0/g` is serialized, `null` will be normalized to `"Unknown"`/`""`.
- **PII processing strategy**: `e0/c2.o()` When the operator **∉ {46001,46006,46009}** (non-China Unicom), do a total of 10 fields in the same group of `ICCID1/MSISDN1/IMSI1/CellID1/LAC1` and slot 2 **MD5 hexadecimal**; **China Unicom card reports** in clear text. At the same time, the slots are normalized according to the size of IMEI (for `g.g > g.r`, the entire group is swapped and `DataSlot` 1↔2).

---

## 7. Timing, keep-alive and state machine

### 7.1 Alarm table (`f0/a`, all `type=2`=ELAPSED_REALTIME_WAKEUP + `setExactAndAllowWhileIdle`) [Observed]

| method | target receiver | Delay source | Default value | Purpose |
|---|---|---|---|---|
| `f0/a.b` | `OnBootReceiver` | constant | 240,000 ms (4 min) / 3,000 ms (3 s retry) | First startup delay, network waiting for retry |
| `f0/a.d` | `ScheduleRegisterReceiver` | pref `scheduleInterval` | 86,400,000 ms（24 h） | Regular registration and reporting, self-renewal |
| `f0/a.a` | `AppInfoRegisterReceiver` | pref `appInfoInterval` | 604,800,000 ms（7 d） | Application list collection, self-renewal |
| `f0/a.c` | `SIMStateReceiver` | constant | 360,000 ms（6 min） | SIM Delayed re-registration after change |

- `PendingIntent` flags = `0x0C000000`（`FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`）。
- **None `setRepeating`**: All rely on the receiver to self-renew + re-seeding after power-on. `SCHEDULE_EXACT_ALARM` permission + `setExactAndAllowWhileIdle` means **deliberately fights against Doze**.
- `f0/a.e()`/`f0/a.f()` is the stub **that** always returns true, causing the ordinary `set()` branch of `if(...)` to be dead code.

### 7.2 `RegType` semantics [observation + inference]

| value | sender | meaning |
|---|---|---|
| `F` | OnBootReceiver(`firstPowerOn` is empty) | First time boot registration |
| `B` | OnBootReceiver (not the first time) | Start up registration |
| `S` | ScheduleRegisterReceiver | Regular registration (24h) |
| `A` | AppInfoRegisterReceiver | Application Manifest Registration (7d) |
| `T` | SIMStateReceiver / ConnectStateReceiver | SIM/Network change trigger (requires ICCID change) |
| `R` | **No sender** | Signal history loading path (dead) |

### 7.3 SIM change determination [Observed]
`"T"` branch of `e0/c2.g`: Compare the current `ICCID1/ICCID2` with pref `iccid1/iccid2`; same → print `SIM not changed，not post！` and **terminate**; change → record `SIM1/2 Changed,old iccid:…, new iccid:…` and continue reporting (pref is updated at the same time).

### 7.4 Boundaries and dirty states [Inferred]
- `f0/f.t` (retry count) is the static **in the** process, and will return to 2 upon restart; if the network is not ready for a long time, it will last up to 3 times × 1 s at each boot.
- `e0/c2.b` (Reader singleton) does not expire; the slot exchange of `e0/c2.o()` will modify the shared instance of `d0/g` → Multiple calls can theoretically accumulate exchanges (idempotence has not been verified on a real machine).
- The writeback branch of `firstPowerOn` appears as "key value writes itself" in decompilation. The actual written value depends on register multiplexing. **has signs of being written as an empty string** (newly installed machines are judged as `F` every time they are turned on).

---

## 8. Configuration, switches and dead code

### 8.1 SharedPreferences (file `com_mobiletools_systemhelper_preference`, `MODE_MULTI_PROCESS`=4, obsolete) [Observed]

| key | Type | Default | Semantics |
|---|---|---|---|
| `serverAddress` | String | `http://dm.wo.com.cn:18080/registerv3` | Report address (can be rewritten by AIDL) |
| `scheduleInterval` | long | 86,400,000 | regular cycle |
| `appInfoInterval` | long | 604,800,000 | App manifest cycle |
| `signalInfoInterval` | long | (No default when reading) | Signal period (only AIDL writes, there is no reading point **in the** code) |
| `preference_enable_log` | bool | false | Log switch |
| `firstPowerOn` | String | `""` | First start mark |
| `iccid1` / `iccid2` | String | — | SIM change comparison |
| `CellID0` / `CellID1` / `LAC0` / `LAC1` | String | — | Cell cache |
| `dm_user_agreement` / `operator_service_improve_plan` / `operator_registration_switch` | (Secure Settings, not pref) | — | "User consent" access control for some Readers |

- The file path is hardcoded at `f0/l.a()`:`/data/data/com.mobiletools.systemhelper/shared_prefs/com_mobiletools_systemhelper_preference.xml` (for deletion).

### 8.2 Access control ("Does the user agree or not") implementation is inconsistent [Observed]

| realize | Appear location |
|---|---|
| `Settings.Global.device_provisioned == 1` | `a0/b2/c1/d0/e0/f1/j0/k1` etc. |
| `Settings.Secure["dm_user_agreement"] == 1` | `c/b2` |
| `Settings.Secure["operator_service_improve_plan"] == 1` | `c0/g0` |
| `device_provisioned==1 && Settings.Secure["operator_registration_switch"]==1` | `d0` |
| **`return true` hard coded** | `e0/a1`（5G HYUN） |
| No access control | Most of the rest Reader |
| The only true rejection branch | `e0/u1`（vivo）：`isUserPermissionOpen==false` → `do not read!` |

At the same time, `a(Context)` ("whether this Reader is applicable") overwritten by all Readers is `new ServiceState().getRoaming()` (constantly false), and `e0/c2.k()` is all APK without caller → **"whether to collect" does not actually pass through any access control**.

### 8.3 Dead code / unenabled capability list[Observed]

| item | Status | influence |
|---|---|---|
| `f0/f.b` (IP switch) | No write point | `d0/h` never starts, `IPv4/IPv6` key never appears |
| `f0/f.e` (signal switch) | No write point | `d0/f.onSignalStrengthsChanged` first line return; `SignalInfo` key never appears |
| `f0/f.c` (VoLTE write switch) | No write points (8 files, 16 reads) | Hengzou read-only `e0/r.d()`; the write path of "Force opening VoLTE" is not executed |
| `RegType="R"` | no sender | The link between `d0/j` and the signal table is a dead path |
| `signalhelper.signal` table | No insert method | `SignalInfo` Hengkong |
| `RegisterJobService` | No JobScheduler reference | never dispatch |
| `com.mobiletools.systemhelper.permission` | Zero references | dead statement |
| `network_security_config.xml` | Manifest not quoted | Dead resource (the one that really takes effect is `usesCleartextTraffic`) |
| `setting_advice_*` string | Zero references | **Privacy Notice Unreachable** |
| `d0/g.q`、`d0/g.O`、`d0/g.P/Q` | No writing/no serialization | dead field |
| `e0/c2.j()/k()` | no caller | death method |

---

## 9. Manufacturer/operator private channel

### 9.1 Two sets of manufacturers AIDL (`b0/a`, `c0/a`) [Observed]

| interface | descriptor | Binding components | Number of transactions | Result point | consumer side |
|---|---|---|---|---|---|
| `b0/a` | `com.android.phone.nubia.INBExtTelephony` | `com.android.phone/com.android.phone.nubia.NBExtTelephonyService` | 10（1=`v()` … 10=`d()`） | `f0/f.A` | `e0/z0`（nubia） |
| `c0/a` | `com.gree.telephonymanager.IGreeTelephonyManager` | `com.gree.telephonymanager/com.gree.telephonymanager.GreeTelephonyManagerService` | 12（1=`m(I,S,S)` … 12=`j()`） | `f0/f.B` | `e0/c0`、`e0/e0`（Gree/EWPE/TOSOT） |

- `RegisterIntentService.onCreate` is `bindService(BIND_AUTO_CREATE)` only if Reader is `z0/c0/e0`.
- `onHandleIntent` uses `Thread.sleep(5)` to wait for Binder without timeout spin; when Proxy's `transact()` returns false, **falls back to the static local pile** (`b` field in `b0/a$a` and `c0/a$a`).
- `onDestroy` not `unbindService`.
- The true semantics of the transaction can only be inferred from the consumer usage (**[Inferred]**, non-stub annotation).

### 9.2 Manufacturer’s proprietary API / hidden channel[Observed]

| channel | class | Purpose |
|---|---|---|
| MTK Binder | `com.mediatek.internal.telephony.ITelephonyEx$Stub` + `ServiceManager.getService("phoneEx")` | `getSvlteImei(slot)` takes IMEI (**completely bypasses TelephonyManager**) |
| MTK | `com.mediatek.telephony.TelephonyManagerEx` | `getCellLocationFromSlotId`、`getSimOperator` |
| vivo | `ITelephony#getITelephony` + `com.android.internal.telephony.VivoTelephonyApiParams.getParamValues()` | `API_TAG_getNrCapability`、`API_TAG_getNRSwitchState`、`API_TAG_isVolteEnable` |
| Huawei/Honor | `HwTelephonyManager.getServiceAbility`、`isVolteAvailable`、`TelephonyManagerEx.getDefault4GSlotId` | VoLTE/4G slot |
| Xiaomi | `miui.telephony.TelephonyManager(Ex)`：`getSubscriberIdForSubscription`、`getSimSerialNumberForSubscription`、`getNetworkTypeForSubscription`、`getCellLocationForSlot`、`isFiveGCapable` | Dual SIM card + 5G |
| OPPO/realme/TCL/Coolpad/YinKe | `ColorOSTelephonyManager`、`isOemEnhanced4gLteModeSettingEnabledByUser` | VoLTE status |
| Samsung | `TelephonyManager.getTelephonyProperty("ril.hw_ver"/"ro.build.PDA")`、`createForSubscriptionId`、`Settings$System.getInt("voicecall_type")` | 5G/VoLTE Specialization |
| Meizu | `MzTelephonyManager` | Dual SIM |
| ZTE | `com.android.vts.VendorTelephonyManager` + China Unicom customized `DmUnicomAbsTelephonyManager.getVoLTEState` | VoLTE |
| universal reflection | `f0/o` (`Class.forName`+`getMethod`+`setAccessible(true)`+`invoke`), `e0/d2` (`getXXX(int)`→`getXXX(long)` fallback, `getSubId`’s `long[]`/`int[]` double parsing) | Hide all bases called by API |

### 9.3 Key system attribute table [Observed]

`ro.product.manufacturer`, `ro.build.version.external`, `ro.yunos.name/version`, `ro.build.display.hardware.id`, `gsm.mtk.imei1/2`, `gsm.rgk.imei1/2`, `gsm.yunos.imei0/1`, `ril.imei.sim1/2`, `persist.sys.fota_deviceid1/2`, `persist.sys.card.imsi`, `persist.sys.cucc.dm`, `ro.boot.dram_info`, `ro.board.hardware_version`, `ro.build.rom.pcb.silkscreen`, `ro.internal.version`, `ro.sf.lcd_density`, `ro.zui.software.display`, `ro.miui.cust_hardware`, `ro.tcl.hardware.version`, `ro.mtk.build.version`, `ro.vivo.op.entry`, `ro.vivo.*`/`ro.huawei.*` (manufacturer version string), `persist.radio.vivo.fiveg`, `persist.vendor.radio.5g_mode_pref`, `ro.nubia.nr.support`, `ro.boot.5GEnable1/2`, `ro.denqin.*` (Changhong).

---

## 10. Privacy and Security Assessment

### 10.1 Data sensitivity classification

| level | Data | Authorization requirements |
|---|---|---|
| **Extremely high** | IMEI1/2, IMSI1/2, ICCID1/2, MSISDN1/2 (dual SIM card four elements) | READ_PHONE_STATE / READ_PRIVILEGED_PHONE_STATE (most also need to hide API or manufacturer AIDL) |
| **high** | CellID/LAC (can reverse base station location and trajectory), VoLTE/5G switch (can fingerprint user settings), application usage time (7 days), per application traffic | ACCESS_COARSE/FINE_LOCATION、PACKAGE_USAGE_STATS |
| **in** | Model/manufacturer/version string, RAM/ROM/available space, DataSlot, AccType, registration time | None (readable with zero permission) |
| **low** | Protocol version, client version number, DeviceType encoding | None |

### 10.2 Transmission and cryptography flaws[Observed]

1. **plain text HTTP** (not TLS), non-standard port 18080 → The same network segment can be sniffed, and the operator's intermediate device can be tampered with.
2. **AES key = plaintext MD5**: AES/ECB and the key can be derived from the ciphertext itself → encryption is just obfuscation, and anyone who intercepts can decipher all PII.
3. **`sign` has no timestamp/serial number/nonce**: can be replayed infinitely; `416` is also considered successful, further weakening the consistency.
4. **RSA-1024 + Self-signed 100-year certificate**: Insufficient strength; the certificate is only used to "sign MD5" and is not used for any identity authentication.
5. **VoLTE writing side effects (code exists, not enabled in this version)**: Base class `e0/r.c(ctx,slot)` (`e0/r.smali:560-688`), when `ImsMmTelManager.isAdvancedCallingSettingEnabled()` is false, reflection calls **`setAdvancedCallingSettingEnabled(true)` to force the user's VoLTE switch on**, and then reads and writes it back `VolteEnabled1/2` (requires `MODIFY_PHONE_STATE`). This path is gated by `f0/f.c`: `f0/f.c==true` **and** operators ∈ {46001,46006,46009} only take `c()`, otherwise take read-only `d()`. `f0/f.c` The whole project **has no write point (constant false)**, so this version actually only takes the read-only path; but `e0/b2,j0,k1,t0,t1,v1,w0` and the base class `l()` have a total of 8 and 16 call points in place. **can activate** with one static domain rewrite.
6. **access control is useless**: 5 inconsistent "agree" judgments + one `return true` hard-coding + all `a()` constant false.

### 10.3 Local storage[Observed]

- `SharedPreferences` uses the obsolete `MODE_MULTI_PROCESS` (no synchronization guarantee, cross-process consistency issues).
- `systemhelper.history` saves **'s complete reported message** (including IMEI/IMSI/mobile phone number) in clear text, up to 100 messages; `allowBackup=false` but root/privapp can be read directly.
- No encrypted storage, no integrity check.

### 10.4 Export component abuse surface [observation + inference]

| components | attack surface | Consequences |
|---|---|---|
| **`RegisterHelperService` (most serious)** | No authority, no authentication; `r(String)` changes to `serverAddress`; `i(int)` changes 24h to 1s; `u()` deletes configuration | ① Telemetry/privacy data **is redirected to the attacker server** (the victim device continues to send out IMEI/IMSI/location) ② The reporting frequency is amplified → traffic/power/server pressure ③ Configure DoS |
| `OnBootReceiver` | exported, only by action branch | Any application can trigger "collection + reporting" at high frequency to amplify traffic |
| `SIMStateReceiver` | exported, branch by action | Same as OnBootReceiver (and will hang a 6-minute delay task) |
| `TestActivity` | exported | Three system permission pop-ups can be raised (social engineering induced authorization), targetSdk 28 has no background startup restrictions |

> Note: Although `ConnectStateReceiver` is exported, `onReceive` only writes logs and does not constitute an amplification area.

> Reproduction path (only for local machine/authorization verification): Bind `com.mobiletools.systemhelper/.service.RegisterHelperService`, `asInterface`, then write `transact(1, ...)` to any `serverAddress`, and then send `BOOT_COMPLETED` to trigger a report to direct all the packets to your own server.

### 10.5 Privilege minimization problem

- `READ_EXTERNAL_STORAGE` has not been found to be actually used; `QUERY_ALL_PACKAGES` is only used for package name resolution of UsageStats (can be replaced by a visibility statement); `ACCESS_BACKGROUND_LOCATION` has no actual location and reporting path in this version. It belongs to **out-of-range application**.

### 10.6 Notification and Consent

- `setting_advice_title/content` ("The operator may collect your device information and other data through WLAN or mobile data network. Turning it on is deemed to be consent to the Internet, access to the application list and positioning.") **has zero reference to** in the code and resources → users will never see this notification.
- There is no "exit/close acquisition" UI; the only switch (`f0/f.g`) is always true in the memory, and there is no UI/AIDL to turn it off.
- The application name is in English `SystemHelper`, and the icon is the system Android robot → It has the characteristics of **system components disguised as** (in the pre-installed scenario, it is difficult for users to identify the components collected by the operator).

---

## 11. Inference of attribution and use (strictly distinguish between evidence and inference)

**Evidence supports**:
- Target domain name `dm.wo.com.cn` (China Unicom "Wo" system DM platform), port 18080, path `registerv3`;
- The subject/issuer of the built-in self-signed certificate is `UNICOM` (Beijing);
- Only report PLMN `46001/46006/46009` (China Unicom) in clear text, other operators will report MD5;
- Version number suffix `-U`; privapp whitelist (pre-installed form); private registration protocol of `SpecVersion=3.0`/`RegVersion=1.0`;
- A large number of China Unicom customized model branches (`Liantong`, `H10`, `DmUnicomAbsTelephonyManager`, `persist.sys.cucc.dm`).

**Inference (not evidence)**: The function of this component is positioned as "China Unicom terminal information registration/network quality and 5G/VoLTE capability census" - used for operator side statistics on the model distribution, dual-SIM capabilities, 5G/VoLTE activation status, resident cell and application usage portrait of terminals on the network; it is pre-installed on the customized machine `/system/priv-app` and is resident upon startup.
**failed to confirm**: the relationship between the signer and the official certificate chain of China Unicom (self-signed, unable to be verified); whether it is a stock commercial firmware version; whether there is a configuration issued by the server (this version of `serverAddress` can only be rewritten by the local AIDL, and there is no remote delivery code).

---

## 12. Uncertain items and verification suggestions

| # | unknown item | Recommend verification |
|---|---|---|
| 1 | `d0/g.P/Q` (int), `d0/g.O` (private String) semantics | Dynamic hook `d0/g` construction/serialization, or compare with a higher version of the same series APK |
| 2 | `signalhelper.signal` table has no writing method → `SignalInfo` actual content | Dump database and report text after running on real machine |
| 3 | Is `f0/f.b/c/e` "the server switch has been cut off" or is it a legacy from history? | Compare to version 1.1/1.0 APK; or grab the server return body (the current code does not parse the response body) |
| 4 | Two sets of manufacturers AIDL The real semantics of each transaction | Decompile nubia/Gree system APK, or dynamic trace Binder |
| 5 | `final b/c` constant of `e0/q1`, VoLTE/5G conversion threshold of each manufacturer | Value-by-value dump + real machine comparison |
| 6 | `firstPowerOn` Write back whether it is an empty string (affects `F` every time the computer is turned on) | Real machine observation prefs value |
| 7 | `e0/c2.o()` Idempotence of slot swapping | Compare `IMEI1/IMEI2/DataSlot` after two consecutive collections |
| 8 | Is the signer connected to the official China Unicom? | `apksigner verify --print-certs` + Comparison with China Unicom official application certificate |
| 9 | Whether the `res/xml` whitelist is actually adopted by a certain ROM | Capture target model `/system/etc/permissions/` directory |

---

## Appendix A: Class Index (Own Code)

| package | Number of classes | content |
|---|---|---|
| `com.mobiletools.systemhelper` | 15 | Activity 1, Receiver 5, Service 5 (including internal classes), Application 1, R 1, AIDL 2 |
| `e0` | 85 | Base class 1 (`r`), Orchestrator 1 (`c2`+3 internal class), 8 bridges, 33 OEM Reader, the rest are OEM variants/internal classes |
| `d0` | 15 | `g`(DTO) `e`/`f`(monitoring) `h`(IP) `i`(UsageStats) `j`(signal history) `k`(reporting) `a,b,c,d`(API bridge) |
| `f0` | 15 | `a`(alarm) `b`(JSON bean) `c`(crypto) `d`/`m`(DB) `e/f`(constant) `g`(HTTP) `h`(MD5) `i`(memory) `j`(log) `k`(RSA) `l`(prefs) `n`(bean) `o`(reflection) |
| `a,a0,b,c,d,e,i,j,k,m,n,s,t,u,v,w,x,y,z` | 30+ | Thread pool/resource/AppCompat/ConstraintLayout and other libraries and tools |

## Appendix B: REA Evidence List (Summary)

See §1.3 table (8 pieces of core evidence), the complete evidence chain can be exported through `get_evidence_bundle`. Four sub-reports:

- `findings\app-shell.md` (42.7 KB) — Component/Tool Layer/Resource
- `findings\collectors-1.md` (63.4 KB) — e0/a…l1 42 categories in total
- `findings\collectors-2.md` (82.6 KB) — e0/m…z1 41 categories in total
- `findings\device-core.md` (52.4 KB) — layer d0 + e0/r + e0/c2 + AIDL
- `findings\c2_map_a_full.tsv` — 304 `MODEL→Brand` fully mapped

## Appendix C: Table of Key Constants

| constant | value | location |
|---|---|---|
| Report address | `http://dm.wo.com.cn:18080/registerv3` | `f0/f.w` |
| IP services | `http://dm.wo.com.cn:18080/public/ip` | `f0/f.x` |
| regular cycle | 86,400,000 ms（24h） | `f0/f.y` |
| App manifest cycle | 604,800,000 ms（7d） | `f0/f.z` |
| first delay | 240,000 ms（4min） | `OnBootReceiver:112` |
| Network retry | 3,000 ms（3s）×3 | `OnBootReceiver:280`、`f0/f.t=2` |
| SIM delay | 360,000 ms（6min） | `SIMStateReceiver:211` |
| success code | 200（+416） | `f0/g`、`d0/k:273` |
| history upper limit | 100 items | `f0/d` |
| listening mask | `0x100500` | `d0/e`、`d0/f` |
| Protocol version | `SpecVersion=3.0`、`RegVersion=1.0` | `d0/g` |
| China Unicom PLMN | 46001 / 46006 / 46009 | `f0/l.n/m` |
| Certificate | RSA-1024, self-signed CN=UNICOM, 2019→2119 | `assets/pub.cer` |

---

## Appendix D: `e0` 85-class full-featured index (complete list of `e0/` packages)

> Note: **bridge**=API compatible bridge (it does not produce data); **base class/arrangement**=infrastructure; the rest are manufacturer collectors. Privacy level: **extremely high**=direct output IMEI/IMSI/ICCID/mobile phone number/cell, **high**=application list/IP, **low**=infrastructure.

| class | Role/Brand | critical private channel | Privacy |
|---|---|---|---|
| `a` | BROR / Mobile Customization CM (YunOS) | `gsm.yunos.imei0/1`、`ro.yunos.*` | extremely high |
| `a0` | DOUQI (including RAM/ROM/remaining space) | `StatFs(/system,/data)`、`ro.boot.dram_info` | extremely high |
| `a1` | 5G HYUN / OFeng（5GP50） | MTK `TelephonyManagerEx`+`phone_ex`; **access control hard coded `return true`** | extremely high |
| `a2` | YuLong / Coolpad | `ro.yulong.version.hardware`、`ro.config.le_hardware_version` | extremely high |
| `b` | **bridge** `TelephonyManager.getImei()` | — | low |
| `b0` | EAMEY | `ril.imei.sim1/2` | extremely high |
| `b1` | Onefive | `/proc/meminfo`、`ro.bd.cmcc_*` | extremely high |
| `b2` | ZTE (including China Unicom DM customization) | `com.android.vts.VendorTelephonyManager`、`DmUnicomAbsTelephonyManager.getVoLTEState`、`dm_user_agreement` | extremely high |
| `c` | Baidu / Spreadtrum (XDM-01) | `dm_user_agreement`、`DeviceType=007001029` | extremely high |
| `c0` | EWPE / Gree | **manufacturer AIDL `IGreeTelephonyManager`** (`operator_service_improve_plan` access control) | extremely high |
| `c1` | OPPO / ColorOS (110+ models) | `ColorOSTelephonyManager`、`isOemEnhanced4gLteModeSettingEnabledByUser`、`ro.boot.dram_info` | extremely high |
| `c2` | **Orchestrator** DeviceManager (RegType distribution/vendor selection/reporting trigger) | — | low |
| `c2$a` | Callback: IP/reporting thread after UsageStats is completed | — | high |
| `c2$b` | Callback: `d0/h` Write IPv4/IPv6 after completion | — | high |
| `c2$c` | Interface: Reader contract `a(ctx)Z`/`b(ctx,g)V` | — | low |
| `d` | **bridge** `unregisterTelephonyCallback` | — | low |
| `d0` | Gionee | `device_provisioned`+`operator_registration_switch`、`ro.ssui.*` | extremely high |
| `d1` | ORANGE (RGK OEM) | `gsm.rgk.imei1/2`、`ro.qc.*` | extremely high |
| `d2` | **reflection tool**: `getXXX(int)`→`getXXX(long)` automatic fallback, `getSubId` double type analysis | — | low |
| `e` | **bridge** `CellIdentityNr.getNci()` | — | low |
| `e0` | TOSOT (Dasong/Gree) | Same as `c0` (Gree AIDL) | extremely high |
| `e1` | OYSIN | `ro.xh.display.version` etc. | extremely high |
| `f` | **bridge** `CellIdentityNr.getTac()` | — | low |
| `f0` | Haipainoble | **MTK `ITelephonyEx.getSvlteImei`** | extremely high |
| `f1` | OnePlus（LE21xx） | `ro.boot.dram_info`、`SubscriptionManager` | extremely high |
| `g` | **bridge** `ImsManager.getImsMmTelManager(int)` | — | low |
| `g0` | Hisense | `ro.hmct.volte.enabled`, `persist.sys.volte.enable`; `DeviceType` is the model name written | extremely high |
| `g1` | PHICOMM | The manufacturer of **reflection `android.os.Build` injects the static field** (OS_NAME/HARDWARE_VERSION_DISPLAY…) | extremely high |
| `h` | **bridge** `ImsMmTelManager.isAdvancedCallingSettingEnabled()` | — | low |
| `h0` | HUAWEI | `HwTelephonyManager.getServiceAbility`、`TelephonyManagerEx.getDefault4GSlotId` | extremely high |
| `h1` | PHILIPS | `ro.custom.hw.version`；`isValidSubscriptionId` setAccessible | extremely high |
| `i` | **bridge** `getImei(int slot)` | — | low |
| `i0` | **bridge** `SubscriptionManager.getSubscriptionIds(int)` | — | low |
| `i1` | PPTV | MTK `getSvlteImei`；`ro.pptv.version` | extremely high |
| `j` | **bridge** `Context.getMainExecutor()` | — | low |
| `j0` | HZTL（TAI-M01） | `HwTelephonyManager`+`MSimTelephonyManager` | extremely high |
| `j1` | PROTRULY | `ro.hardware_version` + `d2` reflection | extremely high |
| `k` | **bridge** `registerTelephonyCallback(Executor,Callback)` | — | low |
| `k0` | INTKI | Reflection `getCustomHWBuildVer/getCustomSWBuildVer` | extremely high |
| `k1` | PTAC (Huawei Smart Selection BD00 series) | Same as `j0` | extremely high |
| `l` | **bridge** `ServiceState.getNetworkRegistrationInfoList()` | — | low |
| `l0` | **bridge** `CellInfoNr.getCellIdentity()` | — | low |
| `l1` | QIKU | `ro.yulong.version.hardware` | extremely high |
| `m` | **bridge** `NetworkRegistrationInfo.getDomain()` | — | low |
| `m0` | JingLing（JingPad） | `TelephonyManagerEx`、`getCellLocationForPhone` | extremely high |
| `m1` | realme | `ColorOSTelephonyManager`、`ro.boot.dram_info` | extremely high |
| `n` | **bridge** `getTransportType()` | — | low |
| `n0` | kingsun (three sets of readings for ** **) | `MSimTelephonyManager.getPhoneCid/getPhoneLac` | extremely high |
| `n1` | Shown（360OS） | `gsm.mtk.imei1/2`、`ro.system.OSVersion` | extremely high |
| `o` | **bridge** `getCellIdentity()` | — | low |
| `o0` | KONKA | `getNetworkOperatorForPhone`、`ro.boot.hardware.revision` | extremely high |
| `o1` | OYSIN / SINTAVE | `isValidSubscriptionId`、`getDeviceId` | extremely high |
| `p` | **bridge** `CellIdentityTdscdma.getCid()` | — | low |
| `p0` | K-Touch | `PhoneFactory.getPhone(i)` | extremely high |
| `p1` | SOP | `/proc/meminfo`、`StatFs(root)` | extremely high |
| `q` | **bridge** `CellIdentityTdscdma.getLac()` | — | low |
| `q0` | lephone (**five sets of pronunciation**) | `getAllCellInfo` list parsing, `getMsisdn` | extremely high |
| `q1` | Samsung (5G/VoLTE Specialized) | `getTelephonyProperty`、`Settings$System["voicecall_type"]`、`ro.telephony.default_network` | extremely high |
| `r` | **base class** DeviceInfoReaderBase (template + static monitoring + dual card reading and writing) | ImsMmTelManager / CellIdentity* / SubscriptionManager | extremely high |
| `r0` | LETV (five sets of reading methods) | `com.mediatek.telephony.TelephonyManagerEx` | extremely high |
| `r1` | Spreadtrum | `phone_ex` + `getMsisdn` | extremely high |
| `s` | Bee | `getAllCellInfo`、`ro.sf.lcd_density` | extremely high |
| `s0` | Lenovo (tablet: single card 16 fields only) | `Settings$Global`、`getDefaultDataSubscriptionId` | extremely high |
| `s1` | TCL (95%+ homologous to `y`/`z1`) | `ColorOSTelephonyManager`、`ro.tcl.hardware.version` | extremely high |
| `t` | BenWee | `MSimTelephonyManager.getDefault` full field | extremely high |
| `t0` | Liantong / H10 (Customized by China Unicom) | `Settings$Global["is5GEnable"/"preferred_network_mode"]`、ColorOS | extremely high |
| `t1` | TDTECH (Dingqiao) | `HwTelephonyManager.getServiceAbility`、`ro.boot.dram_info` | extremely high |
| `u` | BestSonny | `getDeviceId`/`getMsisdn`/`getNetworkOperatorForPhone` | extremely high |
| `u0` | meizu | `MzTelephonyManager`、`getSimOperatorNumericForPhone` | extremely high |
| `u1` | vivo (**deepest**) | `ITelephony`+`VivoTelephonyApiParams.getParamValues`（`API_TAG_getNrCapability/getNRSwitchState/isVolteEnable`）、`persist.radio.vivo.fiveg`、`ro.vivo.op.entry` | extremely high |
| `v` | CHANGHONG (**only uses model, does not touch SIM**) | `ro.denqin.*`、`ro.yunos.*` | low |
| `v0` | **bridge** `SubscriptionInfo.getMncString()` | — | low |
| `v1` | WIKO / PTAC | Same as `t1` + public API fallback + variable length reflection | extremely high |
| `w` | ChuiZi (hammer/nut) | `ro.smartisan.version` | extremely high |
| `w0` | motorola / ZUI | `getActiveSubscriptionInfoForSimSlotIndex`、`ro.zui.software.display` | extremely high |
| `w1` | Xiaodu | `StatFs` three-partition capacity, `ro.boot.5GEnable1/2`, `persist.sys.cucc.dm` | extremely high |
| `x` | **bridge** `SubscriptionManager.getSlotIndex(subId)` (26 calls) | — | low |
| `x0` | mfox | `gsm.mtk.imei1/2`, `e0.d2` reflection | extremely high |
| `x1` | Little pepper (**model has the densest branch**) | 20+ `Build.MODEL` branch, `ro.product.yuxin*` | extremely high |
| `y` | Coolpad (same origin as `s1`/`z1`, log residue `DeviceInfoReaderOPPO`) | `ColorOSTelephonyManager`、`ro.board.hardware_version` | extremely high |
| `y0` | BROR / NOAIN | **`ITelephonyEx$Stub` + `ServiceManager.getService("phoneEx")` + `getSvlteImei`** | extremely high |
| `y1` | Xiaomi (**Max: 200+ model whitelist**) | `miui.telephony.TelephonyManager(Ex)`、`isFiveGCapable`、`/proc/meminfo` | extremely high |
| `z` | **bridge** `SubscriptionManager.getActiveDataSubscriptionId()` | — | low |
| `z0` | nubia | **manufacturer AIDL `INBExtTelephony`**, `ro.nubia.nr.support`, `ro.build.rom.pcb.silkscreen` | extremely high |
| `z1` | YinKe (sound engraving) | Same as `s1`/`y` | extremely high |

> Counting check: `e0` has 85 files in total = 83 in the above table + `e0/r` (base class) + `e0/c2` (arranger). 20 of them are API bridges, 60 are OEM collectors/variants, 1 base class, 1 orchestrator + 3 inner classes.
> Full class-by-class details (API calls for each class, key name source, access control, downgrade branch, smali line number) are found in `findings/collectors-1.md` (a–l1, class 42) and `findings/collectors-2.md` (m–z1, class 41).
