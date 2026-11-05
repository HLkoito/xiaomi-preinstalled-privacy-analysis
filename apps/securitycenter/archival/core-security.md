> **历史分析存档，非最终结论。** 本文保留原始分析的技术内容，可能包含尚未验证或已被纠正的表述。隐私数据上传范围、同意条件、FBO 本地委托、报警短信加密、Provider 鉴权等，以 [最终报告](../REPORT.md) 与 [独立复核](../REVIEW.md) 为准。文中源码定位保留为原反编译相对路径；只有 evidence/ 中列出的精选文件随包提供，未打包完整源码树。

# MIUI 安全中心 —— 「安全与拦截」业务模块逆向分析

> 目标：`MIUISecurityCenter-10.4.9-250402.0.1.apk`（SHA-256 `fc63de0e…78cfd`）
> 分析人：Team Lead `lead` 的队友 **core-security**
> 证据基线：`_analysis/jadxout/sources/`（10624 .java）+ `_analysis/apktool_sc/`（Manifest / res）+ `_analysis/apk_full/assets/`
> 标注约定：**【观测】**= shipped artifact 直接可读事实；**【推断】**= 由代码结构强推、未动态验证；**【未知】**= 现有材料无法判定

---

## 0. 全局结构速览

### 0.1 进程模型【观测】

`AndroidManifest.xml` 里 3 个进程（统计命令：`Select-String android:process` → 100/6/3 次）：

| 进程 | 用途 | 关键组件数 |
|---|---|---|
| 主进程 `com.miui.securitycenter` | 全部 UI（293 个 Activity 绝大多数）、securityscan 首页 | — |
| `com.miui.securitycenter:ui` | 6 个组件（`of.e.r()` 等 UI 隔离） | 6 |
| `com.miui.securitycenter.remote` | **常驻后台进程**：antivirus/antispam/luckymoney/policeassist/tvm/antifraud/RemoteProvider/NotificationService/RemoteService 全部在此 | 100 |
| `com.miui.securitycenter.bootaware` | DirectBoot（FBE 解锁前）可用：`com.miui.simlock.*` | 3 |

【观测】`com.miui.securitycenter.Application#attachBaseContext` 用 `x4.f1.u(context)` 取当前进程名，据此分派 3 条完全不同的启动路径（`Application.java:683-689`、`710-842`）：

| 进程名 | 走的分支 | 做的事（节选） |
|---|---|---|
| `com.miui.securitycenter`（主） | `onCreate:717-729` | `AnalyticsUtil.initMiStats`、`registerActivityLifecycleCallbacks`、`TmBinderCacher.initForUIProcess` |
| `com.miui.securitycenter:ui` | `onCreate:730-734` | 只做 `initMiStats` + `of.e.r()` |
| `com.miui.securitycenter.remote` | `onCreate:735-841` | **全部后台能力初始化**（见下） |

【观测】remote 进程 `onCreate` 的初始化清单（`Application.java:735-841`）——这是本 APK 的"常驻守护进程"全景：

```
735  if ("com.miui.securitycenter.remote".equals(this.f20261f)) {
739      v();                                   // 删 /MIUI/debug_log/com.miui.privacy
742      L();                                   // 启用超级省电 Tile
743      w.E();
744      l.a(this);
745      B(this);   // 注册 miui.intent.action.FIREWALL_UPDATED
746      C(this);   // xf.b 白名单/黑名单初始化
747      G(this);   // monthreport + push
749-751  ConnectivityChangeJobService2.s / c3.d.g / c3.d.c
758      v.m(this, new mf.a(), CONNECTIVITY_CHANGE + action_update_sc_network_allow)
764      v.m(this, new i3.b(), ACTION_USER_PRESENT)
767      AppOpsUtilsCompat.autoOptmize(this)
769      k.a().b(this)
788-800  df.h.c/E + registerContentObserver(default_input_method)   // 输入法安全
801      j.d(this, "application")               // 应用安装监听
802      d0(this)                                // 通知权限同步到 PermissionManager(32768L)
808      Z()                                     // BehaviorWidget 开关
809      ef.a0.a(this)
816      Looper.myQueue().addIdleHandler(→ K())   // ★ 空闲时初始化"重"业务
820-831  ...
```

`K()`（`Application.java:361-410`）在 **IdleHandler** 里跑，顺序即"重量级业务清单"（【观测】）：

```
364  VirusAutoUpdateJobService.d(this);      // 病毒库每日更新 Job(99)
367  M(this);      → VideoToolBoxService
368  LuckyMoneyHelper.init(this);            // ★ 红包助手
369-373 v.w(FirewallService / TrafficManageService / MiuiVpnManageService)
375-377 WarningCenterDisasterService（非国际版且开推送）
384-391 EarthquakeWarningService（开启地震预警时）
```

### 0.2 APK 声明的对外能力面（本文档范围内的组件）【观测】

| 组件 | 类型 | exported | permission | 进程 | Manifest 行 |
|---|---|---|---|---|---|
| `com.miui.antivirus.service.GuardService` | service | false | — | remote | 835 |
| `com.miui.antivirus.service.VirusAutoUpdateJobService` | service(Job) | false | BIND_JOB_SERVICE | remote | 870 |
| `com.miui.antispam.service.AntiSpamService` | service | **true** | `com.miui.antispam.permission.DATA_MONTH_REPORT` | remote | 1193 |
| `com.miui.antispam.db.AntiSpamProvider` | provider `antispam` | **true** | `com.miui.antispam.permission.CONTENTPROVIDER` | remote | 1194 |
| `com.miui.riskcontrol.SensitiveWordDetectProvider` | provider `com.miui.riskcontrol` | **true** | `com.miui.security.permission.DETECT_TEXT` | (主) | 1733 |
| `com.miui.luckymoney.service.LuckyMoneyMonitorService` | service | false | — | remote | 1126 |
| `com.miui.luckymoney.provider.LuckyMoneyProvider` | provider `com.miui.luckymoney.provider` | **true** | `miui.permission.EXTRA_NETWORK` | remote | 1127 |
| `com.miui.warningcenter.policeassist.PaService` | service | false | — | remote | 475 |
| `com.miui.tvm.service.TvmManagerService` | service | **true**（enabled=false） | `miui.permission.TVM` | (主) | 476 |
| `com.miui.antifraud.AntiFraudReceiver` | receiver | **true** | `android.permission.MODIFY_PHONE_STATE` | remote | 433 |
| `com.miui.antifraud.PayActivityMonitorService` | service | false | — | remote | 432 |
| `com.miui.securitycenter.service.RemoteService` | service | false | — | remote | 1724 |
| `com.miui.securitycenter.service.NotificationService` | service | **true** | `…permission.NOTIFICATION_SERVICE` | remote | 1725 |
| `com.miui.securitycenter.receiver.BootReceiver` | receiver | false | — | remote | 890 |
| `com.miui.common.persistence.RemoteProvider` | provider `com.miui.securitycenter.remoteprovider` | **true** | `…permission.ACCESS_SECURITY_CENTER_PROVIDER` | remote | (flatten 表) |
| `com.xiaomi.security.xsof.XSOFProvider` | provider | **true** | — | remote | 1758 |
| `com.xiaomi.security.xsof.MiSafetyDetectService` | service | **true** | — | remote | 1759 |

对外 Action 入口（`<intent-filter>`，供系统/桌面/其他 App 调起）【观测】：
`miui.intent.action.ANTI_VIRUS`、`miui.intent.action.ANTI_VIRUS_WHITELIST`、`com.miui.securitycenter.action.ANTIVIRUS_SETTINGS`、`miui.intent.action.SAFE_PAY_MONITOR_SETTINGS`、`miui.intent.action.SET_FIREWALL`、`miui.intent.action.CALL_FIREWALL`、`miui.intent.action.VIEW_BLACKLIST`、`miui.intent.action.ADD_FIREWALL`、`miui.intent.action.REMOVE_BLACKLIST`、`com.miui.antispam.action.ANTISPAM_SETTINGS`、`miui.intent.action.HB_MAIN_ACTIVITY`、`miui.intent.action.LUCKY_MORE_SETTING`、`miui.intent.action.LUCKY_ALARM_SETTING`、`miui.intent.action.WARNINGCENTER_POLICE_ASSIST`、`miui.intent.action.START_TVM`。

### 0.3 跨进程偏好设置（贯穿全部模块）【观测】

`r4.a`（`r4/a.java`）**不是**普通 SharedPreferences，而是通过 ContentProvider `call()` 跨进程读写：

```java
// r4/a.java:19-45
private static Bundle a(String str, Bundle bundle) {
    ...
    return f34090a.call(uriBuild, "callPreference", str, bundle);
}

// r4/a.java:151-158  SET
Bundle b = ...; b.putInt("type", 1); b.putString("key", str); b.putBoolean("p", z10);
a("SET", b);
f34090a.notifyChange(Uri.withAppendedPath(Uri.parse("content://com.miui.securitycenter.remoteprovider"), str), null, false);
```

服务端 `com.miui.common.persistence.RemoteProvider`（`RemoteProvider.java:264`）：

```java
public boolean onCreate() {
    this.f12718a = getContext().getSharedPreferences("remote_provider_preferences", 0);
    if (!i()) return true;    // i() = !containsKey("key_default_preferences_saved")
    j();                      // 首次：把 PreferenceManager.getDefaultSharedPreferences 全量搬进来
    return true;
}
```

- 【观测】**模块间真正共享的偏好文件只有一个：`remote_provider_preferences`**（在 `com.miui.securitycenter.remote` 进程）。`Application` / `antivirus` / `antispam` / `securityscan` 的绝大多数开关、时间戳、包名列表都走它。
- 【观测】`type` 0=int / 1=boolean / 2=String / 3=float / 4=long / 5=StringSet；支持 `GET` / `SET` / `CONTAIN_KEY` 三种 method（`RemoteProvider.java:40-178`）。
- 【观测】有一处强制安全校验：写 `security_adb_install_enable=true` 时用 `SecurityManager#getPackageNameByPid` 反查调用方，只允许本包或 `com.miui.klo.bugreport`（`RemoteProvider.java:180-194`）。
- 【观测】同一 Provider 还带 `isFeatureSupport` / `getFeatureStatus` / `setFeatureStatus` 三个 method（`RemoteProvider.java:232-240`）。

---

## 1. `com.miui.securitycenter` —— 主 Application / 常驻服务 / 开机广播

### 1.1 功能清单

| 功能 | 入口类 | 核心类·方法 | 数据 | 云端 | 证据 |
|---|---|---|---|---|---|
| 应用进程分派（3 进程 3 套初始化） | `Application#onCreate` | `Application#attachBaseContext / onCreate` | `remote_provider_preferences` | — | `Application.java:683-842`【观测】 |
| 常驻前台服务（保活） | Manifest:1724 | `RemoteService#onCreate / onStartCommand` | `Notification` channel `securitycenter_resident_notification_hide` | — | `RemoteService.java:57-117`【观测】 |
| 状态栏"内存/清理"常驻通知 | Manifest:1725 | `NotificationService`（23.5 KB） | `t.m()` 总内存 | — | `NotificationService.java:47-90`【观测】 |
| 开机广播 | Manifest:890 | `BootReceiver#onReceive` | — | — | `BootReceiver.java:80-118`【观测】 |
| 云数据更新（缩屏清理免杀名单） | Manifest:824 | `CloudDataUpdateService#a` | `pref ver` + `items` | `POST https://api.sec.miui.com/trashCleaner/screenLock` | `CloudDataUpdateService.java:30-60`【观测】 |
| 游戏自动扫描 Job | Manifest:823 | `AutoScanGameJobService`（7.6 KB） | — | — | — |
| 隐私/凭证 Provider | Manifest:1758/1759、flatten 表 | `XSOFProvider#call`、`MiSafetyDetectService` | — | — | `XSOFProvider.java:25-32`【观测】 |
| Widget | `WidgetProvider`（16.7 KB） | — | — | — | 文件名【观测】 |

### 1.2 `RemoteService`：把 remote 进程"钉"成前台【观测】

```java
// RemoteService.java:38-49
private void a() {
    Object objH = f.h(Class.forName("android.app.ActivityManagerNative"), "getDefault", null, new Object[0]);
    if (Build.VERSION.SDK_INT >= 26) {
        f.d(objH, "setProcessImportant", new Class[]{IBinder.class, Integer.TYPE, Boolean.TYPE, String.class},
            this.f20418b, Integer.valueOf(Process.myPid()), Boolean.TRUE, StatConstants.Channel.SECURITYCENTER);
    } else {
        f.d(objH, "setProcessForeground", ...);   // 反射调用隐藏 API
    }
}
```

`onStartCommand` 的 `cmd` 分支（`RemoteService.java:95-115`）：

| cmd | 行为 |
|---|---|
| `recommend_app_installed` | `e.p(this)`（应用管理推荐） |
| `app_lock` | `e.j(intent, this)`（应用锁） |
| `competitive_app_installed` | `e.n(this, "000015")` |
| `app_installed_scan` | `e.m(this)` |
| `show_virus_notification` | `n.f20925f.a().t(this, (ArrayList) intent.getSerializableExtra("key_virus_pkg_list"))` ← **病毒结果通知入口** |
| `hide_virus_notification` | `n.f20925f.a().f(this)` |

