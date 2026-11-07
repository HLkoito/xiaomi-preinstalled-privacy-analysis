> **Historical analysis.** This archive includes observations that remain unverified or were later corrected. See the [final report](../REPORT.md) and [independent review](../REVIEW.md) for the reviewed findings on upload scope, consent, local FBO delegation, emergency SMS encryption, and provider authorization. Source references retain their original decompilation paths. The publication includes the selected files under evidence/.

# Network/Traffic/Firewall · Early Warning Center · Earthquake Early Warning · Electricity Risk - Module Reverse Report

> Target: `MIUISecurityCenter-10.4.9-250402.0.1.apk` (`com.miui.securitycenter`, versionName 10.4.9-250402.0.1, versionCode 40001049)
> Method: Static reading of full jadx Java source code + AndroidManifest + apktool resources + assets binary; **does not dynamically run**, and all "runtime behaviors" are code path inferences.
> Note: `Observed`=shipped artifact is directly visible; `Inferred`=reasonably extrapolated from the code path; `Unknown`=retrieval by keyword has failed or requires dynamic verification.
> Evidence format: `class name.java:line number` (the line number corresponds to the file under `_analysis/jadxout/sources/`).

---

## 0. Module map (scope of this report)

| package | Number of files | Responsibilities (one sentence) | main process |
|---|---|---|---|
| `com.miui.networkassistant` | 417 | Network Assistant: Traffic Statistics/Package/Network Control (Firewall)/Dual SIM/Roaming/Hotspot/Traffic Package Purchase/VPN Management | Main process + `:remote` |
| `com.miui.warningcenter` | 81 | Early warning center: 12379 disaster warning subscription and push, Mijia early warning, police assistance (policeassist) | Main process + `:remote` |
| `com.miui.earthquakewarning` | 96 | Earthquake early warning: MiPush early warning display, subscription cities, volunteer sensor reporting, SOS countdown sound | Main process + `:remote` |
| `com.miui.electricrisk` | 27 | ⚠️ Named "Electricity Risk", it is actually **AI anti-fraud protection** (not related to battery/charging, see §4) | `:remote` |
| `com.miui.simlock` | 25 | SIM card lock query/setting/PIN and power-on monitoring (see §5) | `:bootaware` / `:remote` |
| `com.miui.bubbles` | 49 | Notification bubble (see §6) | `:ui` / `:remote` |
| `com.miui.monthreport` | 4 | Monthly Report Data Provider (see §7) | `:remote` |
| `com.miui.net` | 2 | Thin encapsulation for `miui.securitycenter.net.*` (traffic session/background restriction policy) | main process |
| `com.miui.vpnsdkmanager` | 1 | One AIDL interface (see §8) | — |
| `com.miui.wakepath` | 2 | Chain start "whether jump is allowed" confirmation page (see §8) | main process |
| `com.miui.analytics` / `com.xiaomi.analytics` / `com.xiaomi.onetrack` | 10 / — / 179 | Burial point (see §9) | main process |

**Key prerequisite facts (observations)**

- In the entire Java code of APK, **does not have** `iptables` / `ip6tables` strings, **does not have** `Runtime.exec` execution commands (only xcrash, onetrack capture logcat, `getprop` Three unrelated uses), and **does not directly call** `NetworkManagementService` → see `§1.4`.
  - Evidence: Whole-tree grep `iptables|ip6tables|Runtime\.getRuntime\(\)\.exec|ProcessBuilder` only hits `xcrash/j.java:288`, `com/xiaomi/onetrack/util/b.java:296`, `miuix/animation/utils/CommonUtils.java:273` (`getprop`).
- `libtcp.so` **is not the** firewall/traffic-control library, but the SMS template parsing engine of the "traffic calibration" operator (see `§1.5`). The only loading point is: `com/miui/sdk/tc/TcManager.java:116` `System.loadLibrary("tcp")`.
- `libnap.so` has the only loading point: `traffic/statistic/NaTrafficStats.java:19`, providing `nativeGetUidIfaceStat(uid, iface, type)` (`NaTrafficStats.java:69`).

---

## 1. `com.miui.networkassistant` (network assistant/network control)

### 1.1 Function list (observation, evidenced by code + Chinese copywriting)

| # | User visible features | Code entry | Documentary evidence |
|---|---|---|---|
| 1 | Network Assistant Home Page (Traffic Overview Card) | `ui/NetworkAssistantActivity`（`miui.intent.action.NETWORKASSISTANT_ENTRANCE`） | `app_name_na`=「Network Assistant」(`strings.xml:516`) |
| 2 | Traffic ranking (by App/by time/by front and backend) | `ui/activity/TrafficSortedActivity` + `ui/fragment/TrafficSortedFragment` | `usage_sorted_activity_lable`=「Traffic ranking」(`:6512`) |
| 3 | Traffic package settings (total/used/remaining/monthly settlement date) | `ui/fragment/PackageSettingFragment`、`PackageSettingGuideFragment`、`fragments/TemplateSettingFragment` | `NETWORKASSISTANT_MONTH_PACKAGE_SETTING` (`Manifest:1026`) |
| 4 | Operator/province/city template (calibration SMS template) | `ui/fragment/OperatorSettingFragment` | `NETWORKASSISTANT_OPERATOR_SETTING` (`Manifest:1038`) |
| 5 | Flow Calibration (SMS/Web/Manual) | `traffic/correction/**`、`TcSmsReportService` | — |
| 6 | Traffic reminder (threshold reminder, over-limit network disconnection, lock screen traffic) | `ui/fragment/TrafficReminderSettingFragment`、`TrafficLimitSettingFragment`、`LockScreenTrafficFragment` | `NETWORKASSISTANT_LIMIT_SETTING` (`Manifest:1042`) |
| 7 | Daily rental card (daily limit/daily rental card special) | `ui/fragment/DailyCardSettingFragment`、`config/DailyCardBrandConfig` | — |
| 8 | Network control (mobile/WLAN/roaming three rule tables) | `ui/activity/FirewallActivity`、`FirewallListFragment`、`MobileFirewallFragment`、`WlanFirewallFragment`、`RoamingWhiteListFragment`、`SystemAppFirewallFragment` | `firewall_activity_lable`=「Network Control」(`:1999`) |
| 9 | Default networking policy for newly installed applications | `ui/fragment/NewInstalledPreSettingFragment`（`pref_key_new_installed_app_firewall_mobile1/2/_wifi`） | `:23-25` |
| 10 | Background networking restrictions (BG networking) | `firewall/BackgroundPolicyService` + `ui/fragment/BgNetworkAppListFragment` | — |
| 11 | traffic saving | `ProviderConstant.NASettingsInfoColumns.TRAFFIC_SAVING_*` (`na_settings_info` table) | — |
| 12 | Hotspot/shared traffic limits and over-limit handling | `service/tm/TetherStatsManager`、`ui/activity/TetheringStatsSettingActivity`、`TetherStatsOverLimitActivity` | `pref_key_tethering_setting`=「Single hotspot traffic limit」(`:4742`) |
| 13 | International roaming (roaming switch/whitelist/roaming reminder) | `ui/activity/InternationalRoamingSettingActivity` | `international_roaming_setting`=「Roaming Settings」(`:3003`) |
| 14 | Data package purchase (recharge/order/refund/payment status) | `traffic/purchase/PurchaseUtil`、`ui/order/**`、`ui/revert/RevertResultActivity`、`ui/GetChargeTrafficActivity` | `bh_charge_traffic`=「Charging capacity」(`:958`) |
| 15 | Operator traffic/call balance inquiry (SMS channel) | `service/tm/TrafficSimManager.checkBillRemainder/checkBillArrears` | — |
| 16 | MIUI VPN Management (including Xunyou xunyou) | `vpn/miui/MiuiVpnManageService`（exported=true） | `MIUI_VPN_MANAGE_SERVICE` (`Manifest:1070-1072`) |
| 17 | Status bar shows real-time traffic | `CommonPerConstants.KEY.STATUS_BAR_SHOW_TRAFFIC` | `NETWORKASSISTANT_STATUS_BAR_SETTING` (`Manifest:1034`) |
| 18 | Traffic statistics exception prompt (qtaguid is not enabled) | `utils/NotificationUtil.sendNetworkStatsExceptionNotify` + `ui/activity/NetworkStatsExceptionAlertActivity` | `R.string.exception_titile` |
| 19 | Network diagnosis (**only prompts page**) | `ui/activity/NetworkDiagnosticsTipActivity` | `network_diagnostics`=「Network Diagnosis」(`:3469`) |
| 20 | Virtual SIM / meter SIM (SeedSim, virtualsim provider) | `utils/VirtualSimUtil`、`model/VirtualSimInfo` | — |

**"Network Acceleration" Conclusion (Observation + Inference)**: There is no **network acceleration/dual WiFi/link aggregation implementation in APK** - there is no `*cceler*` business class (only animation interpolation class) in the whole tree, and no related Service/permissions. `Inferred`: Network acceleration is undertaken by other system components (such as `com.miui.networkstack` / game acceleration `com.miui.gamebooster`), and the security center only performs entrance jumps and statistical display. `Unknown`: The jump target is not resolved within this APK.

### 1.2 "Network Assistant" main interface structure (observation)

- Main Activity: `com.miui.networkassistant.ui.NetworkAssistantActivity`, **is not** `MainActivity`; `exported=true`, `launchMode=singleTop`, `theme=Theme.MainPage.ActionBar`, intent-filter is `miui.intent.action.NETWORKASSISTANT_ENTRANCE` (`AndroidManifest.xml:957-960`).
- The homepage consists of `ui/fragment/FrontPageFragment` + self-developed card container `ui/widget/CardsView` + `ui/view/ListItem` + `ui/view/ScanningBar` (`ScanningBar` loads `listitem_network_diagnostics_scanning` layout, `ScanningBar.java:87`) → `Inferred`: The homepage is "Card Flow + "Scan state" structure, the card content can be delivered from the cloud (`com.miui.common.card.models.*` exists in `FunctionCardModel/NewsCardModel/AdvCardModel`, and these models `import NetworkDiagnosticsTipActivity.TITLE_KEY_NAME` reuse the `"title"` constant).
- The secondary entrances are all exposed as **independent Activity + exclusive action** (`Manifest:963-1065`), which can be directly pulled up by system settings/negative screen/search, such as `NETWORKASSISTANT_FIREWALL`, `NETWORKASSISTANT_TRAFFIC_SORTED`, `NETWORKASSISTANT_TC_DIAGNOSTIC`, `NETWORKASSISTANT_SMS_REPORT`, `NETWORKASSISTANT_APP_DETAIL`.

### 1.3 Implementation logic of traffic statistics (observation)

The data source is **`android.app.usage.NetworkStatsManager`**, not `TrafficStats`, nor qtaguid direct reading:

1. `com.miui.net.MiuiNetworkSessionStats` is a thin package, and the actual implementation is `miui/securitycenter/net/MiuiNetworkSessionStats.java` that comes with **inside**APK:
   - `mStatsService = (NetworkStatsManager) Application.A().getSystemService(NetworkStatsManager.class)`（`:14`）
   - `getMobileSummaryForAllUid(imsi, begin, end)` → `querySummary(0, imsi, begin, end)`（`:85`；`0`=`ConnectivityManager.TYPE_MOBILE`）
   - `getWifiSummaryForAllUid(begin, end)` → `querySummary(1, null, …)`（`:127`）
   - `getMobileHistoryForUid / getWifiHistoryForUid` → `queryDetailsForUid(...)`（`:75`、`:117`）
   - `buildNetworkStatsMap`: Aggregated by `NetworkStats.Bucket`, when **`bucket.getState()==2`, `txForegroundBytes/rxForegroundBytes`** (`:27-32`) is additionally accumulated → the front/backend traffic split comes from this (`SET_FOREGROUND`).
   - `forceUpdate()` calls the hidden method (`:66`) through reflection `bh.f.d(mStatsService, "forceUpdate", null)`.
2. Aggregation and sorting: `traffic/statistic/StatisticAppTraffic` provides 4 time windows (yesterday/today/last month/this month, `initData():169-181`), `buildMobileDataUsage/buildWifiDataUsage` generates `SparseArray<AppDataUsage[3]>` ([0]=total, [1]=foreground, [2]=background, `addEntryToAppUsageItem():41-52`); `getAppTodayPerHourTraffic` Cut window by hour (`:236-238`).
3. Real-time total: `NaTrafficStats` (inherits `TrafficStats`) uses `TrafficStats.getTotalRxBytes()+getTotalTxBytes()` (`:57-59`) and JNI `nativeGetUidIfaceStat(uid, iface, type)` (`:69`, `libnap.so`) to get the send and receive bytes of "specified network card × specified uid"; the network card name comes from `d.d(context)` (`:32`).
4. Statistics granularity configuration: `TrafficManageService.initNetworkStatsConfig()` actively changes **`Settings.Global["netstats_uid_bucket_duration"]` / `["netstats_uid_tag_bucket_duration"]` to `3600000` (1 hour)** (`service/tm/TrafficManageService.java:1273-1276`) → This explains why the traffic statistics on MIUI can produce graphs by hour; this is the App Modify the behavior of **system global settings** (`Observed`).
5. Exception handling: After `MiuiNetworkSessionStats.openSession()` captures `IllegalStateException`, if it is `net.qtaguid_enabled==false` and it is >24h since the last prompt, the "Traffic Statistics Abnormal" notification (`com/miui/net/MiuiNetworkSessionStats.java:98-112`; `v1.a("net.qtaguid_enabled", false)`) will pop up.
6. There are other application dimension data sources: `traffic/statistic/MiServiceFrameworkHelper` Read `content://com.xiaomi.push.providers.TrafficProvider/traffic` (`:24-25`) → Traffic statistics of Xiaomi push service (for special display of "Xiaomi Service").

**permission** (observation): List line 145 declares `<uses-permission android:name="android.permission.READ_NETWORK_USAGE_HISTORY"/>` (signature|privileged level), `registerTrafficStatsReceiver()` uses `ACTION_NETWORK_STATS_UPDATED` + permission `Constants.System.PERMISSION_READ_NETWORK_USAGE_HISTORY` to receive system statistics updates (`TrafficManageService.java:1474-1476`) → these two points together explain `NetworkStatsManager.querySummary` Why can all UID data be returned?

### 1.4 Key projects: How to implement firewall/network control (observation)

**conclusion: It is not iptables, not `NetworkManagementService`, nor `libtcp.so`, but the hidden method `setMiuiFirewallRule` that serves `security` (`miui.security.SecurityManager`) through the MIUI system, implemented by framework→netd rules.**

Evidence chain:

1. `firewall/impl/MiuiNetdFirewall.java:226`：`this.mSecurityManager = (SecurityManager) this.mContext.getSystemService("security");`
2. Set the rules to use **to reflect**, and make dual signature compatible with old and new models (`:177-202`):
   - New: `SecurityManager.setMiuiFirewallRule(String pkg, int uid, int rule, int type)` (`:183`)
   - Old: `SecurityManager.setMiuiFirewallRule(String pkg, int rule, int type)` (`:192`), if failed, use `mIsOldDevice=true` to use the old signature
   - Before each call, `q.u(context, pkg, uid, rule, type)` (`:178`, statistics/buried points)
