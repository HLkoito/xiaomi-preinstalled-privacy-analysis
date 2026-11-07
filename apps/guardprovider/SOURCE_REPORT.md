> **Historical analysis with subsequent corrections.** Use the [final review](REPORT.md) for the reviewed findings. It supersedes earlier claims about plaintext IMEI lists, actual uploads under default settings, absolute coverage, and unverified native behavior. The DAU IMEI and IMSI lists use MD5 and remain subject to region, permission, and network checks. Certificate expiry alone does not establish that ciphertext cannot be decrypted. Local identity paths were removed. Historical source line references may point to files outside the published evidence set.

# GuardProvider 2.0.3 complete reverse analysis report (integrated version)

> **SHA-256**：`b0c02fa682b8ca82429b471113d0702042ac76234c85fdcd8efeb7c03d313024`（13,251,548 B）
> **method**: REA (jadx-headless 0.7.1 / apk-signature / ELF semantic layer) as the forensic backbone; full JADX decompilation (3,231 classes dropped) + apktool 3.0.3 (smali/res/DEX String pool) + byte-level parsing of 12 `.so` with 8 assets as a coverage base.
> **Evidence discipline**: Each conclusion is marked **[Observation]** (source code/bytes are directly visible, attached `File: line` or offset), **[inference]** (supported semantic induction), **[unknown]** (not covered). All are static conclusions, APK is not executed.
> **attachment**: `10_core.md` (main control layer), `15_obf.md` (confusion layer semantic map), `20_cloud.md` (cloud/DB/protocol), `30_native.md` (native/assets), `40_vendor.md` (third party SDK), `_obf_index.tsv` (full class index), `dex_strings_all.txt` (29,263 DEX strings).

---

## 0. Qualitative in one sentence

This is not an "App", but the virus scanning and security detection background component **built into the**MIUI system: no Activity, no desktop portal, and no advertiser process.

It packages three anti-virus engines (**Xiaomi self-developed / Antiy AVL / Tencent TMSDK**) under the same layer of abstraction, through **a `system|signature` level ContentProvider (`content://guard`)** Binder Service **, protected by two permissions with the same name as**, provides 9 types of capabilities to **Xiaomi Security Center (`com.miui.securitycenter`)** and **package installer**:

| # | Ability | landing point |
|---|---|---|
| 1 | **APK Static scan** (synchronous/asynchronous/by scene/incremental/full) | `guard/sync_scan`、`install_scan`、`IAntiVirusServer.w/p/u` |
| 2 | **Multi-engine virus determination and result merging** | `n0/g` → Three engines parallel → `D()` merge |
| 3 | **Cloud fingerprint verification** (only the fingerprint is transmitted, not APK) | `p0/f` → `POST /virus/scan` |
| 4 | **Black and white gray list library synchronization and matching** | Room 8 table + 3 plaintext SQLite |
| 5 | **signature/rule engine** (JEXL3 expression + Rust Aho-Corasick) | `v5`(=JEXL3) + `p0/g` + `libflamingo.so` |
| 6 | **URL Phishing detection** (Anty engine only) | `IURLScanServer.H` → `AVLEngine.scanURL` → `libavlurl` |
| 7 | **WiFi security detection** (Tencent engine only, category 4) | `IWifiDetectObserver` → `TencentEngine.n()` → TMSDK |
| 8 | **virus database/engine update** (three independent channels) | `IAntiVirusServer.h`、`q0/*`、AVL/TMSDK native |
| 9 | **risk (gray production) application list export + incident reporting** | `call("getRiskAppData")`, `TRACK_ANTIVURS_DATA` broadcast |

**determines that the cores are all native**: `libavlm.so` (Anty engine body, 98 `AVLM_*` C API), `libavlurl.so` (URL reputation, 33 `AVLM_Match_*`), `libams` (Tencent AMF feature scanning), `libflamingo.so` (Xiaomi self-developed Rust Aho-Corasick matching), `libturingvd.so` (Tencent Turing device risk control/root/emulator detection).

---

## 1. Identity and Integrity

### 1.1 Package identity [observation]

| item | value |
|---|---|
| package | `com.miui.guardprovider` |
| versionName / versionCode | `2.0.3-20231124.0` / `209` |
| minSdk / targetSdk / compileSdk | 26 / 33 / 33 |
| Application | `com.miui.guardprovider.GuardApplication` → `b extends Application` (Hilt injection) |
| label | `System Security Component` (zh-rCN)/`System security components` (en) |
| Build | R8 8.0.46 **full mode** (both dex with R8 metadata), `min-api=26` |
| Others | `allowBackup=false`、`largeHeap=true`、`extractNativeLibs=false`、`requestLegacyExternalStorage=true` |

### 1.2 Archive composition [observations, 155 items]

| Category | content |
|---|---|
| DEX | `classes.dex` 4,155,760 B; `classes2.dex` **3,500 B (`j$/util/stream/DesugarCollectors` only, no business logic)** |
| native (**only arm64-v8a, 12**) | `libavlm`(2,304,352) `libflamingo`(1,506,088) `libams-1.3.0-mfr`(744,392) `libavlurl`(629,976) `libTmsdk-2.0.13-mfr`(370,088) `libturingvd`(240,448) `libdce-1.1.21-mfr`(202,976) `libtmf-gm`(177,920) `libxcrash_dumper`(129,344) `libavlasys`(89,144) `libxcrash`(88,360) `liburldetectorsys`(14,408) |
| assets | `avlsdk`(1,566,355) `70003.amf`(680,928) `mdetector`(136,636) `40706.dat`(9,368) `licence.conf`(392) `licence1.conf`(368) `dexopt/baseline.prof{,m}` |
| META-INF | 41 items: `CERT.RSA/SF`, `MANIFEST.MF`, 24 `androidx.*_*.version`, **`flamingo.dat`(304)**, `app-metadata.properties`, 6 `services/*` SPI registration |

> The truth about **scale**: Among 3,231 classes, **real business only has about 25 obfuscated packages**; the largest residue is **kotlin-reflect + embedded Kotlin compiler front end (about 45 package)**, and coroutine, Gson, OkHttp/Okio, Retrofit, protobuf, Room, JEXL3 (see §9.1, attachment `15_obf.md` for details).

### 1.3 Signature [Observation, REA `inspect_apk_signature`]

| item | value |
|---|---|
| Plan | **v1(JAR) + v2 + v3 Three signatures and deposit**, no v3.1 |
| Signer | The three have the same certificate: `c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025` |
| self-consistency | `self_consistent=true` (statement digest == current byte digest) |
| Key rotation / source stamp | None / None |
| **limit** | **has not been cryptographically verified to sign**; the certificate chain, revocation, and authenticity have not been evaluated. This report does not draw a conclusion on whether it is officially released or not. |

---

## 2. Permission and trust boundaries (the first key to understanding this software)

### 2.1 Export components - there are only 3 [observations] in all APK

| Type | Name | exported | protect |
|---|---|---|---|
| `provider` | `com.miui.guardprovider.GuardProvider`，authority **`guard`** | **true** | `permission=com.miui.guardprovider.permission.Guard_Provider`，`protectionLevel=system\|signature` |
| `service` | `com.miui.guardprovider.manager.SecurityService`，action `…action.antivirusservice` / `…action.urlscan` | **true** | Same as above |
| `service` | `androidx.room.MultiInstanceInvalidationService` | false | — |

**has no Activity or any static BroadcastReceiver.** Therefore, all function entries converge to: **1 Provider (7 entries) + 1 Service (26 Binder transactions)**.

### 2.2 The true meaning of trust boundary [inference]

`system|signature` means: any preset application under **`/system/priv-app` (holding `system` attribute) or application** with the same signature can penetrate. This is not "unreachable by third parties", but "any privileged component within the **platform can reach**". And its exposed capabilities are extremely strong (any path APK parsing, any path scanning, cross-provider content proxy reading, full application list export) - see the attack surface in §12.

### 2.3 Startup sequence [observation, `GuardApplication.java`]

```
attachBaseContext(ctx)
  ├─ f1743d = this // Global singleton, multiple direct references
  └─ if (isUserUnlocked) f.b(ctx) // Unlocked → Create prefs singleton immediately
     else registerReceiver(ACTION_USER_UNLOCKED) // Not unlocked → Waiting for unlocking
onCreate() → super.onCreate() (Build Hilt component) → w.c.a(this) // w/c.java:6-8 is an empty method
USER_UNLOCKED → f.b(ctx) + cancel receiver
```
**[Observation] Application phase zero initialization**: does not init TMSDK/AVL/OneTrack, does not build a library, and does not read system properties. **all lazily load** - the engine and buried points are only started for the first scan/update.

### 2.4 Network main switch (global only switch) [Observation, `GuardApplication.java:38-50`]

```
e(Context):
  Domestic version: = SystemProperties.getBoolean("persist.sys.sc_allow_conn", false)
  International version: ok = com.miui.securitycenter installed && This attribute is true
          region = SystemProperties.get("ro.miui.customized.region", "")
          if region == "jp_kd": ok &&= (Settings.Secure "antivirus_engine_network_enable" == 1)
          return ok
```
**only calls** [observation, t-core] in 5 places in the whole tree: `f0/a:66` (engine base class `b()`), `h0/b:84,153` (cloud client), `n0/g:642` (whitelist refresh before scanning), `z/b:41` (OneTrack).
→ **When this attribute is false: the networking bit of the third/fourth engine, virus database and whitelist synchronization, anti-fraud upload, OneTrack buried points, and whitelist refresh before scanning are all closed**, and the offline signature database is still available.

---

## 3. External functions (IPC full table)

### 3.1 ContentProvider `content://guard` [Observation, `GuardProvider.java`]

| URI / Method | code | realize | Function |
|---|---|---|---|
| `guard/engine_info_list` | 1005 | `query :384-394` | Returns 11-column engine metadata: `Name, NameString, DescriptionName, DialogTitle, DialogHeader, DialogPolicy, DialogCloudScan, DialogSummary, DialogUrl, Enable, SupportCloud`. Data source = `f0.b` enumeration + 8 localized strings of `getStringArray(f3553h)` + `g.z().w(name)` + `f3551f` |
| `guard/sync_scan` | 1006 | `query :395-399` → `f()` | **scans single file** simultaneously. `selectionArgs` must be 2 `[path, deep]`; return `MatrixCursor(Type, VirusName, Description, EngineName, AdType, AdName, AdDescription)` |
| `guard/virus` | 1007 | `query/insert/delete` | **of plain text table `t_virus_white_list` is added and deleted to check** (virus whitelist); after writing `notifyChange` + `c0.d.g(ctx).c(paths)` |
| `guard/install_scan` | 1008 | `query :403-415` | **synchronous scan (with 4th parameter)**. 3 Reference `[path, deep, engineName]` |
| `guard/support_incremental` | 1009 | `query :405-411` | `i0.h.h(ctx)` returns `{support:1}` if true, **otherwise returns `null`** |
| `call("parseApk", path, bundle)` | — | `d() :159-190` | **parses any APK**: If the system `PackageManager` fails, it will fall back to **'s own `PackageParser`**; `flags&128` additionally returns `icon`(Bitmap)+`label` |
| `call("getRiskAppData")` | — | `b() :103-127` | **Risk application list**: All installed applications (skip `flags&1`) × local gray production library → JSON `{packageName, versionCode, type, signaturesMd5}` |