`dump()` 还能输出构建配置（`FLAVOR : cnPhone` / `IS_FLAVOR_REGION_CN : true`）与 `PackageVerificationReceiver` 状态 —— `RemoteService.java:51-71`【观测】。

### 1.3 `BootReceiver`：开机做了什么【观测】

`BootReceiver.java:81-118` 顺序执行：

1. `z1.e() != z1.g(context)` → 跨用户/切用户时重置部分开关（`x.W/x.E/t.R("com.android.settings",0,false)`）
2. `r1.a(context)`、`j.d(context)`、`c3.f.m0/C0`（统计）
3. **`SecurityManager#startServiceAsUser(AntiSpamService, CURRENT_OR_SELF)`** ← 反射拉起拦截服务
4. `TrafficManageService` 启动
5. `fe.f.b(0L)`、`b(context)=jd.a.b/c`、`d(context)`（`PowerSaveService` 发 `TRY_CLOSE_SAVE_MODE`）、`c(context)=g.l`、`a()`（首启时间戳）
6. `e.r(context)` 与 `e(context)`：
   - `e()` → `SecurityManager#updateLauncherPackageNames`（反射）
7. `t.h()==9` 时，对 `{com.miui.player, com.miui.fm, com.android.soundrecorder, com.android.browser, com.miui.voiceassist, com.mipay.wallet}` 逐个把通知开关写入 `PermissionManager.setApplicationPermission(32768L, …)`（`BootReceiver.java:31,105-115`）
8. `com.miui.common.base.asyn.a.a(new a(context))` → `le.b.a/b`（非国际版）

### 1.4 持久化

| 载体 | 名称 | 内容 | 证据 |
|---|---|---|---|
| SharedPreferences | `remote_provider_preferences` | 全 APK 跨进程共享偏好（含全部安全开关） | `RemoteProvider.java:264`【观测】 |
| SharedPreferences | `credential_service_config` | `is_credential_configured` | `Application.java:542-547`【观测】 |
| SharedPreferences | `SmsEnginePreference` | SMS 引擎版本/时间（见 §4） | `j2/a.java:316`【观测】 |
| Settings.Secure | `security_adb_install_enable`、`accessibility_button_targets`、`power_supersave_tile_enabled`、`credential_service*`、`default_input_method` | 系统级开关 | 多处【观测】 |
| 文件 | `files/scanitem.json_v{0,1}`、`files/resource.zip`、`files/SmsEngineResource/` | 见 §2/§4 | `ModelFactory.java:88-95`、`j2/a.java:177`【观测】 |
| Provider | `content://com.miui.securitycenter.remoteprovider` | 见 0.3 | 【观测】 |

---

## 2. `com.miui.securityscan` —— 首页"一键体检"

（133 个 .java，987.6 KB；`MainFragment.java` 单文件 153 KB）

### 2.1 功能清单

| 功能 | 入口 | 核心类·方法 | 关键数据 | 云端 | 证据 |
|---|---|---|---|---|---|
| 首页 + 一键体检 | `MainEntryActivity`（**Manifest:238**，`enabled=false`，导出；intent-filter 4 组：`android.intent.action.MAIN`、`mi.quicksearch.intent.action.RETRIEVE`、`miui.intent.action.SECURITY_CENTER`、`VIEW securitycenter://home:8888/mainActivity` — Manifest:239-256）→ 实际 UI `com.miui.securityscan.MainActivity`（**Manifest:546**） | `MainFragment`（153 KB） | `assets/scanitem.json` | 无 | 【观测】 |
| 扫描总调度 | `k.n(context)` 单例 | `com.miui.securityscan.scanner.k#w/z/v` | — | — | `scanner/k.java:840-960`【观测】 |
| 打分 | 同上 | `scanner.ScoreManager#o/q/i` | — | — | `ScoreManager.java:167-220,445-509`【观测】 |
| 定时/后台扫描 | Manifest:1590 `ScanJobService` | `ScanJobService.c(context)` | — | — | `Application.java:178`【观测】 |
| 体检项（两组） | `ModelFactory#produceManualGroupModel / produceSystemGroupModel` | `AbsModel#scan/optimize` | `scanitem.json` | — | `ModelFactory.java:213-229`【观测】 |
| 急救箱（另一套清单） | `ModelFactory#produceFirstAidKitGroupModel` | 同上 | `assets/firstaid_scanitem.json`（5 组：Performance/Internet/Operation/ConsumePower/Other） | — | `ModelFactory.java:174-202`【观测】 |
| 桌面快捷方式（清理/网络诊断/游戏加速） | `shortcut.ShortcutActivity`（11.1 KB） | `shortcut.d#q/v` | 枚举 `shortcut.d.b`：`CLEANMASTER(2)/NETWORK_DIAGNOSTICS(12)/GAME_TURBO(14)` + 更多 | — | `shortcut/d.java:102-116`【观测】 |
| 图片隐私保护（相册删除拦截） | `fileobserver.ImageProtectService`（23.8 KB）/ `DelImgInterceptActivity`（22.9 KB） | — | — | — | 文件名【观测】 |
| 设置 / 白名单 / 关于 | `ui/settings/*` | `SettingsFragment`（27.2 KB）、`WhiteListActivity` | — | — | 【观测】 |
| 结果页 | `OptimizeAndResultActivity`（50.3 KB） | — | — | — | 【观测】 |

### 2.2 "一键体检"检查项全表 —— 直接来自 `assets/scanitem.json`

`scanitem.json` 顶层三个键：`manual`（28 组）、`system`（8 组）、`version`。每个 item 有 `key`/`name`(实现类全限定名)/`version`(适用渠道 alpha/development/stable)/`score`(扣分权重)。
【观测】加载逻辑（`ModelFactory.java:159-172`）：`Class.forName(name).getConstructor(String.class, Integer.class).newInstance(key, score)` —— **反射实例化**；`version` 里必须含 `x4.a0.h()` 返回值否则跳过。

**manual（手动项，28 组；同组内为"择优展示"的候选，`GroupModel#scan` 取第一个非 SAFE 项）**

| # | key | 权重 | 实现类 | 组内候选 |
|---|---|---|---|---|
| 0 | `VIRUS` | 0 | `model.system.VirusScanModel` | 单 |
| 1 | `CLEANER_SHORTCUT` | 3 | `model.manualitem.CleanerShortcutModel` | 单 |
| 2 | `DARK_MODEL` | 0 | `model.manualitem.DarkModel` | 单 |
| 3 | `NETWORK_SWITCH` | 5 | `…NetworkSwitchModel` | 7 项同组：`VIRUS_CLOUDSCAN(0)`、`CLEANER_CLOUDSCAN(0)`、`GARBAGE_LIB(0)`、`FLOW_NOTIFICATION(0)`、`TELECOM_OPERAOTR(0)`、`AUTO_UPDATE(2)` |
| 4 | `GARBAGE_CLEAN` | 5 | `…GarbageCleanModel` | 单 |
| 5 | `MI_SIM_LOCK` | 5 | `…MiSimLockModel` | 单 |
| 6 | `DEFAULT_LAUNCHER` | 0 | `…defaultapp.DefaultLauncherModel` | 8 项同组：`DEFAULT_DIAL/MESSAGING/BROWSER/CAMERA/GALLERY/MUSIC/EMAIL`（全 0） |
| 7 | `DATA_FLOW` | 3 | `…DataPackageModel` | 单 |
| 8 | `KEY_SYNC` | 3 | `…KeySyncModel` | 单 |
| 9 | `PAYMENT_RISK` | 3 | `…PaymentRiskModel` | 单 |
| 10 | `SAFE_MODE` | 3 | `…SafeModeModel` | 单 |
| 11 | `FIND_DEVICE` | 3 | `…FindDeviceModel` | 单 |
| 12 | `FLOW_VERIFY` | 0 | `…PackageVerifyModel` | 单 |
| 13 | `FLOW_VERIFY_FAILED` | 0 | `…PackageVerifyFailedModel` | 单 |
| 14 | `CLOUD_SERVICE` | 2 | `…CloudServiceModel` | 单 |
| 15 | `MIFI_INSURANCE` | 0 | `…MifiInsuranceModel` | 单 |
| 16 | `MIUI_UPDATE` | 5 | `…MiuiUpdaterModel` | 单 |
| 17 | `FIRST_AID_KIT` | 0 | `…FirstAidModel` | 单 |
| 18 | `PERMISSION_ROOT` | 0 | `…PermissionRootModel` | 单 |
| 19 | `APP_UPDATE` | 0 | `…AppUpdateModel` | 单 |
| 20 | `PERMANENT_NOTIFICATIONBAR` | 0 | `…SecurityNotificationModel` | 单 |
| 21 | `FLOW_PURCHASE` | 0 | `…DataPurchaseModel` | 单 |
| 22 | `CLOUD_SPACE` | 0 | `…CloudSpaceModel` | 单 |
| 23 | `FLOW_RANK` | 0 | `…FlowRankModel` | 单 |
| 24 | `MI_INPUTMETHOD` | 5 | `…MiInputMethodModel` | 单 |
| 25 | `CONSUME_POWER_RANK` | 0 | `…ConsumePowerRankModel` | 单 |
| 26 | `APP_LOCK` | 0 | `…AppLockModel` | 单 |
| 27 | `SHUT_DOWN_PASSWORD` | 5 | `…ShutDownPasswordModel` | 单 |

**system（系统项，8 组）**

| # | key | 权重 | 实现类 |
|---|---|---|---|
| 0 | `VIRUS_LIB` | 5 | `model.system.VirusDbModel` |
| 1 | `SYSTEM_AUTO_DOWNLOAD` | 0 | `model.system.AutoDownloadModel` |
| 2 | `INSTALL_MONITOR` | 5 | `model.system.InstallMonitorModel` |
| 3 | `DEV_MODE` | 2 | `model.system.DevModeModel` |
| 4 | `USB` | 3 | `model.system.UsbModel` |
| 5 | `MMS` | 5 | `model.system.MmsModel` |
| 6 | `CHAIN_START` | 0 | `model.system.ChainStartModel` |
| 7 | `GMS_OFF` | 0 | `model.system.GmsModel` |

> 【观测】另有 6 个 model 类存在于源码但**未出现在 scanitem.json**：`AppLockModel`(在)、`AutoUpdateModel`(在)、`DefaultAppModel`、`FlowRankModel`(在)、`MiAccountModel`、`MiRoamingModel`、`PaymentRiskModel`(在)… 具体未出现者：`model/manualitem/DefaultAppModel.java`、`MiAccountModel.java`、`MiRoamingModel.java`、`FirstAidModel`(在)。完整索引常量见 `model/ModelConstants.java`（0–66，含未启用的 `MODEL_LIVE_TALK=25`、`MODEL_REALNAME_VERIFY=37` 等）。【观测/未知】未在 JSON 中者可视为**当前版本未启用**，风险权重以 JSON 为准。

### 2.3 扫描编排：5 条并发流水线【观测】

`scanner/k.java`（类名混淆为 `k`，日志 TAG 是 `SecurityManager`）：

```java
// scanner/k.java:940-956
public void w(InterfaceC0264k cb, rf.i iVar, rf.c cVar, m mVar) {   // startScanAndOptimize
    this.f20833a = false;
    this.f20842j.a();                                     // 优化队列重置
    this.f20847o.clear(); this.f20845m.a();
    this.f20847o.addAll(Arrays.asList(zf.d.values()));    // 优化项枚举
    this.f20839g.E();                                     // ScoreManager.clear
    A(false, iVar);   // 1) 手动项扫描     → scanner/c.e(...)
    x(false, mVar);   // 2) 自动项扫描     → scanner/m.o(...)
    C(false, cVar, mVar, cb);  // 3) 系统应用扫描（或增量扫描）
    B(false, cVar, mVar);      // 4) 内存扫描    → scanner/d.n(...)
    y(mVar);          // 5) 缓存扫描    → CacheCheckManager.c(...)
}
// scanner/k.java:829-838 —— 第 3 步分支
private void C(...) {
    if (o.f20935o.a().E()) {                 // E()=读 key_is_guard_support
        bVar.a().O(new o(..., "incremental_scan_bg"));   // GuardProvider 增量扫描
    } else {
        this.f20835c.l(z10, new o(..., "pre_scan"), "HOMEPAGE_SCAN", z10 ? 6 : 7);
    }
}
```

各子扫描器的角色【观测，按类声明与调用点】：

| 文件 | 角色 | 触发方法 |
|---|---|---|
| `scanner/c.java`(3 KB) | 手动项（manual group）扫描器 | `e(回调)`；`k` 里 `A()` 调它 |
| `scanner/m.java`(20.8 KB) | 自动项 + 系统应用扫描 + 优化 | `o(...)`、`l(...)`、`k(...)`、`n(...)` |
| `scanner/d.java`(15.6 KB) | 内存扫描/优化 | `n(回调)`、`m(list,回调)` |
| `scanner/CacheCheckManager.java`(5 KB) | 缓存过期检查 | `c(回调, 回调)` |
| `scanner/o.java`(28.5 KB，Kotlin) | **VirusScanManager**（病毒扫描协调器，见 §3） | `r()/s()/N()/O()` |
| `scanner/n.java`(15 KB) | 病毒结果通知（`n.f20925f`） | `t(context, list)`、`f(context)` |
| `scanner/IncrementalScanObserver.java` | 增量扫描观察者 | `R0/r8/s8/t8` |

