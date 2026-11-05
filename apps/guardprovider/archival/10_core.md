> **归档警示（已纠错）**：这是原分析材料的脱敏归档，不能代替 [最终复核报告](../REPORT.md)。原文关于“明文 IMEI 列表”“默认配置即实际上传”、绝对覆盖率及未知 native 行为的部分结论不成立。IMEI/IMSI 的相关 DAU 列表经过 MD5，仍受区域/权限/联网门控。证书过期也不能单独推出密文无法解密。请先阅读最终报告与其中纠错表。本文删除本地身份路径，原有文件行号引用可能指未纳入发布的完整分析树。

# 应用主控层（进程初始化 / IPC 门面 / 扫描编排 / 引擎适配）

> 承接 `00_brief.md`。本文覆盖范围（按 Lead 收敛后的分工）：
> `com/miui/guardprovider/**`（core / engine / manager / natives / sdk 之外的桥接点）
> \+ `n0` `c0` `i0` `d0` `w0` `x` `f0` `y` 八个包
> \+ 与 native 直接交互的点（`p0/y.java`、`natives/RustSupport`）。
> 混淆包（`a6`/`s4`/`c4`/`l2`/`x4`/`f2`/`o2`/`u2` …）的逐包语义映射由 **t-obf** 负责（见 `_obf_index.tsv` 与 t-obf 报告），本文只在必要处引用其结论。
>
> 陈述分级：**【观测】**= 源码/字节直接可见；**【推断】**= 有依据的语义归纳；**【未知】**= 未覆盖。

---

## 1. 职责概述

`com.miui.guardprovider` 是 MIUI 安全中心（`com.miui.securitycenter`）的**杀毒后端进程**，本身没有 UI、没有 Activity、没有开机接收器。它只暴露两个出口：

| 出口 | 类型 | 权限 |
|---|---|---|
| `com.miui.guardprovider.GuardProvider` | ContentProvider，authority `guard`，`android:exported="true"` | `com.miui.guardprovider.permission.Guard_Provider`（signature\|privileged） |
| `com.miui.guardprovider.manager.SecurityService` | Service，`exported="true"`，双 action | 同上 |

主控层做四件事：

1. **进程/状态初始化**：Application 只做 SharedPreferences 预热 + 用户解锁等待；所有引擎 SDK（AVL / TMSDK）、OneTrack、数据库都是**懒初始化**，由第一次扫描触发（§4.1）。
2. **IPC 门面**：把 ContentProvider 的 5 个 URI + 2 个 `call()` 方法、Service 的 2 个 AIDL 接口（24 + 2 个事务码）翻译成对 `n0.g` 门面的调用（§4.2、§4.4）。
3. **扫描编排**：`n0.g`（`n0/g.java`，1450 行）单例 `g.z()` 持有任务表、按启用引擎集合**每个引擎一个线程**并发扫描、按文件路径做多引擎结果计数与合并、限流最多 10 个并发任务、2 分钟超时、支持取消与进程死亡清理（§4.3）。
4. **引擎适配**：`f0.a` 抽象契约 + 4 个实现（`Mi`/`MiEngineNG` 走 Kotlin+Rust、`AVL` 走 `libavlm.so`、`TENCENT` 走 TMSDK），统一把各自结果转成 `y.f`（`VirusInfo`）模型（§4.5）。

配套三个本地判定库（§4.6）：
- `miui_antivirus.db`（服务端下发的**病毒/灰产库**）
- `mi_engine_white_virus.db`（**白名单**，`getRiskAppData` 用它做豁免）
- `risk_gray_result.db`（**灰产扫描结果缓存**，`getRiskAppData` 的数据源）

---

## 2. 组件与文件清单

### 2.1 `com.miui.guardprovider.*`（core / engine / manager / natives）

| 文件 | 类 | 作用 |
|---|---|---|
| `com/miui/guardprovider/GuardApplication.java` | `GuardApplication` | 进程入口；`attachBaseContext` 预热 SP 或注册 `USER_UNLOCKED` 接收器；`onCreate` → Hilt（空）+ `w.c.a()`（空方法）；静态 `f1743d` 全局 Context |
| `com/miui/guardprovider/b.java` | `b`（包内抽象） | `extends Application implements d1.b`；`onCreate` 先跑 Hilt 注入 `c()`，再 `super.onCreate()` |
| `com/miui/guardprovider/a.java` | `a` / `a$a` / `a$b` | Hilt 生成组件；`b` 实现 `w.d.a(GuardApplication)` **方法体为空** → 注入无任何副作用 |
| `com/miui/guardprovider/GuardProvider.java` | `GuardProvider` + 内部 `a`(观察者)、`b`(结果容器) | ContentProvider：`query` 5 URI、`call` 2 方法、`insert/delete` 维护 `t_virus_white_list` |
| `com/miui/guardprovider/manager/SecurityService.java` | `SecurityService` + `a`、`b` | AIDL 服务：`onBind` 按 action 返回 `y.a`（24 事务）或 `y.b`（2 事务） |
| `com/miui/guardprovider/engine/antiy/AvlEngine.java` | `AvlEngine` | AVL 适配器（`com.avl.engine.*`） |
| `com/miui/guardprovider/engine/mi/MiEngine.java` | `MiEngine` | 旧 MI 引擎适配器（`i0.a` 数据源 + `g0.e` 扫描） |
| `com/miui/guardprovider/engine/mi/MiEngineNG.java` | `MiEngineNG` | 新 MI 引擎适配器（Kotlin，委托 `p0.n` → Flamingo/Rust） |
| `com/miui/guardprovider/engine/tencent/TencentEngine.java` | `TencentEngine` | 腾讯 TMSDK 适配器（QScannerV2 / UpdateManager / WifiDetect） |
| `com/miui/guardprovider/natives/RustSupport.java` | `RustSupport` | `System.loadLibrary("flamingo")` + 3 个 native 方法 |
| `com/miui/guardprovider/natives/RustException.java` | `RustException` | Kotlin `Exception` 子类，仅供 `p0/y.java` 捕获 |
| `com/miui/guardprovider/sdk/android/db/*` | `EngineDatabase` / `EngineDatabase_Impl` | Room `flamingo.db`（8 表，版本 2）——详见 t-cloud 报告 |
| `com/miui/guardprovider/sdk/android/pojo/**` | 28 个 pojo | Flamingo SDK 数据模型（`ScanTarget`/`Condition`/`StringItem`/`GuiltyPackageRecord`/`TrustCertRecord`/`networkdomain.*`） |

### 2.2 八个受管包

| 包 | 主要类 | 作用 |
|---|---|---|
| `n0` | `g`(1450)、`k`(105)、`j`(94)、`a`(32)、`h`(32)、`i`(17) | `g`=AntivirusManager 扫描门面；`k`/`j`=UrlScanManager / WifiDetect 分发；`a`=固定 6 线程池；`h`=结果广播；`i`=引擎反射实例化 |
| `c0` | `d`(289)、`b`(164)、`c`(60)、`a`(45) | `d`=ScanResultDBManager(`scan_result.db`)；`b`=GrayResultDBManager(`risk_gray_result.db`)；`c`/`a`=两个 SQLiteOpenHelper |
| `i0` | `l`(417)、`d`(211)、`f`(140)、`h`(93)、`g`(53)、`e`(51)、`b`(26)、`m`(26)、`a`/`c`/`i`/`j` | `l`=病毒库查询；`d`=白名单/病毒库网络同步；`f`=白名单判定（`e(pkg)`/`d(pkg,sig)`/`f(path)`）；`h`=包/签名工具；`g`=签名提取；`e`/`b`=DB helper；`m`=结果记录；`a`/`j`/`i`=接口 |
| `d0` | `a`(13)、`b`(37)、`c`(12)、`d`(27) | `d`=`android.os.SystemProperties` 反射；`b`=`MiuiSettings$SettingsCloudData` / `MiuiSettings$Privacy` 反射（云控）；`c`=通用反射 invoke；`a`=`AppOpsUtils.isXOptMode` |
| `w0` | `f`(85)、`d`(128)、`h`(94)、`i`(29)、`a`(26)、`b`(26)、`c`(11)、`e`(20)、`g`(49) | `f`=`SharedPreferences("GuardProvider")` 包装；`d`=PackageParser/归档包解析；`h`+`i`=`miui_virus.db` v5；`a`=文件 MD5；`b`=deviceId/UUID；`c`=日志空实现；`e`=跨应用"授权"；`g`=解锁判断/归档 PackageInfo |
| `x` | `a`(197)、`b`(199) | `a`=EngineSettingUtils（引擎启用初值/云引擎轮换）；`b`=VirusEngineCloudControl（云控拉取引擎列表与权重） |
| `f0` | `a`(102)、`b`(61) | `a`=引擎抽象基类；`b`=**引擎注册表枚举**（3 个常量） |
| `y` | `a`(226)、`c`(289)、`f`(163)、`d`(61)、`b`(48)、`e`(51) | AIDL：`a`=`IAntiVirusServer`(24)、`c`=`IVirusObserver`(13)、`b`=`IURLScanServer`(2)、`d`=`IWifiDetectObserver`(1)；`f`=`VirusInfo`（Parcelable 结果模型）；`e`=`EngineUpdateInfo` |

### 2.3 直接协作的其它包（仅列本文用到的）

| 包 | 类 | 作用 |
|---|---|---|
| `p0` | `y`(132) | `Callable`，调用 `RustSupport.matches/search` 做特征码匹配（**唯一 native 调用点**） |
| `p0` | `n`/`g`/`f`/`z`/`b0`/`c` | Flamingo 扫描管线（详见 t-cloud 报告，本文只引用 `scan_params` 位域） |
| `b0` | `a` | 单任务状态容器（引擎实例、进度、云扫开关、extras） |
| `o0` | `a` | `IVirusObserver` 全空实现基类，供 `GuardProvider` 与增量扫描复用 |
| `j0` | `c`/`a`/`b` | AntiDefraudAppManager（`/detect/app` 上传+拉取） |
| `g0` | `e` | MiEngineUtils（旧 MI 引擎扫描实现） |
| `h0` | `b`/`a` | NetworkApiHelper（HttpURLConnection + MD5 签名）、mix-scan 探测 |
| `z` | `a`/`b`/`c` | OneTrackManager（埋点，appId `2882303761517522561`） |
| `v` | `a` | IdentifierManager（OAID/UDID/VAID/AAID 反射） |
| `u` | `a` | `com.miui.analytics.ITrack` 客户端桩 |
| `d1` / `w` / `e0` / `a1` | — | Hilt/Dagger 生成物与接口（运行期无副作用） |

### 2.4 混淆包 → 语义映射（**范围受限版**）

> 全量 3231 类 / 约 1300 个混淆文件的逐包判定由 t-obf 产出（`_obf_index.tsv`）。下表只收录本文论证过程中**必须引用**的包，供后续成员交叉核对。

| 包 | 文件数 | 语义判定 | 依据 |
|---|---|---|---|
| `n0` | 6 | AntivirusManager / UrlScanManager 门面 | Log tag `AntivirusManager`(g)、`UrlScanManager`(j) |
| `c0` | 4 | 灰产/扫描结果 DB 管理层 | Log tag `GrayResultDBManager`(b)、`ScanResultDBManager`(d) |
| `i0` | 12 | 病毒库/白名单/包工具 | Log tag `MiEngine`(l)、`PackageUtils`(h)、`MiWhiteVirusDBHelper`(e) |
| `d0` | 4 | MiuiSettings / SystemProperties 反射兼容层 | Log tag `MiuiSettingsCompat`(b)、`DeviceUtils`? 无；类名反射串 |
| `w0` | 9 | 通用工具（SP/包解析/MD5/DB） | Log tag `GuardProvider`、`MD5`、`PackageUtils` |
| `x` | 2 | 引擎启用策略 + 云控 | Log tag `EngineSettingUtils`、`VirusEngineCloudControl` |
| `f0` | 2 | 引擎抽象 + 注册表枚举 | 类结构 + REA `ev_80d5f9…` |
| `y` | 6 | AIDL 接口 + Parcelable 结果模型 | `onTransact` 中 `enforceInterface("com.miui.guardprovider.aidl.*")` |
| `o0` | 1 | `IVirusObserver` 空实现基类 | `extends c.a` |
| `b0` | 1 | 任务状态容器 | `new b0.a(aVar)` 唯一构造点 `n0/g.java:512` |
| `g0` | 1 | MiEngineUtils（旧 MI 扫描） | Log tag `MiEngineUtils` |
| `h0` | 2 | 网络 API 助手 + mix-scan 探测 | Log tag `NetworkApiHelper`；读 `securitycenter.support.mix.scan` |
| `j0` | 3 | 反诈骗云检测（`/detect/app`） | Log tag `AntiDefraudAppManager` |
| `z` | 3 | OneTrack 埋点封装 | Log tag `OneTrackManager`；`OneTrack.createInstance` |
| `v` | 1 | 设备标识反射（OAID 等） | Log tag `IdentifierManager`；反射 `com.android.id.impl.IdProviderImpl` |
| `u` | 1 | `com.miui.analytics.ITrack` 客户端桩 | `writeInterfaceToken("com.miui.analytics.ITrack")` |
| `p0` | 13 | Flamingo 扫描管线（Code/Cloud/Matcher/Real Scanner + WhiteFilter + MiEngineNG façade） | Log tag `CodeScanner`/`CloudScanner`/`MatcherScanner`/`RealScanner`/`WhiteFilter`/`MiEngineNG` |
| `w1` | 34 | Kotlin `kotlin.jvm.internal`（`w1.l`=`Intrinsics`，`w1.n`=`Lambda`） | `w1.l.f(Object,String)` 等签名 |
| `v1` | 23 | Kotlin `kotlin.jvm.functions`（`Function0..N`） | 全部 6 行接口声明 |
| `g1` | 27 | Kotlin `Result`/`Unit`（`g1.z.f4408a`=`Unit.INSTANCE`） | `MiEngineNG` 返回 `z.f4408a` |
| `m1`/`k1`/`l1`/`q1` | 11/8/4/1 | kotlinx.coroutines 基础（Continuation/SuspendLambda/Job） | `m1.a`/`k1.d`/`l1.d`/`q1.e` 引用关系 |
| `s4`/`x4`/`w4`/`t4`/`z4`/`u4`/`v4` | 89/43/12/5/14/22/16 | kotlinx.coroutines 运行时 + Flamingo 调度器 | `s4.c0`=Dispatcher、`s4.i.d`=launch |
| `e`/`h`/`i`/`j`/`g` | 27/13/5/1/5 | androidx.room / androidx.sqlite.db | tag `ROOM`(e)、`SupportSQLite`(h/i)、`SupportSQLiteLock`(j)、`RoomCursorUtil`(g) |
| `l`/`n`/`o`/`r`/`s`/`p`/`q`/`m`/`d`/`b`/`c`/`f`/`k` | 23/14/14/4/1/1/1/5/4/4/1/1/1 | **Gson**（R8 重命名） | `l.d`=Gson 主类（`ConcurrentMap` TypeToken 缓存）、`o.n`=TypeAdapters、`p.a`=ISO8601Utils、`s.a`=TypeToken、`m.c`=`@SerializedName` |
| `d6`/`f6` | 23/25 | Retrofit2（核心 / 注解） | `d6.u`=Retrofit、`f6.f`=`@GET`、`f6.o`=`@POST`、`f6.a`=`@Body`、`f6.t`=`@Field` |
| `g5` | 33 | OkHttp3 | `g5.z`=`OkHttpClient implements Cloneable, e.a`、`g5.w`=Interceptor |
| `c4` | 73 | androidx.collection（ArrayMap/SimpleArrayMap 等） | `c4.e0 implements m2.a, g4.i` |
| `h1` | 46 | Kotlin `kotlin.text`（`StringsKt` 等） | `h1.y extends x`、`h1.q`=`emptyList` 用法 |
| `l2` | 65 | Kotlin `kotlin.collections` 扩展 | `l2.z0` 为 interface，被 200+ 类引用 |
| `p4` | 25 | Kotlin `kotlin.ranges`/`sequences` 辅助 | `p4.v`/`p4.w` 高频用于 `for` 循环跨界 |
| `a6`/`o2`/`f3`/`t5`/`y`/`b4`/`i2`/`d3`/`d4`/`q3`/`w2`/`o3` | 114/40/23/30/14/14/14/30/22/27/8/19 | **腾讯 TMSDK/TMF**（`tmsdk.*`、`tmsdkobf.*` 的实现体） | 均 import `tmsdk.common`/`tmsdk.bg`；`a6` 独占 9039 行 `z2.java` |
| `q0` | 18 | Flamingo 云端 API 层（7 个 Retrofit 端点 + 签名拦截器 + base URL） | `@f6.*("/virus/…")`、`q0.m.f7703a` |
| `r0` | 44 | Flamingo Room DAO/Entity + TypeConverter | `EngineDatabase.F()/G()…` 返回 `r0.*` |
| `s0`/`u0`/`t0`/`l0`/`m0` | 30/10/21/6/1 | Flamingo SDK 引导 / 响应包装 / 领域模型 / 管线阶段 / Room 适配 | `s0.s` 建 Retrofit、`t0.a`=ScanTarget 接口、`l0.b/c/d/e`=Stage 接口 |
| `c5`/`d5`/`y0`/`x0`/`z` | 1/1/1/1/3 | 设备判定 / 全局 `_global` 标志 / 崩溃栈 MD5 / `content://guard/virus` 常量 / 埋点 | `c5.a`=DeviceUtils、`d5.a.f2965a` |

