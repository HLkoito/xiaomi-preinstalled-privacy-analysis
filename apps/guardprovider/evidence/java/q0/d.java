package q0;

import com.miui.guardprovider.sdk.android.pojo.networkdomain.CloudScanApkInfo;
import com.miui.guardprovider.sdk.android.pojo.networkdomain.CloudScanResponse;
import f6.u;
import java.util.List;
import java.util.Map;
import kotlin.Metadata;

/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J9\u0010\t\u001a\u00020\b2\u000e\b\u0001\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00030\u00022\u0014\b\u0001\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00060\u0005H§@ø\u0001\u0000¢\u0006\u0004\b\t\u0010\nø\u0001\u0001\u0082\u0002\n\n\u0002\b\u0019\n\u0004\b!0\u0001¨\u0006\u000bÀ\u0006\u0001"}, d2 = {"Lq0/d;", "", "", "Lcom/miui/guardprovider/sdk/android/pojo/networkdomain/CloudScanApkInfo;", "body", "", "", "query", "Lcom/miui/guardprovider/sdk/android/pojo/networkdomain/CloudScanResponse;", "a", "(Ljava/util/List;Ljava/util/Map;Lk1/d;)Ljava/lang/Object;", "flamingo-android_release"}, k = 1, mv = {1, 9, 0})
public interface d {
    @f6.o("/virus/scan")
    Object a(@f6.a List<CloudScanApkInfo> list, @u Map<String, String> map, k1.d<? super CloudScanResponse> dVar);
}
