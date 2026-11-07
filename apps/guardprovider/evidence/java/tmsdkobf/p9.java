package tmsdkobf;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.qq.taf.jce.JceStruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tmsdk.bg.creator.BaseManagerB;
import tmsdk.bg.module.wifidetect.IWifiBatchDetectListener;
import tmsdk.bg.module.wifidetect.IWifiDetectListener;
import tmsdk.bg.module.wifidetect.WifiDetectManager;

/* JADX INFO: loaded from: classes.dex */
public class p9 extends BaseManagerB {

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private Context f9853b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private WifiManager f9854c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private Handler f9855d;

    class a extends Handler {
        a(p9 p9Var, Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            IWifiBatchDetectListener iWifiBatchDetectListener;
            int i6 = message.what;
            if (i6 != 4097) {
                if (i6 != 4098 || (iWifiBatchDetectListener = (IWifiBatchDetectListener) message.obj) == null) {
                    return;
                }
                iWifiBatchDetectListener.onResult(message.getData().getIntArray("RET"));
                return;
            }
            sb.a("WifiDetectManager", (Object) ("onResult-CLOUND_CHECK:[" + message.arg1 + "]"));
            IWifiDetectListener iWifiDetectListener = (IWifiDetectListener) message.obj;
            if (iWifiDetectListener != null) {
                iWifiDetectListener.onResult(message.arg1);
            }
        }
    }

    class b implements v3 {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        final /* synthetic */ IWifiDetectListener f9856a;

        b(IWifiDetectListener iWifiDetectListener) {
            this.f9856a = iWifiDetectListener;
        }

        @Override // tmsdkobf.v3
        public void onFinish(int i6, int i7, int i8, int i9, JceStruct jceStruct) {
            int i10;
            sb.a("WifiDetectManager", (Object) ("onFinish retCode:[" + i8 + "]dataRetCode:[" + i9 + "]"));
            sb.b("WifiDetectManager", "[WifiDetectManager] disconnect for detectDnsAndPhishing");
            Message messageObtainMessage = p9.this.f9855d.obtainMessage(4097);
            if (i8 == 0 && jceStruct != null && (jceStruct instanceof c1)) {
                c1 c1Var = (c1) jceStruct;
                int i11 = c1Var.f9011b;
                if (i11 == 2) {
                    sb.a("WifiDetectManager", (Object) "[CLOUND_CHECK_DNS_FAKE]ESafeType.EST_DnsException");
                    y9.c(1320068, "CLOUND_DNS_FAKE");
                    i10 = 18;
                } else if (i11 == 3) {
                    sb.a("WifiDetectManager", (Object) "[CLOUND_CHECK_PHISHING_FAKE]ESafeType.EST_Phishing");
                    y9.c(1320068, "CLOUND_PHISHING_FAKE");
                    i10 = 19;
                } else if (i11 == 4) {
                    sb.a("WifiDetectManager", (Object) "[CLOUND_CHECK_PASSWORD_LEAK_RISK]ESafeType.EST_PasswordLeak");
                    y9.c(1320068, "CLOUND_PASSWORD_LEAK_RISK");
                    i10 = 20;
                } else {
                    sb.a("WifiDetectManager", (Object) ("[CLOUND_CHECK_NO_FAKE]safeType:[" + c1Var.f9011b + "]"));
                    y9.c(1320068, "CLOUND_NO_FAKE");
                    i10 = 17;
                }
            } else {
                sb.a("WifiDetectManager", (Object) "[CLOUND_CHECK_NETWORK_ERROR]");
                i10 = 16;
            }
            messageObtainMessage.arg1 = i10;
            messageObtainMessage.obj = this.f9856a;
            p9.this.f9855d.sendMessage(messageObtainMessage);
        }
    }

    class c implements v3 {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        final /* synthetic */ int f9858a;

        /* JADX INFO: renamed from: b, reason: collision with root package name */
        final /* synthetic */ z0 f9859b;

