> **Historical analysis.** This archive includes observations that remain unverified or were later corrected. See the [final report](../REPORT.md) and [independent review](../REVIEW.md) for the reviewed findings on upload scope, consent, local FBO delegation, emergency SMS encryption, and provider authorization. Source references retain their original decompilation paths. The publication includes the selected files under evidence/.

# Power and performance/storage cleaning/game acceleration/automatic tasks/device-side AI — module reverse engineering report

> Target: `MIUISecurityCenter-10.4.9-250402.0.1.apk` (`com.miui.securitycenter`, versionCode 40001049)
> SHA-256 `fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd`
> Analyst: `power-gaming`｜Evidence source: jadx full decompilation (10624 `.java`), apktool resources, `assets/` binary, REA SQLite
> Annotation convention: **observation** = shipped artifact fact (can goto directly to the line) | **inference** = reasonable conclusion drawn from the combination of observations | **unknown** = no evidence
> All conclusions of **are static analysis, without equipment/dynamic evidence.**

---

## 0. Three corrections to shared baselines (important)

| Baseline original statement | This observation | Evidence |
|---|---|---|
| `assets/character.json` non-plaintext/suspected encryption | **is wrong**. It is 600B plain text JSON: `{"<pad>":"0","<unk>":"1","a":"2","b":"3",…}`, used by `ea.c.a()` `BufferedReader` + `JSONObject` when character table parsing | File header hex `7b 0a 20 20 20 20 22 3c 70 61 64 3e…`; `ea/c.java:21`, `ea/c.java:44-58` |
| `assets/dict`(3.6MB)/`assets/model`(680KB) is a device-side AI prediction model | **does not belong to the app to predict**. Both are resources of the **anti-spam/anti-phishing text classifier** (`lj.c.f29439b = {"dict","model","pattern","phish"}`, `lj.g.d()` feeds `dict` to `mj.a`). The app prediction uses only one model: `assets/rec_model_20230425.mnn` | `lj/c.java:17`、`lj/g.java:32-36`、`ia/d.java:105-113` |
| `assets/garbage_clean.db` is a garbage cleaning rule base (implied to be used by this APK) | The table structure is correct (`t_cache`/`t_ad`/`t_residual` three **isomorphic** tables, each with 8 columns, **zero index/zero view/zero trigger**), but **is all DEX There is no code referencing the file name**. The real cleaning engine is outside the **App** (`com.miui.cleanmaster`) | `inspect_sqlite_database` evidence `ev_864f519d…`; `garbage_clean` in `findings/dex_strings_all.txt` only hits `R.string`; `securityscan/scanner/CacheCheckManager.java:104-114` |

`pinyinindex.idf` = Chinese → Pinyin index (custom `IDF ` magic number + `cl.a` DirectIndexedFile reader), service contact/application T9 search, **has nothing to do with power/game/AI prediction** (`am/a.java:47-58`, `cl/b.java:29`).

---

## 1. Global structure: who really does the work

```
                       ┌──────────────────────────────────────────────┐
   User visible UI ────────►│ com.miui.securitycenter (this APK) │
   (PowerCenter/ │ · Only do: UI / Policy Determination / Write Settings / Broadcast │
    GameBooster/… ) │ · Do not adjust CPU/GPU governor directly │
                       └───┬───────────┬──────────┬─────────┬────────┘
                           │           │          │         │
          ContentResolver.call()   bindService  bindService  AIDL
                           │           │          │         │
             ┌─────────────▼──┐  ┌─────▼──────┐ ┌─▼────────────┐ ┌▼──────────────┐
             │ powerkeeper    │  │ joyose     │ │ cleanmaster  │ │ powercenter   │
             │ (hidden mode/thermal│ │ (GPU tuner │ │ (GC engine) │ │ .powersaver │
             │  /feedback) │ │ GunSight/ │ │ │ │ (this APK itself │
             │                │ │ 4D vibration) │ │ │ │ ContentProvider)│
             └───────┬────────┘  └─────┬──────┘ └──────────────┘ └───────────────┘
                     │                 │
              /sys/class/thermal/* GPU/CPU frequency modulation (outside the process, this APK is not visible)
```

**core conclusion (observation)**: The implementation method of APK for "power saving/performance/game acceleration" is **cross-process configuration to deliver**, not local sysfs frequency modulation. The only place where sysfs is written directly is related to **temperature/charging** (see §2.6), all strings are written in plain text through `FileWriter`.

---

## 2. `com.miui.powercenter` (Category 158) - power and performance subject

### 2.1 Function list and entry

| User functions | Entry component (Manifest) | key class |
|---|---|---|
| Power & Performance Home Page | `PowerMainActivity`（exported） | `powercenter/PowerMainActivity.java` |
| Power saving mode details | `savemode.PowerSaveActivity`（`com.miui.powercenter.POWER_SAVE`） | `powercenter/savemode/PowerSaveFragment.java` |
| Power settings (performance mode switch + 120s reminder) | `PowerSettings` / `PowerSettingsFragment` | `powercenter/PowerSettingsFragment.java:125,161-174` |
| Performance mode shortcut switch | `powersaver.PerformanceModeTileService`（Tile） | `powersaver/PerformanceModeTileService.java:107` |
| Wild mode shortcut switch | `powersaver.PerformanceWildModeTileService`（Tile） | Same as above (`pg.i.H()` switch copywriting) |
| Power consumption ranking/application power consumption details | `legacypowerrank.PowerDetailActivity`（exported） | `legacypowerrank/*`、`batteryhistory/*` |
| Power consumption anomaly detection | Home card | `abnormalscan/AbnormalDataModel.java`、`abnormalscan/AbScanModel.java` |
| Battery Health / Unofficial Battery | `unofficalbattery.UnofficalBatteryActivity` | `unofficalbattery/*` |
| Night charging protection (smart charging) | `nightcharge.IntellectProtectSettingActivity`、`ChargerProtectActivity` | `nightcharge/*` |
| Charging characteristics (fast charging/wireless/reverse) | `charge.ChargeFeatureActivity`、`fastCharge.FastChargeActivity`、`wirelesscharge.*` | `charge/*`、`fastCharge/*`、`wirelesscharge/*` |
| Timer switch | `bootshutdown.PowerShutdownOnTime`（`miui.powercenter.intent.action.BOOT_SHUTDOWN_ONTIME`） | `bootshutdown/PowerShutdownOnTime.java` |
| Power saving suggestions | `deepsave.BatterySaveIdeaActivity` | `deepsave/*` |
| Low battery notification/boot | Notification + `PowerReceiver` | `powerui/PowerReceiver.java`、`de.PowerNoticeUtils(de/j.java)` |
| Monthly report data | `MonthReportProvider`（`content://…`） | `monthreport/MonthReportProvider.java` |

**Widget**: This module **does not have** AppWidget; `com.miui.securitycenter.WidgetProvider` handles `com.miui.intent.action.CHANGE_POWER_SAVE_MODE` (`securitycenter/WidgetProvider.java:92,261`), which is a desktop shortcut/large folder entry, not an appwidget.

### 2.2 Four "modes" of authoritative status bits (observations)

| mode | Settings key | reading and writing point |
|---|---|---|
| Power saving mode (hidden mode `enhance`) | `Settings.System.POWER_SAVE_MODE_OPEN` | `me/w.java` `Q()`；`PowerSaverProvider.java:279,333` |
| Performance mode (hidden mode `performance`) | `Settings.System.POWER_PERFORMANCE_MODE_OPEN` | `me/w.java` `P()`；`PowerSaverProvider.java:218,222` |
| equilibrium mode | `Settings.System.POWER_BALANCED_MODE_OPEN` | `me/w.java` `F()`；`PowerSaverProvider.java:186` |
| Super power saving/extreme power saving | `Settings.System.power_supersave_mode_open` + `Settings.Secure.EXTREME_POWER_MODE_ENABLE` | `me/w.java` `S()`；`de/j.java:200` |
| Super power-saving transition state | `Settings.System.superpower_switching_state` | `pg/i.java:74,152` |

### 2.3 The only authoritative writing port: `PowerSaverProvider.call()` (observation)

`content://com.miui.powercenter.powersaver` (authority is not listed in the Manifest, but `Uri.parse("content://com.miui.powercenter.powersaver")` is hard-coded in the code) exposes 8 methods:

| method | Parameter key | Purpose |
|---|---|---|
| `changePowerMode` | `POWER_SAVE_MODE_OPEN`, (optional) `LOW_BATTERY_DIALOG`, `deviceId` | Switch power saving mode → `n(boolean,boolean,String)` |
| `changePerformanceMode` | `POWER_PERFORMANCE_MODE_OPEN`、`POWER_PERFORMANCE_MODE_FROMUSER` | Switching performance mode → `l()` → `k()` |
| `changeBalancedMode` | `POWER_BALANCED_MODE_OPEN`、`POWER_BALANCED_MODE_FROMUSER` | Switching equalization → `j()` → `i()` |
| `changeSuperPowerMode` | `POWER_SUPERSAVE_MODE_OPEN`、`POWER_SUPERSAVE_MODE_FROMUSER` | Super power saving → `c(bundle,false)` |
| `changeExtremeEnduranceMode` | Same as above | Extreme battery life → `c(bundle,true)` |
| `cleanMemory` | `CLEAN_MEMORY_EXCEPTION_LIST`(ArrayList) | → `jg.b.b(list)` Clear memory (exempted in whitelist) |
| `showLowBatteryDialog` | — | Different prompts pop up according to the battery level |
| `showSuperPowerHome` | — | → `e.G(ctx)` |

Evidence: `powercenter/powersaver/PowerSaverProvider.java:387-432`.

Mutually exclusive logic (observation): `n()` turns on `if (w.Q()) return` first (exit if turned on), and **turns off performance/balancing first**: `k(false)`+`j.y()`, `i(false)` (`:305-315`); `l()` When turning on performance, turn off power saving and balancing first (`:236-249`).

### 2.4 7 practical actions of power saving mode (`be/*`, implementing logic core)

