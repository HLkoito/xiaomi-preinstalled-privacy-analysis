> **历史分析材料，含已纠正结论，不能作为最终结论引用。** 请先阅读 [最终复核报告](../REPORT.md)。原材料中的“MD5 派生密钥等于不加密”“全量已安装应用清单”“隐私告知永远不可达”“设为 1 秒即每秒上传”“无时间戳/已证实重放”等说法已被收窄或纠正；本次公开版未重新验证全部 OEM 分支。已删除本地身份路径，未包含真实用户采集值。

# SystemHelper APK — `e0/*` 采集器集合分析（collectors-2）

- **目标 APK**：`[LOCAL_PATH_REMOVED]`
- **包名**：`com.mobiletools.systemhelper`（versionName `1.2-20230925-U`，versionCode 3，minSdk 24，targetSdk 28）
- **分析素材**：apktool 3.0.3 解码 smali 树（只读）`...\analysis\SystemHelper\apktool\smali`；全量字符串清单 `...\all_const_strings.txt`
- **本次负责范围**：`e0\` 下 41 个类 —— `m, m0, m1, n, n0, n1, o, o0, o1, p, p0, p1, q, q0, q1, r0, r1, s, s0, s1, t, t0, t1, u, u0, u1, v, v0, v1, w, w0, w1, x, x0, x1, y, y0, y1, z, z0, z1`
  > 任务描述写“共 43 个文件”，实际列出的类名为 **41 个**，且 41 个文件全部存在、全部在本报告中逐类覆盖（`e0\r`、`e0\c2` 仅作为契约被阅读，未单独成节）。
- **方法**：以 smali 为主（字段 / 方法体 / `const-string` / `invoke-*` / `iput Ld0/g;` 全覆盖），配合脚本化结构抽取与克隆体比对；未修改 APK 与 smali 树。
- 文中行号均为对应 `.smali` 文件的**原始行号**。

---

## 0. 关键结论速览

1. **这 41 个类不是 41 种能力，而是“同一套采集模板 + 33 个 OEM 变体 + 8 个编译器桥”**。`e0/c2.h()` 用一串 `Build.MANUFACTURER` 字符串比较在 60 个分支里 `new-instance` 出对应厂商类；`e0/c2.n()` 再调 `((e0.c2$c)x).b(ctx, d0/g)` 完成一次“设备信息”采集。
2. **模板固定产出“双卡全量画像”**：`IMEI1/2`、`IMSI1/2`、`ICCID1/2`、`MSISDN1/2`、`MNC1/2`、`NCLS1/2`（网络类型）、`CellID1/2`、`LAC1/2`、`VolteEnabled1/2`、`5GEnabled1/2`、`DataSlot`，加上 `Manuf/Model/HWVersion/SWVersion/OS/OSVersion`。即 `d0.g` 的 `a`–`B` 字段几乎被填满。
3. **几乎所有敏感读取都绕开公开 API**：统一走 `f0.o`（`Class.forName` + `getMethod` + `invoke`）调隐藏方法，例如 `TelephonyManager.getDeviceId/getSubscriberId/getSimSerialNumber/getLine1Number/getImei/getNetworkOperatorForPhone/getCellLocationForPhone`、`SubscriptionManager.getSubId/getPhoneId/isValidSubscriptionId`、`MSimTelephonyManager.getDefault`、`HwTelephonyManager.getServiceAbility`、`miui.telephony.TelephonyManager`、`ColorOSTelephonyManager`、`MzTelephonyManager`、`com.mediatek.telephony.TelephonyManagerEx`、`com.android.internal.telephony.PhoneFactory.getPhone`。
4. **两处真正“跨进程 / 系统级”能力**：
   - `e0/y0`：`Class.forName("com.mediatek.internal.telephony.ITelephonyEx$Stub")` + `ServiceManager.getService("phoneEx")` + `asInterface` → 直接调 MTK `ITelephonyEx.getSvlteImei(slot)`（Binder 调用，`e0/y0.smali:15-127`）。
   - `e0/u1`：`getITelephony()` + `com.android.internal.telephony.VivoTelephonyApiParams.getParamValues()` → 取 vivo 私有 5G/VoLTE 参数（`e0/u1.smali:1325-1852`）。
5. **有副作用，不只是“读”**：基类 `e0/r.c()` 检测到 VoLTE 关闭时会**反射调用 `ImsMmTelManager.setAdvancedCallingSettingEnabled(true)` 把 VoLTE 打开**（`e0/r.smali:592-636`）。这是本模块唯一对系统状态的写入，需要 `MODIFY_PHONE_STATE`；由 `t0、t1、v1、w0、w1、q1、u1、y1` 以及直接 `invoke Le0/r;->l()` 的 `m0、m1、s1、y、z1` 触发。
6. **品牌白名单是硬编码的可执行模型清单**：`e0/c2.i()` 把“型号 → 厂商”写进静态 Map（`SM-G9750/SM-N9760/MI 9/Redmi K30 5G/…`），`e0/y1`（小米）自带 200+ 型号白名单，`e0/x1`（小辣椒）用 20+ 个 `Build.MODEL` 逐型号选属性。
7. **`a(Context)` 门禁是“死桩”**：41 个类里所有被覆写的 `a()` 都是 `new ServiceState().getRoaming()`（恒 `false`），`e0/c2.k()` 因此恒返回 `Boolean.FALSE`，且 `c2.k()` 全 APK 无调用者。**厂商选择只由厂商字符串决定，采集无条件执行**。
8. **权限降级是“静默置空”**：拿不到权限 / 抛异常时字段留 `null`，由 `d0/g.e()` 统一填 `"Unknown"` 或 `""`；`e0/c2.n()` 还会把空 IMSI 主动 `iput null`（`e0/c2.smali:3770-3790`）。
9. **代码高度复制粘贴**：`e0/s1`(TCL)、`e0/y`(Coolpad)、`e0/z1`(YinKe) 归一化后 Jaccard ≥95.6%（几乎逐行相同，只差日志 tag、型号字符串、属性名）；`e0/t1`(TDTECH) 与 `e0/v1`(WIKO) 57.8%。`e0/y` 的“未知型号”日志甚至误写成 `DeviceInfoReaderOPPO module unknow!!!`（复制残留）。

---

## 1. 总表（类名 | 业务含义 | 关键 Android API | 输出键名 | 隐私等级）

隐私等级：**P0** 仅机型/版本；**P1** 设备硬件标识（IMEI 等）；**P2** SIM 身份（IMSI/ICCID/MSISDN）；**P3** 位置（CellID/LAC/CellInfo）；**P4** = P1+P2+P3 全量画像（+ 厂商私有 5G/VoLTE 状态）。

| 类名 | 业务含义 | 关键 Android API / 隐藏面 | 输出键名（d0.g 字段 → JSON key） | 隐私 |
|---|---|---|---|---|
| `e0/m` | **合成桥**：`NetworkRegistrationInfo.getDomain()` | `android.telephony.NetworkRegistrationInfo#getDomain`（API 29） | 无（返回 int 给调用者） | P0 |
| `e0/m0` | JingLing（京灵 / JingPad）采集器 | `TelephonyManagerEx`(反射) `getImei`/`getCellLocationForPhone`；`SubscriptionManager.getPhoneId` | `a Manuf,b Model,c HWVersion,d SWVersion,e OS,f OSVersion,g IMEI1,h ICCID1,i IMSI1,j MSISDN1,k MNC1,l NCLS1,m CellID1,n LAC1,o VolteEnabled1,p 5GEnabled1,r IMEI2,s ICCID2,t IMSI2,u MSISDN2,v MNC2,w NCLS2,z VolteEnabled2,B DataSlot,F SubType,G DeviceType` | **P4** |
| `e0/m1` | realme 采集器（含 DRAM/RAM 解析） | `ColorOSTelephonyManager.getDefault`；`isOemEnhanced4gLteModeSettingEnabledByUser`；`SystemProperties ro.boot.dram_info`/`ro.build.hardware.version` | 同上 + `B/F/G` | **P4** |
| `e0/n` | **合成桥**：`NetworkRegistrationInfo.getTransportType()` | `NetworkRegistrationInfo#getTransportType`（API 29） | 无 | P0 |
| `e0/n0` | kingsun 采集器（**三套读法**） | `MSimTelephonyManager.getDefault` + `getPhoneCid`/`getPhoneLac`/`getSimOperator` | 全量双卡 + `m/n/x/y` | **P4** |
| `e0/n1` | Shown（360OS）采集器 | `SystemProperties`（`gsm.mtk.imei1/2`、`ro.system.OSVersion`、`ro.internal.version`、`ro.sf.lcd_density`）；通用 `Class.forName` 反射工具 | 全量双卡 + `m/n/x/y` | **P4** |
| `e0/o` | **合成桥**：`NetworkRegistrationInfo.getCellIdentity()` | `NetworkRegistrationInfo#getCellIdentity`（API 29） | 无 | P0 |
| `e0/o0` | KONKA 采集器 | `TelephonyManagerEx`(反射) `getCellLocationForPhone`；`getNetworkOperatorForPhone`；`getSubId`；`ro.boot.hardware.revision` | 全量双卡 + `B/F/G` | **P4** |
| `e0/o1` | OYSIN / SINTAVE 采集器 | `SubscriptionManager.isValidSubscriptionId`；`getDeviceId`；`getCellLocationForPhone` | 全量双卡 | **P4** |
| `e0/p` | **合成桥**：`CellIdentityTdscdma.getCid()` | `android.telephony.CellIdentityTdscdma#getCid`（API 29） | 无（TD-SCDMA CID） | P0 |
| `e0/p0` | K-Touch 采集器 | `com.android.internal.telephony.PhoneFactory.getPhone(i)`；`getPhoneCount`；`getSimOperator` | 全量双卡 | **P4** |
| `e0/p1` | SOP 采集器（**内存 / 存储解析**） | `/proc/meminfo`；`StatFs`；`isValidSubscriptionId`；`persist.sys.hardware.version` | 全量双卡 + `m/n/x/y` | **P4**（+容量） |
| `e0/q` | **合成桥**：`CellIdentityTdscdma.getLac()` | `CellIdentityTdscdma#getLac`（API 29） | 无（TD-SCDMA LAC） | P0 |
| `e0/q0` | lephone 采集器（**五套读法**） | `getAllCellInfo`（列表解析）；`getCellLocationForPhone`；`getNetworkOperatorForSubscription`；`getMsisdn` | 全量双卡 + `m/n/x/y` | **P4** |
| `e0/q1` | 三星采集器（**5G/VoLTE 专精**） | `getTelephonyProperty("ril.hw_ver"/"ro.build.PDA")`；`createForSubscriptionId`；`getCellLocationBySubId`；`Settings$System.getInt("voicecall_type")`；`ro.telephony.default_network` | 全量双卡 + `B/F/G` + `p/A/z` | **P4** |
| `e0/r0` | LETV / 乐视采集器（**五套读法**） | `com.mediatek.telephony.TelephonyManagerEx.getDefault`；`getCellLocationFromSlotId`；`getSimOperator` | 全量双卡 | **P4** |
| `e0/r1` | Spreadtrum / 展讯采集器 | `phone_ex`(TelephonyManagerEx) + `getDeviceId` + `getMsisdn`；`getSubId` | 全量双卡 | **P4** |
| `e0/s` | Bee 采集器 | `getAllCellInfo` + 列表解析；`ro.sf.lcd_density` | 全量双卡 + `m/n/x/y` | **P4** |
| `e0/s0` | Lenovo 采集器（**仅单卡**） | `isValidSubscriptionId`；`getDeviceId`…；`Settings$Global.getInt`；`getDefaultDataSubscriptionId`+`getPhoneId` | `g…l,o` + `B/F/G` | **P3+** |
| `e0/s1` | TCL 采集器（**与 y/z1 同源**） | `ColorOSTelephonyManager`；`isOemEnhanced4gLteModeSettingEnabledByUser`；`ro.mtk.build.version`/`ro.tcl.hardware.version` | 全量双卡 + `B/F/G` | **P4** |
| `e0/t` | BenWee 采集器 | `android.telephony.MSimTelephonyManager.getDefault`（全字段） | 全量双卡 | **P4** |
| `e0/t0` | “Liantong”（联通定制）采集器 | `ColorOSTelephonyManager`；`Settings$Global`（`is5GEnable`/`preferred_network_mode`）；`isOemEnhanced4gLteModeSettingEnabledByUser`；READ_PHONE_STATE 门禁 | 全量双卡 + `B/F/G` + `p/A/o/z` | **P4** |
| `e0/t1` | TDTECH（鼎桥）采集器 | `HwTelephonyManager.getServiceAbility`/`isVolteAvailable`；`MSimTelephonyManager`；`e0.i0` 取 subId 数组；`ro.boot.dram_info` | 全量双卡 + `B/F/G` + `m/n/x/y` + `p/A/o/z` | **P4** |
| `e0/u` | BestSonny 采集器 | `getDeviceId`/`getCellLocationForPhone`/`getMsisdn`/`getNetworkOperatorForPhone` | 全量双卡 | **P4** |
| `e0/u0` | 魅族采集器 | `MzTelephonyManager`；`getDefaultSubscriptionId`；`getSimOperatorNumericForPhone` | 全量双卡 + `B/F/G` | **P4** |
| `e0/u1` | vivo 采集器（**最深**） | `ITelephony#getITelephony` + `VivoTelephonyApiParams.getParamValues`；`persist.radio.vivo.fiveg`/`persist.vendor.radio.5g_mode_pref`；`ro.vivo.op.entry` | 全量双卡 + `B/F/G` + `m/n/x/y` + `p/A/o/z` | **P4** |
| `e0/v` | CHANGHONG 采集器（**自身只读版本，靠 `base.p()` 补齐**） | 无系统服务；仅 `SystemProperties`(`ro.denqin.*`, `ro.yunos.*`) | `a,b,c,d,e,f`（自身 6 项）+ 基类补双卡 | **P4** |
| `e0/v0` | **合成桥**：`SubscriptionInfo.getMncString()` | `android.telephony.SubscriptionInfo#getMncString`（API 29） | 无 | P0 |
| `e0/v1` | WIKO / PTAC 采集器（≈t1） | 同 `t1` + 直连 `TelephonyManager` 回退 + 变长反射参数 | 同 `t1` | **P4** |
| `e0/w` | ChuiZi（锤子 / 坚果）采集器 | `getImei`/`getNetworkOperatorForPhone`/`getSimOperatorNumericForPhone`；`ro.smartisan.version` | 全量双卡 | **P4** |
| `e0/w0` | Motorola / ZUI 采集器 | `getActiveSubscriptionInfoForSimSlotIndex(slot)`；`ro.zui.software.display`；`ro.boot.dram_info` | 全量双卡 + `B/F/G` + `p/A/o/z` | **P4** |
| `e0/w1` | xiaodu（小度）采集器（**存储画像**） | `Environment`+`StatFs`（blockSize/availableBlocks/blockCount）；`ro.boot.5GEnable1/2`；`persist.sys.cucc.dm` | 全量双卡 + `B/F/G` + `p/A/o/z` | **P4**（+分区容量） |
| `e0/x` | **合成桥**：`SubscriptionManager.getSlotIndex(subId)` | `SubscriptionManager#getSlotIndex`（API 29，26 处调用） | 无 | P0 |
| `e0/x0` | mfox 采集器 | `gsm.mtk.imei1/2`（MTK 双 IMEI 属性）；`e0.d2` 反射 subId 读取 | 全量双卡 + `m/n/x/y` | **P4** |
| `e0/x1` | 小辣椒采集器（**型号分支最碎**） | 20+ `Build.MODEL` 分支 + `ro.build.display.hwid`/`ro.product.yuxin*` 等；`getNetworkOperatorForPhone`/`getSimOperator` | 全量双卡 + `m/n/x/y` | **P4** |
| `e0/y` | Coolpad 采集器（≈s1/z1） | `ColorOSTelephonyManager`；`ro.board.hardware_version` | 全量双卡 + `B/F/G` | **P4** |
| `e0/y0` | BROR / NOAIN 采集器 | **`ITelephonyEx$Stub` + `ServiceManager.getService("phoneEx")` + `getSvlteImei`**；`getMsisdn`；`getCellLocationForPhone` | 全量双卡 + `m/n/x/y` | **P4** |
| `e0/y1` | 小米采集器（**最大，200+ 型号白名单**） | `miui.telephony.TelephonyManager(Ex)`：`getSubscriberIdForSubscription`/`getSimSerialNumberForSubscription`/`getNetworkTypeForSubscription`/`getCellLocationForSlot`/`isFiveGCapable`；`/proc/meminfo` | 全量双卡 + `B/F/G` + `p/A/o/z` | **P4** |
| `e0/z` | **合成桥**：`SubscriptionManager.getActiveDataSubscriptionId()` | `SubscriptionManager#getActiveDataSubscriptionId`（API 29） | 无 | P0 |
| `e0/z0` | nubia 采集器 | `ro.nubia.nr.support`；`Settings$Secure.getInt`；`getSubId`；`ro.build.rom.pcb.silkscreen` | 全量双卡 + `B/F/G` + `m/n/x/y` + `o/z` | **P4** |
| `e0/z1` | YinKe 采集器（≈s1/y） | `ColorOSTelephonyManager`；`ro.board.hardware_version` | 全量双卡 + `B/F/G` | **P4** |

