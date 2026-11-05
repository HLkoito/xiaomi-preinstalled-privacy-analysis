> **历史分析材料，含已纠正结论，不能作为最终结论引用。** 请先阅读 [最终复核报告](../REPORT.md)。原材料中的“MD5 派生密钥等于不加密”“全量已安装应用清单”“隐私告知永远不可达”“设为 1 秒即每秒上传”“无时间戳/已证实重放”等说法已被收窄或纠正；本次公开版未重新验证全部 OEM 分支。已删除本地身份路径，未包含真实用户采集值。

# SystemHelper 遥测采集核心层（d0 / e0/r / e0/c2 / AIDL）逆向分析

- 目标 APK：`[LOCAL_PATH_REMOVED]`
- 包名：`com.mobiletools.systemhelper` v1.2-20230925-U（versionCode 3，minSdk 24，targetSdk 28）
- 分析基线：`analysis\SystemHelper\apktool\smali`（apktool 反编译，只读）+ `classes.dex` 原始字节校验
- 本文范围：`d0/*`（15 文件）、`e0/r`（基类）、`e0/c2`（编排器）、`b0/a`+`c0/a`（AIDL）
- 交付附带：`findings\c2_map_a_full.tsv`（`e0/c2.i()` 的 304 条 `Build.MODEL → 品牌` 全量映射，含 smali 行号）

> 说明：本文所有 `file:line` 均指 `apktool\smali\<file>`（如 `d0/g.smali:341`）。行号为 apktool 输出行号，不是反编译前源码行号。

---

## 1. d0 层总览（角色分工）

`d0` 包是"采集结果容器 + 事件源 + 上传线程"三件套，本身**不含** 85 个采集器的业务逻辑（那在 `e0/*`，各厂商 reader 子类）。

| 文件 | 类型/父类 | 角色 | 关键入口 |
|---|---|---|---|
| `d0/a` | synthetic final | API30 脱糖桥：`TelephonyDisplayInfo.toString()` | `a(TelephonyDisplayInfo)String` |
| `d0/b` | synthetic final | 脱糖桥：`TelephonyManager.getServiceState()` | `a(TelephonyManager)ServiceState` |
| `d0/c` | synthetic final | 脱糖桥：`TelephonyDisplayInfo.getNetworkType()` | `a(...)I` |
| `d0/d` | synthetic final | 脱糖桥：`TelephonyDisplayInfo.getOverrideNetworkType()` | `a(...)I` |
| `d0/e` | `android.telephony.PhoneStateListener` | API<31 的制式监听器（每卡槽一个实例） | `onDisplayInfoChanged` |
| `d0/f` | `android.telephony.TelephonyCallback` | API≥31 的制式/信号监听器（实现 DisplayInfo/CellInfo/SignalStrengths 三个子接口） | `onDisplayInfoChanged` / `onSignalStrengthsChanged` |
| `d0/f$a` | `LocationListener` | `fused` 提供者的定位回调（在 `d0/f.onSignalStrengthsChanged` 内匿名创建） | `onLocationChanged` |
| `d0/g` | 普通类（54KB） | **采集结果容器**（"DeviceInfo"），43 个字段（`a`–`z` + `A`–`Q`）+ 2 个 JSON 序列化器 + 2 个 CellLocation 解析器 | `d()` / `e()` |
| `d0/h` | `Thread` | **IP 地址线程**（IPv4=服务端回显公网 IP，IPv6=本机网卡） | `run()` |
| `d0/h$a` | interface | `d0/h` 完成回调 | `a(Context,d0/g,String,String)` |
| `d0/i` | `Thread` | **应用使用统计 + 流量线程**（UsageStats / NetworkStatsManager） | `run()` |
| `d0/i$a` | interface | `d0/i` 完成回调 | `a(Context,d0/g,List)` |
| `d0/i$b` | `Comparator<UsageStats>` | 按前台时长**降序**排序 | `a(UsageStats,UsageStats)I` |
| `d0/j` | `Thread` | 历史信号记录装载器（**工作在构造函数里，`run()` 并未重写**） | `<init>` |
| `d0/k` | `Thread` | **上报线程**（唯一调用 `f0/g.c(String,[B,[B)` 的地方） | `run()` |

装配关系（谁 new 谁）：

- `d0/i` 只由 `e0/c2.f()` 与 `e0/c2.g()` 启动（`e0/c2.smali:167`、`:290`）。
- `d0/j` 只在 `e0/c2.g()` 的 `"R"` 分支启动（`e0/c2.smali:314`）。
- `d0/h` **只由 `e0/c2$a`（`d0/i` 的回调）创建**（`e0/c2$a.smali:145`），全 APK 唯一一处 `new-instance ... Ld0/h;`。
- `d0/k` 由四处启动：`e0/c2.g()` 兜底分支（`:343`）、`e0/c2$a`（`:156`）、`e0/c2$b`（`:69`，IP 回调后）。
- `d0/f` / `d0/e` 由 `e0/r.k(Context)` 注册（`e0/r.smali:245`、`:268`、`:322`、`:345`）。

---

## 2. `d0/g` 数据结构（字段 → 含义 → 写入者 → JSON 键）

字段声明：`d0/g.smali:7..91`；构造函数全量置 null（`B` 例外，初值 `"0"`）：`d0/g.smali:95..195`。

| 字段 | 类型 | JSON 键（`d()`,行号） | 语义 | 写入者（证据） |
|---|---|---|---|---|
| `a` | String | `Manuf` (354-358) | 厂商 | `e0/r.b()` ← `Build.MANUFACTURER`（`e0/r.smali:498`）；上报 URL 也用它 |
| `b` | String | `Model` (369-373) | 机型 | `e0/r.b()` ← `Build.MODEL`（`e0/r.smali:494`） |
| `c` | String | `HWVersion` (384-388) | 硬件版本 | `e0/r.b()` ← SystemProperties `ro.hardware.version`（`e0/r.smali:514-522`） |
| `d` | String | `SWVersion` (399-403) | 软件版本 | `Build.DISPLAY`（`e0/r.smali:510-512`） |
| `e` | String | `OS` (414-418) | 固定 `"Android"` | `e0/r.smali:502-504` |
| `f` | String | `OSVersion` (429-433) | `Build.VERSION.RELEASE` | `e0/r.smali:506-508` |
| `g` | String | `IMEI1` (444-448) | IMEI(slot0) | `e0/r.l/m/o/p/q`；上报前用 `d0/k.a()` 校验 15 位纯数字 |
| `h` | String | `ICCID1` (459-463) | SIM 序列号/ICCID | `e0/r.l/o/n/p/q`；`e0/c2.o()` 可能 MD5 化 |
| `i` | String | `IMSI1` (474-478) | 订阅者 ID | 同上；`d0/i` 用它作为移动流量 `subscriberId` 查询条件 |
| `j` | String | `MSISDN1` (489-493) | 手机号 | 同上（`getLine1Number` / `SubscriptionInfo.getNumber()`） |
| `k` | String | `MNC1` (504-508) | **实为完整网络运营商号**（如 `46001`），不是 2~3 位 MNC | `getNetworkOperator()` / `getNetworkOperatorForSubscription` |
| `l` | String | `NCLS1` (519-523) | 网络制式字符串（NR/LTE/NSA/SA/WCDMA/GSM/CDMA/TD-SCDMA） | `e0/r.g(int,String)` 映射；另由 `f0/f.n`（`d0/e`/`d0/f` 写入）覆盖（`e0/r.smali:1366-1368`） |
| `m` | String | `CellID1` (534-538) | 小区 ID | `d0/g.f(CellLocation)`；`e0/r.r()`（CellIdentity）；`f0/l.h("CellID0")` |
| `n` | String | `LAC1` (549-553) | LAC/TAC | 同上（`"LAC0"`） |
| `o` | String | `VolteEnabled1` (564-568) | VoLTE 开关 `"1"/"0"/"-1"` | `e0/r.c()/d()`（IMS 反射） |
| `p` | String | `5GEnabled1` (579-583) | 5G 能力 `"1"/"0"` | `e0/r.j(ctx,slot)`（`preferred_network_mode` ∈ 23..33） |
| `q` | String | **无**（无 JSON 键） | 仅类内使用/预留给 slot1 配对 | 仅构造置 null |
| `r` | String | `IMEI2` (594-598) | IMEI(slot1) | 同 `g` |
| `s` | String | `ICCID2` (609-613) | SIM2 | 同 `h` |
| `t` | String | `IMSI2` (624-628) | IMSI2 | 同 `i`；`d0/i` 用它做第二路流量查询 |
| `u` | String | `MSISDN2` (639-643) | 号码2 | 同 `j` |
| `v` | String | `MNC2` (654-658) | 运营商号2 | 同 `k` |
| `w` | String | `NCLS2` (669-673) | 制式2 | `e0/r.l()` ← `f0/f.o` |
| `x` | String | `CellID2` (684-688) | 小区2 | `d0/g.g(CellLocation)`；`e0/r.r()` |
| `y` | String | `LAC2` (699-703) | LAC2 | 同上 |
| `z` | String | `VolteEnabled2` (714-718) | VoLTE2 | `e0/r.c()/d()` slot1 |
| `A` | String | `5GEnabled2` (729-733) | 5G2 | `e0/r.j(ctx,1)` |
| `B` | String | `DataSlot` (776-780) | 默认数据卡 `"1"/"2"`，其他值被强制归 `"0"` | `e0/r.n()/o()`：`SubscriptionManager.getDefaultDataSubscriptionId()+1`（`e0/r.smali:1735-1747`、`:1845-1857`） |
| `C` | String | `AccType` (791-795) | `"WiFi"/"Mobile"/"Disconnected"` | `e0/c2.n()`（`e0/c2.smali:3820-3855`） |
| `D` | String | `IPv4` (810-814) **仅当 `f0/f.b`** | 公网 IPv4 | `e0/c2$b` ← `d0/h` |
| `E` | String | `IPv6` (825-829) **仅当 `f0/f.b`** | 本机 IPv6 | `e0/c2$b` ← `d0/h` |
| `F` | JSONObject | `SubType` (836-847) | 厂商自定义子信息 | 采集器（厂商子类）写入，本包不写 |
| `G` | String | `DeviceType` (861-872) | 默认 `"000"` | 本包只兜底；实际由壳层/采集器写 |
| `H` | String | `RegType` (884-888) | 注册类型（`A/S/B/T/R`…） | `e0/c2.g(ctx,String)` 入参（`e0/c2.smali:219`） |
| `I` | String | `SpecVersion` (890-896) | **恒定 `"3.0"`**，每次序列化都覆写 | `d0/g.d()` 自身 |
| `J` | String | `RegVersion` (911-913) | 默认 `"1.0"`，实际=App `versionName` | `e0/c2.n()`（`e0/c2.smali:3739-3741`） |
| `K` | String | `Time` (925-929) | `SimpleDateFormat` 格式化当前时间 | `e0/c2.n()` ← `f0/l.d()`（`e0/c2.smali:3859-3863`） |
| `L` | JSONArray | `AppInfo` (935-946) | 应用使用/流量数组，元素来自 `f0/b.a()` | `e0/c2$a`（`e0/c2$a.smali:96`） |
| `M` | JSONArray | `History` (972-979，**key 存在才写**) | 待上报历史（SQLite `systemhelper.history`） | `d0/k.b()`（`d0/k.smali:124-129`；失败时置 null `:336`） |
| `N` | String | `SignalInfo` (953-968) **仅当 `f0/f.e`** | 历史信号 JSON 字符串 | `d0/j.<init>`（`d0/j.smali:54-62`） |
| `O` | String（private） | —（序列化缓存） | `d()/e()` 末尾缓存整串 JSON | `d0/g.smali:985`、`:1644` |
| `P`/`Q` | final int | — | 常量 0 / 1，**仅构造赋值，无任何读取点**（全库 `iget Ld0/g;->P:` 为空） | — |

