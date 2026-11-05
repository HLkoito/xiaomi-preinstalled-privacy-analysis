package x4;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.util.Log;

/* JADX INFO: loaded from: classes2.dex */
public class u1 {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private static int f37033a = -1;

    public static long a() {
        try {
            StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
            return statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
        } catch (Exception e10) {
            Log.e("StorageUtils", "get available space failed", e10);
            return 0L;
        }
    }

    public static int b(Context context) {
        if (f37033a >= 0) {
            Log.d("StorageUtils", "ufs size = " + f37033a);
            return f37033a;
        }
        if (context == null) {
            return 0;
        }
        try {
            f37033a = ((Integer) bh.f.d((StorageManager) context.getSystemService("storage"), "getEUASupportSize", null, new Object[0])).intValue();
        } catch (Exception unused) {
        }
        if (f37033a < 0) {
            f37033a = 0;
        }
        return f37033a;
    }

    public static long c(Context context) {
        try {
            return Build.VERSION.SDK_INT > 24 ? ((Long) bh.f.d((StorageManager) context.getSystemService("storage"), "getPrimaryStorageSize", null, new Object[0])).longValue() : d(Environment.getDataDirectory().getTotalSpace() + Environment.getRootDirectory().getTotalSpace());
        } catch (Exception unused) {
            return 0L;
        }
    }

    public static long d(long j10) {
        long j11 = 1;
        long j12 = 1;
        while (true) {
            long j13 = j11 * j12;
            if (j13 >= j10) {
                return j13;
            }
            j11 <<= 1;
            if (j11 > 512) {
                j12 *= 1000;
                j11 = 1;
            }
        }
    }
}
