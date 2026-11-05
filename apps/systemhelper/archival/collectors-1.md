> **历史分析材料，含已纠正结论，不能作为最终结论引用。** 请先阅读 [最终复核报告](../REPORT.md)。原材料中的“MD5 派生密钥等于不加密”“全量已安装应用清单”“隐私告知永远不可达”“设为 1 秒即每秒上传”“无时间戳/已证实重放”等说法已被收窄或纠正；本次公开版未重新验证全部 OEM 分支。已删除本地身份路径，未包含真实用户采集值。

# SystemHelper（com.mobiletools.systemhelper 1.2-20230925-U）采集器分析 · 第一批（e0/a … e0/l1，42 个类）

- 目标 APK：`[LOCAL_PATH_REMOVED]`（只读，未修改）
- smali 来源：`...\analysis\SystemHelper\apktool\smali\e0\*.smali`（apktool 3.0.3 解码，未修改）
- 覆盖类：**42 个** —— a, a0, a1, a2, b, b0, b1, b2, c, c0, c1, c2$a, c2$b, c2$c, d, d0, d1, d2, e, e0, e1, f, f0, f1, g, g0, g1, h, h0, h1, i, i0, i1, j, j0, j1, k, k0, k1, l, l0, l1
- 契约类（**由他人负责，本文只引用不改写**）：`e0/r`（采集器基类）、`e0/c2`（调度器）。本文用到它们的：`e0/r` 的 `b/c/d/f/g/h/i/j/l/m/n/o/p/q/r` 与静态字段；`e0/c2` 的 `a`（MODEL→厂商 Map）、`h()`（厂商→Reader 分发）、`f()/n()`（装配 d0/g）。
- 行号均为原始 `.smali` 文件行号（本报告引用的行号与 apktool 输出一致）。

---

## 0. 前置：结果容器 `d0/g` 的字段 → JSON 键对照（决定"输出键名"）

所有采集器最终只写 `d0/g` 的字段，键名在 `d0/g.d()` / `d0/g.e()` 序列化时才出现。实测对照（`d0/g.smali:345-980` 为 `d()`，`:1048-1683` 为 `e()`，`e()` 与 `d()` 唯一差别是**不输出 IPv4/IPv6（D/E）与 History（M）**）：

