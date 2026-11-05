> **归档警示（已纠错）**：这是原分析材料的脱敏归档，不能代替 [最终复核报告](../REPORT.md)。原文关于“明文 IMEI 列表”“默认配置即实际上传”、绝对覆盖率及未知 native 行为的部分结论不成立。IMEI/IMSI 的相关 DAU 列表经过 MD5，仍受区域/权限/联网门控。证书过期也不能单独推出密文无法解密。请先阅读最终报告与其中纠错表。本文删除本地身份路径，原有文件行号引用可能指未纳入发布的完整分析树。

# 20 云端扫描协议 + 本地库 + 网络层

> 分析员：t-cloud（GuardProvider 逆向分析团队）
> 目标：`GuardProvider-2.0.3-20231124.0.apk`，SHA-256 `b0c02fa682b8ca82429b471113d0702042ac76234c85fdcd8efeb7c03d313024`
> 主语料：`_analysis\src\sources\`（3231 个 JADX 文件）
> 陈述分级：**观测**（源码/字节直接可见）｜**推断**（有依据的语义归纳）｜**未知**（未覆盖）

---

## 1. 职责概述

本子系统是 GuardProvider 的「云端 + 本地威胁情报」闭环，由四块拼成：

1. **本地情报库（Room/SQLite）** — `EngineDatabase`，8 张表，承载黑名单（guilty_package）、白名单（innocent_package）、特征码规则（characteristics / black_gray_chs）、灰产应用（black_gray_app）、可信证书/安装器（trust_certs / trust_installers）以及 7 个数据集版本号（version 表）。**所有 BLOB 列实为 AES-CBC 密文**（见 §4.3），库文件本身不加密。
2. **云端协议报文** — `com.miui.guardprovider.sdk.android.pojo.networkdomain` 共 21 个类，Gson 序列化的 Kotlin data class，构成「同步（blocklist/allowlist/featureCodeList/blackGrays）+ 云端扫描（/virus/scan）+ 结果上报（/virus/localResult/report）」三组报文。
3. **网络层（两套并存）**
   - **旧栈**：`h0.b`（`NetworkApiHelper`）— `java.net.HttpURLConnection`，POST，MD5(排序参数 + 静态盐) 签名。服务 `api.sec.miui.com`（病毒库/白名单全量同步）、`flash.sec.miui.com`（非系统应用清单上报）。
   - **新栈**：Retrofit2 + OkHttp3（`g5`=okhttp3、`d6`=retrofit2、`f6`=retrofit2.http），基址 `https://virus-engine.sec.miui.com`（国际版 `.intl.`）。四个 OkHttp Interceptor 负责签名、请求体 AES 加密 + gzip、响应解密解压、网络总开关。
4. **JCE 序列化框架** — `com.qq.taf.jce`，**仅被内置腾讯 tmsdk 使用**（117 个 `JceStruct` 子类全部位于 `tmsdkobf` / `tmsdk.*`），**不参与 GuardProvider 自有云端协议**（自有协议用 Gson）。这是与简报预期不同的一点，详见 §4.5。

---

## 2. 组件与文件清单

### 2.1 数据库层

| 文件 | 类 | 作用 |
|---|---|---|
| `com\miui\guardprovider\sdk\android\db\EngineDatabase.java` | `EngineDatabase`（抽象，继承 `e.y`=RoomDatabase） | 暴露 7 个 DAO 访问器 `F()/G()/H()/I()/J()/K()/L()` :16-28 |
| `com\miui\guardprovider\sdk\android\db\EngineDatabase_Impl.java` | `EngineDatabase_Impl` | 生成实现：`onCreate` DDL :63-79、`onOpen` 校验 `g()` :129-242、版本/迁移策略 :351-382 |
| `r0\c.java` / `r0\d.java` | DAO 接口 / 实现 | `black_gray_app`（`t0.e` 实体） |
| `r0\e.java` / `r0\f.java` | DAO 接口 / 实现 | `black_gray_chs`（`BlackGrayTraitRecord`） |
| `r0\k.java` / `r0\l.java` | DAO 接口 / 实现 | `characteristics`（`CharacteristicRecord`） |
| `r0\z.java` / `r0\a0.java` | DAO 接口 / 实现 | `guilty_package`（`GuiltyPackageRecord`） |
| `r0\f0.java` / `r0\g0.java` | DAO 接口 / 实现 | `innocent_package`（`InnocentPackageRecord`） |
| `r0\k0.java` / `r0\l0.java` | DAO 接口 / 实现 | `trust_certs`（`TrustCertRecord`） |
| `r0\o0.java` / `r0\p0.java` | DAO 接口 / 实现 | `version`（`t0.u` = `VersionRecord`） |
| `r0\t.java` | `DatabaseCryptoServiceImpl`（实现 `r0.s`） | 列级加解密：Java 序列化 + AES |
| `r0\o.java` | `CommonFiledAdapter` | String/int 与密文互转 |
| `r0\q.java` | `ConditionAdapter` | `Condition` 与密文互转 + Gson TypeAdapter |
| `r0\a.java` | `DelegatingVirusDao`（实现 `l0.b`） | 按位掩码聚合 virusDao(bit1)/blackGrayDao(bit2) 的 `b()` :39-57 |

> 注：`trust_installers` 表在 DDL 中创建且列入 `g()` 校验，但**没有对应 DAO 接口**（`q()` 的 8 项里无 `TrustInstallerRecord` DAO）。观测：`EngineDatabase_Impl.q()` :370-382。

### 2.2 实体（pojo）层

| 文件 | 类 | 对应表 |
|---|---|---|
| `pojo\GuiltyPackageRecord.java` | `GuiltyPackageRecord`(id, packageName, signaturesMd5, signaturesSha256, versionCode, threatName, mfSha256) | `guilty_package` |
| `pojo\InnocentPackageRecord.java` | `InnocentPackageRecord`(id, packageName, signaturesMd5, signaturesSha256) | `innocent_package` |
| `pojo\CharacteristicRecord.java` | `CharacteristicRecord`(id, name, strings, condition, threatName, version) | `characteristics` |
| `pojo\BlackGrayTraitRecord.java` | `BlackGrayTraitRecord`(id, name, condition, strings, version) | `black_gray_chs` |
| `pojo\TrustCertRecord.java` | `TrustCertRecord`(id, developer, fingerprintSha256) | `trust_certs` |
| `pojo\TrustInstallerRecord.java` | `TrustInstallerRecord`(id, packageName, vendor) | `trust_installers`（无 DAO） |
| `pojo\Condition.java` | `Condition`(rawExp, exp) | 条件表达式（内嵌于上面两表 BLOB） |
| `pojo\StringItem.java` | `StringItem`(key, value, target:ScanTarget) | 特征字符串项（JSON 化后加密） |
| `pojo\ScanTarget.java` | `ScanTarget`（抽象）+ `AndroidManifest`/`Dex`/`Mf`/`So`/`SpecifiedFile` | 特征匹配目标文件 |
| `t0\u.java` | `VersionRecord`（JADX 混淆名；`toString` 自证 :96） | `version` |
| `t0\e.java` | `BlackGrayAppRecord`（JADX 混淆名） | `black_gray_app` |

### 2.3 报文（networkdomain）层

见 §5.3 完整字段表。文件均在 `com\miui\guardprovider\sdk\android\pojo\networkdomain\`。

### 2.4 网络层

| 文件 | 类 | 作用 |
|---|---|---|
| `h0\b.java` | `NetworkApiHelper` | 旧栈 HTTP 客户端 + 签名 + 设备参数 |
| `h0\a.java` | `support.mix.scan` 探测 | 读 `com.miui.securitycenter` metaData |
| `i0\d.java` | 病毒库/白名单同步器 | `api.sec.miui.com/antivirus/{viruses,whitelist}` |
| `i0\b.java` | `miui_antivirus.db` SQLiteOpenHelper | 旧版本地库（`antivirus_db`/`antivirus_db_version`） |
| `i0\e.java` | `mi_engine_white_virus.db` | `mi_virus_white_db` / `mi_detect_app_db` |
| `i0\f.java` | 白名单库访问器 | `whitelist` 响应落库 + 查询 |
| `i0\l.java` | `MiEngine` 本地查杀 | 解析 APK lite → 查 `antivirus_db` → 签名/整包 MD5 比对 |
| `j0\a.java` / `j0\b.java` / `j0\c.java` | `AntiDefraudAppManager` | `flash.sec.miui.com/detect/app` 上报 + `mi_detect_app_db` 缓存 |
| `q0\*` | Retrofit 接口 + 拦截器 | 见 §5.2 |
| `s0\s.java` | 依赖装配（Gson/OkHttpClient/Retrofit） | :76-127 |
| `s0\d.java` | DAO/适配器工厂 | :140-199 |
| `v0\j.java` | `CryptoService` | AES 密钥派生 + 加解密 |
| `v0\k.java` | `GzipService` | gzip 压缩/解压 |
| `v\a.java` | `IdentifierManager` | 反射 `com.android.id.impl.IdProviderImpl` 取 OAID/UDID/VAID/AAID |
| `w0\b.java` | uuid 提供者 | OAID 优先，回落随机 UUID |
| `r0\w.java` | 同步器基接口 | 统一 query 参数（uuid/versionType/timestamp） |
| `r0\g.java`/`r0\h`… | 各数据集同步器 | 下载 + 版本推进 |
| `p0\f.java` | `CloudScanner` | `/virus/scan` 云端扫描 |
| `p0\b0.java` | `RealScanner` | `/virus/localResult/report` 结果上报 |
| `p0\j.java` | Dagger 依赖图 | 全网关系装配 |


---

## 3. 数据流 / 时序

### 3.1 启动与同步（版本号增量拉取）

```
GuardApplication 启动
  └─ p0.j (Dagger) 装配：s0.s.j() 建 Retrofit（baseUrl=q0.m.f7703a）
        └─ OkHttpClient = [q0.q sign] + [q0.g i4Req] + [q0.i i4Resp] + [q0.k netGuard] + [s0.s.a log]
  └─ EngineDatabase（Room, version=2, destructive fallback）
  └─ 同步任务集合 = p0.j.b.a()  ← s0.k.c(...) ← s0.d.g(m,b0,d0,i,g)  [p0\j.java:233, :277-279]
```

每个数据集同步器（如 `r0.g` 黑灰产应用）的时序：

```
1. 组装 query：
     uuid        = p0.w.f7555a.a()          [r0\w.java:23]
     versionType = stable|development|alpha [r0\w.java:24-29]
     timestamp   = System.currentTimeMillis() [r0\w.java:30]
   ← super.c(dVar) 得到 Map
