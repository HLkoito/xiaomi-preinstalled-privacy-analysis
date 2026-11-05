# 隐私数据获取与发送总表

本表记录静态代码中可追踪的数据流。字段名代表程序的数据键，具体值由运行环境及权限状态决定。

| ID | 模块/入口 | 数据来源与处理 | 发送方向 | 条件与相关设置 | 证据入口 |
|---|---|---|---|---|---|
| SH-01 | SystemHelper 设备注册 | 电话 API 读取双卡 IMEI、IMSI、ICCID、MSISDN；序列化设备与 SIM 信息 | 默认 `http://dm.wo.com.cn:18080/registerv3` | 开机、定时、SIM 变化等触发；对运营商代码非空的非联通卡，指定 SIM/小区字段额外 MD5 | [SystemHelper 报告](../apps/systemhelper/REPORT.md) |
| SH-02 | SystemHelper 网络/设备画像 | CellID、LAC/TAC、网络类型、VoLTE/5G、容量和系统版本 | 同一注册 JSON | 依据采集开关及各字段读取结果组装 | [SystemHelper 报告](../apps/systemhelper/REPORT.md) |
| SH-03 | SystemHelper 应用统计 | 默认最近 7 天 UsageStats；名称、包名、前台时长；UID 关联移动/Wi-Fi 流量 | 同一注册端点，另可携带失败缓存补传 | 应用信息任务；受使用统计等权限约束 | [SystemHelper 报告](../apps/systemhelper/REPORT.md) |
| SH-04 | SystemHelper 日志 | 上传前完整 JSON 进入 `Log.d` | 本地日志 | 日志可读范围由系统权限决定 | [SystemHelper 报告](../apps/systemhelper/REPORT.md) |
| GP-01 | GuardProvider 反诈/应用检测 | 枚举可见的非系统应用；名称、包名、版本、签名 MD5；持久 UUID、系统版本、时间 | `https://flash.sec.miui.com/detect/app` | 业务调用进入检测，检查联网许可 | [GuardProvider 报告](../apps/guardprovider/REPORT.md) |
| GP-02 | GuardProvider Wi-Fi 检测 | 当前 SSID、BSSID、DNS1、DNS2 组装检测对象 | 腾讯 TMSDK 云端检测 | Wi-Fi 检测任务 | [GuardProvider 报告](../apps/guardprovider/REPORT.md) |
| GP-03 | GuardProvider OneTrack | 设备身份、扫描事件、包信息；IMEI/IMSI 读取分支对标识做 MD5 | OneTrack，默认 `tracking.miui.com/track/v4` | 采集配置、区域、权限、联网/隐私状态共同控制 | [GuardProvider 报告](../apps/guardprovider/REPORT.md) |
| GP-04 | GuardProvider 云扫 | 包名、版本、证书/文件指纹、安装来源、威胁判定等 | 小米云扫及相关事件统计 | 具体扫描引擎和调用场景 | [GuardProvider 报告](../apps/guardprovider/REPORT.md) |
| GP-05 | GuardProvider 腾讯初始化 | Android ID 与手机型号交给 SDK | 腾讯 SDK 接口 | 腾讯引擎初始化 | [GuardProvider 报告](../apps/guardprovider/REPORT.md) |
| SC-01 | 安全中心国内广告布局 | `x`=Android ID，`oa`=OAID，`m`=电话设备 ID 摘要；`deviceId`=设备 ID/Android ID/序列号的 SHA-1；电量/存储/系统状态 | `https://adv.sec.miui.com/info/layout` | 国内分支；联网许可；设备 ID 分量受限制属性控制；个性化广告状态作为请求参数 | [安全中心复核 §2](../apps/securitycenter/REVIEW.md) |
| SC-02 | 隐私风险检测页面 | 枚举非系统应用，生成包名→版本映射 | `https://flash.sec.miui.com/user/ra` | 页面启动检测任务，发送检查联网许可 | [安全中心复核 §3](../apps/securitycenter/REVIEW.md) |
| SC-03 | 流量查询业务 | IMSI、ICCID、手机号、城市/运营商/月用量；`imei` 键填入 Android ID 派生 MD5 | `https://api.miui.security.xiaomi.com/netassist/flow/queryflow` | 流量业务请求及联网开关 | [安全中心复核 §4](../apps/securitycenter/REVIEW.md) |
| SC-04 | 流量校准短信反馈 | 短信正文、指令、回执号码、省市、运营商等；号码遮罩 | `https://api.sec.miui.com/v1/traffic/report` | 上传告知弹窗确认后 `reportSms()` | [安全中心复核 §5](../apps/securitycenter/REVIEW.md) |
| SC-05 | 报警辅助 | 经纬度、文字地址、OAID、可获取的手机号、紧急号码、呼叫和定位时间 | `gateway-papa.cloud.weizhi.com/papa/upload`；独立开关控制 `eps.caict.ac.cn/eps/collect` | 报警场景、设置及协议入口；HTTP载荷/SMS有各自加密处理 | [安全中心复核 §6](../apps/securitycenter/REVIEW.md) |
| SC-06 | 地震志愿者 | OAID、位置、时间、机型/ROM、志愿者 token、传感器数组及公共参数 | `https://api.sec.miui.com/earthquake/volunteer/sensor/upload` | 志愿者加入流程、隐私复选框 | [安全中心复核 §7](../apps/securitycenter/REVIEW.md) |
| SC-07 | 通知与游戏辅助 | 通知监听访问；相关无障碍服务读取 QQ/微信通知文本匹配关键词 | 本地业务处理 | 对应系统能力及功能启用流程 | [安全中心复核](../apps/securitycenter/REVIEW.md) |

## 解释这些数据流时的关键区别

- SystemHelper 的使用记录来源是 UsageStats；GuardProvider 和安全中心的上述清单来源是非系统应用枚举。两类清单的覆盖范围不同。
- `imei` 等键名需要追到赋值点：安全中心流量查询中的该字段使用 Android ID 派生值；OneTrack 的标识列表经过 MD5。
- 请求体加密与传输层保护是两层处理。SystemHelper 在 AES 处理 JSON 后使用 HTTP；URL 中的型号/厂商等元数据位于请求参数。
- 部分上报有明确的业务确认步骤。校准短信的上传确认、地震志愿者加入流程和报警辅助开关，是分析相应数据流时的一部分。
- FBO 名为 `upLoadPackageList()` 的路径写入本机 `Settings.Secure` 并交给系统服务，应归为本地数据传递。
