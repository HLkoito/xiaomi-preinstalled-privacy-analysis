package com.miui.gamebooster.gbservices;

import android.accessibilityservice.AccessibilityService;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import com.miui.gamebooster.customview.InCallNotificationView;
import com.miui.gamebooster.customview.j;
import com.miui.gamebooster.service.ISecurityCenterNotificationListener;
import com.miui.gamebooster.service.NotificationListener;
import com.miui.gamebooster.service.NotificationListenerCallback;
import com.miui.luckymoney.config.AppConstants;
import com.miui.luckymoney.utils.SettingsUtil;
import com.miui.maml.folme.AnimatedProperty;
import com.miui.securitycenter.R;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import miui.notification.NotificationRowLayout;
import x4.v;
import x4.z1;

/* JADX INFO: loaded from: classes2.dex */
public class AntiMsgAccessibilityService extends AccessibilityService {

    /* JADX INFO: renamed from: v, reason: collision with root package name */
    private static final ArrayList<String> f14222v = new a();

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private InCallNotificationView f14223a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private FrameLayout f14224b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private j f14225c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private Intent f14226d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    private Intent f14227e;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    private String f14228f;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    private int f14229g;

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    private WindowManager f14232j;

    /* JADX INFO: renamed from: k, reason: collision with root package name */
    private Handler f14233k;

    /* JADX INFO: renamed from: l, reason: collision with root package name */
    private ISecurityCenterNotificationListener f14234l;

    /* JADX INFO: renamed from: t, reason: collision with root package name */
    private NotificationListenerCallback f14242t;

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    private boolean f14230h = false;

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    private boolean f14231i = false;

    /* JADX INFO: renamed from: m, reason: collision with root package name */
    private Map<String, String> f14235m = new b();

    /* JADX INFO: renamed from: n, reason: collision with root package name */
    private BroadcastReceiver f14236n = new c();

    /* JADX INFO: renamed from: o, reason: collision with root package name */
    private final int f14237o = 1;

    /* JADX INFO: renamed from: p, reason: collision with root package name */
    private final int f14238p = 2;

    /* JADX INFO: renamed from: q, reason: collision with root package name */
    private final int f14239q = 3;

    /* JADX INFO: renamed from: r, reason: collision with root package name */
    public final int f14240r = 200;

    /* JADX INFO: renamed from: s, reason: collision with root package name */
    ObjectAnimator f14241s = new ObjectAnimator();

    /* JADX INFO: renamed from: u, reason: collision with root package name */
    private ServiceConnection f14243u = new f();

    class a extends ArrayList<String> {
        a() {
            add("[视频电话]");
            add("[语音聊天]");
            add("[视频聊天]");
            add("[QQ电话]");
            add("未接来电");
            add("[语音通话]");
            add("[视频通话]");
        }
    }

    class b extends HashMap<String, String> {
        b() {
            put("com.tencent.av.ui.VideoInviteLock", "QQ语音");
            put("com.tencent.av.ui.VideoInviteFull", "QQ电话");
            put("com.tencent.av.ui.VideoInviteActivity", "QQ电话");
            put("com.tencent.mm.plugin.voip.ui.VideoActivity", "微信电话");
        }
    }

