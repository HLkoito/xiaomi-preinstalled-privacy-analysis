> **Historical analysis with subsequent corrections.** Use the [final review](../REPORT.md) for the reviewed findings. It supersedes earlier claims about plaintext IMEI lists, actual uploads under default settings, absolute coverage, and unverified native behavior. The DAU IMEI and IMSI lists use MD5 and remain subject to region, permission, and network checks. Certificate expiry alone does not establish that ciphertext cannot be decrypted. Local identity paths were removed. Historical source line references may point to files outside the published evidence set.

# R8 obfuscates business layer semantic mapping and feature link restoration

> Scope: All single/double letter obfuscation packages (about 200) under `_analysis/src/sources/`, excluding
> `com/ androidx/ tmsdk/ tmsdkobf/ xcrash/ android/ dagger/ j$/ kotlin/ okhttp3/ org/ miuix/`；
> `n0 c0 i0 d0 w0 x f0 y` belongs to t-core. This report only quotes without expansion (all references are marked as `[core]`).
> Annotation convention: **observed** = source code is directly visible (file: line) | **inferred** = based on summary | **unknown** = not covered.

## 0. Methods, boundaries and a key premise

**key premise (observation)**: R8's repackage is "assigning short package names by class", **is not semantically divided into directories**. May be mixed in the same package
Classes from different libraries (for example: `l0` contains both the business pipeline interface `l0.c/d/e` and the `newInputStream` literal of `kotlin.io`).
Therefore, the "Package → Functional Domain" in Section 2 of this report will be judged according to the class **that accounts for the majority of the package**, and the mixed package will be noted separately.

Classification means (all reproducible):
1. Full tree class declaration index `_analysis/report/_obf_index.tsv` (line 3231, `path <TAB> declaration`).
2. Packet-by-package `import` statistics (import will not be polluted by the constant beautification of JADX, which is the most reliable first-hand evidence for this report).
3. Packet-by-packet "highest frequency long string literal" fingerprint (used to identify the library identity, for example: `Operation is not supported for read-only collection`).
4. By whom `import` (in-degree) per package - used to distinguish "support libraries referenced by the business" and "dead code/library internals that are not referenced".
5. Chase down from the business anchor point: `GuardProvider` → `n0.g` → `f0.a` → Engine implementation; `p0` scanning pipeline; `r0` Room; `s0/q0` network.
6. Full-text keyword scanning (exec / DexClassLoader / reflection / root / emulator / IMEI / clipboard...), aggregated by package.

> **JADX Trap (must know)**: `case 13:` of `switch` will be marked as `QScanConfig.EADT_BANNER /* 13 */` by JADX
> This type of **is any equivalent constant name** (see `o2/a.java:111-127`), which is just a decoration of the literal 13. **is not a reference to tmsdk by**.
> In the same way, `j0.a(...)` in `f2/*` refers to the class `j0` in the `f2` package, not the package `j0`. All "cited" conclusions in this report only use `import` and qualified names.

## 1. Overview of Responsibilities

`com.miui.guardprovider.*` has only 44 classes, which are **thin shell**: ContentProvider (`GuardProvider`), Service
(`SecurityService`), AIDL implementation, three engine adapters (Mi/MiEngineNG, AVL, Tencent), PackageParser packaging.
Real business implementation all falls in the confusing package of this report, which can be classified into 8 main lines:

1. **scan schedule**: `n0.g` (task table/engine fan-out/result merging) + new coroutine pipeline `l0`/`p0`/`t0`;
2. **rule engine**: `t0` (Trait/StringItem/conclusion factory) + `p4` (kotlin.text regular) + `v5/a6/x5/y5/z5` (Apache JEXL3 expression) + native `libflamingo`;
3. **cloud**: `q0` (Retrofit interface/signature/encryption interceptor) + `s0` (MiEngineNG client + Room DAO) + `p0.f` (lightning scan) + `p0.b0` (result reporting);
4. **list/library synchronization**: `i0.d` (api.sec.miui.com incremental/full amount) + `c0` (sqlite result cache) + `r0` (Room 8 table);
5. **URL/fishing**: `n0.j` → `f0.a.m()` → `AVLEngine.scanURL` → `libavlurl.so`/`liburldetectorsys.so`;
6. **WiFi**：`n0.k` → `f0.a.n()` → `TencentEngine` → tmsdk `WifiDetectManager`；
7. **anti-fraud/gray production**: `g0` (MiEngineUtils) + `j0` (`mi_detect_app_db`) + `c0.b` (`risk_gray_result_db`);
8. **infrastructure**: `v` (OAID reflection), `z`/`u` (OneTrack/analytics buried point), `h0` (NetworkApiHelper), `v0` (AES/RSA + verification), `w0` (SharedPreferences/DB migration) [core].

Third-party library residues account for most of the confusion tree: kotlin stdlib/collections/text/sequences, kotlinx-coroutines,
**kotlin-reflect + embedded Kotlin compiler frontend (~40 packages, 700+ classes)**, Gson, OkHttp/Okio, Retrofit,
Room, protobuf, Apache Commons JEXL3, commons-logging. If **does not do this layer of stripping, the compiler front end will be regarded as business logic.**

## 2. Package-level semantic map

### 2.1 General list of business domains (look at this first)

