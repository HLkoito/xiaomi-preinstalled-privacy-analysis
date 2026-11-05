# SystemHelper：隐私数据获取与上传实现复核

基于静态代码分析，以下描述实现路径。历史原报告及专项材料保留在 [SOURCE_REPORT.md](SOURCE_REPORT.md) 和 [archival](archival)，结论以本报告为准。

## 1. 结论

**本样本有明确的设备/SIM 身份、小区信息、应用使用时长与流量采集和上报实现。默认接收地址为 `http://dm.wo.com.cn:18080/registerv3`。** 最值得关注的组合是不可轻易重置的身份标识、蜂窝小区线索和应用使用画像可以进入同一份 JSON。

字段读取受首次设置状态、系统权限、AppOps、SIM 状态和厂商 API 条件控制。

## 2. 样本身份

| 项目 | 内容 |
|---|---|
| 包名 | `com.mobiletools.systemhelper` |
| versionName / versionCode | `1.2-20230925-U` / `3` |
| APK SHA-256 | `efa1af0d0be75985701fd0eca79a90f0be0bb9fd2735245915200ec1a3e9657f` |
| APK 大小 | 911,821 字节 |
| minSdk / targetSdk / compileSdk | 24 / 28 / 33 |
| 分析类型 | 既有 apktool smali/Manifest 文本复核；历史报告还引用 REA 静态证据 |

哈希用于限定软件样本。公开的可直接检查证据是选取的 [smali/Manifest 文件](evidence/README.md)。

## 3. 数据类别与发送方向

| 类别 | 静态实现获取/组装的数据 | 条件及解释 |
|---|---|---|
| 设备身份 | 双卡槽 IMEI、厂商、型号、硬件/系统/固件版本、Android 版本 | 按厂商分支及权限读取 |
| SIM 身份 | 双卡 IMSI、ICCID、MSISDN、运营商代码 | MSISDN 为手机号字段，读取受 SIM 及系统返回值控制 |
| 蜂窝网络与位置线索 | CellID、LAC/TAC、蜂窝网络制式 | 基站标识可提供位置线索 |
| 网络设置 | VoLTE/5G 开关、默认数据卡槽、Wi-Fi/移动网络类型 | 反映网络配置与接入方式 |
| 硬件画像 | RAM、存储容量和剩余空间 | 用于设备配置画像 |
| 应用使用 | UsageStats 查询返回的应用包名、名称、前台使用时长；默认 7 天窗口 | 已安装包枚举用于补全标签和 UID；序列化以 UsageStats 记录为基础 |
| 应用流量 | 按应用 UID 关联移动数据收发、Wi-Fi 总流量 | 应用流量统计量 |
| 上报元数据及缓存 | 时间 `Time`、触发类型、协议/组件版本，历史失败 JSON | 有补传及至多 100 条历史缓存实现 |

