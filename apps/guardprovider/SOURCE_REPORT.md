> **归档警示（已纠错）**：这是原分析材料的脱敏归档，不能代替 [最终复核报告](REPORT.md)。原文关于“明文 IMEI 列表”“默认配置即实际上传”、绝对覆盖率及未知 native 行为的部分结论不成立。IMEI/IMSI 的相关 DAU 列表经过 MD5，仍受区域/权限/联网门控。证书过期也不能单独推出密文无法解密。请先阅读最终报告与其中纠错表。本文删除本地身份路径，原有文件行号引用可能指未纳入发布的完整分析树。

# GuardProvider 2.0.3 完整逆向分析报告（整合版）

> **SHA-256**：`b0c02fa682b8ca82429b471113d0702042ac76234c85fdcd8efeb7c03d313024`（13,251,548 B）
> **方法**：REA（jadx-headless 0.7.1 / apk-signature / ELF 语义层）作为取证主干；全量 JADX 反编译（3,231 个类落盘）+ apktool 3.0.3（smali/res/DEX 字符串池）+ 12 个 `.so` 与 8 个 assets 的字节级解析作为覆盖面基座。
> **证据纪律**：每条结论标注 **〔观测〕**（源码/字节直接可见，附 `文件:行` 或偏移）、**〔推断〕**（有依据的语义归纳）、**〔未知〕**（未覆盖）。全部为静态结论，APK 未执行。
> **附件**：`10_core.md`（主控层）、`15_obf.md`（混淆层语义地图）、`20_cloud.md`（云端/DB/协议）、`30_native.md`（native/assets）、`40_vendor.md`（第三方 SDK）、`_obf_index.tsv`（全类索引）、`dex_strings_all.txt`（29,263 条 DEX 串）。

---

## 0. 一句话定性

这不是一个"App"，而是 **MIUI 系统内置的病毒扫描与安全检测后台组件**：没有 Activity、没有桌面入口、没有广告主流程。

它把三种杀毒引擎（**小米自研 / 安天 AVL / 腾讯 TMSDK**）包在同一层抽象下，通过 **一个 `system|signature` 级 ContentProvider（`content://guard`）** 和**两个同名权限保护的 Binder Service**，向 **小米安全中心（`com.miui.securitycenter`）** 与**包安装器**提供 9 类能力：

| # | 能力 | 落点 |
|---|---|---|
| 1 | **APK 静态扫描**（同步/异步/按场景/增量/全盘） | `guard/sync_scan`、`install_scan`、`IAntiVirusServer.w/p/u` |
| 2 | **多引擎病毒判定与结果合并** | `n0/g` → 三引擎并行 → `D()` 合并 |
| 3 | **云端指纹校验**（只传指纹，不传 APK） | `p0/f` → `POST /virus/scan` |
| 4 | **黑白灰名单库同步与匹配** | Room 8 表 + 3 个明文 SQLite |
| 5 | **特征码/规则引擎**（JEXL3 表达式 + Rust Aho-Corasick） | `v5`(=JEXL3) + `p0/g` + `libflamingo.so` |
| 6 | **URL 钓鱼检测**（仅安天引擎） | `IURLScanServer.H` → `AVLEngine.scanURL` → `libavlurl` |
| 7 | **WiFi 安全检测**（仅腾讯引擎，4 类） | `IWifiDetectObserver` → `TencentEngine.n()` → TMSDK |
| 8 | **病毒库/引擎更新**（三条独立通道） | `IAntiVirusServer.h`、`q0/*`、AVL/TMSDK native |
| 9 | **风险（灰产）应用清单导出 + 事件上报** | `call("getRiskAppData")`、`TRACK_ANTIVURS_DATA` 广播 |

**判定内核全部在 native**：`libavlm.so`（安天引擎本体，98 个 `AVLM_*` C API）、`libavlurl.so`（URL 信誉，33 个 `AVLM_Match_*`）、`libams`（腾讯 AMF 特征扫描）、`libflamingo.so`（小米自研 Rust Aho-Corasick 匹配）、`libturingvd.so`（腾讯 Turing 设备风控/root/模拟器检测）。

---

## 1. 身份与完整性

### 1.1 包身份〔观测〕

| 项 | 值 |
|---|---|
| package | `com.miui.guardprovider` |
| versionName / versionCode | `2.0.3-20231124.0` / `209` |
| minSdk / targetSdk / compileSdk | 26 / 33 / 33 |
| Application | `com.miui.guardprovider.GuardApplication` → `b extends Application`（Hilt 注入） |
| label | `系统安全组件`（zh-rCN）/ `System security components`（en） |
| 构建 | R8 8.0.46 **full mode**（两个 dex 均带 R8 元数据），`min-api=26` |
| 其他 | `allowBackup=false`、`largeHeap=true`、`extractNativeLibs=false`、`requestLegacyExternalStorage=true` |

### 1.2 归档构成〔观测，155 项〕

| 类别 | 内容 |
|---|---|
| DEX | `classes.dex` 4,155,760 B；`classes2.dex` **3,500 B（仅 `j$/util/stream/DesugarCollectors`，无业务逻辑）** |
| native（**仅 arm64-v8a，12 个**） | `libavlm`(2,304,352) `libflamingo`(1,506,088) `libams-1.3.0-mfr`(744,392) `libavlurl`(629,976) `libTmsdk-2.0.13-mfr`(370,088) `libturingvd`(240,448) `libdce-1.1.21-mfr`(202,976) `libtmf-gm`(177,920) `libxcrash_dumper`(129,344) `libavlasys`(89,144) `libxcrash`(88,360) `liburldetectorsys`(14,408) |
| assets | `avlsdk`(1,566,355) `70003.amf`(680,928) `mdetector`(136,636) `40706.dat`(9,368) `licence.conf`(392) `licence1.conf`(368) `dexopt/baseline.prof{,m}` |
| META-INF | 41 项：`CERT.RSA/SF`、`MANIFEST.MF`、24 个 `androidx.*_*.version`、**`flamingo.dat`(304)**、`app-metadata.properties`、6 个 `services/*` SPI 注册 |

> **规模真相**：3,231 个类中，**真业务只有约 25 个混淆包**；最大残留是 **kotlin-reflect + 内嵌 Kotlin 编译器前端（约 45 个包）**，另有协程、Gson、OkHttp/Okio、Retrofit、protobuf、Room、JEXL3（详见 §9.1、附件 `15_obf.md`）。

### 1.3 签名〔观测，REA `inspect_apk_signature`〕

| 项 | 值 |
|---|---|
| 方案 | **v1(JAR) + v2 + v3 三签同存**，无 v3.1 |
| 签名者 | 三者同一证书：`c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025` |
| 自洽性 | `self_consistent=true`（声明摘要 == 当前字节摘要） |
| 密钥轮转 / source stamp | 无 / 无 |
| **限制** | **未做密码学验签**；证书链、撤销、真实性均未评估。本报告不对"是否官方发布"下结论 |

---

## 2. 权限与信任边界（理解本软件的第一把钥匙）

### 2.1 导出组件 —— 全 APK 只有 3 个〔观测〕

| 类型 | 名称 | exported | 保护 |
|---|---|---|---|
| `provider` | `com.miui.guardprovider.GuardProvider`，authority **`guard`** | **true** | `permission=com.miui.guardprovider.permission.Guard_Provider`，`protectionLevel=system\|signature` |
| `service` | `com.miui.guardprovider.manager.SecurityService`，action `…action.antivirusservice` / `…action.urlscan` | **true** | 同上 |
| `service` | `androidx.room.MultiInstanceInvalidationService` | false | — |

**没有 Activity、没有任何静态 BroadcastReceiver。** 因此全部功能入口收敛为：**1 个 Provider（7 个入口）+ 1 个 Service（26 个 Binder 事务）**。

### 2.2 信任边界的真实含义〔推断〕

`system|signature` 意味着：**`/system/priv-app` 下的任一预置应用（持 `system` 属性）或同签名应用**都能穿透。这不是"第三方不可达"，而是"**平台内任何特权组件可达**"。而它暴露的能力极强（任意路径 APK 解析、任意路径扫描、跨 provider 内容代理读取、全量应用清单导出）—— 见 §12 的攻击面。

### 2.3 启动时序〔观测，`GuardApplication.java`〕

```
attachBaseContext(ctx)
  ├─ f1743d = this                                   // 全局单例，多处直接引用
  └─ if (isUserUnlocked) f.b(ctx)                    // 已解锁 → 立即建 prefs 单例
     else registerReceiver(ACTION_USER_UNLOCKED)      // 未解锁 → 等解锁
onCreate() → super.onCreate()（构建 Hilt 组件）→ w.c.a(this)   // w/c.java:6-8 是空方法
USER_UNLOCKED → f.b(ctx) + 注销接收器
```
**〔观测〕Application 阶段零初始化**：不 init TMSDK/AVL/OneTrack、不建库、不读系统属性。**全部懒加载**——第一次扫描/更新时才拉起引擎与埋点。

### 2.4 网络总闸（全局唯一开关）〔观测，`GuardApplication.java:38-50`〕

```
e(Context):
  国内版: = SystemProperties.getBoolean("persist.sys.sc_allow_conn", false)
  国际版: ok = 已装 com.miui.securitycenter && 该属性为真
          region = SystemProperties.get("ro.miui.customized.region", "")
          if region == "jp_kd": ok &&= (Settings.Secure "antivirus_engine_network_enable" == 1)
          return ok
```
**全树仅 5 处调用**〔观测，t-core〕：`f0/a:66`（引擎基类 `b()`）、`h0/b:84,153`（云端客户端）、`n0/g:642`（扫描前白名单刷新）、`z/b:41`（OneTrack）。
→ **该属性为 false 时：三/四引擎的联网位、病毒库与白名单同步、反诈上传、OneTrack 埋点、扫描前白名单刷新全部关闭**，离线特征库仍可用。

---

## 3. 对外功能面（IPC 全表）

### 3.1 ContentProvider `content://guard`〔观测，`GuardProvider.java`〕

| URI / 方法 | code | 实现 | 功能 |
|---|---|---|---|
| `guard/engine_info_list` | 1005 | `query :384-394` | 返回 11 列引擎元数据：`Name, NameString, DescriptionName, DialogTitle, DialogHeader, DialogPolicy, DialogCloudScan, DialogSummary, DialogUrl, Enable, SupportCloud`。数据源 = `f0.b` 枚举 + `getStringArray(f3553h)` 的 8 个本地化串 + `g.z().w(name)` + `f3551f` |
| `guard/sync_scan` | 1006 | `query :395-399` → `f()` | **同步扫描单文件**。`selectionArgs` 必须 2 个 `[path, deep]`；返回 `MatrixCursor(Type, VirusName, Description, EngineName, AdType, AdName, AdDescription)` |
| `guard/virus` | 1007 | `query/insert/delete` | 明文表 `t_virus_white_list` 的**增删查**（病毒白名单）；写后 `notifyChange` + `c0.d.g(ctx).c(paths)` |
| `guard/install_scan` | 1008 | `query :403-415` | **同步扫描（带第 4 参）**。3 参 `[path, deep, engineName]` |
| `guard/support_incremental` | 1009 | `query :405-411` | `i0.h.h(ctx)` 为真返回 `{support:1}`，**否则返回 `null`** |
| `call("parseApk", path, bundle)` | — | `d() :159-190` | **解析任意 APK**：系统 `PackageManager` 失败则回退**自带 `PackageParser`**；`flags&128` 额外返回 `icon`(Bitmap)+`label` |
| `call("getRiskAppData")` | — | `b() :103-127` | **风险应用清单**：全部已装应用（跳过 `flags&1`）× 本地灰产库 → JSON `{packageName, versionCode, type, signaturesMd5}` |

