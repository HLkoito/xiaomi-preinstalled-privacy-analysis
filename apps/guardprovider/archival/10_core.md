> **Historical analysis with subsequent corrections.** Use the [final review](../REPORT.md) for the reviewed findings. It supersedes earlier claims about plaintext IMEI lists, actual uploads under default settings, absolute coverage, and unverified native behavior. The DAU IMEI and IMSI lists use MD5 and remain subject to region, permission, and network checks. Certificate expiry alone does not establish that ciphertext cannot be decrypted. Local identity paths were removed. Historical source line references may point to files outside the published evidence set.

# Application main control layer (process initialization/IPC facade/scan orchestration/engine adaptation)

> Undertake `00_brief.md`. Coverage of this article (division of labor after lead convergence):
> `com/miui/guardprovider/**` (bridge point outside core/engine/manager/natives/sdk)
> \+ `n0` `c0` `i0` `d0` `w0` `x` `f0` `y` eight packages
> \+ Points for direct interaction with native (`p0/y.java`, `natives/RustSupport`).
> The per-packet semantic mapping of obfuscated packages (`a6`/`s4`/`c4`/`l2`/`x4`/`f2`/`o2`/`u2`…) is responsible for **t-obf** (see `_obf_index.tsv` and t-obf report), whose conclusions are cited only where necessary.
>
> Statement classification: **[Observed]**= Source code/bytes are directly visible; **[Inferred]**= Evidence-based semantic induction; **[Unknown]**= Not covered.

---

## 1. Overview of Responsibilities

`com.miui.guardprovider` is the **antivirus back-end process** of the MIUI Security Center (`com.miui.securitycenter`). It has no UI, no Activity, and no boot receiver. It only exposes two exits:

| export | Type | Permission |
|---|---|---|
| `com.miui.guardprovider.GuardProvider` | ContentProvider，authority `guard`，`android:exported="true"` | `com.miui.guardprovider.permission.Guard_Provider`（signature\|privileged） |
| `com.miui.guardprovider.manager.SecurityService` | Service, `exported="true"`, double action | Same as above |

The main control layer does four things:

1. **process/state initialization**: Application only does SharedPreferences warm-up + user unlock waiting; all engines SDK (AVL / TMSDK), OneTrack, and databases are **lazy initialization**, triggered by the first scan (§4.1).
2. **IPC Facade**: Translate the 5 URI + 2 `call()` methods of the ContentProvider and the 2 AIDL interfaces (24 + 2 transaction codes) of the Service into the pair `n0.g` Invocation of facade (§4.2, §4.4).
3. **scan orchestration**: `n0.g` (`n0/g.java`, 1450 lines) single instance `g.z()` holds task table, according to enable engine collection **one thread per engine** concurrent scanning, multi-engine result counting and merging according to file path, current limit up to 10 concurrent tasks, 2 Minute timeout, support for cancellation and process death cleanup (§4.3).
4. **engine adapts to**: `f0.a` abstract contract + 4 implementations (`Mi`/`MiEngineNG` uses Kotlin+Rust, `AVL` uses `libavlm.so`, `TENCENT` uses TMSDK), and uniformly converts the respective results into `y.f` (`VirusInfo`) model (§4.5).

Supported by three local decision libraries (§4.6):
- `miui_antivirus.db` (**virus issued by the server/gray production library**)
- `mi_engine_white_virus.db` (**whitelists**, `getRiskAppData` uses it as exemption)
- `risk_gray_result.db` (**gray scan result cache**, data source of `getRiskAppData`)

---

## 2. Component and file list

### 2.1 `com.miui.guardprovider.*`（core / engine / manager / natives）

| File | class | Purpose |
|---|---|---|
| `com/miui/guardprovider/GuardApplication.java` | `GuardApplication` | Process entry; `attachBaseContext` preheats SP or registers `USER_UNLOCKED` receiver; `onCreate` → Hilt (empty) + `w.c.a()` (empty method); static `f1743d` global Context |
| `com/miui/guardprovider/b.java` | `b` (abstraction in package) | `extends Application implements d1.b`; `onCreate` first runs Hilt and injects `c()`, then `super.onCreate()` |
| `com/miui/guardprovider/a.java` | `a` / `a$a` / `a$b` | Hilt generates components; `b` implements `w.d.a(GuardApplication)` **method body is empty** → injection without any side effects |
| `com/miui/guardprovider/GuardProvider.java` | `GuardProvider` + internal `a` (observer), `b` (result container) | ContentProvider: `query` 5 URI, `call` 2 methods, `insert/delete` maintenance `t_virus_white_list` |
| `com/miui/guardprovider/manager/SecurityService.java` | `SecurityService` + `a`、`b` | AIDL service: `onBind` press action to return `y.a` (24 transactions) or `y.b` (2 transactions) |
| `com/miui/guardprovider/engine/antiy/AvlEngine.java` | `AvlEngine` | AVL adapter (`com.avl.engine.*`) |
| `com/miui/guardprovider/engine/mi/MiEngine.java` | `MiEngine` | Legacy MI Engine Adapter (`i0.a` Data Source + `g0.e` Scan) |
| `com/miui/guardprovider/engine/mi/MiEngineNG.java` | `MiEngineNG` | New MI engine adapter (Kotlin, delegate `p0.n` → Flamingo/Rust) |
| `com/miui/guardprovider/engine/tencent/TencentEngine.java` | `TencentEngine` | Tencent TMSDK adapter (QScannerV2 / UpdateManager / WifiDetect) |
| `com/miui/guardprovider/natives/RustSupport.java` | `RustSupport` | `System.loadLibrary("flamingo")` + 3 native methods |
| `com/miui/guardprovider/natives/RustException.java` | `RustException` | Kotlin `Exception` subclass, only captured by `p0/y.java` |
| `com/miui/guardprovider/sdk/android/db/*` | `EngineDatabase` / `EngineDatabase_Impl` | Room `flamingo.db` (8 tables, version 2) - see t-cloud report for details |
| `com/miui/guardprovider/sdk/android/pojo/**` | 28 pojos | Flamingo SDK data model (`ScanTarget`/`Condition`/`StringItem`/`GuiltyPackageRecord`/`TrustCertRecord`/`networkdomain.*`) |

### 2.2 Eight managed packages

| package | main class | Purpose |
|---|---|---|
| `n0` | `g`(1450)、`k`(105)、`j`(94)、`a`(32)、`h`(32)、`i`(17) | `g`=AntivirusManager scanning facade; `k`/`j`=UrlScanManager / WifiDetect distribution; `a`=Fixed 6 thread pool; `h`=Result broadcast; `i`=Engine reflection instantiation |
| `c0` | `d`(289)、`b`(164)、`c`(60)、`a`(45) | `d`=ScanResultDBManager(`scan_result.db`); `b`=GrayResultDBManager(`risk_gray_result.db`); `c`/`a`=two SQLiteOpenHelper |
| `i0` | `l`(417)、`d`(211)、`f`(140)、`h`(93)、`g`(53)、`e`(51)、`b`(26)、`m`(26)、`a`/`c`/`i`/`j` | `l`=Virus database query; `d`=Whitelist/Virus database network synchronization; `f`=Whitelist determination (`e(pkg)`/`d(pkg,sig)`/`f(path)`); `h`=Package/Signature Tool; `g`=Signature Extraction; `e`/`b`=DB helper;`m`=result record;`a`/`j`/`i`=interface |
| `d0` | `a`(13)、`b`(37)、`c`(12)、`d`(27) | `d`=`android.os.SystemProperties` reflection; `b`=`MiuiSettings$SettingsCloudData` / `MiuiSettings$Privacy` reflection (cloud control); `c`=universal reflection invoke; `a`=`AppOpsUtils.isXOptMode` |
| `w0` | `f`(85)、`d`(128)、`h`(94)、`i`(29)、`a`(26)、`b`(26)、`c`(11)、`e`(20)、`g`(49) | `f`=`SharedPreferences("GuardProvider")` package; `d`=PackageParser/archive package parsing; `h`+`i`=`miui_virus.db` v5; `a`=file MD5; `b`=deviceId/UUID; `c`=Log empty implementation; `e`=Cross-application "authorization"; `g`=Unlock judgment/archive PackageInfo |
| `x` | `a`(197)、`b`(199) | `a`=EngineSettingUtils (engine activation initial value/cloud engine rotation); `b`=VirusEngineCloudControl (cloud control pull engine list and weight) |
| `f0` | `a`(102)、`b`(61) | `a`=Engine abstract base class; `b`=**engine registry enumeration** (3 constants) |
| `y` | `a`(226)、`c`(289)、`f`(163)、`d`(61)、`b`(48)、`e`(51) | AIDL: `a`=`IAntiVirusServer`(24), `c`=`IVirusObserver`(13), `b` =`IURLScanServer`(2), `d`=`IWifiDetectObserver`(1); `f`=`VirusInfo` (Parcelable Result model); `e`=`EngineUpdateInfo` |

### 2.3 Other packages that directly collaborate (only those used in this article)

| package | class | Purpose |
|---|---|---|
| `p0` | `y`(132) | `Callable`, call `RustSupport.matches/search` for feature code matching (**’s only native call point**) |
| `p0` | `n`/`g`/`f`/`z`/`b0`/`c` | Flamingo scanning pipeline (see t-cloud report for details, this article only quotes the `scan_params` bit field) |
| `b0` | `a` | Single task status container (engine instance, progress, cloud scan switch, extras) |
| `o0` | `a` | `IVirusObserver` all-empty implementation base class for reuse of `GuardProvider` and incremental scanning |
| `j0` | `c`/`a`/`b` | AntiDefraudAppManager (`/detect/app` upload + pull) |
| `g0` | `e` | MiEngineUtils (old MI engine scanning implementation) |
| `h0` | `b`/`a` | NetworkApiHelper (HttpURLConnection + MD5 signature), mix-scan detection |
| `z` | `a`/`b`/`c` | OneTrackManager (buried point, appId `2882303761517522561`) |
| `v` | `a` | IdentifierManager(OAID/UDID/VAID/AAID reflection) |
| `u` | `a` | `com.miui.analytics.ITrack` client stub |
| `d1` / `w` / `e0` / `a1` | — | Hilt/Dagger products and interfaces (no side effects during runtime) |

### 2.4 Obfuscation package → Semantic mapping (**range limited version**)

> The package-by-package determination of a total of 3231 categories/approximately 1300 obfuscated files is produced by t-obf (`_obf_index.tsv`). The following table only includes the packages that **must reference** in the argumentation process of this article for cross-checking by subsequent members.

| package | Number of files | Semantic decision | Basis |
|---|---|---|---|
| `n0` | 6 | AntivirusManager / UrlScanManager Facade | Log tag `AntivirusManager`(g)、`UrlScanManager`(j) |
| `c0` | 4 | Gray output/scan results DB management | Log tag `GrayResultDBManager`(b)、`ScanResultDBManager`(d) |
| `i0` | 12 | Virus database/whitelist/package tool | Log tag `MiEngine`(l)、`PackageUtils`(h)、`MiWhiteVirusDBHelper`(e) |
| `d0` | 4 | MiuiSettings / SystemProperties reflection compatibility layer | Log tag `MiuiSettingsCompat`(b), `DeviceUtils`? None; class name reflection string |
| `w0` | 9 | General tools (SP/package parsing/MD5/DB) | Log tag `GuardProvider`、`MD5`、`PackageUtils` |
| `x` | 2 | Engine activation strategy + cloud control | Log tag `EngineSettingUtils`、`VirusEngineCloudControl` |
| `f0` | 2 | Engine abstraction + registry enumeration | Class structure + REA `ev_80d5f9…` |
| `y` | 6 | AIDL interface + Parcelable result model | `onTransact` Medium `enforceInterface("com.miui.guardprovider.aidl.*")` |
| `o0` | 1 | `IVirusObserver` empty implementation base class | `extends c.a` |
| `b0` | 1 | Task status container | `new b0.a(aVar)` unique construction point `n0/g.java:512` |
| `g0` | 1 | MiEngineUtils (old MI scan) | Log tag `MiEngineUtils` |
| `h0` | 2 | Network API Assistant + mix-scan detection | Log tag `NetworkApiHelper`; read `securitycenter.support.mix.scan` |
| `j0` | 3 | Anti-fraud cloud detection (`/detect/app`) | Log tag `AntiDefraudAppManager` |
| `z` | 3 | OneTrack buried point packaging | Log tag `OneTrackManager`；`OneTrack.createInstance` |
| `v` | 1 | Device identification reflection (OAID, etc.) | Log tag `IdentifierManager`; reflection `com.android.id.impl.IdProviderImpl` |
| `u` | 1 | `com.miui.analytics.ITrack` client stub | `writeInterfaceToken("com.miui.analytics.ITrack")` |
| `p0` | 13 | Flamingo scanning pipeline (Code/Cloud/Matcher/Real Scanner + WhiteFilter + MiEngineNG façade) | Log tag `CodeScanner`/`CloudScanner`/`MatcherScanner`/`RealScanner`/`WhiteFilter`/`MiEngineNG` |
| `w1` | 34 | Kotlin `kotlin.jvm.internal`（`w1.l`=`Intrinsics`，`w1.n`=`Lambda`） | `w1.l.f(Object,String)` and other signatures |
| `v1` | 23 | Kotlin `kotlin.jvm.functions`（`Function0..N`） | All 6 lines of interface declaration |
| `g1` | 27 | Kotlin `Result`/`Unit`（`g1.z.f4408a`=`Unit.INSTANCE`） | `MiEngineNG` Return `z.f4408a` |
| `m1`/`k1`/`l1`/`q1` | 11/8/4/1 | kotlinx.coroutines basic (Continuation/SuspendLambda/Job) | `m1.a`/`k1.d`/`l1.d`/`q1.e` reference relationship |
| `s4`/`x4`/`w4`/`t4`/`z4`/`u4`/`v4` | 89/43/12/5/14/22/16 | kotlinx.coroutines runtime + Flamingo scheduler | `s4.c0`=Dispatcher、`s4.i.d`=launch |
| `e`/`h`/`i`/`j`/`g` | 27/13/5/1/5 | androidx.room / androidx.sqlite.db | tag `ROOM`(e)、`SupportSQLite`(h/i)、`SupportSQLiteLock`(j)、`RoomCursorUtil`(g) |
| `l`/`n`/`o`/`r`/`s`/`p`/`q`/`m`/`d`/`b`/`c`/`f`/`k` | 23/14/14/4/1/1/1/5/4/4/1/1/1 | **Gson** (R8 renamed) | `l.d`=Gson main class (`ConcurrentMap` TypeToken cache), `o.n`=TypeAdapters, `p.a`=ISO8601Utils, `s.a`=TypeToken, `m.c`=`@SerializedName` |
| `d6`/`f6` | 23/25 | Retrofit2 (core/annotations) | `d6.u`=Retrofit、`f6.f`=`@GET`、`f6.o`=`@POST`、`f6.a`=`@Body`、`f6.t`=`@Field` |
| `g5` | 33 | OkHttp3 | `g5.z`=`OkHttpClient implements Cloneable, e.a`、`g5.w`=Interceptor |
| `c4` | 73 | androidx.collection (ArrayMap/SimpleArrayMap, etc.) | `c4.e0 implements m2.a, g4.i` |
| `h1` | 46 | Kotlin `kotlin.text` (`StringsKt`, etc.) | `h1.y extends x`, `h1.q`=`emptyList` Usage |
| `l2` | 65 | Kotlin `kotlin.collections` extension | `l2.z0` is interface, referenced by 200+ classes |
| `p4` | 25 | Kotlin `kotlin.ranges`/`sequences` auxiliary | `p4.v`/`p4.w` high frequency is used for `for` cycle crossover |
| `a6`/`o2`/`f3`/`t5`/`y`/`b4`/`i2`/`d3`/`d4`/`q3`/`w2`/`o3` | 114/40/23/30/14/14/14/30/22/27/8/19 | **Tencent TMSDK/TMF** (implementation of `tmsdk.*`, `tmsdkobf.*`) | Both import `tmsdk.common`/`tmsdk.bg`; `a6` exclusive 9039 lines `z2.java` |
| `q0` | 18 | Flamingo Cloud API layer (7 Retrofit endpoints + signature interceptor + base URL) | `@f6.*("/virus/…")`、`q0.m.f7703a` |
| `r0` | 44 | Flamingo Room DAO/Entity + TypeConverter | `EngineDatabase.F()/G()…` Return `r0.*` |
| `s0`/`u0`/`t0`/`l0`/`m0` | 30/10/21/6/1 | Flamingo SDK Bootstrap/Response Packaging/Domain Model/Pipeline Stage/Room Adaptation | `s0.s` builds Retrofit, `t0.a`=ScanTarget interface, `l0.b/c/d/e`=Stage interface |
| `c5`/`d5`/`y0`/`x0`/`z` | 1/1/1/1/3 | Device determination / global `_global` flag / crash stack MD5 / `content://guard/virus` constant / buried point | `c5.a`=DeviceUtils、`d5.a.f2965a` |

