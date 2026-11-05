package com.xiaomi.onetrack.util;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.roach.nest.PowerNest;

/* JADX INFO: loaded from: classes.dex */
public class DeviceUtil {
    private static String A = null;
    private static String B = null;
    private static String C = null;
    private static String D = null;
    private static String E = null;
    private static Boolean F = null;
    private static String G = null;
    private static volatile boolean H = false;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    static final int f2482a = 15;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    static final int f2483b = 6;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private static final String f2484c = "DeviceUtil";

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private static final String f2485d = "";

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    private static final String f2486e = "ro.build.description";

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    private static final int f2487f = 15;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    private static final int f2488g = 14;

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    private static Method f2489h;

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    private static Method f2490i;

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    private static Method f2491j;

    /* JADX INFO: renamed from: k, reason: collision with root package name */
    private static Object f2492k;

    /* JADX INFO: renamed from: l, reason: collision with root package name */
    private static Method f2493l;

    /* JADX INFO: renamed from: m, reason: collision with root package name */
    private static Method f2494m;

    /* JADX INFO: renamed from: n, reason: collision with root package name */
    private static volatile String f2495n;

    /* JADX INFO: renamed from: o, reason: collision with root package name */
    private static volatile String f2496o;

    /* JADX INFO: renamed from: p, reason: collision with root package name */
    private static String f2497p;

    /* JADX INFO: renamed from: q, reason: collision with root package name */
    private static String f2498q;

    /* JADX INFO: renamed from: r, reason: collision with root package name */
    private static String f2499r;

    /* JADX INFO: renamed from: s, reason: collision with root package name */
    private static String f2500s;

    /* JADX INFO: renamed from: t, reason: collision with root package name */
    private static String f2501t;

    /* JADX INFO: renamed from: u, reason: collision with root package name */
    private static String f2502u;

    /* JADX INFO: renamed from: v, reason: collision with root package name */
    private static String f2503v;

    /* JADX INFO: renamed from: w, reason: collision with root package name */
    private static String f2504w;

    /* JADX INFO: renamed from: x, reason: collision with root package name */
    private static String f2505x;

    /* JADX INFO: renamed from: y, reason: collision with root package name */
    private static String f2506y;

    /* JADX INFO: renamed from: z, reason: collision with root package name */
    private static String f2507z;

    private static class GAIDClient {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private static final String f2508a = "GAIDClient";

        private GAIDClient() {
        }

        static String a(Context context) {
            if (!c(context)) {
                p.a(f2508a, "Google play service is not available");
                return "";
            }
            AdvertisingConnection advertisingConnection = new AdvertisingConnection(null);
            try {
                try {
                    Intent intent = new Intent("com.google.android.gms.ads.identifier.service.START");
                    intent.setPackage("com.google.android.gms");
                    if (context.bindService(intent, advertisingConnection, 1)) {
                        return new a(advertisingConnection.a()).a();
                    }
                } catch (Exception e7) {
                    p.a(f2508a, "Query Google ADID failed ", e7);
                }
                return "";
            } finally {
                context.unbindService(advertisingConnection);
            }
        }

        static boolean b(Context context) {
            if (!c(context)) {
                p.a(f2508a, "Google play service is not available");
                return false;
            }
            AdvertisingConnection advertisingConnection = new AdvertisingConnection(null);
            try {
                try {
                    Intent intent = new Intent("com.google.android.gms.ads.identifier.service.START");
                    intent.setPackage("com.google.android.gms");
                    if (context.bindService(intent, advertisingConnection, 1)) {
                        return new a(advertisingConnection.a()).a(true);
                    }
                } catch (Exception e7) {
                    p.a(f2508a, "Query Google isLimitAdTrackingEnabled failed ", e7);
                }
                return false;
            } finally {
                context.unbindService(advertisingConnection);
            }
        }

        private static boolean c(Context context) {
            try {
                context.getPackageManager().getPackageInfo("com.android.vending", 16384);
                return true;
            } catch (PackageManager.NameNotFoundException unused) {
                return false;
            }
        }

        private static final class AdvertisingConnection implements ServiceConnection {

            /* JADX INFO: renamed from: a, reason: collision with root package name */
            private static final int f2509a = 30000;

            /* JADX INFO: renamed from: b, reason: collision with root package name */
            private boolean f2510b;

            /* JADX INFO: renamed from: c, reason: collision with root package name */
            private IBinder f2511c;