**`sync_scan` 的完整逻辑**〔观测，`f() :200-327`〕
1. `content://` 开头 → `openInputStream` 复制到 **`<cacheDir>/copy.apk`**，扫后删除（**confused deputy 面**）。
2. `file://` → 取 `Uri.getPath()`；普通路径 → 直接用。
3. `g.z().L(new String[]{path}, cb, deep, **2**, engineName)`。
4. `CountDownLatch.await(**2, MINUTES**)` —— 超时返回 `null`（**任务不取消**）。
5. 命中则按 `y.f` 组装 7 列（`Type<-f11667l`, `VirusName<-f11659d`, `Description<-f11660e`, `EngineName<-f11661f`, `AdType<-f11668m`, `AdName<-f11663h`, `AdDescription<-f11664i`）。
6. **权限旁路**：无 `READ_EXTERNAL_STORAGE` 且路径不在 `/data` 下 → `w0.e.a(ctx)` → `ContentResolver.call(Uri.parse("content://com.lbe.security.miui.su"), "GrantGuardProvider", null, null)`，请求 LBE 安全组件授予存储权限（`d0.a.a()` 为真即 XOpt 模式则跳过）。

### 3.2 Service `SecurityService`（26 个 Binder 事务）〔观测〕

`onBind` 按 action 分发两个 Binder：

**`IAntiVirusServer`**（descriptor `com.miui.guardprovider.aidl.IAntiVirusServer`，24 个事务）

| code | 方法 | 语义 | 用户锁定行为 |
|---|---|---|---|
| 1 | `w(String[], IVirusObserver, boolean)` | startScan（scanType=0） | 回 `v(-1,null)` |
| 2 | `q(String, boolean)` | setEngineEnable | 忽略 |
| 3 | `g(String)` | getEngineEnable | `false` |
| 4 | `P(String)` | isSupportCloudScan | **不判锁** |
| 5 | `N(int)` | cancelScan | 忽略 |
| 6 | `C(IVirusObserver)` | checkUpdate → **恒 -1**（桩） | `-1` |
| 7 | `h(IVirusObserver)` | updateScan（病毒库更新） | 回 `x({engine:"",type:2})` + `K(-1)` |
| 8 | `k(int)` | cancelUpdate | 忽略 |
| 9 | `getVersion()` | **恒 `"1"`** | `"1"` |
| 10 | `J(String)` | 引擎显示名 | **不判锁** |
| 11 | `i()` | 引擎名数组 → **末位恒 null（bug）** | **不判锁** |
| 12 | `B(String, IWifiDetectObserver)` | wifiDetectNetworkState | `0` |
| 13 | `D(...)` | wifiDetectDnsAndPhishing | `0` |
| 14 | `O(String, observer, String ip)` | wifiDetectArp | `0` |
| 15 | `Q(...)` | wifiDetectSecurity | `0` |
| 16 | `j()` | detectAppsScan 结果（`basisOf==2` 的包名） | `new String[0]` |
| 17 | `G(String)` | 单包判定（命中缓存名单则回原名） | `null` |
| 18 | `A()` | detectAppsScan 上传（`/detect/app`） | `-1` |
| 19 | `e()` | 病毒缓存是否非空 | **不判锁** |
| 20 | `u(IVirusObserver, boolean)` | **增量扫描** | **不判锁（唯一无锁的写操作）** |
| 21 | `f(List)` | 按包名过滤白名单 | **不判锁** |
| 22 | `E(String, int)` | 包变更通知：2/3 → 清 `scan_result_db`+`risk_gray_result_db`；1 → 增量更新 | **不判锁** |
| 23 | `M(List)` | 按 apk 路径过滤白名单 | **不判锁** |
| 24 | `p(String[], IVirusObserver, boolean, int)` | startScanAsScene | 回 `v(-1,null)` |

**`IURLScanServer`**（2 个事务）

| code | 方法 | 语义 |
|---|---|---|
| 1 | `H(engineName, url)` | → `n0.j.b()` → `f0.a.m(1, url)` → **仅 `AVLEngine.scanURL`**（`AVLEngine.java:186`）；未锁定也执行 |
| 2 | `t(engineName)` | → `m(2, null)` → `AVLEngine.updateURLRule()` + 落 `avl_url_sdk_version`；锁定返回 `-2` |

**`IVirusObserver`**（13 个回调）：`d`=onScanStarted、`c(i,String)`=onScanStartItem、`s(i,total,VirusInfo[])`=onScanProgress、`v(i,VirusInfo[])`=onScanFinished、`x(EngineUpdateInfo)`=onUpdateItemFinished、`K`=onUpdateFinished、`y`=onUpdateCanceled、`a`=onCheckStarted、`m(VirusInfo[])`=onCacheScanFinished；`b/l/r` 未被使用。
**`IWifiDetectObserver`**：仅 `onResult(int)`。

### 3.3 结果上报通道〔观测，`n0/h.java:10-34`〕

命中（`virusType ∉ {1,4,5}`）时向 `com.miui.securitycenter` 定向发广播：

```
action = "miui.intent.action.TRACK_ANTIVURS_DATA"     // 官方拼写错误 ANTIVURS
setPackage("com.miui.securitycenter")
15 个 extra: path, packageName, virusName, virusDescription, engineName, versionName,
             adName, adDescription, versionCode, virusType, virusLevel,
             adLevel, isAuthoritative, isInCache, extras
```
〔推断〕安全中心 UI 的"最近风险事件"数据源。**未设 `receiverPermission`**（P5）。

### 3.4 功能域 → 代码包 的全景索引（14 个域，附件 `15_obf.md` §2.1 详表）

| # | 功能域 | 承载包 | 代表性证据 |
|---|---|---|---|
| B1 | 扫描调度 / 任务队列 / 并发 | `n0`(门面) `l0`(Scanner 接口) `m0` `p0` | `n0/g:1260` `L()`、`:1272` 上限 10、`:1132` `K()` 扇出、`:1296` `M()` type==1→增量；`p0/b0:202` 装配三扫描器 |
| B2 | 特征码 / 规则引擎 | `t0` `p4` `v5` `a6` `x5` `y5` `z5` | `t0` 被 `p0` import **133 次**（全树最高）；`v5/a`=JEXL 解释器、`a6`=JEXL 语法、`z5`=`org.apache.commons.jexl3 {JexlBuilder}` |
| B3 | 云端扫描客户端（flash scan） | `q0` `s0` `p0` `u0` | `q0/d` `@f6.f("/virus/scan")`；`p0/f:264-295` 组包（androidVersion/miuiVersion/timestamp/uuid/versionType/scanType）；`s0/s:124` base=`q0.m.f7703a` |
| B4 | 云端上报 | `q0` `p0.b0` `s0` | `q0/n` `@f6.f("/virus/localResult/report")`；`p0/b0:235-277` 组装 `ResultReportData`（含 `VERSION.INCREMENTAL/SDK_INT/DEVICE`） |
| B5 | URL 检测 / 钓鱼 | `n0.j` `com.avl.engine.urldetector` `libavlurl` | `n0/j:39-72` 单例；`AvlEngine:195-205` `m(1)→scanURL`、`m(2)→updateURLRule`；`UrlDetector:80-87` `loadLibrary("urldetectorsys")` |
| B6 | WiFi 安全检测 | `n0.k` `f5` `t` | `n0/k:99-115` 四方法 → `f0.a.d.{ARP,DNSANDPHISHING,NETWORKSTATE,SECURITY}`；`TencentEngine:497` `n()→i(...)`；`f5/a` import `WifiDetectManager` |
| B7 | 病毒库 / 引擎更新 | `q0` `s0` `i0` `r0` `w0` | `q0/m:12` 双域名；`i0/d:203` `antivirus/viruses`、`:55` `whitelist`；`AvlEngine:176-193`；`TencentEngine` `UpdateManager.check(UPDATE_FLAG_VIRUS_BASE,…)` |
| B8 | 安装拦截 / 静默安装 | `com.avl`（跨域）`p0` `n0.g` | `guard/install_scan`→`GuardProvider:415`；**`AVLA.java:83` native `installPackage(...)`**，由 `com/avl/engine/l/b/c.java:27,80` 调用 |
| B9 | 结果聚合 / 去重 / 缓存 / 白名单 | `c0` `r0` `g0` `i0` | `c0/c:8` `scan_result.db`、`c0/a:12` `risk_gray_result.db`、`w0/h:19` `miui_virus.db`；`n0/g:1014-1087` `D()` 按 level 合并 |
| B10 | 反诈 / 灰产判定 | `g0` `j0` `c0` `r0` | `g0/e:20-34` 造 `virusLevel=6` + `extras="AntiDefraud:danger;"` + `engine="Mi"`；`j0/a:37-58` 解析云端 `basisOf==2` 落 `mi_detect_app_db` |
| B11 | 埋点 / 统计 | `z` `u` `v` | `z` 持 OneTrack appid `2882303761517522561`；`u/a` import `com.miui.analytics.ITrack`；`v/a:30-34` OAID/UDID 反射 |
| B12 | 加密 / 编码 | `v0` `e5` `y0` | `v0` 11 文件、`javax.crypto` 9 处、字面量 `AES/CBC/PKCS5Padding` / `AES/ECB/PKCS5Padding` / `manifestMFSha256` |
| B13 | 配置 / 云控 / 系统属性 | `c5` `d5` `t` `p1` | `c5/a` 反射 `isMiuiLiteRom`；`d5/a` import `miuix.core.util.SystemProperties` + `ro.product.mod_device` |
| B14 | DB / DAO（Room） | `r0` `s0.d` `h i j e g` | `r0` 44 文件；`s0/d:140-199` 直接取 DAO；`h/i/j/e/g` = Room runtime |

---

## 4. 引擎体系

### 4.1 三引擎注册表〔观测，`f0/b.java:14-18`〕

```java
MI      ("Mi",       100, MiEngine/MiEngineNG, "sp_key_mi_engine_enable",      true, <nameRes>, <arrayRes>, 3)
AVL     ("AVL",       11, AvlEngine,           "sp_key_avl_engine_enable",     true, <nameRes>, <arrayRes>, 2)
TENCENT ("TENCENT",   10, TencentEngine,       "sp_key_tencent_engine_enable", true, <nameRes>, <arrayRes>, 2)
```
- `MI` 的实现类**在枚举构造时由云控决定**〔观测，`f0/b.java:56-60`〕：读 `MiuiSettings$SettingsCloudData` 的 `VirusScan` / `miEngineNgEnable`（默认 `"true"`）→ `MiEngineNG`，否则 `MiEngine`。**同一个 `MI` 常量可指向两个不同实现**。
- **默认启用策略**〔观测，t-core〕：`Mi` **恒开且不可关**（`J()` 拒绝写 Mi）；若 SecurityCenter 声明 `securitycenter.support.mix.scan` 则三引擎全开，否则 AVL/TENCENT 随机取 1（默认 2 引擎）。

### 4.2 引擎能力矩阵〔观测，`f0/a` 抽象契约 + 4 个实现〕

