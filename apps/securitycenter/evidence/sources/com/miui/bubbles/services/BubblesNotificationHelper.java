package com.miui.bubbles.services;

import android.content.Context;
import android.database.ContentObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import com.miui.bubbles.IUiProcessBinder;
import com.miui.bubbles.settings.BubblesSettings;
import java.lang.ref.WeakReference;

/* JADX INFO: loaded from: classes2.dex */
public class BubblesNotificationHelper {
    private static final String TAG = "BubblesNotificationHelper";
    private WeakReference<IUiProcessBinder> iUiProcessBinder;
    private String mActiveBubbleApps;
    private final ContentObserver mContentObserver;
    private final Context mContext;

    private static class InstanceHolder {
        private static final BubblesNotificationHelper sInstance = new BubblesNotificationHelper();

        private InstanceHolder() {
        }
    }

    private BubblesNotificationHelper() {
        ContentObserver contentObserver = new ContentObserver(null) { // from class: com.miui.bubbles.services.BubblesNotificationHelper.1
            @Override // android.database.ContentObserver
            public void onChange(boolean z10) {
                super.onChange(z10);
                BubblesNotificationHelper bubblesNotificationHelper = BubblesNotificationHelper.this;
                bubblesNotificationHelper.mActiveBubbleApps = Settings.Secure.getStringForUser(bubblesNotificationHelper.mContext.getContentResolver(), BubblesSettings.SETTINGS_KEY_MIUI_BUBBLES_APP, UserHandle.myUserId());
                Log.d(BubblesNotificationHelper.TAG, "onChange: " + BubblesNotificationHelper.this.mActiveBubbleApps);
            }
        };
        this.mContentObserver = contentObserver;
        Context contextC = com.miui.common.e.c();
        this.mContext = contextC;
        contextC.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(BubblesSettings.SETTINGS_KEY_MIUI_BUBBLES_APP), false, contentObserver, -1);
        this.mActiveBubbleApps = Settings.Secure.getStringForUser(contextC.getContentResolver(), BubblesSettings.SETTINGS_KEY_MIUI_BUBBLES_APP, UserHandle.myUserId());
    }

    public static BubblesNotificationHelper getInstance() {
        return InstanceHolder.sInstance;
    }

    public void onNotificationPosted(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
        WeakReference<IUiProcessBinder> weakReference = this.iUiProcessBinder;
        if (weakReference == null || weakReference.get() == null) {
            if (TextUtils.isEmpty(this.mActiveBubbleApps)) {
                return;
            }
            BubblesSettings.getInstance(this.mContext).notifyBubbleAppChanged(true, "onNotificationPosted");
            return;
        }
        IUiProcessBinder iUiProcessBinder = this.iUiProcessBinder.get();
        Log.d(TAG, "onNotificationPosted: " + iUiProcessBinder);
        try {
            iUiProcessBinder.onNotificationPosted(statusBarNotification, rankingMap);
        } catch (Exception e10) {
            Log.d(TAG, "onNotificationPosted Exception: " + e10);
        }
    }

    public void onNotificationRemoved(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap, int i10) {
        IUiProcessBinder iUiProcessBinder;
        WeakReference<IUiProcessBinder> weakReference = this.iUiProcessBinder;
        if (weakReference == null || (iUiProcessBinder = weakReference.get()) == null) {
            return;
        }
        try {
            iUiProcessBinder.onNotificationRemoved(statusBarNotification);
        } catch (Exception e10) {
            Log.d(TAG, "onNotificationRemoved Exception: " + e10);
        }
    }

    public void setUiProcessBinder(IUiProcessBinder iUiProcessBinder) {
        this.iUiProcessBinder = new WeakReference<>(iUiProcessBinder);
    }
}