| # | functional domain | package | Judgment basis (verifiable) | Representative class |
|---|---|---|---|---|
| B1 | Scan Scheduling/Task Queue/Concurrency | `n0`[core] `l0` `m0` `p0` | `n0/g.java:1260` `L()` task creation, `:1272` task upper limit 10, `:1132` `K()` fan-out engine, `:1296` `M()` type==1 incremental; `l0/e.java` is the Scanner interface, `p0/b0.java:202` assembles three scanners | `n0.g` `l0.e` `p0.b0` |
| B2 | Signature/rule engine | `t0` `p4` `v5` `a6` `x5` `y5` `z5` | `t0` has been imported 133 times by `p0` (`p0/*` is the highest in the whole tree); `p0/f.java:73` `t0.d.c(...)` is the conclusion factory; `v5/a.java` is the JEXL interpreter, and `a6` is JEXL Syntax/parsing (`"<DECIMAL_LITERAL>"`, `"variable is already declared"`), `z5` including `"org.apache.commons.jexl3 { JexlBuilder {} }"` | `t0.s` `t0.j` `v5.b` `z5.a` |
| B3 | Cloud scanning client (flash scan) | `q0` `s0` `p0` `u0` | `q0/d.java` `@f6.f("/virus/scan")`; `p0/f.java:264-295` group package (androidVersion/miuiVersion/timestamp/uuid/versionType/scanType); `s0/s.java:124` base=`q0.m.f7703a`; `u0/g.java` status enumeration deleted/online | `q0.d` `s0.s` `p0.f` `u0.e` |
| B4 | Cloud reporting | `q0` `p0.b0` `s0` | `q0/n.java` `@f6.f("/virus/localResult/report")`; `p0/b0.java:235-277` group `ResultReportData` (including `Build.VERSION.INCREMENTAL/SDK_INT/DEVICE`) | `q0.n` `p0.b0` |
| B5 | URL detection/fishing/QR code | `n0.j`[core] `p5`? `okhttp3` `com.avl.engine.urldetector` | `n0/j.java:39-72` URL scanning singleton; `f0/a.java:95` `m(int,String)`; `AvlEngine.java:195-205` `m(1)→AVLEngine.scanURL`, `m(2)→updateURLRule`; `UrlDetector.java:80-87` `loadLibrary("urldetectorsys")`+`libavlurl.so` | `n0.j` `AvlEngine.m` `UrlDetector` |
| B6 | WiFi security check | `n0.k`[core] `f5` `t` | `n0/k.java:99-115` four method mappings `f0.a.d.ARP/DNSANDPHISHING/NETWORKSTATE/SECURITY`; `SecurityService.java:48-143` four binders; `TencentEngine.java:497` `n()→i(...)`; `f5/a.java` import `tmsdk.bg.module.wifidetect.WifiDetectManager` | `n0.k` `TencentEngine` |
| B7 | Virus database/engine updates | `q0` `s0` `i0`[core] `r0` `w0`[core] | `q0/m.java:12` double domain name; `s0/s.java:124` points to this domain name; `i0/d.java:203` `antivirus/viruses`, `:55` `whitelist`; `AvlEngine.java:176-193` `checkUpdate()/update()`; `TencentEngine` `UpdateManager.check(UPDATE_FLAG_VIRUS_BASE,...)` | `q0.m` `s0.s` `i0.d` `AvlEngine.l` |
| B8 | Installation interception/silent installation | `com.avl` (cross-domain) `p0` `n0.g` | `guard/install_scan`→`GuardProvider.java:415` `f(path,bool,engineName)`; `AVLA.java:83` `native installPackage(...)`; `com/avl/engine/l/b/c.java:27,80` Adjust it | `AVLA.installPackage` |
| B9 | Result aggregation/deduplication/caching/whitelisting | `c0` `r0` `g0` `i0`[core] | `c0/c.java:8` `scan_result.db`, `c0/a.java:12` `risk_gray_result.db`, `w0/h.java:19` `miui_virus.db`; `n0/g.java:1014-1087` `D()` merge and deduplicate (partial order of level 1<2<3<4<5<6) | `c0.d` `c0.b` `r0.*` |
| B10 | Anti-fraud/gray product determination | `g0` `j0` `c0` `r0` | `g0/e.java:20-34` makes `virusLevel=6`, `extras="AntiDefraud:danger;"`, `engine="Mi"`; `j0/a.java:37-58` parses the cloud `basisOf==2` writes `mi_detect_app_db`; `GuardProvider.java:103-127` `getRiskAppData` | `g0.e` `j0.a` `c0.b` |
| B11 | Buried points/statistics | `z` `u` `v` | `z` contains OneTrack appid `2882303761517522561` + `com.miui.guardprovider_` prefix; `u/a.java` import `com.miui.analytics.ITrack`; `v/a.java:30-34` OAID/UDID reflection | `z.a` `u.a` `v.a` |
| B12 | Encryption/Encoding | `v0` `e5` `y0` | `v0` 11 files, `javax.crypto` 9 places, literal `AES/CBC/PKCS5Padding`/`AES/ECB/PKCS5Padding`/`manifestMFSha256`; `e5` `NoSuchAlgorithmException`; `y0` disk error branch | `v0.j` `v0.k` `v0.l` |
| B13 | Configuration/Cloud Control/System Properties | `c5` `d5` `t` `p1` | `c5/a.java` `isMiuiLiteRom failed` + `).getDeclaredField(`; `d5/a.java` import `miuix.core.util.SystemProperties`, `ro.product.mod_device`; `t/a.java` Same as above + `QScanConfig` | `c5.a` `d5.a` |
| B14 | DB/DAO（Room） | `r0` `s0.d` `h i j e g` | `r0` 44 files, import `androidx.room` class; `s0/d.java:140-199` takes DAO directly from `EngineDatabase`; `h/i/j/e/g` = Room runtime (`SupportSQLiteLock`, `cancellationSignal`, `room_fts_content_sync_`) | `r0.d` `r0.a` `s0.d` |

> Note: For the determination of `n0 c0 i0 w0 x f0 y`, see the t-core report; this table only lists the coupling points with this layer.

### 2.2 Third-party library residue identification table (**should not be regarded as business**)

| Library | package | Evidence |
|---|---|---|
| Kotlin stdlib `kotlin.jvm.internal` | `w1` | `w1/l.java` provides `f(obj,"name")` null value check; literal `"(Kotlin reflection is not available)"` |
| `kotlin.jvm.functions` | `v1` | `v1/a.java` `interface a extends g1.c { Object f(); }` = `Function0.invoke`；`v1/l`=Function1、`v1/p`=Function2 |
| `kotlin.collections` | `h1` `i1` `o4` | `h1/a.java:11-15` read-only collection throw `"Operation is not supported for read-only collection"`; `o4` `"Sequence is empty."`; `i1` `"copyOf(this, newSize)"` |
| `kotlin.text` / regex | `p4` `q4` `k4` `h5` `s1` | `p4` literal `missingDelimiterValue`, `substring(startIndex, endIndex)`; `q4` `"Retrieving groups by name is not supported"` |
| `kotlin.ranges` / `kotlin.time` / `kotlin.io` | `b2` `r4` `l0` (part) `r5` | `b2` `"Step must be non-zero."`；`r4` `"TimeSource(System.nanoTime())"`、`"ms is denormalized"` |
| `kotlin.coroutines` Basic | `k1` `l1` `m1` `g1` `t1` `a1` `a2` `r1` `r3` `f4` `n4` | `k1` `"EmptyCoroutineContext"`; `l1/d` is the `COROUTINE_SUSPENDED` singleton (`s0/d.java:52,98` uses `l1.d.c()`); `m1/a,d,k` is `BaseContinuationImpl/ContinuationImpl`; `g1` `"Lazy value not initialized yet."` |
| **kotlinx-coroutines** | `s4` `x4` `u4` `z4` `w4` `v4` `t4` `y4` | `s4/a.java:~20` `"... was cancelled"` + `g0(Throwable)`；`x4` `"kotlinx.coroutines.internal.StackTraceRecoveryKt"`、`"Inconsistent state"`；`u4` `"Unexpected waiter: "`、`"Channel was closed"`；`w4` `"Flow was aborted, no more elements needed"`；`z4` `"[Pool Size {core = "`；`t4` `"The main looper is not available"` |
| **kotlin-reflect + Kotlin compiler front-end** (maximum residual) | `c4` `l2` `o2` `o3` `u2` `g4` `b3` `b4` `m4` `m2` `w2` `w3` `v2` `v3` `y2` `i2` `k2` `d3` `h3` `j3` `t3` `z2` `s3` `n3` `c3` `i4` `q3` `r2` `f2` `c2` `e2` `u1` `y1` `h4` `p2` `p3` `d2` `g2` `j4` `a4` `e4` `u3` `h2` `l4` | `c4/b.java:22` abnormal string `"kotlin/reflect/jvm/internal/impl/types/AbstractClassTypeConstructor"`; `f2/i.java` aggregation `v1.a..w`+`c2.b` (= `KFunction` whole family); `r2/a.java:40-44` reflection `Method.getType/getAccessor`; `m2` literal `"kotlin/reflect/jvm/internal/impl/descriptors/annotations/AnnotatedImpl"`; `m4` `".../utils/WrappedValues"`; `u2/l.java` `implements o3.g` (SPI, `META-INF/services` has been disproven by lead); `g4/b.java` enumeration `FOR_SUBTYPING/FOR_INCORPORATION/FROM_EXPRESSION`; `b3` are all interfaces and are only `y2/v2/z2/x2` Quote |
| Gson | `l` `o` `n` `p` `q` `r` `t` `m` `c` `d` `e6` | `l` `"Not a JSON Object:"`/`"GSON cannot serialize"`；`o` `"ReflectionAccessFilter does not permit using reflection"`；`t` `"Expected a name but was"`；`c/d` `"This container does not support retaining Map.Entry objects"`；`q` `"getRecordComponents"` |
| OkHttp | `g5` `l5` `m5` `p5` `q5` `i5` `n5` `o5` `j5` `s5` `okhttp3` | `g5/e.java` `interface e extends Cloneable` (`Call`), `g5/c.java` `abstract Closeable,Flushable` (`RequestBody`); `m5` `"Unexpected status line:"`/`"network interceptor"`; `p5` `javax.net` 29 places + `org.conscrypt`; `q5` `p5` depends on (TLS provider) |
| Okio | `t5` `u5` `o5` (part) | `t5/b.java` `implements d, t5.c, Cloneable, ByteChannel`（`Buffer`）；`t5/e.java` `Serializable,Comparable`（`ByteString`）；`u5` `"Unexpected hex digit"` |
| Retrofit | `f6` | `q0.a:9` `@f6.f("/blackGrays/feature/codeList")`、`@f6.u`、`@f6.t`；`d6/u` = `Retrofit.Builder` |
| Room | `h` `i` `j` `e` `g` | `j` `"SupportSQLiteLock"`/`"Unable to grab file lock."`；`h` `"cancellationSignal"`/`"cursor.notificationUri"`；`g` `room_fts_content_sync_`、`PRAGMA foreign_key_list(` |
| Protocol Buffers | `m3` `f3` `h3` `g3` `i3` `x3` `z3` `j3` | `m3` `"Wrong object type used with protocol message reflection."`; `f3/*` 24 categories `extends m3.i implements m3.q` (`GeneratedMessageV3`) |
| Apache Commons JEXL3 | `v5` `a6` `x5` `y5` `z5` | `z5` `"org.apache.commons.jexl3 { JexlBuilder {} }"`; `v5` `"error calling operator '"`/`"unsolvable function/method '"`; `y5` `"unable to find class:"`; `a6` `"<DECIMAL_LITERAL>"`/`"variable is already declared"` (JavaCC syntax) |
| commons-logging | `c6` `org` | `c6/a.java` import `org.apache.commons.logging.Log`；`org/*` `"[CUSTOM LOG FACTORY]"` |
| Dagger | `e1` `e0` `z0` `z1` | `e1` `"Cannot return null from a non-@Nullable @Provides method"`；`z0` `"Given component holder %s does not implement %s or %s"` |
| androidx.room / core | `androidx` `e` | `e` `"androidx.room.IMultiInstanceInvalidationService"` |