2. 追加数据集本地版本：map["dataVersion"] = q0.b(dVar)   [r0\g.java:294-309]
3. GET /blackGrays/blocklist?…&sign=…                    [q0\b.java:11]
4. 响应 BlackGrayAppResponse{code,desc,data{dataVersion, blocklist[]}}
5. 逐条映射 networkdomain → Room 实体（哈希统一 toLowerCase）
     u0.a.b(BlackGrayApp) → t0.e          [u0\a.java:16-28]
6. dao.o(newItems) / dao.j(deletedItems)                 [r0\g.java:153-165]
7. vm 写入新 dataVersion：q0Obj.i(newVersion)            [r0\g.java:166-171]
```

### 3.2 云端扫描（`/virus/scan`）端到端

```
调用方（SecurityService → p0.n MiEngineNG）
  └─ 扫描器链：p0.b0 RealScanner { codeScanner(g), cloudScanner(f), matcherScanner(z), uploadApi(q0.n) }
        ├─ 本地：p0.g CodeScanner / p0.z MatcherScanner / p0.y 规则编译
        └─ 云端：p0.f CloudScanner                          [p0\b0.java:202-211]

p0.f CloudScanner（l0.e 管线阶段）:
  a(dVar) :372-379  读取 scan_params(int) → f7380c
                    f7381d = (scan_params & 8) == 0     ← bit3 关闭云扫
                    f7382e.c(scan_params): bit0|1 → 威胁模式, bit2 → 黑灰模式
  c(cVar) :383-385  协程执行 b.k()
  b.k()   :256-345
     if (!f7381d) throw IOException("cloud scan is denied!!!")     :258-261
     待扫列表 = v4.i.c(cVar)                                        :262-271
     CloudScanApkInfo[] = 逐条 k(t0.a)                              :273-277
        packageName / versionCode(String) / f() / b() / g()         :358-368
     公共参数：androidVersion, miuiVersion, timestamp(ms),
               uuid=w.f7555a.a(), versionType, scanType=f7380c       :279-286
     retrofit q0.d.a(List<CloudScanApkInfo>, @QueryMap Map)          :291
     → POST {base}/virus/scan  (body 经 q0.g 加密+gzip)
     校验：index 一一对应且 packageName 必须不等（防错位）          :212-216, :315-318
     消费：f.a.b(app, payload) 合并结论                              :241
  a.b()   :80-104   Threat 模式 + 黑灰模式 叠加，Threats 优先，
                    Threats→t0.o（infected），BlackGrays→t0.f（gray）
  a.a()   :44-52    云端威胁名 → t0.m.c(name) → t0.k(level,name,desc,null,-1,"cloud")
                    authority 固定 "cloud"，versionCode 固定 -1

结果上报（并入同一 RealScanner 管线）p0.b0.h() :215-297
   infected  → 每个 t0.k 一条 ResultReportData
   gray      → threatLevel 硬编码 "BlackGrays"
   POST {base}/virus/localResult/report?timestamp=<ms>  [q0\n.java:12-13]
   异常：d6.j / IOException 仅 Log.e("RealScanner","Result upload failed")，不重试 :291-295
```

### 3.3 `flash.sec.miui.com/detect/app` 端到端（与简报预期不同的真实链路）

```
j0.c.h(ctx) / j0.c.a()                    [j0\c.java:155-165, :25-55]
  └─ g(ctx) 枚举 getInstalledPackages(0)，跳过 flags&1（系统应用）  :133-153
       每项 {pkg, version, signature=MD5(cert[0] 原始字节), appname=URLEncode(label)}
  └─ b(list)                                                       :57-80
       body = {
         timestamp : 秒级时间戳,
         os        : "MIUI-"+Build.VERSION.INCREMENTAL,
         biz_id    : "virus_scan",
         uuid      : w0.b.b()          ← 随机持久 UUID，不是 OAID
         content[] : {pkg, version, signature, appname}
       }
       POST https://flash.sec.miui.com/detect/app
            appKey = "6988567a-4220-4b51-bc2d-ccdec27a74a1"        :75
            Content-Type: application/json；body 追加 "sign"
  └─ 响应：{data:[{pkg, signature, version, basisOf}], error_code?}
       仅保留 basisOf == 2 的项（= 判定为不安全/不予信任）           :41-44, j0\a.java:51-58
  └─ 落库：j0.a.b(json) → 全表 delete 后重插 mi_detect_app_db(package,signatures)
       [j0\a.java:37-70]
  └─ 查询：j0.a.c(pkg) → SELECT … WHERE package=? AND signatures=MD5(cert[0])
       [j0\a.java:72-91]
```

**结论（推断）**：这条链不是"逐样本云端扫描"，而是**「非系统应用清单批量上报 ←→ 反诈/不予信任应用名单整体下发」**的粗粒度同步。`biz_id=virus_scan` 只是业务标记；接口不接受、也不返回任何"该 APK 是否病毒"的单样本结论，且落库表无 TTL、无版本号（每次全量替换）。真正的单样本云端判定走 `https://virus-engine.sec.miui.com/virus/scan`。

---

## 4. 关键逻辑逐条解析

### 4.1 数据库 DDL（完整）

**观测**：`EngineDatabase_Impl$a.a(h.i)` :63-79，REA 字节码级复核一致（ev_3415eba8…）。
**观测**：数据库版本 = 2（`new a0(iVar, new a(2), "8856aeeb52eb3fc27eb4570c1cd903b7", "4033f04c51ab315bb8679d5c0f44fb16")` :357）。

| # | 表名 | 列定义 | 主键/索引 |
|---|---|---|---|
| 1 | `trust_certs` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`developer` BLOB NOT NULL<br>`fingerprintSha256` BLOB NOT NULL | UNIQUE INDEX `index_trust_certs_fingerprintSha256`(`fingerprintSha256` ASC) |
| 2 | `trust_installers` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`packageName` BLOB NOT NULL<br>`vendor` BLOB NOT NULL | UNIQUE INDEX `pk_index`(`packageName` ASC) |
| 3 | `innocent_package` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`packageName` BLOB NOT NULL<br>`signaturesSha256` BLOB **NULL**<br>`signaturesMd5` BLOB NOT NULL | UNIQUE INDEX `index_innocent_package_packageName_signaturesMd5`(`packageName`,`signaturesMd5`) |
| 4 | `characteristics` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`name` BLOB NOT NULL<br>`strings` BLOB NOT NULL<br>`condition` BLOB NOT NULL<br>`threatName` BLOB NOT NULL<br>`version` INTEGER NOT NULL | UNIQUE INDEX `index_characteristics_name`(`name` ASC) |
| 5 | `guilty_package` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`packageName` BLOB NOT NULL<br>`signaturesMd5` BLOB NOT NULL<br>`signaturesSha256` BLOB **NULL**<br>`versionCode` BLOB **NULL**<br>`threatName` BLOB NOT NULL<br>`mfSha256` BLOB **NULL** | UNIQUE INDEX `index_guilty_package_packageName_signaturesMd5_mfSha256`(`packageName`,`signaturesMd5`,`mfSha256`) |
| 6 | `black_gray_chs` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`name` BLOB NOT NULL<br>`condition` BLOB NOT NULL<br>`strings` BLOB NOT NULL<br>`version` INTEGER NOT NULL | UNIQUE INDEX `index_black_gray_chs_name`(`name` ASC) |
| 7 | `black_gray_app` | `id` INTEGER PK AUTOINCREMENT NOT NULL<br>`packageName` BLOB NOT NULL<br>`signaturesMd5` BLOB NOT NULL<br>`signaturesSha256` BLOB NOT NULL<br>`versionCode` BLOB **NULL**<br>`mfSha256` BLOB NOT NULL | UNIQUE INDEX `index_black_gray_app_packageName_signaturesMd5_mfSha256`(`packageName`,`signaturesMd5`,`mfSha256`) |
| 8 | `version` | `characteristicVersion` INTEGER NOT NULL<br>`guiltyPackageVersion` INTEGER NOT NULL<br>`innocentPackageVersion` INTEGER NOT NULL<br>`trustCertVersion` INTEGER NOT NULL<br>`trustInstallerVersion` INTEGER NOT NULL<br>`blackGrayChsVersion` INTEGER NOT NULL<br>`blackGrayAppsVersion` INTEGER NOT NULL<br>`id` INTEGER NOT NULL | PRIMARY KEY(`id`)，**无 autoincrement** |
| — | `room_master_table` | `id` INTEGER PRIMARY KEY<br>`identity_hash` TEXT | Room 内部；写入 `42 / '8856aeeb52eb3fc27eb4570c1cd903b7'` |

**迁移**：
- **观测**：`j(Map)` 返回 `Arrays.asList(new f.a[0])`（空迁移数组）:361-363；`p()` 返回 `new HashSet()`（无 autoMigration 规格）:366-368。
- **观测**：`q()` 的 8 项映射中含 `o.class`（`o` = 未知 DAO 占位）与 `m0.class`（`n0.v()`，无实体表）:370-382。
- **推断**：采用 Room 破坏性迁移（`fallbackToDestructiveMigration`），版本升级即清库重建；这也解释了 DDL 全部使用 `CREATE TABLE IF NOT EXISTS` 且 `isDestructiveMigration` 路径存在。
- **未知**：`e.y`（RoomDatabase 子类）的 builder 装配点未定位到（应在 `s0\r.java` 之上的一层，本次未展开）。

**DAO SQL 全量**（`r0` 包，逐条）：

| DAO 实现 | SQL | 位置 |
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
| `r0\g0` | `select * from innocent_package where packageName = ? and signaturesMd5 = ?`（出现两处：读单条 + 存在性判断） | g0.java:296 / 341 |
| `r0\g0` | `select * from innocent_package` | g0.java:312 |
| `r0\g0` | `INSERT OR REPLACE … innocent_package (id,packageName,signaturesSha256,signaturesMd5)` | g0.java:76 |
| `r0\l0` | `select * from trust_certs` / `… where fingerprintSha256 = ?` | l0.java:269 / 299 / 310 |
| `r0\l0` | `INSERT OR REPLACE … trust_certs (id,developer,fingerprintSha256)` | l0.java:73 |
| `r0\p0` | `SELECT * FROM version` / `… WHERE id = ?` | p0.java:171 / 209 |
| `r0\p0` | `INSERT OR IGNORE INTO version (…) VALUES (?,?,?,?,?,?,?,?)` | p0.java:31 |

- **观测**：所有 `INSERT OR REPLACE` 首绑定 `mVar.F(1, record.getId())`，列值写成 `nullif(?, 0)`；即 **id=0 时插入新行（自增），id>0 时覆盖**。
- **观测**：写入前所有 String 字段都过 `r0.o.d(String)`（加密），读取时过 `r0.o.b(byte[])`（解密）。
- **观测**：`innocent_package` 的 insert 列顺序是 `(signaturesSha256, signaturesMd5)`（与表定义顺序一致），而实体构造器参数顺序是 `(packageName, signaturesSha256, signaturesMd5)`——两处顺序不同但绑定按列名，无错位。

### 4.2 库内容加密（列级 AES）

