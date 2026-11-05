package com.miui.securitycenter.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.exoplayer2.C;
import com.google.common.base.Ascii;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import miui.security.SecurityManager;
import x4.z0;
import x4.z1;

/* JADX INFO: loaded from: classes3.dex */
public class DeviceCredentialProvider extends ContentProvider {

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private static final Map<String, String> f20343b;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private SecurityManager f20344a;

    static {
        HashMap map = new HashMap();
        f20343b = map;
        map.put("com.xiangkan.android", "E7:C0:85:D3:33:98:17:BD:13:7F:AD:9A:2B:11:AC:96:DD:1D:45:A4:00:F9:48:4B:3C:57:97:80:01:E4:F0:73");
        map.put("com.xiaomi.jr", "C9:00:9D:01:EB:F9:F5:D0:30:2B:C7:1B:2F:E9:AA:9A:47:A4:32:BB:A1:73:08:A3:11:1B:75:D7:B2:14:90:25");
        map.put("com.xiaomi.loan", "D9:92:69:71:E9:B8:49:B6:A6:52:64:CE:AD:4D:26:B9:1D:5F:95:82:08:ED:25:F1:73:7B:BC:17:70:27:8D:FF");
        map.put("com.xiaomi.loanx", "D9:92:69:71:E9:B8:49:B6:A6:52:64:CE:AD:4D:26:B9:1D:5F:95:82:08:ED:25:F1:73:7B:BC:17:70:27:8D:FF");
        map.put("com.wali.live", "B0:31:FE:98:A4:DB:B0:D4:D8:26:61:78:7F:25:DE:64:31:82:B3:78:E9:EF:63:2D:8A:DE:A7:5A:AB:58:F2:D8");
        map.put("com.xiaomi.shop", "C9:00:9D:01:EB:F9:F5:D0:30:2B:C7:1B:2F:E9:AA:9A:47:A4:32:BB:A1:73:08:A3:11:1B:75:D7:B2:14:90:25");
        map.put("com.xiaomi.youpin", "A8:C2:EE:61:F5:59:97:7A:39:B2:F6:EE:A7:5D:05:FE:4C:B4:D5:B8:0D:CA:F4:B0:CF:23:30:E9:BE:71:8D:34");
        map.put("com.xiaomi.o2o", "C9:00:9D:01:EB:F9:F5:D0:30:2B:C7:1B:2F:E9:AA:9A:47:A4:32:BB:A1:73:08:A3:11:1B:75:D7:B2:14:90:25");
        map.put("com.xiaomi.smarthome", "B0:31:FE:98:A4:DB:B0:D4:D8:26:61:78:7F:25:DE:64:31:82:B3:78:E9:EF:63:2D:8A:DE:A7:5A:AB:58:F2:D8");
        map.put("com.duokan.reader", "88:7E:40:DA:D9:6C:D7:B4:CC:0A:59:67:2B:93:81:19:9F:7D:E2:04:15:B8:92:D7:06:89:5F:84:93:17:8E:2A");
        map.put("com.duokan.fiction", "88:7E:40:DA:D9:6C:D7:B4:CC:0A:59:67:2B:93:81:19:9F:7D:E2:04:15:B8:92:D7:06:89:5F:84:93:17:8E:2A");
        map.put("com.miui.miuibbs", "C9:00:9D:01:EB:F9:F5:D0:30:2B:C7:1B:2F:E9:AA:9A:47:A4:32:BB:A1:73:08:A3:11:1B:75:D7:B2:14:90:25");
        map.put("com.kuaiest.video", "E1:D4:BB:28:5E:26:6D:A6:F5:FC:23:ED:9F:C1:03:CF:11:93:23:B2:E5:02:31:6A:EE:8C:7A:66:E5:A6:99:51");
        map.put("com.xiaomi.antifake3", "D4:5F:07:6F:E2:3A:1A:5B:7F:48:6E:3F:F4:15:47:A2:02:3D:BF:E1:FE:73:35:3B:1E:48:EB:DF:ED:72:CC:6F");
    }

    private static String a(byte[] bArr) {
        char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        StringBuilder sb2 = new StringBuilder();
        for (byte b10 : bArr) {
            int i10 = (b10 >> 4) & 15;
            int i11 = b10 & Ascii.SI;
            sb2.append(cArr[i10]);
            sb2.append(cArr[i11]);
        }
        return sb2.toString();
    }

    public static String b(String str) {
        try {
            return a(z0.g().n(1, str.getBytes(C.UTF8_NAME), true)).toLowerCase();
        } catch (Exception e10) {
            Log.e("DeviceCredentialProvider", " getFidNonceSign failure:  ", e10);
            return "";
        }
    }