防重入/取消：`k#m()` 置 `f20833a=true` 并 `interrupt()` 两个线程，同时 `o.q()` 取消增量扫描（`k.java:882-901`）。

### 2.4 打分算法（`ScoreManager#o` / `#q` / `#i`）【观测】

**扣分制**（`ScoreManager.java:167-220`）：

```
deduct =  Σ(score of system models with State==DANGER)          // 系统项
        + n( g() )                                              // 内存扣分
        + f()                                                   // 缓存陈旧扣分
        +  Σ(score of manual models with State==DANGER)         // 手动项
deduct = clamp(deduct, 0, 100)
if (!z() && deduct < 41) deduct = 41;    // ★ 只要"还有任何风险"就至少扣 41 分
```

- `n(memoryBytes)`（`ScoreManager.java:141-165`）：`ratio = 已用/总量*100`，`≥50→15`、`≥40→10`、`≥30→8`、`≥20→5`；且距上次缓存检查 <300 s 直接返回 0。
- `f()`（`ScoreManager.java:112-132`）：距上次扫描 `>3 天→15`、`>DEFAULT_UPDATE_FREQUENCY→10`、`>24 h→8`、`>12 h→5`、`>6 h→3`、否则 0。
- `z()`（`ScoreManager.java:565-567`）= `w()<=0 && f20792c.isEmpty()`，即"无风险项且无病毒列表"。
- **最终得分 `q()` = `100 - deduct`**（`ScoreManager.java:501-509`）；`i()` 是另一条路径（`100 - x.m(context)`），用于增量刷新（`ScoreManager.java:445-456`）。
- 扣分同时写回偏好：`x.c0(Application.A(), deduct)`（`ScoreManager.java:215`）。

**展示**【观测】：
- 真正的分数控件是 `MainFragment.S0`（类型 `wf.a` 接口，`MainFragment.java:154`），由 `MainContentFrame` / `MainContentFrameFold` / `MainContentFrameLite` / `MainContentFramePad` / `ScanContentFramePad` 五个实现按机型渲染：
  ```java
  // MainFragment.java:1566-1567
  mainFragment.S0.setScoreText(this.f20575g.intValue());
  mainFragment.S0.setResultScoreText(this.f20575g.intValue());
  // MainContentFrame.java:587-589
  public void setScoreText(int i10) { this.f21107b.setScore(i10); }
  // MainContentFrame.java:576-578
  public void setResultScoreText(int i10) { this.f21109d.setScore(i10); }
  ```
- 另有 `MainFragment.f20481n`（`NativeInterstitialAdLayout`，`MainFragment.java:239`）**也用分数**：`f20481n.a(ScoreManager.j().q())`（`:1695`）→ 设置广告卡片内的分数，并按 `score >= this.f21208d` 在 `shape_interstitial_ad_blue` / `shape_interstitial_ad_yellow` 之间切换背景（`NativeInterstitialAdLayout.java:35-46`）。
- 打点：`qf.c.T(ScoreManager.j().q())`（`MainFragment.java:2248`）。

`ScoreManager` 还负责病毒列表聚合：`c(com.miui.antivirus.model.i)` / `u()` / `R()` 把 `RISK_APP` 与普通风险分开计数并写 `o2.h.I/J`（`ScoreManager.java:344-369`）。

### 2.5 单项实现示例

| 项 | 实现 | 代码 |
|---|---|---|
| `VIRUS`（病毒项） | 直接读 ScoreManager 是否"干净"；优化时跳转 `miui.intent.action.ANTI_VIRUS` 并带 `key_auto_to_result_page=true` + `key_risk_list` | `model/system/VirusScanModel.java:49-63`【观测】 |
| `VIRUS_CLOUDSCAN` | 只判断偏好开关 `preference_key_open_virus_cloud_scan` 是否开；未开则 DANGER，`optimize()` 直接写 true | `model/manualitem/VirusCloudScanModel.java:56-69`【观测】 |
| `PAYMENT_RISK` | `o2.h.g() == w2.r.SAFE`；结果同步进 `zf.f` 的 SECURITY 卡片池 | `model/manualitem/PaymentRiskModel.java:51-62`【观测】 |

---

## 3. `com.miui.antivirus` —— 病毒扫描

（82 个 .java，660.7 KB）

### 3.1 功能清单

| 功能 | 入口 | 核心类·方法 | 数据 | 云端/AIDL | 证据 |
|---|---|---|---|---|---|
| 病毒扫描主界面 | `activity.MainActivity`（96.5 KB，Manifest:252 段，action `miui.intent.action.ANTI_VIRUS`） | `MainActivity` + `result/n.java`(44.8 KB) + `ui/MainContentFrame` | — | AIDL | 【观测】 |
| 病毒结果详情 | `activity.VirusDetailActivity` | — | `key_risk_list` | — | Manifest |
| 病毒设置 | `activity.SettingsActivity`（45.2 KB，action `com.miui.securitycenter.action.ANTIVIRUS_SETTINGS`） | `SettingsFragment` | 见 3.4 | — | 【观测】 |
| 白名单 | `whitelist.WhiteListActivity`（22.2 KB，action `miui.intent.action.ANTI_VIRUS_WHITELIST`） | `whitelist/d.java`(11 KB) | `r4.a` `key_safepay_sign_exception` 等 | — | Manifest【观测】 |
| **常驻安全服务** | Manifest:835 `GuardService`（32.1 KB，remote 进程） | 见 3.3 | — | AIDL + ProcessManager | 【观测】 |
| 病毒库每日更新 | Manifest:870 `VirusAutoUpdateJobService`（8.4 KB） | `onStartJob` | Job id **99**，周期 86400000 ms | AIDL + URL | `VirusAutoUpdateJobService.java:197-229`【观测】 |
| 支付安全检测（SafePay） | `activity.MonitoredAppSettingsActivity`（20.2 KB，action `miui.intent.action.SAFE_PAY_MONITOR_SETTINGS`） | `GuardService` + `w2.t` | `assets/safepay_*` | — | 【观测】 |
| 网址安全检测 | `activity.WebsiteSecurityCheckActivity` | `h2.f`(URLFilterManager) | `assets/white_url.json` | `com.miui.guardprovider.action.urlscan` | 【观测】 |
| 签名异常/安装拦截 | `activity.SignExceptionActivity`、`receiver.UpdaterReceiver` | — | — | — | Manifest |
| 危险提示对话框 | `activity.DangerousAlertActivity`、`VirusMonitorDialogActivity`、`service.DialogService`（`com.miui.safepay.SHOW_WARNING_DIALOG`） | — | — | — | Manifest |

### 3.2 扫描引擎：本 APK 只是客户端，引擎在 `com.miui.guardprovider`【观测】

**关键发现 1：本 APK 内没有任何病毒特征库文件。**
`_analysis/apk_full/assets/` 全量清单里没有 `.db`/`.dat`/签名库类文件（唯一的 SQLite 是 `garbage_clean.db`，属清理模块）；`lib/` 下的 18 个 .so 中也没有 `libavl*`/`libscan*` 之类引擎库。→ **本地特征库格式在本 APK 中不存在**，属 GuardProvider APK。【观测】

**关键发现 2：病毒查杀本体不在本 APK 内。**

1. `p9.a`（`GpBinderManager`）绑定外部服务的固定 Action/Package：

```java
// p9/a.java:136-140
private void h() {
    Intent intent = new Intent("com.miui.guardprovider.action.antivirusservice");
    intent.setPackage("com.miui.guardprovider");
    this.f33092a.bindService(intent, this.f33096e, 1);
}
```

2. 接口由本 APK 内的 AIDL 桩 `com.miui.guardprovider.aidl.IAntiVirusServer` 描述（26.9 KB 生成代码），暴露方法（`IAntiVirusServer.java:561-607`【观测】）：

```
int  v0(String[] paths, IVirusObserver cb, boolean flag)     // 批量扫文件
int  y6(String[] paths, IVirusObserver cb, boolean flag, int mode)
int  q1(IVirusObserver cb)     int  D(IVirusObserver cb)      // 全盘/取消
boolean r7(String) / X3(String)                               // 单包/单路径查询
String n0(String) / P6(String)                                // 引擎/库版本
String[] O() / Q0()                                           // 引擎清单 / 支持能力
List<String> m6(List) / x0(List)                              // 批量查（白名单/结果）
int  C2/K/c5/O3(String url, IWifiDetectObserver, …)           // ★ Wi-Fi 安全检测
boolean K0()  int h1()  void W/L5/O1/j2(int|String,…)          // 状态与控制
```

3. `IAntiVirusServer` 副本还声明了 Wi-Fi 检测子接口 `IWifiDetectObserver`（`aidl/IWifiDetectObserver.java`），说明 GuardProvider 同时提供 Wi-Fi 风险检测。

4. 两个 Provider URI 也指向 GuardProvider【观测】：
   - `content://guard/support_incremental`（`scanner/o.java:653`）——是否支持增量扫描
   - `content://guard/network_set`（`o2/b.java:85-88`）——写入网络状态

5. **扫描对象**【观测】`VirusScanManager#y(Context)`（`scanner/o.java:710-769`）：

```java
for (PackageInfo packageInfo : i4.a.k(context).j()) {
    ApplicationInfo applicationInfo = packageInfo.applicationInfo;
    if ((applicationInfo.flags & 1) == 0) {       // 只扫非系统应用（FLAG_SYSTEM==0）
        … iVar.q(applicationInfo.sourceDir);      // 记录 APK 路径
        map.put(applicationInfo.sourceDir, iVar);
    }
}
// 追加：外部存储里的孤立 .apk
Cursor cursorQuery = contentResolver.query(MediaStore.Files.getContentUri("external"),
        new String[]{"_data", "date_modified"}, "_data LIKE '%.apk'", null, null);
```

→ **扫描对象 = 已安装第三方 APK 的 `sourceDir` + 外部存储中所有 `*.apk` 文件**。未见内存扫描/进程扫描代码路径。【观测】`o2.b.c` 两个枚举值也印证：`INSTALLED_APP` / `UNINSTALLED_APK`（`o2/b.java:59-62`）。

6. 结果模型【观测】`o2.b.d` 四态：`SAFE / RISK / VIRUS / RISK_APP`（`o2/b.java:64-69`）。

7. **未使用 `libsec-common-utils` 做查杀**【观测】：全 APK 只有 `com.miui.securitykey.ConfigManager` 加载它（`ConfigManager.java:42`），用途是取密钥，与查杀无关（见 §7）。

### 3.3 `GuardService`：支付安全 + 输入法 + Wi-Fi 的常驻守护【观测】

`GuardService.java` 结构：

| 机制 | 代码 |
|---|---|
| 注册**进程级 Activity 切换监听**（反射 `miui.process.ProcessManager`） | `bh.e.f(Class.forName("miui.process.ProcessManager"), "registerActivityChangeListener", new Class[]{List, List, Class.forName(IActivityChangeListener.DESCRIPTOR)}, o2.h.k(this), o2.h.j(this), this.f10637h)` — `GuardService.java:390` |
| 监听集合来自 SafePay 规则（应用包 + Activity） | `f10651v = o2.h.k(guardService)`（`key_safepay_monitored_apps_list`）、`f10652w = o2.h.j(...)`（`key_safepay_monitored_activity_list`）、`f10653x = t.p(guardService)`（`safepay_target_activity` 里 `type==0` 的包）、`f10654y = t.m(guardService)`（`safepay_calling_activity`）— `GuardService.java:385-388` |
| 进入/离开支付页 → 状态栏安全态 | `t.P(this, 0, "com.miui.app.ExtraStatusBarManager.action_status_safepay", false, new Bundle())` — `GuardService.java:452`、`617` |
| 支付警示弹窗 | `com.miui.safepay.SHOW_WARNING_DIALOG` / `HIDE_WARNING_DIALOG` — `GuardService.java:598`、`629` |
| **输入法防劫持**：检测到当前默认输入法是第三方时切回系统输入法 | `S()` — `GuardService.java:700-735`：`Settings.Secure.putString(cr,"default_input_method", 系统输入法.getId())`，并把原值存 `f10655z` 供 `O()` 还原 |
| 通过 AppOps 限制后台 | `setUserRestriction(24, z10, binder, new String[]{F, "com.android.pcmode", "com.miui.handwriting"})` — `GuardService.java:675` |
| Wi-Fi 风险检测 | 注册 `WIFI_STATE_CHANGE / CONFIGURED_NETWORKS_CHANGE / supplicant.*`，交给 `f10644o`（`o2.i`）与 `w2.t`；结果写 `key_safepay_wifi_scan_result` — `GuardService.java:638-648`、`o2/h.java:123` |
| 游戏联运判断（联合运营游戏在支付时绑游戏收银台） | `e extends AsyncTask` → `POST https://flash.sec.miui.com/game/union`，字段 `pkg`，响应 `isUnionGame`/`frequency` — `GuardService.java:330-352` |
| onStartCommand 命令 | `action_register_foreground_notification`(4) / `action_unregister_foreground_notification`(5) / `action_pay_safe_dialog_click_ignore` / `action_unshelf_warning_dialog_click_ignore`（带 `prePackageName`/`preClassName`/`curPackageName`/`curClassName`）/ `action_anti_fraud_auto_scan_apps` / `action_anti_fraud_auto_scan_single_app`（带 `curPackageName`）— `GuardService.java:775-797` |
| onCreate 注入 | `o2.g.J(this)`（监控器）、`i.c(this)`（Wi-Fi）、`p9.a.j(this)`（Guard 绑定）、`w2.b.b()`（防诈骗）— `GuardService.java:749-764` |