---

## 3. Data flow/timing

### 3.1 Process startup sequence (`b`/`GuardApplication`)

```
zygote → new GuardApplication
  └─ Application.<init>  → com.miui.guardprovider.b.<init>            [b.java:34]
       └─ new dagger.hilt.android.internal.managers.a(new a())      [b.java:21]
  └─ attachBaseContext(ctx)                                          [GuardApplication.java:72]
       ├─ f1743d = this                                              [GuardApplication.java:74]
       ├─ w0.g.b(ctx) == androidx.core.os.a.a(ctx) (User unlocked?) [w0/g.java:19]
       │    ├─ true → w0.f.b(ctx) warm-up SharedPreferences("GuardProvider") [w0/f.java:36]
       │    └─ false → f()   registerReceiver(new a(), ACTION_USER_UNLOCKED) [GuardApplication.java:50-56]
       │                 └─ onReceive: w0.f.b(ctx) + g()(logout) + nothing else [GuardApplication.java:29-35]
  └─ onCreate()                                                      [GuardApplication.java:83]
       ├─ super.onCreate() → b.onCreate → c() → dagger injection [b.java:44-47]
       │    └─ com.miui.guardprovider.a$b.a(GuardApplication) **method body is empty** [a.java:29-31]
       └─ w.c.a(this) **method body is empty** [w/c.java:8-9]
```

**[Observed]** Application stage **does not have any** SDK initialization: do not call `TMSDKContext.init`, do not call `AVLEngine.init`, do not call `OneTrack.createInstance`, do not create a database, do not create a directory, do not read anything `SystemProperties` (the 87 lines of the full text of `GuardApplication` only have the 4 external calls listed in §4.1).
**[Observed]** `guardprovider.support.mix.scan` / `guardprovider.scan_properties` Two meta-data reads **at**0 in the whole tree Java (grep `scan_properties` / `mix.scan` only hits `h0/a.java:14` and reads SecurityCenter another key). → Dead configuration.
**[Observed]** process-level static initialization is only: `d5.a.f2965a` (read `ro.product.mod_device` to determine `_global`, `d5/a.java:10`), `h0.b`’s 4 `d.a(...)` Static fields (`h0/b.java:31-34`), `f0.b` enumeration `b()` (**is triggered when `VirusScan/miEngineNgEnable`, `f0/b.java:56-60`,** first accesses `f0.b.values()` from cloud control).

### 3.2 `guard/sync_scan` (1006) → native scan complete link

Trigger: `contentResolver.query(Uri "content://guard/sync_scan", ..., selectionArgs=[path, bool])`.

```
GuardProvider.query                                        [GuardProvider.java:381]
 └─ case 1006: selectionArgs must have length == 2, otherwise return null [395-398]
 └─ e(strArr2[0], Boolean.parseBoolean(strArr2[1]))          [399]
     └─ f(path, useCloud, engineName=null)                   [192-193]
         ├─ path starts with "content://":
         │    copy to cacheDir/copy.apk (Files.copy + REPLACE_EXISTING) [211-216]
         ├─ path starts with "file://": take Uri.getPath() [269-270]
         ├─ g.z().L(new String[]{path}, observer a, useCloud, 2, null)      [247-250]
         │    ├─ Filter null and !File.exists() [1260-1271]
         │    ├─ If list is empty or static task count f6412h >= 10 → observer.v(-1,null); return -1 [1272-1282]
         │    ├─ w0.e.a(ctx) → Request GrantGuardProvider (get external storage permission) from "content://com.lbe.security.miui.su" [w0/e.java:8-17]
         │    ├─ Create 5 tables: f6418d (signature cache) f6417c (result) f6419e (gray mark) f6420f (scanType) f6416b (task table) [1285-1289]
         │    └─ new n0.g$f(...).executeOnExecutor(THREAD_POOL_EXECUTOR)      [1290-1292]
         └─ countDownLatch.await(2, TimeUnit.MINUTES) ← **2 minute hard timeout** [250]
            Timeout or iL<0 or result is null → return null (Cursor is null) [251-256]

n0.g$f.doInBackground (AsyncTask, 1 time) [n0/g.java:791]
 ├─ observer.d(1)                    // onScanStarted                     [796]
 ├─ observer.c(-1, paths[0])         // onScanStartItem                   [803]
 ├─ Path-by-path whitelist preflight (CheckWhiteList): [811-859]
 │    ├─ PackageParser.parsePackageLite(new File(path), 0)           [821]
 │    ├─ i0.h.d(ctx, packageName) or i0.h.f(ctx, path) get PackageInfo [826]
 │    ├─ i0.g.a(new PackageParser.Package(pkg), info) → Signature[]   [831]
 │    ├─ i0.h.c(sigs) → MD5(hex, multi-signatures are connected with '|') [832, i0/h.java:36-50]
 │    ├─ Cache d{path→(pkgName, versionName, versionCode, sigMd5)} [827-837]
 │    ├─ i0.f.a(ctx).d(pkg, sigMd5) hit → Considered safe:
 │    │ arrayList.remove(path); level=1; direct observer.s(progress) [838-857]
 │    └─ Miss → Reserved for scanning
 └─ onPostExecute: If there are still items to be scanned → K(list, observer, useCloud, 2, null, number of skipped items) [866-874]
      If all are skipped by the whitelist → cancel after observer.v(-1, whitelist result array) [875-890]

n0.g.K(list, observer, useCloud, scanType, extras, offset)    [n0/g.java:1132]
 ├─ listT = t(x.a.c()) // x.a.c() = list of engine names enabled in SP [1134]
 │    └─ t(): Arrays.stream(f0.b.values()).filter(name in listT)
 │            .map(new n0.i().a(bVar))                                [1172-1188]
 │            └─ n0.i.a(): bVar.f3549d.getConstructor(f0.b.class).newInstance(bVar) ← **reflection instance** [n0/i.java:10-15]
 ├─ this.f6415a.put(observer, new ConcurrentHashMap()) // path→number of completed engines [1136]
 └─ for engine in listT:                                              [1137-1146]
      ├─ c task = u(observer, engine); task.p(p(task)) // Assign taskId 0..100 ring [1139-1140]
      ├─ task.n(listT) //Record the number of engines in the same batch [1141]
      ├─ list2.add(task)                                                  [1142]
      ├─ b thread = new b(task, new a(mainLooper, observer, list2, listT, offset, list),
      │                    list, useCloud, extras)                        [1143]
      ├─ task.q(thread)                                                   [1144]
      └─ thread.start() ← **One thread per engine, concurrently start** [1145]
 └─ z.a.c("security_scan") // OneTrack buried point (the first call triggers OneTrack init) [1147]

n0.g$b.run (threads per engine) [n0/g.java:275-279]
 ├─ task.o(handler) // Bind the main thread Handler
 └─ task.r(list, useCloud, extras)                            [620-628]
      ├─ f6438c.d(useCloud) // b0.a.f311f = Cloud scan switch
      ├─ f6438c.e(list) // b0.a.f312g = Copy of the list to be scanned
      ├─ f6438c.f314i = extras
      ├─ f6438c.f307b = true
      └─ l()                                                     [521-541]
          ├─ engine.f() is true (not init yet) → engine.e(callback InterfaceC0073a) [526-528]
          │    └─ onInitComplete(code) → engine.j(list, listener, useCloud&&engine.g(), extras)  [391-402]
          │         Note: MiEngine branch will be Thread.sleep(f309d) first, f309d defaults to 0 [394-400]
          └─ init → engine.j(list, listener, useCloud&&engine.g(), extras) [539]

engine.j(...) // See §5.4 Four-engine implementation
 └─ Scan file by file → listener(b0.a.c implementation).d(scanType, i+1, total, [y.f]) [n0/g.java:326-362]
      ├─ level ∈ {2,3,6} and i0.f.a().f(path) (file MD5 in t_virus_white_list) → level=1 [335-337]
      ├─ Cache to b0.a.f313h (path→optimal result, compareTo takes more severe) [338-340]
      ├─ level ∉ {2,3,6} → Directly send progress message task.m(i-1, path) [343-346]
      ├─ level ∈ {2,3,6} → i0.d.g() (whitelist) when entering for the first time [348-354]
      ├─ If the file signature MD5 is in mi_virus_white_db → level=1 ("add white from whiteList") [355-360]
      └─ task.m(i-1, path)   → handler.obtainMessage(3, bundle{pathIndex, path})          [602-610]

n0.g$a.handleMessage(3) // Main thread progress aggregation [n0/g.java:115-246]
 ├─ Single engine (f6424c.size()==1): Get the engine result directly → observer.s(...) [190-211]
 └─ Multi-engine: f6415a[path] count++
      ├─ count < number of engines → continue waiting [217-221]
      ├─ Count == number of engines → f313h gets the results one by one, fold g.D() merges [222-230]
      │    └─ D(): Overwrite by virusLevel priority 2 > 3 > 6 > 4 > 5 > 1; extras string accumulation [1014-1087]
      ├─ f6417c Append the merge result, observer.s(offset+idx, total+offset, [merged]) [232-237]
      └─ The path count is cleared; if keySet is empty → sendEmptyMessage(2) [238-240]

n0.g$a.handleMessage(2) //Scan ends [n0/g.java:118-175]
 ├─ Take f6417c → y.f[] and fill in versionName/versionCode (from f6418d cache) simultaneously [120-145]
 ├─ observer.v(taskId, results)                                        [146-151]
 ├─ Clean up 5 tables + cancel main AsyncTask [152-160]
 ├─ **non-international version**: new AsyncTaskC0152a(...).execute → c0.d.g(ctx).i(results) write scan_result_db [161-163]
 └─ Task by task: thread.interrupt() + handler.removeCallbacksAndMessages(null) + I(taskId) [168-174]

GuardProvider$a.v(taskId, results) //State machine end point [GuardProvider.java:56-77]
 ├─ results[0] is stored in b.f1752a
 ├─ n0.h.a(ctx, results[0]) // Send TRACK_ANTIVURS_DATA broadcast to com.miui.securitycenter
 └─ countDownLatch.countDown()

Return to GuardProvider.f():
 └─ Group MatrixCursor{"Type","VirusName","Description","EngineName","AdType","AdName","AdDescription"}
    Value = {f11667l, f11659d, f11660e, f11661f, f11668m, f11663h, f11664i} [257-260]
 └─ Delete cacheDir/copy.apk (if you go through the content:// branch) [261-267]
```

**[Inferred] The true meaning of the scan type parameter `2`**: The fourth parameter of `L(strArr, cVar, z6, i6=2, str)` `i6` In `n0/g` internal **is only used as the progress base (offset)** - `K()` Pass it as `i7` to the `a` (Handler) constructor. The Handler is only used for `observer.s(i8 + f6425d, ...)` (`n0/g.java:199`, `237`). It **does not participate in** scanning mode determination and engine selection. The real mode split occurs at `M()`: `i6 == 1 → C()` (incremental), the rest → `L()` (`n0/g.java:1296-1303`). Therefore, `2` is just a progress offset mark of a "synchronous scan scenario", not the `scan_params` bit field of `p0`.
**[Observed]** `guard/install_scan` (1008) uses the same **function as** `f(path, bool, engineName)`, which is also hard-coded internally `2` (`GuardProvider.java:249`, `271`).
**[Observed]** Even if the native scan is still running after the 2-minute timeout, the Provider returns `null` (`GuardProvider.java:250-256`) - the result is sent to the SecurityCenter through the broadcast bypass; the task itself will not be cancelled.

### 3.3 `guard/engine_info_list`（1005）

```
GuardProvider.query case 1005                             [GuardProvider.java:384-394]
 ├─ MatrixCursor column names: Name, NameString, DescriptionName, DialogTitle, DialogHeader,
 │                     DialogPolicy, DialogCloudScan, DialogSummary, DialogUrl, Enable, SupportCloud
 └─ for bVar in x.a.f() // See §5.5 "Engine Visible List"
      objArr[0] = bVar.f3547b // Internal name "Mi"/"AVL"/"TENCENT"
      System.arraycopy(getStringArray(bVar.f3553h), 0, objArr, 1, 8) // Resource array 8 items
      objArr[9] = g.z().w(bVar.f3547b) ? 1 : 0 // Enabled status in SP
      objArr[10] = bVar.f3551f ? 1 : 0 //Support cloud scanning (compile-time constant true)
```
See §5.6 for the semantic mapping and engine-by-engine values of the 8 strings.

