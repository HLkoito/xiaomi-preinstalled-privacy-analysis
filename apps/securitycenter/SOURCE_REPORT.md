> **历史分析存档，非最终结论。** 本文保留原始分析的技术内容，可能包含尚未验证或已被纠正的表述。隐私数据上传范围、同意条件、FBO 本地委托、报警短信加密、Provider 鉴权等，以 [最终报告](REPORT.md) 与 [独立复核](REVIEW.md) 为准。文中源码定位保留为原反编译相对路径；只有 evidence/ 中列出的精选文件随包提供，未打包完整源码树。

# MIUI 安全中心 (MIUISecurityCenter) 完整逆向分析报告

> 目标：`MIUISecurityCenter-10.4.9-250402.0.1.apk`
> SHA-256：`fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd`（66,917,411 B）
> 方法：静态逆向（jadx 全量反编译 10,624 个 .java / apktool 资源解码 / 原生库 ELF 静态解析 / SQLite 结构解析 / REA 证据链）
> **本文不含任何真机动态验证**：所有"行为"均为**代码静态事实**或**推断**，三态标注见下。

## 0. 阅读说明与证据分级

| 标注 | 含义 |
|---|---|
| `观测` | 直接来自 shipped artifact（文件字节、反编译源码、SQLite 结构、签名结构、ELF 符号/字符串） |
| `推断` | 由多处观测推出的结论（可能错，均附依据） |
| `未知` | 未取得证据，明确留白 |

**工作副本（可复现）**
| 路径 | 内容 |
|---|---|
| `_analysis\apk_full\` | ZIP 完整解包（9231 文件；另 305 个大小写冲突项加 `.dup` 后缀） |
| `_analysis\apktool_sc\` | apktool 3.0.3 解码：`AndroidManifest.xml` + 全部 `res/` |
| `_analysis\jadxout\sources\` | jadx（自带 jadx-core 直驱，`tools/JadxDump.java`）全量 Java 源码，10,624 文件 |
| `_analysis\findings\` | 各专项证据文件（见 §12 附录） |
| `_analysis\tools\` | 自建解析器（jieba 词典、IDF、AArch64 反汇编器、AES 爆破等） |

**全量派生数据**：`findings/dex_strings_all.txt`（212,045 条唯一字符串）、`findings/url_usage.tsv`（URL→引用类）、`findings/components_all.tsv`（424 个组件全表）。

**覆盖声明**：本报告基于 **7 路并行静态逆向**（原生库 / 内置数据资产 / 清单与 SDK / 安全与拦截 / 网络与预警 / 权限与隐私 / 电量与游戏）汇总，另有 3 处队友间互相纠错已并入正文（`electricrisk` 实为 AI 反诈、`garbage_clean.db` 为死资产、`character.json`/`dict`/`pinyinindex.idf` 非加密）。**未做**：真机动态验证、服务端响应抓包、`com.miui.guardprovider` 与 `com.miui.cleanmaster` 等外部 APK 的逆向、aarch64 ELF 的伪代码级反编译（本机 REA 的 Ghidra 仅支持 PE，已用自建 AArch64 反汇编器替代）。

**按问题定位**：①"包含哪些功能"→ §4（模块总表 + 逐域）；②"对应实现逻辑"→ §5（14 条链路）；③"内置了什么数据库"→ §6（2 个随包 SQLite + 20 个运行时库 + 明文词表/规则 + 证书密钥）；④"其他信息"→ §2（身份/签名）、§7（云接口）、§8（native）、§9（SDK/广告）、§10（安全与隐私）。

---

## 1. 结论速览（20 条）

1. **这是一个以 `android.uid.system` 身份运行、MIUI 平台签名的 ROM 预置系统应用**，不是普通 App：`sharedUserId=android.uid.system`、签名证书 `CN=MIUI, O=Xiaomi`（2011 起，2039 到期）、`minSdk 23 / targetSdk 34`（Android 14 编译）。`观测`
2. **它是一个"超级工具箱"**：295 个 Activity / 67 Service / 22 Receiver / 40 Provider，至少覆盖 **20 个业务域**（体检与杀毒、骚扰拦截与反诈、网络与流量防火墙、权限与隐私、电量与性能、存储清理、游戏加速、自动任务、端侧 AI 预测、灾害/地震预警、AI 反诈守护、SIM 锁、通知气泡、应用锁、安全分享、运营商流量业务、TEE 可信 UI、广告开屏……，详见 §4.1 的 25 行模块表）。
3. **进程模型 4 个**：主进程（UI）、`:ui`、**`remote`（承载 100 个组件，是后台业务核心）**、`bootaware`（SIM 锁）。
4. **首页/结果页是"云端下发的卡片流"**：`com.miui.common.card`（60 类 CardModel）由 `https://adv.sec.miui.com/info/layout` 下发 JSON 渲染，卡片类型包含**广告**（`Adv*CardModel`、`AdvInternationalCardModel`）、活动、资讯、功能宫格、排行。即**首页有广告位，可由服务端随时改版**。`观测`
5. **一键体检 36 组检查项**由 `assets/scanitem.json`（version 8）数据驱动：`manual` 28 组 + `system` 8 组，每项映射到 `com.miui.securityscan.model.**` 类并带权重分（0~5）。评测/打分由 `securityscan/scanner/ScoreManager.java` 汇总。`观测`
6. **自研 HTTP 层 + 私有签名**：`eg/j.java`（`NetUtil`，43 KB）统一发请求，签名 = `MD5(Base64(按 key 排序的 k=v 串 + "&" + APP_KEY))`；公共参数含 **`m` = IMEI 的 MD5**、机型/区域/MIUI 版本/渠道/versionCode/时间戳。`观测`
7. **两套 APPK 常量被硬编码**：`2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9`、`5cdd8678-cddf-4269-ab73-48387445bba6`（用于不同业务线的接口签名）。`观测`
8. **内置 2 个 SQLite 数据库**：`res/raw/parse.db`（**明文，1043 条应用目录映射 + 10 条系统临时目录**，深度清理用）与 `assets/garbage_clean.db`（3 表骨架，4 行数据**全部被流密码式混淆**，且**全 APK 无读取者**）。另有 **20+ 个运行时创建的库**（流量、防火墙、灾害、地震、电量历史、游戏加速、自动任务、月报、预测、通知图等）。`观测`
9. **真正可用的"词表/规则库"是一批明文资产**：jieba 中文分词词典 `assets/dict`（**348,982 词 + 35,224 字表**，已完整解码）、汉字拼音表 `pinyinindex.idf`（**20,902 字 / 1,336 拼音串**，已完整解码）、短信识别规则 `assets/pattern`（v1.2 正则集）、URL 白名单 452 条、服务号码 224 条、省市表 34/336、支付安全目标 211 个包、游戏榜 607 个包、行为记录白名单 57/43 条（按签名摘要）。`观测`
10. **`assets/censor_source`（2,688 B，熵 7.933）是加密的敏感词库**，由 `liblib-riskcontrol.so` 的 `SensitiveWordDetector::load()` 经自实现 AES 解密后建 Trie 树；**密钥未解出**（48 候选×5 IV×2 模式全部失败）。`未知`
11. **18 个原生库**（arm64 + v7a 各 18）：唯一硬编码网络端点在 `libtcp.so` → `https://net-assistant.10046.xiaomimobile.com/v1/traffic/directives`，指令格式 **base64(JSON)**，能力含短信模板匹配、`getSubscriberId`、`setImsi`、`collectUserData`。`观测`
12. **`libtcp.so` 内藏 13 段 `PROGUARD_*` 片段化常量**，解出的多段 base64 是中文垃圾短信话术（如"死不休哦，要想清楚哦"），兼作 AES 密钥拼料与短信语料。`观测`
13. **`libsec-common-utils.so` 是"密钥中心 + 调用方门禁"**：提供 6 个 `ConfigManager.get*SecretKey`（Encode/Decode/Http/Uuid/Rubbish/NetworkAssistant），并用 `getPackageInfo().signatures`+`sha256` 校验调用方包名白名单（`com.miui.securitycenter`、`com.miui.cleaner`、`com.miui.cleanmaster`、`com.miui.global.packageinstaller`）；硬编码证书指纹 `C8:A2:E9:…:2A:B8` 与**本 APK 实际签名不符**（pin 的是另一把证书）。`观测`
14. **端侧 AI 是真实存在的**：`assets/rec_model_20230425.mnn`（阿里 MNN FlatBuffers，9 个 embedding + 5 个 linear + MaxPool，**二分类"下一个 App"打分**）+ `assets/character.json`（40 字符词表）+ `libMNN*`/`libMNNTrain` → **支持端侧增量训练**（`MNNTrainNative.nativeTrainRecModel`，样本 <50 放弃）。`观测`
15. **`libmicontinuity_sdk.so` 是跨设备通道**（`ChannelImplBySocket::{SendFile,SendBytes,ContinueSend}` + `NInputReader_read/skip`），**导入符号里没有 SSL**；`libxcrash_dumper.so` 不是 JNI 库而是**被拉起的独立崩溃转储进程**（`__libc_init`、无 `JNI_OnLoad`、`ptrace`+`/proc/self/maps`）。`观测`
16. **native 层无加壳、无反调试**（有界否定）：18 个库全为 stripped + BIND_NOW，但 `.rodata` 保留完整 C++ RTTI 与源文件路径；`frida/xposed/magisk/TracerPid` 零命中。`观测`
17. **网络配置刻意放宽**：`network_security_config.xml` 全局 `cleartextTrafficPermitted="true"`，并为 `miwifi.com` 系列域名导入 2 张自签/私有 CA 作为信任锚（`wlan_speed.crt` RSA-1024 自签 `CN=www.miwifi.com`；`wlan_speed_new.crt` `MIWIFI ROOT CA`，RSA-2048，有效期至 **2120 年**）。`观测`
18. **广告与统计链路完整**：`adv.sec.miui.com`（卡片/游戏/视频广告）、`data.sec.miui.com`（埋点上报）、`sdkconfig.ad.xiaomi.com`（广告 SDK 配置）、`com.miui.msa.util.MsaUtils`（MSA 联盟 OAID）、`assets/placement_id` = **Facebook 广告位** `fb:196567740764356_196569107430886`、`com.miui.systemAdSolution`（开屏广告 Provider）、`SystemAdContentProvider`（authority `com.miui.securitycenter.splashAd`）。`观测`
19. **隐私相关能力密度极高**：读/写短信与通话记录、读取联系人与音频、`MASTER_CLEAR`/`REBOOT`/`FORCE_STOP_PACKAGES`/`CLEAR_APP_USER_DATA`/`PACKAGE_VERIFICATION_AGENT`/`MANAGE_USB`；具备**无障碍服务**（`AntiMsgAccessibilityService`）、**通知监听**（`BubblesNotificationListenerService`）、**屏幕/窗口叠加**（`SYSTEM_ALERT_WINDOW`）、**输入法监控**（`df.h` 监听 `default_input_method`）、**剪贴板/输入隐私模式**、**应用锁**、**隐私空间**、**隐私模糊**。
20. **隐私数据流已复核（2026-11-06 增补 §10.5）**：证据最强的是 **广告请求同时携带多种可关联标识**（`x`=Android ID、`oa`=OAID、`m`=IMEI-MD5、`deviceId`=SHA1(IMEI+AndroidID+序列号)，`eg/j.java:569-572` + `com/miui/antivirus/result/f.java:272`），而"个性化广告开关"只是请求参数、**不会停止拼接**；其次是**隐私风险检测页会触发"非系统应用清单"上传**（`xc/h.java:13` 用 `getInstalledPackages(0)` 全量过滤 `FLAG_SYSTEM`，**不筛桌面可见**）。其余链路均有开关、弹窗或本地短路（详见 §10.5 的分级表），不能一概而论为"未经同意采集"。

---

## 2. 目标物与构建事实

### 2.1 身份
| 项 | 值 |
|---|---|
| 包名 | `com.miui.securitycenter` |
| versionName / versionCode | `10.4.9-250402.0.1` / `40001049`（内部 `BuildConfig.VERSION_CODE` 一致） |
| SDK | min 23、target 34、compile 34（Android 14） |
| `sharedUserId` | **`android.uid.system`** |
| application | `com.miui.securitycenter.Application`（继承 `com.miui.common.h`） |
| backupAgent | `com.miui.antispam.service.backup.SecurityCenterBackupAgent`（骚扰拦截配置备份） |
| 图标可隐藏 | `meta-data app_hide_enable=true`、`disable_notifications_when_app_hide=false`；`meta-data miui.support_full_scan=true` |
| 原生库装载 | `extractNativeLibs=false`（直接 mmap APK 内 .so） |
| 网络策略 | `@xml/network_security_config`（见 §10.3） |
| 语言 | `default` + `zh-rCN`(607 KB) / `zh-rTW`(614 KB) / `zh-rHK`(611 KB) + `bo-rCN`(藏, 1.27 MB) + `ug-rCN`(维, 901 KB) |
| 构建 | AGP 7.3.1（`META-INF/com/android/build/gradle/app-metadata.properties`） |
| 启动入口 | `com.miui.securityscan.MainEntryActivity`（**`android:enabled="false"`**，由 ROM/云控开关；intent-filter：MAIN / `miui.intent.action.SECURITY_CENTER` / `mi.quicksearch.intent.action.RETRIEVE` / VIEW） |

### 2.2 体积构成
| 组成 | 大小 |
|---|---|
| `classes.dex` / `classes2.dex` / `classes3.dex` | 8.14 MB / 8.84 MB / 7.54 MB（合计 24.5 MB） |
| `resources.arsc` | 8.13 MB（9231 个资源条目） |
| `assets/` | 248 文件 / 8.97 MB（`dict` 3.64 MB 最大） |
| `lib/` | 36 个 .so（arm64 18 + v7a 18），单架构约 15 MB |
| `res/raw/` | 38 个（含 `parse.db`、2 张证书、视频/音频/shader） |
| `META-INF/` | v1 签名（`PLATFORM.SF`/`PLATFORM.RSA`）+ Kotlin 协程 SPI 注册 |

### 2.3 代码规模
- 顶层类 **10,642**（含内部类 25,031），方法 **166,513**，指令 **4,437,463**（jadx `RootNode` 观测）。
- 混淆：**业务包保留可读命名**（`com.miui.securityscan.*`、`com.miui.networkassistant.*`…），**基础设施与三方 SDK 被 R8 重命名为单/双字母包**（`a`…`zm`、`eg`、`xe`、`m7`、`u3`、`vf`、`m3`、`i9`、`ra`、`lj`、`mj`、`cl`、`am`、`ea`、`ia`…），因此"功能可读、底层需追溯"。

---

## 3. 整体架构

### 3.1 进程与生命周期（`观测`）
| 进程 | 组件数 | 承载 |
|---|---|---|
| `com.miui.securitycenter`（主） | UI 与少量组件 | 全部 Activity/UI、`WidgetProvider` 入口 |
| `com.miui.securitycenter:ui` | 6 | `com.miui.bubbles.settings.*`（气泡设置）等 |
| **`com.miui.securitycenter.remote`** | **100** | **后台业务核心**：40 个 Provider（antispam/autotask/earthquake/disaster/gamebooster/networkassistant/permcenter/powercenter/warningcenter/xsof/idprovider…）、绝大多数 Service/Receiver |
| `com.miui.securitycenter.bootaware` | 3 | `com.miui.simlock.*`（SIM 锁，开机即启） |

