# GuardProvider 2.0.3 Privacy Review

This report covers `com.miui.guardprovider` version `2.0.3-20231124.0` (versionCode 209). The sample is 13,251,548 bytes and has SHA-256 `b0c02fa682b8ca82429b471113d0702042ac76234c85fdcd8efeb7c03d313024`. It is one of three preinstalled applications from the same source device. Device identity and personal runtime logs are excluded from the publication.

The findings below describe collection and transmission paths identified through static analysis.

**This version implements paths for uploading inventories of non-system apps, submitting Wi-Fi environment data, reporting scan events, and conditionally reading device identifiers. A persistent UUID can link successive inventories. OneTrack's IMEI and IMSI lists use MD5 digests.**

The `evidence/` directory contains selected decompiled files. Line numbers refer to those files. See [evidence-index.json](evidence-index.json) for original and published file hashes and original relative paths.

## Data flows

| Data | Source and processing | Destination | Conditions |
|---|---|---|---|
| Non-system app package names, version numbers, signature MD5 digests, and URL-encoded app names | Calls `PackageManager.getInstalledPackages(0)`, filters out `FLAG_SYSTEM`, and serializes the `content` array | `https://flash.sec.miui.com/detect/app` | Anti-fraud or risky-app detection path. Checks network permission before sending. Covers the non-system apps returned by package enumeration. |
| Inventory correlation identifier and environment | Generates a random UUID once and stores it as `key_uuid` for reuse. Includes the incremental OS build, a timestamp in seconds, and `biz_id=virus_scan`. | Same request as the inventory | The persistent UUID can link successive requests. |
| Current Wi-Fi SSID, BSSID, and DHCP DNS1/DNS2 | Reads from the Wi-Fi manager and DHCP data and populates the `a1/d1` request object | Tencent TMSDK command 794 for cloud DNS and phishing detection | The DNSANDPHISHING branch runs with Wi-Fi connected and the fields available. |
| IMEI and IMSI | OneTrack reads the raw identifiers. For the daily active user (DAU) event, it hashes each list entry with MD5 before adding `imeis` and `imsis`. | OneTrack telemetry channel. The default domestic endpoint is `https://tracking.miui.com/track/v4`. | The default configuration permits reads, subject to region checks, permissions, and network settings. |
| Android ID and phone model | The host passes these values to `TMSDKContext.setDeviceInfo()` | Tencent SDK | Tencent engine initialization |
| Scanned app fingerprints, installer, and threat result | `MiEngineNG` telemetry includes the package, version, manifest and certificate fingerprints, installer, and threat name. | OneTrack. Cloud scanning uses a separate Xiaomi antivirus engine API. | Triggered by the scan scenario and result |

## 1. Application inventory upload

```text
j0.c.a()/h()
  → g(context): getInstalledPackages(0), filter out system apps
  → f(context,pkg): signature certificate MD5
  → b(list): {timestamp,os,biz_id,uuid,content:[{pkg,version,signature,appname}]}
  → h0.b.d(..., https://flash.sec.miui.com/detect/app, ...)
      → GuardApplication.e(context): network permission check
      → HttpURLConnection POST JSON
```

See [j0/c.java, lines 57 and 133](evidence/java/j0/c.java), [UUID persistence in w0/b.java, line 19](evidence/java/w0/b.java), and [transmission in h0/b.java, line 149](evidence/java/h0/b.java). The signature digest comes from the certificate bytes.

The detection flow invokes this path and checks network permission before sending. Including the persistent UUID with the inventory allows the recipient to link successive inventories. App names, package names, and versions can reveal the user's software choices.

## 2. Cloud Wi-Fi checks submit network environment data

The DNSANDPHISHING branch in `TencentEngine` calls `detectDnsAndPhishing()`. In `tmsdkobf.p9`, the code reads connection details and DHCP DNS addresses, sets `d1.f9072b=ssid` and `d1.f9073c=bssid`, populates the DNS string list, and calls `x5VarF.a(794, a1Var, ...)`.