            private AdvertisingConnection() {
                this.f2510b = false;
            }

            public IBinder a() {
                IBinder iBinder = this.f2511c;
                if (iBinder != null) {
                    return iBinder;
                }
                if (iBinder == null && !this.f2510b) {
                    synchronized (this) {
                        wait(30000L);
                        if (this.f2511c == null) {
                            throw new InterruptedException("Not connect or connect timeout to google play service");
                        }
                    }
                }
                return this.f2511c;
            }

            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                synchronized (this) {
                    this.f2511c = iBinder;
                    notifyAll();
                }
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName componentName) {
                this.f2510b = true;
                this.f2511c = null;
            }

            /* synthetic */ AdvertisingConnection(h hVar) {
                this();
            }
        }

        private static final class a implements IInterface {

            /* JADX INFO: renamed from: a, reason: collision with root package name */
            private IBinder f2512a;

            public a(IBinder iBinder) {
                this.f2512a = iBinder;
            }

            public String a() {
                if (this.f2512a == null) {
                    return "";
                }
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                    this.f2512a.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.f2512a;
            }

            public boolean a(boolean z6) {
                if (this.f2512a == null) {
                    return false;
                }
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                    parcelObtain.writeInt(z6 ? 1 : 0);
                    this.f2512a.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }

    static {
        try {
            f2489h = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
        } catch (Throwable th) {
            p.b(f2484c, "sGetProp init failed ex: " + th.getMessage());
        }
        try {
            Class<?> cls = Class.forName("miui.telephony.TelephonyManagerEx");
            f2492k = cls.getMethod("getDefault", new Class[0]).invoke(null, new Object[0]);
            f2490i = cls.getMethod("getImeiList", new Class[0]);
            f2491j = cls.getMethod("getMeidList", new Class[0]);
            f2494m = cls.getMethod("getSubscriberIdForSlot", Integer.TYPE);
        } catch (Throwable th2) {
            p.b(f2484c, "TelephonyManagerEx init failed ex: " + th2.getMessage());
        }
        try {
            f2493l = Class.forName("android.telephony.TelephonyManager").getMethod("getImei", Integer.TYPE);
        } catch (Throwable th3) {
            p.b(f2484c, "sGetImeiForSlot init failed ex: " + th3.getMessage());
        }
    }

    private static String A(Context context) {
        if (!TextUtils.isEmpty(f2497p)) {
            return f2497p;
        }
        D(context);
        return !TextUtils.isEmpty(f2497p) ? f2497p : "";
    }

    private static List<String> B(Context context) {
        if (f2493l == null) {
            return null;
        }
        try {
            ArrayList arrayList = new ArrayList();
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
            String str = (String) f2493l.invoke(telephonyManager, 0);
            if (c(str)) {
                arrayList.add(str);
            }
            if (n()) {
                String str2 = (String) f2493l.invoke(telephonyManager, 1);
                if (c(str2)) {
                    arrayList.add(str2);
                }
            }
            return arrayList;
        } catch (Exception e7) {
            p.a(f2484c, "getImeiListAboveLollipop failed ex: " + e7.getMessage());
            return null;
        }
    }

    private static List<String> C(Context context) {
        try {
            ArrayList arrayList = new ArrayList();
            Class<?> cls = Class.forName("android.telephony.TelephonyManager");
            if (!n()) {
                String deviceId = ((TelephonyManager) cls.getMethod("getDefault", new Class[0]).invoke(null, new Object[0])).getDeviceId();
                if (c(deviceId)) {
                    arrayList.add(deviceId);
                }
                return arrayList;
            }
            Class<?> cls2 = Integer.TYPE;
            String deviceId2 = ((TelephonyManager) cls.getMethod("getDefault", cls2).invoke(null, 0)).getDeviceId();
            String deviceId3 = ((TelephonyManager) cls.getMethod("getDefault", cls2).invoke(null, 1)).getDeviceId();
            if (c(deviceId2)) {
                arrayList.add(deviceId2);
            }
            if (c(deviceId3)) {
                arrayList.add(deviceId3);
            }
            return arrayList;
        } catch (Throwable th) {
            p.a(f2484c, "getImeiListBelowLollipop failed ex: " + th.getMessage());
            return null;
        }
    }