`Application.onCreate`（`com/miui/securitycenter/Application.java:711`）按 `getPackageName()` 判断进程并分别初始化：
- 主进程：`R()`（UI 基础）、`W()`、`D()`、`com.miui.common.f.g()`、`hh.b.m()`；
- `:ui`：`AnalyticsUtil.initMiStats()`、`of.e.r()`；
- `remote`（最长分支）：`AnalyticsUtil`、`w.E()`、`l.a()`、`ConnectivityChangeJobService2.s()`、`c3.d.g/c()`、`SimCardHelper.asyncInit()`、**`AppOpsUtilsCompat.autoOptmize()`**（自动权限优化）、`k.a().b()`、`AiGuardUtils`（反射调用 AI 用电守护，支持则拉起 `com.miui.electricrisk.AiGuardSceneService`）、`xc.p.h()`/`df.h.c()`（国际版/国内版分流）、`df.h.E()` + 监听 `default_input_method`（**输入法隐私**）、`com.miui.simlock.c.p()`、`j.d(ctx,"application")`、`d0/Z/X`、`p3.g.f()`、空闲处理器 `U()` 等 20+ 项；对 `cappu`/`clover` 机型额外跑 `new a().execute()`。

### 3.2 组件规模与暴露面
| 类型 | 数量 |
|---|---|
| activity / activity-alias | 293 / 2 |
| service | 67 |
| receiver | 22 |
| provider | 40 |
| 自定义 `<permission>` | 全部为 `signatureOrSystem`/`signature`（极少数 `normal`） |
| `uses-permission` | 约 150 条（含大量 MIUI 私有系统权限） |

> 完整 425 行组件表见 `findings/components_all.tsv`；导出面清单与风险评级见 §10.1。

### 3.3 核心基础设施（决定"能力上限"的 6 个框架）
| 框架 | 位置 | 作用 |
|---|---|---|
| **卡片系统** | `com.miui.common.card`（60 类，`models/` 49 个 CardModel） | 首页/结果页 = 云端 JSON 卡片流。类型：`FuncGrid6CardModel`（功能宫格）、`FuncTopBanner*`（Banner）、`Adv*CardModel`（**广告**：大图/三图/网站/列表/国际）、`Activity*CardModel`（活动）、`NewsListBannerCardModel`（资讯）、`ListTitleFlowRankCardModel`/`ListTitleConsumePowerRankCardModel`（流量/耗电排行）、`ScanResultTop/BottomCardModel*`（体检结果） |
| **远端配置** | `com.miui.common.persistence.RemoteProvider` + `remote_provider_preferences` | 跨进程键值配置（`RemoteProvider.java:264`） |
| **HTTP 层** | `eg.j`（`NetUtil`，43 KB）+ `xe.a`（AdNetUtils） | 统一请求、签名、UA、失败重试、埋点回调 `p4.i` |
| **动态 UI 引擎** | `com.miui.maml`（221 类） | MAML 卡片/动画渲染（`assets/maml/**/manifest.xml` 即其资源），支持脚本与表达式 → **可云端下发 UI 描述** |
| **埋点** | `com.miui.analytics`(10) + `com.xiaomi.onetrack`(179) + `com.xiaomi.analytics`(12) | 双通道统计，本地队列库 `events/events_cloud/monitor` |
| **端侧 AI** | `com.miui.mlkit.mobilerec.ranking.mnn.*` + `libMNN*` / `libtensorflowlite_jni` | App 预测/排序，含端侧训练 |

### 3.4 云端驱动的"布局/广告"链路（`观测`）
```
首页/结果页请求  →  POST https://adv.sec.miui.com/info/layout
   参数: functionId / 设备与版本公共参数 / isPersonalizedAdEnabled / cv=3 / ror / rbr
   签名: sign = MD5(Base64(sortedParams + "&" + APP_KEY))
   回调: p4.i("antivirus_scanresultcontrol" | "optimizemanage_omdatamodel" | ...)
返回 → CardModelMaker 解析 → 生成 CardModel 列表 → RecyclerView 渲染
```
调用点：`com/miui/antivirus/result/f.java:284`、`c0.java:263,283`、`com/miui/optimizemanage/optimizeresult/e.java:244,249`。
→ **首页"功能入口/广告/资讯"完全由服务端决定**，本地只保留兜底布局（`FuncGrid6CardModel` 等）。

## 4. 功能全景（按业务域）

### 4.1 模块总表
| # | 业务域 | 包（类数） | Activity 数 | 用户能做什么 |
|---|---|---|---|---|
| 1 | **体检/优化** | `securityscan`(133) + `common.card`(60) | 10 | 一键体检、评分、结果卡片、白名单、设置 |
| 2 | **病毒查杀** | `antivirus`(82) | 9 | 全盘/快速查杀（引擎在外部 GuardProvider）、病毒库更新、隔离/处理、支付安全守护 |
| 3 | **骚扰拦截** | `antispam`(72) + `maml` 卡片 | 15 | 黑/白名单、关键词、21 条策略链、号码标记、拦截记录、备份 |
| 4 | **反诈与风控** | `antifraud`(5) + `riskcontrol`(1) + **`electricrisk`(27)** | 6 | 支付页监控、敏感词检测、AI 反诈场景守护（电话/短信/应用/网页/投屏） |
| 5 | **红包助手** | `luckymoney`(54) | 6 | 微信/QQ 红包提醒、自动跳转、声音提示 |
| 6 | **报警辅助** | `policeassist`(20) | 1 | 紧急报警引导、位置信息辅助 |
| 7 | **网络与流量** | `networkassistant`(417) | 21 | 流量统计/套餐/校准、联网控制（防火墙）、热点限额、漫游、流量包购买、VPN、状态栏流量 |
| 8 | **灾害预警** | `warningcenter`(81) | 10 | 12379 灾害预警订阅/推送、米家联动、防御指南 |
| 9 | **地震预警** | `earthquakewarning`(96) | 12 | 预警展示、订阅城市、倒计时/SOS 音、志愿者传感器 |
| 10 | **AI 反诈守护** | `electricrisk`(27) | 6 | 电话/短信/应用/网页/投屏诈骗场景识别与劝阻（模块名"用电风险"是历史命名，与电池无关） |
| 11 | **SIM 卡锁** | `simlock`(25) | 9 | SIM 锁查询/设置/PIN、开机校验 |
| 12 | **通知气泡** | `bubbles`(49) | 2 | 通知气泡化、应用与设置管理 |
| 13 | **权限中心/隐私** | `permcenter`(162)+`permission`(16)+`privacyapps`(10)+`applicationlock`(34)+`blur`(18)+`appmanager`(22)+`devicepermission`(7)+`combinepermission`(4)+`idprovider`(11) | 53 | 逐应用/逐权限授权、自启动、隐私风险检测、隐私空间、应用锁、缩略图模糊、隐私输入、关机密码、跨端权限、OAID 授权、应用管理 |
| 14 | **电量与性能** | `powercenter`(158)+`superpower`(23)+`powerkeeper`(5) | 27 | 省电/性能/均衡/超级省电、耗电排行、异常耗电、充电保护、电池健康 |
| 15 | **存储清理** | `optimizecenter`(40)+`optimizemanage`(29)+`cleanmaster`(6)+`cleaner`(1)+`firstaidkit`(28) | 8 | 垃圾清理、深度清理、大文件、应用瘦身、一键急救 |
| 16 | **游戏加速** | `gamebooster`(280)+`gameturbo`(6)+`gamecenter`(2) | 29 | 游戏识别、性能/触控/画质/HDR/4D/肩键/变声/美颜/快捷回复/悬浮窗/主播模式 |
| 17 | **自动任务** | `autotask`(125) + `powercenter.autotask`(22) | 12 | 条件→动作自动化（22 类条件 × 28 类动作） |
| 18 | **应用预测/端侧 AI** | `apppredict`(25)+`mlkit`(5)+`ai`(1) | 6 | 桌面文件夹推荐、Widget 推荐、分类、端侧训练 |
| 19 | **动态卡片（MAML）** | `maml`(221) | — | 渲染云端/本地下发的 MAML 卡片与动画 |
| 20 | **安全分享** | `zman`(8) | 3 | 分享前隐私脱敏（`ShareActivity`/`PrivacyProtectSettingsActivity`） |
| 21 | **唤醒路径拦截** | `wakepath`(2) | 1 | 链式启动确认页（`ConfirmStartActivity`） |
| 22 | **可信 UI（TEE）** | `tvm`(13) + `securitykey`(4) | 1 | `TvmManagerService` + `ITvmManager` AIDL + nonce/URL 模型；`EncryptionUtils` |
| 23 | **运营商流量业务 SDK** | `sdk.tc`(9) | — | `TcManager`/`TcPlugin`/`TcDirection`/`SmsSender`（套餐校准指令库） |
| 24 | **广告/开屏** | `systemAdSolution`(3)+`international`(4)+`msa`(1) | 1 | 开屏广告 Provider、国际版广告开关、MSA OAID |
| 25 | **基础设施** | `common`(120)+`hybrid.accessory`(49)+`net`(2)+`sec`(1)+`push`(2)+`analytics`(10)+`devicepermission`… | — | 卡片/持久化/网络/埋点/推送/配件互联 |

> 说明：Activity 数来自 `AndroidManifest.xml` 按包分组统计；类数来自 jadx 树文件计数。`com.miui.dock`(20，Dock 悬浮栏)、`com.miui.applicationmanagement`(2)、`com.miui.googlebase`(1，GMS 设置)、`com.miui.appcompatibility`(1) 属小模块，见 §4.6。

### 4.2 体检与杀毒（securityscan / antivirus）
**一键体检（观测）**：入口 `MainEntryActivity → MainActivity`，UI 由 `ui/MainContentFrame*` 五种形态（`MainContentFrame`/`…Fold`/`…Lite`/`…Pad`/`ScanContentFramePad`）实现同一 `wf.a` 接口；扫描引擎在 `securityscan/scanner/`：
- `k.java`(35 KB)/`m.java`(21 KB)/`n.java`(15 KB)/`o.java`(29 KB) 负责扫描调度与并发；`ScoreManager.java`(17 KB) 负责**评分与分数区间**；`CacheCheckManager.java` 负责调用外部清理引擎；`IncrementalScanObserver.java` 做增量扫描。
- 检查项**完全数据驱动**：`assets/scanitem.json` v8 → `manual` 28 组 + `system` 8 组，每项 `<key, name=模型类, version=[alpha,development,stable], score>`。
  - `manual` 组（28）：`VIRUS`、`CLEANER_SHORTCUT`、`DARK_MODEL`、`NETWORK_SWITCH+VIRUS_CLOUDSCAN+CLEANER_CLOUDSCAN+GARBAGE_LIB+FLOW_NOTIFICATION+TELECOM_OPERAOTR+AUTO_UPDATE`、`GARBAGE_CLEAN`、`MI_SIM_LOCK`、`DEFAULT_LAUNCHER/DIAL/MESSAGING/BROWSER/CAMERA/GALLERY/MUSIC/EMAIL`、`DATA_FLOW`、`KEY_SYNC`、`PAYMENT_RISK`、`SAFE_MODE`、`FIND_DEVICE`、`FLOW_VERIFY`、`FLOW_VERIFY_FAILED`、`CLOUD_SERVICE`、`MIFI_INSURANCE`、`MIUI_UPDATE`、`FIRST_AID_KIT`、`PERMISSION_ROOT`、`APP_UPDATE`、`PERMANENT_NOTIFICATIONBAR`、`FLOW_PURCHASE`、`CLOUD_SPACE`、`FLOW_RANK`、`MI_INPUTMETHOD`、`CONSUME_POWER_RANK`、`APP_LOCK`、`SHUT_DOWN_PASSWORD`。
  - `system` 组（8）：`VIRUS_LIB`、`SYSTEM_AUTO_DOWNLOAD`、`INSTALL_MONITOR`、`DEV_MODE`、`USB`、`MMS`、`CHAIN_START`、`GMS_OFF`。
  - 每项对应 `com.miui.securityscan.model.system.*` / `model.manualitem.*`（`model/` 共 62 类）。
- 结果页 = 云端卡片：`ScanResultTopCardModel`/`ScanResultBottomCardModelNew` + `adv.sec.miui.com/info/layout`（`antivirus/result/f.java:284`）。

**病毒查杀（观测）**：`antivirus/` 下 `service/GuardService`（常驻守护，`flash.sec.miui.com/game/union` 也在其中）、`service/VirusAutoUpdateJobService`（病毒库定时更新）、`result/*`（结果页与广告卡）、`ui/AntivirusAnimFrameLayout`（扫描动画 `R.raw.antivirus_animation1/2`）、`SettingsActivity`。本地/云端判定与 `com.miui.guardprovider`（外部 APK，含腾讯 TMS/AVL 引擎）协作：`content://guard` + 权限 `com.miui.guardprovider.permission.Guard_Provider`（本 APK 只有 8 个 GuardProvider 客户端类，**不含引擎**）。

### 4.3 骚扰拦截与反诈（antispam / antifraud / riskcontrol / luckymoney / policeassist）
**骚扰拦截（观测）**：
- 15 个 Activity：`antispam_call_intercept_settings`、`antispam_msg_intercept_settings(_v12)`、`antispam_mark_number_settings`、黑白名单、关键词、拦截记录等（`res/xml/antispam_*.xml`）。
- 规则引擎：`assets/pattern`（v1.2，`lj/c.java:22` 逐行读入）定义短信"重要性/钓鱼/发票/汇款/订阅/验证码/话费/官方号码"等特征；分词用 `assets/dict`（jieba）+ 拼音 `pinyinindex.idf`；敏感词由 **native** `liblib-riskcontrol.so` 的 `SensitiveWordDetector`（Trie）+ `assets/censor_source`（AES 加密词库，未解）。
- 数据：`AntiSpamProvider`（authority `com.miui.antispam`，`remote` 进程）、`MessageInfo.db`；备份/同步用 protobuf（`assets/protobuf/antispam.proto`：BlacklistEntry/WhitelistEntry/KeywordEntry/PolicyEntry/SwitchEntry/NotificationEntry/AutoUpdateLibrary/SimsShareSettings/ReportedNumberGuide），`backupAgent=SecurityCenterBackupAgent`。
- 号码能力：`service_num.json`(224) + `white_url.json`(452) + `address.json`(34 省/336 市) + `api.sec.miui.com/common/whiteList/*`；URL 检测 `url-detect.engine.mi.com/url/detect`（`antispam/policy/b.java`）；号码标记上报 `eps.caict.ac.cn`（`com.xiaomi.security.xsof`，Provider `com.miui.riskcontrol` 的 `SensitiveWordDetectProvider` 亦在此链路）。
- 拦截执行：`antispam/service/AntiSpamService`（常驻，`remote` 进程）+ 系统 `com.miui.permission.MANAGE_SMS_INTERCEPT` 权限 + `startInterceptSmsBySender`（见 §5.5）。
- **真正实现的查询只有两个（观测）**：`content://antispam/sms_judge`（3 参）与 `call_judge`（5 参）→ 返回 `MatrixCursor`（`smsBlockType`/`callBlockType`）；而 `url_judge`、`service_num_judge`、`call_transfer_intercept_judge` **只注册未实现**（`AntiSpamProvider.java:904-906` 落 default 返回 null）→ 这三条是"接口占位"。
- **权威号码数据不在本 APK（观测）**：真正的黑白名单/关键字在系统 Provider `miui.provider.ExtraTelephony.Phonelist/Keyword`（`h2/b.java:84-145`）；本地 `AntiSpam` DB（v10，7 张表，建表 SQL 存在 `res/values/strings.xml` 的 `create_table_*`）主要做云同步/上报/双卡/回铃音。
- **备份（观测）**：`SecurityCenterBackupAgent`（`miui.app.backup.FullBackupAgent`）+ Lite protobuf（`assets/protobuf/antispam.proto` 9 个 message 聚合成 `AntiSpam`，每条带 `guid/luid`），`onFullBackup/onDataRestore` 读写。

