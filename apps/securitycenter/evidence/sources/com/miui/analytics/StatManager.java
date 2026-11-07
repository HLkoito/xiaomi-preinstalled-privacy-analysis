package com.miui.analytics;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.miui.common.e;
import com.xiaomi.onetrack.Configuration;
import com.xiaomi.onetrack.OneTrack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import x4.g;
import x4.l1;

/* JADX INFO: loaded from: classes2.dex */
public class StatManager {
    private static final String ANTIVIRUS_ID = "31000000943";
    private static final String APP_ID = "31000000037";
    private static final String APP_ID_APP_MAN = "31000000945";
    private static final String APP_ID_V2 = "31000401489";
    private static final String CONVERSATION_TOOL_BOX_ID = "31000401977";
    private static final String DOCK_ID = "31000000538";
    private static final String EARTHQUAKE_WARNING_ID = "31000401472";
    private static final String GAME_BOX_ID = "31000000057";
    private static final String GAME_TURBO_ID = "31000000560";
    private static final String GAME_TURBO_NEW_ID = "31000401977";
    private static final String IME_APP_ID = "31000000494";
    private static final String MALICIOUS_APP_TRACK_ID = "31000401557";
    private static final String MI_SAFETY_OPEN_SERVICE_APP_ID = "31000401800";
    private static final String NETWORK_ASSISTANT = "31000000807";
    private static final String ONE_TRACK_AD_APP_ID = "31000000893";
    private static final String PAD_APP_ID = "31000000506";
    private static final String TAG = "BaseStatManager";
    private static final String TRACK_KEY_PAY_EVENT_TOTAL = "antivirus_pay_event_total";
    private static final Object mLock = new Object();
    private static StatManager sInstance;
    private OneTrack mAppManOneTrack;
    private OneTrack mOneTrack;
    private OneTrack mOneTrackV2;
    private final Map<String, OneTrack> mOneTrackMap = new HashMap();
    private final Configuration.Builder mConfigBuilder = new Configuration.Builder().setChannel(e.d().getPackageName()).setExceptionCatcherEnable(true).setMode(OneTrack.Mode.APP).setAdEventAppId(ONE_TRACK_AD_APP_ID).setGAIDEnable(false);

    private StatManager() {
        initSdkInternal();
    }

    public static StatManager getInstance() {
        StatManager statManager;
        synchronized (mLock) {
            if (sInstance == null) {
                sInstance = new StatManager();
            }
            statManager = sInstance;
        }
        return statManager;
    }

