> **历史分析存档，非最终结论。** 本文保留原始分析的技术内容，可能包含尚未验证或已被纠正的表述。隐私数据上传范围、同意条件、FBO 本地委托、报警短信加密、Provider 鉴权等，以 [最终报告](../REPORT.md) 与 [独立复核](../REVIEW.md) 为准。文中源码定位保留为原反编译相对路径；只有 evidence/ 中列出的精选文件随包提供，未打包完整源码树。

# 网络/流量/防火墙 · 预警中心 · 地震预警 · 用电风险 —— 模块逆向报告

> 目标：`MIUISecurityCenter-10.4.9-250402.0.1.apk`（`com.miui.securitycenter`，versionName 10.4.9-250402.0.1，versionCode 40001049）
> 方法：全量 jadx Java 源码静态阅读 + AndroidManifest + apktool 资源 + assets 二进制；**未做动态运行**，所有"运行时行为"均为代码路径推断。
> 标注：`观测`＝shipped artifact 直接可见；`推断`＝由代码路径合理外推；`未知`＝已按关键词检索未命中或需动态验证。
> 证据格式：`类名.java:行号`（行号对应 `_analysis/jadxout/sources/` 下文件）。

---

## 0. 模块地图（本报告范围）

| 包 | 文件数 | 职责（一句话） | 主要进程 |
|---|---|---|---|
| `com.miui.networkassistant` | 417 | 网络助手：流量统计/套餐/联网控制(防火墙)/双卡/漫游/热点/流量包购买/VPN 管理 | 主进程 + `:remote` |
| `com.miui.warningcenter` | 81 | 预警中心：12379 灾害预警订阅与推送、米家(Mijia)预警、警务协助(policeassist) | 主进程 + `:remote` |
| `com.miui.earthquakewarning` | 96 | 地震预警：MiPush 预警展示、订阅城市、志愿者传感器上报、SOS 倒计时音 | 主进程 + `:remote` |
| `com.miui.electricrisk` | 27 | ⚠️ 名为"电风险"，实为 **AI 反诈防护**（与电池/充电无关，见 §4） | `:remote` |
| `com.miui.simlock` | 25 | SIM 卡锁查询/设置/PIN 与开机监控（见 §5） | `:bootaware` / `:remote` |
| `com.miui.bubbles` | 49 | 通知气泡（见 §6） | `:ui` / `:remote` |
| `com.miui.monthreport` | 4 | 月度报告数据 Provider（见 §7） | `:remote` |
| `com.miui.net` | 2 | 对 `miui.securitycenter.net.*` 的薄封装（流量会话/后台限制策略） | 主进程 |
| `com.miui.vpnsdkmanager` | 1 | 一个 AIDL 接口（见 §8） | — |
| `com.miui.wakepath` | 2 | 链式启动"是否允许跳转"确认页（见 §8） | 主进程 |
| `com.miui.analytics` / `com.xiaomi.analytics` / `com.xiaomi.onetrack` | 10 / — / 179 | 埋点（见 §9） | 主进程 |

**关键前置事实（观测）**

- 整个 APK 的 Java 代码里**没有** `iptables` / `ip6tables` 字符串，**没有** `Runtime.exec` 执行命令（仅 xcrash、onetrack 抓 logcat、`getprop` 三处无关用途），也**没有** `NetworkManagementService` 的直接调用 → 见 `§1.4`。
  - 证据：全树 grep `iptables|ip6tables|Runtime\.getRuntime\(\)\.exec|ProcessBuilder` 仅命中 `xcrash/j.java:288`、`com/xiaomi/onetrack/util/b.java:296`、`miuix/animation/utils/CommonUtils.java:273`（`getprop`）。
- `libtcp.so` **不是**防火墙/traffic-control 库，而是「流量校准」运营商的短信模板解析引擎（见 `§1.5`）。加载点唯一：`com/miui/sdk/tc/TcManager.java:116` `System.loadLibrary("tcp")`。
- `libnap.so` 加载点唯一：`traffic/statistic/NaTrafficStats.java:19`，提供 `nativeGetUidIfaceStat(uid, iface, type)`（`NaTrafficStats.java:69`）。

---

## 1. `com.miui.networkassistant`（网络助手 / 联网控制）

### 1.1 功能清单（观测，以代码 + 中文文案为证）

| # | 用户可见功能 | 代码入口 | 文案证据 |
|---|---|---|---|
| 1 | 网络助手首页（流量总览卡片） | `ui/NetworkAssistantActivity`（`miui.intent.action.NETWORKASSISTANT_ENTRANCE`） | `app_name_na`=「网络助手」(`strings.xml:516`) |
| 2 | 流量排行（按 App/按时间/按前台后台） | `ui/activity/TrafficSortedActivity` + `ui/fragment/TrafficSortedFragment` | `usage_sorted_activity_lable`=「流量排行」(`:6512`) |
| 3 | 流量套餐设置（总量/已用/剩余/月结日） | `ui/fragment/PackageSettingFragment`、`PackageSettingGuideFragment`、`fragments/TemplateSettingFragment` | `NETWORKASSISTANT_MONTH_PACKAGE_SETTING` (`Manifest:1026`) |
| 4 | 运营商/省份/城市模板（校准短信模板） | `ui/fragment/OperatorSettingFragment` | `NETWORKASSISTANT_OPERATOR_SETTING` (`Manifest:1038`) |
| 5 | 流量校准（短信 / 网页 / 手动） | `traffic/correction/**`、`TcSmsReportService` | — |
| 6 | 流量提醒（阈值提醒、超限断网、锁屏流量） | `ui/fragment/TrafficReminderSettingFragment`、`TrafficLimitSettingFragment`、`LockScreenTrafficFragment` | `NETWORKASSISTANT_LIMIT_SETTING` (`Manifest:1042`) |
| 7 | 日租卡（日限额 / 日租卡专项） | `ui/fragment/DailyCardSettingFragment`、`config/DailyCardBrandConfig` | — |
| 8 | 联网控制（移动/WLAN/漫游 三张规则表） | `ui/activity/FirewallActivity`、`FirewallListFragment`、`MobileFirewallFragment`、`WlanFirewallFragment`、`RoamingWhiteListFragment`、`SystemAppFirewallFragment` | `firewall_activity_lable`=「联网控制」(`:1999`) |
| 9 | 新装应用默认联网策略 | `ui/fragment/NewInstalledPreSettingFragment`（`pref_key_new_installed_app_firewall_mobile1/2/_wifi`） | `:23-25` |
| 10 | 后台联网限制（BG 联网） | `firewall/BackgroundPolicyService` + `ui/fragment/BgNetworkAppListFragment` | — |
| 11 | 流量节省（traffic saving） | `ProviderConstant.NASettingsInfoColumns.TRAFFIC_SAVING_*`（`na_settings_info` 表） | — |
| 12 | 热点/共享流量限额与超限处置 | `service/tm/TetherStatsManager`、`ui/activity/TetheringStatsSettingActivity`、`TetherStatsOverLimitActivity` | `pref_key_tethering_setting`=「单次热点流量限额」(`:4742`) |
| 13 | 国际漫游（漫游开关/白名单/漫游提醒） | `ui/activity/InternationalRoamingSettingActivity` | `international_roaming_setting`=「漫游设置」(`:3003`) |
| 14 | 流量包购买（充流量/订单/退款/支付状态） | `traffic/purchase/PurchaseUtil`、`ui/order/**`、`ui/revert/RevertResultActivity`、`ui/GetChargeTrafficActivity` | `bh_charge_traffic`=「充流量」(`:958`) |
| 15 | 运营商流量/话费余额查询（SMS 通道） | `service/tm/TrafficSimManager.checkBillRemainder/checkBillArrears` | — |
| 16 | MIUI VPN 管理（含迅游 xunyou） | `vpn/miui/MiuiVpnManageService`（exported=true） | `MIUI_VPN_MANAGE_SERVICE` (`Manifest:1070-1072`) |
| 17 | 状态栏显示实时流量 | `CommonPerConstants.KEY.STATUS_BAR_SHOW_TRAFFIC` | `NETWORKASSISTANT_STATUS_BAR_SETTING` (`Manifest:1034`) |
| 18 | 流量统计异常提示（qtaguid 未启用） | `utils/NotificationUtil.sendNetworkStatsExceptionNotify` + `ui/activity/NetworkStatsExceptionAlertActivity` | `R.string.exception_titile` |
| 19 | 网络诊断（**仅提示页**） | `ui/activity/NetworkDiagnosticsTipActivity` | `network_diagnostics`=「网络诊断」(`:3469`) |
| 20 | 虚拟 SIM / 米 SIM（SeedSim、virtualsim provider） | `utils/VirtualSimUtil`、`model/VirtualSimInfo` | — |

**「网络加速」结论（观测+推断）**：本 APK 内**没有**网络加速/双 WiFi/链路聚合实现——全树无 `*cceler*` 业务类（仅动画插值类），无相关 Service/权限。`推断`：网络加速由其他系统组件（如 `com.miui.networkstack` / 游戏加速 `com.miui.gamebooster`）承担，安全中心只做入口跳转与统计展示。`未知`：跳转目标未在本 APK 内解析。

### 1.2 「网络助手」主界面结构（观测）

- 主 Activity：`com.miui.networkassistant.ui.NetworkAssistantActivity`，**不是** `MainActivity`；`exported=true`，`launchMode=singleTop`，`theme=Theme.MainPage.ActionBar`，intent-filter 为 `miui.intent.action.NETWORKASSISTANT_ENTRANCE`（`AndroidManifest.xml:957-960`）。
- 首页由 `ui/fragment/FrontPageFragment` + 自研卡片容器 `ui/widget/CardsView` + `ui/view/ListItem` + `ui/view/ScanningBar`（`ScanningBar` 加载 `listitem_network_diagnostics_scanning` 布局，`ScanningBar.java:87`）组成 → `推断`：首页是「卡片流 + 扫描态」的结构，卡片内容可由云端下发（`com.miui.common.card.models.*` 存在 `FunctionCardModel/NewsCardModel/AdvCardModel`，且这些模型 `import NetworkDiagnosticsTipActivity.TITLE_KEY_NAME` 复用 `"title"` 常量）。
- 二级入口均以**独立 Activity + 专属 action** 暴露（`Manifest:963-1065`），可被系统设置/负一屏/搜索直接拉起，例如 `NETWORKASSISTANT_FIREWALL`、`NETWORKASSISTANT_TRAFFIC_SORTED`、`NETWORKASSISTANT_TC_DIAGNOSTIC`、`NETWORKASSISTANT_SMS_REPORT`、`NETWORKASSISTANT_APP_DETAIL`。

### 1.3 流量统计的实现逻辑（观测）

数据源是 **`android.app.usage.NetworkStatsManager`**，不是 `TrafficStats`，也不是 qtaguid 直读：

1. `com.miui.net.MiuiNetworkSessionStats` 是薄封装，真正实现是 **APK 内自带**的 `miui/securitycenter/net/MiuiNetworkSessionStats.java`：
   - `mStatsService = (NetworkStatsManager) Application.A().getSystemService(NetworkStatsManager.class)`（`:14`）
   - `getMobileSummaryForAllUid(imsi, begin, end)` → `querySummary(0, imsi, begin, end)`（`:85`；`0`=`ConnectivityManager.TYPE_MOBILE`）
   - `getWifiSummaryForAllUid(begin, end)` → `querySummary(1, null, …)`（`:127`）
   - `getMobileHistoryForUid / getWifiHistoryForUid` → `queryDetailsForUid(...)`（`:75`、`:117`）
   - `buildNetworkStatsMap`：按 `NetworkStats.Bucket` 聚合，**`bucket.getState()==2` 时额外累加 `txForegroundBytes/rxForegroundBytes`**（`:27-32`）→ 前台/后台流量拆分即来源于此（`SET_FOREGROUND`）。
   - `forceUpdate()` 通过反射 `bh.f.d(mStatsService, "forceUpdate", null)` 调用隐藏方法（`:66`）。
2. 聚合与排序：`traffic/statistic/StatisticAppTraffic` 提供 4 个时间窗（昨天/今天/上月/本月，`initData():169-181`），`buildMobileDataUsage/buildWifiDataUsage` 生成 `SparseArray<AppDataUsage[3]>`（[0]=总、[1]=前台、[2]=后台，`addEntryToAppUsageItem():41-52`）；`getAppTodayPerHourTraffic` 按小时切窗（`:236-238`）。
3. 实时总量：`NaTrafficStats`（继承 `TrafficStats`）用 `TrafficStats.getTotalRxBytes()+getTotalTxBytes()`（`:57-59`）与 JNI `nativeGetUidIfaceStat(uid, iface, type)`（`:69`，`libnap.so`）取「指定网卡 × 指定 uid」的收发字节；网卡名来自 `d.d(context)`（`:32`）。
4. 统计粒度配置：`TrafficManageService.initNetworkStatsConfig()` 主动把 **`Settings.Global["netstats_uid_bucket_duration"]` / `["netstats_uid_tag_bucket_duration"]` 改成 `3600000`（1 小时）**（`service/tm/TrafficManageService.java:1273-1276`）→ 这解释了 MIUI 上流量统计为何能按小时出图；这是本 App 修改**系统全局设置**的行为（`观测`）。
5. 例外处理：`MiuiNetworkSessionStats.openSession()` 捕获 `IllegalStateException` 后，若 `net.qtaguid_enabled==false` 且距上次提示 >24h，则弹「流量统计异常」通知（`com/miui/net/MiuiNetworkSessionStats.java:98-112`；`v1.a("net.qtaguid_enabled", false)`）。
6. 应用维度数据源另有：`traffic/statistic/MiServiceFrameworkHelper` 读 `content://com.xiaomi.push.providers.TrafficProvider/traffic`（`:24-25`）→ 小米推送服务的流量统计（供「小米服务」专项展示）。

**权限**（观测）：清单第 145 行声明 `<uses-permission android:name="android.permission.READ_NETWORK_USAGE_HISTORY"/>`（signature|privileged 级），`registerTrafficStatsReceiver()` 用 `ACTION_NETWORK_STATS_UPDATED` + 权限 `Constants.System.PERMISSION_READ_NETWORK_USAGE_HISTORY` 接收系统统计更新（`TrafficManageService.java:1474-1476`）→ 这两点共同解释了 `NetworkStatsManager.querySummary` 为何能返回全 UID 数据。

### 1.4 重点专项：防火墙 / 联网控制到底怎么实现（观测）

**结论：不是 iptables，不是 `NetworkManagementService`，也不是 `libtcp.so`，而是通过 MIUI 系统服务 `security`（`miui.security.SecurityManager`）的隐藏方法 `setMiuiFirewallRule`，由 framework→netd 落地规则。**

证据链：

1. `firewall/impl/MiuiNetdFirewall.java:226`：`this.mSecurityManager = (SecurityManager) this.mContext.getSystemService("security");`
2. 设置规则走**反射**，且做了新旧机型双签名兼容（`:177-202`）：
   - 新：`SecurityManager.setMiuiFirewallRule(String pkg, int uid, int rule, int type)`（`:183`）
   - 旧：`SecurityManager.setMiuiFirewallRule(String pkg, int rule, int type)`（`:192`），失败后置 `mIsOldDevice=true` 走旧签名
   - 每次调用前先 `q.u(context, pkg, uid, rule, type)`（`:178`，统计/埋点）
