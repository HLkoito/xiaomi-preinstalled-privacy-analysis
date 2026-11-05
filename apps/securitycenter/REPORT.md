# MIUI 安全中心：隐私数据获取与发送的整理结论

本报告针对 `com.miui.securitycenter`，版本 `10.4.9-250402.0.1`，versionCode `40001049`。样本 SHA-256 为 `fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd`，大小 66,917,411 字节。结论仅适用于该样本，不据此推广到其他版本或全部小米手机。

**基于静态代码分析，该 APK 实现了敏感信息读取、设备关联标识构造和网络发送。其中最值得关注的是广告请求中的多种稳定标识组合，以及进入隐私风险检测后上传非系统应用包名和版本。**

详细调用链、42 个定位引用和纠错见 [REVIEW.md](REVIEW.md)。随附 [证据索引](evidence/index.json) 与 [先前静态核验记录](evidence/verification.json)；40 个完整 Java/XML 精选文件保留原行号。公开包不含设备运行日志或用户真实字段值。

## 数据流及触发条件

| 场景 | 已见采集与发送数据 | 目标 | 触发、门控与证据边界 |
|---|---|---|---|
| 国内版杀毒结果页广告/布局 | Android ID、OAID、条件性电话设备 ID 摘要；电话设备 ID＋Android ID＋序列号的 SHA-1 复合指纹；电量、存储比例及机型/ROM/地区等 | `https://adv.sec.miui.com/info/layout` | 普通请求检查总联网许可；电话设备 ID 受限制属性与 API 成功条件影响。个性化广告开关作为请求字段，与这些设备标识一同组装。见复核 §2 |
| 隐私风险检测 | `getInstalledPackages(0)` 返回后过滤系统应用，序列化 `{包名: 版本号}`，附类型、时间、系统与机型代号等 | `https://flash.sec.miui.com/user/ra` | 页面初始化自动启动检测，发送检查联网开关；枚举范围为所有返回的非系统应用。见 §3 |
| 流量查询 | IMSI、ICCID、手机号、城市/区域编码、运营商、本月已用流量，以及 Android ID 派生摘要 | `https://api.miui.security.xiaomi.com/netassist/flow/queryflow` | 特定流量查询任务且联网允许；`imei` 字段实际为 Android ID 加固定业务串的 MD5。见 §4 |
| 流量校准问题上报 | 指定回执号码的短信正文、校准指令、回执号码、省市、运营商等 | `https://api.sec.miui.com/v1/traffic/report` | UI 有专门上传告知，确认后才提交；存在失败缓存/重试。并非读取全部短信。追加手机号字段做星号遮罩，不保证短信正文中的个人内容脱敏。见 §5 |
| 报警辅助 | 位置与文字地址、OAID、可取得时的手机号、紧急号码、呼叫/定位时间 | `gateway-papa.cloud.weizhi.com/papa/upload`；独立开关控制 `eps.caict.ac.cn/eps/collect`；短信降级通道 | 报警场景、设置/协议流程；网络载荷有 AES，短信敏感载荷先 RSA 加密。EPS 用于紧急呼叫定位。见 §6 |
| 地震志愿者监测 | OAID、经纬度、时间、机型、ROM、非空志愿者 token、传感器数组，且发送时合并公共设备参数 | `https://api.sec.miui.com/earthquake/volunteer/sensor/upload` | 有隐私勾选和加入志愿者流程。见 §7 |

稳定 MD5/SHA-1 的相同输入产生相同摘要，具有跨请求关联价值。表中“目标”是客户端代码中的网络端点。

`q2/a.java` 对设备信息派生的标识使用静态字段作进程内缓存。

## 本地数据访问与统计接入

气泡通知组件在受支持设备上调用系统接口授予通知监听；游戏无障碍服务读取特定 QQ/微信通知文本并作关键词匹配。见复核 §8。

OneTrack 多业务实例及广告事件统计已接入，同时设置联网许可。月报上传条件调用 `ma.c.a()`，该方法在此构建返回 `false`。

`FboPackageBean.upLoadPackageList()` 将包列表写入本地设置并交给本机 FBO 系统服务。

## 原始分析的修正与阅读顺序

以本文件与 [REVIEW.md](REVIEW.md) 为最终整理依据。[SOURCE_REPORT.md](SOURCE_REPORT.md) 和 [archival/](archival/) 保留原始功能分析、资源/native 探索与证据定位。

主要修正如下：

- 隐私检测枚举返回的非系统应用，上传对象中的 `apps` 只序列化包名和版本号。
- FBO 写本地设置并委托本机系统服务。
- 流量 API 的 `imei` 字段来自 Android ID 派生摘要；流量短信号码字段使用星号遮罩；报警短信敏感载荷使用 RSA 加密。
- 地震预警与志愿者监测是不同场景，短信上报和志愿者流程存在具体确认门槛。
- 部分 Provider 有代码层 UID、包名或签名校验。
- 网络安全配置为 miwifi 域及子域设置信任锚，表列主要网络上传端点使用 HTTPS。

## 分析方法

基于静态代码分析，描述实现路径。复核计算 APK 与三个 DEX 的哈希、检查关键 DEX 字符串，并抽查 Java/XML。公开包内的精选反编译文本与原文件哈希一致，保留原行号及上下文。

## 随附分析工具

[tools/README.md](tools/README.md) 说明参数化静态验证、AXML 解码、DEX 字符串统计和 AArch64 局部反汇编工具。