| 能力 | 抽象方法 | MiEngine | MiEngineNG | AvlEngine | TencentEngine |
|---|---|---|---|---|---|
| 初始化 | `e(InterfaceC0073a)` / `f()` | ✓ | ✓ | ✓ | ✓ |
| 扫描 | `j(List, c, boolean, String)` | ✓ | ✓ | ✓ | ✓ |
| 取消扫描 | `k()` | ✓ | ✓ | ✓ | ✓ |
| 更新 | `l(b)` | ✓ | ✓ | ✓ | ✓（TMSDK UpdateManager） |
| **URL 扫描/规则更新** | `m(int, String)` | ✗（基类 -1） | ✗ | **✓ 唯一实现** | ✗ |
| **WiFi 检测** | `n(IWifiDetectObserver, d, Object)` | ✗（基类 0） | ✗ | ✗ | **✓ 唯一实现** |
| 联网位设置 | — | `o(boolean)` | `w(boolean)` | `s(boolean)` | `v(boolean)` |
| 支持云扫 | `g()` | ✓ | ✓ | ✓ | ✓ |
| 初始化异步回调 | — | ✓ | ✓ | ✓ | ✓ |

→ **URL 检测只有安天能做；WiFi 检测只有腾讯能做**。这是本软件功能不完整的一半原因（另一半是 QAX 未实现）。

### 4.3 引擎元数据 / 显示名〔观测，resources〕

| 键 | en | zh-rCN |
|---|---|---|
| `AVL` / `DesAVL` | AVL | **安天** / 安天AVL |
| `Tencent` / `DesTencent` | Tencent | **腾讯** |
| `Mi` / `DesMi` | Xiaomi | **小米** |
| `QAX` / `DesQAX` | — | **奇安信** |

8 个数组项依次映射：`NameString, DescriptionName, DialogTitle, DialogHeader, DialogPolicy, DialogCloudScan, DialogSummary(=dbupdate), DialogUrl`。

### 4.4 第四引擎「奇安信 QAX」= **未实现**〔观测〕

| 证据 | 结论 |
|---|---|
| `res/values/arrays.xml:23-32` 有完整 `qax_string_info`；`public.xml` 有 `QAX=0x7f0b0006` 等 | 资源存在 |
| `res/values/strings.xml` 6 个 `qax_dialog_*` = 字面量 **`TODO`**；zh-rCN 版是**从腾讯文案复制**（`qax_dialog_header`="本功能使用了腾讯提供的杀毒引擎"、`qax_dialog_url` 指向 `p=tencent`） | 占位 |
| `f0.b` 枚举**只有 3 个常量**（REA `ev_80d5f9…`） | 无 QAX 常量 |
| 全树 grep `QAX\|qax` 只命中 `com/tencent/tmsecure/R.java`（TMSDK 自带 R 别名） | 无引擎类、无调用点 |

### 4.5 引擎隐私文案 vs 实际行为〔观测 + 对照〕

| 文案键 | en | zh-rCN | 代码实际 |
|---|---|---|---|
| `avl_dialog_header` | "This feature encrypts and uploads **your device's ID** to AVL's servers (%s)." | 本功能使用了安天AVL提供的杀毒引擎，详情可见%s | Java 层只 `setRegion`/`setNetworkEnabled`/`setCloudScanEnabled`/`init(deviceType)`；native 有 `AVLM_LogUpLoad` + `AVLM_GetSIDInfo` |
| `tencent_dialog_header` | "…uploads **your device's ID** to Tencent's servers (%s)." | 同上（未提上传） | `TMSDKContext.setDeviceInfo(android_id, Build.MODEL)`（`TencentEngine.java:402`） |
| `tencent_dialog_cloudscan` | "Upload and scan app names and details…" | 我们会上传应用包名和应用的特征至云病毒引擎进行扫描 | `CloudScanApkInfo(包名, 版本号, 证书MD5, 证书SHA256, MANIFEST.MF SHA256)` —— **只传指纹，不传 APK** |
| `tencent_dialog_dbupdate` | "Update virus definitions automatically" | 我们会上传**加密后的IMEI**至服务器用于更新病毒库 | **无 IMEI 上传实现**；真正读 IMEI/IMSI 的是 OneTrack（`Configuration.Builder` 默认 true，`z/b` 未关闭） |

**〔推断〕两点合规级错位**：
1. **国内外披露不对称**：en 版明说"上传设备 ID 到安天/腾讯服务器"，zh 版弱化为"使用了某引擎"。
2. **腾讯文案与实际不符**：中文文案承诺"I 上传加密 IMEI"，实际代码传的是 `android_id + Build.MODEL`；而真正读 IMEI/IMSI 的是同 APK 内的 OneTrack 埋点 SDK。

---

## 5. 扫描管线（端到端）

### 5.1 组件〔观测，`p0/*`〕

| 类 | 体量 | 职责 |
|---|---|---|
| `p0/b0` RealScanner | 13 KB | **编排器**：CodeScanner + CloudScanner + MatcherScanner 串联 + 结果上报 |
| `p0/g` CodeScanner | 32 KB | **代码级特征扫描**（规则匹配，逐文件） |
| `p0/f` CloudScanner | 15 KB | 云端指纹校验（`POST /virus/scan`） |
| `p0/z` MatcherScanner | 21 KB | 本地黑/白/灰名单匹配（对应 Room 8 表） |
| `p0/y` 规则编译 | 5 KB | JEXL3 表达式求值 + `RustSupport.matches/search` |
| `p0/j` | — | 装配点 |

### 5.2 特征码引擎 = **JEXL3 布尔表达式 + Rust Aho-Corasick**〔观测，已独立复核〕

- **规则形态**：`characteristics` 表每行有 `name / strings / condition / threatName / version`；`condition` 是**布尔表达式**，变量名 = `StringItem.key`。`Condition.reduce(Map<String,Boolean>)` 求值。
- **表达式引擎 = Apache Commons JEXL3**（不是自研）——证据：
  - `z5/b` 是 `JexlBuilder`（字段 `z5.h`=JexlEngine、`z5.h.c`=JexlInfo）
  - `v5/b` 是 `JexlBuilder` 包装，`v5/e` 是 `JexlException`（`"JEXL error : "`，且 `org.apache.commons.jexl3.internal/parser` 前缀判断）
  - `a6/c3` 是 JavaCC 生成的 **JexlParser 语法常量表**（`JexlScript/Annotation/IfStatement/TernaryNode/…` 全套 100+ 节点名）
  - `a6/u2`、`x5/k` 处理 `jexl.import` / `jexl.options` / `jexl.namespace.*`
  - `z5` 内含 `org.apache.commons.jexl3 { JexlBuilder {} }`
- **单文件判定**（`p0/y.e(file, StringItem)`）〔观测，`p0/y.java:101-124`〕：
  - `Files.size(path) > 20971520`（20 MB）→ 跳过
  - `t0.j.b(item)` 真 → `RustSupport.matches(bytes, pattern)`（**二进制特征 + `?` 通配**）
  - 否则 → `RustSupport.search(bytes, value)`（**文本/正则**）
  - `-1` 视为不匹配；`ClosedFileSystemException` → `CancellationException("External cancellation")`
- **Rust 引擎 = Aho-Corasick**〔观测，`libflamingo.so` 内嵌 crate 信息 `aho-corasick-1.0.1`、`rustc-demangle-0.1.21`〕
- **扫描目标模式**〔观测，`ScanTarget`〕：`/AndroidManifest.xml`、`/*.dex`、`/META-INF/MANIFEST.MF`、`/lib/**/*.so`，另有 `SpecifiedFile` 兜底。

### 5.3 扫描门面 `n0/g`（`g.z()`）〔观测，56 KB / 1450 行〕

静态字段：`f6412h`=活跃任务数（上限 10）、`f6413i`=taskId→任务、`f6414j`=单例；
实例 6 个 `ConcurrentHashMap`（observer→已完成引擎数/任务/结果/签名缓存/灰产标记/scanType）。

| 公开方法 | 语义 |
|---|---|
| `z()` | 单例；首次构造执行 `x.a.l()`（引擎启用初始化 + 可能云控拉取） |
| `L(paths, observer, useCloud, scanType, extras)` | **扫描主入口**：过滤 → 限流(≥10 拒绝) → 建 6 表 → 起 `n0.g$f`；返回任务数 |
| `M(paths, observer, useCloud, scanType, extras)` | **场景分发**：`scanType==1` → `C()` 增量；否则 → `L()` |
| `C(observer, useCloud)` | **增量扫描**（`n0.g$e` AsyncTask：已装包 + `MediaStore` APK 全表 → 缓存命中且 level∈{2,3,6} 直接回放） |
| `N(observer)` | **病毒库更新任务**（≥10 拒绝） |
| `O(list)` / `P(list)` | 按**包名** / 按 **apk 路径**过滤白名单 |
| `q(id)` / `r(id)` | cancelScan / cancelUpdate |
| `s()` | checkUpdate —— **恒 `return -1`（桩）** |
| `J(name, enable)` / `w(name)` | 写/读引擎启用（**Mi 永不被写**） |
| `F(name)` | 是否支持云扫 → 恒返回枚举里的 `f3551f`（三引擎都 true，**与引擎无关的恒真值**） |
| `x()` | 引擎名数组 → **`new String[4]` 只填 3 个，末位恒 null（bug）** |
| `y(name)` | 引擎显示名 |
| `D(a, b)`（private） | **多引擎结果合并**：按 `virusLevel` 优先级 **2 > 3 > 6 > 4 > 5 > 1** 覆盖，`extras` 字符串拼接 |
| `K(...)`（private） | 起引擎线程（每引擎一个 `n0.g$b` Thread）+ 埋点 `security_scan` |
| `v(ctx)`（private） | `MediaStore.Files` 查 `%.apk` → 全盘 APK 列表 |

**并发模型**〔观测〕：**引擎级并行、文件级串行**（每文件 `sleep(5)` 毫秒），任务上限 10。

### 5.4 扫描类型码语义（**修正了常见误读**）〔观测〕

- `GuardProvider.sync_scan` 传 `scanType = 2`；`SecurityService.w()` 传 `0`；`p()` 传调用方给的值。
- `M()` 只在 `==1` 时走增量；**其余值原样传给 `L()`，`L()` 里它只是"进度偏移量"**（`observer.s(i8 + offset, …)`）。
- → **`2` 不是"同步扫描模式"**，只是进度基准偏移。`install_scan` 的 `engineName`（第 4 参）也只是作为 `extras` 一路传到 `f0.a.j()` 第 4 参，**四个引擎实现全部忽略它** → **"按引擎名扫描"的语义不存在**，引擎集合恒由 `x.a.c()`（SP 启用项）决定。

### 5.5 结果模型 `y.f`（VirusInfo）〔观测，15 字段〕

