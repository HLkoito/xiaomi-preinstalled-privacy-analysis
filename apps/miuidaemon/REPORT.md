# MiuiDaemon 2.0 — Reverse Engineering and Privacy Analysis

## 1. Executive assessment

`com.miui.daemon` is a privileged MIUI performance and stability component. It applies configuration, gathers application/process performance data and fault records, coordinates diagnostic capture, and submits information through system Analytics, bundled telemetry SDKs and direct HTTP(S) clients.

Its most significant privacy implications arise from application/process names and activity identifiers in telemetry, stable device association, exception/log content, screen-on statistics and authentication-behavior records. Original IMEI, serial and Xiaomi-account heartbeat fields occur in an unreleased-device branch and must not be generalized to all retail devices.

A direct performance uploader installs an all-trusting certificate manager. This is a concrete transport-security weakness on that path, including when reached through the ordinary event-upload job. It does not establish that every SDK or system-service transport in the process has the same weakness.

All findings concern static implementations in the identified APK. The analysis does not establish actual delivery from a particular device, the contents of a collected diagnostic archive, or server retention and sharing practices.

## 2. Sample identity and method

| Property | Value |
|---|---|
| Package | `com.miui.daemon` |
| Version / version code | `2.0` / `30` |
| APK size | 2,605,685 bytes |
| APK SHA-256 | `fd6da9078566c0405cf4c76394c6cff4d1637b63f2cbcd0ebf21c49e1c4dddaa` |
| DEX | One `classes.dex`, 6,839,800 bytes |
| DEX class definitions | 4,769 |
| Decompiled Java files | 1,769 |
| Bundled native libraries | None |
| Manifest identity | `android.uid.system`, `persistent=true` |

The evidence base consists of the APK inventory, manifest/resource inspection, DEX inventories, JADX output, focused review of collection/upload/authorization code and retained REA inspection results. Decompiled Java remains a reconstruction; a declared table, imported class, permission, constant or method name is not by itself proof of an active collection path.

The retained signature inspection records v1/v2/v3 structures with certificate SHA-256 `c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025`. Signature structure and self-consistency do not independently establish publisher identity or rule out all repackaging scenarios.

Selected decompiled excerpts are published in [EVIDENCE.md](EVIDENCE.md), with hashes and source-relative locations in the [evidence index](evidence-index.json). Device-specific material, personal paths, staff contact information and embedded credential/key values are excluded.

## 3. System role, startup and dependencies

The manifest requests a system shared identity and persistent application behavior. These attributes are intended for an integrated system installation; actual privileges also depend on installation, signing, platform and SELinux policy. No ordinary third-party installation should be assumed to acquire the same environment.

