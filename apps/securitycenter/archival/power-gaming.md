> **历史分析存档，非最终结论。** 本文保留原始分析的技术内容，可能包含尚未验证或已被纠正的表述。隐私数据上传范围、同意条件、FBO 本地委托、报警短信加密、Provider 鉴权等，以 [最终报告](../REPORT.md) 与 [独立复核](../REVIEW.md) 为准。文中源码定位保留为原反编译相对路径；只有 evidence/ 中列出的精选文件随包提供，未打包完整源码树。

# 电量与性能 / 存储清理 / 游戏加速 / 自动任务 / 端侧 AI — 模块逆向报告

> 目标：`MIUISecurityCenter-10.4.9-250402.0.1.apk`（`com.miui.securitycenter`，versionCode 40001049）
> SHA-256 `fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd`
> 分析者：`power-gaming`｜证据源：jadx 全量反编译（10624 `.java`）、apktool 资源、`assets/` 二进制、REA SQLite
> 标注约定：**观测** = shipped artifact 事实（可直接 goto 到行）｜**推断** = 由观测组合出的合理结论｜**未知** = 无证据
> **全部结论为静态分析，无设备/动态证据。**

---

## 0. 对共享基线的三处修正（重要）

| 基线原述 | 本次观测 | 证据 |
|---|---|---|
| `assets/character.json` 非明文/疑似加密 | **错**。它是 600B 明文 JSON：`{"<pad>":"0","<unk>":"1","a":"2","b":"3",…}`，被 `ea.c.a()` 用 `BufferedReader` + `JSONObject` 当字符表解析 | 文件头 hex `7b 0a 20 20 20 20 22 3c 70 61 64 3e…`；`ea/c.java:21`、`ea/c.java:44-58` |
| `assets/dict`(3.6MB)/`assets/model`(680KB) 属端侧 AI 预测模型 | **不属 app 预测**。二者是**反垃圾/防钓鱼文本分类器**的资源（`lj.c.f29439b = {"dict","model","pattern","phish"}`，`lj.g.d()` 把 `dict` 喂给 `mj.a`）。app 预测只用一个模型：`assets/rec_model_20230425.mnn` | `lj/c.java:17`、`lj/g.java:32-36`、`ia/d.java:105-113` |
| `assets/garbage_clean.db` 是垃圾清理规则库（暗示被本 APK 使用） | 表结构正确（`t_cache`/`t_ad`/`t_residual` 三张**同构**表，各 8 列，**零索引/零视图/零触发器**），但**全 DEX 无任何代码引用**该文件名。真正的清理引擎在**外部 App**（`com.miui.cleanmaster`） | `inspect_sqlite_database` evidence `ev_864f519d…`；`findings/dex_strings_all.txt` 中 `garbage_clean` 仅命中 `R.string` 名；`securityscan/scanner/CacheCheckManager.java:104-114` |

`pinyinindex.idf` = 中文→拼音索引（自定义 `IDF ` 魔数 + `cl.a` DirectIndexedFile 读取器），服务联系人/应用 T9 搜索，**与电量/游戏/AI 预测无关**（`am/a.java:47-58`、`cl/b.java:29`）。

---

## 1. 全局架构：谁真正干活

```
                       ┌──────────────────────────────────────────────┐
   用户可见 UI ────────►│ com.miui.securitycenter (本 APK)            │
   (PowerCenter/       │  · 只做：UI / 策略判定 / 写 Settings / 广播  │
    GameBooster/… )    │  · 不直接调 CPU/GPU governor                │
                       └───┬───────────┬──────────┬─────────┬────────┘
                           │           │          │         │
          ContentResolver.call()   bindService  bindService  AIDL
                           │           │          │         │
             ┌─────────────▼──┐  ┌─────▼──────┐ ┌─▼────────────┐ ┌▼──────────────┐
             │ powerkeeper    │  │ joyose     │ │ cleanmaster  │ │ powercenter   │
             │ (隐藏模式/thermal│ │ (GPU tuner │ │ (GC 引擎)     │ │ .powersaver   │
             │  /feedback)     │ │  GunSight/ │ │              │ │ (本 APK 自身   │
             │                │ │  4D 振感)   │ │              │ │  ContentProvider)│
             └───────┬────────┘  └─────┬──────┘ └──────────────┘ └───────────────┘
                     │                 │
              /sys/class/thermal/*  GPU/CPU 调频（进程外，本 APK 不可见）
```

**核心结论（观测）**：本 APK 对"省电/性能/游戏加速"的落地方式是**跨进程配置下发**，不是本地 sysfs 调频。唯一直写 sysfs 的地方是**温度/充电**相关（见 §2.6），全部通过 `FileWriter` 明文写字符串。

---

## 2. `com.miui.powercenter`（158 类）—— 电量与性能主体

### 2.1 功能清单与入口

| 用户功能 | 入口组件（Manifest） | 关键类 |
|---|---|---|
| 电量与性能主页 | `PowerMainActivity`（exported） | `powercenter/PowerMainActivity.java` |
| 省电模式详情 | `savemode.PowerSaveActivity`（`com.miui.powercenter.POWER_SAVE`） | `powercenter/savemode/PowerSaveFragment.java` |
| 电量设置（性能模式开关+120s 提醒） | `PowerSettings` / `PowerSettingsFragment` | `powercenter/PowerSettingsFragment.java:125,161-174` |
| 性能模式快捷开关 | `powersaver.PerformanceModeTileService`（Tile） | `powersaver/PerformanceModeTileService.java:107` |
| 狂野模式快捷开关 | `powersaver.PerformanceWildModeTileService`（Tile） | 同上（`pg.i.H()` 切换文案） |
| 耗电排行 / 应用耗电详情 | `legacypowerrank.PowerDetailActivity`（exported） | `legacypowerrank/*`、`batteryhistory/*` |
| 耗电异常检测 | 主页卡片 | `abnormalscan/AbnormalDataModel.java`、`abnormalscan/AbScanModel.java` |
| 电池健康 / 非官方电池 | `unofficalbattery.UnofficalBatteryActivity` | `unofficalbattery/*` |
| 夜间充电保护（智能充电） | `nightcharge.IntellectProtectSettingActivity`、`ChargerProtectActivity` | `nightcharge/*` |
| 充电特性（快充/无线/反向） | `charge.ChargeFeatureActivity`、`fastCharge.FastChargeActivity`、`wirelesscharge.*` | `charge/*`、`fastCharge/*`、`wirelesscharge/*` |
| 定时开关机 | `bootshutdown.PowerShutdownOnTime`（`miui.powercenter.intent.action.BOOT_SHUTDOWN_ONTIME`） | `bootshutdown/PowerShutdownOnTime.java` |
| 省电建议 | `deepsave.BatterySaveIdeaActivity` | `deepsave/*` |
| 低电量通知/引导 | 通知 + `PowerReceiver` | `powerui/PowerReceiver.java`、`de.PowerNoticeUtils(de/j.java)` |
| 月报数据 | `MonthReportProvider`（`content://…`） | `monthreport/MonthReportProvider.java` |

**Widget**：本模块**无** AppWidget；`com.miui.securitycenter.WidgetProvider` 处理 `com.miui.intent.action.CHANGE_POWER_SAVE_MODE`（`securitycenter/WidgetProvider.java:92,261`），属桌面快捷方式/大文件夹入口，不是 appwidget。

### 2.2 四种"模式"的权威状态位（观测）

| 模式 | Settings 键 | 读写点 |
|---|---|---|
| 省电模式（隐藏模式 `enhance`） | `Settings.System.POWER_SAVE_MODE_OPEN` | `me/w.java` `Q()`；`PowerSaverProvider.java:279,333` |
| 性能模式（隐藏模式 `performance`） | `Settings.System.POWER_PERFORMANCE_MODE_OPEN` | `me/w.java` `P()`；`PowerSaverProvider.java:218,222` |
| 均衡模式 | `Settings.System.POWER_BALANCED_MODE_OPEN` | `me/w.java` `F()`；`PowerSaverProvider.java:186` |
| 超级省电/极限省电 | `Settings.System.power_supersave_mode_open` + `Settings.Secure.EXTREME_POWER_MODE_ENABLE` | `me/w.java` `S()`；`de/j.java:200` |
| 超级省电过渡态 | `Settings.System.superpower_switching_state` | `pg/i.java:74,152` |

### 2.3 唯一权威写入口：`PowerSaverProvider.call()`（观测）

`content://com.miui.powercenter.powersaver`（Manifest 未列 authority，但代码里 `Uri.parse("content://com.miui.powercenter.powersaver")` 硬编码调用）暴露 8 个方法：

| method | 参数 key | 作用 |
|---|---|---|
| `changePowerMode` | `POWER_SAVE_MODE_OPEN`、(可选)`LOW_BATTERY_DIALOG`、`deviceId` | 开关省电模式 → `n(boolean,boolean,String)` |
| `changePerformanceMode` | `POWER_PERFORMANCE_MODE_OPEN`、`POWER_PERFORMANCE_MODE_FROMUSER` | 开关性能模式 → `l()`→`k()` |
| `changeBalancedMode` | `POWER_BALANCED_MODE_OPEN`、`POWER_BALANCED_MODE_FROMUSER` | 开关均衡 → `j()`→`i()` |
| `changeSuperPowerMode` | `POWER_SUPERSAVE_MODE_OPEN`、`POWER_SUPERSAVE_MODE_FROMUSER` | 超级省电 → `c(bundle,false)` |
| `changeExtremeEnduranceMode` | 同上 | 极限续航 → `c(bundle,true)` |
| `cleanMemory` | `CLEAN_MEMORY_EXCEPTION_LIST`(ArrayList) | → `jg.b.b(list)` 清内存（白名单内豁免） |
| `showLowBatteryDialog` | — | 按电量档位弹不同提示 |
| `showSuperPowerHome` | — | → `e.G(ctx)` |

证据：`powercenter/powersaver/PowerSaverProvider.java:387-432`。

互斥逻辑（观测）：`n()` 开省电时先 `if (w.Q()) return`（已开则退出），并**先关性能/均衡**：`k(false)`+`j.y()`、`i(false)`（`:305-315`）；`l()` 开性能时也先关省电与均衡（`:236-249`）。

### 2.4 省电模式的 7 个实际动作（`be/*`，实现逻辑核心）

