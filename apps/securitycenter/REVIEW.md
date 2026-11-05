# MIUI 安全中心隐私数据行为：快速独立复核

复核日期：2026-11-06。目标：`com.miui.securitycenter`，`10.4.9-250402.0.1`。

**结论：APK 实现了隐私数据读取、关联标识构造和向服务端发送的路径。其中，广告链路组合多种稳定设备标识、隐私风险检测上传非系统应用清单，是优先关注项。**

本文件为补充复核的脱敏公开版；源码链接指向本目录随附的证据文件。原报告及专项存档中的相冲突结论，以本文件和 REPORT.md 的纠错为准。

## 1. 分析方法

- 阅读总报告、`core-security.md` 的重点链路，以及其他专项中与隐私有关的索引；直接抽查 HTTP、广告、应用枚举、流量查询、短信回传、报警定位、地震志愿者、通知、Provider 等反编译代码。
- 重新计算原始 APK SHA-256：`fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd`，大小 `66,917,411` 字节，与总报告一致。
- 从原 APK ZIP 中读取三个 DEX，确认其 SHA-256 与 `apk_full` 的三个解包副本逐一相同；15 个关键端点/字段/方法字符串均能在 APK 内 DEX 字符串表找到。
- 保存抽查 Java 与 XML 文件的哈希，便于识别对应版本。基于静态代码分析，描述实现路径。

复现脚本：[privacy_review_verify.cjs](tools/privacy_review_verify.cjs)。机器可读证据：[privacy-review-evidence-20261106.json](evidence/verification.json)。

表中的优先级按数据敏感度和分析价值排列。

## 2. 最值得关注：广告请求组合多种设备标识

### 2.1 数据来源、加工与发送链路

在国内构建的杀毒结果页布局请求中，可串起以下路径：

```text
antivirus/result/f.l()
  添加 deviceId、ua、渠道等
  → xe.a.f()
      添加 isPersonalizedAdEnabled、cv、ror、rbr
  → eg.j.B() → eg.j.C(..., false, true, ...)
      检查联网允许开关
      → eg.j.b() 合并公共参数
      → HttpURLConnection POST
  → https://adv.sec.miui.com/info/layout
```

| 字段 | 实际数据来源 | 隐私含义与限制 |
|---|---|---|
| `x` | `Settings.Secure.android_id`，经 `eg.c.a()` | 国内分支中直接加入的 Android ID；不是只传 IMEI 哈希 |
| `oa` | `ef.w.a()` → `y4.a.a()` → 反射 `IdProviderImpl.getOAID()` | OAID；接口不可用或返回空时会为空 |
| `m` | `eg.c.c()` 的 `TelephonyManager.getDeviceId()` → MD5 → 字符串编码 | 条件性电话设备 ID 摘要 |
| `deviceId` | `q2.a.a()` 对 `getDeviceId() + Android ID + 硬件序列号` 做 SHA-1 | 设备信息派生的复合指纹，由静态字段作进程内缓存 |
| `rbr` | `BatteryManager.getIntProperty(4)` | 当前电量百分比 |
| `ror` | 外部存储可用字节数 / 主存储容量，换算成百分比 | 存储剩余比例，读取失败会为 `-1` |
| 其他公共参数 | 机型、ROM/系统版本、地区、运营商、语言、网络类型等 | 可与稳定标识一起关联请求 |

精确证据：[广告布局请求 f.java:261][ad-request]、[公共参数 j.java:542][net-params]、[静态参数 j.java:129][net-static]、[复合指纹 q2/a.java:18][fingerprint]、[广告状态 xe/a.java:19][ad-state]、[设备 ID eg/c.java:54][device-id]、[OAID 反射 y4/a.java:28][oaid]。

### 2.2 开关、条件与实际风险

- `m` 受 `ro.miui.restrict_imei` / `ro.miui.restrict_imei_p` 控制；限制开启时为空。国际版公共参数另走广告 ID 分支。
- 普通请求 `eg.j.C()` 检查 `x4.l1.b()`。其读取系统联网允许属性，默认参数为 `false`；国际版额外检查隐私状态。
- `xe.a.a()` 读取个性化广告开关，并将布尔值与设备标识一同加入请求。`xe.a` → `eg.j.b()` 按上述条件组装 `x`、`oa`、`m` 和 `deviceId`。
- 稳定哈希的相同输入产生相同摘要。单次请求组合 OAID、Android ID 和序列号派生标识，具有跨请求关联价值。
- `q2.a.b()` 正常分支返回哈希，异常分支直接返回输入字符串。

**判断：商业广告/内容布局请求组合多种设备标识与设备状态，形成较强的设备关联能力。**

## 3. 隐私风险检测上传非系统应用包名与版本