`PowerSaverProvider.d()` constructs the action list `[be.g, be.c, be.h, be.i, (unstable version)be.a, (SDK<33)be.b]`, `e()` is `[be.e]`; the forward mode is adjusted to `b()`, and the backward mode is adjusted to `a()` (`PowerSaverProvider.java:110-129, 275-332`).

| class | Log tag | Actual action (observation) |
|---|---|---|
| `be.g` **LowFpsPowerMode** | `LowFpsPowerMode` | Refresh rate→**60Hz**. Channel ① `miui.hardware.display.DisplayFeatureManager.setScreenEffect(24, fps[,250ms])` reflection (`:106-124`); channel ② `Settings.Secure.user_refresh_rate` (SDK≥33)/`Settings.System.user_refresh_rate`, `peak_refresh_rate`, `miui_refresh_rate`. The original frame rate is taken from `persist.vendor.dfps.level` or `user_refresh_rate`, the default is `FeatureParser.getInteger("smart_fps_value"/"defaultFps")`; if `ro.vendor.fps.switch.default=true`, go to the Settings branch. Save the original refresh rate/intelligent frame rate to `gd.c.d2()`/`gd.c.o1()` before entering the mode, and restore it when exiting (`:170-297`) |
| `be.c` **FingerAodPowerMode** | `FingerAodPowerMode` | Fingerprint AOD with the screen off: `Settings.Secure.gxzw_icon_aod_show_enable` → 0, the original value is backed up in `power_center_finger_aod`. Only if `ro.hardware.fp.fod=true` and recorded fingerprint>0 |
| `be.h` **SoundPowerMode** | `SoundPowerMode` | All 5 system tones are turned off: `dtmf_tone`/`sound_effects_enabled`/`lockscreen_sounds_enabled`/`has_screenshot_sound`/`delete_sound_effect`, and each original value is backed up to `power_center_sound_mode_*` (the value -1 indicates no backup) |
| `be.i` **WakeupPowerMode** | — | Turn off wrist raising/double-click to brighten screen/notification to brighten screen: `pick_up_gesture_wakeup_mode`/`gesture_wakeup`/`wakeup_for_keyguard_notification` → 0, backup to `power_center_wakeup_*` |
| `be.a` | — | Automatic brightness upper limit is pressed to 30% (`gd.c.v()` backup, `me.t.O(n)` setting); only `!Build.IS_STABLE_VERSION` is listed |
| `be.b` **BrightnessPowerMode** | `BrightnessPowerMode` | Manual brightness **-5%** (`me.m.r(ctx, cur-5)`) when not automatic brightness; only `SDK<33` is listed |
| `be.d` | — | 5G off (international version): `power_center_is_5g_mode_enable(_slot0/1)` backup, `me.h.k(false)`/`me.h.m(false,slot)` |
| `be.e` **HapticPowerMode** | — | Off vibration feedback `haptic_feedback_enabled`→0; only when `sys.haptic.motor ∈ {linear, zlinear}` (linear motor) |

`n()` Inline extra actions (`PowerSaverProvider.java:266-353`):
- Synchronous switch: `ContentResolver.setMasterSyncAutomatically(false)`, key `POWER_SAVE_PRE_SYNCHRONIZE_ENABLE` (`ce/g.java:31-51`)
- Memory cleaning time: `POWER_SAVE_PRE_CLEAN_MEMORY_TIME` raised to 600s (`ce/g.java:304-317`)
- Positioning downgrade: `POWER_SAVE_PRE_LOCATION_MODE` → `Settings.Secure.location_mode` (only SDK≤28, `ce/g.java:319-338`)
- Wi-Fi Antenna Halved: Reflective `android.net.wifi.MiuiWifiManager.setAntHalf(int)` (`ce/g.java:53-76`)
- **real FM request**: `g.y(ctx, "ultimate"|"performance"|"enhance")` → see §2.5

### 2.5 Real CPU/GPU FM channel (observation, correction of "sysfs FM" statement)

`ce.g.y()` does not write sysfs, but writes ContentProvider**of**powerkeeper:

```
ce/g.java:340-363  y(ctx, status)
  → x(ctx, str)  :354
      → ContentResolver.call("content://com.miui.powerkeeper.configure/GlobalFeatureTable",
                              "GlobalFeatureTableupdate", null, {userConfigureStatus: str})
```

`status ∈ {enhance, performance, ultimate}`, read back with `GlobalFeatureTablequery` (`ce/g.java:137-145`). **behavior frequency modulation is executed by the powerkeeper process according to `userConfigureStatus`** (outside the process, this APK is not visible → `Unknown` specific governor node).

Auxiliary channel (same as provider, `content://com.miui.powerkeeper.configure`):

| method / uri | Purpose | Evidence |
|---|---|---|
| `changeExtremePowerMode` | Extreme power saving switch (bundle key `EXTREME_POWER_SAVE_MODE_OPEN`/`IS_NOTIFY`/`SOURCE`) | `de/j.java:250-260` |
| `getBcasTempModeState` → `BcasTempMode` | Temperature control gear, compared with `low_temperature_extreme_endurance_temp_mode_value` (default 3) | `ce/g.java:195-205` |
| `isSupportSleepMode` → `power_support_sleep_mode` | Sleep mode support | `ce/g.java:255-262` |
| `SimpleSettings/misc` `GET_misc sleep_mode_user` | Sleep mode user value `on`/`off` | `ce/g.java:167-176` |
| `SimpleSettings/misc` `GET_misc user_de_configured_apps` | User-configured background policy application | `legacypowerrank/PowerDetailActivity.java:902` |
| `SimpleSettings/misc` `GET_misc key_thermal_configure` / `PUT_misc` | Temperature control configuration reading and writing | `me/a0.java:190,227` |
| `warmControlModeSupported` | Temperature control mode support | `me/a0.java:216` |
| `UserConfigure` table `query` / `METHOD_UPDATE` | Single application background/power saving strategy | `a8/k0.java:210,288`、`networkassistant/firewall/UserConfigure.java:27` |
| `IPowerKeeper` AIDL `B7/t0/t6/X5/D2/M1/k5` | Bundle-based universal command channel + application active monitoring | `powerkeeper/IPowerKeeper.java:202-214` |

In addition: `miui.permission.powerkeeper.HIDDEN_MODE_PROVIDER` and `com.miui.powerkeeper.permission.POWER_NOTIFY` are the docking permissions declared by the Manifest (observation, `AndroidManifest.xml`).

`PowerManager` reflective gasket: `h(boolean)` When SDK≥30 and `persist.sys.miui_optimization` is allowed, the reflective adjustment is hidden API `PowerManager.setPowerSaveModeEnabled(boolean)` (`PowerSaverProvider.java:160-172`, via `bh.d.c(...)`).

### 2.6 Direct writing to sysfs (only temperature/charge, observation)

**This is the only sysfs write surface** of this APK, all `FileWriter` plaintext, no CPU/GPU governor node:

| node | direction | Purpose | Evidence |
|---|---|---|---|
| `/sys/class/thermal/thermal_message/sconfig` | W | Write temperature control gear int (`SystemProperties.set("persist.sys.thermal.config", i)` at the same time) | `me/a0.java:127-145` |
| `/sys/class/qcom-battery/lpd_charging` | R/W | Read/write LPD limited charge status (smart charging/night protection landing) | `me/f.java:171(read), 229(write)` |
| `/sys/class/thermal/thermal_message/charger_temp` | W | Write the charger temperature threshold as `"0"` or a numerical value | `sd/f.java:256` |
| `/sys/class/thermal/thermal_message/board_sensor_temp`、`board_sensor_second_temp` | R | Motherboard temperature | `me/w.java:66`、`x5/p.java:488,491` |
| `/sys/class/thermal/thermal_message/charger_temp` | R | Charging temperature | `me/e.java:45` |
| `/sys/class/power_supply/battery/{charge_full,charge_full_design,model_name}`、`/sys/class/qcom-battery/battery/{charge_full,charge_full_design}`、`fg1_rm`/`fg2_rm`、`slave_authentic`、`real_type`、`tx_adapter`、`power_max`、`usb/type` | R | Battery Health/Capacity/Dual Cell/Original Identification/Charger Type | `me/e.java:131-133,212-214,295,318`、`me/b.java:62`、`me/w.java:139,636,743,748` |
| `/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq` | **R only** | Maximum reading frequency (for display/scoring, **does not write**) | `hl/a.java:322` |
| `/sys/class/net/wlan0/address` | R | MAC | `v9/b.java:18` |

`/proc/cpuinfo` is also read-only (`hl/a.java:517,662`).

### 2.7 Configuration and Thresholds

`gd.c` (~26KB, about 400 accessors in total) is a centralized layer for all power/charging/temperature control thresholds, implemented in **`content://com.miui.securitycenter.remoteprovider`’s `callPreference`** (cross-process SP agent, supports `110@authority` multi-user/delegate prefix), see `r4/a.java:19-51`.

Representative threshold (observations, all via `gd.c`):

| key | Default | meaning | Evidence |
|---|---|---|---|
| `key_partical_wakelock_abnormal_threshold` | 7 | Partial wake lock exception determination | `gd/c.java:65` |
| `cpu_abnormal_threshold` | — | CPU power consumption abnormality determination (visible in `PowerSaveService` dump) | `powercenter/provider/PowerSaveService.java:659` |
| `on_time_shutdown_time` | 1410 | Scheduled shutdown (23:30) | `gd/c.java:22` |
| `pc_low_temperature_extreme_endurance_switch` | 1 | Low temperature limit battery life switch | `ce/g.java:149` |
| `low_temperature_extreme_endurance_temp_mode_value` | 3 | Low temperature trigger temperature control gear | `ce/g.java:157` |
| `low_temperature_extreme_endurance_support` | 0 | Model support bit | `ce/g.java:225` |
| `sec_pc_config_scenario_policies_open` | -1 | Scenario strategy (sleep mode) | `ce/g.java:162` |
| `key_charge_full_in_night` | false | full of night | `gd/c.java:17` |
| `pc_notification_id_smart_charge_pogo` | int res | Pogo Pin charging notification ID | `de/j.java:606` |
| `power_dialog_performance_mode_checkbox` | The copy contains **120** | Performance mode 120s automatic exit prompt | `batteryhistory/c0.java:512`、`j0.java:1036` |

