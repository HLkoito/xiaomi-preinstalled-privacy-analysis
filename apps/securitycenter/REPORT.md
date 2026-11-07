# MIUI Security Center Privacy Review

This report covers `com.miui.securitycenter` version `10.4.9-250402.0.1` (versionCode `40001049`). The sample is 66,917,411 bytes and has SHA-256 `fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd`. The findings apply to this sample.

**Static analysis identified code paths that read sensitive information, derive identifiers tied to the device, and transmit data. The advertising request combines several stable identifiers. Opening the privacy risk scan triggers a check that can upload non-system app package names and versions.**

See [the detailed review](REVIEW.md) for call chains, 42 source references, and corrections. The [evidence index](evidence/index.json) and [previous static verification record](evidence/verification.json) cover 40 selected Java and XML files with their original line numbers. Device runtime logs and actual user field values are excluded from the publication.

## Data flows and triggers

| Scenario | Data collected and sent in the implementation | Destination | Triggers, checks, and scope |
|---|---|---|---|
| Domestic antivirus results page advertising and layout | Android ID, OAID, and a conditional telephony device ID digest. A composite SHA-1 fingerprint of telephony device ID, Android ID, and serial number. Battery level, storage ratio, model, ROM, region, and other device attributes. | `https://adv.sec.miui.com/info/layout` | Ordinary requests check global network permission. Telephony device ID depends on a restriction property and successful API calls. The request includes the personalized-ads setting alongside the identifiers. See review §2. |
| Privacy risk scan | Calls `getInstalledPackages(0)`, filters out system apps, and serializes a map from package name to version number. Includes type, timestamp, system information, and model codename. | `https://flash.sec.miui.com/user/ra` | Opening the page automatically starts detection. Sending checks the network switch. The scope covers all non-system apps returned by enumeration. See §3. |
| Data usage query | IMSI, ICCID, phone number, city or region code, carrier, current monthly data usage, and an Android ID-derived digest | `https://api.miui.security.xiaomi.com/netassist/flow/queryflow` | Runs for specific data usage query tasks when network access is permitted. The `imei` field contains an MD5 digest derived from Android ID and a fixed business string. See §4. |
| Data usage calibration issue report | SMS body from the designated reply number, calibration command, reply number, province and city, carrier, and related fields | `https://api.sec.miui.com/v1/traffic/report` | The UI discloses the upload and requires confirmation. The implementation caches and retries failed submissions. It reads messages from the designated sender. Asterisks mask the additional phone number field. Personal content can still appear in the SMS body. See §5. |
| Emergency call assistance | Location and text address, OAID, phone number when available, emergency number, and call and location timestamps | `gateway-papa.cloud.weizhi.com/papa/upload`. A separate switch controls `eps.caict.ac.cn/eps/collect`. An SMS fallback is also implemented. | Applies to emergency calls and depends on settings and the agreement flow. The network payload uses AES. Sensitive SMS payloads are encrypted with RSA before sending. EPS supports emergency caller location. See §6. |
| Earthquake volunteer monitoring | OAID, latitude and longitude, timestamp, model, ROM, a nonempty volunteer token, and sensor arrays. Shared device parameters are added on send. | `https://api.sec.miui.com/earthquake/volunteer/sensor/upload` | Includes a privacy checkbox and a volunteer enrollment flow. See §7. |

MD5 and SHA-1 produce the same digest for the same input, so these values can link requests. The listed destinations are endpoints found in the client code.

`q2/a.java` caches identifiers derived from device information in process-wide static fields.

## Local data access and telemetry

On supported devices, the notification bubbles component invokes a system interface to grant notification listener access. The gaming accessibility service reads selected QQ and WeChat notification text and checks it for keywords. See review §8.

The app integrates OneTrack instances for multiple features and advertising events, and sets network permission. Monthly report upload checks `ma.c.a()`, which returns `false` in this build.

`FboPackageBean.upLoadPackageList()` writes the package list to local settings and passes it to the on-device FBO system service.

## Corrections to earlier analysis and reading order

Use this report and the [detailed review](REVIEW.md) for the reviewed findings. The [original report](SOURCE_REPORT.md) and [archives](archival/) document the original functional analysis, asset and native-code exploration, and evidence locations.

- The privacy scan enumerates the non-system apps returned by the API. The upload object's `apps` field contains package names and version numbers only.
- FBO writes to local settings and delegates to an on-device system service.
- The data usage API's `imei` field contains a digest derived from Android ID. The data usage SMS report masks its phone number field with asterisks. Emergency assistance encrypts sensitive SMS payloads with RSA.
- Earthquake warnings and volunteer monitoring are separate features. SMS reporting and volunteer enrollment have specific confirmation requirements.
- Some providers verify the caller's UID, package name, or signature in code.
- The network security configuration sets trust anchors for the miwifi domain and its subdomains. The primary network upload endpoints in the table use HTTPS.

## Analysis method

This report traces static code paths. The review computed hashes for the APK and its three DEX files, checked key DEX strings, and inspected selected Java and XML files. The published decompiled files match the original file hashes and retain line numbers and context.

## Included analysis tools

The [tools documentation](tools/README.md) covers parameterized static verification, AXML decoding, DEX string statistics, and partial AArch64 disassembly.
