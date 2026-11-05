package com.miui.guardprovider.engine.mi;

import android.content.Context;
import android.util.Log;
import com.miui.guardprovider.engine.mi.MiEngineNG;
import com.xiaomi.onetrack.b.e;
import f0.a;
import g1.p;
import g1.w;
import g1.z;
import h1.r;
import h1.y;
import i0.m;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.IntConsumer;
import java.util.function.ObjIntConsumer;
import kotlin.Metadata;
import miui.os.Build;
import p0.n;
import r4.h;
import t0.k;
import t0.o;
import v1.q;
import w1.l;
import y.f;

/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 \u00162\u00020\u0001:\u0001\u0014B\u0011\b\u0007\u0012\u0006\u0010$\u001a\u00020#¢\u0006\u0004\b%\u0010&J\u0010\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0003\u001a\u00020\u0002H\u0002J\u0010\u0010\u0006\u001a\u00020\u00042\u0006\u0010\u0003\u001a\u00020\u0002H\u0002J\u0019\u0010\t\u001a\b\u0012\u0004\u0012\u00020\b0\u0007*\u00020\u0002H\u0002¢\u0006\u0004\b\t\u0010\nJ2\u0010\u0013\u001a\u00020\u00042\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\f0\u000b2\b\u0010\u000f\u001a\u0004\u0018\u00010\u000e2\u0006\u0010\u0011\u001a\u00020\u00102\b\u0010\u0012\u001a\u0004\u0018\u00010\fH\u0016J\b\u0010\u0014\u001a\u00020\u0004H\u0016J\u0012\u0010\u0016\u001a\u00020\u00042\b\u0010\u000f\u001a\u0004\u0018\u00010\u0015H\u0016J\b\u0010\u0017\u001a\u00020\u0010H\u0016J\b\u0010\u0018\u001a\u00020\u0004H\u0016J\u0012\u0010\u001a\u001a\u00020\u00042\b\u0010\u000f\u001a\u0004\u0018\u00010\u0019H\u0016J\b\u0010\u001b\u001a\u00020\u0004H\u0016J\u0010\u0010\u001d\u001a\u00020\u00042\u0006\u0010\u001c\u001a\u00020\u0010H\u0016J\b\u0010\u001e\u001a\u00020\u0010H\u0016R\u0014\u0010\"\u001a\u00020\u001f8\u0002X\u0082\u0004¢\u0006\u0006\n\u0004\b \u0010!¨\u0006'"}, d2 = {"Lcom/miui/guardprovider/engine/mi/MiEngineNG;", "Lf0/a;", "Lt0/a;", "app", "Lg1/z;", "y", "s", "", "Ly/f;", "x", "(Lt0/a;)[Ly/f;", "", "", "paths", "Lf0/a$c;", "listener", "", "useCloudScan", "extras", "j", "a", "Lf0/a$a;", "e", "f", "h", "Lf0/a$b;", e.f2234a, "k", "enable", "w", "g", "Lp0/n;", "d", "Lp0/n;", "delegate", "Lf0/b;", "engineDesc", "<init>", "(Lf0/b;)V", "GuardProvider_cnRelease"}, k = 1, mv = {1, 9, 0})
public final class MiEngineNG extends a {

    /* JADX INFO: renamed from: d, reason: collision with root package name and from kotlin metadata */
    private final n delegate;

    static final class b extends w1.n implements q {

        /* JADX INFO: renamed from: c, reason: collision with root package name */
        final /* synthetic */ a.c f1763c;

        /* JADX INFO: renamed from: d, reason: collision with root package name */
        final /* synthetic */ List f1764d;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        b(a.c cVar, List list) {
            super(3);
            this.f1763c = cVar;
            this.f1764d = list;
        }

        public final void a(int i6, Object obj, Exception exc) {
            l.f(obj, com.xiaomi.onetrack.api.b.G);
            l.f(exc, "e");
            a.c cVar = this.f1763c;
            if (cVar != null) {
                int size = this.f1764d.size();
                f fVar = new f();
                fVar.f11657b = ((Path) obj).toAbsolutePath().toString();
                fVar.f11671p = "MiEngineNG:safe;";
                z zVar = z.f4408a;
                cVar.d(0, i6 + 1, size, new f[]{fVar});
            }
        }