    class c extends BroadcastReceiver {
        c() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Intent intent2;
            if (!intent.getAction().equals("miui.intent.action.gb_show_window") || (intent2 = (Intent) intent.getParcelableExtra("passby_intent")) == null) {
                return;
            }
            AntiMsgAccessibilityService.this.p(intent2);
        }
    }

    class d extends AnimatorListenerAdapter {
        d() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            AntiMsgAccessibilityService.this.m();
        }
    }

    class e extends AnimatorListenerAdapter {
        e() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            AntiMsgAccessibilityService.this.m();
        }
    }

    class f implements ServiceConnection {
        f() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            AntiMsgAccessibilityService.this.f14234l = ISecurityCenterNotificationListener.Stub.asInterface(iBinder);
            try {
                AntiMsgAccessibilityService.this.f14234l.Z2(AntiMsgAccessibilityService.this.f14242t);
            } catch (Exception e10) {
                Log.e("AntiMsgAccessibilityService", "mNoticationListenerBinder:" + e10);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            AntiMsgAccessibilityService.this.f14234l = null;
        }
    }

    private static class g implements Runnable {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private StatusBarNotification f14249a;

        /* JADX INFO: renamed from: b, reason: collision with root package name */
        private WeakReference<AntiMsgAccessibilityService> f14250b;

        public g(WeakReference<AntiMsgAccessibilityService> weakReference, StatusBarNotification statusBarNotification) {
            this.f14249a = statusBarNotification;
            this.f14250b = weakReference;
        }

        @Override // java.lang.Runnable
        public void run() {
            AntiMsgAccessibilityService antiMsgAccessibilityService = this.f14250b.get();
            if (antiMsgAccessibilityService == null) {
                return;
            }
            antiMsgAccessibilityService.l(this.f14249a.getNotification());
        }
    }

    private static class h extends NotificationListenerCallback {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private Handler f14251a;

        /* JADX INFO: renamed from: k, reason: collision with root package name */
        private WeakReference<AntiMsgAccessibilityService> f14252k;

        public h(Handler handler, AntiMsgAccessibilityService antiMsgAccessibilityService) {
            this.f14251a = handler;
            this.f14252k = new WeakReference<>(antiMsgAccessibilityService);
        }

        @Override // com.miui.gamebooster.service.NotificationListenerCallback, com.miui.gamebooster.service.INotificationListenerCallback
        public void onNotificationPostedCallBack(StatusBarNotification statusBarNotification) {
            String packageName = statusBarNotification.getPackageName();
            if (AppConstants.Package.PACKAGE_NAME_QQ.equals(packageName) || AppConstants.Package.PACKAGE_NAME_MM.equals(packageName)) {
                this.f14251a.post(new g(this.f14252k, statusBarNotification));
            }
        }

        @Override // com.miui.gamebooster.service.NotificationListenerCallback, com.miui.gamebooster.service.INotificationListenerCallback
        public void onNotificationRemovedCallBack(StatusBarNotification statusBarNotification) {
        }
    }

    private void g() {
        this.f14225c.removeAllViews();
    }

    private boolean i() {
        FrameLayout frameLayout = this.f14224b;
        return frameLayout != null && frameLayout.getVisibility() == 0;
    }

    private int k() {
        return getResources().getDimensionPixelSize(R.dimen.float_notification_panel_width);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void l(Notification notification) {
        if (notification == null) {
            return;
        }
        Bundle bundle = notification.extras;
        CharSequence charSequence = bundle != null ? bundle.getCharSequence("android.text") : null;
        if (TextUtils.isEmpty(charSequence)) {
            charSequence = notification.tickerText;
        }
        for (String str : f14222v) {
            if (charSequence != null && charSequence.toString().contains(str)) {
                h(true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void m() {
        FrameLayout frameLayout = this.f14224b;
        if (frameLayout != null) {
            frameLayout.setVisibility(8);
        }
        g();
        InCallNotificationView inCallNotificationView = this.f14223a;
        if (inCallNotificationView != null) {
            inCallNotificationView.c();
        }
    }

    private void n() {
        try {
            if (this.f14224b == null) {
                this.f14224b = (FrameLayout) LayoutInflater.from(this).inflate(R.layout.gb_status_bar_float_notification_container, (ViewGroup) null);
                WindowManager.LayoutParams layoutParamsJ = j(new ViewGroup.LayoutParams(k(), -2));
                layoutParamsJ.type = 2003;
                this.f14232j.addView(this.f14224b, layoutParamsJ);
                j jVar = new j(this);
                this.f14225c = jVar;
                jVar.a(this);
                this.f14225c.setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
                this.f14225c.setGravity(17);
                this.f14224b.addView(this.f14225c);
            }
        } catch (Exception e10) {
            Log.e("AntiMsgAccessibilityService", "init float error", e10);
        }
    }

    private boolean o(Intent intent, String str) {
        String className = intent.getComponent().getClassName();
        if (className == null) {
            return false;
        }
        if ("com.tencent.av.ui.VChatActivity".equals(className)) {
            this.f14227e = intent;
            return false;
        }
        String str2 = this.f14235m.get(className);
        if (str2 == null) {
            if (AppConstants.Package.PACKAGE_NAME_QQ.equals(str)) {
                str2 = "QQ电话";
            } else if (AppConstants.Package.PACKAGE_NAME_MM.equals(str)) {
                str2 = "微信电话";
            }
        }
        this.f14223a.e(str2, null);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void p(Intent intent) {
        this.f14228f = intent.getStringExtra("android.intent.extra.shortcut.NAME");
        this.f14226d = (Intent) intent.getParcelableExtra("android.intent.extra.INTENT");
        this.f14229g = intent.getIntExtra("originating_uid", -1);
        if (this.f14223a == null) {
            InCallNotificationView inCallNotificationView = (InCallNotificationView) LayoutInflater.from(this).inflate(R.layout.gb_notification_float_incall, (ViewGroup) null);
            this.f14223a = inCallNotificationView;
            inCallNotificationView.b(this);
        }
        if (o(this.f14226d, this.f14228f)) {
            this.f14223a.d();
            n();
            g();
            this.f14225c.addView(this.f14223a);
            this.f14224b.setVisibility(0);
            this.f14225c.setBackgroundResource(R.drawable.float_incall_notification_bg);
            r(1);
            this.f14231i = true;
            r4.a.n("gb_show_window", true);
        }
    }

    private void q(AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent.getParcelableData() == null) {
            return;
        }
        l((Notification) accessibilityEvent.getParcelableData());
    }

    private void r(int i10) {
        ObjectAnimator objectAnimatorOfFloat;
        long j10 = 200;
        if (i10 == 1) {
            NotificationRowLayout notificationRowLayout = this.f14225c;
            objectAnimatorOfFloat = ObjectAnimator.ofFloat(notificationRowLayout, "translationY", -notificationRowLayout.getHeight(), 0.0f);
            this.f14241s = objectAnimatorOfFloat;
        } else {
            if (i10 != 2) {
                if (i10 == 3) {
                    ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.f14225c, AnimatedProperty.PROPERTY_NAME_ALPHA, 1.0f, 0.0f);
                    this.f14241s = objectAnimatorOfFloat2;
                    objectAnimatorOfFloat2.addListener(new e());
                    objectAnimatorOfFloat = this.f14241s;
                    j10 = 100;
                }
                this.f14241s.start();
            }
            NotificationRowLayout notificationRowLayout2 = this.f14225c;
            ObjectAnimator objectAnimatorOfFloat3 = ObjectAnimator.ofFloat(notificationRowLayout2, "translationY", 0.0f, -notificationRowLayout2.getHeight());
            this.f14241s = objectAnimatorOfFloat3;
            objectAnimatorOfFloat3.addListener(new d());
            objectAnimatorOfFloat = this.f14241s;
        }
        objectAnimatorOfFloat.setDuration(j10);
        this.f14241s.start();
    }

    public void h(boolean z10) {
        if (i()) {
            if (z10) {
                r(2);
            } else {
                m();
            }
            this.f14231i = false;
            r4.a.n("gb_show_window", false);
        }
    }

    protected WindowManager.LayoutParams j(ViewGroup.LayoutParams layoutParams) {
        WindowManager.LayoutParams layoutParams2 = new WindowManager.LayoutParams(layoutParams.width, layoutParams.height, 2017, 8389128, -2);
        layoutParams2.flags |= 16777216;
        layoutParams2.gravity = 49;
        layoutParams2.setTitle("FloatNotificationPanel");
        return layoutParams2;
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        CharSequence packageName = accessibilityEvent.getPackageName();
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        if (packageName.equals(AppConstants.Package.PACKAGE_NAME_QQ)) {
            if (accessibilityEvent.getEventType() != 64) {
                return;
            }
        } else if (!packageName.equals(AppConstants.Package.PACKAGE_NAME_MM) || accessibilityEvent.getEventType() != 64) {
            return;
        }
        q(accessibilityEvent);
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        if (!this.f14230h) {
            this.f14230h = true;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("miui.intent.action.gb_show_window");
            v.n(this, this.f14236n, intentFilter, "com.miui.securitycenter.permission.GB_SHOW_WINDOW", null, 4);
        }
        this.f14232j = (WindowManager) getSystemService("window");
        this.f14233k = new Handler(Looper.myLooper());
        this.f14242t = new h(this.f14233k, this);
        SettingsUtil.enableNotificationListener(this, NotificationListener.class);
        SettingsUtil.enableAccessibility(this, NotificationListener.class);
        v.b(this, new Intent(this, (Class<?>) NotificationListener.class), this.f14243u, 1, z1.A());
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
        if (this.f14230h) {
            this.f14230h = false;
            unregisterReceiver(this.f14236n);
        }
        FrameLayout frameLayout = this.f14224b;
        if (frameLayout != null) {
            this.f14232j.removeView(frameLayout);
        }
        if (this.f14231i) {
            s();
        }
        r4.a.n("gb_show_window", false);
        this.f14231i = false;
        try {
            this.f14234l.O2(this.f14242t);
        } catch (Exception e10) {
            Log.e("AntiMsgAccessibilityService", "mNoticationListenerBinder:" + e10);
        }
        SettingsUtil.closeNotificationListener(this, NotificationListener.class);
        SettingsUtil.closeAccessibility(this, NotificationListener.class);
        unbindService(this.f14243u);
        this.f14233k.removeCallbacksAndMessages(null);
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onInterrupt() {
    }

    public void s() {
        t(this.f14227e);
        t(this.f14226d);
        this.f14227e = null;
    }

    public void t(Intent intent) {
        if (intent != null) {
            try {
                Bundle bundle = ActivityOptions.makeCustomAnimation(this, R.anim.activity_open_enter, R.anim.activity_open_exit).toBundle();
                int i10 = this.f14229g;
                if (i10 != -1) {
                    bh.f.a(ContextWrapper.class, this, "startActivityAsUser", new Class[]{Intent.class, Bundle.class, UserHandle.class}, intent, bundle, z1.z(i10));
                } else {
                    intent.addFlags(268435456);
                    getApplication().startActivity(intent, bundle);
                }
            } catch (Exception e10) {
                e10.printStackTrace();
            }
        }
    }
}
