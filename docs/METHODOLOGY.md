# Methodology

## Evidence scope

This document covers three preinstalled apps exported from the same source device. Package name, version, size, and APK SHA-256 identify each sample. The findings describe implementation paths in static code.

The evidence comes from three original reverse-engineering investigations and two follow-up reviews with code verification.

| Source | Focus | Published materials |
|---|---|---|
| Original analysis A | SystemHelper | Main report, four focused analyses, and selected smali and manifest files |
| Original analysis B | GuardProvider | Main report, five focused analyses, and selected Java and manifest files |
| Original analysis C | MIUI Security Center | Main report, seven focused analyses, an independent privacy review, selected Java and XML files, and offline analysis tools |
| Follow-up review | Field assignments, upload calls, permissions, and consent flows | Corrections in the final reports and data-flow table |

The original conversations and device logs stay in the source environment. The publication includes sanitized reports and selected code evidence.

## Tracing a finding

Follow data from the read API through field assignment, hashing, request construction, and the sending call. Check the feature entry points and permission or configuration gates along that path. Interpret interface and method names in the context of their callers and assignments.

The [SystemHelper](../apps/systemhelper/evidence-index.json), [GuardProvider](../apps/guardprovider/evidence-index.json), and [Security Center](../apps/securitycenter/evidence/index.json) indexes identify the selected evidence. Files preserve their original line numbers and are identified by SHA-256. Relative links in the reports lead to the included evidence. The original REA evidence IDs document the investigation, and the files and hashes support offline review.

## Corrections incorporated in the reviewed reports

| Topic | Reviewed finding |
|---|---|
| SystemHelper registration body | AES encrypts the JSON before it is sent over HTTP. |
| SystemHelper app inventory | UsageStats records are the source, with a default seven-day window. |
| GuardProvider OneTrack | The code reads IMEI and IMSI and generates MD5 lists subject to region, configuration, and other gates. |
| Security Center app inventory | The privacy scan task assembles the package names and versions of enumerable non-system apps. |
| Security Center data usage `imei` | The field contains an MD5 digest of Android ID combined with a fixed business string. |
| Security Center calibration SMS | The UI prompts for confirmation before upload, and the phone number field is masked. |
| Security Center emergency call assistance | The HTTP and SMS paths use AES and RSA respectively for their sensitive payloads. |
| Security Center FBO | Writes local settings and passes the list to an on-device system service. |

## File organization

Each `REPORT.md` and the Security Center `REVIEW.md` contain the reviewed findings. `SOURCE_REPORT.md` and `archival/` contain sanitized historical analyses and notes about subsequent corrections. Use the reviewed reports and corresponding code when citing conclusions.

The directories contain text relevant to the investigation. Sample hashes and sizes identify the APKs. The full decompilation trees are stored locally, and the publication includes selected files that support the data-flow findings.
