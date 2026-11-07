package com.miui.networkassistant.webapi;

import android.util.Log;
import com.miui.common.net.a;
import com.miui.common.net.b;
import com.miui.common.net.c;
import com.miui.networkassistant.config.CommonPerConstants;
import com.miui.networkassistant.provider.ProviderConstant;
import com.miui.networkassistant.utils.AnalyticsHelper;
import com.miui.networkassistant.utils.DeviceUtil;
import com.xiaomi.onetrack.api.ah;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import p4.i;

/* JADX INFO: loaded from: classes2.dex */
public class WebApiAccessHelper {
    private static final String TAG = "WebApiAccessHelper";

    private WebApiAccessHelper() {
    }

    private static String accessInternetByPost(String str, String str2) {
        return a.b(str, str2, "5cad8778-cddf-4269-ab73-48007445baa3", getBaseParams(), new i("networkassistant_webapiaccesshelper"));
    }

    public static PurchaseOnlineResult checkRichPurchaseOnlineResult(String str, String str2, String str3) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("imei", DeviceUtil.getAndroidId("1119a27f-c197-49c2-ab4c-2e0aa53e74b9"));
            jSONObject.put("zipCode", str);
            jSONObject.put("phonenum", str2);
            jSONObject.put("spType", str3);
        } catch (JSONException e10) {
            Log.i(TAG, "checkRichPurchaseOnlineResult", e10);
        }
        return new PurchaseOnlineResult(accessInternetByPost(jSONObject.toString(), "https://api.miui.security.xiaomi.com/netassist/floworderunity/supportfloworder"));
    }

    private static List<b> getBaseParams() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_DEVICE, DeviceUtil.DEVICE_NAME));
        arrayList.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_VERSION_TYPE, DeviceUtil.getMiuiVersionType()));
        arrayList.add(new b("region", DeviceUtil.getRegion()));
        arrayList.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_MIUI_VERSION, DeviceUtil.MIUI_VERSION));
        arrayList.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_CARRIER, DeviceUtil.CARRIER));
        arrayList.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_APP_VERSION, DeviceUtil.getAppVersionCode()));
        return arrayList;
    }

    private static List<b> getCloudDataParams() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_DEVICE, DeviceUtil.DEVICE_NAME));
        arrayList.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_T, "stable"));
        arrayList.add(new b("region", DeviceUtil.getRegion()));
        arrayList.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_MIUI_VERSION, DeviceUtil.MIUI_VERSION));
        arrayList.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_CARRIER, DeviceUtil.CARRIER));
        arrayList.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_APP_VERSION, DeviceUtil.getAppVersionCode()));
        arrayList.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_DATA_VERSION, "100"));
        arrayList.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_INIT_DEV, "false"));
        arrayList.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_IS_DIFF, com.xiaomi.onetrack.util.a.f23518i));
        return arrayList;
    }

    public static DataUsageResult queryDataUsage(String str, String str2, String str3, String str4, long j10, String str5) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("imei", DeviceUtil.getAndroidId("14ca47b4-7302-4900-91ef-6a76c65b40cc"));
            jSONObject.put(ProviderConstant.TrafficDistributionColumns.IMSI, str);
            jSONObject.put("zipCode", str2);
            jSONObject.put("phonenum", str3);
            jSONObject.put("spType", str4);
            jSONObject.put("monthused", j10);
            jSONObject.put("iccid", str5);
        } catch (JSONException e10) {
            Log.i(TAG, "DataUsageResult", e10);
        }
        return new DataUsageResult(accessInternetByPost(jSONObject.toString(), "https://api.miui.security.xiaomi.com/netassist/flow/queryflow"));
    }

    public static c reportTrafficCorrectionSms(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("imei", DeviceUtil.getAndroidId("14ca47b4-7302-4900-91ef-6a76c65b40cc"));
            jSONObject.put("province", str5);
            jSONObject.put("city", str6);
            jSONObject.put(com.miui.luckymoney.config.Constants.JSON_KEY_CARRIER, str7);
            jSONObject.put(AnalyticsHelper.TRACK_KEY_ID_TRAFFIC_PRODUCT_CLICK, str8);
            jSONObject.put("upward", str);
            jSONObject.put("directive", str2);
            jSONObject.put("downward", str3);
            jSONObject.put(ah.f22943m, str4);
            jSONObject.put("verison", "1.1");
            jSONObject.put("type", str9);
            jSONObject.put("result", str10);
        } catch (JSONException e10) {
            Log.i(TAG, "reportTrafficCorrectionSms", e10);
        }
        return new c(a.b(jSONObject.toString(), "https://api.sec.miui.com/v1/traffic/report", "5cdd8678-cddf-4269-ab73-48387445bba4", getBaseParams(), new i("networkassistant_reporttrafficcorrectionsms")));
    }

    public static CloudModuleResult updateMiuiVpnInfos() {
        List<b> cloudDataParams = getCloudDataParams();
        cloudDataParams.add(new b(com.miui.luckymoney.config.Constants.JSON_KEY_MODULE, CommonPerConstants.KEY.MIUI_VPN_INFOS));
        return new CloudModuleResult(a.b("update", "https://api.sec.miui.com/common/whiteList/listByModule", "21da76da-224c-2313-ac60-abcd70139283", cloudDataParams, new i("networkassistant_updatemiuivpninfos")));
    }

    public static PurchaseSmsNumberResult updatePurchaseSmsNumberWhiteList() {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("imei", DeviceUtil.getAndroidId("3e68adeb-b164-c212-s332-b0fb4dec6bf9"));
            jSONObject.put(com.miui.luckymoney.config.Constants.JSON_KEY_DATA_VERSION, 0);
            jSONObject.put(com.miui.luckymoney.config.Constants.JSON_KEY_IS_DIFF, false);
        } catch (JSONException e10) {
            Log.i(TAG, "updatePurchaseSmsNumberWhiteList", e10);
        }
        return new PurchaseSmsNumberResult(a.b(jSONObject.toString(), "https://api.sec.miui.com/common/whiteList/trafficChargeNum", "3e68adeb-b164-c212-s332-b0fb4dec6bf9", getBaseParams(), new i("networkassistant_updatepurchasesmsnumberwhitelist")));
    }
}