3. Rule type constants (`MiuiNetdFirewall.java:46-50`): `TYPE_MOBILE=0`, `TYPE_WIFI=1`, `TYPE_ROAMING=2`, `TYPE_ALL=3`, `TYPE_OTHER=4`. For the rule values, see `model/FirewallRule.java:7-11`: `Init=-1 / Allow=0 / Restrict=1 / Alert=2`.
4. The current network state is also pushed to the system: `SecurityManager.setCurrentNetworkState(int)` + `q.t(int)` (`MiuiNetdFirewall.java:214-218`); state machine `Inited/MobileConnected/WifiConnected/EthernetConnected/BluetoothConnected/OtherConnected/WifiApConnected/Diconnected` (`AnonymousClass5:110-145`, mapping `d.a` enumeration).
5. The **interception callback is the system broadcast**: `registerFirewallReceiver()` dynamically registers `IntentFilter("miui.intent.action.FIREWALL")` (`:308`), and calls back `mListener.onAppNetworkRestrict(pkg, networkState)` (`:105`) after receiving it → the framework sends this broadcast when rejecting the first package of an App, and the App accordingly pops up "XX has been banned" Networking" notification (`FirewallService.resolveAppBlocked()`, `service/FirewallService.java:729-763`).
6. **default policy**: mobile/WLAN default `Allow` (`getMobileRule:414-426`, `getWifiRule:497-509` cannot find the rule, namely `Allow`); **roaming default `Restrict`** (`getRoamingRule:429-437` Rule not found (return `Restrict`) → Roaming whitelist has "reverse list" semantics. The default values ​​for newly installed applications are from `firewall_mobile_preconfig` / `firewall_wifi_preconfig` (default `Allow`, `CommonPerConstants.java:40-42`).
7. Rule persistence: **is not SQLite, but an encrypted private file** (see §1.9): `<subscriberId>_fire_wall_config2.db`, `fire_wall_wifi_config.db`, `fire_wall_roaming_config.db` (`firewall/FirewallConstants.java:5-7`; see `FirewallService.java:635` for file name splicing).
8. Start and load: `FirewallService.createFireWall()` → `new MiuiNetdFirewall(ctx)` → `start()` → `loadWifiAndRoamingFirewall()` → `loadMobileFirewall()` → `setWhiteListAppMobileAllow()/setWhiteListAppWifiAllow()` (`FirewallService.java:569-579`). The whitelist is from `PreSetGroup.getPreFirewallWhiteList()` (`:819-837`) and forces the system to apply the WLAN rule as `Allow` (`setSystemAppWifiRuleAllow():807-816`).
9. **temporary rule** (Third-party App applies for temporary release): `setTempMobileRule/setTempWifiRule` (`MiuiNetdFirewall.java:624-669`) records `srcPkgName`. Only one applicant is allowed in the same package. If there is a conflict, it will be rejected and logged.
10. Behavioral aspect of "Unauthorized traffic interception": `resolveAppBlocked` is only prompted (`:748`) when **24h deduplicates** (`mToastedAppMap`, `:747`) and App **is in the foreground**; mobile network + roaming + When the roaming whitelist is turned on, it is changed to `RoamingStateAlertActivity` (`:751-752`); the "Allow" button on the notification is sent to `action_broadcast_allow_app_firewall` (`:666-670`), and `FirewallService.AnonymousClass8:159-178` directly rewrites the rule to `Allow` after receiving it.

> `Unknown`: The implementation of `miui.security.SecurityManager.setMiuiFirewallRule` within the framework (whether the `bandwidth`/`firewall` chain of `netd` is eventually written) is not included in this APK and needs to be verified on the framework/`services.jar` side. `Observed`: The APK side only sends calls and broadcasts, without rule text.

### 1.5 Traffic package calibration (emphasis: the real use of libtcp.so)

**link (observation)**

1. SDK entrance `com.miui.sdk.tc.TcManager`: `init(context, packageName, SEC_KEY)` (`:244-256`, `SEC_KEY="A2FscFVdX1+ULfEz/TTPQVNRXE+lzSe2"`, `:17`) → `System.loadLibrary("tcp")` (`:116`).
2. Carrier coding mapping: `CMCC→"100"`, `UNICOM→"200"`, `TELECOM→"300"` (`:88-93`), in conjunction with `TelephonyUtil.CMCC/UNICOM/TELECOM`.
3. Template capability: `getProvinces()/getCities(code)/getBrands()/getOperators()/getInstructions()` (`:154-226`) → provided by the "province × operator × brand → query command" library built into `libtcp.so`; each command is `TcDirection{ sendNumber, receiveNumber, direction, cmdType }` (`:187-205`).
4. `cmdType` bit mask: `TC_TYPE_TRAFFIC=1`, `TC_TYPE_BILL=2`, `TC_TYPE_CALLTIME=4`, `TC_TYPE_ALL=7` (`:19-22`); `isTrafficCmdType` is determined by `(i10 & 1) != 0` (`MiuiTrafficCorrection.java:150-152`).
5. Perform calibration (`traffic/correction/impl/MiuiTrafficCorrection.java`):
   - First `mSecurityManager.startInterceptSmsBySender(context, receiveNumber, list.size()*2)` (`:192`) → **requests the system to intercept the SMS** from the operator's short number, so that the user cannot receive this calibration SMS;
   - At the same time, `SMS_RECEIVED` is registered in an orderly broadcast (`:125-131`) with **`IntentFilter.setPriority(Integer.MAX_VALUE)` + permission `Constants.System.PERMISSION_BROADCAST_SMS`**. After receiving it, `abortBroadcast()` + `setResultCode(0)` swallows it completely (`traffic/correction/impl/InterceptionReceiverDelegate.java:31-42,63-71`);
   - Send text message (`sendSmsMessage`), wait for receipt, `TcManager.getResult(...)` analysis → `dataUsage.parse(map.get("KeyValues"))` (`TcManager.java:228-242`);
   - The result is written back to `TrafficUsedStatus` (`mTotalTrafficB/mUsedTrafficB/mRemainTrafficB`), and `handleJustRemainStatus` handles "returning only the remaining amount" to the operator (`MiuiTrafficCorrection.java:137-144`).
6. Three calibration engines: `ITrafficCorrection` (SMS, `MiuiTrafficCorrection`), `IWebCorrection` (Web page, `traffic/correction/webcorrection/MiCorrection`), `TrafficCorrectionManager` (Facade). The web page calibration check is Xiaomi mobile/operator page: `https://mihall.10046.xiaomimobile.com/mimobile/consumption` (`MiCorrection.java:60`, `p4.i("query_micard_info")`).
7. Timeout and status: `TIMEOUT_MILLION=120000` (`:58`); waiting upper limit 180s/300s (`:156`, `InterceptionReceiverDelegate.java:135`); `PowerManager.WakeLock(1)` guarantees no sleep during calibration (`:78-83`).
8. The package-related data falls into the `bill_detail / calltime_detail / datausage_status / sms_correction` and other tables of `traffic.db` (see §1.9), and `TrafficSimManager` makes the four-category determination of "normal flow/daily card flow/free time flow/unlimited" (`service/tm/TrafficSimManager.java:315-494`).

**Conclusion (Observation)**: `libtcp.so` = "Traffic Correction Plugin", responsible for the **operator's SMS template library + parsing**; it has nothing to do with firewalls, iptables, and traffic control.

### 1.6 Dual Card Strategy (Observation)

- The rules are divided according to **slot (0/1)**: `mMobileRuleArrMap: SparseArray<ArrayMap<...>>` and `mTempMobileRule` are both slot 2 (`MiuiNetdFirewall.java:62-66`), and slot 1 (`:229-232`) is built only when `DeviceUtil.IS_DUAL_CARD` is used.
- One set of encrypted configuration files for each card: file name = `TelephonyUtil.getSubscriberId(slot)` + `_fire_wall_config2.db` (`FirewallService.java:629-637`).
- **Only the rules of "current default data card" are actually delivered to the kernel**: `loadActiveFirewallConfig()` monitors changes in the default data card. When switching `makeActiveSlotFirewall(newSlot, oldSlot)`, rolls back the Restrict rule of **old card** to Allow (revert=true), and issues the new card Restrict (`FirewallService.java:606-619`, `648-651`, `MiuiNetdFirewall.loadMobileRules():521-547`).
- The flow side has the same dual cards: `TrafficManageService` inside `mTrafficManagers = new TrafficSimManager[2]`, `mAppMonitor = new AppMonitor[2]`, `TrafficCornBinder[2]` (`TrafficManageService.java:1079-1086`), separated by `SimUserInfo.getInstance(ctx, slot)` and `imsi`.
- Cardless pocket IMSI: `SimUserInfo.DEFAULT_NULL_IMSI_1/2` (`MiuiTrafficCorrection.java:97-107`).

### 1.7 Other network sub-capabilities

- **lock screen traffic**: `service/tm/LockScreenManager` (`TrafficManageService.initLockScreenMonitor():1248-1250`), the traffic during the lock screen period is calculated according to uid increment (`LockScreenManager.java:80,106` uses `NaTrafficStats.getMobileBytes`).
- **AppMonitor / SocketTagger**: `service/tm/AppMonitor`, `SocketTaggerManager` (`class SocketTaggerManager` `:20`) → `Inferred`: used to tag the socket to attribute traffic (tag dimension statistics).
- **front/background networking restrictions**: `BackgroundPolicyService`, Android M+ goes `MiuiNetworkPolicyManager.setAppRestrictBackground(uid, bool)` / `getAppRestrictBackground(uid)` / `setRestrictBackground(bool)` (`firewall/BackgroundPolicyService.java:100-132`); M goes before `content://com.miui.powerkeeper.configure/userTable` `call("userTableupdate", …)`, `bgControl ∈ {miuiAuto,noRestrict,restrictBg,noBg}` (`firewall/UserConfigure.java:7-31`, `:85-98`). `Note`: `TrafficManageService.initNetworkBackgroundRestrict()` forces **on startup** `setRestrictBackground(false)` (`:1261-1271`).
  - ⚠️ `miui.securitycenter.net.MiuiNetworkPolicyManager` **referenced internally by `com.miui.net.MiuiNetworkPolicyManager` does not exist** in this APK source tree (the whole tree grep only has 7 references and no definition) → `Inferred`: This class is provided by MIUI framework (boot classpath), APK only relies on compile time; `Unknown`: its internal implementation.

### 1.8 Data and persistence (observation)

**(a) ContentProvider**

- authority: `com.miui.networkassistant.provider` (`provider/ProviderConstant.java:9`), `exported=true`, `permission="miui.permission.EXTRA_NETWORK"`, process `:remote` (`Manifest:1075`).
- There are also `com.miui.networkassistant.fileprovider` (`androidx.core.content.FileProvider`, `exported=false`, `grantUriPermissions=true`, `Manifest:1076`).
- URI routing (`provider/NetworkAssistantProvider.java:159-185`): `traffic_distribution`, `firewall/*`, `mobile_firewall/*/*`, `temp_mobile_firewall/*/*`, `temp_wifi_firewall/*`, `wifi_firewall/*`, `wlan_restrict`, `mobile_restrict`, `firewall_background_restrict`, `datausage_status`, `datausage_status_detailed` , `datausage_noti_status` , `na_settings_info` , `na_traffic_stats` , `na_traffic_purchase` , `traffic_purchase_config` , `top_usage_app` , `bill_detail` , `calltime_detail` , `sms_correction` , `tethering_limit` , `traffic_limit` , `bill_traffic_inquire` .

**(b) SQLite**

- `traffic.db`, version 3 (`provider/DBHelper.java:9-14`); `onUpgrade` meets `DROP TABLE traffic_saving_white_list / traffic_saving_pending_compress_list` (`:22-27`).
- Tables and columns (`provider/ProviderConstant.java`, itemized):

| table | key column |
|---|---|
| `traffic_distribution` | `_id, imsi, from_pkgname, to_pkgname, mobile_rxbytes, mobile_txbytes, wifi_rxbytes, wifi_txbytes, storage_time`（`:197-212`，`CONTENT_URI=content://…/traffic_distribution`） |
| `firewall` | `package_name, mobile_rule, wifi_rule`（`:113-121`） |
| `mobile_firewall` | `package_name, mobile_rule, mobile_rule_slot, source_package_name`（`:123-132`） |
| `wifi_firewall` | `package_name, wifi_rule, source_package_name`（`:265-273`） |
| `temp_mobile_firewall` | `package_name, temp_mobile_rule, temp_mobile_rule_slot, source_package_name`（`:168-177`） |
| `temp_wifi_firewall` | `package_name, temp_wifi_rule, source_package_name`（`:179-187`） |
| `mobile_restrict` / `wlan_restrict` / `firewall_background_restrict` | `package_name`（`:134-140,275-281,105-111`） |
| `datausage_status` | `month_used, month_warning, today_used, total_limit, purchase_tips_enable`（`:70-80`） |
| `datausage_status_detailed` | Column 21: `sim_slot, active_slot_num, month_used, today_used, total_limit, package_type, bill_*, traffic_*(name/value/unit/time/icon), click_action` (`:82-103`) |
| `datausage_noti_status` | `icon, text1, text2, action1, action2`（`:58-68`） |
| `na_settings_info` | `auto_traffic_correction, correction_time, needed_traffic_purchase, operator_setted, oversea_version, show_status_bar_setted, tc_diagnostic, traffic_saving_enabled, traffic_saving_started`（`:142-156`） |
| `na_traffic_stats` | `total_rx_byte, total_tx_byte`（`:246-253`） |
| `na_traffic_purchase` | `traffic_purchase_enabled`（`:238-244`） |
| `traffic_purchase_config` | `slot_num, first_enter_config, traffic_alert`（`:228-236`） |
| `top_usage_app` | `_id, package_name, traffic_used`（`:255-263`） |
| `bill_detail` / `calltime_detail` | `slot_num, package_total, package_used, package_remained, package_setted`（`:34-56`） |
| `sms_correction` | `traffic_correction, bill_correction, calltime_correction`（`:158-166`） |
| `tethering_limit` | `tethering_limit_enabled`（`:189-195`） |
| `traffic_limit` | `limit_status ∈ {-1,0,1(daily limit),2(package limit),3(daily rental card limit),4(unlimited)}`(`:214-226`) |
| `bill_traffic_inquire` | `sim_slot, bill_type, bill_value, traffic_type, traffic_value, traffic_unit`（`:15-32`） |

**(c) Encrypted configuration file (not SQLite!)**

- Catalog: `filesDir/<name>`; head magic number `HEADER = {119,97,108,105,109,105,110,105}` = ASCII `"walimini"` (`config/ConfigFile.java:20`).
- Structure: `"walimini" + int16 number of entries + (int16 len + key + int16 len + value)*`, the whole key returned by `ConfigManager.getNetworkAssistantEncryptKey()` does **loop XOR** (`ConfigFile.java:21,59-62,160-171,252-306`).
  - `Observed`: The key comes from native `libsec-common-utils.so` (loaded by `com/miui/securitykey/ConfigManager.java:42`), so the plaintext key cannot be obtained statically.
- Files: `_fire_wall_config2.db` (mobile, one copy per card), `fire_wall_wifi_config.db`, `fire_wall_roaming_config.db` (`firewall/FirewallConstants.java:5-7`).

**(d) SharedPreferences key** (`config/CommonPerConstants.java:49-92`, SP name is determined by `SharedPreferenceHelper` constructor input parameters, `:235` `getSharedPreferences(str, 0)`)

`cmcc_web_correct_available`, `common_analytics_update_time`, `data_usage_daily_analytics_update_time`, `direct_correction_time_`, `firewall_mobile_preconfig`, `firewall_roaming_whitelist_preconfig`, `firewall_wifi_preconfig`, `first_enter_traffic_purchase_declare`, `float_notification_enabled`, `has_shown_enable_to_send_msg_to_correct_dialog` , `enable_to_send_msg_to_correct`, `is_mi_sim_enable`, `is_privacy_agreed`, `is_xman_uninstalled`, `lock_screen_traffic_guide_notify_count`, `lock_screen_traffic_monitor`, `lock_screen_traffic_opened`, `miui_vpn_infos`, `miui_vpn_state`, `miui_vpn_userid` , `mi_sim_action`, `mi_sim_cloud_data`, `is_mi_sim_tips`, `mobile_daily_turn_on_time`, `netowrk_diagnostics_float_notification_enabled` (original spelling like this), `network_exception_update_time`, `no_more_ask_roaming`, `purchase_sms_number`, `purchase_sms_number_update_time`, `roaming_app_white_list_init`, `roaming_white_list_notify_enable`, `sms_number_receiver_update_time`, `status_bar_show_traffic`, `tethering_data_usage_over_limit`, `tethering_limit_enabled`, `tethering_limit_traffic`, `tethering_over_limit_opt_type`, `upload_month_report_update_time`, `wifi_daily_turn_on_time`.

> The default value of `purchase_sms_number` is embedded with a string of **traffic recharging SMS short number whitelist** (14, `106555062`, `10086`, `106581784`...`10690094613533`, `CommonPerConstants.java:26`) - used to identify "recharge/card purchase type" text messages, which is an observation.

**(e) Other**: `Settings.Global` writes to `netstats_uid_bucket_duration` (§1.3); `Settings.Secure["mobile_policy"]` reads (`TrafficManageService.java:1253`).

### 1.9 Cloud interface (all from code, call points marked one by one)

| URL | method/channel | Purpose and fields (from code) | Evidence |
|---|---|---|---|
| `https://api.miui.security.xiaomi.com/netassist/flow/queryflow` | POST | Check the balance/used of the package. Request body: `imei` (=`DeviceUtil.getAndroidId(SALT)`), `imsi`, `zipCode`, `phonenum`, `spType`, `monthused`, `iccid` | `WebApiAccessHelper.java:67-81`; constant `webapi/Constants.java:73` |
| `https://api.miui.security.xiaomi.com/netassist/floworderunity/supportfloworder` | POST | Determine whether "online traffic package purchase" is supported. Request body: `imei`, `zipCode`, `phonenum`, `spType` | `WebApiAccessHelper.java:29-40`; constant `:74` |
| `https://api.miui.security.xiaomi.com/views/netassist/productlist.html` | WebView | Traffic package product list page | `traffic/purchase/PurchaseUtil.java:9` |
| `https://api.sec.miui.com/v1/traffic/report` | POST | **reports the calibration SMS text** (the operator's SMS messages are returned to the cloud for template mining). Request body: `imei,province,city,carrier,<TRACK_KEY_ID_TRAFFIC_PRODUCT_CLICK>,upward,directive,downward,<ah.f22943m>,verison="1.1",type,result` | `WebApiAccessHelper.java:83-102`; constant `:77`; service `service/TcSmsReportService` |
| `https://api.sec.miui.com/common/whiteList/trafficChargeNum` | POST | Pull the "white list of SMS short numbers for recharging". Request body: `imei, dataVersion=0, isDiff=false` | `WebApiAccessHelper.java:110-120`; constant `:78`; see `CommonPerConstants.java:26` for default value |
| `https://api.sec.miui.com/common/whiteList/listByModule` | POST(`"update"`) | Cloud control module whitelist: `module=miui_vpn_infos` (`updateMiuiVpnInfos`, `:104-108`); xman/zman also reuses the same URL (`xman/XmanHelper.java:51`, `zman/ZmanHelper.java:50`) | `:82` |
| `https://api-flow-intl.10046.xiaomimobile.com/*` (10 endpoints) | GET/POST | **International version** (`*_intl`) traffic store: `/system/isp_rule`, `/product/product_pay_info`, `/order/product_order_details` , `/privacy/query_agree_info`, `/product/product_list`, `/product/recommend_product`, `/order/product_order_list`, `/system/flow_system_status`, `/privacy/agree`, `/privacy/withdraw_agree` | `utils/NetRequestUtils.java:24-34` (`preview-` prefix is debugging domain, `:7-17`) |
| `https://mihall.10046.xiaomimobile.com/mimobile/consumption` | GET | Xiaomi Mobile (MiCard) consumption/remaining balance inquiry, `query_micard_info` | `traffic/correction/webcorrection/MiCorrection.java:60`；`ui/fragment/PackageSettingGuideFragment.java:367` |
| `https://api-new-misim.10046.xiaomimobile.com/external/isSatisfyYYLLUser` | GET | Determine whether the "meter SIM flow" condition is met, `query_micard_info` | `service/tm/TrafficSimManager.java:380` |
| `https://connect.rom.miui.com/generate_204` | GET | Network connectivity detection (captive portal style) | `u6/a.java:51` |

Signature mechanism (observation): Except for `queryflow`/`supportfloworder` direct connection, the rest adopt `com.miui.common.net.a.b(json, url, SALT, baseParams, i)` unified encapsulation, SALT is hard-coded UUID (`webapi/Constants.java:66-68,79-81`), and the public parameters are `device, versionType, region, miuiVersion, carrier, appVersion` (`WebApiAccessHelper.getBaseParams():42-51`).
> `Unknown`: The specific signature algorithm that SALT participates in (in `com.miui.common.net.a`, it may be MD5/HMAC splicing), and has not been decompiled.

### 1.10 What exactly is `AntiTrackCloudService` doing (correction, observation)

**It is not** in the `com.miui.networkassistant` package, but `com.miui.securitycenter.service.AntiTrackCloudService` (`securitycenter/service/AntiTrackCloudService.java`, `IntentService`, started by `com/miui/permcenter/i.java:256`).

- `b()` (`:70-112`): Read **from `MiuiSettingsCompat$SettingsCloudData.c(cr, "AntiTrackSettings")` and the cloud control issues**’s `{"system":[...],"global":[...],"secure":[...]}` and the corresponding `*_default` array, item by item `Settings.System/Global/Secure.putString(key, value)` → **Overwrite the system setting items with cloud data** (Typical "anti-tracking default value delivery").
- `c()` (`:114-123`): For two hard-coded 64-bit hex constants (`DDF25F63…F4D4`, `4B4081EE…F4D4`), first decrypt `m4.a.c()` + use the string `"Behavior_Record_"` as the key `m4.a.b()` and decrypt it again (`:117`) to get the real Settings key Name, write an empty string, and register `ContentObserver`: Once **is changed back,** (`:43-51`) will be cleared immediately.
- Semantics (`Inferred`): This is the **anti-tamper lock** of the "behavior recording/advertising logo switch" - the cloud control forces certain system settings to be blank and continues to monitor them to prevent third parties SDK from turning on the tracking switch. It has nothing to do with network assistants and everything to do with "anti-tracking privacy protection."
- `Observed`: The two hex constants cannot resolve the plaintext key in static analysis (relying on the native/encrypted implementation of `m4.a`), `Unknown`: The specific locked Settings key name.

### 1.11 Components and exposed surfaces (observation, `AndroidManifest.xml`)

| components | exported | protect | Notes |
|---|---|---|---|
| `ui.NetworkAssistantActivity` | true | — | `NETWORKASSISTANT_ENTRANCE` |
| `ui.activity.FirewallActivity` | true | — | `NETWORKASSISTANT_FIREWALL` ("Network Control" can be launched by any application) |
| `ui.base.UniversalFragmentActivity` / `UniversalPreferenceActivity` | true | — | Two "universal containers": the former eats `NETWORKASSISTANT_UNIVERSAL_FRAGMENT_ACTIVITY` / `_APP_DETAIL` / `_TC_DIAGNOSTIC` / `_SMS_REPORT` (`:988-1010`), the latter eats `_SETTINGS` / `_MONTH_PACKAGE_SETTING` / `_AUTO_TRAFFIC_CORRECTION_SETTING` / `_STATUS_BAR_SETTING` / `_OPERATOR_SETTING` / `_LIMIT_SETTING` / `_SET_PACKAGE_GUIDE` (`:1016-1047`); Which page to open for **is determined by the intent parameter.** → `Inferred`: Large attack surface (can be routed externally to any subpage) |
| `ui.activity.TrafficSortedActivity` | true | — | `NETWORKASSISTANT_TRAFFIC_SORTED` |
| `ui.GetChargeTrafficActivity` / `BhOrderListActivity` / `RevertResultActivity` / `BhNormalOrderDetailActivity` / `GetPayStatusActivity` | true | — | Payment/order callback related, `singleTop/singleTask` |
| `ui.activity.InternationalRoamingSettingActivity` / `TetheringStatsSettingActivity` | true | — | |
| `provider.NetworkAssistantProvider` | **true** | `miui.permission.EXTRA_NETWORK` | See §1.8 |
| `androidx FileProvider` (`…fileprovider`) | false | grantUri | |
| `service.FirewallService` / `service.tm.TrafficManageService` | false | — | Process `:remote` |
| `service.TcSmsReportService` | exported is not declared (default false) | — | Report calibration SMS |
| `vpn.miui.MiuiVpnManageService` | **true** | No permission declared | action `com.miui.networkassistant.vpn.MIUI_VPN_MANAGE_SERVICE` → **Any application can bind**, `Observed`; its AIDL `IMiuiVpnManageService` 30KB (more methods), `Unknown`: whether method-by-method authentication is implemented |
| Several `*AlertActivity` (`NetworkOverLimitActivity`, `RoamingStateAlertActivity`, `TrafficConfigAlertActivity`, `NetworkStatsExceptionAlertActivity`, `TetherStatsOverLimitActivity`) | false | — | `singleInstance/singleTop` floating pop-up window |

---

## 2. `com.miui.warningcenter` (Early Warning Center/Disaster Warning/Mijia Warning)

### 2.1 Function list (observation)

| # | Function | entrance |
|---|---|---|
| 1 | Warning center homepage (displays recent warnings by location/subscription area) | `WarningCenterMainActivity`（`miui.intent.action.WARNINGCENTER_MAIN`，`Manifest:1447-1450`） |
| 2 | Disaster warning main page + strong reminder/system reminder two-level switch and reception level | `disasterwarning.WarningCenterDisasterActivity`（`WARNINGCENTER_DISASTER`，`:1549-1552`） |
| 3 | Subscription regional management (province/city/district three levels + reception level per district) | `WarningCenterDisasterSubscribeAreaActivity`、`WarningCenterDisasterAreaAdapter/StatusAdapter/NormalAdapter` |
| 4 | Disaster warning list (filtered by level) | `WarningCenterDisasterListActivity` + `WarningCenterDisasterListAdapter` |
| 5 | Disaster details (icon + official preparedness guide) | `WarningCenterDisasterDetailActivity` |
| 6 | Full-screen strong warning page (red/orange level) | `WarningCenterDisasterAlertActivity`（`singleInstance`，`DisasterAlertTheme`） |
| 7 | Mijia warning: account binding, warning pop-up window, cancellation | `mijia/**`、`WarningCenterMijiaActivity`（`WARNINGCENTER_MIJIA`） |
| 8 | Mijia early warning full screen window | `mijia.WarningCenterAlertActivity` + `AlertWindowHelper` + `AlertWindowViewModel` |
| 9 | Police assistance (policeassist: positioning/privacy floating window, AES encrypted reporting) | `policeassist.PaService`（`:remote`）、`PaSettingActivity`（`WARNINGCENTER_POLICE_ASSIST`，`:520-523`） |
| 10 | Top capsule/status bar entry | `MiuiStatusBarCapsule` |

### 2.2 Disaster warning type coding (meaning of `11A01 / 11B01 / 11D01 / fire`) (observation)

`disasterwarning/model/EventType.java:10-34` gives **complete code table**, both of which are 12379 (National Early Warning Information Release Center) event type code:

| code | Chinese (string resource) | enumeration |
|---|---|---|
| `11B01` | typhoon | Typhoon |
| `11B03` | Heavy rain | Rainstorm |
| `11B04` | blizzard | Snowstorm |
| `11B05` | cold wave | ColdWave |
| `11B15` | Hail | Hailstone |
| `11B06` | Strong wind | Hurricane |
| `11B09` | high temperature | Hot |
| `11B17` | Heavy fog | Fog |
| `11B19` | Haze | Haze |
| `11B21` | icy roads | FrozenRoad |
| `11B29` | Heavy pollution | Pollution |
| `11E06` | waves | Wave |
| `11C02` | Earthquake (**earthquake on disaster warning side**, different from earthquake warning in §3) | Earthquake |
| `11E02` | storm surge | WindStorm |
| `11B20` | Thunderstorm and strong wind | Thunderstorm |
| `11B25` | forest fire insurance | ForestFire |
| `11B37` | Geological disasters and meteorological risks | Disaster |
| `11A01` | **flood** | Flood |
| `11A02` | waterlogging | WaterLogging |
| `11A51` | flash flood | FlashFlood |
| `11D01` | **landslide** | LandSlide |
| `11D02` | mudslide | Mudslide |
| `11G01` | Forest fire (domestic) | ForestFireWithinTerritory |
| `11G05` | Grassland fires (domestic) | GrasslandFireWithinTerritory |
| `null` | Others | Other |

- The first letter is the major category: `11A`=flood and drought, `11B`=meteorology, `11C`=earthquake, `11D`=geological disaster, `11E`=ocean, `11G`=forest and grassland fire (`Inferred`, consistent with the national standard "Early Warning Information Release Specification" coding).
- Level and bit mask (`model/Severity.java:8-11`): `blue=1, yellow=2, orange=4, red=8`; four-color copy `warningcenter_disaster_level_1..4`.
- `fire` of `assets/warningcenter` is not an event code, but the **icon name**: `warningcenter_fire_{blue|orange|red|yellow}.webp` (`apk_full/assets/warningcenter/`), which corresponds to `ForestFireWithinTerritory/GrasslandFireWithinTerritory` reusing the same set of fire icons (`EventType.java:32-33`). `iconRes` of both is `warningcenter_disaster_icon_huozai`).

Purpose of **`assets/warningcenter`**: 71 files, named `warningcenter_<eventCode>_<severity>.(png|webp)`, indirectly referenced by the JSON index -

- `assets/warning_center_disaster_icon` (5,363 B, JSON array): `[{"code":"11A01","data":{"blue":"warningcenter_11A01_blue.webp",...}}, …]`;
- Access logic: `WarningCenterDisasterDetailActivity` First press `eventType.code` to find the entry in JSON, then press `data.get(severity.name().toLowerCase())` to get the file name, `Drawable.createFromStream(assets.open("warningcenter/" + fileName))` (`:341-370`).
- The large picture at the top of the details page uses drawables other than `assets/warning_center_disaster_icon` (`R.drawable.warningcenter_disaster_icon_*`, `EventType.getIconRes()`).
- `Observed`: Not all "code × severity" have pictures - for example, `11B07/11B09/11B14/11B16/11B19/11B21/11B22` lacks `blue`, `11A01/11A02/11A51/11D01/11D02/fire` uses `.webp`, and the rest `.png` → lacks pictures `getOfficialGuidance` /icon loading returns `null` (`:361-373`'s `return null` branch).

### 2.3 What is `warning_center_disaster_guide_text` (observation, original text read)

**is an assets file, not a string resource.**: `apk_full/assets/warning_center_disaster_guide_text`, 14,041 bytes UTF-8 JSON:

```json
[ {"code":"11B01","data":{"blue":"1.政府及相关部门按照职责做好防台风准备工作；…",
                        "yellow":"…","orange":"…","red":"…"} }, … ]
```

- Reading implementation: `WarningCenterDisasterDetailActivity$Companion.getOfficialGuidance(assets, code, severity)` → `assets.open("warning_center_disaster_guide_text")` → `JsonParser.parseReader(...)` → Linear matching `code` → `data.<severity>` → Return text (`:97-129`).
- Nature of content: ** **, the official "Precautionary Guide" standard article of the China Meteorological Administration/Emergency Department, gives 3~5 action suggestions according to "event type × four-color level" (such as 11B01 Typhoon Red: Stop gatherings, schools and businesses, return to Hong Kong to take shelter, reinforce structures...).
- Call point: details page `bindData` (`$bindData$2$guideText$1.java:52`) and full-screen strong warning page (`WarningCenterDisasterAlertActivity.java:340,368,465,493` - four branches corresponding to "strong reminder/system reminder × whether to fold").
- Default: `""` (`:128`) is returned when code/severity is not found.

### 2.4 Subscription, matching and push (observation)

1. The **push channel is also MiPush topic** (shares the same set as earthquake warning `com.miui.push.a`):
   - topic prefix `PREX_TOPIC = "12379W_"` (`disasterwarning/service/WarningCenterDisasterService.java:44`)
   - topic actual name = `"12379W_" + MD5(districtId + UUID_DISASTER_CODE) + "_" + registLevel` (`uploadTopic():163-172`), `UUID_DISASTER_CODE="0279a99a-80f9-42ec-8945-bfe7d5070ed6"` (`DisasterConstants.java:29`)
   - Subscription/unsubscription: `a.b(ctx).d(topic,null)` / `.f(topic,null)`; `deleteAllTopic()` Scan all topics on the device and delete the ones with the prefix `12379W_` (`WarningCenterDisasterService.java:67-90`)
2. **reception level is bit mask**: `getRegistLevel()` = `Σ (LEVEL[i] & strongLevel) | (LEVEL[i] & systemLevel)` (`disasterwarning/Utils.java:99-123`); default `strongLevel=8` (red), `systemLevel=12` (red | orange) (`:125-139`); that is, the **strong reminder only accepts red by default, and the system reminder defaults to red + orange**.
3. Whether **pops up** is determined by a two-level mask: `if (Utils.getSystemPushToggle() && (systemPushLevel & level) == level)`...(`WarningCenterDisasterManager.java:348,361`).
4. **positioning → Subscribe to the current zone**: `WarningCenterDisasterService.getLocation()` uses `LocationRecordManager.startLocation(true, cb)`. After success, get the longitude and latitude to request `RequestAreaCodeTask.execute(lng, lat)`, get `districtId` and then `uploadTopic`, and `deleteTopic(previous)` (`:92-126`); and **12-hour throttling** (`UPLOAD_TOPIC_INTERVAL=43200000`, `:45,56`) + screen light trigger (`:48-65`).
5. **area code source**: `https://api.sec.miui.com/12379/warning/getRegionInfo` (`DisasterConstants.java:24`, SALT `7war5230-a1cf-3k79-ec73-75cb82145n5x`).
6. How does the early warning issued by **cloud come in**: `XiaomiPushReceiver` (§2.5) → `WarningCenterDisasterManager.parseQuake()` / `parseAccurateWarningData()`.
7. **Mijia early warning**: `WarningCenterProvider.call("setMijiaAccount", …)` is triggered by **Mijia App** (see §2.6); `WarningCenterManager.handlePush()` handles the `MijiaWarning` module of MiPush; cancels the report `POST https://warning-mijia.sec.miui.com/mijia/warning/cancel` (`mijia/api/MijiaRemoteApi.java:11-23`, SALT `6f875c57-e561-56b5-84a1-68b55ad68066`).

### 2.5 Push distribution center (observation, cross-module key)

`com/miui/push/XiaomiPushReceiver.java` is the main entrance of all APK MiPush transparent messages (`onReceivePassThroughMessage`, `:322-331`), distributed according to `content.SecurityCenterPassThroughMessage.moduleName`:

| moduleName | Process | Evidence |
|---|---|---|
| `wakePath` | Transfer `miui.intent.action.DELAY_UPDATE_WAKEPATH` to `com.lbe.security.miui` (`forceUpdate` optional) | `:82-99` |
| `monthReport` | Pop-up notification, PendingIntent opens `securitycenter://monthreport?content=<data>`, only `c.a()` and not the international version | `:101-110` |
| `illegalSales` | `b0.a(ctx).c(data)` (illegal sales module) | `:111-114` |
| `antivirus`（`StatConstants.Channel.ANTIVIRUS`） | `e.e(ctx).h(info)` | `:115-118` |
| `luckymoney` | `LuckyPushUtil.processCMD` | `:119-122` |
| `cloudControl` | Broadcast `com.android.settings.action.PULL_CLOUD_DATA` (with `FLAG_RECEIVER_INCLUDE_BACKGROUND`) | `:123-131` |
| `earthquakeWarning` | `quake` + `quakeSign` use `EARTHQUAKE_PUBKEY` **SHA1WithRSA to verify** `EarthquakeWarningManager.showWarningInfo` | `:132-145` |
| `MijiaWarning` | `WarningCenterManager.getInstance().handlePush` | `:146-149` |
| `12379Warning` | `warning` + `warningSign` After verification with **disaster-specific public key** `WarningCenterDisasterManager.parseQuake` | `:150-167` |
| `accurate-warning` | Same as above for signature verification → `parseAccurateWarningData` (accurate early warning) | `:168-185` |

MiPush initialization: `com/miui/push/a.java:60` `n.I(ctx, "2882303761517330652", "5691733067652")` (appId/appKey, `Observed`); topic interface `n.d0/n.i0/n.h0` (`:64-77`).
> `Inferred`: The final source of the **early warning is the Xiaomi server** (issued by MiPush). APK does not contain the protocol code for "direct connection with ICA/Chengdu High-tech Disaster Reduction Institute Seismic Network"; `ICL` Just query the Xiaomi server for the channel identifier when "historical warning of a certain channel" (see §3.3).

### 2.6 Data and persistence (observation)

| library/table | definition | Notes |
|---|---|---|
| `warning_center_disaster.db` v2 | `db/DisasterDBHelper.java:13-17` | three tables |
| `disaster` | `:51`: `_id, description, effective, eventType, eventTypeCN, expires, headline, identifier, msgType, note, referencesInfo, sender, severity, receivePosition, warningType, instruction, province, city, county, location` (21 columns) | Disaster warning received (CAP protocol field: `identifier/msgType/references/severity/effective/expires`) |
| `area` | `:42`：`_id, code(INTEGER), province, city, region` | Regional dictionary (delivered from the cloud) |
| `subscribe` | `:60`：`_id, code, province, city, region, subscribeLevel(INTEGER)` | Subscribed region + reception level bitmask |

- Providers(all `exported=false`, process `:remote`): `com.miui.warningcenter.DisasterContentProvider` (`disaster`), `…DisasterAreaProvider` (`area`), `…DisasterSubscribeProvider` (`subscribe`) - `DisasterContentProvider.java:23`, `DisasterAreaProvider.java:23`, `DisasterSubscribeProvider.java:23` only register a single path.
- Exception for **`com.miui.warningcenter.provider` (WarningCenterProvider): `exported=true` and no permission** (`Manifest:1463`), only `call("setMijiaAccount")` (`provider/WarningCenterProvider.java:45-83`) is implemented, and all other `query/insert/update/delete` return `null/0`.
- SP key (`DisasterConstants.java:14-22`): `key_disaster_first_update`, `key_disaster_strong_level`, `key_disaster_strong_toggle`, `key_disaster_system_level`, `key_disaster_system_toggle`, `key_disaster_update_area_time`, `key_disaster_warning_set_previous_area`, `key_disaster_warning_set_previous_topic`, `key_disaster_warning_set_topic_time`.

### 2.7 Exposure surface and permission verification (observation, noteworthy)

`WarningCenterProvider.matchSmartHome()` (`:32-41`) is a rare **bidirectional verification** in this APK:

1. `getCallingPackage()` must be equal to `com.xiaomi.smarthome`;
2. And the package’s signing certificate SHA-256 must be equal to the hard-coded `B0:31:FE:98:A4:DB:B0:D4:D8:26:61:78:7F:25:DE:64:31:82:B3:78:E9:EF:63:2D:8A:DE:A7:5A:AB:58:F2:D8` (`:29`, extracted with `eg.c.f(pm.getPackageInfo(pkg,64))`);
3. Any one that does not meet the requirements is directly `return null` (`:51-53`), and the international version is directly disabled (`:47`).

→ `Observed`: This is a cross-application authorization model of "certificate pinning + caller whitelist"; `Inferred`: can be used to determine whether "Mijia is installed with the original version", but **does not constitute the capability** that can be used by any application.

---

## 3. `com.miui.earthquakewarning` (earthquake warning)

### 3.1 Function list (observation)

| # | Function | Entrance/evidence |
|---|---|---|
| 1 | Earthquake warning homepage (switch, receiving intensity threshold, recent warning) | `ui/EarthquakeWarningMainActivity`（`miui.intent.action.EARTHQUAKE_WARNING_MAIN`） |
| 2 | Full screen strong warning (countdown + digital voice broadcast + SOS style) | `ui/EarthquakeWarningAlertActivity`（`AlarmAlertFullScreenTheme`，`singleInstance`） |
| 3 | Subscription area (by city code; up to multiple) | `ui/EarthquakeWarningSubscribeAreaActivity` |
| 4 | Alert history/not received list | `ui/EarthquakeWarningListActivity`、`EarthquakeWarningListFragment`、`EarthquakeWarningUnreceivedListFragment` |
| 5 | Warning instructions/Legal terms/Privacy | `EarthquakeWarningInfoActivity`、`EarthquakeWarningGuideActivity`、`EarthquakeWarningGuideLawFragment` |
| 6 | Early warning demonstration (drill without disturbing the real push) | `EarthquakeWarningDemoFragment`, `EarthquakeWarningGuideSampleActivity`; copywriting "Earthquake Early Warning (Exercise)/(Test)/(Trial Operation)" `strings.xml:1720-1723` |
| 7 | Emergency information card (blood type/allergy/medication/organ donation/medical history/ID card/birthday/emergency contact) | `EarthquakeWarningEmergencyActivity/…EditActivity` + `Constants.PREF_KEY_*`（`:51-60`） |
| 8 | Volunteer monitoring (join/withdraw, contribution list, sharing) | `ui/EarthquakeWarningMonitorActivity`、`MonitorOpenFragment`、`MonitorShareFragment` |
| 9 | Refuge point navigation (Amap/Baidu) | `EarthquakeWarningManager.java:62-65`: `androidamap://poi?…keywords=应急避难场所`, `baidumap://map/place/nearby?query=应急避难场所` |
| 10 | Lock screen/inactive screen, screen brightness and volume/brightness temporary takeover | `Constants.PREFERENCE_KEY_PREVIOUS_VOLUME/BRIGHTNESS/BRIGHTNESS_MODE` (`:43-48`, `Inferred`: adjust volume and brightness during pop-up window, end recovery) |

### 3.2 Full link (observation)

```
MiPush transparent transmission (moduleName="earthquakeWarning")
   └─ XiaomiPushReceiver:132-145
        ├─ SHA1WithRSA.virefy(quake.toString(), quakeSign, EARTHQUAKE_PUBKEY) ← Verify signature, discard if failed
        └─ EarthquakeWarningManager.showWarningInfo(ctx, quakeJson, countries)

showWarningInfo
   ├─ parseQuake(json, cityCode)        EarthquakeWarningManager.java:497-527
   │    Fields: index, magnitude, longitude, latitude, epicenter, depth,
   │          type, startTime, updateTime, xmUpdateTime, channel, eventId, signature[]
   ├─ getWhiteList(ctx, item)           :236-257
   │    type!=1 → enter the positioning directly; type==1 → require Mi account + whitelist verification
   │    POST https://api.sec.miui.com/universalWhitelist/checkUserId  (params: accountId, "miuisec")
   └─ getLocationStep(ctx, item)        :153-232
        ├─ LocationUtils.getAdminAreaLocation2(...) ← Reverse geography + positioning
        ├─ item.calIC(IntensityTransformer.DEFAULT) ← Calculate local expected intensity; expired PUSH_EXPIRED
        ├─ intensity >= Utils.getSelectIntensity() ?
        │     Yes → ManageDataTask(insert) + showAlarmNotification(strong reminder)
        │     No → the same eventId has been played → play again (remove eventId)
        │          → Otherwise, if "Low Earthquake Warning" is turned on → showLowEarthquakeWarning (normal notification)
        │          → Otherwise, bury the point PUSH_ERROR_INTENSITY_LOW / DO_NOT_REACH_THRESHOLD
        ├─ City matching: cityCode contains local area code → matchMyPositionInSupport(:263-281) → supports area list verification
        └─ matchSignature(...)  :287-495
             POST https://srv.sec.miui.com/earthquake/warning/signature  (channel=…)
             Return datas[channel].data[code].signs[] to get the multi-line text of "Hedging Guidelines"
             When searching, do 100^N truncation and step-by-step rollback of code (i16=(i16/100^i)*100^i,:378-381)
```

**Early warning source determination (key answers)**

- `Observed`: **in APK does not have any HTTP or socket protocol directly connected to the earthquake monitoring network/station network in**; the only entrance for early warning is **MiPush transparent transmission of message** (§2.5) → `Inferred`: The **warning is generated by the Xiaomi server and issues**. The terminal only performs signature verification, judgment, and display.
- `Observed`: The `quake.channel` field is given by the server (`EarthquakeWarningManager.java:510`). There is only one hard-coded channel identifier **in APK** `"ICL"`:
  - `EarthquakeWarningManager.java:718` `requestSignatureTask.execute("ICL")`
  - `ui/EarthquakeWarningUnreceivedListFragment.java:103` `RequestSignatureTask.download("ICL")`
  - `Inferred`: `ICL` = The channel code of Chengdu High-tech Disaster Reduction Institute (Institute of Care-life), which is one of the early warning data providers of **Xiaomi Earthquake Warning**. `Unknown`: `"ICA"` or other channel constants do not exist in APK. The complete value set of `channel` is determined by the server and cannot be enumerated from the client.
- `Observed`: The direction of the volunteer/monitoring network is **and the reverse** - the mobile phone acts as a sensor and reports the data **to** to Xiaomi (§3.4) instead of receiving it.

### 3.3 Subscription and push (observation)

- City subscription topic: `MD5(cityCode + UUID_CITY_CODE)`, `UUID_CITY_CODE="7bhr4579-a8we-3k79-ec73-45678324bh5c"` (`Constants.java:77`); `setTopicForPush()` (`EarthquakeWarningManager.java:762-764`)/`unsetTopicForPush()` (`:793-795`).
- Subscription effective point: `RequestAreaCodeTask` (`service/RequestAreaCodeTask.java:70`), `EarthquakeWarningMainFragment.java:101`, `EarthquakeWarningSubscribeAreaActivity.java:373`, `EarthquakeWarningService.onStartCommand` (`:307`).
- Regional analysis: `POST https://api.sec.miui.com/LBS/geoconv/admin` (`Constants.java:61`, `areaCodeResult.getData().getDistrictId()`).
- Supported city: `GET https://srv.sec.miui.com/earthquake/warning/supportCityCode` (`:67`); historical warning: `https://srv.sec.miui.com/earthquake/warning/records` (`:68`); signature/guideline: `https://srv.sec.miui.com/earthquake/warning/signature` (`:66`).
- Account whitelist: `https://api.sec.miui.com/universalWhitelist/checkUserId` (`:69`, UUID `f31feb6a-…`).
- Double write switch: `SharedPreferences` + `Settings.Secure["key_open_earthquake_warning"]` (`utils/Utils.java:122-125,253-254`) → System settings page is readable (`Inferred`: for Settings/notification bar linkage).

### 3.4 Volunteer sensor reporting (focus: `/earthquake/volunteer/sensor/upload`)

**data source (observation)**: not the standard `TYPE_ACCELEROMETER`, but **manufacturer’s private sensor type ID `33171100`**:

- `Constants.EQM_SENSOR_ID = 33171100`（`Constants.java:11`）
- Registration: `sensorManager.getDefaultSensor(33171100, true)` + `registerListener(this, sensor, 0)` (`service/EarthquakeWarningService.java:136-142`; samplingPeriod `0` = fastest)
- Purpose detection: `Utils.java:314` also uses the same ID to determine whether the device has this sensor.

**reporting fields (observations, all from code, not speculation)**: `service/EarthquakeMonitorDataManager.threadPoolRun()` (`:51-83`)

| Field | value |
|---|---|
| `oaid` | `w.a(context)` (device anonymous ID, `EarthquakeWarningService.java:120`) |
| `lng` / `lat` | The latest positioning (prioritize the converted value of GCJ, use `xi.a.b(...)` for `EarthquakeWarningService.java:174-179`) |
| `timestamp` | `System.currentTimeMillis()` |
| `model` | `getprop ro.product.device` |
| `rom` | `getprop ro.miui.ui.version.name` |
| `token` | `Utils.getEarthquakeVolunteerToken()` (issued after volunteer registration) |
| `data` | JSON serialized string **of** three-axis acceleration sampling array (`ewTranData.arrays` → `JSONArray.toString()`) |

- Transmission: `EarthquakeMonitorUtils.uploadData(url, map)` → `POST application/x-www-form-urlencoded; charset=UTF-8`, connection/read timeout 18s, and first passed `x.z()` (network available) judgment (`EarthquakeMonitorUtils.java:54-78`); request parameters will be signed by `j.b(map, UUID_MONITOR_DATA_UPLOAD)` (`:64`, UUID `5cggc678-cddf-4269-ab73-780174cvfba3`).
- Throttling/linkage: Every frame `onSensorChanged` will `EarthquakeMonitorUtils.exitSleepMode()` (`:285`) → broadcast `com.miui.powerkeeper_request_wake` (`reason=1`=DESK_CLOCK), **10 to `com.miui.powerkeeper` Only send** (`EarthquakeMonitorUtils.java:36-52`) → `Inferred` once per minute: Let the power saving strategy wake up the device when "suspected vibration" occurs to ensure continuous sampling.
- Positioning refresh: `LOCATION_TIME` SP key + **10 minutes** throttling, trigger `startLocationSingleUpdate` (gps/network dual provider, `2500ms`/`0m`, 10s timeout; `EarthquakeMonitorDataManager.java:129-208`).
- Volunteer registration/cancellation/number of volunteers: `…/volunteer/register`, `…/volunteer/cancelRegister`, `…/volunteer/countVolunteers` (`Constants.java:63-65`).
- Privacy Statement: `https://privacy.mi.com/earthquake_monitor/` (`:72`).

> `Unknown`: The actual physical meaning of sensor 33171100 in the HAL layer (whether it is "seismic mode" sampling of the accelerometer, or a dedicated low-power wake-up channel). `Observed`: There is no corresponding manufacturer HAL so in unpacking `lib/`, so it can only go to the Java side.
> `Unknown`: Is the `data` array the original three-axis value or the filtered/downsampled result (`EwTranData.arrays` is just `float[]`, this APK does not do preprocessing, directly transparently transmits `sensorEvent.values.clone()`, see `EarthquakeWarningService.java:278`).

### 3.5 Countdown / SOS tone (observation)

- The countdown seconds **is not given by the** server, but calculated by the client using the empirical travel time formula: `soundplay/CalcCountdown.java`
  - `SLOPE[]`/`INTERCEPT[]` each has 7 groups (`:6-7`), `ARRAY[]` is a lookup table of "epicentral distance → P wave/S wave travel time" (`:8`, hundreds of items, such as `0,0,10.637,0,0,22.264,15.738,6.619,…`)
  - `getCountDownSeconds(float,float)`（`:18`）、`getIntensity(float,float)`（`:43`）、`distance(...)`（`:10`，`EARTH_RADIUS=6371.0`）
  - → `Inferred`: According to (epicentral distance, focal depth), the S wave arrival time is inverted as a countdown, and the local intensity is calculated according to the attenuation relationship.
- Announcement tone: `soundplay/PlaySound`, `SoundPool(15, 3, 100)` (`:19`) loads 14 `res/raw` syllables:
  `one…ten` (one to ten), `non` ("zero"), `f20281di` ("dot"), `didi` (beep), `wu` ("five"/or "none").
  - `play(int countdownValue, float intensity)` (`:141-150`): Select the broadcast style according to the intensity - `intensity<=3 → 0`, `<=5 → 1`, `>5 → 2`; the difference in style is reflected in the insertion between numbers `non/f20281di/didi` (`addToPlayList():39-111`)
  - Playing rhythm: `size>11 ? mInterval(500ms) : 1280ms` (`:124-131`)
- Coupling with UI: `EarthquakeWarningAlertActivity` subscribes to `viewModel.getCountdown()`, and the value changes are `mPlaySound.play(value, intensity)` (`:352-355,372-375`), `onCountdown` (`:279`), and `stop/release` (`:301,626-629`).
- Time base: `soundplay/SntpClient` (5KB) → `Inferred`: Use NTP to correct the local clock to ensure that the countdown is consistent with the server time (`Unknown`: Whether it is actually called on the warning path, the call point needs to be confirmed; `PlaySound` does not directly reference it in this document).

### 3.6 `EARTHQUAKE_WARNING_MAIN` exports the exposed surface of Activity (observation)

- Component: `com.miui.earthquakewarning.ui.EarthquakeWarningMainActivity`
  - `exported="true"`、`screenOrientation="portrait"`、`theme=Theme.Settings.Miuix`、`label=@string/ew_app_name`（`AndroidManifest.xml:276`）
  - intent-filter：`miui.intent.action.EARTHQUAKE_WARNING_MAIN` + `category.DEFAULT`（`:277-280`）
- There are also a number of exported sibling components of the same type (observe `Manifest`):
  | components | exported | Notes |
  |---|---|---|
  | `EarthquakeWarningEmergencyEditActivity` | true | Emergency information editing (`:1418`) |
  | `EarthquakeWarningMonitorActivity` | true | Volunteer monitoring page (`:1426`) |
  | `EarthquakeWarningAlertActivity` | **false** | Full screen strong warning, can only be pulled up internally (`:1403`) |
  | `EarthquakeWarningService` | false | `:remote`（`:1407`） |
  | `EarthquakeContentProvider` / `…Provider2` | false | `:1424-1425` |
- **risk surface assessment (`Inferred`)**:
  1. `EARTHQUAKE_WARNING_MAIN` No permission, no caller verification → Any application can launch the main interface (low risk: UI only).
  2. The real high-risk action is in `EarthquakeWarningService.onStartCommand`: accept action `action_subscribe_area` / `action_unsubscribe_area` / `action_join_volunteer` / `action_exit_volunteer` / `action_restore_data` / `action_exit_earthquakewarning` (`:37-42`), and the Service of **is `exported=false`** → Only this App can be started, and the outside cannot directly forge subscription/unsubscription (`Observed`: The constraint is "Service cannot be exported", not action authentication).
  3. `miui.intent.action.EARTHQUAKE_WARNING_MAIN_ID` (`securitycenter/provider/SecSettingsSearchProvider.java:370`) is also registered in `SearchProvider` → the two actions point to the same Activity.
  4. `EarthquakeWarningAlertActivity` with `miui.continuity.policy=5` (`Manifest:1405`) → related to the "Xiaomi Miaoxiang/continuity" cross-device policy (`Inferred`: allowed to be transferred to tablets/cars).

### 3.7 Data and persistence (observation)

| library/table | definition | Column |
|---|---|---|
| `earthquake_warning2.db` v4 | `db/EarthquakeDBHelper.java:11-16` | Three tables |
| `earthquake` | `:63` | `_id, eventID, index_ew, magnitude, longitude, latitude, myLongitude, myLatitude, epicenter, startTime, signature, distance, intensity, depth, type, updateTime, warntime, cityCode, subscribe, isMyCity` |
| `subscribe` | `:84` | `_id, code, city, updateTime, support, counties, subscribeTime` |
| `area` | `:26` (+`:32-33,48`) | `_id, province, code, city, support, counties` (since v3, `province` was changed from `ALTER` to INTEGER, see `:44-56` for historical migration) |

- Providers：`content://com.miui.earthquakewarning.EarthquakeContentProvider2/{earthquake|area|subscribe}`（`EarthquakeWarningManager.java:61,68-70`），`exported=false`。
- File: Signature cache `/sdcard?/earthquakewarning/<SIGNATURE_FILE_NAME>` (`utils/FileUtils.java:10,33,37`, the actual root directory is determined by `y.c/y.h`, `Unknown`: final path).
- SP key: `Constants.java:29-60` (`key_open_earthquake_warning`, `key_open_low_earthquake_warning`, `key_select_push_intensity`, `key_earthquake_warning_monitor_token/order`, `key_earthquake_warning_previous_area_code`, `key_earthquake_warning_previous_volume/brightness/_mode`, emergency information 11 `preference_key_earthquake_warning_emergency_*`, etc.).

---

## 4. `com.miui.electricrisk` - The naming of **is seriously misleading: it is actually "AI anti-fraud protection" and has nothing to do with power/battery**

### 4.1 Correction conclusion (observation, this section is the most important)

The package name is `electricrisk` ("electrical risk"), but **has nothing to do with batteries, charging, bulging, leakage, or high temperature.**:

- Full search `BatteryManager|power_supply|sysfs|EXTRA_HEALTH|EXTRA_TEMPERATURE|BATTERY_PROPERTY|therm|charger` → **0 hits** (the only `http` hits are Privacy Policy URL, `ElectricProtectPhoneFragment.java:99`).
- Positive evidence (prove what it is):
  - `android:label` of `ElectricRiskMainActivity` = **`@string/system_anti_fraud_dialog_message`**(`Manifest:284`);
  - `AiGuardPosterActivity` registration `com.android.settings.action.AI_ENHANCED_SETTINGS`, meta-data `key = "ai_anti_fraud"` (`Manifest:291-299`);
  - There are only five types of cards: phone / text message / application / **webpage** / screen sharing (`ElectricRiskMainFragment.java:64-75` assembled with `protect_phone/protect_mms/protect_app/protect_web/protect_screen_share`);
  - Capability bits `CAPABILITY_PHONE=1`, `CAPABILITY_VIDEO=2` (`AiGuardUtils.java:15-17`).
- Conclusion: **'s "electricity risk/charging protection" is not in this module**. The "charging protection" in this APK is in `com.miui.powercenter.nightcharge.ChargeProtectFragment` (power saving and battery module, which belongs to another analysis scope); `assets/charge_current_config.json` also belongs to powercenter.

### 4.2 Function list (observation)

| # | Function | realize |
|---|---|---|
| 1 | Anti-fraud protection general page (5 cards, hidden by ability) | `ElectricRiskMainActivity`（`miui.intent.action.ELECTRIC_RISK`）+ `ElectricRiskMainFragment` |
| 2 | Telephone anti-fraud (AI identifies fraud during calls) | `ElectricProtectPhone*` + `AiGuardSceneService` of `PHONE_SCAM_SCENE=2` |
| 3 | SMS anti-fraud | `ElectricProtectMms*` |
| 4 | Application risk management | `ElectricProtectApp*` |
| 5 | Web risk warning | Card 4 → Issue `com.android.browser.OPEN_SETTINGS` (extra `current_page_type=31`, `ElectricProtectionFunctionPreference.java:139-150`) |
| 6 | Screen sharing protection | `ElectricProtectScreen*` |
| 7 | Video call anti-fraud | `AiGuardSceneService` of `VIDEO_SCAM_SCENE=1/2` |
| 8 | Cloud control availability + model blacklist | `AiGuardCloudController` |
| 9 | "AI Enhanced Settings" entry page (can be dynamically disabled) | `AiGuardPosterActivity` + `AiGuardUtils.setPosterActivityState():62-64` |

### 4.3 Data sources (observations, item by item)

| scene | data source | Evidence |
|---|---|---|
| call status | `TelephonyManager.listen(PhoneStateListener, LISTEN_CALL_STATE)` | `AiGuardSceneService.java:158`、`:117` |
| Determination of coming number | ① Local contact `ContactsContract.CommonDataKinds.Phone`: `replace(data1,' ','') LIKE ?`; ② Yellow Pages `YellowPageUtils.getPhoneInfo(...)` | `AiGuardSceneService.java:1335`、`:1360` |
| VoIP/Video Call (WeChat) | `AudioManager.addOnModeChangedListener` + `getActiveRecordingConfigurations()` takes `getClientPackageName()` | `AiGuardSceneService.java:1238`、`:1191-1197` |
| VoIP recording status broadcast | `miui.media.AUDIO_VOIP_RECORD_STATE_CHANGED_ACTION` (the sender needs `com.miui.permission.AUDIO_VOIP_RECORD_STATE_CHANGED_ACTION`), read extra `"pkg"` | `AiGuardSceneService.java:1116`、`:1081` |
| Foreground Activity (chain observation) | `monitorActivities(list1, list2)` → callback `onActivityChanged(caller, callee)` | `AiGuardSceneService.java:1276-1278`、`AiGuardSceneService$monitorActivities$1.java:98` |
| Monitor target packages | WeChat `AppConstants.Package.PACKAGE_NAME_MM`, QQ `PACKAGE_NAME_QQ`; video page `com.tencent.mm.plugin.voip.ui.VideoActivity`, `com.tencent.av.ui.AVActivity`; alarm page `com.miui.guardprovider/.aiguard.AlertActivity` | `electricrisk/f.java:14,18,22` |
| Capability gating | `SystemProperties "debug.config.media.video.voip.detect"`; `Build.DEVICE ∈ {dada, haotian}`; `com.miui.guardprovider`’s `meta-data["miui.aiGuardVersion"] >= 1` and `SDK_INT >= 35` | `AiGuardUtils.java:23-39` |
| Whether to allow networking | `x.z()` → `SystemProperties KEY_SECURITY_CENTER_ALLOW_CONNECT_NETWORK` (Determine which authorization page to go to) | `x4/l1.java:11-19` |

### 4.4 What does `AiGuardSceneService` do (implement logic, observation)

1. **starts**: `Application.java:773-781` reflection adjusts `AiGuardUtils.isAiGuardSupported/isAiGuardEnabled` + `setPosterActivityState(...)`, `startService` only meets the conditions. The service is in the `:remote` process, `exported=false` (`Manifest:283`), only `onCreate/onBind(null)/onDestroy`, **does not have `onStartCommand`** (`:1365-1383`).
2. **only performs "scenario triggering" and does not perform detection.**: Send explicit Intent to **`com.miui.guardprovider/.aiguard.AiGuardService`** after hit, extras is an integer:
   - `COMMAND` (1 = phone scene, 2 = video scene)
   - `PHONE_SCAM_SCENE`（1..4）、`VIDEO_SCAM_SCENE`（1..2）
   - `CAPABILITIES` (ability bit)
   - Evidence: `AiGuardSceneService.java:212`, `:367-370`, `:1260-1264`
3. **scene code meaning** (observation): Incoming call from "unknown number (not in the contact list and not in the yellow pages)" → `COMMAND=1, PHONE_SCAM_SCENE=2` (`:210-214`, see `:1267-1269` for the semantics of judgment function `j()`); WeChat → `PHONE_SCAM_SCENE=3 / VIDEO_SCAM_SCENE=1`; QQ → `PHONE_SCAM_SCENE=4 / VIDEO_SCAM_SCENE=2` (`:355-371`).
4. **ends with**: the call returns to `IDLE` → stop guardprovider service (`:215-217`, `:1281-1283`).
5. **pre-interception**: `l()` compares the process slot `z1.e()` with the creation time `z1.y()`, non-current users return directly (`:1272-1274`).
6. `Inferred`: The real speech/picture determination model is in **guardprovider** (another APK), this package does not hold any model or audio processing code (`Observed` support: this package does not have `MNN/AudioRecord/MediaCodec`).
7. ⚠️ **`Command` is an empty annotation.** (`Command.java:7-10`), `@interface Command{}`, and **do not participate in the issuance of the** command. The real "command" is the integer extra `"COMMAND"`. This is an easy pit to misjudge.

### 4.5 The true ownership of MNN / `assets/model` / `assets/rec_model_20230425.mnn` (observation)

| assets | consumer | relationship with electricrisk |
|---|---|---|
| `assets/rec_model_20230425.mnn`（810,908 B） | `com.miui.mlkit.mobilerec`: `ia/j.java:88-116` copied to `filesDir/apprecmodel` and `MNNNetNative.nativeCreateNetFromFile` | **No** |
| `libMNN/libMNN_Express/libMNNTrain/libmnncore.so` | `com.miui.mlkit.mobilerec.ranking.mnn.MNNNetNative` / `MNNTrainNative` | **No** |
| `assets/model` (680,154 B, non-MNN/GGUF header) | `lj/c.java:53` `getAssets().open(...)`, whitelist array `{"dict","model","pattern","phish"}` → Web page/phishing signature library | **No** |

- Input/output tensor name of MNN inference: input `sparse_inputs_1..6` + `sparse_inputs_time`, output `linear_9.tmp_1` (`ia/j.java:138-144`, `:262`); train `MNNTrainNative.nativeTrainRecModel(...)` on the end, and drop `apprecmodel/checkpoint_<n>` (`:451,:467`).
- `MNNNetInstance` does not exist in the whole position (real class name `MNNNetNative`) → If you see the statement "`AiGuardSceneService` uses MNN", it is a misjudgment of **to**.
- `Unknown`: `lj.d.h()` jadx decompilation failed, and the specific algorithm of `assets/model` is not visible.

### 4.6 Cloud interface and persistence (observation)

- **`AiGuardCloudController` does not have any URL** (in-module `http|OkHttp|Retrofit|HttpURLConnection|p4.c` 0 hits). It is **cloud control issued to read** and go to MIUI `MiuiSettingsCompat$SettingsCloudData`:
  - module `AiGuardAvailability`; data key `ai_guard_function_available` (bool), `deviceBlacklist` (JSON array, default **58 model codes**, `liuqin/yudi/malachite/…/matisse`, `AiGuardCloudController.java:19`)
  - Determination formula: `cloudSwitch && !deviceBlacklisted` (`:52`); not available → write SP → turn off switch → `stopService(AiGuardSceneService)` → `stopService(com.miui.guardprovider/.aiguard.AiGuardService)` (`:55-63`)
- Only URL: `https://privacy.mi.com/AIalert/{lang}_{COUNTRY}` (`ElectricProtectPhoneFragment.java:99`, `ACTION_VIEW`).
- Persistence key (observation):

| Type | key | Semantics | Evidence |
|---|---|---|---|
| SP (default) | `ai_guard_function_switch` | AI protection master switch (default false) | `AiGuardUtils.java:47`、`h.java:22` |
| SP file | `ai_guard_function_cloud_data` / `ai_guard_function_available` | Cloud control cache | `AiGuardCloudController.java:21,40,56` |
| `Settings.Secure` | `com.miui.guardprovider.AI_GUARD_PRIVACY_AGREEMENT` | guardprovider privacy agreement (only available if =1) | `ElectricProtectPhoneFragment.java:139,318` |
| `Settings.Secure` | `settings_sms_risk_warn_enable`(1) / `settings_sms_code_clipboard_warn_enable`(1) / `screen_share_protection`(1) / `com.android.browser.web_page_warning_states`(1) | Each card switch | `h.java:54,106,128,143` |
| `Settings.System` | `cloud_antispam`、`key_yellowpage_main_switch`、`pref_never_show_smart_antispam_user_notice` | Yellow Pages/Call Identification | `h.java:110,178-179` |
| Remote preference (`content://com.miui.securitycenter.remoteprovider`) | `key_risk_application_warning_enable`、`key_risk_sms_and_call_pay_warn_enable` | Risk warning | `ef/x.java:197-202`、`r4/a.java:20-40` |

- Close/undo process: 10-second countdown confirmation dialog box → write `ai_guard_function_switch=false` → stop two services → write `AI_GUARD_PRIVACY_AGREEMENT=0` (`ElectricProtectPhoneFragment.java:135-170`). No database, no local model.

### 4.7 Components and exposed surfaces (observation)

| components | exported | permission | filter |
|---|---|---|---|
| `AiGuardSceneService` | false | None | None (`:remote`) |
| `ElectricRiskMainActivity` | **true** | **No** | `miui.intent.action.ELECTRIC_RISK` |
| `ElectricProtectPhoneActivity` | **true** | **No** | None |
| `AiGuardPosterActivity` | **true** | **No** | `com.android.settings.action.AI_ENHANCED_SETTINGS` + 4 meta-data |
| `ElectricProtectMms/App/ScreenActivity` | false | None | None |

→ 3 exported and unprotected activities (`Manifest:284,290,291`).

---

## 5. `com.miui.simlock` (SIM card lock/anti-card replacement)

### 5.1 Function list (observation)

| class | Responsibilities |
|---|---|
| `SimLockManager` | Singleton + `ISimLockManager.Stub` implementation; PIN/PUK automatic verification, notification, binding determination (`SimLockManager.java:40,279-288`) |
| `ISimLockManager` | AIDL, descriptor `com.miui.simlock.ISimLockManager`, **without any business method** (only DESCRIPTOR branch, `ISimLockManager.java:9,53-59`) |
| `SimLockMonitorService` | The first monitoring service: Register Class 4 broadcast + `SubscriptionManager` monitoring, `onBind` returns to Manager Binder (`:136-161`) |
| `SimLockMonitorNormalService` | Same as above, `onStartCommand` returns `START_STICKY` (`:6-11`), process `:remote` |
| `SimLockNotificationService` | Notification landing: `notification_extra=0` → Settings page; `=1` → Resend PIN page (`:57-67`, `:22-40`) |
| `BootCompleteReceiver` | `LOCKED_BOOT_COMPLETED` + switch on → start monitoring service (`:13-23`) |
| `a` | AES-GCM encryption and decryption of PIN (`:154-176,206-226`) |
| `c` | Reflection Telephony Hide API / Status Determination / Intent Structure (`:62-319`) |
| `b` | Satellite communication status reception (`com.android.app.action.SATELLITE_STATE_CHANGE`, `:38-46,80-105`) |
| `SimLockStartActivity/Fragment` | 3 steps to activate the main process: lock screen password → Xiaomi account → set PIN (`:154-208`) |
| `SimLockChooseActivity/Fragment` | Dual SIM card selection (`:24-45`) |
| `SimLockPinActivity` | PIN/PUK input state machine (line 825, `:378-399,619-686`) |
| `SimLockQuery(All)Activity/Fragment` | Query/modify PIN (pass the lock screen password first, `:62-65,110-158`) |
| `SimLockSettingActivity/Fragment` | Setting page: master switch `master_switch` + query entrance (`:79-86`) |
| `SimLockDoneActivity` | Complete page; when **PIN is `1234`, "Suggested Modification" pops up** (`:135-140`, `:125-131`) |
| `SuccessDialog(Activity/NormalActivity)` | Startup success pop-up window (bootaware/normal dual version) |

### 5.2 Complete logic (observation)

1. **boots**: `BootCompleteReceiver` (directBootAware, `:bootaware`) receives `LOCKED_BOOT_COMPLETED`; only when `c.l()` (domestic + SDK≥30 + main user + tablet exception) and `Settings.Secure["sim_lock_enable"] != 0` `startService(SimLockMonitorService)` (`BootCompleteReceiver.java:15-21`, `c.java:187-196`).
2. **monitoring registration**: `SubscriptionManager.addOnSubscriptionsChangedListener` + broadcast `ACTION_SIM_STATE_CHANGED`, `ACTION_USER_PRESENT`, `android.miui.intent.action.SIM_LOCKED`, `DEVICE_POLICY_MANAGER_STATE_CHANGED` (`SimLockMonitorService.java:148-160`).
3. **status analysis**: `fg.e.a(intent)` maps `"ss"` (`ABSENT/READY/LOCKED/NETWORK/CARD_IO_ERROR/...`) and `"reason"` (`PIN/PUK/PERM_DISABLED`) into `r1.a` enumeration (`fg/e.java:30-83`).
4. **automatic unlocking (core)** `M8(e)`:
   - `PUK_REQUIRED` → `H8([slot])` plays PUK input;
   - `PIN_REQUIRED` and `A8(slot)==true` → read and saved PIN → `c.u(subId, pin)`;
     - `result[0]==0` (success): **is silent without pop-up window** when satellite communication is turned on; the lock screen status is `mPendingSuccess=true` and other `USER_PRESENT`; otherwise, the success page pops up after `mSuccessSlots.add(slot)` + 5s (`:229-277`, `:396-400`);
     - `result[0]==1` (failed): Write back PIN record `F8(pin, subId, true, 12)` and pop up the input page (`:260-266`).
5. **PIN/PUK state machine** (`SimLockPinActivity`, extra `pin_state` default 2, `slot_ids` is `ArrayList<Integer>`):
   - `0/1/2/3` → Set/Cancel ICC lock: `c.g(subId)` Read `isIccLockEnabled`, `enable = (state==0 || (state==2 && !locked))`, then `c.r(...)`=`setIccLockEnabled` (`:237-243,394-398`);
   - `4` → Verify PIN (`c.u`). After success, save `c.q(...,12)` and cut out a card (`:360-362,628-648,593-617`);
   - `6` → PUK dialog box submission (`:365-371`);
   - `>=8` → `c.v(subId, puk, newPin)`=`supplyPukReportResultForSubscriber`（`:650-686`）；
   - The remaining times are returned to the array `[1]` (`:310-317`).
6. **completes**: `T0(pin)` writes `Settings.Secure["sim_lock_enable"]=1` → `SimLockDoneActivity` (`:584-591`).
7. **notification details**: lock screen notification id=`OP_DELETE_SMS`(10011)(`:336-352`); PIN locked id=`10086`(`:363-367`); pull out the card id=`10000` (`:449-474`); channel `notify_channel_name_security` (`fg/h.java:30-33`).
8. **satellite communication linkage**: `is_enable=true` → `STATE_OPEN` and **temporarily write `sim_lock_enable=1`**; `false` → `STATE_CLOSING`, `STATE_CLOSED` writes back after 60s 0 (`b.java:86-104`, `SimLockManager.java:413-433`).
9. **precondition** `A8/z8`: The lock screen password `c.i()` has been set, Xiaomi account exists `c.m()`, `sim_lock_enable != 0`, and the ICCID record satisfies `f()==true && g()==false && e()!=null` (`SimLockManager.java:310-325,529-547`, `c.java:152-154,198-200`).
10. `SimLockMonitorService.onDestroy` is in the bootaware process and `c.f21538d==false` is `Process.killProcess(myPid())` (`:169-176`).

### 5.3 System services used / reflection (observation)

| Purpose | call | Evidence |
|---|---|---|
| Read ICC lock | Reflection `TelephonyManager.isIccLockEnabled()` (`createForSubscriptionId(subId)`) | `c.java:135-146` |
| Write ICC lock | Reflect `setIccLockEnabled(boolean, String)`; SDK>30 returns `PinResult`, use `getResult()` to compare `PIN_RESULT_TYPE_SUCCESS`, if failed, use `getAttemptsRemaining()` | `c.java:280-298,156-159` |
| Change PIN | Reflection `changeIccLockPassword(String,String)` | `c.java:62-80` |
| Check PIN | `ITelephony$Stub.asInterface(ServiceManager.checkService("phone")).supplyPinReportResultForSubscriber(int,String)` | `c.java:313-315` |
| Solution PUK | Same as above `.supplyPukReportResultForSubscriber(int,String,String)` | `c.java:317-319` |
| Card information | `miui.telephony.SubscriptionManager` + `SubscriptionInfo.getIccId/getSlotId/getSubscriptionId/getDisplayName` | `SimLockManager.java:115-117,221-227` |
| Lock screen status | `KeyguardManager.inKeyguardRestrictedInputMode()` / `isKeyguardSecure()` | `c.java:148-154` |
| Xiaomi account | `miui.accounts.ExtraAccountManager.getXiaomiAccount()`；`AccountManager.addAccount("com.xiaomi","passportapi",...)` | `c.java:198-200`、`SimLockStartFragment.java:155` |
| Lock screen password verification | Explicit `com.android.settings/.MiuiConfirmCommonPassword`, `.MiuiSecurityChooseUnlock` | `SimLockStartFragment.java:184,197` |
| Exclude virtual SIM | `MiuiSettings.VirtualSim.isVirtualSimEnabled/getVirtualSimSlotId` | `vb/a.java:31-33` |

### 5.4 Data and persistence (observation, focus)

| Type | Name | content | Evidence |
|---|---|---|---|
| Device encrypted files | `<deviceProtected filesDir>/encryped_pin.pwd` (original spelling) | `IV(12B) ‖ AES-GCM ciphertext`; plaintext = JSON `Map<iccId, {iccId, pin, phoneNumber, displayName, bool, bool}>` | `a.java:174-176,206-212,43-70` |
| Keystore alias | `key_pin_alias`（AES/GCM/NoPadding，`KeyGenParameterSpec` purposes=3） | The key to encrypt and decrypt PIN | `a.java:164,217-222` |
| `Settings.Secure` | `sim_lock_enable`（0/1） | main switch | `SimLockPinActivity.java:585`、`SimLockSettingFragment.java:47`、`SimLockManager.java:422,429` |
| r4 remote preference | `sim_lock_enable` (Boolean mirror) | "Whether it is temporarily enabled by the system" flag | `SimLockManager.java:421,425-428` |
| bury the point | `key_sim_lock_enable`、`key_sim_lock_bind_enable`、`key_sim_lock_bind_success`、`key_pin_lock_dialog_*`、`key_puk_lock_dialog_*`、`key_change_pin_dialog_*`、`key_success_dialog_*`、`key_cloud_dialog_*`、`key_keyguard_dialog_*`、`key_close_dialog_*` | process funnel | `fg/i.java:5-11`、`SimLockStartFragment.java:69-100`、`SimLockPinActivity.java:356,367,535,539,552,559,572` |

> **security points (observation)**: PIN is **reversibly encrypted disk**, the Keystore key is not set `setUserAuthenticationRequired` (`a.java:218`) → PIN can be restored as long as the process can read the file and access the Keystore alias; another `SimLockPinActivity.onSaveInstanceState` writes user input into the `Bundle` key `key_input` (`:818`), which may be persisted by the system snapshot (`Inferred`).

### 5.5 Cloud interface (observation)

**does not have**. `com/miui/simlock/**` Retrieve `http|Retrofit|OkHttp|URL(|p4.c|HttpURLConnection` → 0 hits. The only "networking semantics" are buried points (`fg/i`) and Xiaomi account login (AccountManager), both of which are carried by other SDK.

### 5.6 Components and exposed surfaces (observation)

| components | exported | permission | Notes |
|---|---|---|---|
| `SimLockChooseActivity` | **true** | **No** | `miui.intent.action.SIM_LOCK_CHOOSE`（`Manifest:391-396`） |
| `SimLockStartActivity` | **true** | `com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE` | `SIM_LOCK_START`（`:397-402`） |
| **`BootCompleteReceiver`** | **true** | **No** | `action.LOCKED_BOOT_COMPLETED`，directBootAware，`:bootaware`（`:415-419`） |
| `SimLockQuery/QueryAll/Pin/Done/SuccessDialog/Setting` | false | None | internal pages |
| `SimLockMonitorService` | false | None | directBootAware，`:bootaware`（`:420`） |
| `SimLockMonitorNormalService` / `SimLockNotificationService` | false | None | `:remote`（`:421-422`） |

→ `BootCompleteReceiver` is a `LOCKED_BOOT_COMPLETED` receiver with **exported and no permission**. A third party can forge the broadcast to launch the monitoring service (it is still gated internally by `sim_lock_enable` + `c.l()`, and the actual harm is limited to "repeated initialization") `Inferred`.
→ Caller added: `com/miui/permcenter/q.java:436-438` will `startService(SimLockMonitorNormalService)` and register the satellite status receiver (`:71,73` start/stop).

---

## 6. `com.miui.bubbles` (notification bubble)

### 6.1 Conclusion: **self-developed floating window, not Android Bubbles API** (observation)

- All inclusive `BubbleMetadata` / `setBubbleMetadata` → **0 hits**.
- Empirical evidence `BubbleController.addToWindowManagerMaybe()` (`BubbleController.java:141-168`):
  ```java
  WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2002, 16777256, -3);
  mWindowManager.addView(mStackView, mWmLayoutParams);
  ```
  `type=2002`（`TYPE_SYSTEM_ALERT`）、`flags=0x1000028`（`HARDWARE_ACCELERATED|NOT_TOUCH_MODAL|NOT_FOCUSABLE`）、`format=-3`（TRANSLUCENT）。
- That is, "full-screen transparent floating layer + `BubbleStackView` self-drawing", not a system notification bubble.

### 6.2 Form and implementation key points (observation)

| form | realize | Notes |
|---|---|---|
| Notification bubble stack | `BubbleStackView`(33KB)、`BubbleController`、`BubblePositioner` | Drag/Snap; `BubblePositioner` read `Settings.Secure["sidebar_bounds"]` (`:118`) and use `getCurrentWindowMetrics()` (`:374`) |
| Bubble content | `views/BubbleMessageView`(25KB)、`views/BubbleImageView` | Expand view |
| small window | `controller/FreeFormController` + `utils/MiuiFreeFormManagerWrapper` | Click the bubble → Open the App in the free window (`Inferred`) |
| Barrage | `controller/MiuiBarrageController` | The game scene turns the notification into a barrage; key `gb_bullet_chat`, `gb_boosting`, `enabled_notification_listeners` |
| notification listening | `services/BubblesNotificationListenerService` | `Manifest:449`：exported=true、`BIND_NOTIFICATION_LISTENER_SERVICE`、`enabled="false"`、`singleUser=true`、`:remote` |
| process bridge | `BubblesService`(`:ui`)、`BubbleRemoteService`(`:remote`)、`MiuiBubbleServiceManager`、`IUiProcessBinder`(6.8KB AIDL) | |
| settings | `BubblesSettingsActivity`(`com.miui.bubbles.BUBBLES_SETTINGS`)、`BubbleAppManageActivity` | Both exported=true, `:ui` |

### 6.3 Settings key (observation, original text)

| key | Scope | meaning |
|---|---|---|
| `miui_bubbles_notification_switch` | Secure(getIntForUser) | Master switch (default 1) |
| `miui_notification_bubbles_support` | Secure | Device support (default 1) |
| `miui_bubbles_pinned_apps` | Secure(String) | Apps that allow bubbles to appear |
| `have_notification_bubbles` | Secure | Has it ever appeared (guidance) |
| `all_app_workespace_status` | Secure | Show/hide bubbles (`BubblesManager.java:16`, original spelling) |
| `sidebar_bounds` | Secure(String) | sidebar area |
| `enabled_notification_listeners` | Secure | Authorized listener |
| `gb_boosting` / `gb_bullet_chat` / `gb_notification` | Secure | Game acceleration/barrage/in-game notifications |
| `miui_mirror_dnd_mode` | Secure(getIntForUser) | cast screen do not disturb |
| `settings_focus_mode_status` | Global | Focus mode (`BubblesService.java:209`) |
| `heads_up_notifications_enabled` | Global | Heads-up style (`BubbleUpManager.java:63`) |
| `miui_fsgesture_state` | Global | Full screen gestures (`:68`) |
| `show_bubble_tip_in_game_mode` | Secure | The in-game prompt only plays once (`TipsManager.java:114,126`) |

- Buried point keys (`BubbleMessageTrackUtil.java:33-39`): `hide_window_notification_click/expose/retract`, `notification_switch_status`, `enter`, `set`.
- ⚠️ Write `miui_bubble_app_settings` (`BubblesSettings.java:119`) and the read constant is `miui_bubbles_pinned_apps` (`:29,215`) → The key name is inconsistent, `Inferred`: historical write path.

---

## 7. `com.miui.monthreport` (monthly report)

### 7.1 Architecture (observation)

```
[Writer] --ContentResolver.insert--> content://com.miui.monthreport/report_json
   MonthReportProvider.insert()                    (MonthReportProvider.java:31-53)
     ├─ Required eventType; missing eventId → eventType+UUID+ts; missing moduleName → "monthReport"
     └─ a.f().i(values) → SQLite drop library
  ↓
 month_report.db / report_json                     (a.java:42,46,50)
  ↓  c.java TaskManager
 POST https://api.sec.miui.com/data/check  {module}     (c.java:157)
  ↓ code==1 (and not a mobile network or waiting to be transmitted <20000)
 POST https://data.sec.miui.com/data/upload            (c.java:199)
```

### 7.2 Table structure (observation)

`month_report.db` v2, single table `report_json` (`a.java:42-50`):
`_id INTEGER PK AUTOINCREMENT, eventId TEXT, eventTime INTEGER, pkgName TEXT, eventType INTEGER, version INTEGER, data TEXT, moduleName TEXT`
(`moduleName` was added from v1→v2 `ALTER TABLE ... DEFAULT monthReport`, `:50`.) Cleanup: delete `eventId` (`:138`), delete `eventId` in batches (`:153`), delete `eventTime <` after expiration (`:173`).

### 7.3 Who writes/what writes (observations)

The **network assistant is explicitly written to** (`com/miui/networkassistant/utils/DataUsageReportUtil.java`):

| eventType | version | content | Evidence |
|---|---|---|---|
| `105` | `4` | Yesterday’s traffic volume **Total volume** | `:9,12,31-33` |
| `106` | `1` | Yesterday’s traffic flow **details** | `:10,11,35-37` |

- `eventId = SHA-1(String(eventType) + String(eventTime))` (`:17`) → idempotent key; `pkgName = caller package name` (`:18`); `URL = "content://com.miui.monthreport/report_json"` (`:13`, `insert` see `:25`).
- Trigger: `Application.java:334` initializes `c.m(context)`; `ConnectivityChangeJobService2.java:419` `.p()` flushes; `monthReport` module of `XiaomiPushReceiver` is used for push display (`securitycenter://monthreport?content=`, §2.5).
- Declaration side: The manifest declares both `com.miui.monthreport.permission.DATA_MONTH_REPORT` (signatureOrSystem, `Manifest:41`) and `uses-permission` (`Manifest:136`) → the writer is a system-level App (antispam, etc.).

### 7.4 Reporting messages (observation, `b.java`)

| message key | meaning | Source |
|---|---|---|
| `reportId` | Report ID | `UUID (remove horizontal lines) + milliseconds` (`:188`) |
| `d` / `c` / `r` | Device code/operator name/region | `ro.product.device` / `ro.carrier.name` / `miui.os.Build.getRegion()`（`:114-116`） |
| `l` | language | `Locale.getDefault().toString()`（`:117`） |
| `v` | version string | `"MIUI-" + Build.VERSION.INCREMENTAL`（`:118`） |
| `t` | Timestamp | `x4.a0.h()`（`:119`） |
| `a` | Device fingerprint hash | `MD5(deviceId + "-5fdd8678-cddf-4269-bb73-48187445bba7")`（`ma/b.java:5-7`） |
| `m` | another hash | `MD5(x4.a0.e(context))`（`ma/b.java:9-11`） |
| `startTime/endTime/date` | Event time window and date | `:229-242` |
| `events` | Event array (≤500 items/batch) | `:160` |
| `o` | append string | `x4.a0.k(context)`（`:120`） |

### 7.5 Reporting control - **This build is hard-coded to short-circuit** (observation, key)

- Prerequisites: `x.z() && u.b(ctx) && ma.c.a()` (`c.java:149-151`), while **`ma.c.a()` constant `false`** (`ma/c.java:4-5`) ⇒ **This build will not actually upload**.
- Cloud control access (if reachable): `POST https://api.sec.miui.com/data/check`, body `{"module":<moduleName>}`, must be `code==1` and no `error_code` (`c.java:153-174`).
- Upload: `POST https://data.sec.miui.com/data/upload`, `data` = Base64 (message JSON), success criterion = response does not contain `error_code` (`c.java:134-147,194-201`).
- Add signature: `p4.c` → `p4.e.c(...)` Add `sign = g(list, appId)` to the form, appId = `5fdd8678-cddf-4269-bb73-48187445bba7` (`p4/e.java`).
- Rhythm/restrictions: Next module after 300s after success; press `count*300s` to try again after failure `count<3`; delete 30 days ago first each time; only report `eventTime < today 00:00`; non-WiFi and pending transmission > 20000 rejection; international version returns directly (`c.java:169-173,212-233,271-274`, `b.java:222-242`).
- `key_month_report_enabled` is write only but not read (`ma/c.java:10`, there is no read point in this dex) `Unknown`.

### 7.6 Components and exposed surfaces (observation)

| components | exported | permission |
|---|---|---|
| `com.miui.monthreport.MonthReportProvider` | **true** | `com.miui.monthreport.permission.DATA_MONTH_REPORT`（signatureOrSystem，`Manifest:41,905`） |

---

## 8. `com.miui.net` / `com.miui.vpnsdkmanager` / `com.miui.wakepath`

### 8.1 `com.miui.net` (2 thin packages; **both have actual call points**, do not believe the "no call" statement)

| class | Purpose | call point |
|---|---|---|
| `MiuiNetworkSessionStats` | Package APK comes with `miui.securitycenter.net.MiuiNetworkSessionStats` (see §1.3), and additional exception notifications for `isBandwidthModuleEnable()` (`SystemProperties "net.qtaguid_enabled"`) and `openSession()` are provided | `traffic/statistic/StatisticAppTraffic.java:186-189`（`initStatsSession`） |
| `MiuiNetworkPolicyManager` | Package **framework class** `miui.securitycenter.net.MiuiNetworkPolicyManager` (**within APK has no such class definition**, §1.7 Note): `get/setAppRestrictBackground(uid,bool)`, `get/setRestrictBackground(bool)` | `firewall/BackgroundPolicyService.java:9,22,102,112,117,131` |

> `Observed`: `miui.securitycenter.net.MiuiNetworkPolicyManager` There are only 7 places where **references**, and 0 places where **defines** → platform class; `MiuiNetworkSessionStats` then **is in dex It contains the definition** (`miui/securitycenter/net/MiuiNetworkSessionStats.java`, based on `NetworkStatsManager`).

### 8.2 `com.miui.vpnsdkmanager.IMiuiVpnSdkService` (1 AIDL)

- Descriptors `com.miui.vpnsdkmanager.IMiuiVpnSdkService` (`IMiuiVpnSdkService.java:185`) and `onTransact` cover **transaction 1..32** (`:743-911`), corresponding to 34 methods of the interface (some parameterless convenience methods share a code point group).
- Method grouping (observations):
  - **VPN / Accelerator business**: `refreshUserState()` (5), `H0/M4/B3/q2/w4` (6,4,7,3,23), `getSetting/setSetting` (8,9), `getSettingWithChannel` (31), `n8()` (15), `c2(boolean):byte[]` (27), `n4(int,String,int)` (16), `z7(int,int,int)` (17), `detectTimeDelay(String,int)` (29), `f7(String,int)` (10), `getCoupons()` (11), `b2(int,List<String>)` (2), `registerCallback(IMiuiVpnManageServiceCallback)` (1), `setDSDASwitch(boolean)` (30)
  - **AI voice changer**: `D7/O4/Q5/S/W1/h6/I/R5` (all with `IMiuiVoiceChangeCallback`, code bits 19,14,13,20,18,21,32,28), `y4(String,int):List<VoiceModel>`(22), `T1/W2`(24,26)
- `Inferred`: MIUI plugs "VPN Accelerator SDK" and "Game Voice Changer" into the same Binder interface for use by `com.miui.gamebooster` / Xunyou type SDK. `Unknown`: Server implementation and caller authentication (not in this APK; `Manifest` 0 hits the interface string → there is no server declared in this APK).

### 8.3 `com.miui.wakepath` (chain startup confirmation, not "wakeup statistics")

- Component: `ui.ConfirmStartActivity` (`Manifest:1087-1094`): **exported=true, no permission**, filter = **`android.app.action.CHECK_ALLOW_START_ACTIVITY`**; otherwise `excludeFromRecents`, `finishOnCloseSystemDialogs`, `relinquishTaskIdentity`, `documentLaunchMode=never`, `property miui.continuity.policy=5`.
- Semantics (observation): label `app_behavior_link_start`, that is, user confirmation when "Application A wants to start application B".
- Input parameters: `CallerPkgName`, `CalleePkgName`, `UserId`, `callerUserId`, `calleeUserId`, `restrictForChain`, `restrictForGetApps`, `Intent.EXTRA_INTENT` (`ConfirmStartActivity.java:240-261`).
- **anti-forgery check `l0()` (observation)**: only `SDK>33` is valid; first verify that `UserId` belongs to the real `UserProfile`; then reflect `getLaunchedFromUid()`, and the package uid, callerUserId, calleeUserId of caller/callee Compare them one by one, if any does not match, give up (`:132-150,265-272`).
  → ⚠️ When **SDK ≤ 33, `l0()` directly `return false` (deemed not suspicious)**, protection degradation.
- **silently releases**: callee is WeChat/Alipay, or action is `com.sina.weibo.sdk.action.ACTION_SDK_REQ_ACTIVITY`, or target is `<callee>.wxapi.WXEntryActivity`/`.wxapi.WXPayEntryActivity` (`:274-280`).
- **non-dialog prompt**: The remaining chain scenes use `ui.e`'s `WindowManager` **type=2038** (`TYPE_APPLICATION_OVERLAY`) floating prompt, copy `autostart_now_tip`, button `allow_onetime`, disappears in 5 seconds; click → `j0()`: `SecurityManager.addRestrictChainMaps(caller, callee)` (`:281-290,116-123`, `e.java:156-199`).
- User permission (`onClick(-1)` → `p0(true)`): `v.t(...)` real forwarding starts; if it is `PICK`/`SEND` and `o.f18010g` → `ContentResolver.call(StoragePolicyContract, GRANT_TEMP_PATH, {uid, packageName, RESTRICTED_PATH=ClipData The first item lastPathSegment})` grants a temporary path (`:190-230,152-161`); otherwise `r.b(ctx, pkgName, calleePkg)` (`:125-130`); and `call(..., method=14, {pkgName, calleePkg, type=1, mode, user, EXTRA_CALLER_UID})` of `PermissionContract` records an authorization (`:163-174`).
- Rejection (`onClick(-2)`): `n0(false)` + buried point + `setResult(0)` (`:337-341`).
- Persistence: The chained permission table is in the framework (`SecurityManager.addRestrictChainMaps`). This module **has no SP, no DB, and no cloud interface**.
- ⚠️ There is also `com.miui.permcenter.wakepath.WakePathManagerActivity` (`Manifest:614`, exported=false), which is the management page of another package path **of**. Do not confuse it with `com.miui.wakepath`.

---

## 9. `com.miui.analytics` / `com.xiaomi.analytics` / `com.xiaomi.onetrack` (buried point)

### 9.1 Structure (observation)

- `com.miui.analytics.AnalyticsUtil`: unified entrance on the business side (`recordCountEvent/recordNumericEvent/recordCalculateEvent/recordPageStart/End/trackXxxEvent`), all **are forwarded to `StatManager`**.
- `com.miui.analytics.StatManager`: Multi-instance manager **(`StatManager.java:58-100`) for**OneTrack SDK.
- `com.miui.analytics.RemoteAnalyticsReceiver`: Receive the broadcast **`com.miui.security.remote.analytics`** (`RemoteAnalyticsReceiver.java:13`, corresponding to `Manifest:1400`), and hand it to `RemoteAnalyticsManager` for delivery in the sub-thread.
- `ICore` / `ITrack`: Two AIDL proxy interfaces (`ICore` has 9 transactions such as `trackEvent(String)/trackEvents(String[])/setDefaultPolicy/isPolicyReady/deleteAllEvents`; `ITrack` has `trackEvent/trackPSEvent`) → `Inferred`: used for **cross-process/cross-process App (plug-in channels, such as sidebar, input method, game acceleration) reuses the same set of buried points**.
- `com.xiaomi.analytics`: Residual namespace from the older version of Analytics SDK (same set of `StatConstants` channel concepts).

### 9.2 Channel and appId (observation, `StatManager.java:17-32` + `StatConstants.java:6-19`)

| Channel constants | appId |
|---|---|
| `securitycenter` (default `APP_ID`) | `31000000037` |
| `APP_ID_V2` | `31000401489` |
| `network_assistant` (**network assistant**) | `31000000807` |
| `earthquake_warning` (**earthquake early warning**) | `31000401472` |
| `antivirus` | `31000000943` |
| `malicious_app_track` | `31000401557` |
| `xiaomi_safety_open_service` | `31000401800` |
| `conversation_tool_box` | `31000401977`（`Mode.PLUGIN`） |
| `gameturbo` / `game_turbo_new` | `31000000560` / `31000401977` |
| `gamebox` | `31000000057` |
| `dock` (sidebar, pluginId `securitycenter_sidebar`) | `31000000538` |
| `input_method` | `31000000494` |
| `app_man` (created only when `isPkgProcess`) | `31000000945` |
| advertising event | `31000000893`（`setAdEventAppId`） |
| Pad | `31000000506` |

- Initialization options (`:41`): `setChannel(package name)`, `setExceptionCatcherEnable(true)`, `setMode(APP/PLUGIN)`, **`setGAIDEnable(false)`**, `setAutoTrackActivityAction(false)` (antivirus/input method).
- Global switch: `OneTrack.setAccessNetworkEnable(ctx, bool)`, `OneTrack.setUseSystemNetTrafficOnly()` (`:61-62`); debugging mode is controlled by `uf.a.f35870a` (`:94-96`).

### 9.3 Report the endpoint (observation, **is hard-coded in byte[], avoiding plain text grep**)

`com/xiaomi/onetrack/util/y.java`：

| constant | byte array | Decoding result |
|---|---|---|
| `f23781q`（:71） | `{116,114,97,99,107,105,110,103,46,109,105,117,105,46,99,111,109}` | **`tracking.miui.com`**（CN） |
| `f23782r`（:74） | `…105,110,116,108…` | **`tracking.intl.miui.com`** (International) |
| `f23783s`（:77） | `…114,117,115…` | **`tracking.rus.miui.com`** (Russian area) |
| `f23784t`（:80） | `…105,110,100,105,97…` | **`tracking.india.miui.com`** (India) |

Path constants (`:13-16,67`): `/track/v4` (event reporting), `/track/key_get` (key retrieval), `/api/v4/detail/config`, `/api/v4/detail/config_common` (configuration delivery), `/api/v1/token`.
→ `Inferred`: The complete URL is in the form of `https://tracking.miui.com/track/v4`; `Observed`: The host name exists in the form of a byte array, and the **plaintext** search of `grep "https://tracking"` will be missed (this report has been confirmed by byte decoding).

### 9.4 Example of event name (observation)

`networkassistant` Side (`utils/AnalyticsHelper.java`): `net_control` (TRACK_ITEM_FIREWALL, `:27`), `change_mobile_firewall` (`:49`), `change_wlan_firewall` (`:50`), `firewall_item` (`:82`), `toggle_newapp_data_allow` (`:124`), `toggle_newapp_wlan_allow` (`:125`).
`earthquakewarning` Side (`analytics/AnalyticHelper.java`): `push_receive`, `push_show`, `push_error_illgal_type`, `push_error_intensity_low`, `push_error_location_failed`, `push_error_no_sign_area`, `push_error_parse_signature`, `push_error_time_long`, `toggle_earthquake_warning` (`:25-41`).
Named splicing: `AnalyticsUtil.createEventName(String,String)` / `RemoteAnalyticsManager.createEventName(String,String)` (`AnalyticsUtil.java:19`, `RemoteAnalyticsManager.java:54`) - `Unknown`: specific splicing format (method body is not expanded).

---

## 10. Unsolved mystery/needs dynamic verification

> Each item is marked with "What has been searched and why there is no conclusion" to avoid mistaking "not found" for "does not exist".

### 10.1 Network Assistant/Firewall

1. The last mile of **firewall**: `SecurityManager.setMiuiFirewallRule` → framework → netd implementation form (`netd`’s `bandwidth`/`firewall` chain, or system_server running iptables on behalf of it). APK side does not have any rule text (§1.4), requires `services.jar` / `framework.jar`.
2. **`miui.securitycenter.net.MiuiNetworkPolicyManager` implements**: 7 references and 0 definitions in the whole tree (§1.7/§8.1), presumably provided by the MIUI framework.
3. **`com.miui.common.net.a.b(...)` Signature algorithm**: SALT is hardcoded UUID, but the digest/splicing method is not expanded (§1.9).
4. Implementation method for **over-limit network disconnection/speed limitation**: Only `SimUserInfo.toggleDataUsageOverLimitStopNetwork` and `MobileDataPolicy` are observed, but no call point for directly changing the network switch → The full text of `TrafficSimManager`/`ITrafficManageBinder` needs to be completed (§1.5).
5. **`AntiTrackCloudService` Settings key plain text** solved by two hex constants: depends on `m4.a.b/c` (possibly native), statically irreducible (§1.10).

### 10.2 Early Warning Center/Earthquake Early Warning

6. **earthquake early warning `channel` full set of values ​​**: The client only hardcodes `"ICL"` (`EarthquakeWarningManager.java:718`, `EarthquakeWarningUnreceivedListFragment.java:103`), and the rest is determined by the server; whether `ICL` is equivalent to the "Chengdu High-tech Disaster Reduction Research Institute" `Inferred` (§3.2/3.3).
7. **private sensor `33171100`'s HAL semantics**: There is no corresponding manufacturer HAL in `lib/`, and it cannot be statically confirmed whether it is "accelerometer earthquake mode" or a dedicated wake-up channel (§3.4).
8. Whether **`SntpClient` participates in the countdown**: The file exists but is not seen `PlaySound` / `AlertActivityViewModel.startCountdown` calls it (§3.5).
9. **early warning sound resource**: `res/raw/{one..ten,non,f20281di,didi,wu}`’s actual hearing sense and “point/zero” mapping need to be confirmed by audition (`PlaySound.java:22-35` only gives the resource name).

### 10.3 Risks of electricity consumption (AI anti-fraud)

10. The upstream registration point **of**Activity stack observation: the caller of `monitorActivities` and the system service it depends on (whether it is `miui.process`/`AccessibilityManager`/hidden ActivityManager) are undecided (§4.3).
11. **`assets/model` (680 KB) Algorithm**: `lj.d.h()` jadx decompilation failed, it can only be confirmed that it is the loading path of "Web Page/Phishing Signature Library" (`lj/c.java:53`), and the algorithm is invisible (§4.5).
12. **guardprovider side real detection logic**: `com.miui.guardprovider/.aiguard.AiGuardService` is not in this APK, the semantics and decision model of `PHONE_SCAM_SCENE=1..4` / `VIDEO_SCAM_SCENE=1..2` are unknown (§4.4).

### 10.4 SIM card lock

13. Why is **`ISimLockManager` a Binder but has no business method**: `ISimLockManager.java:9,53-59` only has a branch of DESCRIPTOR, but `SimLockManager` is an implementation of `Stub` → It may be a "placeholder AIDL" or compatible with older versions (§5.1).
14. **`NETWORK_LOCKED` is treated as PUK and**: `SimLockManager.java:233` and `SimLockNotificationService.java:31` branches mix `PUK_REQUIRED` with network locks. Whether the PUK page will be accidentally popped up requires dynamic verification (§5.2).
15. **`SimLockMonitorService`'s `onBind` caller**: `onBind` returned Binder, but no external bind point was seen (`com.miui.permcenter.q` only `startService`).

### 10.5 Monthly Report/Others

16. The complete list of writers of **monthreport**: This report only confirms the 105/106 two types of events of `networkassistant` (§7.3); the `com.miui.antispam.permission.DATA_MONTH_REPORT` declared in the manifest implies that antispam is also written, but the antispam side call point is not located.
17. **monthreport upload trigger point**: `c.o()/c.p()` has no call point in this dex (`Application.java:334` only creates a single instance, `ConnectivityChangeJobService2.java:419` only adjusts the packaging of `.p()`) → The complete link needs to be strung together; and `ma.c.a()` is always false, making the upload unreachable (§7.5).
18. The framework sender **of**`CHECK_ALLOW_START_ACTIVITY`: `wakepath` only implements the receiving end. Whoever sends the broadcast before `startActivity` does not belong to this APK (§8.3).
19. The server of **`IMiuiVpnSdkService` and the host**: This APK only has the AIDL agent, 0 in `Manifest` hits the interface string, and the host App is unknown (§8.2).
20. **OneTrack event name splicing rule**: `AnalyticsUtil.createEventName(String,String)` (`:19`) and `RemoteAnalyticsManager.createEventName` (`:54`) method bodies are not expanded, and the event name format is still a black box (§9.4).
