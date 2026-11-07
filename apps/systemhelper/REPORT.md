# SystemHelper Privacy Review

This report traces the implementation through static analysis. The [original report](SOURCE_REPORT.md) and [archived analyses](archival) document the earlier investigation. Use the findings below for the reviewed conclusions.

## 1. Findings

**The sample implements collection and reporting of device and SIM identifiers, cellular cell information, foreground app usage, and app network traffic. The default endpoint is `http://dm.wo.com.cn:18080/registerv3`.** A single JSON payload can combine identifiers that are difficult to reset, cellular location clues, and an app usage profile.

Field reads depend on initial device setup, system permissions, AppOps, SIM state, and vendor API conditions.

## 2. Sample identity

| Item | Value |
|---|---|
| Package | `com.mobiletools.systemhelper` |
| versionName / versionCode | `1.2-20230925-U` / `3` |
| APK SHA-256 | `efa1af0d0be75985701fd0eca79a90f0be0bb9fd2735245915200ec1a3e9657f` |
| APK size | 911,821 bytes |
| minSdk / targetSdk / compileSdk | 24 / 28 / 33 |
| Analysis | Review of existing apktool smali and manifest output. The original reports also cite REA static evidence. |

The hash identifies the sample. The publication includes selected [smali and manifest files](evidence/README.md) for direct inspection.

## 3. Data categories

| Category | Data read or assembled by the code | Conditions and interpretation |
|---|---|---|
| Device identity | IMEIs for both slots, manufacturer, model, hardware, system, and firmware versions, and Android version | Reads vary by vendor branch and permissions. |
| SIM identity | IMSI, ICCID, MSISDN, and carrier code for both SIMs | MSISDN is the phone number field. Availability depends on the SIM and system API results. |
| Cellular network and location clues | Cell ID, LAC/TAC, and cellular radio technology | Cell identifiers can provide location clues. |
| Network settings | VoLTE and 5G switches, default data SIM slot, and Wi-Fi or mobile network type | Describes network configuration and connectivity. |
| Hardware profile | RAM, storage capacity, and free space | Describes the device configuration. |
| App usage | Package names, app names, and foreground duration from UsageStats, with a default seven-day window | Package enumeration fills in labels and UIDs. Serialization is based on UsageStats records. |
| App traffic | Mobile bytes sent and received and total Wi-Fi traffic, associated by app UID | Per-app network usage statistics. |
| Reporting metadata and cache | Timestamp `Time`, trigger type, protocol and component versions, and JSON from failed reports | Supports retries and caches up to 100 failed entries. |

