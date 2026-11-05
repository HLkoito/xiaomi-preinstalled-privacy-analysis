package com.miui.earthquakewarning.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import cl.e;
import com.google.common.net.HttpHeaders;
import com.miui.earthquakewarning.Constants;
import ef.x;
import eg.j;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/* JADX INFO: loaded from: classes2.dex */
public class EarthquakeMonitorUtils {
    private static final String ACTION_REQUEST_WAKE = "com.miui.powerkeeper_request_wake";
    private static final String EXTRA_REASON = "reason";
    private static final String PACKAGE_NAME = "com.miui.powerkeeper";
    private static final int REASON_DESK_CLOCK = 1;
    private static final String SP_EXIT_SLEEP_MODE_TIME = "EXIT_SLEEP_MODE_TIME";
    private static final String TAG = "EarthquakeMonitorUtils";
    private static final int TIME_OUT_MILLS = 18000;

    private EarthquakeMonitorUtils() {
    }

    private static void addLogData(String str) {
    }

    public static void exitSleepMode(Context context) {
        if (System.currentTimeMillis() - r4.a.j(SP_EXIT_SLEEP_MODE_TIME, 0L) < 600000) {
            return;
        }
        try {
            r4.a.q(SP_EXIT_SLEEP_MODE_TIME, System.currentTimeMillis());
            Intent intent = new Intent(ACTION_REQUEST_WAKE);
            intent.putExtra(EXTRA_REASON, 1);
            intent.setPackage(PACKAGE_NAME);
            context.sendBroadcast(intent);
            Log.i(TAG, "request exit sleep mode");
            addLogData("exitSleepMode time=" + System.currentTimeMillis());
        } catch (Exception e10) {
            Log.e(TAG, "exitSleepMode error: " + e10.getMessage());
            addLogData("exitSleepMode error time=" + System.currentTimeMillis());
        }
    }

    public static String uploadData(String str, Map<String, String> map) throws Throwable {
        InputStreamReader inputStreamReader;
        InputStream inputStream;
        BufferedReader bufferedReader;
        if (!x.z()) {
            return "";
        }
        StringBuilder sb2 = new StringBuilder();
        DataOutputStream dataOutputStream = null;
        try {
            Map<String, String> mapB = j.b(map, Constants.UUID_MONITOR_DATA_UPLOAD);
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.addRequestProperty(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
            httpURLConnection.addRequestProperty("Connection", HttpHeaders.KEEP_ALIVE);
            byte[] bytes = (mapB == null || mapB.size() <= 0) ? null : j.h(mapB).getBytes();
            httpURLConnection.setConnectTimeout(TIME_OUT_MILLS);
            httpURLConnection.setReadTimeout(TIME_OUT_MILLS);
            DataOutputStream dataOutputStream2 = new DataOutputStream(httpURLConnection.getOutputStream());
            try {
                dataOutputStream2.write(bytes);
                if (httpURLConnection.getResponseCode() >= 300) {
                    throw new IOException("HTTP Request is not success, Response code is " + httpURLConnection.getResponseCode());
                }
                inputStream = httpURLConnection.getInputStream();
                try {
                    inputStreamReader = new InputStreamReader(inputStream);
                    try {
                        bufferedReader = new BufferedReader(inputStreamReader);
                        while (true) {
                            try {
                                String line = bufferedReader.readLine();
                                if (line == null) {
                                    break;
                                }
                                sb2.append(line);
                            } catch (Exception e10) {
                                e = e10;
                                dataOutputStream = dataOutputStream2;
                                try {
                                    Log.e(TAG, "uploadData exception: ", e);
                                    e.c(dataOutputStream);
                                    e.d(bufferedReader);
                                    e.d(inputStreamReader);
                                    e.b(inputStream);
                                } catch (Throwable th2) {
                                    th = th2;
                                    e.c(dataOutputStream);
                                    e.d(bufferedReader);
                                    e.d(inputStreamReader);
                                    e.b(inputStream);
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                dataOutputStream = dataOutputStream2;
                                inputStream = inputStream;
                                th = th;
                                e.c(dataOutputStream);
                                e.d(bufferedReader);
                                e.d(inputStreamReader);
                                e.b(inputStream);
                                throw th;
                            }
                        }
                        e.c(dataOutputStream2);
                        e.d(bufferedReader);
                        e.d(inputStreamReader);
                        e.b(inputStream);
                    } catch (Exception e11) {
                        e = e11;
                        bufferedReader = null;
                    } catch (Throwable th4) {
                        th = th4;
                        bufferedReader = null;
                    }
                } catch (Exception e12) {
                    bufferedReader = null;
                    dataOutputStream = dataOutputStream2;
                    inputStream = inputStream;
                    e = e12;
                    inputStreamReader = null;
                } catch (Throwable th5) {
                    bufferedReader = null;
                    dataOutputStream = dataOutputStream2;
                    inputStream = inputStream;
                    th = th5;
                    inputStreamReader = null;
                }
                return sb2.toString();
            } catch (Exception e13) {
                e = e13;
                inputStreamReader = null;
                bufferedReader = null;
                dataOutputStream = dataOutputStream2;
                inputStream = null;
            } catch (Throwable th6) {
                th = th6;
                inputStreamReader = null;
                bufferedReader = null;
                dataOutputStream = dataOutputStream2;
                inputStream = null;
            }
        } catch (Exception e14) {
            e = e14;
            inputStreamReader = null;
            inputStream = null;
            bufferedReader = null;
        } catch (Throwable th7) {
            th = th7;
            inputStreamReader = null;
            inputStream = null;
            bufferedReader = null;
        }
    }
}
