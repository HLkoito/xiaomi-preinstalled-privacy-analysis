package com.miui.earthquakewarning.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.miui.earthquakewarning.EarthquakeWarningManager;
import com.miui.earthquakewarning.model.EwTranData;
import com.miui.earthquakewarning.model.SaveAreaResult;
import com.miui.earthquakewarning.utils.LocationRecordManager;
import com.miui.earthquakewarning.utils.NotificationUtil;
import com.miui.earthquakewarning.utils.Utils;
import com.miui.networkassistant.config.Constants;
import com.xiaomi.mipush.sdk.n;
import com.xiaomi.onetrack.api.au;
import ef.w;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import x4.v;
import xb.e;

/* JADX INFO: loaded from: classes2.dex */
public class EarthquakeWarningService extends Service implements SensorEventListener {
    public static final String ACTION_EXIT_EARTHQUAKEWARNING = "action_exit_earthquakewarning";
    public static final String ACTION_EXIT_VOLUNTEER = "action_exit_volunteer";
    public static final String ACTION_JOIN_VOLUNTEER = "action_join_volunteer";
    public static final String ACTION_RESTORE_DATA = "action_restore_data";
    public static final String ACTION_SUBSCRIBE_AREA = "action_subscribe_area";
    public static final String ACTION_UNSUBSCRIBE_AREA = "action_unsubscribe_area";
    public static final String EXTRA_AREA_CITY = "extra_area_city";
    public static final String EXTRA_AREA_CODE = "extra_area_code";
    public static final String EXTRA_AREA_COUNTRIES = "extra_area_countries";
    public static final String EXTRA_AREA_PROVINCE = "extra_area_province";
    public static final String EXTRA_AREA_REGION = "extra_area_region";
    public static final String EXTRA_AREA_SUBSCRIBELEVEL = "extra_area_subscribe_level";
    public static final String EXTRA_AREA_SUPPORT = "extra_area_support";
    private static final String TAG = "EwService";
    private int mCurrentCallState;
    private boolean mPlaying;
    private SensorManager mSensorManager;
    private TelephonyManager mTelephonyManager;
    private boolean mFlag = true;
    private boolean registered = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.miui.earthquakewarning.service.EarthquakeWarningService.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Log.i(EarthquakeWarningService.TAG, "onReceive: " + intent.getAction());
            if (Constants.System.ACTION_SCREEN_ON.equals(intent.getAction())) {
                if (!Utils.isEarthquakeWarningOpen() && !Utils.isEarthquakeMonitorOpen()) {
                    EarthquakeWarningService.this.stopSelf();
                    return;
                }
                if (Utils.isEarthquakeWarningOpen()) {
                    long uploadTopicTime = Utils.getUploadTopicTime();
                    long jCurrentTimeMillis = System.currentTimeMillis();
                    if (jCurrentTimeMillis - uploadTopicTime > 21600000) {
                        UpdateAreaCodeManager.getInstance().uploadSettings(context);
                        Utils.setUploadTopicTime(jCurrentTimeMillis);
                    }
                }
                if (Utils.isNeedRestore()) {
                    RestoreHelper.restore(context);
                    Utils.needRestore(false);
                }
            }
        }
    };
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() { // from class: com.miui.earthquakewarning.service.EarthquakeWarningService.2
        @Override // android.telephony.PhoneStateListener
        public void onCallStateChanged(int i10, String str) {
            int callState = ((TelephonyManager) EarthquakeWarningService.this.getSystemService(au.f23034d)).getCallState();
            if (EarthquakeWarningService.this.mCurrentCallState == callState) {
                return;
            }
            if (EarthquakeWarningService.this.mPlaying) {
                EarthquakeWarningService earthquakeWarningService = EarthquakeWarningService.this;
                if (callState != 0) {
                    NotificationUtil.muteVolume(earthquakeWarningService);
                } else {
                    NotificationUtil.remuteVolume(earthquakeWarningService);
                }
            }
            EarthquakeWarningService.this.mCurrentCallState = callState;
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    public void addLogData(String str) {
    }

    private void exitEarthquakeWarningService() {
        if (!Utils.isEarthquakeMonitorOpen()) {
            stopSelf();
        } else {
            unRegisterMyReceiver();
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
    }

    private void getLocation(float[] fArr) {
        EwTranData ewTranData = new EwTranData();
        float fG = r4.a.g(com.miui.earthquakewarning.Constants.PREFERENCE_KEY_EARTHQUAKE_WARNING_MONITOR_LOCATION_LAT, 0.0f);
        float fG2 = r4.a.g(com.miui.earthquakewarning.Constants.PREFERENCE_KEY_EARTHQUAKE_WARNING_MONITOR_LOCATION_LNG, 0.0f);
        ewTranData.lat = fG;
        ewTranData.lng = fG2;
        ewTranData.time = System.currentTimeMillis();
        ewTranData.f13111id = w.a(this);
        ewTranData.arrays = fArr;
        EarthquakeMonitorDataManager.getInstance().sendData(ewTranData);
        long j10 = r4.a.j("LOCATION_TIME", 0L);
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (this.mFlag) {
            if (jCurrentTimeMillis - j10 > 600000 || Float.compare(fG, 0.0f) == 0) {
                this.mFlag = false;
                addLogData("getLocation start = " + System.currentTimeMillis());
                addLogData("is network connect = " + e.c(this) + " ,time = " + System.currentTimeMillis());
                r4.a.q("LOCATION_TIME", System.currentTimeMillis());
                startLocation();
            }
        }
    }

    private void initSensor() {
        SensorManager sensorManager = (SensorManager) getSystemService("sensor");
        this.mSensorManager = sensorManager;
        Sensor defaultSensor = sensorManager.getDefaultSensor(com.miui.earthquakewarning.Constants.EQM_SENSOR_ID, true);
        if (defaultSensor != null) {
            this.mSensorManager.registerListener(this, defaultSensor, 0);
        }
        startLocation();
    }

    private void manageData(SaveAreaResult saveAreaResult, int i10) {
        new ManageSubscribeDataTask(this, i10, saveAreaResult).execute(new String[0]);
    }

    private void registerMyReceiver() {
        if (this.registered) {
            return;
        }
        this.registered = true;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.System.ACTION_SCREEN_ON);
        v.m(this, this.mReceiver, intentFilter, 4);
    }

    private void startLocation() {
        EarthquakeMonitorDataManager.getInstance().startLocationSingleUpdate(new LocationRecordManager.CallBack() { // from class: com.miui.earthquakewarning.service.EarthquakeWarningService.3
            @Override // com.miui.earthquakewarning.utils.LocationRecordManager.CallBack
            public void onLocationFailed(String str) {
                EarthquakeWarningService.this.addLogData("getLocation time = " + System.currentTimeMillis() + ", location failed");
            }

            /* JADX WARN: Code duplicated, block: B:8:0x0028 A[Catch: Exception -> 0x002f, TRY_LEAVE, TryCatch #0 {Exception -> 0x002f, blocks: (B:3:0x000d, B:5:0x001a, B:7:0x0021, B:8:0x0028), top: B:15:0x000d }] */
            @Override // com.miui.earthquakewarning.utils.LocationRecordManager.CallBack
            public void onLocationSuccess(Location location) {
                EwTranData ewTranData = new EwTranData();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                try {
                    double[] dArrB = xi.a.b(latitude, longitude);
                    double d10 = dArrB[0];
                    if (d10 != 0.0d) {
                        double d11 = dArrB[1];
                        if (d11 != 0.0d) {
                            ewTranData.lat = (float) d10;
                            ewTranData.lng = (float) d11;
                        } else {
                            ewTranData.lat = (float) latitude;
                            ewTranData.lng = (float) longitude;
                        }
                    } else {
                        ewTranData.lat = (float) latitude;
                        ewTranData.lng = (float) longitude;
                    }
                } catch (Exception e10) {
                    Log.e(EarthquakeWarningService.TAG, "get diff privacy error", e10);
                    ewTranData.lat = (float) latitude;
                    ewTranData.lng = (float) longitude;
                }
                r4.a.o(com.miui.earthquakewarning.Constants.PREFERENCE_KEY_EARTHQUAKE_WARNING_MONITOR_LOCATION_LAT, ewTranData.lat);
                r4.a.o(com.miui.earthquakewarning.Constants.PREFERENCE_KEY_EARTHQUAKE_WARNING_MONITOR_LOCATION_LNG, ewTranData.lng);
                EarthquakeMonitorDataManager.getInstance().stopLocation();
                EarthquakeWarningService.this.addLogData("getLocation time = " + System.currentTimeMillis() + ", get location already,Latitude =" + location.getLatitude());
            }
        });
        new CountDownTimer(10000L, 1000L) { // from class: com.miui.earthquakewarning.service.EarthquakeWarningService.4
            @Override // android.os.CountDownTimer
            public void onFinish() {
                EarthquakeWarningService.this.mFlag = true;
                EarthquakeWarningService.this.addLogData("getLocation CountDownTimer finish = " + System.currentTimeMillis());
                EarthquakeMonitorDataManager.getInstance().stopLocation();
            }

            @Override // android.os.CountDownTimer
            public void onTick(long j10) {
            }
        }.start();
    }

    private void unRegisterMyReceiver() {
        if (this.registered) {
            unregisterReceiver(this.mReceiver);
            this.registered = false;
        }
    }

    @Override // android.app.Service
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("device regId:" + n.C(this));
        printWriter.println("device all topics:" + com.miui.push.a.b(this).a());
        printWriter.println("device earthquake warning switch:" + Utils.isEarthquakeWarningOpen());
        printWriter.println("device notification switch:" + Utils.isLowEarthquakeWarningOpen());
        printWriter.println("device eqm support:" + Utils.showEqmEntry(this));
        printWriter.println("device eqm switch:" + Utils.isEarthquakeMonitorOpen());
        printWriter.println("device defined threshold:" + Utils.getSelectIntensity());
    }

    public void exitVolunteer() {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        EarthquakeMonitorDataManager.getInstance().release();
        if (Utils.isEarthquakeWarningOpen()) {
            return;
        }
        stopSelf();
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int i10) {
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        registerMyReceiver();
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(au.f23034d);
        this.mTelephonyManager = telephonyManager;
        telephonyManager.listen(this.mPhoneStateListener, 32);
        Log.i(TAG, "EarthquakeWarningService created");
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        unRegisterMyReceiver();
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        EarthquakeMonitorDataManager.getInstance().release();
        Log.w(TAG, "EarthquakeWarningService destroyed");
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent sensorEvent) {
        float[] fArr = (float[]) sensorEvent.values.clone();
        if (fArr.length <= 0) {
            addLogData("eqw_onSensorChanged time = " + System.currentTimeMillis() + ", values = null");
            return;
        }
        Log.d(TAG, "receive data: " + Arrays.toString(fArr));
        addLogData("eqw_onSensorChanged time = " + System.currentTimeMillis() + ", values = " + Arrays.toString(fArr));
        EarthquakeMonitorUtils.exitSleepMode(this);
        getLocation(fArr);
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i10, int i11) {
        Log.i(TAG, "EarthquakeWarningService onStartCommand");
        if (intent == null || TextUtils.isEmpty(intent.getAction())) {
            if (Utils.isNeedRestore()) {
                RestoreHelper.restore(this);
                Utils.needRestore(false);
            }
            registerMyReceiver();
        } else {
            if ("updatePlayingStatus".equals(intent.getAction())) {
                setPlaying(intent.getBooleanExtra("playing", false));
            }
            if ("action_subscribe_area".equals(intent.getAction())) {
                String stringExtra = intent.getStringExtra("extra_area_code");
                String stringExtra2 = intent.getStringExtra("extra_area_region");
                int intExtra = intent.getIntExtra(EXTRA_AREA_SUPPORT, 1);
                String stringExtra3 = intent.getStringExtra(EXTRA_AREA_COUNTRIES);
                EarthquakeWarningManager.getInstance().setTopicForPush(this, String.valueOf(stringExtra));
                SaveAreaResult saveAreaResult = new SaveAreaResult();
                saveAreaResult.setCode(stringExtra);
                saveAreaResult.setCity(stringExtra2);
                saveAreaResult.setSupport(intExtra);
                saveAreaResult.setCounties(stringExtra3);
                manageData(saveAreaResult, 1);
            }
            if (ACTION_UNSUBSCRIBE_AREA.equals(intent.getAction())) {
                String stringExtra4 = intent.getStringExtra("extra_area_code");
                if (!String.valueOf(Utils.getPreviousAreaCode()).equals(stringExtra4)) {
                    EarthquakeWarningManager.getInstance().unsetTopicForPush(this, String.valueOf(stringExtra4));
                }
                SaveAreaResult saveAreaResult2 = new SaveAreaResult();
                saveAreaResult2.setCode(stringExtra4);
                manageData(saveAreaResult2, 2);
            }
            if (ACTION_RESTORE_DATA.equals(intent.getAction()) && Utils.isNeedRestore()) {
                RestoreHelper.restore(this);
                Utils.needRestore(false);
            }
            if (ACTION_JOIN_VOLUNTEER.equals(intent.getAction())) {
                initSensor();
            }
            if (ACTION_EXIT_VOLUNTEER.equals(intent.getAction())) {
                exitVolunteer();
            }
            if (ACTION_EXIT_EARTHQUAKEWARNING.equals(intent.getAction())) {
                exitEarthquakeWarningService();
            }
            this.mCurrentCallState = this.mTelephonyManager.getCallState();
        }
        return super.onStartCommand(intent, i10, i11);
    }

    public void setPlaying(boolean z10) {
        this.mPlaying = z10;
    }
}
