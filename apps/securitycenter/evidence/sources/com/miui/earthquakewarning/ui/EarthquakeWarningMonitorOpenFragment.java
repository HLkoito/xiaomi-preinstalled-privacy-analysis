package com.miui.earthquakewarning.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.FragmentManager;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.text.ttml.TtmlNode;
import com.google.common.base.Ascii;
import com.miui.earthquakewarning.Constants;
import com.miui.earthquakewarning.analytics.AnalyticHelper;
import com.miui.earthquakewarning.service.EarthquakeWarningService;
import com.miui.earthquakewarning.utils.Utils;
import com.miui.securitycenter.Application;
import com.miui.securitycenter.R;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import miuix.animation.Folme;
import miuix.animation.base.AnimConfig;
import miuix.appcompat.app.Fragment;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public class EarthquakeWarningMonitorOpenFragment extends Fragment {
    private static final String TAG = "EarthquakeMonitor";
    private jf.e binding;
    private TextView mOpenText;
    private CheckBox mPrivacyCheckBox;

    private static class PrivacyAgreeRunnable implements Runnable {
        private PrivacyAgreeRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            Log.w(EarthquakeWarningMonitorOpenFragment.TAG, "earthquake monitor open agree = " + ue.e.b(Application.A(), Utils.EARTHQUAKE_MONITOR_POLICE_NAME, ef.w.a(Application.A())));
        }
    }

    private static class RequestVolunteerDataTask extends AsyncTask<String, Void, String> {
        private final WeakReference<EarthquakeWarningMonitorOpenFragment> fragmentRef;

        public RequestVolunteerDataTask(EarthquakeWarningMonitorOpenFragment earthquakeWarningMonitorOpenFragment) {
            this.fragmentRef = new WeakReference<>(earthquakeWarningMonitorOpenFragment);
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public String doInBackground(String... strArr) {
            if (ef.x.z()) {
                return eg.j.r(new HashMap(), Constants.REQUEST_MONITOR_VOLUNTEERS_COUNT_URL, Constants.UUID_MONITOR_DATA_UPLOAD, new p4.i("ew_requestareacode"));
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(String str) {
            String str2;
            EarthquakeWarningMonitorOpenFragment earthquakeWarningMonitorOpenFragment = this.fragmentRef.get();
            if (earthquakeWarningMonitorOpenFragment == null || TextUtils.isEmpty(str)) {
                return;
            }
            try {
                JSONObject jSONObject = new JSONObject(str);
                int iOptInt = jSONObject.optInt("code");
                String strOptString = jSONObject.optString("status");
                String strOptString2 = jSONObject.optString("data");
                if (iOptInt == 200 && "success".equals(strOptString)) {
                    if (TextUtils.isEmpty(strOptString2)) {
                        str2 = "00000000";
                    } else {
                        int length = 8 - strOptString2.length();
                        StringBuilder sb2 = new StringBuilder();
                        for (int i10 = 0; i10 < length; i10++) {
                            sb2.append("0");
                        }
                        str2 = ((Object) sb2) + strOptString2;
                    }
                    earthquakeWarningMonitorOpenFragment.showJoinNumber(str2);
                }
            } catch (Exception e10) {
                Log.e(EarthquakeWarningMonitorOpenFragment.TAG, "RequestVolunteerDataTask error: ", e10);
            }
        }
    }

    private static class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String str) {
            super(str);
        }

        @Override // android.text.style.ClickableSpan, android.text.style.CharacterStyle
        public void updateDrawState(TextPaint textPaint) {
            super.updateDrawState(textPaint);
            textPaint.setUnderlineText(false);
        }
    }

    private static class VolunteerRegisterTask extends AsyncTask<String, Void, String> {
        private final WeakReference<EarthquakeWarningMonitorOpenFragment> fragmentRef;

        public VolunteerRegisterTask(EarthquakeWarningMonitorOpenFragment earthquakeWarningMonitorOpenFragment) {
            this.fragmentRef = new WeakReference<>(earthquakeWarningMonitorOpenFragment);
        }

        private String binToHex(byte[] bArr) {
            char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
            StringBuilder sb2 = new StringBuilder();
            for (byte b10 : bArr) {
                int i10 = (b10 >> 4) & 15;
                int i11 = b10 & Ascii.SI;
                sb2.append(cArr[i10]);
                sb2.append(cArr[i11]);
            }
            return sb2.toString();
        }

        private String sign(String str) {
            try {
                return binToHex(x4.z0.g().o(str.getBytes(C.UTF8_NAME), true)).toLowerCase();
            } catch (Exception e10) {
                Log.e("TAG", e10.toString());
                return str;
            }
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public String doInBackground(String... strArr) {
            EarthquakeWarningMonitorOpenFragment earthquakeWarningMonitorOpenFragment;
            if (!ef.x.z() || (earthquakeWarningMonitorOpenFragment = this.fragmentRef.get()) == null) {
                return null;
            }
            try {
                HashMap map = new HashMap();
                String strA = ef.w.a(earthquakeWarningMonitorOpenFragment.getActivity());
                String strF = x4.z0.g().f();
                map.put("oaid", strA);
                map.put("fid", strF);
                map.put("tzSign", sign(strA));
                return eg.j.r(map, Constants.REQUEST_MONITOR_VOLUNTEERS_REGISTER_URL, Constants.UUID_MONITOR_DATA_UPLOAD, new p4.i("ew_requestareacode"));
            } catch (Exception e10) {
                Log.e(EarthquakeWarningMonitorOpenFragment.TAG, "build data error", e10);
                return null;
            }
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(String str) {
            EarthquakeWarningMonitorOpenFragment earthquakeWarningMonitorOpenFragment = this.fragmentRef.get();
            if (earthquakeWarningMonitorOpenFragment == null || TextUtils.isEmpty(str)) {
                return;
            }
            try {
                JSONObject jSONObject = new JSONObject(str);
                int iOptInt = jSONObject.optInt("code");
                String strOptString = jSONObject.optString("status");
                String strOptString2 = jSONObject.optString("data");
                if (iOptInt != 200 || !"success".equals(strOptString) || TextUtils.isEmpty(strOptString2)) {
                    throw new IllegalStateException("Register failed.");
                }
                JSONObject jSONObject2 = new JSONObject(strOptString2);
                long jOptLong = jSONObject2.optLong("num");
                String strOptString3 = jSONObject2.optString("token");
                if (TextUtils.isEmpty(strOptString3)) {
                    strOptString3 = "";
                }
                Utils.setEarthquakeVolunteerToken(strOptString3);
                Utils.setEarthquakeMonitorOrder(String.valueOf(jOptLong));
                Utils.openEarthquakeMonitor(true);
                if (earthquakeWarningMonitorOpenFragment.getActivity() != null) {
                    Intent intent = new Intent(earthquakeWarningMonitorOpenFragment.getActivity(), (Class<?>) EarthquakeWarningService.class);
                    intent.setAction(EarthquakeWarningService.ACTION_JOIN_VOLUNTEER);
                    earthquakeWarningMonitorOpenFragment.getActivity().startService(intent);
                    earthquakeWarningMonitorOpenFragment.showShareFragment();
                }
            } catch (Exception e10) {
                Toast.makeText(Application.A(), R.string.ew_monitor_bad_request_tips, 0).show();
                Log.e(EarthquakeWarningMonitorOpenFragment.TAG, "VolunteerRegisterTask error: ", e10);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onInflateView$0(View view) {
        requireActivity().finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onInflateView$1(CompoundButton compoundButton, boolean z10) {
        this.mPrivacyCheckBox.setChecked(z10);
        this.mOpenText.setEnabled(z10);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onInflateView$2(View view) {
        if (this.mPrivacyCheckBox.isChecked()) {
            if (TextUtils.isEmpty(Utils.getEarthquakeMonitorOrder())) {
                new VolunteerRegisterTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[0]);
            } else {
                Utils.openEarthquakeMonitor(true);
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), (Class<?>) EarthquakeWarningService.class);
                    intent.setAction(EarthquakeWarningService.ACTION_JOIN_VOLUNTEER);
                    getActivity().startService(intent);
                    showShareFragment();
                }
            }
            com.miui.common.base.asyn.a.a(new PrivacyAgreeRunnable());
            AnalyticHelper.trackMonitorClickActionModuleClick("join");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showJoinNumber(String str) {
        this.binding.f27876e.setText(String.valueOf(str.charAt(0)));
        this.binding.f27877f.setText(String.valueOf(str.charAt(1)));
        this.binding.f27878g.setText(String.valueOf(str.charAt(2)));
        this.binding.f27879h.setText(String.valueOf(str.charAt(3)));
        this.binding.f27880i.setText(String.valueOf(str.charAt(4)));
        this.binding.f27881j.setText(String.valueOf(str.charAt(5)));
        this.binding.f27882k.setText(String.valueOf(str.charAt(6)));
        this.binding.f27883l.setText(String.valueOf(str.charAt(7)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showShareFragment() {
        EarthquakeWarningMonitorShareFragment earthquakeWarningMonitorShareFragment = new EarthquakeWarningMonitorShareFragment();
        FragmentManager supportFragmentManager = requireActivity().getSupportFragmentManager();
        androidx.fragment.app.s sVarM = supportFragmentManager.m();
        earthquakeWarningMonitorShareFragment.setArguments(new Bundle());
        if (supportFragmentManager.g0(R.id.share_content) == null) {
            sVarM.c(R.id.share_content, earthquakeWarningMonitorShareFragment, EarthquakeWarningMonitorActivity.FRAGMENT_TAG_EEW_MONITOR_SHARE);
            sVarM.l();
            androidx.fragment.app.s sVarM2 = supportFragmentManager.m();
            sVarM2.u(this);
            sVarM2.l();
        }
    }

    private void stripUnderlines(TextView textView) {
        Spannable spannable = (Spannable) textView.getText();
        for (URLSpan uRLSpan : (URLSpan[]) spannable.getSpans(0, spannable.length(), URLSpan.class)) {
            int spanStart = spannable.getSpanStart(uRLSpan);
            int spanEnd = spannable.getSpanEnd(uRLSpan);
            spannable.removeSpan(uRLSpan);
            spannable.setSpan(new URLSpanNoUnderline(uRLSpan.getURL()), spanStart, spanEnd, 0);
        }
        textView.setText(spannable);
    }

    @Override // miuix.appcompat.app.Fragment, androidx.fragment.app.Fragment, androidx.lifecycle.k
    @NotNull
    public /* bridge */ /* synthetic */ p0.a getDefaultViewModelCreationExtras() {
        return androidx.lifecycle.j.a(this);
    }

    @Override // miuix.appcompat.app.Fragment, androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setThemeRes(R.style.EarthquakeMonitorTheme);
    }

    @Override // miuix.appcompat.app.Fragment, androidx.fragment.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        this.binding = null;
    }

    @Override // miuix.appcompat.app.Fragment, miuix.appcompat.app.IFragment
    public View onInflateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.earthquake_warning_fragment_monitor_open, viewGroup, false);
        this.binding = jf.e.a(viewInflate);
        TextView textView = (TextView) viewInflate.findViewById(R.id.btn_open);
        this.mOpenText = textView;
        x4.h0.e(textView, 1.0f);
        viewInflate.findViewById(R.id.monitor_back).setOnClickListener(new View.OnClickListener() { // from class: com.miui.earthquakewarning.ui.s0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f13160a.lambda$onInflateView$0(view);
            }
        });
        TextView textView2 = (TextView) viewInflate.findViewById(R.id.privacy_text);
        textView2.setText(androidx.core.text.e.a(getString(R.string.ew_monitor_read_privacy, Constants.URL_EARTHQUAKE_MONITOR_PRIVACY + Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry()), 63));
        textView2.setMovementMethod(LinkMovementMethod.getInstance());
        stripUnderlines(textView2);
        CheckBox checkBox = (CheckBox) viewInflate.findViewById(R.id.privacy_checkbox);
        this.mPrivacyCheckBox = checkBox;
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.miui.earthquakewarning.ui.t0
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z10) {
                this.f13162a.lambda$onInflateView$1(compoundButton, z10);
            }
        });
        this.mOpenText.setOnClickListener(new View.OnClickListener() { // from class: com.miui.earthquakewarning.ui.u0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f13164a.lambda$onInflateView$2(view);
            }
        });
        this.mOpenText.setEnabled(this.mPrivacyCheckBox.isChecked());
        Folme.useAt(this.mOpenText).touch().handleTouchOf(this.mOpenText, new AnimConfig[0]);
        new RequestVolunteerDataTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[0]);
        AnalyticHelper.trackMonitorShowActionModuleClick(TtmlNode.TEXT_EMPHASIS_MARK_OPEN);
        return viewInflate;
    }
}
