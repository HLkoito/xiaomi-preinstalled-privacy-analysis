package com.miui.monthreport;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.miui.luckymoney.config.Constants;
import ef.x;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import miui.os.Build;
import org.json.JSONException;
import org.json.JSONObject;
import p4.i;
import x4.u;

/* JADX INFO: loaded from: classes2.dex */
public class c {

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    private static final String f16658g = "c";

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    private static c f16659h;

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    private static final Executor f16660i = Executors.newSingleThreadExecutor();

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private Context f16662b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private Exception f16663c = null;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private List<String> f16664d = null;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    private volatile boolean f16665e = false;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    private Handler f16666f = new a();

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private com.miui.monthreport.a f16661a = com.miui.monthreport.a.f();

    class a extends Handler {
        a() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            Log.i(c.f16658g, "Handle task message : " + message.what);
            switch (message.what) {
                case 101:
                    Object obj = message.obj;
                    if (obj != null && (obj instanceof String)) {
                        c.this.n((String) obj, null);
                        break;
                    }
                    break;
                case 102:
                    Object obj2 = message.obj;
                    if (obj2 != null && (obj2 instanceof com.miui.monthreport.b)) {
                        com.miui.monthreport.b bVar = (com.miui.monthreport.b) obj2;
                        c.this.n(bVar.d(), bVar);
                        break;
                    }
                    break;
                case 103:
                    c.this.q();
                    break;
            }
        }
    }

    class b implements Runnable {
        b() {
        }

        @Override // java.lang.Runnable
        public void run() {
            if (c.this.f16665e) {
                return;
            }
            c.this.f16665e = true;
            new AsyncTaskC0199c(c.this, null).executeOnExecutor(c.f16660i, new Void[0]);
        }
    }

    /* JADX INFO: renamed from: com.miui.monthreport.c$c, reason: collision with other inner class name */
    private class AsyncTaskC0199c extends AsyncTask<Void, Void, List<String>> {
        private AsyncTaskC0199c() {
        }

        /* synthetic */ AsyncTaskC0199c(c cVar, a aVar) {
            this();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        /* JADX INFO: renamed from: a, reason: merged with bridge method [inline-methods] */
        public List<String> doInBackground(Void... voidArr) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(5, -30);
            int iD = c.this.f16661a.d(calendar.getTimeInMillis());
            Log.i(c.f16658g, "Old data cleaned : " + iD);
            return c.this.f16661a.g();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        /* JADX INFO: renamed from: b, reason: merged with bridge method [inline-methods] */
        public void onPostExecute(List<String> list) {
            super.onPostExecute(list);
            c.this.f16664d = list;
            c.this.q();
        }
    }

    private class d extends AsyncTask<String, Void, Integer> {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private com.miui.monthreport.b f16670a;

        public d(com.miui.monthreport.b bVar) {
            this.f16670a = bVar;
        }

        private boolean a(String str) {
            if (TextUtils.isEmpty(str)) {
                return false;
            }
            JSONObject jSONObject = new JSONObject(str);
            if (!jSONObject.has("error_code")) {
                c cVar = c.this;
                cVar.f16663c = cVar.f16661a.c(this.f16670a.b());
                Log.i(c.f16658g, "Upload successfully");
                return true;
            }
            Log.i(c.f16658g, "Error : " + jSONObject.optString("error", "Unknown error"));
            return false;
        }

        private boolean b() {
            return x.z() && u.b(c.this.f16662b) && ma.c.a();
        }

        private boolean d(String str) throws JSONException {
            int iE;
            JSONObject jSONObject = new JSONObject();
            jSONObject.put(Constants.JSON_KEY_MODULE, str);
            String strC = p4.c.c(c.this.f16662b, "https://api.sec.miui.com/data/check", jSONObject, "5fdd8678-cddf-4269-bb73-48187445bba7", new i("monthreport_taskmanager"));
            Log.d(c.f16658g, "Available : " + strC);
            if (TextUtils.isEmpty(strC)) {
                return false;
            }
            JSONObject jSONObject2 = new JSONObject(strC);
            if (jSONObject2.has("error_code")) {
                return false;
            }
            if (!(jSONObject2.optInt("code", 0) == 1)) {
                return false;
            }
            if (!u.a(c.this.f16662b) || ((iE = c.this.f16661a.e(str)) > 0 && iE < 20000)) {
                return true;
            }
            Log.d(c.f16658g, "Rejected : many events upload in mobile network");
            return false;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        /* JADX INFO: renamed from: c, reason: merged with bridge method [inline-methods] */
        public Integer doInBackground(String... strArr) {
            if (strArr.length < 0) {
                return 103;
            }
            try {
                String str = strArr[0];
                if (this.f16670a == null) {
                    Log.i(c.f16658g, String.format("Module %s task is null.", str));
                    this.f16670a = com.miui.monthreport.b.e(str, c.this.f16663c);
                }
                String strI = this.f16670a.i();
                if (strI == null) {
                    Log.i(c.f16658g, String.format("Module %s has no data.", str));
                    return 103;
                }
                if (b()) {
                    if (!d(str)) {
                        return 103;
                    }
                    Log.i(c.f16658g, "Uploading " + this.f16670a.toString());
                    if (a(p4.c.b(c.this.f16662b, "https://data.sec.miui.com/data/upload", this.f16670a.j(), strI, "5fdd8678-cddf-4269-bb73-48187445bba7", new i("monthreport_taskmanager_uploadtask")))) {
                        return 101;
                    }
                }
                return 102;
            } catch (Exception e10) {
                e10.printStackTrace();
            }
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        /* JADX INFO: renamed from: e, reason: merged with bridge method [inline-methods] */
        public void onPostExecute(Integer num) {
            super.onPostExecute(num);
            if (num != null) {
                switch (num.intValue()) {
                    case 101:
                        c.this.f16666f.sendMessageDelayed(c.this.f16666f.obtainMessage(101, this.f16670a.d()), 300000L);
                        break;
                    case 102:
                        com.miui.monthreport.b bVar = this.f16670a;
                        if (bVar != null) {
                            bVar.k();
                            if (this.f16670a.c() < 3) {
                                c.this.f16666f.sendMessageDelayed(c.this.f16666f.obtainMessage(102, this.f16670a), ((long) this.f16670a.c()) * 300000);
                            }
                        }
                        break;
                    case 103:
                        c.this.f16666f.sendEmptyMessage(103);
                        break;
                }
            }
        }
    }

    private c(Context context) {
        this.f16662b = context.getApplicationContext();
    }

    public static synchronized c m(Context context) {
        if (f16659h == null) {
            f16659h = new c(context);
        }
        return f16659h;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void n(String str, com.miui.monthreport.b bVar) {
        this.f16666f.removeMessages(102);
        this.f16666f.removeMessages(101);
        new d(bVar).executeOnExecutor(f16660i, str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void q() {
        List<String> list = this.f16664d;
        if (list == null || list.isEmpty()) {
            Log.i(f16658g, "Module is null");
            this.f16665e = false;
        } else {
            String str = this.f16664d.get(0);
            this.f16664d.remove(0);
            n(str, null);
        }
    }

    public void o() {
        this.f16666f.post(new b());
    }

    public void p() {
        if (Build.IS_INTERNATIONAL_BUILD) {
            return;
        }
        m(this.f16662b).o();
    }
}
