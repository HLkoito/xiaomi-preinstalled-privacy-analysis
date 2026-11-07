> **Historical analysis with subsequent corrections.** Use the [final review](../REPORT.md) for the reviewed findings. It supersedes earlier claims about plaintext IMEI lists, actual uploads under default settings, absolute coverage, and unverified native behavior. The DAU IMEI and IMSI lists use MD5 and remain subject to region, permission, and network checks. Certificate expiry alone does not establish that ciphertext cannot be decrypted. Local identity paths were removed. Historical source line references may point to files outside the published evidence set.

# Built-in third-party SDK’s actual capabilities and called interface (t-vendor)

> Target: `GuardProvider-2.0.3-20231124.0.apk` (SHA-256 `b0c02fa682b8ca82429b471113d0702042ac76234c85fdcd8efeb7c03d313024`)
> Corpus: JADX full source code `_analysis/src/sources/` (3231 files), apktool `smali/`, `lib/arm64-v8a/`, `assets/`
> Annotation convention: **[observation]** source code/bytes are directly visible; **[inference]** has a basis for semantic induction; **[unknown]** is not covered.
> Coverage boundary: This article only answers "What can SDK do + what is actually used by this APK", and does not do a class-by-class running account; the class count is by `Get-ChildItem -Recurse -Filter *.java | Measure-Object` Obtained (com\avl=411, tmsdk=90, tmsdkobf=342, com\tencent=8, com\xiaomi\onetrack=163, xcrash=12, com\sun=12, android\content\pm=3).

## 0. Methodology prefix: JADX "constant pool replacement" pseudo-reference of this corpus (must read, otherwise the calling relationship will be misjudged)

**[Observation]** The JADX output used in this project replaces "a literal with the same value as a packaged `public static final` constant" with a symbolic reference to the constant, and automatically completes the import. Evidence (three mutual confirmations of the same mechanism):

| phenomenon | Evidence | real literal |
|---|---|---|
| `new StringBuilder(PowerNest.SHARK_TCP)` as the capacity parameter of `toString()` | `com/avl/engine/b/a/a.java:45`、`com/avl/engine/e/a/d/f.java:4`；`tmsdk/common/roach/nest/PowerNest.java:15` `SHARK_TCP = 1024` | `new StringBuilder(1024)` |
| 211 `case WifiDetectManager.CLOUND_CHECK_PASSWORD_LEAK_RISK /* 20 */` in the whole tree (including okhttp/AVL/OneTrack irrelevant files) | `a6/b3.java:670`, `b4/f.java:919`, etc. | Integer `20` |
| `com.xiaomi.onetrack.a.a.C0039a.f1965g` is regarded as the directory name | `UrlDetector.java:221/228/229`; smali side `apktool/smali/com/avl/engine/urldetector/UrlDetector.smali`’s `const-string "avlurl"` | String `"avlurl"` |
| `com.xiaomi.onetrack.b.m.f2273f` as engine map key | `com/avl/engine/security/g.java:48`；`com/xiaomi/onetrack/b/m.java:22` `f2273f = "av"` | String `"av"` |
| `com.xiaomi.onetrack.util.z.f2775b` as join separator | `com/miui/guardprovider/engine/mi/MiEngineNG.java:192`；`util/z.java:16` `f2775b = ","` | String `","` |
| `com.xiaomi.onetrack.g.b.f2461a` as timeout in milliseconds | `tmsdkobf/p9.java:365`；`onetrack/g/b.java:24` `f2461a = 10000` | Integer `10000` |

**Conclusion/Usage [Inference]**: Whenever the "constant classes" of `tmsdk.*` and `com.xiaomi.onetrack.*` appear in positions unrelated to semantics (switch labels, timeouts, capacities, delimiters), they are replaced by the JADX constant pool, and **does not constitute a violation of SDK The call is**. Determining "live code" must see the **real API calling** (`ManagerCreatorX.getManager(...)`, `TMSDKContext.*`, `OneTrack.*`) or the **type** (field/parameter/`new`). All "actual activation" conclusions in this article are based on this caliber and double-checked with smali/constant values.

## 1. Overview of Responsibilities

This APK packages 5 executable third-party subsystems + 4 groups of "deliberately introduced framework classes":

| SDK / Components | Belong | The characters in this APK | Are you really involved in the business? |
|---|---|---|---|
| Tencent AVL engine `com.avl.engine.**` (Class 411 + `libavlm.so` 2.30 MB / `libavlurl.so` 615 KB / `libavlasys.so` 87 KB + `assets/avlsdk` 1.53 MB) | Tencent (Antiy/AVL engine, customized by Xiaomi) | One of the three anti-virus engines (engine name `AVL`, code 11) | **is**, `AvlEngine` is fully wired |
| Tencent TMSDK `tmsdk.**` + `tmsdkobf.**` (Category 432 + `libTmsdk-2.0.13-mfr.so`) | Tencent MSUI | Engine `TENCENT` (code 10): APK scanning + signature database update + **WiFi security detection** | **is**, but only uses 3 Managers (QScannerV2 / Update / WifiDetect) |
| Xiaomi OneTrack `com.xiaomi.onetrack.**` (Category 163) | Xiaomi | bury the point and report it | **is**, only 4 event names + 2 SDK automatic events |
| xCrash 2.4.9 `xcrash.**` (Category 12 + `libxcrash*.so`) | iQiyi open source | crash capture | **is**, pulled up by OneTrack reflection |
| Tencent National Secret `com.tencent.tmf.gm.**` (Category 4 + `libtmf-gm.so`) | Tencent TMF | TMSDK channel encrypted SM2/SM4 | **is** (called by `tmsdkobf.y5`) |
| `com.sun.nio.zipfs.**` (Category 12) + `META-INF/services/java.nio.file.spi.FileSystemProvider` | OpenJDK | Let Android support `jar:`/`zip:` FileSystem | **is**, `p0.e` is used to read APK within dex/manifest |
| `android.content.pm.{PackageParser,PackageUserState,VerifierInfo}` | AOSP | Parse any APK and get signatures without installation | **is**, `parseApk` IPC + get signatures from each engine |
| `dagger.hilt.**` / `androidx.room.**` / `okhttp3.internal.publicsuffix.**` / `org.apache.commons.logging.**` / `j$.util.**` / `kotlin.**` | Each family | Compilation time or compatibility residue | **No** (see §4.9) |

## 2. Component and file list

### 2.1 AVL engine (`com.avl.engine`, Class 411)

| File | class | Purpose |
|---|---|---|
| `com/avl/engine/AVLEngine.java` | `AVLEngine` | Only public facade (22 public static methods), see §4.1 |
| `com/avl/engine/AVLAppInfo.java` | `AVLAppInfo` | Scan result interface (6 getters) |
| `com/avl/engine/AVLSdkConfig.java` | `AVLSdkConfig` + `Builder` | Only 2 configurations: `deviceType`(0=LITE/1=HIGH), `logUploadUrl` |
| `com/avl/engine/AVLDebug.java` | `AVLDebug` | Log switch (`setLogEnable`) |
| `com/avl/engine/security/AVLA.java` | `AVLA extends NativeUtils` | **JNI main entrance** (40 native), the real capabilities of the engine are here |
| `com/avl/engine/security/AVLScanOption.java` | `AVLScanOption` | Scan option bit container (not used by this APK, see §4.1.3) |
| `com/avl/engine/security/FileSearcher.java` | `FileSearcher` (interface) | File traversal callback (`onSearch`/`includeSuffix`/`isStop`) |
| `com/avl/engine/security/JniFuncProcess.java` | `JniFuncProcess` (interface) | native callback `checkFuncEnable(name,int)` |
| `com/avl/engine/security/NativeUtils.java` | `NativeUtils` | Decompress/traverse/hash native (`unzipAssetsFile`, `searchFile`, `getFileMD5`…) |
| `com/avl/engine/security/c.java` | `security.c` | Engine single instance and life cycle (`init/update/stopUpdate/scanURL`) |
| `com/avl/engine/security/g.java` | `security.g extends a.c` | Initial assembly (UrlDetector, log reporter, SDK configuration) |
| `com/avl/engine/security/i.java` | `security.i` | Scan mode and device memory binning (default `f1715a=6`) |
| `com/avl/engine/security/k.java` | `security.k` | URL engine facade (`a(String)`→`scanURL`) |
| `com/avl/engine/urldetector/UrlDetector.java` | `UrlDetector` | URL detection (`libavlurl.so`) |
| `com/avl/engine/a/**` (including `a/e.java`, `a/c.java`, `a/k.java`) | engine module | Life cycle/configuration loading/decompression `assets/avlsdk`/result container adaptation |
| `com/avl/engine/b/**` | Results and updates | `b/a/a`=`AVLAppInfo` implementation; `b/b/k`=Engine signature library update manager |
| `com/avl/engine/e/a/d/{e,f,g}.java` | Scan result model | native result → mapping layer for Java structs |
| `com/avl/engine/j/**` | Report | `j/p`=Log/Exception Reporter (HTTP POST, gzip, URL from configuration) |
| `com/avl/engine/{c,d,f,h,i,k,l}/**` | internal support | Module registry, certificates/signatures, cache, features, tools |

### 2.2 TMSDK（`tmsdk` 90 + `tmsdkobf` 342）

| File | class | Purpose |
|---|---|---|
| `tmsdk/common/TMSDKContext.java` | `TMSDKContext` | SDK unique initialization entrance (v6.5.0), device information injection port, native library loading |
| `tmsdk/common/module/qscanner/QScannerManagerV2.java` | `QScannerManagerV2` | APK Scan facade (`initScanner/scanUninstalledApks/scanInstalledPackages/cancelScan/freeScanner`) |
| `tmsdk/common/module/qscanner/impl/f.java` (line 1583) | `impl.f` | Scanning implementation: local AmScannerV2 + cloud search (cmd 2016) + feature verification |
| `tmsdk/common/module/update/UpdateManager.java` + `UpdateConfig.java` | `UpdateManager` | Signature database update (`check/update/cancel`), 62 file IDs |
| `tmsdk/bg/module/wifidetect/WifiDetectManager.java` + `tmsdkobf/p9.java` (line 548) | `WifiDetectManager` | WiFi security detection (network reachable/DNS hijacking/phishing/ARP/SSLStrip) |
| `tmsdk/fg/module/urlcheck/{UrlCheckManager,UrlCheckManagerV3}.java`（+`a,b`） | URL detection | **exists but zero calls** (see §4.2.3) |
| `tmsdk/common/tcc/{TccCryptor,DeepCleanEngine,QSdcardScanner,TrafficSmsParser,VideoMetaRunner}.java` | TCC Tools | Encryption/deep cleaning/memory card scanning/text message analysis/video metadata (mostly native) |
| `tmsdk/common/module/sms_check_v4/internal/exception/*` | SMS verification | **There are only 3 exception classes left, and the main body is not packaged** |
| `tmsdk/common/{TMSService,TMServiceFactory,CallerIdent,YellowPages,SmsEntity,OfflineVideo,NumMarker}.java` | Services/Data | TMSDK resident service and data model |
| `tmsdkobf/w5.java` | `PubSharkConfig` | **All access points/domain names on Tencent side** (see §5.1) |
| `tmsdkobf/{b5,x5,u5,y9,z4,j4,f5,xb,qb,na}.java` | core | Connection management/Statistics reporting/License (`f5`)/Device fields (`xb`, `qb`)/Preferences (`na`) |