3. 规则类型常量（`MiuiNetdFirewall.java:46-50`）：`TYPE_MOBILE=0`、`TYPE_WIFI=1`、`TYPE_ROAMING=2`、`TYPE_ALL=3`、`TYPE_OTHER=4`。规则值见 `model/FirewallRule.java:7-11`：`Init=-1 / Allow=0 / Restrict=1 / Alert=2`。
4. 当前网络态也推给系统：`SecurityManager.setCurrentNetworkState(int)` + `q.t(int)`（`MiuiNetdFirewall.java:214-218`）；状态机 `Inited/MobileConnected/WifiConnected/EthernetConnected/BluetoothConnected/OtherConnected/WifiApConnected/Diconnected`（`AnonymousClass5:110-145`，映射 `d.a` 枚举）。
5. **拦截回调是系统广播**：`registerFirewallReceiver()` 动态注册 `IntentFilter("miui.intent.action.FIREWALL")`（`:308`），收到后回调 `mListener.onAppNetworkRestrict(pkg, networkState)`（`:105`）→ framework 在拒绝某 App 首包时发出该广播，本 App 据此弹「已禁止 XX 联网」通知（`FirewallService.resolveAppBlocked()`，`service/FirewallService.java:729-763`）。
6. **默认策略**：移动/WLAN 默认 `Allow`（`getMobileRule:414-426`、`getWifiRule:497-509` 找不到规则即 `Allow`）；**漫游默认 `Restrict`**（`getRoamingRule:429-437` 找不到规则返回 `Restrict`）→ 漫游白名单是「反向清单」语义。新装应用默认值来自 `firewall_mobile_preconfig` / `firewall_wifi_preconfig`（默认 `Allow`，`CommonPerConstants.java:40-42`）。
7. 规则持久化：**不是 SQLite，是加密的私有文件**（见 §1.9）：`<subscriberId>_fire_wall_config2.db`、`fire_wall_wifi_config.db`、`fire_wall_roaming_config.db`（`firewall/FirewallConstants.java:5-7`；文件名拼接见 `FirewallService.java:635`）。
8. 启动与加载：`FirewallService.createFireWall()` → `new MiuiNetdFirewall(ctx)` → `start()` → `loadWifiAndRoamingFirewall()` → `loadMobileFirewall()` → `setWhiteListAppMobileAllow()/setWhiteListAppWifiAllow()`（`FirewallService.java:569-579`）。白名单来自 `PreSetGroup.getPreFirewallWhiteList()`（`:819-837`），并强制系统应用 WLAN 规则为 `Allow`（`setSystemAppWifiRuleAllow():807-816`）。
9. **临时规则**（第三方 App 申请临时放行）：`setTempMobileRule/setTempWifiRule`（`MiuiNetdFirewall.java:624-669`）记录 `srcPkgName`，同一包只允许一个申请者，冲突则拒绝并打日志。
10. 「未授权流量拦截」的行为面：`resolveAppBlocked` 只在 **24h 去重**（`mToastedAppMap`，`:747`）且 App **正在前台**时才提示（`:748`）；移动网络 + 漫游 + 漫游白名单开启时改为弹 `RoamingStateAlertActivity`（`:751-752`）；通知上的「允许」按钮发 `action_broadcast_allow_app_firewall`（`:666-670`），`FirewallService.AnonymousClass8:159-178` 收到后直接把规则改写为 `Allow`。

> `未知`：`miui.security.SecurityManager.setMiuiFirewallRule` 在 framework 内部的实现（是否最终写 `netd` 的 `bandwidth`/`firewall` 链）不在本 APK 内，需 framework/`services.jar` 侧验证。`观测`：APK 侧只发调用与广播，不含规则文本。

### 1.5 流量套餐校准（重点：libtcp.so 的真实用途）

**链路（观测）**

1. SDK 入口 `com.miui.sdk.tc.TcManager`：`init(context, packageName, SEC_KEY)`（`:244-256`，`SEC_KEY="A2FscFVdX1+ULfEz/TTPQVNRXE+lzSe2"`，`:17`）→ `System.loadLibrary("tcp")`（`:116`）。
2. 运营商编码映射：`CMCC→"100"`、`UNICOM→"200"`、`TELECOM→"300"`（`:88-93`），配合 `TelephonyUtil.CMCC/UNICOM/TELECOM`。
3. 模板能力：`getProvinces()/getCities(code)/getBrands()/getOperators()/getInstructions()`（`:154-226`）→ 由 `libtcp.so` 内置的「省份×运营商×品牌 → 查询指令」库提供；每条指令是 `TcDirection{ sendNumber, receiveNumber, direction, cmdType }`（`:187-205`）。
4. `cmdType` 位掩码：`TC_TYPE_TRAFFIC=1`、`TC_TYPE_BILL=2`、`TC_TYPE_CALLTIME=4`、`TC_TYPE_ALL=7`（`:19-22`）；`isTrafficCmdType` 用 `(i10 & 1) != 0` 判定（`MiuiTrafficCorrection.java:150-152`）。
5. 执行校准（`traffic/correction/impl/MiuiTrafficCorrection.java`）：
   - 先 `mSecurityManager.startInterceptSmsBySender(context, receiveNumber, list.size()*2)`（`:192`）→ **请求系统拦截来自运营商短号的短信**，使用户收不到这条校准短信；
   - 同时以 **`IntentFilter.setPriority(Integer.MAX_VALUE)` + 权限 `Constants.System.PERMISSION_BROADCAST_SMS`** 注册 `SMS_RECEIVED` 有序广播（`:125-131`），收到后 `abortBroadcast()` + `setResultCode(0)` 彻底吞掉（`traffic/correction/impl/InterceptionReceiverDelegate.java:31-42,63-71`）；
   - 发短信（`sendSmsMessage`）、等回执、`TcManager.getResult(...)` 解析 → `dataUsage.parse(map.get("KeyValues"))`（`TcManager.java:228-242`）；
   - 结果写回 `TrafficUsedStatus`（`mTotalTrafficB/mUsedTrafficB/mRemainTrafficB`），`handleJustRemainStatus` 处理「只回剩余量」的运营商（`MiuiTrafficCorrection.java:137-144`）。
6. 三种校准引擎：`ITrafficCorrection`（短信，`MiuiTrafficCorrection`）、`IWebCorrection`（网页，`traffic/correction/webcorrection/MiCorrection`）、`TrafficCorrectionManager`（门面）。网页校准查的是小米移动/运营商页面：`https://mihall.10046.xiaomimobile.com/mimobile/consumption`（`MiCorrection.java:60`，`p4.i("query_micard_info")`）。
7. 超时与状态：`TIMEOUT_MILLION=120000`（`:58`）；等待上限 180s/300s（`:156`、`InterceptionReceiverDelegate.java:135`）；`PowerManager.WakeLock(1)` 保证校准期间不休眠（`:78-83`）。
8. 套餐相关数据落 `traffic.db` 的 `bill_detail / calltime_detail / datausage_status / sms_correction` 等表（见 §1.9），并由 `TrafficSimManager` 做「正常流量 / 日租卡流量 / 闲时流量 / 不限量」四类判定（`service/tm/TrafficSimManager.java:315-494`）。

**结论（观测）**：`libtcp.so` = 「Traffic Correction Plugin」，负责**运营商短信模板库 + 解析**；与防火墙、iptables、traffic control 无关。

### 1.6 双卡策略（观测）

- 规则按 **slot（0/1）** 分开：`mMobileRuleArrMap: SparseArray<ArrayMap<...>>`、`mTempMobileRule` 同为 2 槽（`MiuiNetdFirewall.java:62-66`），仅当 `DeviceUtil.IS_DUAL_CARD` 才建槽 1（`:229-232`）。
- 每张卡一套加密配置文件：文件名 = `TelephonyUtil.getSubscriberId(slot)` + `_fire_wall_config2.db`（`FirewallService.java:629-637`）。
- **只有「当前默认数据卡」的规则真正下发给内核**：`loadActiveFirewallConfig()` 监听默认数据卡变化，切换时 `makeActiveSlotFirewall(newSlot, oldSlot)`，把**旧卡**的 Restrict 规则回滚为 Allow（revert=true），新卡下发 Restrict（`FirewallService.java:606-619`、`648-651`、`MiuiNetdFirewall.loadMobileRules():521-547`）。
- 流量侧同样双卡：`TrafficManageService` 里 `mTrafficManagers = new TrafficSimManager[2]`、`mAppMonitor = new AppMonitor[2]`、`TrafficCornBinder[2]`（`TrafficManageService.java:1079-1086`），按 `SimUserInfo.getInstance(ctx, slot)` 与 `imsi` 隔离。
- 无卡兜底 IMSI：`SimUserInfo.DEFAULT_NULL_IMSI_1/2`（`MiuiTrafficCorrection.java:97-107`）。

### 1.7 其他网络子能力

- **锁屏流量**：`service/tm/LockScreenManager`（`TrafficManageService.initLockScreenMonitor():1248-1250`），按 uid 增量算锁屏期间流量（`LockScreenManager.java:80,106` 用 `NaTrafficStats.getMobileBytes`）。
- **AppMonitor / SocketTagger**：`service/tm/AppMonitor`、`SocketTaggerManager`（`class SocketTaggerManager` `:20`）→ `推断`：用于把 socket 打 tag 以归因流量（tag 维度统计）。
- **前台/后台联网限制**：`BackgroundPolicyService`，Android M+ 走 `MiuiNetworkPolicyManager.setAppRestrictBackground(uid, bool)` / `getAppRestrictBackground(uid)` / `setRestrictBackground(bool)`（`firewall/BackgroundPolicyService.java:100-132`）；M 之前走 `content://com.miui.powerkeeper.configure/userTable` 的 `call("userTableupdate", …)`，`bgControl ∈ {miuiAuto,noRestrict,restrictBg,noBg}`（`firewall/UserConfigure.java:7-31`、`:85-98`）。`注意`：`TrafficManageService.initNetworkBackgroundRestrict()` 在启动时**强制** `setRestrictBackground(false)`（`:1261-1271`）。
  - ⚠️ `com.miui.net.MiuiNetworkPolicyManager` 内部引用的 `miui.securitycenter.net.MiuiNetworkPolicyManager` **在本 APK 源码树中不存在**（全树 grep 仅 7 处引用、无定义）→ `推断`：该类由 MIUI framework（boot classpath）提供，APK 只编译期依赖；`未知`：其内部实现。

### 1.8 数据与持久化（观测）

**(a) ContentProvider**

- authority：`com.miui.networkassistant.provider`（`provider/ProviderConstant.java:9`），`exported=true`，`permission="miui.permission.EXTRA_NETWORK"`，进程 `:remote`（`Manifest:1075`）。
- 另有 `com.miui.networkassistant.fileprovider`（`androidx.core.content.FileProvider`，`exported=false`、`grantUriPermissions=true`，`Manifest:1076`）。
- URI 路由（`provider/NetworkAssistantProvider.java:159-185`）：`traffic_distribution`、`firewall/*`、`mobile_firewall/*/*`、`temp_mobile_firewall/*/*`、`temp_wifi_firewall/*`、`wifi_firewall/*`、`wlan_restrict`、`mobile_restrict`、`firewall_background_restrict`、`datausage_status`、`datausage_status_detailed`、`datausage_noti_status`、`na_settings_info`、`na_traffic_stats`、`na_traffic_purchase`、`traffic_purchase_config`、`top_usage_app`、`bill_detail`、`calltime_detail`、`sms_correction`、`tethering_limit`、`traffic_limit`、`bill_traffic_inquire`。

**(b) SQLite**

- `traffic.db`，version 3（`provider/DBHelper.java:9-14`）；`onUpgrade` 会 `DROP TABLE traffic_saving_white_list / traffic_saving_pending_compress_list`（`:22-27`）。
- 表与列（`provider/ProviderConstant.java`，逐项列出）：

| 表 | 关键列 |
|---|---|
| `traffic_distribution` | `_id, imsi, from_pkgname, to_pkgname, mobile_rxbytes, mobile_txbytes, wifi_rxbytes, wifi_txbytes, storage_time`（`:197-212`，`CONTENT_URI=content://…/traffic_distribution`） |
| `firewall` | `package_name, mobile_rule, wifi_rule`（`:113-121`） |
| `mobile_firewall` | `package_name, mobile_rule, mobile_rule_slot, source_package_name`（`:123-132`） |
| `wifi_firewall` | `package_name, wifi_rule, source_package_name`（`:265-273`） |
| `temp_mobile_firewall` | `package_name, temp_mobile_rule, temp_mobile_rule_slot, source_package_name`（`:168-177`） |
| `temp_wifi_firewall` | `package_name, temp_wifi_rule, source_package_name`（`:179-187`） |
| `mobile_restrict` / `wlan_restrict` / `firewall_background_restrict` | `package_name`（`:134-140,275-281,105-111`） |
| `datausage_status` | `month_used, month_warning, today_used, total_limit, purchase_tips_enable`（`:70-80`） |
| `datausage_status_detailed` | 21 列：`sim_slot, active_slot_num, month_used, today_used, total_limit, package_type, bill_*, traffic_*(name/value/unit/time/icon), click_action`（`:82-103`） |
| `datausage_noti_status` | `icon, text1, text2, action1, action2`（`:58-68`） |
| `na_settings_info` | `auto_traffic_correction, correction_time, needed_traffic_purchase, operator_setted, oversea_version, show_status_bar_setted, tc_diagnostic, traffic_saving_enabled, traffic_saving_started`（`:142-156`） |
| `na_traffic_stats` | `total_rx_byte, total_tx_byte`（`:246-253`） |
| `na_traffic_purchase` | `traffic_purchase_enabled`（`:238-244`） |
| `traffic_purchase_config` | `slot_num, first_enter_config, traffic_alert`（`:228-236`） |
| `top_usage_app` | `_id, package_name, traffic_used`（`:255-263`） |
| `bill_detail` / `calltime_detail` | `slot_num, package_total, package_used, package_remained, package_setted`（`:34-56`） |
| `sms_correction` | `traffic_correction, bill_correction, calltime_correction`（`:158-166`） |
| `tethering_limit` | `tethering_limit_enabled`（`:189-195`） |
| `traffic_limit` | `limit_status ∈ {-1,0,1(日限),2(套餐限),3(日租卡限),4(不限量)}`（`:214-226`） |
| `bill_traffic_inquire` | `sim_slot, bill_type, bill_value, traffic_type, traffic_value, traffic_unit`（`:15-32`） |

**(c) 加密配置文件（不是 SQLite！）**

- 目录：`filesDir/<name>`；头部魔数 `HEADER = {119,97,108,105,109,105,110,105}` = ASCII `"walimini"`（`config/ConfigFile.java:20`）。
- 结构：`"walimini" + int16 条目数 + (int16 len + key + int16 len + value)*`，整体与 `ConfigManager.getNetworkAssistantEncryptKey()` 返回的密钥做**循环 XOR**（`ConfigFile.java:21,59-62,160-171,252-306`）。
  - `观测`：密钥来自 native `libsec-common-utils.so`（`com/miui/securitykey/ConfigManager.java:42` 加载），故静态拿不到明文 key。