See [TencentEngine.java, line 122](evidence/java/com/miui/guardprovider/engine/tencent/TencentEngine.java) and [p9.java, lines 200–256](evidence/java/tmsdkobf/p9.java).

The SDK's network channel receives the detection request. SSID and BSSID can identify a network and provide location clues. The DNS addresses reveal its name-resolution configuration.

## 3. OneTrack reads identifiers and hashes IMEI and IMSI list entries with MD5

The host configuration in [z/b.java, line 22](evidence/java/z/b.java) leaves IMEI and IMSI collection enabled by default and explicitly disables GAID. See [Configuration.java](evidence/java/com/xiaomi/onetrack/Configuration.java) for the defaults. DAU event construction in [api/c.java, lines 109–115](evidence/java/com/xiaomi/onetrack/api/c.java) checks the region state before calling `DeviceUtil.q()` and `DeviceUtil.v()`.

- Starting at [DeviceUtil.java, line 609](evidence/java/com/xiaomi/onetrack/util/DeviceUtil.java), `q()` calls `com.xiaomi.onetrack.d.d.c()` for each raw IMEI.
- Starting at line 724 of the same file, `v()` calls `d.h()` for each IMSI.
- [d/d.java, lines 34, 68, and 87](evidence/java/com/xiaomi/onetrack/d/d.java) shows the corresponding MD5 calculations.

The DAU lists use deterministic MD5 digests. Repeated occurrences of the same identifier produce the same digest and can be linked.

If `q.a() ? q.h() : configuration.isInternational()` evaluates to `true`, the code skips both lists. Reading identifiers also requires the relevant permissions. See [z/b.java, line 39](evidence/java/z/b.java) for OneTrack network permission and [util/x.java, lines 35 and 129](evidence/java/com/xiaomi/onetrack/util/x.java) for the default domestic telemetry domain and its regional and configuration overrides.

## 4. Network permission checks

The domestic branch of [GuardApplication.e(), lines 38–48](evidence/java/com/miui/guardprovider/GuardApplication.java) reads `persist.sys.sc_allow_conn`, which defaults to `false`. The international branch checks whether Security Center is installed. The `jp_kd` case also checks `antivirus_engine_network_enable`.

## 5. Scan events and SDK inputs

[MiEngineNG.java, line 201](evidence/java/com/miui/guardprovider/engine/mi/MiEngineNG.java) constructs a scan result event with `packageName/versionCode/versionName/mfSha256/signaturesMd5/signaturesSha256/installer/threatName`. This describes app use, installation sources, and security outcomes and can potentially be linked to telemetry identifiers.

During initialization, [TencentEngine.java, line 402](evidence/java/com/miui/guardprovider/engine/tencent/TencentEngine.java) passes Android ID and `Build.MODEL` to the Tencent SDK.

See [p0/f.java](evidence/java/p0/f.java) and [q0/d.java](evidence/java/q0/d.java) for the cloud scan payload, which includes app metadata and fingerprints. [AndroidManifest.xml](evidence/AndroidManifest.xml) lists the app's components and declared permissions.

## Corrections to earlier analysis and reading order

This report summarizes collected fields, transmission paths, and conditions. The [sanitized original report](SOURCE_REPORT.md) and [archived analyses](archival/) document the investigation. The main corrections are listed below.

| Topic | Reviewed finding |
|---|---|
| OneTrack DAU identifiers | `DeviceUtil.q()` hashes each IMEI with MD5. The IMSI list also uses MD5. |
| Identifier collection conditions | Region checks, permissions, network permission, and feature-specific triggers all apply. |
| Network checks by build | The domestic branch checks the system network property. The Security Center installation check is in the international branch. |
| Application inventory scope and fields | Non-system apps returned by package enumeration, with package name, version, signature MD5, and app name. |
| Cloud scan payload | App metadata and fingerprints |