The complete logic of **`sync_scan`** [observation, `f() :200-327`]
1. Starting with `content://` → `openInputStream` Copy to **`<cacheDir>/copy.apk`**, scan and delete (**confused deputy**).
2. `file://` → Take `Uri.getPath()`; ordinary path → use it directly.
3. `g.z().L(new String[]{path}, cb, deep, **2**, engineName)`。
4. `CountDownLatch.await(**2, MINUTES**)` - timeout returns `null` (**task does not cancel**).
5. If hit, 7 columns are assembled according to `y.f` (`Type<-f11667l`, `VirusName<-f11659d`, `Description<-f11660e`, `EngineName<-f11661f`, `AdType<-f11668m`, `AdName<-f11663h`, `AdDescription<-f11664i`).
6. **permission bypass**: There is no `READ_EXTERNAL_STORAGE` and the path is not under `/data` → `w0.e.a(ctx)` → `ContentResolver.call(Uri.parse("content://com.lbe.security.miui.su"), "GrantGuardProvider", null, null)`, request the LBE security component to grant storage permission (`d0.a.a()` is true, that is, skipped in XOpt mode).

### 3.2 Service `SecurityService` (26 Binder transactions) [Observation]

`onBind` distributes two Binders according to action:

**`IAntiVirusServer`** (descriptor `com.miui.guardprovider.aidl.IAntiVirusServer`, 24 transactions)

| code | method | Semantics | User lockout behavior |
|---|---|---|---|
| 1 | `w(String[], IVirusObserver, boolean)` | startScan（scanType=0） | Return to `v(-1,null)` |
| 2 | `q(String, boolean)` | setEngineEnable | ignore |
| 3 | `g(String)` | getEngineEnable | `false` |
| 4 | `P(String)` | isSupportCloudScan | **does not lock** |
| 5 | `N(int)` | cancelScan | ignore |
| 6 | `C(IVirusObserver)` | checkUpdate → **heng -1** (stake) | `-1` |
| 7 | `h(IVirusObserver)` | updateScan (virus database update) | Back to `x({engine:"",type:2})` + `K(-1)` |
| 8 | `k(int)` | cancelUpdate | ignore |
| 9 | `getVersion()` | **heng `"1"`** | `"1"` |
| 10 | `J(String)` | Engine display name | **does not lock** |
| 11 | `i()` | Engine name array → **The last bit is always null (bug)** | **does not lock** |
| 12 | `B(String, IWifiDetectObserver)` | wifiDetectNetworkState | `0` |
| 13 | `D(...)` | wifiDetectDnsAndPhishing | `0` |
| 14 | `O(String, observer, String ip)` | wifiDetectArp | `0` |
| 15 | `Q(...)` | wifiDetectSecurity | `0` |
| 16 | `j()` | detectAppsScan result (package name of `basisOf==2`) | `new String[0]` |
| 17 | `G(String)` | Single package determination (if the cache list is hit, the original name will be returned) | `null` |
| 18 | `A()` | detectAppsScan upload (`/detect/app`) | `-1` |
| 19 | `e()` | Whether the virus cache is not empty | **does not lock** |
| 20 | `u(IVirusObserver, boolean)` | **incremental scan** | **does not determine lock (the only write operation without lock)** |
| 21 | `f(List)` | Filter whitelist by package name | **does not lock** |
| 22 | `E(String, int)` | Package change notification: 2/3 → Clear `scan_result_db`+`risk_gray_result_db`; 1 → Incremental update | **does not lock** |
| 23 | `M(List)` | Filter whitelist by apk path | **does not lock** |
| 24 | `p(String[], IVirusObserver, boolean, int)` | startScanAsScene | Return to `v(-1,null)` |

**`IURLScanServer`** (2 transactions)

| code | method | Semantics |
|---|---|---|
| 1 | `H(engineName, url)` | → `n0.j.b()` → `f0.a.m(1, url)` → **Only `AVLEngine.scanURL`** (`AVLEngine.java:186`); also executed if not locked |
| 2 | `t(engineName)` | → `m(2, null)` → `AVLEngine.updateURLRule()` + drop `avl_url_sdk_version`; lock return `-2` |

**`IVirusObserver`** (13 callbacks): `d`=onScanStarted, `c(i,String)`=onScanStartItem, `s(i,total,VirusInfo[])`=onScanProgress, `v(i,VirusInfo[])`=onScanFinished, `x(EngineUpdateInfo)`=onUpdateItem Finished, `K`=onUpdateFinished, `y`=onUpdateCanceled, `a`=onCheckStarted, `m(VirusInfo[])`=onCacheScanFinished; `b/l/r` Not used.
**`IWifiDetectObserver`**: `onResult(int)` only.

### 3.3 Result reporting channel [observation, `n0/h.java:10-34`]

When hitting (`virusType ∉ {1,4,5}`), send a directed broadcast to `com.miui.securitycenter`:

```
action = "miui.intent.action.TRACK_ANTIVURS_DATA" // Official spelling error ANTIVURS
setPackage("com.miui.securitycenter")
15 extras: path, packageName, virusName, virusDescription, engineName, versionName,
             adName, adDescription, versionCode, virusType, virusLevel,
             adLevel, isAuthoritative, isInCache, extras
```
[Inference] The "Recent Risk Events" data source for the Security Center UI. **is not set `receiverPermission`** (P5).

### 3.4 Functional domain → Panoramic index of code package (14 domains, attachment `15_obf.md` §2.1 detailed table)

| # | functional domain | Carrier package | representative evidence |
|---|---|---|---|
| B1 | Scan Scheduling/Task Queue/Concurrency | `n0` (facade) `l0` (Scanner interface) `m0` `p0` | `n0/g:1260` `L()`, `:1272` upper limit 10, `:1132` `K()` fan-out, `:1296` `M()` type==1→increment; `p0/b0:202` equipped with three scanners |
| B2 | Signature/Rule Engine | `t0` `p4` `v5` `a6` `x5` `y5` `z5` | `t0` was imported by `p0` **133 times** (the highest in the whole tree); `v5/a`=JEXL interpreter, `a6`=JEXL syntax, `z5`=`org.apache.commons.jexl3 {JexlBuilder}` |
| B3 | Cloud scanning client (flash scan) | `q0` `s0` `p0` `u0` | `q0/d` `@f6.f("/virus/scan")`; `p0/f:264-295` group package (androidVersion/miuiVersion/timestamp/uuid/versionType/scanType); `s0/s:124` base=`q0.m.f7703a` |
| B4 | Cloud reporting | `q0` `p0.b0` `s0` | `q0/n` `@f6.f("/virus/localResult/report")`; `p0/b0:235-277` assembly `ResultReportData` (including `VERSION.INCREMENTAL/SDK_INT/DEVICE`) |
| B5 | URL detection/fishing | `n0.j` `com.avl.engine.urldetector` `libavlurl` | `n0/j:39-72` singleton; `AvlEngine:195-205` `m(1)→scanURL`, `m(2)→updateURLRule`; `UrlDetector:80-87` `loadLibrary("urldetectorsys")` |
| B6 | WiFi security check | `n0.k` `f5` `t` | `n0/k:99-115` four methods → `f0.a.d.{ARP,DNSANDPHISHING,NETWORKSTATE,SECURITY}`; `TencentEngine:497` `n()→i(...)`; `f5/a` import `WifiDetectManager` |
| B7 | Virus database/engine update | `q0` `s0` `i0` `r0` `w0` | `q0/m:12` double domain name; `i0/d:203` `antivirus/viruses`, `:55` `whitelist`; `AvlEngine:176-193`; `TencentEngine` `UpdateManager.check(UPDATE_FLAG_VIRUS_BASE,…)` |
| B8 | Installation interception/silent installation | `com.avl` (cross-domain) `p0` `n0.g` | `guard/install_scan`→`GuardProvider:415`; **`AVLA.java:83` native `installPackage(...)`**, called by `com/avl/engine/l/b/c.java:27,80` |
| B9 | Result aggregation / deduplication / caching / whitelist | `c0` `r0` `g0` `i0` | `c0/c:8` `scan_result.db`, `c0/a:12` `risk_gray_result.db`, `w0/h:19` `miui_virus.db`; `n0/g:1014-1087` `D()` merge by level |
| B10 | Anti-fraud/gray product determination | `g0` `j0` `c0` `r0` | `g0/e:20-34` makes `virusLevel=6` + `extras="AntiDefraud:danger;"` + `engine="Mi"`; `j0/a:37-58` analyzes the cloud `basisOf==2` drops `mi_detect_app_db` |
| B11 | Buried points/statistics | `z` `u` `v` | `z` holds OneTrack appid `2882303761517522561`; `u/a` import `com.miui.analytics.ITrack`; `v/a:30-34` OAID/UDID reflection |
| B12 | Encryption/Encoding | `v0` `e5` `y0` | `v0` 11 files, `javax.crypto` 9 places, literal `AES/CBC/PKCS5Padding` / `AES/ECB/PKCS5Padding` / `manifestMFSha256` |
| B13 | Configuration/Cloud Control/System Properties | `c5` `d5` `t` `p1` | `c5/a` reflection `isMiuiLiteRom`; `d5/a` import `miuix.core.util.SystemProperties` + `ro.product.mod_device` |
| B14 | DB / DAO（Room） | `r0` `s0.d` `h i j e g` | `r0` 44 files; `s0/d:140-199` directly takes DAO; `h/i/j/e/g` = Room runtime |

---

## 4. Engine system

### 4.1 Three-engine registry [observation, `f0/b.java:14-18`]

```java
MI      ("Mi",       100, MiEngine/MiEngineNG, "sp_key_mi_engine_enable",      true, <nameRes>, <arrayRes>, 3)
AVL     ("AVL",       11, AvlEngine,           "sp_key_avl_engine_enable",     true, <nameRes>, <arrayRes>, 2)
TENCENT ("TENCENT",   10, TencentEngine,       "sp_key_tencent_engine_enable", true, <nameRes>, <arrayRes>, 2)
```
- The implementation class **of `MI` is determined by the cloud control during enumeration construction.** [observation, `f0/b.java:56-60`]: read `VirusScan` / `miEngineNgEnable` of `MiuiSettings$SettingsCloudData` (default `"true"`) → `MiEngineNG`, otherwise `MiEngine`. **The same `MI` constant can point to two different implementations**.
- **enables the policy** by default [observation, t-core]: `Mi` **is always on and cannot be turned off** (`J()` refuses to write Mi); if the SecurityCenter declares `securitycenter.support.mix.scan`, the three engines are fully open, otherwise AVL/TENCENT is randomly selected 1 (default 2 engines).

### 4.2 Engine capability matrix [observation, `f0/a` abstract contract + 4 implementations]