`PowerSaverProvider.d()` 构造动作列表 `[be.g, be.c, be.h, be.i, (非稳定版)be.a, (SDK<33)be.b]`，`e()` 为 `[be.e]`；进模式调 `b()`，退模式调 `a()`（`PowerSaverProvider.java:110-129, 275-332`）。

| 类 | Log tag | 实际动作（观测） |
|---|---|---|
| `be.g` **LowFpsPowerMode** | `LowFpsPowerMode` | 刷新率→**60Hz**。通道①`miui.hardware.display.DisplayFeatureManager.setScreenEffect(24, fps[,250ms])` 反射（`:106-124`）；通道②`Settings.Secure.user_refresh_rate`(SDK≥33)/`Settings.System.user_refresh_rate`、`peak_refresh_rate`、`miui_refresh_rate`。原始帧率取自 `persist.vendor.dfps.level` 或 `user_refresh_rate`，缺省 `FeatureParser.getInteger("smart_fps_value"/"defaultFps")`；若 `ro.vendor.fps.switch.default=true` 走 Settings 分支。进模式前把原刷新率/智能帧率存 `gd.c.d2()`/`gd.c.o1()`，退出时恢复（`:170-297`） |
| `be.c` **FingerAodPowerMode** | `FingerAodPowerMode` | 关屏下指纹 AOD：`Settings.Secure.gxzw_icon_aod_show_enable` → 0，原值备份在 `power_center_finger_aod`。仅当 `ro.hardware.fp.fod=true` 且已录指纹>0 |
| `be.h` **SoundPowerMode** | `SoundPowerMode` | 5 个系统音全关：`dtmf_tone`/`sound_effects_enabled`/`lockscreen_sounds_enabled`/`has_screenshot_sound`/`delete_sound_effect`，各原值备份到 `power_center_sound_mode_*`（值 -1 表示未备份） |
| `be.i` **WakeupPowerMode** | — | 关抬腕/双击亮屏/通知亮屏：`pick_up_gesture_wakeup_mode`/`gesture_wakeup`/`wakeup_for_keyguard_notification` → 0，备份到 `power_center_wakeup_*` |
| `be.a` | — | 自动亮度上限压到 30%（`gd.c.v()` 备份，`me.t.O(n)` 设置）；仅 `!Build.IS_STABLE_VERSION` 时入列 |
| `be.b` **BrightnessPowerMode** | `BrightnessPowerMode` | 非自动亮度时手动亮度 **-5%**（`me.m.r(ctx, cur-5)`）；仅 `SDK<33` 时入列 |
| `be.d` | — | 5G 关闭（国际版）：`power_center_is_5g_mode_enable(_slot0/1)` 备份，`me.h.k(false)`/`me.h.m(false,slot)` |
| `be.e` **HapticPowerMode** | — | 关振动反馈 `haptic_feedback_enabled`→0；仅当 `sys.haptic.motor ∈ {linear, zlinear}`（线性马达） |

`n()` 内联的额外动作（`PowerSaverProvider.java:266-353`）：
- 同步开关：`ContentResolver.setMasterSyncAutomatically(false)`，键 `POWER_SAVE_PRE_SYNCHRONIZE_ENABLE`（`ce/g.java:31-51`）
- 内存清理时机：`POWER_SAVE_PRE_CLEAN_MEMORY_TIME` 抬到 600s（`ce/g.java:304-317`）
- 定位降级：`POWER_SAVE_PRE_LOCATION_MODE` → `Settings.Secure.location_mode`（仅 SDK≤28，`ce/g.java:319-338`）
- Wi-Fi 天线减半：反射 `android.net.wifi.MiuiWifiManager.setAntHalf(int)`（`ce/g.java:53-76`）
- **真实调频请求**：`g.y(ctx, "ultimate"|"performance"|"enhance")` → 见 §2.5

### 2.5 真正的 CPU/GPU 调频通道（观测，纠正"sysfs 调频"说法）

`ce.g.y()` 并不写 sysfs，而是写 **powerkeeper 的 ContentProvider**：

```
ce/g.java:340-363  y(ctx, status)
  → x(ctx, str)  :354
      → ContentResolver.call("content://com.miui.powerkeeper.configure/GlobalFeatureTable",
                              "GlobalFeatureTableupdate", null, {userConfigureStatus: str})
```

`status ∈ {enhance, performance, ultimate}`，读回用 `GlobalFeatureTablequery`（`ce/g.java:137-145`）。**行为调频由 powerkeeper 进程根据 `userConfigureStatus` 执行**（进程外，本 APK 不可见 → `未知` 具体 governor 节点）。

辅助通道（同 provider，`content://com.miui.powerkeeper.configure`）：

| method / uri | 用途 | 证据 |
|---|---|---|
| `changeExtremePowerMode` | 极限省电开关（bundle key `EXTREME_POWER_SAVE_MODE_OPEN`/`IS_NOTIFY`/`SOURCE`） | `de/j.java:250-260` |
| `getBcasTempModeState` → `BcasTempMode` | 温控档位，与 `low_temperature_extreme_endurance_temp_mode_value`(默认 3) 比较 | `ce/g.java:195-205` |
| `isSupportSleepMode` → `power_support_sleep_mode` | 睡眠模式支持 | `ce/g.java:255-262` |
| `SimpleSettings/misc` `GET_misc sleep_mode_user` | 睡眠模式用户值 `on`/`off` | `ce/g.java:167-176` |
| `SimpleSettings/misc` `GET_misc user_de_configured_apps` | 用户已配置的后台策略应用 | `legacypowerrank/PowerDetailActivity.java:902` |
| `SimpleSettings/misc` `GET_misc key_thermal_configure` / `PUT_misc` | 温控配置读写 | `me/a0.java:190,227` |
| `warmControlModeSupported` | 温控模式支持 | `me/a0.java:216` |
| `UserConfigure` 表 `query` / `METHOD_UPDATE` | 单应用后台/省电策略 | `a8/k0.java:210,288`、`networkassistant/firewall/UserConfigure.java:27` |
| `IPowerKeeper` AIDL `B7/t0/t6/X5/D2/M1/k5` | Bundle 化的通用命令通道 + 应用活跃态监听 | `powerkeeper/IPowerKeeper.java:202-214` |

另：`miui.permission.powerkeeper.HIDDEN_MODE_PROVIDER` 与 `com.miui.powerkeeper.permission.POWER_NOTIFY` 是 Manifest 声明的对接权限（观测，`AndroidManifest.xml`）。

`PowerManager` 反射垫片：`h(boolean)` 在 SDK≥30 且 `persist.sys.miui_optimization` 允许时反射调隐藏 API `PowerManager.setPowerSaveModeEnabled(boolean)`（`PowerSaverProvider.java:160-172`，经 `bh.d.c(...)`）。

### 2.6 直写 sysfs 的地方（仅温度/充电，观测）

**这是本 APK 唯一的 sysfs 写入面**，全部 `FileWriter` 明文，无 CPU/GPU governor 节点：

| 节点 | 方向 | 用途 | 证据 |
|---|---|---|---|
| `/sys/class/thermal/thermal_message/sconfig` | W | 写温控档位 int（同时 `SystemProperties.set("persist.sys.thermal.config", i)`） | `me/a0.java:127-145` |
| `/sys/class/qcom-battery/lpd_charging` | R/W | 读/写 LPD 限充状态（智能充电/夜间保护落地） | `me/f.java:171(读), 229(写)` |
| `/sys/class/thermal/thermal_message/charger_temp` | W | 充电器温度阈值写 `"0"` 或数值 | `sd/f.java:256` |
| `/sys/class/thermal/thermal_message/board_sensor_temp`、`board_sensor_second_temp` | R | 主板温度 | `me/w.java:66`、`x5/p.java:488,491` |
| `/sys/class/thermal/thermal_message/charger_temp` | R | 充电温度 | `me/e.java:45` |
| `/sys/class/power_supply/battery/{charge_full,charge_full_design,model_name}`、`/sys/class/qcom-battery/battery/{charge_full,charge_full_design}`、`fg1_rm`/`fg2_rm`、`slave_authentic`、`real_type`、`tx_adapter`、`power_max`、`usb/type` | R | 电池健康 / 容量 / 双电芯 / 原装识别 / 充电器类型 | `me/e.java:131-133,212-214,295,318`、`me/b.java:62`、`me/w.java:139,636,743,748` |
| `/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq` | **R only** | 读最大频率（用于展示/评分，**不写**） | `hl/a.java:322` |
| `/sys/class/net/wlan0/address` | R | MAC | `v9/b.java:18` |

`/proc/cpuinfo` 亦只读（`hl/a.java:517,662`）。

### 2.7 配置与阈值

`gd.c`（~26KB，共约 400 个存取器）是全部电量/充电/温控阈值的集中层，落地在 **`content://com.miui.securitycenter.remoteprovider` 的 `callPreference`**（跨进程 SP 代理，支持 `110@authority` 多用户/分身前缀），见 `r4/a.java:19-51`。

代表性阈值（观测，全部经 `gd.c`）：

| 键 | 默认 | 含义 | 证据 |
|---|---|---|---|
| `key_partical_wakelock_abnormal_threshold` | 7 | 部分唤醒锁异常判定 | `gd/c.java:65` |
| `cpu_abnormal_threshold` | — | CPU 耗电异常判定（`PowerSaveService` dump 可见） | `powercenter/provider/PowerSaveService.java:659` |
| `on_time_shutdown_time` | 1410 | 定时关机（23:30） | `gd/c.java:22` |
| `pc_low_temperature_extreme_endurance_switch` | 1 | 低温极限续航开关 | `ce/g.java:149` |
| `low_temperature_extreme_endurance_temp_mode_value` | 3 | 低温触发温控档 | `ce/g.java:157` |
| `low_temperature_extreme_endurance_support` | 0 | 机型支持位 | `ce/g.java:225` |
| `sec_pc_config_scenario_policies_open` | -1 | 场景策略（睡眠模式） | `ce/g.java:162` |
| `key_charge_full_in_night` | false | 夜间充满 | `gd/c.java:17` |
| `pc_notification_id_smart_charge_pogo` | int res | Pogo Pin 充电通知 ID | `de/j.java:606` |
| `power_dialog_performance_mode_checkbox` | 文案含 **120** | 性能模式 120s 自动退出提示 | `batteryhistory/c0.java:512`、`j0.java:1036` |

