# Privacy Practices for the Publication

## Publication scope

The publication includes app sample metadata, sanitized reports, selected static code evidence, analysis tools, and file hashes. Source-device identity and runtime records stay local.

- Remove local usernames, home directories, personal accounts, chat and session identifiers, and paths tied to the source user.
- Exclude raw ADB output, the phone's installed-app inventory, permission access history, usage records, scheduler logs, and raw chat exports.
- Exclude actual device serial numbers, IMEIs, IMSIs, ICCIDs, phone numbers, network names, router addresses, personal IP addresses, coordinates, and token values.
- Check file names, text, and Git author metadata separately. The public history uses a neutral research identity.
- Scan for excluded identifiers without regard to case, including after URL decoding and Unicode normalization.

## Information included

Package names, versions, SHA-256 hashes, vendor service domains, API field names, public certificate identifiers, SDK endpoints embedded in the code, and algorithm constants support sample identification and code review.

Terms such as `IMEI`, `SSID`, and `OAID` identify fields in the implementation. Actual field values from the source device stay local.

## Verification

Review selected files and scan results before publication. Run `tools/privacy-check.mjs` to check for user paths, session IDs, common credential patterns, and excluded identifiers. Run `tools/verify-integrity.mjs` to recompute the hashes in `ARTIFACTS.json`.

The privacy scan applies the listed rules. Publication review records also describe the manual checks performed on selected files.
