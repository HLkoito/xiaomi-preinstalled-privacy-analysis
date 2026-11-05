package com.miui.networkassistant.utils;

import android.content.Context;
import android.content.res.MiuiConfiguration;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.WindowManager;
import com.miui.maml.folme.AnimatedProperty;
import com.miui.networkassistant.dual.Sim;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import miui.os.Build;
import miui.telephony.ServiceProviderUtils;
import x4.t;
import x4.v1;

/* JADX INFO: loaded from: classes2.dex */
public class DeviceUtil {
    public static final String CARRIER;
    private static final Set<String> CUSTOMIZED_VERSION_WHITE_LIST;
    public static final String DEVICE_NAME;
    public static final boolean IS_ALPHA_BUILD;
    public static final boolean IS_CUSTOMIZED_VERSION;
    public static final boolean IS_DEVELOPMENT_VERSION;
    public static final boolean IS_KITKAT_OR_LATER;
    public static final boolean IS_L_OR_LATER;
    public static final boolean IS_MIUI12;
    public static final boolean IS_M_OR_LATER;
    public static final boolean IS_N_OR_LATER;
    public static final boolean IS_OFFICIAL_VERSION;
    public static final boolean IS_P_OR_LATER;
    public static final boolean IS_Q_OR_LATER;
    public static final boolean IS_R_OR_LATER;
    public static final boolean IS_STABLE_VERSION;
    public static final String MIUI_VERSION;
    private static final String USE_CONTROL_PANEL = "use_control_panel";
    public static final boolean IS_CM_CUSTOMIZATION_TEST = "cm".equals(v1.c("ro.cust.test", "unknow"));
    public static final boolean IS_INTERNATIONAL_BUILD = Build.IS_INTERNATIONAL_BUILD;
    public static final boolean IS_DUAL_CARD = bh.d.a.d("miui.telephony.TelephonyManager").c("getDefault", null, new Object[0]).l().b("isMultiSimEnabled", null, new Object[0]).a();

    static {
        int i10 = android.os.Build.VERSION.SDK_INT;
        IS_KITKAT_OR_LATER = true;
        IS_L_OR_LATER = true;
        IS_M_OR_LATER = true;
        IS_N_OR_LATER = i10 >= 24;
        IS_P_OR_LATER = i10 >= 28;
        IS_Q_OR_LATER = i10 >= 29;
        IS_R_OR_LATER = i10 >= 30;
        IS_STABLE_VERSION = Build.IS_STABLE_VERSION;
        IS_DEVELOPMENT_VERSION = Build.IS_DEVELOPMENT_VERSION;
        IS_ALPHA_BUILD = Build.IS_ALPHA_BUILD;
        IS_OFFICIAL_VERSION = Build.IS_OFFICIAL_VERSION;
        IS_MIUI12 = t.h() > 9;
        HashSet hashSet = new HashSet();
        CUSTOMIZED_VERSION_WHITE_LIST = hashSet;
        hashSet.add("lm_cr");
        hashSet.add("it_vodafone");
        hashSet.add("mx_telcel");
        hashSet.add("cl_moviestar");
        hashSet.add("es_vodafone");
        hashSet.add("tr_turkcell");
        hashSet.add("cl_wom");
        hashSet.add("hk_h3g");
        hashSet.add("it_tim");
        hashSet.add("fr_orange");
        hashSet.add("fr_sfr");
        hashSet.add("ph_smart");
        hashSet.add("es_telefonica");
        hashSet.add("kh_seatel");
        hashSet.add("cl_en");
        hashSet.add("cl_entel");
        IS_CUSTOMIZED_VERSION = hashSet.contains(wg.a.a("ro.miui.customized.region", ""));
        MIUI_VERSION = android.os.Build.VERSION.INCREMENTAL;
        DEVICE_NAME = android.os.Build.DEVICE;
        CARRIER = wg.a.a("ro.carrier", "null");
    }

    private DeviceUtil() {
    }

    public static String getAndroidId(String str) {
        return tm.a.d(mm.a.b(Settings.Secure.getString(com.miui.common.e.c().getContentResolver(), "android_id") + "-" + str, "MD5"));
    }

    public static String getAppVersionCode() {
        Context contextC = com.miui.common.e.c();
        return String.valueOf(PackageUtil.getAppVersionCode(contextC.getPackageManager(), contextC.getPackageName()));
    }

    public static String getImeiMd5() {
        return getMd5(TelephonyUtil.getImei());
    }

    public static String getImeiMd5LastLetter() {
        String imeiMd5 = getImeiMd5();
        return TextUtils.isEmpty(imeiMd5) ? "0" : imeiMd5.substring(imeiMd5.length() - 1);
    }

    public static String getMd5(String str) {
        return TextUtils.isEmpty(str) ? "" : tm.a.d(mm.a.b(str, "MD5"));
    }

    public static String getMiuiVersionType() {
        if (IS_STABLE_VERSION) {
            return "stable";
        }
        if (IS_ALPHA_BUILD) {
            return AnimatedProperty.PROPERTY_NAME_ALPHA;
        }
        return IS_DEVELOPMENT_VERSION ? "dev" : "stable";
    }

    public static String getRegion() {
        return Build.getRegion();
    }

    public static int getScreenWidth(Context context) {
        return ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getWidth();
    }

    public static boolean isCNLanguage() {
        String language = Locale.getDefault().getLanguage();
        return language != null && language.equals("zh");
    }

    public static boolean isCmccSIM(Context context) {
        return ServiceProviderUtils.isChinaMobile(TelephonyUtil.getSubscriberId(Sim.getCurrentActiveSlotNum()));
    }

    public static boolean isCnSIM(Context context) {
        String subscriberId = TelephonyUtil.getSubscriberId(Sim.getCurrentActiveSlotNum());
        return ServiceProviderUtils.isChinaMobile(subscriberId) || ServiceProviderUtils.isChinaUnicom(subscriberId) || ServiceProviderUtils.isChinaTelecom(subscriberId);
    }

    public static boolean isDarkMode(Context context) {
        return (context.getResources().getConfiguration().uiMode & 48) == 32;
    }

    public static boolean isLargeScaleMode() {
        int scaleMode = MiuiConfiguration.getScaleMode();
        return scaleMode == 13 || scaleMode == 14 || scaleMode == 15 || scaleMode == 11;
    }

    public static boolean isMiPushRestricted(Context context) {
        return Settings.System.getInt(context.getContentResolver(), "mishare_wifi_connect_state", 0) == 1;
    }

    public static boolean isSmartDiagnostics(Context context) {
        return context != null && !IS_INTERNATIONAL_BUILD && IS_Q_OR_LATER && p4.d.e(context.getApplicationContext()) && isCnSIM(context.getApplicationContext());
    }

    public static boolean isUseControlPanel(Context context) {
        if (!IS_MIUI12) {
            return false;
        }
        String string = Settings.System.getString(context.getContentResolver(), USE_CONTROL_PANEL);
        return TextUtils.isEmpty(string) || Integer.parseInt(string) == 1;
    }

    public static boolean isXiaoMiDevice(Context context) {
        return true;
    }

    public static void rebootPhone(Context context, String str) {
        ((PowerManager) context.getSystemService("power")).reboot(str);
    }
}
