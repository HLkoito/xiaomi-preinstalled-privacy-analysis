> **归档警示（已纠错）**：这是原分析材料的脱敏归档，不能代替 [最终复核报告](../REPORT.md)。原文关于“明文 IMEI 列表”“默认配置即实际上传”、绝对覆盖率及未知 native 行为的部分结论不成立。IMEI/IMSI 的相关 DAU 列表经过 MD5，仍受区域/权限/联网门控。证书过期也不能单独推出密文无法解密。请先阅读最终报告与其中纠错表。本文删除本地身份路径，原有文件行号引用可能指未纳入发布的完整分析树。

# R8 混淆业务层语义映射与特性链路还原

> 范围：`_analysis/src/sources/` 下全部单/双字母混淆包（约 200 个），不含
> `com/ androidx/ tmsdk/ tmsdkobf/ xcrash/ android/ dagger/ j$/ kotlin/ okhttp3/ org/ miuix/`；
> `n0 c0 i0 d0 w0 x f0 y` 属 t-core，本报告只引用不展开（凡引用均标 `[core]`）。
> 标注约定：**观测**＝源码直接可见（文件:行）｜**推断**＝有依据归纳｜**未知**＝未覆盖。

## 0. 方法、边界与一个关键前提

**关键前提（观测）**：R8 的 repackage 是"按类分配短包名"，**不是按语义分目录**。同一个包内可能混有
来自不同库的类（例：`l0` 里既有业务 pipeline 接口 `l0.c/d/e`，也有 `kotlin.io` 的 `newInputStream` 字面量）。
因此本报告第 2 节的"包 → 功能域"一律按**该包内占多数的类**判定，并对混装包单独注明。

分类手段（全部可复现）：
1. 全树类声明索引 `_analysis/report/_obf_index.tsv`（3231 行，`路径<TAB>声明`）。
2. 逐包 `import` 统计（import 不会被 JADX 的常量美化污染，是本报告最可靠的一手证据）。
3. 逐包"最高频长字符串字面量"指纹（用于识别库身份，例：`Operation is not supported for read-only collection`）。
4. 逐包被谁 `import`（入度）——用于区分"被业务引用的支撑库"和"无人引用的死代码/库内部"。
5. 从业务锚点向下追：`GuardProvider` → `n0.g` → `f0.a` → 引擎实现；`p0` 扫描管线；`r0` Room；`s0/q0` 网络。
6. 全文关键词扫描（exec / DexClassLoader / 反射 / root / emulator / IMEI / 剪贴板…），按包聚合。

> **JADX 陷阱（必须知道）**：`switch` 的 `case 13:` 会被 JADX 标注成 `QScanConfig.EADT_BANNER /* 13 */`
> 这类**任意同值常量名**（见 `o2/a.java:111-127`），那只是字面量 13 的装饰，**不是**对 tmsdk 的引用。
> 同理 `j0.a(...)` 在 `f2/*` 里指的是 `f2` 包内的类 `j0`，不是包 `j0`。本报告所有"引用"结论只用 `import` 与限定名。

## 1. 职责概述

`com.miui.guardprovider.*` 只有 44 个类，是**薄壳**：ContentProvider（`GuardProvider`）、Service
（`SecurityService`）、AIDL 实现、三个引擎适配器（Mi/MiEngineNG、AVL、Tencent）、PackageParser 包装。
真正的业务实现全部落在本报告的混淆包里，可归为 8 条主线：

1. **扫描调度**：`n0.g`（任务表/引擎扇出/结果合并）+ 协程化的新管线 `l0`/`p0`/`t0`；
2. **规则引擎**：`t0`（Trait/StringItem/结论工厂）+ `p4`（kotlin.text 正则）+ `v5/a6/x5/y5/z5`（Apache JEXL3 表达式）+ native `libflamingo`；
3. **云端**：`q0`（Retrofit 接口/签名/加密拦截器）+ `s0`（MiEngineNG 客户端 + Room DAO）+ `p0.f`（闪电扫描）+ `p0.b0`（结果上报）；
4. **名单/库同步**：`i0.d`（api.sec.miui.com 增量/全量）+ `c0`（sqlite 结果缓存）+ `r0`（Room 8 表）；
5. **URL/钓鱼**：`n0.j` → `f0.a.m()` → `AVLEngine.scanURL` → `libavlurl.so`/`liburldetectorsys.so`；
6. **WiFi**：`n0.k` → `f0.a.n()` → `TencentEngine` → tmsdk `WifiDetectManager`；
7. **反诈/灰产**：`g0`（MiEngineUtils）+ `j0`（`mi_detect_app_db`）+ `c0.b`（`risk_gray_result_db`）；
8. **基建**：`v`（OAID 反射）、`z`/`u`（OneTrack/analytics 埋点）、`h0`（NetworkApiHelper）、`v0`（AES/RSA + 校验）、`w0`（SharedPreferences/DB 迁移）[core]。

第三方库残留占了混淆树的大部分体积：kotlin stdlib/collections/text/sequences、kotlinx-coroutines、
**kotlin-reflect + 内嵌的 Kotlin 编译器前端（约 40 个包、700+ 类）**、Gson、OkHttp/Okio、Retrofit、
Room、protobuf、Apache Commons JEXL3、commons-logging。**不做这层剥离就会把编译器前端当成业务逻辑。**

## 2. 包级语义地图

### 2.1 业务域总表（先看这张）