    private static PackageInfo c(Context context, String str) {
        try {
            return context.getPackageManager().getPackageInfo(str, 64);
        } catch (PackageManager.NameNotFoundException e10) {
            Log.e("DeviceCredentialProvider", "getPackageVersionName failure", e10);
            return null;
        }
    }

    private static String d(PackageInfo packageInfo) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
            messageDigest.update(packageInfo.signatures[0].toByteArray());
            StringBuilder sb2 = new StringBuilder();
            byte[] bArrDigest = messageDigest.digest();
            int length = bArrDigest.length;
            for (int i10 = 0; i10 < length; i10++) {
                if (i10 > 0) {
                    sb2.append(":");
                }
                sb2.append(Integer.toString((bArrDigest[i10] & 255) + 256, 16).substring(1));
            }
            return sb2.toString().toUpperCase();
        } catch (Exception e10) {
            Log.e("DeviceCredentialProvider", "getPackageSHA256 failure", e10);
            return "";
        }
    }

    @Override // android.content.ContentProvider
    public Bundle call(String str, String str2, Bundle bundle) {
        String str3;
        String str4;
        Bundle bundle2 = new Bundle();
        if (z1.b(Binder.getCallingUid()) == 2000) {
            bundle2.putInt("error_code", -108);
            Log.d("DeviceCredentialProvider", "current appid has no permissions");
            return bundle2;
        }
        try {
            int callingPid = Binder.getCallingPid();
            String packageNameByPid = this.f20344a.getPackageNameByPid(callingPid);
            Log.d("DeviceCredentialProvider", "callingPid is [" + callingPid + "] and the callingPackageName is [" + packageNameByPid + "]");
            if (packageNameByPid == null) {
                Log.d("DeviceCredentialProvider", "current calling packageName is null");
                bundle2.putInt("error_code", -100);
                return bundle2;
            }
            try {
                if ((getContext().getPackageManager().getApplicationInfo(packageNameByPid, 0).flags & 1) == 0) {
                    String str5 = f20343b.get(packageNameByPid);
                    if (TextUtils.isEmpty(str5)) {
                        bundle2.putInt("error_code", -101);
                        Log.d("DeviceCredentialProvider", "current application has no permissions");
                        return bundle2;
                    }
                    if (!str5.equals(d(c(getContext(), packageNameByPid)))) {
                        bundle2.putInt("error_code", -103);
                        Log.d("DeviceCredentialProvider", "current application signature error");
                        return bundle2;
                    }
                }
            } catch (Exception e10) {
                Log.e("DeviceCredentialProvider", "getApplicationInfo", e10);
            }
            if ("getContentSign".equals(str)) {
                if (TextUtils.isEmpty(str2)) {
                    bundle2.putInt("error_code", -104);
                    str4 = "the parameters that need to be signed are empty";
                } else {
                    String strB = b(str2);
                    if (TextUtils.isEmpty(strB)) {
                        bundle2.putInt("error_code", -105);
                        str4 = "signature failure,the device may not support";
                    } else {
                        Log.d("DeviceCredentialProvider", "signature success");
                        bundle2.putInt("error_code", 100);
                        bundle2.putString("sign", strB);
                    }
                }
                Log.d("DeviceCredentialProvider", str4);
            } else if ("getSecurityDeviceId".equals(str)) {
                try {
                    String strF = z0.g().f();
                    bundle2.putInt("error_code", 100);
                    bundle2.putString("sid", strF);
                    Log.d("DeviceCredentialProvider", "getSecurityDeviceId success");
                } catch (Exception e11) {
                    e = e11;
                    bundle2.putInt("error_code", -106);
                    str3 = "getSecurityDeviceId failure :";
                    Log.e("DeviceCredentialProvider", str3, e);
                }
            } else if ("isThisDeviceSupported".equals(str)) {
                try {
                    boolean zE = z0.g().e();
                    bundle2.putInt("error_code", 100);
                    bundle2.putBoolean("isSupport", zE);
                    Log.d("DeviceCredentialProvider", "isThisDeviceSupported success");
                } catch (Exception e12) {
                    e = e12;
                    bundle2.putInt("error_code", -107);
                    str3 = "isThisDeviceSupported failure :";
                    Log.e("DeviceCredentialProvider", str3, e);
                }
            }
            return bundle2;
        } catch (Exception e13) {
            Log.e("DeviceCredentialProvider", "getPackageNameByPid failure,invalid package name", e13);
            bundle2.putInt("error_code", -100);
            return bundle2;
        }
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        return 0;
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        return null;
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        this.f20344a = (SecurityManager) getContext().getSystemService("security");
        return true;
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        return null;
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        return 0;
    }
}
