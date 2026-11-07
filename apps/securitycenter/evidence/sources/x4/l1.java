package x4;

import android.content.Context;
import android.util.Log;
import com.miui.powercenter.bean.StatusBarGuideParams;
import miui.os.Build;
import miuix.core.util.SystemProperties;

/* JADX INFO: loaded from: classes2.dex */
public class l1 {
    public static Object a(String str, String str2) {
        try {
            return bh.f.m(Class.forName(str), str2);
        } catch (Exception e10) {
            Log.i("PrivacyUtils", e10.toString());
            return null;
        }
    }

    public static boolean b() {
        if (Build.IS_INTERNATIONAL_BUILD) {
            return c(com.miui.common.e.d(), StatusBarGuideParams.MY_PACKAGE_NAME) && SystemProperties.getBoolean((String) a("android.provider.MiuiSettings$System", "KEY_SECURITY_CENTER_ALLOW_CONNECT_NETWORK"), false);
        }
        return SystemProperties.getBoolean((String) a("android.provider.MiuiSettings$System", "KEY_SECURITY_CENTER_ALLOW_CONNECT_NETWORK"), false);
    }

    public static boolean c(Context context, String str) {
        try {
            return ((Boolean) bh.f.g(Class.forName("android.provider.MiuiSettings$Privacy"), Boolean.TYPE, "isEnabled", new Class[]{Context.class, String.class}, context, str)).booleanValue();
        } catch (Exception e10) {
            Log.i("PrivacyUtils", e10.toString());
            return false;
        }
    }
}