        @Override // v1.q
        public /* bridge */ /* synthetic */ Object v(Object obj, Object obj2, Object obj3) {
            a(((Number) obj).intValue(), obj2, (Exception) obj3);
            return z.f4408a;
        }
    }

    static final class c extends w1.n implements v1.l {

        /* JADX INFO: renamed from: c, reason: collision with root package name */
        public static final c f1765c = new c();

        c() {
            super(1);
        }

        @Override // v1.l
        /* JADX INFO: renamed from: a, reason: merged with bridge method [inline-methods] */
        public final CharSequence e(k kVar) {
            l.f(kVar, "it");
            return kVar.d();
        }
    }

    static final class d extends w1.n implements v1.l {

        /* JADX INFO: renamed from: c, reason: collision with root package name */
        public static final d f1766c = new d();

        d() {
            super(1);
        }

        @Override // v1.l
        /* JADX INFO: renamed from: a, reason: merged with bridge method [inline-methods] */
        public final CharSequence e(k kVar) {
            l.f(kVar, "it");
            return kVar.d();
        }
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public MiEngineNG(f0.b bVar) {
        super(bVar);
        l.f(bVar, "engineDesc");
        Context contextC = c();
        l.e(contextC, "getContext(...)");
        String strB = w0.b.b();
        l.e(strB, "getUUID(...)");
        this.delegate = new n(contextC, strB, null, 4, null);
    }

