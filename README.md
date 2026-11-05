# 小米三个内置应用的隐私数据行为分析

整理日期：2026-11-06。本文基于同一来源设备导出的三个 APK 的静态代码分析，按“数据来源 → 字段处理 → 触发条件 → 发送目的地”整理采集与外发实现。

## 主要发现

| 应用 | 采集的数据 | 发送路径与条件 | 详细报告 |
|---|---|---|---|
| SystemHelper | 双卡设备/SIM 标识、小区信息、设备配置；UsageStats 应用使用记录与应用流量 | 开机、SIM 变化及定时任务等进入注册流程；JSON 经 AES 处理后通过 HTTP 发送，默认 `dm.wo.com.cn:18080/registerv3` | [报告](apps/systemhelper/REPORT.md) |
| GuardProvider | 非系统应用名称、包名、版本、签名 MD5，持久 UUID；Wi-Fi SSID、BSSID、DNS；扫描与统计信息 | 应用清单进入 `flash.sec.miui.com/detect/app`；Wi-Fi 检测使用腾讯 SDK；OneTrack 标识采集有区域及隐私门控 | [报告](apps/guardprovider/REPORT.md) |
| MIUI 安全中心 | 广告请求中的 Android ID、OAID、设备 ID 摘要、复合指纹及设备状态；非系统应用包名/版本 | 国内广告布局请求进入 `adv.sec.miui.com/info/layout`；隐私检测页面启动清单请求，发送至 `flash.sec.miui.com/user/ra`；发送检查联网许可 | [报告](apps/securitycenter/REPORT.md) · [逐链路复核](apps/securitycenter/REVIEW.md) |

安全中心还包含流量业务中的 SIM/号码关联、经确认的校准短信上报、报警辅助位置共享，以及加入地震志愿者后的传感器贡献。各项的具体字段、设置和同意流程见[数据流总表](docs/DATA_FLOWS.md)。

最值得关注的组合是：**稳定设备标识与应用行为关联**、**批量非系统应用清单外发**、**SIM/设备/基站数据注册**。MD5 或 SHA-1 对同一输入给出稳定摘要，具有持续关联价值；使用摘要与使用随机标识是两种不同的处理方式。

## 阅读顺序

1. [数据流总表](docs/DATA_FLOWS.md)：对照字段、触发条件、目的地与证据。
2. 三份 `apps/*/REPORT.md`：本次整理的最终结论。
3. 各应用 `evidence/` 和证据索引：选取的完整反编译文本、原行号与 SHA-256。
4. `SOURCE_REPORT.md` 和 `archival/`：原分析的脱敏历史资料，带版本纠错提示。
5. [方法与来源](docs/METHODOLOGY.md)、[样本身份](samples/manifest.json)、[隐私处理说明](docs/PRIVACY.md)。

## 样本范围

| 包名 | 版本 | versionCode |
|---|---|---:|
| `com.mobiletools.systemhelper` | `1.2-20230925-U` | 3 |
| `com.miui.guardprovider` | `2.0.3-20231124.0` | 209 |
| `com.miui.securitycenter` | `10.4.9-250402.0.1` | 40001049 |

这些版本构成本文结论的适用范围。完整 SHA-256 及字节大小见[样本清单](samples/manifest.json)。仓库以应用样本定义研究对象，来源设备的个人信息留在本地。

## 离线核验

需要 Node.js 18 或更新版本。在仓库根目录运行：

```sh
node tools/verify-integrity.mjs
node tools/privacy-check.mjs
```

[文件清单](ARTIFACTS.json)记录发布文件的 SHA-256；[检查结果](validation/publication-check.json)记录本次自动检查。隐私检查规则用于辅助人工审阅，具体处理范围见[隐私处理说明](docs/PRIVACY.md)。

反编译证据来自对应 APK，权利归原权利人；本仓库按分析目的选取文本证据。报告保留原工具与作者的证据来源说明。
