package p0;

import android.os.Build;
import android.util.Log;
import com.miui.guardprovider.sdk.android.pojo.networkdomain.CloudScanApkInfo;
import com.miui.guardprovider.sdk.android.pojo.networkdomain.CloudScanPayload;
import h1.l0;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class f implements l0.e {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private final q0.d f7378a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private final t0.m f7379b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private int f7380c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private boolean f7381d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    private final a f7382e;

    public final class a {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private boolean f7383a;

        /* JADX INFO: renamed from: b, reason: collision with root package name */
        private boolean f7384b;

        public a() {
        }

        private final t0.k a(CloudScanPayload cloudScanPayload) {
            t0.m mVar = f.this.f7379b;
            String threatName = cloudScanPayload.getThreatName();
            if (threatName == null) {
                throw new IllegalArgumentException("Required value was null.".toString());
            }
            t0.l lVarC = mVar.c(threatName);
            return new t0.k(lVarC.a(), cloudScanPayload.getThreatName(), lVarC.b(), null, -1, "cloud");
        }

        private final t0.a d(t0.a aVar, CloudScanPayload cloudScanPayload) {
            EnumSet<CloudScanPayload.a> resultType = cloudScanPayload.getResultType();
            f fVar = f.this;
            if (resultType.contains(CloudScanPayload.a.BlackGrays)) {
                return t0.d.b(aVar);
            }
            if (resultType.contains(CloudScanPayload.a.Unknown) || resultType.contains(CloudScanPayload.a.Clean)) {
                return aVar;
            }
            Log.w("CloudScanner", "Unknown result with flag " + fVar.f7380c + ":" + resultType);
            return aVar;
        }

        private final t0.a e(t0.a aVar, CloudScanPayload cloudScanPayload) {
            EnumSet<CloudScanPayload.a> resultType = cloudScanPayload.getResultType();
            f fVar = f.this;
            if (resultType.contains(CloudScanPayload.a.Threats)) {
                return t0.d.c(aVar, h1.p.e(a(cloudScanPayload)));
            }
            if (resultType.contains(CloudScanPayload.a.Unknown) || resultType.contains(CloudScanPayload.a.Clean)) {
                return aVar;
            }
            Log.w("CloudScanner", "Unknown result with flag " + fVar.f7380c + ":" + resultType);
            return aVar;
        }

        public final t0.a b(t0.a aVar, CloudScanPayload cloudScanPayload) {
            w1.l.f(aVar, "app");
            w1.l.f(cloudScanPayload, "cloudScanResult");
            boolean z6 = this.f7383a;
            if (!z6 || !this.f7384b) {
                if (z6) {
                    return e(aVar, cloudScanPayload);
                }
                if (this.f7384b) {
                    return d(aVar, cloudScanPayload);
                }
                Log.w("CloudScanner", "Scan mode is not specified yet.The original input will be returned.");
                return aVar;
            }
            t0.a aVarE = e(aVar, cloudScanPayload);
            t0.a aVarD = d(aVar, cloudScanPayload);
            boolean z7 = aVarE instanceof t0.o;
            if (z7 && (aVarD instanceof t0.f)) {
                return t0.d.e(aVarE, null, 1, null);
            }
            if (z7) {
                return aVarE;
            }
            return aVarD instanceof t0.f ? aVarD : aVar;
        }

        public final void c(int i6) {
            this.f7383a = (i6 & 3) != 0;
            this.f7384b = (i6 & 4) != 0;
        }
    }

    static final class b extends m1.k implements v1.p {

        /* JADX INFO: renamed from: f, reason: collision with root package name */
        Object f7386f;

        /* JADX INFO: renamed from: g, reason: collision with root package name */
        Object f7387g;

        /* JADX INFO: renamed from: h, reason: collision with root package name */
        Object f7388h;

        /* JADX INFO: renamed from: i, reason: collision with root package name */
        int f7389i;

        /* JADX INFO: renamed from: j, reason: collision with root package name */
        int f7390j;

        /* JADX INFO: renamed from: k, reason: collision with root package name */
        private /* synthetic */ Object f7391k;

        /* JADX INFO: renamed from: m, reason: collision with root package name */
        final /* synthetic */ v4.c f7393m;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        b(v4.c cVar, k1.d dVar) {
            super(2, dVar);
            this.f7393m = cVar;
        }

        @Override // v1.p
        /* JADX INFO: renamed from: D, reason: merged with bridge method [inline-methods] */
        public final Object d(v4.d dVar, k1.d dVar2) {
            return ((b) b(dVar, dVar2)).k(g1.z.f4408a);
        }

        @Override // m1.a
        public final k1.d b(Object obj, k1.d dVar) {
            b bVar = f.this.new b(this.f7393m, dVar);
            bVar.f7391k = obj;
            return bVar;
        }

        /* JADX WARN: Code duplicated, block: B:35:0x012a  */
        /* JADX WARN: Code duplicated, block: B:37:0x0130  */
        /* JADX WARN: Code duplicated, block: B:40:0x013d  */
        /* JADX WARN: Code duplicated, block: B:42:0x0145  */
        /* JADX WARN: Code duplicated, block: B:45:0x015e A[LOOP:1: B:38:0x0137->B:45:0x015e, LOOP_END] */
        /* JADX WARN: Code duplicated, block: B:51:0x017e  */
        /* JADX WARN: Code duplicated, block: B:53:0x0186  */
        /* JADX WARN: Code duplicated, block: B:57:0x01ac A[LOOP:0: B:49:0x0178->B:57:0x01ac, LOOP_END] */
        /* JADX WARN: Code duplicated, block: B:60:0x01b1  */
        /* JADX WARN: Code duplicated, block: B:62:0x01bd  */
        /* JADX WARN: Code duplicated, block: B:67:0x01ab A[SYNTHETIC] */
        /* JADX WARN: Code duplicated, block: B:70:0x0160 A[SYNTHETIC] */
        /* JADX WARN: Instruction removed from duplicated block: B:62:0x01bd, please report this as an issue */
        @Override // m1.a
        public final Object k(Object obj) throws Throwable {
            v4.d dVar;
            v4.d dVar2;
            List list;
            List list2;
            u0.e eVar;
            f fVar;
            Object data;
            int i6;
            Iterator it;
            int i7;
            List list3;
            f fVar2;
            v4.d dVar3;
            int i8;
            int i9;
            t0.a aVarB;
            Object objC = l1.d.c();
            int i10 = this.f7390j;
            if (i10 != 0) {
                if (i10 == 1) {
                    dVar = (v4.d) this.f7391k;
                    g1.r.b(obj);
                } else if (i10 == 2) {
                    list = (List) this.f7387g;
                    list2 = (List) this.f7386f;
                    dVar2 = (v4.d) this.f7391k;
                    g1.r.b(obj);
                    eVar = (u0.e) obj;
                    fVar = f.this;
                    if (eVar.getCode() == 0) {
                        throw new IOException("Response code:" + eVar.getCode() + ", threatDesc:" + eVar.getDesc());
                    }
                    data = eVar.getData();
                    if (data != null) {
                        throw new IllegalStateException("Required value was null.".toString());
                    }
                    List list4 = (List) data;
                    i6 = 0;
                    for (Object obj2 : list4) {
                        i8 = i6 + 1;
                        if (i6 < 0) {
                            h1.q.s();
                        }
                        if (w1.l.a(((CloudScanPayload) obj2).getPackageName(), ((CloudScanApkInfo) list.get(i6)).getPackageName())) {
                            throw new IllegalStateException("Check failed.".toString());
                        }
                        i6 = i8;
                    }
                    it = list4.iterator();
                    i7 = 0;
                    v4.d dVar4 = dVar2;
                    list3 = list2;
                    fVar2 = fVar;
                    dVar3 = dVar4;
                } else {
                    if (i10 != 3) {
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                    }
                    int i11 = this.f7389i;
                    it = (Iterator) this.f7388h;
                    fVar2 = (f) this.f7387g;
                    list3 = (List) this.f7386f;
                    dVar3 = (v4.d) this.f7391k;
                    g1.r.b(obj);
                    i7 = i11;
                }
                while (it.hasNext()) {
                    Object next = it.next();
                    i9 = i7 + 1;
                    if (i7 < 0) {
                        h1.q.s();
                    }
                    aVarB = fVar2.f7382e.b((t0.a) list3.get(i7), (CloudScanPayload) next);
                    this.f7391k = dVar3;
                    this.f7386f = list3;
                    this.f7387g = fVar2;
                    this.f7388h = it;
                    this.f7389i = i9;
                    this.f7390j = 3;
                    if (dVar3.a(aVarB, this) == objC) {
                        return objC;
                    }
                    i7 = i9;
                }
                return g1.z.f4408a;
            }
            g1.r.b(obj);
            v4.d dVar5 = (v4.d) this.f7391k;
            Log.i("CloudScanner", "Scanning...");
            if (!f.this.f7381d) {
                Log.i("CloudScanner", "scan: invalid!!!!");
                throw new IOException("cloud scan is denied!!!");
            }
            v4.c cVar = this.f7393m;
            this.f7391k = dVar5;
            this.f7390j = 1;
            Object objC2 = v4.i.c(cVar, null, this, 1, null);
            if (objC2 == objC) {
                return objC;
            }
            dVar = dVar5;
            obj = objC2;
            List<t0.a> list5 = (List) obj;
            f fVar3 = f.this;
            ArrayList arrayList = new ArrayList(h1.r.t(list5, 10));
            for (t0.a aVar : list5) {
                aVar.getPackageName();
                arrayList.add(fVar3.k(aVar));
            }
            q0.d dVar6 = f.this.f7378a;
            g1.p[] pVarArr = new g1.p[6];
            pVarArr[0] = g1.w.a("androidVersion", String.valueOf(Build.VERSION.SDK_INT));
            pVarArr[1] = g1.w.a("miuiVersion", Build.VERSION.INCREMENTAL);
            pVarArr[2] = g1.w.a("timestamp", String.valueOf(System.currentTimeMillis()));
            pVarArr[3] = g1.w.a("uuid", w.f7555a.a());
            pVarArr[4] = g1.w.a("versionType", miui.os.Build.IS_STABLE_VERSION ? "stable" : miui.os.Build.IS_DEVELOPMENT_VERSION ? "development" : "alpha");
            pVarArr[5] = g1.w.a("scanType", String.valueOf(f.this.f7380c));
            Map<String, String> mapK = l0.k(pVarArr);
            this.f7391k = dVar;
            this.f7386f = list5;
            this.f7387g = arrayList;
            this.f7390j = 2;
            Object objA = dVar6.a(arrayList, mapK, this);
            if (objA == objC) {
                return objC;
            }
            dVar2 = dVar;
            list = arrayList;
            list2 = list5;
            obj = objA;
            eVar = (u0.e) obj;
            fVar = f.this;
            if (eVar.getCode() == 0) {
                throw new IOException("Response code:" + eVar.getCode() + ", threatDesc:" + eVar.getDesc());
            }
            data = eVar.getData();
            if (data != null) {
                throw new IllegalStateException("Required value was null.".toString());
            }
            List list6 = (List) data;
            i6 = 0;
            while (r7.hasNext()) {
                i8 = i6 + 1;
                if (i6 < 0) {
                    h1.q.s();
                }
                if (w1.l.a(((CloudScanPayload) obj2).getPackageName(), ((CloudScanApkInfo) list.get(i6)).getPackageName())) {
                    throw new IllegalStateException("Check failed.".toString());
                }
                i6 = i8;
            }
            it = list6.iterator();
            i7 = 0;
            v4.d dVar7 = dVar2;
            list3 = list2;
            fVar2 = fVar;
            dVar3 = dVar7;
            while (it.hasNext()) {
                Object next2 = it.next();
                i9 = i7 + 1;
                if (i7 < 0) {
                    h1.q.s();
                }
                aVarB = fVar2.f7382e.b((t0.a) list3.get(i7), (CloudScanPayload) next2);
                this.f7391k = dVar3;
                this.f7386f = list3;
                this.f7387g = fVar2;
                this.f7388h = it;
                this.f7389i = i9;
                this.f7390j = 3;
                if (dVar3.a(aVarB, this) == objC) {
                    return objC;
                }
                i7 = i9;
            }
            return g1.z.f4408a;
        }
    }

    public f(q0.d dVar, t0.m mVar) {
        w1.l.f(dVar, "cloudScanApi");
        w1.l.f(mVar, "threatTypeAnalyzer");
        this.f7378a = dVar;
        this.f7379b = mVar;
        this.f7380c = 3;
        this.f7382e = new a();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final CloudScanApkInfo k(t0.a aVar) {
        String packageName = aVar.getPackageName();
        String strValueOf = String.valueOf(aVar.d());
        String strF = aVar.f();
        String strB = aVar.b();
        String strG = aVar.g();
        if (strG == null) {
            strG = "";
        }
        return new CloudScanApkInfo(packageName, strValueOf, strF, strB, strG);
    }

    @Override // l0.e, l0.c
    public Object a(l0.d dVar, k1.d dVar2) {
        Object objB = dVar.a().b("scan_params");
        w1.l.d(objB, "null cannot be cast to non-null type kotlin.Int");
        int iIntValue = ((Integer) objB).intValue();
        this.f7380c = iIntValue;
        this.f7381d = (iIntValue & 8) == 0;
        this.f7382e.c(iIntValue);
        return super.a(dVar, dVar2);
    }

    @Override // l0.e
    /* JADX INFO: renamed from: j, reason: merged with bridge method [inline-methods] */
    public Object c(v4.c cVar, k1.d dVar) {
        return v4.e.i(new b(cVar, null));
    }
}
