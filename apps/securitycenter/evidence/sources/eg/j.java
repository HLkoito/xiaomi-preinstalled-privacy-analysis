package eg;

import android.app.Application;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.google.android.exoplayer2.C;
import com.google.common.net.HttpHeaders;
import com.milink.sdk.cast.MiLinkDevice;
import com.miui.luckymoney.config.Constants;
import com.miui.luckymoney.model.message.Impl.QQMessage;
import com.miui.maml.folme.AnimatedProperty;
import com.miui.securitykey.ConfigManager;
import ef.v;
import ef.w;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import miui.cloud.CloudPushConstants;
import miui.os.Build;
import org.json.JSONObject;
import x4.l0;
import x4.l1;
import x4.v1;
import x4.y;

/* JADX INFO: loaded from: classes3.dex */
public class j {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private static final boolean f25513a = uf.a.f35870a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public static final String f25514b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private static final String f25515c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private static final String f25516d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    private static final String f25517e;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    private static final String f25518f;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    private static final String f25519g;

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    private static final String f25520h;

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    private static final HashMap<String, String> f25521i;

    static /* synthetic */ class a {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        static final /* synthetic */ int[] f25522a;

        static {
            int[] iArr = new int[b.values().length];
            f25522a = iArr;
            try {
                iArr[b.GET.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                f25522a[b.DELETE.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                f25522a[b.POST.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                f25522a[b.PUT.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    public enum b {
        GET,
        POST,
        PUT,
        DELETE
    }

    private static class c implements Comparable<c> {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private String f25528a;

        /* JADX INFO: renamed from: b, reason: collision with root package name */
        private String f25529b;

        public c(String str, String str2) {
            this.f25528a = str;
            this.f25529b = str2;
        }

        @Override // java.lang.Comparable
        /* JADX INFO: renamed from: c, reason: merged with bridge method [inline-methods] */
        public int compareTo(c cVar) {
            return this.f25528a.compareTo(cVar.f25528a);
        }
    }

    static {
        String strC = v1.c("ro.product.device", MiLinkDevice.TYPE_UNKNOWN);
        f25514b = strC;
        String strC2 = v1.c("ro.carrier.name", MiLinkDevice.TYPE_UNKNOWN);
        f25515c = strC2;
        String region = Build.getRegion();
        f25516d = region;
        String str = android.os.Build.VERSION.INCREMENTAL;
        f25517e = str;
        String strC3 = v1.c("ro.miui.ui.version.name", MiLinkDevice.TYPE_UNKNOWN);
        f25518f = strC3;
        String str2 = android.os.Build.VERSION.RELEASE;
        f25519g = str2;
        String str3 = android.os.Build.MODEL;
        f25520h = str3;
        HashMap<String, String> map = new HashMap<>();
        f25521i = map;
        map.put("d", strC);
        map.put("c", strC2);
        map.put("r", region);
        map.put("v", str);
        map.put("vn", strC3);
        map.put(com.xiaomi.onetrack.b.m.f23200f, str2);
        map.put(Constants.JSON_KEY_T, eg.c.d());
        map.put("si", String.valueOf(android.os.Build.VERSION.SDK_INT));
        map.put("mo", str3);
        Application applicationD = com.miui.common.e.d();
        if (!Build.IS_INTERNATIONAL_BUILD) {
            boolean zO = o();
            map.put("m", !zO ? tm.a.d(mm.a.b(eg.c.c(applicationD), "MD5")) : "");
            map.put("ri", String.valueOf(zO));
        }
        map.put("o", eg.c.h(applicationD));
        try {
            map.put("e", String.valueOf(applicationD.getPackageManager().getPackageInfo(applicationD.getPackageName(), 0).versionCode));
        } catch (PackageManager.NameNotFoundException e10) {
            e10.printStackTrace();
        }
    }

    public static String A(String str, b bVar, String str2, p4.i iVar) throws Throwable {
        ByteArrayOutputStream byteArrayOutputStream;
        if (!l1.b()) {
            return "";
        }
        int i10 = -1;
        InputStream inputStream = null;
        try {
            HashMap map = new HashMap();
            map.put("u", UUID.randomUUID().toString().replace("-", ""));
            map.put("sign", m(map, str2));
            if (bVar == b.GET) {
                String strH = h(map);
                if (!str.contains("?")) {
                    str = str.concat("?");
                }
                str = str.concat(strH);
            }
            boolean z10 = f25513a;
            if (z10) {
                Log.d("NetUtil", "request start : " + str);
            }
            HttpURLConnection httpURLConnectionP = p(new URL(str));
            E(httpURLConnectionP, bVar, map);
            int responseCode = httpURLConnectionP.getResponseCode();
            if (z10) {
                try {
                    Log.d("NetUtil", " responseCode :  " + responseCode);
                } catch (Exception e10) {
                    e = e10;
                    i10 = responseCode;
                    byteArrayOutputStream = null;
                    try {
                        e.printStackTrace();
                        p4.h.a(iVar, i10, 0);
                        d(inputStream);
                        d(byteArrayOutputStream);
                        return "";
                    } catch (Throwable th2) {
                        th = th2;
                        p4.h.a(iVar, i10, 0);
                        d(inputStream);
                        d(byteArrayOutputStream);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    i10 = responseCode;
                    byteArrayOutputStream = null;
                    p4.h.a(iVar, i10, 0);
                    d(inputStream);
                    d(byteArrayOutputStream);
                    throw th;
                }
            }
            if (responseCode != 200) {
                p4.h.a(iVar, responseCode, 0);
                d(null);
                d(null);
                return "";
            }
            InputStream inputStream2 = httpURLConnectionP.getInputStream();
            try {
                byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    byte[] bArr = new byte[4096];
                    while (true) {
                        int i11 = inputStream2.read(bArr);
                        if (i11 == -1) {
                            break;
                        }
                        byteArrayOutputStream.write(bArr, 0, i11);
                    }
                    String string = byteArrayOutputStream.toString();
                    if (f25513a) {
                        Log.d("NetUtil", "request result  : " + string);
                    }
                    p4.h.a(iVar, responseCode, 0);
                    d(inputStream2);
                    d(byteArrayOutputStream);
                    return string;
                } catch (Exception e11) {
                    inputStream = inputStream2;
                    i10 = responseCode;
                    e = e11;
                    e.printStackTrace();
                    p4.h.a(iVar, i10, 0);
                    d(inputStream);
                    d(byteArrayOutputStream);
                    return "";
                } catch (Throwable th4) {
                    inputStream = inputStream2;
                    i10 = responseCode;
                    th = th4;
                    p4.h.a(iVar, i10, 0);
                    d(inputStream);
                    d(byteArrayOutputStream);
                    throw th;
                }
            } catch (Exception e12) {
                i10 = responseCode;
                inputStream = inputStream2;
                e = e12;
                byteArrayOutputStream = null;
            } catch (Throwable th5) {
                i10 = responseCode;
                inputStream = inputStream2;
                th = th5;
                byteArrayOutputStream = null;
            }
        } catch (Exception e13) {
            e = e13;
        } catch (Throwable th6) {
            th = th6;
        }
    }

    public static String B(Map<String, String> map, String str, b bVar, String str2, p4.i iVar) {
        return C(map, str, bVar, str2, false, true, iVar);
    }

    private static String C(Map<String, String> map, String str, b bVar, String str2, boolean z10, boolean z11, p4.i iVar) throws Throwable {
        ByteArrayOutputStream byteArrayOutputStream;
        if (!l1.b() && !z10) {
            return "";
        }
        int i10 = -1;
        InputStream inputStream = null;
        try {
            if (z11) {
                map = b(map, str2);
            } else {
                if (map == null) {
                    map = new HashMap<>();
                }
                map.put("sign", m(map, str2));
            }
            if (bVar == b.GET && map != null) {
                String strH = h(map);
                if (!str.contains("?")) {
                    str = str.concat("?");
                }
                str = str.concat(strH);
            }
            boolean z12 = f25513a;
            if (z12) {
                Log.d("NetUtil", "request start : " + str);
            }
            HttpURLConnection httpURLConnectionP = p(new URL(str));
            E(httpURLConnectionP, bVar, map);
            int responseCode = httpURLConnectionP.getResponseCode();
            if (z12) {
                try {
                    Log.d("NetUtil", " responseCode :  " + responseCode);
                } catch (Exception e10) {
                    i10 = responseCode;
                    e = e10;
                    byteArrayOutputStream = null;
                    try {
                        Log.e("NetUtil", "request: " + e);
                        p4.h.a(iVar, i10, 0);
                        d(inputStream);
                        d(byteArrayOutputStream);
                        return "";
                    } catch (Throwable th2) {
                        th = th2;
                        p4.h.a(iVar, i10, 0);
                        d(inputStream);
                        d(byteArrayOutputStream);
                        throw th;
                    }
                } catch (Throwable th3) {
                    i10 = responseCode;
                    th = th3;
                    byteArrayOutputStream = null;
                    p4.h.a(iVar, i10, 0);
                    d(inputStream);
                    d(byteArrayOutputStream);
                    throw th;
                }
            }
            if (responseCode != 200) {
                p4.h.a(iVar, responseCode, 0);
                d(null);
                d(null);
                return "";
            }
            InputStream inputStream2 = httpURLConnectionP.getInputStream();
            try {
                byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    byte[] bArr = new byte[4096];
                    while (true) {
                        int i11 = inputStream2.read(bArr);
                        if (i11 == -1) {
                            break;
                        }
                        byteArrayOutputStream.write(bArr, 0, i11);
                    }
                    String string = byteArrayOutputStream.toString();
                    if (f25513a) {
                        Log.d("NetUtil", "request result  : " + string);
                    }
                    p4.h.a(iVar, responseCode, 0);
                    d(inputStream2);
                    d(byteArrayOutputStream);
                    return string;
                } catch (Exception e11) {
                    i10 = responseCode;
                    inputStream = inputStream2;
                    e = e11;
                    Log.e("NetUtil", "request: " + e);
                    p4.h.a(iVar, i10, 0);
                    d(inputStream);
                    d(byteArrayOutputStream);
                    return "";
                } catch (Throwable th4) {
                    i10 = responseCode;
                    inputStream = inputStream2;
                    th = th4;
                    p4.h.a(iVar, i10, 0);
                    d(inputStream);
                    d(byteArrayOutputStream);
                    throw th;
                }
            } catch (Exception e12) {
                i10 = responseCode;
                e = e12;
                byteArrayOutputStream = null;
                inputStream = inputStream2;
            } catch (Throwable th5) {
                i10 = responseCode;
                th = th5;
                byteArrayOutputStream = null;
                inputStream = inputStream2;
            }
        } catch (Exception e13) {
            e = e13;
        } catch (Throwable th6) {
            th = th6;
        }
    }

    public static String D(String str, String str2) throws Throwable {
        ByteArrayOutputStream byteArrayOutputStream;
        InputStream inputStream;
        ByteArrayOutputStream byteArrayOutputStream2;
        if (!l1.b()) {
            return "";
        }
        InputStream inputStream2 = null;
        try {
            try {
                boolean z10 = f25513a;
                if (z10) {
                    Log.d("NetUtil", "request post json start : " + str2);
                }
                HttpURLConnection httpURLConnectionP = p(new URL(str2));
                httpURLConnectionP.setConnectTimeout(QQMessage.TYPE_DISCUSS_GROUP);
                httpURLConnectionP.setRequestMethod("POST");
                httpURLConnectionP.addRequestProperty(HttpHeaders.CONTENT_TYPE, "application/json");
                httpURLConnectionP.setDoOutput(true);
                DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnectionP.getOutputStream());
                dataOutputStream.write(str.getBytes());
                dataOutputStream.close();
                int responseCode = httpURLConnectionP.getResponseCode();
                if (z10) {
                    Log.d("NetUtil", " responseCode :  " + responseCode);
                }
                if (responseCode != 200) {
                    d(null);
                    d(null);
                    return null;
                }
                InputStream inputStream3 = httpURLConnectionP.getInputStream();
                try {
                    byteArrayOutputStream = new ByteArrayOutputStream();
                    try {
                        byte[] bArr = new byte[4096];
                        while (true) {
                            int i10 = inputStream3.read(bArr);
                            if (i10 == -1) {
                                break;
                            }
                            byteArrayOutputStream.write(bArr, 0, i10);
                        }
                        String string = byteArrayOutputStream.toString();
                        if (f25513a) {
                            Log.d("NetUtil", "request post json result : " + string);
                        }
                        d(inputStream3);
                        d(byteArrayOutputStream);
                        return string;
                    } catch (Exception e10) {
                        inputStream = inputStream3;
                        e = e10;
                        byteArrayOutputStream2 = byteArrayOutputStream;
                    } catch (Throwable th2) {
                        inputStream2 = inputStream3;
                        th = th2;
                        d(inputStream2);
                        d(byteArrayOutputStream);
                        throw th;
                    }
                } catch (Exception e11) {
                    byteArrayOutputStream2 = null;
                    inputStream = inputStream3;
                    e = e11;
                } catch (Throwable th3) {
                    inputStream2 = inputStream3;
                    th = th3;
                    byteArrayOutputStream = null;
                }
            } catch (Exception e12) {
                e = e12;
                inputStream = null;
                byteArrayOutputStream2 = null;
            } catch (Throwable th4) {
                th = th4;
                byteArrayOutputStream = null;
            }
            e.printStackTrace();
            d(inputStream);
            d(byteArrayOutputStream2);
            return null;
        } catch (Throwable th5) {
            th = th5;
            inputStream2 = inputStream;
            byteArrayOutputStream = byteArrayOutputStream2;
            d(inputStream2);
            d(byteArrayOutputStream);
            throw th;
        }
    }

    private static void E(HttpURLConnection httpURLConnection, b bVar, Map<String, String> map) throws IOException {
        String str;
        int i10 = a.f25522a[bVar.ordinal()];
        if (i10 == 1) {
            str = "GET";
        } else if (i10 == 2) {
            str = "DELETE";
        } else if (i10 == 3) {
            httpURLConnection.setRequestMethod("POST");
            a(httpURLConnection, map);
            return;
        } else {
            if (i10 != 4) {
                throw new IllegalStateException("Unknown method type.");
            }
            str = "PUT";
        }
        httpURLConnection.setRequestMethod(str);
    }

    private static void a(HttpURLConnection httpURLConnection, Map<String, String> map) throws IOException {
        byte[] bytes;
        if (map == null || map.size() <= 0) {
            bytes = null;
        } else {
            String strH = h(map);
            if (f25513a) {
                Log.d("NetUtil", " post body : " + strH);
            }
            bytes = strH.getBytes();
        }
        if (bytes != null) {
            httpURLConnection.setDoOutput(true);
            httpURLConnection.addRequestProperty(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
            DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            dataOutputStream.write(bytes);
            dataOutputStream.close();
        }
    }

    public static Map<String, String> b(Map<String, String> map, String str) {
        if (map == null) {
            map = new HashMap<>();
        }
        map.putAll(f25521i);
        Application applicationD = com.miui.common.e.d();
        map.put(com.xiaomi.onetrack.b.e.f23161a, Locale.getDefault().toString());
        map.put("n", eg.c.e(applicationD));
        if (Build.IS_INTERNATIONAL_BUILD) {
            try {
                String strB = v.b();
                if (!TextUtils.isEmpty(strB)) {
                    ConfigManager.UuidAesKey uuidAesKey = ConfigManager.getUuidAesKey(applicationD);
                    String strB2 = "";
                    if (uuidAesKey != null) {
                        String key = uuidAesKey.getKey();
                        String iv = uuidAesKey.getIv();
                        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(iv)) {
                            strB2 = eg.a.b(strB, key, iv);
                        }
                    }
                    map.put(CloudPushConstants.WATERMARK_TYPE.GLOBAL, strB2);
                }
                map.put("cr", y.d());
            } catch (Exception e10) {
                Log.e("NetUtil", "getAdvertisingId error", e10);
            }
        } else {
            map.put(AnimatedProperty.PROPERTY_NAME_X, eg.c.a(applicationD));
            map.put(com.xiaomi.onetrack.b.m.f23195a, w.a(applicationD));
        }
        map.put("sign", m(map, str));
        return map;
    }

    public static void c(HttpURLConnection httpURLConnection, String str) throws IOException {
        byte[] bytes;
        if (TextUtils.isEmpty(str)) {
            bytes = null;
        } else {
            if (f25513a) {
                Log.d("NetUtil", " post body : " + str);
            }
            bytes = str.getBytes();
        }
        if (bytes != null) {
            httpURLConnection.setDoOutput(true);
            httpURLConnection.addRequestProperty(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
            DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            dataOutputStream.write(bytes);
            dataOutputStream.close();
        }
    }

    private static void d(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException unused) {
            }
        }
    }

    public static String e(String str, Map<String, String> map, p4.i iVar) {
        return g(str, map, b.GET, iVar);
    }

    public static String f(String str, Map<String, String> map, p4.i iVar) {
        return g(str, map, b.POST, iVar);
    }

    private static String g(String str, Map<String, String> map, b bVar, p4.i iVar) throws Throwable {
        ByteArrayOutputStream byteArrayOutputStream;
        if (!l1.b()) {
            return "";
        }
        int i10 = -1;
        InputStream inputStream = null;
        try {
            if (bVar == b.GET && map != null) {
                String strH = h(map);
                if (!str.contains("?")) {
                    str = str.concat("?");
                }
                str = str.concat(strH);
            }
            boolean z10 = f25513a;
            if (z10) {
                Log.d("NetUtil", "request start : " + str);
            }
            HttpURLConnection httpURLConnectionP = p(new URL(str));
            E(httpURLConnectionP, bVar, map);
            int responseCode = httpURLConnectionP.getResponseCode();
            if (z10) {
                try {
                    Log.d("NetUtil", " responseCode :  " + responseCode);
                } catch (Exception e10) {
                    e = e10;
                    i10 = responseCode;
                    byteArrayOutputStream = null;
                    try {
                        e.printStackTrace();
                        Log.e("NetExp", "Comment request exp", e);
                        p4.h.a(iVar, i10, 0);
                        d(inputStream);
                        d(byteArrayOutputStream);
                        return "";
                    } catch (Throwable th2) {
                        th = th2;
                        p4.h.a(iVar, i10, 0);
                        d(inputStream);
                        d(byteArrayOutputStream);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    i10 = responseCode;
                    byteArrayOutputStream = null;
                    p4.h.a(iVar, i10, 0);
                    d(inputStream);
                    d(byteArrayOutputStream);
                    throw th;
                }
            }
            if (responseCode != 200) {
                p4.h.a(iVar, responseCode, 0);
                d(null);
                d(null);
                return "";
            }
            InputStream inputStream2 = httpURLConnectionP.getInputStream();
            try {
                byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    byte[] bArr = new byte[4096];
                    while (true) {
                        int i11 = inputStream2.read(bArr);
                        if (i11 == -1) {
                            break;
                        }
                        byteArrayOutputStream.write(bArr, 0, i11);
                    }
                    String string = byteArrayOutputStream.toString();
                    if (f25513a) {
                        Log.d("NetUtil", "request result  : " + string);
                    }
                    p4.h.a(iVar, responseCode, 0);
                    d(inputStream2);
                    d(byteArrayOutputStream);
                    return string;
                } catch (Exception e11) {
                    inputStream = inputStream2;
                    i10 = responseCode;
                    e = e11;
                    e.printStackTrace();
                    Log.e("NetExp", "Comment request exp", e);
                    p4.h.a(iVar, i10, 0);
                    d(inputStream);
                    d(byteArrayOutputStream);
                    return "";
                } catch (Throwable th4) {
                    inputStream = inputStream2;
                    i10 = responseCode;
                    th = th4;
                    p4.h.a(iVar, i10, 0);
                    d(inputStream);
                    d(byteArrayOutputStream);
                    throw th;
                }
            } catch (Exception e12) {
                i10 = responseCode;
                inputStream = inputStream2;
                e = e12;
                byteArrayOutputStream = null;
            } catch (Throwable th5) {
                i10 = responseCode;
                inputStream = inputStream2;
                th = th5;
                byteArrayOutputStream = null;
            }
        } catch (Exception e13) {
            e = e13;
        } catch (Throwable th6) {
            th = th6;
        }
    }

    public static String h(Map<String, String> map) {
        StringBuilder sb2 = new StringBuilder();
        boolean z10 = true;
        try {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (!z10) {
                    sb2.append('&');
                }
                sb2.append(URLEncoder.encode(entry.getKey(), C.UTF8_NAME));
                sb2.append('=');
                sb2.append(URLEncoder.encode(entry.getValue(), C.UTF8_NAME));
                z10 = false;
            }
            return sb2.toString();
        } catch (UnsupportedEncodingException e10) {
            throw new RuntimeException("Encoding not supported: " + ((Object) sb2), e10);
        }
    }

    public static String i(Map<String, String> map, String str, p4.i iVar) {
        return C(map, str, b.POST, "2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9", false, false, iVar);
    }

    public static String j(Map<String, String> map, String str, p4.i iVar) {
        return C(map, str, b.GET, "2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9", false, true, iVar);
    }

    private static byte[] k(String str) {
        try {
            return str.getBytes(C.UTF8_NAME);
        } catch (UnsupportedEncodingException unused) {
            return str.getBytes();
        }
    }

    public static String l(String str) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(k(str));
            return String.format("%1$032X", new BigInteger(1, messageDigest.digest()));
        } catch (NoSuchAlgorithmException e10) {
            throw new RuntimeException(e10);
        }
    }

    public static String m(Map<String, String> map, String str) {
        StringBuilder sb2 = new StringBuilder();
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            arrayList.add(new c(entry.getKey(), entry.getValue()));
        }
        Collections.sort(arrayList);
        int size = arrayList.size();
        for (int i10 = 0; i10 < size; i10++) {
            if (i10 > 0) {
                sb2.append("&");
            }
            c cVar = (c) arrayList.get(i10);
            sb2.append(cVar.f25528a);
            sb2.append("=");
            sb2.append(cVar.f25529b);
        }
        sb2.append("&");
        sb2.append(str);
        return l(new String(Base64.encodeToString(k(sb2.toString()), 2)));
    }