**观测** `r0\t.java`（`DatabaseCryptoServiceImpl`）：

| 方法 | 行为 |
|---|---|
| `a(Object)` :17-47 | `ObjectOutputStream.writeObject(obj)` → `v0.j.b(bytes)` AES 加密 |
| `b(int)` :50-55 | `ByteBuffer.allocate(4).putInt(i)` → AES 加密 |
| `c(byte[], Class)` :58-81 | AES 解密 → 按 `Class` 走 ByteBuffer（Boolean/Byte/Char/Short/Int/Long/Float/Double）或 `ObjectInputStream.readObject()` |

**观测** `v0\j.java`（`CryptoService`，REA 复核 ev_73f513b6…）：

| 项 | 值 |
|---|---|
| 算法 | `AES/CBC/PKCS5Padding` :59 / :79 |
| 密钥来源 | classpath `META-INF/flamingo.dat`，`skip(144)` 后读 16 字节 :38-41 |
| 密钥变换 | 前 8 字节与后 8 字节各作 **大端 long**，分别 `^ 4831221107097930563L`（= `0x430BF1FFFFF10B43`）:43-44 |
| **还原出的 AES-128 密钥** | ASCII `wcw1xbplx7aijwqr`（hex `77 63 77 31 78 62 70 6c 78 37 61 69 6a 77 71 72`） |
| IV | `new SecureRandom().generateSeed(16)`，**随机且前置**：输出 = `IV(16) ‖ ciphertext` :58 / :66 |
| 解密 | 取 `bytes[0..16)` 作 IV，`bytes[16..]` 为密文 :81-84 |
| `EncryptVersion` 头取值 | `d()` 恒返回 `"0"` :99-101 |

**交叉验证**：`META-INF/flamingo.dat` 在 APK 中确实存在，304 字节（`_analysis\apktool\unknown\META-INF\flamingo.dat`）；偏移 144..159 的原始 16 字节为 `34 68 86 ce 87 93 7b 2f 3b 3c 90 96 95 86 7a 31`，异或后全为可打印 ASCII —— 该密钥自证成功。

**推断（重要）**：因为 IV 每次随机，**同一明文每次加密结果都不同**。于是：
1. `guilty_package` / `innocent_package` / `black_gray_app` 上的三列 UNIQUE 索引**无法起到去重作用**（同一 APK 每轮同步都会插入新行）；
2. DAO 中 `WHERE packageName = ? AND signaturesMd5 = ?` 这类等值查询，用"当前重新加密的参数"去匹配"历史密文"，**几乎不可能命中**。
3. 因此实际匹配路径应是 `l0.b.b()` 全表加载 → 内存解密 → 内存比对（`r0.a.b()` :48-57 聚合两个 DAO 的全表列表，与此一致）。

- **未知**：`r0.a`（`DelegatingVirusDao`）的调用方与内存匹配器（`p0.z` MatcherScanner / `p0.y` CodeScanner）的连接未逐行追完，故"等值查询是否为死代码"未做运行期确认。

### 4.3 条件表达式引擎（`Condition`）

- **观测**：`Condition(String rawExp)` 构造时 `engine.a(str)` 解析，失败抛 `IllegalArgumentException("Bad input expression:" + rawExp)` :20-28。引擎由 `new v5.b().g()` 静态构建 :16。
- **观测**：`reduce(Map<String,Boolean> matches)` 用 `n(matches)` 作变量上下文求值 :50-59；`reduce(v1.l supplier)` 用惰性 supplier + 缓存 :115-120、:92-101。
- **观测**：`equals`/`hashCode` 只基于 `rawExp` 字符串 :30-48。
- **推断**：特征库规则形态为**布尔表达式**（变量名 = `StringItem.key`），由客户端本地对每个文件计算 `key→bool` 后求值。`v0\l.b(s)` 会校验「表达式里出现的变量集合 == strings 的 key 集合」，否则抛 `"<name> isn't computable."` :55-58。
- **观测**：`v0\l.a(String)` 用分隔符 `" ()!\t\n\r\f&|"` 切分 token :28 —— 说明表达式的运算符集合至少含 `!`、`&`、`|`、括号。
- **未知**：`v5` 包具体是哪个表达式库（自研 or 移植）；本次未展开 `v5\b.java`(3.3KB) / `v5\a.java`(38.9KB) 的语法定义。

### 4.4 `StringItem` 的两种匹配语义

**观测** `v0\l.d(s)` :61-79：
- 若 `t0.j.b(stringItem)` 为真 → 把 `t0.j.a(stringItem)` 按 `\s` 切分，要求每段都匹配正则 `([\da-fA-F][\da-fA-F])|(\?[\da-fA-F])|([\da-fA-F]\?)` —— 即**十六进制字节模式 + `?` 通配的二进制特征**（用于 dex/so 字节扫描）。
- 否则 → `new p4.k(stringItem.getValue())` 编译为普通正则 —— **文本正则特征**。

**推断**：`StringItem.value` 的语义由某个布尔开关（`t0.j.b`）分流，形成"二进制特征码 / 文本正则"双模特征库。

**观测** `StringItem` 的 JSON 形态（`toString` :91）：`{"key": …, "value": …, "scanFile": <ScanTarget pathPattern>}`，`target` 字段用 `@c("scanFile")` 重命名 :15-16。
**观测** `ScanTarget` 的四个固定模式常量：`/AndroidManifest.xml`、`/*.dex`、`/META-INF/MANIFEST.MF`、`/lib/**/*.so` :14-17，另有 `SpecifiedFile` 兜底（`q0\o.java:32`）。

### 4.5 JCE 序列化框架（`com.qq.taf.jce`）

**编码规则（观测 `JceOutputStream` / `JceStruct`）**

字节序：**大端**（全部走 `ByteBuffer` 默认序；`putShort/putInt/putLong` 直接写）。

Tag/类型头编码（`writeHead(byte type, int tag)` :82-92）：

```
tag < 15 :  单字节 = type | (tag << 4)
tag ≥ 15 :  两字节 = (type | 0xF0), tag      // tag 上限 255，超出抛 c("tag is too large")
```

类型码表（`JceStruct` :7-21）：

| 码 | 常量 | 含义 | 长度/写法 |
|---|---|---|---|
| 0 | `BYTE` | byte | 1 字节；**值为 0 时改用 `ZERO_TAG` 省字节**（:110-115） |
| 1 | `SHORT` | short | 2 字节；`-128..127` 时降级为 BYTE |
| 2 | `INT` | int | 4 字节；`-32768..32767` 时降级为 SHORT |
| 3 | `LONG` | long | 8 字节；`int32` 范围内降级为 INT |
| 4 | `FLOAT` | float | 4 字节 |
| 5 | `DOUBLE` | double | 8 字节 |
| 6 | `STRING1` | 短字符串 | 1 字节长度 + bytes（长度 ≤ 255） |
| 7 | `STRING4` | 长字符串 | 4 字节长度 + bytes（长度 > 255） |
| 8 | `MAP` | Map | 4 字节 size + 逐项 `(key@tag0, value@tag1)`（:188-198） |
| 9 | `LIST` | 数组/List | 4 字节 size + 逐项 `@tag0`（:25-32, :266-276） |
| 10 | `STRUCT_BEGIN` | 嵌套结构开始 | 后接 `writeTo` 内容 |
| 11 | `STRUCT_END` | 结构结束 | `write(JceStruct,tag)` 自动补写 `writeHead(11,0)`（:278-284） |
| 12 | `ZERO_TAG` | 数值 0 的紧凑表示 | 仅 1 字节头，无负载 |
| 13 | `SIMPLE_LIST` | `byte[]` 专用 | 头 + `writeHead(0,0)` + 4 字节长度 + 原始字节（:209-215） |

默认字符串编码 `"GBK"`（:21, :119），可 `setServerEncoding` 改。

**本项目实际使用的 JCE 结构体**

- **观测**：全树 `grep "extends JceStruct"` 共 **117 处**：`tmsdkobf` 113 个、`tmsdk.*` 4 个（`tmsdk\common\module\qscanner\impl\{a,d,g,h}`）。
- **观测**：全树 `grep "com.qq.taf.jce.(JceInputStream|JceOutputStream|JceStruct|JceDisplayer)"` 的引用方，除 `tmsdkobf`/`tmsdk.*` 外只有两处，且都是**仅借常量**：
  - `g5\y.java:182` → `new byte[]{JceStruct.SIMPLE_LIST, 10}`（okhttp 内部某常量数组）
  - `o5\k.java:22` → 一个含 `JceStruct.SIMPLE_LIST/ZERO_TAG/STRUCT_END` 的字节模式数组（无线检测模块）
- **观测**：`com.miui.guardprovider` 自有包内**零**引用 JCE；自有报文（`networkdomain`）全部为 Gson（`l.*` = gson、`m.c` = `SerializedName`）。
- **结论（观测+推断）**：JCE 是 **腾讯 tmsdk（TencentEngine / AVL 引擎 SDK）自带的序列化层**，随 SDK 打包进来；GuardProvider 的云端协议不使用它。因此本任务的"JCE 报文"解读应针对 tmsdk 私有协议，而非 GuardProvider 云端协议。

### 4.6 网络层定位

#### 4.6.1 两套客户端

| 栈 | 客户端 | 特征 | 使用方 |
|---|---|---|---|
| 旧 | `java.net.HttpURLConnection`（`h0\b.java`） | 手写 POST，`addRequestProperty`，4096 缓冲循环读 | `i0\d`（api.sec）、`j0\c`（flash.sec） |
| 新 | OkHttp3 + Retrofit2（`g5`/`d6`/`f6`） | Kotlin、协程挂起函数、Interceptor 链 | `q0\*` / `s0\*` / `p0\f` / `p0\b0` |

**未使用**：无自研 socket 客户端、无 Volley、无 WebView 网络。

#### 4.6.2 旧栈请求格式与签名（`h0\b`）

**方法 / 头（观测）**

| 路径 | 函数 | Method | Content-Type | Body |
|---|---|---|---|---|
| 表单式 | `c(ctx,url,Map,appKey)` :80-147 | POST | `application/x-www-form-urlencoded; charset=UTF-8` :73 | `k=v&k=v…`（`URLEncoder.encode(...,"UTF-8")`，**按 HashMap 迭代序，不排序**）:219-236 |
| JSON 式 | `d(ctx,url,JSONObject,appKey)` :149-217 | POST | `application/json` :62 | `JSONObject.toString()`，**签名先写入 `"sign"` 键** :160-161 |

**超时**：`setConnectTimeout/ setReadTimeout` 均取 `com.xiaomi.onetrack.g.b.f2462b`（常量，同一值）:333-334；`setUseCaches(false)`。

**签名算法（观测 + REA 字节码复核 ev_96277430…）**