Device feature gating (`miui.util.FeatureParser`, `/system/etc/device_features/*.xml`):
- `support_extreme_battery_saver`, `support_superpower_replace_extremesaver`, `support_wild_boost_bat_perf` (wild mode V1), `support_power_save_new`, `bcl_level0_region_list`, `fpsList`, `smart_fps_value`, `defaultFps`, `support_hangup_while_screen_off` (`pg/i.java:65-68,131`, `ce/g.java:116,252`, `be/g.java` static)
- `persist.vendor.battery.erp` (international version ERP battery), `persist.sys.power.default.powermode` (default hidden mode, default `"performance"`, `"unSupported"` Indicates not supported), `persist.vendor.audio.power.save.setting & 9 == 9` (audio power saving), `persist.vendor.fps.switch.default`, `ro.hardware.fp.fod`, `sys.haptic.motor`, `ro.miui.cts`
- Wild Mode V2 Judgment: `pg.i.J()` Compare `i.g()` (model) with `FeatureParser.getStringArray` whitelist (`pg/i.java:134-146`)

### 2.8 Data and persistence

- **does not have its own database** (batteryhistory has a Provider but `batteryhistory/BatteryHistoryProvider` was not explored in depth this time).
- The Settings.System/Secure key is about **40+** (above table + `POWER_SAVE_*` backup key family).
- SP all go to `remoteprovider.callPreference` (`r4.a`); `base_model`/`train_configuration` wait in apppredict’s SP (§10).
- Cross-process: `com.miui.securitycenter.remote` process hosts `PowerSaveService`/`PowerCenterProvider`/`AlarmReceiver`/`AutoTaskIntentService`/`ShutdownAlarmIntentService`/`FboJobService` (Manifest).

### 2.9 `com.miui.superpower` (Class 23)

**observed**: It **is not the power saving logic of**, but the "minimalist desktop" UI**of** super power saving mode - self-drawn status bar.
- `SuperPowerProgressActivity` (enter/exit progress animation, `PowerSaverProvider.java:92` starts)
- `SuperPowerSettings` / `SuperPowerSettingsFragement` / `SuperPowerDetailPreference`
- `notification.SuperPowerTileService` (QS Tile, `enabled="false"` is off by default)
- `statusbar/*`：`Clock`、`WifiViewLinearLayout`、`icon.BatteryView/SimSignalView/WifiSignalView/FlightModeView`、`button.{BlueTooth,Cellular,GPS,Vibrate,Wifi}Button`、`panel.NotificationPanelLayout`、`slider.ToggleSeekBar/ToggleSliderView`、`utils.DotsFirstLineCenterTextView`
- Evidence: File list in the package + Manifest `<service …SuperPowerTileService>`; `pg/i.L()` broadcasts `com.miui.powercenter.SUPER_POWER` and carries `progress`(0/1/2/3) to drive the UI (`pg/i.java:148-189`)

### 2.10 `com.miui.powerkeeper` (Category 5)

**observes**: only 3 AIDL + 2 beans, **does not implement**. This APK is just the **client** of powerkeeper.
- `IPowerKeeper` (7 transacts: `X5(int)`/`B7(Bundle)`/`t0(Bundle,Bundle)`/`D2()→List<AppStateInfo>`/`M1`, `k5` (register/cancel `IAppActiveUIChangedListener`)/`t6(String)`)
- `feedbackcontrol.IFeedbackControl`(12 transact：`f3(int)`/`Y3()`/`L4`,`X0`(listener)/`G2()→Bundle`/`K7()`/`r5(int,String)`/`Z5()`/`f1()`/`i0(boolean)`/`a4(String[])`/`r3()`)—— For gamebooster to do**Temperature control/performance feedback**(`gamebooster/service/w.java:28`import)
- `ui.AppStateInfo`、`ui.IAppActiveUIChangedListener`
- Call point: `j8/u.java:79` (VideoEffect), `legacypowerrank/PowerDetailActivity.java:976`

---

## 3. `com.miui.optimizecenter` (Category 40) - Storage Cleanup

### 3.1 Function list and entry

| Function | entrance | class |
|---|---|---|
| Storage Home Page (Colored Bar Chart) | `storage.StorageActivity`（exported，action `com.miui.cleanmaster.action.STORAGE_MANAGE`） | `storage/StorageFragment.java`、`storage/StorageFragmentWork.java` |
| Deep cleaning entrance | Same as above (`button_deepclean`) | `StorageFragment.java:167` |
| Application storage list | `AppStorageListActivity` | `storage/AppStorageListActivity.java` |
| Application storage details (cache/data/APK/others) | `AppStorageDetailActivity` (`getStringExtra("model")` transfer package name) | `storage/AppStorageDetailActivity.java:765` |
| System data | `SystemStorageDataActivity` | `storage/AppSystemDataManager.java`(27KB) |
| Public large files (pictures/videos/audio/documents) | `StoragePublicFileListActivity` | `storage/model/PublicFileModel.java` |
| Save settings | `StorageSettingsActivity` | — |
| **FBO** (File Based Optimization) result page | `FboResultActivity`（`miui.permission.USE_INTERNAL_GENERAL_API`） | `storage/FboResultActivity.java` |
| UFS Tips | `UfsTipActivity` | `storage/UfsTipActivity.java` |
| Storage Widget | No appwidget (`widget/storage/StorageColumnView` is a self-drawn View) | — |

### 3.2 Clean up the rule engine - **is not here APK** (observation)

```
securityscan/scanner/CacheCheckManager.java:103-115
  a.run() → Domestic: pkg="com.miui.cleanmaster", action="com.miui.cleanmaster.action.CHECK_GARBAGE_CHECK"
            International: pkg=c4.j.c(ctx), action=c4.j.f7946b.get(pkg)
  → o4.a.d(action, pkg, callback)
      → callback.v1(IBinder)  :92
          → IGarbageCheck.Stub.asInterface(binder)  :72
              → iGarbageCheck.F3(IGarbageScanCallback) // Start scanning
              → iGarbageCheck.J(list, IGarbageCleanupCallback) //Perform cleanup
```

`com.miui.optimizecenter.garbagecheck.IGarbageCheck` has only 3 transacts: `F3(IGarbageScanCallback)` / `v1?` / `J(List<String>, IGarbageCleanupCallback)`, see `IGarbageCheck.java:108`.
Scan callback `IGarbageScanCallback`: `U5(pkg, path, ?, size, isDir)` (item callback), `u(String)` (filter?), `d()`/`h()` (start/end), see `IGarbageScanCallback.java`.

**Therefore `assets/garbage_clean.db` is "dead weight" in this APK**: SQLite structure is complete (3 isomorphic tables + `android_metadata` + `sqlite_sequence`, `advise_del` column storage "is it recommended to delete"), but zero code references. The real rule base is distributed with `com.miui.cleanmaster`. **infers**: It may be mistakenly packaged during the build period / or left to cleanmaster to read through the signature level provider.

`FboJobService` semantics (observation, `storage/fbo/FboJobService.java`):
- **is not the algorithm implementation of** "File Based Optimization", it is just a **JobScheduler trigger**
- `d(ctx)`: When SDK≥28 and `ta.d.b()` (=`fboIsSupport`) is true, register `JobInfo(id=LEVEL_LARGE_ONLY_TITLE, periodic=**21_600_000ms = 6h**, persisted=true, requiresDeviceIdle=true)`
- `onStartJob` → The background thread executes `FboPackageBean.getInstance().upLoadPackageList()`, that is, **uploads the application package list** (for portrait/statistical purposes), and does not perform file operations.
- Evidence: `FboJobService.java:30-95`

### 3.3 `com.miui.optimizemanage` (Category 29) - One-click acceleration/first aid

| Function | class |
|---|---|
| Speed up the homepage with one click (turntable animation) | `OptimizemanageMainActivity`、`view/OptimizeMainView.java`、`view/OptimizeManageAnimView.java` |
| Cleanup/results page | `CleanFragment.java`(48KB), `ResultFragment.java`(40KB), `optimizeresult/*`(12 categories) |
| Memory cleaning | `memoryclean/a.java`, `memoryclean/LockAppManageActivity` (locked applications are not cleaned) |
| Settings (drop-down lock screen/memory usage style) | `settings/SettingsFragment.java`、`settings/DropDownLockScreenAdapter`、`DropDownMemoryOccupyAdapter` |
| Cleaning results linkage card (including skip garbage cleaning) | `optimizeresult/e.java:127`（`miui.intent.action.GARBAGE_CLEANUP`） |

Entrance: `OptimizemanageMainActivity` (exported). `com.miui.cleanmaster` (6 Class) is just **thin shell**: `CleanerHandleService` (`android:process="com.miui.securitycenter.remote"`, `permission=com.miui.securitycenter.permission.CLEANER_MANAGE`), `CleanerInstallActivity` (`com.miui.cleanmaster.InstallAndLunchCleanMaster`), `LowMemoryIntentDispatchActivity` (`com.miui.securitycenter.LunchCleanMaster`), `ICleanerShortcut` AIDL. `com.miui.cleaner.ICleanerShortcut` (Class 1) = Legacy AIDL compatible.

### 3.4 `com.miui.firstaidkit` (Category 28)——Mobile first aid

`FirstAidKitActivity` (exported) + whitelist Activity. The Model layer is the complete set of functions (observations):

| Group | Model class | User visible items |
|---|---|---|
| Power consumption | `consumePower/AutoStartApplicationModel`、`HotspotModel` | Self-starting management, hotspot exceptions |
| network | `internet/BackgroundConnectionModel`、`RestrictDataUsageModel`、`WifiOrFlowSwitchOffModel` | Background networking, traffic limit, WiFi/data turned off |
| Operation | `operation/AirplaneModel`、`BlockCallsFromContactsModel`、`BlockCallsFromStrangersModel`、`DoNotDisturbModel`、`EyeshieldModel`、`FidCleanerShortcutModel` | Airplane mode, intercept contacts/strangers, do not disturb, eye protection, clean shortcuts |
| Others | `other/SimModel`、`SystemRootModel`、`UnableInstallApplicationModel` | SIM card, Root, unable to install applications |
| Performance | `performance/AuxiliaryFunctionModel`、`DeviceManagerModel`、`InsufficientMemoryModel` | Accessibility Service Abuse, Device Manager, Out of Memory |