| # | 功能域 | 包 | 判定依据（可验证） | 代表类 |
|---|---|---|---|---|
| B1 | 扫描调度/任务队列/并发 | `n0`[core] `l0` `m0` `p0` | `n0/g.java:1260` `L()` 建任务、`:1272` 任务上限 10、`:1132` `K()` 扇出引擎、`:1296` `M()` type==1 走增量；`l0/e.java` 为 Scanner 接口，`p0/b0.java:202` 组装三扫描器 | `n0.g` `l0.e` `p0.b0` |
| B2 | 特征码/规则引擎 | `t0` `p4` `v5` `a6` `x5` `y5` `z5` | `t0` 被 `p0` import 133 次（`p0/*` 全树最高）；`p0/f.java:73` `t0.d.c(...)` 结论工厂；`v5/a.java` 为 JEXL 解释器、`a6` 为 JEXL 语法/解析（`"<DECIMAL_LITERAL>"`、`"variable is already declared"`）、`z5` 含 `"org.apache.commons.jexl3 { JexlBuilder {} }"` | `t0.s` `t0.j` `v5.b` `z5.a` |
| B3 | 云端扫描客户端（flash scan） | `q0` `s0` `p0` `u0` | `q0/d.java` `@f6.f("/virus/scan")`；`p0/f.java:264-295` 组包（androidVersion/miuiVersion/timestamp/uuid/versionType/scanType）；`s0/s.java:124` base=`q0.m.f7703a`；`u0/g.java` 状态枚举 deleted/online | `q0.d` `s0.s` `p0.f` `u0.e` |
| B4 | 云端上报 | `q0` `p0.b0` `s0` | `q0/n.java` `@f6.f("/virus/localResult/report")`；`p0/b0.java:235-277` 组 `ResultReportData`（含 `Build.VERSION.INCREMENTAL/SDK_INT/DEVICE`） | `q0.n` `p0.b0` |
| B5 | URL 检测/钓鱼/二维码 | `n0.j`[core] `p5`? `okhttp3` `com.avl.engine.urldetector` | `n0/j.java:39-72` URL 扫描单例；`f0/a.java:95` `m(int,String)`；`AvlEngine.java:195-205` `m(1)→AVLEngine.scanURL`、`m(2)→updateURLRule`；`UrlDetector.java:80-87` `loadLibrary("urldetectorsys")`+`libavlurl.so` | `n0.j` `AvlEngine.m` `UrlDetector` |
| B6 | WiFi 安全检测 | `n0.k`[core] `f5` `t` | `n0/k.java:99-115` 四个方法映射 `f0.a.d.ARP/DNSANDPHISHING/NETWORKSTATE/SECURITY`；`SecurityService.java:48-143` 四个 binder；`TencentEngine.java:497` `n()→i(...)`；`f5/a.java` import `tmsdk.bg.module.wifidetect.WifiDetectManager` | `n0.k` `TencentEngine` |
| B7 | 病毒库/引擎更新 | `q0` `s0` `i0`[core] `r0` `w0`[core] | `q0/m.java:12` 双域名；`s0/s.java:124` 指向该域名；`i0/d.java:203` `antivirus/viruses`、`:55` `whitelist`；`AvlEngine.java:176-193` `checkUpdate()/update()`；`TencentEngine` `UpdateManager.check(UPDATE_FLAG_VIRUS_BASE,...)` | `q0.m` `s0.s` `i0.d` `AvlEngine.l` |
| B8 | 安装拦截/静默安装 | `com.avl`(跨域) `p0` `n0.g` | `guard/install_scan`→`GuardProvider.java:415` `f(path,bool,engineName)`；`AVLA.java:83` `native installPackage(...)`；`com/avl/engine/l/b/c.java:27,80` 调它 | `AVLA.installPackage` |
| B9 | 结果聚合/去重/缓存/白名单 | `c0` `r0` `g0` `i0`[core] | `c0/c.java:8` `scan_result.db`、`c0/a.java:12` `risk_gray_result.db`、`w0/h.java:19` `miui_virus.db`；`n0/g.java:1014-1087` `D()` 合并去重（等级 1<2<3<4<5<6 的偏序） | `c0.d` `c0.b` `r0.*` |
| B10 | 反诈/灰产判定 | `g0` `j0` `c0` `r0` | `g0/e.java:20-34` 造 `virusLevel=6`、`extras="AntiDefraud:danger;"`、`engine="Mi"`；`j0/a.java:37-58` 解析云端 `basisOf==2` 写 `mi_detect_app_db`；`GuardProvider.java:103-127` `getRiskAppData` | `g0.e` `j0.a` `c0.b` |
| B11 | 埋点/统计 | `z` `u` `v` | `z` 含 OneTrack appid `2882303761517522561` + `com.miui.guardprovider_` 前缀；`u/a.java` import `com.miui.analytics.ITrack`；`v/a.java:30-34` OAID/UDID 反射 | `z.a` `u.a` `v.a` |
| B12 | 加密/编码 | `v0` `e5` `y0` | `v0` 11 文件、`javax.crypto` 9 处、字面量 `AES/CBC/PKCS5Padding`/`AES/ECB/PKCS5Padding`/`manifestMFSha256`；`e5` `NoSuchAlgorithmException`；`y0` 磁盘错误分支 | `v0.j` `v0.k` `v0.l` |
| B13 | 配置/云控/系统属性 | `c5` `d5` `t` `p1` | `c5/a.java` `isMiuiLiteRom failed` + `).getDeclaredField(`；`d5/a.java` import `miuix.core.util.SystemProperties`、`ro.product.mod_device`；`t/a.java` 同上 + `QScanConfig` | `c5.a` `d5.a` |
| B14 | DB/DAO（Room） | `r0` `s0.d` `h i j e g` | `r0` 44 文件、import `androidx.room` 类；`s0/d.java:140-199` 直接从 `EngineDatabase` 取 DAO；`h/i/j/e/g` = Room runtime（`SupportSQLiteLock`、`cancellationSignal`、`room_fts_content_sync_`） | `r0.d` `r0.a` `s0.d` |

> 说明：`n0 c0 i0 w0 x f0 y` 的判定见 t-core 报告；本表只列与本层耦合处。

### 2.2 第三方库残留识别表（**别当业务**）

| 库 | 包 | 证据 |
|---|---|---|
| Kotlin stdlib `kotlin.jvm.internal` | `w1` | `w1/l.java` 提供 `f(obj,"name")` 空值检查；字面量 `"(Kotlin reflection is not available)"` |
| `kotlin.jvm.functions` | `v1` | `v1/a.java` `interface a extends g1.c { Object f(); }` = `Function0.invoke`；`v1/l`=Function1、`v1/p`=Function2 |
| `kotlin.collections` | `h1` `i1` `o4` | `h1/a.java:11-15` 只读集合抛 `"Operation is not supported for read-only collection"`；`o4` `"Sequence is empty."`；`i1` `"copyOf(this, newSize)"` |
| `kotlin.text` / regex | `p4` `q4` `k4` `h5` `s1` | `p4` 字面量 `missingDelimiterValue`、`substring(startIndex, endIndex)`；`q4` `"Retrieving groups by name is not supported"` |
| `kotlin.ranges` / `kotlin.time` / `kotlin.io` | `b2` `r4` `l0`(部分) `r5` | `b2` `"Step must be non-zero."`；`r4` `"TimeSource(System.nanoTime())"`、`"ms is denormalized"` |
| `kotlin.coroutines` 基础 | `k1` `l1` `m1` `g1` `t1` `a1` `a2` `r1` `r3` `f4` `n4` | `k1` `"EmptyCoroutineContext"`；`l1/d` 为 `COROUTINE_SUSPENDED` 单例（`s0/d.java:52,98` 用 `l1.d.c()`）；`m1/a,d,k` 为 `BaseContinuationImpl/ContinuationImpl`；`g1` `"Lazy value not initialized yet."` |
| **kotlinx-coroutines** | `s4` `x4` `u4` `z4` `w4` `v4` `t4` `y4` | `s4/a.java:~20` `"... was cancelled"` + `g0(Throwable)`；`x4` `"kotlinx.coroutines.internal.StackTraceRecoveryKt"`、`"Inconsistent state"`；`u4` `"Unexpected waiter: "`、`"Channel was closed"`；`w4` `"Flow was aborted, no more elements needed"`；`z4` `"[Pool Size {core = "`；`t4` `"The main looper is not available"` |
| **kotlin-reflect + Kotlin 编译器前端**（最大残留） | `c4` `l2` `o2` `o3` `u2` `g4` `b3` `b4` `m4` `m2` `w2` `w3` `v2` `v3` `y2` `i2` `k2` `d3` `h3` `j3` `t3` `z2` `s3` `n3` `c3` `i4` `q3` `r2` `f2` `c2` `e2` `u1` `y1` `h4` `p2` `p3` `d2` `g2` `j4` `a4` `e4` `u3` `h2` `l4` | `c4/b.java:22` 异常串 `"kotlin/reflect/jvm/internal/impl/types/AbstractClassTypeConstructor"`；`f2/i.java` 聚合 `v1.a..w`+`c2.b`（= `KFunction` 全家族）；`r2/a.java:40-44` 反射 `Method.getType/getAccessor`；`m2` 字面量 `"kotlin/reflect/jvm/internal/impl/descriptors/annotations/AnnotatedImpl"`；`m4` `".../utils/WrappedValues"`；`u2/l.java` `implements o3.g`（SPI，`META-INF/services` 已由 lead 反证）；`g4/b.java` 枚举 `FOR_SUBTYPING/FOR_INCORPORATION/FROM_EXPRESSION`；`b3` 全为接口且只被 `y2/v2/z2/x2` 引用 |
| Gson | `l` `o` `n` `p` `q` `r` `t` `m` `c` `d` `e6` | `l` `"Not a JSON Object:"`/`"GSON cannot serialize"`；`o` `"ReflectionAccessFilter does not permit using reflection"`；`t` `"Expected a name but was"`；`c/d` `"This container does not support retaining Map.Entry objects"`；`q` `"getRecordComponents"` |
| OkHttp | `g5` `l5` `m5` `p5` `q5` `i5` `n5` `o5` `j5` `s5` `okhttp3` | `g5/e.java` `interface e extends Cloneable`（`Call`）、`g5/c.java` `abstract Closeable,Flushable`（`RequestBody`）；`m5` `"Unexpected status line:"`/`"network interceptor"`；`p5` `javax.net` 29 处 + `org.conscrypt`；`q5` `p5` 依赖（TLS provider） |
| Okio | `t5` `u5` `o5`(部分) | `t5/b.java` `implements d, t5.c, Cloneable, ByteChannel`（`Buffer`）；`t5/e.java` `Serializable,Comparable`（`ByteString`）；`u5` `"Unexpected hex digit"` |
| Retrofit | `f6` | `q0.a:9` `@f6.f("/blackGrays/feature/codeList")`、`@f6.u`、`@f6.t`；`d6/u` = `Retrofit.Builder` |
| Room | `h` `i` `j` `e` `g` | `j` `"SupportSQLiteLock"`/`"Unable to grab file lock."`；`h` `"cancellationSignal"`/`"cursor.notificationUri"`；`g` `room_fts_content_sync_`、`PRAGMA foreign_key_list(` |
| Protocol Buffers | `m3` `f3` `h3` `g3` `i3` `x3` `z3` `j3` | `m3` `"Wrong object type used with protocol message reflection."`；`f3/*` 24 个类 `extends m3.i implements m3.q`（`GeneratedMessageV3`） |
| Apache Commons JEXL3 | `v5` `a6` `x5` `y5` `z5` | `z5` `"org.apache.commons.jexl3 { JexlBuilder {} }"`；`v5` `"error calling operator '"`/`"unsolvable function/method '"`；`y5` `"unable to find class:"`；`a6` `"<DECIMAL_LITERAL>"`/`"variable is already declared"`（JavaCC 语法） |
| commons-logging | `c6` `org` | `c6/a.java` import `org.apache.commons.logging.Log`；`org/*` `"[CUSTOM LOG FACTORY]"` |
| Dagger | `e1` `e0` `z0` `z1` | `e1` `"Cannot return null from a non-@Nullable @Provides method"`；`z0` `"Given component holder %s does not implement %s or %s"` |
| androidx.room / core | `androidx` `e` | `e` `"androidx.room.IMultiInstanceInvalidationService"` |

