# Selected Static Evidence

These are excerpts from the decompiled sample identified in [REPORT.md](REPORT.md). Source paths are relative to a decompilation root, not an analyst workstation. Each numbered line retains its original source line number. Decompiled syntax and comments are tool output; excerpts may not compile. Gaps between ranges are intentional. Embedded credential/key values and personal contact details are omitted where encountered. No runtime output is included.

## A01

Source: `sources/a/b/a/e/h0/b.java`

Source-file SHA-256: `ebd3e0899c0e959e772026ff578e242396c5ca67f497471ed19560146b6d4285`

Original lines 182–230:

```text
0182 |     public static String h(Context context, boolean z) {
0183 |         String str;
0184 |         JSONObject jSONObject = new JSONObject();
0185 |         try {
0186 |             String strW = a.b.a.e.l0.c.w(context);
0187 |             if (TextUtils.isEmpty(a.b.a.e.l0.c.f565b)) {
0188 |                 a.b.a.e.l0.c.K(context);
0189 |                 if (TextUtils.isEmpty(a.b.a.e.l0.c.f565b)) {
0190 |                     str = "";
0191 |                 } else {
0192 |                     str = a.b.a.e.l0.c.f565b;
0193 |                 }
0194 |             } else {
0195 |                 str = a.b.a.e.l0.c.f565b;
0196 |             }
0197 |             jSONObject.put("imei1", strW);
0198 |             jSONObject.put("imei2", str);
0199 |             jSONObject.put("sn", a.b.a.e.l0.c.x());
0200 |             ArrayList arrayList = (ArrayList) a.b.a.e.l0.c.o(context);
0201 |             if (arrayList.size() >= 1) {
0202 |                 jSONObject.put("i1", arrayList.get(0));
0203 |             }
0204 |             if (arrayList.size() >= 2) {
0205 |                 jSONObject.put("i2", arrayList.get(1));
0206 |             }
0207 |             if (!a.b.a.e.l0.r.q()) {
0208 |                 Account accountD = a.b.a.e.l0.c.D(context);
0209 |                 jSONObject.put("mid", accountD != null ? accountD.name : "");
0210 |             }
0211 |         } catch (Exception e) {
0212 |             a.b.a.e.l0.p.d("ActiveEvent", "getTZContentJson e: ", e);
0213 |             a.b.a.d.b.d dVarV = a.b.a.a.a.c.a.V(z);
0214 |             if (dVarV != null) {
0215 |                 dVarV.a(Level.DEBUG, "##XLogger##, TAG: ", "ActiveEvent: getTZContentJson e: ", e);
0216 |             }
0217 |         }
0218 |         return jSONObject.toString();
0219 |     }
0220 | 
0221 |     /* JADX WARN: Code duplicated, block: B:101:0x0294 A[Catch: Exception -> 0x026a, TRY_LEAVE, TryCatch #2 {Exception -> 0x026a, blocks: (B:88:0x022d, B:104:0x02b7, B:91:0x0246, B:93:0x025f, B:98:0x0270, B:100:0x0289, B:101:0x0294, B:103:0x02ad), top: B:352:0x0227 }] */
0222 |     /* JADX WARN: Code duplicated, block: B:112:0x02ca  */
0223 |     /* JADX WARN: Code duplicated, block: B:114:0x02d7  */
0224 |     /* JADX WARN: Code duplicated, block: B:115:0x02db  */
0225 |     /* JADX WARN: Code duplicated, block: B:117:0x02e4  */
0226 |     /* JADX WARN: Code duplicated, block: B:120:0x0309 A[PHI: r16 r26
0227 |   0x0309: PHI (r16v5 java.lang.String) = (r16v2 java.lang.String), (r16v6 java.lang.String) binds: [B:111:0x02c4, B:119:0x0307] A[DONT_GENERATE, DONT_INLINE]
0228 |   0x0309: PHI (r26v7 long) = (r26v1 long), (r26v8 long) binds: [B:111:0x02c4, B:119:0x0307] A[DONT_GENERATE, DONT_INLINE]] */
0229 |     /* JADX WARN: Code duplicated, block: B:121:0x030b  */
0230 |     /* JADX WARN: Code duplicated, block: B:123:0x0311  */
```

## A02

Source: `sources/a/b/a/e/h0/b.java`

Source-file SHA-256: `ebd3e0899c0e959e772026ff578e242396c5ca67f497471ed19560146b6d4285`

Original lines 1950–2100:

```text
1950 |                 if (fFloor2 > 921.6d) {
1951 |                     fFloor2 = ((float) Math.floor((fFloor2 / 1024.0f) * 10.0f)) / 10.0f;
1952 |                     str15 = "TB";
1953 |                 } else {
1954 |                     str15 = str2;
1955 |                 }
1956 |                 if (fFloor2 > 921.6d) {
1957 |                     fFloor2 /= 1000.0f;
1958 |                     str15 = "PB";
1959 |                 }
1960 |                 a.b.a.e.l0.p.c("StorageUtil", "suffix: " + str15 + ", result: " + fFloor2);
1961 |                 str10 = String.format(locale, "%s%s", String.format(locale, "%.0f", Float.valueOf(fFloor2)), str15);
1962 |             }
1963 |         }
1964 |         jSONObject.put("rom", str10);
1965 |         jSONObject.put("ui_ver", a.b.a.a.a.c.a.F("ro.miui.ui.version.name", ""));
1966 |         jSONObject.put("mi_os_ver", a.b.a.a.a.c.a.F("ro.mi.os.version.name", ""));
1967 |         Class cls5 = a.b.a.e.l0.r.f609a;
1968 |         jSONObject.put("android_ver_int", Build.VERSION.SDK_INT);
1969 |         jSONObject.put("uep", a.b.a.e.l0.r.v(contextB));
1970 |         jSONObject.put("sign", strSubstring);
1971 |         jSONObject.put("cust_variant", a.b.a.a.a.c.a.G("ro.miui.cust_variant", ""));
1972 |         jSONObject.put("intl", a.b.a.e.l0.r.q());
1973 |         jSONObject.put("desc", a.b.a.a.a.c.a.G("ro.build.description", ""));
1974 |         jSONObject.put("radio", a.b.a.e.l0.c.z());
1975 |         jSONObject.put("radio2", a.b.a.e.l0.c.A(1));
1976 |         try {
1977 |             Object objInvoke5 = Class.forName("android.content.pm.IPackageManager$Stub").getDeclaredMethod("asInterface", IBinder.class).invoke(null, (IBinder) Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class).invoke(null, "package"));
1978 |             zBooleanValue = ((Boolean) objInvoke5.getClass().getDeclaredMethod("isFirstBoot", new Class[0]).invoke(objInvoke5, new Object[0])).booleanValue();
1979 |             str13 = "is first boot:" + zBooleanValue;
1980 |             a.b.a.e.l0.p.s("ActiveEvent", str13);
1981 |             dVarV = a.b.a.a.a.c.a.V(z);
1982 |             if (dVarV != null) {
1983 |                 dVarV.a(Level.WARNING, "##XLogger##, TAG: ", "ActiveEvent: " + str13, null);
1984 |             }
1985 |         } catch (Exception e28) {
1986 |             a.b.a.a.a.c.a.s(z, "ActiveEvent", "isFirstBoot e: ", e28);
1987 |             zBooleanValue = false;
1988 |         }
1989 |         jSONObject.put("first_boot", zBooleanValue);
1990 |         accountD = a.b.a.e.l0.c.D(contextB);
1991 |         if (!a.b.a.e.l0.r.q()) {
1992 |             if (accountD != null) {
1993 |                 str12 = accountD.name;
1994 |             } else {
1995 |                 str12 = "";
1996 |             }
1997 |             if (!TextUtils.isEmpty(str12) && !a.b.a.e.l0.u.b("r_m", false)) {
1998 |                 a.b.a.e.l0.u.k("r_m", true);
1999 |             }
2000 |         }
2001 |         if (accountD == null) {
2002 |             i5 = 0;
2003 |         } else {
2004 |             i4 = 1;
2005 |             while (r1.hasNext()) {
2006 |                 key = entry.getKey();
2007 |                 iIntValue = entry.getValue().intValue();
2008 |                 if (j(accountD, key, z)) {
2009 |                     i4 |= iIntValue;
2010 |                 }
2011 |             }
2012 |             if (a.b.a.e.l0.r.o(contextB, "miui.cloud.finddevice.AccessFindDevice")) {
2013 |                 if (a.b.a.c.v.g.a(contextB)) {
2014 |                     i6 = i4 | 8;
2015 |                 } else {
2016 |                     i6 = i4;
2017 |                 }
2018 |             } else if (a.b.a.e.l0.r.o(contextB, "android.permission.AUTHENTICATE_ACCOUNTS") || !"true".equals(((AccountManager) contextB.getSystemService("account")).getUserData(accountD, "extra_find_device_enabled"))) {
2019 |                 i6 = i4;
2020 |             } else {
2021 |                 i6 = i4 | 8;
2022 |             }
2023 |             if (i(contextB, accountD, z)) {
2024 |                 i5 = i6 | 8192;
2025 |             } else {
2026 |                 i5 = i6;
2027 |             }
2028 |         }
2029 |         jSONObject.put("bind_stats", String.valueOf(i5));
2030 |         strA = a.b.a.e.l0.h.a("release_time");
2031 |         if (!TextUtils.isEmpty(strA)) {
2032 |             j4 = Long.parseLong(strA);
2033 |         }
2034 |         jSONObject.put("release_time", j4);
2035 |         jSONObject.put("first_conect_time", a.b.a.e.l0.u.e());
2036 |         jSONObject.put("lock_state", a.b.a.a.a.c.a.E("ro.secureboot.lockstate"));
2037 |         jSONObject.put("rootable", a.b.a.a.a.c.a.E("ro.debuggable"));
2038 |         if (a.b.a.a.a.c.a.G0(a.b.a.e.e.b(), Boolean.valueOf(z))) {
2039 |             zF = a.b.a.e.l0.u.f();
2040 |             a.b.a.a.a.c.a.m(z, "ActiveEvent", "isGetNonce: " + zF);
2041 |             if (!zF) {
2042 |                 String strC7 = a.b.a.e.g0.c.c(a.b.a.a.a.c.a.t(h(contextB, z).getBytes()));
2043 |                 e(jSONObject, strC7, a.b.a.a.a.c.a.P(a.b.a.e.e.b(), Boolean.valueOf(z)), 1, a.b.a.a.a.c.a.E0(a.b.a.e.g0.d.d(strC7), Boolean.valueOf(z)), z);
2044 |             }
2045 |         } else {
2046 |             jSONObject.put("tz_content", a.b.a.e.g0.c.c(a.b.a.a.a.c.a.t(h(contextB, z).getBytes())));
2047 |             jSONObject.put("tz_support", false);
2048 |             jSONObject.put("tz_cpuid", g(null, z));
2049 |         }
2050 |         if (!a.b.a.e.l0.r.q() && !a.b.a.e.l0.r.w("ActiveEvent")) {
2051 |             a.b.a.c.v.j jVarD6 = a.b.a.c.v.j.d(contextB);
2052 |             Objects.requireNonNull(jVarD6);
2053 |             string2 = a.b.a.c.v.j.a.a(jVarD6.f156a).toString();
2054 |             jSONObject.put("loc", string2);
2055 |         }
2056 |         jSONObject.put("allow_report_usage", a.b.a.e.l0.r.b(contextB));
2057 |         jSONObject.put("protocol_ver", 1);
2058 |         jSONObject.put("ot_activation", z);
2059 |         try {
2060 |             strG2 = a.b.a.a.a.c.a.G("ro.boot.hwlevel", "");
2061 |         } catch (Throwable th7) {
2062 |             a.b.a.e.l0.p.h(a.b.a.e.l0.p.a(str4), "getHWLevel e", th7);
2063 |             strG2 = "";
2064 |         }
2065 |         jSONObject.put("hwlevel", strG2);
2066 |         try {
2067 |             strG3 = a.b.a.a.a.c.a.G("ro.boot.hwc", "");
2068 |         } catch (Throwable th8) {
2069 |             a.b.a.e.l0.p.h(a.b.a.e.l0.p.a(str4), "getHWC e", th8);
2070 |             strG3 = "";
2071 |         }
2072 |         jSONObject.put("hwc", strG3);
2073 |         try {
2074 |             strG4 = a.b.a.a.a.c.a.G("ro.boot.vbmeta.device_state", "");
2075 |         } catch (Throwable th9) {
2076 |             a.b.a.e.l0.p.h(a.b.a.e.l0.p.a(str4), "getDeviceState e", th9);
2077 |             strG4 = "";
2078 |         }
2079 |         jSONObject.put("vbmeta_state", strG4);
2080 |         try {
2081 |             strG5 = a.b.a.a.a.c.a.G("ro.boot.vbmeta.digest", "");
2082 |         } catch (Throwable th10) {
2083 |             a.b.a.e.l0.p.h(a.b.a.e.l0.p.a(str4), "getDigest e", th10);
2084 |             strG5 = "";
2085 |         }
2086 |         jSONObject.put("vbmeta_digest", strG5);
2087 |         if (a.b.a.e.l0.r.q()) {
2088 |             jSONObject.put("custom_version", a.b.a.e.l0.c.e());
2089 |             jSONObject.put("pure_custom_carrier", a.b.a.a.a.c.a.E("ro.miui.customized.region"));
2090 |             jSONObject.put("cota_carrier", a.b.a.a.a.c.a.E("persist.sys.cota.carrier"));
2091 |             lVarK = a.b.a.a.a.c.a.K(contextB);
2092 |             if (lVarK != null) {
2093 |                 if (TextUtils.isEmpty(lVarK.f591a)) {
2094 |                     str11 = "";
2095 |                 } else {
2096 |                     str11 = lVarK.f591a;
2097 |                 }
2098 |                 jSONObject.put("g_c_i_ms", str11);
2099 |                 jSONObject.put("g_c_i_am", TextUtils.isEmpty(lVarK.f592b) ? "" : lVarK.f592b);
2100 |             }
```

## A03

Source: `sources/a/b/a/c/v/j.java`

Source-file SHA-256: `af890d42724ffcfb2179f6c90003c7575cb79773009f97f75f42ee404246c4cc`

Original lines 1–275:

```text
0001 | package a.b.a.c.v;
0002 | 
0003 | import a.b.a.e.l0.u;
0004 | import a.b.a.e.v;
0005 | import android.content.Context;
0006 | import android.location.Address;
0007 | import android.location.Location;
0008 | import android.net.wifi.WifiInfo;
0009 | import android.net.wifi.WifiManager;
0010 | import android.os.Build;
0011 | import android.telephony.CellLocation;
0012 | import android.telephony.TelephonyManager;
0013 | import android.telephony.cdma.CdmaCellLocation;
0014 | import android.telephony.gsm.GsmCellLocation;
0015 | import android.text.TextUtils;
0016 | import android.util.Log;
0017 | import java.util.ArrayList;
0018 | import java.util.List;
0019 | import java.util.Objects;
0020 | import org.json.JSONArray;
0021 | import org.json.JSONObject;
0022 | 
0023 | /* JADX INFO: loaded from: classes.dex */
0024 | public class j {
0025 | 
0026 |     /* JADX INFO: renamed from: d, reason: collision with root package name */
0027 |     public static volatile j f155d;
0028 | 
0029 |     /* JADX INFO: renamed from: a, reason: collision with root package name */
0030 |     public a f156a;
0031 | 
0032 |     /* JADX INFO: renamed from: b, reason: collision with root package name */
0033 |     public Context f157b;
0034 | 
0035 |     /* JADX INFO: renamed from: c, reason: collision with root package name */
0036 |     public long f158c = -1;
0037 | 
0038 |     public class a {
0039 |         public a() {
0040 |         }
0041 | 
0042 |         public static JSONObject a(a aVar) {
0043 |             Objects.requireNonNull(aVar);
0044 |             JSONObject jSONObject = new JSONObject();
0045 |             long jCurrentTimeMillis = System.currentTimeMillis();
0046 |             try {
0047 |                 v.k().o(j.this.f157b);
0048 |                 v.k().n(j.this.f157b);
0049 |                 String strC = aVar.c();
0050 |                 List<v.c> listH = v.k().h(j.this.f157b, false);
0051 |                 JSONArray jSONArray = new JSONArray();
0052 |                 boolean z = false;
0053 |                 for (v.c cVar : (ArrayList) listH) {
0054 |                     if (TextUtils.isEmpty(strC) && cVar.f) {
0055 |                         strC = aVar.b(cVar);
0056 |                         z = true;
0057 |                     }
0058 |                     jSONArray.put(aVar.b(cVar));
0059 |                 }
0060 |                 jSONObject.put("b", strC);
0061 |                 jSONObject.put("w", aVar.d());
0062 |                 jSONObject.put("s", jSONArray);
0063 |                 j jVar = j.this;
0064 |                 jSONObject.put("wl", j.a(jVar, jVar.f157b));
0065 |                 jSONObject.put("wlt", j.this.f158c);
0066 |                 if (!u.b("first_active_upload_position", false)) {
0067 |                     j jVar2 = j.this;
0068 |                     jSONObject.put("g", j.b(jVar2, jVar2.f157b));
0069 |                     u.j(true);
0070 |                 }
0071 |                 j jVar3 = j.this;
0072 |                 jSONObject.put("ss", j.c(jVar3, jVar3.f157b));
0073 |                 v.k().m(j.this.f157b);
0074 |                 if (!z && v.k().h != null && !TextUtils.isEmpty(strC)) {
0075 |                     v vVarK = v.k();
0076 |                     long j = v.k().h.f647b;
0077 |                     Objects.requireNonNull(vVarK);
0078 |                     if (j > 0 && Math.abs(System.currentTimeMillis() - j) < 60000) {
0079 |                         jSONObject.put("b", aVar.c() + "," + v.k().h.f646a);
0080 |                     }
0081 |                 }
0082 |             } catch (Exception e) {
0083 |                 a.b.a.e.l0.p.d(i.a("MC"), "get getCoarseMdsInfo exception: ", e);
0084 |             }
0085 |             i.b("MC", String.format("coarse %dms, %s", Long.valueOf(System.currentTimeMillis() - jCurrentTimeMillis), jSONObject.toString()));
0086 |             return jSONObject;
0087 |         }
0088 | 
0089 |         public final String b(v.c cVar) {
0090 |             return cVar.f640a + "," + cVar.f641b + "," + cVar.f642c + "," + cVar.f643d + "," + cVar.e;
0091 |         }
0092 | 
0093 |         public final String c() {
0094 |             if (!a.b.a.a.a.c.a.h0(j.this.f157b, "android.permission.ACCESS_COARSE_LOCATION")) {
0095 |                 Log.d(i.a("MC"), "no permission ACCESS_COARSE_LOCATION");
0096 |                 return "";
0097 |             }
0098 |             try {
0099 |                 TelephonyManager telephonyManager = (TelephonyManager) j.this.f157b.getSystemService("phone");
0100 |                 CellLocation cellLocation = telephonyManager.getCellLocation();
0101 |                 if (cellLocation == null) {
0102 |                     return "";
0103 |                 }
0104 |                 String networkOperator = telephonyManager.getNetworkOperator();
0105 |                 i.b("MC", "operator " + networkOperator);
0106 |                 return (TextUtils.isEmpty(networkOperator) || networkOperator.length() <= 3) ? "" : e(cellLocation, networkOperator);
0107 |             } catch (SecurityException e) {
0108 |                 StringBuilder sbC = a.a.a.a.a.c("get bs SecurityException:");
0109 |                 sbC.append(e.getMessage());
0110 |                 a.b.a.e.l0.p.c("MC", sbC.toString());
0111 |                 return "";
0112 |             } catch (Exception e2) {
0113 |                 Log.e(i.a("MC"), "get bs exception: ", e2);
0114 |                 return "";
0115 |             }
0116 |         }
0117 | 
0118 |         public final String d() {
0119 |             String strSubstring;
0120 |             StringBuilder sb = new StringBuilder();
0121 |             try {
0122 |                 WifiInfo connectionInfo = ((WifiManager) j.this.f157b.getSystemService("wifi")).getConnectionInfo();
0123 |                 if (connectionInfo != null && connectionInfo.getBSSID() != null && connectionInfo.getSSID() != null) {
0124 |                     String ssid = connectionInfo.getSSID();
0125 |                     int rssi = connectionInfo.getRssi();
0126 |                     sb.append(connectionInfo.getBSSID() + ",");
0127 |                     try {
0128 |                         strSubstring = ssid.substring(ssid.indexOf("\"") + 1, ssid.lastIndexOf("\""));
0129 |                     } catch (Exception e) {
0130 |                         i.b("MC", "getSSIDInfo exception " + e);
0131 |                         strSubstring = "";
0132 |                     }
0133 |                     sb.append(strSubstring);
0134 |                     sb.append("," + rssi);
0135 |                     if (Build.VERSION.SDK_INT >= 21) {
0136 |                         sb.append("," + connectionInfo.getFrequency());
0137 |                     }
0138 |                 }
0139 |             } catch (Exception e2) {
0140 |                 Log.e(i.a("MC"), "get wifi exception: ", e2);
0141 |             }
0142 |             return sb.toString();
0143 |         }
0144 | 
0145 |         public final String e(CellLocation cellLocation, String str) {
0146 |             int baseStationId;
0147 |             StringBuilder sb = new StringBuilder();
0148 |             if (cellLocation instanceof GsmCellLocation) {
0149 |                 GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
0150 |                 if (gsmCellLocation.getLac() != -1 && gsmCellLocation.getCid() != -1) {
0151 |                     sb.append(str.substring(0, 3));
0152 |                     sb.append(",");
0153 |                     sb.append(str.substring(3));
0154 |                     sb.append(",");
0155 |                     sb.append(gsmCellLocation.getLac());
0156 |                     sb.append(",");
0157 |                     baseStationId = gsmCellLocation.getCid();
0158 |                     sb.append(baseStationId);
0159 |                 }
0160 |             } else if (cellLocation instanceof CdmaCellLocation) {
0161 |                 CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
0162 |                 if (cdmaCellLocation.getSystemId() != -1 && cdmaCellLocation.getBaseStationId() != -1 && cdmaCellLocation.getNetworkId() != -1) {
0163 |                     sb.append(str.substring(0, 3));
0164 |                     sb.append(",");
0165 |                     sb.append(cdmaCellLocation.getSystemId());
0166 |                     sb.append(",");
0167 |                     sb.append(cdmaCellLocation.getNetworkId());
0168 |                     sb.append(",");
0169 |                     baseStationId = cdmaCellLocation.getBaseStationId();
0170 |                     sb.append(baseStationId);
0171 |                 }
0172 |             }
0173 |             return sb.toString();
0174 |         }
0175 |     }
0176 | 
0177 |     public j(Context context) {
0178 |         try {
0179 |             this.f157b = a.b.a.a.a.c.a.I(context);
0180 |             this.f156a = new a();
0181 |         } catch (Exception e) {
0182 |             Log.e(i.a("MC"), "MdsCollector exception:", e);
0183 |         }
0184 |     }
0185 | 
0186 |     public static JSONArray a(j jVar, Context context) {
0187 |         Objects.requireNonNull(jVar);
0188 |         JSONArray jSONArray = new JSONArray();
0189 |         try {
0190 |             List<v.f> listJ = v.k().j(context);
0191 |             if (listJ == null || listJ.isEmpty()) {
0192 |                 jVar.f158c = -1L;
0193 |             } else {
0194 |                 jVar.f158c = v.k().e;
0195 |                 for (v.f fVar : listJ) {
0196 |                     StringBuilder sb = new StringBuilder();
0197 |                     sb.append(fVar.f648a);
0198 |                     sb.append(",");
0199 |                     sb.append(fVar.f649b);
0200 |                     sb.append(",");
0201 |                     sb.append(fVar.f650c);
0202 |                     sb.append(",");
0203 |                     sb.append(fVar.f651d);
0204 |                     jSONArray.put(sb);
0205 |                 }
0206 |             }
0207 |         } catch (Exception e) {
0208 |             a.a.a.a.a.f(e, a.a.a.a.a.c("getWifiListInfo exception:"), "MC");
0209 |         }
0210 |         return jSONArray;
0211 |     }
0212 | 
0213 |     public static JSONArray b(j jVar, Context context) {
0214 |         Objects.requireNonNull(jVar);
0215 |         JSONArray jSONArray = new JSONArray();
0216 |         try {
0217 |             List<Location> listG = v.k().g(context, 5);
0218 |             if (listG != null && !listG.isEmpty()) {
0219 |                 for (Location location : listG) {
0220 |                     JSONObject jSONObject = new JSONObject();
0221 |                     jSONObject.put("loc_type", location.getProvider());
0222 |                     jSONObject.put("lat", location.getLatitude());
0223 |                     jSONObject.put("lng", location.getLongitude());
0224 |                     Address addressD = v.k().d(context, location.getLatitude(), location.getLongitude());
0225 |                     if (addressD != null) {
0226 |                         jSONObject.put("cc", addressD.getCountryCode());
0227 |                         jSONObject.put("cn", addressD.getCountryName());
0228 |                         jSONObject.put("aa", addressD.getAdminArea());
0229 |                         jSONObject.put("loc", addressD.getLocality());
0230 |                         jSONObject.put("sl", addressD.getSubLocality());
0231 |                         jSONObject.put("tho", addressD.getThoroughfare());
0232 |                     }
0233 |                     jSONObject.put("loc_t", location.getTime());
0234 |                     jSONArray.put(jSONObject);
0235 |                 }
0236 |             }
0237 |         } catch (Throwable th) {
0238 |             a.a.a.a.a.j(th, a.a.a.a.a.c("getLocationDataInfo error:"), "MC");
0239 |         }
0240 |         return jSONArray;
0241 |     }
0242 | 
0243 |     public static JSONObject c(j jVar, Context context) {
0244 |         boolean zIsWifiEnabled;
0245 |         Objects.requireNonNull(jVar);
0246 |         JSONObject jSONObject = new JSONObject();
0247 |         try {
0248 |             if (v.k().l(context)) {
0249 |                 jSONObject.put("gss", 1);
0250 |             } else {
0251 |                 jSONObject.put("gss", 0);
0252 |             }
0253 |             Objects.requireNonNull(v.k());
0254 |             try {
0255 |                 zIsWifiEnabled = ((WifiManager) context.getSystemService("wifi")).isWifiEnabled();
0256 |             } catch (Throwable unused) {
0257 |                 zIsWifiEnabled = false;
0258 |             }
0259 |             if (zIsWifiEnabled) {
0260 |                 jSONObject.put("wss", 1);
0261 |             } else {
0262 |                 jSONObject.put("wss", 0);
0263 |             }
0264 |         } catch (Throwable unused2) {
0265 |         }
0266 |         return jSONObject;
0267 |     }
0268 | 
0269 |     public static synchronized j d(Context context) {
0270 |         if (f155d == null) {
0271 |             f155d = new j(context);
0272 |         }
0273 |         return f155d;
0274 |     }
0275 | }
```

## A04

Source: `fallback/a/b/a/e/h0/m.java`

Source-file SHA-256: `606d8f6f96dab72c6b266ab8cd340668c32ebddd8fc4899980b35c34a302f2ce`

Original lines 120–293:

```text
0120 |     public static java.util.List<java.lang.String> e() {
0121 |             java.lang.String r0 = "InstalledAppEvent"
0122 |             java.util.ArrayList r1 = new java.util.ArrayList
0123 |             r1.<init>()
0124 |             android.content.Context r2 = a.b.a.e.e.b()     // Catch: java.lang.Throwable -> L2e java.lang.NullPointerException -> L35
0125 |             java.lang.String r3 = "input_method"
0126 |             java.lang.Object r2 = r2.getSystemService(r3)     // Catch: java.lang.Throwable -> L2e java.lang.NullPointerException -> L35
0127 |             android.view.inputmethod.InputMethodManager r2 = (android.view.inputmethod.InputMethodManager) r2     // Catch: java.lang.Throwable -> L2e java.lang.NullPointerException -> L35
0128 |             java.util.List r2 = r2.getEnabledInputMethodList()     // Catch: java.lang.Throwable -> L2e java.lang.NullPointerException -> L35
0129 |             int r3 = r2.size()     // Catch: java.lang.Throwable -> L2e java.lang.NullPointerException -> L35
0130 |             r4 = 0
0131 |         L1c:
0132 |             if (r4 >= r3) goto L3a
0133 |             java.lang.Object r5 = r2.get(r4)     // Catch: java.lang.Throwable -> L2e java.lang.NullPointerException -> L35
0134 |             android.view.inputmethod.InputMethodInfo r5 = (android.view.inputmethod.InputMethodInfo) r5     // Catch: java.lang.Throwable -> L2e java.lang.NullPointerException -> L35
0135 |             java.lang.String r5 = r5.getPackageName()     // Catch: java.lang.Throwable -> L2e java.lang.NullPointerException -> L35
0136 |             r1.add(r5)     // Catch: java.lang.Throwable -> L2e java.lang.NullPointerException -> L35
0137 |             int r4 = r4 + 1
0138 |             goto L1c
0139 |         L2e:
0140 |             r2 = move-exception
0141 |             java.lang.String r3 = "getImeList error"
0142 |             a.b.a.e.l0.p.h(r0, r3, r2)     // Catch: java.lang.Throwable -> L3b
0143 |             goto L3a
0144 |         L35:
0145 |             java.lang.String r2 = "null pointer while getting ime list"
0146 |             a.b.a.e.l0.p.g(r0, r2)     // Catch: java.lang.Throwable -> L3b
0147 |         L3a:
0148 |             return r1
0149 |         L3b:
0150 |             r0 = move-exception
0151 |             goto L3e
0152 |         L3d:
0153 |             throw r0
0154 |         L3e:
0155 |             goto L3d
0156 |     }
0157 | 
0158 |     public final org.json.JSONObject d() throws org.json.JSONException {
0159 |             r15 = this;
0160 |             org.json.JSONObject r0 = new org.json.JSONObject
0161 |             r0.<init>()
0162 |             org.json.JSONArray r1 = new org.json.JSONArray
0163 |             r1.<init>()
0164 |             android.content.Intent r2 = new android.content.Intent
0165 |             java.lang.String r3 = "android.intent.action.MAIN"
0166 |             r2.<init>(r3)
0167 |             java.lang.String r3 = "android.intent.category.LAUNCHER"
0168 |             r2.addCategory(r3)
0169 |             android.content.Context r3 = a.b.a.e.e.b()
0170 |             android.content.pm.PackageManager r4 = r3.getPackageManager()
0171 |             r5 = 0
0172 |             java.util.List r2 = r4.queryIntentActivities(r2, r5)
0173 |             java.util.HashSet r6 = new java.util.HashSet
0174 |             r6.<init>()
0175 |             java.util.Iterator r7 = r2.iterator()
0176 |         L2c:
0177 |             boolean r8 = r7.hasNext()
0178 |             if (r8 == 0) goto L40
0179 |             java.lang.Object r8 = r7.next()
0180 |             android.content.pm.ResolveInfo r8 = (android.content.pm.ResolveInfo) r8
0181 |             android.content.pm.ActivityInfo r8 = r8.activityInfo
0182 |             java.lang.String r8 = r8.packageName
0183 |             r6.add(r8)
0184 |             goto L2c
0185 |         L40:
0186 |             java.util.List r7 = e()
0187 |             r8 = 16384(0x4000, float:2.2959E-41)
0188 |             java.util.List r8 = r4.getInstalledPackages(r8)
0189 |             java.util.Iterator r9 = r8.iterator()
0190 |         L4e:
0191 |             boolean r10 = r9.hasNext()
0192 |             r11 = 1
0193 |             if (r10 == 0) goto Ldf
0194 |             java.lang.Object r10 = r9.next()
0195 |             android.content.pm.PackageInfo r10 = (android.content.pm.PackageInfo) r10
0196 |             if (r10 == 0) goto L4e
0197 |             java.lang.String r12 = r10.packageName
0198 |             if (r12 != 0) goto L62
0199 |             goto L4e
0200 |         L62:
0201 |             android.content.pm.ApplicationInfo r13 = r10.applicationInfo
0202 |             int r13 = r13.flags
0203 |             r13 = r13 & r11
0204 |             if (r13 == 0) goto L6a
0205 |             goto L6b
0206 |         L6a:
0207 |             r11 = 0
0208 |         L6b:
0209 |             boolean r12 = r6.contains(r12)
0210 |             if (r12 != 0) goto L88
0211 |             java.util.List<java.lang.String> r12 = a.b.a.e.h0.m.g
0212 |             java.lang.String r13 = r10.packageName
0213 |             boolean r12 = r12.contains(r13)
0214 |             if (r12 != 0) goto L88
0215 |             java.lang.String r12 = r10.packageName
0216 |             r13 = r7
0217 |             java.util.ArrayList r13 = (java.util.ArrayList) r13
0218 |             boolean r12 = r13.contains(r12)
0219 |             if (r12 != 0) goto L88
0220 |             if (r11 != 0) goto L4e
0221 |         L88:
0222 |             org.json.JSONObject r11 = new org.json.JSONObject
0223 |             r11.<init>()
0224 |             java.lang.String r12 = r10.packageName
0225 |             java.lang.String r13 = "pkg"
0226 |             r11.put(r13, r12)
0227 |             java.lang.String r12 = r10.versionName
0228 |             java.lang.String r13 = ""
0229 |             if (r12 != 0) goto L9b
0230 |             r12 = r13
0231 |         L9b:
0232 |             java.lang.String r14 = "vn"
0233 |             r11.put(r14, r12)
0234 |             int r12 = r10.versionCode
0235 |             java.lang.String r14 = "vc"
0236 |             r11.put(r14, r12)
0237 |             java.lang.String r12 = r10.packageName
0238 |             java.lang.String r12 = a.b.a.a.a.c.a.S(r4, r12)
0239 |             if (r12 != 0) goto Lb0
0240 |             goto Lb1
0241 |         Lb0:
0242 |             r13 = r12
0243 |         Lb1:
0244 |             java.lang.String r12 = "installer"
0245 |             r11.put(r12, r13)
0246 |             long r12 = r10.firstInstallTime
0247 |             java.lang.String r14 = "fit"
0248 |             r11.put(r14, r12)
0249 |             long r12 = r10.lastUpdateTime
0250 |             java.lang.String r14 = "lut"
0251 |             r11.put(r14, r12)
0252 |             java.lang.String r12 = r10.packageName
0253 |             int r12 = a.b.a.e.l0.r.d(r3, r12)
0254 |             java.lang.String r13 = "status"
0255 |             r11.put(r13, r12)
0256 |             java.lang.String r10 = r10.packageName
0257 |             boolean r10 = a.b.a.e.l0.r.c(r3, r10)
0258 |             java.lang.String r12 = "ot_enable"
0259 |             r11.put(r12, r10)
0260 |             r1.put(r11)
0261 |             goto L4e
0262 |         Ldf:
0263 |             r2.clear()
0264 |             r6.clear()
0265 |             java.util.ArrayList r7 = (java.util.ArrayList) r7
0266 |             r7.clear()
0267 |             r8.clear()
0268 |             java.lang.StringBuilder r2 = new java.lang.StringBuilder
0269 |             r2.<init>()
0270 |             java.lang.String r3 = "getInstalledApps size: "
0271 |             r2.append(r3)
0272 |             int r3 = r1.length()
0273 |             r2.append(r3)
0274 |             java.lang.String r2 = r2.toString()
0275 |             java.lang.String r3 = "InstalledAppEvent"
0276 |             a.b.a.e.l0.p.c(r3, r2)
0277 |             java.lang.String r2 = "apps"
0278 |             r0.put(r2, r1)
0279 |             java.lang.String r1 = a.b.a.e.l0.c.z()
0280 |             java.lang.String r2 = "radio"
0281 |             r0.put(r2, r1)
0282 |             java.lang.String r1 = a.b.a.e.l0.c.A(r11)
0283 |             java.lang.String r2 = "radio2"
0284 |             r0.put(r2, r1)
0285 |             boolean r1 = a.b.a.e.l0.r.q()
0286 |             if (r1 == 0) goto L12d
0287 |             java.lang.String r1 = a.b.a.e.l0.c.e()
0288 |             java.lang.String r2 = "custom_version"
0289 |             r0.put(r2, r1)
0290 |         L12d:
0291 |             return r0
0292 |     }
0293 | }
```