### 2.3 Package-by-package index (all packages in this layer)

| package | Belong | Basis/Remarks |
|---|---|---|
| `p0` | B1+B3+B4 scanner | See 2.1; Internal `b0`=RealScanner, `g`=CodeScanner, `f`=CloudScanner, `z`=MatcherScanner, `y`=Rule Compilation |
| `t0` | B2 Domain Model | `p0` imported it 133 times; `t0/s`=Trait, `t0/j`=StringItem, `t0/d`=Conclusion Factory, `t0/m`=Threat name |
| `r0` | B14 Room DAO | 44 File; `r0/d`=black_gray_app, `r0/f`=black_gray_chs, `r0/l`=characteristics, `r0/a0`= guilty_package, `r0/g0`=innocent_package, `r0/l0`=trust_certs, `r0/p0`=version, `r0/a`=aggregation |
| `s0` | B3+B7 cloud client | `s0/s.java` Retrofit/OkHttp assembly + decryption interceptor + Gson adapter; `s0/d` EngineDatabase DAO packaging |
| `q0` | B3+B7 API definition | 7 Retrofit interfaces + 3 OkHttp interceptors (signing/encryption/logging) + `q0/m` domain name |
| `u0` | B3 Cloud Response | `u0/g` `enum {deleted, online}`; `u0/i` is its Gson adapter; `u0/e` response shell (code/desc/data) |
| `v0` | B12 encryption service | `v0/j` CryptoService, `v0/k` Gzip, `v0/l` verification; literal `AES/CBC/PKCS5Padding`, `manifestMFSha256` |
| `g0` | B10 Anti-fraud | `g0/e.java` `MiEngineUtils`：level=6、`AntiDefraud:danger;`、engine=`Mi` |
| `j0` | B10 anti-fraud library | `j0/a.java` `mi_detect_app_db` (package/signatures), source cloud `basisOf==2` |
| `c0` | B9 result library | `c0/a`=risk_gray_result.db, `c0/c`=scan_result.db, `c0/b/d`=two DAO |
| `h0` | B3 Network Assistant | `h0/a` NetworkApiHelper, `h0/b` with salt GET (`94e6cec2-…`) |
| `c5` `d5` `t` `p1` | B13 system properties | Reflection reading MIUI attribute / `ro.product.mod_device` |
| `v5` `a6` `x5` `y5` `z5` | B2 JEXL3 | See 2.2 |
| `f6` | Retrofit | `@f6.f` Note |
| `d6` | Mix: Gson `$Gson$Types` + OkHttp | Literal `"must include generic type (e.g., "`; import `g5` 17 times → low confidence |
| `f5` | WiFi adaptation (tmsdk) | `f5/a.java` import `WifiDetectManager`、`"Exception throw during when append"` |
| `w` | Empty shell/compatibility layer | `w/c.java` method body is empty; `w/e.java` only implements the interface, `guardprovider` is referenced 2 times |
| `z` `u` `v` | B11 buried point/OAID | See 2.1 |
| `x0` | provider client constants | `x0/a.java` `Uri.parse("content://guard/virus")` |
| `y0` | IO error handling | `"No space left on device"`、`OutOfMemoryError` |
| `e5` | Summary/Exception encapsulation | `"NoSuchAlgorithmException"`、`"IO exception happend in ByteArrayInputStream"` |
| `k5` | Task queue (tmsdk style) | `"task is in multiple queues"`、`"getLogger(TaskRunner::class.java.name)"` |
| `l0` `m0` | B1 pipeline abstraction | `l0/c,d,e` is implemented by `p0`; `m0` `"scanner"`/`"interceptor"` + import `l0.*` |
| `b5` | Singleton management | `"manager create instance cannot return null"`、`"Cannot acquire object after close()"` |
| `m3` `f3` `h3` `g3` `i3` `x3` `z3` `j3` | protobuf/TMF message | See 2.2 |
| `h` `i` `j` `e` `g` | Room runtime | See 2.2 |
| `l` `o` `n` `p` `q` `r` `t` `m` `c` `d` `e6` | Gson | See 2.2 |
| `g5` `l5` `m5` `p5` `q5` `i5` `n5` `o5` `j5` `s5` | OkHttp Family Bucket | See 2.2 |
| `t5` `u5` | Okio | See 2.2 |
| `w1` `v1` `h1` `i1` `o4` `p4` `q4` `k4` `h5` `s1` `b2` `r4` `r5` | Kotlin stdlib sub-packages | See 2.2 |
| `s4` `x4` `u4` `z4` `w4` `v4` `t4` `y4` | kotlinx-coroutines | See 2.2 |
| `k1` `l1` `m1` `g1` `t1` `a1` `a2` `r1` `r3` `f4` `n4` | kotlin.coroutines Basic | See 2.2 |
| `c4` `l2` `o2` `o3` `u2` `g4` `b3` `b4` `m4` `m2` `w2` `w3` `v2` `v3` `y2` `i2` `k2` `d3` `h3` `j3` `t3` `z2` `s3` `n3` `c3` `i4` `q3` `r2` `f2` `c2` `e2` `u1` `y1` `h4` `p2` `p3` `d2` `g2` `j4` `a4` `e4` `u3` `h2` `l4` | kotlin-reflect/compiler front-end | See 2.2; where `u2`=parse/overwrite check (`u2/l` implements `o3.g`), `g4`=type check context (`FOR_SUBTYPING`…), `b3`=pure interface markup layer (low confidence) |
| `e1` `e0` `z0` `z1` | Dagger | See 2.2 |
| `c6` `org` | commons-logging | See 2.2 |

