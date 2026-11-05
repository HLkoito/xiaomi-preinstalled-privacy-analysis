package com.miui.warningcenter.policeassist;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.b;
import androidx.annotation.Nullable;
import androidx.lifecycle.j;
import com.miui.common.base.BaseActivity;
import com.miui.earthquakewarning.utils.Utils;
import com.miui.policeassist.EPSManager;
import com.miui.policeassist.ui.PoliceassistGuideActivity;
import com.miui.securitycenter.Application;
import com.miui.securitycenter.R;
import com.miui.warningcenter.analytics.AnalyticHelper;
import d.c;
import ef.w;
import java.lang.ref.WeakReference;
import java.util.Locale;
import miui.os.Build;
import miuix.appcompat.app.ActionBar;
import miuix.appcompat.app.AlertDialog;
import miuix.appcompat.widget.PopupMenu;
import miuix.autodensity.i;
import miuix.preference.PreferenceFragment;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import p4.d;
import ue.e;
import x4.h0;

/* JADX INFO: loaded from: classes3.dex */
public class PaSettingActivity extends BaseActivity {
    private static final int OPTION_MENU_PRIVACY_PRIVACY = 102;
    private static final int OPTION_MENU_REVOKE = 100;
    private static final int OPTION_MENU_USER_NOTICE = 101;
    private Button mClosePoliceAssist;
    private CountDownTimer mCountdownTimer;
    private Handler mHandler;
    private Button mOpenPoliceAssist;
    private AlertDialog mRevokeDialog;
    private b<Intent> resultLauncher;

    private class PrivacyRevokeTask extends AsyncTask<Void, Void, Integer> {
        private Context context = Application.A();
        private String policeName;

