> **Historical analysis with subsequent corrections.** Read the [final review](../REPORT.md) first. The review narrows or corrects earlier claims about MD5-derived keys, full installed-app inventories, the reachability of privacy notices, one-second upload intervals, timestamps, and confirmed replay. Some OEM branches remain outside the follow-up review. Local identity paths and actual user data are excluded.

# SystemHelper APK — `e0/*` collector collection analysis (collectors-2)

- **target APK**: `[LOCAL_PATH_REMOVED]`
- **package name**: `com.mobiletools.systemhelper` (versionName `1.2-20230925-U`, versionCode 3, minSdk 24, targetSdk 28)
- **analysis material**: apktool 3.0.3 decoding smali tree (read-only) `...\analysis\SystemHelper\apktool\smali`; full string list `...\all_const_strings.txt`
- **is responsible for this time**: 41 categories under `e0\` - `m, m0, m1, n, n0, n1, o, o0, o1, p, p0, p1, q, q0, q1, r0, r1, s, s0, s1, t, t0, t1, u, u0, u1, v, v0, v1, w, w0, w1, x, x0, x1, y, y0, y1, z, z0, z1`
  > The task description states "a total of 43 files", but the actual listed class names are **and 41**, and all 41 files exist and are covered class by class in this report (`e0\r` and `e0\c2` are only read as contracts and are not separated into separate sections).
- **method**: mainly smali (field / method body / `const-string` / `invoke-*` / `iput Ld0/g;` full coverage), with scripted structure extraction and clone comparison; APK and smali trees have not been modified.
- The line numbers in the article are all **original line numbers** corresponding to the `.smali` file.

---

## 0. Quick overview of key findings

1. The 41 classes **are not 41 capabilities, but "the same set of acquisition templates + 33 OEM variants + 8 compiler bridges"**. `e0/c2.h()` uses a string of `Build.MANUFACTURER` to compare `new-instance` in 60 branches to find the corresponding manufacturer class; `e0/c2.n()` then adjusts `((e0.c2$c)x).b(ctx, d0/g)` to complete a "device information" collection.
2. The **template fixedly outputs "Dual SIM Full Portrait"**: `IMEI1/2`, `IMSI1/2`, `ICCID1/2`, `MSISDN1/2`, `MNC1/2`, `NCLS1/2` (network type), `CellID1/2`, `LAC1/2`, `VolteEnabled1/2`, `5GEnabled1/2`, `DataSlot`, plus `Manuf/Model/HWVersion/SWVersion/OS/OSVersion`. That is, the `a`–`B` fields of `d0.g` are almost filled.
3. **Almost all sensitive reads bypass the public API**: Unify `f0.o` (`Class.forName` + `getMethod` + `invoke`) to adjust the hidden method, such as `TelephonyManager.getDeviceId/getSubscriberId/getSimSerialNumber/getLine1Number/getImei/getNetworkOperatorForPhone/getCellLocationForPhone`, `SubscriptionManager.getSubId/getPhoneId/isValidSubscriptionId`, `MSimTelephonyManager.getDefault`, `HwTelephonyManager.getServiceAbility`, `miui.telephony.TelephonyManager`, `ColorOSTelephonyManager`, `MzTelephonyManager`, `com.mediatek.telephony.TelephonyManagerEx`, `com.android.internal.telephony.PhoneFactory.getPhone`.
4. **Two truly “cross-process/system-level” capabilities**:
   - `e0/y0`: `Class.forName("com.mediatek.internal.telephony.ITelephonyEx$Stub")` + `ServiceManager.getService("phoneEx")` + `asInterface` → Directly call MTK `ITelephonyEx.getSvlteImei(slot)` (Binder call, `e0/y0.smali:15-127`).
   - `e0/u1`: `getITelephony()` + `com.android.internal.telephony.VivoTelephonyApiParams.getParamValues()` → Get vivo’s private 5G/VoLTE parameters (`e0/u1.smali:1325-1852`).
5. **has side effects, not just "reading"**: the base class `e0/r.c()` detects that VoLTE is turned off and will **reflectively call `ImsMmTelManager.setAdvancedCallingSettingEnabled(true)` to turn VoLTE on** (`e0/r.smali:592-636`). This is the only writing of system status by this module, which requires `MODIFY_PHONE_STATE`; it is triggered by `t0、t1、v1、w0、w1、q1、u1、y1` and `m0、m1、s1、y、z1` directly from `invoke Le0/r;->l()`.
6. The **brand whitelist is a hard-coded executable model list**: `e0/c2.i()` writes "Model → Manufacturer" into the static Map (`SM-G9750/SM-N9760/MI 9/Redmi K30 5G/…`), `e0/y1` (Xiaomi) comes with 200+ model whitelists, `e0/x1` (Little Pepper) uses 20+ `Build.MODEL` Select attributes by model.
7. **`a(Context)` access control is a "dead stub"**: all overwritten `a()` in 41 classes are `new ServiceState().getRoaming()` (constant `false`), `e0/c2.k()` therefore always returns `Boolean.FALSE`, and `c2.k()` is all APK No caller. **manufacturer selection is only determined by the manufacturer string, and collection of** is performed unconditionally.
8. **permission downgrade is "silently blanked"**: Can't get permission/When an exception is thrown, the field is left `null`, and `d0/g.e()` fills in `"Unknown"` or `""` uniformly; `e0/c2.n()` will also leave IMSI blank automatically `iput null` (`e0/c2.smali:3770-3790`).
9. **code height copy and paste**: `e0/s1`(TCL), `e0/y`(Coolpad), `e0/z1`(YinKe) Jaccard ≥95.6% after normalization (almost the same line by line, only missing the log tag, model string, attribute name); `e0/t1` (TDTECH) and `e0/v1` (WIKO) 57.8%. The "unknown model" log for `e0/y` even reads `DeviceInfoReaderOPPO module unknow!!!` by mistake (copy residue).

---

## 1. General table (class name | business meaning | key Android API | output key name | privacy level)

Privacy level: **P0** model/version only; **P1** device hardware identification (IMEI, etc.); **P2** SIM Identity (IMSI/ICCID/MSISDN); **P3** location (CellID/LAC/CellInfo); **P4** = P1+P2+P3 full portrait (+ manufacturer private 5G/VoLTE status).

| Class name | business implication | Key Android API / Hidden Side | Output key name (d0.g field → JSON key) | Privacy |
|---|---|---|---|---|
| `e0/m` | **synthetic bridge**: `NetworkRegistrationInfo.getDomain()` | `android.telephony.NetworkRegistrationInfo#getDomain`（API 29） | None (returns int to caller) | P0 |
| `e0/m0` | JingLing (JingLing/JingPad) collector | `TelephonyManagerEx` (reflection) `getImei`/`getCellLocationForPhone`; `SubscriptionManager.getPhoneId` | `a Manuf,b Model,c HWVersion,d SWVersion,e OS,f OSVersion,g IMEI1,h ICCID1,i IMSI1,j MSISDN1,k MNC1,l NCLS1,m CellID1,n LAC1,o VolteEnabled1,p 5GEnabled1,r IMEI2,s ICCID2,t IMSI2,u MSISDN2,v MNC2,w NCLS2,z VolteEnabled2,B DataSlot,F SubType,G DeviceType` | **P4** |
| `e0/m1` | realme collector (including DRAM/RAM analysis) | `ColorOSTelephonyManager.getDefault`；`isOemEnhanced4gLteModeSettingEnabledByUser`；`SystemProperties ro.boot.dram_info`/`ro.build.hardware.version` | Same as above + `B/F/G` | **P4** |
| `e0/n` | **synthetic bridge**: `NetworkRegistrationInfo.getTransportType()` | `NetworkRegistrationInfo#getTransportType`（API 29） | None | P0 |
| `e0/n0` | kingsun collector (**three sets of reading**) | `MSimTelephonyManager.getDefault` + `getPhoneCid`/`getPhoneLac`/`getSimOperator` | Full Dual SIM Card + `m/n/x/y` | **P4** |
| `e0/n1` | Shown (360OS) collector | `SystemProperties` (`gsm.mtk.imei1/2`, `ro.system.OSVersion`, `ro.internal.version`, `ro.sf.lcd_density`); universal `Class.forName` reflection tool | Full Dual SIM Card + `m/n/x/y` | **P4** |
| `e0/o` | **synthetic bridge**: `NetworkRegistrationInfo.getCellIdentity()` | `NetworkRegistrationInfo#getCellIdentity`（API 29） | None | P0 |
| `e0/o0` | KONKA collector | `TelephonyManagerEx` (reflection) `getCellLocationForPhone`; `getNetworkOperatorForPhone`; `getSubId`; `ro.boot.hardware.revision` | Full Dual SIM Card + `B/F/G` | **P4** |
| `e0/o1` | OYSIN / SINTAVE collector | `SubscriptionManager.isValidSubscriptionId`；`getDeviceId`；`getCellLocationForPhone` | Full Dual SIM Card | **P4** |
| `e0/p` | **synthetic bridge**: `CellIdentityTdscdma.getCid()` | `android.telephony.CellIdentityTdscdma#getCid`（API 29） | None (TD-SCDMA CID) | P0 |
| `e0/p0` | K-Touch collector | `com.android.internal.telephony.PhoneFactory.getPhone(i)`；`getPhoneCount`；`getSimOperator` | Full Dual SIM Card | **P4** |
| `e0/p1` | SOP collector (**memory/storage analysis**) | `/proc/meminfo`；`StatFs`；`isValidSubscriptionId`；`persist.sys.hardware.version` | Full Dual SIM Card + `m/n/x/y` | **P4** (+capacity) |
| `e0/q` | **synthetic bridge**: `CellIdentityTdscdma.getLac()` | `CellIdentityTdscdma#getLac`（API 29） | None (TD-SCDMA LAC) | P0 |
| `e0/q0` | lephone collector (**five sets of reading**) | `getAllCellInfo` (list parsing); `getCellLocationForPhone`; `getNetworkOperatorForSubscription`; `getMsisdn` | Full Dual SIM Card + `m/n/x/y` | **P4** |
| `e0/q1` | Samsung collector (**5G/VoLTE specialization**) | `getTelephonyProperty("ril.hw_ver"/"ro.build.PDA")`；`createForSubscriptionId`；`getCellLocationBySubId`；`Settings$System.getInt("voicecall_type")`；`ro.telephony.default_network` | Full dual SIM card + `B/F/G` + `p/A/z` | **P4** |
| `e0/r0` | LETV / LeTV collector (**five sets of reading methods**) | `com.mediatek.telephony.TelephonyManagerEx.getDefault`；`getCellLocationFromSlotId`；`getSimOperator` | Full Dual SIM Card | **P4** |
| `e0/r1` | Spreadtrum / Spreadtrum collector | `phone_ex`(TelephonyManagerEx) + `getDeviceId` + `getMsisdn`；`getSubId` | Full Dual SIM Card | **P4** |
| `e0/s` | Bee Collector | `getAllCellInfo` + list parsing; `ro.sf.lcd_density` | Full Dual SIM Card + `m/n/x/y` | **P4** |
| `e0/s0` | Lenovo collector (**only has single card**) | `isValidSubscriptionId`；`getDeviceId`…；`Settings$Global.getInt`；`getDefaultDataSubscriptionId`+`getPhoneId` | `g…l,o` + `B/F/G` | **P3+** |
| `e0/s1` | TCL collector (**and y/z1 have the same origin as**) | `ColorOSTelephonyManager`；`isOemEnhanced4gLteModeSettingEnabledByUser`；`ro.mtk.build.version`/`ro.tcl.hardware.version` | Full Dual SIM Card + `B/F/G` | **P4** |
| `e0/t` | BenWee collector | `android.telephony.MSimTelephonyManager.getDefault` (all fields) | Full Dual SIM Card | **P4** |
| `e0/t0` | "Liantong" (customized by China Unicom) collector | `ColorOSTelephonyManager`; `Settings$Global` (`is5GEnable`/`preferred_network_mode`); `isOemEnhanced4gLteModeSettingEnabledByUser`; READ_PHONE_STATE access control | Full dual SIM card + `B/F/G` + `p/A/o/z` | **P4** |
| `e0/t1` | TDTECH (Dingqiao) collector | `HwTelephonyManager.getServiceAbility`/`isVolteAvailable`; `MSimTelephonyManager`; `e0.i0` Get the subId array; `ro.boot.dram_info` | Full Dual SIM Card + `B/F/G` + `m/n/x/y` + `p/A/o/z` | **P4** |
| `e0/u` | BestSonny Collector | `getDeviceId`/`getCellLocationForPhone`/`getMsisdn`/`getNetworkOperatorForPhone` | Full Dual SIM Card | **P4** |
| `e0/u0` | Meizu collector | `MzTelephonyManager`；`getDefaultSubscriptionId`；`getSimOperatorNumericForPhone` | Full Dual SIM Card + `B/F/G` | **P4** |
| `e0/u1` | vivo collector (**, deepest**) | `ITelephony#getITelephony` + `VivoTelephonyApiParams.getParamValues`；`persist.radio.vivo.fiveg`/`persist.vendor.radio.5g_mode_pref`；`ro.vivo.op.entry` | Full Dual SIM Card + `B/F/G` + `m/n/x/y` + `p/A/o/z` | **P4** |
| `e0/v` | CHANGHONG collector (read-only version of **itself, relying on `base.p()` to complete**) | No system service; only `SystemProperties`(`ro.denqin.*`, `ro.yunos.*`) | `a,b,c,d,e,f` (6 items by itself) + base class to supplement double cards | **P4** |
| `e0/v0` | **synthetic bridge**: `SubscriptionInfo.getMncString()` | `android.telephony.SubscriptionInfo#getMncString`（API 29） | None | P0 |
| `e0/v1` | WIKO / PTAC collector (≈t1) | Same as `t1` + direct connection `TelephonyManager` fallback + variable length reflection parameters | Same as `t1` | **P4** |
| `e0/w` | ChuiZi (hammer/nut) collector | `getImei`/`getNetworkOperatorForPhone`/`getSimOperatorNumericForPhone`；`ro.smartisan.version` | Full Dual SIM Card | **P4** |
| `e0/w0` | Motorola / ZUI collector | `getActiveSubscriptionInfoForSimSlotIndex(slot)`；`ro.zui.software.display`；`ro.boot.dram_info` | Full dual SIM card + `B/F/G` + `p/A/o/z` | **P4** |
| `e0/w1` | Xiaodu (Xiaodu) collector (**storage image**) | `Environment`+`StatFs`（blockSize/availableBlocks/blockCount）；`ro.boot.5GEnable1/2`；`persist.sys.cucc.dm` | Full dual SIM card + `B/F/G` + `p/A/o/z` | **P4** (+ partition capacity) |
| `e0/x` | **synthetic bridge**: `SubscriptionManager.getSlotIndex(subId)` | `SubscriptionManager#getSlotIndex` (API 29, called at 26) | None | P0 |
| `e0/x0` | mfox collector | `gsm.mtk.imei1/2` (MTK double IMEI attribute); `e0.d2` reflection subId read | Full Dual SIM Card + `m/n/x/y` | **P4** |
| `e0/x1` | Small pepper collector (**model branch is the most broken**) | 20+ `Build.MODEL` branch + `ro.build.display.hwid`/`ro.product.yuxin*` etc.; `getNetworkOperatorForPhone`/`getSimOperator` | Full Dual SIM Card + `m/n/x/y` | **P4** |
| `e0/y` | Coolpad collector (≈s1/z1) | `ColorOSTelephonyManager`；`ro.board.hardware_version` | Full Dual SIM Card + `B/F/G` | **P4** |
| `e0/y0` | BROR / NOAIN collector | **`ITelephonyEx$Stub` + `ServiceManager.getService("phoneEx")` + `getSvlteImei`**；`getMsisdn`；`getCellLocationForPhone` | Full Dual SIM Card + `m/n/x/y` | **P4** |
| `e0/y1` | Xiaomi collector (**is the largest, 200+ models whitelist**) | `miui.telephony.TelephonyManager(Ex)`：`getSubscriberIdForSubscription`/`getSimSerialNumberForSubscription`/`getNetworkTypeForSubscription`/`getCellLocationForSlot`/`isFiveGCapable`；`/proc/meminfo` | Full dual SIM card + `B/F/G` + `p/A/o/z` | **P4** |
| `e0/z` | **synthetic bridge**: `SubscriptionManager.getActiveDataSubscriptionId()` | `SubscriptionManager#getActiveDataSubscriptionId`（API 29） | None | P0 |
| `e0/z0` | nubia collector | `ro.nubia.nr.support`；`Settings$Secure.getInt`；`getSubId`；`ro.build.rom.pcb.silkscreen` | Full Dual SIM Card + `B/F/G` + `m/n/x/y` + `o/z` | **P4** |
| `e0/z1` | YinKe collector (≈s1/y) | `ColorOSTelephonyManager`；`ro.board.hardware_version` | Full Dual SIM Card + `B/F/G` | **P4** |

