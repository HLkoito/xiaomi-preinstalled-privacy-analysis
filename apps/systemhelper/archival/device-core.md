> **Historical analysis with subsequent corrections.** Read the [final review](../REPORT.md) first. The review narrows or corrects earlier claims about MD5-derived keys, full installed-app inventories, the reachability of privacy notices, one-second upload intervals, timestamps, and confirmed replay. Some OEM branches remain outside the follow-up review. Local identity paths and actual user data are excluded.

# SystemHelper telemetry acquisition core layer (d0/e0/r/e0/c2/AIDL) reverse analysis

- Target APK: `[LOCAL_PATH_REMOVED]`
- Package name: `com.mobiletools.systemhelper` v1.2-20230925-U (versionCode 3, minSdk 24, targetSdk 28)
- Analysis baseline: `analysis\SystemHelper\apktool\smali` (apktool decompilation, read-only) + `classes.dex` original byte verification
- Scope of this article: `d0/*` (15 files), `e0/r` (base class), `e0/c2` (arranger), `b0/a`+`c0/a` (AIDL)
- Delivery comes with: `findings\c2_map_a_full.tsv` (`e0/c2.i()`’s 304 `Build.MODEL → Brand` full mapping, including smali line number)

> Note: All `file:line` in this article refer to `apktool\smali\<file>` (such as `d0/g.smali:341`). The line number is the apktool output line number, not the source code line number before decompilation.

---

## 1. Overview of d0 layer (division of roles)

The `d0` package is a three-piece set of "collection result container + event source + upload thread". **itself does not contain the business logic of the 85 collectors of** (that is in `e0/*`, the reader subclass of each manufacturer).

| File | Type/parent class | role | key entrance |
|---|---|---|---|
| `d0/a` | synthetic final | API30 Sugar removal bridge: `TelephonyDisplayInfo.toString()` | `a(TelephonyDisplayInfo)String` |
| `d0/b` | synthetic final | Sugar removal bridge: `TelephonyManager.getServiceState()` | `a(TelephonyManager)ServiceState` |
| `d0/c` | synthetic final | Sugar removal bridge: `TelephonyDisplayInfo.getNetworkType()` | `a(...)I` |
| `d0/d` | synthetic final | Sugar removal bridge: `TelephonyDisplayInfo.getOverrideNetworkType()` | `a(...)I` |
| `d0/e` | `android.telephony.PhoneStateListener` | Standard monitor for API<31 (one instance per card slot) | `onDisplayInfoChanged` |
| `d0/f` | `android.telephony.TelephonyCallback` | API≥31 standard/signal monitor (implementing three sub-interfaces of DisplayInfo/CellInfo/SignalStrengths) | `onDisplayInfoChanged` / `onSignalStrengthsChanged` |
| `d0/f$a` | `LocationListener` | Positioning callback for `fused` provider (created anonymously within `d0/f.onSignalStrengthsChanged`) | `onLocationChanged` |
| `d0/g` | Ordinary category (54KB) | **acquisition result container** ("DeviceInfo"), 43 fields (`a`–`z` + `A`–`Q`) + 2 JSON serializers + 2 CellLocation parsers | `d()` / `e()` |
| `d0/h` | `Thread` | **IP address thread** (IPv4=server echoes public network IP, IPv6=local network card) | `run()` |
| `d0/h$a` | interface | `d0/h` completed callback | `a(Context,d0/g,String,String)` |
| `d0/i` | `Thread` | **application usage statistics + traffic thread** (UsageStats/NetworkStatsManager) | `run()` |
| `d0/i$a` | interface | `d0/i` completed callback | `a(Context,d0/g,List)` |
| `d0/i$b` | `Comparator<UsageStats>` | Sort by front desk duration **descending order** | `a(UsageStats,UsageStats)I` |
| `d0/j` | `Thread` | Historical signal record loader (**works in the constructor, `run()` does not override**) | `<init>` |
| `d0/k` | `Thread` | **reports thread** (the only place where `f0/g.c(String,[B,[B)` is called) | `run()` |

Assembly relationship (who new who):

- `d0/i` is only activated by `e0/c2.f()` and `e0/c2.g()` (`e0/c2.smali:167`, `:290`).
- `d0/j` is only launched on the `"R"` branch of `e0/c2.g()` (`e0/c2.smali:314`).
- `d0/h` **is only created by `e0/c2$a` (the callback of `d0/i`).** (`e0/c2$a.smali:145`) is the only `new-instance ... Ld0/h;` in APK.
- `d0/k` is started from four places: `e0/c2.g()` bottom branch (`:343`), `e0/c2$a` (`:156`), `e0/c2$b` (`:69`, after IP callback).
- `d0/f` / `d0/e` are registered by `e0/r.k(Context)` (`e0/r.smali:245`, `:268`, `:322`, `:345`).

---

## 2. `d0/g` data structure (field → meaning → writer → JSON key)

Field declaration: `d0/g.smali:7..91`; the constructor is all set to null (except `B`, initial value `"0"`): `d0/g.smali:95..195`.