| 字段 | JSON 键 | 语义 | 典型来源 |
|---|---|---|---|
| a | `Manuf` | 厂商 | `e0/c2.a.get(Build.MODEL)` 或 `Build.MANUFACTURER` / `ro.product.manufacturer` |
| b | `Model` | 机型 | `Build.MODEL` |
| c | `HWVersion` | 硬件版本 | `SystemProperties` 各厂商私有键 |
| d | `SWVersion` | 软件版本 | `Build.DISPLAY` / 厂商私有键 |
| e | `OS` | 系统名 | `"Android"` 或 `ro.yunos.name` |
| f | `OSVersion` | 系统版本 | `Build.VERSION.RELEASE` / `ro.yunos.version` |
| g | **`IMEI1`** | 串号1 | `getImei/getDeviceId/getDeviceId(int)`/厂商属性 |
| h | **`ICCID1`** | SIM 卡序列 | `getSimSerialNumber`/`SubscriptionInfo.getIccId` |
| i | **`IMSI1`** | 用户标识 | `getSubscriberId` |
| j | **`MSISDN1`** | 手机号 | `getLine1Number(ForSubscriber)`/`getMsisdn`/`SubscriptionInfo.getNumber` |
| k | `MNC1` | 运营商数字（实为 MCC+MNC，如 `46001`） | `getNetworkOperator(ForPhone/ForSubscription)`/`getSimOperator` |
| l | `NCLS1` | 网络类型字符串（NR/NSA/SA/LTE/WCDMA/TD-SCDMA/CDMA/GSM） | `e0/r.g(int,String)` 或 `f0/f.n` 缓存 |
| m | **`CellID1`** | GSM CI / CDMA BID / LTE CI / NR NCI | `CellLocation`、`CellIdentity*`、厂商 AIDL、SharedPreferences |
| n | `LAC1` | GSM LAC / LTE TAC / CDMA `lat:lon` | 同上 |
| o | `VolteEnabled1` | VoLTE 开关 | `ImsMmTelManager` 反射 / 厂商接口 |
| p | `5GEnabled1` | 5G 开关 | `Settings.Global."preferred_network_mode"+subId` |
| q | —（**未序列化**） | 死字段，仅 `d0/g.smali:134` 构造时置 null，全工程无写入 | — |
| r | **`IMEI2`** | 串号2 | 同 g |
| s | **`ICCID2`** | | 同 h |
| t | **`IMSI2`** | | 同 i |
| u | **`MSISDN2`** | | 同 j |
| v | `MNC2` | | 同 k |
| w | `NCLS2` | | 同 l |
| x | **`CellID2`** | | 同 m |
| y | `LAC2` | | 同 n |
| z | `VolteEnabled2` | | 同 o |
| A | `5GEnabled2` | | 同 p |
| B | `DataSlot` | 默认数据卡槽，`"0"`未知/`"1"`/`"2"` | `SubscriptionManager.getDefaultDataSubscriptionId()`(+`getPhoneId`/`e0/x.a`) |
| C | `AccType` | 账号类型 | 默认 `Unknown` |
| D | `IPv4`（仅 d()） | 公网 IPv4 | `d0/h` 线程 + `c2$b` 回调 |
| E | `IPv6`（仅 d()） | 公网 IPv6 | 同上 |
| F | `SubType` | 存储/内存子对象 `{RAM,ROM,FREE}` | `f0/i.c()/a()/e()` 或各机型的私有解析 |
| G | `DeviceType` | 设备类型码（`000`默认 / `000004` / `000005` / `007001029` / `HNR550T`） | 各机型硬编码 |
| H | `RegType` | 注册类型（由调用方传入，`c2.g(ctx,String)`） | `c2.smali:219` |
| I | `SpecVersion` | 固定 `"3.0"` | `d0/g.smali:890` |
| J | `RegVersion` | 客户端版本名（`versionName`）| `c2.n()` → `g.J`；默认 `"1.0"` |
| K | `Time` | 时间戳 | 默认 `""`（本批 42 类**均未写**，由 f0/* 侧写） |
| L | `AppInfo` | 已安装应用清单 JSONArray | `c2$a` 回调（`f0/b` 对象） |
| M | `History` | 历史记录 JSONArray（仅 d()） | 未在本批出现 |
| N | `SignalInfo` | 信号 JSON（RSRP/RSRQ/SINR/cellid/LAC/time） | `d0/f` 回调 |

`d0/g.f(CellLocation)`（`:1697`）写 `m/n`（CellID1/LAC1）；`d0/g.g(CellLocation)`（`:1943`）写 `x/y`（CellID2/LAC2）。GSM 用 `getCid/getLac`（-1 视为无效）；CDMA 用 `getBaseStationId`，LAC 位写 `"%d:%d"`（longitude:latitude，`0x7fffffff` 视为无效）。

## 0.1 触发链（理解"开关"的前提）

1. `d0/g.a(ctx)`（`:197`）→ `e0/c2.f(ctx)`：`i()` 初始化 → `new d0/g` → `c2.n(ctx,g)` → `c2.l/m(ctx,g)`（ICCID 变化比对，读 prefs `iccid1`/`iccid2`）→ `c2.o(g)` → 起 `d0/i` 线程（应用清单）。
2. `c2.n()`（`:3717`）先写 `g.J = versionName`，再 `c2.h(ctx).b(ctx,g)` —— **`c2.h()` 才是分发点**：`c2.a.get(Build.MODEL)` 得到厂商字符串，按厂商 `equals` 逐个 new 出对应 Reader（`c2.smali:413-1358`），无匹配则 `new e0/r`（通用实现）。
3. 因此"哪台设备走哪个采集器"由 **`Build.MODEL` → 厂商名 → Reader** 三级决定；每个 Reader 的 `a(Context)` 绝大多数是 `new ServiceState().getRoaming()` 的**恒 false 桩**（见 §5 交叉发现 1），真正的机型判定写在 `b()` 内部。
4. `c2$a`（`d0/i$a` 回调）把 `f0/f.g` 置为 `c2.a(ctx)` 的返回值（实为 `c2.e()`：`Settings.Global."device_provisioned"==1`），为 true 时才把应用清单塞进 `g.L`。

## 0.2 本批涉及的公共能力（被反复调用，后文简称）

| 符号 | 语义 | 位置 |
|---|---|---|
| `e0/r.h(int slot)` | 反射 `SubscriptionManager.getSubId(int)` / `getSubscriptionId(int)` → subId | `r.smali:95` |
| `e0/r.f(TM,int)` | 反射 `getNetworkType(int)`，为 0 再取 `getVoiceNetworkType(int)` | `r.smali:745` |
| `e0/r.g(int netType,String operator)` | 网络类型→字符串；`46000/46002/46007` 特殊处理 | `r.smali:814` |
| `e0/r.c/d(ctx,int)` | VoLTE：`ImsManager.getImsMmTelManager` + `setAdvancedCallingSettingEnabled(true)` 后读回，`"1"/"0"/"-1"` | `r.smali:560/690` |
| `e0/r.j(ctx,int)` | `Settings.Global.getInt("preferred_network_mode"+subId)` → 5G 开关 | `r.smali:1168` |
| `e0/r.l/m/n/o/p/q(ctx,g)` | 四条"整机读取"实现（API≥30 / 旧 / API26 / "read26" / 反射版） | `r.smali:1242-2612` |
| `e0/r.r(ctx,slot,g)` | API≥28：`ServiceState.getNetworkRegistrationInfoList()` + `CellIdentityGsm/Wcdma/Tdscdma/Cdma/Lte/Nr` 取 CellID/LAC | `r.smali:2614` |
| `e0/d2.b/c(TM,name,nameLong,subId)` | 反射 `TelephonyManager.getXXX(int)`，`NoSuchMethodException` 时回退 `getXXX(long)` | `d2.smali:37/47` |
| `e0/d2.a(int)` | 反射 `SubscriptionManager.getSubId(int)`→`long[]`→`int[]`（失败回退 `int[]`） | `d2.smali:7/132` |
| `f0/o.a/b/c` | `Class.forName` / `getDeclaredMethod` / `setAccessible(true)+invoke`（**吞异常返回 null**） | `f0/o.smali:7/39/74` |
| `f0/l.i/j/k(String[,def])` | 反射 `android.os.SystemProperties.get(...)` 读系统属性 | `f0/l.smali:175/225/303` |
| `f0/l.h/j` | SharedPreferences `getString` / 属性读 | `f0/l.smali:157` |
| `f0/f.g` | 全局"允许采集"布尔（被各 Reader 的 `t()` 覆写） | `f0/f.smali:23` |
| `f0/f.n` / `f0/f.o` | 槽1/槽2 网络类型字符串缓存（由 `d0/f` 回调写） | `f0/f.smali:37/39` |
| `f0/f.B` | 厂商 AIDL `c0/a`（iGreeTelephony）实例 | `f0/f.smali:9` |

---

## 1. 总表（42 类）

| 类 | 业务含义（采集目标） | 关键 Android API / 隐藏 API / 属性 | 输出键名 | 隐私等级 |
|---|---|---|---|---|
| `e0/a` | 厂商适配：**BROR / 中国移动定制 CM 系列（YunOS）** 整机信息 | `Build.*`；属性 `gsm.yunos.imei0/1`、`ro.yunos.name/version`、`ro.build.version.external`、`ro.build.display.hardware.id`、`ro.product.hardware`、`ro.mediatek.version.release`；`f0/o` 反射 `TelephonyManager.getSubscriberId/getSimSerialNumber/getMsisdn/getNetworkOperatorForPhone/getCellLocation*` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2 | **极高** |
| `e0/a0` | 厂商适配：**DOUQI（读曲）**，含 RAM/ROM/剩余空间解析 | `ActivityManager.MemoryInfo.totalMem`、`StatFs(/system,/data)`、`ro.product.manufacturer`、`ro.boot.hardware.revision`、`Settings.Global.device_provisioned`；`e0/i.a`(getImei(int))、`e0/r.h`、`e0/z.a`(getDefaultDataSubscriptionId) | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType{RAM,ROM,FREE}, DeviceType=`000005`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, Volte1/2, 5G1/2, DataSlot | **极高** |
| `e0/a1` | 厂商适配：**"5G HYUN"/OFeng（5GP50）** | `ro.product.hardware`；`android.telephony.TelephonyManagerEx` + `ServiceManager("phone_ex")`（MTK 隐藏服务）；`f0/o` 反射全套 | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, 5G1/2, DataSlot | **极高** |
| `e0/a2` | 厂商适配：**YuLong / Coolpad（酷派/宇龙）** | 属性 `ro.product.hardware(.version)`、`ro.build.display.id/.hardware`、`ro.config.le_hardware_version`、`ro.yulong.version.hardware`；MTK `TelephonyManagerEx.getCellLocation(int)`；`f0/o` 反射 | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2（**未写 DeviceType**） | **极高** |
| `e0/b` | **API 桥**：`TelephonyManager.getImei()`（无参，API26+） | `TelephonyManager.getImei()` | 不写（返回值给调用者） | 低 |
| `e0/b0` | 厂商适配：**EAMEY（一墨/壹秘）** | 属性 `ril.imei.sim1/sim2`、`ro.build.tyd.custom.hw_version`；MTK `TelephonyManagerEx`；`e0/d2` 反射 | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 | **极高** |
| `e0/b1` | 厂商适配：**Onefive（万五/Onefive 全系）**，含 `/proc/meminfo` RAM 解析 | `/proc/meminfo`、`StatFs`、`Formatter.formatFileSize`、属性 `ro.yunos.*`、`ro.build.description`、`ro.bd.cmcc_software_version`、`ro.bd.cmcc_hardware_version`、`ro.bird.custom.sw.version`、`ro.product.hardware`；MTK Ex | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2（RAM 仅用于日志/分支） | **极高** |
| `e0/b2` | 厂商适配：**ZTE（中兴）**，含沃/联通 DM 定制与 DeMai 双栈 | `com.android.vts.VendorTelephonyManager.getDefault().getCellLocation(int)`；`com.dmyk.android.telephony.DmUnicomAbsTelephonyManager.getVoLTEState`；`Settings.Secure."dm_user_agreement"`；`TelephonyManager.getDeviceId(int)/getLine1Number/getSimSerialNumber`；`ro.boot.hardware.revision` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, Volte1/2, DataSlot | **极高** |
| `e0/c` | 厂商适配：**Baidu/展讯（XDM-01，"sprd"）** | `Settings.Secure."dm_user_agreement"`；`e0/b.a`(getImei)；`getSubscriberId/getSimSerialNumber/getLine1Number/getNetworkOperator`；`getVoiceNetworkType` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, **DeviceType=`007001029`**, DataSlot=`1`, IMEI1, ICCID1, IMSI1, MSISDN1, MNC1, NCLS1 | **极高** |
| `e0/c0` | 厂商适配：**EWPE/Gree（格力）**，走厂商 AIDL iGreeTelephony | `Settings.Secure."operator_service_improve_plan"`；`f0/f.B`(c0/a)：`n()/p()/l()/h()`取槽位、`m(slot,key,def)` 取属性、`e()/b()/c()/a()/d()/g()/j()` 取 ICCID/号码/运营商/网络类型/LAC/CellID/VoLTE；属性 `persist.sys.fota_deviceid1/2`、`persist.sys.card.imsi`、`ro.hw_version` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2, Volte1/2, DataSlot | **极高** |
| `e0/c1` | 厂商适配：**OPPO/ColorOS 全系（110+ 机型白名单）** | `ColorOSTelephonyManager.getDefault()`（反射）；`Settings.Global."device_provisioned"`；`ActivityManager.MemoryInfo`；属性 `ro.boot.dram_info`、`ro.build.hardware.version`、`ro.build.display.id`；反射 `isOemEnhanced4gLteModeSettingEnabledByUser`；`e0/r.l` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`/`000004`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, Volte1/2, 5G1/2, DataSlot | **极高** |
| `e0/c2$a` | **回调**：采集线程结束后的收尾（应用清单落 `g.L`，起 IP 采集线程） | `Wechat`? 否 —— `f0/f.b` 分支：`d0/h`（IP+运营商）或 `d0/k` | AppInfo(`L`), 间接 IPv4/IPv6 | **高**（应用列表） |
| `e0/c2$b` | **回调**：`d0/h$a`，写公网 IP | `d0/h` 线程回传 | IPv4(`D`), IPv6(`E`) | **高** |
| `e0/c2$c` | **接口**：厂商 Reader 契约（`a(ctx)Z` / `b(ctx,g)V`） | 无 | — | 低 |
| `e0/d` | **API 桥**：`TelephonyManager.unregisterTelephonyCallback` | 同上（API 31+） | 不写 | 低 |
| `e0/d0` | 厂商适配：**Gionee（金立，"gionee"）** | `Settings.Global."device_provisioned"` + `Settings.Secure."operator_registration_switch"`；属性 `ro.ssui.hardwarever`、`ro.ssui.sv.version`；MTK `TelephonyManagerEx.getDefault().getCellLocation(int)` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000004`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2, DataSlot | **极高** |
| `e0/d1` | 厂商适配：**ORANGE（阿尔卡特/锐嘉科 RGK 代工）** | 属性 `gsm.rgk.imei1/2`、`ro.qc.hardwareversion`、`ro.qc.softversion`；`f0/o` 反射；CellID/LAC 取自 SharedPreferences | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2 | **极高** |
| `e0/d2` | **反射工具**：TelephonyManager/SubscriptionManager 分槽取值 | `Class.forName("android.telephony.TelephonyManager")`、`SubscriptionManager.getSubId(int)`；`getXXX(int)`→`getXXX(long)` 自动回退 | 不直接写（返回 Object） | 低（能力层） |
| `e0/e` | **API 桥**：`CellIdentityNr.getNci()`（5G 小区标识，long） | `CellIdentityNr.getNci()` | 不写 | 低 |
| `e0/e0` | 厂商适配：**TOSOT（大松/格力，"Gree"）** | `Settings.Global."device_provisioned"`；属性 `ro.hw_version`、`ro.build.display.id`；`f0/f.B` AIDL（同 c0）；`SubscriptionManager.getDefaultDataSubscriptionId`+`getPhoneId`/`e0/x.a` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, LAC1/2, Volte1/2, 5G1/2, DataSlot | **极高** |
| `e0/e1` | 厂商适配：**OYSIN（欧信）** | 属性 `ro.yunos.name/version`、`ro.product.hardware`、`ro.hardware.version`、`ro.xh.display.version`；`f0/o` 反射；`e0/r.p` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 | **极高** |
| `e0/f` | **API 桥**：`CellIdentityNr.getTac()` | 同上 | 不写 | 低 |
| `e0/f0` | 厂商适配：**Haipainoble（海派诺伯）** | `com.mediatek.internal.telephony.ITelephonyEx$Stub.asInterface(ServiceManager.getService("phoneEx")).getSvlteImei(int)`（MTK 隐藏 Binder）；属性 `ro.yunos.*`、`ro.hardware.version`、`ro.build.display.id` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 | **极高** |
| `e0/f1` | 厂商适配：**OnePlus（一加 LE21xx）** | `ActivityManager.MemoryInfo.totalMem`、属性 `ro.boot.dram_info`、`ro.build.display.id`；`SubscriptionManager...getPhoneId`；`e0/r.l` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType{RAM,ROM,FREE}, DeviceType=`000005`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, Volte1/2, 5G1/2, DataSlot | **极高** |
| `e0/g` | **API 桥**：`ImsManager.getImsMmTelManager(int subId)` | 同上（ImsMmTelManager，隐藏/受限 API） | 不写 | 低 |
| `e0/g0` | 厂商适配：**Hisense（海信）** | 属性 `ro.hardware.version`、`ro.software.version`、`ro.hmct.volte.enabled`、`persist.sys.volte.enable`；`e0/r.l`；`f0/o` 反射 | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000004`/`000005`/`HNR550T`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, Volte1/2, DataSlot | **极高** |
| `e0/g1` | 厂商适配：**PHICOMM（斐讯）** | **反射 `android.os.Build` 的厂商注入静态字段** `OS_NAME/OS_VERSION/HARDWARE_VERSION_DISPLAY/OUTER_VERSION_DISPLAY`；属性 `ro.yunos.*`、`ro.internal.version`、`ro.build.display.hw.version`、`ro.build.display.soft`、`ro.hardware.version` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 | **极高** |
| `e0/h` | **API 桥**：`ImsMmTelManager.isAdvancedCallingSettingEnabled()`（VoLTE 读回） | 同上 | 不写 | 低 |
| `e0/h0` | 厂商适配：**HUAWEI（EVR-AN00）** | `android.telephony.HwTelephonyManager.getDefault().getCellLocation(int)`、`com.huawei.android.telephony.TelephonyManagerEx.getDefault4GSlotId()`；`TelephonyManager.getImei(int)` 反射；属性 `ro.product.hardwareversion` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, DataSlot, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2 | **极高** |
| `e0/h1` | 厂商适配：**PHILIPS（E266W）** | `Settings` 无；属性 `ro.custom.hw.version`(默认 `MBV1.0`)；`f0/o` 反射 `getDeviceId(int)/getSubscriberId(int)/getSimSerialNumber(int)/getLine1NumberForSubscriber(int)/getNetworkOperatorForSubscription(int)/getCellLocationForPhone(int)`；`SubscriptionManager.isValidSubscriptionId`(setAccessible) | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2 | **极高** |
| `e0/i` | **API 桥**：`TelephonyManager.getImei(int slot)` | 同上（API26+） | 不写 | 低 |
| `e0/i0` | **API 桥**：`SubscriptionManager.getSubscriptionIds(int slot)` | API 30+ | 不写 | 低 |
| `e0/i1` | 厂商适配：**PPTV（KING 7S / PP5200 / PP5600）** | MTK `ITelephonyEx$Stub.asInterface(ServiceManager.getService("phoneEx")).getSvlteImei(int)`；属性 `ro.cust.hwversion`、`ro.build.custom.display.id`、`ro.product.hardware.version`、`ro.pptv.version`；`e0/r.p` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2 | **极高** |
| `e0/j` | **API 桥**：`Context.getMainExecutor()` | API 28+ | 不写 | 低 |
| `e0/j0` | 厂商适配：**HZTL（华智/TAI-M01）**，实际用华为 HwTelephonyManager | `HwTelephonyManager.getDefault()`、`MSimTelephonyManager`("from")、`setServiceAbility`、`isVolteAvailable`；`READ_PHONE_STATE/READ_PHONE_NUMBERS` 权限检查；属性 `ro.product.hardwareversion`、`ro.boot.dram_info`；`e0/b.a`、`e0/i.a`、`e0/i0.a` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, DataSlot, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2, Volte1/2, 5G1/2 | **极高** |
| `e0/j1` | 厂商适配：**PROTRULY（D7 / D8）** | 属性 `ro.hardware_version`；`e0/d2` 反射全套 + `e0/r.f/g` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2 | **极高** |
| `e0/k` | **API 桥**：`TelephonyManager.registerTelephonyCallback(Executor,TelephonyCallback)` | API 31+ | 不写 | 低 |
| `e0/k0` | 厂商适配：**INTKI（E9000/E9003）** | 反射 `TelephonyManager.getCustomHWBuildVer()` / `getCustomSWBuildVer()`；属性 `ro.yunos.name/version`；`e0/r.p` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 | **极高** |
| `e0/k1` | 厂商适配：**PTAC（华为智选：FIO/Hebe/Hera/MGZ/ALH/MIZ/MRK-BD00）** | 与 j0 近乎同构：`HwTelephonyManager`、`MSimTelephonyManager`、`setServiceAbility`、`isVolteAvailable`、`READ_PHONE_STATE` 检查；属性 `ro.board.hardware_version`、`ro.boot.dram_info` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, DataSlot, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2, Volte1/2, 5G1/2 | **极高** |
| `e0/l` | **API 桥**：`ServiceState.getNetworkRegistrationInfoList()` | API 28+ | 不写 | 低 |
| `e0/l0` | **API 桥**：`CellInfoNr.getCellIdentity()` | API 29+ | 不写 | 低 |
| `e0/l1` | 厂商适配：**QIKU（奇酷，1501-A02）** | 属性 `ro.build.description`、`ro.yulong.version.hardware`；`e0/r.p` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 | **极高** |

> 隐私等级口径：**极高** = 直接拿到 IMEI/IMSI/ICCID/手机号/小区标识（可跨应用长期追踪、可定位到基站级）；**高** = 应用清单 / 公网 IP；**低** = 纯 API 桥或反射工具（本身不产出个人数据，但为上述能力的基础设施）。

---

## 2. 逐类详解

### 2.1 `e0/a` —— BROR / 中国移动 CM 定制机（YunOS）

- 声明：`.class public Le0/a;` / `.super Le0/r;`（`a.smali:1-3`），2282 行，注册键 **`BROR`**（`c2.smali:443-455`）。
- **采集目标**：YunOS/CM 定制 ROM 上的双卡身份 + 小区信息。这类 ROM 把 IMEI 放在**厂商系统属性**而不是 TelephonyManager，所以该类绕过 Framework 直接读属性。
- 关键 API/属性（`a.smali:1961-2260`）：
  - `gsm.yunos.imei0` → `IMEI1`（`:104-112`，`f0/l.j`）；`gsm.yunos.imei1` → `IMEI2`（`:291-297`）
  - `ro.yunos.name`（默认 `"YunOS"`）→ `OS`；`ro.yunos.version` → `OSVersion`
  - `ro.build.version.external` → `SWVersion`；`ro.build.display.hardware.id` → `HWVersion`
  - `ro.product.hardware`、`ro.mediatek.version.release`
  - 反射（`f0/o`）：`getSubscriberId`、`getSimSerialNumber`、`getMsisdn`、`getNetworkOperatorForPhone`、`getCellLocationForPhone`、`getDeviceId`、`getNetworkType`、`getVoiceNetworkType`
  - `CellID0/LAC0/CellID1/LAC1` 从 **SharedPreferences** 读（`f0/l.h(ctx,"CellID0",null)`，`:266-282`、`:437-453`）——即由 `d0/e`/`d0/f` 监听器预先写入的缓存。
- 机型分支（`b()`，`:1959-2260`）：
  - `CM CM1` → `SWVersion=ro.build.version.external`、`HWVersion=ro.build.display.hardware.id`、OS=YunOS，调 `v()`（`:2006`）
  - `CM CM5S` → `HWVersion=ro.product.hardware`，调 `x()`（`:2055`）
  - `CM CM5D` → 同上，**并把 Model 改写为 `MODEL_<ROM>_<RAM>`**（`:2092-2122`，ROM/RAM 取自 `f0/i.e()`/`f0/i.d()`），调 `w()`（`:2124`）
  - `CM CM7S` → `SW/HWVersion=ro.mediatek.version.release`，调 `u()`（`:2164`）
  - `CM CM3`（`:2200`）、`CM CM5`（`:2248`）→ `v()`
  - 其它 → `f0/j.c(ctx,"DeviceInfoReaderBROR unknow model:"+MODEL)`（`:2267`）**只记日志、不降级到通用读取**
- 四个子读取器差异：`u()` 走属性 IMEI + 反射（含 `getMsisdn`）；`v()` 走 `getDeviceId` + 反射；`w()` 走 `getDeviceId(int)`/`getCellLocationForPhone`；`x()` 与 `w()` 同但**不写 CellID/LAC**。
- 触发/开关：无权限、无设置项判断；仅由 `c2.h()` 的厂商匹配进入；`a()` 为恒 false 桩。
- 副作用：仅写 `f0/j` 日志（`f0/j.c` 为"上报错误日志"，见 §5）。
- 隐私：**极高**——双 IMEI（属性直读，不受 READ_PHONE_STATE 变化影响）、IMSI、ICCID、MSISDN、LAC/CID。
- 未知：`f0/j.c(Context,String)` 是否真走网络（属 f0 侧，未验证）。

### 2.2 `e0/a0` —— DOUQI（读曲）：整机 + RAM/ROM/剩余空间

- 1056 行，注册键 **`DOUQI`**（`c2.smali:1304-1312`）。
- **采集目标**：整机标识 + 存储画像（RAM 总量、ROM 总量、可用空间），用于设备画像/容量分档。
- API：
  - `ActivityManager.getMemoryInfo().totalMem` → `RAM`（`z()`，`:393-448`，`ceil(totalMem/1024³)+"G"`）
  - `StatFs(/system).blockCount*blockSize` + `StatFs(/data)` 同上 → 合计后 `t(J)` 取**不小于总容量的最小 2 的幂** + `"G"` → `ROM`（`A()`/`t()`，`:15-125`）
  - `StatFs(/data).availableBlocksLong*blockSizeLong/1024/1024/1024` → `FREE`（`v()`，`:170-270`）
  - `ro.product.manufacturer` → `Manuf`（**不用 `Build.MANUFACTURER`**，`:1021-1027`）
  - `ro.boot.hardware.revision`，空则回退 `"MP_0.1"` → `HWVersion`（`y()`，`:368-391`）
  - `Settings.Global."device_provisioned"` → `f0/f.g`（`u()`，`:127-168`）
  - `e0/i.a(tm,0)`=`getImei(0)` → `IMEI1`；`e0/r.h(0)` → subId0
  - 反射 `TelephonyManager.getSubscriberId(int)`、`getSimSerialNumber(int)`、`getMsisdn(int)`、`getNetworkOperatorForPhone(0)`、`getNetworkType(int)`+`getVoiceNetworkType(int)`
  - `e0/r.r(ctx,0,g)`、`e0/r.d(ctx,0)`（VoLTE）、`e0/r.j(ctx,subId)`（5G）
  - `e0/z.a()` = `SubscriptionManager.getDefaultDataSubscriptionId()`（桥）
- 输出：`SubType` 为 `{RAM,ROM,FREE}` 的 JSONObject（`:300-366`），`DeviceType="000005"`（`:483-485`）；`DataSlot` 判定为 `activeDataSubId==subId0?"1": ==subId1?"2": 不写`（`:981-1003`）。
- 失败分支：`StatFs`/JSON 异常只记日志（`:352-365`）；`subId<0` 时整段槽1读取跳过（`:517`）。
- 隐私：**极高**（IMEI/IMSI/ICCID/MSISDN）+ 设备容量画像。
- 未知：`e0/i`、`e0/z`、`e0/x` 属 API 桥（`e0/i` 在本批，`x`/`z` 不在本批）。

### 2.3 `e0/a1` —— "5G HYUN" / OFeng（5GP50）

- 854 行，注册键 **`5G HYUN`**（`c2.smali:1135-1143`）。
- **采集目标**：MTK 平台白牌 5G 机型，通过 MTK 隐藏服务取双卡信息。
- 关键：`t(Context)Z` **直接 `return true`**（`:15-21`）——即"用户协议/权限"判断被写死为真，之后才 `sput-boolean f0/f.g`；若为假会打印 `"isUserPermissionOpen==false, do not read!"` 并**整体跳过**（`:644-655`）。这是本批唯一"门禁被硬编码放行"的 Reader。
- API/属性：
  - `ro.product.hardware` → `HWVersion`（`u()`，`:23-33`）
  - `android.telephony.TelephonyManagerEx` + `ServiceManager.getService("phone_ex")`（MTK 双卡扩展服务，隐藏 Binder）→ `getImei(int)`、`getCellLocationForPhone`
  - `SubscriptionManager.getSubId(int)`/`getDefaultSubId` 反射（`v()`，`:35-92`）
  - 反射 `getSubscriberId/getSimSerialNumber/getLine1Number/getNetworkOperator/getNetworkType/getVoiceNetworkType`
- 输出：`DeviceType="000005"`（模型 `5GP50` 时，`:817-827`），`DataSlot = getPhoneId(defaultDataSubId)+1`（SDK≥29 用 `e0/x.a`），IMEI1/2、ICCID/IMSI/MSISDN/MNC1/2、NCLS1/2、`5GEnabled1/2`。
- 隐私：**极高**。
- 未知：`TelephonyManagerEx` 具体实现随 MTK 版本变化，失败即 `printStackTrace` 后返回 null（不降级到 AOSP API）。

### 2.4 `e0/a2` —— YuLong / Coolpad（酷派/宇龙）

- 2336 行，注册键 **`YuLong`**（`c2.smali:623-631`）。
- **采集目标**：酷派/宇龙多代机型的硬件版本与双卡信息；**不带 `DeviceType`**（`g.G` 从不写，落默认 `"000"`）。
- 机型 → 属性映射（`b()`，`:2082-2302`）：
  - `Coolpad A8-930 / Y72-921 / Y91-921 / Y92-9 / Y803-9` → `HWVersion=ro.product.hardware`、`SWVersion=ro.build.display.id` → `y()`（`:2164`）
  - `MTS-U0` → 同上（`:2152-2162`）→ `x()`（`:2193`）
  - `Coolpad 5380CA` → `HWVersion=ro.product.hardware.version`、`SWVersion=ro.build.display.id` → `x()`（`:2222`）
  - `Coolpad 5370` → `HWVersion=ro.build.display.hardware` → `w()`（`:2284`）
  - `C106-7 / C106-6 / C105-6` → `HWVersion=ro.config.le_hardware_version`（乐视系）→ `v()`（`:2304`）
  - 其它（YuLong 兜底）→ `HWVersion=ro.yulong.version.hardware`，并在异常时 `f0/j.c(ctx,"DeviceInfoReaderYuLong read() error --> "+msg)`（`:2290-2319`）
- API：MTK `TelephonyManagerEx.getDefault()` + `getCellLocation(int)`；`e0/d2`/`f0/o` 反射 `getImei/getSubscriberId/getSimSerialNumber/getLine1Number/getSimOperator/getSimOperatorNumericForSubscription/getNetworkOperatorForPhone/getMsisdn`；`SubscriptionManager.isValidSubscriptionId`、`getSubId`。
- 隐私：**极高**（4 条读取路径都覆盖 IMEI1/2、IMSI1/2、ICCID1/2、MSISDN1/2、NCLS1/2、CellID1/2、LAC1/2）。
- 未知：4 条路径（v/w/x/y）的细分差异为"API 版本 + 机型批次"组合，未逐一还原其触发阈值。

### 2.5 `e0/b` —— API 桥：`TelephonyManager.getImei()`

- 15 行，`final synthetic`（D8 为 API 级别分支合成的桥）。`b.smali:7-15`：`invoke-virtual {p0}, TelephonyManager->getImei()Ljava/lang/String;`。
- 语义：API 26+ 才有无参 `getImei()`；调用方（如 `e0/c`）先判 `SDK_INT>=26` 再走此桥，否则走 `getDeviceId()`。
- 隐私：本身不写数据，但**它是"取 IMEI"这条最敏感链路的入口**（`e0/c.smali:51`）。

### 2.6 `e0/b0` —— EAMEY

- 1041 行，注册键 **`EAMEY`**（`c2.smali:563-571`）。
- 机型：`EAMEY-M9`、`EAMEY-L9`（`:532-596`）；属性 `ro.build.tyd.custom.hw_version` → `HWVersion`，`ro.build.display.id` → `SWVersion`。
- IMEI 走**属性**：`ril.imei.sim1` → `IMEI1`（`:138-144`）、`ril.imei.sim2` → `IMEI2`（`:323-331`）——RIL 属性直读，绕过 TelephonyManager。
- 其余：MTK `TelephonyManagerEx.getDefault().getCellLocation(int)`（`v()`，`:84-491`）、`e0/d2` 反射（`u()`，`:635-1010`）。
- 隐私：**极高**。

### 2.7 `e0/b1` —— Onefive（全系）+ `/proc/meminfo`

- 2082 行，注册键 **`Onefive`**（`c2.smali:473-481`）。
- **采集目标**：Onefive 多机型（C2/M1/T5/T3/T2S/C5/C3/S1…）双卡信息；额外做内存/存储档位解析（用于型号归一与日志）。
- 独有：`t()` 直接读 **`/proc/meminfo`**（`BufferedReader(new FileReader("/proc/meminfo"))`，`b1.smali:15-192`），按 `MemTotal` 落档到 `256M/512M/768M/1G/2G/3G/4G/8G/16G/32G/64G`；`u(Context)` 用 `StatFs(/data)`+`Formatter.formatFileSize` 生成 `"x.xxG"`（`:193-256`）。本批唯一读 `/proc` 的类。
- 属性：`ro.yunos.name/version`、`ro.build.description`、`ro.product.hardware`、`ro.bd.cmcc_software_version`、`ro.bd.cmcc_hardware_version`、`ro.bird.custom.sw.version`。
- 分支：`Onefive C2`→`x()`、`M1/T5`→`y()`、`T3`/`C5`/`C3`→`y()`/兜底、`S1`→`w()`（public，供外部调用）、另有 `e0/r.p()`（`:1667`）。
- 输出：全套身份键（IMEI1/2、ICCID1/2、IMSI1/2、MSISDN1/2、MNC1/2、NCLS1/2）+ `Manuf/Model/HWVersion/SWVersion/OS/OSVersion`；**不写 DeviceType**。
- 隐私：**极高**；`/proc/meminfo` 属系统信息（低敏感）。

### 2.8 `e0/b2` —— ZTE（中兴）

- 1804 行，注册键 **`ZTE`**（`c2.smali:653-661`）。
- 门禁：`Settings.Secure."dm_user_agreement"==1` → `f0/f.g`（`t()`，`:15-103`），失败仍继续读。
- 机型：`ZTE A2020N2 Pro`、`ZTE 9000N`、`ZTE 7532N` → `DeviceType="000005"`（`:1310-1390`）；`HWVersion/SWVersion` 均取 `ro.boot.hardware.revision`（`f0/l.k` 带默认值 `"Unknown"`）。
- **双厂商栈**：
  - `com.android.vts.VendorTelephonyManager.getDefault().getCellLocation(int)`（`v()`，用于取 CellLocation；空/异常时 `Log.e("getZTECellLocation object is null"/"exception")`）
  - `com.dmyk.android.telephony.DmUnicomAbsTelephonyManager.getDefault().getVoLTEState()`（DeMai/联通定制）→ `VolteEnabled1/2` = `"1"/"0"/"-1"`
- AOSP 侧：`TelephonyManager.createForSubscriptionId(int)`、`getDeviceId(int)`、`getLine1Number()`、`getSimSerialNumber()`、`getSubscriberId()`、`getNetworkOperator()`；`SubscriptionManager.getDefaultDataSubscriptionId()`+`getPhoneId` 反射。
- 权限自检：`READ_PHONE_STATE`/`READ_PHONE_NUMBERS`（`j/a.a`），无权限时打印 `"readAndroidR no READ_PHONE_STATE PERMISSION_GRANTED"` 并跳过身份读取（`w()`）。
- 运营商判定：`46009/46006/46001`（联通）用于 VoLTE/DataSlot 覆盖（`:1415-1419`）。
- 隐私：**极高**。
- 未知：`com.dmyk.*`、`com.android.vts.*` 属定制 ROM 私有 API，非公版；其返回语义只能按其方法名推断。

### 2.9 `e0/c` —— Baidu/展讯（sprd，XDM-01）

- 205 行，注册键 **`sprd`**（`c2.smali:1032-1040`）。
- **采集目标**：小体量"全量读一次"的极简实现，`DeviceType` 硬编码 `007001029`（Baidu 侧设备类型码）。
- 门禁：`Settings.Secure."dm_user_agreement"==1` → `f0/f.g`（`t()`，`:15-38`）。
- API：`e0/b.a(tm)`（`getImei()`，API26+）→ `IMEI1`；`getSubscriberId/getSimSerialNumber/getLine1Number/getNetworkOperator`；`getNetworkType`（为 0 再 `getVoiceNetworkType`）→ `e0/r.g()` → `NCLS1`；`getCellLocation()` → `d0/g.f()`。
- 机型仅 `XDM-01`（`:184-190`），其它只记 `"DeviceInfoReaderBaidu module unknow!!!"`；`OS=Android`、`OSVersion=RELEASE`、`SWVersion=Build.DISPLAY`、`HWVersion=ro.product.hardware`、`SubType=f0/i.c()`、`DataSlot` 硬编码 `"1"`（`:160-162`）。
- 隐私：**极高**（但只单卡）。

### 2.10 `e0/c0` —— EWPE / Gree（格力）厂商 AIDL

- 557 行，注册键 **`EWPE`**（`c2.smali:1062-1070`）。
- **采集目标**：通过格力私有 AIDL（`f0/f.B : c0/a`，"iGreeTelephony"）取**双卡完整身份**，因为公版 API 在该 ROM 上被裁剪。
- 入口保护：`if (f0/f.B == null) { f0/j.b("DeviceInfoReaderEWPE","iGreeTelephony==Null !!!"); return; }`（`:353-363`）——**没有厂商服务就整体放弃**，不降级。
- AIDL 方法语义（`c0/a`，据调用点推断）：`n()/p()/l()/h()` 返回各卡槽索引；`m(slot,key,def)` 读属性；`e(slot)`=ICCID；`b(slot)`=MSISDN；`c(slot)`=运营商数字；`a(slot)`=网络类型；`d(slot)`=LAC；`g(slot)`=CellID；`j(slot)`=VoLTE 布尔。
- 输出：`IMEI1/2 ← m(slot,"persist.sys.fota_deviceid1/2","")`；`IMSI1/2 ← m(slot,"persist.sys.card.imsi","")`；`ICCID/MSISDN/MNC/NCLS/LAC/CellID` 全部来自 AIDL；`DeviceType="000005"`；`DataSlot=getPhoneId(...)+1`（`:408-499`）；`VolteEnabled1/2="1"`（AIDL 布尔为真时）。
- 门禁：`Settings.Secure."operator_service_improve_plan"==1` → `f0/f.g`（`t()`，`:15-38`）。
- 机型：`G0515D`（`:520-528`），`HWVersion=ro.hw_version`，`SWVersion=Build.DISPLAY`。
- 异常：`RemoteException` → `printStackTrace` + `f0/j.b("DeviceInfoReaderEWPE", msg)`（`:328-343`）。
- 隐私：**极高**，且**数据源是厂商服务而非 Android Framework**（隐蔽性更强）。

### 2.11 `e0/c1` —— OPPO / ColorOS（110+ 机型白名单）

- 2362 行，注册键 **`OPPO`**（`c2.smali:788-796`）。
- **采集目标**：OPPO 全系（Reno/Find/K/A 系列共 110+ 型号，`c1.smali:1263-2186`）双卡身份；`DeviceType` 按机型分两档：**`000005`**（较新/5G 机型，`:2203`）与 **`000004`**（较旧机型，`:2225`）。
- 独有 API：
  - `android.telephony.ColorOSTelephonyManager.getDefault()`（反射）→ `getImei/getVoiceNetworkType/getCellLocation/getNetworkType/getNetworkOperator/getLine1Number/getSimSerialNumber/getSubscriberId`
  - `ro.boot.dram_info`（`x()`：按 `,` 分割取最后一段、`replaceAll("\\D+","")` 解析为 GB 数）→ 用于 RAM 分档；无该属性打印 `"No dram_info property"`（`f1` 是同一套逻辑的 OnePlus 版）
  - VoLTE：`u(Context,int slot)`（`c1.smali:2255-2362`）反射 `TelephonyManager.isOemEnhanced4gLteModeSettingEnabledByUser(int)`，日志 `"checkVolteState slotId:/enable:"`，返回 `"1"/"0"/"-1"`；由 `x()` 在 `c1.smali:466`（槽1）、`:722`（槽2）调用写入 `VolteEnabled1/2`
  - `ActivityManager.MemoryInfo` + `Settings.Global."device_provisioned"`
  - `b()` 末尾（`:2244/:2249`）先 `e0/r.l()` 再做 OPPO 专有 `x()` 覆盖，属"通用读 + 厂商读"叠加
- 属性：`ro.build.hardware.version` → `HWVersion`，`ro.build.display.id` → `SWVersion`。
- 输出：全套身份键 + `VolteEnabled1/2`（`"1"/"0"/"-1"`）+ `5GEnabled1/2` + `DataSlot`；`SubType = c1.w(ctx)`，键为 `{RAM: c1.v(ctx)+"G", ROM: f0/i.e(), FREE: f0/i.a()}`（`c1.smali:87-168`，在 `:1137` 写入 `g.F`），RAM 优先取 `ro.boot.dram_info`、失败回退 `MemoryInfo.totalMem`。
- 隐私：**极高**。
- 未知：白名单里 110+ 型号的"两档 DeviceType"边界规则（哪些算 5G/新机）未逐条还原。

### 2.12 `e0/c2$a` —— 采集收尾回调（`d0/i$a`）

- 164 行。`a(Context,d0/g,List<f0/b>)`：
  1. `f0/f.g = c2.a(ctx)`（`c2$a.smali:44-48`），即 `device_provisioned==1`；
  2. 若为真：`c2.c(list)` 存入静态 `c2.c`（`:52-54`），遍历 `f0/b` 逐个 `a()` 得 JSONObject 塞进 `JSONArray`（`:60-92`）；
  3. 写 `g.L = AppInfo`（`:96`），并打印 `"appInfo:"+json` 与 `"appInfolength:"+n`（`:102-138`）——**应用清单会整份进 logcat**；
  4. `f0/f.b` 为真 → `new d0/h(ctx,g,c2$b)`（IPv4/IPv6 + 运营商线程），否则 `new d0/k(ctx,g)`；两者都 `Thread.start()`（`:141-161`）。
- 隐私：**高**（已安装应用清单，含包名/版本，属强画像数据）。
- 未知：`f0/f.b` 的赋值点不在本批（f0 侧），其语义（是否"支持双栈 IP"）未确证。

### 2.13 `e0/c2$b` —— 公网 IP 回调（`d0/h$a`）

- 76 行。`a(ctx,g,ipv4,ipv6)`：`g.D=ipv4`、`g.E=ipv6`（`c2$b.smali:34-38`），打印 `"Ipv4:…, Ipv6:…"`，随后再 `new d0/k(ctx,g).start()`（`:69-73`）。
- 联动：只有 `d()` 序列化会输出 `IPv4/IPv6`（受 `f0/f.b` 控制，`g.smali:797-831`），`e()` 不输出。
- 隐私：**高**（公网 IP = 网络侧定位/关联标识）。

### 2.14 `e0/c2$c` —— 厂商 Reader 接口

- 22 行：`a(Context)Z`（是否适用）、`b(Context,d0/g)V`（执行采集）。仅 `c2.j()/c2.n()` 调用（`c2.smali:3413/3768`）。
- 说明：`a()` 的返回值语义在实现层退化为"恒 false 桩"（见 §5）。

### 2.15 `e0/d` —— API 桥：`unregisterTelephonyCallback`

- 15 行，`TelephonyManager.unregisterTelephonyCallback(TelephonyCallback)`（API 31+）。与 `e0/k`（注册）成对，被 `e0/r.s(ctx)` 用于停采集（`r.smali:403/438`）。

### 2.16 `e0/d0` —— Gionee（金立）

- 704 行，注册键 **`gionee`**（`c2.smali:972-980`）。
- 门禁：`device_provisioned==1` **且** `Settings.Secure."operator_registration_switch"==1`（`t()`，`:15-103`），日志 `"checkUserPermission:"+bool`。
- 机型：`20190619G` → `DeviceType="000004"`（`:274-290`）；`HWVersion=ro.ssui.hardwarever`、`SWVersion=ro.ssui.sv.version`（`u()/v()`，`:105-127`）。
- API：MTK `TelephonyManagerEx.getDefault()` + `getCellLocation(int)`（`w()`，`:302-703`），`e0/d2` 反射 `getDeviceId/getSubscriberId/getSimSerialNumber/getLine1Number/getNetworkOperator(ForPhone)`、`e0/r.f`。
- 输出：IMEI1/2、IMSI1/2、ICCID1/2、MSISDN1/2、MNC1/2、NCLS1/2、CellID1/2、LAC1/2、`DataSlot`、`SubType`。
- 隐私：**极高**。

### 2.17 `e0/d1` —— ORANGE（锐嘉科 RGK 代工）

- 1487 行，注册键 **`ORANGE`**（`c2.smali:578-586`）。
- 机型：`D5277CT`、`D5377CT`、`GO 55307`、`D5287CT`（`:1320-1414`）；`HWVersion=ro.qc.hardwareversion`、`SWVersion=ro.qc.softversion`。
- **IMEI 走属性**：`gsm.rgk.imei1` → `IMEI1`、`gsm.rgk.imei2` → `IMEI2`（`:31-37`、`:236-242`）。
- 三条路径 `v()/u()/t()`（`:827`、`:421`、`:15`）：`v()` 用 `f0/o` 反射带 `getCellLocation`，`u()/t()` 用 `e0/d2` + SharedPreferences 里的 `CellID0/LAC0/CellID1/LAC1`。
- 未匹配机型 → `f0/j.c(ctx,"DeviceInfoReaderORANGE unknow model:"+MODEL)`（`:1439`）。
- 隐私：**极高**。

### 2.18 `e0/d2` —— 反射工具（分槽 TelephonyManager）

- 291 行。核心是**重载自适应**：
  - `c(TM, nameByInt, nameByLong, subId)`（`:47-130`）：先 `TelephonyManager.class.getDeclaredMethod(nameByInt, int.class).invoke(tm, subId)`；`NoSuchMethodException` 时改 `(nameByLong, long.class)` 并 `int→long` 传参。
  - `b(TM,name,subId)`（`:37`）= `c(tm,name,name,subId)`。
  - `a(int slot)`（`:7-35`）→ `d(int)`（`:132-291`）：`SubscriptionManager.getSubId(int)` 返回 `long[]`，逐元素 `long→int`；`ClassCastException` 时按 `int[]` 重试。
- 意义：**这是整套"分槽取 IMSI/ICCID/号码/运营商/网络类型"的通用能力层**，同时兼容 Android 5→13 的 `int/long` subId 签名漂移。异常一律 `printStackTrace`，失败返回 null（调用方再判空跳过）。
- 隐私：自身不产出数据，但**是 f0/d0/d1/i1/j1/b0/b1 等 Reader 获取个人身份数据的唯一通道**。

### 2.19 `e0/e` —— API 桥：`CellIdentityNr.getNci()`

- 15 行。NR（5G）小区标识 NCI（`long`）。被 `e0/r.r()` 用于填 `CellID2`（`r.smali:2887-2891`）。语义：5G 基站小区唯一号，**配合 TAC 可定位到街区级**。

### 2.20 `e0/e0` —— TOSOT（大松/格力，"Gree"）

- 555 行，注册键 **`TOSOT`**（`c2.smali:1199-1207`）。
- 门禁：`device_provisioned==1` → `f0/f.g`（`t()`，`:15-64`）。
- 机型：`G0615D` → `DeviceType="000005"`、`SWVersion=ro.build.display.id`、`HWVersion=ro.hw_version`（`u()`）；`SDK>=30` 才进 `v()`（厂商 AIDL）。
- `v()`（`:78-346`）与 `e0/c0` 同构：`f0/f.B` 为空则 `f0/j.b("DeviceInfoReaderGree","iGreeTelephony==Null !!!")` 直接返回；否则 `IMEI1/2 ← persist.sys.fota_deviceid1/2`、`IMSI1/2 ← persist.sys.card.imsi`、ICCID/MSISDN/MNC/LAC/CellID 来自 AIDL，`NCLS1/2 ← f0/f.n/o`，`VolteEnabled1/2="1"`，`5GEnabled1/2` 由 `e0/r.j()` 决定。
- 隐私：**极高**（数据源为厂商服务）。

### 2.21 `e0/e1` —— OYSIN（欧信）

- 1217 行，注册键 **`OYSIN`**（`c2.smali:488-496`）。
- 机型：`OYSIN X5`、`OYSIN X8`、`MX6`、`P7`（`:1050-1181`）；属性 `ro.yunos.name/version`、`ro.product.hardware`、`ro.hardware.version`、`ro.xh.display.version`。
- 路径：`X5`/`X8` → `v()`（`f0/o` 反射）；`MX6` → `w()`（MTK `TelephonyManagerEx`）；`P7` → `e0/r.p()`（基类反射全量读）。未匹配 → `f0/j.c(...unknow model)`。
- 隐私：**极高**。

### 2.22 `e0/f` —— API 桥：`CellIdentityNr.getTac()`

- 15 行。NR TAC（跟踪区码，`int`）。与 `e0/e` 配对，被 `e0/r.r()` 写入 `LAC2`（`r.smali:2895`）。语义：5G 跟踪区，配合 NCI 定位。

### 2.23 `e0/f0` —— Haipainoble（海派诺伯）

- 1121 行，注册键 **`Haipainoble`**（`c2.smali:927-935`）。
- 机型：`Haipainoble S9`、`Haipainoble S18`（`:172-260`）；属性 `ro.yunos.name/version`、`ro.hardware.version`、`ro.build.display.id`。
- **MTK 隐藏 Binder 直连**（`t()`，`:15-127`）：
  `Class.forName("com.mediatek.internal.telephony.ITelephonyEx$Stub").getDeclaredMethod("asInterface", IBinder.class)` +
  `ITelephonyEx.getDeclaredMethod("getSvlteImei", int.class)` +
  `ServiceManager.getMethod("getService", String.class).invoke(null,"phoneEx")` → `getSvlteImei(slot)`。
  `getSvlteImei` = **SVLTE（单卡双待 LTE）场景下的 IMEI**，公版 API 拿不到。
- 其余：`e0/d2` 反射 + `e0/r.f`；`u()`/`v()` 两条路径分别对应"有/无 `getCellLocation` 反射"。
- 隐私：**极高**（IMEI 来自 Binder，且是"隐藏 IMEI"）。

### 2.24 `e0/f1` —— OnePlus（LE2120/LE2110/LE2100）

- 759 行，注册键 **`OnePlus`**（`c2.smali:1120-1128`）。
- **采集目标**：一加 9 系列整机 + RAM/ROM 画像。
- RAM 双路：`x()` 读 `ro.boot.dram_info`（`f0/l.k`，取最后一段数字，`:398-510`）；失败则 `w()` 用 `totalMem/1MB` 落**档位表**（`0x7800→32`、`0x7000→30`、`0x6800→28`…`0x300→1`，`:170-396`）——即"标称 RAM 档"而非真实值，属于**规避精确取值**的写法。
- ROM/FREE：`f0/i.e()`/`f0/i.a()`（`:108-116`）→ `SubType{RAM,ROM,FREE}`（`:87-168`）。
- `HWVersion = Build.DISPLAY.split("_")[1]`（`:718-741`）；`DeviceType="000005"`；`SDK>=30` 走 `e0/r.l()` 全量读，否则打印 `"DeviceInfoReaderOnePlus no API"` 并**放弃身份读取**（`:743-757`）。
- 隐私：**极高**（但仅 API≥30 才读身份）。

### 2.25 `e0/g` —— API 桥：`ImsManager.getImsMmTelManager(int)`

- 15 行。`ImsManager.getImsMmTelManager(subId)`（ImsMmTelManager 属 `@SystemApi`，普通应用需反射）。被 `e0/r.c/d()` 用于读 VoLTE 开关（`r.smali:584/712`）。

### 2.26 `e0/g0` —— Hisense（海信）

- 2099 行，注册键 **`Hisense`**（`c2.smali:638-646`）。
- 门禁：`device_provisioned` → `f0/f.g`；`Settings.Secure."operator_service_improve_plan"`（`t()`）。
- 机型分档（`:1847-2036`）：
  - `HLTE226T/HLTE227T/HLTE260T/HLTE217T` → `DeviceType="000004"` → `z()`
  - `HNR550T` → `DeviceType="000005"` → `A()` + `e0/r.l()`（`:2016`）
  - `HNR560T/HNR553T` → `DeviceType="000005"` → `B()`
  - `HNR550T` 特例：`DeviceType` 被直接写成机型名 `"HNR550T"`（`:2024`）——**DeviceType 字段被复用为型号字符串**（契约污染）。
- 属性：`ro.hardware.version`、`ro.software.version`、`ro.hmct.volte.enabled`、`persist.sys.volte.enable`（VoLTE 判定，`:988-1010`）。
- 输出：IMEI1/2、ICCID1/2、IMSI1/2、MSISDN1/2、MNC1/2、NCLS1/2、Volte1/2、`SubType`、`DataSlot`。
- 隐私：**极高**。

### 2.27 `e0/g1` —— PHICOMM（斐讯）

- 1242 行，注册键 **`PHICOMM`**（`c2.smali:803-811`）。
- 机型：`C832Lw`（主）、`PHICOMM E1030`、`C1230L`、`C1530L`、`C1330`（`:1000-1196`）。
- **独有技巧：反射 `android.os.Build` 的厂商注入静态字段**（`g1.smali:974, 1011-1113`）：
  `const-class Landroid/os/Build;` → `getField("OS_NAME").get(null)` → `OS`；`OS_VERSION` → `OSVersion`；`HARDWARE_VERSION_DISPLAY` → `HWVersion`；`OUTER_VERSION_DISPLAY` → `SWVersion`。
  这些字段在 AOSP 不存在，只有斐讯/YunOS 定制 Build 才有 —— 属于**用反射探测厂商私有 Build 字段**的典型手法；异常时回落 `null`（字段被清空）。
- 其它：`YunOS` 分支用 `ro.yunos.name/version` + `ro.internal.version` + `ro.build.display.hw.version`；老机型用 `ro.build.display.soft` + `ro.hardware.version`。
- 隐私：**极高**。

### 2.28 `e0/h` —— API 桥：`ImsMmTelManager.isAdvancedCallingSettingEnabled()`

- 15 行。VoLTE（高级通话）开关读回。`e0/r.c/d()` 在 `setAdvancedCallingSettingEnabled(true)` 前后各读一次以**强制开启并确认**（`r.smali:584-643`）——即**会修改用户的 VoLTE 设置**（副作用，不只是读）。

### 2.29 `e0/h0` —— HUAWEI（EVR-AN00）

- 883 行，注册键 **`HUAWEI`**（`c2.smali:428-436`）。
- 机型：`EVR-AN00` → `DeviceType="000005"`、`HWVersion=ro.product.hardwareversion`、`SWVersion=Build.DISPLAY`（`b()`，`:692-792`）。
- **华为私有 API**：
  - `Class.forName("android.telephony.HwTelephonyManager").getDeclaredMethod("getDefault").invoke(CLASS)`（注意：对 **Class 对象** invoke，静态方法）→ 实例；再 `getDeclaredMethod("getCellLocation", int.class)` → 分槽 CellLocation（`t()`，`:39-119`）；异常 `Log.e("getHWCellLocation exception")`。
  - `com.huawei.android.telephony.TelephonyManagerEx.getDefault4GSlotId()` → 默认 4G 槽（`u()`，`:795-883`），据此决定 `getImei(slot)` 的取哪个槽；空则 `Log.i("getDefault4GSlotId return null")` 并回 `-1`。
- 反射 `TelephonyManager.getImei(int)`、`getSubscriberId(int)`、`getSimSerialNumber(int)`、`getNetworkType(int)`、`getSimOperator(int)`、`getLine1Number(int)`→回退 `getLine1NumberForSubscriber(int)`、`getNetworkOperator(int)`→回退 `getSimOperator(int)`。
- 注意实现细节：`u(0)` 与 `u(1)` 在两条分支上**都返回 `getImei(getDefault4GSlot())`**（`:852-880`），因此 `IMEI1`/`IMEI2` 很可能相同（同一张卡的 IMEI 被写两次）——见 §5。
- 隐私：**极高**。

### 2.30 `e0/h1` —— PHILIPS（E266W）

- 664 行，注册键 **`PHILIPS`**（`c2.smali:867-875`）。
- 机型：`E266W`（`:628-652`）；`HWVersion=ro.custom.hw.version`（默认 `"MBV1.0"`）、`SWVersion=Build.DISPLAY`。
- 独有：`t(int subId)Z` 反射 `SubscriptionManager.isValidSubscriptionId(int)` 并 **`setAccessible(true)`**（`:15-71`，违反隐藏 API 反射限制的常用绕法），据此决定是否读该槽。
- `u()`（`:73-585`）全 `f0/o` 反射：`getDeviceId(int)`、`getSubscriberId(int)`、`getSimSerialNumber(int)`、`getLine1NumberForSubscriber(int)`、`getNetworkOperatorForSubscription(int)`、`getNetworkType(int)`→0 时 `getVoiceNetworkType(int)`、`getCellLocationForPhone(int)`。
- 隐私：**极高**。

### 2.31 `e0/i` —— API 桥：`TelephonyManager.getImei(int slot)`

- 15 行。API 26+ 分槽 IMEI。被 `e0/a0`、`e0/j0`、`e0/k1`、`e0/r.l/o` 使用（如 `a0.smali:489`、`r.smali:1280`）。**这是"双 IMEI"采集的关键入口**。

### 2.32 `e0/i0` —— API 桥：`SubscriptionManager.getSubscriptionIds(int slot)`

- 15 行。API 30+ 由槽取 subId 数组（替代 `getSubId(int)`）。被 `e0/j0/k1` 使用。

### 2.33 `e0/i1` —— PPTV（KING 7S / PP5200 / PP5600）

- 851 行，注册键 **`PPTV`**（`c2.smali:743-751`）。
- 机型与属性（`:170-256`）：
  - `KING 7S` → `SWVersion=Build.DISPLAY`、`HWVersion=ro.cust.hwversion`（`trim()`）→ `e0/r.p()`
  - `PP5200` → `SWVersion=ro.build.custom.display.id`、`HWVersion=ro.product.hardware.version` → `u()`
  - `PP5600` → `SWVersion=ro.pptv.version`、`HWVersion=Build.HARDWARE` → `u()`
- **IMEI 来自 MTK 隐藏 Binder**：`t()`（`:15-127`）同 `e0/f0` 的 `getSvlteImei(slot)`（`ITelephonyEx$Stub.asInterface(ServiceManager.getService("phoneEx"))`）。
- 其余：`e0/d2` 反射 + `f0/l.h` 读 SharedPreferences 的 `CellID0/LAC0/CellID1/LAC1`。
- 隐私：**极高**。

### 2.34 `e0/j` —— API 桥：`Context.getMainExecutor()`

- 15 行。API 28+；供 `registerTelephonyCallback` 使用（`r.smali:253/330`）。

### 2.35 `e0/j0` —— HZTL（TAI-M01，实为华为平台）

- 2063 行，注册键 **`HZTL`**（`c2.smali:1259-1267`）。
- 机型：`TAI-M01` → `DeviceType="000005"`、`SWVersion=Build.DISPLAY.split("\\s+")[?]`、`HWVersion=ro.product.hardwareversion`（`:1991-2023`）；另有 `TAI-M01` 之外的 `read8PAD` 日志标记（`:2036`）。
- **门禁三重**：`device_provisioned`、`READ_PHONE_STATE`、`READ_PHONE_NUMBERS`（`j/a.a`）；无 `READ_PHONE_STATE` 时 `"readAndroidR no READ_PHONE_STATE PERMISSION_GRANTED"` 并跳过身份（`t()`，`:33-100`）。
- 华为私有 API（反射）：`android.telephony.HwTelephonyManager.getDefault()` 的 `getImei`、`getSubscriberId`、`getServiceAbility`、`isVolteAvailable`；`android.telephony.MSimTelephonyManager`（`"from"` 字段/方法）；`getServiceAbility` 返回值映射 `SA`/`NSA`（5G 组网模式）。
- 其他：`Settings.Global."preferred_network_mode"`、`e0/b.a`(getImei)、`e0/i.a`(getImei(int))、`e0/i0.a`(getSubscriptionIds)、`e0/r.c/r.d`(VoLTE)、`e0/r.r`(小区)、`ro.boot.dram_info` RAM 解析。
- 运营商：`46009/46006/46001` 联通判定（`:141-147`）。
- 隐私：**极高**（IMEI/IMSI/ICCID/MSISDN/CellID/LAC/5G 组网）。
- 未知：`MSimTelephonyManager` 若不存在，相关分支静默失败（异常仅 `printStackTrace`）。

### 2.36 `e0/j1` —— PROTRULY（D7 / D8）

- 487 行，注册键 **`PROTRULY`**（`c2.smali:912-920`）。
- 机型：`PROTRULY D7`、`PROTRULY D8`（`:416-432`）；`SWVersion=Build.DISPLAY`、`HWVersion=ro.hardware_version`。
- 全文仅 `e0/d2` 反射（`getDeviceId/getSubscriberId/getSimSerialNumber/getLine1NumberForSubscriber/getNetworkOperator(ForSubscription)/getNetworkType`）+ `f0/l.h` 读 `CellID0/LAC0/CellID1/LAC1`；`b()` 外层 `try/catch(Exception)` 包住 `t()`（`:473-485`）——**任何异常只打栈、不上抛**。
- 隐私：**极高**。

### 2.37 `e0/k` —— API 桥：`registerTelephonyCallback`

- 15 行。`TelephonyManager.registerTelephonyCallback(Executor, TelephonyCallback)`（API 31+）。被 `e0/r.k(ctx)` 用于注册双槽监听（`r.smali:259/336`），是**持续监听网络/信号变化**的入口。

### 2.38 `e0/k0` —— INTKI（E9000 / E9003）

- 225 行，注册键 **`INTKI`**（`c2.smali:533-541`）。
- 字段：`b:Class`（= `Class.forName("android.telephony.TelephonyManager")`，构造时缓存）、`c:TelephonyManager`（`f0/f.f` 取 `phone` 服务）。
- 独有：反射 `TelephonyManager.getCustomHWBuildVer()` → `HWVersion`、`getCustomSWBuildVer()` → `SWVersion`（厂商在 TelephonyManager 上挂的私有方法）。
- 机型：`E9003`/`E9000`（`:147-163`）；`OS=ro.yunos.name`（默认 `YunOS`）、`OSVersion=ro.yunos.version`；随后 `e0/r.p()` 全量读。
- 隐私：**极高**。

### 2.39 `e0/k1` —— PTAC（华为智选 FIO/Hebe/Hera/MGZ/ALH/MIZ/MRK-BD00）

- 2132 行，注册键 **`PTAC`**（`c2.smali:1244-1252`）。
- 机型白名单（`:1998-2064`）：`FIO-BD00`、`Hebe-BD00`、`Hera-BD00`、`MGZ-BD00`、`ALH-BD00`、`MIZ-BD00`、`MRK-BD00` → `DeviceType="000005"`、`SWVersion=Build.DISPLAY.split("\\s+")`、`HWVersion=ro.board.hardware_version`（`:2091-2113`）。
- **与 `e0/j0` 近乎逐行同构**（同门禁、同华为 API、同 `read8PAD` 日志），差异：
  - `k1` 在 `x()` 中**同时打印两槽**的小区信息：`"CellID1 : …, LAC1 : …"`（`:1072-1084`）与 `"CellID2 : …, LAC2 : …"`（`:1465-1477`）；`j0` 只有槽1 的 `"CellID1 : "`（`:1079`）与一条 `"readHZTL-Q->CellID1 : "`（`:618`）
  - `j0` 额外用 `e0/i0.a`（`getSubscriptionIds(int)`，`:1861/:1867`）取 subId；`k1` 不用该桥，改走 `e0/x.a`+`getPhoneId` 反射算 `DataSlot`
  - 两者都用 `e0/b.a`（`getImei()`，`j0:93`/`k1:111`）与 `e0/i.a`（`getImei(int)`，`j0:103,113`/`k1:121,131`）
- 隐私：**极高**。
- 未知：`CellID3/LAC3` 的第三套写入（`:1455-1459`）落到 `g.x/g.y`（CellID2/LAC2），与"槽2"复用同一键——**存在键冲突/覆盖**，但无法确定地判断哪次写入最终生效（取决于分支）。

### 2.40 `e0/l` —— API 桥：`ServiceState.getNetworkRegistrationInfoList()`

- 15 行。API 28+；`e0/r.r()` 用它枚举注册网络并按 `NetworkRegistrationInfo` 取 `CellIdentity*`（`r.smali:2662-2900`）。是"**不依赖 `getCellLocation()` 也能拿小区标识**"的关键（`getCellLocation` 在高版本受限）。

### 2.41 `e0/l0` —— API 桥：`CellInfoNr.getCellIdentity()`

- 15 行。API 29+；从 `CellInfoNr` 取 `CellIdentity`（配合 `e0/e`/`e0/f` 取 NCI/TAC）。

### 2.42 `e0/l1` —— QIKU（奇酷 1501-A02）

- 108 行（本批最短的 Reader），注册键 **`QIKU`**（`c2.smali:548-556`）。
- 机型：`1501-A02` → `SWVersion=ro.build.description`、`HWVersion=ro.yulong.version.hardware`，随后 `e0/r.p()`（`:56-86`）。
- `Manuf = c2.a.get(Build.MODEL)`（`:38-46`）；未匹配 → `f0/j.c(ctx,"DeviceInfoReaderQIKU unknow model:"+MODEL)`。
- 隐私：**极高**（全部身份数据由基类 `p()` 采集）。

---

## 3. 交叉发现（跨类规律）

1. **`a(Context)` 几乎是废桩。** 16 个 Reader（a, a1, a2, b0, b1, b2, d1, e1, f0, g1, h0, h1, i1, j1, k0, l1）的 `a()` 全部是同一段代码：`new ServiceState()` → `getRoaming()` → 返回。新建的空 `ServiceState` 恒 `roaming=false`，因此 **`c2.j()/c2.k()` 恒得到 false**，"Reader 是否适用"实际上完全由 `c2.h()` 的厂商字符串匹配 + 各 `b()` 内部的 `Build.MODEL.equals(...)` 决定。`a()` 是遗留/占位接口。
2. **厂商分发基于 `Build.MODEL` 的静态映射表 `c2.a`**（`Map<MODEL,String>`），`c2.h()` 再用**厂商字符串**做 60+ 路 `equals` 分支。这意味着：**改机型（改 `ro.product.model`）即可切换到另一个厂商的采集器**，而每个采集器的属性键/反射 API 完全不同 —— 采集侧对"机型伪装"没有任何一致性校验。
3. **"用户同意"开关有 5 种互不相同的实现**：
   - `Settings.Global."device_provisioned"==1`（a0, b2?, c1, d0, e0, f1, j0, k1, c2$a/c2.e）
   - `Settings.Secure."dm_user_agreement"==1`（c, b2）
   - `Settings.Secure."operator_service_improve_plan"==1`（c0, g0）
   - `Settings.Global."device_provisioned" && Settings.Secure."operator_registration_switch"==1`（d0）
   - **`return true` 硬编码**（a1，`a1.smali:15-21`）
   且阈值全部是 `==1`（默认 0 = 不允许），但这些 key 都是**运营商/厂商私有 Settings 键**，普通应用读它们不需要任何权限。
4. **IMEI 的 6 条互不相同的取法**，说明目标就是"绕开 AOSP 限制也要拿到 IMEI"：
   ① `getImei()`（API26+, `e0/b`）；② `getImei(int)`（`e0/i`）；③ `getDeviceId()`/`getDeviceId(int)`（`e0/r.m/o/p`）；④ **系统属性**（`gsm.yunos.imei0/1`、`ril.imei.sim1/2`、`gsm.rgk.imei1/2`、`persist.sys.fota_deviceid1/2`）；⑤ **MTK 隐藏 Binder** `ITelephonyEx.getSvlteImei(int)`；⑥ **厂商 AIDL**（iGreeTelephony `m(slot,"persist.sys.fota_deviceid…")`）。
5. **`5GEnabled1/2` 的实现是"读 `Settings.Global.preferred_network_mode<subId>`"**（`e0/r.j`，`r.smali:1168-1240`），返回布尔 → `"1"/"0"`。这不是"手机是否支持 5G"，而是"用户当前首选网络模式"。含义容易被误读。
6. **VoLTE 采集带写副作用**：`e0/r.c()` 先反射 `setAdvancedCallingSettingEnabled(true)` 再读回（`r.smali:614-643`），即**为了读出 `"1"` 会先把用户的 VoLTE 开关打开**。失败返回 `"-1"`。
7. **`DeviceType` 语义混乱**：`"000004"`/`"000005"` 是厂商/制式分档码（c1/g0/d0/g0 各机型不同），`e0/c` 用 `"007001029"`（Baidu 自有编码），`e0/g0` 甚至把机型名 `"HNR550T"` 写进 `DeviceType`（`g0.smali:2024`）。该字段在下游（f0/g 上报）如何被解释，本批无法确证。
8. **`DataSlot` 有 4 种算法**：`getDefaultDataSubscriptionId()==subId0?"1":"2"`（a0）、`getPhoneId(defaultDataSubId)+1`（a1/b2/c0/e0/f1/g0/j0/k1/c1，SDK≥29 走 `e0/x.a`，否则反射 `getPhoneId`）、`getDefaultDataSubscriptionId()+1`（h0, d0 的反射版）、硬编码 `"1"`（c）。**含义不统一（0 基/1 基混用）**。
9. **`SubType{RAM,ROM,FREE}` 是唯一被广泛填充的结构化子对象**：RAM 有 `MemoryInfo.totalMem`（a0/f1/c1/j0/k1）、`ro.boot.dram_info`（c1/f1/j0/k1）、`/proc/meminfo`（b1）、档位表（f1）；ROM/FREE 有 `StatFs`（a0/b1）与 `f0/i.e()/a()`（f1/c1/g0/j0/k1）。
10. **大量机型专属"隐藏属性"清单**（可作为指纹表）：`gsm.yunos.imei0/1`、`ril.imei.sim1/2`、`gsm.rgk.imei1/2`、`persist.sys.fota_deviceid1/2`、`persist.sys.card.imsi`、`ro.boot.hardware.revision`、`ro.boot.dram_info`、`ro.ssui.hardwarever`、`ro.ssui.sv.version`、`ro.custom.hw.version`、`ro.cust.hwversion`、`ro.hmct.volte.enabled`、`persist.sys.volte.enable`、`ro.board.hardware_version`、`ro.product.hardwareversion`、`ro.config.le_hardware_version`、`ro.yulong.version.hardware`、`ro.qc.hardwareversion`、`ro.qc.softversion`、`ro.internal.version`、`ro.xh.display.version`、`ro.pptv.version`、`ro.bd.cmcc_*`、`ro.bird.custom.sw.version`、`ro.build.tyd.custom.hw_version`、`ro.build.display.hardware(.id)`、`ro.mediatek.version.release`。
11. **反射是普遍手段且异常全部吞掉**：`f0/o.c()` 统一 `setAccessible(true)` 后 `invoke`，`catch` 里只 `printStackTrace()` 并返回 `null`；`e0/d2` 同样。因此失败**静默降级为"该字段为空字符串"**（`d0/g.d()` 把 null 补成 `""`/`"Unknown"`），上报端无法区分"没有"与"读失败"。
12. **`e0/b1` 读 `/proc/meminfo`、`e0/a0/a2/c0/d0/e1/g1/i1` 等读 `StatFs`/属性**——全部无需运行时权限，属于"零权限系统画像"。

## 4. 本批 42 类的隐私汇总

| 数据类型 | 涉及类（代表） | 权限需求 |
|---|---|---|
| IMEI（双卡） | 除 13 个桥/工具类外**全部** | `READ_PHONE_STATE`；属性/AIDL/Binder 路径**无需权限** |
| IMSI | a, a0, a1, a2, b0, b1, b2, c, c0, c1, d0, d1, e0, e1, f0, g0, g1, h0, h1, i1, j0, j1, k1, r 系 | `READ_PHONE_STATE`（`getSubscriberId`） |
| ICCID | 同上 | `READ_PHONE_STATE`；`SubscriptionInfo.getIccId()` 同 |
| 手机号 MSISDN | 同上（`getLine1Number`/`getMsisdn`/`SubscriptionInfo.getNumber`） | `READ_PHONE_NUMBERS`(API26+)/`READ_PHONE_STATE` |
| 小区标识 CellID/LAC/TAC/NCI | a, a1, a2, b2, c, c0, c1, d0, d1, e0, e1, f0, g0, h0, h1, i1, j0, j1, k1, l1, r 系 | `ACCESS_FINE_LOCATION`（`getCellLocation`）；`getNetworkRegistrationInfoList` 在 API 28+ 亦受限 |
| 运营商/网络类型/5G 模式/VoLTE | 几乎全部 | 无（`getNetworkOperator` 需 `READ_PHONE_STATE`；`Settings.Global` 读无需） |
| 应用清单 | c2$a（AppInfo） | `QUERY_ALL_PACKAGES`(API30+)/无（旧版） |
| 公网 IP | c2$b（IPv4/IPv6） | 无（走网络） |
| 硬件/存储/内存画像 | a0, b1, c1, f1, g0, j0, k1 | 无 |

## 5. 未解决 / 未知项

1. `d0/g.q` 字段**无任何写入点**（全工程 grep 仅构造器置 null），且 `d()/e()` 都不序列化它 —— 判定为**死字段**；但它在 `q()` 方法名/字段名上是否曾被用作 `NCLS` 之外的语义，无法从字节码判断。
2. `e0/c2$a` 里 `f0/f.b` 的赋值点不在本批；`f0/f.b==true` 时走 `d0/h`（IPv4/IPv6 + 运营商），否则 `d0/k`。`f0/f.b` 到底是"支持双栈"还是"允许上报 IP"，**未知**（需 f0 侧确认）。
3. `e0/g0` 把 `DeviceType` 写成机型名 `"HNR550T"`（`g0.smali:2024`），与其它类的 `0000xx` 编码冲突；下游如何解释**未知**。
4. `e0/k1.x()` 内两处写 `m/n`（CellID1/LAC1，`:1062-1066`）与两处写 `x/y`（CellID2/LAC2，`:1455-1459`），且多次 `d0/g.f()/g()` 也会写同一批键；最终值取决于分支顺序，**未做动态验证**。
5. `e0/h0.u(int)` 的两条分支都返回 `getImei(getDefault4GSlot())`（`h0.smali:852-880`），因此 `IMEI1` 与 `IMEI2` 极可能相同；是否为**逻辑缺陷**还是华为平台"双卡同 IMEI"的预期，**未知**（未在真机验证）。
6. `e0/c0`/`e0/e0` 依赖的 `c0/a`（iGreeTelephony）AIDL 接口的**真实方法语义**仅由调用点推断（`e(slot)=ICCID`、`b(slot)=MSISDN`、`d(slot)=LAC`、`g(slot)=CellID` 等），无 stub 文件佐证（`c0/a` 不在本批）。
7. `e0/b2` 的 `com.android.vts.VendorTelephonyManager` / `com.dmyk.android.telephony.DmUnicomAbsTelephonyManager` 为定制 ROM 私有类，`getVoLTEState()` 的返回编码（`"1"/"0"/"-1"`）只能按字符串猜测其语义。
8. 各机型 `DeviceType`（`000004` vs `000005` vs `007001029`）的**业务定义**没有任何字符串/注释说明；推测与"是否 5G/是否新制式"相关，但**无证据**。
9. `e0/a` 的 `CM CM5D` 分支会把 `Model` 改写为 `MODEL_ROM_RAM`（`a.smali:2092-2122`），其中 `f0/i.e()`/`f0/i.d()` 的确切返回（ROM/RAM 字符串格式）属 f0 侧，**未验证**。
10. 各 Reader 的 `b()` 内部机型分支与"读取路径选择"（如 a2 的 v/w/x/y、b1 的 x/y/w + 基类 p）之间的**判定阈值**（API 版本/机型批次）未逐分支还原；本文只给出了"存在哪些路径 + 各自覆盖的键"。
11. 本批所有 `printStackTrace()`（`e0/d2`、`f0/o`、各 `try/catch`）之外，是否还有 `f0/j.c(Context,String)` 触发的**网络上报**，属 f0 侧，未验证。