---

## 1.1 权威字段写入表（大小写敏感，作为各小节的最终口径）

> **口径说明**：`d0/g` 字段名区分大小写（`b`=Model 与 `B`=DataSlot、`g`=IMEI1 与 `G`=DeviceType、`f`=OSVersion 与 `F`=SubType 是不同字段）。下表由 `iput-object … Ld0/g;->X:` **逐行大小写敏感**抽取，`X@行号` 为首次写入位置。若总表/小节与本表冲突，**以本表为准**。

| 类 | 字段数 | 写入字段@行号 |
|---|---|---|
| `m0` | 25 | `m@159 n@165 g@293 i@325 h@383 j@411 k@435 l@498 o@504 r@540 t@572 s@606 u@632 v@656 w@719 z@751 b@770 a@780 e@784 f@788 F@794 B@896 G@930 d@934 c@942` |
| `m1` | 23 | `g@255 i@287 h@349 j@377 k@401 l@464 o@470 r@530 t@562 s@596 u@622 v@646 w@709 z@715 b@1102 a@1112 e@1116 f@1120 F@1126 B@1228 G@1389 d@1397 c@1413` |
| `n0` | 22 | `g@72 i@98 h@152 j@176 k@200 l@263 m@289 n@315 r@355 t@381 s@415 u@439 v@463 w@526 x@552 y@578 b@1174 a@1184 e@1188 f@1192 d@1218 c@1220` |
| `n1` | 22 | `g@253 i@297 h@329 j@353 k@377 l@411 r@468 t@496 s@528 u@552 v@576 w@610 m@1261 n@1269 x@1426 y@1434 b@1461 a@1471 e@1475 f@1479 d@1524 c@1528` |
| `o0` | 21 | `g@314 i@364 h@396 j@420 k@440 l@474 r@549 t@577 s@609 u@633 v@653 w@687 b@728 a@738 e@742 f@746 F@752 B@854 G@888 c@894 d@898` |
| `o1` | 18 | `g@134 i@178 h@210 j@234 k@258 l@292 r@349 t@377 s@409 u@433 v@457 w@491 b@538 a@548 e@552 f@556 d@605 c@615` |
| `p0` | 18 | `g@130 i@156 h@196 j@220 k@244 l@278 r@408 t@432 s@464 u@488 v@512 w@546 b@1017 a@1027 e@1031 f@1035 d@1074 c@1084` |
| `p1` | 22 | `g@372 i@414 h@446 j@470 k@494 l@557 m@567 n@575 r@612 t@638 s@670 u@694 v@718 w@781 x@793 y@801 b@828 a@838 e@842 f@846 d@866 c@874` |
| `q0` | 22 | `m@155 x@160 n@172 y@177 g@306 i@332 h@376 j@400 k@424 l@487 r@523 t@547 s@581 u@605 v@629 w@692 b@2714 a@2724 e@2728 f@2732 c@2791 d@2795` |
| `q1` | 25 | `c@687 d@713 B@811 g@875 i@905 h@937 j@961 k@985 l@1019 o@1025 r@1076 t@1106 s@1138 u@1162 v@1186 w@1220 z@1226 p@1774 A@2344 b@2497 a@2501 e@2505 f@2509 F@2515 G@2527` |
| `r0` | 18 | `g@76 i@102 h@156 j@180 k@204 l@267 r@323 t@349 s@383 u@407 v@431 w@494 b@541 a@551 e@555 f@559 d@585 c@595` |
| `r1` | 18 | `g@206 i@254 h@286 j@310 k@330 l@364 r@437 t@465 s@497 u@521 v@541 w@575 b@626 a@636 e@640 f@644 d@658 c@668` |
| `s` | 22 | `m@209 x@214 n@226 y@231 g@415 i@441 h@485 j@509 k@533 l@596 r@632 t@656 s@690 u@714 v@738 w@801 b@1330 a@1340 e@1344 f@1348 d@1370 c@1378` |
| `s0` | 16 | `g@62 i@98 h@128 j@150 k@172 l@231 o@259 b@439 a@449 e@453 f@457 F@463 B@565 G@599 d@603 c@609` |
| `s1` | 23 | `g@151 i@213 h@249 j@273 k@297 l@360 o@366 r@437 t@471 s@505 u@529 v@553 w@616 z@622 b@667 a@677 e@681 f@685 F@691 B@793 G@827 d@835 c@843` |
| `t` | 18 | `g@78 i@100 h@142 j@162 k@182 l@212 r@260 t@280 s@310 u@330 v@350 w@380 b@427 a@437 e@441 f@445 d@459 c@463` |
| `t0` | 25 | `g@59 r@89 i@127 h@141 j@147 k@153 p@161 l@171 o@228 t@251 s@265 u@271 v@277 A@287 w@297 z@354 b@2135 a@2145 e@2149 f@2153 F@2159 B@2261 G@2342 d@2348 c@2354` |
| `t1` | 29 | `g@119 r@147 i@185 h@199 j@205 k@211 l@215 o@272 p@311 t@334 s@348 u@354 v@360 w@366 z@423 A@462 m@1066 n@1070 x@1459 y@1463 b@1852 a@1862 e@1866 f@1870 F@1876 B@2038 G@2115 d@2127 c@2135` |
| `u` | 18 | `g@200 i@244 h@276 j@300 k@320 l@354 r@415 t@443 s@475 u@499 v@519 w@553 b@600 a@610 e@614 f@618 d@665 c@671` |
| `u0` | 21 | `g@245 i@267 h@319 j@339 k@359 l@414 r@474 t@500 s@534 u@558 v@578 w@641 b@682 a@692 e@696 f@700 F@706 B@780 G@837 d@841 c@845` |
| `u1` | 29 | `g@229 i@313 h@341 j@361 k@381 p@393 l@404 o@412 r@569 t@621 s@653 u@677 v@697 A@709 w@720 z@728 m@996 x@1001 n@1015 y@1020 b@2024 a@2034 e@2038 f@2042 c@2048 d@2054 F@2060 B@2162 G@2192` |
| `v` | 6 | `b@36 a@46 e@50 f@54 c@74 d@82`（其余 SIM 字段由 `Le0/r.p()` 在 `r.smali` 内写入） |
| `v1` | 29 | `g@83 r@111 i@149 h@163 j@169 k@175 l@187 o@244 p@283 t@306 s@320 u@326 v@332 w@344 z@401 A@440 m@1040 n@1044 x@1433 y@1437 b@1826 a@1836 e@1840 f@1844 F@1850 B@1952 G@2031 d@2043 c@2051` |
| `w` | 18 | `g@182 i@234 h@262 j@282 k@304 l@359 r@408 t@440 s@472 u@496 v@518 w@581 b@614 a@624 e@628 f@632 d@648 c@654` |
| `w0` | 25 | `g@454 r@460 i@511 h@525 j@531 k@539 p@548 l@558 o@620 t@677 s@691 u@697 v@705 A@714 w@724 z@800 b@933 a@943 e@947 f@951 F@957 B@1059 G@1083 d@1091 c@1099` |
| `w1` | 25 | `G@524 g@532 i@580 h@612 j@636 k@660 l@723 p@733 o@739 r@752 t@788 s@820 u@844 v@868 w@931 A@941 z@947 B@999 b@1030 a@1040 c@1046 d@1052 e@1056 f@1060 F@1066` |
| `x0` | 22 | `b@36 a@46 e@50 f@54 c@74 d@78 g@137 i@178 h@209 j@230 k@251 l@276 m@294 n@302 r@318 t@343 s@374 u@395 v@416 w@441 x@459 y@467` |
| `x1` | 22 | `g@194 i@220 h@268 j@292 k@312 l@346 m@356 n@364 r@400 t@426 s@460 u@484 v@504 w@538 x@548 y@556 b@2423 a@2433 e@2437 f@2441 d@2492 c@2500` |
| `y` | 23 | `g@151 i@213 h@249 j@273 k@297 l@360 o@366 r@437 t@471 s@505 u@529 v@553 w@616 z@622 b@667 a@677 e@681 f@685 F@691 B@793 G@827 d@835 c@843` |
| `y0` | 22 | `g@248 i@298 h@330 j@354 k@378 l@441 r@504 t@532 s@564 u@588 v@612 w@675 b@722 a@732 e@736 f@740 d@762 c@768 m@1104 n@1112 x@1269 y@1277` |
| `y1` | 25 | `g@94 i@156 h@184 j@204 k@224 l@279 r@340 t@374 s@406 u@430 v@454 w@517 p@797 o@817 A@1081 z@1173 b@1638 a@1648 e@1652 f@1656 F@1662 B@1764 G@1804 c@2967 d@2973` |
| `z0` | 27 | `B@84 g@100 i@108 h@131 j@139 k@147 l@161 n@173 m@185 o@195 r@210 t@218 s@234 u@242 v@250 w@264 y@276 x@288 z@298 b@348 a@358 e@362 f@366 G@394 F@400 d@449 c@457` |
| `z1` | 23 | `g@151 i@213 h@249 j@273 k@297 l@360 o@366 r@437 t@471 s@505 u@529 v@553 w@616 z@622 b@667 a@677 e@681 f@685 F@691 B@793 G@827 d@835 c@843` |

**大写字段的全局写入者**（`C/D/E/H/I/J/K/L/M/N` 在这 33 个类中**无人写入**）：

| 字段 | 语义 | 写入类 |
|---|---|---|
| `B` | `DataSlot` | `m0 m1 o0 q1 s0 s1 t0 t1 u0 u1 v1 w0 w1 y y1 z0 z1`（17 个） |
| `F` | `SubType`(JSONObject) | 同 `B`（17 个完全一致） |
| `G` | `DeviceType` | 同 `B`（17 个完全一致） |
| `A` | `5GEnabled2` | `q1 t0 t1 u1 v1 w0 w1 y1`（8 个） |

> 观测：`B/F/G` 三者写入者完全重合且总在同一段 `b()` 收尾里连续出现 —— 它们是**同一“设备注册块”**（数据卡槽 + 子类型 + 设备类型），由 17 个较新的采集器统一产出；`A`（5G 卡 2）只有 8 个“5G 专精”采集器写。

---

## 2. 公共契约（理解 41 个类的前提）

### 2.1 `e0/c2` 调度：厂商 → 采集器