| 字段 | 类型 | 语义 | 备注 |
|---|---|---|---|
| `f11657b` | String | `path`（content:// 时为 `cacheDir/copy.apk`） | |
| `f11658c` | String | `packageName` | |
| `f11659d` | String | `virusName` | AVL `getVirusName()` / TENCENT `entity.virusName` / NG `threatName` |
| `f11660e` | String | `virusDescription` | |
| `f11661f` | String | `engineName`（`"Mi"`/`"AVL"`/`"TENCENT"`/NG 硬编码 `"MiEngineNG"`） | |
| `f11662g` | String | `versionName` | |
| `f11663h` | String | `adName` | **全树 0 赋值（未实现）** |
| `f11664i` | String | `adDescription` | **全树 0 赋值** |
| `f11665j` | int | `versionCode` | **不序列化** |
| `f11666k` | int | `virusType` | **实际只有 0/1** |
| `f11667l` | int | **`virusLevel`**：1=安全 2=病毒 3=风险 4=? 5=灰产 6=灰产/反诈 | 4/5 不上报广播 |
| `f11668m` | int | `adLevel` | **0 赋值**；写了不读 |
| `f11669n` | boolean | `isAuthoritative` | **不序列化** |
| `f11670o` | boolean | `isInCache` | **0 赋值、不序列化** |
| `f11671p` | String | `extras`，格式 `"<引擎>:<safe\|danger>;"` | 多引擎合并时**字符串拼接** |

**`extras` 实测取值**（可作"哪个引擎判定"的指纹）：`Mi:safe;` / `Mi:danger;` / `AVL:safe;` / `AVL:danger;` / `TENCENT:safe;` / `TENCENT:danger;` / `MiEngineNG:safe;` / `MiEngineNG:danger;` / `AntiDefraud:danger;`

**Parcel 不对称缺陷**〔观测〕：`writeToParcel` 写 **12** 个值，`f(Parcel)` 读 **10** 个 → 跨进程后 `adLevel` 丢失；`versionCode` / `isAuthoritative` / `isInCache` 完全不参与序列化。

**威胁分类的真相**〔观测〕：`res/values/strings.xml` 有 12 个 `threat_type_*_desc`（adware/backdoor/default/exploit/hacktool/monitoring/rootkit/spyware/trackware/trojan/virus/worm），但**业务代码 0 处引用**（只有 TMSDK 自带的 `R.java` 常量）。`virusType` 实际是**二值**：`1`=有检出（AVL/腾讯），`0`=未赋值。真实威胁分类靠 `virusName` 字符串 + `virusLevel`，由上层（安全中心）自行映射。

---

## 6. 数据层：5 个库 / 15 张表

### 6.1 加密 Room 库（v2，8 表）〔观测，`EngineDatabase_Impl`〕

| # | 表 | 列 | 唯一索引 |
|---|---|---|---|
| 1 | `trust_certs` | `id` PK AI, `developer` BLOB, `fingerprintSha256` BLOB | `index_trust_certs_fingerprintSha256` |
| 2 | `trust_installers` | `id`, `packageName` BLOB, `vendor` BLOB | `pk_index(packageName)` |
| 3 | `innocent_package` | `id`, `packageName`, `signaturesSha256`(NULL), `signaturesMd5` | `index_innocent_package_packageName_signaturesMd5` |
| 4 | `characteristics` | `id`, `name`, `strings`, `condition`, `threatName`, `version` | `index_characteristics_name` |
| 5 | `guilty_package` | `id`, `packageName`, `signaturesMd5`, `signaturesSha256`(NULL), `versionCode`(NULL), `threatName`, `mfSha256`(NULL) | `index_guilty_package_packageName_signaturesMd5_mfSha256` |
| 6 | `black_gray_chs` | `id`, `name`, `condition`, `strings`, `version` | `index_black_gray_chs_name` |
| 7 | `black_gray_app` | `id`, `packageName`, `signaturesMd5`, `signaturesSha256`, `versionCode`(NULL), `mfSha256` | `index_black_gray_app_packageName_signaturesMd5_mfSha256` |
| 8 | `version` | 7 个整型版本号 + `id` PK（**无 autoincrement**） | — |

+ `room_master_table`（identity `8856aeeb52eb3fc27eb4570c1cd903b7`）
**迁移**〔观测〕：迁移数组为空 → **破坏性迁移**（版本升级即清库重建）。

### 6.2 列级 AES 加密（**本报告已独立复算验证**）〔观测〕

| 项 | 值 |
|---|---|
| 算法 | `AES/CBC/PKCS5Padding`（`v0/j.java:59,79`） |
| 密钥来源 | classpath `META-INF/flamingo.dat`，`skip(144)` 读 16 字节（`v0/j.java:38-41`） |
| 密钥变换 | 前 8 / 后 8 字节各作**大端 long**，分别 `^ 4831221107097930563`（=`0x430BF1FFFFF10B43`） |
| **还原密钥** | **ASCII `wcw1xbplx7aijwqr`** |
| IV | `SecureRandom` 随机，**前置**：输出 = `IV(16) ‖ ciphertext` |
| `EncryptVersion` | 恒 `"0"` |

**Lead 独立验算**〔观测〕：直接读 APK 内 `META-INF/flamingo.dat` 偏移 144 的 16 字节
`34 68 86 ce 87 93 7b 2f | 3b 3c 90 96 95 86 7a 31`
→ LE int64 `0x2f7b9387ce866834 ^ 0x430BF1FFFFF10B43 = 0x6c70627831776377` → `wcw1xbpl`
→ LE int64 `0x317a869596903c3b ^ 0x430BF1FFFFF10B43 = 0x7271776a69613778` → `x7aijwqr`
→ 拼接 **`wcw1xbplx7aijwqr`**（16 字节 ASCII）✅

**由此导出的设计缺陷**〔推断〕：IV 每次随机 ⇒ 同一明文每次密文不同 ⇒
1. 三张表上的 `UNIQUE` 索引**无法去重**（同一 APK 每轮同步都插新行）；
2. `WHERE packageName = ? AND signaturesMd5 = ?` 这类等值查询**几乎不可能命中**；
3. 实际匹配路径只能是 `r0/a.b()` **全表加载 → 内存解密 → 内存比对**（代码与此一致）。
→ 库会**无限增长**，且索引形同虚设（性能缺陷，非安全缺陷）。

### 6.3 四个明文 SQLite〔观测，t-core〕

| 库文件 | 版本 | 表 | 写入方 | 读取方 |
|---|---|---|---|---|
| `miui_antivirus.db` | 2 | `antivirus_db_version(version, release_time)`；`antivirus_db(package, version, signatures, md5, type, virus_name, description, strategy, descType)` | `i0/d`（`/antivirus/viruses` 下发） | `i0/l:308`（`package=?` 逐条） |
| `mi_engine_white_virus.db` | 1 | `mi_virus_white_db(package, signatures, path)`；`mi_detect_app_db(package, signatures)` | `i0/f:29-72`（`/antivirus/whitelist`） | `i0/f:77-118` |
| `risk_gray_result.db` | 1 | `risk_gray_result_db(packageName, versionCode, signatures, type, time)` | `c0/b:113-152`、`MiEngineNG:120-127`（写 type=5） | `c0/b:97-121` |
| `scan_result.db` | 2 | `scan_result_db(path, packageName, virusName, virusDescription, engineName, versionName, versionCode, virusLevel, extras, sign, time)` | `c0/d:205-288` | `c0/d:179-203` |
| **`miui_virus.db`** | **5** | `t_virus_white_list(_id, scan_result_type, scan_item_type, app_label, desc, dir_path, pkg_name, virus_name, virus_md5)`；`virus_white_db(_id, package, path, version_code, size, engine, local_version, extra)` | `GuardProvider.insert/delete` | `GuardProvider.query`、`i0/f:88-111` |

`miui_virus.db` 升级路径〔观测，`w0/h.java`〕：v1→2 加 `version_code/size` → 3 加 `engine/local_version` → 4 建 `t_virus_white_list` → 5 加 `extra`。

### 6.4 白名单判定的两条维度〔观测〕

```
e(PackageInfo) → 取签名 → c(sigs) 拼接 MD5（大写hex，多签名用 "|" 连接）
  → d(pkg, sigMd5):
      b(pkg, sigMd5)   // i0/f:37-39 是 `return false` 的死 hook（内置白名单被裁）
      SELECT ... FROM mi_virus_white_db WHERE package=? AND signatures=?
f(path) → w0.a.a(path)（全文件 MD5）→ query("content://guard/virus", null, "virus_md5 = ?")   // 回调自己的 Provider
```

---

## 7. 云端面

### 7.1 两套网络栈〔观测〕

| 栈 | 技术 | 基址 | 端点 |
|---|---|---|---|
| **新** | Retrofit + OkHttp + Gson | `https://virus-engine.sec.miui.com`（国际 `…sec.intl.miui.com`，`q0/m.java:12`） | GET `/virus/{blocklist,allowlist,feature/codeList}`、`/blackGrays/{blocklist,feature/codeList}`；POST `/virus/scan`、`/virus/localResult/report` |
| **旧** | `HttpURLConnection` | `api.sec.miui.com` / `flash.sec.miui.com` | POST `/antivirus/viruses`、`/antivirus/whitelist`、`/detect/app` |

4 个 OkHttp Interceptor：**签名 / 请求加密 / 响应解密 / 网络总闸**。

### 7.2 签名与加密〔观测〕

| 项 | 值 |
|---|---|
| 签名算法 | **MD5(排序后的 `k=v` 拼接 + 静态盐)** —— **无 HMAC、无 nonce、无时间戳防重放** |
| 旧栈盐（病毒库/白名单） | `94e6cec2-19dc-4bb6-823c-613f72d3ea16`（`i0/d.java:55,203`） |
| 旧栈盐（闪电检测） | `6988567a-4220-4b51-bc2d-ccdec27a74a1`（`j0/c.java:75`） |
| 新栈盐 | `62b36629-ecb4-4725-9f33-ccb0fd158191`（`q0/q.java:22`） |
| 体加密 | AES + gzip，头 `EncryptVersion=0` |
| TLS | 系统默认校验，**无 pinning、无自定义 TrustManager、无关闭校验** |
| 设备标识 | 旧栈 `mi` = `MD5(android_id + "-" + appKey)`、`oa` = OAID；新栈 `uuid` 外部注入 |

〔推断〕签名可**离线伪造**（盐为静态常量，无防重放），但服务端若校验来源 IP/TLS 客户端证书则另当别论——本轮未见客户端证书。

### 7.3 云端扫描：**只传指纹，不传 APK**〔观测，`p0/f.k()`〕

```
CloudScanApkInfo(
  packageName         = aVar.getPackageName(),
  versionCode         = String.valueOf(aVar.d()),
  apkCertificateMD5   = aVar.f(),
  apkCertificateSHA256= aVar.b(),
  mfFileSHA256        = aVar.g() ?: ""
)
```
**全树无 `RequestBody.create(File)`、无 `MultipartBody`** ⇒ 不存在上传 APK/dex/so 的代码路径。
→ 云端判定只是**"包名+版本+证书+MF 指纹"四元组的名单匹配**，不是代码级云查杀。

**触发条件是位域，不是"本地无结论"**〔观测，`p0/f.java:348-379`〕：

| bit | 掩码 | 含义 |
|---|---|---|
| 0,1 | `& 3` | 威胁扫描模式 |
| 2 | `& 4` | 黑灰产扫描模式 |
| 3 | `& 8` | **禁用云端扫描** |
| 其余 | — | 原值整体作为 `scanType` 上报 |

默认 `f7380c = 3`（威胁模式、云扫允许）。Manifest 的 `guardprovider.scan_properties=2` **全树 0 处读取**（死配置）⇒ **一旦允许云扫，所有待扫项整批送 `/virus/scan`，无"仅上传可疑项"过滤**。

**响应消费**〔观测〕：`resultType ∈ {Clean, Threats, BlackGrays, Unknown}`；Threats → `t0.d.c()` 感染；BlackGrays → `t0.d.b()` 灰产；结论**不写入 Room**，只改内存中 `t0.a` 状态，`authority="cloud"`、`version=-1`。**索引对齐断言写成 `payload.packageName != apkInfo.packageName` 即报错**（疑似把 `!=` 写成 `equals` 取反，属可疑实现）。

