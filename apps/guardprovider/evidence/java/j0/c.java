package j0;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import com.miui.guardprovider.GuardApplication;
import com.xiaomi.onetrack.OneTrack;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public abstract class c {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private static final String f5352a = "MIUI-" + Build.VERSION.INCREMENTAL;

    public static String[] a() {
        try {
            String strB = b(g(GuardApplication.f1743d));
            if (TextUtils.isEmpty(strB)) {
                return new String[0];
            }
            a.a(GuardApplication.f1743d).b(strB);
            JSONObject jSONObject = new JSONObject(strB);
            if (jSONObject.has("error_code")) {
                return new String[0];
            }
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("data");
            ArrayList arrayList = new ArrayList();
            if (jSONArrayOptJSONArray != null) {
                for (int i6 = 0; i6 < jSONArrayOptJSONArray.length(); i6++) {
                    JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i6);
                    if (jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.optInt("basisOf") == 2) {
                        arrayList.add(jSONObjectOptJSONObject.optString(OneTrack.Param.PKG));
                    }
                }
            }
            String[] strArr = new String[arrayList.size()];
            for (int i7 = 0; i7 < arrayList.size(); i7++) {
                strArr[i7] = (String) arrayList.get(i7);
            }
            return strArr;
        } catch (Exception e7) {
            Log.e("AntiDefraudAppManager", "getAllDetectUnsafeApps error, ", e7);
            return new String[0];
        }
    }

    public static String b(List list) {
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            jSONObject.put("os", f5352a);
            jSONObject.put("biz_id", "virus_scan");
            jSONObject.put("uuid", w0.b.b());
            JSONArray jSONArray = new JSONArray();
            for (int i6 = 0; i6 < list.size(); i6++) {
                JSONObject jSONObject2 = new JSONObject();
                b bVar = (b) list.get(i6);
                jSONObject2.put(OneTrack.Param.PKG, bVar.b());
                jSONObject2.put("version", bVar.d());
                jSONObject2.put("signature", bVar.c());
                jSONObject2.put("appname", bVar.a());
                jSONArray.put(jSONObject2);
            }
            jSONObject.put("content", jSONArray);
            return h0.b.d(GuardApplication.f1743d, "https://flash.sec.miui.com/detect/app", jSONObject, "6988567a-4220-4b51-bc2d-ccdec27a74a1");
        } catch (Exception e7) {
            Log.e("AntiDefraudAppManager", "getAllUnSystemAppsStatus error, ", e7);
            return null;
        }
    }

    public static String c(String str) {
        if (a.a(GuardApplication.f1743d).c(str)) {
            return str;
        }
        return null;
    }

    public static String d(byte[] bArr) {
        char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(bArr);
            byte[] bArrDigest = messageDigest.digest();
            char[] cArr2 = new char[bArrDigest.length * 2];
            int i6 = 0;
            for (byte b7 : bArrDigest) {
                int i7 = i6 + 1;
                cArr2[i6] = cArr[(b7 >>> 4) & 15];
                i6 = i7 + 1;
                cArr2[i7] = cArr[b7 & 15];
            }
            return new String(cArr2);
        } catch (Exception e7) {
            Log.e("AntiDefraudAppManager", "getMessageDigest error, ", e7);
            return null;
        }
    }

    private static Signature[] e(Context context, String str) {
        if (str != null && str.length() != 0) {
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(str, 64);
                if (packageInfo == null) {
                    return null;
                }
                return packageInfo.signatures;
            } catch (PackageManager.NameNotFoundException e7) {
                Log.e("AntiDefraudAppManager", "getRawSignature error, ", e7);
            }
        }
        return null;
    }

    public static String f(Context context, String str) {
        Signature[] signatureArrE = e(context, str);
        if (signatureArrE == null || signatureArrE.length == 0) {
            return null;
        }
        return d(signatureArrE[0].toByteArray());
    }

    public static List g(Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);
        ArrayList arrayList = new ArrayList();
        for (PackageInfo packageInfo : installedPackages) {
            if ((packageInfo.applicationInfo.flags & 1) != 1) {
                String str = packageInfo.packageName;
                b bVar = new b();
                bVar.f(str);
                bVar.h(String.valueOf(packageInfo.versionCode));
                bVar.g(f(context, str));
                try {
                    bVar.e(URLEncoder.encode(packageInfo.applicationInfo.loadLabel(packageManager).toString(), "UTF-8"));
                } catch (Exception e7) {
                    Log.e("AntiDefraudAppManager", "getUnSystemAppList error, ", e7);
                }
                arrayList.add(bVar);
            }
        }
        return arrayList;
    }

    public static void h(Context context) {
        try {
            String strB = b(g(GuardApplication.f1743d));
            if (TextUtils.isEmpty(strB)) {
                return;
            }
            a.a(context).b(strB);
        } catch (Exception e7) {
            Log.e("AntiDefraudAppManager", "updateAllDetectApps error, ", e7);
        }
    }
}
