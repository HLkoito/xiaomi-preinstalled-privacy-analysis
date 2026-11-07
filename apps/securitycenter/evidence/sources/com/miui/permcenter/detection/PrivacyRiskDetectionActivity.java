package com.miui.permcenter.detection;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.z;
import com.miui.analytics.StatConstants;
import com.miui.common.base.BaseActivity;
import com.miui.common.customview.AutoPasteRecyclerView;
import com.miui.maml.folme.AnimatedProperty;
import com.miui.networkassistant.utils.DeviceUtil;
import com.miui.permcenter.detection.model.RiskAppInfoBean;
import com.miui.permcenter.privacyblur.PrivacyThumbnailBlurSettings;
import com.miui.permission.PermissionManager;
import com.miui.securitycenter.R;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import miui.os.Build;
import miuix.appcompat.app.AlertDialog;
import miuix.springback.view.SpringBackLayout;
import x4.a0;
import x4.t;
import x4.v;

/* JADX INFO: loaded from: classes2.dex */
public class PrivacyRiskDetectionActivity extends BaseActivity implements wb.b {
    private static final Handler N = new Handler();
    private HashMap<Long, Integer> A;
    private AlertDialog B;
    private z C;
    private m D;
    private View.OnClickListener E;
    private AutoPasteRecyclerView.c F;
    private final bc.a.InterfaceC0069a<Boolean> G;
    private final bc.a.InterfaceC0069a<List<RiskAppInfoBean>> H;
    private final bc.a.InterfaceC0069a<Boolean> I;
    private final bc.a.InterfaceC0069a<Boolean> J;
    private final bc.a.InterfaceC0069a<List<Integer>> K;
    private final uc.b.a L;
    private tb.b.d M;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private View f17757a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private RelativeLayout f17758b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private sb.c f17759c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private ImageView f17760d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    private ImageView f17761e;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    private TextView f17762f;

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    private Button f17766j;

    /* JADX INFO: renamed from: k, reason: collision with root package name */
    private View f17767k;

    /* JADX INFO: renamed from: m, reason: collision with root package name */
    private AnimatorSet f17769m;

    /* JADX INFO: renamed from: n, reason: collision with root package name */
    private int f17770n;

    /* JADX INFO: renamed from: o, reason: collision with root package name */
    private tb.b<com.miui.permcenter.detection.model.b> f17771o;

    /* JADX INFO: renamed from: p, reason: collision with root package name */
    private AutoPasteRecyclerView f17772p;

    /* JADX INFO: renamed from: q, reason: collision with root package name */
    private OffsetLinearLayoutManager f17773q;

    /* JADX INFO: renamed from: s, reason: collision with root package name */
    private ub.b f17775s;

    /* JADX INFO: renamed from: t, reason: collision with root package name */
    private ub.d f17776t;

    /* JADX INFO: renamed from: u, reason: collision with root package name */
    private ub.c f17777u;

    /* JADX INFO: renamed from: v, reason: collision with root package name */
    private ub.e f17778v;

    /* JADX INFO: renamed from: w, reason: collision with root package name */
    private uc.b f17779w;

    /* JADX INFO: renamed from: x, reason: collision with root package name */
    private ub.a f17780x;

    /* JADX INFO: renamed from: y, reason: collision with root package name */
    private d f17781y;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    private final List<ImageView> f17763g = new ArrayList(3);

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    private final List<View> f17764h = new ArrayList(3);

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    private final List<TextView> f17765i = new ArrayList(3);

    /* JADX INFO: renamed from: l, reason: collision with root package name */
    private int f17768l = 0;

    /* JADX INFO: renamed from: r, reason: collision with root package name */
    private SparseArray<com.miui.permcenter.detection.model.b> f17774r = new SparseArray<>();

    /* JADX INFO: renamed from: z, reason: collision with root package name */
    private List<View> f17782z = new ArrayList();

    class a implements DialogInterface.OnClickListener {
        a() {
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i10) {
            PrivacyRiskDetectionActivity.this.Z0(false);
        }
    }

    class b implements Runnable {
        b() {
        }

        @Override // java.lang.Runnable
        public void run() {
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity;
            Resources resources;
            int i10;
            if (a0.D()) {
                privacyRiskDetectionActivity = PrivacyRiskDetectionActivity.this;
                resources = privacyRiskDetectionActivity.getResources();
                i10 = R.dimen.dp_112;
            } else {
                privacyRiskDetectionActivity = PrivacyRiskDetectionActivity.this;
                resources = privacyRiskDetectionActivity.getResources();
                i10 = R.dimen.dp_189;
            }
            privacyRiskDetectionActivity.f17770n = resources.getDimensionPixelOffset(i10) + PrivacyRiskDetectionActivity.this.f17762f.getHeight();
            PrivacyRiskDetectionActivity.this.f17771o.s(PrivacyRiskDetectionActivity.this.f17770n);
        }
    }

    private static class c implements Runnable {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final WeakReference<PrivacyRiskDetectionActivity> f17785a;

        public c(PrivacyRiskDetectionActivity privacyRiskDetectionActivity) {
            this.f17785a = new WeakReference<>(privacyRiskDetectionActivity);
        }

        @Override // java.lang.Runnable
        public void run() {
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity = this.f17785a.get();
            if (privacyRiskDetectionActivity == null) {
                return;
            }
            privacyRiskDetectionActivity.O0();
            privacyRiskDetectionActivity.P0();
        }
    }