        /* JADX INFO: renamed from: c, reason: collision with root package name */
        final /* synthetic */ int[] f9860c;

        /* JADX INFO: renamed from: d, reason: collision with root package name */
        final /* synthetic */ Message f9861d;

        c(int i6, z0 z0Var, int[] iArr, Message message) {
            this.f9858a = i6;
            this.f9859b = z0Var;
            this.f9860c = iArr;
            this.f9861d = message;
        }

        @Override // tmsdkobf.v3
        public void onFinish(int i6, int i7, int i8, int i9, JceStruct jceStruct) {
            sb.a("WifiDetectManager", (Object) ("onFinish Cmd_CSBatchGetCloudCheck retCode:[" + i8 + "]dataRetCode:[" + i9 + "]"));
            sb.b("WifiDetectManager", "[WifiDetectManager] disconnect for detectPhishingBatch");
            boolean z6 = true;
            if (i8 == 0 && jceStruct != null && (jceStruct instanceof b1)) {
                b1 b1Var = (b1) jceStruct;
                ArrayList<Integer> arrayList = b1Var.f8977b;
                if (arrayList == null || arrayList.size() != this.f9858a) {
                    z6 = false;
                } else {
                    for (int i10 = 0; i10 < this.f9858a; i10++) {
                        sb.c("WifiDetectManager", (Object) ("[" + this.f9859b.f10542b.get(i10).f9072b + "][" + b1Var.f8977b.get(i10).intValue() + "]"));
                        if (b1Var.f8977b.get(i10).intValue() == 3) {
                            this.f9860c[i10] = 19;
                        } else if (b1Var.f8977b.get(i10).intValue() == 4) {
                            this.f9860c[i10] = 20;
                        } else {
                            this.f9860c[i10] = 17;
                        }
                    }
                }
            }
            if (z6) {
                Bundle bundle = new Bundle();
                bundle.putIntArray("RET", this.f9860c);
                this.f9861d.setData(bundle);
            }
            p9.this.f9855d.sendMessage(this.f9861d);
        }
    }

    public p9() {
        new AtomicBoolean(false);
    }

    private String b(InputStream inputStream) {
        String strA = a(inputStream);
        sb.a("WifiDetectManager", (Object) ("parsePage-pageContent:[" + strA + "]"));
        String[] strArr = {"http-equiv\\s*=\\s*[\"']*refresh[\"']*\\s*content\\s*=\\s*[\"']*[^;]*;\\s*url\\s*=\\s*[\"']*([^\"'\\s>]+)", "[^\\w](?:location.href\\s*=|location\\s*=|location.replace\\s*\\()\\s*[\"']*([^\"'>]+)", "<NextURL>([^<]+)", "\\s+action\\s*=\\s*[\"']*([^\"'>]+)[\"'>\\s]*.*submit", "<LoginURL>([^<]+)"};
        String strGroup = null;
        for (int i6 = 0; i6 < 5 && strGroup == null; i6++) {
            Matcher matcher = Pattern.compile(strArr[i6], 2).matcher(strA);
            while (matcher.find() && strGroup == null) {
                strGroup = matcher.group(matcher.groupCount());
                if (strGroup != null && !strGroup.trim().toLowerCase().startsWith("http")) {
                    strGroup = null;
                }
            }
        }
        sb.a("WifiDetectManager", (Object) ("parsePage-location:[" + strGroup + "]"));
        return strGroup;
    }

    public synchronized int detectARPNoRoot(long j6) {
        int iA;
        sb.a("WifiDetectManager", (Object) "[Beg]detectARPNoRoot");
        y9.f(29978);
        try {
            iA = new q9().a(this.f9854c.getConnectionInfo().getBSSID(), j6);
        } catch (Throwable th) {
            sb.a("WifiDetectManager", (Object) ("no root detect error:[" + th + "]"));
            iA = 261;
        }
        y9.c(1320068, iA == 262 ? "NOROOT_ARP_FAKE" : "NOROOT_ARP_OK");
        sb.a("WifiDetectManager", (Object) ("[End]detectARPNoRoot-nRetState:[" + iA + "]"));
        return iA;
    }