### 3.4 `guard/virus`（1007）

| Operation | behavior | location |
|---|---|---|
| `query` | `SELECT * FROM t_virus_white_list`（`miui_virus.db`） | `GuardProvider.java:401` |
| `insert` | `insert("t_virus_white_list","_id",cv)`; after success, `notifyChange` + `c0.d.g(ctx).c(new String[]{cv.get("dir_path")})` clears the path cache in `scan_result_db` | `357-370` |
| `delete` | `delete("t_virus_white_list", selection, args)`; `notifyChange` + `c0.d.g(ctx).c(args)` when `>0` (use args as a path array) | `338-349` |
| `update` | Constant `return 0` (not implemented) | `419-421` |
| Others URI | `delete`/`insert` are both `return 0`/`null` (first get the writable db and then determine the match) | `340`、`359` |

**[Observed]** This table is both a "virus whitelist" and a "skip table": `i0.f.f(path)` uses `virus_md5 = ?` to check it (`i0/f.java:97`), and a hit reduces the result level to 1 (`n0/g.java:335-337`).

### 3.5 `guard/support_incremental`（1009）

```
case 1009: if (!i0.h.h(ctx)) return null;         // GuardProvider.java:406-408
           MatrixCursor{"support"} + row{1}        // 409-411
i0.h.h(ctx) = ctx.getPackageManager()
                 .getPackageInfo("com.miui.securitycenter",128)
                 .applicationInfo.metaData.getBoolean("miui.support_full_scan")   // i0/h.java:86-92
```
**[Observed]** URI is named `support_incremental`, but the underlying read key is **`miui.support_full_scan`** ("full scan" capability bit), and the semantics are inconsistent with the name. It is consistent with the Lead conclusion and can be cross-confirmed.

### 3.6 `call("parseApk")` / `call("getRiskAppData")`

```
call(method, arg, extras)                                  [GuardProvider.java:330-335]
 ├─ "parseApk"      → d(arg, extras)                        [159-190]
 │    ├─ If there is no READ_EXTERNAL_STORAGE and arg is not under /data → w0.e.a(ctx) (LBE authorization) [163-165]
 │    ├─ w0.d.a(ctx, path, flags) // API>=29 is used to hide PackageParser, otherwise getPackageArchiveInfo [w0/d.java:11-127]
 │    ├─ flags&128 → SecurityCenterHelper.getLabel + icon (Bitmap or drawable→bitmap) [175-187]
 │    └─ Bundle{"pkgInfo", ["label","icon"]}
 └─ "getRiskAppData" → b()                                  [103-127]
      ├─ getInstalledPackages(0)
      ├─ listE = c0.b.f(ctx).e() // risk_gray_result_db full list (and clear expiration first) [c0/b.java:97-121]
      ├─ Package by package: skip system applications ((flags&1)==1); c(pkgInfo, listE) find records with the same package name + the same versionCode [110-111]
      ├─ After hit: if i0.f.a(ctx).e(pkgInfo) (whitelist) → c0.b.f(ctx).b(pkgName) delete the line and discard [117-119]
      │           Otherwise put JSON{packageName, versionCode, type, signaturesMd5} [139-145]
      └─ Return Bundle{"riskData": JSONArray string}
```

### 3.7 `SecurityService` link (summary, see §5.2 for complete transaction list)

```
bindService(action="...antivirusservice") → onBind returns y.a implementation (SecurityService$a) [SecurityService.java:312-314]
bindService(action="...urlscan") → onBind returns y.b implementation (SecurityService$b) [315-317]
Each call first c() = !androidx.core.os.a.a(this) (whether the user is locked) [297-299]
  → Locking will be downgraded uniformly: return -1 / 0 / false / null, or call back v(-1,null), K(-1) to the observer
```

### 3.8 Two bypasses of scan results

1. **writes library**: `n0.g$a.handleMessage(2)` → non-international version → `c0.d.g(ctx).i(a[])` writes `scan_result_db` (`n0/g.java:161-163`, `c0/d.java:243-288`).
2. **broadcasts**: `GuardProvider$a.v()` → `n0.h.a(ctx, f)` → `sendBroadcast(Intent("miui.intent.action.TRACK_ANTIVURS_DATA").setPackage("com.miui.securitycenter"))`, 14 extras, not sent when `level ∈ {1,4,5}` (`n0/h.java:8-30`).
   [Observed]`n0/h.a` is only called in `GuardProvider$a.v()` (`GuardProvider.java:74`); other observers of `o0/a` (such as incremental scan `BinderC0154g`) do not broadcast.

---

## 4. Analysis of key logic one by one

### 4.1 Process initialization and global network switch

| # | location | Observed | Inferred | Unknown |
|---|---|---|---|---|
| 4.1.1 | `GuardApplication.java:38-48` | `e(Context)`: Non-international version = `d.b("persist.sys.sc_allow_conn", false)`; International version = `d0.b.c(ctx,"com.miui.securitycenter")` (Privacy.isEnabled) && this attribute; `ro.miui.customized.region=="jp_kd"` and then && `Settings.Secure.getInt(cr,"antivirus_engine_network_enable",0)==1` | This is the only network gate **in the entire process of**, all read through `d0/d.java` reflection `android.os.SystemProperties.getBoolean` | Writer of `persist.sys.sc_allow_conn` (should be SecurityCenter/System) |
| 4.1.2 | Whole tree grep `GuardApplication.e(` | **has exactly 5 call points**: `f0/a.java:66` (`f0.a.b()`, engine base class "whether networking is allowed"), `h0/b.java:84`, `h0/b.java:153` (two HTTP methods **prefix return null**), `n0/g.java:642` (whitelist refresh before virus database update), `z/b.java:41` (`OneTrack.setAccessNetworkEnable`) | Functions that are completely shut down when the network is disconnected: ① `b()` of all engines (AVL `setNetworkEnabled`, Tencent `setAutoConnectionSwitch`, MiEngineNG `delegate.m()`) → all cloud scans/cloud libraries are invalid; ② `api.sec.miui.com` virus database + whitelist synchronization; ③ `flash.sec.miui.com/detect/app` Anti-fraud; ④ OneTrack reporting; ⑤ Whitelist **is not refreshed before** is scanned (`n0/g.java:642` and `i0.d.d().j(true)` are skipped) | Whether the cloud `virus-engine.sec.miui.com` (new Retrofit stack) is also controlled by this gate → see 4.1.3 |
| 4.1.3 | `q0/q.java` (signature interceptor), `s0/s.java:124` | The base URL of the new Retrofit stack is fixed by `q0.m.f7703a`. **does not check `GuardApplication.e()` during the** build period; but `m(boolean)` of `p0/n.java:828` writes `GuardApplication.e()` into static `AtomicBoolean f7476g` for `p0` Each Scanner reads | [Inferred] The gate of the new stack is inside the Scanner (`p0/f.java`, `f7380d`, etc.), not at the OkHttp layer | The specific judgment points in `p0` have not been confirmed line by line (belonging to the scope of t-cloud) |
| 4.1.4 | `f0/b.java:56-60` | `b()`: `d0.b.b(getContentResolver(),"VirusScan","miEngineNgEnable","true")` determines whether the `MI` constant points to `MiEngineNG` or `MiEngine` | This is the only switch for **Cloud Control (SettingsCloudData) to determine the engine implementation class**. The default is `"true"` → the default MiEngineNG. | Cloud control issuer (MIUI cloud control platform) |
| 4.1.5 | `d0/b.java:14-27` | `MiuiSettings$SettingsCloudData.getCloudDataInt/String` reflective; `d0/c.java:7-11` universal `getDeclaredMethod + setAccessible(true) + invoke(null,...)` | All remote configuration readings are done through reflection, without `Settings.Global` or SecurityCenter provider direct reading. | When reflection fails, `b()` returns `""` (`d0/b.java:26`), and `a()` returns `0` (`d0/b.java:23`). |
| 4.1.6 | `d0/a.java:7-12` | `android.miui.AppOpsUtils.isXOptMode` reflection | Used for `w0/e.java:9` to determine whether to skip LBE authorization | — |
| 4.1.7 | whole tree | **None** `DexClassLoader` / `InMemoryDexClassLoader` / `PathClassLoader` Self-built | No plug-ins/hot fixes | — |
| 4.1.8 | `w0/h.java:11`、`i0/b.java:10`、`i0/e.java:12`、`c0/a.java:12`、`c0/c.java:12`、`r0/x.java:22` | 6+1 libraries: `miui_virus.db`(v5), `miui_antivirus.db`(v2), `mi_engine_white_virus.db`(v1), `risk_gray_result.db`(v1), `scan_result.db`(v2), `flamingo.db`(Room v2) | All lazy creation: triggered by `SQLiteOpenHelper`/Room for the first time `getWritableDatabase()` | — |

### 4.2 Boundary behavior of IPC facade

| # | location | Observed | Inferred |
|---|---|---|---|
| 4.2.1 | `GuardProvider.java:396-398` | `selectionArgs` of `sync_scan` is not exactly 2 → directly `return null` (no exception is thrown) | The caller must strictly give 2 parameters; if `install_scan` is less than 3, **will degrade** to `query("t_virus_white_list")` (`401` / `415`), returning the whitelist cursor instead of the scan result - **dangerous silent degradation** |
| 4.2.2 | `GuardProvider.java:383-415` | `case 1008: break;` in `switch` falls to `(strArr2.length < 3) ? cursorQuery : f(...)` at the end of the method | When 1008 passes 2 parameters, `cursorQuery` is `null` (value is assigned only after 1007) → return null |
| 4.2.3 | `GuardProvider.java:210-216` | `content://` input will be copied by **to `cacheDir/copy.apk`** (fixed file name) | Two concurrent `content://` scans will cover each other's same path → race condition; and must be deleted before exiting (`220-221`, `252-254`, `261-267`) |
| 4.2.4 | `GuardProvider.java:247-256` | Create a new `b bVar` (result container) for each scan; after returning `countDownLatch.await(2, MINUTES)`, if `f1752a==null`, null will be returned | Timeout/failure appears to the caller as "Cursor is null" and no error code |
| 4.2.5 | `GuardProvider.java:111-114` | `b()` (getRiskAppData) calls `d.g(this.f1748b)` (whitelist network request) when it first detects gray production. **only does** (`z6` flag) once in the `for` loop. | This network call is executed synchronously in the Binder thread → may slow down `getRiskAppData` |
| 4.2.6 | `SecurityService.java:297-299` | Each AIDL method starts with `c()` (= if the user has not unlocked it, it is regarded as locked). When locked, it will be downgraded uniformly. | See §5.2 for the degradation matrix |
| 4.2.7 | `SecurityService.java:76-89` | `E(str,int)`: `i6∈{2,3}` → Asynchronous thread pool execution `d.g(ctx).b(str)` + `c0.b.f(ctx).b(str)` (clear two result libraries according to package name); `i6==1` → `i0.d.d()` incrementally update the virus database | This is the "Uninstall/Change Notification" entry: type 2/3 = The package is uninstalled/updated and the cache needs to be cleared, 1 = Triggers incremental virus database update |
| 4.2.8 | `SecurityService.java:169-172` | `getVersion()` always returns the string `"1"` | The protocol version is hardcoded and has nothing to do with `versionName 2.0.3` |
| 4.2.9 | `SecurityService.java:1415` (actually `n0/g.java:s()`) | `g.z().s()` Heng `return -1` | `checkUpdate` (transaction 6) is actually **empty implementation**, and always returns -1 |

### 4.3 Scanning arrangement (`n0.g`)

| # | location | Observed | Inferred |
|---|---|---|---|
| 4.3.1 | `n0/g.java:1228-1237` | `z()` double check single case; `private g(){ x.a.l(); }` (`983-985`) | The first time `g.z()` will execute `x.a.l()` → engine activation initial value + may trigger cloud control pull (§4.5) |
| 4.3.2 | `n0/g.java:41-66` | Static `f6413i` (`HashMap<int, c>` task table), `f6412h` (task count); 6 instances of `ConcurrentHashMap` all with `y.c` (observer Binder) as key | An observer can only run one batch at the same time (multiple batches will cover each other `f6416b` and other tables) |
| 4.3.3 | `n0/g.java:1260-1294` | Before task creation: `filter(nonNull) → filter(File::exists)`; `list.isEmpty() \|\| f6412h >= 10` → `observer.v(-1,null)` + `return -1` | **has a maximum of 10 concurrent scanning tasks**; non-existent paths are silently eliminated; `install_scan` is also limited |
| 4.3.4 | `n0/g.java:1150-1170` | `p(c)` assigns taskId: Find the empty position starting from `f6412h`, `>100` wraps to 0 | taskId space 0..100, reused after recycling; `cancelScan(id)` depends on it |
| 4.3.5 | `n0/g.java:1132-1148` | The engine collection comes from `x.a.c()` (SP enabled item), and the engine-by-engine reflection construction instance (`n0/i.java:10-15`) and **each have one Thread** | **concurrency model = engine-level parallelism, file-level serial**: `for (String path : list) { Thread.sleep(5); scan(path); }` inside each engine (see `AvlEngine.java:149-161`, `TencentEngine.java:460-473`, `g0/e.java:86-127`) |
| 4.3.6 | `n0/g.java:184-245` | Progress aggregation: `f6415a[observer]` is `path → number of completed engines`; merge when equal to the number of engines | Multi-engine result **merges** according to path, not according to "engine voting"; `D()` is covered with `virusLevel` priority 2>3>6>4>5>1, `extras` (`f11671p`) string **is spliced and accumulated to** |
| 4.3.7 | `n0/g.java:1014-1087` | Six cases of `D(a,b)`: when base=1, it accepts 2/3/4/5/6; when base=2, **only accumulates extras**; base=3, only accepts 2; base=4, accepts 2/3/6; base=5, accepts 2/3/6/4; base=6, only accepts 2/3 | "The more serious one wins", and level 2 (virus) > 3 (risk) > 6 (gray production/anti-fraud) > 4 > 5 > 1 (safety) |
| 4.3.8 | `n0/g.java:987-995`、`1117-1129` | `A(id)` Find tasks by id; `I(id)` removes and `f6412h--` | Must be called in pairs when canceling/ending, otherwise count leaks result in permanent `>=10` rejection of new tasks |
| 4.3.9 | `n0/g.java:1372-1405` | `q(id)`: First press `f6416b` to check the observer → for each subtask `i()` (set `f307b=false`, `f310e=0`, `thread.interrupt()`, `engine.a()`, `I(id)`, `handler.removeCallbacksAndMessages(null)`); then `cancel(true)` master AsyncTask | [Observed] Only cancel `f6421g` (the last main AsyncTask created by `L()`), the previous ones will not be canceled in multiple batches. |
| 4.3.10 | `n0/g.java:1407-1413` | `r(id)` (cancel update): `A(id).j()` → `k()` for each engine | `k()` is implemented differently by each engine: AVL=`stopUpdate()`, Tencent=`UpdateManager.cancel()`, MiEngine=empty, MiEngineNG=`delegate.g()` |
| 4.3.11 | `n0/g.java:553-576` | `binderDied()`: Scanning → for all subtasks `i()`; Updating → `j()`; `cancel` Main AsyncTask; `unlinkToDeath` | The engine can be released correctly when the calling process dies; [Observed] `f6416b.get(...)` directly `.iterator()` is not determined to be null. If the timing is abnormal, it may be NPE |
| 4.3.12 | `n0/g.java:128-145` | In the end callback, take `d{versionName,versionCode,sigMd5}` one by one from `f6418d` and backfill `y.f` | The engine itself does not guarantee that the version information will be filled in (AVL/Tencent will fill it in, MiEngineNG will fill it in), and the cache is used to provide details. |
| 4.3.13 | `n0/g.java:1428-1440` | `x()` creates `new String[4]` but only fill in 3 → return `["Mi","AVL","TENCENT",null]` | **real bug**: The last bit of the array returned by AIDL transaction 11 (`i()`) is always null |
| 4.3.14 | `n0/g.java:1243-1250` / `1419-1426` | `F(name)` returns the compile-time constant `f3551f` (all three engines are `true`); `w(name)` returns the SP actual value | "Whether cloud scanning is supported" = constant truth value independent of the engine |
| 4.3.15 | `n0/g.java:1252-1258` | `J(name, enable)`: `(z6 \|\| name != "Mi")` only writes SP | **MI engine cannot be shut down** (even if false is passed, it will not be written) |
| 4.3.16 | `n0/g.java:1330-1370` | `O(list)` filters the whitelist by package name + `sourceDir`; `P(list)` filters by apk path | For use by AIDL transaction 23/21 (`M`/`f`), used to "remove known security items before scanning" |
| 4.3.17 | `n0/g.java:1195-1226` | `v(ctx)`: `MediaStore.Files` query `_data LIKE '%.apk'` → `i0.h.f(ctx, path)` get PackageInfo | Incremental scan will **traverse all apk** on external storage |
| 4.3.18 | `n0/g.java:664-754` | Incremental scan (AsyncTask `e`): `getInstalledPackages(64)` + `v(ctx)`; there is a cache hit and level∈{2,3,6} → play back the cache directly (`m()`); otherwise, batch the "no cache" path to `L(..., 1, null)` and perform a normal scan | Increment = Scan only the "No Cache" item; see §5.5 `virus_cache_validty_perod` for the cache validity period |