### 3.4 病毒库更新：两条独立通道【观测】

`VirusAutoUpdateJobService#onStartJob`（`VirusAutoUpdateJobService.java:209-229`）：

```java
d dVar = new d(jobParameters);
j2.a.e(this).d(new a(dVar));      // ① 更新 SMS 规则 resource.zip（见 §4.4）
if (t.G(this)) { /* FBE */ } else {
    b(dVar);                       // ② 走 IAntiVirusServer 更新病毒库（VirusObserver.A(UpdateInfo)）
    e();                           // ③ updateAvlURLRules → h2.f#f() → IURLScanServer.b0("AVL")
}
me.c.g().m(dVar);
this.f10674a.postDelayed(dVar, 12000L);   // 12 s 超时兜底
```

更新结果字段【观测】：`UpdateInfo{ updateResult(0/2/3), engineName }`，成功写 `preference_key_database_auto_update_time_%s`（`VirusAutoUpdateJobService.java:86-109`、`SettingsActivity` 的 `key_database_auto_update_*`）。

Job 注册（`VirusAutoUpdateJobService.java:197-201`）：`JobInfo.Builder(99, …).setPeriodic(86400000L).setPersisted(true).setRequiredNetworkType(2)`，即 **24 h、可持久化、仅非计量网络**；受 `persist.sys.miui_optimization` / `ro.miui.cts` 控制。

### 3.5 持久化（antivirus 相关 key，全部在 `remote_provider_preferences`）【观测】

| key | 含义 | 证据 |
|---|---|---|
| `key_is_guard_support` | GuardProvider 是否支持增量扫描 | `scanner/o.java:554-578` |
| `key_full_scan_time` | 上次全盘扫描时间 | `scanner/o.java:570,663,697` |
| `key_test_type` | AB 测试：新/旧扫描策略（0/1） | `scanner/o.java:661,691-694` |
| `key_database_auto_update_enabled_%s` / `key_database_auto_update_time_%s` | 引擎库自动更新开关/时间（`%s`=引擎名或 `all`） | `res/values/strings.xml` |
| `key_pay_safety_status` | 支付安全状态（`w2.r` 枚举 int） | `o2/h.java:53-55,157` |
| `key_safepay_monitored_apps_list` / `key_safepay_monitored_activity_list` | SafePay 监听集合 | `o2/h.java:69-75,168-179` |
| `key_safepay_sign_exception` | 签名异常白名单 | `o2/h.java:103,187` |
| `key_safepay_wifi_scan_result` | Wi-Fi 扫描结论字符串 | `o2/h.java:123,191` |
| `key_safepay_input_method_state` / `key_safepay_auto_scan_state` | 输入法/自动扫描开关 | `o2/h.java:87-99` |
| `key_anti_fraud_packages` | 反诈扫描过的应用集合 | `o2/h.java:127` |
| `key_latest_virus_scan_date` | 最近病毒扫描时间（同时写 `Settings.Secure`） | `o2/h.java:66-67` |
| `key_last_scan_risk_count` / `key_last_scan_virus_count` | 上次风险/病毒计数 | `o2/h.java:58-62` |
| `key_free_wifi_alert` / `key_first_enter_antivirus_v3` / `key_risk_application_warning_enable` | 提示开关 | `o2/h.java:41-45,183,199` |
| `preference_key_open_virus_cloud_scan` | **云查杀开关**（字符串资源） | `o2/h.java:23`、`VirusCloudScanModel.java:32` |
| `key_delte_gameunion_file_frequency` | 游戏联运文件清理频率 | `o2/h.java:141` |
| SharedPreferences `data_config` | 结果页/线框动画配置 | `antivirus/result/ScanResultFrame.java:223`、`result/c0.java:123` |

> **未知**：`preference_key_open_virus_cloud_scan` 打开后"云查杀"具体走哪个接口，在本 APK 代码中**没有找到**上传/查询 URL；`IAntiVirusServer` 也没有云查方法。**推断**：由 GuardProvider（另一个 APK）自行联网，本 APK 只负责开关与提示。

---

## 4. `com.miui.antispam` —— 骚扰拦截

（72 个 .java，618.7 KB）

### 4.1 判定链路的整体调用方向【观测 + 推断】

本 APK **不自己拦截电话/短信**；由系统（MIUI framework / Telephony）在来电/收短信时通过 ContentProvider 反查策略：

```
系统 Telephony
   ↓ ContentResolver.query(content://antispam/sms_judge)   ← 参数 3 个：[号码, 正文, simId]
   ↓ ContentResolver.query(content://antispam/call_judge)  ← 参数 5 个：[号码, callType, is_forward_call,
   ↓                                                              is_repeated_normal_call, is_repeated_blocked_call]
com.miui.antispam.db.AntiSpamProvider   (Manifest:1194, exported + permission CONTENTPROVIDER)
   ↓ f9555b = b2.b.c(context)            [PolicyManager 单例]
b2.b#b(cc) ← 来电        b2.b#e(cc) ← 短信
   ↓ 按 h2.e 枚举 ordinal 顺序逐条跑 policy（TreeMap 按 f26798a 排序）
com.miui.antispam.policy.*  (21 条；短信链最后一环再挂 URLFilterPolicy)
   ↓ 命中即 break，返回 blockType（int）
短信：ExtraTelephony.getRealBlockType(i10) → 若 ≥3：m2.f.a(ctx, number, realBlockType, keyword) 写拦截日志
```

证据：`AntiSpamProvider.java:62-91`（URI 表）、`:106-124`（`b()`=getCallBlockType / `c()`=getSMSBlockType → `this.f9555b.b/e(cVar)`）、`:828-884`（`case 9` / `case 10` 真实实现，`MatrixCursor` 列名 `smsBlockType` / `callBlockType`）、`b2/b.java:81-169`（PolicyManager 主循环 + `ExtraTelephony.getRealBlockType` + `f.a`）。

> **注意**（见 §4.6-C）：`url_judge`(13) / `service_num_judge`(14) / `call_transfer_intercept_judge`(21) 三个 URI 虽在 `UriMatcher` 注册，但 `query()` 走 `default` 分支直接返回 null —— **本版本未实现**。

### 4.2 21 条策略，按优先级顺序【观测】（`h2/e.java:113-159`）

| ord | 枚举 | 适用 | 实现类 |
|---|---|---|---|
| 0 | `EMPTY_NUMBER_POLICY` | CALL | `EmptyNumberPolicy` |
| 1 | `STRONG_CLOUD_WHITELIST_POLICY` | BOTH | `StrongCloudWhiteListPolicy` |
| 2 | `STRONG_CLOUD_BLACKLIST_POLICY` | BOTH | `StrongCloudBlackListPolicy` |
| 3 | `WHITELIST_POLICY` | BOTH | `WhiteListPolicy` |
| 4 | `BLACKLIST_POLICY` | BOTH | `BlackListPolicy` |
| 5 | `VAGUE_WHITELIST_POLICY` | BOTH | `VagueWhiteListPolicy` |
| 6 | `BLACK_PREFIX_POLICY` | BOTH | `BlackPrefixPolicy` |
| 7 | `BLACK_ADDRESS_POLICY` | BOTH | `BlackAddressPolicy`（含"号段/归属地"黑名单） |
| 8 | `SERVICE_SMS_POLICY` | SMS | `ServiceSmsPolicy`（106 服务号短信） |
| 9 | `KEYWORDS_WHITELIST_POLICY` | SMS | `KeywordsWhiteListPolicy` |
| 10 | `CLOUD_WHITELIST_POLICY` | BOTH | `CloudWhiteListPolicy` |
| 11 | `CALL_ALL_NUMBER_POICY` | CALL | `CallAllNumberPolicy` |
| 12 | `STRANGER_POLICY` | BOTH | `StrangerPolicy`（陌生人） |
| 13 | `OVERSEA_POLICY` | CALL | `OverseaPolicy` |
| 14 | `KEYWORDS_BLACKLIST_POLICY` | SMS | `KeywordsBlackListPolicy` |
| 15 | `CLOUD_BLACKLIST_POLICY` | BOTH | `CloudBlackListPolicy` |
| 16 | `CLOUD_WHITE_KEYWORDS_POLICY` | SMS | `CloudWhiteKeywordsPolicy` |
| 17 | `CLOUD_BLACK_KEYWORDS_POLICY` | SMS | `CloudBlackKeywordsPolicy` |
| 18 | `SMART_SMS_FILTER_POLICY` | SMS | `SmartSmsFilterPolicy` ← **接 TFLite 引擎** |
| 19 | `CALL_TRANSFER_POLICY` | CALL | `CallTransferPolicy`（呼叫转移拦截） |
| 20 | `REPORTED_NUMBER_POLICY` | CALL | `ReportedNumberPolicy`（被举报号码） |

策略基类 `com.miui.antispam.policy.a`（`policy/a.java:10-44`）持有 `mJudge`（`h2.b`）与 `mPolicyManager`；返回值 `a.C0122a{ boolean a; int b; String c; }`，`a=true` 表示"已处理/终止链"。

`b2.b#e()` 末尾额外一步（`b2/b.java:140-144`）：短信判定后再过 `com.miui.antispam.policy.b#d(blockType, cVar)`（**URLFilterPolicy**，类名不在枚举里，单独挂载）。

### 4.3 `assets/pattern` —— 规则如何被加载与匹配【观测】

**加载路径**：`assets/pattern`（GBK/UTF-8 混排 JSON）→ `lj.c.a(Context)` 读成字符串（优先 `files/pattern`，否则 assets，`lj/c.java:42-57`）→ `nj.d#d(String)` 解析。

```java
// nj/d.java:61-85
JSONObject jSONObject = new JSONObject(new JSONTokener(str));
String string = jSONObject.getString("version");          // "1.2"
JSONArray jSONArray = jSONObject.getJSONArray("features");
for (…) {
    c cVar = new c(this.f32120a);
    if (cVar.c(jSONArray.getJSONObject(i10))) {
        this.f32120a.put(cVar.a(), cVar);                  // name → feature
        this.f32121b.add(cVar);
    }
}
```

**单个 feature 的 schema**（`nj/c.java:51-84`）：

```json
{
  "name": "Fei_xin",
  "sms_type": 1,                 // 位掩码，最终 OR 起来
  "assist": true,                // true=辅助特征（不计入打分）
  "address": { "type":[1,2,3,4,5,6], "pattern":["^12520"], "neg_pattern":[…], "or_pattern":[…] },
  "body":    { "pattern":[ …全部命中… ], "neg_pattern":[ …任一命中即排除… ], "or_pattern":[ …任一命中… ], "body_len":["0-15"] },
  "combi":   [ "featA&featB", … ]    // 组合特征：任一行内全部命中即算命中
}
```

匹配算法【观测】：
- `nj.e#a(String)`（`nj/e.java:70-97`）：`pattern` 为 AND（全部 `find()` 命中），`neg_pattern` 为排除（任一命中即 false），`or_pattern` 为 OR。
- `nj.c#e(lj.b)`（`nj/c.java:90-123`）：address 与 body 都满足，再按 `combi` 递归求值。
- `nj.d#e(lj.b)`（`nj/d.java:87-95`）：**遍历全部非 assist 特征，把命中的 `sms_type` 做位或**得到 `featureBits`，写回 `bVar.n(featureBits)`（`lj/e.java:20-32`）。
- 位与权重换算【观测】`nj.d#b(int)`（bits 12..23 → 权重 1..12）与 `nj.d#a(int)`（bits 4..15 → 12..1），用于给模型输出做偏移修正：`lj/e#g(double[], b)` 按 `(iA-iB)*(p0-p1)/4` 平移 logits（`lj/e.java:49-64`）。

### 4.4 新一代短信引擎（TFLite，`asset/resource.zip` 下发）【观测】

`h2.a`（`SmsEngineHandler`，339 行）：

| 环节 | 代码 |
|---|---|
| 解压规则包 | `j(context, dir)`：`ZipInputStream(context.openFileInput("resource.zip"))` → `files/SmsEngineResource/`，带路径穿越防护（`getCanonicalPath().startsWith(str)`）— `h2/a.java:98-181` |
| 结构校验 | `l(context)`：逐个 entry 比对文件大小，不一致即重解 — `h2/a.java:184-224` |
| 建模 | `da.a.b(dir)` → `com.miui.sms...` 三件套（`da/a.java:31-57`） |
| 判短信 | `h(sender, body)`：`mProxy.c(sender, body)` → 是否重要；`d(body)` → 是否拦截。返回 0=不拦/重要、2=拦截 — `h2/a.java:234-331` |
| 触发更新 | `j2.a`（`SmsEngineUpdateManager`）：`POST https://api.sec.miui.com/GuardProviderV1/GetResourceZipFile`，body `{app_version, ver:"2.1.2"}`，响应 `{code:200, data.fileUrl}`；下载后 **MD5 必须等于 `6206c9f17a64246498b52e117d0cff1e`**，落盘 `files/resource.zip`，然后 `ContentResolver.call(content://antispam, "initSmsEngine")` — `j2/a.java:261-276, 166-198` |