设备特性门控（`miui.util.FeatureParser`，`/system/etc/device_features/*.xml`）：
- `support_extreme_battery_saver`、`support_superpower_replace_extremesaver`、`support_wild_boost_bat_perf`（狂野模式 V1）、`support_power_save_new`、`bcl_level0_region_list`、`fpsList`、`smart_fps_value`、`defaultFps`、`support_hangup_while_screen_off`（`pg/i.java:65-68,131`、`ce/g.java:116,252`、`be/g.java` static）
- `persist.vendor.battery.erp`（国际版 ERP 电量）、`persist.sys.power.default.powermode`（默认隐藏模式，缺省 `"performance"`，`"unSupported"` 表示不支持）、`persist.vendor.audio.power.save.setting & 9 == 9`（音频省电）、`persist.vendor.fps.switch.default`、`ro.hardware.fp.fod`、`sys.haptic.motor`、`ro.miui.cts`
- 狂野模式 V2 判定：`pg.i.J()` 把 `i.g()`（机型）与 `FeatureParser.getStringArray` 白名单比对（`pg/i.java:134-146`）

### 2.8 数据与持久化

- **无自有数据库**（batteryhistory 有 Provider 但 `batteryhistory/BatteryHistoryProvider` 未在本次深挖）。
- Settings.System/Secure 键约 **40+**（上表 + `POWER_SAVE_*` 备份键族）。
- SP 全部走 `remoteprovider.callPreference`（`r4.a`）；`base_model`/`train_configuration` 等在 apppredict 的 SP（§10）。
- 跨进程：`com.miui.securitycenter.remote` 进程承载 `PowerSaveService`/`PowerCenterProvider`/`AlarmReceiver`/`AutoTaskIntentService`/`ShutdownAlarmIntentService`/`FboJobService`（Manifest）。

### 2.9 `com.miui.superpower`（23 类）

**观测**：它**不是**省电逻辑，而是**超级省电模式的"极简桌面"UI**——自绘状态栏。
- `SuperPowerProgressActivity`（进入/退出进度动画，`PowerSaverProvider.java:92` 启动）
- `SuperPowerSettings` / `SuperPowerSettingsFragement` / `SuperPowerDetailPreference`
- `notification.SuperPowerTileService`（QS Tile，`enabled="false"` 默认关）
- `statusbar/*`：`Clock`、`WifiViewLinearLayout`、`icon.BatteryView/SimSignalView/WifiSignalView/FlightModeView`、`button.{BlueTooth,Cellular,GPS,Vibrate,Wifi}Button`、`panel.NotificationPanelLayout`、`slider.ToggleSeekBar/ToggleSliderView`、`utils.DotsFirstLineCenterTextView`
- 证据：包内文件清单 + Manifest `<service …SuperPowerTileService>`；`pg/i.L()` 广播 `com.miui.powercenter.SUPER_POWER` 携带 `progress`(0/1/2/3) 驱动该 UI（`pg/i.java:148-189`）

### 2.10 `com.miui.powerkeeper`（5 类）

**观测**：仅 3 个 AIDL + 2 个 bean，**无实现**。本 APK 只是 powerkeeper 的**客户端**。
- `IPowerKeeper`（7 个 transact：`X5(int)`/`B7(Bundle)`/`t0(Bundle,Bundle)`/`D2()→List<AppStateInfo>`/`M1`,`k5`(注册/注销 `IAppActiveUIChangedListener`)/`t6(String)`）
- `feedbackcontrol.IFeedbackControl`（12 个 transact：`f3(int)`/`Y3()`/`L4`,`X0`(listener)/`G2()→Bundle`/`K7()`/`r5(int,String)`/`Z5()`/`f1()`/`i0(boolean)`/`a4(String[])`/`r3()`）—— 供 gamebooster 做**温控/性能反馈**（`gamebooster/service/w.java:28` 导入）
- `ui.AppStateInfo`、`ui.IAppActiveUIChangedListener`
- 调用点：`j8/u.java:79`（VideoEffect）、`legacypowerrank/PowerDetailActivity.java:976`

---

## 3. `com.miui.optimizecenter`（40 类）—— 存储清理

### 3.1 功能清单与入口

| 功能 | 入口 | 类 |
|---|---|---|
| 存储空间主页（彩色柱状图） | `storage.StorageActivity`（exported，action `com.miui.cleanmaster.action.STORAGE_MANAGE`） | `storage/StorageFragment.java`、`storage/StorageFragmentWork.java` |
| 深度清理入口 | 同上（`button_deepclean`） | `StorageFragment.java:167` |
| 应用存储列表 | `AppStorageListActivity` | `storage/AppStorageListActivity.java` |
| 应用存储详情（缓存/数据/APK/其他） | `AppStorageDetailActivity`（`getStringExtra("model")` 传包名） | `storage/AppStorageDetailActivity.java:765` |
| 系统数据 | `SystemStorageDataActivity` | `storage/AppSystemDataManager.java`(27KB) |
| 公共大文件（图片/视频/音频/文档） | `StoragePublicFileListActivity` | `storage/model/PublicFileModel.java` |
| 存储设置 | `StorageSettingsActivity` | — |
| **FBO**（File Based Optimization）结果页 | `FboResultActivity`（`miui.permission.USE_INTERNAL_GENERAL_API`） | `storage/FboResultActivity.java` |
| UFS 提示 | `UfsTipActivity` | `storage/UfsTipActivity.java` |
| 存储 Widget | 无 appwidget（`widget/storage/StorageColumnView` 是自绘 View） | — |

### 3.2 清理规则引擎 —— **不在本 APK**（观测）

```
securityscan/scanner/CacheCheckManager.java:103-115
  a.run() → 国内: pkg="com.miui.cleanmaster", action="com.miui.cleanmaster.action.CHECK_GARBAGE_CHECK"
            国际: pkg=c4.j.c(ctx), action=c4.j.f7946b.get(pkg)
  → o4.a.d(action, pkg, callback)
      → callback.v1(IBinder)  :92
          → IGarbageCheck.Stub.asInterface(binder)  :72
              → iGarbageCheck.F3(IGarbageScanCallback)   // 开始扫描
              → iGarbageCheck.J(list, IGarbageCleanupCallback) // 执行清理
```

`com.miui.optimizecenter.garbagecheck.IGarbageCheck` 只有 3 个 transact：`F3(IGarbageScanCallback)` / `v1?` / `J(List<String>, IGarbageCleanupCallback)`，见 `IGarbageCheck.java:108`。
扫描回调 `IGarbageScanCallback`：`U5(pkg, path, ?, size, isDir)`（逐项回调）、`u(String)`（过滤?）、`d()`/`h()`（开始/结束），见 `IGarbageScanCallback.java`。

**因此 `assets/garbage_clean.db` 在本 APK 内是"死重量"**：SQLite 结构完整（3 张同构表 + `android_metadata` + `sqlite_sequence`，`advise_del` 列存"是否建议删除"），但零代码引用。真实规则库随 `com.miui.cleanmaster` 分发。**推断**：可能是构建期误打包 / 或留给 cleanmaster 通过 signature 级 provider 读取的历史遗留。

`FboJobService` 语义（观测，`storage/fbo/FboJobService.java`）：
- **不是** "File Based Optimization" 的算法实现，只是一个 **JobScheduler 触发器**
- `d(ctx)`：SDK≥28 且 `ta.d.b()`（=`fboIsSupport`）为真时，注册 `JobInfo(id=LEVEL_LARGE_ONLY_TITLE, periodic=**21_600_000ms = 6h**, persisted=true, requiresDeviceIdle=true)`
- `onStartJob` → 后台线程执行 `FboPackageBean.getInstance().upLoadPackageList()`，即**上传应用包列表**（画像/统计用途），不做文件操作
- 证据：`FboJobService.java:30-95`

### 3.3 `com.miui.optimizemanage`（29 类）—— 一键加速 / 急救

| 功能 | 类 |
|---|---|
| 一键加速主页（转盘动画） | `OptimizemanageMainActivity`、`view/OptimizeMainView.java`、`view/OptimizeManageAnimView.java` |
| 清理/结果页 | `CleanFragment.java`(48KB)、`ResultFragment.java`(40KB)、`optimizeresult/*`(12 类) |
| 内存清理 | `memoryclean/a.java`、`memoryclean/LockAppManageActivity`（锁定应用不清理） |
| 设置（下拉锁屏/内存占用样式） | `settings/SettingsFragment.java`、`settings/DropDownLockScreenAdapter`、`DropDownMemoryOccupyAdapter` |
| 清理结果联动卡片（含跳垃圾清理） | `optimizeresult/e.java:127`（`miui.intent.action.GARBAGE_CLEANUP`） |

入口：`OptimizemanageMainActivity`（exported）。`com.miui.cleanmaster`（6 类）只是**薄壳**：`CleanerHandleService`（`android:process="com.miui.securitycenter.remote"`，`permission=com.miui.securitycenter.permission.CLEANER_MANAGE`）、`CleanerInstallActivity`（`com.miui.cleanmaster.InstallAndLunchCleanMaster`）、`LowMemoryIntentDispatchActivity`（`com.miui.securitycenter.LunchCleanMaster`）、`ICleanerShortcut` AIDL。`com.miui.cleaner.ICleanerShortcut`（1 类）= 旧版 AIDL 兼容。

### 3.4 `com.miui.firstaidkit`（28 类）—— 手机急救

`FirstAidKitActivity`（exported）+ 白名单 Activity。Model 层即功能全集（观测）：

| 分组 | Model 类 | 用户可见项 |
|---|---|---|
| 耗电 | `consumePower/AutoStartApplicationModel`、`HotspotModel` | 自启动管理、热点异常 |
| 网络 | `internet/BackgroundConnectionModel`、`RestrictDataUsageModel`、`WifiOrFlowSwitchOffModel` | 后台联网、流量限制、WiFi/数据关闭 |
| 操作 | `operation/AirplaneModel`、`BlockCallsFromContactsModel`、`BlockCallsFromStrangersModel`、`DoNotDisturbModel`、`EyeshieldModel`、`FidCleanerShortcutModel` | 飞行模式、拦截联系人/陌生人、免打扰、护眼、清理快捷方式 |
| 其他 | `other/SimModel`、`SystemRootModel`、`UnableInstallApplicationModel` | SIM 卡、Root、无法安装应用 |
| 性能 | `performance/AuxiliaryFunctionModel`、`DeviceManagerModel`、`InsufficientMemoryModel` | 无障碍服务滥用、设备管理器、内存不足 |

