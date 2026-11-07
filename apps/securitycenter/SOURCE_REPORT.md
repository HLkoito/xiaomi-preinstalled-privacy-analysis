> **Historical analysis.** This archive includes observations that remain unverified or were later corrected. See the [final report](REPORT.md) and [independent review](REVIEW.md) for the reviewed findings on upload scope, consent, local FBO delegation, emergency SMS encryption, and provider authorization. Source references retain their original decompilation paths. The publication includes the selected files under evidence/.

# MIUI Security Center (MIUISecurityCenter) complete reverse analysis report

> Target: `MIUISecurityCenter-10.4.9-250402.0.1.apk`
> SHA-256：`fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd`（66,917,411 B）
> Method: Static reverse engineering (jadx fully decompiled 10,624 .java / apktool resource decoding / native library ELF static analysis / SQLite structure analysis / REA evidence chain)
> **This article does not contain any real machine dynamic verification**: all "behaviors" are **code static facts** or **inferred**, the three-state annotation is shown below.

## 0. Reading instructions and grading of evidence

| mark | meaning |
|---|---|
| `Observed` | Directly from shipped artifact (file bytes, decompiled source, SQLite structure, signature structure, ELF symbol/string) |
| `Inferred` | Conclusions derived from multiple observations (may be wrong, all with evidence) |
| `Unknown` | No evidence has been obtained, clearly left blank |

