> **Historical analysis with subsequent corrections.** Read the [final review](../REPORT.md) first. The review narrows or corrects earlier claims about MD5-derived keys, full installed-app inventories, the reachability of privacy notices, one-second upload intervals, timestamps, and confirmed replay. Some OEM branches remain outside the follow-up review. Local identity paths and actual user data are excluded.

# SystemHelper (com.mobiletools.systemhelper 1.2-20230925-U) collector analysis · First batch (e0/a … e0/l1, 42 categories)

- Target APK: `[LOCAL_PATH_REMOVED]` (read only, not modified)
- smali source: `...\analysis\SystemHelper\apktool\smali\e0\*.smali` (apktool 3.0.3 decoded, unmodified)
- Coverage class: **42** —— a, a0, a1, a2, b, b0, b1, b2, c, c0, c1, c2$a, c2$b, c2$c, d, d0, d1, d2, e, e0, e1, f, f0, f1, g, g0, g1, h, h0, h1, i, i0, i1, j, j0, j1, k, k0, k1, l, l0, l1
- Contract class (**is the responsibility of others, this article only quotes and does not rewrite**): `e0/r` (collector base class), `e0/c2` (scheduler). This article uses them: `b/c/d/f/g/h/i/j/l/m/n/o/p/q/r` and static fields of `e0/r`; `a` of `e0/c2` (MODEL→Manufacturer Map), `h()` (Manufacturer→Reader Distribution), `f()/n()` (Assembly d0/g).
- The line numbers are all original `.smali` file line numbers (the line numbers quoted in this report are consistent with the apktool output).

---

## 0. Prefix: field of result container `d0/g` → JSON key comparison (determine "output key name")

All collectors ultimately only write the fields of `d0/g`, and the key names appear only when `d0/g.d()` / `d0/g.e()` is serialized. Actual measurement comparison (`d0/g.smali:345-980` is `d()`, `:1048-1683` is `e()`, the only difference between `e()` and `d()` is that **does not output IPv4/IPv6 (D/E) and History (M)**):

