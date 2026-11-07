package com.xiaomi.onetrack.api;

import android.content.Context;
import com.xiaomi.onetrack.Configuration;
import com.xiaomi.onetrack.OneTrack;
import com.xiaomi.onetrack.ServiceQualityEvent;
import com.xiaomi.onetrack.util.DeviceUtil;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class c {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public static final String f2116a = "B";

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public static final String f2117b = "H";

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private static final String f2118c = "EventDataBuilder";

    public static String a(String str, JSONObject jSONObject, Configuration configuration, OneTrack.IEventHook iEventHook, JSONObject jSONObject2, com.xiaomi.onetrack.util.v vVar, boolean z6) throws JSONException {
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put(f2117b, com.xiaomi.onetrack.f.b.a(str, configuration, iEventHook, vVar, z6, false));
        jSONObject3.put(f2116a, com.xiaomi.onetrack.util.r.a(jSONObject, jSONObject2));
        return jSONObject3.toString();
    }

    public static String b(JSONObject jSONObject, Configuration configuration, OneTrack.IEventHook iEventHook, JSONObject jSONObject2, com.xiaomi.onetrack.util.v vVar, boolean z6) throws JSONException {
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put(f2117b, com.xiaomi.onetrack.f.b.a(b.f2093d, configuration, iEventHook, vVar, z6, false));
        jSONObject3.put(f2116a, com.xiaomi.onetrack.util.r.a(jSONObject, jSONObject2));
        return jSONObject3.toString();
    }

    public static String c(JSONObject jSONObject, Configuration configuration, OneTrack.IEventHook iEventHook, JSONObject jSONObject2, com.xiaomi.onetrack.util.v vVar, boolean z6) throws JSONException {
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put(f2117b, com.xiaomi.onetrack.f.b.a("ot_login", configuration, iEventHook, vVar, z6, false));
        jSONObject3.put(f2116a, com.xiaomi.onetrack.util.r.a(jSONObject, jSONObject2));
        return jSONObject3.toString();
    }

    public static String d(JSONObject jSONObject, Configuration configuration, OneTrack.IEventHook iEventHook, JSONObject jSONObject2, com.xiaomi.onetrack.util.v vVar, boolean z6) throws JSONException {
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put(f2117b, com.xiaomi.onetrack.f.b.a("ot_logout", configuration, iEventHook, vVar, z6, false));
        jSONObject3.put(f2116a, com.xiaomi.onetrack.util.r.a(jSONObject, jSONObject2));
        return jSONObject3.toString();
    }

    public static String a(d dVar, JSONObject jSONObject, Configuration configuration, OneTrack.IEventHook iEventHook, JSONObject jSONObject2, com.xiaomi.onetrack.util.v vVar, boolean z6) throws JSONException {
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put(f2117b, com.xiaomi.onetrack.f.b.a(dVar, configuration, iEventHook, vVar, z6, false));
        jSONObject3.put(f2116a, com.xiaomi.onetrack.util.r.a(jSONObject, jSONObject2));
        return jSONObject3.toString();
    }

    public static String a(String str, String str2, Configuration configuration, OneTrack.IEventHook iEventHook, JSONObject jSONObject, boolean z6, com.xiaomi.onetrack.util.v vVar, boolean z7) throws JSONException {
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put(f2117b, com.xiaomi.onetrack.f.b.a(str2, configuration, iEventHook, vVar, z7, false));
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("class", str);
        jSONObject3.put("type", 1);
        jSONObject3.put(b.f2109t, z6);
        jSONObject2.put(f2116a, com.xiaomi.onetrack.util.r.a(jSONObject3, jSONObject));
        return jSONObject2.toString();
    }

    public static String a(String str, String str2, long j6, Configuration configuration, OneTrack.IEventHook iEventHook, JSONObject jSONObject, com.xiaomi.onetrack.util.v vVar, boolean z6) throws JSONException {
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put(f2117b, com.xiaomi.onetrack.f.b.a(str2, configuration, iEventHook, vVar, z6, false));
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("class", str);
        jSONObject3.put("type", 2);
        jSONObject3.put("duration", j6);
        jSONObject2.put(f2116a, com.xiaomi.onetrack.util.r.a(jSONObject3, jSONObject));
        return jSONObject2.toString();
    }

    public static String a(String str, String str2, String str3, String str4, String str5, long j6, Configuration configuration, OneTrack.IEventHook iEventHook, JSONObject jSONObject, com.xiaomi.onetrack.util.v vVar, boolean z6) throws JSONException {
        JSONObject jSONObject2 = new JSONObject();
        JSONObject jSONObjectA = com.xiaomi.onetrack.f.b.a("onetrack_bug_report", configuration, iEventHook, vVar, z6, false);
        if (str5 != null) {
            jSONObjectA.put(com.xiaomi.onetrack.f.b.C0041b.f2430o, str5);
        }
        jSONObject2.put(f2117b, jSONObjectA);
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("exception", str);
        jSONObject3.put("type", str3);
        jSONObject3.put("message", str2);
        jSONObject3.put(b.f2103n, str4);
        jSONObject3.put(b.f2104o, j6);
        jSONObject2.put(f2116a, com.xiaomi.onetrack.util.r.a(jSONObject3, jSONObject));
        return jSONObject2.toString();
    }

    public static String a(Configuration configuration, OneTrack.IEventHook iEventHook, JSONObject jSONObject, com.xiaomi.onetrack.util.v vVar, boolean z6) throws JSONException {
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put(f2117b, com.xiaomi.onetrack.f.b.a("onetrack_dau", configuration, iEventHook, vVar, z6, false));
        JSONObject jSONObject3 = new JSONObject();
        Context contextB = com.xiaomi.onetrack.f.a.b();
        boolean zS = com.xiaomi.onetrack.util.aa.s();
        if (zS) {
            com.xiaomi.onetrack.util.aa.c(false);
        }
        jSONObject3.put("first_open", zS);
        if (!(com.xiaomi.onetrack.util.q.a() ? com.xiaomi.onetrack.util.q.h() : configuration.isInternational())) {
            if (configuration.isIMEIEnable()) {
                jSONObject3.put(b.f2114y, DeviceUtil.q(contextB));
            }
            if (configuration.isIMSIEnable()) {
                jSONObject3.put(b.f2115z, DeviceUtil.v(contextB));
            }
        }
        jSONObject3.put(b.C, a.a(configuration));
        jSONObject2.put(f2116a, com.xiaomi.onetrack.util.r.a(jSONObject3, jSONObject));
        return jSONObject2.toString();
    }

    public static String a(JSONObject jSONObject, Configuration configuration, OneTrack.IEventHook iEventHook, JSONObject jSONObject2, com.xiaomi.onetrack.util.v vVar, boolean z6) throws JSONException {
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put(f2117b, com.xiaomi.onetrack.f.b.a(b.f2092c, configuration, iEventHook, vVar, z6, false));
        jSONObject3.put(f2116a, com.xiaomi.onetrack.util.r.a(jSONObject, jSONObject2));
        return jSONObject3.toString();
    }

    public static String a(String str, String str2, JSONObject jSONObject, Configuration configuration, OneTrack.IEventHook iEventHook, JSONObject jSONObject2, com.xiaomi.onetrack.util.v vVar, boolean z6) throws JSONException {
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put(f2117b, com.xiaomi.onetrack.f.b.a(str2, configuration, iEventHook, str, vVar, z6, false));
        jSONObject3.put(f2116a, com.xiaomi.onetrack.util.r.a(jSONObject, jSONObject2));
        return jSONObject3.toString();
    }

    public static String a(Configuration configuration, OneTrack.IEventHook iEventHook, com.xiaomi.onetrack.util.v vVar, boolean z6) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put(f2117b, com.xiaomi.onetrack.f.b.a(b.f2098i, configuration, iEventHook, vVar, z6, false));
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put(b.f2105p, com.xiaomi.onetrack.c.i.b());
        jSONObject.put(f2116a, jSONObject2);
        return jSONObject.toString();
    }

    public static String a(ServiceQualityEvent serviceQualityEvent, Configuration configuration, OneTrack.IEventHook iEventHook, com.xiaomi.onetrack.util.v vVar, boolean z6) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put(f2117b, com.xiaomi.onetrack.f.b.a(b.f2094e, configuration, iEventHook, vVar, z6, false));
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put(b.D, serviceQualityEvent.getScheme());
        jSONObject2.put(b.E, serviceQualityEvent.getHost());
        jSONObject2.put(b.F, serviceQualityEvent.getPort());
        jSONObject2.put(b.G, serviceQualityEvent.getPath());
        jSONObject2.put(b.H, serviceQualityEvent.getIps());
        jSONObject2.put(b.I, serviceQualityEvent.getResponseCode());
        jSONObject2.put(b.J, serviceQualityEvent.getStatusCode());
        jSONObject2.put("exception", serviceQualityEvent.getExceptionTag());
        jSONObject2.put(b.L, serviceQualityEvent.getResultType());
        jSONObject2.put(b.M, serviceQualityEvent.getRetryCount());
        jSONObject2.put(b.N, serviceQualityEvent.getRequestTimestamp());
        jSONObject2.put(b.O, serviceQualityEvent.getRequestNetType());
        jSONObject2.put(b.P, serviceQualityEvent.getDnsLookupTime());
        jSONObject2.put(b.Q, serviceQualityEvent.getTcpConnectTime());
        jSONObject2.put(b.S, serviceQualityEvent.getHandshakeTime());
        jSONObject2.put(b.T, serviceQualityEvent.getReceiveFirstByteTime());
        jSONObject2.put(b.U, serviceQualityEvent.getReceiveAllByteTime());
        jSONObject2.put(b.R, serviceQualityEvent.getRequestDataSendTime());
        jSONObject2.put("duration", serviceQualityEvent.getDuration());
        jSONObject2.put(b.W, serviceQualityEvent.getNetSdkVersion());
        Map<String, Object> extraParams = serviceQualityEvent.getExtraParams();
        if (extraParams != null && extraParams.size() > 0) {
            for (Map.Entry<String, Object> entry : extraParams.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (com.xiaomi.onetrack.util.r.b(value)) {
                    jSONObject2.put(key, value);
                }
            }
        }
        jSONObject.put(f2116a, jSONObject2);
        return jSONObject.toString();
    }

    public static String a(long j6, String str, long j7, long j8, Configuration configuration, OneTrack.IEventHook iEventHook, com.xiaomi.onetrack.util.v vVar, boolean z6) {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put(f2117b, com.xiaomi.onetrack.f.b.a(b.f2099j, configuration, iEventHook, vVar, z6, false));
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put(b.X, j6);
        jSONObject2.put(b.Y, str);
        jSONObject2.put(b.Z, j7);
        jSONObject2.put(b.aa, j8);
        jSONObject.put(f2116a, jSONObject2);
        return jSONObject.toString();
    }

    public static String a(String str, JSONObject jSONObject, Configuration configuration, OneTrack.IEventHook iEventHook, JSONObject jSONObject2, com.xiaomi.onetrack.util.v vVar, JSONArray jSONArray, boolean z6) throws JSONException {
        JSONObject jSONObject3 = new JSONObject();
        JSONObject jSONObjectA = com.xiaomi.onetrack.f.b.a(str, configuration, iEventHook, vVar, z6, true);
        if (jSONArray != null && jSONArray.length() > 0) {
            jSONObjectA.put(com.xiaomi.onetrack.f.b.C0041b.H, jSONArray);
        }
        jSONObject3.put(f2117b, jSONObjectA);
        jSONObject3.put(f2116a, com.xiaomi.onetrack.util.r.a(jSONObject, jSONObject2));
        return jSONObject3.toString();
    }
}