### 2.1 两个序列化器的差异

- `d()LString;`（`d0/g.smali:335`，`synchronized`）：完整版 —— 含 `IPv4/IPv6`（受 `f0/f.b` 控制）、`SignalInfo`（受 `f0/f.e`）、`History`（`M != null` 才写）。**上报用这个**。
- `e()Ljava/lang/String;`（`d0/g.smali:1038`，`synchronized`）：精简版 —— **没有 IPv4/IPv6，也没有 History**。仅被 `d0/g.c(Context)`（`d0/g.smali:301`）调用，而 `d0/g.c` 只被 `com.mobiletools.systemhelper.Activity.TestActivity:59` 调用（调试用）。
- 两者都做"写时兜底"：把 null 字段回写成 `"Unknown"` / `""` / `"0"`（`d()`:345-780 大量 `if-nez ... iput-object`），并把 `DataSlot` 归一到 `{"0","1","2"}`、`SpecVersion` 强制 `"3.0"`；`DeviceType` 空则置 `"000"`。
- `JSONException` 时打 `"DeviceInfo"/"JSONException->"` 并返回 `O`（上次缓存串，首次为 null）（`d0/g.smali:997-1030`）。

### 2.2 单例与 Context 绑定

`d0/g` **自身没有单例**，也没有静态 Context；三个静态工厂全部把 `Context` 塞进 `f0/f.f` 后转调 `e0/c2`：

```smali
# d0/g.smali:197  a(Context)
sput-object p0, Lf0/f;->f:Landroid/content/Context;   # 全局 Context 落到 f0/f.f
invoke-static {p0}, Le0/c2;->f(Landroid/content/Context;)Ld0/g;
...
catch Exception → log "DeviceInfo" → return null
```
- `a(Context)g`（`:197`）→ `e0/c2.f()`（完整采集 + 启动 `d0/i`）
- `b(Context,String)g`（`:249`）→ `e0/c2.g()`（带 RegType 的分支采集 + 启动对应线程）
- `c(Context)String`（`:301`）→ `e0/c2.f().e()`（调试用精简 JSON）

即：**每次调用都新建一个 `d0/g`**（`e0/c2.f`/`g` 内 `new-instance Ld0/g`），"单例"只体现在 `e0/c2.b`（vendor reader 缓存）与 `f0/f` 的静态槽位上。

### 2.3 CellLocation 处理（`f(CellLocation)` / `g(CellLocation)`）

`f()`=slot1 → 写 `m/x`(CellID)、`n/y`(LAC)；`g()`=slot2 → 写 `x/y`。两方法结构完全对称（`d0/g.smali:1697` / `:1943`）：

1. 入参 null → 打 `"sim1 location is null"` / `"sim2 location is null"` 直接返回。
2. `GsmCellLocation`：`getCid()`/`getLac()`，值为 `-1` 视为无效（只打日志，不覆盖字段）。
3. `CdmaCellLocation`：`getBaseStationId()` → CellID；`getBaseStationLongitude()+getBaseStationLatitude()` → `String.format("%d:%d", lon, lat)` → 写入 LAC 位（`0x7fffffff` 视为无效并跳过）。
4. 其他类型 → `"unknow CellLocation type:"`。
5. 全程 `declared-synchronized`（`monitor-enter p0`），即对 `d0/g` 实例加锁。

---

## 3. 事件源与数据流时序

### 3.1 注册路径（谁把监听器挂上去）

`e0/r.k(Context)`（`e0/r.smali:187`，`static synchronized`），日志 `"DeviceInfoReaderBase"/"listenNetworkType---->"`：

| 卡槽 | 条件 | 动作 |
|---|---|---|
| slot0 | `h(0) > 0 && f0/f.h == null` | `tm.createForSubscriptionId(subId0)` → `f0/f.h` |
| slot0 | `SDK_INT >= 31 (0x1f)` | `new d0/f(ctx,0)` → `f0/f.l`；`e0/k.a(tm, e0/j.a(ctx), callback)` 注册 |
| slot0 | `SDK_INT < 31` | `new d0/e(ctx,0)` → `f0/f.j`；`tm.listen(listener, 0x100500)` |
| slot1 | `h(1) > 0 && f0/f.i == null` | `createForSubscriptionId(subId1)` → `f0/f.i`；同样按 SDK 分 `f0/f.m`(d0/f) / `f0/f.k`(d0/e) |

`0x100500 = LISTEN_SIGNAL_STRENGTHS(0x100) | LISTEN_CELL_INFO(0x400) | LISTEN_DISPLAY_INFO_CHANGED(0x100000)`。
`e0/r.s(Context)`（`:373`）反向注销并把 6 个静态槽位全部置 null。

调用点（同一套"先 k() 后 s()"模式）：
`DMApplication:34`（进程启动）、`OnBootReceiver:80`、`AppInfoRegisterReceiver:60/104`、`ScheduleRegisterReceiver:60/104`、`SIMStateReceiver:189/202`、`RegisterHelperService:78`、`TestActivity:119`。

### 3.2 制式（5G/NR）状态机：`d0/e.onDisplayInfoChanged` ≡ `d0/f.onDisplayInfoChanged`

两者逻辑逐行等价（`d0/e.smali:55..452` ↔ `d0/f.smali:156..515`），差异仅日志 tag（`"DMPhoneStateListener"` vs `"DMTelephonyCallback"`）与 `synchronized` 修饰：