### 7.4 `flash.sec.miui.com/detect/app` = 反诈清单上报（**非逐样本云扫**）〔观测〕

`j0/c.java:75`：POST JSON `{timestamp, os, biz_id:"virus_scan", uuid, content[], sign}` → 响应 `{data:[{pkg, signature, version, basisOf}]}`。
〔推断〕**上报全量非系统应用清单 ←→ 下发"不予信任名单"**，`basisOf==2` 的条目构成 `SecurityService.j()` 的返回集。这是**反诈/灰产画像**通道，与 §7.3 的逐样本校验是两条不同链路。

### 7.5 端点总表（28 条：22 主机名 + 6 裸 IP:端口）〔观测〕

| 域名 | 归属 | 触发 | 明文? |
|---|---|---|---|
| `api.sec.miui.com` | GuardProvider | 病毒库/白名单同步 | HTTPS |
| `flash.sec.miui.com` | GuardProvider | 反诈清单上报 | HTTPS |
| `virus-engine.sec.miui.com` / `….intl.miui.com` | GuardProvider | 规则库同步 + 云扫 + 结果上报 | HTTPS |
| `staging.api.sec.miui.com` | — | **仅出现在 network_security_config 明文白名单**（测试残留） | **HTTP 允许** |
| `api.sec.miui.com/policy/proxy?p=antiy\|tencent&l=…` | GuardProvider | 引擎隐私政策页 | HTTPS |
| `tracking{.intl,.india,.rus}.miui.com` | OneTrack | 事件上报 | HTTPS |
| `sdkconfig.ad.{,intl.,india.,rus.}xiaomi.com`、`api.ad.xiaomi.com` | OneTrack | 拉配置 | HTTPS |
| `tmfsdk.m.qq.com` | TMSDK | 云查/WiFi/统计 | HTTPS |
| `tmfsdk4.m.qq.com` | TMSDK | IPv4 上报 | HTTPS |
| `tmfsdktcp.m.qq.com:8001` / `tmfsdktcpv4.m.qq.com:8002` | TMSDK | TCP 长连接 | 私有协议 |
| `120.241.190.227:8002`、`61.241.44.59:8002`、`14.18.175.81:8002` + 3 个 IPv6 | TMSDK | **IP 直连兜底**（绕 DNS） | 私有协议 |
| `mmgr.gtimg.com/qqsecure_config_update/qqsecure_config_update` | TMSDK | 特征库更新清单 | HTTPS |
| `tools.3g.qq.com`（`/wifi/cw.html`、`/j/sslstrip`） | TMSDK WifiDetect（代号 **Meri**） | 网络认证/劫持/SSLStrip 探测 | **HTTP** |
| `m.qq.com`、`www.qq.com:80`、`miwifi.com/diagnosis/index.html` | TMSDK | SSLStrip 比对 / TCP 连通性 | 裸 TCP |
| **（无）** | **AVL** | 云查/日志/URL 规则更新 —— **3 个 .so 与 Java 均无业务域名**，全 config 驱动 | — |

〔观测〕`res/xml/network_security_config.xml`：仅 `staging.api.sec.miui.com` 与 `tools.3g.qq.com` 允许明文，其余走系统默认 TLS。

---

## 8. Native 层

### 8.1 12 个 `.so` 身份表〔观测，REA + 字节级〕

| 文件 | SONAME | JNI 形态 | 定性 |
|---|---|---|---|
| `libams-1.3.0-mfr.so` | 同名 | **8 个 `Java_tmsdk_…_AmScannerV2_*` 直接导出** | TMSDK AMF 特征扫描器（配 `70003.amf`） |
| `libavlasys.so` | 同名 | 无 `Java_*`；`JNI_OnLoad` + RegisterNatives | AVL SDK 的 JNI 壳（注册 `AVLA`/`NativeUtils`） |
| `libavlm.so` | 同名 | 无（纯 C，**98 个 `AVLM_*`**） | **安天引擎本体** |
| `libavlurl.so` | **`liburl.so`（≠文件名）** | 无（纯 C，33 个 `AVLM_Match_*`） | 安天 URL 信誉引擎 |
| `libdce-1.1.21-mfr.so` | 同名 | `JNI_OnLoad` + RegisterNatives | TMSDK DeepCleanEngine（含 `nativeDeleteAllChildren` 等删除原语） |
| `libflamingo.so` | **空** | **3 个 `Java_com_miui_…_RustSupport_*`** | **小米自研 Rust Aho-Corasick 匹配引擎** |
| `libtmf-gm.so` | 同名 | **8 个 `Java_com_tencent_tmf_gm_{Sm2,Sm4}_*`** | 国密 SM2/SM4 JNI |
| `libTmsdk-2.0.13-mfr.so` | **`libTmsdk-2.0.12-mfr.so`** | `JNI_OnLoad` + RegisterNatives（5 导出） | TMSDK 核心（TccCryptor / 差分包 / 黄页） |
| `libturingvd.so` | 同名 | `JNI_OnLoad` + RegisterNatives | 腾讯 Turing 设备风控（root/模拟器检测） |
| `liburldetectorsys.so` | 同名 | `JNI_OnLoad` + RegisterNatives | `UrlDetector` 的极薄 dlopen 转发壳 |
| `libxcrash.so` | 同名 | `JNI_OnLoad` + RegisterNatives | xcrash 崩溃捕获主库 |
| `libxcrash_dumper.so` | **空 + 有 `PT_INTERP`** | 无 | xcrash **独立 dumper 可执行体**（ptrace 抓 tombstone） |

**JNI 契约（Lead 独立扫描确认）**：
- `libflamingo.so` 仅 3 个导出，与 `RustSupport` 的 3 个 native 方法**逐一对应**，含重载签名 `search___3B_3Ljava_lang_String_2`（=`search(byte[], String[])`，**无调用点**）。
- `libams` 的 8 个 JNI 方法 = 腾讯 AMF 扫描器的全部入口（`newObject/deleteObject/initScanner/getOpcode/extractApkInfo/scanApkBytes/nativeLoadAmfHeaderBytes/nativeUpdateMalwareInfoBytes`）。
- **6 个动态注册库的 `RegisterNatives` 表在 `.rodata` 中为明文**，类名+签名已全部取出（t-native）：

| 库 | 注册的 Java 类 |
|---|---|
| `libavlasys` | **`AVLA`** + **`NativeUtils`**（用自建 AArch64 `ADRP+ADD` 解码器机械证明 `JNI_OnLoad` 只引用这两个类名） |
| `libTmsdk` | `TccCryptor` / `TelNumberLocator` / `NumMarker` / `YellowPages` |
| `libdce` | `QFile` / `QSdcardScanner` / `DeepCleanEngine` |
| `libturingvd` | `TNative$aa`（含 `$aa$bb.onServiceConnected`） |
| `liburldetectorsys` | `UrlDetector` |
| `libxcrash` | `NativeHandler`（含 31 参数的 `nativeInit`） |

合计：**5 个库承载 7 个类群，21 个直接 `Java_*` 导出 + 6 张 RegisterNatives 注册表（约 60 个方法）**。
**工具限制（重要）**：本机 REA 的 Ghidra 在 Windows 上**只接受 PE/6502**，拒绝 ELF-arm64（`target_format_unsupported`）；Hopper 需 macOS；IDA/Kuna 未配置 ⇒ **本轮无可用反编译路径**，native 结论全部来自 ELF 结构解析 + 明文符号/字符串 + AArch64 指令解码。

### 8.2 8 个 assets 鉴定〔观测〕

| asset | 大小 | 判定 |
|---|---|---|
| **`avlsdk`** | 1,566,355 | **ZIP，26 项 = 安天特征库整包**。`av/avl/android/*.avl`（`avlpk_amc` 54KB、`avlpk_apn` 132KB、`avlpk_des`、`avlpk_dhc`、`avlpk_kw`、`avlpk_pornware` 322KB、`avlpk_sfmf` 81KB、`avlpk_sgnl`、`6947e41c_60f53057.avl`）+ `url/url/*`（`fish.avl` 151KB、`fish_url.avl` 417KB、`white.avl` 370KB、`*_re.avl`）+ 配置 + 中英行为描述 `.des` |
| `avlsdk:sdk_conf/version.conf` | 106 | **明文 JSON**：`{"sdk.conf":"1","av.siglib":"20220928.ah","av.engine":"351","url.siglib":"20180917.zz","url.engine":"124"}` |
| `avlsdk:sdk_conf/sdk.conf` | 10,379 | **结构已精确解析**（t-native 复核，修正 Lead 初判）：**前 380 B 是纯 ASCII**（版本头 `00000` + hex 段 + 253 B base64），**密文从 `0x17C` 起共 9,999 B**。因 9,999 非 16 的倍数 ⇒ **AES/SM4 整块模式结构性排除**，与密钥无关（Lead 用 `wcw1xbplx7aijwqr` 试解失败是必然，不是密钥错）。〔未知：流密码/自定义/压缩？〕 |
| `avlsdk:av/kw/common/{en,zh}/*.des` | 477~1,212 | **明文 JSON 文案模板**。`behavior.des` 揭示 **14 类行为标签**：`prv`窃取隐私 / `rmt`接收远程指令 / `pay`恶意扣费 / `spr`自动扩散 / `exp`耗流量发短信 / `sys`破坏系统 / `fra`伪装欺诈 / `rog`占资源弹广告 / `ads`推广告 / `sms`频繁发短信 / `spy`间谍窃密 / `bkd`后门 / `rtt`私自提权 / `lck`锁屏勒索 |
| `avlsdk:*.avl` 魔数 | — | `41 56 4C` = **`"AVL"`** |
| **`70003.amf`** | 680,928 | **TMSDK 反病毒特征基库**：魔数 `"amf\0"` + `u32=4`；`0x18..0x1F` 两个 u32 时间戳 `0x5FF6D2BD` / `0x622575E6`；其后为 **42,556 条 × 16 B 的密文记录**（42556×16=680,896，与文件体量吻合）。与 `libams` 的 `nativeLoadAmfHeaderBytes` 配套，编号见 TMSDK `UpdateConfig`（`70002`=EN 库、`70003`=病毒库、`70701`=payload） |
| **`mdetector`** | 136,636 | **`\x7fELF` + `ET_DYN` + `EM_ARM`(40) + `PT_INTERP=/system/bin/linker`** ⇒ **ARM32 可执行 ELF（不是 .so）**。静态链接 libpcap（机证：yacc 符号 `pcap_lval/pcap_text/pcap_nerrs`）；字符串 `"arp packet size should be 42 bytes or more!"`、**`"!!This Device is being Hijacked!!"`**、`"send msg to java:"`（结果打到 stdout 由 Java 读） ⇒ **被动嗅探 ARP 判定网关劫持的检测器**。**加载方式已定位**：`Runtime.exec` 调起 —— `tmsdkobf/t9.smali:563`、`com/avl/engine/j/b/e.smali:217,383`（两处）。**注意：APK 只提供 arm64-v8a，此 32 位体在纯 64 位机型上无法 exec ⇒ 功能静默失效** |
| **`40706.dat`** | 9,368 | **TMSDK 证书 MD5 白名单**（编号 40706）。`fe 2a cf f7 …` 高熵；**8 字节对齐**（9368 = 1171×8）⇒ 分组/流式加密特征，使用 TMSDK 自有 `TccCryptor`（密钥内置于 `libTmsdk`） |
| `licence.conf` / `licence1.conf` | 392 / 368 | **算法已源码级还原**（t-native）：**`[128 B RSA/ECB/PKCS1Padding 公钥运算][DES/ECB/NoPadding]`** → 解出后是 `Properties`。长度整除双重确认（264/8、240/8 精确）。内置 RSA 证书 **`NotAfter 2012-02-24` 已过期**，且去填充失败 ⇒ **授权链在本包内不可能通过**，TMSDK 授权功能实际降级。附：`avlsdk:av/avl/conf/liscense.conf` 与 `url/…/liscense.conf`（各 404 B）同为加密 |
| `dexopt/baseline.prof{,m}` | 127 / 55 | `pro\0"010\0"` / `prm\0"002\0"` ART profile，正常 |