**反诈与敏感词（观测）**：`antifraud/PayActivityMonitorService`（`:remote`，监听 `PHONE_STATE` 与 `miui.action.mms.SCAN_RISKY_URL`）+ `AntiFraudReceiver`；"转账页监控"目标由 `assets/anti_fraud_pay_activity`(3 组) 定义——命中微信 `RemittanceUI`、支付宝 4 个 `transferapp` Activity、QQ `qwallet`。`riskcontrol/SensitiveWordDetectProvider`（authority `com.miui.riskcontrol`，`com.miui.security.permission.DETECT_TEXT`）**只服务两个包**：`com.android.camera`、`com.miui.mediaeditor`（硬编码白名单，`SensitiveWordDetectProvider.java:24-25`），协议 `call("METHOD_SENSITIVE_WORD_DETECT", 文本, null)` → `Bundle.getBoolean("hasSensitiveWords")`；`System.loadLibrary("lib-riskcontrol")` 后 `setAssetManager(getAssets())` 把整个 assets 交给 native（词表 `assets/censor_source` 由 native 解密，见 §8）。支付安全名单（`safepay_*`）与 `GuardService` 的支付态监控见 §5.2。

**红包助手（观测）**：`luckymoney/`（6 Activity、`provider/LuckyMoneyProvider`、`upgrade/LuckyMoneyHelper`）；**不用无障碍服务**——`LuckyMoneyMonitorService` 绑定**同 APK 内 GameBooster 的通知代理** `com.miui.gamebooster.service.NotificationListener` / `ISecurityCenterNotificationListener.Z2(callback)`（`LuckyMoneyMonitorService.java:73-82,312`）；"自动抢/快开"= 屏幕未锁 + 开关开启 + 前台不在限制名单时直接 `pendingIntent.send()` 触发微信/QQ 通知自带的 contentIntent（`ui/view/PendingIntentRunnable.java:13-23`），锁屏时降级为锁屏卡片+解锁重放；音效 `R.raw.hongbao_arrived.wav`/`lucky_money.mp3`/`one..ten.mp3`；`api.sec.miui.com/redPacket/reportStatus` 上报。

**报警辅助（观测，已按 2026-11-06 复核修正）**：`policeassist/ui/PoliceassistGuideActivity` + `warningcenter.policeassist.PaService`；紧急呼叫时取精确经纬度 → `Geocoder` 反查文字地址 → ①`https://gateway-papa.cloud.weizhi.com/papa/upload`：载荷为 **AES/CBC/PKCS5 加密**（key 硬编码 `HPLvLMJnR6vvuS9VNkpj7DYb`，`PaCommon.java:5`）后再经 **RSA** 处理（`PaUtils.java:97,353,363`），**不是明文**；②`https://eps.caict.ac.cn/eps/collect`（信通院 EPS，SM3 签名，14 字段 `source/caller/called/device/method/type/lng/lat/hAccuracy/vAccuracy/altitude/address/time/extra/sign`）**用途是紧急呼叫定位上报，不是号码标记**；③无本机号时降级发短信到 `106903590009`。第三方域名 ≠ 数据出境，服务器归属未核验。

### 4.4 网络与流量（networkassistant）
- **20 项用户功能**（`ui/NetworkAssistantActivity`，action `miui.intent.action.NETWORKASSISTANT_ENTRANCE`）：流量总览、流量排行、套餐设置、运营商模板、流量校准（短信/网页/手动）、流量提醒与超限断网、日租卡、**联网控制（移动/WLAN/漫游三表）**、新装应用默认策略、后台联网限制、流量节省、热点限额、国际漫游、流量包购买、运营商余额/欠费查询、MIUI VPN（含迅游）、状态栏实时流量、统计异常提示、网络诊断提示、虚拟 SIM。
- **流量统计链路（观测）**：数据源是 `NetworkStatsManager`（`miui/securitycenter/net/MiuiNetworkSessionStats`），按 `Bucket.state==2` 拆前台/后台；实时量用 `TrafficStats` + `libnap.nativeGetUidIfaceStat`；并把 **`Settings.Global.netstats_uid_bucket_duration` 改为 3600000**（`TrafficManageService.java:1273`）以获得小时级粒度。
- **防火墙实现（观测，重要）**：**不用 iptables、不用 netd 直连**，而是反射 `miui.security.SecurityManager.setMiuiFirewallRule(pkg, uid, rule, type)`（`firewall/impl/MiuiNetdFirewall.java:177-202`，新旧双签名兼容），规则 `Init=-1/Allow=0/Restrict=1/Alert=2`，类型 `MOBILE=0/WIFI=1/ROAMING=2/ALL=3/OTHER=4`；拦截回调用系统广播 `miui.intent.action.FIREWALL`；漫游默认 `Restrict`（反向白名单）；规则持久化在加密私有文件 `*_fire_wall_config2.db`、`fire_wall_wifi_config.db`、`fire_wall_roaming_config.db`。
- **套餐校准（观测，已按 2026-11-06 复核修正）**：`com.miui.sdk.tc.TcManager`（`SEC_KEY="A2FscFVdX1+ULfEz/TTPQVNRXE+lzSe2"` 硬编码，`TcManager.java:17`）→ `System.loadLibrary("tcp")`；`libtcp.so` 内置"省份×运营商×品牌 → 短信指令"库（`TcDirection{sendNumber,receiveNumber,direction,cmdType}`，`cmdType` 位掩码 1/2/4）；执行前 `startInterceptSmsBySender()` 让系统**拦截运营商回执短信**。短信上报走 `api.sec.miui.com/v1/traffic/report`，**必须先弹隐私确认框**（`TcSmsReportFragment.tcSmsReportDeclare()` → `OptionTipDialog` 的"上传"按钮，`TcSmsReportFragment.java:416-430`），用户确认才 `reportSms()`；代码中的 `getEncryptPhoneNumber()` **只是用 `*` 遮中间位，属遮罩而非加密，短信原文未脱敏**。
- **流量业务请求字段（观测）**：`api.miui.security.xiaomi.com/netassist/flow/queryflow` 的请求体含 `imsi`、`iccid`、`phonenum`、`zipCode`（城市码）、`spType`（运营商）、`monthused`（本月已用）；**名为 `imei` 的字段实际是 `DeviceUtil.getAndroidId("<固定UUID>")`，即 AndroidID + 固定业务串派生的散列，不是真 IMEI**（`WebApiAccessHelper.java:80` 起多处）——不能按字段名下结论。
- 接口：`api.miui.security.xiaomi.com/netassist/flow/queryflow`、`api.sec.miui.com/v1/traffic/report`、`api-flow-intl.10046.xiaomimobile.com/*`、`net-assistant.10046.xiaomimobile.com/v1/traffic/directives`（native 指令）。

### 4.5 预警与守护（warningcenter / earthquakewarning / electricrisk / monthreport / bubbles / simlock）
- **灾害预警（观测）**：`DisasterDBHelper`（`warning_center_disaster.db`：`area/disaster/subscribe`，CAP 字段）；区域码来自 `api.sec.miui.com/12379/warning/getRegionInfo`（SALT 硬编码），预警数据 **SHA1WithRSA 验签**（`DISASTER_PUBKEY`，验签失败即丢弃）；**唯一推送来源是 MiPush**（`com/miui/push/XiaomiPushReceiver.java`，appId `2882303761517330652`，模块 `12379Warning`/`accurate-warning`/`MijiaWarning`/`earthquakeWarning`/`monthReport`/`wakePath`/`cloudControl`）；订阅=topic `12379W_ + MD5(districtId+UUID) + _ + 等级位掩码`（默认强提醒只收红=8，系统提醒收红|橙=12）；图标 71 张 + 等级位掩码 `1/2/4/8`（蓝/黄/橙/红）；`warning_center_disaster_guide_text` 给出 11 类气象灾害四色防御指南；另有米家联动 `warning-mijia.sec.miui.com`。
- **地震预警（观测）**：`earthquake_warning2.db`（`area/earthquake/subscribe`，含震级/经纬度/震中/烈度/震源深度/预警时间/签名）；数据源 `srv.sec.miui.com/earthquake/warning/{records,signature,supportCityCode}`；**同样经 MiPush 下发并用 `EARTHQUAKE_PUBKEY` 验签**；端侧只做"验签 → `IntensityTransformer` 算本地烈度 → 与用户阈值比较 → 通知/全屏 + 倒计时"；`EARTHQUAKE_WARNING_MAIN` 导出；**志愿者计划**：`/earthquake/volunteer/{register,cancelRegister,countVolunteers,sensor/upload}` 上报本机加速度传感器（`EarthquakeWarningService` 常驻）。预警源在 APK 内只有一处硬编码 `"ICL"` 代号（`EarthquakeWarningManager.java:718`），判定在服务端。
- **"用电风险"模块名不副实（观测，重要纠偏）**：`com.miui.electricrisk`(27 类 / 6 Activity) 与电池无关——全模块 `BatteryManager`/`sysfs`/`EXTRA_TEMPERATURE` **零命中**；它是 **AI 反诈守护**（文案 label `system_anti_fraud_dialog_message`，卡片覆盖电话/短信/应用/网页/屏幕共享四类诈骗场景，`ElectricProtect{Phone,Mms,App,Screen}Activity` 分别对应）。`AiGuardSceneService` **只做场景转交**：向 `com.miui.guardprovider/.aiguard.AiGuardService` 发 `COMMAND(1/2)` + `PHONE_SCAM_SCENE(1-4)` + `VIDEO_SCAM_SCENE(1-2)` + `CAPABILITIES`（`:212,367-370`），微信→3/1、QQ→4/2。真正的模型在 GuardProvider 侧；`AiGuardUtils` 用反射探测支持并 `startService`（`Application.java:788-800`）。`Command.java` 是空注解，不参与指令。**该模块与 MNN 无关**（MNN 属 `mlkit.mobilerec`）。隐私说明 `privacy.mi.com/AIalert/%s`。
- **月报（观测）**：`monthreport/` 4 类 + `MonthReportProvider` + `month_report.db(report_json)`；上报 `data.sec.miui.com/data/upload`、校验 `api.sec.miui.com/data/check`；**但本构建被硬编码关闭**：`ma/c.java:4-6` 的 `a(){return false;}` 使 `ma.c.a()` 恒 false → 上传永不发车（若可达则为 Base64 报文，appId `5fdd8678-…bba7`）。
- **气泡（观测）**：`bubbles/` 49 类，**自研悬浮窗**（`WindowManager.LayoutParams(-1,-1,2002,16777256,-3)`，type=2002 `TYPE_SYSTEM_ALERT`，`BubbleController.java:141-166`；**0 处 `BubbleMetadata`**）+ `BubblesNotificationListenerService`（通知监听）+ `BubblesService`/`BubbleRemoteService` + `:ui` 进程的两个设置 Activity；图片缓存 `pic_db.db(notification_table,pic_table)`。
- **SIM 锁（观测）**：`simlock/` 25 类，`bootaware` 进程 + `directBootAware`（解锁前即运行），9 个 Activity，`BootCompleteReceiver`；**PIN 可逆加密落盘**：`/data/user_de/0/<pkg>/files/encryped_pin.pwd`（IV‖AES-GCM 密文，JSON `iccid→{pin,…}`），Keystore 别名 `key_pin_alias`（`simlock/a.java:164-226`，**未设 `setUserAuthenticationRequired`**）；验 PIN/PUK 用反射 `ITelephony.supplyPinReportResultForSubscriber`/`supplyPukReportResultForSubscriber`；**该模块无任何云端接口**。

### 4.6 权限/隐私/应用管理（permcenter 等）
- **架构是"三体"（观测）**：①权限管家 UI（本 APK 的 `permcenter`+`permission` SDK，115 个 `PERM_ID_*`）；②**预装后端 `com.lbe.security.miui`**（`ContentResolver.call("content://[<userId>@]com.lbe.security.miui.permmgr", <methodCode>, …)`，method 码 3/4/5/6/7/8/9/12/15/18）；③系统服务 `miui.security.SecurityManager` + `AppOpsManager`（MIUI 私有 op `10000–10053`，如 `AUTO_START=10008`、`BOOT_COMPLETED=10007`、`GET_OAID=10037`、`DEVICE_ID=10050`，`MODE_ASK=5` 为 MIUI 扩展）。
- **写入双写（观测）**：先 `setModeNow`(method 9)/`setApplicationPermission`(method 6) 写 permmgr，再用反射 `IPackageManager.grantRuntimePermission/revokeRuntimePermission/updatePermissionFlags` 对齐 AOSP（`g4/b.java:320-417`）。
- **53 个 Activity** 覆盖：逐应用权限编辑（`APP_PERM_EDITOR`）、逐权限应用列表（`PERMISSION_APP_EDITOR_ACTIVITY`）、自启动（`OP_AUTO_START`）、隐私风险检测（`PRIVACY_RISK_DETECTION_ACTIVITY`，8 类检测项）、隐私空间（`MANAGE_PRIVACY_APPS`）、应用锁（`CHECK_ACCESS_CONTROL`/`LOCK_APP`）、缩略图模糊（`PRIVACY_THUMBNAIL_BLUR_SETTING`）、隐私输入模式（`PRIVACY_INPUT_MODE_ACTIVITY`）、关机密码（`SHUT_DOWN_PASSWORD_ACTIVITY`）、跨端权限（`acrossterminal` → `/data/misc/user/<id>/securitycenter/across_terminal.xml`）、OAID 授权（`REQUEST_PERMISSIONS_FOR_OAID`）、组合权限（`REQUEST_PERMISSIONS_COMBINE`）、设备权限（`REQUEST_DEVICE_PERMISSIONS`）、隐身模式（`INVISIBLE_SETTING`）、隐私中心（`PRIVACY_SETTINGS`）、行为记录（`APP_BEHAVIRO_RECORD`）。
- **应用锁（观测）**：密码/图案**不在本 App 落盘**，`SecurityManager.setAccessControlPassword/checkAccessControlPassword/addAccessControlPass` 全在 system_server；`ConfirmAccessControl` 通过 Intent 携带 `android.app.extra.PROTECTED_APP_TOKEN`(IBinder) 与系统联动；忘记密码走 `IXiaomiAccountService`。
- **隐私空间（观测）**：Android 多用户 profile + `SecurityManager` 隐私应用集合；`PrivacyAppsProvider`（authority `com.miui.securitycenter.app.hide`）实际只实现 `call("isPrivacyApp")`；加入时移出 Recents、杀进程、`setNotificationsEnabledForPackage(false)`、广播 `com.miui.gamebooster.PPRIVACYAPP`。
- **缩略图模糊（观测）**：开关写入 `Settings.System.miui_recents_privacy_thumbnail_blur`（**逗号分隔包名串**），由 **`com.miui.home`** 读取并渲染；`com.miui.blur` 是无关的背景虚化 SDK。
- **隐私 CTA SDK（观测）**：`ue` 包，POST `data.sec.miui.com/privacy/{agree,revoke,get,latestVersion}/v1`，`sign=MD5(Base64(key+sortedParams+key))`，key `2dcd9s0c-…`，国际版短路 `-4`，24h 节流，政策落 `files/privacypolicy/<name>/`。
- **上传面（观测，已复核修正）**：`flash.sec.miui.com/user/{cat,ra}` 上传的是 **`PackageManager.getInstalledPackages(0)` 过滤掉 `FLAG_SYSTEM` 后的全部非系统应用**（包名 + 版本号 + 应用名 + uid，`xc/h.java:13`），**没有 launcher/桌面可见性过滤**——即"用户装了什么"而非"桌面上摆了什么"；触发点是**打开/进入隐私风险检测页面后自动起检测任务**，发送前检查总联网开关；该直接链路上没有逐次上传的应用清单二次确认（首次协议与上层告知未经真机核验）。加密：AES/ECB/PKCS5 + GZIP + Base64，key=`SHA1(ConfigManager.getSecurityHttpKey())`，`biz_id="rivacy_risk_check"`（原文拼写缺 r）。
- **应用管理（观测）**：`appmanager` 22 类（批量卸载、清缓存/数据、通知开关、用量统计）、`applicationmanagement/AppManagementService`、`sec/external/InstallProvider`（安装通道）。