可完整串起以下链路：

```text
PrivacyRiskDetectionActivity.onCreate() → Y0()
  → new ub.d(...).execute()
  → xc.h.a(context)
      PackageManager.getInstalledPackages(0)
      过滤掉 applicationInfo.flags & FLAG_SYSTEM != 0 的包
  → ac.c.d(list)
      apps = { packageName: versionCode, ... }
      附 type、timestamp、os、device、biz_id
  → ac.c.c(payload) → ac.c.g()
  → POST https://flash.sec.miui.com/user/ra
```

证据：[页面入口:1114][risk-entry]、[任务启动:920][risk-start]、[ub/d.java:46][risk-task]、[应用枚举 xc/h.java:13][app-list]、[序列化 ac/c.java:62][app-payload]、[发送 ac/c.java:90][app-send]。

`xc.h.a()` 判断应用的系统标记，枚举范围是 **PackageManager 在当前环境返回的所有非系统应用**，包括没有桌面图标的应用。

本地枚举对象还含应用名称、UID、图标 URI；`ac.c.d()` 的 `apps` 序列化包名和版本号，附带的 `device` 字段是机型代号。

此请求检查 `ef.x.z()` 联网开关，页面 `onCreate()` 自动启动检测任务。

应用清单能够反映安装的金融、医疗、交友、宗教、工作等应用类别，具有用户画像价值；该链路将应用清单用于云端风险匹配。

同类 `/user/cat` 构造函数也存在；本节展示 `/user/ra` 的完整调用链。

## 4. 流量业务请求含 SIM 和手机号数据

`TrafficRelatedPreFragment.GetVirtualOperatorTask.doInBackground()` 在联网允许后调用 `queryDataUsage()`，传入 SIM 对象的 IMSI、城市、手机号、运营商、本月使用流量与 ICCID。请求对象字段为：

```json
{
  "imei": "MD5(Android ID + '-' + 固定业务字符串)",
  "imsi": "SIM IMSI",
  "zipCode": "城市/区域编码",
  "phonenum": "手机号",
  "spType": "运营商",
  "monthused": "本月已用流量",
  "iccid": "SIM ICCID"
}
```

目标：`https://api.miui.security.xiaomi.com/netassist/flow/queryflow`。

证据：[调用端 TrafficRelatedPreFragment.java:116][sim-caller]、[字段 WebApiAccessHelper.java:67][sim-payload]、[标识实际来源 DeviceUtil.java:84][androidid-hash]。

**字段实际来源：`imei` 填入带固定业务字符串的 Android ID MD5。** IMSI、ICCID 与手机号具有很强的订户关联性。上表列出加密/封装前请求对象的字段。

这条流量查询路径检查联网开关，并将订户资料加入请求对象。

## 5. 流量校准短信正文会上传，但已有专门确认流程

`TcSmsReportService` 只接收与配置的回执号码相符的短信，然后读取 `getDisplayMessageBody()`；此处不是扫描全部短信收件箱。

上传数据包含校准请求号码、指令、回执号码、收集到的短信正文、省市、运营商、品牌、类型等，目标：

`https://api.sec.miui.com/v1/traffic/report`

其 `report()` 把短信列表放进 JSON 数组，并追加本机号码处理结果。`getEncryptPhoneNumber()` 把手机号中间数字替换为 `*`，属于号码遮罩；短信正文则直接进入上传列表。

证据：[短信读取 TcSmsReportService.java:54][sms-read]、[提交数据 :178][sms-body]、[字段与目标 WebApiAccessHelper.java:83][sms-payload]。

`TcSmsReportFragment.tcSmsReportDeclare()` 弹出 `privacy_prompt_tc_sms_report_message`，只有回调 `onOptionUpdated(true)` 才调用 `reportSms()`。证据：[专门上传告知:416][sms-consent]。失败数据另有本地缓存和重试路径。

**判断：该链路在专门用户确认后上传运营商校准短信内容。**

## 6. 报警辅助向第三方发送位置、号码和时间

`PaService.TransDataTask` 取得坐标，通过 `Geocoder` 反查文字地址，并按 SIM/本机号码状态选择网络或短信渠道。

| 接收端 | 在请求中明确出现的数据 | 条件与边界 |
|---|---|---|
| `gateway-papa.cloud.weizhi.com/papa/upload` | OAID（名为 `deviceId`）、可取得时的本机手机号、紧急号码、区号、呼叫时间、经纬度、文字地址、定位起止时间 | 报警辅助场景；请求体有 AES 加密；普通网络发送有联网允许检查 |
| `eps.caict.ac.cn/eps/collect` | SM3 后的主叫号码、紧急被叫号码、位置、地址、精度/高度（条件性）、时间、设备机型等 | 国内版且 EPS 开关打开；该开关读取默认值为 0 |
| `106903590009` 短信 | 紧急号码、区号、经纬度、时间、OAID 等的二进制载荷 | 无本机号码/网络失败等降级；敏感载荷先做 RSA 加密 |