扫描项资源：`assets/firstaid_scanitem.json`(4923B)。

---

## 4. `com.miui.gamebooster`（280 类）—— 游戏加速

### 4.1 子包全览（观测，类数）

`ui`(51)、`model`(32)、`globalgame`(30)、`customview`(25)、`windowmanager`(22)、`service`(21)、`beauty`(16)、`videobox`(11)、`aihelper`(10)、`widget`(10)、`shoulderkey`(8)、`view`(6)、`utils`(5)、`voicechanger`(5)、`brightness`(4)、`mutiwindow`(3)、`predownload`(3)、`xunyou`(3)、`framerate`(2)、`gbservices`(2)、`storage`(2)、`common`(1)、`encoder`(1)、`gamemode`(1)、`gametime`(1)、`guide`(1)、`provider`(1)

### 4.2 功能清单与入口

| 功能 | 入口组件 | 说明 |
|---|---|---|
| 游戏加速主页 | `ui.GameBoosterMainActivity`（exported，icon `game_booster_icon`）→ `ui.GameBoosterRealMainActivity` | `WelcomActivity` 为 exported 分发器（`:187-193`） |
| 添加游戏 / 白名单 | `ui.SelectGameActivity`、`ui.WhiteListActivity` | |
| 高级设置 | `ui.AdvanceSettingsActivity` / `AdvanceSettingsDetailActivity` | |
| **性能模式（均衡/高性能(狂野)/自定义）** | `ui.CompetitionDetailActivity`、`windowmanager/newbox/PerformancePanelView` | `game_mode_balance` / `game_mode_wild` / `game_mode_fps_graphic` RadioGroup（`PerformancePanelView.java:73-136`） |
| 帧率曲线 | `framerate/FrameRateView`、`framerate/FrameRateViewController`、`gbservices/FrameRateDataService`、`gameturbo/active/IFrameRateDataService` | 实时 FPS 采集 |
| 游戏模式（画面比例：填充/等比） | `gamemode/GameModeSettingsActivity`、`shoulderkey/widget/GbGameModeView`、`gamebooster/assets?` | `gb_game_mode_fill` / `gb_game_mode_ratio` |
| 免打扰 / 功能屏蔽 | `ui.FunctionShieldSettingsActivity` | `function_shield_title` |
| 快捷回复 | `ui.QuickReplySettingsActivity`、`service/NotificationListener`（`BIND_NOTIFICATION_LISTENER_SERVICE`，`singleUser`） | 游戏中横幅回复 |
| 防误触 | `gbservices/AntiMsgAccessibilityService`（`BIND_ACCESSIBILITY_SERVICE`，`xml/accessibility_gamebooster_service_config`） | **无障碍服务**，非触摸屏蔽 |
| **肩键** | `shoulderkey/ShoulderKeyLightEffectActivity`、`ShoulderKeyGuideView`/`ShoulderKeyNewGuideView`/`TbGuideView`、`shoulderkey/widget/{ShoulderNewConfigLayout,ShoulderSsConfigLayout}` | 灯效 + 映射配置；SP `shoulder_key_settings` |
| 变声 | `voicechanger/{SettingsView,VoiceModel,VoiceTipsModel,LoginActivity}`、`service/MiuiVoiceChangeCallback`、`gameturbo/active/IVoiceChangeCallback` | 需登录（`LoginActivity`） |
| **美颜 / 视频通话增强** | `beauty.BeautySettingsActivity`、`beauty.PrivacyCameraGlobalSettingsActivity`、`beauty/ConversationSettingsFragment` | 见 §4.5 |
| 录屏 / 精彩时刻 | `ui.WonderfulMomentActivity`、`GameVideoActivity`、`GameVideoPlayActivity`、`service/VideoToolBoxService`、`service/IVideoToolBox` | 视频工具箱 |
| 主播/悬浮窗 | `service.DockWindowManagerService`（`:ui` 进程）、`ui.WindowCallActivity` | |
| 网络加速 | `ui.WifiBoosterDetail`（迅游）、`xunyou/XunyouAlertActivity`、`service/IGameBoosterTelecomeManager` | `IMiuiVpnManageService.getSettingEx("xunyou","xunyou_wifi_accel_switch")` |
| 预下载 | `predownload/*`、`i7/i.java`（`android.intent.action.joyose.predownload.{COMPLETE,UPDATE}`，权限 `com.xiaomi.joyose.permission.predownload`） | |
| 游戏卸载管理 | `ui.VerticalGameUninstallActivity`、`storage/{GameUninstallFragment,UninstallAppsManager}` | |
| 全景/多窗口 | `mutiwindow/{FreeformWindowService,a,b}`、`ui.GameBoxAlertActivity` | |
| AI 助手（GameGPT） | `aihelper/*`、`GameToolBox2BuryPoint` | `adv.sec.miui.com/gameTurbo/migamegpt/game-ai/gt-setting-info` |

Provider：`provider/GameBoosterProvider`（`com.miui.securitycenter.permission.GAMEBOOSTER_PROVIDER`）。

### 4.3 游戏识别（观测）

`assets/top_200_games.json` = `{"packageNames":[...]}`，**实际 607 条**（名字是历史遗留），首条 `com.tencent.tmgp.sgame`。
读取方 4 处：
- `a8/k0.java:97` — `g.a(ctx,"top_200_games.json")`
- `f7/c.java:73`
- `o6/a.java:43` — `TopGameUtils`，读取失败则 `Log "joyose not support!!!"`（`:95`）
- `com/miui/gamebooster/ui/BoosterFragment.java:497`
- `y3/a.java:285` — `VideoToolBox` 用途

游戏列表**动态化**：`a8/k0.java:161` 和 `f7/c.java:112` 把本机包名列表 POST 到 `https://adv.sec.miui.com/game/pkg` 拉取"哪些是游戏/游戏标签"；`a8/x0.java:88` 是另一条 `game/pkg` 路径。`a8/k1.java:56` 拉 `game/popGames`。

前台判定：`com.miui.gamebooster.mutiwindow.b` 提供 `ForegroundInfo` 监听（`InterfaceC0181b.onForegroundInfoChanged`，`getId()→f7.e`），`AppPredictService.java:91-141`、`BeautyService` 均复用它（`BeautyService.java:139-183,466-472`）。

### 4.4 加速开关的真实落地（**关键，观测**）

**本地只改配置/广播；GPU/CPU 调频全部外包给 Joyose。**

| 层 | 证据 |
|---|---|
| `ServiceManager.getService("xiaomi.joyose")` → `com.xiaomi.joyose.IJoyoseInterface` | `a7/n.java:162` |
| `IGPUTunerInterface`（`com.xiaomi.joyose.securitycenter`，22 个 transact） | `gamebooster/service/w.java:136-147`、`utils/GameBoxVisionEnhanceUtils.java:120`、`a8/f2.java:39` |
| 实际调用：`checkSupportGpuTuner()`、`checkSupportUgd()`、`getProfiles()`、`deleteProfile()` | `w.java:138-147` |
| 绑定方式：`Intent.setPackage("com.xiaomi.joyose")` + bindService | `w.java:428`、`GameBoxVisionEnhanceUtils.java:493`、`a8/f2.java:102`、`i6/a.java:145` |
| 能力握手：`f1.m("com.xiaomi.joyose").getInt("joyose_game_turbo_api_version")`（ContentProvider meta） | `GameBoxVisionEnhanceUtils.java:318-320` |
| `IGunSightInterface`（画面增强/超分） | `com/xiaomi/joyose/securitycenter/IGunSightInterface.java`、`i6/a.java:17,145` |
| 4D 振感：`JoyoseManager.getGameMotorAppList()` 反射 | `o6/b.java:27` |
| 温控反馈：`com.miui.powerkeeper.feedbackcontrol.IFeedbackControl` | `gamebooster/service/w.java:28` |
| 屏幕效果：`android.provider.MiuiSettings$ScreenEffect.GAME_MODE` 反射取值，随后 `resetGameMode` | `gamebooster/service/w.java:501-509` |
| 刷新率/显示：`DisplayFeatureManager.setScreenEffect(24, fps)` | `be/g.java`（省电侧共用） |
| 亮度：`gamebooster/brightness/*`（4 类） | — |

**结论**：`IGPUTunerInterface` 的 `getProfiles()` 返回 `List<GPUProfile>`（`com/xiaomi/joyose/securitycenter/GPUProfile.java`），即"每款游戏一套 GPU 档位配置"。**具体 GPU 频率/电压由 Joyose 进程决定，本 APK 只下发/选择 profile → `未知` 具体节点。**

### 4.5 `BeautyService`（美颜/视频通话增强）实现链路（观测）

`com/miui/gamebooster/beauty/BeautyService.java`（继承 `service/u`，即 `u` 是通用「前台监听型 Service」基类）：

1. **不处理摄像头/音频帧**。它只做**场景判定 + 悬浮窗（Dock）切换**。
2. 前台判定三路并集：
   - `miui.process.ProcessManager.registerActivityChangeListener(List pkg, List cls, IActivityChangeListener)` 反射（`:117` region `Y()`，`BeautyService.java` 的 `Y()` 方法）→ `IActivityChangeListener.onActivityChanged(pre, cur)`
   - `ActivityTaskManager.getService().registerTaskStackListener(ITaskStackListener)` 反射（`c0()`）
   - `com.miui.gamebooster.mutiwindow.b` 的 `ForegroundInfo` 回调（`a0()`）
   - 兜底 `ActivityManager.getRunningTasks(1)`（`P()`、`p0()`）