TFLite 组件（`da` / `aa` / `ca` / `ba` / `z9` 包）：

| 类 | 作用 | 常量 |
|---|---|---|
| `ca.a` | 词表 → 128 维 embedding：文件 `v.txt`（`token\tfloat`），`f8020b=128` | `ca/a.java:19-46` |
| `aa.c` | **UnlawRecognizer**：`1.bin`，阈值 `0.4` | `aa/c.java:11-29` |
| `aa.a` | **ImportanRecognizer**：`2.bin`，阈值 `0.6` | `aa/a.java:11-29` |
| `aa.b` / `ba.b` / `ba.a` | 按发信人（key="0" 为通配）的规则表 | `aa/b.java:15-46` |
| `z9.a` | 预筛正则：含中文、纯数字、纯 ASCII 无空格 → 直接跳过 | `z9/a.java:9-15` |
| `lj.g` + `mj.a` | **旧引擎的词库/分词**：`assets/dict` → `mj.a.h(InputStream)`，CRF 式 4 状态解码（`mj/a.java:56-80`） | — |

> 【观测】`assets/model`(680 KB) 与 `assets/rec_model_20230425.mnn` 在本模块代码里**没有**被引用；`lj.g` 只用 `dict`，`aa.*` 只用 `resource.zip` 内的 `1.bin/2.bin/v.txt`。
> 【未知】`assets/model` / `.mnn` 的实际消费方（**推断**：清理/推荐/`com.miui.mlkit` 路径，非本模块）。

### 4.5 号码库 / 黄页 / 白名单

| 数据源 | 读取方式 | 证据 |
|---|---|---|
| **系统电话本/黑名单**（真正的存储位置） | `ContentResolver.query(ExtraTelephony.Phonelist.CONTENT_URI, {type,state,number,sim_id}, "sync_dirty<>1")` | `h2/b.java:140-145`【观测】 |
| **系统关键字库** | `ContentResolver.query(ExtraTelephony.Keyword.CONTENT_URI, {type,data,sim_id})` | `h2/b.java:84-90`【观测】 |
| 短信白名单（黄页） | `YellowPageContract.AntispamWhiteList.CONTNET_URI`，`number LIKE 'xxx%'`，`106` 开头直接白 | `SmartSmsFilterPolicy.java:27-51`【观测】 |
| 本地服务号表 | `assets/service_num.json`（优先 `files/service_num.json`，可被云覆盖 `MiuiSettingsCompat`） | `m2/e.java:55`【观测】 |
| URL 白名单 | `assets/white_url.json`（`{"content":[…]}`），可被云 `MiuiSettings antispam_white_url/content` 覆盖 | `m2/e.java`【观测】 |
| 云号码库 | `POST https://api.sec.miui.com/harassIntercept/intercepts`，body `{dataVersion}`；响应 `version` + `v[{status:new|updated|deleted, type:1|2, uuid, p, attributes}]`（type=1 号码，attributes 两位数字= 类别+级别；type=2 关键字，attributes 单数字） | `k2/a.java:51-130`【观测】 |

云端号码落库后写 `b2.b#h/i`（`f7520b.E/G`、`F`）同步到内存索引；全量重置时会 `delete from phone_list where cloudUid not null` / `keyword …`（`k2/a.java:45-49`）。

### 4.6 URL 拦截（`URLFilterPolicy`，`policy/b.java`）【观测】

**先说清楚两个易混的辅助函数（全树核对过语义）：**
- `m2.i.f(ctx, str)` → `m2.e.b(ctx, str)`（`m2/i.java:147`、`m2/e.java:47-70`）= **判断 `str` 是否在"服务号号码清单"**（云 `antispam_service_number/content`，否则 `assets/service_num.json` 的 `content` 数组）。**不是** URL 表。
- `m2.i.b(str)`（`m2/i.java:93-129`）= 用正则 `f29607a` 从正文里**抽出所有 URL**；`m2.i.a(str)` = 归一化域名；`m2.i.c(str)` = `\d{7,15}` 判断正文是否含 7–15 位连续数字（疑似账号/卡号）；`m2.i.d(ctx)` = 读 `white_url.json` 白名单集合。

**A. 短信主链里的 URL 判定（实际生效路径）** —— `policy/b.java#d(int blockType, h2.c sms)`

```java
// policy/b.java:220-267
public a d(int i10, c cVar) {
    if (Build.IS_INTERNATIONAL_BUILD) return null;
    ArrayList<String> urls = m2.i.b(cVar.f26766g);            // 正文里的 URL
    if (urls.isEmpty()) return null;
    if (!m2.i.f(this.f9563a, cVar.f26761b)) {                 // ★ 发信人不是服务号
        return new a(true, e(i10));                           // → 直接判风险：blockType |= 128
    }
    if (i10 >= 3) return null;                                // 已被判为更严重级别则不再叠加
    f fVar = new f(this.f9563a);                              // GuardProvider IURLScanServer
    for (String s : urls) {
        if (fVar.d(s) == 1) { c2.a.t("black"); return new a(true, 8); }   // AVL 判定黑 → blockType=8
    }
    if (m2.i.c(cVar.f26766g)) { c2.a.t("number"); return new a(true, e(i10)); }  // 含 7-15 位数字
    HashSet<String> whitelist = m2.i.d(this.f9563a);
    for (String s : norm(urls)) {
        if (!whitelist.contains(s)) { c2.a.t("non_white_black_single_url"/"…multi_url"); return new a(true, e(i10)); }
    }
    c2.a.t("white"); return null;                             // 全部命中白名单 → 放行
}
```
其中 `e(int i) = i | 128`（`policy/b.java:168-170`）。调用点：`b2/b.java:140-144`（短信链最后一环，`this.f7521c.d(i10, cVar)`）。

**B. `c(String oriUrl, String redirectUrl)`**（`policy/b.java:172-218`）—— 独立 URL 判定入口，签名第一个参数是**发信人号码**（用于服务号放行）：

```java
public int c(String str /*发送方号码*/, String str2 /*URL*/) {
    boolean zF = m2.i.f(this.f9563a, str);                    // 发送方是服务号?
    String strA = a(str2, new i("antispam_urlfilterpolicy")); // 跟随 30x 取最终 URL（UA antispam/1.0.0）
    int iD = new f(this.f9563a).d(str2);                      // ① GuardProvider AVL url 扫描
    if (iD != 1) iD = b(str2);                                // ② 云端检测（原始）
    if (iD != 1) iD = b(strA);                                // ③ 云端检测（跳转后）
    if (!zF) { return iD==1 ? 2 : (iD==2 ? 0 : -1); }
    if (iD == 1) return 2;
    HashSet<String> whitelist = m2.i.d(context);              // white_url
    if (whitelist.contains(norm(str2))) return 0;
    if (!e2.b.i()) return -1;                                 // 无联网权限 → 未知
    return whitelist.contains(norm(strA)) ? 0 : 1;
}
```
`b2.b#f(str,str2)` 是对它的包装（`b2/b.java:164-169`）；**【未知】在本 APK 内未找到 `b2.b#f` 的调用点** —— 推断由系统侧（MIUI framework 的浏览器/短信 URL 检测）通过其他通道触发，或本版本暂未接线。

**C. 三个"声明了但没实现"的 Provider URI**【观测】：
`AntiSpamProvider.query()` 的 `switch` 里 `case 13 (url_judge)` / `case 14 (service_num_judge)` / `case 21 (call_transfer_intercept_judge)` 全部落到 `default` → `Log.e(...,"Unknown query URI")` → `return null`（`AntiSpamProvider.java:904-906`）。真正实现的只有 `case 9 (sms_judge)` 与 `case 10 (call_judge)`。

云端 URL 检测接口（`policy/b.java:135-166`）：

```
POST https://url-detect.engine.mi.com/url/detect
form: url=<url>&ts=<sec>&c=2&sign=md5(排序拼接 + "2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9")
resp: {"code":200, "data":{"urlCategoryId": N}}
      N==-1 → 0(未知)   N==0 → 2(危险)   N==1|2 → 1(安全)
UA:   antispam/1.0.0     超时: connect/read 各 5 s
```

### 4.7 数据库结构【观测】

`e2.a`（`AntiSpamDB extends SQLiteOpenHelper`）：**db 名 `AntiSpam`，version 10**（`e2/a.java:117`）。建表 SQL 全部来自字符串资源（`res/values/strings.xml`，`e2/a.java:130-148`）：

| 表 | 建表语句（原文） |
|---|---|
| `fwlog` | `CREATE TABLE fwlog( _id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT DEFAULT NULL, date INTEGER NOT NULL DEFAULT 0, type INTEGER NOT NULL DEFAULT 0, reason INTEGER NOT NULL DEFAULT 0, data1 TEXT DEFAULT NULL, data2 TEXT DEFAULT NULL, read INTEGER NOT NULL DEFAULT 0, simid TEXT DEFAULT -1, mode INTEGER NOT NULL DEFAULT 0, callType INTEGER NOT NULL DEFAULT 0 );` |
| `phone_list` | `CREATE TABLE phone_list( _id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT DEFAULT NULL, display_number TEXT DEFAULT NULL, notes TEXT DEFAULT NULL, state INTEGER NOT NULL DEFAULT 0, isdisplay INTEGER NOT NULL DEFAULT 1, type INTEGER NOT NULL DEFAULT 1, cloudUid TEXT DEFAULT NULL, sync_dirty INTEGER DEFAULT 0, e_tag TEXT DEFAULT NULL, record_id TEXT DEFAULT NULL, location INTEGER DEFAULT 0, sim_id INTEGER NOT NULL DEFAULT 1 );` |
| `keyword` | `CREATE TABLE keyword( _id INTEGER PRIMARY KEY AUTOINCREMENT, data TEXT DEFAULT NULL, type INTEGER NOT NULL DEFAULT 1, cloudUid TEXT DEFAULT NULL, sim_id INTEGER NOT NULL DEFAULT 1 );` |
| `mode` | `CREATE TABLE mode( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT DEFAULT NULL, state TEXT DEFAULT NULL );` |
| `sim` | `CREATE TABLE sim( _id INTEGER PRIMARY KEY AUTOINCREMENT, sim_id TEXT NOT NULL, call_wait TEXT DEFAULT -1, name TEXT DEFAULT NULL, backsound_mode INTEGER DEFAULT 0 );` |
| `reportSms` | `CREATE TABLE reportSms( id TEXT NOT NULL );` |
| `reportSmsPending` | `CREATE TABLE reportSmsPending( id TEXT NOT NULL, address TEXT, body TEXT, date TEXT );` |

> 【观测】`phone_list`/`keyword`/`fwlog` 的**权威副本其实在系统 MIUI Provider**（`miui.provider.ExtraTelephony.*`），本 APK 的 `AntiSpam` 库主要承担 云同步/上报/双卡模式/回铃音 等场景；`onUpgrade` 里两次把 `ExtraTelephony.FirewallLog` 的通话记录 bulkInsert 进 `CallLog.Calls` 再清空（`e2/a.java:42-76`）。

### 4.8 备份 / 同步 protobuf【观测】

- Schema：`assets/protobuf/antispam.proto`（`package antispam; java_package = "com.miui.antispam.service.backup"; java_outer_classname = "AntiSpamProtos"; optimize_for = LITE_RUNTIME`）
  - `AntiSpam{ repeated BlacklistEntry blacklist=1; repeated WhitelistEntry whitelist=2; repeated KeywordEntry keyword=3; repeated PolicyEntry policy=4; repeated SwitchEntry switch=5; repeated NotificationEntry notification=6; AutoUpdateLibrary autoUpdateLibrary=7; SimsShareSettings simsShareSettings=8; ReportedNumberGuide reportedNumberGuide=9; }`
  - 每条 entry 统一带 `guid=1, luid=2`（同步标识），条目字段：`number/notes/state/simId`（黑）、`number/notes/state/isDisplay/simId`（白）、`keyword/simId/type`（关键字）、`name/state/simId`（策略）、`state/simId`（开关/通知）。
  - `ReportedNumberGuide{ bool FraudState=3; bool AgentState=4; bool SellState=5; }` ← 三类标记号码（诈骗/中介/推销）。
- `syncroot.proto`：`SyncRoot{ optional antispam.AntiSpam antispam = 8; }`
- 生成代码：`com.miui.antispam.service.backup.a..n`（Lite protobuf，`c extends GeneratedMessageLite` 即 `AntiSpam`，9 个 `List` + 3 个单例字段，`c/c.java:19-56`）。
- 使用点：`SecurityCenterBackupAgent extends miui.app.backup.FullBackupAgent`（`SecurityCenterBackupAgent.java:17`）：
  - `onFullBackup`：从 DB 读出 5 类列表（`this.f9576a.i()/f()/j()/g()/h()`）灌进 `c.D()` builder，`n.g().k(build).build().writeTo(fos)`（`:92-171`）
  - `onDataRestore`：`n.j(stream)` → 逐条 `this.f9576a.c/a/d/b/r(...)` 回写（`:30-89`）
  - `getVersion()` 固定返回 1。
- **推断**：`guid`/`luid` 是 MiCloud 同步的全局/本地 ID；同步通道本身不在本 APK 实现（`miui.cloud` framework 侧）。

### 4.9 antispam 偏好（几乎全部在 `Settings.Secure`，非 SP 文件）【观测】