### 2.4 Uncategorized list (honest labeling)

> Statistics: There are a total of **179** obfuscated packets in this layer; **155** have been clearly determined, **are undetermined, 18**, and 6 are low-confidence (the last row of the table below).

| package | tried means | Conclusion |
|---|---|---|
| `a` `b` `g4` `n1` `s1` `t2` `u3` `w5` `x1` `j1` `l3` `b0` `b6` `a5` `c1` `d1` `f1` `s` `p` `k` `i` `f` `q` `x3` | Declaration index, import statistics, literal fingerprint, in-degree | **Unknown**. Mostly fragments of 1–3 classes: no import / only `w1.l` / empty package-level tools, in-degree 0–1, cannot be characterized without looking at smali. `n1` is only used by `$values`, the enumeration of `u0.g` (`n1.b.a`), which is suspected to be auxiliary to kotlin enumeration; `a5/c1/d1/f1/b6/w5/x1/t2/j1/l3/b0` has zero penetration in the entire tree. |
| `s` `p` `k` `i` `f` `q` | Same as above | Corner of Gson/OkHttp (`p` contains `"No time zone indicator"` → Gson date parsing; `q` contains `getRecordComponents` → Gson reflective accessor), confidence |

## 3. Critical link timing

### 3.1 `guard/sync_scan` / `install_scan` synchronous scanning of the entire link

```
[Caller App] ContentResolver.query(content://guard/sync_scan, selectionArgs=[path, bool])
  └─ GuardProvider.query  (com/miui/guardprovider/GuardProvider.java:395-399 → :200 f(path,bool,engineName=null))
       ├─ path starts with content:// → first copy to cacheDir/copy.apk (:213-216), scan and then delete (:262-266)
       ├─ path starts with file:// → takes Uri.getPath() (:270)
       ├─ CountDownLatch(1) (:247)
       ├─ n0.g.z().L(new String[]{path}, observer, bool, 2, engineName)   (:249, :271)
       │    ├─ Filter null/non-existent path (:1261-1271); the number of tasks >=10 will directly return -1 (:1272)
       │    ├─ Create 6 ConcurrentHashMap (observer→result/progress/engine/serial number/…) (:1285-1289)
       │    ├─ new n0.g.f(...).executeOnExecutor(THREAD_POOL_EXECUTOR) (:1290-1292) ← Whitelist pre-check
       │    │ └─ f.doInBackground: For each apk parsePackageLite → hitting i0.f whitelist will directly produce y.f(level=1),
       │    │ Otherwise, leave the path in the to-be-scanned list (:820-860)
       │    └─ onPostExecute → n0.g.K(list, observer, bool, 2, engineName, whitelisted number) (:872)
       │         ├─ t(x.a.c()) Get the "Enabled Engine" list → new i().a(engine) (:1134, :1172-1188)
       │         ├─ One thread per engine: engine.j(paths, callback, allowCloud, engineName) (:1143, :539)
       │         │ └─ MiEngine/AvlEngine/TencentEngine implemented separately (com.miui.guardprovider.engine.*)
       │         └─ Progress message what=3 → Press D() to merge multiple engine results of the same path (:217-241, :1014)
       ├─ countDownLatch.await(2, MINUTES) (:250, :272) ← 2 minute hard timeout
       └─ Return MatrixCursor, columns = Type,VirusName,Description,EngineName,AdType,AdName,AdDescription (:257-260)
```

- Semantics (observation + inference) of the **`type` parameter (parameter 4 i6)**: `L()` Store it in `f6420f` and use it only for progress offsets
  (`n0/g.java:1288, :199`); **`i6==1` in `M()` specifically uses the incremental cache scan `C()`** (`:1296-1302`), and the rest uses `L()`.
  `SecurityService.w()` passes 0 (normal scan, `:259`), `GuardProvider`’s `sync_scan`/`install_scan` passes **2**.
  ⇒ **0=normal application scan, 1=incremental (installed application cache), 2=synchronous/external request scan (with engineName)**. Inference, based on the above 3 call points.
- **engineName Select**: `K()` Give `engineName` to `engine.j(..., str)` as it is; the engine list comes from `x.a.c()` (SharedPreferences
  engine enabled in ). `x.a.j()/k()` will randomly select a **engine from the remote list** to be the only one enabled (`x/a.java:110-130`).
  The `cloud_engine` attribute determines the cloud engine (`x/a.java:14-27`).
- The **result is cropped to**: `L()` only returns **and a single** `y.f` (`fVarArr[0]`, `GuardProvider.java:71-73`), which is the most dangerous conclusion after the multi-engine merger;
  No result returns null (`:251-255`).
- **failed/cancelled**: `binderDied` of the observer will interrupt the thread and clear the task (`n0/g.java:553-576`); `cancelScan(id)` will be canceled by looking up the table by id (`:1372-1405`).

### 3.2 Cloud lightning scan (flash scan)

```
p0.b0.c()  [RealScanner]  (p0/b0.java:308-374)
  ├─ f (CloudScanner).a(ctx) :351 ← Cloud Scanner
  │    ├─ a(ctx): Read pipeline's "scan_params" Int (:372)
  │    │ ├─ f7380c = scan_params (reported as scanType; construction default 3, :353)
  │    │ └─ f7381d = (scan_params & 8)==0 ← bit3 = Disable cloud scan; when false, throw IOException("cloud scan is denied!!!") (:376, :260)
  │    ├─ Get the list of t0.a to be scanned → each group ≤N → k(a) group CloudScanApkInfo(pkg, versionCode, versionName, sigMd5, path) (:277-287, :358-368)
  │    ├─ POST /virus/scan  (q0/d.java:8)
  │    │    query: androidVersion=SDK_INT, miuiVersion=Build.VERSION.INCREMENTAL,
  │    │           timestamp=now, uuid=w.f7555a.a(), versionType=stable|development|alpha, scanType (:280-286)
  │    ├─ Response u0.e: code==0 → throw IOException("Response code:..."); otherwise, take data:List<CloudScanPayload> (:300-310)
  │    └─ Item by item a(ctx, payload) → t0.d.c(...) Merged conclusion (hit Threats → create t0.k threat; BlackGrays → gray product) (p0/f.java:44-95)
  ├─ g (CodeScanner).a(ctx) :366 ← Native code features
  ├─ z (MatcherScanner).a(ctx) :371 ← Rule matching (t0)
  └─ b0.h(result)                  :373 → :215-297
       ├─ Group ResultReportData: rule name/description/package name/application name/version/signature/… + Build.VERSION.INCREMENTAL + SDK_INT + Build.DEVICE
       └─ q0.n.a(now, list)  → POST /virus/localResult/report  (q0/n.java:11-13)
```
- Only `Log.e("RealScanner","Result upload failed")` (`p0/b0.java:291-295`) fails to be reported and will not be retried.
- Cloud scan result type: `CloudScanPayload.a = {Threats, BlackGrays, Unknown, Clean}` (`p0/f.java:54-95`).
- **threshold**: There is no **file size/time-consuming threshold for** in the source code (Yunscan clicks the t0.a list to submit the entire batch); the only two switches are `scan_params` bit3 and
  `f7380c` scanType. `Unknown`: The server-side threshold is not visible.

