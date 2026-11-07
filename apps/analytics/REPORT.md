# MIUI Analytics 6.33.0 — Reverse Engineering and Privacy Analysis

## 1. Executive assessment

`com.miui.analytics` implements a system analytics service with three event-processing stacks: legacy Analytics, OneTrack, and a Google Cloud Pub/Sub transport used by selected international configurations. Its code constructs device and SIM identifiers, an installed-application inventory, application usage timelines, installation events, and conditional location information, then persists and submits events through implemented network clients.

The principal privacy concern is the association of these data categories. A device-level record can combine stable identifiers with installed applications, usage timing, and a location snapshot. Hashing identifier fields does not remove their value for repeated matching. An encrypted payload containing original identifiers also remains identifiable to a recipient able to decrypt it.

These are static findings for the sample identified below. They establish implemented collection and transmission paths, not that a particular device executed every path or that a server received a particular record. Permission availability, regional branches, configuration, event-specific gates, and server responses affect execution. Server retention, onward disclosure, and the adequacy of any consent presented to users are outside the observed evidence.

## 2. Sample identity and method

| Property | Value |
|---|---|
| Package | `com.miui.analytics` |
| Version | `6.33.0` |
| Version code | `2026091500` |
| APK size | 525,597 bytes |
| APK SHA-256 | `a3656199f013e2f68bc20022f941122a13ce99e33c5df1cef054c9267189d73f` |
| Application class | `com.miui.analytics.AnalyticsApp` |
| DEX | One `classes.dex`, 1,025,604 bytes |
| Decompiled output | 507 Java files |
| Bundled native libraries | None |

The analysis uses the APK inventory, manifest and resources, JADX 1.5.6 output, focused source inspection, and fallback decompilation for ambiguous control flow. Obfuscated names are retained as evidence locators. Decompiled Java is a reconstruction and may contain incorrect expressions or unstructured branches; the installed-application filter was checked against fallback output rather than relying on its high-level rendering.

The retained signature inspection records v1/v2/v3 structures and the certificate fingerprint `c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025`. Matching certificate metadata and signature self-consistency do not independently establish the publisher's identity, trustworthiness, or the original acquisition channel.

Selected source evidence is included in [EVIDENCE.md](EVIDENCE.md). Its numbered excerpts preserve original source line numbers; the [evidence index](evidence-index.json) records source-relative paths and hashes. No device logs, acquired identifier values, credentials, or APK binaries are distributed.

## 3. Architecture and lifecycle

| Stack | Main responsibilities | Storage / transmission |
|---|---|---|
| Legacy Analytics | `LogEvent`, policy groups, older client interfaces and advertisement monitoring | `analytics.db`, `analyticsv2.db`, `requests.db`; default `/track/v1` |
| OneTrack | Structured event headers and bodies, active/device events, usage, application inventory, configuration, monitoring | `onetrack`, cloud/cache databases; regional `/track/v4` |
| Pub/Sub | Selected event routing, credential/token management, batching and publication | Project/message queues; `pubsub.googleapis.com` publish API |

`AnalyticsApp` initializes asynchronous workers, schedulers, alarms, package receivers, and analytics state. Boot, provisioning, SIM-state changes, package lifecycle events, and scheduled wakeups can feed subsequent work. Initialization alone does not establish that every event is permitted to upload.

The implementation also contains device-validation tokens, TEE-backed signing, self-update handling, advertisement callbacks, a privileged file-based event channel, and bugreport log export. External system services and vendor HALs remain separate trust and execution boundaries.

## 4. Interfaces and trust boundaries

The manifest exposes services and providers used by other applications. `AnalyticsService` supplies the legacy `ICore` interface; `OneTrackService` supplies `ITrack`; `TrackService` supplies the permission-protected `ITrackBinder` interface. `EventService` accepts legacy event fields and `AppenderService` accepts download, first-launch, appended-usage, and hybrid-state inputs.

Several of these components have no manifest-level permission. That fact identifies an interface requiring inspection, but is insufficient by itself to prove that every operation is exploitable. Method-level checks, protected broadcasts, platform restrictions, and the installed system environment must also be considered.

| Interface | Observed boundary and consequence |
|---|---|
| `TrackService` | Uses `com.miui.analytics.onetrack.TRACK_EVENT`; not an unrestricted event entry point |
| `OneTrackService.getWritableFileDescriptor` | Checks calling UID/system or privileged status before exposing the event-file channel |
| `AnalyticsProvider` validation/literal operations | Contain caller checks; their presence must not be generalized to all provider operations |
| `OneTrackProvider.insert`, `insId` path | Writes the distinct `ot_i_c` instance-ID cache via `a.b.a.e.l0.n`, with key/value processing |
| Legacy/event injection interfaces | Need end-to-end caller validation testing before assigning a device-specific exploitation result |