### 8.3 高危 native 能力〔观测 + 推断〕

| # | 发现 | 证据 | 判定 |
|---|---|---|---|
| N1 | `libavlm` / `libavlurl` / `libavlasys` **具备完整出网能力**（`socket/connect/send/recv/gethostbyname/inet_addr/select`），不经 Java 网络栈 | import 表 | 〔推断〕可绕过 `NetworkSecurityPolicy`/代理/StrictMode 直连任意 IP。但**未见硬编码 C2**（`libavlm` 只有 `www.baidu.com` + NTP pool + OpenSSL/w3.org 固有串；`libavlurl` 的 ~400 条域名是 **Public Suffix List + 免费二级域托管商清单**，属防御性名单，**不是回连地址**） |
| N2 | **`assets/mdetector` 是藏在 assets 里的 ARM32 可执行体** | ELF 头 + `PT_INTERP` + libpcap 符号 | **高危形态**：绕过 `lib/` 目录对 PackageManager/安全软件的可见性。功能是 ARP 劫持检测 |
| N3 | `libtmf-gm.so` import **`rand/srand/time`，无 `getrandom`/`/dev/urandom`** | import 表 | 〔未知〕若 SM2 的随机数 `k` 取自 `rand()`，则非签可预测（经典国密高危缺陷）。需反汇编确认。对照：`libflamingo` 正确使用 `getrandom` |
| N4 | `libturingvd.so` **真实 root 检测 + 模拟器检测 + `popen`** | 串 `/system/bin/su`、`/system/xbin/su`、`daemonsu`、进程名正则 `^/system/xbin/ku[.]sud$\|^daemonsu:\|^kworker/[1-9][[:digit:]]{2}$\|^permmgrd$\|^360sguard$`；import `popen/getuid/readlink/statfs` + `ASensorManager_*`（传感器指纹） | 〔观测〕**真实**，行为式模拟器检测（非字符串匹配）。`popen` 给 native 层任意 shell 原语 |
| N5 | **无 Frida/Xposed 检测**（两处命中经上下文核验为 `blackfriday`/`exposed` 假阳性）；**无 `TracerPid` 检测**；无内存扫描/进程注入 | 17 组正则 × 12 库 | 〔观测〕重要**否定结论**，避免夸大 |
| N7 | `libavlasys` 硬编码串 `62BAFD…739` | 字符串 | 〔未知〕若为授权 key 则可用它伪造 licence |

### 8.4 `libavlm.so` 的 C API（98 个，全量）〔观测，REA〕

- **扫描**：`AVLM_Scan`、`ScanEx`、`ScanExWith{Id,Name,Type,File,Path,Key}`、`ScanWithArgs`、`ScanWithScanOpt(Ex)`
- **生命周期**：`Init`、`InitEx`、`InitEnhancedEng`、`Release`、`ReleaseEnhancedEng`
- **选项**：`SetScanOpt`、`SetScanCategoryOpt`、`SetEngineMode`、`SetDEXScanLimit`、`SetOpcDEXScanLimit`、`SetScanLimit(Ex)`、`SetModulePath`、`SetAppInfoExtractSwitch/Rate`、`SetOutputOpt`
- **更新**：`Update`、`UpdateCheck(Ex)`、`UpdateCancel`、`UpdateRate`、`GetUpdateNetTrafficUsed`、`GetLastTotalUpdateTime`
- **结果**：`GetVirusDesc`、`GetVirnameEx`、`GetVirKnowledgeEx`、`GetWhitenameEx`、`Result_Free`、`FreeVirusDesc`
- **APK 元数据**：`GetAPKInfo`、`GetPackageName`、`GetCertHash`、`GetCertFullHash`、`GetMfMD5`、**`InstallPackage`**
- **官方应用防伪**：`CheckOfficalAPP` + `OfficalAPPResult_Get{Type,APPName,AntiyName,PKGName}` + `CheckAPPToken`/`ComputeAPPToken`/`CheckAPPKey`/`ComputeAPPKey`
- **日志云上传**：`LogUpLoad`、`SetLogOpt(WithKey)`、`SetLogPathForSDK`、`WriteLogMessage`、`GetLogContent`、`GetELogInfo`、`GetDetectLogInfo`、`FlushLog`、`LogFlush`
- **许可**：`GetSIDInfo`、`FreeSIDInfo`、`GetPubkeyContent`、`FreePubkeyContent`、`VirNamesChoice`、`AVLPack`
- **可疑短符号**（`size=4` 跳板，AVL 混淆风格）：`wtsr/effe/aesd/zavt/brmy/adce/haoz/exdz/fffe/efff`
- **字符串混淆**：`.rodata` 有连体 blob `http://.https://.Content-Length:.www.baidu.com.GET` 与 `SMS_Send.SMS_Block.System_DexLoad.System_ReflectInvoke.System_ReflectGet.Notification…` ⇒ **定长字符串表 + 别名导出**，非加壳

〔推断〕`AVLM_LogUpLoad` + `GetSIDInfo` + `ComputeAPPKey` 就是隐私文案里"上传 device ID 到安天服务器"的技术落点；**是否真被调用取决于 AVL 配置（`sdk.conf` 加密，〔未知〕）**。

---

## 9. 第三方 SDK：能力 vs 实际启用

### 9.1 类库构成真相〔观测〕

3,231 个类里，**真业务约 25 个混淆包**（`p0 t0 r0 s0 q0 u0 v0 g0 j0 c0 h0 l0 m0 x0` + 埋点 `v/u/z` + 属性 `c5/d5/f5/w`）；其余是库：

| 库 | 包 |
|---|---|
| **kotlin-reflect + 内嵌 Kotlin 编译器前端（最大，~45 包）** | `c4 l2 o2 o3 u2 g4 b3 b4 m4 m2 w2 w3 v2 v3 y2 i2 k2 d3 h3 j3 t3 z2 s3 n3 c3 i4 q3 r2 f2 c2 e2 u1 y1 h4 p2 p3 d2 g2 j4 a4 e4 u3 h2 l4` |
| kotlinx-coroutines | `s4 x4 u4 z4 w4 v4 t4 y4` |
| kotlin stdlib | `w1 v1 h1 i1 o4 p4 q4 k4 b2 r4 g1 k1 l1 m1` |
| **Apache Commons JEXL3**（规则表达式引擎） | `v5 a6 x5 y5 z5` |
| Gson | `l o n p q r t m c d e6` |
| OkHttp / Okio | `g5 l5 m5 p5 q5 i5 n5 o5 j5 s5` / `t5 u5` |
| Retrofit / protobuf / Room | `f6` / `m3 f3 h3` / `h i j e g` |

〔观测〕`META-INF/services/*` 的 SPI 注册反证 5 处：`i2.a`←`z3.b`（kotlin BuiltInsLoader）、`o3.g`←`u2.n/u2.l/u2.t`（kotlin-reflect 可覆盖性）、`s4.d0`←`t4.b`（CoroutineExceptionHandler）、`x4.v`←`t4.a`、`javax.script.ScriptEngineFactory`←`b6.a`（**空实现**）。

### 9.2 腾讯 AVL（411 类 + 3 .so + avlsdk）
- **能力**：多扫描 API、限流、证书解析、云查、日志上报、官方应用防伪、URL 信誉、引擎/库更新
- **实际启用**〔观测〕：只 `scan` / `scanURL` / `update` / `updateURLRule`；**关日志（`SetLogOpt` 未开）、无云配置**（`sdk.conf` 加密）
- `AVLScanOption` 五个选项位：类别 `0x7FFF` / 日志 `2` / 模式 `0` —— **本 APK 零使用**
- 结果模型：`Risk/*` → level 2；`Trojan/G-Ware/PornWare/RiskWare/Tool` → level 1；`White` → 过滤
- appkey `D5DFB878335A3EF4E76F60ED470B8EC8` 从自身 meta-data 读取（`a/e.java:39`）

### 9.3 腾讯 TMSDK（432 类 + 6 .so）
- **能力**：APK 扫描（本地+云）、特征库更新、URL 信誉、深度清理、短信/流量短信解析、号码标记、WiFi 七类检测、离线视频元数据
- **实际启用**〔观测〕：只活 **3 个 Manager**
  - `QScannerManagerV2`（scanType 6，云查 cmd **2016**）
  - `UpdateManager`（`mmgr.gtimg.com`）
  - `WifiDetectManager`（网络 / DNS 钓鱼(cmd **794/800**) / ARP / 加密类型）
- **全零调用**：`UrlCheckManager`、`TccCryptor`（除字符串解密）、`DeepCleanEngine`、`TrafficSmsParser`、`VideoMetaRunner`、`sms_check_v4`（只剩 3 个异常类）
- **死代码**：`com.tencent.tcuser.util.Encode` 静态块 `System.loadLibrary("xy")`，但 **APK 内无 `libxy.so`** ⇒ 一旦加载必 `UnsatisfiedLinkError`

### 9.4 小米 OneTrack（163 类）+ xCrash
- appId `2882303761517522561`（`z/b.java:22`）
- **实际事件只有 6 个**〔观测〕：4 个业务（`MiEngineNG_Scan`、`threats`、`blackgrays`、`blackgrays_threats`）+ 2 个 SDK 自动（`onetrack_dau`、`onetrack_bug_report`）
- 上报链路：gzip → **AES/ECB** → 3 张 SQLite 队列表（`events`/`events_cloud`/`monitor`）→ 重试 → `tracking.*.miui.com`
- **xCrash 2.4.9**：只落盘 `filesDir/tombstones`，**自身不上传**；由 OneTrack `CrashAnalysis` 读摘要后走 `onetrack_bug_report` 代发；`testJavaCrash/testNativeCrash` 是 `public static`（调试残留）