### 4.7 电量/性能/清理（powercenter / superpower / optimizecenter / optimizemanage / firstaidkit）
- **不调 governor（观测，纠正常见误判）**：四个模式位 `POWER_SAVE_MODE_OPEN`/`POWER_PERFORMANCE_MODE_OPEN`/`POWER_BALANCED_MODE_OPEN`/`power_supersave_mode_open` 写 `userConfigureStatus∈{enhance,performance,ultimate}` 到 **`content://com.miui.powerkeeper.configure/GlobalFeatureTable`**（`ce/g.java:340-363`），由 powerkeeper 进程落地。
- **唯一权威写入口（观测）**：`PowerSaverProvider.call()` 8 个方法（`changePowerMode`/`changePerformanceMode`/`changeBalancedMode`/`changeSuperPowerMode`/`changeExtremeEnduranceMode`/`cleanMemory`/`showLowBatteryDialog`/`showSuperPowerHome`，`PowerSaverProvider.java:387-432`），三模式互斥。
- **省电 7 动作（观测）**：`be.g` 限帧 60Hz（`DisplayFeatureManager.setScreenEffect(24,fps)` 反射 + `user_refresh_rate`）、`be.c` 关指纹 AOD、`be.h` 关 5 种系统音、`be.i` 关抬腕/双击/通知亮屏、`be.b` 亮度 -5%、`be.a` 自动亮度上限 30%、`be.e` 关振动（线性马达）；另有同步关闭、内存清理周期 600s、定位降级、`setAntHalf(0)`。
- **真 sysfs 写入仅 4 个温度/充电节点（观测）**：`/sys/class/thermal/thermal_message/sconfig`、`/sys/class/qcom-battery/lpd_charging`、`charger_temp`、`board_sensor_temp`；`cpufreq/cpuinfo_max_freq` 只读；`charge_full/charge_full_design/fg1_rm/slave_authentic` 只读。
- **超级省电（观测）**：`superpower` 是**极简桌面 UI**（自绘状态栏/时钟/WiFi/电池/信号/快捷按钮），由 `SUPER_POWER{progress=0..3}` 广播驱动。
- **清理（观测，已按 2026-11-06 复核修正）**：真正引擎在外部 `com.miui.cleanmaster`（AIDL `IGarbageCheck.F3(scanCb)/J(list,cleanupCb)`，`CacheCheckManager.java:103-115`，action `com.miui.cleanmaster.action.CHECK_GARBAGE_CHECK`）；本 APK 内 `parse.db` 提供"目录→包名"映射（1043 行），`optimizecenter`(40) + `optimizemanage`(29) 做清理 UI/结果；`FboJobService` 每 6 h 触发 `FboPackageBean.upLoadPackageList()`，但该方法**只把包列表写进 `Settings.Secure`（`FBO_UPLOAD_LIST`/`FBO_UPLOAD_TIME`，`ta/f.java:36-52`）交给本机 FBO 系统服务，方法名虽叫 upLoad 但并不联网**。
- **急救箱（观测）**：`firstaidkit`(28)，`assets/firstaid_scanitem.json` 5 组（性能 3 / 网络 3 / 操作 5 / 耗电 2 / 其他 3）。
- **阈值集中（观测）**：`gd.c`（约 400 个存取器）→ `content://com.miui.securitycenter.remoteprovider` 的 `callPreference`（跨用户 `110@` 前缀）。

### 4.8 游戏加速（gamebooster / gameturbo）
- **29 Activity**：`GameBoosterRealMainActivity`、`GameBoosterSettingFragment`、`PerformanceSettingsFragment`、`GameModeSettingsActivity`、`BeautySettingsActivity`、`PrivacyCameraGlobalSettingsActivity`、`SelectGameActivity`、`MarketDownloadV2Activity` 等。
- **调频通道 = Joyose（观测）**：`ServiceManager.getService("xiaomi.joyose")` → `IGPUTunerInterface.checkSupportGpuTuner/getProfiles/deleteProfile`、`IGunSightInterface`（画质增强）、`IJoyoseInterface`、`JoyoseManager.getGameMotorAppList()`（4D 振感）、`joyose_game_turbo_api_version` 握手；温控走 `com.miui.powerkeeper.feedbackcontrol.IFeedbackControl`；加速参数 `adv.sec.miui.com/game/speedParams`。
- **数据（观测）**：`gamebooster.db`（`gamebooster_table` 每游戏 20+ 项：`settings_gs/ts/edge/hdr/4d/follow/hot_area/finger/shake/sensitivity/op_stability/touch_mode`、`game_ratio`、`game_gravity`；`quickreply_table`；`game_tags_t`）；游戏识别用 `top_200_games.json`（**607 包名**，可云端更新 `adv.sec.miui.com/game/pkg` 上行本机包名）。
- **变声/美颜（观测）**：`libjni_sound_effect_2_0.so` = WORLD 声码器（`Dio/CheapTrick/D4C/Synthesis2`），宿主 `gamebooster.encoder.SoundSupport`；`BeautyService` 本身只做前台场景判定（三路监听：`ProcessManager.registerActivityChangeListener` 反射 + `TaskStackListener` 反射 + `getRunningTasks` 兜底）→ bind `DockWindowManagerService` → `IGameBoosterWindow.D4(5,…)` 进会话模式，图像处理在 `:ui` 与 `j8/u.java`。
- **支持名单（观测）**：`beauty_light_default_support_list`(31)、`gesture_effect_support_list`(28)、`pickup_support_activity_list(_phone)`(18/19)、`beauty_face_support_activity_list`(4)、`vtb_default_support_list`(324)、`quickreply_default_list`(263)、`xunyou_support`(20)。

### 4.9 自动任务（autotask ×2）与应用预测（apppredict / 端侧 AI）
- **两套并存（观测）**：新版 `com.miui.autotask`（125 类，Kotlin，authority `com.miui.autotask.provider.auto_task(_service)`，`auto_task.db`）与旧版 `com.miui.powercenter.autotask`（22 类，authority `com.miui.powercenter.autotask`，`power_auto_task.db`）。规则引擎为 `{instanceId,typeId,jsonStr}` 插件式，**22 类条件 × 28 类动作**（时间去/应用/位置围栏/WiFi/蓝牙/耳机/热点/会议/静音/NFC/锁屏/充电/电量/通话 → 开 WiFi/蓝牙/NFC/热点/飞行/免打扰/静音/音量/亮度/深色/护眼/字体/显示/旋转/省电/触控/闪烁/手电/驾驶/启动应用…）。
- **端侧 AI（观测）**：`assets/rec_model_20230425.mnn`（MNN FlatBuffers：`sparse_inputs_1..6`+`sparse_inputs_time` → 9×embedding → MaxPool → relu → 5×linear → `linear_9.tmp_1` 二分类）+ `assets/character.json`（40 字符词表）→ `ia/j.java` 用 `MNNNetNative.nativeCreateNetFromFile`/`nativeCreateSession(FORWARD_CPU,4)` 推理；输入 = 前 5 个前台应用包名的**前 30 字符 char-id** + 第 6 路候选 + `HOUR_OF_DAY`；输出 softmax 取 index1 = P(next)。
- **端侧训练（观测）**：`MNNTrainNative.nativeTrainRecModel(...)`；样本 <50 放弃、20% 随机测试集、正负样本 1:1 负采样、训练前后比较 loss，**劣化则删 checkpoint**；门槛 `train_on==1 && 电量≥20% && 距上次≥period`；超参 `epochs=10, lr=1e-4, batch=128`；任务计划 `POST api.sec.miui.com/mnntrain/taskdata`。
- **融合排序（观测）**：MNN + Bayes(`ka.b`/`ga.b`) + PBB(`la.a`) + 备用(`ia.b`/`ia.f`)，`score += 1/(rank+1)`（top-50）；对外 `AiService`（exported，`miui.permission.APP_PREDICT_VERIFY`）、`AppPredictService`（`IPredictNextApp.R2(n)`）、`AppPredictWidget2x4`（exported Widget）。

### 4.10 MAML 动态卡片引擎（maml）
- **能力（观测）**：自研 DSL（`Expression/RootExpression` + `String/Json/Format/DateFunctions`），**24 种 `ActionCommand`**（含 `IfCommand`/`LoopCommand`/`MethodCommand`(反射调用)/`FieldCommand`/`IntentCommand`/`ExternCommand`/`VariableCommand`/`BinderCommand`）；`WebViewScreenElement` 开 `setJavaScriptEnabled(true)` + `addJavascriptInterface(new MamlInterface(),"maml")`；可绑 ContentProvider/广播/传感器/文件。
- **风险边界（观测 + 推断）**：`ScreenElementRoot` 默认 `capability = -1 = CAPABILITY_ALL`（网络+建对象+持久化全开），只有显式 `setCapability()` 才收窄；**但本 APK 内没有 HTTP 资源 loader** —— 网络下发只经 WebView 与 `issueExternCommand` 抛给宿主，远程 zip 由宿主 App（SystemUI/桌面/主题商店）下载后交 `ZipResourceLoader`。云端包签名：`CloudCoder.generateSignature()` = Base64(SHA1(METHOD&path&kv排序&security))，AES/CBC/PKCS5 + **硬编码 IV `0102030405060708`**（`util/net/CloudCoder.java:26-77`）。

### 4.11 小模块（易被忽略但存在）
| 模块 | 类数 | 结论（观测） |
|---|---|---|
| `zman`（安全分享） | 8 | `ShareActivity`/`ShareSettingActivity`/`PrivacyProtectSettingsActivity`/`ZmanProvider`/`ZmanFileProvider`：分享前脱敏（隐藏位置/设备信息等），受 `com.miui.zman.permission.SECURITY_SHARE` 保护 |
| `wakepath`（唤醒路径） | 2 | `ConfirmStartActivity`：链式启动（A 拉起 B）时的"是否允许跳转"确认页；与体检项 `CHAIN_START`、permmgr method 8（`WRITE_WAKE_PATH_WHITE_LIST`）配套 |
| `tvm`（可信 UI） | 13 | `TvmManagerService` + `ITvmManager/ITvmManagerImpl/ITvmManagerCallback/IMiTrustUICallback` + `TvmNonceModel/TvmUrlModel/SessionModel/TvmStatusModel`：TEE 可信界面（支付/密码场景），`assets/tvm_whitelist.json` 定义 4 个应用及其 TUI 子应用哈希；**由系统属性动态启用**（`vendor.xiaomi.trustedvm.version` / `ro.vendor.mitee_tui.support` 任一非空才 `setComponentEnabledSetting(ENABLED)`，`Application.java:660-668`），状态读 `vendor.qvirtmgr.trustedvm.status`(`RUNNING`)，镜像 ≤140509184 B，走 vendor AIDL `IMTService`/`IMiTrustedUIService`；清单默认 `enabled=false`，且 `miui.permission.TVM` 漏写 protectionLevel（=normal） |
| `securitykey` | 4 | `ConfigManager`（`getEncodeKey/getDecodeKey/getHttpKey/getUuidAesKey/getRubbishKey/getNetworkAssistantKey`）+ `EncryptionUtils`：全 App 的密钥分发点，实际密钥由 `libsec-common-utils.so` 提供 |
| `sdk.tc`（运营商 SDK） | 9 | `TcManager`（`SEC_KEY` 硬编码）/`TcPlugin`/`TcDirection`/`DataUsage`/`SmsSender`/`Base64AesUtil`：套餐校准指令库客户端 |
| `hybrid.accessory` | 49 | 配件/快应用式卡片（`api.hybrid.xiaomi.com`），`icondialog` 等 |
| `applicationmanagement` | 2 | `AppManagementService` + `ApplicationManagementActivity`（应用批量管理，`first_v_forbidden_chain_merge_status` SP） |
| `appcompatibility` | 1 | `AppExcepitonTipsActivity`（应用兼容性异常提示） |
| `googlebase` | 1 | `GmsCoreSettings`（GMS 开关入口） |
| `msa` | 1 | `MsaUtils`（MSA/OAID） |
| `international` | 4 | `AntiGlobalAdBean`/`OptimizeGlobalAdBean`/`GlobalAdBaseBean`（国际版广告开关模型） |
| `nativead` | — | 广告 Activity（`AndroidManifest` 中 1 个 `nativead` 包 Activity） |
| `dock` | 20 | Dock 悬浮栏 `DockMonitorService`/`DockSettingsActivity`/`DockAppEditActivity` + `DockWindowManagerService`（游戏/视频悬浮窗宿主） |
| `net` | 2 | `MiuiNetworkPolicyManager`/`MiuiNetworkSessionStats`（系统服务薄封装） |
| `sec` | 1 | `InstallProvider`（authority `com.miui.sec.INSTALL_PACKAGE`，安装通道） |
| `push` | 2 | `XiaomiPushReceiver` + `a`（推送接收） |
| `analytics` | 10 | `AnalyticsUtil`/`StatManager`/`RemoteAnalyticsManager`/`RemoteAnalyticsReceiver` |

## 5. 关键实现逻辑（逐条链路）

### 5.1 一键体检：反射装配 + 5 条并发流水线 + 扣分制评分
1. **装配（观测）**：`ModelFactory.java:159-172` 读 `assets/scanitem.json`，对每项 `Class.forName(name).getConstructor(String.class, Integer.class).newInstance(key, score)` → **反射实例化检查项**；`version` 数组必须包含当前渠道（`x4.a0.h()`）否则跳过。同组内多个候选按"第一个非 SAFE"择优展示（`GroupModel#scan`）。
2. **编排（观测）**：`scanner/k.java:940-956`（TAG 直接叫 `SecurityManager`）一次启动 5 条流水线：
   - `A()` → `scanner/c` 手动项扫描；
   - `x()` → `scanner/m` 自动项/系统应用扫描；
   - `C()` → `scanner/o`（**VirusScanManager**）：若 `key_is_guard_support` 为真走 GuardProvider **增量扫描**，否则走 `pre_scan` + `HOMEPAGE_SCAN`；
   - `B()` → `scanner/d` 内存扫描/优化；
   - `y()` → `CacheCheckManager` 缓存扫描（调外部清理引擎）。
   - 防重入：`k#m()` 置标志 + `interrupt()` 两个线程 + `o.q()` 取消增量扫描。