| Field | Type | JSON key (`d()`, line number) | Semantics | Writer (evidence) |
|---|---|---|---|---|
| `a` | String | `Manuf` (354-358) | Manufacturer | `e0/r.b()` ← `Build.MANUFACTURER` (`e0/r.smali:498`); also use it when reporting URL |
| `b` | String | `Model` (369-373) | Model | `e0/r.b()` ← `Build.MODEL`（`e0/r.smali:494`） |
| `c` | String | `HWVersion` (384-388) | Hardware version | `e0/r.b()` ← SystemProperties `ro.hardware.version`（`e0/r.smali:514-522`） |
| `d` | String | `SWVersion` (399-403) | Software version | `Build.DISPLAY`（`e0/r.smali:510-512`） |
| `e` | String | `OS` (414-418) | Fixed `"Android"` | `e0/r.smali:502-504` |
| `f` | String | `OSVersion` (429-433) | `Build.VERSION.RELEASE` | `e0/r.smali:506-508` |
| `g` | String | `IMEI1` (444-448) | IMEI(slot0) | `e0/r.l/m/o/p/q`; use `d0/k.a()` to verify the 15-digit pure number before reporting |
| `h` | String | `ICCID1` (459-463) | SIM serial number/ICCID | `e0/r.l/o/n/p/q`; `e0/c2.o()` may be transformed into MD5 |
| `i` | String | `IMSI1` (474-478) | Subscriber ID | Same as above; `d0/i` uses it as the query condition for mobile traffic `subscriberId` |
| `j` | String | `MSISDN1` (489-493) | Mobile phone number | Same as above (`getLine1Number` / `SubscriptionInfo.getNumber()`) |
| `k` | String | `MNC1` (504-508) | **is actually the complete network operator number** (such as `46001`), not the 2~3 digit MNC | `getNetworkOperator()` / `getNetworkOperatorForSubscription` |
| `l` | String | `NCLS1` (519-523) | Network standard string (NR/LTE/NSA/SA/WCDMA/GSM/CDMA/TD-SCDMA) | `e0/r.g(int,String)` mapping; also covered by `f0/f.n` (written by `d0/e`/`d0/f`) (`e0/r.smali:1366-1368`) |
| `m` | String | `CellID1` (534-538) | Cell ID | `d0/g.f(CellLocation)`；`e0/r.r()`（CellIdentity）；`f0/l.h("CellID0")` |
| `n` | String | `LAC1` (549-553) | LAC/TAC | Same as above (`"LAC0"`) |
| `o` | String | `VolteEnabled1` (564-568) | VoLTE switch `"1"/"0"/"-1"` | `e0/r.c()/d()` (IMS reflection) |
| `p` | String | `5GEnabled1` (579-583) | 5G capabilities `"1"/"0"` | `e0/r.j(ctx,slot)`（`preferred_network_mode` ∈ 23..33） |
| `q` | String | **without** (without JSON key) | Only used within class/reserved for slot1 pairing | Construct only set null |
| `r` | String | `IMEI2` (594-598) | IMEI(slot1) | Same as `g` |
| `s` | String | `ICCID2` (609-613) | SIM2 | Same as `h` |
| `t` | String | `IMSI2` (624-628) | IMSI2 | Same as `i`; `d0/i` uses it to query the second traffic flow. |
| `u` | String | `MSISDN2` (639-643) | Number 2 | Same as `j` |
| `v` | String | `MNC2` (654-658) | Operator number 2 | Same as `k` |
| `w` | String | `NCLS2` (669-673) | Format 2 | `e0/r.l()` ← `f0/f.o` |
| `x` | String | `CellID2` (684-688) | Cell 2 | `d0/g.g(CellLocation)`；`e0/r.r()` |
| `y` | String | `LAC2` (699-703) | LAC2 | Same as above |
| `z` | String | `VolteEnabled2` (714-718) | VoLTE2 | `e0/r.c()/d()` slot1 |
| `A` | String | `5GEnabled2` (729-733) | 5G2 | `e0/r.j(ctx,1)` |
| `B` | String | `DataSlot` (776-780) | The default data card is `"1"/"2"`, other values are forced to `"0"` | `e0/r.n()/o()`：`SubscriptionManager.getDefaultDataSubscriptionId()+1`（`e0/r.smali:1735-1747`、`:1845-1857`） |
| `C` | String | `AccType` (791-795) | `"WiFi"/"Mobile"/"Disconnected"` | `e0/c2.n()`（`e0/c2.smali:3820-3855`） |
| `D` | String | `IPv4` (810-814) **only if `f0/f.b`** | Public IPv4 | `e0/c2$b` ← `d0/h` |
| `E` | String | `IPv6` (825-829) **only if `f0/f.b`** | Native IPv6 | `e0/c2$b` ← `d0/h` |
| `F` | JSONObject | `SubType` (836-847) | Manufacturer-defined sub-information | Collector (manufacturer subclass) writes, this package does not write |
| `G` | String | `DeviceType` (861-872) | Default `"000"` | This package only gives the bottom line; it is actually written by the shell/collector |
| `H` | String | `RegType` (884-888) | Registration type (`A/S/B/T/R`…) | `e0/c2.g(ctx,String)` input parameters (`e0/c2.smali:219`) |
| `I` | String | `SpecVersion` (890-896) | **is constant `"3.0"`**, overwritten every time serialization | `d0/g.d()` itself |
| `J` | String | `RegVersion` (911-913) | Default `"1.0"`, actual=App `versionName` | `e0/c2.n()`（`e0/c2.smali:3739-3741`） |
| `K` | String | `Time` (925-929) | `SimpleDateFormat` format current time | `e0/c2.n()` ← `f0/l.d()`（`e0/c2.smali:3859-3863`） |
| `L` | JSONArray | `AppInfo` (935-946) | Application usage/traffic array, elements from `f0/b.a()` | `e0/c2$a`（`e0/c2$a.smali:96`） |
| `M` | JSONArray | `History` (972-979, write **only if**key exists) | History to be reported (SQLite `systemhelper.history`) | `d0/k.b()` (`d0/k.smali:124-129`; set null `:336` on failure) |
| `N` | String | `SignalInfo` (953-968) **only if `f0/f.e`** | Historical signal JSON string | `d0/j.<init>`（`d0/j.smali:54-62`） |
| `O` | String（private） | — (serialization cache) | `d()/e()` buffers the entire string JSON at the end | `d0/g.smali:985`、`:1644` |
| `P`/`Q` | final int | — | Constant 0 / 1, **is only constructed and assigned, without any reading point** (the whole library `iget Ld0/g;->P:` is empty) | — |

### 2.1 Differences between two serializers

- `d()LString;` (`d0/g.smali:335`, `synchronized`): Full version - including `IPv4/IPv6` (controlled by `f0/f.b`), `SignalInfo` (controlled by `f0/f.e`), `History` (written only by `M != null`). When reporting **, use this**.
- `e()Ljava/lang/String;` (`d0/g.smali:1038`, `synchronized`): Lite version - **does not have IPv4/IPv6, nor History**. Only called by `d0/g.c(Context)` (`d0/g.smali:301`), and `d0/g.c` only called by `com.mobiletools.systemhelper.Activity.TestActivity:59` (for debugging).
- Both do "cover when writing": write the null field back to `"Unknown"` / `""` / `"0"` (`d()`:345-780 a large number of `if-nez ... iput-object`), and normalize `DataSlot` to `{"0","1","2"}`, `SpecVersion` mandatory `"3.0"`; if `DeviceType` is empty, set `"000"`.
- Call `"DeviceInfo"/"JSONException->"` when `JSONException` and return `O` (the last cached string, null for the first time) (`d0/g.smali:997-1030`).

### 2.2 Binding singleton to Context

`d0/g` **itself does not have a single instance**, nor does it have a static Context; all three static factories stuff `Context` into `f0/f.f` and then transpose `e0/c2`:

```smali
# d0/g.smali:197  a(Context)
sput-object p0, Lf0/f;->f:Landroid/content/Context;   # Store the global Context in f0/f.f
invoke-static {p0}, Le0/c2;->f(Landroid/content/Context;)Ld0/g;
...
catch Exception → log "DeviceInfo" → return null
```
- `a(Context)g` (`:197`) → `e0/c2.f()` (complete acquisition + start `d0/i`)
- `b(Context,String)g` (`:249`) → `e0/c2.g()` (branch collection with RegType + start corresponding thread)
- `c(Context)String` (`:301`) → `e0/c2.f().e()` (simplified JSON for debugging)

That is: **creates a new `d0/g`** (`new-instance Ld0/g` within `e0/c2.f`/`g`) every time it is called. The "single case" is only reflected in the static slots of `e0/c2.b` (vendor reader cache) and `f0/f`.

### 2.3 CellLocation processing (`f(CellLocation)` / `g(CellLocation)`)

`f()`=slot1 → write `m/x`(CellID), `n/y`(LAC); `g()`=slot2 → write `x/y`. The structures of the two methods are completely symmetrical (`d0/g.smali:1697` / `:1943`):

1. Input parameter null → type `"sim1 location is null"` / `"sim2 location is null"` and return directly.
2. `GsmCellLocation`: `getCid()`/`getLac()`, the value `-1` is considered invalid (only logs are logged, fields are not covered).
3. `CdmaCellLocation`: `getBaseStationId()` → CellID; `getBaseStationLongitude()+getBaseStationLatitude()` → `String.format("%d:%d", lon, lat)` → Write the LAC bit (`0x7fffffff` is considered invalid and skipped).
4. Other types → `"unknow CellLocation type:"`.
5. The whole process is `declared-synchronized` (`monitor-enter p0`), that is, the `d0/g` instance is locked.

---

## 3. Event source and data flow timing

### 3.1 Registration path (who puts the listener on it)

`e0/r.k(Context)` (`e0/r.smali:187`, `static synchronized`), log `"DeviceInfoReaderBase"/"listenNetworkType---->"`:

| card slot | Conditions | action |
|---|---|---|
| slot0 | `h(0) > 0 && f0/f.h == null` | `tm.createForSubscriptionId(subId0)` → `f0/f.h` |
| slot0 | `SDK_INT >= 31 (0x1f)` | `new d0/f(ctx,0)` → `f0/f.l`; `e0/k.a(tm, e0/j.a(ctx), callback)` registration |
| slot0 | `SDK_INT < 31` | `new d0/e(ctx,0)` → `f0/f.j`；`tm.listen(listener, 0x100500)` |
| slot1 | `h(1) > 0 && f0/f.i == null` | `createForSubscriptionId(subId1)` → `f0/f.i`; similarly divided into SDK `f0/f.m`(d0/f) / `f0/f.k`(d0/e) |

`0x100500 = LISTEN_SIGNAL_STRENGTHS(0x100) | LISTEN_CELL_INFO(0x400) | LISTEN_DISPLAY_INFO_CHANGED(0x100000)`。
`e0/r.s(Context)` (`:373`) reversely logs out and sets all 6 static slots to null.