    private void initSdkInternal() {
        try {
            Application applicationD = e.d();
            OneTrack.setAccessNetworkEnable(applicationD, l1.b());
            OneTrack.setUseSystemNetTrafficOnly();
            OneTrack oneTrackCreateInstance = OneTrack.createInstance(applicationD, this.mConfigBuilder.setAppId(g.b() ? PAD_APP_ID : APP_ID).build());
            this.mOneTrack = oneTrackCreateInstance;
            oneTrackCreateInstance.setEventHook(new OneTrack.IEventHook() { // from class: com.miui.analytics.StatManager.1
                @Override // com.xiaomi.onetrack.OneTrack.IEventHook
                public boolean isCustomDauEvent(String str) {
                    return false;
                }

                @Override // com.xiaomi.onetrack.OneTrack.IEventHook
                public boolean isRecommendEvent(String str) {
                    return StatManager.TRACK_KEY_PAY_EVENT_TOTAL.equals(str);
                }
            });
            Configuration.Builder appId = this.mConfigBuilder.setAppId("31000401977");
            OneTrack.Mode mode = OneTrack.Mode.PLUGIN;
            this.mOneTrackMap.put(StatConstants.Channel.CONVERSATION_TOOL_BOX, OneTrack.createInstance(applicationD, appId.setMode(mode).setUseCustomPrivacyPolicy(true).build()));
            this.mConfigBuilder.setMode(OneTrack.Mode.APP);
            this.mOneTrackV2 = OneTrack.createInstance(applicationD, this.mConfigBuilder.setAppId(APP_ID_V2).setUseCustomPrivacyPolicy(true).build());
            if (PackageUtils.isPkgProcess(applicationD.getApplicationContext())) {
                this.mAppManOneTrack = OneTrack.createInstance(applicationD, this.mConfigBuilder.setAppId(APP_ID_APP_MAN).setUseCustomPrivacyPolicy(true).build());
            }
            this.mOneTrackMap.put(StatConstants.Channel.GAMETURBO, OneTrack.createInstance(applicationD.getApplicationContext(), this.mConfigBuilder.setAppId(GAME_TURBO_ID).setMode(mode).setUseCustomPrivacyPolicy(false).build()));
            this.mOneTrackMap.put(StatConstants.Channel.GAME_TURBO_NEW, OneTrack.createInstance(applicationD.getApplicationContext(), this.mConfigBuilder.setAppId("31000401977").build()));
            this.mOneTrackMap.put(StatConstants.Channel.DOCK, OneTrack.createInstance(applicationD.getApplicationContext(), this.mConfigBuilder.setAppId(DOCK_ID).build()));
            this.mOneTrackMap.put(StatConstants.Channel.GAMEBOX, OneTrack.createInstance(applicationD.getApplicationContext(), this.mConfigBuilder.setAppId(GAME_BOX_ID).build()));
            this.mOneTrackMap.put(StatConstants.Channel.NETWORK_ASSISTANT, OneTrack.createInstance(applicationD.getApplicationContext(), this.mConfigBuilder.setAppId(NETWORK_ASSISTANT).build()));
            this.mOneTrackMap.put(StatConstants.Channel.ANTIVIRUS, OneTrack.createInstance(applicationD.getApplicationContext(), this.mConfigBuilder.setAppId(ANTIVIRUS_ID).setAutoTrackActivityAction(false).build()));
            this.mOneTrackMap.put(StatConstants.Channel.EARTHQUAKE_WARNING, OneTrack.createInstance(applicationD.getApplicationContext(), this.mConfigBuilder.setAppId(EARTHQUAKE_WARNING_ID).build()));
            this.mOneTrackMap.put(StatConstants.Channel.MALICIOUS_APP_TRACK, OneTrack.createInstance(applicationD.getApplicationContext(), this.mConfigBuilder.setAppId(MALICIOUS_APP_TRACK_ID).build()));
            this.mOneTrackMap.put(StatConstants.Channel.MI_SAFETY_OPEN_SERVICE, OneTrack.createInstance(applicationD.getApplicationContext(), this.mConfigBuilder.setAppId(MI_SAFETY_OPEN_SERVICE_APP_ID).build()));
            this.mOneTrackMap.put(StatConstants.Channel.INPUT_METHOD, OneTrack.createInstance(applicationD.getApplicationContext(), new Configuration.Builder().setChannel(e.d().getPackageName()).setExceptionCatcherEnable(true).setMode(mode).setGAIDEnable(false).setAppId(IME_APP_ID).setAutoTrackActivityAction(false).build()));
            if (uf.a.f35870a) {
                OneTrack.setDebugMode(true);
            }
        } catch (Exception e10) {
            Log.e(TAG, "init onetrack error", e10);
        }
    }

    public void adTrack(String str, Map<String, Object> map, List<String> list) {
        if (this.mOneTrackV2 != null) {
            if (list == null || list.isEmpty()) {
                this.mOneTrackV2.adTrack(str, map);
            } else {
                this.mOneTrackV2.adTrack(str, map, list);
            }
        }
    }

    public void initSdk(Context context) {
    }

    public void resetAnalyticsData(Context context) {
    }

    public void setAccessNetworkEnable(boolean z10) {
        try {
            OneTrack.setAccessNetworkEnable(e.d(), z10);
            OneTrack oneTrack = this.mOneTrackMap.get(StatConstants.Channel.CONVERSATION_TOOL_BOX);
            if (oneTrack != null) {
                oneTrack.setCustomPrivacyPolicyAccepted(z10);
            }
        } catch (Exception e10) {
            Log.e(TAG, "onetrack setAccessNetwork error", e10);
        }
    }

    public void track(String str) {
        track(str, new HashMap());
    }

    public void track(String str, Map<String, Object> map) {
        OneTrack oneTrack = this.mOneTrack;
        if (oneTrack != null) {
            oneTrack.track(str, map);
        }
    }

    public void track(String str, Map<String, Object> map, String str2) {
        OneTrack oneTrack = this.mOneTrackMap.get(str2);
        if (oneTrack != null) {
            oneTrack.track(str, map);
        }
    }

    public void trackAppMan(String str, Map<String, Object> map) {
        OneTrack oneTrack = this.mAppManOneTrack;
        if (oneTrack != null) {
            oneTrack.track(str, map);
        }
    }

    public void trackPluginEvent(String str, String str2, Map<String, Object> map, String str3) {
        OneTrack oneTrack = this.mOneTrackMap.get(str3);
        if (oneTrack != null) {
            oneTrack.trackPluginEvent(str, str2, map);
        }
    }

    public void trackV2(String str) {
        trackV2(str, new HashMap());
    }

    public void trackV2(String str, Map<String, Object> map) {
        OneTrack oneTrack = this.mOneTrackV2;
        if (oneTrack != null) {
            oneTrack.track(str, map);
        }
    }
}