---

## 3. 数据流 / 时序

### 3.1 进程启动时序（`b`/`GuardApplication`）

```
zygote → new GuardApplication
  └─ Application.<init>  → com.miui.guardprovider.b.<init>            [b.java:34]
       └─ new dagger.hilt.android.internal.managers.a(new a())      [b.java:21]
  └─ attachBaseContext(ctx)                                          [GuardApplication.java:72]
       ├─ f1743d = this                                              [GuardApplication.java:74]
       ├─ w0.g.b(ctx)  == androidx.core.os.a.a(ctx) (用户已解锁?)      [w0/g.java:19]
       │    ├─ true  → w0.f.b(ctx)   预热 SharedPreferences("GuardProvider")  [w0/f.java:36]
       │    └─ false → f()   registerReceiver(new a(), ACTION_USER_UNLOCKED) [GuardApplication.java:50-56]
       │                 └─ onReceive: w0.f.b(ctx) + g()(注销) + 无其它  [GuardApplication.java:29-35]
  └─ onCreate()                                                      [GuardApplication.java:83]
       ├─ super.onCreate() → b.onCreate → c() → dagger 注入         [b.java:44-47]
       │    └─ com.miui.guardprovider.a$b.a(GuardApplication)  **方法体为空** [a.java:29-31]
       └─ w.c.a(this)  **方法体为空**                                [w/c.java:8-9]
```

**【观测】** Application 阶段**没有**任何 SDK 初始化：不调用 `TMSDKContext.init`、不调用 `AVLEngine.init`、不调用 `OneTrack.createInstance`、不建数据库、不建目录、不读任何 `SystemProperties`（`GuardApplication` 全文 87 行只有 §4.1 列出的 4 个外部调用）。
**【观测】** `guardprovider.support.mix.scan` / `guardprovider.scan_properties` 两个 meta-data 在全树 Java 中 **0 处读取**（grep `scan_properties` / `mix.scan` 仅命中 `h0/a.java:14` 读的是 SecurityCenter 的另一键）。→ 死配置。
**【观测】** 进程级静态初始化只有：`d5.a.f2965a`（读 `ro.product.mod_device` 判定 `_global`，`d5/a.java:10`）、`h0.b` 的 4 个 `d.a(...)` 静态字段（`h0/b.java:31-34`）、`f0.b` 枚举 `b()`（读云控 `VirusScan/miEngineNgEnable`，`f0/b.java:56-60`，**首次访问 `f0.b.values()` 时才触发**）。

### 3.2 `guard/sync_scan`（1006）→ native 扫描 完整链路

触发：`contentResolver.query(Uri "content://guard/sync_scan", ..., selectionArgs=[path, bool])`。

```
GuardProvider.query                                        [GuardProvider.java:381]
 └─ case 1006: selectionArgs 必须长度==2，否则 return null     [395-398]
 └─ e(strArr2[0], Boolean.parseBoolean(strArr2[1]))          [399]
     └─ f(path, useCloud, engineName=null)                   [192-193]
         ├─ path 以 "content://" 开头：
         │    copy 到 cacheDir/copy.apk（Files.copy + REPLACE_EXISTING）  [211-216]
         ├─ path 以 "file://" 开头：取 Uri.getPath()                          [269-270]
         ├─ g.z().L(new String[]{path}, observer a, useCloud, 2, null)      [247-250]
         │    ├─ 过滤 null 与 !File.exists()                                  [1260-1271]
         │    ├─ 若 list 空 或 静态任务计数 f6412h >= 10 → observer.v(-1,null); return -1 [1272-1282]
         │    ├─ w0.e.a(ctx)  → 向 "content://com.lbe.security.miui.su" 请求 GrantGuardProvider（拿外部存储权限）[w0/e.java:8-17]
         │    ├─ 建 5 张表：f6418d(签名缓存) f6417c(结果) f6419e(灰产标记) f6420f(scanType) f6416b(任务表) [1285-1289]
         │    └─ new n0.g$f(...).executeOnExecutor(THREAD_POOL_EXECUTOR)      [1290-1292]
         └─ countDownLatch.await(2, TimeUnit.MINUTES)   ← **2 分钟硬超时**     [250]
            超时或 iL<0 或 结果为 null → return null（Cursor 为 null）        [251-256]

n0.g$f.doInBackground（AsyncTask，1 次）                     [n0/g.java:791]
 ├─ observer.d(1)                    // onScanStarted                     [796]
 ├─ observer.c(-1, paths[0])         // onScanStartItem                   [803]
 ├─ 逐 path 白名单预检（CheckWhiteList）：                            [811-859]
 │    ├─ PackageParser.parsePackageLite(new File(path), 0)           [821]
 │    ├─ i0.h.d(ctx, packageName) 或 i0.h.f(ctx, path) 取 PackageInfo  [826]
 │    ├─ i0.g.a(new PackageParser.Package(pkg), info) → Signature[]   [831]
 │    ├─ i0.h.c(sigs) → MD5(hex, 多签名用 '|' 连接)                    [832, i0/h.java:36-50]
 │    ├─ 缓存 d{path→(pkgName, versionName, versionCode, sigMd5)}      [827-837]
 │    ├─ i0.f.a(ctx).d(pkg, sigMd5) 命中 → 认为安全：
 │    │    arrayList.remove(path); level=1; 直接 observer.s(progress)  [838-857]
 │    └─ 未命中 → 保留待扫
 └─ onPostExecute: 若还有待扫项 → K(list, observer, useCloud, 2, null, 已跳过数)  [866-874]
      若全部被白名单跳过 → observer.v(-1, 白名单结果数组) 后取消           [875-890]

n0.g.K(list, observer, useCloud, scanType, extras, offset)    [n0/g.java:1132]
 ├─ listT = t(x.a.c())     // x.a.c() = SP 里启用的引擎名列表          [1134]
 │    └─ t(): Arrays.stream(f0.b.values()).filter(名字 in listT)
 │            .map(new n0.i().a(bVar))                                [1172-1188]
 │            └─ n0.i.a(): bVar.f3549d.getConstructor(f0.b.class).newInstance(bVar)  ← **反射建实例** [n0/i.java:10-15]
 ├─ this.f6415a.put(observer, new ConcurrentHashMap())  // path→已完成引擎数  [1136]
 └─ for engine in listT:                                              [1137-1146]
      ├─ c task = u(observer, engine); task.p(p(task))  // 分配 taskId 0..100 环形 [1139-1140]
      ├─ task.n(listT)                                  // 记录同批引擎数   [1141]
      ├─ list2.add(task)                                                  [1142]
      ├─ b thread = new b(task, new a(mainLooper, observer, list2, listT, offset, list),
      │                    list, useCloud, extras)                        [1143]
      ├─ task.q(thread)                                                   [1144]
      └─ thread.start()          ← **每个引擎一个线程，并发启动**          [1145]
 └─ z.a.c("security_scan")   // OneTrack 埋点（首次调用触发 OneTrack init）[1147]

n0.g$b.run（每引擎线程）                                      [n0/g.java:275-279]
 ├─ task.o(handler)   // 绑定主线程 Handler
 └─ task.r(list, useCloud, extras)                            [620-628]
      ├─ f6438c.d(useCloud)   // b0.a.f311f = 云扫开关
      ├─ f6438c.e(list)       // b0.a.f312g = 待扫列表副本
      ├─ f6438c.f314i = extras
      ├─ f6438c.f307b = true
      └─ l()                                                     [521-541]
          ├─ engine.f() 为 true（尚未 init）→ engine.e(回调 InterfaceC0073a)  [526-528]
          │    └─ onInitComplete(code) → engine.j(list, listener, useCloud&&engine.g(), extras)  [391-402]
          │         注意：MiEngine 分支会先 Thread.sleep(f309d)，f309d 默认 0         [394-400]
          └─ 已 init → engine.j(list, listener, useCloud&&engine.g(), extras)        [539]

engine.j(...)   // 见 §5.4 四引擎实现
 └─ 逐文件扫 → listener(b0.a.c 实现).d(scanType, i+1, total, [y.f])   [n0/g.java:326-362]
      ├─ level ∈ {2,3,6} 且 i0.f.a().f(path)（文件 MD5 在 t_virus_white_list）→ level=1   [335-337]
      ├─ 缓存到 b0.a.f313h（path→最优结果，compareTo 取更严重）                            [338-340]
      ├─ level ∉ {2,3,6} → 直接 task.m(i-1, path) 发进度消息                              [343-346]
      ├─ level ∈ {2,3,6} → 首次进入时 i0.d.g()（拉白名单）                               [348-354]
      ├─ 若该文件签名 MD5 在 mi_virus_white_db → level=1（"add white from whiteList"）     [355-360]
      └─ task.m(i-1, path)   → handler.obtainMessage(3, bundle{pathIndex, path})          [602-610]

n0.g$a.handleMessage(3)  // 主线程进度聚合                        [n0/g.java:115-246]
 ├─ 单引擎（f6424c.size()==1）：直接取该引擎结果 → observer.s(...)  [190-211]
 └─ 多引擎：f6415a[path] 计数++
      ├─ 计数 < 引擎数 → 继续等                                              [217-221]
      ├─ 计数 == 引擎数 → 逐个引擎 f313h 取结果，fold g.D() 合并           [222-230]
      │    └─ D(): 按 virusLevel 优先级 2 > 3 > 6 > 4 > 5 > 1 覆盖；extras 字符串累加  [1014-1087]
      ├─ f6417c 追加合并结果，observer.s(offset+idx, total+offset, [merged])  [232-237]
      └─ 该 path 计数清零；若 keySet 空 → sendEmptyMessage(2)              [238-240]

n0.g$a.handleMessage(2)  // 扫描结束                            [n0/g.java:118-175]
 ├─ 取 f6417c → y.f[]，同步填 versionName/versionCode（来自 f6418d 缓存）  [120-145]
 ├─ observer.v(taskId, results)                                        [146-151]
 ├─ 清理 5 张表 + cancel 主 AsyncTask                                  [152-160]
 ├─ **非国际版**：new AsyncTaskC0152a(...).execute → c0.d.g(ctx).i(results) 写 scan_result_db  [161-163]
 └─ 逐 task：thread.interrupt() + handler.removeCallbacksAndMessages(null) + I(taskId)  [168-174]

GuardProvider$a.v(taskId, results)   // 状态机终点           [GuardProvider.java:56-77]
 ├─ results[0] 存入 b.f1752a
 ├─ n0.h.a(ctx, results[0])   // 向 com.miui.securitycenter 发 TRACK_ANTIVURS_DATA 广播
 └─ countDownLatch.countDown()

回到 GuardProvider.f():
 └─ 组 MatrixCursor{"Type","VirusName","Description","EngineName","AdType","AdName","AdDescription"}
    值 = {f11667l, f11659d, f11660e, f11661f, f11668m, f11663h, f11664i}   [257-260]
 └─ 删除 cacheDir/copy.apk（若走过 content:// 分支）                        [261-267]
```

**【推断】扫描类型参数 `2` 的真实含义**：`L(strArr, cVar, z6, i6=2, str)` 的第 4 参 `i6` 在 `n0/g` 内部**只被当作进度基数（offset）**使用——`K()` 把它当 `i7` 传给 `a`（Handler）构造器，Handler 里仅用于 `observer.s(i8 + f6425d, ...)`（`n0/g.java:199`、`237`）。它**不参与**扫描模式判定、不参与引擎选择。真正的模式分流发生在 `M()`：`i6 == 1 → C()`（增量），其余 → `L()`（`n0/g.java:1296-1303`）。因此 `2` 只是一个「同步扫描场景」的进度偏移量标记，不是 `p0` 那套 `scan_params` 位域。
**【观测】** `guard/install_scan`（1008）走**同一**函数 `f(path, bool, engineName)`，其内部同样硬编码 `2`（`GuardProvider.java:249`、`271`）。
**【观测】** 2 分钟超时后即使 native 扫描仍在跑，Provider 也返回 `null`（`GuardProvider.java:250-256`）——结果通过广播旁路送 SecurityCenter；任务本身不会被取消。

### 3.3 `guard/engine_info_list`（1005）

```
GuardProvider.query case 1005                             [GuardProvider.java:384-394]
 ├─ MatrixCursor 列名：Name, NameString, DescriptionName, DialogTitle, DialogHeader,
 │                     DialogPolicy, DialogCloudScan, DialogSummary, DialogUrl, Enable, SupportCloud
 └─ for bVar in x.a.f()  // 见 §5.5「引擎可见列表」
      objArr[0] = bVar.f3547b                       // 内部名 "Mi"/"AVL"/"TENCENT"
      System.arraycopy(getStringArray(bVar.f3553h), 0, objArr, 1, 8)   // 资源数组 8 项
      objArr[9]  = g.z().w(bVar.f3547b) ? 1 : 0     // SP 里的启用状态
      objArr[10] = bVar.f3551f ? 1 : 0              // 支持云扫（编译期常量 true）
```
8 个字符串的语义映射与逐引擎取值见 §5.6。