Scan item resource: `assets/firstaid_scanitem.json`(4923B).

---

## 4. `com.miui.gamebooster` (Class 280) – Game acceleration

### 4.1 Overview of sub-packages (observations, number of categories)

`ui`(51)、`model`(32)、`globalgame`(30)、`customview`(25)、`windowmanager`(22)、`service`(21)、`beauty`(16)、`videobox`(11)、`aihelper`(10)、`widget`(10)、`shoulderkey`(8)、`view`(6)、`utils`(5)、`voicechanger`(5)、`brightness`(4)、`mutiwindow`(3)、`predownload`(3)、`xunyou`(3)、`framerate`(2)、`gbservices`(2)、`storage`(2)、`common`(1)、`encoder`(1)、`gamemode`(1)、`gametime`(1)、`guide`(1)、`provider`(1)

### 4.2 Function list and entry

| Function | Entry component | Notes |
|---|---|---|
| Game Acceleration Home Page | `ui.GameBoosterMainActivity`（exported，icon `game_booster_icon`）→ `ui.GameBoosterRealMainActivity` | `WelcomActivity` is the exported distributor (`:187-193`) |
| Add game/whitelist | `ui.SelectGameActivity`、`ui.WhiteListActivity` | |
| Advanced settings | `ui.AdvanceSettingsActivity` / `AdvanceSettingsDetailActivity` | |
| **performance mode (balanced/high performance (wild)/custom)** | `ui.CompetitionDetailActivity`、`windowmanager/newbox/PerformancePanelView` | `game_mode_balance` / `game_mode_wild` / `game_mode_fps_graphic` RadioGroup（`PerformancePanelView.java:73-136`） |
| Frame rate curve | `framerate/FrameRateView`、`framerate/FrameRateViewController`、`gbservices/FrameRateDataService`、`gameturbo/active/IFrameRateDataService` | Real-time FPS acquisition |
| Game Mode (Aspect Ratio: Fill/Contact Ratio) | `gamemode/GameModeSettingsActivity`、`shoulderkey/widget/GbGameModeView`、`gamebooster/assets?` | `gb_game_mode_fill` / `gb_game_mode_ratio` |
| Do Not Disturb/Function Blocking | `ui.FunctionShieldSettingsActivity` | `function_shield_title` |
| Quick reply | `ui.QuickReplySettingsActivity`、`service/NotificationListener`（`BIND_NOTIFICATION_LISTENER_SERVICE`，`singleUser`） | In-game banner reply |
| Anti-accidental touch | `gbservices/AntiMsgAccessibilityService`（`BIND_ACCESSIBILITY_SERVICE`，`xml/accessibility_gamebooster_service_config`） | **Accessibility service**, non-touch shielding |
| **shoulder key** | `shoulderkey/ShoulderKeyLightEffectActivity`、`ShoulderKeyGuideView`/`ShoulderKeyNewGuideView`/`TbGuideView`、`shoulderkey/widget/{ShoulderNewConfigLayout,ShoulderSsConfigLayout}` | Lighting effect + mapping configuration; SP `shoulder_key_settings` |
| Change voice | `voicechanger/{SettingsView,VoiceModel,VoiceTipsModel,LoginActivity}`、`service/MiuiVoiceChangeCallback`、`gameturbo/active/IVoiceChangeCallback` | Login required (`LoginActivity`) |
| **Beauty / Video Call Enhancement** | `beauty.BeautySettingsActivity`、`beauty.PrivacyCameraGlobalSettingsActivity`、`beauty/ConversationSettingsFragment` | See §4.5 |
| Screen Recording / Highlights | `ui.WonderfulMomentActivity`、`GameVideoActivity`、`GameVideoPlayActivity`、`service/VideoToolBoxService`、`service/IVideoToolBox` | Video Toolbox |
| Anchor/floating window | `service.DockWindowManagerService` (`:ui` process), `ui.WindowCallActivity` | |
| network acceleration | `ui.WifiBoosterDetail` (Xunyou), `xunyou/XunyouAlertActivity`, `service/IGameBoosterTelecomeManager` | `IMiuiVpnManageService.getSettingEx("xunyou","xunyou_wifi_accel_switch")` |
| pre-download | `predownload/*`, `i7/i.java` (`android.intent.action.joyose.predownload.{COMPLETE,UPDATE}`, authority `com.xiaomi.joyose.permission.predownload`) | |
| Game uninstall management | `ui.VerticalGameUninstallActivity`、`storage/{GameUninstallFragment,UninstallAppsManager}` | |
| Panorama/multi-window | `mutiwindow/{FreeformWindowService,a,b}`、`ui.GameBoxAlertActivity` | |
| AI Assistant (GameGPT) | `aihelper/*`、`GameToolBox2BuryPoint` | `adv.sec.miui.com/gameTurbo/migamegpt/game-ai/gt-setting-info` |

Provider：`provider/GameBoosterProvider`（`com.miui.securitycenter.permission.GAMEBOOSTER_PROVIDER`）。

### 4.3 Game identification (observation)

`assets/top_200_games.json` = `{"packageNames":[...]}`, **, there are actually 607** (the name is a historical legacy), and the first `com.tencent.tmgp.sgame`.
Read at 4 places:
- `a8/k0.java:97` — `g.a(ctx,"top_200_games.json")`
- `f7/c.java:73`
- `o6/a.java:43` — `TopGameUtils`, if the reading fails, `Log "joyose not support!!!"` (`:95`)
- `com/miui/gamebooster/ui/BoosterFragment.java:497`
- `y3/a.java:285` — `VideoToolBox` Application

Game list **dynamizes**: `a8/k0.java:161` and `f7/c.java:112`. Pull the local package name list POST to `https://adv.sec.miui.com/game/pkg` to pull "what are the games/game tags"; `a8/x0.java:88` is another `game/pkg` path. `a8/k1.java:56` pull `game/popGames`.

Front-end judgment: `com.miui.gamebooster.mutiwindow.b` provides `ForegroundInfo` monitoring (`InterfaceC0181b.onForegroundInfoChanged`, `getId()→f7.e`), and `AppPredictService.java:91-141` and `BeautyService` all reuse it (`BeautyService.java:139-183,466-472`).

### 4.4 The actual implementation of the acceleration switch (**is key, observe**)

For **, only the configuration/broadcasting is changed locally; for GPU/CPU, all FM is outsourced to Joyose.**

| layer | Evidence |
|---|---|
| `ServiceManager.getService("xiaomi.joyose")` → `com.xiaomi.joyose.IJoyoseInterface` | `a7/n.java:162` |
| `IGPUTunerInterface` (`com.xiaomi.joyose.securitycenter`, 22 transact) | `gamebooster/service/w.java:136-147`、`utils/GameBoxVisionEnhanceUtils.java:120`、`a8/f2.java:39` |
| Actual calls: `checkSupportGpuTuner()`, `checkSupportUgd()`, `getProfiles()`, `deleteProfile()` | `w.java:138-147` |
| Binding method: `Intent.setPackage("com.xiaomi.joyose")` + bindService | `w.java:428`、`GameBoxVisionEnhanceUtils.java:493`、`a8/f2.java:102`、`i6/a.java:145` |
| Capability handshake: `f1.m("com.xiaomi.joyose").getInt("joyose_game_turbo_api_version")` (ContentProvider meta) | `GameBoxVisionEnhanceUtils.java:318-320` |
| `IGunSightInterface` (picture enhancement/super resolution) | `com/xiaomi/joyose/securitycenter/IGunSightInterface.java`、`i6/a.java:17,145` |
| 4D vibration: `JoyoseManager.getGameMotorAppList()` reflection | `o6/b.java:27` |
| Temperature control feedback: `com.miui.powerkeeper.feedbackcontrol.IFeedbackControl` | `gamebooster/service/w.java:28` |
| Screen effect: `android.provider.MiuiSettings$ScreenEffect.GAME_MODE` reflection value, then `resetGameMode` | `gamebooster/service/w.java:501-509` |
| Refresh rate/display: `DisplayFeatureManager.setScreenEffect(24, fps)` | `be/g.java` (common to power saving side) |
| Brightness: `gamebooster/brightness/*` (Category 4) | — |

**Conclusion**: `getProfiles()` of `IGPUTunerInterface` returns `List<GPUProfile>` (`com/xiaomi/joyose/securitycenter/GPUProfile.java`), that is, "one set of GPU gear configurations for each game". The specific frequency/voltage of **GPU is determined by the Joyose process. This APK only delivers/selects profile → `Unknown` specific node.**

### 4.5 `BeautyService` (beauty/video call enhancement) link implementation (observation)

`com/miui/gamebooster/beauty/BeautyService.java` (inherits `service/u`, that is, `u` is the general "front-end monitoring Service" base class):

1. **does not handle camera/audio frames**. It only does **scene determination + floating window (Dock) switching**.
2. The front desk determines the three-way union:
   - `miui.process.ProcessManager.registerActivityChangeListener(List pkg, List cls, IActivityChangeListener)` reflection (`:117` region `Y()`, `Y()` method of `BeautyService.java`) → `IActivityChangeListener.onActivityChanged(pre, cur)`
   - `ActivityTaskManager.getService().registerTaskStackListener(ITaskStackListener)` reflection (`c0()`)
   - `ForegroundInfo` callback of `com.miui.gamebooster.mutiwindow.b` (`a0()`)
   - `ActivityManager.getRunningTasks(1)` (`P()`, `p0()`)