### 2.3 逐包索引（本层全部包）

| 包 | 归属 | 依据 / 备注 |
|---|---|---|
| `p0` | B1+B3+B4 扫描器 | 见 2.1；内部 `b0`=RealScanner、`g`=CodeScanner、`f`=CloudScanner、`z`=MatcherScanner、`y`=规则编译 |
| `t0` | B2 领域模型 | `p0` 对其 import 133 次；`t0/s`=Trait、`t0/j`=StringItem、`t0/d`=结论工厂、`t0/m`=威胁名 |
| `r0` | B14 Room DAO | 44 文件；`r0/d`=black_gray_app、`r0/f`=black_gray_chs、`r0/l`=characteristics、`r0/a0`=guilty_package、`r0/g0`=innocent_package、`r0/l0`=trust_certs、`r0/p0`=version、`r0/a`=聚合 |
| `s0` | B3+B7 云客户端 | `s0/s.java` Retrofit/OkHttp 装配 + 解密拦截器 + Gson 适配器；`s0/d` EngineDatabase DAO 包装 |
| `q0` | B3+B7 API 定义 | 7 个 Retrofit 接口 + 3 个 OkHttp 拦截器（签名/加密/日志）+ `q0/m` 域名 |
| `u0` | B3 云响应 | `u0/g` `enum {deleted, online}`；`u0/i` 为其 Gson 适配器；`u0/e` 响应壳（code/desc/data） |
| `v0` | B12 加密服务 | `v0/j` CryptoService、`v0/k` Gzip、`v0/l` 校验；字面量 `AES/CBC/PKCS5Padding`、`manifestMFSha256` |
| `g0` | B10 反诈 | `g0/e.java` `MiEngineUtils`：level=6、`AntiDefraud:danger;`、engine=`Mi` |
| `j0` | B10 反诈库 | `j0/a.java` `mi_detect_app_db`（package/signatures），来源云端 `basisOf==2` |
| `c0` | B9 结果库 | `c0/a`=risk_gray_result.db、`c0/c`=scan_result.db、`c0/b/d`=两个 DAO |
| `h0` | B3 网络助手 | `h0/a` NetworkApiHelper、`h0/b` 带盐 GET（`94e6cec2-…`） |
| `c5` `d5` `t` `p1` | B13 系统属性 | 反射读 MIUI 属性 / `ro.product.mod_device` |
| `v5` `a6` `x5` `y5` `z5` | B2 JEXL3 | 见 2.2 |
| `f6` | Retrofit | `@f6.f` 注解 |
| `d6` | 混合：Gson `$Gson$Types` + OkHttp | 字面量 `"must include generic type (e.g., "`；import `g5` 17 次 → 低置信 |
| `f5` | wifi 适配（tmsdk） | `f5/a.java` import `WifiDetectManager`、`"Exception throw during when append"` |
| `w` | 空壳/兼容层 | `w/c.java` 方法体为空；`w/e.java` 仅实现接口，`guardprovider` 引用 2 次 |
| `z` `u` `v` | B11 埋点/OAID | 见 2.1 |
| `x0` | provider 客户端常量 | `x0/a.java` `Uri.parse("content://guard/virus")` |
| `y0` | IO 错误处理 | `"No space left on device"`、`OutOfMemoryError` |
| `e5` | 摘要/异常封装 | `"NoSuchAlgorithmException"`、`"IO exception happend in ByteArrayInputStream"` |
| `k5` | 任务队列（tmsdk 风格） | `"task is in multiple queues"`、`"getLogger(TaskRunner::class.java.name)"` |
| `l0` `m0` | B1 管线抽象 | `l0/c,d,e` 被 `p0` 实现；`m0` `"scanner"`/`"interceptor"` + import `l0.*` |
| `b5` | 单例管理 | `"manager create instance cannot return null"`、`"Cannot acquire object after close()"` |
| `m3` `f3` `h3` `g3` `i3` `x3` `z3` `j3` | protobuf/TMF 消息 | 见 2.2 |
| `h` `i` `j` `e` `g` | Room runtime | 见 2.2 |
| `l` `o` `n` `p` `q` `r` `t` `m` `c` `d` `e6` | Gson | 见 2.2 |
| `g5` `l5` `m5` `p5` `q5` `i5` `n5` `o5` `j5` `s5` | OkHttp 全家桶 | 见 2.2 |
| `t5` `u5` | Okio | 见 2.2 |
| `w1` `v1` `h1` `i1` `o4` `p4` `q4` `k4` `h5` `s1` `b2` `r4` `r5` | Kotlin stdlib 各子包 | 见 2.2 |
| `s4` `x4` `u4` `z4` `w4` `v4` `t4` `y4` | kotlinx-coroutines | 见 2.2 |
| `k1` `l1` `m1` `g1` `t1` `a1` `a2` `r1` `r3` `f4` `n4` | kotlin.coroutines 基础 | 见 2.2 |
| `c4` `l2` `o2` `o3` `u2` `g4` `b3` `b4` `m4` `m2` `w2` `w3` `v2` `v3` `y2` `i2` `k2` `d3` `h3` `j3` `t3` `z2` `s3` `n3` `c3` `i4` `q3` `r2` `f2` `c2` `e2` `u1` `y1` `h4` `p2` `p3` `d2` `g2` `j4` `a4` `e4` `u3` `h2` `l4` | kotlin-reflect/编译器前端 | 见 2.2；其中 `u2`=解析/覆写检查（`u2/l` 实现 `o3.g`）、`g4`=类型检查上下文（`FOR_SUBTYPING`…）、`b3`=纯接口标记层（低置信） |
| `e1` `e0` `z0` `z1` | Dagger | 见 2.2 |
| `c6` `org` | commons-logging | 见 2.2 |

