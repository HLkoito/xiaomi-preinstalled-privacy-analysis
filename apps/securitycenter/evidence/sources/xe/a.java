package xe;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.os.BatteryManager;
import android.util.Log;
import androidx.annotation.NonNull;
import bh.f;
import com.miui.common.e;
import eg.j;
import java.math.BigDecimal;
import java.util.Map;
import p4.i;
import x4.u1;

/* JADX INFO: loaded from: classes3.dex */
public class a {
    private static void a(@NonNull Context context, Map<String, String> map) {
        try {
            Object objH = f.h(Class.forName("android.provider.MiuiSettings$Ad"), "isPersonalizedAdEnabled", new Class[]{ContentResolver.class}, context.getContentResolver());
            if (objH instanceof Boolean) {
                map.put("isPersonalizedAdEnabled", String.valueOf(objH));
            }
        } catch (Exception unused) {
            Log.e("AdNetUtils", "addPersonalizedAdEnable: not support");
        }
    }

    private static void b(Map<String, String> map) {
        if (tl.a.f35470a) {
            return;
        }
        Application applicationD = e.d();
        a(applicationD, map);
        map.put("cv", "3");
        map.put("ror", String.valueOf(d(applicationD)));
        map.put("rbr", String.valueOf(c(applicationD)));
    }

    public static int c(Context context) {
        int intProperty = ((BatteryManager) context.getSystemService("batterymanager")).getIntProperty(4);
        if (intProperty == Integer.MIN_VALUE) {
            return -1;
        }
        return intProperty;
    }

    public static int d(Context context) {
        long jC = u1.c(context);
        long jA = u1.a();
        if (jC <= 0 || jA <= 0) {
            return -1;
        }
        return new BigDecimal(jA).divide(new BigDecimal(jC), 2, 4).multiply(new BigDecimal(100)).intValue();
    }

    public static String e(Map<String, String> map, String str, i iVar) {
        b(map);
        return j.t(map, str, iVar);
    }

    public static String f(Map<String, String> map, String str, j.b bVar, String str2, i iVar) {
        b(map);
        return j.B(map, str, bVar, str2, iVar);
    }
}