### 3.4 `guard/virus`（1007）

| 操作 | 行为 | 位置 |
|---|---|---|
| `query` | `SELECT * FROM t_virus_white_list`（`miui_virus.db`） | `GuardProvider.java:401` |
| `insert` | `insert("t_virus_white_list","_id",cv)`；成功后 `notifyChange` + `c0.d.g(ctx).c(new String[]{cv.get("dir_path")})` 清 `scan_result_db` 中该路径缓存 | `357-370` |
| `delete` | `delete("t_virus_white_list", selection, args)`；`>0` 时 `notifyChange` + `c0.d.g(ctx).c(args)`（把 args 当路径数组） | `338-349` |
| `update` | 恒 `return 0`（未实现） | `419-421` |
| 其它 URI | `delete`/`insert` 都 `return 0`/`null`（先取 writable db 再判 match） | `340`、`359` |

**【观测】** 该表既是「病毒白名单」也是「跳过表」：`i0.f.f(path)` 用 `virus_md5 = ?` 查它（`i0/f.java:97`），命中即把结果 level 降为 1（`n0/g.java:335-337`）。

### 3.5 `guard/support_incremental`（1009）

```
case 1009: if (!i0.h.h(ctx)) return null;         // GuardProvider.java:406-408
           MatrixCursor{"support"} + row{1}        // 409-411
i0.h.h(ctx) = ctx.getPackageManager()
                 .getPackageInfo("com.miui.securitycenter",128)
                 .applicationInfo.metaData.getBoolean("miui.support_full_scan")   // i0/h.java:86-92
```
**【观测】** URI 名为 `support_incremental`，但底层读的键是 **`miui.support_full_scan`**（"全量扫描"能力位），语义与名字不一致。与 Lead 结论一致，可交叉确认。

### 3.6 `call("parseApk")` / `call("getRiskAppData")`

```
call(method, arg, extras)                                  [GuardProvider.java:330-335]
 ├─ "parseApk"      → d(arg, extras)                        [159-190]
 │    ├─ 若无 READ_EXTERNAL_STORAGE 且 arg 不在 /data 下 → w0.e.a(ctx)（LBE 授权）  [163-165]
 │    ├─ w0.d.a(ctx, path, flags)  // API>=29 用隐藏 PackageParser，否则 getPackageArchiveInfo  [w0/d.java:11-127]
 │    ├─ flags&128 → SecurityCenterHelper.getLabel + 图标（Bitmap 或 drawable→bitmap）  [175-187]
 │    └─ Bundle{"pkgInfo", ["label","icon"]}
 └─ "getRiskAppData" → b()                                  [103-127]
      ├─ getInstalledPackages(0)
      ├─ listE = c0.b.f(ctx).e()  // risk_gray_result_db 全表（并先清过期）    [c0/b.java:97-121]
      ├─ 逐包：跳过系统应用((flags&1)==1)；c(pkgInfo, listE) 找同包名+同 versionCode 的记录  [110-111]
      ├─ 命中后：若 i0.f.a(ctx).e(pkgInfo)（白名单）→ c0.b.f(ctx).b(pkgName) 删除该行并丢弃  [117-119]
      │           否则 put JSON{packageName, versionCode, type, signaturesMd5}            [139-145]
      └─ 返回 Bundle{"riskData": JSONArray 字符串}
```

### 3.7 `SecurityService` 链路（摘要，完整事务表见 §5.2）

```
bindService(action="...antivirusservice") → onBind 返回 y.a 实现（SecurityService$a）  [SecurityService.java:312-314]
bindService(action="...urlscan")          → onBind 返回 y.b 实现（SecurityService$b）  [315-317]
每次调用先 c() = !androidx.core.os.a.a(this)（用户是否锁定）                            [297-299]
  → 锁定则统一降级：返回 -1 / 0 / false / null，或给 observer 回调 v(-1,null)、K(-1)
```

### 3.8 扫描结果的两个旁路

1. **写库**：`n0.g$a.handleMessage(2)` → 非国际版 → `c0.d.g(ctx).i(a[])` 写 `scan_result_db`（`n0/g.java:161-163`，`c0/d.java:243-288`）。
2. **广播**：`GuardProvider$a.v()` → `n0.h.a(ctx, f)` → `sendBroadcast(Intent("miui.intent.action.TRACK_ANTIVURS_DATA").setPackage("com.miui.securitycenter"))`，14 个 extra，`level ∈ {1,4,5}` 时不发（`n0/h.java:8-30`）。
   【观测】`n0/h.a` 只在 `GuardProvider$a.v()` 一处被调用（`GuardProvider.java:74`）；`o0/a` 的其它观察者（如增量扫描的 `BinderC0154g`）不走广播。

---

## 4. 关键逻辑逐条解析

### 4.1 进程初始化与全局网络开关

| # | 位置 | 观测 | 推断 | 未知 |
|---|---|---|---|---|
| 4.1.1 | `GuardApplication.java:38-48` | `e(Context)`：非国际版 = `d.b("persist.sys.sc_allow_conn", false)`；国际版 = `d0.b.c(ctx,"com.miui.securitycenter")`（Privacy.isEnabled）&& 该属性；`ro.miui.customized.region=="jp_kd"` 时再 && `Settings.Secure.getInt(cr,"antivirus_engine_network_enable",0)==1` | 这是**整个进程唯一的网络总闸**，全部通过 `d0/d.java` 反射 `android.os.SystemProperties.getBoolean` 读取 | `persist.sys.sc_allow_conn` 的写入方（应为 SecurityCenter/系统） |
| 4.1.2 | 全树 grep `GuardApplication.e(` | **恰好 5 处调用点**：`f0/a.java:66`（`f0.a.b()`，引擎基类"是否允许联网"）、`h0/b.java:84`、`h0/b.java:153`（两个 HTTP 方法的**前置 return null**）、`n0/g.java:642`（更新病毒库前的白名单刷新）、`z/b.java:41`（`OneTrack.setAccessNetworkEnable`） | 断网时被彻底关掉的功能：① 所有引擎的 `b()`（AVL `setNetworkEnabled`、Tencent `setAutoConnectionSwitch`、MiEngineNG `delegate.m()`）→ 云查杀/云库全部失效；② `api.sec.miui.com` 病毒库+白名单同步；③ `flash.sec.miui.com/detect/app` 反诈；④ OneTrack 上报；⑤ **扫描前不刷新白名单**（`n0/g.java:642` 的 `i0.d.d().j(true)` 跳过） | 云端 `virus-engine.sec.miui.com`（新 Retrofit 栈）是否也受此闸控制 → 见 4.1.3 |
| 4.1.3 | `q0/q.java`（签名拦截器）、`s0/s.java:124` | 新 Retrofit 栈的 base URL 由 `q0.m.f7703a` 固定，**构建期**不检查 `GuardApplication.e()`；但 `p0/n.java:828` 的 `m(boolean)` 把 `GuardApplication.e()` 写入静态 `AtomicBoolean f7476g`，供 `p0` 各 Scanner 读取 | 【推断】新栈的闸门在 Scanner 内部（`p0/f.java` 的 `f7380d` 等），不在 OkHttp 层 | `p0` 内具体判定点未逐行确认（属 t-cloud 范围） |
| 4.1.4 | `f0/b.java:56-60` | `b()`：`d0.b.b(getContentResolver(),"VirusScan","miEngineNgEnable","true")` 决定 `MI` 常量指向 `MiEngineNG` 还是 `MiEngine` | 这是**云控（SettingsCloudData）决定引擎实现类**的唯一开关，默认 `"true"` → 默认 MiEngineNG | 云控下发者（MIUI 云控平台） |
| 4.1.5 | `d0/b.java:14-27` | `MiuiSettings$SettingsCloudData.getCloudDataInt/String` 反射；`d0/c.java:7-11` 通用 `getDeclaredMethod + setAccessible(true) + invoke(null,...)` | 远程配置读取全部走反射，无 `Settings.Global` 或 SecurityCenter provider 直读 | 反射失败时 `b()` 返回 `""`（`d0/b.java:26`），`a()` 返回 `0`（`d0/b.java:23`） |
| 4.1.6 | `d0/a.java:7-12` | `android.miui.AppOpsUtils.isXOptMode` 反射 | 用于 `w0/e.java:9` 判断是否跳过 LBE 授权 | — |
| 4.1.7 | 全树 | **无** `DexClassLoader` / `InMemoryDexClassLoader` / `PathClassLoader` 自建 | 无插件化/热修复 | — |
| 4.1.8 | `w0/h.java:11`、`i0/b.java:10`、`i0/e.java:12`、`c0/a.java:12`、`c0/c.java:12`、`r0/x.java:22` | 6+1 个库：`miui_virus.db`(v5)、`miui_antivirus.db`(v2)、`mi_engine_white_virus.db`(v1)、`risk_gray_result.db`(v1)、`scan_result.db`(v2)、`flamingo.db`(Room v2) | 全部懒创建：由 `SQLiteOpenHelper`/Room 首次 `getWritableDatabase()` 触发 | — |

### 4.2 IPC 门面的边界行为

| # | 位置 | 观测 | 推断 |
|---|---|---|---|
| 4.2.1 | `GuardProvider.java:396-398` | `sync_scan` 的 `selectionArgs` 不是恰好 2 个 → 直接 `return null`（不抛异常） | 调用方必须严格给 2 个参数；`install_scan` 少于 3 个则**退化**为走 `query("t_virus_white_list")`（`401` / `415`），返回白名单游标而不是扫描结果 — **危险的静默降级** |
| 4.2.2 | `GuardProvider.java:383-415` | `switch` 中 `case 1008: break;` 后落到方法末尾的 `(strArr2.length < 3) ? cursorQuery : f(...)` | 1008 传 2 参时 `cursorQuery` 为 `null`（1007 才赋值）→ 返回 null |
| 4.2.3 | `GuardProvider.java:210-216` | `content://` 输入会被**复制到 `cacheDir/copy.apk`**（固定文件名） | 两个并发 `content://` 扫描会互相覆盖同一路径 → 竞态；且退出前必删（`220-221`、`252-254`、`261-267`） |
| 4.2.4 | `GuardProvider.java:247-256` | 每次扫描新建 `b bVar`（结果容器）；`countDownLatch.await(2, MINUTES)` 返回后若 `f1752a==null` 即返回 null | 超时/失败对调用方表现为「Cursor 为 null」，无错误码 |
| 4.2.5 | `GuardProvider.java:111-114` | `b()`（getRiskAppData）里第一次发现灰产时调用 `d.g(this.f1748b)`（拉白名单网络请求）**在 `for` 循环内只做一次**（`z6` 标志） | 该网络调用在 Binder 线程同步执行 → 可能拖慢 `getRiskAppData` |
| 4.2.6 | `SecurityService.java:297-299` | 每个 AIDL 方法先 `c()`（= 用户未解锁则视作 locked），locked 时统一降级 | 降级矩阵见 §5.2 |
| 4.2.7 | `SecurityService.java:76-89` | `E(str,int)`：`i6∈{2,3}` → 异步线程池执行 `d.g(ctx).b(str)` + `c0.b.f(ctx).b(str)`（按包名清两个结果库）；`i6==1` → `i0.d.d()` 增量更新病毒库 | 这是「卸载/变更通知」入口：type 2/3 = 包被卸载/更新需清缓存，1 = 触发增量病毒库更新 |
| 4.2.8 | `SecurityService.java:169-172` | `getVersion()` 恒返回字符串 `"1"` | 协议版本硬编码，与 `versionName 2.0.3` 无关 |
| 4.2.9 | `SecurityService.java:1415`（实为 `n0/g.java:s()`） | `g.z().s()` 恒 `return -1` | `checkUpdate`（事务 6）实际是**空实现**，永远返回 -1 |

### 4.3 扫描编排（`n0.g`）

| # | 位置 | 观测 | 推断 |
|---|---|---|---|
| 4.3.1 | `n0/g.java:1228-1237` | `z()` 双重检查单例；`private g(){ x.a.l(); }`（`983-985`） | 首次 `g.z()` 会执行 `x.a.l()` → 引擎启用初值 + 可能触发云控拉取（§4.5） |
| 4.3.2 | `n0/g.java:41-66` | 静态 `f6413i`（`HashMap<int, c>` 任务表）、`f6412h`（任务计数）；实例 6 个 `ConcurrentHashMap` 全部以 `y.c`（observer Binder）为 key | 一个 observer 只能同时跑一个批次（多批次会互相覆盖 `f6416b` 等表） |
| 4.3.3 | `n0/g.java:1260-1294` | 任务创建前：`filter(nonNull) → filter(File::exists)`；`list.isEmpty() \|\| f6412h >= 10` → `observer.v(-1,null)` + `return -1` | **最多 10 个并发扫描任务**；不存在的路径被静默剔除；`install_scan` 同样受限 |
| 4.3.4 | `n0/g.java:1150-1170` | `p(c)` 分配 taskId：从 `f6412h` 起找空位，`>100` 回绕到 0 | taskId 空间 0..100，回收后复用；`cancelScan(id)` 依赖它 |
| 4.3.5 | `n0/g.java:1132-1148` | 引擎集合来自 `x.a.c()`（SP 启用项），逐引擎反射构造实例（`n0/i.java:10-15`）并**各起一个 Thread** | **并发模型 = 引擎级并行、文件级串行**：每个引擎内部 `for (String path : list) { Thread.sleep(5); scan(path); }`（见 `AvlEngine.java:149-161`、`TencentEngine.java:460-473`、`g0/e.java:86-127`） |
| 4.3.6 | `n0/g.java:184-245` | 进度聚合：`f6415a[observer]` 是 `path → 已完成引擎数`；等于引擎数时合并 | 多引擎结果**按 path 归并**，不是按「引擎投票」；`D()` 用 `virusLevel` 优先级 2>3>6>4>5>1 覆盖，`extras`（`f11671p`）字符串**拼接累加** |
| 4.3.7 | `n0/g.java:1014-1087` | `D(a,b)` 的 6 个 case：base=1 时接受 2/3/4/5/6；base=2 时**只累加 extras**；base=3 只接受 2；base=4 接受 2/3/6；base=5 接受 2/3/6/4；base=6 只接受 2/3 | 「更严重者胜」，且 level 2（病毒）> 3（风险）> 6（灰产/反诈）> 4 > 5 > 1（安全） |
| 4.3.8 | `n0/g.java:987-995`、`1117-1129` | `A(id)` 按 id 找任务；`I(id)` 移除并 `f6412h--` | 取消/结束时必须成对调用，否则计数泄漏导致永久 `>=10` 拒绝新任务 |
| 4.3.9 | `n0/g.java:1372-1405` | `q(id)`：先按 `f6416b` 反查 observer → 对每个子任务 `i()`（置 `f307b=false`、`f310e=0`、`thread.interrupt()`、`engine.a()`、`I(id)`、`handler.removeCallbacksAndMessages(null)`）；再 `cancel(true)` 主 AsyncTask | 【观测】只 cancel `f6421g`（最后一次 `L()` 创建的主 AsyncTask），多批次时前面的不会被取消 |
| 4.3.10 | `n0/g.java:1407-1413` | `r(id)`（取消更新）：`A(id).j()` → 对每个引擎 `k()` | `k()` 各引擎实现不同：AVL=`stopUpdate()`、Tencent=`UpdateManager.cancel()`、MiEngine=空、MiEngineNG=`delegate.g()` |
| 4.3.11 | `n0/g.java:553-576` | `binderDied()`：扫描中 → 对所有子任务 `i()`；更新中 → `j()`；`cancel` 主 AsyncTask；`unlinkToDeath` | 调用方进程死亡能正确释放引擎；【观测】`f6416b.get(...)` 直接 `.iterator()` 未判 null，若异常时序可能 NPE |
| 4.3.12 | `n0/g.java:128-145` | 结束回调里逐个从 `f6418d` 取 `d{versionName,versionCode,sigMd5}` 回填 `y.f` | 引擎自身不保证填 version 信息（AVL/Tencent 会填，MiEngineNG 会填），缓存用于兜底 |
| 4.3.13 | `n0/g.java:1428-1440` | `x()` 建 `new String[4]` 但只填 3 个 → 返回 `["Mi","AVL","TENCENT",null]` | **真实 bug**：AIDL 事务 11（`i()`）返回的数组末位恒为 null |
| 4.3.14 | `n0/g.java:1243-1250` / `1419-1426` | `F(name)` 返回编译期常量 `f3551f`（三引擎都是 `true`）；`w(name)` 返回 SP 实际值 | 「是否支持云扫」= 与引擎无关的恒真值 |
| 4.3.15 | `n0/g.java:1252-1258` | `J(name, enable)`：`(z6 \|\| name != "Mi")` 才写 SP | **MI 引擎无法被关闭**（即使传 false 也不写） |
| 4.3.16 | `n0/g.java:1330-1370` | `O(list)` 按包名+`sourceDir` 过滤白名单；`P(list)` 按 apk 路径过滤 | 供 AIDL 事务 23/21 使用（`M`/`f`），用于「扫描前剔除已知安全项」 |
| 4.3.17 | `n0/g.java:1195-1226` | `v(ctx)`：`MediaStore.Files` 查询 `_data LIKE '%.apk'` → `i0.h.f(ctx, path)` 得到 PackageInfo | 增量扫描会**遍历外部存储上所有 apk** |
| 4.3.18 | `n0/g.java:664-754` | 增量扫描（AsyncTask `e`）：`getInstalledPackages(64)` + `v(ctx)`；有缓存命中且 level∈{2,3,6} → 直接回放缓存（`m()`）；否则把「无缓存」的路径批量 `L(..., 1, null)` 走一次普通扫描 | 增量 = 只扫「无缓存」项；缓存有效期见 §5.5 `virus_cache_validty_perod` |