3. **评分（观测，`ScoreManager.java:167-220`）——扣分制**：
   ```
   deduct = Σ(系统项 DANGER 的 score) + n(内存比) + f(上次扫描距今) + Σ(手动项 DANGER 的 score)
   deduct = clamp(deduct, 0, 100)
   if (存在任何风险 && deduct < 41) deduct = 41     // ★ 有风险至少扣 41
   最终分 = 100 - deduct
   ```
   - `n(mem)`：内存占用 ≥50%→15、≥40%→10、≥30%→8、≥20%→5（300 s 内不重复计）；
   - `f()`：>3 天→15、>默认频率→10、>24 h→8、>12 h→5、>6 h→3；
   - 扣分写回偏好（`x.c0(ctx, deduct)`），并**喂给广告卡片**：`NativeInterstitialAdLayout.a(score)` 按分数阈值切换蓝/黄背景（`MainFragment.java:1695`）。
4. **展示（观测）**：5 种壳（`MainContentFrame`/`…Fold`/`…Lite`/`…Pad`/`ScanContentFramePad`）实现同一 `wf.a` 接口渲染分数。

### 5.2 病毒查杀：本 APK 是"客户端 + 守护进程"，引擎在外部 GuardProvider
- **不在本 APK（观测）**：`assets` 与 `lib` 中**没有任何病毒特征库或引擎库**；`p9/a.java:136-140` 绑定 `com.miui.guardprovider.action.antivirusservice`（package `com.miui.guardprovider`），接口为本 APK 内的 AIDL 桩 `com.miui.guardprovider.aidl.IAntiVirusServer`（`v0/y6` 批量扫文件、`q1/D` 全盘/取消、`r7/X3` 单包查询、`n0/P6` 引擎/库版本、`C2/K/c5/O3` **Wi-Fi 安全检测**）。
- **扫描对象（观测）**：`scanner/o.java:710-769` 遍历已安装**非系统**应用取 `sourceDir` + 通过 `MediaStore.Files` 查外部存储所有 `*.apk`；结果四态 `SAFE/RISK/VIRUS/RISK_APP`（`o2/b.java:64-69`）。
- **守护（观测）**：`GuardService` 反射 `miui.process.ProcessManager.registerActivityChangeListener` 监听前台，命中 `safepay_target_*` 名单即进入"支付安全态"（状态栏 `action_status_safepay` + `SHOW_WARNING_DIALOG`）；并做 **输入法防劫持**（默认输入法为第三方时用 `Settings.Secure.putString(default_input_method, 系统IME)` 切回、原值保存供还原，`GuardService.java:700-735`）、`AppOpsManager.setUserRestriction(24,…)` 限制后台、Wi-Fi 风险检测（写 `key_safepay_wifi_scan_result`）、联运游戏判断（`POST flash.sec.miui.com/game/union`，字段 `pkg`）。
- **病毒库（观测）**：两条通道——GuardProvider 引擎库 + `VirusAutoUpdateJobService`（Job 99，每日）。

### 5.3 骚扰拦截：系统反查 Provider → 21 条策略链 → 位掩码特征 → TFLite 引擎
1. **方向是"系统问 App"（观测）**：来电/短信时系统 Telephony 通过 `ContentResolver.query(content://antispam/{sms_judge,call_judge,url_judge,service_num_judge,call_transfer_intercept_judge})` 问策略；`AntiSpamProvider.java:106-124` → `b2.b#b/e/f` → 遍历 `h2.e` 枚举 **21 条策略**（`h2/e.java:113-159`），命中即 break 返回 `blockType`，`≥3` 时写拦截日志。
2. **21 条策略（观测，按优先级）**：空号码 → 强云白 → 强云黑 → 白名单 → 黑名单 → 模糊白 → 黑前缀 → 黑号段/归属地 → 服务号短信(106) → 关键字白 → 云白 → 全部号码 → 陌生人 → 海外 → 关键字黑 → 云黑 → 云白关键字 → 云黑关键字 → **智能短信过滤（接 TFLite）** → 呼叫转移 → 被举报号码；末了再叠一层 `URLFilterPolicy`。
3. **`assets/pattern` 规则（观测）**：`lj/c.java:42-57` 读（优先 `files/pattern`，可热更新）→ `nj/d#d(String)` 用 `JSONTokener` 宽松解析 `{version, features[]}`；单 feature 支持 `address/body` 的 `pattern`(AND)/`neg_pattern`(排除)/`or_pattern`(OR)/`body_len`/`combi`(组合)；`nj/d#e()` 把命中项 `sms_type` **按位或**成 `featureBits`，再由 `nj/d#b/a` 映射成权重，用来平移 TFLite logits（`lj/e.java:49-64`）。
4. **新一代短信引擎（观测）**：`h2/a`(`SmsEngineHandler`) 解压 `files/resource.zip` → `files/SmsEngineResource/`（带 canonicalPath 路径穿越防护 + 逐 entry 大小校验）→ `da.a.b(dir)` 建模：`v.txt`(128 维 embedding)、`1.bin`(UnlawRecognizer, 阈值 0.4)、`2.bin`(ImportanRecognizer, 阈值 0.6)；`h(sender,body)` 返回 0=不拦/重要、2=拦截。更新接口 `POST api.sec.miui.com/GuardProviderV1/GetResourceZipFile`（`{app_version, ver:"2.1.2"}`），下载包 **MD5 必须等于 `6206c9f17a64246498b52e117d0cff1e`**，落盘后 `call(content://antispam, "initSmsEngine")`。
5. **号码库（观测）**：真正的存储在系统 `ExtraTelephony.Phonelist/Keyword` Provider；应用侧 `assets/service_num.json`(224)、`white_url.json`(452)，可被 `MiuiSettings` 云覆盖；云号码库 `POST api.sec.miui.com/harassIntercept/intercepts`（`{dataVersion}` → `version` + `v[{status:new|updated|deleted,type:1|2,uuid,p,attributes}]`，type1=号码/type2=关键字，`attributes` 两位数字=类别+级别）。
6. **URL 拦截（观测）**：`policy/b.java:172-218` 四步——本地风险域名表 → GuardProvider AVL url 扫描 → 云端检测（原始/跳转后 URL）→ 白名单（`white_url.json` + 云）判定，返回 0 放行 / 1 拦截 / 2 危险提示 / -1 未知。

### 5.4 防火墙：反射 MIUI 私有 SecurityManager，规则落 netd
见 §4.4（`MiuiNetdFirewall.java:177-202`）。要点：不写 iptables，规则经 `miui.security.SecurityManager.setMiuiFirewallRule(pkg,uid,rule,type)` 交 framework；`TYPE=MOBILE0/WIFI1/ROAMING2/ALL3/OTHER4`、`RULE=Init-1/Allow0/Restrict1/Alert2`；拦截回调用 `miui.intent.action.FIREWALL` 广播；漫游默认 `Restrict`；规则落加密文件（`FirewallService.java:635`）。

### 5.5 流量：NetworkStatsManager 聚合 + 系统桶粒度改写 + 运营商短信校准
见 §4.4。两条独立链路：①统计（`NetworkStatsManager.querySummary/queryDetailsForUid`，前台/后台按 `Bucket.state==2` 拆分，`Settings.Global.netstats_uid_bucket_duration→3600000`）；②校准（`TcManager`→`libtcp` 内置指令库→`startInterceptSmsBySender` 拦截运营商回执→解析余额/流量短信）。

### 5.6 权限管理：三体架构 + 双写 + MIUI 私有 AppOps
见 §4.6。要点：
- **无 `IPermissionManager`**（全树零命中）；真实边界 = `content://[<uid>@]com.lbe.security.miui.permmgr`（method 码 3/4/5/6/7/8/9/12/15/18）+ `miui.security.SecurityManager` + `AppOpsManager`。
- **双写**：`setModeNow`(9)/`setApplicationPermission`(6) → 再反射 `IPackageManager.grantRuntimePermission/revokeRuntimePermission/updatePermissionFlags`（`g4/b.java:320-417`）。
- **MIUI 私有 op（`android/app/AppOpsManagerCompat.java`）**：`10000–10053`，含 `AUTO_START=10008`、`BOOT_COMPLETED=10007`、`BACKGROUND_START_ACTIVITY=10021`、`GET_INSTALLED_APPS=10022`、`GET_OAID=10037`、`DEVICE_ID=10050`、`CAN_BE_RECORDED=10041`、`MEDIA_PROJECTION=10042`、`MODE_ASK=5`。
- **设置不落私有 SP**：走 `content://com.miui.securitycenter.remoteprovider` 的 `callPreference`（`r4/a.java:19-158`，实体 `SharedPreferences("remote_provider_preferences")`，前缀 `110@`）。

### 5.7 应用锁 / 隐私空间 / 隐私模糊（能力在系统，本 APK 只做 UI 与编排）
- 应用锁：`SecurityManager.setAccessControlPassword/checkAccessControlPassword/addAccessControlPass`；`ConfirmAccessControl` 用 Intent 里的 `PROTECTED_APP_TOKEN`(IBinder) + `needFinishAccessControl(token)` 联动；忘记密码走 `IXiaomiAccountService`。**本 App 不存密码**。
- 隐私空间：多用户 profile + `SecurityManager` 隐私应用集合；`PrivacyAppsProvider` 仅提供 `call("isPrivacyApp")`；加入时移出 Recents、杀进程、关通知、广播 `PPRIVACYAPP`。
- 缩略图模糊：写 `Settings.System.miui_recents_privacy_thumbnail_blur`（逗号分隔包名），由 `com.miui.home` 渲染；`persist.sys.protect_image` 切遮挡模式。
- 隐身模式：`InvisibleModeService` 反射 `AppOpsManager.setUserRestrictionForUser(op∈{0,1,41,42,27,26}, …, {com.android.camera, com.miui.face, com.miui.voicetrigger}, userId∈{0,999})` + `StatusBarManager.setIconVisibility("stealth")`（指示灯隐身）。

### 5.8 端侧 AI：MNN 二分类 + 端侧训练 + 四模型融合
见 §4.9。关键数值：输入 `[batch,30]` int（包名 char-id）+ `[batch,1]` 小时；`nativeCreateSession(FORWARD_CPU, 4)`；输出 `linear_9.tmp_1` softmax；训练 `epochs=10, lr=1e-4, batch=128`，`<50` 样本放弃，loss 劣化即删 checkpoint；融合 `score += 1/(rank+1)`（top-50）。

### 5.9 省电/性能：只写"模式状态"，不碰 governor
见 §4.7。四个模式位写入 `content://com.miui.powerkeeper.configure/GlobalFeatureTable`；`PowerSaverProvider.call()` 8 个方法为唯一权威入口；省电 7 动作（限帧/关 AOD/关音效/关亮屏/亮度 -5%/自动亮度上限 30%/关振动）；真 sysfs 仅 4 个温度/充电节点。

### 5.10 清理：本 APK 是"规则 + UI"，执行在 cleanmaster
见 §4.7。`parse.db`(1043 行) 提供"目录→包名/名称"，`CacheCheckManager.java:103-115` 通过 AIDL `IGarbageCheck.F3/J` 调外部 cleanmaster；`FboJobService` 仅 6 h 上报包列表。

### 5.11 游戏加速：Joyose 才是调频通道
见 §4.8。`ServiceManager.getService("xiaomi.joyose")` → `IGPUTunerInterface`/`IGunSightInterface`/`JoyoseManager`；温控 `IFeedbackControl`；参数 `adv.sec.miui.com/game/speedParams`；每游戏配置落 `gamebooster.db`。

### 5.12 MAML：脚本引擎 + WebView + 全能力默认
见 §4.10。24 种 ActionCommand（含反射 `MethodCommand`/`IntentCommand`/`ExternCommand`）、`MamlInterface` JS 桥、默认 `CAPABILITY_ALL`；本 APK 无 HTTP loader，远程 zip 由宿主下载；`CloudCoder` 用 AES/CBC/PKCS5 + 硬编码 IV `0102030405060708` 封包。

### 5.13 预警数据链：12379 + RSA 验签 + 位掩码等级 + 志愿者传感器
见 §4.5。灾害：`api.sec.miui.com/12379/warning/getRegionInfo`（SALT 硬编码）→ `warning_center_disaster.db`；预警数据用 `DISASTER_PUBKEY`(RSA-2048) 验签；等级位掩码 `蓝1/黄2/橙4/红8`（`LEVEL={8,4,2,1}`，`getSystemPushLevel` 默认 12=红+橙）；图标 71 张覆盖 25 个 `EventType` 枚举（11A 洪水/内涝/山洪、11B 气象 12 类、11C02 地震、11D 滑坡/泥石流、11E 风暴潮/海浪、11G01 森林火灾/11G05 草原火灾；`fire` 图标由两个 11G 码共用）。地震：`srv.sec.miui.com/earthquake/warning/{records,signature,supportCityCode}` + `/earthquake/volunteer/sensor/upload`（加速度计）。

### 5.14 埋点与广告：同一套签名、同一套卡片
- 埋点：`com.miui.analytics`(StatManager/RemoteAnalyticsManager) + OneTrack（本地 `events/events_cloud/monitor` 队列）→ `data.sec.miui.com/data/upload`。
- 广告：`adv.sec.miui.com/info/layout` 下发卡片 → `CardModelMaker` → `Adv*CardModel` 渲染；开屏由 `SystemAdContentProvider`(authority `com.miui.securitycenter.splashAd`)；`assets/placement_id` = Facebook 广告位；MSA OAID 作标识；`sdkconfig.ad.*` 配置；`data.sec.miui.com/adv/*` 回传。

---

## 6. 内置数据库与数据资产

### 6.1 随包内置的 SQLite（2 个）
| 文件 | SHA-256 | 表 / 行数 | 用途 | 读取者 | 明文 |
|---|---|---|---|---|---|
| `res/raw/parse.db`（ZIP 内名 `res/rKy.db`） | `5b1da601c11b20ea…` | `DIR_PARSE(PATH, PACKAGE_NAME, APP_NAME)` **1043 行 / 884 个包**、`TEMP_DIR(EXCLUDED, PATH, TYPE)` 10 行、`android_metadata` | 应用专属缓存目录 → 包名/中英文名映射；深度清理时判定"某目录属于谁" | `ra/e.java:39-60`（`openRawResource(R.raw.parse)` → `getDatabasePath("parse.db")`）、查询 `g(pkg)`/`f(list)`、**服务端 JSON 可覆写** `c(list,json)`(148) | ✅ 明文 |
| `assets/garbage_clean.db` | `e1d838531cc6f604…` | `t_cache`/`t_ad`/`t_residual`（同 8 列 `pkg_name,dir_path,root_dir,res_zh_tw,res_zh_cn,res_default,advise_del`）；`t_cache` **4 行**，另两表 **0 行** | 垃圾清理规则（骨架 + 4 条样例） | **本 APK 内无读取者**（3 个 dex 与 18 个 so 均无 `garbage_clean` 字符串；`t_cache/t_residual` 亦无） | ❌ 4 行数据全为 hex 混淆：等长前缀重复 + 任意长度 → **流密码/CTR 类密钥流复用**特征，非分组密码 |

