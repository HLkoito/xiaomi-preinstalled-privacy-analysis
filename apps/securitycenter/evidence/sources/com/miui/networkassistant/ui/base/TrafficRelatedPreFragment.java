package com.miui.networkassistant.ui.base;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import androidx.lifecycle.j;
import com.miui.analytics.AnalyticsUtil;
import com.miui.common.base.ui.BasePreferenceFragment;
import com.miui.networkassistant.config.SimUserInfo;
import com.miui.networkassistant.dual.Sim;
import com.miui.networkassistant.dual.SimCardHelper;
import com.miui.networkassistant.service.ITrafficCornBinder;
import com.miui.networkassistant.service.ITrafficManageBinder;
import com.miui.networkassistant.service.wrapper.TmBinderCacher;
import com.miui.networkassistant.ui.dialog.CommonDialog;
import com.miui.networkassistant.utils.DeviceUtil;
import com.miui.networkassistant.utils.NotificationUtil;
import com.miui.networkassistant.utils.PrivacyDeclareAndAllowNetworkUtil;
import com.miui.networkassistant.utils.TelephonyUtil;
import com.miui.networkassistant.webapi.DataUsageResult;
import com.miui.networkassistant.webapi.WebApiAccessHelper;
import com.miui.securitycenter.R;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