**working copy (reproducible)**
| Path | content |
|---|---|
| `_analysis\apk_full\` | ZIP complete unpacking (9231 files; another 305 case conflict items plus `.dup` suffix) |
| `_analysis\apktool_sc\` | apktool 3.0.3 decoding: `AndroidManifest.xml` + all `res/` |
| `_analysis\jadxout\sources\` | jadx (comes with jadx-core direct drive, `tools/JadxDump.java`) full Java source code, 10,624 files |
| `_analysis\findings\` | Various special evidence documents (see §12 Appendix) |
| `_analysis\tools\` | Self-built parser (jieba dictionary, IDF, AArch64 disassembler, AES blasting, etc.) |

**fully derived data**: `findings/dex_strings_all.txt` (212,045 unique strings), `findings/url_usage.tsv` (URL → reference class), `findings/components_all.tsv` (full list of 424 components).

**coverage statement**: This report is based on the **7-channel parallel static reverse** (native library/built-in data assets/list and SDK/security and interception/network and early warning/permissions and privacy/power and games), and there are 3 more Mutual correction between team members has been incorporated into the text (`electricrisk` is actually AI anti-fraud, `garbage_clean.db` is a dead asset, `character.json`/`dict`/`pinyinindex.idf` is not encrypted). **has not done**: real machine dynamic verification, server response packet capture, reverse engineering of external APK such as `com.miui.guardprovider` and `com.miui.cleanmaster`, pseudo-code level decompilation of aarch64 ELF (Ghidra of native REA only supports PE, and has used self-built AArch64 disassembler replacement).

**locates** by problem: ① "What functions are included" → §4 (module summary list + domain by domain); ② "Corresponding implementation logic" → §5 (14 links); ③ "What databases are built in" → §6 (2 packaged SQLites + 20 runtime libraries + plain text vocabulary/rules + certificate keys); ④ "Other information" → §2 (Identity/Signature), §7 (Cloud Interface), §8 (native), §9 (SDK/Advertising), §10 (Security and Privacy).

---

## 1. Quick summary of conclusions (20 items)

1. **This is a ROM preset system application** running as `android.uid.system` and signed by MIUI platform. It is not an ordinary App: `sharedUserId=android.uid.system`, signature certificate `CN=MIUI, O=Xiaomi` (starting in 2011, expiring in 2039), `minSdk 23 / targetSdk 34` (Android 14 compiled). `Observed`
2. **It is a "super toolbox"**: 295 Activity / 67 Service / 22 Receiver / 40 Provider, covering at least **20 business domains** (physical examination and anti-virus, harassment interception and anti-fraud, network and traffic firewall, permissions and privacy, power and performance, storage cleaning, game acceleration, automatic tasks, terminal-side AI Forecasting, disaster/earthquake warning, AI anti-fraud protection, SIM lock, notification bubble, application lock, safe sharing, operator traffic business, TEE trusted UI, advertisement opening..., see the 25-line module table in §4.1 for details).
3. **process model 4**: main process (UI), `:ui`, **`remote` (carrying 100 components, the core of the background business)**, `bootaware` (SIM lock).
4. The **home page/result page is the "card flow issued by the cloud"**: `com.miui.common.card` (Category 60 CardModel) is issued by `https://adv.sec.miui.com/info/layout` JSON Rendering, card type includes **advertisement** (`Adv*CardModel`, `AdvInternationalCardModel`), activities, information, function grid, and ranking. That is, there is an advertising space on the home page of **, and** can be modified by the server at any time. `Observed`
5. **one-click physical examination 36 groups of inspection items** are driven by `assets/scanitem.json` (version 8) data: `manual` 28 groups + `system` 8 groups, each item is mapped to the `com.miui.securityscan.model.**` class and has a weighted score (0~5). Reviews/ratings are compiled by `securityscan/scanner/ScoreManager.java`. `Observed`
6. **self-developed HTTP layer + private signature**: `eg/j.java` (`NetUtil`, 43 KB) sends unified request, signature = `MD5(Base64(k=v string sorted by key + "&" + APP_KEY))`; public parameters include **`m` = MD5**, model/region/MIUI version/channel/versionCode/timestamp of IMEI. `Observed`
7. Two sets of **APPK constants are hard-coded**: `2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9`, `5cdd8678-cddf-4269-ab73-48387445bba6` (used for interface signatures of different business lines). `Observed`
8. **has two built-in SQLite databases**: `res/raw/parse.db` (**plain text, 1043 application directory mappings + 10 system temporary directories**, for deep cleaning) and `assets/garbage_clean.db` (3 table skeletons, 4 The row data **are all obfuscated by stream cipher**, and all **APK have no readers**). There are also **20+ libraries** created at runtime (traffic, firewall, disaster, earthquake, power history, game acceleration, automatic tasks, monthly reports, forecasts, notification graphs, etc.). `Observed`
9. The truly usable "vocabulary/rule base" for **is a batch of clear text assets**: jieba Chinese word segmentation dictionary `assets/dict` (**348,982 words + 35,224 character list**, fully decoded), Chinese pinyin table `pinyinindex.idf` (**20,902 words / 1,336 pinyin string**, fully decoded), SMS identification rules `assets/pattern` (v1.2 regular set), URL 452 whitelists, 224 service numbers, province and city table 34/336, payment security target 211 packages, game list 607 packages, behavior record whitelist 57/43 items (summary by signature). `Observed`
10. **`assets/censor_source` (2,688 B, entropy 7.933) is the encrypted sensitive lexicon**, which is built by decrypting the self-implemented AES of `SensitiveWordDetector::load()` from `liblib-riskcontrol.so`. Tree; **key does not solve** (48 candidates × 5 IV × 2 modes all failed). `Unknown`
11. **18 native libraries** (18 each for arm64 + v7a): The only hard-coded network endpoint is in `libtcp.so` → `https://net-assistant.10046.xiaomimobile.com/v1/traffic/directives`, command format **base64(JSON)**, capabilities include SMS template matching, `getSubscriberId`, `setImsi`, `collectUserData`. `Observed`
12. **`libtcp.so` contains 13 segments of `PROGUARD_*` fragmented constant **. The extracted segments of base64 are Chinese spam text messages (such as "I will never stop, think clearly"), which also serves as AES key material and text message corpus. `Observed`
13. **`libsec-common-utils.so` is "Key Center + Caller Access"**: Provides 6 `ConfigManager.get*SecretKey` (Encode/Decode/Http/Uuid/Rubbish/NetworkAssistant), and uses `getPackageInfo().signatures`+`sha256` Verify the caller package name whitelist (`com.miui.securitycenter`, `com.miui.cleaner`, `com.miui.cleanmaster`, `com.miui.global.packageinstaller`); the hard-coded certificate fingerprint `C8:A2:E9:…:2A:B8` and **this APK do not match the actual signature** (the pin is another certificate). `Observed`
14. **client-side AI is a real**: `assets/rec_model_20230425.mnn` (Alibaba MNN FlatBuffers, 9 embedding + 5 linear + MaxPool, **second category "Next App" score**) + `assets/character.json` (40 character vocabulary) + `libMNN*`/`libMNNTrain` → **supports device-side incremental training** (`MNNTrainNative.nativeTrainRecModel`, samples <50 are discarded). `Observed`
15. **`libmicontinuity_sdk.so` is the cross-device channel** (`ChannelImplBySocket::{SendFile,SendBytes,ContinueSend}` + `NInputReader_read/skip`). There is no SSL**in the** import symbol; `libxcrash_dumper.so` is not JNI. The library is the independent crash dump process **(`__libc_init`, `JNI_OnLoad`, `ptrace`+`/proc/self/maps`) that was pulled up by**. `Observed`
16. **native layer has no packing and no anti-debugging** (bounded negation): 18 libraries are all stripped + BIND_NOW, but `.rodata` retains complete C++ RTTI and source file paths; `frida/xposed/magisk/TracerPid` has zero hits. `Observed`
17. The **network configuration is deliberately relaxed for**: `network_security_config.xml` global `cleartextTrafficPermitted="true"`, and 2 self-signed/private CAs are imported as trust anchors for the `miwifi.com` series domain names (`wlan_speed.crt` RSA-1024 self-signed `CN=www.miwifi.com`; `wlan_speed_new.crt` `MIWIFI ROOT CA`, RSA-2048, valid until **2120 (**). `Observed`
18. **advertising and statistical links are complete**: `adv.sec.miui.com` (card/game/video advertising), `data.sec.miui.com` (buried point reporting), `sdkconfig.ad.xiaomi.com` (advertising SDK configuration), `com.miui.msa.util.MsaUtils` (MSA alliance OAID), `assets/placement_id` = **Facebook advertising slot** `fb:196567740764356_196569107430886`, `com.miui.systemAdSolution` (open screen advertising Provider), `SystemAdContentProvider` (authority `com.miui.securitycenter.splashAd`). `Observed`
19. **Extremely high density of privacy-related capabilities**: read/write text messages and call records, read contacts and audio, `MASTER_CLEAR` / `REBOOT` / `FORCE_STOP_PACKAGES` / `CLEAR_APP_USER_DATA` / `PACKAGE_VERIFICATION_AGENT` / `MANAGE_USB`; equipped with **accessibility services** (`AntiMsgAccessibilityService`), **Notification Monitoring** (`BubblesNotificationListenerService` ), **Screen/Window Overlay** (`SYSTEM_ALERT_WINDOW` ), **Input Method Monitoring** (`df.h` Monitoring `default_input_method` ), **Clipboard/Input Privacy Mode**, **Application lock**, **Privacy space**, **Privacy blur**.
20. **privacy data flow has been reviewed (2026-11-06 added §10.5)**: The strongest evidence is that the **advertising request carries multiple associated identifiers** (`x`=Android ID, `oa`=OAID, `m`=IMEI-MD5, `deviceId`=SHA1 (IMEI+AndroidID+serial number), `eg/j.java:569-572` + `com/miui/antivirus/result/f.java:272`), and the "personalized advertising switch" is only a request parameter, **will not stop splicing**; secondly, the **privacy risk detection page will trigger the "non-system application list" to upload** (`xc/h.java:13` is fully filtered with `getInstalledPackages(0)` `FLAG_SYSTEM`, **are not screened (** is visible on the desktop). The remaining links all have switches, pop-ups, or local short circuits (see the classification table in §10.5 for details), and cannot be generally classified as "collection without consent."

---

## 2. Target objects and construction facts

### 2.1 Identity
| item | value |
|---|---|
| package name | `com.miui.securitycenter` |
| versionName / versionCode | `10.4.9-250402.0.1` / `40001049` (internally consistent with `BuildConfig.VERSION_CODE`) |
| SDK | min 23、target 34、compile 34（Android 14） |
| `sharedUserId` | **`android.uid.system`** |
| application | `com.miui.securitycenter.Application` (inherits `com.miui.common.h`) |
| backupAgent | `com.miui.antispam.service.backup.SecurityCenterBackupAgent` (harassment interception configuration backup) |
| Icon can be hidden | `meta-data app_hide_enable=true`、`disable_notifications_when_app_hide=false`；`meta-data miui.support_full_scan=true` |
| Native library loading | `extractNativeLibs=false` (directly mmap APK within .so) |
| network strategy | `@xml/network_security_config` (see §10.3) |
| language | `default` + `zh-rCN`(607 KB) / `zh-rTW`(614 KB) / `zh-rHK`(611 KB) + `bo-rCN`(hidden, 1.27 MB) + `ug-rCN`(dimensional, 901 KB) |
| Build | AGP 7.3.1（`META-INF/com/android/build/gradle/app-metadata.properties`） |
| Start the portal | `com.miui.securityscan.MainEntryActivity` (**`android:enabled="false"`**, controlled by ROM/cloud control switch; intent-filter: MAIN / `miui.intent.action.SECURITY_CENTER` / `mi.quicksearch.intent.action.RETRIEVE` / VIEW) |

### 2.2 Volume composition
| composition | size |
|---|---|
| `classes.dex` / `classes2.dex` / `classes3.dex` | 8.14 MB / 8.84 MB / 7.54 MB (total 24.5 MB) |
| `resources.arsc` | 8.13 MB (9231 resource entries) |
| `assets/` | 248 files / 8.97 MB (`dict` 3.64 MB max) |
| `lib/` | 36 .so (arm64 18 + v7a 18), about 15 MB for a single architecture |
| `res/raw/` | 38 (including `parse.db`, 2 certificates, video/audio/shader) |
| `META-INF/` | v1 signature (`PLATFORM.SF`/`PLATFORM.RSA`) + Kotlin coroutine SPI registration |

### 2.3 Code size
- Top-level class **10,642** (including inner class 25,031), method **166,513**, instruction **4,437,463** (jadx `RootNode` observation).
- Confusion: **Business package retains readable naming** (`com.miui.securityscan.*`, `com.miui.networkassistant.*`...), **Infrastructure and third-party SDK were replaced by R8 Renamed to single/double letter package** (`a`…`zm`, `eg`, `xe`, `m7`, `u3`, `vf`, `m3`, `i9`, `ra`, `lj`, `mj`, `cl`, `am`, `ea`, `ia`...), so "the function is readable and the bottom layer needs to be traced".

---

## 3. Overall architecture

### 3.1 Process and life cycle (`Observed`)
| process | Number of components | bear |
|---|---|---|
| `com.miui.securitycenter` (main) | UI and few components | All Activity/UI, `WidgetProvider` entrances |
| `com.miui.securitycenter:ui` | 6 | `com.miui.bubbles.settings.*` (bubble setting) etc. |
| **`com.miui.securitycenter.remote`** | **100** | **backend business core**: 40 Providers (antispam/autotask/earthquake/disaster/gamebooster/networkassistant/permcenter/powercenter/warningcenter/xsof/idprovider…), most Service/Receiver |
| `com.miui.securitycenter.bootaware` | 3 | `com.miui.simlock.*` (SIM lock, starts immediately after power on) |

`Application.onCreate` (`com/miui/securitycenter/Application.java:711`) judges the process according to `getPackageName()` and initializes them respectively:
- Main process: `R()` (UI basic), `W()`, `D()`, `com.miui.common.f.g()`, `hh.b.m()`;
- `:ui`：`AnalyticsUtil.initMiStats()`、`of.e.r()`；
- `remote` (the longest branch): `AnalyticsUtil`, `w.E()`, `l.a()`, `ConnectivityChangeJobService2.s()`, `c3.d.g/c()`, `SimCardHelper.asyncInit()`, **`AppOpsUtilsCompat.autoOptmize()`** (automatic permission optimization), `k.a().b()`, `AiGuardUtils` (reflective call AI uses electricity to protect, and if supported, pull up `com.miui.electricrisk.AiGuardSceneService`), `xc.p.h()`/`df.h.c()` (international version/domestic version split), `df.h.E()` + monitoring `default_input_method` (**Input Method Privacy**), `com.miui.simlock.c.p()`, `j.d(ctx,"application")`, `d0/Z/X`, `p3.g.f()`, Idle Processor `U()` and other 20+ items; for `cappu`/`clover` models additionally run `new a().execute()`.

### 3.2 Component scale and exposure
| Type | Quantity |
|---|---|
| activity / activity-alias | 293 / 2 |
| service | 67 |
| receiver | 22 |
| provider | 40 |
| Custom `<permission>` | All `signatureOrSystem`/`signature` (very few `normal`) |
| `uses-permission` | About 150 items (including a large number of MIUI private system permissions) |

> See `findings/components_all.tsv` for the complete 425-line component list; see §10.1 for the list of exported surfaces and risk ratings.

### 3.3 Core infrastructure (6 frameworks for determining "capability ceilings")
| frame | location | Purpose |
|---|---|---|
| **card system** | `com.miui.common.card` (category 60, `models/` 49 CardModels) | Home/Results Page = Cloud JSON Card Flow. Type: `FuncGrid6CardModel` (functional grid), `FuncTopBanner*` (Banner), `Adv*CardModel` (**advertising**: big picture/three pictures/website/column Table/International), `Activity*CardModel` (activity), `NewsListBannerCardModel` (information), `ListTitleFlowRankCardModel`/`ListTitleConsumePowerRankCardModel` (traffic/power consumption ranking), `ScanResultTop/BottomCardModel*` (physical examination results) |
| **remote configuration** | `com.miui.common.persistence.RemoteProvider` + `remote_provider_preferences` | Cross-process key value configuration (`RemoteProvider.java:264`) |
| **HTTP layer** | `eg.j`（`NetUtil`，43 KB）+ `xe.a`（AdNetUtils） | Unified request, signature, UA, failed retry, buried callback `p4.i` |
| **dynamic UI engine** | `com.miui.maml` (Class 221) | MAML card/animation rendering (`assets/maml/**/manifest.xml` is its resource), supports scripts and expressions →** can be delivered to the cloud UI Description ** |
| **buried point** | `com.miui.analytics`(10) + `com.xiaomi.onetrack`(179) + `com.xiaomi.analytics`(12) | Dual channel statistics, local queue library `events/events_cloud/monitor` |
| **terminal side AI** | `com.miui.mlkit.mobilerec.ranking.mnn.*` + `libMNN*` / `libtensorflowlite_jni` | App prediction/ranking, including client-side training |

### 3.4 Cloud-driven "layout/advertising" link (`Observed`)
```
Home/Result Page Request → POST https://adv.sec.miui.com/info/layout
   Parameters: functionId / device and version public parameters / isPersonalizedAdEnabled / cv=3 / ror / rbr
   Signature: sign = MD5(Base64(sortedParams + "&" + APP_KEY))
   Callback: p4.i("antivirus_scanresultcontrol" | "optimizemanage_omdatamodel" | ...)
Return → CardModelMaker parsing → Generate CardModel list → RecyclerView rendering
```
Call points: `com/miui/antivirus/result/f.java:284`, `c0.java:263,283`, `com/miui/optimizemanage/optimizeresult/e.java:244,249`.
→ The "function entrance/advertising/information" on the **homepage is completely determined by the server**, and only the bottom-up layout (`FuncGrid6CardModel`, etc.) is retained locally.

## 4. Panorama of functions (by business domain)

### 4.1 Module summary
| # | business domain | Package (number of classes) | Number of activities | What users can do |
|---|---|---|---|---|
| 1 | **Physical examination/optimization** | `securityscan`(133) + `common.card`(60) | 10 | One-click physical examination, scoring, result card, whitelist, settings |
| 2 | **virus detection** | `antivirus`(82) | 9 | Full/quick scanning (engine in external GuardProvider), virus database update, isolation/processing, payment security guard |
| 3 | **harassment interception** | `antispam`(72) + `maml` card | 15 | Black/white list, keywords, 21 policy chains, number tags, interception records, backup |
| 4 | **Anti-fraud and risk control** | `antifraud`(5) + `riskcontrol`(1) + **`electricrisk`(27)** | 6 | Payment page monitoring, sensitive word detection, AI anti-fraud scene protection (phone/SMS/application/webpage/screencast) |
| 5 | **Red envelope assistant** | `luckymoney`(54) | 6 | WeChat/QQ red envelope reminder, automatic jump, sound prompt |
| 6 | **Alarm auxiliary** | `policeassist`(20) | 1 | Emergency alarm guidance, location information assistance |
| 7 | **Network and traffic** | `networkassistant`(417) | 21 | Traffic statistics/package/calibration, network control (firewall), hotspot quota, roaming, traffic package purchase, VPN, status bar traffic |
| 8 | **Disaster warning** | `warningcenter`(81) | 10 | 12379 Disaster warning subscription/push, Mijia linkage, defense guide |
| 9 | **Earthquake Early Warning** | `earthquakewarning`(96) | 12 | Early warning display, subscription city, countdown/SOS sound, volunteer sensor |
| 10 | **AI Anti-Fraud Guard** | `electricrisk`(27) | 6 | Identification and dissuasion of phone/text messages/applications/webpages/screen casting fraud scenarios (the module name "Power Risk" is a historical naming and has nothing to do with the battery) |
| 11 | **SIM card lock** | `simlock`(25) | 9 | SIM lock query/setting/PIN, power-on verification |
| 12 | **notification bubble** | `bubbles`(49) | 2 | Notification bubbles, application and settings management |
| 13 | **Permission Center/Privacy** | `permcenter`(162)+`permission`(16)+`privacyapps`(10)+`applicationlock`(34)+`blur`(18)+`appmanager`(22)+`devicepermission`(7)+`combinepermission`(4)+`idprovider`(11) | 53 | Per-application/per-permission authorization, self-start, privacy risk detection, private space, application lock, thumbnail blur, privacy input, shutdown password, cross-terminal permissions, OAID authorization, application management |
| 14 | **Power and performance** | `powercenter`(158)+`superpower`(23)+`powerkeeper`(5) | 27 | Power saving/performance/balanced/super power saving, power consumption ranking, abnormal power consumption, charging protection, battery health |
| 15 | **Storage Cleanup** | `optimizecenter`(40)+`optimizemanage`(29)+`cleanmaster`(6)+`cleaner`(1)+`firstaidkit`(28) | 8 | Junk cleaning, deep cleaning, large files, application slimming, one-click first aid |
| 16 | **game acceleration** | `gamebooster`(280)+`gameturbo`(6)+`gamecenter`(2) | 29 | Game recognition, performance/touch/image quality/HDR/4D/shoulder key/voice change/beauty/quick reply/floating window/anchor mode |
| 17 | **automatic task** | `autotask`(125) + `powercenter.autotask`(22) | 12 | Condition → Action Automation (22 types of conditions × 28 types of actions) |
| 18 | **Application Forecast/Device Side AI** | `apppredict`(25)+`mlkit`(5)+`ai`(1) | 6 | Desktop folder recommendation, Widget recommendation, classification, client-side training |
| 19 | **dynamic card (MAML)** | `maml`(221) | — | Render MAML cards and animations delivered from the cloud/local |
| 20 | **Safe sharing** | `zman`(8) | 3 | Privacy desensitization before sharing (`ShareActivity`/`PrivacyProtectSettingsActivity`) |
| 21 | **wake-up path interception** | `wakepath`(2) | 1 | Chain startup confirmation page (`ConfirmStartActivity`) |
| 22 | **Trusted UI (TEE)** | `tvm`(13) + `securitykey`(4) | 1 | `TvmManagerService` + `ITvmManager` AIDL + nonce/URL model; `EncryptionUtils` |
| 23 | **operator traffic service SDK** | `sdk.tc`(9) | — | `TcManager`/`TcPlugin`/`TcDirection`/`SmsSender` (package calibration command library) |
| 24 | **Advertisement/open screen** | `systemAdSolution`(3)+`international`(4)+`msa`(1) | 1 | Open screen advertising Provider, international version advertising switch, MSA OAID |
| 25 | **Infrastructure** | `common`(120)+`hybrid.accessory`(49)+`net`(2)+`sec`(1)+`push`(2)+`analytics`(10)+`devicepermission`… | — | Card/Persistence/Network/Buried Point/Push/Accessory Interconnection |

> Note: The number of activities comes from `AndroidManifest.xml` statistics grouped by packages; the number of classes comes from jadx tree file counts. `com.miui.dock` (20, Dock floating bar), `com.miui.applicationmanagement` (2), `com.miui.googlebase` (1, GMS setting), `com.miui.appcompatibility` (1) are small modules, see §4.6.

### 4.2 Physical examination and antivirus (securityscan / antivirus)
**One-click physical examination (observation)**: Entrance `MainEntryActivity → MainActivity`, UI consists of five forms of `ui/MainContentFrame*` (`MainContentFrame`/`…Fold`/`…Lite`/`…Pad`/`ScanContentFramePad`) to achieve the same `wf.a` Interface; scan engine in `securityscan/scanner/`:
- `k.java`(35 KB)/`m.java`(21 KB)/`n.java`(15 KB)/`o.java`(29 KB) is responsible for scan scheduling and concurrency; `ScoreManager.java`(17 KB) Responsible for **scoring and score interval**; `CacheCheckManager.java` is responsible for calling the external cleaning engine; `IncrementalScanObserver.java` does incremental scanning.
- Check item **fully data-driven**: `assets/scanitem.json` v8 → `manual` 28 groups + `system` 8 groups, each item `<key, name=model class, version=[alpha,development,stable], score>`.
  - `manual` group (28): `VIRUS` , `CLEANER_SHORTCUT` , `DARK_MODEL` , `NETWORK_SWITCH+VIRUS_CLOUDSCAN+CLEANER_CLOUDSCAN+GARBAGE_LIB+FLOW_NOTIFICATION+TELECOM_OPERAOTR+AUTO_UPDATE` , `GARBAGE_CLEAN` , `MI_SIM_LOCK` , `DEFAULT_LAUNCHER/DIAL/MESSAGING/BROWSER/CAMERA/GALLERY/MUSIC/EMAIL` , `DATA_FLOW` , `KEY_SYNC` , `PAYMENT_RISK` , `SAFE_MODE` , `FIND_DEVICE` , `FLOW_VERIFY` , `FLOW_VERIFY_FAILED` , `CLOUD_SERVICE` , `MIFI_INSURANCE` , `MIUI_UPDATE` , `FIRST_AID_KIT` , `PERMISSION_ROOT` , `APP_UPDATE` , `PERMANENT_NOTIFICATIONBAR` , `FLOW_PURCHASE` , `CLOUD_SPACE` , `FLOW_RANK` , `MI_INPUTMETHOD`, `CONSUME_POWER_RANK`, `APP_LOCK`, `SHUT_DOWN_PASSWORD`.
  - `system` group (8): `VIRUS_LIB`, `SYSTEM_AUTO_DOWNLOAD`, `INSTALL_MONITOR`, `DEV_MODE`, `USB`, `MMS`, `CHAIN_START`, `GMS_OFF`.
  - Each item corresponds to `com.miui.securityscan.model.system.*` / `model.manualitem.*` (`model/` has 62 categories in total).
- Result page = Cloud card: `ScanResultTopCardModel`/`ScanResultBottomCardModelNew` + `adv.sec.miui.com/info/layout` (`antivirus/result/f.java:284`).

**Virus killing (observation)**: `antivirus/` Next `service/GuardService` (resident guard, `flash.sec.miui.com/game/union` Also included), `service/VirusAutoUpdateJobService` (regular update of virus database), `result/*` (result page and advertising card), `ui/AntivirusAnimFrameLayout` (scan animation `R.raw.antivirus_animation1/2`), `SettingsActivity`. Local/cloud determination and collaboration with `com.miui.guardprovider` (external APK, including Tencent TMS/AVL engine): `content://guard` + permissions `com.miui.guardprovider.permission.Guard_Provider` (this APK only has 8 GuardProviders Client class, **does not contain engine**).

### 4.3 Harassment interception and anti-fraud (antispam / antifraud / riskcontrol / luckymoney / policeassist)
**harassment interception (observation)**:
- 15 activities: `antispam_call_intercept_settings`, `antispam_msg_intercept_settings(_v12)`, `antispam_mark_number_settings`, black and white lists, keywords, interception records, etc. (`res/xml/antispam_*.xml`).
- Rule engine: `assets/pattern` (v1.2, `lj/c.java:22` read line by line) defines the characteristics of text messages such as "importance/phishing/invoice/remittance/subscription/verification code/call charges/official number"; word segmentation uses `assets/dict` (jieba) + pinyin `pinyinindex.idf`; sensitive words are composed of **native** `SensitiveWordDetector` (Trie) of `liblib-riskcontrol.so` + `assets/censor_source` (AES encrypted vocabulary, unsolved).
- Data: `AntiSpamProvider` (authority `com.miui.antispam`, `remote` process), `MessageInfo.db`; protobuf for backup/synchronization (`assets/protobuf/antispam.proto`: BlacklistEntry/WhitelistEntry/KeywordEntry/PolicyEntry/SwitchEntry/NotificationEntry/AutoUpdateLibrary/SimsShareSettings/ReportedNumberGuide), `backupAgent=SecurityCenterBackupAgent`.
- Number capability: `service_num.json`(224) + `white_url.json`(452) + `address.json`(34 provinces/336 cities) + `api.sec.miui.com/common/whiteList/*`; URL detection `url-detect.engine.mi.com/url/detect` (`antispam/policy/b.java`); number mark reporting `eps.caict.ac.cn` (`com.xiaomi.security.xsof`, `SensitiveWordDetectProvider` of Provider `com.miui.riskcontrol` are also in this link).
- Interception execution: `antispam/service/AntiSpamService` (resident, `remote` process) + system `com.miui.permission.MANAGE_SMS_INTERCEPT` permissions + `startInterceptSmsBySender` (see §5.5).
- There are only two (observed) **queries actually implemented by**: `content://antispam/sms_judge` (3 parameters) and `call_judge` (5 parameters) → return `MatrixCursor` (`smsBlockType`/`callBlockType`); and `url_judge`, `service_num_judge`, `call_transfer_intercept_judge` and **are only registered but do not implement** (`AntiSpamProvider.java:904-906` falls into default and returns null) → These three items are "interface placeholders".
- **authoritative number data is not in this APK (observation)**: the real black and white list/keyword is in the system Provider `miui.provider.ExtraTelephony.Phonelist/Keyword` (`h2/b.java:84-145`); local `AntiSpam` DB (v10, 7 tables, build table SQL exists `res/values/strings.xml` `create_table_*`) mainly does cloud synchronization/reporting/dual SIM/ringback tone.
- **backup (observation)**: `SecurityCenterBackupAgent` (`miui.app.backup.FullBackupAgent`) + Lite protobuf (`assets/protobuf/antispam.proto` 9 messages are aggregated into `AntiSpam`, each with `guid/luid`), `onFullBackup/onDataRestore` reads and writes.

**anti-fraud and sensitive words (observation)**: `antifraud/PayActivityMonitorService` (`:remote`, monitoring `PHONE_STATE` and `miui.action.mms.SCAN_RISKY_URL`) + `AntiFraudReceiver`; the "transfer page monitoring" target is defined by `assets/anti_fraud_pay_activity` (3 groups) - hit WeChat `RemittanceUI`, Alipay 4 `transferapp` Activity, QQ `qwallet`. `riskcontrol/SensitiveWordDetectProvider` (authority `com.miui.riskcontrol`, `com.miui.security.permission.DETECT_TEXT`) **only serves two packages**: `com.android.camera`, `com.miui.mediaeditor` (hard-coded whitelist, `SensitiveWordDetectProvider.java:24-25`), protocol `call("METHOD_SENSITIVE_WORD_DETECT", text, null)` → `Bundle.getBoolean("hasSensitiveWords")`; `setAssetManager(getAssets())` after `System.loadLibrary("lib-riskcontrol")` gives the entire assets to native (lexicon `assets/censor_source` is decrypted by native, see §8). For the payment status monitoring of the payment security list (`safepay_*`) and `GuardService`, see §5.2.

**red envelope assistant (observation)**: `luckymoney/` (6 Activity, `provider/LuckyMoneyProvider`, `upgrade/LuckyMoneyHelper`); **does not require accessibility services**——`LuckyMoneyMonitorService` binds **with APK in GameBooster Notification agent** `com.miui.gamebooster.service.NotificationListener` / `ISecurityCenterNotificationListener.Z2(callback)` (`LuckyMoneyMonitorService.java:73-82,312`); "Auto grab/quick open" = screen is not locked + switch is on + when the front desk is not in the restricted list, directly `pendingIntent.send()` triggers WeChat/QQ notifications contentIntent (`ui/view/PendingIntentRunnable.java:13-23`), downgraded to lock screen card + unlock replay when lock screen; sound effect `R.raw.hongbao_arrived.wav`/`lucky_money.mp3`/`one..ten.mp3`; `api.sec.miui.com/redPacket/reportStatus` reported.

**alarm auxiliary (observation, reviewed and corrected according to 2026-11-06)**: `policeassist/ui/PoliceassistGuideActivity` + `warningcenter.policeassist.PaService`; take the precise longitude and latitude during emergency call → `Geocoder` reverse check text address → ①`https://gateway-papa.cloud.weizhi.com/papa/upload`: The load is **AES/CBC/PKCS5 encrypts** (key hard-coded `HPLvLMJnR6vvuS9VNkpj7DYb`, `PaCommon.java:5`) and then passes through **RSA** Processing (`PaUtils.java:97,353,363`), **is not plaintext**; ②`https://eps.caict.ac.cn/eps/collect` (Institute of Information and Communications Technology EPS, SM3 signature, 14 fields `source/caller/called/device/method/type/lng/lat/hAccuracy/vAccuracy/altitude/address/time/extra/sign`) **is used for emergency call location reporting, not number marking**; ③ When there is no local number, downgrade and send the text message to `106903590009`. Third-party domain name ≠ data exported, server ownership has not been verified.

### 4.4 Network and traffic (networkassistant)
- **20 user functions** (`ui/NetworkAssistantActivity`, action `miui.intent.action.NETWORKASSISTANT_ENTRANCE`): Traffic overview, traffic ranking, package settings, operator template, traffic calibration (SMS/webpage/manual), traffic reminder and over-limit disconnection, daily rental card, **Network control (mobile/ WLAN/roaming three tables)**, default policy for newly installed applications, background networking restrictions, data saving, hotspot limit, international roaming, data package purchase, operator balance/arrears query, MIUI VPN (including Xunyou), real-time traffic in the status bar, statistical exception prompts, network diagnostic prompts, virtual SIM.
- **traffic statistics link (observation)**: The data source is `NetworkStatsManager` (`miui/securitycenter/net/MiuiNetworkSessionStats`), press `Bucket.state==2` to disassemble the front/backend; use `TrafficStats` + `libnap.nativeGetUidIfaceStat` for real-time measurement; and change **`Settings.Global.netstats_uid_bucket_duration` to 3600000** (`TrafficManageService.java:1273`) to obtain hourly granularity.
- **firewall implementation (observation, important)**: **does not use iptables or netd to directly connect to**, but reflects `miui.security.SecurityManager.setMiuiFirewallRule(pkg, uid, rule, type)` (`firewall/impl/MiuiNetdFirewall.java:177-202`, compatible with old and new dual signatures), rule `Init=-1/Allow=0/Restrict=1/Alert=2`, type `MOBILE=0/WIFI=1/ROAMING=2/ALL=3/OTHER=4`; intercept callback system broadcast `miui.intent.action.FIREWALL`; roaming default `Restrict` (reverse whitelist); rules are persisted in encrypted private files `*_fire_wall_config2.db`, `fire_wall_wifi_config.db`, `fire_wall_roaming_config.db`.
- **package calibration (observation, reviewed and corrected on 2026-11-06)**: `com.miui.sdk.tc.TcManager` (`SEC_KEY="A2FscFVdX1+ULfEz/TTPQVNRXE+lzSe2"` hard-coded, `TcManager.java:17`) → `System.loadLibrary("tcp")`; `libtcp.so` built-in "province × operator × brand → SMS command "library (`TcDirection{sendNumber,receiveNumber,direction,cmdType}`, `cmdType` bit mask 1/2/4); before execution `startInterceptSmsBySender()` allows the system **to intercept the operator's receipt SMS**. To report SMS messages `api.sec.miui.com/v1/traffic/report` and **, you must first pop up the privacy confirmation box** (`TcSmsReportFragment.tcSmsReportDeclare()` → `OptionTipDialog`'s "upload" button, `TcSmsReportFragment.java:416-430`), and the user must confirm `reportSms()`; `getEncryptPhoneNumber()` ** in the code is just used `*` covers the middle bit, which is masking rather than encryption. The original text of the text message ** is not desensitized.
- **traffic service request field (observation)**: The request body of `api.miui.security.xiaomi.com/netassist/flow/queryflow` contains `imsi`, `iccid`, `phonenum`, `zipCode` (city code), `spType` (operator), `monthused` (used this month); **'s field named `imei` is actually `DeviceUtil.getAndroidId("<fixed UUID>")`, which is a hash derived from AndroidID + fixed business string, is not true IMEI** (from `WebApiAccessHelper.java:80` in multiple places) - conclusions cannot be drawn based on field names.
- Interface: `api.miui.security.xiaomi.com/netassist/flow/queryflow`, `api.sec.miui.com/v1/traffic/report`, `api-flow-intl.10046.xiaomimobile.com/*`, `net-assistant.10046.xiaomimobile.com/v1/traffic/directives` (native command).

### 4.5 Early warning and protection (warningcenter/earthquakewarning/electricrisk/monthreport/bubbles/simlock)
- **disaster warning (observation)**: `DisasterDBHelper` (`warning_center_disaster.db`: `area/disaster/subscribe`, CAP field); area code comes from `api.sec.miui.com/12379/warning/getRegionInfo` (SALT hard-coded), warning data **SHA1WithRSA Verify** (`DISASTER_PUBKEY`, discard if verification fails); the only push source for **is MiPush** (`com/miui/push/XiaomiPushReceiver.java`, appId `2882303761517330652`, module `12379Warning`/`accurate-warning`/`MijiaWarning`/`earthquakeWarning`/`monthReport`/`wakePath`/`cloudControl`); subscription=topic `12379W_ + MD5(districtId+UUID) + _ + Level bit mask` (default strong reminder only accepts red = 8, system reminder only accepts red | orange = 12); 71 icons + level bit mask `1/2/4/8` (blue/yellow/orange/red); `warning_center_disaster_guide_text` gives four-color defense guide for 11 types of meteorological disasters; there is also Mijia linkage `warning-mijia.sec.miui.com`.
- **earthquake early warning (observation)**: `earthquake_warning2.db` (`area/earthquake/subscribe`, including magnitude/latitude and longitude/epicenter/intensity/focal depth/warning time/signature); data source `srv.sec.miui.com/earthquake/warning/{records,signature,supportCityCode}`; **is also issued by MiPush and used `EARTHQUAKE_PUBKEY` Signature verification**; the terminal side only does "signature verification → `IntensityTransformer` calculates local intensity → compare with user threshold → notification/full screen + countdown"; `EARTHQUAKE_WARNING_MAIN` export; **volunteer program**: `/earthquake/volunteer/{register,cancelRegister,countVolunteers,sensor/upload}` reports the local acceleration sensor (`EarthquakeWarningService` resident). The early warning source has only one hard-coded `"ICL"` code (`EarthquakeWarningManager.java:718`) in APK, which is determined on the server side.
- **"Electricity risk" module is not worthy of its name (observation, important correction)**: `com.miui.electricrisk` (27 categories / 6 Activity) has nothing to do with the battery - full module `BatteryManager`/`sysfs`/`EXTRA_TEMPERATURE` **zero hit**; it is **AI anti-fraud guard** (copywriting label `system_anti_fraud_dialog_message`, the card covers four types of fraud scenarios: phone calls/text messages/applications/webpages/screen sharing, `ElectricProtect{Phone,Mms,App,Screen}Activity` corresponds to them respectively). `AiGuardSceneService` **only transfers scenes to**: send `COMMAND(1/2)` + `PHONE_SCAM_SCENE(1-4)` + `VIDEO_SCAM_SCENE(1-2)` + `CAPABILITIES` (`:212,367-370`) to `com.miui.guardprovider/.aiguard.AiGuardService`, WeChat → 3/1, QQ → 4/2. The real model is on the GuardProvider side; `AiGuardUtils` is supported with reflection detection and `startService` (`Application.java:788-800`). `Command.java` is an empty annotation and does not participate in instructions. **This module has nothing to do with MNN** (MNN belongs to `mlkit.mobilerec`). Privacy Notice `privacy.mi.com/AIalert/%s`.
- **monthly report (observation)**: `monthreport/` Category 4 + `MonthReportProvider` + `month_report.db(report_json)`; report `data.sec.miui.com/data/upload`, check `api.sec.miui.com/data/check`; **but this build is hard-coded to close**: `ma/c.java:4-6` `a(){return false;}` makes `ma.c.a()` constant false → the upload will never start (if it is reachable, it will be a Base64 message, appId `5fdd8678-…bba7`).
- **bubble (observation)**: `bubbles/` 49 categories, **self-developed floating window** (`WindowManager.LayoutParams(-1,-1,2002,16777256,-3)`, type=2002 `TYPE_SYSTEM_ALERT`, `BubbleController.java:141-166`; **0) `BubbleMetadata`**) + `BubblesNotificationListenerService` (notification listening) + `BubblesService`/`BubbleRemoteService` + `:ui` Two settings for the process Activity; picture cache `pic_db.db(notification_table,pic_table)`.
- **SIM lock (observation)**: `simlock/` Class 25, `bootaware` process + `directBootAware` (runs before unlocking), 9 activities, `BootCompleteReceiver`; **PIN Reversible encryption disk**: `/data/user_de/0/<pkg>/files/encryped_pin.pwd` (IV‖ AES-GCM ciphertext, JSON `iccid→{pin,…}`), Keystore alias `key_pin_alias` (`simlock/a.java:164-226`, **is not set `setUserAuthenticationRequired`**); Verification PIN/PUK uses reflection `ITelephony.supplyPinReportResultForSubscriber`/`supplyPukReportResultForSubscriber`; **This module does not have any cloud interface**.

