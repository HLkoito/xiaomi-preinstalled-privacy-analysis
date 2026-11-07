package com.miui.warningcenter.policeassist;

import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import bh.f;
import com.google.android.exoplayer2.C;
import com.miui.earthquakewarning.utils.LocationRecordManager;
import com.miui.earthquakewarning.utils.MsgObservable;
import com.miui.earthquakewarning.utils.NotificationUtil;
import com.miui.policeassist.EPSManager;
import com.miui.securitycenter.Application;
import com.miui.warningcenter.analytics.AnalyticHelper;
import com.miui.warningcenter.policeassist.view.PaFloatWindowManager;
import com.xiaomi.onetrack.api.au;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import miui.process.ForegroundInfo;
import miui.process.IForegroundInfoListener;
import miui.securitycenter.NetworkUtils;
import miui.telephony.PhoneNumberUtils;
import p4.d;
import x4.w1;

/* JADX INFO: loaded from: classes3.dex */
public class PaService extends Service implements Observer {
    public static final String ACTION_PA_INIT = "action_pa_init";
    public static final String ACTION_SEND_MESSAGE = "action_send_message";
    public static final String ACTION_SEND_MESSAGE_DONE = "action_send_message_done";
    public static final String ACTION_SHOW_FLOAT_VIEW = "action_show_float_view";
    private static final String CLASS_PROCESS_MANAGER = "miui.process.ProcessManager";
    private static final long LOOP_TIME = 60000;
    private static final int MAX_RETRY_COUNT = 3;
    private static final String METHOD_REGISTER_FOREGROUND_LISTENER = "registerForegroundInfoListener";
    private static final String METHOD_UNREGISTER_FOREGROUND_LISTENER = "unregisterForegroundInfoListener";
    private static final String PACKAGE_INCALL_UI = "com.android.incallui";
    private static final String TAG = "PAService";
    private boolean isSendMessage;
    private String mCallNumber;
    private long mCallTime;
    private CountDownTimer mCountdownTimer;
    private boolean mCounterStart;
    private int mCurrentCallState;
    private long mFinishLocationTime;
    private PaFloatWindowManager mFloatWindowManager;
    private String mForegroundPkgName;
    private GetMyPhoneNumberTask mGetMyPhoneNumberTask;
    private CountDownTimer mLocationTimer;
    private boolean mMobileDataEnable;
    private long mStartLocationTime;
    private CountDownTimer mStopServiceTimer;
    private TelephonyManager mTelephonyManager;
    private TransDataTask mTransDataTask;
    private TransType mTransType;
    private boolean mMobileDataRequest = false;
    private boolean mEnableSend = false;
    private boolean mLastRequest = false;
    private int mSendCount = 0;
    private boolean mFlag = true;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() { // from class: com.miui.warningcenter.policeassist.PaService.4
        @Override // android.telephony.PhoneStateListener
        public void onCallStateChanged(int i10, String str) {
            int callState = ((TelephonyManager) PaService.this.getSystemService(au.f23034d)).getCallState();
            if (PaService.this.mCurrentCallState == callState) {
                return;
            }
            if (callState == 0) {
                Log.d(PaService.TAG, "hang off phone");
                if (!PaUtils.getPoliceAssistToggle(PaService.this) && !PaService.this.isSendMessage) {
                    PaUtils.showPAGuideNotification(PaService.this);
                }
                if (PaService.this.mTransType == TransType.NETWORK) {
                    PaService.this.releaseViews();
                    PaService.this.unregisterForegroundInfoListener();
                    PaService.this.sendLastRequest();
                    PaService.this.stopServiceTimer();
                } else {
                    PaService.this.stopSelf();
                }
            }
            PaService.this.mCurrentCallState = callState;
        }
    };
    private IForegroundInfoListener.Stub listener = new IForegroundInfoListener.Stub() { // from class: com.miui.warningcenter.policeassist.PaService.6
        @Override // miui.process.IForegroundInfoListener
        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) {
            PaService.this.mForegroundPkgName = foregroundInfo.mForegroundPackageName;
            PaService paService = PaService.this;
            if (!paService.isInCallUI(paService.mForegroundPkgName)) {
                PaService.this.removeView();
                return;
            }
            Intent intent = new Intent(PaService.this, (Class<?>) PaService.class);
            intent.setAction(PaService.ACTION_SHOW_FLOAT_VIEW);
            PaService.this.startService(intent);
        }
    };

    private static class GetMyPhoneNumberTask extends AsyncTask<Void, Void, String> {
        private WeakReference<PaService> serviceRef;

        GetMyPhoneNumberTask(PaService paService) {
            this.serviceRef = new WeakReference<>(paService);
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public String doInBackground(Void... voidArr) {
            PaService paService = this.serviceRef.get();
            if (paService == null || isCancelled()) {
                return null;
            }
            return PaUtils.getMyPhoneNumber(paService);
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(String str) {
            TransType transType;
            super.onPostExecute(str);
            PaService paService = this.serviceRef.get();
            if (paService == null) {
                return;
            }
            paService.startLocation(paService.mCallNumber);
            if (PaUtils.isSimCardStateReady(paService)) {
                if (TextUtils.isEmpty(str)) {
                    transType = TransType.SMS;
                    paService.mTransType = transType;
                } else {
                    transType = TransType.NETWORK;
                    paService.mTransType = transType;
                    paService.startRequestLoop();
                }
                AnalyticHelper.trackPaSendStatus(transType.name(), paService.mCallNumber);
            }
        }
    }

    private static class TransDataTask extends AsyncTask<Void, Void, Boolean> {
        private boolean lastRequest;
        private double latitude;
        private Location location;
        private double longitude;
        private WeakReference<PaService> serviceRef;
        private String type;

        TransDataTask(PaService paService, String str, double d10, double d11, boolean z10, Location location) {
            this.serviceRef = new WeakReference<>(paService);
            this.type = str;
            this.latitude = d10;
            this.longitude = d11;
            this.lastRequest = z10;
            this.location = location;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Code duplicated, block: B:35:0x00fa  */
        @Override // android.os.AsyncTask
        public Boolean doInBackground(Void... voidArr) {
            PaService paService = this.serviceRef.get();
            if (paService == null || isCancelled()) {
                return Boolean.FALSE;
            }
            try {
                try {
                    List<Address> fromLocation = new Geocoder(Application.A(), Locale.getDefault()).getFromLocation(this.latitude, this.longitude, 1);
                    if (fromLocation != null && fromLocation.size() > 0) {
                        int i10 = 0;
                        if (fromLocation.get(0) != null) {
                            String addressLine = fromLocation.get(0).getAddressLine(0);
                            MsgObservable.getInstance().sendMessage(1, addressLine);
                            String myPhoneNumber = PaUtils.getMyPhoneNumber(paService);
                            PhoneNumberUtils.PhoneNumber phoneNumber = PhoneNumberUtils.PhoneNumber.parse(paService.mCallNumber);
                            String effectiveNumber = phoneNumber.getEffectiveNumber();
                            String areaCode = phoneNumber.getAreaCode();
                            if (!TextUtils.isEmpty(areaCode) && PaUtils.isNumeric(areaCode)) {
                                i10 = Integer.parseInt(areaCode);
                            }
                            int i11 = i10;
                            if (!PaUtils.isSimCardStateReady(paService)) {
                                Log.i(PaService.TAG, "SIM card not ready, post message");
                                PaUtils.postMessage("", effectiveNumber, String.valueOf(i11), paService.mCallTime, this.type, this.latitude, this.longitude, addressLine, paService.mStartLocationTime, paService.mFinishLocationTime);
                            } else if (TextUtils.isEmpty(myPhoneNumber)) {
                                PaUtils.sendMessage(paService, Integer.parseInt(effectiveNumber), i11, this.latitude, this.longitude, paService.mCallTime);
                            } else {
                                PaUtils.postMessage(myPhoneNumber, effectiveNumber, String.valueOf(i11), paService.mCallTime, this.type, this.latitude, this.longitude, addressLine, paService.mStartLocationTime, paService.mFinishLocationTime);
                                EPSManager.postEPS(this.location, myPhoneNumber, effectiveNumber, areaCode, paService.mCallTime, addressLine);
                            }
                        }
                    }
                    if (this.lastRequest) {
                        Log.d(PaService.TAG, "send last request");
                        MsgObservable.getInstance().sendEmptyMessage(5);
                    }
                } catch (Exception e10) {
                    Log.e(PaService.TAG, "getAddress exp ", e10);
                    if (this.lastRequest) {
                        Log.d(PaService.TAG, "send last request");
                        MsgObservable.getInstance().sendEmptyMessage(5);
                    }
                }
                return Boolean.TRUE;
            } catch (Throwable th2) {
                if (this.lastRequest) {
                    Log.d(PaService.TAG, "send last request");
                    MsgObservable.getInstance().sendEmptyMessage(5);
                }
                throw th2;
            }
        }
    }

    private enum TransType {
        SMS,
        NETWORK
    }

    static /* synthetic */ int access$308(PaService paService) {
        int i10 = paService.mSendCount;
        paService.mSendCount = i10 + 1;
        return i10;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isInCallUI(String str) {
        return PACKAGE_INCALL_UI.equals(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLocationCallback(Location location, boolean z10) {
        if (this.mEnableSend) {
            return;
        }
        this.mEnableSend = true;
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String provider = location.getProvider();
        this.mFinishLocationTime = System.currentTimeMillis();
        TransDataTask transDataTask = new TransDataTask(this, provider, latitude, longitude, z10, location);
        this.mTransDataTask = transDataTask;
        transDataTask.execute(new Void[0]);
    }

    private void registerProcessManager() {
        try {
            f.h(Class.forName(CLASS_PROCESS_MANAGER), METHOD_REGISTER_FOREGROUND_LISTENER, new Class[]{IForegroundInfoListener.class}, this.listener);
            Log.i(TAG, "registerProcessManager");
        } catch (Exception e10) {
            Log.e(TAG, e10.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendLastRequest() {
        startLocation(this.mCallNumber, true);
    }

    private void showFloatingView() {
        NotificationUtil.setGpsStatus(this);
        if (w1.a()) {
            Log.i(TAG, "showFloatingView: skip when in satellite mode!!!");
        } else {
            addView();
        }
    }

    private void startLocationAndSendMessage() {
        if (TextUtils.isEmpty(this.mCallNumber)) {
            return;
        }
        GetMyPhoneNumberTask getMyPhoneNumberTask = new GetMyPhoneNumberTask(this);
        this.mGetMyPhoneNumberTask = getMyPhoneNumberTask;
        getMyPhoneNumberTask.execute(new Void[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startRequestLoop() {
        if (this.mCountdownTimer == null) {
            this.mCountdownTimer = new CountDownTimer(60000L, 1000L) { // from class: com.miui.warningcenter.policeassist.PaService.1
                @Override // android.os.CountDownTimer
                public void onFinish() {
                    PaService.access$308(PaService.this);
                    if (PaService.this.mSendCount >= 10) {
                        if (PaService.this.mCountdownTimer != null) {
                            PaService.this.mCountdownTimer.cancel();
                            PaService.this.mCountdownTimer = null;
                        }
                        PaService.this.stopSelf();
                        return;
                    }
                    if (!PaService.this.isTelephonyCalling()) {
                        if (PaService.this.mCountdownTimer != null) {
                            PaService.this.mCountdownTimer.cancel();
                            PaService.this.mCountdownTimer = null;
                        }
                        PaService.this.stopSelf();
                        return;
                    }
                    Log.d(PaService.TAG, "loop for quest");
                    if (!TextUtils.isEmpty(PaService.this.mCallNumber)) {
                        PaService paService = PaService.this;
                        paService.startLocation(paService.mCallNumber);
                    }
                    PaService.this.mCountdownTimer.start();
                }

                @Override // android.os.CountDownTimer
                public void onTick(long j10) {
                }
            };
        }
        if (this.mCounterStart) {
            return;
        }
        this.mCounterStart = true;
        this.mCountdownTimer.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopServiceTimer() {
        if (this.mStopServiceTimer == null) {
            this.mStopServiceTimer = new CountDownTimer(C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS, 1000L) { // from class: com.miui.warningcenter.policeassist.PaService.5
                @Override // android.os.CountDownTimer
                public void onFinish() {
                    PaService.this.stopSelf();
                }

                @Override // android.os.CountDownTimer
                public void onTick(long j10) {
                }
            };
        }
        this.mStopServiceTimer.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterForegroundInfoListener() {
        try {
            f.h(Class.forName(CLASS_PROCESS_MANAGER), METHOD_UNREGISTER_FOREGROUND_LISTENER, new Class[]{IForegroundInfoListener.class}, this.listener);
            Log.i(TAG, "unRegisterForegroundInfoListener");
        } catch (Exception e10) {
            Log.e(TAG, e10.toString());
        }
    }

    public void addView() {
        this.mFloatWindowManager.addView();
    }

    public boolean isTelephonyCalling() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(au.f23034d);
        return 1 == telephonyManager.getCallState() || 2 == telephonyManager.getCallState();
    }

    @Override // android.app.Service
    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        registerProcessManager();
        MsgObservable.getInstance().addObserver(this);
        if (this.mFloatWindowManager == null) {
            this.mFloatWindowManager = new PaFloatWindowManager(this);
        }
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(au.f23034d);
        this.mTelephonyManager = telephonyManager;
        telephonyManager.listen(this.mPhoneStateListener, 32);
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        LocationRecordManager.getInstance().stopLocation();
        GetMyPhoneNumberTask getMyPhoneNumberTask = this.mGetMyPhoneNumberTask;
        if (getMyPhoneNumberTask != null) {
            getMyPhoneNumberTask.cancel(true);
            this.mGetMyPhoneNumberTask = null;
        }
        TransDataTask transDataTask = this.mTransDataTask;
        if (transDataTask != null) {
            transDataTask.cancel(true);
            this.mTransDataTask = null;
        }
        CountDownTimer countDownTimer = this.mCountdownTimer;
        if (countDownTimer != null) {
            countDownTimer.cancel();
            this.mCountdownTimer = null;
        }
        CountDownTimer countDownTimer2 = this.mStopServiceTimer;
        if (countDownTimer2 != null) {
            countDownTimer2.cancel();
            this.mStopServiceTimer = null;
        }
        CountDownTimer countDownTimer3 = this.mLocationTimer;
        if (countDownTimer3 != null) {
            countDownTimer3.cancel();
            this.mLocationTimer = null;
        }
        NotificationUtil.resetGPS(this);
        if (this.mMobileDataRequest && !this.mMobileDataEnable) {
            Log.d(TAG, "reset mobile enable");
            NetworkUtils.setMobileDataState(this, false);
        }
        releaseViews();
        unregisterForegroundInfoListener();
        MsgObservable.getInstance().deleteObserver(this);
        this.mFloatWindowManager = null;
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i10, int i11) {
        if (intent == null) {
            return super.onStartCommand(intent, i10, i11);
        }
        String action = intent.getAction();
        if (!ACTION_PA_INIT.equals(action)) {
            if (ACTION_SEND_MESSAGE.equals(action)) {
                startLocationAndSendMessage();
            } else if (!ACTION_SHOW_FLOAT_VIEW.equals(action)) {
                if (ACTION_SEND_MESSAGE_DONE.equals(action)) {
                    Log.d(TAG, "already send sms");
                    MsgObservable.getInstance().sendEmptyMessage(3);
                }
            }
            this.mCurrentCallState = this.mTelephonyManager.getCallState();
            return super.onStartCommand(intent, i10, i11);
        }
        Bundle extras = intent.getExtras();
        this.mCallNumber = extras.getString("CallNumber");
        this.mCallTime = extras.getLong("CallTime");
        showFloatingView();
        this.mCurrentCallState = this.mTelephonyManager.getCallState();
        return super.onStartCommand(intent, i10, i11);
    }

    public void releaseViews() {
        removeView();
        this.mFloatWindowManager.releaseViews();
    }

    public void removeView() {
        this.mFloatWindowManager.removeView();
    }

    public void startLocation(String str) {
        startLocation(str, false);
    }

    public void startLocation(String str, boolean z10) {
        if (z10) {
            this.mLastRequest = true;
            CountDownTimer countDownTimer = this.mCountdownTimer;
            if (countDownTimer != null) {
                countDownTimer.cancel();
                this.mCountdownTimer = null;
            }
        }
        this.mEnableSend = false;
        this.mStartLocationTime = System.currentTimeMillis();
        if (!this.mMobileDataRequest) {
            this.mMobileDataRequest = true;
            boolean zC = d.c(this);
            this.mMobileDataEnable = zC;
            if (!zC) {
                NetworkUtils.setMobileDataState(this, true);
            }
        }
        Log.d(TAG, "startLocation:lastRequest=" + z10);
        if (this.mFlag) {
            this.mFlag = false;
            LocationRecordManager.getInstance().startLocation(true, new LocationRecordManager.CallBack() { // from class: com.miui.warningcenter.policeassist.PaService.2
                @Override // com.miui.earthquakewarning.utils.LocationRecordManager.CallBack
                public void onLocationFailed(String str2) {
                    Log.d(PaService.TAG, "onLocationFailed");
                    MsgObservable.getInstance().sendEmptyMessage(2);
                    LocationRecordManager.getInstance().stopLocation();
                    MsgObservable.getInstance().sendEmptyMessage(5);
                }

                @Override // com.miui.earthquakewarning.utils.LocationRecordManager.CallBack
                public void onLocationSuccess(Location location) {
                    Log.d(PaService.TAG, "onLocationSuccess");
                    PaService paService = PaService.this;
                    paService.onLocationCallback(location, paService.mLastRequest);
                    LocationRecordManager.getInstance().stopLocation();
                }
            });
            if (this.mLocationTimer == null) {
                this.mLocationTimer = new CountDownTimer(10000L, 1000L) { // from class: com.miui.warningcenter.policeassist.PaService.3
                    @Override // android.os.CountDownTimer
                    public void onFinish() {
                        PaService.this.mFlag = true;
                        LocationRecordManager.getInstance().stopLocation();
                    }

                    @Override // android.os.CountDownTimer
                    public void onTick(long j10) {
                    }
                };
            }
            this.mLocationTimer.start();
        }
    }

    @Override // java.util.Observer
    public void update(Observable observable, Object obj) {
        if (obj instanceof Message) {
            int i10 = ((Message) obj).what;
            if (i10 == 5) {
                stopSelf();
            } else if (i10 == 3) {
                this.isSendMessage = true;
            }
        }
    }
}
