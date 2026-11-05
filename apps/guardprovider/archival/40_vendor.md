> **归档警示（已纠错）**：这是原分析材料的脱敏归档，不能代替 [最终复核报告](../REPORT.md)。原文关于“明文 IMEI 列表”“默认配置即实际上传”、绝对覆盖率及未知 native 行为的部分结论不成立。IMEI/IMSI 的相关 DAU 列表经过 MD5，仍受区域/权限/联网门控。证书过期也不能单独推出密文无法解密。请先阅读最终报告与其中纠错表。本文删除本地身份路径，原有文件行号引用可能指未纳入发布的完整分析树。

# 内置第三方 SDK 的实际能力与被调用面（t-vendor）

> 目标：`GuardProvider-2.0.3-20231124.0.apk`（SHA-256 `b0c02fa682b8ca82429b471113d0702042ac76234c85fdcd8efeb7c03d313024`）
> 语料：JADX 全量源码 `_analysis/src/sources/`（3231 文件）、apktool `smali/`、`lib/arm64-v8a/`、`assets/`
> 标注约定：**[观测]** 源码/字节直接可见；**[推断]** 有依据的语义归纳；**[未知]** 未覆盖。
> 覆盖边界：本文只回答"SDK 能干什么 + 本 APK 实际用了哪些"，不做逐类流水账；类计数由 `Get-ChildItem -Recurse -Filter *.java | Measure-Object` 得出（com\avl=411、tmsdk=90、tmsdkobf=342、com\tencent=8、com\xiaomi\onetrack=163、xcrash=12、com\sun=12、android\content\pm=3）。

## 0. 方法学前置：本语料的 JADX「常量池替换」伪引用（必读，否则会误判调用关系）

**[观测]** 本工程使用的 JADX 输出把「与某个已打包 `public static final` 常量同值的字面量」替换成了对该常量的符号引用，并自动补 import。证据（同一机制的三处互证）：

| 现象 | 证据 | 真实字面量 |
|---|---|---|
| `new StringBuilder(PowerNest.SHARK_TCP)` 作为 `toString()` 的容量参数 | `com/avl/engine/b/a/a.java:45`、`com/avl/engine/e/a/d/f.java:4`；`tmsdk/common/roach/nest/PowerNest.java:15` `SHARK_TCP = 1024` | `new StringBuilder(1024)` |
| 全树 211 处 `case WifiDetectManager.CLOUND_CHECK_PASSWORD_LEAK_RISK /* 20 */`（含 okhttp/AVL/OneTrack 无关文件） | `a6/b3.java:670`、`b4/f.java:919` 等 | 整数 `20` |
| `com.xiaomi.onetrack.a.a.C0039a.f1965g` 被当作目录名 | `UrlDetector.java:221/228/229`；smali 侧 `apktool/smali/com/avl/engine/urldetector/UrlDetector.smali` 的 `const-string "avlurl"` | 字符串 `"avlurl"` |
| `com.xiaomi.onetrack.b.m.f2273f` 作为引擎 map key | `com/avl/engine/security/g.java:48`；`com/xiaomi/onetrack/b/m.java:22` `f2273f = "av"` | 字符串 `"av"` |
| `com.xiaomi.onetrack.util.z.f2775b` 作为 join 分隔符 | `com/miui/guardprovider/engine/mi/MiEngineNG.java:192`；`util/z.java:16` `f2775b = ","` | 字符串 `","` |
| `com.xiaomi.onetrack.g.b.f2461a` 作为超时毫秒 | `tmsdkobf/p9.java:365`；`onetrack/g/b.java:24` `f2461a = 10000` | 整数 `10000` |

**结论/用法 [推断]**：凡 `tmsdk.*`、`com.xiaomi.onetrack.*` 的"常量类"出现在与语义无关的位置（switch 标签、超时、容量、分隔符），都是 JADX 常量池替换，**不构成对该 SDK 的调用**。判定"活代码"必须看**真实 API 调用**（`ManagerCreatorX.getManager(...)`、`TMSDKContext.*`、`OneTrack.*`）或**类型出现**（字段/参数/`new`）。本文所有"实际启用"结论均按此口径，并用 smali/常量值二次核对。

## 1. 职责概述

本 APK 打包了 5 个可执行第三方子系统 + 4 组"被刻意带入的框架类"：

| SDK / 组件 | 归属 | 本 APK 内的角色 | 是否真正参与业务 |
|---|---|---|---|
| 腾讯 AVL 引擎 `com.avl.engine.**`（411 类 + `libavlm.so` 2.30 MB / `libavlurl.so` 615 KB / `libavlasys.so` 87 KB + `assets/avlsdk` 1.53 MB） | 腾讯（Antiy/AVL 引擎，小米定制） | 三个反病毒引擎之一（引擎名 `AVL`，code 11） | **是**，`AvlEngine` 全量接线 |
| 腾讯 TMSDK `tmsdk.**` + `tmsdkobf.**`（432 类 + `libTmsdk-2.0.13-mfr.so`） | 腾讯 MSUI | 引擎 `TENCENT`（code 10）：APK 扫描 + 特征库更新 + **WiFi 安全检测** | **是**，但只用了 3 个 Manager（QScannerV2 / Update / WifiDetect） |
| 小米 OneTrack `com.xiaomi.onetrack.**`（163 类） | 小米 | 埋点上报 | **是**，仅 4 个事件名 + 2 个 SDK 自动事件 |
| xCrash 2.4.9 `xcrash.**`（12 类 + `libxcrash*.so`） | 爱奇艺开源 | 崩溃捕获 | **是**，由 OneTrack 反射拉起 |
| 腾讯国密 `com.tencent.tmf.gm.**`（4 类 + `libtmf-gm.so`） | 腾讯 TMF | TMSDK 通道加密的 SM2/SM4 | **是**（被 `tmsdkobf.y5` 调用） |
| `com.sun.nio.zipfs.**`（12 类）+ `META-INF/services/java.nio.file.spi.FileSystemProvider` | OpenJDK | 让 Android 支持 `jar:`/`zip:` FileSystem | **是**，`p0.e` 用它读 APK 内 dex/manifest |
| `android.content.pm.{PackageParser,PackageUserState,VerifierInfo}` | AOSP | 不安装即解析任意 APK、取签名 | **是**，`parseApk` IPC + 各引擎取签名 |
| `dagger.hilt.**` / `androidx.room.**` / `okhttp3.internal.publicsuffix.**` / `org.apache.commons.logging.**` / `j$.util.**` / `kotlin.**` | 各家 | 编译期或兼容性残留 | **否**（见 §4.9） |

## 2. 组件与文件清单

### 2.1 AVL 引擎（`com.avl.engine`，411 类）

| 文件 | 类 | 作用 |
|---|---|---|
| `com/avl/engine/AVLEngine.java` | `AVLEngine` | 唯一公开门面（22 个 public static 方法），见 §4.1 |
| `com/avl/engine/AVLAppInfo.java` | `AVLAppInfo` | 扫描结果接口（6 个 getter） |
| `com/avl/engine/AVLSdkConfig.java` | `AVLSdkConfig` + `Builder` | 仅 2 个配置：`deviceType`(0=LITE/1=HIGH)、`logUploadUrl` |
| `com/avl/engine/AVLDebug.java` | `AVLDebug` | 日志开关（`setLogEnable`） |
| `com/avl/engine/security/AVLA.java` | `AVLA extends NativeUtils` | **JNI 主入口**（40 个 native），引擎真正能力都在这里 |
| `com/avl/engine/security/AVLScanOption.java` | `AVLScanOption` | 扫描选项位容器（本 APK 未使用，见 §4.1.3） |
| `com/avl/engine/security/FileSearcher.java` | `FileSearcher`（接口） | 文件遍历回调（`onSearch`/`includeSuffix`/`isStop`） |
| `com/avl/engine/security/JniFuncProcess.java` | `JniFuncProcess`（接口） | native 回调 `checkFuncEnable(name,int)` |
| `com/avl/engine/security/NativeUtils.java` | `NativeUtils` | 解压/遍历/哈希 native（`unzipAssetsFile`、`searchFile`、`getFileMD5`…） |
| `com/avl/engine/security/c.java` | `security.c` | 引擎单例与生命周期（`init/update/stopUpdate/scanURL`） |
| `com/avl/engine/security/g.java` | `security.g extends a.c` | 初始化装配（UrlDetector、日志上报器、SDK 配置） |
| `com/avl/engine/security/i.java` | `security.i` | 扫描模式与设备内存分档（默认 `f1715a=6`） |
| `com/avl/engine/security/k.java` | `security.k` | URL 引擎门面（`a(String)`→`scanURL`） |
| `com/avl/engine/urldetector/UrlDetector.java` | `UrlDetector` | URL 检测（`libavlurl.so`） |
| `com/avl/engine/a/**`（含 `a/e.java`、`a/c.java`、`a/k.java`） | 引擎模块 | 生命周期/配置装载/解压 `assets/avlsdk`/结果容器适配 |
| `com/avl/engine/b/**` | 结果与更新 | `b/a/a`=`AVLAppInfo` 实现；`b/b/k`=引擎特征库更新管理器 |
| `com/avl/engine/e/a/d/{e,f,g}.java` | 扫描结果模型 | native 结果 → Java 结构的映射层 |
| `com/avl/engine/j/**` | 上报 | `j/p`=日志/异常上报器（HTTP POST，gzip，URL 来自配置） |
| `com/avl/engine/{c,d,f,h,i,k,l}/**` | 内部支撑 | 模块注册表、证书/签名、缓存、特征、工具 |

### 2.2 TMSDK（`tmsdk` 90 + `tmsdkobf` 342）

| 文件 | 类 | 作用 |
|---|---|---|
| `tmsdk/common/TMSDKContext.java` | `TMSDKContext` | SDK 唯一初始化入口（v6.5.0）、设备信息注入口、原生库加载 |
| `tmsdk/common/module/qscanner/QScannerManagerV2.java` | `QScannerManagerV2` | APK 扫描门面（`initScanner/scanUninstalledApks/scanInstalledPackages/cancelScan/freeScanner`） |
| `tmsdk/common/module/qscanner/impl/f.java`（1583 行） | `impl.f` | 扫描实现：本地 AmScannerV2 + 云查（cmd 2016）+ 特征校验 |
| `tmsdk/common/module/update/UpdateManager.java` + `UpdateConfig.java` | `UpdateManager` | 特征库更新（`check/update/cancel`），62 个文件 ID |
| `tmsdk/bg/module/wifidetect/WifiDetectManager.java` + `tmsdkobf/p9.java`（548 行） | `WifiDetectManager` | WiFi 安全检测（网络可达/DNS 劫持/钓鱼/ARP/SSLStrip） |
| `tmsdk/fg/module/urlcheck/{UrlCheckManager,UrlCheckManagerV3}.java`（+`a,b`） | URL 检测 | **存在但零调用**（见 §4.2.3） |
| `tmsdk/common/tcc/{TccCryptor,DeepCleanEngine,QSdcardScanner,TrafficSmsParser,VideoMetaRunner}.java` | TCC 工具 | 加密/深度清理/存储卡扫描/短信解析/视频元数据（多为 native） |
| `tmsdk/common/module/sms_check_v4/internal/exception/*` | 短信校验 | **只剩 3 个异常类，主体未打包** |
| `tmsdk/common/{TMSService,TMServiceFactory,CallerIdent,YellowPages,SmsEntity,OfflineVideo,NumMarker}.java` | 服务/数据 | TMSDK 常驻服务与数据模型 |
| `tmsdkobf/w5.java` | `PubSharkConfig` | **腾讯侧全部接入点/域名**（见 §5.1） |
| `tmsdkobf/{b5,x5,u5,y9,z4,j4,f5,xb,qb,na}.java` | 核心 | 连接管理/统计上报/许可(`f5`)/设备字段(`xb`,`qb`)/偏好(`na`) |