    private static List<String> D(Context context) {
        if (u.a(context)) {
            Method method = f2491j;
            if (method != null) {
                try {
                    List<String> list = (List) method.invoke(f2492k, new Object[0]);
                    if (list != null && list.size() > 0 && !b(list)) {
                        Collections.sort(list);
                        f2497p = list.get(0);
                        return list;
                    }
                } catch (Exception e7) {
                    p.a(f2484c, "queryMeidList failed ex: " + e7.getMessage());
                }
            }
            try {
                Class<?> cls = Class.forName("android.telephony.TelephonyManager");
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
                Method method2 = cls.getMethod("getMeid", new Class[0]);
                String str = method2 != null ? (String) method2.invoke(telephonyManager, new Object[0]) : null;
                if (d(str)) {
                    ArrayList arrayList = new ArrayList();
                    arrayList.add(str);
                    f2497p = str;
                    return arrayList;
                }
            } catch (Throwable th) {
                p.a(f2484c, "queryMeidList->getMeid failed ex: " + th.getMessage());
            }
        }
        return null;
    }

    private static boolean E(Context context) {
        return (context.getResources().getConfiguration().screenLayout & 15) >= 3;
    }

    private static boolean F(Context context) {
        return a.a(context) || (context.getResources().getConfiguration().uiMode & 15) == 4;
    }

    public static long a(long j6) {
        float f7 = j6;
        if (f7 > 900.0d) {
            f7 /= 1000.0f;
        }
        if (f7 > 900.0d) {
            f7 /= 1000.0f;
        }
        if (f7 > 900.0d) {
            f7 /= 1000.0f;
        }
        return (long) f7;
    }

    public static String b(Context context) {
        if (!TextUtils.isEmpty(f2502u)) {
            return f2502u;
        }
        String strA = a(context);
        if (TextUtils.isEmpty(strA)) {
            return "";
        }
        String strC = com.xiaomi.onetrack.d.d.c(strA);
        f2502u = strC;
        return strC;
    }

    private static long c(long j6) {
        long j7 = 1;
        long j8 = 1;
        while (true) {
            long j9 = j7 * j8;
            if (j9 >= j6) {
                return j9;
            }
            j7 <<= 1;
            if (j7 > 512) {
                j8 *= 1000;
                j7 = 1;
            }
        }
    }

    public static String d(Context context) {
        if (!TextUtils.isEmpty(A)) {
            return A;
        }
        String strA = a(context);
        if (TextUtils.isEmpty(strA)) {
            return "";
        }
        String strD = com.xiaomi.onetrack.d.d.d(strA);
        A = strD;
        return strD;
    }

    public static String e(Context context) {
        if (!TextUtils.isEmpty(f2496o)) {
            return f2496o;
        }
        r(context);
        return !TextUtils.isEmpty(f2496o) ? f2496o : "";
    }

    public static String f(Context context) {
        if (!TextUtils.isEmpty(f2503v)) {
            return f2503v;
        }
        String strE = e(context);
        if (TextUtils.isEmpty(strE)) {
            return "";
        }
        String strC = com.xiaomi.onetrack.d.d.c(strE);
        f2503v = strC;
        return strC;
    }

    public static String g(Context context) {
        if (!TextUtils.isEmpty(f2507z)) {
            return f2507z;
        }
        String strE = e(context);
        if (TextUtils.isEmpty(strE)) {
            return "";
        }
        String strE2 = com.xiaomi.onetrack.d.d.e(strE);
        f2507z = strE2;
        return strE2;
    }

    public static String h(Context context) {
        if (!TextUtils.isEmpty(B)) {
            return B;
        }
        String strE = e(context);
        if (TextUtils.isEmpty(strE)) {
            return "";
        }
        String strD = com.xiaomi.onetrack.d.d.d(strE);
        B = strD;
        return strD;
    }

    public static String i(Context context) {
        if (!TextUtils.isEmpty(f2504w)) {
            return f2504w;
        }
        String strA = A(context);
        if (TextUtils.isEmpty(strA)) {
            return "";
        }
        String strC = com.xiaomi.onetrack.d.d.c(strA);
        f2504w = strC;
        return strC;
    }

    public static String j(Context context) {
        if (!TextUtils.isEmpty(C)) {
            return C;
        }
        String strA = A(context);
        if (TextUtils.isEmpty(strA)) {
            return "";
        }
        String strE = com.xiaomi.onetrack.d.d.e(strA);
        C = strE;
        return strE;
    }

    public static String k(Context context) throws Throwable {
        if (!TextUtils.isEmpty(f2498q)) {
            return f2498q;
        }
        String strB = a.b(context);
        if (TextUtils.isEmpty(strB)) {
            return "";
        }
        f2498q = strB;
        return strB;
    }

