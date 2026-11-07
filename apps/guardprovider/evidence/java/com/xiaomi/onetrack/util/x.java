package com.xiaomi.onetrack.util;

import android.content.Context;
import android.text.TextUtils;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class x {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private static final String f2752a = "RegionDomainManager";

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private static final String f2753b = "CN";

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private static final String f2754c = "INTL";

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private static final String f2755d = "IN";

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    private static final String f2756e = "RU";

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    private static final String f2757f = "http://";

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    private static final String f2758g = "https://";

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    private static final String f2759h = "tracking.miui.com";

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    private static final String f2760i = "tracking.intl.miui.com";

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    private static final String f2761j = "tracking.rus.miui.com";

    /* JADX INFO: renamed from: k, reason: collision with root package name */
    private static final String f2762k = "tracking.india.miui.com";

    /* JADX INFO: renamed from: l, reason: collision with root package name */
    private static final String f2763l = "sdkconfig.ad.xiaomi.com";

    /* JADX INFO: renamed from: m, reason: collision with root package name */
    private static final String f2764m = "sdkconfig.ad.intl.xiaomi.com";

    /* JADX INFO: renamed from: n, reason: collision with root package name */
    private static final String f2765n = "sdkconfig.ad.india.xiaomi.com";

    /* JADX INFO: renamed from: o, reason: collision with root package name */
    private static final String f2766o = "sdkconfig.ad.rus.xiaomi.com";

    /* JADX INFO: renamed from: p, reason: collision with root package name */
    private static final String f2767p = "/track/v4";

    /* JADX INFO: renamed from: q, reason: collision with root package name */
    private static final String f2768q = "/track/key_get";

    /* JADX INFO: renamed from: r, reason: collision with root package name */
    private static final String f2769r = "/api/v4/detail/config";

    /* JADX INFO: renamed from: s, reason: collision with root package name */
    private static final String f2770s = "/api/v4/detail/config_common";

    /* JADX INFO: renamed from: u, reason: collision with root package name */
    private static ConcurrentHashMap<String, String> f2771u = new ConcurrentHashMap<>();

    /* JADX INFO: renamed from: t, reason: collision with root package name */
    private Context f2772t;

    private static class a {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private static final x f2773a = new x();

        private a() {
        }
    }

    public static x a() {
        return a.f2773a;
    }

    private void f() {
        try {
            String strH = aa.h();
            if (TextUtils.isEmpty(strH)) {
                return;
            }
            a(new JSONObject(strH));
        } catch (JSONException e7) {
            e7.printStackTrace();
        }
    }

    private String g() {
        return f2758g;
    }

    private String h() {
        return a(q.h(), q.i());
    }

    private String i() {
        boolean zH = q.h();
        String strI = q.i();
        if (!zH) {
            return f2763l;
        }
        if (TextUtils.equals(strI, f2755d)) {
            return f2765n;
        }
        return TextUtils.equals(strI, f2756e) ? f2766o : f2764m;
    }

    public String b() {
        try {
            if (TextUtils.isEmpty(aa.l())) {
                com.xiaomi.onetrack.b.e.b();
            }
        } catch (Exception e7) {
            p.a(f2752a, "getTrackingUrl: " + e7.toString());
        }
        return a(g(), h(), f2767p);
    }

    public String c() {
        return a(g(), i(), f2769r);
    }

    public String d() {
        return a(g(), i(), f2770s);
    }

    public String e() {
        return a(g(), h(), f2768q);
    }

    private x() {
        f2771u.put(f2755d, f2762k);
        f2771u.put(f2756e, f2761j);
        f();
    }

    public synchronized void a(JSONObject jSONObject) {
        p.a(f2752a, "updateHostMap:" + jSONObject.toString());
        try {
            Iterator<String> itKeys = jSONObject.keys();
            while (itKeys.hasNext()) {
                String next = itKeys.next();
                String strOptString = jSONObject.optString(next);
                if (!TextUtils.isEmpty(next) && !TextUtils.isEmpty(strOptString)) {
                    f2771u.put(next, strOptString);
                }
            }
            aa.b(new JSONObject(f2771u).toString());
        } catch (Exception e7) {
            p.a(f2752a, "updateHostMap: " + e7.toString());
        }
        p.a(f2752a, "merge config:" + new JSONObject(f2771u).toString());
    }

    public String a(String str, String str2, String str3) {
        return str + str2 + str3;
    }

    private String a(boolean z6, String str) {
        if (!z6) {
            return f2759h;
        }
        String str2 = f2771u.get(str);
        return TextUtils.isEmpty(str2) ? f2760i : str2;
    }
}