```
i(map, secret):
  keys   = sort(map.keySet())              // 字典序，Comparator = String.compareTo  :272-289
  s      = join("k=v", "&")                // 值取自 map（原始串，未 URL 编码）
  s     += "&" + secret
  b64    = Base64.encodeToString(s.getBytes("UTF-8"), NO_WRAP=2)
  return MD5hex_upper(b64)                 // j() :291-299: String.format("%1$032X", …)
```

- **观测**：`l(JSONObject)` 收集签名材料时**排除 `sign` 键**，并特判 `content`（JSONArray 取其 `.toString()`）:308-322。
- **观测**：`m(ctx,map,secret)` = `f(ctx,secret)` + 调用方 map + `sign` :324-329。
- **观测**：用于 `d()` 时，签名材料的 map 由 `l(jSONObject)` 生成，而 `put("sign", …)` 发生在 `d()` 内、`a()` 之前 :160-161 —— 即 `sign` 不参与自身计算，且**以明文 JSON 键随 body 一起发出**。

**两个静态 "appKey/secret"（观测）**

| 常量 | 用途 | 出处 |
|---|---|---|
| `94e6cec2-19dc-4bb6-823c-613f72d3ea16` | `api.sec.miui.com/antivirus/{whitelist,viruses}` | `i0\d.java:55`, `i0\d.java:203` |
| `6988567a-4220-4b51-bc2d-ccdec27a74a1` | `flash.sec.miui.com/detect/app` | `j0\c.java:75` |

#### 4.6.3 旧栈设备标识与鉴权

**观测** `h0\b.f(ctx, secret)` :238-248 固定 7 个参数：

| 参数 | 取值 | 出处 |
|---|---|---|
| `device` | `SystemProperties("ro.product.device","unknown")` | :34, :240 |
| `carrier` | `SystemProperties("ro.carrier.name","unknown")` | :37, :241 |
| `region` | `SystemProperties("ro.product.mod_device","unknown")` | :40, :242 |
| `miuiVersion` | `"MIUI-" + Build.VERSION.INCREMENTAL` | :43, :243 |
| `versionType` | `stable` / `development` / `alpha` | :301-306, :244 |
| `appVersion` | 常量 `"1"` | :46, :245 |
| `mi` | `MD5hex( android_id + "-" + secret )` | :258-270, :246 |
| `sign` | 见 §4.6.2 | :327 |

- **观测**：设备标识来源是 `Settings.Secure.getString(cr, "android_id")` :261；为空则返回 `""` :266-268。**未使用 IMEI / 序列号 / OAID**。
- **观测**：`android_id` 与 secret 拼接后再 MD5，用于弱化跨服务关联，但仍是稳定设备指纹。
- **推断**（未覆盖 `e5.a.b`/`f5.a.a` 实现）：`mi` 的十六进制大小写未知。

#### 4.6.4 新栈（Retrofit/OkHttp）配置

**观测** `s0\s.java`：

| 装配 | 位置 | 内容 |
|---|---|---|
| Gson | :76-90 | `registerTypeAdapter(ScanTarget, q0.o)`、`(Condition, r0.q)`、`(CloudScanPayload.a, CloudScanPayload.b)`、`(u0.g, u0.i)` |
| OkHttpClient | :102-114 | `addNetworkInterceptor(q0.k)`；`addInterceptor(q0.q)`；`addInterceptor(q0.g)`；`addInterceptor(q0.i)`；`addInterceptor(s0.s.a)`（日志，TAG=`MiEngineNG`） |
| Retrofit | :121-127 | `baseUrl(q0.m.f7703a)` + `client(zVar)` + `GsonConverterFactory`（`e6.a.f(gson)`） |

**基址（观测）** `q0\m.java:12`：
```java
f7703a = Build.IS_INTERNATIONAL_BUILD
       ? "https://virus-engine.sec.intl.miui.com"
       : "https://virus-engine.sec.miui.com";
```

**Interceptor 逐一（观测）**

| 拦截器 | 位置 | 行为 |
|---|---|---|
| `q0.q`（signGenInterceptor，application） | q0\q.java:16-48 | 取所有 query 参数（**若调用方自带 `sign` 直接抛 `IllegalArgumentException`** :39-41）→ `TreeMap` 排序 → `"k1=v1&k2=v2"` → 追加 `"&62b36629-ecb4-4725-9f33-ccb0fd158191"` → UTF-8 → `MD5hex_upper`（`l0.f.b(bytes,"MD5",true)`，true→`'X'` 大写，见 `l0\f.java:97-104`、`:70`）→ 作为 query 参数 `sign` 追加 :47 |
| `q0.g`（i4Req，application） | q0\q.java… 实为 q0\g.java:25-38 | ① 加请求头 `EncryptVersion: v0.j.d()`（=`"0"`）；② 若有 body：取字节 → `gzipService.b()` 压缩 → `cryptoService.b()` AES 加密 → 以 `c0.g(原始ContentType, 密文, 0,0)` 换体 |
| `q0.i`（i4Resp，application） | q0\i.java:24-43 | ① 校验响应头 `EncryptVersion` 必须等于本地 `d()`；**不等于且 HTTP≠200 → 直接返回**；**不等于且 HTTP=200 → 抛 `IOException("Header EncryptVersion not found.")`** :32-37；② 解密 `cryptoService.c(body)` → `gzipService.a()` 解压 → 换体 |
| `q0.k`（netGuard，**network** interceptor） | q0\k.java:13-20 | `if (!p0.n.f7475f.a().get()) throw new IOException("Network access disabled.")` —— 全局网络总开关（`AtomicBoolean(false)` 默认关，`p0\n.java:32`，由 `p0.n.m(boolean)` 置位 :828-830） |
| `s0.s.a`（日志） | s0\s.java:35-53 | 再塞一次 `EncryptVersion` 头；打印原始/密文/明文 body，TAG=`MiEngineNG` |

**签名盐（第二个，与旧栈不同）**：`62b36629-ecb4-4725-9f33-ccb0fd158191`（`q0\q.java:22`，REA 复核 ev_94cce8a5…）。

#### 4.6.5 新栈设备标识与鉴权 token

| 参数 | 取值 | 出处 |
|---|---|---|
| `uuid` | `p0.w.f7555a.a()`（进程内 volatile 缓存，未初始化则抛 `IllegalStateException("Not init yet!")`） | `r0\w.java:23`、`p0\w.java:15-21`、`p0\f.java:283` |
| `versionType` | `stable` / `development` / `alpha` | `r0\w.java:24-29`、`p0\f.java:284` |
| `timestamp` | `System.currentTimeMillis()`（**毫秒**） | `r0\w.java:30`、`p0\f.java:282` |
| `scanType` | `String.valueOf(scan_params)` | `p0\f.java:285` |
| `androidVersion` | `Build.VERSION.SDK_INT` | `p0\f.java:280` |
| `miuiVersion` | `Build.VERSION.INCREMENTAL`（**无 `MIUI-` 前缀**，与旧栈不同） | `p0\f.java:281` |
| 每个数据集版本 | `dataVersion` / `characteristicVersion` … | `r0\g.java:296-309` 等 |

- **观测**：`uuid` 无 `Authorization` / `Bearer` / `token` 头——**新栈是"签名即鉴权"**，无会话 token。
- **观测**：`uuid` 的真值来源由外部注入（`p0/w.b(String)`），注入点本次未定位。
- **观测**：旧栈另有一套标识（`oa` = OAID）——见 §4.6.6。

#### 4.6.6 OAID / 设备标识全景

| 标识 | 生成/来源 | 用途 | 出处 |
|---|---|---|---|
| 旧栈 `mi` | `MD5( ANDROID_ID + "-" + appSecret )` | 所有 `h0.b` 请求 | `h0\b.java:258-270` |
| `oa`（OAID） | 反射 `com.android.id.impl.IdProviderImpl.getOAID(Context)` | `api.sec.miui.com/antivirus/{viruses}` 请求体 | `v\a.java:30-44`、`i0\d.java:43`、`:50` |
| 随机持久 UUID | `UUID.randomUUID()` → SharedPreferences `key_uuid` | ① 国际版替代 OAID；② `flash.sec.miui.com` 的 `uuid` | `w0\b.java:17-26`、`i0\d.java:39`/`:48`、`j0\c.java:63` |
| `uuid`（新栈） | 外部注入的缓存串 | 所有 `virus-engine` 请求 | `p0\w.java` |

- **观测**：`v\a` 同时反射出 `getUDID/getOAID/getVAID/AAID` 四个方法 :33-36，但只有 `getOAID` 被暴露使用 :42-44。
- **推断**：`i0\d.e(boolean)` 中国际版用随机 UUID、国内版用 OAID，是为了规避海外 OAID 合规限制。
- **未知**：`p0.w.b()` 的实际调用方（谁把 OAID/ANDROID_ID/其他串注入进来）。

#### 4.6.7 TLS 校验

- **观测**：全树 `grep TrustManager|HostnameVerifier|CertificatePinner|sslSocketFactory|checkServerTrusted` 命中全部落在 **OkHttp3 库自身**（`g5\*`、`p5\*`、`q5\*`、`s5\*`）与 **AVL 引擎自有网络**（`com\avl\engine\i\f\*`，其 `SSLSocketFactory` 由外部传入）。
- **观测**：没有 `SSLContext.init(null, allTrusting, …)` 形态的全信任实现；`p5\c.java:109` 用 `TrustManagerFactory.getInstance("PKIX","BCJSSE")` + `init((KeyStore)null)` = **系统默认信任根**；`s5\d.java` 是 OkHttp 标准 `OkHostnameVerifier`。
- **观测**：**无 `CertificatePinner`**、无 `HostnameVerifier` 覆盖、无 `android:usesCleartextTraffic` 绕过（旧栈两条 URL 均为 https）。
- **结论（推断）**：TLS 为**系统默认校验，无证书 pinning，未关闭校验**。这也意味着**系统 CA 注入即可中间人**（如需抓包，MitM 代理 + 用户 CA 在 API 26+ 需应用未固定 `networkSecurityConfig`；本 APK 未发现自定义 network security config，待另一成员确认 `res\xml`）。

#### 4.6.8 错误码 / 重试 / 退避

| 层 | 行为 | 位置 |
|---|---|---|
| 旧栈 | **仅判 `getResponseCode() != 200` → return null**；异常一律 `Log.e("NetworkApiHelper", "exception when do post request", e)` 后 return null | `h0\b.java:92-96`, `:162-166`, `:143-146` |
| 旧栈业务错误 | 响应 JSON 含 `error_code` 键即视为失败，直接丢弃 | `i0\d.java:108-110`, `i0\f.java:49-51`, `j0\c.java:33-35`, `j0\a.java:42-44` |
| 新栈业务码 | `u0.e.getCode()`：**`code == 0` 视为失败**（`throw IOException("Response code:" + code + ", threatDesc:" + desc)`）；`data == null` 亦抛 | `p0\f.java:198-204`, `:301-307` |
| 新栈上报 | 捕获 `d6.j` / `IOException` 只打日志 `"Result upload failed"` | `p0\b0.java:291-295` |
| 新栈协议头 | 响应头 `EncryptVersion` 缺失且 HTTP=200 → `IOException("Header EncryptVersion not found.")` | `q0\i.java:36` |
| 网络开关 | Off → `IOException("Network access disabled.")` | `q0\k.java:19` |
| 云扫策略 | `scan_params & 8 != 0` → `IOException("cloud scan is denied!!!")` | `p0\f.java:258-261` |