    /* JADX WARN: Code duplicated, block: B:21:0x00af  */
    public static HashMap<String, String> n(String str) {
        String strB;
        String str2;
        HashMap<String, String> map = new HashMap<>();
        map.put("d", f25514b);
        map.put("r", f25516d);
        map.put("v", f25517e);
        map.put("androidVersion", String.valueOf(android.os.Build.VERSION.SDK_INT));
        map.put(Constants.JSON_KEY_T, eg.c.d());
        map.put("t_app", eg.c.g());
        Application applicationD = com.miui.common.e.d();
        map.put("n", eg.c.e(applicationD));
        String strValueOf = "";
        if (Build.IS_INTERNATIONAL_BUILD) {
            try {
                String strB2 = v.b();
                ConfigManager.getUuidAesKey(com.miui.common.e.d());
                ConfigManager.UuidAesKey uuidAesKey = ConfigManager.getUuidAesKey(com.miui.common.e.d());
                if (TextUtils.isEmpty(strB2) || uuidAesKey == null) {
                    strB = "";
                } else {
                    String key = uuidAesKey.getKey();
                    String iv = uuidAesKey.getIv();
                    if (TextUtils.isEmpty(key) || TextUtils.isEmpty(iv)) {
                        strB = "";
                    } else {
                        strB = eg.a.b(strB2, key, iv);
                    }
                }
            } catch (Exception e10) {
                Log.e("TrainRequestBean", "getAdvertisingId error", e10);
            }
            str2 = CloudPushConstants.WATERMARK_TYPE.GLOBAL;
        } else {
            map.put("m", !o() ? tm.a.d(mm.a.b(eg.c.c(applicationD), "MD5")) : "");
            strB = w.a(com.miui.common.e.d());
            str2 = com.xiaomi.onetrack.b.m.f23195a;
        }
        map.put(str2, strB);
        try {
            strValueOf = String.valueOf(applicationD.getPackageManager().getPackageInfo(applicationD.getPackageName(), 0).versionCode);
        } catch (Exception e11) {
            e11.printStackTrace();
        }
        map.put("e", strValueOf);
        map.put("model_version", str);
        return map;
    }

