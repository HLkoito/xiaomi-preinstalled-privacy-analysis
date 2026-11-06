# Analytics and MiuiDaemon: Publication Scope and Method

## Scope

The English [MIUI Analytics report](../apps/analytics/REPORT.md) and [MiuiDaemon report](../apps/miuidaemon/REPORT.md) describe the exact APK hashes listed in the [sample manifest](../samples/manifest.json). They are self-contained static reverse-engineering reports with integrated privacy analysis.

Their evidence consists of APK metadata, decompiled source, manifest/resources, focused control-flow checks and selected source excerpts. Earlier broad inventory results are used for structural context; the included excerpts focus on privacy-sensitive paths. The publication does not claim complete runtime coverage or reproduce every decompiled class.

## Evidence interpretation

- **Observed:** fields, branches, API calls, resources and request implementations directly present in the analyzed artifacts.
- **Inferred:** privacy implications such as repeated device matching or application-interest profiling that follow from the data structure but do not establish server practice.
- **Unobserved:** actual device execution, successful server receipt, server-side retention, downstream disclosure, and behavior of external native services.

Source references use decompilation-relative paths and original line numbers. Each evidence entry records a SHA-256 of the complete local source file for provenance. Published file hashes are separately recorded in `ARTIFACTS.json`; these differ from complete-source hashes because only selected ranges are published. Fallback decompilation is identified separately. Excerpts retain decompiler comments and reconstruction artifacts rather than presenting them as original vendor source.

## Privacy treatment

The reports describe field names and their meanings without publishing real device or user values. The publication excludes workstation usernames and absolute user directories, chat/session references, device logs and captures, installation/usage histories acquired from a person, serial/IMEI/IMSI/ICCID/phone values, personal account identifiers, Wi-Fi names and hardware addresses, personal IP addresses, coordinates, staff emails, tokens and private keys.

Embedded credential/key identifiers and cryptographic key/salt literals are omitted from these additions when unnecessary to establish the finding. A source excerpt requiring such an omission marks the replacement and records its original line number. Source-file hashes do not reveal the omitted values.

Package/class names, sample versions and SHA-256 hashes, public certificate fingerprints, generic platform paths, API field names and software service domains are retained because they identify the software and make the analysis assessable. A statement that software constructs an `imei` or `lat` field is distinct from disclosing a person's identifier or coordinates.

No raw source report, conversation transcript, APK, DEX, captured database, packet capture or diagnostic archive is added with these reports. Their prose integrates the supported findings into a continuous analysis; it does not include prompts, replies, personal context or editorial conversation history.

## Validation

The publication checks sample size/hash locally, verifies every selected source range against its source, checks local Markdown links and evidence anchors, scans added text for privacy/credential candidates, and records publication file hashes. The repository-wide privacy checker supplements manual review; a rule-based pass is not a proof that arbitrary text can never identify someone.

The repository also contains three earlier application studies. Their existing files and language are preserved. Their own methodology and evidence indexes apply to those studies; the two English reports use the scope and evidence conventions documented here.