### 4.4 四个引擎适配器

统一契约 `f0.a`（`f0/a.java`）：`a()` 取消、`e(InterfaceC0073a)` 异步初始化、`f()` 是否待初始化、`g()` 能力位、`h()` 释放、`j(List, c, boolean, String)` 扫描、`k()` 取消更新、`l(b)` 更新、`m(int,String)` URL 类操作、`n(y.d, d, Object)` 无线类操作、`b()` = `GuardApplication.e(c())`、`d()` = 引擎内部名。

| 能力 | `MiEngine` | `MiEngineNG` | `AvlEngine` | `TencentEngine` |
|---|---|---|---|---|
| 实现栈 | `i0.a`(=`i0.d`) + `g0.e` | Kotlin → `p0.n` → `s4`/`r0`/Rust | `com.avl.engine.AVLEngine` | TMSDK `QScannerManagerV2` |
| `e()` 初始化 | `f1759d.a(ctx)`，`f1758f=true`；回调 `b(0)`/`a(-1)` | **恒 `b(0)`**（无初始化，`MiEngineNG.java:227-231`） | `AVLEngine.init(ctx, AVLSdkConfig)`；返回 `0` 才算成功（`AvlEngine.java:99-119`） | `TMSDKContext.init(...)`；`f1767f` 为真才算成功（`TencentEngine.java:403-415`） |
| `f()` 待初始化 | `!f1758f` | **恒 `false`** | `!f1757d` | `!f1767f` |
| `g()` 能力位 | `true` | `true` | `true` | `true` |
| 云扫支持 | **不生效**：`j()` 忽略 `z6` | 生效：`z6` 参与 `scan_params` 计算 | 生效：`r(z6)`=`setCloudScanEnabled` | 生效：`(b() && z6) ? 6 : 2` 作为 `scanUninstallApks` 的 scanType |
| 增量/文件粒度 | `g0.e.d()` 逐 apk | 整体交给 `delegate.i(Collection<Path>, …)`，内部并发 | 逐 apk，`Thread.sleep(5)` | 逐 apk，`Thread.sleep(5)`，每文件一次 `scanUninstallApks` |
| 广告检测 | 无 | 无（`f11668m/f11663h/f11664i` 从不赋值） | 无 | 无 |
| 取消 | `this.f3536c=true`，`g0.e` 循环内检查 | `delegate.f()` = `q1.e(job,"External cancellation")` | `this.f3536c=true`，循环内检查 | `f3536c=true` + `QScannerManagerV2.cancelScan()` |
| 更新 | `l(b)`：无实现（`this.f1760e` 恒 false → 回调 `a(2,1)`） | `n.o(delegate, null, consumer, 1, null)`，`IntConsumer` 映射 -1→`a(2,-1)`、0→`a(0,0)`、1→`a(3,1)` | `checkUpdate()`==0 → `a(3,0)`；否则 `update()`：>0→`a(0,n)`、==0→`a(3,0)`、<0→`a(2,n)` | `UpdateManager.check(UPDATE_FLAG_VIRUS_BASE,…)`；无更新→`a(3,err)`、失败→`a(2,err)`（err 初值 `-2146769239`） |
| 释放 `h()` | 空 | 空 | 空 | `freeScanner()` 后置 null |
| URL 类 `m(int,String)` | **未 override** → 基类 `return -1` | 未 override → `-1` | `i6==1`→`AVLEngine.scanURL(str)`；`i6==2`→`updateURLRule()`，成功(1)时把 `getURLDatabaseVersion()` 写 SP `avl_url_sdk_version` | 未 override → `-1` |
| 无线 `n(...)` | 未 override → `0` | 未 override → `0` | 未 override → `0` | WifiDetect：`NETWORKSTATE`/`DNSANDPHISHING`/`ARP`/`SECURITY` 四路（`TencentEngine.java:82-129`） |
| 结果构造 | `g0/e.c()`：`level = b(type)`，`extras="Mi:safe/danger;"`；另叠加反诈 `"AntiDefraud:danger;"`（`g0/e.java:21-38`） | `x(aVar)`：默认 `level=1, extras="MiEngineNG:safe;"`；`t0.o` 的每个 threat → `level = name.contains("Malware") ? 2 : 3`，`extras="MiEngineNG:danger;"` | `o(info,path)`：`p(info)` 映射 dangerLevel 1→2、2→3、其它→1；`virusName` 以 `risk/` 开头 → 6；`extras="AVL:safe/danger;"` | `u(entity)`：`t(scanResult)` 映射 258→5、259/261/262→3、260→2、其它→1；`extras="TENCENT:safe/danger;"` |
| 错误码 | init 失败 `a(-1)`；无其它错误码 | 同上 | `AVLEngine.init` 返回非 0 → `a(-1)`；`scanURL`/`updateURLRule` 原样透传（`updateURLRule` 非 1 直接 return） | `TMSDKContext.init` false → `a(-1)`；更新用 `-2146769239` 作「无错误」哨兵 |

**【观测】** `virusType`（`y.f.f11666k`）**全树只有 2 个赋值点**：`AvlEngine.java:41`、`57` 与 `TencentEngine.java:377`，都写 `1`。`MiEngine`/`MiEngineNG` 从不赋值（默认 0）。→ 见 §4.7「威胁类型枚举」。
**【观测】** `adLevel/adName/adDescription`（`f11668m`/`f11663h`/`f11664i`）**全树 0 处赋值**，仅被 `GuardProvider` 读出（`GuardProvider.java:260`）和 `n0/h.java:25-26` 作为广播 extra。→ 广告检测是**协议预留位，未实现**。

### 4.5 引擎启用状态与云引擎选择（读写全表）

| 步骤 | 位置 | 观测 |
|---|---|---|
| 读启用集合 | `x/a.java:38-49` `c()` | 遍历 `f0.b.values()`，`w0.f.a().c(f3550e)` 为真则加入 |
| 读单个启用 | `x/a.java` → `n0/g.java:1419-1426` `w(name)` | `w0.f.a().c(bVar.f3550e)`，**默认 false**（`w0/f.java:57` `getBoolean(str,false)`） |
| 写单个启用 | `n0/g.java:1252-1258` `J(name,enable)` | 只写 `w0.f.a().i(f3550e, value)`；`Mi` 被排除 |
| 首次初始化 | `x/a.java:143-165` `l()` | 若 `antivirus_is_init` 未置位：`Mi=true`，其余按 `e()` 选出的引擎**随机选 1 个**（`j()`→`Random().nextInt(size)`），最后置 `antivirus_is_init=true` |
| `mix.scan` 分支（不是国际/国内） | `x/a.java:144`、`166-193` `n()` | `l()` 开头 `if (h0.a.a()) { n(); return; }`；`h0.a.a()` = 读 **SecurityCenter** 的 metaData `securitycenter.support.mix.scan`（非国际版才可能为真）。为真 → `n()`：首次初始化把 `e()` 选出的引擎**全部置 true**；随后按 `cloud_engine_list` 精确同步（`i(list)`）；为假 → `l()`：只随机选 1 个 |
| 每日校正 | `x/a.java:167-171`、`h()` | `h()`：`now - load_cloud_engine_time > 86400000`（24h）→ `x.b.g(ctx)` 拉云控 + `m(now)` 写时间戳；时钟回拨（差为负）时直接重写时间戳并返回 true |
| 云控拉引擎列表 | `x/b.java:114-129` `h(ctx)` | `d0.b.b(cr,"VirusScan", IS_INTL?"global":"mainland", null)` 取 JSON；解析 `[{name,scale}]`；`d(items)` 过滤：名字必须是 `f0.b` 里的 3 个之一，且 `max(1,scale)==min(scale,100)`（即 1..100 的整数） |
| 云端选主 | `x/b.java:153-176` `j(list,ctx)` | 按 `scale` 权重加权随机（`Random().nextInt(sum+1)`）选一个引擎名写入 `cloud_engine`；整表写入 `cloud_engine_list`（JSON 数组） |
| `mix.scan` 特例 | `x/b.java:167-175` | `h0.a.a()` 为真时**不写** `cloud_engine`（只写 `cloud_engine_list`） |
| 反解 cloud_engine | `x/a.java:9-24` `a()` | 读 `cloud_engine`，把**除 Mi 外**所有引擎的开关设为 `name == cloud_engine` |
| 云引擎兜底 | `x/a.java:26-36` `b()` | 遍历启用项，返回**最后一个**「非 Mi 且已启用」的引擎名；无则 `""` |
| 一致性校正 | `x/a.java:176-192` | `c().size()>2` 或 `g().contains(b())` 为假 → `k()` 重新随机；否则若云控清单不含当前引擎 → `a()` 反解 |
| 可见引擎列表 | `x/a.java:60-85` `f()` | `cloud_engine_list` 为空 → 返回**除 Mi 外**全部（AVL、TENCENT）；否则返回清单与枚举的交集 |
| 缓存有效期 | `x/b.java:133-137` | `virus_cache_validty_perod` ← 云控 `VirusScan/virusCacheValidityPeriod`，默认 `3`（天）；被 `c0/b.java:69`、`c0/d.java:97` 用于清理过期缓存 |

**【观测】默认值合成（全新安装 + 云控不可达 + `securitycenter.support.mix.scan` 未声明）**：`x.a.l()` 首次分支 → `Mi=true`，并对 `e()`（= AVL、TENCENT 两项）做 `Random().nextInt(2)` 选**恰好 1 个**置 true、另一个置 false，最后 `antivirus_is_init=true`。即默认启用 **2 个引擎**（Mi + AVL 或 Mi + TENCENT）。若 SecurityCenter 声明了 `securitycenter.support.mix.scan` → 走 `n()`，首次分支把 `e()` 的两项**全部**置 true，即默认 **3 个引擎全开**。
`cloud_engine` / `cloud_engine_list` / `load_cloud_engine_time` 初始不存在 → `f("cloud_engine")` 返回 `null`（`w0/f.java:60-66` 捕获异常返回 null）、`e("load_cloud_engine_time")` 返回 `0`。
**【观测】** `x.a.e()`（`x/a.java:87-96`）的 `if (i6==1) add; else if (i6==2) add;` 两支等价 → 实际返回「`f3554i ∈ {1,2}` 的全部引擎」，即 **AVL(2) 与 TENCENT(2)，排除 MI(3)**。
**【推断】** 因此「首次随机选 1 个云引擎」只会在 AVL/TENCENT 之间选；MI 恒定开启。这与 `J()` 不允许关闭 MI、`e()` 排除 MI 完全自洽。

### 4.6 三个本地判定库与 `getRiskAppData` 的灰产判定

| 库文件 | Helper | 表 | 关键列 | 写入方 | 读取方 |
|---|---|---|---|---|---|
| `miui_antivirus.db` v2 | `i0/b.java` | `antivirus_db_version(version, release_time)`；`antivirus_db(package, version, signatures, md5, type, virus_name, description, strategy, descType)` | `type`、`strategy`、`md5` | `i0/d.java:102-185`（服务端 `/antivirus/viruses` 下发） | `i0/l.java:308`（`package=?` 逐条比对） |
| `mi_engine_white_virus.db` v1 | `i0/e.java` | `mi_virus_white_db(package, signatures, path)`；`mi_detect_app_db(package, signatures)` | `package`+`signatures` | `i0/f.java:29-72`（服务端 `/antivirus/whitelist`） | `i0/f.java:77-118` |
| `risk_gray_result.db` v1 | `c0/a.java` | `risk_gray_result_db(packageName, versionCode, signatures, type, time)` | 全表 | `c0/b.java:113-152`（`g(m)`）、`MiEngineNG.java:120-127`（`s()` 写 type=5） | `c0/b.java:97-121`（`e()` 全表 + 清过期） |
| `scan_result.db` v2 | `c0/c.java` | `scan_result_db(path, packageName, virusName, virusDescription, engineName, versionName, versionCode, virusLevel, extras, sign, time)` | 全表 | `c0/d.java:205-288`（`h()`/`i()`） | `c0/d.java:179-203`（`f()`） |
| `miui_virus.db` v5 | `w0/h.java` | `t_virus_white_list(..., virus_md5, dir_path, pkg_name, ...)`；`virus_white_db(...)` | `virus_md5`、`dir_path` | `GuardProvider.insert/delete` | `GuardProvider.query`、`i0/f.java:88-111` |