Call site (same "k() then s()" pattern):
`DMApplication:34` (process startup), `OnBootReceiver:80`, `AppInfoRegisterReceiver:60/104`, `ScheduleRegisterReceiver:60/104`, `SIMStateReceiver:189/202`, `RegisterHelperService:78`, `TestActivity:119`.

### 3.2 Standard (5G/NR) state machine: `d0/e.onDisplayInfoChanged` ≡ `d0/f.onDisplayInfoChanged`

The two logics are equivalent line by line (`d0/e.smali:55..452` ↔ `d0/f.smali:156..515`). The only difference is the log tag (`"DMPhoneStateListener"` vs `"DMTelephonyCallback"`) and `synchronized` modification:

1. `info == null` → `"displayInfo==null"`, return.
2. `j/a.a(ctx,"android.permission.READ_PHONE_STATE") != 0` → `"... no READ_PHONE_STATE PERMISSION_GRANTED"`, return.
3. Log `"slot:<b>, displayInfo:<TelephonyDisplayInfo.toString()>"` (via `d0/a` desugar bridge).
4. `subId = e0/r.h(slot)`; `tm.createForSubscriptionId(subId).getServiceState()` - **is only used to trigger binding/logging** once, and the result will only be `"serviceState==null"` when it is null, and will not participate in subsequent judgments (`d0/e.smali:131-166`).
5. `netType = d0/c.a(info)`（`getNetworkType()`）；`override = d0/d.a(info)`（`getOverrideNetworkType()`）。
6. Press `slot` to select the target static field: slot0 → `f0/f.n`, slot1 → `f0/f.o`; for other slots → press `"unknown mSlotId:"` and jump to the final log.
7. Format mapping (two slots in the same table, packed-switch see `d0/e.smali:405-451` / `d0/f.smali:468-514`):

| netType | constant | Result string |
|---|---|---|
| 1,2 | GPRS/EDGE | `GSM` |
| 3,8,9,10,15 | UMTS/HSDPA/HSUPA/HSPA/LTE? → See below | `WCDMA` |
| 4,5,6,7,12,14 | CDMA/EVDO series | `CDMA` |
| 13 | LTE | **`LTE` or `NSA`**: `override==3(NR_NSA)→"NSA"`, `override==4(NR_NSA_MMWAVE)→"NSA"`, otherwise `LTE` |
| 16 | GSM | `GSM` (repeated hit) |
| 17 | TD-SCDMA | `TD-SCDMA` |
| 19 | LTE_CA | Same as 13 rules (`LTE`/`NSA`) |
| 20 | NR | `SA` |
| Others (11,18,…) | — | Type `"unknown networktype <n>"` and write `UNKNOWN` |

> Note: `netType` 3/8/9/10/15 all fall into `WCDMA` in this table; `pswitch_5`/`pswitch_c` branches are still **46000/46002/46007 → `TD-SCDMA`, otherwise The operator special penalty for `WCDMA`** (also exists in `e0/r.g()`, see §5). `pswitch_5` of `d0/e` does not come with operator special discount (only `WCDMA`), but `e0/r.g()` does - this is a difference in implementation details between the two.

8. Finally, log `"NetworkTypeSlot1:<f0/f.n>, NetworkTypeSlot2:<f0/f.o>"`.
9. `d0/e` **does not overwrite** `onSignalStrengthsChanged`; `onCellInfoChanged` only returns after `invoke-super` (`d0/e.smali:33`) → The signal strength cannot be collected on the path API<31.

### 3.3 Signal strength + positioning: `d0/f.onSignalStrengthsChanged` (`d0/f.smali:517`)

1. Master switch `f0/f.e == false` → Return directly (this field is also the switch for whether `SignalInfo` enters JSON).
   **is important**: all APK does not have any `sput-boolean ... Lf0/f;->e:` (only `a/g/u/v` has a write point), that is, `f0/f.e` is always the initial value `false` → In the actual operation of this method, **returns** in the first step, below 3.3 The entire process is "design intention" rather than the current effective path (see §6.8 for details).
2. `e0/r.h(slot)` → `tm.createForSubscriptionId()` (result discarded, side effects only).
3. `raw = SignalStrength.toString()` → Remove all blanks → Save `f0/f.c` (static, shared across instances) → Split by `","`.
4. Find the section in the slice where the first 3 characters of **== `"mNr"`** (that is, the NR component), and get `nr`.
5. Hard parse four fields (`substring(indexOf(...)+7, ...)`) with string subscripts: `ssRsrp=`, `ssRsrq=`, `ssSinr=`, `level=` - this is a fragile parser ** ** (`d0/f.smali:599-683`) based on the `SignalStrength.toString()` text format.
6. Fill in the analysis result `f0/n` (`d(rsrp)`/`e(rsrq)`/`f(sinr)`), then convert `Integer.parseInt` into three ints, and log `"rsrp<I>rsrq<I>sinr<I>"`.
7. Positioning trigger judgment (`d0/f.smali:751-760`): skip it only if `rsrp > 0x7fffffff`, `rsrq > 0x7fffffff` and `sinr > 0x0ccd569f` are established at the same time; otherwise, press `"enter====>LOCATION "` and take the positioning branch - because `SignalStrength` uses `0x7fffffff` Indicates unknown, **actually takes the position of** almost every time.
8. Positioning: Either permission `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` is sufficient; `LocationManager.getLastKnownLocation("fused")` → Format → If `lastKnown == null` or `String.valueOf(location) != format string`, then `requestLocationUpdates("fused", 1000ms, 10.0f, new d0/f$a(this, lm))` (`d0/f.smali:798-845`).
9. Group JSON (`d0/f.smali:853-900`): `{RSRP, SINR, RSRQ, cellid←g.x, LAC←g.y, time←g.K, location←formatted location string}`, only log `"Signal json info:<json>"`, **will not be dropped into the library or included in g**.
10. At the end `f0/m.a(ctx, f0/m.c, f0/m.b.intValue())` (`d0/f.smali:946-956`) - **only takes the instance and discards the return value**. There is no insert call for `f0/m` in the entire library (see §9 Unknown items).

`d0/f$a` (`d0/f$a.smali`) three callbacks: `onLocationChanged`/`onProviderDisabled` directly `d0/f.a(Location)` **discards the return value** after formatting; `onProviderEnabled` first verifies the positioning permission, and then `getLastKnownLocation(provider)` discards the same. → In fact, the positioning data **does not enter any reporting field** (`g.D/E` is IP, not latitude and longitude).

### 3.4 Thread timing (three collection threads + reporting thread)

```
[RegType distribution] e0/c2.g(ctx, regType) e0/c2.smali:189
   ├─ "A" && f0/f.g(device_provisioned)  → new d0/i(ctx,g,c2$a).start()      :290
   ├─ "R"                                → new d0/j(ctx,g).start()           :314
   └─ Other / "A" but not provisioned → new d0/k(ctx,g).start() :343

d0/i.run()  d0/i.smali:620
   b = now(); c = now() - f0/f.z(7 days) # Only if b==0&&c==0
   d() → UsageStats aggregation + sorting + package-by-package assembly f0/b
   i$a.a(ctx, g, list)  →  e0/c2$a:
        f0/f.g = e(ctx) # Calculate again device_provisioned
        g.L = JSONArray(f0/b.a() ...)          # AppInfo
        if (f0/f.b) new d0/h(ctx,g,c2$b).start() # IP required
        else        new d0/k(ctx,g).start()

d0/h.run()  d0/h.smali:173
   e = IPv4 = f0/l.l(ctx) ? new f0/e().a() : "" # HTTP GET Public IP
   f = IPv6 = first non-loopback/non-linklocal Inet6Address
   h$a.a(ctx,g,e,f)  →  e0/c2$b: g.D=e; g.E=f; new d0/k(ctx,g).start()

d0/k.run()  d0/k.smali:593
   if (!f0/f.g) { queue(g.d()); log "User Permission Closed!" ; return }
   ok = d(g) # Verification → (Network/Operator) Gate → Encryption → POST
   c(ok) # Empty method (d0/k.smali:407)
```