### 3.3 Virus database/signature database update link (three independent channels)

**Channel A: Cloud list (JSON, incremental/full)**
```
i0.d.j(incremental)  (i0/d.java:202-210)
  ├─ GET https://api.sec.miui.com/antivirus/viruses
  │     params: version=<antivirus_db_version.version>, updateType=incremental|global, <miuiVer key> (:36-45)
  │     Signature salt "94e6cec2-19dc-4bb6-823c-613f72d3ea16" is given to h0.b.c(...) (:203)
  ├─ By the way g(ctx): GET /antivirus/whitelist (same salt) → f.a(ctx).c(json) (:54-60, :204)
  ├─ i(json) transaction (:102-185):
  │    ├─ There is error_code → directly false (**does not roll back**, but the transaction has not started)
  │    ├─ delete antivirus_db_version → insert(version, release_time)
  │    ├─ updateType=="global" → **delete the entire table antivirus_db**, then insert one by one
  │    └─ Item by item status: new/updated → upsert; deleted → delete; key=(package, signatures, md5)
  └─ When false is returned, the upper layer will not be dropped into the library (no rollback file, naturally idempotent) (:206-209)
```
**Channel B: Rules/Gray Production Library (Retrofit + Room, new stack)**
```
s0.s.j(client, gson) → Retrofit.Builder().baseUrl(q0.m.f7703a)  (s0/s.java:123-127)
  q0.m: https://virus-engine.sec.miui.com (international build: .intl) (q0/m.java:12)
  Interceptor chain (s0/s.java:108-113 order): network(k) → sign(q) → i4Req(g) → i4Resp(i) → log(a)
   ├─ q0/q.java:20-22 sign = MD5(TreeMap parameter spelling "k=v&…" + "62b36629-ecb4-4725-9f33-ccb0fd158191")
   ├─ q0/q.java:40 If the caller comes with sign → IllegalArgumentException (anti-overwrite)
   └─ q0/g.java:29 request header EncryptVersion=<v0.j.d()>; request body AES encryption; response body decryption (v0.j.c)
  Endpoint: /virus/blocklist /virus/allowlist /virus/feature/codeList /blackGrays/blocklist /blackGrays/feature/codeList
  Dropped into the library: s0.d → DAO (r0.c/e/k/z/f0/h0/j0/k0/o0) (s0/d.java:140-199) of EngineDatabase
```
**Channel C: Engine binary/feature library (internal to SDK)**
```
AvlEngine.l(bVar)  (AvlEngine.java:176-193)
  ├─ s(b())=AVLEngine.setNetworkEnabled(network switch)
  ├─ AVLEngine.checkUpdate() → callback a(3,0) when ==0 (no update)
  └─ AVLEngine.update() → >0 callback a(0,rc), ==0 callback a(3), <0 callback a(2)
     Corresponding to f0.a.b callback → n0.g$c$C0153c.a(updateType, errCode) → observer.x(y.e) (:407-447)
TencentEngine.l(...) → UpdateManager.check(UpdateConfig.UPDATE_FLAG_VIRUS_BASE, cb, -1L)  (TencentEngine.java:490-496)
```
- **drop path**: JSON list → `/data/data/com.miui.guardprovider/databases/antivirus.db`, `miui_virus.db`;
  Rule base → Room `EngineDatabase` (`.../databases/`); the engine binary is automatically downloaded from `com.avl`/tmsdk (`Unknown`).
- **verifies**: channel A uses URL parameter `sign` (salt MD5); channel B uses `sign` parameter + `EncryptVersion` + AES; channel C is calibrated internally by SDK.
- **rolls back**: **does not explicitly roll back**. A relies on "deleting version rows first + clearing the table in full + single delete/insert" to achieve idempotence; the transaction is not committed when it fails (`setTransactionSuccessful` not reached).

### 3.4 URL scan link (`SecurityService` urlscan binder → judgment)

```
bind action "com.miui.guardprovider.action.urlscan" → SecurityService.b (IBinder)  (SecurityService.java:315-317)
  ├─ H(url, engineName)  y.b  → j.a().b(url, engineName)   (:280-283)
  ├─ t(url) y.b → j.a().c(url) (:286-293) ← Update URL rule base
  └─ n0.j (UrlScanManager)  (n0/j.java)
       ├─ d(url, type, engineName): `str.contains(bVar.f3547b)` Use **engine name substring** to select the engine (:44-79, select engine: 59-66)
       │     ← Note: If engineName is empty/does not match → return -2; if there are multiple matches, the first one in the enumeration order will be used
       ├─ The engine is not initialized → adjust after asynchronous e(cb); initialized → directly m(type, url) (:69-76)
       ├─ b(url, engine) → m(1, url)    (:87-88)
       └─ c(url)         → m(2, url)    (:91-92)
f0.a.m(int,String) default -1 (f0/a.java:95-97)
  └─ AvlEngine.m: 1 → AVLEngine.scanURL(url); 2 → AVLEngine.updateURLRule() + persistence avl_url_sdk_version (AvlEngine.java:195-211)
       └─ com.avl.engine.urldetector.UrlDetector: System.loadLibrary("urldetectorsys") + setLibPath("libavlurl.so") + loadURL(rulePath)  (UrlDetector.java:80-110)
```
- The **judgment result** is int (the native return value of `scanURL`). The Java side only transmits it transparently and does not interpret the semantics (`Unknown`: the specific code table is in native).
- `updateURLRule()` After success, write `w0.f` key `avl_url_sdk_version` (`AvlEngine.java:207`).

### 3.5 WiFi detection link (what each of the four methods does)

| binder（`y.a`） | Server | `n0.k` | `f0.a.d` | Engine implementation |
|---|---|---|---|---|
| `B(state, cb)` | `SecurityService.java:48-54` | `e()` (:102) | `NETWORKSTATE` | `TencentEngine.n()` → `i(...)` → tmsdk `detectNetworkState()`（`TencentEngine.java:60-130`） |
| `D(ssid, cb)` | `:67-73` | `d()` (:94) | `DNSANDPHISHING` | `detectDnsAndPhishing(listener, 0L)` |
| `O(ssid, cb, arg)` | `:122-128` | `c()` (:90) | `ARP` | `detectARPNoRoot(15000L)` → Failure and then `detectSecurity(scanResult)` |
| `Q(ssid, cb)` | `:137-143` | `f()` (:98) | `SECURITY` | `detectSecurity(...)` |
| （`A()`） | `:38-45` | — | — | `c.h(ctx)` triggers a full application detection |