See [d0/g.smali:335](evidence/smali/d0/g.smali#L335) for field serialization. For the first SIM, see the IMEI, ICCID, IMSI, and MSISDN keys at lines [444](evidence/smali/d0/g.smali#L444), [459](evidence/smali/d0/g.smali#L459), [474](evidence/smali/d0/g.smali#L474), and [489](evidence/smali/d0/g.smali#L489), respectively. The second SIM follows in the same method. Cell reads appear at lines [1727](evidence/smali/d0/g.smali#L1727) and [1973](evidence/smali/d0/g.smali#L1973).

For app records, see the [queryAndAggregateUsageStats call](evidence/smali/d0/i.smali#L508), [app enumeration and field completion](evidence/smali/d0/i.smali#L568), and [query window](evidence/smali/d0/i.smali#L637). See [d0/i.smali, line 132](evidence/smali/d0/i.smali#L132) for foreground duration, [f0/b.smali:93](evidence/smali/f0/b.smali#L93) for record serialization, and [d0/g.smali:925](evidence/smali/d0/g.smali#L925) for JSON fields `Time` and `AppInfo`.

The base class reads phone and SIM information at [e0/r.smali:1280](evidence/smali/e0/r.smali#L1280). The package contains adapters for multiple manufacturers. The publication includes the [Xiaomi adapter e0/y1](evidence/smali/e0/y1.smali).

## 4. Upload path, triggers, and gates

```text
Boot / regular timer / SIM state change / app statistics timer
  → Vendor reader collects device, SIM, and cell information
  → App statistics branch queries UsageStats and NetworkStats
  → d0/g.d() assembles JSON, optionally including failed reports
  → d0/k computes MD5, wraps key material with RSA, and encrypts JSON with AES
  → Read the destination from serverAddress
  → HTTP POST application/encrypted-json
  → Default destination: dm.wo.com.cn:18080/registerv3
```

[f0/f.smali:55](evidence/smali/f0/f.smali#L55) defines the default endpoint and intervals of 24 hours for regular reporting and seven days for app statistics. The boot path has a delay of about four minutes, and SIM changes have a delay of about six minutes. See the [boot receiver](evidence/smali/com/mobiletools/systemhelper/control/OnBootReceiver.smali), [regular timer](evidence/smali/com/mobiletools/systemhelper/control/ScheduleRegisterReceiver.smali), [app statistics timer](evidence/smali/com/mobiletools/systemhelper/control/AppInfoRegisterReceiver.smali), and [SIM receiver](evidence/smali/com/mobiletools/systemhelper/control/SIMStateReceiver.smali).

Local configuration can override `serverAddress`. See [f0/l.smali:146](evidence/smali/f0/l.smali#L146) for the configuration read and [d0/k.smali:212](evidence/smali/d0/k.smali#L212) for its use during transmission.

The base check named `checkUserPermission` checks `device_provisioned` and participates in updating `f0/f.g`. See [e0/c2.smali:95](evidence/smali/e0/c2.smali#L95).

## 5. Encryption, carrier-specific processing, and logging

**The app sends an AES-encrypted payload over HTTP.** It uses the MD5 of the JSON to derive AES key material, wraps that material with an RSA public key, and places the result in `sign`. AES uses ECB/PKCS5Padding and RSA uses PKCS1Padding. See [d0/k.smali:176](evidence/smali/d0/k.smali#L176), the [AES implementation](evidence/smali/f0/c.smali#L21), and the [RSA implementation](evidence/smali/f0/k.smali).

HTTP leaves URL parameters such as model and manufacturer without TLS protection and exposes the connection to endpoint authentication and integrity risks. The `sign` field contains public-key-encrypted key material.

For SIMs with a nonempty carrier code other than China Unicom, some IMSI, ICCID, MSISDN, CellID, and LAC fields undergo an additional MD5 step. IMEI and AppInfo are outside this step. AES still covers the entire request body. Deterministic digests can link values across records. See [e0/c2.smali:4159](evidence/smali/e0/c2.smali#L4159).

[d0/k.smali:150](evidence/smali/d0/k.smali#L150) appends the full plaintext JSON to a log string, and [line 163](evidence/smali/d0/k.smali#L163) passes it to `Log.d` before encryption. Access depends on system permissions, device configuration, and log truncation rules.

## 6. Exported component risk

`RegisterHelperService` is exported without an `android:permission` declaration in the [manifest](evidence/AndroidManifest.xml#L41). Its [onBind method](evidence/smali/com/mobiletools/systemhelper/service/RegisterHelperService.smali#L41) returns a Binder. Configuration input flows through [Binder dispatch](evidence/smali/com/mobiletools/systemhelper/service/a$a.smali#L125) to the [configuration writer](evidence/smali/com/mobiletools/systemhelper/service/RegisterHelperService$a.smali#L102).

External caller parameters can reach the reporting endpoint and interval settings. Later scheduling reads the interval when rescheduling. Android alarm management rules also apply.

## 7. Corrections to earlier analysis

| Topic | Reviewed finding |
|---|---|
| Request format | HTTP carries an AES-encrypted payload and RSA-wrapped key material. |
| App inventory scope | UsageStats records within the query window, with labels and UIDs filled in from package enumeration. |
| Timestamp | The JSON includes `Time`. |
| Interval configuration | The Binder writes preferences that later scheduling reads. |

## 8. Published analysis and evidence

- The [evidence index and SHA-256 hashes](evidence-index.json) cover 22 selected manifest and smali files with their original bytes and line numbers.
- The [sanitized original report](SOURCE_REPORT.md) carries a notice about subsequent corrections.
- See the archives for the [component and utility layer](archival/app-shell.md), [collector group one](archival/collectors-1.md), [collector group two](archival/collectors-2.md), and [core collection layer](archival/device-core.md).

The reviewed findings cite the code excerpts included in the publication where available.