### 2.3 OneTrack (`com.xiaomi.onetrack`, Class 163)

| File | class | Purpose |
|---|---|---|
| `z/b.java` (**host side**) | `z.b` | `OneTrackManager`: unique initialization point |
| `z/c.java`, `e0/{a,b,c}.java` (host side) | bury the facade | `z.c.a(String, p...)` → `OneTrack.track` |
| `com/xiaomi/onetrack/OneTrack.java` | `OneTrack` | Public API + `Param`/`Event` constants |
| `Configuration.java` | `Configuration.Builder` | appId/channel/IMEI/IMSI/GAID/Exception capture switch |
| `util/DeviceUtil.java` (line 1184) | `DeviceUtil` | **equipment information collection summary table** (see §4.4.3) |
| `util/oaid/**` (Category 28) | OAID | Each company OEM opens anonymous device identification channel |
| `f/b.java` | public parameters | `C0041b` defines all keys for reporting JSON |
| `util/x.java` | `RegionDomainManager` | Report/Configure domain name selection |
| `c/q.java`、`d/a.java`、`d/b.java` | encoding | gzip + AES/ECB/PKCS5 + RSA certificate encryption |
| `c/c.java`、`b/g.java`、`a/a.java` | Queue | SQLite drop queue (events/events_cloud/monitor) |
| `c/m.java` | Scheduling | Failure retry and backoff |
| `CrashAnalysis.java` | collapse | Reflect to launch xCrash and read the `.xcrash` file report |

### 2.4 xCrash / National Secret / Framework Class

| File | class | Purpose |
|---|---|---|
| `xcrash/XCrash.java`, `NativeHandler.java`, `h.java`(JavaCrash), `d.java`(FileManager), `j.java`(tombstone text) | xCrash 2.4.9 | Crash capture with tombstone placement |
| `com/tencent/tmf/gm/{a,Sm2,Sm4,b}.java` | State secrets | SM2/SM4 (JNI `libtmf-gm.so`) + hardcoded SM4 key |
| `com/tencent/tcuser/util/{a,Encode}.java` | Tencent tools | hex/digital conversion (live); `Encode` depends on **missing `libxy.so`** (dead) |
| `com/tencent/turingfd/sdk/vd/TNative$aa.java` | Turing risk control | Only the native bridge (`libturingvd.so`) is left, **Java side has zero reference to** |
| `com/sun/nio/zipfs/**` (Category 12) + `unknown/META-INF/services/java.nio.file.spi.FileSystemProvider` | OpenJDK zipfs | Support `jar:` FileSystem |
| `android/content/pm/{PackageParser,PackageUserState,VerifierInfo}` | AOSP | Offline APK parsing and signature collection |

## 3. Data flow/timing (call points are marked segment by segment)

### 3.1 File scanning (synchronous scanning IPC → engine)

```
SecurityService(antivirusservice) / GuardProvider.call
  └ f0.b registry: MI(Mi,100)/AVL(AVL,11)/TENCENT(TENCENT,10) f0/b.java:14-16
     ├ AvlEngine.j(List, listener, cloud, engName)                 engine/antiy/AvlEngine.java:137
     │   ├ s(b())  → AVLEngine.setNetworkEnabled(cloud)            :143 / :212
     │   ├ r(z6)   → AVLEngine.setCloudScanEnabled(cloud)          :144 / :79
     │   └ Each path: AVLEngine.scan(path) → AVLAppInfo :159
     │        └ security.c.a().b(6, path) → a.g.b(...) → AVLA.scan : security/c.java:111
     └ TencentEngine.j(...)                                        engine/tencent/TencentEngine.java:438
         ├ ManagerCreatorF.getManager(QScannerManagerV2.class)     :445
         ├ initScanner()                                          :452
         ├ scanType = (cloud && z6) ? 6 : 2                        :459   // 6 = SCAN_LOCAL|SCAN_CLOUD
         └ QScannerManagerV2.scanUninstallApks(scanType, [path], d, 0)  :472
              └ impl.f.a(...)                                     qscanner/impl/f.java:683
                   ├ Local: AmScannerV2 (libTmsdk): 645
                   └ Cloud: x5.a(2016=Cmd_CSVirusCheck, …):1310
```

### 3.2 URL detection (`urlscan` service)

```
SecurityService action=…action.urlscan                          manager/SecurityService.java:315
  └ n0.j.b(url, pkg)                                            n0/j.java:87
      └ engine.m(1, url)                                        engine/antiy/AvlEngine.java:195
          └ AVLEngine.scanURL(url)                              AVLEngine.java:97
              └ security.k.a(url) → UrlDetector.b(url)           security/k.java:104
                  └ native matchURLEx(url,false)                 UrlDetector.java:183
```
**[Observation]** return value `0/1/2/-1` is exposed as is from `UrlDetector.b` (`UrlDetector.java:201-207`), and is directly returned to the AIDL caller via `n0.j` and `SecurityService`; **Java layer has no response to 0/1/2 The semantic enumeration**. **[Inferred]** combines the branch structure (0 returns directly, 1, 2 separate columns) and AVL public document convention: 0=safe, 1=risk, 2=phishing/malicious; **[unknown]** has not been confirmed by the native side.

### 3.3 Feature library/engine update

```
f0.a.l(listener) (engine abstraction)
├ AvlEngine.l(): checkUpdate() → update()                          AvlEngine.java:174/182
│    └ security.c.b()/c() → a.g.e()/f() → b.b.k.a()               security/c.java:52-61
└ TencentEngine.l(): UpdateManager.check(UPDATE_FLAG_VIRUS_BASE)   TencentEngine.java:493
     └ libTmsdk → https://mmgr.gtimg.com/qqsecure_config_update/…  update/a.java:675
```

### 3.4 OneTrack Buried Point → Report

```
Business point z.c.f12069a.a("event", p...) MiEngineNG.java:156/201
  └ z.c$a.a → e0.c.a → z.b.b(event,map) → OneTrack.track          z/c.java:36, e0/c.java:22, z/b.java:47
      └ api.h → public parameters f/b.a(...) → c.i(queue) → c/c.java:85 events table
          └ c/q: JSON→gzip→AES/ECB/PKCS5                           c/q.java:120-147, d/a.java:42
              └ POST https://tracking.miui.com/track/v4            util/x.java:35/59/129
```
Abnormal branch: `OneTrack.createInstance` → `api/h.b()` → `CrashAnalysis.start` → Reflection `XCrash.init` → Read `filesDir/tombstones/*.java.xcrash` → `onetrack_bug_report` (`api/c.java:84`).

## 4. Analysis of key logic one by one

> Each format: **position | observation | inference | unknown**. For "actually enabled" conclusions, the host-side call point is given; for "only claimed" conclusions, the API existence source is given and zero calls are declared.

### 4.1 Tencent AVL engine (`com.avl.engine`)

#### 4.1.1 `AVLEngine` public API full table (22 methods, all public static)

| method | OK | Semantics | Whether this APK is called |
|---|---|---|---|
| `init(Context, AVLSdkConfig): int` | `AVLEngine.java:81` | Initialization (decompress assets, load `libavlm.so`, verify appkey/token) | **is** `AvlEngine.java:99` |
| `scan(String): AVLAppInfo` | `:140` | Scan a single path (internally go `i.a()`=6) | **is** `AvlEngine.java:159` |
| `scan(Context, String): AVLAppInfo` | `:89` | Same as above (explicit context, not used) | No |
| `scanURL(String): int` | `:97` | URL detection, return 0/1/2/-1 | **is** `AvlEngine.java:197` |
| `checkUpdate(): int` | `:25` | Check signature database update (`-4` no network/`-5` uninitialized/`-1..-3` mapping) | **is** `AvlEngine.java:174` |
| `update(): int` | `:125` | Perform updates, >0 indicates there are updates | **is** `AvlEngine.java:182` |
| `stopUpdate(): int` | `:121` | Abort update | **is** `AvlEngine.java:167` |
| `updateURLRule(): int` | `:132` | Update URL rule base | **is** `AvlEngine.java:202` |
| `getEngineVersion()` | `:53` | Engine version | Indirect (`d()` record) |
| `getVirusDatabaseVersion()` | `:77` | Virus database version | No |
| `getURLDatabaseVersion()` | `:65` | URL library version; host writes SP `avl_url_sdk_version` | **is** `AvlEngine.java:206-207` |
| `getURLEngineVersion()` | `:69` | URL engine version | No |
| `getUpdateProgress()` | `:73` | update progress | No |
| `setCloudScanEnabled/getCloudScanEnabled` | `:101/:49` | Cloud check switch | **is** `AvlEngine.java:79` |
| `setNetworkEnabled/getNetworkEnabled` | `:113/:57` | SDK network main switch | **is** `AvlEngine.java:212` |
| `setRegion(String)` | `:117` | Area (`miui.os.Build.getRegion()`) | **is** `AvlEngine.java:83` |
| `setLogUploadEnabled/isLogUploadEnabled` | `:109/:85` | Log reporting switch | No |
| `setLanguage(String)` | `:105` | `zh`/`en` | No |
| `getSDKVersion()` | `:61` | Static block `a.b("4.0.1")` | No |

**[observation]** `static { a.b("4.0.1"); }` (`AVLEngine.java:18-20`) → SDK version 4.0.1; `security/g.java:75` and `a()` return `"4.0.0"` (module-level version, both coexist).

#### 4.1.2 `AVLScanOption`: Option bit/enumeration meaning (SDK claims, **APK zero uses**)

| Field | Default value | Semantics (according to method name/usage) | Source |
|---|---|---|---|
| `f1675d` / `getScanMode()` | `0` | Scan mode (1st int of native `scan(String,int[])`) | `AVLScanOption.java:65`, `:96` |
| `f1676e` / `getScanCategoryOption()` | `32767`(0x7FFF) | **scanning category bit mask**: 15 bits fully open, corresponding to installation package/permissions/advertising/privacy and other categories (specific bits are defined in native) | `:28`, `:57` |
| `f1677f` / `getScanOutputOption()` | `0` | Output options (return content cropped) | `:69`, `:92` |
| `f1678g` / `getScanLogOption()` | `2` | Log level; with static deduplication (`f1673b`/`f1674c` ensures that the same value is only delivered once) | `:31`, `:33-43` |
| `f1679h` / `getScanRange()` | `0` | Scan range | `:73` |
| `f1680i` / `b()` | `0` | timeout/time limit (long) | `:45`, `:100` |
| `c()` | — | Use mode+category+output to spell hexadecimal and then MD5(32) as the native option cache key | `:50` |

**[Observation]** grep `AVLScanOption`: 0 hits except `com/avl`; `AVLEngine.scan` only passes `i.a()` (default 6) and path, **does not construct any AVLScanOption**. **[Inference]** The category/output/log bits of this class are all set to native default values ​​in this APK, and the host cannot tune them; GuardProvider only cares about "whether it is a hit" and does not care about the breakdown of hit categories.

#### 4.1.3 Scanning result model (hit type/virus name/advertising type)