The "thread" of `d0/j` is an illusion: `d0/j.smali` only has a constructor and no `run()` (inherits the empty implementation of `Thread.run()`). The loading action occurs on the **caller thread** before `start()`.

---

## 4. Complete restoration of reporting pipeline (`d0/k`)

### 4.1 Gate (`d0/k.d(d0/g)`, `d0/k.smali:413`, `synchronized`)

```
a(g.g) && a(g.r) # IMEI1 and IMEI2 must be "legal" (see below) otherwise return false directly
  └ f0/l.l(ctx) == false # No network
        → queue(g.d()); log "No network connected!"; return false
  └ f0/l.o(ctx) == true # Mobile data is connected
        → return b(g) # Report directly
  └ c2.k(ctx)==false && f0/l.m(g)==true
        → return b(g) # Unicom card + DataSlot matching
  └ else
        → queue(g.d()); log "isUnicomNetwork=false, not post!"; return false
```
- `d0/k.a(String)` (`:25`): `null`/null → **returns true** (release); length != 15 → `"imei: <x> length is not 15!"` false; not `[0-9]+` → `"imei: <x> is not all number!"` false.
- `c2.k(ctx)`=`e0/c2.j()`=`reader.a(ctx)`; base class `e0/r.a()` returns `new ServiceState().getRoaming()` (**constant false**) (`e0/r.smali:477-489`).
- `f0/l.m(g)` (`f0/l.smali:401`): `DataSlot=="1"` and `g.k ∈ {46001,46006,46009}`, or `DataSlot=="2"` and `g.v ∈ the same set` → true; otherwise, `"isUnicomNetwork->DataSlot："` and false.
- "queue" = `f0/d.b(ctx, f0/d.c, f0/d.b).c(g.d())` (Insert `systemhelper.history`, table `history(id, jsonString)`, `f0/d.smali:496`; the upper limit of **is 100 rows**, if the limit is exceeded, delete the oldest row first: `f0/d.smali:174-206`).

### 4.2 Encrypted link (`d0/k.b(d0/g)`, `d0/k.smali:98`)

```
postString = g.d() # Plain text JSON (§2)
key = f0/c.d(postString) # MD5(UTF-8(json)) → 16 bytes
encKey     = f0/c.c(ctx, key)                         # RSA/ECB/PKCS1Padding(pub.cer, key)
signB64 = f0/c.b(encKey) # Base64.encodeToString(no line break)
signParam  = URLEncoder.encode(signB64, "utf-8")
modelParam = URLEncoder.encode(g.b, "utf-8")
manufParam = URLEncoder.encode(g.a, "utf-8")
url = f0/l.g(ctx)
    + "?ver="  + "3.0"
    + "&model="+ modelParam
    + "&manuf="+ manufParam
    + "&sign=" + signParam
body = f0/c.a(postString, key)                        # AES/ECB/PKCS5Padding(key, json)
ok = new f0/g().c(url, body, key) # The third parameter is immediately overwritten in f0/g and is not actually used.
```

Evidence and implementation details:
- `f0/c.d`：`MessageDigest("MD5").digest(str.getBytes("UTF-8"))`（`f0/c.smali:142-165`）。
- `f0/c.c`: `new f0/k()` → `AssetManager.open("pub.cer")` → `f0/k.b(InputStream)` Use `CertificateFactory("X.509")` to get `X509Certificate.getPublicKey()` → `f0/k.a(Key,[B)` to do `RSA/ECB/PKCS1Padding` encryption (`f0/c.smali:110-140`, `f0/k.smali:55-118`).
- `f0/c.b`：`Base64.encodeToString(b, 0, len, 0)`（`f0/c.smali:96-108`）。
- `f0/c.a`：`SecretKeySpec(key,"AES")` + `Cipher("AES/ECB/PKCS5Padding")` ENCRYPT_MODE（`f0/c.smali:7-47`）。
- **has no secondary encryption in the outer layer.**: The signature only protects the "key", and the text is encrypted in ECB single block mode, with no IV and no integrity check.

### 4.3 HTTP details (`f0/g.c`, `f0/g.smali:52`)

| item | value | Evidence |
|---|---|---|
| method | `POST` | `:100` |
| connect/read timeout | `10000` / `15000` ms | `:86-94` |
| `Content-Type` | `application/encrypted-json` | `:114-118` |
| `Content-Length` | `body.length` decimal | `:122-130` |
| Agreement | **plaintext `http://`** (default endpoint is `http://dm.wo.com.cn:18080/registerv3`) | `f0/f.smali:49` |
| Successful judgment | `responseCode == 200` | `:174-184` |
| Others | Failed to hit `"register fail!"`; `disconnect()` finally | `:187-196` |

### 4.4 Retry/failure handling (`d0/k.smali:255-358`)

- **has no retry, no backoff, and no upper limit of times.**: Single `f0/g.c`, `Exception` → `"sendPOSTRequest exception--->"` and then treated as failure.
- Special release: `f0/g.c` returns false and then reads `f0/g.b()` (response code). If `== 0x1a0` (**HTTP 416**), the result is regarded as success (`d0/k.smali:264-278`). The meaning is "the server thinks that the record already exists".
- Success → `f0/d.a()`: `DELETE FROM history` (clear queue, `f0/d.smali:110-138`).
- Failure → `g.M = null`, and insert this `g.d()` into the queue (`d0/k.smali:333-358`).
- At the beginning, the history is merged into the main text: `g.M = SDK>=28 ? f0/d.e() : f0/d.d()` (`d0/k.smali:118-141`, the former uses 20MB `CursorWindow` to avoid the single window limit, `f0/d.smali:407-417`).

### 4.5 Endpoint and interval constants (`f0/f.smali`)

| constant | value | meaning |
|---|---|---|
| `f0/f.w` | `http://dm.wo.com.cn:18080/registerv3` | Register/Report URL Default value |
| `f0/f.x` | `http://dm.wo.com.cn:18080/public/ip` | Public network IP echo (`f0/e.a()`, GET, 3s timeout) |
| `f0/f.y` | `0x5265c00` = 86,400,000 ms | `scheduleInterval` default 24h |
| `f0/f.z` | `0x240c8400` = 604,800,000 ms | `appInfoInterval` / UsageStats lookback window defaults to 7 days |
| `serverAddress` | prefs key, default = `f0/f.w` | Can be overwritten during runtime (`f0/l.t(Context,String)`, `f0/l.smali:740`) → **'s only "remote configuration" landing point is**, but there is no write call point in this package (see §9) |

Prefs file: `com_mobiletools_systemhelper_preference` (`f0/l.smali:160`); `f0/l.a()` will **directly delete the XML** (`/data/data/com.mobiletools.systemhelper/shared_prefs/...`, `f0/l.smali:39`).

---

## 5. `e0/r` base class contract (template method + shared ability)

`e0/r` implements `e0/c2$c` (`a(Context)Z` + `b(Context,d0/g)V`) and is the parent class of 63 manufacturers' readers. The instance field is only `private TelephonyManager a` (`e0/r.smali:10`, only the constructor is set to null, the whole class is not used - dead field).

### 5.1 Template method