3. Determine "whether beautification is supported": `w5/f.p().c(isFrontCameraOpen, pkg, cls)`; split-screen scene goes to `T()` (traverse `getRunningTasks` and filter `MiuiFreeFormManager.getAllFreeFormStackInfosOnDisplay`)
4. Hit → `bindService(DockWindowManagerService, action="com.miui.gamebooster.service.GameBoxService")` (`:513-517`), get `IGameBoosterWindow` and then adjust `D4(5, false, pkg, cls, 0)` to enter "session mode", exit to adjust `G4(5)` (`M()`/`N()`)
5. do not disturb list `K` (static, `:239-251`): `com.miui.screenrecorder`, `com.lbe.security.miui`, `com.milink.service`, `com.xiaomi.miplay_client`, `com.xiaomi.misubscreenui`, `com.xiaomi.aiasst.vision`, `com.miui.vpnsdkmanager`, `com.xiaomi.migameservice`, `com.xiaomi.gamecenter.sdk.service`, `com.google.android.permissioncontroller`; special class name mapping `L`: `com.milink.service → com.xiaomi.dist.camera.view.CameraPickerActivity`
6. The real image processing is in `DockWindowManagerService` (`:ui` process) and `j8/u.java` (`VideoEffectUtils`, holding `IPowerKeeper`, `j8/u.java:54,79`)
7. Beauty support list (assets, scale observation): `beauty_light_default_support_list.json`(**31**), `beauty_face_support_activity_list.json`(**4**), `gesture_effect_support_list.json`(**28**), `pickup_support_activity_list.json`(**18**) / `pickup_support_activity_list_phone.json`(**19**), `privacy_all_support_activity_list.json`, `privacy_single_support_activity_list.json`, `conversation_record_activity_list.json`, `protrait_support_activity_list.json`, `aisubtitle_support_activity_list.json`; loader `w5/f.java:501,507,511,524`(`pref_light_support_apps` / `pref_gesture_effect_apps_list`)

### 4.6 `com.miui.gameturbo`（6）/ `com.miui.gamecenter`（2）

- `gameturbo` is all **AIDL callback** for `:ui` process ↔ Main process communication: `active/{IActiveManager,IActiveCallback,IFrameRateDataService,IFrameRateDataCallback,IVoiceChangeCallback,IWebPanelCallback}`
- `gamecenter`: `ui/GameCenterMainView.java`, `casual_game/GameBoxRangeTextView.java` (casual game entrance, content comes from `adv.sec.miui.com/gameTurbo/*`)

### 4.7 Game-related allocation assets

| assets | Purpose | Evidence |
|---|---|---|
| `top_200_games.json`(19.9KB, 607 pkgs) | Game package name whitelist (offline security) | §4.3 |
| `performance_mode/data.json`(116KB) + `img_0..7.png` | Performance mode introduction page/animation resources | No code reference → **unknown** (inferred to be a pure resource for use by the `gtb_guide_style1.json` class boot page) |
| `redmi_performance_mode/Ai-Model.json`(119KB) | Same as above (Redmi version) | **Unknown** |
| `game_turbo_box_mode_change/{blue,red}_light.json`(7.2KB) | Game mode switching lighting effect (Lottie) | — |
| `shoulder_key/*`（`enter_guide.json` 63KB、`guide_{next,single,trigger}.zip`、`settings_guide.json`、`using_guide.json`、`img_*.webp`） | Shoulder key guidance/lighting effect | `ShoulderKeyGuideView.java:104-107`（`setAnimation("shoulder_key/settings_guide.json")`）、`ShoulderKeyNewGuideView.java:165-168` |
| `tb_guide_style1.json`(44.7KB) | Game Toolbox Boot Styles | — |
| `game_box_barrage_v3_support_apps.json`(365B, 14 items) | Danmaku v3 supports applications | `n7/a.java:63` |
| `ultraclear_support_activity_list.json`(5) | Ultra HD image quality support page | — |
| `support_split_scene_activities.json`(1) | Split screen scene support | — |
| `vtb_default_support_activity_list.json`(33 items)/`vtb_default_support_list` | Video toolbox supports by default | `f7/d.java:101`、`i8/c.java:286`、`y3/a.java:296` |
| `xunyou_voice{,_en,_tw}.json` | Xunyou acceleration data | — |
| `service_num.json`(4.5KB) | Customer service number | — |

---

## 5. `com.miui.autotask` (Category 125) + `powercenter.autotask` (Category 22) - automatic task

### 5.1 Two sets of implementations (observations)

| package | nature |
|---|---|
| `com.miui.autotask` | **new version** (Kotlin, `ck.m` non-null assertion style), class 125, `androidx`+`Preference` |
| `com.miui.powercenter.autotask` | **Old version** (Java), 22 categories, Manifest still retains 4 Activity + Provider |

The two Provider authorities are different: the new version `com.miui.autotask.provider.auto_task` (`permission=com.miui.autotask.permission.CONTENTPROVIDER`) + `…auto_task_service` (**has no permission, exported=true**); the old version `com.miui.powercenter.autotask` (`permission=com.miui.powercenter.permission.POWER_DATA`).

### 5.2 Data model (observation)

`bean/Task` (task) = `List<Condition>` + `List<Result>`; `bean/Condition` = `{instanceId, typeId, jsonStr}` (Parcelable, `Condition.java:8-17`). That is, the **rule engine is a plug-in structure** of "typeId string + JSON parameter" and has no fixed schema table.

Other beans: `SyncTaskModel` (cloud synchronization), `AppInfoBean`, `r.java` (6.5KB, condition/result deserialization), `s.java`, `u.java` (5KB).

### 5.3 Complete set of conditions (`taskitem/*ConditionItem`, observation)

| Conditional class | user semantics |
|---|---|
| `CustomTimeConditionItem` | Customized time (period/week) |
| `ChargeConditionItem` / `BatteryConditionItem` | Start charging; battery level lower/higher than N% |
| `InCallConditionItem` | In call |
| `AppUseConditionItem` / `StartActivityConditionItem` | Open an application/enter a page |
| `LeaveAppConditionItem` | Leave an app |
| `LocationConditionItem` / `ToSomewhereConditionItem` / `LeaveConditionItem` / `AddressTaskItem` | Arriving somewhere / leaving somewhere (Geofencing, `common/PolarisGeofenceReceiver`) |
| `WlanConditionItem` / `WlanConnectSpecifiedConditionItem` / `WlanDisconnectSpecifiedConditionItem` / `WlanItem` | Connect/disconnect to designated WiFi |
| `BluetoothConditionItem` / `BluetoothDeviceConditionItem` / `BluetoothConnectDeviceConditionItem` / `BluetoothDisconnectDeviceConditionItem` / `BluetoothTaskItem` | Bluetooth connect/disconnect specified device |
| `HeadsetConditionItem` | Plug in headphones |
| `HotspotConditionItem` / `HotspotItem` | Hotspot is on |
| `MeetingConditionItem` | In Meeting (Calendar) |
| `MuteConditionItem` / `MuteItem` | mute state |
| `NfcConditionItem` | NFC switch status |
| `LockScreenConditionItem` / `LockScreenItem` | lock screen |
| `AbsorbedConditionItem` / `DefaultConditionItem` | Combination/Default |

### 5.4 Complete set of actions (`taskitem/*ResultItem`, observation)

| Action | user semantics |
|---|---|
| `WlanResultItem` / `BluetoothResultItem` / `NfcResultItem` / `HotspotResultItem` | Turn on/off WiFi, Bluetooth, NFC, hotspot |
| `AirplaneResultItem` | airplane mode |
| `DisturbResultItem` | Do not disturb |
| `MuteResultItem` / `DialToneResultItem` | Mute / key tone |
| `AdjustVolumeResultItem` | adjust volume |
| `ScreenBrightnessResultItem` / `AutoBrightnessResultItem` | Brightness / Auto Brightness |
| `DarkResultItem` / `EyeCareResultItem` / `TypefaceResultItem` / `ScreenDisplayResultItem` | Dark mode / Eye protection / Font (weight + size, `view/FontWeightAdjustView` + `FontSizeAdjustView`) / Display (resolution?) |
| `RotateOffResultItem` | Turn off auto-rotate |
| `SaveBatteryResultItem` | Turn on power saving mode (linkage §2) |
| `TouchResultItem` | Touch mode (glove/sensitivity?) |
| `TwinkleResultItem` / `FlashlightResultItem` | flash reminder/flashlight |
| `DriveResultItem` | driving mode |
| `LunchAppItem` | Start the application (`R.string.auto_task_open_app`, `:81`) |
| `LocationResultItem` | location related actions |
| `StartActivityResultItem` | Open the specified Activity |
| `NetworkResultItem` / `SwitchResultItem` / `SwitchTypeItem` | Network switch/universal switch |
| `AbsorbedResultItem` / `DefaultResultItem` / `DefaultTaskItem` | Combination/Default |
| `xiaoai/XiaoaiTaskItem` + `XiaoaiCustomTimeCondition` | Xiaoai classmate voice trigger |

The integer type codes of `condition` and `result` are returned by `b()` / `c()` of each Item (`TaskItem.java:26-28`), and `AbsorbedResultItem.b()` uses combinational logic to return 0/1 (`AbsorbedResultItem.java:98-103`).

### 5.5 Triggering and execution (observation)

- Time trigger: `PowerCenterProvider`/`AlarmReceiver`/`DateResetReceiver` (`process=…remote`) + new version `activity/AddTimeConditionActivity` and `AlarmManager`
- Geofencing: `common/PolarisGeofenceReceiver` (exported, `process=…remote`)
- Event triggering: Each `*ConditionItem` registers BroadcastReceiver/ContentObserver internally (see `AbsorbedConditionItem`, `LockScreenConditionItem` for modes)
- Execution: `AutoTaskIntentService` (old)/new version in the background of `AutoTaskProvider` and `TaskManagerActivity`
- Cloud synchronization: `bean/SyncTaskModel`

**Unknown**: Whether the new version really replaces the old version, whether there is a grayscale switch; whether the two share the same Provider data.

---

## 6. `com.miui.apppredict` (Category 25) - Application prediction (main battlefield of client-side AI)

### 6.1 Components and Exposed Surfaces (Observation, Manifest)

