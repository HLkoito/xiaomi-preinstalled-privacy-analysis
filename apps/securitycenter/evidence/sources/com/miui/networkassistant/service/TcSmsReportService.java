package com.miui.networkassistant.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.miui.networkassistant.config.Constants;
import com.miui.networkassistant.config.SimUserInfo;
import com.miui.networkassistant.ui.thread.ThreadPool;
import com.miui.networkassistant.utils.TelephonyUtil;
import com.miui.networkassistant.webapi.WebApiAccessHelper;
import com.miui.securitycenter.R;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONArray;
import x4.v;

/* JADX INFO: loaded from: classes2.dex */
public class TcSmsReportService extends Service {
    private static final String TAG = "TcSmsReportService";
    private int mCurrentSlotNum;
    private String mSmsAllStr;
    private String mSmsDirection;
    private String mSmsNum;
    private String mSmsReceiveNum;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private SmsReportStatus mStatus = SmsReportStatus.Init;
    private int mReportSmsType = -1;
    private BroadcastReceiver mReportStatusReceiver = new BroadcastReceiver() { // from class: com.miui.networkassistant.service.TcSmsReportService.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            TcSmsReportService.this.stopSelf();
        }
    };
    BroadcastReceiver mSmsSentReceiver = new BroadcastReceiver() { // from class: com.miui.networkassistant.service.TcSmsReportService.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (getResultCode() != -1) {
                TcSmsReportService.this.onSmsSentFailure();
            }
        }
    };
    private BroadcastReceiver mSmsReceiver = new BroadcastReceiver() { // from class: com.miui.networkassistant.service.TcSmsReportService.4
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String string;
            SmsMessage[] messagesFromIntent = TelephonyUtil.getMessagesFromIntent(intent);
            if (messagesFromIntent == null || messagesFromIntent.length == 0) {
                return;
            }
            if (TextUtils.equals(TelephonyUtil.removePhoneNumPrefix(messagesFromIntent[0].getDisplayOriginatingAddress(), "+86"), TcSmsReportService.this.mSmsReceiveNum)) {
                if (messagesFromIntent.length == 1) {
                    string = messagesFromIntent[0].getDisplayMessageBody();
                } else {
                    StringBuilder sb2 = new StringBuilder();
                    for (SmsMessage smsMessage : messagesFromIntent) {
                        sb2.append(smsMessage.getDisplayMessageBody());
                    }
                    string = sb2.toString();
                }
                TcSmsReportService.this.onSmsReceived(string);
            }
        }
    };
    private ArrayList<String> mSmsListSelfLocked = new ArrayList<>();
    private ArrayList<SmsReportListener> mListenersSelfLocked = new ArrayList<>();

    public interface SmsReportListener {
        void onSmsReceived();

        void onSmsSentFailure();

        void onTimeOut();
    }

    public enum SmsReportStatus {
        Init,
        Receiving,
        Timeout,
        SmsSendFailure,
        Received
    }

    public class TcSmsReportServiceBinder extends Binder {
        public TcSmsReportServiceBinder() {
        }

        private String getEncryptPhoneNumber(String str) {
            if (str == null || TextUtils.isEmpty(str)) {
                return "";
            }
            if (str.startsWith("+")) {
                str = str.substring(3);
            }
            return str.replaceAll("(?<=\\d{3})\\d(?=\\d{4})", "*");
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$report$0(boolean z10) {
            Toast.makeText(TcSmsReportService.this.getApplicationContext(), z10 ? R.string.tc_sms_report_upload_when_net : R.string.tc_sms_report_upload_success, 1).show();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$report$1(boolean z10, JSONArray jSONArray, SimUserInfo simUserInfo, int i10) {
            final boolean z11 = true;
            if (z10) {
                com.miui.common.net.c cVarReportTrafficCorrectionSms = WebApiAccessHelper.reportTrafficCorrectionSms(TcSmsReportService.this.mSmsNum, TcSmsReportService.this.mSmsDirection, TcSmsReportService.this.mSmsReceiveNum, jSONArray.toString(), String.valueOf(simUserInfo.getProvince()), String.valueOf(simUserInfo.getCity()), simUserInfo.getOperator(), String.valueOf(simUserInfo.getBrand()), String.valueOf(i10), null);
                z11 = (TextUtils.equals(cVarReportTrafficCorrectionSms.getCode(), "report success") || cVarReportTrafficCorrectionSms.isSuccess()) ? false : true;
                Log.d(TcSmsReportService.TAG, "report: result.code = " + cVarReportTrafficCorrectionSms.getCode());
            }
            ThreadPool.runOnUi(new Runnable() { // from class: com.miui.networkassistant.service.d
                @Override // java.lang.Runnable
                public final void run() {
                    this.f16682a.lambda$report$0(z11);
                }
            });
            if (z11) {
                JSONArray jSONArray2 = new JSONArray();
                jSONArray2.put(TcSmsReportService.this.mSmsNum);
                jSONArray2.put(TcSmsReportService.this.mSmsDirection);
                jSONArray2.put(TcSmsReportService.this.mSmsReceiveNum);
                jSONArray2.put(jSONArray.toString());
                jSONArray2.put(simUserInfo.getProvince());
                jSONArray2.put(simUserInfo.getCity());
                jSONArray2.put(simUserInfo.getOperator());
                jSONArray2.put(simUserInfo.getBrand());
                jSONArray2.put(i10);
                SimUserInfo.getInstance(TcSmsReportService.this.getApplicationContext(), TcSmsReportService.this.mCurrentSlotNum).setTcSmsReportCache(jSONArray2.toString());
            }
            TcSmsReportService.this.stopSelf();
        }

        public int getCurrentSlotNum() {
            return TcSmsReportService.this.mCurrentSlotNum;
        }

        public int getReportSmsType() {
            return TcSmsReportService.this.mReportSmsType;
        }

        public String getSmsDirection() {
            return TcSmsReportService.this.mSmsDirection;
        }

        public String getSmsNum() {
            return TcSmsReportService.this.mSmsNum;
        }

        public String getSmsReceiveNum() {
            return TcSmsReportService.this.mSmsReceiveNum;
        }

        public String getSmsReturned() {
            return TcSmsReportService.this.mSmsAllStr;
        }

        public SmsReportStatus getStatus() {
            return TcSmsReportService.this.mStatus;
        }

        public void registerSmsReportListener(SmsReportListener smsReportListener) {
            synchronized (TcSmsReportService.this.mListenersSelfLocked) {
                TcSmsReportService.this.mListenersSelfLocked.add(smsReportListener);
            }
        }

        public void report(final int i10) {
            final SimUserInfo simUserInfo = SimUserInfo.getInstance(TcSmsReportService.this.getApplicationContext(), TcSmsReportService.this.mCurrentSlotNum);
            final JSONArray jSONArray = new JSONArray();
            synchronized (TcSmsReportService.this.mSmsListSelfLocked) {
                Iterator it = TcSmsReportService.this.mSmsListSelfLocked.iterator();
                while (it.hasNext()) {
                    jSONArray.put((String) it.next());
                }
                jSONArray.put(getEncryptPhoneNumber(simUserInfo.getPhoneNumber()));
            }
            final boolean zF = p4.d.f(TcSmsReportService.this.getApplicationContext());
            ThreadPool.runOnPool(new Runnable() { // from class: com.miui.networkassistant.service.c
                @Override // java.lang.Runnable
                public final void run() {
                    this.f16677a.lambda$report$1(zF, jSONArray, simUserInfo, i10);
                }
            });
        }

        public void reset() {
            TcSmsReportService.this.mStatus = SmsReportStatus.Init;
            synchronized (TcSmsReportService.this.mSmsListSelfLocked) {
                TcSmsReportService.this.mSmsListSelfLocked.clear();
                TcSmsReportService.this.mSmsAllStr = null;
            }
            TcSmsReportService.this.stopSelf();
        }

        public void startMonitorSms(String str, String str2, String str3, int i10, int i11) {
            TcSmsReportService.this.mStatus = SmsReportStatus.Receiving;
            TcSmsReportService.this.mSmsNum = str;
            TcSmsReportService.this.mSmsDirection = str2;
            TcSmsReportService.this.mReportSmsType = i11;
            TcSmsReportService.this.mCurrentSlotNum = i10;
            TcSmsReportService.this.mSmsReceiveNum = str3;
            TcSmsReportService.this.sendSms(str, str2, i10);
            TcSmsReportService.this.startTimerTask();
        }

        public void unRegisterSmsReportListener(SmsReportListener smsReportListener) {
            synchronized (TcSmsReportService.this.mListenersSelfLocked) {
                TcSmsReportService.this.mListenersSelfLocked.remove(smsReportListener);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSmsReceived(String str) {
        stopReceiving();
        StringBuilder sb2 = new StringBuilder();
        synchronized (this.mSmsListSelfLocked) {
            for (String str2 : this.mSmsListSelfLocked) {
                if (TextUtils.equals(str, str2)) {
                    return;
                }
                sb2.append(str2);
                sb2.append("\n________________________\n\n");
            }
            this.mSmsListSelfLocked.add(str);
            sb2.append(str);
            this.mSmsAllStr = sb2.toString();
            synchronized (this.mListenersSelfLocked) {
                this.mStatus = SmsReportStatus.Received;
                if (!this.mListenersSelfLocked.isEmpty()) {
                    Iterator<SmsReportListener> it = this.mListenersSelfLocked.iterator();
                    while (it.hasNext()) {
                        it.next().onSmsReceived();
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSmsSentFailure() {
        stopReceiving();
        synchronized (this.mListenersSelfLocked) {
            this.mStatus = SmsReportStatus.SmsSendFailure;
            if (!this.mListenersSelfLocked.isEmpty()) {
                Iterator<SmsReportListener> it = this.mListenersSelfLocked.iterator();
                while (it.hasNext()) {
                    it.next().onSmsSentFailure();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTimeOut() {
        stopReceiving();
        synchronized (this.mListenersSelfLocked) {
            this.mStatus = SmsReportStatus.Timeout;
            if (!this.mListenersSelfLocked.isEmpty()) {
                Iterator<SmsReportListener> it = this.mListenersSelfLocked.iterator();
                while (it.hasNext()) {
                    it.next().onTimeOut();
                }
            }
        }
    }

    private void registerReportStatusReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.App.ACTION_BROADCAST_TC_SMS_REPORT_STATUS);
        v.m(this, this.mReportStatusReceiver, intentFilter, 2);
    }

    private void registerSmsReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.System.ACTION_SMS_RECEIVED);
        intentFilter.setPriority(Integer.MAX_VALUE);
        intentFilter.addCategory("android.intent.category.DEFAULT");
        v.n(this, this.mSmsReceiver, intentFilter, Constants.System.PERMISSION_BROADCAST_SMS, null, 2);
    }

    private void registerSmsSendedReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.System.SMS_RECEIVER_ACTION);
        v.m(this, this.mSmsSentReceiver, intentFilter, 2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendSms(String str, String str2, int i10) {
        registerSmsSendedReceiver();
        Intent intent = new Intent();
        intent.setAction(Constants.System.SMS_RECEIVER_ACTION);
        TelephonyUtil.sendTextMessage(str, null, str2, PendingIntent.getBroadcast(this, 0, intent, 67108864), null, i10);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startTimerTask() {
        this.mTimer = new Timer();
        TimerTask timerTask = new TimerTask() { // from class: com.miui.networkassistant.service.TcSmsReportService.1
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                TcSmsReportService.this.onTimeOut();
            }
        };
        this.mTimerTask = timerTask;
        this.mTimer.schedule(timerTask, 120000L);
    }

    private void stopReceiving() {
        stopTimerTask();
    }

    private void stopTimerTask() {
        Timer timer = this.mTimer;
        if (timer != null) {
            timer.cancel();
        }
    }

    private void unRegisterReportStatusReceiver() {
        unregisterReceiver(this.mReportStatusReceiver);
    }

    private void unRegisterSmsReceiver() {
        unregisterReceiver(this.mSmsReceiver);
    }

    private void unRegisterSmsSendedReceiver() {
        try {
            BroadcastReceiver broadcastReceiver = this.mSmsSentReceiver;
            if (broadcastReceiver != null) {
                unregisterReceiver(broadcastReceiver);
            }
        } catch (IllegalArgumentException e10) {
            Log.i(TAG, "unRegisterSmsSendedReceiver", e10);
        }
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return new TcSmsReportServiceBinder();
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        ThreadPool.startup();
        registerSmsSendedReceiver();
        registerSmsReceiver();
        registerReportStatusReceiver();
    }

    @Override // android.app.Service
    public void onDestroy() {
        unRegisterSmsSendedReceiver();
        unRegisterSmsReceiver();
        unRegisterReportStatusReceiver();
        super.onDestroy();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i10, int i11) {
        return super.onStartCommand(intent, 1, i11);
    }
}