- **观测**：**全树未发现任何 retry / 指数退避 / backoff 实现**（无 `retryOnConnectionFailure` 显式设置、无退避定时器、无重试计数）。
- **推断**：重试责任上移到调用方（`s0` 之外的调度层 / SecurityCenter），本 APK 内部是"单次调用，失败即放弃，下次由外部再次触发"。

#### 4.6.9 网络总闸

**观测** `GuardApplication.e(Context)` :38 起：

| 分支 | 条件 |
|---|---|
| 国内版（`!IS_INTERNATIONAL_BUILD`） | `SystemProperties("persist.sys.sc_allow_conn", false)` |
| 国际版 | 需满足 `com.miui.securitycenter` 已安装 **且** `persist.sys.sc_allow_conn` 为真 |
| 国际版且 `ro.miui.customized.region == "jp_kd"` | 再叠加 `Settings.Secure "antivirus_engine_network_enable" == 1` |

**推断**：GuardProvider 的**全部联网能力由 MIUI 安全中心的系统属性远程/本地开关控制**，默认关闭。这是比 `q0.k` 更外层的闸门（`q0.k` 是进程内 AtomicBoolean，`GuardApplication.e` 是系统属性）。

### 4.7 `guard/support_incremental` 与 `i0.h.h(ctx)`

> **与任务描述不符，需纠正**：`i0.h.h(ctx)` **不是**增量扫描判定，而是**「安全中心是否开启全量扫描」**。`guard/support_incremental` 是把该值**取反语义**地暴露出去。

**观测链**

| 位置 | 内容 |
|---|---|
| `GuardProvider.java:92` | `uriMatcher.addURI("guard", "support_incremental", 1009)` |
| `GuardProvider.java:405-411` | `case 1009:` `if (!i0.h.h(getContext())) return null;` 否则返回 `MatrixCursor(new String[]{"support"})` 并 `addRow(new Object[]{1})` |
| `i0\h.java:84-92` | `h(Context)`：若 `d5.a.f2965a` 为真 → false；否则取 `com.miui.securitycenter` 的 `applicationInfo.metaData.getBoolean("miui.support_full_scan")` |
| `SecurityService.java:145-151` | AIDL `e()`：`if (h.h(ctx)) return !d.g(ctx).f().isEmpty(); return false;` |

**语义（推断）**

- `i0.h.h(ctx) == true` ⇒ 安全中心声明**支持全量扫描**（`miui.support_full_scan=true`）。
- `guard/support_incremental` 在 `h.h(ctx)==true` 时返回 `{support:1}`，为 false 时**返回 null**（不是返回 0）。
- 因此该 URI 的对外语义实际是**"本机具备完整引擎扫描能力"**的一个能力位，命名 `support_incremental` 与实现语义相反/名不对实。
- `SecurityService.e()` 更精确：只有在支持全量扫描时，才返回"本地风险库非空"——即告诉调用方"有增量结论可读"。

**关键交叉验证**：`h0\a.java` 是另一条独立判定 —— `securitycenter.support.mix.scan`（`h0\a.java:9-19`，调用点 `x\a.java:142`、`x\b.java:150`、`:171`）。两者是不同 metaData 键，**不可混用**：
- `securitycenter.support.mix.scan` → 混合引擎（`guardprovider.support.mix.scan=true` 的对称项）
- `miui.support_full_scan` → 全量扫描能力位

**增量同步协议（真实存在的那一套）**

| 数据集 | 端点 | 版本参数 | 落库表 |
|---|---|---|---|
| 特征码规则 | `GET /virus/feature/codeList`（`q0\c.java:11`） | `dataVersion` | `characteristics` |
| 病毒黑名单 | `GET /virus/blocklist`（`q0\e.java:11`） | `dataVersion` | `guilty_package` |
| 白名单+开发者证书 | `GET /virus/allowlist`（`q0\f.java:11`） | `allowlistVersion` / `developerVersion` | `innocent_package` + `trust_certs` |
| 黑灰产特征 | `GET /blackGrays/feature/codeList`（`q0\a.java:11`） | `dataVersion` | `black_gray_chs` |
| 黑灰产应用 | `GET /blackGrays/blocklist`（`q0\b.java:11`） | `dataVersion` | `black_gray_app` |
| 旧病毒库 | `POST https://api.sec.miui.com/antivirus/viruses`（`i0\d.java:203`） | `version` + `updateType=incremental\|global` | `antivirus_db`（旧 SQLite） |
| 旧白名单 | `POST https://api.sec.miui.com/antivirus/whitelist`（`i0\d.java:55`） | 无版本（全量覆盖） | `mi_virus_white_db` |
| 反诈名单 | `POST https://flash.sec.miui.com/detect/app`（`j0\c.java:75`） | 无版本（全量覆盖） | `mi_detect_app_db` |

**旧栈 `updateType` 语义（观测 `i0\d.i(String)` :102-185）**

| 服务器字段 | 客户端动作 |
|---|---|
| `version` / `releaseTime` | 先 `delete antivirus_db_version` 再插入 |
| `updateType == "global"` | `delete antivirus_db`（**整表清空后重建**）:121-123 |
| `updateType == "incremental"` | 保留旧行，按条目 `status` 增量处理 :124-126 |
| 条目 `status == "new" \| "updated"` | `delete … WHERE package=? AND signatures=? AND md5=?` 后 `insert` :145-164 |
| 条目 `status == "deleted"` | 仅 `delete` :166-168 |
| 条目其它 `status` | 忽略 |
| `status` 字段缺失 | 当 `"new"` 处理（`optString(J,"new")`，`b.J == "status"`）:139 |

- **观测**：`i0\d.j(boolean incremental)` 每次先 `g(ctx)`（拉白名单），再处理病毒库 :202-210 —— 即白名单每次全量刷新，病毒库可增量。
- **观测**：`i0\d.e(boolean)` 组装 `{version, updateType, oa}` :36-45；`oa` 来自 `com.xiaomi.onetrack.b.m.f2268a == "oa"` :7。
- **观测**：`n0\g.java:643-644` 是旧栈同步的调用点：`i0.d.d().a(ctx); Log.i("AntivirusManager", "update whitelist: " + i0.d.d().j(true))` —— **固定以 `incremental=true` 触发**。

### 4.8 云端扫描触发条件（`scan_params` 位域）

**观测** `p0\f.java:370-379`：

```java
int v = dVar.a().b("scan_params");      // 从扫描参数上下文取
this.f7380c = v;                         // 传给服务器 scanType
this.f7381d = (v & 8) == 0;              // bit3=0 → 允许云扫
this.f7382e.c(v):
    f7383a = (v & 3) != 0;               // bit0|bit1 → 威胁（黑名单）模式
    f7384b = (v & 4) != 0;               // bit2     → 黑灰产模式
```

| bit | 掩码 | 含义 |
|---|---|---|
| 0,1 | `& 3` | 威胁扫描模式（Threats） |
| 2 | `& 4` | 黑灰产扫描模式（BlackGrays） |
| 3 | `& 8` | **禁用云端扫描**（置 1 → `cloud scan is denied!!!`） |
| 其余 | — | 原值整体作为 `scanType` 上报 |

- **观测**：构造器默认 `f7380c = 3` :353（= 威胁模式，云扫允许）。
- **观测**：`guardprovider.scan_properties=2`（Manifest meta-data，见简报）应与此位域对应，`2` = 仅 bit1 → 威胁模式、云扫允许、非黑灰模式。
- **推断**：`scan_params` 由调用方（`l0.d` 扫描上下文）注入，源头在 SecurityCenter 通过 AIDL 下发的扫描参数；**本地"无结论/可疑"并不构成升级条件**——是否云扫由位域决定，一旦允许则**所有待扫项整批**送 `/virus/scan`，无"仅上传可疑项"的过滤逻辑（`p0\f.java:271-291` 对 `list5` 全量 `map`）。

### 4.9 上传内容：只传指纹，不传 APK 本体

**观测** `p0\f.k(t0.a)` :358-368 → `CloudScanApkInfo(packageName, versionCode, f(), b(), g())`：

| 上传字段 | 来源（`t0.a` 访问器） | 语义（推断） |
|---|---|---|
| `packageName` | `getPackageName()` | 包名 |
| `versionCode` | `String.valueOf(aVar.d())` | 版本号（字符串化） |
| （第3个） | `aVar.f()` | apk 证书 MD5 |
| （第4个） | `aVar.b()` | apk 证书 SHA-256 |
| （第5个） | `aVar.g()`（null→`""`） | META-INF/MANIFEST.MF 的 SHA-256 |

**交叉验证**：字段名与顺序在 `CloudScanApkInfo` 构造器参数名中明确为 `(packageName, versionCode, apkCertificateMD5, apkCertificateSHA256, mfFileSHA256)`（`CloudScanApkInfo.java:16-26`），且 `p0\f.k()` 的取值顺序与之一一对应。

- **观测**：**不存在任何上传 APK 字节流 / dex / so 的代码路径**——没有 `RequestBody.create(File)`、没有 `MultipartBody`、`q0\d.a` 的唯一 `@Body` 参数是 `List<CloudScanApkInfo>`。
- **推断**：云端判定只能基于「包名 + 版本 + 证书指纹 + MF 指纹」四元组，属**指纹白/黑名单匹配**，不是代码级云查杀。这与 `CloudScanPayload` 的响应字段（无 codeFeature、无特征串）自洽。

### 4.10 响应消费与反哺本地

**观测** `p0\f.java`：

| 步骤 | 逻辑 | 位置 |
|---|---|---|
| 索引对齐校验 | 响应条数必须与请求一一对应，且 `payload.packageName != apkInfo.packageName`（**注意：这是"不等"断言**） | :212-216、:315-318 |
| Threats → 感染 | `resultType.contains(Threats)` → `t0.d.c(app, h1.p.e(a(payload)))` | :67-78 |
| BlackGrays → 灰产 | `resultType.contains(BlackGrays)` → `t0.d.b(app)` | :54-65 |
| Clean / Unknown → 不变 | 原样返回 | :60-62、:73-75 |
| 双模式同时开启 | 威胁优先于灰产；两者皆为"感染"时用 `t0.d.e(...,1,...)` 合并 | :94-103 |
| 未指定的模式 | `Log.w("CloudScanner","Scan mode is not specified yet.The original input will be returned.")` | :91 |
| 威胁信息构造 | `threatName` 必填（null 抛异常）；`t0.m.c(name)` 解析出 level/desc → `t0.k(level, name, desc, null, -1, "cloud")` | :44-52 |
| 结果 authority | 固定字符串 `"cloud"`；version 固定 `-1` | :51 |