## A05

Source: `sources/a/b/a/e/h0/y.java`

Source-file SHA-256: `872cbc28487cd46874fee446f9a31a7552831c2e6e69d3ded3016f3d0e31a1a0`

Original lines 1–400:

```text
0001 | package a.b.a.e.h0;
0002 | 
0003 | import android.annotation.TargetApi;
0004 | import android.app.usage.UsageEvents;
0005 | import android.app.usage.UsageStatsManager;
0006 | import android.content.Context;
0007 | import android.content.SharedPreferences;
0008 | import android.os.Build;
0009 | import android.os.IBinder;
0010 | import android.os.PersistableBundle;
0011 | import android.text.TextUtils;
0012 | import android.util.Pair;
0013 | import java.lang.reflect.Field;
0014 | import java.util.ArrayList;
0015 | import java.util.Collections;
0016 | import java.util.HashMap;
0017 | import java.util.Iterator;
0018 | import java.util.List;
0019 | import java.util.Map;
0020 | import org.json.JSONArray;
0021 | import org.json.JSONException;
0022 | import org.json.JSONObject;
0023 | 
0024 | /* JADX INFO: loaded from: classes.dex */
0025 | public class y extends a.b.a.e.f {
0026 |     public static boolean h = true;
0027 |     public static Map<String, Long> i = new HashMap();
0028 |     public int g = 0;
0029 | 
0030 |     public static class a {
0031 | 
0032 |         /* JADX INFO: renamed from: a, reason: collision with root package name */
0033 |         public String f460a;
0034 | 
0035 |         /* JADX INFO: renamed from: b, reason: collision with root package name */
0036 |         public int f461b;
0037 | 
0038 |         /* JADX INFO: renamed from: c, reason: collision with root package name */
0039 |         public long f462c;
0040 | 
0041 |         public a(String str, int i, long j) {
0042 |             this.f460a = str;
0043 |             this.f461b = i;
0044 |             this.f462c = j;
0045 |         }
0046 |     }
0047 | 
0048 |     public static class b {
0049 | 
0050 |         /* JADX INFO: renamed from: a, reason: collision with root package name */
0051 |         public String f463a;
0052 | 
0053 |         /* JADX INFO: renamed from: b, reason: collision with root package name */
0054 |         public String f464b;
0055 | 
0056 |         /* JADX INFO: renamed from: c, reason: collision with root package name */
0057 |         public int f465c;
0058 | 
0059 |         /* JADX INFO: renamed from: d, reason: collision with root package name */
0060 |         public long f466d;
0061 | 
0062 |         public b(String str, String str2, int i, long j) {
0063 |             this.f463a = str;
0064 |             this.f464b = str2;
0065 |             this.f465c = i;
0066 |             this.f466d = j;
0067 |         }
0068 |     }
0069 | 
0070 |     public static class c {
0071 | 
0072 |         /* JADX INFO: renamed from: a, reason: collision with root package name */
0073 |         public static SharedPreferences f467a;
0074 | 
0075 |         /* JADX INFO: renamed from: b, reason: collision with root package name */
0076 |         public static SharedPreferences.Editor f468b;
0077 | 
0078 |         public static void a() {
0079 |             if (f468b != null) {
0080 |                 return;
0081 |             }
0082 |             synchronized (c.class) {
0083 |                 if (f468b == null) {
0084 |                     SharedPreferences sharedPreferences = a.b.a.e.e.b().getSharedPreferences("one_track_pref_kk_usage_stats", 0);
0085 |                     f467a = sharedPreferences;
0086 |                     f468b = sharedPreferences.edit();
0087 |                 }
0088 |             }
0089 |         }
0090 |     }
0091 | 
0092 |     public y(long j, long j2, boolean z) {
0093 |         JSONObject jSONObject;
0094 |         this.f309a = "001";
0095 |         this.f311c = "onetrack_usage";
0096 |         this.f310b = a.b.a.e.e.f264d;
0097 |         this.f312d = 1;
0098 |         try {
0099 |             jSONObject = new JSONObject();
0100 |             JSONObject jSONObjectA = a();
0101 |             JSONObject jSONObjectG = g(j, j2, z);
0102 |             if (jSONObjectG != null) {
0103 |                 jSONObject.put("H", jSONObjectA);
0104 |                 jSONObject.put("B", jSONObjectG);
0105 |             } else {
0106 |                 jSONObject = null;
0107 |             }
0108 |         } catch (Exception e) {
0109 |             a.b.a.e.l0.p.h("UsageEvent", "create usage event error", e);
0110 |         }
0111 |         this.e = jSONObject;
0112 |         this.f = System.currentTimeMillis();
0113 |     }
0114 | 
0115 |     public static Map<String, Pair<Long, Integer>> l() {
0116 |         String string;
0117 |         if (a.b.a.e.l0.r.p()) {
0118 |             c.a();
0119 |             string = c.f467a.getString("today_usage_till_now", null);
0120 |         } else {
0121 |             string = null;
0122 |         }
0123 |         a.b.a.e.l0.p.c("UsageEvent", "get from syspref, prev usage=" + string);
0124 |         if (TextUtils.isEmpty(string)) {
0125 |             return null;
0126 |         }
0127 |         try {
0128 |             JSONObject jSONObject = new JSONObject(string);
0129 |             long j = jSONObject.getLong("last_time");
0130 |             if (!a.b.a.e.l0.v.g(j)) {
0131 |                 a.b.a.e.l0.p.c("UsageEvent", "lastTime: " + j + ". not in today");
0132 |                 return null;
0133 |             }
0134 |             JSONArray jSONArray = jSONObject.getJSONArray("prev_usg");
0135 |             HashMap map = new HashMap();
0136 |             for (int i2 = 0; i2 < jSONArray.length(); i2++) {
0137 |                 JSONObject jSONObject2 = jSONArray.getJSONObject(i2);
0138 |                 map.put(jSONObject2.getString("pkg"), new Pair(Long.valueOf(jSONObject2.getLong("duration")), Integer.valueOf(jSONObject2.getInt("launch_cnt"))));
0139 |             }
0140 |             return map;
0141 |         } catch (Exception e) {
0142 |             a.a.a.a.a.h("get prev usage exception", e, "UsageEvent");
0143 |             return null;
0144 |         }
0145 |     }
0146 | 
0147 |     public final JSONObject d(List<b> list) throws JSONException {
0148 |         boolean z = false;
0149 |         if (list != null && list.size() != 0) {
0150 |             Iterator<b> it = list.iterator();
0151 |             boolean z2 = false;
0152 |             boolean z3 = false;
0153 |             boolean z4 = false;
0154 |             while (it.hasNext()) {
0155 |                 int i2 = it.next().f465c;
0156 |                 if (i2 == 1) {
0157 |                     z2 = true;
0158 |                 } else if (i2 == 2) {
0159 |                     z3 = true;
0160 |                 } else if (i2 == 3) {
0161 |                     z4 = true;
0162 |                 }
0163 |             }
0164 |             if (z2 && z3 && z4) {
0165 |                 z = true;
0166 |             }
0167 |         }
0168 |         String str = null;
0169 |         if (!z) {
0170 |             return null;
0171 |         }
0172 |         long j = 0;
0173 |         long j2 = 0;
0174 |         for (b bVar : list) {
0175 |             int i3 = bVar.f465c;
0176 |             if (i3 == 2) {
0177 |                 j = bVar.f466d;
0178 |             } else if (i3 == 3) {
0179 |                 j2 = bVar.f466d;
0180 |                 str = bVar.f463a;
0181 |             }
0182 |         }
0183 |         long j3 = j - j2;
0184 |         JSONObject jSONObject = new JSONObject();
0185 |         jSONObject.put("event", 10004);
0186 |         jSONObject.put("pkg", str);
0187 |         jSONObject.put("ts", j2);
0188 |         jSONObject.put("duration", j3 > 0 ? j3 : 0L);
0189 |         return jSONObject;
0190 |     }
0191 | 
0192 |     public final JSONObject e(int i2, String str, String str2, long j, long j2) throws JSONException {
0193 |         JSONObject jSONObject = new JSONObject();
0194 |         jSONObject.put("event", i2);
0195 |         jSONObject.put("pkg", str);
0196 |         if (str2 != null) {
0197 |             jSONObject.put("class", str2);
0198 |         }
0199 |         jSONObject.put("ts", j);
0200 |         if (j2 < 0 || j2 > 86400000) {
0201 |             j2 = 0;
0202 |         }
0203 |         jSONObject.put("duration", j2);
0204 |         return jSONObject;
0205 |     }
0206 | 
0207 |     public final JSONObject f(int i2, String str, String str2, long j, long j2) throws JSONException {
0208 |         JSONObject jSONObject = new JSONObject();
0209 |         jSONObject.put("event", i2);
0210 |         jSONObject.put("pkg", str);
0211 |         if (str2 != null) {
0212 |             jSONObject.put("class", str2);
0213 |         }
0214 |         jSONObject.put("ts", j);
0215 |         String strA = a.b.a.e.d0.a.e.f256a.a(j, str);
0216 |         if (strA == null) {
0217 |             strA = "";
0218 |         }
0219 |         jSONObject.put("vn", strA);
0220 |         if (i2 == 2 || i2 == 10002) {
0221 |             if (j2 < 0 || j2 > 86400000) {
0222 |                 j2 = 0;
0223 |             }
0224 |             jSONObject.put("duration", j2);
0225 |         }
0226 |         return jSONObject;
0227 |     }
0228 | 
0229 |     public JSONObject g(long j, long j2, boolean z) throws JSONException {
0230 |         JSONObject jSONObject;
0231 |         JSONArray jSONArrayU;
0232 |         Map<String, Pair<Long, Integer>> map;
0233 |         Field field;
0234 |         Field field2;
0235 |         String str;
0236 |         JSONObject jSONObject2 = new JSONObject();
0237 |         int i2 = Build.VERSION.SDK_INT;
0238 |         if (i2 > 19) {
0239 |             jSONArrayU = u(j, j2, z);
0240 |             jSONObject = jSONObject2;
0241 |         } else {
0242 |             if (i2 != 19) {
0243 |                 a.b.a.e.l0.p.c("UsageEvent", "Don't collect usage below Android KK 4.4");
0244 |                 return jSONObject2;
0245 |             }
0246 |             StringBuilder sbD = a.a.a.a.a.d("queryToadyUsageStatsForKK enter, begin=", j, ", end=");
0247 |             sbD.append(j2);
0248 |             a.b.a.e.l0.p.c("UsageEvent", sbD.toString());
0249 |             try {
0250 |                 Object[] objArr = (Object[]) Class.forName("com.android.internal.app.IUsageStats").getDeclaredMethod("getAllPkgUsageStats", new Class[0]).invoke(Class.forName("com.android.internal.app.IUsageStats$Stub").getMethod("asInterface", IBinder.class).invoke(null, Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class).invoke(null, "usagestats")), new Object[0]);
0251 |                 try {
0252 |                     if (objArr == null || objArr.length == 0) {
0253 |                         jSONObject = jSONObject2;
0254 |                         a.b.a.e.l0.p.c("UsageEvent", "will return, stats=" + objArr);
0255 |                         jSONArrayU = null;
0256 |                     } else {
0257 |                         Class<?> cls = Class.forName("com.android.internal.os.PkgUsageStats");
0258 |                         Field declaredField = cls.getDeclaredField("packageName");
0259 |                         declaredField.setAccessible(true);
0260 |                         Field declaredField2 = cls.getDeclaredField("usageTime");
0261 |                         declaredField2.setAccessible(true);
0262 |                         Field declaredField3 = cls.getDeclaredField("launchCount");
0263 |                         declaredField3.setAccessible(true);
0264 |                         Map<String, Pair<Long, Integer>> mapL = l();
0265 |                         JSONArray jSONArray = new JSONArray();
0266 |                         JSONArray jSONArray2 = new JSONArray();
0267 |                         int length = objArr.length;
0268 |                         int i3 = 0;
0269 |                         int i4 = 0;
0270 |                         while (true) {
0271 |                             if (i3 >= length) {
0272 |                                 jSONObject = jSONObject2;
0273 |                                 break;
0274 |                             }
0275 |                             int i5 = length;
0276 |                             Object obj = objArr[i3];
0277 |                             long j3 = declaredField2.getLong(obj);
0278 |                             Field field3 = declaredField2;
0279 |                             int i6 = declaredField3.getInt(obj);
0280 |                             if (i6 <= 0 || j3 <= 0) {
0281 |                                 jSONObject = jSONObject2;
0282 |                                 map = mapL;
0283 |                                 field = declaredField3;
0284 |                                 field2 = declaredField;
0285 |                                 str = "launch cnt=" + i6 + ", timeFg=" + j3;
0286 |                             } else {
0287 |                                 String str2 = (String) declaredField.get(obj);
0288 |                                 field = declaredField3;
0289 |                                 StringBuilder sb = new StringBuilder();
0290 |                                 field2 = declaredField;
0291 |                                 sb.append("get from system, pkg=");
0292 |                                 sb.append(str2);
0293 |                                 sb.append(", fgTime=");
0294 |                                 sb.append(j3);
0295 |                                 sb.append(", launch=");
0296 |                                 sb.append(i6);
0297 |                                 a.b.a.e.l0.p.c("UsageEvent", sb.toString());
0298 |                                 JSONObject jSONObject3 = new JSONObject();
0299 |                                 jSONObject3.put("pkg", str2);
0300 |                                 jSONObject3.put("duration", j3);
0301 |                                 jSONObject3.put("launch_cnt", i6);
0302 |                                 jSONArray.put(jSONObject3);
0303 |                                 JSONObject jSONObject4 = new JSONObject();
0304 |                                 if (mapL == null || !mapL.containsKey(str2)) {
0305 |                                     jSONObject = jSONObject2;
0306 |                                     map = mapL;
0307 |                                     jSONObject4.put("duration", j3);
0308 |                                     jSONObject4.put("launch_cnt", i6);
0309 |                                 } else {
0310 |                                     map = mapL;
0311 |                                     Pair<Long, Integer> pair = mapL.get(str2);
0312 |                                     jSONObject = jSONObject2;
0313 |                                     long jLongValue = ((Long) pair.first).longValue();
0314 |                                     int iIntValue = ((Integer) pair.second).intValue();
0315 |                                     long j4 = j3 - jLongValue;
0316 |                                     if (j4 > 0 || i6 - iIntValue > 0) {
0317 |                                         jSONObject4.put("duration", j4);
0318 |                                         jSONObject4.put("launch_cnt", i6 - iIntValue);
0319 |                                     } else {
0320 |                                         str = "app not used since last update time";
0321 |                                     }
0322 |                                 }
0323 |                                 jSONObject4.put("event", 10006);
0324 |                                 jSONObject4.put("pkg", str2);
0325 |                                 jSONArray2.put(jSONObject4);
0326 |                                 int length2 = i4 + jSONObject4.toString().length();
0327 |                                 if (length2 >= 286720) {
0328 |                                     a.b.a.e.l0.p.c("UsageEvent", "usage reached max len: " + jSONObject3);
0329 |                                     break;
0330 |                                 }
0331 |                                 i4 = length2;
0332 |                                 i3++;
0333 |                                 length = i5;
0334 |                                 declaredField2 = field3;
0335 |                                 declaredField3 = field;
0336 |                                 declaredField = field2;
0337 |                                 jSONObject2 = jSONObject;
0338 |                                 mapL = map;
0339 |                             }
0340 |                             a.b.a.e.l0.p.c("UsageEvent", str);
0341 |                             i3++;
0342 |                             length = i5;
0343 |                             declaredField2 = field3;
0344 |                             declaredField3 = field;
0345 |                             declaredField = field2;
0346 |                             jSONObject2 = jSONObject;
0347 |                             mapL = map;
0348 |                         }
0349 |                         if (a.b.a.e.l0.p.f602a) {
0350 |                             a.b.a.e.l0.p.c("UsageEvent", "put to syspref, usageTotal=" + jSONArray.toString());
0351 |                         }
0352 |                         JSONObject jSONObject5 = new JSONObject();
0353 |                         jSONObject5.put("last_time", System.currentTimeMillis());
0354 |                         jSONObject5.put("prev_usg", jSONArray);
0355 |                         String string = jSONObject5.toString();
0356 |                         if (a.b.a.e.l0.r.p()) {
0357 |                             c.a();
0358 |                             c.f468b.putString("today_usage_till_now", string).apply();
0359 |                         }
0360 |                         jSONArrayU = jSONArray2;
0361 |                     }
0362 |                 } catch (Exception e) {
0363 |                     e = e;
0364 |                     a.b.a.e.l0.p.h("UsageEvent", "queryToadyUsageStatsForKK exception:", e);
0365 |                     a.b.a.e.l0.p.c("UsageEvent", "queryToadyUsageStatsForKK will return null");
0366 |                 }
0367 |             } catch (Exception e2) {
0368 |                 e = e2;
0369 |                 jSONObject = jSONObject2;
0370 |             }
0371 |         }
0372 |         if (jSONArrayU == null || jSONArrayU.length() == 0) {
0373 |             a.b.a.e.l0.p.c("UsageEvent", "No new usage data to report");
0374 |             return null;
0375 |         }
0376 |         JSONObject jSONObject6 = jSONObject;
0377 |         jSONObject6.put("start", j);
0378 |         jSONObject6.put("end", j2);
0379 |         jSONObject6.put("radio", a.b.a.e.l0.c.z());
0380 |         jSONObject6.put("radio2", a.b.a.e.l0.c.A(1));
0381 |         jSONObject6.put("seq", jSONArrayU);
0382 |         if (a.b.a.e.l0.r.q()) {
0383 |             jSONObject6.put("custom_version", a.b.a.e.l0.c.e());
0384 |         }
0385 |         if (a.b.a.e.l0.p.f602a) {
0386 |             String string2 = jSONObject6.toString();
0387 |             byte[] bytes = string2.getBytes();
0388 |             StringBuilder sbC = a.a.a.a.a.c("Usage Sequence size(K): ");
0389 |             sbC.append(bytes.length / 1024);
0390 |             sbC.append(", content=");
0391 |             sbC.append(string2);
0392 |             a.b.a.e.l0.p.c("UsageEvent", sbC.toString());
0393 |         }
0394 |         return jSONObject6;
0395 |     }
0396 | 
0397 |     public final long h(String str) {
0398 |         if (i.containsKey(str)) {
0399 |             return i.get(str).longValue();
0400 |         }
```