### 4.4 Four engine adapters

Unified contract `f0.a` (`f0/a.java`): `a()` cancellation, `e(InterfaceC0073a)` asynchronous initialization, `f()` whether to be initialized, `g()` capability bit, `h()` release, `j(List, c, boolean, String)` scan, `k()` Cancel update, `l(b)` update, `m(int,String)` URL class operation, `n(y.d, d, Object)` wireless class operation, `b()` = `GuardApplication.e(c())`, `d()` = engine internal name.

| Ability | `MiEngine` | `MiEngineNG` | `AvlEngine` | `TencentEngine` |
|---|---|---|---|---|
| Implement stack | `i0.a`(=`i0.d`) + `g0.e` | Kotlin → `p0.n` → `s4`/`r0`/Rust | `com.avl.engine.AVLEngine` | TMSDK `QScannerManagerV2` |
| `e()` initialization | `f1759d.a(ctx)`, `f1758f=true`; callback `b(0)`/`a(-1)` | **constant `b(0)`** (no initialization, `MiEngineNG.java:227-231`) | `AVLEngine.init(ctx, AVLSdkConfig)`; return `0` to be considered successful (`AvlEngine.java:99-119`) | `TMSDKContext.init(...)`; `f1767f` is successful only if it is true (`TencentEngine.java:403-415`) |
| `f()` to be initialized | `!f1758f` | **heng `false`** | `!f1757d` | `!f1767f` |
| `g()` capability bit | `true` | `true` | `true` | `true` |
| Cloud scanning support | **does not take effect**: `j()` ignores `z6` | Effective: `z6` participates in the calculation of `scan_params` | Effective: `r(z6)`=`setCloudScanEnabled` | Valid: `(b() && z6) ? 6 : 2` as scanType of `scanUninstallApks` |
| Increment/file granularity | `g0.e.d()` by apk | The whole is handed over to `delegate.i(Collection<Path>, …)`, internal concurrency | By apk, `Thread.sleep(5)` | per apk, `Thread.sleep(5)`, once per file `scanUninstallApks` |
| Advertisement detection | None | None (`f11668m/f11663h/f11664i` is never assigned) | None | None |
| Cancel | `this.f3536c=true`, `g0.e` In-cycle inspection | `delegate.f()` = `q1.e(job,"External cancellation")` | `this.f3536c=true`, in-cycle check | `f3536c=true` + `QScannerManagerV2.cancelScan()` |
| update | `l(b)`: No implementation (`this.f1760e` constant false → callback `a(2,1)`) | `n.o(delegate, null, consumer, 1, null)`, `IntConsumer` mapping -1→`a(2,-1)`, 0→`a(0,0)`, 1→`a(3,1)` | `checkUpdate()`==0 → `a(3,0)`; otherwise `update()`: >0→`a(0,n)`, ==0→`a(3,0)`, <0→`a(2,n)` | `UpdateManager.check(UPDATE_FLAG_VIRUS_BASE,…)`; no update → `a(3,err)`, failure → `a(2,err)` (err initial value `-2146769239`) |
| Release `h()` | empty | empty | empty | `freeScanner()` post null |
| URL class `m(int,String)` | **does not override** → base class `return -1` | Not override → `-1` | `i6==1`→`AVLEngine.scanURL(str)`; `i6==2`→`updateURLRule()`, when successful (1), write `getURLDatabaseVersion()` to SP `avl_url_sdk_version` | Not override → `-1` |
| Wireless `n(...)` | Not override → `0` | Not override → `0` | Not override → `0` | WifiDetect: `NETWORKSTATE`/`DNSANDPHISHING`/`ARP`/`SECURITY` four-way (`TencentEngine.java:82-129`) |
| Result construction | `g0/e.c()`: `level = b(type)`, `extras="Mi:safe/danger;"`; plus anti-fraud `"AntiDefraud:danger;"` (`g0/e.java:21-38`) | `x(aVar)`: Default `level=1, extras="MiEngineNG:safe;"`; each threat of `t0.o` → `level = name.contains("Malware") ? 2 : 3`, `extras="MiEngineNG:danger;"` | `o(info,path)`: `p(info)` maps dangerLevel 1→2, 2→3, others→1; `virusName` starts with `risk/` → 6; `extras="AVL:safe/danger;"` | `u(entity)`: `t(scanResult)` mapping 258→5, 259/261/262→3, 260→2, others→1; `extras="TENCENT:safe/danger;"` |
| error code | init failed `a(-1)`; no other error code | Same as above | `AVLEngine.init` returns non-0 → `a(-1)`; `scanURL`/`updateURLRule` is transparently transmitted as is (`updateURLRule` returns non-1 directly) | `TMSDKContext.init` false → `a(-1)`; update with `-2146769239` as "no error" sentinel |

**[Observed]** `virusType` (`y.f.f11666k`) **There are only two assignment points** in the whole tree: `AvlEngine.java:41`, `57` and `TencentEngine.java:377`, all written as `1`. `MiEngine`/`MiEngineNG` are never assigned a value (default 0). → See §4.7 "Threat Type Enumeration".
**[Observed]** `adLevel/adName/adDescription` (`f11668m`/`f11663h`/`f11664i`) **assigns** at 0 in the whole tree, and is only read by `GuardProvider` (`GuardProvider.java:260`) and `n0/h.java:25-26` as broadcast extra. → Advertisement detection is a reserved bit of the **protocol, and** is not implemented.

### 4.5 Engine activation status and cloud engine selection (read and write full table)

| steps | location | Observed |
|---|---|---|
| Read enable collection | `x/a.java:38-49` `c()` | Traverse `f0.b.values()`, add `w0.f.a().c(f3550e)` if it is true |
| read single enable | `x/a.java` → `n0/g.java:1419-1426` `w(name)` | `w0.f.a().c(bVar.f3550e)`, **default false** (`w0/f.java:57` `getBoolean(str,false)`) |
| write single enable | `n0/g.java:1252-1258` `J(name,enable)` | Write only `w0.f.a().i(f3550e, value)`; `Mi` is excluded |
| First initialization | `x/a.java:143-165` `l()` | If `antivirus_is_init` is not set: `Mi=true`, the remaining engines **selected by `e()` randomly select 1** (`j()`→`Random().nextInt(size)`), and finally set `antivirus_is_init=true` |
| `mix.scan` branch (not international/domestic) | `x/a.java:144`、`166-193` `n()` | `l()` starts with `if (h0.a.a()) { n(); return; }`; `h0.a.a()` = reads metaData `securitycenter.support.mix.scan` of **SecurityCenter** (this may be true only in non-international versions). True → `n()`: Set all the engines **selected by `e()` to true** during the first initialization; then press `cloud_engine_list` for precise synchronization (`i(list)`); False → `l()`: Only 1 is randomly selected. |
| daily correction | `x/a.java:167-171`、`h()` | `h()`: `now - load_cloud_engine_time > 86400000` (24h) → `x.b.g(ctx)` pull cloud control + `m(now)` write timestamp; when the clock is dialed back (the difference is negative), the timestamp is directly rewritten and returns true |
| Cloud control pull engine list | `x/b.java:114-129` `h(ctx)` | `d0.b.b(cr,"VirusScan", IS_INTL?"global":"mainland", null)` takes JSON; parses `[{name,scale}]`; `d(items)` filter: the name must be one of the three in `f0.b`, and `max(1,scale)==min(scale,100)` (that is, an integer from 1..100) |
| Cloud selection | `x/b.java:153-176` `j(list,ctx)` | Randomly select an engine name according to `scale` weight (`Random().nextInt(sum+1)`) and write it into `cloud_engine`; write the entire table into `cloud_engine_list` (JSON array) |
| `mix.scan` special case | `x/b.java:167-175` | When `h0.a.a()` is true, **does not write** `cloud_engine` (only writes `cloud_engine_list`) |
| Back solution cloud_engine | `x/a.java:9-24` `a()` | Read `cloud_engine`, and set the switches of all engines of **except Mi** to `name == cloud_engine` |
| Cloud engine knows everything | `x/a.java:26-36` `b()` | Traverse the enabled items and return the engine name of the last **"non-Mi and enabled" of**; if not, `""` |
| consistency correction | `x/a.java:176-192` | `c().size()>2` or `g().contains(b())` is false → `k()` re-randomizes; otherwise if the cloud control list does not contain the current engine → `a()` reverse solution |
| Visible engine list | `x/a.java:60-85` `f()` | `cloud_engine_list` is empty → return all **except Mi and** (AVL, TENCENT); otherwise return the intersection of list and enumeration |
| Cache validity period | `x/b.java:133-137` | `virus_cache_validty_perod` ← Cloud control `VirusScan/virusCacheValidityPeriod`, default `3` (days); used by `c0/b.java:69` and `c0/d.java:97` to clean up expired cache |

**[Observed] Default value synthesis (fresh installation + cloud control unreachable + `securitycenter.support.mix.scan` undeclared)**: `x.a.l()` first branch → `Mi=true`, and for `e()` (= AVL, TENCENT Two items) Do `Random().nextInt(2)` Select **Set exactly 1** to true, another to false, and finally `antivirus_is_init=true`. That is, **2 engines** (Mi + AVL or Mi + TENCENT) are enabled by default. If SecurityCenter declares `securitycenter.support.mix.scan` → go to `n()`, the first branch sets all two items **of `e()` to** to true, that is, by default, all 3 engines of **are fully enabled**.
`cloud_engine` / `cloud_engine_list` / `load_cloud_engine_time` does not exist initially → `f("cloud_engine")` returns `null` (`w0/f.java:60-66` returns null when catching an exception), `e("load_cloud_engine_time")` returns `0`.
**[Observed]** `x.a.e()` (`x/a.java:87-96`) are two equivalents of `if (i6==1) add; else if (i6==2) add;` → actually return "all engines of `f3554i ∈ {1,2}`", that is, **AVL(2) and TENCENT(2), excluded MI(3)**.
**[Inferred]** Therefore, "randomly select a cloud engine for the first time" will only choose between AVL/TENCENT; MI is always on. This is completely consistent with `J()` not allowing MI to be closed and `e()` excluding MI.

### 4.6 Three local judgment libraries and gray production judgment of `getRiskAppData`

| library file | Helper | table | key column | Writer | reader |
|---|---|---|---|---|---|
| `miui_antivirus.db` v2 | `i0/b.java` | `antivirus_db_version(version, release_time)`；`antivirus_db(package, version, signatures, md5, type, virus_name, description, strategy, descType)` | `type`、`strategy`、`md5` | `i0/d.java:102-185` (issued by the server `/antivirus/viruses`) | `i0/l.java:308` (`package=?` comparison one by one) |
| `mi_engine_white_virus.db` v1 | `i0/e.java` | `mi_virus_white_db(package, signatures, path)`；`mi_detect_app_db(package, signatures)` | `package`+`signatures` | `i0/f.java:29-72` (server `/antivirus/whitelist`) | `i0/f.java:77-118` |
| `risk_gray_result.db` v1 | `c0/a.java` | `risk_gray_result_db(packageName, versionCode, signatures, type, time)` | Full table | `c0/b.java:113-152` (`g(m)`), `MiEngineNG.java:120-127` (`s()` write type=5) | `c0/b.java:97-121` (`e()` full table + cleared and expired) |
| `scan_result.db` v2 | `c0/c.java` | `scan_result_db(path, packageName, virusName, virusDescription, engineName, versionName, versionCode, virusLevel, extras, sign, time)` | Full table | `c0/d.java:205-288`（`h()`/`i()`） | `c0/d.java:179-203`（`f()`） |
| `miui_virus.db` v5 | `w0/h.java` | `t_virus_white_list(..., virus_md5, dir_path, pkg_name, ...)`；`virus_white_db(...)` | `virus_md5`、`dir_path` | `GuardProvider.insert/delete` | `GuardProvider.query`、`i0/f.java:88-111` |

