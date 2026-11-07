# MIUI Security Center: Independent Privacy Review

Review date: 2026-11-06. Target: `com.miui.securitycenter`, `10.4.9-250402.0.1`.

**Static analysis identified paths that read sensitive data, construct identifiers tied to the device, and send data to servers. The advertising request combines several stable device identifiers. The privacy risk scan can upload an inventory of non-system apps.**

This sanitized review links to the evidence files included with the publication. Where the original report or archives differ, use this review and the corrections in REPORT.md.

## 1. Analysis methods

- Read the general report, key links of `core-security.md`, and privacy-related indexes in other special projects; directly check the decompiled codes of HTTP, advertising, application enumeration, traffic query, SMS return, alarm positioning, earthquake volunteers, notifications, Provider, etc.
- Recalculate original APK SHA-256: `fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd`, size `66,917,411` bytes, consistent with total report.
- Read three DEX from the original APK ZIP and confirm that the three unpacked copies of SHA-256 and `apk_full` are identical one by one; the 15 key endpoints/fields/method strings can all be found in the DEX string table in APK.
- Save the hash of the spot-checked Java and XML files to easily identify the corresponding versions. Based on static code analysis, the implementation path is described.

Reproduction script: [privacy_review_verify.cjs](tools/privacy_review_verify.cjs). Machine-readable evidence: [privacy-review-evidence-20261106.json](evidence/verification.json).

Priorities in the table are ordered by data sensitivity and analytical value.

## 2. Most noteworthy: Ad requests that combine multiple device identifiers

### 2.1 Data sources, processing and sending links

In the domestically constructed anti-virus result page layout request, the following paths can be strung together:

```text
antivirus/result/f.l()
  Add deviceId, ua, channel, etc.
  → xe.a.f()
      Add isPersonalizedAdEnabled, cv, ror, rbr
  → eg.j.B() → eg.j.C(..., false, true, ...)
      Check the networking permission switch
      → eg.j.b() merges public parameters
      → HttpURLConnection POST
  → https://adv.sec.miui.com/info/layout
```

| Field | actual data source | Privacy Implications and Limitations |
|---|---|---|
| `x` | `Settings.Secure.android_id`, by `eg.c.a()` | The Android ID added directly to the domestic branch; not just the IMEI hash |
| `oa` | `ef.w.a()` → `y4.a.a()` → Reflection `IdProviderImpl.getOAID()` | OAID; will be empty when the interface is unavailable or returns empty |
| `m` | `eg.c.c()` of `TelephonyManager.getDeviceId()` → MD5 → string encoding | Conditional phone device ID summary |
| `deviceId` | `q2.a.a()` does SHA-1 for `getDeviceId() + Android ID + hardware serial number` | Composite fingerprint derived from device information, cached in-process from static fields |
| `rbr` | `BatteryManager.getIntProperty(4)` | Current battery percentage |
| `ror` | Available bytes of external storage / main storage capacity, converted into a percentage | The remaining proportion of storage is `-1` if the read fails. |
| Other public parameters | Model, ROM/system version, region, operator, language, network type, etc. | Requests can be associated with stable identifiers |

Precise evidence: [ad layout request f.java:261][ad-request], [public parameters j.java:542][net-params], [static parameters j.java:129][net-static], [composite fingerprint q2/a.java:18][fingerprint], [ad state xe/a.java:19][ad-state], [device ID eg/c.java:54][device-id],[OAID reflection y4/a.java:28][oaid].

### 2.2 Switches, conditions and actual risks

- `m` is controlled by `ro.miui.restrict_imei` / `ro.miui.restrict_imei_p`; it is empty when the limit is on. The public parameters of the international version are separated into the advertising ID branch.
- Normal request `eg.j.C()` checks `x4.l1.b()`. It reads the system networking permission attribute, and the default parameter is `false`; the international version additionally checks the privacy status.
- `xe.a.a()` reads the personalized advertising switch and adds the Boolean value to the request along with the device ID. `xe.a` → `eg.j.b()` Assemble `x`, `oa`, `m` and `deviceId` according to the above conditions.
- A stable hash of the same input produces the same digest. A single request combines OAID, Android ID, and serial number derived identification, with cross-request correlation value.
- `q2.a.b()` The normal branch returns the hash, and the abnormal branch directly returns the input string.

**Judgment: The commercial advertisement/content layout request combines multiple device identifiers and device status to form a strong device association capability.**

## 3. Upload non-system application package name and version for privacy risk detection

The following links can be completely connected:

```text
PrivacyRiskDetectionActivity.onCreate() → Y0()
  → new ub.d(...).execute()
  → xc.h.a(context)
      PackageManager.getInstalledPackages(0)
      Filter out applicationInfo.flags & FLAG_SYSTEM != 0 packets
  → ac.c.d(list)
      apps = { packageName: versionCode, ... }
      Attached are type, timestamp, os, device, biz_id
  → ac.c.c(payload) → ac.c.g()
  → POST https://flash.sec.miui.com/user/ra
```

Evidence: [Page entry: 1114] [risk-entry], [Task start: 920] [risk-start], [ub/d.java:46] [risk-task], [Application enumeration xc/h.java:13] [app-list], [Serialization ac/c.java:62] [app-payload], [Send ac/c.java:90] [app-send].

`xc.h.a()` determines the system mark of the application. The enumeration range is all non-system applications **returned by**PackageManager in the current environment, including applications without desktop icons.

The local enumeration object also contains the application name, UID, and icon URI; the `apps` serialized package name and version number of `ac.c.d()`, and the attached `device` field is the model code.

This request checks the `ef.x.z()` networking switch, and the page `onCreate()` automatically starts the detection task.

The application list can reflect installed application categories such as finance, medical care, dating, religion, and work, and has user profiling value; this link uses the application list for cloud risk matching.

The similar `/user/cat` constructor also exists; this section shows the complete call chain of `/user/ra`.

## 4. Traffic service request contains SIM and mobile phone number data

`TrafficRelatedPreFragment.GetVirtualOperatorTask.doInBackground()` calls `queryDataUsage()` after the network is allowed, passing in the IMSI, city, mobile phone number, operator, traffic volume used this month and ICCID of the SIM object. The request object fields are:

```json
{
  "imei": "MD5(Android ID + '-' + fixed business string)",
  "imsi": "SIM IMSI",
  "zipCode": "city or region code",
  "phonenum": "phone number",
  "spType": "carrier",
  "monthused": "data used this month",
  "iccid": "SIM ICCID"
}
```

Target: `https://api.miui.security.xiaomi.com/netassist/flow/queryflow`.

Evidence: [calling end TrafficRelatedPreFragment.java:116][sim-caller], [field WebApiAccessHelper.java:67][sim-payload], [identifying actual source DeviceUtil.java:84][androidid-hash].

The actual source of the **field: `imei`. Fill in the Android ID MD5 with the fixed business string.** IMSI and ICCID have strong subscriber correlation with mobile phone numbers. The table above lists the fields of the request object before encryption/encapsulation.

This traffic query path checks the networking switch and adds subscriber information to the request object.

## 5. The text of the flow calibration SMS will be uploaded, but there is a special confirmation process.

`TcSmsReportService` only receives text messages that match the configured receipt number, and then reads `getDisplayMessageBody()`; this does not scan all text message inboxes.

The uploaded data includes calibration request number, instruction, receipt number, collected SMS text, province and city, operator, brand, type, etc. Target:

`https://api.sec.miui.com/v1/traffic/report`

Its `report()` puts the SMS list into the JSON array and appends the local number processing result. `getEncryptPhoneNumber()` replaces the middle digits of the mobile phone number with `*`, which is a number mask; the text message text will directly enter the upload list.

Evidence: [SMS read TcSmsReportService.java:54][sms-read], [Submit data:178][sms-body], [Field and target WebApiAccessHelper.java:83][sms-payload].

`TcSmsReportFragment.tcSmsReportDeclare()` pops up `privacy_prompt_tc_sms_report_message`, and only the callback `onOptionUpdated(true)` calls `reportSms()`. Evidence: [Special upload notification:416][sms-consent]. There is also a local cache and retry path for failed data.

**Judgment: This link uploads the operator’s calibration text message content after confirmation by the dedicated user.**

## 6. Alarm assistance sends location, number and time to third party

Get the coordinates by `PaService.TransDataTask`, check the text address through `Geocoder`, and select the network or SMS channel according to SIM/local number status.

| receiving end | Data that appears explicitly in the request | conditions and boundaries |
|---|---|---|
| `gateway-papa.cloud.weizhi.com/papa/upload` | OAID (named `deviceId`), local mobile phone number when available, emergency number, area code, calling time, longitude and latitude, text address, positioning start and end time | Alarm auxiliary scenario; the request body is encrypted with AES; the normal network transmission has network permission check |
| `eps.caict.ac.cn/eps/collect` | Calling number after SM3, emergency called number, location, address, accuracy/altitude (conditional), time, equipment model, etc. | Domestic version and the EPS switch is turned on; the default value read by this switch is 0 |
| `106903590009` SMS | Binary payload of emergency number, area code, latitude and longitude, time, OAID, etc. | No local number/network failure and other downgrades; sensitive payloads are first encrypted with RSA |