### 2.3 OneTrack（`com.xiaomi.onetrack`，163 类）

| 文件 | 类 | 作用 |
|---|---|---|
| `z/b.java`（**宿主侧**） | `z.b` | `OneTrackManager`：唯一初始化点 |
| `z/c.java`、`e0/{a,b,c}.java`（宿主侧） | 埋点门面 | `z.c.a(String, p...)` → `OneTrack.track` |
| `com/xiaomi/onetrack/OneTrack.java` | `OneTrack` | 公开 API + `Param`/`Event` 常量 |
| `Configuration.java` | `Configuration.Builder` | appId/channel/IMEI/IMSI/GAID/异常捕获开关 |
| `util/DeviceUtil.java`（1184 行） | `DeviceUtil` | **设备信息采集总表**（见 §4.4.3） |
| `util/oaid/**`（28 类） | OAID | 各家 OEM 开放匿名设备标识通道 |
| `f/b.java` | 公共参数 | `C0041b` 定义了上报 JSON 的全部 key |
| `util/x.java` | `RegionDomainManager` | 上报/配置域名选择 |
| `c/q.java`、`d/a.java`、`d/b.java` | 编码 | gzip + AES/ECB/PKCS5 + RSA 证书加密 |
| `c/c.java`、`b/g.java`、`a/a.java` | 队列 | SQLite 落盘队列（events / events_cloud / monitor） |
| `c/m.java` | 调度 | 失败重试与退避 |
| `CrashAnalysis.java` | 崩溃 | 反射拉起 xCrash 并读 `.xcrash` 文件上报 |

### 2.4 xCrash / 国密 / 框架类

| 文件 | 类 | 作用 |
|---|---|---|
| `xcrash/XCrash.java`、`NativeHandler.java`、`h.java`(JavaCrash)、`d.java`(FileManager)、`j.java`(tombstone 文本) | xCrash 2.4.9 | 崩溃捕获与 tombstone 落盘 |
| `com/tencent/tmf/gm/{a,Sm2,Sm4,b}.java` | 国密 | SM2/SM4（JNI `libtmf-gm.so`）+ 硬编码 SM4 密钥 |
| `com/tencent/tcuser/util/{a,Encode}.java` | 腾讯工具 | hex/数字转换（活）；`Encode` 依赖 **缺失的 `libxy.so`**（死） |
| `com/tencent/turingfd/sdk/vd/TNative$aa.java` | Turing 风控 | 仅剩 native 桥（`libturingvd.so`），**Java 侧零引用** |
| `com/sun/nio/zipfs/**`（12 类）+ `unknown/META-INF/services/java.nio.file.spi.FileSystemProvider` | OpenJDK zipfs | 支撑 `jar:` FileSystem |
| `android/content/pm/{PackageParser,PackageUserState,VerifierInfo}` | AOSP | 离线 APK 解析与签名收集 |

## 3. 数据流 / 时序（调用点逐段标注）

### 3.1 文件扫描（同步扫描 IPC → 引擎）

```
SecurityService(antivirusservice) / GuardProvider.call
  └ f0.b 注册表：MI(Mi,100)/AVL(AVL,11)/TENCENT(TENCENT,10)      f0/b.java:14-16
     ├ AvlEngine.j(List, listener, cloud, engName)                 engine/antiy/AvlEngine.java:137
     │   ├ s(b())  → AVLEngine.setNetworkEnabled(cloud)            :143 / :212
     │   ├ r(z6)   → AVLEngine.setCloudScanEnabled(cloud)          :144 / :79
     │   └ 每个 path: AVLEngine.scan(path) → AVLAppInfo            :159
     │        └ security.c.a().b(6, path) → a.g.b(...) → AVLA.scan : security/c.java:111
     └ TencentEngine.j(...)                                        engine/tencent/TencentEngine.java:438
         ├ ManagerCreatorF.getManager(QScannerManagerV2.class)     :445
         ├ initScanner()                                          :452
         ├ scanType = (cloud && z6) ? 6 : 2                        :459   // 6 = SCAN_LOCAL|SCAN_CLOUD
         └ QScannerManagerV2.scanUninstallApks(scanType, [path], d, 0)  :472
              └ impl.f.a(...)                                     qscanner/impl/f.java:683
                   ├ 本地：AmScannerV2（libTmsdk）                 :645
                   └ 云端：x5.a(2016=Cmd_CSVirusCheck, …)          :1310
```

### 3.2 URL 检测（`urlscan` 服务）

```
SecurityService action=…action.urlscan                          manager/SecurityService.java:315
  └ n0.j.b(url, pkg)                                            n0/j.java:87
      └ engine.m(1, url)                                        engine/antiy/AvlEngine.java:195
          └ AVLEngine.scanURL(url)                              AVLEngine.java:97
              └ security.k.a(url) → UrlDetector.b(url)           security/k.java:104
                  └ native matchURLEx(url,false)                 UrlDetector.java:183
```
**[观测]** 返回值 `0/1/2/-1` 由 `UrlDetector.b` 原样透出（`UrlDetector.java:201-207`），经 `n0.j`、`SecurityService` 直接回给 AIDL 调用方；**Java 层没有对 0/1/2 的语义枚举**。**[推断]** 结合分支结构（0 直接返回、1、2 分列）与 AVL 公开文档惯例：0=安全、1=风险、2=钓鱼/恶意；**[未知]** 未经 native 侧确认。

### 3.3 特征库/引擎更新

```
f0.a.l(listener)                                                  (引擎抽象)
├ AvlEngine.l(): checkUpdate() → update()                          AvlEngine.java:174/182
│    └ security.c.b()/c() → a.g.e()/f() → b.b.k.a()               security/c.java:52-61
└ TencentEngine.l(): UpdateManager.check(UPDATE_FLAG_VIRUS_BASE)   TencentEngine.java:493
     └ libTmsdk → https://mmgr.gtimg.com/qqsecure_config_update/…  update/a.java:675
```

### 3.4 OneTrack 埋点 → 上报

```
业务点 z.c.f12069a.a("event", p...)                              MiEngineNG.java:156/201
  └ z.c$a.a → e0.c.a → z.b.b(event,map) → OneTrack.track          z/c.java:36, e0/c.java:22, z/b.java:47
      └ api.h → 公共参数 f/b.a(...) → c.i(队列) → c/c.java:85 events 表
          └ c/q: JSON→gzip→AES/ECB/PKCS5                           c/q.java:120-147, d/a.java:42
              └ POST https://tracking.miui.com/track/v4            util/x.java:35/59/129
```
异常分支：`OneTrack.createInstance` → `api/h.b()` → `CrashAnalysis.start` → 反射 `XCrash.init` → 读 `filesDir/tombstones/*.java.xcrash` → `onetrack_bug_report`（`api/c.java:84`）。

## 4. 关键逻辑逐条解析

> 每条格式：**位置 | 观测 | 推断 | 未知**。凡"实际启用"结论，均给出宿主侧调用点；凡"仅声称"，给出 API 存在性出处并声明零调用。

### 4.1 腾讯 AVL 引擎（`com.avl.engine`）

#### 4.1.1 `AVLEngine` 公开 API 全表（22 个方法，全部 public static）

| 方法 | 行 | 语义 | 本 APK 是否调用 |
|---|---|---|---|
| `init(Context, AVLSdkConfig): int` | `AVLEngine.java:81` | 初始化（解压 assets、载入 `libavlm.so`、校验 appkey/token） | **是** `AvlEngine.java:99` |
| `scan(String): AVLAppInfo` | `:140` | 扫描单个路径（内部走 `i.a()`=6） | **是** `AvlEngine.java:159` |
| `scan(Context, String): AVLAppInfo` | `:89` | 同上（显式 context，未用） | 否 |
| `scanURL(String): int` | `:97` | URL 检测，返回 0/1/2/-1 | **是** `AvlEngine.java:197` |
| `checkUpdate(): int` | `:25` | 检查特征库更新（`-4` 无网络/`-5` 未初始化/`-1..-3` 映射） | **是** `AvlEngine.java:174` |
| `update(): int` | `:125` | 执行更新，>0 表示有更新项 | **是** `AvlEngine.java:182` |
| `stopUpdate(): int` | `:121` | 中止更新 | **是** `AvlEngine.java:167` |
| `updateURLRule(): int` | `:132` | 更新 URL 规则库 | **是** `AvlEngine.java:202` |
| `getEngineVersion()` | `:53` | 引擎版本 | 间接（`d()` 记录） |
| `getVirusDatabaseVersion()` | `:77` | 病毒库版本 | 否 |
| `getURLDatabaseVersion()` | `:65` | URL 库版本；宿主写入 SP `avl_url_sdk_version` | **是** `AvlEngine.java:206-207` |
| `getURLEngineVersion()` | `:69` | URL 引擎版本 | 否 |
| `getUpdateProgress()` | `:73` | 更新进度 | 否 |
| `setCloudScanEnabled/getCloudScanEnabled` | `:101/:49` | 云查开关 | **是** `AvlEngine.java:79` |
| `setNetworkEnabled/getNetworkEnabled` | `:113/:57` | SDK 联网总开关 | **是** `AvlEngine.java:212` |
| `setRegion(String)` | `:117` | 区域（`miui.os.Build.getRegion()`） | **是** `AvlEngine.java:83` |
| `setLogUploadEnabled/isLogUploadEnabled` | `:109/:85` | 日志上报开关 | 否 |
| `setLanguage(String)` | `:105` | `zh`/`en` | 否 |
| `getSDKVersion()` | `:61` | 静态块 `a.b("4.0.1")` | 否 |

**[观测]** `static { a.b("4.0.1"); }`（`AVLEngine.java:18-20`）→ SDK 版本 4.0.1；`security/g.java:75` 另有 `a()` 返回 `"4.0.0"`（模块级版本，二者并存）。

#### 4.1.2 `AVLScanOption`：选项位/枚举含义（SDK 声称，**本 APK 零使用**）

| 字段 | 默认值 | 语义（据方法名/用法） | 出处 |
|---|---|---|---|
| `f1675d` / `getScanMode()` | `0` | 扫描模式（native `scan(String,int[])` 的第 1 个 int） | `AVLScanOption.java:65`, `:96` |
| `f1676e` / `getScanCategoryOption()` | `32767`(0x7FFF) | **扫描类别位掩码**：15 位全开，对应安装包/权限/广告/隐私等类别（具体位定义在 native） | `:28`, `:57` |
| `f1677f` / `getScanOutputOption()` | `0` | 输出选项（返回内容裁剪） | `:69`, `:92` |
| `f1678g` / `getScanLogOption()` | `2` | 日志级别；带静态去重（`f1673b`/`f1674c` 保证同值只下发一次） | `:31`, `:33-43` |
| `f1679h` / `getScanRange()` | `0` | 扫描范围 | `:73` |
| `f1680i` / `b()` | `0` | 超时/时限（long） | `:45`, `:100` |
| `c()` | — | 由 mode+category+output 拼十六进制再 MD5(32)，作为 native 选项缓存键 | `:50` |

**[观测]** 全树 grep `AVLScanOption`：`com/avl` 之外 0 命中；`AVLEngine.scan` 只传 `i.a()`（默认 6）与 path，**不构造任何 AVLScanOption**。**[推断]** 该类的类别/输出/日志位在本 APK 全部走 native 默认值，宿主无法调优；GuardProvider 只关心"是否命中"，不关心命中类别细分。

#### 4.1.3 扫描结果模型（命中类型 / 病毒名 / 广告类型）