1. `info == null` → `"displayInfo==null"`，返回。
2. `j/a.a(ctx,"android.permission.READ_PHONE_STATE") != 0` → `"... no READ_PHONE_STATE PERMISSION_GRANTED"`，返回。
3. 日志 `"slot:<b>, displayInfo:<TelephonyDisplayInfo.toString()>"`（经 `d0/a` 脱糖桥）。
4. `subId = e0/r.h(slot)`；`tm.createForSubscriptionId(subId).getServiceState()` —— **只为触发一次绑定/打日志**，结果仅在为 null 时打 `"serviceState==null"`，不参与后续判定（`d0/e.smali:131-166`）。
5. `netType = d0/c.a(info)`（`getNetworkType()`）；`override = d0/d.a(info)`（`getOverrideNetworkType()`）。
6. 按 `slot` 选目标静态字段：slot0 → `f0/f.n`，slot1 → `f0/f.o`；其他 slot → 打 `"unknown mSlotId:"` 后跳到收尾日志。
7. 制式映射（两槽同表，packed-switch 见 `d0/e.smali:405-451` / `d0/f.smali:468-514`）：

| netType | 常量 | 结果字符串 |
|---|---|---|
| 1,2 | GPRS/EDGE | `GSM` |
| 3,8,9,10,15 | UMTS/HSDPA/HSUPA/HSPA/LTE? → 见下 | `WCDMA` |
| 4,5,6,7,12,14 | CDMA/EVDO 系 | `CDMA` |
| 13 | LTE | **`LTE` 或 `NSA`**：`override==3(NR_NSA)→"NSA"`，`override==4(NR_NSA_MMWAVE)→"NSA"`，否则 `LTE` |
| 16 | GSM | `GSM`（重复命中） |
| 17 | TD-SCDMA | `TD-SCDMA` |
| 19 | LTE_CA | 同 13 规则（`LTE`/`NSA`） |
| 20 | NR | `SA` |
| 其他（11,18,…） | — | 打 `"unknown networktype <n>"`，写 `UNKNOWN` |

> 注：`netType` 3/8/9/10/15 在本表里都落 `WCDMA`；`pswitch_5`/`pswitch_c` 分支还是 **46000/46002/46007 → `TD-SCDMA`，否则 `WCDMA`** 的运营商特判（`e0/r.g()` 里同样存在，见 §5）。`d0/e` 的 `pswitch_5` 未带运营商特判（只 `WCDMA`），`e0/r.g()` 才带 —— 这是两处实现细节差异。

8. 收尾统一打日志 `"NetworkTypeSlot1:<f0/f.n>, NetworkTypeSlot2:<f0/f.o>"`。
9. `d0/e` **未重写** `onSignalStrengthsChanged`；`onCellInfoChanged` 只 `invoke-super` 后 return（`d0/e.smali:33`）→ 信号强度在 API<31 路径上其实采集不到。

### 3.3 信号强度 + 定位：`d0/f.onSignalStrengthsChanged`（`d0/f.smali:517`）

1. 总开关 `f0/f.e == false` → 直接返回（该字段还是 `SignalInfo` 是否入 JSON 的开关）。
   **重要**：全 APK 没有任何 `sput-boolean ... Lf0/f;->e:`（只有 `a/g/u/v` 有写入点），即 `f0/f.e` 恒为初值 `false` → 本方法在真实运行中**第一步就返回**，下面 3.3 全流程为"设计意图"而非当前生效路径（详见 §6.8）。
2. `e0/r.h(slot)` → `tm.createForSubscriptionId()`（结果丢弃，仅副作用）。
3. `raw = SignalStrength.toString()` → 去掉全部空白 → 存 `f0/f.c`（静态，跨实例共享）→ 按 `","` 切分。
4. 在切片里找 **前 3 个字符 == `"mNr"`** 的那一段（即 NR 分量），拿到 `nr`。
5. 用字符串下标硬解析四个字段（`substring(indexOf(...)+7, ...)`）：`ssRsrp=`、`ssRsrq=`、`ssSinr=`、`level=` —— 这是一个**以 `SignalStrength.toString()` 文本格式为前提的脆弱解析器**（`d0/f.smali:599-683`）。
6. 解析结果填入 `f0/n`（`d(rsrp)`/`e(rsrq)`/`f(sinr)`），再 `Integer.parseInt` 成三个 int，打日志 `"rsrp<I>rsrq<I>sinr<I>"`。
7. 定位触发判定（`d0/f.smali:751-760`）：`rsrp > 0x7fffffff` 与 `rsrq > 0x7fffffff` 与 `sinr > 0x0ccd569f` 三者同时成立才跳过；否则打 `"enter====>LOCATION "` 并走定位分支 —— 由于 `SignalStrength` 用 `0x7fffffff` 表示未知，**实际上几乎每次都会走定位**。
8. 定位：权限 `ACCESS_FINE_LOCATION` 或 `ACCESS_COARSE_LOCATION` 任一即可；`LocationManager.getLastKnownLocation("fused")` → 格式化 → 若 `lastKnown == null` 或 `String.valueOf(location) != 格式化串`，则 `requestLocationUpdates("fused", 1000ms, 10.0f, new d0/f$a(this, lm))`（`d0/f.smali:798-845`）。
9. 组 JSON（`d0/f.smali:853-900`）：`{RSRP, SINR, RSRQ, cellid←g.x, LAC←g.y, time←g.K, location←格式化定位串}`，仅打日志 `"Signal json info:<json>"`，**不落库、不入 g**。
10. 末尾 `f0/m.a(ctx, f0/m.c, f0/m.b.intValue())`（`d0/f.smali:946-956`）——**只取实例后丢弃返回值**，全库没有任何 `f0/m` 的 insert 调用（见 §9 未知项）。

`d0/f$a`（`d0/f$a.smali`）三回调：`onLocationChanged`/`onProviderDisabled` 直接 `d0/f.a(Location)` 格式化后**丢弃返回值**；`onProviderEnabled` 先校验定位权限，再 `getLastKnownLocation(provider)` 后同样丢弃。→ 定位数据实际上**没有进入任何上报字段**（`g.D/E` 是 IP，不是经纬度）。

### 3.4 线程时序（三条采集线程 + 上报线程）

```
[RegType 分发]  e0/c2.g(ctx, regType)                     e0/c2.smali:189
   ├─ "A" && f0/f.g(device_provisioned)  → new d0/i(ctx,g,c2$a).start()      :290
   ├─ "R"                                → new d0/j(ctx,g).start()           :314
   └─ 其它 / "A" 但未 provisioned        → new d0/k(ctx,g).start()           :343

d0/i.run()  d0/i.smali:620
   b = now(); c = now() - f0/f.z(7天)          # 仅当 b==0&&c==0
   d() → UsageStats 聚合 + 排序 + 逐包组装 f0/b
   i$a.a(ctx, g, list)  →  e0/c2$a:
        f0/f.g = e(ctx)                        # 再算一次 device_provisioned
        g.L = JSONArray(f0/b.a() ...)          # AppInfo
        if (f0/f.b) new d0/h(ctx,g,c2$b).start()   # 需要 IP
        else        new d0/k(ctx,g).start()

d0/h.run()  d0/h.smali:173
   e = IPv4 = f0/l.l(ctx) ? new f0/e().a() : ""      # HTTP GET 公网 IP
   f = IPv6 = 首个非 loopback/非 linklocal 的 Inet6Address
   h$a.a(ctx,g,e,f)  →  e0/c2$b: g.D=e; g.E=f; new d0/k(ctx,g).start()

d0/k.run()  d0/k.smali:593
   if (!f0/f.g) { queue(g.d()); log "User Permission Closed!" ; return }
   ok = d(g)                                  # 校验→(网络/运营商)闸门→加密→POST
   c(ok)                                      # 空方法（d0/k.smali:407）
```

`d0/j` 的"线程"是假象：`d0/j.smali` 只有构造函数，没有 `run()`（继承 `Thread.run()` 空实现），装载动作发生在 `start()` 之前的**调用者线程**上。

---

## 4. 上报管道完整还原（`d0/k`）

### 4.1 闸门（`d0/k.d(d0/g)`，`d0/k.smali:413`，`synchronized`）

```
a(g.g) && a(g.r)                 # IMEI1、IMEI2 都必须"合法"（见下）否则直接 return false
  └ f0/l.l(ctx) == false         # 无任何网络
        → queue(g.d()); log "No network connected!"; return false
  └ f0/l.o(ctx) == true          # 移动数据已连接
        → return b(g)            # 直接上报
  └ c2.k(ctx)==false && f0/l.m(g)==true
        → return b(g)            # 联通卡 + DataSlot 匹配
  └ else
        → queue(g.d()); log "isUnicomNetwork=false, not post!"; return false
```
- `d0/k.a(String)`（`:25`）：`null`/空 → **返回 true**（放行）；长度 != 15 → `"imei: <x> length is not 15!"` false；非 `[0-9]+` → `"imei: <x> is not all number!"` false。
- `c2.k(ctx)`=`e0/c2.j()`=`reader.a(ctx)`；基类 `e0/r.a()` 返回 `new ServiceState().getRoaming()`（**恒 false**）（`e0/r.smali:477-489`）。
- `f0/l.m(g)`（`f0/l.smali:401`）：`DataSlot=="1"` 且 `g.k ∈ {46001,46006,46009}`，或 `DataSlot=="2"` 且 `g.v ∈ 同集合` → true；否则打 `"isUnicomNetwork->DataSlot："` 并 false。
- "queue" = `f0/d.b(ctx, f0/d.c, f0/d.b).c(g.d())`（插入 `systemhelper.history`，表 `history(id, jsonString)`，`f0/d.smali:496`；**上限 100 行**，超限先删最旧一行：`f0/d.smali:174-206`）。