`m2.a` 是 Settings.Secure 包装器（`bm.a.*` = `Settings.Secure`/`Settings.System`），`e2.b` 在其上做双卡分派：

| key | 类型 | 含义 |
|---|---|---|
| `fraud_num_state` / `fraud_num_state_sim_2` | int | 诈骗号码拦截（0=开） |
| `agent_num_state(_sim_2)` | int | 中介号码 |
| `sell_num_state(_sim_2)` | int | 推销号码 |
| `harass_num_state(_sim_2)` | int | 骚扰号码 |
| `show_notification_type(_sim_2)` | int | 通知样式 |
| `sms_classifier_auto_update` | bool | 短信分类器自动更新（`m2/a.java:96`） |
| `sms_classifier_update_time` | long | 上次更新时间 |
| `service_sms_mode` / `stranger_sms_mode` / `mms_mode` | int | 服务号/陌生人/彩信 拦截模式（默认 1/1/2） |
| `call_all_number_mode(_2)` | int | 全部号码拦截 |
| `backsound_index(_sim_2)` | int | 回铃音 |
| `is_call_transfer_blocked(_sim_2)` | bool | 呼叫转移拦截 |
| `is_repeated_marked_number_permit(_sim_2)` | bool | 重复标记允许 |
| `virtual_sim_slot_id` / `has_new_antispam` | int/bool | 虚拟卡槽/新消息 |
| `unread_mms_count` / `unread_phone_count` | int | 未读 |
| `clouds_data_version_new` | long | 云号码库本地版本（`e2/b.java:36-38,116-118`） |
| `reported_number_settings_reset` | bool | 举报设置已重置 |

---

## 5. `com.miui.antifraud` + `com.miui.riskcontrol`

### 5.1 antifraud（5 个类）

| 功能 | 入口 | 核心 | 数据 | 云端 | 证据 |
|---|---|---|---|---|---|
| 通话/短信诈骗预警 | `AntiFraudReceiver`（Manifest:433，exported，需 `MODIFY_PHONE_STATE`）→ 只处理 `android.intent.action.PHONE_STATE` 与 `miui.action.mms.SCAN_RISKY_URL` | `d.k().p(intent)` / `d.k().q()` | `d` 类状态机 | — | `AntiFraudReceiver.java:20-40`【观测】 |
| 转账/汇款页监控 | `PayActivityMonitorService`（Manifest:432，remote） | 见下 | `assets/anti_fraud_pay_activity` | — | `PayActivityMonitorService.java`【观测】 |

`AntiFraudReceiver` 的前置门（`AntiFraudReceiver.java:21-31`）【观测】：
```java
if (intent == null || Build.IS_INTERNATIONAL_BUILD || !tm.isVoiceCapable()) return;
if (t.h() < 13) { q0.g(TAG,"return,because MIUI Version below 13"); return; }
if (!x.h()) { Log.i(TAG,"sms call pay warn not open"); return; }   // 开关：短信/通话支付预警
```

`PayActivityMonitorService`【观测】（`PayActivityMonitorService.java:85-127`）：
```java
InputStream is = new File(filesDir + "/anti_fraud_pay_activity").exists()
        ? openFileInput("anti_fraud_pay_activity") : getAssets().open("anti_fraud_pay_activity");
JSONArray arr = new JSONArray(cl.e.i(is));   // [{package, activity:[...]}]
bh.e.f("PayActivityMonitorService", Class.forName("miui.process.ProcessManager"),
       "registerActivityChangeListener",
       new Class[]{List.class, List.class, Class.forName(IActivityChangeListener.DESCRIPTOR)},
       v.Q(pkgSet), v.Q(activitySet), this.f9521e);
```
进入命中 Activity（延迟 500 ms）→ `d.k().r(true)` + `d.k().o()`；离开同包 Activity → `d.k().r(false)` + `d.k().j()`（`PayActivityMonitorService.java:57-76`）。

`assets/anti_fraud_pay_activity` 内容【观测】：微信 `com.tencent.mm.plugin.remittance.ui.RemittanceUI`、支付宝 4 个 `com.alipay.mobile.transferapp.ui.*`、QQ `…qwallet.transaction.impl.TransactionActivity` —— **正是"转账/汇款确认页"**。

> 【未知】`d`（13.9 KB）的具体判定规则、是否上报服务端、上报字段 —— 需单独细读；本文件只覆盖到入口与页面监控。**未知**：`x.h()` 对应的偏好 key 与"上报内容"。

### 5.2 riskcontrol（1 个类，79 行）

**`SensitiveWordDetectProvider`** —— 唯一组件，Manifest:1733（`authorities="com.miui.riskcontrol"`，**exported=true**，`permission=com.miui.security.permission.DETECT_TEXT`）【观测】：

```java
// SensitiveWordDetectProvider.java:19-46
static {
    Uri.withAppendedPath(Uri.parse("content://com.miui.riskcontrol"), "censorResult");
    f20241a.add("com.android.camera");
    f20241a.add("com.miui.mediaeditor");      // ★ 硬编码白名单：只服务相机与媒体编辑
    System.loadLibrary("lib-riskcontrol");    // → /lib/arm64-v8a/liblib-riskcontrol.so
}
private native boolean hasSensitiveWords(String str);
private native void setAssetManager(AssetManager assetManager);

public final Bundle call(String str, String str2, Bundle bundle) {
    if (!f20241a.contains(getCallingPackage())) { Log.i("SWDP", getCallingPackage()+"not in whiteList!"); return null; }
    if (!"METHOD_SENSITIVE_WORD_DETECT".equals(str)) return null;
    setAssetManager(getContext().getAssets());          // ★ 把 assets 交给 native
    Bundle b = new Bundle();
    b.putBoolean("hasSensitiveWords", hasSensitiveWords(str2));
    return b;
}
```

- 【观测】**协议**：调用方 `ContentResolver.call(Uri.parse("content://com.miui.riskcontrol"), "METHOD_SENSITIVE_WORD_DETECT", 待检测文本, null)`，返回 `Bundle.getBoolean("hasSensitiveWords")`。
- 【观测】**`assets/dict` / `censor_source` 是否在此被使用**：`setAssetManager(getAssets())` 把整个 assets 交给 native，本 APK 的 Java 层**没有任何** `open("dict")`/`open("censor_source")` 调用（全树 grep 仅 `lj/g.java:32` 读 `dict`，属短信分词）。因此**`censor_source`（2688 B，高熵不可读）与 `dict` 是否被 `liblib-riskcontrol.so` 读取属于 native 侧行为 —— 【未知】，需 `open_binary` 分析 `lib/arm64-v8a/liblib-riskcontrol.so` 的字符串/PLT 才能定论。**
- 【观测】Provider 里那个 `Uri.withAppendedPath(...)` 的返回值被丢弃、`onCreate` 里的 `UriMatcher` 也只是 `new` 出来即弃 —— **纯占位代码**，实际入口只有 `call()`。
- 【推断】用途：相机/媒体编辑在**离线**场景做敏感词/文本审核（如拍照/截图 OCR 后的文本），不联网、不上报。**未知**：是否会在 `hasSensitiveWords` 命中后上报。

---

## 6. `com.miui.luckymoney` —— 红包助手

（54 个 .java，231.8 KB）

### 6.1 功能清单

| 功能 | 入口 | 核心 | 数据 | 云端 | 证据 |
|---|---|---|---|---|---|
| 红包助手设置页 | `ui/activity/LuckySettingActivity`（17 KB，Manifest:1095，action `miui.intent.action.HB_MAIN_ACTIVITY`，`singleTask`） | — | `CommonConfig` | `webapi/WebApiAccessHelper` | Manifest【观测】 |
| 二级设置 | `SecondarySettingActivity`（18.5 KB，action `miui.intent.action.LUCKY_MORE_SETTING`） | — | — | — | Manifest【观测】 |
| 快开白名单管理 | `FastOpenListActivity`（18.8 KB） | `config/FastOpenConfig` | — | — | 【观测】 |
| 提醒方式设置 | `RemoveLuckyMoneyActivity`（action `miui.intent.action.LUCKY_ALARM_SETTING`） | — | — | — | Manifest【观测】 |
| 引导页 | `GuideActivity` | — | — | — | 【观测】 |
| **红包监听服务** | Manifest:1126 `service/LuckyMoneyMonitorService`（20.4 KB，remote 进程） | 见 6.2 | — | — | 【观测】 |
| 跨进程查询 | Manifest:1127 `provider/LuckyMoneyProvider`（`miui.permission.EXTRA_NETWORK`） | — | — | — | Manifest【观测】 |
| 云控 | `service/CloudControlReceiver`（3 KB） | — | — | — | 【观测】 |
| 静音/免打扰联动 | `config/DoNotDisturbConstants`、`utils/SettingsUtil#isQuietModeEnable` | — | — | — | 【观测】 |

### 6.2 实现方式：**不是无障碍服务，是"通知监听"（借 GameBooster 的通知代理）**【观测】

```java
// LuckyMoneyMonitorService.java:312（onCreate）
bindService(new Intent(this, (Class<?>) NotificationListener.class), this.mNotificationListenerConn, 1);
// :73-82
public void onServiceConnected(ComponentName cn, IBinder b) {
    mNoticationListenerBinder = ISecurityCenterNotificationListener.Stub.asInterface(b);
    mNoticationListenerBinder.Z2(mNotificationListenerCallback);   // 注册回调
}
```

- 监听代理类在 `com.miui.gamebooster.service.NotificationListener` / `ISecurityCenterNotificationListener`（**同 APK 内**的 GameBooster 模块，不是系统 `NotificationListenerService`）。
- 回调 `onNotificationPostedCallBack(StatusBarNotification)`（`:58-72`）：过滤 `userId`（当前用户或 999/0）后交给 `onNotificationPosted`。
- 消息分发（`:332-403`）：
  - `AppConstants.Package.PACKAGE_NAME_MM`（微信）→ `new WechatMessage(...)` → `isHongbao()` → 走 `mWeixinPipeline`
  - `PACKAGE_NAME_QQ` → `new QQMessage(...)` → `QQGroupCollector.collect(...)` → `isHongbao()` → 走 `mQQPipeline`；`type == -1` 时走 `processTypeUnknownNotification`
  - 通话中不提醒：`PhoneStateMonitor.isPhoneBusy(ctx)`（`:358`、`:393`）。

### 6.3 "自动抢" 实现（`Pipeline#process`）【观测】

```java
// Pipeline.java:203-251
if (!appMessage.isHongbao()) return false;
if (notifyType == null || notifyType == NONE) { /* 不提醒 */ }
else if (!justForGroupMessage() || appMessage.isGroupMessage()) {
    if (!SettingsUtil.isQuietModeEnable(ctx)) ScreenUtil.powerOnScreen(ctx);      // ★ 点亮屏幕
    if (z10 || !checkFastOpenMode()) {
        MessageView v = obtianMessageView(appMessage);  v.show(appMessage);       // 悬浮窗/锁屏卡片
        if (!z10 && ScreenUtil.isScreenLocked(ctx)) { keyguardPipelineHistory…; lastLockScreenMessage = appMessage; }
    } else {
        new PendingIntentRunnable(appMessage.getAction()).run();                 // ★★ 自动触发红包 PendingIntent
        commonConfig.setWarningLuckyMoneyCount(+1);
        MiStatUtil.recordLuckyMoneyFastOpen(...);
    }
    if (!quiet && needPlaySource()) a.c(ctx);                                     // 播放提示音
}
```

`Pop` 的关键前置 `checkFastOpenMode()`（`Pipeline.java:52-64`）：
```java
if (ScreenUtil.isScreenLocked(ctx) || !CommonConfig.getInstance(ctx).isFastOpenEnable()) return false;
String fg = PackageUtil.getForegroundApp(ctx);
if (shellAppMap.containsKey(fg)) fg = shellAppMap.get(fg);   // com.android.thememanager→themestore, securitycenter→securitymanager
return !FastOpenConfig.getInstance(ctx).isRestrict(fg);      // 前景应用不在"快开黑名单"
```

`PendingIntentRunnable`（`ui/view/PendingIntentRunnable.java:13-23`）就是一句 `pendingIntent.send()`。

→ **结论【观测】**：所谓"自动抢红包"= 在 **屏幕未锁 + 用户开了"快开" + 当前前台应用不在限制名单** 时，**直接触发微信/QQ 通知自带的 `contentIntent`**，等价于用户点了一下通知；不是模拟点击、不操作 UI 层级、不依赖无障碍。锁屏状态下改成"锁屏卡片 + 解锁后重放"（`keyguardPipelineHistory` + `ScreenUtil.KeyguardUnlockedListener`，`Pipeline.java:126-149`）。

其他机制【观测】：`PhoneStateListener` 通话中去重（`:143-158`）、`BroadcastReceiver` 监听 `ACTION_CONFIG_CHANGED` / `ACTION_SCREEN_OFF` / 包安装（`:89-137`）、`HeadsUpView`/`LockScreenView`（`ui/view/*`）。

> 【未知】`com.miui.luckymoney.provider.LuckyMoneyProvider` 暴露的 `call()`/`query()` 具体协议；`webapi/WebApiAccessHelper` 的接口地址（未在 `dex_urls.txt` 中命中明显红包含义条目）。**推断**：云控只下发"开关/文案/限制名单"。