3. 判定"是否支持美颜"：`w5/f.p().c(isFrontCameraOpen, pkg, cls)`；分屏场景走 `T()`（遍历 `getRunningTasks` 过滤 `MiuiFreeFormManager.getAllFreeFormStackInfosOnDisplay`）
4. 命中 → `bindService(DockWindowManagerService, action="com.miui.gamebooster.service.GameBoxService")`（`:513-517`），拿到 `IGameBoosterWindow` 后调 `D4(5, false, pkg, cls, 0)` 进"会话模式"，退出调 `G4(5)`（`M()`/`N()`）
5. 免打扰名单 `K`（静态，`:239-251`）：`com.miui.screenrecorder`、`com.lbe.security.miui`、`com.milink.service`、`com.xiaomi.miplay_client`、`com.xiaomi.misubscreenui`、`com.xiaomi.aiasst.vision`、`com.miui.vpnsdkmanager`、`com.xiaomi.migameservice`、`com.xiaomi.gamecenter.sdk.service`、`com.google.android.permissioncontroller`；特殊类名映射 `L`：`com.milink.service → com.xiaomi.dist.camera.view.CameraPickerActivity`
6. 真正的图像处理在 `DockWindowManagerService`（`:ui` 进程）与 `j8/u.java`（`VideoEffectUtils`，持有 `IPowerKeeper`，`j8/u.java:54,79`）
7. 美颜支持列表（assets，规模观测）：`beauty_light_default_support_list.json`(**31**)、`beauty_face_support_activity_list.json`(**4**)、`gesture_effect_support_list.json`(**28**)、`pickup_support_activity_list.json`(**18**) / `pickup_support_activity_list_phone.json`(**19**)、`privacy_all_support_activity_list.json`、`privacy_single_support_activity_list.json`、`conversation_record_activity_list.json`、`protrait_support_activity_list.json`、`aisubtitle_support_activity_list.json`；加载器 `w5/f.java:501,507,511,524`（`pref_light_support_apps` / `pref_gesture_effect_apps_list`）

### 4.6 `com.miui.gameturbo`（6）/ `com.miui.gamecenter`（2）

- `gameturbo` 全是 **AIDL 回调**，供 `:ui` 进程 ↔ 主进程通信：`active/{IActiveManager,IActiveCallback,IFrameRateDataService,IFrameRateDataCallback,IVoiceChangeCallback,IWebPanelCallback}`
- `gamecenter`：`ui/GameCenterMainView.java`、`casual_game/GameBoxRangeTextView.java`（休闲小游戏入口，内容来自 `adv.sec.miui.com/gameTurbo/*`）

### 4.7 游戏相关配置资产

| 资产 | 用途 | 证据 |
|---|---|---|
| `top_200_games.json`(19.9KB, 607 pkgs) | 游戏包名白名单（离线兜底） | §4.3 |
| `performance_mode/data.json`(116KB) + `img_0..7.png` | 性能模式介绍页/动效资源 | 无代码引用 → **未知**（推断为纯资源，供 `gtb_guide_style1.json` 类引导页使用） |
| `redmi_performance_mode/Ai-Model.json`(119KB) | 同上（Redmi 版本） | **未知** |
| `game_turbo_box_mode_change/{blue,red}_light.json`(7.2KB) | 游戏模式切换灯效（Lottie） | — |
| `shoulder_key/*`（`enter_guide.json` 63KB、`guide_{next,single,trigger}.zip`、`settings_guide.json`、`using_guide.json`、`img_*.webp`） | 肩键引导/灯效 | `ShoulderKeyGuideView.java:104-107`（`setAnimation("shoulder_key/settings_guide.json")`）、`ShoulderKeyNewGuideView.java:165-168` |
| `tb_guide_style1.json`(44.7KB) | 游戏工具箱引导样式 | — |
| `game_box_barrage_v3_support_apps.json`(365B, 14 项) | 弹幕 v3 支持应用 | `n7/a.java:63` |
| `ultraclear_support_activity_list.json`(5) | 超清画质支持页 | — |
| `support_split_scene_activities.json`(1) | 分屏场景支持 | — |
| `vtb_default_support_activity_list.json`(33 项)/`vtb_default_support_list` | 视频工具箱默认支持 | `f7/d.java:101`、`i8/c.java:286`、`y3/a.java:296` |
| `xunyou_voice{,_en,_tw}.json` | 迅游加速数据 | — |
| `service_num.json`(4.5KB) | 客服号码 | — |

---

## 5. `com.miui.autotask`（125 类）+ `powercenter.autotask`（22 类）—— 自动任务

### 5.1 两套实现（观测）

| 包 | 性质 |
|---|---|
| `com.miui.autotask` | **新版**（Kotlin，`ck.m` 非空断言风格），125 类，`androidx`+`Preference` |
| `com.miui.powercenter.autotask` | **旧版**（Java），22 类，Manifest 仍保留 4 个 Activity + Provider |

两者 Provider authority 不同：新版 `com.miui.autotask.provider.auto_task`（`permission=com.miui.autotask.permission.CONTENTPROVIDER`）+ `…auto_task_service`（**无 permission，exported=true**）；旧版 `com.miui.powercenter.autotask`（`permission=com.miui.powercenter.permission.POWER_DATA`）。

### 5.2 数据模型（观测）

`bean/Task`（任务）= `List<Condition>` + `List<Result>`；`bean/Condition` = `{instanceId, typeId, jsonStr}`（Parcelable，`Condition.java:8-17`）。即**规则引擎是"typeId 字符串 + JSON 参数"的插件式结构**，无固定 schema 表。

其他 bean：`SyncTaskModel`（云同步）、`AppInfoBean`、`r.java`(6.5KB, 条件/结果反序列化)、`s.java`、`u.java`(5KB)。

### 5.3 条件全集（`taskitem/*ConditionItem`，观测）

| 条件类 | 用户语义 |
|---|---|
| `CustomTimeConditionItem` | 自定义时间（时段/星期） |
| `ChargeConditionItem` / `BatteryConditionItem` | 开始充电；电量低于/高于 N% |
| `InCallConditionItem` | 通话中 |
| `AppUseConditionItem` / `StartActivityConditionItem` | 打开某应用 / 进入某页面 |
| `LeaveAppConditionItem` | 离开某应用 |
| `LocationConditionItem` / `ToSomewhereConditionItem` / `LeaveConditionItem` / `AddressTaskItem` | 到达某地 / 离开某地（地理围栏，`common/PolarisGeofenceReceiver`） |
| `WlanConditionItem` / `WlanConnectSpecifiedConditionItem` / `WlanDisconnectSpecifiedConditionItem` / `WlanItem` | 连/断指定 WiFi |
| `BluetoothConditionItem` / `BluetoothDeviceConditionItem` / `BluetoothConnectDeviceConditionItem` / `BluetoothDisconnectDeviceConditionItem` / `BluetoothTaskItem` | 蓝牙连/断指定设备 |
| `HeadsetConditionItem` | 插入耳机 |
| `HotspotConditionItem` / `HotspotItem` | 热点开启 |
| `MeetingConditionItem` | 会议中（日历） |
| `MuteConditionItem` / `MuteItem` | 静音状态 |
| `NfcConditionItem` | NFC 开关状态 |
| `LockScreenConditionItem` / `LockScreenItem` | 锁屏 |
| `AbsorbedConditionItem` / `DefaultConditionItem` | 组合/默认 |

### 5.4 动作全集（`taskitem/*ResultItem`，观测）

| 动作类 | 用户语义 |
|---|---|
| `WlanResultItem` / `BluetoothResultItem` / `NfcResultItem` / `HotspotResultItem` | 开/关 WiFi、蓝牙、NFC、热点 |
| `AirplaneResultItem` | 飞行模式 |
| `DisturbResultItem` | 免打扰 |
| `MuteResultItem` / `DialToneResultItem` | 静音 / 按键音 |
| `AdjustVolumeResultItem` | 调节音量 |
| `ScreenBrightnessResultItem` / `AutoBrightnessResultItem` | 亮度 / 自动亮度 |
| `DarkResultItem` / `EyeCareResultItem` / `TypefaceResultItem` / `ScreenDisplayResultItem` | 深色模式 / 护眼 / 字体（粗细+大小，`view/FontWeightAdjustView`+`FontSizeAdjustView`）/ 显示（分辨率?） |
| `RotateOffResultItem` | 关自动旋转 |
| `SaveBatteryResultItem` | 开省电模式（联动 §2） |
| `TouchResultItem` | 触控模式（手套/灵敏度?） |
| `TwinkleResultItem` / `FlashlightResultItem` | 闪烁提醒 / 手电筒 |
| `DriveResultItem` | 驾驶模式 |
| `LunchAppItem` | 启动应用（`R.string.auto_task_open_app`，`:81`） |
| `LocationResultItem` | 位置相关动作 |
| `StartActivityResultItem` | 打开指定 Activity |
| `NetworkResultItem` / `SwitchResultItem` / `SwitchTypeItem` | 网络开关 / 通用开关 |
| `AbsorbedResultItem` / `DefaultResultItem` / `DefaultTaskItem` | 组合/默认 |
| `xiaoai/XiaoaiTaskItem` + `XiaoaiCustomTimeCondition` | 小爱同学语音触发 |

`condition`、`result` 的整型类型码由各 Item 的 `b()` / `c()` 返回（`TaskItem.java:26-28`），`AbsorbedResultItem.b()` 用组合逻辑返回 0/1（`AbsorbedResultItem.java:98-103`）。

### 5.5 触发与执行（观测）

- 时间触发：`PowerCenterProvider`/`AlarmReceiver`/`DateResetReceiver`（`process=…remote`）+ 新版 `activity/AddTimeConditionActivity` 与 `AlarmManager`
- 地理围栏：`common/PolarisGeofenceReceiver`（exported，`process=…remote`）
- 事件触发：各 `*ConditionItem` 内部注册 BroadcastReceiver / ContentObserver（模式见 `AbsorbedConditionItem`、`LockScreenConditionItem`）
- 执行：`AutoTaskIntentService`（旧）/ 新版在 `AutoTaskProvider` 与 `TaskManagerActivity` 的后台
- 云同步：`bean/SyncTaskModel`

**未知**：新版是否真的取代旧版、是否存在灰度开关；两者是否共享同一 Provider 数据。

---

## 6. `com.miui.apppredict`（25 类）—— 应用预测（端侧 AI 主战场）

### 6.1 组件与暴露面（观测，Manifest）