| Field | JSON key | Semantics | Typical sources |
|---|---|---|---|
| a | `Manuf` | Manufacturer | `e0/c2.a.get(Build.MODEL)` or `Build.MANUFACTURER` / `ro.product.manufacturer` |
| b | `Model` | Model | `Build.MODEL` |
| c | `HWVersion` | Hardware version | `SystemProperties` Each manufacturer’s private key |
| d | `SWVersion` | Software version | `Build.DISPLAY` / Manufacturer private key |
| e | `OS` | System name | `"Android"` or `ro.yunos.name` |
| f | `OSVersion` | System version | `Build.VERSION.RELEASE` / `ro.yunos.version` |
| g | **`IMEI1`** | serial number 1 | `getImei/getDeviceId/getDeviceId(int)`/Manufacturer attributes |
| h | **`ICCID1`** | SIM card serial | `getSimSerialNumber`/`SubscriptionInfo.getIccId` |
| i | **`IMSI1`** | User ID | `getSubscriberId` |
| j | **`MSISDN1`** | Mobile phone number | `getLine1Number(ForSubscriber)`/`getMsisdn`/`SubscriptionInfo.getNumber` |
| k | `MNC1` | Operator number (actually MCC+MNC, such as `46001`) | `getNetworkOperator(ForPhone/ForSubscription)`/`getSimOperator` |
| l | `NCLS1` | Network type string (NR/NSA/SA/LTE/WCDMA/TD-SCDMA/CDMA/GSM) | `e0/r.g(int,String)` or `f0/f.n` cache |
| m | **`CellID1`** | GSM CI / CDMA BID / LTE CI / NR NCI | `CellLocation`, `CellIdentity*`, manufacturer AIDL, SharedPreferences |
| n | `LAC1` | GSM LAC / LTE TAC / CDMA `lat:lon` | Same as above |
| o | `VolteEnabled1` | VoLTE switch | `ImsMmTelManager` reflection/manufacturer interface |
| p | `5GEnabled1` | 5G switch | `Settings.Global."preferred_network_mode"+subId` |
| q | —(**does not serialize**) | Dead fields, only `d0/g.smali:134` is set to null when constructed, no writing in the whole project | — |
| r | **`IMEI2`** | serial number 2 | Same as g |
| s | **`ICCID2`** | | Same as h |
| t | **`IMSI2`** | | Same as i |
| u | **`MSISDN2`** | | Same as j |
| v | `MNC2` | | Same as k |
| w | `NCLS2` | | Same as l |
| x | **`CellID2`** | | Same as m |
| y | `LAC2` | | Same as n |
| z | `VolteEnabled2` | | Same as o |
| A | `5GEnabled2` | | Same as p |
| B | `DataSlot` | Default data card slot, `"0"` unknown/`"1"`/`"2"` | `SubscriptionManager.getDefaultDataSubscriptionId()`(+`getPhoneId`/`e0/x.a`) |
| C | `AccType` | Account type | Default `Unknown` |
| D | `IPv4` (d() only) | Public IPv4 | `d0/h` thread + `c2$b` callback |
| E | `IPv6` (d() only) | Public IPv6 | Same as above |
| F | `SubType` | Storage/memory sub-object `{RAM,ROM,FREE}` | `f0/i.c()/a()/e()` or the private resolution of each model |
| G | `DeviceType` | Equipment type code (`000` default / `000004` / `000005` / `007001029` / `HNR550T`) | Hard coded for each model |
| H | `RegType` | Registration type (passed in by the caller, `c2.g(ctx,String)`) | `c2.smali:219` |
| I | `SpecVersion` | Fixed `"3.0"` | `d0/g.smali:890` |
| J | `RegVersion` | Client version name (`versionName`)| `c2.n()` → `g.J`; default `"1.0"` |
| K | `Time` | Timestamp | Default `""` (**is not written in the 42 categories of** in this batch, and is profiled by f0/*) |
| L | `AppInfo` | List of installed apps JSONArray | `c2$a` callback (`f0/b` object) |
| M | `History` | History JSONArray (d() only) | Does not appear in this batch |
| N | `SignalInfo` | Signal JSON (RSRP/RSRQ/SINR/cellid/LAC/time) | `d0/f` callback |

`d0/g.f(CellLocation)` (`:1697`) writes `m/n` (CellID1/LAC1); `d0/g.g(CellLocation)` (`:1943`) writes `x/y` (CellID2/LAC2). GSM uses `getCid/getLac` (-1 is considered invalid); CDMA uses `getBaseStationId`, LAC bits write `"%d:%d"` (longitude:latitude, `0x7fffffff` is considered invalid).

## 0.1 Trigger chain (understanding the premise of "switch")

1. `d0/g.a(ctx)` (`:197`) → `e0/c2.f(ctx)`: `i()` initialization → `new d0/g` → `c2.n(ctx,g)` → `c2.l/m(ctx,g)` (ICCID change comparison, read prefs `iccid1`/`iccid2`) → `c2.o(g)` → Starting from `d0/i` thread (application manifest).
2. `c2.n()` (`:3717`) first writes `g.J = versionName`, then `c2.h(ctx).b(ctx,g)` - **`c2.h()` is the distribution point**: `c2.a.get(Build.MODEL)` gets the manufacturer string, and produces the corresponding new one by one according to the manufacturer `equals` Reader (`c2.smali:413-1358`), if no match, `new e0/r` (universal implementation).
3. Therefore, "which device uses which collector" is determined by **`Build.MODEL` → manufacturer name → Reader**; the `a(Context)` of each Reader is mostly the **of `new ServiceState().getRoaming()` constant false pile** (see §5 Cross Discovery 1), the real model determination is written in `b()` internal.
4. `c2$a` (`d0/i$a` callback) sets `f0/f.g` as the return value of `c2.a(ctx)` (actually `c2.e()`: `Settings.Global."device_provisioned"==1`). Only when it is true, the application list is inserted into `g.L`.

## 0.2 Public capabilities involved in this batch (repeatedly called, hereinafter referred to as short)

| symbol | Semantics | location |
|---|---|---|
| `e0/r.h(int slot)` | Reflection `SubscriptionManager.getSubId(int)` / `getSubscriptionId(int)` → subId | `r.smali:95` |
| `e0/r.f(TM,int)` | Reflect `getNetworkType(int)`, if it is 0 then take `getVoiceNetworkType(int)` | `r.smali:745` |
| `e0/r.g(int netType,String operator)` | Network type → string; `46000/46002/46007` special processing | `r.smali:814` |
| `e0/r.c/d(ctx,int)` | VoLTE: `ImsManager.getImsMmTelManager` + `setAdvancedCallingSettingEnabled(true)` and then read back, `"1"/"0"/"-1"` | `r.smali:560/690` |
| `e0/r.j(ctx,int)` | `Settings.Global.getInt("preferred_network_mode"+subId)` → 5G switch | `r.smali:1168` |
| `e0/r.l/m/n/o/p/q(ctx,g)` | Four "machine read" implementations (API≥30 / old / API26 / "read26" / reflective version) | `r.smali:1242-2612` |
| `e0/r.r(ctx,slot,g)` | API≥28: `ServiceState.getNetworkRegistrationInfoList()` + `CellIdentityGsm/Wcdma/Tdscdma/Cdma/Lte/Nr` takes CellID/LAC | `r.smali:2614` |
| `e0/d2.b/c(TM,name,nameLong,subId)` | Reflect `TelephonyManager.getXXX(int)`, fall back to `getXXX(long)` when `NoSuchMethodException` | `d2.smali:37/47` |
| `e0/d2.a(int)` | Reflection `SubscriptionManager.getSubId(int)`→`long[]`→`int[]` (failure to fall back to `int[]`) | `d2.smali:7/132` |
| `f0/o.a/b/c` | `Class.forName` / `getDeclaredMethod` / `setAccessible(true)+invoke` (**swallows exception and returns null**) | `f0/o.smali:7/39/74` |
| `f0/l.i/j/k(String[,def])` | Reflection `android.os.SystemProperties.get(...)` Read system properties | `f0/l.smali:175/225/303` |
| `f0/l.h/j` | SharedPreferences `getString` / Property Reading | `f0/l.smali:157` |
| `f0/f.g` | Global "allow acquisition" Boolean (overridden by `t()` of each Reader) | `f0/f.smali:23` |
| `f0/f.n` / `f0/f.o` | Slot 1/Slot 2 network type string cache (written by `d0/f` callback) | `f0/f.smali:37/39` |
| `f0/f.B` | Manufacturer AIDL `c0/a` (iGreeTelephony) instance | `f0/f.smali:9` |

---

## 1. General list (42 categories)

| class | Business meaning (collection target) | Key Android API / Hide API / Properties | Output key name | privacy level |
|---|---|---|---|---|
| `e0/a` | Manufacturer adaptation: **BROR / China Mobile customized CM series (YunOS)** machine information | `Build.*`; attributes `gsm.yunos.imei0/1`, `ro.yunos.name/version`, `ro.build.version.external`, `ro.build.display.hardware.id`, `ro.product.hardware`, `ro.mediatek.version.release`; `f0/o` reflection `TelephonyManager.getSubscriberId/getSimSerialNumber/getMsisdn/getNetworkOperatorForPhone/getCellLocation*` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2 | **Extremely high** |
| `e0/a0` | Manufacturer adaptation: **DOUQI (reading music)**, including RAM/ROM/remaining space analysis | `ActivityManager.MemoryInfo.totalMem`、`StatFs(/system,/data)`、`ro.product.manufacturer`、`ro.boot.hardware.revision`、`Settings.Global.device_provisioned`；`e0/i.a`(getImei(int))、`e0/r.h`、`e0/z.a`(getDefaultDataSubscriptionId) | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType{RAM,ROM,FREE}, DeviceType=`000005`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, Volte1/2, 5G1/2, DataSlot | **Extremely high** |
| `e0/a1` | Manufacturer adaptation: **"5G HYUN"/OFeng (5GP50)** | `ro.product.hardware`; `android.telephony.TelephonyManagerEx` + `ServiceManager("phone_ex")` (MTK hidden service); `f0/o` full set of reflections | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, 5G1/2, DataSlot | **Extremely high** |
| `e0/a2` | Manufacturer adaptation: **YuLong / Coolpad (Coolpad/Yulong)** | Properties `ro.product.hardware(.version)`, `ro.build.display.id/.hardware`, `ro.config.le_hardware_version`, `ro.yulong.version.hardware`; MTK `TelephonyManagerEx.getCellLocation(int)`; `f0/o` Reflection | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 (**does not write DeviceType**) | **Extremely high** |
| `e0/b` | **API bridge**: `TelephonyManager.getImei()` (no parameters, API26+) | `TelephonyManager.getImei()` | Not written (return value to caller) | low |
| `e0/b0` | Manufacturer adaptation: **EAMEY (one ink/one secret)** | Properties `ril.imei.sim1/sim2`, `ro.build.tyd.custom.hw_version`; MTK `TelephonyManagerEx`; `e0/d2` reflection | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 | **Extremely high** |
| `e0/b1` | Manufacturer adaptation: **Onefive (all Onefive/Onefive series)**, including `/proc/meminfo` RAM Analysis | `/proc/meminfo`, `StatFs`, `Formatter.formatFileSize`, properties `ro.yunos.*`, `ro.build.description`, `ro.bd.cmcc_software_version`, `ro.bd.cmcc_hardware_version`, `ro.bird.custom.sw.version`, `ro.product.hardware`; MTK Ex | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 (RAM is for log/branch only) | **Extremely high** |
| `e0/b2` | Manufacturer adaptation: **ZTE (ZTE)**, with Wo/Unicom DM customization and DeMai dual stack | `com.android.vts.VendorTelephonyManager.getDefault().getCellLocation(int)`；`com.dmyk.android.telephony.DmUnicomAbsTelephonyManager.getVoLTEState`；`Settings.Secure."dm_user_agreement"`；`TelephonyManager.getDeviceId(int)/getLine1Number/getSimSerialNumber`；`ro.boot.hardware.revision` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, Volte1/2, DataSlot | **Extremely high** |
| `e0/c` | Manufacturer adaptation: **Baidu/Spreadtrum (XDM-01, "sprd")** | `Settings.Secure."dm_user_agreement"`；`e0/b.a`(getImei)；`getSubscriberId/getSimSerialNumber/getLine1Number/getNetworkOperator`；`getVoiceNetworkType` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, **DeviceType=`007001029`**, DataSlot=`1`, IMEI1, ICCID1, IMSI1, MSISDN1, MNC1, NCLS1 | **Extremely high** |
| `e0/c0` | Manufacturer adaptation: **EWPE/Gree (Gree)**, use manufacturer AIDL iGreeTelephony | `Settings.Secure."operator_service_improve_plan"`; `f0/f.B` (c0/a): `n()/p()/l()/h()` gets the slot, `m(slot,key,def)` gets the attributes, `e()/b()/c()/a()/d()/g()/j()` gets ICCID/number/operator/network type/LAC/CellID/VoLTE; attributes `persist.sys.fota_deviceid1/2`, `persist.sys.card.imsi`, `ro.hw_version` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2, Volte1/2, DataSlot | **Extremely high** |
| `e0/c1` | Manufacturer adaptation: **OPPO/ColorOS all series (110+ models white list)** | `ColorOSTelephonyManager.getDefault()` (reflection); `Settings.Global."device_provisioned"`; `ActivityManager.MemoryInfo`; attributes `ro.boot.dram_info`, `ro.build.hardware.version`, `ro.build.display.id`; reflection `isOemEnhanced4gLteModeSettingEnabledByUser`; `e0/r.l` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`/`000004`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, Volte1/2, 5G1/2, DataSlot | **Extremely high** |
| `e0/c2$a` | **callback**: the end after the collection thread ends (the application list falls into `g.L`, starts the IP collection thread) | `Wechat`? No - `f0/f.b` branch: `d0/h` (IP+operator) or `d0/k` | AppInfo(`L`), indirect IPv4/IPv6 | **high** (application list) |
| `e0/c2$b` | **calls back**: `d0/h$a`, writes the public IP | `d0/h` thread postback | IPv4(`D`), IPv6(`E`) | **high** |
| `e0/c2$c` | **interface**: Manufacturer Reader contract (`a(ctx)Z` / `b(ctx,g)V`) | None | — | low |
| `e0/d` | **API Bridge**: `TelephonyManager.unregisterTelephonyCallback` | Same as above (API 31+) | Don't write | low |
| `e0/d0` | Manufacturer adaptation: **Gionee (Gionee, "gionee")** | `Settings.Global."device_provisioned"` + `Settings.Secure."operator_registration_switch"`; attributes `ro.ssui.hardwarever`, `ro.ssui.sv.version`; MTK `TelephonyManagerEx.getDefault().getCellLocation(int)` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000004`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2, DataSlot | **Extremely high** |
| `e0/d1` | Manufacturer adaptation: **ORANGE (Alcatel/Ruijiako RGK OEM)** | Properties `gsm.rgk.imei1/2`, `ro.qc.hardwareversion`, `ro.qc.softversion`; `f0/o` reflection; CellID/LAC taken from SharedPreferences | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2 | **Extremely high** |
| `e0/d2` | **reflection tool**: TelephonyManager/SubscriptionManager slot value | `Class.forName("android.telephony.TelephonyManager")`, `SubscriptionManager.getSubId(int)`; `getXXX(int)`→`getXXX(long)` automatic fallback | Do not write directly (return Object) | Low (ability level) |
| `e0/e` | **API bridge**: `CellIdentityNr.getNci()` (5G cell identification, long) | `CellIdentityNr.getNci()` | Don't write | low |
| `e0/e0` | Manufacturer adaptation: **TOSOT (Dazong/Gree, "Gree")** | `Settings.Global."device_provisioned"`; attributes `ro.hw_version`, `ro.build.display.id`; `f0/f.B` AIDL (same as c0); `SubscriptionManager.getDefaultDataSubscriptionId`+`getPhoneId`/`e0/x.a` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, LAC1/2, Volte1/2, 5G1/2, DataSlot | **Extremely high** |
| `e0/e1` | Manufacturer adaptation: **OYSIN (Ouxin)** | Properties `ro.yunos.name/version`, `ro.product.hardware`, `ro.hardware.version`, `ro.xh.display.version`; `f0/o` reflection; `e0/r.p` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 | **Extremely high** |
| `e0/f` | **API Bridge**: `CellIdentityNr.getTac()` | Same as above | Don't write | low |
| `e0/f0` | Manufacturer adaptation: **Haipainoble (Haipainoble)** | `com.mediatek.internal.telephony.ITelephonyEx$Stub.asInterface(ServiceManager.getService("phoneEx")).getSvlteImei(int)` (MTK hidden Binder); attributes `ro.yunos.*`, `ro.hardware.version`, `ro.build.display.id` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 | **Extremely high** |
| `e0/f1` | Manufacturer adaptation: **OnePlus (OnePlus LE21xx)** | `ActivityManager.MemoryInfo.totalMem`, attributes `ro.boot.dram_info`, `ro.build.display.id`; `SubscriptionManager...getPhoneId`; `e0/r.l` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType{RAM,ROM,FREE}, DeviceType=`000005`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, Volte1/2, 5G1/2, DataSlot | **Extremely high** |
| `e0/g` | **API Bridge**: `ImsManager.getImsMmTelManager(int subId)` | Same as above (ImsMmTelManager, hidden/restricted API) | Don't write | low |
| `e0/g0` | Manufacturer adaptation: **Hisense (Hisense)** | Properties `ro.hardware.version`, `ro.software.version`, `ro.hmct.volte.enabled`, `persist.sys.volte.enable`; `e0/r.l`; `f0/o` reflection | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000004`/`000005`/`HNR550T`, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, Volte1/2, DataSlot | **Extremely high** |
| `e0/g1` | Manufacturer adaptation: **PHICOMM (Fixit)** | **reflects the manufacturer of `android.os.Build` injecting static fields** `OS_NAME/OS_VERSION/HARDWARE_VERSION_DISPLAY/OUTER_VERSION_DISPLAY`; attributes `ro.yunos.*`, `ro.internal.version`, `ro.build.display.hw.version`, `ro.build.display.soft`, `ro.hardware.version` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 | **Extremely high** |
| `e0/h` | **API Bridge**: `ImsMmTelManager.isAdvancedCallingSettingEnabled()` (VoLTE readback) | Same as above | Don't write | low |
| `e0/h0` | Manufacturer adaptation: **HUAWEI (EVR-AN00)** | `android.telephony.HwTelephonyManager.getDefault().getCellLocation(int)`, `com.huawei.android.telephony.TelephonyManagerEx.getDefault4GSlotId()`; `TelephonyManager.getImei(int)` reflection; attribute `ro.product.hardwareversion` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, DataSlot, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2 | **Extremely high** |
| `e0/h1` | Manufacturer adaptation: **PHILIPS (E266W)** | `Settings` None; attribute `ro.custom.hw.version` (default `MBV1.0`); `f0/o` reflection `getDeviceId(int)/getSubscriberId(int)/getSimSerialNumber(int)/getLine1NumberForSubscriber(int)/getNetworkOperatorForSubscription(int)/getCellLocationForPhone(int)`; `SubscriptionManager.isValidSubscriptionId` (setAccessible) | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2 | **Extremely high** |
| `e0/i` | **API Bridge**: `TelephonyManager.getImei(int slot)` | Same as above (API26+) | Don't write | low |
| `e0/i0` | **API Bridge**: `SubscriptionManager.getSubscriptionIds(int slot)` | API 30+ | Don't write | low |
| `e0/i1` | Manufacturer adaptation: **PPTV (KING 7S / PP5200 / PP5600)** | MTK `ITelephonyEx$Stub.asInterface(ServiceManager.getService("phoneEx")).getSvlteImei(int)`; attributes `ro.cust.hwversion`, `ro.build.custom.display.id`, `ro.product.hardware.version`, `ro.pptv.version`; `e0/r.p` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2 | **Extremely high** |
| `e0/j` | **API Bridge**: `Context.getMainExecutor()` | API 28+ | Don't write | low |
| `e0/j0` | Manufacturer adaptation: **HZTL (Huazhi/TAI-M01)**, actually use Huawei HwTelephonyManager | `HwTelephonyManager.getDefault()`, `MSimTelephonyManager`("from"), `setServiceAbility`, `isVolteAvailable`; `READ_PHONE_STATE/READ_PHONE_NUMBERS` permission check; attributes `ro.product.hardwareversion`, `ro.boot.dram_info`; `e0/b.a`, `e0/i.a`, `e0/i0.a` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, DataSlot, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2, Volte1/2, 5G1/2 | **Extremely high** |
| `e0/j1` | Manufacturer adaptation: **PROTRULY (D7/D8)** | Properties `ro.hardware_version`; `e0/d2` reflection complete set + `e0/r.f/g` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2 | **Extremely high** |
| `e0/k` | **API Bridge**: `TelephonyManager.registerTelephonyCallback(Executor,TelephonyCallback)` | API 31+ | Don't write | low |
| `e0/k0` | Manufacturer adaptation: **INTKI (E9000/E9003)** | Reflection `TelephonyManager.getCustomHWBuildVer()` / `getCustomSWBuildVer()`; Properties `ro.yunos.name/version`; `e0/r.p` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 | **Extremely high** |
| `e0/k1` | Manufacturer adaptation: **PTAC (Huawei Smart Selection: FIO/Hebe/Hera/MGZ/ALH/MIZ/MRK-BD00)** | Nearly isomorphic with j0: `HwTelephonyManager`, `MSimTelephonyManager`, `setServiceAbility`, `isVolteAvailable`, `READ_PHONE_STATE` check; attributes `ro.board.hardware_version`, `ro.boot.dram_info` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, SubType, DeviceType=`000005`, DataSlot, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2, Volte1/2, 5G1/2 | **Extremely high** |
| `e0/l` | **API Bridge**: `ServiceState.getNetworkRegistrationInfoList()` | API 28+ | Don't write | low |
| `e0/l0` | **API Bridge**: `CellInfoNr.getCellIdentity()` | API 29+ | Don't write | low |
| `e0/l1` | Manufacturer adaptation: **QIKU (Qiku, 1501-A02)** | Attributes `ro.build.description`, `ro.yulong.version.hardware`; `e0/r.p` | Manuf, Model, HWVersion, SWVersion, OS, OSVersion, IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2 | **Extremely high** |

> Privacy level caliber: **extremely high** = directly obtain IMEI/IMSI/ICCID/mobile phone number/cell identification (can be tracked across applications for a long time and can be located to the base station level); **high** = application list/public network IP; **low** = pure API bridge or reflection tool (which does not produce personal data itself, but is the infrastructure for the above capabilities).

---

## 2. Detailed explanation by category

### 2.1 `e0/a` —— BROR / China Mobile CM customized machine (YunOS)

- Statement: `.class public Le0/a;` / `.super Le0/r;` (`a.smali:1-3`), line 2282, registration key **`BROR`** (`c2.smali:443-455`).
- **collects target**: YunOS/CM customizes the dual-card identity + cell information on ROM. This class ROM puts IMEI in the **manufacturer system attribute** instead of TelephonyManager, so this class bypasses the Framework and reads the attribute directly.
- Key API/Properties (`a.smali:1961-2260`):
  - `gsm.yunos.imei0` → `IMEI1`（`:104-112`，`f0/l.j`）；`gsm.yunos.imei1` → `IMEI2`（`:291-297`）
  - `ro.yunos.name` (default `"YunOS"`) → `OS`; `ro.yunos.version` → `OSVersion`
  - `ro.build.version.external` → `SWVersion`；`ro.build.display.hardware.id` → `HWVersion`
  - `ro.product.hardware`、`ro.mediatek.version.release`
  - Reflection (`f0/o`): `getSubscriberId`, `getSimSerialNumber`, `getMsisdn`, `getNetworkOperatorForPhone`, `getCellLocationForPhone`, `getDeviceId`, `getNetworkType`, `getVoiceNetworkType`
  - `CellID0/LAC0/CellID1/LAC1` reads (`f0/l.h(ctx,"CellID0",null)`, `:266-282`, `:437-453`) from **SharedPreferences** - that is, the cache pre-written by the `d0/e`/`d0/f` listener.
- Model branch (`b()`, `:1959-2260`):
  - `CM CM1` → `SWVersion=ro.build.version.external`, `HWVersion=ro.build.display.hardware.id`, OS=YunOS, adjust `v()` (`:2006`)
  - `CM CM5S` → `HWVersion=ro.product.hardware`, adjust `x()` (`:2055`)
  - `CM CM5D` → Same as above, **and rewrite the Model as `MODEL_<ROM>_<RAM>`** (`:2092-2122`, ROM/RAM is taken from `f0/i.e()`/`f0/i.d()`), adjust `w()` (`:2124`)
  - `CM CM7S` → `SW/HWVersion=ro.mediatek.version.release`, adjust `u()` (`:2164`)
  - `CM CM3`（`:2200`）、`CM CM5`（`:2248`）→ `v()`
  - Others → `f0/j.c(ctx,"DeviceInfoReaderBROR unknow model:"+MODEL)` (`:2267`) **only records logs and does not downgrade to general reading**
- Differences between the four sub-readers: `u()` uses attributes IMEI + reflection (including `getMsisdn`); `v()` uses `getDeviceId` + reflection; `w()` uses `getDeviceId(int)`/`getCellLocationForPhone`; `x()` and `w()` is the same as **but does not write CellID/LAC**.
- Trigger/switch: no permission, no setting item judgment; only entered by `c2.h()` manufacturer matching; `a()` is a constant false pile.
- Side effects: Only write `f0/j` log (`f0/j.c` is "report error log", see §5).
- Privacy: **extremely high** - dual IMEI (direct reading of attributes, not affected by READ_PHONE_STATE changes), IMSI, ICCID, MSISDN, LAC/CID.
- Unknown: Whether `f0/j.c(Context,String)` is really connected to the network (it belongs to the f0 side and has not been verified).

### 2.2 `e0/a0` —— DOUQI (reading music): complete machine + RAM/ROM/remaining space

- Line 1056, registration key **`DOUQI`** (`c2.smali:1304-1312`).
- **collection target**: complete machine identification + storage image (total amount of RAM, total amount of ROM, available space), used for device image/capacity binning.
- API：
  - `ActivityManager.getMemoryInfo().totalMem` → `RAM`（`z()`，`:393-448`，`ceil(totalMem/1024³)+"G"`）
  - `StatFs(/system).blockCount*blockSize` + `StatFs(/data)` Same as above → After totaling `t(J)`, take **which is not less than the minimum power of 2 of the total capacity** + `"G"` → `ROM` (`A()`/`t()`, `:15-125`)
  - `StatFs(/data).availableBlocksLong*blockSizeLong/1024/1024/1024` → `FREE`（`v()`，`:170-270`）
  - `ro.product.manufacturer` → `Manuf` (**does not need `Build.MANUFACTURER`**, `:1021-1027`)
  - `ro.boot.hardware.revision`, fallback if empty `"MP_0.1"` → `HWVersion` (`y()`, `:368-391`)
  - `Settings.Global."device_provisioned"` → `f0/f.g`（`u()`，`:127-168`）
  - `e0/i.a(tm,0)`=`getImei(0)` → `IMEI1`；`e0/r.h(0)` → subId0
  - Reflection `TelephonyManager.getSubscriberId(int)`, `getSimSerialNumber(int)`, `getMsisdn(int)`, `getNetworkOperatorForPhone(0)`, `getNetworkType(int)`+`getVoiceNetworkType(int)`
  - `e0/r.r(ctx,0,g)`、`e0/r.d(ctx,0)`（VoLTE）、`e0/r.j(ctx,subId)`（5G）
  - `e0/z.a()` = `SubscriptionManager.getDefaultDataSubscriptionId()` (bridge)
- Output: `SubType` is the JSONObject of `{RAM,ROM,FREE}` (`:300-366`), `DeviceType="000005"` (`:483-485`); `DataSlot` is determined as `activeDataSubId==subId0?"1": ==subId1?"2": Do not write `(`:981-1003`).
- Failed branch: `StatFs`/JSON exceptions are only logged (`:352-365`); when `subId<0`, the entire slot 1 read is skipped (`:517`).
- Privacy: **extremely high** (IMEI/IMSI/ICCID/MSISDN) + device capacity portrait.
- Unknown: `e0/i`, `e0/z`, and `e0/x` belong to the API bridge (`e0/i` is in this batch, `x`/`z` is not in this batch).

### 2.3 `e0/a1` —— "5G HYUN" / OFeng（5GP50）

- Line 854, registration key **`5G HYUN`** (`c2.smali:1135-1143`).
- **collects target**: MTK platform white brand 5G model, obtains dual-SIM information through MTK hidden service.
- Key: `t(Context)Z` **directly `return true`** (`:15-21`) - that is, the "User Agreement/Permissions" judgment is hard-coded to true, and then `sput-boolean f0/f.g`; if it is false, `"isUserPermissionOpen==false, do not read!"` will be printed And **skips** (`:644-655`) as a whole. This is the only Reader in this batch whose access control is hard-coded.
- API/Properties:
  - `ro.product.hardware` → `HWVersion`（`u()`，`:23-33`）
  - `android.telephony.TelephonyManagerEx` + `ServiceManager.getService("phone_ex")` (MTK dual card expansion service, hidden Binder) → `getImei(int)`, `getCellLocationForPhone`
  - `SubscriptionManager.getSubId(int)`/`getDefaultSubId` reflection (`v()`, `:35-92`)
  - Reflection `getSubscriberId/getSimSerialNumber/getLine1Number/getNetworkOperator/getNetworkType/getVoiceNetworkType`
- Output: `DeviceType="000005"` (when model `5GP50`, `:817-827`), `DataSlot = getPhoneId(defaultDataSubId)+1` (SDK≥29, use `e0/x.a`), IMEI1/2, ICCID/IMSI/MSISDN/MNC1/2, NCLS1/2, `5GEnabled1/2`.
- Privacy: **Extremely high**.
- Unknown: The specific implementation of `TelephonyManagerEx` changes with the MTK version. If it fails, it will return null after `printStackTrace` (it will not downgrade to AOSP API).

### 2.4 `e0/a2` —— YuLong / Coolpad (Coolpad / Yulong)

- Line 2336, registration key **`YuLong`** (`c2.smali:623-631`).
- **collection target**: hardware version and dual-SIM information of Coolpad/Yulon multi-generation models; **does not carry `DeviceType`** (`g.G` is never written, and the default is `"000"`).
- Model → Attribute mapping (`b()`, `:2082-2302`):
  - `Coolpad A8-930 / Y72-921 / Y91-921 / Y92-9 / Y803-9` → `HWVersion=ro.product.hardware`、`SWVersion=ro.build.display.id` → `y()`（`:2164`）
  - `MTS-U0` → Same as above (`:2152-2162`) → `x()` (`:2193`)
  - `Coolpad 5380CA` → `HWVersion=ro.product.hardware.version`、`SWVersion=ro.build.display.id` → `x()`（`:2222`）
  - `Coolpad 5370` → `HWVersion=ro.build.display.hardware` → `w()`（`:2284`）
  - `C106-7 / C106-6 / C105-6` → `HWVersion=ro.config.le_hardware_version` (LeTV series) → `v()` (`:2304`)
  - Others (YuLong takes full advantage) → `HWVersion=ro.yulong.version.hardware`, and when abnormal, `f0/j.c(ctx,"DeviceInfoReaderYuLong read() error --> "+msg)` (`:2290-2319`)
- API: MTK `TelephonyManagerEx.getDefault()` + `getCellLocation(int)`; `e0/d2`/`f0/o` reflection `getImei/getSubscriberId/getSimSerialNumber/getLine1Number/getSimOperator/getSimOperatorNumericForSubscription/getNetworkOperatorForPhone/getMsisdn`; `SubscriptionManager.isValidSubscriptionId`, `getSubId`.
- Privacy: **extremely high** (4 read paths cover IMEI1/2, IMSI1/2, ICCID1/2, MSISDN1/2, NCLS1/2, CellID1/2, LAC1/2).
- Unknown: The subdivision differences of the 4 paths (v/w/x/y) are the combination of "API version + model batch", and their trigger thresholds have not been restored one by one.

### 2.5 `e0/b` —— API Bridge: `TelephonyManager.getImei()`

- Line 15, `final synthetic` (D8 is the bridge synthesized by the API level branch). `b.smali:7-15`: `invoke-virtual {p0}, TelephonyManager->getImei()Ljava/lang/String;`.
- Semantics: Only API 26+ has no parameter `getImei()`; the caller (such as `e0/c`) first determines `SDK_INT>=26` and then takes this bridge, otherwise it takes `getDeviceId()`.
- Privacy: It does not write data itself, but **is the entrance** (`e0/c.smali:51`) to the most sensitive link "get IMEI".

### 2.6 `e0/b0` —— EAMEY

- Line 1041, registration key **`EAMEY`** (`c2.smali:563-571`).
- Model: `EAMEY-M9`, `EAMEY-L9` (`:532-596`); attributes `ro.build.tyd.custom.hw_version` → `HWVersion`, `ro.build.display.id` → `SWVersion`.
- IMEI takes **attribute**: `ril.imei.sim1` → `IMEI1` (`:138-144`), `ril.imei.sim2` → `IMEI2` (`:323-331`) - RIL attribute direct reading, bypassing TelephonyManager.
- The rest: MTK `TelephonyManagerEx.getDefault().getCellLocation(int)` (`v()`, `:84-491`), `e0/d2` reflection (`u()`, `:635-1010`).
- Privacy: **Extremely high**.

### 2.7 `e0/b1` —— Onefive (all series) + `/proc/meminfo`

- Line 2082, registration key **`Onefive`** (`c2.smali:473-481`).
- **collects target**: Onefive multi-model (C2/M1/T5/T3/T2S/C5/C3/S1...) dual-card information; additionally performs memory/storage file analysis (for model normalization and logs).
- Unique: `t()` directly reads **`/proc/meminfo`** (`BufferedReader(new FileReader("/proc/meminfo"))`, `b1.smali:15-192`), press `MemTotal` to shift to `256M/512M/768M/1G/2G/3G/4G/8G/16G/32G/64G`; `u(Context)` is used `StatFs(/data)`+`Formatter.formatFileSize` generates `"x.xxG"` (`:193-256`). The only class in this batch that reads `/proc`.
- Attributes: `ro.yunos.name/version`, `ro.build.description`, `ro.product.hardware`, `ro.bd.cmcc_software_version`, `ro.bd.cmcc_hardware_version`, `ro.bird.custom.sw.version`.
- Branches: `Onefive C2`→`x()`, `M1/T5`→`y()`, `T3`/`C5`/`C3`→`y()`/Pocket, `S1`→`w()` (public, for external calls), and others `e0/r.p()` (`:1667`).
- Output: Full set of identity keys (IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2) + `Manuf/Model/HWVersion/SWVersion/OS/OSVersion`; **does not write DeviceType**.
- Privacy: **is extremely high**; `/proc/meminfo` is system information (low sensitivity).

### 2.8 `e0/b2` —— ZTE (ZTE)

- Line 1804, registration key **`ZTE`** (`c2.smali:653-661`).
- Access control: `Settings.Secure."dm_user_agreement"==1` → `f0/f.g` (`t()`, `:15-103`), continue reading if failed.
- Model: `ZTE A2020N2 Pro`, `ZTE 9000N`, `ZTE 7532N` → `DeviceType="000005"` (`:1310-1390`); `HWVersion/SWVersion` takes `ro.boot.hardware.revision` (`f0/l.k` with default value `"Unknown"`).
- **Dual vendor stack**:
  - `com.android.vts.VendorTelephonyManager.getDefault().getCellLocation(int)` (`v()`, used to get CellLocation; empty/exception `Log.e("getZTECellLocation object is null"/"exception")`)
  - `com.dmyk.android.telephony.DmUnicomAbsTelephonyManager.getDefault().getVoLTEState()` (customized by DeMai/China Unicom) → `VolteEnabled1/2` = `"1"/"0"/"-1"`
- AOSP side: `TelephonyManager.createForSubscriptionId(int)`, `getDeviceId(int)`, `getLine1Number()`, `getSimSerialNumber()`, `getSubscriberId()`, `getNetworkOperator()`; `SubscriptionManager.getDefaultDataSubscriptionId()`+`getPhoneId` reflection.
- Permission self-check: `READ_PHONE_STATE`/`READ_PHONE_NUMBERS` (`j/a.a`), if there is no permission, print `"readAndroidR no READ_PHONE_STATE PERMISSION_GRANTED"` and skip identity reading (`w()`).
- Operator's judgment: `46009/46006/46001` (China Unicom) is used for VoLTE/DataSlot coverage (`:1415-1419`).
- Privacy: **Extremely high**.
- Unknown: `com.dmyk.*` and `com.android.vts.*` are customized, ROM is private, and API is not a public version; its return semantics can only be inferred based on its method name.

### 2.9 `e0/c` —— Baidu/Spreadtrum (sprd, XDM-01)

- Line 205, registration key **`sprd`** (`c2.smali:1032-1040`).
- **collection target**: a minimalist implementation of "full read once" for small size, `DeviceType` hard-coded `007001029` (Baidu side device type code).
- Access control: `Settings.Secure."dm_user_agreement"==1` → `f0/f.g` (`t()`, `:15-38`).
- API: `e0/b.a(tm)` (`getImei()`, API26+) → `IMEI1`; `getSubscriberId/getSimSerialNumber/getLine1Number/getNetworkOperator`; `getNetworkType` (as 0 then `getVoiceNetworkType`) → `e0/r.g()` → `NCLS1`; `getCellLocation()` → `d0/g.f()`.
- The model is only `XDM-01` (`:184-190`), and other models are only `"DeviceInfoReaderBaidu module unknow!!!"`; `OS=Android`, `OSVersion=RELEASE`, `SWVersion=Build.DISPLAY`, `HWVersion=ro.product.hardware`, `SubType=f0/i.c()`, `DataSlot` are hard-coded `"1"` (`:160-162`).
- Privacy: **extremely high** (but only single card).

### 2.10 `e0/c0` —— EWPE / Gree manufacturer AIDL

- Line 557, registration key **`EWPE`** (`c2.smali:1062-1070`).
- **collection target**: Get **dual-card complete identity** through Gree's private AIDL (`f0/f.B : c0/a`, "iGreeTelephony"), because the public version API is cropped on this ROM.
- Entry protection: `if (f0/f.B == null) { f0/j.b("DeviceInfoReaderEWPE","iGreeTelephony==Null !!!"); return; }` (`:353-363`) - **will completely abandon** without manufacturer service and will not be downgraded.
- AIDL method semantics (`c0/a`, inferred from the call point): `n()/p()/l()/h()` returns the index of each card slot; `m(slot,key,def)` Read properties; `e(slot)`=ICCID; `b(slot)`=MSISDN; `c(slot)`=operator number; `a(slot)`=network type; `d(slot)`=LAC; `g(slot)`=CellID; `j(slot)`=VoLTE Boolean.
- Output: `IMEI1/2 ← m(slot,"persist.sys.fota_deviceid1/2","")`; `IMSI1/2 ← m(slot,"persist.sys.card.imsi","")`; `ICCID/MSISDN/MNC/NCLS/LAC/CellID` all from AIDL; `DeviceType="000005"`; `DataSlot=getPhoneId(...)+1` (`:408-499`); `VolteEnabled1/2="1"` (when AIDL boolean is true).
- Access control: `Settings.Secure."operator_service_improve_plan"==1` → `f0/f.g` (`t()`, `:15-38`).
- Model: `G0515D` (`:520-528`), `HWVersion=ro.hw_version`, `SWVersion=Build.DISPLAY`.
- Exception: `RemoteException` → `printStackTrace` + `f0/j.b("DeviceInfoReaderEWPE", msg)` (`:328-343`).
- Privacy: **is extremely high**, and the **data source is a manufacturer service rather than Android Framework** (more concealed).

### 2.11 `e0/c1` —— OPPO / ColorOS (110+ models whitelist)

- Line 2362, registration key **`OPPO`** (`c2.smali:788-796`).
- **collection target**: OPPO full series (Reno/Find/K/A series total 110+ models, `c1.smali:1263-2186`) dual card identity; `DeviceType` is divided into two levels according to model: **`000005`** (newer/5G model, `:2203`) and **`000004`** (older model, `:2225`).
- Exclusive to API:
  - `android.telephony.ColorOSTelephonyManager.getDefault()` (reflection) → `getImei/getVoiceNetworkType/getCellLocation/getNetworkType/getNetworkOperator/getLine1Number/getSimSerialNumber/getSubscriberId`
  - `ro.boot.dram_info` (`x()`: split according to `,` to get the last segment, `replaceAll("\\D+","")` is parsed into the number of GB) → used for RAM binning; without this attribute, print `"No dram_info property"` (`f1` is the OnePlus version of the same set of logic)
  - VoLTE: `u(Context,int slot)` (`c1.smali:2255-2362`) reflects `TelephonyManager.isOemEnhanced4gLteModeSettingEnabledByUser(int)`, logs `"checkVolteState slotId:/enable:"`, returns `"1"/"0"/"-1"`; written by `x()` in `c1.smali:466` (slot 1), `:722` (slot 2) call `VolteEnabled1/2`
  - `ActivityManager.MemoryInfo` + `Settings.Global."device_provisioned"`
  - `b()` at the end (`:2244/:2249`) first `e0/r.l()` and then OPPO exclusive `x()` coverage, which is a "universal read + manufacturer read" superposition
- Properties: `ro.build.hardware.version` → `HWVersion`, `ro.build.display.id` → `SWVersion`.
- Output: Full set of identity keys + `VolteEnabled1/2` (`"1"/"0"/"-1"`) + `5GEnabled1/2` + `DataSlot`; `SubType = c1.w(ctx)`, the key is `{RAM: c1.v(ctx)+"G", ROM: f0/i.e(), FREE: f0/i.a()}` (`c1.smali:87-168`, write `g.F` in `:1137`), RAM takes priority `ro.boot.dram_info`, failure fallback `MemoryInfo.totalMem`.
- Privacy: **Extremely high**.
- Unknown: The "two-level DeviceType" boundary rules of the 110+ models in the whitelist (which are considered 5G/new models) have not been restored one by one.

### 2.12 `e0/c2$a`——Collection ending callback (`d0/i$a`)

- 164 lines. `a(Context,d0/g,List<f0/b>)`：
  1. `f0/f.g = c2.a(ctx)` (`c2$a.smali:44-48`), namely `device_provisioned==1`;
  2. If true: `c2.c(list)` is stored in static `c2.c` (`:52-54`), traverses `f0/b` and `a()` one by one to get the JSONObject and stuffs it into `JSONArray` (`:60-92`);
  3. Write `g.L = AppInfo` (`:96`), and print `"appInfo:"+json` and `"appInfolength:"+n` (`:102-138`) - the entire **application list will be entered into logcat**;
  4. `f0/f.b` is true → `new d0/h(ctx,g,c2$b)` (IPv4/IPv6 + operator thread), otherwise `new d0/k(ctx,g)`; both `Thread.start()` (`:141-161`).
- Privacy: **high** (list of installed applications, including package name/version, strong profiling data).
- Unknown: The assignment point of `f0/f.b` is not in this batch (f0 side), and its semantics (whether "dual-stack IP is supported") have not been confirmed.

### 2.13 `e0/c2$b` - Public IP callback (`d0/h$a`)

- 76 lines. `a(ctx,g,ipv4,ipv6)`: `g.D=ipv4`, `g.E=ipv6` (`c2$b.smali:34-38`), print `"Ipv4:…, Ipv6:…"`, then `new d0/k(ctx,g).start()` (`:69-73`).
- Linkage: Only `d()` serialization will output `IPv4/IPv6` (controlled by `f0/f.b`, `g.smali:797-831`), `e()` will not be output.
- Privacy: **high** (public network IP = network side positioning/association identification).

### 2.14 `e0/c2$c` ——Manufacturer Reader interface

- Line 22: `a(Context)Z` (whether applicable), `b(Context,d0/g)V` (execute collection). Only `c2.j()/c2.n()` calls (`c2.smali:3413/3768`).
- Note: The return value semantics of `a()` degenerate into "constant false stubs" at the implementation layer (see §5).

### 2.15 `e0/d` —— API Bridge: `unregisterTelephonyCallback`

- Line 15, `TelephonyManager.unregisterTelephonyCallback(TelephonyCallback)` (API 31+). Paired with `e0/k` (registration), used by `e0/r.s(ctx)` to stop acquisition (`r.smali:403/438`).

### 2.16 `e0/d0`——Gionee

- Line 704, registration key **`gionee`** (`c2.smali:972-980`).
- Access control: `device_provisioned==1` **and** `Settings.Secure."operator_registration_switch"==1` (`t()`, `:15-103`), log `"checkUserPermission:"+bool`.
- Model: `20190619G` → `DeviceType="000004"` (`:274-290`); `HWVersion=ro.ssui.hardwarever`, `SWVersion=ro.ssui.sv.version` (`u()/v()`, `:105-127`).
- API: MTK `TelephonyManagerEx.getDefault()` + `getCellLocation(int)` (`w()`, `:302-703`), `e0/d2` reflection `getDeviceId/getSubscriberId/getSimSerialNumber/getLine1Number/getNetworkOperator(ForPhone)`, `e0/r.f`.
- Output: IMEI1/2, IMSI1/2, ICCID1/2, MSISDN1/2, MNC1/2, NCLS1/2, CellID1/2, LAC1/2, `DataSlot`, `SubType`.
- Privacy: **Extremely high**.

### 2.17 `e0/d1` —— ORANGE (Ruijake RGK OEM)

- Line 1487, registration key **`ORANGE`** (`c2.smali:578-586`).
- Model: `D5277CT`, `D5377CT`, `GO 55307`, `D5287CT` (`:1320-1414`); `HWVersion=ro.qc.hardwareversion`, `SWVersion=ro.qc.softversion`.
- **IMEI takes the attribute**: `gsm.rgk.imei1` → `IMEI1`, `gsm.rgk.imei2` → `IMEI2` (`:31-37`, `:236-242`).
- Three paths `v()/u()/t()` (`:827`, `:421`, `:15`): `v()` uses `f0/o` reflection tape `getCellLocation`, `u()/t()` uses `e0/d2` + `CellID0/LAC0/CellID1/LAC1` in SharedPreferences.
- Unmatched model → `f0/j.c(ctx,"DeviceInfoReaderORANGE unknow model:"+MODEL)` (`:1439`).
- Privacy: **Extremely high**.

### 2.18 `e0/d2` - Reflection tool (slotted TelephonyManager)

- 291 lines. The core is **overload adaptive**:
  - `c(TM, nameByInt, nameByLong, subId)` (`:47-130`): `TelephonyManager.class.getDeclaredMethod(nameByInt, int.class).invoke(tm, subId)` first; when `NoSuchMethodException`, change `(nameByLong, long.class)` and transfer parameters to `int→long`.
  - `b(TM,name,subId)`（`:37`）= `c(tm,name,name,subId)`。
  - `a(int slot)` (`:7-35`) → `d(int)` (`:132-291`): `SubscriptionManager.getSubId(int)` returns `long[]`, element-wise `long→int`; press `int[]` to retry when `ClassCastException`.
- Meaning: **is a set of general capability layer** that "slots IMSI/ICCID/number/operator/network type" and is compatible with the `int/long` subId signature drift of Android 5→13. The exception is always `printStackTrace`, and null is returned on failure (the caller will skip it if it is empty again).
- Privacy: It does not generate data by itself, but **is the only channel** for readers such as f0/d0/d1/i1/j1/b0/b1 to obtain personal identity data.

### 2.19 `e0/e` —— API Bridge: `CellIdentityNr.getNci()`

- 15 lines. NR (5G) cell identification NCI (`long`). Used by `e0/r.r()` to fill in `CellID2` (`r.smali:2887-2891`). Semantics: The unique number of the 5G base station cell, **combined with TAC can be located to the block level**.

### 2.20 `e0/e0` —— TOSOT (Osomatsu/Gree, "Gree")

- Line 555, registration key **`TOSOT`** (`c2.smali:1199-1207`).
- Access control: `device_provisioned==1` → `f0/f.g` (`t()`, `:15-64`).
- Model: `G0615D` → `DeviceType="000005"`, `SWVersion=ro.build.display.id`, `HWVersion=ro.hw_version` (`u()`); `SDK>=30` followed by `v()` (manufacturer AIDL).
- `v()` (`:78-346`) and `e0/c0` are isomorphic: if `f0/f.B` is empty, `f0/j.b("DeviceInfoReaderGree","iGreeTelephony==Null !!!")` returns directly; otherwise `IMEI1/2 ← persist.sys.fota_deviceid1/2`, `IMSI1/2 ← persist.sys.card.imsi`, ICCID/MSISDN/MNC/LAC/CellID come from AIDL, `NCLS1/2 ← f0/f.n/o`, `VolteEnabled1/2="1"`, `5GEnabled1/2` are determined by `e0/r.j()`.
- Privacy: **extremely high** (data source serves the manufacturer).

### 2.21 `e0/e1` —— OYSIN (Ouxin)

- Line 1217, registration key **`OYSIN`** (`c2.smali:488-496`).
- Model: `OYSIN X5`, `OYSIN X8`, `MX6`, `P7` (`:1050-1181`); attributes `ro.yunos.name/version`, `ro.product.hardware`, `ro.hardware.version`, `ro.xh.display.version`.
- Path: `X5`/`X8` → `v()` (`f0/o` reflection); `MX6` → `w()` (MTK `TelephonyManagerEx`); `P7` → `e0/r.p()` (base class reflection full read). Not matched → `f0/j.c(...unknow model)`.
- Privacy: **Extremely high**.

### 2.22 `e0/f` —— API Bridge: `CellIdentityNr.getTac()`

- 15 lines. NR TAC (tracking area code, `int`). Paired with `e0/e`, written to `LAC2` (`r.smali:2895`) by `e0/r.r()`. Semantics: 5G tracking area, matched with NCI positioning.

### 2.23 `e0/f0` —— Haipainoble

- Line 1121, registration key **`Haipainoble`** (`c2.smali:927-935`).
- Model: `Haipainoble S9`, `Haipainoble S18` (`:172-260`); attributes `ro.yunos.name/version`, `ro.hardware.version`, `ro.build.display.id`.
- **MTK Hide Binder Directly connected to** (`t()`, `:15-127`):
  `Class.forName("com.mediatek.internal.telephony.ITelephonyEx$Stub").getDeclaredMethod("asInterface", IBinder.class)` +
  `ITelephonyEx.getDeclaredMethod("getSvlteImei", int.class)` +
  `ServiceManager.getMethod("getService", String.class).invoke(null,"phoneEx")` → `getSvlteImei(slot)`。
  `getSvlteImei` = **SVLTE (single card dual standby LTE) scenario IMEI**, the public version API is not available.
- The rest: `e0/d2` reflection + `e0/r.f`; `u()`/`v()` two paths correspond to "with/without `getCellLocation` reflection" respectively.
- Privacy: **extremely high** (IMEI comes from Binder and is "hidden IMEI").

### 2.24 `e0/f1` —— OnePlus（LE2120/LE2110/LE2100）

- Line 759, registration key **`OnePlus`** (`c2.smali:1120-1128`).
- **collection target**: OnePlus 9 series complete machine + RAM/ROM portrait.
- RAM dual channel: `x()` reads `ro.boot.dram_info` (`f0/l.k`, take the last number, `:398-510`); if it fails, `w()` uses `totalMem/1MB` **gear table** (`0x7800→32`, `0x7000→30`, `0x6800→28`...`0x300→1`, `:170-396`) - that is, "nominal RAM" file" instead of the real value, which is a way of writing **to avoid the precise value**.
- ROM/FREE：`f0/i.e()`/`f0/i.a()`（`:108-116`）→ `SubType{RAM,ROM,FREE}`（`:87-168`）。
- `HWVersion = Build.DISPLAY.split("_")[1]` (`:718-741`); `DeviceType="000005"`; `SDK>=30`. Read `e0/r.l()` in full, otherwise print `"DeviceInfoReaderOnePlus no API"` and **give up the identity and read** (`:743-757`).
- Privacy: **extremely high** (but only API≥30 can read the identity).

### 2.25 `e0/g` —— API Bridge: `ImsManager.getImsMmTelManager(int)`

- 15 lines. `ImsManager.getImsMmTelManager(subId)` (ImsMmTelManager belongs to `@SystemApi`, reflection is required for normal applications). Used by `e0/r.c/d()` to read the VoLTE switch (`r.smali:584/712`).

### 2.26 `e0/g0` —— Hisense

- Line 2099, registration key **`Hisense`** (`c2.smali:638-646`).
- Access control: `device_provisioned` → `f0/f.g`; `Settings.Secure."operator_service_improve_plan"` (`t()`).
- Model classification (`:1847-2036`):
  - `HLTE226T/HLTE227T/HLTE260T/HLTE217T` → `DeviceType="000004"` → `z()`
  - `HNR550T` → `DeviceType="000005"` → `A()` + `e0/r.l()`（`:2016`）
  - `HNR560T/HNR553T` → `DeviceType="000005"` → `B()`
  - `HNR550T` Special case: `DeviceType` is directly written as the model name `"HNR550T"` (`:2024`) - the **DeviceType field is reused as the model string** (contract pollution).
- Attributes: `ro.hardware.version`, `ro.software.version`, `ro.hmct.volte.enabled`, `persist.sys.volte.enable` (VoLTE determination, `:988-1010`).
- Output: IMEI1/2, ICCID1/2, IMSI1/2, MSISDN1/2, MNC1/2, NCLS1/2, Volte1/2, `SubType`, `DataSlot`.
- Privacy: **Extremely high**.

### 2.27 `e0/g1` —— PHICOMM (Fixit)

- Line 1242, registration key **`PHICOMM`** (`c2.smali:803-811`).
- Model: `C832Lw` (main), `PHICOMM E1030`, `C1230L`, `C1530L`, `C1330` (`:1000-1196`).
- **'s unique trick: Reflecting the manufacturer of `android.os.Build` injects the static field** (`g1.smali:974, 1011-1113`):
  `const-class Landroid/os/Build;` → `getField("OS_NAME").get(null)` → `OS`；`OS_VERSION` → `OSVersion`；`HARDWARE_VERSION_DISPLAY` → `HWVersion`；`OUTER_VERSION_DISPLAY` → `SWVersion`。
  These fields do not exist in AOSP and are only available in Ficom/YunOS custom builds - it is a typical method for **to use reflection to detect the manufacturer's private Build field**; when abnormal, it falls back to `null` (the field is cleared).
- Others: `YunOS` branch uses `ro.yunos.name/version` + `ro.internal.version` + `ro.build.display.hw.version`; old models use `ro.build.display.soft` + `ro.hardware.version`.
- Privacy: **Extremely high**.

### 2.28 `e0/h` —— API Bridge: `ImsMmTelManager.isAdvancedCallingSettingEnabled()`

- 15 lines. VoLTE (Voice Advanced) switch readback. `e0/r.c/d()` is read once before and after `setAdvancedCallingSettingEnabled(true)` to force open **and confirm** (`r.smali:584-643`) - that is, **will modify the user's VoLTE settings** (side effect, not just reading).

### 2.29 `e0/h0` —— HUAWEI（EVR-AN00）

- Line 883, registration key **`HUAWEI`** (`c2.smali:428-436`).
- Model: `EVR-AN00` → `DeviceType="000005"`, `HWVersion=ro.product.hardwareversion`, `SWVersion=Build.DISPLAY` (`b()`, `:692-792`).
- **Huawei proprietary API**:
  - `Class.forName("android.telephony.HwTelephonyManager").getDeclaredMethod("getDefault").invoke(CLASS)` (Note: **invoke, static method for**Class object) → instance; then `getDeclaredMethod("getCellLocation", int.class)` → slotted CellLocation (`t()`, `:39-119`); exception `Log.e("getHWCellLocation exception")`.
  - `com.huawei.android.telephony.TelephonyManagerEx.getDefault4GSlotId()` → Default 4G slot (`u()`, `:795-883`), based on which slot `getImei(slot)` is selected; if empty, `Log.i("getDefault4GSlotId return null")` will be returned to `-1`.
- Reflection `TelephonyManager.getImei(int)`, `getSubscriberId(int)`, `getSimSerialNumber(int)`, `getNetworkType(int)`, `getSimOperator(int)`, `getLine1Number(int)` → fallback `getLine1NumberForSubscriber(int)`, `getNetworkOperator(int)` → fallback `getSimOperator(int)`.
- Note the implementation details: `u(0)` and `u(1)` on both branches **returns `getImei(getDefault4GSlot())`** (`:852-880`), so `IMEI1`/`IMEI2` are likely to be the same (IMEI for the same card is written twice) - see §5.
- Privacy: **Extremely high**.

### 2.30 `e0/h1` —— PHILIPS（E266W）

- Line 664, registration key **`PHILIPS`** (`c2.smali:867-875`).
- Model: `E266W` (`:628-652`); `HWVersion=ro.custom.hw.version` (default `"MBV1.0"`), `SWVersion=Build.DISPLAY`.
- Unique: `t(int subId)Z` reflects `SubscriptionManager.isValidSubscriptionId(int)` and **`setAccessible(true)`** (`:15-71`, a common wraparound that violates the limit of the hidden API reflection), which determines whether to read the slot.
- `u()` (`:73-585`) full `f0/o` reflection: `getDeviceId(int)`, `getSubscriberId(int)`, `getSimSerialNumber(int)`, `getLine1NumberForSubscriber(int)`, `getNetworkOperatorForSubscription(int)`, `getNetworkType(int)`→0 time `getVoiceNetworkType(int)`, `getCellLocationForPhone(int)`.
- Privacy: **Extremely high**.

### 2.31 `e0/i` —— API Bridge: `TelephonyManager.getImei(int slot)`

- 15 lines. API 26+ slot IMEI. Used by `e0/a0`, `e0/j0`, `e0/k1`, `e0/r.l/o` (such as `a0.smali:489`, `r.smali:1280`). **This is the key entrance** for "double IMEI" collection.

### 2.32 `e0/i0` —— API Bridge: `SubscriptionManager.getSubscriptionIds(int slot)`

- 15 lines. API 30+ Get the subId array from the slot (replaces `getSubId(int)`). Used by `e0/j0/k1`.

### 2.33 `e0/i1` —— PPTV（KING 7S / PP5200 / PP5600）

- Line 851, registration key **`PPTV`** (`c2.smali:743-751`).
- Model and attributes (`:170-256`):
  - `KING 7S` → `SWVersion=Build.DISPLAY`、`HWVersion=ro.cust.hwversion`（`trim()`）→ `e0/r.p()`
  - `PP5200` → `SWVersion=ro.build.custom.display.id`、`HWVersion=ro.product.hardware.version` → `u()`
  - `PP5600` → `SWVersion=ro.pptv.version`、`HWVersion=Build.HARDWARE` → `u()`
- **IMEI from MTK Hide Binder**: `t()` (`:15-127`) is the same as `getSvlteImei(slot)` (`ITelephonyEx$Stub.asInterface(ServiceManager.getService("phoneEx"))`) from `e0/f0`.
- Rest: `e0/d2` reflection + `f0/l.h` reads SharedPreferences of `CellID0/LAC0/CellID1/LAC1`.
- Privacy: **Extremely high**.

### 2.34 `e0/j` —— API Bridge: `Context.getMainExecutor()`

- 15 lines. API 28+; for use with `registerTelephonyCallback` (`r.smali:253/330`).

### 2.35 `e0/j0` —— HZTL (TAI-M01, actually Huawei platform)

- Line 2063, registration key **`HZTL`** (`c2.smali:1259-1267`).
- Model: `TAI-M01` → `DeviceType="000005"`, `SWVersion=Build.DISPLAY.split("\\s+")[?]`, `HWVersion=ro.product.hardwareversion` (`:1991-2023`); there is also `read8PAD` log mark (`:2036`) other than `TAI-M01`.
- **access control triple**: `device_provisioned`, `READ_PHONE_STATE`, `READ_PHONE_NUMBERS` (`j/a.a`); without `READ_PHONE_STATE`, `"readAndroidR no READ_PHONE_STATE PERMISSION_GRANTED"` and skip the identity (`t()`, `:33-100`).
- Huawei proprietary API (reflection): `getImei`, `getSubscriberId`, `getServiceAbility`, `isVolteAvailable` of `android.telephony.HwTelephonyManager.getDefault()`; `android.telephony.MSimTelephonyManager` (`"from"` field/method); `getServiceAbility` return value mapping `SA`/`NSA` (5G networking mode).
- Others: `Settings.Global."preferred_network_mode"`, `e0/b.a`(getImei), `e0/i.a`(getImei(int)), `e0/i0.a`(getSubscriptionIds), `e0/r.c/r.d`(VoLTE), `e0/r.r`(cell), `ro.boot.dram_info` RAM analysis.
- Operator: `46009/46006/46001` China Unicom Determination (`:141-147`).
- Privacy: **extremely high** (IMEI/IMSI/ICCID/MSISDN/CellID/LAC/5G network).
- Unknown: If `MSimTelephonyManager` does not exist, the related branch fails silently (the exception is only `printStackTrace`).

### 2.36 `e0/j1` —— PROTRULY（D7 / D8）

- Line 487, registration key **`PROTRULY`** (`c2.smali:912-920`).
- Model: `PROTRULY D7`, `PROTRULY D8` (`:416-432`); `SWVersion=Build.DISPLAY`, `HWVersion=ro.hardware_version`.
- The full text is only `e0/d2` reflection (`getDeviceId/getSubscriberId/getSimSerialNumber/getLine1NumberForSubscriber/getNetworkOperator(ForSubscription)/getNetworkType`) + `f0/l.h` reading `CellID0/LAC0/CellID1/LAC1`; `b()` outer layer `try/catch(Exception)` wrapping `t()` (`:473-485`) - **only stacks any exceptions and does not throw**.
- Privacy: **Extremely high**.

### 2.37 `e0/k` —— API Bridge: `registerTelephonyCallback`

- 15 lines. `TelephonyManager.registerTelephonyCallback(Executor, TelephonyCallback)` (API 31+). Used by `e0/r.k(ctx)` to register dual-slot monitoring (`r.smali:259/336`), which is the entrance for **to continuously monitor network/signal changes**.

### 2.38 `e0/k0` —— INTKI（E9000 / E9003）

- Line 225, registration key **`INTKI`** (`c2.smali:533-541`).
- Fields: `b:Class` (= `Class.forName("android.telephony.TelephonyManager")`, cached during construction), `c:TelephonyManager` (`f0/f.f` takes `phone` service).
- Unique: Reflection `TelephonyManager.getCustomHWBuildVer()` → `HWVersion`, `getCustomSWBuildVer()` → `SWVersion` (private method hung by the manufacturer on TelephonyManager).
- Model: `E9003`/`E9000` (`:147-163`); `OS=ro.yunos.name` (default `YunOS`), `OSVersion=ro.yunos.version`; then `e0/r.p()` is read in full.
- Privacy: **Extremely high**.

### 2.39 `e0/k1` —— PTAC (Huawei Smart Selection FIO/Hebe/Hera/MGZ/ALH/MIZ/MRK-BD00)

- Line 2132, registration key **`PTAC`** (`c2.smali:1244-1252`).
- Model white list (`:1998-2064`): `FIO-BD00`, `Hebe-BD00`, `Hera-BD00`, `MGZ-BD00`, `ALH-BD00`, `MIZ-BD00`, `MRK-BD00` → `DeviceType="000005"`, `SWVersion=Build.DISPLAY.split("\\s+")`, `HWVersion=ro.board.hardware_version` (`:2091-2113`).
- **and `e0/j0` are almost line by line isomorphic to** (same access control, same Huawei API, same `read8PAD` log), the differences are:
  - `k1` In `x()`, **prints the cell information of two slots** at the same time: `"CellID1 : …, LAC1 : …"` (`:1072-1084`) and `"CellID2 : …, LAC2 : …"` (`:1465-1477`); `j0` only has slot 1. `"CellID1 : "` (`:1079`) and one `"readHZTL-Q->CellID1 : "` (`:618`)
  - `j0` additionally uses `e0/i0.a` (`getSubscriptionIds(int)`, `:1861/:1867`) to get the subId; `k1` does not use this bridge, instead use `e0/x.a`+`getPhoneId` reflection calculation `DataSlot`
  - Both use `e0/b.a` (`getImei()`, `j0:93`/`k1:111`) and `e0/i.a` (`getImei(int)`, `j0:103,113`/`k1:121,131`)
- Privacy: **Extremely high**.
- Unknown: The third set of writes to `CellID3/LAC3` (`:1455-1459`) fell to `g.x/g.y` (CellID2/LAC2), reusing the same key as "slot 2" - **has a key conflict/overwrites**, but there is no way to tell with certainty which write ultimately takes effect (depends on the branch).

### 2.40 `e0/l` —— API Bridge: `ServiceState.getNetworkRegistrationInfoList()`

- 15 lines. API 28+; `e0/r.r()` Use it to enumerate the registered network and press `NetworkRegistrationInfo` to get `CellIdentity*` (`r.smali:2662-2900`). It is the key to "**can obtain the cell identification** without relying on `getCellLocation()`" (`getCellLocation` is limited in higher versions).

### 2.41 `e0/l0` —— API Bridge: `CellInfoNr.getCellIdentity()`

- 15 lines. API 29+; take `CellIdentity` from `CellInfoNr` (take NCI/TAC with `e0/e`/`e0/f`).

### 2.42 `e0/l1` —— QIKU (Qiku 1501-A02)

- 108 lines (the shortest Reader in this batch), registration key **`QIKU`** (`c2.smali:548-556`).
- Model: `1501-A02` → `SWVersion=ro.build.description`, `HWVersion=ro.yulong.version.hardware`, then `e0/r.p()` (`:56-86`).
- `Manuf = c2.a.get(Build.MODEL)` (`:38-46`); not matched → `f0/j.c(ctx,"DeviceInfoReaderQIKU unknow model:"+MODEL)`.
- Privacy: **extremely high** (all identity data is collected by the base class `p()`).

---

## 3. Cross-discovery (cross-category rules)

1. **`a(Context)` is almost useless.** The `a()` of 16 Readers (a, a1, a2, b0, b1, b2, d1, e1, f0, g1, h0, h1, i1, j1, k0, l1) are all the same code: `new ServiceState()` → `getRoaming()` → Return. The newly created empty `ServiceState` is always `roaming=false`, so **`c2.j()/c2.k()` is always false**. "Whether Reader is applicable" is actually completely determined by the manufacturer string matching of `c2.h()` + `Build.MODEL.equals(...)` inside each `b()`. `a()` is a legacy/placeholder interface.
2. The **manufacturer distributes the static mapping table `c2.a`** (`Map<MODEL,String>`) based on `Build.MODEL`. `c2.h()` then uses the **manufacturer string** to make 60+ `equals` branches. This means: **can be switched to another manufacturer's collector** by changing the model (to `ro.product.model`), and the attribute keys/reflections of each collector are completely different from API - the collection side does not have any consistency check for "model camouflage".
3. **There are 5 different implementations of the "User Consent" switch**:
   - `Settings.Global."device_provisioned"==1`（a0, b2?, c1, d0, e0, f1, j0, k1, c2$a/c2.e）
   - `Settings.Secure."dm_user_agreement"==1`（c, b2）
   - `Settings.Secure."operator_service_improve_plan"==1`（c0, g0）
   - `Settings.Global."device_provisioned" && Settings.Secure."operator_registration_switch"==1`（d0）
   - **`return true` hard-coded** (a1, `a1.smali:15-21`)
   And the thresholds are all `==1` (default 0 = not allowed), but these keys are **operator/manufacturer private Settings key**, and ordinary applications do not need any permission to read them.
4. The 6 different ways to get **IMEI are**, which shows that the goal is to "get IMEI while bypassing the AOSP restrictions":
   ① `getImei()` (API26+, `e0/b`); ② `getImei(int)` (`e0/i`); ③ `getDeviceId()`/`getDeviceId(int)` (`e0/r.m/o/p`); ④ **system properties** (`gsm.yunos.imei0/1`, `ril.imei.sim1/2`, `gsm.rgk.imei1/2`, `persist.sys.fota_deviceid1/2`); ⑤ **MTK Hide Binder** `ITelephonyEx.getSvlteImei(int)`; ⑥ **manufacturer AIDL** (iGreeTelephony `m(slot,"persist.sys.fota_deviceid…")`).
5. The implementation of **`5GEnabled1/2` is "read `Settings.Global.preferred_network_mode<subId>`"** (`e0/r.j`, `r.smali:1168-1240`), returning Boolean → `"1"/"0"`. This is not "whether the phone supports 5G", but "the user's current preferred network mode". The meaning can easily be misinterpreted.
6. **VoLTE acquisition has write side effects**: `e0/r.c()` first reflects `setAdvancedCallingSettingEnabled(true)` and then reads it back (`r.smali:614-643`), that is, in order to read `"1"`, **will first turn on the user's VoLTE switch**. Failure returns `"-1"`.
7. **`DeviceType` has confusing semantics**: `"000004"`/`"000005"` is the manufacturer/standard sub-code (c1/g0/d0/g0 is different for each model), `e0/c` uses `"007001029"` (Baidu's own code), `e0/g0` Even the model name `"HNR550T"` is written into `DeviceType` (`g0.smali:2024`). How this field is interpreted downstream (reported by f0/g) cannot be confirmed in this batch.
8. **`DataSlot` has 4 algorithms**: `getDefaultDataSubscriptionId()==subId0?"1":"2"` (a0), `getPhoneId(defaultDataSubId)+1` (a1/b2/c0/e0/f1/g0/j0/k1/c1, SDK≥29 goes `e0/x.a`, otherwise it reflects `getPhoneId`), `getDefaultDataSubscriptionId()+1` (h0, Reflected version of d0), hardcoded `"1"` (c). **has inconsistent meaning (0 base/1 base mixed)**.
9. **`SubType{RAM,ROM,FREE}` is the only widely populated structured sub-object**: RAM has `MemoryInfo.totalMem` (a0/f1/c1/j0/k1), `ro.boot.dram_info` (c1/f1/j0/k1), `/proc/meminfo` (b1), gear table (f1); ROM/FREE has `StatFs` (a0/b1) and `f0/i.e()/a()` (f1/c1/g0/j0/k1).
10. **A large number of model-specific "hidden attribute" lists** (can be used as a fingerprint table): `gsm.yunos.imei0/1`, `ril.imei.sim1/2`, `gsm.rgk.imei1/2`, `persist.sys.fota_deviceid1/2`, `persist.sys.card.imsi`, `ro.boot.hardware.revision`, `ro.boot.dram_info`, `ro.ssui.hardwarever`, `ro.ssui.sv.version`, `ro.custom.hw.version`, `ro.cust.hwversion`, `ro.hmct.volte.enabled`, `persist.sys.volte.enable`, `ro.board.hardware_version`, `ro.product.hardwareversion`, `ro.config.le_hardware_version`, `ro.yulong.version.hardware`, `ro.qc.hardwareversion`, `ro.qc.softversion`, `ro.internal.version`, `ro.xh.display.version`, `ro.pptv.version`, `ro.bd.cmcc_*`, `ro.bird.custom.sw.version`, `ro.build.tyd.custom.hw_version`, `ro.build.display.hardware(.id)`, `ro.mediatek.version.release`.
11. **reflection is a common method and exceptions swallow all**: `f0/o.c()` unifies `setAccessible(true)` and then `invoke`, `catch` only `printStackTrace()` and returns `null`; `e0/d2` similarly. Therefore, the failed **is silently downgraded to "This field is an empty string"** (`d0/g.d()` fills null into `""`/`"Unknown"`), and the reporting end cannot distinguish between "none" and "read failure".
12. **`e0/b1` reads `/proc/meminfo`, `e0/a0/a2/c0/d0/e1/g1/i1`, etc. reads `StatFs`/property** - all without runtime permissions and are "zero permission system portraits".

## 4. Privacy summary of this batch of 42 categories

| data type | Involving classes (representatives) | Permission requirements |
|---|---|---|
| IMEI (dual SIM card) | Except 13 bridge/tool types **all** | `READ_PHONE_STATE`; attribute/AIDL/Binder path **no permission required** |
| IMSI | a, a0, a1, a2, b0, b1, b2, c, c0, c1, d0, d1, e0, e1, f0, g0, g1, h0, h1, i1, j0, j1, k1, r system | `READ_PHONE_STATE`（`getSubscriberId`） |
| ICCID | Same as above | `READ_PHONE_STATE`; `SubscriptionInfo.getIccId()` Same as |
| Mobile phone number MSISDN | Same as above (`getLine1Number`/`getMsisdn`/`SubscriptionInfo.getNumber`) | `READ_PHONE_NUMBERS`(API26+)/`READ_PHONE_STATE` |
| Cell ID CellID/LAC/TAC/NCI | a, a1, a2, b2, c, c0, c1, d0, d1, e0, e1, f0, g0, h0, h1, i1, j0, j1, k1, l1, r system | `ACCESS_FINE_LOCATION` (`getCellLocation`); `getNetworkRegistrationInfoList` is also limited in API 28+ |
| Operator/Network Type/5G Mode/VoLTE | almost all | None (`getNetworkOperator` requires `READ_PHONE_STATE`; `Settings.Global` does not require reading) |
| application list | c2$a（AppInfo） | `QUERY_ALL_PACKAGES`(API30+)/none (old version) |
| Public IP | c2$b（IPv4/IPv6） | None (online) |
| Hardware/storage/memory portrait | a0, b1, c1, f1, g0, j0, k1 | None |

## 5. Unresolved/Unknown items

1. `d0/g.q` field **does not have any write point** (the whole grep only constructor is set to null), and `d()/e()` does not serialize it - it is determined to be **dead field**; but has it been used as a `q()` method name/field name? Semantics other than `NCLS` cannot be determined from the bytecode.
2. The assignment point of `f0/f.b` in `e0/c2$a` is not in this batch; when `f0/f.b==true`, go to `d0/h` (IPv4/IPv6 + operator), otherwise `d0/k`. Whether `f0/f.b` "supports dual stack" or "allows IP reporting", **is unknown to** (requires confirmation from the f0 side).
3. `e0/g0` writes `DeviceType` as the model name `"HNR550T"` (`g0.smali:2024`), which conflicts with other types of `0000xx` encoding; how the downstream interprets **is unknown**.
4. Write `m/n` (CellID1/LAC1, `:1062-1066`) in two places and `x/y` (CellID2/LAC2, `:1455-1459`) in two places in `e0/k1.x()`, and `d0/g.f()/g()` multiple times The same batch of keys will also be written; the final value depends on the branch order, **does not dynamically verify**.
5. Both branches of `e0/h0.u(int)` return `getImei(getDefault4GSlot())` (`h0.smali:852-880`), so `IMEI1` and `IMEI2` are very likely to be the same; whether it is a logical defect in **,** or Huawei platform "dual card same" IMEI" is expected, **is unknown and** (not verified on a real machine).
6. `e0/c0`/`e0/e0` depends on `c0/a` (iGreeTelephony) AIDL The **real method semantics of the interface** are only inferred from the call point (`e(slot)=ICCID`, `b(slot)=MSISDN`, `d(slot)=LAC`, `g(slot)=CellID`, etc.), without stub file support (`c0/a` is not in this batch).
7. `com.android.vts.VendorTelephonyManager` / `com.dmyk.android.telephony.DmUnicomAbsTelephonyManager` of `e0/b2` are customized ROM private classes, and the return encoding of `getVoLTEState()` (`"1"/"0"/"-1"`) can only guess its semantics based on strings.
8. The **business definition** of each model `DeviceType` (`000004` vs `000005` vs `007001029`) does not have any string/comment description; it is speculated that it is related to "whether it is 5G/whether it is a new standard", but there is no evidence for ** **.
9. The `CM CM5D` branch of `e0/a` will rewrite `Model` to `MODEL_ROM_RAM` (`a.smali:2092-2122`), where the exact return of `f0/i.e()`/`f0/i.d()` (ROM/RAM string format) belongs to f0 On the other hand, **does not verify**.
10. The **judgment threshold** (API version/model batch) between each Reader's `b()` internal model branch and "read path selection" (such as a2's v/w/x/y, b1's x/y/w + base class p) is not restored branch by branch; this article only gives "which paths + keys covered by each".
11. In addition to all `printStackTrace()` (`e0/d2`, `f0/o`, and each `try/catch`) in this batch, whether there are **triggered by `f0/j.c(Context,String)` and the network reports**, which belongs to the f0 side and has not been verified.