- Unified entrance `n0.k.b()`: Select the engine by engine name → If not initialized, first `e(cb)`, asynchronously initialize and then `n(tag, cb, arg)`; otherwise, directly `n()`,
  **Both return -1** (`n0/k.java:75-88`). The callback returns int via `y.d` (AIDL).
- All WiFi methods directly return 0 when the user locks the screen (`SecurityService.c()` = `!androidx.core.os.a.a(this)`, `:297-299`).

### 3.6 Full/Application Scanning and "Gray Production/Risk Application" Determination (`getRiskAppData` Data Source)

```
ContentProvider.call("getRiskAppData") → GuardProvider.b()  (GuardProvider.java:103-127, :334)
  ├─ getInstalledPackages(0) full
  ├─ listE = c0.b.f(ctx).e() ← Read risk_gray_result.db all gray output cache (c0/b.java:103)
  ├─ Skip system apps ((flags & 1) != 0)
  ├─ c(pkg, listE): packageName + versionCode **double key** match → JSON{packageName,versionCode,type,signaturesMd5} (:129-157)
  ├─ First hit → d.g(this.f1748b) (i0.d whitelist refresh) + i0.f whitelist filtering (:113-119)
  └─ bundle.putString("riskData", JSONArray)
```
- **data source chain**: `risk_gray_result_db` ← `c0.d` write ← scan conclusion `y.f` (level∈{2,3,6} is considered a risk)
  ← `n0.g` merges the results of each engine (`n0/g.java:335-361` triggers whitelist review for 2/3/6).
- **cache validity period**: `w0.f` key `virus_cache_validty_perod` (default 3 days), expired rows are deleted (`c0/b.java:69`).
- **anti-fraud independent channel**: `g0.e` directly generates the result of `virusLevel=6` (`g0/e.java:20-34`) for the application that hits the `j0` library.
  The data source is JSON written from the cloud and written as `mi_detect_app_db` (`j0/a.java:37-58`, only `basisOf==2` is accepted).
- **Full**: `n0.g.v(ctx)` Use MediaStore to check `_data LIKE '%.apk'` (`n0/g.java:1195-1226`), which only covers the visible apk of external storage.

## 4. Data and constants

### 4.1 File path/database

| Path/library name | Purpose | location |
|---|---|---|
| `.../databases/risk_gray_result.db` (table `risk_gray_result_db`) | Gray application cache, TTL 3 days | `c0/a.java:9-12` |
| `.../databases/scan_result.db` (v2, table `scan_result_db`) | Single file scan result caching | `c0/c.java:8-13` |
| `.../databases/miui_virus.db`（v5） | `virus_white_db` + `t_virus_white_list` | `w0/h.java:9-19` |
| `.../databases/antivirus.db` | `antivirus_db` + `antivirus_db_version` (cloud list) | `i0/b.java`（t-core） |
| Room `EngineDatabase` (8 tables) | Rules/Gray Products/Certificate/Installer/Version | `r0/*`、`com/miui/guardprovider/sdk/android/db/EngineDatabase` |
| `mi_detect_app_db` | Anti-fraud application library | `j0/a.java:37-58` |
| `.../cache/copy.apk` | `content://` Temporary copy of input parameters | `GuardProvider.java:213` |
| `getCacheDir()` / `getFilesDir()` / `noBackupFilesDir` | Room and temporary file base address | `h`, `i` package (Room); `GuardProvider.java:213` |
| MediaStore `external` `_data LIKE '%.apk'` | Full disk apk enumeration | `n0/g.java:1199` |
| `/data/data/<pkg>/databases/…` | All of the above sqlite | Same as above |

### 4.2 Intent/action/permission/component

| Name | Purpose | location |
|---|---|---|
| `com.miui.guardprovider.action.antivirusservice` | Bind `y.a` (antivirus master Binder) | `SecurityService.java:312` |
| `com.miui.guardprovider.action.urlscan` | Bind `y.b` (URL scan Binder) | `SecurityService.java:315` |
| `com.miui.guardprovider.permission.Guard_Provider` | system\|signature protection provider+service | Manifest |
| `content://guard/virus` | Virus whitelist CRUD | `x0/a.java:8`、`GuardProvider.java:90` |
| `content://guard/{engine_info_list,sync_scan,install_scan,support_incremental}` | See briefing | `GuardProvider.java:88-92` |
| `android.permission.READ_EXTERNAL_STORAGE` | Dynamic application before `parseApk` (when there is no permission and the path is not /data) | `GuardProvider.java:163-165`, `w0` literal |
| `androidx.room.IMultiInstanceInvalidationService` | Room multi-process failure | `e` package literal |

### 4.3 SQL table and fields (visible part of source code)

| table | Field | location |
|---|---|---|
| `scan_result_db` | `_id, path, packageName, virusName, virusDescription, engineName, versionName, versionCode, virusLevel, extras, sign, time` | `c0/c.java:13` |
| `risk_gray_result_db` | `_id, packageName, versionCode, signatures, type, time` | `c0/a.java:12` |
| `virus_white_db` | `_id, package, path, version_code, size, engine, local_version, extra` | `w0/h.java:19` (including 4 ALTER migrations) |
| `t_virus_white_list` | `_id, scan_result_type, scan_item_type, app_label, desc, dir_path, pkg_name, virus_name, virus_md5` | `w0/h.java:15` |
| `antivirus_db` | `package, version, signatures, md5, type, virus_name, strategy, descType, description` | `i0/d.java:146-164` |
| `antivirus_db_version` | `version, release_time` | `i0/d.java:66,117-120` |
| `mi_detect_app_db` | `package, signatures` (+ cloud version discarded) | `j0/a.java:44-56` |
| Room 8 table | `trust_certs/trust_installers/innocent_package/characteristics/guilty_package/black_gray_chs/black_gray_app/version` | See `20_cloud.md`, `r0/*` |

### 4.4 Key/Salt/IV

| value | Purpose | location |
|---|---|---|
| `94e6cec2-19dc-4bb6-823c-613f72d3ea16` | Old stack list interface signature salt (A channel) | `i0/d.java:55,203` |
| `6988567a-4220-4b51-bc2d-ccdec27a74a1` | Old stack second salt | `j0` package literal (`20_cloud.md` is listed) |
| `62b36629-ecb4-4725-9f33-ccb0fd158191` | New stack `sign` parameter salt (B channel) | `q0/q.java:22` |
| `wcw1xbplx7aijwqr` | AES key (`20_cloud.md`) | `v0` (t-cloud has been verified) |
| `AES/CBC/PKCS5Padding`、`AES/ECB/PKCS5Padding` | Request/response body encryption and decryption | `v0` package literal |
| `manifestMFSha256` / `signaturesSha256` | Rule base field name (completeness) | `v0`, `r0`, `t0` literal |

> `Unknown`: The IV of AES does not appear in this layer literal (it may be spliced with constants in `v0.j`), and t-core/`v0` needs to be read carefully for confirmation.

### version 4.5/protocol/magic