`parse.db` 真实样本（`观测`）：
```
/.budingdoyen      iw.avatar            zh_cn=布丁爱生活&zh_tw=&=
/.helix.thumbnails com.real.RealPlayer  zh_cn=Real Playar&zh_tw=&=
/gomarket          cn.goapk.market      zh_cn=安智市场&zh_tw=&=
/.MTXX             com.mt.mtxx.mtxx     zh_cn=美图秀秀&zh_tw=&=
```
`TEMP_DIR` 10 行：`/downloaded_rom`、`/MIUI/debug_log`、`/ramdump`、`/debug_log`、`/diag_logs`、`/data/tombstones`、`/data/log`、`/LOST.DIR`(EXCLUDED=1)、`/data/system/dropbox`(EXCLUDED=1)、`/cache`。

### 6.2 运行时创建的数据库（20 个，`CREATE TABLE` 全量 32 处）
| 数据库 | 表（要点） | 模块 |
|---|---|---|
| `traffic.db` | `traffic(_id, …)` | 网络助手流量统计（`bj/a.java:24`、`networkassistant/provider/DBHelper.java:9`） |
| `_fire_wall_config2.db` / `fire_wall_wifi_config.db` / `fire_wall_roaming_config.db` | —（**不是 SQLite**：私有加密文件，魔数 ASCII `walimini`，整体与 `ConfigManager.getNetworkAssistantEncryptionKey()` 循环 XOR；密钥在 `libsec-common-utils`） | 防火墙：移动/漫游/WLAN 三套规则，双卡各一套（`config/ConfigFile.java:20,59,160-171`） |
| `_datausage_ignore_applist_config.db` | — | 流量统计忽略名单 |
| `warning_center_disaster.db` | `area`、`disaster`（CAP 字段：`identifier/eventType/severity/headline/instruction/province/city/county/location`…）、`subscribe(subscribeLevel)` | 灾害预警（对接国家预警 12379） |
| `earthquake_warning2.db` | `area(province,code,city,support,counties)`、`earthquake(eventID,magnitude,longitude,latitude,epicenter,intensity,depth,warntime,signature…)`、`subscribe` | 地震预警 |
| `battery_history.db` | `history`、`histogram`、`shutdown` | 电量历史/关机记录 |
| `power_auto_task.db` | `autotasks(_id,name,enabled,condition,operation,repeat_type,restore_operation,restore_level)` | 电量自动任务 |
| `auto_task.db` | `auto_task_table`、`task_condition_and_result_table`、`condition_associated_third_apps` | **系统级自动任务**（条件→动作，含第三方 App 联动） |
| `gamebooster.db` | `gamebooster_table`（每游戏 20+ 项调优：`settings_gs/ts/edge/hdr/4d/follow/hot_area/finger/shake/sensitivity/op_stability/touch_mode`、`game_ratio`、`game_gravity`）、`quickreply_table`、`game_tags_t` | 游戏加速 |
| `app_predict_data.db` | `app_predict_table(pkg_name, click_time, wifi_state)` | 应用预测 |
| `month_report.db` | `report_json(eventId,eventTime,pkgName,eventType,version,data,moduleName)` | 月报 |
| `pic_db.db` | `notification_table`、`pic_table` | 通知图片缓存（气泡） |
| `cloud_no_kill_pkg.db` | — | 免清理/免杀白名单 |
| `global_satisfaction_data.db` | `questions_table(id,data,is_valid)` | 满意度问卷 |
| `MessageInfo.db` | — | 短信/消息记录 |
| `parse.db`（运行时副本） | 同 6.1 | 目录解析 |
| `exoplayer_internal.db` | ExoPlayer 缓存/下载索引 | 视频播放（广告/教程） |
| OneTrack：`events` / `events_cloud` / `monitor` | 埋点队列 | 统计 |

### 6.3 明文规则/词表资产（真正干活的"内置库"）
| 资产 | 规模 | 类型/格式 | 读取者 | 用途 |
|---|---|---|---|---|
| `assets/dict` | 3.64 MB，**348,982 词 + 35,224 字表**，`totalFreq=60,101,983`，`minFreq=2` | 自定义二进制（**jieba 分词词典**，HMM `startProb/transProb` 与开源 jieba 逐位一致） | `lj/g.java:32` → `mj/a.h(InputStream)`（`mj/a.java:260-330`） | 中文分词（短信/文本语义判定） |
| `assets/pinyinindex.idf` | 54 KB，**20,902 汉字 → 拼音**，1,336 去重串 | 自定义 `IDF ` v2 表（`cl/a.java`） | `am/a.java:57` → `cl/a.java` | 首字母/多音字检索（应用名、联系人） |
| `assets/pattern` | 5.5 KB，v1.2，6+ 组正则 | UTF-8 **宽松 JSON**（键无引号） | `lj/c.java:22` | 短信重要性/钓鱼识别（尾号、卡号、验证码、兑换码、"发票/汇款/钓鱼/订阅"等） |
| `assets/white_url.json` | **452** 域名/IP | JSON | `m2/e.java:24` | URL 白名单（免拦截） |
| `assets/service_num.json` | **224** 号码 | JSON | `m2/e.java:55` | 客服/服务号码库 |
| `assets/address.json` | 34 省 / 336 市 + 区号 | JSON | `m2/f.java:416,455` | 号码归属地 |
| `assets/safepay_target_package` | **211** 个 `{package,state}` | JSON | 未知 | 支付安全扫描目标 |
| `assets/safepay_target_activity` / `_calling_activity` / `_imm_whitelist` | 7 / 4 / 13 | JSON | 未知 | 支付页面/输入法白名单（防截屏/防键盘记录） |
| `assets/anti_fraud_pay_activity` | 3 组 | JSON | 未知 | 反诈：转账页监控目标 |
| `assets/top_200_games.json` | **607** 包名 | JSON | 未知 | 游戏识别榜 |
| `assets/financial_package_top` / `photography_package_top` | 300 / 300 | JSON | 未知 | 金融/影像类应用分类榜 |
| `assets/privacyapps_top.json` | 4 组（100/20/85/29） | JSON | 未知 | 隐私空间推荐应用 |
| `assets/behavior_record_white.csv` / `global_…` | 57 / 43 行 | 十六进制摘要白名单（16/32/48/64 B） | `mc/d.java:128` | 行为记录白名单（按签名摘要放行，国内外分版） |
| `assets/tvm_whitelist.json` | 4 条 `{package_name, signature, tui_apps[]}` | JSON | `fh/c.java:112` | **TEE/TUI 可信 UI 白名单** |
| `assets/scanitem.json` | v8，28+8 组 | JSON | 体检引擎 | 一键体检项定义（见 §5.1） |
| `assets/firstaid_scanitem.json` | 5 组（性能/网络/操作/耗电/其他） | JSON | 急救箱 | 一键急救项 |
| `assets/xunyou_support` | 20 个游戏名 | 文本行 | 未知 | 迅游加速支持列表 |
| `assets/vtb_default_support_list` | 324 包名 | 文本行 | 未知 | 视频工具箱白名单 |
| `assets/quickreply_default_list` | 263 包名 | JSON | `f7/b.java:299` | 游戏内快捷回复支持列表 |
| `assets/warning_center_disaster_icon` / `_guide_text` | 21 / 11 条 | JSON（带尾逗号） | 未知（本地无引用） | 灾害图标映射 + 四色防御指南全文 |
| `assets/warningcenter/` | **71** 张 png/webp | 图片 | 数据驱动 | 灾害预警图标：`warning_center_disaster_icon` 的 21 个 code 共 **75 条映射，全部命中真实文件**（`fire` 4 张被 `11G01` 森林火灾与 `11G05` 草原火灾共用 → 71 = 75 − 4 重复）；`11B07`(沙尘暴)/`11B14`/`11B16`/`11B22` 有图标但不在 `EventType` 枚举（云端新类型提前备图） |
| `assets/character.json` | 40 条字符→id | JSON | `ea/c.java:21` | MNN 模型字符词表 |
| `assets/rec_model_20230425.mnn` | 811 KB | **MNN FlatBuffers** | `ia/j.java:88` | App 预测/排序（9 embedding + 5 linear + MaxPool，二分类输出） |
| `assets/model` | 680 KB | 自定义二进制（头 `1,0,5,68,58` + float32BE 权重） | **无实际读取者**（仅 `lj/c.java:17` 名字数组 `{"dict","model","pattern","phish"}` 提及） | `未知`（推断：文本分类权重） |
| `assets/censor_source` | 2,688 B，熵 7.933 | 加密二进制 | `liblib-riskcontrol.so` `SensitiveWordDetector::load` | 敏感词 Trie 树词表（**密钥未解**） |
| `assets/rsapubkey` | 419 B | Java 序列化 `java.security.KeyRep`（RSA 公钥） | 未知 | 通信加密/校验 |
| `assets/protobuf/antispam.proto`+`syncroot.proto` | 2.3 KB + 252 B | protobuf IDL | 生成代码 | 骚扰拦截配置的备份/同步（黑/白名单、关键词、策略、开关、通知、自动更新库、SIM 共享、号码标记引导） |
| `assets/maml/**` | 28 文件 | MAML 卡片（`manifest.xml`+png） | MAML 引擎 | 来电/短信拦截卡片、无 SIM 提示卡（明暗主题） |
| `assets/dexopt/baseline.prof(.profm)` | 1.4 KB | ART baseline profile | 系统 | 启动优化 |

### 6.4 内置证书与密钥材料
| 项 | 内容 |
|---|---|
| APK 签名证书 | `CN=MIUI, OU=MIUI, O=Xiaomi, L=Beijing, S=Beijing, C=CN, E=[VENDOR_EMAIL_REMOVED]`，序列号 `E552A8ECB9011B7C`，2011-12-06 → 2039-04-23，RSA，SHA-256 `c9009d01…49025`，**v1+v2+v3 单一签名者，v2/v3 摘要与字节一致** |
| `res/raw/wlan_speed.crt` | `CN=www.miwifi.com` 自签，**RSA-1024**，2020→2030；作为 `miwifi.com` 信任锚 |
| `res/raw/wlan_speed_new.crt` | `OU=MIWIFI ROOT CA`（由 `CN=Xiaomi Root CA` 签发），RSA-2048，2020→**2120**；同时作为信任锚 |
| `DISASTER_PUBKEY`（`DisasterConstants.java`） | RSA-2048 SPKI（base64，硬编码）→ 校验云端预警数据签名 |
| `REQUEST_CITYCODE_SALT` | `7war5230-a1cf-3k79-ec73-75cb82145n5x`（`api.sec.miui.com/12379/warning/getRegionInfo` 请求盐） |
| `libtcp.so` 常量 | 13 段 `WALI_KEY_SEG_1..3` + `PROGUARD_1..10`（base64 拼料，`OHZoOG1zeTJo`/`ZXc5d3FkeA`/`zdGF0LnNlY3JldA`）+ `0102030405060708`（IV 形状） |
| `libsec-common-utils.so` | 6 个 SecretKey 提供者 + 证书指纹 pin `C8:A2:E9:BC:…:2A:B8`（与 APK 实际签名不符 → 另一把证书） |
| `assets/placement_id` | `123=fb:196567740764356_196569107430886`（**Facebook 广告位 ID**） |

---

## 7. 云端接口全景

> 全量 174 条 URL / 75 个域名，映射到引用类见 `findings/url_usage.tsv`。下表按**功能**归类（均为 `观测`）。

### 7.1 安全中心自有域
| 域名 | 路径（要点） | 用途 | 引用 |
|---|---|---|---|
| `adv.sec.miui.com` | `/info/layout` | **首页/结果页卡片布局（含广告/活动/资讯）** | `antivirus/result/f.java:284`、`c0.java:263`、`optimizemanage/optimizeresult/e.java:244` |
| | `/info/videoAdv`、`/user/ac`、`/info/signinXunyou` | 视频广告、用户画像、迅游签到 | — |
| | `/game/pkg`、`/game/popGames`、`/game2/gameAdv`、`/game/fast_reply`、`/game/speedParams`、`/game/upgrade/pkginfo` | 游戏中心推广/加速参数 | `a8/k0.java`、`a8/k1.java`、`gamebooster/service/w.java` |
| | `/gameTurbo/list/leisureGame`、`/gameTurbo/gcgt/tab/feed`、`/gameTurbo/gcgt/tab/page-detail`、`/gameTurbo/migamegpt/game-ai/gt-setting-info` | 游戏 Turbo 内容流 + **游戏 AI 设置建议** | `c9/d.java`、`c9/h.java`、`gamebooster/aihelper/f.java` |
| `data.sec.miui.com` | `/data/upload`、`/data/check`、`/adv/tail`、`/adv/game_ad/report`、`/privacy/{get,agree,revoke,latestVersion}/v1` | 埋点上传、月报、广告回传、**隐私同意/撤回** | `monthreport/c.java`、`gamebooster/utils/c.java` |
| `api.sec.miui.com` | `/v1/traffic/report`、`/common/whiteList/{listByModule,trafficChargeNum}` | 流量上报、白名单/计费号码 | `networkassistant/webapi/Constants.java` |
| | `/redPacket/reportStatus` | 红包助手状态上报 | `luckymoney/config/Constants.java` |
| | `/LBS/geoconv/admin` | 坐标纠偏 | `earthquakewarning/Constants.java` |
| | `/mnntrain/taskdata` | **端侧训练任务数据** | — |
| | `/earthquake/volunteer/{register,cancelRegister,countVolunteers,sensor/upload}`、`/universalWhitelist/checkUserId` | **地震预警志愿者（含传感器数据上传）** | `earthquakewarning/Constants.java` |
| | `/12379/warning/getRegionInfo` | **国家预警信息发布系统(12379) 区域码** | `DisasterConstants.java` |
| | `/GuardProviderV1/GetResourceZipFile` | 与 GuardProvider 交换资源包 | — |
| | `/res/docs/disclaimer/av/privacy` | 杀毒隐私协议 | `antivirus/activity/MainActivity.java` |
| `srv.sec.miui.com` | `/earthquake/warning/{records,signature,supportCityCode}`、`/data/adb` | 地震记录/签名/支持城市；ADB 安装校验 | `AdbInstallVerifyActivity.java` |
| `flash.sec.miui.com` | `/gc/dir`、`/game/union` | 云控目录、游戏联盟 | `ac/c.java`、`GuardService.java` |
| `api.miui.security.xiaomi.com` | `/netassist/flow/queryflow`、`/netassist/floworderunity/supportfloworder`、`/game/{queryXunYouUserInfo,xunYouReport,queryXunYouActivityList}`、`/views/netassist/productlist.html` | 流量套餐查询/订购、迅游账号与上报 | `networkassistant/webapi/*`、`gamebooster/ui/BoosterFragment.java` |
| `embed.sec.miui.com` | `/earthquake-early-warning(/dsclaimer)` | 地震预警 H5 说明页 | `earthquakewarning/Constants.java` |
| `warning-mijia.sec.miui.com` | — | 米家联动预警 | — |
| `sd-gamespace.sec.miui.com`、`game.sec.miui.com/tencent/award/receive` | — | 游戏空间/腾讯奖励领取 | — |
| `framework.sec.miui.com`、`sec-cdn.static.xiaomi.net` | — | 框架接口、静态资源 CDN | — |
| `url-detect.engine.mi.com/url/detect` | — | **URL 安全检测（钓鱼拦截）** | `antispam/policy/b.java` |
| `eps.caict.ac.cn/eps/collect` | — | **号码标记上报至信通院** | `com.xiaomi.security.xsof` |
| `connect.rom.miui.com/generate_204` | — | 连通性探测 | — |
| `pre-api.miui.security.xiaomi.com`、`api.device(.preview).xiaomi.net`、`account.xiaomi.com`、`micloud.*`（含 preview 预览环境） | — | 账户/云服务/调试环境 | — |

