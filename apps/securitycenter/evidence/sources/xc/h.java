package xc;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import com.miui.permcenter.model.LocalAppInfoBean;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes3.dex */
public class h {
    public static List<LocalAppInfoBean> a(Context context) {
        ArrayList arrayList = new ArrayList();
        PackageManager packageManager = context.getPackageManager();
        for (PackageInfo packageInfo : packageManager.getInstalledPackages(0)) {
            if ((packageInfo.applicationInfo.flags & 1) == 0) {
                LocalAppInfoBean localAppInfoBean = new LocalAppInfoBean();
                localAppInfoBean.name = packageInfo.applicationInfo.loadLabel(packageManager).toString();
                localAppInfoBean.packageName = packageInfo.packageName;
                localAppInfoBean.uid = packageInfo.applicationInfo.uid;
                localAppInfoBean.versionCode = Build.VERSION.SDK_INT >= 28 ? packageInfo.getLongVersionCode() : packageInfo.versionCode;
                localAppInfoBean.iconUrl = "pkg_icon://".concat(packageInfo.packageName);
                arrayList.add(localAppInfoBean);
            }
        }
        return arrayList;
    }
}