The instance-ID write path does **not** establish arbitrary modification of `one_track_pref`, DID state, or upload preferences. Its actual cache scope is narrower. See [A10](EVIDENCE.md#a10).

## 5. Event and data-flow inventory

| Event family | Data constructed | Trigger / scope | Implemented destination path |
|---|---|---|---|
| `onetrack_active` | Device/build attributes, identifiers, synchronization state, conditional location, encrypted raw-identifier payload | Activation checks, time/day boundaries, identifier/account state and regional branches | OneTrack and/or configured international transport |
| `onetrack_installed_app` | Package, version, installer, installation/update times, status, `ot_enable` | Scheduled inventory with package-selection rules | Structured event pipeline |
| `onetrack_usage` | Package, timestamps, event type, duration, version, selected activity names | UsageStats processing and regional rules | Structured event pipeline; international routing can differ |
| `onetrack_app_stat` | Install/update/uninstall/download/first-launch metadata and attribution fields | Package and client events | Structured event pipeline |
| `onetrack_id` | Changes in cached identifier state | Comparison against prior stored identifiers | Identifier event / DID-related processing |
| Custom, exception and bugreport events | Caller-provided bodies, exception or diagnostic information | Client or system requests | Event queue, file channel or configured transport |
| Monitoring / advertising | Request URLs and performance counters; event/queue outcomes; advertisement monitoring URLs | Timers and advertisement callbacks | Monitoring endpoint, Pub/Sub, or advertisement-specific requests |

The following sections distinguish field construction from its privacy implications and from unobserved runtime outcomes.

## 6. Privacy findings

### 6.1 Device, SIM and account identifiers can be associated in one event

The active-event builder handles Android ID, OAID, VAID and UDID, together with hashed device/SIM identifiers such as IMEI, MEID, IMSI, MAC and serial number. Field availability varies by platform and branch. The nested `tz_content` builder explicitly constructs original-value fields:

| Field | Meaning |
|---|---|
| `imei1`, `imei2` | Primary and secondary IMEI |
| `sn` | Device serial number |
| `i1`, `i2` | SIM subscriber identifiers (IMSI) |
| `mid` | Xiaomi account name in the domestic branch |

These values are assembled before encryption. The non-TEE branch also sets encrypted `tz_content`; hardware signing support is not a prerequisite for constructing this sensitive payload. See [A01](EVIDENCE.md#a01) and [A02](EVIDENCE.md#a02).

**Privacy impact.** Placing identifiers from the device, SIM and account in the same record enables association across those identity domains. Stable hashes allow repeat matching when the input remains the same. Encryption protects data under its key-management assumptions but does not make original identifiers anonymous to an authorized decrypting recipient. This establishes a linkability concern without establishing the server's actual joining practices or retention period.

### 6.2 Location collection includes radio context and a coordinate/address branch

The domestic active-event path inserts a `loc` value assembled from cellular and Wi-Fi information. The builder handles cell-network identifiers including MCC/MNC/LAC/CID, signal information, connected Wi-Fi SSID/BSSID/RSSI/frequency, nearby scan results, and collection times.

When `first_active_upload_position` has not been set, it also attempts to add GPS/network location: latitude, longitude, provider/source and timestamp. Reverse geocoding attempts to add country, administrative region, city, subregion and road information. See [A03](EVIDENCE.md#a03) and the event insertion in [A02](EVIDENCE.md#a02).

**Privacy impact.** Coordinates can reveal a location directly. Cell and Wi-Fi identifiers can also support location inference or repeated-place recognition. Associating either with stable identifiers increases the sensitivity beyond anonymous radio-quality statistics. Available permissions, enabled location services, platform restrictions and nonempty results remain execution requirements. The code does not establish continuous GPS tracking or a complete movement history.

### 6.3 Installed-application collection includes non-launcher third-party applications

The package enumeration path selects an application when it has a launcher entry, matches the built-in inclusion list, is an enabled input method, **or is not a system application**. A system application that satisfies none of these conditions is skipped. The fallback branch targets confirm that the built-in package list is an inclusion mechanism. See [A04](EVIDENCE.md#a04).

Each selected entry includes `pkg`, `vn`, `vc`, `installer`, `fit`, `lut`, `status`, and `ot_enable`: package name, version name/code, installation source, first-install time, last-update time and status metadata. In this builder, `ot_enable` is recorded as an attribute; it is not used as a universal condition that prevents an application's collection when false.

**Privacy impact.** Application combinations can indicate interests, services used, organizational affiliation, or sensitive categories of activity. Installation timestamps add a temporal dimension. Non-launcher third-party packages expand the inventory beyond what appears on the home screen. These are profiling possibilities inferred from the data structure; installation alone does not prove actual use or any personal trait.

### 6.4 Usage telemetry can expose application timelines and selected pages

`onetrack_usage` uses `UsageStatsManager.queryEvents` and related usage-statistics APIs. Its records contain package, event type, timestamp, duration and version, with activity/class, `app_start`, `app_end`, and aggregate durations in relevant paths. A 15-minute check interval and a bounded processing window must not be described as collection occurring only once per day. See [A17](EVIDENCE.md#a17) for the scheduling check.

The observed activity-name filtering is inside an international-build branch. Its regional lists include VN/MY/TH entries for wallet, commerce, payment or checkout-related pages. Those lists do not establish identical filtering on all builds. See [A05](EVIDENCE.md#a05) and [A06](EVIDENCE.md#a06).

**Privacy impact.** Start/end timing and durations can disclose routines and sequences of application use. Activity names may indicate entry into a particular workflow. A payment-related page name does not establish a successful payment, transaction amount, card number, order content, or access to screen contents.

### 6.5 Installation and identifier-change events extend longitudinal association

Application lifecycle builders distinguish download, install, update, uninstall, first launch and package-installer success. Their fields include package/version/installer information and attribution identifiers. Identifier-change handling compares current values with an encrypted local cache before constructing change events.

**Privacy impact.** Lifecycle events can update a device-level application profile between full inventory runs. Identifier-change events can support continuity of device records across changes. The existence of these mechanisms does not prove that resetting an advertising identifier is always defeated; that outcome depends on what identifiers remain available and how the recipient uses them.

### 6.6 Privacy controls are distributed across events and regions

The code contains configuration and privacy checks, but the evidence does not establish a single switch covering every collection and upload path. In the relevant international-transport decision, `onetrack_active` is an explicit exception to requiring `upload_debug_log_pref == 1`. The domestic branch of the function corresponding to `isAllowTrackForRussia` returns permission without using that function to enforce the usage-reporting switch. See [A07](EVIDENCE.md#a07) and [A08](EVIDENCE.md#a08).

Active events also report states such as `uep` and `allow_report_usage`. Including a preference value in a record does not itself suppress that record. Active scheduling additionally considers cross-day changes, missing prior valid IMSI information and a newly available domestic account; it is not a simple unconditional 24-hour cooldown.

**Privacy impact.** Users may reasonably expect a setting to apply more broadly than one event family. Static evidence supports testing this mismatch, but does not establish which events actually leave a device after a specific switch is disabled. That requires controlled runtime tests of each event and transport.

### 6.7 Diagnostic, custom and monitoring payloads require separate minimization

Custom event bodies and exception information originate partly in clients. Monitoring records include request URLs/hosts, timings, error categories, application/event identifiers and delivery counters. These fields can contain sensitive context even when they are intended for operational diagnostics.

`bind_stats` represents synchronization/service status flags, including contacts or SMS synchronization status; it does not establish collection of contact records or message contents. `queryLiteral` accepts caller-supplied material and forwards it toward XMSF; that does not establish keyboard interception. No finding here establishes photo, chat-message, bank-card or complete browser-history collection.

## 7. Persistence, encryption and transmission

### 7.1 OneTrack queue

The `onetrack` database uses an `events` table with application/package/event metadata, priority, timestamp, type and a `data` BLOB. The write path calls `m()` and encrypts the event data before storing it; this is not simply plaintext JSON bytes in the BLOB. See [A09](EVIDENCE.md#a09) and the AES helper in [A16](EVIDENCE.md#a16).

The implementation batches queued records, applies header compression where applicable, serializes events, compresses and encrypts the upload data, and sends it to a regional tracking URL. The request path uses `/track/v4`; the domestic default is `tracking.miui.com`. Success handling removes queued events and failure handling influences retry scheduling. See [A11](EVIDENCE.md#a11) and [A12](EVIDENCE.md#a12).

The queue includes size/age cleanup logic, including ordinary and active-event age distinctions. Client cleanup describes local queue management, not a server-side deletion guarantee.

### 7.2 International Pub/Sub routing

Resource defaults and runtime configuration choose whether specific international events use OneTrack, Pub/Sub, or supported combinations. Routing is conditioned on build, region, per-event settings and remote configuration; it is not a statement about every non-domestic device.

The publisher groups messages by attributes, compresses/encrypts event arrays, and calls the Google Cloud Pub/Sub publish API with a bearer token. The code includes token refresh and publication-response handling. See [A13](EVIDENCE.md#a13) and [A14](EVIDENCE.md#a14).

This adds an external transport/processing boundary. The presence of a Google API endpoint does not by itself prove that Google can decrypt every application-layer payload, that downstream ingestion is implemented exactly as a client-side label suggests, or where all eventual processing occurs. Credential values and key identifiers are omitted from this publication.

### 7.3 Legacy, advertisement and supporting requests

Legacy events use policy-controlled batching, sampling, discard and upload settings, with `/track/v1` as a default path. Advertisement events may issue requests to supplied monitoring URLs and persist failed requests for retry. Self-update code retrieves version information, downloads a candidate package and performs digest/signature checks before installation handling.

| Function | Endpoint or domain family present in the implementation |
|---|---|
| OneTrack upload | Regional `tracking.*.miui.com`, `/track/v4` |
| Key/nonce processing | Tracking host, `/track/key_get`, `/track/getNonce` |
| Legacy upload | `tracking.miui.com/track/v1` |
| Cloud configuration | Regional `sdkconfig.*.xiaomi.com` |
| Monitoring | `/track/mt`, including `om.tracking.miui.com` |
| Pub/Sub | `pubsub.googleapis.com/v1/projects/{project}/topics/{topic}:publish` |
| Advertisement log constant | `http://api.ad.xiaomi.com/video/uploadLog` |
| DNS quality measurement | `dns.google/dns-query` |

These are code destinations, not endpoints contacted during this analysis. Regional host overrides and server configuration can change routing. The network-security resource allows cleartext traffic and an HTTP advertisement URL is present, but this alone does not establish that sensitive active/usage fields are sent over HTTP. See [A15](EVIDENCE.md#a15).

## 8. Additional local state and cryptographic boundaries

The implementation defines runtime databases for legacy events, advertisement retries, GUIDs, application-version comparisons, application-inventory comparison, hybrid usage, cached runnable events, Pub/Sub project/message state and monitoring. These are code-defined runtime stores; the APK does not contain a captured user database.

Relevant preferences include `one_track_pref`, `pref_id`, `pref_encrypt`, `pubsub_pref`, and the distinct `ot_i_c` instance-ID cache. Event files under `cache/track_files/` support a checked system/privileged-client channel. Debug-log directories can be exposed to a permission-protected bugreport provider.

Cryptographic functions include payload encryption, Android Keystore wrapping, file-channel AES-GCM and vendor TEE signing. Some local/protocol transformations rely on embedded constants. Their literal values are unnecessary to evaluate collection scope and are not published. TEE signing can authenticate an input under the device service's assumptions; it does not demonstrate consent, minimization, or anonymity.

## 9. Evidence limitations and validation priorities

| Question | Status |
|---|---|
| Are identifier, inventory, usage and conditional-location builders implemented? | Supported by source evidence |
| Is there a queue-to-network implementation? | Supported for OneTrack and Pub/Sub |
| Are original identifiers present before `tz_content` encryption? | Supported |
| Did a particular device send a particular event? | Unobserved |
| Does disabling a named UI setting stop every channel? | Unresolved; requires event-by-event runtime testing |
| What do vendor services and the recipient do with the data? | Outside the APK evidence |
| What are server retention, access and downstream-sharing policies? | Unobserved |

Priority validation would compare event construction and network activity with usage/log preferences on and off, across domestic/international branches, with location denied and allowed, and before/after identifier changes. Tests should inspect decrypted payloads only within an authorized environment and distinguish local queuing from acknowledged delivery.

## 10. Conclusion

The sample implements broad, device-linkable analytics. Its clearest privacy-sensitive capabilities are the combination of device/SIM/account identifiers, application inventory, usage timelines and conditional radio/coordinate location in remotely submitted event pipelines. Distributed controls and regional exceptions prevent a blanket conclusion that one preference governs all telemetry. Encryption and hashing provide specific technical transformations, not evidence of anonymity. Actual execution, consent effectiveness and server handling remain questions for controlled runtime and service-side evidence.
