package com.miui.permcenter.privacycenter;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import ck.g;
import ck.m;
import com.miui.permission.PermissionContract;
import kc.q;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qc.e;
import xc.k;

/* JADX INFO: loaded from: classes2.dex */
public final class PrivacyCenterProvider extends ContentProvider {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    @NotNull
    public static final a f18433a = new a(null);

    public static final class a {
        private a() {
        }

        public /* synthetic */ a(g gVar) {
            this();
        }
    }

    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    @Override // android.content.ContentProvider
    @Nullable
    public Bundle call(@NotNull String str, @Nullable String str2, @Nullable Bundle bundle) {
        m.e(str, PermissionContract.Method.ReadClipboardTipSetting.EXTRA_METHOD);
        switch (str.hashCode()) {
            case -893214752:
                if (str.equals("setCTAStatus")) {
                    e eVar = e.f33671a;
                    String callingPackage = getCallingPackage();
                    if (callingPackage == null) {
                        callingPackage = "";
                    }
                    eVar.k(callingPackage, str2 != null ? Boolean.parseBoolean(str2) : true);
                }
                break;
            case -784237589:
                if (str.equals("queryShowCTA")) {
                    return e.f33671a.z();
                }
                break;
            case 407266212:
                if (str.equals("privacy_provider_get_using")) {
                    if (Binder.getCallingUid() == Process.myUid()) {
                        return q.G(getContext()).H();
                    }
                    throw new SecurityException("Permission denied");
                }
                break;
            case 1759499834:
                if (str.equals("queryCTAStatus")) {
                    String callingPackage2 = getCallingPackage();
                    if (callingPackage2 == null) {
                        return null;
                    }
                    if (Binder.getCallingUid() <= 2000) {
                        if (str2 == null) {
                            str2 = callingPackage2;
                        }
                        callingPackage2 = str2;
                    }
                    return e.f33671a.A(callingPackage2);
                }
                break;
        }
        return super.call(str, str2, bundle);
    }

    @Override // android.content.ContentProvider
    public int delete(@NotNull Uri uri, @Nullable String str, @Nullable String[] strArr) {
        m.e(uri, "uri");
        return 0;
    }

    @Override // android.content.ContentProvider
    @Nullable
    public String getType(@NotNull Uri uri) {
        m.e(uri, "uri");
        return null;
    }

    @Override // android.content.ContentProvider
    @Nullable
    public Uri insert(@NotNull Uri uri, @Nullable ContentValues contentValues) {
        m.e(uri, "uri");
        return null;
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        k.a.f37233a.g();
        e.f33671a.x();
        return true;
    }

    @Override // android.content.ContentProvider
    @Nullable
    public Cursor query(@NotNull Uri uri, @Nullable String[] strArr, @Nullable String str, @Nullable String[] strArr2, @Nullable String str2) {
        m.e(uri, "uri");
        return null;
    }

    @Override // android.content.ContentProvider
    public int update(@NotNull Uri uri, @Nullable ContentValues contentValues, @Nullable String str, @Nullable String[] strArr) {
        m.e(uri, "uri");
        return 0;
    }
}