- **观测**：`CloudScanPayload.resultType` 是 `EnumSet{a}`，枚举 `{Clean, Threats, BlackGrays, Unknown}`（`CloudScanPayload.java:23-32`），由自定义 Gson 适配器 `CloudScanPayload.b` 以 `name()` 字符串序列化（:34-50）。
- **观测**：云端结果**不写入 Room 库**。它只改变 `t0.a`（应用扫描对象）的状态，随后由 `p0\b0.h()` 上报服务器，并由 `p0.z`/`p0.y` 的本地规则再叠加。
- **推断**：`-1` 的 version 与 `"cloud"` 的 authority 使云端结论在 UI 上可区分且不可被本地特征库版本号覆盖。
- **未知**：`t0.d.b/c/e`（结论工厂）与 UI/DB 之间的最终落点未展开；"云结论是否进 `riskData`/`c0.b` 结果库"待另一成员确认。

### 4.11 本地库 TTL / 过期策略

- **观测**：Room 的 8 张表**均无时间戳列、无 `expire`/`ttl`/`updateTime` 列**（§4.1 DDL 全表），也没有任何 `DELETE … WHERE time < ?` 形态的清理 SQL。
- **观测**：唯一的"新鲜度"机制是 `version` 表的 7 个整型版本号 + 服务端 `dataVersion` 比对（§4.7）。
- **观测**：`i0\d`（旧栈）的 `antivirus_db_version` 有 `release_time` 列（`i0\b.java:15`），但**只在响应里落库，未参与任何过期判断**。
- **推断**：过期策略完全由服务端通过"下发 `status=deleted` 条目"或"`updateType=global` 整表替换"实现；客户端侧无本地 TTL。若长期不联网，库内容永久保留。

### 4.12 扫描管线中云端的位置

**观测** `p0\b0.java:202-211`：

```java
b0(codeScanner g, cloudScanner f, matcherScanner z, uploadApi q0.n)
```

| 阶段类 | 职责（推断自命名与 `p0.j` 装配） |
|---|---|
| `p0.g` CodeScanner（32.3KB，最大） | 代码级特征扫描（规则匹配） |
| `p0.f` CloudScanner | 云端指纹校验（本节主体） |
| `p0.z` MatcherScanner（20.9KB） | 本地黑/白/灰名单匹配（对应 Room 8 表） |
| `p0.y`（5.0KB） | 特征规则编译（`CodeScanner` 的 "bad input characteristic" 出自此处 :70） |
| `p0.b0` RealScanner | 编排 + 结果上报 |

---

## 5. 配置项 / 常量 / 协议字段

### 5.1 常量表

| 名称 | 值 | 语义 | 出处 |
|---|---|---|---|
| 旧栈盐（病毒库/白名单） | `94e6cec2-19dc-4bb6-823c-613f72d3ea16` | MD5 签名静态盐 | `i0\d.java:55`, `:203` |
| 旧栈盐（闪电检测） | `6988567a-4220-4b51-bc2d-ccdec27a74a1` | MD5 签名静态盐 | `j0\c.java:75` |
| 新栈盐 | `62b36629-ecb4-4725-9f33-ccb0fd158191` | MD5 签名静态盐 | `q0\q.java:22` |
| AES 密钥 | `wcw1xbplx7aijwqr`（16B ASCII） | 列加密 + 请求/响应体加密 | `v0\j.java:38-45` + `flamingo.dat` |
| 密钥 XOR 常量 | `4831221107097930563` = `0x430BF1FFFFF10B43` | 密钥混淆 | `v0\j.java:43-44` |
| 密钥文件 | `META-INF/flamingo.dat`，偏移 144，长 16 | 密钥载体 | `v0\j.java:38-41` |
| `EncryptVersion` | `"0"` | 加密协议版本 | `v0\j.java:99-101` |
| `appVersion` | `"1"` | 旧栈版本参数 | `h0\b.java:46` |
| 规则引擎库版本 | `"flamingo-android_release"` | Kotlin `@Metadata` 的 module 名 | 各 pojo 的 `Metadata(...)` |
| `Room identity_hash` | `8856aeeb52eb3fc27eb4570c1cd903b7` | 当前 schema 指纹 | `EngineDatabase_Impl.java:79`, `:357` |
| 旧 schema 指纹 | `4033f04c51ab315bb8679d5c0f44fb16` | 历史 schema | `EngineDatabase_Impl.java:357` |
| 库版本号 | `2` | `a(2)` | `EngineDatabase_Impl.java:357` |
| `biz_id` | `"virus_scan"` | 闪电检测业务标识 | `j0\c.java:62` |
| `basisOf` 过滤值 | `2` | 仅保留该值条目 | `j0\c.java:41`, `j0\a.java:51` |
| `android_id` 键 | `"android_id"` | `Settings.Secure` | `h0\b.java:261` |
| 网络开关属性 | `persist.sys.sc_allow_conn` | 系统属性总闸 | `GuardApplication.java:38+` |
| 日本定制区 | `jp_kd` | 追加开关判定 | `GuardApplication.java:38+` |
| 日本区附加开关 | `Settings.Secure "antivirus_engine_network_enable"` | ==1 才允许 | 同上 |
| 全量扫描能力位 | metaData `miui.support_full_scan` | `i0.h.h()` | `i0\h.java:91` |
| 混合扫描能力位 | metaData `securitycenter.support.mix.scan` | `h0.a.a()` | `h0\a.java:14` |
| 本包混合扫描位 | metaData `guardprovider.support.mix.scan=true` | Manifest | 简报 |
| 扫描参数默认 | `scan_properties=2` / `f7380c=3` | 位域 | Manifest / `p0\f.java:353` |
| 签名算法名 | `"MD5"` | 三处签名 | `h0\b.java:293`, `q0\q.java`(`l0.f.b`), `j0\c.java:92` |
| 加密算法名 | `"AES/CBC/PKCS5Padding"` | 列+传输加密 | `v0\j.java:59`, `:79` |

### 5.2 云端 API 完整清单

| # | 栈 | Method | 路径 | 服务基址 | 请求 | 响应类型 | 定义 |
|---|---|---|---|---|---|---|---|
| 1 | 新 | GET | `/blackGrays/feature/codeList` | `virus-engine.sec[.intl].miui.com` | `@QueryMap` | `BlackGraysResponse` | `q0\a.java:11` |
| 2 | 新 | GET | `/blackGrays/blocklist` | 同上 | `@QueryMap` | `BlackGrayAppResponse` | `q0\b.java:11` |
| 3 | 新 | GET | `/virus/feature/codeList` | 同上 | `@QueryMap` | `CharacteristicsResponse` | `q0\c.java:11` |
| 4 | 新 | POST | `/virus/scan` | 同上 | `@Body List<CloudScanApkInfo>` + `@QueryMap` | `CloudScanResponse` | `q0\d.java:13-14` |
| 5 | 新 | GET | `/virus/blocklist` | 同上 | `@QueryMap` | `GuiltyPackagesResponse` | `q0\e.java:11` |
| 6 | 新 | GET | `/virus/allowlist` | 同上 | `@QueryMap` | `InnocentPackagesAndSignaturesResponse` | `q0\f.java:11` |
| 7 | 新 | POST | `/virus/localResult/report` | 同上 | `@Query("timestamp") long` + `@Body List<ResultReportData>` | `ResultRequestResponse` | `q0\n.java:12-13` |
| 8 | 旧 | POST | `https://api.sec.miui.com/antivirus/viruses` | 固定 | form: `{version, updateType, oa, +7 公共, sign}` | JSON `{version, releaseTime, updateType, viruses[]}` | `i0\d.java:203` |
| 9 | 旧 | POST | `https://api.sec.miui.com/antivirus/whitelist` | 固定 | form: `{oa, +7 公共, sign}` | JSON `{whitelist:[{packageName, signature}]}` | `i0\d.java:55` |
| 10 | 旧 | POST | `https://flash.sec.miui.com/detect/app` | 固定 | JSON `{timestamp, os, biz_id, uuid, content[], sign}` | JSON `{data:[{pkg, signature, version, basisOf}]}` | `j0\c.java:75` |
| — | 外部 SDK | — | `https://virus-engine.sec[.intl].miui.com` | 同 #1-7 | 引擎/特征库更新（`TencentEngine`/AVL） | — | `q0\m.java:12`（常量被复用） |

> 新栈是否 GET/POST：`q0.d` / `q0.n` 含 `@f6.a`（`@Body`）→ 必为 POST；`q0.a/b/c/e/f` 仅 `@QueryMap`（`@f6.u`）无 body → 推断为 GET（`f6.f`/`f6.o` 注解类仅有 `value()`，静态无法区分，标为推断）。

### 5.3 报文结构完整字段表

> 所有类均为 Kotlin data class；**无 `@SerializedName` 注解**（除 `StringItem.target` 与 `CharacteristicRecord/BlackGrayTraitRecord` 的 `rule_name`/`threat_name`），因此 JSON 字段名 = Java 字段名。

#### 5.3.1 请求/响应信封与公共

| 类 | 字段 | 类型 | 语义（推断） | 出处 |
|---|---|---|---|---|
| `u0.e`（接口） | `code` | int | 业务码。**0 = 失败** | `u0\e.java:5` |
| | `desc` | String | 失败描述 | `u0\e.java:9` |
| | `data` | Object | 载荷 | `u0\e.java:7` |
| `CloudScanResponse` implements `e` | `code` | int | 同上 | `CloudScanResponse.java:11` |
| | `desc` | String | 同上 | :13 |
| | `data` | `List<CloudScanPayload>` | 逐包结论 | :12 |
| `u0.g`（枚举） | `deleted` / `online` | — | 数据集条目状态 | `u0\g.java:4-6` |
| `u0.f`（接口） | — | — | 带 `status` 的同步条目基接口 | 由 `BlackGray`/`Characteristic`/`GuiltyPackage`/`InnocentPackage`/`TrustCert`/`BlackGrayApp` 实现 |

#### 5.3.2 云端扫描

**`CloudScanApkInfo`（请求体元素）**

| 字段 | 类型 | 语义 | 出处 |
|---|---|---|---|
| `packageName` | String | 包名 | `CloudScanApkInfo.java:13` |
| `versionCode` | **String** | 版本号（字符串化） | :14 |
| `apkCertificateMD5` | String | 签名证书 MD5 | :10 |
| `apkCertificateSHA256` | String | 签名证书 SHA-256 | :11 |
| `mfFileSHA256` | String | `META-INF/MANIFEST.MF` 的 SHA-256 | :12 |