**`getRiskAppData` 的完整判定链（`GuardProvider.java:103-127` + `129-157`）**

```
c0.b.f(ctx).e()      // c0/b.java:97
  └─ a() 先清过期：DELETE FROM risk_gray_result_db WHERE time < now - (virus_cache_validty_perod 天)*86400000
  └─ SELECT * FROM risk_gray_result_db → List<i0.m>
for pkg in getInstalledPackages(0):
   if (pkg.applicationInfo.flags & 1) == 1: continue            // 跳过系统应用
   rec = first(r in list where r.f5034c==pkg.packageName && r.f5033b==pkg.versionCode)
   if rec == null: continue
   if (i0.f.a(ctx).e(pkg)):                                     // 白名单豁免
        c0.b.f(ctx).b(pkg.packageName)   // 从 risk_gray_result_db 删除该行
        continue
   out.add({packageName, versionCode, type, signaturesMd5})
```

**「什么条件算灰产」**
- **【观测】** `risk_gray_result_db` 的 `type` 列在两个写入点被赋予：`MiEngineNG.s()` 恒写 `5`（`MiEngineNG.java:121-126`）；`i0/l.java:348-352` 在「病毒库里 `type==5` 且签名匹配」时也写 `5`，随后把内存记录的 `f5032a` 改回 `0`。
- **【观测】** `i0/l.java:341-356` 的分支语义：`type==0` → 日志「unknown or safe apk」；`type==5` → 日志「update riskgraydb」并落库。
- **【推断】** 因此：**`type==5` 就是"灰产"标记**，来源于 ① 服务端 `antivirus_db.type==5`（云端灰产库）② MiEngineNG 的黑灰产扫描管线（`scan_params` bit2 打开时）。
- **【观测】** `antivirus_db.type` 的取值语义（由 `g0/e.java:40-48` 反推）：`0/1 → level 1（安全）`、`2 → level 3`、`3 → level 2`。即 `type` 是与 `virusLevel` 不同的一套编码。

**`i0.f.a(ctx).e(pkginfo)` 白名单判定怎么实现**（`i0/f.java:77-118`）

```
e(PackageInfo info):
  └─ sigs = i0.g.a(new PackageParser.Package(info.packageName), info)   // 分 API 29/28/<28 三路取签名
  └─ return d(info.packageName, i0.h.c(sigs))
d(pkg, sigMd5):
  └─ if (b(pkg, sigMd5)) return true;        // b() 直接 return false —— 预留 hook，恒不生效
  └─ SELECT package, signatures FROM mi_virus_white_db
        WHERE package = ? AND signatures = ?
     return cursor.getCount() > 0;
```
**【观测】** `i0/f.java:37-39` 的 `private boolean b(String,String) { return false; }` 是**恒 false 的死 hook**（可能是被裁掉的内置白名单）。
**【观测】** `i0/f.java:88-111` 的 `f(path)` 是另一条白名单路径：`w0.a.a(path)`（全文件 MD5）→ `query("content://guard/virus", null, "virus_md5 = ?")` **回调自己的 Provider**。
**【推断】** 因此白名单有两个维度：`(包名, 签名MD5)` 走本地表；`文件MD5` 走自家 Provider 的 `t_virus_white_list`。

### 4.7 QAX（奇安信）引擎是否实现

| 证据 | 结论 |
|---|---|
| `res/values/arrays.xml:23-32` `qax_string_info` 8 项齐全；`res/values/public.xml` 有 `QAX=0x7f0b0006`、`DesQAX=0x7f0b0003`、`qax_dialog_*` 6 个 | 资源存在【观测】 |
| `res/values/strings.xml:33-38` 6 个 `qax_dialog_*` 值全部是字面量 `TODO` | 文案占位【观测】 |
| `f0.b` 枚举**只有 3 个常量**（REA `ev_80d5f9…`：字段 `f3543j`/`f3544k`/`f3545l` + 数组 `f3546m`） | 无 QAX 常量【观测】 |
| 全树 grep `QAX|qax` 只命中 `com/tencent/tmsecure/R.java`（TMSDK 自带的 R 别名表，12 行）与上述资源 | **dex 中无 QAX 引擎类**【观测】 |
| 无 `com/miui/guardprovider/engine/qax/` 目录 | 【观测】 |
| **结论** | **QAX 引擎未实现**：仅残留资源与 TMSDK 的 R 常量。`qax_string_info` 无任何 Java 代码引用（`f0.b` 里只有 `f3553h` 分别指向 `0x7f030000`/`0x7f030001`/`0x7f030003`，跳过 `0x7f030002`）【观测+推断】 |

### 4.8 威胁类型枚举 → `virusType` 数值映射

| 项 | 观测 |
|---|---|
| 枚举资源 | `res/values/strings.xml:53-64`：`threat_type_adware_desc`、`backdoor`、`default`(0x7f0b0034)、`exploit`、`hacktool`、`monitoring`、`rootkit`、`spyware`、`trackware`、`trojan`、`virus`、`worm` —— 共 **12 个** |
| Java 引用 | 只有 `com/tencent/tmsecure/R.java:725-758` 的 12 个 int 常量；**业务代码 0 处引用** |
| `virusType` 赋值 | 仅 `AvlEngine`(41,57) 与 `TencentEngine`(377) 写 `1`；`MiEngine`/`MiEngineNG`/`g0.e` 不写（默认 0） |
| 结论 | **12 个威胁类型没有对应的数值枚举**。`y.f.virusType` 实际是二值：`1` = 「有检出（AVL/腾讯）」、`0` = 「MI 引擎/未赋值」。真实威胁分类靠字符串 `virusName`（如 AVL 的 `risk/xxx`）与 `virusLevel`。广播与 `MatrixCursor` 只是把它透传出去。 |
| 残留原因推断 | 这套 `threat_type_*` 文案应是给 SecurityCenter 展示用、由上层按 `virusName` 自行映射；GuardProvider 侧只做透传。 |

### 4.9 native 桥（`natives`）与唯一调用点

| 项 | 观测 |
|---|---|
| 加载 | `RustSupport.<clinit>` → `System.loadLibrary("flamingo")`（`RustSupport.java:4-6`） |
| native 方法（REA `ev_0c030a…`） | `static native int matches(byte[], String)`、`static native int search(byte[], String)`、`static native boolean[] search(byte[], String[])`（overload_index 0/1） |
| 库文件 | `lib/arm64-v8a/libflamingo.so`，1 506 088 字节（另有 11 个 .so，合计 6.50 MB） |
| 唯一调用点 | `p0/y.java:111`（`matches`）、`p0/y.java:117`（`search(byte[],String)`）；`search(byte[],String[])` **无调用点** |
| 语义 | `y.e(e.a file, StringItem item)`：`Files.size(path) > 20971520`（20 MB）→ 跳过并返回 false；否则从 `LruCache` 取文件字节 → 调 native → `-1` 视为不匹配 |
| 异常 | `catch (RustException)` 打 `Log.e("CodeScanner","bad input characteristic:")`；`ClosedFileSystemException` → 抛 `CancellationException("External cancellation")` |
| 【未知】 | `libflamingo.so` 内部实现（Rust 侧特征码引擎）未反汇编 |

### 4.10 高危行为清单（逐条定位）

| # | 类型 | 位置 | 观测 | 风险判定 |
|---|---|---|---|---|
| R1 | 动态 native 加载（**可写路径**） | `tmsdkobf/ka.java:12-74` | 优先从 `TMSDKContext.getStrFromEnvMap(PRE_LIB_PATH)` 或 `cacheDir` 推导出的 `.../lib/` 目录 `System.load(绝对路径 + 名字)`，失败才 `System.loadLibrary`；尝试 `name`/`name.so`/`libname.so` 三种 | 【推断】若 `/data/data/<pkg>/lib/` 可写（同签名应用或 root），即为 so 劫持面。属 TMSDK 内部，非本应用直接调用点 |
| R2 | 动态 native 加载（固定名） | `com/avl/engine/a/c.java:223`、`com/avl/engine/a/d.java:107` | `System.loadLibrary("avlasys")` / `System.loadLibrary("urldetectorsys")`（参数是常量） | 低（常量库名） |
| R3 | 命令执行 | `tmsdkobf/t9.java:84-95` | `Runtime.getRuntime().exec("ping -i 0.2 -c<n> -w <t> " + host)`，host 来自 `k.a().c(str, observer, str2)` → `detectARPNoRoot` 链路（`n0/k.java:80-86`） | 【推断】无 shell 元字符过滤 → 若 `str2`（IP/域名）可由 IPC 调用方控制，构成命令注入面。IPC 受 signature 权限保护，实际需 SecurityCenter 才能触达 |
| R4 | 进程执行（只读信息） | `xcrash/j.java:293`、`com/xiaomi/onetrack/util/b.java:369` | `new ProcessBuilder(new String[0]).command(arrayList).start()` 读取系统命令输出 | 低（读 logcat/getprop 类） |
| R5 | 反射 | `d0/c.java:7-11` | 通用 `getDeclaredMethod + setAccessible(true) + invoke(null, ...)`；被 `d0/b`（MiuiSettings）、`d0/d`（SystemProperties）、`d0/a`（AppOpsUtils）复用 | 中（反射绕过 hidden-API 限制，读云控与系统属性） |
| R6 | 反射 | `v/a.java:22-38` | `Class.forName("com.android.id.impl.IdProviderImpl")` + `getUDID/getOAID/getVAID/getAAID` | 中（设备标识采集） |
| R7 | 反射 | `c5/a.java:114-121` | `Class.forName("miui.os.Build").getDeclaredField("IS_MIUI_LITE_VERSION")` | 低 |
| R8 | 反射 | `n0/i.java:10-15` | `bVar.f3549d.getConstructor(f0.b.class).newInstance(bVar)` 反射构造引擎 | 低（枚举自带的 Class 字段） |
| R9 | 隐藏 API | `w0/d.java:28-38`、`i0/g.java:11-50`、`n0/g.java:821` | `android.content.pm.PackageParser`（隐藏类）/`PackageParser.PackageLite`/`mSigningDetails` | 中（依赖平台私有 API，plat 升级即碎） |
| R10 | 跨应用授权 | `w0/e.java:8-17` | `contentResolver.call(Uri.parse("content://com.lbe.security.miui.su"), "GrantGuardProvider", null, null)` | 中（向 LBE 安全组件请求授权，绕过存储权限） |
| R11 | 跨应用私有库读取 | `tmsdkobf/nb.java:36,74,126` | `SQLiteDatabase.openDatabase("/data/data/com.qiyi.video/databases/qyvideo.db", …)`、`com.sohu.sohuvideo/files/databases/sohutv.db`、`.../other.db` | **高**（读第三方应用私有目录绝对路径）。属 TMSDK 离线视频模块，本应用是否可达【未知】（未见调用方） |
| R12 | 全盘文件枚举 | `n0/g.java:1195-1226` | `MediaStore.Files` 查 `_data LIKE '%.apk'` 全量 + 逐个 `getPackageArchiveInfo` | 中（读外部存储所有 apk 元数据） |
| R13 | 任意文件 MD5 | `w0/a.java:11-30`、`i0/f.java:88` | `FileInputStream` 读任意路径算 MD5，再查 `virus_md5` | 中（配合 `parseApk` 可探测路径存在性/内容哈希） |
| R14 | 任意路径写/删 | `GuardProvider.java:213-267` | `cacheDir/copy.apk` 的 `Files.copy(REPLACE_EXISTING)` + `Files.deleteIfExists` | 低（固定文件名、限定 cacheDir） |
| R15 | 跨应用广播（信息外泄） | `n0/h.java:8-30` | `TRACK_ANTIVURS_DATA` 携带 **15 个** extra（`path`、`packageName`、`virusName`、`virusDescription`、`engineName`、`versionName`、`adName`、`adDescription`、`versionCode`、`virusType`、`virusLevel`、`adLevel`、`isAuthoritative`、`isInCache`、`extras`）发给 `com.miui.securitycenter` | 中（`setPackage` 限定单收件人，但 **未设 receiverPermission**；实际只被同签名包接收） |
| R16 | 设备指纹 | `h0/b.java:31-34`、`128-146` | 静态读 `ro.product.device`、`ro.carrier.name`、`ro.product.mod_device`、`MIUI-<INCREMENTAL>`；`mi` = `MD5(android_id + "-" + appKey)` | 中（`android_id` + 设备型号上报到 `api.sec.miui.com`） |
| R17 | IMEI/IMSI（**间接触发**） | `com/xiaomi/onetrack/Configuration.java:86-89`、`269-292`；`com/xiaomi/onetrack/util/DeviceUtil.java:305-356,668` | `Configuration.Builder` 的 `f1855h`(IMSI)/`f1856i`(IMEI) **默认 true**（`86`、`89` 行），`Configuration` 构造时按 builder 取值（`284-285`）；`DeviceUtil` 用反射 `getImei(int)`、`getDeviceId()`、`getSubscriberId()` | **高**：`z/b.java:25` 建 `Configuration` 时**未**调用 `setImeiEnable(false)`/`setImsiEnable(false)` → 默认允许采集 IMEI/IMSI。首次 `z.a.c()`（`n0/g.java:1147`，每次扫描必走）即初始化 OneTrack |
| R18 | 独立 SharedPreferences（非 GuardProvider 名） | `com/avl/engine/c/f.java:14,35`、`tmsdkobf/q5.java:20`、`tmsdkobf/m7.java:20` | AVL 用 `"avl_conf"`/`"avl_<x>"`；TMSDK 自建 SP | 低 |
| R19 | `serialVersionUID`/`SharedPreferences` 明文 UUID | `w0/b.java:19-25` | `key_uuid` 首次随机 UUID 落 SP，作为上报身份 | 中（持久化设备标识） |
| R20 | 无 `SystemProperties.set` | 全树 | 只有读，无写 | — |

---

## 5. 配置项 / 常量 / 协议字段

### 5.1 `n0.g`（AntivirusManager，`g.z()`）完整方法表

文件：`n0/g.java`（1450 行）。字段：静态 `public static int f6412h`（活跃任务数）、`public static final HashMap f6413i`（taskId→任务）、`private static volatile g f6414j`（单例）；实例 6 个 `ConcurrentHashMap`：`f6415a`(observer→path→已完成引擎数)、`f6416b`(observer→任务列表)、`f6417c`(observer→结果列表)、`f6418d`(observer→path→签名缓存)、`f6419e`(observer→灰产标记)、`f6420f`(observer→scanType)、`f6421g`(最后一次 `L()` 的 AsyncTask)。

**公开方法**

