package com.miui.guardprovider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.util.Log;
import d0.d;
import miui.os.Build;
import w.c;
import w0.f;
import w0.g;

/* JADX INFO: loaded from: classes.dex */
public class GuardApplication extends b {

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    public static GuardApplication f1743d;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private BroadcastReceiver f1744c;

    class a extends BroadcastReceiver {
        a() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                f.b(context);
                Log.d("GuardProvider", "init task when boot completed");
                GuardApplication.this.g();
            }
        }
    }

    public static boolean e(Context context) {
        if (!Build.IS_INTERNATIONAL_BUILD) {
            return d.b("persist.sys.sc_allow_conn", false);
        }
        String strA = d.a("ro.miui.customized.region", "");
        boolean z6 = d0.b.c(context, "com.miui.securitycenter") && d.b("persist.sys.sc_allow_conn", false);
        if ("jp_kd".equals(strA)) {
            return z6 && (Settings.Secure.getInt(context.getContentResolver(), "antivirus_engine_network_enable", 0) == 1);
        }
        return z6;
    }

    private void f() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.USER_UNLOCKED");
        if (this.f1744c == null) {
            this.f1744c = new a();
        }
        registerReceiver(this.f1744c, intentFilter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void g() {
        BroadcastReceiver broadcastReceiver = this.f1744c;
        if (broadcastReceiver != null) {
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (Exception e7) {
                e7.printStackTrace();
            }
            this.f1744c = null;
        }
    }

    @Override // android.content.ContextWrapper
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        f1743d = this;
        if (g.b(context)) {
            f.b(context);
        } else {
            f();
        }
    }

    @Override // com.miui.guardprovider.b, android.app.Application
    public void onCreate() {
        super.onCreate();
        c.a(this);
    }
}