- 文件：`_fire_wall_config2.db`（移动，每卡一份）、`fire_wall_wifi_config.db`、`fire_wall_roaming_config.db`（`firewall/FirewallConstants.java:5-7`）。

**(d) SharedPreferences 键**（`config/CommonPerConstants.java:49-92`，SP 名由 `SharedPreferenceHelper` 构造器入参决定，`:235` `getSharedPreferences(str, 0)`）

`cmcc_web_correct_available`、`common_analytics_update_time`、`data_usage_daily_analytics_update_time`、`direct_correction_time_`、`firewall_mobile_preconfig`、`firewall_roaming_whitelist_preconfig`、`firewall_wifi_preconfig`、`first_enter_traffic_purchase_declare`、`float_notification_enabled`、`has_shown_enable_to_send_msg_to_correct_dialog`、`enable_to_send_msg_to_correct`、`is_mi_sim_enable`、`is_privacy_agreed`、`is_xman_uninstalled`、`lock_screen_traffic_guide_notify_count`、`lock_screen_traffic_monitor`、`lock_screen_traffic_opened`、`miui_vpn_infos`、`miui_vpn_state`、`miui_vpn_userid`、`mi_sim_action`、`mi_sim_cloud_data`、`is_mi_sim_tips`、`mobile_daily_turn_on_time`、`netowrk_diagnostics_float_notification_enabled`（原文拼写如此）、`network_exception_update_time`、`no_more_ask_roaming`、`purchase_sms_number`、`purchase_sms_number_update_time`、`roaming_app_white_list_init`、`roaming_white_list_notify_enable`、`sms_number_receiver_update_time`、`status_bar_show_traffic`、`tethering_data_usage_over_limit`、`tethering_limit_enabled`、`tethering_limit_traffic`、`tethering_over_limit_opt_type`、`upload_month_report_update_time`、`wifi_daily_turn_on_time`。

> `purchase_sms_number` 的默认值内嵌了一串**流量代充短信短号白名单**（14 个，`106555062`、`10086`、`106581784`…`10690094613533`，`CommonPerConstants.java:26`）——用于识别「代充/购卡类」短信，属观测。

**(e) 其他**：`Settings.Global` 写入 `netstats_uid_bucket_duration`（§1.3）；`Settings.Secure["mobile_policy"]` 读取（`TrafficManageService.java:1253`）。

### 1.9 云端接口（全部来自代码，逐条标注调用点）

| URL | 方法/通道 | 用途与字段（来自代码） | 证据 |
|---|---|---|---|
| `https://api.miui.security.xiaomi.com/netassist/flow/queryflow` | POST | 查询套餐余量/已用。请求体：`imei`(=`DeviceUtil.getAndroidId(SALT)`)、`imsi`、`zipCode`、`phonenum`、`spType`、`monthused`、`iccid` | `WebApiAccessHelper.java:67-81`；常量 `webapi/Constants.java:73` |
| `https://api.miui.security.xiaomi.com/netassist/floworderunity/supportfloworder` | POST | 判断是否支持「线上流量包购买」。请求体：`imei`、`zipCode`、`phonenum`、`spType` | `WebApiAccessHelper.java:29-40`；常量 `:74` |
| `https://api.miui.security.xiaomi.com/views/netassist/productlist.html` | WebView | 流量包商品列表页 | `traffic/purchase/PurchaseUtil.java:9` |
| `https://api.sec.miui.com/v1/traffic/report` | POST | **上报校准短信文本**（运营商短信回流云端做模板挖掘）。请求体：`imei,province,city,carrier,<TRACK_KEY_ID_TRAFFIC_PRODUCT_CLICK>,upward,directive,downward,<ah.f22943m>,verison="1.1",type,result` | `WebApiAccessHelper.java:83-102`；常量 `:77`；服务 `service/TcSmsReportService` |
| `https://api.sec.miui.com/common/whiteList/trafficChargeNum` | POST | 拉取「代充短信短号白名单」。请求体：`imei, dataVersion=0, isDiff=false` | `WebApiAccessHelper.java:110-120`；常量 `:78`；默认值见 `CommonPerConstants.java:26` |
| `https://api.sec.miui.com/common/whiteList/listByModule` | POST(`"update"`) | 云控模块白名单：`module=miui_vpn_infos`（`updateMiuiVpnInfos`，`:104-108`）；xman/zman 也复用同一 URL（`xman/XmanHelper.java:51`、`zman/ZmanHelper.java:50`） | `:82` |
| `https://api-flow-intl.10046.xiaomimobile.com/*`（10 个端点） | GET/POST | **国际版**（`*_intl`）流量商店：`/system/isp_rule`、`/product/product_pay_info`、`/order/product_order_details`、`/privacy/query_agree_info`、`/product/product_list`、`/product/recommend_product`、`/order/product_order_list`、`/system/flow_system_status`、`/privacy/agree`、`/privacy/withdraw_agree` | `utils/NetRequestUtils.java:24-34`（`preview-` 前缀为调试域，`:7-17`） |
| `https://mihall.10046.xiaomimobile.com/mimobile/consumption` | GET | 小米移动(MiCard)消费/余量查询，`query_micard_info` | `traffic/correction/webcorrection/MiCorrection.java:60`；`ui/fragment/PackageSettingGuideFragment.java:367` |
| `https://api-new-misim.10046.xiaomimobile.com/external/isSatisfyYYLLUser` | GET | 判断是否满足「米 SIM 流量」条件，`query_micard_info` | `service/tm/TrafficSimManager.java:380` |
| `https://connect.rom.miui.com/generate_204` | GET | 联网连通性探测（captive portal 风格） | `u6/a.java:51` |

签名机制（观测）：除 `queryflow`/`supportfloworder` 直连外，其余走 `com.miui.common.net.a.b(json, url, SALT, baseParams, i)` 统一封装，SALT 为硬编码 UUID（`webapi/Constants.java:66-68,79-81`），公共参数为 `device, versionType, region, miuiVersion, carrier, appVersion`（`WebApiAccessHelper.getBaseParams():42-51`）。
> `未知`：SALT 参与的具体签名算法（在 `com.miui.common.net.a` 内，可能是 MD5/HMAC 拼接），未展开反编译。

### 1.10 `AntiTrackCloudService` 到底在干什么（纠偏，观测）

**它不在 `com.miui.networkassistant` 包内**，而是 `com.miui.securitycenter.service.AntiTrackCloudService`（`securitycenter/service/AntiTrackCloudService.java`，`IntentService`，由 `com/miui/permcenter/i.java:256` 启动）。

- `b()`（`:70-112`）：从 `MiuiSettingsCompat$SettingsCloudData.c(cr, "AntiTrackSettings")` 读**云控下发**的 `{"system":[...],"global":[...],"secure":[...]}` 与对应 `*_default` 数组，逐项 `Settings.System/Global/Secure.putString(key, value)` → **用云端数据覆盖系统设置项**（典型的"反追踪默认值下发"）。
- `c()`（`:114-123`）：对两个硬编码 64 位 hex 常量（`DDF25F63…F4D4`、`4B4081EE…F4D4`）先做 `m4.a.c()` 解密 + 用字符串 `"Behavior_Record_"` 作为密钥 `m4.a.b()` 再解一次（`:117`），得到真实 Settings key 名，写入空串，并注册 `ContentObserver`：**一旦被改回去就立刻再清空**（`:43-51`）。
- 语义（`推断`）：这是「行为记录 / 广告标识类开关」的**防篡改锁定**——云控把某些系统设置强制置空并持续看守，防止第三方 SDK 把追踪开关打开。与网络助手无关，与「反追踪隐私保护」相关。
- `观测`：两个 hex 常量在静态分析中无法解出明文 key（依赖 `m4.a` 的 native/加密实现），`未知`：具体被锁定的 Settings key 名称。

### 1.11 组件与暴露面（观测，`AndroidManifest.xml`）

| 组件 | exported | 保护 | 备注 |
|---|---|---|---|
| `ui.NetworkAssistantActivity` | true | — | `NETWORKASSISTANT_ENTRANCE` |
| `ui.activity.FirewallActivity` | true | — | `NETWORKASSISTANT_FIREWALL`（「联网控制」可被任意应用拉起） |
| `ui.base.UniversalFragmentActivity` / `UniversalPreferenceActivity` | true | — | 两个"万能容器"：前者吃 `NETWORKASSISTANT_UNIVERSAL_FRAGMENT_ACTIVITY` / `_APP_DETAIL` / `_TC_DIAGNOSTIC` / `_SMS_REPORT`（`:988-1010`），后者吃 `_SETTINGS` / `_MONTH_PACKAGE_SETTING` / `_AUTO_TRAFFIC_CORRECTION_SETTING` / `_STATUS_BAR_SETTING` / `_OPERATOR_SETTING` / `_LIMIT_SETTING` / `_SET_PACKAGE_GUIDE`（`:1016-1047`）；**要打开哪个页面由 intent 参数决定** → `推断`：攻击面较大（外部可路由到任意子页） |
| `ui.activity.TrafficSortedActivity` | true | — | `NETWORKASSISTANT_TRAFFIC_SORTED` |
| `ui.GetChargeTrafficActivity` / `BhOrderListActivity` / `RevertResultActivity` / `BhNormalOrderDetailActivity` / `GetPayStatusActivity` | true | — | 支付/订单回调相关，`singleTop/singleTask` |
| `ui.activity.InternationalRoamingSettingActivity` / `TetheringStatsSettingActivity` | true | — | |
| `provider.NetworkAssistantProvider` | **true** | `miui.permission.EXTRA_NETWORK` | 见 §1.8 |
| `androidx FileProvider` (`…fileprovider`) | false | grantUri | |
| `service.FirewallService` / `service.tm.TrafficManageService` | false | — | 进程 `:remote` |
| `service.TcSmsReportService` | 未声明 exported（默认 false） | — | 上报校准短信 |
| `vpn.miui.MiuiVpnManageService` | **true** | 未声明 permission | action `com.miui.networkassistant.vpn.MIUI_VPN_MANAGE_SERVICE` → **任意应用可 bind**，`观测`；其 AIDL `IMiuiVpnManageService` 30KB（方法较多），`未知`：逐方法鉴权是否在实现内 |
| 若干 `*AlertActivity`（`NetworkOverLimitActivity`、`RoamingStateAlertActivity`、`TrafficConfigAlertActivity`、`NetworkStatsExceptionAlertActivity`、`TetherStatsOverLimitActivity`） | false | — | `singleInstance/singleTop` 悬浮弹窗 |

---

## 2. `com.miui.warningcenter`（预警中心 / 灾害预警 / 米家预警）

### 2.1 功能清单（观测）

| # | 功能 | 入口 |
|---|---|---|
| 1 | 预警中心首页（按位置/按订阅区展示最近预警） | `WarningCenterMainActivity`（`miui.intent.action.WARNINGCENTER_MAIN`，`Manifest:1447-1450`） |
| 2 | 灾害预警总页 + 强提醒/系统提醒两级开关与接收等级 | `disasterwarning.WarningCenterDisasterActivity`（`WARNINGCENTER_DISASTER`，`:1549-1552`） |
| 3 | 订阅地区管理（省/市/区三级 + 每区接收等级） | `WarningCenterDisasterSubscribeAreaActivity`、`WarningCenterDisasterAreaAdapter/StatusAdapter/NormalAdapter` |
| 4 | 灾害预警列表（按等级筛选） | `WarningCenterDisasterListActivity` + `WarningCenterDisasterListAdapter` |
| 5 | 灾害详情（图标 + 官方防范指南） | `WarningCenterDisasterDetailActivity` |
| 6 | 全屏强预警页（红/橙级） | `WarningCenterDisasterAlertActivity`（`singleInstance`，`DisasterAlertTheme`） |
| 7 | 米家(Mijia)预警：账号绑定、预警弹窗、取消 | `mijia/**`、`WarningCenterMijiaActivity`（`WARNINGCENTER_MIJIA`） |
| 8 | 米家预警全屏窗 | `mijia.WarningCenterAlertActivity` + `AlertWindowHelper` + `AlertWindowViewModel` |
| 9 | 警务协助（policeassist：定位/隐私悬浮窗、AES 加密上报） | `policeassist.PaService`（`:remote`）、`PaSettingActivity`（`WARNINGCENTER_POLICE_ASSIST`，`:520-523`） |
| 10 | 顶部胶囊/状态栏入口 | `MiuiStatusBarCapsule` |

### 2.2 灾害预警类型编码（`11A01 / 11B01 / 11D01 / fire` 的含义）（观测）

`disasterwarning/model/EventType.java:10-34` 给出**完整码表**，均为 12379（国家预警信息发布中心）事件类型码：

| code | 中文（字符串资源） | 枚举 |
|---|---|---|
| `11B01` | 台风 | Typhoon |
| `11B03` | 暴雨 | Rainstorm |
| `11B04` | 暴雪 | Snowstorm |
| `11B05` | 寒潮 | ColdWave |
| `11B15` | 冰雹 | Hailstone |
| `11B06` | 大风 | Hurricane |
| `11B09` | 高温 | Hot |
| `11B17` | 大雾 | Fog |
| `11B19` | 霾 | Haze |
| `11B21` | 道路结冰 | FrozenRoad |
| `11B29` | 重污染 | Pollution |
| `11E06` | 海浪 | Wave |
| `11C02` | 地震（**灾害预警侧**的地震，区别于 §3 的地震预警） | Earthquake |
| `11E02` | 风暴潮 | WindStorm |
| `11B20` | 雷雨大风 | Thunderstorm |
| `11B25` | 森林火险 | ForestFire |
| `11B37` | 地质灾害气象风险 | Disaster |
| `11A01` | **洪水** | Flood |
| `11A02` | 内涝 | WaterLogging |
| `11A51` | 山洪 | FlashFlood |
| `11D01` | **滑坡** | LandSlide |
| `11D02` | 泥石流 | Mudslide |
| `11G01` | 森林火灾（境内） | ForestFireWithinTerritory |
| `11G05` | 草原火灾（境内） | GrasslandFireWithinTerritory |
| `null` | 其他 | Other |

- 首字母即大类：`11A`=水旱灾害、`11B`=气象、`11C`=地震、`11D`=地质灾害、`11E`=海洋、`11G`=森林草原火灾（`推断`，与国标《预警信息发布规范》编码一致）。
- 等级与位掩码（`model/Severity.java:8-11`）：`blue=1, yellow=2, orange=4, red=8`；四色文案 `warningcenter_disaster_level_1..4`。
- `assets/warningcenter` 的 `fire` 不是事件码，而是**图标名**：`warningcenter_fire_{blue|orange|red|yellow}.webp`（`apk_full/assets/warningcenter/`），对应 `ForestFireWithinTerritory/GrasslandFireWithinTerritory` 复用同一套火图标（`EventType.java:32-33` 两者的 `iconRes` 均为 `warningcenter_disaster_icon_huozai`）。

**`assets/warningcenter` 的用途（观测）**：71 个文件，命名 `warningcenter_<eventCode>_<severity>.(png|webp)`，由 JSON 索引间接引用——