**[Observation]** interface 6 getters: `getAppName/getDangerLevel/getPackageName/getPath/getVirusDescription/getVirusName` (`AVLAppInfo.java:5-15`).
**[Observation]** implementation chain `com.avl.engine.b.a.a` (`AVLAppInfo`) → `com.avl.engine.a.k` → `com.avl.engine.e.a.d.f`+`g`:

| Field | Value source | Semantics |
|---|---|---|
| `appName` | `f.c().c()` | Application name |
| `packageName` | `f.c().d()` | package name |
| `path` | `f.c().b()` | file path |
| `virusName` | `g.a()`, if equal to `"White"`, leave it blank | Virus name (`"Risk/..."`, `"Trojan*"`, `"G-Ware*"`, `"PornWare*"`, `"RiskWare*"`, `"Tool*"`) |
| `virusDescription` | `g.d()` | Description |
| `dangerLevel` | `g.f()` | Danger level (int, transparent transmission) |
| (Internal)`f767k` | Prefix determination: `Risk/`→2; `Trojan/G-Ware/PornWare/RiskWare/Tool`→1; otherwise 0 | **Local threat classification** (unpublished) |

Source: `com/avl/engine/a/k.java:42-72` (especially `:55` whitelist filtering, `:65-69` prefix mapping), `com/avl/engine/e/a/d/f.java:192-245` (`i()` selects `g.d`/`g.b` result parser according to `b`/`g`).
**[Observation]** host is mapped one layer more: `AvlEngine.p()` puts `dangerLevel` 1→2, 2→3, `virusName` prefix `risk/` → 6 (`AvlEngine.java:63-76`); finally `f.f11671p = "AVL:danger;"`/`"AVL:safe;"` (`:44/:46/:58`).
**[Observation]** advertising/porn category does not come from level, but from **feature package file name**: `assets/avlsdk` contains `avlpk_pornware.avl`(322 KB), `avlpk_apn.avl`(131 KB), `avlpk_amc.avl`, `avlpk_sfmf.avl`, `avlpk_sgnl.avl`, `avlpk_kw.avl`, `avlpk_dhc.avl`, `avlpk_des.avl`. **[Inference]** `pornware`=porn, `apn`=adware plugin, `amc`=malicious deductions/hidden deductions, `sfmf`/`sgnl`=signature/certificate rules, `kw`=keyword rules (matching `av/kw/common/{zh,en}/*.des` Copywriting library); the specific abbreviation is not self-certified by the code → **[Unknown]**.

#### 4.1.4 `UrlDetector`: Ability and Judgment Input and Output

| item | content | Source |
|---|---|---|
| Load | `System.loadLibrary("urldetectorsys")` + `setLibPath("libavlurl.so")` (or configuration path `.../avlurl`) | `UrlDetector.java:80`, `:87` |
| initialization | `loadURL(urlDatePath)`, returns 0/1/2, `-1` fails; after success, `f1728d=true` | `:98-113` |
| Judgment | `matchURLEx(url,false): String[]`, `[0]`=result code, `[1]`=hit details (give to `urldetector.a.b.b()`) | `:183-197` |
| output | `0` security / `1` / `2` / `-1` failed (see `:201-207` for branches) | Tongzuo |
| input | Only URL string (**does not upload URL**, pure local library matching) | `:174-213` |
| Rule base | `assets/avlsdk` within `url/url/{fish_url.avl 417 KB, white.avl 370 KB, fish.avl 150 KB, fish_re.avl, white_re.avl}` + `avlurl` directory | zip list |
| version | `url.siglib=20180917.zz`, `url.engine=124` (`sdk_conf/version.conf`); `avllib.conf` has the same value | files within assets |
| Log directory | `setLogPath(<sdkDir>/avlurl)` | `:234` |

**[Observation]** `UrlDetector` also comes with native group: `matchURL/matchSpUrl/matchUrlForTag(Ex)/updateURL/setNetworkEnabled/setLogOpt/checkAPPToken` (`:124-168`) - that is, SDK has the capabilities of "URL tagging", "matching by tag" and "online update rules".
**[Unknown]** The detailed structure of `matchURLEx[1]` (no longer visible after being consumed by `urldetector.a.b.b()`).

#### 4.1.5 Appkey, package name, channel, and license loading in `AVLSdkConfig` / `AVLAppInfo`

| item | Loading method | Source |
|---|---|---|
| appkey | Read `AVL_OPENSDK_APPKEY` from **itself** `ApplicationInfo.metaData`, if missing, fall back to `AVL_SDK_APPKEY`; if empty, log `Failed to get appKey from AndroidManifest.xml!` | `com/avl/engine/a/e.java:36-47` |
| appkey actual value | `D5DFB878335A3EF4E76F60ED470B8EC8` | `AndroidManifest.xml` meta-data (briefing confirmed) |
| package name | `k.l().getPackageName()` (`com.miui.guardprovider`), used for token calculation | `com/avl/engine/a/c.java:170` |
| channel | Configuration item `av.channel` (`e.b()` returns `f752c.a("av.channel")`) | `com/avl/engine/a/e.java:90-92` |
| license | `assets/avlsdk` contains `av/avl/conf/liscense.conf` (404 B) and `url/url/conf/liscense.conf` (404 B), which are decompressed to `<sdkDir>/avl` and `<sdkDir>/avlurl` during runtime. | `a/c.java:195-201` |
| token | `AVLA.computeToken(packageCodePath, packageName, appkey)` after `checkSdkKey` | `a/c.java:170`, `a/t.java:22` |
| Equipment gear | `AVLSdkConfig.Builder().setDeviceType(c5.a.a()?0:1)` (0=LITE/1=HIGH; `c5.a.a()` indicates low memory) | `AvlEngine.java:99`、`AVLSdkConfig.java:5-6`、`security/i.java:46-75` |
| Log report URL | `setLogUploadUrl` **has not been set by the host** (Builder only linked setDeviceType) | `AvlEngine.java:99` |

#### 4.1.6 AVL: Claimed vs Actual

| Ability | SDK claims (API/native exists) | This APK actual |
|---|---|---|
| Local APK static scan (multi-engine: `scan/scanEx/sceneScan/officialScan/pcit/analyseAppByEngine`) | `AVLA.java:89-101` | **enables `scan`**; `scanEx/sceneScan/officialScan` zero call |
| Scan quota/downgrade (`setScanLimit/setDEXScanLimit/setOpcDEXScanLimit/setMapleScanLimit/setScanLimitEx`) | `AVLA.java:29-39,105`; packaged in `AVLA.a(f)` | **does not enable** (the host does not construct `f`) |
| Cloud query/time gate (`setAIES/setAIER`) | `AVLA.java:29-31`、`AVLA.a(e)` | Not enabled |
| URL detection (local library) | `scanURL` + `UrlDetector` | **enables** |
| URL rules updated online | `updateURLRule` | **enables** (gated by `getNetworkEnabled()`) |
| Engine/virus database online update | `checkUpdate/update/stopUpdate` + `b/b/k` | **enables** |
| Log/Exception reporting | `j/p`（HTTP POST+gzip）+ `setLogUploadEnabled` | **does not enable** (`logUploadUrl=null`; `AVLDebug.setLogEnable(false)` in `AvlEngine.java:101`) |
| Application information/certificate analysis | `getAPKInfo/getCertHash/getCertFullHash/signBlockHash/keySetHash` | Not called directly (the host uses the built-in `PackageParser` instead, see §4.7) |
| Dynamic/syscall level detection | `JniFuncProcess` callback + `sjsp` | Not enabled (no callbacks implemented) |
| Unzip/self-test | `NativeUtils.unzipAvlsdk/unzipAssetsFile/searchFile` | Used internally by initializer |

**[Inference] The "cloud search" of** AVL in this APK is only reflected as `setCloudScanEnabled` a Boolean (`AvlEngine.java:79`). The real cloud interaction relies on `AVLSdkConfig.logUploadUrl` and the configuration-driven `j/p` base URL; the host does not provide either → **AVL side has no own domain name and no URL/file outgoing** (see §5.4).

### 4.2 Tencent TMSDK (`tmsdk` + `tmsdkobf`)

#### 4.2.1 Capability list (public module → one sentence)

| module | Entry class | Ability | Source |
|---|---|---|---|
| APK scan | `QScannerManagerV2` | Installed/not installed APK scan (local AmScannerV2 + cloud search cmd 2016 + feature verification) | `qscanner/QScannerManagerV2.java:50-60` |
| Feature database update | `UpdateManager` | 62 file IDs (virus database/whitelist/certificate MD5, etc.) download and verification | `update/UpdateConfig.java:311` |
| WiFi security | `WifiDetectManager` | Network reachability (urlApprove), DNS hijacking/phishing (cloud), ARP spoofing (no root), WiFi encryption type, SSLStrip, batch phishing | `wifidetect/WifiDetectManager.java:36-82` |
| URL detection | `UrlCheckManager`/`V3` | Synchronous/asynchronous URL cloud query (cmd level 290/291) | `urlcheck/UrlCheckManager.java:15-31` |
| deep cleaning | `DeepCleanEngine` | native rule matching directory/file (`isMatchFile/isMatchPath/findMatchDir`) | `tcc/DeepCleanEngine.java:5-20` |
| Memory card scan | `QSdcardScanner` | Traverse `/storage/emulated/legacy`, `/storage_int`, `/HWUserData` by suffix to find APK | `qscanner/impl/f.java:745-765` |
| SMS verification | `sms_check_v4` | **has only 3 exception classes** (`DatException/EncryptionException/ModelException`), and the main body is missing | directory listing |
| Traffic SMS analysis | `TrafficSmsParser` | Operator traffic SMS identification (`getNumberEntrance/getWrongSmsType`) | `tcc/TrafficSmsParser.java` |
| Video metadata | `VideoMetaRunner` | Only `main()` (command line tool class) | `tcc/VideoMetaRunner.java` |
| number mark | `TelNumberLocator`/`NumMarker`/`YellowPages` | Number ownership/yellow pages | `bg/tcc/TelNumberLocator.java` |
| Encryption | `TccCryptor` | Symmetric encryption and decryption + `makePassword` (native) | `tcc/TccCryptor.java:6-27` |
| Connect/Report | `PubSharkConfig`/`x5`/`y9`/`z4` | Long connection, statistical management, channel reporting | `tmsdkobf/w5.java`、`x5.java`、`y9.java`、`z4.java` |
| permission | `f5`/`TMSDKContext.checkLisence` | Read `assets/licence.conf`/`licence1.conf`, expire `isExpired` | `tmsdkobf/f5.java:101-150`、`TMSDKContext.java:51-53` |

#### 4.2.2 Live code: GuardProvider only uses 3 Managers (evidence one by one)