---

## 1.1 Authoritative fields are written into the table (case sensitive, as the final caliber of each section)

> **Caliber Description**: `d0/g` Field names are case-sensitive (`b`=Model and `B`=DataSlot, `g`=IMEI1 and `G`=DeviceType, `f`=OSVersion and `F`=SubType are different fields). The following table is extracted from `iput-object … Ld0/g;->X:` **case-sensitive** line by line. `X@line number` is the first write position. If the general table/section conflicts with this table, **this table shall prevail**.

| class | Number of fields | Write field@line number |
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
| `v` | 6 | `b@36 a@46 e@50 f@54 c@74 d@82` (the remaining SIM fields are written by `Le0/r.p()` within `r.smali`) |
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

The global writer **of the** uppercase field (`C/D/E/H/I/J/K/L/M/N` and **among these 33 classes no one writes**):

| Field | Semantics | Write class |
|---|---|---|
| `B` | `DataSlot` | `m0 m1 o0 q1 s0 s1 t0 t1 u0 u1 v1 w0 w1 y y1 z0 z1` (17) |
| `F` | `SubType`(JSONObject) | Same as `B` (17 identical) |
| `G` | `DeviceType` | Same as `B` (17 identical) |
| `A` | `5GEnabled2` | `q1 t0 t1 u1 v1 w0 w1 y1` (8 pieces) |

> Observation: The three writers of `B/F/G` completely overlap and always appear continuously in the end of the same segment `b()` - they are the same "device registration block" **(data card slot + subtype + device type) of**, which is uniformly produced by 17 newer collectors; `A` (5G card 2) only has 8 A "5G professional" collector is written.

---

## 2. Public contract (understanding the premise of 41 classes)

### 2.1 `e0/c2` Scheduling: Manufacturer → Collector

| location | behavior |
|---|---|
| `e0/c2.smali:364-1369` | `h(Context)`: `SDK_INT>=30` first `e0.r.k(ctx)` registers TelephonyCallback; `c2.a.get(Build.MODEL)` gets the manufacturer string; then **60 `String.equals` branches** `new-instance` correspond to the collector; miss → base class `e0/r` (`e0/c2.smali:1319, 1358`).|
| `e0/c2.smali:1370-3392` | `i()`: Lazy loading of static `Map a` (model **→ manufacturer**). First use `SystemProperties.get("ro.product.manufacturer")` (`f0/l.i`) to determine whether it belongs to the `vivo/samsung/realme/xiaodu/DOUQI` whitelist, if it hits `a.put(Build.MODEL, manuf)`; then hard-code hundreds of `a.put("SM-G9750","samsung")`...`a.put("XT2335-3","motorola")`.|
| `e0/c2.smali:3404-3417` | `j(Context)` → `h(ctx).a(ctx)`; `k(Context)` is packaged as `Boolean`. There is no caller **in all** and APK, and all `a()` are always false → dead code.|
| `e0/c2.smali:3717-3927` | `n(Context,d0/g)`: `g.J = PackageManager.getPackageInfo(pkg,0).versionName`; `h(ctx).b(ctx,g)`; then clear the empty IMSI/IMEI2 and so on to `null`.|
| `e0/c2.smali:139-187` | `f(Context)`: `i()` → `new d0/g` → `n()` → `l()` (ICCID change detection, prefs key `iccid1`) → `m()` → `o()` → `new d0/i(...).start()` (asynchronous thread supplementary acquisition).|

The `e0/c2$c` interface consists of two methods `a(Context)Z` + `b(Context,d0/g)V` (implemented by `e0/r`).

### 2.2 `e0/r` base class “DeviceInfoReaderBase” (`e0/r.smali`, line 2948)

| method | Semantics (checked implementation) |
|---|---|
| `b(ctx,g)` `:491` | Fill in `g.b=Build.MODEL`, `g.a=Build.MANUFACTURER`, `g.e="Android"`, `g.f=Build.VERSION.RELEASE`, `g.d=Build.DISPLAY`, `g.c=SystemProperties("ro.hardware.version")`; `SDK<26 → m()`, otherwise `o()` (`n()` is judged repeatedly with the same condition → unreachable; `l()` is in the base class Not reachable in `b()`).|
| `p(ctx,g)` `:1862` / `q(ctx,g)` `:2256` | Dual card reflection full read: `getDeviceId`/`getNetworkType`/`getNetworkOperator(ForSubscription)`/`getLine1NumberForSubscriber`/`getSimSerialNumber`/`getSubscriberId`; and write back `CellID0/LAC0/CellID1/LAC1`.|
| `c(ctx,slot)` `:560` | **VoLTE read + force open**: `getSystemService("telephony_ims")`→`ImsManager`; `e0.g.a(ImsManager,subId)`→`ImsMmTelManager`; `e0.h.a(ImsMmTelManager)`→`isAdvancedCallingSettingEnabled()`; **reflects when closed `setAdvancedCallingSettingEnabled(true)`**;return `"1"/"0"/"-1"`.|
| `d(ctx,slot)` `:690` | Ditto for another path (another IMS read variant).|
| `f(tm,subId)` `:745` | Reflection `getNetworkType(subId)`, is 0. Try again `getVoiceNetworkType(subId)`; all abnormal → 0.|
| `g(netType,operator)` `:814` | **network type → string** (see 2.4).|
| `i(slot)` `:987` | Reflect `SubscriptionManager.getSubId(slot)` → `int[]`, log `[getSubId] sub[0] is ` / `[getSubId] result is `.|
| `j(ctx,slot)` `:1168` | **5G determines**: `Settings.Global.getInt("preferred_network_mode"+slot, -1)`, `23 <= v <= 33` → true.|
| `k(ctx)` `:187` / `s(ctx)` `:373` | Register/log out `TelephonyCallback` (log `registerTelephonyCallback--mDMTelephonyCallback1/2`, `listenNetworkType---->`).|
| `r(ctx,slot,g)` `:2614` | Position acquisition: `READ_PHONE_STATE` judgment → `CellIdentity` analysis (`Le0/m`, `n`, `o`, `p`, `q` bridge here, `:2692/2700/2716/2728/2787/2795`) → `"%d:%d"` spelling CID:LAC.|

### 2.3 `d0/g` field → Report JSON key (`d0/g.e()` field by field `JSONObject.put`, `d0/g.smali:1038-1697`)

| Field | JSON key | Field | JSON key | Field | JSON key |
|---|---|---|---|---|---|
| `a` | `Manuf` | `n` | `LAC1` | `A` | `5GEnabled2` |
| `b` | `Model` | `o` | `VolteEnabled1` | `B` | `DataSlot` |
| `c` | `HWVersion` | `p` | `5GEnabled1` | `C` | `AccType` |
| `d` | `SWVersion` | `r` | `IMEI2` | `D` | `IPv4` |
| `e` | `OS` | `s` | `ICCID2` | `E` | `IPv6` |
| `f` | `OSVersion` | `t` | `IMSI2` | `F` | `SubType`(JSONObject) |
| `g` | `IMEI1` | `u` | `MSISDN2` | `G` | `DeviceType` |
| `h` | `ICCID1` | `v` | `MNC2` | `H` | `RegType` |
| `i` | `IMSI1` | `w` | `NCLS2` | `I` | `SpecVersion` (default `"3.0"`) |
| `j` | `MSISDN1` | `x` | `CellID2` | `J` | `RegVersion` (default `"1.0"`) |
| `k` | `MNC1` | `y` | `LAC2` | `K` | `Time` |
| `l` | `NCLS1` | `z` | `VolteEnabled2` | `L`/`M` | `AppInfo`/`SignalInfo` |
| `m` | `CellID1` | | | `N` | `History`; `P`/`Q` is int (semantics unknown) |

Default value: `a–g` defaults to `"Unknown"`, `h–p/r–A` defaults to `""`, `B` defaults to `"0"`.

### 2.4 `e0/r.g()` network type mapping (`packed-switch` base 1, `e0/r.smali:962-984`)

| NETWORK_TYPE value | Name | Mapping results |
|---|---|---|
| 1,2,16 | GPRS/EDGE/GSM | `GSM` |
| 3,8,9,10,15 | UMTS/HSDPA/HSUPA/HSPA/HSPAP | The operator is `46000/46002/46007` → `TD-SCDMA`, otherwise `WCDMA` |
| 4,5,6,7,12,14 | CDMA/EVDO_0/EVDO_A/1xRTT/EVDO_B/eHRPD | `CDMA` |
| 13,19 | LTE/LTE_CA | `LTE` |
| 17 | TD_SCDMA | `TD-SCDMA` |
| 20 | NR | `NR` |
| 11,18 | iDEN/IWLAN | Drop `default` → Log `unknown networktype ` |