- `assets/warning_center_disaster_icon`（5,363 B，JSON 数组）：`[{"code":"11A01","data":{"blue":"warningcenter_11A01_blue.webp",...}}, …]`；
- 取用逻辑：`WarningCenterDisasterDetailActivity` 先按 `eventType.code` 在 JSON 里找条目，再 `data.get(severity.name().toLowerCase())` 拿到文件名，`Drawable.createFromStream(assets.open("warningcenter/" + fileName))`（`:341-370`）。
- 详情页顶部大图另用 `assets/warning_center_disaster_icon` 之外的 drawable（`R.drawable.warningcenter_disaster_icon_*`，`EventType.getIconRes()`）。
- `观测`：并非所有「code × severity」都有图——例如 `11B07/11B09/11B14/11B16/11B19/11B21/11B22` 缺 `blue`，`11A01/11A02/11A51/11D01/11D02/fire` 用 `.webp`，其余 `.png` → 缺图时 `getOfficialGuidance`/icon 加载返回 `null`（`:361-373` 的 `return null` 分支）。

### 2.3 `warning_center_disaster_guide_text` 是什么（观测，已读原文）

**它是 assets 文件，不是字符串资源**：`apk_full/assets/warning_center_disaster_guide_text`，14,041 字节 UTF-8 JSON：

```json
[ {"code":"11B01","data":{"blue":"1.政府及相关部门按照职责做好防台风准备工作；…",
                        "yellow":"…","orange":"…","red":"…"} }, … ]
```

- 读取实现：`WarningCenterDisasterDetailActivity$Companion.getOfficialGuidance(assets, code, severity)` → `assets.open("warning_center_disaster_guide_text")` → `JsonParser.parseReader(...)` → 线性匹配 `code` → `data.<severity>` → 返回文本（`:97-129`）。
- 内容性质：**中国气象局/应急部门的官方「防范指南」标准条文**，按「事件类型 × 四色等级」给出 3~5 条行动建议（如 11B01 台风红色：停止集会停课停业、回港避风、加固搭建物…）。
- 调用点：详情页 `bindData`（`$bindData$2$guideText$1.java:52`）与全屏强预警页（`WarningCenterDisasterAlertActivity.java:340,368,465,493`——四条分支分别对应「强提醒/系统提醒 × 是否折叠」）。
- 缺省：找不到 code/severity 时返回 `""`（`:128`）。

### 2.4 订阅、匹配与推送（观测）

1. **推送通道同为 MiPush topic**（与地震预警共用同一套 `com.miui.push.a`）：
   - topic 前缀 `PREX_TOPIC = "12379W_"`（`disasterwarning/service/WarningCenterDisasterService.java:44`）
   - topic 实际名 = `"12379W_" + MD5(districtId + UUID_DISASTER_CODE) + "_" + registLevel`（`uploadTopic():163-172`），`UUID_DISASTER_CODE="0279a99a-80f9-42ec-8945-bfe7d5070ed6"`（`DisasterConstants.java:29`）
   - 订阅/退订：`a.b(ctx).d(topic,null)` / `.f(topic,null)`；`deleteAllTopic()` 扫设备上所有 topic 删掉前缀为 `12379W_` 的（`WarningCenterDisasterService.java:67-90`）
2. **接收等级是位掩码**：`getRegistLevel()` = `Σ (LEVEL[i] & strongLevel) | (LEVEL[i] & systemLevel)`（`disasterwarning/Utils.java:99-123`）；默认 `strongLevel=8`（红）、`systemLevel=12`（红|橙）（`:125-139`）；即**强提醒默认只收红色，系统提醒默认收红+橙**。
3. **是否弹窗**由两级掩码判定：`if (Utils.getSystemPushToggle() && (systemPushLevel & level) == level)` …（`WarningCenterDisasterManager.java:348,361`）。
4. **定位→订阅当前区**：`WarningCenterDisasterService.getLocation()` 用 `LocationRecordManager.startLocation(true, cb)`，成功后拿经纬度请求 `RequestAreaCodeTask.execute(lng, lat)`，得 `districtId` 后 `uploadTopic`，并 `deleteTopic(上一个)`（`:92-126`）；且 **12 小时节流**（`UPLOAD_TOPIC_INTERVAL=43200000`，`:45,56`）+ 屏幕点亮触发（`:48-65`）。
5. **地区码来源**：`https://api.sec.miui.com/12379/warning/getRegionInfo`（`DisasterConstants.java:24`，SALT `7war5230-a1cf-3k79-ec73-75cb82145n5x`）。
6. **云端下发的预警怎么进来**：`XiaomiPushReceiver`（§2.5）→ `WarningCenterDisasterManager.parseQuake()` / `parseAccurateWarningData()`。
7. **米家预警**：`WarningCenterProvider.call("setMijiaAccount", …)` 由**米家 App** 触发（见 §2.6）；`WarningCenterManager.handlePush()` 处理 MiPush 的 `MijiaWarning` 模块；取消上报 `POST https://warning-mijia.sec.miui.com/mijia/warning/cancel`（`mijia/api/MijiaRemoteApi.java:11-23`，SALT `6f875c57-e561-56b5-84a1-68b55ad68066`）。

### 2.5 推送分发中枢（观测，跨模块关键）

`com/miui/push/XiaomiPushReceiver.java` 是全 APK 的 MiPush 透传消息总入口（`onReceivePassThroughMessage`，`:322-331`），按 `content.SecurityCenterPassThroughMessage.moduleName` 分发：

| moduleName | 处理 | 证据 |
|---|---|---|
| `wakePath` | 转 `miui.intent.action.DELAY_UPDATE_WAKEPATH` 给 `com.lbe.security.miui`（`forceUpdate` 可选） | `:82-99` |
| `monthReport` | 弹通知，PendingIntent 打开 `securitycenter://monthreport?content=<data>`，仅 `c.a()` 且非国际版 | `:101-110` |
| `illegalSales` | `b0.a(ctx).c(data)`（非法销售模块） | `:111-114` |
| `antivirus`（`StatConstants.Channel.ANTIVIRUS`） | `e.e(ctx).h(info)` | `:115-118` |
| `luckymoney` | `LuckyPushUtil.processCMD` | `:119-122` |
| `cloudControl` | 广播 `com.android.settings.action.PULL_CLOUD_DATA`（带 `FLAG_RECEIVER_INCLUDE_BACKGROUND`） | `:123-131` |
| `earthquakeWarning` | `quake` + `quakeSign` 用 `EARTHQUAKE_PUBKEY` **SHA1WithRSA 验签**后 `EarthquakeWarningManager.showWarningInfo` | `:132-145` |
| `MijiaWarning` | `WarningCenterManager.getInstance().handlePush` | `:146-149` |
| `12379Warning` | `warning` + `warningSign` 用**灾害专用公钥**验签后 `WarningCenterDisasterManager.parseQuake` | `:150-167` |
| `accurate-warning` | 同上验签 → `parseAccurateWarningData`（精准预警） | `:168-185` |

MiPush 初始化：`com/miui/push/a.java:60` `n.I(ctx, "2882303761517330652", "5691733067652")`（appId / appKey，`观测`）；topic 接口 `n.d0/n.i0/n.h0`（`:64-77`）。
> `推断`：**预警最终来源是小米服务端**（由 MiPush 下发），APK 内不含「直接对接 ICA/成都高新减灾所地震台网」的协议代码；`ICL` 只是向小米服务端查询「某渠道历史预警」时的渠道标识（见 §3.3）。

### 2.6 数据与持久化（观测）

| 库/表 | 定义 | 说明 |
|---|---|---|
| `warning_center_disaster.db` v2 | `db/DisasterDBHelper.java:13-17` | 三张表 |
| `disaster` | `:51`：`_id, description, effective, eventType, eventTypeCN, expires, headline, identifier, msgType, note, referencesInfo, sender, severity, receivePosition, warningType, instruction, province, city, county, location`（21 列） | 已收到的灾害预警（CAP 协议字段：`identifier/msgType/references/severity/effective/expires`） |
| `area` | `:42`：`_id, code(INTEGER), province, city, region` | 地区字典（云端下发） |
| `subscribe` | `:60`：`_id, code, province, city, region, subscribeLevel(INTEGER)` | 已订阅地区 + 接收等级位掩码 |

- Providers（全部 `exported=false`，进程 `:remote`）：`com.miui.warningcenter.DisasterContentProvider`（`disaster`）、`…DisasterAreaProvider`（`area`）、`…DisasterSubscribeProvider`（`subscribe`）——`DisasterContentProvider.java:23`、`DisasterAreaProvider.java:23`、`DisasterSubscribeProvider.java:23` 均只注册单一路径。
- **`com.miui.warningcenter.provider`（WarningCenterProvider）例外：`exported=true` 且无 permission**（`Manifest:1463`），只实现了 `call("setMijiaAccount")`（`provider/WarningCenterProvider.java:45-83`），其余 `query/insert/update/delete` 全返回 `null/0`。
- SP 键（`DisasterConstants.java:14-22`）：`key_disaster_first_update`、`key_disaster_strong_level`、`key_disaster_strong_toggle`、`key_disaster_system_level`、`key_disaster_system_toggle`、`key_disaster_update_area_time`、`key_disaster_warning_set_previous_area`、`key_disaster_warning_set_previous_topic`、`key_disaster_warning_set_topic_time`。

### 2.7 暴露面与权限校验（观测，值得注意）

`WarningCenterProvider.matchSmartHome()`（`:32-41`）是本 APK 里少见的**双向校验**：

1. `getCallingPackage()` 必须等于 `com.xiaomi.smarthome`；
2. 且该包的签名证书 SHA-256 必须等于硬编码 `B0:31:FE:98:A4:DB:B0:D4:D8:26:61:78:7F:25:DE:64:31:82:B3:78:E9:EF:63:2D:8A:DE:A7:5A:AB:58:F2:D8`（`:29`，用 `eg.c.f(pm.getPackageInfo(pkg,64))` 提取）；
3. 任一不满足直接 `return null`（`:51-53`），且国际版直接禁用（`:47`）。

→ `观测`：这是「签名固定（certificate pinning）+ 调用方白名单」的跨应用授权模型；`推断`：可被用于判断「米家是否原版安装」，但**不构成任意应用可用的能力**。

---

## 3. `com.miui.earthquakewarning`（地震预警）

### 3.1 功能清单（观测）

| # | 功能 | 入口/证据 |
|---|---|---|
| 1 | 地震预警主页（开关、接收烈度阈值、最近预警） | `ui/EarthquakeWarningMainActivity`（`miui.intent.action.EARTHQUAKE_WARNING_MAIN`） |
| 2 | 全屏强预警（倒计时 + 数字语音播报 + SOS 样式） | `ui/EarthquakeWarningAlertActivity`（`AlarmAlertFullScreenTheme`，`singleInstance`） |
| 3 | 订阅地区（按城市码；最多多个） | `ui/EarthquakeWarningSubscribeAreaActivity` |
| 4 | 预警历史/未收到列表 | `ui/EarthquakeWarningListActivity`、`EarthquakeWarningListFragment`、`EarthquakeWarningUnreceivedListFragment` |
| 5 | 预警说明/法律条款/隐私 | `EarthquakeWarningInfoActivity`、`EarthquakeWarningGuideActivity`、`EarthquakeWarningGuideLawFragment` |
| 6 | 预警演示（不打扰真实推送的演练） | `EarthquakeWarningDemoFragment`、`EarthquakeWarningGuideSampleActivity`；文案「地震预警（演习）/（测试）/（试运行）」`strings.xml:1720-1723` |
| 7 | 应急信息卡（血型/过敏/用药/器官捐献/病史/身份证/生日/紧急联系人） | `EarthquakeWarningEmergencyActivity/…EditActivity` + `Constants.PREF_KEY_*`（`:51-60`） |
| 8 | 志愿者监测（加入/退出、贡献榜、分享） | `ui/EarthquakeWarningMonitorActivity`、`MonitorOpenFragment`、`MonitorShareFragment` |
| 9 | 避难点导航（高德/百度） | `EarthquakeWarningManager.java:62-65`：`androidamap://poi?…keywords=应急避难场所`、`baidumap://map/place/nearby?query=应急避难场所` |
| 10 | 锁屏/息屏亮屏与音量/亮度临时接管 | `Constants.PREFERENCE_KEY_PREVIOUS_VOLUME/BRIGHTNESS/BRIGHTNESS_MODE`（`:43-48`，`推断`：弹窗期间调音量与亮度，结束恢复） |

### 3.2 全链路（观测）

```
MiPush 透传 (moduleName="earthquakeWarning")
   └─ XiaomiPushReceiver:132-145
        ├─ SHA1WithRSA.virefy(quake.toString(), quakeSign, EARTHQUAKE_PUBKEY)   ← 验签，失败即丢弃
        └─ EarthquakeWarningManager.showWarningInfo(ctx, quakeJson, countries)

showWarningInfo
   ├─ parseQuake(json, cityCode)        EarthquakeWarningManager.java:497-527
   │    字段: index, magnitude, longitude, latitude, epicenter, depth,
   │          type, startTime, updateTime, xmUpdateTime, channel, eventId, signature[]
   ├─ getWhiteList(ctx, item)           :236-257
   │    type!=1 → 直接进入定位；type==1 → 需 Mi 账号 + 白名单校验
   │    POST https://api.sec.miui.com/universalWhitelist/checkUserId  (params: accountId, "miuisec")
   └─ getLocationStep(ctx, item)        :153-232
        ├─ LocationUtils.getAdminAreaLocation2(...)  ← 逆地理 + 定位
        ├─ item.calIC(IntensityTransformer.DEFAULT)  ← 计算本地预计烈度；过期则 PUSH_EXPIRED
        ├─ intensity >= Utils.getSelectIntensity() ?
        │     是 → ManageDataTask(insert) + showAlarmNotification(强提醒) 
        │     否 → 同一 eventId 已弹过 → 再弹一次（去重按 eventId）
        │          → 否则若「低烈度预警」开启 → showLowEarthquakeWarning(普通通知)
        │          → 否则埋点 PUSH_ERROR_INTENSITY_LOW / DO_NOT_REACH_THRESHOLD
        ├─ 城市匹配：cityCode 含本地区码 → matchMyPositionInSupport(:263-281) → 支持区列表校验
        └─ matchSignature(...)  :287-495
             POST https://srv.sec.miui.com/earthquake/warning/signature  (channel=…)
             从返回 datas[channel].data[code].signs[] 取「避险指引」多行文本
             查找时对 code 做 100^N 截断逐级回退（i16=(i16/100^i)*100^i，:378-381）
```

**预警源判定（重点回答）**

- `观测`：APK 内**没有**任何直连地震监测网/台网的 HTTP 或 socket 协议；预警唯一入口是 **MiPush 透传消息**（§2.5）→ `推断`：**预警由小米服务端产生并下发**，终端只做验签、判定、展示。
- `观测`：`quake.channel` 字段由服务端给出（`EarthquakeWarningManager.java:510`），APK 内只有一处**硬编码渠道标识** `"ICL"`：
  - `EarthquakeWarningManager.java:718` `requestSignatureTask.execute("ICL")`
  - `ui/EarthquakeWarningUnreceivedListFragment.java:103` `RequestSignatureTask.download("ICL")`
  - `推断`：`ICL` = 成都高新减灾研究所（Institute of Care-life）的渠道代号，即**小米地震预警的预警数据提供方之一**。`未知`：APK 内不存在 `"ICA"` 或其他渠道常量，`channel` 的完整取值集合由服务端决定，无法从客户端枚举。
