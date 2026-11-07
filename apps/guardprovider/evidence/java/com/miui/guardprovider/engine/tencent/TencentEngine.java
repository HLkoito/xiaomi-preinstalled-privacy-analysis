package com.miui.guardprovider.engine.tencent;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import miui.os.Build;
import tmsdk.bg.creator.ManagerCreatorB;
import tmsdk.bg.module.wifidetect.IWifiDetectListener;
import tmsdk.bg.module.wifidetect.WifiDetectManager;
import tmsdk.common.ITMSApplicaionConfig;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.qscanner.QScanListener;
import tmsdk.common.module.qscanner.QScanResultEntity;
import tmsdk.common.module.qscanner.QScannerManagerV2;
import tmsdk.common.module.update.CheckResult;
import tmsdk.common.module.update.ICheckListener;
import tmsdk.common.module.update.IUpdateListener;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.module.update.UpdateInfo;
import tmsdk.common.module.update.UpdateManager;
import tmsdk.fg.creator.ManagerCreatorF;
import y.f;

/* JADX INFO: loaded from: classes.dex */
public class TencentEngine extends f0.a {

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    private static boolean f1767f = false;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    private static final Object f1768g = new Object();

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private QScannerManagerV2 f1769d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    private UpdateManager f1770e;

    class a implements Runnable {

        /* JADX INFO: renamed from: b, reason: collision with root package name */
        final /* synthetic */ f0.a.d f1771b;

        /* JADX INFO: renamed from: c, reason: collision with root package name */
        final /* synthetic */ y.d f1772c;

        /* JADX INFO: renamed from: d, reason: collision with root package name */
        final /* synthetic */ Object f1773d;

        a(f0.a.d dVar, y.d dVar2, Object obj) {
            this.f1771b = dVar;
            this.f1772c = dVar2;
            this.f1773d = obj;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static /* synthetic */ void b(f0.a.d dVar, y.d dVar2, WifiDetectManager wifiDetectManager, int i6) {
            try {
                try {
                    Log.i("TencentEngine", "wifiDetect:onResult type = " + dVar + "; ret = " + i6);
                    if (dVar2 != null) {
                        dVar2.onResult(i6);
                    }
                } catch (RemoteException e7) {
                    Log.e("TencentEngine", "onResult: ", e7);
                }
            } finally {
                wifiDetectManager.free();
            }
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (TencentEngine.f1768g) {
                final WifiDetectManager wifiDetectManager = (WifiDetectManager) ManagerCreatorB.getManager(WifiDetectManager.class);
                final f0.a.d dVar = this.f1771b;
                final y.d dVar2 = this.f1772c;
                IWifiDetectListener iWifiDetectListener = new IWifiDetectListener() { // from class: com.miui.guardprovider.engine.tencent.a
                    @Override // tmsdk.bg.module.wifidetect.IWifiDetectListener
                    public final void onResult(int i6) {
                        TencentEngine.a.b(dVar, dVar2, wifiDetectManager, i6);
                    }
                };
                if (wifiDetectManager != null) {
                    wifiDetectManager.init();
                    int i6 = b.f1775a[this.f1771b.ordinal()];
                    if (i6 == 1) {
                        iWifiDetectListener.onResult(wifiDetectManager.detectNetworkState());
                    } else if (i6 != 2) {
                        int iDetectARPNoRoot = -2;
                        if (i6 == 3) {
                            if (this.f1773d instanceof String) {
                                iDetectARPNoRoot = wifiDetectManager.detectARPNoRoot(15000L);
                            }
                            iWifiDetectListener.onResult(iDetectARPNoRoot);
                        } else if (i6 == 4) {
                            try {
                                WifiManager wifiManager = (WifiManager) TencentEngine.this.c().getApplicationContext().getSystemService("wifi");
                                WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                                for (ScanResult scanResult : wifiManager.getScanResults()) {
                                    String bssid = connectionInfo.getBSSID();
                                    if (bssid != null && bssid.compareTo(scanResult.BSSID) == 0) {
                                        iDetectARPNoRoot = wifiDetectManager.detectSecurity(scanResult);
                                        break;
                                    }
                                }
                            } catch (Exception e7) {
                                Log.e("TencentEngine", "error when check wifi security: ", e7);
                            }
                            iWifiDetectListener.onResult(iDetectARPNoRoot);
                        }
                    } else {
                        int iDetectDnsAndPhishing = wifiDetectManager.detectDnsAndPhishing(iWifiDetectListener, 0L);
                        if (iDetectDnsAndPhishing < 0) {
                            iWifiDetectListener.onResult(iDetectDnsAndPhishing);
                        }
                    }
                }
            }
        }
    }