### 7.2 生态与运营商域
`mihall.10046.xiaomimobile.com/mimobile/consumption`（话费/流量套餐校准）、`api-flow-intl.10046.xiaomimobile.com/*`（国际版流量商城：产品/订单/隐私协议）、`preview-api-flow-intl…`（预发）、`api-new-misim.10046.xiaomimobile.com/external/isSatisfyYYLLUser`（运营商用户判定）、**`net-assistant.10046.xiaomimobile.com/v1/traffic/directives`（native 指令下发，见 §8）**、`m.mi.com/maplocation/address?location_type=outlet`（小米之家门店）、`api.jr.mi.com/activity/security/`（金融活动）、`app.market.xiaomi.com`（应用市场反馈）、`miwifi.com/cgi-bin/luci/api/xqsystem/{maccel,ma_check}`（**路由器测速/检测**）、`xunyou.mobi/article-*.html`（迅游加速说明）、`privacy.mi.com` / `privacy.miui.com`（隐私政策）、`web.vip.miui.com`（社区帖）、`beian.miit.gov.cn`（备案号）、`sdkconfig.ad.xiaomi.com`/`sdkconfig.ad.intl.xiaomi.com`（广告 SDK 配置）、`monitor-check.ad.xiaomi.com`（广告监测）、`cn.register.xmpush.xiaomi.com` + `resolver.msg.xiaomi.net`（推送/长连接）、`gateway-papa.cloud.weizhi.com`（第三方）、`t14.market.mi-img.com`（图片 CDN）。

---

## 8. Native 层（18 个库，arm64 + v7a 双架构）

> 方法：REA 无法在本机用 Ghidra/objdump 处理 aarch64 ELF（`Windows Ghidra P0` 仅支持 PE），故自建 AArch64 局部反汇编器（ELF 段 + `.dynsym` + `.eh_frame_hdr` 函数表 + ADRP/ADD/BL/B.COND/CBZ/TBZ/MOVZ/MOVK/LDR/STP 解码 + **PLT→导入符号解析**）。详见 `findings/native-libs.md`。

| 库 | 用途 | 关键观测 |
|---|---|---|
| `liblib-riskcontrol.so` | **敏感词审查** | JNI 仅 `setAssetManager`/`hasSensitiveWords`；`SensitiveWordDetector::load/censor` + `TrieTree::insert/find` + 自实现 AES（ECB/CBC/CFB）；`readFileFromAsset` 引用字符串 `censor_source`；存在调试覆盖路径 `/storage/emulated/0/censor_source` |
| `libtcp.so` | **流量助手 native 插件 + 指令通道** | 唯一硬编码端点 `https://net-assistant.10046.xiaomimobile.com/v1/traffic/directives`；`exec_protocol`：`base64_decode` → JSON → 按 `PluginRegister`/`MsgType`/`CmdType`/`DataType`/`CommandList` 分派；能力含短信模板匹配（`TplId/TplContent/MatchedTplIds/SmsFilterNum/SmsSendNum`）、`getSubscriberId`、`setImsi`、`collectUserData`；13 段 `PROGUARD_*` 常量（含中文垃圾短信语料） |
| `libsec-common-utils.so` | **密钥中心 + 调用方门禁** | 6 个 `ConfigManager.get{Encode,Decode,Http,Uuid,Rubbish,NetworkAssistant}SecretKey`；`getSignature()` 反射链 `getPackageManager→getPackageInfo→getPackageName` + `[Landroid/content/pm/Signature;` + `sha256`；包名白名单 4 个；证书指纹 pin `C8:A2:E9:BC:…:2A:B8`（**与实际签名不符**） |
| `libmicontinuity_sdk.so` | **小米互联跨设备通道** | `micont::channel::ChannelImplBySocket::{SendFile,SendBytes,ContinueSend}`、`RtmSendRetErr`、`NInputReader_read/skip`；导入 socket/connect/accept/epoll/recvfrom，**无 SSL_*** |
| `libxcrash.so` + `libxcrash_dumper.so` | 崩溃捕获 | `libxcrash_dumper` **不是 JNI 库**：导入 `__libc_init`、无 `JNI_OnLoad`、Java 侧无 `loadLibrary`，由父进程拉起，用 `ptrace`/`/proc/self/maps`/XZ/`popen`/`regexec` 抓现场；`/data/local/{,bin/,xbin/}su` 为 root 探测 |
| `libMNN.so` / `libmnncore.so` / `libMNN_Express.so` / `libMNNTrain.so` | **端侧推理 + 端侧训练** | `nativeCreateNetFromFile`/`nativeCreateSession(FORWARD_CPU,4,..)`；`nativeTrainModel`(12.9 KB)/`nativeLoadMapFromFile`(17.5 KB)/`nativeGetRecModelPredictScore`/`nativeTrainRecModel` |
| `libtensorflowlite_jni.so` | TFLite 推理 | `org/tensorflow/lite/TensorFlowLite.java:14` 加载；App 内另有 TFLite 侧模型（`assets/model` 未定位到调用者） |
| `libmiffmpeg.so` | FFmpeg | `Lavf58.45.100`；唯一具备通用网络协议栈（`getaddrinfo/socket/connect`）的库；配合 `libfastplayer.so` 做媒体播放 |
| `libjni_sound_effect(_2_0).so` | **WORLD 声码器变声** | 导出 `Dio/CheapTrick/D4C/Synthesis2` 全套；宿主类 `com.miui.gamebooster.encoder.SoundSupport`（由字符串反推，JNI_OnLoad 动态注册） |
| `libjni_load_serinum.so` | 设备唯一 ID | `/dev/otp` + `ioctl`（MTK OTP），GCC 4.9 mtk 编译串 |
| `libnap.so` | 网络统计 | 解析 `/proc/net/xt_qtaguid/stats`（Android 9+ 已废弃，属兼容路径） |
| `libfastplayer.so` | 轻量播放器 | 与 `libmiffmpeg` 配合 |
| `libc++_shared.so` | C++ 运行时 | 标准库 |

**共性**：18/18 stripped + BIND_NOW；`.rodata` 保留 C++ RTTI/源文件路径 → **无加壳**；`TracerPid|frida|xposed|magisk|ro.debuggable|isDebuggerConnected|substrate|gdbserver` **零命中** → **未见反调试/反注入**（有界否定）；除 `libtcp`、`libmiffmpeg` 外无网络能力。

---

## 9. 第三方 SDK 与广告/统计栈（`观测`，类包统计）

| 组件 | 类数 | 说明 |
|---|---|---|
| `com.google.*` | 1,906 | ExoPlayer 2（媒体播放）、Material、Gson 等 |
| `com.xiaomi.onetrack` | 179 | 小米自研埋点框架（本地队列 + 批量上传） |
| `com.xiaomi.continuity` | 167 | 多设备流转/互联 |
| `com.xiaomi.push` / `com.xiaomi.mipush` / `com.miui.push` | 105 / 70 / 2 | 小米推送 |
| `com.miui.maml` + `miuix.*` | 221 | 动态 UI 引擎 |
| `com.milink.sdk` | 56 | MiLink 投屏/互联 |
| `com.market.*`、`com.xiaomi.market` | 46 | 应用市场（跳转/安装） |
| `com.qti.*` | 43 | 高通 IZAT 定位相关 |
| `com.xiaomi.accountsdk` / `passport` / `micloudsdk` | 21 / 7 / 8 | 小米账号与云服务 |
| `com.miui.msa.util.MsaUtils` | 1 | **MSA 移动安全联盟 OAID** 读取 |
| `com.miui.systemAdSolution` | 3 | **开屏广告**方案（`adSwitch.PROVIDER`） |
| `com.miui.hybrid.accessory` | 49 | 配件/快应用式卡片（`api.hybrid.xiaomi.com`） |
| `com.milink`, `airbnb.lottie`(19), `okhttp3`, `dagger`, `androidx.*`, `kotlinx.*` | — | 常规依赖 |
| 广告位 | — | `assets/placement_id` = Facebook `fb:196567740764356_196569107430886`；`AdvInternationalCardModel`/`AntiGlobalAdBean`/`OptimizeGlobalAdBean` 为国际版广告开关模型 |

**广告链路总结**：`SystemAdContentProvider`（authority `com.miui.securitycenter.splashAd`，exported）+ `AdImageView`/`AdDownloadView` + `adv.sec.miui.com` 布局/素材 + `sdkconfig.ad.*` 配置 + `data.sec.miui.com/adv/*` 回传 + MSA OAID + Facebook 位。**首页插屏广告还吃"安全分"**：`MainFragment.f20481n`（`NativeInterstitialAdLayout`）接收 `ScoreManager.j().q()`，并按阈值在 `shape_interstitial_ad_blue`/`shape_interstitial_ad_yellow` 之间切换背景（`NativeInterstitialAdLayout.java:35-46`）——即**体检分数被用于广告展示**。

---

## 10. 安全与隐私评估

### 10.1 暴露面（exported 组件）
- **424 个组件中 `exported=true` 共 194 个**（全部显式声明，无默认导出）；其中 **128 个零权限保护**，`enabled=false` 的 exported 仅 5 个（含 `MainEntryActivity`）→ **当前有效零权限导出 ≈ 127 个**。`观测`
- 多数通过自定义 action 被系统设置/桌面/其他系统应用拉起（`miui.intent.action.OP_AUTO_START`、`ELECTRIC_RISK`、`EARTHQUAKE_WARNING_MAIN`、`PRIVACY_RISK_DETECTION_ACTIVITY`、`OPEN_WIFI_VERIFY`、`miui.intent.action.SECURITY_CENTER`…），第三方应用可直接拉起这些界面（UI 欺骗/信息探测风险，取决于界面自身二次校验）。
- **最该优先验证的零权限导出**（`观测`）：
  - `com.miui.sec.external.InstallProvider`（authority **`com.miui.sec.INSTALL_PACKAGE`**）—— 安装通道
  - `com.miui.securitycenter.provider.DeviceCredentialProvider`（`com.miui.securitycenter.credentialprovider`）—— 设备凭据
  - `com.miui.securitycenter.provider.SecSettingsSearchProvider` —— **唯一 exported + 无权限 + `grantUriPermissions=true`**
  - `com.miui.idprovider.IdProvider`（OAID）、`com.miui.networkassistant.vpn.miui.MiuiVpnManageService`、`com.xiaomi.security.xsof.XSOFProvider` + `MiSafetyDetectService`、`com.miui.warningcenter.provider.WarningCenterProvider`、`com.miui.securitycenter.SystemAdContentProvider`、`com.miui.autotask.provider.AutoTaskServiceProvider`、`com.miui.permcenter.privacycenter.PrivacyCenterProvider`、`com.miui.securityadd.input.InputProvider`
- **两个高危 UI 面**：`permcenter.permissions.SystemAppPermissionDialogActivity`（接 `miui.intent.action.SYSTEM_PERMISSION_DECLARE(_NEW)`/`CTA_DECLARE` + `content:/file:/image/*` → 可伪造系统授权弹窗）、`networkassistant.ui.base.UniversalFragmentActivity`（接 `com.miui.datatraffic:` scheme → Fragment 注入面）。
- **两条权限缺口**：`miui.permission.TVM` 与 `com.miui.securitycenter.permission.PERM_USE_INFO` **漏写 `protectionLevel`**（=normal）；后者保护的 `AppPermissionsUseActivity` 实际防护失效，前者保护的 `TvmManagerService` 因 `enabled=false` 目前不可达。
- 40 个 Provider 中多数运行在 `remote` 进程，authority 覆盖 `antispam`、`autotask`、`earthquake`、`disaster`、`gamebooster`、`networkassistant`、`powercenter`、`warningcenter`、`securitycenter`、`xsof`、`idprovider`、`app.hide`（隐私空间）、`permissions.acrossterminal`、`remoteprovider`。

### 10.2 高危能力（因 system UID 全部可用）
`MASTER_CLEAR`、`REBOOT`、`FORCE_STOP_PACKAGES`、`CLEAR_APP_USER_DATA`、`CLEAR_APP_CACHE`、`DELETE_CACHE_FILES`、`PACKAGE_VERIFICATION_AGENT`、`MANAGE_USB`、`WRITE_SECURE_SETTINGS` 类（`READ_SECURE_SETTINGS`/`READ_SETTINGS`）、`MODIFY_PHONE_STATE`、`READ_SMS`+`SEND_SMS`+`READ_CALL_LOG`+`WRITE_CALL_LOG`、`RECORD_AUDIO`、`READ_CONTACTS`、`GET_ACCOUNTS`/`MANAGE_ACCOUNTS`/`USE_CREDENTIALS`、`CONTROL_VPN`、`SYSTEM_ALERT_WINDOW`、`DISABLE_KEYGUARD`、`BIND_QUICK_SETTINGS_TILE`、`GET_APP_OPS_STATS`/`UPDATE_APP_OPS_STATS`、`PACKAGE_USAGE_STATS`、`BATTERY_STATS`、`ACCESS_WIFI_STATE`+`CHANGE_WIFI_STATE`+`CONFIGURE_WIFI_DISPLAY`、`MANAGE_NETWORK_POLICY`/`MODIFY_NETWORK_ACCOUNTING`、`miui.permission.SHELL`、`miui.permission.FIREWALL`、`com.miui.security.permission.installOtherApp`、`com.android.settings.permission.SWITCH_DEV_MODE` 等。

### 10.3 传输安全
- `network_security_config.xml`：**`<base-config cleartextTrafficPermitted="true">`**（全局允许明文 HTTP，无 `domain-config` 限制）→ 与代码里大量 `http://*.micloud.xiaomi.net` 明文端点一致。
- 但 `miwifi.com`/`www.miwifi.com`/`router.miwifi.com` **额外信任两张私有证书**：`wlan_speed.crt`（**RSA-1024** 自签，`CN=www.miwifi.com`）与 `wlan_speed_new.crt`（`MIWIFI ROOT CA`，有效期到 **2120 年**）。→ 对该域而言，信任锚被替换为厂商 CA，**标准 PKI 校验被绕过**（仅限这 3 个域）。
- 业务请求自带 `sign`（MD5+Base64+APP_KEY）**而非 TLS 证书固定**；即"防篡改靠自研签名"，且 `sign` 用 MD5（弱）。
- **口径限制（2026-11-06 复核补充）**：`cleartextTrafficPermitted=true` 与私有 CA 是**配置事实**，不等于上述接口真的走明文；代码中确有 `http://*.micloud.xiaomi.net` 字面量，但**未证明这些 URL 在联网路径被实际调用**。判定泄露需抓包，不能用配置代替证据。

