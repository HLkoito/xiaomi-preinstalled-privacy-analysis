> **Historical analysis with subsequent corrections.** Use the [final review](../REPORT.md) for the reviewed findings. It supersedes earlier claims about plaintext IMEI lists, actual uploads under default settings, absolute coverage, and unverified native behavior. The DAU IMEI and IMSI lists use MD5 and remain subject to region, permission, and network checks. Certificate expiry alone does not establish that ciphertext cannot be decrypted. Local identity paths were removed. Historical source line references may point to files outside the published evidence set.

# 20 Cloud scanning protocol + local library + network layer

> Analyst: t-cloud (GuardProvider reverse analysis team)
> Target: `GuardProvider-2.0.3-20231124.0.apk`, SHA-256 `b0c02fa682b8ca82429b471113d0702042ac76234c85fdcd8efeb7c03d313024`
> Main corpus: `_analysis\src\sources\` (3231 JADX files)
> Statement classification: **observed** (source code/bytes directly visible) | **inferred** (founded semantic induction) | **unknown** (not covered)

---

## 1. Overview of Responsibilities

This subsystem is GuardProvider’s “cloud + local threat intelligence” closed loop, which is composed of four parts:

1. **local intelligence database (Room/SQLite)** - `EngineDatabase`, 8 tables, carrying blacklist (guilty_package), whitelist (innocent_package), characteristic code rules (characteristics/ black_gray_chs), gray application (black_gray_app), trusted certificate/installer (trust_certs / trust_installers), and 7 data set version numbers (version table). All **and BLOB are actually listed as AES-CBC cipher text** (see §4.3), and the library file itself is not encrypted.
2. **cloud protocol messages** - `com.miui.guardprovider.sdk.android.pojo.networkdomain` have a total of 21 classes, Gson serialized Kotlin data class, forming three groups of messages "synchronization (blocklist/allowlist/featureCodeList/blackGrays) + cloud scanning (/virus/scan) + result reporting (/virus/localResult/report)".
3. **network layer (two sets coexist)**
   - **old stack**: `h0.b` (`NetworkApiHelper`) - `java.net.HttpURLConnection`, POST, MD5 (sorting parameter + static salt) signature. Service `api.sec.miui.com` (full synchronization of virus database/whitelist), `flash.sec.miui.com` (non-system application list reporting).
   - **new stack**: Retrofit2 + OkHttp3 (`g5`=okhttp3, `d6`=retrofit2, `f6`=retrofit2.http), base address `https://virus-engine.sec.miui.com` (international version `.intl.`). The four OkHttp Interceptors are responsible for signature, request body AES encryption + gzip, response decryption and decompression, and network master switch.
4. **JCE serialization framework** - `com.qq.taf.jce`, **is only built into Tencent tmsdk using** (117 `JceStruct` subclasses are all located in `tmsdkobf`/ `tmsdk.*`), **does not participate in GuardProvider's own cloud protocol** (use Gson for its own protocol). This is a departure from the briefing expectations and is detailed in §4.5.

---

## 2. Component and file list

### 2.1 Database layer

| File | class | Purpose |
|---|---|---|
| `com\miui\guardprovider\sdk\android\db\EngineDatabase.java` | `EngineDatabase` (abstract, inherits `e.y`=RoomDatabase) | Exposed 7 DAO accessors `F()/G()/H()/I()/J()/K()/L()`:16-28 |
| `com\miui\guardprovider\sdk\android\db\EngineDatabase_Impl.java` | `EngineDatabase_Impl` | Generate implementation: `onCreate` DDL: 63-79, `onOpen` verification `g()`: 129-242, version/migration strategy: 351-382 |
| `r0\c.java` / `r0\d.java` | DAO interface/implementation | `black_gray_app` (`t0.e` entity) |
| `r0\e.java` / `r0\f.java` | DAO interface/implementation | `black_gray_chs`（`BlackGrayTraitRecord`） |
| `r0\k.java` / `r0\l.java` | DAO interface/implementation | `characteristics`（`CharacteristicRecord`） |
| `r0\z.java` / `r0\a0.java` | DAO interface/implementation | `guilty_package`（`GuiltyPackageRecord`） |
| `r0\f0.java` / `r0\g0.java` | DAO interface/implementation | `innocent_package`（`InnocentPackageRecord`） |
| `r0\k0.java` / `r0\l0.java` | DAO interface/implementation | `trust_certs`（`TrustCertRecord`） |
| `r0\o0.java` / `r0\p0.java` | DAO interface/implementation | `version`（`t0.u` = `VersionRecord`） |
| `r0\t.java` | `DatabaseCryptoServiceImpl` (implementation `r0.s`) | Column-level encryption and decryption: Java serialization + AES |
| `r0\o.java` | `CommonFiledAdapter` | Convert String/int to ciphertext |
| `r0\q.java` | `ConditionAdapter` | `Condition` and ciphertext conversion + Gson TypeAdapter |
| `r0\a.java` | `DelegatingVirusDao` (implementation `l0.b`) | Bitwise mask aggregation of virusDao(bit1)/blackGrayDao(bit2) of `b()`:39-57 |

> Note: The `trust_installers` table was created in DDL and included in the `g()` check, but **does not correspond to the DAO interface** (`TrustInstallerRecord` DAO is not included in the 8 items of `q()`). Observation: `EngineDatabase_Impl.q()` :370-382.

### 2.2 Entity (pojo) layer

| File | class | Correspondence table |
|---|---|---|
| `pojo\GuiltyPackageRecord.java` | `GuiltyPackageRecord`(id, packageName, signaturesMd5, signaturesSha256, versionCode, threatName, mfSha256) | `guilty_package` |
| `pojo\InnocentPackageRecord.java` | `InnocentPackageRecord`(id, packageName, signaturesMd5, signaturesSha256) | `innocent_package` |
| `pojo\CharacteristicRecord.java` | `CharacteristicRecord`(id, name, strings, condition, threatName, version) | `characteristics` |
| `pojo\BlackGrayTraitRecord.java` | `BlackGrayTraitRecord`(id, name, condition, strings, version) | `black_gray_chs` |
| `pojo\TrustCertRecord.java` | `TrustCertRecord`(id, developer, fingerprintSha256) | `trust_certs` |
| `pojo\TrustInstallerRecord.java` | `TrustInstallerRecord`(id, packageName, vendor) | `trust_installers` (without DAO) |
| `pojo\Condition.java` | `Condition`(rawExp, exp) | Conditional expression (embedded in the above two tables BLOB) |
| `pojo\StringItem.java` | `StringItem`(key, value, target:ScanTarget) | Characteristic string item (encrypted after JSON) |
| `pojo\ScanTarget.java` | `ScanTarget` (abstract) + `AndroidManifest`/`Dex`/`Mf`/`So`/`SpecifiedFile` | Feature matching target file |
| `t0\u.java` | `VersionRecord` (JADX confusing name; `toString` self-certified: 96) | `version` |
| `t0\e.java` | `BlackGrayAppRecord` (JADX confused name) | `black_gray_app` |

### 2.3 Message (networkdomain) layer

See §5.3 for complete field table. The files are all in `com\miui\guardprovider\sdk\android\pojo\networkdomain\`.

### 2.4 Network layer

| File | class | Purpose |
|---|---|---|
| `h0\b.java` | `NetworkApiHelper` | Old stack HTTP client + signature + device parameters |
| `h0\a.java` | `support.mix.scan` detection | Read `com.miui.securitycenter` metaData |
| `i0\d.java` | Virus database/whitelist synchronizer | `api.sec.miui.com/antivirus/{viruses,whitelist}` |
| `i0\b.java` | `miui_antivirus.db` SQLiteOpenHelper | Old version library (`antivirus_db`/`antivirus_db_version`) |
| `i0\e.java` | `mi_engine_white_virus.db` | `mi_virus_white_db` / `mi_detect_app_db` |
| `i0\f.java` | Whitelist library accessor | `whitelist` response to the database + query |
| `i0\l.java` | `MiEngine` local killing | Analysis APK lite → Check `antivirus_db` → Signature/Full package MD5 Comparison |
| `j0\a.java` / `j0\b.java` / `j0\c.java` | `AntiDefraudAppManager` | `flash.sec.miui.com/detect/app` reporting + `mi_detect_app_db` cache |
| `q0\*` | Retrofit interface + interceptor | See §5.2 |
| `s0\s.java` | Dependent assembly (Gson/OkHttpClient/Retrofit) | :76-127 |
| `s0\d.java` | DAO/adapter factory | :140-199 |
| `v0\j.java` | `CryptoService` | AES key derivation + encryption and decryption |
| `v0\k.java` | `GzipService` | gzip compression/decompression |
| `v\a.java` | `IdentifierManager` | Reflection `com.android.id.impl.IdProviderImpl` Take OAID/UDID/VAID/AAID |
| `w0\b.java` | uuid provider | OAID gives priority, falls back to random UUID |
| `r0\w.java` | synchronizer base interface | Unify query parameters (uuid/versionType/timestamp) |
| `r0\g.java`/`r0\h`… | Synchronizer for each data set | Download + Version Advance |
| `p0\f.java` | `CloudScanner` | `/virus/scan` Cloud Scan |
| `p0\b0.java` | `RealScanner` | `/virus/localResult/report` result reporting |
| `p0\j.java` | Dagger dependency graph | Network-wide relationship assembly |


---

## 3. Data flow/timing

### 3.1 Startup and synchronization (incremental pull of version number)

```
GuardApplication starts
  └─ p0.j (Dagger) assembly: s0.s.j() build Retrofit (baseUrl=q0.m.f7703a)
        └─ OkHttpClient = [q0.q sign] + [q0.g i4Req] + [q0.i i4Resp] + [q0.k netGuard] + [s0.s.a log]
  └─ EngineDatabase（Room, version=2, destructive fallback）
  └─ Synchronization task set = p0.j.b.a() ← s0.k.c(...) ← s0.d.g(m,b0,d0,i,g) [p0\j.java:233, :277-279]
```

Timing for each data set synchronizer (such as `r0.g` black and gray production application):

```
1. Assemble query:
     uuid        = p0.w.f7555a.a()          [r0\w.java:23]
     versionType = stable|development|alpha [r0\w.java:24-29]
     timestamp   = System.currentTimeMillis() [r0\w.java:30]
   ← super.c(dVar) get Map
2. Append the local version of the data set: map["dataVersion"] = q0.b(dVar) [r0\g.java:294-309]
3. GET /blackGrays/blocklist?…&sign=…                    [q0\b.java:11]
4. Response BlackGrayAppResponse{code,desc,data{dataVersion, blocklist[]}}
5. Mapping networkdomain → Room entities one by one (hash unification toLowerCase)
     u0.a.b(BlackGrayApp) → t0.e          [u0\a.java:16-28]