        public PrivacyRevokeTask(String str) {
            this.policeName = str;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Integer doInBackground(Void... voidArr) {
            Context context;
            if (isCancelled() || (context = this.context) == null) {
                return null;
            }
            return Integer.valueOf(e.c(context, this.policeName, w.a(context)));
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(Integer num) {
            if (num.intValue() != 1) {
                PaSettingActivity.this.showRevokeFailedDialog();
                return;
            }
            PaSettingActivity.this.mClosePoliceAssist.setVisibility(8);
            PaSettingActivity.this.mOpenPoliceAssist.setVisibility(0);
            PaUtils.setPoliceAssistToggle(PaSettingActivity.this, false);
            PaSettingActivity.this.onCustomizeActionBar(PaSettingActivity.this.getAppCompatActionBar(), false);
        }
    }

    private static class PrivacyUpdateRunnable implements Runnable {
        private WeakReference<PaSettingActivity> mActivityRef;

        public PrivacyUpdateRunnable(PaSettingActivity paSettingActivity) {
            this.mActivityRef = new WeakReference<>(paSettingActivity);
        }

        @Override // java.lang.Runnable
        public void run() {
            final PaSettingActivity paSettingActivity = this.mActivityRef.get();
            if (paSettingActivity == null || paSettingActivity.isFinishing() || paSettingActivity.isDestroyed()) {
                return;
            }
            String strD = e.d(paSettingActivity, Utils.POLICE_ASSIST_POLICE_NAME, w.a(paSettingActivity));
            if (TextUtils.isEmpty(strD) || !strD.contains("{")) {
                return;
            }
            try {
                JSONObject jSONObjectOptJSONObject = new JSONObject(strD).optJSONObject("translation");
                if (jSONObjectOptJSONObject == null) {
                    return;
                }
                final String strReplaceAll = jSONObjectOptJSONObject.optString(Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry()).replaceAll("\n", "<br>");
                paSettingActivity.mHandler.post(new Runnable() { // from class: com.miui.warningcenter.policeassist.PaSettingActivity.PrivacyUpdateRunnable.1
                    @Override // java.lang.Runnable
                    public void run() {
                        if (paSettingActivity.isFinishing() || paSettingActivity.isDestroyed()) {
                            return;
                        }
                        Utils.showPrivacyUpdateDialog(paSettingActivity, strReplaceAll, Utils.POLICE_ASSIST_POLICE_NAME, paSettingActivity.getResources().getString(R.string.warningcenter_policeassist_title), PaCommon.PA_PRIVACY_URL + Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry(), new Utils.Listener() { // from class: com.miui.warningcenter.policeassist.PaSettingActivity.PrivacyUpdateRunnable.1.1
                            @Override // com.miui.earthquakewarning.utils.Utils.Listener
                            public void onAgreePrivacyChange() {
                                EPSManager.setEPSPoliceAssistToggle(paSettingActivity, true);
                            }

                            @Override // com.miui.earthquakewarning.utils.Utils.Listener
                            public void onRefusePrivacyChange() {
                                paSettingActivity.finish();
                            }
                        });
                    }
                });
            } catch (Exception e10) {
                Log.e("TAG", "checkPrivacyUpdate error", e10);
            }
        }
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override // androidx.fragment.app.Fragment, androidx.lifecycle.k
        @NotNull
        public /* bridge */ /* synthetic */ p0.a getDefaultViewModelCreationExtras() {
            return j.a(this);
        }

        @Override // androidx.preference.PreferenceFragmentCompat
        public void onCreatePreferences(@Nullable Bundle bundle, @Nullable String str) {
            setPreferencesFromResource(R.xml.pa_settings, str);
        }
    }

    private void checkPrivacyUpdate() {
        com.miui.common.base.asyn.a.a(new PrivacyUpdateRunnable(this));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void closePoliceAssist() {
        this.mClosePoliceAssist.setVisibility(8);
        this.mOpenPoliceAssist.setVisibility(0);
        PaUtils.setPoliceAssistToggle(this, false);
        onCustomizeActionBar(getAppCompatActionBar(), false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRevoke(String str) {
        new PrivacyRevokeTask(str).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(ActivityResult activityResult) {
        if (activityResult.getResultCode() == -1) {
            openPoliceAssist();
        }
    }

    private void openPoliceAssist() {
        this.mClosePoliceAssist.setVisibility(0);
        this.mOpenPoliceAssist.setVisibility(8);
        PaUtils.setPoliceAssistToggle(this, true);
        onCustomizeActionBar(getAppCompatActionBar(), true);
        com.miui.common.base.asyn.a.a(new Runnable() { // from class: com.miui.warningcenter.policeassist.PaSettingActivity.10
            @Override // java.lang.Runnable
            public void run() {
                PaSettingActivity paSettingActivity = PaSettingActivity.this;
                Log.w("TAG", "pa open agree = " + e.b(paSettingActivity, Utils.POLICE_ASSIST_POLICE_NAME, w.a(paSettingActivity)));
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openPoliceassistPrivacy() {
        this.resultLauncher.a(new Intent(this, (Class<?>) PoliceassistGuideActivity.class));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void revokePrivacy(Context context, String str, String str2) {
        if (d.f(context)) {
            showRevokeDialog(context, str, str2);
        } else {
            showNoNetDialog(context);
        }
    }

    private void showNoNetDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.network_disable_dialog_title).setMessage(R.string.network_disable_dialog_content_cn).setPositiveButton(R.string.network_disable_dialog_btn_text, new DialogInterface.OnClickListener() { // from class: com.miui.warningcenter.policeassist.PaSettingActivity.8
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i10) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        final String string = getResources().getString(R.string.warningcenter_policeassist_title);
        popupMenu.getMenu().add(0, 101, 0, getResources().getString(R.string.privacy_user_notice_title)).setShowAsAction(2);
        popupMenu.getMenu().add(1, 102, 1, getResources().getString(R.string.privacy_privacy_title)).setShowAsAction(2);
        popupMenu.getMenu().add(2, 100, 2, getResources().getString(R.string.privacy_revoke_title)).setShowAsAction(2);
        String language = Locale.getDefault().getLanguage();
        String country = Locale.getDefault().getCountry();
        final String str = PaCommon.PA_PRIVACY_URL + language + "_" + country;
        final String str2 = PaCommon.PA_AGREEMENT_URL + language + "_" + country;
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: com.miui.warningcenter.policeassist.PaSettingActivity.4
            @Override // miuix.appcompat.widget.PopupMenu.OnMenuItemClickListener
            public boolean onMenuItemClick(MenuItem menuItem) {
                PaSettingActivity paSettingActivity;
                String str3;
                if (menuItem.getItemId() == 100) {
                    PaSettingActivity paSettingActivity2 = PaSettingActivity.this;
                    paSettingActivity2.revokePrivacy(paSettingActivity2, string, Utils.POLICE_ASSIST_POLICE_NAME);
                    return true;
                }
                if (menuItem.getItemId() == 102) {
                    paSettingActivity = PaSettingActivity.this;
                    str3 = str;
                } else {
                    if (menuItem.getItemId() != 101) {
                        return true;
                    }
                    paSettingActivity = PaSettingActivity.this;
                    str3 = str2;
                }
                paSettingActivity.startWebPage(str3);
                return true;
            }
        });
        popupMenu.show();
    }

    private void showRevokeDialog(Context context, String str, final String str2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.privacy_revoke_dialog_title_cn).setMessage(context.getResources().getString(R.string.privacy_revoke_dialog_content_cn, str, str, str)).setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.miui.warningcenter.policeassist.PaSettingActivity.6
            @Override // android.content.DialogInterface.OnDismissListener
            public void onDismiss(DialogInterface dialogInterface) {
                if (PaSettingActivity.this.mCountdownTimer != null) {
                    PaSettingActivity.this.mCountdownTimer.cancel();
                    PaSettingActivity.this.mCountdownTimer = null;
                }
            }
        }).setPositiveButton(R.string.privacy_revoke_dialog_negetive_text, (DialogInterface.OnClickListener) null).setNegativeButton(R.string.privacy_revoke_dialog_positive_text, new DialogInterface.OnClickListener() { // from class: com.miui.warningcenter.policeassist.PaSettingActivity.5
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i10) {
                dialogInterface.dismiss();
                PaSettingActivity.this.handleRevoke(str2);
            }
        });
        AlertDialog alertDialogCreate = builder.create();
        this.mRevokeDialog = alertDialogCreate;
        alertDialogCreate.show();
        final Button button = this.mRevokeDialog.getButton(-2);
        button.setEnabled(false);
        this.mCountdownTimer = new CountDownTimer(10000L, 1000L) { // from class: com.miui.warningcenter.policeassist.PaSettingActivity.7
            @Override // android.os.CountDownTimer
            public void onFinish() {
                button.setEnabled(true);
                button.setText(PaSettingActivity.this.getResources().getString(R.string.privacy_revoke_dialog_positive_text));
            }

            @Override // android.os.CountDownTimer
            public void onTick(long j10) {
                button.setText(PaSettingActivity.this.getResources().getString(R.string.privacy_revoke_dialog_positive_text_numbers, Long.valueOf(j10 / 1000)));
            }
        }.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showRevokeFailedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.privacy_revoke_failed_dialog_title).setMessage(R.string.privacy_revoke_failed_dialog_content).setPositiveButton(R.string.network_disable_dialog_btn_text, new DialogInterface.OnClickListener() { // from class: com.miui.warningcenter.policeassist.PaSettingActivity.9
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i10) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startWebPage(String str) {
        startActivity(new Intent("android.intent.action.VIEW", Uri.parse(str)));
    }

    @Override // miuix.appcompat.app.AppCompatActivity, androidx.activity.ComponentActivity, androidx.lifecycle.k
    @NotNull
    public /* bridge */ /* synthetic */ p0.a getDefaultViewModelCreationExtras() {
        return j.a(this);
    }

    @Override // com.miui.common.base.BaseActivity, miuix.autodensity.j
    public /* bridge */ /* synthetic */ int getRatioUiBaseWidthDp() {
        return i.a(this);
    }

    @Override // com.miui.common.base.BaseActivity, com.miui.common.base.c
    public /* bridge */ /* synthetic */ void onActivityCreate(Bundle bundle) {
        com.miui.common.base.b.a(this, bundle);
    }

    @Override // com.miui.common.base.BaseActivity, com.miui.common.base.c
    public /* bridge */ /* synthetic */ void onActivityDestroy() {
        com.miui.common.base.b.b(this);
    }

    public /* bridge */ /* synthetic */ void onActivityPause() {
        com.miui.common.base.b.c(this);
    }

    @Override // com.miui.common.base.BaseActivity, com.miui.common.base.c
    public /* bridge */ /* synthetic */ void onActivityResume() {
        com.miui.common.base.b.d(this);
    }

    @Override // com.miui.common.base.BaseActivity, com.miui.common.base.c
    public /* bridge */ /* synthetic */ void onActivityStart() {
        com.miui.common.base.b.e(this);
    }

    @Override // com.miui.common.base.BaseActivity, com.miui.common.base.c
    public /* bridge */ /* synthetic */ void onActivityStop() {
        com.miui.common.base.b.f(this);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.miui.common.base.BaseActivity, miuix.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.warning_center_pa_settings);
        if (Build.IS_INTERNATIONAL_BUILD) {
            finish();
            return;
        }
        if (bundle == null) {
            getSupportFragmentManager().m().e(R.id.content, SettingsFragment.class, null, null).k();
        }
        this.mHandler = new Handler();
        this.resultLauncher = registerForActivityResult(new c(), new androidx.activity.result.a() { // from class: com.miui.warningcenter.policeassist.a
            @Override // androidx.activity.result.a
            public final void a(Object obj) {
                this.f21915a.lambda$onCreate$0((ActivityResult) obj);
            }
        });
        this.mOpenPoliceAssist = (Button) findViewById(R.id.open_police_assist);
        this.mClosePoliceAssist = (Button) findViewById(R.id.close_police_assist);
        h0.e(this.mOpenPoliceAssist, 1.0f);
        h0.e(this.mClosePoliceAssist, 1.0f);
        ActionBar appCompatActionBar = getAppCompatActionBar();
        if (appCompatActionBar != null) {
            appCompatActionBar.registerCoordinatedScrollView(findViewById(R.id.content));
        }
        if (PaUtils.getPoliceAssistToggle(this)) {
            this.mClosePoliceAssist.setVisibility(0);
            this.mOpenPoliceAssist.setVisibility(8);
            checkPrivacyUpdate();
            onCustomizeActionBar(appCompatActionBar, true);
        } else {
            this.mClosePoliceAssist.setVisibility(8);
            this.mOpenPoliceAssist.setVisibility(0);
        }
        this.mOpenPoliceAssist.setOnClickListener(new View.OnClickListener() { // from class: com.miui.warningcenter.policeassist.PaSettingActivity.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                PaSettingActivity.this.openPoliceassistPrivacy();
            }
        });
        this.mClosePoliceAssist.setOnClickListener(new View.OnClickListener() { // from class: com.miui.warningcenter.policeassist.PaSettingActivity.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                PaSettingActivity.this.closePoliceAssist();
            }
        });
        AnalyticHelper.trackMainModuleShow(AnalyticHelper.WARNINGCENTER_PA);
    }

    protected void onCustomizeActionBar(ActionBar actionBar, boolean z10) {
        ImageView imageView;
        if (z10) {
            actionBar.setDisplayOptions(16, 16);
            imageView = new ImageView(this);
            imageView.setContentDescription(getString(R.string.more));
            imageView.setImageResource(R.drawable.ew_monitor_actionbar_more);
            imageView.setOnClickListener(new View.OnClickListener() { // from class: com.miui.warningcenter.policeassist.PaSettingActivity.3
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    PaSettingActivity.this.showPopupMenu(view);
                }
            });
        } else {
            imageView = null;
        }
        actionBar.setEndView(imageView);
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onPause() {
        super.onPause();
        AlertDialog alertDialog = this.mRevokeDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
            this.mRevokeDialog = null;
        }
        CountDownTimer countDownTimer = this.mCountdownTimer;
        if (countDownTimer != null) {
            countDownTimer.cancel();
            this.mCountdownTimer = null;
        }
    }
}