### 4.2 加密链路（`d0/k.b(d0/g)`，`d0/k.smali:98`）

```
postString = g.d()                                    # 明文 JSON（§2）
key        = f0/c.d(postString)                       # MD5(UTF-8(json)) → 16 字节
encKey     = f0/c.c(ctx, key)                         # RSA/ECB/PKCS1Padding(pub.cer, key)
signB64    = f0/c.b(encKey)                           # Base64.encodeToString(无换行)
signParam  = URLEncoder.encode(signB64, "utf-8")
modelParam = URLEncoder.encode(g.b, "utf-8")
manufParam = URLEncoder.encode(g.a, "utf-8")
url = f0/l.g(ctx)
    + "?ver="  + "3.0"
    + "&model="+ modelParam
    + "&manuf="+ manufParam
    + "&sign=" + signParam
body = f0/c.a(postString, key)                        # AES/ECB/PKCS5Padding(key, json)
ok   = new f0/g().c(url, body, key)                   # 第三参数在 f0/g 内被立即覆盖，实际未使用
```

证据与实现细节：
- `f0/c.d`：`MessageDigest("MD5").digest(str.getBytes("UTF-8"))`（`f0/c.smali:142-165`）。
- `f0/c.c`：`new f0/k()` → `AssetManager.open("pub.cer")` → `f0/k.b(InputStream)` 用 `CertificateFactory("X.509")` 取 `X509Certificate.getPublicKey()` → `f0/k.a(Key,[B)` 做 `RSA/ECB/PKCS1Padding` 加密（`f0/c.smali:110-140`、`f0/k.smali:55-118`）。
- `f0/c.b`：`Base64.encodeToString(b, 0, len, 0)`（`f0/c.smali:96-108`）。
- `f0/c.a`：`SecretKeySpec(key,"AES")` + `Cipher("AES/ECB/PKCS5Padding")` ENCRYPT_MODE（`f0/c.smali:7-47`）。
- **外层无二次加密**：签名只保护"密钥"，正文用 ECB 单块模式加密，无 IV、无完整性校验。

### 4.3 HTTP 细节（`f0/g.c`，`f0/g.smali:52`）

| 项 | 值 | 证据 |
|---|---|---|
| 方法 | `POST` | `:100` |
| 连接/读超时 | `10000` / `15000` ms | `:86-94` |
| `Content-Type` | `application/encrypted-json` | `:114-118` |
| `Content-Length` | `body.length` 十进制 | `:122-130` |
| 协议 | **明文 `http://`**（默认端点是 `http://dm.wo.com.cn:18080/registerv3`） | `f0/f.smali:49` |
| 成功判定 | `responseCode == 200` | `:174-184` |
| 其它 | 失败打 `"register fail!"`；`disconnect()` 于 finally | `:187-196` |

### 4.4 重试 / 失败处理（`d0/k.smali:255-358`）

- **无重试、无退避、无次数上限**：单次 `f0/g.c`，`Exception` → `"sendPOSTRequest exception--->"` 后按失败处理。
- 特殊放行：`f0/g.c` 返回 false 后读 `f0/g.b()`（响应码），若 `== 0x1a0`（**HTTP 416**）则把结果当成功（`d0/k.smali:264-278`）。含义是"服务端认为该记录已存在"。
- 成功 → `f0/d.a()`：`DELETE FROM history`（清队列，`f0/d.smali:110-138`）。
- 失败 → `g.M = null`，并把本次 `g.d()` 插入队列（`d0/k.smali:333-358`）。
- 开头把历史合并进正文：`g.M = SDK>=28 ? f0/d.e() : f0/d.d()`（`d0/k.smali:118-141`，前者用 20MB `CursorWindow` 以避开单窗口限制，`f0/d.smali:407-417`）。

### 4.5 端点与间隔常量（`f0/f.smali`）

| 常量 | 值 | 含义 |
|---|---|---|
| `f0/f.w` | `http://dm.wo.com.cn:18080/registerv3` | 注册/上报 URL 默认值 |
| `f0/f.x` | `http://dm.wo.com.cn:18080/public/ip` | 公网 IP 回显（`f0/e.a()`，GET，3s 超时） |
| `f0/f.y` | `0x5265c00` = 86,400,000 ms | `scheduleInterval` 默认 24h |
| `f0/f.z` | `0x240c8400` = 604,800,000 ms | `appInfoInterval` / UsageStats 回看窗口默认 7 天 |
| `serverAddress` | prefs 键，默认 = `f0/f.w` | 运行时可被覆盖（`f0/l.t(Context,String)`，`f0/l.smali:740`）→ **唯一的"远程配置"落点**，但本包内无写调用点（见 §9） |

Prefs 文件：`com_mobiletools_systemhelper_preference`（`f0/l.smali:160`）；`f0/l.a()` 会**直接删除该 XML**（`/data/data/com.mobiletools.systemhelper/shared_prefs/...`，`f0/l.smali:39`）。

---

## 5. `e0/r` 基类契约（模板方法 + 共用能力）

`e0/r` 实现 `e0/c2$c`（`a(Context)Z` + `b(Context,d0/g)V`），是 63 个厂商 reader 的父类。实例字段只有 `private TelephonyManager a`（`e0/r.smali:10`，仅构造置 null，全类未使用 —— 死字段）。

### 5.1 模板方法

| 方法 | 语义 | 证据 |
|---|---|---|
| `a(Context)Z` | **漫游判定**。基类实现：`new ServiceState().getRoaming()` → 恒 `false`（vendor 子类覆盖为真实值） | `:477-489` |
| `b(Context,d0/g)V` | 模板：先填 6 个静态机型字段，再按 SDK 分派 `m()/n()/o()/l()` | `:491-558` |
| `c(Context,int)String` | 读 VoLTE：`ImsManager.createForSubscriptionId(subId).getImsMmTelManager()` → `ImsMmTelManager.isAdvancedCallingSettingEnabled()`（反射 `Le0/h`）。**副作用**：若为 false，先反射调用 `setAdvancedCallingSettingEnabled(true)` 再复读。仅当 `f0/f.c == true` 时被 `l()` 使用 | `:560-688` |
| `d(Context,int)String` | 只读版 VoLTE（不写系统设置） | `:690-743` |
| `f(TelephonyManager,int)I` | 反射 `getNetworkType(slot)`，为 0 则取 `getVoiceNetworkType(slot)` | `:745-812` |
| `g(int,String)String` | 制式号→字符串映射（见下表） | `:814-985` |
| `i(int)[I` | 反射 `SubscriptionManager.getSubId(int)`，把 `long[]` 强转成 `int[]`（异常时回退按 `int[]` 取） | `:987-1166` |
| `j(Context,int)Z` | `Settings.Global.getInt("preferred_network_mode"+slot, -1)` ∈ `[23,33]` → 5G 能力 | `:1168-1240` |
| `l(Context,d0/g)V` | **API30+ 主流实现**：反射 `getDeviceId(slot)`/`getSubscriberId(slot)`/`getSimSerialNumber(slot)`/`getLine1NumberForSubscriber(slot)`/`getNetworkOperatorForSubscription`/`getNetworkType(slot)`；小区来自 prefs `"CellID0"/"LAC0"`；VoLTE 走 `c()`（联通 46001/46006/46009）或 `d()`；每卡都调 `r()` 补小区 | `:1242-1553` |
| `m(Context,d0/g)V` | **SDK<26 实现**：`getDeviceId()`/`getSubscriberId()`/`getSimSerialNumber()`/`getSimOperator()`/`getNetworkType()` + `getCellLocation()`→`g.f()`；`MSISDN1=""` | `:1555-1622` |
| `n(Context,d0/g)V` | 26~29 实现：`SubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0/1)` → `j/u = carrierName+number`、`h/s = iccid`、`B = defaultDataSubId+1`。**在 `b()` 中不可达** | `:1624-1750` |
| `o(Context,d0/g)V` | **SDK≥26 实现**（日志 `"read26"`）：`e0/i.a(tm,0/1)` 取 IMEI1/2，`getActiveSubscriptionInfoForSimSlotIndex` 取 number/iccid，`B = defaultDataSubId+1` | `:1752-1860` |
| `p(Context,d0/g)V` | API29/30 变体：与 `l()` 同套反射，但网络制式走 `f(tm,slot)` | `:1862-2254` |
| `q(Context,d0/g)V` | API30+ 变体：`p()` + `f()` 组合（厂商子类选择调用） | `:2256-2612` |
| `r(Context,int,d0/g)V` | 用 `ServiceState.getNetworkRegistrationInfoList()` 里 **`domain==2(PS) && transport==1(WWAN)`** 的 `CellIdentity` 解析小区：GSM(`getCid/getLac`)、WCDMA、TD-SCDMA(反射)、CDMA(`%d:%d` lon:lat)、LTE(`getCi/getTac`)、NR(反射 `getNci/getTac`)；按 `int slot` 写 `m/n` 或 `x/y` | `:2614-2948` |