6. dao.o(newItems) / dao.j(deletedItems)                 [r0\g.java:153-165]
7. vm writes new dataVersion: q0Obj.i(newVersion) [r0\g.java:166-171]
```

### 3.2 Cloud scanning (`/virus/scan`) end-to-end

```
Caller (SecurityService → p0.n MiEngineNG)
  └─ Scanner chain: p0.b0 RealScanner { codeScanner(g), cloudScanner(f), matcherScanner(z), uploadApi(q0.n) }
        ├─ Local: p0.g CodeScanner / p0.z MatcherScanner / p0.y rule compilation
        └─ Cloud: p0.f CloudScanner [p0\b0.java:202-211]

p0.f CloudScanner (l0.e pipeline stage):
  a(dVar) :372-379 read scan_params(int) → f7380c
                    f7381d = (scan_params & 8) == 0 ← bit3 Turn off cloud scanning
                    f7382e.c(scan_params): bit0|1 → threat mode, bit2 → black and gray mode
  c(cVar):383-385 coroutine execution b.k()
  b.k()   :256-345
     if (!f7381d) throw IOException("cloud scan is denied!!!")     :258-261
     To-be-scanned list = v4.i.c(cVar):262-271
     CloudScanApkInfo[] = item by item k(t0.a):273-277
        packageName / versionCode(String) / f() / b() / g()         :358-368
     Public parameters: androidVersion, miuiVersion, timestamp(ms),
               uuid=w.f7555a.a(), versionType, scanType=f7380c       :279-286
     retrofit q0.d.a(List<CloudScanApkInfo>, @QueryMap Map)          :291
     → POST {base}/virus/scan (body encrypted by q0.g+gzip)
     Verification: index corresponds one to one and packageName must be unequal (to prevent misalignment): 212-216, :315-318
     Consumption: f.a.b(app, payload) Merged conclusion: 241
  a.b():80-104 Threat mode + black and gray mode superposition, Threats takes priority.
                    Threats→t0.o（infected），BlackGrays→t0.f（gray）
  a.a():44-52 Cloud threat name → t0.m.c(name) → t0.k(level,name,desc,null,-1,"cloud")
                    authority is fixed to "cloud", versionCode is fixed to -1

Result reporting (incorporated into the same RealScanner pipeline) p0.b0.h():215-297
   infected → one ResultReportData for each t0.k
   gray → threatLevel hardcoded "BlackGrays"
   POST {base}/virus/localResult/report?timestamp=<ms>  [q0\n.java:12-13]
   Exception: d6.j / IOException only Log.e("RealScanner","Result upload failed"), no retry: 291-295
```

### 3.3 `flash.sec.miui.com/detect/app` end-to-end (real link different from briefing expectations)

```
j0.c.h(ctx) / j0.c.a()                    [j0\c.java:155-165, :25-55]
  └─ g(ctx) enumeration getInstalledPackages(0), skip flags&1 (system application):133-153
       Each item {pkg, version, signature=MD5(cert[0] raw bytes), appname=URLEncode(label)}
  └─ b(list)                                                       :57-80
       body = {
         timestamp: second-level timestamp,
         os        : "MIUI-"+Build.VERSION.INCREMENTAL,
         biz_id    : "virus_scan",
         uuid : w0.b.b() ← Random persistent UUID, not OAID
         content[] : {pkg, version, signature, appname}
       }
       POST https://flash.sec.miui.com/detect/app
            appKey = "6988567a-4220-4b51-bc2d-ccdec27a74a1"        :75
            Content-Type: application/json; append "sign" to body
  └─ Response: {data:[{pkg, signature, version, basisOf}], error_code?}
       Only keep items with basisOf == 2 (= judged to be unsafe/not trusted):41-44, j0\a.java:51-58
  └─ Drop into the database: j0.a.b(json) → delete the entire table and then re-insert mi_detect_app_db(package, signatures)
       [j0\a.java:37-70]
  └─ Query: j0.a.c(pkg) → SELECT … WHERE package=? AND signatures=MD5(cert[0])
       [j0\a.java:72-91]
```

**Conclusion (inference)**: This chain is not a "sample-by-sample cloud scan", but a coarse-grained synchronization of **'s "batch reporting of non-system application lists ←→ overall delivery of anti-fraud/distrusted application lists"**. `biz_id=virus_scan` is just a business tag; the interface does not accept or return any single-sample conclusion of "whether the APK is a virus", and there is no TTL or version number in the database table (full replacement every time). The real single sample cloud judgment method is `https://virus-engine.sec.miui.com/virus/scan`.

---

## 4. Analysis of key logic one by one

### 4.1 Database DDL (complete)

**observes**: `EngineDatabase_Impl$a.a(h.i)`: 63-79, REA bytecode level review is consistent (ev_3415eba8...).
**observation**: database version = 2 (`new a0(iVar, new a(2), "8856aeeb52eb3fc27eb4570c1cd903b7", "4033f04c51ab315bb8679d5c0f44fb16")`:357).

| # | table name | Column definition | Primary key/index |
|---|---|---|---|
| 1 | `trust_certs` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`developer` BLOB NOT NULL<br>`fingerprintSha256` BLOB NOT NULL | UNIQUE INDEX `index_trust_certs_fingerprintSha256`(`fingerprintSha256` ASC) |
| 2 | `trust_installers` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`packageName` BLOB NOT NULL<br>`vendor` BLOB NOT NULL | UNIQUE INDEX `pk_index`(`packageName` ASC) |
| 3 | `innocent_package` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`packageName` BLOB NOT NULL<br>`signaturesSha256` BLOB **NULL**<br>`signaturesMd5` BLOB NOT NULL | UNIQUE INDEX `index_innocent_package_packageName_signaturesMd5`(`packageName`,`signaturesMd5`) |
| 4 | `characteristics` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`name` BLOB NOT NULL<br>`strings` BLOB NOT NULL<br>`condition` BLOB NOT NULL<br>`threatName` BLOB NOT NULL<br>`version` INTEGER NOT NULL | UNIQUE INDEX `index_characteristics_name`(`name` ASC) |
| 5 | `guilty_package` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`packageName` BLOB NOT NULL<br>`signaturesMd5` BLOB NOT NULL<br>`signaturesSha256` BLOB **NULL**<br>`versionCode` BLOB **NULL**<br>`threatName` BLOB NOT NULL<br>`mfSha256` BLOB **NULL** | UNIQUE INDEX `index_guilty_package_packageName_signaturesMd5_mfSha256`(`packageName`,`signaturesMd5`,`mfSha256`) |
| 6 | `black_gray_chs` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`name` BLOB NOT NULL<br>`condition` BLOB NOT NULL<br>`strings` BLOB NOT NULL<br>`version` INTEGER NOT NULL | UNIQUE INDEX `index_black_gray_chs_name`(`name` ASC) |
| 7 | `black_gray_app` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`packageName` BLOB NOT NULL<br>`signaturesMd5` BLOB NOT NULL<br>`signaturesSha256` BLOB NOT NULL<br>`versionCode` BLOB **NULL**<br>`mfSha256` BLOB NOT NULL | UNIQUE INDEX `index_black_gray_app_packageName_signaturesMd5_mfSha256`(`packageName`,`signaturesMd5`,`mfSha256`) |
| 8 | `version` | `characteristicVersion` INTEGER NOT NULL<br>`guiltyPackageVersion` INTEGER NOT NULL<br>`innocentPackageVersion` INTEGER NOT NULL<br>`trustCertVersion` INTEGER NOT NULL<br>`trustInstallerVersion` INTEGER NOT NULL<br>`blackGrayChsVersion` INTEGER NOT NULL<br>`blackGrayAppsVersion` INTEGER NOT NULL<br>`id` INTEGER NOT NULL | PRIMARY KEY(`id`), **no autoincrement** |
| — | `room_master_table` | `id` INTEGER PRIMARY KEY<br>`identity_hash` TEXT | Inside Room; write `42 / '8856aeeb52eb3fc27eb4570c1cd903b7'` |

Migrate **to**:
- **observes**: `j(Map)` returns `Arrays.asList(new f.a[0])` (empty migration array): 361-363; `p()` returns `new HashSet()` (no autoMigration specification): 366-368.
- **observes**: The 8 mappings of `q()` include `o.class` (`o` = unknown DAO placeholder) and `m0.class` (`n0.v()`, no entity table): 370-382.
- **infers that** uses Room destructive migration (`fallbackToDestructiveMigration`), and the version upgrade means clearing and rebuilding the database; this also explains that DDL all uses `CREATE TABLE IF NOT EXISTS` and the `isDestructiveMigration` path exists.
- **Unknown**: The builder assembly point of `e.y` (RoomDatabase subclass) is not located (it should be on the layer above `s0\r.java` and is not expanded this time).

**DAO SQL Full quantity** (`r0` package, item by item):

| DAO implementation | SQL | location |
|---|---|---|
| `r0\d` | `SELECT * FROM black_gray_app WHERE packageName = ? AND signaturesMd5 = ?` | d.java:348 |
| `r0\d` | `SELECT * FROM black_gray_app` | d.java:364 |
| `r0\d` | `INSERT OR REPLACE INTO black_gray_app (…) VALUES (nullif(?,0),?,?,?,?,?)` | d.java:79 |
| `r0\d` | `DELETE FROM black_gray_app WHERE id = ?` | d.java:126 |
| `r0\d` | `UPDATE OR ABORT black_gray_app SET … WHERE id = ?` | d.java:144 |
| `r0\f` | `SELECT * FROM black_gray_chs` / `… WHERE name = ?` | f.java:324 / 346 |
| `r0\f` | `INSERT OR REPLACE … black_gray_chs (id,name,condition,strings,version)` | f.java:83 |
| `r0\l` | `SELECT * FROM characteristics` / `… WHERE name = ?` | l.java:349 / 372 |
| `r0\l` | `INSERT OR REPLACE … characteristics (id,name,strings,condition,threatName,version)` | l.java:85 |
| `r0\a0` | `select * from guilty_package where packageName = ? and signaturesMd5 = ?` | a0.java:372 |
| `r0\a0` | `select * from guilty_package` | a0.java:388 |
| `r0\a0` | `INSERT OR REPLACE … guilty_package (id,packageName,signaturesMd5,signaturesSha256,versionCode,threatName,mfSha256)` | a0.java:80 |
| `r0\g0` | `select * from innocent_package where packageName = ? and signaturesMd5 = ?` (Appears in two places: reading single item + existence judgment) | g0.java:296 / 341 |
| `r0\g0` | `select * from innocent_package` | g0.java:312 |
| `r0\g0` | `INSERT OR REPLACE … innocent_package (id,packageName,signaturesSha256,signaturesMd5)` | g0.java:76 |
| `r0\l0` | `select * from trust_certs` / `… where fingerprintSha256 = ?` | l0.java:269 / 299 / 310 |
| `r0\l0` | `INSERT OR REPLACE … trust_certs (id,developer,fingerprintSha256)` | l0.java:73 |
| `r0\p0` | `SELECT * FROM version` / `… WHERE id = ?` | p0.java:171 / 209 |
| `r0\p0` | `INSERT OR IGNORE INTO version (…) VALUES (?,?,?,?,?,?,?,?)` | p0.java:31 |