### 9.5 国密 / 框架类 / 残留
- `com.tencent.tmf.gm.{Sm2,Sm4}`：**均 JNI**（`libtmf-gm.so` 8 个导出），SM4 **默认密钥硬编码** `73 42 5A 55 18 08 06 5C 76 70 21 70 24 24 33 29`（`gm/b.java:7`）；SM2 公钥服务端下发（128 hex = X‖Y）
- **`com.sun.nio.zipfs`**：**活依赖** —— 以 `jar:` FileSystem 读 APK 内 dex（`p0/e:155`），靠 `META-INF/services/java.nio.file.spi.FileSystemProvider` 注册
- **`android.content.pm.PackageParser/PackageUserState/VerifierInfo`**：**活依赖** —— `w0/d.a()` 在 SDK≥28 且路径是**目录（split-APK 布局）**时用 `parsePackage` + `collectCertificates`，绕过系统 `PackageManager` 限制离线解析任意 APK
- **构建残留（勿夸大）**：Firebase/FCM/Crashlytics 配置串齐全（`google_app_id=1:484664152262:android:…`、`project_id=guardprovider-c3e77`）但 **APK 内 0 个 `com/google/**` 类、Manifest 无 Firebase 组件**；`libturingvd` + `TNative` **零 Java 引用**；二维码 `com.miui.qr` 只是字符串，**sources 与 smali 双搜 0 实现**
- **安装拦截/静默安装**〔观测〕：**只在 native**（`AVLA.installPackage(path, mode, …)`），Java 层 **0 处 `PackageInstaller`**；`GuardProvider` 对 install 场景只做扫描，不做安装

---

## 10. 隐私与数据外发（8 条已启用链路）

| # | 采集 | 触发点 | 存储 | 去向 |
|---|---|---|---|---|
| 1 | `ANDROID_ID` + `Build.MODEL` | `TencentEngine.java:402` → `TMSDKContext.setDeviceInfo` | 进程内静态 | TMSDK shark 请求（`tmfsdk.m.qq.com` / TCP 8001-8002） |
| 2 | WiFi `SSID`/`BSSID`/DHCP `DNS1/DNS2` | `TencentEngine.java:122` → `tmsdkobf/p9:210-254` | 仅请求体 | 腾讯云查 cmd **794**（DNS 劫持/钓鱼判定） |
| 3 | 当前 BSSID + `ScanResult` 列表 | `TencentEngine.java:107-114` | 不落盘 | 仅本地判定 |
| 4 | **IMEI 列表（明文）**、IMSI 列表（MD5） | `api/c.java:111,114`（`onetrack_dau`） | SQLite `events.data`（AES 加密） | `https://tracking.miui.com/track/v4` |
| 5 | IMEI(MD5)、OAID、AAID | `f/b.java:304-305`、`util/g:45-46` | 同上 + SP `instanceId` | 同上 / `…/api/open/device/writeBack` |
| 6 | 被扫 APK 的包名/版本/签名摘要/安装器/威胁名 | `MiEngineNG.java:201` | SQLite `events` | `tracking.miui.com` |
| 7 | 崩溃现场（堆栈、`/proc/maps`、fd、logcat 片段） | `XCrash` 落盘 → `CrashAnalysis:158-178` | `filesDir/tombstones/*.xcrash` | `onetrack_bug_report` → `tracking.miui.com`（仅摘要） |
| 8 | 任意 APK 的包名/版本/label/icon/签名 | `GuardProvider.java:168`（`parseApk`） | 不落盘 | 返回给持 `Guard_Provider` 权限的调用方 |

**门控事实**〔观测〕：OneTrack 的 IMEI/IMSI 采集**受区域开关门控**（`api/c.java:109`），且 `Configuration.Builder` 的 `f1855h`(IMSI)/`f1856i`(IMEI) **默认 true**，而 `z/b.java` **未调用 `setImeiEnable(false)` 关闭** ⇒ 在允许区域内**默认采集 IMEI/IMSI**。这是本软件隐私面最实质的风险点。
**未启用**〔观测〕：通讯录/剪贴板/定位/短信正文 在本 APK 无调用点；TMSDK "已安装应用列表"仅在扫描列表为空时触发（本 APK 不触发）。

---

## 11. 配置面（完整键表）

### 11.1 SharedPreferences `"GuardProvider"`〔观测，`w0/f.java:20`〕

| 键 | 类型 | 语义 |
|---|---|---|
| `sp_key_mi_engine_enable` / `sp_key_avl_engine_enable` / `sp_key_tencent_engine_enable` | bool | 引擎启用（默认 false；**`J()` 拒绝写 Mi**） |
| `cloud_engine` | String | 当前云引擎名（只能是非 MI） |
| `cloud_engine_list` | String(JSON) | 云控下发的引擎名集合 |
| `antivirus_is_init` | bool | 首次初始化哨兵 |
| `load_cloud_engine_time` | long | 云控拉取时间戳 |
| `virus_cache_validty_perod` | int | 缓存有效期（天，默认 3）—— **原拼写 `validty`（少一个 i）** |
| `key_uuid` | String | 首次生成的 `UUID.randomUUID()`，上报身份 |
| `avl_url_sdk_version` | String | AVL URL 库版本（**只写不读**） |
| `antivirus_db` / `antivirus_db_version` / `antivirus_is_init` / `cloud_cache` / `cloud_data` / `VIRUS_BASE_NAME` / `VIRUS_BASE_EN_NAME` / `virus_scan` / `virus_scanning` / `virus_name` | — | 旧栈与 native 侧键（DEX 字符串池确认存在） |

〔观测〕另有 4 组第三方偏好文件：`avl_conf` 与 `avl_<后缀>`、TMSDK 自建、OneTrack 自建。

### 11.2 系统属性 / 云控 / 设置〔观测〕

| 来源 | 键 |
|---|---|
| `SystemProperties`（反射 `d0/d`） | `persist.sys.sc_allow_conn`、`ro.miui.customized.region`、`ro.miui.region`、`ro.miui.cust_variant`、`ro.miui.restrict_imei`、`ro.miui.ui.version.code/name` |
| `MiuiSettings$SettingsCloudData`（反射 `d0/b`） | 模块 `VirusScan` 的 `miEngineNgEnable` 等 |
| `MiuiSettings$Privacy`（反射） | `isEnabled(context, key)` |
| `Settings.Secure` | `antivirus_engine_network_enable`、`android_id` |
| SecurityCenter metaData | `securitycenter.support.mix.scan`（混合扫描）、`miui.support_full_scan`（`guard/support_incremental` 的判定源） |
| 自身 metaData | `AVL_OPENSDK_APPKEY`、`guardprovider.support.mix.scan`、`guardprovider.scan_properties` —— **后两者全树 0 读取**（死配置；给**别人**读的声明） |
| `android.miui.AppOpsUtils.isXOptMode`（反射 `d0/a`） | XOpt 模式判定 |

---

## 12. 已知缺陷与风险清单（按严重度）

### 12.1 安全 / 隐私

| # | 问题 | 位置 | 性质 |
|---|---|---|---|
| S1 | **OneTrack 默认开启 IMEI/IMSI 采集**，未调用 `setImeiEnable(false)` | `Configuration.java:86-89` + `z/b.java` | 违反最小必要；与腾讯文案"上传加密 IMEI"错位 |
| S2 | **`sync_scan` 接受任意 `content://` URI 并复制到 `cacheDir/copy.apk`** | `GuardProvider.java:211-215` | **confused deputy**：以本进程权限读调用方无权读的 provider 内容 |
| S3 | **`parseApk` 可解析任意路径 APK**（无路径白名单） | `GuardProvider.java:159-190` | 任意 APK 元数据/签名/图标读取器 |
| S4 | **固定临时文件名 `copy.apk` + `REPLACE_EXISTING` 无大小限制** | `GuardProvider.java:213` | 并发互相覆盖；大文件耗尽 cacheDir |
| S5 | **命令注入面**：`Runtime.exec("ping -i 0.2 -c<n> -w <t> " + host)`，`host` 来自 IPC 参数且未过滤 shell 元字符 | `tmsdkobf/t9.java:84-95`，链路 `n0/k.c()` → `detectARPNoRoot` | 需 signature 权限才能触达，但**无二次校验** |
| S6 | **`tmsdkobf/nb.java` 以绝对路径打开第三方应用私有 DB**（`/data/data/com.qiyi.video/databases/…`、`com.sohu.sohuvideo/…`） | `nb.java:36,74,126` | TMSDK 遗留；本应用可达性〔未知〕 |
| S7 | **TMSDK 可写目录 `.so` 加载**：`System.load(<cacheDir 推导的 lib/> + name)`，尝试三种命名 | `tmsdkobf/ka.java:12-74` | 若该目录可写（同签名/root）即 so 劫持面 |
| S8 | **`libturingvd.so` 用 `popen` 执行 shell** + 读传感器做设备指纹 | import 表 | native 层任意 shell 原语 |
| S9 | **`libtmf-gm.so` 用 `rand/srand/time` 做随机源**（无 `getrandom`） | import 表 | SM2 的 `k` 若来自 `rand()` ⇒ 可预测 〔未知，需反汇编〕 |
| S10 | **TMSDK 走明文 HTTP** `http://tools.3g.qq.com/wifi/cw.html` | `p9.java:269` | MITM 可伪造 `Meri` 响应头 ⇒ 把"被劫持"误报为"安全" |
| S11 | **裸 IP 直连兜底**（`120.241.190.227:8002` 等） | `w5.java:154-166` | 绕过 DNS/域名封禁策略 |
| S12 | **SM4 默认密钥硬编码** | `com/tencent/tmf/gm/b.java:7` | 若 TMSDK 通道用默认 key 而非会话 key，抓包可离线解密 |
| S13 | **签名盐为静态常量、无 HMAC/nonce/时间戳** | §7.2 | 请求可离线伪造、可重放 |
| S14 | **`assets/mdetector` 是藏在 assets 的 ARM32 可执行体，且用 `Runtime.exec` 调起**（`tmsdkobf/t9.smali:563`、`com/avl/engine/j/b/e.smali:217,383`） | §8.2 | **高危形态**：绕过 `lib/` 目录可见性直接跑外部可执行体；结果经 stdout 回传 Java |
| S15 | **审计盲区**：`SecurityService` 事务 20/21/22/23 **不判用户锁** | `SecurityService.java` | 锁屏期间可被触发增量扫描与清库 |
| S16 | **`assets/40706.dat`、`licence*.conf`、`avlsdk:sdk.conf` 均为加密二进制** | §8.2 | 静态不可读〔未知〕 |

### 12.2 功能缺陷 / 实现 bug（影响防护效果）

