package ef;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Log;
import com.miui.powercenter.bean.StatusBarGuideParams;
import com.miui.securitycenter.service.NotificationService;
import java.util.ArrayList;
import java.util.List;
import miui.os.Build;
import x4.l1;
import x4.v1;

/* JADX INFO: loaded from: classes3.dex */
public class x {
    public static boolean A(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "key_launcher_cleaner_first", 1) == 1;
    }

    public static boolean B(Context context) {
        boolean zB = a8.x.b(context);
        ContentResolver contentResolver = context.getContentResolver();
        return zB ? bm.a.a(contentResolver, "key_garbage_danger_in_flag", false) : bm.b.a(contentResolver, "key_garbage_danger_in_flag", false);
    }

    public static boolean C(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "key_homelist_cache_deleted", 0) == 1;
    }

    public static boolean D(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "key_launcher_loading_finished", 0) == 1;
    }

    public static boolean E(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "key_is_locked_application", x4.t.v("com.android.settings", 0) ? 1 : 0) == 1;
    }

    public static boolean F(Context context) {
        return bm.a.a(context.getContentResolver(), "optimizer_scan_cloud", false);
    }

    public static boolean G() {
        return r4.a.e("pref_key_predict_widget_enable", false);
    }

    public static boolean H(ContentResolver contentResolver) {
        return !com.miui.common.a.d() && MiuiSettings.System.getBoolean(contentResolver, "extra_show_security_notification", false);
    }

    public static boolean I() {
        return r4.a.e("show_auto_task_desktop_icon_dialog", true);
    }

    public static boolean J(Context context) {
        boolean zB = a8.x.b(context);
        ContentResolver contentResolver = context.getContentResolver();
        return zB ? bm.a.a(contentResolver, "key_notification_wechat_size_need", false) : bm.b.a(contentResolver, "key_notification_wechat_size_need", false);
    }

    public static boolean K(Context context) {
        boolean zB = a8.x.b(context);
        ContentResolver contentResolver = context.getContentResolver();
        return zB ? bm.a.a(contentResolver, "key_notification_whatsapp_clean_need", false) : bm.b.a(contentResolver, "key_notification_whatsapp_clean_need", false);
    }

    public static void L(boolean z10) {
        r4.a.n("pref_key_ai_enable", z10);
    }

    public static void M(ArrayList<String> arrayList) {
        r4.a.s("pref_key_ai_labels", arrayList);
    }

    public static void N(boolean z10) {
        r4.a.n("pref_key_agree_ai_predict_privacy", z10);
    }

    public static void O(Context context, boolean z10) {
        Settings.Secure.putInt(context.getContentResolver(), "key_app_lock_state_data_migrated", z10 ? 1 : 0);
    }

    public static void P(Context context, boolean z10) {
        bm.a.g(context.getContentResolver(), "key_cleanup_db_auto_update_enabled", z10);
    }

    public static void Q(Context context, boolean z10, long j10) {
        bm.a.g(context.getContentResolver(), "key_notificaiton_general_clean_need", z10);
        bm.a.i(context.getContentResolver(), "key_notificaiton_general_clean_size", j10);
        bm.b.e(context.getContentResolver(), "key_notificaiton_general_clean_need", z10);
        bm.b.g(context.getContentResolver(), "key_notificaiton_general_clean_size", j10);
    }

    public static void R(boolean z10) {
        v1.d((String) a8.c0.f("android.provider.MiuiSettings$System", "KEY_SECURITY_CENTER_ALLOW_CONNECT_NETWORK"), z10 ? com.xiaomi.onetrack.util.a.f23518i : "false");
        if (Build.IS_INTERNATIONAL_BUILD) {
            a8.c0.g(com.miui.common.e.c(), StatusBarGuideParams.MY_PACKAGE_NAME, z10);
        }
        a(z10);
    }

    public static void S(List<String> list) {
        ArrayList<String> arrayListM = r4.a.m("KEY_FBO_RESULT_PKG_LIST", new ArrayList());
        for (String str : list) {
            if (!arrayListM.contains(str)) {
                arrayListM.add(str);
            }
        }
        r4.a.s("KEY_FBO_RESULT_PKG_LIST", arrayListM);
    }

    public static void T(Context context, boolean z10) {
        Settings.Secure.putInt(context.getContentResolver(), "key_launcher_cleaner_first", z10 ? 1 : 0);
    }

    public static void U(Context context, boolean z10) {
        Settings.Secure.putInt(context.getContentResolver(), "key_homelist_cache_deleted", z10 ? 1 : 0);
    }

    public static void V(ArrayList<String> arrayList) {
        r4.a.s("pref_key_ignore_suggest_data", arrayList);
    }

    public static void W(Context context, boolean z10) {
        Settings.Secure.putInt(context.getContentResolver(), "key_is_in_miui_sos_mode", z10 ? 1 : 0);
    }

    public static final void X(boolean z10) {
        r4.a.n("key_is_deleted_privacy_file", z10);
    }

    public static void Y(long j10) {
        r4.a.q("key_last_get_wn_check_result_time", j10);
    }

    public static void Z(long j10) {
        r4.a.q("key_last_pre_scan_time", j10);
    }

    private static void a(boolean z10) {
        Context contextC = com.miui.common.e.c();
        Intent intent = new Intent("action_update_sc_network_allow");
        intent.putExtra("extra_network_status", z10);
        contextC.sendBroadcast(intent);
    }

    public static void a0(long j10) {
        r4.a.q("key_last_upload_switch_analytics_time", j10);
    }

    public static ArrayList<String> b() {
        return r4.a.m("pref_key_ai_labels", new ArrayList());
    }

    public static void b0(Context context, boolean z10) {
        Settings.Secure.putInt(context.getContentResolver(), "key_launcher_loading_finished", z10 ? 1 : 0);
    }

    public static long c(Context context) {
        boolean zB = a8.x.b(context);
        ContentResolver contentResolver = context.getContentResolver();
        return zB ? bm.a.d(contentResolver, "key_notificaiton_general_clean_size", 0L) : bm.b.c(contentResolver, "key_notificaiton_general_clean_size", 0L);
    }

    public static void c0(Context context, int i10) {
        bm.a.h(context.getContentResolver(), "key_minus_score_in_security_exclude_virus", i10);
    }

    public static String d() {
        return v.a();
    }

    public static void d0(Context context, boolean z10) {
        bm.a.g(context.getContentResolver(), "optimizer_scan_cloud", z10);
    }

    public static long e(Context context) {
        boolean zB = a8.x.b(context);
        ContentResolver contentResolver = context.getContentResolver();
        return zB ? bm.a.d(contentResolver, "key_garbage_danger_in_size", 0L) : bm.b.c(contentResolver, "key_garbage_danger_in_size", 0L);
    }

    public static void e0(String str) {
        r4.a.r("pref_key_pre_input_method", str);
    }

    public static long f(Context context) {
        return Settings.Secure.getLong(context.getContentResolver(), "key_garbage_normal_size", 0L);
    }

    public static void f0(boolean z10) {
        r4.a.n("pref_key_predict_widget_enable", z10);
    }

    public static boolean g() {
        return r4.a.e("key_risk_application_warning_enable", true);
    }

    public static void g0(boolean z10) {
        r4.a.n("pref_key_privacy_uploaded", z10);
    }

    public static boolean h() {
        return r4.a.e("key_risk_sms_and_call_pay_warn_enable", true);
    }

    public static void h0(boolean z10) {
        r4.a.n("key_risk_application_warning_enable", z10);
    }

    public static ArrayList<String> i() {
        return r4.a.m("pref_key_ignore_suggest_data", new ArrayList());
    }

    public static void i0(boolean z10) {
        r4.a.n("key_risk_sms_and_call_pay_warn_enable", z10);
    }

    public static final boolean j() {
        return r4.a.e("key_is_deleted_privacy_file", false);
    }

    public static void j0(Context context, int i10) {
        bm.a.h(context.getContentResolver(), "key_score_in_security", i10);
    }

    public static long k() {
        return r4.a.j("key_last_pre_scan_time", 0L);
    }

    public static void k0(ContentResolver contentResolver, boolean z10) {
        if (com.miui.common.a.d()) {
            return;
        }
        try {
            bh.f.h(Class.forName("android.provider.MiuiSettings$System"), "putBoolean", new Class[]{ContentResolver.class, String.class, Boolean.TYPE}, contentResolver, "extra_show_security_notification", Boolean.valueOf(z10));
        } catch (Exception e10) {
            Log.i("Preferences", e10.toString());
        }
    }

    public static long l() {
        return r4.a.j("key_last_upload_switch_analytics_time", 0L);
    }

    public static void l0(Context context, boolean z10) {
        if (com.miui.common.a.d()) {
            return;
        }
        k0(context.getContentResolver(), z10);
        Intent intent = new Intent(context, (Class<?>) NotificationService.class);
        intent.setPackage(context.getPackageName());
        if (z10) {
            context.startService(intent);
        } else {
            context.stopService(intent);
        }
    }

    public static int m(Context context) {
        return bm.a.c(context.getContentResolver(), "key_minus_score_in_security_exclude_virus", 0);
    }

    public static void m0(boolean z10) {
        r4.a.n("show_auto_task_desktop_icon_dialog", z10);
    }

    public static String n() {
        return r4.a.l("pref_key_pre_input_method", "");
    }

    public static void n0(long j10) {
        r4.a.q("key_start_device_time_for_gms", j10);
    }

    public static boolean o() {
        return r4.a.e("pref_key_privacy_uploaded", false);
    }

    public static void o0(long j10) {
        r4.a.q("key_trigger_clean_master_auto_clean_time", j10);
    }

    public static int p(Context context) {
        return bm.a.c(context.getContentResolver(), "key_score_in_security", 100);
    }

    public static void p0(Context context, boolean z10, long j10) {
        bm.a.g(context.getContentResolver(), "key_notification_wechat_size_need", z10);
        bm.a.i(context.getContentResolver(), "key_notification_wechat_size", j10);
        bm.b.e(context.getContentResolver(), "key_notification_wechat_size_need", z10);
        bm.b.g(context.getContentResolver(), "key_notification_wechat_size", j10);
    }

    public static long q() {
        return r4.a.j("key_start_device_time_for_gms", -1L);
    }

    public static void q0(Context context, boolean z10, long j10) {
        bm.a.g(context.getContentResolver(), "key_notification_whatsapp_clean_need", z10);
        bm.a.i(context.getContentResolver(), "key_notificaiton_whatsapp_clean_size", j10);
        bm.b.e(context.getContentResolver(), "key_notification_whatsapp_clean_need", z10);
        bm.b.g(context.getContentResolver(), "key_notificaiton_whatsapp_clean_size", j10);
    }

    public static long r() {
        return r4.a.j("key_trigger_clean_master_auto_clean_time", 0L);
    }

    public static long s(Context context) {
        boolean zB = a8.x.b(context);
        ContentResolver contentResolver = context.getContentResolver();
        return zB ? bm.a.d(contentResolver, "key_notification_wechat_size", 0L) : bm.b.c(contentResolver, "key_notification_wechat_size", 0L);
    }

    public static long t(Context context) {
        boolean zB = a8.x.b(context);
        ContentResolver contentResolver = context.getContentResolver();
        return zB ? bm.a.d(contentResolver, "key_notificaiton_whatsapp_clean_size", 0L) : bm.b.c(contentResolver, "key_notificaiton_whatsapp_clean_size", 0L);
    }

    public static boolean u() {
        return r4.a.e("pref_key_ai_enable", false);
    }

    public static boolean v() {
        return r4.a.e("pref_key_agree_ai_predict_privacy", false);
    }

    public static boolean w(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "key_app_lock_state_data_migrated", 0) == 1;
    }

    public static boolean x(Context context) {
        return bm.a.a(context.getContentResolver(), "key_cleanup_db_auto_update_enabled", true);
    }

    public static boolean y(Context context) {
        boolean zB = a8.x.b(context);
        ContentResolver contentResolver = context.getContentResolver();
        return zB ? bm.a.a(contentResolver, "key_notificaiton_general_clean_need", false) : bm.b.a(contentResolver, "key_notificaiton_general_clean_need", false);
    }

    public static boolean z() {
        return l1.b();
    }
}