- **observes**: all `INSERT OR REPLACE` are first bound to `mVar.F(1, record.getId())`, and the column value is written as `nullif(?, 0)`; that is, when **id=0, a new row is inserted (incremented), and when id>0,** is overwritten.
- **observes**: All String fields pass `r0.o.d(String)` (encrypted) before writing, and pass `r0.o.b(byte[])` (decrypted) when reading.
- **observes**: The insert column order of `innocent_package` is `(signaturesSha256, signaturesMd5)` (consistent with the table definition order), and the entity constructor parameter order is `(packageName, signaturesSha256, signaturesMd5)` - the two orders are different but the binding is by column name, without misalignment.

### 4.2 Library content encryption (column level AES)

**observes** `r0\t.java` (`DatabaseCryptoServiceImpl`):

| method | behavior |
|---|---|
| `a(Object)` :17-47 | `ObjectOutputStream.writeObject(obj)` → `v0.j.b(bytes)` AES encryption |
| `b(int)` :50-55 | `ByteBuffer.allocate(4).putInt(i)` → AES encryption |
| `c(byte[], Class)` :58-81 | AES decryption → press `Class` to go to ByteBuffer (Boolean/Byte/Char/Short/Int/Long/Float/Double) or `ObjectInputStream.readObject()` |

**observes** `v0\j.java` (`CryptoService`, REA review ev_73f513b6…):

| item | value |
|---|---|
| algorithm | `AES/CBC/PKCS5Padding` :59 / :79 |
| Key source | classpath `META-INF/flamingo.dat`, read 16 bytes after `skip(144)`: 38-41 |
| Key transformation | The first 8 bytes and the last 8 bytes are respectively **big-endian long**, respectively `^ 4831221107097930563L` (= `0x430BF1FFFFF10B43`): 43-44 |
| AES-128 key **restored by** | ASCII `wcw1xbplx7aijwqr`（hex `77 63 77 31 78 62 70 6c 78 37 61 69 6a 77 71 72`） |
| IV | `new SecureRandom().generateSeed(16)`, **random and front**: output = `IV(16) ‖ ciphertext` :58 / :66 |
| Decrypt | Take `bytes[0..16)` as IV and `bytes[16..]` as ciphertext: 81-84 |
| `EncryptVersion` header value | `d()` Constant return `"0"`:99-101 |

**cross-validates**: `META-INF/flamingo.dat` does exist in APK, 304 bytes (`_analysis\apktool\unknown\META-INF\flamingo.dat`); the original 16 bytes at offset 144..159 are `34 68 86 ce 87 93 7b 2f 3b 3c 90 96 95 86 7a 31`, and after XOR, they are all printable ASCII - the key self-certifies successfully.

**Inference (Important)**: Because the IV is random every time, **the encryption result of the same plaintext will be different every time**. So:
1. The three columns UNIQUE index **on `guilty_package` / `innocent_package` / `black_gray_app` cannot perform the deduplication effect** (the same APK will insert new rows in each round of synchronization);
2. For equivalent queries such as DAO in `WHERE packageName = ? AND signaturesMd5 = ?`, the "current re-encrypted parameters" are used to match the "historical ciphertext". It is almost impossible for **to hit**.
3. Therefore, the actual matching path should be `l0.b.b()` full table load → memory decryption → memory comparison (`r0.a.b()`:48-57 aggregates the full table list of two DAO, consistent with this).

- **unknown**: The connection between the caller of `r0.a` (`DelegatingVirusDao`) and the memory matcher (`p0.z` MatcherScanner / `p0.y` CodeScanner) has not been chased line by line, so "whether the equivalent query is dead code" has not been confirmed at runtime.

### 4.3 Conditional expression engine (`Condition`)

- **observes**: `engine.a(str)` is parsed during `Condition(String rawExp)` construction, and `IllegalArgumentException("Bad input expression:" + rawExp)` is thrown on failure: 20-28. Engine statically built by `new v5.b().g()` :16.
- **observes**: `reduce(Map<String,Boolean> matches)` uses `n(matches)` as variable context evaluation: 50-59; `reduce(v1.l supplier)` uses lazy supplier + cache: 115-120,: 92-101.
- **observes**: `equals`/`hashCode` is based only on `rawExp` string: 30-48.
- **infers**: The feature library rule form is the **Boolean expression** (variable name = `StringItem.key`). The client locally calculates `key→bool` for each file and then evaluates it. `v0\l.b(s)` will verify that "the variable set appearing in the expression == the key set of strings", otherwise it will throw `"<name> isn't computable."`:55-58.
- **observes**: `v0\l.a(String)` uses the delimiter `" ()!\t\n\r\f&|"` to split the token: 28 - indicates that the operator set of the expression contains at least `!`, `&`, `|`, and parentheses.
- **Unknown**: Which expression library is the `v5` package (self-developed or transplanted)? The syntax definition of `v5\b.java`(3.3KB) / `v5\a.java`(38.9KB) is not expanded this time.

### 4.4 Two matching semantics of `StringItem`

**observation** `v0\l.d(s)` :61-79:
- If `t0.j.b(stringItem)` is true → split `t0.j.a(stringItem)` according to `\s`, requiring each segment to match the regular `([\da-fA-F][\da-fA-F])|(\?[\da-fA-F])|([\da-fA-F]\?)` - that is, **hexadecimal byte pattern + `?` wildcard binary feature** (used for dex/so byte scan).
- Otherwise → `new p4.k(stringItem.getValue())` is compiled into ordinary regular expression - **text regular feature**.

**infers**: The semantics of `StringItem.value` are diverted by a certain Boolean switch (`t0.j.b`) to form a "binary feature code/text regular" dual-mode feature library.

**observes the JSON form of** `StringItem` (`toString`:91): `{"key": …, "value": …, "scanFile": <ScanTarget pathPattern>}`, and the `target` field is renamed with `@c("scanFile")`:15-16.
**observes four fixed mode constants of** `ScanTarget`: `/AndroidManifest.xml`, `/*.dex`, `/META-INF/MANIFEST.MF`, `/lib/**/*.so`: 14-17, and `SpecifiedFile` (`q0\o.java:32`).

### 4.5 JCE serialization framework (`com.qq.taf.jce`)

**encoding rules (observation `JceOutputStream` / `JceStruct`)**

Byte order: **big endian** (all go to `ByteBuffer` default order; `putShort/putInt/putLong` writes directly).

Tag/type header encoding (`writeHead(byte type, int tag)`:82-92):

```
tag < 15 : single byte = type | (tag << 4)
tag ≥ 15: two bytes = (type | 0xF0), tag // The upper limit of tag is 255, if exceeded c("tag is too large")
```

Type code table (`JceStruct`:7-21):

| code | constant | meaning | Length/Writing |
|---|---|---|---|
| 0 | `BYTE` | byte | 1 byte; when the **value is 0, use `ZERO_TAG` instead to save bytes** (:110-115) |
| 1 | `SHORT` | short | 2 bytes; downgraded to BYTE when `-128..127` |
| 2 | `INT` | int | 4 bytes; downgraded to SHORT when `-32768..32767` |
| 3 | `LONG` | long | 8 bytes; `int32` range downgraded to INT |
| 4 | `FLOAT` | float | 4 bytes |
| 5 | `DOUBLE` | double | 8 bytes |
| 6 | `STRING1` | short string | 1 byte length + bytes (length ≤ 255) |
| 7 | `STRING4` | long string | 4 bytes length + bytes (length > 255) |
| 8 | `MAP` | Map | 4 bytes size + itemized `(key@tag0, value@tag1)` (:188-198) |
| 9 | `LIST` | Array/List | 4 bytes size + itemized `@tag0` (:25-32, :266-276) |
| 10 | `STRUCT_BEGIN` | Nested structure starts | Followed by `writeTo` content |
| 11 | `STRUCT_END` | end of structure | `write(JceStruct,tag)` automatically adds `writeHead(11,0)` (:278-284) |
| 12 | `ZERO_TAG` | Compact representation of the value 0 | 1 byte header only, no payload |
| 13 | `SIMPLE_LIST` | `byte[]` special | Header + `writeHead(0,0)` + 4-byte length + raw bytes (:209-215) |

The default string encoding is `"GBK"` (:21, :119), which can be changed to `setServerEncoding`.

**JCE structure** actually used in this project

- **observed**: `grep "extends JceStruct"` in the whole tree, a total of 117 **117**: 113 `tmsdkobf`, 4 `tmsdk.*` (`tmsdk\common\module\qscanner\impl\{a,d,g,h}`).
- **observes**: There are only two references to `grep "com.qq.taf.jce.(JceInputStream|JceOutputStream|JceStruct|JceDisplayer)"` in the whole tree except `tmsdkobf`/`tmsdk.*`, and both are **only borrowing the constant**:
  - `g5\y.java:182` → `new byte[]{JceStruct.SIMPLE_LIST, 10}` (okhttp internal constant array)
  - `o5\k.java:22` → a byte pattern array containing `JceStruct.SIMPLE_LIST/ZERO_TAG/STRUCT_END` (wireless detection module)
- **Observation**: `com.miui.guardprovider` Self-owned package **zero** references JCE; all self-owned messages (`networkdomain`) are Gson (`l.*` = gson, `m.c` = `SerializedName`).
- **Conclusion (observation + inference)**: JCE is the serialization layer **that comes with** Tencent tmsdk (TencentEngine / AVL engine SDK), packaged with SDK; GuardProvider The cloud protocol does not use it. Therefore, the interpretation of "JCE message" in this task should be based on the tmsdk private protocol, not the GuardProvider cloud protocol.

### 4.6 Network layer positioning

#### 4.6.1 Two sets of clients

| stack | client | Features | User |
|---|---|---|---|
| old | `java.net.HttpURLConnection`（`h0\b.java`） | Handwriting POST, `addRequestProperty`, 4096 buffered loop reading | `i0\d`（api.sec）、`j0\c`（flash.sec） |
| new | OkHttp3 + Retrofit2（`g5`/`d6`/`f6`） | Kotlin, coroutine suspension function, Interceptor chain | `q0\*` / `s0\*` / `p0\f` / `p0\b0` |