---

## 7. `com.miui.policeassist` + `com.miui.warningcenter.policeassist` —— 报警辅助

（policeassist 20 个类 61.9 KB；实际主体在 `warningcenter/policeassist`）

### 7.1 功能清单

| 功能 | 入口 | 核心类·方法 | 数据 | 云端 | 证据 |
|---|---|---|---|---|---|
| 设置页 | `warningcenter.policeassist.PaSettingActivity`（20 KB，Manifest:520，exported，action `miui.intent.action.WARNINGCENTER_POLICE_ASSIST`） | — | `Settings.Secure` | 协议/隐私 URL | Manifest【观测】 |
| 引导页 | `policeassist.ui.PoliceassistGuideActivity` / `PoliceassistGuideLawFragment` | — | — | — | 【观测】 |
| **紧急呼叫时上报位置** | Manifest:475 `warningcenter.policeassist.PaService`（23 KB，remote） | 见 7.2 | 定位 + 反地理编码地址 | 3 条通道 | 【观测】 |
| 浮窗提示 | `PaFloatWindowManager` / `PaLocationFloatingView` / `PaPrivacyFloatingView` | 挂在 InCallUI 上 | — | — | 【观测】 |
| EPS（国家应急平台）上报 | `com.miui.policeassist.EPSManager` | `postEPS(...)` | `EPSRequestBean` | `https://eps.caict.ac.cn/eps/collect` | `EPSManager.java:97-143`【观测】 |
| 短信通道 | `PaCommon.SMS_ADDRESS = "106903590009"` | — | — | SMS | `PaCommon.java:23-24`【观测】 |

### 7.2 流程【观测】

1. **触发**：`PaService` 注册 `PhoneStateListener`（`PaService.java:75-98`）；`onCallStateChanged` 用 `TelephonyManager.getCallState()` 判状态。挂断（`callState==0`）时若不满足条件则弹引导通知，并按 `mTransType` 收尾（`NETWORK`→`sendLastRequest()+stopServiceTimer()`，否则 `stopSelf()`）。
2. **号码**：`GetMyPhoneNumberTask` → `PaUtils.getMyPhoneNumber(service)`；有本机号 → `TransType.NETWORK` 并 `startRequestLoop()`；**无本机号 → `TransType.SMS`**（`PaService.java:131-152`）。
3. **定位**：`startLocation(mCallNumber)`（`:468`）；成功后 `onLocationSuccess(Location)`；失败 `onLocationFailed(String)`。
4. **地址**：`TransDataTask#doInBackground` 用 `Geocoder.getFromLocation(lat,lng,1).getAddressLine(0)`（`PaService.java:182-187`）—— **反地理编码把坐标变成文字地址**。
5. **通道 A：网络**（`PaCommon.java:17-18`）
   - `NET_ADDRESS = https://gateway-papa.cloud.weizhi.com/papa/upload`
   - `NET_XIAOMI_CHANNEL = "3016"`，`NET_XIAOMI_CHANNEL_NAME = "xiaomi"`
   - 加解密素材：`AES_ENCRYPT_KEY = "HPLvLMJnR6vvuS9VNkpj7DYb"`、`RSA_ENCRYPT_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCsPsxtHUmeUiBfvdGqBIJQUloI52iMczf/5glfBgjo3NCV5tMq4bV4mUV4g6/tEkbh+0N2xcavZWnDVNpCDrYFQcVupICPwSdS3wZyoWogt9RdoLs7lnQ4l7OPWNjweMqDE2b8gacmA+aebDgSb3wqT8iNIh+yjvgwpq9s88u+cQIDAQAB"（`AesCbcUtils.java` 实现）
6. **通道 B：EPS 国家平台**（`EPSManager.java`）
   - `EPS_NET_ADDRESS = "https://eps.caict.ac.cn/eps/collect"`，`EPS_PRIVATE_KEY = "97782f2e2e75445180d664fb5705a8a5"`，`EPS_XIAOMI_CHANNEL = "17"`
   - 请求体（`EPSRequestBean` 全字段，`EPSRequestBean.java:5-19`）：`source/caller/called/device/method/type/lng/lat/hAccuracy/vAccuracy/altitude/address/time/extra/sign`
   - 签名算法（`EPSManager.java:35-47`）：对 `TreeMap` 顺序拼成 `k=v&…` 后追加 `key=EPS_PRIVATE_KEY`，整串做 **SM3** 并转 hex（`sm3/SM3Digest.java` + `Sm3Utils`）；`caller` 也是 SM3 hex（`:52`）
   - `called` 编码：`"0"+method+lng+lat` 形式拼接（`:53-57`，`LOCATION_EXCEPTION="0"`/`LOCATION_GPS="1"`/`LOCATION_NET="2"`）
   - 重试 3 次、间隔 1000 ms（`:103-120`）
7. **开关**：`Settings.Secure` `com_miui_warningcenter_eps_status`（EPS）与 `com_miui_warningcenter_pa_status`（整体）；`getEPSPoliceAssistToggle` 要求 `==1`（`EPSManager.java:82-84`、`PaCommon.java:10-11`）。
8. **浮窗**：`miui.process.IForegroundInfoListener` 监听前台包名，仅当前台是 InCallUI（`isInCallUI(pkg)`）时保留浮窗（`PaService.java:99-112`）。

### 7.3 隐私影响（基于代码事实，不做价值判断）

| 事实 | 影响 | 证据 |
|---|---|---|
| 通话结束/紧急呼叫时自动取精确经纬度 | 位置精度取决于 `Location.getAccuracy()`/`getVerticalAccuracyMeters()`，且**上报到第三方域名** | `PaService.java:182`、`EPSManager.java:59-70`【观测】 |
| 反地理编码得到"文字地址"并上报 | 比裸坐标更直接的住址推断 | `PaService.java:182-187`【观测】 |
| 上报目标是 `weizhi.com`（"位置"拼音、非小米域名）+ `caict.ac.cn`（中国信通院） | 数据出境/第三方共享面 | `PaCommon.java:6,17`【观测】 |
| 硬编码 AES 口令与 RSA 公钥在 assets 同级代码里 | 传输加密强度受制于硬编码密钥 | `PaCommon.java:5,22`【观测】 |
| 无本机号时降级为**明文短信**发送到 `106903590009` | 地址经运营商短信通道传输 | `PaService.java:141-152`、`PaCommon.java:23`【观测】 |
| 需 `IForegroundInfoListener`（进程级 Activity 可见性）与浮窗权限 | 具备常驻感知前台应用的能力 | `PaService.java:99-112`【观测】 |
| 默认开关是"关"（`Settings.Secure.getInt(..., 0)`，把 `-1` 当关） | 需用户显式开启 | `EPSManager.java:82-84,122-124`【观测】 |

---

## 8. `com.miui.tvm` —— TrustZone/TEE 虚拟机；`com.miui.securitykey` —— 本地密钥

### 8.1 tvm（13 个类，68.1 KB）

| 功能 | 入口 | 核心 | 数据 | 证据 |
|---|---|---|---|---|
| TVM 管理服务（AIDL，`miui.permission.TVM`） | Manifest:476 `com.miui.tvm.service.TvmManagerService`（**`enabled=false`**，由 `Application#w()` 动态启用） | `ITvmManagerImpl`(16.8 KB) / `ITvmManager`/`ITvmManagerCallback` | — | 【观测】 |
| TVM 系统镜像下载/安装 | `TvmManager#b(url)` | `DownloadManager` + `setFileSize(140509184)`(≈134 MB) | — | `TvmManager.java:105-134`【观测】 |
| 状态机 | `TvmManager#q(ctx)` → `fh.c#x(ctx)` | 0=未安装,1=可升级,**2=下载中**,3=已就绪,4=TVM 运行中,6=条件不满足,7=镜像损坏 | `vendor.*` 系统属性 | `fh/c.java:756-783`【观测】 |
| 状态条/对话框 | `ui/TvmDialogActivity`（`AlertDialog.Theme.DayNight`） | `TvmManager#y(ctx,status)` | — | `TvmManager.java:98-103`【观测】 |
| TrustedUI 调用 | `fh.c#p()` → `IMiTrustedUIService` | — | — | `fh/c.java:627-629`【观测】 |
| 镜像安装 | `IMTService`（`vendor.xiaomi.hardware.aidl.mtdservice.IMTService/default`） | `TvmManager#i()` | — | `TvmManager.java:89-91`【观测】 |
| 白名单 | `assets/tvm_whitelist.json` | `fh.c#E(ctx)` | — | `fh/c.java:109-112`【观测】 |

**动态启用逻辑**【观测】（`Application.java:660-668`）：
```java
private void w() {
    if (Build.IS_INTERNATIONAL_BUILD) return;
    String strC  = x4.v1.c("vendor.xiaomi.trustedvm.version", null);
    String strC2 = x4.v1.c("ro.vendor.mitee_tui.support", null);
    getPackageManager().setComponentEnabledSetting(
        new ComponentName(this, "com.miui.tvm.service.TvmManagerService"),
        (TextUtils.isEmpty(strC) && TextUtils.isEmpty(strC2)) ? 2 : 1, 1);   // 2=DISABLED 1=ENABLED
}
```

系统属性消费点（`fh/c.java:785-793`）：
```java
public static String y()  { return v1.c("vendor.xiaomi.trustedvm.version", null); }
public static boolean z() { return "RUNNING".equalsIgnoreCase(v1.c("vendor.qvirtmgr.trustedvm.status", null)); }
```

> 【推断】TVM = 小米在 TEE/虚拟化层跑的"可信虚拟机"，给安全中心提供可信 UI（输入密码/生物识别确认）与可信环境能力；本 APK 只做"镜像版本比对 + 下载安装 + 状态展示"。
> 【未知】镜像真正的加载/校验流程（在 vendor 侧 `IMTService` 实现里）；`lib` 目录中无对应 .so，说明纯 AIDL 调用。

### 8.2 securitykey（4 个类，4.1 KB）—— **只用 `libsec-common-utils` 取密钥**

`com.miui.securitykey.ConfigManager`（`ConfigManager.java`）【观测】：

```java
static { System.loadLibrary("sec-common-utils"); }   // :42
private static native String getDecodeSecretKey();   // getDecodeKey()
private static native String getEncodeSecretKey();   // getEncodeKey()
private static native String getHttpKey();           // getSecurityHttpKey()
private static native String getNetworkAssistantKey(); // getNetworkAssistantEncryptKey()
private native String getRubbishSecurityKey(Context, boolean);  // getRubbishKey(ctx[,true])
private static native String getUuidKey(Context, Boolean);      // getUuidAesKey → UuidAesKey{key,iv}
```

- `getUuidAesKey` 把 native 返回的 `"<key>-<iv>"` 按 `-` 切成 2 段（`ConfigManager.java:83-105`），供 AES 用。
- 【观测】**这就是 `libsec-common-utils` 在整个 APK 里的唯一用途**（全树 `System.loadLibrary` 只有 6 处：`sec-common-utils` / `jni_load_serinum` / `tcp` / `lib-riskcontrol` / `fastplayer`+`miffmpeg` / `tensorflowlite_jni`）。
- 【未知】密钥本体在 native 中的派生（需 `open_binary` 分析 `lib/arm64-v8a/libsec-common-utils.so`）。

---

## 9. `com.xiaomi.security`（10 个类）

| 功能 | 类 | 说明 | 证据 |
|---|---|---|---|
| XSOF 能力协商 | `xsof/XSOFProvider`（Manifest:1758, exported） | `call("getServiceVersion")` 且调用方持 `com.xiaomi.security.permission.ACCESS_XSOF` 时返回 `version_name="1.0"`, `version_code=1` | `XSOFProvider.java:25-32`【观测】 |
| 安全检测服务 | `xsof/MiSafetyDetectService`（Manifest:1759, exported, remote） | `MiSafetyDetectImpl extends IMiSafetyDetectServer.Stub`；每个 API 调用都先 `Binder.getCallingPid()` → 包名 → `gj.e.a(ctx, pkg, "com.xiaomi.security.permission.ACCESS_XSOF")` 校验，再校验 caller 与自己声明的包名一致；错误码如 `1006`、`AnalyticsListener.EVENT_VIDEO_INPUT_FORMAT_CHANGED`(1004) | `MiSafetyDetectService.java:52-90`【观测】 |
| 设备凭证 | `devicecredential/*`（`ISecurityDeviceCredentialManager` 6.1 KB） | 凭证管理 AIDL（与 `credential_service` 系统设置配套，见 `Application#a0`） | 【观测】 |

> 【未知】XSOF 各 API 的语义编号（`C3`/`d8` 等混淆名）与返回值枚举，需要读 `hj.*`、`ij.*` 等支持类；本次未展开。

---

## 9.5 附：`com.miui.msa` / `com.miui.sec` / `com.miui.support`（与主扫描的交界）