> This string is the reported `NCLS1/NCLS2`, which is one of the direct basis for determining 5G access.

### 2.5 Reflection and attribute tools

| Tools | realize |
|---|---|
| `f0.o.a(String)` | `Class.forName` |
| `f0.o.b(Class,String,Class[])` | `Class.getMethod` (swallowing exception returns null) |
| `f0.o.c(Method,Object,Object[])` | `Method.invoke` (swallowing exception returns null) |
| `f0.l.i(String)` | Reflection `android.os.SystemProperties.get(key)` (`f0/l.smali:175-223`, catch → null) |
| `f0.l.j(String,String)` | Reflection `SystemProperties.get(key, def)` (`:225-301`, catch → def) |
| `f0.l.h(ctx,String,String)` | `SharedPreferences.getString` (**CellID/LAC’s cache reads**) |
| `e0.d2.a(slot)` | Reflection `SubscriptionManager.getSubId(slot)` → `int[]` |
| `e0.d2.b(tm,name,subId)` | Reflection `tm.<name>(subId)` |
| `e0.d2.c(tm,name1,name2,subId)` | First `name1(subId)`, then `name2(subId)` if abnormal (such as `getNetworkOperator` → `getNetworkOperatorForSubscription`) |
| `e0.i0.a(SubscriptionManager,int)` | For `e0/t1`, take the subId array |
| `e0.g` / `e0.h` | API Bridge: `ImsManager.getImsMmTelManager(int)` / `ImsMmTelManager.isAdvancedCallingSettingEnabled()` (out of the scope of this report, only referenced in the VoLTE path) |

---

## 3. Category-by-category analysis (in order of task list)

> Note: `e0/c2` will only call `b(Context,d0/g)` of the collector. The "Base Call" column lists the calls to `Le0/r;->x(...)` within the class (indicate which logic of the base class it reuses). The number of fields written is subject to §1.1.

### 3.1 `e0/m` — Synthetic Bridge: `NetworkRegistrationInfo.getDomain()`

- **essence**: R8/ProGuard generated for Java 8 method reference (`NetworkRegistrationInfo::getDomain`) `bridge synthetic` direct method bridge, `super=Ljava/lang/Object;`, no fields.
- **smali** (15 lines in full text):
  ```
  .method public static bridge synthetic a(Landroid/telephony/NetworkRegistrationInfo;)I
      invoke-virtual {p0}, Landroid/telephony/NetworkRegistrationInfo;->getDomain()I
  ```
- The only caller of **is**: `e0/r.smali:2692` (CellIdentity resolution path of base class `r(Context,int,d0/g)`).
- **numerical semantics (key)**: `getDomain()` Returns `NetworkRegistrationInfo.DOMAIN_*`: `0=DOMAIN_UNKNOWN`, `1=DOMAIN_CS` (circuit domain), `2=DOMAIN_PS` (packet domain), `3=DOMAIN_CS_PS`. The application determines whether the registration information contains the PS (data) field based on this, and then decides whether to take `CellIdentity` from this `NetworkRegistrationInfo` as the base station information.
- **Privacy**: No direct output; it is a link in the base station collection chain.

### 3.2 `e0/m0` — JingLing (JingLing/JingPad) collector `DeviceInfoReaderJingLing`

- **method structure**: `t(TM,d0/g)@15` (main reading), `u(Context)Z@173` (boot wizard completes judgment, `b()` in `:762` Call), `w(Context,d0/g)@224` (second reading), `b(ctx,g)@759` (entry), `v(Context,int)String@980` (**'s per-card VoLTE valuer** is called by `:500` and `:747`, and the results are written to `o`/`z` respectively).
- **uses API / hidden side**
  - `Settings$Global.getInt(cr,"device_provisioned",0)` + a `Settings$Secure` item + `checkUserPermission:` log (`:180-213`).
  - `Class.forName("android.telephony.TelephonyManager")` and **`android.telephony.TelephonyManagerEx`** (`:233-249`), log tag `phone_ex`.
  - Reflection method: `getImei`, `getSubscriberId`, `getSimSerialNumber`, `getLine1Number`, `getNetworkOperator`, `getNetworkType`, `getVoiceNetworkType`, **`getCellLocationForPhone`** (`:273-739`).
  - `SubscriptionManager.getDefaultDataSubscriptionId()` + Reflective `getPhoneId(subId)` (`:796-867`) → data card slot.
- **write key** (25, see §1.1 for details): `m,n` (card 1 cell), `g,i,h,j,k,l` (card 1 four elements + MNC + network type), `o` (VoLTE1), `r,t,s,u,v,w,z` (card 2 Full quantity), `b,a,e,f,d,c` (model/version), `F,B,G` (device registration block).
- **Base calls**: `r.h@295` (getSubId), `r.g@494` (network type → NCLS), **`r.l@948`** (the entire section SDK≥30 supplementary acquisition, including VoLTE literacy and cell).
- **trigger/switch**: `c2.h()` in the manufacturer string `JingLing` (`e0/c2.smali:1222`); in `b()`, press `Build.MODEL` whitelist (`JingPad C1`) to select attributes; if not hit, hit `DeviceInfoReaderJingLing unknow model:` and take the default attributes.
- **downgrade/side effects**: all reflection packets `try/catch`, failure to leave `null`; `r.l()` triggers VoLTE reading (including the side effect of "turn on when turned off", see §2.2).
- **Privacy**: **P4**.

### 3.3 `e0/m1` — realme collector `DeviceInfoReaderRealme` (including DRAM analysis)

- **method structure**: `t(ctx)Z@15` (boot wizard), `static v(ctx)J@66` / `static w(ctx)JSONObject@87` (memory information), `x(ctx,g)@170` (main read), `static y(ctx)J@747` / `static z()J@975` (DRAM Analysis), `b(ctx,g)@1091`, `u(ctx,int)String@1490` (**per-card VoLTE valuer**, called by `:466`, `:711` → write `o`/`z`).
- **uses API / hidden side**
  - **`android.telephony.ColorOSTelephonyManager`** (`:193`, OPPO/realme private class) + `getDefault()` (`:217-229`).
  - `getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getNetworkOperator`、`getNetworkType`、`getVoiceNetworkType`、`getCellLocation`（`:239-735`）。
  - **`isOemEnhanced4gLteModeSettingEnabledByUser`**（`:1541`）→ `u()`。
  - Properties: `ro.boot.dram_info` (`:978`, log `dramInfo: ` / `parsed value : ` / `No dram_info property`), `ro.build.hardware.version` (`:1258`), `ro.build.display.id` (`:1262`).
- **write key** (23): Card 1 `g,i,h,j,k,l` + `o`; Card 2 `r,t,s,u,v,w` + `z`; `b,a,e,f`, `d,c`, `F,B,G`.
- **Base calls**: `h@257`, `g@460`, **`l@1437`**.
- **trigger/switch**: manufacturer `realme` (`e0/c2.smali:1100`); model whitelist `RMX2201/RMX3043/RMX3093/RMX3122/RMX3125/RMX3143/RMX3300/RMX3334/RMX3362/RMX3461/RMX3611/RMX3708/RMX3781`.
- **downgrade/side effects**: `ro.boot.dram_info` missing → log and return to default; `l()` triggers VoLTE side effects.
- **privacy**: **P4** (+ RAM capacity portrait).

### 3.4 `e0/n` — Synthetic Bridge: `NetworkRegistrationInfo.getTransportType()`

- **smali**：`invoke-virtual {p0}, Landroid/telephony/NetworkRegistrationInfo;->getTransportType()I`。
- **caller**: `e0/r.smali:2700`.
- **numerical semantics**: `AccessNetworkType` —— `0=UNKNOWN`, `1=GERAN`(2G), `2=UTRAN`(3G), `3=EUTRAN`(4G) LTE), `4=CDMA2000`, `5=IWLAN`, `6=NR` (5G, API 29+). The base class determines "whether this registration information is 5G NR wireless access" based on this, which is a link in the **5G determination chain**.
- **Privacy**: No direct output.

### 3.5 `e0/n0` — kingsun collector `DeviceInfoReaderKINGSUN` (three sets of reading methods)

- **method structure**: `u(ctx,g)@15` (card 1 reading method), `v(ctx,g)@584` (card 2 reading method, and `u` Almost identical line by line), `a(ctx)Z@1155` (**pile**), `b(ctx,g)@1169`, `t(ctx,g)@1375` (**third set of** reading, `public`).
- **uses API / hidden side**
  - `Class.forName("android.telephony.TelephonyManager")` + `getDeviceId`/`getSubscriberId`/`getSimSerialNumber`/`getLine1NumberForSubscriber`/`getNetworkOperatorForSubscription`/`getNetworkType`/`getVoiceNetworkType`。
  - **`getPhoneCid` / `getPhoneLac`** (`:104,269,295,673,838,1606`) - manufacturer's proprietary "press phone to get CID/LAC", replacing `getCellLocation()`.
  - `android.telephony.MSimTelephonyManager.getDefault()`（`:1382-1406`）+ `getSimOperator`。
- **write keys** (22): `g,i,h,j,k,l`, `m,n` (card 1 cell), `r,t,s,u,v,w`, `x,y` (card 2 cell), `b,a,e,f,d,c`.
- **Base calls**: `e@40` (`getSubId` reflection), `g@259`.
- **Trigger/Switch**: Manufacturer `kingsun` (`e0/c2.smali:676`); Model `KINGSUN-F2/F4/F7/F8/F9`.
- **downgrade/side effects**: `u()`/`v()` are independent try/catch; silently null when `getPhoneCid/getPhoneLac` does not exist.
- **Notes (Defects)**: `t(ctx,g)` has been completely re-collected, and `iput g.x` appears twice (`e0/n0.smali:552` and `1121`). The latter may overwrite the former - a copy-paste legacy.
- **Privacy**: **P4**.

### 3.6 `e0/n1` — Shown (360OS customized) collector `DeviceInfoReaderShown`

- **method structure**: `<init>@15` (read `ro.sf.lcd_density` when constructed), `t(int)String@45` (replaced by `<init>` in `:36` Call), `u(String,String)String@69` (**general reflection tool**: `Class.forName(clazz)` + method name + Parameters, `:77`), `v(int)Z@130` (`isValidSubscriptionId`), `w(ctx,g)@188`, `x(ctx,g)@636`, `y(ctx,g)@1084`, `a(ctx)Z@1442` (stake), `b(ctx,g)@1456`.
- **uses API / hidden side**
  - `android.os.SystemProperties.get` series: **`gsm.mtk.imei1` / `gsm.mtk.imei2`** (MTK double IMEI, `:1098,1279`), `ro.system.OSVersion` (`:1559`), `ro.internal.version` (`:1759`) , `ro.sf.lcd_density` (`:20`), `ro.yunos.version`/`ro.yunos.systemname`, `ro.build.version.release`, `ro.hardware.version`.
  - `getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getLine1NumberForSubscriber`、`getNetworkOperatorForSubscription`、`getNetworkType`、`getVoiceNetworkType`、**`getCellLocationForPhone`**。
- **write keys** (22): `g,i,h,j,k,l`, `r,t,s,u,v,w`, `m,n,x,y`, `b,a,e,f,d,c`.
- **Base calls**: `e@217`, `g@407`, `f@1233`, `p@1727` (finally falls back to the base class `p()` for full reading).
- **trigger/switch**: manufacturer `Shown` (`e0/c2.smali:616`); model `SHOWN P1/P1+/P2/P5/P6/P7/P9`, `SHOWN_P1/_P6/_P7`; OS identification string `360OS`.
- **Privacy**: **P4**; additionally exposes "360OS/YunOS customized system" and screen density.

### 3.7 `e0/o` — Synthetic Bridge: `NetworkRegistrationInfo.getCellIdentity()`

- **smali**：`invoke-virtual {p0}, Landroid/telephony/NetworkRegistrationInfo;->getCellIdentity()Landroid/telephony/CellIdentity;`
- **caller**: `e0/r.smali:2716`, `:2728` (two places, corresponding to the cell 1 / cell 2 path).
- **semantics**: Return the base class `CellIdentity`, and then assign `instance-of` to `CellIdentityGsm/Lte/Nr/Tdscdma/Cdma` to get `Cid/Lac/Tac/Nci`. This is **API 29+ base station acquisition main channel** (replacing `getCellLocation()`).
- **Privacy**: No direct output, but determines the source of CellID/LAC.

### 3.8 `e0/o0` — KONKA collector `DeviceInfoReaderKONKA`