The complete judgment chain of **`getRiskAppData` (`GuardProvider.java:103-127` + `129-157`)**

```
c0.b.f(ctx).e()      // c0/b.java:97
  └─ a() Clear expiration first: DELETE FROM risk_gray_result_db WHERE time < now - (virus_cache_validty_perod days)*86400000
  └─ SELECT * FROM risk_gray_result_db → List<i0.m>
for pkg in getInstalledPackages(0):
   if (pkg.applicationInfo.flags & 1) == 1: continue // Skip system applications
   rec = first(r in list where r.f5034c==pkg.packageName && r.f5033b==pkg.versionCode)
   if rec == null: continue
   if (i0.f.a(ctx).e(pkg)): // Whitelist exemption
        c0.b.f(ctx).b(pkg.packageName) // Delete this line from risk_gray_result_db
        continue
   out.add({packageName, versionCode, type, signaturesMd5})
```

**"What conditions count as gray production"**
- **[Observed]** The `type` column of `risk_gray_result_db` is assigned at two writing points: `MiEngineNG.s()` always writes `5` (`MiEngineNG.java:121-126`); `i0/l.java:348-352` is also written when "`type==5` is in the virus database and the signature matches" `5`, and then change the `f5032a` recorded in the memory back to `0`.
- **[Observed]** The branch semantics of `i0/l.java:341-356`: `type==0` → log "unknown or safe apk"; `type==5` → log "update riskgraydb" and dropped.
- **[Inferred]** Therefore: **`type==5` is the "gray production" mark**, which comes from ① server `antivirus_db.type==5` (cloud gray production library) ② MiEngineNG's black and gray production scanning pipeline (when `scan_params` bit2 is turned on).
- **[Observed]** The value semantics of `antivirus_db.type` (inferred from `g0/e.java:40-48`): `0/1 → level 1 (safe)`, `2 → level 3`, `3 → level 2`. That is, `type` is a different set of codes from `virusLevel`.

**`i0.f.a(ctx).e(pkginfo)` How to implement whitelist determination** (`i0/f.java:77-118`)

```
e(PackageInfo info):
  └─ sigs = i0.g.a(new PackageParser.Package(info.packageName), info) // divide API 29/28/<28 three-way signature acquisition
  └─ return d(info.packageName, i0.h.c(sigs))
d(pkg, sigMd5):
  └─ if (b(pkg, sigMd5)) return true; // b() directly returns false - reserved hook, never effective
  └─ SELECT package, signatures FROM mi_virus_white_db
        WHERE package = ? AND signatures = ?
     return cursor.getCount() > 0;
```
**[Observed]** `i0/f.java:37-39`'s `private boolean b(String,String) { return false; }` is **'s constant false dead hook** (probably the built-in whitelist that was cut off).
**[Observed]** `i0/f.java:88-111`'s `f(path)` is another whitelist path: `w0.a.a(path)` (full file MD5) → `query("content://guard/virus", null, "virus_md5 = ?")` **calls back its own Provider**.
**[Inferred]** Therefore, the whitelist has two dimensions: `(package name, signature MD5)` goes to the local table; `file MD5` goes to `t_virus_white_list` of its own Provider.

### 4.7 Is the QAX (Qi’anxin) engine implemented?

| Evidence | Conclusion |
|---|---|
| `res/values/arrays.xml:23-32` `qax_string_info` has 8 complete items; `res/values/public.xml` has 6 items `QAX=0x7f0b0006`, `DesQAX=0x7f0b0003` and `qax_dialog_*` | Resource exists[Observed] |
| `res/values/strings.xml:33-38` 6 `qax_dialog_*` values are all literals `TODO` | Copywriting placeholder[Observed] |
| `f0.b` enumeration **has only 3 constants** (REA `ev_80d5f9…`: field `f3543j`/`f3544k`/`f3545l` + array `f3546m`) | None QAX constant[Observed] |
| Whole-tree grep `QAX|qax` only hits `com/tencent/tmsecure/R.java` (the R alias table that comes with TMSDK, 12 lines) and the above resources | **dex None QAX Engine class**[Observed] |
| None `com/miui/guardprovider/engine/qax/` catalog | [Observed] |
| **Conclusion** | **QAX engine does not implement**: only residual resources and R constants of TMSDK. `qax_string_info` does not have any Java code references (only `f3553h` in `f0.b` points to `0x7f030000`/`0x7f030001`/`0x7f030003` respectively, skipping `0x7f030002`) [Observation + Inference] |

### 4.8 Threat type enumeration → `virusType` value mapping

| item | Observed |
|---|---|
| Enumerate resources | `res/values/strings.xml:53-64`: `threat_type_adware_desc`, `backdoor`, `default` (0x7f0b0034), `exploit`, `hacktool`, `monitoring`, `rootkit`, `spyware`, `trackware`, `trojan`, `virus` , `worm` —— Total **12** |
| Java reference | There are only 12 int constants of `com/tencent/tmsecure/R.java:725-758`; **business code 0 refers to** |
| `virusType` assignment | Only `AvlEngine`(41,57) and `TencentEngine`(377) write `1`; `MiEngine`/`MiEngineNG`/`g0.e` do not write (default 0) |
| Conclusion | **12 threat types do not have corresponding numerical enumeration**. `y.f.virusType` is actually a binary value: `1` = "Detected (AVL/Tencent)", `0` = "MI engine/not assigned". Real threat classification relies on the strings `virusName` (such as `risk/xxx` for AVL) and `virusLevel`. Broadcasting with `MatrixCursor` just passes it through. |
| Residual cause inference | This set of `threat_type_*` copywriting should be used for display by SecurityCenter and mapped by the upper layer according to `virusName`; the GuardProvider side only does transparent transmission. |

### 4.9 native bridge (`natives`) and unique call point

| item | Observed |
|---|---|
| Load | `RustSupport.<clinit>` → `System.loadLibrary("flamingo")`（`RustSupport.java:4-6`） |
| native method (REA `ev_0c030a…`) | `static native int matches(byte[], String)`、`static native int search(byte[], String)`、`static native boolean[] search(byte[], String[])`（overload_index 0/1） |
| library file | `lib/arm64-v8a/libflamingo.so`, 1 506 088 bytes (11 more .so, total 6.50 MB) |
| unique call point | `p0/y.java:111` (`matches`), `p0/y.java:117` (`search(byte[],String)`); `search(byte[],String[])` **has no call point** |
| Semantics | `y.e(e.a file, StringItem item)`: `Files.size(path) > 20971520` (20 MB) → skip and return false; otherwise, get the file bytes from `LruCache` → call native → `-1` as a mismatch |
| Abnormal | `catch (RustException)` beats `Log.e("CodeScanner","bad input characteristic:")`; `ClosedFileSystemException` → throws `CancellationException("External cancellation")` |
| [Unknown] | `libflamingo.so` internal implementation (Rust side signature engine) is not disassembled |

### 4.10 List of high-risk behaviors (item-by-item positioning)

| # | Type | location | Observed | Risk determination |
|---|---|---|---|---|
| R1 | Dynamic native loading (**writable path**) | `tmsdkobf/ka.java:12-74` | Give priority to the `.../lib/` directory `System.load(absolute path + name)` derived from `TMSDKContext.getStrFromEnvMap(PRE_LIB_PATH)` or `cacheDir`. If it fails, use `System.loadLibrary`; try `name`/`name.so`/`libname.so`. | [Inferred] If `/data/data/<pkg>/lib/` is writable (with the same signature application or root), it is the so hijacking surface. It is internal to TMSDK and is not a direct call point of this application. |
| R2 | Dynamic native loading (fixed name) | `com/avl/engine/a/c.java:223`、`com/avl/engine/a/d.java:107` | `System.loadLibrary("avlasys")` / `System.loadLibrary("urldetectorsys")` (parameters are constants) | low (const library name) |
| R3 | command execution | `tmsdkobf/t9.java:84-95` | `Runtime.getRuntime().exec("ping -i 0.2 -c<n> -w <t> " + host)`, host from `k.a().c(str, observer, str2)` → `detectARPNoRoot` link (`n0/k.java:80-86`) | [Inferred] No shell metacharacter filtering → If `str2` (IP/domain name) can be controlled by the IPC caller, it forms a command injection surface. IPC is protected by signature permissions and actually requires SecurityCenter to reach it. |
| R4 | Process execution (read-only information) | `xcrash/j.java:293`、`com/xiaomi/onetrack/util/b.java:369` | `new ProcessBuilder(new String[0]).command(arrayList).start()` Read system command output | Low (reads class logcat/getprop) |
| R5 | reflection | `d0/c.java:7-11` | General `getDeclaredMethod + setAccessible(true) + invoke(null, ...)`; reused by `d0/b` (MiuiSettings), `d0/d` (SystemProperties), `d0/a` (AppOpsUtils) | Medium (reflection bypasses hidden-API restriction, reads cloud control and system properties) |
| R6 | reflection | `v/a.java:22-38` | `Class.forName("com.android.id.impl.IdProviderImpl")` + `getUDID/getOAID/getVAID/getAAID` | Medium (device identification collection) |
| R7 | reflection | `c5/a.java:114-121` | `Class.forName("miui.os.Build").getDeclaredField("IS_MIUI_LITE_VERSION")` | low |
| R8 | reflection | `n0/i.java:10-15` | `bVar.f3549d.getConstructor(f0.b.class).newInstance(bVar)` reflection construction engine | Low (the Class field that comes with the enumeration) |
| R9 | Hide API | `w0/d.java:28-38`、`i0/g.java:11-50`、`n0/g.java:821` | `android.content.pm.PackageParser` (hidden class)/`PackageParser.PackageLite`/`mSigningDetails` | Medium (depends on platform private API, platform will be broken when upgraded) |
| R10 | Cross-application authorization | `w0/e.java:8-17` | `contentResolver.call(Uri.parse("content://com.lbe.security.miui.su"), "GrantGuardProvider", null, null)` | Medium (request authorization from LBE security component, bypassing storage permissions) |
| R11 | Read across application private libraries | `tmsdkobf/nb.java:36,74,126` | `SQLiteDatabase.openDatabase("/data/data/com.qiyi.video/databases/qyvideo.db", …)`、`com.sohu.sohuvideo/files/databases/sohutv.db`、`.../other.db` | **high** (read the absolute path of the third-party application private directory). It belongs to TMSDK offline video module. Is this application reachable [Unknown](caller not seen) |
| R12 | Full disk file enumeration | `n0/g.java:1195-1226` | `MediaStore.Files` Check `_data LIKE '%.apk'` Full quantity + One by one `getPackageArchiveInfo` | Medium (read all apk metadata from external storage) |
| R13 | Any file MD5 | `w0/a.java:11-30`、`i0/f.java:88` | `FileInputStream` Read any path and calculate MD5, then check `virus_md5` | Medium (with `parseApk` detectable path existence/content hashing) |
| R14 | Write/delete any path | `GuardProvider.java:213-267` | `cacheDir/copy.apk` of `Files.copy(REPLACE_EXISTING)` + `Files.deleteIfExists` | Low (fixed filename, limited cacheDir) |
| R15 | Cross-application broadcast (information leakage) | `n0/h.java:8-30` | `TRACK_ANTIVURS_DATA` carries **15** extra (`path`, `packageName`, `virusName`, `virusDescription`, `engineName`, `versionName`, `adName`, `adDescription`, `versionCode`, `virusType`, `virusLevel`, `adLevel`, `isAuthoritative`, `isInCache`, `extras`) issued `com.miui.securitycenter` | Medium (`setPackage` is a single recipient, but **does not have receiverPermission**; it is actually only received by packages with the same signature) |
| R16 | Device fingerprint | `h0/b.java:31-34`、`128-146` | Static reading `ro.product.device`, `ro.carrier.name`, `ro.product.mod_device`, `MIUI-<INCREMENTAL>`; `mi` = `MD5(android_id + "-" + appKey)` | Medium (`android_id` + device model reported to `api.sec.miui.com`) |
| R17 | IMEI/IMSI (**indirectly triggers**) | `com/xiaomi/onetrack/Configuration.java:86-89`、`269-292`；`com/xiaomi/onetrack/util/DeviceUtil.java:305-356,668` | `Configuration.Builder`'s `f1855h`(IMSI)/`f1856i`(IMEI) **defaults to true** (`86`, `89` rows), `Configuration` is constructed by builder Value (`284-285`); `DeviceUtil` uses reflection `getImei(int)`, `getDeviceId()`, `getSubscriberId()` | **high**: When `z/b.java:25` builds `Configuration`, **is not called** `setImeiEnable(false)`/`setImsiEnable(false)` → IMEI/IMSI is allowed to be collected by default. `z.a.c()` (`n0/g.java:1147`, required for every scan) initializes OneTrack for the first time |
| R18 | Standalone SharedPreferences (non-GuardProvider names) | `com/avl/engine/c/f.java:14,35`、`tmsdkobf/q5.java:20`、`tmsdkobf/m7.java:20` | AVL uses `"avl_conf"`/`"avl_<x>"`; TMSDK self-built SP | low |
| R19 | `serialVersionUID`/`SharedPreferences` plain text UUID | `w0/b.java:19-25` | `key_uuid` randomly drops SP for the first time as UUID as the reported identity | Medium (persistent device identity) |
| R20 | None `SystemProperties.set` | whole tree | Only reading, no writing | — |

---

## 5. Configuration items/constants/protocol fields

### 5.1 `n0.g` (AntivirusManager, `g.z()`) complete method table

File: `n0/g.java` (line 1450). Fields: static `public static int f6412h` (number of active tasks), `public static final HashMap f6413i` (taskId→task), `private static volatile g f6414j` (single case); 6 instances `ConcurrentHashMap`: `f6415a` (observer→path→number of completed engines), `f6416b` (observer→task list), `f6417c` (observer→result list), `f6418d` (observer→path→signature cache), `f6419e` (observer→gray mark), `f6420f` (observer→scanType), `f6421g` (last AsyncTask of `L()`).

**public method**