**`CloudScanPayload`（响应体元素）**

| 字段 | 类型 | 语义 | 出处 |
|---|---|---|---|
| `threatName` | String（可空） | 威胁名（Threats 时必填） | `CloudScanPayload.java:20` |
| `packageName` | String | 包名（与请求按序对应） | :18 |
| `versionCode` | int | 版本号 | :21 |
| `apkCertificateSHA256` | String | 证书 SHA-256 | :17 |
| `resultType` | `EnumSet<CloudScanPayload.a>` | `{Clean, Threats, BlackGrays, Unknown}` | :19, :23-32 |

**`ResultReportData`（上报元素）** — 13 字段

| 字段 | 类型 | 语义（推断） | 出处 |
|---|---|---|---|
| `threatLevel` | String | 威胁等级（`t0.k.threatLevel`） | `ResultReportData.java:19` |
| `threatName` | String | 威胁名 | :20 |
| `malwareCodeFeature` | String | 命中代码特征（`t0.k.codeFeature`） | :15 |
| `version` | int | 特征库版本（`t0.k.version`） | :21 |
| `packageName` | String | 包名 | :18 |
| `appName` | String | 应用名 | :13 |
| `versionCode` | int | 版本号 | :22 |
| `apkCertificateMD5` | String | 证书 MD5 | :11 |
| `apkCertificateSHA256` | String | 证书 SHA-256 | :12 |
| `mfFileSHA256` | String | MF 指纹（空串兜底） | :16 |
| `miuiVersion` | String | `Build.VERSION.INCREMENTAL` | :17 |
| `androidVersion` | int | `Build.VERSION.SDK_INT` | :10 |
| `device` | String | `Build.DEVICE` | :14 |

**`ResultRequestResponse`**

| 字段 | 类型 | 语义 | 出处 |
|---|---|---|---|
| `code` | int | 业务码 | `ResultRequestResponse.java:10` |
| `desc` | String | 描述 | :11 |

#### 5.3.3 名单同步

**`GuiltyPackage`（黑名单条目）**

| 字段 | 类型 | 语义 | 出处 |
|---|---|---|---|
| `packageName` | String | 包名 | `GuiltyPackage.java:15` |
| `versionCode` | Integer | 版本号（可空 → 全版本命中） | :18 |
| `apkCertificateMD5` | String | 证书 MD5 | :12 |
| `apkCertificateSHA256` | String | 证书 SHA-256（可空） | :13 |
| `mfFileSHA256` | String | MF 指纹（可空） | :14 |
| `threatName` | String | 威胁名 | :17 |
| `status` | `u0.g` | `deleted` / `online` | :16 |

**`GuiltyPackagesData` / `GuiltyPackagesResponse`**

| 类 | 字段 | 类型 | 语义 | 出处 |
|---|---|---|---|---|
| `GuiltyPackagesData` | `blocklist` | `List<GuiltyPackage>` | 黑名单 | `GuiltyPackagesData.java:11` |
| | `dataVersion` | int | 数据集版本（用于增量） | :12 |
| `GuiltyPackagesResponse` | `code` / `desc` / `data` | int / String / `GuiltyPackagesData` | 信封 | `GuiltyPackagesResponse.java:10-12` |

**`InnocentPackage`**

| 字段 | 类型 | 语义 | 出处 |
|---|---|---|---|
| `packageName` | String | 包名 | `InnocentPackage.java:14` |
| `apkCertificateMD5` | String | 证书 MD5 | :12 |
| `apkCertificateSHA256` | String | 证书 SHA-256（可空） | :13 |
| `status` | `u0.g` | 状态 | :15 |

**`TrustCert`（可信开发者证书）**

| 字段 | 类型 | 语义 | 出处 |
|---|---|---|---|
| `developer` | String | 开发者名 | `TrustCert.java:13` |
| `apkCertificateSHA256` | String | 证书 SHA-256 | :12 |
| `status` | `u0.g` | 状态 | :14 |

**`InnocentPackagesAndSignatures`（白名单 + 开发者证书，一个响应两套数据）**

| 字段 | 类型 | 语义 | 出处 |
|---|---|---|---|
| `allowlist` | `List<InnocentPackage>` | 白名单 | `InnocentPackagesAndSignatures.java:11` |
| `allowlistVersion` | int | 白名单版本 | :12 |
| `developerSignList` | `List<TrustCert>` | 可信证书列表 | :13 |
| `developerVersion` | int | 证书列表版本 | :14 |

**`InnocentPackagesAndSignaturesResponse`**：`code` / `desc` / `data:InnocentPackagesAndSignatures`（:10-12）

#### 5.3.4 特征码同步

**`Characteristic`（威胁特征规则）**

| 字段 | 类型 | 语义 | 出处 |
|---|---|---|---|
| `ruleName` | String | 规则名 | `Characteristic.java:16` |
| `version` | int | 规则版本 | :20 |
| `strings` | `List<StringItem>` | 特征串集合 | :18 |
| `condition` | `Condition` | 布尔条件表达式 | :15 |
| `threatName` | String | 威胁名 | :19 |
| `status` | `u0.g` | 状态 | :17 |

**`CharacteristicsData`**：`dataVersion: int`、`featureCodeList: List<Characteristic>`（:11-12）
**`CharacteristicsResponse`**：`code` / `desc` / `data:CharacteristicsData`（:10-12）

**`StringItem`（特征串项）**

| 字段 | 类型 | JSON 名 | 语义 | 出处 |
|---|---|---|---|---|
| `key` | String | `key` | 变量名（条件表达式引用） | `StringItem.java:13` |
| `value` | String | `value` | 十六进制字节模式 **或** 文本正则 | :17 |
| `target` | `ScanTarget` | **`scanFile`** | 匹配目标 | :15-16 |

**`ScanTarget`（多态，靠 Gson 适配器 `q0.o` 以字符串往返）**

| 子类 | pathPattern | 语义 | 出处 |
|---|---|---|---|
| `AndroidManifest` | `/AndroidManifest.xml` | 只扫清单 | `ScanTarget.java:14`, :22-28 |
| `Dex` | `/*.dex` | 扫所有 dex | :15, :32 |
| `Mf` | `/META-INF/MANIFEST.MF` | 扫 MF | :16 |
| `So` | `/lib/**/*.so` | 扫 native 库 | :17 |
| `SpecifiedFile` | 任意字符串 | 指定文件 | `q0\o.java:32` |

**`BlackGray`（黑灰产特征规则）**

| 字段 | 类型 | 语义 | 出处 |
|---|---|---|---|
| `ruleName` | String | 规则名 | `BlackGray.java:16` |
| `strings` | `List<StringItem>` | 特征串 | :18 |
| `condition` | `Condition` | 条件表达式 | :15 |
| `version` | int | 版本 | :19 |
| `status` | `u0.g` | 状态 | :17 |

**`BlackGraysData`**：`dataVersion:int`、`featureCodeList:List<BlackGray>`（:11-12）
**`BlackGraysResponse`**：`code`/`desc`/`data:BlackGraysData`（:10-12）

**`BlackGrayApp`（黑灰产应用条目）**

| 字段 | 类型 | 语义 | 出处 |
|---|---|---|---|
| `packageName` | String | 包名 | `BlackGrayApp.java:15` |
| `versionCode` | Integer | 版本号（可空） | :17 |
| `apkCertificateMD5` | String | 证书 MD5 | :12 |
| `apkCertificateSHA256` | String | 证书 SHA-256 | :13 |
| `mfFileSHA256` | String | MF 指纹 | :14 |
| `status` | `u0.g` | 状态 | :16 |

**`BlackGrayAppData`**：`blocklist:List<BlackGrayApp>`、`dataVersion:int`（:11-12）
**`BlackGrayAppResponse`**：`code`/`desc`/`data:BlackGrayAppData`（:10-12）

#### 5.3.5 `networkdomain.a`（Gson 适配器工厂）

| 项 | 内容 | 出处 |
|---|---|---|
| 类 | `a implements e1.c`（Dagger `Provider` of Gson `TypeAdapterFactory`） | `networkdomain\a.java:6` |
| 单例 | 静态内部类持有 | :9-13 |
| 产出 | `new CloudScanPayload.b()` | :19-21 |
| 作用 | 注册 `CloudScanPayload.a`（枚举）的 Gson 适配器 | 由 `s0\s.java:85` 消费 |

### 5.4 OkHttp / Retrofit 装配参数

| 项 | 值/来源 | 出处 |
|---|---|---|
| baseUrl | `q0.m.f7703a` | `s0\s.java:124` |
| ConverterFactory | `e6.a.f(gson)`（GsonConverterFactory） | `s0\s.java:124` |
| OkHttp 拦截器顺序（application） | `q0.q` → `q0.g` → `q0.i` → `s0.s.a` | `s0\s.java:110-112`, `p0\j.java:257` |
| networkInterceptor | `q0.k` | `s0\s.java:109` |
| 请求头 `EncryptVersion` | `"0"` | `q0\g.java:29`, `s0\s.java:43` |
| 日志 TAG | `MiEngineNG` | `s0\s.java:41-51` |
| 旧栈超时 | `com.xiaomi.onetrack.g.b.f2462b`（connect=read） | `h0\b.java:333-334` |

---

## 6. 权限与攻击面 / 风险点