| components | Export | Permission | process |
|---|---|---|---|
| `service.AppPredictService` | false | — | `…remote` |
| **`service.AiService`** | **true** | **`miui.permission.APP_PREDICT_VERIFY`** | `…remote` |
| `db.WidgetBlackListContentProvider`（`com.miui.securitycenter.widget.black.list.provider`） | false | — | — |
| **`widget.AppPredictWidget2x4`**（receiver，`app_predict_widget_info_2x4`） | **true** | — | `…remote` |
| `activity.AppClassificationActivity` (application classification) | false | — | — |
| `activity.FolderSearchActivity` (folder search) | false | — | — |
| `activity.WidgetSettingActivity` / `WidgetBlackListSettingActivity` | false | — | — |
| `activity.AppSuggestPrivacyActivity` | false | — | — |

`AiService` exposed AIDL `IAppPredict` (3 methods, `AiService.java:26-83`):
- `c1(String label)` → `c.d(ctx).h(str)`
- `k3(String label)` → `c.d(ctx).c(str)`
- `n1(String label, IAppPredictCallBack cb)` → asynchronous `cb.j5(d.a(f.d()))`, **label must have registered** in `p3.c`, otherwise `Log "label = X is not register"`

`AppPredictService.f extends IPredictNextApp.Stub.R2(int n)` → Return top-n prediction package name (`AppPredictService.java:238-245`). This is the channel **where the** desktop (`com.miui.home`) takes the prediction list.

`AppPredictWidget2x4` = 2×4 desktop Widget; there is also `app_behavior_widget_info_2x2` (belonging to another module).

### 6.2 Three-stage recommendation engine (observation)

`o3/c.AppPredictManager` (single case, `o3/c.java`):
- `d()`: `k.c().b()` (UsageStats) → Filter with launcher intent and `totalTimeInForeground>0` → `PredictApp(pkg, lastTimeUsed, lastTimeUsed+60s, wifi)` (`c.java:94-113`) - **features: package name + timestamp + current WiFi**
- `l()`：`ea.b.a(recentApps)` → `ResultWithMetrics{mnnapps, bayesapps, pbbapps, apps}`
- `j()`: initialization; `r(trainPlanBean)`: training

`fa/a` (`ea.b` implementation) holds **5 submodel**:
| Field | Type | Purpose |
|---|---|---|
| `f25892d` | `ia.j` | **MNN model** (`rec_model_20230425.mnn`) |
| `f25889a` | `la.a` | PBB（Personalized Bayesian Bandit） |
| `f25890b` | `ha.a` | Default/cold start list |
| `f25891c` | `ka.b` | Bayesian Statistics → `ga.b` |
| `f25893e` | `ia.b` | another sorter |
| `f25894f` | `ia.f` | the other way |
| `f25895g` | String | files directory |

Fusion (`fa/a.java:60-89` → `ia/d.c(...)`): `d.c(mnn, mnn2, bayes, pbb)` does **reciprocal ranking weighting** `score[pkg] += 1/(rank+1)` (`ia/d.java:62-88`, `e()` implementation, `Math.min(size,50)` truncation) on 4 lists, in global descending order.

### 6.3 Device-side AI details (**focus on**)

#### (a) Model file and loading

| item | Observed |
|---|---|
| model | `assets/rec_model_20230425.mnn`, 810,908 B, header `1c 00 00 00 18 00 1c 00` —— **FlatBuffers starting point + MNN Net structure** (`MNNNet_NET`), no custom encryption |
| landing path | `<filesDir>/rec_model_20230425.mnn` (first copy from assets, `ia/d.d()`, `ia/j.java:105-113`) |
| weight after training | `<filesDir>/apprecmodel/checkpoint_N` (`ia/j.java:83-101`), when starting **, if the directory is not empty, the latest checkpoint** will be loaded first. |
| Load class | `ia/j` constructor (`ia/j.java:78-145`) |
| JNI class | `com.miui.mlkit.mobilerec.ranking.mnn.MNNNetNative` / `MNNTrainNative` |
| .so | `libMNN`, `libMNN_Express`, `libMNNTrain`, `libmnncore` (`MNNNetNative.java:5-9` static block `System.loadLibrary`) |

#### (b) Network structure (reverse from tensor name + reshape, observation)

```
inputs:  sparse_inputs_1 … sparse_inputs_6   shape [batch, 30]  int32
         sparse_inputs_time                  shape [batch, 1]   int32
output:  linear_9.tmp_1                      shape [batch, 2]   float
```

`MNNNetNative.nativeCreateSession(net, FWD=0/CPU, threads=4, null, null)` (`ia/j.java:131`), `ja/a.FORWARD_CPU=0` (also `OPENCL=3`/`AUTO=4`/`OPENGL=6`/`VULKAN=7` enumeration, not enabled).

#### (c) Input feature semantics (**This is the answer to "What are the input features"**)

`ia/d.f(String pkg, HashMap<String,Integer> vocab)`（`ia/d.java:184-199`）：

```java
int[] v = new int[30];
String s = pkg.toLowerCase(Locale.ROOT);
for (i=0; i<min(s.length(),30); i++)
    v[i] = vocab.getOrDefault(String.valueOf(s.charAt(i)), vocab.get("<unk>"));
```

That is, **"char-id sequence of the first 30 characters of the package name" (right complement 0 = `<pad>`)**, the vocabulary is `assets/character.json` (600B, `<pad>=0, <unk>=1, a=2, b=3, …`, parsed by `ea/c.a()`, `ea/c.java:44-58`).

`sparse_inputs_1..5` = char-id of the latest 5 front-end applications **(t-1...t-5) in** history;
`sparse_inputs_6` = char-id of **candidate application**;
`sparse_inputs_time` = `Calendar.get(HOUR_OF_DAY)`（0-23，`ia/j.java:230-233`）。
`batch = len(candidate list)`, `sparse_inputs_time` is copied to the entire batch by `Arrays.fill(iArr8, hour)`.

(`ia/d.g(ts)` provides the 144-bucket time encoding of `hour*6 + minute/10`, and `ia/d.a(ts)` provides weekday remapping of Monday=0 -- **is neither used by `ia/j.e()`.** → Alternate feature, `Unknown` if enabled in future models.)

#### (d) Output and post-processing (observation)

`nativeTensorGetData` gets `float[2*batch]`, and the softmax (`ia/j.java:275-296`) of `[2]` is chosen one by one, and the probability of `P(class=1)` = "the application will be opened next" is obtained. In descending order of probability, it is `mnnapps`.
Then do multiplication reweighting with the Bayesian/PBB results (`ia/j.java:309-345`: `score * count / totalCount`, NaN → 0).

**semantic conclusion (inference)**: This is a **two-class next-app predictor**, not a sequence generation model; `linear_9` is the last layer of full connection.

#### (e) Whether there is on-device training - **has it, and it has automatic rollback** (observation, **is important**)

`MNNTrainNative` (2 native methods):
```java
float[] nativeGetRecModelPredictScore(String modelPath, int[][] x6, int[] y);        // Only evaluate
float[] nativeTrainRecModel(String modelPath, String savePath,
        int[][] x6_train, int[] y_train,          // Training set: 5 history + candidate → label
        int[][] x6_test,  int[] y_test,           // test set
        int epochs, float lr, int workers, int trainBatch, int testBatch);           // Return [loss]
```
`ia/j.f(list, TrainPlanBean)` complete process (`ia/j.java:356-540`):
1. Only keep packets that appear in `dict` (character.json vocabulary) → otherwise discard
2. **sample < 50 → Return directly to** (`"trainData size is not enough, min size is 50"`, `:372-375`)
3. Sliding window `size-6` samples, 20% randomly selected as test set (`(int)(size*0.2)`)
4. Label generation: 50% probability using the real next application (label=1), 50% probability using a random negative sample of `random.nextInt(5000)` (label=0) → **negative sampling**
5. First `nativeGetRecModelPredictScore` gets the **baseline loss**
6. `nativeTrainRecModel` output to `<filesDir>/apprecmodel/checkpoint_{n+1}`
7. **`new_loss > baseline_loss` → Delete new checkpoint and collocate `is_good_than_before=false`** (`:472-484` / `:525-537`)
8. Return JSON: `train_count / current_score / new_score / train_data_size / test_data_size / train_use_time / is_good_than_before / task_epoch`

**training trigger threshold (observation, `AppPredictService.l()`, `:313-335`)**:
- Cloud `TrainPlanBean.canTrain()` (`train_on == 1`)
- User switch `sp_apppredict.getBoolean("train_enable", true)`
- **power ≥ 20%** (`BatteryManager.getIntProperty(4)`)
- The foreground application is not blacklisted (`p3.g.f32962c`)
- ≥ `TrainPlanBean.period` days since last training (`sp_apppredict "key_last_train_time"`, `AppPredictService.java:476-481`)
- The specific execution time is calculated by the cloud `start_time` (float hour), `AlarmManager.set(RTC_WAKEUP=0, …)` (`:482-495`)

`TrainPlanBean` Default hyperparameter (observation, `TrainPlanBean.java:39-46`): `epochs=10, lr=1e-4, batch_size=128, test_batch_size=1, num_workers=4, period=7`; cloud field `base_model/model_version/all_train_count/min_data_size/max_duration/task_name/type`.

#### (f) Does TFLite participate in forecasting? ——**does not participate in**

`libtensorflowlite_jni` exists in `lib/`, but **app predicts that the link only uses MNN**. TFLite belongs to the anti-spam/SMS classification (`lj/*` + `mj.a` + `assets/model` + `assets/dict`). Evidence: `grep MNNNetNative|Interpreter` None `Interpreter` hits; `lj.c.f29439b={"dict","model","pattern","phish"}`.

#### (g) Wake-up/refresh timing (observation)