- `观测`：志愿者/监测网方向是**反过来的**——手机作为传感器把数据**上报**给小米（§3.4），而非接收。

### 3.3 订阅与推送（观测）

- 城市订阅 topic：`MD5(cityCode + UUID_CITY_CODE)`，`UUID_CITY_CODE="7bhr4579-a8we-3k79-ec73-45678324bh5c"`（`Constants.java:77`）；`setTopicForPush()`（`EarthquakeWarningManager.java:762-764`）/`unsetTopicForPush()`（`:793-795`）。
- 订阅生效点：`RequestAreaCodeTask`（`service/RequestAreaCodeTask.java:70`）、`EarthquakeWarningMainFragment.java:101`、`EarthquakeWarningSubscribeAreaActivity.java:373`、`EarthquakeWarningService.onStartCommand`（`:307`）。
- 区域解析：`POST https://api.sec.miui.com/LBS/geoconv/admin`（`Constants.java:61`，`areaCodeResult.getData().getDistrictId()`）。
- 支持城市：`GET https://srv.sec.miui.com/earthquake/warning/supportCityCode`（`:67`）；历史预警：`https://srv.sec.miui.com/earthquake/warning/records`（`:68`）；签名/指引：`https://srv.sec.miui.com/earthquake/warning/signature`（`:66`）。
- 账号白名单：`https://api.sec.miui.com/universalWhitelist/checkUserId`（`:69`，UUID `f31feb6a-…`）。
- 开关双写：`SharedPreferences` + `Settings.Secure["key_open_earthquake_warning"]`（`utils/Utils.java:122-125,253-254`）→ 系统设置页可读（`推断`：供 Settings/通知栏联动）。

### 3.4 志愿者传感器上报（重点：`/earthquake/volunteer/sensor/upload`）

**数据来源（观测）**：不是标准 `TYPE_ACCELEROMETER`，而是**厂商私有传感器类型 ID `33171100`**：

- `Constants.EQM_SENSOR_ID = 33171100`（`Constants.java:11`）
- 注册：`sensorManager.getDefaultSensor(33171100, true)` + `registerListener(this, sensor, 0)`（`service/EarthquakeWarningService.java:136-142`；samplingPeriod `0` = 最快）
- 用途探测：`Utils.java:314` 也用同一 ID 判断设备是否具备该传感器。

**上报字段（观测，全部来自代码，非推测）**：`service/EarthquakeMonitorDataManager.threadPoolRun()`（`:51-83`）

| 字段 | 取值 |
|---|---|
| `oaid` | `w.a(context)`（设备匿名 ID，`EarthquakeWarningService.java:120`） |
| `lng` / `lat` | 最近一次定位（优先 GCJ 转换后的值，`EarthquakeWarningService.java:174-179` 用 `xi.a.b(...)`） |
| `timestamp` | `System.currentTimeMillis()` |
| `model` | `getprop ro.product.device` |
| `rom` | `getprop ro.miui.ui.version.name` |
| `token` | `Utils.getEarthquakeVolunteerToken()`（志愿者注册后下发） |
| `data` | **三轴加速度采样数组的 JSON 序列化字符串**（`ewTranData.arrays` → `JSONArray.toString()`） |

- 传输：`EarthquakeMonitorUtils.uploadData(url, map)` → `POST application/x-www-form-urlencoded; charset=UTF-8`，连接/读超时 18s，且先过 `x.z()`（网络可用）判断（`EarthquakeMonitorUtils.java:54-78`）；请求参数会经 `j.b(map, UUID_MONITOR_DATA_UPLOAD)` 加签（`:64`，UUID `5cggc678-cddf-4269-ab73-780174cvfba3`）。
- 节流/联动：每帧 `onSensorChanged` 都会 `EarthquakeMonitorUtils.exitSleepMode()`（`:285`）→ 向 `com.miui.powerkeeper` 发广播 `com.miui.powerkeeper_request_wake`（`reason=1`=DESK_CLOCK），**10 分钟内只发一次**（`EarthquakeMonitorUtils.java:36-52`）→ `推断`：让省电策略在"疑似震动"时唤醒设备以保证采样连续。
- 定位刷新：`LOCATION_TIME` SP 键 + **10 分钟**节流，触发 `startLocationSingleUpdate`（gps/network 双 provider，`2500ms`/`0m`，10s 超时；`EarthquakeMonitorDataManager.java:129-208`）。
- 志愿者注册/注销/人数：`…/volunteer/register`、`…/volunteer/cancelRegister`、`…/volunteer/countVolunteers`（`Constants.java:63-65`）。
- 隐私声明：`https://privacy.mi.com/earthquake_monitor/`（`:72`）。

> `未知`：传感器 33171100 在 HAL 层的实际物理含义（是加速度计的"地震模式"采样、还是专用低功耗唤醒通道）。`观测`：解包 `lib/` 中无对应厂商 HAL so，故只能到 Java 侧为止。
> `未知`：`data` 数组是原始三轴值还是已滤波/降采样结果（`EwTranData.arrays` 只是 `float[]`，本 APK 不做预处理，直接把 `sensorEvent.values.clone()` 透传，见 `EarthquakeWarningService.java:278`）。

### 3.5 倒计时 / SOS 音（观测）

- 倒计时秒数**不是**服务端给的，而是端侧用经验走时公式算的：`soundplay/CalcCountdown.java`
  - `SLOPE[]`/`INTERCEPT[]` 各 7 组（`:6-7`），`ARRAY[]` 是一张「震中距 → P 波/S 波走时」的查表（`:8`，数百项，例如 `0,0,10.637,0,0,22.264,15.738,6.619,…`）
  - `getCountDownSeconds(float,float)`（`:18`）、`getIntensity(float,float)`（`:43`）、`distance(...)`（`:10`，`EARTH_RADIUS=6371.0`）
  - → `推断`：按 (震中距, 震源深度) 反演 S 波到达时间作为倒计时，同时按衰减关系算本地烈度。
- 播报音：`soundplay/PlaySound`，`SoundPool(15, 3, 100)`（`:19`）加载 14 个 `res/raw` 音节：
  `one…ten`（一~十）、`non`（"零"）、`f20281di`（"点"）、`didi`（提示音）、`wu`（"五"/或"无"）。
  - `play(int countdownValue, float intensity)`（`:141-150`）：按烈度选播报风格——`intensity<=3 → 0`，`<=5 → 1`，`>5 → 2`；风格差异体现在数字之间插入 `non/f20281di/didi`（`addToPlayList():39-111`）
  - 播放节奏：`size>11 ? mInterval(500ms) : 1280ms`（`:124-131`）
- 与 UI 的耦合：`EarthquakeWarningAlertActivity` 订阅 `viewModel.getCountdown()`，值变化即 `mPlaySound.play(value, intensity)`（`:352-355,372-375`），`onCountdown`（`:279`）、`stop/release`（`:301,626-629`）。
- 时间基准：`soundplay/SntpClient`（5KB）→ `推断`：用 NTP 校正本地时钟，保证倒计时与服务器时间一致（`未知`：是否在预警路径上被实际调用，需确认调用点；本文件未见 `PlaySound` 直接引用它）。

### 3.6 `EARTHQUAKE_WARNING_MAIN` 导出 Activity 的暴露面（观测）

- 组件：`com.miui.earthquakewarning.ui.EarthquakeWarningMainActivity`
  - `exported="true"`、`screenOrientation="portrait"`、`theme=Theme.Settings.Miuix`、`label=@string/ew_app_name`（`AndroidManifest.xml:276`）
  - intent-filter：`miui.intent.action.EARTHQUAKE_WARNING_MAIN` + `category.DEFAULT`（`:277-280`）
- 同类还有一批 exported 的兄弟组件（观测 `Manifest`）：
  | 组件 | exported | 说明 |
  |---|---|---|
  | `EarthquakeWarningEmergencyEditActivity` | true | 应急信息编辑（`:1418`） |
  | `EarthquakeWarningMonitorActivity` | true | 志愿者监测页（`:1426`） |
  | `EarthquakeWarningAlertActivity` | **false** | 全屏强预警，只能内部拉起（`:1403`） |
  | `EarthquakeWarningService` | false | `:remote`（`:1407`） |
  | `EarthquakeContentProvider` / `…Provider2` | false | `:1424-1425` |
- **风险面评估（`推断`）**：
  1. `EARTHQUAKE_WARNING_MAIN` 无 permission、无调用方校验 → 任何应用可拉起主界面（低危：仅 UI）。
  2. 真正的高危动作在 `EarthquakeWarningService.onStartCommand`：接受 action `action_subscribe_area` / `action_unsubscribe_area` / `action_join_volunteer` / `action_exit_volunteer` / `action_restore_data` / `action_exit_earthquakewarning`（`:37-42`），且**该 Service 是 `exported=false`** → 只能本 App 启动，外部无法直接伪造订阅/退订（`观测`：约束是"Service 不可导出"，而非 action 鉴权）。
  3. `SearchProvider` 里还注册了 `miui.intent.action.EARTHQUAKE_WARNING_MAIN_ID`（`securitycenter/provider/SecSettingsSearchProvider.java:370`）→ 两个 action 指向同一 Activity。
  4. `EarthquakeWarningAlertActivity` 带 `miui.continuity.policy=5`（`Manifest:1405`）→ 与「小米妙享/continuity」跨设备策略相关（`推断`：允许流转到平板/车机）。

### 3.7 数据与持久化（观测）

| 库/表 | 定义 | 列 |
|---|---|---|
| `earthquake_warning2.db` v4 | `db/EarthquakeDBHelper.java:11-16` | 三表 |
| `earthquake` | `:63` | `_id, eventID, index_ew, magnitude, longitude, latitude, myLongitude, myLatitude, epicenter, startTime, signature, distance, intensity, depth, type, updateTime, warntime, cityCode, subscribe, isMyCity` |
| `subscribe` | `:84` | `_id, code, city, updateTime, support, counties, subscribeTime` |
| `area` | `:26` (+`:32-33,48`) | `_id, province, code, city, support, counties`（v3 起 `province` 被 `ALTER` 成 INTEGER，历史迁移见 `:44-56`） |

- Providers：`content://com.miui.earthquakewarning.EarthquakeContentProvider2/{earthquake|area|subscribe}`（`EarthquakeWarningManager.java:61,68-70`），`exported=false`。
- 文件：签名缓存 `/sdcard?/earthquakewarning/<SIGNATURE_FILE_NAME>`（`utils/FileUtils.java:10,33,37`，实际根目录由 `y.c/y.h` 决定，`未知`：最终路径）。
- SP 键：`Constants.java:29-60`（`key_open_earthquake_warning`、`key_open_low_earthquake_warning`、`key_select_push_intensity`、`key_earthquake_warning_monitor_token/order`、`key_earthquake_warning_previous_area_code`、`key_earthquake_warning_previous_volume/brightness/_mode`、应急信息 11 个 `preference_key_earthquake_warning_emergency_*` 等）。

---

## 4. `com.miui.electricrisk` —— **命名严重误导：实际是「AI 反诈防护」，与用电/电池无关**

### 4.1 纠偏结论（观测，本节最重要）

包名叫 `electricrisk`（"电风险"），但**与电池、充电、鼓包、漏电、高温一律无关**：

- 全包检索 `BatteryManager|power_supply|sysfs|EXTRA_HEALTH|EXTRA_TEMPERATURE|BATTERY_PROPERTY|therm|charger` → **0 命中**（唯一 `http` 命中是隐私政策 URL，`ElectricProtectPhoneFragment.java:99`）。
- 正面证据（证明它是什么）：
  - `ElectricRiskMainActivity` 的 `android:label` = **`@string/system_anti_fraud_dialog_message`**（`Manifest:284`）；
  - `AiGuardPosterActivity` 注册 `com.android.settings.action.AI_ENHANCED_SETTINGS`，meta-data `key = "ai_anti_fraud"`（`Manifest:291-299`）；
  - 卡片只有五种：电话 / 短信 / 应用 / **网页** / 屏幕共享（`ElectricRiskMainFragment.java:64-75` 装配 `protect_phone/protect_mms/protect_app/protect_web/protect_screen_share`）；
  - 能力位 `CAPABILITY_PHONE=1`、`CAPABILITY_VIDEO=2`（`AiGuardUtils.java:15-17`）。
- 结论：**"用电风险/充电保护"不在本模块**。本 APK 内的「充电保护」在 `com.miui.powercenter.nightcharge.ChargeProtectFragment`（省电与电池模块，属另一份分析范围）；`assets/charge_current_config.json` 也属 powercenter。

### 4.2 功能清单（观测）

| # | 功能 | 实现 |
|---|---|---|
| 1 | 反诈防护总页（5 张卡，按能力位隐藏） | `ElectricRiskMainActivity`（`miui.intent.action.ELECTRIC_RISK`）+ `ElectricRiskMainFragment` |
| 2 | 电话反诈（AI 识别通话中的诈骗） | `ElectricProtectPhone*` + `AiGuardSceneService` 的 `PHONE_SCAM_SCENE=2` |
| 3 | 短信反诈 | `ElectricProtectMms*` |
| 4 | 应用风险管控 | `ElectricProtectApp*` |
| 5 | 网页风险提示 | 卡片 4 → 发 `com.android.browser.OPEN_SETTINGS`（extra `current_page_type=31`，`ElectricProtectionFunctionPreference.java:139-150`） |
| 6 | 屏幕共享防护 | `ElectricProtectScreen*` |
| 7 | 视频通话反诈 | `AiGuardSceneService` 的 `VIDEO_SCAM_SCENE=1/2` |
| 8 | 云控可用性 + 机型黑名单 | `AiGuardCloudController` |
| 9 | "AI 增强设置"入口页（可被动态禁停） | `AiGuardPosterActivity` + `AiGuardUtils.setPosterActivityState():62-64` |

### 4.3 数据来源（观测，逐条）