    public int detectDnsAndPhishing(IWifiDetectListener iWifiDetectListener, long j6) {
        String strA;
        String str;
        y9.f(29979);
        if (!vb.b(this.f9853b)) {
            sb.a("WifiDetectManager", (Object) "[WifiConnected false]");
            iWifiDetectListener.onResult(-1);
            return -1;
        }
        sb.a("WifiDetectManager", (Object) ("[Beg]detectDnsAndPhishinglistener:[" + iWifiDetectListener + "]"));
        WifiInfo connectionInfo = ((WifiManager) this.f9853b.getSystemService("wifi")).getConnectionInfo();
        if (connectionInfo == null) {
            return -2;
        }
        String ssid = connectionInfo.getSSID();
        String bssid = connectionInfo.getBSSID();
        sb.a("WifiDetectManager", (Object) ("ssid:[" + ssid + "]bssid:[" + bssid + "]"));
        if (bc.b(ssid) || bc.b(bssid)) {
            sb.a("WifiDetectManager", (Object) "[End]detectDnsAndPhishing 参数不对");
            return -3;
        }
        String strA2 = null;
        try {
            DhcpInfo dhcpInfo = this.f9854c.getDhcpInfo();
            if (dhcpInfo != null) {
                strA = dc.a(dhcpInfo.dns1);
                try {
                    strA2 = dc.a(dhcpInfo.dns2);
                } catch (Exception unused) {
                }
                str = strA2;
                strA2 = strA;
            } else {
                str = null;
            }
        } catch (Exception unused2) {
            strA = null;
        }
        sb.a("WifiDetectManager", (Object) ("dns1:[" + strA2 + "]dns2:[" + str + "]"));
        a1 a1Var = new a1();
        d1 d1Var = new d1();
        a1Var.f8913b = d1Var;
        d1Var.f9072b = ssid;
        d1Var.f9073c = bssid;
        a1Var.f8914c = new ArrayList<>();
        if (strA2 != null && strA2.length() > 0) {
            a1Var.f8914c.add(strA2);
        }
        if (str != null && str.length() > 0) {
            a1Var.f8914c.add(str);
        }
        x5 x5VarF = b5.f();
        sb.a("WifiDetectManager", (Object) ("[GUID] " + x5VarF.b()));
        sb.a("WifiDetectManager", "[WifiDetectManager] connect for detectDnsAndPhishing");
        x5VarF.a(794, a1Var, new c1(), 0, new b(iWifiDetectListener), j6);
        w9.b();
        sb.a("WifiDetectManager", (Object) ("[End]detectDnsAndPhishing-ssid:[" + ssid + "]bssid:[" + bssid + "]"));
        return 0;
    }