| # | 风险点 | 依据 | 等级 |
|---|---|---|---|
| R1 | **加密密钥硬编码于 APK 资源**：`META-INF/flamingo.dat` 明放在包内，密钥仅做一次 XOR；已完整还原为 `wcw1xbplx7aijwqr`。任何人可解开 Room 全部 BLOB 列与全部 HTTPS 请求/响应体 | `v0\j.java:38-45` + 实测 | 高 |
| R2 | **三处签名均为 `MD5(排序参数 + 静态盐)`**，无 HMAC、无时间戳防重放、无 nonce、无服务端下发的会话密钥；盐值硬编码。签名可离线伪造 | `h0\b.java:272-299`, `q0\q.java:16-30` | 高 |
| R3 | **请求/响应体加密密钥全局共用且版本固定 `"0"`**，无密钥轮转机制（`d()` 恒返回 `"0"`）；一旦泄露对所有版本、所有设备通用 | `v0\j.java:99-101` | 高 |
| R4 | **列级加密使用随机 IV，破坏等值查询与唯一约束**：UNIQUE 索引对同明文无效（每次密文不同），`WHERE packageName=?` 永不命中；黑名单去重失效 → 库无限膨胀 + 匹配必须全表解密 | `v0\j.java:58`, §4.2 | 中 |
| R5 | **TLS 无 pinning、无自定义 TrustManager**：系统 CA 注入即可中间人；叠加 R1（密钥可得）后，攻击者可完整解密/篡改云端协议 | §4.6.7 | 中 |
| R6 | **网络总闸依赖系统属性 `persist.sys.sc_allow_conn`**：具备系统属性写权限的组件（或 root）可强制开启/关闭全部联网，属未受保护的隐式控制面 | `GuardApplication.java:38+` | 中 |
| R7 | **`guard/support_incremental` 在能力不足时返回 `null` 而非错误**：调用方可能把 "null cursor" 误判为"无限制"或崩溃 | `GuardProvider.java:405-411` | 低 |
| R8 | **`CloudScanPayload` 下标对齐断言方向可疑**：`if (equals(pkgA,pkgB)) throw "Check failed."` —— 语义是"包名相同即报错"，与"一一对应"直觉相反；若服务端按包名回显（常见做法）将 100% 抛异常 | `p0\f.java:212-216`, `:315-318` | 中（待运行验证） |
| R9 | **`code == 0` 被判为失败**：与常见"0=成功"约定相反；若服务端返回 0 表示成功，客户端会永久视为错误 | `p0\f.java:198-204`, `:301-307` | 中（待运行验证） |
| R10 | **`/virus/scan` 一次上传全部待扫应用的包名+版本+证书指纹**：无分批、无去标识化的固定上限；配合稳定的设备标识（`oa`/`uuid`/`mi`）形成可长期追踪的应用清单侧信道 | `p0\f.java:271-291`, §4.6.3/§4.6.5 | 中（隐私） |
| R11 | **`flash.sec.miui.com/detect/app` 上报全部非系统应用（含应用名）**，`appname` 仅 URL-encode 未脱敏；响应结果整表替换 `mi_detect_app_db` 且无版本校验 | `j0\c.java:57-80`, `j0\a.java:37-70` | 中（隐私） |
| R12 | **无任何重试/退避**：网络抖动即导致特征库整轮同步失败，且失败静默（仅日志）；结合无 TTL，可造成长期使用过期库 | §4.6.8, §4.11 | 低 |
| R13 | **DB 破坏性迁移**（空迁移数组 + `p()` 空集）：版本升级清库，若升级失败会导致全量重新下载，无增量恢复 | `EngineDatabase_Impl.java:361-368` | 低 |
| R14 | **`trust_installers` 表建表但无 DAO**：`pk_index` 索引与数据永不被应用读取，疑似遗留/未接线功能 | `EngineDatabase_Impl.java:65-66`, `q()` 无对应项 | 低 |
| R15 | **`TrustInstallerRecord` 业务语义可被误解**：名字暗示"可信安装器"，但无任何代码路径写入/读取，其真实用途未知 | 同上 | 低（信息） |
| R16 | **`scan_properties` 位域语义靠调用方约定**：`scan_params` 从 `l0.d` 上下文取，若外部 IPC 调用方能控制该值即可关闭云扫（`bit3`）——但 `scan_properties=2` 由 Manifest 固定，实际可控性未知 | `p0\f.java:370-379` | 低 |

---

## 7. 未解决问题

| # | 问题 | 已掌握线索 | 建议下一步 |
|---|---|---|---|
| Q1 | `p0.w.b(String)`（新栈 `uuid`）的注入点与取值来源 | `p0\w.java:23-33` 只做缓存；调用方未定位 | 全树搜 `p0.w.f7555a.b(` / `w.f7555a` |
| Q2 | `EngineDatabase` 的 `RoomDatabase.Builder` 装配点（是否 `fallbackToDestructiveMigration`、`openHelperFactory` 是否用 SQLCipher） | `s0\r.java` 在 `p0\j.java:184` 被 `s0.r.a(pVar, …)` 引用但未展开 | 读 `s0\r.java`（1350B 级）与其上 `s0\j.java` |
| Q3 | `Room.databaseBuilder(...).build()` 的 db 文件名 | 未在已读代码中出现字符串 | grep `"engine.db"` / `.databaseBuilder(` |
| Q4 | `guilty_package` / `innocent_package` / `black_gray_app` 的等值查询是否为死代码（随机 IV 导致永不命中） | §4.2 推断成立则必为死代码 | 动态验证：同一 APK 连跑两轮同步，观察行数增长 |
| Q5 | `p0.f` 的 index 对齐断言方向（R8）与 `code==0` 判负（R9）是否真实 bug | 源码直线可读，但语义反直觉 | 抓包 + 实测 `/virus/scan` 响应 |
| Q6 | `t0.a` 五个访问器 `b()/d()/f()/g()` 与 `apkCertificateMD5/SHA256/mfSHA256/versionCode` 的精确对应 | 已按 `CloudScanApkInfo` 构造器参数顺序推定；`t0.a` 是接口（`t0\a.java:4`）未展开实现 | 读 `t0.d`（`t0\d.java` 2290B）与具体 `t0.a` 实现 |
| Q7 | `v5` 条件表达式引擎的语法定义（支持的运算符/函数） | 分隔符 `" ()!\t\n\r\f&|"` 提示 `!&|()` | 读 `v5\b.java` / `v5\a.java` 的 parser 入口 |
| Q8 | `v0\l` 中 `t0.j.b(StringItem)` 判定"二进制特征 vs 文本正则"的依据 | `v0\l.java:63` 调用 | 读 `t0\j.java`（511B 级） |
| Q9 | `t0.d.b/c/e`（结论工厂）如何把云结论落到结果库/UI；云结论是否写入 `c0.b` 风险库 | `p0\b0.java:235-278` 只看到上报，未见落库 | 读 `t0\d.java` + `c0\b` |
| Q10 | 云端响应中的 `dataVersion` 与实际表内容的强一致性（是否在事务内） | `r0\g.java:153-171` 先写表后写版本，但未见显式 `withTransaction` | 读 `r0\r.java` / `r0\q0.java` 的 `i(int)` 实现 |
| Q11 | `o5\k.java` / `g5\y.java` 为何引用 `JceStruct` 常量（是否 tmsdk 与 okhttp 共用某二进制协议） | 仅常量引用，无 `JceInputStream` 调用 | 低优先；读 `o5\k.java` 上下文（wifidetect） |
| Q12 | `trust_installers` 表与 `TrustInstallerRecord` 的写入方 | 全树无 DAO | grep `TrustInstallerRecord(` |
| Q13 | 是否存在自定义 `network_security_config`（影响 R5） | 本次未查 `res\xml` | 另一成员查 `apktool\res\xml\network_security_config*.xml` 与 Manifest `android:networkSecurityConfig` |
| Q14 | `i0\d` 旧栈的 `sync` 触发频率（定时/开机/按需） | `n0\g.java:643` 可见调用点，未定位调度 | 读 `n0\g.java` 的调用上下文 |

---

## 8. REA 证据 ID 列表

| # | evidence_id | 操作 | 目标 | 核验内容 |
|---|---|---|---|---|
| 1 | `ev_c243d381cb5d8b7d5b504fd6ee872e0567597a94270a5a8d8e68e055c87649da` | `inspect_android_class` | `com.miui.guardprovider.sdk.android.pojo.networkdomain.CloudScanPayload` | 5 字段 + 16 方法 + 2 内部类（`a`=枚举、`b`=Gson 适配器）；确认 `resultType` 为 `EnumSet<CloudScanPayload$a>`，`versionCode` 为 `int` —— 与源码一致 |
| 2 | `ev_962774300548c3ef27d8bf5ece9a1e5faa8d555bdccbf20ac094b0b5059f76a8` | `inspect_android_method` | `h0.b.i(Map,String)` | 字节码级确认签名 = 排序 → `k=v&…` → 追加 secret → `Base64(…,2)` → `j()`(MD5 upper hex) |
| 3 | `ev_73f513b6297f98af28c05f9a72e76fc7de048e96e2b2cf54d1f0d31842be04b6` | `inspect_android_method` | `v0.j.a()` | 确认密钥读取路径 `META-INF/flamingo.dat` + `skip(144)` + 双 long XOR `4831221107097930563` + `SecretKeySpec(…,"AES")` |
| 4 | `ev_3415eba82c6fc7618221660100562cfc2a37b45c3e1c4439e2af205dbbfdb3b4` | `inspect_android_method` | `com.miui.guardprovider.sdk.android.db.EngineDatabase_Impl$a.a(h.i)` | 7 表 DDL 原文 + 6 个 UNIQUE INDEX + `room_master_table` 与 `identity_hash=8856aeeb52eb3fc27eb4570c1cd903b7`，与 §4.1 表格逐字一致 |
| 5 | `ev_94cce8a5ff954f5a64232136f7c1bf09efc1134e057a292a523eec6ff5cfe818` | `inspect_android_method` | `q0.q.b(Map)` | 确认新栈签名 = `TreeMap` 排序 → `StringJoiner("&")` → 追加 `62b36629-ecb4-4725-9f33-ccb0fd158191` → UTF-8 → `l0.f.b(bytes,"MD5",true)` |

---

## 附：与共享简报的冲突/修正清单

| # | 简报陈述 | 本次观测 | 结论 |
|---|---|---|---|
| C1 | 「`flash.sec.miui.com/detect/app` # 云端闪电扫描」 | 该端点是 `j0.c` AntiDefraudAppManager 的**非系统应用清单批量上报 / 不予信任名单整体下发**，落 `mi_detect_app_db`，非逐样本云查杀 | **修正**：真正的单样本云扫是 `https://virus-engine.sec.miui.com/virus/scan`（`q0\d.java`） |
| C2 | 「`com\qq\taf\jce\*.java` — JCE 序列化框架……指出本项目实际使用的 JCE 结构体是哪些」 | 117 个 `JceStruct` 子类**全部**在 `tmsdkobf`/`tmsdk.*`；`com.miui.guardprovider` 自有包内零引用，自有协议全用 Gson | **修正**：JCE 属腾讯 tmsdk 依赖，不承载 GuardProvider 云端协议 |
| C3 | 报告 pojo 清单为 9 个 | 存在第 10 个实体 `VersionRecord`（JADX 名为 `t0.u`），另有 `t0.e` = `BlackGrayAppRecord` | **补充**：`pojo` 目录下确实 9 个文件，但 `version` 表实体在 `t0` 包 |
| C4 | 简报未提及 | 新增发现：`EngineDatabase` 全部 BLOB 列为 **AES-CBC 随机 IV 密文**，密钥可从 `META-INF/flamingo.dat` 完整还原 | **新增**（本报告 R1–R4） |
| C5 | 简报未提及 | 新增发现：**两套签名盐 + 两套设备标识 + 两层网络开关** | **新增**（§4.6） |
| C6 | 任务描述：「`guard/support_incremental` 背后的 `i0.h.h(ctx)` 判定条件与增量同步协议」 | `i0.h.h(ctx)` 读的是 `miui.support_full_scan`（**全量扫描能力位**），不是增量判定；`support_incremental` 在能力不足时返回 **null** | **修正**：见 §4.7；真正的增量同步是 `dataVersion` 版本比对（新栈）与 `updateType=incremental`（旧栈） |