    class d extends BroadcastReceiver {
        d() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            PrivacyRiskDetectionActivity.this.T0(PrivacyRiskDetectionActivity.this.f17771o.m(22));
        }
    }

    private static class e implements uc.b.a {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final WeakReference<PrivacyRiskDetectionActivity> f17787a;

        private e(PrivacyRiskDetectionActivity privacyRiskDetectionActivity) {
            this.f17787a = new WeakReference<>(privacyRiskDetectionActivity);
        }

        /* synthetic */ e(PrivacyRiskDetectionActivity privacyRiskDetectionActivity, a aVar) {
            this(privacyRiskDetectionActivity);
        }

        @Override // uc.b.a
        public void a(HashMap<Long, Integer> map) {
            Integer num;
            Integer num2;
            Integer num3;
            Integer num4;
            Integer num5;
            Integer num6;
            Integer num7;
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity = this.f17787a.get();
            if (privacyRiskDetectionActivity == null || privacyRiskDetectionActivity.isFinishing() || privacyRiskDetectionActivity.isDestroyed()) {
                return;
            }
            privacyRiskDetectionActivity.A = map;
            if (map != null) {
                int i10 = 0;
                if (map.containsKey(32L) && (num7 = map.get(32L)) != null && num7.intValue() > 0) {
                    i10 = 1;
                }
                if (map.containsKey(8L) && (num6 = map.get(8L)) != null && num6.intValue() > 0) {
                    i10++;
                }
                if (map.containsKey(16L) && (num5 = map.get(16L)) != null && num5.intValue() > 0) {
                    i10++;
                }
                if (map.containsKey(131072L) && (num4 = map.get(131072L)) != null && num4.intValue() > 0) {
                    i10++;
                }
                if (map.containsKey(Long.valueOf(PermissionManager.PERM_ID_EXTERNAL_STORAGE)) && (num3 = map.get(Long.valueOf(PermissionManager.PERM_ID_EXTERNAL_STORAGE))) != null && num3.intValue() > 0) {
                    i10++;
                }
                if (map.containsKey(-3L) && (num2 = map.get(-3L)) != null && num2.intValue() > 0) {
                    i10++;
                }
                if (map.containsKey(-2L) && (num = map.get(-2L)) != null && num.intValue() > 0) {
                    i10++;
                }
                privacyRiskDetectionActivity.K0(i10);
            }
        }
    }

    private static class f implements bc.a.InterfaceC0069a<Boolean> {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final WeakReference<PrivacyRiskDetectionActivity> f17788a;

        private f(PrivacyRiskDetectionActivity privacyRiskDetectionActivity) {
            this.f17788a = new WeakReference<>(privacyRiskDetectionActivity);
        }

        /* synthetic */ f(PrivacyRiskDetectionActivity privacyRiskDetectionActivity, a aVar) {
            this(privacyRiskDetectionActivity);
        }

        @Override // bc.a.InterfaceC0069a
        /* JADX INFO: renamed from: b, reason: merged with bridge method [inline-methods] */
        public void a(Boolean bool) {
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity = this.f17788a.get();
            if (privacyRiskDetectionActivity == null || privacyRiskDetectionActivity.isFinishing() || privacyRiskDetectionActivity.isDestroyed()) {
                return;
            }
            ((View) privacyRiskDetectionActivity.f17764h.get(1)).setVisibility(8);
            if (bool.booleanValue()) {
                privacyRiskDetectionActivity.f17777u.c(true);
                privacyRiskDetectionActivity.f17774r.remove(22);
            } else {
                privacyRiskDetectionActivity.f17774r.put(22, com.miui.permcenter.detection.model.d.c(privacyRiskDetectionActivity));
            }
            ((ImageView) privacyRiskDetectionActivity.f17763g.get(1)).setImageResource(privacyRiskDetectionActivity.S0(16, 14, 22));
            ((TextView) privacyRiskDetectionActivity.f17765i.get(1)).setText(privacyRiskDetectionActivity.R0(16, 14, 22));
            privacyRiskDetectionActivity.f17759c.setState(privacyRiskDetectionActivity.f17774r.size());
            privacyRiskDetectionActivity.N0();
        }
    }

    private static class g implements bc.a.InterfaceC0069a<List<Integer>> {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final WeakReference<PrivacyRiskDetectionActivity> f17789a;

        private g(PrivacyRiskDetectionActivity privacyRiskDetectionActivity) {
            this.f17789a = new WeakReference<>(privacyRiskDetectionActivity);
        }

        /* synthetic */ g(PrivacyRiskDetectionActivity privacyRiskDetectionActivity, a aVar) {
            this(privacyRiskDetectionActivity);
        }

        @Override // bc.a.InterfaceC0069a
        /* JADX INFO: renamed from: b, reason: merged with bridge method [inline-methods] */
        public void a(List<Integer> list) {
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity = this.f17789a.get();
            if (privacyRiskDetectionActivity == null || privacyRiskDetectionActivity.isFinishing() || privacyRiskDetectionActivity.isDestroyed()) {
                return;
            }
            ((View) privacyRiskDetectionActivity.f17764h.get(0)).setVisibility(8);
            int i10 = 0;
            for (Integer num : list) {
                if (num != null && num.intValue() > 0) {
                    i10++;
                }
            }
            if (i10 > 0) {
                privacyRiskDetectionActivity.f17774r.put(12, com.miui.permcenter.detection.model.d.d(privacyRiskDetectionActivity, i10));
            } else {
                privacyRiskDetectionActivity.f17774r.remove(12);
            }
            ((ImageView) privacyRiskDetectionActivity.f17763g.get(0)).setImageResource(privacyRiskDetectionActivity.S0(11, 12));
            ((TextView) privacyRiskDetectionActivity.f17765i.get(0)).setText(privacyRiskDetectionActivity.R0(11, 12));
            privacyRiskDetectionActivity.f17759c.setState(privacyRiskDetectionActivity.f17774r.size());
            privacyRiskDetectionActivity.N0();
        }
    }

    private static class h implements bc.a.InterfaceC0069a<Boolean> {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final WeakReference<PrivacyRiskDetectionActivity> f17790a;

        private h(PrivacyRiskDetectionActivity privacyRiskDetectionActivity) {
            this.f17790a = new WeakReference<>(privacyRiskDetectionActivity);
        }

        /* synthetic */ h(PrivacyRiskDetectionActivity privacyRiskDetectionActivity, a aVar) {
            this(privacyRiskDetectionActivity);
        }

        @Override // bc.a.InterfaceC0069a
        /* JADX INFO: renamed from: b, reason: merged with bridge method [inline-methods] */
        public void a(Boolean bool) {
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity = this.f17790a.get();
            if (privacyRiskDetectionActivity == null || privacyRiskDetectionActivity.isFinishing() || privacyRiskDetectionActivity.isDestroyed()) {
                return;
            }
            if (bool.booleanValue()) {
                privacyRiskDetectionActivity.f17774r.put(14, com.miui.permcenter.detection.model.d.f(privacyRiskDetectionActivity, 1));
                privacyRiskDetectionActivity.f17777u.c(true);
            } else {
                privacyRiskDetectionActivity.f17774r.remove(14);
            }
            privacyRiskDetectionActivity.f17759c.setState(privacyRiskDetectionActivity.f17774r.size());
            ((TextView) privacyRiskDetectionActivity.f17765i.get(1)).setText(privacyRiskDetectionActivity.R0(16, 14));
            if (!Build.IS_INTERNATIONAL_BUILD) {
                privacyRiskDetectionActivity.f17780x = new ub.a(privacyRiskDetectionActivity.J);
                privacyRiskDetectionActivity.f17780x.execute(new Void[0]);
            } else {
                ((View) privacyRiskDetectionActivity.f17764h.get(1)).setVisibility(8);
                ((ImageView) privacyRiskDetectionActivity.f17763g.get(1)).setImageResource(privacyRiskDetectionActivity.S0(16, 14));
                privacyRiskDetectionActivity.N0();
            }
        }
    }

    private static class i implements tb.b.d {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final WeakReference<PrivacyRiskDetectionActivity> f17791a;

        private i(PrivacyRiskDetectionActivity privacyRiskDetectionActivity) {
            this.f17791a = new WeakReference<>(privacyRiskDetectionActivity);
        }

        /* synthetic */ i(PrivacyRiskDetectionActivity privacyRiskDetectionActivity, a aVar) {
            this(privacyRiskDetectionActivity);
        }

        @Override // tb.b.d
        public void a(int i10, int i11, int i12) {
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity = this.f17791a.get();
            if (privacyRiskDetectionActivity == null || privacyRiskDetectionActivity.isFinishing() || privacyRiskDetectionActivity.isDestroyed()) {
                return;
            }
            if (i10 == 21) {
                if (((com.miui.permcenter.detection.model.b) privacyRiskDetectionActivity.f17771o.l().get(i11)).a() == 21) {
                    com.miui.permcenter.detection.model.c cVar = (com.miui.permcenter.detection.model.c) privacyRiskDetectionActivity.f17771o.l().get(i11);
                    cVar.b(privacyRiskDetectionActivity);
                    if ("miui.intent.action.OP_AUTO_START".equals(cVar.f17841f)) {
                        sb.a.a();
                        return;
                    } else if ("miui.intent.action.GARBAGE_CLEANUP".equals(cVar.f17841f)) {
                        sb.a.b();
                        return;
                    } else {
                        if ("miui.intent.action.GARBAGE_UNINSTALL_APPS".equals(cVar.f17841f)) {
                            sb.a.p();
                            return;
                        }
                        return;
                    }
                }
                return;
            }
            if (i10 != 22) {
                switch (i10) {
                    case 11:
                        if (i12 != 10) {
                            Intent intent = new Intent(privacyRiskDetectionActivity, (Class<?>) PrivacyPermissionsSetActivity.class);
                            intent.putExtra("privacyType", 10);
                            if (privacyRiskDetectionActivity.A != null) {
                                intent.putExtra("privacyData", privacyRiskDetectionActivity.A);
                            }
                            privacyRiskDetectionActivity.startActivityForResult(intent, 100);
                            sb.a.n();
                            return;
                        }
                        sb.a.h();
                        break;
                        break;
                    case 12:
                        if (i12 != 10) {
                            Intent intent2 = new Intent(privacyRiskDetectionActivity, (Class<?>) PrivacyPermissionsSetActivity.class);
                            intent2.putExtra("privacyType", 11);
                            privacyRiskDetectionActivity.startActivityForResult(intent2, 101);
                            sb.a.j();
                            return;
                        }
                        sb.a.d();
                        break;
                        break;
                    case 13:
                        if (i12 != 10) {
                            if (privacyRiskDetectionActivity.f17771o.l().get(i11) instanceof com.miui.permcenter.detection.model.a) {
                                com.miui.permcenter.detection.model.a aVar = (com.miui.permcenter.detection.model.a) privacyRiskDetectionActivity.f17771o.l().get(i11);
                                if (aVar.b() instanceof ArrayList) {
                                    RiskAppDeleteActivity.i0(privacyRiskDetectionActivity, (ArrayList) aVar.b(), 102);
                                    sb.a.m();
                                    return;
                                }
                                return;
                            }
                            return;
                        }
                        sb.a.g();
                        break;
                        break;
                    case 14:
                        if (i12 != 10) {
                            if (((com.miui.permcenter.detection.model.b) privacyRiskDetectionActivity.f17771o.l().get(i11)).a() == 14) {
                                PrivacyThumbnailBlurSettings.v0(privacyRiskDetectionActivity, 103);
                                sb.a.o();
                                return;
                            }
                            return;
                        }
                        sb.a.i();
                        break;
                        break;
                    case 15:
                        if (i12 != 10) {
                            sb.a.k();
                            return;
                        }
                        sb.a.e();
                        break;
                    case 16:
                        if (i12 != 10) {
                            jg.a.e(privacyRiskDetectionActivity, true);
                            sb.a.l();
                        } else {
                            sb.a.f();
                        }
                        break;
                    default:
                        return;
                }
            } else if (i12 != 10) {
                ((InputMethodManager) privacyRiskDetectionActivity.getSystemService(StatConstants.Channel.INPUT_METHOD)).showInputMethodPicker();
                return;
            }
            privacyRiskDetectionActivity.T0(i11);
        }
    }

    private static class j implements View.OnClickListener {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final WeakReference<PrivacyRiskDetectionActivity> f17792a;

        private j(PrivacyRiskDetectionActivity privacyRiskDetectionActivity) {
            this.f17792a = new WeakReference<>(privacyRiskDetectionActivity);
        }

        /* synthetic */ j(PrivacyRiskDetectionActivity privacyRiskDetectionActivity, a aVar) {
            this(privacyRiskDetectionActivity);
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity = this.f17792a.get();
            if (privacyRiskDetectionActivity == null) {
                return;
            }
            if (view.getId() != R.id.btn_stop) {
                if (view.getId() == R.id.view_continue) {
                    privacyRiskDetectionActivity.W0();
                }
            } else if (privacyRiskDetectionActivity.B == null || !(privacyRiskDetectionActivity.B.isShowing() || privacyRiskDetectionActivity.f17768l == 2)) {
                privacyRiskDetectionActivity.X0();
                sb.a.c();
            }
        }
    }

    private static class k implements bc.a.InterfaceC0069a<Boolean> {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final WeakReference<PrivacyRiskDetectionActivity> f17793a;

        private k(PrivacyRiskDetectionActivity privacyRiskDetectionActivity) {
            this.f17793a = new WeakReference<>(privacyRiskDetectionActivity);
        }

        /* synthetic */ k(PrivacyRiskDetectionActivity privacyRiskDetectionActivity, a aVar) {
            this(privacyRiskDetectionActivity);
        }

        @Override // bc.a.InterfaceC0069a
        /* JADX INFO: renamed from: b, reason: merged with bridge method [inline-methods] */
        public void a(Boolean bool) {
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity = this.f17793a.get();
            if (privacyRiskDetectionActivity == null || privacyRiskDetectionActivity.isFinishing() || privacyRiskDetectionActivity.isDestroyed()) {
                return;
            }
            if (bool.booleanValue() || !vf.n.a(privacyRiskDetectionActivity)) {
                privacyRiskDetectionActivity.f17774r.remove(16);
            } else {
                privacyRiskDetectionActivity.f17774r.put(16, com.miui.permcenter.detection.model.d.g(privacyRiskDetectionActivity));
                privacyRiskDetectionActivity.f17777u.c(true);
            }
            ((TextView) privacyRiskDetectionActivity.f17765i.get(1)).setText(privacyRiskDetectionActivity.R0(16));
            privacyRiskDetectionActivity.f17759c.setState(privacyRiskDetectionActivity.f17774r.size());
            privacyRiskDetectionActivity.f17778v = new ub.e(privacyRiskDetectionActivity, privacyRiskDetectionActivity.G);
            privacyRiskDetectionActivity.f17778v.execute(new Void[0]);
        }
    }

    private static class l implements bc.a.InterfaceC0069a<List<RiskAppInfoBean>> {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final WeakReference<PrivacyRiskDetectionActivity> f17794a;

        private l(PrivacyRiskDetectionActivity privacyRiskDetectionActivity) {
            this.f17794a = new WeakReference<>(privacyRiskDetectionActivity);
        }

        /* synthetic */ l(PrivacyRiskDetectionActivity privacyRiskDetectionActivity, a aVar) {
            this(privacyRiskDetectionActivity);
        }

        @Override // bc.a.InterfaceC0069a
        /* JADX INFO: renamed from: b, reason: merged with bridge method [inline-methods] */
        public void a(List<RiskAppInfoBean> list) {
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity = this.f17794a.get();
            if (privacyRiskDetectionActivity == null || privacyRiskDetectionActivity.isFinishing() || privacyRiskDetectionActivity.isDestroyed()) {
                return;
            }
            ((View) privacyRiskDetectionActivity.f17764h.get(2)).setVisibility(8);
            if (list == null || list.size() <= 0) {
                privacyRiskDetectionActivity.f17774r.remove(13);
            } else {
                privacyRiskDetectionActivity.f17774r.put(13, com.miui.permcenter.detection.model.d.b(privacyRiskDetectionActivity, list.size(), list));
            }
            ((ImageView) privacyRiskDetectionActivity.f17763g.get(2)).setImageResource(privacyRiskDetectionActivity.S0(13));
            ((TextView) privacyRiskDetectionActivity.f17765i.get(2)).setText(privacyRiskDetectionActivity.R0(13));
            privacyRiskDetectionActivity.f17759c.setState(privacyRiskDetectionActivity.f17774r.size());
            privacyRiskDetectionActivity.N0();
        }
    }

    private static class m implements Runnable {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final WeakReference<PrivacyRiskDetectionActivity> f17795a;

        private m(PrivacyRiskDetectionActivity privacyRiskDetectionActivity) {
            this.f17795a = new WeakReference<>(privacyRiskDetectionActivity);
        }

        /* synthetic */ m(PrivacyRiskDetectionActivity privacyRiskDetectionActivity, a aVar) {
            this(privacyRiskDetectionActivity);
        }

        @Override // java.lang.Runnable
        public void run() {
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity = this.f17795a.get();
            if (privacyRiskDetectionActivity == null) {
                return;
            }
            int height = (privacyRiskDetectionActivity.f17772p.getHeight() - privacyRiskDetectionActivity.f17772p.computeVerticalScrollRange()) + privacyRiskDetectionActivity.f17770n;
            Log.d("Privacy", "diffHeight = " + height);
            if (height > 0) {
                privacyRiskDetectionActivity.f17772p.setPadding(0, privacyRiskDetectionActivity.f17772p.getPaddingTop(), 0, height);
            } else {
                privacyRiskDetectionActivity.f17772p.setPadding(0, privacyRiskDetectionActivity.f17772p.getPaddingTop(), 0, 0);
            }
            privacyRiskDetectionActivity.V0();
        }
    }

    private static class n implements AutoPasteRecyclerView.c {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final WeakReference<PrivacyRiskDetectionActivity> f17796a;

        private n(PrivacyRiskDetectionActivity privacyRiskDetectionActivity) {
            this.f17796a = new WeakReference<>(privacyRiskDetectionActivity);
        }

        /* synthetic */ n(PrivacyRiskDetectionActivity privacyRiskDetectionActivity, a aVar) {
            this(privacyRiskDetectionActivity);
        }

        @Override // com.miui.common.customview.AutoPasteRecyclerView.c
        public void a(float f10) {
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity = this.f17796a.get();
            if (privacyRiskDetectionActivity == null) {
                return;
            }
            privacyRiskDetectionActivity.f17758b.setAlpha(1.0f - f10);
        }
    }

    private static class o extends gm.a {

        /* JADX INFO: renamed from: u, reason: collision with root package name */
        private final WeakReference<PrivacyRiskDetectionActivity> f17797u;

        private o(PrivacyRiskDetectionActivity privacyRiskDetectionActivity) {
            this.f17797u = new WeakReference<>(privacyRiskDetectionActivity);
        }

        /* synthetic */ o(PrivacyRiskDetectionActivity privacyRiskDetectionActivity, a aVar) {
            this(privacyRiskDetectionActivity);
        }

        @Override // androidx.recyclerview.widget.z
        public void T(RecyclerView.c0 c0Var) {
            super.T(c0Var);
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity = this.f17797u.get();
            if (privacyRiskDetectionActivity == null) {
                return;
            }
            Log.d("Privacy", "height = " + privacyRiskDetectionActivity.f17772p.getHeight() + ",range = " + privacyRiskDetectionActivity.f17772p.computeVerticalScrollRange());
            privacyRiskDetectionActivity.f17772p.post(privacyRiskDetectionActivity.D);
        }
    }

    private static class p implements lb.a.c {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private WeakReference<PrivacyRiskDetectionActivity> f17798a;

        /* JADX INFO: renamed from: b, reason: collision with root package name */
        private float f17799b;

        public p(PrivacyRiskDetectionActivity privacyRiskDetectionActivity, float f10) {
            this.f17798a = new WeakReference<>(privacyRiskDetectionActivity);
            this.f17799b = f10;
        }

        @Override // lb.a.c
        public void a(float f10) {
            PrivacyRiskDetectionActivity privacyRiskDetectionActivity = this.f17798a.get();
            if (privacyRiskDetectionActivity == null || privacyRiskDetectionActivity.isFinishing() || privacyRiskDetectionActivity.isDestroyed()) {
                return;
            }
            privacyRiskDetectionActivity.f17759c.setMainViewScaleX(f10);
            privacyRiskDetectionActivity.f17759c.setMainViewScaleY(f10);
            float f11 = f10 <= 0.58f ? 1.0f : f10 * this.f17799b;
            if (privacyRiskDetectionActivity.f17760d != null) {
                privacyRiskDetectionActivity.f17760d.setScaleX(f11);
                privacyRiskDetectionActivity.f17760d.setScaleY(f11);
            }
            privacyRiskDetectionActivity.f17761e.setScaleX(f11);
            privacyRiskDetectionActivity.f17761e.setScaleY(f11);
        }
    }

    public PrivacyRiskDetectionActivity() {
        a aVar = null;
        this.G = new h(this, aVar);
        this.H = new l(this, aVar);
        this.I = new k(this, aVar);
        this.J = new f(this, aVar);
        this.K = new g(this, aVar);
        this.L = new e(this, aVar);
        this.M = new i(this, aVar);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void K0(int i10) {
        if (i10 > 0) {
            this.f17774r.put(11, com.miui.permcenter.detection.model.d.h(this, i10));
        }
        this.f17765i.get(0).setText(R0(11));
        this.f17759c.setState(this.f17774r.size());
        ub.b bVar = new ub.b(this, this.K);
        this.f17775s = bVar;
        bVar.execute(new Void[0]);
    }

    private void L0() {
        if (DeviceUtil.isLargeScaleMode()) {
            SpringBackLayout springBackLayout = (SpringBackLayout) findViewById(R.id.spring_backlayout);
            ViewGroup.LayoutParams layoutParams = springBackLayout.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                marginLayoutParams.setMargins(marginLayoutParams.leftMargin, getResources().getDimensionPixelSize(R.dimen.view_dimen_320), marginLayoutParams.rightMargin, marginLayoutParams.bottomMargin);
                springBackLayout.setLayoutParams(marginLayoutParams);
            }
        }
    }

    private void M0() {
        List<View> list = this.f17782z;
        if (list == null || list.size() == 0) {
            return;
        }
        if (t.r() || t.E()) {
            int dimension = (int) getResources().getDimension(R.dimen.privacy_detection_card_margin);
            for (View view : this.f17782z) {
                if (view != null) {
                    view.setPadding(dimension, view.getPaddingTop(), dimension, view.getPaddingBottom());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void N0() {
        for (int i10 = 1; i10 < this.f17764h.size(); i10++) {
            if (this.f17764h.get(i10).getVisibility() == 0) {
                return;
            }
        }
        ArrayList arrayList = new ArrayList();
        arrayList.add(this.f17774r.get(14, null));
        arrayList.add(this.f17774r.get(16, null));
        arrayList.add(com.miui.permcenter.detection.model.d.e(this, this.f17774r.get(18), this.f17774r.get(17)));
        arrayList.add(this.f17774r.get(15, null));
        arrayList.add(this.f17774r.get(13, null));
        arrayList.add(this.f17774r.get(11, null));
        arrayList.add(this.f17774r.get(12, null));
        arrayList.add(this.f17774r.get(22, null));
        arrayList.removeAll(Collections.singleton(null));
        arrayList.add(com.miui.permcenter.detection.model.c.c(this));
        arrayList.add(com.miui.permcenter.detection.model.c.e(this));
        arrayList.add(com.miui.permcenter.detection.model.c.d(this));
        arrayList.add(0, new com.miui.permcenter.detection.model.e());
        this.f17771o.r(arrayList);
        Z0(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void O0() {
        this.f17759c.stop();
        PathInterpolator pathInterpolator = new PathInterpolator(0.6f, 0.35f, 0.19f, 1.0f);
        ObjectAnimator objectAnimatorB = this.f17759c.b(1.0f, 0.0f);
        objectAnimatorB.setDuration(400L);
        objectAnimatorB.setInterpolator(pathInterpolator);
        this.f17758b.setVisibility(0);
        this.f17766j.setVisibility(8);
        this.f17762f.setAlpha(0.0f);
        this.f17758b.setAlpha(0.0f);
        ImageView imageView = this.f17760d;
        if (imageView != null) {
            imageView.setScaleX(1.71f);
            this.f17760d.setScaleY(1.71f);
        }
        this.f17761e.setScaleX(1.71f);
        this.f17761e.setScaleY(1.71f);
        lb.a.h();
        lb.a.g(1.0f, 0.58f);
        lb.a.c(new p(this, 1.71f));
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.f17758b, AnimatedProperty.PROPERTY_NAME_ALPHA, 0.0f, 1.0f);
        objectAnimatorOfFloat.setDuration(400L);
        objectAnimatorOfFloat.setInterpolator(pathInterpolator);
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.f17762f, AnimatedProperty.PROPERTY_NAME_ALPHA, 0.0f, 1.0f);
        objectAnimatorOfFloat2.setDuration(300L);
        objectAnimatorOfFloat2.setStartDelay(300L);
        objectAnimatorOfFloat2.setInterpolator(pathInterpolator);
        AnimatorSet animatorSet = new AnimatorSet();
        this.f17769m = animatorSet;
        animatorSet.playTogether(objectAnimatorB, objectAnimatorOfFloat, objectAnimatorOfFloat2);
        this.f17769m.start();
    }

    private int Q0(int i10) {
        int i11;
        switch (i10) {
            case 100:
                i11 = 11;
                break;
            case 101:
                i11 = 12;
                break;
            case 102:
                i11 = 13;
                break;
            case 103:
                i11 = 14;
                break;
            case 104:
                i11 = 15;
                break;
            default:
                i11 = -1;
                break;
        }
        return this.f17771o.m(i11);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String R0(int... iArr) {
        int i10 = 0;
        if (iArr != null) {
            int length = iArr.length;
            int i11 = 0;
            while (i10 < length) {
                if (this.f17774r.get(iArr[i10], null) != null) {
                    i11++;
                }
                i10++;
            }
            i10 = i11;
        }
        return i10 == 0 ? getString(R.string.privacy_risk_result_item_safe) : sb.b.a(this, R.plurals.privacy_risk_result_item_error, i10);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int S0(int... iArr) {
        if (iArr == null) {
            return R.drawable.scan_state_safe;
        }
        for (int i10 : iArr) {
            if (this.f17774r.get(i10, null) != null) {
                return R.drawable.scan_state_risky;
            }
        }
        return R.drawable.scan_state_safe;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void T0(int i10) {
        Resources resources;
        int i11;
        if (i10 == -1) {
            return;
        }
        this.f17771o.q(i10);
        if (this.f17771o.n() != 0) {
            sb.b.c(this.f17762f, R.plurals.privacy_risk_result_msg, this.f17771o.n());
            return;
        }
        b1();
        if (((int) this.f17762f.getPaint().measureText(this.f17762f.getText().toString())) + this.f17762f.getPaddingStart() + this.f17762f.getPaddingEnd() > sb.b.b(this)) {
            if (a0.D()) {
                resources = getResources();
                i11 = R.dimen.dp_162;
            } else {
                resources = getResources();
                i11 = R.dimen.dp_231;
            }
            this.f17770n = resources.getDimensionPixelOffset(i11) + this.f17762f.getHeight();
            this.f17771o.s(this.f17770n);
        }
    }

    private void U0() {
        this.f17781y = new d();
        v.m(this, this.f17781y, new IntentFilter("action_mi_ime_opened"), 4);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void V0() {
        AutoPasteRecyclerView autoPasteRecyclerView;
        int i10 = 0;
        View childAt = this.f17772p.getChildAt(0);
        if (childAt == null) {
            return;
        }
        int bottom = this.f17770n - childAt.getBottom();
        Log.d("Privacy", "resetResult mScrollY = " + bottom);
        int i11 = this.f17770n;
        if (bottom > i11) {
            this.f17758b.setAlpha(0.0f);
            return;
        }
        this.f17758b.setAlpha(1.0f - (bottom / i11));
        if (bottom < this.f17770n / 2) {
            autoPasteRecyclerView = this.f17772p;
        } else {
            autoPasteRecyclerView = this.f17772p;
            i10 = 1;
        }
        autoPasteRecyclerView.smoothScrollToPosition(i10);
    }

    private void Y0() {
        this.f17768l = 0;
        this.f17771o.r(null);
        this.f17774r.clear();
        uc.b bVar = new uc.b();
        this.f17779w = bVar;
        bVar.d(this.L);
        this.f17779w.execute(new String[0]);
        ub.c cVar = new ub.c(this.I);
        this.f17777u = cVar;
        cVar.execute(new Void[0]);
        ub.d dVar = new ub.d(this, this.H);
        this.f17776t = dVar;
        dVar.execute(new Void[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void Z0(boolean z10) {
        if (this.f17768l == 0) {
            this.f17768l = z10 ? 1 : 2;
            this.f17767k.setVisibility(z10 ? 8 : 0);
            a1();
            ub.e eVar = this.f17778v;
            if (eVar != null) {
                eVar.cancel(true);
            }
            ub.b bVar = this.f17775s;
            if (bVar != null) {
                bVar.cancel(true);
            }
            ub.c cVar = this.f17777u;
            if (cVar != null) {
                cVar.cancel(true);
            }
            ub.d dVar = this.f17776t;
            if (dVar != null) {
                dVar.cancel(true);
            }
            uc.b bVar2 = this.f17779w;
            if (bVar2 != null) {
                bVar2.cancel(true);
                this.f17779w.c();
            }
        }
    }

    private void a1() {
        b1();
        N.postDelayed(new c(this), 600L);
    }

    private void b1() {
        if (isTabletSpitModel()) {
            this.f17762f.setSingleLine(true);
            this.f17762f.setEllipsize(TextUtils.TruncateAt.END);
            setHorizontalPadding(this.f17762f);
        }
        this.f17762f.setTextColor(ContextCompat.c(this, this.f17771o.n() > 0 ? R.color.first_aid_box_summary_textcolor2 : R.color.first_aid_box_summary_textcolor));
        if (this.f17768l != 2) {
            if (this.f17771o.n() > 0) {
                this.f17761e.setImageResource(R.drawable.firstaid_result_icon_have_risk);
                sb.b.c(this.f17762f, R.plurals.privacy_risk_result_msg, this.f17771o.n());
                sb.a.t(2);
                return;
            } else {
                this.f17762f.setText(this.f17774r.size() > 0 ? R.string.privacy_risk_result_success_msg : R.string.privacy_risk_result_success_empty_msg);
                this.f17761e.setImageResource(R.drawable.firstaid_result_icon_compelete);
                sb.a.t(this.f17774r.size() > 0 ? 3 : 1);
                return;
            }
        }
        sb.a.t(4);
        this.f17762f.setText(R.string.privacy_risk_result_cancel_msg);
        this.f17761e.setImageResource(R.drawable.firstaid_result_icon_not_complete);
        ArrayList arrayList = new ArrayList();
        arrayList.add(com.miui.permcenter.detection.model.c.c(this));
        arrayList.add(com.miui.permcenter.detection.model.c.e(this));
        arrayList.add(com.miui.permcenter.detection.model.c.d(this));
        arrayList.add(0, new com.miui.permcenter.detection.model.e());
        this.f17771o.r(arrayList);
    }

    private void initView() {
        ViewGroup.MarginLayoutParams marginLayoutParams;
        Resources resources;
        int i10;
        this.f17757a = findViewById(R.id.progressLayout);
        ViewStub viewStub = (ViewStub) findViewById(R.id.privacy_risk_detection_result_stub);
        ViewStub viewStub2 = (ViewStub) findViewById(R.id.privacy_risk_detection_anim_stub);
        this.f17767k = findViewById(R.id.view_continue);
        if (a0.D()) {
            this.f17770n = getResources().getDimensionPixelOffset(R.dimen.privacy_risk_detection_result_lite_top);
            viewStub2.setLayoutResource(R.layout.v_privacy_risk_detection_anim_lite_layout);
            viewStub.setLayoutResource(R.layout.v_privicy_risk_detection_result_lite_layout);
            marginLayoutParams = (ViewGroup.MarginLayoutParams) this.f17767k.getLayoutParams();
            resources = getResources();
            i10 = R.dimen.privacy_risk_detection_result_lite_summary_margin_top;
        } else {
            this.f17770n = getResources().getDimensionPixelOffset(R.dimen.privacy_risk_detection_result_top);
            viewStub2.setLayoutResource(R.layout.v_privacy_risk_detection_anim_layout);
            viewStub.setLayoutResource(R.layout.v_privicy_risk_detection_result_layout);
            marginLayoutParams = (ViewGroup.MarginLayoutParams) this.f17767k.getLayoutParams();
            resources = getResources();
            i10 = R.dimen.view_dimen_736;
        }
        marginLayoutParams.topMargin = resources.getDimensionPixelSize(i10);
        this.f17767k.setLayoutParams(marginLayoutParams);
        viewStub.inflate();
        this.f17759c = (sb.c) viewStub2.inflate().findViewById(R.id.ll_top_main);
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.ll_top_result);
        this.f17758b = relativeLayout;
        relativeLayout.setVisibility(8);
        this.f17760d = (ImageView) findViewById(R.id.iv_circle);
        this.f17761e = (ImageView) findViewById(R.id.result_score_icon);
        this.f17762f = (TextView) findViewById(R.id.tv_summary_result);
        this.f17764h.add(0, findViewById(R.id.permissions_status_bar));
        this.f17764h.add(1, findViewById(R.id.privacy_status_bar));
        this.f17764h.add(2, findViewById(R.id.app_status_bar));
        this.f17763g.add(0, (ImageView) findViewById(R.id.permissions_status_iv));
        this.f17763g.add(1, (ImageView) findViewById(R.id.privacy_status_iv));
        this.f17763g.add(2, (ImageView) findViewById(R.id.app_status_iv));
        this.f17765i.add(0, (TextView) findViewById(R.id.permissions_msg_tv));
        this.f17765i.add(1, (TextView) findViewById(R.id.privacy_msg_tv));
        this.f17765i.add(2, (TextView) findViewById(R.id.app_msg_tv));
        Button button = (Button) findViewById(R.id.btn_stop);
        this.f17766j = button;
        setViewHorizontalMargin(button);
        setViewHorizontalMargin(findViewById(R.id.risk_scan_content));
        this.f17772p = (AutoPasteRecyclerView) findViewById(R.id.risk_recycler);
        if (t.F(this)) {
            L0();
        }
        a aVar = null;
        this.C = new o(this, aVar);
        this.D = new m(this, aVar);
        this.E = new j(this, aVar);
        this.F = new n(this, aVar);
        OffsetLinearLayoutManager offsetLinearLayoutManager = new OffsetLinearLayoutManager(this);
        this.f17773q = offsetLinearLayoutManager;
        this.f17772p.setLayoutManager(offsetLinearLayoutManager);
        tb.b<com.miui.permcenter.detection.model.b> bVar = new tb.b<>(this.M);
        this.f17771o = bVar;
        this.f17772p.setAdapter(bVar);
        this.f17772p.setOnScrollPercentChangeListener(this.F);
        this.f17772p.setItemAnimator(this.C);
        this.f17771o.k(this);
        this.f17772p.setAlignItemIndex(0);
        this.f17766j.setOnClickListener(this.E);
        this.f17767k.setOnClickListener(this.E);
        vc.b.b(this.f17760d, R.drawable.result_top_icon);
    }

    public void P0() {
        eg.t.k(getApplicationContext(), this.f17757a, this.f17772p);
        if (((int) this.f17762f.getPaint().measureText(this.f17762f.getText().toString())) + this.f17762f.getPaddingStart() + this.f17762f.getPaddingEnd() > sb.b.b(this)) {
            this.f17762f.post(new b());
        }
        this.f17772p.post(this.D);
    }

    public void W0() {
        this.f17758b.setVisibility(8);
        this.f17759c.a();
        this.f17766j.setVisibility(0);
        this.f17757a.setVisibility(0);
        this.f17757a.setAlpha(1.0f);
        this.f17772p.setVisibility(8);
        Y0();
    }

    public void X0() {
        this.B = new AlertDialog.Builder(this).setTitle(R.string.privacy_risk_detection_stop_title).setMessage(R.string.privacy_risk_detection_stop_msg).setPositiveButton(R.string.privacy_risk_detection_stop_btn, new a()).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity
    protected void onActivityResult(int i10, int i11, Intent intent) {
        super.onActivityResult(i10, i11, intent);
        T0(Q0(i10));
    }

    @Override // com.miui.common.base.BaseActivity, miuix.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (t.s(getApplicationContext())) {
            L0();
        }
        M0();
        if (isTabletSpitModel()) {
            this.f17771o.notifyDataSetChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.miui.common.base.BaseActivity, miuix.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setNeedHorizontalPadding(false);
        setContentView(R.layout.activity_privacy_risk_detection);
        pg.i.b(this);
        if (!Build.IS_INTERNATIONAL_BUILD) {
            U0();
        }
        initView();
        Y0();
        sb.a.s();
    }

    @Override // com.miui.common.base.BaseActivity, miuix.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        this.f17759c.release();
        N.removeCallbacksAndMessages(null);
        super.onDestroy();
        lb.a.f();
        lb.a.e();
        AnimatorSet animatorSet = this.f17769m;
        if (animatorSet != null && animatorSet.isRunning()) {
            this.f17769m.cancel();
        }
        if (!Build.IS_INTERNATIONAL_BUILD) {
            unregisterReceiver(this.f17781y);
        }
        ub.e eVar = this.f17778v;
        if (eVar != null) {
            eVar.cancel(true);
        }
        ub.b bVar = this.f17775s;
        if (bVar != null) {
            bVar.cancel(true);
        }
        ub.c cVar = this.f17777u;
        if (cVar != null) {
            cVar.cancel(true);
        }
        ub.d dVar = this.f17776t;
        if (dVar != null) {
            dVar.cancel(true);
        }
        uc.b bVar2 = this.f17779w;
        if (bVar2 != null) {
            bVar2.cancel(true);
            this.f17779w.c();
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        mb.b.h("1127.20.6.1.28285");
    }

    @Override // wb.b
    public void setHorizontalPadding(View view) {
        if (!this.f17782z.contains(view)) {
            this.f17782z.add(view);
        }
        if (isTabletSpitModel()) {
            int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.pad_common_split_margin_start);
            view.setPadding(dimensionPixelSize, view.getPaddingTop(), dimensionPixelSize, view.getPaddingBottom());
        }
    }
}