    public static String l(Context context) {
        if (!TextUtils.isEmpty(f2499r)) {
            return f2499r;
        }
        String strU = u(context);
        if (TextUtils.isEmpty(strU)) {
            return "";
        }
        f2499r = strU;
        return strU;
    }

    public static String m(Context context) {
        if (!TextUtils.isEmpty(f2505x)) {
            return f2505x;
        }
        String strL = l(context);
        if (TextUtils.isEmpty(strL)) {
            return "";
        }
        String strC = com.xiaomi.onetrack.d.d.c(strL);
        f2505x = strC;
        return strC;
    }

    public static String n(Context context) {
        if (!TextUtils.isEmpty(E)) {
            return E;
        }
        String strL = l(context);
        if (TextUtils.isEmpty(strL)) {
            return "";
        }
        String strE = com.xiaomi.onetrack.d.d.e(strL);
        E = strE;
        return strE;
    }

    private static boolean o() {
        return false;
    }

    public static String p(Context context) {
        if (!TextUtils.isEmpty(f2501t)) {
            return f2501t;
        }
        if (GAIDClient.b(context)) {
            return "";
        }
        String strA = GAIDClient.a(context);
        if (TextUtils.isEmpty(strA)) {
            return "";
        }
        f2501t = strA;
        return strA;
    }

    public static List<String> q(Context context) {
        List<String> listR = r(context);
        ArrayList arrayList = new ArrayList();
        if (listR != null && !listR.isEmpty()) {
            for (int i6 = 0; i6 < listR.size(); i6++) {
                if (!TextUtils.isEmpty(listR.get(i6))) {
                    arrayList.add(i6, com.xiaomi.onetrack.d.d.c(listR.get(i6)));
                }
            }
        }
        return arrayList;
    }

    public static List<String> r(Context context) {
        List<String> listB = null;
        if (u.a(context)) {
            if (H) {
                return null;
            }
            List<String> listM = m();
            listB = (listM == null || listM.isEmpty()) ? B(context) : listM;
            H = true;
        }
        if (listB != null && !listB.isEmpty()) {
            Collections.sort(listB);
            f2495n = listB.get(0);
            if (listB.size() >= 2) {
                f2496o = listB.get(1);
            }
        }
        return listB;
    }

    public static String s(Context context) {
        try {
            List<String> listD = D(context);
            if (listD == null) {
                return "";
            }
            Collections.sort(listD, new h());
            for (int i6 = 0; i6 < listD.size(); i6++) {
                listD.set(i6, com.xiaomi.onetrack.d.d.h(listD.get(i6)));
            }
            return listD.toString();
        } catch (Throwable th) {
            p.b(f2484c, "getMeidListMd5 e", th);
            return "";
        }
    }

    public static List<String> t(Context context) {
        String str;
        String str2;
        if (!u.b(context)) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        try {
            if (!n()) {
                String subscriberId = ((TelephonyManager) context.getSystemService("phone")).getSubscriberId();
                if (e(subscriberId)) {
                    arrayList.add(subscriberId);
                }
                return arrayList;
            }
            Class<?> cls = Class.forName("android.telephony.TelephonyManager");
            int i6 = Build.VERSION.SDK_INT;
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
            SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService("telephony_subscription_service");
            Class<?> cls2 = Class.forName("android.telephony.SubscriptionManager");
            if (i6 < 29) {
                str = a(cls, cls2, telephonyManager, subscriptionManager)[0];
                str2 = a(cls, cls2, telephonyManager, subscriptionManager)[1];
            } else {
                str = b(cls, cls2, telephonyManager, subscriptionManager)[0];
                str2 = b(cls, cls2, telephonyManager, subscriptionManager)[1];
            }
            if (!e(str)) {
                str = "";
            }
            arrayList.add(str);
            if (!e(str2)) {
                str2 = "";
            }
            arrayList.add(str2);
            return arrayList;
        } catch (SecurityException unused) {
            p.a(f2484c, "getImsiList failed with on permission");
            return null;
        } catch (Throwable th) {
            p.b(f2484c, "getImsiList failed: " + th.getMessage());
            return null;
        }
    }

