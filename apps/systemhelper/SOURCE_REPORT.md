> **历史分析材料，含已纠正结论，不能作为最终结论引用。** 请先阅读 [最终复核报告](REPORT.md)。原材料中的“MD5 派生密钥等于不加密”“全量已安装应用清单”“隐私告知永远不可达”“设为 1 秒即每秒上传”“无时间戳/已证实重放”等说法已被收窄或纠正；本次公开版未重新验证全部 OEM 分支。已删除本地身份路径，未包含真实用户采集值。

# SystemHelper（com.mobiletools.systemhelper）完整逆向分析报告

> 目标：`[LOCAL_PATH_REMOVED]`
> 分析方式：REA（MCP，v6.8.0）Android/JADX 静态分析 + apktool 3.0.3 全量反编译 + 四路并行深度分析（Lead 汇总与交叉复核）
> 权威度约定：**【观测】**=字节码/资源直接可见；**【推断】**=由调用关系或字符串语义推出；**【未知】**=本样本内无证据。

---

## 0. 一句话结论

这是一个**预装型（/system/priv-app）中国联通终端信息采集/注册组件**（显示名 "SystemHelper"，内部自称 "运营商服务"）：以"设备管理（DM）"名义常驻，开机后经 AlarmManager 定时（4 分钟首启 / 24 小时常规 / 7 天应用清单），把**双卡 IMEI、IMSI、ICCID、MSISDN、当前小区 CellID/LAC、VoLTE 与 5G 开关状态、机型厂商版本、存储与内存画像、以及最近 7 天应用使用与流量统计**序列化成 JSON，用 `AES/MD5/RSA` 三重处理后，**以明文 HTTP** POST 到 `http://dm.wo.com.cn:18080/registerv3`（中国联通 DM 平台，端口 18080），协议版本 `SpecVersion=3.0`、`RegVersion=1.0`。

工程实现质量与安全强度都很低：内置 **RSA-1024 自签名"CN=UNICOM"证书**（有效期至 2119 年）、AES 密钥由**明文自身的 MD5** 派生（等于不加密）、导出且**无任何权限保护的 AIDL 服务**可被任意第三方应用改写上报地址/上报周期、隐私告知字符串在代码中**零引用**、桌面图标是**系统 Android 机器人**。

---

## 1. 样本身份与分析方法

### 1.1 样本身份【观测】

| 项 | 值 |
|---|---|
| 文件名 | `SystemHelper-1.2-20230925-U.apk` |
| SHA-256 | `efa1af0d0be75985701fd0eca79a90f0be0bb9fd2735245915200ec1a3e9657f` |
| 大小 | 911,821 字节 |
| 包名 | `com.mobiletools.systemhelper` |
| versionName / Code | `1.2-20230925-U` / `3` |
| minSdk / targetSdk / compileSdk | 24 / **28** / 33 |
| sharedUserId | `com.mobiletools.systemhelper.uid`，`sharedUserMaxSdkVersion=32` |
| application 类 | `com.mobiletools.systemhelper.control.DMApplication` |
| 其它 | `usesCleartextTraffic=true`、`allowBackup=false`、`extractNativeLibs=false`、`appComponentFactory=android.support.v4.app.CoreComponentFactory` |

### 1.2 签名【观测】

- 仅有 **v2** 方案（无 v1/v3/v3.1），1 个签名者，`self_consistent=true`；无密钥轮换谱系、无 source stamp。
- 签名证书 SHA-256：`874b8ca2297f53a373b1b935c134cb0eb1ff7381e54491b88c643a6b9c46db77`
- **签名未被密码学验证**（REA 明确：`signature_verification=not_performed`），`self_consistent` 只表示"声明摘要与当前字节一致"，不构成真实性证明。
- 签名者身份与"中国联通官方"之间**无本样本内证据可证**（见 §11）。

### 1.3 分析工具链与证据

| 环节 | 工具 / 操作 | 证据 ID（REA） |
|---|---|---|
| 包/组件/权限 | `inspect_android_package` | `ev_d49242be02b911af209753b080d8a8ea43ddf1342c2017f8f52f07c1f14ade1c` |
| 解码后 Manifest（4380 字符全文） | `inspect_analysis_view{kandroid,facet:manifest}` | `ev_ae62a3b5e3f0a068aa535829c6ab72d400ba6b63dce66b6bdb14e0ac8082e8b7` |
| 包内 409 条目清单 | `inspect_artifact` + inventory view | `ev_c1904f4e2b323b3a64d21db0f4e1e8df2255f95998339357169de424d68bdd33` / `ev_39fad11288bc176284d5be5e43aacd9ebc5ebf210c33e6f4373e0af6dfd43445` |
| 签名结构 | `inspect_apk_signature` | `ev_8001f625fd02e3bace98a316edbbbf5fc4d9926c7807d5f0bdfd69d90e97c415` |
| 476 个顶层类全名单 | `search_android_classes` | `ev_0235e39e76b9d6b6307467e688dddea1a11fe9d0d1d07729b1d3fe46076a57f1` |
| 资源与自有字符串 | `decode_android_resources`（apktool 3.0.3） | `ev_9d7c3abf81048f4137b6fa92c5847ac8309c75f91741b840f83cdde591432cdf` |
| 上报线程 Java 源码 | `inspect_android_method d0.k.run` | `ev_408dcf228ff95090d81dc7686f653e63aec9b70366ceea9e59bc914871e29a85` |
| 全局配置字段（28 个静态域） | `inspect_android_class f0.f` | `ev_15bbb4403c5e6a0319d2c0b14030a062de71f838d038a0e0e6daabc7076e1474` |
| 其余结论 | apktool 全量 smali（783 文件）+ 交叉复核 | 见各节 `文件:行号` |

**反编译产物**：`analysis\SystemHelper\apktool\`（smali/res/assets/original/unknown）、`all_const_strings.txt`（1880 条去重常量）、`method_index.txt`（387 个非库类的完整方法签名索引）。四份分项报告见 `findings\`。

### 1.4 覆盖度与限制

- 覆盖：`com.mobiletools.*` 15 个类、`e0/*` 85 个、`d0/*` 15 个、`f0/*` 15 个、工具/常量类若干；**自有代码 100% 逐类过目**（库代码：`android.support.*`、`androidx.*`、`android.arch.*` 共 390+ 文件为 AppCompat/ConstraintLayout/生命周期库，仅识别不展开）。
- **未执行**：APK 从未运行（无 adb/模拟器参与），所有行为均为静态推断；无真机验证。
- apktool 解出的 `res/**/*.xml` 中文为乱码（工具编码往返所致），**中文原文以 REA `decode_android_resources` 与 `resources.arsc` 解码结果为准**。
- 字符串检索边界：`all_const_strings.txt` 仅覆盖 `const-string` 指令；`static final String` 编译期常量与拼接串可能缺漏（已用 `f0/f` 静态域补齐关键常量）。

---

## 2. 应用构成

### 2.1 Manifest 全文要点【观测】

```
<permission android:name="com.mobiletools.systemhelper.permission" android:protectionLevel="signature"/>
<uses-permission INTERNET, RECEIVE_BOOT_COMPLETED,
  ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION,
  READ_EXTERNAL_STORAGE, ACCESS_NETWORK_STATE,
  READ_PHONE_STATE, READ_PRIVILEGED_PHONE_STATE,
  PACKAGE_USAGE_STATS, QUERY_ALL_PACKAGES, SCHEDULE_EXACT_ALARM/>