| 组件 | 导出 | 权限 | 进程 |
|---|---|---|---|
| `service.AppPredictService` | false | — | `…remote` |
| **`service.AiService`** | **true** | **`miui.permission.APP_PREDICT_VERIFY`** | `…remote` |
| `db.WidgetBlackListContentProvider`（`com.miui.securitycenter.widget.black.list.provider`） | false | — | — |
| **`widget.AppPredictWidget2x4`**（receiver，`app_predict_widget_info_2x4`） | **true** | — | `…remote` |
| `activity.AppClassificationActivity`（应用分类） | false | — | — |
| `activity.FolderSearchActivity`（文件夹搜索） | false | — | — |
| `activity.WidgetSettingActivity` / `WidgetBlackListSettingActivity` | false | — | — |
| `activity.AppSuggestPrivacyActivity` | false | — | — |

`AiService` 暴露的 AIDL `IAppPredict`（3 方法，`AiService.java:26-83`）：
- `c1(String label)` → `c.d(ctx).h(str)`
- `k3(String label)` → `c.d(ctx).c(str)`
- `n1(String label, IAppPredictCallBack cb)` → 异步 `cb.j5(d.a(f.d()))`，**label 必须已在 `p3.c` 注册**，否则 `Log "label = X is not register"`

`AppPredictService.f extends IPredictNextApp.Stub.R2(int n)` → 返回 top-n 预测包名（`AppPredictService.java:238-245`）。这是**桌面（`com.miui.home`）取预测列表的通道**。

`AppPredictWidget2x4` = 2×4 桌面 Widget；另有 `app_behavior_widget_info_2x2`（属另一模块）。

### 6.2 推荐引擎三段式（观测）

`o3/c.AppPredictManager`（单例，`o3/c.java`）：
- `d()`：`k.c().b()`（UsageStats）→ 过滤有 launcher intent 且 `totalTimeInForeground>0` → `PredictApp(pkg, lastTimeUsed, lastTimeUsed+60s, wifi)`（`c.java:94-113`）——**特征：包名 + 时间戳 + 当前 WiFi**
- `l()`：`ea.b.a(recentApps)` → `ResultWithMetrics{mnnapps, bayesapps, pbbapps, apps}`
- `j()`：初始化；`r(trainPlanBean)`：训练

`fa/a`（`ea.b` 实现）持有 **5 个子模型**：
| 字段 | 类型 | 作用 |
|---|---|---|
| `f25892d` | `ia.j` | **MNN 模型**（`rec_model_20230425.mnn`） |
| `f25889a` | `la.a` | PBB（Personalized Bayesian Bandit） |
| `f25890b` | `ha.a` | 默认/冷启动列表 |
| `f25891c` | `ka.b` | 贝叶斯统计 → `ga.b` |
| `f25893e` | `ia.b` | 另一路排序器 |
| `f25894f` | `ia.f` | 另一路 |
| `f25895g` | String | files 目录 |

融合（`fa/a.java:60-89` → `ia/d.c(...)`）：`d.c(mnn, mnn2, bayes, pbb)` 对 4 条列表做 **倒数排名加权** `score[pkg] += 1/(rank+1)`（`ia/d.java:62-88`，`e()` 实现，`Math.min(size,50)` 截断），全局降序。

### 6.3 端侧 AI 细节（**重点**）

#### (a) 模型文件与加载

| 项 | 观测 |
|---|---|
| 模型 | `assets/rec_model_20230425.mnn`，810,908 B，头部 `1c 00 00 00 18 00 1c 00` —— **FlatBuffers 起点 + MNN Net 结构**（`MNNNet_NET`），无自定义加密 |
| 落地路径 | `<filesDir>/rec_model_20230425.mnn`（首次从 assets 拷贝，`ia/d.d()`，`ia/j.java:105-113`） |
| 训练后权重 | `<filesDir>/apprecmodel/checkpoint_N`（`ia/j.java:83-101`），启动时**若目录非空则优先加载最新 checkpoint** |
| 加载类 | `ia/j` 构造函数（`ia/j.java:78-145`） |
| JNI 类 | `com.miui.mlkit.mobilerec.ranking.mnn.MNNNetNative` / `MNNTrainNative` |
| .so | `libMNN`、`libMNN_Express`、`libMNNTrain`、`libmnncore`（`MNNNetNative.java:5-9` static 块 `System.loadLibrary`） |

#### (b) 网络结构（由 tensor 名 + reshape 反推，观测）

```
inputs:  sparse_inputs_1 … sparse_inputs_6   shape [batch, 30]  int32
         sparse_inputs_time                  shape [batch, 1]   int32
output:  linear_9.tmp_1                      shape [batch, 2]   float
```

`MNNNetNative.nativeCreateSession(net, FWD=0/CPU, threads=4, null, null)`（`ia/j.java:131`），`ja/a.FORWARD_CPU=0`（另有 `OPENCL=3`/`AUTO=4`/`OPENGL=6`/`VULKAN=7` 枚举，未启用）。

#### (c) 输入特征语义（**这是"输入特征是什么"的答案**）

`ia/d.f(String pkg, HashMap<String,Integer> vocab)`（`ia/d.java:184-199`）：

```java
int[] v = new int[30];
String s = pkg.toLowerCase(Locale.ROOT);
for (i=0; i<min(s.length(),30); i++)
    v[i] = vocab.getOrDefault(String.valueOf(s.charAt(i)), vocab.get("<unk>"));
```

即 **"包名前 30 个字符的 char-id 序列"（右补 0=`<pad>`）**，词表就是 `assets/character.json`（600B，`<pad>=0, <unk>=1, a=2, b=3, …`，由 `ea/c.a()` 解析，`ea/c.java:44-58`）。

`sparse_inputs_1..5` = **历史最近 5 个前台应用**（t-1 … t-5）的 char-id；
`sparse_inputs_6` = **候选应用** 的 char-id；
`sparse_inputs_time` = `Calendar.get(HOUR_OF_DAY)`（0-23，`ia/j.java:230-233`）。
`batch = len(候选列表)`，`sparse_inputs_time` 被 `Arrays.fill(iArr8, hour)` 复制到整批。

（`ia/d.g(ts)` 提供 `hour*6 + minute/10` 的 144 桶时间编码，`ia/d.a(ts)` 提供周一=0 的星期重映射 —— **两者均未被 `ia/j.e()` 使用** → 备用特征，`未知` 是否在未来模型中启用。）

#### (d) 输出与后处理（观测）

`nativeTensorGetData` 得到 `float[2*batch]`，逐候选做 `[2]` 的 softmax（`ia/j.java:275-296`），取 `P(class=1)` = "该应用会被下一个打开"的概率，按概率降序即为 `mnnapps`。
之后与贝叶斯/PBB 结果做乘法重加权（`ia/j.java:309-345`：`score * count / totalCount`，NaN → 0）。

**语义结论（推断）**：这是一个**二分类 next-app predictor**，不是序列生成模型；`linear_9` 是最后一层全连接。

#### (e) 是否有 on-device 训练 —— **有，且带自动回滚**（观测，**重要**）

`MNNTrainNative`（2 个 native 方法）：
```java
float[] nativeGetRecModelPredictScore(String modelPath, int[][] x6, int[] y);        // 只评估
float[] nativeTrainRecModel(String modelPath, String savePath,
        int[][] x6_train, int[] y_train,          // 训练集：5 历史 + 候选 → label
        int[][] x6_test,  int[] y_test,           // 测试集
        int epochs, float lr, int workers, int trainBatch, int testBatch);           // 返回 [loss]
```
`ia/j.f(list, TrainPlanBean)` 完整流程（`ia/j.java:356-540`）：
1. 只保留 `dict`（character.json 词表）里出现过的包 → 否则丢弃
2. **样本 < 50 → 直接返回**（`"trainData size is not enough, min size is 50"`，`:372-375`）
3. 滑窗 `size-6` 个样本，20% 随机抽为测试集（`(int)(size*0.2)`）
4. label 生成：50% 概率用真实下一个应用（label=1），50% 概率用 `random.nextInt(5000)` 的随机负样本（label=0）→ **负采样**
5. 先 `nativeGetRecModelPredictScore` 得到**基线 loss**
6. `nativeTrainRecModel` 输出到 `<filesDir>/apprecmodel/checkpoint_{n+1}`
7. **`new_loss > baseline_loss` → 删除新 checkpoint 并置 `is_good_than_before=false`**（`:472-484` / `:525-537`）
8. 返回 JSON：`train_count / current_score / new_score / train_data_size / test_data_size / train_use_time / is_good_than_before / task_epoch`

**训练触发门槛（观测，`AppPredictService.l()`，`:313-335`）**：
- 云端 `TrainPlanBean.canTrain()`（`train_on == 1`）
- 用户开关 `sp_apppredict.getBoolean("train_enable", true)`
- **电量 ≥ 20%**（`BatteryManager.getIntProperty(4)`）
- 前台应用不是黑名单（`p3.g.f32962c`）
- 距上次训练 ≥ `TrainPlanBean.period` 天（`sp_apppredict "key_last_train_time"`，`AppPredictService.java:476-481`）
- 具体执行时刻由云端 `start_time`（float 小时）计算，`AlarmManager.set(RTC_WAKEUP=0, …)`（`:482-495`）

`TrainPlanBean` 默认超参（观测，`TrainPlanBean.java:39-46`）：`epochs=10, lr=1e-4, batch_size=128, test_batch_size=1, num_workers=4, period=7`；云端字段 `base_model/model_version/all_train_count/min_data_size/max_duration/task_name/type`。

#### (f) TFLite 是否参与预测？—— **不参与**

`libtensorflowlite_jni` 存在于 `lib/`，但 **app 预测链路只用 MNN**。TFLite 属反垃圾/短信分类（`lj/*` + `mj.a` + `assets/model`+`assets/dict`）。证据：`grep MNNNetNative|Interpreter` 无 `Interpreter` 命中；`lj.c.f29439b={"dict","model","pattern","phish"}`。

#### (g) 唤醒/刷新时机（观测）