**[观测]** 接口 6 个 getter：`getAppName/getDangerLevel/getPackageName/getPath/getVirusDescription/getVirusName`（`AVLAppInfo.java:5-15`）。
**[观测]** 实现链 `com.avl.engine.b.a.a`（`AVLAppInfo`）→ `com.avl.engine.a.k` → `com.avl.engine.e.a.d.f`+`g`：

| 字段 | 取值来源 | 语义 |
|---|---|---|
| `appName` | `f.c().c()` | 应用名 |
| `packageName` | `f.c().d()` | 包名 |
| `path` | `f.c().b()` | 文件路径 |
| `virusName` | `g.a()`，若等于 `"White"` 则置空 | 病毒名（`"Risk/..."`、`"Trojan*"`、`"G-Ware*"`、`"PornWare*"`、`"RiskWare*"`、`"Tool*"`） |
| `virusDescription` | `g.d()` | 描述 |
| `dangerLevel` | `g.f()` | 危险级别（int，透传） |
| （内部）`f767k` | 前缀判定：`Risk/`→2；`Trojan/G-Ware/PornWare/RiskWare/Tool`→1；否则 0 | **本地威胁分类**（未公开） |

出处：`com/avl/engine/a/k.java:42-72`（尤其 `:55` 白名单过滤、`:65-69` 前缀映射）、`com/avl/engine/e/a/d/f.java:192-245`（`i()` 依 `b`/`g` 选择 `g.d`/`g.b` 结果解析器）。
**[观测]** 宿主再映射一层：`AvlEngine.p()` 把 `dangerLevel` 1→2、2→3，`virusName` 前缀 `risk/` → 6（`AvlEngine.java:63-76`）；最终 `f.f11671p = "AVL:danger;"`/`"AVL:safe;"`（`:44/:46/:58`）。
**[观测]** 广告/色情类别不来自 level，而来自**特征包文件名**：`assets/avlsdk` 内含 `avlpk_pornware.avl`(322 KB)、`avlpk_apn.avl`(131 KB)、`avlpk_amc.avl`、`avlpk_sfmf.avl`、`avlpk_sgnl.avl`、`avlpk_kw.avl`、`avlpk_dhc.avl`、`avlpk_des.avl`。**[推断]** `pornware`=色情、`apn`=广告插件(adware plugin)、`amc`=恶意扣费/暗扣、`sfmf`/`sgnl`=签名/证书类规则、`kw`=关键词规则（配套 `av/kw/common/{zh,en}/*.des` 文案库）；具体缩写未由代码自证 → **[未知]**。

#### 4.1.4 `UrlDetector`：能力与判定输入输出

| 项 | 内容 | 出处 |
|---|---|---|
| 加载 | `System.loadLibrary("urldetectorsys")` + `setLibPath("libavlurl.so")`（或配置路径 `.../avlurl`） | `UrlDetector.java:80`, `:87` |
| 初始化 | `loadURL(urlDatePath)`，返回 0/1/2，`-1` 失败；成功后 `f1728d=true` | `:98-113` |
| 判定 | `matchURLEx(url,false): String[]`，`[0]`=结果码、`[1]`=命中详情（交给 `urldetector.a.b.b()`） | `:183-197` |
| 输出 | `0` 安全 / `1` / `2` / `-1` 失败（分支见 `:201-207`） | 同左 |
| 输入 | 仅 URL 字符串（**不上传 URL**，纯本地库匹配） | `:174-213` |
| 规则库 | `assets/avlsdk` 内 `url/url/{fish_url.avl 417 KB, white.avl 370 KB, fish.avl 150 KB, fish_re.avl, white_re.avl}` + `avlurl` 目录 | zip 清单 |
| 版本 | `url.siglib=20180917.zz`、`url.engine=124`（`sdk_conf/version.conf`）；`avllib.conf` 同值 | assets 内文件 |
| 日志目录 | `setLogPath(<sdkDir>/avlurl)` | `:234` |

**[观测]** `UrlDetector` 还带 native 组：`matchURL/matchSpUrl/matchUrlForTag(Ex)/updateURL/setNetworkEnabled/setLogOpt/checkAPPToken`（`:124-168`）——即 SDK 具备"URL 打标签""按标签匹配""在线更新规则"能力。
**[未知]** `matchURLEx[1]` 的详情结构（被 `urldetector.a.b.b()` 消费后不再可见）。

#### 4.1.5 `AVLSdkConfig` / `AVLAppInfo` 里的 appkey、包名、渠道、license 装载

| 项 | 装载方式 | 出处 |
|---|---|---|
| appkey | 从**自身** `ApplicationInfo.metaData` 读 `AVL_OPENSDK_APPKEY`，缺失回落 `AVL_SDK_APPKEY`；空则打日志 `Failed to get appKey from AndroidManifest.xml!` | `com/avl/engine/a/e.java:36-47` |
| appkey 实际值 | `D5DFB878335A3EF4E76F60ED470B8EC8` | `AndroidManifest.xml` meta-data（简报已确认） |
| 包名 | `k.l().getPackageName()`（`com.miui.guardprovider`），用于 token 计算 | `com/avl/engine/a/c.java:170` |
| 渠道 | 配置项 `av.channel`（`e.b()` 返回 `f752c.a("av.channel")`） | `com/avl/engine/a/e.java:90-92` |
| license | `assets/avlsdk` 内 `av/avl/conf/liscense.conf`(404 B) 与 `url/url/conf/liscense.conf`(404 B)，运行时解压到 `<sdkDir>/avl` 与 `<sdkDir>/avlurl` | `a/c.java:195-201` |
| token | `AVLA.computeToken(packageCodePath, packageName, appkey)` 后 `checkSdkKey` | `a/c.java:170`, `a/t.java:22` |
| 设备档位 | `AVLSdkConfig.Builder().setDeviceType(c5.a.a()?0:1)`（0=LITE/1=HIGH；`c5.a.a()` 判低内存） | `AvlEngine.java:99`、`AVLSdkConfig.java:5-6`、`security/i.java:46-75` |
| 日志上报 URL | `setLogUploadUrl` **未被宿主设置**（Builder 只链了 setDeviceType） | `AvlEngine.java:99` |

#### 4.1.6 AVL：声称 vs 实际

| 能力 | SDK 声称（API/native 存在） | 本 APK 实际 |
|---|---|---|
| 本地 APK 静态扫描（多引擎：`scan/scanEx/sceneScan/officialScan/pcit/analyseAppByEngine`） | `AVLA.java:89-101` | **启用 `scan`**；`scanEx/sceneScan/officialScan` 零调用 |
| 扫描配额/降级（`setScanLimit/setDEXScanLimit/setOpcDEXScanLimit/setMapleScanLimit/setScanLimitEx`） | `AVLA.java:29-39,105`；封装在 `AVLA.a(f)` | **未启用**（宿主不构造 `f`） |
| 云端查询/时间闸（`setAIES/setAIER`） | `AVLA.java:29-31`、`AVLA.a(e)` | 未启用 |
| URL 检测（本地库） | `scanURL` + `UrlDetector` | **启用** |
| URL 规则在线更新 | `updateURLRule` | **启用**（受 `getNetworkEnabled()` 门控） |
| 引擎/病毒库在线更新 | `checkUpdate/update/stopUpdate` + `b/b/k` | **启用** |
| 日志/异常上报 | `j/p`（HTTP POST+gzip）+ `setLogUploadEnabled` | **未启用**（`logUploadUrl=null`；`AVLDebug.setLogEnable(false)` 于 `AvlEngine.java:101`） |
| 应用信息/证书解析 | `getAPKInfo/getCertHash/getCertFullHash/signBlockHash/keySetHash` | 未直接调用（宿主改用自带 `PackageParser`，见 §4.7） |
| 动态/syscall 级检测 | `JniFuncProcess` 回调 + `sjsp` | 未启用（不实现回调） |
| 解压/自检 | `NativeUtils.unzipAvlsdk/unzipAssetsFile/searchFile` | 由初始化内部使用 |

**[推断]** AVL 的"云查"在本 APK 只体现为 `setCloudScanEnabled` 一个布尔（`AvlEngine.java:79`），真正的云端交互依赖 `AVLSdkConfig.logUploadUrl` 与配置驱动的 `j/p` base URL；宿主两者都未提供 → **AVL 侧无自有域名、无 URL/文件外发**（见 §5.4）。

### 4.2 腾讯 TMSDK（`tmsdk` + `tmsdkobf`）

#### 4.2.1 能力清单（公开模块 → 一句话）

| 模块 | 入口类 | 能力 | 出处 |
|---|---|---|---|
| APK 扫描 | `QScannerManagerV2` | 已装/未装 APK 扫描（本地 AmScannerV2 + 云查 cmd 2016 + 特征校验） | `qscanner/QScannerManagerV2.java:50-60` |
| 特征库更新 | `UpdateManager` | 62 个文件 ID（病毒库/白名单/证书 MD5 等）下载与校验 | `update/UpdateConfig.java:311` |
| WiFi 安全 | `WifiDetectManager` | 网络可达(urlApprove)、DNS 劫持/钓鱼(云)、ARP 欺骗(无 root)、WiFi 加密类型、SSLStrip、批量钓鱼 | `wifidetect/WifiDetectManager.java:36-82` |
| URL 检测 | `UrlCheckManager`/`V3` | 同步/异步 URL 云查（cmd 290/291 级） | `urlcheck/UrlCheckManager.java:15-31` |
| 深度清理 | `DeepCleanEngine` | native 规则匹配目录/文件（`isMatchFile/isMatchPath/findMatchDir`） | `tcc/DeepCleanEngine.java:5-20` |
| 存储卡扫描 | `QSdcardScanner` | 按后缀遍历 `/storage/emulated/legacy`、`/storage_int`、`/HWUserData` 找 APK | `qscanner/impl/f.java:745-765` |
| 短信校验 | `sms_check_v4` | **只剩 3 个异常类**（`DatException/EncryptionException/ModelException`），主体缺失 | 目录清单 |
| 流量短信解析 | `TrafficSmsParser` | 运营商流量短信识别（`getNumberEntrance/getWrongSmsType`） | `tcc/TrafficSmsParser.java` |
| 视频元数据 | `VideoMetaRunner` | 仅 `main()`（命令行工具类） | `tcc/VideoMetaRunner.java` |
| 号码标记 | `TelNumberLocator`/`NumMarker`/`YellowPages` | 号码归属地/黄页 | `bg/tcc/TelNumberLocator.java` |
| 加密 | `TccCryptor` | 对称加解密 + `makePassword`（native） | `tcc/TccCryptor.java:6-27` |
| 连接/上报 | `PubSharkConfig`/`x5`/`y9`/`z4` | 长连接、统计打点、渠道上报 | `tmsdkobf/w5.java`、`x5.java`、`y9.java`、`z4.java` |
| 许可 | `f5`/`TMSDKContext.checkLisence` | 读 `assets/licence.conf`/`licence1.conf`，过期则 `isExpired` | `tmsdkobf/f5.java:101-150`、`TMSDKContext.java:51-53` |

#### 4.2.2 活代码：GuardProvider 只用了 3 个 Manager（逐条举证）

