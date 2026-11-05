package com.miui.bubbles.services;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.miui.bubbles.utils.BubbleUpManager;
import com.miui.networkassistant.utils.VirtualSimUtil;

/* JADX INFO: loaded from: classes2.dex */
public class BubblesNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "MiuiBubbles.NotificationController";

    public static void configNotificationListener(Context context) {
        Log.i(TAG, "configNotificationListener: ");
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(VirtualSimUtil.LAUNCH_FROM_NOTIFICATION);
        ComponentName componentName = new ComponentName(context.getPackageName(), BubblesNotificationListenerService.class.getName());
        if (BubbleUpManager.isSupportBubbleUpNotification() && !notificationManager.isNotificationListenerAccessGranted(componentName)) {
            notificationManager.setNotificationListenerAccessGranted(componentName, true);
        }
        try {
            context.getPackageManager().setComponentEnabledSetting(componentName, BubbleUpManager.isSupportBubbleUpNotification() ? 1 : 2, 1);
        } catch (Exception unused) {
        }
        Log.i(TAG, "configNotificationListener: end");
    }

    @Override // android.service.notification.NotificationListenerService
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.i(TAG, "onListenerConnected: ...");
    }

    @Override // android.service.notification.NotificationListenerService
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.i(TAG, "onListenerDisconnected: ...");
    }

    @Override // android.service.notification.NotificationListenerService
    public void onNotificationPosted(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
        super.onNotificationPosted(statusBarNotification, rankingMap);
        BubblesNotificationHelper.getInstance().onNotificationPosted(statusBarNotification, rankingMap);
    }

    @Override // android.service.notification.NotificationListenerService
    public void onNotificationRemoved(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap, int i10) {
        super.onNotificationRemoved(statusBarNotification, rankingMap, i10);
        BubblesNotificationHelper.getInstance().onNotificationRemoved(statusBarNotification, rankingMap, i10);
    }
}