    private final void s(t0.a aVar) {
        m mVar = new m();
        mVar.f5034c = aVar.getPackageName();
        mVar.f5032a = 5;
        mVar.f5038g = aVar.f();
        mVar.f5033b = aVar.d();
        c0.b.f(c()).g(mVar);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void t(a.c cVar, int i6) {
        if (cVar != null) {
            cVar.b();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void u(a.c cVar, List list, MiEngineNG miEngineNG, t0.a aVar, int i6) {
        l.f(list, "$paths");
        l.f(miEngineNG, "this$0");
        if (cVar != null) {
            int size = list.size();
            l.c(aVar);
            cVar.d(0, i6 + 1, size, miEngineNG.x(aVar));
        }
        if (aVar instanceof t0.f) {
            miEngineNG.s(aVar);
        }
        l.c(aVar);
        miEngineNG.y(aVar);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void v(List list, w1.z zVar) {
        l.f(list, "$paths");
        l.f(zVar, "$start");
        z.c.f12069a.a("MiEngineNG_Scan", new p("scan_count", Integer.valueOf(list.size())), new p("scan_time", Long.valueOf(r4.a.f(h.a.b(zVar.f11129b)))));
        Log.i("MiEngineNG", "Scan completely.");
    }

    private final f[] x(t0.a aVar) {
        f fVar = new f();
        fVar.f11658c = aVar.getPackageName();
        fVar.f11657b = aVar.getPath().toAbsolutePath().toString();
        fVar.f11661f = "MiEngineNG";
        fVar.f11665j = aVar.d();
        fVar.f11662g = aVar.c();
        fVar.f11667l = 1;
        fVar.f11671p = "MiEngineNG:safe;";
        f[] fVarArr = {fVar};
        if (!(aVar instanceof o)) {
            return fVarArr;
        }
        List<k> listA = ((o) aVar).a();
        ArrayList arrayList = new ArrayList(r.t(listA, 10));
        for (k kVar : listA) {
            f fVar2 = new f(fVar);
            fVar2.f11667l = l.a(kVar.c(), "Malware") ? 2 : 3;
            fVar2.f11660e = kVar.b();
            fVar2.f11659d = kVar.d();
            fVar2.f11671p = "MiEngineNG:danger;";
            arrayList.add(fVar2);
        }
        return (f[]) arrayList.toArray(new f[0]);
    }

    private final void y(t0.a aVar) {
        String strC0;
        String str;
        boolean z6 = aVar instanceof o;
        if (z6 || (aVar instanceof t0.f)) {
            if (z6 && (aVar instanceof t0.f)) {
                strC0 = y.c0(((o) aVar).a(), com.xiaomi.onetrack.util.z.f2775b, null, null, 0, null, c.f1765c, 30, null);
                str = "blackgrays_threats";
            } else if (z6) {
                strC0 = y.c0(((o) aVar).a(), com.xiaomi.onetrack.util.z.f2775b, null, null, 0, null, d.f1766c, 30, null);
                str = "threats";
            } else {
                strC0 = null;
                str = "blackgrays";
            }
            z.c.f12069a.a(str, w.a("packageName", aVar.getPackageName()), w.a("versionCode", Integer.valueOf(aVar.d())), w.a("versionName", aVar.c()), w.a("mfSha256", aVar.g()), w.a("signaturesMd5", aVar.f()), w.a("signaturesSha256", aVar.b()), w.a("installer", aVar.e()), w.a("threatName", strC0));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void z(a.b bVar, int i6) {
        if (i6 == -1) {
            if (bVar != null) {
                bVar.a(2, i6);
            }
            Log.e("MiEngineNG", "update: ");
        } else if (i6 == 0) {
            if (bVar != null) {
                bVar.a(0, i6);
            }
        } else if (i6 == 1 && bVar != null) {
            bVar.a(3, i6);
        }
    }

    @Override // f0.a
    public void a() {
        this.delegate.f();
    }

    @Override // f0.a
    public void e(a.InterfaceC0073a interfaceC0073a) {
        if (interfaceC0073a != null) {
            interfaceC0073a.b(0);
        }
    }

    @Override // f0.a
    public boolean f() {
        return false;
    }

    @Override // f0.a
    public boolean g() {
        return true;
    }

    @Override // f0.a
    public void h() {
    }

    @Override // f0.a
    public void j(final List list, final a.c cVar, boolean z6, String str) {
        int i6;
        l.f(list, "paths");
        w(b());
        Log.i("MiEngineNG", "scan: count " + list.size());
        final w1.z zVar = new w1.z();
        zVar.f11129b = h.f8256a.a();
        n nVar = this.delegate;
        ArrayList arrayList = new ArrayList(r.t(list, 10));
        Iterator it = list.iterator();
        while (it.hasNext()) {
            Path path = Paths.get((String) it.next(), new String[0]);
            l.e(path, "get(path)");
            arrayList.add(path);
        }
        b bVar = new b(cVar, list);
        IntConsumer intConsumer = new IntConsumer() { // from class: g0.b
            @Override // java.util.function.IntConsumer
            public final void accept(int i7) {
                MiEngineNG.t(cVar, i7);
            }
        };
        ObjIntConsumer objIntConsumer = new ObjIntConsumer() { // from class: g0.c
            @Override // java.util.function.ObjIntConsumer
            public final void accept(Object obj, int i7) {
                MiEngineNG.u(cVar, list, this, (t0.a) obj, i7);
            }
        };
        Runnable runnable = new Runnable() { // from class: g0.d
            @Override // java.lang.Runnable
            public final void run() {
                MiEngineNG.v(list, zVar);
            }
        };
        if (Build.IS_INTERNATIONAL_BUILD) {
            i6 = 3;
        } else {
            i6 = (!z6 ? 8 : 0) | 0;
        }
        nVar.i(arrayList, (96 & 2) != 0 ? null : bVar, (96 & 4) != 0 ? null : intConsumer, (96 & 8) != 0 ? null : objIntConsumer, (96 & 16) != 0 ? null : runnable, (96 & 32) == 0 ? null : null, (96 & 64) != 0 ? new Executor() { // from class: p0.m
            @Override // java.util.concurrent.Executor
            public final void execute(Runnable runnable2) {
                n.k(runnable2);
            }
        } : null, (96 & 128) != 0 ? 3 : 4 | i6);
    }

    @Override // f0.a
    public void k() {
        this.delegate.g();
    }

    @Override // f0.a
    public void l(final a.b bVar) {
        w(b());
        n.o(this.delegate, null, new IntConsumer() { // from class: g0.a
            @Override // java.util.function.IntConsumer
            public final void accept(int i6) {
                MiEngineNG.z(bVar, i6);
            }
        }, 1, null);
    }

    public void w(boolean z6) {
        this.delegate.m(z6);
    }
}
