package com.miui.privacyapps;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import ck.g;
import ck.m;
import com.miui.permission.PermissionContract;
import miui.security.SecurityManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: classes3.dex */
public final class PrivacyAppsProvider extends ContentProvider {

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    @NotNull
    public static final a f20131b = new a(null);

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    @Nullable
    private SecurityManager f20132a;

    public static final class a {
        private a() {
        }

        public /* synthetic */ a(g gVar) {
            this();
        }
    }

    @Override // android.content.ContentProvider
    @Nullable
    public Bundle call(@NotNull String str, @Nullable String str2, @Nullable Bundle bundle) {
        m.e(str, PermissionContract.Method.ReadClipboardTipSetting.EXTRA_METHOD);
        if (!m.a(str, "isPrivacyApp")) {
            return null;
        }
        String string = bundle != null ? bundle.getString("packageName") : null;
        Integer numValueOf = bundle != null ? Integer.valueOf(bundle.getInt("userId")) : null;
        if ((string == null || string.length() == 0) || numValueOf == null) {
            return null;
        }
        SecurityManager securityManager = this.f20132a;
        m.b(securityManager);
        boolean zIsPrivacyApp = securityManager.isPrivacyApp(string, numValueOf.intValue());
        Bundle bundle2 = new Bundle();
        bundle2.putBoolean("result", zIsPrivacyApp);
        return bundle2;
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
        Context context = getContext();
        this.f20132a = (SecurityManager) (context != null ? context.getSystemService("security") : null);
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
        return -1;
    }
}