- **method structure**: `t(ctx)Z@15` (`device_provisioned` + `Settings$Secure` + `checkUserPermission:`), `u()String@105`, `v(int)I@117` (`getSubId`), `w(int)Z@175` (`isValidSubscriptionId`), `x(ctx,g)@233` (main read), `b(ctx,g)@717`.
- **uses API / hidden side**
  - `TelephonyManagerEx`（`:256`）+ **`getCellLocationForPhone`**（`:480,693`）。
  - `getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getMsisdn`、**`getNetworkOperatorForPhone`**、`getVoiceNetworkType`。
  - Attributes `ro.boot.hardware.revision` (`:108`), `operator_service_improve_plan` (carrier configuration database key).
  - `SubscriptionManager.getDefaultDataSubscriptionId()` + `getPhoneId`（`:754-825`）。
- **write key** (21): `g,i,h,j,k,l`, `r,t,s,u,v,w`, `b,a,e,f`, `F,B,G`, `c,d` (no `m/n/x/y`, no `o/p/z/A`).
- **Base calls**: `g@470`.
- **Trigger/Switch**: Manufacturer `KONKA` (`e0/c2.smali:691`; `:1113` has a duplicate `KONKA→o0`); Model `KONKA X10`.
- **Privacy**: **P4** (lacks cell/5G, but dual SIM and four elements are complete).

### 3.9 `e0/o1` — OYSIN / SINTAVE collector `DeviceInfoReaderOYSIN`

- **method structure**: `t(int)Z@15` (`isValidSubscriptionId`), `u(ctx,g)@73` (main read), `a(ctx)Z@519` (stub), `b(ctx,g)@533`.
- **uses API / Hidden surface**: `getDeviceId`, `getSubscriberId`, `getSimSerialNumber`, `getLine1NumberForSubscriber`, `getNetworkOperatorForSubscription`, `getVoiceNetworkType`, **`getCellLocationForPhone`** (`:136-511`).
- **write key** (18): `g,i,h,j,k,l`, `r,t,s,u,v,w`, `b,a,e,f,d,c`; `c` takes `ro.product.hardware` (`:607`), `e/f` takes `ro.yunos.name`/`ro.yunos.version` (`:617-633`).
- **Base calls**: `e@96`, `g@288`.
- **trigger/switch**: manufacturer `OYSIN` (`e0/c2.smali:496`); model `SINTAVE S1000`, `SINTAVE S1000 PLUS`.
- **Privacy**: **P4**.

### 3.10 `e0/p` — Synthetic Bridge: `CellIdentityTdscdma.getCid()`

- **smali**: `invoke-virtual {p0}, Landroid/telephony/CellIdentityTdscdma;->getCid()I` (TD-SCDMA identity class only available in API 29).
- **caller**: `e0/r.smali:2787`.
- **semantics**: TD-SCDMA cell number (CID). `getLac()` combined with `e0/q` is spelled as "CID:LAC" and reported (the base class is formatted with `"%d:%d"`, `e0/r.smali:2830`).
- **Privacy**: No direct output (participating in P3).

### 3.11 `e0/p0` — K-Touch collector `DeviceInfoReaderKTOUCH`

- **method structure**: `t(int)Z@15` (`isValidSubscriptionId`), `u(ctx,g)@73` (main read, including **PhoneFactory**), `v(ctx,g)@552` (second reading), `a(ctx)Z@998` (stake), `b(ctx,g)@1012`.
- **uses API / hidden side**
  - `getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1NumberForSubscriber`、`getSimOperator`、`getNetworkType`。
  - **`getPhoneCount()` + `com.android.internal.telephony.PhoneFactory#getPhone(i)`** (`:280-339`) - directly read the internal `Phone` object card by card.
  - `getDeviceId`、`getCellLocationForPhone`、`getNetworkOperatorForSubscription`、`getVoiceNetworkType`。
  - Attributes `ro.product.hardware` (`:1076`, including `SPREADTRUM` determination `:1078`), `ro.hw_version` (`:1123`), `ro.yunos.version`/`ro.yunos.systemname`.
- **writing keys** (18): `g,i,h,j,k,l`, `r,t,s,u,v,w`, `b,a,e,f,d,c`.
- **Base calls**: `e@98`, `g@274`.
- **trigger/switch**: manufacturer `K-Touch` (`e0/c2.smali:601`); model `K-Touch C1`, `K-Touch M3s Plus`.
- **Privacy**: **P4**.

### 3.12 `e0/p1` — SOP collector `DeviceInfoReaderKONKA` (tag mixed use) + memory/storage analysis

- **method structure**: `t()String@15` (**/proc/meminfo parses**), `u(ctx)String@193` (**StatFs Storage**), `v(int)Z@257` (`isValidSubscriptionId`), `w(ctx,g)@315` (main read), `a(ctx)Z@809` (stake), `b(ctx,g)@823`, `u(ctx,int)@704` (**). Each card VoLTE valuer**, is `:906` call).
- **uses API / hidden side**
  - `new FileReader("/proc/meminfo")` (`:18`) → parse `MemTotal:`, etc. → normalize to `256M/512M/768M/1G/2G/3G/4G/8G/16G/32G/64G`.
  - `new StatFs(...)` → `getBlockSize()*getBlockCount()` (`:200-214`) → ROM capacity.
  - `getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getMsisdn`、`getNetworkOperatorForSubscription`、`getNetworkType`、`getVoiceNetworkType`。
  - Attributes `persist.sys.hardware.version` (`:868`), `persist.sys.update.version` (`:858`), `ro.yunos.version`/`ro.yunos.name`.
- **write keys** (22): `g,i,h,j,k,l`, `m,n` (from prefs `CellID0`/`LAC0`), `r,t,s,u,v,w`, `x,y`, `b,a,e,f,d,c`.
- **Base calls**: `e@334`, `g@553`.
- **trigger/switch**: manufacturer `SOP` (`e0/c2.smali:781`); model `SOP-W1`, `SOP-W2`.
- **privacy**: **P4**; additional collection **RAM/ROM capacity** (device fingerprint enhancement).

### 3.13 `e0/q` — Synthetic Bridge: `CellIdentityTdscdma.getLac()`

- **smali**：`invoke-virtual {p0}, Landroid/telephony/CellIdentityTdscdma;->getLac()I`。
- **caller**: `e0/r.smali:2795`.
- **semantics**: TD-SCDMA location area code (LAC).
- **Privacy**: No direct output (participating in P3).

### 3.14 `e0/q0` — lephone collector `DeviceInfoReaderLEPHONE` (**five sets of reading**)

- **method structure**: `t(TM,g,int)@15` (**`getAllCellInfo` Analyze**), `u(int)Z@185` (`isValidSubscriptionId`), `v(ctx,g)@249`, `w(ctx,g)@700`, `x(ctx,g)@1151`, `y(ctx,g)@1665`, `z(ctx,g)@2179`, `a(ctx)Z@2695` (pile), `b(ctx,g)@2709`.
- **uses API / hidden side**
  - **`getAllCellInfo()`** (`:32`) → `List<CellInfo>`, traverse to get the serving cell → `x/y`.
  - `getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1NumberForSubscriber`、`getNetworkOperatorForSubscription`、`getNetworkType`、`getVoiceNetworkType`、`getMsisdn`、`getCellLocationForPhone`。
  - Properties: `ro.sw_version` (`:2840`), `ro.hw_version` (`:2842`), **`persist.sys.hardware_version`** (`:2969`) , `ro.hcthardware.version.number` (`:3046`), `ro.custom.version.release` (`:3054`), `ro.build.display.id` (`:3084`), `ro.yunos.version`, `ro.product.hardware`.
- **write keys** (22): `m,x,n,y` (cell writes first), `g,i,h,j,k,l`, `r,t,s,u,v,w`, `b,a,e,f,c,d`.
- **Base calls**: `e@274`, `g@483`, `p@3008`, `q@3100` (finally falls back to the base class dual-channel full read).
- **Trigger/Switch**: Manufacturer `lephone` (`e0/c2.smali:706`); Model `lephone A7/A9/A10/A12/A12note/F5W/F5WPro/V8/W5/W6/W6Pro/W7/W7+/W9/W9+`.
- **Privacy**: **P4**.

### 3.15 `e0/q1` — Samsung collector `DeviceInfoReaderSamSung` (the most professional for 5G/VoLTE)