| 行 | 签名 | 语义 |
|---|---|---|
| 1228 | `static g z()` | 单例；首次构造执行 `x.a.l()`（引擎启用初始化 + 可能云控拉取） |
| 1239 | `void C(y.c observer, boolean useCloud)` | **增量扫描**：`new n0.g$e(observer,useCloud).executeOnExecutor(THREAD_POOL_EXECUTOR)`（对应 AIDL 事务 20 `u`） |
| 1243 | `boolean F(String engineName)` | 「是否支持云扫」→ 返回 `f0.b` 常量的 `f3551f`（三引擎都 `true`，与引擎无关的恒真值） |
| 1252 | `void J(String engineName, boolean enable)` | 写引擎启用到 SP `sp_key_*_engine_enable`；**`Mi` 永不被写** |
| 1260 | `int L(String[] paths, y.c observer, boolean useCloud, int scanType, String extras)` | **同步/全量扫描主入口**：过滤 → 限流(≥10 拒绝) → 建 6 张表 → 起 `n0.g$f`；返回 `f6412h`（任务数） |
| 1296 | `int M(String[] paths, y.c observer, boolean useCloud, int scanType, String extras)` | 场景分发：`scanType==1` → `C()` 增量；否则 → `L()` |
| 1305 | `int N(y.c observer)` | **病毒库更新任务**：`≥10` 拒绝；`u(observer,null)` + `n(t(x.a.c()))` + 线程池跑 `task.s()`；返回 taskId |
| 1330 | `List O(List<String> pkgs)` | 按**包名**过滤白名单：`i0.h.d()` 取 PackageInfo → `i0.f.a().e(pkg)` 或 `.f(sourceDir)` 命中则从列表移除 |
| 1352 | `List P(List<String> paths)` | 按**apk 路径**过滤白名单：`i0.h.f()` + `i0.f.a().e()/.f(path)` |
| 1372 | `void q(int taskId)` | **cancelScan**：反查 observer → 逐子任务 `i()`（停引擎、`I(id)`、清 handler）；最后 `f6421g.cancel(true)` |
| 1407 | `void r(int taskId)` | **cancelUpdate**：`A(taskId).j()` → 各引擎 `k()` |
| 1415 | `int s()` | **checkUpdate 桩：恒 `return -1`** |
| 1419 | `boolean w(String engineName)` | 读 SP 启用状态（默认 false） |
| 1428 | `String[] x()` | 返回引擎名数组；**`new String[4]` 只填 3 个 → 末位恒 null（bug）** |
| 1442 | `String y(String engineName)` | 引擎显示名：`getString(f3552g)` = 小米/安天/腾讯 |

**私有 / 内部方法**

| 行 | 签名 | 语义 |
|---|---|---|
| 987 | `c A(int taskId)` | 按 id 从 `f6413i` 取任务（同步块），无则 null |
| 998 | `y.f B(y.f in)` | 浅拷贝结果（10 个字段） |
| 1014 | `y.f D(y.f a, y.f b)` | **多引擎结果合并**：按 `virusLevel` 优先级 2>3>6>4>5>1 覆盖，`extras` 拼接；`packageName` 为空时补 |
| 1090 | `static c0.d.a E(PackageInfo, List cache)` | 缓存命中：包名+versionName+签名MD5+sourceDir 四元组全等 |
| 1107 | `static boolean G(List, f0.b)` | `list.contains(name)` 谓词 |
| 1112 | `static boolean H(String path)` | `new File(path).exists()` 谓词 |
| 1117 | `int I(int taskId)` | 从 `f6413i` 移除并 `f6412h--`；不存在返回 `-1` |
| 1132 | `void K(List list, y.c, boolean useCloud, int scanType, String extras, int offset)` | **起引擎线程**：`t(x.a.c())` 建引擎 → 每引擎 `u()` 建任务 + `n0.g$b` Thread → `start()` → `z.a.c("security_scan")` 埋点 |
| 1150 | `int p(c task)` | **addNewTask**：0..100 环形找空位，返回 taskId 并 `f6412h++` |
| 1172 | `List t(List<String> names)` | `f0.b.values()` 过滤 `names` → `n0.i().a()` 反射实例化 |
| 1190 | `c u(y.c observer, f0.a engine)` | `new c(observer, engine)`，内含 `linkToDeath` + `new b0.a(engine)` |
| 1195 | `static List v(Context)` | `MediaStore.Files` 查 `%.apk` → `i0.h.f()` 转 PackageInfo 列表 |
| 983 | `private g()` | 构造器：调 `x.a.l()` |

**内部类**

| 内部类 | 行 | 角色 |
|---|---|---|
| `a` extends `Handler` | 68-247 | 主线程进度/结束聚合；`what==2` 结束、`what==3` 单文件进度；`AsyncTaskC0152a` 负责把结果写 `scan_result_db`（仅国内版） |
| `b` extends `Thread` | 249-280 | 每引擎一个线程：`task.o(handler)` + `task.r(list,useCloud,extras)` |
| `c` implements `IBinder.DeathRecipient` | 282-647 | **单引擎任务**：持 `y.c` observer、taskId、`b0.a` 状态、Thread；方法 `k()`取id、`l()`启动引擎、`o(Handler)`、`q(Thread)`、`binderDied()`、`i()`取消、`j()`取消更新、`m(i,path)`发消息、`n(List)`、`p(id)`、`r(list,cloud,extras)`、`s()`更新；内含回调 `a`(f0.a.c 扫描回调)、`b`(f0.a.InterfaceC0073a 初始化回调)、`C0153c`(f0.a.b 更新回调)、`d`(InterfaceC0073a 更新前初始化) |
| `d` | 649-662 | 签名缓存结构：`f6453a`(path) `f6454b`(versionName) `f6455c`(versionCode) `f6456d`(sigMd5) |
| `e` extends `AsyncTask` | 664-754 | **增量扫描**：`getInstalledPackages(64)` + `v(ctx)`；缓存命中且 level∈{2,3,6} 直接回放；否则批量走 `L(...,1,null)`；内含 `BinderC0154g` 汇总回调 |
| `f` extends `AsyncTask` | 756-892 | **白名单预检**：`PackageParser.parsePackageLite` + `i0.f` 判定，命中直接回调安全结果；未命中项交 `K()` |
| `BinderC0154g` extends `o0.a` | 895-963 | 增量扫描的 observer：累计结果 → `onCacheScanFished==2` 时回调 `y.c.m(f[])` |
| `h` | 965-981 | `WhiteCheckInfo{PackageInfo, c0.d.a}` |

### 5.2 `SecurityService` Binder 完整事务表

**`IAntiVirusServer`**（descriptor `com.miui.guardprovider.aidl.IAntiVirusServer`，`y/a.java:19`；实现 `SecurityService$a`）

| code | onTransact 参数（`y/a.java`） | 方法 | 语义 | 用户锁定时（`SecurityService.java`） |
|---|---|---|---|---|
| 1 | `String[]`, `IVirusObserver`, `int→bool` | `w(arr,c,useCloud)→int` | startScan（scanType=0） | `c.v(-1,null)`，返回 0 |
| 2 | `String`, `int→bool` | `q(name,enable)` | setEngineEnable | 直接 return（忽略） |
| 3 | `String` | `g(name)→boolean` | getEngineEnable | 返回 `false` |
| 4 | `String` | `P(name)→boolean` | isSupportCloudScan | **不判锁**，直接 `g.z().F()` |
| 5 | `int` | `N(id)` | cancelScan | 忽略 |
| 6 | `IVirusObserver` | `C(c)→int` | checkUpdate（恒 -1） | 返回 `-1` |
| 7 | `IVirusObserver` | `h(c)→int` | updateScan（病毒库更新） | 回 `c.x({engine:"",type:2})` + `c.K(-1)`，返回 0 |
| 8 | `int` | `k(id)` | cancelUpdate | 忽略 |
| 9 | — | `getVersion()→String` | 恒 `"1"` | 恒 `"1"` |
| 10 | `String` | `J(name)→String` | getNameString（显示名） | **不判锁** |
| 11 | — | `i()→String[]` | 引擎名数组（末位 null，bug） | **不判锁** |
| 12 | `String`, `IWifiDetectObserver` | `B(name,d)→int` | wifiDetectNetworkState | 返回 0 |
| 13 | `String`, `IWifiDetectObserver` | `D(name,d)→int` | wifiDetectDnsAndPhishing | 返回 0 |
| 14 | `String`, `IWifiDetectObserver`, `String` | `O(name,d,ip)→int` | wifiDetectArp | 返回 0 |
| 15 | `String`, `IWifiDetectObserver` | `Q(name,d)→int` | wifiDetectSecurity | 返回 0 |
| 16 | — | `j()→String[]` | detectAppsScan 结果（`basisOf==2` 的包名） | 返回 `new String[0]` |
| 17 | `String` | `G(pkg)→String` | detectAppScan 单包判定（在缓存名单则回原名，否则 null） | 返回 `null` |
| 18 | — | `A()→int` | detectAppsScan 上传（`/detect/app`） | 返回 `-1` |
| 19 | — | `e()→boolean` | 病毒缓存是否非空 | **不判锁** |
| 20 | `IVirusObserver`, `int→bool` | `u(c,useCloud)` | **增量扫描**（`g.z().C()`） | **不判锁（唯一无锁检查的写操作）** |
| 21 | `List<String>` | `f(list)→List` | 按包名过滤白名单 | **不判锁** |
| 22 | `String`, `int` | `E(str,type)` | 包变更通知：`2/3`→异步清 `scan_result_db`+`risk_gray_result_db`；`1`→增量更新病毒库 | **不判锁** |
| 23 | `List<String>` | `M(list)→List` | 按 apk 路径过滤白名单 | **不判锁** |
| 24 | `String[]`, `IVirusObserver`, `int→bool`, `int` | `p(arr,c,useCloud,scanType)→int` | startScanAsScene | `c.v(-1,null)`，返回 0 |

**`IURLScanServer`**（descriptor `com.miui.guardprovider.aidl.IURLScanServer`，`y/b.java:18`）

| code | 参数 | 方法 | 语义 | 锁定 |
|---|---|---|---|---|
| 1 | `String`, `String` | `H(engineName,url)→int` | `j.a().b()` → `UrlScanManager` → `f0.a.m(1,url)`（只有 AVL 实现：`AVLEngine.scanURL`） | **不判锁** |
| 2 | `String` | `t(engineName)→int` | `j.a().c()` → `f0.a.m(2,null)` → `AVLEngine.updateURLRule()` | 返回 `-2` + 日志 |

**`IVirusObserver`**（callback，descriptor `com.miui.guardprovider.aidl.IVirusObserver`，`y/c.java:202`）

| code | 方法 | 语义（据 `n0/g` 调用点） |
|---|---|---|
| 1 | `d(int)` | onScanStarted |
| 2 | `b(int)` | 未使用的回调位 |
| 3 | `c(int, String)` | onScanStartItem（`n0/g.java:803`） |
| 4 | `s(int,int, VirusInfo[])` | onScanProgress（当前/总数/增量结果） |
| 5 | `v(int, VirusInfo[])` | onScanFinished（taskId + 全量结果） |
| 6 | `r(int)` | 未使用 |
| 7 | `L(int,int)` | onScanPaused / 二值进度 |
| 8 | `y(int)` | onUpdateCanceled（`n0/g.java:468`） |
| 9 | `K(int)` | onUpdateFinished（`n0/g.java:438`） |
| 10 | `a(int)` | onCheckStarted（`n0/g.java:457`） |
| 11 | `l(int)` | 未使用 |
| 12 | `x(EngineUpdateInfo)` | onUpdateItemFinished（`n0/g.java:429`） |
| 13 | `m(VirusInfo[])` | onCacheScanFinished（`n0/g.java:718/741/956`） |

**`IWifiDetectObserver`**（`y/d.java`）：仅 `onResult(int)`（code 1）。

### 5.3 `y.f`（VirusInfo）完整字段表

`y/f.java`，implements `Parcelable, Comparable`。

| 字段 | 类型 | 语义 | 赋值方 | 序列化 |
|---|---|---|---|---|
| `f11657b` | String | `path`（apk 绝对路径；content:// 时是 cacheDir/copy.apk） | 各引擎 | `writeToParcel` ① |
| `f11658c` | String | `packageName` | 各引擎 / 白名单填充 | ② |
| `f11659d` | String | `virusName` | AVL `info.getVirusName()`、TENCENT `entity.virusName`、NG `threatName` | ③ |
| `f11660e` | String | `virusDescription` | AVL `getVirusDescription()`、TENCENT `virusDiscription`、NG `kVar.b()` | ⑥ |
| `f11661f` | String | `engineName`（`d()` 的返回值：`"Mi"`/`"AVL"`/`"TENCENT"`；NG 硬编码 `"MiEngineNG"`） | 各引擎 | ⑦ |
| `f11662g` | String | `versionName` | AVL/TENCENT/NG 填；`n0/g.java:139` 用缓存兜底 | ⑧ |
| `f11663h` | String | `adName`（**全树 0 赋值，未实现**） | — | ⑨ |
| `f11664i` | String | `adDescription`（**全树 0 赋值，未实现**） | — | ⑩ |
| `f11665j` | int | `versionCode` | 各引擎 | 不序列化（Parcel 里丢失） |
| `f11666k` | int | `virusType`（实际只 0/1，见 §4.8） | AVL/TENCENT 写 1 | ④ |
| `f11667l` | int | **`virusLevel`**：1=安全、2=病毒/最高危、3=风险、4=?、5=?、6=灰产/反诈 | 各引擎 + `n0/g` 白名单降级 | ⑤ |
| `f11668m` | int | `adLevel`（**全树 0 赋值，未实现**） | — | ⑪ |
| `f11669n` | boolean | `isAuthoritative`（是否「权威」判定，只有 `g0/e.c()` 在 `i0.m.type ∈ 1..3` 时置 true） | MiEngine 路径 | **不序列化** |
| `f11670o` | boolean | `isInCache`（标志位，**全树 0 赋值**） | — | **不序列化** |
| `f11671p` | String | `extras`；格式 `"<引擎>:<safe\|danger>;"`，多引擎合并时**字符串拼接** | AVL/TENCENT/NG/g0.e | ⑫ |

**`extras` 实测取值**（可作为"哪个引擎判定"的指纹）：`"Mi:safe;"`/`"Mi:danger;"`（`g0/e.java:71-73`）、`"AVL:safe;"`/`"AVL:danger;"`（`AvlEngine.java:44-46`）、`"TENCENT:safe;"`/`"TENCENT:danger;"`（`TencentEngine.java:382-384`）、`"MiEngineNG:safe;"`/`"MiEngineNG:danger;"`（`MiEngineNG.java:62,168,180`）、`"AntiDefraud:danger;"`（`g0/e.java:29`）。

**`Comparable` 排序权重**（`y/f.java:77-97`）：`isAuthoritative` → 100；level 1→0、2→5、3→4、4→2、5→1、6→3、其它→-1。用于 `n0/g.java:338` 的「同 path 取最严重」缓存策略。

**Parcel 不对称缺陷【观测】**：`writeToParcel` 写 12 个值（含 `f11668m`、`f11671p`），`f(Parcel)` 读 10 个（`f11657b,c,d,k,l,e,f,g,p`）。`f11668m` 写了不读 → 跨进程后 `adLevel` 丢失；`f11665j`(versionCode)、`f11669n`、`f11670o` 完全不参与序列化。

### 5.4 SharedPreferences 完整键表

文件名 `"GuardProvider"`，mode `0`（`w0/f.java:20`）。`w0.f` 提供 `c()`=getBoolean(默认 false)、`d(k,def)`=getInt、`e(k)`=getLong(默认 0)、`f(k)`=getString(默认 null)、`g/h/i/j`=put+**commit()**（同步落盘）。