    public int detectNetworkState() {
        HttpURLConnection httpURLConnection;
        y9.f(29981);
        sb.a("WifiDetectManager", (Object) "[Beg]detectNetworkState-urlApprove:[http://tools.3g.qq.com/wifi/cw.html]customHeader:[Meri]");
        int i6 = 2;
        HttpURLConnection httpURLConnection2 = null;
        try {
            long jCurrentTimeMillis = System.currentTimeMillis();
            sb.a("WifiDetectManager", (Object) "openConnection");
            httpURLConnection = (HttpURLConnection) new URL("http://tools.3g.qq.com/wifi/cw.html").openConnection();
            try {
                if (zb.a() < 8) {
                    System.setProperty("http.keepAlive", "false");
                }
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setRequestProperty("Pragma", "no-cache");
                httpURLConnection.setRequestProperty("Cache-Control", "no-cache");
                httpURLConnection.setInstanceFollowRedirects(false);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setReadTimeout(30000);
                int responseCode = httpURLConnection.getResponseCode();
                sb.a("WifiDetectManager", (Object) ("getResponseCode:[" + responseCode + "]timeMillis:[" + (System.currentTimeMillis() - jCurrentTimeMillis) + "]"));
                if (responseCode == 200 || (responseCode >= 301 && responseCode <= 305)) {
                    String headerField = httpURLConnection.getHeaderField("Meri");
                    sb.a("WifiDetectManager", (Object) ("customHeader: " + headerField));
                    if (headerField == null || headerField.compareToIgnoreCase("Meri") != 0) {
                        String strA = a("http://tools.3g.qq.com/wifi/cw.html", httpURLConnection);
                        if (strA != null && strA.compareToIgnoreCase("http://miwifi.com/diagnosis/index.html") != 0) {
                            i6 = 3;
                        }
                    } else {
                        i6 = 1;
                    }
                }
            } catch (Throwable th) {
                th = th;
                httpURLConnection2 = httpURLConnection;
                try {
                    sb.a("WifiDetectManager", (Object) ("Throwable:[" + th + "]"));
                    if (httpURLConnection2 != null) {
                        httpURLConnection = httpURLConnection2;
                    }
                    sb.a("WifiDetectManager", (Object) ("[End]detectNetworkState-nRet:[" + i6 + "]"));
                    return i6;
                } catch (Throwable th2) {
                    if (httpURLConnection2 != null) {
                        httpURLConnection2.disconnect();
                    }
                    throw th2;
                }
            }
        } catch (Throwable th3) {
            th = th3;
        }
        httpURLConnection.disconnect();
        sb.a("WifiDetectManager", (Object) ("[End]detectNetworkState-nRet:[" + i6 + "]"));
        return i6;
    }

    public int detectPhishingBatch(List<String> list, List<String> list2, IWifiBatchDetectListener iWifiBatchDetectListener, long j6) {
        y9.f(29979);
        Message messageObtainMessage = this.f9855d.obtainMessage(4098);
        messageObtainMessage.obj = iWifiBatchDetectListener;
        if (!vb.d()) {
            sb.a("WifiDetectManager", (Object) "[isNetworkConnected false]");
            this.f9855d.sendMessage(messageObtainMessage);
            return -1;
        }
        sb.a("WifiDetectManager", (Object) ("detectPhishingBatchlistener:[" + iWifiBatchDetectListener + "]"));
        z0 z0Var = new z0();
        z0Var.f10542b = new ArrayList<>();
        int size = list.size();
        int[] iArr = new int[size];
        for (int i6 = 0; i6 < size; i6++) {
            d1 d1Var = new d1();
            d1Var.f9072b = list.get(i6);
            d1Var.f9073c = list2.get(i6);
            z0Var.f10542b.add(d1Var);
            iArr[i6] = 16;
        }
        z0Var.f10549i = xb.e();
        x5 x5VarF = b5.f();
        sb.c("WifiDetectManager", (Object) ("[GUID] " + x5VarF.b()));
        sb.a("WifiDetectManager", "[WifiDetectManager] connect for detectPhishingBatch");
        x5VarF.a(800, z0Var, new b1(), 0, new c(size, z0Var, iArr, messageObtainMessage), j6);
        return 0;
    }