| 位置 | 行为 |
|---|---|
| `e0/c2.smali:364-1369` | `h(Context)`：`SDK_INT>=30` 先 `e0.r.k(ctx)` 注册 TelephonyCallback；`c2.a.get(Build.MODEL)` 得到厂商串；随后 **60 个 `String.equals` 分支** `new-instance` 对应采集器；未命中 → 基类 `e0/r`（`e0/c2.smali:1319, 1358`）。|
| `e0/c2.smali:1370-3392` | `i()`：懒加载静态 `Map a`（**型号 → 厂商**）。先用 `SystemProperties.get("ro.product.manufacturer")`（`f0/l.i`）判断是否属于 `vivo/samsung/realme/xiaodu/DOUQI` 白名单，命中则 `a.put(Build.MODEL, manuf)`；之后硬编码数百条 `a.put("SM-G9750","samsung")`…`a.put("XT2335-3","motorola")`。|
| `e0/c2.smali:3404-3417` | `j(Context)` → `h(ctx).a(ctx)`；`k(Context)` 包装为 `Boolean`。**全 APK 内无调用者**，且所有 `a()` 恒 false → 死代码。|
| `e0/c2.smali:3717-3927` | `n(Context,d0/g)`：`g.J = PackageManager.getPackageInfo(pkg,0).versionName`；`h(ctx).b(ctx,g)`；随后把空 IMSI/IMEI2 等清成 `null`。|
| `e0/c2.smali:139-187` | `f(Context)`：`i()` → `new d0/g` → `n()` → `l()`（ICCID 变更检测，prefs key `iccid1`）→ `m()` → `o()` → `new d0/i(...).start()`（异步线程补采）。|

`e0/c2$c` 接口即 `a(Context)Z` + `b(Context,d0/g)V` 两个方法（`e0/r` 实现）。

### 2.2 `e0/r` 基类 “DeviceInfoReaderBase”（`e0/r.smali`，2948 行）

| 方法 | 语义（已核对实现） |
|---|---|
| `b(ctx,g)` `:491` | 填 `g.b=Build.MODEL`、`g.a=Build.MANUFACTURER`、`g.e="Android"`、`g.f=Build.VERSION.RELEASE`、`g.d=Build.DISPLAY`、`g.c=SystemProperties("ro.hardware.version")`；`SDK<26 → m()`，否则 `o()`（`n()` 同一条件重复判断 → 不可达；`l()` 在基类 `b()` 中不可达）。|
| `p(ctx,g)` `:1862` / `q(ctx,g)` `:2256` | 双卡反射全量读取：`getDeviceId`/`getNetworkType`/`getNetworkOperator(ForSubscription)`/`getLine1NumberForSubscriber`/`getSimSerialNumber`/`getSubscriberId`；并把 `CellID0/LAC0/CellID1/LAC1` 写回。|
| `c(ctx,slot)` `:560` | **VoLTE 读取 + 强制开启**：`getSystemService("telephony_ims")`→`ImsManager`；`e0.g.a(ImsManager,subId)`→`ImsMmTelManager`；`e0.h.a(ImsMmTelManager)`→`isAdvancedCallingSettingEnabled()`；**关闭时反射 `setAdvancedCallingSettingEnabled(true)`**；返回 `"1"/"0"/"-1"`。|
| `d(ctx,slot)` `:690` | 同上另一路径（另一条 IMS 读取变体）。|
| `f(tm,subId)` `:745` | 反射 `getNetworkType(subId)`，为 0 再试 `getVoiceNetworkType(subId)`；全异常 → 0。|
| `g(netType,operator)` `:814` | **网络类型 → 字符串**（见 2.4）。|
| `i(slot)` `:987` | 反射 `SubscriptionManager.getSubId(slot)` → `int[]`，日志 `[getSubId] sub[0] is ` / `[getSubId] result is `。|
| `j(ctx,slot)` `:1168` | **5G 判定**：`Settings.Global.getInt("preferred_network_mode"+slot, -1)`，`23 <= v <= 33` → true。|
| `k(ctx)` `:187` / `s(ctx)` `:373` | 注册 / 注销 `TelephonyCallback`（日志 `registerTelephonyCallback--mDMTelephonyCallback1/2`、`listenNetworkType---->`）。|
| `r(ctx,slot,g)` `:2614` | 位置采集：`READ_PHONE_STATE` 判定 → `CellIdentity` 解析（`Le0/m`,`n`,`o`,`p`,`q` 桥在此处，`:2692/2700/2716/2728/2787/2795`）→ `"%d:%d"` 拼 CID:LAC。|

### 2.3 `d0/g` 字段 → 上报 JSON 键（`d0/g.e()` 逐字段 `JSONObject.put`，`d0/g.smali:1038-1697`）

| 字段 | JSON key | 字段 | JSON key | 字段 | JSON key |
|---|---|---|---|---|---|
| `a` | `Manuf` | `n` | `LAC1` | `A` | `5GEnabled2` |
| `b` | `Model` | `o` | `VolteEnabled1` | `B` | `DataSlot` |
| `c` | `HWVersion` | `p` | `5GEnabled1` | `C` | `AccType` |
| `d` | `SWVersion` | `r` | `IMEI2` | `D` | `IPv4` |
| `e` | `OS` | `s` | `ICCID2` | `E` | `IPv6` |
| `f` | `OSVersion` | `t` | `IMSI2` | `F` | `SubType`(JSONObject) |
| `g` | `IMEI1` | `u` | `MSISDN2` | `G` | `DeviceType` |
| `h` | `ICCID1` | `v` | `MNC2` | `H` | `RegType` |
| `i` | `IMSI1` | `w` | `NCLS2` | `I` | `SpecVersion`(默认`"3.0"`) |
| `j` | `MSISDN1` | `x` | `CellID2` | `J` | `RegVersion`(默认`"1.0"`) |
| `k` | `MNC1` | `y` | `LAC2` | `K` | `Time` |
| `l` | `NCLS1` | `z` | `VolteEnabled2` | `L`/`M` | `AppInfo`/`SignalInfo` |
| `m` | `CellID1` | | | `N` | `History`；`P`/`Q` 为 int（语义未知） |

默认值：`a–g` 缺省 `"Unknown"`，`h–p/r–A` 缺省 `""`，`B` 缺省 `"0"`。

### 2.4 `e0/r.g()` 网络类型映射（`packed-switch` 基数 1，`e0/r.smali:962-984`）

| NETWORK_TYPE 值 | 名称 | 映射结果 |
|---|---|---|
| 1,2,16 | GPRS/EDGE/GSM | `GSM` |
| 3,8,9,10,15 | UMTS/HSDPA/HSUPA/HSPA/HSPAP | 运营商为 `46000/46002/46007` → `TD-SCDMA`，否则 `WCDMA` |
| 4,5,6,7,12,14 | CDMA/EVDO_0/EVDO_A/1xRTT/EVDO_B/eHRPD | `CDMA` |
| 13,19 | LTE/LTE_CA | `LTE` |
| 17 | TD_SCDMA | `TD-SCDMA` |
| 20 | NR | `NR` |
| 11,18 | iDEN/IWLAN | 落 `default` → 日志 `unknown networktype ` |

> 该字符串即上报的 `NCLS1/NCLS2`，是判定 5G 接入的直接依据之一。

### 2.5 反射与属性工具

| 工具 | 实现 |
|---|---|
| `f0.o.a(String)` | `Class.forName` |
| `f0.o.b(Class,String,Class[])` | `Class.getMethod`（吞异常返回 null） |
| `f0.o.c(Method,Object,Object[])` | `Method.invoke`（吞异常返回 null） |
| `f0.l.i(String)` | 反射 `android.os.SystemProperties.get(key)`（`f0/l.smali:175-223`，catch → null） |
| `f0.l.j(String,String)` | 反射 `SystemProperties.get(key, def)`（`:225-301`，catch → def） |
| `f0.l.h(ctx,String,String)` | `SharedPreferences.getString`（**CellID/LAC 的缓存读**） |
| `e0.d2.a(slot)` | 反射 `SubscriptionManager.getSubId(slot)` → `int[]` |
| `e0.d2.b(tm,name,subId)` | 反射 `tm.<name>(subId)` |
| `e0.d2.c(tm,name1,name2,subId)` | 先 `name1(subId)`，异常再 `name2(subId)`（如 `getNetworkOperator` → `getNetworkOperatorForSubscription`） |
| `e0.i0.a(SubscriptionManager,int)` | 供 `e0/t1` 用，取 subId 数组 |
| `e0.g` / `e0.h` | API 桥：`ImsManager.getImsMmTelManager(int)` / `ImsMmTelManager.isAdvancedCallingSettingEnabled()`（非本报告范围，仅在 VoLTE 路径引用） |

---

## 3. 逐类分析（按任务清单顺序）

> 说明：`e0/c2` 只会调用采集器的 `b(Context,d0/g)`。“Base 调用”一栏列出该类内部对 `Le0/r;->x(...)` 的调用（说明它复用了基类哪段逻辑）。字段写入数量以 §1.1 为准。

### 3.1 `e0/m` — 合成桥：`NetworkRegistrationInfo.getDomain()`

- **本质**：R8/ProGuard 为 Java 8 方法引用（`NetworkRegistrationInfo::getDomain`）生成的 `bridge synthetic` 直接方法桥，`super=Ljava/lang/Object;`，无字段。
- **smali**（全文 15 行）：
  ```
  .method public static bridge synthetic a(Landroid/telephony/NetworkRegistrationInfo;)I
      invoke-virtual {p0}, Landroid/telephony/NetworkRegistrationInfo;->getDomain()I
  ```
- **唯一调用者**：`e0/r.smali:2692`（基类 `r(Context,int,d0/g)` 的 CellIdentity 解析路径）。
- **数值语义（关键）**：`getDomain()` 返回 `NetworkRegistrationInfo.DOMAIN_*`：`0=DOMAIN_UNKNOWN`、`1=DOMAIN_CS`（电路域）、`2=DOMAIN_PS`（分组域）、`3=DOMAIN_CS_PS`。应用据此判断该注册信息是否包含 PS（数据）域，从而决定是否从这条 `NetworkRegistrationInfo` 取 `CellIdentity` 作基站信息。
- **隐私**：无直接输出；是基站采集链的一环。

### 3.2 `e0/m0` — JingLing（京灵 / JingPad）采集器 `DeviceInfoReaderJingLing`

- **方法结构**：`t(TM,d0/g)@15`（主读取）、`u(Context)Z@173`（开机向导完成判定，被 `b()` 在 `:762` 调用）、`w(Context,d0/g)@224`（第二读法）、`b(ctx,g)@759`（入口）、`v(Context,int)String@980`（**每卡 VoLTE 取值器**，被 `:500`、`:747` 调用，结果分别写入 `o`/`z`）。
- **使用 API / 隐藏面**
  - `Settings$Global.getInt(cr,"device_provisioned",0)` + 一个 `Settings$Secure` 项 + `checkUserPermission:` 日志（`:180-213`）。
  - `Class.forName("android.telephony.TelephonyManager")` 与 **`android.telephony.TelephonyManagerEx`**（`:233-249`），日志 tag `phone_ex`。
  - 反射方法：`getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getNetworkOperator`、`getNetworkType`、`getVoiceNetworkType`、**`getCellLocationForPhone`**（`:273-739`）。
  - `SubscriptionManager.getDefaultDataSubscriptionId()` + 反射 `getPhoneId(subId)`（`:796-867`）→ 数据卡槽位。
- **写入键**（25 个，详见 §1.1）：`m,n`（卡1 小区）、`g,i,h,j,k,l`（卡1 四要素 + MNC + 网络类型）、`o`（VoLTE1）、`r,t,s,u,v,w,z`（卡2 全量）、`b,a,e,f,d,c`（机型/版本）、`F,B,G`（设备注册块）。
- **Base 调用**：`r.h@295`(getSubId)、`r.g@494`(网络类型→NCLS)、**`r.l@948`**（整段 SDK≥30 补采，含 VoLTE 读写与小区）。
- **触发/开关**：`c2.h()` 中厂商串 `JingLing`（`e0/c2.smali:1222`）；`b()` 内按 `Build.MODEL` 白名单（`JingPad C1`）选属性；未命中打 `DeviceInfoReaderJingLing unknow model:` 并走默认属性。
- **降级/副作用**：全部反射包 `try/catch`，失败留 `null`；`r.l()` 触发 VoLTE 读取（含“关闭则开启”的副作用，见 §2.2）。
- **隐私**：**P4**。

### 3.3 `e0/m1` — realme 采集器 `DeviceInfoReaderRealme`（含 DRAM 解析）

- **方法结构**：`t(ctx)Z@15`（开机向导）、`static v(ctx)J@66` / `static w(ctx)JSONObject@87`（内存信息）、`x(ctx,g)@170`（主读取）、`static y(ctx)J@747` / `static z()J@975`（DRAM 解析）、`b(ctx,g)@1091`、`u(ctx,int)String@1490`（**每卡 VoLTE 取值器**，被 `:466`、`:711` 调用 → 写 `o`/`z`）。
- **使用 API / 隐藏面**
  - **`android.telephony.ColorOSTelephonyManager`**（`:193`，OPPO/realme 私有类）+ `getDefault()`（`:217-229`）。
  - `getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getNetworkOperator`、`getNetworkType`、`getVoiceNetworkType`、`getCellLocation`（`:239-735`）。
  - **`isOemEnhanced4gLteModeSettingEnabledByUser`**（`:1541`）→ `u()`。
  - 属性：`ro.boot.dram_info`（`:978`，日志 `dramInfo: ` / `parsed value : ` / `No dram_info property`）、`ro.build.hardware.version`（`:1258`）、`ro.build.display.id`（`:1262`）。
- **写入键**（23 个）：卡1 `g,i,h,j,k,l` + `o`；卡2 `r,t,s,u,v,w` + `z`；`b,a,e,f`、`d,c`、`F,B,G`。
- **Base 调用**：`h@257`、`g@460`、**`l@1437`**。
- **触发/开关**：厂商 `realme`（`e0/c2.smali:1100`）；型号白名单 `RMX2201/RMX3043/RMX3093/RMX3122/RMX3125/RMX3143/RMX3300/RMX3334/RMX3362/RMX3461/RMX3611/RMX3708/RMX3781`。
- **降级/副作用**：`ro.boot.dram_info` 缺失 → 日志并返回默认；`l()` 触发 VoLTE 副作用。
- **隐私**：**P4**（+ RAM 容量画像）。

