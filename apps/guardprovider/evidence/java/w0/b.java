package w0;

import android.content.Context;
import android.text.TextUtils;
import java.util.UUID;

/* JADX INFO: loaded from: classes.dex */
public abstract class b {
    public static String a(Context context) {
        if (!v.a.c()) {
            return "";
        }
        String strA = v.a.a(context);
        return TextUtils.isEmpty(strA) ? "" : strA;
    }

    public static String b() {
        f fVarA = f.a();
        String strF = fVarA.f("key_uuid");
        if (!TextUtils.isEmpty(strF)) {
            return strF;
        }
        String string = UUID.randomUUID().toString();
        fVarA.j("key_uuid", string);
        return string;
    }
}