**is not used**: no self-developed socket client, no Volley, no WebView network.

#### 4.6.2 Old stack request format and signature (`h0\b`)

**method / head (observation)**

| Path | function | Method | Content-Type | Body |
|---|---|---|---|---|
| form | `c(ctx,url,Map,appKey)` :80-147 | POST | `application/x-www-form-urlencoded; charset=UTF-8` :73 | `k=v&k=v…` (`URLEncoder.encode(...,"UTF-8")`, **in HashMap iteration order,** not sorted): 219-236 |
| JSON type | `d(ctx,url,JSONObject,appKey)` :149-217 | POST | `application/json` :62 | `JSONObject.toString()`, **signature is written first `"sign"` key**: 160-161 |

**timeout**: `setConnectTimeout/ setReadTimeout` are all taken as `com.xiaomi.onetrack.g.b.f2462b` (constant, same value): 333-334; `setUseCaches(false)`.

**signature algorithm (observation + REA bytecode review ev_96277430…)**

```
i(map, secret):
  keys = sort(map.keySet()) // Dictionary order, Comparator = String.compareTo:272-289
  s = join("k=v", "&") // Value is taken from map (original string, not URL encoded)
  s     += "&" + secret
  b64    = Base64.encodeToString(s.getBytes("UTF-8"), NO_WRAP=2)
  return MD5hex_upper(b64)                 // j() :291-299: String.format("%1$032X", …)
```

- **Observation**: `l(JSONObject)` When collecting signature materials, **excludes `sign` key**, and specifically judges `content` (JSONArray chooses `.toString()`): 308-322.
- **observes**: `m(ctx,map,secret)` = `f(ctx,secret)` + caller map + `sign` :324-329.
- **observes**: When used for `d()`, the map of signature material is generated by `l(jSONObject)`, and `put("sign", …)` occurs within `d()` and before `a()`: 160-161 - that is, `sign` It does not participate in its own calculation, and **sends** with the body in plaintext JSON key.

**Two static "appKey/secret" (observation)**

| constant | Purpose | Source |
|---|---|---|
| `94e6cec2-19dc-4bb6-823c-613f72d3ea16` | `api.sec.miui.com/antivirus/{whitelist,viruses}` | `i0\d.java:55`, `i0\d.java:203` |
| `6988567a-4220-4b51-bc2d-ccdec27a74a1` | `flash.sec.miui.com/detect/app` | `j0\c.java:75` |

#### 4.6.3 Old stack device identification and authentication

**observation** `h0\b.f(ctx, secret)`: 238-248 fixed 7 parameters:

| parameters | value | Source |
|---|---|---|
| `device` | `SystemProperties("ro.product.device","unknown")` | :34, :240 |
| `carrier` | `SystemProperties("ro.carrier.name","unknown")` | :37, :241 |
| `region` | `SystemProperties("ro.product.mod_device","unknown")` | :40, :242 |
| `miuiVersion` | `"MIUI-" + Build.VERSION.INCREMENTAL` | :43, :243 |
| `versionType` | `stable` / `development` / `alpha` | :301-306, :244 |
| `appVersion` | Constant `"1"` | :46, :245 |
| `mi` | `MD5hex( android_id + "-" + secret )` | :258-270, :246 |
| `sign` | See §4.6.2 | :327 |

- **observes**: the source of the device identification is `Settings.Secure.getString(cr, "android_id")`:261; if it is empty, it returns `""`:266-268. **is not used IMEI / serial number / OAID**.
- **observes**: `android_id` is spliced ​​with secret and then MD5 is used to weaken the cross-service association, but it is still a stable device fingerprint.
- **infers** (not covering `e5.a.b`/`f5.a.a` implementation): The hexadecimal case of `mi` is unknown.

#### 4.6.4 New stack (Retrofit/OkHttp) configuration

**observes** `s0\s.java`:

| Assembly | location | content |
|---|---|---|
| Gson | :76-90 | `registerTypeAdapter(ScanTarget, q0.o)`、`(Condition, r0.q)`、`(CloudScanPayload.a, CloudScanPayload.b)`、`(u0.g, u0.i)` |
| OkHttpClient | :102-114 | `addNetworkInterceptor(q0.k)`; `addInterceptor(q0.q)`; `addInterceptor(q0.g)`; `addInterceptor(q0.i)`; `addInterceptor(s0.s.a)` (log, TAG=`MiEngineNG`) |
| Retrofit | :121-127 | `baseUrl(q0.m.f7703a)` + `client(zVar)` + `GsonConverterFactory`（`e6.a.f(gson)`） |

**base address (observation)** `q0\m.java:12`:
```java
f7703a = Build.IS_INTERNATIONAL_BUILD
       ? "https://virus-engine.sec.intl.miui.com"
       : "https://virus-engine.sec.miui.com";
```

**Interceptor one by one (observation)**

| Interceptor | location | behavior |
|---|---|---|
| `q0.q`（signGenInterceptor，application） | q0\q.java:16-48 | Get all query parameters (**, if the caller comes with `sign`, directly throw `IllegalArgumentException`**:39-41) → `TreeMap` sorting → `"k1=v1&k2=v2"` → Append `"&62b36629-ecb4-4725-9f33-ccb0fd158191"` → UTF-8 → `MD5hex_upper` (`l0.f.b(bytes,"MD5",true)`, true→`'X'` in uppercase, see `l0\f.java:97-104`, `:70`)→As query parameter `sign` append: 47 |
| `q0.g`（i4Req，application） | q0\q.java… is actually q0\g.java:25-38 | ① Add request header `EncryptVersion: v0.j.d()` (=`"0"`); ② If there is body: get bytes → `gzipService.b()` compression → `cryptoService.b()` AES encryption → replace the body with `c0.g(original ContentType, ciphertext, 0,0)` |
| `q0.i`（i4Resp，application） | q0\i.java:24-43 | ① Verification response header `EncryptVersion` must be equal to local `d()`; **is not equal and HTTP≠200 → directly return**; **is not equal and HTTP=200 → throw `IOException("Header EncryptVersion not found.")`**:32-37; ② Decrypt `cryptoService.c(body)` → `gzipService.a()` Decompress → Body swap |
| `q0.k`（netGuard，**network** interceptor） | q0\k.java:13-20 | `if (!p0.n.f7475f.a().get()) throw new IOException("Network access disabled.")` - Global network master switch (`AtomicBoolean(false)` is off by default, `p0\n.java:32`, set by `p0.n.m(boolean)`: 828-830) |
| `s0.s.a` (log) | s0\s.java:35-53 | Plug the `EncryptVersion` header again; print the original/ciphertext/plaintext body, TAG=`MiEngineNG` |

**signature salt (second one, different from the old stack)**: `62b36629-ecb4-4725-9f33-ccb0fd158191` (`q0\q.java:22`, REA review ev_94cce8a5…).

#### 4.6.5 New stack device identification and authentication token

| parameters | value | Source |
|---|---|---|
| `uuid` | `p0.w.f7555a.a()` (in-process volatile cache, throw `IllegalStateException("Not init yet!")` if not initialized) | `r0\w.java:23`、`p0\w.java:15-21`、`p0\f.java:283` |
| `versionType` | `stable` / `development` / `alpha` | `r0\w.java:24-29`、`p0\f.java:284` |
| `timestamp` | `System.currentTimeMillis()` (**ms**) | `r0\w.java:30`、`p0\f.java:282` |
| `scanType` | `String.valueOf(scan_params)` | `p0\f.java:285` |
| `androidVersion` | `Build.VERSION.SDK_INT` | `p0\f.java:280` |
| `miuiVersion` | `Build.VERSION.INCREMENTAL` (**has no `MIUI-` prefix**, which is different from the old stack) | `p0\f.java:281` |
| Each dataset version | `dataVersion` / `characteristicVersion` … | `r0\g.java:296-309` etc. |

- **observes**: `uuid` has no `Authorization` / `Bearer` / `token` header - the new stack of **is "signature is authentication"**, without session token.
- **observes**: The true value source of `uuid` is injected from outside (`p0/w.b(String)`), and the injection point is not located this time.
- **observes**: The old stack has another set of identifiers (`oa` = OAID) - see §4.6.6.

#### 4.6.6 OAID / Equipment identification panorama

| logo | Generate/Source | Purpose | Source |
|---|---|---|---|
| Old stack `mi` | `MD5( ANDROID_ID + "-" + appSecret )` | All `h0.b` requests | `h0\b.java:258-270` |
| `oa`（OAID） | Reflection `com.android.id.impl.IdProviderImpl.getOAID(Context)` | `api.sec.miui.com/antivirus/{viruses}` request body | `v\a.java:30-44`、`i0\d.java:43`、`:50` |
| Random persistent UUID | `UUID.randomUUID()` → SharedPreferences `key_uuid` | ① The international version replaces OAID; ② `uuid` of `flash.sec.miui.com` | `w0\b.java:17-26`、`i0\d.java:39`/`:48`、`j0\c.java:63` |
| `uuid` (new stack) | Externally injected cache string | All `virus-engine` requests | `p0\w.java` |

- **observes**: `v\a` simultaneously reflects `getUDID/getOAID/getVAID/AAID` in four methods: 33-36, but only `getOAID` is exposed and used: 42-44.
- **infers**: `i0\d.e(boolean)` uses random UUID for the international version and OAID for the domestic version in order to avoid overseas OAID compliance restrictions.
- **Unknown**: The actual caller of `p0.w.b()` (who injected OAID/ANDROID_ID/other strings).

#### 4.6.7 TLS verification

- **observation**: The whole tree `grep TrustManager|HostnameVerifier|CertificatePinner|sslSocketFactory|checkServerTrusted` hits all fall on **OkHttp3 library itself** (`g5\*`, `p5\*`, `q5\*`, `s5\*`) and **AVL The engine's own network** (`com\avl\engine\i\f\*`, its `SSLSocketFactory` is passed in from the outside).
- **observes**: There is no full trust implementation in the form of `SSLContext.init(null, allTrusting, …)`; `p5\c.java:109` uses `TrustManagerFactory.getInstance("PKIX","BCJSSE")` + `init((KeyStore)null)` = **The system default trust root**; `s5\d.java` is the OkHttp standard `OkHostnameVerifier`.
- **observes**: **has no `CertificatePinner`**, no `HostnameVerifier` coverage, and no `android:usesCleartextTraffic` bypass (both URL in the old stack are https).
- **Conclusion (inference)**: TLS is the default verification of the **system, without certificate pinning, and the verification of** is not turned off. This also means that the **system CA can be injected into the middleman** (if packet capture is required, MitM agent + user CA in API 26+ needs to apply unfixed `networkSecurityConfig`; this APK has not found a custom network security config, waiting for confirmation by another member `res\xml`).