### 5.2 版本/厂商分支（`b()` 实际行为）

```smali
# e0/r.smali:491-558 (逐行还原)
b(ctx, g):
  g.b = Build.MODEL; g.a = Build.MANUFACTURER; g.e = "Android"
  g.f = Build.VERSION.RELEASE; g.d = Build.DISPLAY
  g.c = SystemProperties("ro.hardware.version", null)
  if (SDK_INT < 26)  m(ctx,g)          # :530
  else               o(ctx,g)          # :544   ← 26..∞ 都走这里
  # 下面两段是死代码（前一条分支已 goto 收尾）：
  #   if (SDK_INT < 26) n(ctx,g)       # :537 不可达
  #   if (SDK_INT >= 30) l(ctx,g)      # :553 不可达
```
→ 基类 `b()` 在运行时**只有两条路**：`SDK<26 → m()`，`SDK≥26 → o()`；`n()/l()` 由厂商子类显式调用（如 `e0/c1/e0/f1/e0/g0/e0/m0/e0/m1/e0/s1/e0/y/e0/z1/r0` 调 `l()`，`e0/b1/e0/e1/e0/i1/e0/k0/e0/l1/e0/n1/e0/q0/e0/r0/e0/s1/e0/t/e0/v/e0/x1` 调 `p()`/`q()`）。共 **62 个子类重写 `b()`**。

### 5.3 SDK 分支清单（全部版本判定点）

| 判定 | 位置 | 行为 |
|---|---|---|
| `SDK_INT <= 33(0x21)` | `h()` `:114` | 反射 `getSubId(int)`，失败返回 `-1` |
| `SDK_INT > 33` | `h()` `:153` | 反射 `getSubscriptionId(int)` |
| `SDK_INT >= 31(0x1f)` | `k()` `:233`、`s()` `:393/428` | `TelephonyCallback`（`d0/f`，经 `e0/k.a`/`e0/d.a`）否则 `PhoneStateListener`（`d0/e`） |
| `SDK_INT < 26` / `>= 26` | `b()` `:528/542` | `m()` / `o()` |
| `SDK_INT >= 28` | `d0/k:118`、`d0/j:48` | `f0/d.e()`（大 CursorWindow）/ `f0/m.b()` |
| `SDK_INT >= 31` | `e0/c2.h:367-373` | 先 `e0/r.k(ctx)` 注册监听 |

### 5.4 键名规则 / 前缀

结论：**`d0/g` 的 JSON 键是硬编码英文字面量，没有任何前缀拼接**（`Manuf/Model/HWVersion/…/History`，见 §2 表）。
任务里提到的两个"前缀"其实是**日志前缀**，不是键名前缀：

| 字符串 | 位置 | 性质 |
|---|---|---|
| `"deviceinfo.signalinfo:"` | `e0/c2.smali:326` | `e0/c2.g()` 中 `"R"` 分支的 **Log 前缀** + `g.N` 的值 |
| `"apilevel:"` | `e0/q1.smali:2101` | Samsung reader 内构造 Log 消息前缀（`"apilevel:"+n`），与 JSON 无关 |

日志 tag 清单（便于全量检索）：`DeviceInfoReaderBase`、`DeviceManager`、`DMPhoneStateListener`、`DMTelephonyCallback`、`DeviceInfo`、`RegisterThread`、`RegisterIntentService`、`RegisterHelperService`、`HttpEncryptHelper`、`RSAHelper`、`GetIPAddressThread`、`GetIPAddressHttpHelper`、`AppInfo`、`SignalDatabaseHelper`、`RegisterUtils`。

### 5.5 `e0/r` 与 `d0/g` 的读写关系（字段归属）

- **只写**：`a,b,c,d,e,f`（机型静态信息，`b()` 一次写全）
- **读+写**：`g,h,i,j,k,l,m,n,o,p`（slot1）与 `r,s,t,u,v,w,x,y,z,A`（slot2）—— 注意 `d0/g.f()/g(CellLocation)` 与 `e0/r.r()` 会写同一批小区字段（`m/n/x/y`），后者在 `l()/p()/q()` 里后排覆盖
- **只写（本层不读）**：`B`（`e0/r.n/o` 写，`f0/l.m()` 与 `d0/g.d()` 读）、`H`（`c2.g` 写入参）、`C`（`c2.n` 写）、`J`（`c2.n` 写 `versionName`）、`K`（`c2.n` 写 `f0/l.d()`，被 `d0/f` 读走做 `time` 键）、`L`（`c2$a` 写）、`M`（`d0/k` 写）、`N`（`d0/j` 写）
- **`e0/r` 不读不写**：`F`（`SubType`，厂商采集器专写）、`G`（`DeviceType`）、`I`（`SpecVersion`，`d()` 自赋）、`O`（内部缓存）、`P/Q`（常量）

---

## 6. `e0/c2` 编排器（"DeviceManager"）

`e0/c2` 不是 85 个采集器的调度器，而是**设备信息读取器（vendor reader）的选择器 + 注册流程编排器 + 上报触发器**。采集器（`e0/z1` 等）由壳层另行调用，不经过 `c2`。

### 6.1 静态状态

| 字段 | 类型 | 作用 |
|---|---|---|
| `a` | `Map<String,String>` | `Build.MODEL → 品牌` 覆盖表（**304 条**，懒加载于 `i()`） |
| `b` | `e0/c2$c` | 已选定的 reader 实例缓存（**单例**，`h()` 命中后直接返回） |
| `c` | `List<f0/b>` | 最近一次 AppInfo 列表（`c2$a` 写入，供二次使用） |
| `d` | `d0/i$a` | `new e0/c2$a()`（UsageStats 完成回调） |
| `e` | `d0/h$a` | `new e0/c2$b()`（IP 完成回调） |

### 6.2 `i()`：`Build.MODEL → 品牌` 的全量映射（`e0/c2.smali:1370-3391`）

- 懒加载：`if (a != null) return;`（`:1373-1375`），`new HashMap()` 后立即 `sput`（`:1377-1381`）。
- 先读 `ro.product.manufacturer`（`f0/l.i`），对 `vivo / samsung / realme / xiaodu / DOUQI` 五类**把当前 `Build.MODEL` 映射到该厂商值**（`:1496`、`:1689`）。
- 随后连续 302 条 `map.put(model, brand)`。品牌分布：

| 品牌 | 条数 | 品牌 | 条数 | 品牌 | 条数 |
|---|---|---|---|---|---|
| Xiaomi | 117 | OPPO | 92 | vivo | 20 |
| realme | 13 | Hisense | 7 | PTAC | 7 |
| samsung | 6 | Lenovo | 4 | ZTE | 4 |
| Liantong | 4 | nubia | 3 | OnePlus | 3 |
| WIKO | 3 | TDTECH | 3 | meizu | 2 |
| motorola/TCL/Coolpad/gionee/HUAWEI/KONKA/EWPE/YIK/HZTL/JingLing/TOSOT/5G HYUN/H10/sprd | 各 1 | | | | |

- 全量 304 行见 `findings\c2_map_a_full.tsv`（列：`smali_line / model / brand`）。

### 6.3 `h(Context)`：品牌 → reader 类（62 路 if-else，`:364-1368`）

入口先做一次注册：`if (SDK_INT >= 30) e0/r.k(ctx)`（`:367-373`）。然后 `a.get(Build.MODEL)`：命中则按品牌串 62 路比较（`equals`/`equalsIgnoreCase` 混用），未命中直接 `new e0/r()`。