### 10.4 隐私影响面（按能力归类）
| 能力 | 实现 | 风险点 |
|---|---|---|
| 短信读取/隐藏/分类 | `antispam` + `assets/pattern` + `MessageInfo.db` | 可读取并本地留存短信内容 |
| 号码归属/标记上报 | `xsof` → `api.sec.miui.com`（号码库/标记）；`eps.caict.ac.cn` 走的是**报警辅助的紧急呼叫定位上报**，不是号码标记 | 号码与标记上行；两条链路需分开看 |
| 设备标识 | `libjni_load_serinum`(OTP 唯一 ID)、**广告/业务请求的 `x`(AndroidID)+`oa`(OAID)+`m`(IMEI-MD5)+`deviceId`(SHA1(IMEI+AndroidID+序列号))**、MSA OAID | **单次请求即携带多种可关联标识**；散列不改变可链接性；`ro.miui.restrict_imei` 只抑制 `m`/`deviceId` 中的 IMEI 分量 |
| 输入法监控 | `df.h` 监听 `default_input_method`、`securityadd/input/InputProvider`、`safepay_imm_whitelist` | 输入法切换检测 + 支付键盘白名单 |
| 无障碍 | `AntiMsgAccessibilityService`（gamebooster） | 读取屏幕内容/自动操作 |
| 通知监听 | `BubblesNotificationListenerService` | 读取所有通知 |
| 屏幕/悬浮窗 | `SYSTEM_ALERT_WINDOW`、`DockWindowManagerService`、`FreeformWindowService` | 覆盖其他应用界面 |
| 摄像头/麦克风 | `RECORD_AUDIO`、`beauty`（美颜/变声）、`PrivacyCamera` | 通话中处理音视频 |
| 位置 | `ACCESS_FINE_LOCATION`；报警辅助（AES+RSA 加密后上传第三方 `gateway-papa.cloud.weizhi.com`）；地震志愿者（OAID+经纬度+时间戳+机型+ROM+token+传感器数组，`/earthquake/volunteer/sensor/upload`） | 两条链路都有独立场景开关/志愿者协议与 UI 勾选，**不是开启即上传**；志愿者上报**非匿名**（含 OAID） |
| 剪贴板/截屏 | 隐私模糊 `com.miui.blur`、`PRIVACY_THUMBNAIL_BLUR_SETTING` | 缩略图/通知内容模糊 |
| 应用锁/隐私空间 | `applicationlock`、`privacyapps`（`app.hide`） | 本地加密与伪装入口 |
| 应用行为记录 | `BehaviorWidget`、`behavior_record_white.csv` | 记录应用行为并按签名白名单放行 |

### 10.5 隐私数据流复核（2026-11-06 增补，逐条回源码验证）

> 说明：本节是对 §10.4 的分级化，回答"哪些是证据最强的隐私问题、哪些只是能力存在"。**证据强度 ≠ 违法/违规结论**；服务端如何使用数据、首次协议如何告知，均未核验。

**A. 证据最强（建议按"超出用户直觉的采集"定性）**
| 链路 | 具体内容 | 证据 |
|---|---|---|
| **广告请求的多标识捆绑** | 国内分支对 `adv.sec.miui.com/info/layout` 一次请求同时带：`x`=Android ID、`oa`=OAID（读成功才加）、`m`=**IMEI 的 MD5**、`deviceId`=**SHA1(IMEI+AndroidID+硬件序列号)**、`rbr`=电量%、`ror`=存储剩余比例，外加机型/系统/地区/运营商/语言/网络类型 | `eg/j.java:545-575`（`x`/`oa` 插入）、`com/miui/antivirus/result/f.java:272`（`deviceId`）、`q2/a.java:18-46`（SHA1 组合）、`eg/c.java`（`a()`=AndroidID、`c()`=getDeviceId） |
| 关键点 | **"个性化广告开关"只是被当作请求参数带上（`xe/a.java:19-28` 的 `isPersonalizedAdEnabled`），所查链路没有"关闭即停止拼接标识"的分支**；MD5/SHA-1 不构成匿名化，相同输入恒定输出→可跨请求/跨应用关联 | 同上 |
| 限制 | IMEI 分量受 `ro.miui.restrict_imei(_p)` 控制（`eg/c.java:l()`）；普通请求仍检查总联网开关 → **不能推成"拒绝联网仍偷传"**，也不能证明服务端如何画像 | — |

**B. 证据强（有明确触发条件与目的，但范围/告知可议）**
| 链路 | 内容 | 触发/同意 | 证据 |
|---|---|---|---|
| **隐私风险检测 → 应用清单上传** | `PackageManager.getInstalledPackages(0)` 过滤 `FLAG_SYSTEM` → `{包名, 版本号, 应用名, uid}` → `POST flash.sec.miui.com/user/ra`（`biz_id="rivacy_risk_check"`，AES/ECB+GZIP+Base64） | 进入隐私风险检测页**自动起检测任务**；发送前查总联网开关；**该直接链路无逐次上传的二次确认** | `xc/h.java:13`（无 launcher 过滤）、`ac/a.java:63-76`、`ac/c.java:118-160` |
| 隐私含义 | 暴露已装的医疗/金融/交友/工作类应用 → 画像价值高；云端风险检测可解释功能目的，但"是否需要整份清单"可议 | 首次协议/上层告知未做真机核验 | — |
| **流量业务请求** | `netassist/flow/queryflow` 携带 `imsi`、`iccid`、`phonenum`、`zipCode`、`spType`、`monthused`；**字段 `imei` 实为 AndroidID+固定 UUID 的散列** | 流量查询场景 + 联网开关 | `WebApiAccessHelper.java:80` 起 |
| **校准短信上报** | 上报短信正文、指令、回执号码、省市、运营商 → `api.sec.miui.com/v1/traffic/report`；`getEncryptPhoneNumber()` **只是 `*` 遮罩** | **有明确弹窗**：`TcSmsReportFragment.tcSmsReportDeclare()` → `OptionTipDialog`「上传」确认后才 `reportSms()` | `TcSmsReportFragment.java:416-430` |
| **报警辅助** | 经纬度+文字地址+OAID+可取得的本机号+呼叫号码/时间+定位起止 → `gateway-papa.cloud.weizhi.com/papa/upload`（**AES/CBC + RSA，非明文**）；另有 `eps.caict.ac.cn/eps/collect`（**紧急呼叫定位上报**，SM3 签名） | 报警辅助场景 + 设置开关 + 协议入口 | `PaCommon.java:5,22`、`PaUtils.java:97,353,363` |
| **地震志愿者** | OAID+经纬度+时间戳+机型+ROM+token+传感器数组 → `/earthquake/volunteer/sensor/upload`（并合并公共设备参数，**非匿名**） | 志愿者加入流程 + UI 隐私勾选；≠ 所有开地震预警的用户 | `EarthquakeMonitorDataManager.java:55` 等 |

**C. 能力存在但未证实外传（不应列为"已发生"）**
| 项 | 事实 | 缺口 |
|---|---|---|
| 通知内容访问 | 气泡组件在支持设备上可自行授予通知监听；游戏相关无障碍服务会筛 QQ/微信通知并匹配文本 | **未找到通知正文发往远端的完整链路** |
| 输入法相关 | `df.h` 监听 `default_input_method` 变化，`GuardService` 检测到第三方输入法会切回系统 IME | 监听切换 ≠ 记录按键 |
| 广告配色 | 首页插屏广告按安全分阈值换蓝/黄背景 | 换色 ≠ 安全分已上传 |
| 月报上传 | `ma/c.java` 的 `a()` 恒 `false` → 本构建不发车 | 是否仅本构建如此未知 |
| FBO 包列表 | `FboPackageBean.upLoadPackageList()` 写 `Settings.Secure("FBO_UPLOAD_LIST")` 交本机 FBO 服务 | 方法名 ≠ 联网上传 |
| 明文 HTTP / 自定义 CA | 全局 `cleartextTrafficPermitted=true`；`miwifi.com` 信任私有 CA | 上述接口均走 HTTPS，**不能据此断言明文泄露** |
| 导出组件无权限 | Manifest 上确有 127 个零权限导出 | 部分 Provider 有代码层 `Binder.getCallingUid()`/包名/签名校验（如 `RemoteProvider.java:180-194` 用 `getPackageNameByPid` 只放行本包与 `com.miui.klo.bugreport`）→ **Manifest 无权限 ≠ 无鉴权** |

**D. 结论口径**
- 可以说：**存在多条隐私数据采集与向外发送路径；广告链路的多标识关联、隐私检测页上传非系统应用清单，是证据最强的两处。**
- 不可以说：**"所有数据处理均未经同意"**（多条链路有开关/弹窗/志愿者协议）、**"拒绝联网仍必然上传"**（未见绕过总开关的分支）、**"数据出境"**（仅域名非小米，服务器归属未核验）。
- 仍缺：首次启动协议/隐私政策的实际告知范围、服务端画像用途、以及真机抓包验证。

---

## 11. 未解之谜（明确留白）

| # | 问题 | 已知 | 缺口 |
|---|---|---|---|
| 1 | `assets/censor_source` 加密算法/密钥 | 由 `liblib-riskcontrol` 的 `SensitiveWordDetector::load` 读取，AES + Trie；已排除单字节 XOR、25 组口令派生、deflate/gzip/brotli；**.so 内全部 16/32 B 连续窗口（约 49 万）作 AES-128/256 密钥 + ECB/CBC 的 PKCS5 校验全部失败** | 密钥不是任何连续字节窗口 → 需 Linux+Ghidra/rizin 反汇编 `_ZN21SensitiveWordDetector4loadEPc` 追 `AES::KeyExpansion` 实参，或真机 hook |
| 2 | `assets/garbage_clean.db` 的读者与混淆算法 | 3 dex + 18 so 无 `garbage_clean` 引用；数据呈流密码特征（等长前缀重复 + 任意长度）；`t_ad`/`t_residual` 0 行 | 可能属外部 `com.miui.cleanmaster` 或历史遗留（死资产） |
| 3 | `assets/model`（680 KB）格式/用途 | 头 `1,0,5,68,58` + float32BE 权重；`lj/c.java:17` 的名字数组含 `model`/`phish`，但**全树无任何调用方用 `lj.c.b(ctx,"model")`**，`phish` 资源不存在 | 需找到实际加载者或格式规范 |
| 4 | 病毒查杀引擎与云查杀接口 | 引擎/特征库/云查杀 URL 均**不在本 APK**，通过 AIDL `IAntiVirusServer`（20 方法）与 `content://guard/*` 委托 `com.miui.guardprovider` | 需 GuardProvider APK 才能回答"特征库格式/云查杀协议" |
| 5 | `libsec-common-utils` pin 的证书 `C8:A2:E9:BC:…:2A:B8` 属于谁 | 与本 APK 实际签名（`c9009d01…`）不符 | 需 MIUI 平台证书样本比对 |
| 6 | `libtcp` 的 `WALI_KEY_SEG_*` 拼接顺序 | 13 段常量字面值已解（含中文短信语料） | 需跟踪 `init_weak_string`/`destroy_weak_string` 装配逻辑 |
| 7 | `libmicontinuity_sdk` 的 Rtm 层是否加密 | 导入符号无 `SSL_*` | 需协议逆向或抓包 |
| 8 | `libjni_load_serinum` ioctl 请求号/返回结构 | `/dev/otp` + ioctl（MTK OTP） | 需反编译器或 strace |
| 9 | 云端卡片布局协议完整字段 | 只有请求侧与部分解析（`functionId`/`sign`/广告参数） | 需服务端响应样本（抓包） |
| 10 | `setMiuiFirewallRule` 在 framework→netd 的落地形式 | APK 只发调用与收广播 | 需 `services.jar`/framework 侧 |
| 11 | `com.miui.antifraud.d`(13.9 KB) 判定规则与上报 payload | 只确认入口与转账页名单 | 需展开该类 |
| 12 | `XSOF`（`MiSafetyDetectService`）各 API 语义 | 方法名全混淆（`C3/d8`） | 需读 `hj.*`/`ij.*` 支持类 |
| 13 | 各模块运行时行为（实际拦截率、清理效果、训练是否真的发生） | — | 需真机动态验证（本报告不含） |

---

## 12. 附录

### 12.1 证据产物索引
| 文件 | 内容 |
|---|---|
| `findings/_BASELINE.md` | 共享事实基线（含 3 处后续修正） |
| `_analysis/PRIVACY_REVIEW_20261106.md` | **独立隐私复核报告**（字段/调用链/接收端/开关条件 + 42 处源码引用；本报告 §10.5 即其结论的并入版） |
| `findings/assets-data.md` | 717 行：全部 assets/res.raw/证书/签名 deep dive（含 dict/IDF 完整解码、db 全量行、AES 爆破记录） |
| `findings/native-libs.md` | 18 个 .so 逐库分析 + 6 条未解之谜 |
| `findings/manifest-sdk.md` | 清单/权限/组件/签名/SDK/混淆结构 |
| `findings/core-security.md` | 体检/杀毒/骚扰拦截/反诈/红包/报警辅助 |
| `findings/network-warning.md` | 网络助手/防火墙/流量/地震/灾害/AI 反诈守护（含 55 KB 专项附录 `_parts/`） |
| `findings/privacy-permcenter.md` | 权限中心/隐私空间/应用锁/隐私模糊/OAID |
| `findings/power-gaming.md` | 省电/性能/清理/游戏加速/自动任务/端侧 AI |
| `findings/components_all.tsv` | 424 个组件全表（kind/name/exported/permission/actions/process） |
| `findings/url_usage.tsv` / `jadx_urls.txt` / `dex_urls.txt` / `dex_strings_all.txt` | URL 与字符串全量 |
| `tools/*.js|mjs` | jieba 词典解析、IDF 解析、AArch64 反汇编、AES 爆破、资产指纹等复现脚本 |

### 12.2 关键代码索引（精确定位）
| 主题 | 位置 |
|---|---|
| 进程分流初始化 | `com/miui/securitycenter/Application.java:711`（`onCreate`）、`:683`（`attachBaseContext`） |
| 体检项配置 | `assets/scanitem.json`（v8，28+8 组）→ `com/miui/securityscan/scanner/*`、`ScoreManager.java` |
| 首页/结果页卡片 | `com/miui/common/card/**`（49 个 CardModel） |
| HTTP 与签名 | `eg/j.java`（`NetUtil`）：静态参数 `:129-176`、签名 `m()` `:764-795` |
| 广告参数 | `xe/a.java:19-58`（`isPersonalizedAdEnabled`/`cv`/`ror`/`rbr`） |
| 布局接口 | `antivirus/result/f.java:284`、`c0.java:263,283`、`optimizemanage/optimizeresult/e.java:244` |
| 目录规则库 | `ra/e.java:39-60`（复制 `R.raw.parse`）、`:148`（云端覆写）、`:211/256/267`（查询/拼装） |
| 分词词典 | `lj/g.java:32` → `mj/a.java:260-330` |
| 拼音索引 | `am/a.java:47/57/73` → `cl/a.java`（IDF v2）、`:121-197`（多音字/姓氏覆盖） |
| App 预测模型 | `ia/j.java:88-294`（加载/输入/输出）、`ia/d.java:71-182`（拷贝/编码）、`ea/c.java:21`（词表） |
| 敏感词 Provider | `com/miui/riskcontrol/SensitiveWordDetectProvider.java:23` |
| 密钥中心 | `com/miui/securitykey/ConfigManager.java:42` |
| Native 指令通道 | `com/miui/sdk/tc/TcManager.java:116` |
| 灾害常量 | `com/miui/warningcenter/disasterwarning/DisasterConstants.java`（12379 URL/SALT/RSA 公钥/等级位掩码 1/2/4/8） |
| 地震预警常量 | `com/miui/earthquakewarning/Constants.java` |
| 网络配置 | `res/xml/network_security_config.xml` |
| 签名文件 | `META-INF/PLATFORM.SF`、`PLATFORM.RSA`（CN=MIUI） |