字段序列化见 [d0/g.smali:335](evidence/smali/d0/g.smali#L335)，第一卡 IMEI/ICCID/IMSI/MSISDN 键依次见 [444 行](evidence/smali/d0/g.smali#L444)、[459 行](evidence/smali/d0/g.smali#L459)、[474 行](evidence/smali/d0/g.smali#L474)、[489 行](evidence/smali/d0/g.smali#L489)，第二卡位于同一序列化方法后续。小区读取见 [1727 行](evidence/smali/d0/g.smali#L1727)及 [1973 行](evidence/smali/d0/g.smali#L1973)。

应用记录来源见 [queryAndAggregateUsageStats 调用](evidence/smali/d0/i.smali#L508)、[应用遍历与补全](evidence/smali/d0/i.smali#L568)、[查询时间窗口](evidence/smali/d0/i.smali#L637)。前台时长见 [132 行](evidence/smali/d0/i.smali#L132)，记录序列化见 [f0/b.smali:93](evidence/smali/f0/b.smali#L93)，JSON `Time`/`AppInfo` 见 [d0/g.smali:925](evidence/smali/d0/g.smali#L925)。

基类的电话/SIM 读取见 [e0/r.smali:1280](evidence/smali/e0/r.smali#L1280)。包内含多厂商适配代码；公开副本另提供 [小米适配器 e0/y1](evidence/smali/e0/y1.smali)。

## 4. 上传路径、触发与开关

```text
开机/常规定时器/SIM 状态变化/应用统计定时器
  → 厂商 Reader + 设备/SIM/小区信息
  → 应用统计分支查询 UsageStats/NetworkStats
  → d0/g.d() 组装 JSON（可附失败历史）
  → d0/k 计算 MD5、RSA 封装密钥材料、AES 加密 JSON
  → 从 serverAddress 配置读取目的地址
  → HTTP POST application/encrypted-json
  → 默认 dm.wo.com.cn:18080/registerv3
```

默认地址和调度周期见 [f0/f.smali:55](evidence/smali/f0/f.smali#L55)：常规 24 小时、应用统计 7 天。开机路径配置约 4 分钟延迟，SIM 变化配置约 6 分钟延迟。[开机接收器](evidence/smali/com/mobiletools/systemhelper/control/OnBootReceiver.smali)、[常规定时器](evidence/smali/com/mobiletools/systemhelper/control/ScheduleRegisterReceiver.smali)、[应用统计定时器](evidence/smali/com/mobiletools/systemhelper/control/AppInfoRegisterReceiver.smali)、[SIM 接收器](evidence/smali/com/mobiletools/systemhelper/control/SIMStateReceiver.smali)提供相应实现。

`serverAddress` 可经本地配置覆盖；读取见 [f0/l.smali:146](evidence/smali/f0/l.smali#L146)，发送时调用读取见 [d0/k.smali:212](evidence/smali/d0/k.smali#L212)。

名为 `checkUserPermission` 的基础门控检查 `device_provisioned`，见 [e0/c2.smali:95](evidence/smali/e0/c2.smali#L95)，并参与 `f0/f.g` 的更新。

## 5. 加密、运营商处理与日志

**AES 加密载荷经 HTTP 发送。** JSON 的 MD5 被用于派生 AES 密钥材料，再经 RSA 公钥封装放入 `sign`；见 [d0/k.smali:176](evidence/smali/d0/k.smali#L176)、[AES 实现](evidence/smali/f0/c.smali#L21)、[RSA 实现](evidence/smali/f0/k.smali)。AES 模式为 ECB/PKCS5Padding，RSA 封装使用 PKCS1Padding。

HTTP URL 的型号、厂商等参数缺乏 TLS 保护，传输端点认证和完整性存在风险。`sign` 的内容是公钥加密的密钥材料。

对非联通且运营商代码非空的 SIM，部分 IMSI/ICCID/MSISDN/CellID/LAC 字段还会额外 MD5，见 [e0/c2.smali:4159](evidence/smali/e0/c2.smali#L4159)。IMEI 和 AppInfo 不在这组额外处理字段内；整个请求体仍经过 AES。稳定哈希保留跨记录关联价值。

另外，[d0/k.smali:150](evidence/smali/d0/k.smali#L150)将完整 JSON 拼接进字符串，随后 [163 行](evidence/smali/d0/k.smali#L163)直接调用 `Log.d`。这是加密前的日志暴露实现，日志访问受系统权限、设备配置和截断规则控制。

## 6. 导出组件风险

[Manifest:41](evidence/AndroidManifest.xml#L41) 的 `RegisterHelperService` 为导出服务，未附 `android:permission`；[onBind](evidence/smali/com/mobiletools/systemhelper/service/RegisterHelperService.smali#L41)直接返回 Binder。配置输入经 [Binder 分派](evidence/smali/com/mobiletools/systemhelper/service/a$a.smali#L125)进入 [写配置实现](evidence/smali/com/mobiletools/systemhelper/service/RegisterHelperService$a.smali#L102)。

外部调用参数可进入上报地址和周期配置，构成静态接口风险。周期配置由后续调度续期读取，调度还受 Android 的 Alarm 管理规则控制。

## 7. 原结论的准确表述

| 事项 | 本次公开版采用的结论 |
|---|---|
| 请求封装 | AES 加密载荷经 HTTP 发送，RSA 封装密钥材料 |
| 应用清单范围 | UsageStats 查询窗口中的记录，使用安装包枚举补全标签/UID |
| 时间字段 | JSON 包含 `Time` |
| 周期配置 | Binder 写入偏好配置，由后续调度读取 |

## 8. 公开分析产物

- [证据文件及 SHA-256 索引](evidence-index.json)：22 份选取的 Manifest/smali，原字节和文本行号保持一致。
- [历史主报告脱敏副本](SOURCE_REPORT.md)：含纠错警示。
- [组件/工具层归档](archival/app-shell.md)、[采集器一归档](archival/collectors-1.md)、[采集器二归档](archival/collectors-2.md)、[核心采集层归档](archival/device-core.md)。

可复查结论优先引用随公开包提供的实际代码文本。