| 品牌（区分大小写与否） | reader | 行 |
|---|---|---|
| LETV | `e0/r0` | 421 |
| HUAWEI | `e0/h0` | 436 |
| BROR | `e0/a` | 451 |
| Xiaomi | `e0/y1` | 466 |
| Onefive | `e0/b1` | 481 |
| OYSIN | `e0/e1` | 496 |
| meizu | `e0/u0` | 511 |
| BenWee | `e0/t` | 526 |
| INTKI | `e0/k0` | 541 |
| QIKU | `e0/l1` | 556 |
| EAMEY | `e0/b0` | 571 |
| ORANGE | `e0/d1` | 586 |
| K-Touch | `e0/p0` | 601 |
| Shown | `e0/n1` | 616 |
| YuLong | `e0/a2` | 631 |
| Hisense | `e0/g0` | 646 |
| ZTE | `e0/b2` | 661 |
| kingsun | `e0/n0` | 676 |
| KONKA / "konka"(忽略大小写) | `e0/o0` | 691 / 1113 |
| lephone | `e0/q0` | 706 |
| SINTAVE | `e0/o1` | 721 |
| TCL | `e0/s1` | 736 |
| PPTV | `e0/i1` | 751 |
| BestSonny | `e0/u` | 766 |
| SOP | `e0/p1` | 781 |
| OPPO | `e0/c1` | 796 |
| PHICOMM | `e0/g1` | 811 |
| Spreadtrum | `e0/r1` | 826 |
| BBK / vivo | `e0/u1` | 839/849 |
| CHANGHONG | `e0/v` | 860 |
| PHILIPS | `e0/h1` | 875 |
| Bee | `e0/s` | 890 |
| ChuiZi | `e0/w` | 905 |
| PROTRULY | `e0/j1` | 920 |
| Haipainoble | `e0/f0` | 935 |
| NOAIN | `e0/y0` | 950 |
| mfox | `e0/x0` | 965 |
| gionee | `e0/d0` | 980 |
| Xiaolajiao | `e0/x1` | 995 |
| motorola | `e0/w0` | 1010 |
| **nubia** | **`e0/z0`** | 1025 |
| sprd | `e0/c` | 1040 |
| Lenovo | `e0/s0` | 1055 |
| **EWPE** | **`e0/c0`** | 1070 |
| samsung | `e0/q1` | 1085 |
| realme | `e0/m1` | 1100 |
| OnePlus | `e0/f1` | 1128 |
| 5G HYUN | `e0/a1` | 1143 |
| Liantong / H10 | `e0/t0` | 1156/1166 |
| Coolpad | `e0/y` | 1177 |
| YIK | `e0/z1` | 1192 |
| **TOSOT** | **`e0/e0`** | 1207 |
| JingLing | `e0/m0` | 1222 |
| TDTECH | `e0/t1` | 1237 |
| PTAC | `e0/k1` | 1252 |
| HZTL | `e0/j0` | 1267 |
| WIKO | `e0/v1` | 1282 |
| xiaodu | `e0/w1` | 1297 |
| DOUQI | `e0/a0` | 1312 |
| 其它 | `e0/r`（基类） | 1319 |
| 映射表未命中 | `e0/r` + 日志 `"getReader error!! manufacture==null! model="` | 1341-1360 |

> 注：只有 `nubia(→e0/z0)`、`EWPE(→e0/c0)`、`TOSOT(→e0/e0)` 三家需要绑定厂商系统服务（见 §7），其余 reader 纯本地反射。

### 6.4 注册流程：`f()` / `g(ctx,String)`

`f(Context)`（`:139`，`synchronized`）—— "全量采集 + 应用统计"：
```
i(); g = new d0/g(); n(ctx,g); l(ctx,g); m(ctx,g); o(g); new d0/i(ctx,g,c2.d).start()
```

`g(Context,String regType)`（`:189`，`synchronized`）—— 由 `RegType` 决定后续动作：
```
g = new d0/g(); g.H = regType; n(ctx,g)
if (regType.trim()=="T")  { if (!l && !m) { log "SIM not changed，not post！"; return g } }   # 只上报 SIM 变化
else                      { l(ctx,g); m(ctx,g); }                                           # 无条件走
o(g)
f0/f.g = e(ctx)              # device_provisioned → 上报总开关
if (regType.trim()=="A") { if (f0/f.g) { new d0/i(...).start(); goto done } }
if (regType.trim()=="R") { new d0/j(...).start(); log "deviceinfo.signalinfo:"+g.N; goto done }
new d0/k(ctx,g).start()      # 兜底：直接上报
```
`RegType` 字面量来源（壳层）：`AppInfoRegisterReceiver="A"`（`:81`）、`SIMStateReceiver="T"`（`:229`）、`ScheduleRegisterReceiver="S"`（`:67`）、`OnBootReceiver="B"`（`:367`）、`RegisterIntentService` 取 Intent extra `"RegType"` 并 `toUpperCase()`，为空则走 `d0/g.a(ctx)`（`:195-263`）。**`"R"` 在本包内无调用者**（`d0/j` 因此实际是死路径）。

### 6.5 条件开关一览

| 开关 | 判定 | 位置 |
|---|---|---|
| 设备已初始化（用户已过 SetupWizard） | `Settings.Global.getInt("device_provisioned",0)==1` | `e0/c2.e()` `:96-137` |
| 是否上报/写盘 | `f0/f.g`（`device_provisioned`，写点很多：`e0/c2:272`、`c2$a:48`、各 reader 的 `b()`） | `d0/k.run()` `:596`；`c2$a` `:48` |
| 是否采集 IP | `f0/f.b` —— **无写入点，恒 false** → `d0/h` 实际不启动 | `c2$a` `:141` |
| 是否含信号历史 | `f0/f.e` —— **无写入点，恒 false** → `SignalInfo` 不入 JSON | `d0/g.d()` `:949`、`d0/f` `:520` |
| VoLTE 是否走"改系统设置"版本 | `f0/f.c` —— **无写入点，恒 false** → 恒走只读的 `e0/r.d()` | `e0/r.l()` `:1372` |
| 是否联通网络 | `f0/l.m(g)`（`DataSlot` + 46001/46006/46009） | `d0/k.d()` `:488` |
| SIM1/SIM2 是否更换 | prefs `iccid1`/`iccid2` 与 `g.h`/`g.s` 比对 | `c2.l()` `:3443`、`c2.m()` `:3580` |
| 是否 5G 能力 | `preferred_network_mode{slot}` ∈ [23,33] | `e0/r.j()` |
| 是否 VoLTE（联通专用判定） | 运营商 ∈ {46001,46006,46009} → `c()`（可写系统设置），否则 `d()`；受上一行 `f0/f.c` 二次限制 | `e0/r.l()` `:1372-1425` |
| 权限闸门 | `READ_PHONE_STATE`（多处）、`READ_PHONE_NUMBERS`、`ACCESS_FINE/COARSE_LOCATION`、`ACCESS_NETWORK_STATE` | `e0/r.l/r/c`、`d0/e/f`、`c2.n` |

### 6.6 有效路径 vs 死分支（实测语义修正）

`f0/f` 的 6 个布尔开关中**只有 `a`(同意协议)、`g`(device_provisioned)、`u`/`v`(SIM/网络状态)** 有 `sput` 写入点；`b`、`c`、`d`、`e`、`t` **全库无写入**（`f0/f.smali` 只有初值）。因此以下"设计路径"在**本版本运行时不成立**：

| 分支 | 触发条件 | 实际结果 |
|---|---|---|
| `d0/h`（IP 线程） | `f0/f.b == true`（`c2$a:141`） | 永不创建 → `IPv4/IPv6` 永不出现在 JSON；`d0/h` 的 IPv4/IPv6、`d0/h$a`、`c2$b` 全为死代码 |
| `d0/f.onSignalStrengthsChanged` 全流程 | `f0/f.e == true`（`d0/f:520`） | 立即返回 → 信号 JSON、fused 定位、`d0/f$a` 回调链均不触发 |
| `SignalInfo` 键 | `f0/f.e == true`（`d0/g.d:949`） | 不写入 |
| `e0/r.c()`（写 VoLTE 开关） | `f0/f.c == true`（`e0/r.l:1372`） | 恒走 `e0/r.d()`（只读），不修改系统设置 |
| `d0/j` + `SignalInfo` 装载 | `RegType == "R"` | 本包无 `"R"` 发送方 → 不可达 |
| `e0/r.n()`（API26 路径） | `SDK_INT < 26`（`e0/r.b:537`） | 前一条 `SDK<26` 已 `goto` → 不可达 |

→ **本版本实际生效的上报链路**为：`e0/c2.g()` 组装 `d0/g`（机型/SIM/小区/制式/VoLTE/5G/DataSlot/AccType/Time/RegVersion）→（`A` 分支再叠加 `AppInfo`）→ `d0/k` 直接 `POST` 到 `http://dm.wo.com.cn:18080/registerv3`，正文 35 个固定键（另加条件键：`IPv4/IPv6` 受 `f0/f.b`、`SignalInfo` 受 `f0/f.e`、`History` 受 `g.M != null`，上限 39 键），**实际不含 IPv4/IPv6/SignalInfo**；失败则落 `systemhelper.history`（≤100 条）等待下次重发。


### 6.7 异常处理与"是否已初始化"标志

- `c2.h()` 结果缓存在 `c2.b`；**没有** "initialized" 布尔量，`i()` 靠 `a == null` 判断首次。
- 所有 `f/g/n/l/m/o` 均 `synchronized`（类锁），`d0/g` 的 `d/e/f/g` 为实例锁 → 存在**类锁 + 实例锁**双锁结构。
- 内部大量 `try/catch Exception → printStackTrace()`（`e0/r.p/q` 每字段一段），失败静默降级为字段缺省。
- `n()` 中 `PackageManager$NameNotFoundException` 单独捕获（`:3743`）。
- `d0/g.a/b` 捕获后统一 `log "DeviceInfo"` 并 **返回 null**（调用方不判空 → 潜在 NPE）。