| Ability | call point | Observed |
|---|---|---|
| SDK initialization | `TencentEngine.java:403-408` | `TMSDKContext.init(context, null, cfg)`; `cls=null` → **does not start `TMSService`** (the `startService` branch of `TMSDKContext.java:140-146` is skipped) |
| Device information injection | `TencentEngine.java:402` | `TMSDKContext.setDeviceInfo(ctx, Settings.Secure.getString(cr,"android_id"), Build.MODEL)` → `TMSDKContext.java:188-207` Write `xb.f10474a`(androidId) and `qb.f9915a`(model) |
| Automatic connection/channel reporting | `TencentEngine.java:507` (`v(z)`) + `:400/:456/:487` call | `TMSDKContext.setAutoConnectionSwitch(cloud)` → `y9.f(29987/29988)`; `true` then `reportChannelInfo()` → `z4.a()` (`TMSDKContext.java:173-182`), `z4` to `"reportlc"` (`z4.java:39-43`) |
| APK scan | `TencentEngine.java:445/452/472` | `ManagerCreatorF.getManager(QScannerManagerV2.class)` → `initScanner()` → `scanUninstallApks(scanType,[path],listener,0)` |
| Scan type | `TencentEngine.java:459` | `(cloud && z6) ? 6 : 2` → Bit semantics `2=SCAN_LOCAL`, `4=SCAN_CLOUD` (`qscanner/impl/f.java:723-738`), 6=local+cloud |
| Cloud check protocol | `qscanner/impl/f.java:1304-1310` | `x5.a(2016 /*Cmd_CSVirusCheck*/, request body, …, SHARK_HTTP, …)`, the request body contains `l1` fields such as package name/version/certificate (`:1294-1303`) |
| Feature database update | `TencentEngine.java:489-493` | `ManagerCreatorC.getManager(UpdateManager.class)` → `check(UpdateConfig.UPDATE_FLAG_VIRUS_BASE, listener, -1)` |
| WiFi detection | `TencentEngine.java:84,94-122` | `ManagerCreatorB.getManager(WifiDetectManager.class)` → `init()` → `detectNetworkState()` / `detectDnsAndPhishing(listener,0)` / `detectARPNoRoot(15000)` / `detectSecurity(scanResult)`; `free()` to `:77` |
| WiFi self-collection (host side) | `TencentEngine.java:107-114` | The host owns `getConnectionInfo()`+`getScanResults()`, matches it with BSSID and hands `ScanResult` to SDK |
| Cancel | `TencentEngine.java:393` / `:480` | `QScannerManagerV2.cancelScan()` / `UpdateManager.cancel()` |

**[Observation]** `TencentEngine.java:122` incoming timeout `0L`, SDK side corrects `<2000` to `180000` (`WifiDetectManager.java:50-52`).
**[Observation]** scan result mapping: `scanResult` 258→5, 259/261/262→3, 260→2, the rest→1 (`TencentEngine.java:352-365`), the host then calls `"TENCENT:danger;"`/`"TENCENT:safe;"` (`:380-385`).

#### 4.2.3 The ability to exist but have zero calls (exhaustive grep evidence)

grep range: `_analysis/src/sources` whole tree, excluding `tmsdk/` and `tmsdkobf/` themselves; the number of hits is 0:

