package ub;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.IBinder;
import android.text.TextUtils;
import bh.f;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.miui.permcenter.detection.model.RiskAppInfoBean;
import com.miui.permcenter.model.LocalAppInfoBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xc.h;

/* JADX INFO: loaded from: classes2.dex */
public class d extends bc.a<List<RiskAppInfoBean>> {

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    @SuppressLint({"StaticFieldLeak"})
    private final Context f35806c;

    class a extends TypeToken<HashMap<String, String>> {
        a() {
        }
    }

    public d(Context context, bc.a.InterfaceC0069a<List<RiskAppInfoBean>> interfaceC0069a) {
        super(interfaceC0069a);
        this.f35806c = context.getApplicationContext();
    }

    private Object e() {
        try {
            return f.h(Class.forName("android.content.pm.IPackageManager$Stub"), "asInterface", new Class[]{IBinder.class}, (IBinder) f.h(Class.forName("android.os.ServiceManager"), "getService", new Class[]{String.class}, "package"));
        } catch (Exception e10) {
            e10.printStackTrace();
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // bc.a
    /* JADX INFO: renamed from: d, reason: merged with bridge method [inline-methods] */
    public List<RiskAppInfoBean> a() {
        if (isCancelled()) {
            return new ArrayList();
        }
        List<LocalAppInfoBean> listA = h.a(this.f35806c);
        String strC = ac.c.c(ac.c.d(listA));
        if (!TextUtils.isEmpty(strC)) {
            try {
                return RiskAppInfoBean.getRiskList(listA, new ArrayList(((HashMap) new Gson().fromJson(strC, new a().getType())).keySet()), e());
            } catch (Exception e10) {
                e10.printStackTrace();
            }
        }
        return new ArrayList();
    }
}
