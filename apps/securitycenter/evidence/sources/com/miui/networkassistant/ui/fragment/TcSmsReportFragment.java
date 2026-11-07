package com.miui.networkassistant.ui.fragment;

import android.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import com.miui.apppredict.bean.AppClassificationBaseBean;
import com.miui.common.base.ui.BaseFragment;
import com.miui.common.base.ui.BaseUniversalFragmentActivity;
import com.miui.networkassistant.service.TcSmsReportService;
import com.miui.networkassistant.traffic.correction.ITrafficCorrection;
import com.miui.networkassistant.ui.base.TrafficRelatedFragment;
import com.miui.networkassistant.ui.dialog.MessageDialog;
import com.miui.networkassistant.ui.dialog.OptionTipDialog;
import com.miui.networkassistant.ui.dialog.SingleChoiceItemsDialog;
import com.miui.networkassistant.ui.dialog.TextInputDialog;
import com.miui.networkassistant.ui.view.ToolbarItemView;
import com.miui.networkassistant.utils.AnalyticsHelper;
import com.miui.networkassistant.utils.NotificationUtil;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/* JADX INFO: loaded from: classes2.dex */
public class TcSmsReportFragment extends TrafficRelatedFragment {
    private static final int ACTION_SMS_DIRECTION = 2;
    private static final int ACTION_SMS_NUM = 1;
    private static final int ACTION_SMS_RECEIVE_NUM = 3;
    public static final String EXTRA_VIEW_FROM = "view_from";
    private static final int TITLE_FILED = 2131892651;
    private LinearLayout mGetAgainAndReportLayout;
    private Button mGetSmsAgainButton;
    private Button mGetSmsButton;
    private TextInputDialog mInputDialog;
    private Button mReportButton;
    private TextView mReturnSmsTextView;
    private ToolbarItemView mSmsDirectionToolbar;
    private ToolbarItemView mSmsNumberToolbar;
    private ToolbarItemView mSmsReceiverNumberToolbar;
    private String[] mSmsReportString;
    private ToolbarItemView mSmsReportTypeToolbar;
    private SingleChoiceItemsDialog mSortChoiceDialog;
    private TcSmsReportService.TcSmsReportServiceBinder mTcBinder;
    private TextView mTipsResultTextView;
    private String mViewFrom;
    private int mSmsReportSelected = 0;
    private int mUploadType = 1;
    private SingleChoiceItemsDialog.SingleChoiceItemsDialogListener mSortedChoiceDialogListener = new SingleChoiceItemsDialog.SingleChoiceItemsDialogListener() { // from class: com.miui.networkassistant.ui.fragment.TcSmsReportFragment.1
        @Override // com.miui.networkassistant.ui.dialog.SingleChoiceItemsDialog.SingleChoiceItemsDialogListener
        public void onSelectItemUpdate(int i10, int i11) {
            TcSmsReportFragment.this.mSmsReportSelected = i10;
            TcSmsReportFragment tcSmsReportFragment = TcSmsReportFragment.this;
            tcSmsReportFragment.mUploadType = tcSmsReportFragment.parseReportType(i10);
            TcSmsReportFragment.this.mGetSmsButton.setText(TcSmsReportFragment.this.getSmsButtonText());
            TcSmsReportFragment.this.mSmsReportTypeToolbar.setDesc(TcSmsReportFragment.this.mSmsReportString[TcSmsReportFragment.this.mSmsReportSelected]);
            TcSmsReportFragment.this.getPreDirectionAndNumber();
        }
    };
    private View.OnClickListener mClickListener = new View.OnClickListener() { // from class: com.miui.networkassistant.ui.fragment.TcSmsReportFragment.2
        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            TextInputDialog textInputDialog;
            String string;
            int i10 = 1;
            switch (view.getId()) {
                case R.id.button1:
                    new MessageDialog(((BaseFragment) TcSmsReportFragment.this).mActivity).buildShowDialog(TcSmsReportFragment.this.getString(com.miui.securitycenter.R.string.tc_sms_report_tips_dialog_title), TcSmsReportFragment.this.getString(com.miui.securitycenter.R.string.tc_sms_report_tips_dialog_message));
                    return;
                case com.miui.securitycenter.R.id.button_get_sms /* 2131427810 */:
                    if (TcSmsReportFragment.this.isTcServiceConnected()) {
                        ((BaseFragment) TcSmsReportFragment.this).mAppContext.startService(new Intent(((BaseFragment) TcSmsReportFragment.this).mAppContext, (Class<?>) TcSmsReportService.class));
                        if (TextUtils.isEmpty(TcSmsReportFragment.this.mSmsNumberToolbar.getDesc()) || TextUtils.isEmpty(TcSmsReportFragment.this.mSmsDirectionToolbar.getDesc()) || TextUtils.isEmpty(TcSmsReportFragment.this.mSmsReceiverNumberToolbar.getDesc())) {
                            AnalyticsHelper.trackEmptySmsReport();
                            Toast.makeText(((BaseFragment) TcSmsReportFragment.this).mAppContext, com.miui.securitycenter.R.string.traffic_correction_failed, 1).show();
                        } else {
                            Toast.makeText(((BaseFragment) TcSmsReportFragment.this).mAppContext, TcSmsReportFragment.this.getString(com.miui.securitycenter.R.string.tc_sms_report_toast_text), 1).show();
                            TcSmsReportFragment.this.mTcBinder.startMonitorSms(TcSmsReportFragment.this.mSmsNumberToolbar.getDesc(), TcSmsReportFragment.this.mSmsDirectionToolbar.getDesc(), TcSmsReportFragment.this.mSmsReceiverNumberToolbar.getDesc(), ((TrafficRelatedFragment) TcSmsReportFragment.this).mSlotNum, TcSmsReportFragment.this.mUploadType);
                        }
                        TcSmsReportFragment.this.checkAndApplyStatus();
                        return;
                    }
                    return;
                case com.miui.securitycenter.R.id.button_get_sms_again /* 2131427811 */:
                    TcSmsReportFragment.this.mTipsResultTextView.setText(TcSmsReportFragment.this.getString(com.miui.securitycenter.R.string.tc_sms_report_get_default));
                    if (TcSmsReportFragment.this.isTcServiceConnected()) {
                        TcSmsReportFragment.this.mTcBinder.reset();
                    }
                    TcSmsReportFragment.this.initData();
                    return;
                case com.miui.securitycenter.R.id.button_report_sms /* 2131427816 */:
                    TcSmsReportFragment.this.tcSmsReportDeclare();
                    return;
                case com.miui.securitycenter.R.id.layout_sms_direction /* 2131429020 */:
                    TcSmsReportFragment.this.mInputDialog.setNumberText(false);
                    textInputDialog = TcSmsReportFragment.this.mInputDialog;
                    string = TcSmsReportFragment.this.getString(com.miui.securitycenter.R.string.tc_sms_report_direction);
                    i10 = 2;
                    break;
                case com.miui.securitycenter.R.id.layout_sms_number /* 2131429021 */:
                    TcSmsReportFragment.this.mInputDialog.setNumberText(true);
                    textInputDialog = TcSmsReportFragment.this.mInputDialog;
                    string = TcSmsReportFragment.this.getString(com.miui.securitycenter.R.string.tc_sms_report_send_num);
                    break;
                case com.miui.securitycenter.R.id.layout_sms_receive_num /* 2131429022 */:
                    TcSmsReportFragment.this.mInputDialog.setNumberText(true);
                    textInputDialog = TcSmsReportFragment.this.mInputDialog;
                    string = TcSmsReportFragment.this.getString(com.miui.securitycenter.R.string.tc_sms_report_receive_num);
                    i10 = 3;
                    break;
                case com.miui.securitycenter.R.id.layout_upload_type /* 2131429029 */:
                    TcSmsReportFragment.this.mSortChoiceDialog.buildDialog(((BaseFragment) TcSmsReportFragment.this).mAppContext.getString(com.miui.securitycenter.R.string.tc_sms_report_type), TcSmsReportFragment.this.mSmsReportString, TcSmsReportFragment.this.mSmsReportSelected, 0);
                    return;
                default:
                    return;
            }
            textInputDialog.buildInputDialog(string, "", i10);
        }
    };
    private TextInputDialog.TextInputDialogListener mTextInputDialogListener = new TextInputDialog.TextInputDialogListener() { // from class: com.miui.networkassistant.ui.fragment.TcSmsReportFragment.5
        @Override // com.miui.networkassistant.ui.dialog.TextInputDialog.TextInputDialogListener
        public void onTextSetted(String str, int i10) {
            ToolbarItemView toolbarItemView;
            if (i10 == 1) {
                toolbarItemView = TcSmsReportFragment.this.mSmsNumberToolbar;
            } else {
                if (i10 != 2) {
                    if (i10 == 3) {
                        toolbarItemView = TcSmsReportFragment.this.mSmsReceiverNumberToolbar;
                    }
                    TcSmsReportFragment.this.mGetSmsButton.setEnabled(TcSmsReportFragment.this.isSmsAndDirectionOk());
                }
                toolbarItemView = TcSmsReportFragment.this.mSmsDirectionToolbar;
            }
            toolbarItemView.setDesc(str);
            TcSmsReportFragment.this.mGetSmsButton.setEnabled(TcSmsReportFragment.this.isSmsAndDirectionOk());
        }
    };
    private ServiceConnection mConnection = new ServiceConnection() { // from class: com.miui.networkassistant.ui.fragment.TcSmsReportFragment.6
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TcSmsReportFragment.this.mTcBinder = (TcSmsReportService.TcSmsReportServiceBinder) iBinder;
            TcSmsReportFragment.this.mTcBinder.registerSmsReportListener(TcSmsReportFragment.this.mSmsReportListener);
            TcSmsReportFragment.this.initData();
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            TcSmsReportFragment.this.mTcBinder = null;
        }
    };
    private TcSmsReportService.SmsReportListener mSmsReportListener = new TcSmsReportService.SmsReportListener() { // from class: com.miui.networkassistant.ui.fragment.TcSmsReportFragment.7
        @Override // com.miui.networkassistant.service.TcSmsReportService.SmsReportListener
        public void onSmsReceived() {
            TcSmsReportFragment.this.checkAndApplyStatus();
            Toast.makeText(((BaseFragment) TcSmsReportFragment.this).mAppContext, com.miui.securitycenter.R.string.tc_sms_report_get_success, 1).show();
        }

        @Override // com.miui.networkassistant.service.TcSmsReportService.SmsReportListener
        public void onSmsSentFailure() {
            TcSmsReportFragment.this.checkAndApplyStatus();
            TcSmsReportFragment.this.postOnUiThread(new com.miui.common.base.asyn.b(TcSmsReportFragment.this) { // from class: com.miui.networkassistant.ui.fragment.TcSmsReportFragment.7.2
                @Override // com.miui.common.base.asyn.b
                public void runOnUiThread() {
                    new MessageDialog(((BaseFragment) TcSmsReportFragment.this).mActivity).buildShowDialog(getString(com.miui.securitycenter.R.string.tc_sms_report_notify_get_failure_title), getString(com.miui.securitycenter.R.string.tc_sms_report_notify_get_failure_body));
                }
            });
        }

        @Override // com.miui.networkassistant.service.TcSmsReportService.SmsReportListener
        public void onTimeOut() {
            TcSmsReportFragment.this.checkAndApplyStatus();
            TcSmsReportFragment.this.postOnUiThread(new com.miui.common.base.asyn.b(TcSmsReportFragment.this) { // from class: com.miui.networkassistant.ui.fragment.TcSmsReportFragment.7.1
                @Override // com.miui.common.base.asyn.b
                public void runOnUiThread() {
                    new MessageDialog(((BaseFragment) TcSmsReportFragment.this).mActivity).buildShowDialog(getString(com.miui.securitycenter.R.string.tc_sms_report_notify_get_timeout_title), getString(com.miui.securitycenter.R.string.tc_sms_report_notify_get_timeout_body));
                }
            });
        }
    };

    /* JADX INFO: renamed from: com.miui.networkassistant.ui.fragment.TcSmsReportFragment$9, reason: invalid class name */
    static /* synthetic */ class AnonymousClass9 {
        static final /* synthetic */ int[] $SwitchMap$com$miui$networkassistant$service$TcSmsReportService$SmsReportStatus;

        static {
            int[] iArr = new int[TcSmsReportService.SmsReportStatus.values().length];
            $SwitchMap$com$miui$networkassistant$service$TcSmsReportService$SmsReportStatus = iArr;
            try {
                iArr[TcSmsReportService.SmsReportStatus.Init.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$miui$networkassistant$service$TcSmsReportService$SmsReportStatus[TcSmsReportService.SmsReportStatus.Receiving.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$com$miui$networkassistant$service$TcSmsReportService$SmsReportStatus[TcSmsReportService.SmsReportStatus.Received.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$com$miui$networkassistant$service$TcSmsReportService$SmsReportStatus[TcSmsReportService.SmsReportStatus.SmsSendFailure.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                $SwitchMap$com$miui$networkassistant$service$TcSmsReportService$SmsReportStatus[TcSmsReportService.SmsReportStatus.Timeout.ordinal()] = 5;
            } catch (NoSuchFieldError unused5) {
            }
        }
    }

    private void bindTcSmsReportService() {
        this.mAppContext.bindService(new Intent(this.mAppContext, (Class<?>) TcSmsReportService.class), this.mConnection, 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkAndApplyStatus() {
        if (this.mServiceConnected && isTcServiceConnected()) {
            postOnUiThread(new com.miui.common.base.asyn.b(this) { // from class: com.miui.networkassistant.ui.fragment.TcSmsReportFragment.4
                @Override // com.miui.common.base.asyn.b
                public void runOnUiThread() {
                    TextView textView;
                    int i10;
                    TcSmsReportFragment.this.mGetSmsButton.setEnabled(TcSmsReportFragment.this.isSmsAndDirectionOk());
                    TcSmsReportService.SmsReportStatus status = TcSmsReportFragment.this.mTcBinder.getStatus();
                    int i11 = AnonymousClass9.$SwitchMap$com$miui$networkassistant$service$TcSmsReportService$SmsReportStatus[status.ordinal()];
                    if (i11 == 2) {
                        TcSmsReportFragment.this.mSmsNumberToolbar.setDesc(TcSmsReportFragment.this.mTcBinder.getSmsNum());
                        TcSmsReportFragment.this.mSmsDirectionToolbar.setDesc(TcSmsReportFragment.this.mTcBinder.getSmsDirection());
                        TcSmsReportFragment.this.mSmsReceiverNumberToolbar.setDesc(TcSmsReportFragment.this.mTcBinder.getSmsReceiveNum());
                        TcSmsReportFragment.this.mGetSmsButton.setEnabled(false);
                        TcSmsReportFragment.this.setToolbarItemEnable(false);
                        TcSmsReportFragment.this.mGetSmsButton.setText(getString(com.miui.securitycenter.R.string.tc_sms_report_geting_sms));
                        textView = TcSmsReportFragment.this.mTipsResultTextView;
                        i10 = com.miui.securitycenter.R.string.tc_sms_report_get_default;
                    } else {
                        if (i11 != 3) {
                            if (i11 == 4 || i11 == 5) {
                                TcSmsReportFragment.this.mSmsNumberToolbar.setDesc(TcSmsReportFragment.this.mTcBinder.getSmsNum());
                                TcSmsReportFragment.this.mSmsDirectionToolbar.setDesc(TcSmsReportFragment.this.mTcBinder.getSmsDirection());
                                TcSmsReportFragment.this.mSmsReceiverNumberToolbar.setDesc(TcSmsReportFragment.this.mTcBinder.getSmsReceiveNum());
                                TcSmsReportFragment.this.mReturnSmsTextView.setText(TcSmsReportFragment.this.mTcBinder.getSmsReturned());
                                TcSmsReportFragment.this.mGetSmsButton.setText(com.miui.securitycenter.R.string.tc_sms_report_get_sms2);
                                TcSmsReportFragment.this.mGetSmsButton.setVisibility(0);
                                TcSmsReportFragment.this.mGetAgainAndReportLayout.setVisibility(8);
                                TcSmsReportFragment.this.setToolbarItemEnable(true);
                                TcSmsReportFragment.this.mTipsResultTextView.setVisibility(0);
                                TcSmsReportFragment.this.mTipsResultTextView.setText(getString(status == TcSmsReportService.SmsReportStatus.SmsSendFailure ? com.miui.securitycenter.R.string.tc_sms_report_get_sms_failure : com.miui.securitycenter.R.string.tc_sms_report_get_sms_timeout));
                                ((BaseFragment) TcSmsReportFragment.this).mAppContext.stopService(new Intent(((BaseFragment) TcSmsReportFragment.this).mAppContext, (Class<?>) TcSmsReportService.class));
                                return;
                            }
                            return;
                        }
                        TcSmsReportFragment.this.mSmsNumberToolbar.setDesc(TcSmsReportFragment.this.mTcBinder.getSmsNum());
                        TcSmsReportFragment.this.mSmsDirectionToolbar.setDesc(TcSmsReportFragment.this.mTcBinder.getSmsDirection());
                        TcSmsReportFragment.this.mSmsReceiverNumberToolbar.setDesc(TcSmsReportFragment.this.mTcBinder.getSmsReceiveNum());
                        TcSmsReportFragment.this.mReturnSmsTextView.setText(TcSmsReportFragment.this.mTcBinder.getSmsReturned());
                        TcSmsReportFragment.this.mGetSmsButton.setVisibility(8);
                        TcSmsReportFragment.this.mGetAgainAndReportLayout.setVisibility(0);
                        TcSmsReportFragment.this.setToolbarItemEnable(false);
                        TcSmsReportFragment.this.mTipsResultTextView.setVisibility(0);
                        textView = TcSmsReportFragment.this.mTipsResultTextView;
                        i10 = com.miui.securitycenter.R.string.tc_sms_report_get_sms_successs;
                    }
                    textView.setText(getString(i10));
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getPreDirectionAndNumber() {
        Map instructions;
        String customizedSmsContent;
        if (this.mServiceConnected) {
            String customizedSmsNum = null;
            try {
                instructions = this.mTrafficCornBinders[this.mSlotNum].getInstructions(this.mUploadType);
            } catch (RemoteException e10) {
                e10.printStackTrace();
                instructions = null;
            }
            if (instructions == null || instructions.size() <= 0) {
                return;
            }
            Map.Entry entry = (Map.Entry) instructions.entrySet().iterator().next();
            String strSubstring = (String) entry.getKey();
            int iIndexOf = strSubstring.indexOf(AppClassificationBaseBean.OTHER);
            if (iIndexOf > 0) {
                strSubstring = strSubstring.substring(0, iIndexOf);
            }
            String[] strArrSplit = ((String) entry.getValue()).split(AppClassificationBaseBean.OTHER);
            if (this.mUploadType == 1) {
                customizedSmsNum = this.mSimUserInfos[this.mSlotNum].getCustomizedSmsNum();
                customizedSmsContent = this.mSimUserInfos[this.mSlotNum].getCustomizedSmsContent();
            } else {
                customizedSmsContent = null;
            }
            if (!TextUtils.isEmpty(customizedSmsNum)) {
                strSubstring = customizedSmsNum;
            }
            if (TextUtils.isEmpty(customizedSmsContent)) {
                customizedSmsContent = strArrSplit[0];
            }
            this.mSmsNumberToolbar.setDesc(strSubstring);
            this.mSmsDirectionToolbar.setDesc(customizedSmsContent);
            this.mSmsReceiverNumberToolbar.setDesc(strArrSplit[1]);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getSmsButtonText() {
        int i10 = this.mUploadType;
        if (i10 != 2) {
            return i10 != 4 ? com.miui.securitycenter.R.string.tc_sms_report_get_sms : com.miui.securitycenter.R.string.tc_sms_report_get_sms_calltime;
        }
        return com.miui.securitycenter.R.string.tc_sms_report_get_sms_bill;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initData() {
        if (this.mServiceConnected && isTcServiceConnected()) {
            if (this.mSlotNum != this.mTcBinder.getCurrentSlotNum()) {
                this.mTcBinder.reset();
            }
            this.mUploadType = this.mActivity.getIntent().getIntExtra(ITrafficCorrection.KEY_CORRECTION_TYPE, 1);
            Bundle arguments = getArguments();
            if (arguments != null && arguments.containsKey(ITrafficCorrection.KEY_CORRECTION_TYPE)) {
                this.mUploadType = arguments.getInt(ITrafficCorrection.KEY_CORRECTION_TYPE, 1);
            }
            if (this.mTcBinder.getReportSmsType() != -1) {
                this.mUploadType = this.mTcBinder.getReportSmsType();
            }
            int reportSelected = parseReportSelected(this.mUploadType);
            this.mSmsReportSelected = reportSelected;
            this.mSmsReportTypeToolbar.setDesc(this.mSmsReportString[reportSelected]);
            setToolbarItemEnable(true);
            this.mGetSmsButton.setText(getSmsButtonText());
            this.mGetSmsButton.setVisibility(0);
            this.mGetAgainAndReportLayout.setVisibility(8);
            this.mGetSmsButton.setEnabled(false);
            this.mReturnSmsTextView.setText(Html.fromHtml(getString(com.miui.securitycenter.R.string.tc_sms_report_receive_content_null)));
            getPreDirectionAndNumber();
            checkAndApplyStatus();
        }
    }

    private void initToolbarListItem() {
        this.mSmsReportTypeToolbar = (ToolbarItemView) findViewById(com.miui.securitycenter.R.id.layout_upload_type);
        this.mSmsDirectionToolbar = (ToolbarItemView) findViewById(com.miui.securitycenter.R.id.layout_sms_direction);
        this.mSmsNumberToolbar = (ToolbarItemView) findViewById(com.miui.securitycenter.R.id.layout_sms_number);
        this.mSmsReceiverNumberToolbar = (ToolbarItemView) findViewById(com.miui.securitycenter.R.id.layout_sms_receive_num);
        this.mSmsReportTypeToolbar.setName(com.miui.securitycenter.R.string.tc_sms_report_type);
        this.mSmsDirectionToolbar.setName(com.miui.securitycenter.R.string.tc_sms_report_direction);
        this.mSmsNumberToolbar.setName(com.miui.securitycenter.R.string.tc_sms_report_send_num);
        this.mSmsReceiverNumberToolbar.setName(com.miui.securitycenter.R.string.tc_sms_report_receive_num);
        this.mSmsReportTypeToolbar.setOnClickListener(this.mClickListener);
        this.mSmsDirectionToolbar.setOnClickListener(this.mClickListener);
        this.mSmsNumberToolbar.setOnClickListener(this.mClickListener);
        this.mSmsReceiverNumberToolbar.setOnClickListener(this.mClickListener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSmsAndDirectionOk() {
        return (this.mSmsNumberToolbar.getDesc() == null || this.mSmsDirectionToolbar.getDesc() == null || this.mSmsReceiverNumberToolbar.getDesc() == null) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTcServiceConnected() {
        return this.mTcBinder != null;
    }

    private int parseReportSelected(int i10) {
        if (i10 != 2) {
            return i10 != 4 ? 0 : 2;
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int parseReportType(int i10) {
        if (i10 != 1) {
            return i10 != 2 ? 1 : 4;
        }
        return 2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportSms() {
        if (isTcServiceConnected()) {
            this.mTcBinder.report(this.mUploadType);
            finish();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setToolbarItemEnable(boolean z10) {
        this.mSmsReportTypeToolbar.setItemEnabled(z10);
        this.mSmsNumberToolbar.setItemEnabled(z10);
        this.mSmsDirectionToolbar.setItemEnabled(z10);
        this.mSmsReceiverNumberToolbar.setItemEnabled(z10);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tcSmsReportDeclare() {
        new OptionTipDialog(this.mActivity, new OptionTipDialog.OptionDialogListener() { // from class: com.miui.networkassistant.ui.fragment.TcSmsReportFragment.3
            @Override // com.miui.networkassistant.ui.dialog.OptionTipDialog.OptionDialogListener
            public void onOptionUpdated(boolean z10) {
                if (z10) {
                    TcSmsReportFragment.this.reportSms();
                    AnalyticsHelper.trackSmsReport();
                    AnalyticsHelper.trackTcSmsReport(TcSmsReportFragment.this.mUploadType);
                }
            }
        }).buildShowDialog(this.mAppContext.getResources().getString(com.miui.securitycenter.R.string.privacy_declare_dialog_title), this.mAppContext.getResources().getString(com.miui.securitycenter.R.string.privacy_prompt_tc_sms_report_message), null, this.mAppContext.getResources().getString(com.miui.securitycenter.R.string.privacy_prompt_upload));
    }

    private void unBindTcSmsReportService() {
        this.mAppContext.unbindService(this.mConnection);
    }

    @Override // com.miui.networkassistant.ui.base.TrafficRelatedFragment, miuix.appcompat.app.Fragment, androidx.fragment.app.Fragment, androidx.lifecycle.k
    @NotNull
    public /* bridge */ /* synthetic */ p0.a getDefaultViewModelCreationExtras() {
        return androidx.lifecycle.j.a(this);
    }

    @Override // com.miui.common.base.ui.BaseFragment
    protected void initView() {
        initToolbarListItem();
        this.mGetSmsButton = (Button) findViewById(com.miui.securitycenter.R.id.button_get_sms);
        this.mGetAgainAndReportLayout = (LinearLayout) findViewById(com.miui.securitycenter.R.id.layout_buttons_again_and_report);
        this.mGetSmsAgainButton = (Button) findViewById(com.miui.securitycenter.R.id.button_get_sms_again);
        this.mReportButton = (Button) findViewById(com.miui.securitycenter.R.id.button_report_sms);
        this.mReturnSmsTextView = (TextView) findViewById(com.miui.securitycenter.R.id.textview_sms_receive_content);
        this.mTipsResultTextView = (TextView) findViewById(com.miui.securitycenter.R.id.tips_textview);
        this.mGetSmsButton.setOnClickListener(this.mClickListener);
        this.mGetSmsAgainButton.setOnClickListener(this.mClickListener);
        this.mReportButton.setOnClickListener(this.mClickListener);
        this.mSmsReportString = this.mAppContext.getResources().getStringArray(com.miui.securitycenter.R.array.sms_report_type);
        this.mSortChoiceDialog = new SingleChoiceItemsDialog(this.mActivity, this.mSortedChoiceDialogListener);
        this.mInputDialog = new TextInputDialog(this.mActivity, this.mTextInputDialogListener);
        Bundle bundleExtra = this.mActivity.getIntent().getBundleExtra(BaseUniversalFragmentActivity.FRAGMENT_ARGS);
        if (bundleExtra != null) {
            this.mViewFrom = bundleExtra.getString("view_from");
        }
        if (TextUtils.isEmpty(this.mViewFrom)) {
            this.mViewFrom = "other";
        }
        AnalyticsHelper.trackTcSmsShow(this.mViewFrom);
    }

    @Override // com.miui.common.base.ui.BaseFragment, androidx.fragment.app.Fragment
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        bindTcSmsReportService();
    }

    @Override // com.miui.networkassistant.ui.base.TrafficRelatedFragment, com.miui.common.base.ui.LoadingFragment, miuix.appcompat.app.Fragment, androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setThemeRes(2131952702);
    }

    @Override // com.miui.common.base.ui.BaseFragment
    protected int onCreateViewLayout() {
        return com.miui.securitycenter.R.layout.fragment_tc_sms_report;
    }

    @Override // com.miui.common.base.ui.BaseFragment
    protected int onCustomizeActionBar(ActionBar actionBar) {
        actionBar.setDisplayOptions(16, 16);
        ImageView imageView = new ImageView(this.mActivity);
        imageView.setContentDescription(this.mAppContext.getString(com.miui.securitycenter.R.string.tips_dialog_title));
        imageView.setId(R.id.button1);
        imageView.setBackgroundResource(com.miui.securitycenter.R.drawable.app_manager_info_icon);
        imageView.setOnClickListener(this.mClickListener);
        if (!(actionBar instanceof miuix.appcompat.app.ActionBar)) {
            return 0;
        }
        ((miuix.appcompat.app.ActionBar) actionBar).setEndView(imageView);
        return 0;
    }

    @Override // com.miui.common.base.ui.BaseFragment, androidx.fragment.app.Fragment
    public void onDetach() {
        super.onDetach();
        TcSmsReportService.TcSmsReportServiceBinder tcSmsReportServiceBinder = this.mTcBinder;
        if (tcSmsReportServiceBinder != null) {
            tcSmsReportServiceBinder.unRegisterSmsReportListener(this.mSmsReportListener);
        }
        unBindTcSmsReportService();
    }

    @Override // com.miui.networkassistant.ui.base.TrafficRelatedFragment, androidx.fragment.app.Fragment
    public void onPause() {
        super.onPause();
        TcSmsReportService.TcSmsReportServiceBinder tcSmsReportServiceBinder = this.mTcBinder;
        if (tcSmsReportServiceBinder != null) {
            tcSmsReportServiceBinder.unRegisterSmsReportListener(this.mSmsReportListener);
        }
    }

    @Override // com.miui.networkassistant.ui.base.TrafficRelatedFragment, miuix.appcompat.app.Fragment, androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        TcSmsReportService.TcSmsReportServiceBinder tcSmsReportServiceBinder = this.mTcBinder;
        if (tcSmsReportServiceBinder != null) {
            tcSmsReportServiceBinder.registerSmsReportListener(this.mSmsReportListener);
        }
        NotificationUtil.cancelTcSmsReceivedNotify(this.mAppContext);
        NotificationUtil.cancelTcSmsTimeOutOrFailureNotify(this.mAppContext);
        NotificationUtil.cancelDataUsageCorrectionTimeOutOrFailureNotify(this.mAppContext);
    }

    @Override // com.miui.common.base.ui.BaseFragment
    protected int onSetTitle() {
        return com.miui.securitycenter.R.string.tc_sms_report_title;
    }

    @Override // com.miui.networkassistant.ui.base.TrafficRelatedFragment
    protected void onTrafficManageServiceConnected() {
        postOnUiThread(new com.miui.common.base.asyn.b(this) { // from class: com.miui.networkassistant.ui.fragment.TcSmsReportFragment.8
            @Override // com.miui.common.base.asyn.b
            public void runOnUiThread() {
                TcSmsReportFragment.this.initData();
            }
        });
    }
}
