package ta;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.WorkerThread;
import com.miui.securitycenter.Application;
import com.xiaomi.onetrack.util.aa;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

/* JADX INFO: loaded from: classes2.dex */
public class d {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private static int f35364a = -1;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private static int f35365b = 1;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private static int f35366c = 0;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private static int f35367d = -1;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    private static String f35368e = "";

    public static ArrayList<String> a(ArrayList<String> arrayList) {
        Iterator<String> it = arrayList.iterator();
        while (it.hasNext()) {
            String next = it.next();
            if (!next.contains(aa.f23523b) || next.contains("-")) {
                it.remove();
            }
        }
        return arrayList;
    }

    @WorkerThread
    public static boolean b() {
        if (!f.d()) {
            Log.d("FboManagerUtils", "Fbo isCloud close!");
            return false;
        }
        String strC = f.c();
        if (strC == null) {
            strC = "";
        }
        if (!TextUtils.equals(strC, f35368e) || f35367d == f35364a) {
            f35367d = c(strC) ? f35365b : f35366c;
            f35368e = strC;
        }
        Log.i("FboManagerUtils", "System states " + f35367d);
        return f35367d == f35365b;
    }

    private static boolean c(String str) {
        Log.i("FboManagerUtils", "Fbo cloud rules = " + str);
        try {
            Object objH = bh.f.h(Class.forName("miui.fbo.FboManager"), "getInstance", new Class[]{Context.class}, Application.A());
            if (objH != null) {
                return ((Boolean) bh.f.b(objH, Boolean.TYPE, "FBO_new_isSupport", new Class[]{String.class}, str)).booleanValue();
            }
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e10) {
            e10.printStackTrace();
        }
        return false;
    }

    public static void d(ArrayList<String> arrayList, long j10) {
        String strB = ab.a.b();
        String str = strB + aa.f23523b + j10;
        int size = arrayList.size();
        for (int i10 = 0; i10 < size; i10++) {
            if (arrayList.get(i10).contains(strB)) {
                arrayList.set(i10, str);
                str = null;
                break;
            }
        }
        if (TextUtils.isEmpty(str)) {
            return;
        }
        if (arrayList.size() == 7) {
            arrayList.remove(0);
        }
        arrayList.add(str);
    }

    public static void e(String str, boolean z10) {
        try {
            Log.d("FboManagerUtils", "upLoadPath = " + str + ", isOver30Day = " + z10);
            Object objH = bh.f.h(Class.forName("miui.fbo.FboManager"), "getInstance", new Class[]{Context.class}, Application.A());
            if (objH != null) {
                bh.f.d(objH, "FBO_new_trigger", new Class[]{String.class, Boolean.TYPE}, str, Boolean.valueOf(z10));
            }
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e10) {
            e10.printStackTrace();
        }
    }
}