    /* JADX WARN: Code duplicated, block: B:27:0x00d5  */
    /* JADX WARN: Code duplicated, block: B:28:0x00d8  */
    public int detectSSLStrip() {
        HttpURLConnection httpURLConnection;
        Object th;
        String str;
        sb.a("WifiDetectManager", (Object) "[Beg]detectSSLStrip");
        y9.f(1320084);
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (!vb.b(this.f9853b)) {
            sb.a("WifiDetectManager", (Object) "[End]detectSSLStrip-SSLSTRIP_WIFI_NOTAVILABLE");
            return WifiDetectManager.SSLSTRIP_WIFI_NOTAVILABLE;
        }
        int i6 = WifiDetectManager.SSLSTRIP_OK;
        try {
            httpURLConnection = (HttpURLConnection) new URL("http://tools.3g.qq.com/j/sslstrip").openConnection();
            try {
                httpURLConnection.setConnectTimeout(com.xiaomi.onetrack.g.b.f2461a);
                httpURLConnection.setReadTimeout(com.xiaomi.onetrack.g.b.f2461a);
                httpURLConnection.setInstanceFollowRedirects(false);
                int responseCode = httpURLConnection.getResponseCode();
                List<String> list = httpURLConnection.getHeaderFields().get("Location");
                if (list.size() == 1) {
                    String lowerCase = list.get(0).toLowerCase();
                    sb.c("WifiDetectManager", (Object) ("url: " + lowerCase));
                    if (lowerCase.startsWith("http://m.qq.com")) {
                        i6 = 272;
                    }
                } else if (responseCode == 200) {
                    sb.c("WifiDetectManager", (Object) "StatusCode 200");
                    i6 = 272;
                }
            } catch (Throwable th2) {
                th = th2;
                try {
                    sb.b("WifiDetectManager", (Object) ("t:[" + th + "]"));
                    if (httpURLConnection != null) {
                    }
                    sb.a("WifiDetectManager", (Object) ("[End]detectSSLStrip-nRetState:[" + i6 + "]time(millis) elapsed:[" + (System.currentTimeMillis() - jCurrentTimeMillis) + "]"));
                    if (i6 == 272) {
                        str = "SSLSTRIP_ATTACK";
                    } else {
                        str = "SSLSTRIP_OK";
                    }
                    y9.c(1320068, str);
                    return i6;
                } catch (Throwable th3) {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    throw th3;
                }
            }
        } catch (Throwable th4) {
            httpURLConnection = null;
            th = th4;
        }
        httpURLConnection.disconnect();
        sb.a("WifiDetectManager", (Object) ("[End]detectSSLStrip-nRetState:[" + i6 + "]time(millis) elapsed:[" + (System.currentTimeMillis() - jCurrentTimeMillis) + "]"));
        if (i6 == 272) {
            str = "SSLSTRIP_ATTACK";
        } else {
            str = "SSLSTRIP_OK";
        }
        y9.c(1320068, str);
        return i6;
    }

    public int detectSecurity(ScanResult scanResult) {
        String str;
        int i6;
        y9.f(29980);
        sb.a("WifiDetectManager", (Object) ("[Beg]detectSecurity-AP:[" + scanResult + "]"));
        if (scanResult == null || (str = scanResult.capabilities) == null) {
            return -2;
        }
        if (str.contains("WEP")) {
            i6 = 257;
        } else if (scanResult.capabilities.contains("PSK")) {
            i6 = 258;
        } else if (scanResult.capabilities.contains("EAP")) {
            i6 = 259;
        } else {
            i6 = scanResult.capabilities.contains("SAE") ? 260 : WifiDetectManager.SECURITY_NONE;
        }
        sb.a("WifiDetectManager", (Object) ("[End]detectSecurity-Ret:[" + i6 + "]"));
        return i6;
    }

    @Override // tmsdkobf.r4
    public int getSingletonType() {
        return 2;
    }

    @Override // tmsdkobf.r4
    public void onCreate(Context context) {
        sb.a("WifiDetectManager", (Object) ("OnCreate-context:[" + context + "]"));
        this.f9853b = context;
        this.f9854c = (WifiManager) context.getSystemService("wifi");
        this.f9855d = new a(this, Looper.getMainLooper());
    }