| 能力 | 调用点 | 观测 |
|---|---|---|
| SDK 初始化 | `TencentEngine.java:403-408` | `TMSDKContext.init(context, null, cfg)`；`cls=null` → **不启动 `TMSService`**（`TMSDKContext.java:140-146` 的 `startService` 分支被跳过） |
| 设备信息注入 | `TencentEngine.java:402` | `TMSDKContext.setDeviceInfo(ctx, Settings.Secure.getString(cr,"android_id"), Build.MODEL)` → `TMSDKContext.java:188-207` 写入 `xb.f10474a`(androidId) 与 `qb.f9915a`(model) |
| 自动连接/渠道上报 | `TencentEngine.java:507`（`v(z)`）+ `:400/:456/:487` 调用 | `TMSDKContext.setAutoConnectionSwitch(cloud)` → `y9.f(29987/29988)`；`true` 时再 `reportChannelInfo()`→`z4.a()`（`TMSDKContext.java:173-182`），`z4` 发 `"reportlc"`（`z4.java:39-43`） |
| APK 扫描 | `TencentEngine.java:445/452/472` | `ManagerCreatorF.getManager(QScannerManagerV2.class)` → `initScanner()` → `scanUninstallApks(scanType,[path],listener,0)` |
| 扫描类型 | `TencentEngine.java:459` | `(cloud && z6) ? 6 : 2` → 位语义 `2=SCAN_LOCAL`、`4=SCAN_CLOUD`（`qscanner/impl/f.java:723-738`），6=本地+云 |
| 云查协议 | `qscanner/impl/f.java:1304-1310` | `x5.a(2016 /*Cmd_CSVirusCheck*/, 请求体, …, SHARK_HTTP, …)`，请求体含包名/版本/证书等 `l1` 字段（`:1294-1303`） |
| 特征库更新 | `TencentEngine.java:489-493` | `ManagerCreatorC.getManager(UpdateManager.class)` → `check(UpdateConfig.UPDATE_FLAG_VIRUS_BASE, listener, -1)` |
| WiFi 检测 | `TencentEngine.java:84,94-122` | `ManagerCreatorB.getManager(WifiDetectManager.class)` → `init()` → `detectNetworkState()` / `detectDnsAndPhishing(listener,0)` / `detectARPNoRoot(15000)` / `detectSecurity(scanResult)`；`free()` 于 `:77` |
| WiFi 自采集（宿主侧） | `TencentEngine.java:107-114` | 宿主自己 `getConnectionInfo()`+`getScanResults()`，用 BSSID 匹配后把 `ScanResult` 交给 SDK |
| 取消 | `TencentEngine.java:393` / `:480` | `QScannerManagerV2.cancelScan()` / `UpdateManager.cancel()` |

**[观测]** `TencentEngine.java:122` 传入超时 `0L`，SDK 侧把 `<2000` 纠正为 `180000`（`WifiDetectManager.java:50-52`）。
**[观测]** 扫描结果映射：`scanResult` 258→5、259/261/262→3、260→2、其余→1（`TencentEngine.java:352-365`），宿主再打 `"TENCENT:danger;"`/`"TENCENT:safe;"`（`:380-385`）。

#### 4.2.3 存在但零调用的能力（穷尽 grep 举证）

grep 范围：`_analysis/src/sources` 全树，排除 `tmsdk/`、`tmsdkobf/` 自身；命中数为 0：

| 符号 | 外部命中 | 结论 |
|---|---|---|
| `UrlCheckManager` | 0 | URL 云查**完全不启用**（GuardProvider 的 URL 检测只走 AVL，`n0/j.java:58` 按引擎名匹配） |
| `TccCryptor` | 0 | TMSDK 加解密不启用 |
| `DeepCleanEngine` | 0 | 深度清理不启用 |
| `QSdcardScanner` | 0（`tmsdk` 内部仅 `qscanner/impl/f.java:751`，且仅当调用方传 `list==null` 时触发） | 宿主恒传单元素 list（`TencentEngine.java:470-472`）→ **不触发全盘遍历** |
| `TrafficSmsParser` / `VideoMetaRunner` / `sms_check` | 0 / 0 / 0 | 短信、视频、短信校验模块不启用；`sms_check_v4` 且主体缺失 |
| `TelNumberLocator` / `NumMarker` | 0 | 号码归属地不启用 |
| `TMSDKContext.startPersistentLink` | 0 | 长连接未主动拉起（但 `setAutoConnectionSwitch(true)` 后连接管理仍会建链） |

**[推断]** TMSDK 在本 APK 是"**被裁剪到只剩扫描+WiFi+更新**"的形态；`sms_check_v4` 仅剩异常类、`libxy.so`（`tcuser.util.Encode` 依赖）不在 `lib/arm64-v8a`，说明打包方做过一次模块裁剪。

#### 4.2.4 TMSDK 的联网与隐私（链路）

| 采集项 | 采集点 | 去向 | 触发条件 |
|---|---|---|---|
| `ANDROID_ID` + `Build.MODEL` | `TencentEngine.java:402` → `TMSDKContext.java:200-206`（`xb.f10474a`/`qb.f9915a`） | 进 TMSDK 请求头/上报体（`x5`/`y9` 通道） | `TENCENT` 引擎 `e()` 初始化（首次扫描/更新前） |
| WiFi `SSID`/`BSSID`/`DNS1`/`DNS2` | `tmsdkobf/p9.java:210-238` | 云查 cmd **794**（`:254`），走 `x5` → `https://tmfsdk.m.qq.com` / TCP `:8001/:8002` | 宿主 `n(DNSANDPHISHING)` → `TencentEngine.java:122` |
| 当前 WiFi 加密类型/ARP 网关 | `p9.java:190`（`q9.a(bssid,timeout)`）、`WifiDetectManager.detectSecurity` | **仅本地判定**，只在异常时随统计打点（`y9.c(1320068,…)`） | `SECURITY`/`ARP` 类型请求 |
| 已安装应用列表 | `qscanner/impl/f.java:434`（`getInstalledApplications(0)`，排除自身） | 云查请求体（`l1` 序列） | **仅当** `scanUninstalledApks` 的 list 为空；本 APK 不触发 |
| 网络类型 / SIM 状态 | `tmsdkobf/xb.java:84-86`、`y3.java:178` | 请求上下文（`xb.e()`） | 任意联网请求 |
| 扫描目标 APK 的包名/版本/证书摘要 | `qscanner/impl/f.java:1294-1303` | 云查 cmd 2016 | cloud 开启时的每次 `scan` |
| root 检测 | native（`libTmsdk-2.0.13-mfr.so`），su 路径合集见 §5.6 | 本地判定，不上传（无 Java 调用点） | 扫描/初始化内部 |

### 4.3 `com.tencent`：国密 / 工具 / Turing

| 类 | 观测 | 推断 |
|---|---|---|
| `com.tencent.tmf.gm.a` | `static { System.loadLibrary("tmf-gm"); }`（`:25-30`）；三个错误码 1/2/3 | 国密是 **JNI 实现**，非纯 Java |
| `com.tencent.tmf.gm.Sm2` | `doSm2Encrypt2(pubX[32], pubY[32], data, AtomicReference)` native（`:28`）；前置校验 32/32 字节 | 公钥为 **64 字节 X||Y**（`b.a()` 再拆 32+32，`b.java:48-52`） |
| `com.tencent.tmf.gm.Sm4` | `doSm4DecryptECB/doSm4EncryptECB` native（`:49-51`），ECB 模式 | SM4 无 IV，ECB |
| `com.tencent.tmf.gm.b` | **硬编码 16 字节 SM4 密钥** `{115,66,90,85,24,8,6,92,118,112,33,112,36,36,51,41}`（`:7`）；`a()/b()/c()` 作为默认 key 兜底（`:17-18,:31-32`）；`a(data,pub64)` 要求公钥长度 64（`:45-46`） | 会话密钥不落地，**默认密钥是常量** → 反编译即可解密 TMSDK 的 SM4 段 |
| 密钥来源（SM2 公钥） | `tmsdkobf/j7.java:157-164`：`i6==0` 用 `y7.a(h9.a(str),0)`，`i6==1` 要求 `str.length()==128` 否则抛 `"SM2 public key must be 128 chars!"` | 公钥**由服务端配置下发**（128 hex），不是内置常量 |
| 调用方 | `tmsdkobf/y5.java:6-17`（实现 `x3`：`a/b/c` → `gm.b.c/b/a`） | 国密用于 TMSDK 的 shark 通道载荷 |
| `com.tencent.tcuser.util.a` | hex→byte（`:33-45`）、`Byte/Integer` 解析（`:9-31`） | 纯工具；被 `qscanner/impl/c.java:114,123`、`tmsdkobf/{ba,ca,gb,v2}.java` 使用 |
| `com.tencent.tcuser.util.Encode` | `System.loadLibrary("xy")`（`:43`）；`nativePs(List,List<ProcessInfo>)` 读**进程列表**（`:57`）；`native x/y(Context,byte[])`（`:71-73`） | **`libxy.so` 不在 `lib/arm64-v8a`** → 一旦调用即 `UnsatisfiedLinkError`；且全树无调用点 → **死代码** |
| `com.tencent.turingfd.sdk.vd.TNative$aa` | 9 个 native，全部 `SparseArray<Object>` 进出（`:22-38`）；内嵌 `bb implements ServiceConnection` 且 `onServiceConnected` 是 native（`:13-20`） | Turing 风控的全部逻辑在 `libturingvd.so`；**Java 侧零引用**（grep `turingfd` 无宿主命中）→ 运行时是否被 native 自拉起 **[未知]**；采集项无法从 Java 判定 **[未知]** |
| `com.tencent.tmsecure.R` | 28 KB 资源 ID 常量表 | 腾讯代码迁移残留（`com.tencent.tmsecure.demo` 等包名残留，见 §4.8） |

### 4.4 小米 OneTrack（`com.xiaomi.onetrack`）

#### 4.4.1 初始化与关键配置

**[观测]** 唯一初始化点 `z/b.java:21-30`（REA `ev_4c3c9e94…` 已核验）：

```java
new Configuration.Builder()
  .setAppId("2882303761517522561")          // z/b.java:22
  .setChannel(GuardApplication.f1743d.getPackageName())   // 渠道 = "com.miui.guardprovider"
  .setExceptionCatcherEnable(true)          // → 拉起 xCrash
  .setMode(OneTrack.Mode.APP)
  .setAutoTrackActivityAction(true)         // 本 APK 无 Activity → 无效果
  .setGAIDEnable(false)                     // 禁 Google 广告 ID 的"推荐事件"用途
  .build();
OneTrack.createInstance(GuardApplication.f1743d, configurationBuild);  // :26
```
- **未设置** `setImeiEnable/setImsiEnable/setRegion/setInternational/setInstanceId/setUseCustomPrivacyPolicy` → 走默认：`IMEIEnable=true`、`IMSIEnable=true`、`GAIDEnable(默认 true，但被显式 false)`、`international=false`（`Configuration.java:269-291`）。
- 网络门控：`z/b.java:39-42` → `OneTrack.setAccessNetworkEnable(ctx, GuardApplication.e(ctx))`；`GuardApplication.e()` 要求 `persist.sys.sc_allow_conn=true` **且**已装 `com.miui.securitycenter`（国际版 `jp_kd` 还要求 `Settings.Secure.antivirus_engine_network_enable==1`）（`GuardApplication.java:38-48`）。**[推断]** 非 MIUI 设备上 OneTrack 只落盘不上报。
- **未调用**：`registerCrashHook`（但 `api/h.java:170-171` 在 `Mode.APP && isExceptionCatcherEnable` 时自动 `CrashAnalysis.start`）、`login/logout/setUserProfile/adTrack/trackPluginEvent`。

#### 4.4.2 本 APK 实际调用的事件名全表（含动态拼接来源）