    static /* synthetic */ class b {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        static final /* synthetic */ int[] f1775a;

        static {
            int[] iArr = new int[f0.a.d.values().length];
            f1775a = iArr;
            try {
                iArr[f0.a.d.NETWORKSTATE.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                f1775a[f0.a.d.DNSANDPHISHING.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                f1775a[f0.a.d.ARP.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                f1775a[f0.a.d.SECURITY.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    class c implements ICheckListener {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final f0.a.b f1776a;

        /* JADX INFO: renamed from: b, reason: collision with root package name */
        private int f1777b = -2146769239;

        class a implements IUpdateListener {
            a() {
            }

            @Override // tmsdk.common.module.update.IUpdateListener
            public void onProgressChanged(UpdateInfo updateInfo, int i6) {
                if (c.this.f1776a != null) {
                    c.this.f1776a.b(i6);
                }
            }

            @Override // tmsdk.common.module.update.IUpdateListener
            public void onUpdateCanceled() {
                if (c.this.f1776a != null) {
                    c.this.f1776a.onUpdateCanceled();
                }
                if (TencentEngine.this.f1770e != null) {
                    TencentEngine.this.f1770e = null;
                }
            }

            @Override // tmsdk.common.module.update.IUpdateListener
            public void onUpdateEvent(UpdateInfo updateInfo, int i6) {
                c.this.f1777b = i6;
            }

            @Override // tmsdk.common.module.update.IUpdateListener
            public void onUpdateFinished() {
                if (c.this.f1776a != null) {
                    if (c.this.f1777b != -2146769239) {
                        Log.d("TencentEngine", "TencentEngine onUpdateFinished failed:" + c.this.f1777b);
                        c.this.f1776a.a(2, c.this.f1777b);
                    } else {
                        Log.d("TencentEngine", "TencentEngine onUpdateFinished success:" + c.this.f1777b);
                        c.this.f1776a.b(100);
                        c.this.f1776a.a(0, c.this.f1777b);
                    }
                }
                if (TencentEngine.this.f1770e != null) {
                    TencentEngine.this.f1770e = null;
                }
            }

            @Override // tmsdk.common.module.update.IUpdateListener
            public void onUpdateStarted() {
                if (c.this.f1776a != null) {
                    Log.d("TencentEngine", "TencentEngine onUpdateStarted");
                    c.this.f1776a.onUpdateStarted();
                }
            }
        }

        c(f0.a.b bVar) {
            this.f1776a = bVar;
        }

        @Override // tmsdk.common.module.update.ICheckListener
        public void onCheckCanceled() {
            f0.a.b bVar = this.f1776a;
            if (bVar != null) {
                bVar.onUpdateCanceled();
            }
            if (TencentEngine.this.f1770e != null) {
                TencentEngine.this.f1770e = null;
            }
        }

        @Override // tmsdk.common.module.update.ICheckListener
        public void onCheckEvent(int i6) {
            this.f1777b = i6;
            Log.i("TencentEngine", "check error:" + i6);
        }

        @Override // tmsdk.common.module.update.ICheckListener
        public void onCheckFinished(CheckResult checkResult) {
            List<UpdateInfo> list;
            if (TencentEngine.this.f1770e != null && checkResult != null && (list = checkResult.mUpdateInfoList) != null && list.size() > 0) {
                TencentEngine.this.f1770e.update(checkResult.mUpdateInfoList, new a());
                return;
            }
            if (this.f1777b != -2146769239) {
                if (this.f1776a != null) {
                    Log.d("TencentEngine", "TencentEngine onCheckFinished failed:" + this.f1777b);
                    this.f1776a.a(2, this.f1777b);
                }
                if (TencentEngine.this.f1770e != null) {
                    TencentEngine.this.f1770e = null;
                    return;
                }
                return;
            }
            if (this.f1776a != null) {
                Log.d("TencentEngine", "TencentEngine onCheckFinished no update:" + this.f1777b);
                this.f1776a.a(3, this.f1777b);
            }
            if (TencentEngine.this.f1770e != null) {
                TencentEngine.this.f1770e = null;
            }
        }

        @Override // tmsdk.common.module.update.ICheckListener
        public void onCheckStarted() {
            f0.a.b bVar = this.f1776a;
            if (bVar != null) {
                bVar.onCheckStarted();
            }
        }
    }

    class d implements QScanListener {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final f0.a.c f1780a;

        /* JADX INFO: renamed from: b, reason: collision with root package name */
        int f1781b;

        /* JADX INFO: renamed from: c, reason: collision with root package name */
        int f1782c = 0;

        d(f0.a.c cVar, int i6) {
            this.f1780a = cVar;
            this.f1781b = i6;
        }

        @Override // tmsdk.common.module.qscanner.QScanListener
        public void onScanCanceled(int i6) {
            Log.i("TencentEngine", "onScanCanceled");
            this.f1780a.a();
        }

        @Override // tmsdk.common.module.qscanner.QScanListener
        public void onScanContinue(int i6) {
            Log.i("TencentEngine", "onScanContinue, scanType:[" + i6 + "]");
        }

        @Override // tmsdk.common.module.qscanner.QScanListener
        public void onScanError(int i6) {
            Log.i("TencentEngine", "onScanError, errCode:[" + i6 + "]");
        }

        @Override // tmsdk.common.module.qscanner.QScanListener
        public void onScanFinished(int i6, List list) {
            Log.i("TencentEngine", "onScanFinished");
            if (list != null) {
                int size = list.size();
                Iterator it = list.iterator();
                int i7 = 0;
                while (it.hasNext()) {
                    QScanResultEntity qScanResultEntity = (QScanResultEntity) it.next();
                    if (TencentEngine.this.f3536c) {
                        return;
                    }
                    Log.i("TencentEngine", "onScanFinished:scanType: " + i6 + "current:" + i7 + " result:" + qScanResultEntity.scanResult);
                    this.f1780a.d(i6, i7, size, new f[]{TencentEngine.this.u(qScanResultEntity)});
                    i7++;
                }
            }
            int i8 = this.f1782c + 1;
            this.f1782c = i8;
            if (i8 == this.f1781b) {
                this.f1780a.c(false);
            }
        }

        @Override // tmsdk.common.module.qscanner.QScanListener
        public void onScanPaused(int i6) {
            Log.i("TencentEngine", "onScanPaused, scanType:[" + i6 + "]");
        }

        @Override // tmsdk.common.module.qscanner.QScanListener
        public void onScanProgress(int i6, int i7, int i8, String str, String str2) {
            Log.i("TencentEngine", "onScanProgress, packageName:[" + str + "] ,scanType:[" + i6 + "]");
        }

        @Override // tmsdk.common.module.qscanner.QScanListener
        public void onScanStarted(int i6) {
            Log.i("TencentEngine", "onScanStarted, scanType:" + i6);
        }
    }

    public TencentEngine(f0.b bVar) {
        super(bVar);
    }

    private int t(int i6) {
        switch (i6) {
            case 258:
                return 5;
            case 259:
            case 261:
            case 262:
                return 3;
            case 260:
                return 2;
            default:
                return 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public f u(QScanResultEntity qScanResultEntity) {
        f fVar = new f();
        fVar.f11662g = qScanResultEntity.version;
        fVar.f11665j = qScanResultEntity.versionCode;
        fVar.f11660e = qScanResultEntity.virusDiscription;
        fVar.f11658c = qScanResultEntity.packageName;
        fVar.f11657b = qScanResultEntity.path;
        fVar.f11659d = qScanResultEntity.virusName;
        fVar.f11667l = t(qScanResultEntity.scanResult);
        fVar.f11666k = 1;
        fVar.f11661f = d();
        int i6 = fVar.f11667l;
        if (i6 == 2 || i6 == 3 || i6 == 6) {
            Log.i("TencentEngine", "getVirusInfo packageName : " + fVar.f11658c + " virusLevel : " + fVar.f11667l);
            fVar.f11671p = "TENCENT:danger;";
        } else {
            fVar.f11671p = "TENCENT:safe;";
        }
        return fVar;
    }

    @Override // f0.a
    public void a() {
        if (this.f1769d != null) {
            this.f3536c = true;
            this.f1769d.cancelScan();
        }
    }

    @Override // f0.a
    public void e(f0.a.InterfaceC0073a interfaceC0073a) {
        synchronized (TencentEngine.class) {
            if (!f1767f) {
                v(b());
                TMSDKContext.setDeviceInfo(c(), Settings.Secure.getString(c().getContentResolver(), "android_id"), Build.MODEL);
                f1767f = TMSDKContext.init(c(), null, new ITMSApplicaionConfig() { // from class: k0.a
                    @Override // tmsdk.common.ITMSApplicaionConfig
                    public final HashMap config(Map map) {
                        return new HashMap(map);
                    }
                });
            }
        }
        if (f1767f) {
            interfaceC0073a.b(0);
        } else {
            interfaceC0073a.a(-1);
        }
    }

    @Override // f0.a
    public boolean f() {
        return !f1767f;
    }

    @Override // f0.a
    public boolean g() {
        return true;
    }

    @Override // f0.a
    public void h() {
        QScannerManagerV2 qScannerManagerV2 = this.f1769d;
        if (qScannerManagerV2 != null) {
            qScannerManagerV2.freeScanner();
            this.f1769d = null;
        }
    }

    @Override // f0.a
    public void j(List list, f0.a.c cVar, boolean z6, String str) {
        if (cVar == null) {
            Log.e("TencentEngine", "ERROR ! Tencent SCAN EXIT caused by null listener");
            return;
        }
        this.f3536c = false;
        if (this.f1769d == null) {
            this.f1769d = (QScannerManagerV2) ManagerCreatorF.getManager(QScannerManagerV2.class);
        }
        QScannerManagerV2 qScannerManagerV2 = this.f1769d;
        if (qScannerManagerV2 == null) {
            cVar.c(false);
            return;
        }
        if (qScannerManagerV2.initScanner() != 0) {
            cVar.c(false);
            return;
        }
        v(b());
        Log.w("TencentEngine", " is cloud scan : " + b());
        d dVar = new d(cVar, list.size());
        int i6 = (b() && z6) ? 6 : 2;
        Iterator it = list.iterator();
        while (it.hasNext()) {
            String str2 = (String) it.next();
            try {
                Thread.sleep(5L);
            } catch (Exception unused) {
            }
            if (this.f3536c || this.f1769d == null) {
                return;
            }
            ArrayList arrayList = new ArrayList();
            arrayList.add(str2);
            this.f1769d.scanUninstallApks(i6, arrayList, dVar, 0L);
        }
    }

    @Override // f0.a
    public void k() {
        UpdateManager updateManager = this.f1770e;
        if (updateManager != null) {
            updateManager.cancel();
            this.f1770e = null;
        }
    }

    @Override // f0.a
    public void l(f0.a.b bVar) {
        v(b());
        if (this.f1770e == null) {
            this.f1770e = (UpdateManager) ManagerCreatorC.getManager(UpdateManager.class);
        }
        UpdateManager updateManager = this.f1770e;
        if (updateManager != null) {
            updateManager.check(UpdateConfig.UPDATE_FLAG_VIRUS_BASE, new c(bVar), -1L);
        } else if (bVar != null) {
            bVar.a(2, -2146769239);
        }
    }

    @Override // f0.a
    public int n(y.d dVar, f0.a.d dVar2, Object obj) {
        Log.i("TencentEngine", "wifiDetect:observer=" + dVar + "; type=" + dVar2 + "; arg" + obj);
        i(new a(dVar2, dVar, obj));
        return -1;
    }

    public void v(boolean z6) {
        TMSDKContext.setAutoConnectionSwitch(z6);
    }
}
