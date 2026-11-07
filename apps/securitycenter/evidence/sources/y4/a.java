package y4;

import android.content.Context;
import android.util.Log;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes2.dex */
public class a {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private static Object f37451a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private static Class<?> f37452b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private static Method f37453c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private static Method f37454d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    private static Method f37455e;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    private static Method f37456f;

    static {
        try {
            Class<?> cls = Class.forName("com.android.id.impl.IdProviderImpl");
            f37452b = cls;
            f37451a = cls.newInstance();
            f37453c = f37452b.getMethod("getUDID", Context.class);
            f37454d = f37452b.getMethod("getOAID", Context.class);
            f37455e = f37452b.getMethod("getVAID", Context.class);
            f37456f = f37452b.getMethod("getAAID", Context.class);
        } catch (Exception e10) {
            Log.e("IdentifierManager", "reflect exception!", e10);
        }
    }

    public static String a(Context context) {
        return c(context, f37454d);
    }

    public static String b(Context context) {
        return c(context, f37455e);
    }

    private static String c(Context context, Method method) {
        Object obj = f37451a;
        if (obj == null || method == null) {
            return null;
        }
        try {
            Object objInvoke = method.invoke(obj, context);
            if (objInvoke != null) {
                return (String) objInvoke;
            }
            return null;
        } catch (Exception e10) {
            Log.e("IdentifierManager", "invoke exception!", e10);
            return null;
        }
    }

    public static boolean d() {
        return (f37452b == null || f37451a == null) ? false : true;
    }
}