| Ability | abstract method | MiEngine | MiEngineNG | AvlEngine | TencentEngine |
|---|---|---|---|---|---|
| initialization | `e(InterfaceC0073a)` / `f()` | ✓ | ✓ | ✓ | ✓ |
| Scan | `j(List, c, boolean, String)` | ✓ | ✓ | ✓ | ✓ |
| Cancel scan | `k()` | ✓ | ✓ | ✓ | ✓ |
| update | `l(b)` | ✓ | ✓ | ✓ | ✓（TMSDK UpdateManager） |
| **URL Scan/Rule Update** | `m(int, String)` | ✗(base class -1) | ✗ | **✓ The only implementation of** | ✗ |
| **WiFi detection** | `n(IWifiDetectObserver, d, Object)` | ✗(base class 0) | ✗ | ✗ | **✓ The only implementation of** |
| Networking bit settings | — | `o(boolean)` | `w(boolean)` | `s(boolean)` | `v(boolean)` |
| Support cloud scanning | `g()` | ✓ | ✓ | ✓ | ✓ |
| Initialize asynchronous callback | — | ✓ | ✓ | ✓ | ✓ |

→ Only Antiy can detect **URL; only Tencent can detect WiFi**. This is half the reason why the functionality of this software is incomplete (the other half is that QAX is not implemented).

### 4.3 Engine metadata/display name [observations, resources]

| key | en | zh-rCN |
|---|---|---|
| `AVL` / `DesAVL` | AVL | **Antiy** / Antiy AVL |
| `Tencent` / `DesTencent` | Tencent | **Tencent** |
| `Mi` / `DesMi` | Xiaomi | **Xiaomi** |
| `QAX` / `DesQAX` | — | **Qi’anxin** |

The 8 array items are mapped in sequence: `NameString, DescriptionName, DialogTitle, DialogHeader, DialogPolicy, DialogCloudScan, DialogSummary(=dbupdate), DialogUrl`.

### 4.4 The fourth engine "Qi'anxin QAX" = **is not implemented** [observation]

| Evidence | Conclusion |
|---|---|
| `res/values/arrays.xml:23-32` has complete `qax_string_info`; `public.xml` has `QAX=0x7f0b0006`, etc. | resource exists |
| `res/values/strings.xml` 6 `qax_dialog_*` = literal **`TODO`**; zh-rCN The version is **copied from Tencent copywriting** (`qax_dialog_header`="This function uses the anti-virus engine provided by Tencent", `qax_dialog_url` points to `p=tencent`) | Placeholder |
| `f0.b` enumeration **has only 3 constants** (REA `ev_80d5f9…`) | None QAX constant |
| The whole tree grep `QAX\|qax` only hits `com/tencent/tmsecure/R.java` (TMSDK comes with its own R alias) | No engine classes, no call points |

### 4.5 Engine privacy copy vs. actual behavior [observation + comparison]

| Copy key | en | zh-rCN | code actual |
|---|---|---|---|
| `avl_dialog_header` | "This feature encrypts and uploads **your device's ID** to AVL's servers (%s)." | This function uses the anti-virus engine provided by Antiy AVL. For details, see %s | Java layer only has `setRegion`/`setNetworkEnabled`/`setCloudScanEnabled`/`init(deviceType)`; native has `AVLM_LogUpLoad` + `AVLM_GetSIDInfo` |
| `tencent_dialog_header` | "…uploads **your device's ID** to Tencent's servers (%s)." | Same as above (no mention of uploading) | `TMSDKContext.setDeviceInfo(android_id, Build.MODEL)`（`TencentEngine.java:402`） |
| `tencent_dialog_cloudscan` | "Upload and scan app names and details…" | We will upload the application package name and application characteristics to the cloud virus engine for scanning | `CloudScanApkInfo(package name, version number, certificate MD5, certificate SHA256, MANIFEST.MF SHA256)` —— **only transmits fingerprints, not APK** |
| `tencent_dialog_dbupdate` | "Update virus definitions automatically" | We will upload **encrypted IMEI** to the server for updating the virus database | **None IMEI uploads to implement**; the one that actually reads IMEI/IMSI is OneTrack (`Configuration.Builder` defaults to true, `z/b` is not closed) |

**[Inference] Two points of compliance level misalignment**:
1. **Asymmetric disclosures at home and abroad**: The en version clearly states "upload the device ID to Antiy/Tencent servers", while the zh version weakens it to "use a certain engine".
2. **Tencent's copywriting is inconsistent with the actual**: The Chinese copywriting promises "I upload encrypted IMEI", but the actual code is `android_id + Build.MODEL`; and what actually reads IMEI/IMSI is the OneTrack buried point SDK in the same APK.

---

## 5. Scan pipeline (end-to-end)

### 5.1 Component [Observation, `p0/*`]

| class | Volume | Responsibilities |
|---|---|---|
| `p0/b0` RealScanner | 13 KB | **orchestrator**: CodeScanner + CloudScanner + MatcherScanner series + result reporting |
| `p0/g` CodeScanner | 32 KB | **code-level feature scanning** (rule matching, file by file) |
| `p0/f` CloudScanner | 15 KB | Cloud fingerprint verification (`POST /virus/scan`) |
| `p0/z` MatcherScanner | 21 KB | Local black/white/grey list matching (corresponding to Room 8 table) |
| `p0/y` rule compilation | 5 KB | JEXL3 expression evaluation + `RustSupport.matches/search` |
| `p0/j` | — | assembly point |

### 5.2 Signature engine = **JEXL3 Boolean expression + Rust Aho-Corasick** [observation, independently reviewed]

- **rule form**: Each row of the `characteristics` table has `name / strings / condition / threatName / version`; `condition` is the **Boolean expression**, variable name = `StringItem.key`. `Condition.reduce(Map<String,Boolean>)` evaluation.
- **expression engine = Apache Commons JEXL3** (not self-developed) - evidence:
  - `z5/b` is `JexlBuilder` (fields `z5.h`=JexlEngine, `z5.h.c`=JexlInfo)
  - `v5/b` is `JexlBuilder` package, `v5/e` is `JexlException` (`"JEXL error : "`, and `org.apache.commons.jexl3.internal/parser` prefix judgment)
  - `a6/c3` is the **JexlParser syntax constant table** generated by JavaCC (`JexlScript/Annotation/IfStatement/TernaryNode/…` full set of 100+ node names)
  - `a6/u2`, `x5/k` processing `jexl.import` / `jexl.options` / `jexl.namespace.*`
  - `z5` contains `org.apache.commons.jexl3 { JexlBuilder {} }`
- **single file determination** (`p0/y.e(file, StringItem)`) [observation, `p0/y.java:101-124`]:
  - `Files.size(path) > 20971520` (20 MB) → Skip
  - `t0.j.b(item)` true → `RustSupport.matches(bytes, pattern)` (**binary characteristics + `?` wildcard**)
  - Otherwise → `RustSupport.search(bytes, value)` (**text/regular**)
  - `-1` is regarded as a mismatch; `ClosedFileSystemException` → `CancellationException("External cancellation")`
- **Rust engine = Aho-Corasick** [observation, `libflamingo.so` embedded crate information `aho-corasick-1.0.1`, `rustc-demangle-0.1.21`]
- **scanning target mode** [observation, `ScanTarget`]: `/AndroidManifest.xml`, `/*.dex`, `/META-INF/MANIFEST.MF`, `/lib/**/*.so`, and `SpecifiedFile`.

### 5.3 Scan facade `n0/g` (`g.z()`) [Observation, 56 KB / 1450 lines]

Static fields: `f6412h`=number of active tasks (upper limit 10), `f6413i`=taskId→task, `f6414j`=single instance;
Instance 6 `ConcurrentHashMap` (observer→Number of Completed Engines/Tasks/Results/Signature Cache/Gray Mark/scanType).

| public method | Semantics |
|---|---|
| `z()` | Singleton; first construction execution `x.a.l()` (engine enabled initialization + possible cloud control pull) |
| `L(paths, observer, useCloud, scanType, extras)` | **scans the main entrance**: filter → current limit (≥10 reject) → build 6 tables → start `n0.g$f`; return the number of tasks |
| `M(paths, observer, useCloud, scanType, extras)` | **scene distribution**: `scanType==1` → `C()` incremental; otherwise → `L()` |
| `C(observer, useCloud)` | **incremental scan** (`n0.g$e` AsyncTask: packaged + `MediaStore` APK full table → cache hit and level∈{2,3,6} direct playback) |
| `N(observer)` | **virus database update task** (≥10 rejected) |
| `O(list)` / `P(list)` | Filter the whitelist by **package name** / by **apk path** |
| `q(id)` / `r(id)` | cancelScan / cancelUpdate |
| `s()` | checkUpdate —— **constant `return -1` (pile)** |
| `J(name, enable)` / `w(name)` | Write/read engine enabled (**Mi will never be written to**) |
| `F(name)` | Whether cloud scanning is supported → Constantly returns `f3551f` in the enumeration (all three engines are true, **is a constant true value** that has nothing to do with the engine) |
| `x()` | Engine name array → **`new String[4]` Only fill in 3, the last bit is always null (bug)** |
| `y(name)` | Engine display name |
| `D(a, b)`（private） | **multi-engine results merge**: according to `virusLevel` priority **2 > 3 > 6 > 4 > 5 > 1** override, `extras` string splicing |
| `K(...)`（private） | Starting engine thread (one `n0.g$b` Thread for each engine) + buried point `security_scan` |
| `v(ctx)`（private） | `MediaStore.Files` Check `%.apk` → Full APK list |

**concurrency model** [observation]: **engine-level parallelism, file-level serial** (`sleep(5)` milliseconds per file), task upper limit 10.

### 5.4 Scan type code semantics (**corrects common misreading**) [Observation]

- `GuardProvider.sync_scan` passes `scanType = 2`; `SecurityService.w()` passes `0`; `p()` passes the value given by the caller.
- `M()` only increments when `==1`; the remaining value of **is passed to `L()` as it is, and in `L()` it is just the "progress offset"** (`observer.s(i8 + offset, …)`).
- → **`2` is not "synchronous scan mode"**, just a progress baseline offset. The `engineName` (4th parameter) of `install_scan` is only passed as `extras` all the way to the 4th parameter of `f0.a.j()`. All four engine implementations of **ignore it** → **The semantics of "scan by engine name" do not exist**, the engine set is always determined by `x.a.c()` (SP enablement item).

### 5.5 Result model `y.f` (VirusInfo) [Observation, 15 fields]