### 2.4 未归类清单（诚实标注）

> 统计：本层共 **179** 个混淆包；已明确判定 **155** 个，**未定性 18** 个，低置信 6 个（下表末行）。

| 包 | 试过的手段 | 结论 |
|---|---|---|
| `a` `b` `g4` `n1` `s1` `t2` `u3` `w5` `x1` `j1` `l3` `b0` `b6` `a5` `c1` `d1` `f1` `s` `p` `k` `i` `f` `q` `x3` | 声明索引、import 统计、字面量指纹、入度 | **未知**。多为 1–3 个类的残片：无 import / 仅 `w1.l` / 空包级工具，入度 0–1，无法在不看 smali 的情况下定性。`n1` 仅被 `u0.g` 的枚举 `$values` 用（`n1.b.a`），疑为 kotlin 枚举辅助；`a5/c1/d1/f1/b6/w5/x1/t2/j1/l3/b0` 全树零入度。 |
| `s` `p` `k` `i` `f` `q` | 同上 | Gson/OkHttp 的边角（`p` 含 `"No time zone indicator"` → Gson 日期解析；`q` 含 `getRecordComponents` → Gson 反射访问器），置信中 |

## 3. 关键链路时序

### 3.1 `guard/sync_scan` / `install_scan` 同步扫描全链路

```
[调用方 App] ContentResolver.query(content://guard/sync_scan, selectionArgs=[path, bool])
  └─ GuardProvider.query  (com/miui/guardprovider/GuardProvider.java:395-399 → :200 f(path,bool,engineName=null))
       ├─ path 以 content:// 开头 → 先复制到 cacheDir/copy.apk (:213-216)，扫描后删除 (:262-266)
       ├─ path 以 file:// 开头 → 取 Uri.getPath() (:270)
       ├─ CountDownLatch(1) (:247)
       ├─ n0.g.z().L(new String[]{path}, observer, bool, 2, engineName)   (:249, :271)
       │    ├─ 过滤 null/不存在路径 (:1261-1271)；任务数 >=10 直接回 -1 (:1272)
       │    ├─ 建 6 张 ConcurrentHashMap（observer→结果/进度/引擎/序号/…) (:1285-1289)
       │    ├─ new n0.g.f(...).executeOnExecutor(THREAD_POOL_EXECUTOR) (:1290-1292)  ← 白名单预检
       │    │    └─ f.doInBackground: 对每个 apk parsePackageLite → 命中 i0.f 白名单则直接产 y.f(level=1)，
       │    │       否则把路径留在待扫列表 (:820-860)
       │    └─ onPostExecute → n0.g.K(list, observer, bool, 2, engineName, 已白名单数)  (:872)
       │         ├─ t(x.a.c()) 取"已启用引擎"列表 → new i().a(engine) (:1134, :1172-1188)
       │         ├─ 每个引擎一个线程：engine.j(paths, callback, allowCloud, engineName) (:1143, :539)
       │         │    └─ MiEngine/AvlEngine/TencentEngine 各自实现（com.miui.guardprovider.engine.*）
       │         └─ 进度消息 what=3 → 同一 path 多引擎结果按 D() 合并 (:217-241, :1014)
       ├─ countDownLatch.await(2, MINUTES)  (:250, :272)  ← 2 分钟硬超时
       └─ 返回 MatrixCursor，列 = Type,VirusName,Description,EngineName,AdType,AdName,AdDescription (:257-260)
```

- **`type` 参数（第 4 参 i6）的语义（观测+推断）**：`L()` 把它存进 `f6420f` 并只用于进度偏移
  (`n0/g.java:1288, :199`)；`M()` 里 **`i6==1` 特判走增量缓存扫描 `C()`**（`:1296-1302`），其余走 `L()`。
  `SecurityService.w()` 传 0（普通扫描，`:259`），`GuardProvider` 的 `sync_scan`/`install_scan` 传 **2**。
  ⇒ **0=普通应用扫描、1=增量(已装应用缓存)、2=同步/外部请求扫描（带 engineName）**。推断，依据上述 3 个调用点。
- **engineName 选择**：`K()` 把 `engineName` 原样交给 `engine.j(..., str)`；引擎列表来自 `x.a.c()`（SharedPreferences
  中启用的引擎）。`x.a.j()/k()` 会从远端名单中**随机挑一个**引擎置为唯一启用（`x/a.java:110-130`），
  `cloud_engine` 属性决定云引擎（`x/a.java:14-27`）。
- **结果裁剪**：`L()` 只返回**单个** `y.f`（`fVarArr[0]`，`GuardProvider.java:71-73`），即多引擎合并后的最高危结论；
  无结果返回 null（`:251-255`）。
- **失败/取消**：observer 的 `binderDied` 会中断线程并清任务（`n0/g.java:553-576`）；`cancelScan(id)` 按 id 查表取消（`:1372-1405`）。

### 3.2 云端闪电扫描（flash scan）

```
p0.b0.c()  [RealScanner]  (p0/b0.java:308-374)
  ├─ f (CloudScanner).a(ctx)       :351   ← 先云扫
  │    ├─ a(ctx): 读 pipeline 的 "scan_params" Int (:372)
  │    │    ├─ f7380c = scan_params（作为 scanType 上报；构造默认 3，:353）
  │    │    └─ f7381d = (scan_params & 8)==0   ← bit3 = 禁用云扫；为 false 时抛 IOException("cloud scan is denied!!!") (:376, :260)
  │    ├─ 取待扫 t0.a 列表 → 每组 ≤N 个 → k(a) 组 CloudScanApkInfo(pkg, versionCode, versionName, sigMd5, path) (:277-287, :358-368)
  │    ├─ POST /virus/scan  (q0/d.java:8)
  │    │    query: androidVersion=SDK_INT, miuiVersion=Build.VERSION.INCREMENTAL,
  │    │           timestamp=now, uuid=w.f7555a.a(), versionType=stable|development|alpha, scanType (:280-286)
  │    ├─ 响应 u0.e：code==0 → 抛 IOException("Response code:…")；否则取 data:List<CloudScanPayload> (:300-310)
  │    └─ 逐条 a(ctx, payload) → t0.d.c(...) 合并结论（命中 Threats → 造 t0.k 威胁；BlackGrays → 灰产） (p0/f.java:44-95)
  ├─ g (CodeScanner).a(ctx)        :366   ← 本地代码特征
  ├─ z (MatcherScanner).a(ctx)     :371   ← 规则匹配（t0）
  └─ b0.h(result)                  :373 → :215-297
       ├─ 组 ResultReportData：规则名/描述/包名/应用名/版本/签名/… + Build.VERSION.INCREMENTAL + SDK_INT + Build.DEVICE
       └─ q0.n.a(now, list)  → POST /virus/localResult/report  (q0/n.java:11-13)
```
- 上报失败仅 `Log.e("RealScanner","Result upload failed")`（`p0/b0.java:291-295`），不重试。
- 云扫结果类型：`CloudScanPayload.a = {Threats, BlackGrays, Unknown, Clean}`（`p0/f.java:54-95`）。
- **阈值**：源码中**没有**文件大小/耗时阈值（云扫按 t0.a 列表整批提交）；唯二开关是 `scan_params` bit3 与
  `f7380c` scanType。`未知`：服务端阈值不可见。

