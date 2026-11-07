package com.miui.warningcenter.policeassist;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.k;
import cl.e;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.audio.AacUtil;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.miui.earthquakewarning.utils.MsgObservable;
import com.miui.networkassistant.utils.TelephonyUtil;
import com.miui.policeassist.EPSManager;
import com.miui.powercenter.bean.StatusBarGuideParams;
import com.miui.securitycenter.Application;
import com.miui.securitycenter.R;
import com.xiaomi.accountsdk.activate.ActivateManager;
import com.xiaomi.onetrack.api.au;
import ef.w;
import ef.x;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import miui.telephony.PhoneNumberUtils;
import miui.telephony.SmsManager;
import miui.telephony.SubscriptionInfo;
import miui.telephony.SubscriptionManager;
import miui.telephony.TelephonyManager;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class PaUtils {
    private static List<String> EMERGENCY_NUMBERS = null;
    public static final int NOTIFICATION_ID = 500;
    public static final String TAG = "PaUtils";

    static {
        ArrayList arrayList = new ArrayList();
        EMERGENCY_NUMBERS = arrayList;
        arrayList.add("110");
        EMERGENCY_NUMBERS.add("120");
        EMERGENCY_NUMBERS.add("119");
        EMERGENCY_NUMBERS.add("122");
    }

    private PaUtils() {
    }

    private static void addBodyIfExists(HttpURLConnection httpURLConnection, String str) throws IOException {
        if (str != null) {
            httpURLConnection.setDoOutput(true);
            httpURLConnection.addRequestProperty(HttpHeaders.CONTENT_TYPE, "application/json");
            DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            dataOutputStream.writeBytes(str);
            dataOutputStream.close();
        }
    }

    public static byte[] buildMessageData(int i10, int i11, double d10, double d11, long j10) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(3);
        byteBufferAllocate.put((byte) 3);
        byteBufferAllocate.put((byte) 16);
        byteBufferAllocate.put((byte) 0);
        byte[] bArrArray = byteBufferAllocate.array();
        ByteBuffer byteBufferAllocate2 = ByteBuffer.allocate(117);
        byteBufferAllocate2.putShort((short) i10);
        byteBufferAllocate2.putShort((short) i11);
        byteBufferAllocate2.putDouble(d10);
        byteBufferAllocate2.putDouble(d11);
        byteBufferAllocate2.putLong(System.currentTimeMillis());
        byteBufferAllocate2.put(w.a(Application.A()).getBytes());
        byteBufferAllocate2.put((byte) 0);
        byteBufferAllocate2.putLong(j10);
        byte[] bArrRsaEncrypt = rsaEncrypt(byteBufferAllocate2.array());
        ByteBuffer byteBufferAllocate3 = ByteBuffer.allocate(131);
        byteBufferAllocate3.put(bArrArray);
        byteBufferAllocate3.put(bArrRsaEncrypt);
        return byteBufferAllocate3.array();
    }

    private static String buildNetMessage(String str, String str2, String str3, long j10, String str4, double d10, double d11, String str5, long j11, long j12) {
        PapaLocation papaLocation = new PapaLocation();
        papaLocation.setChannelName(PaCommon.NET_XIAOMI_CHANNEL_NAME);
        papaLocation.setChannelNo(PaCommon.NET_XIAOMI_CHANNEL);
        papaLocation.setDeviceId(w.a(Application.A()));
        papaLocation.setMobile(str);
        papaLocation.setEmergencyNum(str2);
        papaLocation.setAreaCode(str3);
        papaLocation.setCallMillisTime(String.valueOf(j10));
        if (!TextUtils.isEmpty(str4)) {
            papaLocation.setType(str4);
        }
        ArrayList arrayList = new ArrayList();
        PapaLocation.LocationList locationList = new PapaLocation.LocationList();
        locationList.setAddress(str5);
        locationList.setLatitude(String.valueOf(d10));
        locationList.setLongtitude(String.valueOf(d11));
        locationList.setLocationStartTime(String.valueOf(j11));
        locationList.setLocationEndTime(String.valueOf(j12));
        arrayList.add(locationList);
        papaLocation.setVersion(String.valueOf(3));
        papaLocation.setLocationList(arrayList);
        return new Gson().toJson(papaLocation);
    }

    public static String doPost(String str, String str2) throws Throwable {
        ByteArrayOutputStream byteArrayOutputStream;
        InputStream inputStream;
        InputStream inputStream2 = null;
        if (TextUtils.isEmpty(str) || !x.z()) {
            return null;
        }
        try {
            HttpURLConnection httpURLConnectionOpenConnection = openConnection(str);
            httpURLConnectionOpenConnection.setRequestMethod("POST");
            addBodyIfExists(httpURLConnectionOpenConnection, str2);
            if (httpURLConnectionOpenConnection.getResponseCode() != 200) {
                e.b(null);
                e.c(null);
                return null;
            }
            inputStream = httpURLConnectionOpenConnection.getInputStream();
            try {
                byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    try {
                        byte[] bArr = new byte[4096];
                        while (true) {
                            int i10 = inputStream.read(bArr);
                            if (i10 == -1) {
                                String string = byteArrayOutputStream.toString();
                                e.b(inputStream);
                                e.c(byteArrayOutputStream);
                                return string;
                            }
                            byteArrayOutputStream.write(bArr, 0, i10);
                            th = th;
                            inputStream2 = inputStream;
                            e.b(inputStream2);
                            e.c(byteArrayOutputStream);
                            throw th;
                        }
                    } catch (Exception e10) {
                        e = e10;
                        Log.e(TAG, "exception when do post request", e);
                        e.b(inputStream);
                        e.c(byteArrayOutputStream);
                        return null;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Exception e11) {
                e = e11;
                byteArrayOutputStream = null;
            } catch (Throwable th3) {
                th = th3;
                byteArrayOutputStream = null;
            }
        } catch (Exception e12) {
            e = e12;
            inputStream = null;
            byteArrayOutputStream = null;
        } catch (Throwable th4) {
            th = th4;
            byteArrayOutputStream = null;
        }
        Log.e(TAG, "exception when do post request", e);
        e.b(inputStream);
        e.c(byteArrayOutputStream);
        return null;
    }

    public static byte[] encoder(byte[] bArr) {
        byte[] bArr2 = new byte[140];
        int i10 = 0;
        int i11 = 0;
        while (i10 < bArr.length * 8) {
            if (i10 % 15 == 0) {
                setBit(bArr2, i11, (byte) 0);
                i11++;
            }
            setBit(bArr2, i11, getBit(bArr, i10));
            i10++;
            i11++;
        }
        return bArr2;
    }

    public static byte getBit(byte[] bArr, int i10) {
        return (byte) ((bArr[i10 / 8] & ((byte) (1 << (7 - (i10 % 8))))) == 0 ? 0 : 1);
    }

    private static int getCurrentCallingSlot(Context context) {
        try {
            if (TelephonyManager.getDefault().getPhoneCount() <= 1) {
                return -1;
            }
            int callStateForSlot = TelephonyManager.getDefault().getCallStateForSlot(0);
            int callStateForSlot2 = TelephonyManager.getDefault().getCallStateForSlot(1);
            if (1 == callStateForSlot || 2 == callStateForSlot) {
                return 0;
            }
            return (1 == callStateForSlot2 || 2 == callStateForSlot2) ? 1 : -1;
        } catch (Exception e10) {
            e10.printStackTrace();
            return -1;
        }
    }

    public static SubscriptionInfo getCurrentEnableSubInfo() {
        List<SubscriptionInfo> subscriptionInfoList = SubscriptionManager.getDefault().getSubscriptionInfoList();
        if (subscriptionInfoList != null && !subscriptionInfoList.isEmpty()) {
            SubscriptionInfo subscriptionInfoForSlot = SubscriptionManager.getDefault().getSubscriptionInfoForSlot(SubscriptionManager.getDefault().getDefaultVoiceSlotId());
            if (subscriptionInfoForSlot != null && subscriptionInfoForSlot.isActivated()) {
                return subscriptionInfoForSlot;
            }
            for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                if (subscriptionInfo.isActivated()) {
                    return subscriptionInfo;
                }
            }
        }
        return null;
    }

    public static String getMyPhoneNumber(Context context) throws Throwable {
        SubscriptionInfo currentEnableSubInfo = getCurrentEnableSubInfo();
        if (currentEnableSubInfo != null) {
            int currentCallingSlot = getCurrentCallingSlot(context);
            if (currentCallingSlot <= -1) {
                currentCallingSlot = currentEnableSubInfo.getSlotId();
            }
            if (!TelephonyUtil.isChinaOperatorForNetworkAndSim(currentCallingSlot)) {
                return null;
            }
            String phoneNumber = getPhoneNumber(Application.A(), currentCallingSlot);
            if (!TextUtils.isEmpty(phoneNumber)) {
                return phoneNumber;
            }
        }
        return null;
    }

    /* JADX WARN: Code duplicated, block: B:16:0x0027 A[PHI: r0 r5
  0x0027: PHI (r0v3 java.lang.String) = (r0v12 java.lang.String), (r0v7 java.lang.String) binds: [B:22:0x0032, B:15:0x0025] A[DONT_GENERATE, DONT_INLINE]
  0x0027: PHI (r5v2 com.xiaomi.accountsdk.activate.ActivateManager$ActivateManagerFuture) = 
  (r5v5 com.xiaomi.accountsdk.activate.ActivateManager$ActivateManagerFuture)
  (r5v7 com.xiaomi.accountsdk.activate.ActivateManager$ActivateManagerFuture)
 binds: [B:22:0x0032, B:15:0x0025] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Code duplicated, block: B:31:0x0048  */
    public static String getPhoneNumber(Context context, int i10) throws Throwable {
        ActivateManager.ActivateManagerFuture activateInfo;
        Context context2 = null;
        string = null;
        string = null;
        String string = null;
        if (i10 < 0 || i10 > 2) {
            return null;
        }
        try {
            try {
                activateInfo = ActivateManager.get(context).getActivateInfo(i10);
                if (activateInfo != null) {
                    try {
                        string = ((Bundle) activateInfo.getResult(C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS, TimeUnit.MILLISECONDS)).getString("activate_phone");
                    } catch (Exception e10) {
                        e = e10;
                        e.printStackTrace();
                        if (activateInfo != null) {
                            activateInfo.cancel(true);
                        }
                    }
                }
                if (activateInfo != null) {
                    activateInfo.cancel(true);
                }
            } catch (Throwable th2) {
                th = th2;
                context2 = context;
                if (context2 != null) {
                    context2.cancel(true);
                }
                throw th;
            }
        } catch (Exception e11) {
            e = e11;
            activateInfo = null;
        } catch (Throwable th3) {
            th = th3;
            if (context2 != null) {
                context2.cancel(true);
            }
            throw th;
        }
        return TextUtils.isEmpty(string) ? TelephonyManager.getDefault().getLine1NumberForSlot(i10) : string;
    }

    public static boolean getPoliceAssistToggle(Context context) {
        return EPSManager.getEPSPoliceAssistToggle(context) || Settings.Secure.getInt(context.getContentResolver(), PaCommon.KEY_POLICE_ASSIST_TOGGLE, 0) == 1;
    }

    public static boolean isEmergencyNumber(String str) {
        String effectiveNumber = PhoneNumberUtils.PhoneNumber.parse(str).getEffectiveNumber();
        List<String> list = EMERGENCY_NUMBERS;
        return list != null && list.contains(effectiveNumber);
    }

    public static boolean isNumeric(String str) {
        return Pattern.compile("\\d*").matcher(str).matches();
    }

    public static boolean isSimCardStateReady(Context context) {
        return ((android.telephony.TelephonyManager) context.getSystemService(au.f23034d)).getSimState() == 5;
    }

    public static HttpURLConnection openConnection(String str) {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(AacUtil.AAC_LC_MAX_RATE_BYTES_PER_SECOND);
        httpURLConnection.setReadTimeout(AacUtil.AAC_LC_MAX_RATE_BYTES_PER_SECOND);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setDoInput(true);
        return httpURLConnection;
    }

    public static void postMessage(String str, String str2, String str3, long j10, String str4, double d10, double d11, String str5, long j11, long j12) {
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("channelNo", PaCommon.NET_XIAOMI_CHANNEL);
            jSONObject.put("data", new AesCbcUtils(PaCommon.AES_ENCRYPT_KEY).encrypt(buildNetMessage(str, str2, str3, j10, str4, d10, d11, str5, j11, j12)));
            if (TextUtils.isEmpty(doPost(PaCommon.NET_ADDRESS, jSONObject.toString()))) {
                sendMessage(Application.A(), Integer.parseInt(str2), Integer.parseInt(str3), d10, d11, j10);
            }
            MsgObservable.getInstance().sendEmptyMessage(3);
        } catch (Exception e10) {
            Log.e(TAG, "postMessage Exception:", e10);
        }
    }

    public static byte[] rsaEncrypt(byte[] bArr) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(1, keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(PaCommon.RSA_ENCRYPT_KEY))));
        return cipher.doFinal(bArr);
    }

    public static void sendMessage(Context context, int i10, int i11, double d10, double d11, long j10) {
        try {
            byte[] bArrEncoder = encoder(buildMessageData(i10, i11, d10, d11, j10));
            Intent intent = new Intent(context, (Class<?>) PaService.class);
            intent.setAction(PaService.ACTION_SEND_MESSAGE_DONE);
            SmsManager.getDefault().sendTextMessage(PaCommon.SMS_ADDRESS, (String) null, new String(bArrEncoder, "UTF_16BE"), PendingIntent.getService(context, 0, intent, 201326592), (PendingIntent) null);
        } catch (Exception e10) {
            Log.e(TAG, "sendMessage Exception:", e10);
        }
    }

    public static void setBit(byte[] bArr, int i10, byte b10) {
        int i11 = i10 / 8;
        bArr[i11] = (byte) ((b10 << (7 - (i10 % 8))) | bArr[i11]);
    }

    public static void setCallNumberAndStartService(Context context, String str) {
        android.telephony.TelephonyManager telephonyManager = (android.telephony.TelephonyManager) context.getSystemService(au.f23034d);
        SubscriptionInfo currentEnableSubInfo = getCurrentEnableSubInfo();
        if (currentEnableSubInfo == null) {
            return;
        }
        int currentCallingSlot = getCurrentCallingSlot(context);
        if (currentCallingSlot <= -1) {
            currentCallingSlot = currentEnableSubInfo.getSlotId();
        }
        if (TelephonyUtil.isChinaOperatorForNetworkAndSim(currentCallingSlot)) {
            if (telephonyManager.isNetworkRoaming()) {
                Log.w(TAG, "Not support roaming data");
                return;
            }
            Intent intent = new Intent(context, (Class<?>) PaService.class);
            intent.setAction(PaService.ACTION_PA_INIT);
            Bundle bundle = new Bundle();
            bundle.putString("CallNumber", str);
            bundle.putLong("CallTime", System.currentTimeMillis());
            intent.putExtras(bundle);
            context.startService(intent);
        }
    }

    public static void setPoliceAssistToggle(Context context, boolean z10) {
        Settings.Secure.putInt(context.getContentResolver(), PaCommon.KEY_POLICE_ASSIST_TOGGLE, z10 ? 1 : -1);
        EPSManager.setEPSPoliceAssistToggle(context, z10);
    }

    public static void showPAGuideNotification(Context context) {
        NotificationCompat.c cVarG = new NotificationCompat.c(context, StatusBarGuideParams.MY_PACKAGE_NAME).y(R.drawable.security_small_icon).m(context.getString(R.string.warningcenter_policeassist_noti_title)).l(context.getString(R.string.warningcenter_policeassist_noti_content)).x(false).v(2).n(-1).r("single").g(true);
        cVarG.a(0, context.getResources().getString(R.string.btn_text_open_now), PendingIntent.getActivity(context, 0, new Intent(context, (Class<?>) PaSettingActivity.class), 201326592));
        Bundle bundle = new Bundle();
        bundle.putBoolean("miui.showAction", true);
        cVarG.p(bundle);
        Intent intent = new Intent(context, (Class<?>) PaSettingActivity.class);
        intent.setFlags(268435456);
        cVarG.k(PendingIntent.getActivity(context, 0, intent, 67108864));
        Notification notificationD = cVarG.d();
        rg.a.c(notificationD, true);
        NotificationManagerCompat notificationManagerCompatF = NotificationManagerCompat.f(context);
        notificationManagerCompatF.e(new k.c(StatusBarGuideParams.MY_PACKAGE_NAME, 5).b(context.getString(R.string.notify_channel_name_security)).a());
        notificationManagerCompatF.h(500, notificationD);
    }

    public static void showPAPrivacyNotification(Context context) {
        NotificationCompat.c cVarG = new NotificationCompat.c(context, StatusBarGuideParams.MY_PACKAGE_NAME).y(R.drawable.security_small_icon).m(context.getString(R.string.warningcenter_policeassist_noti_send)).l(context.getString(R.string.warningcenter_policeassist_noti_privacy)).x(false).v(2).n(-1).r("single").g(true);
        Intent intent = new Intent(context, (Class<?>) PaSettingActivity.class);
        intent.setFlags(268435456);
        cVarG.a(0, context.getResources().getString(R.string.card_main_monthreport_action), PendingIntent.getActivity(context, 0, intent, 201326592));
        Bundle bundle = new Bundle();
        bundle.putBoolean("miui.showAction", true);
        cVarG.p(bundle);
        Intent intent2 = new Intent(context, (Class<?>) PaSettingActivity.class);
        intent2.setFlags(268435456);
        cVarG.k(PendingIntent.getActivity(context, 0, intent2, 67108864));
        Notification notificationD = cVarG.d();
        rg.a.c(notificationD, true);
        NotificationManagerCompat notificationManagerCompatF = NotificationManagerCompat.f(context);
        notificationManagerCompatF.e(new k.c(StatusBarGuideParams.MY_PACKAGE_NAME, 5).b(context.getString(R.string.notify_channel_name_security)).a());
        notificationManagerCompatF.h(500, notificationD);
    }

    public static void showPASendSuccessNotification(Context context) {
        NotificationCompat.c cVarN = new NotificationCompat.c(context, StatusBarGuideParams.MY_PACKAGE_NAME).y(R.drawable.security_small_icon).m(context.getResources().getString(R.string.warningcenter_policeassist_title)).l(context.getResources().getString(R.string.warningcenter_policeassist_noti_send)).x(false).r("single").v(2).n(-1);
        Bundle bundle = new Bundle();
        bundle.putBoolean("miui.showAction", true);
        cVarN.p(bundle);
        Notification notificationD = cVarN.d();
        rg.a.c(notificationD, true);
        NotificationManagerCompat notificationManagerCompatF = NotificationManagerCompat.f(context);
        notificationManagerCompatF.e(new k.c(StatusBarGuideParams.MY_PACKAGE_NAME, 5).b(context.getString(R.string.notify_channel_name_security)).a());
        notificationManagerCompatF.h(500, notificationD);
    }
}