    public static boolean o() {
        return eg.c.l();
    }

    private static HttpURLConnection p(URL url) {
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setConnectTimeout(15000);
        httpURLConnection.setReadTimeout(15000);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setDoInput(true);
        return httpURLConnection;
    }

    public static String q(String str, p4.i iVar) {
        return t(null, str, iVar);
    }

    public static String r(Map<String, String> map, String str, String str2, p4.i iVar) {
        return C(map, str, b.POST, str2, false, true, iVar);
    }

    public static String s(Map<String, String> map, String str, String str2, boolean z10, boolean z11, p4.i iVar) {
        return C(map, str, b.POST, str2, z10, z11, iVar);
    }

    public static String t(Map<String, String> map, String str, p4.i iVar) {
        return C(map, str, b.POST, "2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9", false, true, iVar);
    }

    public static String u(String str) {
        return C(n(str), "https://api.sec.miui.com/mnntrain/taskdata", b.POST, "5cdd8678-cddf-4269-ab73-48017445bba3", true, false, new p4.i("ai_request_task"));
    }

    public static String v(String str, JSONObject jSONObject) throws Throwable {
        ByteArrayOutputStream byteArrayOutputStream;
        DataOutputStream dataOutputStream;
        InputStream inputStream;
        ByteArrayOutputStream byteArrayOutputStream2;
        if (!l1.b()) {
            return "";
        }
        InputStream inputStream2 = null;
        try {
            try {
                boolean z10 = f25513a;
                if (z10) {
                    Log.d("NetUtil", "request start : " + str);
                }
                HttpURLConnection httpURLConnectionP = p(new URL(str));
                httpURLConnectionP.setRequestMethod("POST");
                String string = jSONObject.toString();
                if (z10) {
                    Log.d("NetUtil", "body : " + string);
                }
                byte[] bytes = !TextUtils.isEmpty(string) ? string.getBytes() : null;
                if (bytes != null) {
                    httpURLConnectionP.addRequestProperty(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
                }
                dataOutputStream = new DataOutputStream(httpURLConnectionP.getOutputStream());
                try {
                    dataOutputStream.write(bytes);
                    int responseCode = httpURLConnectionP.getResponseCode();
                    if (z10) {
                        Log.d("NetUtil", "responseCode : " + responseCode);
                    }
                    if (responseCode != 200) {
                        d(null);
                        d(dataOutputStream);
                        d(null);
                        return null;
                    }
                    InputStream inputStream3 = httpURLConnectionP.getInputStream();
                    try {
                        byteArrayOutputStream = new ByteArrayOutputStream();
                        try {
                            byte[] bArr = new byte[4096];
                            while (true) {
                                int i10 = inputStream3.read(bArr);
                                if (i10 == -1) {
                                    break;
                                }
                                byteArrayOutputStream.write(bArr, 0, i10);
                            }
                            String string2 = byteArrayOutputStream.toString();
                            if (f25513a) {
                                Log.d("NetUtil", "request result  : " + string2);
                            }
                            d(inputStream3);
                            d(dataOutputStream);
                            d(byteArrayOutputStream);
                            return string2;
                        } catch (Exception e10) {
                            inputStream = inputStream3;
                            e = e10;
                            byteArrayOutputStream2 = byteArrayOutputStream;
                        } catch (Throwable th2) {
                            inputStream2 = inputStream3;
                            th = th2;
                            d(inputStream2);
                            d(dataOutputStream);
                            d(byteArrayOutputStream);
                            throw th;
                        }
                    } catch (Exception e11) {
                        byteArrayOutputStream2 = null;
                        inputStream = inputStream3;
                        e = e11;
                    } catch (Throwable th3) {
                        inputStream2 = inputStream3;
                        th = th3;
                        byteArrayOutputStream = null;
                    }
                } catch (Exception e12) {
                    e = e12;
                    inputStream = null;
                    byteArrayOutputStream2 = null;
                } catch (Throwable th4) {
                    th = th4;
                    byteArrayOutputStream = null;
                }
            } catch (Exception e13) {
                e = e13;
                inputStream = null;
                byteArrayOutputStream2 = null;
                dataOutputStream = null;
            } catch (Throwable th5) {
                th = th5;
                byteArrayOutputStream = null;
                dataOutputStream = null;
            }
            Log.e("NetUtil", "postAdvTailData failed ", e);
            d(inputStream);
            d(dataOutputStream);
            d(byteArrayOutputStream2);
            return null;
        } catch (Throwable th6) {
            th = th6;
            inputStream2 = inputStream;
            byteArrayOutputStream = byteArrayOutputStream2;
            d(inputStream2);
            d(dataOutputStream);
            d(byteArrayOutputStream);
            throw th;
        }
    }

    public static <T extends Map<String, String>> String w(T t10, String str) {
        return x(t10, str, true);
    }

    public static <T extends Map<String, String>> String x(T t10, String str, boolean z10) {
        return y(t10, str, z10, false);
    }

    public static <T extends Map<String, String>> String y(T t10, String str, boolean z10, boolean z11) throws Throwable {
        ByteArrayOutputStream byteArrayOutputStream;
        InputStream inputStream;
        ByteArrayOutputStream byteArrayOutputStream2;
        if (!l1.b()) {
            return "";
        }
        InputStream inputStream2 = null;
        try {
            try {
                boolean z12 = f25513a;
                if (z12) {
                    Log.d("NetUtil", "request start : " + str);
                }
                HttpURLConnection httpURLConnectionP = p(new URL(str));
                String strA = z11 ? k.a(t10, "2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9") : m(t10, "2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9");
                t10.put("sign", strA);
                String strA2 = l0.a(t10);
                if (z12) {
                    Log.d("NetUtil", "jsonBodyStr : " + strA2);
                }
                if (z10) {
                    httpURLConnectionP.setRequestProperty("sign", strA);
                }
                c(httpURLConnectionP, strA2);
                int responseCode = httpURLConnectionP.getResponseCode();
                if (z12) {
                    Log.d("NetUtil", " responseCode :  " + responseCode);
                }
                if (responseCode != 200) {
                    d(null);
                    d(null);
                    return null;
                }
                InputStream inputStream3 = httpURLConnectionP.getInputStream();
                try {
                    byteArrayOutputStream = new ByteArrayOutputStream();
                    try {
                        byte[] bArr = new byte[4096];
                        while (true) {
                            int i10 = inputStream3.read(bArr);
                            if (i10 == -1) {
                                break;
                            }
                            byteArrayOutputStream.write(bArr, 0, i10);
                        }
                        String string = byteArrayOutputStream.toString();
                        if (f25513a) {
                            Log.d("NetUtil", "request result  : " + string);
                        }
                        d(inputStream3);
                        d(byteArrayOutputStream);
                        return string;
                    } catch (Exception e10) {
                        inputStream = inputStream3;
                        e = e10;
                        byteArrayOutputStream2 = byteArrayOutputStream;
                    } catch (Throwable th2) {
                        inputStream2 = inputStream3;
                        th = th2;
                        d(inputStream2);
                        d(byteArrayOutputStream);
                        throw th;
                    }
                } catch (Exception e11) {
                    byteArrayOutputStream2 = null;
                    inputStream = inputStream3;
                    e = e11;
                } catch (Throwable th3) {
                    inputStream2 = inputStream3;
                    th = th3;
                    byteArrayOutputStream = null;
                }
            } catch (Exception e12) {
                e = e12;
                inputStream = null;
                byteArrayOutputStream2 = null;
            } catch (Throwable th4) {
                th = th4;
                byteArrayOutputStream = null;
            }
            e.printStackTrace();
            d(inputStream);
            d(byteArrayOutputStream2);
            return null;
        } catch (Throwable th5) {
            th = th5;
            inputStream2 = inputStream;
            byteArrayOutputStream = byteArrayOutputStream2;
            d(inputStream2);
            d(byteArrayOutputStream);
            throw th;
        }
    }

    public static String z(String str, p4.i iVar) {
        return A(str, b.POST, "2dcd9s0c-ad3f-2fas-0l3a-abzo301jd0s9", iVar);
    }
}