### 3.3 病毒库/特征库更新链路（三条独立通道）

**通道 A：云端名单（JSON，增量/全量）**
```
i0.d.j(incremental)  (i0/d.java:202-210)
  ├─ GET https://api.sec.miui.com/antivirus/viruses
  │     params: version=<antivirus_db_version.version>, updateType=incremental|global, <miuiVer 键>  (:36-45)
  │     签名盐 "94e6cec2-19dc-4bb6-823c-613f72d3ea16" 交给 h0.b.c(...)  (:203)
  ├─ 顺带 g(ctx): GET /antivirus/whitelist（同盐）→ f.a(ctx).c(json)  (:54-60, :204)
  ├─ i(json) 事务 (:102-185):
  │    ├─ 有 error_code → 直接 false（**不回滚**，但事务未开始）
  │    ├─ delete antivirus_db_version → insert(version, release_time)
  │    ├─ updateType=="global" → **delete 全表 antivirus_db**，随后逐条 insert
  │    └─ 逐条 status: new/updated → upsert；deleted → delete；键=(package, signatures, md5)
  └─ 返回 false 时上层不落库（无回滚文件，天然幂等）(:206-209)
```
**通道 B：规则/灰产库（Retrofit + Room，新栈）**
```
s0.s.j(client, gson) → Retrofit.Builder().baseUrl(q0.m.f7703a)  (s0/s.java:123-127)
  q0.m: https://virus-engine.sec.miui.com（国际版 .intl）(q0/m.java:12)
  拦截器链 (s0/s.java:108-113 顺序)：network(k) → sign(q) → i4Req(g) → i4Resp(i) → log(a)
   ├─ q0/q.java:20-22  sign = MD5(TreeMap 参数拼 "k=v&…" + "62b36629-ecb4-4725-9f33-ccb0fd158191")
   ├─ q0/q.java:40    若调用方自带 sign → IllegalArgumentException（防覆盖）
   └─ q0/g.java:29    请求头 EncryptVersion=<v0.j.d()>；请求体 AES 加密；响应体解密（v0.j.c）
  端点：/virus/blocklist /virus/allowlist /virus/feature/codeList /blackGrays/blocklist /blackGrays/feature/codeList
  落库：s0.d → EngineDatabase 的 DAO（r0.c/e/k/z/f0/h0/j0/k0/o0）(s0/d.java:140-199)
```
**通道 C：引擎二进制/特征库（SDK 内部）**
```
AvlEngine.l(bVar)  (AvlEngine.java:176-193)
  ├─ s(b())=AVLEngine.setNetworkEnabled(网络开关)
  ├─ AVLEngine.checkUpdate() → ==0 时回调 a(3,0)（无更新）
  └─ AVLEngine.update() → >0 回调 a(0,rc)，==0 回调 a(3)，<0 回调 a(2)
     对应 f0.a.b 回调 → n0.g$c$C0153c.a(updateType, errCode) → observer.x(y.e) (:407-447)
TencentEngine.l(...) → UpdateManager.check(UpdateConfig.UPDATE_FLAG_VIRUS_BASE, cb, -1L)  (TencentEngine.java:490-496)
```
- **落盘路径**：JSON 名单 → `/data/data/com.miui.guardprovider/databases/antivirus.db`、`miui_virus.db`；
  规则库 → Room `EngineDatabase`（`.../databases/`）；引擎二进制由 `com.avl`/tmsdk 自行落盘（`未知`）。
- **校验**：通道 A 用 URL 参数 `sign`（盐 MD5）；通道 B 用 `sign` 参数 + `EncryptVersion` + AES；通道 C 由 SDK 内部校验。
- **回滚**：**没有显式回滚**。A 靠"先删版本行 + 全量时清表 + 单条删/插"实现幂等；失败时事务不提交（`setTransactionSuccessful` 未达）。

### 3.4 URL 扫描链路（`SecurityService` urlscan binder → 判定）

```
bind action "com.miui.guardprovider.action.urlscan" → SecurityService.b (IBinder)  (SecurityService.java:315-317)
  ├─ H(url, engineName)  y.b  → j.a().b(url, engineName)   (:280-283)
  ├─ t(url)              y.b  → j.a().c(url)               (:286-293)  ← 更新 URL 规则库
  └─ n0.j (UrlScanManager)  (n0/j.java)
       ├─ d(url, type, engineName):  `str.contains(bVar.f3547b)` 用**引擎名子串**选引擎  (:44-79，选引擎 :59-66)
       │     ← 注意：若 engineName 为空/不匹配 → 返回 -2；匹配到多个则取枚举顺序第一个
       ├─ 引擎未初始化 → 异步 e(cb) 后再调；已初始化 → 直接 m(type, url)  (:69-76)
       ├─ b(url, engine) → m(1, url)    (:87-88)
       └─ c(url)         → m(2, url)    (:91-92)
f0.a.m(int,String) 默认 -1  (f0/a.java:95-97)
  └─ AvlEngine.m: 1 → AVLEngine.scanURL(url)；2 → AVLEngine.updateURLRule() + 持久化 avl_url_sdk_version  (AvlEngine.java:195-211)
       └─ com.avl.engine.urldetector.UrlDetector: System.loadLibrary("urldetectorsys") + setLibPath("libavlurl.so") + loadURL(rulePath)  (UrlDetector.java:80-110)
```
- **判定结果**为 int（`scanURL` 的 native 返回值），Java 侧只透传，不解释语义（`未知`：具体码表在 native）。
- `updateURLRule()` 成功后写 `w0.f` 键 `avl_url_sdk_version`（`AvlEngine.java:207`）。

### 3.5 WiFi 检测链路（四个方法各自做什么）

| binder（`y.a`） | 服务端 | `n0.k` | `f0.a.d` | 引擎实现 |
|---|---|---|---|---|
| `B(state, cb)` | `SecurityService.java:48-54` | `e()` (:102) | `NETWORKSTATE` | `TencentEngine.n()` → `i(...)` → tmsdk `detectNetworkState()`（`TencentEngine.java:60-130`） |
| `D(ssid, cb)` | `:67-73` | `d()` (:94) | `DNSANDPHISHING` | `detectDnsAndPhishing(listener, 0L)` |
| `O(ssid, cb, arg)` | `:122-128` | `c()` (:90) | `ARP` | `detectARPNoRoot(15000L)` → 失败再 `detectSecurity(scanResult)` |
| `Q(ssid, cb)` | `:137-143` | `f()` (:98) | `SECURITY` | `detectSecurity(...)` |
| （`A()`） | `:38-45` | — | — | `c.h(ctx)` 触发一次全量应用检测 |

- 统一入口 `n0.k.b()`：按引擎名子串选引擎 → 若未初始化先 `e(cb)` 异步初始化再 `n(tag, cb, arg)`；否则直接 `n()`，
  **两者都返回 -1**（`n0/k.java:75-88`）。回调通过 `y.d`（AIDL）回传 int。
