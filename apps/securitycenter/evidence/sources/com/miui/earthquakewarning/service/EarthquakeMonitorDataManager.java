package com.miui.earthquakewarning.service;

import android.annotation.SuppressLint;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import com.milink.sdk.cast.MiLinkDevice;
import com.miui.earthquakewarning.Constants;
import com.miui.earthquakewarning.model.EwTranData;
import com.miui.earthquakewarning.utils.LocationRecordManager;
import com.miui.earthquakewarning.utils.Utils;
import com.miui.securitycenter.Application;
import java.util.Arrays;
import java.util.HashMap;
import org.json.JSONArray;
import x4.v1;

/* JADX INFO: loaded from: classes2.dex */
public class EarthquakeMonitorDataManager {
    private static final String TAG = "EwService";
    private static volatile EarthquakeMonitorDataManager sInstance;
    private LocationListener mGpsListener;
    private LocationManager mLocationManager;
    private LocationListener mNetworkListener;

    private EarthquakeMonitorDataManager() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addLogData(String str) {
    }

    public static EarthquakeMonitorDataManager getInstance() {
        if (sInstance == null) {
            synchronized (EarthquakeMonitorDataManager.class) {
                if (sInstance == null) {
                    sInstance = new EarthquakeMonitorDataManager();
                }
            }
        }
        return sInstance;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void threadPoolRun(EwTranData ewTranData) throws Throwable {
        try {
            addLogData("udp_send_data  receive data time = " + System.currentTimeMillis() + ", values = " + Arrays.toString(ewTranData.arrays));
            HashMap map = new HashMap();
            map.put("oaid", ewTranData.f13111id);
            map.put("lng", String.valueOf(ewTranData.lng));
            map.put("lat", String.valueOf(ewTranData.lat));
            map.put("timestamp", String.valueOf(ewTranData.time));
            String strC = v1.c("ro.product.device", MiLinkDevice.TYPE_UNKNOWN);
            String strC2 = v1.c("ro.miui.ui.version.name", MiLinkDevice.TYPE_UNKNOWN);
            map.put("model", strC);
            map.put("rom", strC2);
            String earthquakeVolunteerToken = Utils.getEarthquakeVolunteerToken();
            if (!TextUtils.isEmpty(earthquakeVolunteerToken)) {
                map.put("token", earthquakeVolunteerToken);
            }
            JSONArray jSONArray = new JSONArray();
            int i10 = 0;
            while (true) {
                float[] fArr = ewTranData.arrays;
                if (i10 >= fArr.length) {
                    map.put("data", jSONArray.toString());
                    addLogData("udp_send_data time = " + System.currentTimeMillis() + "result = " + EarthquakeMonitorUtils.uploadData(Constants.REQUEST_MONITOR_DATA_UPLOAD_URL, map) + ", values = " + Arrays.toString(ewTranData.arrays));
                    return;
                }
                jSONArray.put(fArr[i10]);
                i10++;
            }
        } catch (Exception e10) {
            addLogData("udp_send_data time = " + System.currentTimeMillis() + ", error = " + e10.toString());
            e10.printStackTrace();
        }
    }

    public void release() {
        stopLocation();
    }

    public void sendData(final EwTranData ewTranData) {
        Log.d("DataManager", "send data: " + Arrays.toString(ewTranData.arrays));
        addLogData("send data2 time = " + System.currentTimeMillis() + ", send start ");
        try {
            EWThreadPool.execute(new Runnable() { // from class: com.miui.earthquakewarning.service.EarthquakeMonitorDataManager.1
                @Override // java.lang.Runnable
                public void run() throws Throwable {
                    EarthquakeMonitorDataManager.this.addLogData("send data2 time = " + System.currentTimeMillis() + ", send before ");
                    EarthquakeMonitorDataManager.this.threadPoolRun(ewTranData);
                }
            });
        } catch (Exception e10) {
            addLogData("send data2 time = " + System.currentTimeMillis() + ", error = " + e10.toString());
            StringBuilder sb2 = new StringBuilder();
            sb2.append("send data error:");
            sb2.append(e10.toString());
            Log.e(TAG, sb2.toString());
        }
    }

    public void sendGeomData(final EwTranData ewTranData) {
        addLogData("send data3 time = " + System.currentTimeMillis() + ", send start ");
        try {
            EWThreadPool.execute(new Runnable() { // from class: com.miui.earthquakewarning.service.EarthquakeMonitorDataManager.2
                @Override // java.lang.Runnable
                public void run() throws Throwable {
                    EarthquakeMonitorDataManager.this.addLogData("send data3 time = " + System.currentTimeMillis() + ", send before ");
                    EarthquakeMonitorDataManager.this.threadPoolRun(ewTranData);
                }
            });
        } catch (Exception e10) {
            addLogData("send data3 time = " + System.currentTimeMillis() + ", error = " + e10.toString());
            StringBuilder sb2 = new StringBuilder();
            sb2.append("send data error:");
            sb2.append(e10.toString());
            Log.e(TAG, sb2.toString());
        }
    }

    @SuppressLint({"MissingPermission"})
    public void startLocationSingleUpdate(final LocationRecordManager.CallBack callBack) {
        if (Application.A() == null) {
            return;
        }
        if (this.mLocationManager == null) {
            this.mLocationManager = (LocationManager) Application.A().getSystemService("location");
        }
        Criteria criteria = new Criteria();
        criteria.setAccuracy(1);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(1);
        criteria.setSpeedRequired(false);
        new HandlerThread("EarthMonitorLocation").start();
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() { // from class: com.miui.earthquakewarning.service.a
            @Override // java.lang.Runnable
            public final void run() {
                this.f13112a.stopLocation();
            }
        };
        handler.postDelayed(runnable, 10000L);
        if (this.mGpsListener == null) {
            this.mGpsListener = new LocationListener() { // from class: com.miui.earthquakewarning.service.EarthquakeMonitorDataManager.3
                @Override // android.location.LocationListener
                public void onLocationChanged(Location location) {
                    EarthquakeMonitorDataManager.this.stopLocation();
                    handler.removeCallbacks(runnable);
                    LocationRecordManager.CallBack callBack2 = callBack;
                    if (callBack2 != null) {
                        callBack2.onLocationSuccess(location);
                    }
                }

                @Override // android.location.LocationListener
                public void onProviderDisabled(String str) {
                }

                @Override // android.location.LocationListener
                public void onProviderEnabled(String str) {
                }

                @Override // android.location.LocationListener
                public void onStatusChanged(String str, int i10, Bundle bundle) {
                }
            };
        }
        if (this.mNetworkListener == null) {
            this.mNetworkListener = new LocationListener() { // from class: com.miui.earthquakewarning.service.EarthquakeMonitorDataManager.4
                @Override // android.location.LocationListener
                public void onLocationChanged(Location location) {
                    EarthquakeMonitorDataManager.this.stopLocation();
                    handler.removeCallbacks(runnable);
                    LocationRecordManager.CallBack callBack2 = callBack;
                    if (callBack2 != null) {
                        callBack2.onLocationSuccess(location);
                    }
                }

                @Override // android.location.LocationListener
                public void onProviderDisabled(String str) {
                }

                @Override // android.location.LocationListener
                public void onProviderEnabled(String str) {
                }

                @Override // android.location.LocationListener
                public void onStatusChanged(String str, int i10, Bundle bundle) {
                }
            };
        }
        if (this.mLocationManager.isProviderEnabled("gps")) {
            this.mLocationManager.requestLocationUpdates("gps", 2500L, 0.0f, this.mGpsListener);
        }
        if (this.mLocationManager.isProviderEnabled("network")) {
            this.mLocationManager.requestLocationUpdates("network", 2500L, 0.0f, this.mNetworkListener);
        }
    }

    public void stopLocation() {
        LocationManager locationManager;
        LocationManager locationManager2;
        LocationListener locationListener = this.mGpsListener;
        if (locationListener != null && (locationManager2 = this.mLocationManager) != null) {
            locationManager2.removeUpdates(locationListener);
        }
        LocationListener locationListener2 = this.mNetworkListener;
        if (locationListener2 != null && (locationManager = this.mLocationManager) != null) {
            locationManager.removeUpdates(locationListener2);
        }
        this.mLocationManager = null;
    }
}