```
- 声明了 signature 级自定义权限 `com.mobiletools.systemhelper.permission` —— **全工程零使用**（既未 `uses-permission`，也未给任何组件加 `android:permission`），属死声明。
- `READ_PRIVILEGED_PHONE_STATE` 与 `PACKAGE_USAGE_STATS` 对普通应用不可获得，必须由系统授予（见 §2.5）。

### 2.2 组件清单【观测】

| 类型 | 名称 | exported | 过滤器 action |
|---|---|---|---|
| activity | `Activity.TestActivity` | **true** | `com.mobiletools.systemhelper.activity.testactivity` |
| receiver | `control.OnBootReceiver` | **true** | `BOOT_COMPLETED`、`QUICKBOOT_POWERON`（`priority=2147483647`） |
| receiver | `control.SIMStateReceiver` | **true** | `SIM_STATE_CHANGED` |
| receiver | `control.ConnectStateReceiver` | **true** | `CONNECTIVITY_CHANGE` |
| receiver | `control.ScheduleRegisterReceiver` | false | — |
| receiver | `control.AppInfoRegisterReceiver` | false | — |
| service | `service.RegisterHelperService` | **true** | `com.mobiletools.systemhelper.service.IRegisterHelperService` |
| service | `service.RegisterIntentService` | false | — |
| service | `service.RegisterJobService` | false（`android:permission=BIND_JOB_SERVICE`） | — |

### 2.3 包内容【观测】

- 单一 `classes.dex`（979,860 B），**476 个顶层类**、783 个 smali 文件；**无 `lib/` 原生库**（纯 Java/Kotlin-free）。
- 资源 408 项（绝大多数为库资源，`res/` 文件名被 R8 收缩成 `-A.png`、`0H.9.png` 之类）。
- **唯一天然资产**：`assets/pub.cer`（596 B，见 §6.2）。
- `META-INF/com/android/build/gradle/app-metadata.properties`：`appMetadataVersion=1.1`、`androidGradlePluginVersion=7.4.1`。
- `original/AndroidManifest.xml` = 原始二进制清单（8120 B）；`unknown/` = 22 个 `META-INF/*.version`。

### 2.4 权限用途对照【观测 + 推断】

| 权限 | 实际用途（证据） |
|---|---|
| INTERNET | 上报（`f0/g`）、取公网 IP（`d0/h`，本版未启用） |
| RECEIVE_BOOT_COMPLETED | 开机自启（`OnBootReceiver`） |
| ACCESS_FINE/COARSE/BACKGROUND_LOCATION | 定位：`d0/f$a` 的 `LocationManager.requestLocationUpdates`（本版被开关关闭）＋ 部分 OEM reader 取 `getLastKnownLocation`（库代码残留路径） |
| READ_PHONE_STATE | IMEI/IMSI/ICCID/MSISDN/小区（`e0/r`、OEM reader） |
| READ_PRIVILEGED_PHONE_STATE | 同上，用于无 `READ_PHONE_STATE` 运行时对话框即可读（需系统授予） |
| PACKAGE_USAGE_STATS | `d0/i` 的 `UsageStatsManager.queryUsageStats` 应用使用统计 |
| QUERY_ALL_PACKAGES | `d0/i` 遍历已安装应用（Android 11+ 需要） |
| ACCESS_NETWORK_STATE | `f0/l.l()`/`o()` 判断网络类型（Mobile/WiFi/Disconnected） |
| READ_EXTERNAL_STORAGE | 未见实际读取路径（`f0`/`e0` 未出现 `getExternalStorage*` 读取）→ 疑似冗余【推断】 |
| SCHEDULE_EXACT_ALARM | `f0/a` 的 `setExactAndAllowWhileIdle` |

### 2.5 privapp 白名单（关键设计证据）【观测】

`res/xml/motosystemhelper.xml`：
```xml
<permissions>
  <privapp-permissions package="com.mobiletools.systemhelper">
    <permission name="android.permission.PACKAGE_USAGE_STATS"/>
    <permission name="android.permission.READ_PRIVILEGED_PHONE_STATE"/>
  </privapp-permissions>
</permissions>
```
该文件**未被 Manifest 引用**（无 `android:networkSecurityConfig`/`android:permission` 之类引用），是给 ROM 集成方放进 `/system/etc/permissions/` 用的白名单。**结论：本 APK 的设计部署形态是预装进 `/system/priv-app/` 的运营商定制固件组件**，而非应用商店分发。
（对照：`res/xml/network_security_config.xml` 存在但同样未被引用——真正生效的是 `usesCleartextTraffic=true`。）

### 2.6 资源、图标与"伪装"【观测】

| 项 | 内容 |
|---|---|
| `app_name` | `SystemHelper`（英文，`values-zh-rCN` **未覆盖**） |
| `setting_advice_title` | `运营商服务（SystemHelper）` |
| `setting_advice_content` | `运营商可能通过 WLAN 或移动数据网络收集您的设备信息等数据，开启即视为同意联网、访问应用列表和定位。` |
| 自有布局 | 仅 `activity_test.xml`：ConstraintLayout → ScrollView → TextView（id `tv_device_info`，默认文案 "Device Info"）；**无任何开关/设置控件** |
| 主题 | `AppTheme` = Android Studio 默认模板（`#3f51b5/#303f9f/#ff4081`） |
| 图标 | 5 套密度 `ic_launcher`(+`round`)，图案为**绿色 Android 机器人（bugdroid）**，无任何品牌/运营商语义 |
| 字符串引用 | **`setting_advice_title` / `setting_advice_content` 在 smali 与 res 中零引用**，隐私告知永远不可达 |

---

## 3. 总体架构

### 3.1 分层【观测】

```
com.mobiletools.systemhelper.*        ← 外壳：Application / 4 Receiver / 3 Service / 1 Activity
        │  (触发)
        ▼
e0/c2   DeviceManager：RegType 编排 + 厂商 Reader 选择 + 上报触发
e0/r    DeviceInfoReaderBase：采集模板（VoLTE/5G/小区/运营商/双卡写入）
e0/*    8 个 API 桥 + 33 个 OEM Reader（Samsung/Xiaomi/OPPO/vivo/HUAWEI/...）
        │  (写入数据)
        ▼
d0/g    DeviceInfo DTO（26 个 String + 3 个 JSON 容器 + 2 个 int）→ JSON 序列化
        │
        ├─ d0/e  PhoneStateListener（掩码 0x100500）
        ├─ d0/f  TelephonyCallback（API≥31；SignalStrengths/CellInfo/DisplayInfo）
        ├─ d0/h  IP 采集线程（GET /public/ip）——本版被开关关闭
        ├─ d0/i  UsageStats + NetworkStats 线程（7 天窗口）
        ├─ d0/j  信号历史装载器（构造函数内工作，无 run()）
        └─ d0/k  唯一上报线程
        ▼
f0/g    HTTPS/HTTP POST（Content-Type: application/encrypted-json）
f0/c    MD5 / AES-128-ECB / Base64 封装；f0/k RSA-1024（assets/pub.cer）
f0/d    SQLite systemhelper（失败重试队列，≤100 条）
f0/l    SharedPreferences（8 类键）
f0/a    AlarmManager 封装（4 个接收器的精确定时）
f0/f    全局静态配置（28 个域，含默认 URL 与三个周期）
```

支撑库（识别结论，非本应用逻辑）：`f/g/h` = ConstraintLayout 求解器；`q` = ArrayMap/SimpleArrayMap；`r/x/y/t` = AppCompat（MenuInflater/WindowCallback/AppCompatActivity）；`l/o/p/k` = TypefaceCompat/字体资源/PrecomputedText；`s/m/i/w/v/j` = accessibility/DrawableCompat/资源工具；`b` = LinkedHashMap；`a` = TaskExecutor；`u/a..u/j` = **AppCompat 的 R 类**（attr/id/layout/string/styleable…），**不是协议常量**（此点已纠正早期猜测）。

### 3.2 端到端数据流（时序）【观测 + 推断】

```
BOOT_COMPLETED
  └─ OnBootReceiver.onReceive
       ├─ 读 pref preference_enable_log → f0/f.a
       ├─ SDK≥30 → e0/r.k(ctx)   （创建 TelephonyCallback 监听）
       └─ action==BOOT/QUICKBOOT
            ├─ f0/a.b(ctx, 240000ms)  → 4 分钟后再次进入 OnBootReceiver（延迟首启）
            ├─ f0/a.d(ctx, scheduleInterval=24h)  → ScheduleRegisterReceiver 自我续期
            └─ f0/a.a(ctx, appInfoInterval=7d)    → AppInfoRegisterReceiver 自我续期

4 分钟后的 OnBootReceiver（非 boot action 分支）
  ├─ SIM_STATE==READY 且 SDK≥30 且 f0/f.t≥0 → 递减计数；网络类型未知 → f0/a.b(ctx, 3000ms) 重试（≤3 次）
  ├─ e0/c2.i()  建立 MODEL→厂商 静态表
  ├─ reader = e0/c2.h(ctx)   厂商分支 → OEM Reader（未命中→e0/r）
  └─ reader ∈ {z0,c0,e0} ?  → startService(RegisterIntentService, RegType=F/B)   （先 bind 厂商 AIDL）
                            :  d0/g.b(ctx, "F"/"B")  → 直接采集+上报

ScheduleRegisterReceiver（每 24h） → d0/g.b(ctx,"S")
AppInfoRegisterReceiver（每 7d）    → d0/g.b(ctx,"A")
SIMStateReceiver（SIM 变化）        → d0/g.b(ctx,"T")
ConnectStateReceiver（联网变化）    → 仅写日志（不采集、不上报，见 §4.4）

d0/g.b/a/c(ctx[,type]) → e0/c2.g(ctx,type)
  ├─ "T"：比对 ICCID 与 pref iccid1/iccid2，未变 → 记 "SIM not changed，not post！" 并终止
  ├─ "A"：device_provisioned==1 ? 起 d0/i（应用统计）: 起 d0/k
  ├─ "R"：起 d0/j（装信号历史），**不上报**（本包无发送方 → 死路径）
  └─ "B"/"F"/"S"/其它：起 d0/k

d0/k.run()
  ├─ f0/f.g（默认 true=允许联网/采集）
  │    └─ 是：d0/g.d() → MD5 → AES+sign → URL → POST → 200/416 算成功 → 清空 history 表
  │    └─ 否：把 d0/g.d() 结果写入 systemhelper.history（≤100 条），日志 "User Permission Closed!"
```

### 3.3 全局静态配置 `f0/f`（28 个域，非 final、可任意改写）【观测】

| 域 | 初值 | 语义 | 是否有写入点 |
|---|---|---|---|
| `w` | `http://dm.wo.com.cn:18080/registerv3` | 上报地址默认值 | 否（pref 可覆盖） |
| `x` | `http://dm.wo.com.cn:18080/public/ip` | 公网 IP 回显服务 | 否 |
| `y` | `0x5265C00`=86,400,000 | scheduleInterval 默认 24h | 否 |
| `z` | `0x240C8400`=604,800,000 | appInfoInterval 默认 7d | 否 |
| `t` | `2` | 网络等待重试计数初值 | 是（OnBootReceiver 递减） |
| `g` | `true` | **联网/采集总开关**（d0/k 判定） | 否（仅初始化） |
| `a` | `false` | 日志开关 | 是（pref / AIDL s()） |
| `b` | `false` | IP 采集开关 | **否 → 死** |
| `c` | `false` | VoLTE 写系统开关 | **否 → 死** |
| `e` | `false` | 信号信息开关 | **否 → 死** |
| `f` | `null` | Context | 是（d0/g 工厂） |
| `h/i` | `null` | TelephonyManager（槽1/槽2） | 是 |
| `j/k` | `null` | `d0/e` PhoneStateListener ×2 | 是 |
| `l/m` | `null` | `d0/f` TelephonyCallback ×2 | 是 |
| `n..s` | `""` | NetworkTypeSlot1/2、DataSlot 缓存等 | 是 |
| `A` | `null` | nubia `b0/a` Binder 代理 | 是 |
| `B` | `null` | Gree `c0/a` Binder 代理 | 是 |
| `u/v` | `false` | 其它分支标志 | 是 |

> 说明：`b/c/e` 三个开关**全工程无 `sput` 写入点**（只有初始化 0），因此本版本中 **IP 采集、信号强度采集、VoLTE 强制开启三条链路全部不生效**——但代码与数据通路完整保留，一次静态域改写即可激活。

---

## 4. 组件与外部触发面

### 4.1 `DMApplication`【观测】
仅 `Log("DMApplication","onCreate--->")`；`SDK≥30` 时调用 `e0/r.k(this)` 注册 Telephony 监听。无其它初始化。

### 4.2 `OnBootReceiver`（13 KB，核心调度）【观测】

1. 读 `preference_enable_log` → `f0/f.a`。
2. 记录 action；`SDK≥30` → `e0/r.k(ctx)`。
3. **action ∈ {BOOT_COMPLETED, QUICKBOOT_POWERON}**：
   - 日志 `boot delayed for 4 minutes-->`；`f0/a.b(ctx, 0x3A980=240000ms)` 挂 4 分钟后再入自己；
   - `f0/a.d(ctx, f0/l.f(ctx)=scheduleInterval)`；
   - `f0/a.a(ctx, f0/l.b(ctx)=appInfoInterval)`；返回。
4. **其它 action（含 null、含自唤醒）**：
   - 若 `SIM_STATE==5(READY)`、`SDK≥30`、`f0/f.t≥0` → `f0/f.t--`；若 `f0/f.n`(NetworkTypeSlot1) 与 `f0/f.o` 皆为空 → `f0/a.b(ctx, 0xBB8=3000ms)` 再等 1 秒，返回；
   - `e0/c2.i()`；`reader=e0/c2.h(ctx)`；
   - 读 pref `firstPowerOn`：为空 → `RegType="F"`，否则 `"B"`；
   - `reader ∈ {e0/z0, e0/c0, e0/e0}` → `startService(RegisterIntentService, RegType=F|B)`；否则 → `d0/g.b(ctx, "F"|"B")`；
   - 写回 pref `firstPowerOn`。

> **风险**：该接收器 `exported=true` 且除 action 字符串外**不校验来源**；任意应用发送任意 action 即可反复触发"立即采集 + 上报"（见 §10.4）。

### 4.3 `SIMStateReceiver`【观测】
`SIM_STATE_CHANGED` → 静态方法 `a()`（重置）+ `b(Context)`：`f0/a.c(ctx, 0x57E40=360000ms=6 分钟)` 后自触发一次"等 SIM 就绪再上报"（`RegType="T"`）。即 SIM 变化后延迟 6 分钟按需重注册。

### 4.4 `ConnectStateReceiver`【观测】
- 静态注册（Manifest，`CONNECTIVITY_CHANGE`）+ 由 `OnBootReceiver` 调 `b(ctx)` 动态再注册一次（`b()` 中先 `unregister` 旧的，再用 `f0/f.v=true` 抑制"注册瞬间的粘性回调"）。
- `onReceive` 只做一件事：用 `f0/l.l(ctx)` 判断当前是否联网，打印 `NetWork connected` / `NetWork disconnected`，**不采集、不上报**。
- `a()` 为注销方法（`unregisterReceiver` + 清 `f0/f.v`）。

### 4.5 `ScheduleRegisterReceiver`【观测】
每 24h 自唤醒：重新挂 24h alarm（自我续期）+ 触发一次常规采集上报（`RegType="S"`）。

### 4.6 `AppInfoRegisterReceiver`【观测】
每 7d 自唤醒：重新挂 7d alarm + 触发一次应用清单采集（`RegType="A"` → 起 `d0/i` 统计 UsageStats/流量）。

### 4.7 `TestActivity`（exported）【观测】
```
onCreate:
  setContentView(activity_test)
  f0/f.a = pref preference_enable_log
  SDK≥30 → e0/r.k(ctx)
  检查 READ_PHONE_STATE + ACCESS_COARSE_LOCATION + ACCESS_NETWORK_STATE
    全部已授予 → B()
    否则 → requestPermissions([...], 1)
onRequestPermissionsResult(1): 三项全授予 → B()；否则 finish()
B():
  tv = findViewById(tv_device_info)
  e0/c2.i()
  r = e0/c2.h(ctx)
  r ∈ {e0/z0, e0/c0, e0/e0} ? startService(RegisterIntentService) : tv.setText(d0/g.c(ctx))
```
即：**界面上就是一个 TextView**；`d0/g.c(ctx)` 返回的是把 `d0/g` 各字段拼成的纯文本摘要（`d0/g.e()`），供"测试/自检"展示。
> 风险：任意应用可拉起本 Activity 触发三项系统权限弹窗（targetSdk 28 下不受后台启动限制）。

### 4.8 `RegisterHelperService` + `IRegisterHelperService`（**最高风险组件**）【观测】

- `exported=true`、**无 `android:permission`**、带 action 过滤器 → 任意应用可 `bindService` 拿到 Binder。
- AIDL 描述符：`com.mobiletools.systemhelper.service.IRegisterHelperService`；`onTransact` 仅做 `enforceInterface`（只校验接口名，**不做调用者鉴权/Uid 校验/签名校验**）。
- 5 个事务的真实语义（`RegisterHelperService$a.smali:34-144`）：

| 事务 | 方法 | 实际行为 |
|---|---|---|
| 1 | `r(String s)` | `f0/l.t(ctx,s)` → 写 pref **`serverAddress` = s**（**可把整条遥测改道到任意服务器**） |
| 2 | `i(int n)` | `scheduleInterval=n`；`appInfoInterval=n*8/5`；`signalInfoInterval=n*9/5`（**可把 24h 改成 1s，形成流量/电量放大**） |
| 3 | `u()` | `f0/l.a()` → 删除 `/data/data/com.mobiletools.systemhelper/shared_prefs/com_mobiletools_systemhelper_preference.xml`（配置 DoS） |
| 4 | `s(boolean z)` | `f0/f.a=z` + 写 pref `preference_enable_log`（远程开关日志） |
| 5 | `o()→boolean` | 读 pref `preference_enable_log` |

`onCreate` 中仅在 `SDK≥30` 时注册 Telephony 监听；`onUnbind` 恒返回 `true`（下次 bind 会走 `onRebind`，但未实现 → 实际行为按系统默认）。

### 4.9 `RegisterIntentService`（OEM 私有 Binder 引导器）【观测】

- 仅当 reader 属于 `e0/z0`(nubia)、`e0/c0`/`e0/e0`(Gree) 时由外部 `startService`；`onCreate` 里按需 `bindService`：
  - `com.android.phone/com.android.phone.nubia.NBExtTelephonyService`（`BIND_AUTO_CREATE`）→ 代理存 `f0/f.A`
  - `com.gree.telephonymanager/com.gree.telephonymanager.GreeTelephonyManagerService` → 代理存 `f0/f.B`
- `onHandleIntent` 用 `Thread.sleep(5)` **无超时自旋**等待代理就绪，然后 `d0/g.b(ctx, RegType)`。
- `onDestroy` **不 unbind**（连接泄漏）。
- 内部类 `$a`/`$b` 是两个 `ServiceConnection`。

### 4.10 `RegisterJobService`（**死组件**）【观测】
声明了 `BIND_JOB_SERVICE`，但全工程**无 `JobScheduler`/`JobInfo` 引用** → 永不被调度；`onStartJob` 无实质逻辑。

---

## 5. 采集能力全景（功能清单）

### 5.1 `e0` 层的构成（85 个类的角色划分）【观测】

| 角色 | 数量 | 成员 | 说明 |
|---|---|---|---|
| 基础设施 | 5 | `e0/r`（基类）、`e0/c2` + `c2$a`/`c2$b`/`c2$c` | 模板 + 编排器 + 两个回调 |
| API 兼容桥 | 20 | a..l1 侧 12：`b,d,e,f,g,h,i0,j,k,l,l0`（及同类单方法类）；m..z1 侧 8：`m,n,o,p,q,v0,x,z` | 每个只包一个 API≥29 新成员（`getDomain`/`getTransportType`/`getCellIdentity`/`CellIdentityTdscdma.getCid,getLac`/`SubscriptionInfo.getMncString`/`SubscriptionManager.getSlotIndex`/`getActiveDataSubscriptionId`/`getImei(int)`/`getCellIdentityNr` 等），是"compileSdk 33 / targetSdk 28"跨版本兼容的产物 |
| 反射工具 | 1 | `e0/d2` | `getXXX(int)`→`getXXX(long)` 回退、`getSubId` 的 `long[]`/`int[]` 双解析 |
| OEM 采集器 | 59 | 见 §5.2 与附录 D（其中 48 个出现在 `c2.h()` 的品牌分发表中，其余为无派发入口的变体） | 一套模板 + 每厂商一份拷贝，覆盖 Samsung/Xiaomi/OPPO/vivo/HUAWEI/Lenovo/ZTE/Coolpad/meizu/nubia/realme/OnePlus/TCL/Hisense/KONKA/lephone/gionee/Changhong/WIKO/PTAC/TDTECH/Liantong/K-Touch/JingLing/Shown/LETV/Bee/BestSonny/kingsun/mfox/Xiaolajiao/ChuiZi/SINTAVE/OYSIN/BROR/NOAIN/YinKe/HZTL/INTKI/PPTV/Onefive/sprd 等 |

### 5.2 厂商派发与型号白名单【观测】

- `e0/c2.i()` 懒加载静态 `Map a`：**`Build.MODEL → 厂商名`**，共 **304 条**（302 条硬编码 + 2 条 `ro.product.manufacturer` 直取），行号 1370–3391。品牌分布：Xiaomi 117、OPPO 92、vivo 20、realme 13、Hisense 7、PTAC 7、samsung 6、Lenovo/ZTE/Liantong 各 4、nubia/OnePlus/WIKO/TDTECH 各 3、meizu 2、其余 14 品牌各 1。
- `e0/c2.h(Context)`：`SDK≥30` 先 `e0/r.k(ctx)`；取 `a.get(Build.MODEL)`，再用 **60 路 `String.equals` 分支** `new` 出对应 Reader；未命中 → `e0/r` 并打 `getReader error!! manufacture==null!`。结果缓存到 `e0/c2.b`（永不复算）。
- 关键映射（`e0/c2.h()` 的 60 分支，`Build.MANUFACTURER` 字符串精确 `equals`，行号为 `e0/c2.smali`）：`samsung→e0/q1`、`Xiaomi→e0/y1`、`OPPO→e0/c1`、`BBK/vivo→e0/u1`、`HUAWEI→e0/h0`、`nubia→e0/z0`、`meizu→e0/u0`、`motorola→e0/w0`、`Lenovo→e0/s0`、`ZTE→e0/b2`、`Coolpad→e0/y`、`TCL→e0/s1`、`realme→e0/m1`、`OnePlus→e0/f1`、`Hisense→e0/g0`、`KONKA→e0/o0`、`kingsun→e0/n0`、`lephone→e0/q0`、`gionee→e0/d0`、`LETV→e0/r0`、`sprd→e0/c`、`CHANGHONG→e0/v`、`TDTECH→e0/t1`、`PTAC→e0/k1`、`WIKO→e0/v1`、`EWPE→e0/c0`、`TOSOT→e0/e0`、`Liantong/H10→e0/t0`、`YIK→e0/z1`、`xiaodu→e0/w1`、`HZTL→e0/j0`、`Shown→e0/n1`、`OYSIN→e0/e1`、`QIKU→e0/l1`、`PHICOMM→e0/g1`、`PHILIPS→e0/h1`、`PROTRULY→e0/j1`、`Haipainoble→e0/f0`、`NOAIN→e0/y0`、`SOP→e0/p1`、`Xiaolajiao→e0/x1`、`5G HYUN→e0/a1`、`INTKI→e0/k0`、`PPTV→e0/i1`、`Onefive→e0/b1`、`BROR→e0/a`；未命中 → `e0/r` 并打 `getReader error!! manufacture==null!`（完整 60 分支与行号见 `findings/device-core.md §6.3`）。
- 结论：**"是否采集"完全由 `Build.MODEL` 决定**，与权限状态、用户同意无关。改机型即可切换/触发不同采集器（甚至可让非联通机型跑联通机型的最深采集分支）。

### 5.3 采集数据项总表（数据项 → 来源 → 落点）【观测 + 推断】

| # | 数据项 | 来源（API / 属性 / 文件） | `d0/g` 字段 | 上报键 | 备注 |
|---|---|---|---|---|---|
| 1 | 厂商 | `c2.a.get(Build.MODEL)` / `Build.MANUFACTURER` | `a` | `Manuf` | |
| 2 | 机型 | `Build.MODEL` | `b` | `Model` | |
| 3 | 硬件版本 | `ro.*` 厂商属性（`ro.board.hardware_version`、`ro.build.hardware.version`、`ro.huawei.*`、`ro.vivo.*`…） | `c` | `HWVersion` | 各厂商键不同 |
| 4 | 软件版本 | `ro.build.display.id`/`ro.build.PDA`/`ro.tcl.*`/`ro.miui.cust_hardware`… | `d` | `SWVersion` | |
| 5 | OS 名 | 常量 `"Android"`（YunOS 机型改写） | `e` | `OS` | |
| 6 | OS 版本 | `Build.VERSION.RELEASE`（`ro.build.version.external`） | `f` | `OSVersion` | |
| 7 | **IMEI1** | `getImei()/getDeviceId()`（反射）、`gsm.mtk.imei1`、`ril.imei.sim1`、`gsm.rgk.imei1`、`gsm.yunos.imei0`、`persist.sys.fota_deviceid1`、MTK `ITelephonyEx.getSvlteImei(0)`、nubia/Gree AIDL、`miui.telephony.*` | `g` | `IMEI1` | 6 条独立取法 |
| 8 | **ICCID1** | `getSimSerialNumber()`、`SubscriptionInfo.getIccId()` | `h` | `ICCID1` | |
| 9 | **IMSI1** | `getSubscriberId()`、`miui.*ForSubscription` | `i` | `IMSI1` | |
| 10 | **MSISDN1（手机号）** | `getLine1Number()`、`getMsisdn()`、`SubscriptionInfo.getNumber()` | `j` | `MSISDN1` | |
| 11 | 运营商1 | `getNetworkOperator()`/`getSimOperator`（**完整 MCC+MNC**，如 `46001`） | `k` | `MNC1` | 键名"名不副实" |
| 12 | 网络类型1（NCLS） | `TelephonyManager.getNetworkType()/getVoiceNetworkType()` + 映射表 | `l` | `NCLS1` | 见 §5.4 |
| 13 | **小区 ID1** | `getCellLocation()`、`getAllCellInfo()`、`getNetworkRegistrationInfoList()`（domain=PS, transport=WWAN）、`CellIdentity{Gsm,Wcdma,Tdscdma,Cdma,Lte,Nr}`、pref `CellID0/1` | `m` | `CellID1` | NR 用 `NCI`(e0/e) + `TAC`(e0/f) |
| 14 | **LAC1/TAC1** | 同上 | `n` | `LAC1` | |
| 15 | **VoLTE 开关1** | `ImsMmTelManager.isAdvancedCallingSettingEnabled()`、`TelephonyManager.isVolteAvailable`、`Settings.Global`、ColorOS `isOemEnhanced4gLteModeSettingEnabledByUser`、vivo `API_TAG_isVolteEnable`、ZTE `DmUnicomAbsTelephonyManager.getVoLTEState` | `o` | `VolteEnabled1` | **基类会先把开关打开再读**（见 §10.2） |
| 16 | **5G 开关1** | `Settings.Global["preferred_network_mode"+subId] ∈ [23,33]`、`isFiveGCapable`、`ro.nubia.nr.support`、`persist.radio.vivo.fiveg` | `p` | `5GEnabled1` | 语义是"用户首选网络模式"，非"是否支持 5G" |
| 17–26 | 槽 2 同项 | 同上，`slot=1` 路径 | `r..A` | `IMEI2/ICCID2/IMSI2/MSISDN2/MNC2/NCLS2/CellID2/LAC2/VolteEnabled2/5GEnabled2` | |
| 27 | 数据卡槽位 | 4 种算法（`getPhoneId(defaultDataSubId)+1`、`getDefaultDataSubscriptionId()+1`、`==0?"1":"2"`、硬编码 `"1"`） | `B` | `DataSlot` | 0/1 基混用 |
| 28 | 接入类型 | `ConnectivityManager`：Mobile / WiFi / Disconnected | `C` | `AccType` | |
| 29 | IPv4 / IPv6 | 服务端 `GET /public/ip`；首个非 loopback/linklocal Inet6Address | `D/E` | `IPv4/IPv6` | **开关 `f0/f.b` 恒 false → 永不采集** |
| 30 | RAM/ROM/可用 | `/proc/meminfo`、`/proc/meminfo_memsize`、`StatFs`、`ActivityManager.MemoryInfo` | `F` | `SubType{RAM,ROM,FREE}` | 无权限即可读 |
| 31 | 设备类型 | 硬编码 `"000"`，个别厂商写 `"007001029"`(Baidu 编码)/`"000004"`/`"000005"`/机型名 | `G` | `DeviceType` | 语义不明、被污染 |
| 32 | 注册类型 | `F/B/S/A/T/R` | `H` | `RegType` | |
| 33 | 协议版本 | 常量 `"3.0"` | — | `SpecVersion` | |
| 34 | 客户端版本 | `PackageManager.getPackageInfo().versionName` | `J` | `RegVersion` | |
| 35 | 时间 | `SimpleDateFormat("yyyy-MM-dd HH:mm:ss")` | `K` | `Time` | |
| 36 | **应用使用清单** | `UsageStatsManager.queryUsageStats(now-7d, now)` + `NetworkStatsManager` | `L` | `AppInfo` | 见 §5.5 |
| 37 | **信号历史** | `f0/m`（signalhelper.signal 表读） | `N` | `SignalInfo` | 表无写入方 → 恒空/`"null"`；且开关 `f0/f.e` 恒 false → 键被省略 |
| 38 | **失败队列** | `f0/d`（systemhelper.history 表） | `M` | `History` | 上报失败的历史明文报文数组 |

> 未采集（明确排除）：`Build.SERIAL`、`Settings.Secure.ANDROID_ID`、OAID/GAID、WiFi MAC/BSSID、蓝牙地址、通讯录、短信、通话记录、相册、麦克风、摄像头、剪贴板、已安装应用的包名+版本全量（仅 UsageStats 的可见统计）、精确 GPS（本版路径被关闭）。

### 5.4 NCLS 网络类型映射（已按 `packed-switch` 展开）【观测】

| `getNetworkType()` | NCLS |
|---|---|
| 1, 2, 16 | GSM |
| 3, 8, 9, 10, 15 | WCDMA / TD-SCDMA（按 MCC：46000/46002/46007 → TD-SCDMA，其余 → WCDMA） |
| 4, 5, 6, 7, 12, 14 | CDMA（1xRTT/EvDo） |
| 13, 19 | LTE |
| 17 | TD-SCDMA |
| 20 | **NR（5G）** |
| 11, 18 | unknown |

### 5.5 `d0/i`：应用使用与流量采集【观测】

- 时间窗：`now - 7 天`（`f0/f.z` = 604,800,000 ms）→ `now`，通过 `UsageStatsManager.queryUsageStats`。
- 过滤/排序：按 `getTotalTimeInForeground()` **降序**；前台时长 `ms/60000` 取整分钟（`long→int` 截断）。
- 输出：`JSONArray` → `d0/g.L` → `AppInfo`。每项 `f0/b`：包名、前台分钟、最后使用时间、流量（`NetworkStatsManager` queryDetailsForUid：移动数据 KB=`/1024`、WiFi 总 MB=`/1048576`）。
- 依赖：`PACKAGE_USAGE_STATS`（需用户在"使用情况访问"里授权，或 privapp 白名单直接授予）+ `QUERY_ALL_PACKAGES`。

### 5.6 位置与小区【观测】

- `d0/g.f(CellLocation)` / `g(CellLocation)`：解析 GSM/CDMA 两种 `CellLocation`，写 `CellID1/LAC1`（槽1）与 `CellID2/LAC2`（槽2），并打印 `gsm location cellId1 = ...` 等日志。
- API≥29 时用 `getNetworkRegistrationInfoList()` 过滤 `domain=PS, transport=WWAN`，再取 `CellIdentityNr`（`NCI`+`TAC`）、`CellIdentityLte`、`CellIdentityWcdma` 等；`e0/p,q` 桥提供 `CellIdentityTdscdma.getCid/getLac`。
- 兜底：pref `CellID0/1`、`LAC0/1`（历史值）。
- **精确 GPS**：`d0/f$a`（`LocationManager.requestLocationUpdates`）与库代码的 `getLastKnownLocation` 路径存在，但 `d0/f.onSignalStrengthsChanged`/全链路被 `f0/f.e`（无写入点）关闭 → 本版本**不上报经纬度**。

---

## 6. 上报协议（DM register v3）

### 6.1 目标地址【观测】

```
GET  http://dm.wo.com.cn:18080/public/ip            ← 公网 IP 回显（本版未启用）
POST http://dm.wo.com.cn:18080/registerv3?ver=3.0&model=<URLEnc(Model)>&manuf=<URLEnc(Manuf)>&sign=<URLEnc(Base64(RSA(MD5)))>
```
- 默认值来自 `f0/f.w`；实际取值 `f0/l.g(ctx)` 读 pref `serverAddress`（可由 §4.8 事务 1 改写）。
- **明文 HTTP、非标端口 18080、无 TLS**（`usesCleartextTraffic=true`，`network_security_config.xml` 未被引用）。
- `dm.wo.com.cn` = 中国联通"沃"体系的设备管理域名（`wo`）；`ver=3.0` 与 `SpecVersion=3.0` 对应"register v3"规范。

### 6.2 加密链（`d0/k.smali:143-260`，已逐指令核对）【观测】

```
plaintext = d0/g.d()                                  // JSON 字符串
K         = MD5(plaintext)                            // 16 字节，同时充当 AES key
sign      = Base64( RSA/ECB/PKCS1Padding(pub.cer, K) ) // 1024-bit
body      = AES/ECB/PKCS5Padding(K, plaintext)         // 原始字节（非 Base64）
URL       = serverAddress + "?ver=3.0&model=..&manuf=..&sign=" + URLEncode(sign)
POST body=body, Content-Type: application/encrypted-json, Content-Length=len(body)
```
- `assets/pub.cer`：X.509 DER，**自签名**，`Subject=Issuer=CN=UNICOM, OU=UNICOM, O=UNICOM, L=BJ, S=BJ, C=CN`，Serial `528B9360`，有效期 **2019-05-24 → 2119-04-30**，签名算法 `sha256RSA`，**RSA 1024 位**，仅含 SKI 扩展；DER SHA-256 `810c0122ba6af7a4b6a5921fdbbbf9197fb2e0b2e82de0c5aa40d28f1b7c6501`。
- 第三参数 `f0/g.c(url, body, key)` 中的 `key` **完全未被使用**（签名只走 URL）。
- **密码学评估**：key = MD5(明文) → 任何拿到 `body` 的人都能用 MD5(body) 反解 AES，"加密"只等同于混淆；`sign` 无时间戳/随机数/序号 → **可无限重放**；RSA-1024 已可被国家级算力分解。

### 6.3 HTTP 细节【观测】

| 项 | 值 |
|---|---|
| 方法 | `POST` |
| 头 | `Content-Type: application/encrypted-json`、`Content-Length` |
| 超时 | connect 10,000 ms、read 15,000 ms |
| 成功判定 | `200` → 成功；**`416`(0x1A0) 也当成功** |
| 重试 | **无重试、无退避**（一次失败即落库，等下一个 24h 周期） |
| 日志 | `RegisterThread` TAG：`register postString：<明文>`、`server responseCode-->`、`register success!`/`register fail!` |

### 6.4 失败处理与落库【观测】

- 失败：`d0/g.M = null` 后把明文 JSON 写入 SQLite `systemhelper.history(id INTEGER PRIMARY KEY AUTOINCREMENT, jsonString TEXT)`；**表上限 100 条**（`count>=0x64` 时按"最早一行的 jsonString 值"删除，注意是按值删而非按 id，同值会多删）。
- 成功：`DELETE FROM history`（全清）；同时下次上报把历史并入正文 `History` 数组（SDK≥28 用 20 MB `CursorWindow` 读）。
- 因此**离线期间的数据会堆积并在恢复网络后一次性带出（≤100 条）**，形成"补传"能力。

### 6.5 完整报文结构（`d0/g.d()`，`d0/g.smali:335-981`）【观测】

```json
{
  "DeviceInfo": {
    "Manuf": "...", "Model": "...", "HWVersion": "...", "SWVersion": "...",
    "OS": "Android", "OSVersion": "...",
    "IMEI1": "...", "ICCID1": "...", "IMSI1": "...", "MSISDN1": "...",
    "MNC1": "46001", "NCLS1": "LTE", "CellID1": "...", "LAC1": "...",
    "VolteEnabled1": "1", "5GEnabled1": "0",
    "IMEI2": "...", "ICCID2": "...", "IMSI2": "...", "MSISDN2": "...",
    "MNC2": "...", "NCLS2": "...", "CellID2": "...", "LAC2": "...",
    "VolteEnabled2": "...", "5GEnabled2": "...",
    "DataSlot": "1", "AccType": "Mobile",
    "SubType": {"RAM":"8G","ROM":"128G","FREE":"50G"},
    "DeviceType": "000", "RegType": "S", "SpecVersion": "3.0",
    "RegVersion": "1.2-20230925-U", "Time": "2026-11-05 02:00:00",
    "IPv4": "...", "IPv6": "..."          // 仅 f0/f.b 为真时（本版恒假）
  },
  "AppInfo":  [ {…}, … ],                  // 仅 RegType=A / 有数据时
  "SignalInfo": "…",                       // 仅 f0/f.e 为真（本版恒假）
  "History":  [ "…", "…" ]                 // 仅存在失败缓存时
}
```
- 26 个 `DeviceInfo` 单值键为**恒定输出**（缺失时填 `"Unknown"` 或 `""`），另有 4 个条件键。
- 空 IMSI 会被 `e0/c2.n()` 主动清为 `null`；`d0/g` 序列化前对 `null` 做 `"Unknown"`/`""` 归一。
- **PII 处理策略**：`e0/c2.o()` 在运营商 **∉ {46001,46006,46009}**（非联通）时，对 `ICCID1/MSISDN1/IMSI1/CellID1/LAC1` 及槽 2 同组共 10 个字段做 **MD5 十六进制**；**联通卡则明文上报**。同时按 IMEI 大小做槽位归一（`g.g > g.r` 则整组交换并把 `DataSlot` 1↔2）。

---

## 7. 定时、保活与状态机

### 7.1 Alarm 表（`f0/a`，全部 `type=2`=ELAPSED_REALTIME_WAKEUP + `setExactAndAllowWhileIdle`）【观测】

| 方法 | 目标接收器 | 延时来源 | 默认值 | 用途 |
|---|---|---|---|---|
| `f0/a.b` | `OnBootReceiver` | 常量 | 240,000 ms（4 min） / 3,000 ms（3 s 重试） | 首启延迟、网络等待重试 |
| `f0/a.d` | `ScheduleRegisterReceiver` | pref `scheduleInterval` | 86,400,000 ms（24 h） | 常规注册上报，自续期 |
| `f0/a.a` | `AppInfoRegisterReceiver` | pref `appInfoInterval` | 604,800,000 ms（7 d） | 应用清单采集，自续期 |
| `f0/a.c` | `SIMStateReceiver` | 常量 | 360,000 ms（6 min） | SIM 变更后延迟重注册 |

- `PendingIntent` flags = `0x0C000000`（`FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`）。
- **无 `setRepeating`**：全部靠接收器自我续期 + 开机重新播种。`SCHEDULE_EXACT_ALARM` 权限 + `setExactAndAllowWhileIdle` 表示**刻意对抗 Doze**。
- `f0/a.e()`/`f0/a.f()` 是**恒返回 true 的桩**，导致 `if(...)` 的普通 `set()` 分支为死代码。

### 7.2 `RegType` 语义【观测 + 推断】

| 值 | 发送者 | 含义 |
|---|---|---|
| `F` | OnBootReceiver（`firstPowerOn` 为空） | 首次开机注册 |
| `B` | OnBootReceiver（已非首次） | 开机注册 |
| `S` | ScheduleRegisterReceiver | 定时注册（24h） |
| `A` | AppInfoRegisterReceiver | 应用清单注册（7d） |
| `T` | SIMStateReceiver / ConnectStateReceiver | SIM/网络变更触发（需 ICCID 变化） |
| `R` | **无发送者** | 信号历史装载路径（死） |

### 7.3 SIM 变化判定【观测】
`e0/c2.g` 的 `"T"` 分支：把当前 `ICCID1/ICCID2` 与 pref `iccid1/iccid2` 比较；相同 → 打印 `SIM not changed，not post！` 并**终止**；变化 → 记录 `SIM1/2 Changed,old iccid:…, new iccid:…` 并继续上报（同时更新 pref）。

### 7.4 边界与脏状态【推断】
- `f0/f.t`（重试计数）是**进程内静态**，重启即回 2；若网络长期不就绪，每次开机最多 3 次 × 1 s。
- `e0/c2.b`（Reader 单例）不失效；`e0/c2.o()` 的槽位交换会修改 `d0/g` 的共享实例 → 多次调用理论上可累积交换（幂等性未真机验证）。
- `firstPowerOn` 的写回分支在反编译中表现为"键值写自身"，实际写入值依赖寄存器复用，**存在写成空串的迹象**（新装机每次开机都判为 `F`）。

---

## 8. 配置、开关与死代码

### 8.1 SharedPreferences（文件 `com_mobiletools_systemhelper_preference`，`MODE_MULTI_PROCESS`=4，已废弃）【观测】

| 键 | 类型 | 默认 | 语义 |
|---|---|---|---|
| `serverAddress` | String | `http://dm.wo.com.cn:18080/registerv3` | 上报地址（可被 AIDL 改写） |
| `scheduleInterval` | long | 86,400,000 | 常规周期 |
| `appInfoInterval` | long | 604,800,000 | 应用清单周期 |
| `signalInfoInterval` | long | （读时无默认） | 信号周期（仅 AIDL 写，**代码内无读取点**） |
| `preference_enable_log` | bool | false | 日志开关 |
| `firstPowerOn` | String | `""` | 首启标记 |
| `iccid1` / `iccid2` | String | — | SIM 变化比对 |
| `CellID0` / `CellID1` / `LAC0` / `LAC1` | String | — | 小区兜底缓存 |
| `dm_user_agreement` / `operator_service_improve_plan` / `operator_registration_switch` | （Secure Settings，非 pref） | — | 部分 Reader 的"用户同意"门禁 |

- 文件路径被硬编码在 `f0/l.a()`：`/data/data/com.mobiletools.systemhelper/shared_prefs/com_mobiletools_systemhelper_preference.xml`（用于删除）。

### 8.2 门禁（"用户是否同意"）实现不一致【观测】

| 实现 | 出现位置 |
|---|---|
| `Settings.Global.device_provisioned == 1` | `a0/b2/c1/d0/e0/f1/j0/k1` 等 |
| `Settings.Secure["dm_user_agreement"] == 1` | `c/b2` |
| `Settings.Secure["operator_service_improve_plan"] == 1` | `c0/g0` |
| `device_provisioned==1 && Settings.Secure["operator_registration_switch"]==1` | `d0` |
| **`return true` 硬编码** | `e0/a1`（5G HYUN） |
| 无任何门禁 | 其余多数 Reader |
| 唯一真拒绝分支 | `e0/u1`（vivo）：`isUserPermissionOpen==false` → `do not read!` |

同时所有 Reader 覆写的 `a(Context)`（"本 Reader 是否适用"）都是 `new ServiceState().getRoaming()`（恒 false），且 `e0/c2.k()` 全 APK 无调用者 → **"是否采集"实际不经过任何门禁**。

### 8.3 死代码 / 未启用能力清单【观测】

| 项 | 状态 | 影响 |
|---|---|---|
| `f0/f.b`（IP 开关） | 无写入点 | `d0/h` 永不启动，`IPv4/IPv6` 键永不出现 |
| `f0/f.e`（信号开关） | 无写入点 | `d0/f.onSignalStrengthsChanged` 首行 return；`SignalInfo` 键永不出现 |
| `f0/f.c`（VoLTE 写开关） | 无写入点（8 个文件 16 处读取） | 恒走只读 `e0/r.d()`；"强制打开 VoLTE"的写路径不执行 |
| `RegType="R"` | 无发送者 | `d0/j` 与 signal 表链路为死路径 |
| `signalhelper.signal` 表 | 无 insert 方法 | `SignalInfo` 恒空 |
| `RegisterJobService` | 无 JobScheduler 引用 | 从不调度 |
| `com.mobiletools.systemhelper.permission` | 零引用 | 死声明 |
| `network_security_config.xml` | Manifest 未引用 | 死资源（真正生效的是 `usesCleartextTraffic`） |
| `setting_advice_*` 字符串 | 零引用 | **隐私告知不可达** |
| `d0/g.q`、`d0/g.O`、`d0/g.P/Q` | 无写入/不序列化 | 死字段 |
| `e0/c2.j()/k()` | 无调用者 | 死方法 |

---

## 9. 厂商 / 运营商私有通道

### 9.1 两套厂商 AIDL（`b0/a`、`c0/a`）【观测】

| 接口 | 描述符 | 绑定组件 | 事务数 | 结果落点 | 消费方 |
|---|---|---|---|---|---|
| `b0/a` | `com.android.phone.nubia.INBExtTelephony` | `com.android.phone/com.android.phone.nubia.NBExtTelephonyService` | 10（1=`v()` … 10=`d()`） | `f0/f.A` | `e0/z0`（nubia） |
| `c0/a` | `com.gree.telephonymanager.IGreeTelephonyManager` | `com.gree.telephonymanager/com.gree.telephonymanager.GreeTelephonyManagerService` | 12（1=`m(I,S,S)` … 12=`j()`） | `f0/f.B` | `e0/c0`、`e0/e0`（Gree/EWPE/TOSOT） |

- 仅当 Reader 为 `z0/c0/e0` 时 `RegisterIntentService.onCreate` 才 `bindService(BIND_AUTO_CREATE)`。
- `onHandleIntent` 用 `Thread.sleep(5)` 无超时自旋等待 Binder；Proxy 的 `transact()` 返回 false 时**回落到静态本地桩**（`b0/a$a`、`c0/a$a` 内的 `b` 字段）。
- `onDestroy` 不 `unbindService`。
- 事务真实语义仅能由消费方用法推断（**【推断】**，非 stub 注释）。

### 9.2 厂商私有 API / 隐藏通道【观测】

| 通道 | 类 | 用途 |
|---|---|---|
| MTK Binder | `com.mediatek.internal.telephony.ITelephonyEx$Stub` + `ServiceManager.getService("phoneEx")` | `getSvlteImei(slot)` 取 IMEI（**完全绕过 TelephonyManager**） |
| MTK | `com.mediatek.telephony.TelephonyManagerEx` | `getCellLocationFromSlotId`、`getSimOperator` |
| vivo | `ITelephony#getITelephony` + `com.android.internal.telephony.VivoTelephonyApiParams.getParamValues()` | `API_TAG_getNrCapability`、`API_TAG_getNRSwitchState`、`API_TAG_isVolteEnable` |
| 华为/荣耀 | `HwTelephonyManager.getServiceAbility`、`isVolteAvailable`、`TelephonyManagerEx.getDefault4GSlotId` | VoLTE/4G 槽位 |
| 小米 | `miui.telephony.TelephonyManager(Ex)`：`getSubscriberIdForSubscription`、`getSimSerialNumberForSubscription`、`getNetworkTypeForSubscription`、`getCellLocationForSlot`、`isFiveGCapable` | 双卡全量 + 5G |
| OPPO/realme/TCL/Coolpad/YinKe | `ColorOSTelephonyManager`、`isOemEnhanced4gLteModeSettingEnabledByUser` | VoLTE 状态 |
| 三星 | `TelephonyManager.getTelephonyProperty("ril.hw_ver"/"ro.build.PDA")`、`createForSubscriptionId`、`Settings$System.getInt("voicecall_type")` | 5G/VoLTE 专精 |
| 魅族 | `MzTelephonyManager` | 双卡 |
| ZTE | `com.android.vts.VendorTelephonyManager` + 联通定制 `DmUnicomAbsTelephonyManager.getVoLTEState` | VoLTE |
| 通用反射 | `f0/o`（`Class.forName`+`getMethod`+`setAccessible(true)`+`invoke`）、`e0/d2`（`getXXX(int)`→`getXXX(long)` 回退、`getSubId` 的 `long[]`/`int[]` 双解析） | 全部隐藏 API 调用的底座 |

### 9.3 关键系统属性表【观测】

`ro.product.manufacturer`、`ro.build.version.external`、`ro.yunos.name/version`、`ro.build.display.hardware.id`、`gsm.mtk.imei1/2`、`gsm.rgk.imei1/2`、`gsm.yunos.imei0/1`、`ril.imei.sim1/2`、`persist.sys.fota_deviceid1/2`、`persist.sys.card.imsi`、`persist.sys.cucc.dm`、`ro.boot.dram_info`、`ro.board.hardware_version`、`ro.build.rom.pcb.silkscreen`、`ro.internal.version`、`ro.sf.lcd_density`、`ro.zui.software.display`、`ro.miui.cust_hardware`、`ro.tcl.hardware.version`、`ro.mtk.build.version`、`ro.vivo.op.entry`、`ro.vivo.*`/`ro.huawei.*`（厂商版本串）、`persist.radio.vivo.fiveg`、`persist.vendor.radio.5g_mode_pref`、`ro.nubia.nr.support`、`ro.boot.5GEnable1/2`、`ro.denqin.*`（长虹）。

---

## 10. 隐私与安全评估

### 10.1 数据敏感度分级

| 级别 | 数据 | 授权要求 |
|---|---|---|
| **极高** | IMEI1/2、IMSI1/2、ICCID1/2、MSISDN1/2（双卡四要素） | READ_PHONE_STATE / READ_PRIVILEGED_PHONE_STATE（多数还需隐藏 API 或厂商 AIDL） |
| **高** | CellID/LAC（可反推基站位置与轨迹）、VoLTE/5G 开关（可指纹化用户设置）、应用使用时长（7 天）、每应用流量 | ACCESS_COARSE/FINE_LOCATION、PACKAGE_USAGE_STATS |
| **中** | 机型/厂商/版本串、RAM/ROM/可用空间、DataSlot、AccType、注册时间 | 无（零权限可读） |
| **低** | 协议版本、客户端版本号、DeviceType 编码 | 无 |

### 10.2 传输与密码学缺陷【观测】

1. **明文 HTTP**（非 TLS）、非标端口 18080 → 同网段可嗅探、运营商中间设备可篡改。
2. **AES 密钥 = 明文 MD5**：AES/ECB 且密钥可由密文自身推出 → 加密只是混淆，任何截获者可解出全部 PII。
3. **`sign` 无时间戳/序号/nonce**：可无限重放；`416` 也被视为成功，进一步弱化一致性。
4. **RSA-1024 + 自签名 100 年证书**：强度不足；证书仅用于"给 MD5 签名"，不用于任何身份认证。
5. **VoLTE 写入副作用（代码存在、本版未启用）**：基类 `e0/r.c(ctx,slot)`（`e0/r.smali:560-688`）在 `ImsMmTelManager.isAdvancedCallingSettingEnabled()` 为 false 时，反射调用 **`setAdvancedCallingSettingEnabled(true)` 把用户 VoLTE 开关强行打开**，然后再读回写入 `VolteEnabled1/2`（需 `MODIFY_PHONE_STATE`）。该路径由 `f0/f.c` 门控：`f0/f.c==true` **且** 运营商 ∈ {46001,46006,46009} 才走 `c()`，否则走只读的 `d()`。`f0/f.c` 全工程**无写入点（恒 false）**，故本版本实际只走只读路径；但 `e0/b2,j0,k1,t0,t1,v1,w0` 与基类 `l()` 共 8 处、16 个调用点均已就绪，**一次静态域改写即可激活**。
6. **门禁形同虚设**：5 种不一致的"同意"判定 + 一处 `return true` 硬编码 + 所有 `a()` 恒 false。

### 10.3 本地存储【观测】

- `SharedPreferences` 使用已废弃的 `MODE_MULTI_PROCESS`（无同步保障，可跨进程一致性问题）。
- `systemhelper.history` 明文保存**完整上报报文**（含 IMEI/IMSI/手机号），最多 100 条；`allowBackup=false` 但 root/privapp 可直读。
- 无任何加密存储、无完整性校验。

### 10.4 导出组件滥用面【观测 + 推断】

| 组件 | 攻击面 | 后果 |
|---|---|---|
| **`RegisterHelperService`（最严重）** | 无权限、无鉴权；`r(String)` 改 `serverAddress`；`i(int)` 把 24h 改 1s；`u()` 删配置 | ① 遥测/隐私数据**改道至攻击者服务器**（受害者设备持续外发 IMEI/IMSI/位置）② 上报频率放大 → 流量/电量/服务端压力 ③ 配置 DoS |
| `OnBootReceiver` | exported，仅按 action 分支 | 任意应用可高频触发"采集+上报"，放大流量 |
| `SIMStateReceiver` | exported，按 action 分支 | 同 OnBootReceiver（并会挂 6 分钟延迟任务） |
| `TestActivity` | exported | 可拉起三项系统权限弹窗（社工诱导授权），targetSdk 28 无后台启动限制 |

> 说明：`ConnectStateReceiver` 虽然 exported，但 `onReceive` 只写日志，不构成放大面。

> 复现路径（仅供本机/授权验证）：绑定 `com.mobiletools.systemhelper/.service.RegisterHelperService`，`asInterface` 后 `transact(1, ...)` 写入任意 `serverAddress`，再发 `BOOT_COMPLETED` 触发一次上报即可把全量报文引向自有服务器。

### 10.5 权限最小化问题

- `READ_EXTERNAL_STORAGE` 未发现实际使用；`QUERY_ALL_PACKAGES` 仅用于 UsageStats 的包名解析（可用可见性声明替代）；`ACCESS_BACKGROUND_LOCATION` 在本版无实际定位上报路径。属于**超范围申请**。

### 10.6 告知与同意

- `setting_advice_title/content`（"运营商可能通过 WLAN 或移动数据网络收集您的设备信息等数据，开启即视为同意联网、访问应用列表和定位。"）**在代码与资源中零引用** → 用户永远看不到该告知。
- 没有任何"退出/关闭采集"的 UI；唯一开关（`f0/f.g`）在内存中恒为 true，且无 UI/AIDL 可关。
- 应用名为英文 `SystemHelper`、图标为系统 Android 机器人 → 具备**系统组件伪装**特征（预装场景下用户难以辨识为运营商采集组件）。

---

## 11. 归属与用途推断（严格区分证据与推断）

**证据支持**：
- 目标域名 `dm.wo.com.cn`（联通"沃"体系 DM 平台）、端口 18080、路径 `registerv3`；
- 内置自签名证书主体/颁发者均为 `UNICOM`（北京）；
- 仅对 PLMN `46001/46006/46009`（中国联通）明文上报，其它运营商做 MD5；
- 版本号后缀 `-U`；privapp 白名单（预装形态）；`SpecVersion=3.0`/`RegVersion=1.0` 的私有注册协议；
- 大量联通定制机型分支（`Liantong`、`H10`、`DmUnicomAbsTelephonyManager`、`persist.sys.cucc.dm`）。

**推断（非证据）**：该组件功能定位为"联通终端信息注册/网络质量与 5G/VoLTE 能力普查"——用于运营商侧统计在网终端的机型分布、双卡能力、5G/VoLTE 开通状态、驻留小区与应用使用画像；预装于定制机 `/system/priv-app`，随开机常驻。
**未能证实**：签名者与联通官方的证书链关系（自签名，无法验证）；是否为存量商用固件版本；是否存在服务端下发配置（本版 `serverAddress` 只能由本地 AIDL 改写，未见远程下发代码）。

---

## 12. 未确定项与验证建议

| # | 未知项 | 建议验证 |
|---|---|---|
| 1 | `d0/g.P/Q`（int）、`d0/g.O`（私有 String）语义 | 动态 hook `d0/g` 构造/序列化，或找同系列更高版本 APK 对比 |
| 2 | `signalhelper.signal` 表无写入方 → `SignalInfo` 实际内容 | 真机运行后 dump 数据库与上报正文 |
| 3 | `f0/f.b/c/e` 是"服务端开关被裁剪"还是历史遗留 | 对比 1.1/1.0 版本 APK；或抓取服务端返回体（当前代码未解析响应体） |
| 4 | 两套厂商 AIDL 各事务的真实语义 | 反编译 nubia/Gree 系统 APK，或动态 trace Binder |
| 5 | `e0/q1` 的 `final b/c` 常量、各厂商 VoLTE/5G 折算阈值 | 逐值 dump + 真机对照 |
| 6 | `firstPowerOn` 写回是否为空串（影响每次开机都判 `F`） | 真机观察 prefs 值 |
| 7 | `e0/c2.o()` 槽位交换的幂等性 | 连续两次采集后比对 `IMEI1/IMEI2/DataSlot` |
| 8 | 签名者是否联通官方 | `apksigner verify --print-certs` + 与联通官方应用证书比对 |
| 9 | `res/xml` 白名单是否被某 ROM 实际采用 | 抓取目标机型 `/system/etc/permissions/` 目录 |

---

## 附录 A：类索引（自有代码）

| 包 | 类数 | 内容 |
|---|---|---|
| `com.mobiletools.systemhelper` | 15 | Activity 1、Receiver 5、Service 5（含内部类）、Application 1、R 1、AIDL 2 |
| `e0` | 85 | 基类 1（`r`）、编排器 1（`c2`+3 内部类）、8 桥、33 OEM Reader、其余为 OEM 变体/内部类 |
| `d0` | 15 | `g`(DTO) `e`/`f`(监听) `h`(IP) `i`(UsageStats) `j`(信号历史) `k`(上报) `a,b,c,d`(API 桥) |
| `f0` | 15 | `a`(alarm) `b`(JSON bean) `c`(crypto) `d`/`m`(DB) `e/f`(常量) `g`(HTTP) `h`(MD5) `i`(内存) `j`(log) `k`(RSA) `l`(prefs) `n`(bean) `o`(反射) |
| `a,a0,b,c,d,e,i,j,k,m,n,s,t,u,v,w,x,y,z` | 30+ | 线程池/资源/AppCompat/ConstraintLayout 等库与工具 |

## 附录 B：REA 证据清单（摘要）

见 §1.3 表格（8 条核心证据），完整证据链可通过 `get_evidence_bundle` 导出。四份分项报告：

- `findings\app-shell.md`（42.7 KB）— 组件/工具层/资源
- `findings\collectors-1.md`（63.4 KB）— e0/a…l1 共 42 类
- `findings\collectors-2.md`（82.6 KB）— e0/m…z1 共 41 类
- `findings\device-core.md`（52.4 KB）— d0 层 + e0/r + e0/c2 + AIDL
- `findings\c2_map_a_full.tsv` — 304 条 `MODEL→品牌` 全量映射

## 附录 C：关键常量表

| 常量 | 值 | 位置 |
|---|---|---|
| 上报地址 | `http://dm.wo.com.cn:18080/registerv3` | `f0/f.w` |
| IP 服务 | `http://dm.wo.com.cn:18080/public/ip` | `f0/f.x` |
| 常规周期 | 86,400,000 ms（24h） | `f0/f.y` |
| 应用清单周期 | 604,800,000 ms（7d） | `f0/f.z` |
| 首次延迟 | 240,000 ms（4min） | `OnBootReceiver:112` |
| 网络重试 | 3,000 ms（3s）×3 | `OnBootReceiver:280`、`f0/f.t=2` |
| SIM 延迟 | 360,000 ms（6min） | `SIMStateReceiver:211` |
| 成功码 | 200（+416） | `f0/g`、`d0/k:273` |
| history 上限 | 100 条 | `f0/d` |
| 监听掩码 | `0x100500` | `d0/e`、`d0/f` |
| 协议版本 | `SpecVersion=3.0`、`RegVersion=1.0` | `d0/g` |
| 联通 PLMN | 46001 / 46006 / 46009 | `f0/l.n/m` |
| 证书 | RSA-1024，自签名 CN=UNICOM，2019→2119 | `assets/pub.cer` |

---

## 附录 D：`e0` 85 个类全功能索引（`e0/` 包完整清单）

> 标注：**桥**=API 兼容桥（本身不产出数据）；**基类/编排**=基础设施；其余为厂商采集器。隐私等级：**极高**=直接产出 IMEI/IMSI/ICCID/手机号/小区，**高**=应用清单/IP，**低**=基础设施。

| 类 | 角色 / 品牌 | 关键私有通道 | 隐私 |
|---|---|---|---|
| `a` | BROR / 移动定制 CM（YunOS） | `gsm.yunos.imei0/1`、`ro.yunos.*` | 极高 |
| `a0` | DOUQI（含 RAM/ROM/剩余空间） | `StatFs(/system,/data)`、`ro.boot.dram_info` | 极高 |
| `a1` | 5G HYUN / OFeng（5GP50） | MTK `TelephonyManagerEx`+`phone_ex`；**门禁硬编码 `return true`** | 极高 |
| `a2` | YuLong / Coolpad | `ro.yulong.version.hardware`、`ro.config.le_hardware_version` | 极高 |
| `b` | **桥** `TelephonyManager.getImei()` | — | 低 |
| `b0` | EAMEY | `ril.imei.sim1/2` | 极高 |
| `b1` | Onefive | `/proc/meminfo`、`ro.bd.cmcc_*` | 极高 |
| `b2` | ZTE（含联通 DM 定制） | `com.android.vts.VendorTelephonyManager`、`DmUnicomAbsTelephonyManager.getVoLTEState`、`dm_user_agreement` | 极高 |
| `c` | Baidu / 展讯（XDM-01） | `dm_user_agreement`、`DeviceType=007001029` | 极高 |
| `c0` | EWPE / Gree | **厂商 AIDL `IGreeTelephonyManager`**（`operator_service_improve_plan` 门禁） | 极高 |
| `c1` | OPPO / ColorOS（110+ 机型） | `ColorOSTelephonyManager`、`isOemEnhanced4gLteModeSettingEnabledByUser`、`ro.boot.dram_info` | 极高 |
| `c2` | **编排器** DeviceManager（RegType 分发/厂商选择/上报触发） | — | 低 |
| `c2$a` | 回调：UsageStats 完成后起 IP/上报线程 | — | 高 |
| `c2$b` | 回调：`d0/h` 完成后写 IPv4/IPv6 | — | 高 |
| `c2$c` | 接口：Reader 契约 `a(ctx)Z`/`b(ctx,g)V` | — | 低 |
| `d` | **桥** `unregisterTelephonyCallback` | — | 低 |
| `d0` | Gionee | `device_provisioned`+`operator_registration_switch`、`ro.ssui.*` | 极高 |
| `d1` | ORANGE（RGK 代工） | `gsm.rgk.imei1/2`、`ro.qc.*` | 极高 |
| `d2` | **反射工具**：`getXXX(int)`→`getXXX(long)` 自动回退、`getSubId` 双类型解析 | — | 低 |
| `e` | **桥** `CellIdentityNr.getNci()` | — | 低 |
| `e0` | TOSOT（大松/格力） | 同 `c0`（Gree AIDL） | 极高 |
| `e1` | OYSIN | `ro.xh.display.version` 等 | 极高 |
| `f` | **桥** `CellIdentityNr.getTac()` | — | 低 |
| `f0` | Haipainoble | **MTK `ITelephonyEx.getSvlteImei`** | 极高 |
| `f1` | OnePlus（LE21xx） | `ro.boot.dram_info`、`SubscriptionManager` | 极高 |
| `g` | **桥** `ImsManager.getImsMmTelManager(int)` | — | 低 |
| `g0` | Hisense | `ro.hmct.volte.enabled`、`persist.sys.volte.enable`；`DeviceType` 被写机型名 | 极高 |
| `g1` | PHICOMM | **反射 `android.os.Build` 的厂商注入静态字段**（OS_NAME/HARDWARE_VERSION_DISPLAY…） | 极高 |
| `h` | **桥** `ImsMmTelManager.isAdvancedCallingSettingEnabled()` | — | 低 |
| `h0` | HUAWEI | `HwTelephonyManager.getServiceAbility`、`TelephonyManagerEx.getDefault4GSlotId` | 极高 |
| `h1` | PHILIPS | `ro.custom.hw.version`；`isValidSubscriptionId` setAccessible | 极高 |
| `i` | **桥** `getImei(int slot)` | — | 低 |
| `i0` | **桥** `SubscriptionManager.getSubscriptionIds(int)` | — | 低 |
| `i1` | PPTV | MTK `getSvlteImei`；`ro.pptv.version` | 极高 |
| `j` | **桥** `Context.getMainExecutor()` | — | 低 |
| `j0` | HZTL（TAI-M01） | `HwTelephonyManager`+`MSimTelephonyManager` | 极高 |
| `j1` | PROTRULY | `ro.hardware_version` + `d2` 反射 | 极高 |
| `k` | **桥** `registerTelephonyCallback(Executor,Callback)` | — | 低 |
| `k0` | INTKI | 反射 `getCustomHWBuildVer/getCustomSWBuildVer` | 极高 |
| `k1` | PTAC（华为智选 BD00 系列） | 同 `j0` | 极高 |
| `l` | **桥** `ServiceState.getNetworkRegistrationInfoList()` | — | 低 |
| `l0` | **桥** `CellInfoNr.getCellIdentity()` | — | 低 |
| `l1` | QIKU | `ro.yulong.version.hardware` | 极高 |
| `m` | **桥** `NetworkRegistrationInfo.getDomain()` | — | 低 |
| `m0` | JingLing（JingPad） | `TelephonyManagerEx`、`getCellLocationForPhone` | 极高 |
| `m1` | realme | `ColorOSTelephonyManager`、`ro.boot.dram_info` | 极高 |
| `n` | **桥** `getTransportType()` | — | 低 |
| `n0` | kingsun（**三套读法**） | `MSimTelephonyManager.getPhoneCid/getPhoneLac` | 极高 |
| `n1` | Shown（360OS） | `gsm.mtk.imei1/2`、`ro.system.OSVersion` | 极高 |
| `o` | **桥** `getCellIdentity()` | — | 低 |
| `o0` | KONKA | `getNetworkOperatorForPhone`、`ro.boot.hardware.revision` | 极高 |
| `o1` | OYSIN / SINTAVE | `isValidSubscriptionId`、`getDeviceId` | 极高 |
| `p` | **桥** `CellIdentityTdscdma.getCid()` | — | 低 |
| `p0` | K-Touch | `PhoneFactory.getPhone(i)` | 极高 |
| `p1` | SOP | `/proc/meminfo`、`StatFs(root)` | 极高 |
| `q` | **桥** `CellIdentityTdscdma.getLac()` | — | 低 |
| `q0` | lephone（**五套读法**） | `getAllCellInfo` 列表解析、`getMsisdn` | 极高 |
| `q1` | Samsung（5G/VoLTE 专精） | `getTelephonyProperty`、`Settings$System["voicecall_type"]`、`ro.telephony.default_network` | 极高 |
| `r` | **基类** DeviceInfoReaderBase（模板 + 静态监听 + 双卡读写） | ImsMmTelManager / CellIdentity* / SubscriptionManager | 极高 |
| `r0` | LETV（五套读法） | `com.mediatek.telephony.TelephonyManagerEx` | 极高 |
| `r1` | Spreadtrum | `phone_ex` + `getMsisdn` | 极高 |
| `s` | Bee | `getAllCellInfo`、`ro.sf.lcd_density` | 极高 |
| `s0` | Lenovo（平板：仅单卡 16 字段） | `Settings$Global`、`getDefaultDataSubscriptionId` | 极高 |
| `s1` | TCL（与 `y`/`z1` 95%+ 同源） | `ColorOSTelephonyManager`、`ro.tcl.hardware.version` | 极高 |
| `t` | BenWee | `MSimTelephonyManager.getDefault` 全字段 | 极高 |
| `t0` | Liantong / H10（联通定制） | `Settings$Global["is5GEnable"/"preferred_network_mode"]`、ColorOS | 极高 |
| `t1` | TDTECH（鼎桥） | `HwTelephonyManager.getServiceAbility`、`ro.boot.dram_info` | 极高 |
| `u` | BestSonny | `getDeviceId`/`getMsisdn`/`getNetworkOperatorForPhone` | 极高 |
| `u0` | meizu | `MzTelephonyManager`、`getSimOperatorNumericForPhone` | 极高 |
| `u1` | vivo（**最深**） | `ITelephony`+`VivoTelephonyApiParams.getParamValues`（`API_TAG_getNrCapability/getNRSwitchState/isVolteEnable`）、`persist.radio.vivo.fiveg`、`ro.vivo.op.entry` | 极高 |
| `v` | CHANGHONG（**只采机型，不碰 SIM**） | `ro.denqin.*`、`ro.yunos.*` | 低 |
| `v0` | **桥** `SubscriptionInfo.getMncString()` | — | 低 |
| `v1` | WIKO / PTAC | 同 `t1` + 公开 API 回退 + 变长反射 | 极高 |
| `w` | ChuiZi（锤子/坚果） | `ro.smartisan.version` | 极高 |
| `w0` | motorola / ZUI | `getActiveSubscriptionInfoForSimSlotIndex`、`ro.zui.software.display` | 极高 |
| `w1` | xiaodu（小度） | `StatFs` 三分区容量、`ro.boot.5GEnable1/2`、`persist.sys.cucc.dm` | 极高 |
| `x` | **桥** `SubscriptionManager.getSlotIndex(subId)`（26 处调用） | — | 低 |
| `x0` | mfox | `gsm.mtk.imei1/2`、`e0.d2` 反射 | 极高 |
| `x1` | 小辣椒（**型号分支最密**） | 20+ `Build.MODEL` 分支、`ro.product.yuxin*` | 极高 |
| `y` | Coolpad（与 `s1`/`z1` 同源，日志残留 `DeviceInfoReaderOPPO`） | `ColorOSTelephonyManager`、`ro.board.hardware_version` | 极高 |
| `y0` | BROR / NOAIN | **`ITelephonyEx$Stub` + `ServiceManager.getService("phoneEx")` + `getSvlteImei`** | 极高 |
| `y1` | Xiaomi（**最大：200+ 型号白名单**） | `miui.telephony.TelephonyManager(Ex)`、`isFiveGCapable`、`/proc/meminfo` | 极高 |
| `z` | **桥** `SubscriptionManager.getActiveDataSubscriptionId()` | — | 低 |
| `z0` | nubia | **厂商 AIDL `INBExtTelephony`**、`ro.nubia.nr.support`、`ro.build.rom.pcb.silkscreen` | 极高 |
| `z1` | YinKe（音刻） | 同 `s1`/`y` | 极高 |

> 计数核对：`e0` 共 85 个文件 = 上表 83 个 + `e0/r`（基类）+ `e0/c2`（编排器）。其中 20 个为 API 桥、60 个为 OEM 采集器/变体、1 基类、1 编排器 + 3 内部类。
> 全量逐类细节（每类的 API 调用、键名来源、门禁、降级分支、smali 行号）见 `findings/collectors-1.md`（a–l1，42 类）与 `findings/collectors-2.md`（m–z1，41 类）。
