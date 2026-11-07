package z;

import android.util.Log;
import com.miui.guardprovider.GuardApplication;
import com.xiaomi.onetrack.Configuration;
import com.xiaomi.onetrack.OneTrack;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class b {

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    public static b f12066c;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private OneTrack f12067a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private final Configuration f12068b;

    private b() {
        Configuration configurationBuild = new Configuration.Builder().setAppId("2882303761517522561").setChannel(GuardApplication.f1743d.getPackageName()).setExceptionCatcherEnable(true).setMode(OneTrack.Mode.APP).setAutoTrackActivityAction(true).setGAIDEnable(false).build();
        this.f12068b = configurationBuild;
        try {
            c();
            this.f12067a = OneTrack.createInstance(GuardApplication.f1743d, configurationBuild);
        } catch (Exception e7) {
            Log.e("OneTrackManager", "init onetrack error", e7);
        }
    }

    public static synchronized b a() {
        if (f12066c == null) {
            f12066c = new b();
        }
        return f12066c;
    }

    public static void c() {
        GuardApplication guardApplication = GuardApplication.f1743d;
        OneTrack.setAccessNetworkEnable(guardApplication, GuardApplication.e(guardApplication));
    }

    public void b(String str, Map map) {
        OneTrack oneTrack = this.f12067a;
        if (oneTrack != null) {
            oneTrack.track(str, map);
        }
    }
}