| 键 | 类型 | 读 | 写 | 默认/语义 |
|---|---|---|---|---|
| `sp_key_mi_engine_enable` | boolean | `n0/g.java:1422`（经 `f0.b.MI.f3550e`） | `n0/g.java:1255`、`x/a.java:148,180` | false；**`J()` 拒绝写 Mi** |
| `sp_key_avl_engine_enable` | boolean | 同上 | 同上 | false |
| `sp_key_tencent_engine_enable` | boolean | 同上 | 同上 | false |
| `cloud_engine` | String | `x/a.java:16` | `x/b.java:174` | null；当前云引擎名（"AVL"/"TENCENT"） |
| `cloud_engine_list` | String(JSON 数组) | `x/b.java:32`、`x/a.java:67`（经 `b.a()`） | `x/b.java:170` | null；云控下发的引擎名集合 |
| `antivirus_is_init` | boolean | `x/a.java:147,178` | `x/a.java:152,184` | false；首次初始化哨兵 |
| `load_cloud_engine_time` | long | `x/a.java:52` | `x/a.java:173` | 0；上次云控拉取时间（ms） |
| `virus_cache_validty_perod` | int | `c0/b.java:69`、`c0/d.java:97` | `x/b.java:135` | 3（天）；**注意拼写 `validty`（少一个 i）** |
| `key_uuid` | String | `w0/b.java:19` | `w0/b.java:24` | null；首次生成 `UUID.randomUUID()` |
| `avl_url_sdk_version` | String | — （只写不读） | `AvlEngine.java:207` | null；AVL URL 库版本号 |

**【观测】** 全树 grep `sp_key_`/`cloud_engine`/`antivirus_is_init`/`key_uuid`/`avl_url_sdk_version`/`virus_cache` 共命中 10 个键，**无遗漏**（`_obf_index.tsv` 亦无第 11 个键）。
**【观测】** 另有 4 组**非本 SP** 的偏好文件（属第三方库）：`avl_conf` 及 `avl_<后缀>`（`com/avl/engine/c/f.java:14,35`）、TMSDK 自建（`tmsdkobf/q5.java:20`、`tmsdkobf/m7.java:20`）、OneTrack 自建（`com/xiaomi/onetrack/util/aa.java:109`）。

### 5.5 引擎元数据（`engine_info_list` 的 11 列与 8 个字符串）

`f0.b` 三个常量的完整参数（`f0/b.java:15-17`）：

| 常量 | `f3547b` 内部名 | `f3548c` code | `f3549d` 实现类 | `f3550e` SP 键 | `f3551f` 云扫 | `f3552g` 名称资源 | `f3553h` 数组资源 | `f3554i` 分组 |
|---|---|---|---|---|---|---|---|---|
| `MI` | `"Mi"` | 100 | `MiEngineNG`（云控可切 `MiEngine`） | `sp_key_mi_engine_enable` | true | `0x7f0b0005` | `0x7f030001` | 3 |
| `AVL` | `"AVL"` | 11 | `AvlEngine` | `sp_key_avl_engine_enable` | true | `0x7f0b0000` | `0x7f030000` | 2 |
| `TENCENT` | `"TENCENT"` | 10 | `TencentEngine` | `sp_key_tencent_engine_enable` | true | `0x7f0b0007` | `0x7f030003` | 2 |

**8 个字符串数组项的列语义**（`GuardProvider.java:389` 的 `arraycopy(..., 1, 8)` → 列 1..8）：

| 数组下标 | `MatrixCursor` 列 | 资源名（avl/tencent） |
|---|---|---|
| 0 | `NameString` | `@string/AVL` / `@string/Tencent` |
| 1 | `DescriptionName` | `@string/DesAVL` / `@string/DesTencent` |
| 2 | `DialogTitle` | `avl_dialog_title` / `tencent_dialog_title` |
| 3 | `DialogHeader` | `avl_dialog_header` / `tencent_dialog_header` |
| 4 | `DialogPolicy` | `avl_dialog_policy` / `tencent_dialog_policy` |
| 5 | `DialogCloudScan` | `avl_dialog_cloudscan` / `tencent_dialog_cloudscan` |
| 6 | `DialogSummary` | `avl_dialog_dbupdate` / `tencent_dialog_dbupdate`（**数组项叫 dbupdate，列名叫 Summary**） |
| 7 | `DialogUrl` | `avl_dialog_url` / `tencent_dialog_url` |

**逐引擎实际取值（`res/values/strings.xml` 默认=en，`values-zh-rCN` 中文）**

| 项 | AVL（en / zh-rCN） | TENCENT（en / zh-rCN） | MI（en / zh-rCN） |
|---|---|---|---|
| NameString | `AVL` / `安天` | `Tencent` / `腾讯` | `Xiaomi` / `小米` |
| DescriptionName | `AVL` / `安天AVL` | `Tencent` / `腾讯` | `Xiaomi` / `小米` |
| DialogTitle | `"AVL's Privacy Policy"` / `病毒扫描` | `Tencent’s Privacy Policy` / `病毒扫描` | *（空串）* |
| DialogHeader | `"This feature encrypts and uploads your device's ID to AVL's servers (%s)."` / `本功能使用了安天AVL提供的杀毒引擎，详情可见%s` | `"This feature encrypts and uploads your device's ID to Tencent's servers (%s)."` / `本功能使用了腾讯提供的杀毒引擎，详情可见%s` | *（空串）* |
| DialogPolicy | `Privacy Policy` / `《安天AVL移动反病毒引擎许可协议》` | `Privacy Policy` / `《腾讯手机管家软件许可协议》` | *（空串）* |
| DialogCloudScan | *（空串，资源未定义）* | `Upload and scan app names and details using online virus definitions.` / `我们会上传应用包名和应用的特征至云病毒引擎进行扫描` | *（空串）* |
| DialogSummary | *（空串）* | `Update virus definitions automatically` / `我们会上传加密后的IMEI至服务器用于更新病毒库` | *（空串）* |
| DialogUrl | `http://api.sec.miui.com/policy/proxy?p=antiy&l=<zh_CN\|en_US\|zh_TW>` | `http://api.sec.miui.com/policy/proxy?p=tencent&l=<…>` | *（空串）* |

**【观测】** `mi_string_info` 的 6 个 `<item>` 是空的（`arrays.xml:14-22`）→ MI 引擎在 UI 上只有名字、没有隐私文案。
**【观测】** `values-zh-rTW` 也完整存在（安天/腾讯台湾用词），`values-en-rGB` **不存在**该组字符串（回落默认值）。

#### 5.5.1 「文案 vs 实际实现」隐私对照

| 引擎 | 声明文案（zh-rCN） | 代码实际行为 | 一致性 |
|---|---|---|---|
| TENCENT | 「我们会上传应用包名和应用的特征至云病毒引擎进行扫描」 | `TencentEngine.j()` 把 `path` 交给 `QScannerManagerV2.scanUninstallApks(scanType, [path], listener, 0)`；`scanType=6`（本地+云，当 `b()&&z6`）否则 `2`。上传内容由 TMSDK 内部决定，本应用只传路径 | **部分一致**（云端特征上传确实发生，但由 SDK 内部实现，本应用不可控） |
| TENCENT | 「我们会上传加密后的IMEI至服务器用于更新病毒库」 | 本应用侧：`TencentEngine.e()` 只做 `TMSDKContext.setDeviceInfo(ctx, Settings.Secure.getString(cr,"android_id"), Build.MODEL)`（`TencentEngine.java:402`）。**没有任何 `getImei`/`getDeviceId` 调用**（全树 grep 仅在 OneTrack 的 `DeviceUtil` 里，见下）。TMSDK 内部是否读 IMEI：【未知】（未反汇编 `libTmsdk-2.0.13-mfr.so`） | **文案超出可见实现**：本应用传的是 **android_id + Build.MODEL**，不是 IMEI |
| AVL | 「本功能使用了安天AVL提供的杀毒引擎，详情可见%s」（`DialogHeader` 说 "uploads your device's ID"） | `AvlEngine` 无任何显式上传调用；`AVLEngine.setRegion(Build.getRegion())`、`setNetworkEnabled`、`setCloudScanEnabled`、`init(ctx, AVLSdkConfig.setDeviceType(...))`。设备标识上传在 `libavlm.so` 内部 | **无法证伪**（native 内部） |
| — | — | **但**：`OneTrack`（`z/b.java:25`）的 `Configuration.Builder` **未关闭 IMEI/IMSI**，而 `Configuration.Builder` 的 `f1855h`(IMSI)/`f1856i`(IMEI) 默认 `true`（`com/xiaomi/onetrack/Configuration.java:86,89`），`DeviceUtil` 会用反射 `getImei(int)`/`getDeviceId()`/`getSubscriberId()`（`DeviceUtil.java:305-356,668`） | **实际的 IMEI 采集来自 OneTrack，而非腾讯引擎**；首次扫描（`n0/g.java:1147` `z.a.c("security_scan")`）即初始化 |

**【结论】** 腾讯的两条隐私文案中，第一条与实现方向一致（云扫），第二条「上传 IMEI」在本应用 Java 层**找不到对应实现**（只传 android_id）；而真正会读 IMEI/IMSI 的是同 APK 内的 OneTrack 埋点 SDK，且**默认开启**。这是一个「文案指向 A、实现落在 B」的偏差，值得在上层报告中单列。

### 5.6 常量 / 协议字段

**AIDL / Provider 协议**

| 项 | 值 | 位置 |
|---|---|---|
| authority | `guard` | `AndroidManifest.xml`、`GuardProvider.java:88-92` |
| URI | `engine_info_list`(1005)、`sync_scan`(1006)、`virus`(1007)、`install_scan`(1008)、`support_incremental`(1009) | `GuardProvider.java:88-92` |
| `call()` 方法名 | `"parseApk"`、`"getRiskAppData"` | `GuardProvider.java:331-334` |
| ContentProvider 权限 | `com.miui.guardprovider.permission.Guard_Provider`（signature\|privileged） | Manifest |
| Service action | `com.miui.guardprovider.action.antivirusservice` / `…urlscan` | `SecurityService.java:312-317` |
| AIDL descriptor | `com.miui.guardprovider.aidl.IAntiVirusServer` / `IURLScanServer` / `IVirusObserver` / `IWifiDetectObserver` | `y/a.java:19`、`y/b.java:18`、`y/c.java:202`、`y/d.java`（`writeInterfaceToken`） |
| 广播 action | `miui.intent.action.TRACK_ANTIVURS_DATA`（**拼写如此**），`setPackage("com.miui.securitycenter")` | `n0/h.java:13-14` |
| 广播 extras（14） | `path`、`packageName`、`virusName`、`virusDescription`、`engineName`、`versionName`、`adName`、`adDescription`、`versionCode`、`virusType`、`virusLevel`、`adLevel`、`isAuthoritative`、`isInCache`、`extras`（**实为 15 个**） | `n0/h.java:15-29` |
| 广播抑制条件 | `level ∈ {1,4,5}` 不发；`fVar==null` 不发 | `n0/h.java:11-12` |
| `parseApk` flags | bit6(64)=收集证书；bit7(128)=附带 icon+label | `GuardProvider.java:168,175` |
| `sync_scan` 返回列 | `Type, VirusName, Description, EngineName, AdType, AdName, AdDescription` | `GuardProvider.java:257` |

**密钥 / 签名盐（硬编码，3 个 UUID）**

| 值 | 用途 | 位置 |
|---|---|---|
| `94e6cec2-19dc-4bb6-823c-613f72d3ea16` | `api.sec.miui.com/antivirus/{viruses,whitelist}` 的 `sign` 盐 | `i0/d.java:55,203` |
| `6988567a-4220-4b51-bc2d-ccdec27a74a1` | `flash.sec.miui.com/detect/app` 的 `sign` 盐 | `j0/c.java:75` |
| `62b36629-ecb4-4725-9f33-ccb0fd158191` | `virus-engine.sec.miui.com` 的 query `sign` 盐 | `q0/q.java:20` |
| 签名算法（旧栈） | `MD5( Base64( sorted("k=v"&…) + "&" + salt ) )` 大写十六进制 | `h0/b.java:200-232` |
| 签名算法（新栈） | `MD5( sorted("k=v"&…) + "&" + salt )` 小写十六进制（无 Base64） | `q0/q.java:13-30` |
| 设备指纹 | `mi = MD5( android_id + "-" + salt )` | `h0/b.java:128-146` |
| OneTrack appId | `2882303761517522561`，channel = 包名，`GAIDEnable=false` | `z/b.java:22` |

**其它常量**

| 项 | 值 | 位置 |
|---|---|---|
| 最大并发扫描任务 | 10 | `n0/g.java:1272` |
| 同步等待超时 | 2 分钟 | `GuardProvider.java:250,272` |
| 引擎内部节流 | 每文件 `Thread.sleep(5)` | `AvlEngine.java:152`、`TencentEngine.java:464`、`g0/e.java:89` |
| 特征码扫描大小上限 | 20 MB（`20971520`） | `p0/y.java:103` |
| taskId 空间 | 0..100 环形 | `n0/g.java:1159-1161` |
| 云控刷新周期 | 86400000 ms（24h） | `x/a.java:63` |
| 缓存默认有效期 | 3 天 | `x/b.java:135` |
| 全局 `_global` 标志 | `ro.product.mod_device` contains `"_global"` | `d5/a.java:10` |
| `jp_kd` 特殊区域 | `ro.miui.customized.region == "jp_kd"` | `GuardApplication.java:44` |
| 网络总闸属性 | `persist.sys.sc_allow_conn` | `GuardApplication.java:40,43` |
| jp_kd 附加闸 | `Settings.Secure "antivirus_engine_network_enable" == 1` | `GuardApplication.java:45` |
| 混合扫描能力（SecurityCenter 侧） | metaData `securitycenter.support.mix.scan` | `h0/a.java:14` |
| 全量扫描能力（SecurityCenter 侧） | metaData `miui.support_full_scan` | `i0/h.java:92` |
| AVL appKey | metaData `AVL_OPENSDK_APPKEY`（`D5DFB878335A3EF4E76F60ED470B8EC8`），回落 `AVL_SDK_APPKEY` | `com/avl/engine/a/e.java:39-46` |
| AVL DB 版本键 | `AVLA_DATA_VERSION`、`AVLA_LAST_INIT_STATE` | `com/avl/engine/a/c.java:213` |
| TMSDK 更新哨兵错误码 | `-2146769239` | `TencentEngine.java:165,196,247` |
| TMSDK 更新标志 | `UpdateConfig.UPDATE_FLAG_VIRUS_BASE` | `TencentEngine.java:493` |
| AVL URL 库版本 SP 键 | `avl_url_sdk_version` | `AvlEngine.java:207` |

### 5.7 数据库与表（本层视角）