- 所有 WiFi 方法在用户锁屏时直接返回 0（`SecurityService.c()` = `!androidx.core.os.a.a(this)`，`:297-299`）。

### 3.6 全盘/应用扫描与"灰产/风险应用"判定（`getRiskAppData` 数据来源）

```
ContentProvider.call("getRiskAppData") → GuardProvider.b()  (GuardProvider.java:103-127, :334)
  ├─ getInstalledPackages(0) 全量
  ├─ listE = c0.b.f(ctx).e()   ← 读 risk_gray_result.db 全部灰产缓存 (c0/b.java:103)
  ├─ 跳过系统应用 ((flags & 1) != 0)
  ├─ c(pkg, listE): packageName + versionCode **双键**匹配 → JSON{packageName,versionCode,type,signaturesMd5} (:129-157)
  ├─ 首次命中 → d.g(this.f1748b)（i0.d 白名单刷新）+ i0.f 白名单过滤 (:113-119)
  └─ bundle.putString("riskData", JSONArray)
```
- **数据来源链**：`risk_gray_result_db` ← `c0.d` 写入 ← 扫描结论 `y.f`（level∈{2,3,6} 视为风险）
  ← `n0.g` 合并各引擎结果（`n0/g.java:335-361` 对 2/3/6 触发白名单复核）。
- **缓存有效期**：`w0.f` 键 `virus_cache_validty_perod`（默认 3 天），过期行删除（`c0/b.java:69`）。
- **反诈独立通道**：`g0.e` 直接对 `j0` 库命中的应用造 `virusLevel=6` 的结果（`g0/e.java:20-34`），
  其数据源是云端下发 JSON 写 `mi_detect_app_db`（`j0/a.java:37-58`，只收 `basisOf==2`）。
- **全盘**：`n0.g.v(ctx)` 用 MediaStore 查 `_data LIKE '%.apk'`（`n0/g.java:1195-1226`），只覆盖外部存储可见 apk。

## 4. 数据与常量

### 4.1 文件路径 / 数据库

| 路径/库名 | 用途 | 位置 |
|---|---|---|
| `.../databases/risk_gray_result.db`（表 `risk_gray_result_db`） | 灰产应用缓存，TTL 3 天 | `c0/a.java:9-12` |
| `.../databases/scan_result.db`（v2，表 `scan_result_db`） | 单文件扫描结果缓存 | `c0/c.java:8-13` |
| `.../databases/miui_virus.db`（v5） | `virus_white_db` + `t_virus_white_list` | `w0/h.java:9-19` |
| `.../databases/antivirus.db` | `antivirus_db` + `antivirus_db_version`（云名单） | `i0/b.java`（t-core） |
| Room `EngineDatabase`（8 表） | 规则/灰产/证书/安装器/版本 | `r0/*`、`com/miui/guardprovider/sdk/android/db/EngineDatabase` |
| `mi_detect_app_db` | 反诈应用库 | `j0/a.java:37-58` |
| `.../cache/copy.apk` | `content://` 入参的临时副本 | `GuardProvider.java:213` |
| `getCacheDir()` / `getFilesDir()` / `noBackupFilesDir` | Room 与临时文件基址 | `h`、`i` 包（Room）；`GuardProvider.java:213` |
| MediaStore `external` `_data LIKE '%.apk'` | 全盘 apk 枚举 | `n0/g.java:1199` |
| `/data/data/<pkg>/databases/…` | 上述所有 sqlite | 同上 |

### 4.2 Intent / action / 权限 / 组件

| 名称 | 用途 | 位置 |
|---|---|---|
| `com.miui.guardprovider.action.antivirusservice` | 绑定 `y.a`（杀毒主 Binder） | `SecurityService.java:312` |
| `com.miui.guardprovider.action.urlscan` | 绑定 `y.b`（URL 扫描 Binder） | `SecurityService.java:315` |
| `com.miui.guardprovider.permission.Guard_Provider` | system\|signature 保护 provider+service | Manifest（简报） |
| `content://guard/virus` | 病毒白名单 CRUD | `x0/a.java:8`、`GuardProvider.java:90` |
| `content://guard/{engine_info_list,sync_scan,install_scan,support_incremental}` | 见简报 | `GuardProvider.java:88-92` |
| `android.permission.READ_EXTERNAL_STORAGE` | `parseApk` 前动态申请（无权限且非 /data 路径时） | `GuardProvider.java:163-165`、`w0` 字面量 |
| `androidx.room.IMultiInstanceInvalidationService` | Room 多进程失效 | `e` 包字面量 |

### 4.3 SQL 表与字段（源码可见部分）

| 表 | 字段 | 位置 |
|---|---|---|
| `scan_result_db` | `_id, path, packageName, virusName, virusDescription, engineName, versionName, versionCode, virusLevel, extras, sign, time` | `c0/c.java:13` |
| `risk_gray_result_db` | `_id, packageName, versionCode, signatures, type, time` | `c0/a.java:12` |
| `virus_white_db` | `_id, package, path, version_code, size, engine, local_version, extra` | `w0/h.java:19`（含 4 次 ALTER 迁移） |
| `t_virus_white_list` | `_id, scan_result_type, scan_item_type, app_label, desc, dir_path, pkg_name, virus_name, virus_md5` | `w0/h.java:15` |
| `antivirus_db` | `package, version, signatures, md5, type, virus_name, strategy, descType, description` | `i0/d.java:146-164` |
| `antivirus_db_version` | `version, release_time` | `i0/d.java:66,117-120` |
| `mi_detect_app_db` | `package, signatures`（+ 云端 version 被丢弃） | `j0/a.java:44-56` |
| Room 8 表 | `trust_certs/trust_installers/innocent_package/characteristics/guilty_package/black_gray_chs/black_gray_app/version` | 见 `20_cloud.md`、`r0/*` |

### 4.4 密钥 / 盐 / IV

| 值 | 用途 | 位置 |
|---|---|---|
| `94e6cec2-19dc-4bb6-823c-613f72d3ea16` | 旧栈名单接口签名盐（A 通道） | `i0/d.java:55,203` |
| `6988567a-4220-4b51-bc2d-ccdec27a74a1` | 旧栈第二盐 | `j0` 包字面量（`20_cloud.md` 已列） |
| `62b36629-ecb4-4725-9f33-ccb0fd158191` | 新栈 `sign` 参数盐（B 通道） | `q0/q.java:22` |
| `wcw1xbplx7aijwqr` | AES 密钥（`20_cloud.md`） | `v0`（t-cloud 已核） |
| `AES/CBC/PKCS5Padding`、`AES/ECB/PKCS5Padding` | 请求/响应体加解密 | `v0` 包字面量 |
| `manifestMFSha256` / `signaturesSha256` | 规则库字段名（完整性） | `v0`、`r0`、`t0` 字面量 |

> `未知`：AES 的 IV 未在本层字面量中出现（可能在 `v0.j` 内以常量拼接），需要 t-core/`v0` 细读确认。

### 4.5 版本 / 协议 / magic