#### 4.6.8 Error code/retry/backoff

| layer | behavior | location |
|---|---|---|
| old stack | **only determines `getResponseCode() != 200` → return null**; exceptions will always return null after `Log.e("NetworkApiHelper", "exception when do post request", e)` | `h0\b.java:92-96`, `:162-166`, `:143-146` |
| Old stack business error | The response JSON containing the `error_code` key is considered a failure and will be discarded directly. | `i0\d.java:108-110`, `i0\f.java:49-51`, `j0\c.java:33-35`, `j0\a.java:42-44` |
| New stack business code | `u0.e.getCode()`: **`code == 0` is regarded as failure** (`throw IOException("Response code:" + code + ", threatDesc:" + desc)`); `data == null` is also thrown | `p0\f.java:198-204`, `:301-307` |
| New stack reporting | Capture `d6.j` / `IOException` and log only `"Result upload failed"` | `p0\b0.java:291-295` |
| New stack protocol header | Response header `EncryptVersion` is missing and HTTP=200 → `IOException("Header EncryptVersion not found.")` | `q0\i.java:36` |
| network switch | Off → `IOException("Network access disabled.")` | `q0\k.java:19` |
| Cloud scanning strategy | `scan_params & 8 != 0` → `IOException("cloud scan is denied!!!")` | `p0\f.java:258-261` |

- **observes**: **does not find any retry / exponential backoff / backoff in the whole tree to implement** (no `retryOnConnectionFailure` explicit setting, no backoff timer, no retry count).
- **infers**: The retry responsibility is moved up to the caller (scheduling layer/SecurityCenter outside `s0`). The inside of APK is "single call, give up if failed, and trigger again externally next time".

#### 4.6.9 Network gateway

**observation** `GuardApplication.e(Context)`: 38 and up:

| branch | Conditions |
|---|---|
| Domestic version (`!IS_INTERNATIONAL_BUILD`) | `SystemProperties("persist.sys.sc_allow_conn", false)` |
| International version | Need to meet `com.miui.securitycenter` **is installed and** `persist.sys.sc_allow_conn` is true |
| International version and `ro.miui.customized.region == "jp_kd"` | Then superimpose `Settings.Secure "antivirus_engine_network_enable" == 1` |

**Infers**: All networking capabilities of GuardProvider **are controlled by the remote/local switch of the system properties of MIUI Security Center**, which is turned off by default. This is an outer gate than `q0.k` (`q0.k` is an in-process AtomicBoolean and `GuardApplication.e` is a system property).

### 4.7 `guard/support_incremental` and `i0.h.h(ctx)`

> **does not match the task description.** needs to be corrected: `i0.h.h(ctx)` **is not the incremental scan determination of**, but **"Whether the security center enables full scan"**. `guard/support_incremental` exposes the value **with the inverse semantics**.

**observation chain**

| location | content |
|---|---|
| `GuardProvider.java:92` | `uriMatcher.addURI("guard", "support_incremental", 1009)` |
| `GuardProvider.java:405-411` | `case 1009:` `if (!i0.h.h(getContext())) return null;` otherwise return `MatrixCursor(new String[]{"support"})` and `addRow(new Object[]{1})` |
| `i0\h.java:84-92` | `h(Context)`: If `d5.a.f2965a` is true → false; otherwise, take `applicationInfo.metaData.getBoolean("miui.support_full_scan")` of `com.miui.securitycenter` |
| `SecurityService.java:145-151` | AIDL `e()`：`if (h.h(ctx)) return !d.g(ctx).f().isEmpty(); return false;` |

**Semantics (inference)**

- `i0.h.h(ctx) == true` ⇒ Security Center states that **supports full scanning of** (`miui.support_full_scan=true`).
- `guard/support_incremental` returns `{support:1}` when `h.h(ctx)==true` is false, and **returns null** (not 0) when it is false.
- Therefore, the external semantics of URI are actually a capability bit of **"the machine has complete engine scanning capabilities" and**. The name `support_incremental` is contrary to the implementation semantics/misnamed.
- `SecurityService.e()` is more precise: only when full scan is supported, "the local risk library is not empty" is returned - that is, the caller is told that "there are incremental conclusions readable".

**key cross-validation**: `h0\a.java` is another independent determination - `securitycenter.support.mix.scan` (`h0\a.java:9-19`, call points `x\a.java:142`, `x\b.java:150`, `:171`). The two are different metaData keys, **cannot be mixed with**:
- `securitycenter.support.mix.scan` → Hybrid engine (symmetry of `guardprovider.support.mix.scan=true`)
- `miui.support_full_scan` → Full scanning capability bit

**incremental synchronization protocol (the real one)**

| Dataset | endpoint | version parameters | Drop-in list |
|---|---|---|---|
| signature rules | `GET /virus/feature/codeList`（`q0\c.java:11`） | `dataVersion` | `characteristics` |
| Virus blacklist | `GET /virus/blocklist`（`q0\e.java:11`） | `dataVersion` | `guilty_package` |
| Whitelist + Developer Certificate | `GET /virus/allowlist`（`q0\f.java:11`） | `allowlistVersion` / `developerVersion` | `innocent_package` + `trust_certs` |
| Characteristics of black ash production | `GET /blackGrays/feature/codeList`（`q0\a.java:11`） | `dataVersion` | `black_gray_chs` |
| Black ash product application | `GET /blackGrays/blocklist`（`q0\b.java:11`） | `dataVersion` | `black_gray_app` |
| old virus database | `POST https://api.sec.miui.com/antivirus/viruses`（`i0\d.java:203`） | `version` + `updateType=incremental\|global` | `antivirus_db` (old SQLite) |
| old whitelist | `POST https://api.sec.miui.com/antivirus/whitelist`（`i0\d.java:55`） | No version (full coverage) | `mi_virus_white_db` |
| Anti-fraud list | `POST https://flash.sec.miui.com/detect/app`（`j0\c.java:75`） | No version (full coverage) | `mi_detect_app_db` |

**old stack `updateType` semantics (observation `i0\d.i(String)`:102-185)**

| server field | client action |
|---|---|
| `version` / `releaseTime` | `delete antivirus_db_version` first and then insert |
| `updateType == "global"` | `delete antivirus_db` (**is cleared and then** is rebuilt): 121-123 |
| `updateType == "incremental"` | Keep the old rows and process them in increments of entries `status`: 124-126 |
| Entry `status == "new" \| "updated"` | `delete … WHERE package=? AND signatures=? AND md5=?` after `insert`:145-164 |
| Entry `status == "deleted"` | Only `delete`:166-168 |
| Item Other `status` | ignore |
| `status` field is missing | When `"new"` is processed (`optString(J,"new")`, `b.J == "status"`): 139 |

- **observes**: `i0\d.j(boolean incremental)` first `g(ctx)` (pulling the whitelist) each time, and then processes the virus database: 202-210 - that is, the whitelist is fully refreshed every time, and the virus database can be incremented.
- **observation**: `i0\d.e(boolean)` assembly `{version, updateType, oa}`:36-45; `oa` from `com.xiaomi.onetrack.b.m.f2268a == "oa"`:7.
- **observes**: `n0\g.java:643-644` is the call point of the old stack synchronization: `i0.d.d().a(ctx); Log.i("AntivirusManager", "update whitelist: " + i0.d.d().j(true))` - **is fixed to `incremental=true` to trigger**.

### 4.8 Cloud scanning trigger conditions (`scan_params` bit field)

**observes** `p0\f.java:370-379`:

```java
int v = dVar.a().b("scan_params");      // Taken from scan parameter context
this.f7380c = v;                         // Passed to server scanType
this.f7381d = (v & 8) == 0;              // bit3=0 → Allow cloud scanning
this.f7382e.c(v):
    f7383a = (v & 3) != 0;               // bit0|bit1 → Threat (blacklist) mode
    f7384b = (v & 4) != 0;               // bit2 → black and gray production mode
```

| bit | mask | meaning |
|---|---|---|
| 0,1 | `& 3` | Threat scanning mode (Threats) |
| 2 | `& 4` | BlackGrays scan mode (BlackGrays) |
| 3 | `& 8` | **disables cloud scanning** (set to 1 → `cloud scan is denied!!!`) |
| The rest | — | The entire original value is reported as `scanType` |

- **Observation**: Constructor default `f7380c = 3`:353 (= threat mode, cloud scan allowed).
- **observes**: `guardprovider.scan_properties=2` (Manifest meta-data, see briefing) should correspond to this bit field, `2` = only bit1 → threat mode, cloud scanning allowed, non-black and gray mode.
- **Infers**: `scan_params` is injected by the caller (`l0.d` scan context) and the source is in SecurityCenter via AIDL Scan parameters issued; **Local "inconclusive/suspicious" does not constitute an upgrade condition** - whether cloud scanning is determined by the bit field, once allowed, **all items to be scanned will be sent in batches** `/virus/scan`, and there is no filtering logic of "only uploading suspicious items" (`p0\f.java:271-291` `list5` Full quantity `map`).

### 4.9 Upload content: only upload fingerprints, not the APK body

**observation** `p0\f.k(t0.a)` :358-368 → `CloudScanApkInfo(packageName, versionCode, f(), b(), g())`：

| Upload fields | Source (`t0.a` accessor) | Semantics (inference) |
|---|---|---|
| `packageName` | `getPackageName()` | package name |
| `versionCode` | `String.valueOf(aVar.d())` | version number (stringified) |
| (3rd) | `aVar.f()` | apk certificate MD5 |
| (4th) | `aVar.b()` | apk certificate SHA-256 |
| (5th) | `aVar.g()`（null→`""`） | META-INF/MANIFEST.MF of SHA-256 |

**cross-validation**: The field name and order are clearly `(packageName, versionCode, apkCertificateMD5, apkCertificateSHA256, mfFileSHA256)` (`CloudScanApkInfo.java:16-26`) in the `CloudScanApkInfo` constructor parameter name, and the value order of `p0\f.k()` corresponds to one.

- **Observation**: **does not exist for any code path uploading APK byte stream / dex / so** - no `RequestBody.create(File)`, no `MultipartBody`, `q0\d.a`'s only `@Body` parameter is `List<CloudScanApkInfo>`.
- **infers**: Cloud determination can only be based on the "package name + version + certificate fingerprint + MF fingerprint" quadruple, which belongs to **fingerprint white/blacklist matching**, not code-level cloud killing. This is consistent with the response field of `CloudScanPayload` (no codeFeature, no feature string).

### 4.10 Respond to consumption and feed back the local area

**observes** `p0\f.java`:

| steps | logic | location |
|---|---|---|
| Index alignment check | The number of responses must correspond to the request one-to-one, and `payload.packageName != apkInfo.packageName` (**Note: This is the "not equal" assertion**) | :212-216、:315-318 |
| Threats → Infection | `resultType.contains(Threats)` → `t0.d.c(app, h1.p.e(a(payload)))` | :67-78 |
| BlackGrays → gray product | `resultType.contains(BlackGrays)` → `t0.d.b(app)` | :54-65 |
| Clean / Unknown → unchanged | Return as is | :60-62、:73-75 |
| Dual mode enabled at the same time | Threats take priority over gray products; when both are "infected", use `t0.d.e(...,1,...)` to merge | :94-103 |
| Unspecified mode | `Log.w("CloudScanner","Scan mode is not specified yet.The original input will be returned.")` | :91 |
| Threat information structure | `threatName` is required (null will throw an exception); `t0.m.c(name)` parses out level/desc → `t0.k(level, name, desc, null, -1, "cloud")` | :44-52 |
| Result authority | Fixed string `"cloud"`; version fixed `-1` | :51 |

- **observes**: `CloudScanPayload.resultType` is `EnumSet{a}`, enum `{Clean, Threats, BlackGrays, Unknown}` (`CloudScanPayload.java:23-32`), serialized by custom Gson adapter `CloudScanPayload.b` as `name()` string (:34-50).
- **observes**: The cloud result **is not written to the Room library**. It only changes the status of `t0.a` (application scan object), which is then reported to the server by `p0\b0.h()`, and superimposed by the local rules of `p0.z`/`p0.y`.
- **infers**: The version of `-1` and the authority of `"cloud"` make the cloud conclusion distinguishable on the UI and cannot be overwritten by the local feature library version number.
- **Unknown**: The final location between `t0.d.b/c/e` (conclusion factory) and UI/DB has not been launched; "Whether the cloud conclusion will enter the `riskData`/`c0.b` result library" needs to be confirmed by another member.

### 4.11 Local library TTL / Expiration policy

- **observes**: None of Room’s eight tables **has a timestamp column, no `expire`/`ttl`/`updateTime` column** (§4.1 DDL full table), and no `DELETE … WHERE time < ?` Cleanup of form SQL.
- **observes**: the only "freshness" mechanism is the 7 integer version numbers of the `version` table + server-side `dataVersion` comparison (§4.7).
- **observes**: `antivirus_db_version` of `i0\d` (old stack) has the `release_time` column (`i0\b.java:15`), but **is only dropped in the response and does not participate in any expiration judgment**.
- **infers**: The expiration policy is completely implemented by the server through "issuing `status=deleted` entries" or "`updateType=global` whole table replacement"; there is no local TTL on the client side. If you are not connected to the Internet for a long time, the library content will be retained permanently.

### 4.12 Scan the location of the cloud in the pipeline

**observes** `p0\b0.java:202-211`:

```java
b0(codeScanner g, cloudScanner f, matcherScanner z, uploadApi q0.n)
```