| method | Semantics | Evidence |
|---|---|---|
| `a(Context)Z` | **roaming determines**. Base class implementation: `new ServiceState().getRoaming()` → constant `false` (vendor subclass overrides to true value) | `:477-489` |
| `b(Context,d0/g)V` | Template: Fill in the 6 static model fields first, then assign `m()/n()/o()/l()` according to SDK | `:491-558` |
| `c(Context,int)String` | Read VoLTE: `ImsManager.createForSubscriptionId(subId).getImsMmTelManager()` → `ImsMmTelManager.isAdvancedCallingSettingEnabled()` (reflected `Le0/h`). **side effects**: If it is false, first reflect call `setAdvancedCallingSettingEnabled(true)` and then reread. Only used by `l()` when `f0/f.c == true` | `:560-688` |
| `d(Context,int)String` | Read-only version of VoLTE (does not write system settings) | `:690-743` |
| `f(TelephonyManager,int)I` | Reflection `getNetworkType(slot)`, if it is 0, take `getVoiceNetworkType(slot)` | `:745-812` |
| `g(int,String)String` | Standard number → string mapping (see the table below) | `:814-985` |
| `i(int)[I` | Reflect `SubscriptionManager.getSubId(int)` and force `long[]` into `int[]` (return to `int[]` in case of exception) | `:987-1166` |
| `j(Context,int)Z` | `Settings.Global.getInt("preferred_network_mode"+slot, -1)` ∈ `[23,33]` → 5G Capability | `:1168-1240` |
| `l(Context,d0/g)V` | **API30+ mainstream implementation**: reflection `getDeviceId(slot)`/`getSubscriberId(slot)`/`getSimSerialNumber(slot)`/`getLine1NumberForSubscriber(slot)`/`getNetworkOperatorForSubscription`/`getNetworkType(slot)`; cell from prefs `"CellID0"/"LAC0"`; VoLTE go `c()` (China Unicom 46001/46006/46009) or `d()`; adjust `r()` for each card to supplement the cell | `:1242-1553` |
| `m(Context,d0/g)V` | **SDK<26 implements**: `getDeviceId()`/`getSubscriberId()`/`getSimSerialNumber()`/`getSimOperator()`/`getNetworkType()` + `getCellLocation()`→`g.f()`; `MSISDN1=""` | `:1555-1622` |
| `n(Context,d0/g)V` | 26~29 Implementation: `SubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0/1)` → `j/u = carrierName+number`, `h/s = iccid`, `B = defaultDataSubId+1`. **is not reachable in `b()`** | `:1624-1750` |
| `o(Context,d0/g)V` | **SDK≥26 implements** (log `"read26"`): `e0/i.a(tm,0/1)` takes IMEI1/2, `getActiveSubscriptionInfoForSimSlotIndex` takes number/iccid, `B = defaultDataSubId+1` | `:1752-1860` |
| `p(Context,d0/g)V` | API29/30 variant: the same set of reflections as `l()`, but the network standard is `f(tm,slot)` | `:1862-2254` |
| `q(Context,d0/g)V` | API30+ variant: `p()` + `f()` combination (manufacturer subcategory selection call) | `:2256-2612` |
| `r(Context,int,d0/g)V` | Use `CellIdentity` in `ServiceState.getNetworkRegistrationInfoList()` **`domain==2(PS) && transport==1(WWAN)`** Analyze cells: GSM (`getCid/getLac`), WCDMA, TD-SCDMA (reflection), CDMA (`%d:%d` lon:lat), LTE (`getCi/getTac`), NR (reflection `getNci/getTac`); press `int slot` writes `m/n` or `x/y` | `:2614-2948` |

### 5.2 version/vendor branch (actual behavior of `b()`)

```smali
# e0/r.smali:491-558 (line-by-line reconstruction)
b(ctx, g):
  g.b = Build.MODEL; g.a = Build.MANUFACTURER; g.e = "Android"
  g.f = Build.VERSION.RELEASE; g.d = Build.DISPLAY
  g.c = SystemProperties("ro.hardware.version", null)
  if (SDK_INT < 26)  m(ctx,g)          # :530
  else               o(ctx,g)          # :544   ← all API levels 26 and above take this branch
  # The next two branches are unreachable because the previous branch jumps to the end:
  #   if (SDK_INT < 26) n(ctx,g)       # :537 unreachable
  #   if (SDK_INT >= 30) l(ctx,g)      # :553 unreachable
```
→ The base class `b()` has only two paths **to** at runtime: `SDK<26 → m()`, `SDK≥26 → o()`; `n()/l()` is explicitly called by the manufacturer's subclass (such as `e0/c1/e0/f1/e0/g0/e0/m0/e0/m1/e0/s1/e0/y/e0/z1/r0` calls `l()`, `e0/b1/e0/e1/e0/i1/e0/k0/e0/l1/e0/n1/e0/q0/e0/r0/e0/s1/e0/t/e0/v/e0/x1` calls `p()` / `q()`). A total of **62 subclasses override `b()`**.

### 5.3 SDK branch list (all version determination points)

| Judgment | location | behavior |
|---|---|---|
| `SDK_INT <= 33(0x21)` | `h()` `:114` | Reflection `getSubId(int)`, failure returns `-1` |
| `SDK_INT > 33` | `h()` `:153` | Reflection `getSubscriptionId(int)` |
| `SDK_INT >= 31(0x1f)` | `k()` `:233`、`s()` `:393/428` | `TelephonyCallback` (`d0/f`, via `e0/k.a`/`e0/d.a`) otherwise `PhoneStateListener` (`d0/e`) |
| `SDK_INT < 26` / `>= 26` | `b()` `:528/542` | `m()` / `o()` |
| `SDK_INT >= 28` | `d0/k:118`、`d0/j:48` | `f0/d.e()` (Large CursorWindow)/`f0/m.b()` |
| `SDK_INT >= 31` | `e0/c2.h:367-373` | First register `e0/r.k(ctx)` to monitor |

### 5.4 Key name rules/prefixes

Conclusion: The JSON key of **`d0/g` is a hard-coded English literal without any prefix to splice** (`Manuf/Model/HWVersion/…/History`, see §2 table).
The two "prefixes" mentioned in the task are actually the **log prefix**, not the key name prefix:

| string | location | nature |
|---|---|---|
| `"deviceinfo.signalinfo:"` | `e0/c2.smali:326` | The value of the **Log prefix** + `g.N` of the `"R"` branch in `e0/c2.g()` |
| `"apilevel:"` | `e0/q1.smali:2101` | Samsung reader constructs Log message prefix (`"apilevel:"+n`), which has nothing to do with JSON |

log tag List (for easy full search): `DeviceInfoReaderBase`, `DeviceManager`, `DMPhoneStateListener`, `DMTelephonyCallback`, `DeviceInfo`, `RegisterThread`, `RegisterIntentService` , `RegisterHelperService`, `HttpEncryptHelper`, `RSAHelper`, `GetIPAddressThread`, `GetIPAddressHttpHelper`, `AppInfo`, `SignalDatabaseHelper`, `RegisterUtils`.

### 5.5 Reading and writing relationship between `e0/r` and `d0/g` (field ownership)