| 项 | 值 | 位置 |
|---|---|---|
| 病毒库协议版本字段 | `version`、`releaseTime`、`updateType∈{incremental,global}`、`viruses[]` | `i0/d.java:115-127` |
| 云扫请求字段 | `androidVersion, miuiVersion, timestamp, uuid, versionType, scanType` | `p0/f.java:289-295` |
| `scan_params` 位域 | bit0/1=威胁模式、bit2=黑灰产、**bit3=禁用云扫**，默认 3 | `p0/f.java:370-379`（t-cloud 已核） |
| `EncryptVersion` 头 | 加密版本协商 | `q0/g.java:27` |
| 引擎枚举 | `MI("Mi",100) / AVL("AVL",11) / TENCENT("TENCENT",10)` | `f0/b.java`（t-core） |
| 结果等级 `virusLevel` | 1=安全/白、2/3=威胁、6=反诈；4/5 存在但语义未定 | `GuardProvider.java:260`、`g0/e.java:29-34`、`n0/g.java:1015-1082` |
| 云状态枚举 | `deleted`、`online` | `u0/g.java:5-6` |
| `extra` 标记 | `"AntiDefraud:danger;"` | `g0/e.java:29` |
| 引擎名持久化键 | `cloud_engine`、`load_cloud_engine_time`、`antivirus_is_init`、`avl_url_sdk_version`、`virus_cache_validty_perod` | `x/a.java`、`AvlEngine.java:207`、`c0/b.java:105` |

## 5. 高危与可疑行为清单

| 类别 | 行为 | 位置 | 触发条件 |
|---|---|---|---|
| 动态库加载 | `libflamingo`（Rust 规则引擎）、`libavlurl`+`liburldetectorsys`（URL）、`libavlm/libavlasys`、`libTmsdk-2.0.13-mfr`、`libtmf-gm`、`libxy`、`libdce-1.1.21-mfr`、`libturingvd`、`libxcrash*` | `com/miui/guardprovider/natives/RustSupport.java:6`、`UrlDetector.java:80-87`、`com/avl/engine/a/c.java:223`、`com/tencent/tcuser/util/Encode.java:43` | 首次扫描/URL 检测/引擎初始化 |
| 反射 | OAID/UDID/VAID/AAID：`Class.forName("com.android.id.impl.IdProviderImpl")` + `getMethod(...).invoke` | `v/a.java:30-34, 42-57` | 埋点/设备标识初始化 |
| 反射 | MIUI 属性：`isMiuiLiteRom`、`ro.product.mod_device`、`getDeclaredField` | `c5/a.java`、`d5/a.java`、`t/a.java` | 启动时判断 ROM/机型 |
| 反射 | Gson/OkHttp/kotlin-reflect 大量 `setAccessible`、`getDeclaredMethod` | `o`、`q`、`p5`、`q5`、`r2`、`f2` 包 | 库正常行为，非业务 |
| 命令执行 | `Runtime.exec`/`ProcessBuilder` 仅出现在 `com`、`tmsdkobf`、`z5`（JEXL 内省） | 见 §0 扫描 | 非本层业务路径 |
| 文件遍历/删除 | MediaStore 全盘 apk 枚举；扫描后 `Files.deleteIfExists(copy.apk)`；结果库过期删除 | `n0/g.java:1199`、`GuardProvider.java:221,239,253`、`c0/b.java:100` | 全盘扫描/每次同步扫描/定时清理 |
| 静默安装 | `AVLA.installPackage(path, 0|1, str2, str3)` native，`com/avl/engine/l/b/c.java:27,80` 调用 | `com/avl/engine/security/AVLA.java:83` | 安装拦截/包解析（`AVL` 引擎启用时） |
| 读取他人应用数据 | `parseApk(path)` 解析任意 apk（含 `/data` 路径），返回 `pkgInfo`+icon+label；`content://` 入参会复制到自身 cache | `GuardProvider.java:159-190, 210-216` | 调用方持有 provider 权限（system\|signature） |
| 绕过权限 | `parseApk` 在无 READ_EXTERNAL_STORAGE 时先 `e.a(context)` 触发动态授权，再继续 | `GuardProvider.java:163-165` | 外部传入非 /data 路径 |
| root/模拟器检测 | **本层未发现**（`root=`/`emu=` 命中为 0；`tmsdkobf` 内部有） | 全层扫描 | — |
| 隐私：OAID | 见上（`v`） | `v/a.java` | 埋点 |
| 隐私：已装应用列表 | `getInstalledPackages(0)`（`getRiskAppData`）、`getInstalledPackages(64)`（增量扫描）、`queryIntentActivities` | `GuardProvider.java:107`、`n0/g.java:689`、`j0/a.java` | 每次 `getRiskAppData`/增量扫描 |
| 隐私：设备指纹 | `Build.VERSION.INCREMENTAL`、`SDK_INT`、`Build.DEVICE` 随扫描结果上报 | `p0/b0.java:254-259, 272-277` | 每次上报 |
| 隐私：IMEI/MAC/位置/通讯录/SMS/剪贴板 | **本层未发现**（命中集中在 `com` 与 tmsdk，不属本层） | 全层扫描 | — |
| 反调试 | 本层未发现；`xcrash` 有 crash 捕获 | — | — |

## 6. 未解决问题

1. `a6/x5/y5/z5/v5` 的 JEXL3 归属已由字面量确认，但**谁在运行时喂表达式**未闭环：全树只有 `com` 包 import `v5` 5 次，
   `p0`/`t0` 均未直接引用。需要 smali 交叉引用确认是否由 `libflamingo` 之外的 Java 规则路径使用。
2. `virusLevel` 4/5 的语义、以及 `Type` 列在 `sync_scan` 返回中的取值集合（`未知`，需 `t0.d`/`t0.m` 细读）。
3. URL `scanURL` 的 int 码表在 native（`libavlurl.so`），Java 侧无映射。
4. 通道 C（引擎二进制更新）的下载 URL/落盘路径在 `com.avl`/tmsdk 内部，本层不可见。
5. `d6` 包（Gson `$Gson$Types` 与 OkHttp 混装）与 `l0`（业务 pipeline + `kotlin.io`）的**逐类**归属边界。
6. 约 20 个零入度小包（见 2.4）未定性；需要 smali 级别 `invoke` 图才能判定。

## 6.5 补遗（lead 点名的 4 个空白）

### 6.5.1 二维码：**不存在**（负结论，附搜索边界）

- 搜 `_analysis/src/sources/**/*.java` 全树正则 `com\.miui\.qr|QRCode|qrcode|MultiFormat|BarcodeFormat` → **0 命中**。
- 搜 `_analysis/apktool/smali/**`：目录/文件名含 `qr` → **0**；smali 字符串含 `miui/qr` → **0**。
- 结论（观测）：本 APK 的可执行代码里**没有二维码扫描/生成实现**，`com.miui.qr` 若存在于 dex 清单，也只是
  不可达残留或仅出现在资源/混淆映射表，不构成功能面。`未知`：是否存在通过 `Intent` 拉起外部扫码应用的行为（未做 intent 全表）。

### 6.5.2 安装拦截 / 静默安装

```
[调用方] query(content://guard/install_scan, args=[path, bool, engineName])
  GuardProvider.query case 1008 (GuardProvider.java:403-415)
    → args.length < 3 时返回 null（**必须**给 engineName）
    → f(path, parseBoolean(args[1]), args[2]) → g.z().L(..., 2, engineName)  (:415, :249)
```
- 与 `sync_scan` 完全同一条 `L()` 路径，差别只有两点：**必须带 engineName**、`selectionArgs[1]` 语义由调用方解释（是否允许云扫，透传为 `z6`）。
- `Install` 语义不在 Java 侧判定，而在 AVL 引擎：`AVLA.java:83` 声明
  `public native int installPackage(String path, int mode, String str2, String str3)`；
  调用点 `com/avl/engine/l/b/c.java:27`（mode=0，返回值 != 1 视为失败）与 `:80`（mode=1）。
  ⇒ **安装拦截的判断与"放行/拦截"动作在 native `libavlm.so`**；Java 只把 apk 路径交进去。
