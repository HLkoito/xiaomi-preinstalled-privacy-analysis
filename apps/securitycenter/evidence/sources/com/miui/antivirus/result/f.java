package com.miui.antivirus.result;

import android.text.TextUtils;
import com.miui.luckymoney.config.Constants;
import com.miui.networkassistant.ui.activity.NetworkDiagnosticsTipActivity;
import com.miui.permission.PermissionContract;
import com.miui.securitycenter.Application;
import com.miui.securitycenter.R;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import miui.cloud.CloudPushConstants;
import miui.os.Build;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public class f implements Serializable {

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    private static int f10489i = 2;

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    private static int f10490j = 1;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private String f10492b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private String f10493c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private String f10494d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    private boolean f10495e;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    private String f10496f;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    private String f10497g;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private List<c> f10491a = new ArrayList();

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    private boolean f10498h = false;

    private static void a(JSONArray jSONArray, k kVar) throws JSONException {
        int length = jSONArray.length();
        for (int i10 = 0; i10 < length; i10++) {
            JSONObject jSONObject = jSONArray.getJSONObject(i10);
            String strOptString = jSONObject.optString("type");
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("data");
            if ("003".equals(strOptString)) {
                k kVarD = k.d(jSONObjectOptJSONObject);
                if (kVar != null && kVarD != null) {
                    kVar.b(kVarD);
                }
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private static void b(JSONArray jSONArray, ArrayList<JSONObject> arrayList, Set<String> set, f fVar, String str, e eVar, boolean z10) throws JSONException {
        int i10;
        JSONArray jSONArrayOptJSONArray;
        int i11 = 1;
        boolean z11 = eVar != null;
        boolean z12 = eVar != null && eVar.l();
        String string = str;
        int i12 = 0;
        while (i12 < jSONArray.length()) {
            boolean z13 = ((i12 != 0 || z12) && z11) ? 0 : i11;
            boolean z14 = (i12 == jSONArray.length() - i11 || !z11) ? i11 : 0;
            JSONObject jSONObject = jSONArray.getJSONObject(i12);
            String strOptString = jSONObject.optString("type");
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("data");
            if ("002".equals(strOptString)) {
                g gVarI = i(g.e(jSONObjectOptJSONObject), arrayList, set);
                if (gVarI != null) {
                    gVarI.setTop(z13);
                    gVarI.setBottom(z14);
                    fVar.c(gVarI);
                }
                i10 = i11;
            } else if ("001".equals(strOptString) || "0010".equals(strOptString)) {
                int iOptInt = jSONObjectOptJSONObject.optInt("template");
                if (!(i12 == 0 && iOptInt == 70) && z10) {
                    i10 = 1;
                    if (f10490j == 1) {
                        if (!Build.IS_INTERNATIONAL_BUILD && iOptInt != 109) {
                            int i13 = f10490j + 1;
                            f10490j = i13;
                            if (i13 == f10489i + 1) {
                                f10490j = 1;
                            }
                        }
                        c cVarF = b.f(jSONObjectOptJSONObject, "");
                        if (cVarF != null) {
                            if (cVarF instanceof b) {
                                b bVar = (b) cVarF;
                                bVar.w(fVar.f10494d);
                                cVarF.setTop(z13);
                                cVarF.setBottom(z14);
                                bVar.s("");
                            }
                            d(fVar, eVar, cVarF);
                            if (iOptInt == 109 && eVar != null) {
                                eVar.c(cVarF);
                            }
                        }
                    }
                } else {
                    i10 = 1;
                }
            } else {
                if ("003".equals(strOptString)) {
                    k kVarD = k.d(jSONObjectOptJSONObject);
                    if (kVarD != null) {
                        kVarD.setTop(z13);
                        kVarD.setBottom(z14);
                        fVar.c(kVarD);
                    }
                } else if ("004".equals(strOptString)) {
                    if (TextUtils.isEmpty(string)) {
                        string = Application.A().getString(R.string.hot_today);
                    }
                    l lVar = new l(jSONObjectOptJSONObject);
                    lVar.e(string);
                    lVar.setTop(z13);
                    lVar.setBottom(z14);
                    d(fVar, eVar, lVar);
                } else if ("card".equals(jSONObject.optString("rowType")) && e.j(jSONObject.optInt("template"))) {
                    JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray("list");
                    e eVar2 = new e(jSONObject);
                    if (jSONArrayOptJSONArray2 != null && jSONArrayOptJSONArray2.length() > 0) {
                        if (eVar2.h() == 5) {
                            k kVar = new k();
                            kVar.j(1001);
                            a(jSONArrayOptJSONArray2, kVar);
                            if (kVar.f() >= 3) {
                                kVar.setTop(false);
                                kVar.setBottom(true);
                                fVar.c(eVar2);
                                fVar.c(kVar);
                            }
                        } else {
                            if (eVar2.l()) {
                                fVar.c(eVar2);
                            }
                            b(jSONArrayOptJSONArray2, arrayList, set, fVar, jSONObject.optString(NetworkDiagnosticsTipActivity.TITLE_KEY_NAME), eVar2, z10);
                        }
                    }
                    if (eVar2.k() && (jSONArrayOptJSONArray = jSONObject.optJSONArray(Constants.JSON_KEY_MODULE)) != null) {
                        if (jSONArrayOptJSONArray.length() == 1) {
                            d dVar = new d(jSONObject);
                            dVar.a(eVar2.f());
                            fVar.c(dVar);
                        } else {
                            i10 = 1;
                        }
                    }
                }
                i10 = 1;
            }
            i12++;
            i11 = i10;
        }
    }

    private void c(c cVar) {
        if (cVar != null) {
            cVar.setTestKey(this.f10496f);
            this.f10491a.add(cVar);
        }
    }

    private static void d(f fVar, e eVar, c cVar) {
        if (eVar == null) {
            fVar.c(cVar);
            return;
        }
        if (eVar.k()) {
            if (eVar.i() < eVar.g()) {
                fVar.c(cVar);
            }
            eVar.c(cVar);
            cVar.setTestKey(fVar.j());
        } else {
            fVar.c(cVar);
        }
        if (cVar instanceof l) {
            ((l) cVar).d(eVar.f());
        }
    }

    public static f f(JSONObject jSONObject, boolean z10) {
        f fVar = new f();
        fVar.f10492b = jSONObject.optString("type");
        fVar.f10493c = jSONObject.optString(Constants.JSON_KEY_DATA_VERSION);
        fVar.f10494d = jSONObject.optString("layoutId");
        fVar.f10495e = jSONObject.optInt("status") == 1;
        fVar.f10496f = jSONObject.optString("tn");
        fVar.f10497g = jSONObject.optString("lang", "");
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("data");
        if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() == 0) {
            return null;
        }
        JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray("functions");
        ArrayList arrayList = new ArrayList(0);
        HashSet hashSet = new HashSet(0);
        if (jSONArrayOptJSONArray2 != null) {
            for (int i10 = 0; i10 < jSONArrayOptJSONArray2.length(); i10++) {
                arrayList.add(jSONArrayOptJSONArray2.getJSONObject(i10));
            }
            for (int i11 = 0; i11 < jSONArrayOptJSONArray.length(); i11++) {
                JSONObject jSONObject2 = jSONArrayOptJSONArray.getJSONObject(i11);
                String strOptString = jSONObject2.optString("type");
                JSONObject jSONObjectOptJSONObject = jSONObject2.optJSONObject("data");
                if ("002".equals(strOptString)) {
                    hashSet.add(jSONObjectOptJSONObject.optString("functionId"));
                }
            }
        }
        b(jSONArrayOptJSONArray, arrayList, hashSet, fVar, null, null, z10);
        return fVar;
    }

    public static g i(g gVar, ArrayList<JSONObject> arrayList, Set<String> set) {
        g gVarE;
        JSONObject jSONObject = null;
        if (gVar == null) {
            while (arrayList.size() > 0) {
                JSONObject jSONObjectRemove = arrayList.remove(0);
                if (!set.contains(jSONObjectRemove.optString("functionId")) && (gVarE = g.e(jSONObjectRemove)) != null) {
                    return gVarE;
                }
            }
            return null;
        }
        for (JSONObject jSONObject2 : arrayList) {
            if (jSONObject2.optString("functionId").equals(String.valueOf(gVar.g()))) {
                jSONObject = jSONObject2;
                break;
            }
        }
        if (jSONObject != null) {
            arrayList.remove(jSONObject);
        }
        return gVar;
    }

    public static String l(Map<String, String> map) {
        String str;
        if (map == null) {
            map = new HashMap<>();
        }
        Application applicationA = Application.A();
        if (Build.IS_INTERNATIONAL_BUILD) {
            map.put("channel", "01-16");
            str = "nt";
        } else {
            map.put("channel", "01-10");
            map.put("deviceId", q2.a.a(applicationA));
            map.put("landingPageUrlType", com.miui.common.c.a(Application.A()));
            map.put("ua", x4.a0.o());
            str = "vs_version";
        }
        map.put(str, "1");
        boolean zV = pf.g.v();
        if (!pf.g.w()) {
            map.put(PermissionContract.Method.ReadClipboardTipSetting.EXTRA_SETTING, CloudPushConstants.CHANNEL_ID);
        } else if (zV) {
            map.put(PermissionContract.Method.ReadClipboardTipSetting.EXTRA_SETTING, "1");
        }
        return xe.a.f(map, "https://adv.sec.miui.com/info/layout", eg.j.b.POST, "2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9", new p4.i("antivirus_datamodel_post"));
    }

    public boolean e() {
        String str = this.f10497g;
        return str != null && str.equalsIgnoreCase(Locale.getDefault().toString());
    }

    public String g() {
        return this.f10493c;
    }

    public List<c> h() {
        return this.f10491a;
    }

    public String j() {
        return this.f10496f;
    }

    public boolean k() {
        return this.f10495e;
    }

    public void m(String str) {
        this.f10493c = str;
    }

    public void n(List<c> list) {
        this.f10491a = list;
    }

    public void o(String str) {
        this.f10492b = str;
    }
}