| 时机 | 动作 | 证据 |
|---|---|---|
| `ACTION_USER_PRESENT` / `SCREEN_ON` | 立即 predict + 5s 后再刷新列表广播 | `AppPredictService.java:275-280` |
| 前台应用切换（`ForegroundInfo`） | 若上次切换 <60s 则取消排队；否则延迟 60s predict | `:105-140` |
| 包安装/卸载 | 重建索引 | `:168-172` |
| 壁纸变更 | 重建 | `:294-297` |
| 深色模式/图标主题变更（`uiMode` / `themeChangedFlags&8`） | 重建 | `:531-556` |
| 桌面加载完成（`com.miui.home.intent.action.LOADING_FINISHED`） | 重建 | `:223-226` |
| 隐私应用变更（`pe.a` ContentObserver） | 重建 | `:253-261` |
| 广播 `com.miui.action.UPDATE_PREDICT_LIST(_EXTERNAL)` | 通知桌面刷新 | `:454-461` |

### 6.4 数据与持久化（观测）

| 存储 | 键/表 | 证据 |
|---|---|---|
| SP `sp_apppredict` | `train_enable`, `key_last_train_time` | `AppPredictService.java:563,305,476` |
| SP `n.f32972b` | `key_last_train_time`（同一语义的另一个入口） | `:305` |
| `train_configuration` | 云端下发的 JSON 原串 | `o3/c.java:78` |
| 文件 | `<filesDir>/rec_model_20230425.mnn`、`<filesDir>/apprecmodel/checkpoint_N` | `ia/j.java:83-113` |
| ContentProvider | `com.miui.securitycenter.widget.black.list.provider` | Manifest |
| 训练样本 | `RespImpl.saveTrainData/getTrainData`（`com.miui.apppredict.bean.RespImpl`） | `o3/c.java:169,273` |

---

## 7. `com.miui.maml`（221 类）—— 通用卡片渲染引擎 / 远程 UI 下发（**重点专项**）

### 7.1 它是什么（观测）

`com.miui.maml`（**M**IUI **A**dvanced **M**arkup **L**anguage）是一个**数据驱动的矢量 UI 运行时**：解析 XML（`manifest.xml` + 元素树）→ 构建 `ScreenElement` 树 → 自绘到 `Canvas`/GL。

本 APK 内的能力规模（类数）：
`elements`(58)、`animation`(42)、`util`(37)、`data`(32)、`folme`(8)、`shader`(6)、`component`(4)、`widget`(1)，加顶层 ~40 个类（其中 **`ActionCommand.java` 单文件 172KB**）。

顶层关键类：`ScreenElementRoot`(70KB)、`ActionCommand`(172KB)、`NotifierManager`(23KB)、`RendererController`、`RenderThread`、`ResourceManager`、`ResourceLoader`、`ScreenElementRootFactory`、`MamlDrawable`/`FancyDrawable`/`AnimatingDrawable`、`MiAdvancedView`、`MultipleRenderable`、`SoundManager`、`StylesManager`、`CommandTrigger(s)`、`VibrateCommand`、`MamlDisplayEventReceiver`、`LifecycleResourceManager`、`FramerateTokenList`

`AndroidManifest.xml` 声明 `<meta-data android:name="com.miui.maml.sdk" android:value="true"/>`。

本 APK 内置的 MAML 卡片资源：`assets/maml/{antispam, na_nosim, na_nosim_dark}/{call,sms}/` + `manifest.xml`(4199B) + `bg.png/bgmask.png/board.png/shape.png`。

### 7.2 能力边界（**结论**）

| 能力 | 是否支持 | 证据 |
|---|---|---|
| **脚本执行** | ✅ **是**（表达式语言 + 反射） | 见 §7.3 |
| **网络加载 UI** | ✅ **是**（WebView + `issueExternCommand` 外抛给宿主） | `WebViewScreenElement.java:298-310`、`ScreenElementRoot.java:1072-1092` |
| **动态下发 UI** | ✅ **是**（XML 来自 `ZipResourceLoader`/`FolderResourceLoader`/`AssetsResourceLoader`；`ResourceLoader` 可指向任意目录/zip，配合 `CloudCoder` 的签名/加密封装） | `ResourceLoader.java`、`util/{Zip,Folder,Assets,FancyIcon}ResourceLoader.java`、`util/net/CloudCoder.java` |
| **WebView 内嵌** | ✅ JS 开启 + `addJavascriptInterface(new MamlInterface(), "maml")` | `WebViewScreenElement.java:132,138` |
| **跨进程 intent 触发** | ✅ `IntentCommand` | `ActionCommand.java:1928` |
| **反射调用任意方法/构造器** | ✅ `MethodCommand` / `FieldCommand`，但构造器受 `getCapability(4)` 门控 | `ActionCommand.java:2292-2400`、`:1096` |
| **读写 ContentProvider** | ✅ `ContentProviderBinder` | `data/ContentProviderBinder.java` |
| **收广播** | ✅ `BroadcastBinder` | `data/BroadcastBinder.java` |
| **读文件/传感器/音频/音量/电池** | ✅ `FileBinder`/`SensorBinder`/`MusicController`/`VolumeVariableUpdater`/`BatteryVariableUpdater` | `data/*`、`elements/MusicController.java` |
| **持久化变量** | ✅ `CAPABILITY_VAR_PERSISTENCE=2` | `ScreenElementRoot.java:68` |
| **默认权限姿态** | ⚠️ **`mCapability = -1`（CAPABILITY_ALL）= 全开** | `ScreenElementRoot.java:505, 992`；`setCapability(int, boolean)` 才收窄（`:1480-1489`） |

### 7.3 "脚本"能力的确切形态（观测）

MAML 有一套**图灵完备度较高**的表达式 + 命令体系（不是 JS，是自研 DSL）：

**表达式层**（`data/`）：
- `Expression` / `RootExpression` / `ExpressionVisitor` — AST 求值
- `BaseFunctions` / `StringFunctions` / `JsonFunctions` / `FormatFunctions` / `DateFunctions` / `FunctionsLoader` — 内建函数库，按 XML 的 `<Function>` 声明延迟加载
- `Variables` / `Variable` / `IndexedVariable` / `VariableBinder(Manager)` / `VariableUpdater(Manager)` — 变量系统，支持 `@var`、`#index`、表达式插值
- `JSONPath`（`util/JSONPath.java`）— 对绑定到的 JSON 数据做路径查询

**命令层**（`ActionCommand` 内部类，共 20+ `TAG_NAME`，观测自 `grep TAG_NAME`）：
`ActionCommand`、`AnimConfigCommand`、`AnimStateCommand`、`AnimationCommand`、`EaseTypeCommand`、`ExternCommand`、`FieldCommand`、`FolmeCommand`、`FrameRateCommand`、`FunctionCommand`、`GraphicsCommand`、`IfCommand`、`IntentCommand`（含 `Fallback`）、`JsonObjectCommand`、`LoopCommand`、`MethodCommand`、`MultiCommand`/`GroupCommand`、`PbrCommand`、`SensorCommand`、`SoundCommand`、`TickListenerCommand`、`VariableCommand`、`BinderCommand`、`VideoCommand`

⇒ 具备 **`if` 分支 + `loop` 循环 + 可变状态 + 反射调用 + intent 派发 + 外部命令**，等价于一个受限的嵌入式脚本 VM。

`ExternCommand` 的执行语义：`root.issueExternCommand(command, numPara, strPara)`（`ActionCommand.java:1086-1090` → `ScreenElementRoot.java:1072-1092`），最终转给 `MamlView` / `MamlSurface` 的宿主回调 —— 宿主（SystemUI/锁屏/桌面）决定能做什么，因此**能力上限 = 宿主实现 + capability 位**。

### 7.4 云端内容的安全封装（观测）

`util/net/CloudCoder.java`：
- `generateSignature(method, url, params, security)` = `Base64(SHA1( METHOD & path & k=v(排序) & security ))`（`:26-56`）
- `newAESCipher(keyBase64, mode)` = **`AES/CBC/PKCS5Padding`，硬编码 IV `"0102030405060708"`**（`:66-77`）

配套异常类型：`AccessDeniedException` / `AuthenticationFailureException` / `CipherException` / `InvalidResponseException`（`util/net/`）→ 说明 MAML 资源是**带签名+可选 AES 加密的云端下发包**。

**风险面（推断）**：旁路 `RC4` 常量声明在 `CloudCoder.java:39` 但本类未使用（可能是历史/未完整移植）；IV 固定且硬编码，密钥走 `security` 参数 → 若同一签名密钥复用，加密强度受限于密钥分发而非密码学本身。

### 7.5 渲染与性能

`RenderThread` / `RendererController` / `RenderUpdater` / `RenderVsyncUpdater` / `FramerateTokenList` / `elements/FramerateController` — 独立渲染线程 + vsync 对齐 + 分元素帧率预算；`ScreenElementRoot.java` 70KB 主要是布局/变量/命令分发/生命周期。
`elements/` 覆盖：`Text/Image/ImageNumber/DateTime/Circle/Rectangle/Arc/Ellipse/Line/Curve/CanvasDrawer/Paint/AdvancedSlider/Button/List/Mirror/Layer/ElementGroup/VariableArray/WebView/Video/Window/ViewHolder/GLLayer/PhysicallyBasedRendering/MusicControl/SpectrumVisualizer/Timepanel`。
`folme`(8) = MIUI 物理动画引擎绑定；`shader`(6) = GLSL 支持。

---

## 8. 其它小模块

| 包 | 类数 | 内容（观测） |
|---|---|---|
| `com.miui.dock` | 20 | 车载/ dock 模式；资源 `assets/dock_settings_{fold,phone_sidebar,phone_unsidebar,…}.json`(27-64KB)、`dock_tips.zip`(211KB)、`phone_wireless_*.json`(72KB)、`flip_wireless_*.json`(127-139KB)、`normal-{light,dark}(-miui15).json`(18-37KB)、`unfold-*.json`、`external{Light,Dark}.json`、`popNotice*.json` — 全部为 MAML/dock 卡片布局资源 |
| `com.miui.fastplayer` | 2 | `FastPlayer.java` + `a.java`，对应 `libfastplayer.so`；媒体快速播放（推断：视频工具箱/精彩时刻预览） |
| `com.miui.ai` | 1 | `service/OperationListCollectService` — 收集用户操作序列（为 apppredict 供训练数据，**推断**） |
| `com.miui.mlkit` | 5 | `mobilerec/bean/{PredictApp,ResultWithMetrics,TrainPlanBean}` + `ranking/mnn/{MNNNetNative,MNNTrainNative}`，见 §6.3 |
| `com.miui.monthreport` | 4 | `month_report.db`（`SQLiteOpenHelper`，version 2），表含 `eventType/eventId/eventTime/moduleName`（`MonthReportProvider.insert`）；`b.java` 有 `Base64`+`URLEncoder`+`UUID`+`JSONObject` → 上报月度报告（含电量相关，因 `moduleName` 可设为 `monthReport`/电量模块）。`a.java:37` `super(ctx,"month_report.db",null,2)` |

