package com.xiaomi.onetrack;

import android.text.TextUtils;

/* JADX INFO: loaded from: classes.dex */
public class Configuration {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private String f1832a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    private String f1833b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    private String f1834c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    private boolean f1835d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    private String f1836e;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    private OneTrack.Mode f1837f;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    private boolean f1838g;

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    private boolean f1839h;

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    private boolean f1840i;

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    private boolean f1841j;

    /* JADX INFO: renamed from: k, reason: collision with root package name */
    private boolean f1842k;

    /* JADX INFO: renamed from: l, reason: collision with root package name */
    private boolean f1843l;

    /* JADX INFO: renamed from: m, reason: collision with root package name */
    private String f1844m;

    /* JADX INFO: renamed from: n, reason: collision with root package name */
    private boolean f1845n;

    /* JADX INFO: renamed from: o, reason: collision with root package name */
    private String f1846o;

    /* JADX INFO: renamed from: p, reason: collision with root package name */
    private OneTrack.IEventHook f1847p;

    public static class Builder {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private String f1848a;

        /* JADX INFO: renamed from: b, reason: collision with root package name */
        private String f1849b;

        /* JADX INFO: renamed from: c, reason: collision with root package name */
        private String f1850c;

        /* JADX INFO: renamed from: d, reason: collision with root package name */
        private boolean f1851d;

        /* JADX INFO: renamed from: e, reason: collision with root package name */
        private String f1852e;

        /* JADX INFO: renamed from: m, reason: collision with root package name */
        private String f1860m;

        /* JADX INFO: renamed from: o, reason: collision with root package name */
        private String f1862o;

        /* JADX INFO: renamed from: f, reason: collision with root package name */
        private OneTrack.Mode f1853f = OneTrack.Mode.APP;

        /* JADX INFO: renamed from: g, reason: collision with root package name */
        private boolean f1854g = true;

        /* JADX INFO: renamed from: h, reason: collision with root package name */
        private boolean f1855h = true;

        /* JADX INFO: renamed from: i, reason: collision with root package name */
        private boolean f1856i = true;

        /* JADX INFO: renamed from: j, reason: collision with root package name */
        private boolean f1857j = false;

        /* JADX INFO: renamed from: k, reason: collision with root package name */
        private boolean f1858k = true;

        /* JADX INFO: renamed from: l, reason: collision with root package name */
        private boolean f1859l = false;

        /* JADX INFO: renamed from: n, reason: collision with root package name */
        private boolean f1861n = false;

        public Configuration build() {
            return new Configuration(this);
        }

        public Builder setAdEventAppId(String str) {
            this.f1862o = str;
            return this;
        }

        public Builder setAppId(String str) {
            this.f1848a = str;
            return this;
        }

        public Builder setAutoTrackActivityAction(boolean z6) {
            this.f1858k = z6;
            return this;
        }

        public Builder setChannel(String str) {
            this.f1850c = str;
            return this;
        }

        public Builder setExceptionCatcherEnable(boolean z6) {
            this.f1857j = z6;
            return this;
        }

        @Deprecated
        public Builder setGAIDEnable(boolean z6) {
            this.f1854g = z6;
            return this;
        }

        public Builder setImeiEnable(boolean z6) {
            this.f1856i = z6;
            return this;
        }

        public Builder setImsiEnable(boolean z6) {
            this.f1855h = z6;
            return this;
        }

        public Builder setInstanceId(String str) {
            this.f1860m = str;
            return this;
        }

        public Builder setInternational(boolean z6) {
            this.f1851d = z6;
            return this;
        }

        public Builder setMode(OneTrack.Mode mode) {
            this.f1853f = mode;
            return this;
        }

        public Builder setOverrideMiuiRegionSetting(boolean z6) {
            this.f1859l = z6;
            return this;
        }

        public Builder setPluginId(String str) {
            this.f1849b = str;
            return this;
        }

        public Builder setRegion(String str) {
            this.f1852e = str;
            return this;
        }

        public Builder setUseCustomPrivacyPolicy(boolean z6) {
            this.f1861n = z6;
            return this;
        }
    }

    private String a(String str) {
        StringBuilder sb = new StringBuilder();
        if (TextUtils.isEmpty(str) || str.length() <= 4) {
            sb.append(str);
        } else {
            for (int i6 = 0; i6 < str.length(); i6++) {
                if (i6 == 0 || i6 == 1 || i6 == str.length() - 2 || i6 == str.length() - 1) {
                    sb.append(str.charAt(i6));
                } else {
                    sb.append("*");
                }
            }
        }
        return sb.toString();
    }

    public String getAdEventAppId() {
        return this.f1846o;
    }

    public String getAppId() {
        return this.f1832a;
    }

    public String getChannel() {
        return this.f1834c;
    }

    public String getInstanceId() {
        return this.f1844m;
    }

    public OneTrack.Mode getMode() {
        return this.f1837f;
    }

    public String getPluginId() {
        return this.f1833b;
    }

    public String getRegion() {
        return this.f1836e;
    }

    public boolean isAutoTrackActivityAction() {
        return this.f1842k;
    }

    public boolean isExceptionCatcherEnable() {
        return this.f1841j;
    }

    @Deprecated
    public boolean isGAIDEnable() {
        return this.f1838g;
    }

    public boolean isIMEIEnable() {
        return this.f1840i;
    }

    public boolean isIMSIEnable() {
        return this.f1839h;
    }

    public boolean isInternational() {
        return this.f1835d;
    }

    public boolean isOverrideMiuiRegionSetting() {
        return this.f1843l;
    }

    public boolean isUseCustomPrivacyPolicy() {
        return this.f1845n;
    }

    public String toString() {
        try {
            return "Configuration{appId='" + a(this.f1832a) + "', pluginId='" + a(this.f1833b) + "', channel='" + this.f1834c + "', international=" + this.f1835d + ", region='" + this.f1836e + "', overrideMiuiRegionSetting=" + this.f1843l + ", mode=" + this.f1837f + ", GAIDEnable=" + this.f1838g + ", IMSIEnable=" + this.f1839h + ", IMEIEnable=" + this.f1840i + ", ExceptionCatcherEnable=" + this.f1841j + ", instanceId=" + a(this.f1844m) + '}';
        } catch (Exception unused) {
            return "";
        }
    }

    private Configuration(Builder builder) {
        this.f1837f = OneTrack.Mode.APP;
        this.f1838g = true;
        this.f1839h = true;
        this.f1840i = true;
        this.f1842k = true;
        this.f1843l = false;
        this.f1845n = false;
        this.f1832a = builder.f1848a;
        this.f1833b = builder.f1849b;
        this.f1834c = builder.f1850c;
        this.f1835d = builder.f1851d;
        this.f1836e = builder.f1852e;
        this.f1837f = builder.f1853f;
        this.f1838g = builder.f1854g;
        this.f1840i = builder.f1856i;
        this.f1839h = builder.f1855h;
        this.f1841j = builder.f1857j;
        this.f1842k = builder.f1858k;
        this.f1843l = builder.f1859l;
        this.f1844m = builder.f1860m;
        this.f1845n = builder.f1861n;
        this.f1846o = builder.f1862o;
    }
}
