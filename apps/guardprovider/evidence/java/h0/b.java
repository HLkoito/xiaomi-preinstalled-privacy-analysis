package h0;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.miui.guardprovider.GuardApplication;
import d0.d;
import java.io.ByteArrayOutputStream;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public abstract class b {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private static final String f4871a = d.a("ro.product.device", "unknown");

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private static final String f4872b = d.a("ro.carrier.name", "unknown");

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private static final String f4873c = d.a("ro.product.mod_device", "unknown");

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private static final String f4874d = "MIUI-" + Build.VERSION.INCREMENTAL;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    private static final String f4875e = String.valueOf(1);

    class a implements Comparator {
        a() {
        }

        @Override // java.util.Comparator
        /* JADX INFO: renamed from: a, reason: merged with bridge method [inline-methods] */
        public int compare(String str, String str2) {
            return str.compareTo(str2);
        }
    }

    private static void a(HttpURLConnection httpURLConnection, String str) throws IOException {
        if (str != null) {
            httpURLConnection.setDoOutput(true);
            httpURLConnection.addRequestProperty("Content-Type", "application/json");
            DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            dataOutputStream.writeBytes(str);
            dataOutputStream.close();
        }
    }

    private static void b(HttpURLConnection httpURLConnection, Map map) throws IOException {
        byte[] bytes = (map == null || map.size() <= 0) ? null : e(map).getBytes();
        if (bytes != null) {
            httpURLConnection.setDoOutput(true);
            httpURLConnection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            dataOutputStream.write(bytes);
            dataOutputStream.close();
        }
    }

    public static String c(Context context, String str, Map map, String str2) throws Throwable {
        ByteArrayOutputStream byteArrayOutputStream;
        InputStream inputStream;
        InputStream inputStream2 = null;
        if (!GuardApplication.e(context) || TextUtils.isEmpty(str)) {
            return null;
        }
        Map mapM = m(context, map, str2);
        try {
            HttpURLConnection httpURLConnectionN = n(str);
            httpURLConnectionN.setRequestMethod("POST");
            b(httpURLConnectionN, mapM);
            if (httpURLConnectionN.getResponseCode() != 200) {
                b5.a.b(null);
                b5.a.c(null);
                return null;
            }
            inputStream = httpURLConnectionN.getInputStream();
            try {
                byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    try {
                        byte[] bArr = new byte[4096];
                        while (true) {
                            int i6 = inputStream.read(bArr);
                            if (i6 == -1) {
                                String string = byteArrayOutputStream.toString();
                                b5.a.b(inputStream);
                                b5.a.c(byteArrayOutputStream);
                                return string;
                            }
                            byteArrayOutputStream.write(bArr, 0, i6);
                            th = th;
                            inputStream2 = inputStream;
                            b5.a.b(inputStream2);
                            b5.a.c(byteArrayOutputStream);
                            throw th;
                        }
                    } catch (Exception e7) {
                        e = e7;
                        Log.e("NetworkApiHelper", "exception when do post request", e);
                        b5.a.b(inputStream);
                        b5.a.c(byteArrayOutputStream);
                        return null;
                    }
                } catch (Throwable th) {
                    th = th;
                }
            } catch (Exception e8) {
                e = e8;
                byteArrayOutputStream = null;
            } catch (Throwable th2) {
                th = th2;
                byteArrayOutputStream = null;
            }
        } catch (Exception e9) {
            e = e9;
            inputStream = null;
            byteArrayOutputStream = null;
        } catch (Throwable th3) {
            th = th3;
            byteArrayOutputStream = null;
        }
        Log.e("NetworkApiHelper", "exception when do post request", e);
        b5.a.b(inputStream);
        b5.a.c(byteArrayOutputStream);
        return null;
    }

    public static String d(Context context, String str, JSONObject jSONObject, String str2) throws Throwable {
        ByteArrayOutputStream byteArrayOutputStream;
        InputStream inputStream;
        InputStream inputStream2 = null;
        if (!GuardApplication.e(context) || TextUtils.isEmpty(str)) {
            return null;
        }
        String strI = i(l(jSONObject), str2);
        try {
            HttpURLConnection httpURLConnectionN = n(str);
            httpURLConnectionN.setRequestMethod("POST");
            jSONObject.put("sign", strI);
            a(httpURLConnectionN, jSONObject.toString());
            if (httpURLConnectionN.getResponseCode() != 200) {
                b5.a.b(null);
                b5.a.c(null);
                return null;
            }
            inputStream = httpURLConnectionN.getInputStream();
            try {
                byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    try {
                        byte[] bArr = new byte[4096];
                        while (true) {
                            int i6 = inputStream.read(bArr);
                            if (i6 == -1) {
                                String string = byteArrayOutputStream.toString();
                                b5.a.b(inputStream);
                                b5.a.c(byteArrayOutputStream);
                                return string;
                            }
                            byteArrayOutputStream.write(bArr, 0, i6);
                            th = th;
                            inputStream2 = inputStream;
                            b5.a.b(inputStream2);
                            b5.a.c(byteArrayOutputStream);
                            throw th;
                        }
                    } catch (Exception e7) {
                        e = e7;
                        Log.e("NetworkApiHelper", "exception when do post request", e);
                        b5.a.b(inputStream);
                        b5.a.c(byteArrayOutputStream);
                        return null;
                    }
                } catch (Throwable th) {
                    th = th;
                }
            } catch (Exception e8) {
                e = e8;
                byteArrayOutputStream = null;
            } catch (Throwable th2) {
                th = th2;
                byteArrayOutputStream = null;
            }
        } catch (Exception e9) {
            e = e9;
            inputStream = null;
            byteArrayOutputStream = null;
        } catch (Throwable th3) {
            th = th3;
            byteArrayOutputStream = null;
        }
        Log.e("NetworkApiHelper", "exception when do post request", e);
        b5.a.b(inputStream);
        b5.a.c(byteArrayOutputStream);
        return null;
    }

    private static String e(Map map) {
        StringBuilder sb = new StringBuilder();
        try {
            boolean z6 = true;
            for (String str : map.keySet()) {
                if (!z6) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(str, "UTF-8"));
                sb.append('=');
                sb.append(URLEncoder.encode((String) map.get(str), "UTF-8"));
                z6 = false;
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e7) {
            throw new RuntimeException("Encoding not supported: " + ((Object) sb), e7);
        }
    }

    public static Map f(Context context, String str) {
        HashMap map = new HashMap();
        map.put("device", f4871a);
        map.put("carrier", f4872b);
        map.put("region", f4873c);
        map.put("miuiVersion", f4874d);
        map.put("versionType", k());
        map.put("appVersion", f4875e);
        map.put("mi", h(context, str));
        return map;
    }

    private static byte[] g(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException unused) {
            return str.getBytes();
        }
    }

    public static String h(Context context, String str) {
        String string;
        try {
            string = Settings.Secure.getString(context.getContentResolver(), "android_id");
        } catch (Exception e7) {
            e7.printStackTrace();
            string = null;
        }
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        return f5.a.a(e5.a.b(string + "-" + str, "MD5"));
    }

    public static String i(Map map, String str) {
        ArrayList<String> arrayList = new ArrayList(map.keySet());
        Collections.sort(arrayList, new a());
        StringBuilder sb = new StringBuilder();
        boolean z6 = true;
        for (String str2 : arrayList) {
            if (!z6) {
                sb.append("&");
            }
            sb.append(str2);
            sb.append("=");
            sb.append((String) map.get(str2));
            z6 = false;
        }
        sb.append("&");
        sb.append(str);
        return j(Base64.encodeToString(g(sb.toString()), 2));
    }

    private static String j(String str) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(g(str));
            return String.format("%1$032X", new BigInteger(1, messageDigest.digest()));
        } catch (NoSuchAlgorithmException e7) {
            throw new RuntimeException(e7);
        }
    }

    public static String k() {
        if (miui.os.Build.IS_STABLE_VERSION) {
            return "stable";
        }
        return miui.os.Build.IS_DEVELOPMENT_VERSION ? "development" : "alpha";
    }

    public static Map l(JSONObject jSONObject) {
        HashMap map = new HashMap();
        Iterator<String> itKeys = jSONObject.keys();
        while (itKeys.hasNext()) {
            String next = itKeys.next();
            if (!next.equals("sign")) {
                if (next.equals("content")) {
                    map.put(next, jSONObject.optJSONArray(next).toString());
                } else {
                    map.put(next, jSONObject.optString(next));
                }
            }
        }
        return map;
    }

    public static Map m(Context context, Map map, String str) {
        Map mapF = f(context, str);
        mapF.putAll(map);
        mapF.put("sign", i(mapF, str));
        return mapF;
    }

    public static HttpURLConnection n(String str) {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(com.xiaomi.onetrack.g.b.f2462b);
        httpURLConnection.setReadTimeout(com.xiaomi.onetrack.g.b.f2462b);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setDoInput(true);
        return httpURLConnection;
    }
}