### 6.8 远程配置下发

**没有远程配置解析逻辑**。唯一的"可被远程改写"的落点是 `serverAddress`（prefs，`f0/l.t()` 写，`f0/l.g()` 读，`d0/k` 用它拼 URL），但全 APK 内 `f0/l.t(` 无调用者 → 实际恒为默认 `http://dm.wo.com.cn:18080/registerv3`。服务端响应体除状态码外**未被读取**（`f0/g.c` 只取 `getResponseCode()`），因此不存在基于响应的动态下发/指令通道。

---

## 7. AIDL 系统服务与设备标识获取

两组 AIDL 都是**厂商私有 Telephony 扩展服务**，用于在"标准 API 被 ROM 阉割"的设备上取 IMSI/IMEI/ICCID/号码/网络制式。

### 7.1 `b0/a` —— 努比亚（ZTE/nubia）`INBExtTelephony`

- 接口：`b0/a`（`extends IInterface`，10 方法），Stub `b0/a$a`，Proxy `b0/a$a$a`
- 描述符：`"com.android.phone.nubia.INBExtTelephony"`（`b0/a$a.smali:37`；Proxy 内每方法 `writeInterfaceToken` 都用它）
- 事务号（`b0/a$a$a.smali`，逐方法 `const/16` 提取）：

| 事务 | 方法 | 签名 |
|---|---|---|
| 1 | `v` | `()I` |
| 2 | `q` | `(I)I` |
| 3 | `t` | `(I)String` |
| 4 | `e` | `(I)String` |
| 5 | `k` | `(I)String` |
| 6 | `b` | `(I)String` |
| 7 | `c` | `(I)String` |
| 8 | `a` | `(I)I` |
| 9 | `f` | `(I)I` |
| 10 | `d` | `(I)I` |

- 特殊机制：Proxy 在 `transact()` 返回 false（服务不存在/已死）时，**回落到静态字段 `b0/a$a;->b:Lb0/a`**（本地实现），`b0/a$a;->x()` 暴露该字段（`b0/a$a.smali:61-67`）。即"远端失败→本地桩"。
- 服务端组件：`com.android.phone/com.android.phone.nubia.NBExtTelephonyService`（`RegisterIntentService.b()`，`RegisterIntentService.smali:104-116`，`bindService(..., BIND_AUTO_CREATE=1)`）。
- 绑定结果写入 `f0/f.A`（`RegisterIntentService$a` 的 `onServiceConnected`：`sput-object p1, Lf0/f;->A`，`RegisterIntentService$a.smali:38/48`）。
- 消费者：`e0/z0`（nubia reader）全程用 `f0/f.A`：`v()`（卡数）、`q(slot)`、`t(slot)`、`k(slot)`、`e(slot)`、`b(slot)`、`c(slot)`、`a(slot)`、`d(slot)`、`f(slot)`（`e0/z0.smali:70-330`）。

### 7.2 `c0/a` —— 格力（Gree）`IGreeTelephonyManager`

- 接口：`c0/a`（12 方法），描述符 `"com.gree.telephonymanager.IGreeTelephonyManager"`（`c0/a$a.smali:37`）
- 事务号：

| 事务 | 方法 | 签名 |
|---|---|---|
| 1 | `m` | `(I,String,String)String` |
| 2 | `e` | `(I)String` |
| 3 | `b` | `(I)String` |
| 4 | `a` | `(I)I` |
| 5 | `c` | `(I)String` |
| 6 | `d` | `(I)I` |
| 7 | `g` | `(I)I` |
| 8 | `p` | `()I` |
| 9 | `h` | `()I` |
| 10 | `n` | `()I` |
| 11 | `l` | `()I` |
| 12 | `j` | `(I)Z` |

- 服务端组件：`com.gree.telephonymanager/com.gree.telephonymanager.GreeTelephonyManagerService`（`RegisterIntentService.a()`，`RegisterIntentService.smali:58-70`）
- 绑定结果写入 `f0/f.B`（`RegisterIntentService$b.smali:44/60`）
- 消费者：`e0/c0`（`e0/c0.smali:60-353`）与 `e0/e0`（`e0/e0.smali:85-310`），用法模式：`p()/h()/n()/l()` 取"卡数/当前卡"，`m(slot,"","")` 取字符串（疑似 IMSI/ICCID/号码），`e/b/c` 取三类标识，`a/d/g` 取整型（信号/制式），`j(slot)` 取布尔。

### 7.3 绑定时机与前置条件

`com.mobiletools.systemhelper.service.RegisterIntentService`（`IntentService`，tag `"RegisterIntentService"`）：

1. `onCreate()`（`:123`）：`e0/c2.i()` → `e0/c2.h(ctx)` 选 reader；**只有** reader 是 `e0/z0`（nubia）→ `b(ctx)` 绑 nubia；是 `e0/c0` 或 `e0/e0`（Gree）→ `a(ctx)` 绑 gree。其它品牌**不绑定**。
2. `onHandleIntent()`（`:192`）：取 `RegType`；再按 reader 类型**自旋等待 5ms** 直到 `f0/f.A`（nubia）或 `f0/f.B`（gree）非 null（`Thread.sleep(5)` 死循环，无超时）；拿到后：
   - `regType == null` → `d0/g.a(ctx)`（并打 `"onStartCommand-->regType==null"`）
   - 否则 → `d0/g.b(ctx, regType.toUpperCase())`
   - 拿不到 → `"onHandleIntent-->iNBExtTelephony==null"` / `"onHandleIntent-->iGreeTelephony==null"`
3. 无 `unbindService`（`onDestroy` 只 `invoke-super`，`ServiceConnection` 泄漏）。

### 7.4 获取到的设备标识（全景）

| 标识 | 字段 | 主通道 | 兜底/厂商通道 |
|---|---|---|---|
| IMEI1 / IMEI2 | `g.g` / `g.r` | 反射 `getDeviceId()/(int)`（`e0/d2.b`，`e0/r.p/q`）或 `e0/i.a(tm,slot)`（`e0/r.o/l`） | nubia `b0/a` 的 `e/b/c/k/t`；gree `c0/a` 的 `e/b/c/m` |
| IMSI1 / IMSI2 | `g.i` / `g.t` | `getSubscriberId()` / `getSubscriberId(int)`（反射） | 同上 |
| ICCID1 / ICCID2 | `g.h` / `g.s` | `getSimSerialNumber()`、`SubscriptionInfo.getIccId()` | 同上 |
| MSISDN1 / MSISDN2 | `g.j` / `g.u` | `getLine1Number()`、`getLine1NumberForSubscriber(slot)`、`SubscriptionInfo.getNumber()` | 同上 |
| 运营商 | `g.k` / `g.v` | `getNetworkOperator()` / `getNetworkOperatorForSubscription(slot)`；<26 用 `getSimOperator()` | — |
| 小区 | `g.m/n`、`g.x/y` | `getCellLocation()`、`getNetworkRegistrationInfoList()`+`CellIdentity*`、prefs `"CellID0/1"`、`"LAC0/1"` | — |
| 序列号（Build.SERIAL） | 未使用 | — | — |
| Android ID / OAID / MAC | 未使用 | — | — |

---

## 8. 关键 smali 片段附录

### 8.1 上报 URL 拼装（`d0/k.smali:184-246`）
```smali
const-string v4, "utf-8"
invoke-static {v3, v4}, Ljava/net/URLEncoder;->encode(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;  # sign
move-result-object v3
iget-object v4, p1, Ld0/g;->b:Ljava/lang/String;      # Model
... encode → v4
iget-object v5, p1, Ld0/g;->a:Ljava/lang/String;      # Manuf
... encode → v5
invoke-static {v7}, Lf0/l;->g(Landroid/content/Context;)Ljava/lang/String;   # serverAddress
append "?ver="  "3.0"
append "&model=" v4
append "&manuf=" v5
append "&sign="  v3
```

### 8.2 正文加密 + 提交（`d0/k.smali:169-260`）
```smali
invoke-static {v0}, Lf0/c;->d(Ljava/lang/String;)[B        # MD5(json) → key
move-result-object v2
invoke-static {v3, v2}, Lf0/c;->c(Landroid/content/Context;[B)[B   # RSA(key)
invoke-static {v3}, Lf0/c;->b([B)Ljava/lang/String;        # Base64
invoke-static {v0, v2}, Lf0/c;->a(Ljava/lang/String;[B)[B   # AES(json,key) → body
...
invoke-virtual {v4, v3, v0, v2}, Lf0/g;->c(Ljava/lang/String;[B[B)Z
```

### 8.3 416 当成功（`d0/k.smali:264-282`）
```smali
if-nez v0, :cond_1
invoke-virtual {v4}, Lf0/g;->b()I
move-result v1
const/16 v2, 0x1a0
if-ne v1, v2, :cond_1
const/4 v0, 0x1
```