| symbol | external hit | Conclusion |
|---|---|---|
| `UrlCheckManager` | 0 | URL cloud check **does not enable** at all (GuardProvider's URL detection only checks AVL, `n0/j.java:58` matches by engine name) |
| `TccCryptor` | 0 | TMSDK Encryption and decryption are not enabled |
| `DeepCleanEngine` | 0 | Deep cleaning is not enabled |
| `QSdcardScanner` | 0 (`tmsdk` only contains `qscanner/impl/f.java:751` internally, and is only triggered when the caller passes `list==null`) | Host constant leaflet element list (`TencentEngine.java:470-472`) → **does not trigger full traversal of** |
| `TrafficSmsParser` / `VideoMetaRunner` / `sms_check` | 0 / 0 / 0 | SMS, video, and SMS verification modules are not enabled; `sms_check_v4` and the main body is missing |
| `TelNumberLocator` / `NumMarker` | 0 | Number ownership is not enabled |
| `TMSDKContext.startPersistentLink` | 0 | The long connection is not actively pulled up (but the connection management will still establish the link after `setAutoConnectionSwitch(true)`) |

**[Inference]** TMSDK In this APK, "**is cropped to only scan + WiFi + update**"; `sms_check_v4` only has exception classes, `libxy.so` (`tcuser.util.Encode` depends on it) is not there `lib/arm64-v8a`, indicating that the packaging party has done module cutting once.

#### 4.2.4 TMSDK’s networking and privacy (link)

| Collection items | collection point | Where to go | Trigger condition |
|---|---|---|---|
| `ANDROID_ID` + `Build.MODEL` | `TencentEngine.java:402` → `TMSDKContext.java:200-206`（`xb.f10474a`/`qb.f9915a`） | Enter TMSDK request header/report body (`x5`/`y9` channel) | `TENCENT` engine `e()` initialization (before first scan/update) |
| WiFi `SSID`/`BSSID`/`DNS1`/`DNS2` | `tmsdkobf/p9.java:210-238` | Cloud check cmd **794** (`:254`), go `x5` → `https://tmfsdk.m.qq.com` / TCP `:8001/:8002` | Host `n(DNSANDPHISHING)` → `TencentEngine.java:122` |
| Current WiFi Encryption Type/ARP Gateway | `p9.java:190`（`q9.a(bssid,timeout)`）、`WifiDetectManager.detectSecurity` | **only determines** locally, and only performs statistical management when exceptions occur (`y9.c(1320068,…)`) | `SECURITY`/`ARP` type request |
| List of installed apps | `qscanner/impl/f.java:434` (`getInstalledApplications(0)`, excludes itself) | Cloud search request body (`l1` sequence) | **is only triggered when the list of** `scanUninstalledApks` is empty; this APK is not triggered |
| Network type/SIM status | `tmsdkobf/xb.java:84-86`、`y3.java:178` | Request context (`xb.e()`) | Any internet request |
| Scan the package name/version/certificate summary of the target APK | `qscanner/impl/f.java:1294-1303` | Cloud check cmd 2016 | `scan` every time when cloud is turned on |
| root detection | native (`libTmsdk-2.0.13-mfr.so`), see §5.6 for the su path collection | Local determination, no upload (no Java call point) | Scan/initialize internal |

### 4.3 `com.tencent`: National Secret/Tools/Turing

| class | Observed | Inferred |
|---|---|---|
| `com.tencent.tmf.gm.a` | `static { System.loadLibrary("tmf-gm"); }` (`:25-30`); three error codes 1/2/3 | The national secret is **JNI, which implements** and is not pure Java. |
| `com.tencent.tmf.gm.Sm2` | `doSm2Encrypt2(pubX[32], pubY[32], data, AtomicReference)` native (`:28`); pre-check 32/32 bytes | The public key is **64 bytes X||Y** (`b.a()` then disassembled into 32+32, `b.java:48-52`) |
| `com.tencent.tmf.gm.Sm4` | `doSm4DecryptECB/doSm4EncryptECB` native (`:49-51`), ECB mode | SM4 No IV, ECB |
| `com.tencent.tmf.gm.b` | **hard-coded 16-byte SM4 key** `{115,66,90,85,24,8,6,92,118,112,33,112,36,36,51,41}` (`:7`); `a()/b()/c()` is used as the default key (`:17-18,:31-32`); `a(data,pub64)` requires a public key length of 64 (`:45-46`) | The session key is not implemented, and the default key of **is the constant** → Decompile to decrypt the SM4 segment of TMSDK |
| Key source (SM2 public key) | `tmsdkobf/j7.java:157-164`: `i6==0` uses `y7.a(h9.a(str),0)`, `i6==1` requires `str.length()==128` otherwise throws `"SM2 public key must be 128 chars!"` | The public key **is configured by the server to issue** (128 hex), which is not a built-in constant. |
| caller | `tmsdkobf/y5.java:6-17` (implementation `x3`: `a/b/c` → `gm.b.c/b/a`) | National Secret Shark Channel Payload for TMSDK |
| `com.tencent.tcuser.util.a` | hex→byte (`:33-45`), `Byte/Integer` analysis (`:9-31`) | Pure tool; used by `qscanner/impl/c.java:114,123`, `tmsdkobf/{ba,ca,gb,v2}.java` |
| `com.tencent.tcuser.util.Encode` | `System.loadLibrary("xy")` (`:43`); `nativePs(List,List<ProcessInfo>)` Read **process list** (`:57`); `native x/y(Context,byte[])` (`:71-73`) | **`libxy.so` is not in `lib/arm64-v8a`** → `UnsatisfiedLinkError` once called; and there is no call point in the whole tree → **dead code** |
| `com.tencent.turingfd.sdk.vd.TNative$aa` | 9 native, all `SparseArray<Object>` in and out (`:22-38`); embedded `bb implements ServiceConnection` and `onServiceConnected` are native (`:13-20`) | All the logic of Turing risk control is in `libturingvd.so`; **Java side zero reference** (grep `turingfd` no host hit) → whether the runtime is natively pulled up **[unknown]**; collection items cannot be determined from Java **[Unknown]** |
| `com.tencent.tmsecure.R` | 28 KB resource ID constant table | Tencent code migration residue (`com.tencent.tmsecure.demo` and other package name residues, see §4.8) |

### 4.4 Xiaomi OneTrack (`com.xiaomi.onetrack`)

#### 4.4.1 Initialization and key configuration

**[Observation]** unique initialization point `z/b.java:21-30` (REA `ev_4c3c9e94…` verified):

```java
new Configuration.Builder()
  .setAppId("2882303761517522561")          // z/b.java:22
  .setChannel(GuardApplication.f1743d.getPackageName())   // Channel = "com.miui.guardprovider"
  .setExceptionCatcherEnable(true)          // → Pull up xCrash
  .setMode(OneTrack.Mode.APP)
  .setAutoTrackActivityAction(true)         // This APK has no activity → no effect
  .setGAIDEnable(false)                     // Disable "recommended events" use of the Google Advertising ID
  .build();
OneTrack.createInstance(GuardApplication.f1743d, configurationBuild);  // :26
```
- **is not set** `setImeiEnable/setImsiEnable/setRegion/setInternational/setInstanceId/setUseCustomPrivacyPolicy` → Default: `IMEIEnable=true`, `IMSIEnable=true`, `GAIDenable (default true, but explicitly false)`, `international=false` (`Configuration.java:269-291`).
- Network gating: `z/b.java:39-42` → `OneTrack.setAccessNetworkEnable(ctx, GuardApplication.e(ctx))`; `GuardApplication.e()` requires `persist.sys.sc_allow_conn=true` **and** has `com.miui.securitycenter` installed (international version `jp_kd` also requires `Settings.Secure.antivirus_engine_network_enable==1`) (`GuardApplication.java:38-48`). **[Inference] On** non-MIUI devices, OneTrack only places orders but does not report.
- **does not call**: `registerCrashHook` (but `api/h.java:170-171` automatically calls `CrashAnalysis.start` when `Mode.APP && isExceptionCatcherEnable`), `login/logout/setUserProfile/adTrack/trackPluginEvent`.

#### 4.4.2 Full list of event names actually called by APK (including dynamic splicing sources)

| event name | Trigger condition | Attribute (key=value source) | Source |
|---|---|---|---|
| `MiEngineNG_Scan` | MI engine scan ends | `scan_count`=list.size(); `scan_time`=Time consuming (ms) | `MiEngineNG.java:156` |
| `threats` | The MI engine result is "hit threat but not gray production" (`t0.o` only) | `packageName`,`versionCode`,`versionName`,`mfSha256`,`signaturesMd5`,`signaturesSha256`,`installer`,`threatName` (=threat list join ",",`MiEngineNG.java:192,195`) | `MiEngineNG.java:196,201` |
| `blackgrays` | The result is a gray product but not a threat (`t0.f` only) | Same as above 8 fields, `threatName=null` | `:199,201` |
| `blackgrays_threats` | At the same time, it is a gray product and a hit threat. | Same as above for 8 fields, `threatName` is a gray list join | `:193,201` |
| `onetrack_dau` (SDK automatic) | First batch of events per session | `first_open`, `imeis` (IMEI list), `imsis` (IMSI list), `config_status` | `api/c.java:99-119` |
| `onetrack_bug_report` (SDK automatic) | Read `.xcrash` crash file | `exception` (abstract), `type`, `message`, `feature`, `crashtime` | `api/c.java:82-97` + `CrashAnalysis.java:158-178` |

**[Observation] There is no constant table for the** event name. `threats/blackgrays/blackgrays_threats` is assigned a literal value (`MiEngineNG.java:190-201`) from the `str` variable according to the `instanceof` branch. **is not dynamically spliced into**; the only splicing is in `threatName` (`y.c0(list, ",", …)`).
**[Observation]** `OneTrack.Event`/`OneTrack.Param` The 19 event names (`VIEW/LIKE/LOGIN…`) defined in this APK **zero call** (`z/b.java:47` is the only `track` Entry, caller only `MiEngineNG`).
**[Inference]** Since `z.b.a()` is lazily loaded by DI (`e0/a.java:14-18`, `e0/b.java:8`), OneTrack is only initialized when **’s first MI engine scan results** → no reporting or collection during cold start.

#### 4.4.3 Complete table of collection fields (`DeviceUtil` / `util.oaid.*`)

| Field | Collection implementation | Source | Whether to enter the report |
|---|---|---|---|
| IMEI (Multiple cards) | `TelephonyManagerEx.getImeiList()` takes priority; falls back to `TelephonyManager.getImei(slot)`; then falls back to `getDeviceId()` | `DeviceUtil.java:296-341,343-368,1046-1060` | Is: `imei`=MD5 (`f/b.java:304`, `util/g.java:45`) of first IMEI; `imeis`=IMEI **plain text list** (`api/c.java:111`, only DAU event) |
| MEID | `TelephonyManagerEx.getMeidList()` / `TelephonyManager.getMeid()` | `:370-401` | Indirect (`s()` output MD5 splicing) |
| IMSI (dual SIM card) | `getSubscriberId()` / `SubscriptionManager.getSubId` | `:659-702,1124-1165` | Is: `imsis`=IMSI list MD5 (`api/c.java:114`) |
| SN | `Build.getSerial()` reflection | `:704-721` | Yes: The `sn` field has been defined (`f/b.java:139`), and this path has not been assigned a value → **[Unknown]** |
| OAID | `util/oaid/a.a().a(ctx)` (OEM channel: vivo/nubia/meizu/honor/asus/oppo, etc.) | `:761-763`、`util/oaid/**` | Yes: `oaid` (`f/b.java:305`), writeBack parameter (`util/g.java:46`) |
| AAID | `content://com.miui.analytics.server.AnalyticsProvider/aaid` → Fall back `MiuiSettings$Ad.getAaid` | `:785-806` | Yes (for advertising monitoring, this APK does not have adTrack → only configure the chain) |
| GAID | Binding `com.google.android.gms.ads.identifier.service.START` (Parcel transact 1/2) | `:127-287` | Only the international version of "Recommended Events" (`f/b.java:298`); this APK `GAIDEnable=false` → **does not adopt** |
| AndroidID | By SDK external incoming/internal reading (`f/b.java:145` definition key) | `f/b.java:145` | The key exists, but no value is found in this path → **[Unknown]** |
| MAC | MiTV exclusive: `Settings.Global "mi_device_mac"` → `/sys/class/net/{wlan0,eth0}/address` → `ro.boot.btmac` → `persist.service.bdroid.bdaddr` | `:842-870` | No (only model branch) |
| Model | `Build.MODEL` / `ro.product.model.real` / `ro.product.marketname` | `:998-1006,1074-1076,1100-1102` | Yes: `model`, `market_name` (`f/b.java:313,338`) |
| Manufacturer | `Build.MANUFACTURER` | `:1078-1080` | Yes: `mfrs` (`f/b.java:312`) |
| Equipment/Products | `ro.product.mod_device`→`Build.DEVICE`；`ro.product.name`；`ro.build.description` | `:1008-1020,1167-1169` | Part (`build`) |
| System/MIUI version | `q.d()/q.c()/q.e()` | `f/b.java:315-317` | Yes: `miui`/`build`/`os_ver` |
| Network type/operator | `g.c.a(ctx).toString()` (NetType enumeration, including 2G/3G/4G/5G/WIFI/ETHERNET) | `f/b.java:321`、`OneTrack.NetType` | Yes: `net` |
| time zone/region | `q.b()`、`q.i()`、`q.f()` | `f/b.java:320-322` | Yes: `tz`/`region`/`sid` |
| screen resolution | `getRealSize` | `:765-770` | Not entered this APK event |
| storage capacity | `StatFs` External storage total/free | `:1031-1044` | Not entered this APK event |
| Equipment form | Pad/TV/Phone（`miui.os.Build.IS_TABLET`、`FeatureParser is_pad`、`uiMode`） | `:739-759,1082-1094` | Not entered this APK event |
| Installed apps list | **does not contain** (`DeviceUtil` does not contain `getInstalledPackages`) | grep 0 hits | No |
| clipboard | **no** (`ClipboardManager` 0 hits) | grep 0 hits | No |
| location/base station | **no** (`getLastKnownLocation/getCellLocation` 0 hits) | grep 0 hits | No |
| Contacts/SMS | **no** (`ContactsContract`/`content://sms` 0 hits) | grep 0 hits | No |

**[Observation]** `Configuration.isIMEIEnable()=true` and `isIMSIEnable()=true`, and the gate control of `api/c.java:109` is "`q.a() ? q.h() : isInternational()`" - **domestic MIUI goes `q.h()`**, if true Then **does not use IMEI/IMSI**. **[Unknown] The runtime value of** `q.h()` (depending on the MIUI area switch) is not solidified in the source code constants.

#### 4.4.4 Reporting protocol (batch/encryption/drop queue/retry/domain name)

| Dimensions | Observed | Source |
|---|---|---|
| domain name | Domestic `https://tracking.miui.com`; International `tracking.intl.miui.com`; India `tracking.india.miui.com`; Russia `tracking.rus.miui.com` (can be overridden by remote configuration, save `aa.b`) | `util/x.java:35-56,145-147,150-166` |
| Path | Event `…/track/v4`; get key `…/track/key_get`; configure `https://sdkconfig.ad.{xiaomi,intl.xiaomi,india.xiaomi,rus.xiaomi}.com/api/v4/detail/config` and `…/config_common` | `util/x.java:59-68,121-142` |
| Advertising Monitoring Writeback | `https://<host>/api/open/device/writeBack` (parameters `instanceId`, `imei`(MD5), `oaid`, `projectId`, `user`) | `util/g.java:38-49`、`util/d.java:28` |
| Placement queue | 3 SQLite tables: `events(_id,appid,package,event_name,priority,data BLOB,timestamp)`, `events_cloud(_id,app_id,cloud_data,timestamp,data_hash)`, `monitor(_id,appid,package,event_name,url,send_count,timestamp)` | `c/c.java:85`、`b/g.java:33`、`a/a.java:81` |
| Encryption | JSON → **gzip** → **AES/ECB/PKCS5Padding** (see `d/a.java` for key/salt); and RSA (`d/b.java`, decrypt with keystore private key) | `c/q.java:120-147`、`d/a.java:14-92`、`d/b.java:42-60` |
| batch/retry | `c/m.java`: `"will restart retry msg after N"`, `"fire retry timer after N"`, `"retry success"` after failure; `c/i` controls the sending switch; `c/q` records `"before zip and encrypt, len=… after=…"` | `c/m.java:59-99`、`c/q.java:120` |
| timeout | `CONNECT/READ = 10000 ms` (`g/b.f2461a=10000`), another constant 15000 | `g/b.java:24-25` |
| Collection switch | Network master switch `setAccessNetworkEnable`; basic mode `setBasicModeEnable`; privacy policy `setCustomPrivacyPolicyAccepted` | `OneTrack.java:225-234,280-290` |

### 4.5 xCrash: crash capture capability

| item | Observed | Source |
|---|---|---|
| version | tombstone head `"xCrash 2.4.9"` | `xcrash/j.java:211` |
| capture type | Java uncaught exception (`h.java`), ANR (`b.java`/`d.java`), native signal (`NativeHandler.nativeInit`, `libxcrash*.so`) | `XCrash.java:448-453`、`NativeHandler.java:92-148` |
| signal | Native side registration (`nativeTraceSignalInit`), signal set in `.so` **[Unknown]** | `NativeHandler.java:41,99,129` |
| Drop path | Default `context.getFilesDir() + "/tombstones"` (can be covered by `setLogDir`; not covered by this APK) | `XCrash.java:430-433` |
| file name | `tombstone_<pid>_<milliseconds>_<pkg>__<type>{.java.xcrash,.anr.xcrash,.native.xcrash}` | `h.java:165`、`d.java:365`、`NativeHandler.java:112` |
| Whether to upload | **itself does not upload** (there is no HTTP/URL among the 12 categories); OneTrack `CrashAnalysis` reads the file and then goes `onetrack_bug_report` → `tracking.miui.com` | grep `http` hits `xcrash` 0; `CrashAnalysis.java:158-178`, `api/c.java:84` |
| Initialization method | `CrashAnalysis` uses **to reflect** to tune `XCrash.init(Context, InitParameters)` (to avoid compile-time dependencies) | `CrashAnalysis.java:242-252` |
| Which handlers are opened by default | `InitParameters` is determined by the default field; `init()` is only registered when `f11576g`(java)/`E`(anr)/`f11587r`(native) is true | `XCrash.java:445-453`; specific default value **[unknown]** (the initial value of the field has not been verified one by one) |
| Debugging remnants | `testJavaCrash/testNativeCrash` public method exists (can be triggered externally to cause crash) | `XCrash.java:391-402` |

### 4.6 `com.sun.nio.zipfs`: why it is needed + actual call point

**[Observation]** APK contains `unknown/META-INF/services/java.nio.file.spi.FileSystemProvider` (apktool unpacking, falling outside `unknown/` instead of `META-INF/`), with 12 `com.sun.nio.zipfs.*` class registration `jar`/`zip` scheme.

**unique call point**: `p0/e.java:152-157`

```java
public e(Path path) {                       // path = to be scanned APK
    this.f7366b = path;
    this.f7368d = FileSystems.newFileSystem(new URI("jar:" + path.toUri()),
                     k0.e(g1.w.a("useTempFile", Boolean.FALSE)), e.class.getClassLoader());
    ...
}
```
Subsequent `e.e(ScanTarget)` uses `Files.walkFileTree(fs.getPath("/"), depth 1)` to traverse the entries in APK, hit `pathMatcher` of `ScanTarget.{Dex,AndroidManifest}` and get `Files.size` (`p0/e.java:160-179`).

| Judgment | content |
|---|---|
| Purpose | **decompresses** directly and fetch `classes*.dex` / `AndroidManifest.xml` from the zip namespace of APK for MI engine NG (`MiEngineNG` + `t0.a`/`ScanTarget`) to do in-package scanning. |
| Why it is necessary to pack | Android default `FileSystemProvider` does not support `jar:`, and the zipfs of standard JDK is not in AOSP; if it is not packaged, `FileSystems.newFileSystem` throws `ProviderNotFoundException` |
| Whether "framework class remains" | **is not**, it is a live code dependency (`p0/e` is used by the `t0`/`MiEngineNG` link) |
| Other zipfs users | 0 (There is only one `p0/e.java:155` `newFileSystem` in the whole tree) |

### 4.7 `android.content.pm.{PackageParser,PackageUserState,VerifierInfo}`: Why + call point is needed

**[Observation]** 3 classes total 6.8 KB (`PackageParser.java` 5590 B, is the cut down AOSP parser). The call points are exhausted as follows:

| call point | Usage | Purpose |
|---|---|---|
| `w0/d.java:47,80,112` | `new PackageParser().parsePackage(file, 0)` + `collectCertificates` + `generatePackageInfo(..., new PackageUserState())` | **parses any APK** offline (not installed, not subject to the API restrictions of `PackageManager` for uninstalled packages); signatures are collected only when `i6 & 64` |
| `GuardProvider.java:168` | `w0.d.a(context, str, (i6&64)!=0?64:0)` | Implementation of `call("parseApk")` (`:331` branch) |
| `i0/g.java:11-46` | After resetting `mSigningDetails` to existing `Package`, `PackageParser.collectCertificates(pkg,false)` | **only takes the signature** (`signaturesMd5/Sha256`) for reporting by each engine and the cloud |
| `n0/g.java:821,831,1094` | `PackageParser.parsePackageLite(new File(str),0)`、`i0.g.a(...)` | Scan result supplement `versionCode/versionName/signature` |
| `i0/l.java:89,102,312`、`i0/f.java:106`、`c0/b.java:90`、`c0/d.java:118,157` | `parsePackageLite` / `new Package(name)` + `collectCertificates` | Ash production storage and result assembly |
| `g0/e.java:99` | `parsePackageLite` | Scan target metadata |

| Judgment | content |
|---|---|
| Purpose | ① `parseApk` IPC parses the package name/version/label/icon for any path ** ** (including download directories other than `/data/app`); ② Unifiedly obtain **signature summary** for local library and cloud query (`mfSha256/signaturesMd5/signaturesSha256`); ③ `parsePackageLite` read-only manifest to avoid the overhead of whole package parsing |
| Why it is necessary to pack | `PackageManager.getPackageArchiveInfo` is restricted on some ROM/partition paths, and `parsePackageLite` and `SigningDetails` (v2/v3 signature scheme) are incomplete on system API with targetSdk 33; bringing your own copy of AOSP ensures consistent behavior |
| Risk point | The built-in parser **does not perform** `unknown sources`/partition verification, which is equivalent to turning "any APK parsing" into provider capability (protected by the `Guard_Provider` system signature authority) |

### 4.8 Tencent residue on the dex side (distinguished from "actual capabilities")

| Residue | Evidence | Qualitative |
|---|---|---|
| `com.tencent.meri` / `com.tencent.tmsecure.demo` / `com.tencent.tmsecure.common.ISDKClient` / `com.tencent.feedback.eup.CrashReport` | String constant (lead has been cored, within dex/smali) | **package name/class name remains**, no corresponding implementation class → does not constitute capability |
| `com.tencent.tmsecure.R`（28 KB） | `com/tencent/tmsecure/R.java` | Resource ID table residue |
| `"Meri"` custom head | `tmsdkobf/p9.java:263,283-291` | **or**: `detectNetworkState()` uses `http://tools.3g.qq.com/wifi/cw.html` to determine "the network is hijacked/authentication required" in the response header `Meri` |
| `Encode`（`libxy.so`） | `Encode.java:43`; `lib/arm64-v8a` None `libxy.so` | **dead code** |

### 4.9 Other dependencies: qualitative in one sentence

| package | Number of files/lines | Qualitative | Is there business logic? |
|---|---|---|---|
| `dagger.hilt.android.internal.managers.**` | 3 files (30/6/5 lines) | Minimal residue of Hilt's `ViewComponentManager` (Activity component holder) compile-time generation | No (this APK has no activity) |
| `androidx.room.MultiInstanceInvalidationService` | 1 file 147 lines | Room multi-process failure notification Service | No (no `RoomDatabase` subclasses reference it; `AndroidManifest` is not registered) |
| `okhttp3.internal.publicsuffix.PublicSuffixDatabase` | 1 file 343 lines | OkHttp’s eTLD+1 determination (cookie domain matching) | No (OkHttp internal only; this APK OkHttp engine update for `s0/s.java`) |
| `org.apache.commons.logging.**` | 4 files (`LogFactory` at line 953) | Apache Commons Logging facade | No (no `LogFactory.getLog` call point) |
| `j$.util.stream.{DesugarCollectors,e}` | 2 files | **desugar product** (`java.util.stream.Collector` is in the compatibility layer of API<24) | No (purely compatible) |
| `kotlin.Metadata`、`kotlin.coroutines.jvm.internal.DebugProbesKt` | 2 files 29 lines | Kotlin compiler annotations + debugging probes | No (compile time/debug) |

**[Inference] The six groups** are all "brought in by dependency closures" and have nothing to do with GuardProvider's anti-virus/cleaning business; `room` and `commons-logging` are most likely to come from the transitive dependency of a certain AAR (`room` has only one class, `commons-logging` has no call point).

## 5. Configuration items/constants/protocol fields

### 5.1 Tencent side network endpoint (TMSDK / National Secret Channel)

| endpoint | Type | Trigger condition | Source |
|---|---|---|---|
| `https://tmfsdk.m.qq.com` | HTTPS (JSON configuration/service) | Any shark request (Yuncha 2016, WiFi 794/800, statistics 29987/29988) | `tmsdkobf/w5.java:53-54,127-128`；`qscanner/impl/f.java:1310`；`p9.java:254` |
| `https://tmfsdk4.m.qq.com` | HTTPS (only for IPv4 reporting) | IP reporting in IPv4 environment | `w5.java:61-64` |
| `tmfsdktcp.m.qq.com:8001` | TCP long connection (IPv6 channel) | `setAutoConnectionSwitch(true)` post-construction link | `w5.java:134-140` |
| `tmfsdktcpv4.m.qq.com:8002` | TCP long connection (IPv4) | Same as above | `w5.java:68-74` |
| `120.241.190.227:8002` / `61.241.44.59:8002` / `14.18.175.81:8002` | Naked IP TCP | `PRE_USE_IP_LIST != "false"` (default empty string → **goes to IP list**) | `w5.java:150-169`、`TMSDKContext.java:36-47` |
| `2409:8c54:1003:1019::2d:8001` / `2408:8756:2cf2:19::d7:8001` / `240e:ff:f100:8019::6c:8001` | IPv6 TCP The whole story | Same as above | `w5.java:84-103` |
| `https://mmgr.gtimg.com/qqsecure_config_update/qqsecure_config_update` | HTTPS (signature database update list) | `UpdateManager.check(UPDATE_FLAG_VIRUS_BASE)` | `tmsdk/common/module/update/a.java:675,811`；`TencentEngine.java:493` |
| `http://tools.3g.qq.com/wifi/cw.html` | **plain text HTTP** (network authentication/hijacking detection) | `WifiDetectManager.detectNetworkState()` | `tmsdkobf/p9.java:269,286` |
| `http://tools.3g.qq.com/j/sslstrip` | **plain text HTTP** (SSLStrip detection) | `detectSSLStrip()` (this APK is not called) | `p9.java:363` |
| `http://m.qq.com` | String prefix comparison (no request initiated) | SSLStrip redirection determination | `p9.java:373` |
| `www.qq.com:80` | TCP connectivity detection | Network availability determination | `tmsdkobf/m8.java:94` |

### 5.2 Xiaomi side network endpoint

| endpoint | Belong | Trigger condition | Source |
|---|---|---|---|
| `https://api.sec.miui.com/antivirus/viruses` | GuardProvider itself (cloud virus database) | Synchronized virus database (`sync`) | `i0/d.java:203` |
| `https://api.sec.miui.com/antivirus/whitelist` | Same as above | Sync whitelist | `i0/d.java:55` |
| `https://flash.sec.miui.com/detect/app` | Same as above | Cloud Lightning Scan | `j0/c.java:75` |
| `https://virus-engine.sec.miui.com` / `https://virus-engine.sec.intl.miui.com` | Same as above (engine/feature library) | Engine update (`s0/s.java:124` as OkHttp baseUrl) | `q0/m.java:12` |
| `https://tracking.miui.com` | OneTrack Incident Reporting | There are event batches and the network allows it | `util/x.java:35,129` |
| `https://tracking.intl.miui.com` / `tracking.india.miui.com` / `tracking.rus.miui.com` | OneTrack (Region) | Area=INTL/IN/RU or remote host coverage | `util/x.java:38-44,145-147` |
| `https://sdkconfig.ad.xiaomi.com`（+`.intl`/`.india`/`.rus`） | OneTrack configuration pull | `RegionDomainManager` initialization/configuration refresh | `util/x.java:47-56,132-142` |
| `<host>/api/open/device/writeBack` | OneTrack Advertising Monitoring Writeback | When advertising monitoring is enabled (this APK has not left `adTrack`) | `util/g.java:38-49`、`util/d.java:28` |
| `api.ad.xiaomi.com` (only string judgment) | OneTrack configuration filtering | When parsing ad configuration | `a/b/b.java:62` |

### 5.3 AVL: **does not have its own hard-coded domain name** (important conclusion)

**[Observation]** 3 exhaustive searches all yielded 0 hits: ① `com/avl/**` whole tree `https?://` 0 hits; ② 26 files in `assets/avlsdk` (including `sdk.conf`/`version.conf`/`avllib.conf`) has no host name, `sdk.conf` is an encrypted/obfuscated block; ③ `libavlm.so`/`libavlasys.so`/`libavlurl.so` string scanning only results `http://www.w3.org`, `http://www.wapforum.org`, OpenSSL FAQ and other** libraries have fixed strings ** and no business domain name.
**[Observation]** reports/updates the base address from the configuration: `j/p.java:93` (`this.f1543i = a(kVar, new c(kVar.c()).a())`), and finally spells `?channel=<channel>&pkg=<package name>` (`:142-161`). The host has not set `logUploadUrl` (`AvlEngine.java:99`).
**[Inference] The cloud query/log/update endpoint of** AVL is **(config/license driver)**. This APK is not solidified in the static layer → If enabled, the real domain name can only be obtained from `sdk.conf`/license at runtime Solved. **[Unknown]** specific domain name.

### 5.4 OneTrack reports the JSON field (protocol field table)

Two-part request body: `{"e":<public parameters>, "d":<event body>}` (`api/c.java:41-43,53-58`, etc.).

| segment | key | meaning | Source |
|---|---|---|---|
| e | `event` | event name | `f/b.java:130` |
| e | `imei` | IMEI of MD5 | `f/b.java:133`；`DeviceUtil.b()` |
| e | `oaid` | OAID plain text | `:136` |
| e | `sn` | Serial number (reserved, not assigned yet) | `:139` |
| e | `gaid` | Google Advertising ID (International Referral Event) | `:142` |
| e | `android_id` | AndroidID (reserved) | `:145` |
| e | `instance_id` | Instance ID (`o.a().b()`) | `:148` |
| e | `mfrs` / `model` | Make/Model | `:151,:154` |
| e | `platform` = "Android" | — | `:157` |
| e | `miui` / `build` / `os_ver` | MIUI Version/Build/System Version | `:160,:163,:166` |
| e | `app_id` / `app_ver` / `pkg` / `channel` / `sdk_ver` | Applications and channels | `:169-196` |
| e | `e_ts` / `tz` / `net` / `region` | Time/Time Zone/Network/Region | `:181-190` |
| e | `uid` / `uid_type` / `sid` / `plugin_id` | Users and sessions | `:193-205` |
| e | `sdk_mode` / `ot_first_day` / `ot_test_env` / `ot_privacy_policy` / `market_name` / `ot_ad` / `ot_basic_mode` | Modes and switches | `:119-127,123` |
| d(`onetrack_dau`) | `first_open` / `imeis` / `imsis` / `config_status` | First boot, IMEI list, IMSI list (MD5), configuration status | `api/c.java:108-117` |
| d(`onetrack_bug_report`) | `exception` / `type` / `message` / `feature` / `crashtime` | Crash summary | `api/c.java:90-94` |
| d(`threats`…) | 8 scan fields | See §4.4.2 | `MiEngineNG.java:201` |

### 5.5 Domain name/endpoint summary list (merged)

| Domain name (including port) | Attribution SDK | Trigger condition | Source | Plain text? |
|---|---|---|---|---|
| `api.sec.miui.com` | GuardProvider itself | Virus database/whitelist synchronization | `i0/d.java:55,203` | HTTPS |
| `flash.sec.miui.com` | GuardProvider itself | Cloud Lightning Scan | `j0/c.java:75` | HTTPS |
| `virus-engine.sec.miui.com` / `virus-engine.sec.intl.miui.com` | GuardProvider itself | Engine/feature database update | `q0/m.java:12` | HTTPS |
| `tracking.miui.com` | OneTrack | Incident reporting (domestic) | `util/x.java:35` | HTTPS |
| `tracking.intl.miui.com` | OneTrack | Incident Reporting (International) | `util/x.java:38` | HTTPS |
| `tracking.india.miui.com` | OneTrack | area=IN | `util/x.java:44` | HTTPS |
| `tracking.rus.miui.com` | OneTrack | area=RU | `util/x.java:41` | HTTPS |
| `sdkconfig.ad.xiaomi.com` | OneTrack | Pull configuration (domestic) | `util/x.java:47` | HTTPS |
| `sdkconfig.ad.intl.xiaomi.com` | OneTrack | Pull Configuration (International) | `util/x.java:50` | HTTPS |
| `sdkconfig.ad.india.xiaomi.com` | OneTrack | Pull Configuration (India) | `util/x.java:53` | HTTPS |
| `sdkconfig.ad.rus.xiaomi.com` | OneTrack | Pull configuration (Russian) | `util/x.java:56` | HTTPS |
| `api.ad.xiaomi.com` | OneTrack | Advertisement configuration filtering (string determination) | `a/b/b.java:62` | — |
| `tmfsdk.m.qq.com` | TMSDK | Cloud check/WiFi/Statistics | `w5.java:53` | HTTPS |
| `tmfsdk4.m.qq.com` | TMSDK | IPv4 reporting | `w5.java:63` | HTTPS |
| `tmfsdktcp.m.qq.com:8001` | TMSDK | TCP long connection | `w5.java:137` | private protocol |
| `tmfsdktcpv4.m.qq.com:8002` | TMSDK | TCP long connection | `w5.java:71` | private protocol |
| `120.241.190.227:8002` / `61.241.44.59:8002` / `14.18.175.81:8002` | TMSDK | IP direct connection | `w5.java:154-166` | private protocol |
| `2409:8c54:1003:1019::2d:8001` and other 3 IPv6 | TMSDK | IPv6 Secrets | `w5.java:88-100` | private protocol |
| `mmgr.gtimg.com` | TMSDK | Feature database update | `update/a.java:675` | HTTPS |
| `tools.3g.qq.com`（`/wifi/cw.html`、`/j/sslstrip`） | TMSDK (WifiDetect, codename Meri) | Network authentication/hijacking/SSLStrip detection | `p9.java:269,363` | **HTTP** |
| `m.qq.com` | TMSDK | SSLStrip redirect comparison | `p9.java:373` | Not requested |
| `www.qq.com:80` | TMSDK | TCP connectivity detection | `m8.java:94` | Naked TCP |
| (No hard-coded domain name, configure driver) | AVL | Cloud check/log/URL rule update | §5.3 | Unknown |

**has a total of 22 host names + 6 bare IPs/ports.**

### 5.6 Other key constants

| constant | value | Source |
|---|---|---|
| AVL SDK version | `4.0.1` (facade)/`4.0.0` (module) | `AVLEngine.java:19`、`security/g.java:75` |
| AVL engine/library version | `av.siglib=20220928.ah`、`av.engine=351`、`url.siglib=20180917.zz`、`url.engine=124` | `assets/avlsdk!sdk_conf/version.conf` |
| AVL appkey | `D5DFB878335A3EF4E76F60ED470B8EC8`（meta-data `AVL_OPENSDK_APPKEY`） | `a/e.java:39` + Manifest |
| AVL equipment gear | `FLAG_LITE_DEVICE=0`/`FLAG_HIGH_DEVICE=1` | `AVLSdkConfig.java:5-6` |
| TMSDK SDK version | `"6.5.0 20220325111039"`; native library name `Tmsdk-2.0.13-mfr` | `TMSDKContext.java:25,31` |
| TMSDK signature file ID | 62 pieces (including `70003`→`.amf`, `40706`→`.dat`) | `update/UpdateConfig.java:197,207,311` |
| TMSDK Statistics ID | `29987/29988` (automatic connection), `29953` (cloud search), `29979` (DNS/fishing), `29978` (ARP), `1320056/1320057` (scanning), `1320068` | `TMSDKContext.java:177`、`qscanner/impl/f.java:1264`、`p9.java:203,262,320,195` |
| SM4 default key | `73 42 5A 55 18 08 06 5C 76 70 21 70 24 24 33 29` | `com/tencent/tmf/gm/b.java:7` |
| SM2 public key length constraint | 128 hex（X||Y 32 bytes each) | `tmsdkobf/j7.java:161-164`、`gm/b.java:45-51` |
| xCrash version | 2.4.9 | `xcrash/j.java:211` |
| xCrash download directory | `filesDir/tombstones` | `XCrash.java:431` |
| OneTrack appId | `2882303761517522561` | `z/b.java:22` |
| OneTrack SDK version | `BuildConfig.SDK_VERSION` (value not taken) → Report field `sdk_ver` | `f/b.java:323`; **[Unknown]** specific value |
| OneTrack timeout | Connect/read 10000 ms; additional 15000 | `g/b.java:24-25` |
| OneTrack Queue List | `events` / `events_cloud` / `monitor` | `c/c.java:85`、`b/g.java:33`、`a/a.java:81` |
| AVL key assets | `assets/avlsdk`(1.53 MB ZIP, 26 items), `assets/mdetector`(ELF32 ARM, 136 KB, **has no Java reference**), TMSDK `assets/70003.amf`/`40706.dat`/`licence.conf`/`licence1.conf` | Unpacking list |