Original lines 1090–1160:

```text
1090 |         JSONObject jSONObjectE2;
1091 |         int length3;
1092 |         int iIntValue;
1093 |         ArrayList arrayList12;
1094 |         long j21;
1095 |         String str20;
1096 |         String str21;
1097 |         ArrayList arrayList13;
1098 |         int i10;
1099 |         String str22;
1100 |         long timeStamp2;
1101 |         int eventType3;
1102 |         long timeStamp3;
1103 |         long timeStamp4;
1104 |         UsageEvents usageEvents3;
1105 |         boolean z6;
1106 |         long timeStamp5;
1107 |         long jLongValue3;
1108 |         long j22;
1109 |         y yVar = this;
1110 |         StringBuilder sbD = a.a.a.a.a.d("queryUsageEventSequence: startTime:", j, "  endTime:");
1111 |         sbD.append(j2);
1112 |         a.b.a.e.l0.p.c("UsageEvent", sbD.toString());
1113 |         Context contextB = a.b.a.e.e.b();
1114 |         JSONArray jSONArray2 = new JSONArray();
1115 |         UsageEvents usageEventsQueryEvents = ((UsageStatsManager) contextB.getSystemService("usagestats")).queryEvents(j, j2);
1116 |         if (usageEventsQueryEvents == null) {
1117 |             a.b.a.e.l0.p.s("UsageEvent", "queryUsageEventSequence usageEvents is null");
1118 |             return jSONArray2;
1119 |         }
1120 |         String str23 = "last_usage_resume_event";
1121 |         String str24 = "";
1122 |         String strH2 = a.b.a.e.l0.u.h("last_usage_resume_event", "");
1123 |         ArrayList arrayList14 = new ArrayList();
1124 |         String str25 = ",";
1125 |         String str26 = "\\|";
1126 |         if (!TextUtils.isEmpty(strH2)) {
1127 |             String[] strArrSplit3 = strH2.split("\\|");
1128 |             if (strArrSplit3.length <= 2) {
1129 |                 int length4 = strArrSplit3.length;
1130 |                 int i11 = 0;
1131 |                 while (i11 < length4) {
1132 |                     String[] strArrSplit4 = strArrSplit3[i11].split(",");
1133 |                     String[] strArr = strArrSplit3;
1134 |                     int i12 = length4;
1135 |                     if (strArrSplit4.length == 2) {
1136 |                         try {
1137 |                             arrayList14.add(Pair.create(strArrSplit4[0], Long.valueOf(Long.parseLong(strArrSplit4[1]))));
1138 |                         } catch (Exception e) {
1139 |                             a.b.a.e.l0.p.h("UsageEvent", "getLastNotAssembledEvent error, e:", e);
1140 |                         }
1141 |                     }
1142 |                     i11++;
1143 |                     strArrSplit3 = strArr;
1144 |                     length4 = i12;
1145 |                 }
1146 |             }
1147 |         }
1148 |         if (arrayList14.size() != 0) {
1149 |             a.b.a.e.l0.p.c("UsageEvent", "assemble usage events before->last not assembled event : " + arrayList14);
1150 |         }
1151 |         String str27 = "last_usage_input_shown_event";
1152 |         String strH3 = a.b.a.e.l0.u.h("last_usage_input_shown_event", "");
1153 |         if (TextUtils.isEmpty(strH3)) {
1154 |             pairCreate = null;
1155 |         } else {
1156 |             String[] strArrSplit5 = strH3.split(",");
1157 |             if (strArrSplit5.length != 2) {
1158 |                 pairCreate = null;
1159 |             } else {
1160 |                 try {
```

Original lines 2890–2950:

```text
2890 |             if (i4 == 1 || i4 == 2) {
2891 |                 String string = jSONObject2.getString("pkg");
2892 |                 if (i4 == 1 && !i.containsKey(string)) {
2893 |                     jSONObject2.put("stat", "app_start");
2894 |                     i.put(string, 0L);
2895 |                 } else if (i4 == 2) {
2896 |                     i.put(string, Long.valueOf(h(string) + jSONObject2.getLong("duration")));
2897 |                     int i5 = i3 + 1;
2898 |                     if (i5 != jSONArray.length() ? true ^ jSONArray.getJSONObject(i3).getString("pkg").equals(jSONArray.getJSONObject(i5).getString("pkg")) : true) {
2899 |                         long jH = h(string);
2900 |                         jSONObject2.put("stat", "app_end");
2901 |                         jSONObject2.put("app_duration", jH);
2902 |                         i.remove(string);
2903 |                     }
2904 |                 }
2905 |                 if (i3 + 1 == length2) {
2906 |                     JSONArray jSONArray3 = new JSONArray();
2907 |                     for (String str : i.keySet()) {
2908 |                         long jLongValue = i.get(str).longValue();
2909 |                         JSONObject jSONObject3 = new JSONObject();
2910 |                         jSONObject3.put("pkg", str);
2911 |                         jSONObject3.put("total_duration", jLongValue);
2912 |                         jSONArray3.put(jSONObject3);
2913 |                     }
2914 |                     a.b.a.e.l0.u.m("last_not_ended_event_array", jSONArray3.toString());
2915 |                 }
2916 |             }
2917 |             i3++;
2918 |         }
2919 |         if (a.b.a.c.v.h.f()) {
2920 |             String strJ = a.b.a.e.l0.r.j();
2921 |             for (int i6 = length2 - 1; i6 >= 0; i6--) {
2922 |                 JSONObject jSONObject4 = (JSONObject) jSONArray.get(i6);
2923 |                 String strOptString = jSONObject4.optString("pkg");
2924 |                 String strOptString2 = jSONObject4.optString("class");
2925 |                 boolean zC = w.b.f459a.c(strJ, strOptString, strOptString2);
2926 |                 a.b.a.e.l0.p.c("UsageEvent", "pkg: " + strOptString + ", className: " + strOptString2 + ", shouldCollect: " + zC);
2927 |                 if (!zC) {
2928 |                     if (jSONObject4.has("class") && !jSONObject4.has("stat")) {
2929 |                         jSONArray.remove(i6);
2930 |                     }
2931 |                     jSONObject4.remove("class");
2932 |                     jSONObject4.remove("stat");
2933 |                 }
2934 |             }
2935 |         }
2936 |         if (a.b.a.e.l0.p.f602a) {
2937 |             StringBuilder sbC2 = a.a.a.a.a.c("------------ after tagged ----------sequence size: ");
2938 |             sbC2.append(jSONArray.length());
2939 |             a.b.a.e.l0.p.c("UsageEvent", sbC2.toString());
2940 |             for (int i7 = 0; i7 < jSONArray.length(); i7++) {
2941 |                 try {
2942 |                     a.b.a.e.l0.p.c("UsageEvent", jSONArray.get(i7).toString());
2943 |                 } catch (Exception e) {
2944 |                     a.b.a.e.l0.p.h("UsageEvent", "??", e);
2945 |                 }
2946 |             }
2947 |         }
2948 |         return jSONArray;
2949 |     }
2950 | }
```

## A06

Source: `sources/a/b/a/e/h0/w.java`

Source-file SHA-256: `51418cf7c6b6a9b43beb66b468c2b9866de5ce639f3160bc279ee56ca19789eb`

Original lines 1–143:

```text
0001 | package a.b.a.e.h0;
0002 | 
0003 | import android.text.TextUtils;
0004 | import java.util.ArrayList;
0005 | import java.util.Arrays;
0006 | import java.util.HashMap;
0007 | import java.util.Iterator;
0008 | import java.util.List;
0009 | import org.json.JSONArray;
0010 | import org.json.JSONException;
0011 | import org.json.JSONObject;
0012 | 
0013 | /* JADX INFO: loaded from: classes.dex */
0014 | public class w {
0015 | 
0016 |     /* JADX INFO: renamed from: a, reason: collision with root package name */
0017 |     public final HashMap<String, HashMap<String, List<String>>> f458a = new HashMap<>();
0018 | 
0019 |     public static class b {
0020 | 
0021 |         /* JADX INFO: renamed from: a, reason: collision with root package name */
0022 |         public static final w f459a = new w(null);
0023 |     }
0024 | 
0025 |     public w(a aVar) {
0026 |         String strC = a.b.a.e.l0.u.c();
0027 |         a.b.a.e.l0.p.c("UsageCollectManager", "data: " + strC);
0028 |         if (TextUtils.isEmpty(strC)) {
0029 |             a(a.b.a.e.l0.r.j());
0030 |             return;
0031 |         }
0032 |         try {
0033 |             b(new JSONObject(strC));
0034 |         } catch (JSONException e) {
0035 |             a.b.a.e.l0.p.h("UsageCollectManager", "parse local config error, e: ", e);
0036 |         }
0037 |     }
0038 | 
0039 |     public final void a(String str) {
0040 |         HashMap<String, List<String>> map;
0041 |         this.f458a.clear();
0042 |         a.b.a.e.l0.p.c("UsageCollectManager", "region: " + str);
0043 |         if (TextUtils.equals("VN", str)) {
0044 |             map = new HashMap<>();
0045 |             map.put(d(new byte[]{99, 111, 109, 46, 98, 112, 108, 117, 115, 46, 118, 116, 112, 97, 121}), Arrays.asList(d(new byte[]{99, 111, 109, 46, 98, 112, 108, 117, 115, 46, 118, 116, 112, 97, 121, 46, 118, 105, 110, 101, 115, 46, 114, 101, 115, 117, 108, 116, 46, 87, 101, 98, 86, 105, 101, 119, 78, 97, 112, 97, 115, 86, 105, 110, 101, 115, 65, 99, 116, 105, 118, 105, 116, 121}), d(new byte[]{99, 111, 109, 46, 98, 112, 108, 117, 115, 46, 118, 116, 112, 97, 121, 46, 97, 99, 116, 105, 118, 105, 116, 121, 46, 77, 97, 105, 110, 70, 114, 97, 103, 109, 101, 110, 116, 65, 99, 116, 105, 118, 105, 116, 121}), d(new byte[]{118, 110, 46, 118, 105, 101, 116, 116, 101, 108, 112, 97, 121, 46, 117, 105, 46, 115, 99, 114, 101, 101, 110, 46, 114, 101, 115, 117, 108, 116, 46, 82, 101, 115, 117, 108, 116, 65, 99, 116, 105, 118, 105, 116, 121})));
0046 |             map.put(d(new byte[]{118, 110, 46, 99, 111, 109, 46, 118, 110, 103, 46, 122, 97, 108, 111, 112, 97, 121}), Arrays.asList(d(new byte[]{118, 110, 46, 99, 111, 109, 46, 118, 110, 103, 46, 122, 97, 108, 111, 112, 97, 121, 46, 99, 97, 115, 104, 105, 101, 114, 46, 112, 114, 101, 115, 101, 110, 116, 97, 116, 105, 111, 110, 46, 117, 105, 46, 109, 97, 105, 110, 46, 77, 97, 105, 110, 67, 97, 115, 104, 105, 101, 114, 86, 50, 65, 99, 116, 105, 118, 105, 116, 121}), d(new byte[]{118, 110, 46, 99, 111, 109, 46, 118, 110, 103, 46, 114, 101, 115, 117, 108, 116, 112, 97, 103, 101, 46, 112, 114, 101, 115, 101, 110, 116, 97, 116, 105, 111, 110, 46, 117, 105, 46, 97, 99, 116, 105, 118, 105, 116, 121, 46, 82, 101, 115, 117, 108, 116, 80, 97, 103, 101, 86, 50, 65, 99, 116, 105, 118, 105, 116, 121})));
0047 |             map.put(d(new byte[]{99, 111, 109, 46, 103, 111, 111, 103, 108, 101, 46, 97, 110, 100, 114, 111, 105, 100, 46, 103, 109, 115}), Arrays.asList(d(new byte[]{99, 111, 109, 46, 103, 111, 111, 103, 108, 101, 46, 97, 110, 100, 114, 111, 105, 100, 46, 103, 109, 115, 46, 119, 97, 108, 108, 101, 116, 46, 97, 99, 116, 105, 118, 105, 116, 121, 46, 71, 101, 110, 101, 114, 105, 99, 68, 101, 108, 101, 103, 97, 116, 111, 114, 73, 110, 116, 101, 114, 110, 97, 108, 65, 99, 116, 105, 118, 105, 116, 121, 88})));
0048 |             map.put(d(new byte[]{99, 111, 109, 46, 115, 104, 111, 112, 101, 101, 46, 118, 110}), Arrays.asList(d(new byte[]{99, 111, 109, 46, 115, 104, 111, 112, 101, 101, 46, 97, 112, 112, 46, 114, 101, 97, 99, 116, 46, 82, 101, 97, 99, 116, 65, 99, 116, 105, 118, 105, 116, 121, 95})));
0049 |             map.put(d(new byte[]{99, 111, 109, 46, 108, 97, 122, 97, 100, 97, 46, 97, 110, 100, 114, 111, 105, 100}), Arrays.asList(d(new byte[]{99, 111, 109, 46, 108, 97, 122, 97, 100, 97, 46, 97, 110, 100, 114, 111, 105, 100, 46, 112, 97, 121, 109, 101, 110, 116, 46, 77, 105, 110, 105, 80, 97, 121, 109, 101, 110, 116, 65, 99, 116, 105, 118, 105, 116, 121}), d(new byte[]{99, 111, 109, 46, 108, 97, 122, 97, 100, 97, 46, 97, 110, 100, 114, 111, 105, 100, 46, 99, 104, 101, 99, 107, 111, 117, 116, 46, 115, 104, 105, 112, 112, 105, 110, 103, 46, 76, 97, 122, 83, 104, 105, 112, 112, 105, 110, 103, 84, 111, 111, 108, 65, 99, 116, 105, 118, 105, 116, 121})));
0050 |         } else if (TextUtils.equals("MY", str)) {
0051 |             map = new HashMap<>();
0052 |             map.put(d(new byte[]{109, 121, 46, 99, 111, 109, 46, 116, 110, 103, 100, 105, 103, 105, 116, 97, 108, 46, 101, 119, 97, 108, 108, 101, 116}), Arrays.asList(d(new byte[]{99, 111, 109, 46, 97, 108, 105, 98, 97, 98, 97, 46, 103, 114, 105, 118, 101, 114, 46, 99, 111, 114, 101, 46, 117, 105, 46, 97, 99, 116, 105, 118, 105, 116, 121, 46, 71, 114, 105, 118, 101, 114, 66, 97, 115, 101, 65, 99, 116, 105, 118, 105, 116, 121, 36, 77, 97, 105, 110})));
0053 |             map.put(d(new byte[]{99, 111, 109, 46, 103, 111, 111, 103, 108, 101, 46, 97, 110, 100, 114, 111, 105, 100, 46, 103, 109, 115}), Arrays.asList(d(new byte[]{99, 111, 109, 46, 103, 111, 111, 103, 108, 101, 46, 97, 110, 100, 114, 111, 105, 100, 46, 103, 109, 115, 46, 119, 97, 108, 108, 101, 116, 46, 97, 99, 116, 105, 118, 105, 116, 121, 46, 71, 101, 110, 101, 114, 105, 99, 68, 101, 108, 101, 103, 97, 116, 111, 114, 73, 110, 116, 101, 114, 110, 97, 108, 65, 99, 116, 105, 118, 105, 116, 121, 88})));
0054 |             map.put(d(new byte[]{99, 111, 109, 46, 115, 104, 111, 112, 101, 101, 46, 109, 121}), Arrays.asList(d(new byte[]{99, 111, 109, 46, 115, 104, 111, 112, 101, 101, 46, 97, 112, 112, 46, 114, 101, 97, 99, 116, 46, 82, 101, 97, 99, 116, 65, 99, 116, 105, 118, 105, 116, 121, 95})));
0055 |             map.put(d(new byte[]{99, 111, 109, 46, 108, 97, 122, 97, 100, 97, 46, 97, 110, 100, 114, 111, 105, 100}), Arrays.asList(d(new byte[]{99, 111, 109, 46, 108, 97, 122, 97, 100, 97, 46, 97, 110, 100, 114, 111, 105, 100, 46, 112, 97, 121, 109, 101, 110, 116, 46, 77, 105, 110, 105, 80, 97, 121, 109, 101, 110, 116, 65, 99, 116, 105, 118, 105, 116, 121}), d(new byte[]{99, 111, 109, 46, 108, 97, 122, 97, 100, 97, 46, 97, 110, 100, 114, 111, 105, 100, 46, 99, 104, 101, 99, 107, 111, 117, 116, 46, 115, 104, 105, 112, 112, 105, 110, 103, 46, 76, 97, 122, 83, 104, 105, 112, 112, 105, 110, 103, 84, 111, 111, 108, 65, 99, 116, 105, 118, 105, 116, 121})));
0056 |         } else if (TextUtils.equals("TH", str)) {
0057 |             map = new HashMap<>();
0058 |             map.put(d(new byte[]{99, 111, 109, 46, 115, 104, 111, 112, 101, 101, 46, 116, 104}), Arrays.asList(d(new byte[]{99, 111, 109, 46, 115, 104, 111, 112, 101, 101, 46, 97, 112, 112, 46, 114, 101, 97, 99, 116, 46, 82, 101, 97, 99, 116, 65, 99, 116, 105, 118, 105, 116, 121, 95})));
0059 |             map.put(d(new byte[]{99, 111, 109, 46, 103, 111, 111, 103, 108, 101, 46, 97, 110, 100, 114, 111, 105, 100, 46, 103, 109, 115}), Arrays.asList(d(new byte[]{99, 111, 109, 46, 103, 111, 111, 103, 108, 101, 46, 97, 110, 100, 114, 111, 105, 100, 46, 103, 109, 115, 46, 119, 97, 108, 108, 101, 116, 46, 97, 99, 116, 105, 118, 105, 116, 121, 46, 71, 101, 110, 101, 114, 105, 99, 68, 101, 108, 101, 103, 97, 116, 111, 114, 73, 110, 116, 101, 114, 110, 97, 108, 65, 99, 116, 105, 118, 105, 116, 121, 88})));
0060 |             map.put(d(new byte[]{99, 111, 109, 46, 108, 97, 122, 97, 100, 97, 46, 97, 110, 100, 114, 111, 105, 100}), Arrays.asList(d(new byte[]{99, 111, 109, 46, 108, 97, 122, 97, 100, 97, 46, 97, 110, 100, 114, 111, 105, 100, 46, 112, 97, 121, 109, 101, 110, 116, 46, 77, 105, 110, 105, 80, 97, 121, 109, 101, 110, 116, 65, 99, 116, 105, 118, 105, 116, 121}), d(new byte[]{99, 111, 109, 46, 108, 97, 122, 97, 100, 97, 46, 97, 110, 100, 114, 111, 105, 100, 46, 99, 104, 101, 99, 107, 111, 117, 116, 46, 115, 104, 105, 112, 112, 105, 110, 103, 46, 76, 97, 122, 83, 104, 105, 112, 112, 105, 110, 103, 84, 111, 111, 108, 65, 99, 116, 105, 118, 105, 116, 121})));
0061 |         } else {
0062 |             map = null;
0063 |         }
0064 |         if (map != null) {
0065 |             this.f458a.put(str, map);
0066 |         }
0067 |     }
0068 | 
0069 |     public void b(JSONObject jSONObject) {
0070 |         this.f458a.clear();
0071 |         JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("ac_w");
0072 |         if (jSONObjectOptJSONObject == null) {
0073 |             a(a.b.a.e.l0.r.j());
0074 |         } else if (jSONObjectOptJSONObject.length() != 0) {
0075 |             Iterator<String> itKeys = jSONObjectOptJSONObject.keys();
0076 |             while (itKeys.hasNext()) {
0077 |                 String next = itKeys.next();
0078 |                 a.b.a.e.l0.p.c("UsageCollectManager", "parseConfig->region: " + next);
0079 |                 JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject.optJSONObject(next);
0080 |                 if (!TextUtils.isEmpty(next) && jSONObjectOptJSONObject2 != null && jSONObjectOptJSONObject2.length() > 0) {
0081 |                     Iterator<String> itKeys2 = jSONObjectOptJSONObject2.keys();
0082 |                     HashMap map = new HashMap();
0083 |                     while (itKeys2.hasNext()) {
0084 |                         String next2 = itKeys2.next();
0085 |                         a.b.a.e.l0.p.c("UsageCollectManager", "parseConfig->pkg: " + next2);
0086 |                         JSONArray jSONArrayOptJSONArray = jSONObjectOptJSONObject2.optJSONArray(next2);
0087 |                         if (!TextUtils.isEmpty(next2) && jSONArrayOptJSONArray != null && jSONArrayOptJSONArray.length() > 0) {
0088 |                             ArrayList arrayList = new ArrayList();
0089 |                             for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
0090 |                                 arrayList.add(jSONArrayOptJSONArray.optString(i));
0091 |                                 if (arrayList.size() >= 3) {
0092 |                                     break;
0093 |                                 }
0094 |                             }
0095 |                             map.put(next2, arrayList);
0096 |                             if (map.size() >= 20) {
0097 |                                 break;
0098 |                             }
0099 |                         }
0100 |                     }
0101 |                     this.f458a.put(next, (HashMap<String, List<String>>) map);
0102 |                     if (this.f458a.size() >= 7) {
0103 |                         break;
0104 |                     }
0105 |                 }
0106 |             }
0107 |         }
0108 |         StringBuilder sbC = a.a.a.a.a.c("parseConfig finish, mUsageCollectWhiteList: ");
0109 |         sbC.append(this.f458a);
0110 |         a.b.a.e.l0.p.c("UsageCollectManager", sbC.toString());
0111 |     }
0112 | 
0113 |     public boolean c(String str, String str2, String str3) {
0114 |         HashMap<String, List<String>> map;
0115 |         List<String> list;
0116 |         if (!this.f458a.isEmpty() && this.f458a.containsKey(str) && (map = this.f458a.get(str)) != null && map.containsKey(str2) && (list = map.get(str2)) != null) {
0117 |             Iterator<String> it = list.iterator();
0118 |             while (it.hasNext()) {
0119 |                 if (TextUtils.equals(it.next(), str3)) {
0120 |                     return true;
0121 |                 }
0122 |             }
0123 |         }
0124 |         return false;
0125 |     }
0126 | 
0127 |     public final String d(byte[] bArr) {
0128 |         String str;
0129 |         try {
0130 |             str = new String(bArr, "UTF-8");
0131 |             try {
0132 |                 a.b.a.e.l0.p.c("UsageCollectManager", "transmitToString content:" + str);
0133 |             } catch (Exception e) {
0134 |                 e = e;
0135 |                 a.b.a.e.l0.p.g("UsageCollectManager", e.getMessage());
0136 |             }
0137 |         } catch (Exception e2) {
0138 |             e = e2;
0139 |             str = "";
0140 |         }
0141 |         return str;
0142 |     }
0143 | }
```