---

## 9. 云端接口（全部在 DEX 字符串中命中，且均有代码调用点）

| URL | 用途 | 调用点 |
|---|---|---|
| `https://adv.sec.miui.com/mnntrain/taskdata` → 实为 `https://api.sec.miui.com/mnntrain/taskdata` | **训练任务计划下发**（POST，appId `5cdd8678-cddf-4269-ab73-48017445bba3`，埋点 `ai_request_task`），返回 `BaseResult<TrainPlanBean>` | `eg/j.java:876`（`u(String)`）；调用者 `o3/c.java:200-202`(`h()`) → `AppPredictService.v():467` |
| `https://adv.sec.miui.com/game/pkg` | **游戏识别/标签**（POST 本机包名列表） | `a8/k0.java:161`、`a8/x0.java:88`、`f7/c.java:112` |
| `https://adv.sec.miui.com/game/popGames` | 热门游戏列表 | `a8/k1.java:56` |
| `https://adv.sec.miui.com/game/speedParams` | **游戏加速参数（loadlimitparamsfromnet）** | `gamebooster/service/w.java:283`（`:290` 失败日志 `"loadlimitparamsfromnet failed!"`） |
| `https://adv.sec.miui.com/game/fast_reply` | 快捷回复模板 | `f7/b.java:121` |
| `https://adv.sec.miui.com/game/upgrade/pkginfo` | 游戏升级包信息（POST） | `gamebooster/model/a.java:39` |
| `https://adv.sec.miui.com/game2/gameAdv` | 游戏中心广告位 | `ve/k.java:120`、`ve/b.java:291` |
| `https://adv.sec.miui.com/gameTurbo/gcgt/tab/config` / `tab/feed` / `tab/page-detail` / `list/leisureGame` | 游戏中心 tab 配置/信息流/详情/休闲游戏 | `ve/k.java:121`、`c9/h.java:66,78`、`c9/d.java:325` |
| `https://adv.sec.miui.com/gameTurbo/migamegpt/game-ai/gt-setting-info` | **GameGPT AI 助手设置**（JSON+language+highEnergyVersion） | `gamebooster/aihelper/f.java:52` |
| `https://adv.sec.miui.com/info/layout` / `info/videoAdv` / `user/ac` | 主页布局/视频广告/用户 | dex 字符串（本模块外，未逐一定位） |
| `https://flash.sec.miui.com/game/union` | 游戏联运 | `antivirus/service/GuardService.java:338`（`antivirus_gameunion_request`） |
| `https://sd-gamespace.sec.miui.com` | 游戏空间 H5（拼 `?language=xx_YY`） | `i7/i.java:411` |
| `https://data.sec.miui.com/adv/game_ad/report` | 游戏广告上报 | dex 字符串 |
| `https://data.sec.miui.com/adv/tail` / `data/upload` | 尾部广告/数据上传 | dex 字符串 |
| `https://data.sec.miui.com/privacy/{get,agree}/v1`、`privacy/latestVersion` | 隐私协议版本 | dex 字符串 |
| `https://api.sec.miui.com/v1/traffic/report` / `data/check` / `redPacket/reportStatus` / `common/whiteList/trafficChargeNum` | 流量/数据校验/红包/免费流量 | dex 字符串 |
| `https://api.sec.miui.com/LBS/geoconv/admin` | 坐标系转换（**自动任务地理围栏用**） | `earthquakewarning/Constants.java:61`（同常量被多处复用） |

**注**：`adv.sec.miui.com` 与 `api.sec.miui.com` 在 `mnntrain` 上并存 → **推断**：`adv` 为对外/广告域，`api` 为内部 API 域，老路径未清理。

---

## 10. 未解之谜（明确标注）

1. **`PowerKeeper` 实际调频节点**：`userConfigureStatus` → powerkeeper 进程内的 governor/thermal 写入路径不在本 APK（`未知`）。需要 powerkeeper APK 才能闭环。
2. **`Joyose` GPU profile 内容**：`IGPUTunerInterface.getProfiles()` 返回的 `GPUProfile` 字段语义、profile 与频率/电压的映射不在本 APK（`未知`）。`GPUProfile.java` 存在但需要 `com.xiaomi.joyose` 才能解释。
3. **`assets/garbage_clean.db` 的真实消费者**：本 APK 零引用；是构建遗留，还是 cleanmaster 通过 signature provider 拿 assets 副本（`openFileInput`/`context.fileList` 机制见 `lj/c.b()`）—— 无法从本 APK 判定。`未知`。
4. **`performance_mode/data.json`(116KB) 与 `redmi_performance_mode/Ai-Model.json`(119KB)**：无 `assets.open("performance_mode/…")` 调用点，`grep` 仅命中 `R.string`/`r4.a` key。是纯 Lottie/布局资源，还是被 `MAML` 的 `FolderResourceLoader` 按目录名访问？`未知`（后者可能性较高，因为 MAML 用目录而非单文件）。
5. **`assets/model` + `assets/dict`** 的模型类型：`mj.a` 的 native 实现（应在 `libtensorflowlite_jni` 或另有的 JNI）未在本次展开；`model`(680KB) 头部非 TFLite flatbuffer 魔数（`TFL3`），需单独验证。`未知`。
6. **MAML 的 `maml/*/manifest.xml` 与 `LifecycleResourceManager`** 是否支持**运行期从网络拉新 manifest**：`ResourceLoader` 子类里未见到 HTTP loader；网络化只经 `WebViewScreenElement` 与 `issueExternCommand`。**"远程 UI 下发"是通过宿主 App（SystemUI/桌面/主题商店）把下载好的 zip 交给 `ZipResourceLoader` 实现的** —— 本 APK 内不含下载器。这是重要的边界澄清：**本 APK 内的 MAML 是"解释器"，不是"分发器"**。
7. **新版/旧版 autotask 的切换条件**：两套 Provider 同时存在于 Manifest，哪套生效取决于调用方（`com.miui.home`/设置）选择哪个 authority。`未知`。
8. **`com.miui.apppredict` 的 `AiService` 具体被谁 bind**：需 `miui.permission.APP_PREDICT_VERIFY`（signature 级），候选：`com.miui.home`、小爱。`未知`。
9. **`libMNNTrain` 是否真的执行反向传播**：`nativeTrainRecModel` 返回 `float[]`（`[0]` 当 loss 用），行为符合训练；但训练在**主进程后台线程**（`ef.z` 线程池）执行，**未见电量/温度熔断之外的资源限制**（仅 `battery>=20%`）。实际 CPU 占用 `未知`。
10. **`jg.b.b(exceptionList)`（`cleanMemory`）** 的真实实现：`jg` 包未在本次展开。

---

## 11. 一页速查：模式 → 写入点

```
省电模式开
  ├─ Settings.System.POWER_SAVE_MODE_OPEN = 1
  ├─ be.g  → DisplayFeatureManager.setScreenEffect(24, 60) + user_refresh_rate=60
  ├─ be.c  → Settings.Secure.gxzw_icon_aod_show_enable = 0
  ├─ be.h  → dtmf_tone/sound_effects/lockscreen_sounds/screenshot_sound/delete_sound = 0
  ├─ be.i  → pick_up_gesture_wakeup_mode/gesture_wakeup/wakeup_for_keyguard = 0
  ├─ be.b  → 手动亮度 -5%（SDK<33）
  ├─ ce.g  → setMasterSyncAutomatically(false) + POWER_SAVE_PRE_CLEAN_MEMORY_TIME=600
  ├─ ce.g  → Settings.Secure.location_mode 降级（SDK≤28）
  ├─ ce.g  → MiuiWifiManager.setAntHalf(0)（反射）
  ├─ ce.g  → powerkeeper GlobalFeatureTable.userConfigureStatus = "ultimate"
  └─ 广播 miui.intent.action.POWER_SAVE_MODE_CHANGED {POWER_SAVE_MODE_OPEN=true}

性能模式开
  ├─ Settings.System.POWER_PERFORMANCE_MODE_OPEN = 1
  ├─ ce.g  → powerkeeper GlobalFeatureTable.userConfigureStatus = "performance"
  ├─ 广播 miui.intent.action.HIDE_MODE_CHANGE {hideModeStatus="performance"}
  └─ 通知 channel "com.miui.powercenter.low"（120s 后可关）

均衡模式开
  ├─ Settings.System.POWER_BALANCED_MODE_OPEN = 1
  └─ ce.g  → userConfigureStatus = "enhance"

超级省电/极限省电
  ├─ powerkeeper.configure "changeExtremePowerMode" {EXTREME_POWER_SAVE_MODE_OPEN}
  ├─ Settings.System.power_supersave_mode_open
  └─ broadcast com.miui.powercenter.SUPER_POWER {progress=0..3}  → com.miui.superpower 极简桌面

游戏加速
  ├─ bindService(pkg=com.xiaomi.joyose) → IGPUTunerInterface.getProfiles()/checkSupport*
  ├─ Settings: MiuiSettings$ScreenEffect.GAME_MODE（反射）
  ├─ powerkeeper IFeedbackControl（温控反馈）
  ├─ 网络参数：GET adv.sec.miui.com/game/speedParams
  └─ GAME_MODE 复位：resetGameMode()

app 预测
  ├─ <filesDir>/rec_model_20230425.mnn (MNN, CPU, 4 threads)
  ├─ 输入: [pkg(t-1..t-5) char-id[30], pkg(cand) char-id[30], hour]
  ├─ 输出: linear_9.tmp_1 → softmax → P(next)
  ├─ 融合: MNN ⊕ Bayes ⊕ PBB（倒数排名加权）
  └─ 训练: libMNNTrain, epochs=10 lr=1e-4 batch=128, ≥50 样本, 电量≥20%, loss 劣化即回滚
```

---

*报告结束。所有 `类名:行号` 均可在 `_analysis/jadxout/sources/` 下直接定位。*
