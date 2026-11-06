# Xiaomi Preinstalled Applications — Privacy and Reverse Engineering

Static analysis of five preinstalled Android application samples. Each study follows data sources, field construction, conditions, local storage and implemented transmission paths. Findings apply to the exact versions and hashes in the [sample manifest](samples/manifest.json).

## English reports

| Application | Principal findings | Report and evidence |
|---|---|---|
| MIUI Analytics 6.33.0 | Device/SIM/account identifier association; application inventory; usage timelines; conditional radio and coordinate location; OneTrack and international Pub/Sub paths; event-specific privacy controls | [Complete report](apps/analytics/REPORT.md) · [Source excerpts](apps/analytics/EVIDENCE.md) · [Evidence index](apps/analytics/evidence-index.json) |
| MiuiDaemon 2.0 | Application/process memory and activity telemetry; exception/diagnostic disclosure risks; authentication-behavior metadata; retail versus unreleased-device heartbeat scope; certificate-validation weakness in the direct performance uploader | [Complete report](apps/miuidaemon/REPORT.md) · [Source excerpts](apps/miuidaemon/EVIDENCE.md) · [Evidence index](apps/miuidaemon/evidence-index.json) |

The reports distinguish static implementations, privacy implications and runtime questions. An implemented request is evidence of transmission capability, not proof of a transfer from a particular device. Hashing and encryption are described according to their actual functions; neither is treated as a blanket guarantee of anonymity.

The two reports include 39 selected, source-numbered evidence entries. Their publication scope, redaction rules and evidence conventions are documented in [Analytics and MiuiDaemon: Publication Scope and Method](docs/ANALYTICS_DAEMON_PUBLICATION.md).

## Earlier studies

The existing Chinese-language studies remain available:

| Application | Report |
|---|---|
| SystemHelper `1.2-20230925-U` | [Report](apps/systemhelper/REPORT.md) |
| GuardProvider `2.0.3-20231124.0` | [Report](apps/guardprovider/REPORT.md) |
| MIUI Security Center `10.4.9-250402.0.1` | [Report](apps/securitycenter/REPORT.md) · [Detailed verification](apps/securitycenter/REVIEW.md) |

Their [data-flow table](docs/DATA_FLOWS.md), [methodology](docs/METHODOLOGY.md), [privacy treatment](docs/PRIVACY.md) and evidence indexes document the scope of those three studies.

## Sample identity

| Package | Version | Version code |
|---|---|---:|
| `com.miui.analytics` | `6.33.0` | 2026091500 |
| `com.miui.daemon` | `2.0` | 30 |
| `com.mobiletools.systemhelper` | `1.2-20230925-U` | 3 |
| `com.miui.guardprovider` | `2.0.3-20231124.0` | 209 |
| `com.miui.securitycenter` | `10.4.9-250402.0.1` | 40001049 |

Sample hashes identify software artifacts. Device-owner identities, acquired personal values and raw device records are excluded from the publication.

## Offline verification

With Node.js 18 or newer, run from the repository root:

```sh
node tools/verify-integrity.mjs
node tools/privacy-check.mjs
```

[ARTIFACTS.json](ARTIFACTS.json) records publication file hashes. [The validation record](validation/analytics-daemon-publication-check.json) covers the two English reports and selected evidence. The [earlier validation record](validation/publication-check.json) applies to the preceding three-application publication.

Decompiled evidence derives from the identified software samples; rights in the original code remain with the respective rights holders. The repository publishes selected text for analysis, not APK binaries or a complete source redistribution.