| # | 问题 | 影响 |
|---|---|---|
| B1 | `n0/g.x()` 返回 `new String[4]` 只填 3 个 ⇒ **末位恒 null** | 调用方遍历会拿到 null 引擎名 |
| B2 | `y.f` Parcel 写 12 读 10 ⇒ `adLevel` 丢失；`versionCode`/`isAuthoritative`/`isInCache` 不序列化 | 跨进程后广告信息与版本信息失效 |
| B3 | `n0/g.F(name)` 恒返回枚举 `f3551f`（三引擎都 true） | **"是否支持云扫"与引擎无关，是恒真值** |
| B4 | `n0/g.s()`（checkUpdate）**恒 `return -1`** | `IAntiVirusServer.C` 永远报告"检查失败" |
| B5 | `install_scan` 的 `engineName` 被四引擎全部忽略 | **"按引擎名扫描"功能不存在** |
| B6 | `scanType` 只作进度偏移，非模式 | 语义误解 |
| B7 | `guardprovider.scan_properties` / `guardprovider.support.mix.scan` **0 读取** | 死配置 |
| B8 | `i0/f.b(pkg,sig)` 是 `return false` 的死 hook | 内置白名单被裁，只剩服务端下发 |
| B9 | `qax_dialog_*` 全 `TODO`、zh 版复制腾讯文案 | QAX 引擎未实现 |
| B10 | **`virusType` 只有 0/1**；12 个 `threat_type_*` 零引用；`adLevel/adName/adDescription` 全树 0 赋值 | 广告检测是**空协议位**；威胁分类不在本层 |
| B11 | **URL 特征库 `20180917.zz` 落后约 5 年**（AV 库为 `20220928.ah`） | 2019 年后新钓鱼域名几乎无覆盖 |
| B12 | **`libavlurl.so` 的 SONAME 是 `liburl.so`**，而 `liburldetectorsys` 按 `liburl.so` dlopen | 若未调 `setLibPath()`，Android **不会**按 SONAME 匹配文件名 ⇒ **URL 引擎整体失效、静默降级** |
| B13 | `libTmsdk` 文件名 2.0.13 / SONAME 2.0.12 | 版本错配 |
| B14 | **内置 RSA 公钥证书 `NotAfter 2012-02-24` 已过期**，且与随包 licence 不匹配 | TMSDK 授权校验链在本包内不可能通过 ⇒ 授权功能降级 |
| B15 | `mdetector` 是 ARM32，而 APK 只带 arm64-v8a | 纯 64 位机型上 ARP 检测静默失效 |
| B16 | Room 列级 AES 用随机 IV ⇒ UNIQUE 索引与等值查询失效 | 库无限增长、查询退化为全表扫描 |
| B17 | 云扫响应索引对齐写成 `payload.packageName != apkInfo.packageName` 即报错 | 疑似 `!=`/`equals` 取反错误 |
| B18 | `i0.h.h()` 读的是 `miui.support_full_scan`（**全量扫描能力位**），而 URI 名叫 `support_incremental` | 命名与语义错位 |
| B19 | `Library` 版本 `libtmg-gm.so` **在 APK 中不存在**（只有 `libtmf-gm.so`） | 命名不一致 |

---

## 13. 未解决问题（汇总，按优先级）

| # | 问题 | 现状 | 下一步 |
|---|---|---|---|
| U0 | **本机无可用 ELF 反编译路径**：Ghidra(win32) 仅 PE/6502、Hopper 需 macOS、IDA/Kuna 未配置 | 已用自写 ELF32/64 解析 + 全库正则取证 + AArch64 指令解码替代 | 在 Linux/macOS 上 `open_binary` ELF-arm64，或配置 IDA/Kuna |
| U1 | `libflamingo.so` 内部算法（Aho-Corasick 已知，但 `matches` 的二进制特征语法/边界条件未知） | 仅 3 个 JNI 符号 + crate 信息 | arm64 反汇编 `Java_…_matches` |
| U2 | 6 张 `RegisterNatives` 表的**方法签名逐条语义**（类名已知，60 个方法的行为未逐条落定） | 类名+签名已取明文 | 逐签名映射到 Java 侧调用点 |
| U3 | **`avlsdk:sdk_conf/sdk.conf` 的密文段（`0x17C` 起 9,999 B）用什么算法**（可能含 AVL 云端域名/开关） | 已排除 AES/SM4 整块模式（长度非 16 倍数）+ 已排除已知密钥 | 反汇编 `libavlasys`/`libavlm` 的 config 解析；或动态 dump 解密后明文 |
| U4 | **`assets/40706.dat` 的 `TccCryptor` 具体算法与密钥位置** | 已知 8 字节对齐、已知走 TccCryptor | 反汇编 `libTmsdk` 的 `TccCryptor.decrypt` |
| U5 | ~~`assets/mdetector` 的释放/exec 代码位置~~ | **已闭合**：`Runtime.exec` @ `tmsdkobf/t9.smali:563`、`com/avl/engine/j/b/e.smali:217,383` | — |
| U6 | **SM2 的 `k` 是否真的取自 `rand()`** | 只有 import 证据 | 反汇编 `libtmf-gm` 的 SM2 加密路径 |
| U7 | **TMSDK / AVL 实际上报字段**（是否含 IMEI） | Java 层确认只传 `android_id`+`MODEL` | 抓包 `tmfsdk.m.qq.com` / `virus-engine.sec.miui.com` |
| U8 | Room 等值查询是否为死代码（因随机 IV） | 代码上几乎不可能命中 | 运行期验证（需真机 + 触发同步） |
| U9 | 云扫响应的 `!=` 断言是否真 bug | 静态可疑 | 运行期验证 |
| U10 | `tmsdkobf/nb.java` 跨应用 DB 读取的可达性 | 代码存在，无调用方 | `trace_android_references` 追入边 |
| U11 | `tmsdkobf/t9.java` 的 `ping` 注入是否能被 IPC 参数控制 | 链路存在，`str2` 未过滤 | 追事务 14 → `t9.a()` 参数流 |
| U12 | `y.f` 的 level `4`/`5` 语义 | 只从合并优先级与上报抑制反推 | 需安全中心侧消费代码 |
| U13 | 18 个混淆包完全无法归类（另 6 个低置信） | 已列清单 | 需要更多交叉引用或动态验证 |
| U14 | `libavlurl.so` 的 ~400 条域名（Public Suffix List 判断）是否还含其它用途 | 已达 400 上限，实际更多 | 提高上限重扫 |

---

## 14. 附件与证据索引

### 14.1 附件

| 文件 | 规模 | 内容 |
|---|---|---|
| `10_core.md` | 905 行 / 96 KB | 主控层：`n0/g` 全方法表、26 个 AIDL 事务码、`y.f` 全字段表、四引擎矩阵、R1–R20 高危清单、SharedPreferences 全表 |
| `15_obf.md` | 481 行 / 45 KB | 179 个混淆包语义地图（业务 ~25 / 库其余）、6 条可复现链路、路径/Intent/SQL/密钥全表、未归类 18 个 |
| `20_cloud.md` | 940 行 / 70 KB | Room 8 表完整 DDL、列级 AES、JCE 编码规则、两套网络栈、7 个新栈端点、报文全字段、TTL 策略 |
| `30_native.md` | 859 行 / 90 KB | 12 个 `.so` 身份表 + 8 个 assets 鉴定 + JNI 边界 + 可疑能力 17 组正则举证 + 常量表 + R1–R12 |
| `40_vendor.md` | 716 行 / 72 KB | AVL/TMSDK/OneTrack/xCrash/国密/zipfs/PackageParser 的"声称 vs 启用"总账、28 条端点、8 条隐私链路 |
| `_obf_index.tsv` | 3,231 行 | 全类索引（`包/类.java<TAB>类声明`） |
| `dex_strings_all.txt` | 29,263 行 | `classes.dex` 原始 MUTF-8 串全量（交叉验证用） |
| `src/sources/` | 3,231 个 .java | 全量 JADX 反编译源码 |
| `apktool/` | — | smali + res（含 44 个语言 `strings.xml`）+ 原样 `lib/`/`assets/` |
| `avlsdk_extract/` | 26 项 | `assets/avlsdk` 解包（AVL 特征库） |

### 14.2 REA 证据 ID（关键 12 条）

| 操作 | 对象 | 支撑 |
|---|---|---|
| `inspect_android_package` `ev_5fdff3ac…` | APK | 包身份、权限、组件、3242 类 |
| `inspect_analysis_view`(manifest) `ev_8389fb90…` | APK | 完整 Manifest 文本 |
| `inspect_apk_signature` `ev_c65c7674…` | APK | v1/v2/v3 三签同证书、无轮转 |
| `inspect_android_class` `ev_37fb1e7f…` | `GuardProvider` | 15 方法 / 2 内部类 |
| `inspect_android_class` `ev_cdd2481e…` | `SecurityService` | 12 方法、两个 Binder 字段 |
| `inspect_android_class` `ev_f6df526e…` | `SecurityService$a` | **24 个 AIDL 方法** |
| `inspect_android_class` `ev_80d5f918…` | `f0.b` | **仅 3 个枚举常量**（无 QAX） |
| `inspect_android_class` `ev_0c030a86…` | `RustSupport` | 3 个 native 方法 |
| `inspect_native_exports` `ev_3917ad9c…` / `ev_7a386e6f…` / `ev_07e78735…` | `libflamingo.so` / `libavlm.so` | 3 导出 / 98 个 `AVLM_*` 全量 |
| `inspect_android_class` `ev_3a4f3298…` `ev_52917bb2…` `ev_4c3c9e94…` | `TencentEngine.e` / `w0.d.a` / `z.b.<init>` | TMSDK 初始化 / 离线 APK 解析 / OneTrack 配置 |
| `inspect_android_class` / `inspect_android_method` `ev_3415eba8…` `ev_73f513b6…` `ev_b6d546cf…` | `EngineDatabase_Impl$a.a` / `v0.j` / 云端 POJO | DDL / 密钥派生 / 报文结构 |

### 14.3 本报告 §10 已独立复核的结论

| # | 结论 | 复核方式 |
|---|---|---|
| 1 | AES 密钥 = `wcw1xbplx7aijwqr` | 直接读 APK 内 `flamingo.dat` 偏移 144 的 16 字节，LE int64 拆分 XOR `0x430BF1FFFFF10B43` |
| 2 | 三个盐值 | 源码精确定位 `i0/d:55,203`、`j0/c:75`、`q0/q:22` |
| 3 | `scan_params` 位域 + `CloudScanApkInfo` 五字段 | 逐行读 `p0/f:348-379` |
| 4 | 云端 base URL 国内外切换 | `q0/m:12` |
| 5 | `n0/g.x()` 返回 `String[4]` 只填 3 | 逐行读 `n0/g` |
| 6 | `M()` 中 `scanType==1` 才是增量 | 逐行读 `n0/g.M()` |
| 7 | **规则引擎 = Apache Commons JEXL3** | `z5/b`=JexlBuilder、`v5/e` 判 `org.apache.commons.jexl3.*`、`a6/c3`=JexlParser 语法常量表 |
| 8 | `AVLA` 是 JNI 门面（方法名混淆 `pcit/icf/gcv/fcf`） | 源码 + `libavlasys` 有 `JNI_OnLoad` |
| 9 | `libflamingo` 仅 3 导出，且与 `RustSupport` 三方法一一对应 | REA `inspect_native_exports` + 明文符号扫描 |
| 10 | `assets/avlsdk` 是 26 项 ZIP；`version.conf` 明文 | 直接解包读取 |
| 11 | `mdetector` = ELF32 ET_DYN ARM + libpcap + ARP 劫持串；加载方式 = `Runtime.exec`（`t9.smali:563`、`com/avl/engine/j/b/e.smali:217,383`） | ELF 头字段 + 字符串 + smali 定位 |
| 12 | `70003.amf` = `"amf\0"`+v4 + 双时间戳 + **42,556×16 B 密文记录** | 头部 hex + 长度整除 |
| 13 | `licence.conf`/`licence1.conf` = `[128 B RSA/ECB/PKCS1][DES/ECB/NoPadding]` → `Properties`；内置证书 2012 已过期 | 长度整除 + RSA 去填充失败 |
| 14 | `sdk.conf` 密文段非 16 倍数 ⇒ **AES/SM4 整块模式结构性排除**（修正 Lead 初判） | 偏移 0x17C 起的 9,999 B |
| 15 | 奇安信 QAX 未实现 | 资源 TODO + 枚举 3 项 + 无类 |
| 16 | Firebase 为构建残留 | 配置串存在但 0 个 `com/google/**` 类 |
| 17 | QR 码无实现 | sources + smali 双搜仅 1 处字符串命中 |
| 18 | **12 个库均无 Frida/Xposed 检测**（`Frida`←`Friday`、`frida`←`blackfriday`、`xposed`←`exposed` 三处假阳性） | 17 组正则 × 12 库 + 上下文核验 |