| 库 | 版本 | Helper | 表 → 关键列 |
|---|---|---|---|
| `miui_virus.db` | 5 | `w0/h.java` | `virus_white_db(_id, package, path, version_code, size, engine, local_version, extra)`；`t_virus_white_list(_id, scan_result_type, scan_item_type, app_label, desc, dir_path, pkg_name, virus_name, virus_md5)` |
| `miui_antivirus.db` | 2 | `i0/b.java` | `antivirus_db_version(_id, version, release_time)`；`antivirus_db(_id, package, version, signatures, md5, type, virus_name, description, strategy, descType)` |
| `mi_engine_white_virus.db` | 1 | `i0/e.java` | `mi_virus_white_db(_id, package, signatures, path)`；`mi_detect_app_db(_id, package, signatures)` |
| `risk_gray_result.db` | 1 | `c0/a.java` | `risk_gray_result_db(_id, packageName, versionCode, signatures, type, time)` |
| `scan_result.db` | 2 | `c0/c.java` | `scan_result_db(_id, path, packageName, virusName, virusDescription, engineName, versionName, versionCode, virusLevel, extras, sign, time)` |
| `flamingo.db` | 2（Room） | `com/miui/guardprovider/sdk/android/db/EngineDatabase` + `r0/x.java:22` | 8 表：`trust_certs`、`trust_installers`、`innocent_package`、`characteristics`、`guilty_package`、`black_gray_chs`、`black_gray_app`、`version`（7 个版本号）→ 详见 t-cloud 报告 |

**迁移路径【观测】**：`w0/h.onUpgrade` 1→2 加 `version_code,size`、2→3 加 `engine,local_version`、3→4 建 `t_virus_white_list`、4→5 加 `extra`；`onDowngrade` 空实现（数据保留）。`i0/e.onDowngrade` / `c0/a.onDowngrade` / `c0/c.onDowngrade` 则 `DROP TABLE` 后重建（**降级即清库**）。`c0/c.onUpgrade` 1→2：`RENAME TO scan_result_db_old` 后 `DROP`（等于清空重建）。

---

## 6. 权限与攻击面 / 风险点

### 6.1 入口收敛：只有 2 个 exported 组件

| 组件 | exported | 保护 | 可触达的方法 |
|---|---|---|---|
| `GuardProvider`（provider, authority `guard`） | true | `com.miui.guardprovider.permission.Guard_Provider`（**signature\|privileged**） | `query`(5 URI)、`insert`、`delete`、`call`(2 方法)、`update`(空) |
| `SecurityService`（service, 双 action） | true | 同上 | 24 个 `IAntiVirusServer` 事务 + 2 个 `IURLScanServer` 事务 |

**【推断】** 攻击面被 `signature|privileged` 严格限制：只有同签名应用（MIUI 安全中心族）或持有 `privileged` 的平台组件能调用。**但这不等于零风险**：`privileged` 意味着任何 `/system/priv-app` 下的预置应用都能调用全部 26 个事务码 —— 包括 `parseApk`（解析任意路径 apk）与 `sync_scan`（扫描任意路径）。

**申请到的权限（与本文相关的）**：`READ_PHONE_STATE`、`READ_PRIVILEGED_PHONE_STATE`、`INTERNET`、`READ/WRITE_EXTERNAL_STORAGE`(maxSdk 29)、`MANAGE_EXTERNAL_STORAGE`、`QUERY_ALL_PACKAGES`、`PEERS_MAC_ADDRESS`、`ACCESS_WIFI_STATE`、`ACCESS_NETWORK_STATE`、`RECEIVE_BOOT_COMPLETED`、`com.miui.securitycenter.permission.ACCESS_SECURITY_CENTER_PROVIDER`、`com.android.settings.permission.CLOUD_SETTINGS_PROVIDER`、`INTERACT_ACROSS_USERS`、`ACCESS_MTP`、`miui.permission.READ_AND_WIRTE_PERMISSION_MANAGER`。
**【观测】** 申请了 `RECEIVE_BOOT_COMPLETED` 但**无任何 Receiver**（Manifest 只注册了动态 `USER_UNLOCKED`）→ 冗余权限。
**【观测】** `READ_PRIVILEGED_PHONE_STATE` 存在，但本应用 Java 层无任何 IMEI 读取代码（见 §5.5.1）。

### 6.2 协议层风险

| # | 风险 | 位置 | 说明 |
|---|---|---|---|
| P1 | `parseApk` 可解析**任意路径** | `GuardProvider.java:159-190` | 无路径白名单；仅当「无 READ_EXTERNAL_STORAGE 且不在 `/data` 下」时先尝试 LBE 授权。持有 signature 权限者可用它枚举 `PackageInfo`（含 `signatures`、`versionCode`、`sourceDir`）与读取应用标签/图标 —— 等价于一个**任意 apk 元数据读取器** |
| P2 | `sync_scan` 接受 `content://` 任意 URI | `GuardProvider.java:211-215` | 会 `openInputStream` 后复制到 `cacheDir/copy.apk` → 可读调用方无权直接读的 content provider 内容（以本进程权限读取）。这是典型的 **provider 代理读取（confused deputy）** 面 |
| P3 | 固定临时文件名 `copy.apk` | `GuardProvider.java:213` | 并发 `content://` 扫描互相覆盖；且 `Files.copy(..., REPLACE_EXISTING)` 不做大小限制 |
| P4 | `getRiskAppData` 无参数、**全量导出**已装应用灰产结论 | `GuardProvider.java:103-127` | 返回所有非系统应用中被判定灰产的包名+versionCode+type+签名MD5；调用方可据此做应用画像 |
| P5 | 广播未设 permission | `n0/h.java:13-14` | `setPackage` 限定单包，但广播**没有** `sendBroadcast(intent, receiverPermission)`。任何能声明 `com.miui.securitycenter` 包名的组件（理论上不可，除非同签名/system）可接收；实际风险低 |
| P6 | 事务 20/21/22/23 **不检查用户锁** | `SecurityService.java:251-253,154-156,76-89,107-109` | 用户锁定时其它方法都降级，唯独「增量扫描 / 白名单过滤 / 包变更通知」照常执行 → 锁屏期间可被触发扫描与清库 |
| P7 | `getVersion()` 恒 `"1"` | `SecurityService.java:169-172` | 无协议版本协商机制，后续改协议无法向后兼容 |
| P8 | 事务 11 返回数组含 `null` | `n0/g.java:1429` | `new String[4]` 只填 3 个；调用方若直接 `for` 会把 null 当引擎名 |
| P9 | `install_scan` 参数不足时**静默退化** | `GuardProvider.java:415` | 3 参退化为 2 参时返回的是 `t_virus_white_list` 的查询结果（`cursorQuery` 在 1008 分支为 null，故实为 null）→ 语义漂移 |

### 6.3 主机侧高危行为

见 §4.10 的 R1–R20 表。按危害排序的前 5 条：

1. **R17 IMEI/IMSI 采集**（OneTrack 默认开启，无 `setImeiEnable(false)`）——违反「最小必要」，且与腾讯文案错位。
2. **R11 跨应用私有 SQLite 读取**（`/data/data/com.qiyi.video/...`、`com.sohu.sohuvideo/...`）——TMSDK 遗留代码，本应用可达性未知。
3. **R3 命令注入面**（`Runtime.exec("ping …" + host)`）——`host` 来自 IPC 参数。
4. **R1 可写目录 `.so` 加载**（`System.load(cacheDir 推导的 lib/ + name)`）——TMSDK 内部。
5. **R10 跨应用授权**（`content://com.lbe.security.miui.su` 的 `GrantGuardProvider`）——绕过存储权限。

### 6.4 与「已确认事实」的偏差 / 修正

| 简报原述 | 本文观测 | 影响 |
|---|---|---|
| `install_scan` = 同步扫描 3 参 `[path, bool, engineName]` | **`engineName` 不参与引擎选择**。它作为 `extras` 一路传到 `f0.a.j(List, c, boolean, String)` 的第 4 参（`n0/g.java:624` → `b0.a.f314i` → `n0/g.java:539`），而**四个引擎实现全部忽略该参数**（`AvlEngine.j` 只用 `z6`；`MiEngine.j` 不引用；`MiEngineNG.j` 不引用；`TencentEngine.j` 不引用） | 「按引擎名扫描」的语义**不存在**；引擎集合恒由 `x.a.c()`（SP 启用项）决定 |
| `guardprovider.scan_properties=2` 可能对应扫描类型 2 | 该 meta-data **全树 0 处读取** | 死配置；`2` 与 `n0/g.L()` 的 `scanType=2` 无因果关系 |
| `guardprovider.support.mix.scan=true` 控制混合扫描 | 该 meta-data **0 处读取**；混合扫描判定读的是 **SecurityCenter 的** `securitycenter.support.mix.scan`（`h0/a.java:14`） | 本应用的 meta-data 是给**别人**读的（SecurityCenter 侧读 guardprovider 的声明），不是自己用 |
| 引擎注册表 `MI("Mi",100,…)` | 一致，但需补充：`Mi` 的实现类在**枚举构造时**由云控 `VirusScan/miEngineNgEnable` 决定（`f0/b.java:56-60`），默认 `MiEngineNG` | 同一个 `MI` 常量可能是两个不同实现类 |
| 「引擎名参数如何选择引擎」 | 见上，不选择 | — |
| `i0.h.h(ctx)` = 增量支持判定 | 底层读 `miui.support_full_scan`（**全量扫描**能力位）；Provider 的 URI 名却叫 `support_incremental` | 命名与语义错位（与 Lead 结论一致） |
| （简报未提）QAX 第四引擎 | 资源齐全但**无枚举常量、无引擎类、无调用点**，6 个 `qax_dialog_*` 值为 `TODO` | QAX 未实现 |
| （简报未提）`virusType` 与 12 种威胁类型 | 12 个 `threat_type_*` 资源**无 Java 引用**；`virusType` 实际只有 0/1 | 威胁分类不在本层 |

---

## 7. 未解决问题

| # | 问题 | 已做到哪一步 | 下一步建议 |
|---|---|---|---|
| U1 | `libflamingo.so` 内部实现（`matches`/`search` 的算法、是否可被恶意特征触发 OOM） | 只确认 3 个 native 符号与唯一调用点 | 用 REA `open_binary` + Ghidra 对 `libflamingo.so` 做符号级分析（arm64） |
| U2 | TMSDK 是否真的读取/上传 IMEI 或 IMSI（腾讯文案的落实点） | 确认 Java 层只传 `android_id`+`MODEL` | 反汇编 `libTmsdk-2.0.13-mfr.so`，或抓包观察 `tmfsdk.m.qq.com` 上报体 |
| U3 | AVL 的 `libavlm.so` 上报字段（`DialogHeader` 声称上传 "device's ID"） | 确认 Java 层只 `setRegion`/`setNetworkEnabled`/`setCloudScanEnabled`/`init(deviceType)` | 反汇编或抓包 `virus-engine.sec.miui.com` |
| U4 | `tmsdkobf/nb.java` 跨应用 DB 读取是否可达 | 确认代码存在；未找到调用方 | 用 `trace_android_references` 追 `nb.b(OfflineVideo)` 的入边 |
| U5 | `tmsdkobf/t9.java` 的 `ping` 注入在实际链路上是否可被 IPC 参数控制 | 确认 `n0/k.c()` → `detectARPNoRoot` 链路存在；`str2` 未做过滤 | 追 `k.a().c()` 的 IPC 入口（事务 14）到 `t9.a()` 的完整参数流 |
| U6 | `x.a.l()` 中 `h0.a.a()` 为真时（SecurityCenter 支持 mix scan）的具体启用组合 | 确认两条分支（`l()` vs `n()`）的写法与差异 | 实测或读 SecurityCenter 侧代码 |
| U7 | `c0.b`（灰产缓存）与 `EngineDatabase.guilty_package`（Flamingo 库）的关系与优先级 | 确认两者独立存在、`getRiskAppData` 只读前者 | 属 t-obf/t-cloud 交叉范围 |
| U8 | `p0` 管线中 `scan_params` 的 bit3（禁用云扫）与 `GuardApplication.e()` 网络闸的叠加顺序 | 已知两处独立判定 | 属 t-cloud 范围 |
| U9 | `y.f` 的 level `4` 与 `5` 的语义 | 只从 `D()` 优先级与 `n0/h` 的抑制条件反推（4/5 不上报） | 需 SecurityCenter 侧消费代码 |
| U10 | `w0/e.java` 的 LBE 授权在非 MIUI 设备上的行为 | 只确认 `catch(Exception)` 静默 | 实测 |
| U11 | `f0.b.MI.f3554i=3` 这个「分组」字段的实际用途 | `x.a.e()` 只用 `1`/`2`，`3` 被排除 | 确认是否存在其它消费点（未找到） |
| U12 | 广播 15 个 extra 中 `adName/adDescription/adLevel` 恒为空 | 确认无赋值点 | 需确认是否死协议 |

---

## 8. REA 证据 ID 列表

> provider = `jadx`（JADX headless 0.7.1，reas-jadx-bridge v7）；target SHA-256 = `b0c02fa682b8ca82429b471113d0702042ac76234c85fdcd8efeb7c03d313024`。全部为**静态**证据（APK 未执行）。

| # | Evidence ID | 操作 | 对象 | 用于支撑 |
|---|---|---|---|---|
| 1 | `ev_37fb1e7ff90b4aa164d782fe3b6884a5286668e49a896189e73bdc3bc6fd4a2d` | `inspect_android_class` | `com.miui.guardprovider.GuardProvider` | 15 个方法 / 3 字段 / 2 内部类（`a`、`b`）；证明 `call`/`query`/`delete`/`insert` 的 DEX 原型与 §3.2 链路一致 |
| 2 | `ev_cdd2481ef4691e629b465b3dbf2a0e4b5c67468163e50c2215762321c8c21856` | `inspect_android_class` | `com.miui.guardprovider.manager.SecurityService` | 12 个方法、字段 `f1787a:y.a$a`、`f1788b:y.b$a`；证明两个 Binder 实现类字段 |
| 3 | `ev_f6df526e2304e20b8b94873de133e216f7b9cf614feac15cf2c9790bb9809563` | `inspect_android_class` | `com.miui.guardprovider.manager.SecurityService$a` | **24 个 AIDL 方法** + 辅助 `R`/`S`；逐条支撑 §5.2 事务表 1–24 |
| 4 | `ev_80d5f918be73df59aab7cde8f6ae521b6942ee9b74977c2baede12332debb110` | `inspect_android_class` | `f0.b` | 12 字段、**3 个枚举常量**（`f3543j/k/l` + 数组 `f3546m`）、`b():Class`；支撑「只有 3 个引擎、无 QAX」 |
| 5 | `ev_0c030a86f4a9248825ba7e3e35078c3f8fd11d8c87de7d7cb3a4c9aa12db4932` | `inspect_android_class` | `com.miui.guardprovider.natives.RustSupport` | 3 个 native 方法（`matches(byte[],String)`、`search(byte[],String)` overload 0、`search(byte[],String[])` overload 1）+ `<clinit>`；支撑 §4.9 |

**【局限（provider 自述）】** 静态反编译不证明运行期行为；方法签名为 provider 显示类型，DEX 原型来自冻结的 JADX 元数据；类清单含合成成员，源码位置未知；APK 签名、split、native 库与运行时未被该 provider 验证。