/* JADX INFO: loaded from: classes2.dex */
public abstract class TrafficRelatedPreFragment extends BasePreferenceFragment {
    protected static final int ACTION_FLAG_CITY = 2;
    protected static final int ACTION_FLAG_OPERATOR = 3;
    protected static final int ACTION_FLAG_PROVINCE = 1;
    public static final String BUNDLE_KEY_FROM_NOTIFICATION = "bundle_key_from_other_task";
    public static final String BUNDLE_KEY_NEED_BACK = "key_back";
    public static final String BUNDLE_KEY_TRAFFIC_GUIDE = "traffic_guide";
    public static final String UPDATE_OPERATOR_FROM_NOTIFICATION = "update_operator";
    protected Map<Integer, String> mCityMap;
    protected LinkedHashMap<String, String> mOperatorMap;
    protected Map<Integer, String> mProvinceMap;
    protected boolean mServiceConnected;
    protected SimCardHelper mSimCardHelper;
    protected ITrafficManageBinder mTrafficManageBinder;
    protected GetVirtualOperatorTask mVirtualOperatorTask;
    protected int mBrand = -1;
    protected int mProvince = -1;
    protected int mCity = -1;
    protected String mOperator = "";
    protected int mCityCode = -1;
    protected int mProvinceCode = -1;
    protected ITrafficCornBinder[] mTrafficCornBinders = new ITrafficCornBinder[2];
    protected SimUserInfo[] mSimUserInfos = new SimUserInfo[2];
    protected int mSlotNum = 0;
    private Handler mHandler = new Handler();
    private ServiceConnection mConn = new ServiceConnection() { // from class: com.miui.networkassistant.ui.base.TrafficRelatedPreFragment.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TrafficRelatedPreFragment.this.mTrafficManageBinder = ITrafficManageBinder.Stub.asInterface(iBinder);
            try {
                TrafficRelatedPreFragment trafficRelatedPreFragment = TrafficRelatedPreFragment.this;
                trafficRelatedPreFragment.mTrafficCornBinders[0] = trafficRelatedPreFragment.mTrafficManageBinder.getTrafficCornBinder(0);
            } catch (RemoteException e10) {
                e10.printStackTrace();
            }
            TrafficRelatedPreFragment trafficRelatedPreFragment2 = TrafficRelatedPreFragment.this;
            trafficRelatedPreFragment2.mSimUserInfos[0] = SimUserInfo.getInstance(((BasePreferenceFragment) trafficRelatedPreFragment2).mAppContext, 0);
            if (DeviceUtil.IS_DUAL_CARD) {
                try {
                    TrafficRelatedPreFragment trafficRelatedPreFragment3 = TrafficRelatedPreFragment.this;
                    trafficRelatedPreFragment3.mTrafficCornBinders[1] = trafficRelatedPreFragment3.mTrafficManageBinder.getTrafficCornBinder(1);
                } catch (RemoteException e11) {
                    e11.printStackTrace();
                }
                TrafficRelatedPreFragment trafficRelatedPreFragment4 = TrafficRelatedPreFragment.this;
                trafficRelatedPreFragment4.mSimUserInfos[1] = SimUserInfo.getInstance(((BasePreferenceFragment) trafficRelatedPreFragment4).mAppContext, 1);
            }
            TrafficRelatedPreFragment trafficRelatedPreFragment5 = TrafficRelatedPreFragment.this;
            trafficRelatedPreFragment5.mServiceConnected = true;
            trafficRelatedPreFragment5.onTrafficManageServiceConnected();
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            TrafficRelatedPreFragment trafficRelatedPreFragment = TrafficRelatedPreFragment.this;
            trafficRelatedPreFragment.mTrafficManageBinder = null;
            trafficRelatedPreFragment.mServiceConnected = false;
        }
    };
    private DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() { // from class: com.miui.networkassistant.ui.base.a
        @Override // android.content.DialogInterface.OnClickListener
        public final void onClick(DialogInterface dialogInterface, int i10) {
            this.f16751a.lambda$new$0(dialogInterface, i10);
        }
    };

    protected class GetVirtualOperatorTask extends AsyncTask<Void, Void, DataUsageResult> {
        private Context mContext;
        private String mPhoneNumber;
        private SimUserInfo mSimUserInfo;

        public GetVirtualOperatorTask(Context context, SimUserInfo simUserInfo) {
            this.mContext = context.getApplicationContext();
            this.mSimUserInfo = simUserInfo;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public DataUsageResult doInBackground(Void... voidArr) {
            this.mPhoneNumber = TelephonyUtil.getPhoneNumber(this.mContext, this.mSimUserInfo.getSlotNum());
            if (!PrivacyDeclareAndAllowNetworkUtil.isAllowNetwork()) {
                return null;
            }
            long correctedNormalMonthDataUsageUsed = 0;
            try {
                TrafficRelatedPreFragment trafficRelatedPreFragment = TrafficRelatedPreFragment.this;
                ITrafficManageBinder iTrafficManageBinder = trafficRelatedPreFragment.mTrafficManageBinder;
                if (iTrafficManageBinder != null) {
                    correctedNormalMonthDataUsageUsed = iTrafficManageBinder.getCorrectedNormalMonthDataUsageUsed(trafficRelatedPreFragment.mSlotNum);
                }
            } catch (RemoteException e10) {
                Log.i("TrafficRelatedPre", "get month used exception", e10);
            }
            return WebApiAccessHelper.queryDataUsage(this.mSimUserInfo.getImsi(), String.valueOf(this.mSimUserInfo.getCity()), this.mPhoneNumber, this.mSimUserInfo.getOperator(), correctedNormalMonthDataUsageUsed, this.mSimUserInfo.getIccid());
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(DataUsageResult dataUsageResult) {
            super.onPostExecute(dataUsageResult);
            if (dataUsageResult != null && dataUsageResult.isSuccess()) {
                Log.i("TrafficRelatedPre", dataUsageResult.toString());
                TrafficRelatedPreFragment.this.mOperator = dataUsageResult.getOperator();
            } else if (dataUsageResult != null) {
                Log.i("TrafficRelatedPre", "failed result : " + dataUsageResult.toString());
            }
            TrafficRelatedPreFragment.this.initOperator(this.mPhoneNumber);
        }
    }

    private <T> Map<T, String> addTipsTitleToMap(T t10, Map<T, String> map) {
        TreeMap treeMap = new TreeMap();
        treeMap.put(t10, this.mAppContext.getString(R.string.traffic_setting_fragment_default));
        if (map != null) {
            treeMap.putAll(map);
        }
        return treeMap;
    }

    private void bindTrafficManageService() {
        TmBinderCacher.getInstance().bindTmService(this.mConn);
    }

    private void buildLocationAlertDialog(String str, String str2) {
        CommonDialog commonDialog = new CommonDialog(this.mActivity, this.mOnClickListener);
        commonDialog.setNagetiveText(this.mAppContext.getString(R.string.traffic_setting_fragment_loacation_alert_nomore));
        commonDialog.setTitle(str);
        commonDialog.setMessage(str2);
        commonDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(DialogInterface dialogInterface, int i10) {
        if (i10 != -1) {
            if (i10 == -2) {
                this.mSimUserInfos[this.mSlotNum].setSimLocationAlertIgnore(true);
            }
        } else {
            int i11 = this.mProvinceCode;
            this.mProvince = i11;
            this.mCity = this.mCityCode;
            selectProvince(i11);
        }
    }

    private void setSlotNum() {
        int currentActiveSlotNum;
        this.mSimCardHelper = SimCardHelper.getInstance(this.mAppContext);
        if (DeviceUtil.IS_DUAL_CARD) {
            Bundle arguments = getArguments();
            if (arguments == null || !arguments.containsKey(Sim.SIM_SLOT_NUM_TAG)) {
                currentActiveSlotNum = this.mSimCardHelper.isDualSimInsertedOne() ? Sim.getCurrentActiveSlotNum() : Sim.getCurrentOptSlotNum();
            } else {
                currentActiveSlotNum = arguments.getInt(Sim.SIM_SLOT_NUM_TAG, 0);
            }
            this.mSlotNum = currentActiveSlotNum;
            if (this.mSimCardHelper.isDualSimInserted()) {
                resetTitle();
            }
        }
    }

    private void unbindTrafficManageService() {
        TmBinderCacher.getInstance().unbindTmService(this.mConn);
    }

    protected void cancelVirtualOperatorTask() {
        GetVirtualOperatorTask getVirtualOperatorTask = this.mVirtualOperatorTask;
        if (getVirtualOperatorTask == null || getVirtualOperatorTask.getStatus() == AsyncTask.Status.FINISHED) {
            return;
        }
        this.mVirtualOperatorTask.cancel(true);
    }

    protected void exeGetVirtualOperatorTask() {
        GetVirtualOperatorTask getVirtualOperatorTask = new GetVirtualOperatorTask(this.mAppContext, this.mSimUserInfos[this.mSlotNum]);
        this.mVirtualOperatorTask = getVirtualOperatorTask;
        getVirtualOperatorTask.execute(new Void[0]);
    }

    protected Map<Integer, String> getCityMapByProvinceId(int i10) {
        Map cities;
        if (i10 > 0) {
            try {
                cities = this.mTrafficCornBinders[this.mSlotNum].getCities(i10);
            } catch (RemoteException e10) {
                Log.i("TrafficRelatedPre", "getCityMapByProvinceId", e10);
                cities = null;
            }
        } else {
            cities = null;
        }
        return addTipsTitleToMap(-1, cities);
    }

    @Override // androidx.fragment.app.Fragment, androidx.lifecycle.k
    @NotNull
    public /* bridge */ /* synthetic */ p0.a getDefaultViewModelCreationExtras() {
        return j.a(this);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void getOperatorMap() {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
        this.mOperatorMap = linkedHashMap;
        linkedHashMap.put("", this.mAppContext.getString(R.string.traffic_setting_fragment_default));
        this.mOperatorMap.put(TelephonyUtil.CMCC, this.mAppContext.getString(R.string.operator_name_cmcc));
        this.mOperatorMap.put(TelephonyUtil.UNICOM, this.mAppContext.getString(R.string.operator_name_unicom));
        this.mOperatorMap.put(TelephonyUtil.TELECOM, this.mAppContext.getString(R.string.operator_name_telcom));
        this.mOperatorMap.put(TelephonyUtil.CBN, this.mAppContext.getString(R.string.operator_name_cbn));
        this.mOperatorMap.put(TelephonyUtil.MIMOBILE, this.mAppContext.getString(R.string.operator_name_mi_mobile));
        this.mOperatorMap.put(TelephonyUtil.VIRTUALOPT, this.mAppContext.getString(R.string.other_operator_name_virtual_operator));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void getOperatorMap(int i10) {
        getOperatorMap();
    }

    protected <T> int getPosByTag(T t10, Map<T, String> map) {
        if (t10 == null || map == null) {
            return -1;
        }
        Iterator<T> it = map.keySet().iterator();
        int i10 = 0;
        while (it.hasNext()) {
            if (it.next().equals(t10)) {
                return i10;
            }
            i10++;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void getProvinceMap() {
        try {
            this.mProvinceMap = this.mTrafficCornBinders[this.mSlotNum].getProvinces();
        } catch (RemoteException e10) {
            Log.i("TrafficRelatedPre", "get province map failed", e10);
        }
        this.mProvinceMap = addTipsTitleToMap(-1, this.mProvinceMap);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void getProvinceMap(int i10) {
        try {
            this.mProvinceMap = this.mTrafficCornBinders[i10].getProvinces();
        } catch (RemoteException e10) {
            Log.i("TrafficRelatedPre", "get province map failed", e10);
        }
        this.mProvinceMap = addTipsTitleToMap(-1, this.mProvinceMap);
    }

    public String getSimCardLocation(int i10, int i11) {
        String str = this.mProvinceMap.get(Integer.valueOf(i10));
        Log.i("TrafficRelatedPre", "location : " + i10);
        Map<Integer, String> cityMapByProvinceId = getCityMapByProvinceId(i10);
        if (cityMapByProvinceId == null) {
            return "";
        }
        String str2 = cityMapByProvinceId.get(Integer.valueOf(i11));
        return TextUtils.equals(str, str2) ? str : String.format("%s%s", str, str2);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void initCardStuff() {
        Bundle arguments = getArguments();
        this.mSlotNum = (arguments == null || !arguments.containsKey(Sim.SIM_SLOT_NUM_TAG)) ? Sim.getCurrentOptSlotNum() : arguments.getInt(Sim.SIM_SLOT_NUM_TAG, 0);
        Sim.operateOnSlotNum(this.mSlotNum);
        NotificationUtil.cancelNormalTotalPackageNotSetted(this.mActivity);
    }

    protected void initOperator(String str) {
    }

    @Override // com.miui.common.base.ui.BasePreferenceFragment, androidx.fragment.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        bindTrafficManageService();
    }

    @Override // com.miui.common.base.ui.BasePreferenceFragment, miuix.preference.PreferenceFragment, androidx.preference.PreferenceFragmentCompat, androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setSlotNum();
        PrivacyDeclareAndAllowNetworkUtil.showSecurityCenterAllowNetwork(this.mActivity);
    }

    @Override // androidx.fragment.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        unbindTrafficManageService();
    }

    @Override // androidx.fragment.app.Fragment
    public void onPause() {
        super.onPause();
        AnalyticsUtil.recordPageEnd(getClass().getName());
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        AnalyticsUtil.recordPageStart(getClass().getName());
    }

    protected abstract void onTrafficManageServiceConnected();

    protected void resetTitle() {
        this.mHandler.post(new Runnable() { // from class: com.miui.networkassistant.ui.base.TrafficRelatedPreFragment.2
            @Override // java.lang.Runnable
            public void run() {
                if (TrafficRelatedPreFragment.this.isAttatched()) {
                    Object[] objArr = new Object[2];
                    objArr[0] = TrafficRelatedPreFragment.this.getTitle();
                    objArr[1] = ((BasePreferenceFragment) TrafficRelatedPreFragment.this).mAppContext.getString(TrafficRelatedPreFragment.this.mSlotNum == 0 ? R.string.dual_setting_simcard1 : R.string.dual_setting_simcard2);
                    TrafficRelatedPreFragment.this.setTitle(String.format("%s-%s", objArr));
                }
            }
        });
    }

    protected void selectProvince(int i10) {
    }

    protected void setSimLocationAlertIgnore() {
        if (this.mProvince == this.mSimUserInfos[this.mSlotNum].getProvince() && this.mCity == this.mSimUserInfos[this.mSlotNum].getCity()) {
            return;
        }
        this.mSimUserInfos[this.mSlotNum].setSimLocationAlertIgnore(false);
    }

    protected void showSimLocationErrorDialog() {
        Bundle arguments = getArguments();
        if (arguments == null || !arguments.getBoolean(UPDATE_OPERATOR_FROM_NOTIFICATION)) {
            return;
        }
        String string = this.mAppContext.getResources().getString(R.string.sim_location_error_notify_title);
        String simCardLocation = getSimCardLocation(this.mProvince, this.mCity);
        Log.i("TrafficRelatedPre", "mProvinceCode: " + this.mProvinceCode + ",mCityCode: " + this.mCityCode);
        String simCardLocation2 = getSimCardLocation(this.mProvinceCode, this.mCityCode);
        String str = null;
        if (!TextUtils.isEmpty(simCardLocation) && !TextUtils.isEmpty(simCardLocation2)) {
            str = String.format(this.mAppContext.getString(R.string.sim_location_error_dialog_message), simCardLocation2, simCardLocation, simCardLocation2);
        }
        buildLocationAlertDialog(string, str);
    }
}