Evidence: [Location and channel selection PaService.java:182][police-flow],[Network object PaUtils.java:104][police-fields],[EPS field EPSManager.java:49][eps-fields],[EPS switch:82][eps-toggle],[Endpoint PaCommon.java:6][police-endpoints],[SMS structure PaUtils.java:82][police-sms].

`buildMessageData()` calls `rsaEncrypt()` on the 117-byte data block containing coordinates and other contents, and then uses UTF-16 characters to wrap it. The fields of EPSManager are used for emergency call location reporting.

This module has settings switches, protocol/privacy entrance and withdrawal process. `PaSettingActivity.showRevokeDialog()` Set 10 second countdown on withdraw confirmation button disabled.

**Judgment: Alarm Assist implements a path to share location and identity-related data with third parties.**

## 7. Earthquake volunteers upload OAID, location, time and sensor sequence

Fields can be confirmed directly from `EarthquakeMonitorDataManager.threadPoolRun()`:

- `oaid`、`lng`、`lat`、`timestamp`、`model`、`rom`；
- Non-available volunteer `token`;
- Floating point sensor array in `data`.

Target: `https://api.sec.miui.com/earthquake/volunteer/sensor/upload`.

Evidence: [Field assembly: 51][quake-fields], [Also merge public parameters EarthquakeMonitorUtils.java:54 when sending][quake-send], [OAID/location data source EarthquakeWarningService.java:113][quake-source].

`EarthquakeMonitorUtils.uploadData()` calls `eg.j.b()` to add public parameters, and merge Android ID, OAID and device ID summary according to the aforementioned conditions. The earthquake early warning service adopts 10-minute positioning refresh throttling logic.

Clear evidence of user interaction: In `EarthquakeWarningMonitorOpenFragment`, only the privacy checkbox is checked to allow "joining", then registering volunteers or launching `ACTION_JOIN_VOLUNTEER`. Evidence: [Condition:207][quake-consent].

**Judgment: The process of joining volunteer monitoring realizes the uploading of the location and sensor data of the associated device.**

## 8. Local content access and burying points

### 8.1 Notification monitoring can be authorized by the system capability itself

`BubblesNotificationListenerService.configNotificationListener()` When the device supports bubble notifications and has not yet obtained access, call:

```java
notificationManager.setNotificationListenerAccessGranted(componentName, true);
```

Components are then enabled/disabled according to their support status. The initial value of this service in the manifest is `enabled=false`.

The received `StatusBarNotification` is forwarded to the bubble UI Binder. Evidence: [Authorization configuration:16][notification-grant], [Object forward BubblesNotificationHelper.java:51][notification-forward].

On **supported devices, this component configures notification access rights through the system interface.**

### 8.2 Accessibility service actually reads QQ/WeChat notification text

`AntiMsgAccessibilityService.onAccessibilityEvent()` only handles QQ/WeChat event type 64; `l(Notification)` reads `extras["android.text"]` or `tickerText` for keyword matching. Evidence: [Event filtering: 401] [accessibility-events], [Text reading: 253] [notification-text].


### 8.3 The OneTrack buried point is indeed connected, but the network switch also exists

`StatManager` initializes multiple business OneTrack instances, including the advertising event App ID, and calls `OneTrack.setAccessNetworkEnable(..., l1.b())` and `setUseSystemNetTrafficOnly()`. Evidence: [StatManager.java:41][analytics].

These instances are connected to the usage behavior statistics and advertising event pipeline.

## 9. Supplementary implementation and field correction

| Project | code facts |
|---|---|
| Privacy detection application scope | Enumerate `getInstalledPackages(0)` post-filter system application; `apps` serialize package name and version |
| FBO package list | Write local settings, `ta.d.e()` reflection calls the local machine `miui.fbo.FboManager.FBO_new_trigger()` |
| `imei` field of traffic API | `DeviceUtil.getAndroidId()` Calculate Android ID plus fixed business string MD5 |
| Public device ID summary | Domestic version, restricted attributes are turned off and device API is assembled when conditions such as success are met; international version uses another branch |
| Alarm SMS | Sensitive data blocks are first encrypted with RSA |
| EPSManager | Used for emergency call location reporting |
| DeviceCredentialProvider | `call()` Check caller identity, package name and signature whitelist |
| PrivacyCenterProvider | Make UID check for some methods |
| PrivacyAppsProvider | Manifest configuration `SYSTEM_PERMISSION_DECLARE`, permission level is `signatureOrSystem` |
| Network security configuration | Set trust anchors for miwifi domains and subdomains, and use HTTPS as the main upload endpoint. |
| Monthly report gating | The upload condition contains `ma.c.a()`, and this method returns `false` in this build. |