| 事件名 | 触发条件 | 属性（key=value 来源） | 出处 |
|---|---|---|---|
| `MiEngineNG_Scan` | MI 引擎一次扫描结束 | `scan_count`=list.size()；`scan_time`=耗时(ms) | `MiEngineNG.java:156` |
| `threats` | MI 引擎结果为"命中威胁但非灰产"(仅 `t0.o`) | `packageName`,`versionCode`,`versionName`,`mfSha256`,`signaturesMd5`,`signaturesSha256`,`installer`,`threatName`（=威胁列表 join ","，`MiEngineNG.java:192,195`） | `MiEngineNG.java:196,201` |
| `blackgrays` | 结果为灰产但非威胁(仅 `t0.f`) | 同上 8 字段，`threatName=null` | `:199,201` |
| `blackgrays_threats` | 同时是灰产且命中威胁 | 同上 8 字段，`threatName` 为灰产列表 join | `:193,201` |
| `onetrack_dau`（SDK 自动） | 每次会话首个事件批 | `first_open`、`imeis`(IMEI 列表)、`imsis`(IMSI 列表)、`config_status` | `api/c.java:99-119` |
| `onetrack_bug_report`（SDK 自动） | 读到 `.xcrash` 崩溃文件 | `exception`(摘要)、`type`、`message`、`feature`、`crashtime` | `api/c.java:82-97` + `CrashAnalysis.java:158-178` |

**[观测]** 事件名没有常量表，`threats/blackgrays/blackgrays_threats` 由 `str` 变量按 `instanceof` 分支赋字面量（`MiEngineNG.java:190-201`），**不是动态拼接**；唯一拼接在 `threatName`（`y.c0(list, ",", …)`）。
**[观测]** `OneTrack.Event`/`OneTrack.Param` 里定义的 19 个事件名（`VIEW/LIKE/LOGIN…`）在本 APK **零调用**（`z/b.java:47` 是唯一 `track` 入口，调用方仅 `MiEngineNG`）。
**[推断]** 由于 `z.b.a()` 由 DI 懒加载（`e0/a.java:14-18`、`e0/b.java:8`），OneTrack 只在**首次 MI 引擎扫描出结果**时才初始化 → 冷启动不上报、不采集。

#### 4.4.3 采集字段全表（`DeviceUtil` / `util.oaid.*`）

| 字段 | 采集实现 | 出处 | 是否进入上报 |
|---|---|---|---|
| IMEI（多卡） | `TelephonyManagerEx.getImeiList()` 优先；回落 `TelephonyManager.getImei(slot)`；再回落 `getDeviceId()` | `DeviceUtil.java:296-341,343-368,1046-1060` | 是：`imei`=首 IMEI 的 MD5（`f/b.java:304`、`util/g.java:45`）；`imeis`=IMEI **明文列表**（`api/c.java:111`，仅 DAU 事件） |
| MEID | `TelephonyManagerEx.getMeidList()` / `TelephonyManager.getMeid()` | `:370-401` | 间接（`s()` 输出 MD5 拼接） |
| IMSI（双卡） | `getSubscriberId()` / `SubscriptionManager.getSubId` | `:659-702,1124-1165` | 是：`imsis`=IMSI 列表 MD5（`api/c.java:114`） |
| SN | `Build.getSerial()` 反射 | `:704-721` | 是：`sn` 字段已定义（`f/b.java:139`），本路径未赋值 → **[未知]** |
| OAID | `util/oaid/a.a().a(ctx)`（OEM 通道：vivo/nubia/meizu/honor/asus/oppo 等） | `:761-763`、`util/oaid/**` | 是：`oaid`（`f/b.java:305`）、writeBack 参数（`util/g.java:46`） |
| AAID | `content://com.miui.analytics.server.AnalyticsProvider/aaid` → 回落 `MiuiSettings$Ad.getAaid` | `:785-806` | 是（广告监测用，本 APK 无 adTrack → 仅配置链） |
| GAID | 绑定 `com.google.android.gms.ads.identifier.service.START`（Parcel transact 1/2） | `:127-287` | 仅国际版"推荐事件"（`f/b.java:298`）；本 APK `GAIDEnable=false` → **不采** |
| AndroidID | 由 SDK 外部传入/内部读取（`f/b.java:145` 定义 key） | `f/b.java:145` | key 存在，本路径未见赋值 → **[未知]** |
| MAC | MiTV 专用：`Settings.Global "mi_device_mac"` → `/sys/class/net/{wlan0,eth0}/address` → `ro.boot.btmac` → `persist.service.bdroid.bdaddr` | `:842-870` | 否（仅机型分支） |
| 型号 | `Build.MODEL` / `ro.product.model.real` / `ro.product.marketname` | `:998-1006,1074-1076,1100-1102` | 是：`model`、`market_name`（`f/b.java:313,338`） |
| 厂商 | `Build.MANUFACTURER` | `:1078-1080` | 是：`mfrs`（`f/b.java:312`） |
| 设备/产品 | `ro.product.mod_device`→`Build.DEVICE`；`ro.product.name`；`ro.build.description` | `:1008-1020,1167-1169` | 部分（`build`） |
| 系统/MIUI 版本 | `q.d()/q.c()/q.e()` | `f/b.java:315-317` | 是：`miui`/`build`/`os_ver` |
| 网络类型/运营商 | `g.c.a(ctx).toString()`（NetType 枚举，含 2G/3G/4G/5G/WIFI/ETHERNET） | `f/b.java:321`、`OneTrack.NetType` | 是：`net` |
| 时区/区域 | `q.b()`、`q.i()`、`q.f()` | `f/b.java:320-322` | 是：`tz`/`region`/`sid` |
| 屏幕分辨率 | `getRealSize` | `:765-770` | 未进入本 APK 事件 |
| 存储容量 | `StatFs` 外置存储 total/free | `:1031-1044` | 未进入本 APK 事件 |
| 设备形态 | Pad/TV/Phone（`miui.os.Build.IS_TABLET`、`FeatureParser is_pad`、`uiMode`） | `:739-759,1082-1094` | 未进入本 APK 事件 |
| 已装应用列表 | **无**（`DeviceUtil` 内无 `getInstalledPackages`） | grep 0 命中 | 否 |
| 剪贴板 | **无**（`ClipboardManager` 0 命中） | grep 0 命中 | 否 |
| 位置/基站 | **无**（`getLastKnownLocation/getCellLocation` 0 命中） | grep 0 命中 | 否 |
| 通讯录/短信 | **无**（`ContactsContract`/`content://sms` 0 命中） | grep 0 命中 | 否 |

**[观测]** `Configuration.isIMEIEnable()=true` 且 `isIMSIEnable()=true`，而 `api/c.java:109` 的门控是"`q.a() ? q.h() : isInternational()`"——**国内 MIUI 走 `q.h()`**，若为 true 则**不采 IMEI/IMSI**。**[未知]** `q.h()` 的运行时取值（依赖 MIUI 区域开关），未在源码常量中固化。

#### 4.4.4 上报协议（批次 / 加密 / 落盘队列 / 重试 / 域名）

| 维度 | 观测 | 出处 |
|---|---|---|
| 域名 | 国内 `https://tracking.miui.com`；国际 `tracking.intl.miui.com`；印度 `tracking.india.miui.com`；俄 `tracking.rus.miui.com`（可由远端配置覆盖，存 `aa.b`） | `util/x.java:35-56,145-147,150-166` |
| 路径 | 事件 `…/track/v4`；取密钥 `…/track/key_get`；配置 `https://sdkconfig.ad.{xiaomi,intl.xiaomi,india.xiaomi,rus.xiaomi}.com/api/v4/detail/config` 与 `…/config_common` | `util/x.java:59-68,121-142` |
| 广告监测回写 | `https://<host>/api/open/device/writeBack`（参数 `instanceId`、`imei`(MD5)、`oaid`、`projectId`、`user`） | `util/g.java:38-49`、`util/d.java:28` |
| 落盘队列 | 3 张 SQLite 表：`events(_id,appid,package,event_name,priority,data BLOB,timestamp)`、`events_cloud(_id,app_id,cloud_data,timestamp,data_hash)`、`monitor(_id,appid,package,event_name,url,send_count,timestamp)` | `c/c.java:85`、`b/g.java:33`、`a/a.java:81` |
| 加密 | JSON → **gzip** → **AES/ECB/PKCS5Padding**（密钥/盐见 `d/a.java`）；另有 RSA（`d/b.java`，用 keystore 私钥解密） | `c/q.java:120-147`、`d/a.java:14-92`、`d/b.java:42-60` |
| 批次/重试 | `c/m.java`：失败后 `"will restart retry msg after N"`、`"fire retry timer after N"`、`"retry success"`；`c/i` 控制发送开关；`c/q` 记录 `"before zip and encrypt, len=… after=…"` | `c/m.java:59-99`、`c/q.java:120` |
| 超时 | `CONNECT/READ = 10000 ms`（`g/b.f2461a=10000`），另一常量 15000 | `g/b.java:24-25` |
| 采集开关 | 网络总开关 `setAccessNetworkEnable`；基础模式 `setBasicModeEnable`；隐私策略 `setCustomPrivacyPolicyAccepted` | `OneTrack.java:225-234,280-290` |

### 4.5 xCrash：崩溃捕获能力

| 项 | 观测 | 出处 |
|---|---|---|
| 版本 | tombstone 头 `"xCrash 2.4.9"` | `xcrash/j.java:211` |
| 捕获类型 | Java 未捕获异常（`h.java`）、ANR（`b.java`/`d.java`）、native 信号（`NativeHandler.nativeInit`，`libxcrash*.so`） | `XCrash.java:448-453`、`NativeHandler.java:92-148` |
| 信号 | native 侧注册（`nativeTraceSignalInit`），信号集在 `.so` 内 **[未知]** | `NativeHandler.java:41,99,129` |
| 落盘路径 | 默认 `context.getFilesDir() + "/tombstones"`（可 `setLogDir` 覆盖；本 APK 未覆盖） | `XCrash.java:430-433` |
| 文件名 | `tombstone_<pid>_<毫秒>_<pkg>__<type>{.java.xcrash,.anr.xcrash,.native.xcrash}` | `h.java:165`、`d.java:365`、`NativeHandler.java:112` |
| 是否上传 | **自身不上传**（12 个类中无 HTTP/URL）；由 OneTrack `CrashAnalysis` 读文件后走 `onetrack_bug_report` → `tracking.miui.com` | grep `http` 于 `xcrash` 0 命中；`CrashAnalysis.java:158-178`、`api/c.java:84` |
| 初始化方式 | `CrashAnalysis` 用**反射**调 `XCrash.init(Context, InitParameters)`（避免编译期依赖） | `CrashAnalysis.java:242-252` |
| 默认开哪些 handler | `InitParameters` 默认字段决定；`init()` 只在 `f11576g`(java)/`E`(anr)/`f11587r`(native) 为真时注册 | `XCrash.java:445-453`；具体默认值 **[未知]**（字段初值未逐一取证） |
| 调试残留 | `testJavaCrash/testNativeCrash` 公开方法存在（可被外部触发造崩溃） | `XCrash.java:391-402` |

### 4.6 `com.sun.nio.zipfs`：为什么需要它 + 实际调用点

**[观测]** APK 内含 `unknown/META-INF/services/java.nio.file.spi.FileSystemProvider`（apktool 解包，落在 `unknown/` 而非 `META-INF/` 之外），配合 12 个 `com.sun.nio.zipfs.*` 类注册 `jar`/`zip` scheme。

**唯一调用点**：`p0/e.java:152-157`

```java
public e(Path path) {                       // path = 待扫 APK
    this.f7366b = path;
    this.f7368d = FileSystems.newFileSystem(new URI("jar:" + path.toUri()),
                     k0.e(g1.w.a("useTempFile", Boolean.FALSE)), e.class.getClassLoader());
    ...
}
```
后续 `e.e(ScanTarget)` 用 `Files.walkFileTree(fs.getPath("/"), 深度 1)` 遍历 APK 内条目，按 `ScanTarget.{Dex,AndroidManifest}` 的 `pathMatcher` 命中并取 `Files.size`（`p0/e.java:160-179`）。