| OK | signature | Semantics |
|---|---|---|
| 1228 | `static g z()` | Singleton; first construction execution `x.a.l()` (engine enabled initialization + possible cloud control pull) |
| 1239 | `void C(y.c observer, boolean useCloud)` | **incremental scan**: `new n0.g$e(observer,useCloud).executeOnExecutor(THREAD_POOL_EXECUTOR)` (corresponding to AIDL transaction 20 `u`) |
| 1243 | `boolean F(String engineName)` | "Whether cloud scanning is supported" → Returns the `f0.b` constant `f3551f` (all three engines are `true`, a constant true value independent of the engine) |
| 1252 | `void J(String engineName, boolean enable)` | Write engine enabled to SP `sp_key_*_engine_enable`; **`Mi` will never be written to** |
| 1260 | `int L(String[] paths, y.c observer, boolean useCloud, int scanType, String extras)` | **synchronization/full scan main entrance**: filter → current limit (≥10 rejected) → create 6 tables → start `n0.g$f`; return `f6412h` (number of tasks) |
| 1296 | `int M(String[] paths, y.c observer, boolean useCloud, int scanType, String extras)` | Scene distribution: `scanType==1` → `C()` increment; otherwise → `L()` |
| 1305 | `int N(y.c observer)` | **virus database update task**: `≥10` rejected; `u(observer,null)` + `n(t(x.a.c()))` + thread pool run `task.s()`; return taskId |
| 1330 | `List O(List<String> pkgs)` | Filter the whitelist by **package name**: `i0.h.d()` Get PackageInfo → `i0.f.a().e(pkg)` or `.f(sourceDir)` If hit, remove from the list |
| 1352 | `List P(List<String> paths)` | Filter the whitelist according to **apk path**: `i0.h.f()` + `i0.f.a().e()/.f(path)` |
| 1372 | `void q(int taskId)` | **cancelScan**: Check observer → subtask `i()` (stop engine, `I(id)`, clear handler); finally `f6421g.cancel(true)` |
| 1407 | `void r(int taskId)` | **cancelUpdate**: `A(taskId).j()` → `k()` for each engine |
| 1415 | `int s()` | **checkUpdate pile: Heng `return -1`** |
| 1419 | `boolean w(String engineName)` | Read SP enabled status (default false) |
| 1428 | `String[] x()` | Returns an array of engine names; **`new String[4]` only fills in 3 → the last bit is always null (bug)** |
| 1442 | `String y(String engineName)` | Engine display name: `getString(f3552g)` = Xiaomi/Anty/Tencent |

**Private / Internal method**

| OK | signature | Semantics |
|---|---|---|
| 987 | `c A(int taskId)` | Get the task (synchronization block) from `f6413i` by id, otherwise null |
| 998 | `y.f B(y.f in)` | Shallow copy results (10 fields) |
| 1014 | `y.f D(y.f a, y.f b)` | **multi-engine results are merged with**: covered by `virusLevel` priority 2>3>6>4>5>1, `extras` spliced; `packageName` is supplemented when empty |
| 1090 | `static c0.d.a E(PackageInfo, List cache)` | Cache hit: package name+versionName+signature MD5+sourceDir four-tuple congruent |
| 1107 | `static boolean G(List, f0.b)` | `list.contains(name)` predicate |
| 1112 | `static boolean H(String path)` | `new File(path).exists()` predicate |
| 1117 | `int I(int taskId)` | Remove from `f6413i` and return `f6412h--`; if not present return `-1` |
| 1132 | `void K(List list, y.c, boolean useCloud, int scanType, String extras, int offset)` | **starts engine thread**: `t(x.a.c())` builds engine → each engine `u()` builds task + `n0.g$b` Thread → `start()` → `z.a.c("security_scan")` buried point |
| 1150 | `int p(c task)` | **addNewTask**: 0..100 circularly finds vacancies, returns taskId and `f6412h++` |
| 1172 | `List t(List<String> names)` | `f0.b.values()` filter `names` → `n0.i().a()` reflection instantiation |
| 1190 | `c u(y.c observer, f0.a engine)` | `new c(observer, engine)`, including `linkToDeath` + `new b0.a(engine)` |
| 1195 | `static List v(Context)` | `MediaStore.Files` check `%.apk` → `i0.h.f()` to PackageInfo list |
| 983 | `private g()` | Constructor: call `x.a.l()` |

**internal class**

| inner class | OK | role |
|---|---|---|
| `a` extends `Handler` | 68-247 | Main thread progress/end aggregation; `what==2` ends, `what==3` single file progress; `AsyncTaskC0152a` is responsible for writing the results to `scan_result_db` (domestic version only) |
| `b` extends `Thread` | 249-280 | One thread per engine: `task.o(handler)` + `task.r(list,useCloud,extras)` |
| `c` implements `IBinder.DeathRecipient` | 282-647 | **Single engine task**: supports `y.c` observer, taskId, `b0.a` status, Thread; method `k()`Get id, `l()`Start engine, `o(Handler)`, `q(Thread)`, `binderDied()`, `i()`Cancel, `j()`Cancel update, `m(i,path)`Send message, `n(List)`, `p(id)`, `r(list,cloud,extras)`, `s()`Update; contains callbacks `a`(f0.a.c Scan callback), `b`(f0.a.InterfaceC0073a Initialization callback), `C0153c`(f0.a.b Update callback), `d`(InterfaceC0073a Initialization before update) |
| `d` | 649-662 | Signature cache structure: `f6453a`(path) `f6454b`(versionName) `f6455c`(versionCode) `f6456d`(sigMd5) |
| `e` extends `AsyncTask` | 664-754 | **incremental scan**: `getInstalledPackages(64)` + `v(ctx)`; cache hit and level∈{2,3,6} will be played back directly; otherwise `L(...,1,null)` will be processed in batches; `BinderC0154g` summary callback included |
| `f` extends `AsyncTask` | 756-892 | **whitelist pre-check**: `PackageParser.parsePackageLite` + `i0.f` judgment, the hit will call back the security result directly; the missed item will be handed over to `K()` |
| `BinderC0154g` extends `o0.a` | 895-963 | Incremental scan observer: callback `y.c.m(f[])` when the cumulative result → `onCacheScanFished==2` |
| `h` | 965-981 | `WhiteCheckInfo{PackageInfo, c0.d.a}` |

### 5.2 `SecurityService` Binder complete transaction table

**`IAntiVirusServer`** (descriptor `com.miui.guardprovider.aidl.IAntiVirusServer`, `y/a.java:19`; implement `SecurityService$a`)

| code | onTransact parameter (`y/a.java`) | method | Semantics | When the user is locked (`SecurityService.java`) |
|---|---|---|---|---|
| 1 | `String[]`, `IVirusObserver`, `int→bool` | `w(arr,c,useCloud)→int` | startScan（scanType=0） | `c.v(-1,null)`, return 0 |
| 2 | `String`, `int→bool` | `q(name,enable)` | setEngineEnable | Return directly (ignore) |
| 3 | `String` | `g(name)→boolean` | getEngineEnable | Return `false` |
| 4 | `String` | `P(name)→boolean` | isSupportCloudScan | **does not determine the lock**, directly `g.z().F()` |
| 5 | `int` | `N(id)` | cancelScan | ignore |
| 6 | `IVirusObserver` | `C(c)→int` | checkUpdate (constant -1) | Return `-1` |
| 7 | `IVirusObserver` | `h(c)→int` | updateScan (virus database update) | Return `c.x({engine:"",type:2})` + `c.K(-1)`, return 0 |
| 8 | `int` | `k(id)` | cancelUpdate | ignore |
| 9 | — | `getVersion()→String` | Heng `"1"` | Heng `"1"` |
| 10 | `String` | `J(name)→String` | getNameString (display name) | **does not lock** |
| 11 | — | `i()→String[]` | Array of engine names (last bit null, bug) | **does not lock** |
| 12 | `String`, `IWifiDetectObserver` | `B(name,d)→int` | wifiDetectNetworkState | Return 0 |
| 13 | `String`, `IWifiDetectObserver` | `D(name,d)→int` | wifiDetectDnsAndPhishing | Return 0 |
| 14 | `String`, `IWifiDetectObserver`, `String` | `O(name,d,ip)→int` | wifiDetectArp | Return 0 |
| 15 | `String`, `IWifiDetectObserver` | `Q(name,d)→int` | wifiDetectSecurity | Return 0 |
| 16 | — | `j()→String[]` | detectAppsScan result (package name of `basisOf==2`) | Return `new String[0]` |
| 17 | `String` | `G(pkg)→String` | detectAppScan single package determination (returns to the original name in the cache list, otherwise null) | Return `null` |
| 18 | — | `A()→int` | detectAppsScan upload (`/detect/app`) | Return `-1` |
| 19 | — | `e()→boolean` | Whether the virus cache is not empty | **does not lock** |
| 20 | `IVirusObserver`, `int→bool` | `u(c,useCloud)` | **incremental scan** (`g.z().C()`) | **does not determine lock (the only write operation without lock check)** |
| 21 | `List<String>` | `f(list)→List` | Filter whitelist by package name | **does not lock** |
| 22 | `String`, `int` | `E(str,type)` | Package change notification: `2/3`→Asynchronous cleaning `scan_result_db`+`risk_gray_result_db`; `1`→Incremental update of virus database | **does not lock** |
| 23 | `List<String>` | `M(list)→List` | Filter whitelist by apk path | **does not lock** |
| 24 | `String[]`, `IVirusObserver`, `int→bool`, `int` | `p(arr,c,useCloud,scanType)→int` | startScanAsScene | `c.v(-1,null)`, return 0 |

**`IURLScanServer`**（descriptor `com.miui.guardprovider.aidl.IURLScanServer`，`y/b.java:18`）

| code | parameters | method | Semantics | Lock |
|---|---|---|---|---|
| 1 | `String`, `String` | `H(engineName,url)→int` | `j.a().b()` → `UrlScanManager` → `f0.a.m(1,url)` (only AVL implements: `AVLEngine.scanURL`) | **does not lock** |
| 2 | `String` | `t(engineName)→int` | `j.a().c()` → `f0.a.m(2,null)` → `AVLEngine.updateURLRule()` | Return `-2` + Log |

**`IVirusObserver`**（callback，descriptor `com.miui.guardprovider.aidl.IVirusObserver`，`y/c.java:202`）

| code | method | Semantics (according to `n0/g` call site) |
|---|---|---|
| 1 | `d(int)` | onScanStarted |
| 2 | `b(int)` | Unused callback bit |
| 3 | `c(int, String)` | onScanStartItem（`n0/g.java:803`） |
| 4 | `s(int,int, VirusInfo[])` | onScanProgress(current/total/incremental results) |
| 5 | `v(int, VirusInfo[])` | onScanFinished(taskId + full result) |
| 6 | `r(int)` | Not used |
| 7 | `L(int,int)` | onScanPaused / binary progress |
| 8 | `y(int)` | onUpdateCanceled（`n0/g.java:468`） |
| 9 | `K(int)` | onUpdateFinished（`n0/g.java:438`） |
| 10 | `a(int)` | onCheckStarted（`n0/g.java:457`） |
| 11 | `l(int)` | Not used |
| 12 | `x(EngineUpdateInfo)` | onUpdateItemFinished（`n0/g.java:429`） |
| 13 | `m(VirusInfo[])` | onCacheScanFinished（`n0/g.java:718/741/956`） |

**`IWifiDetectObserver`** (`y/d.java`): only `onResult(int)` (code 1).

### 5.3 `y.f` (VirusInfo) complete field table

`y/f.java`，implements `Parcelable, Comparable`。