| timing | action | Evidence |
|---|---|---|
| `ACTION_USER_PRESENT` / `SCREEN_ON` | predict immediately + refresh the list broadcast after 5s | `AppPredictService.java:275-280` |
| Foreground application switching (`ForegroundInfo`) | If the last switch is <60s, cancel the queue; otherwise, delay 60s predict | `:105-140` |
| Package installation/uninstallation | Rebuild index | `:168-172` |
| Wallpaper change | rebuild | `:294-297` |
| Dark mode/icon theme changes (`uiMode`/`themeChangedFlags&8`) | rebuild | `:531-556` |
| Desktop loading completed (`com.miui.home.intent.action.LOADING_FINISHED`) | rebuild | `:223-226` |
| Privacy application changes (`pe.a` ContentObserver) | rebuild | `:253-261` |
| Broadcast `com.miui.action.UPDATE_PREDICT_LIST(_EXTERNAL)` | Notify desktop refresh | `:454-461` |

### 6.4 Data and persistence (observation)

| storage | key/table | Evidence |
|---|---|---|
| SP `sp_apppredict` | `train_enable`, `key_last_train_time` | `AppPredictService.java:563,305,476` |
| SP `n.f32972b` | `key_last_train_time` (another entry to the same semantics) | `:305` |
| `train_configuration` | JSON original string sent from the cloud | `o3/c.java:78` |
| File | `<filesDir>/rec_model_20230425.mnn`、`<filesDir>/apprecmodel/checkpoint_N` | `ia/j.java:83-113` |
| ContentProvider | `com.miui.securitycenter.widget.black.list.provider` | Manifest |
| training samples | `RespImpl.saveTrainData/getTrainData`（`com.miui.apppredict.bean.RespImpl`） | `o3/c.java:169,273` |

---

## 7. `com.miui.maml` (Category 221) - Universal card rendering engine/remote UI delivery (**key special project**)

### 7.1 What is it (observation)

`com.miui.maml` (**M**IUI **A**dvanced **M**arkup **L**anguage) is a **data-driven vector UI runtime**: parse XML (`manifest.xml` + element tree) → build `ScreenElement` tree → self-draw to `Canvas`/GL.

Capability scale (number of categories) within this APK:
`elements`(58), `animation`(42), `util`(37), `data`(32), `folme`(8), `shader`(6), `component`(4), `widget`(1), plus top-level ~40 classes (including **`ActionCommand.java` single file 172KB**).

Top-level key classes: `ScreenElementRoot`(70KB), `ActionCommand`(172KB), `NotifierManager`(23KB), `RendererController`, `RenderThread`, `ResourceManager`, `ResourceLoader`, `ScreenElementRootFactory`, `MamlDrawable`/`FancyDrawable`/`AnimatingDrawable`, `MiAdvancedView`, `MultipleRenderable`, `SoundManager`, `StylesManager`, `CommandTrigger(s)`, `VibrateCommand`, `MamlDisplayEventReceiver`, `LifecycleResourceManager`, `FramerateTokenList`

`AndroidManifest.xml` declares `<meta-data android:name="com.miui.maml.sdk" android:value="true"/>`.

The MAML card resources built into this APK: `assets/maml/{antispam, na_nosim, na_nosim_dark}/{call,sms}/` + `manifest.xml`(4199B) + `bg.png/bgmask.png/board.png/shape.png`.

### 7.2 Capability Boundary (**Conclusion**)

| Ability | Whether to support | Evidence |
|---|---|---|
| **script executes** | ✅ **is** (Expression Language + Reflection) | See §7.3 |
| **network loading UI** | ✅ **is** (WebView + `issueExternCommand` is thrown to the host) | `WebViewScreenElement.java:298-310`、`ScreenElementRoot.java:1072-1092` |
| **is dynamically issued UI** | ✅ **is** (XML comes from `ZipResourceLoader`/`FolderResourceLoader`/`AssetsResourceLoader`; `ResourceLoader` can point to any directory/zip, with the signature/encryption package of `CloudCoder`) | `ResourceLoader.java`、`util/{Zip,Folder,Assets,FancyIcon}ResourceLoader.java`、`util/net/CloudCoder.java` |
| **WebView embedded** | ✅ JS enabled + `addJavascriptInterface(new MamlInterface(), "maml")` | `WebViewScreenElement.java:132,138` |
| **cross-process intent triggers** | ✅ `IntentCommand` | `ActionCommand.java:1928` |
| **Reflection calls any method/constructor** | ✅ `MethodCommand` / `FieldCommand`, but the constructor is gated by `getCapability(4)` | `ActionCommand.java:2292-2400`、`:1096` |
| **read and write ContentProvider** | ✅ `ContentProviderBinder` | `data/ContentProviderBinder.java` |
| **Receive broadcast** | ✅ `BroadcastBinder` | `data/BroadcastBinder.java` |
| **read file/sensor/audio/volume/battery** | ✅ `FileBinder`/`SensorBinder`/`MusicController`/`VolumeVariableUpdater`/`BatteryVariableUpdater` | `data/*`、`elements/MusicController.java` |
| **persistent variable** | ✅ `CAPABILITY_VAR_PERSISTENCE=2` | `ScreenElementRoot.java:68` |
| **Default permission posture** | ⚠️ **`mCapability = -1` (CAPABILITY_ALL) = Fully open** | `ScreenElementRoot.java:505, 992`; `setCapability(int, boolean)` narrowed (`:1480-1489`) |

### 7.3 The exact form of the "script" ability (observation)

MAML has a set of expression + command system **with higher Turing completeness** (not JS, but self-developed DSL):

**expression layer** (`data/`):
- `Expression` / `RootExpression` / `ExpressionVisitor` — AST Evaluation
- `BaseFunctions` / `StringFunctions` / `JsonFunctions` / `FormatFunctions` / `DateFunctions` / `FunctionsLoader` — built-in function library, delayed loading according to `<Function>` declaration of XML
- `Variables` / `Variable` / `IndexedVariable` / `VariableBinder(Manager)` / `VariableUpdater(Manager)` — variable system, supports `@var`, `#index`, expression interpolation
- `JSONPath` (`util/JSONPath.java`) - perform path query on the bound JSON data

**command layer** (`ActionCommand` internal class, 20+ `TAG_NAME` in total, observed from `grep TAG_NAME`):
`ActionCommand` , `AnimConfigCommand` , `AnimStateCommand` , `AnimationCommand` , `EaseTypeCommand` , `ExternCommand` , `FieldCommand` , `FolmeCommand` , `FrameRateCommand` , `FunctionCommand` , `GraphicsCommand` , `IfCommand` , `IntentCommand` (Including `Fallback`), `JsonObjectCommand`, `LoopCommand`, `MethodCommand`, `MultiCommand` / `GroupCommand`, `PbrCommand`, `SensorCommand`, `SoundCommand`, `TickListenerCommand`, `VariableCommand`, `BinderCommand`, `VideoCommand`

⇒ Equipped with **`if` branch + `loop` loop + variable state + reflection call + intent dispatch + external command**, which is equivalent to a restricted embedded script VM.

The execution semantics of `ExternCommand`: `root.issueExternCommand(command, numPara, strPara)` (`ActionCommand.java:1086-1090` → `ScreenElementRoot.java:1072-1092`) are eventually transferred to the host callback of `MamlView` / `MamlSurface` - the host (SystemUI/lock screen/desktop) decides what it can do, so the upper limit of **'s capability = host implementation + capability Bit**.

### 7.4 Secure Encapsulation of Cloud Content (Observation)

`util/net/CloudCoder.java`：
- `generateSignature(method, url, params, security)` = `Base64(SHA1( METHOD & path & k=v(sort) & security ))`（`:26-56`）
- `newAESCipher(keyBase64, mode)` = **`AES/CBC/PKCS5Padding`, hard-coded IV `"0102030405060708"`** (`:66-77`)

Supported exception types: `AccessDeniedException` / `AuthenticationFailureException` / `CipherException` / `InvalidResponseException` (`util/net/`) → Description MAML resource is **signed + optional AES encrypted cloud delivery package**.

**risk surface (inference)**: Bypass `RC4` constants are declared in `CloudCoder.java:39` but this class is not used (may be historical/incomplete porting); IV is fixed and hard-coded, the key is `security` parameter → If the same signature key is reused, the encryption strength is limited by key distribution rather than cryptography itself.

### 7.5 Rendering and performance

`RenderThread` / `RendererController` / `RenderUpdater` / `RenderVsyncUpdater` / `FramerateTokenList` / `elements/FramerateController` — independent rendering thread + vsync alignment + element-by-element frame rate budget; `ScreenElementRoot.java` 70KB mainly layout/variables/command distribution/life cycle.
`elements/` Covers: `Text/Image/ImageNumber/DateTime/Circle/Rectangle/Arc/Ellipse/Line/Curve/CanvasDrawer/Paint/AdvancedSlider/Button/List/Mirror/Layer/ElementGroup/VariableArray/WebView/Video/Window/ViewHolder/GLLayer/PhysicallyBasedRendering/MusicControl/SpectrumVisualizer/Timepanel`.
`folme`(8) = MIUI physical animation engine binding; `shader`(6) = GLSL supported.

---

## 8. Other small modules

| package | Number of classes | Content (observation) |
|---|---|---|
| `com.miui.dock` | 20 | Car/dock mode; resources `assets/dock_settings_{fold,phone_sidebar,phone_unsidebar,…}.json`(27-64KB), `dock_tips.zip`(211KB), `phone_wireless_*.json`(72KB), `flip_wireless_*.json`(127-139KB), `normal-{light,dark}(-miui15).json`(18-37KB), `unfold-*.json`, `external{Light,Dark}.json`, `popNotice*.json` — All are MAML/dock card layout resources |
| `com.miui.fastplayer` | 2 | `FastPlayer.java` + `a.java`, corresponding to `libfastplayer.so`; fast media playback (inference: video toolbox/highlight moment preview) |
| `com.miui.ai` | 1 | `service/OperationListCollectService` — Collect user operation sequences (provide training data for apppredict, **infers**) |
| `com.miui.mlkit` | 5 | `mobilerec/bean/{PredictApp,ResultWithMetrics,TrainPlanBean}` + `ranking/mnn/{MNNNetNative,MNNTrainNative}`, see §6.3 |
| `com.miui.monthreport` | 4 | `month_report.db` (`SQLiteOpenHelper`, version 2), the table contains `eventType/eventId/eventTime/moduleName` (`MonthReportProvider.insert`); `b.java` has `Base64`+`URLEncoder`+`UUID`+`JSONObject` → Submit monthly report (contains power related, due to `moduleName` can be set to `monthReport`/power module). `a.java:37` `super(ctx,"month_report.db",null,2)` |