| 场景 | 数据源 | 证据 |
|---|---|---|
| 通话状态 | `TelephonyManager.listen(PhoneStateListener, LISTEN_CALL_STATE)` | `AiGuardSceneService.java:158`、`:117` |
| 来号码判定 | ① 本地联系人 `ContactsContract.CommonDataKinds.Phone`：`replace(data1,' ','') LIKE ?`；② 黄页 `YellowPageUtils.getPhoneInfo(...)` | `AiGuardSceneService.java:1335`、`:1360` |
| VoIP/视频通话（微信） | `AudioManager.addOnModeChangedListener` + `getActiveRecordingConfigurations()` 取 `getClientPackageName()` | `AiGuardSceneService.java:1238`、`:1191-1197` |
| VoIP 录音状态广播 | `miui.media.AUDIO_VOIP_RECORD_STATE_CHANGED_ACTION`（发送方需 `com.miui.permission.AUDIO_VOIP_RECORD_STATE_CHANGED_ACTION`），读 extra `"pkg"` | `AiGuardSceneService.java:1116`、`:1081` |
| 前台 Activity（链式观测） | `monitorActivities(list1, list2)` → 回调 `onActivityChanged(caller, callee)` | `AiGuardSceneService.java:1276-1278`、`AiGuardSceneService$monitorActivities$1.java:98` |
| 监控目标包 | 微信 `AppConstants.Package.PACKAGE_NAME_MM`、QQ `PACKAGE_NAME_QQ`；视频页 `com.tencent.mm.plugin.voip.ui.VideoActivity`、`com.tencent.av.ui.AVActivity`；告警页 `com.miui.guardprovider/.aiguard.AlertActivity` | `electricrisk/f.java:14,18,22` |
| 能力门控 | `SystemProperties "debug.config.media.video.voip.detect"`；`Build.DEVICE ∈ {dada, haotian}`；`com.miui.guardprovider` 的 `meta-data["miui.aiGuardVersion"] >= 1` 且 `SDK_INT >= 35` | `AiGuardUtils.java:23-39` |
| 是否允许联网 | `x.z()` → `SystemProperties KEY_SECURITY_CENTER_ALLOW_CONNECT_NETWORK`（决定走哪个授权页） | `x4/l1.java:11-19` |

### 4.4 `AiGuardSceneService` 做什么（实现逻辑，观测）

1. **启动**：`Application.java:773-781` 反射调 `AiGuardUtils.isAiGuardSupported/isAiGuardEnabled` + `setPosterActivityState(...)`，满足条件才 `startService`。服务在 `:remote` 进程、`exported=false`（`Manifest:283`），只有 `onCreate/onBind(null)/onDestroy`，**没有 `onStartCommand`**（`:1365-1383`）。
2. **只做"场景触发"，不做检测**：命中后向 **`com.miui.guardprovider/.aiguard.AiGuardService`** 发显式 Intent，extras 为整型：
   - `COMMAND`（1 = 电话场景、2 = 视频场景）
   - `PHONE_SCAM_SCENE`（1..4）、`VIDEO_SCAM_SCENE`（1..2）
   - `CAPABILITIES`（能力位）
   - 证据：`AiGuardSceneService.java:212`、`:367-370`、`:1260-1264`
3. **场景码含义**（观测）：来电"陌生号（不在联系人且非黄页）"→ `COMMAND=1, PHONE_SCAM_SCENE=2`（`:210-214`，判定函数 `j()` 语义见 `:1267-1269`）；微信 → `PHONE_SCAM_SCENE=3 / VIDEO_SCAM_SCENE=1`；QQ → `PHONE_SCAM_SCENE=4 / VIDEO_SCAM_SCENE=2`（`:355-371`）。
4. **收尾**：通话回 `IDLE` → 停 guardprovider 服务（`:215-217`、`:1281-1283`）。
5. **前置拦截**：`l()` 比较进程槽位 `z1.e()` 与创建时 `z1.y()`，非当前用户直接返回（`:1272-1274`）。
6. `推断`：真正的语音/画面判定模型在 **guardprovider**（另一个 APK），本包不持有任何模型或音频处理代码（`观测`支撑：本包无 `MNN/AudioRecord/MediaCodec`）。
7. ⚠️ **`Command` 是空注解**（`Command.java:7-10`），`@interface Command{}`，**不参与**指令下发。真正的"命令"是整型 extra `"COMMAND"`。这是一处容易误判的坑。

### 4.5 MNN / `assets/model` / `assets/rec_model_20230425.mnn` 的真实归属（观测）

| 资产 | 消费者 | 与 electricrisk 关系 |
|---|---|---|
| `assets/rec_model_20230425.mnn`（810,908 B） | `com.miui.mlkit.mobilerec`：`ia/j.java:88-116` 复制到 `filesDir/apprecmodel` 并 `MNNNetNative.nativeCreateNetFromFile` | **无** |
| `libMNN/libMNN_Express/libMNNTrain/libmnncore.so` | `com.miui.mlkit.mobilerec.ranking.mnn.MNNNetNative` / `MNNTrainNative` | **无** |
| `assets/model`（680,154 B，非 MNN/GGUF 头） | `lj/c.java:53` `getAssets().open(...)`，白名单数组 `{"dict","model","pattern","phish"}` → 网页/钓鱼特征库 | **无** |

- MNN 推理的输入/输出张量名：输入 `sparse_inputs_1..6` + `sparse_inputs_time`，输出 `linear_9.tmp_1`（`ia/j.java:138-144`、`:262`）；端上训练 `MNNTrainNative.nativeTrainRecModel(...)`，落盘 `apprecmodel/checkpoint_<n>`（`:451,:467`）。
- 全仓不存在 `MNNNetInstance`（真实类名 `MNNNetNative`）→ 若看到"`AiGuardSceneService` 用 MNN"的说法，属于**误判**。
- `未知`：`lj.d.h()` jadx 反编译失败，`assets/model` 的具体算法不可见。

### 4.6 云端接口与持久化（观测）

- **`AiGuardCloudController` 没有任何 URL**（模块内 `http|OkHttp|Retrofit|HttpURLConnection|p4.c` 0 命中）。它是**云控下发读取**，走 MIUI `MiuiSettingsCompat$SettingsCloudData`：
  - module `AiGuardAvailability`；data key `ai_guard_function_available`（bool）、`deviceBlacklist`（JSON 数组，默认 **58 个机型代号**，`liuqin/yudi/malachite/…/matisse`，`AiGuardCloudController.java:19`）
  - 判定式：`cloudSwitch && !deviceBlacklisted`（`:52`）；不可用 → 写 SP → 关开关 → `stopService(AiGuardSceneService)` → `stopService(com.miui.guardprovider/.aiguard.AiGuardService)`（`:55-63`）
- 唯一 URL：`https://privacy.mi.com/AIalert/{lang}_{COUNTRY}`（`ElectricProtectPhoneFragment.java:99`，`ACTION_VIEW`）。
- 持久化键（观测）：

| 类型 | 键 | 语义 | 证据 |
|---|---|---|---|
| SP（默认） | `ai_guard_function_switch` | AI 防护总开关（默认 false） | `AiGuardUtils.java:47`、`h.java:22` |
| SP 文件 | `ai_guard_function_cloud_data` / `ai_guard_function_available` | 云控缓存 | `AiGuardCloudController.java:21,40,56` |
| `Settings.Secure` | `com.miui.guardprovider.AI_GUARD_PRIVACY_AGREEMENT` | guardprovider 隐私协议（=1 才可用） | `ElectricProtectPhoneFragment.java:139,318` |
| `Settings.Secure` | `settings_sms_risk_warn_enable`(1) / `settings_sms_code_clipboard_warn_enable`(1) / `screen_share_protection`(1) / `com.android.browser.web_page_warning_states`(1) | 各卡片子开关 | `h.java:54,106,128,143` |
| `Settings.System` | `cloud_antispam`、`key_yellowpage_main_switch`、`pref_never_show_smart_antispam_user_notice` | 黄页/来电识别 | `h.java:110,178-179` |
| 远程偏好（`content://com.miui.securitycenter.remoteprovider`） | `key_risk_application_warning_enable`、`key_risk_sms_and_call_pay_warn_enable` | 风险提示 | `ef/x.java:197-202`、`r4/a.java:20-40` |

- 关闭/撤销流程：10 秒倒计时确认对话框 → 写 `ai_guard_function_switch=false` → 停两个服务 → 写 `AI_GUARD_PRIVACY_AGREEMENT=0`（`ElectricProtectPhoneFragment.java:135-170`）。无数据库、无本地模型。

### 4.7 组件与暴露面（观测）

| 组件 | exported | permission | filter |
|---|---|---|---|
| `AiGuardSceneService` | false | 无 | 无（`:remote`） |
| `ElectricRiskMainActivity` | **true** | **无** | `miui.intent.action.ELECTRIC_RISK` |
| `ElectricProtectPhoneActivity` | **true** | **无** | 无 |
| `AiGuardPosterActivity` | **true** | **无** | `com.android.settings.action.AI_ENHANCED_SETTINGS` + 4 个 meta-data |
| `ElectricProtectMms/App/ScreenActivity` | false | 无 | 无 |

→ 3 个 exported 且无权限保护的 Activity（`Manifest:284,290,291`）。

---

## 5. `com.miui.simlock`（SIM 卡锁 / 防换卡）

### 5.1 功能清单（观测）

| 类 | 职责 |
|---|---|
| `SimLockManager` | 单例 + `ISimLockManager.Stub` 实现；PIN/PUK 自动校验、通知、绑定判定（`SimLockManager.java:40,279-288`） |
| `ISimLockManager` | AIDL，描述符 `com.miui.simlock.ISimLockManager`，**无任何业务方法**（仅 DESCRIPTOR 分支，`ISimLockManager.java:9,53-59`） |
| `SimLockMonitorService` | 首个监控服务：注册 4 类广播 + `SubscriptionManager` 监听，`onBind` 返回 Manager Binder（`:136-161`） |
| `SimLockMonitorNormalService` | 同上，`onStartCommand` 返回 `START_STICKY`（`:6-11`），进程 `:remote` |
| `SimLockNotificationService` | 通知落地：`notification_extra=0` → 设置页；`=1` → 重发 PIN 页（`:57-67`、`:22-40`） |
| `BootCompleteReceiver` | `LOCKED_BOOT_COMPLETED` + 开关打开 → 拉起监控服务（`:13-23`） |
| `a` | PIN 的 AES-GCM 加解密（`:154-176,206-226`） |
| `c` | 反射 Telephony 隐藏 API / 状态判定 / Intent 构造（`:62-319`） |
| `b` | 卫星通信状态接收（`com.android.app.action.SATELLITE_STATE_CHANGE`，`:38-46,80-105`） |
| `SimLockStartActivity/Fragment` | 开通主流程 3 步：锁屏密码 → 小米账号 → 设 PIN（`:154-208`） |
| `SimLockChooseActivity/Fragment` | 双卡选择（`:24-45`） |
| `SimLockPinActivity` | PIN/PUK 输入状态机（825 行，`:378-399,619-686`） |
| `SimLockQuery(All)Activity/Fragment` | 查询/修改 PIN（先过锁屏密码，`:62-65,110-158`） |
| `SimLockSettingActivity/Fragment` | 设置页：总开关 `master_switch` + 查询入口（`:79-86`） |
| `SimLockDoneActivity` | 完成页；**PIN 为 `1234` 时弹"建议修改"**（`:135-140`、`:125-131`） |
| `SuccessDialog(Activity/NormalActivity)` | 启动成功弹窗（bootaware / 普通双版本） |

### 5.2 完整逻辑（观测）

1. **开机**：`BootCompleteReceiver`（directBootAware，`:bootaware`）收 `LOCKED_BOOT_COMPLETED`；仅当 `c.l()`（国内 + SDK≥30 + 主用户 + 平板例外）且 `Settings.Secure["sim_lock_enable"] != 0` 才 `startService(SimLockMonitorService)`（`BootCompleteReceiver.java:15-21`、`c.java:187-196`）。
2. **监控注册**：`SubscriptionManager.addOnSubscriptionsChangedListener` + 广播 `ACTION_SIM_STATE_CHANGED`、`ACTION_USER_PRESENT`、`android.miui.intent.action.SIM_LOCKED`、`DEVICE_POLICY_MANAGER_STATE_CHANGED`（`SimLockMonitorService.java:148-160`）。
3. **状态解析**：`fg.e.a(intent)` 把 `"ss"`（`ABSENT/READY/LOCKED/NETWORK/CARD_IO_ERROR/...`）与 `"reason"`（`PIN/PUK/PERM_DISABLED`）映射成 `r1.a` 枚举（`fg/e.java:30-83`）。
4. **自动解锁（核心）** `M8(e)`：
   - `PUK_REQUIRED` → `H8([slot])` 弹 PUK 输入；
   - `PIN_REQUIRED` 且 `A8(slot)==true` → 读已存 PIN → `c.u(subId, pin)`；
     - `result[0]==0`（成功）：卫星通信开启时**静默不弹窗**；锁屏状态则 `mPendingSuccess=true` 等 `USER_PRESENT`；否则 `mSuccessSlots.add(slot)` + 5s 后弹成功页（`:229-277`、`:396-400`）；
     - `result[0]==1`（失败）：写回 PIN 记录 `F8(pin, subId, true, 12)` 并弹输入页（`:260-266`）。
5. **PIN/PUK 状态机**（`SimLockPinActivity`，extra `pin_state` 默认 2、`slot_ids` 为 `ArrayList<Integer>`）：
   - `0/1/2/3` → 设/取消 ICC 锁：`c.g(subId)` 读 `isIccLockEnabled`，`enable = (state==0 || (state==2 && !locked))`，再 `c.r(...)`=`setIccLockEnabled`（`:237-243,394-398`）；
   - `4` → 校验 PIN（`c.u`），成功后 `c.q(...,12)` 保存并切下一张卡（`:360-362,628-648,593-617`）；
   - `6` → PUK 对话框提交（`:365-371`）；
   - `>=8` → `c.v(subId, puk, newPin)`=`supplyPukReportResultForSubscriber`（`:650-686`）；
   - 剩余次数取返回数组 `[1]`（`:310-317`）。
6. **完成**：`T0(pin)` 写 `Settings.Secure["sim_lock_enable"]=1` → `SimLockDoneActivity`（`:584-591`）。
7. **通知兜底**：锁屏态通知 id=`OP_DELETE_SMS`(10011)（`:336-352`）；PIN 锁定 id=`10086`（`:363-367`）；拔卡 id=`10000`（`:449-474`）；通道 `notify_channel_name_security`（`fg/h.java:30-33`）。
8. **卫星通信联动**：`is_enable=true` → `STATE_OPEN` 并**临时写 `sim_lock_enable=1`**；`false` → `STATE_CLOSING`，60s 后 `STATE_CLOSED` 回写 0（`b.java:86-104`、`SimLockManager.java:413-433`）。
9. **前置条件** `A8/z8`：已设锁屏密码 `c.i()`、小米账号存在 `c.m()`、`sim_lock_enable != 0`、该 ICCID 记录满足 `f()==true && g()==false && e()!=null`（`SimLockManager.java:310-325,529-547`、`c.java:152-154,198-200`）。
10. `SimLockMonitorService.onDestroy` 在 bootaware 进程且 `c.f21538d==false` 时 `Process.killProcess(myPid())`（`:169-176`）。

### 5.3 用的系统服务 / 反射（观测）