| Field | Type | Semantics | assignor | serialization |
|---|---|---|---|---|
| `f11657b` | String | `path` (apk absolute path; content:// is cacheDir/copy.apk) | Each engine | `writeToParcel` ① |
| `f11658c` | String | `packageName` | Each engine/whitelist population | ② |
| `f11659d` | String | `virusName` | AVL `info.getVirusName()`、TENCENT `entity.virusName`、NG `threatName` | ③ |
| `f11660e` | String | `virusDescription` | AVL `getVirusDescription()`、TENCENT `virusDiscription`、NG `kVar.b()` | ⑥ |
| `f11661f` | String | `engineName` (return value of `d()`: `"Mi"`/`"AVL"`/`"TENCENT"`; NG hard-coded `"MiEngineNG"`) | Each engine | ⑦ |
| `f11662g` | String | `versionName` | AVL/TENCENT/NG is filled in; `n0/g.java:139` is cached | ⑧ |
| `f11663h` | String | `adName` (**whole tree 0 assignment,** is not implemented) | — | ⑨ |
| `f11664i` | String | `adDescription` (**whole tree 0 assignment,** is not implemented) | — | ⑩ |
| `f11665j` | int | `versionCode` | Each engine | Not serialized (lost in Parcel) |
| `f11666k` | int | `virusType` (actually only 0/1, see §4.8) | AVL/TENCENT write 1 | ④ |
| `f11667l` | int | **`virusLevel`**: 1=safe, 2=virus/most dangerous, 3=risk, 4=?, 5=?, 6=gray/anti-fraud | Each engine + `n0/g` whitelist downgrade | ⑤ |
| `f11668m` | int | `adLevel` (**whole tree 0 assignment,** is not implemented) | — | ⑪ |
| `f11669n` | boolean | `isAuthoritative` (determination of whether it is "authoritative", only `g0/e.c()` is set to true when `i0.m.type ∈ 1..3`) | MiEngine path | **does not serialize** |
| `f11670o` | boolean | `isInCache` (flag bit, **whole tree 0 assigned value**) | — | **does not serialize** |
| `f11671p` | String | `extras`; format `"<engine>:<safe\|danger>;"`, **string splicing** when merging multiple engines | AVL/TENCENT/NG/g0.e | ⑫ |

**`extras` Actual measured value** (can be used as a fingerprint for "which engine determines"): `"Mi:safe;"`/`"Mi:danger;"` (`g0/e.java:71-73`), `"AVL:safe;"`/`"AVL:danger;"` (`AvlEngine.java:44-46`), `"TENCENT:safe;"`/`"TENCENT:danger;"` (`TencentEngine.java:382-384`), `"MiEngineNG:safe;"`/`"MiEngineNG:danger;"` (`MiEngineNG.java:62,168,180`), `"AntiDefraud:danger;"` (`g0/e.java:29`).

**`Comparable` Sorting weight** (`y/f.java:77-97`): `isAuthoritative` → 100; level 1→0, 2→5, 3→4, 4→2, 5→1, 6→3, others→-1. The "same path takes the most severe" cache strategy for `n0/g.java:338`.

**Parcel asymmetry defect [Observed]**: `writeToParcel` writes 12 values ​​(including `f11668m`, `f11671p`), `f(Parcel)` reads 10 values ​​(`f11657b,c,d,k,l,e,f,g,p`). `f11668m` is written but not read → `adLevel` is lost after crossing processes; `f11665j` (versionCode), `f11669n`, and `f11670o` do not participate in serialization at all.

### 5.4 SharedPreferences complete key table

File name `"GuardProvider"`, mode `0` (`w0/f.java:20`). `w0.f` provides `c()`=getBoolean (default false), `d(k,def)`=getInt, `e(k)`=getLong (default 0), `f(k)`=getString (default null), `g/h/i/j`=put+**commit()** (synchronous placement).

| key | Type | read | write | Default/Semantics |
|---|---|---|---|---|
| `sp_key_mi_engine_enable` | boolean | `n0/g.java:1422` (via `f0.b.MI.f3550e`) | `n0/g.java:1255`、`x/a.java:148,180` | false; **`J()` refuses to write Mi** |
| `sp_key_avl_engine_enable` | boolean | Same as above | Same as above | false |
| `sp_key_tencent_engine_enable` | boolean | Same as above | Same as above | false |
| `cloud_engine` | String | `x/a.java:16` | `x/b.java:174` | null; current cloud engine name ("AVL"/"TENCENT") |
| `cloud_engine_list` | String(JSON array) | `x/b.java:32`, `x/a.java:67` (via `b.a()`) | `x/b.java:170` | null; a collection of engine names issued by cloud control |
| `antivirus_is_init` | boolean | `x/a.java:147,178` | `x/a.java:152,184` | false; initialize sentinel for the first time |
| `load_cloud_engine_time` | long | `x/a.java:52` | `x/a.java:173` | 0; last cloud control pull time (ms) |
| `virus_cache_validty_perod` | int | `c0/b.java:69`、`c0/d.java:97` | `x/b.java:135` | 3 (days); **pay attention to the spelling `validty` (one missing i)** |
| `key_uuid` | String | `w0/b.java:19` | `w0/b.java:24` | null; `UUID.randomUUID()` is generated for the first time |
| `avl_url_sdk_version` | String | — (Write only, not read) | `AvlEngine.java:207` | null; AVL URL library version number |

**[Observed]** Whole tree grep `sp_key_`/`cloud_engine`/`antivirus_is_init`/`key_uuid`/`avl_url_sdk_version`/`virus_cache` Total hits 10 keys, **does not omit** (`_obf_index.tsv` also does not have the 11th key).
**[Observed]** There are also 4 sets of **non-original SP** preference files (belonging to third-party libraries): `avl_conf` and `avl_<suffix>` (`com/avl/engine/c/f.java:14,35`), TMSDK Self-built (`tmsdkobf/q5.java:20`, `tmsdkobf/m7.java:20`), OneTrack self-built (`com/xiaomi/onetrack/util/aa.java:109`).

### 5.5 Engine metadata (11 columns and 8 strings of `engine_info_list`)

`f0.b` Complete parameters of three constants (`f0/b.java:15-17`):

| constant | `f3547b` internal name | `f3548c` code | `f3549d` implementation class | `f3550e` SP key | `f3551f` cloud scan | `f3552g` name resource | `f3553h` array resource | `f3554i` group |
|---|---|---|---|---|---|---|---|---|
| `MI` | `"Mi"` | 100 | `MiEngineNG` (cloud control can cut `MiEngine`) | `sp_key_mi_engine_enable` | true | `0x7f0b0005` | `0x7f030001` | 3 |
| `AVL` | `"AVL"` | 11 | `AvlEngine` | `sp_key_avl_engine_enable` | true | `0x7f0b0000` | `0x7f030000` | 2 |
| `TENCENT` | `"TENCENT"` | 10 | `TencentEngine` | `sp_key_tencent_engine_enable` | true | `0x7f0b0007` | `0x7f030003` | 2 |

Column semantics of **8 string array items** (`arraycopy(..., 1, 8)` of `GuardProvider.java:389` → columns 1..8):

| array subscript | `MatrixCursor` column | Resource name (avl/tencent) |
|---|---|---|
| 0 | `NameString` | `@string/AVL` / `@string/Tencent` |
| 1 | `DescriptionName` | `@string/DesAVL` / `@string/DesTencent` |
| 2 | `DialogTitle` | `avl_dialog_title` / `tencent_dialog_title` |
| 3 | `DialogHeader` | `avl_dialog_header` / `tencent_dialog_header` |
| 4 | `DialogPolicy` | `avl_dialog_policy` / `tencent_dialog_policy` |
| 5 | `DialogCloudScan` | `avl_dialog_cloudscan` / `tencent_dialog_cloudscan` |
| 6 | `DialogSummary` | `avl_dialog_dbupdate` / `tencent_dialog_dbupdate` (**array item is called dbupdate, column name is Summary**) |
| 7 | `DialogUrl` | `avl_dialog_url` / `tencent_dialog_url` |

**actual value per engine (`res/values/strings.xml` default=en, `values-zh-rCN` Chinese)**

| item | AVL（en / zh-rCN） | TENCENT（en / zh-rCN） | MI（en / zh-rCN） |
|---|---|---|---|
| NameString | `AVL` / `Antian` | `Tencent` / `Tencent` | `Xiaomi` / `Xiaomi` |
| DescriptionName | `AVL` / `Anty AVL` | `Tencent` / `Tencent` | `Xiaomi` / `Xiaomi` |
| DialogTitle | `"AVL's Privacy Policy"` / `Virus Scan` | `Tencent’s Privacy Policy` / `Virus Scan` | *(empty string)* |
| DialogHeader | `"This feature encrypts and uploads your device's ID to AVL's servers (%s)."` / `This function uses the anti-virus engine provided by Antiy AVL, details can be found in %s` | `"This feature encrypts and uploads your device's ID to Tencent's servers (%s)."` / `This function uses the anti-virus engine provided by Tencent, details can be found in %s` | *(empty string)* |
| DialogPolicy | `Privacy Policy` / `Anty AVL Mobile Anti-Virus Engine License Agreement` | `Privacy Policy` / `Tencent Mobile Manager Software License Agreement` | *(empty string)* |
| DialogCloudScan | *(empty string, resource is undefined)* | `Upload and scan app names and details using online virus definitions.` / `We will upload the application package name and application characteristics to the cloud virus engine for scanning` | *(empty string)* |
| DialogSummary | *(empty string)* | `Update virus definitions automatically` / `We will upload the encrypted IMEI to the server for updating the virus database` | *(empty string)* |
| DialogUrl | `http://api.sec.miui.com/policy/proxy?p=antiy&l=<zh_CN\|en_US\|zh_TW>` | `http://api.sec.miui.com/policy/proxy?p=tencent&l=<…>` | *(empty string)* |

**[Observed]** `mi_string_info`’s 6 `<item>` are empty (`arrays.xml:14-22`) → MI engine only has names and no privacy copy on the UI.
**[Observed]** `values-zh-rTW` also exists completely (Anty/Tencent Taiwan terms), `values-en-rGB` **does not exist** string string (fall back to the default value).

#### 5.5.1 "Copywriting vs. Actual Implementation" Privacy Comparison

| engine | Statement copy (zh-rCN) | Actual behavior of code | Consistency |
|---|---|---|---|
| TENCENT | "We will upload the application package name and application characteristics to the cloud virus engine for scanning." | `TencentEngine.j()` gives `path` to `QScannerManagerV2.scanUninstallApks(scanType, [path], listener, 0)`; `scanType=6` (local + cloud, as `b()&&z6`) otherwise `2`. The upload content is determined internally by TMSDK. This application only transmits the path. | **is partially consistent with** (cloud feature upload does occur, but it is implemented internally by SDK and is not controllable by this application) |
| TENCENT | "We will upload the encrypted IMEI to the server for updating the virus database." | This application side: `TencentEngine.e()` only works with `TMSDKContext.setDeviceInfo(ctx, Settings.Secure.getString(cr,"android_id"), Build.MODEL)` (`TencentEngine.java:402`). **does not have any `getImei`/`getDeviceId` calls to** (full tree grep is only in `DeviceUtil` in OneTrack, see below). Whether TMSDK is internally read IMEI: [Unknown](`libTmsdk-2.0.13-mfr.so` is not disassembled) | **copy exceeds visible implementation**: What this application transmits is **android_id + Build.MODEL**, not IMEI |
| AVL | "This function uses the anti-virus engine provided by Antiy AVL, please see %s for details" (`DialogHeader` said "uploads your device's ID") | `AvlEngine` does not have any explicit upload call; `AVLEngine.setRegion(Build.getRegion())`, `setNetworkEnabled`, `setCloudScanEnabled`, `init(ctx, AVLSdkConfig.setDeviceType(...))`. The device identification is uploaded inside `libavlm.so` | **cannot falsify** (native internal) |
| — | — | **but**: `Configuration.Builder` of `OneTrack` (`z/b.java:25`) **is not closed IMEI/IMSI**, while `Configuration.Builder`'s `f1855h`(IMSI)/`f1856i`(IMEI) Default `true`(`com/xiaomi/onetrack/Configuration.java:86,89`), `DeviceUtil` will use reflection `getImei(int)`/`getDeviceId()`/`getSubscriberId()`(`DeviceUtil.java:305-356,668`) | **The actual IMEI collection comes from OneTrack, not the Tencent engine**; the first scan (`n0/g.java:1147` `z.a.c("security_scan")`) is initialized |

**[Conclusion]** Among the two privacy documents of Tencent, the first one is consistent with the implementation direction (cloud scanning), and the second one "Upload IMEI" cannot find the corresponding implementation **in the Java layer** of this application (only android_id); and the one who can really read IMEI/IMSI is the same OneTrack in APK is buried in SDK, and **turns on** by default. This is a deviation of "the copywriting points to A and the implementation falls on B", which is worthy of being listed separately in the upper-level report.

### 5.6 Constants/Protocol Fields

**AIDL / Provider protocol**

| item | value | location |
|---|---|---|
| authority | `guard` | `AndroidManifest.xml`、`GuardProvider.java:88-92` |
| URI | `engine_info_list`(1005)、`sync_scan`(1006)、`virus`(1007)、`install_scan`(1008)、`support_incremental`(1009) | `GuardProvider.java:88-92` |
| `call()` method name | `"parseApk"`、`"getRiskAppData"` | `GuardProvider.java:331-334` |
| ContentProvider permissions | `com.miui.guardprovider.permission.Guard_Provider`（signature\|privileged） | Manifest |
| Service action | `com.miui.guardprovider.action.antivirusservice` / `…urlscan` | `SecurityService.java:312-317` |
| AIDL descriptor | `com.miui.guardprovider.aidl.IAntiVirusServer` / `IURLScanServer` / `IVirusObserver` / `IWifiDetectObserver` | `y/a.java:19`、`y/b.java:18`、`y/c.java:202`、`y/d.java`（`writeInterfaceToken`） |
| broadcast action | `miui.intent.action.TRACK_ANTIVURS_DATA` (**is spelled like**), `setPackage("com.miui.securitycenter")` | `n0/h.java:13-14` |
| Broadcast extras(14) | `path`, `packageName`, `virusName`, `virusDescription`, `engineName`, `versionName`, `adName`, `adDescription`, `versionCode`, `virusType`, `virusLevel`, `adLevel`, `isAuthoritative`, `isInCache`, `extras` (**is actually 15**) | `n0/h.java:15-29` |
| Broadcast suppression conditions | `level ∈ {1,4,5}` is not sent; `fVar==null` is not sent | `n0/h.java:11-12` |
| `parseApk` flags | bit6(64)=Collection certificate; bit7(128)=With icon+label | `GuardProvider.java:168,175` |
| `sync_scan` return column | `Type, VirusName, Description, EngineName, AdType, AdName, AdDescription` | `GuardProvider.java:257` |

**Key/Signature Salt (hardcoded, 3 UUID)**

| value | Purpose | location |
|---|---|---|
| `94e6cec2-19dc-4bb6-823c-613f72d3ea16` | `sign` salt of `api.sec.miui.com/antivirus/{viruses,whitelist}` | `i0/d.java:55,203` |
| `6988567a-4220-4b51-bc2d-ccdec27a74a1` | `sign` salt of `flash.sec.miui.com/detect/app` | `j0/c.java:75` |
| `62b36629-ecb4-4725-9f33-ccb0fd158191` | `virus-engine.sec.miui.com` query `sign` salt | `q0/q.java:20` |
| Signature algorithm (old stack) | `MD5( Base64( sorted("k=v"&…) + "&" + salt ) )` uppercase hexadecimal | `h0/b.java:200-232` |
| Signature algorithm (new stack) | `MD5( sorted("k=v"&…) + "&" + salt )` lowercase hexadecimal (no Base64) | `q0/q.java:13-30` |
| Device fingerprint | `mi = MD5( android_id + "-" + salt )` | `h0/b.java:128-146` |
| OneTrack appId | `2882303761517522561`, channel = package name, `GAIDEnable=false` | `z/b.java:22` |

**Other constants**

| item | value | location |
|---|---|---|
| Maximum concurrent scanning tasks | 10 | `n0/g.java:1272` |
| Sync wait timeout | 2 minutes | `GuardProvider.java:250,272` |
| internal engine throttling | Per file `Thread.sleep(5)` | `AvlEngine.java:152`、`TencentEngine.java:464`、`g0/e.java:89` |
| Maximum signature scan size | 20 MB（`20971520`） | `p0/y.java:103` |
| taskId space | 0..100 ring | `n0/g.java:1159-1161` |
| Cloud control refresh cycle | 86400000 ms（24h） | `x/a.java:63` |
| Cache default validity period | 3 days | `x/b.java:135` |
| Global `_global` flag | `ro.product.mod_device` contains `"_global"` | `d5/a.java:10` |
| `jp_kd` special area | `ro.miui.customized.region == "jp_kd"` | `GuardApplication.java:44` |
| Network gate properties | `persist.sys.sc_allow_conn` | `GuardApplication.java:40,43` |
| jp_kd additional gate | `Settings.Secure "antivirus_engine_network_enable" == 1` | `GuardApplication.java:45` |
| Hybrid scanning capabilities (SecurityCenter side) | metaData `securitycenter.support.mix.scan` | `h0/a.java:14` |
| Full scanning capability (SecurityCenter side) | metaData `miui.support_full_scan` | `i0/h.java:92` |
| AVL appKey | metaData `AVL_OPENSDK_APPKEY` (`D5DFB878335A3EF4E76F60ED470B8EC8`), fell back to `AVL_SDK_APPKEY` | `com/avl/engine/a/e.java:39-46` |
| AVL DB version key | `AVLA_DATA_VERSION`、`AVLA_LAST_INIT_STATE` | `com/avl/engine/a/c.java:213` |
| TMSDK Update Sentinel error code | `-2146769239` | `TencentEngine.java:165,196,247` |
| TMSDK update logo | `UpdateConfig.UPDATE_FLAG_VIRUS_BASE` | `TencentEngine.java:493` |
| AVL URL library version SP key | `avl_url_sdk_version` | `AvlEngine.java:207` |

### 5.7 Database and tables (perspective of this layer)

| Library | version | Helper | table → key column |
|---|---|---|---|
| `miui_virus.db` | 5 | `w0/h.java` | `virus_white_db(_id, package, path, version_code, size, engine, local_version, extra)`；`t_virus_white_list(_id, scan_result_type, scan_item_type, app_label, desc, dir_path, pkg_name, virus_name, virus_md5)` |
| `miui_antivirus.db` | 2 | `i0/b.java` | `antivirus_db_version(_id, version, release_time)`；`antivirus_db(_id, package, version, signatures, md5, type, virus_name, description, strategy, descType)` |
| `mi_engine_white_virus.db` | 1 | `i0/e.java` | `mi_virus_white_db(_id, package, signatures, path)`；`mi_detect_app_db(_id, package, signatures)` |
| `risk_gray_result.db` | 1 | `c0/a.java` | `risk_gray_result_db(_id, packageName, versionCode, signatures, type, time)` |
| `scan_result.db` | 2 | `c0/c.java` | `scan_result_db(_id, path, packageName, virusName, virusDescription, engineName, versionName, versionCode, virusLevel, extras, sign, time)` |
| `flamingo.db` | 2（Room） | `com/miui/guardprovider/sdk/android/db/EngineDatabase` + `r0/x.java:22` | 8 Table: `trust_certs`, `trust_installers`, `innocent_package`, `characteristics`, `guilty_package`, `black_gray_chs`, `black_gray_app`, `version` (7 version numbers) → See t-cloud report for details |

**migration path [Observed]**: `w0/h.onUpgrade` 1→2 plus `version_code,size`, 2→3 plus `engine,local_version`, 3→4 build `t_virus_white_list`, 4→5 plus `extra`; `onDowngrade` empty implementation (data retained). `i0/e.onDowngrade` / `c0/a.onDowngrade` / `c0/c.onDowngrade` will be rebuilt after `DROP TABLE` (**is downgraded and the library is cleared to**). `c0/c.onUpgrade` 1→2: `RENAME TO scan_result_db_old` followed by `DROP` (equal to clear and rebuild).

---

## 6. Permissions and attack surface/risk points

### 6.1 Ingress convergence: only 2 exported components

| components | exported | protect | accessible methods |
|---|---|---|---|
| `GuardProvider`（provider, authority `guard`） | true | `com.miui.guardprovider.permission.Guard_Provider`（**signature\|privileged**） | `query`(5 URI), `insert`, `delete`, `call`(2 methods), `update`(empty) |
| `SecurityService` (service, double action) | true | Same as above | 24 `IAntiVirusServer` transactions + 2 `IURLScanServer` transactions |

The attack surface of **[Inferred]** is strictly restricted by `signature|privileged`: only applications with the same signature (MIUI security center family) or platform components holding `privileged` can call it. **But this does not mean zero risk**: `privileged` means that any preset application under `/system/priv-app` can call all 26 transaction codes - including `parseApk` (parse any path apk) and `sync_scan` (scan any path).

Permissions applied for by **(related to this article)**: `READ_PHONE_STATE`, `READ_PRIVILEGED_PHONE_STATE`, `INTERNET`, `READ/WRITE_EXTERNAL_STORAGE` (maxSdk 29), `MANAGE_EXTERNAL_STORAGE`, `QUERY_ALL_PACKAGES`, `PEERS_MAC_ADDRESS`, `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER`, `com.android.settings.permission.CLOUD_SETTINGS_PROVIDER`, `INTERACT_ACROSS_USERS`, `ACCESS_MTP`, `miui.permission.READ_AND_WIRTE_PERMISSION_MANAGER`.
**[Observed]** applied for `RECEIVE_BOOT_COMPLETED` but **does not have any Receiver** (Manifest only registered dynamic `USER_UNLOCKED`) → redundant permissions.
**[Observed]** `READ_PRIVILEGED_PHONE_STATE` exists, but there is no IMEI reading code in the Java layer of this application (see §5.5.1).

### 6.2 Protocol layer risks

| # | risk | location | Notes |
|---|---|---|---|
| P1 | `parseApk` can resolve any path ** ** | `GuardProvider.java:159-190` | No path whitelist; only try LBE authorization first when "there is no READ_EXTERNAL_STORAGE and is not under `/data`". Those with signature authority can use it to enumerate `PackageInfo` (including `signatures`, `versionCode`, `sourceDir`) and read application tags/icons - equivalent to a **arbitrary apk metadata reader** |
| P2 | `sync_scan` Accept `content://` Any URI | `GuardProvider.java:211-215` | After meeting `openInputStream`, copy it to `cacheDir/copy.apk` → Readable content provider content that the caller does not have permission to read directly (read with the permissions of this process). This is a typical **provider agent reading (confused deputy)** surface |
| P3 | Fixed temporary file name `copy.apk` | `GuardProvider.java:213` | Concurrent `content://` scans cover each other; and `Files.copy(..., REPLACE_EXISTING)` has no size limit |
| P4 | `getRiskAppData` has no parameters, **fully exports** installed application gray output conclusion | `GuardProvider.java:103-127` | Returns the package name + versionCode + type + signature MD5 in all non-system applications that are judged to be gray; the caller can make an application portrait based on this |
| P5 | Broadcast without permission | `n0/h.java:13-14` | `setPackage` is limited to a single package, but broadcast **does not have** `sendBroadcast(intent, receiverPermission)`. Any component that can declare the `com.miui.securitycenter` package name (theoretically not, unless it has the same signature/system) can be accepted; the actual risk is low |
| P6 | Transaction 20/21/22/23 **does not check user lock** | `SecurityService.java:251-253,154-156,76-89,107-109` | When the user is locked, all other methods will be downgraded. Only "incremental scanning/whitelist filtering/package change notification" will be executed as usual → scanning and clearing can be triggered during the lock screen period. |
| P7 | `getVersion()` Heng `"1"` | `SecurityService.java:169-172` | There is no protocol version negotiation mechanism, and subsequent changes to the protocol are not backward compatible. |
| P8 | Transaction 11 returns an array containing `null` | `n0/g.java:1429` | `new String[4]` only fills in 3; if the caller directly `for`, null will be used as the engine name |
| P9 | `install_scan` **silently degrades** when there are insufficient parameters | `GuardProvider.java:415` | When 3 parameters degenerate into 2 parameters, the query result of `t_virus_white_list` is returned (`cursorQuery` is null in the 1008 branch, so it is actually null) → Semantic drift |

### 6.3 High-risk behaviors on the host side

See table R1–R20 in §4.10. Top 5 by hazard:

1. **R17 IMEI/IMSI collects** (OneTrack is enabled by default, without `setImeiEnable(false)`) - violates the "minimum necessity" and is misaligned with Tencent's copywriting.
2. **R11 Cross-application private SQLite reads** (`/data/data/com.qiyi.video/...`, `com.sohu.sohuvideo/...`) - TMSDK legacy code, the reachability of this application is unknown.
3. **R3 command injection surface** (`Runtime.exec("ping …" + host)`) - `host` comes from IPC parameters.
4. **R1 writable directory `.so` loads** (`System.load(cacheDir derived lib/ + name)`) - internal to TMSDK.
5. **R10 Cross-app authorization** (`GrantGuardProvider` of `content://com.lbe.security.miui.su`) - Bypass storage permissions.

### 6.4 Deviations/Correction from “Confirmed Facts”

| Original briefing | Observations in this article | influence |
|---|---|---|
| `install_scan` = Synchronous scan 3 parameters `[path, bool, engineName]` | **`engineName` does not participate in engine selection**. It is passed all the way to the fourth parameter of `f0.a.j(List, c, boolean, String)` as `extras` (`n0/g.java:624` → `b0.a.f314i` → `n0/g.java:539`), while the four engines of **all ignore this parameter** (`AvlEngine.j` only uses `z6`; `MiEngine.j` not quoted; `MiEngineNG.j` not quoted; `TencentEngine.j` not quoted) | The semantics of "scan by engine name" **does not exist**; the engine set is always determined by `x.a.c()` (SP enablement item) |
| `guardprovider.scan_properties=2` may correspond to scan type 2 | The meta-data **reads** at 0 in the whole tree | Dead configuration; `2` has no causal relationship with `scanType=2` of `n0/g.L()` |
| `guardprovider.support.mix.scan=true` Control hybrid scan | The meta-data **0 reads**; the hybrid scan determines that it reads **`securitycenter.support.mix.scan` (`h0/a.java:14`) of**SecurityCenter | The meta-data of this application is for **and others** to read (SecurityCenter side-reads the statement of guardprovider), not for your own use |
| Engine registry `MI("Mi",100,…)` | Consistent, but need to be supplemented: the implementation class of `Mi` is determined by cloud control `VirusScan/miEngineNgEnable` when **enumeration is constructed** (`f0/b.java:56-60`), and the default is `MiEngineNG` | The same `MI` constant may be two different implementation classes |
| "How to select the engine with the engine name parameter" | See above, no choice | — |
| `i0.h.h(ctx)` = Incremental support determination | The bottom layer reads `miui.support_full_scan` (**fully scans the** capability bit); the provider’s URI name is `support_incremental` | Naming and semantic misalignment (consistent with Lead’s conclusion) |
| (Not mentioned in the briefing) QAX fourth engine | The resources are complete but **has no enumeration constant, no engine class, and no call point**. The value of 6 `qax_dialog_*` is `TODO`. | QAX not implemented |
| (Not mentioned in the briefing) `virusType` and 12 threat types | 12 `threat_type_*` resources **have no Java reference**; `virusType` actually only has 0/1 | Threat classification is not in this layer |

---

## 7. Unresolved issues

| # | question | What step has been achieved | Suggestions for next steps |
|---|---|---|---|
| U1 | `libflamingo.so` internal implementation (`matches`/`search` algorithm, whether OOM can be triggered by malicious features) | Only confirm 3 native symbols and unique call sites | Use REA `open_binary` + Ghidra to perform symbol-level analysis on `libflamingo.so` (arm64) |
| U2 | Does TMSDK really read/upload IMEI or IMSI (implementation point of Tencent copywriting) | Confirm that the Java layer only transmits `android_id`+`MODEL` | Disassemble `libTmsdk-2.0.13-mfr.so`, or capture and observe the `tmfsdk.m.qq.com` report body |
| U3 | `libavlm.so` reporting field of AVL (`DialogHeader` claims to upload "device's ID") | Confirm that the Java layer is only `setRegion`/`setNetworkEnabled`/`setCloudScanEnabled`/`init(deviceType)` | Disassemble or capture packet `virus-engine.sec.miui.com` |
| U4 | `tmsdkobf/nb.java` Is cross-application DB read reachable? | Confirm that code exists; caller not found | Use `trace_android_references` to chase the incoming edge of `nb.b(OfflineVideo)` |
| U5 | Whether the `ping` injection of `tmsdkobf/t9.java` can be controlled by the IPC parameter on the actual link | Confirm that the `n0/k.c()` → `detectARPNoRoot` link exists; `str2` is not filtered | Trace the IPC entry (transaction 14) of `k.a().c()` to the complete parameter flow of `t9.a()` |
| U6 | The specific enabling combination when `h0.a.a()` in `x.a.l()` is true (SecurityCenter supports mix scan) | Confirm the writing and differences of the two branches (`l()` vs `n()`) | Actual measurement or reading SecurityCenter side code |
| U7 | The relationship and priority between `c0.b` (gray cache) and `EngineDatabase.guilty_package` (Flamingo library) | Confirm that the two exist independently, `getRiskAppData` only reads the former | Belongs to t-obf/t-cloud cross range |
| U8 | The superposition sequence of bit3 of `scan_params` (disabled cloud scanning) and `GuardApplication.e()` network gate in `p0` pipeline | Two independent judgments are known | Belongs to t-cloud scope |
| U9 | The semantics of level `4` and `5` of `y.f` | Only infer from the `D()` priority and the suppression condition of `n0/h` (4/5 is not reported) | Requires SecurityCenter side consumption code |
| U10 | Behavior of LBE license for `w0/e.java` on non-MIUI devices | Only confirm `catch(Exception)` silently | Actual measurement |
| U11 | `f0.b.MI.f3554i=3` The actual use of this "group" field | `x.a.e()` only uses `1`/`2`, `3` is excluded | Confirm whether there are other consumption points (not found) |
| U12 | `adName/adDescription/adLevel` is always empty among the 15 extras broadcasted. | Confirm that there is no assignment point | Need to confirm whether the agreement is dead |

---

## 8. REA evidence ID list

> provider = `jadx` (JADX headless 0.7.1, reas-jadx-bridge v7); target SHA-256 = `b0c02fa682b8ca82429b471113d0702042ac76234c85fdcd8efeb7c03d313024`. All are **static** evidence (APK is not executed).

| # | Evidence ID | Operation | object | for support |
|---|---|---|---|---|
| 1 | `ev_37fb1e7ff90b4aa164d782fe3b6884a5286668e49a896189e73bdc3bc6fd4a2d` | `inspect_android_class` | `com.miui.guardprovider.GuardProvider` | 15 methods / 3 fields / 2 inner classes (`a`, `b`); prove that the DEX prototype of `call`/`query`/`delete`/`insert` is consistent with the §3.2 link |
| 2 | `ev_cdd2481ef4691e629b465b3dbf2a0e4b5c67468163e50c2215762321c8c21856` | `inspect_android_class` | `com.miui.guardprovider.manager.SecurityService` | 12 methods and fields `f1787a:y.a$a`, `f1788b:y.b$a`; prove two Binder implementation class fields |
| 3 | `ev_f6df526e2304e20b8b94873de133e216f7b9cf614feac15cf2c9790bb9809563` | `inspect_android_class` | `com.miui.guardprovider.manager.SecurityService$a` | **24 AIDL method** + auxiliary `R`/`S`; support one by one §5.2 Transaction table 1–24 |
| 4 | `ev_80d5f918be73df59aab7cde8f6ae521b6942ee9b74977c2baede12332debb110` | `inspect_android_class` | `f0.b` | 12 fields, **3 enumeration constants** (`f3543j/k/l` + array `f3546m`), `b():Class`; supports "only 3 engines, no QAX" |
| 5 | `ev_0c030a86f4a9248825ba7e3e35078c3f8fd11d8c87de7d7cb3a4c9aa12db4932` | `inspect_android_class` | `com.miui.guardprovider.natives.RustSupport` | 3 native methods (`matches(byte[],String)`, `search(byte[],String)` overload 0, `search(byte[],String[])` overload 1) + `<clinit>`; support §4.9 |

**[Limitations (provider readme)]** static decompilation does not prove runtime behavior; the method signature is the provider display type, and the DEX prototype comes from frozen JADX metadata; the class list contains synthetic members, and the source code location is unknown; APK signature, split, native library, and runtime are not verified by the provider.