### 4.6 Permissions/privacy/application management (permcenter, etc.)
- The **architecture is a "three-body" (observation)**: ① Permission manager UI (this APK's `permcenter`+`permission` SDK, 115 `PERM_ID_*`); ②**pre-installed backend `com.lbe.security.miui`** (`ContentResolver.call("content://[<userId>@]com.lbe.security.miui.permmgr", <methodCode>, …)`, method code 3/4/5/6/7/8/9/12/15/18); ③ System service `miui.security.SecurityManager` + `AppOpsManager` (MIUI private op `10000–10053`, such as `AUTO_START=10008`, `BOOT_COMPLETED=10007`, `GET_OAID=10037`, `DEVICE_ID=10050`, `MODE_ASK=5` is an extension of MIUI).
- **writes double write (observation)**: first write `setModeNow`(method 9)/`setApplicationPermission`(method 6) permmgr, then use reflection `IPackageManager.grantRuntimePermission/revokeRuntimePermission/updatePermissionFlags` to align AOSP (`g4/b.java:320-417`).
- **53 Activity** covers: per-application permission editing (`APP_PERM_EDITOR`), per-permission application list (`PERMISSION_APP_EDITOR_ACTIVITY`), self-starting (`OP_AUTO_START`), privacy risk detection (`PRIVACY_RISK_DETECTION_ACTIVITY`, 8 Class detection items), privacy space (`MANAGE_PRIVACY_APPS`), application lock (`CHECK_ACCESS_CONTROL`/`LOCK_APP`), thumbnail blur (`PRIVACY_THUMBNAIL_BLUR_SETTING`), private input mode (`PRIVACY_INPUT_MODE_ACTIVITY`), shutdown password (`SHUT_DOWN_PASSWORD_ACTIVITY`), cross-terminal permissions (`acrossterminal` → `/data/misc/user/<id>/securitycenter/across_terminal.xml`), OAID authorization (`REQUEST_PERMISSIONS_FOR_OAID`), combined permissions (`REQUEST_PERMISSIONS_COMBINE`), device permissions (`REQUEST_DEVICE_PERMISSIONS`), stealth mode (`INVISIBLE_SETTING`), privacy center (`PRIVACY_SETTINGS`), behavior records (`APP_BEHAVIRO_RECORD`).
- **application lock (observation)**: Password/pattern **is not in this App. Place**, `SecurityManager.setAccessControlPassword/checkAccessControlPassword/addAccessControlPass` are all in system_server; `ConfirmAccessControl` is carried through Intent to link `android.app.extra.PROTECTED_APP_TOKEN` (IBinder) with the system; forget the password and leave `IXiaomiAccountService`.
- **privacy space (observation)**: Android multi-user profile + `SecurityManager` privacy application collection; `PrivacyAppsProvider` (authority `com.miui.securitycenter.app.hide`) actually only implements `call("isPrivacyApp")`; when added, remove Recents, kill process, `setNotificationsEnabledForPackage(false)`, and broadcast `com.miui.gamebooster.PPRIVACYAPP`.
- **thumbnail blur (observation)**: switch writes `Settings.System.miui_recents_privacy_thumbnail_blur` (**comma separated package name string**), read and rendered by **`com.miui.home`**; `com.miui.blur` is irrelevant background blur SDK.
- **Privacy CTA SDK (observation)**: `ue` package, POST `data.sec.miui.com/privacy/{agree,revoke,get,latestVersion}/v1`, `sign=MD5(Base64(key+sortedParams+key))`, key `2dcd9s0c-…`, international version short circuit `-4`, 24h throttling, policy implementation `files/privacypolicy/<name>/`.
- **uploaded surface (observation, reviewed and corrected)**: `flash.sec.miui.com/user/{cat,ra}` uploaded **`PackageManager.getInstalledPackages(0)`. Filter out all non-system applications** after `FLAG_SYSTEM` (package name + version number + application name + uid, `xc/h.java:13`), **no launcher/desktop visibility filter** - that is, "what is installed by the user" rather than "what is on the desktop"; the trigger point is that **automatically starts the detection task** after opening/entering the privacy risk detection page, and checks the total network switch before sending; there is no secondary confirmation of the application list uploaded one after another on the direct link (the first agreement and notification from the upper layer have not been verified by a real machine). Encryption: AES/ECB/PKCS5 + GZIP + Base64, key=`SHA1(ConfigManager.getSecurityHttpKey())`, `biz_id="rivacy_risk_check"` (the original spelling is missing r).
- **application management (observation)**: `appmanager` 22 categories (batch uninstall, clear cache/data, notification switch, usage statistics), `applicationmanagement/AppManagementService`, `sec/external/InstallProvider` (installation channel).

### 4.7 Power/Performance/Cleaning (powercenter/superpower/optimizecenter/optimizemanage/firstaidkit)
- **does not adjust the governor (observation, correct common misjudgments)**: four mode bits `POWER_SAVE_MODE_OPEN`/`POWER_PERFORMANCE_MODE_OPEN`/`POWER_BALANCED_MODE_OPEN`/`power_supersave_mode_open` Write `userConfigureStatus∈{enhance,performance,ultimate}` to **`content://com.miui.powerkeeper.configure/GlobalFeatureTable`** (`ce/g.java:340-363`) is implemented by the powerkeeper process.
- **The only authoritative write-in port (observation)**: `PowerSaverProvider.call()` 8 Methods (`changePowerMode`/`changePerformanceMode`/`changeBalancedMode`/`changeSuperPowerMode`/`changeExtremeEnduranceMode`/`cleanMemory`/`showLowBatteryDialog`/`showSuperPowerHome`, `PowerSaverProvider.java:387-432`), the three modes are mutually exclusive.
- **power saving 7 actions (observation)**: `be.g` frame limit 60Hz (`DisplayFeatureManager.setScreenEffect(24,fps)` reflection + `user_refresh_rate`), `be.c` off fingerprint AOD, `be.h` off 5 system sounds, `be.i` Turn off wrist raising/double-click/notification bright screen, `be.b` brightness -5%, `be.a` automatic brightness limit 30%, `be.e` turn off vibration (linear motor); there are also synchronous shutdown, memory cleaning cycle 600s, positioning degradation, `setAntHalf(0)`.
- **true sysfs write only 4 temperature/charge nodes (observation)**: `/sys/class/thermal/thermal_message/sconfig`, `/sys/class/qcom-battery/lpd_charging`, `charger_temp`, `board_sensor_temp`; `cpufreq/cpuinfo_max_freq` read only; `charge_full/charge_full_design/fg1_rm/slave_authentic` read only.
- **super power saving (observation)**: `superpower` is **minimalist desktop UI** (self-drawn status bar/clock/WiFi/battery/signal/shortcut button), driven by `SUPER_POWER{progress=0..3}` broadcast.
- **cleanup (observation, revised as per 2026-11-06 review)**: The real engine is external `com.miui.cleanmaster` (AIDL `IGarbageCheck.F3(scanCb)/J(list,cleanupCb)`, `CacheCheckManager.java:103-115`, action `com.miui.cleanmaster.action.CHECK_GARBAGE_CHECK`); this APK is inside `parse.db` Provide "directory → package name" mapping (line 1043), `optimizecenter`(40) + `optimizemanage`(29) to clean up UI/results; `FboJobService` triggers `FboPackageBean.upLoadPackageList()` every 6 hours, but this method **only writes the package list into `Settings.Secure` (`FBO_UPLOAD_LIST`/`FBO_UPLOAD_TIME`, `ta/f.java:36-52`) is handed over to the local FBO system service. Although the method name is called upLoad, it is not connected to the**.
- **first aid kit (observation)**: `firstaidkit`(28), `assets/firstaid_scanitem.json` 5 sets (Performance 3 / Network 3 / Operation 5 / Power consumption 2 / Others 3).
- **threshold concentration (observation)**: `gd.c` (~400 accessors) → `content://com.miui.securitycenter.remoteprovider` of `callPreference` (across users `110@` prefix).

### 4.8 Game acceleration (gamebooster / gameturbo)
- **29 Activity**: `GameBoosterRealMainActivity`, `GameBoosterSettingFragment`, `PerformanceSettingsFragment`, `GameModeSettingsActivity`, `BeautySettingsActivity`, `PrivacyCameraGlobalSettingsActivity`, `SelectGameActivity`, `MarketDownloadV2Activity`, etc.
- **FM channel = Joyose (observation)**: `ServiceManager.getService("xiaomi.joyose")` → `IGPUTunerInterface.checkSupportGpuTuner/getProfiles/deleteProfile`, `IGunSightInterface` (image quality enhancement), `IJoyoseInterface`, `JoyoseManager.getGameMotorAppList()` (4D vibration), `joyose_game_turbo_api_version` handshake; temperature control `com.miui.powerkeeper.feedbackcontrol.IFeedbackControl`; acceleration parameters `adv.sec.miui.com/game/speedParams`.
- **data (observation)**: `gamebooster.db` (`gamebooster_table` 20+ items per game: `settings_gs/ts/edge/hdr/4d/follow/hot_area/finger/shake/sensitivity/op_stability/touch_mode`, `game_ratio`, `game_gravity`; `quickreply_table`; `game_tags_t`); for game identification `top_200_games.json` (**607 package name**, `adv.sec.miui.com/game/pkg` uplink local package name can be updated in the cloud).
- **voice changing/beautifying (observation)**: `libjni_sound_effect_2_0.so` = WORLD vocoder (`Dio/CheapTrick/D4C/Synthesis2`), host `gamebooster.encoder.SoundSupport`; `BeautyService` itself only does foreground scene determination (three-way monitoring: `ProcessManager.registerActivityChangeListener` reflection + `TaskStackListener` Reflection + `getRunningTasks`) → bind `DockWindowManagerService` → `IGameBoosterWindow.D4(5,…)` to enter session mode, image processing is in `:ui` and `j8/u.java`.
- **support list (observation)**: `beauty_light_default_support_list`(31), `gesture_effect_support_list`(28), `pickup_support_activity_list(_phone)` (18/19), `beauty_face_support_activity_list`(4), `vtb_default_support_list`(324), `quickreply_default_list`(263), `xunyou_support`(20).

### 4.9 Automatic tasks (autotask ×2) and application prediction (apppredict/device-side AI)
- Two sets of **coexist (observation)**: the new version `com.miui.autotask` (125 categories, Kotlin, authority `com.miui.autotask.provider.auto_task(_service)`, `auto_task.db`) and the old version `com.miui.powercenter.autotask` (22 categories, authority `com.miui.powercenter.autotask`, `power_auto_task.db`). The rule engine is `{instanceId,typeId,jsonStr}` plug-in, **22 type conditions × 28 types of actions** (time/application/location fence/WiFi/Bluetooth/headset/hotspot/conference/mute/NFC/lock screen/charging/battery/call → on WiFi/Bluetooth/NFC/Hotspot/Flight/Do Not Disturb/Mute/Volume/Brightness/Dark Color/Eye Protection/Font/Display/Rotate/Power Saving/Touch/Flash/Flashlight/Driving/Launch Application...).
- **end-side AI (observation)**: `assets/rec_model_20230425.mnn` (MNN FlatBuffers: `sparse_inputs_1..6`+`sparse_inputs_time` → 9×embedding → MaxPool → relu → 5×linear → `linear_9.tmp_1` two categories) + `assets/character.json` (40 Character vocabulary) → `ia/j.java` uses `MNNNetNative.nativeCreateNetFromFile`/`nativeCreateSession(FORWARD_CPU,4)` for inference; input = the first 30 characters of **of the first 5 front-end application package names char-id** + 6th candidate + `HOUR_OF_DAY`; output softmax takes index1 = P(next).
- **end-side training (observation)**: `MNNTrainNative.nativeTrainRecModel(...)`; sample <50 abandon, 20% random test set, positive and negative samples 1:1 negative sampling, comparison before and after training loss, if **deteriorates, delete checkpoint**; threshold `train_on==1 && power ≥20% && Since last time ≥ period`; super parameter `epochs=10, lr=1e-4, batch=128`; task plan `POST api.sec.miui.com/mnntrain/taskdata`.
- **fusion sorting (observation)**: MNN + Bayes(`ka.b`/`ga.b`) + PBB(`la.a`) + Standby (`ia.b`/`ia.f`), `score += 1/(rank+1)` (top-50); external `AiService` (exported, `miui.permission.APP_PREDICT_VERIFY`), `AppPredictService` (`IPredictNextApp.R2(n)`), `AppPredictWidget2x4` (exported Widget).

### 4.10 MAML dynamic card engine (maml)
- **ability (observation)**: self-developed DSL (`Expression/RootExpression` + `String/Json/Format/DateFunctions`), **24 types `ActionCommand`** (incl. `IfCommand`/`LoopCommand`/`MethodCommand` (reflection call)/`FieldCommand`/`IntentCommand`/`ExternCommand`/`VariableCommand`/`BinderCommand`); `WebViewScreenElement` on `setJavaScriptEnabled(true)` + `addJavascriptInterface(new MamlInterface(),"maml")`; can be tied ContentProvider/broadcast/sensor/file.
- **risk boundary (observation + inference)**: `ScreenElementRoot` defaults to `capability = -1 = CAPABILITY_ALL` (network + object creation + persistence fully enabled), only explicit `setCapability()` is narrowed; **but there is no HTTP resource in this APK loader** - The network delivery is only thrown to the host through WebView and `issueExternCommand`, and the remote zip is downloaded by the host App (SystemUI/Desktop/Theme Store) and handed over to `ZipResourceLoader`. Cloud package signature: `CloudCoder.generateSignature()` = Base64(SHA1(METHOD&path&kv sorting&security)), AES/CBC/PKCS5 + **hard-coded IV `0102030405060708`** (`util/net/CloudCoder.java:26-77`).

### 4.11 Small modules (easily ignored but exist)
| module | Number of classes | Conclusion (observation) |
|---|---|---|
| `zman` (safe sharing) | 8 | `ShareActivity`/`ShareSettingActivity`/`PrivacyProtectSettingsActivity`/`ZmanProvider`/`ZmanFileProvider`: Desensitize before sharing (hide location/device information, etc.), protected by `com.miui.zman.permission.SECURITY_SHARE` |
| `wakepath` (wake-up path) | 2 | `ConfirmStartActivity`: "Whether jump is allowed" confirmation page during chain start (A pulls up B); matched with physical examination item `CHAIN_START`, permmgr method 8 (`WRITE_WAKE_PATH_WHITE_LIST`) |
| `tvm` (Trusted UI) | 13 | `TvmManagerService` + `ITvmManager/ITvmManagerImpl/ITvmManagerCallback/IMiTrustUICallback` + `TvmNonceModel/TvmUrlModel/SessionModel/TvmStatusModel`: TEE trusted interface (payment/password scenario), `assets/tvm_whitelist.json` defines 4 applications and their TUI sub-application hashes; **is dynamically enabled by system properties** (`vendor.xiaomi.trustedvm.version` / `ro.vendor.mitee_tui.support` (any non-empty `setComponentEnabledSetting(ENABLED)`, `Application.java:660-668`), the status reads `vendor.qvirtmgr.trustedvm.status` (`RUNNING`), the image is ≤140509184 B, go to vendor AIDL `IMTService`/`IMiTrustedUIService`; the list defaults to `enabled=false`, and `miui.permission.TVM` Missing write protectionLevel (=normal) |
| `securitykey` | 4 | `ConfigManager` (`getEncodeKey/getDecodeKey/getHttpKey/getUuidAesKey/getRubbishKey/getNetworkAssistantKey`) + `EncryptionUtils`: key distribution point for the entire App, the actual key is provided by `libsec-common-utils.so` |
| `sdk.tc` (operator SDK) | 9 | `TcManager` (`SEC_KEY` hard-coded)/`TcPlugin`/`TcDirection`/`DataUsage`/`SmsSender`/`Base64AesUtil`: package calibration command library client |
| `hybrid.accessory` | 49 | Accessories/quick application cards (`api.hybrid.xiaomi.com`), `icondialog`, etc. |
| `applicationmanagement` | 2 | `AppManagementService` + `ApplicationManagementActivity` (apply batch management, `first_v_forbidden_chain_merge_status` SP) |
| `appcompatibility` | 1 | `AppExcepitonTipsActivity` (application compatibility exception prompt) |
| `googlebase` | 1 | `GmsCoreSettings` (GMS switch entrance) |
| `msa` | 1 | `MsaUtils`（MSA/OAID） |
| `international` | 4 | `AntiGlobalAdBean`/`OptimizeGlobalAdBean`/`GlobalAdBaseBean` (international version advertising switch model) |
| `nativead` | — | Advertising Activity (1 `nativead` package Activity in `AndroidManifest`) |
| `dock` | 20 | Dock floating bar `DockMonitorService`/`DockSettingsActivity`/`DockAppEditActivity` + `DockWindowManagerService` (game/video floating window host) |
| `net` | 2 | `MiuiNetworkPolicyManager`/`MiuiNetworkSessionStats` (system service thin package) |
| `sec` | 1 | `InstallProvider` (authority `com.miui.sec.INSTALL_PACKAGE`, installation channel) |
| `push` | 2 | `XiaomiPushReceiver` + `a` (push reception) |
| `analytics` | 10 | `AnalyticsUtil`/`StatManager`/`RemoteAnalyticsManager`/`RemoteAnalyticsReceiver` |

## 5. Key implementation logic (link by link)

### 5.1 One-click physical examination: reflective assembly + 5 concurrent assembly lines + deduction system scoring
1. **assembly (observation)**: `ModelFactory.java:159-172` reads `assets/scanitem.json`, for each item `Class.forName(name).getConstructor(String.class, Integer.class).newInstance(key, score)` → **reflection instantiation check item**; `version` array must contain the current channel (`x4.a0.h()`) otherwise skipped. Multiple candidates in the same group will be displayed based on "the first non-SAFE" (`GroupModel#scan`).
2. **arrangement (observation)**: `scanner/k.java:940-956` (TAG is directly called `SecurityManager`) starts 5 assembly lines at one time:
   - `A()` → `scanner/c` manual item scanning;
   - `x()` → `scanner/m` automatic item/system application scan;
   - `C()` → `scanner/o` (**VirusScanManager**): If `key_is_guard_support` is true, go to GuardProvider **to incrementally scan**, otherwise go to `pre_scan` + `HOMEPAGE_SCAN`;
   - `B()` → `scanner/d` memory scan/optimization;
   - `y()` → `CacheCheckManager` cache scan (call external cleaning engine).
   - Anti-reentrancy: `k#m()` sets flag + `interrupt()` two threads + `o.q()` cancels incremental scan.
3. **score (observation, `ScoreManager.java:167-220`) - deduction system**:
   ```
   deduct = Σ(score of system item DANGER) + n(memory ratio) + f(last scan since) + Σ(score of manual item DANGER)
   deduct = clamp(deduct, 0, 100)
   if (any risk exists && deduct < 41) deduct = 41 // ★ There is a risk of deducting at least 41
   Final score = 100 - deduct
   ```
   - `n(mem)`: Memory usage ≥50%→15, ≥40%→10, ≥30%→8, ≥20%→5 (no repeated counting within 300 s);
   - `f()`: >3 days→15, >Default frequency→10, >24 h→8, >12 h→5, >6 h→3;
   - Deduct points and write back the preference (`x.c0(ctx, deduct)`), and feed **to the advertising card**: `NativeInterstitialAdLayout.a(score)`. Switch the blue/yellow background according to the score threshold (`MainFragment.java:1695`).
4. **demonstration (observation)**: 5 kinds of shells (`MainContentFrame`/`…Fold`/`…Lite`/`…Pad`/`ScanContentFramePad`) achieve the same `wf.a` interface rendering score.

### 5.2 Virus scanning: This APK is a "client + daemon", and the engine is in the external GuardProvider
- **is not in this APK (observation)**: `assets` and `lib` **does not have any virus signature library or engine library**; `p9/a.java:136-140` is bound to `com.miui.guardprovider.action.antivirusservice` (package `com.miui.guardprovider`), the interface is based on AIDL pile `com.miui.guardprovider.aidl.IAntiVirusServer` in APK (`v0/y6` batch scan files, `q1/D` full/cancel, `r7/X3` single package query, `n0/P6` engine/library version, `C2/K/c5/O3` **Wi-Fi security detection**).
- **scanning object (observation)**: `scanner/o.java:710-769` Traverse the installed **non-system** application and get `sourceDir` + pass `MediaStore.Files` to check all `*.apk` in external storage; the result is four states `SAFE/RISK/VIRUS/RISK_APP` (`o2/b.java:64-69`).
- **guard (observation)**: `GuardService` reflection `miui.process.ProcessManager.registerActivityChangeListener` monitors the front desk, hits the `safepay_target_*` list and enters the "payment security state" (status bar `action_status_safepay` + `SHOW_WARNING_DIALOG`); and do **input method anti-hijacking** (when the default input method is a third party, use `Settings.Secure.putString(default_input_method, system IME)` to switch back and save the original value for restoration, `GuardService.java:700-735`), `AppOpsManager.setUserRestriction(24,…)` limit background, Wi-Fi risk detection (write `key_safepay_wifi_scan_result`), intermodal game judgment (`POST flash.sec.miui.com/game/union`, field `pkg`).
- **virus library (observation)**: two channels - GuardProvider engine library + `VirusAutoUpdateJobService` (Job 99, daily).

### 5.3 Harassment interception: System reverse check Provider → 21 policy chains → Bit mask characteristics → TFLite engine
1. The direction of **is "System Ask App" (observation)**: When a call/text message comes, the system Telephony asks the policy through `ContentResolver.query(content://antispam/{sms_judge,call_judge,url_judge,service_num_judge,call_transfer_intercept_judge})`; `AntiSpamProvider.java:106-124` → `b2.b#b/e/f` → Traverse `h2.e` enumeration **21 Policy** (`h2/e.java:113-159`), when hit, break returns `blockType`, and writes interception log when `≥3`.
2. **21 policies (observation, by priority)**: empty number → strong cloud white → strong cloud black → white list → black list → fuzzy white → black prefix → black number segment/attribution → service number SMS (106) → keyword white → cloud white → all numbers → stranger → overseas → keyword black → cloud black → cloud white keyword → cloud black keyword → **Intelligent SMS filtering (connected to TFLite)** → Call transfer → Reported number; finally add another layer `URLFilterPolicy`.
3. **`assets/pattern` Rule (observation)**: `lj/c.java:42-57` Read (priority `files/pattern`, hot update possible) → `nj/d#d(String)` Use `JSONTokener` Relaxed parsing `{version, features[]}`; single feature support `address/body` of `pattern`(AND)/`neg_pattern`(excluded)/`or_pattern`(OR)/`body_len`/`combi`(combination); `nj/d#e()` put the hit item `sms_type` **is bitwise ORed to** into `featureBits`, and then `nj/d#b/a` is mapped into a weight, which is used to translate TFLite logits (`lj/e.java:49-64`).
4. **New generation SMS engine (observation)**: `h2/a` (`SmsEngineHandler`) Decompression `files/resource.zip` → `files/SmsEngineResource/` (with canonicalPath path crossing protection + entry-by-entry size verification) → `da.a.b(dir)` Modeling: `v.txt` (128 dimensions embedding), `1.bin`(UnlawRecognizer, threshold 0.4), `2.bin`(ImportanRecognizer, threshold 0.6); `h(sender,body)` returns 0=unblock/important, 2=interception. Update interface `POST api.sec.miui.com/GuardProviderV1/GetResourceZipFile` (`{app_version, ver:"2.1.2"}`), download package **MD5 must be equal to `6206c9f17a64246498b52e117d0cff1e`**, `call(content://antispam, "initSmsEngine")` after downloading.
5. **number library (observation)**: Really stored in the system `ExtraTelephony.Phonelist/Keyword` Provider; application side `assets/service_num.json`(224), `white_url.json`(452), can be covered by `MiuiSettings` cloud; cloud number library `POST api.sec.miui.com/harassIntercept/intercepts` (`{dataVersion}` → `version` + `v[{status:new|updated|deleted,type:1|2,uuid,p,attributes}]`, type1=number/type2=keyword, `attributes` two digits=category+level).
6. **URL Interception (observation)**: `policy/b.java:172-218` Four steps - local risk domain name table → GuardProvider AVL url scan → Cloud detection (original/post-jump URL) → Whitelist (`white_url.json` + cloud) determination, return 0 release / 1 interception / 2 danger warning / -1 unknown.

### 5.4 Firewall: Reflection MIUI private SecurityManager, rules fall into netd
See §4.4 (`MiuiNetdFirewall.java:177-202`). Key points: Do not write iptables, the rules are delivered to the framework through `miui.security.SecurityManager.setMiuiFirewallRule(pkg,uid,rule,type)`; `TYPE=MOBILE0/WIFI1/ROAMING2/ALL3/OTHER4`, `RULE=Init-1/Allow0/Restrict1/Alert2`; intercept the callback call `miui.intent.action.FIREWALL` broadcast; roaming defaults to `Restrict`; the rules fall into the encrypted file (`FirewallService.java:635`).

### 5.5 Traffic: NetworkStatsManager aggregation + system bucket granularity rewriting + operator SMS calibration
See §4.4. Two independent links: ① Statistics (`NetworkStatsManager.querySummary/queryDetailsForUid`, front/backend is split according to `Bucket.state==2`, `Settings.Global.netstats_uid_bucket_duration→3600000`); ② Calibration (`TcManager` → `libtcp` built-in command library → `startInterceptSmsBySender` intercepts operator receipt → parses balance/traffic SMS).

### 5.6 Permission management: three-body architecture + dual-write + MIUI private AppOps
See §4.6. Key points:
- **has no `IPermissionManager`** (zero hits in the whole tree); true boundary = `content://[<uid>@]com.lbe.security.miui.permmgr` (method code 3/4/5/6/7/8/9/12/15/18) + `miui.security.SecurityManager` + `AppOpsManager`.
- **double-writes**: `setModeNow`(9)/`setApplicationPermission`(6) → then reflects `IPackageManager.grantRuntimePermission/revokeRuntimePermission/updatePermissionFlags` (`g4/b.java:320-417`).
- **MIUI private op (`android/app/AppOpsManagerCompat.java`)**: `10000–10053`, incl. `AUTO_START=10008`, `BOOT_COMPLETED=10007`, `BACKGROUND_START_ACTIVITY=10021`, `GET_INSTALLED_APPS=10022`, `GET_OAID=10037`, `DEVICE_ID=10050`, `CAN_BE_RECORDED=10041`, `MEDIA_PROJECTION=10042`, `MODE_ASK=5`.
- **setting does not fall into private SP**: go to `content://com.miui.securitycenter.remoteprovider`'s `callPreference` (`r4/a.java:19-158`, entity `SharedPreferences("remote_provider_preferences")`, prefix `110@`).

### 5.7 Application lock/privacy space/privacy blur (the ability lies in the system, this APK only does UI and arrangement)
- Application lock: `SecurityManager.setAccessControlPassword/checkAccessControlPassword/addAccessControlPass`; `ConfirmAccessControl` is linked with `PROTECTED_APP_TOKEN`(IBinder) + `needFinishAccessControl(token)` in the Intent; if you forget the password, use `IXiaomiAccountService`. **This App does not save the password**.
- Privacy space: multi-user profile + `SecurityManager` privacy application collection; `PrivacyAppsProvider` only provides `call("isPrivacyApp")`; when joining, remove Recents, kill processes, turn off notifications, and broadcast `PPRIVACYAPP`.
- Thumbnail blur: write `Settings.System.miui_recents_privacy_thumbnail_blur` (comma separated package name), rendered by `com.miui.home`; `persist.sys.protect_image` switches to occlusion mode.
- Stealth mode: `InvisibleModeService` reflection `AppOpsManager.setUserRestrictionForUser(op∈{0,1,41,42,27,26}, …, {com.android.camera, com.miui.face, com.miui.voicetrigger}, userId∈{0,999})` + `StatusBarManager.setIconVisibility("stealth")` (indicator light invisible).

### 5.8 Device-side AI: MNN two-class classification + device-side training + four-model fusion
See §4.9. Key values: input `[batch,30]` int (package name char-id) + `[batch,1]` hours; `nativeCreateSession(FORWARD_CPU, 4)`; output `linear_9.tmp_1` softmax; training `epochs=10, lr=1e-4, batch=128`, `<50` samples are abandoned, checkpoint is deleted when loss deteriorates; fusion `score += 1/(rank+1)` (top-50).

### 5.9 Power saving/performance: only write "mode status" and do not touch the governor
See §4.7. Four mode bits are written to `content://com.miui.powerkeeper.configure/GlobalFeatureTable`; `PowerSaverProvider.call()` 8 methods are the only authoritative entrance; power saving 7 actions (limit frame/off AOD/off sound effects/off bright screen/brightness -5%/automatic brightness upper limit 30%/off vibration); true sysfs only has 4 temperature/charging nodes.

### 5.10 Cleaning: This APK is "rules + UI", executed in cleanmaster
See §4.7. `parse.db` (line 1043) provides "Directory → Package Name/Name", `CacheCheckManager.java:103-115` calls the external cleanmaster through AIDL `IGarbageCheck.F3/J`; `FboJobService` only reports the package list in 6 hours.

### 5.11 Game acceleration: Joyose is the FM channel
See §4.8. `ServiceManager.getService("xiaomi.joyose")` → `IGPUTunerInterface`/`IGunSightInterface`/`JoyoseManager`; temperature control `IFeedbackControl`; parameters `adv.sec.miui.com/game/speedParams`; each game configuration falls into `gamebooster.db`.

### 5.12 MAML: Script Engine + WebView + Full Capability Default
See §4.10. 24 types of ActionCommand (including reflection `MethodCommand`/`IntentCommand`/`ExternCommand`), `MamlInterface` JS bridge, default `CAPABILITY_ALL`; this APK does not have HTTP loader, the remote zip is downloaded by the host; `CloudCoder` uses AES/CBC/PKCS5 + hardcoded IV `0102030405060708` packet.

### 5.13 Early warning data link: 12379 + RSA signature verification + bit mask level + volunteer sensor
See §4.5. Disaster: `api.sec.miui.com/12379/warning/getRegionInfo` (SALT hard-coded) → `warning_center_disaster.db`; early warning data is verified with `DISASTER_PUBKEY` (RSA-2048); level bit mask `Blue 1/Yellow 2/Orange 4/Red 8` (`LEVEL={8,4,2,1}`, `getSystemPushLevel` default 12=red+orange); 71 icons covering 25 `EventType` enumerations (11A flood/waterlogging/flash flood, 11B meteorological 12 categories, 11C02 earthquake, 11D landslide/mudslide, 11E storm surge/wave, 11G01 forest fire/11G05 grassland fire; `fire` icon consists of two 11G code shared). Earthquake: `srv.sec.miui.com/earthquake/warning/{records,signature,supportCityCode}` + `/earthquake/volunteer/sensor/upload` (accelerometer).

### 5.14 Embedding and advertising: the same set of signatures and the same set of cards
- Buried point: `com.miui.analytics`(StatManager/RemoteAnalyticsManager) + OneTrack (local `events/events_cloud/monitor` queue) → `data.sec.miui.com/data/upload`.
- Advertisement: `adv.sec.miui.com/info/layout` issues card → `CardModelMaker` → `Adv*CardModel` renders; the screen is opened by `SystemAdContentProvider` (authority `com.miui.securitycenter.splashAd`); `assets/placement_id` = Facebook advertising space; MSA OAID is used as the mark; `sdkconfig.ad.*` Configuration; `data.sec.miui.com/adv/*` return.

---

## 6. Built-in database and data assets

### 6.1 SQLite built-in with the package (2 pieces)
| File | SHA-256 | table/number of rows | Purpose | reader | plain text |
|---|---|---|---|---|---|
| `res/raw/parse.db` (ZIP internal name `res/rKy.db`) | `5b1da601c11b20ea…` | `DIR_PARSE(PATH, PACKAGE_NAME, APP_NAME)` **1043 lines / 884 packages**, `TEMP_DIR(EXCLUDED, PATH, TYPE)` 10 lines, `android_metadata` | Application-specific cache directory → package name/Chinese and English name mapping; determine "who a certain directory belongs to" during deep cleaning | `ra/e.java:39-60` (`openRawResource(R.raw.parse)` → `getDatabasePath("parse.db")`), query `g(pkg)`/`f(list)`, **server JSON can overwrite** `c(list,json)`(148) | ✅ Clear text |
| `assets/garbage_clean.db` | `e1d838531cc6f604…` | `t_cache`/`t_ad`/`t_residual` (same as 8 columns `pkg_name,dir_path,root_dir,res_zh_tw,res_zh_cn,res_default,advise_del`); `t_cache` **4 row**, and the other two tables **0 row** | Trash removal rules (skeleton + 4 examples) | There is no reader **in the** book APK (there is no `garbage_clean` string in 3 dex and 18 so; there is no `t_cache/t_residual` either) | ❌ 4 rows of data are all hex confusion: equal-length prefix repetition + arbitrary length → **stream cipher/CTR-like key stream multiplexing** features, non-block cipher |

`parse.db` real sample (`Observed`):
```
/.budingdoyen      iw.avatar            zh_cn=布丁爱生活&zh_tw=&=
/.helix.thumbnails com.real.RealPlayer  zh_cn=Real Playar&zh_tw=&=
/gomarket          cn.goapk.market      zh_cn=安智市场&zh_tw=&=
/.MTXX             com.mt.mtxx.mtxx     zh_cn=美图秀秀&zh_tw=&=
```
`TEMP_DIR` 10 Rows: `/downloaded_rom`, `/MIUI/debug_log`, `/ramdump`, `/debug_log`, `/diag_logs`, `/data/tombstones`, `/data/log`, `/LOST.DIR`(EXCLUDED=1), `/data/system/dropbox`(EXCLUDED=1), `/cache`.

### 6.2 Databases created during runtime (20, total 32 in `CREATE TABLE`)
| database | Table (key points) | module |
|---|---|---|
| `traffic.db` | `traffic(_id, …)` | Network assistant traffic statistics (`bj/a.java:24`, `networkassistant/provider/DBHelper.java:9`) |
| `_fire_wall_config2.db` / `fire_wall_wifi_config.db` / `fire_wall_roaming_config.db` | — (**is not SQLite**: private encrypted file, magic number ASCII `walimini`, whole loop with `ConfigManager.getNetworkAssistantEncryptionKey()` XOR; key in `libsec-common-utils`) | Firewall: three sets of rules for mobile/roaming/WLAN, one set for each dual SIM card (`config/ConfigFile.java:20,59,160-171`) |
| `_datausage_ignore_applist_config.db` | — | Traffic statistics ignore list |
| `warning_center_disaster.db` | `area`, `disaster` (CAP field: `identifier/eventType/severity/headline/instruction/province/city/county/location`…), `subscribe(subscribeLevel)` | Disaster early warning (connected to national early warning 12379) |
| `earthquake_warning2.db` | `area(province,code,city,support,counties)`、`earthquake(eventID,magnitude,longitude,latitude,epicenter,intensity,depth,warntime,signature…)`、`subscribe` | earthquake early warning |
| `battery_history.db` | `history`、`histogram`、`shutdown` | Power history/shutdown record |
| `power_auto_task.db` | `autotasks(_id,name,enabled,condition,operation,repeat_type,restore_operation,restore_level)` | Automatic battery task |
| `auto_task.db` | `auto_task_table`、`task_condition_and_result_table`、`condition_associated_third_apps` | **system-level automatic task** (condition → action, including third-party App linkage) |
| `gamebooster.db` | `gamebooster_table` (20+ tunings per game: `settings_gs/ts/edge/hdr/4d/follow/hot_area/finger/shake/sensitivity/op_stability/touch_mode`, `game_ratio`, `game_gravity`), `quickreply_table`, `game_tags_t` | game acceleration |
| `app_predict_data.db` | `app_predict_table(pkg_name, click_time, wifi_state)` | Application forecasting |
| `month_report.db` | `report_json(eventId,eventTime,pkgName,eventType,version,data,moduleName)` | monthly report |
| `pic_db.db` | `notification_table`、`pic_table` | Notification image cache (bubble) |
| `cloud_no_kill_pkg.db` | — | No cleaning/no killing whitelist |
| `global_satisfaction_data.db` | `questions_table(id,data,is_valid)` | satisfaction questionnaire |
| `MessageInfo.db` | — | SMS/message records |
| `parse.db` (runtime copy) | Same as 6.1 | Directory parsing |
| `exoplayer_internal.db` | ExoPlayer cache/download index | Video playback (advertising/tutorials) |
| OneTrack：`events` / `events_cloud` / `monitor` | Buried point queue | statistics |

### 6.3 Plain text rules/vocabulary assets (the "built-in library" that really does the work)
| assets | scale | Type/Format | reader | Purpose |
|---|---|---|---|---|
| `assets/dict` | 3.64 MB, **348,982 words + 35,224 word lists**, `totalFreq=60,101,983`, `minFreq=2` | Custom binary (**jieba word segmentation dictionary**, HMM `startProb/transProb` is consistent with the open source jieba bit by bit) | `lj/g.java:32` → `mj/a.h(InputStream)`（`mj/a.java:260-330`） | Chinese word segmentation (SMS/text semantic determination) |
| `assets/pinyinindex.idf` | 54 KB, **20,902 Chinese characters → Pinyin**, 1,336 duplicate strings removed | Custom `IDF ` v2 table (`cl/a.java`) | `am/a.java:57` → `cl/a.java` | Initial letter/polyphone search (application name, contact person) |
| `assets/pattern` | 5.5 KB, v1.2, 6+ sets of regular rules | UTF-8 **loose JSON** (key without quotes) | `lj/c.java:22` | SMS importance/phishing identification (last number, card number, verification code, redemption code, "invoice/remittance/phishing/subscription", etc.) |
| `assets/white_url.json` | **452** domain name/IP | JSON | `m2/e.java:24` | URL whitelist (no interception) |
| `assets/service_num.json` | **224** number | JSON | `m2/e.java:55` | Customer service/service number database |
| `assets/address.json` | 34 provinces / 336 cities + area code | JSON | `m2/f.java:416,455` | Number ownership |
| `assets/safepay_target_package` | **211** `{package,state}` | JSON | Unknown | Payment Security Scan Targets |
| `assets/safepay_target_activity` / `_calling_activity` / `_imm_whitelist` | 7 / 4 / 13 | JSON | Unknown | Payment page/input method whitelist (anti-screenshot/anti-keylogging) |
| `assets/anti_fraud_pay_activity` | 3 groups | JSON | Unknown | Anti-fraud: Transfer page monitoring target |
| `assets/top_200_games.json` | **607** package name | JSON | Unknown | Game recognition list |
| `assets/financial_package_top` / `photography_package_top` | 300 / 300 | JSON | Unknown | Financial/imaging application classification list |
| `assets/privacyapps_top.json` | 4 sets (100/20/85/29) | JSON | Unknown | Recommended apps for privacy space |
| `assets/behavior_record_white.csv` / `global_…` | 57 / 43 lines | Hex digest whitelist (16/32/48/64 B) | `mc/d.java:128` | Behavior record whitelist (released based on signature summary, domestic and foreign editions) |
| `assets/tvm_whitelist.json` | 4 items `{package_name, signature, tui_apps[]}` | JSON | `fh/c.java:112` | **TEE/TUI Trusted UI whitelist** |
| `assets/scanitem.json` | v8, 28+8 group | JSON | Physical examination engine | Definition of one-click physical examination items (see §5.1) |
| `assets/firstaid_scanitem.json` | 5 groups (Performance/Network/Operation/Power Consumption/Others) | JSON | first aid kit | One-click first aid |
| `assets/xunyou_support` | 20 game titles | text line | Unknown | Xunyou acceleration support list |
| `assets/vtb_default_support_list` | 324 package name | text line | Unknown | Video Toolbox Whitelist |
| `assets/quickreply_default_list` | 263 package name | JSON | `f7/b.java:299` | In-game quick reply support list |
| `assets/warning_center_disaster_icon` / `_guide_text` | 21 / 11 items | JSON (with trailing comma) | Unknown (no local reference) | Disaster Icon Mapping + Full Four-Color Defense Guide |
| `assets/warningcenter/` | **71** Zhang png/webp | pictures | data driven | Disaster warning icon: There are 21 codes of `warning_center_disaster_icon` with a total of 75 mappings of **, all of which hit the real file** (4 of `fire` are shared by `11G01` forest fire and `11G05` grassland fire → 71 = 75 − 4 Repeat); `11B07` (Sandstorm)/`11B14`/`11B16`/`11B22` has an icon but is not in the `EventType` enumeration (new types in the cloud are prepared in advance) |
| `assets/character.json` | 40 characters→id | JSON | `ea/c.java:21` | MNN model character vocabulary |
| `assets/rec_model_20230425.mnn` | 811 KB | **MNN FlatBuffers** | `ia/j.java:88` | App prediction/sorting (9 embedding + 5 linear + MaxPool, binary classification output) |
| `assets/model` | 680 KB | Custom binary (header `1,0,5,68,58` + float32BE weights) | **has no actual reader** (only `lj/c.java:17` name array `{"dict","model","pattern","phish"}` mentions) | `Unknown` (inference: text classification weight) |
| `assets/censor_source` | 2,688 B, entropy 7.933 | encrypted binary | `liblib-riskcontrol.so` `SensitiveWordDetector::load` | Sensitive word Trie tree vocabulary list (**key is not solved**) |
| `assets/rsapubkey` | 419 B | Java serialization `java.security.KeyRep` (RSA public key) | Unknown | Communication encryption/verification |
| `assets/protobuf/antispam.proto`+`syncroot.proto` | 2.3 KB + 252 B | protobuf IDL | Generate code | Backup/synchronization of harassment interception configuration (black/white list, keywords, policies, switches, notifications, automatic update library, SIM sharing, number tag guidance) |
| `assets/maml/**` | 28 documents | MAML card (`manifest.xml`+png) | MAML engine | Call/SMS blocking card, no SIM prompt card (light and dark theme) |
| `assets/dexopt/baseline.prof(.profm)` | 1.4 KB | ART baseline profile | system | Start optimization |

### 6.4 Built-in certificate and key material
| item | content |
|---|---|
| APK signature certificate | `CN=MIUI, OU=MIUI, O=Xiaomi, L=Beijing, S=Beijing, C=CN, E=[VENDOR_EMAIL_REMOVED]`, serial number `E552A8ECB9011B7C`, 2011-12-06 → 2039-04-23, RSA, SHA-256 `c9009d01…49025`, **v1+v2+v3 single signer, v2/v3 The digest is consistent with the bytes** |
| `res/raw/wlan_speed.crt` | `CN=www.miwifi.com` self-signed, **RSA-1024**, 2020→2030; as `miwifi.com` trust anchor |
| `res/raw/wlan_speed_new.crt` | `OU=MIWIFI ROOT CA` (issued by `CN=Xiaomi Root CA`), RSA-2048, 2020→**2120**; also serves as a trust anchor |
| `DISASTER_PUBKEY`（`DisasterConstants.java`） | RSA-2048 SPKI (base64, hard-coded) → Verify cloud warning data signature |
| `REQUEST_CITYCODE_SALT` | `7war5230-a1cf-3k79-ec73-75cb82145n5x` (`api.sec.miui.com/12379/warning/getRegionInfo` request salt) |
| `libtcp.so` constant | 13 segments `WALI_KEY_SEG_1..3` + `PROGUARD_1..10` (base64 assembly, `OHZoOG1zeTJo`/`ZXc5d3FkeA`/`zdGF0LnNlY3JldA`) + `0102030405060708` (IV shape) |
| `libsec-common-utils.so` | 6 SecretKey providers + certificate fingerprint pin `C8:A2:E9:BC:…:2A:B8` (does not match the actual signature of APK → another certificate) |
| `assets/placement_id` | `123=fb:196567740764356_196569107430886` (**Facebook ad slot ID**) |

---

## 7. Cloud interface panorama

> A total of 174 URL / 75 domain names, mapped to the reference class see `findings/url_usage.tsv`. The following table is classified according to **function** (all are `Observed`).

### 7.1 Security Center’s own domain
| domain name | path (key points) | Purpose | Quote |
|---|---|---|---|
| `adv.sec.miui.com` | `/info/layout` | **Homepage/result page card layout (including advertisements/activities/information)** | `antivirus/result/f.java:284`、`c0.java:263`、`optimizemanage/optimizeresult/e.java:244` |
| | `/info/videoAdv`、`/user/ac`、`/info/signinXunyou` | Video advertising, user portraits, Xunyou check-in | — |
| | `/game/pkg`、`/game/popGames`、`/game2/gameAdv`、`/game/fast_reply`、`/game/speedParams`、`/game/upgrade/pkginfo` | Game Center Promotion/Acceleration Parameters | `a8/k0.java`、`a8/k1.java`、`gamebooster/service/w.java` |
| | `/gameTurbo/list/leisureGame`、`/gameTurbo/gcgt/tab/feed`、`/gameTurbo/gcgt/tab/page-detail`、`/gameTurbo/migamegpt/game-ai/gt-setting-info` | Game Turbo Content Streaming + **Game AI Settings Recommendations** | `c9/d.java`、`c9/h.java`、`gamebooster/aihelper/f.java` |
| `data.sec.miui.com` | `/data/upload`、`/data/check`、`/adv/tail`、`/adv/game_ad/report`、`/privacy/{get,agree,revoke,latestVersion}/v1` | Buried upload, monthly report, advertising return, **privacy consent/withdrawal** | `monthreport/c.java`、`gamebooster/utils/c.java` |
| `api.sec.miui.com` | `/v1/traffic/report`、`/common/whiteList/{listByModule,trafficChargeNum}` | Traffic reporting, whitelist/billing numbers | `networkassistant/webapi/Constants.java` |
| | `/redPacket/reportStatus` | Red envelope assistant status reporting | `luckymoney/config/Constants.java` |
| | `/LBS/geoconv/admin` | Coordinate correction | `earthquakewarning/Constants.java` |
| | `/mnntrain/taskdata` | **client-side training task data** | — |
| | `/earthquake/volunteer/{register,cancelRegister,countVolunteers,sensor/upload}`、`/universalWhitelist/checkUserId` | **Earthquake early warning volunteers (including sensor data upload)** | `earthquakewarning/Constants.java` |
| | `/12379/warning/getRegionInfo` | **National Early Warning Information Release System (12379) Area Code** | `DisasterConstants.java` |
| | `/GuardProviderV1/GetResourceZipFile` | Exchange resource bundles with GuardProvider | — |
| | `/res/docs/disclaimer/av/privacy` | Antivirus Privacy Agreement | `antivirus/activity/MainActivity.java` |
| `srv.sec.miui.com` | `/earthquake/warning/{records,signature,supportCityCode}`、`/data/adb` | Earthquake records/signatures/supported cities; ADB installation verification | `AdbInstallVerifyActivity.java` |
| `flash.sec.miui.com` | `/gc/dir`、`/game/union` | Cloud control directory, game alliance | `ac/c.java`、`GuardService.java` |
| `api.miui.security.xiaomi.com` | `/netassist/flow/queryflow`、`/netassist/floworderunity/supportfloworder`、`/game/{queryXunYouUserInfo,xunYouReport,queryXunYouActivityList}`、`/views/netassist/productlist.html` | Data package inquiry/ordering, Xunyou account and reporting | `networkassistant/webapi/*`、`gamebooster/ui/BoosterFragment.java` |
| `embed.sec.miui.com` | `/earthquake-early-warning(/dsclaimer)` | Earthquake Early Warning H5 Instruction Page | `earthquakewarning/Constants.java` |
| `warning-mijia.sec.miui.com` | — | Mijia linkage early warning | — |
| `sd-gamespace.sec.miui.com`、`game.sec.miui.com/tencent/award/receive` | — | Game space/Tencent reward collection | — |
| `framework.sec.miui.com`、`sec-cdn.static.xiaomi.net` | — | Framework interface, static resources CDN | — |
| `url-detect.engine.mi.com/url/detect` | — | **URL Security detection (phishing interception)** | `antispam/policy/b.java` |
| `eps.caict.ac.cn/eps/collect` | — | **number mark reported to CAICT** | `com.xiaomi.security.xsof` |
| `connect.rom.miui.com/generate_204` | — | Connectivity detection | — |
| `pre-api.miui.security.xiaomi.com`, `api.device(.preview).xiaomi.net`, `account.xiaomi.com`, `micloud.*` (including preview environment) | — | Account/Cloud Service/Debugging Environment | — |

### 7.2 Ecosystem and operator domain
`mihall.10046.xiaomimobile.com/mimobile/consumption` (Calibration of call charges/traffic packages), `api-flow-intl.10046.xiaomimobile.com/*` (International Data Mall: Products/Orders/Privacy Agreement), `preview-api-flow-intl…` (Pre-release), `api-new-misim.10046.xiaomimobile.com/external/isSatisfyYYLLUser` (Operator user determination), **`net-assistant.10046.xiaomimobile.com/v1/traffic/directives` (native command issuance, see §8)**, `m.mi.com/maplocation/address?location_type=outlet` (Xiaomi Home Store), `api.jr.mi.com/activity/security/` (Financial Activities), `app.market.xiaomi.com` (Application Market Feedback), `miwifi.com/cgi-bin/luci/api/xqsystem/{maccel,ma_check}` (**Router Speed Test/Detection** ), `xunyou.mobi/article-*.html` (Xunyou Acceleration Instructions), `privacy.mi.com` / `privacy.miui.com` (Privacy Policy), `web.vip.miui.com` (Community Post), `beian.miit.gov.cn` (Registration number), `sdkconfig.ad.xiaomi.com` / `sdkconfig.ad.intl.xiaomi.com` (Advertising SDK configuration), `monitor-check.ad.xiaomi.com` (Advertising monitoring), `cn.register.xmpush.xiaomi.com` + `resolver.msg.xiaomi.net` (Push/long connection), `gateway-papa.cloud.weizhi.com` (Third party), `t14.market.mi-img.com` (Picture CDN ).

---

## 8. Native layer (18 libraries, arm64 + v7a dual architecture)

> Method: REA cannot use Ghidra/objdump to process aarch64 ELF on this machine (`Windows Ghidra P0` only supports PE), so we built our own AArch64 partial disassembler (ELF section + `.dynsym` + `.eh_frame_hdr` function table + ADRP/ADD/BL/B.COND/CBZ/TBZ/MOVZ/MOVK/LDR/STP Decoding + **PLT → Import symbol parsing**). See `findings/native-libs.md` for details.

| Library | Purpose | key observations |
|---|---|---|
| `liblib-riskcontrol.so` | **Sensitive word review** | JNI only `setAssetManager`/`hasSensitiveWords`; `SensitiveWordDetector::load/censor` + `TrieTree::insert/find` + self-implemented AES (ECB/CBC/CFB); `readFileFromAsset` reference string `censor_source`; debug coverage path `/storage/emulated/0/censor_source` exists |
| `libtcp.so` | **flow assistant native plug-in + command channel** | Unique hardcoded endpoint `https://net-assistant.10046.xiaomimobile.com/v1/traffic/directives`; `exec_protocol`: `base64_decode` → JSON → by `PluginRegister`/`MsgType`/`CmdType`/`DataType`/`CommandList` Distribution; capabilities include SMS template matching (`TplId/TplContent/MatchedTplIds/SmsFilterNum/SmsSendNum`), `getSubscriberId`, `setImsi`, `collectUserData`; 13 segments of `PROGUARD_*` constants (including Chinese spam text message corpus) |
| `libsec-common-utils.so` | **Key Center + Caller Access Control** | 6 `ConfigManager.get{Encode,Decode,Http,Uuid,Rubbish,NetworkAssistant}SecretKey`; `getSignature()` reflection chain `getPackageManager→getPackageInfo→getPackageName` + `[Landroid/content/pm/Signature;` + `sha256`; 4 package name whitelists; certificate fingerprint pin `C8:A2:E9:BC:…:2A:B8` (**does not match the actual signature**) |
| `libmicontinuity_sdk.so` | **Xiaomi Internet cross-device channel** | `micont::channel::ChannelImplBySocket::{SendFile,SendBytes,ContinueSend}`, `RtmSendRetErr`, `NInputReader_read/skip`; import socket/connect/accept/epoll/recvfrom, **none SSL_*** |
| `libxcrash.so` + `libxcrash_dumper.so` | crash capture | `libxcrash_dumper` **is not JNI Library**: import `__libc_init`, no `JNI_OnLoad`, no `loadLibrary` on the Java side, pulled up by the parent process, use `ptrace`/`/proc/self/maps`/XZ/`popen`/`regexec` capture the scene; `/data/local/{,bin/,xbin/}su` is root detection |
| `libMNN.so` / `libmnncore.so` / `libMNN_Express.so` / `libMNNTrain.so` | **Device-side reasoning + Device-side training** | `nativeCreateNetFromFile`/`nativeCreateSession(FORWARD_CPU,4,..)`；`nativeTrainModel`(12.9 KB)/`nativeLoadMapFromFile`(17.5 KB)/`nativeGetRecModelPredictScore`/`nativeTrainRecModel` |
| `libtensorflowlite_jni.so` | TFLite reasoning | `org/tensorflow/lite/TensorFlowLite.java:14` is loaded; there is also a TFLite side model in the App (`assets/model` does not locate the caller) |
| `libmiffmpeg.so` | FFmpeg | `Lavf58.45.100`; the only library with a universal network protocol stack (`getaddrinfo/socket/connect`); used with `libfastplayer.so` for media playback |
| `libjni_sound_effect(_2_0).so` | **WORLD Vocoder voice changer** | Export the complete set of `Dio/CheapTrick/D4C/Synthesis2`; host class `com.miui.gamebooster.encoder.SoundSupport` (reverse from string, JNI_OnLoad dynamic registration) |
| `libjni_load_serinum.so` | Device unique ID | `/dev/otp` + `ioctl` (MTK OTP), GCC 4.9 mtk compiled string |
| `libnap.so` | Network statistics | Parsing `/proc/net/xt_qtaguid/stats` (obsolete for Android 9+ and is a compatible path) |
| `libfastplayer.so` | lightweight player | Works with `libmiffmpeg` |
| `libc++_shared.so` | C++ runtime | standard library |

**common**: 18/18 stripped + BIND_NOW; `.rodata` reserved C++ RTTI/source file path → **unpacked**; `TracerPid|frida|xposed|magisk|ro.debuggable|isDebuggerConnected|substrate|gdbserver` **zero hit** → **does not see anti-debugging/anti-injection** (bounded negation); no network capabilities except `libtcp` and `libmiffmpeg`.

---

## 9. Third-party SDK and advertising/statistics stack (`Observed`, class package statistics)

| components | Number of classes | Notes |
|---|---|---|
| `com.google.*` | 1,906 | ExoPlayer 2 (media playback), Material, Gson, etc. |
| `com.xiaomi.onetrack` | 179 | Xiaomi’s self-developed tracking framework (local queue + batch upload) |
| `com.xiaomi.continuity` | 167 | Multi-device transfer/interconnection |
| `com.xiaomi.push` / `com.xiaomi.mipush` / `com.miui.push` | 105 / 70 / 2 | Xiaomi push |
| `com.miui.maml` + `miuix.*` | 221 | Dynamic UI engine |
| `com.milink.sdk` | 56 | MiLink screen projection/interconnection |
| `com.market.*`、`com.xiaomi.market` | 46 | App Market (Jump/Install) |
| `com.qti.*` | 43 | Qualcomm IZAT positioning related |
| `com.xiaomi.accountsdk` / `passport` / `micloudsdk` | 21 / 7 / 8 | Xiaomi account and cloud services |
| `com.miui.msa.util.MsaUtils` | 1 | **MSA Mobile Security Alliance OAID** Read |
| `com.miui.systemAdSolution` | 3 | **open screen advertising** solution (`adSwitch.PROVIDER`) |
| `com.miui.hybrid.accessory` | 49 | Accessories/Quick Application Card (`api.hybrid.xiaomi.com`) |
| `com.milink`, `airbnb.lottie`(19), `okhttp3`, `dagger`, `androidx.*`, `kotlinx.*` | — | Regular dependencies |
| Advertising space | — | `assets/placement_id` = Facebook `fb:196567740764356_196569107430886`; `AdvInternationalCardModel`/`AntiGlobalAdBean`/`OptimizeGlobalAdBean` are international version advertising switch models |

**advertising link summary**: `SystemAdContentProvider` (authority `com.miui.securitycenter.splashAd`, exported) + `AdImageView`/`AdDownloadView` + `adv.sec.miui.com` layout/material + `sdkconfig.ad.*` configuration + `data.sec.miui.com/adv/*` postback + MSA OAID + Facebook bit. **'s homepage interstitial advertisement also earns "security points"**: `MainFragment.f20481n` (`NativeInterstitialAdLayout`) receives `ScoreManager.j().q()` and presses the threshold on `shape_interstitial_ad_blue`/`shape_interstitial_ad_yellow` Switch between backgrounds (`NativeInterstitialAdLayout.java:35-46`) - that is, **physical examination scores are used to display** in advertisements.

---

## 10. Security and Privacy Assessment

### 10.1 Exposed surface (exported component)
- Among **424 components, `exported=true` has a total of 194** (all explicitly declared, no default export); among them, **, 128 zero-permission protected**, and `enabled=false` are exported only 5 (including `MainEntryActivity`)→ **currently has zero permission to export ≈ 127**. `Observed`
- Most of them are pulled up by system settings/desktop/other system applications through custom actions (`miui.intent.action.OP_AUTO_START`, `ELECTRIC_RISK`, `EARTHQUAKE_WARNING_MAIN`, `PRIVACY_RISK_DETECTION_ACTIVITY`, `OPEN_WIFI_VERIFY`, `miui.intent.action.SECURITY_CENTER`...). Third-party applications can directly pull up these interfaces (UI deception/information detection risks depend on the interface's own secondary verification).
- **should give priority to the zero-permission export** (`Observed`):
  - `com.miui.sec.external.InstallProvider` (authority **`com.miui.sec.INSTALL_PACKAGE`**)——Installation channel
  - `com.miui.securitycenter.provider.DeviceCredentialProvider` (`com.miui.securitycenter.credentialprovider`) - Device Credentials
  - `com.miui.securitycenter.provider.SecSettingsSearchProvider` —— **only exported + no permission + `grantUriPermissions=true`**
  - `com.miui.idprovider.IdProvider`（OAID）、`com.miui.networkassistant.vpn.miui.MiuiVpnManageService`、`com.xiaomi.security.xsof.XSOFProvider` + `MiSafetyDetectService`、`com.miui.warningcenter.provider.WarningCenterProvider`、`com.miui.securitycenter.SystemAdContentProvider`、`com.miui.autotask.provider.AutoTaskServiceProvider`、`com.miui.permcenter.privacycenter.PrivacyCenterProvider`、`com.miui.securityadd.input.InputProvider`
- **has two high-risk UI surfaces**: `permcenter.permissions.SystemAppPermissionDialogActivity` (connected to `miui.intent.action.SYSTEM_PERMISSION_DECLARE(_NEW)`/`CTA_DECLARE` + `content:/file:/image/*` → forged system authorization pop-up window), `networkassistant.ui.base.UniversalFragmentActivity` (connected to `com.miui.datatraffic:` scheme → Fragment injection surface).
- **has two permission gaps**: `miui.permission.TVM` and `com.miui.securitycenter.permission.PERM_USE_INFO` **misses writing `protectionLevel`** (=normal); the actual protection of `AppPermissionsUseActivity` protected by the latter fails, and the `TvmManagerService` protected by the former fails due to `enabled=false` Currently unreachable.
- Most of the 40 Providers run in the `remote` process, with authority coverage `antispam`, `autotask`, `earthquake`, `disaster`, `gamebooster`, `networkassistant`, `powercenter`, `warningcenter`, `securitycenter`, `xsof`, `idprovider`, `app.hide` (Privacy Space), `permissions.acrossterminal`, `remoteprovider`.

### 10.2 High-risk capabilities (all available due to system UID)
`MASTER_CLEAR`, `REBOOT`, `FORCE_STOP_PACKAGES`, `CLEAR_APP_USER_DATA`, `CLEAR_APP_CACHE`, `DELETE_CACHE_FILES`, `PACKAGE_VERIFICATION_AGENT`, `MANAGE_USB`, `WRITE_SECURE_SETTINGS` Class (`READ_SECURE_SETTINGS`/`READ_SETTINGS`), `MODIFY_PHONE_STATE`, `READ_SMS`+`SEND_SMS`+`READ_CALL_LOG`+`WRITE_CALL_LOG`, `RECORD_AUDIO`, `READ_CONTACTS`, `GET_ACCOUNTS`/`MANAGE_ACCOUNTS`/`USE_CREDENTIALS`, `CONTROL_VPN`, `SYSTEM_ALERT_WINDOW`, `DISABLE_KEYGUARD`、`BIND_QUICK_SETTINGS_TILE`、`GET_APP_OPS_STATS`/`UPDATE_APP_OPS_STATS`、`PACKAGE_USAGE_STATS`、`BATTERY_STATS`、`ACCESS_WIFI_STATE`+`CHANGE_WIFI_STATE`+`CONFIGURE_WIFI_DISPLAY`, `MANAGE_NETWORK_POLICY`/`MODIFY_NETWORK_ACCOUNTING`, `miui.permission.SHELL`, `miui.permission.FIREWALL`, `com.miui.security.permission.installOtherApp`, `com.android.settings.permission.SWITCH_DEV_MODE` Wait.

### 10.3 Transmission security
- `network_security_config.xml`: **`<base-config cleartextTrafficPermitted="true">`** (plaintext HTTP is allowed globally, without `domain-config` restriction) → consistent with a large number of `http://*.micloud.xiaomi.net` plaintext endpoints in the code.
- But `miwifi.com`/`www.miwifi.com`/`router.miwifi.com` **trusts two additional private certificates**: `wlan_speed.crt` (**RSA-1024** self-signed, `CN=www.miwifi.com`) and `wlan_speed_new.crt` (`MIWIFI ROOT CA`, valid until **2120**). → For this domain, the trust anchor is replaced by the vendor CA, and the **standard PKI checksum is bypassed** (only for these 3 domains).
- The business request comes with `sign` (MD5+Base64+APP_KEY) **instead of TLS. The certificate is fixed to**; that is, "anti-tampering relies on self-developed signature", and `sign` uses MD5 (weak).
- **caliber limit (2026-11-06 review and addition)**: `cleartextTrafficPermitted=true` and the private CA are **configuration fact**, which does not mean that the above interface is really in the clear; there is indeed `http://*.micloud.xiaomi.net` literal in the code, but **does not prove this URL is actually called** on the networking path. Packet capture is required to determine leakage, and configuration cannot be used in place of evidence.

### 10.4 Privacy impact (categorized by capabilities)
| Ability | realize | Risk point |
|---|---|---|
| SMS reading/hide/categorization | `antispam` + `assets/pattern` + `MessageInfo.db` | Can read and save text message content locally |
| Number attribution/tag reporting | `xsof` → `api.sec.miui.com` (number library/mark); `eps.caict.ac.cn` uses the **alarm-assisted emergency call positioning report**, not the number mark | Number and label uplink; the two links need to be viewed separately |
| Device identification | `libjni_load_serinum` (OTP unique ID), **advertising/business request `x`(AndroidID)+`oa`(OAID)+`m`(IMEI-MD5)+`deviceId`(SHA1(IMEI+AndroidID+serial number))**, MSA OAID | **carries multiple associated identifiers** in a single request; hashing does not change linkability; `ro.miui.restrict_imei` only suppresses the IMEI component in `m`/`deviceId` |
| Input method monitoring | `df.h` monitors `default_input_method`, `securityadd/input/InputProvider`, `safepay_imm_whitelist` | Input method switching detection + payment keyboard whitelist |
| Accessibility | `AntiMsgAccessibilityService`（gamebooster） | Read screen content/automate operations |
| notification listening | `BubblesNotificationListenerService` | Read all notifications |
| Screen/floating window | `SYSTEM_ALERT_WINDOW`、`DockWindowManagerService`、`FreeformWindowService` | Cover other application interfaces |
| Camera/Microphone | `RECORD_AUDIO`, `beauty` (beautification/voice change), `PrivacyCamera` | Process audio and video during a call |
| location | `ACCESS_FINE_LOCATION`; alarm assistance (AES+RSA is encrypted and uploaded to the third party `gateway-papa.cloud.weizhi.com`); earthquake volunteer (OAID+latitude and longitude+timestamp+model+ROM+token+sensor array, `/earthquake/volunteer/sensor/upload`) | Both links have independent scene switches/volunteer protocols and UI checks. **uploads** when it is not turned on; volunteers report **non-anonymous** (including OAID) |
| Clipboard/Screenshot | Privacy blur `com.miui.blur`, `PRIVACY_THUMBNAIL_BLUR_SETTING` | Thumbnail/notification content is blurry |
| App Lock/Privacy Space | `applicationlock`、`privacyapps`（`app.hide`） | Local encryption and disguised entrance |
| Application behavior record | `BehaviorWidget`、`behavior_record_white.csv` | Record application behavior and release according to signature whitelist |

### 10.5 Private data flow review (Added on 2026-11-06, return to source code one by one for verification)

> Note: This section is a gradation of §10.4, answering "Which are the privacy issues with the strongest evidence, and which are just capabilities". **Evidence strength ≠ Illegal/violative conclusion**; how the server uses the data and how the first agreement is notified have not been verified.

**A. The strongest evidence (it is recommended to qualitatively follow "collection beyond user intuition")**
| link | Specific content | Evidence |
|---|---|---|
| **Multi-identity bundling of ad requests** | The domestic branch's request for `adv.sec.miui.com/info/layout` also contains: `x`=Android ID, `oa`=OAID (added only after successful reading), `m`=**IMEI MD5**, `deviceId`=**SHA1 (IMEI+AndroidID+hardware serial number)**, `rbr`=battery%, `ror`=storage remaining proportion, plus model/system/region/operator/language/network type | `eg/j.java:545-575` (`x`/`oa` insertion), `com/miui/antivirus/result/f.java:272` (`deviceId`), `q2/a.java:18-46` (SHA1 combination), `eg/c.java` (`a()`=AndroidID, `c()`=getDeviceId) |
| Key points | **"Personalized advertising switch" is only brought as a request parameter (`isPersonalizedAdEnabled` of `xe/a.java:19-28`). The checked link does not have the branch** with "close and stop splicing identification"; MD5/SHA-1 does not constitute anonymization, the same input constant output → can be associated across requests/cross applications | Same as above |
| Limit | The IMEI component is controlled by `ro.miui.restrict_imei(_p)` (`eg/c.java:l()`); ordinary requests still check the total network switch → **cannot be pushed to "refuse to connect to the Internet and still secretly transmit"**, nor can it prove how the server portraits | — |

**B. Strong evidence (clear trigger conditions and purpose, but scope/notification are negotiable)**
| link | content | trigger/consent | Evidence |
|---|---|---|---|
| **privacy risk detection → application list upload** | `PackageManager.getInstalledPackages(0)` filter `FLAG_SYSTEM` → `{package name, version number, application name, uid}` → `POST flash.sec.miui.com/user/ra` (`biz_id="rivacy_risk_check"`, AES/ECB+GZIP+Base64) | Enter the privacy risk detection page **and automatically start the detection task**; check the total network switch before sending; **this direct link does not have a secondary confirmation for sequential uploads** | `xc/h.java:13` (no launcher filtering), `ac/a.java:63-76`, `ac/c.java:118-160` |
| privacy implications | Exposing installed medical/financial/dating/work applications → High profile value; cloud risk detection can explain the purpose of the function, but "whether the entire list is needed" is negotiable | First agreement/notification from upper management that no real machine verification was done | — |
| **traffic service request** | `netassist/flow/queryflow` carries `imsi`, `iccid`, `phonenum`, `zipCode`, `spType`, `monthused`; **field `imei` is actually AndroidID+fixed UUID The hash of** | Traffic query scenario + networking switch | Starting from `WebApiAccessHelper.java:80` |
| **calibration SMS report** | Report SMS text, instructions, receipt number, province, city, operator → `api.sec.miui.com/v1/traffic/report`; `getEncryptPhoneNumber()` ** is just `*` mask ** | **has a clear pop-up window**: `TcSmsReportFragment.tcSmsReportDeclare()` → `OptionTipDialog` After confirming "Upload" `reportSms()` | `TcSmsReportFragment.java:416-430` |
| **Alarm auxiliary** | Longitude and latitude + text address + OAID + available local number + calling number/time + positioning start and end → `gateway-papa.cloud.weizhi.com/papa/upload` (**AES/CBC + RSA, non-clear text**); and `eps.caict.ac.cn/eps/collect` (**emergency call location report**, SM3 signature) | Alarm auxiliary scene + setting switch + protocol entrance | `PaCommon.java:5,22`、`PaUtils.java:97,353,363` |
| **Earthquake Volunteer** | OAID+latitude and longitude+time stamp+model+ROM+token+sensor array → `/earthquake/volunteer/sensor/upload` (and merge public device parameters, **is not anonymous**) | Volunteer joining process + UI privacy check; ≠ all users who turn on earthquake warning | `EarthquakeMonitorDataManager.java:55` etc. |

**C. Ability exists but has not been confirmed (should not be listed as "occurred")**
| item | facts | notch |
|---|---|---|
| Notification content access | The bubble component can grant notification monitoring on its own on supported devices; game-related accessibility services will screen QQ/WeChat notifications and match text | **The complete link** that the notification text sent to the remote end was not found |
| Input method related | `df.h` monitors changes in `default_input_method`, `GuardService` detects the third-party input method and switches back to the system IME | Monitor switching ≠ record keystrokes |
| Advertising color matching | Home page interstitial ads change to blue/yellow background according to safety threshold | Color change ≠ Safety score has been uploaded |
| Monthly report upload | `ma/c.java`'s `a()` Heng `false` → This build does not start | Whether this is the case only for this build is unknown |
| FBO package list | `FboPackageBean.upLoadPackageList()` writes `Settings.Secure("FBO_UPLOAD_LIST")` to the local machine FBO service | Method name ≠ Internet upload |
| Plain text HTTP / Custom CA | Global `cleartextTrafficPermitted=true`; `miwifi.com` Trust Private CA | The above interfaces all use HTTPS. **cannot be used to conclude that the clear text of** is leaked. |
| No permission to export components | There are indeed 127 zero-permission exports on Manifest | Some Providers have code layer `Binder.getCallingUid()`/package name/signature verification (for example, `RemoteProvider.java:180-194` uses `getPackageNameByPid` to only release this package and `com.miui.klo.bugreport`) → **Manifest No permission ≠ No authentication** |

**D. Conclusion caliber**
- It can be said that **has multiple private data collection and outward transmission paths; the multi-identity association of advertising links and the upload of non-system application lists on the privacy detection page are the two strongest evidences.**
- It is not allowed to say: **"All data processing is without consent"** (multiple links have switches/pop-ups/volunteer agreements), **"Refuse to connect to the Internet It is still necessary to upload "** (no branch that bypasses the main switch), **" data export "**" (only the domain name is not Xiaomi, and the server ownership has not been verified).
- Still missing: the actual scope of notification of the first launch agreement/privacy policy, the use of server-side portraits, and real machine packet capture verification.

---

## 11. Unsolved mysteries (clearly left blank)

| # | question | known | notch |
|---|---|---|---|
| 1 | `assets/censor_source` encryption algorithm/key | Read by `SensitiveWordDetector::load` of `liblib-riskcontrol`, AES + Trie; single-byte XOR, 25 groups of password derivation, deflate/gzip/brotli have been excluded; all 16/32 B continuous windows (about 490,000) in **.so are used for operation AES-128/256 key + PKCS5 of ECB/CBC all failed verification** | The key is not any continuous byte window → Requires Linux+Ghidra/rizin to disassemble `_ZN21SensitiveWordDetector4loadEPc` and chase `AES::KeyExpansion` actual parameters, or real machine hook |
| 2 | Reader and obfuscation algorithm of `assets/garbage_clean.db` | 3 dex + 18 so No reference to `garbage_clean`; data has stream cipher characteristics (equal-length prefix repetition + arbitrary length); `t_ad`/`t_residual` 0 lines | May be external `com.miui.cleanmaster` or historical legacy (dead asset) |
| 3 | `assets/model` (680 KB) Format/Use | Header `1,0,5,68,58` + float32BE weight; the name array of `lj/c.java:17` contains `model`/`phish`, but **does not have any caller in the whole tree. `lj.c.b(ctx,"model")`**, `phish` resources do not exist. | Need to find the actual loader or format specification |
| 4 | Virus scanning engine and cloud scanning interface | Engine/signature library/cloud killing URL **is not included in APK**, `com.miui.guardprovider` is entrusted by AIDL `IAntiVirusServer` (20 methods) and `content://guard/*` | GuardProvider APK is required to answer "Signature library format/cloud scanning and killing protocol" |
| 5 | Who does the certificate of `libsec-common-utils` pin `C8:A2:E9:BC:…:2A:B8` belong to? | Does not match the actual signature of this APK (`c9009d01…`) | MIUI platform certificate sample required for comparison |
| 6 | `WALI_KEY_SEG_*` splicing sequence of `libtcp` | 13 constant literal values have been solved (including Chinese text message corpus) | Need to track `init_weak_string`/`destroy_weak_string` assembly logic |
| 7 | Is the Rtm layer of `libmicontinuity_sdk` encrypted? | Import symbol None `SSL_*` | Requires protocol reversal or packet capture |
| 8 | `libjni_load_serinum` ioctl request number/return structure | `/dev/otp` + ioctl（MTK OTP） | Requires decompiler or strace |
| 9 | Complete fields of cloud card layout protocol | Only the request side and partial parsing (`functionId`/`sign`/advertising parameters) | Server response sample required (packet capture) |
| 10 | The implementation form of `setMiuiFirewallRule` in framework→netd | APK only sends calls and receives broadcasts | Requires `services.jar`/framework side |
| 11 | `com.miui.antifraud.d`(13.9 KB) Determination rules and reporting payload | Only confirm the entry and transfer page lists | Need to expand this category |
| 12 | `XSOF` (`MiSafetyDetectService`) each API semantics | Method name is completely confused (`C3/d8`) | Need to read `hj.*`/`ij.*` support category |
| 13 | Runtime behavior of each module (actual interception rate, cleaning effect, whether training actually occurs) | — | Real machine dynamic verification is required (not included in this report) |

---

## 12. Appendix

### 12.1 Evidence product index
| File | content |
|---|---|
| `findings/_BASELINE.md` | Shared factual baseline (with 3 subsequent corrections) |
| `_analysis/PRIVACY_REVIEW_20261106.md` | **Independent privacy review report** (field/call chain/receiver/switch condition + 42 source code references; §10.5 of this report is the incorporated version of its conclusions) |
| `findings/assets-data.md` | Line 717: All assets/res.raw/certificate/signature deep dives (including dict/IDF complete decoding, db full rows, AES blasting records) |
| `findings/native-libs.md` | 18 .so database-by-library analysis + 6 unsolved mysteries |
| `findings/manifest-sdk.md` | manifest/permissions/components/signatures/SDK/obfuscated-structure |
| `findings/core-security.md` | Physical examination/antivirus/harassment interception/anti-fraud/red envelope/alarm assistance |
| `findings/network-warning.md` | Network assistant/firewall/traffic/earthquake/disaster/AI anti-fraud protection (including 55 KB special appendix `_parts/`) |
| `findings/privacy-permcenter.md` | Permission Center/Privacy Space/App Lock/Privacy Blur/OAID |
| `findings/power-gaming.md` | Power saving/performance/cleaning/game acceleration/automatic tasks/device-side AI |
| `findings/components_all.tsv` | Full list of 424 components (kind/name/exported/permission/actions/process) |
| `findings/url_usage.tsv` / `jadx_urls.txt` / `dex_urls.txt` / `dex_strings_all.txt` | URL and full string |
| `tools/*.js|mjs` | jieba dictionary analysis, IDF analysis, AArch64 disassembly, AES blasting, asset fingerprint and other recurrence scripts |

### 12.2 Key code index (precise positioning)
| Topic | location |
|---|---|
| Process offload initialization | `com/miui/securitycenter/Application.java:711`（`onCreate`）、`:683`（`attachBaseContext`） |
| Physical examination item configuration | `assets/scanitem.json` (v8, 28+8 group) → `com/miui/securityscan/scanner/*`, `ScoreManager.java` |
| Home/Results Page Card | `com/miui/common/card/**` (49 CardModels) |
| HTTP and signature | `eg/j.java` (`NetUtil`): static parameters `:129-176`, signature `m()` `:764-795` |
| Advertisement parameters | `xe/a.java:19-58`（`isPersonalizedAdEnabled`/`cv`/`ror`/`rbr`） |
| layout interface | `antivirus/result/f.java:284`、`c0.java:263,283`、`optimizemanage/optimizeresult/e.java:244` |
| directory rule base | `ra/e.java:39-60` (copy `R.raw.parse`), `:148` (cloud overwrite), `:211/256/267` (query/assembly) |
| word segmentation dictionary | `lj/g.java:32` → `mj/a.java:260-330` |
| Pinyin index | `am/a.java:47/57/73` → `cl/a.java` (IDF v2), `:121-197` (polyphonetic/surname coverage) |
| App prediction model | `ia/j.java:88-294` (load/input/output), `ia/d.java:71-182` (copy/encode), `ea/c.java:21` (vocabulary) |
| Sensitive word Provider | `com/miui/riskcontrol/SensitiveWordDetectProvider.java:23` |
| key center | `com/miui/securitykey/ConfigManager.java:42` |
| Native command channel | `com/miui/sdk/tc/TcManager.java:116` |
| disaster constant | `com/miui/warningcenter/disasterwarning/DisasterConstants.java` (12379 URL/SALT/RSA public key/level bitmask 1/2/4/8) |
| Earthquake warning constant | `com/miui/earthquakewarning/Constants.java` |
| Network configuration | `res/xml/network_security_config.xml` |
| Signature file | `META-INF/PLATFORM.SF`、`PLATFORM.RSA`（CN=MIUI） |