    public static String u(Context context) {
        String str = null;
        if (u.b(context)) {
            try {
                Method method = Class.forName("android.os.Build").getMethod("getSerial", new Class[0]);
                if (method != null) {
                    str = (String) method.invoke(null, new Object[0]);
                }
            } catch (Throwable th) {
                p.a(f2484c, "querySerial failed ex: " + th.getMessage());
            }
        }
        if (TextUtils.isEmpty(str) || "unknown".equals(str)) {
            return "";
        }
        f2499r = str;
        return str;
    }

    public static String v(Context context) {
        try {
            List<String> listT = t(context);
            if (listT == null) {
                return "";
            }
            for (int i6 = 0; i6 < listT.size(); i6++) {
                listT.set(i6, com.xiaomi.onetrack.d.d.h(listT.get(i6)));
            }
            return listT.toString();
        } catch (Throwable th) {
            p.b(p.a(f2484c), "getImeiListMd5 failed!", th);
            return "";
        }
    }

    public static String w(Context context) {
        if (!TextUtils.isEmpty(G)) {
            return G;
        }
        if (p()) {
            G = "Pad";
            return "Pad";
        }
        if (!F(context)) {
            return "Phone";
        }
        G = "Tv";
        return "Tv";
    }

    public static boolean x(Context context) {
        if (F == null) {
            F = Boolean.valueOf(a.a(context));
        }
        return F.booleanValue();
    }

    public static String y(Context context) {
        return com.xiaomi.onetrack.util.oaid.a.a().a(context);
    }

    public static String z(Context context) {
        Display defaultDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getRealSize(point);
        return String.format("%d*%d", Integer.valueOf(point.y), Integer.valueOf(point.x));
    }

    public static String c(Context context) {
        if (!TextUtils.isEmpty(f2506y)) {
            return f2506y;
        }
        String strA = a(context);
        if (TextUtils.isEmpty(strA)) {
            return "";
        }
        String strE = com.xiaomi.onetrack.d.d.e(strA);
        f2506y = strE;
        return strE;
    }

    public static String o(Context context) {
        if (!TextUtils.isEmpty(f2500s)) {
            return f2500s;
        }
        try {
            String type = context.getContentResolver().getType(Uri.parse("content://com.miui.analytics.server.AnalyticsProvider/aaid"));
            if (!TextUtils.isEmpty(type)) {
                f2500s = type;
                return type;
            }
            Object objInvoke = Class.forName("android.provider.MiuiSettings$Ad").getDeclaredMethod("getAaid", ContentResolver.class).invoke(null, context.getContentResolver());
            if (!(objInvoke instanceof String) || TextUtils.isEmpty((String) objInvoke)) {
                return "";
            }
            String str = (String) objInvoke;
            f2500s = str;
            return str;
        } catch (Throwable th) {
            p.a(f2484c, "getAaid failed ex: " + th.getMessage());
            return "";
        }
    }

    private static class a {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private static final String f2513a = "box";

        /* JADX INFO: renamed from: b, reason: collision with root package name */
        private static final String f2514b = "tvbox";

        /* JADX INFO: renamed from: c, reason: collision with root package name */
        private static final String f2515c = "projector";

        /* JADX INFO: renamed from: d, reason: collision with root package name */
        private static final String f2516d = "tv";

        /* JADX INFO: renamed from: e, reason: collision with root package name */
        private static final String f2517e = "mi_device_mac";

        /* JADX INFO: renamed from: f, reason: collision with root package name */
        private static Signature[] f2518f;