- **field**: `final b:I@7`, `final c:I@9` (constant during construction, suspected to be "slot 1/2" or API constant; **'s specific semantics are unknown**). Construct `<init>@13`.
- **method structure**: `t(ctx)Z@29` (boot wizard), `static v(ctx)J@80` / `static w(ctx)JSONObject@101` (memory), `x()Z@184` (**isSupport5GConcept**), `static y(ctx)J@275` / `static z()J@503` (DRAM), `A(ctx,g)@619`, `B(ctx,g)@1464`, `b(ctx,g)@2468`, `u(ctx,int)String@2647` (**per card VoLTE valuer**, 6 calls: `:1021/1222/1429/1906/2187/2436` → Write `o`/`z`).
- **uses API / hidden side**
  - **`TelephonyManager.getTelephonyProperty(...)`** (`:665-707`) reads `ril.hw_ver`, `ro.build.PDA`; `B()` reads `ril.sw_ver` (`:1524`).
  - **`createForSubscriptionId(subId)`** (`:1614-1698`) - Get the `TelephonyManager` bound to the subId and then read `getSubscriberId/getSimSerialNumber/getMsisdn/getSimOperator`.
  - **`getCellLocationBySubId`**（`:833,1602`）。
  - `getImei`、`getNetworkType`、`getSimOperatorNumeric`、`getLine1Number`。
  - **`Settings$System.getInt(cr,"voicecall_type"/"voicecall_type2",-1)`** (`:2671`) → Samsung VoLTE switch.
  - Attributes: `ro.telephony.default_network` (`:192`, `Integer.parseInt` followed by `>=30` → supports 5G concept), `ro.product.first_api_level` (`:1596`), `ro.boot.dram_info` (`:506`).
- **write key** (25 ): `c,d` (write the version first), `B`, `g,i,h,j,k,l`, `o`, `r,t,s,u,v,w`, `z`, `p`, `A`, `b,a,e,f,F,G`; the cell base class `r()` falls to `d0/g.m/n/x/y`.
- **Base calls**: `h@877`, `g@1015`, **`j@1768`** (5G: `preferred_network_mode{slot}` ∈ [23,33]), **`r@1842`** (cell analysis).
- **Trigger/Switch**: Manufacturer `samsung` (`e0/c2.smali:1085`); Model `SM-G9750/SM-N9700/SM-N9760/SM-A5070/SM-A9080/SM-T735C/SM-X205C`.
- **downgrade/side effect**: `isSupport5GConcept` captures `NumberFormatException` (`:251`); `p` is covered by `iput` twice consecutively (`:1774`, `:1779`), the latter.
- **privacy**: **P4**; additional exposure **PDA / ril hardware version, first version API level, Samsung voicecall_type settings**.

### 3.16 `e0/r0` — LETV / LeTV collector `DeviceInfoReaderLETV` (**five sets of reading methods**)

- **method structure**: `t(ctx,g)@15` (main reading), `a(ctx)Z@522` (stub), `b(ctx,g)@536`, `q(ctx,g)@882`, `u(ctx,g)@1246` (MTK `TelephonyManagerEx`), `v(ctx,g)@1654` (three sets of parallel reading methods).
- **Use API / hidden face**: `getImei`, `getSubscriberId`, `getSimSerialNumber`, `getLine1NumberForSubscriber`, `getSimOperator`, `getNetworkType`, `getVoiceNetworkType`, **`getCellLocationFromSlotId`** ( `:106,270,500,920,1692` ); **`com.mediatek.telephony.TelephonyManagerEx.getDefault()`** ( `:1267-1287` ) + its `getCellLocation` ( `:1314,1457,1630` ).
- **Properties**: **`ro.product.letv_model`** (`:569`), `ro.build.display.id` (`:571`), `ro.config.le_hardware_version` (`:610`), `ro.product.hardware.version` (`:682`), `ro.config.hardware_version` (`:727`), `ro.hardware.version` (`:818`).
- **writing keys** (18): `g,i,h,j,k,l`, `r,t,s,u,v,w`, `b,a,e,f,d,c`.
- **Base calls**: `e@44`, `g@263`, **`p@597`**, `f@1033`.
- **trigger/switch**: manufacturer `LETV` (`e0/c2.smali:421`); models `Le X525/X620/X820`, `Letv X500`, `LEX555/622/626/652/659`.
- **Privacy**: **P4**.

### 3.17 `e0/r1` — Spreadtrum / Spreadtrum collector

- **method structure**: `t(int)I@15` (reflection `getSubId`), `u(int)Z@73` (`isValidSubscriptionId`), `v(ctx,g)@131` (main read, **`phone_ex` Channel**), `a(ctx)Z@607` (pile), `b(ctx,g)@621`.
- **uses API / hidden side**: `getSystemService("phone_ex")` (`:154`) forced transfer `TelephonyManagerEx`; `getDeviceId`, `getSubscriberId`, `getSimSerialNumber`, **`getMsisdn`**, `getNetworkOperatorForPhone`, `getVoiceNetworkType`, `getCellLocationForPhone` (`:190-599`).
- **write key** (18): `g,i,h,j,k,l`, `r,t,s,u,v,w`, `b,a,e,f,d,c`; `c` = `ro.product.hardware` (`:660`).
- **Base calls**: `g@360`.
- **Trigger/Switch**: Manufacturer `Spreadtrum` (`e0/c2.smali:826`); Model `SP9830A`.
- **downgrades**: the whole section `try/catch`; the end of `b()` `DeviceRegister` / `DeviceInfoReaderSpreadtrum module unknow!!!`.
- **Privacy**: **P4**.

### 3.18 `e0/s` — Bee collector `DeviceInfoReaderBee` (`getAllCellInfo` analysis)

- **method structure**: `<init>@15` (read `ro.sf.lcd_density`), `t(int)String@45`, `u(TM,g,int)@69` (**CellInfo List parsing**), `v(String,String)String@239` (universal reflection), `w(int)Z@300` (`isValidSubscriptionId`), `x(ctx,g)@358`, `y(ctx,g)@809`, `b(ctx,g)@1325`.
- **uses API / hidden surface**: **`getAllCellInfo()`** (`:86`) → traverse `List<CellInfo>` and write after hitting the service cell `x`/`y` (`:214,231`); `getDeviceId`, `getSubscriberId`, `getSimSerialNumber` , `getLine1NumberForSubscriber`, `getNetworkOperatorForSubscription`, `getNetworkType`, `getVoiceNetworkType`, `getMsisdn`, `getCellLocationForPhone`.
- **attributes**: `ro.sw_version` (`:1364`), `ro.hw_version` (`:1372`), `ro.build.display.id`, `ro.product.hardware`, `ro.yunos.version`, `ro.yunos.name`.
- **write keys** (22): `m,x,n,y`, `g,i,h,j,k,l`, `r,t,s,u,v,w`, `b,a,e,f,d,c`.
- **Base calls**: `e@383`, `g@592`.
- **trigger/switch**: manufacturer `Bee` (`e0/c2.smali:890`); model `bee E7S`, `bee X9`.
- **Privacy**: **P4**.

### 3.19 `e0/s0` — Lenovo collector `DeviceInfoReaderLenovo` (**only uses SIM1**)

- **method structure**: `A(ctx,g)@15` (main read), `t(ctx)Z@265` (`Settings$Global.getInt` + `checkUserPermission:`), `v()@309` / `w()@321` / `x()@333` / `y()@345` (four attributes getter), `z(ctx,g)@357` (**public API fallback**: directly use `TelephonyManager.getDeviceId/getSubscriberId/getSimSerialNumber/getLine1Number/getNetworkOperator/getNetworkType/getVoiceNetworkType/getCellLocation`), `b(ctx,g)@428`, `u(ctx,int)@704` (**per card VoLTE value getter**, was used by `:255` Call → write `o@259`).
- **Use API / hidden surface**: `isValidSubscriptionId` (`:73`), `getDeviceId`, `getSubscriberId`, `getSimSerialNumber`, `getLine1Number`, `getSimOperator`, `getNetworkType`, `getVoiceNetworkType`, `getCellLocation` ; `SubscriptionManager.getDefaultDataSubscriptionId()` + `getPhoneId` ( `:465-536` ).
- **write key** (16, **no card 2 SIM field**): `g@62 i@98 h@128 j@150 k@172 l@231 o@259`, `b@439 a@449 e@453 f@457`, `F@463 B@565 G@599`, `d@603 c@609`.
- **Base calls**: `h@64`, `g@227`.
- **trigger/switch**: manufacturer `Lenovo` (`e0/c2.smali:1055`); model `Lenovo TB-8505N`, `TB-X306NC_PRC`, `TB-X505N`, `TB-X605M` (**flat panel line**, so it is treated as a single card); operator key `agree_unicom`.
- **Privacy**: **P3+** (single card IMEI/IMSI/ICCID/MSISDN, no cell).

### 3.20 `e0/s1` — TCL collector `DeviceInfoReaderTCL` (**and y/z1 have the same origin as**)

- **method structure**: `t(ctx)Z@15` (boot wizard), `v(ctx,g)@66` (main read), `b(ctx,g)@656`, `u(ctx,int)@914` (**per card VoLTE value taker**, `:362`, `:618` call → write `o`/`z`).
- **Use API / hidden surface**: **`ColorOSTelephonyManager.getDefault()`** (`:89-125`); `getImei`, `getSubscriberId`, `getSimSerialNumber`, `getLine1Number`, `getNetworkOperator`, `getNetworkType`, `getVoiceNetworkType`, `getCellLocation`; `isOemEnhanced4gLteModeSettingEnabledByUser` (`:965`).
- **attributes**: `ro.build.display.id` (`:829`), `ro.board.hardware_version` (`:837`), `ro.tcl.hardware.version` (`:870`), `ro.mtk.build.version` (`:880`).
- **write keys** (23): `g@151 i@213 h@249 j@273 k@297 l@360 o@366`, `r@437 t@471 s@505 u@529 v@553 w@616 z@622`, `b@667 a@677 e@681 f@685`, `F@691 B@793 G@827`, `d@835 c@843`.
- **Base calls**: `h@153`, `g@356`, **`l@849`**, `p@888`.
- **trigger/switch**: manufacturer `TCL` (`e0/c2.smali:736`); model `TCL P561U`, `F810`.
- **Privacy**: **P4**.

### 3.21 `e0/t` — BenWee collector `DeviceInfoReaderBenWee`

- **method structure**: `t(ctx,g)@15` (main read, **`MSimTelephonyManager` single channel**), `a(ctx)Z@408` (stub), `b(ctx,g)@422`.
- **uses API / hidden surface**: `Class.forName("android.telephony.MSimTelephonyManager")` + `getDefault()` (`:22-46`), which is subsequently reflected on that instance `getDeviceId`, `getSubscriberId`, `getSimSerialNumber`, `getLine1Number`, `getNetworkOperator`, `getVoiceNetworkType`, `getCellLocation` (`:62-394`) - **all fields are taken from the MSim instance**.
- **writing keys** (18): `g,i,h,j,k,l`, `r,t,s,u,v,w`, `b,a,e,f,d,c`.
- **Base calls**: `g@208`, `p@465`.
- **trigger/switch**: manufacturer `BenWee` (`e0/c2.smali:526`); models `BenWee L5`, `BenWee V9`, `BenWee_L5`, `V6`.
- **Privacy**: **P4**.

### 3.22 `e0/t0` - "Liantong" (customized by China Unicom) collector `DeviceInfoReaderLiantong` (**is the only class** that overwrites `j()`)

- **method structure**: `t(ctx,g)@15` (**permission access + ColorOS**), `u(ctx)Z@360` (boot wizard), `w(ctx,g)@411`, `x(ctx,g)@999`, `y(ctx,g)@1441`, `b(ctx,g)@2124`, **`j(ctx,int)Z@2459` (overwriting base class 5G Determination)**, `v(ctx,int)String@2547` (**VoLTE value taker** per card, 4 calls `:707/963/1829/2111` → write `o`/`z`).
- **uses API / hidden side**
  - Permission access control: `android.permission.READ_PHONE_NUMBERS`/`READ_PHONE_STATE` (`:18-36`), log `readAndroidR no READ_PHONE_STATE PERMISSION_GRANTED`.
  - `ColorOSTelephonyManager.getDefault()`（`:434-470`）、`getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getNetworkOperator`、`getNetworkType`、`getVoiceNetworkType`、`getCellLocation`、`getNetworkOperatorForPhone`、`getSimOperatorNumericForPhone`。
  - **`Settings$Global.getInt(cr,"preferred_network_mode{slot}",-1)`** (`:2508`) - `j()` is 5G.
  - `isOemEnhanced4gLteModeSettingEnabledByUser`（`:2598`）。
  - Attributes: `ro.build.display.id` (`:2291`), `ro.board.hardware_version` (`:2293`).
- **write key** (25 ): `g@59`, `r@89`, `i@127 h@141 j@147 k@153`, `p@161`, `l@171`, `o@228`, `t@251 s@265 u@271 v@277`, `A@287`, `w@297`, `z@354`, `b@2135 a@2145 e@2149 f@2153`, `F@2159 B@2261 G@2342`, `d@2348 c@2354`.
- **Base calls**: `h@97`, `r@173`, **`c@216`**, **`d@223`** (→ VoLTE Read + "Off turns on" side effects), `g@701`.
- **trigger/switch**: manufacturer `Liantong` (`e0/c2.smali:1207`); model `S10`, `VP001/VP003/VP004/VP005`.
- **privacy**: **P4**; additionally exposed “China Unicom customized machine” channel fingerprint.

### 3.23 `e0/t1` — TDTECH (Dingqiao) collector `DeviceInfoReaderTDTECH` (**Huawei series 5G surface**)

- **field**: `b TelephonyManager@7`, `c:I@9`, `d:I@11`, `e/f/g/h:String@13-19` (instance cache).
- **method structure**: `t(ctx,g)@51` (**HwTelephonyManager determines VoLTE/5G**), `u(ctx)Z@468` (boot wizard), `static v(ctx)J@519` / `static w(ctx)JSONObject@540` (memory), `x(ctx,g)@623` (main read, called by `b()` in `:2146`), `static y(ctx)J@1497` / `static z()J@1725` (DRAM), `b(ctx,g)@1841`.
- **uses API / hidden side**
  - **`android.telephony.HwTelephonyManager.getDefault()`**（`:54-74, 686-702`）+ **`getServiceAbility`**（`:163,754,884`）+ **`isVolteAvailable`**（`:770`）。
  - `android.telephony.MSimTelephonyManager`（`:646`）+ `getSubscriberId`/`getSimSerialNumber`/`getLine1Number`/`getNetworkOperator`。
  - **`e0.i0.a(SubscriptionManager,int)`** Get the subId array (`:1892,1898`), log `subId0:` / `subId1:`.
  - Attributes: `ro.product.hardwareversion` (`:2129`), `ro.boot.dram_info` (`:1728`). Network type determination string: `NR` / `SA` / `NSA` / `LTE`.
- **write key** (29 ): `g@119 r@147`, `i@185 h@199 j@205 k@211 l@215`, `o@272`, `p@311`, `t@334 s@348 u@354 v@360 w@366`, `z@423`, `A@462`, `m@1066 n@1070 x@1459 y@1463`, `b@1852 a@1862 e@1866 f@1870`, `F@1876 B@2038 G@2115`, `d@2127 c@2135`.
- **Base calls**: `h@155`, `r@217`, **`c@260`**, **`d@267`**, `g@929`.
- **trigger/switch**: manufacturer `TDTECH` (`e0/c2.smali:1237`); models `TDT-MA01`, `RAI-AN01`, `AVA-PA00`.
- **privacy**: **P4**; additional exposure **VoLTE availability, 5G SA/NSA capability, DRAM capacity**.

### 3.24 `e0/u` — BestSonny collector

- **method structure**: `t(int)I@15` (`getSubId`), `u(int)Z@73` (`isValidSubscriptionId` ), `w(ctx,g)@131` (primary reading), `a(ctx)Z@581` (stake), `b(ctx,g)@595`, `v(ctx,g)@772` (second reading, `getCellLocation` direct connection).
- **uses API / Hidden surface**: `getDeviceId`, `getSubscriberId`, `getSimSerialNumber`, `getMsisdn`, `getNetworkOperatorForPhone`, `getVoiceNetworkType`, `getCellLocationForPhone`, `getCellLocation` (`:136-1158`).
- **writing keys** (18): `g,i,h,j,k,l`, `r,t,s,u,v,w`, `b,a,e,f,d,c`.
- **Base calls**: `g@350`, `f@945`.
- **Trigger/Switch**: Manufacturer `BestSonny` (`e0/c2.smali:766`); Model `Best sonny LT580/LT581/LT587/LT680/LT681`.
- **Privacy**: **P4**.

### 3.25 `e0/u0` — Meizu collector `DeviceInfoReaderMEIZU`

- **method structure**: `t(ctx)Z@15` (Boot Wizard + `Settings$Secure` + `checkUserPermission:`), `u(int)I@105` (`getSubId` + **`getDefaultSubscriptionId`**), `v(ctx,g)@174` (primary read, **`MzTelephonyManager`**), `b(ctx,g)@671`.
- **uses API / hidden surface**: `Class.forName("android.telephony.MzTelephonyManager")` (`:197`) + this example `getCellLocation` (`:420,647`); `getImei`, `getSubscriberId`, `getSimSerialNumber`, `getLine1Number`, `getNetworkType`, `getVoiceNetworkType`, **`getSimOperatorNumericForPhone`** (`:275`); `getDefaultDataSubscriptionId()` + `getPhoneId` (`:708-750`).
- **write keys** (21): `g,i,h,j,k,l`, `r,t,s,u,v,w`, `b,a,e,f`, `F@706 B@780 G@837`, `d,c`.
- **Base calls**: `g@410`.
- **trigger/switch**: manufacturer `meizu` (`e0/c2.smali:511`); model `M926Q`, `meizu 16Xs`; operator key `operator_service_improve_plan`.
- **Privacy**: **P4**.

### 3.26 `e0/u1` — vivo collector `DeviceInfoReaderVIVO` (**is the deepest in the entire project**)

- **static constant (plain text)**: `c = "persist.vendor.radio.5g_mode_pref"`, `d = "persist.radio.vivo.fiveg"` (`<clinit>@17-40`) - two **5G switch attribute keys** are promoted to constants.
- **Method structure**: `A(String,String)Object@55` (universal reflection), `G()Z@123` (**isUnicomEntry**: read `ro.vivo.op.entry`, including `UNICOM` Judgment), `H(ctx,g)@172` (main read), `t(TM,g,int)@844` (cell read), `u(ctx)Z@1028` (**isUserPermissionOpen**), `w(ctx,int)String@1118` (**5G Status**), `y()@1241`, `z()@1266`, `B()@1292`, `C(ctx,String,HashMap)@1325`, `D(ctx,String,String,int)@1580`, `E()@1853`, `F(ctx)Z@1914`, `a(ctx)Z@1988` (stake), `b(ctx,g)@2002`, `v(ctx,int)String@2448` (VoLTE, `:485`, `:802`), `x(ctx,int)String@2529` (5G switch, called by `:1181`).
- **uses API / hidden surface (key point)**
  - **`getITelephony()`** (`:1367,1635`) → Structure **`com.android.internal.telephony.VivoTelephonyApiParams`** (`:1383,1651`) → **`vivoTelephonyApi(...)` / `getParamValues(...)`** (`:1497,1521,1759,1783`) - Directly read vivo's private 5G/NR parameter table.
  - Parameter labels: `API_TAG_getNrCapability` (`:1927`), `API_TAG_getNRSwitchState` (`:2532`), `API_TAG_isVolteEnable` (`:2451`); `isDualMode5G: `, `the 5G attribute:`, `get5GSwitchState: `, `get5GState: ----switchState:`.
  - Attributes: `persist.radio.vivo.fiveg`, `persist.vendor.radio.5g_mode_pref`, `ro.vivo.op.entry`, `ro.vivo.op.entry.version`, `ro.vivo.product.version`, `ro.vivo.hardware_version`.
  - `getImei`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getNetworkType`、`getVoiceNetworkType`、`getCellLocation`、**`getSimOperatorNumericForPhone`**、`getNetworkTypeForSubscription`。
  - `getDefaultDataSubscriptionId()` + `getPhoneId` (`:2062-2133`); Carrier key `operator_service_improve_plan`; `dual_nr_support`.
- **Write key**(29 ): `g@229 i@313 h@341 j@361 k@381`, `p@393`, `l@404`, `o@412`, `r@569 t@621 s@653 u@677 v@697`, `A@709`, `w@720`, `z@728`, `m@996 x@1001 n@1015 y@1020`, `b@2024 a@2034 e@2038 f@2042`, `c@2048 d@2054`, `F@2060 B@2162 G@2192`.
- **Base calls**: `h@231`, `r@406`, **`d@408`**, `g@479`.
- **trigger/switch**: manufacturer `vivo` (`e0/c2.smali:841-849` takes the `equalsIgnoreCase` branch); model number `V1901A…V1945A`, total 20+, `MP_0.1`. **hard access control**: `if (!isUserPermissionOpen) { log "isUserPermissionOpen==false, do not read!"; return; }` (`:2011-2015`) within `b()` - **, the only true "rejection of acquisition" branch among the 41 classes**.
- **Privacy**: **P4 +** (full dual-SIM, cell, VoLTE, 5G SA/NSA/dual-mode, Unicom customized identity).

### 3.27 `e0/v` — CHANGHONG collector `DeviceInfoReaderCHANGHONG` (**is the shortest implementation of**)

- **method structure**: `a(ctx)Z@17` (stake), `b(ctx,g)@31` (**all logical**).
- **uses API / hidden surface**: **does not apply for any system service**; only `f0.l.j(...)` (`SystemProperties.get(k,def)`) is used.
- **write keys (6 by itself)**: `b Model@36`, `a Manuf@46`, `e OS@50`, `f OSVersion@54`, `c HWVersion@74`, `d SWVersion@82`.
  - `c = ro.denqin.hardware.version` (`:66`), `d = ro.denqin.version` (`:76`), then **`f = ro.yunos.version` (`:84`) covers OSVersion, `e = ro.yunos.name` (`:92`) covers OS**.
- **Base calls**: **`p@102`** - that is, `Le0/r.p(ctx,g)` dual-card reflection full read, so this class actually also outputs `g…w` and all SIM fields (written in `r.smali`).
- **Trigger/Switch**: Manufacturer `CHANGHONG` (`e0/c2.smali:860`); Model `Changhong W01`.
- **Privacy**: **P4** (it can only read 6 version fields, but `base.p()` completes the dual card identification).

### 3.28 `e0/v0` — Synthetic Bridge: `SubscriptionInfo.getMncString()`

- **smali**：`invoke-virtual {p0}, Landroid/telephony/SubscriptionInfo;->getMncString()Ljava/lang/String;`
- **calls**: `e0/w0.smali:535`, `e0/w0.smali:701` (two slot branches of Motorola collector).
- **semantics**: Return **MNC in string form** (only available since API 29; the old version `getMnc()` is int, leading 0 will be lost). Used to write `MNC1/MNC2` into a fixed-length string such as `"00"/"01"`.
- **Privacy**: Indirect output `MNC1/MNC2` (carrier identification).

### 3.29 `e0/v1` — WIKO / PTAC collector (same origin as `t1`, **double tag**)

- The difference between **and `t1`** (normalized row set Jaccard 57.8%, the difference is concentrated):
  1. One more **directly connects to the public API and falls back to**: directly `TelephonyManager.getSubscriberId()/getSimSerialNumber()/getLine1Number()/getNetworkOperator()`, and then normalized by `Le0/r;->f(tm,slot)` + `Le0/r;->g(netType,operator)`.
  2. During reflection, **variable length parameter array**: `new-array v6, v12, [Ljava/lang/Class;` (`v12` is determined by the string length) - `getMethod(name, Class[])` passes N parameters.
  3. The network type string is expanded to `NR`/`SA`/`NSA`/`LTE` four states, mapping `"1"/"0"`.
  4. Reserve `HwTelephonyManager.getServiceAbility` + `isVolteAvailable`.
- **method structure**: `t(ctx,g)`, `u(ctx)Z`, `static v/w` (memory), `x(ctx,g)` (main read, by `b()` in `:2062` Call), `static y/z` (DRAM), `b(ctx,g)@1815`.
- **write key** (29, completely isomorphic with `t1`): `g@83 r@111 i@149 h@163 j@169 k@175 l@187 o@244 p@283 t@306 s@320 u@326 v@332 w@344 z@401 A@440 m@1040 n@1044 x@1433 y@1437 b@1826 a@1836 e@1840 f@1844 F@1850 B@1952 G@2031 d@2043 c@2051`.
- **Base Call**: `h@119`, `f@177`, `g@183`, `r@189`, **`c@232`**, **`d@239`**.
- **trigger/switch**: manufacturer `WIKO` (`e0/c2.smali:1282`) and `PTAC` (`:1252`, `FIO-BD00/Hebe-BD00/Hera-BD00/MGZ-BD00/ALH-BD00/MIZ-BD00/MRK-BD00`, a long list of model numbers mapped to PTAC); model number `GAR-AN00`, `LGN-AN00`, `GAR-AN60`.
- **Privacy**: **P4 +** (same level as `t1`).

### 3.30 `e0/w` — ChuiZi (hammer/nut) collector `DeviceInfoReaderChuiZi`

- **method structure**: `t()String@15` (model determination, including `ro.product.model`), `u()String@77` (`ro.smartisan.version`), `v(ctx,g)@125` (main reading), `b(ctx,g)@609`.
- **Use API / hidden surface**: `getImei`, `getSubscriberId`, `getSimSerialNumber`, `getLine1Number`, `getNetworkOperator`, **`getNetworkOperatorForPhone`**, `getNetworkType`, `getVoiceNetworkType`, `getCellLocation`, **`getSimOperatorNumericForPhone`**; `d0/g.f()/g()` directly feed `CellLocation` (`:379,601`).
- **writing keys** (18): `g,i,h,j,k,l`, `r,t,s,u,v,w`, `b,a,e,f,d,c`.
- **Base calls**: `h@184`, `g@355`.
- **trigger/switch**: manufacturer `ChuiZi` (`e0/c2.smali:905`); model `OD101/OD103/OD105`, `MMR500030/MMR500032`; system identification `ro.smartisan.version`.
- **Privacy**: **P4**.

### 3.31 `e0/w0` — Motorola / ZUI collector `DeviceInfoReaderMOTOl`

- **method structure**: `t(ctx)Z@15` (boot wizard), `static u(ctx)J@66` / `static v(ctx)JSONObject@87` (memory), `static w(ctx)J@170` (RAM value), `x(ctx,g)@398` (main read, `b()` in `:1101` Call), `static y()J@806` (**`ro.boot.dram_info` parses**), `b(ctx,g)@922`.
- **uses API / hidden side**
  - Permission access control `READ_PHONE_NUMBERS` / `READ_PHONE_STATE` (`:407-425`), log `readAndroidR no READ_PHONE_STATE PERMISSION_GRANTED`.
  - **`SubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(slot)`** (`:503,669`) → `SubscriptionInfo`, then use `e0/v0` bridge to get `getMncString()` and write `o/p/z`.
  - `getDefaultDataSubscriptionId()` + `getPhoneId`（`:959-1030`）。
  - Attributes: `ro.zui.software.display` (`:1085`), `ro.product.model` (`:1093`), `ro.boot.dram_info` (`:809`).
- **write key** (25): `g@454 r@460 i@511 h@525 j@531 k@539 p@548 l@558 o@620 t@677 s@691 u@697 v@705 A@714 w@724 z@800 b@933 a@943 e@947 f@951 F@957 B@1059 G@1083 d@1091 c@1099`.
- **Base Call**: `h@463`, **`j@542`** (5G), **`r@566`** (cell), **`c@616`**, **`d@631`** (VoLTE).
- **trigger/switch**: manufacturer `motorola` (`e0/c2.smali:1010`); model whitelist includes `XT2335-3`, etc.; ZUI system identification.
- **Privacy**: **P4 +** (VoLTE/5G + RAM).

### 3.32 `e0/w1` — Xiaodu (Xiaodu) collector `DeviceInfoReaderXiaodu` (**has the most complete image storage**)

- **method structure**: `A()String@15` (`ro.build.version.software_version_display`), `static B(ctx)JSONObject@40` (storage summary JSON), `static t(J)I@108` (`analyseTotalStorage(), total: `), `u(ctx)Z@152` (`persist.sys.cucc.dm` + `device_provisioned` + `checkUserPermission:`), `static v()String@209` (**`Environment.getDataDirectory()` + `StatFs.getBlockSizeLong()*getAvailableBlocksLong()`**), `static w(File)J@311` (`getBlockSize()*getBlockCount()`), `x()String@339` (`ro.boot.hardware_version`, replaced by `:1042` call), `static y(ctx)String@364`, `static z()String@424` (**root partition + data partition capacity**), `C(ctx,g)@494` (primary read), `b(ctx,g)@1019`.
- **uses API / hidden surface**: `getVoiceNetworkType`, `getNetworkType`, **`getNetworkOperatorForPhone`**, `getMsisdn`, `getSimSerialNumber`, `getSubscriberId`; attributes **`ro.boot.5GEnable1` (`:727`) and `ro.boot.5GEnable2` (`:935`)** → Directly write `p`/`z`/`o`; `persist.sys.cucc.dm` (`:155`, China Unicom DM Customized mark); log `subId0:`/`，subId1:`/`, activeDataSubId：`.
- **write key** (25): `G@524 g@532 i@580 h@612 j@636 k@660 l@723 p@733 o@739 r@752 t@788 s@820 u@844 v@868 w@931 A@941 z@947 B@999 b@1030 a@1040 c@1046 d@1052 e@1056 f@1060 F@1066`.
- **Base calls**: `h@534`, `g@719`, `r@725`, **`d@735`**.
- **trigger/switch**: manufacturer `xiaodu` (`e0/c2.smali:1297`); model `G`, `MP_0.1`.
- **Privacy**: **P4 +** (full capacity of dual cards + VoLTE/5G + **total capacity/available capacity/system partition/data partition**).

### 3.33 `e0/x` — Synthetic Bridge: `SubscriptionManager.getSlotIndex(subId)`

- **smali**：`invoke-static {p0}, Landroid/telephony/SubscriptionManager;->getSlotIndex(I)I`
- **caller (26 places, one of the most commonly used bridges in the entire project)**: `e0/a1.smali:721`, `e0/b2.smali:1205`, `e0/c0.smali:426`, `e0/c1.smali:1165`, `e0/e0.smali:413`, `e0/f1.smali:577`, `e0/g0.smali:1751`, `e0/j0.smali:1897`, etc.
- **semantics**: `subId` → **physical card slot subscript** (0/1). It is the basis for the attribution of all `xxx1/xxx2` dual card fields.
- **Privacy**: Indirectly determines the correspondence between the dual card fields and the card slot.

### 3.34 `e0/x0` — mfox collector `DeviceInfoReaderMfox` (**MTK attribute double IMEI**)

- **method structure**: `a(ctx)Z@17` (stub), `b(ctx,g)@31` (version field), `t(ctx,g)@117` (main read).
- **uses API / hidden side**
  - **`SystemProperties.get("gsm.mtk.imei1")` / `("gsm.mtk.imei2")`** (`:131,312`) - MTK platform-specific dual IMEI properties, bypassing `getDeviceId`.
  - `Le0/d2.b/c(TM,name,subId)`：`getSubscriberId`、`getSimSerialNumber`、`getLine1NumberForSubscriber`、`getNetworkOperator`↔`getNetworkOperatorForSubscription`（`:155-416`）。
  - Property `ro.product.brand` (`:66`).
  - `CellID0/LAC0`, `CellID1/LAC1` read back from **SharedPreferences** (`:288-302, 455-467`).
- **write keys** (22): `b@36 a@46 e@50 f@54 c@74 d@78`, `g@137 i@178 h@209 j@230 k@251 l@276 m@294 n@302`, `r@318 t@343 s@374 u@395 v@416 w@441 x@459 y@467`.
- **Base calls**: `f@266` (network type), `g@272` (NCLS).
- **Trigger/Switch**: Manufacturer `mfox` (`e0/c2.smali:965`); Model `A-10`.
- **Privacy**: **P4**.

### 3.35 `e0/x1` — Small pepper collector `DeviceInfoReaderXiaolajiao` (**model branch is the most broken**)

- **method structure**: `t(int)I@15`, `u(int)Z@73`, `v(ctx,g)@131`, `w(ctx,g)@562`, `x(ctx,g)@1001` (replaced by `b()` in `:2602` Call), `y(ctx,g)@1506`, `z(ctx,g)@1954`, `a(ctx)Z@2404` (stake), `b(ctx,g)@2418`, `p(ctx,g)@2882` / `q(ctx,g)@2912`.
- Key Differences (Important) from **`b()`**: `g.a` (Manuf) **is not** `Build.MANUFACTURER`, but `e0/c2.a.get(Build.MODEL)` The manufacturer string obtained by looking up the table (`e0/x1.smali:2425-2433`); then do **20+ times for `Build.MODEL` `equals` Branch** (`20150204W`, `20150718`, `20150818`, `20160202W`, `20160212W`, `20160212W-V1.0`, `20160526Q`, `20160727Q`, `20160926Q`, `20161101Q`, `20170212`...), choose different attribute combinations for each one. `c/d/f/e`; and finally `p()` or `q()`.
- **uses API / hidden surface**: `getImei` (with `slot` parameters), `Le0/r.e(slot)` takes subId, `getNetworkOperatorForPhone`, `getSimOperator`, `getSubId`; attributes `ro.build.display.hardware.id`, `ro.build.display.hwid`, `ro.build.version.external`, `ro.product.yuxinhardwareversion`, `ro.product.yuxinosname`, `ro.product.yuxinsoftwareversion`, `ro.xh.display.version`, `ro.hardware.version`, `ro.build.display.id`.
- **write keys** (22): `g@194 i@220 h@268 j@292 k@312 l@346 m@356 n@364`, `r@400 t@426 s@460 u@484 v@504 w@538`, `x@548 y@556`, `b@2423 a@2433 e@2437 f@2441 d@2492 c@2500`.
- **Base calls**: `i@160` (`getSubId`), `g@342`, `e@1030`, `p@2882`, `q@2912`.
- **trigger/switch**: manufacturer `Xiaolajiao` (`e0/c2.smali:995`); models `GM-W`, `JD-PLUS` and the above version number string; **model** that is not in the whitelist will return null due to `c2.a.get()` `g.a` is empty (it is filled in by `d0/g.e()` and `"Unknown"` when reporting).
- **Privacy**: **P4**; additionally exposes the strong fingerprint of "YunOS/Little Pepper customized firmware version number".

### 3.36 `e0/y` — Coolpad collector `DeviceInfoReaderCoolpad` (**and s1/z1 have the same origin as**)

- **method structure**: `t(ctx)Z@15`, `v(ctx,g)@66`, `b(ctx,g)@656`, `u(ctx,int)@914` (VoLTE valuer, called by `:362`/`:618`).
- The difference between **and `s1`** (Jaccard 97.3%, only 3 places): log tag `DeviceInfoReaderCoolpad`; model whitelist `CP03`; attributes only use `ro.board.hardware_version` (no more trying `ro.tcl.*`/`ro.mtk.*`).
- **additional defect**: The "unknown model" branch log of `b()` is written as **`DeviceInfoReaderOPPO module unknow!!!`** (copy residue), and there is a structural difference of `goto :goto_1`.
- **write key** (23): `g@151 i@213 h@249 j@273 k@297 l@360 o@366 r@437 t@471 s@505 u@529 v@553 w@616 z@622 b@667 a@677 e@681 f@685 F@691 B@793 G@827 d@835 c@843`.
- **Base calls**: `h@153`, `g@356`, **`l@849`** (VoLTE side effect path).
- **Trigger/Switch**: Manufacturer `Coolpad` (`e0/c2.smali:1177`).
- **Privacy**: **P4**.

### 3.37 `e0/y0` — BROR / NOAIN collector (**only Binder/AIDL direct adjustment**)

- **method structure**: **`static t(Context,int)String@15`** (AIDL direct transfer IMEI), `u(int)Z@129` (`isValidSubscriptionId`), `w(ctx,g)@187` (main read), `a(ctx)Z@703` (stub), `b(ctx,g)@717`, `q(ctx,g)@925`, `v(ctx,g)@1283`.
- The complete call chain (core discovery) of **`t()`**:
  ```
  Class.forName("com.mediatek.internal.telephony.ITelephonyEx$Stub")   // :19
  Class.forName("com.mediatek.internal.telephony.ITelephonyEx")        // :41
  getMethod("getSvlteImei", ...)                                       // :53
  Class.forName("getService") → ServiceManager.getService("phoneEx")    // :65-69
  ... asInterface(...) → ITelephonyEx.getSvlteImei(slot) // Follow-up
  ```
  That is: **does not pass any public API, directly reflects the Binder agent of MTK and calls `getSvlteImei`** across processes (SVLTE = IMEI in the single-card dual-standby VoLTE scenario).
- **other hidden sides**: `getDeviceId`, `getSubscriberId`, `getSimSerialNumber`, `getMsisdn`, `getNetworkOperatorForSubscription`, `getNetworkType`, `getVoiceNetworkType`, `getCellLocationForPhone`.
- **write keys** (22): `g@248 i@298 h@330 j@354 k@378 l@441`, `r@504 t@532 s@564 u@588 v@612 w@675`, `m@1104 n@1112 x@1269 y@1277`, `b@722 a@732 e@736 f@740 d@762 c@768`.
- **Base calls**: `e@210`, `g@437`, `f@1076`.
- **trigger/switch**: manufacturer `BROR` (`e0/c2.smali:451`); model `NOAIN K11 Plus`, `NOAIN X11`, `NOAIN X13L`, `NOAIN X15L`; attributes `ro.build.hardware.version`, `ro.build.display.id`, `ro.bird.update.version`, `ro.product.hardware`, `ro.software.version`, `ro.hardware.version`, `ro.yunos.version`, `ro.yunos.name`.
- **downgrades**: When `ITelephonyEx`/`phoneEx` does not exist (not MTK), `Class.forName` throws `ClassNotFoundException` → returns null, and `getDeviceId` of `w()` takes over.
- **privacy**: **P4**; and it is the most hidden IMEI acquisition channel **of**.

### 3.38 `e0/y1` — Xiaomi collector `DeviceInfoReaderXiaomi` (**is the largest, 200+ model whitelist**)

- **method structure**: `A(ctx,g)@15` (main read, `TelephonyManagerEx`), `B(ctx,g)@545` (**permission access control + MIUI Private reading method**), `t(ctx)Z@1229` (boot wizard), `v(g)String@1272`, `w()String@1300`, `static x()JSONObject@1312` (memory), `static y()String@1381` (**`/proc/meminfo` FileReader Parse**), `z()Z@1575` (**isFiveGCapable**), `b(ctx,g)@1627`, `u(ctx,int)String@3130` (**per card VoLTE valuer**: `isVolteEnabledByUser`, `:884`, `:1168` call → `o`/`z`).
- **uses API / hidden side**
  - **`miui.telephony.TelephonyManagerEx.getDefault()`**（`:38-62`）+ **`getCellLocationForSlot`**（`:110,285,523,683,864,1148`）。
  - **`miui.telephony.TelephonyManager.getDefault()`**（`:585-605`）+ **`getSubscriberIdForSubscription` / `getSimSerialNumberForSubscription` / `getSimOperatorForSubscription` / `getNetworkTypeForSubscription`**（`:685-773`）。
  - **`isFiveGCapable`**（`:1602`）、**`isVolteEnabledByUser`**（`:3165`）。
  - `getImei`、`getNetworkType`、`getVoiceNetworkType`、`getLine1Number`、`getSimOperator`。
  - Properties: `ro.build.hardware.version` (`:1285`), **`ro.miui.cust_hardware`** (`:1295`), `ro.build.version.incremental` (`:1303`), `/proc/meminfo` (`:1393`), `Settings$Global.getString(...)` (`:3003`, `miui_version_name` ).
- **write key** (25): `g@94 i@156 h@184 j@204 k@224 l@279`, `r@340 t@374 s@406 u@430 v@454 w@517`, `p@797 o@817`, `A@1081 z@1173`, `b@1638 a@1648 e@1652 f@1656`, `F@1662 B@1764 G@1804`, `c@2967 d@2973`.
- **Base Call**: `h@96`, `g@275`, **`j@791`** (5G), **`r@811`** (cell), **`d@813`** (VoLTE).
- **trigger/switch**: Manufacturer `Xiaomi` (`e0/c2.smali:466`); `b()` internal pair `Build.MODEL` Make **hundreds of models `equals` Whitelist** (`Mi 10`, `Mi 10 Pro`, `MI 8 Lite`, `MI 9`, `MI CC 9`, `Redmi 7/7A/8/8A/K30/K30 5G/K30 Pro Zoom Edition/Note 7/7 Pro/8/8 Pro`, `Mi9 Pro 5G`, `M1808D2TC`…`2312DRAABC`), among which `Mi9 Pro 5G` and `Redmi K30 5G` use a separate 5G branch (`:1825,1885`).
- **Privacy**: **P4 +** (full dual SIM card + cell + VoLTE user switch + 5G capability + RAM + MIUI version).
- **is not solved**: `w()` and `x()`/`y()` return values ​​respectively correspond to "total memory/available memory/..." which one needs to be rechecked according to the call point.

### 3.39 `e0/z` — Synthetic Bridge: `SubscriptionManager.getActiveDataSubscriptionId()`

- **smali**：`invoke-static {}, Landroid/telephony/SubscriptionManager;->getActiveDataSubscriptionId()I`
- **caller**: `e0/a0.smali:945`, **`e0/w1.smali:958`** (only `w1` is within the scope of this report; log `, activeDataSubId：`).
- **semantics**: The subId of the current **carrying data service** (-1 means none). Different from `getDefaultDataSubscriptionId()`: the latter is "default" and the former is "current actual". The application uses it to determine `B DataSlot` when switching between multiple cards.
- **Privacy**: Indirectly determines the ownership of `DataSlot` (it can be inferred which card the user is currently using to access the Internet).

### 3.40 `e0/z0` — nubia collector `DeviceInfoReaderNubia`

- **method structure**: `t(ctx)Z@23` (`Settings$Secure.getInt` + `checkUserPermission:`), `u(ctx,g)@66` (main read), `b(ctx,g)@324`.
- **uses API / hidden side**
  - `getImei`/`getDeviceId`、`getSubscriberId`、`getSimSerialNumber`、`getLine1Number`、`getNetworkType`、`getVoiceNetworkType`、`getCellLocation`、`getSimOperatorNumericForPhone`；`getSubId`（`:117`）+ `isValidSubscriptionId`（`:175`）。
  - **`ro.nubia.nr.support`** (`:368`) → Write `G DeviceType`; `ro.build.rom.pcb.silkscreen` (`:451`, **motherboard silk screen number**, hardware-level fingerprint); `ro.build.display.id` (`:443`).
  - Carrier key `operator_service_improve_plan`; models `NX651J`, `NX659J`, `angelcare N9`.
- **write key** (27): `B@84 g@100 i@108 h@131 j@139 k@147 l@161 n@173 m@185 o@195 r@210 t@218 s@234 u@242 v@250 w@264 y@276 x@288 z@298 b@348 a@358 e@362 f@366 G@394 F@400 d@449 c@457`.
- **Base calls**: `g@157`.
- **Trigger/Switch**: Manufacturer `nubia` (`e0/c2.smali:1025`).
- **Privacy**: **P4 +** (including strong hardware fingerprints such as **motherboard silk screen**).

### 3.41 `e0/z1` — YinKe collector `DeviceInfoReaderYinKe` (**and s1/y have the same origin as**)

- The difference between **and `s1`** (Jaccard 97.6%): only log tag `DeviceInfoReaderYinKe`, model `YK6701`, and "unknown model" log text; `ColorOSTelephonyManager` + `isOemEnhanced4gLteModeSettingEnabledByUser` + `ro.board.hardware_version` are all consistent.
- **write key** (23): `g@151 i@213 h@249 j@273 k@297 l@360 o@366 r@437 t@471 s@505 u@529 v@553 w@616 z@622 b@667 a@677 e@681 f@685 F@691 B@793 G@827 d@835 c@843`.
- **Base calls**: `h@153`, `g@356`, **`l@849`**.
- **Trigger/Switch**: Manufacturer `YIK` (`e0/c2.smali:1192`).
- **Privacy**: **P4**.

---

## 4. Cross discovery

### 4.1 Collection matrix (who collects what, all statistics are based on the authority table in §1.1)

| Collection items | Number of classes | class |
|---|---|---|
| Dual SIM card IMEI/IMSI/ICCID/MSISDN/MNC/NCLS (`g…w`) | 32 | All except `s0` |
| Only single card (`g…l`+`o`) | 1 | `s0` (Lenovo tablet) |
| CellID1/LAC1（`m/n`） | 13 | `m0 n0 n1 p1 q0 s t1 u1 v1 x0 x1 y0 z0` (`q1/y1/w0` indirectly via `d0/g`) |
| CellID2/LAC2（`x/y`） | 12 | `n0 n1 p1 q0 s t1 u1 v1 x0 x1 y0 z0` |
| VoLTE1/2（`o/z`） | 15 / 14 | `m0 m1 q1 s0 s1 t0 t1 u1 v1 w0 w1 y y1 z0 z1` (`z` less `s0`, `z0`) |
| 5G1/2（`p/A`） | 8 | `q1 t0 t1 u1 v1 w0 w1 y1` |
| Device registration block (`B/F/G`) | 17 | `m0 m1 o0 q1 s0 s1 t0 t1 u0 u1 v1 w0 w1 y y1 z0 z1` |
| Memory/DRAM | 7 | `m1 p1 q1 t1 v1 w0 y1` |
| storage capacity | 2 | `p1` (StatFs root), `w1` (root + data + available) |
| 5G proprietary judgment | 6 | `q1` (`ro.telephony.default_network>=30`), `t1/v1` (SA/NSA), `u1` (vivo Parameter table), `y1` (`isFiveGCapable`), `w1` (`ro.boot.5GEnable1/2`), `z0` (`ro.nubia.nr.support`) |

### 4.2 Hide API dependency list (categorized by capabilities)

| Ability | Hidden / Private API | use class |
|---|---|---|
| IMEI | `TelephonyManager.getDeviceId/getImei` (reflection) | All |
| IMEI (MTK attribute) | `SystemProperties gsm.mtk.imei1/gsm.mtk.imei2` | `n1、x0` |
| IMEI（Binder AIDL） | `ITelephonyEx$Stub` + `getService("phoneEx")` + `getSvlteImei` | `y0` |
| IMSI | `getSubscriberId(ForSubscription)` | All |
| ICCID | `getSimSerialNumber(ForSubscription)` | All |
| Local number | `getLine1Number(ForSubscriber)`、`getMsisdn` | All / `o0 p1 q0 r1 s u w0 w1 y0` |
| Operator | `getNetworkOperator(ForSubscription/ForPhone)`、`getSimOperator(Numeric(ForPhone))` | All |
| Cell | `getCellLocation(ForPhone/ForSlot/FromSlotId/BySubId)`、`getAllCellInfo`、`getPhoneCid/getPhoneLac` | See 4.1 |
| Card slot / subId | `SubscriptionManager.getSubId/getPhoneId/getSlotIndex/isValidSubscriptionId/getDefaultSubscriptionId/getActiveSubscriptionInfoForSimSlotIndex` | All |
| VoLTE (system) | `ImsMmTelManager.isAdvancedCallingSettingEnabled` + **`setAdvancedCallingSettingEnabled`** | Base class `c()/d()`; triggered by `t0 t1 v1 w0 w1 q1 u1 y1` (+ `m0 m1 s1 y z1` that directly adjusts `r.l()`) |
| VoLTE (vendor) | `isOemEnhanced4gLteModeSettingEnabledByUser` | `m1 s1 y z1 t0` |
| VoLTE (vendor) | `HwTelephonyManager.getServiceAbility`/`isVolteAvailable` | `t1 v1` |
| VoLTE (vendor) | `miui.telephony.TelephonyManager.isVolteEnabledByUser` | `y1` |
| 5G | `Settings.Global preferred_network_mode{slot}` ∈ [23,33] | Base class `j()`; `q1 t0 w0 y1`, etc. |
| 5G (manufacturer) | `VivoTelephonyApiParams.getParamValues` / `isFiveGCapable` / `ro.boot.5GEnable1|2` / `ro.nubia.nr.support` | `u1 / y1 / w1 / z0` |
| Direct access to system services | `getSystemService("phone_ex")`、`("telephony_ims")`、`("telephony_subscription_service")` | `r1 / base class c,d / majority ` |
| inner class | `com.android.internal.telephony.PhoneFactory#getPhone` | `p0` |

### 4.3 Current status of permissions and access control

- The permission string **appearing in the** code: `android.permission.READ_PHONE_STATE`, `android.permission.READ_PHONE_NUMBERS` (`e0/r`, `t0`, `t1`, `v1`, `w0`, `y1`, etc.).
- There is only one branch of **that actually plays a "blocking" role:**: `isUserPermissionOpen==false, do not read!` (`e0/u1.smali:2011-2015`) of `e0/u1`.
- The rest of the permission determination (`checkUserPermission:` log, `readAndroidR no READ_PHONE_STATE PERMISSION_GRANTED`) **only records the log and does not change the process** - when permissions are missing, rely on reflection exception → field `null` downgrade.
- `a(Context)` access control is all stakes (constantly false), `e0/c2.k()` has no caller.

### 4.4 Code quality and traceability

- **template replicates**: `s1(TCL) ≡ y(Coolpad) ≡ z1(YinKe)`; `t1(TDTECH) ≈ v1(WIKO/PTAC)`; `u()/v()/t()` of `n0` is approximated in three copies; `r0` and `q0` have 5 sets of readings each; `e0/y` remains `DeviceInfoReaderOPPO module unknow!!!`.
- **is written repeatedly (the latter overwrites the former)**: `q1`’s `p` (`:1774`, `:1779`), `q1`’s `A` (`:2344`, `:2349`), `n0`’s `x` (`:552`, `:1121`).
- **dead code**: all `a()`; `n()` branch in base class `b()` is not reachable, `l()` is not reachable in base class `b()` - but `m0 m1 s1 y z1` will **directly `invoke Le0/r;->l()`** Reactivate this set of "SDK≥30 supplementary acquisition (VoLTE + 5G + cell)".
  > Note: The `u()/v()/x(ctx,int)` VoLTE/5G getter **of each manufacturer is not the** dead code, and is called by the same type (see the line number §3.2/3.3/3.12/3.19/3.20/3.22/3.23/3.26/3.29/3.31/3.32/3.35/3.36/3.38/3.41).

### 4.5 Data outflow form (connected with `f0/g`)

All the collection results fall into `d0/g`, and then `d0/g.e()` is used to assemble JSON; the key name style is "**manufacturer name + card sequence suffix 1/2**", which is combined with `Content-Type: application/encrypted-json` reporting of `f0/g` to form a complete "device fingerprint + dual card identity + location + 5G/VoLTE capabilities” portrait. Key point: The reporting key of **is `IMEI1/IMSI1/ICCID1/MSISDN1`. This is a straightforward naming of** without any desensitization or hashing (`d0/g.smali:1147-1411`).

---

## 5. Unknown items/to be confirmed

| # | unknown point | Current situation and impact |
|---|---|---|
| 1 | Semantics of `d0/g.P`, `d0/g.Q` (two `int`, initial value 0 / 1) | Initialized in the structure, `e()` is not written with a fixed key, and the purpose of **is not confirmed for** (suspected to be "card number/specification version").|
| 2 | `d0/g.O` (`private String`, not in `e()` output) | The author and purpose of **are not confirmed**.|
| 3 | `final b:I`, `final c:I` constant truth value of `e0/q1` | Value assignment during construction, suspected slot or level API; **has not been confirmed value by value for**.|
| 4 | Branch thresholds within the VoLTE/5G getter of each manufacturer | It has been confirmed that it has been called (see §4.4 description), but the specific judgment **(such as the comparison after `q1.u()` reads `voicecall_type`) that converts the original value into `"0"/"1"` within each getter of** has not been expanded one by one.|
| 5 | `HashMap` key names of `e0/u1.C(ctx,String,HashMap)HashMap` and `B/D` | I only saw logs such as `resultMap:`/`ret:`/`response`/`put`, and the specific keys of the **parameter table were not restored to** one by one.|
| 6 | Correspondence between `w()/x()/y()` (three types of memory values) of `e0/y1` and the reported fields | `y()` clearly reads `/proc/meminfo`; the final ownership of `w()`/`x()` is not fully aligned.|
| 7 | The final form of `g.a` when `c2.a.get(Build.MODEL)` in `e0/x1` is null | `"Unknown"` is supplemented by `d0/g.e()`; **is not confirmed whether** is backfilled elsewhere.|
| 8 | `e0/p0` of `new-array [Landroid/provider/ContactsContract$CommonDataKinds$Phone;` (`:300`) | It is obviously the product of decompilation/type analysis (actually it should be `Object[]`/`Phone[]`). The real type of **is unknown**, which does not affect the semantic judgment.|
| 9 | External uses of `e0/c2.k(Context)` | No in-dex caller, may be called by host SDK; **cannot confirm** from this APK.|
| 10 | The actual values of the attribute keys of each manufacturer on the real machine | All are read at runtime, and static analysis can only give key names and default values.|

---

## 6. Self-examination of coverage

- [x] All 41/41 classes are analyzed class by class (including 8 synthetic bridges of API and numerical semantics).
- [x] Each class is given: collection target, Android/hidden API, output key name (including JSON key), trigger conditions (manufacturer/model/permission/attribute), downgrade branch, and privacy meaning.
- [x] Key smali line number quoted (`e0/*.smali:linenumber`).
- [x] The clones have been confirmed by normalized row set comparison (`s1≈y≈z1`, `t1≈v1`), and the differences are listed one by one.
- [x] Field writing is based on the **case-sensitive** authoritative table in §1.1 (the first edition merged `b/B`, `f/F`, and `g/G`, which has been corrected).
- [x] Deeds with `e0/r`, `e0/c2`, `d0/g`, `f0/o`, `f0/l`, `e0/d2`, `e0/i0` have been cross-checked.
- [x] The APK / smali tree has not been modified; only this report and the temporary extraction file (`analysis\SystemHelper\_tmp\`) have been added.