## 6. Permissions and attack surface/risk points

### 6.1 Privacy collection link (only "enabled" is listed; collection → call → storage → destination)

| # | What was collected | who calls (file:line) | Where to save | To whom |
|---|---|---|---|---|
| 1 | `ANDROID_ID`、`Build.MODEL` | `TencentEngine.java:402` → `TMSDKContext.java:188-207` | In-process static `xb.f10474a`, `qb.f9915a` (not dropped) | TMSDK shark request (`tmfsdk.m.qq.com`/TCP 8001-8002) and statistical management `y9` |
| 2 | WiFi `SSID`、`BSSID`、DHCP `DNS1/DNS2` | `TencentEngine.java:122` → `tmsdkobf/p9.java:210-254` | Request body memory only | Tencent cmd **794** (`p9.java:254`), cloud determination DNS hijacking/phishing |
| 3 | Currently connected BSSID + scanned `ScanResult` | `TencentEngine.java:107-114` (self-collected by the host) → `WifiDetectManager.detectSecurity` | Not placing the order | Only local judgment (check `1320068` when abnormal) |
| 4 | IMEI list (plain text), IMSI list (MD5) | `api/c.java:111,114`（`onetrack_dau`） | SQLite `events.data` (AES encrypted BLOB) | `https://tracking.miui.com/track/v4` |
| 5 | IMEI(MD5)、OAID、AAID | `f/b.java:304-305` (common parameters for each event), `util/g.java:45-46` (writeBack) | Same as above; `instanceId` save as SP | Same as above / `…/api/open/device/writeBack` |
| 6 | Package name/version/signature summary/installer/threat name of the scanned APK | `MiEngineNG.java:201`（`threats`/`blackgrays*`） | SQLite `events` | Same as above |
| 7 | Crash scene (tombstone: stack, `/proc/maps`, fd, logcat fragment) | `XCrash` drop (`XCrash.java:431`) → `CrashAnalysis.java:158-178` read | `filesDir/tombstones/*.{java,anr,native}.xcrash` | `onetrack_bug_report` → `tracking.miui.com` (summary only: `exception/type/message/feature/crashtime`) |
| 8 | Any package name/version/label/icon/signature of APK (including uninstalled packages) | `GuardProvider.java:168` (`parseApk`) → `w0.d.a` → Comes with `PackageParser` | Do not place the order, return with Binder | Returned to the caller with `Guard_Provider` authority (system/same signature) |

