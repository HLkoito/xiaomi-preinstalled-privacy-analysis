package ac;

import android.os.Build;
import cl.e;
import com.google.android.exoplayer2.C;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.milink.sdk.cast.MiLinkDevice;
import com.miui.luckymoney.config.Constants;
import com.miui.permcenter.model.LocalAppInfoBean;
import com.miui.securitykey.ConfigManager;
import ef.x;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;
import x4.a0;
import x4.v1;

/* JADX INFO: loaded from: classes2.dex */
public class c {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private static final String f673a = b() + "/user/cat";

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private static final String f674b = b() + "/user/ra";

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private static final String f675c = ConfigManager.getSecurityHttpKey();

    private static JsonObject a(JsonObject jsonObject) {
        if (jsonObject == null) {
            jsonObject = new JsonObject();
        }
        jsonObject.addProperty("type", a0.h());
        jsonObject.addProperty("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        jsonObject.addProperty("os", "MIUI_" + Build.VERSION.INCREMENTAL);
        jsonObject.addProperty(Constants.JSON_KEY_DEVICE, v1.c("ro.product.device", MiLinkDevice.TYPE_UNKNOWN));
        jsonObject.addProperty("biz_id", "rivacy_risk_check");
        return jsonObject;
    }

    public static String b() {
        return "https://flash.sec.miui.com";
    }

    public static String c(String str) {
        return g(f674b, str, f675c);
    }

    public static String d(List<LocalAppInfoBean> list) {
        JsonObject jsonObject = new JsonObject();
        JsonObject jsonObject2 = new JsonObject();
        for (LocalAppInfoBean localAppInfoBean : list) {
            jsonObject2.addProperty(localAppInfoBean.packageName, String.valueOf(localAppInfoBean.versionCode));
        }
        jsonObject.add("apps", jsonObject2);
        a(jsonObject);
        return new Gson().toJson((JsonElement) jsonObject);
    }

    public static String e(String str) {
        return g(f673a, str, f675c);
    }

    public static String f(List<LocalAppInfoBean> list) {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        Iterator<LocalAppInfoBean> it = list.iterator();
        while (it.hasNext()) {
            jsonArray.add(new JsonPrimitive(it.next().packageName));
        }
        jsonObject.add("apps", jsonArray);
        a(jsonObject);
        return new Gson().toJson((JsonElement) jsonObject);
    }

    /* JADX WARN: Code duplicated, block: B:47:0x00fa  */
    private static String g(String str, String str2, String str3) throws Throwable {
        InputStream inputStream;
        HttpURLConnection httpURLConnection;
        DataOutputStream dataOutputStream;
        if (!x.z()) {
            return "";
        }
        try {
            URL url = new URL(str);
            byte[] bArrE = a.e(a.d(str2, str3));
            try {
                try {
                    httpURLConnection = url.getProtocol().toLowerCase().equals("https") ? (HttpsURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection();
                    try {
                        httpURLConnection.setConnectTimeout(10000);
                        httpURLConnection.setReadTimeout(10000);
                        httpURLConnection.setRequestMethod("POST");
                        httpURLConnection.setRequestProperty("Charset", C.UTF8_NAME);
                        httpURLConnection.setDoOutput(true);
                        httpURLConnection.setDoInput(true);
                        httpURLConnection.setUseCaches(false);
                        httpURLConnection.setRequestProperty(HttpHeaders.CONTENT_LENGTH, String.valueOf(bArrE.length));
                        httpURLConnection.setRequestProperty(HttpHeaders.CONTENT_TYPE, "text/xml");
                        httpURLConnection.connect();
                        dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
                        try {
                            dataOutputStream.write(bArrE);
                            dataOutputStream.flush();
                            dataOutputStream.close();
                            if (httpURLConnection.getResponseCode() != 200) {
                                httpURLConnection.disconnect();
                                e.b(null);
                                e.c(dataOutputStream);
                                e.d(null);
                                return "";
                            }
                            InputStream inputStream2 = httpURLConnection.getInputStream();
                            try {
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                byte[] bArr = new byte[4096];
                                while (true) {
                                    int i10 = inputStream2.read(bArr);
                                    if (i10 == -1) {
                                        String strOptString = new JSONObject(a.c(a.h(byteArrayOutputStream.toByteArray()), str3)).optString("data");
                                        httpURLConnection.disconnect();
                                        e.b(inputStream2);
                                        e.c(dataOutputStream);
                                        e.d(null);
                                        return strOptString;
                                    }
                                    byteArrayOutputStream.write(bArr, 0, i10);
                                }
                            } catch (Exception e10) {
                                inputStream = inputStream2;
                                e = e10;
                            } catch (Throwable th2) {
                                inputStream = inputStream2;
                                th = th2;
                                if (httpURLConnection != null) {
                                    httpURLConnection.disconnect();
                                    e.b(inputStream);
                                    e.c(dataOutputStream);
                                    e.d(null);
                                }
                                throw th;
                            }
                        } catch (Exception e11) {
                            e = e11;
                            inputStream = null;
                        } catch (Throwable th3) {
                            th = th3;
                            inputStream = null;
                        }
                    } catch (Exception e12) {
                        e = e12;
                        inputStream = null;
                        dataOutputStream = null;
                    } catch (Throwable th4) {
                        th = th4;
                        inputStream = null;
                        dataOutputStream = null;
                    }
                } catch (Exception e13) {
                    e = e13;
                    inputStream = null;
                    httpURLConnection = null;
                    dataOutputStream = null;
                } catch (Throwable th5) {
                    th = th5;
                    inputStream = null;
                    httpURLConnection = null;
                    dataOutputStream = null;
                }
                e.printStackTrace();
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                    e.b(inputStream);
                    e.c(dataOutputStream);
                    e.d(null);
                }
                return "";
            } catch (Throwable th6) {
                th = th6;
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                    e.b(inputStream);
                    e.c(dataOutputStream);
                    e.d(null);
                }
                throw th;
            }
        } catch (MalformedURLException unused) {
            return "";
        }
    }
}