| 用途 | 调用 | 证据 |
|---|---|---|
| 读 ICC 锁 | 反射 `TelephonyManager.isIccLockEnabled()`（`createForSubscriptionId(subId)`） | `c.java:135-146` |
| 写 ICC 锁 | 反射 `setIccLockEnabled(boolean, String)`；SDK>30 返回 `PinResult`，用 `getResult()` 比 `PIN_RESULT_TYPE_SUCCESS`，失败取 `getAttemptsRemaining()` | `c.java:280-298,156-159` |
| 改 PIN | 反射 `changeIccLockPassword(String,String)` | `c.java:62-80` |
| 验 PIN | `ITelephony$Stub.asInterface(ServiceManager.checkService("phone")).supplyPinReportResultForSubscriber(int,String)` | `c.java:313-315` |
| 解 PUK | 同上 `.supplyPukReportResultForSubscriber(int,String,String)` | `c.java:317-319` |
| 卡信息 | `miui.telephony.SubscriptionManager` + `SubscriptionInfo.getIccId/getSlotId/getSubscriptionId/getDisplayName` | `SimLockManager.java:115-117,221-227` |
| 锁屏态 | `KeyguardManager.inKeyguardRestrictedInputMode()` / `isKeyguardSecure()` | `c.java:148-154` |
| 小米账号 | `miui.accounts.ExtraAccountManager.getXiaomiAccount()`；`AccountManager.addAccount("com.xiaomi","passportapi",...)` | `c.java:198-200`、`SimLockStartFragment.java:155` |
| 锁屏密码校验 | 显式 `com.android.settings/.MiuiConfirmCommonPassword`、`.MiuiSecurityChooseUnlock` | `SimLockStartFragment.java:184,197` |
| 排除虚拟 SIM | `MiuiSettings.VirtualSim.isVirtualSimEnabled/getVirtualSimSlotId` | `vb/a.java:31-33` |

### 5.4 数据与持久化（观测，重点）

| 类型 | 名称 | 内容 | 证据 |
|---|---|---|---|
| 设备加密文件 | `<deviceProtected filesDir>/encryped_pin.pwd`（原文拼写如此） | `IV(12B) ‖ AES-GCM 密文`；明文 = JSON `Map<iccId, {iccId, pin, phoneNumber, displayName, bool, bool}>` | `a.java:174-176,206-212,43-70` |
| Keystore 别名 | `key_pin_alias`（AES/GCM/NoPadding，`KeyGenParameterSpec` purposes=3） | 加解密 PIN 的密钥 | `a.java:164,217-222` |
| `Settings.Secure` | `sim_lock_enable`（0/1） | 总开关 | `SimLockPinActivity.java:585`、`SimLockSettingFragment.java:47`、`SimLockManager.java:422,429` |
| r4 远程偏好 | `sim_lock_enable`（布尔镜像） | "是否由系统临时开启"标记 | `SimLockManager.java:421,425-428` |
| 埋点 | `key_sim_lock_enable`、`key_sim_lock_bind_enable`、`key_sim_lock_bind_success`、`key_pin_lock_dialog_*`、`key_puk_lock_dialog_*`、`key_change_pin_dialog_*`、`key_success_dialog_*`、`key_cloud_dialog_*`、`key_keyguard_dialog_*`、`key_close_dialog_*` | 流程漏斗 | `fg/i.java:5-11`、`SimLockStartFragment.java:69-100`、`SimLockPinActivity.java:356,367,535,539,552,559,572` |

> **安全要点（观测）**：PIN 是**可逆加密落盘**，Keystore 密钥未设 `setUserAuthenticationRequired`（`a.java:218`）→ 只要进程能读该文件并可访问该 Keystore 别名即可还原 PIN；另 `SimLockPinActivity.onSaveInstanceState` 把用户输入写进 `Bundle` 键 `key_input`（`:818`），存在被系统快照持久化的可能（`推断`）。

### 5.5 云端接口（观测）

**无**。`com/miui/simlock/**` 检索 `http|Retrofit|OkHttp|URL(|p4.c|HttpURLConnection` → 0 命中。唯一"联网语义"是埋点（`fg/i`）与小米账号登录（AccountManager），都由其它 SDK 承载。

### 5.6 组件与暴露面（观测）

| 组件 | exported | permission | 备注 |
|---|---|---|---|
| `SimLockChooseActivity` | **true** | **无** | `miui.intent.action.SIM_LOCK_CHOOSE`（`Manifest:391-396`） |
| `SimLockStartActivity` | **true** | `com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE` | `SIM_LOCK_START`（`:397-402`） |
| **`BootCompleteReceiver`** | **true** | **无** | `action.LOCKED_BOOT_COMPLETED`，directBootAware，`:bootaware`（`:415-419`） |
| `SimLockQuery/QueryAll/Pin/Done/SuccessDialog/Setting` | false | 无 | 内部页 |
| `SimLockMonitorService` | false | 无 | directBootAware，`:bootaware`（`:420`） |
| `SimLockMonitorNormalService` / `SimLockNotificationService` | false | 无 | `:remote`（`:421-422`） |

→ `BootCompleteReceiver` 是 **exported 且无 permission** 的 `LOCKED_BOOT_COMPLETED` 接收器，第三方可伪造该广播拉起监控服务（内部仍受 `sim_lock_enable` + `c.l()` 门控，实际危害限于"重复初始化"）`推断`。
→ 调用方补充：`com/miui/permcenter/q.java:436-438` 会 `startService(SimLockMonitorNormalService)` 并注册卫星状态接收器（`:71,73` 启动/停止）。

---

## 6. `com.miui.bubbles`（通知气泡）

### 6.1 结论：**自研悬浮窗，不是 Android Bubbles API**（观测）

- 全包 `BubbleMetadata` / `setBubbleMetadata` → **0 命中**。
- 实证 `BubbleController.addToWindowManagerMaybe()`（`BubbleController.java:141-168`）：
  ```java
  WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2002, 16777256, -3);
  mWindowManager.addView(mStackView, mWmLayoutParams);
  ```
  `type=2002`（`TYPE_SYSTEM_ALERT`）、`flags=0x1000028`（`HARDWARE_ACCELERATED|NOT_TOUCH_MODAL|NOT_FOCUSABLE`）、`format=-3`（TRANSLUCENT）。
- 即"全屏透明悬浮层 + `BubbleStackView` 自绘"，不是系统通知气泡。

### 6.2 形态与实现要点（观测）

| 形态 | 实现 | 说明 |
|---|---|---|
| 通知气泡栈 | `BubbleStackView`(33KB)、`BubbleController`、`BubblePositioner` | 拖拽/吸附；`BubblePositioner` 读 `Settings.Secure["sidebar_bounds"]`（`:118`）并用 `getCurrentWindowMetrics()`（`:374`） |
| 气泡内容 | `views/BubbleMessageView`(25KB)、`views/BubbleImageView` | 展开视图 |
| 小窗 | `controller/FreeFormController` + `utils/MiuiFreeFormManagerWrapper` | 点击气泡 → 自由窗口打开 App（`推断`） |
| 弹幕 | `controller/MiuiBarrageController` | 游戏场景把通知变弹幕；键 `gb_bullet_chat`、`gb_boosting`、`enabled_notification_listeners` |
| 通知监听 | `services/BubblesNotificationListenerService` | `Manifest:449`：exported=true、`BIND_NOTIFICATION_LISTENER_SERVICE`、`enabled="false"`、`singleUser=true`、`:remote` |
| 进程桥 | `BubblesService`(`:ui`)、`BubbleRemoteService`(`:remote`)、`MiuiBubbleServiceManager`、`IUiProcessBinder`(6.8KB AIDL) | |
| 设置 | `BubblesSettingsActivity`(`com.miui.bubbles.BUBBLES_SETTINGS`)、`BubbleAppManageActivity` | 均 exported=true，`:ui` |

### 6.3 Settings 键（观测，原文）

| 键 | 作用域 | 含义 |
|---|---|---|
| `miui_bubbles_notification_switch` | Secure(getIntForUser) | 总开关（默认 1） |
| `miui_notification_bubbles_support` | Secure | 设备支持（默认 1） |
| `miui_bubbles_pinned_apps` | Secure(String) | 允许出气泡的应用 |
| `have_notification_bubbles` | Secure | 是否曾出现过（引导） |
| `all_app_workespace_status` | Secure | 显示/隐藏气泡（`BubblesManager.java:16`，拼写原文） |
| `sidebar_bounds` | Secure(String) | 侧边栏区 |
| `enabled_notification_listeners` | Secure | 已授权监听器 |
| `gb_boosting` / `gb_bullet_chat` / `gb_notification` | Secure | 游戏加速/弹幕/游戏内通知 |
| `miui_mirror_dnd_mode` | Secure(getIntForUser) | 投屏勿扰 |
| `settings_focus_mode_status` | Global | 专注模式（`BubblesService.java:209`） |
| `heads_up_notifications_enabled` | Global | Heads-up 样式（`BubbleUpManager.java:63`） |
| `miui_fsgesture_state` | Global | 全面屏手势（`:68`） |
| `show_bubble_tip_in_game_mode` | Secure | 游戏内提示只弹一次（`TipsManager.java:114,126`） |

- 埋点键（`BubbleMessageTrackUtil.java:33-39`）：`hide_window_notification_click/expose/retract`、`notification_switch_status`、`enter`、`set`。
- ⚠️ 写入 `miui_bubble_app_settings`（`BubblesSettings.java:119`）而读取常量是 `miui_bubbles_pinned_apps`（`:29,215`）→ 键名不一致，`推断`：历史遗留写入路径。

---

## 7. `com.miui.monthreport`（月度报告）

### 7.1 架构（观测）

```
[写入方] --ContentResolver.insert--> content://com.miui.monthreport/report_json
   MonthReportProvider.insert()                    (MonthReportProvider.java:31-53)
     ├─ 必填 eventType；缺 eventId → eventType+UUID+ts；缺 moduleName → "monthReport"
     └─ a.f().i(values) → SQLite 落库
  ↓
 month_report.db / report_json                     (a.java:42,46,50)
  ↓  c.java TaskManager
 POST https://api.sec.miui.com/data/check  {module}     (c.java:157)
  ↓ code==1（且非移动网络或待传<20000）
 POST https://data.sec.miui.com/data/upload            (c.java:199)
```

### 7.2 表结构（观测）

`month_report.db` v2，单表 `report_json`（`a.java:42-50`）：
`_id INTEGER PK AUTOINCREMENT, eventId TEXT, eventTime INTEGER, pkgName TEXT, eventType INTEGER, version INTEGER, data TEXT, moduleName TEXT`
（`moduleName` 由 v1→v2 `ALTER TABLE ... DEFAULT monthReport` 加入，`:50`。）清理：删空 `eventId`（`:138`）、按 `eventId` 批删（`:153`）、`eventTime <` 过期删（`:173`）。

### 7.3 谁写入 / 写什么（观测）

**网路助手是明确写入方**（`com/miui/networkassistant/utils/DataUsageReportUtil.java`）：

| eventType | version | 内容 | 证据 |
|---|---|---|---|
| `105` | `4` | 昨日流量**总量** | `:9,12,31-33` |
| `106` | `1` | 昨日流量**明细** | `:10,11,35-37` |

- `eventId = SHA-1(String(eventType) + String(eventTime))`（`:17`）→ 幂等键；`pkgName = 调用方包名`（`:18`）；`URL = "content://com.miui.monthreport/report_json"`（`:13`，`insert` 见 `:25`）。
- 触发方：`Application.java:334` 初始化 `c.m(context)`；`ConnectivityChangeJobService2.java:419` `.p()` 冲刷；`XiaomiPushReceiver` 的 `monthReport` 模块用于推送展示（`securitycenter://monthreport?content=`，§2.5）。
- 声明面：清单同时声明 `com.miui.monthreport.permission.DATA_MONTH_REPORT`（signatureOrSystem，`Manifest:41`）与 `uses-permission`（`Manifest:136`）→ 写入方是系统级 App（antispam 等）。

### 7.4 上报报文（观测，`b.java`）

| 报文键 | 含义 | 来源 |
|---|---|---|
| `reportId` | 上报 ID | `UUID(去横线)+毫秒`（`:188`） |
| `d` / `c` / `r` | 设备代号 / 运营商名 / 区域 | `ro.product.device` / `ro.carrier.name` / `miui.os.Build.getRegion()`（`:114-116`） |
| `l` | 语言 | `Locale.getDefault().toString()`（`:117`） |
| `v` | 版本串 | `"MIUI-" + Build.VERSION.INCREMENTAL`（`:118`） |
| `t` | 时间戳 | `x4.a0.h()`（`:119`） |
| `a` | 设备指纹哈希 | `MD5(deviceId + "-5fdd8678-cddf-4269-bb73-48187445bba7")`（`ma/b.java:5-7`） |
| `m` | 另一哈希 | `MD5(x4.a0.e(context))`（`ma/b.java:9-11`） |
| `startTime/endTime/date` | 事件时间窗与日期 | `:229-242` |
| `events` | 事件数组（≤500 条/批） | `:160` |
| `o` | 附加串 | `x4.a0.k(context)`（`:120`） |

### 7.5 上报控制 —— **本构建被硬编码短路**（观测，关键）

- 前置条件：`x.z() && u.b(ctx) && ma.c.a()`（`c.java:149-151`），而 **`ma.c.a()` 恒 `false`**（`ma/c.java:4-5`）⇒ **本构建不会真正上传**。
- 云控准入（若可达）：`POST https://api.sec.miui.com/data/check`，body `{"module":<moduleName>}`，必须 `code==1` 且无 `error_code`（`c.java:153-174`）。
- 上传：`POST https://data.sec.miui.com/data/upload`，`data` = Base64(报文 JSON)，成功判据 = 响应不含 `error_code`（`c.java:134-147,194-201`）。
- 加签：`p4.c` → `p4.e.c(...)` 在 form 追加 `sign = g(list, appId)`，appId = `5fdd8678-cddf-4269-bb73-48187445bba7`（`p4/e.java`）。
- 节奏/限制：成功 300s 后下一 module；失败 `count<3` 按 `count*300s` 重试；每次先删 30 天前；只上报 `eventTime < 今日 00:00`；非 WiFi 且待传 > 20000 拒绝；国际版直接 return（`c.java:169-173,212-233,271-274`、`b.java:222-242`）。
- `key_month_report_enabled` 只写不读（`ma/c.java:10`，本 dex 内无读取点）`未知`。

### 7.6 组件与暴露面（观测）

| 组件 | exported | permission |
|---|---|---|
| `com.miui.monthreport.MonthReportProvider` | **true** | `com.miui.monthreport.permission.DATA_MONTH_REPORT`（signatureOrSystem，`Manifest:41,905`） |

---

## 8. `com.miui.net` / `com.miui.vpnsdkmanager` / `com.miui.wakepath`

### 8.1 `com.miui.net`（2 个薄封装；**均有实际调用点**，勿信"无调用"说法）

| 类 | 用途 | 调用点 |
|---|---|---|
| `MiuiNetworkSessionStats` | 封装 APK 内自带的 `miui.securitycenter.net.MiuiNetworkSessionStats`（见 §1.3），额外提供 `isBandwidthModuleEnable()`（`SystemProperties "net.qtaguid_enabled"`）与 `openSession()` 异常兜底通知 | `traffic/statistic/StatisticAppTraffic.java:186-189`（`initStatsSession`） |
| `MiuiNetworkPolicyManager` | 封装 **framework 类** `miui.securitycenter.net.MiuiNetworkPolicyManager`（APK 内**无该类定义**，§1.7 注）：`get/setAppRestrictBackground(uid,bool)`、`get/setRestrictBackground(bool)` | `firewall/BackgroundPolicyService.java:9,22,102,112,117,131` |