| Stage class | Responsibilities (inferred from self-named assembly with `p0.j`) |
|---|---|
| `p0.g` CodeScanner (32.3KB, maximum) | Code-level feature scanning (rule matching) |
| `p0.f` CloudScanner | Cloud fingerprint verification (main body of this section) |
| `p0.z` MatcherScanner（20.9KB） | Local black/white/grey list matching (corresponding to Room 8 table) |
| `p0.y`（5.0KB） | Compilation of characteristic rules (`CodeScanner`'s "bad input characteristic" comes from here: 70) |
| `p0.b0` RealScanner | Arrangement + Result Reporting |

---

## 5. Configuration items/constants/protocol fields

### 5.1 Constant table

| Name | value | Semantics | Source |
|---|---|---|---|
| Old stack salt (virus database/whitelist) | `94e6cec2-19dc-4bb6-823c-613f72d3ea16` | MD5 signature static salt | `i0\d.java:55`, `:203` |
| Old Stack Salt (Lightning Detection) | `6988567a-4220-4b51-bc2d-ccdec27a74a1` | MD5 signature static salt | `j0\c.java:75` |
| Xinzhan Salt | `62b36629-ecb4-4725-9f33-ccb0fd158191` | MD5 signature static salt | `q0\q.java:22` |
| AES key | `wcw1xbplx7aijwqr`（16B ASCII） | Column encryption + request/response body encryption | `v0\j.java:38-45` + `flamingo.dat` |
| Key XOR constant | `4831221107097930563` = `0x430BF1FFFFF10B43` | Key obfuscation | `v0\j.java:43-44` |
| key file | `META-INF/flamingo.dat`, offset 144, length 16 | key carrier | `v0\j.java:38-41` |
| `EncryptVersion` | `"0"` | Encryption protocol version | `v0\j.java:99-101` |
| `appVersion` | `"1"` | Old stack version parameters | `h0\b.java:46` |
| Rule engine library version | `"flamingo-android_release"` | Kotlin `@Metadata` module name | `Metadata(...)` for each pojo |
| `Room identity_hash` | `8856aeeb52eb3fc27eb4570c1cd903b7` | Current schema fingerprint | `EngineDatabase_Impl.java:79`, `:357` |
| old schema fingerprint | `4033f04c51ab315bb8679d5c0f44fb16` | historical schema | `EngineDatabase_Impl.java:357` |
| Library version number | `2` | `a(2)` | `EngineDatabase_Impl.java:357` |
| `biz_id` | `"virus_scan"` | Lightning detection service identification | `j0\c.java:62` |
| `basisOf` filter value | `2` | Keep only entries with this value | `j0\c.java:41`, `j0\a.java:51` |
| `android_id` key | `"android_id"` | `Settings.Secure` | `h0\b.java:261` |
| Network switch properties | `persist.sys.sc_allow_conn` | System property gate | `GuardApplication.java:38+` |
| Japanese customization area | `jp_kd` | Add switch judgment | `GuardApplication.java:38+` |
| Japan area additional switch | `Settings.Secure "antivirus_engine_network_enable"` | Only allowed if ==1 | Same as above |
| Full scanning capabilities | metaData `miui.support_full_scan` | `i0.h.h()` | `i0\h.java:91` |
| Mixed scanning capability bits | metaData `securitycenter.support.mix.scan` | `h0.a.a()` | `h0\a.java:14` |
| This package contains mixed scan bits | metaData `guardprovider.support.mix.scan=true` | Manifest | Briefing |
| Scan parameters default | `scan_properties=2` / `f7380c=3` | bit field | Manifest / `p0\f.java:353` |
| Signature algorithm name | `"MD5"` | Three signatures | `h0\b.java:293`, `q0\q.java`(`l0.f.b`), `j0\c.java:92` |
| Encryption algorithm name | `"AES/CBC/PKCS5Padding"` | Column+Transport Encryption | `v0\j.java:59`, `:79` |

### 5.2 Cloud API complete list

| # | stack | Method | Path | Service base address | Request | response type | definition |
|---|---|---|---|---|---|---|---|
| 1 | new | GET | `/blackGrays/feature/codeList` | `virus-engine.sec[.intl].miui.com` | `@QueryMap` | `BlackGraysResponse` | `q0\a.java:11` |
| 2 | new | GET | `/blackGrays/blocklist` | Same as above | `@QueryMap` | `BlackGrayAppResponse` | `q0\b.java:11` |
| 3 | new | GET | `/virus/feature/codeList` | Same as above | `@QueryMap` | `CharacteristicsResponse` | `q0\c.java:11` |
| 4 | new | POST | `/virus/scan` | Same as above | `@Body List<CloudScanApkInfo>` + `@QueryMap` | `CloudScanResponse` | `q0\d.java:13-14` |
| 5 | new | GET | `/virus/blocklist` | Same as above | `@QueryMap` | `GuiltyPackagesResponse` | `q0\e.java:11` |
| 6 | new | GET | `/virus/allowlist` | Same as above | `@QueryMap` | `InnocentPackagesAndSignaturesResponse` | `q0\f.java:11` |
| 7 | new | POST | `/virus/localResult/report` | Same as above | `@Query("timestamp") long` + `@Body List<ResultReportData>` | `ResultRequestResponse` | `q0\n.java:12-13` |
| 8 | old | POST | `https://api.sec.miui.com/antivirus/viruses` | Fixed | form: `{version, updateType, oa, +7 public, sign}` | JSON `{version, releaseTime, updateType, viruses[]}` | `i0\d.java:203` |
| 9 | old | POST | `https://api.sec.miui.com/antivirus/whitelist` | Fixed | form: `{oa, +7 public, sign}` | JSON `{whitelist:[{packageName, signature}]}` | `i0\d.java:55` |
| 10 | old | POST | `https://flash.sec.miui.com/detect/app` | Fixed | JSON `{timestamp, os, biz_id, uuid, content[], sign}` | JSON `{data:[{pkg, signature, version, basisOf}]}` | `j0\c.java:75` |
| — | External SDK | — | `https://virus-engine.sec[.intl].miui.com` | Same as #1-7 | Engine/signature database update (`TencentEngine`/AVL) | — | `q0\m.java:12` (constants are reused) |

> Whether the new stack is GET/POST: `q0.d` / `q0.n` contains `@f6.a` (`@Body`) → must be POST; `q0.a/b/c/e/f` only `@QueryMap` (`@f6.u`) has no body → inferred to be GET (`f6.f`/`f6.o` annotation class only has `value()`, which cannot be distinguished statically and is marked as inferred).

### 5.3 Complete field table of message structure

> All classes are Kotlin data classes; **has no `@SerializedName` annotation** (except `rule_name`/`threat_name` for `StringItem.target` and `CharacteristicRecord/BlackGrayTraitRecord`), so JSON field name = Java field name.

#### 5.3.1 Request/Response Envelopes and Public

| class | Field | Type | Semantics (inference) | Source |
|---|---|---|---|---|
| `u0.e` (interface) | `code` | int | Business code. **0 = Failed** | `u0\e.java:5` |
| | `desc` | String | Failure description | `u0\e.java:9` |
| | `data` | Object | load | `u0\e.java:7` |
| `CloudScanResponse` implements `e` | `code` | int | Same as above | `CloudScanResponse.java:11` |
| | `desc` | String | Same as above | :13 |
| | `data` | `List<CloudScanPayload>` | Packet-by-packet conclusion | :12 |
| `u0.g` (enumeration) | `deleted` / `online` | — | Dataset entry status | `u0\g.java:4-6` |
| `u0.f` (interface) | — | — | Synchronization entry base interface with `status` | Implemented by `BlackGray`/`Characteristic`/`GuiltyPackage`/`InnocentPackage`/`TrustCert`/`BlackGrayApp` |

#### 5.3.2 Cloud scanning

**`CloudScanApkInfo` (request body element)**

| Field | Type | Semantics | Source |
|---|---|---|---|
| `packageName` | String | package name | `CloudScanApkInfo.java:13` |
| `versionCode` | **String** | version number (stringified) | :14 |
| `apkCertificateMD5` | String | Signature certificate MD5 | :10 |
| `apkCertificateSHA256` | String | Signature certificate SHA-256 | :11 |
| `mfFileSHA256` | String | SHA-256 of `META-INF/MANIFEST.MF` | :12 |

**`CloudScanPayload` (response body element)**

| Field | Type | Semantics | Source |
|---|---|---|---|
| `threatName` | String (nullable) | Threat name (required for Threats) | `CloudScanPayload.java:20` |
| `packageName` | String | Package name (corresponding to the request in order) | :18 |
| `versionCode` | int | version number | :21 |
| `apkCertificateSHA256` | String | Certificate SHA-256 | :17 |
| `resultType` | `EnumSet<CloudScanPayload.a>` | `{Clean, Threats, BlackGrays, Unknown}` | :19, :23-32 |

**`ResultReportData` (reporting element)** — 13 fields

| Field | Type | Semantics (inference) | Source |
|---|---|---|---|
| `threatLevel` | String | Threat level (`t0.k.threatLevel`) | `ResultReportData.java:19` |
| `threatName` | String | Threat name | :20 |
| `malwareCodeFeature` | String | Hit code signature (`t0.k.codeFeature`) | :15 |
| `version` | int | Feature database version (`t0.k.version`) | :21 |
| `packageName` | String | package name | :18 |
| `appName` | String | Application name | :13 |
| `versionCode` | int | version number | :22 |
| `apkCertificateMD5` | String | Certificate MD5 | :11 |
| `apkCertificateSHA256` | String | Certificate SHA-256 | :12 |
| `mfFileSHA256` | String | MF fingerprint (empty string pocket bottom) | :16 |
| `miuiVersion` | String | `Build.VERSION.INCREMENTAL` | :17 |
| `androidVersion` | int | `Build.VERSION.SDK_INT` | :10 |
| `device` | String | `Build.DEVICE` | :14 |

**`ResultRequestResponse`**

| Field | Type | Semantics | Source |
|---|---|---|---|
| `code` | int | Business code | `ResultRequestResponse.java:10` |
| `desc` | String | Description | :11 |

#### 5.3.3 List synchronization

**`GuiltyPackage` (blacklist entry)**

| Field | Type | Semantics | Source |
|---|---|---|---|
| `packageName` | String | package name | `GuiltyPackage.java:15` |
| `versionCode` | Integer | Version number (can be empty → hit all versions) | :18 |
| `apkCertificateMD5` | String | Certificate MD5 | :12 |
| `apkCertificateSHA256` | String | Certificate SHA-256 (can be empty) | :13 |
| `mfFileSHA256` | String | MF fingerprint (can be empty) | :14 |
| `threatName` | String | Threat name | :17 |
| `status` | `u0.g` | `deleted` / `online` | :16 |

**`GuiltyPackagesData` / `GuiltyPackagesResponse`**

| class | Field | Type | Semantics | Source |
|---|---|---|---|---|
| `GuiltyPackagesData` | `blocklist` | `List<GuiltyPackage>` | blacklist | `GuiltyPackagesData.java:11` |
| | `dataVersion` | int | Dataset version (for delta) | :12 |
| `GuiltyPackagesResponse` | `code` / `desc` / `data` | int / String / `GuiltyPackagesData` | envelope | `GuiltyPackagesResponse.java:10-12` |

**`InnocentPackage`**

| Field | Type | Semantics | Source |
|---|---|---|---|
| `packageName` | String | package name | `InnocentPackage.java:14` |
| `apkCertificateMD5` | String | Certificate MD5 | :12 |
| `apkCertificateSHA256` | String | Certificate SHA-256 (can be empty) | :13 |
| `status` | `u0.g` | Status | :15 |

**`TrustCert` (Trusted Developer Certificate)**

| Field | Type | Semantics | Source |
|---|---|---|---|
| `developer` | String | Developer name | `TrustCert.java:13` |
| `apkCertificateSHA256` | String | Certificate SHA-256 | :12 |
| `status` | `u0.g` | Status | :14 |

**`InnocentPackagesAndSignatures` (whitelist + developer certificate, one response with two sets of data)**

| Field | Type | Semantics | Source |
|---|---|---|---|
| `allowlist` | `List<InnocentPackage>` | whitelist | `InnocentPackagesAndSignatures.java:11` |
| `allowlistVersion` | int | whitelist version | :12 |
| `developerSignList` | `List<TrustCert>` | Trusted certificate list | :13 |
| `developerVersion` | int | Certificate list version | :14 |

**`InnocentPackagesAndSignaturesResponse`**：`code` / `desc` / `data:InnocentPackagesAndSignatures`（:10-12）

#### 5.3.4 Feature code synchronization

**`Characteristic` (threat signature rule)**

| Field | Type | Semantics | Source |
|---|---|---|---|
| `ruleName` | String | Rule name | `Characteristic.java:16` |
| `version` | int | Rule version | :20 |
| `strings` | `List<StringItem>` | feature string collection | :18 |
| `condition` | `Condition` | Boolean conditional expression | :15 |
| `threatName` | String | Threat name | :19 |
| `status` | `u0.g` | Status | :17 |

**`CharacteristicsData`**：`dataVersion: int`、`featureCodeList: List<Characteristic>`（:11-12）
**`CharacteristicsResponse`**：`code` / `desc` / `data:CharacteristicsData`（:10-12）

**`StringItem` (characteristic string item)**

| Field | Type | JSON name | Semantics | Source |
|---|---|---|---|---|
| `key` | String | `key` | Variable name (conditional expression reference) | `StringItem.java:13` |
| `value` | String | `value` | Hexadecimal byte pattern **or** text regular | :17 |
| `target` | `ScanTarget` | **`scanFile`** | match target | :15-16 |

**`ScanTarget` (polymorphic, relying on Gson adapter `q0.o` to string to and from)**

| Subclass | pathPattern | Semantics | Source |
|---|---|---|---|
| `AndroidManifest` | `/AndroidManifest.xml` | Scan the list only | `ScanTarget.java:14`, :22-28 |
| `Dex` | `/*.dex` | Scan all dex | :15, :32 |
| `Mf` | `/META-INF/MANIFEST.MF` | Scan MF | :16 |
| `So` | `/lib/**/*.so` | Scan native library | :17 |
| `SpecifiedFile` | any string | Specify file | `q0\o.java:32` |

**`BlackGray` (black and gray production characteristic rules)**

| Field | Type | Semantics | Source |
|---|---|---|---|
| `ruleName` | String | Rule name | `BlackGray.java:16` |
| `strings` | `List<StringItem>` | Feature string | :18 |
| `condition` | `Condition` | conditional expression | :15 |
| `version` | int | version | :19 |
| `status` | `u0.g` | Status | :17 |

**`BlackGraysData`**：`dataVersion:int`、`featureCodeList:List<BlackGray>`（:11-12）
**`BlackGraysResponse`**：`code`/`desc`/`data:BlackGraysData`（:10-12）

**`BlackGrayApp` (black and gray product application entry)**

| Field | Type | Semantics | Source |
|---|---|---|---|
| `packageName` | String | package name | `BlackGrayApp.java:15` |
| `versionCode` | Integer | Version number (can be empty) | :17 |
| `apkCertificateMD5` | String | Certificate MD5 | :12 |
| `apkCertificateSHA256` | String | Certificate SHA-256 | :13 |
| `mfFileSHA256` | String | MF fingerprint | :14 |
| `status` | `u0.g` | Status | :16 |

**`BlackGrayAppData`**：`blocklist:List<BlackGrayApp>`、`dataVersion:int`（:11-12）
**`BlackGrayAppResponse`**：`code`/`desc`/`data:BlackGrayAppData`（:10-12）

#### 5.3.5 `networkdomain.a` (Gson adapter factory)

| item | content | Source |
|---|---|---|
| class | `a implements e1.c`（Dagger `Provider` of Gson `TypeAdapterFactory`） | `networkdomain\a.java:6` |
| Singleton | Static inner class holds | :9-13 |
| output | `new CloudScanPayload.b()` | :19-21 |
| Purpose | Register Gson adapter for `CloudScanPayload.a` (enumeration) | Consumed by `s0\s.java:85` |

### 5.4 OkHttp / Retrofit assembly parameters

| item | value/source | Source |
|---|---|---|
| baseUrl | `q0.m.f7703a` | `s0\s.java:124` |
| ConverterFactory | `e6.a.f(gson)`（GsonConverterFactory） | `s0\s.java:124` |
| OkHttp interceptor order (application) | `q0.q` → `q0.g` → `q0.i` → `s0.s.a` | `s0\s.java:110-112`, `p0\j.java:257` |
| networkInterceptor | `q0.k` | `s0\s.java:109` |
| Request header `EncryptVersion` | `"0"` | `q0\g.java:29`, `s0\s.java:43` |
| Log TAG | `MiEngineNG` | `s0\s.java:41-51` |
| Old stack timeout | `com.xiaomi.onetrack.g.b.f2462b`（connect=read） | `h0\b.java:333-334` |

---

## 6. Permissions and attack surface/risk points

| # | Risk point | Basis | Level |
|---|---|---|---|
| R1 | The **encryption key is hard-coded in APK resource**: `META-INF/flamingo.dat` is clearly placed in the package, and the key is only made once XOR; it has been completely restored to `wcw1xbplx7aijwqr`. Anyone can unlock all BLOB columns and all HTTPS request/response bodies in Room | `v0\j.java:38-45` + actual measurement | high |
| R2 | The three signatures of **are all `MD5 (sorting parameters + static salt)`**. There is no HMAC, no timestamp anti-replay, no nonce, and no session key issued by the server; the salt value is hard-coded. Signatures can be forged offline | `h0\b.java:272-299`, `q0\q.java:16-30` | high |
| R3 | **request/response body encryption key is globally shared and the version is fixed `"0"`**, no key rotation mechanism (`d()` always returns `"0"`); once leaked, it will be common to all versions and all devices | `v0\j.java:99-101` | high |
| R4 | **column-level encryption uses random IV, destroying the equivalent query and unique constraints**: UNIQUE index is invalid for the same plaintext (the ciphertext is different each time), `WHERE packageName=?` never hits; blacklist deduplication fails → infinite expansion of the library + matching must be decrypted in the entire table | `v0\j.java:58`, §4.2 | in |
| R5 | **TLS No pinning, no customization TrustManager**: System CA injection can be the middleman; after superimposing R1 (the key is available), the attacker can completely decrypt/tamper with the cloud protocol | §4.6.7 | in |
| R6 | **network gate relies on system properties `persist.sys.sc_allow_conn`**: A component (or root) with write permissions for system properties can forcibly turn on/off all networking, which is an unprotected implicit control plane | `GuardApplication.java:38+` | in |
| R7 | **`guard/support_incremental` returns `null` instead of error** when capacity is insufficient: the caller may misjudge "null cursor" as "unlimited" or crash | `GuardProvider.java:405-411` | low |
| R8 | **`CloudScanPayload` The subscript alignment assertion direction is suspicious**: `if (equals(pkgA,pkgB)) throw "Check failed."` - the semantics is "if the package name is the same, an error will be reported", which is contrary to the intuition of "one-to-one correspondence"; if the server echoes according to the package name (common practice), an exception will be thrown 100% | `p0\f.java:212-216`, `:315-318` | Medium (pending verification) |
| R9 | **`code == 0` is judged as failure**: Contrary to the common "0=success" convention; if the server returns 0 to indicate success, the client will permanently regard it as an error | `p0\f.java:198-204`, `:301-307` | Medium (pending verification) |
| R10 | **`/virus/scan` Upload the package name + version + certificate fingerprint of all applications to be scanned at once**: No batching, no fixed upper limit of de-identification; combined with stable device identification (`oa`/`uuid`/`mi`) to form an application list side channel that can be tracked for a long time | `p0\f.java:271-291`, §4.6.3/§4.6.5 | Medium (Privacy) |
| R11 | **`flash.sec.miui.com/detect/app` reports all non-system applications (including application names)**, `appname` only URL-encode is not desensitized; the response result is replaced with `mi_detect_app_db` in the entire table and there is no version verification | `j0\c.java:57-80`, `j0\a.java:37-70` | Medium (Privacy) |
| R12 | **without any retry/backoff**: Network jitter will cause the entire round of signature library synchronization to fail, and the failure will be silent (log only); combined with no TTL, it can cause long-term use of expired libraries | §4.6.8, §4.11 | low |
| R13 | **DB destructive migration** (empty migration array + `p()` empty set): version upgrade clears the database. If the upgrade fails, it will cause a full re-download and no incremental recovery. | `EngineDatabase_Impl.java:361-368` | low |
| R14 | **`trust_installers` table is created but there is no DAO**: `pk_index` index and data are never read by the application, suspected to be legacy/unwired functions | `EngineDatabase_Impl.java:65-66`, `q()` No corresponding item | low |
| R15 | **`TrustInstallerRecord` Business semantics can be misunderstood**: The name implies "trusted installer", but there is no code path to write/read, and its true purpose is unknown | Same as above | low (information) |
| R16 | The **`scan_properties` bit field semantics depends on the caller's agreement on**: `scan_params` is taken from the `l0.d` context. If the external IPC caller can control this value, cloud scanning (`bit3`) can be turned off - but `scan_properties=2` is fixed by the Manifest, and the actual controllability is unknown. | `p0\f.java:370-379` | low |

---

## 7. Unresolved issues

| # | question | Got the clue | Suggest next steps |
|---|---|---|---|
| Q1 | Injection point and value source of `p0.w.b(String)` (new stack `uuid`) | `p0\w.java:23-33` only caches; the caller is not located | Whole tree search `p0.w.f7555a.b(` / `w.f7555a` |
| Q2 | `RoomDatabase.Builder` assembly point of `EngineDatabase` (whether `fallbackToDestructiveMigration` and `openHelperFactory` use SQLCipher) | `s0\r.java` is referenced by `s0.r.a(pVar, …)` in `p0\j.java:184` but is not expanded | Read `s0\r.java` (level 1350B) and above `s0\j.java` |
| Q3 | db file name of `Room.databaseBuilder(...).build()` | String does not appear in read code | grep `"engine.db"` / `.databaseBuilder(` |
| Q4 | Whether the equivalent query of `guilty_package` / `innocent_package` / `black_gray_app` is dead code (random IV leads to never hitting) | §4.2 If the inference is true, it must be dead code | Dynamic verification: run the same APK for two consecutive rounds of synchronization and observe the increase in the number of rows |
| Q5 | Whether the index alignment assertion direction (R8) of `p0.f` and the negative judgment (R9) of `code==0` are real bugs | The source code is straight-line and readable, but the semantics are counter-intuitive. | Packet capture + measured `/virus/scan` response |
| Q6 | `t0.a` The precise correspondence between the five accessors `b()/d()/f()/g()` and `apkCertificateMD5/SHA256/mfSHA256/versionCode` | It has been deduced according to the order of `CloudScanApkInfo` constructor parameters; `t0.a` is the interface (`t0\a.java:4`) and has not been implemented. | Read `t0.d` (`t0\d.java` 2290B) with specific `t0.a` implementation |
| Q7 | Syntax definition of `v5` conditional expression engine (supported operators/functions) | Delimiter `" ()!\t\n\r\f&|"` Prompt `!&|()` | Read the parser entry of `v5\b.java` / `v5\a.java` |
| Q8 | The basis for determining "binary features vs text regularity" in `v0\l` `t0.j.b(StringItem)` | `v0\l.java:63` call | Read `t0\j.java` (Level 511B) |
| Q9 | How does `t0.d.b/c/e` (Conclusion Factory) put the cloud conclusion into the result library/UI; whether the cloud conclusion is written into the `c0.b` risk library | `p0\b0.java:235-278` Only saw the report, but not the inventory | Read `t0\d.java` + `c0\b` |
| Q10 | Strong consistency between `dataVersion` in the cloud response and the actual table content (whether within a transaction) | `r0\g.java:153-171` writes the table first and then the version, but there is no explicit `withTransaction` | `i(int)` implementation that reads `r0\r.java` / `r0\q0.java` |
| Q11 | Why does `o5\k.java` / `g5\y.java` quote the `JceStruct` constant (whether tmsdk and okhttp share a certain binary protocol) | Only constant references, no `JceInputStream` calls | Low priority; read `o5\k.java` context (wifidetect) |
| Q12 | `trust_installers` table and `TrustInstallerRecord` writing method | No DAO in the whole tree | grep `TrustInstallerRecord(` |
| Q13 | Is there a custom `network_security_config` (affects R5) | Not checked this time `res\xml` | Another member checked `apktool\res\xml\network_security_config*.xml` and Manifest `android:networkSecurityConfig` |
| Q14 | `i0\d` `sync` trigger frequency of the old stack (timing/boot/on-demand) | `n0\g.java:643` The call point is visible and the schedule is not located. | Read the calling context of `n0\g.java` |

---

## 8. REA evidence ID list

| # | evidence_id | Operation | target | Verify content |
|---|---|---|---|---|
| 1 | `ev_c243d381cb5d8b7d5b504fd6ee872e0567597a94270a5a8d8e68e055c87649da` | `inspect_android_class` | `com.miui.guardprovider.sdk.android.pojo.networkdomain.CloudScanPayload` | 5 fields + 16 methods + 2 internal classes (`a`=enumeration, `b`=Gson adapter); confirm that `resultType` is `EnumSet<CloudScanPayload$a>`, `versionCode` is `int` - consistent with the source code |
| 2 | `ev_962774300548c3ef27d8bf5ece9a1e5faa8d555bdccbf20ac094b0b5059f76a8` | `inspect_android_method` | `h0.b.i(Map,String)` | Bytecode level confirmation signature = sort → `k=v&…` → append secret → `Base64(…,2)` → `j()`(MD5 upper hex) |
| 3 | `ev_73f513b6297f98af28c05f9a72e76fc7de048e96e2b2cf54d1f0d31842be04b6` | `inspect_android_method` | `v0.j.a()` | Confirm key read path `META-INF/flamingo.dat` + `skip(144)` + double long XOR `4831221107097930563` + `SecretKeySpec(…,"AES")` |
| 4 | `ev_3415eba82c6fc7618221660100562cfc2a37b45c3e1c4439e2af205dbbfdb3b4` | `inspect_android_method` | `com.miui.guardprovider.sdk.android.db.EngineDatabase_Impl$a.a(h.i)` | 7 table DDL original text + 6 UNIQUE INDEX + `room_master_table` and `identity_hash=8856aeeb52eb3fc27eb4570c1cd903b7`, consistent with the table in §4.1 verbatim |
| 5 | `ev_94cce8a5ff954f5a64232136f7c1bf09efc1134e057a292a523eec6ff5cfe818` | `inspect_android_method` | `q0.q.b(Map)` | Confirm new stack signature = `TreeMap` Sort → `StringJoiner("&")` → Append `62b36629-ecb4-4725-9f33-ccb0fd158191` → UTF-8 → `l0.f.b(bytes,"MD5",true)` |

---

## Attached: List of conflicts/fixes with shared briefings

| # | briefing statement | This observation | Conclusion |
|---|---|---|---|
| C1 | 「`flash.sec.miui.com/detect/app` #Cloud Lightning Scan」 | This endpoint is `j0.c` AntiDefraudAppManager's **non-system application list is reported in batches/the distrust list is issued to** as a whole, and `mi_detect_app_db` is dropped, which is not a sample-by-sample cloud killing | **corrects**: the real single sample cloud scan is `https://virus-engine.sec.miui.com/virus/scan` (`q0\d.java`) |
| C2 | "`com\qq\taf\jce\*.java` - JCE serialization framework...point out which JCE structures are actually used in this project" | 117 `JceStruct` subclasses **, all** in `tmsdkobf`/`tmsdk.*`; `com.miui.guardprovider` has zero references in its own package, and all its own protocols use Gson | **corrects**: JCE is dependent on Tencent tmsdk and does not carry the GuardProvider cloud protocol |
| C3 | Report pojo list is 9 | There is a 10th entity `VersionRecord` (JADX is named `t0.u`), and `t0.e` = `BlackGrayAppRecord` | **supplements**: There are indeed 9 files in the `pojo` directory, but the `version` table entity is in the `t0` package |
| C4 | Not mentioned in the briefing | New discovery: `EngineDatabase` All BLOB are listed as **AES-CBC Random IV ciphertext**, the key can be completely restored from `META-INF/flamingo.dat` | **Added** (R1–R4 in this report) |
| C5 | Not mentioned in the briefing | New discovery: **two sets of signature salts + two sets of device identification + two-layer network switch** | **adds** (§4.6) |
| C6 | Task description: "`i0.h.h(ctx)` judgment conditions and incremental synchronization protocol behind `guard/support_incremental`" | `i0.h.h(ctx)` reads `miui.support_full_scan` (**full scan capability bit**), not incremental judgment; `support_incremental` returns **null** when the capability is insufficient | **corrects**: see §4.7; the real incremental synchronization is `dataVersion` version comparison (new stack) and `updateType=incremental` (old stack) |