Supplementary evidence: [FBO entrusted ta/d.java:93][fbo], [device credential authentication: 102] [credential], [Privacy Provider method authentication: 38] [privacy-provider], [monthly report conditions: 149] [month-gate], [permanent false switch ma/c.java:5][month-false],[Manifest][manifest],[Network Security Configuration][network-xml].

[ad-request]: evidence/sources/com/miui/antivirus/result/f.java#L261
[net-params]: evidence/sources/eg/j.java#L542
[net-static]: evidence/sources/eg/j.java#L129
[fingerprint]: evidence/sources/q2/a.java#L18
[ad-state]: evidence/sources/xe/a.java#L19
[device-id]: evidence/sources/eg/c.java#L54
[oaid]: evidence/sources/y4/a.java#L28
[risk-entry]: evidence/sources/com/miui/permcenter/detection/PrivacyRiskDetectionActivity.java#L1114
[risk-start]: evidence/sources/com/miui/permcenter/detection/PrivacyRiskDetectionActivity.java#L920
[risk-task]: evidence/sources/ub/d.java#L46
[app-list]: evidence/sources/xc/h.java#L13
[app-payload]: evidence/sources/ac/c.java#L62
[app-send]: evidence/sources/ac/c.java#L90
[sim-caller]: evidence/sources/com/miui/networkassistant/ui/base/TrafficRelatedPreFragment.java#L116
[sim-payload]: evidence/sources/com/miui/networkassistant/webapi/WebApiAccessHelper.java#L67
[androidid-hash]: evidence/sources/com/miui/networkassistant/utils/DeviceUtil.java#L84
[sms-read]: evidence/sources/com/miui/networkassistant/service/TcSmsReportService.java#L54
[sms-body]: evidence/sources/com/miui/networkassistant/service/TcSmsReportService.java#L178
[sms-payload]: evidence/sources/com/miui/networkassistant/webapi/WebApiAccessHelper.java#L83
[sms-consent]: evidence/sources/com/miui/networkassistant/ui/fragment/TcSmsReportFragment.java#L416
[police-flow]: evidence/sources/com/miui/warningcenter/policeassist/PaService.java#L182
[police-fields]: evidence/sources/com/miui/warningcenter/policeassist/PaUtils.java#L104
[eps-fields]: evidence/sources/com/miui/policeassist/EPSManager.java#L49
[eps-toggle]: evidence/sources/com/miui/policeassist/EPSManager.java#L82
[police-endpoints]: evidence/sources/com/miui/warningcenter/policeassist/PaCommon.java#L6
[police-sms]: evidence/sources/com/miui/warningcenter/policeassist/PaUtils.java#L82
[quake-fields]: evidence/sources/com/miui/earthquakewarning/service/EarthquakeMonitorDataManager.java#L51
[quake-send]: evidence/sources/com/miui/earthquakewarning/service/EarthquakeMonitorUtils.java#L54
[quake-source]: evidence/sources/com/miui/earthquakewarning/service/EarthquakeWarningService.java#L113
[quake-consent]: evidence/sources/com/miui/earthquakewarning/ui/EarthquakeWarningMonitorOpenFragment.java#L207
[notification-grant]: evidence/sources/com/miui/bubbles/services/BubblesNotificationListenerService.java#L16
[notification-forward]: evidence/sources/com/miui/bubbles/services/BubblesNotificationHelper.java#L51
[accessibility-events]: evidence/sources/com/miui/gamebooster/gbservices/AntiMsgAccessibilityService.java#L401
[notification-text]: evidence/sources/com/miui/gamebooster/gbservices/AntiMsgAccessibilityService.java#L253
[analytics]: evidence/sources/com/miui/analytics/StatManager.java#L41
[fbo]: evidence/sources/ta/d.java#L93
[credential]: evidence/sources/com/miui/securitycenter/provider/DeviceCredentialProvider.java#L102
[privacy-provider]: evidence/sources/com/miui/permcenter/privacycenter/PrivacyCenterProvider.java#L38
[month-gate]: evidence/sources/com/miui/monthreport/c.java#L149
[month-false]: evidence/sources/ma/c.java#L5
[manifest]: evidence/manifest/AndroidManifest.xml
[network-xml]: evidence/manifest/res/xml/network_security_config.xml