- **静默安装**：本层与 `com` 层均未出现 `PackageInstaller`/`pm install`/`INSTALL_PACKAGES` 字符串（grep 0 命中）；
  唯一"装包"入口就是上面的 native `installPackage`，其权限/实现依赖调用方进程（system uid）。
- 结论（推断）：GuardProvider 自身**不执行**静默安装，只提供"扫描 apk → 返回结论"与 AVL 的 native 安装钩子给系统侧调用。

### 6.5.3 更新/升级链路补遗（三通道对照）

| | 通道 A 名单 | 通道 B 规则库 | 通道 C 引擎/特征库 |
|---|---|---|---|
| 触发 | `SecurityService.E(type∈{1,2,3})`；`i0.d.j(incremental)`（`i0/d.java:202`）；`GuardProvider` `support_incremental` 前置 `i0.h.h()`（`GuardProvider.java:406`） | 同左，`s0` 客户端在 `MiEngineNG` 初始化后拉取 | `n0.g.N(cb)` 建更新任务（`n0/g.java:1305-1328`）→ `c.s()` 遍历引擎 `l(cb)`（`n0/g.java:630-646`） |
| 传输 | `h0.b.c(ctx,url,params,salt)`，GET+签名参数 | Retrofit + OkHttp 拦截器（sign/EncryptVersion/AES） | SDK 内部（`AVLEngine.checkUpdate/update`、tmsdk `UpdateManager`） |
| 地址 | `https://api.sec.miui.com/antivirus/{viruses,whitelist}` | `https://virus-engine.sec.miui.com` + `/virus/*`、`/blackGrays/*` | 由 `q0/m` 同域或 SDK 内置（`未知`） |
| 校验 | `sign`=MD5(参数+盐 `94e6cec2…`) | `sign`=MD5(排序参数+盐 `62b36629…`) + `EncryptVersion` + 响应 AES 解密 | SDK 私有（`libavlm`） |
| 落盘 | `antivirus.db`（`antivirus_db`/`antivirus_db_version`） | Room `EngineDatabase` 8 表 | `assets/avlsdk`（1.5 MB，随包）+ 运行时更新目录（`未知`） |
| 失败/回滚 | 事务不提交；全量时先清表后逐条插入（无备份，靠重拉恢复） | 同上（Room 事务） | `AvlEngine.l`: rc>0→`a(0,rc)`；rc==0→`a(3,0)`；rc<0→`a(2,rc)`（`AvlEngine.java:176-193`）；无回滚，靠下次 `checkUpdate` |
| 进度回调 | — | — | `f0.a.b`: `onCheckStarted/onUpdateStarted/a(updateType,errCode)/onUpdateCanceled` → `n0.g$c$C0153c`（`n0/g.java:407-479`）→ `y.c.x(y.e)`/`y.c.K(id)` |

- 命名对应（观测）：`f0.a.d` = `{UNKNOWN, NETWORKSTATE, DNSANDPHISHING, ARP, SECURITY}`（`f0/a.java:49-55`），
  与通道 C 的 updateType 不是一套编码；`n0.g$c$C0153c.a(i6,i7)` 的 i6 直接来自引擎回调，语义由引擎定（`未知`）。
- **增量**在 A 通道是服务端 diff（`status: new/updated/deleted`，`i0/d.java:139-168`），
  在 B 通道是 `version` 参数 + Room 全量替换；两者都不是"二进制差分包"。

### 6.5.4 WiFi 宿主侧参数流（补齐 t-vendor 的 TMSDK 侧）

```
y.a.B/D/O/Q(ssid, observer[, arg])
  → SecurityService 锁屏判定 (:48-143)
  → n0.k.{e,d,c,f}(ssid, observer[, arg])
      → b(engineName, d, observer, arg): engineName 子串匹配 f0.b 枚举 (:57-73)
      → engine 未 init? 先 e(cb) 再 n(observer, type, arg)  (:75-88)
  → f0.a.n(y.d, f0.a.d, Object)  抽象 (:99)
      → TencentEngine.n(): Log + i(new a(type, observer, arg))  (TencentEngine.java:500-505)
          → 异步 a: detectNetworkState() → detectARPNoRoot(15000L) → detectSecurity(scanResult) → detectDnsAndPhishing(listener, 0L)
          → 回调 observer.v(type, ret)（`dVar` = y.d）
```
- 参数：`ssid/state` 为 String，`arg` 为 Object（ARP 分支里是 scanResult 或超时值）；超时 **15000 ms** 硬编码（`TencentEngine.java:102`）。
- `n0.k.b()` 返回值恒为 -1（异步），仅表示"已受理"。

### 6.5.5 资产与 native 清单（本轮实测）

| 文件 | 大小 | 关联 |
|---|---|---|
| `lib/arm64-v8a/libavlm.so` | 2,304,352 | AVL 主引擎（`AVLA.installPackage`/scan native） |
| `libflamingo.so` | 1,506,088 | Rust 规则引擎（`RustSupport`） |
| `libams-1.3.0-mfr.so` | 744,392 | tmsdk |
| `libavlurl.so` | 629,976 | URL 规则库/检测（`UrlDetector.setLibPath`） |
| `libTmsdk-2.0.13-mfr.so` | 370,088 | 腾讯 TMSDK |
| `libturingvd.so` | 240,448 | 设备指纹（tmsdk） |
| `libdce-1.1.21-mfr.so` | 202,976 | `QFile` 加载 |
| `libtmf-gm.so` | 177,920 | 国密（SM2/SM4） |
| `libavlasys.so` | 89,144 | AVL 辅助 |
| `liburldetectorsys.so` | 14,408 | URL 检测 JNI 桥 |
| `assets/avlsdk` | 1,566,355 | AVL SDK 数据/配置 |
| `assets/70003.amf` | 680,928 | tmsdk 模型/规则 |
| `assets/mdetector` | 136,636 | 检测器数据 |
| `assets/40706.dat` / `licence.conf` / `licence1.conf` | 9,368 / 392 / 368 | AVL 授权与码表 |

## 7. REA 证据

一手证据为 JADX 源码 + 全树类索引；对 3 个最关键类做了 REA（`inspect_android_class`）证据级复核：

| 类 | evidence_id | 复核结论 |
|---|---|---|
| `p0.b0`（RealScanner） | `ev_37be64d9bf4ee3337e3f814541866a08ced5bb6e504bf369df5e34e701b0ef10` | 9 方法/6 字段；依赖正是 `p0.g`+`p0.f`+`p0.z`+`q0.n`（云扫/代码/匹配/上报四件套），与 §3.2 管线一致；`h(t0.a,k1.d)` 即上报方法 |
| `q0.q`（签名拦截器） | `ev_a6ebd88f286d3c6591c70d68e8de3905253145dabb8ea438c7c851bd69072d35` | 仅 3 方法；`b(Map):String` = 计算 sign，`a(g5.w$a):g5.d0` = OkHttp 拦截，证实"签名拦截器"而非普通业务类 |
| `i0.d`（名单同步） | `ev_b2d12fbd6b6549d99696d8a7218e115a8d35675f70b019b5e8a3f8bda9f0a23d` | 11 方法；`e(boolean):Map`（增量/全量参数）、`i(String):boolean`（解析入库）、`g(Context):void`（白名单）三件套与 §3.3 通道 A 完全对应 |

未复核但建议后续补的证据点：`AvlEngine.m`（URL native 桥）、`s0.s`（MiEngineNG 客户端装配）、`c0.d`（结果缓存写入）。