        /* JADX INFO: renamed from: g, reason: collision with root package name */
        private static final Signature f2519g = new Signature("3082033b30820223a003020102020900a07a328482f70d2a300d06092a864886f70d01010505003035310b30090603550406130255533113301106035504080c0a43616c69666f726e69613111300f06035504070c084d6f756e7461696e301e170d3133303430313033303831325a170d3430303831373033303831325a3035310b30090603550406130255533113301106035504080c0a43616c69666f726e69613111300f06035504070c084d6f756e7461696e30820120300d06092a864886f70d01010105000382010d00308201080282010100ac678c9234a0226edbeb75a43e8e18f632d8c8a094c087fffbbb0b5e4429d845e36bffbe2d7098e320855258aa777368c18c538f968063d5d61663dc946ab03acbb31d00a27d452e12e6d42865e27d6d0ad2d8b12cf3b3096a7ec66a21db2a6a697857fd4d29fb4cdf294b3371d7601f2e3f190c0164efa543897026c719b334808e4f612fe3a3da589115fc30f9ca89862feefdf31a9164ecb295dcf7767e673be2192dda64f88189fd6e6ebd62e572c7997c2385a0ea9292ec799dee8f87596fc73aa123fb6f577d09ac0c123179c3bdbc978c2fe6194eb9fa4ab3658bfe8b44040bb13fe7809409e622189379fbc63966ab36521793547b01673ecb5f15cf020103a350304e301d0603551d0e0416041447203684e562385ada79108c4c94c5055037592f301f0603551d2304183016801447203684e562385ada79108c4c94c5055037592f300c0603551d13040530030101ff300d06092a864886f70d010105050003820101008d530fe05c6fe694c7559ddb5dd2c556528dd3cad4f7580f439f9a90a4681d37ce246b9a6681bdd5a5437f0b8bba903e39bac309fc0e9ee5553681612e723e9ec4f6abab6b643b33013f09246a9b5db7703b96f838fb27b00612f5fcd431bea32f68350ae51a4a1d012c520c401db7cccc15a7b19c4310b0c3bfc625ce5744744d0b9eeb02b0a4e7d51ed59849ce580b9f7c3062c84b9a0b13cc211e1c916c289820266a610801e3316c915649804571b147beadbf88d3b517ee04121d40630853f2f2a506bb788620de9648faeacff568e5033a666316bc2046526674ed3de25ceefdc4ad3628f1a230fd41bf9ca9f6a078173850dba555768fe1c191483ad9");

        private a() {
        }

        static boolean a(Context context) {
            if (f2518f == null) {
                f2518f = new Signature[]{c(context)};
            }
            Signature signature = f2518f[0];
            return signature != null && signature.equals(f2519g);
        }

        public static String b(Context context) throws Throwable {
            String strA;
            try {
                String string = Settings.Global.getString(context.getContentResolver(), f2517e);
                if (!TextUtils.isEmpty(string)) {
                    return string;
                }
            } catch (Exception unused) {
            }
            try {
                String str = Build.PRODUCT;
                String strB = DeviceUtil.b("ro.product.model");
                boolean z6 = true;
                if ((!TextUtils.equals(f2516d, a()) || "batman".equals(str) || "conan".equals(str)) && !"augustrush".equals(str) && !"casablanca".equals(str)) {
                    z6 = false;
                }
                if (TextUtils.equals("me2", str)) {
                    strA = DeviceUtil.b("persist.service.bdroid.bdaddr");
                } else if ((TextUtils.equals("transformers", str) && TextUtils.equals("MiBOX4C", strB)) || TextUtils.equals("dolphin-zorro", str)) {
                    strA = a("/sys/class/net/wlan0/address");
                } else {
                    strA = z6 ? a("/sys/class/net/eth0/address") : a("ro.boot.btmac");
                }
                return !TextUtils.isEmpty(strA) ? strA.trim() : "";
            } catch (Exception e7) {
                p.a(DeviceUtil.f2484c, "getMiTvMac exception", e7);
                return "";
            }
        }

        private static Signature c(Context context) {
            try {
                return a(context.getPackageManager().getPackageInfo("android", 64));
            } catch (Exception unused) {
                return null;
            }
        }