> `观测`：`miui.securitycenter.net.MiuiNetworkPolicyManager` 全树仅 7 处**引用**、0 处**定义** → 平台类；`MiuiNetworkSessionStats` 则**在 dex 内有定义**（`miui/securitycenter/net/MiuiNetworkSessionStats.java`，基于 `NetworkStatsManager`）。

### 8.2 `com.miui.vpnsdkmanager.IMiuiVpnSdkService`（1 个 AIDL）

- 描述符 `com.miui.vpnsdkmanager.IMiuiVpnSdkService`（`IMiuiVpnSdkService.java:185`），`onTransact` 覆盖 **transaction 1..32**（`:743-911`），对应接口 34 个方法（部分无参便捷方法共用一个码位组）。
- 方法分组（观测）：
  - **VPN/加速器业务**：`refreshUserState()`(5)、`H0/M4/B3/q2/w4`(6,4,7,3,23)、`getSetting/setSetting`(8,9)、`getSettingWithChannel`(31)、`n8()`(15)、`c2(boolean):byte[]`(27)、`n4(int,String,int)`(16)、`z7(int,int,int)`(17)、`detectTimeDelay(String,int)`(29)、`f7(String,int)`(10)、`getCoupons()`(11)、`b2(int,List<String>)`(2)、`registerCallback(IMiuiVpnManageServiceCallback)`(1)、`setDSDASwitch(boolean)`(30)
  - **AI 变声**：`D7/O4/Q5/S/W1/h6/I/R5`（全部带 `IMiuiVoiceChangeCallback`，码位 19,14,13,20,18,21,32,28）、`y4(String,int):List<VoiceModel>`(22)、`T1/W2`(24,26)
- `推断`：MIUI 把「VPN 加速器 SDK」与「游戏变声」塞进同一个 Binder 接口，供 `com.miui.gamebooster` / 迅游类 SDK 共用。`未知`：服务端实现与调用方鉴权（不在本 APK；`Manifest` 内 0 命中该接口字符串 → 无本 APK 声明的服务端）。

### 8.3 `com.miui.wakepath`（链式启动确认，不是"唤醒统计"）

- 组件：`ui.ConfirmStartActivity`（`Manifest:1087-1094`）：**exported=true、无 permission**，filter = **`android.app.action.CHECK_ALLOW_START_ACTIVITY`**；另有 `excludeFromRecents`、`finishOnCloseSystemDialogs`、`relinquishTaskIdentity`、`documentLaunchMode=never`、`property miui.continuity.policy=5`。
- 语义（观测）：label `app_behavior_link_start`，即"应用 A 想启动应用 B"时的用户确认。
- 入参：`CallerPkgName`、`CalleePkgName`、`UserId`、`callerUserId`、`calleeUserId`、`restrictForChain`、`restrictForGetApps`、`Intent.EXTRA_INTENT`（`ConfirmStartActivity.java:240-261`）。
- **反伪造校验 `l0()`（观测）**：仅 `SDK>33` 生效；先校验 `UserId` 属于真实 `UserProfile`；再反射 `getLaunchedFromUid()`，与 caller/callee 的包 uid、callerUserId、calleeUserId 逐一比对，任一不符即放弃（`:132-150,265-272`）。
  → ⚠️ **SDK ≤ 33 时 `l0()` 直接 `return false`（视为不可疑）**，防护退化。
- **静默放行**：callee 是微信/支付宝，或 action 为 `com.sina.weibo.sdk.action.ACTION_SDK_REQ_ACTIVITY`，或目标为 `<callee>.wxapi.WXEntryActivity` / `.wxapi.WXPayEntryActivity`（`:274-280`）。
- **非对话框提示**：其余链式场景用 `ui.e` 的 `WindowManager` **type=2038**（`TYPE_APPLICATION_OVERLAY`）悬浮提示，文案 `autostart_now_tip`，按钮 `allow_onetime`，5 秒自消失；点击 → `j0()`：`SecurityManager.addRestrictChainMaps(caller, callee)`（`:281-290,116-123`、`e.java:156-199`）。
- 用户允许（`onClick(-1)` → `p0(true)`）：`v.t(...)` 真正转发启动；若为 `PICK`/`SEND` 且 `o.f18010g` → `ContentResolver.call(StoragePolicyContract, GRANT_TEMP_PATH, {uid, packageName, RESTRICTED_PATH=ClipData 首项 lastPathSegment})` 授临时路径（`:190-230,152-161`）；否则 `r.b(ctx, pkgName, calleePkg)`（`:125-130`）；并 `PermissionContract` 的 `call(..., method=14, {pkgName, calleePkg, type=1, mode, user, EXTRA_CALLER_UID})` 记一次授权（`:163-174`）。
- 拒绝（`onClick(-2)`）：`n0(false)` + 埋点 + `setResult(0)`（`:337-341`）。
- 持久化：链式允许表在 framework（`SecurityManager.addRestrictChainMaps`），本模块**无 SP、无 DB、无云端接口**。
- ⚠️ 另有 `com.miui.permcenter.wakepath.WakePathManagerActivity`（`Manifest:614`，exported=false），是**另一个包路径**的管理页，不要与 `com.miui.wakepath` 混淆。

---

## 9. `com.miui.analytics` / `com.xiaomi.analytics` / `com.xiaomi.onetrack`（埋点）

### 9.1 结构（观测）

- `com.miui.analytics.AnalyticsUtil`：业务侧统一入口（`recordCountEvent/recordNumericEvent/recordCalculateEvent/recordPageStart/End/trackXxxEvent`），**全部转发到 `StatManager`**。
- `com.miui.analytics.StatManager`：**OneTrack SDK 的多实例管理器**（`StatManager.java:58-100`）。
- `com.miui.analytics.RemoteAnalyticsReceiver`：接收广播 **`com.miui.security.remote.analytics`**（`RemoteAnalyticsReceiver.java:13`，与 `Manifest:1400` 对应），交给 `RemoteAnalyticsManager` 在子线程投递。
- `ICore` / `ITrack`：两个 AIDL 代理接口（`ICore` 有 `trackEvent(String)/trackEvents(String[])/setDefaultPolicy/isPolicyReady/deleteAllEvents` 等 9 个 transaction；`ITrack` 有 `trackEvent/trackPSEvent`）→ `推断`：用于**跨进程/跨 App（插件化渠道，如侧边栏、输入法、游戏加速）复用同一套埋点**。
- `com.xiaomi.analytics`：旧版 Analytics SDK 的残留命名空间（同一套 `StatConstants` 通道概念）。

### 9.2 通道（Channel）与 appId（观测，`StatManager.java:17-32` + `StatConstants.java:6-19`）

| Channel 常量 | appId |
|---|---|
| `securitycenter`（默认 `APP_ID`） | `31000000037` |
| `APP_ID_V2` | `31000401489` |
| `network_assistant`（**网络助手**） | `31000000807` |
| `earthquake_warning`（**地震预警**） | `31000401472` |
| `antivirus` | `31000000943` |
| `malicious_app_track` | `31000401557` |
| `xiaomi_safety_open_service` | `31000401800` |
| `conversation_tool_box` | `31000401977`（`Mode.PLUGIN`） |
| `gameturbo` / `game_turbo_new` | `31000000560` / `31000401977` |
| `gamebox` | `31000000057` |
| `dock`（侧边栏，pluginId `securitycenter_sidebar`） | `31000000538` |
| `input_method` | `31000000494` |
| `app_man`（仅 `isPkgProcess` 时创建） | `31000000945` |
| 广告事件 | `31000000893`（`setAdEventAppId`） |
| Pad | `31000000506` |

- 初始化选项（`:41`）：`setChannel(包名)`、`setExceptionCatcherEnable(true)`、`setMode(APP/PLUGIN)`、**`setGAIDEnable(false)`**、`setAutoTrackActivityAction(false)`（antivirus/输入法）。
- 全局开关：`OneTrack.setAccessNetworkEnable(ctx, bool)`、`OneTrack.setUseSystemNetTrafficOnly()`（`:61-62`）；调试模式由 `uf.a.f35870a` 控制（`:94-96`）。

### 9.3 上报端点（观测，**以 byte[] 硬编码，规避明文 grep**）

`com/xiaomi/onetrack/util/y.java`：

| 常量 | 字节数组 | 解码结果 |
|---|---|---|
| `f23781q`（:71） | `{116,114,97,99,107,105,110,103,46,109,105,117,105,46,99,111,109}` | **`tracking.miui.com`**（CN） |
| `f23782r`（:74） | `…105,110,116,108…` | **`tracking.intl.miui.com`**（国际） |
| `f23783s`（:77） | `…114,117,115…` | **`tracking.rus.miui.com`**（俄区） |
| `f23784t`（:80） | `…105,110,100,105,97…` | **`tracking.india.miui.com`**（印度） |

路径常量（`:13-16,67`）：`/track/v4`（事件上报）、`/track/key_get`（取密钥）、`/api/v4/detail/config`、`/api/v4/detail/config_common`（配置下发）、`/api/v1/token`。
→ `推断`：完整 URL 形如 `https://tracking.miui.com/track/v4`；`观测`：主机名以字节数组形式存在，`grep "https://tracking"` 的**明文**检索会漏掉（本报告已用字节解码确证）。

### 9.4 事件名示例（观测）

`networkassistant` 侧（`utils/AnalyticsHelper.java`）：`net_control`（TRACK_ITEM_FIREWALL，`:27`）、`change_mobile_firewall`（`:49`）、`change_wlan_firewall`（`:50`）、`firewall_item`（`:82`）、`toggle_newapp_data_allow`（`:124`）、`toggle_newapp_wlan_allow`（`:125`）。
`earthquakewarning` 侧（`analytics/AnalyticHelper.java`）：`push_receive`、`push_show`、`push_error_illgal_type`、`push_error_intensity_low`、`push_error_location_failed`、`push_error_no_sign_area`、`push_error_parse_signature`、`push_error_time_long`、`toggle_earthquake_warning`（`:25-41`）。
命名拼接：`AnalyticsUtil.createEventName(String,String)` / `RemoteAnalyticsManager.createEventName(String,String)`（`AnalyticsUtil.java:19`、`RemoteAnalyticsManager.java:54`）——`未知`：具体拼接格式（未展开方法体）。

---

## 10. 未解之谜 / 需动态验证

> 每条都标注了"已搜什么、为什么没结论"，避免把"没找到"误当成"不存在"。

### 10.1 网络助手 / 防火墙

1. **防火墙最后一公里**：`SecurityManager.setMiuiFirewallRule` → framework → netd 的落地形式（`netd` 的 `bandwidth`/`firewall` 链，还是 system_server 代跑 iptables）。APK 侧无任何规则文本（§1.4），需 `services.jar` / `framework.jar`。
2. **`miui.securitycenter.net.MiuiNetworkPolicyManager` 实现**：全树 7 处引用、0 处定义（§1.7/§8.1），推测由 MIUI framework 提供。
3. **`com.miui.common.net.a.b(...)` 签名算法**：SALT 为硬编码 UUID，但摘要/拼接方式未展开（§1.9）。
4. **超限断网/限速的落地手段**：只看到 `SimUserInfo.toggleDataUsageOverLimitStopNetwork` 与 `MobileDataPolicy` 观察，未见直接改网络开关的调用点 → 需补完 `TrafficSimManager`/`ITrafficManageBinder` 全文（§1.5）。
5. **`AntiTrackCloudService` 两个 hex 常量解出的 Settings key 明文**：依赖 `m4.a.b/c`（可能 native），静态不可还原（§1.10）。

### 10.2 预警中心 / 地震预警

6. **地震预警 `channel` 取值全集**：客户端只硬编码 `"ICL"`（`EarthquakeWarningManager.java:718`、`EarthquakeWarningUnreceivedListFragment.java:103`），其余由服务端决定；`ICL` 是否等同"成都高新减灾研究所"属 `推断`（§3.2/3.3）。
7. **私有传感器 `33171100` 的 HAL 语义**：`lib/` 内无对应厂商 HAL，无法静态确认是"加速度计地震模式"还是专用唤醒通道（§3.4）。
8. **`SntpClient` 是否参与倒计时**：文件存在，但未见 `PlaySound` / `AlertActivityViewModel.startCountdown` 调用它（§3.5）。
9. **预警告警音资源**：`res/raw/{one..ten,non,f20281di,didi,wu}` 的实际听感与"点/零"映射需试听确认（`PlaySound.java:22-35` 只给了资源名）。

### 10.3 用电风险（AI 反诈）

10. **Activity 栈观测的上游注册点**：`monitorActivities` 的调用方与其依赖的系统服务（是否为 `miui.process`/`AccessibilityManager`/隐藏 ActivityManager）未定（§4.3）。
11. **`assets/model`（680 KB）算法**：`lj.d.h()` jadx 反编译失败，只能确认它是"网页/钓鱼特征库"的载入路径（`lj/c.java:53`），算法不可见（§4.5）。
12. **guardprovider 侧真实检测逻辑**：`com.miui.guardprovider/.aiguard.AiGuardService` 不在本 APK，`PHONE_SCAM_SCENE=1..4` / `VIDEO_SCAM_SCENE=1..2` 的语义与判定模型未知（§4.4）。

### 10.4 SIM 卡锁

13. **`ISimLockManager` 为何是 Binder 却无业务方法**：`ISimLockManager.java:9,53-59` 只有 DESCRIPTOR 分支，`SimLockManager` 却是 `Stub` 实现 → 可能为"占位 AIDL"或旧版兼容（§5.1）。
14. **`NETWORK_LOCKED` 被当作 PUK 处理**：`SimLockManager.java:233`、`SimLockNotificationService.java:31` 的分支把 `PUK_REQUIRED` 与网络锁混用，是否会误弹 PUK 页需动态验证（§5.2）。
15. **`SimLockMonitorService` 的 `onBind` 调用方**：`onBind` 返回 Binder，但未见外部 bind 点（`com.miui.permcenter.q` 只 `startService`）。

### 10.5 月报 / 其它

16. **monthreport 的完整写入方清单**：本报告只确证 `networkassistant` 的 105/106 两类事件（§7.3）；清单声明的 `com.miui.antispam.permission.DATA_MONTH_REPORT` 暗示 antispam 也写，但 antispam 侧调用点未定位。
17. **monthreport 上传触发点**：`c.o()/c.p()` 在本 dex 内无调用点（`Application.java:334` 只建单例，`ConnectivityChangeJobService2.java:419` 只调 `.p()` 的包装）→ 完整链路需串起来；且 `ma.c.a()` 恒 false 使上传不可达（§7.5）。
18. **`CHECK_ALLOW_START_ACTIVITY` 的 framework 发送方**：`wakepath` 只实现接收端，谁在 `startActivity` 前发该广播不在本 APK（§8.3）。
19. **`IMiuiVpnSdkService` 的服务端与宿主**：本 APK 只有 AIDL 代理，`Manifest` 内 0 命中该接口字符串，宿主 App 未知（§8.2）。
20. **OneTrack 事件名拼接规则**：`AnalyticsUtil.createEventName(String,String)`（`:19`）与 `RemoteAnalyticsManager.createEventName`（`:54`）方法体未展开，事件名格式仍是黑盒（§9.4）。