## A07

Source: `sources/a/b/a/b/a.java`

Source-file SHA-256: `8b0683ca4c147d1ece391e3c7c30bf78463f2ed78cf96802ac844e8009638339`

Original lines 1–152:

```text
0001 | package a.b.a.b;
0002 | 
0003 | import a.b.a.b.b.b;
0004 | import a.b.a.e.e;
0005 | import a.b.a.e.l0.r;
0006 | import a.b.a.g.c.c;
0007 | import android.text.TextUtils;
0008 | import java.util.Objects;
0009 | 
0010 | /* JADX INFO: loaded from: classes.dex */
0011 | public class a {
0012 |     public static boolean a(String str, String str2, boolean z) {
0013 |         String str3;
0014 |         if (z) {
0015 |             try {
0016 |                 b.b().d();
0017 |                 if (b(str)) {
0018 |                     b bVarB = b.b();
0019 |                     Objects.requireNonNull(bVarB);
0020 |                     return !((TextUtils.isEmpty(str) || bVarB.a(str) == null) ? false : bVarB.a(str).f6b);
0021 |                 }
0022 |                 if (a.b.a.g.f.b.f892b.contains(str)) {
0023 |                     if (!a.b.a.g.f.a.n(str2)) {
0024 |                         str2.hashCode();
0025 |                         str2.hashCode();
0026 |                         str2.hashCode();
0027 |                         str2.hashCode();
0028 |                         byte b2 = -1;
0029 |                         switch (str2.hashCode()) {
0030 |                             case 100707:
0031 |                                 if (str2.equals("et2")) {
0032 |                                     b2 = 0;
0033 |                                 }
0034 |                                 break;
0035 |                             case 100709:
0036 |                                 if (str2.equals("et4")) {
0037 |                                     b2 = 1;
0038 |                                 }
0039 |                                 break;
0040 |                             case 100711:
0041 |                                 if (str2.equals("et6")) {
0042 |                                     b2 = 2;
0043 |                                 }
0044 |                                 break;
0045 |                         }
0046 |                         switch (b2) {
0047 |                             case 0:
0048 |                             case 1:
0049 |                                 str3 = "r3";
0050 |                                 break;
0051 |                             case 2:
0052 |                                 str3 = "r4";
0053 |                                 break;
0054 |                             default:
0055 |                                 str3 = "";
0056 |                                 break;
0057 |                         }
0058 |                     } else {
0059 |                         str3 = "r9";
0060 |                     }
0061 |                     c.f(true, str2, 1, str3);
0062 |                 }
0063 |                 return false;
0064 |             } catch (Exception e) {
0065 |                 a.a.a.a.a.g(e, a.a.a.a.a.c("check failed: "), "FireBaseChecker");
0066 |             }
0067 |         }
0068 |         return false;
0069 |     }
0070 | 
0071 |     public static boolean b(String str) {
0072 |         try {
0073 |             return "onetrack_active".equals(str) || r.b(e.b()) == 1;
0074 |         } catch (Exception e) {
0075 |             a.a.a.a.a.g(e, a.a.a.a.a.c("isAllowReportAppUsageOpen failed: "), "FireBaseChecker");
0076 |             return false;
0077 |         }
0078 |     }
0079 | 
0080 |     public static boolean c(String str) {
0081 |         try {
0082 |             if (!r.q()) {
0083 |                 return true;
0084 |             }
0085 |             String strJ = r.j();
0086 |             a.b.a.e.e0.a.d().k(strJ);
0087 |             return !r.t(strJ) || b(str);
0088 |         } catch (Exception e) {
0089 |             a.a.a.a.a.g(e, a.a.a.a.a.c("isAllowTrackForRussia failed: "), "FireBaseChecker");
0090 |             return true;
0091 |         }
0092 |     }
0093 | 
0094 |     public static boolean d() {
0095 |         try {
0096 |             if (g()) {
0097 |                 b bVarB = b.b();
0098 |                 Objects.requireNonNull(bVarB);
0099 |                 if ((TextUtils.isEmpty("onetrack_app_update") || bVarB.a("onetrack_app_update") == null) ? false : bVarB.a("onetrack_app_update").f5a) {
0100 |                     return true;
0101 |                 }
0102 |             }
0103 |         } catch (Exception e) {
0104 |             a.a.a.a.a.g(e, a.a.a.a.a.c("isCloseAnalyticsUpdateApp failed: "), "FireBaseChecker");
0105 |         }
0106 |         return false;
0107 |     }
0108 | 
0109 |     public static boolean e(String str) {
0110 |         try {
0111 |             if (g()) {
0112 |                 b bVarB = b.b();
0113 |                 Objects.requireNonNull(bVarB);
0114 |                 if ((TextUtils.isEmpty(str) || bVarB.a(str) == null) ? false : bVarB.a(str).f5a) {
0115 |                     return true;
0116 |                 }
0117 |             }
0118 |         } catch (Exception e) {
0119 |             a.a.a.a.a.g(e, a.a.a.a.a.c("isCloseOneTrackOverseasChannel failed: "), "FireBaseChecker");
0120 |         }
0121 |         return false;
0122 |     }
0123 | 
0124 |     public static boolean f() {
0125 |         try {
0126 |             if (g()) {
0127 |                 b bVarB = b.b();
0128 |                 Objects.requireNonNull(bVarB);
0129 |                 if ((TextUtils.isEmpty("onetrack_pull_config") || bVarB.a("onetrack_pull_config") == null) ? false : bVarB.a("onetrack_pull_config").f5a) {
0130 |                     return true;
0131 |                 }
0132 |             }
0133 |         } catch (Exception e) {
0134 |             a.a.a.a.a.g(e, a.a.a.a.a.c("isCloseOneTrackPullConfig failed: "), "FireBaseChecker");
0135 |         }
0136 |         return false;
0137 |     }
0138 | 
0139 |     public static boolean g() {
0140 |         try {
0141 |             if (!r.q()) {
0142 |                 return false;
0143 |             }
0144 |             String strJ = r.j();
0145 |             a.b.a.e.e0.a.d().k(strJ);
0146 |             return !r.t(strJ);
0147 |         } catch (Exception e) {
0148 |             a.a.a.a.a.g(e, a.a.a.a.a.c("isUseFirebaseTrack failed: "), "FireBaseChecker");
0149 |             return false;
0150 |         }
0151 |     }
0152 | }
```

## A08

Source: `sources/a/b/a/e/l.java`

Source-file SHA-256: `ba983f00d7f72042b419cdf8a69de9fbe97d4ba883d528f419e8da182bad8776`

Original lines 1–53:

```text
0001 | package a.b.a.e;
0002 | 
0003 | import android.os.SystemClock;
0004 | import java.util.Objects;
0005 | 
0006 | /* JADX INFO: loaded from: classes.dex */
0007 | public class l implements Runnable {
0008 | 
0009 |     /* JADX INFO: renamed from: a, reason: collision with root package name */
0010 |     public final /* synthetic */ boolean f559a;
0011 | 
0012 |     public l(boolean z) {
0013 |         this.f559a = z;
0014 |     }
0015 | 
0016 |     @Override // java.lang.Runnable
0017 |     public void run() {
0018 |         try {
0019 |             a.b.a.e.e0.a aVarD = a.b.a.e.e0.a.d();
0020 |             String str = e.f263c;
0021 |             aVarD.i("001", str, str);
0022 |             long jElapsedRealtime = SystemClock.elapsedRealtime();
0023 |             if (!a.b.a.e.h0.a.f438b.a(this.f559a) && !this.f559a) {
0024 |                 a.b.a.e.l0.p.c("EventTrack", "active event don't meet conditions, check cost: " + (SystemClock.elapsedRealtime() - jElapsedRealtime));
0025 |                 return;
0026 |             }
0027 |             a.b.a.g.c.c.f(false, "et1", 1, "triggered");
0028 |             a.b.a.e.h0.b bVar = new a.b.a.e.h0.b(this.f559a);
0029 |             if (!bVar.c()) {
0030 |                 a.b.a.a.a.c.a.m(this.f559a, "EventTrack", "active event is inValid");
0031 |                 a.b.a.g.c.c.f(false, "et1", 1, "r4");
0032 |                 return;
0033 |             }
0034 |             if (bVar.e.toString().length() > 307200) {
0035 |                 a.b.a.a.a.c.a.m(this.f559a, "EventTrack", "active event, too large data, skip it");
0036 |                 a.b.a.g.c.c.f(false, "et1", 1, "r5");
0037 |                 return;
0038 |             }
0039 |             boolean zD = a.b.a.e.f0.f.p().d(bVar, "et1", this.f559a);
0040 |             if (zD) {
0041 |                 a.b.a.e.h0.a aVar = a.b.a.e.h0.a.f438b;
0042 |                 e.b();
0043 |                 long jCurrentTimeMillis = System.currentTimeMillis();
0044 |                 Objects.requireNonNull(aVar);
0045 |                 a.b.a.e.l0.u.l("last_upload_active_time", jCurrentTimeMillis);
0046 |             }
0047 |             a.b.a.a.a.c.a.m(this.f559a, "EventTrack", "active event success:" + zD + ",use time:" + (SystemClock.elapsedRealtime() - jElapsedRealtime));
0048 |         } catch (Exception e) {
0049 |             a.b.a.a.a.c.a.s(this.f559a, "EventTrack", "Track active event failed,", e);
0050 |             a.b.a.a.a.c.a.y0("co", "s1", e);
0051 |         }
0052 |     }
0053 | }
```

## A09

Source: `sources/a/b/a/e/f0/f.java`

Source-file SHA-256: `001ab784e33a1bdee8c9a7d8db5cbbd1f56d8aee420e8f6caaaf84cd377b572a`

Original lines 400–535:

```text
0400 |                 f.sendEmptyMessageDelayed(1, j);
0401 |                 str = "check db size,start MESSAGE_CHECK_OVERSIZE";
0402 |             } else {
0403 |                 if (!f.hasMessages(1)) {
0404 |                     return;
0405 |                 }
0406 |                 f.removeMessages(1);
0407 |                 str = "check db size,stop MESSAGE_CHECK_OVERSIZE";
0408 |             }
0409 |             a.b.a.e.l0.p.c("EventManager", str);
0410 |         } catch (Exception e2) {
0411 |             a.b.a.e.l0.p.h("EventManager", "checkDatabaseSize error", e2);
0412 |         }
0413 |     }
0414 | 
0415 |     public static int i(a.b.a.e.f fVar, String str, boolean z) {
0416 |         try {
0417 |             if ("et7".equals(str)) {
0418 |                 return 1;
0419 |             }
0420 |             String str2 = fVar.f311c;
0421 |             if ("onetrack_active".equals(str2)) {
0422 |                 return z ? 3 : 2;
0423 |             }
0424 |             if ("onetrack_app_stat".equals(str2)) {
0425 |                 try {
0426 |                     JSONObject jSONObjectOptJSONObject = fVar.e.optJSONObject("B");
0427 |                     return a.b.a.a.a.c.a.g0(jSONObjectOptJSONObject != null ? jSONObjectOptJSONObject.optString("action", "") : "");
0428 |                 } catch (Exception unused) {
0429 |                     return 0;
0430 |                 }
0431 |             }
0432 |         } catch (Exception e2) {
0433 |             StringBuilder sbC = a.a.a.a.a.c("computeEventType error: ");
0434 |             sbC.append(e2.getMessage());
0435 |             a.b.a.e.l0.p.s("EventManager", sbC.toString());
0436 |         }
0437 |         return 0;
0438 |     }
0439 | 
0440 |     public static String j(byte[] bArr) {
0441 |         byte[] bArrB = a.b.a.e.g0.a.b(bArr, a.b.a.e.g0.d.a(a.b.a.e.g0.c.f429d, true).getBytes());
0442 |         if (bArrB == null || bArrB.length == 0) {
0443 |             bArrB = a.b.a.e.g0.a.b(bArr, a.b.a.e.g0.c.d().getBytes());
0444 |         }
0445 |         return (bArrB == null || bArrB.length == 0) ? "" : new String(bArrB);
0446 |     }
0447 | 
0448 |     public static byte[] m(String str) {
0449 |         return a.b.a.e.g0.a.e(str.getBytes(), a.b.a.e.g0.d.a(a.b.a.e.g0.c.f429d, true).getBytes());
0450 |     }
0451 | 
0452 |     public static f p() {
0453 |         if (f328c == null) {
0454 |             a.b.a.e.e.b();
0455 |             v();
0456 |         }
0457 |         return f328c;
0458 |     }
0459 | 
0460 |     public static void v() {
0461 |         if (f328c == null) {
0462 |             synchronized (f.class) {
0463 |                 if (f328c == null) {
0464 |                     f328c = new f();
0465 |                 }
0466 |             }
0467 |         }
0468 |     }
0469 | 
0470 |     public final void B(a.b.a.e.f fVar, boolean z) {
0471 |         if (!TextUtils.equals("001", fVar.f309a)) {
0472 |             StringBuilder sbC = a.a.a.a.a.c("addEvent: ");
0473 |             sbC.append(fVar.f311c);
0474 |             sbC.append(", appId: ");
0475 |             sbC.append(fVar.f309a);
0476 |             a.b.a.e.l0.p.c("EventManager", sbC.toString());
0477 |             return;
0478 |         }
0479 |         if (!"onetrack_active".equals(fVar.f311c)) {
0480 |             String str = fVar.f311c;
0481 |             a.b.a.e.l0.p.f("EventManager", "addEvent: " + str + ", data:" + fVar.e.toString(), a.b.a.e.l0.p.m(str));
0482 |             return;
0483 |         }
0484 |         StringBuilder sbC2 = a.a.a.a.a.c("addEvent: onetrack_active, data:");
0485 |         sbC2.append(fVar.e.toString());
0486 |         String string = sbC2.toString();
0487 |         boolean zM = a.b.a.e.l0.p.m(fVar.f311c);
0488 |         Level level = Level.DEBUG;
0489 |         if (zM) {
0490 |             a.b.a.e.l0.p.c("EventManager", string);
0491 |         } else {
0492 |             a.b.a.d.b.d dVarB = a.b.a.d.b.b(a.b.a.e.e.b());
0493 |             if (dVarB != null) {
0494 |                 dVarB.a(level, "##XLogger##, TAG: ", a.a.a.a.a.b("EventManager", ": ", string), null);
0495 |             }
0496 |         }
0497 |         a.b.a.d.b.d dVarV = a.b.a.a.a.c.a.V(z);
0498 |         if (dVarV != null) {
0499 |             dVarV.a(level, "##XLogger##, TAG: ", a.a.a.a.a.b("EventManager", ": ", string), null);
0500 |         }
0501 |     }
0502 | 
0503 |     public final void C(String str, String str2, String str3) {
0504 |         if (TextUtils.isEmpty(str) && TextUtils.isEmpty(str2)) {
0505 |             return;
0506 |         }
0507 |         a.b.a.e.l0.p.c("EventManager", String.format(str3, str, str2));
0508 |     }
0509 | 
0510 |     public void D() {
0511 |         e.b(new a(), 1000L);
0512 |         d dVar = f;
0513 |         if (dVar == null || dVar.hasMessages(2)) {
0514 |             return;
0515 |         }
0516 |         f.sendEmptyMessageDelayed(2, 86400000L);
0517 |     }
0518 | 
0519 |     public final boolean E(a.b.a.e.f fVar) {
0520 |         boolean z;
0521 |         String strG;
0522 |         JSONArray jSONArrayD;
0523 |         StringBuilder sb;
0524 |         StringBuilder sbC;
0525 |         JSONObject jSONObject = fVar.e;
0526 |         if (jSONObject == null) {
0527 |             sbC = a.a.a.a.a.c("AppId is ");
0528 |             sbC.append(fVar.f309a);
0529 |             sbC.append(", this event has no data, skip it.");
0530 |         } else {
0531 |             String str = fVar.f309a;
0532 |             if (!TextUtils.isEmpty(str) ? a.b.a.e.e0.g.c.f287a.g(str, "wl") : false) {
0533 |                 a.b.a.e.l0.p.c("EventManager", fVar.f309a + ",This appId and device in whiteList");
0534 |                 return true;
0535 |             }
```

## A10

Source: `sources/a/b/a/e/l0/n.java`

Source-file SHA-256: `b852b358bfffc1d8f732bff86e2aaced8ce7f989eaded83a52b5ec6c18bdf1a1`

Original lines 1–84:

```text
0001 | package a.b.a.e.l0;
0002 | 
0003 | import android.content.Context;
0004 | import android.content.Intent;
0005 | import android.content.SharedPreferences;
0006 | import android.text.TextUtils;
0007 | 
0008 | /* JADX INFO: loaded from: classes.dex */
0009 | public class n {
0010 | 
0011 |     /* JADX INFO: renamed from: a, reason: collision with root package name */
0012 |     public static SharedPreferences f597a;
0013 | 
0014 |     /* JADX INFO: renamed from: b, reason: collision with root package name */
0015 |     public static SharedPreferences.Editor f598b;
0016 | 
0017 |     public static void a(Context context) {
0018 |         if (f598b != null) {
0019 |             return;
0020 |         }
0021 |         synchronized (n.class) {
0022 |             if (f598b == null && context != null) {
0023 |                 SharedPreferences sharedPreferences = context.getSharedPreferences("ot_i_c", 0);
0024 |                 f597a = sharedPreferences;
0025 |                 f598b = sharedPreferences.edit();
0026 |             }
0027 |         }
0028 |     }
0029 | 
0030 |     public static String b(Context context, String str) {
0031 |         a(context);
0032 |         if (TextUtils.isEmpty(str)) {
0033 |             return null;
0034 |         }
0035 |         try {
0036 |             String string = f597a.getString(a.b.a.e.g0.d.d(str + a.b.a.e.g0.b.f423b), "");
0037 |             return !TextUtils.isEmpty(string) ? a.b.a.e.g0.a.a(string, a.b.a.e.g0.b.f422a) : string;
0038 |         } catch (Exception e) {
0039 |             p.g("InstanceIdCacheUtil", e.getMessage());
0040 |             return null;
0041 |         }
0042 |     }
0043 | 
0044 |     public static void c(Context context, String str, String str2) {
0045 |         a(context);
0046 |         if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
0047 |             return;
0048 |         }
0049 |         try {
0050 |             f598b.putString(a.b.a.e.g0.d.d(str + a.b.a.e.g0.b.f423b), a.b.a.e.g0.a.c(str2, a.b.a.e.g0.b.f422a)).apply();
0051 |         } catch (Exception e) {
0052 |             p.g("InstanceIdCacheUtil", e.getMessage());
0053 |         }
0054 |     }
0055 | 
0056 |     public static void d(Intent intent) {
0057 |         a(a.b.a.e.e.b());
0058 |         try {
0059 |             if (intent.getData() != null && intent.getAction() != null) {
0060 |                 String schemeSpecificPart = intent.getData().getSchemeSpecificPart();
0061 |                 if (TextUtils.isEmpty(schemeSpecificPart) || !intent.getAction().equals("android.intent.action.PACKAGE_REMOVED") || intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
0062 |                     return;
0063 |                 }
0064 |                 e(schemeSpecificPart);
0065 |             }
0066 |         } catch (Exception e) {
0067 |             StringBuilder sbC = a.a.a.a.a.c("PackageReceiver error: ");
0068 |             sbC.append(e.toString());
0069 |             p.g("InstanceIdCacheUtil", sbC.toString());
0070 |         }
0071 |     }
0072 | 
0073 |     public static void e(String str) {
0074 |         a(a.b.a.e.e.b());
0075 |         if (TextUtils.isEmpty(str)) {
0076 |             return;
0077 |         }
0078 |         try {
0079 |             f598b.remove(a.b.a.e.g0.d.d(str + a.b.a.e.g0.b.f423b)).apply();
0080 |         } catch (Exception e) {
0081 |             p.g("InstanceIdCacheUtil", e.getMessage());
0082 |         }
0083 |     }
0084 | }
```

## A11

Source: `sources/a/b/a/e/f0/z.java`

Source-file SHA-256: `b421db109e5ad3a92123e27a71813f7f987c48d3731de649ea228309f37370e6`

Original lines 1–220:

```text
0001 | package a.b.a.e.f0;
0002 | 
0003 | import android.database.Cursor;
0004 | import android.database.sqlite.SQLiteBlobTooBigException;
0005 | import android.database.sqlite.SQLiteDatabase;
0006 | import android.os.Bundle;
0007 | import android.os.Message;
0008 | import android.text.TextUtils;
0009 | import android.util.ArrayMap;
0010 | import java.io.ByteArrayOutputStream;
0011 | import java.nio.charset.StandardCharsets;
0012 | import java.util.ArrayList;
0013 | import java.util.Objects;
0014 | import java.util.concurrent.Callable;
0015 | import java.util.concurrent.ConcurrentHashMap;
0016 | import java.util.concurrent.FutureTask;
0017 | import java.util.concurrent.ThreadPoolExecutor;
0018 | import java.util.concurrent.TimeUnit;
0019 | import java.util.zip.GZIPOutputStream;
0020 | import org.json.JSONArray;
0021 | import org.json.JSONException;
0022 | import org.json.JSONObject;
0023 | 
0024 | /* JADX INFO: loaded from: classes.dex */
0025 | public class z {
0026 | 
0027 |     /* JADX INFO: renamed from: b, reason: collision with root package name */
0028 |     public static volatile z f397b;
0029 | 
0030 |     /* JADX INFO: renamed from: a, reason: collision with root package name */
0031 |     public y f398a = new y();
0032 | 
0033 |     public class a implements Callable<Boolean> {
0034 | 
0035 |         /* JADX INFO: renamed from: a, reason: collision with root package name */
0036 |         public final /* synthetic */ a.b.a.e.f f399a;
0037 | 
0038 |         /* JADX INFO: renamed from: b, reason: collision with root package name */
0039 |         public final /* synthetic */ boolean f400b;
0040 | 
0041 |         public a(a.b.a.e.f fVar, boolean z) {
0042 |             this.f399a = fVar;
0043 |             this.f400b = z;
0044 |         }
0045 | 
0046 |         @Override // java.util.concurrent.Callable
0047 |         public Boolean call() throws Exception {
0048 |             boolean zB = false;
0049 |             try {
0050 |                 z.a(z.this, this.f399a, this.f400b);
0051 |                 JSONArray jSONArray = new JSONArray();
0052 |                 jSONArray.put(this.f399a.e);
0053 |                 String strF = z.this.f(jSONArray);
0054 |                 byte[] bArrC = z.this.c(a.b.a.a.a.c.a.U0(strF));
0055 |                 if (bArrC != null && bArrC.length != 0) {
0056 |                     a.b.a.e.l0.p.e(this.f399a.f311c, "UploaderEngine", "uploadAddDbFailedEvent before zip and encrypt, len=" + strF.length() + ", after=" + bArrC.length, this.f400b);
0057 |                     for (int i = 1; i >= 0 && !zB; i--) {
0058 |                         zB = z.b(z.this, bArrC, this.f399a.f311c, this.f400b);
0059 |                     }
0060 |                     return Boolean.valueOf(zB);
0061 |                 }
0062 |                 return Boolean.FALSE;
0063 |             } catch (Exception e) {
0064 |                 a.b.a.e.l0.p.i(this.f399a.f311c, "UploaderEngine", "uploadAddDbFailedEvent call e: ", e, this.f400b);
0065 |                 a.b.a.a.a.c.a.y0("sa", "s8", e);
0066 |             }
0067 |         }
0068 |     }
0069 | 
0070 |     public static class b {
0071 | 
0072 |         /* JADX INFO: renamed from: a, reason: collision with root package name */
0073 |         public boolean f402a;
0074 | 
0075 |         /* JADX INFO: renamed from: b, reason: collision with root package name */
0076 |         public boolean f403b;
0077 | 
0078 |         public b(boolean z, boolean z2) {
0079 |             this.f402a = z;
0080 |             this.f403b = z2;
0081 |         }
0082 |     }
0083 | 
0084 |     public static void a(z zVar, a.b.a.e.f fVar, boolean z) {
0085 |         String str;
0086 |         Objects.requireNonNull(zVar);
0087 |         JSONObject jSONObjectOptJSONObject = fVar.e.optJSONObject("H");
0088 |         if (jSONObjectOptJSONObject != null) {
0089 |             a.b.a.e.e0.l.b.f306a.b(jSONObjectOptJSONObject);
0090 |         }
0091 |         if ("onetrack_active".equals(fVar.f311c)) {
0092 |             JSONObject jSONObjectOptJSONObject2 = fVar.e.optJSONObject("B");
0093 |             if (jSONObjectOptJSONObject2 == null) {
0094 |                 str = "body is null, return!";
0095 |             } else {
0096 |                 if (!jSONObjectOptJSONObject2.has("tz_content")) {
0097 |                     try {
0098 |                         o.d().f((a.b.a.e.h0.b) fVar, z);
0099 |                         return;
0100 |                     } catch (Exception e) {
0101 |                         a.b.a.a.a.c.a.s(z, "UploaderEngine", "addParamsToEvent e: ", e);
0102 |                         a.b.a.a.a.c.a.y0("sa", "s7", e);
0103 |                         return;
0104 |                     }
0105 |                 }
0106 |                 str = "body has tz_content field, return!";
0107 |             }
0108 |             a.b.a.a.a.c.a.m(z, "UploaderEngine", str);
0109 |         }
0110 |     }
0111 | 
0112 |     public static boolean b(z zVar, byte[] bArr, String str, boolean z) {
0113 |         Objects.requireNonNull(zVar);
0114 |         try {
0115 |             ConcurrentHashMap<String, String> concurrentHashMap = a.b.a.e.y.f663c;
0116 |             String strL = a.b.a.e.i0.c.n().l(a.b.a.e.y.b.f667a.e(), bArr, "onetrack_active".equals(str));
0117 |             a.b.a.e.l0.p.e(str, "UploaderEngine", "doUpload response: " + strL, z);
0118 |             if (!TextUtils.isEmpty(strL)) {
0119 |                 return zVar.g(strL).f402a;
0120 |             }
0121 |         } catch (Exception e) {
0122 |             a.b.a.e.l0.p.h("UploaderEngine", "doUpload get exception, ", e);
0123 |         }
0124 |         return false;
0125 |     }
0126 | 
0127 |     public static z e() {
0128 |         if (f397b == null) {
0129 |             synchronized (z.class) {
0130 |                 if (f397b == null) {
0131 |                     f397b = new z();
0132 |                 }
0133 |             }
0134 |         }
0135 |         return f397b;
0136 |     }
0137 | 
0138 |     public byte[] c(byte[] bArr) {
0139 |         if (bArr == null) {
0140 |             a.b.a.e.l0.p.g("UploaderEngine", "content is null");
0141 |             return null;
0142 |         }
0143 |         int i = a.b.a.e.g0.e.e;
0144 |         return a.b.a.e.g0.a.e(bArr, a.b.a.e.g0.c.a(a.b.a.e.g0.e.b.f436a.a(false)[0]));
0145 |     }
0146 | 
0147 |     public synchronized void d(int i) {
0148 |         y yVar = this.f398a;
0149 |         if (yVar != null) {
0150 |             yVar.b(i);
0151 |         } else {
0152 |             a.b.a.e.l0.p.g("UploaderEngine", "*** impossible, upload timer should not be null");
0153 |         }
0154 |     }
0155 | 
0156 |     public final String f(JSONArray jSONArray) {
0157 |         try {
0158 |             if (a.b.a.e.e0.d.b().e()) {
0159 |                 for (int i = 0; i < jSONArray.length(); i++) {
0160 |                     a.b.a.e.l0.i.a(jSONArray.getJSONObject(i));
0161 |                 }
0162 |             }
0163 |             return jSONArray.toString();
0164 |         } catch (Exception e) {
0165 |             a.b.a.e.l0.p.h("UploaderEngine", "getPayload error: ", e);
0166 |             return null;
0167 |         }
0168 |     }
0169 | 
0170 |     public b g(String str) {
0171 |         boolean z;
0172 |         boolean z2 = false;
0173 |         b bVar = new b(false, false);
0174 |         try {
0175 |             JSONObject jSONObject = new JSONObject(str);
0176 |             if (jSONObject.has("refresh_cfg")) {
0177 |                 j(jSONObject.getJSONObject("refresh_cfg"));
0178 |             }
0179 |             int iOptInt = jSONObject.optInt("code");
0180 |             long jOptLong = jSONObject.optLong("delay");
0181 |             if (jOptLong <= 0 || jOptLong >= System.currentTimeMillis() + 172800000) {
0182 |                 z = false;
0183 |             } else {
0184 |                 a.b.a.e.l0.u.l("forbidden_upload_until", System.currentTimeMillis() + jOptLong);
0185 |                 z = true;
0186 |             }
0187 |             if (iOptInt == 0) {
0188 |                 a.b.a.e.l0.p.c("UploaderEngine", "成功发送数据到服务端");
0189 |                 if (a.b.a.e.l0.r.q() && jSONObject.has("ot_ri")) {
0190 |                     a.b.a.e.e0.m.b.f308a.e(jSONObject.optString("ot_ri"));
0191 |                 }
0192 |                 if (!a.b.a.e.l0.r.q() && jSONObject.has("log")) {
0193 |                     a.b.a.e.j0.a.a.a().b(jSONObject.optJSONObject("log"));
0194 |                 }
0195 |                 if (!a.b.a.b.a.f()) {
0196 |                     a.b.a.e.e0.a aVarD = a.b.a.e.e0.a.d();
0197 |                     Objects.requireNonNull(aVarD);
0198 |                     a.b.a.e.e0.b bVar2 = new a.b.a.e.e0.b(aVarD, jSONObject);
0199 |                     ThreadPoolExecutor threadPoolExecutor = a.b.a.e.l0.g.f580a;
0200 |                     try {
0201 |                         a.b.a.e.l0.g.f580a.execute(bVar2);
0202 |                     } catch (Throwable unused) {
0203 |                     }
0204 |                 }
0205 |                 z2 = true;
0206 |             } else if (iOptInt == -3) {
0207 |                 a.b.a.e.l0.p.g("UploaderEngine", "signature expired, will update");
0208 |                 int i = a.b.a.e.g0.e.e;
0209 |                 a.b.a.e.g0.e.b.f436a.c(false);
0210 |             } else {
0211 |                 a.b.a.e.l0.p.g("UploaderEngine", "Error: status code=" + iOptInt);
0212 |             }
0213 |             return new b(z2, z);
0214 |         } catch (Exception e) {
0215 |             a.b.a.e.l0.p.h("UploaderEngine", "parseUploadingResult exception ", e);
0216 |             a.b.a.a.a.c.a.y0("up", "s9", e);
0217 |             return bVar;
0218 |         }
0219 |     }
0220 | 
```