| item | value | location |
|---|---|---|
| Virus database protocol version field | `version`、`releaseTime`、`updateType∈{incremental,global}`、`viruses[]` | `i0/d.java:115-127` |
| Cloud scan request fields | `androidVersion, miuiVersion, timestamp, uuid, versionType, scanType` | `p0/f.java:289-295` |
| `scan_params` bit field | bit0/1=Threat mode, bit2=Black and gray production, **bit3=Disable cloud scanning**, default 3 | `p0/f.java:370-379` (t-cloud has been verified) |
| `EncryptVersion` head | Encryption version negotiation | `q0/g.java:27` |
| Engine enumeration | `MI("Mi",100) / AVL("AVL",11) / TENCENT("TENCENT",10)` | `f0/b.java`（t-core） |
| Result level `virusLevel` | 1=safe/white, 2/3=threat, 6=anti-fraud; 4/5 exists but the semantics are undetermined | `GuardProvider.java:260`、`g0/e.java:29-34`、`n0/g.java:1015-1082` |
| Cloud status enumeration | `deleted`、`online` | `u0/g.java:5-6` |
| `extra` mark | `"AntiDefraud:danger;"` | `g0/e.java:29` |
| Engine name persistence key | `cloud_engine`、`load_cloud_engine_time`、`antivirus_is_init`、`avl_url_sdk_version`、`virus_cache_validty_perod` | `x/a.java`、`AvlEngine.java:207`、`c0/b.java:105` |

## 5. List of high-risk and suspicious behaviors

| Category | behavior | location | Trigger condition |
|---|---|---|---|
| Dynamic library loading | `libflamingo` (Rust rule engine), `libavlurl`+`liburldetectorsys` (URL), `libavlm/libavlasys`, `libTmsdk-2.0.13-mfr`, `libtmf-gm`, `libxy`, `libdce-1.1.21-mfr`, `libturingvd`, `libxcrash*` | `com/miui/guardprovider/natives/RustSupport.java:6`、`UrlDetector.java:80-87`、`com/avl/engine/a/c.java:223`、`com/tencent/tcuser/util/Encode.java:43` | First scan/URL detection/engine initialization |
| reflection | OAID/UDID/VAID/AAID：`Class.forName("com.android.id.impl.IdProviderImpl")` + `getMethod(...).invoke` | `v/a.java:30-34, 42-57` | Buried point/device identification initialization |
| reflection | MIUI attributes: `isMiuiLiteRom`, `ro.product.mod_device`, `getDeclaredField` | `c5/a.java`、`d5/a.java`、`t/a.java` | Determine when starting ROM/model |
| reflection | Gson/OkHttp/kotlin-reflect Large quantity `setAccessible`, `getDeclaredMethod` | `o`, `q`, `p5`, `q5`, `r2`, `f2` package | Library normal behavior, not business |
| command execution | `Runtime.exec`/`ProcessBuilder` only appears in `com`, `tmsdkobf`, `z5` (JEXL introspection) | See §0 Scanning | Non-local business paths |
| File traversal/deletion | MediaStore full apk enumeration; `Files.deleteIfExists(copy.apk)` after scanning; result library expired and deleted | `n0/g.java:1199`、`GuardProvider.java:221,239,253`、`c0/b.java:100` | Full scan/synchronized scan every time/scheduled cleanup |
| Silent installation | `AVLA.installPackage(path, 0|1, str2, str3)` native, `com/avl/engine/l/b/c.java:27,80` call | `com/avl/engine/security/AVLA.java:83` | Install interception/packet parsing (when `AVL` engine is enabled) |
| Read other people’s application data | `parseApk(path)` parses any apk (including `/data` path) and returns `pkgInfo`+icon+label; `content://` joins the conference and copies it to its own cache | `GuardProvider.java:159-190, 210-216` | The caller holds provider permission (system\|signature) |
| Bypass permissions | `parseApk` When there is no READ_EXTERNAL_STORAGE, `e.a(context)` triggers dynamic authorization before continuing. | `GuardProvider.java:163-165` | External incoming non-/data path |
| root/emulator detection | ** ** is not found in this layer (`root=`/`emu=` hit is 0; `tmsdkobf` is inside) | Full layer scan | — |
| Privacy:OAID | See above (`v`) | `v/a.java` | bury the point |
| Privacy: List of installed apps | `getInstalledPackages(0)` (`getRiskAppData`), `getInstalledPackages(64)` (incremental scan), `queryIntentActivities` | `GuardProvider.java:107`、`n0/g.java:689`、`j0/a.java` | Each `getRiskAppData`/incremental scan |
| Privacy: Device Fingerprinting | `Build.VERSION.INCREMENTAL`, `SDK_INT`, `Build.DEVICE` are reported with the scan results | `p0/b0.java:254-259, 272-277` | Report every time |
| Privacy: IMEI/MAC/Location/Contacts/SMS/Clipboard | ** ** is not found in this layer (the hits are concentrated in `com` and tmsdk, which do not belong to this layer) | Full layer scan | — |
| Anti-debugging | This layer is not found; `xcrash` has crash capture | — | — |

## 6. Unresolved issues

1. The JEXL3 ownership of `a6/x5/y5/z5/v5` has been confirmed by the literal, but the **who feeds the expression** at runtime does not close the loop: the whole tree only has the `com` package import `v5` 5 times,
   `p0`/`t0` are not quoted directly. A smali cross-reference is required to confirm if it is used by a Java rule path other than `libflamingo`.
2. The semantics of `virusLevel` 4/5, and the value set of `Type` listed in the return of `sync_scan` (`Unknown`, need to be read carefully by `t0.d`/`t0.m`).
3. The int code table of URL `scanURL` is in native (`libavlurl.so`), and there is no mapping on the Java side.
4. The download path of channel C (engine binary update) URL/disk is inside `com.avl`/tmsdk, and this layer is not visible.
5. `d6` package (Gson `$Gson$Types` mixed with OkHttp) and `l0` (business pipeline + `kotlin.io`) **class-by-class** belonging boundary.
6. About 20 zero-penetration packets (see 2.4) are uncharacterized; a smali level `invoke` plot is required to determine.

## 6.5 Addendum (4 spaces named by lead)

### 6.5.1 QR code: **does not exist** (negative conclusion, search boundary attached)

- Search `_analysis/src/sources/**/*.java` for the whole tree regularity `com\.miui\.qr|QRCode|qrcode|MultiFormat|BarcodeFormat` → **0 and hit**.
- Search `_analysis/apktool/smali/**`: directory/file name contains `qr` →**0**; smali string contains `miui/qr` →**0**.
- Conclusion (observation): In the executable code of this APK, **does not have QR code scanning/generation to implement**. If `com.miui.qr` exists in the dex list, it is only
  Unreachable residues or only appear in the resource/obfuscation mapping table and do not constitute a functional surface. `Unknown`: Is there any behavior of launching an external code scanning application through `Intent` (no full list of intents).

### 6.5.2 Installation interception/silent installation

```
[Caller] query(content://guard/install_scan, args=[path, bool, engineName])
  GuardProvider.query case 1008 (GuardProvider.java:403-415)
    → Returns null when args.length < 3 (**must give** to engineName)
    → f(path, parseBoolean(args[1]), args[2]) → g.z().L(..., 2, engineName)  (:415, :249)
```
- It is exactly the same `L()` path as `sync_scan`. There are only two differences: **must carry engineName** and `selectionArgs[1]`. The semantics are interpreted by the caller (whether cloud scanning is allowed, transparent transmission is `z6`).
- The semantics of `Install` are not determined on the Java side, but on the AVL engine: `AVLA.java:83` statement
  `public native int installPackage(String path, int mode, String str2, String str3)`；
  Call points `com/avl/engine/l/b/c.java:27` (mode=0, return value != 1 is considered a failure) and `:80` (mode=1).
  ⇒ **installation interception judgment and "release/intercept" action are in native `libavlm.so`**; Java only passes in the apk path.