- **only writes**: `a,b,c,d,e,f` (model static information, `b()` writes all at once)
- **reads + writes**: `g,h,i,j,k,l,m,n,o,p` (slot1) and `r,s,t,u,v,w,x,y,z,A` (slot2) - Note that `d0/g.f()/g(CellLocation)` and `e0/r.r()` will write the same batch of cell fields (`m/n/x/y`), the latter is covered in the back row in `l()/p()/q()`
- **is write only (this layer is not read)**: `B` (`e0/r.n/o` is write, `f0/l.m()` and `d0/g.d()` are read), `H` (`c2.g` is write parameter), `C` (`c2.n` Write), `J` (`c2.n` writes `versionName`), `K` (`c2.n` writes `f0/l.d()`, and is read by `d0/f` as `time` key), `L` (`c2$a` Write), `M` (Write `d0/k`), `N` (Write `d0/j`)
- **`e0/r` does not read or write**: `F` (`SubType`, specially written by the manufacturer's collector), `G` (`DeviceType`), `I` (`SpecVersion`, `d()` Self-assigned), `O` (internal cache), `P/Q` (constant)

---

## 6. `e0/c2` Orchestrator ("DeviceManager")

`e0/c2` is not the scheduler of 85 collectors, but the selector of **device information reader (vendor reader) + registration process orchestrator + reporting trigger**. The collector (`e0/z1`, etc.) is called separately by the shell layer without going through `c2`.

### 6.1 Static state

| Field | Type | Purpose |
|---|---|---|
| `a` | `Map<String,String>` | `Build.MODEL → Brand` coverage table (**304**, lazy loaded in `i()`) |
| `b` | `e0/c2$c` | Selected reader instance cache (**singleton**, `h()` returns directly after hit) |
| `c` | `List<f0/b>` | The latest AppInfo list (written by `c2$a` for secondary use) |
| `d` | `d0/i$a` | `new e0/c2$a()` (UsageStats completion callback) |
| `e` | `d0/h$a` | `new e0/c2$b()` (IP completion callback) |

### 6.2 `i()`: Full mapping of `Build.MODEL → Brand` (`e0/c2.smali:1370-3391`)

- Lazy loading: `if (a != null) return;` (`:1373-1375`), `new HashMap()` immediately followed by `sput` (`:1377-1381`).
- Read `ro.product.manufacturer` (`f0/l.i`) first, and map the current `Build.MODEL` to the manufacturer value **(`:1496`, `:1689`) for `vivo / samsung / realme / xiaodu / DOUQI` Category 5**.
- Then there are 302 consecutive `map.put(model, brand)`. Brand distribution:

| brand | Number of items | brand | Number of items | brand | Number of items |
|---|---|---|---|---|---|
| Xiaomi | 117 | OPPO | 92 | vivo | 20 |
| realme | 13 | Hisense | 7 | PTAC | 7 |
| samsung | 6 | Lenovo | 4 | ZTE | 4 |
| Liantong | 4 | nubia | 3 | OnePlus | 3 |
| WIKO | 3 | TDTECH | 3 | meizu | 2 |
| motorola/TCL/Coolpad/gionee/HUAWEI/KONKA/EWPE/YIK/HZTL/JingLing/TOSOT/5G HYUN/H10/sprd | 1 each | | | | |

- Full 304 rows see `findings\c2_map_a_full.tsv` (column: `smali_line / model / brand`).

### 6.3 `h(Context)`: brand → reader class (62-way if-else, `:364-1368`)

First register at the entrance: `if (SDK_INT >= 30) e0/r.k(ctx)` (`:367-373`). Then `a.get(Build.MODEL)`: If it hits, compare 62 ways according to the brand string (`equals`/`equalsIgnoreCase` mixed), if it doesn’t hit, it will be directly `new e0/r()`.

| Brand (case sensitive or not) | reader | OK |
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
| KONKA / "konka" (ignore case) | `e0/o0` | 691 / 1113 |
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
| Others | `e0/r` (base class) | 1319 |
| Mapping table miss | `e0/r` + log `"getReader error!! manufacture==null! model="` | 1341-1360 |

> Note: Only three readers, `nubia(→e0/z0)`, `EWPE(→e0/c0)`, and `TOSOT(→e0/e0)`, need to be bound to the manufacturer's system services (see §7), and the remaining readers are purely local reflection.

### 6.4 Registration process: `f()` / `g(ctx,String)`

`f(Context)` (`:139`, `synchronized`) - "Full collection + application statistics":
```
i(); g = new d0/g(); n(ctx,g); l(ctx,g); m(ctx,g); o(g); new d0/i(ctx,g,c2.d).start()
```

`g(Context,String regType)` (`:189`, `synchronized`) - `RegType` determines the subsequent actions:
```
g = new d0/g(); g.H = regType; n(ctx,g)
if (regType.trim()=="T") { if (!l && !m) { log "SIM not changed, not post!"; return g } } # Only report SIM changes
else { l(ctx,g); m(ctx,g); } # Go unconditionally
o(g)
f0/f.g = e(ctx) # device_provisioned → Report to main switch
if (regType.trim()=="A") { if (f0/f.g) { new d0/i(...).start(); goto done } }
if (regType.trim()=="R") { new d0/j(...).start(); log "deviceinfo.signalinfo:"+g.N; goto done }
new d0/k(ctx,g).start() # Don’t worry: report directly
```
`RegType` literal source (shell): `AppInfoRegisterReceiver="A"` (`:81`), `SIMStateReceiver="T"` (`:229`), `ScheduleRegisterReceiver="S"` (`:67`), `OnBootReceiver="B"` (`:367`), `RegisterIntentService` takes the Intent extra `"RegType"` and `toUpperCase()`, if empty, go to `d0/g.a(ctx)` (`:195-263`). **`"R"` has no caller** in this package (`d0/j` is therefore actually a dead path).

### 6.5 List of conditional switches

| switch | Judgment | location |
|---|---|---|
| The device has been initialized (the user has passed the SetupWizard) | `Settings.Global.getInt("device_provisioned",0)==1` | `e0/c2.e()` `:96-137` |
| Whether to report/write disk | `f0/f.g` (`device_provisioned`, many writing points: `e0/c2:272`, `c2$a:48`, `b()` from each reader) | `d0/k.run()` `:596`；`c2$a` `:48` |
| Whether to collect IP | `f0/f.b` —— **has no writing point, constant false** → `d0/h` does not actually start | `c2$a` `:141` |
| Does it include signal history? | `f0/f.e` —— **has no writing point, constant false** → `SignalInfo` does not enter JSON | `d0/g.d()` `:949`、`d0/f` `:520` |
| Does VoLTE use the "Change System Settings" version? | `f0/f.c` —— **has no writing point, constant false** → constant read-only `e0/r.d()` | `e0/r.l()` `:1372` |
| Is it connected to the Internet? | `f0/l.m(g)`（`DataSlot` + 46001/46006/46009） | `d0/k.d()` `:488` |
| SIM1/SIM2 whether to replace | prefs `iccid1`/`iccid2` vs. `g.h`/`g.s` | `c2.l()` `:3443`、`c2.m()` `:3580` |
| Is it 5G capable? | `preferred_network_mode{slot}` ∈ [23,33] | `e0/r.j()` |
| Whether it is VoLTE (exclusive judgment for China Unicom) | Operator ∈ {46001,46006,46009} → `c()` (writable system settings), otherwise `d()`; subject to the quadratic limit of the previous line `f0/f.c` | `e0/r.l()` `:1372-1425` |
| permission gate | `READ_PHONE_STATE` (multiple places), `READ_PHONE_NUMBERS`, `ACCESS_FINE/COARSE_LOCATION`, `ACCESS_NETWORK_STATE` | `e0/r.l/r/c`、`d0/e/f`、`c2.n` |

### 6.6 Effective path vs dead branch (measured semantic correction)

Among the 6 Boolean switches of `f0/f`, **only has `a` (agreement agreement), `g` (device_provisioned), `u`/`v` (SIM/network status),** has `sput` Writing point; `b`, `c`, `d`, `e`, `t` **has no** written in the entire database (`f0/f.smali` only has an initial value). Therefore, the following "design path" does not hold when **runs this version**:

| branch | Trigger condition | actual results |
|---|---|---|
| `d0/h` (IP thread) | `f0/f.b == true`（`c2$a:141`） | Never created → `IPv4/IPv6` will never appear in JSON; IPv4/IPv6, `d0/h$a`, and `c2$b` of `d0/h` are all dead code |
| `d0/f.onSignalStrengthsChanged` whole process | `f0/f.e == true`（`d0/f:520`） | Return immediately → Signal JSON, fused positioning, and `d0/f$a` callback chain are not triggered |
| `SignalInfo` key | `f0/f.e == true`（`d0/g.d:949`） | Do not write |
| `e0/r.c()` (write VoLTE switch) | `f0/f.c == true`（`e0/r.l:1372`） | Hengzao `e0/r.d()` (read-only), do not modify system settings |
| `d0/j` + `SignalInfo` loading | `RegType == "R"` | There is no `"R"` sender in this package → Unreachable |
| `e0/r.n()` (API26 path) | `SDK_INT < 26`（`e0/r.b:537`） | Previous article `SDK<26` has become `goto` → unreachable |

→ **The actual reporting link** in this version is: `e0/c2.g()` assembly `d0/g` (model/SIM/cell/standard/VoLTE/5G/DataSlot/AccType/Time/RegVersion) → (`A` branch and then superimposed `AppInfo`) → `d0/k` direct `POST` to `http://dm.wo.com.cn:18080/registerv3`, 35 fixed keys in the text (plus condition keys: `IPv4/IPv6` is subject to `f0/f.b`, `SignalInfo` is subject to `f0/f.e`, `History` is subject to `g.M != null`, the upper limit is 39 keys), **actually does not contain IPv4/IPv6/SignalInfo**; if failed, it will fall to `systemhelper.history` (≤100 entries) and wait for the next resend.


### 6.7 Exception handling and "whether initialized" flag

- The `c2.h()` result is cached in `c2.b`; **does not have the "initialized" Boolean value of**, and `i()` relies on `a == null` to determine the first time.
- All `f/g/n/l/m/o` are `synchronized` (class locks), `d/e/f/g` of `d0/g` are instance locks → there is a **class lock + instance lock** double lock structure.
- There are a large number of `try/catch Exception → printStackTrace()` internally (`e0/r.p/q` is one segment per field), and the failure is silently downgraded to field default.
- `PackageManager$NameNotFoundException` in `n()` is captured separately (`:3743`).
- `d0/g.a/b` is captured and unified to `log "DeviceInfo"` and **returns null** (the caller does not detect null → potential NPE).

### 6.8 Remote configuration delivery

**does not have remote configuration parsing logic**. The only "remotely rewriteable" landing point is `serverAddress` (prefs, `f0/l.t()` writes, `f0/l.g()` reads, `d0/k` uses it to spell URL), but there is no caller in APK `f0/l.t(` → the actual default is always `http://dm.wo.com.cn:18080/registerv3`. Except for the status code **in the server response body,** is not read (`f0/g.c` only takes `getResponseCode()`), so there is no dynamic delivery/instruction channel based on the response.

---

## 7. AIDL system service and device identification acquisition

Both groups of AIDL are **manufacturer's proprietary Telephony extension service**, used to obtain IMSI/IMEI/ICCID/number/network format on the device where "standard API is castrated by ROM".

### 7.1 `b0/a` —— Nubia (ZTE/nubia) `INBExtTelephony`

- Interface: `b0/a` (`extends IInterface`, 10 methods), Stub `b0/a$a`, Proxy `b0/a$a$a`
- Descriptor: `"com.android.phone.nubia.INBExtTelephony"` (`b0/a$a.smali:37`; every method `writeInterfaceToken` in Proxy uses it)
- Transaction number (`b0/a$a$a.smali`, extracted by method `const/16`):

| affairs | method | signature |
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

- Special mechanism: When Proxy returns false in `transact()` (the service does not exist/is dead), **falls back to the static field `b0/a$a;->b:Lb0/a`** (local implementation), and `b0/a$a;->x()` exposes this field (`b0/a$a.smali:61-67`). That is, "remote failure → local stub".
- Server components: `com.android.phone/com.android.phone.nubia.NBExtTelephonyService` (`RegisterIntentService.b()`, `RegisterIntentService.smali:104-116`, `bindService(..., BIND_AUTO_CREATE=1)`).
- The binding result is written to `f0/f.A` (`onServiceConnected` of `RegisterIntentService$a`: `sput-object p1, Lf0/f;->A`, `RegisterIntentService$a.smali:38/48`).
- Consumer: `e0/z0` (nubia reader) for full use `f0/f.A`: `v()` (number of cards), `q(slot)`, `t(slot)`, `k(slot)`, `e(slot)`, `b(slot)`, `c(slot)`, `a(slot)`, `d(slot)`, `f(slot)` (`e0/z0.smali:70-330`).

### 7.2 `c0/a` —— Gree `IGreeTelephonyManager`

- Interface: `c0/a` (12 methods), descriptor `"com.gree.telephonymanager.IGreeTelephonyManager"` (`c0/a$a.smali:37`)
- Transaction number:

| affairs | method | signature |
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

- Server component: `com.gree.telephonymanager/com.gree.telephonymanager.GreeTelephonyManagerService` (`RegisterIntentService.a()`, `RegisterIntentService.smali:58-70`)
- The binding result is written to `f0/f.B` (`RegisterIntentService$b.smali:44/60`)
- Consumer: `e0/c0` (`e0/c0.smali:60-353`) and `e0/e0` (`e0/e0.smali:85-310`), usage mode: `p()/h()/n()/l()` gets "card number/current card", `m(slot,"","")` gets string (suspected IMSI/ICCID/number), `e/b/c` Take the three types of identification, `a/d/g` takes the integer type (signal/format), `j(slot)` takes the Boolean type.

### 7.3 Binding timing and preconditions

`com.mobiletools.systemhelper.service.RegisterIntentService`（`IntentService`，tag `"RegisterIntentService"`）：

1. `onCreate()` (`:123`): `e0/c2.i()` → `e0/c2.h(ctx)` select reader; **only** reader is `e0/z0` (nubia) → `b(ctx)` tied to nubia; is `e0/c0` or `e0/e0` (Gree) → `a(ctx)` tied to gree. Other brands **are not bound to**.
2. `onHandleIntent()` (`:192`): Get `RegType`; then press the reader type **to spin and wait for 5ms** until `f0/f.A` (nubia) or `f0/f.B` (gree) is not null (`Thread.sleep(5)` infinite loop, no timeout); after getting it:
   - `regType == null` → `d0/g.a(ctx)` (and type `"onStartCommand-->regType==null"`)
   - Otherwise → `d0/g.b(ctx, regType.toUpperCase())`
   - Can’t get it → `"onHandleIntent-->iNBExtTelephony==null"` / `"onHandleIntent-->iGreeTelephony==null"`
3. No `unbindService` (`onDestroy` only `invoke-super`, `ServiceConnection` leaks).

### 7.4 Obtained device identification (panorama)

| logo | Field | main channel | Bottom line/manufacturer channel |
|---|---|---|---|
| IMEI1 / IMEI2 | `g.g` / `g.r` | Reflection `getDeviceId()/(int)` (`e0/d2.b`, `e0/r.p/q`) or `e0/i.a(tm,slot)` (`e0/r.o/l`) | `e/b/c/k/t` for nubia `b0/a`; `e/b/c/m` for gree `c0/a` |
| IMSI1 / IMSI2 | `g.i` / `g.t` | `getSubscriberId()` / `getSubscriberId(int)` (reflective) | Same as above |
| ICCID1 / ICCID2 | `g.h` / `g.s` | `getSimSerialNumber()`、`SubscriptionInfo.getIccId()` | Same as above |
| MSISDN1 / MSISDN2 | `g.j` / `g.u` | `getLine1Number()`、`getLine1NumberForSubscriber(slot)`、`SubscriptionInfo.getNumber()` | Same as above |
| Operator | `g.k` / `g.v` | `getNetworkOperator()` / `getNetworkOperatorForSubscription(slot)`; <26 use `getSimOperator()` | — |
| Cell | `g.m/n`、`g.x/y` | `getCellLocation()`、`getNetworkRegistrationInfoList()`+`CellIdentity*`、prefs `"CellID0/1"`、`"LAC0/1"` | — |
| Serial number (Build.SERIAL) | Not used | — | — |
| Android ID / OAID / MAC | Not used | — | — |

---

## 8. Key smali snippet appendix

### 8.1 Report URL assembly (`d0/k.smali:184-246`)
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

### 8.2 Text encryption + submission (`d0/k.smali:169-260`)
```smali
invoke-static {v0}, Lf0/c;->d(Ljava/lang/String;)[B        # MD5(json) → key
move-result-object v2
invoke-static {v3, v2}, Lf0/c;->c(Landroid/content/Context;[B)[B   # RSA(key)
invoke-static {v3}, Lf0/c;->b([B)Ljava/lang/String;        # Base64
invoke-static {v0, v2}, Lf0/c;->a(Ljava/lang/String;[B)[B   # AES(json,key) → body
...
invoke-virtual {v4, v3, v0, v2}, Lf0/g;->c(Ljava/lang/String;[B[B)Z
```

### 8.3 416 when successful (`d0/k.smali:264-282`)
```smali
if-nez v0, :cond_1
invoke-virtual {v4}, Lf0/g;->b()I
move-result v1
const/16 v2, 0x1a0
if-ne v1, v2, :cond_1
const/4 v0, 0x1
```

### 8.4 Slot normalization (IMEI, the smaller one is slot1) (`e0/c2.smali:3957-4109`)
```smali
iget-object v1, p0, Ld0/g;->g:Ljava/lang/String;   # IMEI1
iget-object v2, p0, Ld0/g;->r:Ljava/lang/String;   # IMEI2
invoke-virtual {v1, v2}, Ljava/lang/String;->compareTo(Ljava/lang/String;)I
if-lez v1, :cond_1                              # IMEI1 > IMEI2 → swap
  Create a temporary d0/g object and swap the two sets of 10 fields.
  Also swap DataSlot "1" and "2".
```

### 8.5 Non-China Unicom operators → mark MD5 (`e0/c2.smali:4147-4274`)
```smali
iget-object v1, p0, Ld0/g;->k:Ljava/lang/String;    # carrier code
invoke-static {v1}, Lf0/l;->n(Ljava/lang/String;)Ljava/lang/Boolean;   # ∈{46001,46006,46009}?
if-nez v1, :cond_2                                  # carrier other than China Unicom
  g.h = MD5(g.h); g.j = MD5(g.j); g.i = MD5(g.i); g.m = MD5(g.m); g.n = MD5(g.n)
  g.s = MD5(g.s); g.u = MD5(g.u); g.t = MD5(g.t); g.x = MD5(g.x); g.y = MD5(g.y)
```

### 8.6 Listener registration (`e0/r.smali:233-259`)
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

### 8.7 UsageStats sorting (`d0/i$b.smali:46-62`)
```smali
invoke-virtual {p2}, Landroid/app/usage/UsageStats;->getTotalTimeInForeground()J
invoke-virtual {p1}, Landroid/app/usage/UsageStats;->getTotalTimeInForeground()J
sub-long/2addr v0, p1
long-to-int p1, v0          # ← descending order with long-to-int truncation (overflow above 2^31 ms)
```

### 8.8 Mobile traffic is attributed according to IMSI (`d0/i.smali:185-196`)
```smali
iget-object v3, v1, Ld0/g;->i:Ljava/lang/String;    # IMSI1
iget-wide v4, p0, Ld0/i;->c:J                       # start
iget-wide v6, p0, Ld0/i;->b:J                       # end
invoke-virtual {v1, v2, v3, v4, v5, v6, v7}, Landroid/app/usage/NetworkStatsManager;->querySummary(ILjava/lang/String;JJ)
# v2 = 0 (TYPE_MOBILE)，then accumulate only when bucket.getUid() equals this app’s UID
```

---

## 9. Unknown items/to be confirmed

1. **`f0/m` (signal DB) has no writer**: `d0/f.smali:946-956` only takes the instance and discards the return value; there is no insert call for `Lf0/m;->` in the entire library. Therefore, `g.N` (`SignalInfo`) in practice can only come from `d0/j`. A query on the empty table → `String.valueOf(null JSONArray)` will get the literal `"null"` (`d0/j.smali:58`). Whether there are also third-party write paths (such as shell direct SQL) has not been verified.
2. **`"R"` RegType No caller**: `d0/j` (historical signal) is not reachable in this package; whether there is an external Intent/ADB trigger (`RegisterIntentService` accepts any `RegType` extra) is not confirmed - whether `RegisterIntentService` is exported needs to be checked Manifest (outside the scope of this layer).
3. **`e0/c2.b` singleton never fails**: After the reader is selected, it will not be recalculated across SIM change/model unchanged scenarios; `o()`'s slot exchange is a stateful side effect. Whether calling `d0/g.b()` multiple times will result in repeated exchanges (idempotence) has not been verified on a real machine.
4. **`site`/Server-side semantics**: `HTTP 416` is regarded as successful and there is no resolution beyond 200, indicating that the protocol is not documented; the relationship between `ver=3.0`, `SpecVersion=3.0`, and `RegVersion=versionName` can only be inferred from the words.
5. **`f0/f` Multiple switches have no write points**: `sput-boolean ... Lf0/f;->b/c/d/e/t` has zero hits in the entire library (only static initial values), and only the ones with writes `a` (`TestActivity:111`/`OnBootReceiver:50`/`RegisterHelperService$a:119`), `g` (`e0/c2:272`, etc. 30+ places), `u` (`SIMStateReceiver`), `v` (`ConnectStateReceiver`). Conclusion: IP collection, signal history, and VoLTE writing are "compiled in but turned off" in this version. Whether the original design is left to be switched on the server side or has been cropped cannot be determined from the bytecode (there is no Prefs/network to read the codes of the three keys).
6. **`e0/r.l()` Medium `setAdvancedCallingSettingEnabled(true)`**: A common path that can only be taken by models other than nubia/gree (when using China Unicom card), it belongs to "Silently Modify System VoLTE Switch"; whether it can really succeed depends on the IMS permissions, which cannot be determined at this level.
7. **`d0/e` has no signal strength callback** (`onSignalStrengthsChanged` is not overwritten) on API<31, and `f0/f.c` is only assigned in `d0/f` → `e0/r` The integrity of `NCLS`/signal history is unknown on older systems.
8. **`b0/a` and `c0/a` The semantics of `v()/q()/t()/k()/e()/b()/c()/a()/d()/f()`** can only be inferred from the calling sequence and context (`e0/z0` uses `t/k/e/b/c` as IMSI/ICCID /number/standard), AIDL has no `@` comment or string constant for confirmation.
9. **`e0/r.h()` uses `SDK_INT <= 33` branch**: Android 14(34)+ uses `getSubscriptionId`, but the API is `@SystemApi`/hidden interface. Whether the reflection can succeed depends on ROM; failure returns `-1` → Follow-up `createForSubscriptionId(-1)` behavior is not verified.
10. The relationship between **`e0/c2` and 85 collectors**: This layer does not see `c2` calling any `e0/z1` collector (only `c2` in `e0/*.smali` calls each other internally). The scheduling entry of the collector is in the shell layer (not within this scope), and needs to be cross-verified with the shell analysis conclusion.
11. The two final constants `d0/g.P/Q`, the `d0/g.q` field, and `d0/f.d` (`List<f0/n>`) are all dead codes that **only declares/assigns values ​​and does not read**.
12. Resource boundaries such as the upper limit of 100 rows in the `f0/d` table and the 20MB window in the `f0/m` table were only observed on the `d0/k`/`d0/j` side, and no real machine stress test was performed.

---

### Appendix A: `RegType` semantic quick check

| RegType | sender | `e0/c2.g()` behavior |
|---|---|---|
| `A` | `AppInfoRegisterReceiver` | If `device_provisioned` → go to `d0/i` (application usage + traffic) → then `d0/h`/`d0/k`; otherwise `d0/k` |
| `S` | `ScheduleRegisterReceiver` | Directly `d0/k` (periodic reporting) |
| `B` | `OnBootReceiver` | Directly `d0/k` (report at startup) |
| `T` | `SIMStateReceiver` | Only continue if `iccid1/iccid2` changes, otherwise `"SIM not changed，not post！"` returns directly |
| `R` | (Not included in this package) | After `d0/j` loads the signal history to `g.N`, **does not start to report** (only log `deviceinfo.signalinfo:`) |
| `null` | `RegisterIntentService` | Go `d0/g.a(ctx)` → `e0/c2.f()` (full collection + `d0/i`) |

### Appendix B: Placement

| Data | location | Evidence |
|---|---|---|
| To be reported JSON queue | SQLite `systemhelper` → `history(id INTEGER PK AUTOINCREMENT, jsonString TEXT)` | `f0/d.smali:496` |
| Signal history | SQLite `signalhelper` → `signal(id, jsonString)` | `f0/m.smali:272` |
| Various switches/ICCID snapshot/server address | SharedPreferences `com_mobiletools_systemhelper_preference`（`iccid1`、`iccid2`、`serverAddress`、`appInfoInterval`、`scheduleInterval`、`signalInfoInterval`、`CellID0/1`、`LAC0/1`） | `f0/l.smali:160/146/132/58/743`、`e0/r.smali:2044/2226` |
| public key | `assets/pub.cer` (X.509, RSA public key, used to encrypt MD5 digest) | `f0/c.smali:125-131` |