### 8.4 槽位归一（IMEI 小者当 slot1）（`e0/c2.smali:3957-4109`）
```smali
iget-object v1, p0, Ld0/g;->g:Ljava/lang/String;   # IMEI1
iget-object v2, p0, Ld0/g;->r:Ljava/lang/String;   # IMEI2
invoke-virtual {v1, v2}, Ljava/lang/String;->compareTo(Ljava/lang/String;)I
if-lez v1, :cond_1                              # IMEI1 > IMEI2 → 交换
  new d0/g 临时对象；把 1 组 10 个字段与 2 组 10 个字段互换；
  并把 DataSlot "1"↔"2"
```

### 8.5 非联通运营商 → 标识 MD5 化（`e0/c2.smali:4147-4274`）
```smali
iget-object v1, p0, Ld0/g;->k:Ljava/lang/String;    # 运营商号
invoke-static {v1}, Lf0/l;->n(Ljava/lang/String;)Ljava/lang/Boolean;   # ∈{46001,46006,46009}?
if-nez v1, :cond_2                                  # 非联通
  g.h = MD5(g.h); g.j = MD5(g.j); g.i = MD5(g.i); g.m = MD5(g.m); g.n = MD5(g.n)
  g.s = MD5(g.s); g.u = MD5(g.u); g.t = MD5(g.t); g.x = MD5(g.x); g.y = MD5(g.y)
```

### 8.6 监听器注册（`e0/r.smali:233-259`）
```smali
sget v2, Landroid/os/Build$VERSION;->SDK_INT:I
if-lt v2, v4            # v4 = 0x1f
  new-instance v2, Ld0/f;  invoke-direct {v2, p0, v1}      # ctx, slot=0
  sput-object v2, Lf0/f;->l:Ld0/f;
  invoke-static {p0}, Le0/j;->a(Landroid/content/Context;)Ljava/util/concurrent/Executor;
  invoke-static {v1, v2, v5}, Le0/k;->a(...)              # TelephonyManager, Executor, Callback
else
  new-instance v2, Ld0/e;  sput-object v2, Lf0/f;->j:Ld0/e;
  invoke-virtual {v1, v2, v3}, Landroid/telephony/TelephonyManager;->listen(...)   # v3 = 0x100500
```

### 8.7 UsageStats 排序（`d0/i$b.smali:46-62`）
```smali
invoke-virtual {p2}, Landroid/app/usage/UsageStats;->getTotalTimeInForeground()J
invoke-virtual {p1}, Landroid/app/usage/UsageStats;->getTotalTimeInForeground()J
sub-long/2addr v0, p1
long-to-int p1, v0          # ← 降序，但 long→int 截断（>2^31ms 会溢出）
```

### 8.8 移动流量按 IMSI 归属（`d0/i.smali:185-196`）
```smali
iget-object v3, v1, Ld0/g;->i:Ljava/lang/String;    # IMSI1
iget-wide v4, p0, Ld0/i;->c:J                       # start
iget-wide v6, p0, Ld0/i;->b:J                       # end
invoke-virtual {v1, v2, v3, v4, v5, v6, v7}, Landroid/app/usage/NetworkStatsManager;->querySummary(ILjava/lang/String;JJ)
# v2 = 0 (TYPE_MOBILE)，随后 bucket.getUid() == 本应用 uid 才累加
```

---

## 9. 未知项 / 待确认

1. **`f0/m`（signal DB）无写入方**：`d0/f.smali:946-956` 只取实例后丢弃返回值；全库无 `Lf0/m;->` 的 insert 调用。因此 `g.N`（`SignalInfo`）在实践中只可能来自 `d0/j` 对空表的查询 → `String.valueOf(null JSONArray)` 会得到字面量 `"null"`（`d0/j.smali:58`）。是否还存在第三方写入路径（如壳层直接 SQL）未验证。
2. **`"R"` RegType 无调用者**：`d0/j`（历史信号）在本包内不可达；是否存在外部 Intent/ADB 触发（`RegisterIntentService` 接受任意 `RegType` extra）未确认——`RegisterIntentService` 是否 exported 需查 Manifest（超出本层范围）。
3. **`e0/c2.b` 单例永不失效**：reader 选定后跨 SIM 变更/机型不变场景不会重算；`o()` 的槽位交换是有状态副作用。多次调用 `d0/g.b()` 是否会重复交换（幂等性）未在真机验证。
4. **`site`/服务端语义**：`HTTP 416` 被当成功、200 之外无解析，说明协议未文档化；`ver=3.0`、`SpecVersion=3.0`、`RegVersion=versionName` 三者关系只能从字面推断。
5. **`f0/f` 多个开关无写入点**：`sput-boolean ... Lf0/f;->b/c/d/e/t` 在全库零命中（仅静态初值），有写入的只有 `a`（`TestActivity:111`/`OnBootReceiver:50`/`RegisterHelperService$a:119`）、`g`（`e0/c2:272` 等 30+ 处）、`u`（`SIMStateReceiver`）、`v`（`ConnectStateReceiver`）。结论：IP 采集、信号历史、VoLTE 写入三条支路在本版本被"编译进但关掉"。是原设计留待服务端开关、还是被裁剪，无法从字节码判定（无 Prefs/网络读取该三个键的代码）。
6. **`e0/r.l()` 中 `setAdvancedCallingSettingEnabled(true)`**：仅 nubia/gree 以外机型也会走的通用路径（联通卡时），属"静默修改系统 VoLTE 开关"；是否真能成功取决于 IMS 权限，本层无法判定。
7. **`d0/e` 在 API<31 上没有信号强度回调**（未重写 `onSignalStrengthsChanged`），而 `f0/f.c` 只在 `d0/f` 里被赋值 → `e0/r` 的 `NCLS`/信号历史在旧系统上的完整性未知。
8. **`b0/a`、`c0/a` 的 `v()/q()/t()/k()/e()/b()/c()/a()/d()/f()` 语义**只能从调用顺序与上下文推断（`e0/z0` 把 `t/k/e/b/c` 当 IMSI/ICCID/号码/制式使用），AIDL 侧无 `@` 注释或字符串常量可供确认。
9. **`e0/r.h()` 用 `SDK_INT <= 33` 分支**：Android 14(34)+ 走 `getSubscriptionId`，但该 API 为 `@SystemApi`/隐藏接口，反射能否成功取决于 ROM；失败返回 `-1` → 后续 `createForSubscriptionId(-1)` 行为未验证。
10. **`e0/c2` 与 85 个采集器的关系**：本层未见 `c2` 调用任何 `e0/z1` 类采集器（`e0/*.smali` 中仅 `c2` 内部互相调用），采集器的调度入口在壳层（非本次范围），需与外壳分析结论交叉验证。
11. `d0/g.P/Q` 两个 final 常量、`d0/g.q` 字段、`d0/f.d`（`List<f0/n>`）均为**只声明/赋值、无读取**的死代码。
12. `f0/d` 表 100 行上限、`f0/m` 表 20MB 窗口等资源边界只在 `d0/k`/`d0/j` 侧观察到，未做真机压测。

---

### 附录 A：`RegType` 语义速查

| RegType | 发送方 | `e0/c2.g()` 行为 |
|---|---|---|
| `A` | `AppInfoRegisterReceiver` | 若 `device_provisioned` → 走 `d0/i`（应用使用+流量）→ 再 `d0/h`/`d0/k`；否则 `d0/k` |
| `S` | `ScheduleRegisterReceiver` | 直接 `d0/k`（周期上报） |
| `B` | `OnBootReceiver` | 直接 `d0/k`（开机上报） |
| `T` | `SIMStateReceiver` | 仅当 `iccid1/iccid2` 变化才继续，否则 `"SIM not changed，not post！"` 直接返回 |
| `R` | （本包无） | `d0/j` 装载信号历史到 `g.N` 后**不启动上报**（仅日志 `deviceinfo.signalinfo:`） |
| `null` | `RegisterIntentService` | 走 `d0/g.a(ctx)` → `e0/c2.f()`（全量采集 + `d0/i`） |

### 附录 B：落盘位置

| 数据 | 位置 | 证据 |
|---|---|---|
| 待上报 JSON 队列 | SQLite `systemhelper` → `history(id INTEGER PK AUTOINCREMENT, jsonString TEXT)` | `f0/d.smali:496` |
| 信号历史 | SQLite `signalhelper` → `signal(id, jsonString)` | `f0/m.smali:272` |
| 各类开关/ICCID 快照/服务器地址 | SharedPreferences `com_mobiletools_systemhelper_preference`（`iccid1`、`iccid2`、`serverAddress`、`appInfoInterval`、`scheduleInterval`、`signalInfoInterval`、`CellID0/1`、`LAC0/1`） | `f0/l.smali:160/146/132/58/743`、`e0/r.smali:2044/2226` |
| 公钥 | `assets/pub.cer`（X.509，RSA 公钥，用于加密 MD5 摘要） | `f0/c.smali:125-131` |