- **installs** silently: `PackageInstaller`/`pm install`/`INSTALL_PACKAGES` strings do not appear in this layer and `com` layer (grep 0 hit);
  The only "packaging" entry is the native `installPackage` above, and its permissions/implementation depend on the caller process (system uid).
- Conclusion (inference): GuardProvider itself **does not perform silent installation of**, but only provides "scan apk → return conclusion" and AVL's native installation hook for system side calls.

### 6.5.3 Update/upgrade link addendum (three-channel comparison)

| | Channel A list | Channel B Rule Base | Channel C Engine/Feature Library |
|---|---|---|---|
| trigger | `SecurityService.E(type∈{1,2,3})`; `i0.d.j(incremental)` (`i0/d.java:202`); `GuardProvider` `support_incremental` front `i0.h.h()` (`GuardProvider.java:406`) | Same as left, `s0` client pulls after `MiEngineNG` initialization | `n0.g.N(cb)` build update task (`n0/g.java:1305-1328`) → `c.s()` traversal engine `l(cb)` (`n0/g.java:630-646`) |
| transmission | `h0.b.c(ctx,url,params,salt)`, GET+ signature parameters | Retrofit + OkHttp interceptor (sign/EncryptVersion/AES) | SDK internal (`AVLEngine.checkUpdate/update`, tmsdk `UpdateManager`) |
| address | `https://api.sec.miui.com/antivirus/{viruses,whitelist}` | `https://virus-engine.sec.miui.com` + `/virus/*`、`/blackGrays/*` | Built-in by `q0/m` or SDK (`Unknown`) |
| Verification | `sign`=MD5(parameter+salt `94e6cec2…`) | `sign`=MD5(sort parameter + salt `62b36629…`) + `EncryptVersion` + response AES decryption | SDK Private (`libavlm`) |
| Place the order | `antivirus.db`（`antivirus_db`/`antivirus_db_version`） | Room `EngineDatabase` 8 table | `assets/avlsdk` (1.5 MB, included in the package) + runtime update directory (`Unknown`) |
| fail/rollback | The transaction is not submitted; in full operation, the table is cleared first and then inserted one by one (no backup, recovery by re-pull) | Same as above (Room transaction) | `AvlEngine.l`: rc>0→`a(0,rc)`; rc==0→`a(3,0)`; rc<0→`a(2,rc)` (`AvlEngine.java:176-193`); no rollback, rely on next time `checkUpdate` |
| Progress callback | — | — | `f0.a.b`: `onCheckStarted/onUpdateStarted/a(updateType,errCode)/onUpdateCanceled` → `n0.g$c$C0153c`（`n0/g.java:407-479`）→ `y.c.x(y.e)`/`y.c.K(id)` |

- Naming correspondence (observation): `f0.a.d` = `{UNKNOWN, NETWORKSTATE, DNSANDPHISHING, ARP, SECURITY}` (`f0/a.java:49-55`),
  The updateType of channel C is not a set of encoding; the i6 of `n0.g$c$C0153c.a(i6,i7)` comes directly from the engine callback, and the semantics are determined by the engine (`Unknown`).
- **delta** in A channel is server side diff (`status: new/updated/deleted`, `i0/d.java:139-168`),
  In channel B, it is `version` parameter + Room full replacement; both are not "binary differential packets".

### 6.5.4 WiFi host side parameter flow (completing the TMSDK side of t-vendor)

```
y.a.B/D/O/Q(ssid, observer[, arg])
  → SecurityService lock screen determination (:48-143)
  → n0.k.{e,d,c,f}(ssid, observer[, arg])
      → b(engineName, d, observer, arg): engineName substring matches f0.b enumeration (:57-73)
      → engine is not init? First e(cb) and then n(observer, type, arg) (:75-88)
  → f0.a.n(y.d, f0.a.d, Object) Abstract (:99)
      → TencentEngine.n(): Log + i(new a(type, observer, arg))  (TencentEngine.java:500-505)
          → Asynchronous a: detectNetworkState() → detectARPNoRoot(15000L) → detectSecurity(scanResult) → detectDnsAndPhishing(listener, 0L)
          → Callback observer.v(type, ret) (`dVar` = y.d)
```
- Parameters: `ssid/state` is String, `arg` is Object (ARP branch is scanResult or timeout value); timeout **15000 ms** is hard-coded (`TencentEngine.java:102`).
- The return value of `n0.k.b()` is always -1 (asynchronous), which only means "accepted".

### 6.5.5 Assets and native list (actual measurement in this round)

| File | size | association |
|---|---|---|
| `lib/arm64-v8a/libavlm.so` | 2,304,352 | AVL main engine (`AVLA.installPackage`/scan native) |
| `libflamingo.so` | 1,506,088 | Rust rules engine (`RustSupport`) |
| `libams-1.3.0-mfr.so` | 744,392 | tmsdk |
| `libavlurl.so` | 629,976 | URL rule base/detection (`UrlDetector.setLibPath`) |
| `libTmsdk-2.0.13-mfr.so` | 370,088 | Tencent TMSDK |
| `libturingvd.so` | 240,448 | Device fingerprint (tmsdk) |
| `libdce-1.1.21-mfr.so` | 202,976 | `QFile` loading |
| `libtmf-gm.so` | 177,920 | National Secret (SM2/SM4) |
| `libavlasys.so` | 89,144 | AVL Auxiliary |
| `liburldetectorsys.so` | 14,408 | URL detection JNI bridge |
| `assets/avlsdk` | 1,566,355 | AVL SDK data/configuration |
| `assets/70003.amf` | 680,928 | tmsdk models/rules |
| `assets/mdetector` | 136,636 | Detector data |
| `assets/40706.dat` / `licence.conf` / `licence1.conf` | 9,368 / 392 / 368 | AVL authorization and code table |

## 7. REA Evidence

The first-hand evidence is JADX source code + full tree class index; REA (`inspect_android_class`) evidence-level review was done on the three most critical classes:

| class | evidence_id | Review conclusion |
|---|---|---|
| `p0.b0`（RealScanner） | `ev_37be64d9bf4ee3337e3f814541866a08ced5bb6e504bf369df5e34e701b0ef10` | 9 methods/6 fields; the dependency is exactly `p0.g`+`p0.f`+`p0.z`+`q0.n` (four-piece set of cloud scanning/code/matching/reporting), which is consistent with the §3.2 pipeline; `h(t0.a,k1.d)` is the reporting method |
| `q0.q` (signature interceptor) | `ev_a6ebd88f286d3c6591c70d68e8de3905253145dabb8ea438c7c851bd69072d35` | Only 3 methods; `b(Map):String` = calculate sign, `a(g5.w$a):g5.d0` = OkHttp interception, confirm "signature interceptor" rather than ordinary business class |
| `i0.d` (list synchronization) | `ev_b2d12fbd6b6549d99696d8a7218e115a8d35675f70b019b5e8a3f8bda9f0a23d` | 11 methods; the three-piece set of `e(boolean):Map` (incremental/full parameters), `i(String):boolean` (analysis and storage), and `g(Context):void` (white list) completely corresponds to §3.3 Channel A |

Evidence points that have not been reviewed but are recommended to be supplemented later: `AvlEngine.m` (URL native bridge), `s0.s` (MiEngineNG client assembly), `c0.d` (result cache writing).