### 3.4 `e0/n` — 合成桥：`NetworkRegistrationInfo.getTransportType()`

- **smali**：`invoke-virtual {p0}, Landroid/telephony/NetworkRegistrationInfo;->getTransportType()I`。
- **调用者**：`e0/r.smali:2700`。
- **数值语义**：`AccessNetworkType` —— `0=UNKNOWN`、`1=GERAN`(2G)、`2=UTRAN`(3G)、`3=EUTRAN`(4G LTE)、`4=CDMA2000`、`5=IWLAN`、`6=NR`(5G, API 29+)。基类据此判断“这条注册信息是不是 5G NR 无线接入”，是 **5G 判定链**的一环。
- **隐私**：无直接输出。

### 3.5 `e0/n0` — kingsun 采集器 `DeviceInfoReaderKINGSUN`（三套读法）

- **方法结构**：`u(ctx,g)@15`（卡1 读法）、`v(ctx,g)@584`（卡2 读法，与 `u` 几乎逐行相同）、`a(ctx)Z@1155`（**桩**）、`b(ctx,g)@1169`、`t(ctx,g)@1375`（**第三套**读法，`public`）。
- **使用 API / 隐藏面**
  - `Class.forName("android.telephony.TelephonyManager")` + `getDeviceId`/`getSubscriberId`/`getSimSerialNumber`/`getLine1NumberForSubscriber`/`getNetworkOperatorForSubscription`/`getNetworkType`/`getVoiceNetworkType`。
  - **`getPhoneCid` / `getPhoneLac`**（`:104,269,295,673,838,1606`）—— 厂商私有“按 phone 取 CID/LAC”，替代 `getCellLocation()`。
  - `android.telephony.MSimTelephonyManager.getDefault()`（`:1382-1406`）+ `getSimOperator`。
- **写入键**（22 个）：`g,i,h,j,k,l`、`m,n`（卡1 小区）、`r,t,s,u,v,w`、`x,y`（卡2 小区）、`b,a,e,f,d,c`。
- **Base 调用**：`e@40`（`getSubId` 反射）、`g@259`。
- **触发/开关**：厂商 `kingsun`（`e0/c2.smali:676`）；型号 `KINGSUN-F2/F4/F7/F8/F9`。
- **降级/副作用**：`u()`/`v()` 各自独立 try/catch；`getPhoneCid/getPhoneLac` 不存在时静默 null。
- **附注（缺陷）**：`t(ctx,g)` 又完整重采一遍，且 `iput g.x` 出现两次（`e0/n0.smali:552` 与 `1121`），后者可能覆盖前者 —— 复制粘贴遗留。
- **隐私**：**P4**。

### 3.6 `e0/n1` — Shown（360OS 定制）采集器 `DeviceInfoReaderShown`

- **方法结构**：`<init>@15`（构造时读 `ro.sf.lcd_density`）、`t(int)String@45`（被 `<init>` 在 `:36` 调用）、`u(String,String)String@69`（**通用反射工具**：`Class.forName(clazz)` + 方法名 + 参数，`:77`）、`v(int)Z@130`（`isValidSubscriptionId`）、`w(ctx,g)@188`、`x(ctx,g)@636`、`y(ctx,g)@1084`、`a(ctx)Z@1442`（桩）、`b(ctx,g)@1456`。
- **使用 API / 隐藏面**
  - `android.os.SystemProperties.get` 系列：**`gsm.mtk.imei1` / `gsm.mtk.imei2`**（MTK 双 IMEI，`:1098,1279`）、`ro.system.OSVersion`（`:1559`）、`ro.internal.version`（`:1759`）、`ro.sf.lcd_density`（`:20`）、`ro.yunos.version`/`ro.yunos.systemname`、`ro.build.version.release`、`ro.hardware.version`。
  - `getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getLine1NumberForSubscriber`、`getNetworkOperatorForSubscription`、`getNetworkType`、`getVoiceNetworkType`、**`getCellLocationForPhone`**。
- **写入键**（22 个）：`g,i,h,j,k,l`、`r,t,s,u,v,w`、`m,n,x,y`、`b,a,e,f,d,c`。
- **Base 调用**：`e@217`、`g@407`、`f@1233`、`p@1727`（最后回落到基类 `p()` 全量读）。
- **触发/开关**：厂商 `Shown`（`e0/c2.smali:616`）；型号 `SHOWN P1/P1+/P2/P5/P6/P7/P9`、`SHOWN_P1/_P6/_P7`；OS 标识串 `360OS`。
- **隐私**：**P4**；额外暴露“360OS/YunOS 定制系统”与屏幕密度。

### 3.7 `e0/o` — 合成桥：`NetworkRegistrationInfo.getCellIdentity()`

- **smali**：`invoke-virtual {p0}, Landroid/telephony/NetworkRegistrationInfo;->getCellIdentity()Landroid/telephony/CellIdentity;`
- **调用者**：`e0/r.smali:2716`、`:2728`（两处，对应小区1 / 小区2 路径）。
- **语义**：返回基类 `CellIdentity`，再 `instance-of` 分派到 `CellIdentityGsm/Lte/Nr/Tdscdma/Cdma` 取 `Cid/Lac/Tac/Nci`。这是 **API 29+ 基站采集主通道**（替代 `getCellLocation()`）。
- **隐私**：无直接输出，但决定 CellID/LAC 的来源。

### 3.8 `e0/o0` — KONKA 采集器 `DeviceInfoReaderKONKA`

- **方法结构**：`t(ctx)Z@15`（`device_provisioned` + `Settings$Secure` + `checkUserPermission:`）、`u()String@105`、`v(int)I@117`（`getSubId`）、`w(int)Z@175`（`isValidSubscriptionId`）、`x(ctx,g)@233`（主读取）、`b(ctx,g)@717`。
- **使用 API / 隐藏面**
  - `TelephonyManagerEx`（`:256`）+ **`getCellLocationForPhone`**（`:480,693`）。
  - `getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getMsisdn`、**`getNetworkOperatorForPhone`**、`getVoiceNetworkType`。
  - 属性 `ro.boot.hardware.revision`（`:108`）、`operator_service_improve_plan`（运营商配置数据库键）。
  - `SubscriptionManager.getDefaultDataSubscriptionId()` + `getPhoneId`（`:754-825`）。
- **写入键**（21 个）：`g,i,h,j,k,l`、`r,t,s,u,v,w`、`b,a,e,f`、`F,B,G`、`c,d`（无 `m/n/x/y`，无 `o/p/z/A`）。
- **Base 调用**：`g@470`。
- **触发/开关**：厂商 `KONKA`（`e0/c2.smali:691`；`:1113` 有一条重复 `KONKA→o0`）；型号 `KONKA X10`。
- **隐私**：**P4**（缺小区 / 5G，但双卡四要素齐全）。

### 3.9 `e0/o1` — OYSIN / SINTAVE 采集器 `DeviceInfoReaderOYSIN`

- **方法结构**：`t(int)Z@15`（`isValidSubscriptionId`）、`u(ctx,g)@73`（主读取）、`a(ctx)Z@519`（桩）、`b(ctx,g)@533`。
- **使用 API / 隐藏面**：`getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getLine1NumberForSubscriber`、`getNetworkOperatorForSubscription`、`getVoiceNetworkType`、**`getCellLocationForPhone`**（`:136-511`）。
- **写入键**（18 个）：`g,i,h,j,k,l`、`r,t,s,u,v,w`、`b,a,e,f,d,c`；`c` 取 `ro.product.hardware`（`:607`）、`e/f` 取 `ro.yunos.name`/`ro.yunos.version`（`:617-633`）。
- **Base 调用**：`e@96`、`g@288`。
- **触发/开关**：厂商 `OYSIN`（`e0/c2.smali:496`）；型号 `SINTAVE S1000`、`SINTAVE S1000 PLUS`。
- **隐私**：**P4**。

### 3.10 `e0/p` — 合成桥：`CellIdentityTdscdma.getCid()`

- **smali**：`invoke-virtual {p0}, Landroid/telephony/CellIdentityTdscdma;->getCid()I`（API 29 才有的 TD-SCDMA 身份类）。
- **调用者**：`e0/r.smali:2787`。
- **语义**：TD-SCDMA 小区号（CID）。配合 `e0/q` 的 `getLac()` 拼成 “CID:LAC” 上报（基类用 `"%d:%d"` 格式化，`e0/r.smali:2830`）。
- **隐私**：无直接输出（参与 P3）。

### 3.11 `e0/p0` — K-Touch 采集器 `DeviceInfoReaderKTOUCH`

- **方法结构**：`t(int)Z@15`（`isValidSubscriptionId`）、`u(ctx,g)@73`（主读取，含 **PhoneFactory**）、`v(ctx,g)@552`（第二读法）、`a(ctx)Z@998`（桩）、`b(ctx,g)@1012`。
- **使用 API / 隐藏面**
  - `getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1NumberForSubscriber`、`getSimOperator`、`getNetworkType`。
  - **`getPhoneCount()` + `com.android.internal.telephony.PhoneFactory#getPhone(i)`**（`:280-339`）—— 直接拿内部 `Phone` 对象逐卡读取。
  - `getDeviceId`、`getCellLocationForPhone`、`getNetworkOperatorForSubscription`、`getVoiceNetworkType`。
  - 属性 `ro.product.hardware`（`:1076`，含 `SPREADTRUM` 判定 `:1078`）、`ro.hw_version`（`:1123`）、`ro.yunos.version`/`ro.yunos.systemname`。
- **写入键**（18 个）：`g,i,h,j,k,l`、`r,t,s,u,v,w`、`b,a,e,f,d,c`。
- **Base 调用**：`e@98`、`g@274`。
- **触发/开关**：厂商 `K-Touch`（`e0/c2.smali:601`）；型号 `K-Touch C1`、`K-Touch M3s Plus`。
- **隐私**：**P4**。

### 3.12 `e0/p1` — SOP 采集器 `DeviceInfoReaderKONKA`（tag 混用）+ 内存 / 存储解析

- **方法结构**：`t()String@15`（**/proc/meminfo 解析**）、`u(ctx)String@193`（**StatFs 存储**）、`v(int)Z@257`（`isValidSubscriptionId`）、`w(ctx,g)@315`（主读取）、`a(ctx)Z@809`（桩）、`b(ctx,g)@823`、`u(ctx,int)@704`（**每卡 VoLTE 取值器**，被 `:906` 调用）。
- **使用 API / 隐藏面**
  - `new FileReader("/proc/meminfo")`（`:18`）→ 解析 `MemTotal:` 等 → 归一化为 `256M/512M/768M/1G/2G/3G/4G/8G/16G/32G/64G`。
  - `new StatFs(...)` → `getBlockSize()*getBlockCount()`（`:200-214`）→ ROM 容量。
  - `getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getMsisdn`、`getNetworkOperatorForSubscription`、`getNetworkType`、`getVoiceNetworkType`。
  - 属性 `persist.sys.hardware.version`（`:868`）、`persist.sys.update.version`（`:858`）、`ro.yunos.version`/`ro.yunos.name`。
- **写入键**（22 个）：`g,i,h,j,k,l`、`m,n`（来自 prefs `CellID0`/`LAC0`）、`r,t,s,u,v,w`、`x,y`、`b,a,e,f,d,c`。
- **Base 调用**：`e@334`、`g@553`。
- **触发/开关**：厂商 `SOP`（`e0/c2.smali:781`）；型号 `SOP-W1`、`SOP-W2`。
- **隐私**：**P4**；额外收集 **RAM/ROM 容量**（设备指纹增强）。

### 3.13 `e0/q` — 合成桥：`CellIdentityTdscdma.getLac()`

- **smali**：`invoke-virtual {p0}, Landroid/telephony/CellIdentityTdscdma;->getLac()I`。
- **调用者**：`e0/r.smali:2795`。
- **语义**：TD-SCDMA 位置区号（LAC）。
- **隐私**：无直接输出（参与 P3）。

### 3.14 `e0/q0` — lephone 采集器 `DeviceInfoReaderLEPHONE`（**五套读法**）

- **方法结构**：`t(TM,g,int)@15`（**`getAllCellInfo` 解析**）、`u(int)Z@185`（`isValidSubscriptionId`）、`v(ctx,g)@249`、`w(ctx,g)@700`、`x(ctx,g)@1151`、`y(ctx,g)@1665`、`z(ctx,g)@2179`、`a(ctx)Z@2695`（桩）、`b(ctx,g)@2709`。
- **使用 API / 隐藏面**
  - **`getAllCellInfo()`**（`:32`）→ `List<CellInfo>`，遍历取服务小区 → `x/y`。
  - `getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1NumberForSubscriber`、`getNetworkOperatorForSubscription`、`getNetworkType`、`getVoiceNetworkType`、`getMsisdn`、`getCellLocationForPhone`。
  - 属性：`ro.sw_version`（`:2840`）、`ro.hw_version`（`:2842`）、**`persist.sys.hardware_version`**（`:2969`）、`ro.hcthardware.version.number`（`:3046`）、`ro.custom.version.release`（`:3054`）、`ro.build.display.id`（`:3084`）、`ro.yunos.version`、`ro.product.hardware`。
- **写入键**（22 个）：`m,x,n,y`（小区先写）、`g,i,h,j,k,l`、`r,t,s,u,v,w`、`b,a,e,f,c,d`。
- **Base 调用**：`e@274`、`g@483`、`p@3008`、`q@3100`（最后回落到基类双路全量读）。
- **触发/开关**：厂商 `lephone`（`e0/c2.smali:706`）；型号 `lephone A7/A9/A10/A12/A12note/F5W/F5WPro/V8/W5/W6/W6Pro/W7/W7+/W9/W9+`。
- **隐私**：**P4**。