| 判定 | 内容 |
|---|---|
| 用途 | **不落地解压**直接在 APK 的 zip 命名空间里取 `classes*.dex` / `AndroidManifest.xml`，供 MI 引擎 NG（`MiEngineNG` + `t0.a`/`ScanTarget`）做包内扫描 |
| 为什么必须打包 | Android 默认 `FileSystemProvider` 不支持 `jar:`，标准 JDK 的 zipfs 不在 AOSP；不打包则 `FileSystems.newFileSystem` 抛 `ProviderNotFoundException` |
| 是否"框架类残留" | **否**，是活代码依赖（`p0/e` 被 `t0`/`MiEngineNG` 链路使用） |
| 其它 zipfs 使用者 | 0（全树仅 `p0/e.java:155` 一处 `newFileSystem`） |

### 4.7 `android.content.pm.{PackageParser,PackageUserState,VerifierInfo}`：为什么需要 + 调用点

**[观测]** 3 个类共 6.8 KB（`PackageParser.java` 5590 B，是被裁减的 AOSP 解析器）。调用点穷尽如下：

| 调用点 | 用法 | 用途 |
|---|---|---|
| `w0/d.java:47,80,112` | `new PackageParser().parsePackage(file, 0)` + `collectCertificates` + `generatePackageInfo(..., new PackageUserState())` | **离线解析任意 APK**（不安装、不受 `PackageManager` 对未安装包的 API 限制）；`i6 & 64` 时才收集签名 |
| `GuardProvider.java:168` | `w0.d.a(context, str, (i6&64)!=0?64:0)` | `call("parseApk")` 的实现（`:331` 分支） |
| `i0/g.java:11-46` | 对已有 `Package` 重设 `mSigningDetails` 后 `PackageParser.collectCertificates(pkg,false)` | **只取签名**（`signaturesMd5/Sha256`），供各引擎与云端上报 |
| `n0/g.java:821,831,1094` | `PackageParser.parsePackageLite(new File(str),0)`、`i0.g.a(...)` | 扫描结果补充 `versionCode/versionName/签名` |
| `i0/l.java:89,102,312`、`i0/f.java:106`、`c0/b.java:90`、`c0/d.java:118,157` | `parsePackageLite` / `new Package(name)` + `collectCertificates` | 灰产库落库与结果组装 |
| `g0/e.java:99` | `parsePackageLite` | 扫描目标元数据 |

| 判定 | 内容 |
|---|---|
| 用途 | ① `parseApk` IPC 对**任意路径**（含 `/data/app` 之外的下载目录）解析包名/版本/label/icon；② 统一取**签名摘要**用于本地库与云查（`mfSha256/signaturesMd5/signaturesSha256`）；③ `parsePackageLite` 只读 manifest，避免整包解析开销 |
| 为什么必须打包 | `PackageManager.getPackageArchiveInfo` 在部分 ROM/分区路径上受限，且 `parsePackageLite` 与 `SigningDetails`（v2/v3 签名方案）在 targetSdk 33 的系统 API 上不完整；自带 AOSP 副本可保证行为一致 |
| 风险点 | 自带解析器**不做** `unknown sources`/分区校验，等于把"任意 APK 解析"变成 provider 能力（受 `Guard_Provider` 系统签名权限保护） |

### 4.8 dex 侧腾讯残留（与"实际能力"区分）

| 残留 | 证据 | 定性 |
|---|---|---|
| `com.tencent.meri` / `com.tencent.tmsecure.demo` / `com.tencent.tmsecure.common.ISDKClient` / `com.tencent.feedback.eup.CrashReport` | 字符串常量（lead 已核，dex/smali 内） | **包名/类名残留**，无对应实现类 → 不构成能力 |
| `com.tencent.tmsecure.R`（28 KB） | `com/tencent/tmsecure/R.java` | 资源 ID 表残留 |
| `"Meri"` 自定义头 | `tmsdkobf/p9.java:263,283-291` | **活**：`detectNetworkState()` 用 `http://tools.3g.qq.com/wifi/cw.html`，以响应头 `Meri` 判定"网络被劫持/需认证" |
| `Encode`（`libxy.so`） | `Encode.java:43`；`lib/arm64-v8a` 无 `libxy.so` | **死代码** |

### 4.9 其余依赖：一句话定性

| 包 | 文件/行数 | 定性 | 是否有业务逻辑 |
|---|---|---|---|
| `dagger.hilt.android.internal.managers.**` | 3 文件（30/6/5 行） | Hilt 的 `ViewComponentManager`（Activity 组件持有者）编译期生成物的最小残留 | 否（本 APK 无 Activity） |
| `androidx.room.MultiInstanceInvalidationService` | 1 文件 147 行 | Room 多进程失效通知 Service | 否（无 `RoomDatabase` 子类引用它；`AndroidManifest` 未注册） |
| `okhttp3.internal.publicsuffix.PublicSuffixDatabase` | 1 文件 343 行 | OkHttp 的 eTLD+1 判定（cookie 域匹配） | 否（仅 OkHttp 内部；本 APK OkHttp 用于 `s0/s.java` 的引擎更新） |
| `org.apache.commons.logging.**` | 4 文件（953 行的 `LogFactory`） | Apache Commons Logging 门面 | 否（无 `LogFactory.getLog` 调用点） |
| `j$.util.stream.{DesugarCollectors,e}` | 2 文件 | **desugar 产物**（`java.util.stream.Collector` 在 API<24 的兼容层） | 否（纯兼容） |
| `kotlin.Metadata`、`kotlin.coroutines.jvm.internal.DebugProbesKt` | 2 文件 29 行 | Kotlin 编译器注解 + 调试探针 | 否（编译期/调试） |

**[推断]** 这 6 组均为"依赖闭包带入"，与 GuardProvider 的反病毒/清理业务无关；`room` 与 `commons-logging` 大概率来自某个 AAR 的传递依赖（`room` 仅 1 个类、`commons-logging` 无调用点）。

## 5. 配置项 / 常量 / 协议字段

### 5.1 腾讯侧网络端点（TMSDK / 国密通道）

| 端点 | 类型 | 触发条件 | 出处 |
|---|---|---|---|
| `https://tmfsdk.m.qq.com` | HTTPS（JSON 配置/业务） | 任意 shark 请求（云查 2016、WiFi 794/800、统计 29987/29988） | `tmsdkobf/w5.java:53-54,127-128`；`qscanner/impl/f.java:1310`；`p9.java:254` |
| `https://tmfsdk4.m.qq.com` | HTTPS（IPv4 上报专用） | IPv4 环境下的 IP 上报 | `w5.java:61-64` |
| `tmfsdktcp.m.qq.com:8001` | TCP 长连接（IPv6 通道） | `setAutoConnectionSwitch(true)` 后建链 | `w5.java:134-140` |
| `tmfsdktcpv4.m.qq.com:8002` | TCP 长连接（IPv4） | 同上 | `w5.java:68-74` |
| `120.241.190.227:8002` / `61.241.44.59:8002` / `14.18.175.81:8002` | 裸 IP TCP 兜底 | `PRE_USE_IP_LIST != "false"`（默认空串 → **走 IP 列表**） | `w5.java:150-169`、`TMSDKContext.java:36-47` |
| `2409:8c54:1003:1019::2d:8001` / `2408:8756:2cf2:19::d7:8001` / `240e:ff:f100:8019::6c:8001` | IPv6 TCP 兜底 | 同上 | `w5.java:84-103` |
| `https://mmgr.gtimg.com/qqsecure_config_update/qqsecure_config_update` | HTTPS（特征库更新清单） | `UpdateManager.check(UPDATE_FLAG_VIRUS_BASE)` | `tmsdk/common/module/update/a.java:675,811`；`TencentEngine.java:493` |
| `http://tools.3g.qq.com/wifi/cw.html` | **明文 HTTP**（网络认证/劫持探测） | `WifiDetectManager.detectNetworkState()` | `tmsdkobf/p9.java:269,286` |
| `http://tools.3g.qq.com/j/sslstrip` | **明文 HTTP**（SSLStrip 探测） | `detectSSLStrip()`（本 APK 未调用） | `p9.java:363` |
| `http://m.qq.com` | 字符串前缀比对（不发起请求） | SSLStrip 重定向判定 | `p9.java:373` |
| `www.qq.com:80` | TCP 连通性探测 | 网络可用性判定 | `tmsdkobf/m8.java:94` |

### 5.2 小米侧网络端点

| 端点 | 归属 | 触发条件 | 出处 |
|---|---|---|---|
| `https://api.sec.miui.com/antivirus/viruses` | GuardProvider 自身（云端病毒库） | 同步病毒库（`sync`） | `i0/d.java:203` |
| `https://api.sec.miui.com/antivirus/whitelist` | 同上 | 同步白名单 | `i0/d.java:55` |
| `https://flash.sec.miui.com/detect/app` | 同上 | 云端闪电扫描 | `j0/c.java:75` |
| `https://virus-engine.sec.miui.com` / `https://virus-engine.sec.intl.miui.com` | 同上（引擎/特征库） | 引擎更新（`s0/s.java:124` 作为 OkHttp baseUrl） | `q0/m.java:12` |
| `https://tracking.miui.com` | OneTrack 事件上报 | 有事件批且网络允许 | `util/x.java:35,129` |
| `https://tracking.intl.miui.com` / `tracking.india.miui.com` / `tracking.rus.miui.com` | OneTrack（区域） | 区域=INTL/IN/RU 或远端 host 覆盖 | `util/x.java:38-44,145-147` |
| `https://sdkconfig.ad.xiaomi.com`（+`.intl`/`.india`/`.rus`） | OneTrack 配置拉取 | `RegionDomainManager` 初始化/配置刷新 | `util/x.java:47-56,132-142` |
| `<host>/api/open/device/writeBack` | OneTrack 广告监测回写 | 广告监测启用时（本 APK 未走 `adTrack`） | `util/g.java:38-49`、`util/d.java:28` |
| `api.ad.xiaomi.com`（仅字符串判定） | OneTrack 配置过滤 | 解析广告配置时 | `a/b/b.java:62` |

### 5.3 AVL：**无自有硬编码域名**（重要结论）

**[观测]** 3 次穷尽检索均为 0 命中：① `com/avl/**` 全树 `https?://` 0 命中；② `assets/avlsdk` 内 26 个文件（含 `sdk.conf`/`version.conf`/`avllib.conf`）无主机名，`sdk.conf` 为加密/混淆块；③ `libavlm.so`/`libavlasys.so`/`libavlurl.so` 字符串扫描仅得 `http://www.w3.org`、`http://www.wapforum.org`、OpenSSL FAQ 等**库内固定串**，无业务域名。
**[观测]** 上报/更新基址来自配置：`j/p.java:93`（`this.f1543i = a(kVar, new c(kVar.c()).a())`），最终拼 `?channel=<渠道>&pkg=<包名>`（`:142-161`）。宿主未设 `logUploadUrl`（`AvlEngine.java:99`）。
**[推断]** AVL 的云查/日志/更新端点是**下发式（config/license 驱动）**，本 APK 未在静态层固化 → 若启用，真实域名只能在运行时从 `sdk.conf`/license 解出。**[未知]** 具体域名。

### 5.4 OneTrack 上报 JSON 字段（协议字段表）

请求体两段式：`{"e":<公共参数>, "d":<事件体>}`（`api/c.java:41-43,53-58` 等）。