证据：[位置与通道选择 PaService.java:182][police-flow]、[网络对象 PaUtils.java:104][police-fields]、[EPS 字段 EPSManager.java:49][eps-fields]、[EPS 开关 :82][eps-toggle]、[端点 PaCommon.java:6][police-endpoints]、[短信构造 PaUtils.java:82][police-sms]。

`buildMessageData()` 对含坐标等内容的 117 字节数据块调用 `rsaEncrypt()`，然后采用 UTF-16 字符包装。EPSManager 的字段用于紧急呼叫定位上报。

该模块有设置开关、协议/隐私入口和撤回流程。`PaSettingActivity.showRevokeDialog()` 在撤回确认按钮上设置 10 秒倒计时禁用。

**判断：报警辅助实现了向第三方共享位置和身份关联数据的路径。**

## 7. 地震志愿者上传 OAID、位置、时间和传感器序列

字段可直接从 `EarthquakeMonitorDataManager.threadPoolRun()` 确认：

- `oaid`、`lng`、`lat`、`timestamp`、`model`、`rom`；
- 非空时的志愿者 `token`；
- `data` 中的浮点传感器数组。

目标：`https://api.sec.miui.com/earthquake/volunteer/sensor/upload`。

证据：[字段组装:51][quake-fields]、[发送时还合并公共参数 EarthquakeMonitorUtils.java:54][quake-send]、[OAID/位置数据来源 EarthquakeWarningService.java:113][quake-source]。

`EarthquakeMonitorUtils.uploadData()` 调用 `eg.j.b()` 加入公共参数，按前述条件合并 Android ID、OAID 和设备 ID 摘要等。地震预警服务采用 10 分钟定位刷新节流逻辑。

明确的用户交互证据：`EarthquakeWarningMonitorOpenFragment` 中，只有隐私复选框勾选后才允许“加入”，随后注册志愿者或启动 `ACTION_JOIN_VOLUNTEER`。证据：[加入条件:207][quake-consent]。

**判断：加入志愿者监测的流程实现了可关联设备的位置与传感器数据上传。**

## 8. 本地内容访问与埋点

### 8.1 通知监听可以由系统能力自行授权

`BubblesNotificationListenerService.configNotificationListener()` 在设备支持气泡通知、且尚未取得访问时，调用：

```java
notificationManager.setNotificationListenerAccessGranted(componentName, true);
```

随后按支持状态启用/禁用组件。Manifest 中此服务初始 `enabled=false`。

收到的 `StatusBarNotification` 转发给气泡 UI Binder。证据：[授权配置:16][notification-grant]、[对象转发 BubblesNotificationHelper.java:51][notification-forward]。

**受支持设备上，该组件通过系统接口配置通知访问权限。**

### 8.2 无障碍服务实际读取 QQ/微信通知文本

`AntiMsgAccessibilityService.onAccessibilityEvent()` 只处理 QQ/微信的事件类型 64；`l(Notification)` 读取 `extras["android.text"]` 或 `tickerText`，做关键词匹配。证据：[事件筛选:401][accessibility-events]、[文本读取:253][notification-text]。


### 8.3 OneTrack 埋点确实接入，但网络开关也存在

`StatManager` 初始化多个业务 OneTrack 实例，包含广告事件 App ID，并调用 `OneTrack.setAccessNetworkEnable(..., l1.b())` 和 `setUseSystemNetTrafficOnly()`。证据：[StatManager.java:41][analytics]。

这些实例接入了使用行为统计与广告事件管线。

## 9. 补充实现与字段修正

| 项目 | 代码事实 |
|---|---|
| 隐私检测应用范围 | 枚举 `getInstalledPackages(0)` 后过滤系统应用；`apps` 序列化包名和版本 |
| FBO 包列表 | 写入本地设置，`ta.d.e()` 反射调用本机 `miui.fbo.FboManager.FBO_new_trigger()` |
| 流量 API 的 `imei` 字段 | `DeviceUtil.getAndroidId()` 计算 Android ID 加固定业务字符串的 MD5 |
| 公共设备 ID 摘要 | 国内版、限制属性关闭且设备 API 成功等条件成立时组装；国际版使用另一分支 |
| 报警短信 | 敏感数据块先经 RSA 加密 |
| EPSManager | 用于紧急呼叫定位上报 |
| DeviceCredentialProvider | `call()` 检查调用者身份、包名及签名白名单 |
| PrivacyCenterProvider | 对部分方法作 UID 检查 |
| PrivacyAppsProvider | Manifest 配置 `SYSTEM_PERMISSION_DECLARE`，权限级别为 `signatureOrSystem` |
| 网络安全配置 | 对 miwifi 域及子域设置信任锚，主要上传端点采用 HTTPS |
| 月报门控 | 上传条件包含 `ma.c.a()`，该方法在本构建返回 `false` |