    private String a(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        while (true) {
            try {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line);
            } catch (IOException unused) {
            } catch (Throwable th) {
                try {
                    bufferedReader.close();
                } catch (IOException unused2) {
                }
                throw th;
            }
        }
        bufferedReader.close();
        return sb.toString();
        try {
            bufferedReader.close();
        } catch (IOException unused3) {
        }
        return sb.toString();
    }

    /* JADX WARN: Code duplicated, block: B:17:0x0082 A[Catch: all -> 0x00c7, TryCatch #1 {all -> 0x00c7, blocks: (B:9:0x004b, B:11:0x0051, B:13:0x006e, B:15:0x0074, B:17:0x0082, B:18:0x0089, B:20:0x00a2, B:22:0x00a8, B:23:0x00ac), top: B:34:0x004b }] */
    /* JADX WARN: Code duplicated, block: B:20:0x00a2 A[Catch: all -> 0x00c7, TryCatch #1 {all -> 0x00c7, blocks: (B:9:0x004b, B:11:0x0051, B:13:0x006e, B:15:0x0074, B:17:0x0082, B:18:0x0089, B:20:0x00a2, B:22:0x00a8, B:23:0x00ac), top: B:34:0x004b }] */
    /* JADX WARN: Code duplicated, block: B:22:0x00a8 A[Catch: all -> 0x00c7, TryCatch #1 {all -> 0x00c7, blocks: (B:9:0x004b, B:11:0x0051, B:13:0x006e, B:15:0x0074, B:17:0x0082, B:18:0x0089, B:20:0x00a2, B:22:0x00a8, B:23:0x00ac), top: B:34:0x004b }] */
    /* JADX WARN: Code duplicated, block: B:36:0x00c9 A[EXC_TOP_SPLITTER, PHI: r4 r9
  0x00c9: PHI (r4v1 java.io.InputStream) = (r4v2 java.io.InputStream), (r4v4 java.io.InputStream) binds: [B:27:0x00c7, B:24:0x00c3] A[DONT_GENERATE, DONT_INLINE]
  0x00c9: PHI (r9v2 java.lang.String) = (r9v4 java.lang.String), (r9v13 java.lang.String) binds: [B:27:0x00c7, B:24:0x00c3] A[DONT_GENERATE, DONT_INLINE], SYNTHETIC] */
    private String a(String str, HttpURLConnection httpURLConnection) {
        String externalForm;
        String[] strArrSplit;
        InputStream inputStream = null;
        try {
            String host = new URL(str).getHost();
            String host2 = httpURLConnection.getURL().getHost();
            sb.a("WifiDetectManager", (Object) ("urlHost:[" + host + "]httpHost:[" + host2 + "]"));
            externalForm = !host.equals(host2) ? httpURLConnection.getURL().toExternalForm() : null;
            if (externalForm == null) {
                try {
                    if (httpURLConnection.getHeaderField("Location") != null) {
                        externalForm = httpURLConnection.getHeaderField("Location");
                        sb.a("WifiDetectManager", (Object) ("111location:[" + externalForm + "]"));
                    }
                    if (externalForm == null && httpURLConnection.getHeaderField("Refresh") != null) {
                        strArrSplit = httpURLConnection.getHeaderField("Refresh").split(";");
                        if (strArrSplit.length == 2) {
                            externalForm = strArrSplit[1].trim();
                        }
                        sb.a("WifiDetectManager", (Object) ("222location:[" + externalForm + "]"));
                    }
                    if (externalForm == null) {
                        inputStream = httpURLConnection.getInputStream();
                        if (inputStream != null) {
                            externalForm = b(inputStream);
                        }
                        sb.a("WifiDetectManager", (Object) ("333location:[" + externalForm + "]"));
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException unused) {
                        }
                    }
                } catch (Throwable unused2) {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            } else {
                if (externalForm == null) {
                    strArrSplit = httpURLConnection.getHeaderField("Refresh").split(";");
                    if (strArrSplit.length == 2) {
                        externalForm = strArrSplit[1].trim();
                    }
                    sb.a("WifiDetectManager", (Object) ("222location:[" + externalForm + "]"));
                }
                if (externalForm == null) {
                    inputStream = httpURLConnection.getInputStream();
                    if (inputStream != null) {
                        externalForm = b(inputStream);
                    }
                    sb.a("WifiDetectManager", (Object) ("333location:[" + externalForm + "]"));
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (Throwable unused3) {
            externalForm = null;
        }
        return externalForm;
    }
}