| Field | Type | Semantics | Notes |
|---|---|---|---|
| `f11657b` | String | `path` (`cacheDir/copy.apk` when content://) | |
| `f11658c` | String | `packageName` | |
| `f11659d` | String | `virusName` | AVL `getVirusName()` / TENCENT `entity.virusName` / NG `threatName` |
| `f11660e` | String | `virusDescription` | |
| `f11661f` | String | `engineName` (`"Mi"`/`"AVL"`/`"TENCENT"`/NG hard-coded `"MiEngineNG"`) | |
| `f11662g` | String | `versionName` | |
| `f11663h` | String | `adName` | **whole tree 0 assignment (not implemented)** |
| `f11664i` | String | `adDescription` | **whole tree 0 assignment** |
| `f11665j` | int | `versionCode` | **does not serialize** |
| `f11666k` | int | `virusType` | **is actually only 0/1** |
| `f11667l` | int | **`virusLevel`**: 1=Safety 2=Virus 3=Risk 4=? 5=Gray production 6=Gray production/anti-fraud | 4/5 Do not report broadcast |
| `f11668m` | int | `adLevel` | **0 assigns the value**; it is written but not read. |
| `f11669n` | boolean | `isAuthoritative` | **does not serialize** |
| `f11670o` | boolean | `isInCache` | **0 assigns value, does not serialize** |
| `f11671p` | String | `extras`, format `"<engine>:<safe\|danger>;"` | **string splicing** when merging multiple engines |

**`extras` Actual measured value** (can be used as a fingerprint for "which engine determines"): `Mi:safe;` / `Mi:danger;` / `AVL:safe;` / `AVL:danger;` / `TENCENT:safe;` / `TENCENT:danger;` / `MiEngineNG:safe;` / `MiEngineNG:danger;` / `AntiDefraud:danger;`

**Parcel asymmetric defect** [observation]: `writeToParcel` writes **12** values, `f(Parcel)` reads **10** → after cross-process `adLevel` Lost; `versionCode` / `isAuthoritative` / `isInCache` does not participate in serialization at all.

The truth about **threat classification** [Observation]: `res/values/strings.xml` has 12 `threat_type_*_desc` (adware/backdoor/default/exploit/hacktool/monitoring/rootkit/spyware/trackware/trojan/virus/worm), but **business code 0 references** (only `R.java` that comes with TMSDK constant). `virusType` is actually **binary value**: `1` = detected (AVL/Tencent), `0` = not assigned. The real threat classification relies on the `virusName` string + `virusLevel`, which is mapped by the upper layer (security center).

---

## 6. Data layer: 5 libraries / 15 tables

### 6.1 Encrypted Room library (v2, 8 tables) [Observation, `EngineDatabase_Impl`]

| # | table | Column | unique index |
|---|---|---|---|
| 1 | `trust_certs` | `id` PK AI, `developer` BLOB, `fingerprintSha256` BLOB | `index_trust_certs_fingerprintSha256` |
| 2 | `trust_installers` | `id`, `packageName` BLOB, `vendor` BLOB | `pk_index(packageName)` |
| 3 | `innocent_package` | `id`, `packageName`, `signaturesSha256`(NULL), `signaturesMd5` | `index_innocent_package_packageName_signaturesMd5` |
| 4 | `characteristics` | `id`, `name`, `strings`, `condition`, `threatName`, `version` | `index_characteristics_name` |
| 5 | `guilty_package` | `id`, `packageName`, `signaturesMd5`, `signaturesSha256`(NULL), `versionCode`(NULL), `threatName`, `mfSha256`(NULL) | `index_guilty_package_packageName_signaturesMd5_mfSha256` |
| 6 | `black_gray_chs` | `id`, `name`, `condition`, `strings`, `version` | `index_black_gray_chs_name` |
| 7 | `black_gray_app` | `id`, `packageName`, `signaturesMd5`, `signaturesSha256`, `versionCode`(NULL), `mfSha256` | `index_black_gray_app_packageName_signaturesMd5_mfSha256` |
| 8 | `version` | 7 integer version numbers + `id` PK (**without autoincrement**) | — |

+ `room_master_table`（identity `8856aeeb52eb3fc27eb4570c1cd903b7`）
**migrates** [Observation]: The migration array is empty → **destructively migrates** (version upgrade means clearing and rebuilding the database).

### 6.2 Rank AES Encryption (**This report has been independently recalculated and verified**) [Observation]

| item | value |
|---|---|
| algorithm | `AES/CBC/PKCS5Padding`（`v0/j.java:59,79`） |
| Key source | classpath `META-INF/flamingo.dat`, `skip(144)` read 16 bytes (`v0/j.java:38-41`) |
| Key transformation | The first 8 / last 8 bytes are respectively **big-endian long**, respectively `^ 4831221107097930563` (=`0x430BF1FFFFF10B43`) |
| **restore key** | **ASCII `wcw1xbplx7aijwqr`** |
| IV | `SecureRandom` random, **preceded by**: output = `IV(16) ‖ ciphertext` |
| `EncryptVersion` | Heng `"0"` |

**Lead independent verification** [observation]: directly read the 16 bytes of `META-INF/flamingo.dat` offset 144 in APK
`34 68 86 ce 87 93 7b 2f | 3b 3c 90 96 95 86 7a 31`
→ LE int64 `0x2f7b9387ce866834 ^ 0x430BF1FFFFF10B43 = 0x6c70627831776377` → `wcw1xbpl`
→ LE int64 `0x317a869596903c3b ^ 0x430BF1FFFFF10B43 = 0x7271776a69613778` → `x7aijwqr`
→ Splicing **`wcw1xbplx7aijwqr`** (16 bytes ASCII)✅

**The design flaw** derived from this [inference]: IV is random every time ⇒ The same plaintext is different every time ⇒
1. The `UNIQUE` index **on the three tables cannot deduplicate** (the same APK inserts new rows in each round of synchronization);
2. `WHERE packageName = ? AND signaturesMd5 = ?` This type of equivalent query **is almost impossible to hit**;
3. The actual matching path can only be `r0/a.b()` **full table loading → memory decryption → memory comparison** (the code is consistent with this).
→ The library will grow **infinitely to**, and the index will be useless (performance defect, not security defect).

### 6.3 Four plaintext SQLite [observation, t-core]

| library file | version | table | Writer | reader |
|---|---|---|---|---|
| `miui_antivirus.db` | 2 | `antivirus_db_version(version, release_time)`；`antivirus_db(package, version, signatures, md5, type, virus_name, description, strategy, descType)` | `i0/d` (`/antivirus/viruses` issued) | `i0/l:308` (`package=?` item by item) |
| `mi_engine_white_virus.db` | 1 | `mi_virus_white_db(package, signatures, path)`；`mi_detect_app_db(package, signatures)` | `i0/f:29-72`（`/antivirus/whitelist`） | `i0/f:77-118` |
| `risk_gray_result.db` | 1 | `risk_gray_result_db(packageName, versionCode, signatures, type, time)` | `c0/b:113-152`, `MiEngineNG:120-127` (write type=5) | `c0/b:97-121` |
| `scan_result.db` | 2 | `scan_result_db(path, packageName, virusName, virusDescription, engineName, versionName, versionCode, virusLevel, extras, sign, time)` | `c0/d:205-288` | `c0/d:179-203` |
| **`miui_virus.db`** | **5** | `t_virus_white_list(_id, scan_result_type, scan_item_type, app_label, desc, dir_path, pkg_name, virus_name, virus_md5)`；`virus_white_db(_id, package, path, version_code, size, engine, local_version, extra)` | `GuardProvider.insert/delete` | `GuardProvider.query`、`i0/f:88-111` |

`miui_virus.db` upgrade path [observation, `w0/h.java`]: v1 → 2 add `version_code/size` → 3 add `engine/local_version` → 4 build `t_virus_white_list` → 5 add `extra`.

### 6.4 Two dimensions of whitelist determination [observation]

```
e(PackageInfo) → Get signature → c(sigs) Splice MD5 (uppercase hex, use "|" to connect multiple signatures)
  → d(pkg, sigMd5):
      b(pkg, sigMd5) // i0/f:37-39 is the dead hook of `return false` (the built-in whitelist has been eliminated)
      SELECT ... FROM mi_virus_white_db WHERE package=? AND signatures=?
f(path) → w0.a.a(path) (full file MD5) → query("content://guard/virus", null, "virus_md5 = ?") // Call back your own Provider
```

---

## 7. Cloud interface

### 7.1 Two sets of network stacks [Observation]

| stack | Technology | base address | endpoint |
|---|---|---|---|
| **new** | Retrofit + OkHttp + Gson | `https://virus-engine.sec.miui.com` (International `…sec.intl.miui.com`, `q0/m.java:12`) | GET `/virus/{blocklist,allowlist,feature/codeList}`、`/blackGrays/{blocklist,feature/codeList}`；POST `/virus/scan`、`/virus/localResult/report` |
| **Old** | `HttpURLConnection` | `api.sec.miui.com` / `flash.sec.miui.com` | POST `/antivirus/viruses`、`/antivirus/whitelist`、`/detect/app` |

4 OkHttp Interceptor: **signature / request encryption / response decryption / network gate**.

### 7.2 Signature and Encryption [Observation]

| item | value |
|---|---|
| Signature algorithm | **MD5 (sorted `k=v` splicing + static salt)** —— **no HMAC, no nonce, no timestamp anti-replay** |
| Old stack salt (virus database/whitelist) | `94e6cec2-19dc-4bb6-823c-613f72d3ea16`（`i0/d.java:55,203`） |
| Old Stack Salt (Lightning Detection) | `6988567a-4220-4b51-bc2d-ccdec27a74a1`（`j0/c.java:75`） |
| Xinzhan Salt | `62b36629-ecb4-4725-9f33-ccb0fd158191`（`q0/q.java:22`） |
| body encryption | AES + gzip, header `EncryptVersion=0` |
| TLS | System default verification, **has no pinning, no custom TrustManager, no shutdown verification** |
| Device identification | Old stack `mi` = `MD5(android_id + "-" + appKey)`, `oa` = OAID; new stack `uuid` external injection |

[Inference] The signature **can be forged offline** (the salt is a static constant, no anti-replay), but if the server verifies the source IP/TLS client certificate, it is another matter - no client certificate is seen in this round.

### 7.3 Cloud scanning: **only transmits fingerprints, not APK** [observation, `p0/f.k()`]

```
CloudScanApkInfo(
  packageName         = aVar.getPackageName(),
  versionCode         = String.valueOf(aVar.d()),
  apkCertificateMD5   = aVar.f(),
  apkCertificateSHA256= aVar.b(),
  mfFileSHA256        = aVar.g() ?: ""
)
```
**There is no `RequestBody.create(File)` and no `MultipartBody`** in the whole tree ⇒ There is no code path for uploading APK/dex/so.
→ The cloud judgment is only that the list of **"package name + version + certificate + MF fingerprint" quadruple matches**, not code-level cloud scanning.

**trigger condition is bit field, not "local inconclusive"** [observation, `p0/f.java:348-379`]:

| bit | mask | meaning |
|---|---|---|
| 0,1 | `& 3` | Threat scan mode |
| 2 | `& 4` | Black and gray scan mode |
| 3 | `& 8` | **Disable cloud scanning** |
| The rest | — | The entire original value is reported as `scanType` |

Default `f7380c = 3` (threat mode, cloud scan allowed). Manifest's `guardprovider.scan_properties=2` **reads** (dead configuration) at 0 locations in the entire tree ⇒ **Once cloud scanning is allowed, all items to be scanned will be sent to `/virus/scan` in batches, and there is no "upload only suspicious items" filter**.

**responds to consumption** [observation]: `resultType ∈ {Clean, Threats, BlackGrays, Unknown}`; Threats → `t0.d.c()` infection; BlackGrays → `t0.d.b()` gray production; conclusion **does not write to Room**, only changes `t0.a` in memory Status, `authority="cloud"`, `version=-1`. The **index alignment assertion is written as `payload.packageName != apkInfo.packageName`, which means the error** is reported (it is suspected that `!=` is written as `equals`, which is a suspicious implementation).

### 7.4 `flash.sec.miui.com/detect/app` = Anti-fraud list reporting (**non-sample-by-sample cloud scan**) [Observation]

`j0/c.java:75`: POST JSON `{timestamp, os, biz_id:"virus_scan", uuid, content[], sign}` → respond to `{data:[{pkg, signature, version, basisOf}]}`.
[Inference] **reports the full list of non-system applications ←→ and issues the "distrust list"**, and the entries of `basisOf==2` constitute the return set of `SecurityService.j()`. This is the **anti-fraud/gray product image** channel, which is two different links from the sample-by-sample verification in §7.3.

### 7.5 Endpoint summary list (28 entries: 22 host names + 6 bare IP: ports) [Observation]

| domain name | Belong | trigger | Plain text? |
|---|---|---|---|
| `api.sec.miui.com` | GuardProvider | Virus database/whitelist synchronization | HTTPS |
| `flash.sec.miui.com` | GuardProvider | Anti-fraud list reporting | HTTPS |
| `virus-engine.sec.miui.com` / `….intl.miui.com` | GuardProvider | Rule base synchronization + cloud scanning + result reporting | HTTPS |
| `staging.api.sec.miui.com` | — | **only appears in network_security_config clear text whitelist** (test residue) | **HTTP allows** |
| `api.sec.miui.com/policy/proxy?p=antiy\|tencent&l=…` | GuardProvider | Engine Privacy Policy Page | HTTPS |
| `tracking{.intl,.india,.rus}.miui.com` | OneTrack | Incident reporting | HTTPS |
| `sdkconfig.ad.{,intl.,india.,rus.}xiaomi.com`、`api.ad.xiaomi.com` | OneTrack | pull configuration | HTTPS |
| `tmfsdk.m.qq.com` | TMSDK | Cloud check/WiFi/Statistics | HTTPS |
| `tmfsdk4.m.qq.com` | TMSDK | IPv4 reporting | HTTPS |
| `tmfsdktcp.m.qq.com:8001` / `tmfsdktcpv4.m.qq.com:8002` | TMSDK | TCP long connection | private protocol |
| `120.241.190.227:8002`, `61.241.44.59:8002`, `14.18.175.81:8002` + 3 IPv6 | TMSDK | **IP directly connected to the bottom of the pocket** (around DNS) | private protocol |
| `mmgr.gtimg.com/qqsecure_config_update/qqsecure_config_update` | TMSDK | Feature database update list | HTTPS |
| `tools.3g.qq.com`（`/wifi/cw.html`、`/j/sslstrip`） | TMSDK WifiDetect (code name **Meri**) | Network authentication/hijacking/SSLStrip detection | **HTTP** |
| `m.qq.com`、`www.qq.com:80`、`miwifi.com/diagnosis/index.html` | TMSDK | SSLStrip comparison / TCP connectivity | Naked TCP |
| **(none)** | **AVL** | Cloud check/log/URL rule update - **3 .so and Java have no business domain name**, full config driver | — |

[Observation] `res/xml/network_security_config.xml`: Only `staging.api.sec.miui.com` and `tools.3g.qq.com` allow plain text, and the system defaults to TLS for the rest.

---

## 8. Native layer

### 8.1 12 `.so` identity tables [observation, REA + byte level]

| File | SONAME | JNI form | Qualitative |
|---|---|---|---|
| `libams-1.3.0-mfr.so` | Same name | **8 `Java_tmsdk_…_AmScannerV2_*` directly export ** | TMSDK AMF feature scanner (with `70003.amf`) |
| `libavlasys.so` | Same name | None `Java_*`; `JNI_OnLoad` + RegisterNatives | AVL SDK for JNI case (registered `AVLA`/`NativeUtils`) |
| `libavlm.so` | Same name | None (Pure C, **98 `AVLM_*`**) | **Antiy engine body** |
| `libavlurl.so` | **`liburl.so` (≠ file name)** | None (Pure C, 33 `AVLM_Match_*`) | Antiy URL Reputation Engine |
| `libdce-1.1.21-mfr.so` | Same name | `JNI_OnLoad` + RegisterNatives | TMSDK DeepCleanEngine (including `nativeDeleteAllChildren` and other deletion primitives) |
| `libflamingo.so` | **empty** | **3 `Java_com_miui_…_RustSupport_*`** | **Xiaomi self-developed Rust Aho-Corasick matching engine** |
| `libtmf-gm.so` | Same name | **8 pieces `Java_com_tencent_tmf_gm_{Sm2,Sm4}_*`** | National Secret SM2/SM4 JNI |
| `libTmsdk-2.0.13-mfr.so` | **`libTmsdk-2.0.12-mfr.so`** | `JNI_OnLoad` + RegisterNatives (5 exports) | TMSDK core (TccCryptor/differential package/yellow pages) |
| `libturingvd.so` | Same name | `JNI_OnLoad` + RegisterNatives | Tencent Turing device risk control (root/emulator detection) |
| `liburldetectorsys.so` | Same name | `JNI_OnLoad` + RegisterNatives | Ultra-thin dlopen forwarding case for `UrlDetector` |
| `libxcrash.so` | Same name | `JNI_OnLoad` + RegisterNatives | xcrash crash capture main library |
| `libxcrash_dumper.so` | **empty + yes `PT_INTERP`** | None | xcrash **independent dumper executable** (ptrace catches tombstone) |

**JNI Contract (Lead independent scanning confirmation)**:
- `libflamingo.so` has only 3 exports, which correspond to the 3 native methods **of `RustSupport` one by one**, including the overload signature `search___3B_3Ljava_lang_String_2` (=`search(byte[], String[])`, **has no call point**).
- 8 JNI methods of `libams` = all entries of Tencent AMF scanner (`newObject/deleteObject/initScanner/getOpcode/extractApkInfo/scanApkBytes/nativeLoadAmfHeaderBytes/nativeUpdateMalwareInfoBytes`).
- The `RegisterNatives` table of **6 dynamic registration libraries is plain text** in `.rodata`, and the class name + signature have all been taken out (t-native):

| Library | Registered Java class |
|---|---|
| `libavlasys` | **`AVLA`** + **`NativeUtils`** (using the self-built AArch64 `ADRP+ADD` decoder to mechanically prove that `JNI_OnLoad` only references these two class names) |
| `libTmsdk` | `TccCryptor` / `TelNumberLocator` / `NumMarker` / `YellowPages` |
| `libdce` | `QFile` / `QSdcardScanner` / `DeepCleanEngine` |
| `libturingvd` | `TNative$aa` (including `$aa$bb.onServiceConnected`) |
| `liburldetectorsys` | `UrlDetector` |
| `libxcrash` | `NativeHandler` (`nativeInit` with 31 parameters) |

Total: **5 libraries carrying 7 taxa, 21 direct `Java_*` exports + 6 RegisterNatives registration tables (about 60 methods) **.
**Tool Limitation (Important)**: Ghidra of native REA on Windows **only accepts PE/6502**, rejects ELF-arm64 (`target_format_unsupported`); Hopper requires macOS; IDA/Kuna Not configured ⇒ **has no available decompilation path** in this round, and the native conclusions all come from ELF structure analysis + plain text symbols/strings + AArch64 instruction decoding.

### 8.2 8 assets identification [observation]

| asset | size | Judgment |
|---|---|---|
| **`avlsdk`** | 1,566,355 | **ZIP, 26 items = Antiy signature library package**. `av/avl/android/*.avl` (`avlpk_amc` 54KB, `avlpk_apn` 132KB, `avlpk_des`, `avlpk_dhc`, `avlpk_kw`, `avlpk_pornware` 322KB, `avlpk_sfmf` 81KB, `avlpk_sgnl`, `6947e41c_60f53057.avl`) + `url/url/*` (`fish.avl` 151KB, `fish_url.avl` 417KB, `white.avl` 370KB, `*_re.avl`) + Configuration + Chinese and English behavior description `.des` |
| `avlsdk:sdk_conf/version.conf` | 106 | **clear text JSON**: `{"sdk.conf":"1","av.siglib":"20220928.ah","av.engine":"351","url.siglib":"20180917.zz","url.engine":"124"}` |
| `avlsdk:sdk_conf/sdk.conf` | 10,379 | The **structure has been accurately parsed** (t-native review, revised Lead preliminary judgment): 380 B before **is pure ASCII** (version header `00000` + hex segment + 253 B base64), **ciphertext from From `0x17C` total 9,999 B**. Because 9,999 is not a multiple of 16 ⇒ **AES/SM4 The whole block mode structurally excludes**, which has nothing to do with the key (Lead's trial solution failure with `wcw1xbplx7aijwqr` is inevitable, not an error in the key). [Unknown: stream cipher/custom/compression? 〕 |
| `avlsdk:av/kw/common/{en,zh}/*.des` | 477~1,212 | **plain text JSON copywriting template**. `behavior.des` reveals **14 behavior label**: `prv` steals privacy / `rmt` receives remote instructions / `pay` malicious deductions / `spr` automatically spreads / `exp` consumes data and sends text messages / `sys` damages the system / `fra` disguises fraud / `rog` takes up resources to bomb ads / `ads` pushes ads / `sms` frequently sends text messages / `spy` spy steals secrets / `bkd` backdoor / `rtt` privately elevates privileges / `lck` lock screen ransomware |
| `avlsdk:*.avl` magic number | — | `41 56 4C` = **`"AVL"`** |
| **`70003.amf`** | 680,928 | **TMSDK Anti-virus signature base library**: magic number `"amf\0"` + `u32=4`; `0x18..0x1F` two u32 timestamps `0x5FF6D2BD` / `0x622575E6`; followed by **42,556 items × 16 B The ciphertext record** (42556×16=680,896, consistent with the file size). Matched with `nativeLoadAmfHeaderBytes` of `libams`, the number is as shown in TMSDK `UpdateConfig` (`70002`=EN library, `70003`=virus library, `70701`=payload) |
| **`mdetector`** | 136,636 | **`\x7fELF` + `ET_DYN` + `EM_ARM`(40) + `PT_INTERP=/system/bin/linker`** ⇒ **ARM32 executable ELF (not .so)**. Static link libpcap (machine certificate: yacc symbol `pcap_lval/pcap_text/pcap_nerrs`); string `"arp packet size should be 42 bytes or more!"`, **`"!!This Device is being Hijacked!!"`**, `"send msg to java:"` (the result is hit to stdout and read by Java) ⇒ **passive sniffing ARP Detector** that determines gateway hijacking. The **loading method has positioned**: `Runtime.exec` is called up - `tmsdkobf/t9.smali:563`, `com/avl/engine/j/b/e.smali:217,383` (two places). **Note: APK only provides arm64-v8a, this 32-bit body cannot exec on pure 64-bit models ⇒ The function is silently disabled** |
| **`40706.dat`** | 9,368 | **TMSDK certificate MD5 whitelist** (number 40706). `fe 2a cf f7 …` High entropy; **8 byte aligned** (9368 = 1171×8) ⇒ Packet/streaming encryption features, using TMSDK’s own `TccCryptor` (key built into `libTmsdk`) |
| `licence.conf` / `licence1.conf` | 392 / 368 | The **algorithm has been restored to** (t-native) at the source code level: **`[128 B RSA/ECB/PKCS1Padding public key operation][DES/ECB/NoPadding]`** → After solving, it is `Properties`. Double confirmation of length divisibility (264/8, 240/8 to be exact). The built-in RSA certificate **`NotAfter 2012-02-24` has expired**, and depopulation failed ⇒ The **authorization chain in this package cannot be actually downgraded by the**, TMSDK authorization function. Attachment: `avlsdk:av/avl/conf/liscense.conf` and `url/…/liscense.conf` (404 B each) are both encrypted |
| `dexopt/baseline.prof{,m}` | 127 / 55 | `pro\0"010\0"` / `prm\0"002\0"` ART profile, normal |

### 8.3 High-risk native capabilities [observation + inference]

| # | discover | Evidence | Judgment |
|---|---|---|---|
| N1 | `libavlm` / `libavlurl` / `libavlasys` **has complete network outgoing capabilities** (`socket/connect/send/recv/gethostbyname/inet_addr/select`) without going through the Java network stack | import table | [Inference] It can bypass `NetworkSecurityPolicy`/Agent/StrictMode and directly connect to any IP. But there is no hard-coded C2**in** (`libavlm` only has the inherent string `www.baidu.com` + NTP pool + OpenSSL/w3.org; ~400 domain names of `libavlurl` are **Public Suffix List + The list of free secondary domain hosts** is a defensive list, **is not the backlink address**) |
| N2 | **`assets/mdetector` is the ARM32 executable** hidden in assets | ELF header + `PT_INTERP` + libpcap symbol | **high-risk form**: Bypassing the visibility of the `lib/` directory to PackageManager/security software. The function is ARP hijack detection |
| N3 | `libtmf-gm.so` import **`rand/srand/time`, no `getrandom`/`/dev/urandom`** | import table | [Unknown] If the random number `k` of SM2 is taken from `rand()`, it will not be predictable (a classic high-risk state secret defect). Disassembly is required for confirmation. Comparison: `libflamingo` Correct use of `getrandom` |
| N4 | `libturingvd.so` **real root detection + emulator detection + `popen`** | String `/system/bin/su`, `/system/xbin/su`, `daemonsu`, regular process name `^/system/xbin/ku[.]sud$\|^daemonsu:\|^kworker/[1-9][[:digit:]]{2}$\|^permmgrd$\|^360sguard$`; import `popen/getuid/readlink/statfs` + `ASensorManager_*` (sensor fingerprint) | [Observation] **is the real**, behavioral simulator detection (non-string matching). `popen` gives any shell primitives to the native layer |
| N5 | **None Frida/Xposed detects** (two hits are verified by context as `blackfriday`/`exposed` false positives); **None `TracerPid` detects**; no memory scan/process injection | 17 sets of regular expressions × 12 libraries | [Observation] Important **Negative conclusion**, avoid exaggeration |
| N7 | `libavlasys` hard-coded string `62BAFD…739` | string | [Unknown] If it is an authorization key, it can be used to forge the license. |

### 8.4 `libavlm.so` of C API (98 pieces, full quantity) [observation, REA]

- **scans**: `AVLM_Scan`, `ScanEx`, `ScanExWith{Id,Name,Type,File,Path,Key}`, `ScanWithArgs`, `ScanWithScanOpt(Ex)`
- **life cycle**: `Init`, `InitEx`, `InitEnhancedEng`, `Release`, `ReleaseEnhancedEng`
- **option**: `SetScanOpt`, `SetScanCategoryOpt`, `SetEngineMode`, `SetDEXScanLimit`, `SetOpcDEXScanLimit`, `SetScanLimit(Ex)`, `SetModulePath`, `SetAppInfoExtractSwitch/Rate`, `SetOutputOpt`
- **updates**: `Update`, `UpdateCheck(Ex)`, `UpdateCancel`, `UpdateRate`, `GetUpdateNetTrafficUsed`, `GetLastTotalUpdateTime`
- **results**: `GetVirusDesc`, `GetVirnameEx`, `GetVirKnowledgeEx`, `GetWhitenameEx`, `Result_Free`, `FreeVirusDesc`
- **APK metadata**: `GetAPKInfo`, `GetPackageName`, `GetCertHash`, `GetCertFullHash`, `GetMfMD5`, **`InstallPackage`**
- **official application anti-counterfeiting**: `CheckOfficalAPP` + `OfficalAPPResult_Get{Type,APPName,AntiyName,PKGName}` + `CheckAPPToken`/`ComputeAPPToken`/`CheckAPPKey`/`ComputeAPPKey`
- **log cloud upload**: `LogUpLoad`, `SetLogOpt(WithKey)`, `SetLogPathForSDK`, `WriteLogMessage`, `GetLogContent`, `GetELogInfo`, `GetDetectLogInfo`, `FlushLog`, `LogFlush`
- **licenses**: `GetSIDInfo`, `FreeSIDInfo`, `GetPubkeyContent`, `FreePubkeyContent`, `VirNamesChoice`, `AVLPack`
- **Suspicious short symbol** (`size=4` springboard, AVL confusion style): `wtsr/effe/aesd/zavt/brmy/adce/haoz/exdz/fffe/efff`
- **string confusion**: `.rodata` has conjoined blobs `http://.https://.Content-Length:.www.baidu.com.GET` and `SMS_Send.SMS_Block.System_DexLoad.System_ReflectInvoke.System_ReflectGet.Notification…` ⇒ **fixed-length string table + alias export**, non-packed

[Inference] `AVLM_LogUpLoad` + `GetSIDInfo` + `ComputeAPPKey` is the technical point of "uploading device ID to Antiy server" in the privacy document; whether **is actually called depends on the AVL configuration (`sdk.conf` encryption, [unknown])**.

---

## 9. Third Party SDK: Capability vs Actual Enablement

### 9.1 The truth about the composition of class libraries [Observation]

Among the 3,231 classes, the **real business contains about 25 confusion packages** (`p0 t0 r0 s0 q0 u0 v0 g0 j0 c0 h0 l0 m0 x0` + buried points `v/u/z` + attributes `c5/d5/f5/w`); the rest are libraries:

| Library | package |
|---|---|
| **kotlin-reflect + embedded Kotlin compiler frontend (max, ~45 packages)** | `c4 l2 o2 o3 u2 g4 b3 b4 m4 m2 w2 w3 v2 v3 y2 i2 k2 d3 h3 j3 t3 z2 s3 n3 c3 i4 q3 r2 f2 c2 e2 u1 y1 h4 p2 p3 d2 g2 j4 a4 e4 u3 h2 l4` |
| kotlinx-coroutines | `s4 x4 u4 z4 w4 v4 t4 y4` |
| kotlin stdlib | `w1 v1 h1 i1 o4 p4 q4 k4 b2 r4 g1 k1 l1 m1` |
| **Apache Commons JEXL3** (regular expression engine) | `v5 a6 x5 y5 z5` |
| Gson | `l o n p q r t m c d e6` |
| OkHttp / Okio | `g5 l5 m5 p5 q5 i5 n5 o5 j5 s5` / `t5 u5` |
| Retrofit / protobuf / Room | `f6` / `m3 f3 h3` / `h i j e g` |

[Observation] `META-INF/services/*`’s SPI registration counter-evidence in 5 places: `i2.a`←`z3.b` (kotlin BuiltInsLoader), `o3.g`←`u2.n/u2.l/u2.t` (kotlin-reflect Coverability), `s4.d0`←`t4.b` (CoroutineExceptionHandler), `x4.v`←`t4.a`, `javax.script.ScriptEngineFactory`←`b6.a` (**empty implementation**).

### 9.2 Tencent AVL (class 411 + 3.so + avlsdk)
- **capabilities**: multi-scan API, current limiting, certificate analysis, cloud search, log reporting, official application anti-counterfeiting, URL reputation, engine/library update
- **actually enables** [observation]: only `scan` / `scanURL` / `update` / `updateURLRule`; **logs off (`SetLogOpt` is not enabled), no cloud configuration** (`sdk.conf` encryption)
- `AVLScanOption` Five option bits: Category `0x7FFF` / Log `2` / Mode `0` —— **APK Zero use**
- Result model: `Risk/*` → level 2; `Trojan/G-Ware/PornWare/RiskWare/Tool` → level 1; `White` → filter
- appkey `D5DFB878335A3EF4E76F60ED470B8EC8` reads from its own meta-data (`a/e.java:39`)

### 9.3 Tencent TMSDK (class 432 + 6.so)
- **capabilities**: APK scanning (local + cloud), signature database update, URL reputation, deep cleaning, SMS/traffic SMS analysis, number tag, WiFi category seven detection, offline video metadata
- **actually enables** [observation]: only **3 Manager** are active
  - `QScannerManagerV2` (scanType 6, cloud check cmd **2016**)
  - `UpdateManager`（`mmgr.gtimg.com`）
  - `WifiDetectManager` (network / DNS phishing (cmd **794/800**) / ARP / encryption type)
- **calls** with all zeros: `UrlCheckManager`, `TccCryptor` (except string decryption), `DeepCleanEngine`, `TrafficSmsParser`, `VideoMetaRunner`, `sms_check_v4` (only 3 exception classes remain)
- **dead code**: `com.tencent.tcuser.util.Encode` static block `System.loadLibrary("xy")`, but **APK does not contain `libxy.so`** ⇒ Once loaded it must `UnsatisfiedLinkError`

### 9.4 Xiaomi OneTrack (Class 163) + xCrash
- appId `2882303761517522561`（`z/b.java:22`）
- The actual event **is only 6** [observations]: 4 services (`MiEngineNG_Scan`, `threats`, `blackgrays`, `blackgrays_threats`) + 2 SDK automatic (`onetrack_dau`, `onetrack_bug_report`)
- Report link: gzip → **AES/ECB** → 3 SQLite queue tables (`events`/`events_cloud`/`monitor`) → Retry → `tracking.*.miui.com`
- **xCrash 2.4.9**: Only `filesDir/tombstones` is placed, **itself does not upload**; OneTrack `CrashAnalysis` reads the summary and then goes to `onetrack_bug_report`; `testJavaCrash/testNativeCrash` is `public static` (debugging residue)

### 9.5 State secrets/framework/residues
- `com.tencent.tmf.gm.{Sm2,Sm4}`: **are all JNI** (`libtmf-gm.so` 8 exports), SM4 **default key hardcoded** `73 42 5A 55 18 08 06 5C 76 70 21 70 24 24 33 29` (`gm/b.java:7`); SM2 Public key issued by server (128 hex = X‖Y)
- **`com.sun.nio.zipfs`**: **depends on** - use `jar:` FileSystem to read the dex (`p0/e:155`) in APK, and rely on `META-INF/services/java.nio.file.spi.FileSystemProvider` to register
- **`android.content.pm.PackageParser/PackageUserState/VerifierInfo`**: **depends on** - `w0/d.a()` is used when SDK≥28 and the path is the **directory (split-APK layout)** `parsePackage` + `collectCertificates`, bypass the system `PackageManager` limit offline analysis of any APK
- **build residue (do not exaggerate)**: Firebase/FCM/Crashlytics has complete configuration strings (`google_app_id=1:484664152262:android:…`, `project_id=guardprovider-c3e77`) but **APK contains 0 `com/google/**` classes and no Manifest Firebase Component **; `libturingvd` + `TNative`** zero Java reference **; QR code `com.miui.qr` is just a string,**sources and smali double search 0 to implement **
- **installation interception/silent installation** [Observation]: **is only installed at native** (`AVLA.installPackage(path, mode, …)`), Java layer **0 `PackageInstaller`**; `GuardProvider` on install The scene only scans and does not install.

---

## 10. Privacy and data outgoing (8 enabled links)

| # | Collect | trigger point | storage | Where to go |
|---|---|---|---|---|
| 1 | `ANDROID_ID` + `Build.MODEL` | `TencentEngine.java:402` → `TMSDKContext.setDeviceInfo` | In-process static | TMSDK shark request (`tmfsdk.m.qq.com` / TCP 8001-8002) |
| 2 | WiFi `SSID`/`BSSID`/DHCP `DNS1/DNS2` | `TencentEngine.java:122` → `tmsdkobf/p9:210-254` | Request body only | Tencent Cloud Check cmd **794** (DNS hijacking/phishing determination) |
| 3 | Current BSSID + `ScanResult` list | `TencentEngine.java:107-114` | Not placing the order | Only local determination |
| 4 | **IMEI list (plain text)**, IMSI list (MD5) | `api/c.java:111,114`（`onetrack_dau`） | SQLite `events.data` (AES encrypted) | `https://tracking.miui.com/track/v4` |
| 5 | IMEI(MD5)、OAID、AAID | `f/b.java:304-305`、`util/g:45-46` | Same as above + SP `instanceId` | Same as above / `…/api/open/device/writeBack` |
| 6 | Package name/version/signature summary/installer/threat name of the scanned APK | `MiEngineNG.java:201` | SQLite `events` | `tracking.miui.com` |
| 7 | Crash scene (stack, `/proc/maps`, fd, logcat fragments) | `XCrash` placement → `CrashAnalysis:158-178` | `filesDir/tombstones/*.xcrash` | `onetrack_bug_report` → `tracking.miui.com` (summary only) |
| 8 | Any package name/version/label/icon/signature of APK | `GuardProvider.java:168`（`parseApk`） | Not placing the order | Returned to the caller with `Guard_Provider` authority |

**gating fact** [Observation]: OneTrack's IMEI/IMSI acquisition **is subject to zone switch gating** (`api/c.java:109`), and `Configuration.Builder`'s `f1855h`(IMSI)/`f1856i`(IMEI) **defaults to true**, while `z/b.java` **does not call `setImeiEnable(false)` Close** ⇒ In the allowed area, **collects IMEI/IMSI** by default. This is the most substantial privacy risk point of this software.
**is not enabled** [Observation]: Address Book/Clipboard/Location/SMS Text There is no call point in this APK; TMSDK "Installed Application List" is only triggered when the scan list is empty (this APK does not trigger).

---

## 11. Configuration surface (complete key table)

### 11.1 SharedPreferences `"GuardProvider"` [Observation, `w0/f.java:20`]

| key | Type | Semantics |
|---|---|---|
| `sp_key_mi_engine_enable` / `sp_key_avl_engine_enable` / `sp_key_tencent_engine_enable` | bool | Engine enabled (default false; **`J()` refuses to write Mi**) |
| `cloud_engine` | String | Current cloud engine name (can only be non-MI) |
| `cloud_engine_list` | String(JSON) | A collection of engine names issued by cloud control |
| `antivirus_is_init` | bool | Initializing Sentinel for the first time |
| `load_cloud_engine_time` | long | Cloud control pull timestamp |
| `virus_cache_validty_perod` | int | Cache validity period (days, default 3) - **original spelling `validty` (one less i)** |
| `key_uuid` | String | `UUID.randomUUID()` generated for the first time, reporting identity |
| `avl_url_sdk_version` | String | AVL URL library version (**only writes but not reads**) |
| `antivirus_db` / `antivirus_db_version` / `antivirus_is_init` / `cloud_cache` / `cloud_data` / `VIRUS_BASE_NAME` / `VIRUS_BASE_EN_NAME` / `virus_scan` / `virus_scanning` / `virus_name` | — | Old stack and native side keys (DEX string pool confirmed to exist) |

[Observation] There are also 4 sets of third-party preference files: `avl_conf` and `avl_<suffix>`, TMSDK self-built, and OneTrack self-built.

### 11.2 System Properties/Cloud Control/Settings [Observation]

| Source | key |
|---|---|
| `SystemProperties` (reflective `d0/d`) | `persist.sys.sc_allow_conn`、`ro.miui.customized.region`、`ro.miui.region`、`ro.miui.cust_variant`、`ro.miui.restrict_imei`、`ro.miui.ui.version.code/name` |
| `MiuiSettings$SettingsCloudData` (reflective `d0/b`) | Module `VirusScan`, `miEngineNgEnable`, etc. |
| `MiuiSettings$Privacy` (reflection) | `isEnabled(context, key)` |
| `Settings.Secure` | `antivirus_engine_network_enable`、`android_id` |
| SecurityCenter metaData | `securitycenter.support.mix.scan` (hybrid scan), `miui.support_full_scan` (judgment source of `guard/support_incremental`) |
| self metaData | `AVL_OPENSDK_APPKEY`, `guardprovider.support.mix.scan`, `guardprovider.scan_properties` - the latter two of **full tree 0 read** (dead configuration; statement read to **by others**) |
| `android.miui.AppOpsUtils.isXOptMode` (reflective `d0/a`) | XOpt mode determination |

---

## 12. List of known defects and risks (by severity)

### 12.1 Security/Privacy

| # | question | location | nature |
|---|---|---|---|
| S1 | **OneTrack is enabled by default IMEI/IMSI collects**, `setImeiEnable(false)` is not called | `Configuration.java:86-89` + `z/b.java` | Violates minimum necessity; misaligned with Tencent copywriting "Upload Encrypted IMEI" |
| S2 | **`sync_scan` accepts any `content://` URI and copies to `cacheDir/copy.apk`** | `GuardProvider.java:211-215` | **confused deputy**: Use the permissions of this process to read the provider content that the caller does not have permission to read. |
| S3 | **`parseApk` can resolve any path APK** (no path whitelist) | `GuardProvider.java:159-190` | Any APK metadata/signature/icon reader |
| S4 | **Fixed temporary file name `copy.apk` + `REPLACE_EXISTING` No size limit** | `GuardProvider.java:213` | Concurrency overwrites each other; large files exhaust cacheDir |
| S5 | **command injection plane**: `Runtime.exec("ping -i 0.2 -c<n> -w <t> " + host)`, `host` from IPC parameters and unfiltered shell metacharacters | `tmsdkobf/t9.java:84-95`, link `n0/k.c()` → `detectARPNoRoot` | Signature permission is required to access, but **does not have secondary verification** |
| S6 | **`tmsdkobf/nb.java` Open third-party application private DB** (`/data/data/com.qiyi.video/databases/…`, `com.sohu.sohuvideo/…`) with absolute path | `nb.java:36,74,126` | TMSDK Legacy; Reachability of this application [Unknown] |
| S7 | **TMSDK Writable directory `.so` Load**: `System.load(<cacheDir derived lib/> + name)`, try three naming options | `tmsdkobf/ka.java:12-74` | If the directory is writable (same signature/root), it means so hijacking interface |
| S8 | **`libturingvd.so` Use `popen` to execute shell** + read the sensor to make the device fingerprint | import table | native layer arbitrary shell primitives |
| S9 | **`libtmf-gm.so` uses `rand/srand/time` as random source** (without `getrandom`) | import table | `k` of SM2 if from `rand()` ⇒ Predictable [Unknown, needs to be disassembled] |
| S10 | **TMSDK Clear text HTTP** `http://tools.3g.qq.com/wifi/cw.html` | `p9.java:269` | MITM can forge `Meri` response header ⇒ Falsely report "hijacked" as "safe" |
| S11 | **bare IP directly connected to** (`120.241.190.227:8002`, etc.) | `w5.java:154-166` | Bypass DNS/domain name ban policy |
| S12 | **SM4 Default key hardcoded** | `com/tencent/tmf/gm/b.java:7` | If the TMSDK channel uses the default key instead of the session key, the captured packets can be decrypted offline. |
| S13 | **signature salt is a static constant, none HMAC/nonce/time stamp** | §7.2 | Requests can be forged offline and replayed |
| S14 | **`assets/mdetector` is the ARM32 executable hidden in assets, and `Runtime.exec` is used to call** (`tmsdkobf/t9.smali:563`, `com/avl/engine/j/b/e.smali:217,383`) | §8.2 | **high-risk form**: bypassing the `lib/` directory visibility and directly running the external executable; the result is returned to Java via stdout |
| S15 | **audit blind area**: `SecurityService` transaction 20/21/22/23 **does not judge user lock** | `SecurityService.java` | Incremental scanning and library clearing can be triggered during the lock screen period |
| S16 | **`assets/40706.dat`, `licence*.conf`, `avlsdk:sdk.conf` are all encrypted binary ** | §8.2 | Static unreadable [unknown] |

### 12.2 Functional defects/implementation bugs (affecting the protection effect)

| # | question | influence |
|---|---|---|
| B1 | `n0/g.x()` Return `new String[4]` Only fill in 3 ⇒ **last digit is constant null** | The caller will get the null engine name when traversing |
| B2 | `y.f` Parcel Write 12 Read 10 ⇒ `adLevel` is lost; `versionCode`/`isAuthoritative`/`isInCache` is not serialized | Advertising information and version information become invalid after cross-process |
| B3 | `n0/g.F(name)` always returns enumeration `f3551f` (all three engines are true) | **"Whether cloud scanning is supported" has nothing to do with the engine, it is a constant true value** |
| B4 | `n0/g.s()` (checkUpdate) **constant `return -1`** | `IAntiVirusServer.C` always reports "check failed" |
| B5 | `engineName` of `install_scan` is ignored by all four engines | **"Scan by engine name" function does not exist** |
| B6 | `scanType` only performs progress offset, not mode | semantic misunderstanding |
| B7 | `guardprovider.scan_properties` / `guardprovider.support.mix.scan` **0 Read** | Dead configuration |
| B8 | `i0/f.b(pkg,sig)` is the dead hook of `return false` | The built-in whitelist has been cut, and only the server is left to issue it. |
| B9 | `qax_dialog_*` Full `TODO`, zh version copy Tencent copywriting | QAX engine is not implemented |
| B10 | **`virusType` has only 0/1**; 12 `threat_type_*` zero references; `adLevel/adName/adDescription` whole tree 0 assignment | Advertisement detection is **and the protocol bit is empty**; the threat classification is not in this layer |
| B11 | **URL feature library `20180917.zz` is about 5 years behind** (AV library is `20220928.ah`) | There will be almost no coverage of new phishing domain names after 2019 |
| B12 | **`libavlurl.so` is SONAME is `liburl.so`**, and `liburldetectorsys` is `liburl.so` dlopen | If `setLibPath()` is not adjusted, Android **will not** press SONAME to match the file name ⇒ **URL The overall engine will fail and silently degrade** |
| B13 | `libTmsdk` file name 2.0.13 / SONAME 2.0.12 | Version mismatch |
| B14 | **built-in RSA public key certificate `NotAfter 2012-02-24` has expired**, and does not match the license included in the package | TMSDK The authorization verification chain cannot pass in this package ⇒ The authorization function is downgraded |
| B15 | `mdetector` is ARM32, while APK only comes with arm64-v8a | ARP detection silently fails on pure 64-bit models |
| B16 | Room column level AES uses random IV ⇒ UNIQUE index and equivalent query invalid | The database grows infinitely and the query degrades to a full table scan. |
| B17 | If the cloud scan response index alignment is written as `payload.packageName != apkInfo.packageName`, an error will be reported. | Suspected `!=`/`equals` negation error |
| B18 | `i0.h.h()` reads `miui.support_full_scan` (**full scan capability is**), and URI is named `support_incremental` | Naming and semantic mismatch |
| B19 | `Library` version `libtmg-gm.so` **does not exist in APK** (only `libtmf-gm.so`) | inconsistent naming |

---

## 13. Unresolved issues (summary, by priority)

| # | question | Current situation | Next step |
|---|---|---|---|
| U0 | **is not available on this machine ELF Decompilation path**: Ghidra (win32) Only PE/6502 and Hopper require macOS, IDA/Kuna is not configured | Used self-written ELF32/64 analysis + full library regular forensics + AArch64 instruction decoding replacement | `open_binary` ELF-arm64 on Linux/macOS, or configure IDA/Kuna |
| U1 | `libflamingo.so` internal algorithm (Aho-Corasick is known, but binary feature syntax/boundary conditions for `matches` are unknown) | Only 3 JNI symbols + crate information | arm64 disassembly `Java_…_matches` |
| U2 | **method signatures of 6 `RegisterNatives` tables and semantics** one by one (the class name is known, but the behavior of the 60 methods has not been settled one by one) | Class name + signature has been taken in plain text | Signature-by-signature mapping to Java-side call sites |
| U3 | What algorithm is used for the ciphertext segment of **`avlsdk:sdk_conf/sdk.conf` (starting at 9,999 B from `0x17C`)** (may contain AVL cloud domain name/switch) | Excluded AES/SM4 block mode (length not a multiple of 16) + Excluded known keys | Disassemble the config analysis of `libavlasys`/`libavlm`; or dynamic dump decrypt the plain text |
| U4 | **`assets/40706.dat`’s `TccCryptor` specific algorithm and key location** | Known 8-byte alignment, known path TccCryptor | Disassemble `libTmsdk` of `TccCryptor.decrypt` |
| U5 | ~~The release/exec code location of `assets/mdetector`~~ | **has closed**: `Runtime.exec` @ `tmsdkobf/t9.smali:563`, `com/avl/engine/j/b/e.smali:217,383` | — |
| U6 | Is `k` of **SM2 really taken from `rand()`** | Only import evidence | Disassemble SM2 encryption path of `libtmf-gm` |
| U7 | **TMSDK / AVL actually reports field** (whether it contains IMEI) | Java layer confirmation only transmits `android_id`+`MODEL` | Capture packet `tmfsdk.m.qq.com` / `virus-engine.sec.miui.com` |
| U8 | Is Room equivalent query dead code (due to random IV) | Code is almost impossible to hit | Runtime verification (requires real machine + trigger synchronization) |
| U9 | Whether the `!=` assertion responded by Cloud Scan is really a bug | static suspicious | Runtime verification |
| U10 | `tmsdkobf/nb.java` Cross-application DB read reachability | Code exists, no caller | `trace_android_references` chasing edge |
| U11 | Whether the `ping` injection of `tmsdkobf/t9.java` can be controlled by the IPC parameter | The link exists, `str2` is not filtered | Chase transaction 14 → `t9.a()` parameter flow |
| U12 | Level `4`/`5` semantics of `y.f` | Only suppress back push from merge priority and reporting | Security center side consumption code required |
| U13 | 18 obfuscated packages cannot be classified at all (another 6 have low confidence) | Listed | Need more cross-references or dynamic validation |
| U14 | Whether the ~400 domain names of `libavlurl.so` (determined by Public Suffix List) also have other uses | The upper limit of 400 has been reached, but it is actually more | Increase the upper limit and rescan |

---

## 14. Attachments and evidence index

### 14.1 Accessories

| File | scale | content |
|---|---|---|
| `10_core.md` | 905 lines / 96 KB | Main control layer: `n0/g` full method table, 26 AIDL transaction codes, `y.f` full field table, four-engine matrix, R1–R20 high-risk list, SharedPreferences full table |
| `15_obf.md` | 481 lines / 45 KB | 179 obfuscated package semantic maps (business ~25/rest of library), 6 reproducible links, path/Intent/SQL/full key table, 18 unclassified |
| `20_cloud.md` | 940 lines / 70 KB | Room 8 table complete DDL, column-level AES, JCE encoding rules, two sets of network stacks, 7 new stack endpoints, full message fields, TTL policy |
| `30_native.md` | 859 lines / 90 KB | 12 `.so` identity tables + 8 assets identification + JNI boundaries + suspicious capabilities 17 sets of regular evidence + constant table + R1–R12 |
| `40_vendor.md` | 716 lines / 72 KB | "Claim vs Enable" ledger, 28 endpoints, 8 privacy links for AVL/TMSDK/OneTrack/xCrash/State Secrets/zipfs/PackageParser |
| `_obf_index.tsv` | 3,231 lines | Full class index (`package/class.java<TAB> class declaration`) |
| `dex_strings_all.txt` | 29,263 lines | `classes.dex` Original MUTF-8 full string (for cross-validation) |
| `src/sources/` | 3,231 .java | Full JADX decompiled source code |
| `apktool/` | — | smali + res (with 44 languages `strings.xml`) + as-is `lib/`/`assets/` |
| `avlsdk_extract/` | 26 items | `assets/avlsdk` unpacking (AVL feature library) |

### 14.2 REA evidence ID (12 key items)

| Operation | object | support |
|---|---|---|
| `inspect_android_package` `ev_5fdff3ac…` | APK | Package Identity, Permissions, Components, 3242 Classes |
| `inspect_analysis_view`(manifest) `ev_8389fb90…` | APK | Complete manifest text |
| `inspect_apk_signature` `ev_c65c7674…` | APK | v1/v2/v3 Three signatures and the same certificate, no rotation |
| `inspect_android_class` `ev_37fb1e7f…` | `GuardProvider` | 15 methods / 2 inner classes |
| `inspect_android_class` `ev_cdd2481e…` | `SecurityService` | 12 methods, two Binder fields |
| `inspect_android_class` `ev_f6df526e…` | `SecurityService$a` | **24 AIDL method** |
| `inspect_android_class` `ev_80d5f918…` | `f0.b` | **has only 3 enumeration constants** (no QAX) |
| `inspect_android_class` `ev_0c030a86…` | `RustSupport` | 3 native methods |
| `inspect_native_exports` `ev_3917ad9c…` / `ev_7a386e6f…` / `ev_07e78735…` | `libflamingo.so` / `libavlm.so` | 3 export / 98 `AVLM_*` full quantity |
| `inspect_android_class` `ev_3a4f3298…` `ev_52917bb2…` `ev_4c3c9e94…` | `TencentEngine.e` / `w0.d.a` / `z.b.<init>` | TMSDK Initialization / Offline APK Analysis / OneTrack Configuration |
| `inspect_android_class` / `inspect_android_method` `ev_3415eba8…` `ev_73f513b6…` `ev_b6d546cf…` | `EngineDatabase_Impl$a.a` / `v0.j` / Cloud POJO | DDL / Key derivation / Message structure |

### 14.3 Conclusions of independent review in §10 of this report

| # | Conclusion | Review method |
|---|---|---|
| 1 | AES key = `wcw1xbplx7aijwqr` | Directly read 16 bytes of `flamingo.dat` offset 144 in APK, LE int64 split XOR `0x430BF1FFFFF10B43` |
| 2 | Three salt values | Source code precise positioning `i0/d:55,203`, `j0/c:75`, `q0/q:22` |
| 3 | `scan_params` bit field + `CloudScanApkInfo` five fields | Read `p0/f:348-379` line by line |
| 4 | Cloud base URL domestic and foreign switching | `q0/m:12` |
| 5 | `n0/g.x()` Return `String[4]` Fill in only 3 | Read `n0/g` line by line |
| 6 | Among `M()`, `scanType==1` is the increment | Read `n0/g.M()` line by line |
| 7 | **Rule Engine = Apache Commons JEXL3** | `z5/b`=JexlBuilder, `v5/e` determines `org.apache.commons.jexl3.*`, `a6/c3`=JexlParser syntax constant table |
| 8 | `AVLA` is the JNI facade (method name confusion `pcit/icf/gcv/fcf`) | Source code + `libavlasys` has `JNI_OnLoad` |
| 9 | `libflamingo` only exports 3, and corresponds to the three methods of `RustSupport` one-to-one | REA `inspect_native_exports` + plain text symbol scan |
| 10 | `assets/avlsdk` is 26 items ZIP; `version.conf` plain text | Directly unpack and read |
| 11 | `mdetector` = ELF32 ET_DYN ARM + libpcap + ARP hijack string; loading method = `Runtime.exec` (`t9.smali:563`, `com/avl/engine/j/b/e.smali:217,383`) | ELF header field + string + smali positioning |
| 12 | `70003.amf` = `"amf\0"`+v4 + dual timestamp + **42,556×16 B ciphertext record** | Header hex + length divisible |
| 13 | `licence.conf`/`licence1.conf` = `[128 B RSA/ECB/PKCS1][DES/ECB/NoPadding]` → `Properties`; built-in certificate 2012 expired | Length divisible + RSA depadding failed |
| 14 | `sdk.conf` The ciphertext segment is not a multiple of 16 ⇒ **AES/SM4 The whole block mode structurally excludes** (revised Lead initial judgment) | 9,999 B starting at offset 0x17C |
| 15 | Qi'anxin QAX not implemented | Resource TODO + enumeration 3 items + no class |
| 16 | Firebase build remnants | Configuration string exists but 0 `com/google/**` classes |
| 17 | QR code is not implemented | Sources + smali double search only 1 string hit |
| 18 | **None of the 12 libraries Frida/Xposed detected** (three false positives: `Frida`←`Friday`, `frida`←`blackfriday`, `xposed`←`exposed`) | 17 sets of regular rules × 12 libraries + context verification |