| 段 | key | 含义 | 出处 |
|---|---|---|---|
| e | `event` | 事件名 | `f/b.java:130` |
| e | `imei` | IMEI 的 MD5 | `f/b.java:133`；`DeviceUtil.b()` |
| e | `oaid` | OAID 明文 | `:136` |
| e | `sn` | 序列号（预留，未见赋值） | `:139` |
| e | `gaid` | Google 广告 ID（国际推荐事件） | `:142` |
| e | `android_id` | AndroidID（预留） | `:145` |
| e | `instance_id` | 实例 ID（`o.a().b()`） | `:148` |
| e | `mfrs` / `model` | 厂商 / 型号 | `:151,:154` |
| e | `platform` = "Android" | — | `:157` |
| e | `miui` / `build` / `os_ver` | MIUI 版本 / 构建 / 系统版本 | `:160,:163,:166` |
| e | `app_id` / `app_ver` / `pkg` / `channel` / `sdk_ver` | 应用与渠道 | `:169-196` |
| e | `e_ts` / `tz` / `net` / `region` | 时间/时区/网络/区域 | `:181-190` |
| e | `uid` / `uid_type` / `sid` / `plugin_id` | 用户与会话 | `:193-205` |
| e | `sdk_mode` / `ot_first_day` / `ot_test_env` / `ot_privacy_policy` / `market_name` / `ot_ad` / `ot_basic_mode` | 模式与开关 | `:119-127,123` |
| d(`onetrack_dau`) | `first_open` / `imeis` / `imsis` / `config_status` | 首启、IMEI 列表、IMSI 列表(MD5)、配置状态 | `api/c.java:108-117` |
| d(`onetrack_bug_report`) | `exception` / `type` / `message` / `feature` / `crashtime` | 崩溃摘要 | `api/c.java:90-94` |
| d(`threats`…) | 8 个扫描字段 | 见 §4.4.2 | `MiEngineNG.java:201` |

### 5.5 域名/端点总表（合并）

| 域名（含端口） | 归属 SDK | 触发条件 | 出处 | 明文? |
|---|---|---|---|---|
| `api.sec.miui.com` | GuardProvider 自身 | 病毒库/白名单同步 | `i0/d.java:55,203` | HTTPS |
| `flash.sec.miui.com` | GuardProvider 自身 | 云端闪电扫描 | `j0/c.java:75` | HTTPS |
| `virus-engine.sec.miui.com` / `virus-engine.sec.intl.miui.com` | GuardProvider 自身 | 引擎/特征库更新 | `q0/m.java:12` | HTTPS |
| `tracking.miui.com` | OneTrack | 事件上报（国内） | `util/x.java:35` | HTTPS |
| `tracking.intl.miui.com` | OneTrack | 事件上报（国际） | `util/x.java:38` | HTTPS |
| `tracking.india.miui.com` | OneTrack | 区域=IN | `util/x.java:44` | HTTPS |
| `tracking.rus.miui.com` | OneTrack | 区域=RU | `util/x.java:41` | HTTPS |
| `sdkconfig.ad.xiaomi.com` | OneTrack | 拉配置（国内） | `util/x.java:47` | HTTPS |
| `sdkconfig.ad.intl.xiaomi.com` | OneTrack | 拉配置（国际） | `util/x.java:50` | HTTPS |
| `sdkconfig.ad.india.xiaomi.com` | OneTrack | 拉配置（印度） | `util/x.java:53` | HTTPS |
| `sdkconfig.ad.rus.xiaomi.com` | OneTrack | 拉配置（俄） | `util/x.java:56` | HTTPS |
| `api.ad.xiaomi.com` | OneTrack | 广告配置过滤（字符串判定） | `a/b/b.java:62` | — |
| `tmfsdk.m.qq.com` | TMSDK | 云查/WiFi/统计 | `w5.java:53` | HTTPS |
| `tmfsdk4.m.qq.com` | TMSDK | IPv4 上报 | `w5.java:63` | HTTPS |
| `tmfsdktcp.m.qq.com:8001` | TMSDK | TCP 长连接 | `w5.java:137` | 私有协议 |
| `tmfsdktcpv4.m.qq.com:8002` | TMSDK | TCP 长连接 | `w5.java:71` | 私有协议 |
| `120.241.190.227:8002` / `61.241.44.59:8002` / `14.18.175.81:8002` | TMSDK | IP 直连兜底 | `w5.java:154-166` | 私有协议 |
| `2409:8c54:1003:1019::2d:8001` 等 3 个 IPv6 | TMSDK | IPv6 兜底 | `w5.java:88-100` | 私有协议 |
| `mmgr.gtimg.com` | TMSDK | 特征库更新 | `update/a.java:675` | HTTPS |
| `tools.3g.qq.com`（`/wifi/cw.html`、`/j/sslstrip`） | TMSDK（WifiDetect，代号 Meri） | 网络认证/劫持/SSLStrip 探测 | `p9.java:269,363` | **HTTP** |
| `m.qq.com` | TMSDK | SSLStrip 重定向比对 | `p9.java:373` | 不请求 |
| `www.qq.com:80` | TMSDK | TCP 连通性探测 | `m8.java:94` | 裸 TCP |
| （无硬编码域名，配置驱动） | AVL | 云查/日志/URL 规则更新 | §5.3 | 未知 |

**合计 22 个主机名 + 6 个裸 IP/端口。**

### 5.6 其余关键常量

| 常量 | 值 | 出处 |
|---|---|---|
| AVL SDK 版本 | `4.0.1`（门面）/`4.0.0`（模块） | `AVLEngine.java:19`、`security/g.java:75` |
| AVL 引擎/库版本 | `av.siglib=20220928.ah`、`av.engine=351`、`url.siglib=20180917.zz`、`url.engine=124` | `assets/avlsdk!sdk_conf/version.conf` |
| AVL appkey | `D5DFB878335A3EF4E76F60ED470B8EC8`（meta-data `AVL_OPENSDK_APPKEY`） | `a/e.java:39` + Manifest |
| AVL 设备档位 | `FLAG_LITE_DEVICE=0`/`FLAG_HIGH_DEVICE=1` | `AVLSdkConfig.java:5-6` |
| TMSDK SDK 版本 | `"6.5.0 20220325111039"`；原生库名 `Tmsdk-2.0.13-mfr` | `TMSDKContext.java:25,31` |
| TMSDK 特征文件 ID | 62 个（含 `70003`→`.amf`、`40706`→`.dat`） | `update/UpdateConfig.java:197,207,311` |
| TMSDK 统计 ID | `29987/29988`（自动连接）、`29953`（云查）、`29979`（DNS/钓鱼）、`29978`（ARP）、`1320056/1320057`（扫描）、`1320068` | `TMSDKContext.java:177`、`qscanner/impl/f.java:1264`、`p9.java:203,262,320,195` |
| SM4 默认密钥 | `73 42 5A 55 18 08 06 5C 76 70 21 70 24 24 33 29` | `com/tencent/tmf/gm/b.java:7` |
| SM2 公钥长度约束 | 128 hex（X||Y 各 32 字节） | `tmsdkobf/j7.java:161-164`、`gm/b.java:45-51` |
| xCrash 版本 | 2.4.9 | `xcrash/j.java:211` |
| xCrash 落盘目录 | `filesDir/tombstones` | `XCrash.java:431` |
| OneTrack appId | `2882303761517522561` | `z/b.java:22` |
| OneTrack SDK 版本 | `BuildConfig.SDK_VERSION`（值未取）→ 上报字段 `sdk_ver` | `f/b.java:323`；**[未知]** 具体值 |
| OneTrack 超时 | 连接/读 10000 ms；另有 15000 | `g/b.java:24-25` |
| OneTrack 队列表 | `events` / `events_cloud` / `monitor` | `c/c.java:85`、`b/g.java:33`、`a/a.java:81` |
| AVL 关键资产 | `assets/avlsdk`(1.53 MB ZIP, 26 项)、`assets/mdetector`(ELF32 ARM, 136 KB, **无 Java 引用**)、TMSDK `assets/70003.amf`/`40706.dat`/`licence.conf`/`licence1.conf` | 解包清单 |

## 6. 权限与攻击面 / 风险点

### 6.1 隐私采集链路（只列"已启用"；采集→调用→存储→去向）

| # | 采集了什么 | 谁调用（文件:行） | 存哪 | 发给谁 |
|---|---|---|---|---|
| 1 | `ANDROID_ID`、`Build.MODEL` | `TencentEngine.java:402` → `TMSDKContext.java:188-207` | 进程内静态 `xb.f10474a`、`qb.f9915a`（不落盘） | TMSDK shark 请求（`tmfsdk.m.qq.com`/TCP 8001-8002）与统计打点 `y9` |
| 2 | WiFi `SSID`、`BSSID`、DHCP `DNS1/DNS2` | `TencentEngine.java:122` → `tmsdkobf/p9.java:210-254` | 仅请求体内存 | Tencent cmd **794**（`p9.java:254`），云判定 DNS 劫持/钓鱼 |
| 3 | 当前连接 BSSID + 扫描到的 `ScanResult` | `TencentEngine.java:107-114`（宿主自采）→ `WifiDetectManager.detectSecurity` | 不落盘 | 仅本地判定（异常时打点 `1320068`） |
| 4 | IMEI 列表（明文）、IMSI 列表（MD5） | `api/c.java:111,114`（`onetrack_dau`） | SQLite `events.data`（AES 加密 BLOB） | `https://tracking.miui.com/track/v4` |
| 5 | IMEI(MD5)、OAID、AAID | `f/b.java:304-305`（每条事件公共参数）、`util/g.java:45-46`（writeBack） | 同上；`instanceId` 另存 SP | 同上 / `…/api/open/device/writeBack` |
| 6 | 被扫 APK 的包名/版本/签名摘要/安装器/威胁名 | `MiEngineNG.java:201`（`threats`/`blackgrays*`） | SQLite `events` | 同上 |
| 7 | 崩溃现场（tombstone：堆栈、`/proc/maps`、fd、logcat 片段） | `XCrash` 落盘（`XCrash.java:431`）→ `CrashAnalysis.java:158-178` 读取 | `filesDir/tombstones/*.{java,anr,native}.xcrash` | `onetrack_bug_report` → `tracking.miui.com`（仅摘要：`exception/type/message/feature/crashtime`） |
| 8 | 任意 APK 的包名/版本/label/icon/签名（含未安装包） | `GuardProvider.java:168`（`parseApk`）→ `w0.d.a` → 自带 `PackageParser` | 不落盘，随 Binder 返回 | 返回给持 `Guard_Provider` 权限的调用方（系统/同签名） |

**未启用（重要，避免误判）**：OneTrack 的 IMEI/IMSI **受 `api/c.java:109` 区域开关门控**（见 §7-④）；TMSDK 的"已安装应用列表"仅在扫描列表为空时触发（本 APK 不触发，`qscanner/impl/f.java:434`+`TencentEngine.java:470-472`）；`UrlCheckManager`/短信/通讯录/剪贴板/定位在本 APK **均无调用点**（§4.2.3、§4.4.3）。

### 6.2 权限与攻击面