**is not enabled (important to avoid misjudgment)**: OneTrack's IMEI/IMSI **is controlled by `api/c.java:109` area switch gate** (see §7-④); TMSDK's "Installed Application List" is only triggered when the scan list is empty (this APK does not trigger, `qscanner/impl/f.java:434`+`TencentEngine.java:470-472`); `UrlCheckManager`/SMS/Contact Book/Clipboard/located in this APK **have no call point** (§4.2.3, §4.4.3).

### 6.2 Permissions and Attack Surface

| noodles | Observed | risk |
|---|---|---|
| `provider guard` | `protectionLevel=system|signature` (briefing has been finalized) | Not directly accessible to external third parties; but the capabilities themselves are extremely powerful (see below) |
| `parseApk` | Accept **and** strings in any path, and the built-in parser does not perform source/partition verification (`GuardProvider.java:168`, `w0/d.java:47`) | If the caller (system component) is induced to pass in malicious APK, the parser can be triggered OOM/Exception (DoS); the return value is leaked label/icon/Signature (information collection) |
| `getRiskAppData` | Enumerate installed applications × local gray production library (briefing has been decided) | Application manifest side channel (only system adjustable) |
| `sync_scan`/`install_scan` | Do local + cloud scanning for any path (`TencentEngine.java:472`) | Cloud Check will send **package name/version/certificate summary** to Tencent (§6.1-6) |
| AIDL `IURLScanServer` | `y/b.java:13-37` handwritten Parcel interface | URL is passed into AVL through Binder and determined by the local library; the return code 0/1/2 semantics are not enumerated in the Java layer, and if the caller misjudges "1" as safe, it will be missed (**[Unknown]**) |
| xCrash debugging method | `testJavaCrash/testNativeCrash` is `public static` (`XCrash.java:391-402`) | Any code in the same process can actively cause crashes (debugging residue) |
| TMSDK plain text HTTP | `http://tools.3g.qq.com/wifi/cw.html`（`p9.java:269`） | MITM can be forged `Meri` response header → falsely reports "hijacked" as "safe" (security conclusion can be manipulated) |
| Hardcoded SM4 key | `gm/b.java:7` | If the TMSDK channel uses the default key (instead of the session key), the captured packets can be decrypted offline. |
| TMSDK bare IP direct connection | `120.241.190.227:8002` etc. (`w5.java:154-166`) | Bypass DNS/domain name ban policy; traffic characteristics are fixed |
| OneTrack debugging component | `com.xiaomi.onetrack.otdebugger.FloatWindowService` (floating window debugger), `com.xiaomi.onetrack.DEBUG` and other switches (lead has been cored, dex string) | If the component is exported, the third party can bring up the floating window debugging interface/trigger the upload (**[Unknown]** needs to be determined by the `exported` of the Manifest; the only exported component of this APK is provider+service, so it is inferred that **has not exported**) |
| Bring in dead code | `libturingvd.so` + `TNative` (zero reference), `Encode` (`libxy.so` is missing), `sms_check_v4` (main body is missing), AVL A large number of unused natives in class 411 | Increase the attack surface and volume; native self-pull cannot be excluded from the Java layer |