## A12

Source: `sources/a/b/a/e/y.java`

Source-file SHA-256: `427a5b962d517b94381399a5a3ff8efd4f0fda7428debd49867b1204529c74f0`

Original lines 1–200:

```text
0001 | package a.b.a.e;
0002 | 
0003 | import android.text.TextUtils;
0004 | import java.util.Iterator;
0005 | import java.util.Locale;
0006 | import java.util.concurrent.ConcurrentHashMap;
0007 | import org.json.JSONException;
0008 | import org.json.JSONObject;
0009 | 
0010 | /* JADX INFO: loaded from: classes.dex */
0011 | public class y {
0012 | 
0013 |     /* JADX INFO: renamed from: c, reason: collision with root package name */
0014 |     public static ConcurrentHashMap<String, String> f663c = new ConcurrentHashMap<>();
0015 | 
0016 |     /* JADX INFO: renamed from: d, reason: collision with root package name */
0017 |     public static ConcurrentHashMap<String, ConcurrentHashMap<String, String>> f664d = new ConcurrentHashMap<>();
0018 | 
0019 |     /* JADX INFO: renamed from: a, reason: collision with root package name */
0020 |     public volatile boolean f665a = false;
0021 | 
0022 |     /* JADX INFO: renamed from: b, reason: collision with root package name */
0023 |     public volatile boolean f666b = false;
0024 | 
0025 |     public static class b {
0026 | 
0027 |         /* JADX INFO: renamed from: a, reason: collision with root package name */
0028 |         public static final y f667a = new y(null);
0029 |     }
0030 | 
0031 |     public y(a aVar) {
0032 |         f663c.put("IN", "tracking.india.miui.com");
0033 |         f663c.put("RU", "tracking.rus.miui.com");
0034 |         try {
0035 |             String strH = a.b.a.e.l0.u.h("region_rul", "");
0036 |             if (!TextUtils.isEmpty(strH)) {
0037 |                 g(new JSONObject(strH));
0038 |             }
0039 |             String strH2 = a.b.a.e.l0.u.h("backup_host_data", "");
0040 |             if (TextUtils.isEmpty(strH2)) {
0041 |                 return;
0042 |             }
0043 |             f(new JSONObject(strH2));
0044 |         } catch (JSONException e) {
0045 |             e.printStackTrace();
0046 |         }
0047 |     }
0048 | 
0049 |     public String a(String str, String str2, String str3) {
0050 |         return str + str2 + str3;
0051 |     }
0052 | 
0053 |     public final String b(String str, String str2) {
0054 |         String str3;
0055 |         ConcurrentHashMap<String, String> concurrentHashMap = f664d.get(str);
0056 |         if (concurrentHashMap != null) {
0057 |             Class cls = a.b.a.e.l0.r.f609a;
0058 |             if (a.b.a.a.a.c.a.w0(str2)) {
0059 |                 str3 = "CN";
0060 |                 if (!"CN".equalsIgnoreCase(str2)) {
0061 |                     str3 = "RU";
0062 |                     if (!"RU".equalsIgnoreCase(str2)) {
0063 |                         if ("IN".equalsIgnoreCase(str2)) {
0064 |                             str3 = "IN";
0065 |                         } else {
0066 |                             str3 = a.b.a.e.l0.a.f560a.contains(str2.toUpperCase(Locale.US)) ? "EU" : "INTL";
0067 |                         }
0068 |                     }
0069 |                 }
0070 |             } else {
0071 |                 str3 = "";
0072 |             }
0073 |             String str4 = concurrentHashMap.get(str3);
0074 |             if (!TextUtils.isEmpty(str4)) {
0075 |                 return str4;
0076 |             }
0077 |         }
0078 |         if (!TextUtils.equals(str2, "IN")) {
0079 |             return null;
0080 |         }
0081 |         if ("tracking".equals(str)) {
0082 |             return "pn-tracking-bak.onetrack.mi.com";
0083 |         }
0084 |         if ("sdkconfig".equals(str)) {
0085 |             return "pn-sdkconfig-bak.onetrack.mi.com";
0086 |         }
0087 |         return null;
0088 |     }
0089 | 
0090 |     public final String c() {
0091 |         String strJ = a.b.a.e.l0.r.q() ? a.b.a.e.l0.r.j() : "CN";
0092 |         if (this.f666b) {
0093 |             String strB = b("sdkconfig", strJ);
0094 |             if (!TextUtils.isEmpty(strB)) {
0095 |                 return strB;
0096 |             }
0097 |         }
0098 |         if (!a.b.a.e.l0.r.q()) {
0099 |             return "sdkconfig.xiaomi.com";
0100 |         }
0101 |         if (TextUtils.equals(strJ, "IN")) {
0102 |             return "sdkconfig.india.xiaomi.com";
0103 |         }
0104 |         if (TextUtils.equals(strJ, "RU")) {
0105 |             return "sdkconfig.rus.xiaomi.com";
0106 |         }
0107 |         try {
0108 |             return a.b.a.e.l0.a.f560a.contains(strJ.toUpperCase()) ? "sdkconfig.eu.xiaomi.com" : "sdkconfig.intl.xiaomi.com";
0109 |         } catch (Exception e) {
0110 |             a.b.a.e.l0.p.h("RegionDomainManager", "getSdkConfigHost error: ", e);
0111 |             return "sdkconfig.intl.xiaomi.com";
0112 |         }
0113 |     }
0114 | 
0115 |     public final String d() {
0116 |         String strJ = a.b.a.e.l0.r.q() ? a.b.a.e.l0.r.j() : "CN";
0117 |         if (this.f665a) {
0118 |             String strB = b("tracking", strJ);
0119 |             if (!TextUtils.isEmpty(strB)) {
0120 |                 return strB;
0121 |             }
0122 |         }
0123 |         if (!a.b.a.e.l0.r.q()) {
0124 |             return "tracking.miui.com";
0125 |         }
0126 |         String str = f663c.get(strJ);
0127 |         if (!TextUtils.isEmpty(str)) {
0128 |             return str;
0129 |         }
0130 |         try {
0131 |             if (a.b.a.e.l0.a.f560a.contains(strJ.toUpperCase())) {
0132 |                 return "tracking.eu.miui.com";
0133 |             }
0134 |         } catch (Exception e) {
0135 |             a.b.a.e.l0.p.h("RegionDomainManager", "getTrackingHostname error: ", e);
0136 |         }
0137 |         return "tracking.intl.miui.com";
0138 |     }
0139 | 
0140 |     public String e() {
0141 |         try {
0142 |             if (TextUtils.isEmpty(a.b.a.e.l0.u.c())) {
0143 |                 a.b.a.e.e0.d.b().f();
0144 |             }
0145 |         } catch (Exception e) {
0146 |             a.b.a.e.l0.p.h("RegionDomainManager", "getTrackingUrl error: ", e);
0147 |         }
0148 |         return a("https://", d(), "/track/v4");
0149 |     }
0150 | 
0151 |     public synchronized void f(JSONObject jSONObject) {
0152 |         try {
0153 |             Iterator<String> itKeys = jSONObject.keys();
0154 |             while (itKeys.hasNext()) {
0155 |                 String next = itKeys.next();
0156 |                 JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject(next);
0157 |                 if (jSONObjectOptJSONObject != null) {
0158 |                     ConcurrentHashMap<String, String> concurrentHashMap = f664d.get(next);
0159 |                     if (concurrentHashMap == null) {
0160 |                         concurrentHashMap = new ConcurrentHashMap<>();
0161 |                         f664d.put(next, concurrentHashMap);
0162 |                     }
0163 |                     Iterator<String> itKeys2 = jSONObjectOptJSONObject.keys();
0164 |                     while (itKeys2.hasNext()) {
0165 |                         String next2 = itKeys2.next();
0166 |                         String strOptString = jSONObjectOptJSONObject.optString(next2);
0167 |                         if (!TextUtils.isEmpty(next2) && !TextUtils.isEmpty(strOptString)) {
0168 |                             concurrentHashMap.put(next2, strOptString);
0169 |                         }
0170 |                     }
0171 |                 }
0172 |             }
0173 |             a.b.a.e.l0.u.m("backup_host_data", jSONObject.toString());
0174 |         } catch (Exception e) {
0175 |             a.b.a.e.l0.p.g("RegionDomainManager", "updateBackupHostMap error: " + e.toString());
0176 |         }
0177 |         if (a.b.a.e.l0.p.f602a) {
0178 |             a.b.a.e.l0.p.c("RegionDomainManager", "backupHostMap:" + f664d.toString());
0179 |         }
0180 |     }
0181 | 
0182 |     public synchronized void g(JSONObject jSONObject) {
0183 |         try {
0184 |             Iterator<String> itKeys = jSONObject.keys();
0185 |             while (itKeys.hasNext()) {
0186 |                 String next = itKeys.next();
0187 |                 String strOptString = jSONObject.optString(next);
0188 |                 if (!TextUtils.isEmpty(next) && !TextUtils.isEmpty(strOptString)) {
0189 |                     f663c.put(next, strOptString);
0190 |                 }
0191 |             }
0192 |             a.b.a.e.l0.u.m("region_rul", new JSONObject(f663c).toString());
0193 |         } catch (Exception e) {
0194 |             a.b.a.e.l0.p.c("RegionDomainManager", "updateHostMap: " + e.toString());
0195 |         }
0196 |         if (a.b.a.e.l0.p.f602a) {
0197 |             a.b.a.e.l0.p.c("RegionDomainManager", "merge config:" + new JSONObject(f663c).toString());
0198 |         }
0199 |     }
0200 | }
```

## A13

Source: `sources/a/b/a/f/g/d.java`

Source-file SHA-256: `5ab3ab9106fddbcb01265601bc7b4722fd868453b5b2b3800cc28fc2b5ccb3d8`

Original lines 1–230:

```text
0001 | package a.b.a.f.g;
0002 | 
0003 | import a.b.a.e.g0.e;
0004 | import a.b.a.e.l0.i;
0005 | import a.b.a.e.l0.p;
0006 | import a.b.a.e.l0.r;
0007 | import a.b.a.e.l0.v;
0008 | import android.text.TextUtils;
0009 | import android.util.ArrayMap;
0010 | import java.util.ArrayList;
0011 | import java.util.HashMap;
0012 | import java.util.Iterator;
0013 | import java.util.List;
0014 | import java.util.Map;
0015 | import java.util.Objects;
0016 | import org.json.JSONArray;
0017 | import org.json.JSONException;
0018 | import org.json.JSONObject;
0019 | 
0020 | /* JADX INFO: loaded from: classes.dex */
0021 | public class d {
0022 | 
0023 |     /* JADX INFO: renamed from: a, reason: collision with root package name */
0024 |     public static ArrayMap<String, Integer> f727a = new ArrayMap<>();
0025 | 
0026 |     /* JADX INFO: renamed from: b, reason: collision with root package name */
0027 |     public static ArrayMap<String, Integer> f728b = new ArrayMap<>();
0028 | 
0029 |     public static class a {
0030 | 
0031 |         /* JADX INFO: renamed from: a, reason: collision with root package name */
0032 |         public boolean f729a;
0033 | 
0034 |         /* JADX INFO: renamed from: b, reason: collision with root package name */
0035 |         public boolean f730b;
0036 | 
0037 |         /* JADX INFO: renamed from: c, reason: collision with root package name */
0038 |         public boolean f731c;
0039 | 
0040 |         public a(boolean z, boolean z2) {
0041 |             this.f730b = false;
0042 |             this.f731c = false;
0043 |             this.f729a = z;
0044 |             this.f730b = z2;
0045 |         }
0046 | 
0047 |         public a(boolean z, boolean z2, boolean z3) {
0048 |             this.f730b = false;
0049 |             this.f731c = false;
0050 |             this.f729a = z;
0051 |             this.f730b = z2;
0052 |             this.f731c = z3;
0053 |         }
0054 |     }
0055 | 
0056 |     public static void a(String str, String str2, List<a.b.a.f.e.b> list) {
0057 |         ArrayMap<String, Integer> arrayMap;
0058 |         Integer num;
0059 |         ArrayMap<String, Integer> arrayMap2;
0060 |         if ("miui-analytics".equals(str) && a.b.a.g.f.b.f892b.contains(str2)) {
0061 |             Iterator<a.b.a.f.e.b> it = list.iterator();
0062 |             while (it.hasNext()) {
0063 |                 if (v.h(it.next().g)) {
0064 |                     if (f727a.get(str2) == null) {
0065 |                         arrayMap = f727a;
0066 |                         arrayMap.put(str2, 1);
0067 |                     } else {
0068 |                         num = f727a.get(str2);
0069 |                         arrayMap2 = f727a;
0070 |                         arrayMap2.put(str2, Integer.valueOf(num.intValue() + 1));
0071 |                     }
0072 |                 } else if (f728b.get(str2) == null) {
0073 |                     arrayMap = f728b;
0074 |                     arrayMap.put(str2, 1);
0075 |                 } else {
0076 |                     num = f728b.get(str2);
0077 |                     arrayMap2 = f728b;
0078 |                     arrayMap2.put(str2, Integer.valueOf(num.intValue() + 1));
0079 |                 }
0080 |             }
0081 |         }
0082 |     }
0083 | 
0084 |     public static List<a.b.a.f.e.b> b(List<a.b.a.f.e.b> list) throws Throwable {
0085 |         String str;
0086 |         if (list.size() == 0) {
0087 |             p.c("UploaderDispense", "messages is empty, return null");
0088 |             return null;
0089 |         }
0090 |         ArrayList arrayList = new ArrayList();
0091 |         try {
0092 |             int i = e.e;
0093 |             e eVar = e.b.f436a;
0094 |             String str2 = eVar.a(false)[1];
0095 |             String str3 = eVar.a(false)[0];
0096 |             if (!TextUtils.isEmpty(str2) && !TextUtils.isEmpty(str3)) {
0097 |                 HashMap map = new HashMap();
0098 |                 for (a.b.a.f.e.b bVar : list) {
0099 |                     Map<String, String> map2 = bVar.f;
0100 |                     if (map2 != null) {
0101 |                         if (map.get(map2) == null) {
0102 |                             map.put(map2, new JSONArray());
0103 |                         }
0104 |                         String string = bVar.e;
0105 |                         if (a.b.a.e.e0.d.b().e()) {
0106 |                             JSONObject jSONObject = new JSONObject(string);
0107 |                             i.a(jSONObject);
0108 |                             string = jSONObject.toString();
0109 |                         }
0110 |                         ((JSONArray) map.get(map2)).put(string);
0111 |                     }
0112 |                 }
0113 |                 for (Map.Entry entry : map.entrySet()) {
0114 |                     Map map3 = (Map) entry.getKey();
0115 |                     map3.put("pub_gzipencrypt", "true");
0116 |                     map3.put("pub_sid", str2);
0117 |                     JSONArray jSONArray = (JSONArray) entry.getValue();
0118 |                     if (jSONArray == null || jSONArray.length() == 0) {
0119 |                         str = "value is null, continue!";
0120 |                     } else {
0121 |                         String string2 = jSONArray.toString();
0122 |                         byte[] bArrU0 = a.b.a.a.a.c.a.U0(string2);
0123 |                         if (bArrU0 == null || bArrU0.length == 0) {
0124 |                             str = "zipData is null, continue!";
0125 |                         } else {
0126 |                             p.c("UploaderDispense", "zip before : " + string2.length() + ", zip after : " + bArrU0.length);
0127 |                             byte[] bArrE = a.b.a.e.g0.a.e(bArrU0, a.b.a.e.g0.c.a(str3));
0128 |                             if (bArrE != null && bArrE.length != 0) {
0129 |                                 arrayList.add(new a.b.a.f.e.b(-1L, null, null, null, a.b.a.e.g0.c.c(bArrE), map3, -1L));
0130 |                             }
0131 |                         }
0132 |                     }
0133 |                     p.s("UploaderDispense", str);
0134 |                 }
0135 |                 return arrayList;
0136 |             }
0137 |             return null;
0138 |         } catch (Exception e) {
0139 |             p.h("UploaderDispense", "sortByAttribute error: ", e);
0140 |             a.b.a.a.a.c.a.y0("up", "s23", e);
0141 |         }
0142 |     }
0143 | 
0144 |     public static a c(a.b.a.f.c.c cVar, String str, String str2, List<a.b.a.f.e.b> list) {
0145 |         try {
0146 |             if (!TextUtils.isEmpty(str) && !TextUtils.isEmpty(str2)) {
0147 |                 if (cVar != null && !TextUtils.isEmpty(cVar.f683c)) {
0148 |                     String strD = d(list);
0149 |                     if (TextUtils.isEmpty(strD)) {
0150 |                         p.s("UploaderDispense", "build json failed!");
0151 |                         return new a(false, false);
0152 |                     }
0153 |                     byte[] bytes = strD.getBytes("UTF-8");
0154 |                     String str3 = String.format("%s/v1/projects/%s/topics/%s:publish", "https://pubsub.googleapis.com", str, str2);
0155 |                     f();
0156 |                     c cVarK = a.b.a.e.i0.c.n().k(str3, bytes, cVar.f683c);
0157 |                     if (cVarK == null) {
0158 |                         p.s("UploaderDispense", "upload failed for IOException");
0159 |                         return new a(false, false, true);
0160 |                     }
0161 |                     int i = cVarK.f723a;
0162 |                     if (i == 401) {
0163 |                         cVar.i = true;
0164 |                         a.b.a.f.c.d dVarB = a.b.a.f.c.d.b();
0165 |                         Objects.requireNonNull(dVarB);
0166 |                         int i2 = a.b.a.f.c.d.f687d + 1;
0167 |                         a.b.a.f.c.d.f687d = i2;
0168 |                         if (i2 >= 2) {
0169 |                             dVarB.a();
0170 |                         }
0171 |                         dVarB.c();
0172 |                         return c(a.b.a.f.c.b.e().b(), str, str2, list);
0173 |                     }
0174 |                     if (i == 404) {
0175 |                         if ("NOT_FOUND".equals(cVarK.f726d) && !TextUtils.isEmpty(cVarK.f725c) && (cVarK.f725c.contains(str) || cVarK.f725c.contains(str2))) {
0176 |                             return new a(false, true);
0177 |                         }
0178 |                     } else if (i == 200) {
0179 |                         a.b.a.f.c.d dVarB2 = a.b.a.f.c.d.b();
0180 |                         Objects.requireNonNull(dVarB2);
0181 |                         a.b.a.f.c.d.f686c = 0;
0182 |                         a.b.a.f.c.d.f687d = 0;
0183 |                         a.b.a.f.c.d.f685b = 0L;
0184 |                         dVarB2.c();
0185 |                         return new a(true, false);
0186 |                     }
0187 |                     return new a(false, false);
0188 |                 }
0189 |                 p.s("UploaderDispense", "token or access_token is null!");
0190 |                 return new a(false, false);
0191 |             }
0192 |             p.s("UploaderDispense", "projectID or topic is null!");
0193 |             return new a(false, false);
0194 |         } catch (Exception e) {
0195 |             p.h("UploaderDispense", "Exception while uploading ", e);
0196 |             a.b.a.a.a.c.a.y0("up", "s24", e);
0197 |         }
0198 |     }
0199 | 
0200 |     public static String d(List<a.b.a.f.e.b> list) {
0201 |         if (list == null) {
0202 |             return "";
0203 |         }
0204 |         try {
0205 |             JSONObject jSONObject = new JSONObject();
0206 |             JSONArray jSONArray = new JSONArray();
0207 |             for (a.b.a.f.e.b bVar : list) {
0208 |                 JSONObject jSONObject2 = new JSONObject();
0209 |                 Map<String, String> map = bVar.f;
0210 |                 if (map != null) {
0211 |                     JSONObject jSONObject3 = new JSONObject();
0212 |                     for (Map.Entry<String, String> entry : map.entrySet()) {
0213 |                         if (!TextUtils.isEmpty(entry.getKey())) {
0214 |                             jSONObject3.put(entry.getKey(), entry.getValue());
0215 |                         }
0216 |                     }
0217 |                     jSONObject2.put("attributes", jSONObject3);
0218 |                 }
0219 |                 jSONObject2.put("data", bVar.e);
0220 |                 jSONArray.put(jSONObject2);
0221 |             }
0222 |             jSONObject.put("messages", jSONArray);
0223 |             return jSONObject.toString();
0224 |         } catch (Exception unused) {
0225 |             return "";
0226 |         }
0227 |     }
0228 | 
0229 |     public static void e(List<a.b.a.f.e.b> list) {
0230 |         if (p.f602a) {
```

## A14

Source: `resources/res/xml/remote_config_defaults.xml`

Source-file SHA-256: `abb7d784080e16d585b230f01dc4d46564b23a2248fb3015270cc465751f8bd1`

Original lines 1–75:

```text
0001 | <?xml version="1.0" encoding="utf-8"?>
0002 | <defaultsMap>
0003 |     <entry>
0004 |         <key>onetrack_active
0005 |         </key>
0006 |         <value>{&quot;onetrack_disable_log&quot;:false,&quot;firebase_disable_log&quot;:false}
0007 |         </value>
0008 |     </entry>
0009 |     <entry>
0010 |         <key>onetrack_usage
0011 |         </key>
0012 |         <value>{&quot;onetrack_disable_log&quot;:true,&quot;firebase_disable_log&quot;:false}
0013 |         </value>
0014 |     </entry>
0015 |     <entry>
0016 |         <key>onetrack_app_stat
0017 |         </key>
0018 |         <value>{&quot;onetrack_disable_log&quot;:true,&quot;firebase_disable_log&quot;:false}
0019 |         </value>
0020 |     </entry>
0021 |     <entry>
0022 |         <key>onetrack_bug_report
0023 |         </key>
0024 |         <value>{&quot;onetrack_disable_log&quot;:true,&quot;firebase_disable_log&quot;:false}
0025 |         </value>
0026 |     </entry>
0027 |     <entry>
0028 |         <key>onetrack_installed_app
0029 |         </key>
0030 |         <value>{&quot;onetrack_disable_log&quot;:true,&quot;firebase_disable_log&quot;:false}
0031 |         </value>
0032 |     </entry>
0033 |     <entry>
0034 |         <key>onetrack_id
0035 |         </key>
0036 |         <value>{&quot;onetrack_disable_log&quot;:true,&quot;firebase_disable_log&quot;:false}
0037 |         </value>
0038 |     </entry>
0039 |     <entry>
0040 |         <key>onetrack_custom
0041 |         </key>
0042 |         <value>{&quot;onetrack_disable_log&quot;:false,&quot;firebase_disable_log&quot;:false}
0043 |         </value>
0044 |     </entry>
0045 |     <entry>
0046 |         <key>onetrack_pull_config
0047 |         </key>
0048 |         <value>{&quot;onetrack_disable_log&quot;:false}
0049 |         </value>
0050 |     </entry>
0051 |     <entry>
0052 |         <key>analytics_ad
0053 |         </key>
0054 |         <value>{&quot;onetrack_disable_log&quot;:false,&quot;black_list&quot;:&quot;&quot;,&quot;white_list&quot;:&quot;&quot;}
0055 |         </value>
0056 |     </entry>
0057 |     <entry>
0058 |         <key>onetrack_app_update
0059 |         </key>
0060 |         <value>{&quot;onetrack_disable_log&quot;:false}
0061 |         </value>
0062 |     </entry>
0063 |     <entry>
0064 |         <key>onetrack_custom_exception
0065 |         </key>
0066 |         <value>{&quot;firebase_disable_log&quot;:false}
0067 |         </value>
0068 |     </entry>
0069 |     <entry>
0070 |         <key>onetrack_monitor
0071 |         </key>
0072 |         <value>{&quot;onetrack_disable_log&quot;:true,&quot;firebase_disable_log&quot;:false}
0073 |         </value>
0074 |     </entry>
0075 | </defaultsMap>
```

## A15

Source: `resources/res/xml/network_security_config.xml`

Source-file SHA-256: `1e9049e7e951184595011afe7a628965a8995868944795f7606bbb0ae6b30b4c`

Original lines 1–4:

```text
0001 | <?xml version="1.0" encoding="utf-8"?>
0002 | <network-security-config>
0003 |     <base-config cleartextTrafficPermitted="true"/>
0004 | </network-security-config>
```

## A16

Source: `sources/a/b/a/e/g0/a.java`

Source-file SHA-256: `a36d704144e5a6fbcba59104b6a7958088f3bc40d36469e8a7d432f940417a7f`

Original lines 70–125:

```text
0070 |             return cipher.doFinal(bArr);
0071 |         } catch (Exception e) {
0072 |             p.d("AES", "decrypt exception:", e);
0073 |             p.c("AES", "content len=" + bArr.length + ", passwd len=" + bArr2.length);
0074 |             return null;
0075 |         }
0076 |     }
0077 | 
0078 |     public static String c(String str, String str2) {
0079 |         byte[] bArrE = e(str.getBytes(), str2.getBytes());
0080 |         if (bArrE == null) {
0081 |             return "";
0082 |         }
0083 |         StringBuilder sb = new StringBuilder();
0084 |         for (byte b2 : bArrE) {
0085 |             String hexString = Integer.toHexString(b2 & 255);
0086 |             while (hexString.length() < 2) {
0087 |                 hexString = a.a.a.a.a.a("0", hexString);
0088 |             }
0089 |             sb.append(hexString);
0090 |         }
0091 |         return sb.toString();
0092 |     }
0093 | 
0094 |     public static String d(String str) {
0095 |         try {
0096 |             char[] charArray = (str + b.f424c).toCharArray();
0097 |             for (int i = 0; i < charArray.length; i++) {
0098 |                 for (int i2 = 0; i2 < charArray.length - 1; i2++) {
0099 |                     if (charArray[i] < charArray[i2]) {
0100 |                         char c2 = charArray[i];
0101 |                         charArray[i] = charArray[i2];
0102 |                         charArray[i2] = c2;
0103 |                     }
0104 |                 }
0105 |             }
0106 |             return d.b(new String(charArray));
0107 |         } catch (Exception e) {
0108 |             a.a.a.a.a.f(e, a.a.a.a.a.c("encode is err:"), "AES");
0109 |             return "";
0110 |         }
0111 |     }
0112 | 
0113 |     public static byte[] e(byte[] bArr, byte[] bArr2) {
0114 |         try {
0115 |             SecretKeySpec secretKeySpec = new SecretKeySpec(bArr2, "AES");
0116 |             Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
0117 |             cipher.init(1, secretKeySpec);
0118 |             return cipher.doFinal(bArr);
0119 |         } catch (Exception e) {
0120 |             p.h("AES", "encrypt exception:", e);
0121 |             a.b.a.a.a.c.a.y0("up", "s8", e);
0122 |             return null;
0123 |         }
0124 |     }
0125 | }
```

## A17

Source: `sources/a/b/a/e/g.java`

Source-file SHA-256: `71460a385872d18ba98379aab1932fd4831aa21afbbd5a72c02a8257a85c1e34`

Original lines 100–185:

```text
0100 |                 try {
0101 |                     try {
0102 |                         if (a.b.a.e.l0.r.q()) {
0103 |                             String strJ = a.b.a.e.l0.r.j();
0104 |                             a.b.a.e.e0.a.d().k(strJ);
0105 |                             if (a.b.a.e.l0.r.t(strJ)) {
0106 |                                 z = false;
0107 |                             } else {
0108 |                                 z = true;
0109 |                             }
0110 |                         } else {
0111 |                             z = false;
0112 |                         }
0113 |                     } catch (Throwable th) {
0114 |                         th = th;
0115 |                         if (z4) {
0116 |                             a.b.a.e.l0.j.a("usageEvent", 30000L);
0117 |                         }
0118 |                         throw th;
0119 |                     }
0120 |                 } catch (Exception e) {
0121 |                     try {
0122 |                         a.b.a.e.l0.p.g("FireBaseChecker", "isUseFirebaseTrack failed: " + e.getMessage());
0123 |                     } catch (Exception e2) {
0124 |                         e = e2;
0125 |                         a.b.a.e.l0.p.h("EventTrack", "Track usage event failed,", e);
0126 |                         a.b.a.a.a.c.a.y0("co", "s2", e);
0127 |                         if (!z4) {
0128 |                         }
0129 |                         a.b.a.e.l0.j.a("usageEvent", 30000L);
0130 |                     }
0131 |                 }
0132 |                 long jElapsedRealtime = SystemClock.elapsedRealtime();
0133 |                 long jCurrentTimeMillis = System.currentTimeMillis();
0134 |                 long j = 900000;
0135 |                 if (Math.abs(jCurrentTimeMillis - a.b.a.e.l0.u.g("last_check_collect_usage_time", 0L)) >= j) {
0136 |                     a.b.a.e.l0.u.a();
0137 |                     a.b.a.e.l0.u.f617b.putLong("last_check_collect_usage_time", jCurrentTimeMillis).commit();
0138 |                     z2 = true;
0139 |                 } else {
0140 |                     z2 = false;
0141 |                 }
0142 |                 if (!z2) {
0143 |                     try {
0144 |                         if (!this.f409a) {
0145 |                             a.b.a.e.l0.p.c("EventTrack", "Track usage event don't meet conditions, won't track, check cost: " + (SystemClock.elapsedRealtime() - jElapsedRealtime));
0146 |                             a.b.a.g.c.c.f(z, "et2", 1, "r2");
0147 |                             return;
0148 |                         }
0149 |                     } catch (Exception e3) {
0150 |                         e = e3;
0151 |                         a.b.a.e.l0.p.h("EventTrack", "Track usage event failed,", e);
0152 |                         a.b.a.a.a.c.a.y0("co", "s2", e);
0153 |                         if (!z4) {
0154 |                         }
0155 |                         a.b.a.e.l0.j.a("usageEvent", 30000L);
0156 |                     }
0157 |                 }
0158 |                 a.b.a.g.c.c.f(z, "et2", 1, "triggered");
0159 |                 long jCurrentTimeMillis2 = System.currentTimeMillis();
0160 |                 long j2 = jCurrentTimeMillis2 - j;
0161 |                 long jG = a.b.a.e.l0.u.g("last_upload_usage_time", j2);
0162 |                 if (jCurrentTimeMillis2 < jG || jCurrentTimeMillis2 - jG > 86400000) {
0163 |                     a.b.a.e.l0.p.c("EventTrack", "usage change beginTime :" + j2);
0164 |                     z3 = false;
0165 |                 } else {
0166 |                     j2 = jG;
0167 |                     z3 = true;
0168 |                 }
0169 |                 if (a.b.a.b.a.a("onetrack_usage", "et2", z)) {
0170 |                     a.b.a.f.b.e().r(j2, jCurrentTimeMillis2);
0171 |                 }
0172 |                 if (a.b.a.b.a.e("onetrack_usage")) {
0173 |                     a.b.a.e.l0.u.a();
0174 |                     a.b.a.e.l0.u.f617b.putLong("last_upload_usage_time", jCurrentTimeMillis2).commit();
0175 |                     return;
0176 |                 }
0177 |                 if (!a.b.a.b.a.c("onetrack_usage")) {
0178 |                     a.b.a.e.l0.p.c("EventTrack", "The usage event is not allowed to track, because the switch to Send usage and diagnostic data is turned off");
0179 |                     a.b.a.g.c.c.f(false, "et2", 1, "r3");
0180 |                     return;
0181 |                 }
0182 |                 synchronized (a.b.a.a.a.c.a.class) {
0183 |                     yVar = new a.b.a.e.h0.y(j2, jCurrentTimeMillis2, z3);
0184 |                 }
0185 |                 boolean z5 = yVar.g >= 80;
```
