# GuardProvider 2.0.3 隐私数据获取与上传复核

本报告仅适用于 `com.miui.guardprovider`，版本 `2.0.3-20231124.0`（versionCode 209），样本 SHA-256 为 `b0c02fa682b8ca82429b471113d0702042ac76234c85fdcd8efeb7c03d313024`，大小 13,251,548 字节。样本来自本项目同一来源设备的三个内置应用之一；设备身份与个人运行日志不公开。

基于静态代码分析，以下描述采集及发送实现路径。

**结论：该版本有明确的非系统应用清单上传、Wi-Fi 环境信息提交、扫描事件统计及条件性设备标识读取路径。持久 UUID 可以关联多次应用清单；OneTrack 的 IMEI/IMSI 列表使用 MD5 摘要。**

`evidence/` 保留用于复核的反编译文本，引用行号按这些文件计；原文件摘要、发布副本摘要和原始相对路径见 [evidence-index.json](evidence-index.json)。

## 数据流一览

| 数据 | 代码来源与转换 | 发送方向 | 条件和判断 |
|---|---|---|---|
| 非系统应用的包名、版本号、签名 MD5、URL 编码名称 | `PackageManager.getInstalledPackages(0)` 后过滤 `FLAG_SYSTEM`；序列化 `content` 数组 | `https://flash.sec.miui.com/detect/app` | 反诈/风险应用检测路径；发送前检查联网许可；范围为系统枚举结果中的非系统应用 |
| 清单关联标识及环境 | 首次随机 UUID，保存 `key_uuid` 并复用；系统增量版本、秒级时间戳、`biz_id=virus_scan` | 与上述清单同请求 | UUID 持久保存，可持续关联请求 |
| 当前 Wi-Fi SSID、BSSID、DHCP DNS1/DNS2 | Wi-Fi 管理器与 DHCP 数据进入 `a1/d1` 请求对象 | 腾讯 TMSDK cmd 794 云端 DNS/钓鱼检测 | 走 DNSANDPHISHING 检测分支，已连接 Wi-Fi 且字段可取得 |
| IMEI/IMSI | OneTrack 读取原始标识；DAU 中列表经 MD5 后加入 `imeis`/`imsis` | OneTrack 统计通道，国内默认 `https://tracking.miui.com/track/v4` | 默认配置允许读取，受区域门控、权限及网络设置约束 |
| Android ID、手机型号 | 宿主向 `TMSDKContext.setDeviceInfo()` 提供 | 腾讯 SDK | 腾讯引擎初始化路径 |
| 被扫应用的指纹、安装来源、威胁结果 | `MiEngineNG` 的统计事件包含包名、版本、Manifest/证书指纹、installer、threatName | OneTrack；云扫描另走小米病毒引擎 API | 由扫描场景及结果触发 |

## 1. 应用清单上传具有完整静态链路

```text
j0.c.a()/h()
  → g(context): getInstalledPackages(0), 过滤系统应用
  → f(context,pkg): 签名证书 MD5
  → b(list): {timestamp,os,biz_id,uuid,content:[{pkg,version,signature,appname}]}
  → h0.b.d(..., https://flash.sec.miui.com/detect/app, ...)
      → GuardApplication.e(context) 网络门控
      → HttpURLConnection POST JSON
```

证据：[j0/c.java 第 57、133 行](evidence/java/j0/c.java)、[UUID 持久化 w0/b.java 第 19 行](evidence/java/w0/b.java)、[发送 h0/b.java 第 149 行](evidence/java/h0/b.java)。签名摘要来自证书字节。

该链路由检测流程调用，发送受联网许可控制。持久 UUID 与应用清单放在同一请求中，使接收方具备关联多次清单的条件；应用名称、包名和版本可反映用户的软件选择。

## 2. Wi-Fi 云端检测提交可关联网络环境的数据

`TencentEngine` 的 DNSANDPHISHING 分支调用 `detectDnsAndPhishing()`；`tmsdkobf.p9` 读取连接信息和 DHCP DNS，分别写入 `d1.f9072b=ssid`、`d1.f9073c=bssid`、DNS 字符串列表，然后调用 `x5VarF.a(794, a1Var, ...)`。