### 6.3 "Claim vs Enable" ledger (one sentence version)

- **AVL**: Can scan files, scan URL, cloud search, current limit, certificate analysis, log reporting; this APK can only open `scan`/`scanURL`/`update`/`updateURLRule`, **log off, cloudless configuration**.
- **TMSDK**: Can scan APK (local + cloud), update signature database, check URL reputation, deep clean, read SMS/traffic SMS, number tag, WiFi seven types of detection; this APK only opens **APK Scan (including cloud) + signature database update + WiFi four types of detection (network/DNS phishing/ARP/encryption type)**.
- **OneTrack**: can collect IMEI/IMSI/OAID/GAID/AAID/MAC/model/network/storage/screen, can automatically bury activities, and can catch crashes; this APK only sends **4 business events + 2 SDK automatic events**, and the collection area is switched with the area switch `GAIDEnable=false` shrinks.
- **national secrets**: SM2/SM4 are all JNI; SM4 has a hard-coded default key, and SM2 public key is issued by the server (128 hex).
- **xCrash**: Only downloading but not uploading, uploading is done by OneTrack.
- **zipfs/PackageParser**: They are all live dependencies (read the zip in APK; offline parse APK + get the signature), not residuals.

## 7. Unresolved issues

| # | question | What step has been achieved | What is needed to close the loop? |
|---|---|---|---|
| ① | AVL cloud search/log/update/URL real domain name ** ** | It has been certified that the Java layer and the three .so do not have business domain names; the base address comes from the configuration item of `j/p.java:93` (`com.avl.engine.j.c`) | Decrypt `assets/avlsdk!sdk_conf/sdk.conf` or capture `AVLEngine.update()`/`scanURL` |
| ② | `AVLEngine.scanURL` returns the semantics of `0/1/2` | It has been proven that Java only does transparent transmission (`UrlDetector.java:201-207`), no enumeration | AVL official document or native `matchURLEx` inverse |
| ③ | Category meaning of `avlpk_{apn,amc,sfmf,sgnl,kw,dhc,des}.avl` | Only `pornware` (porn) can be self-certified; the rest are inferred based on naming (§4.1.3) | Rule file format analysis/engine documentation |
| ④ | Did OneTrack really report IMEI/IMSI? | Gate `api/c.java:109` has been located (`q.h()` determines whether to skip) and default switch `isIMEIEnable=true` | Get the true value of `util/q.h()` (MIUI region/privacy settings) at runtime |
| ⑤ | Which handlers are enabled by xCrash by default (java/anr/native) | Certified `init()` registered by `f11576g/f11587r/E` three Boolean (`XCrash.java:445-453`) | Read the initial value of `InitParameters` field by field |
| ⑥ | Is Turing risk control running and which fields are collected? | It has been proven that `TNative` is fully native (9 `SparseArray` methods) and **Java has zero references to** | Inverse `libturingvd.so` / Verify whether it is self-pushed at runtime |
| ⑦ | TMSDK `x5`/`y9` Complete field mapping of the report body | It has been verified that 117 JCE structures belong to tmsdk (t-cloud has given coding rules) and located cmd 794/800/2016/29987/29988/29953/29979 | Create a complete comparison table of cmd↔JceStruct↔fields |
| ⑧ | TMSDK root detection su path complete set | lead has given 11 paths from the dex/native side + `content://com.lbe.security.miui.su` | I have not reviewed the `libTmsdk-2.0.13-mfr.so` string, pending cross verification |
| ⑨ | `assets/mdetector` (ELF32 ARM) who is loading | Proven Java layer zero references (grep `mdetector` 0 hits) | Inverse `libavlm.so`/`libTmsdk`, or run-time monitoring `execve`/`dlopen` |
| ⑩ | Format of `assets/{40706.dat,70003.amf}` | Certified `amf` magic + `UpdateConfig` named with ID (`40706`→`.dat`, `70003`→`.amf`); `40706` is read by `qscanner/impl/f.java:415` | Decrypt encrypted/serialized format (`b2`/`qa` deserializer) |
| ⑪ | OneTrack batch size, number of retries, and backoff limit values | Certified mechanism (`c/m.java:59-99`, `c/q.java:120-147`) | Constant-by-constant setting (int field of `c/i`, `c/m`) |
| ⑫ | Whether `parseApk` can be transferred externally to the **directory** (take the system API branch) | It has been certified that `w0/d.a` will run `getPackageArchiveInfo` (`w0/d.java:56/89/121`) for the directory, and `PackageParser` will run for the file. | Audit the path verification of `GuardProvider.java:160-180` and the caller |
| ⑬ | Actual value for OneTrack `sdk_ver` | The key has been located (`f/b.java:323`), the value comes from `BuildConfig.SDK_VERSION` | Read `com/xiaomi/onetrack/BuildConfig.java` constant |
| ⑭ | OneTrack debugs whether the floating window component is exported | lead gives `otdebugger.FloatWindowService`; this APK is only exported as provider+service (briefing) | Directly check `android:exported` of `AndroidManifest` |

## 8. REA evidence ID list

Tools: REA MCP `inspect_android_method`, provider `jadx` 0.7.1, target `GuardProvider-2.0.3-20231124.0.apk` (SHA-256 checksum is consistent). The three calls are all static decompilation of **and observation of**, but APK is not executed.

| evidence_id | target | what proved |
|---|---|---|
| `ev_3a4f329853af4179576b94977a9ea352640a435f2b597f95dd378e4d73297be0` | `com.miui.guardprovider.engine.tencent.TencentEngine.e(f0.a$InterfaceC0073a)` | `TMSDKContext.setDeviceInfo(ctx, Settings.Secure.getString(cr,"android_id"), Build.MODEL)` and **of `TMSDKContext.init(ctx,null,cfg)` have a sequential relationship within the same method** (privacy injection is initialized before SDK); `f1767f` single initialization semantics |
| `ev_52917bb293318b419051f6073ed950055fb152d3f34f00c58517a2db0c3fa348` | `w0.d.a(Context,String,int): PackageInfo` | **that comes with `PackageParser` can call** (`parsePackage`+`collectCertificates`+`generatePackageInfo`) and SDK≥29/28/<28 three-level branches; the directory goes to the system `getPackageArchiveInfo`, and the file goes to the parser |
| `ev_4c3c9e940c85e3845fdb29bda1d56aad8043ddfc10765930229e329d613dce9f` | `z.b.<init>()` | OneTrack’s only initialization configuration: `appId=2882303761517522561`, `channel=package name`, `ExceptionCatcherEnable=true`, `Mode.APP`, `AutoTrackActivityAction=true`, `GAIDEnable=false` |

A failure and correction in the **evidence collection process** (leaving traces): The first call to `z.b` with `&lt;init&gt;` as `method_name` was rejected (`invalid_input: Method z.b.&lt;init&gt; was not found`), and succeeded after changing back to the literal `<init>` - indicating that the method name needs to be passed in as it is and cannot be HTML escape.

The boundary between **and other reports**: JCE coding rules and `com.qq.taf.jce` The classification of 117 structures is covered by t-cloud in `20_cloud.md`. This article only quotes its conclusions (§5.1, §7-⑦) and does not repeat the derivation. The native side (`libavlm.so`/`libTmsdk`/`libturingvd.so`/`mdetector`) only performs string-level scanning and does not disassemble.

## Appendix A: List of assets and native libraries (byte-level evidence used in this article)

| Path | size | Observed |
|---|---|---|
| `lib/arm64-v8a/libavlm.so` | 2 304 352 | AVL main engine (`scan`/result analysis) |
| `lib/arm64-v8a/libavlurl.so` | 629 976 | URL detection engine |
| `lib/arm64-v8a/libavlasys.so` | 89 144 | AVL Auxiliary |
| `lib/arm64-v8a/liburldetectorsys.so` | 14 408 | `System.loadLibrary("urldetectorsys")` target (`UrlDetector.java:80`) |
| `lib/arm64-v8a/libTmsdk-2.0.13-mfr.so` | 370 088 | TMSDK native (AmScannerV2/root detection) |
| `lib/arm64-v8a/libtmf-gm.so` | 177 920 | SM2/SM4 JNI |
| `lib/arm64-v8a/libturingvd.so` | 240 448 | Turing risk control (Java side zero reference) |
| `lib/arm64-v8a/libxcrash.so` / `libxcrash_dumper.so` | 88 360 / 129 344 | xCrash native |
| `lib/arm64-v8a/libflamingo.so` / `libams-1.3.0-mfr.so` / `libdce-1.1.21-mfr.so` | 1 506 088 / 744 392 / 202 976 | Xiaomi side: cloud control/application management/deep cleaning (outside the scope of this article) |
| `assets/avlsdk` | 1 566 355 | ZIP, 26 items: `av/avl/android/avlpk_*.avl`(8), `url/url/*.avl`(5), `av/kw/**/*.des`(7), `sdk_conf/{sdk.conf,version.conf}`, two groups of `liscense.conf`+`avllib.conf` |
| `assets/mdetector` | 136 636 | **ELF32 ARM** (`7f 45 4c 46 01 01 01`), Java zero reference |
| `assets/70003.amf` / `assets/40706.dat` | 680 928 / 9 368 | `amf\0` magic / encryption block; TMSDK `UpdateConfig` named by ID |
| `assets/licence.conf` / `licence1.conf` | 392 / 368 | TMSDK license (`f5.java:146-150` reads `licence.conf`/`licence<N>.conf`) |
| `unknown/META-INF/services/java.nio.file.spi.FileSystemProvider` | — | Register `com.sun.nio.zipfs.*` provider (§4.6) |