| 面 | 观测 | 风险 |
|---|---|---|
| `provider guard` | `protectionLevel=system|signature`（简报已定） | 外部第三方不可直接访问；但能力本身极强（见下） |
| `parseApk` | 接受**任意路径**字符串，自带解析器不做来源/分区校验（`GuardProvider.java:168`、`w0/d.java:47`） | 若调用方（系统组件）被诱导传入恶意 APK，可触发解析器 OOM/异常（DoS）；返回值泄露 label/icon/签名（信息收集） |
| `getRiskAppData` | 枚举已装应用 × 本地灰产库（简报已定） | 应用清单侧信道（仅系统可调） |
| `sync_scan`/`install_scan` | 对任意路径做本地+云扫描（`TencentEngine.java:472`） | 云查会把**包名/版本/证书摘要**发往腾讯（§6.1-6） |
| AIDL `IURLScanServer` | `y/b.java:13-37` 手写 Parcel 接口 | URL 经 Binder 传入 AVL 本地库判定；返回码 0/1/2 语义在 Java 层无枚举，调用方若误判"1"为安全将漏报（**[未知]**） |
| xCrash 调试方法 | `testJavaCrash/testNativeCrash` 为 `public static`（`XCrash.java:391-402`） | 同进程任意代码可主动制造崩溃（调试残留） |
| TMSDK 明文 HTTP | `http://tools.3g.qq.com/wifi/cw.html`（`p9.java:269`） | MITM 可伪造 `Meri` 响应头 → 把"被劫持"误报为"安全"（安全性结论可被操纵） |
| 硬编码 SM4 密钥 | `gm/b.java:7` | 若 TMSDK 通道使用默认 key（而非会话 key），抓包可离线解密 |
| TMSDK 裸 IP 直连 | `120.241.190.227:8002` 等（`w5.java:154-166`） | 绕过 DNS/域名封禁策略；流量特征固定 |
| OneTrack 调试组件 | `com.xiaomi.onetrack.otdebugger.FloatWindowService`（浮窗调试器）、`com.xiaomi.onetrack.DEBUG` 等开关（lead 已核，dex 字符串） | 若组件被导出，第三方可拉起浮窗调试界面/触发上传（**[未知]** 需以 Manifest 的 `exported` 定论；本 APK 唯一 exported 组件为 provider+service，故推断**未导出**） |
| 死代码带入 | `libturingvd.so` + `TNative`（零引用）、`Encode`（`libxy.so` 缺失）、`sms_check_v4`（主体缺失）、AVL 411 类中大量未用 native | 增大攻击面与体积；native 自拉起无法从 Java 层排除 |

### 6.3 "声称 vs 启用"总账（一句话版）

- **AVL**：能扫文件、扫 URL、云查、限流、证书解析、日志上报；本 APK 只开 `scan`/`scanURL`/`update`/`updateURLRule`，**关日志、无云配置**。
- **TMSDK**：能扫 APK（本地+云）、更特征库、查 URL 信誉、深度清理、读短信/流量短信、号码标记、WiFi 七类检测；本 APK 只开 **APK 扫描（含云）+ 特征库更新 + WiFi 四类检测（网络/DNS钓鱼/ARP/加密类型）**。
- **OneTrack**：能采 IMEI/IMSI/OAID/GAID/AAID/MAC/机型/网络/存储/屏幕、能自动埋点 Activity、能抓崩溃；本 APK 只发 **4 个业务事件 + 2 个 SDK 自动事件**，采集面随区域开关与 `GAIDEnable=false` 收缩。
- **国密**：SM2/SM4 均 JNI；SM4 有硬编码默认密钥，SM2 公钥服务端下发（128 hex）。
- **xCrash**：只落盘不上传，上传由 OneTrack 代劳。
- **zipfs/PackageParser**：都是活依赖（读 APK 内 zip；离线解析 APK+取签名），不是残留。

## 7. 未解决问题

| # | 问题 | 已做到哪一步 | 需要什么才能闭环 |
|---|---|---|---|
| ① | AVL 云查/日志/更新/URL 规则的**真实域名** | 已证 Java 层与 3 个 .so 均无业务域名；基址来自 `j/p.java:93` 的配置项（`com.avl.engine.j.c`） | 解密 `assets/avlsdk!sdk_conf/sdk.conf` 或对 `AVLEngine.update()`/`scanURL` 抓包 |
| ② | `AVLEngine.scanURL` 返回 `0/1/2` 的语义 | 已证 Java 只做透传（`UrlDetector.java:201-207`），无枚举 | AVL 官方文档或 native `matchURLEx` 逆 |
| ③ | `avlpk_{apn,amc,sfmf,sgnl,kw,dhc,des}.avl` 的类别含义 | 仅 `pornware`（色情）可自证；其余按命名推测（§4.1.3） | 规则文件格式解析 / 引擎文档 |
| ④ | OneTrack 是否真的上报 IMEI/IMSI | 已定位门控 `api/c.java:109`（`q.h()` 决定是否跳过）与默认开关 `isIMEIEnable=true` | 运行时取 `util/q.h()`（MIUI 区域/隐私设置）真值 |
| ⑤ | xCrash 默认启用了哪些 handler（java/anr/native） | 已证 `init()` 按 `f11576g/f11587r/E` 三布尔注册（`XCrash.java:445-453`） | 逐字段读 `InitParameters` 初值 |
| ⑥ | Turing 风控是否在运行、采集哪些字段 | 已证 `TNative` 全 native（9 个 `SparseArray` 方法）且 **Java 零引用** | 逆 `libturingvd.so` / 运行时验证是否被自拉起 |
| ⑦ | TMSDK `x5`/`y9` 上报体的完整字段映射 | 已证 117 个 JCE 结构归 tmsdk（t-cloud 已给编码规则），并定位 cmd 794/800/2016/29987/29988/29953/29979 | 建立 cmd↔JceStruct↔字段 的完整对照表 |
| ⑧ | TMSDK root 检测 su 路径全集 | lead 已从 dex/native 侧给出 11 条路径 + `content://com.lbe.security.miui.su` | 本人未复核 `libTmsdk-2.0.13-mfr.so` 字符串，待交叉验证 |
| ⑨ | `assets/mdetector`（ELF32 ARM）谁在加载 | 已证 Java 层零引用（grep `mdetector` 0 命中） | 逆 `libavlm.so`/`libTmsdk`，或运行时监控 `execve`/`dlopen` |
| ⑩ | `assets/{40706.dat,70003.amf}` 的格式 | 已证 `amf` magic + `UpdateConfig` 以 ID 命名（`40706`→`.dat`、`70003`→`.amf`）；`40706` 由 `qscanner/impl/f.java:415` 读取 | 解出加密/序列化格式（`b2`/`qa` 反序列化器） |
| ⑪ | OneTrack 批次大小、重试次数、退避上限的数值 | 已证机制（`c/m.java:59-99`、`c/q.java:120-147`） | 逐常量定值（`c/i`、`c/m` 的 int 字段） |
| ⑫ | `parseApk` 是否能被外部传入**目录**（走系统 API 分支） | 已证 `w0/d.a` 对目录走 `getPackageArchiveInfo`（`w0/d.java:56/89/121`），对文件走自带 `PackageParser` | 审计 `GuardProvider.java:160-180` 的 path 校验与调用方 |
| ⑬ | OneTrack `sdk_ver` 的实际值 | key 已定位（`f/b.java:323`），值来自 `BuildConfig.SDK_VERSION` | 读 `com/xiaomi/onetrack/BuildConfig.java` 常量 |
| ⑭ | OneTrack 调试浮窗组件是否 exported | lead 给出 `otdebugger.FloatWindowService`；本 APK 唯一 exported 为 provider+service（简报） | 直接核对 `AndroidManifest` 的 `android:exported` |

## 8. REA 证据 ID 列表

工具：REA MCP `inspect_android_method`，provider `jadx` 0.7.1，target `GuardProvider-2.0.3-20231124.0.apk`（SHA-256 校验一致）。三次调用均为**静态反编译观测**，不执行 APK。

| evidence_id | 目标 | 证明了什么 |
|---|---|---|
| `ev_3a4f329853af4179576b94977a9ea352640a435f2b597f95dd378e4d73297be0` | `com.miui.guardprovider.engine.tencent.TencentEngine.e(f0.a$InterfaceC0073a)` | `TMSDKContext.setDeviceInfo(ctx, Settings.Secure.getString(cr,"android_id"), Build.MODEL)` 与 `TMSDKContext.init(ctx,null,cfg)` 的**同方法内先后关系**（隐私注入先于 SDK 初始化）；`f1767f` 单次初始化语义 |
| `ev_52917bb293318b419051f6073ed950055fb152d3f34f00c58517a2db0c3fa348` | `w0.d.a(Context,String,int): PackageInfo` | 自带 `PackageParser` 的**活调用**（`parsePackage`+`collectCertificates`+`generatePackageInfo`）与 SDK≥29/28/<28 三档分支；目录走系统 `getPackageArchiveInfo`，文件走自带解析器 |
| `ev_4c3c9e940c85e3845fdb29bda1d56aad8043ddfc10765930229e329d613dce9f` | `z.b.<init>()` | OneTrack 唯一初始化配置：`appId=2882303761517522561`、`channel=包名`、`ExceptionCatcherEnable=true`、`Mode.APP`、`AutoTrackActivityAction=true`、`GAIDEnable=false` |

**取证过程中的一次失败与修正**（留痕）：首次以 `&lt;init&gt;` 作为 `method_name` 调用 `z.b` 被拒（`invalid_input: Method z.b.&lt;init&gt; was not found`），改回字面量 `<init>` 后成功 —— 说明该方法名需原样传入，不能 HTML 转义。

**与其它报告的边界**：JCE 编码规则与 `com.qq.taf.jce` 117 个结构的分类由 t-cloud 在 `20_cloud.md` 覆盖，本文只引用其结论（§5.1、§7-⑦），不重复推导。native 侧（`libavlm.so`/`libTmsdk`/`libturingvd.so`/`mdetector`）只做了字符串级扫描，未做反汇编。

## 附录 A：资产与原生库清单（本文用到的字节级证据）

| 路径 | 大小 | 观测 |
|---|---|---|
| `lib/arm64-v8a/libavlm.so` | 2 304 352 | AVL 主引擎（`scan`/结果解析） |
| `lib/arm64-v8a/libavlurl.so` | 629 976 | URL 检测引擎 |
| `lib/arm64-v8a/libavlasys.so` | 89 144 | AVL 辅助 |
| `lib/arm64-v8a/liburldetectorsys.so` | 14 408 | `System.loadLibrary("urldetectorsys")` 目标（`UrlDetector.java:80`） |
| `lib/arm64-v8a/libTmsdk-2.0.13-mfr.so` | 370 088 | TMSDK native（AmScannerV2/root 检测） |
| `lib/arm64-v8a/libtmf-gm.so` | 177 920 | SM2/SM4 JNI |
| `lib/arm64-v8a/libturingvd.so` | 240 448 | Turing 风控（Java 侧零引用） |
| `lib/arm64-v8a/libxcrash.so` / `libxcrash_dumper.so` | 88 360 / 129 344 | xCrash native |
| `lib/arm64-v8a/libflamingo.so` / `libams-1.3.0-mfr.so` / `libdce-1.1.21-mfr.so` | 1 506 088 / 744 392 / 202 976 | 小米侧：云控/应用管理/深度清理（不在本文范围） |
| `assets/avlsdk` | 1 566 355 | ZIP，26 项：`av/avl/android/avlpk_*.avl`(8)、`url/url/*.avl`(5)、`av/kw/**/*.des`(7)、`sdk_conf/{sdk.conf,version.conf}`、两组 `liscense.conf`+`avllib.conf` |
| `assets/mdetector` | 136 636 | **ELF32 ARM**（`7f 45 4c 46 01 01 01`），Java 零引用 |
| `assets/70003.amf` / `assets/40706.dat` | 680 928 / 9 368 | `amf\0` magic / 加密块；TMSDK `UpdateConfig` 按 ID 命名 |
| `assets/licence.conf` / `licence1.conf` | 392 / 368 | TMSDK 许可（`f5.java:146-150` 读取 `licence.conf`/`licence<N>.conf`） |
| `unknown/META-INF/services/java.nio.file.spi.FileSystemProvider` | — | 注册 `com.sun.nio.zipfs.*` provider（§4.6） |