        private static Signature a(PackageInfo packageInfo) {
            Signature[] signatureArr;
            if (packageInfo == null || (signatureArr = packageInfo.signatures) == null || signatureArr.length <= 0) {
                return null;
            }
            return signatureArr[0];
        }

        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r1v0 */
        /* JADX WARN: Type inference failed for: r1v1 */
        /* JADX WARN: Type inference failed for: r1v12 */
        /* JADX WARN: Type inference failed for: r1v13 */
        /* JADX WARN: Type inference failed for: r1v2, types: [java.io.Closeable] */
        /* JADX WARN: Type inference failed for: r1v4 */
        /* JADX WARN: Type inference failed for: r1v5 */
        private static String a(String str) throws Throwable {
            String string = "";
            ?? r6 = 0;
            StringBuilder sb = null;
            BufferedReader bufferedReader = null;
            try {
                try {
                    BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(new FileInputStream(str)), PowerNest.SHARK_HTTP);
                    try {
                        String line = bufferedReader2.readLine();
                        if (line != null) {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("");
                            sb2.append(line);
                            string = sb2.toString();
                            sb = sb2;
                        }
                        m.a(bufferedReader2);
                        r6 = sb;
                    } catch (Exception e7) {
                        e = e7;
                        bufferedReader = bufferedReader2;
                        p.b(DeviceUtil.f2484c, "catEntry exception", e);
                        m.a(bufferedReader);
                        r6 = bufferedReader;
                    } catch (Throwable th) {
                        th = th;
                        r6 = bufferedReader2;
                        m.a((Closeable) r6);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Exception e8) {
                e = e8;
            }
            return string;
        }

        private static String a() {
            try {
                Class<?> cls = Class.forName("mitv.common.ConfigurationManager");
                int i6 = Integer.parseInt(String.valueOf(cls.getMethod("getProductCategory", new Class[0]).invoke(cls.getMethod("getInstance", new Class[0]).invoke(cls, new Object[0]), new Object[0])));
                Class<?> cls2 = Class.forName("mitv.tv.TvContext");
                if (i6 == Integer.parseInt(String.valueOf(a(cls2, "PRODUCT_CATEGORY_MITV")))) {
                    return f2516d;
                }
                if (i6 == Integer.parseInt(String.valueOf(a(cls2, "PRODUCT_CATEGORY_MIBOX")))) {
                    return f2513a;
                }
                if (i6 == Integer.parseInt(String.valueOf(a(cls2, "PRODUCT_CATEGORY_MITVBOX")))) {
                    return f2514b;
                }
                return i6 == Integer.parseInt(String.valueOf(a(cls2, "PRODUCT_CATEGORY_MIPROJECTOR"))) ? f2515c : "";
            } catch (Throwable th) {
                p.a(DeviceUtil.f2484c, "getMiTvProductCategory exception: " + th.getMessage());
                return "";
            }
        }

        private static <T> T a(Class<?> cls, String str) {
            try {
                Field declaredField = cls.getDeclaredField(str);
                declaredField.setAccessible(true);
                return (T) declaredField.get(null);
            } catch (Exception e7) {
                p.b(DeviceUtil.f2484c, "getStaticVariableValue exception", e7);
                return null;
            }
        }
    }

    public static String a(Context context) {
        if (!TextUtils.isEmpty(f2495n)) {
            return f2495n;
        }
        r(context);
        return !TextUtils.isEmpty(f2495n) ? f2495n : "";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String b(String str) {
        try {
            Method method = f2489h;
            if (method != null) {
                return String.valueOf(method.invoke(null, str));
            }
        } catch (Exception e7) {
            p.a(f2484c, "getProp failed ex: " + e7.getMessage());
        }
        return null;
    }

    private static boolean d(String str) {
        return (str == null || str.length() != 14 || str.matches("^0*$")) ? false : true;
    }

    private static boolean e(String str) {
        return str != null && str.length() >= 6 && str.length() <= 15 && !str.matches("^0*$");
    }

    public static String f() {
        try {
            String strB = b("ro.product.model.real");
            return TextUtils.isEmpty(strB) ? c() : strB;
        } catch (Exception e7) {
            p.b(f2484c, "getModelReal Exception: ", e7);
            return "";
        }
    }

    public static String g() {
        try {
            String strA = ab.a("ro.product.mod_device", "");
            return TextUtils.isEmpty(strA) ? Build.DEVICE : strA;
        } catch (Exception e7) {
            p.b(f2484c, "getModDevice Exception: ", e7);
            return "";
        }
    }

    public static String h() {
        return ab.a(f2486e, "");
    }

    public static String i() {
        try {
            return ab.a("ro.product.manufacturer", "");
        } catch (Exception e7) {
            p.b(p.a(f2484c), "getProductManufacturer e", e7);
            return "";
        }
    }

    public static long j() {
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return (statFs.getBlockSizeLong() * statFs.getBlockCountLong()) / UpdateConfig.UPDATE_FLAG_VIRUS_BASE;
    }

    public static String k() {
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return String.format("%dGB", Long.valueOf(b(statFs.getBlockSizeLong() * statFs.getBlockCountLong())));
    }

    public static long l() {
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return a(statFs.getBlockSizeLong() * statFs.getAvailableBlocksLong());
    }

    private static List<String> m() {
        if (f2490i == null || o()) {
            return null;
        }
        try {
            List<String> list = (List) f2490i.invoke(f2492k, new Object[0]);
            if (list == null || list.size() <= 0 || a(list)) {
                return null;
            }
            return list;
        } catch (Exception e7) {
            p.a(f2484c, "getImeiListFromMiui failed ex: " + e7.getMessage());
            return null;
        }
    }

    private static boolean n() {
        if ("dsds".equals(b("persist.radio.multisim.config"))) {
            return true;
        }
        String str = Build.DEVICE;
        return "lcsh92_wet_jb9".equals(str) || "lcsh92_wet_tdd".equals(str) || "HM2013022".equals(str) || "HM2013023".equals(str) || "armani".equals(str) || "HM2014011".equals(str) || "HM2014012".equals(str);
    }

    private static boolean c(String str) {
        return (str == null || str.length() != 15 || str.matches("^0*$")) ? false : true;
    }

    public static String d() {
        return b("ro.product.marketname");
    }

    public static String e() {
        return Build.MANUFACTURER;
    }

    private static boolean p() {
        try {
            return ((Boolean) Class.forName("miui.os.Build").getField("IS_TABLET").get(null)).booleanValue();
        } catch (Throwable th) {
            p.b(f2484c, "get miui.os.Build Exception: " + th.getMessage());
            try {
                return ((Boolean) Class.forName("miui.util.FeatureParser").getMethod("getBoolean", String.class, Boolean.TYPE).invoke(null, "is_pad", Boolean.FALSE)).booleanValue();
            } catch (Throwable th2) {
                p.b(f2484c, "get miui.util.FeatureParser Exception: " + th2.getMessage());
                return false;
            }
        }
    }

    public static void a() {
        f2501t = null;
    }

    public static String c() {
        return Build.MODEL;
    }

    private static boolean a(List<String> list) {
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (!c(it.next())) {
                return true;
            }
        }
        return false;
    }

