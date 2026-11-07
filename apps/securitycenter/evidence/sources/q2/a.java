package q2;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import bh.f;
import com.google.android.exoplayer2.C;
import com.miui.securitycenter.Application;
import java.math.BigInteger;
import java.security.MessageDigest;

/* JADX INFO: loaded from: classes2.dex */
public class a {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public static String f33582a = a(Application.A());

    public static String a(Context context) {
        String str;
        String strC = eg.c.c(context);
        String strA = eg.c.a(context);
        if (Build.VERSION.SDK_INT < 28) {
            str = Build.SERIAL;
        } else {
            try {
                str = (String) f.g(Class.forName("android.os.Build"), String.class, "getSerial", new Class[0], new Object[0]);
            } catch (Exception e10) {
                Log.e("AdScoreAnalytics", "get serial number error", e10);
                str = "";
            }
        }
        return b(strC + strA + str);
    }

    private static String b(String str) {
        if (str == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            messageDigest.update(str.getBytes(C.UTF8_NAME));
            return String.format("%1$032X", new BigInteger(1, messageDigest.digest()));
        } catch (Exception unused) {
            return str;
        }
    }
}