---

## 9. Cloud interface (all hits in the DEX string, and all have code call points)

| URL | Purpose | call point |
|---|---|---|
| `https://adv.sec.miui.com/mnntrain/taskdata` → actually `https://api.sec.miui.com/mnntrain/taskdata` | The **training task plan issues** (POST, appId `5cdd8678-cddf-4269-ab73-48017445bba3`, buried point `ai_request_task`), and returns `BaseResult<TrainPlanBean>` | `eg/j.java:876`(`u(String)`); caller `o3/c.java:200-202`(`h()`) → `AppPredictService.v():467` |
| `https://adv.sec.miui.com/game/pkg` | **game identification/label** (POST native package name list) | `a8/k0.java:161`、`a8/x0.java:88`、`f7/c.java:112` |
| `https://adv.sec.miui.com/game/popGames` | Popular game list | `a8/k1.java:56` |
| `https://adv.sec.miui.com/game/speedParams` | **game acceleration parameters (loadlimitparamsfromnet)** | `gamebooster/service/w.java:283` (`:290` failure log `"loadlimitparamsfromnet failed!"`) |
| `https://adv.sec.miui.com/game/fast_reply` | Quick reply template | `f7/b.java:121` |
| `https://adv.sec.miui.com/game/upgrade/pkginfo` | Game upgrade package information (POST) | `gamebooster/model/a.java:39` |
| `https://adv.sec.miui.com/game2/gameAdv` | Game center advertising space | `ve/k.java:120`、`ve/b.java:291` |
| `https://adv.sec.miui.com/gameTurbo/gcgt/tab/config` / `tab/feed` / `tab/page-detail` / `list/leisureGame` | Game Center tab configuration/information flow/details/casual games | `ve/k.java:121`、`c9/h.java:66,78`、`c9/d.java:325` |
| `https://adv.sec.miui.com/gameTurbo/migamegpt/game-ai/gt-setting-info` | **GameGPT AI assistant settings** (JSON+language+highEnergyVersion) | `gamebooster/aihelper/f.java:52` |
| `https://adv.sec.miui.com/info/layout` / `info/videoAdv` / `user/ac` | Home Page Layout/Video Ads/Users | dex string (outside of this module, not located one by one) |
| `https://flash.sec.miui.com/game/union` | Game intermodal transportation | `antivirus/service/GuardService.java:338`（`antivirus_gameunion_request`） |
| `https://sd-gamespace.sec.miui.com` | Game space H5 (fight `?language=xx_YY`) | `i7/i.java:411` |
| `https://data.sec.miui.com/adv/game_ad/report` | Game advertising reporting | dex string |
| `https://data.sec.miui.com/adv/tail` / `data/upload` | Tail advertising/data upload | dex string |
| `https://data.sec.miui.com/privacy/{get,agree}/v1`、`privacy/latestVersion` | Privacy Agreement Version | dex string |
| `https://api.sec.miui.com/v1/traffic/report` / `data/check` / `redPacket/reportStatus` / `common/whiteList/trafficChargeNum` | Traffic/data verification/red envelope/free traffic | dex string |
| `https://api.sec.miui.com/LBS/geoconv/admin` | Coordinate system conversion (**for** automatic task geofencing) | `earthquakewarning/Constants.java:61` (the same constant is reused in many places) |

**notes**: `adv.sec.miui.com` and `api.sec.miui.com` coexist on `mnntrain` → **infers**: `adv` is the external/advertising domain, `api` is the internal API domain, the old path has not been cleared.

---

## 10. Unsolved mysteries (clearly marked)

1. **`PowerKeeper` The actual FM node**: `userConfigureStatus` → governor/thermal write path in the powerkeeper process is not in this APK (`Unknown`). Powerkeeper APK is required to close the loop.
2. **`Joyose` GPU profile content**: The mapping of the `GPUProfile` field semantics, profile and frequency/voltage returned by `IGPUTunerInterface.getProfiles()` is not in this APK (`Unknown`). `GPUProfile.java` exists but requires `com.xiaomi.joyose` to explain.
3. The real consumer of **`assets/garbage_clean.db`**: This APK has zero references; whether it is a legacy build or cleanmaster gets a copy of the assets through the signature provider (see `lj/c.b()` for the mechanism of `openFileInput`/`context.fileList`) - it cannot be determined from this APK. `Unknown`.
4. **`performance_mode/data.json`(116KB) and `redmi_performance_mode/Ai-Model.json`(119KB)**: No `assets.open("performance_mode/…")` call point, `grep` only hits the `R.string`/`r4.a` key. Is it a pure Lottie/layout resource, or is it accessed by directory name by `FolderResourceLoader` of `MAML`? `Unknown` (the latter is more likely because MAML uses a directory instead of a single file).
5. Model type of **`assets/model` + `assets/dict`**: The native implementation of `mj.a` (should be in `libtensorflowlite_jni` or another JNI) is not expanded this time; `model`(680KB) header is not TFLite flatbuffer The magic number (`TFL3`) needs to be verified separately. `Unknown`.
6. **MAML’s `maml/*/manifest.xml` and `LifecycleResourceManager`**support** pulling new manifests from the network during runtime **: `ResourceLoader` is not seen in the subclass HTTP loader; networking is only available through `WebViewScreenElement` and `issueExternCommand`.** "Remote UI delivery" is implemented by handing the downloaded zip to `ZipResourceLoader` through the host App (SystemUI/Desktop/Theme Store) **—— This APK does not include a downloader. This is an important boundary clarification: MAML within**this APK is the "interpreter", not the "distributor" **.
7. **New version/old version autotask switching condition**: Two sets of Providers exist in the Manifest at the same time. Which set takes effect depends on which authority the caller (`com.miui.home`/settings) chooses. `Unknown`.
8. Who specifically binds `AiService` of **`com.miui.apppredict`**: `miui.permission.APP_PREDICT_VERIFY` (signature level) is required, candidates: `com.miui.home`, Xiaoai. `Unknown`.
9. Does **`libMNNTrain` really perform backpropagation**: `nativeTrainRecModel` returns `float[]` (`[0]` is used as loss), and the behavior is consistent with training; but the training is in the **main process background thread** (`ef.z` Thread pool) execution, **does not see resource restrictions other than power/temperature fuse** (only `battery>=20%`). Actual CPU occupies `Unknown`.
10. The real implementation of **`jg.b.b(exceptionList)` (`cleanMemory`)**: `jg` package is not expanded this time.

---

## 11. One-page quick reference: Mode → Write point

```
Power saving mode on
  ├─ Settings.System.POWER_SAVE_MODE_OPEN = 1
  ├─ be.g  → DisplayFeatureManager.setScreenEffect(24, 60) + user_refresh_rate=60
  ├─ be.c  → Settings.Secure.gxzw_icon_aod_show_enable = 0
  ├─ be.h  → dtmf_tone/sound_effects/lockscreen_sounds/screenshot_sound/delete_sound = 0
  ├─ be.i  → pick_up_gesture_wakeup_mode/gesture_wakeup/wakeup_for_keyguard = 0
  ├─ be.b → Manual brightness -5% (SDK<33)
  ├─ ce.g  → setMasterSyncAutomatically(false) + POWER_SAVE_PRE_CLEAN_MEMORY_TIME=600
  ├─ ce.g → Settings.Secure.location_mode downgrade (SDK≤28)
  ├─ ce.g → MiuiWifiManager.setAntHalf(0) (reflection)
  ├─ ce.g  → powerkeeper GlobalFeatureTable.userConfigureStatus = "ultimate"
  └─ Broadcast miui.intent.action.POWER_SAVE_MODE_CHANGED {POWER_SAVE_MODE_OPEN=true}

Performance mode on
  ├─ Settings.System.POWER_PERFORMANCE_MODE_OPEN = 1
  ├─ ce.g  → powerkeeper GlobalFeatureTable.userConfigureStatus = "performance"
  ├─ Broadcast miui.intent.action.HIDE_MODE_CHANGE {hideModeStatus="performance"}
  └─ Notification channel "com.miui.powercenter.low" (can be closed after 120s)

Balance mode on
  ├─ Settings.System.POWER_BALANCED_MODE_OPEN = 1
  └─ ce.g  → userConfigureStatus = "enhance"

Super power saving/extreme power saving
  ├─ powerkeeper.configure "changeExtremePowerMode" {EXTREME_POWER_SAVE_MODE_OPEN}
  ├─ Settings.System.power_supersave_mode_open
  └─ broadcast com.miui.powercenter.SUPER_POWER {progress=0..3} → com.miui.superpower minimalist desktop

game acceleration
  ├─ bindService(pkg=com.xiaomi.joyose) → IGPUTunerInterface.getProfiles()/checkSupport*
  ├─ Settings: MiuiSettings$ScreenEffect.GAME_MODE (reflection)
  ├─ powerkeeper IFeedbackControl (temperature control feedback)
  ├─ Network parameters: GET adv.sec.miui.com/game/speedParams
  └─ GAME_MODE reset: resetGameMode()

app prediction
  ├─ <filesDir>/rec_model_20230425.mnn (MNN, CPU, 4 threads)
  ├─ Input: [pkg(t-1..t-5) char-id[30], pkg(cand) char-id[30], hour]
  ├─ Output: linear_9.tmp_1 → softmax → P(next)
  ├─ Fusion: MNN ⊕ Bayes ⊕ PBB (reciprocal ranking weighted)
  └─ Training: libMNNTrain, epochs=10 lr=1e-4 batch=128, ≥50 samples, power ≥20%, loss will be rolled back if it deteriorates
```

---

*End of report. All `class name:line number` can be directly located under `_analysis/jadxout/sources/`. *