| 类 | 事实 | 与本模块的关系 | 证据 |
|---|---|---|---|
| `com.miui.msa.util.MsaUtils` | 只做一件事：返回 MSA（小米广告 SDK）包名 —— 国际版且已安装则 `com.miui.msa.global`，否则 `com.miui.systemAdSolution` | 【推断】广告归因；`assets/placement_id`（`123=fb:196567740764356_196569107430886`）是 Facebook Audience Network 广告位 ID | `MsaUtils.java:9-38`【观测】 |
| `com.miui.sec.external.InstallProvider` | `call()` 只在 `IS_INTERNATIONAL_BUILD && SDK>=25` 时转发给 super，其余返回 null；`insert/query/update/delete` 全空 | **空壳 Provider**（国际版安装器兼容） | `InstallProvider.java:12-60`【观测】 |
| `com.miui.support.provider.MiuiSettingsCompat$SettingsCloudData` | **统一的云配置读取口**：`content://com.android.settings.cloud.CloudSettings/cloud_all_data`（及 `/single`、`/notify`）；`CloudData` 是 `Parcelable`，内部懒解析 `JSONObject` | ★ 被 antispam 用来覆盖 `white_url.json`（`MiuiSettingsCompat.getCloudDataString(cr,"antispam_white_url","content",null)`，`m2/e.java`）与 `service_num.json`；其他模块（预警/流量）同理 | `MiuiSettingsCompat$SettingsCloudData.java:15-54`【观测】 |
| `com.miui.support.cardview.*` / `drawable.*` | 卡片阴影/描边自绘（`CardDrawable` 7.8 KB、`CardStateDrawable` 13.4 KB） | 纯 UI，与扫描无关 | 【观测】 |

> 【观测】广告相关：首页 `MainFragment.f20481n` 是 `NativeInterstitialAdLayout`（插屏原生广告卡），会接收安全分并按阈值换配色（§2.4）；`securitycenter/ad/view/*`（`AdDownloadView`/`AdImageView`/`RoundImageView`/`RoundResizeFrameLayout`）是广告素材渲染组件。
> 【未知】广告 SDK 的服务端拉取接口未出现在 `dex_urls.txt` 的本模块条目中（`adv.sec.miui.com/*` 属 GameBooster/主题等其它模块）。

---

## 10. 云端接口汇总（本文档范围内）

| 用途 | 方法与 URL | 请求字段 | 响应字段 | 证据 |
|---|---|---|---|---|
| **SMS 规则包更新** | POST `https://api.sec.miui.com/GuardProviderV1/GetResourceZipFile` | `app_version`、`ver="2.1.2"`；header 带固定 appKey `7htr5238-a8cf-3k79-ec73-75382145ns5c` | `code`、`data.fileUrl`；包 MD5 必须 `6206c9f17a64246498b52e117d0cff1e` | `j2/a.java:261-276`、`:166-173`【观测】 |
| **云号码库增量同步** | POST `https://api.sec.miui.com/harassIntercept/intercepts` | `dataVersion`；appKey `5cdd8678-cddf-4269-ab73-48387445bba5` | `version`、`v[{status,type,uuid,p,attributes}]` | `k2/a.java:65-130`【观测】 |
| **云 URL 检测（钓鱼判定）** | POST `https://url-detect.engine.mi.com/url/detect` | `url`、`ts`、`c=2`、`sign=md5(…+"2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9")`；UA `antispam/1.0.0` | `code`、`data.urlCategoryId`（-1/0/1/2） | `policy/b.java:135-166`【观测】 |
| 缩屏清理免杀名单 | POST `https://api.sec.miui.com/trashCleaner/screenLock` | `param=Base64({"ver":n,"isDiff":true})`；appKey `5cdd8678-cddf-4269-ab73-48387445bba6` | `version`、`items[]` | `CloudDataUpdateService.java:30-60`【观测】 |
| 游戏联运判定 | POST `https://flash.sec.miui.com/game/union` | `pkg`（form） | `isUnionGame`、`frequency`（秒，默认 259200） | `GuardService.java:330-352`【观测】 |
| **报警辅助-第三方** | POST `https://gateway-papa.cloud.weizhi.com/papa/upload` | AES/RSA 加密载荷（见 §7.2） | — | `PaCommon.java:17-22`【观测】 |
| **报警辅助-信通院 EPS** | POST `https://eps.caict.ac.cn/eps/collect` | `EPSRequestBean` 14 字段 + SM3 `sign` | `status==200` 认为成功 | `EPSManager.java:97-143`【观测】 |
| 隐私政策/协议 | `https://privacy.mi.com/emergency_location/`、`https://terms.miui.com/doc/alarmassistance.html?lang=` | — | — | `PaCommon.java:20-21`【观测】 |
| **病毒云查杀** | （本 APK 内**未找到** URL） | — | — | 【未知】 |

其他相关但本次未展开的 `api.sec.miui.com` 路径（来自 `dex_urls.txt`，归属其他模块）：`/data/check`（月报）、`/v1/traffic/report`、`/universalWhitelist/checkUserId`、`/common/whiteList/*`、`/health/v1/getOptimizationFile2`、`/earthquake/*`、`/redPacket/reportStatus`、`/LBS/geoconv/admin`、`/mnntrain/taskdata`、`/12379/warning/getRegionInfo`。

---

## 11. 持久化总表（本文档范围内）

| 类型 | 名称 | 归属 | 内容 |
|---|---|---|---|
| SP | `remote_provider_preferences` | 全模块（remote 进程） | 见 0.3 + §3.5 + §4 |
| SP | `SmsEnginePreference` | antispam(SMS 引擎) | `j2/a.java:316` |
| SP | `data_config` | antivirus 结果页 | `result/ScanResultFrame.java:223` |
| SP | `credential_service_config` | securitycenter | `Application.java:542` |
| SP | `sp_apppredict`、`autoTaskSp`、`gb_gamead_data_config` 等 | 非本模块（其他队友） | — |
| **SQLite** | `AntiSpam`(v10)：`fwlog`/`phone_list`/`keyword`/`mode`/`sim`/`reportSms`/`reportSmsPending` | antispam | §4.7 |
| **SQLite(asset)** | `assets/garbage_clean.db`：`t_cache`/`t_ad`/`t_residual` | 清理（他模块，基线已录） | — |
| 文件 | `files/scanitem.json_v{0,1}` | securityscan | 云下发的体检清单覆盖 assets |
| 文件 | `files/resource.zip` → `files/SmsEngineResource/{1.bin,2.bin,v.txt,…}` | antispam | §4.4 |
| 文件 | `files/anti_fraud_pay_activity` | antifraud | 覆盖 assets 同名文件 |
| 文件 | `files/service_num.json`、`files/white_url.json`、`files/dict`/`pattern`/`phish`（`lj.c#b` 优先 `openFileInput`） | antispam | `lj/c.java:42-57`、`m2/e.java` |
| 文件 | `Download/resource.zip.tmp` | antispam 下载中转 | `j2/a.java:43,166` |
| Provider | `content://com.miui.securitycenter.remoteprovider`（`remote_provider_preferences` + `whitelist` 表） | 全局 | `RemoteProvider.java:34-38` |
| Provider | `content://antispam/{sms_judge,call_judge,url_judge,service_num_judge,call_transfer_intercept_judge,phone_list,log,log_sms,logconversation,keyword,mode,sim,unsynced_count,synced_count,allow_repeat,account,markednumber,cloudantispam,category,report_sms,report_sms_pending}` | antispam | `AntiSpamProvider.java:59-92` |
| Provider | `content://guard/{support_incremental,network_set}` | 外部 GuardProvider | §3.2 |
| Provider | `content://com.miui.riskcontrol`（仅 `call`） | riskcontrol | §5.2 |
| Provider | `content://com.miui.luckymoney.provider` | luckymoney | Manifest:1127 |
| Provider | `content://com.xiaomi.security.xsof.provider/XSOF` | XSOF | `XSOFProvider.java:21` |
| Settings.Secure | `fraud_num_state*`/`agent_num_state*`/`sell_num_state*`/`harass_num_state*`/`sms_classifier_*`/`*_mode`/`backsound_index*`/`is_call_transfer_blocked*`/`show_notification_type*`/`com_miui_warningcenter_(eps|pa)_status`/`key_latest_virus_scan_date`/`security_adb_install_enable`/`default_input_method`/`accessibility_button_targets` | 各模块 | 见各节 |
| 系统属性（读） | `vendor.xiaomi.trustedvm.version`、`vendor.qvirtmgr.trustedvm.status`、`ro.vendor.mitee_tui.support`、`persist.sys.miui_optimization`、`ro.miui.cts`、`ro.mi.os.version.code` | tvm / job 调度 | 【观测】 |

---

## 12. 未解之谜（需后续专项）

1. **病毒云查杀的真实通道**：`preference_key_open_virus_cloud_scan` 打开后走哪个接口？本 APK 无 URL、`IAntiVirusServer` 无云查方法 → 极可能由 `com.miui.guardprovider` APK 自行联网。需拿到 GuardProvider 才能定论。
2. **`assets/dict` / `assets/censor_source` 的消费方**：Java 层只有 `lj/g.java:32` 读 `dict`（短信分词）；`censor_source` 无 Java 引用。是否被 `liblib-riskcontrol.so` 通过 `setAssetManager` 读取 —— 必须 native 分析。
3. **`assets/model`(680 KB) 与 `rec_model_20230425.mnn`(810 KB)**：本模块零引用。消费方未知。
4. **`com.miui.antifraud.d`(13.9 KB) 的判定与上报**：只确认了入口（PHONE_STATE / SCAN_RISKY_URL）与页面监控，未见完整规则与上报 payload。
5. **`liblib-riskcontrol.so` 内部**：敏感词表来源、是否落地、是否有远端词典。
6. **TVM 镜像的安装/校验**：`IMTService` 是 vendor 实现，本 APK 仅 AIDL 客户端。
7. **`assets/pattern` 编码**：文件以 GBK/混合编码存储，`lj.c` 用 `new InputStreamReader(stream)`（**平台默认编码**，未显式指定），中文 pattern 在某些 locale 下可能被误读 —— 这可能是"规则在部分设备上失效"的潜在缺陷（**推断**）。
8. **`SensitiveWordDetectProvider` 的 `censorResult` URI**：只为占位，无实际 query 实现（`query()` 恒返回 null）。

---

## 附录 A：单条证据索引（按需回查）

| 主题 | 文件:行 |
|---|---|
| 进程分派 | `com/miui/securitycenter/Application.java:683-842` |
| IdleHandler 重业务 | `Application.java:820-825` → `K()` `:361-410` |
| TVM 组件动态开关 | `Application.java:660-668` |
| 保活（反射 setProcessImportant） | `com/miui/securitycenter/service/RemoteService.java:38-49` |
| 开机广播 | `com/miui/securitycenter/receiver/BootReceiver.java:80-118` |
| 跨进程偏好 | `r4/a.java:19-200` + `com/miui/common/persistence/RemoteProvider.java:40-284` |
| 体检清单 | `apk_full/assets/scanitem.json` + `securityscan/model/ModelFactory.java:53-229` |
| 扫描编排 | `securityscan/scanner/k.java:808-960` |
| 打分 | `securityscan/scanner/ScoreManager.java:112-220,445-509` |
| 病毒扫描目标枚举 | `securityscan/scanner/o.java:710-769` |
| GuardProvider 绑定 | `p9/a.java:136-192` + `com/miui/guardprovider/aidl/IAntiVirusServer.java:561-607` |
| 病毒库 Job | `antivirus/service/VirusAutoUpdateJobService.java:197-236` |
| GuardService 主逻辑 | `antivirus/service/GuardService.java:385-410,638-800` |
| SafePay 规则资产 | `apk_full/assets/safepay_calling_activity`、`safepay_target_activity`、`safepay_target_package`、`safepay_imm_whitelist` |
| 拦截策略链 | `h2/e.java:113-159` + `b2/b.java:81-169` |
| 系统号码/关键字库读取 | `h2/b.java:84-156` |
| pattern 解析 | `nj/d.java:61-95`、`nj/c.java:51-123`、`nj/e.java:70-97` |
| SMS TFLite 引擎 | `da/a.java:31-117`、`aa/a.java`、`aa/c.java`、`ca/a.java`、`h2/a.java:234-331` |
| 规则包下载/校验 | `j2/a.java:133-276` |
| 云号码库 | `k2/a.java:45-130` |
| URL 拦截 | `com/miui/antispam/policy/b.java:135-267` |
| antispam DB | `e2/a.java:116-148` + `res/values/strings.xml`(`create_table_*`) |
| protobuf 备份 | `apk_full/assets/protobuf/{antispam,syncroot}.proto` + `com/miui/antispam/service/backup/SecurityCenterBackupAgent.java:30-171` |
| 反诈入口 | `com/miui/antifraud/AntiFraudReceiver.java:20-40`、`PayActivityMonitorService.java:85-127` |
| 敏感词 Provider | `com/miui/riskcontrol/SensitiveWordDetectProvider.java:19-79` |
| 红包监听 | `com/miui/luckymoney/service/LuckyMoneyMonitorService.java:58-100,293-403` |
| 红包"快开自动点" | `com/miui/luckymoney/controller/Pipeline.java:52-64,203-251` + `ui/view/PendingIntentRunnable.java:13-23` |
| 报警辅助 | `com/miui/warningcenter/policeassist/PaService.java:75-200,434-520`、`PaCommon.java`、`com/miui/policeassist/EPSManager.java` |
| TVM | `com/miui/tvm/TvmManager.java`、`fh/c.java:756-793` |
| 本地密钥 | `com/miui/securitykey/ConfigManager.java:41-107` |
| XSOF | `com/xiaomi/security/xsof/XSOFProvider.java`、`MiSafetyDetectService.java:52-90` |