### 3.15 `e0/q1` — 三星采集器 `DeviceInfoReaderSamSung`（5G/VoLTE 最专业）

- **字段**：`final b:I@7`、`final c:I@9`（构造期常量，疑似“槽位 1/2”或 API 常量；**具体语义未知**）。构造 `<init>@13`。
- **方法结构**：`t(ctx)Z@29`（开机向导）、`static v(ctx)J@80` / `static w(ctx)JSONObject@101`（内存）、`x()Z@184`（**isSupport5GConcept**）、`static y(ctx)J@275` / `static z()J@503`（DRAM）、`A(ctx,g)@619`、`B(ctx,g)@1464`、`b(ctx,g)@2468`、`u(ctx,int)String@2647`（**每卡 VoLTE 取值器**，6 处调用：`:1021/1222/1429/1906/2187/2436` → 写 `o`/`z`）。
- **使用 API / 隐藏面**
  - **`TelephonyManager.getTelephonyProperty(...)`**（`:665-707`）读 `ril.hw_ver`、`ro.build.PDA`；`B()` 里再读 `ril.sw_ver`（`:1524`）。
  - **`createForSubscriptionId(subId)`**（`:1614-1698`）—— 拿到绑定 subId 的 `TelephonyManager` 后再读 `getSubscriberId/getSimSerialNumber/getMsisdn/getSimOperator`。
  - **`getCellLocationBySubId`**（`:833,1602`）。
  - `getImei`、`getNetworkType`、`getSimOperatorNumeric`、`getLine1Number`。
  - **`Settings$System.getInt(cr,"voicecall_type"/"voicecall_type2",-1)`**（`:2671`）→ 三星 VoLTE 开关。
  - 属性：`ro.telephony.default_network`（`:192`，`Integer.parseInt` 后判 `>=30` → 支持 5G 概念）、`ro.product.first_api_level`（`:1596`）、`ro.boot.dram_info`（`:506`）。
- **写入键**（25 个）：`c,d`（先写版本）、`B`、`g,i,h,j,k,l`、`o`、`r,t,s,u,v,w`、`z`、`p`、`A`、`b,a,e,f,F,G`；小区经基类 `r()` 落到 `d0/g.m/n/x/y`。
- **Base 调用**：`h@877`、`g@1015`、**`j@1768`**（5G：`preferred_network_mode{slot}` ∈ [23,33]）、**`r@1842`**（cell 解析）。
- **触发/开关**：厂商 `samsung`（`e0/c2.smali:1085`）；型号 `SM-G9750/SM-N9700/SM-N9760/SM-A5070/SM-A9080/SM-T735C/SM-X205C`。
- **降级/副作用**：`isSupport5GConcept` 捕获 `NumberFormatException`（`:251`）；`p` 被连续 `iput` 两次（`:1774`、`:1779`），后者覆盖。
- **隐私**：**P4**；额外暴露 **PDA / ril 硬件版本、首版 API level、三星 voicecall_type 设置**。

### 3.16 `e0/r0` — LETV / 乐视采集器 `DeviceInfoReaderLETV`（**五套读法**）

- **方法结构**：`t(ctx,g)@15`（主读取）、`a(ctx)Z@522`（桩）、`b(ctx,g)@536`、`q(ctx,g)@882`、`u(ctx,g)@1246`（MTK `TelephonyManagerEx`）、`v(ctx,g)@1654`（三套并行读法）。
- **使用 API / 隐藏面**：`getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1NumberForSubscriber`、`getSimOperator`、`getNetworkType`、`getVoiceNetworkType`、**`getCellLocationFromSlotId`**（`:106,270,500,920,1692`）；**`com.mediatek.telephony.TelephonyManagerEx.getDefault()`**（`:1267-1287`）+ 其 `getCellLocation`（`:1314,1457,1630`）。
- **属性**：**`ro.product.letv_model`**（`:569`）、`ro.build.display.id`（`:571`）、`ro.config.le_hardware_version`（`:610`）、`ro.product.hardware.version`（`:682`）、`ro.config.hardware_version`（`:727`）、`ro.hardware.version`（`:818`）。
- **写入键**（18 个）：`g,i,h,j,k,l`、`r,t,s,u,v,w`、`b,a,e,f,d,c`。
- **Base 调用**：`e@44`、`g@263`、**`p@597`**、`f@1033`。
- **触发/开关**：厂商 `LETV`（`e0/c2.smali:421`）；型号 `Le X525/X620/X820`、`Letv X500`、`LEX555/622/626/652/659`。
- **隐私**：**P4**。

### 3.17 `e0/r1` — Spreadtrum / 展讯采集器