    private static boolean b(List<String> list) {
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (!d(it.next())) {
                return true;
            }
        }
        return false;
    }

    private static String[] a(Class<?> cls, Class<?> cls2, TelephonyManager telephonyManager, SubscriptionManager subscriptionManager) {
        Method method;
        Object obj;
        String[] strArr = new String[2];
        try {
            Class<?> cls3 = Integer.TYPE;
            String str = (String) cls.getMethod("getSubscriberId", cls3).invoke(telephonyManager, Integer.valueOf(((int[]) cls2.getMethod("getSubId", cls3).invoke(subscriptionManager, 0))[0]));
            strArr[0] = str;
            if (e(str) || (method = f2494m) == null || (obj = f2492k) == null) {
                strArr[1] = (String) cls.getMethod("getSubscriberId", cls3).invoke(telephonyManager, Integer.valueOf(((int[]) cls2.getMethod("getSubId", cls3).invoke(subscriptionManager, 1))[0]));
            } else {
                strArr[0] = (String) method.invoke(obj, 0);
                strArr[1] = (String) f2494m.invoke(f2492k, 1);
            }
        } catch (Exception e7) {
            p.a(f2484c, "getImsiFromLToP: " + e7);
        }
        return strArr;
    }

    private static String[] b(Class<?> cls, Class<?> cls2, TelephonyManager telephonyManager, SubscriptionManager subscriptionManager) {
        String[] strArr = new String[2];
        try {
            Class<?> cls3 = Integer.TYPE;
            int[] iArr = (int[]) cls2.getMethod("getSubscriptionIds", cls3).invoke(subscriptionManager, 0);
            if (iArr != null) {
                strArr[0] = (String) cls.getMethod("getSubscriberId", cls3).invoke(telephonyManager, Integer.valueOf(iArr[0]));
            }
        } catch (Exception e7) {
            p.b(f2484c, "get imsi1 above Android Q exception:" + e7);
        }
        try {
            Class<?> cls4 = Integer.TYPE;
            int[] iArr2 = (int[]) cls2.getMethod("getSubscriptionIds", cls4).invoke(subscriptionManager, 1);
            if (iArr2 != null) {
                strArr[1] = (String) cls.getMethod("getSubscriberId", cls4).invoke(telephonyManager, Integer.valueOf(iArr2[0]));
            }
        } catch (Exception e8) {
            p.b(f2484c, "get imsi2 above Android Q exception:" + e8);
        }
        return strArr;
    }

    public static String b() {
        return b("ro.product.name");
    }

    private static long b(long j6) {
        long jPow;
        if (j6 >= 8000000000L) {
            jPow = (long) Math.pow(2.0d, Math.ceil(Math.log(j6 / 1000000000) / Math.log(2.0d)));
        } else {
            jPow = (j6 / 1000000000) + 1;
        }
        long jC = jPow * 1000000000;
        if (jC < j6) {
            jC = c(j6);
        }
        return jC / 1000000000;
    }
}