补充证据：[FBO 委托 ta/d.java:93][fbo]、[设备凭据鉴权:102][credential]、[隐私 Provider 方法鉴权:38][privacy-provider]、[月报条件:149][month-gate]、[恒假开关 ma/c.java:5][month-false]、[Manifest][manifest]、[网络安全配置][network-xml]。

[ad-request]: evidence/sources/com/miui/antivirus/result/f.java#L261
[net-params]: evidence/sources/eg/j.java#L542
[net-static]: evidence/sources/eg/j.java#L129
[fingerprint]: evidence/sources/q2/a.java#L18
[ad-state]: evidence/sources/xe/a.java#L19
[device-id]: evidence/sources/eg/c.java#L54
[oaid]: evidence/sources/y4/a.java#L28
[risk-entry]: evidence/sources/com/miui/permcenter/detection/PrivacyRiskDetectionActivity.java#L1114
[risk-start]: evidence/sources/com/miui/permcenter/detection/PrivacyRiskDetectionActivity.java#L920
[risk-task]: evidence/sources/ub/d.java#L46
[app-list]: evidence/sources/xc/h.java#L13
[app-payload]: evidence/sources/ac/c.java#L62
[app-send]: evidence/sources/ac/c.java#L90
[sim-caller]: evidence/sources/com/miui/networkassistant/ui/base/TrafficRelatedPreFragment.java#L116
[sim-payload]: evidence/sources/com/miui/networkassistant/webapi/WebApiAccessHelper.java#L67
[androidid-hash]: evidence/sources/com/miui/networkassistant/utils/DeviceUtil.java#L84
[sms-read]: evidence/sources/com/miui/networkassistant/service/TcSmsReportService.java#L54
[sms-body]: evidence/sources/com/miui/networkassistant/service/TcSmsReportService.java#L178
[sms-payload]: evidence/sources/com/miui/networkassistant/webapi/WebApiAccessHelper.java#L83
[sms-consent]: evidence/sources/com/miui/networkassistant/ui/fragment/TcSmsReportFragment.java#L416
[police-flow]: evidence/sources/com/miui/warningcenter/policeassist/PaService.java#L182
[police-fields]: evidence/sources/com/miui/warningcenter/policeassist/PaUtils.java#L104
[eps-fields]: evidence/sources/com/miui/policeassist/EPSManager.java#L49
[eps-toggle]: evidence/sources/com/miui/policeassist/EPSManager.java#L82
[police-endpoints]: evidence/sources/com/miui/warningcenter/policeassist/PaCommon.java#L6
[police-sms]: evidence/sources/com/miui/warningcenter/policeassist/PaUtils.java#L82
[quake-fields]: evidence/sources/com/miui/earthquakewarning/service/EarthquakeMonitorDataManager.java#L51
[quake-send]: evidence/sources/com/miui/earthquakewarning/service/EarthquakeMonitorUtils.java#L54
[quake-source]: evidence/sources/com/miui/earthquakewarning/service/EarthquakeWarningService.java#L113
[quake-consent]: evidence/sources/com/miui/earthquakewarning/ui/EarthquakeWarningMonitorOpenFragment.java#L207
[notification-grant]: evidence/sources/com/miui/bubbles/services/BubblesNotificationListenerService.java#L16
[notification-forward]: evidence/sources/com/miui/bubbles/services/BubblesNotificationHelper.java#L51
[accessibility-events]: evidence/sources/com/miui/gamebooster/gbservices/AntiMsgAccessibilityService.java#L401
[notification-text]: evidence/sources/com/miui/gamebooster/gbservices/AntiMsgAccessibilityService.java#L253
[analytics]: evidence/sources/com/miui/analytics/StatManager.java#L41
[fbo]: evidence/sources/ta/d.java#L93
[credential]: evidence/sources/com/miui/securitycenter/provider/DeviceCredentialProvider.java#L102
[privacy-provider]: evidence/sources/com/miui/permcenter/privacycenter/PrivacyCenterProvider.java#L38
[month-gate]: evidence/sources/com/miui/monthreport/c.java#L149
[month-false]: evidence/sources/ma/c.java#L5
[manifest]: evidence/manifest/AndroidManifest.xml
[network-xml]: evidence/manifest/res/xml/network_security_config.xml