- **方法结构**：`t(int)I@15`（反射 `getSubId`）、`u(int)Z@73`（`isValidSubscriptionId`）、`v(ctx,g)@131`（主读取，**`phone_ex` 通道**）、`a(ctx)Z@607`（桩）、`b(ctx,g)@621`。
- **使用 API / 隐藏面**：`getSystemService("phone_ex")`（`:154`）强转 `TelephonyManagerEx`；`getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、**`getMsisdn`**、`getNetworkOperatorForPhone`、`getVoiceNetworkType`、`getCellLocationForPhone`（`:190-599`）。
- **写入键**（18 个）：`g,i,h,j,k,l`、`r,t,s,u,v,w`、`b,a,e,f,d,c`；`c` = `ro.product.hardware`（`:660`）。
- **Base 调用**：`g@360`。
- **触发/开关**：厂商 `Spreadtrum`（`e0/c2.smali:826`）；型号 `SP9830A`。
- **降级**：整段 `try/catch`；`b()` 末尾 `DeviceRegister` / `DeviceInfoReaderSpreadtrum module unknow!!!`。
- **隐私**：**P4**。

### 3.18 `e0/s` — Bee 采集器 `DeviceInfoReaderBee`（`getAllCellInfo` 解析）

- **方法结构**：`<init>@15`（读 `ro.sf.lcd_density`）、`t(int)String@45`、`u(TM,g,int)@69`（**CellInfo 列表解析**）、`v(String,String)String@239`（通用反射）、`w(int)Z@300`（`isValidSubscriptionId`）、`x(ctx,g)@358`、`y(ctx,g)@809`、`b(ctx,g)@1325`。
- **使用 API / 隐藏面**：**`getAllCellInfo()`**（`:86`）→ 遍历 `List<CellInfo>`，命中服务小区后写 `x`/`y`（`:214,231`）；`getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getLine1NumberForSubscriber`、`getNetworkOperatorForSubscription`、`getNetworkType`、`getVoiceNetworkType`、`getMsisdn`、`getCellLocationForPhone`。
- **属性**：`ro.sw_version`（`:1364`）、`ro.hw_version`（`:1372`）、`ro.build.display.id`、`ro.product.hardware`、`ro.yunos.version`、`ro.yunos.name`。
- **写入键**（22 个）：`m,x,n,y`、`g,i,h,j,k,l`、`r,t,s,u,v,w`、`b,a,e,f,d,c`。
- **Base 调用**：`e@383`、`g@592`。
- **触发/开关**：厂商 `Bee`（`e0/c2.smali:890`）；型号 `bee E7S`、`bee X9`。
- **隐私**：**P4**。

### 3.19 `e0/s0` — Lenovo 采集器 `DeviceInfoReaderLenovo`（**只采 SIM1**）

- **方法结构**：`A(ctx,g)@15`（主读取）、`t(ctx)Z@265`（`Settings$Global.getInt` + `checkUserPermission:`）、`v()@309` / `w()@321` / `x()@333` / `y()@345`（四个属性 getter）、`z(ctx,g)@357`（**公开 API 回退**：直接用 `TelephonyManager.getDeviceId/getSubscriberId/getSimSerialNumber/getLine1Number/getNetworkOperator/getNetworkType/getVoiceNetworkType/getCellLocation`）、`b(ctx,g)@428`、`u(ctx,int)@704`（**每卡 VoLTE 取值器**，被 `:255` 调用 → 写 `o@259`）。
- **使用 API / 隐藏面**：`isValidSubscriptionId`（`:73`）、`getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getSimOperator`、`getNetworkType`、`getVoiceNetworkType`、`getCellLocation`；`SubscriptionManager.getDefaultDataSubscriptionId()` + `getPhoneId`（`:465-536`）。
- **写入键**（16 个，**无卡 2 SIM 字段**）：`g@62 i@98 h@128 j@150 k@172 l@231 o@259`、`b@439 a@449 e@453 f@457`、`F@463 B@565 G@599`、`d@603 c@609`。
- **Base 调用**：`h@64`、`g@227`。
- **触发/开关**：厂商 `Lenovo`（`e0/c2.smali:1055`）；型号 `Lenovo TB-8505N`、`TB-X306NC_PRC`、`TB-X505N`、`TB-X605M`（**平板线**，故按单卡处理）；运营商键 `agree_unicom`。
- **隐私**：**P3+**（单卡 IMEI/IMSI/ICCID/MSISDN，无小区）。

### 3.20 `e0/s1` — TCL 采集器 `DeviceInfoReaderTCL`（**与 y / z1 同源**）

- **方法结构**：`t(ctx)Z@15`（开机向导）、`v(ctx,g)@66`（主读取）、`b(ctx,g)@656`、`u(ctx,int)@914`（**每卡 VoLTE 取值器**，被 `:362`、`:618` 调用 → 写 `o`/`z`）。
- **使用 API / 隐藏面**：**`ColorOSTelephonyManager.getDefault()`**（`:89-125`）；`getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getNetworkOperator`、`getNetworkType`、`getVoiceNetworkType`、`getCellLocation`；`isOemEnhanced4gLteModeSettingEnabledByUser`（`:965`）。
- **属性**：`ro.build.display.id`（`:829`）、`ro.board.hardware_version`（`:837`）、`ro.tcl.hardware.version`（`:870`）、`ro.mtk.build.version`（`:880`）。
- **写入键**（23 个）：`g@151 i@213 h@249 j@273 k@297 l@360 o@366`、`r@437 t@471 s@505 u@529 v@553 w@616 z@622`、`b@667 a@677 e@681 f@685`、`F@691 B@793 G@827`、`d@835 c@843`。
- **Base 调用**：`h@153`、`g@356`、**`l@849`**、`p@888`。
- **触发/开关**：厂商 `TCL`（`e0/c2.smali:736`）；型号 `TCL P561U`、`F810`。
- **隐私**：**P4**。

### 3.21 `e0/t` — BenWee 采集器 `DeviceInfoReaderBenWee`

- **方法结构**：`t(ctx,g)@15`（主读取，**`MSimTelephonyManager` 单通道**）、`a(ctx)Z@408`（桩）、`b(ctx,g)@422`。
- **使用 API / 隐藏面**：`Class.forName("android.telephony.MSimTelephonyManager")` + `getDefault()`（`:22-46`），随后在该实例上反射 `getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getNetworkOperator`、`getVoiceNetworkType`、`getCellLocation`（`:62-394`）—— **全部字段都从 MSim 实例取**。
- **写入键**（18 个）：`g,i,h,j,k,l`、`r,t,s,u,v,w`、`b,a,e,f,d,c`。
- **Base 调用**：`g@208`、`p@465`。
- **触发/开关**：厂商 `BenWee`（`e0/c2.smali:526`）；型号 `BenWee L5`、`BenWee V9`、`BenWee_L5`、`V6`。
- **隐私**：**P4**。

### 3.22 `e0/t0` — “Liantong”（联通定制）采集器 `DeviceInfoReaderLiantong`（**唯一覆写 `j()` 的类**）

- **方法结构**：`t(ctx,g)@15`（**权限门禁 + ColorOS 读法**）、`u(ctx)Z@360`（开机向导）、`w(ctx,g)@411`、`x(ctx,g)@999`、`y(ctx,g)@1441`、`b(ctx,g)@2124`、**`j(ctx,int)Z@2459`（覆写基类 5G 判定）**、`v(ctx,int)String@2547`（**每卡 VoLTE 取值器**，4 处调用 `:707/963/1829/2111` → 写 `o`/`z`）。
- **使用 API / 隐藏面**
  - 权限门禁：`android.permission.READ_PHONE_NUMBERS` / `READ_PHONE_STATE`（`:18-36`），日志 `readAndroidR no READ_PHONE_STATE PERMISSION_GRANTED`。
  - `ColorOSTelephonyManager.getDefault()`（`:434-470`）、`getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getNetworkOperator`、`getNetworkType`、`getVoiceNetworkType`、`getCellLocation`、`getNetworkOperatorForPhone`、`getSimOperatorNumericForPhone`。
  - **`Settings$Global.getInt(cr,"preferred_network_mode{slot}",-1)`**（`:2508`）—— `j()` 里判 5G。
  - `isOemEnhanced4gLteModeSettingEnabledByUser`（`:2598`）。
  - 属性：`ro.build.display.id`（`:2291`）、`ro.board.hardware_version`（`:2293`）。
- **写入键**（25 个）：`g@59`、`r@89`、`i@127 h@141 j@147 k@153`、`p@161`、`l@171`、`o@228`、`t@251 s@265 u@271 v@277`、`A@287`、`w@297`、`z@354`、`b@2135 a@2145 e@2149 f@2153`、`F@2159 B@2261 G@2342`、`d@2348 c@2354`。
- **Base 调用**：`h@97`、`r@173`、**`c@216`**、**`d@223`**（→ VoLTE 读取 + “关闭则开启”的副作用）、`g@701`。
- **触发/开关**：厂商 `Liantong`（`e0/c2.smali:1207`）；型号 `S10`、`VP001/VP003/VP004/VP005`。
- **隐私**：**P4**；额外暴露“联通定制机”渠道指纹。

### 3.23 `e0/t1` — TDTECH（鼎桥）采集器 `DeviceInfoReaderTDTECH`（**华为系 5G 面**）

- **字段**：`b TelephonyManager@7`、`c:I@9`、`d:I@11`、`e/f/g/h:String@13-19`（实例缓存）。
- **方法结构**：`t(ctx,g)@51`（**HwTelephonyManager 判 VoLTE/5G**）、`u(ctx)Z@468`（开机向导）、`static v(ctx)J@519` / `static w(ctx)JSONObject@540`（内存）、`x(ctx,g)@623`（主读取，被 `b()` 在 `:2146` 调用）、`static y(ctx)J@1497` / `static z()J@1725`（DRAM）、`b(ctx,g)@1841`。
- **使用 API / 隐藏面**
  - **`android.telephony.HwTelephonyManager.getDefault()`**（`:54-74, 686-702`）+ **`getServiceAbility`**（`:163,754,884`）+ **`isVolteAvailable`**（`:770`）。
  - `android.telephony.MSimTelephonyManager`（`:646`）+ `getSubscriberId`/`getSimSerialNumber`/`getLine1Number`/`getNetworkOperator`。
  - **`e0.i0.a(SubscriptionManager,int)`** 取 subId 数组（`:1892,1898`），日志 `subId0:` / `subId1:`。
  - 属性：`ro.product.hardwareversion`（`:2129`）、`ro.boot.dram_info`（`:1728`）。网络类型判定串：`NR` / `SA` / `NSA` / `LTE`。
- **写入键**（29 个）：`g@119 r@147`、`i@185 h@199 j@205 k@211 l@215`、`o@272`、`p@311`、`t@334 s@348 u@354 v@360 w@366`、`z@423`、`A@462`、`m@1066 n@1070 x@1459 y@1463`、`b@1852 a@1862 e@1866 f@1870`、`F@1876 B@2038 G@2115`、`d@2127 c@2135`。
- **Base 调用**：`h@155`、`r@217`、**`c@260`**、**`d@267`**、`g@929`。
- **触发/开关**：厂商 `TDTECH`（`e0/c2.smali:1237`）；型号 `TDT-MA01`、`RAI-AN01`、`AVA-PA00`。
- **隐私**：**P4**；额外暴露 **VoLTE 可用性、5G SA/NSA 能力、DRAM 容量**。

### 3.24 `e0/u` — BestSonny 采集器

- **方法结构**：`t(int)I@15`（`getSubId`）、`u(int)Z@73`（`isValidSubscriptionId`）、`w(ctx,g)@131`（主读取）、`a(ctx)Z@581`（桩）、`b(ctx,g)@595`、`v(ctx,g)@772`（第二读法，`getCellLocation` 直连）。
- **使用 API / 隐藏面**：`getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getMsisdn`、`getNetworkOperatorForPhone`、`getVoiceNetworkType`、`getCellLocationForPhone`、`getCellLocation`（`:136-1158`）。
- **写入键**（18 个）：`g,i,h,j,k,l`、`r,t,s,u,v,w`、`b,a,e,f,d,c`。
- **Base 调用**：`g@350`、`f@945`。
- **触发/开关**：厂商 `BestSonny`（`e0/c2.smali:766`）；型号 `Best sonny LT580/LT581/LT587/LT680/LT681`。
- **隐私**：**P4**。

### 3.25 `e0/u0` — 魅族采集器 `DeviceInfoReaderMEIZU`

- **方法结构**：`t(ctx)Z@15`（开机向导 + `Settings$Secure` + `checkUserPermission:`）、`u(int)I@105`（`getSubId` + **`getDefaultSubscriptionId`**）、`v(ctx,g)@174`（主读取，**`MzTelephonyManager`**）、`b(ctx,g)@671`。
- **使用 API / 隐藏面**：`Class.forName("android.telephony.MzTelephonyManager")`（`:197`）+ 该实例 `getCellLocation`（`:420,647`）；`getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getNetworkType`、`getVoiceNetworkType`、**`getSimOperatorNumericForPhone`**（`:275`）；`getDefaultDataSubscriptionId()` + `getPhoneId`（`:708-750`）。
- **写入键**（21 个）：`g,i,h,j,k,l`、`r,t,s,u,v,w`、`b,a,e,f`、`F@706 B@780 G@837`、`d,c`。
- **Base 调用**：`g@410`。
- **触发/开关**：厂商 `meizu`（`e0/c2.smali:511`）；型号 `M926Q`、`meizu 16Xs`；运营商键 `operator_service_improve_plan`。
- **隐私**：**P4**。

### 3.26 `e0/u1` — vivo 采集器 `DeviceInfoReaderVIVO`（**全项目最深**）

- **静态常量（明文）**：`c = "persist.vendor.radio.5g_mode_pref"`、`d = "persist.radio.vivo.fiveg"`（`<clinit>@17-40`）—— 两个 **5G 开关属性键**被提升为常量。
- **方法结构**：`A(String,String)Object@55`（通用反射）、`G()Z@123`（**isUnicomEntry**：读 `ro.vivo.op.entry`，含 `UNICOM` 判定）、`H(ctx,g)@172`（主读取）、`t(TM,g,int)@844`（小区读取）、`u(ctx)Z@1028`（**isUserPermissionOpen**）、`w(ctx,int)String@1118`（**5G 状态**）、`y()@1241`、`z()@1266`、`B()@1292`、`C(ctx,String,HashMap)@1325`、`D(ctx,String,String,int)@1580`、`E()@1853`、`F(ctx)Z@1914`、`a(ctx)Z@1988`（桩）、`b(ctx,g)@2002`、`v(ctx,int)String@2448`（VoLTE，被 `:485`、`:802` 调用）、`x(ctx,int)String@2529`（5G 开关，被 `:1181` 调用）。
- **使用 API / 隐藏面（重点）**
  - **`getITelephony()`**（`:1367,1635`）→ 构造 **`com.android.internal.telephony.VivoTelephonyApiParams`**（`:1383,1651`）→ **`vivoTelephonyApi(...)` / `getParamValues(...)`**（`:1497,1521,1759,1783`）—— 直接读取 vivo 私有 5G/NR 参数表。
  - 参数标签：`API_TAG_getNrCapability`（`:1927`）、`API_TAG_getNRSwitchState`（`:2532`）、`API_TAG_isVolteEnable`（`:2451`）；`isDualMode5G: `、`the 5G attribute:`、`get5GSwitchState: `、`get5GState: ----switchState:`。
  - 属性：`persist.radio.vivo.fiveg`、`persist.vendor.radio.5g_mode_pref`、`ro.vivo.op.entry`、`ro.vivo.op.entry.version`、`ro.vivo.product.version`、`ro.vivo.hardware_version`。
  - `getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getNetworkType`、`getVoiceNetworkType`、`getCellLocation`、**`getSimOperatorNumericForPhone`**、`getNetworkTypeForSubscription`。
  - `getDefaultDataSubscriptionId()` + `getPhoneId`（`:2062-2133`）；运营商键 `operator_service_improve_plan`；`dual_nr_support`。
- **写入键**（29 个）：`g@229 i@313 h@341 j@361 k@381`、`p@393`、`l@404`、`o@412`、`r@569 t@621 s@653 u@677 v@697`、`A@709`、`w@720`、`z@728`、`m@996 x@1001 n@1015 y@1020`、`b@2024 a@2034 e@2038 f@2042`、`c@2048 d@2054`、`F@2060 B@2162 G@2192`。
- **Base 调用**：`h@231`、`r@406`、**`d@408`**、`g@479`。
- **触发/开关**：厂商 `vivo`（`e0/c2.smali:841-849` 走 `equalsIgnoreCase` 分支）；型号 `V1901A…V1945A` 共 20+、`MP_0.1`。**硬门禁**：`b()` 内 `if (!isUserPermissionOpen) { log "isUserPermissionOpen==false, do not read!"; return; }`（`:2011-2015`）—— 41 个类里**唯一一处真正的“拒绝采集”分支**。
- **隐私**：**P4 +**（双卡全量、小区、VoLTE、5G SA/NSA/双模、联通定制身份）。

### 3.27 `e0/v` — CHANGHONG 采集器 `DeviceInfoReaderCHANGHONG`（**最短实现**）

- **方法结构**：`a(ctx)Z@17`（桩）、`b(ctx,g)@31`（**全部逻辑**）。
- **使用 API / 隐藏面**：**不申请任何系统服务**；只用 `f0.l.j(...)`（`SystemProperties.get(k,def)`）。
- **写入键（自身 6 个）**：`b Model@36`、`a Manuf@46`、`e OS@50`、`f OSVersion@54`、`c HWVersion@74`、`d SWVersion@82`。
  - `c = ro.denqin.hardware.version`（`:66`）、`d = ro.denqin.version`（`:76`）、随后 **`f = ro.yunos.version`（`:84`）覆盖 OSVersion、`e = ro.yunos.name`（`:92`）覆盖 OS**。
- **Base 调用**：**`p@102`** —— 即 `Le0/r.p(ctx,g)` 双卡反射全量读取，因此该类实际也产出 `g…w` 全部 SIM 字段（写在 `r.smali` 内）。
- **触发/开关**：厂商 `CHANGHONG`（`e0/c2.smali:860`）；型号 `Changhong W01`。
- **隐私**：**P4**（自身只读 6 个版本字段，但 `base.p()` 补齐双卡标识）。

### 3.28 `e0/v0` — 合成桥：`SubscriptionInfo.getMncString()`

- **smali**：`invoke-virtual {p0}, Landroid/telephony/SubscriptionInfo;->getMncString()Ljava/lang/String;`
- **调用者**：`e0/w0.smali:535`、`e0/w0.smali:701`（Motorola 采集器两条槽位分支）。
- **语义**：返回 **MNC 字符串形式**（API 29 起才有；旧版 `getMnc()` 是 int，前导 0 会丢）。用于把 `MNC1/MNC2` 写成 `"00"/"01"` 这类定长串。
- **隐私**：间接输出 `MNC1/MNC2`（运营商识别）。

### 3.29 `e0/v1` — WIKO / PTAC 采集器（与 `t1` 同源，**双 tag**）

- **与 `t1` 的差异**（规范化行集合 Jaccard 57.8%，差异集中）：
  1. 多一条**直连公开 API 回退**：直接 `TelephonyManager.getSubscriberId()/getSimSerialNumber()/getLine1Number()/getNetworkOperator()`，再经 `Le0/r;->f(tm,slot)` + `Le0/r;->g(netType,operator)` 归一。
  2. 反射时**变长参数数组**：`new-array v6, v12, [Ljava/lang/Class;`（`v12` 由字符串长度决定）—— `getMethod(name, Class[])` 传 N 个参数。
  3. 网络类型串扩展为 `NR`/`SA`/`NSA`/`LTE` 四态，映射 `"1"/"0"`。
  4. 保留 `HwTelephonyManager.getServiceAbility` + `isVolteAvailable`。
- **方法结构**：`t(ctx,g)`、`u(ctx)Z`、`static v/w`（内存）、`x(ctx,g)`（主读取，被 `b()` 在 `:2062` 调用）、`static y/z`（DRAM）、`b(ctx,g)@1815`。
- **写入键**（29 个，与 `t1` 完全同构）：`g@83 r@111 i@149 h@163 j@169 k@175 l@187 o@244 p@283 t@306 s@320 u@326 v@332 w@344 z@401 A@440 m@1040 n@1044 x@1433 y@1437 b@1826 a@1836 e@1840 f@1844 F@1850 B@1952 G@2031 d@2043 c@2051`。
- **Base 调用**：`h@119`、`f@177`、`g@183`、`r@189`、**`c@232`**、**`d@239`**。
- **触发/开关**：厂商 `WIKO`（`e0/c2.smali:1282`）与 `PTAC`（`:1252`，`FIO-BD00/Hebe-BD00/Hera-BD00/MGZ-BD00/ALH-BD00/MIZ-BD00/MRK-BD00` 一长串型号映射到 PTAC）；型号 `GAR-AN00`、`LGN-AN00`、`GAR-AN60`。
- **隐私**：**P4 +**（与 `t1` 同级）。

### 3.30 `e0/w` — ChuiZi（锤子 / 坚果）采集器 `DeviceInfoReaderChuiZi`

- **方法结构**：`t()String@15`（型号判定，含 `ro.product.model`）、`u()String@77`（`ro.smartisan.version`）、`v(ctx,g)@125`（主读取）、`b(ctx,g)@609`。
- **使用 API / 隐藏面**：`getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getNetworkOperator`、**`getNetworkOperatorForPhone`**、`getNetworkType`、`getVoiceNetworkType`、`getCellLocation`、**`getSimOperatorNumericForPhone`**；`d0/g.f()/g()` 直接喂 `CellLocation`（`:379,601`）。
- **写入键**（18 个）：`g,i,h,j,k,l`、`r,t,s,u,v,w`、`b,a,e,f,d,c`。
- **Base 调用**：`h@184`、`g@355`。
- **触发/开关**：厂商 `ChuiZi`（`e0/c2.smali:905`）；型号 `OD101/OD103/OD105`、`MMR500030/MMR500032`；系统标识 `ro.smartisan.version`。
- **隐私**：**P4**。

### 3.31 `e0/w0` — Motorola / ZUI 采集器 `DeviceInfoReaderMOTOl`

- **方法结构**：`t(ctx)Z@15`（开机向导）、`static u(ctx)J@66` / `static v(ctx)JSONObject@87`（内存）、`static w(ctx)J@170`（RAM 数值）、`x(ctx,g)@398`（主读取，被 `b()` 在 `:1101` 调用）、`static y()J@806`（**`ro.boot.dram_info` 解析**）、`b(ctx,g)@922`。
- **使用 API / 隐藏面**
  - 权限门禁 `READ_PHONE_NUMBERS` / `READ_PHONE_STATE`（`:407-425`），日志 `readAndroidR no READ_PHONE_STATE PERMISSION_GRANTED`。
  - **`SubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(slot)`**（`:503,669`）→ `SubscriptionInfo`，再用 `e0/v0` 桥取 `getMncString()` 并写 `o/p/z`。
  - `getDefaultDataSubscriptionId()` + `getPhoneId`（`:959-1030`）。
  - 属性：`ro.zui.software.display`（`:1085`）、`ro.product.model`（`:1093`）、`ro.boot.dram_info`（`:809`）。
- **写入键**（25 个）：`g@454 r@460 i@511 h@525 j@531 k@539 p@548 l@558 o@620 t@677 s@691 u@697 v@705 A@714 w@724 z@800 b@933 a@943 e@947 f@951 F@957 B@1059 G@1083 d@1091 c@1099`。
- **Base 调用**：`h@463`、**`j@542`**（5G）、**`r@566`**（cell）、**`c@616`**、**`d@631`**（VoLTE）。
- **触发/开关**：厂商 `motorola`（`e0/c2.smali:1010`）；型号白名单含 `XT2335-3` 等；ZUI 系统识别。
- **隐私**：**P4 +**（VoLTE/5G + RAM）。

### 3.32 `e0/w1` — xiaodu（小度）采集器 `DeviceInfoReaderXiaodu`（**存储画像最全**）

- **方法结构**：`A()String@15`（`ro.build.version.software_version_display`）、`static B(ctx)JSONObject@40`（存储汇总 JSON）、`static t(J)I@108`（`analyseTotalStorage(), total: `）、`u(ctx)Z@152`（`persist.sys.cucc.dm` + `device_provisioned` + `checkUserPermission:`）、`static v()String@209`（**`Environment.getDataDirectory()` + `StatFs.getBlockSizeLong()*getAvailableBlocksLong()`**）、`static w(File)J@311`（`getBlockSize()*getBlockCount()`）、`x()String@339`（`ro.boot.hardware_version`，被 `:1042` 调用）、`static y(ctx)String@364`、`static z()String@424`（**root 分区 + data 分区容量**）、`C(ctx,g)@494`（主读取）、`b(ctx,g)@1019`。
- **使用 API / 隐藏面**：`getVoiceNetworkType`、`getNetworkType`、**`getNetworkOperatorForPhone`**、`getMsisdn`、`getSimSerialNumber`、`getSubscriberId`；属性 **`ro.boot.5GEnable1`（`:727`）与 `ro.boot.5GEnable2`（`:935`）** → 直接写 `p`/`z`/`o`；`persist.sys.cucc.dm`（`:155`，联通 DM 定制标记）；日志 `subId0:`/`，subId1:`/`, activeDataSubId：`。
- **写入键**（25 个）：`G@524 g@532 i@580 h@612 j@636 k@660 l@723 p@733 o@739 r@752 t@788 s@820 u@844 v@868 w@931 A@941 z@947 B@999 b@1030 a@1040 c@1046 d@1052 e@1056 f@1060 F@1066`。
- **Base 调用**：`h@534`、`g@719`、`r@725`、**`d@735`**。
- **触发/开关**：厂商 `xiaodu`（`e0/c2.smali:1297`）；型号 `G`、`MP_0.1`。
- **隐私**：**P4 +**（双卡全量 + VoLTE/5G + **总容量/可用容量/系统分区/数据分区**）。

### 3.33 `e0/x` — 合成桥：`SubscriptionManager.getSlotIndex(subId)`

- **smali**：`invoke-static {p0}, Landroid/telephony/SubscriptionManager;->getSlotIndex(I)I`
- **调用者（26 处，全项目最常用桥之一）**：`e0/a1.smali:721`、`e0/b2.smali:1205`、`e0/c0.smali:426`、`e0/c1.smali:1165`、`e0/e0.smali:413`、`e0/f1.smali:577`、`e0/g0.smali:1751`、`e0/j0.smali:1897` 等。
- **语义**：`subId` → **物理卡槽下标**（0/1）。是所有 `xxx1/xxx2` 双卡字段归属的依据。
- **隐私**：间接决定双卡字段与卡槽的对应关系。

### 3.34 `e0/x0` — mfox 采集器 `DeviceInfoReaderMfox`（**MTK 属性双 IMEI**）

- **方法结构**：`a(ctx)Z@17`（桩）、`b(ctx,g)@31`（版本字段）、`t(ctx,g)@117`（主读取）。
- **使用 API / 隐藏面**
  - **`SystemProperties.get("gsm.mtk.imei1")` / `("gsm.mtk.imei2")`**（`:131,312`）—— MTK 平台专有双 IMEI 属性，绕开 `getDeviceId`。
  - `Le0/d2.b/c(TM,name,subId)`：`getSubscriberId`、`getSimSerialNumber`、`getLine1NumberForSubscriber`、`getNetworkOperator`↔`getNetworkOperatorForSubscription`（`:155-416`）。
  - 属性 `ro.product.brand`（`:66`）。
  - `CellID0/LAC0`、`CellID1/LAC1` 从 **SharedPreferences** 读回（`:288-302, 455-467`）。
- **写入键**（22 个）：`b@36 a@46 e@50 f@54 c@74 d@78`、`g@137 i@178 h@209 j@230 k@251 l@276 m@294 n@302`、`r@318 t@343 s@374 u@395 v@416 w@441 x@459 y@467`。
- **Base 调用**：`f@266`（网络类型）、`g@272`（NCLS）。
- **触发/开关**：厂商 `mfox`（`e0/c2.smali:965`）；型号 `A-10`。
- **隐私**：**P4**。

### 3.35 `e0/x1` — 小辣椒采集器 `DeviceInfoReaderXiaolajiao`（**型号分支最碎**）

- **方法结构**：`t(int)I@15`、`u(int)Z@73`、`v(ctx,g)@131`、`w(ctx,g)@562`、`x(ctx,g)@1001`（被 `b()` 在 `:2602` 调用）、`y(ctx,g)@1506`、`z(ctx,g)@1954`、`a(ctx)Z@2404`（桩）、`b(ctx,g)@2418`、`p(ctx,g)@2882` / `q(ctx,g)@2912`。
- **`b()` 的关键差异（重要）**：`g.a`（Manuf）**不是** `Build.MANUFACTURER`，而是 `e0/c2.a.get(Build.MODEL)` 查表得到的厂商串（`e0/x1.smali:2425-2433`）；随后对 `Build.MODEL` 做 **20+ 次 `equals` 分支**（`20150204W`、`20150718`、`20150818`、`20160202W`、`20160212W`、`20160212W-V1.0`、`20160526Q`、`20160727Q`、`20160926Q`、`20161101Q`、`20170212`…），每支挑不同属性组合写 `c/d/f/e`；最后 `p()` 或 `q()`。
- **使用 API / 隐藏面**：`getImei`（带 `slot` 参数）、`Le0/r.e(slot)` 取 subId、`getNetworkOperatorForPhone`、`getSimOperator`、`getSubId`；属性 `ro.build.display.hardware.id`、`ro.build.display.hwid`、`ro.build.version.external`、`ro.product.yuxinhardwareversion`、`ro.product.yuxinosname`、`ro.product.yuxinsoftwareversion`、`ro.xh.display.version`、`ro.hardware.version`、`ro.build.display.id`。
- **写入键**（22 个）：`g@194 i@220 h@268 j@292 k@312 l@346 m@356 n@364`、`r@400 t@426 s@460 u@484 v@504 w@538`、`x@548 y@556`、`b@2423 a@2433 e@2437 f@2441 d@2492 c@2500`。
- **Base 调用**：`i@160`（`getSubId`）、`g@342`、`e@1030`、`p@2882`、`q@2912`。
- **触发/开关**：厂商 `Xiaolajiao`（`e0/c2.smali:995`）；型号 `GM-W`、`JD-PLUS` 及上述版本号串；**未在白名单的型号**会因 `c2.a.get()` 返回 null 使 `g.a` 为空（上报时被 `d0/g.e()` 填 `"Unknown"`）。
- **隐私**：**P4**；额外暴露“YunOS / 小辣椒定制固件版本号”这一强指纹。

### 3.36 `e0/y` — Coolpad 采集器 `DeviceInfoReaderCoolpad`（**与 s1/z1 同源**）

- **方法结构**：`t(ctx)Z@15`、`v(ctx,g)@66`、`b(ctx,g)@656`、`u(ctx,int)@914`（VoLTE 取值器，被 `:362`/`:618` 调用）。
- **与 `s1` 的差异**（Jaccard 97.3%，仅 3 处）：日志 tag `DeviceInfoReaderCoolpad`；型号白名单 `CP03`；属性只用 `ro.board.hardware_version`（不再试 `ro.tcl.*`/`ro.mtk.*`）。
- **额外缺陷**：`b()` 的“未知型号”分支日志写成 **`DeviceInfoReaderOPPO module unknow!!!`**（复制残留），并有一处 `goto :goto_1` 结构差异。
- **写入键**（23 个）：`g@151 i@213 h@249 j@273 k@297 l@360 o@366 r@437 t@471 s@505 u@529 v@553 w@616 z@622 b@667 a@677 e@681 f@685 F@691 B@793 G@827 d@835 c@843`。
- **Base 调用**：`h@153`、`g@356`、**`l@849`**（VoLTE 副作用路径）。
- **触发/开关**：厂商 `Coolpad`（`e0/c2.smali:1177`）。
- **隐私**：**P4**。

### 3.37 `e0/y0` — BROR / NOAIN 采集器（**唯一 Binder/AIDL 直调**）

- **方法结构**：**`static t(Context,int)String@15`**（AIDL 直调取 IMEI）、`u(int)Z@129`（`isValidSubscriptionId`）、`w(ctx,g)@187`（主读取）、`a(ctx)Z@703`（桩）、`b(ctx,g)@717`、`q(ctx,g)@925`、`v(ctx,g)@1283`。
- **`t()` 的完整调用链（核心发现）**：
  ```
  Class.forName("com.mediatek.internal.telephony.ITelephonyEx$Stub")   // :19
  Class.forName("com.mediatek.internal.telephony.ITelephonyEx")        // :41
  getMethod("getSvlteImei", ...)                                       // :53
  Class.forName("getService") → ServiceManager.getService("phoneEx")    // :65-69
  ... asInterface(...) → ITelephonyEx.getSvlteImei(slot)               // 后续
  ```
  即：**不通过任何公开 API，直接反射拿 MTK 的 Binder 代理并跨进程调用 `getSvlteImei`**（SVLTE = 单卡双待 VoLTE 场景下的 IMEI）。
- **其它隐藏面**：`getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getMsisdn`、`getNetworkOperatorForSubscription`、`getNetworkType`、`getVoiceNetworkType`、`getCellLocationForPhone`。
- **写入键**（22 个）：`g@248 i@298 h@330 j@354 k@378 l@441`、`r@504 t@532 s@564 u@588 v@612 w@675`、`m@1104 n@1112 x@1269 y@1277`、`b@722 a@732 e@736 f@740 d@762 c@768`。
- **Base 调用**：`e@210`、`g@437`、`f@1076`。
- **触发/开关**：厂商 `BROR`（`e0/c2.smali:451`）；型号 `NOAIN K11 Plus`、`NOAIN X11`、`NOAIN X13L`、`NOAIN X15L`；属性 `ro.build.hardware.version`、`ro.build.display.id`、`ro.bird.update.version`、`ro.product.hardware`、`ro.software.version`、`ro.hardware.version`、`ro.yunos.version`、`ro.yunos.name`。
- **降级**：`ITelephonyEx`/`phoneEx` 不存在（非 MTK）时 `Class.forName` 抛 `ClassNotFoundException` → 返回 null，由 `w()` 的 `getDeviceId` 兜底。
- **隐私**：**P4**；且是**最隐蔽的一条 IMEI 获取通道**。

### 3.38 `e0/y1` — 小米采集器 `DeviceInfoReaderXiaomi`（**体量最大，200+ 型号白名单**）

- **方法结构**：`A(ctx,g)@15`（主读取，`TelephonyManagerEx`）、`B(ctx,g)@545`（**权限门禁 + MIUI 私有读法**）、`t(ctx)Z@1229`（开机向导）、`v(g)String@1272`、`w()String@1300`、`static x()JSONObject@1312`（内存）、`static y()String@1381`（**`/proc/meminfo` FileReader 解析**）、`z()Z@1575`（**isFiveGCapable**）、`b(ctx,g)@1627`、`u(ctx,int)String@3130`（**每卡 VoLTE 取值器**：`isVolteEnabledByUser`，被 `:884`、`:1168` 调用 → `o`/`z`）。
- **使用 API / 隐藏面**
  - **`miui.telephony.TelephonyManagerEx.getDefault()`**（`:38-62`）+ **`getCellLocationForSlot`**（`:110,285,523,683,864,1148`）。
  - **`miui.telephony.TelephonyManager.getDefault()`**（`:585-605`）+ **`getSubscriberIdForSubscription` / `getSimSerialNumberForSubscription` / `getSimOperatorForSubscription` / `getNetworkTypeForSubscription`**（`:685-773`）。
  - **`isFiveGCapable`**（`:1602`）、**`isVolteEnabledByUser`**（`:3165`）。
  - `getImei`、`getNetworkType`、`getVoiceNetworkType`、`getLine1Number`、`getSimOperator`。
  - 属性：`ro.build.hardware.version`（`:1285`）、**`ro.miui.cust_hardware`**（`:1295`）、`ro.build.version.incremental`（`:1303`）、`/proc/meminfo`（`:1393`）、`Settings$Global.getString(...)`（`:3003`，`miui_version_name`）。
- **写入键**（25 个）：`g@94 i@156 h@184 j@204 k@224 l@279`、`r@340 t@374 s@406 u@430 v@454 w@517`、`p@797 o@817`、`A@1081 z@1173`、`b@1638 a@1648 e@1652 f@1656`、`F@1662 B@1764 G@1804`、`c@2967 d@2973`。
- **Base 调用**：`h@96`、`g@275`、**`j@791`**（5G）、**`r@811`**（cell）、**`d@813`**（VoLTE）。
- **触发/开关**：厂商 `Xiaomi`（`e0/c2.smali:466`）；`b()` 内对 `Build.MODEL` 做**数百个型号的 `equals` 白名单**（`Mi 10`、`Mi 10 Pro`、`MI 8 Lite`、`MI 9`、`MI CC 9`、`Redmi 7/7A/8/8A/K30/K30 5G/K30 Pro Zoom Edition/Note 7/7 Pro/8/8 Pro`、`Mi9 Pro 5G`、`M1808D2TC`…`2312DRAABC`），其中 `Mi9 Pro 5G`、`Redmi K30 5G` 走单独 5G 分支（`:1825,1885`）。
- **隐私**：**P4 +**（双卡全量 + 小区 + VoLTE 用户开关 + 5G 能力 + RAM + MIUI 版本）。
- **未解**：`w()` 与 `x()`/`y()` 返回值分别对应“总内存 / 可用内存 / …”哪一个，需按调用点再核。

### 3.39 `e0/z` — 合成桥：`SubscriptionManager.getActiveDataSubscriptionId()`

- **smali**：`invoke-static {}, Landroid/telephony/SubscriptionManager;->getActiveDataSubscriptionId()I`
- **调用者**：`e0/a0.smali:945`、**`e0/w1.smali:958`**（本报告范围内仅 `w1`；日志 `, activeDataSubId：`）。
- **语义**：当前**承载数据业务**的 subId（-1 表示无）。与 `getDefaultDataSubscriptionId()` 不同：后者是“默认”，前者是“当前实际”。应用用它在多卡切换时确定 `B DataSlot`。
- **隐私**：间接决定 `DataSlot` 归属（可推断用户当前用哪张卡上网）。

### 3.40 `e0/z0` — nubia 采集器 `DeviceInfoReaderNubia`

- **方法结构**：`t(ctx)Z@23`（`Settings$Secure.getInt` + `checkUserPermission:`）、`u(ctx,g)@66`（主读取）、`b(ctx,g)@324`。
- **使用 API / 隐藏面**
  - `getImei`/`getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getNetworkType`、`getVoiceNetworkType`、`getCellLocation`、`getSimOperatorNumericForPhone`；`getSubId`（`:117`）+ `isValidSubscriptionId`（`:175`）。
  - **`ro.nubia.nr.support`**（`:368`）→ 写 `G DeviceType`；`ro.build.rom.pcb.silkscreen`（`:451`，**主板丝印号**，硬件级指纹）；`ro.build.display.id`（`:443`）。
  - 运营商键 `operator_service_improve_plan`；型号 `NX651J`、`NX659J`、`angelcare N9`。
- **写入键**（27 个）：`B@84 g@100 i@108 h@131 j@139 k@147 l@161 n@173 m@185 o@195 r@210 t@218 s@234 u@242 v@250 w@264 y@276 x@288 z@298 b@348 a@358 e@362 f@366 G@394 F@400 d@449 c@457`。
- **Base 调用**：`g@157`。
- **触发/开关**：厂商 `nubia`（`e0/c2.smali:1025`）。
- **隐私**：**P4 +**（含**主板丝印**这种强硬件指纹）。

### 3.41 `e0/z1` — YinKe 采集器 `DeviceInfoReaderYinKe`（**与 s1/y 同源**）

- **与 `s1` 的差异**（Jaccard 97.6%）：仅日志 tag `DeviceInfoReaderYinKe`、型号 `YK6701`、以及“未知型号”日志文本；`ColorOSTelephonyManager` + `isOemEnhanced4gLteModeSettingEnabledByUser` + `ro.board.hardware_version` 全部一致。
- **写入键**（23 个）：`g@151 i@213 h@249 j@273 k@297 l@360 o@366 r@437 t@471 s@505 u@529 v@553 w@616 z@622 b@667 a@677 e@681 f@685 F@691 B@793 G@827 d@835 c@843`。
- **Base 调用**：`h@153`、`g@356`、**`l@849`**。
- **触发/开关**：厂商 `YIK`（`e0/c2.smali:1192`）。
- **隐私**：**P4**。

---

## 4. 交叉发现

### 4.1 采集矩阵（谁采什么，均按 §1.1 权威表统计）

| 采集项 | 类数 | 类 |
|---|---|---|
| 双卡 IMEI/IMSI/ICCID/MSISDN/MNC/NCLS（`g…w`） | 32 | 除 `s0` 外全部 |
| 仅单卡（`g…l`+`o`） | 1 | `s0`（Lenovo 平板） |
| CellID1/LAC1（`m/n`） | 13 | `m0 n0 n1 p1 q0 s t1 u1 v1 x0 x1 y0 z0`（`q1/y1/w0` 经 `d0/g` 间接） |
| CellID2/LAC2（`x/y`） | 12 | `n0 n1 p1 q0 s t1 u1 v1 x0 x1 y0 z0` |
| VoLTE1/2（`o/z`） | 15 / 14 | `m0 m1 q1 s0 s1 t0 t1 u1 v1 w0 w1 y y1 z0 z1`（`z` 少 `s0`、`z0`） |
| 5G1/2（`p/A`） | 8 | `q1 t0 t1 u1 v1 w0 w1 y1` |
| 设备注册块（`B/F/G`） | 17 | `m0 m1 o0 q1 s0 s1 t0 t1 u0 u1 v1 w0 w1 y y1 z0 z1` |
| 内存 / DRAM | 7 | `m1 p1 q1 t1 v1 w0 y1` |
| 存储容量 | 2 | `p1`（StatFs root）、`w1`（root + data + 可用） |
| 5G 专有判定 | 6 | `q1`（`ro.telephony.default_network>=30`）、`t1/v1`（SA/NSA）、`u1`（vivo 参数表）、`y1`（`isFiveGCapable`）、`w1`（`ro.boot.5GEnable1/2`）、`z0`（`ro.nubia.nr.support`） |

### 4.2 隐藏 API 依赖清单（按能力归类）

| 能力 | 隐藏 / 私有 API | 使用类 |
|---|---|---|
| IMEI | `TelephonyManager.getDeviceId/getImei`（反射） | 全部 |
| IMEI（MTK 属性） | `SystemProperties gsm.mtk.imei1/gsm.mtk.imei2` | `n1、x0` |
| IMEI（Binder AIDL） | `ITelephonyEx$Stub` + `getService("phoneEx")` + `getSvlteImei` | `y0` |
| IMSI | `getSubscriberId(ForSubscription)` | 全部 |
| ICCID | `getSimSerialNumber(ForSubscription)` | 全部 |
| 本机号 | `getLine1Number(ForSubscriber)`、`getMsisdn` | 全部 / `o0 p1 q0 r1 s u w0 w1 y0` |
| 运营商 | `getNetworkOperator(ForSubscription/ForPhone)`、`getSimOperator(Numeric(ForPhone))` | 全部 |
| 小区 | `getCellLocation(ForPhone/ForSlot/FromSlotId/BySubId)`、`getAllCellInfo`、`getPhoneCid/getPhoneLac` | 见 4.1 |
| 卡槽 / subId | `SubscriptionManager.getSubId/getPhoneId/getSlotIndex/isValidSubscriptionId/getDefaultSubscriptionId/getActiveSubscriptionInfoForSimSlotIndex` | 全部 |
| VoLTE（系统） | `ImsMmTelManager.isAdvancedCallingSettingEnabled` + **`setAdvancedCallingSettingEnabled`** | 基类 `c()/d()`；由 `t0 t1 v1 w0 w1 q1 u1 y1`（+ 直接调 `r.l()` 的 `m0 m1 s1 y z1`）触发 |
| VoLTE（厂商） | `isOemEnhanced4gLteModeSettingEnabledByUser` | `m1 s1 y z1 t0` |
| VoLTE（厂商） | `HwTelephonyManager.getServiceAbility`/`isVolteAvailable` | `t1 v1` |
| VoLTE（厂商） | `miui.telephony.TelephonyManager.isVolteEnabledByUser` | `y1` |
| 5G | `Settings.Global preferred_network_mode{slot}` ∈ [23,33] | 基类 `j()`；`q1 t0 w0 y1` 等 |
| 5G（厂商） | `VivoTelephonyApiParams.getParamValues` / `isFiveGCapable` / `ro.boot.5GEnable1|2` / `ro.nubia.nr.support` | `u1 / y1 / w1 / z0` |
| 系统服务直取 | `getSystemService("phone_ex")`、`("telephony_ims")`、`("telephony_subscription_service")` | `r1 / 基类c,d / 多数` |
| 内部类 | `com.android.internal.telephony.PhoneFactory#getPhone` | `p0` |

### 4.3 权限与门禁现状

- **代码中出现的权限串**：`android.permission.READ_PHONE_STATE`、`android.permission.READ_PHONE_NUMBERS`（`e0/r`、`t0`、`t1`、`v1`、`w0`、`y1` 等）。
- **真正起“阻断”作用的分支只有 1 处**：`e0/u1` 的 `isUserPermissionOpen==false, do not read!`（`e0/u1.smali:2011-2015`）。
- 其余权限判定（`checkUserPermission:` 日志、`readAndroidR no READ_PHONE_STATE PERMISSION_GRANTED`）**只记日志，不改变流程** —— 权限缺失时靠反射异常 → 字段 `null` 降级。
- `a(Context)` 门禁全为桩（恒 false），`e0/c2.k()` 无调用者。

### 4.4 代码质量与可追溯性

- **模板复制**：`s1(TCL) ≡ y(Coolpad) ≡ z1(YinKe)`；`t1(TDTECH) ≈ v1(WIKO/PTAC)`；`n0` 的 `u()/v()/t()` 三份近似重复；`r0` 与 `q0` 各 5 套读法；`e0/y` 残留 `DeviceInfoReaderOPPO module unknow!!!`。
- **重复写入（后者覆盖前者）**：`q1` 的 `p`（`:1774`,`:1779`）、`q1` 的 `A`（`:2344`,`:2349`）、`n0` 的 `x`（`:552`,`:1121`）。
- **死代码**：所有 `a()`；基类 `b()` 中的 `n()` 分支不可达、`l()` 在基类 `b()` 中不可达 —— 但 `m0 m1 s1 y z1` 会**直接 `invoke Le0/r;->l()`** 把这套“SDK≥30 补采（VoLTE + 5G + 小区）”重新激活。
  > 说明：各厂商的 `u()/v()/x(ctx,int)` VoLTE/5G getter **不是**死代码，均被同类调用（行号见 §3.2/3.3/3.12/3.19/3.20/3.22/3.23/3.26/3.29/3.31/3.32/3.35/3.36/3.38/3.41）。

### 4.5 数据外流形态（与 `f0/g` 衔接）

采集结果全部落进 `d0/g`，再由 `d0/g.e()` 组装 JSON；键名风格是“**厂商命名 + 卡序后缀 1/2**”，与 `f0/g` 的 `Content-Type: application/encrypted-json` 上报组合，构成完整的“设备指纹 + 双卡身份 + 位置 + 5G/VoLTE 能力”画像。关键点：**上报键是 `IMEI1/IMSI1/ICCID1/MSISDN1` 这种直白命名**，没有任何脱敏或哈希（`d0/g.smali:1147-1411`）。

---

## 5. 未知项 / 待确认

| # | 未知点 | 现状与影响 |
|---|---|---|
| 1 | `d0/g.P`、`d0/g.Q`（两个 `int`，初值 0 / 1）的语义 | 构造中初始化，`e()` 未见以固定键写出，**用途未确认**（疑为“卡数 / 规格版本”）。|
| 2 | `d0/g.O`（`private String`，不在 `e()` 输出中） | **写入者与用途未确认**。|
| 3 | `e0/q1` 的 `final b:I`、`final c:I` 常量真值 | 构造期赋值，疑似槽位或 API 级别；**未逐值确认**。|
| 4 | 各厂商 VoLTE/5G getter 内部的分支阈值 | 已确认被调用（见 §4.4 说明），但**每个 getter 内部把原始值折算成 `"0"/"1"` 的具体判定**（如 `q1.u()` 读 `voicecall_type` 后的比较）未逐条展开。|
| 5 | `e0/u1.C(ctx,String,HashMap)HashMap` 与 `B/D` 的 `HashMap` 键名 | 只见 `resultMap:`/`ret:`/`response`/`put` 等日志，**参数表具体键未逐一还原**。|
| 6 | `e0/y1` 的 `w()/x()/y()`（内存三类值）与上报字段的对应 | `y()` 明确读 `/proc/meminfo`；`w()`/`x()` 的最终归属未完全对齐。|
| 7 | `e0/x1` 中 `c2.a.get(Build.MODEL)` 为 null 时 `g.a` 的最终形态 | 由 `d0/g.e()` 补 `"Unknown"`；**是否还有别处回填未确认**。|
| 8 | `e0/p0` 的 `new-array [Landroid/provider/ContactsContract$CommonDataKinds$Phone;`（`:300`） | 明显是反编译 / 类型解析产物（实际应为 `Object[]`/`Phone[]`），**真实类型未知**，不影响语义判断。|
| 9 | `e0/c2.k(Context)` 的外部用途 | 无 in-dex 调用者，可能供宿主 SDK 调用；**无法从本 APK 确认**。|
| 10 | 各厂商属性键在真机上的实际取值 | 全部为运行时读取，静态分析只能给出键名与默认值。|

---

## 6. 覆盖度自查

- [x] 41/41 个类全部逐类分析（含 8 个合成桥的 API 与数值语义）。
- [x] 每个类均给出：采集目标、Android/隐藏 API、输出键名（含 JSON key）、触发条件（厂商/型号/权限/属性）、降级分支、隐私含义。
- [x] 关键 smali 行号已引用（`e0/*.smali:行号`）。
- [x] 克隆体已用归一化行集合比对确认（`s1≈y≈z1`、`t1≈v1`），差异点逐条列出。
- [x] 字段写入以 §1.1 的**大小写敏感**权威表为准（初版曾把 `b/B`、`f/F`、`g/G` 合并，已更正）。
- [x] 与 `e0/r`、`e0/c2`、`d0/g`、`f0/o`、`f0/l`、`e0/d2`、`e0/i0` 的契约已交叉核对。
- [x] 未修改 APK / smali 树；仅新增本报告与临时抽取文件（`analysis\SystemHelper\_tmp\`）。