`BaseDaemonApplication` initializes MQS, device details, device-level classification, conditional file observation, telemetry and application-memory tracking. MQS registers `miui.mqsas.MQSService` with `ServiceManager`, initializes cloud-control handling and schedules background work. See [D01](EVIDENCE.md#d01).

| Dependency / subsystem | Role |
|---|---|
| `perfshielder` | MIUI framework performance service consumed through Binder |
| `miui.mqsas.IMQSNative` / external `mqsasd` | Diagnostic capture, filesystem and packet-filter operations |
| `StatsManager` | Hidden-API metric configuration and report retrieval |
| `SettingsCloudData` | System-mediated cloud configuration input |
| `com.miui.analytics.TrackService` | Structured event handoff to a separate package |
| OneTrack / PubSub SDKs | Region-dependent telemetry queues and transport |
| Vendor `misys` interfaces | Hardware/system integration through external services |

The APK contains no `.so` libraries, but it can invoke native operations through external services and shell processes. Absence of bundled native code does not imply absence of native behavior.

## 4. Functional architecture

### 4.1 Configuration and performance management

`performance.cloudcontrol` parses system cloud data and bundled defaults, applies device/build/memory filters, and writes selected system properties or settings. `PerfTurboModule` declares 67 options: 37 property targets, 26 settings targets and four PerfTurbo targets in the retained inventory. These declarations describe configuration coverage, not 67 independent active samplers.

Memory-management code includes compaction/reclaim and zram operations. Other modules expose configuration switches while their named services collect no corresponding metric. Several strategy and executor methods are empty. The supplied atrace method constructs/logs a command and checks output without executing that command in the observed body.

Bundled JSON, parser permissiveness and external property consumers are distinct boundaries. A strict desktop JSON parse failure does not by itself establish that Android's parser rejects a bundled default or that all performance settings fail.

### 4.2 Performance and stability collection

Performance modules cover graphics statistics, application startup, notification outcomes, memory, process statistics, PSI, binder statistics and related diagnostics. MQS handles ANR, Java/native/kernel exceptions, hangs, recovery/rescue, camera and other event types.

The common conceptual flow is:

```text
event producer → normalization → local storage
                       ├→ optional capture through external mqsasd
                       └→ upload job → system Analytics / SDK / direct client
```

Not every defined event table has an active producer in this sample. In particular, the presence of package-event models and package enumeration does not establish complete installed-application-list transmission. `new_screenon` does have a writer in `HeartBeatUploader` and must be included among active paths.

## 5. Privacy findings

### 5.1 Application and process names accompany memory telemetry

`AppMemoryTrack` initializes on non-debuggable systems, periodically enumerates running processes and reads memory measurements. Qualifying processes produce samples; `app_memory` includes a `package` field populated from the process name, which may contain an application package and a process suffix. Statistics include minimum, maximum and average Java/native/swap/total memory and a proportion of device memory.

Low-memory handling selects the ten highest-memory processes and submits a `low_memory` event. The ordinary scheduled upload path is subject to its job gate. The direct low-memory submission method does not contain the same local check; downstream service or SDK checks remain relevant. See [D02](EVIDENCE.md#d02) and [D03](EVIDENCE.md#d03).

**Privacy impact.** These records can reveal which applications or processes were running and their resource profiles. Repeated records can support a partial application-usage profile. Process enumeration is not a complete foreground history, and memory statistics do not expose the application's in-memory business contents by themselves.

### 5.2 Activity launch and notification outcomes reveal behavior metadata

`ActivityUtils` assembles application/version and activity-component information with `launch_times` and `avg_launch_time`. `NotificationUtils` handles notification-click outcomes associated with XMSF and accumulates failures to launch a target application, grouped by target package. See [D04](EVIDENCE.md#d04) and [D05](EVIDENCE.md#d05).

The implemented route is:

```text
EventUploadService → PerfEventUploader → module.getEvents()
                                       → DataUploader.request()
```

The event job checks `Utils.isUploadEnabled()` before invoking this performance upload. The direct client therefore cannot be dismissed as an isolated debug-only remnant. See [D03](EVIDENCE.md#d03) and [D06](EVIDENCE.md#d06).

**Privacy impact.** Activity names may disclose a feature or workflow inside an application. Notification outcomes are interaction metadata. The inspected fields do not establish upload of notification text, message bodies, screen contents or user-entered values.

### 5.3 Stable association makes performance data linkable

`DeviceUtil.getID()` prefers OAID and otherwise uses a persisted UUID. MQS base-parameter construction distinguishes domestic and international branches; a parameter named `imei` may contain an OAID-derived or UUID value rather than a raw IMEI. `DataUploader` also constructs a stable MD5 from Android ID plus a fixed input and carries model/build/region/carrier metadata. See [D07](EVIDENCE.md#d07), [D08](EVIDENCE.md#d08) and [D09](EVIDENCE.md#d09).

**Privacy impact.** Repeated application-performance and exception reports can be grouped under a stable value. A persistent random UUID still supports matching across reports; a deterministic hash is not anonymization. At the same time, identifying a field name is insufficient to claim that all channels transmit a raw telephony identifier.

### 5.4 Exception details and diagnostic files may carry incidental sensitive content

`ExceptionUploader` serializes event information and hands it to the system analytics service on the released-device path. Relevant fields include package/process, timestamp, summary, details, log names and keywords. Other paths request diagnostic capture, packaging and file submission. See [D10](EVIDENCE.md#d10).

Much of the underlying capture is delegated to external `mqsasd`. Its complete capture rules, archive composition, encryption and possible redaction cannot be established from this APK. Heap-analysis support and hprof-related paths are also present; their existence is not proof that every heap dump is uploaded.

**Privacy impact.** Exception messages, stack traces, logs and paths can contain URLs, account references or application-generated text if upstream components include them. This is a risk of incidental disclosure, not evidence that a specific archive contains those categories. Database fields and digest/parser classes also do not establish comprehensive removal of sensitive material.

### 5.5 Screen-on and boot duration have different retail and test-device fields

The ordinary `screenon_release` path reports `screen_on_duration` and `boot_duration`. `HeartBeatUploader` explicitly calls `storeNewScreenonEvent()` and reads stored data for upload. See [D11](EVIDENCE.md#d11).

A separate unreleased-device branch constructs heartbeat information that includes serial number, dual-SIM IMEI, Xiaomi-account association, a persistent UUID and a per-boot UUID. The unreleased-device predicate considers development/internal-trial attributes and hardware stage; it is not equivalent to every development ROM or every retail device. See [D07](EVIDENCE.md#d07) and [D11](EVIDENCE.md#d11).

**Privacy impact.** Ordinary duration data can indicate usage intensity. The additional test-device fields enable substantially stronger identity association. These branches must remain separate when describing affected populations.

### 5.6 Fingerprint telemetry concerns authentication behavior

`FingerprintUploader` handles records containing calling package, invocation time, state and selected authentication/unlock timing or count fields. `BiometricInfoUploader` includes hardware supplier, device attributes and fault timing for biometric diagnostics. See [D12](EVIDENCE.md#d12) and [D13](EVIDENCE.md#d13).

**Privacy impact.** These fields can disclose which application invoked authentication and when, even without disclosing biometric content. The evidence does not establish upload of fingerprint images or templates. Behavioral authentication metadata and the biometric template are different data categories.

### 5.7 Network diagnostics do not establish full traffic-content upload

`MobileNetworkCapture` calls the external native service's `attachSockFilter` with address/protocol/port and related filtering inputs, and reads the resulting packet stream. The reviewed application-side diagnostic output includes application identification and RTT buckets. See [D14](EVIDENCE.md#d14).

**Privacy impact.** Network-quality records can reveal application/network activity. Packet access is sensitive, but the observed upload output does not establish that complete packets, browsing contents or chat-message contents are submitted. The external native implementation remains outside this sample.

### 5.8 Gallery observation is narrowly gated

The file-observer initializer requires a non-stable build and an allowlisted legacy device (`mido` or `sagit`). It targets standard camera/gallery directories. The subscribed mask is `1536 = 0x600`, corresponding to `DELETE | DELETE_SELF`; callback branches for other event types do not mean those types were subscribed. See the initializer in [D15](EVIDENCE.md#d15), callback in [D21](EVIDENCE.md#d21), and subscription in [D22](EVIDENCE.md#d22).

This establishes a conditional deletion-observation capability. It does not establish photo-content upload, general file-access surveillance or activation on an arbitrary modern retail device.

## 6. Privacy controls and authorization

The APK contains `Utils.isUploadEnabled()`, log-upload preferences, regional privacy checks, `PrivacyUtils`, and SDK user-experience checks. SDK initialization at process startup is insufficient to conclude that all collection or transmission is unconditional. The main event job visibly gates several uploaders, with additional region and device conditions. See [D03](EVIDENCE.md#d03), [D16](EVIDENCE.md#d16) and [D17](EVIDENCE.md#d17).

`PrivacyUtils.canUploadLog()` evaluates:

```java
(!needShowDialog(context) || canShowDialog(context))
    && PreferenceUtils.getBoolean(context, Constants.CAN_UPLOAD_LOG, true)
```

The preference defaults to true, and the expression includes whether a dialog can be displayed. Button handling can change permission and delete associated logs. This is evidence of a consent-related mechanism, but the expression alone does not prove that a transfer always waits for an affirmative response. The dialog is launched through an external action, adding another component boundary. See [D18](EVIDENCE.md#d18).

The correct privacy question is therefore whether each actual event/file transfer follows the intended preference and response ordering. Runtime tests must distinguish collection, local persistence, service handoff, network submission and server acknowledgement.

## 7. Transport-security findings

### 7.1 All-trusting certificate manager on the direct performance path

`TrustAllCerts` implements an `X509TrustManager` with empty certificate checks. `DataUploader.request()` creates a TLS context using that manager and installs its socket factory for HTTPS uploads. It subsequently writes the serialized event JSON to the connection. See [D09](EVIDENCE.md#d09) and [D19](EVIDENCE.md#d19).

**Impact.** When this path executes and an attacker can interpose on the connection, normal certificate-chain authentication is weakened, exposing device-linked application/performance records to interception or modification. Hostname verification, network position and platform details still affect an actual attack; the code does not imply passive decryption by every observer.

The finding applies to this client and reachable upload flow. It cannot be automatically extended to the independent system Analytics transport, every OneTrack/PubSub client, or all outbound requests in the process. Lack of certificate pinning alone would not establish the same defect.

### 7.2 File-upload ACL and path metadata

The international file-upload helper includes `cannedAcl=PUBLIC_READ` and submits a local absolute path as form metadata. See [D20](EVIDENCE.md#d20).

**Impact.** The ACL request raises a potential confidentiality concern if accepted by the storage service, and the path can reveal diagnostic naming and filesystem context. The code does not establish that the server accepts the ACL, that uploaded objects become public, or that names can be enumerated. No remote objects were tested.

### 7.3 Other transport and cryptographic boundaries

Cleartext is permitted by the network-security resource, and direct MQS/performance implementations coexist with system-service handoffs. Some signing/encryption mechanisms use embedded constants. Literal key, salt, credential and token values are omitted from this publication.

An allow-cleartext setting identifies capability, not a complete trace of sensitive data over HTTP. Likewise, app-layer signing or encryption must be assessed per path; neither an unsigned path nor one faulty TLS client establishes that the entire process lacks confidentiality and integrity.

## 8. Local storage and retention boundaries

The code defines eight database families with region-dependent SDK use:

| Database | Purpose |
|---|---|
| `mqsas.db` (version 25) | MQS events and configuration; retained schema inventory lists 48 tables |
| `failkeeper.db` | After-sales/reboot state |
| `performance.db` | Process/performance records |
| `otpubsub.db`, `othbpubsub.db` | PubSub queues |
| `onetrack`, `one_track_cloud`, `onetrack_ad` | OneTrack events, configuration and advertisement state |

Schema presence is distinct from runtime creation, writes or successful uploads. Domestic/international initialization selects different SDK families, so the table above does not imply that all stores exist on every device. The ORM default string `liteorm.db` is not evidence of a separately instantiated business database.

Shared preferences include MQS configuration and separate log/debug-log permissions. Diagnostic directories include `/data/mqsas/` and recovery-related paths. These are generic application/system paths, not paths copied from a person's captured logs.

Success handling can remove queued records; some system-service handoffs do not supply end-to-end server-delivery proof. Database migration or cleanup also does not establish service-side deletion or a documented retention promise.

## 9. Exposed components and implementation limits

`PerfTurboProvider` and the command executor use signature-level permissions. `MQSProvider` has no explicit exported flag in the retained manifest and is treated as non-exported under the applicable provider default. Internal lack of a caller check should not be converted into a claim that it is currently available to arbitrary applications.

`CloudServerReceiver` exposes the `update_profile` configuration trigger without a manifest permission. `OmniTestReceiver` uses a custom permission without an explicit restrictive protection level. They warrant validation of caller control and downstream action limits. The declared `BrokenScreenRescueService` has no matching class in the retained DEX inventory, so its manifest entry is not proof of a working rescue or command capability.

Hidden-API reporting uses `CONFIG_ID=888`. That is a statsd configuration identifier, not evidence that the collected atom number is 888. The unavailable configuration method body leaves the exact metrics configuration unresolved. Generated statsd/protobuf classes and imports likewise do not prove broad behavioral collection by every importing class.

These distinctions avoid confusing dormant models, compatibility code, generated constants and active telemetry. They also prevent attributing a compliance-evasion motive to the switch to system Analytics without supporting evidence.

## 10. Evidence gaps and validation priorities

| Question | Status |
|---|---|
| Are app/process memory, activity, diagnostic and duration event builders present? | Supported |
| Is direct performance upload reachable from the event job? | Supported, subject to gates |
| Does that uploader use an all-trusting certificate manager? | Supported |
| Are raw heartbeat identifiers universal on retail devices? | Unsupported; unreleased-device branch is separate |
| Is a complete installed-app list, photo content or full traffic content uploaded? | Not established |
| What exactly does external `mqsasd` capture and redact? | Outside this APK |
| Does a log transfer wait for an affirmative dialog action? | Requires runtime ordering tests |
| Does `PUBLIC_READ` produce publicly readable objects? | Unobserved |
| What does the server retain, join or share? | Unobserved |

Useful runtime validation would exercise the ordinary upload job with preferences enabled/disabled, observe low-memory direct submissions, test log-dialog ordering, inspect diagnostic minimization, and check the TLS client under an authorized controlled certificate test. The unreleased-device predicate and regional branches must be recorded for every result.

## 11. Conclusion

MiuiDaemon implements device-linkable application behavior and diagnostic telemetry as part of its system-performance role. The strongest supported concerns are application/process/activity association, incidental disclosure through diagnostics, authentication-behavior metadata, and a concrete certificate-validation defect in the direct performance uploader. Existing privacy controls, regional branches and unreleased-device conditions materially limit the scope of several paths. Static analysis establishes these capabilities and weaknesses while leaving actual delivery, consent ordering, archive contents and server handling unresolved.