证据：[TencentEngine.java 第 122 行](evidence/java/com/miui/guardprovider/engine/tencent/TencentEngine.java)、[p9.java 第 200—256 行](evidence/java/tmsdkobf/p9.java)。

检测请求对象被提交给 SDK 网络通道。SSID/BSSID 可关联所在网络，DNS 地址反映网络解析配置；这些字段具有识别网络环境和提供地点线索的价值。

## 3. OneTrack 读取标识并对 IMEI/IMSI 列表做 MD5

宿主 [z/b.java 第 22 行](evidence/java/z/b.java) 未关闭 IMEI/IMSI 配置，并显式关闭 GAID。默认值见 [Configuration.java](evidence/java/com/xiaomi/onetrack/Configuration.java)。DAU 构造在 [api/c.java 第 109—115 行](evidence/java/com/xiaomi/onetrack/api/c.java) 先检查区域状态，再调用 `DeviceUtil.q()` 和 `DeviceUtil.v()`。

- [DeviceUtil.java 第 609 行起](evidence/java/com/xiaomi/onetrack/util/DeviceUtil.java)：`q()` 对原 IMEI 列表逐项调用 `com.xiaomi.onetrack.d.d.c()`。
- 同文件第 724 行起：`v()` 对 IMSI 列表逐项调用 `d.h()`。
- [摘要函数 d/d.java 第 34、68、87 行](evidence/java/com/xiaomi/onetrack/d/d.java) 显示对应 MD5 计算。

DAU 字段中的 IMEI/IMSI 列表使用稳定 MD5 摘要，相同原始标识可得到相同摘要，具备持续关联价值。

`q.a() ? q.h() : configuration.isInternational()` 为 `true` 时跳过上述列表采集；标识读取还需要相应权限。OneTrack 网络许可设置见 [z/b.java 第 39 行](evidence/java/z/b.java)。默认国内统计域名见 [util/x.java 第 35、129 行](evidence/java/com/xiaomi/onetrack/util/x.java)，域名支持区域和配置覆盖。

## 4. 网络许可条件

[GuardApplication.e() 第 38—48 行](evidence/java/com/miui/guardprovider/GuardApplication.java) 的国内构建分支读取 `persist.sys.sc_allow_conn`，默认值为 `false`；国际分支还要求安全中心安装，`jp_kd` 另外检查 `antivirus_engine_network_enable`。

国内和国际构建分别执行上述门控；安全中心安装检查位于国际分支。

## 5. 扫描事件与 SDK 数据提供

[MiEngineNG.java 第 201 行](evidence/java/com/miui/guardprovider/engine/mi/MiEngineNG.java) 构造的结果统计含 `packageName/versionCode/versionName/mfSha256/signaturesMd5/signaturesSha256/installer/threatName`。这属于应用使用/安装来源和安全结果信息，可能与统计标识关联。

[TencentEngine.java 第 402 行](evidence/java/com/miui/guardprovider/engine/tencent/TencentEngine.java) 在初始化时把 Android ID 与 `Build.MODEL` 交给腾讯 SDK。

云扫的载荷包括应用元数据和指纹，见 [p0/f.java](evidence/java/p0/f.java)、[q0/d.java](evidence/java/q0/d.java)。应用组件和权限声明见 [AndroidManifest.xml](evidence/AndroidManifest.xml)。

## 原材料纠错与阅读顺序

本报告汇总采集字段、发送路径和条件；[原总报告脱敏副本](SOURCE_REPORT.md) 和 [专项归档](archival/) 保留分析过程。关键修订如下：

| 项目 | 最终描述 |
|---|---|
| OneTrack DAU 标识处理 | `DeviceUtil.q()` 对 IMEI 逐项 MD5；IMSI 亦为 MD5 |
| 标识采集条件 | 区域门控、权限、网络许可和业务触发共同约束 |
| 联网判断的构建差异 | 国内分支检查系统联网属性；安全中心安装检查在国际分支 |
| 应用清单范围与字段 | 系统枚举结果中的非系统应用；包名、版本、签名 MD5、应用名称 |
| 云扫描载荷 | 应用元数据与指纹 |
