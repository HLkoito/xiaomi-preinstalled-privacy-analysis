.class public Le0/r;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Le0/c2$c;


# instance fields
.field private a:Landroid/telephony/TelephonyManager;


# direct methods
.method public constructor <init>()V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const/4 v0, 0x0

    iput-object v0, p0, Le0/r;->a:Landroid/telephony/TelephonyManager;

    return-void
.end method

.method public static e(I)I
    .locals 5

    const-string v0, "android.telephony.SubscriptionManager"

    invoke-static {v0}, Lf0/o;->a(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v0

    const/4 v1, 0x1

    new-array v2, v1, [Ljava/lang/Class;

    sget-object v3, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    const/4 v4, 0x0

    aput-object v3, v2, v4

    const-string v3, "getSubId"

    invoke-static {v0, v3, v2}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    new-array v1, v1, [Ljava/lang/Object;

    invoke-static {p0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object p0

    aput-object p0, v1, v4

    const/4 p0, 0x0

    invoke-static {v2, p0, v1}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, [I

    const-string v2, "getDefaultSubId"

    new-array v3, v4, [Ljava/lang/Class;

    invoke-static {v0, v2, v3}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v0

    new-array v2, v4, [Ljava/lang/Object;

    invoke-static {v0, p0, v2}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Ljava/lang/Integer;

    invoke-virtual {p0}, Ljava/lang/Integer;->intValue()I

    move-result p0

    if-eqz v1, :cond_0

    aget p0, v1, v4

    :cond_0
    return p0
.end method

.method public static h(I)I
    .locals 6

    const-string v0, "android.telephony.SubscriptionManager"

    invoke-static {v0}, Lf0/o;->a(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v0

    sget v1, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v2, 0x21

    const/4 v3, 0x0

    const/4 v4, 0x1

    const/4 v5, 0x0

    if-gt v1, v2, :cond_1

    new-array v1, v4, [Ljava/lang/Class;

    sget-object v2, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    aput-object v2, v1, v5

    const-string v2, "getSubId"

    invoke-static {v0, v2, v1}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v0

    new-array v1, v4, [Ljava/lang/Object;

    invoke-static {p0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object p0

    aput-object p0, v1, v5

    invoke-static {v0, v3, v1}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    check-cast p0, [I

    if-eqz p0, :cond_0

    aget p0, p0, v5

    return p0

    :cond_0
    const/4 p0, -0x1

    return p0

    :cond_1
    new-array v1, v4, [Ljava/lang/Class;

    sget-object v2, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    aput-object v2, v1, v5

    const-string v2, "getSubscriptionId"

    invoke-static {v0, v2, v1}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v0

    new-array v1, v4, [Ljava/lang/Object;

    invoke-static {p0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object p0

    aput-object p0, v1, v5

    invoke-static {v0, v3, v1}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Ljava/lang/Integer;

    invoke-virtual {p0}, Ljava/lang/Integer;->intValue()I

    move-result p0

    return p0
.end method

.method public static declared-synchronized k(Landroid/content/Context;)V
    .locals 6

    const-class v0, Le0/r;

    monitor-enter v0

    :try_start_0
    const-string v1, "DeviceInfoReaderBase"

    const-string v2, "listenNetworkType---->"

    invoke-static {v1, v2}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const/4 v1, 0x0

    invoke-static {v1}, Le0/r;->h(I)I

    move-result v2

    const v3, 0x100500

    const/16 v4, 0x1f

    if-lez v2, :cond_1

    sget-object v5, Lf0/f;->h:Landroid/telephony/TelephonyManager;

    if-nez v5, :cond_1

    const-string v5, "phone"

    invoke-virtual {p0, v5}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Landroid/telephony/TelephonyManager;

    invoke-virtual {v5, v2}, Landroid/telephony/TelephonyManager;->createForSubscriptionId(I)Landroid/telephony/TelephonyManager;

    move-result-object v2

    sput-object v2, Lf0/f;->h:Landroid/telephony/TelephonyManager;

    sget v2, Landroid/os/Build$VERSION;->SDK_INT:I

    if-lt v2, v4, :cond_0

    sget-object v2, Lf0/f;->l:Ld0/f;

    if-nez v2, :cond_1

    const-string v2, "DeviceInfoReaderBase"

    const-string v5, "registerTelephonyCallback--mDMTelephonyCallback1"

    invoke-static {v2, v5}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v2, Ld0/f;

    invoke-direct {v2, p0, v1}, Ld0/f;-><init>(Landroid/content/Context;I)V

    sput-object v2, Lf0/f;->l:Ld0/f;

    sget-object v1, Lf0/f;->h:Landroid/telephony/TelephonyManager;

    invoke-static {p0}, Le0/j;->a(Landroid/content/Context;)Ljava/util/concurrent/Executor;

    move-result-object v2

    sget-object v5, Lf0/f;->l:Ld0/f;

    invoke-static {v1, v2, v5}, Le0/k;->a(Landroid/telephony/TelephonyManager;Ljava/util/concurrent/Executor;Landroid/telephony/TelephonyCallback;)V

    goto :goto_0

    :cond_0
    sget-object v2, Lf0/f;->j:Ld0/e;

    if-nez v2, :cond_1

    new-instance v2, Ld0/e;

    invoke-direct {v2, p0, v1}, Ld0/e;-><init>(Landroid/content/Context;I)V

    sput-object v2, Lf0/f;->j:Ld0/e;

    sget-object v1, Lf0/f;->h:Landroid/telephony/TelephonyManager;

    sget-object v2, Lf0/f;->j:Ld0/e;

    invoke-virtual {v1, v2, v3}, Landroid/telephony/TelephonyManager;->listen(Landroid/telephony/PhoneStateListener;I)V

    :cond_1
    :goto_0
    const/4 v1, 0x1

    invoke-static {v1}, Le0/r;->h(I)I

    move-result v2

    if-lez v2, :cond_3

    sget-object v5, Lf0/f;->i:Landroid/telephony/TelephonyManager;

    if-nez v5, :cond_3

    const-string v5, "phone"

    invoke-virtual {p0, v5}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Landroid/telephony/TelephonyManager;

    invoke-virtual {v5, v2}, Landroid/telephony/TelephonyManager;->createForSubscriptionId(I)Landroid/telephony/TelephonyManager;

    move-result-object v2

    sput-object v2, Lf0/f;->i:Landroid/telephony/TelephonyManager;

    sget v2, Landroid/os/Build$VERSION;->SDK_INT:I

    if-lt v2, v4, :cond_2

    sget-object v2, Lf0/f;->m:Ld0/f;

    if-nez v2, :cond_3

    const-string v2, "DeviceInfoReaderBase"

    const-string v3, "registerTelephonyCallback--mDMTelephonyCallback2"

    invoke-static {v2, v3}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v2, Ld0/f;

    invoke-direct {v2, p0, v1}, Ld0/f;-><init>(Landroid/content/Context;I)V

    sput-object v2, Lf0/f;->m:Ld0/f;

    sget-object v1, Lf0/f;->i:Landroid/telephony/TelephonyManager;

    invoke-static {p0}, Le0/j;->a(Landroid/content/Context;)Ljava/util/concurrent/Executor;

    move-result-object p0

    sget-object v2, Lf0/f;->m:Ld0/f;

    invoke-static {v1, p0, v2}, Le0/k;->a(Landroid/telephony/TelephonyManager;Ljava/util/concurrent/Executor;Landroid/telephony/TelephonyCallback;)V

    goto :goto_1

    :cond_2
    sget-object v2, Lf0/f;->k:Ld0/e;

    if-nez v2, :cond_3

    new-instance v2, Ld0/e;

    invoke-direct {v2, p0, v1}, Ld0/e;-><init>(Landroid/content/Context;I)V

    sput-object v2, Lf0/f;->k:Ld0/e;

    sget-object p0, Lf0/f;->i:Landroid/telephony/TelephonyManager;

    sget-object v1, Lf0/f;->k:Ld0/e;

    invoke-virtual {p0, v1, v3}, Landroid/telephony/TelephonyManager;->listen(Landroid/telephony/PhoneStateListener;I)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    :cond_3
    :goto_1
    monitor-exit v0

    return-void

    :catchall_0
    move-exception p0

    monitor-exit v0

    throw p0
.end method

.method public static declared-synchronized s(Landroid/content/Context;)V
    .locals 5

    const-class p0, Le0/r;

    monitor-enter p0

    :try_start_0
    const-string v0, "DeviceInfoReaderBase"

    const-string v1, "unlistenNetworkType-->"

    invoke-static {v0, v1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/4 v1, 0x0

    const/16 v2, 0x1f

    if-lt v0, v2, :cond_0

    sget-object v3, Lf0/f;->l:Ld0/f;

    if-eqz v3, :cond_0

    sget-object v3, Lf0/f;->h:Landroid/telephony/TelephonyManager;

    sget-object v4, Lf0/f;->l:Ld0/f;

    invoke-static {v3, v4}, Le0/d;->a(Landroid/telephony/TelephonyManager;Landroid/telephony/TelephonyCallback;)V

    goto :goto_0

    :cond_0
    sget-object v3, Lf0/f;->j:Ld0/e;

    if-eqz v3, :cond_1

    sget-object v3, Lf0/f;->h:Landroid/telephony/TelephonyManager;

    sget-object v4, Lf0/f;->j:Ld0/e;

    invoke-virtual {v3, v4, v1}, Landroid/telephony/TelephonyManager;->listen(Landroid/telephony/PhoneStateListener;I)V

    :cond_1
    :goto_0
    const/4 v3, 0x0

    sput-object v3, Lf0/f;->h:Landroid/telephony/TelephonyManager;

    sput-object v3, Lf0/f;->l:Ld0/f;

    sput-object v3, Lf0/f;->j:Ld0/e;

    if-lt v0, v2, :cond_2

    sget-object v0, Lf0/f;->m:Ld0/f;

    if-eqz v0, :cond_2

    sget-object v0, Lf0/f;->i:Landroid/telephony/TelephonyManager;

    sget-object v1, Lf0/f;->m:Ld0/f;

    invoke-static {v0, v1}, Le0/d;->a(Landroid/telephony/TelephonyManager;Landroid/telephony/TelephonyCallback;)V

    goto :goto_1

    :cond_2
    sget-object v0, Lf0/f;->k:Ld0/e;

    if-eqz v0, :cond_3

    sget-object v0, Lf0/f;->i:Landroid/telephony/TelephonyManager;

    sget-object v2, Lf0/f;->k:Ld0/e;

    invoke-virtual {v0, v2, v1}, Landroid/telephony/TelephonyManager;->listen(Landroid/telephony/PhoneStateListener;I)V

    :cond_3
    :goto_1
    sput-object v3, Lf0/f;->i:Landroid/telephony/TelephonyManager;

    sput-object v3, Lf0/f;->m:Ld0/f;

    sput-object v3, Lf0/f;->k:Ld0/e;
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    monitor-exit p0

    return-void

    :catchall_0
    move-exception v0

    monitor-exit p0

    throw v0
.end method


# virtual methods
.method public a(Landroid/content/Context;)Z
    .locals 0

    new-instance p1, Landroid/telephony/ServiceState;

    invoke-direct {p1}, Landroid/telephony/ServiceState;-><init>()V

    invoke-virtual {p1}, Landroid/telephony/ServiceState;->getRoaming()Z

    move-result p1

    return p1
.end method

.method public b(Landroid/content/Context;Ld0/g;)V
    .locals 2

    sget-object v0, Landroid/os/Build;->MODEL:Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    sget-object v0, Landroid/os/Build;->MANUFACTURER:Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->a:Ljava/lang/String;

    const-string v0, "Android"

    iput-object v0, p2, Ld0/g;->e:Ljava/lang/String;

    sget-object v0, Landroid/os/Build$VERSION;->RELEASE:Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->f:Ljava/lang/String;

    sget-object v0, Landroid/os/Build;->DISPLAY:Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->d:Ljava/lang/String;

    const-string v0, "ro.hardware.version"

    const/4 v1, 0x0

    invoke-static {v0, v1}, Lf0/l;->j(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->c:Ljava/lang/String;

    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v1, 0x1a

    if-ge v0, v1, :cond_0

    invoke-virtual {p0, p1, p2}, Le0/r;->m(Landroid/content/Context;Ld0/g;)V

    goto :goto_0

    :cond_0
    if-ge v0, v1, :cond_1

    invoke-virtual {p0, p1, p2}, Le0/r;->n(Landroid/content/Context;Ld0/g;)V

    goto :goto_0

    :cond_1
    if-lt v0, v1, :cond_2

    invoke-virtual {p0, p1, p2}, Le0/r;->o(Landroid/content/Context;Ld0/g;)V

    goto :goto_0

    :cond_2
    const/16 v1, 0x1e

    if-lt v0, v1, :cond_3

    invoke-virtual {p0, p1, p2}, Le0/r;->l(Landroid/content/Context;Ld0/g;)V

    :cond_3
    :goto_0
    return-void
.end method

.method protected declared-synchronized c(Landroid/content/Context;I)Ljava/lang/String;
    .locals 6

    monitor-enter p0

    :try_start_0
    const-string v0, "-1"

    const-string v1, "telephony_ims"

    invoke-virtual {p1, v1}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Landroid/telephony/ims/ImsManager;

    invoke-static {p2}, Le0/r;->h(I)I

    move-result v1

    const/4 v2, 0x0

    if-ltz v1, :cond_2

    invoke-static {p1, v1}, Le0/g;->a(Landroid/telephony/ims/ImsManager;I)Landroid/telephony/ims/ImsMmTelManager;

    move-result-object p1

    invoke-static {p1}, Le0/h;->a(Landroid/telephony/ims/ImsMmTelManager;)Z

    move-result v0

    if-nez v0, :cond_0

    const-string v0, "DeviceInfoReaderBase"

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "before open:"

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v0, v1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const-class v0, Landroid/telephony/ims/ImsMmTelManager;

    const-string v1, "setAdvancedCallingSettingEnabled"

    const/4 v3, 0x1

    new-array v4, v3, [Ljava/lang/Class;

    sget-object v5, Ljava/lang/Boolean;->TYPE:Ljava/lang/Class;

    aput-object v5, v4, v2

    invoke-static {v0, v1, v4}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v0

    new-array v1, v3, [Ljava/lang/Object;

    sget-object v3, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    aput-object v3, v1, v2

    invoke-static {v0, p1, v1}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    :cond_0
    invoke-static {p1}, Le0/h;->a(Landroid/telephony/ims/ImsMmTelManager;)Z

    move-result v2

    if-eqz v2, :cond_1

    const-string v0, "1"

    goto :goto_0

    :cond_1
    const-string v0, "0"

    :cond_2
    :goto_0
    const-string p1, "DeviceInfoReaderBase"

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "VoLTE state slotId:"

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string p2, "\uff0cenable:"

    invoke-virtual {v1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-static {p1, p2}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    monitor-exit p0

    return-object v0

    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method

.method protected declared-synchronized d(Landroid/content/Context;I)Ljava/lang/String;
    .locals 2

    monitor-enter p0

    :try_start_0
    const-string v0, "-1"

    const-string v1, "telephony_ims"

    invoke-virtual {p1, v1}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Landroid/telephony/ims/ImsManager;

    invoke-static {p2}, Le0/r;->h(I)I

    move-result p2

    if-ltz p2, :cond_1

    invoke-static {p1, p2}, Le0/g;->a(Landroid/telephony/ims/ImsManager;I)Landroid/telephony/ims/ImsMmTelManager;

    move-result-object p1

    invoke-static {p1}, Le0/h;->a(Landroid/telephony/ims/ImsMmTelManager;)Z

    move-result p1

    if-eqz p1, :cond_0

    const-string v0, "1"

    goto :goto_0

    :cond_0
    const-string v0, "0"
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    :cond_1
    :goto_0
    monitor-exit p0

    return-object v0

    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method

.method public f(Landroid/telephony/TelephonyManager;I)I
    .locals 2

    :try_start_0
    const-string v0, "getNetworkType"

    invoke-static {p1, v0, p2}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/Integer;

    invoke-virtual {v0}, Ljava/lang/Integer;->intValue()I

    move-result v0

    const-string v1, "getVoiceNetworkType"

    invoke-static {p1, v1, p2}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Ljava/lang/Integer;

    invoke-virtual {p1}, Ljava/lang/Integer;->intValue()I

    move-result p1
    :try_end_0
    .catch Ljava/lang/ClassNotFoundException; {:try_start_0 .. :try_end_0} :catch_3
    .catch Ljava/lang/IllegalArgumentException; {:try_start_0 .. :try_end_0} :catch_2
    .catch Ljava/lang/IllegalAccessException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/reflect/InvocationTargetException; {:try_start_0 .. :try_end_0} :catch_0

    if-eqz v0, :cond_0

    return v0

    :cond_0
    if-eqz p1, :cond_1

    return p1

    :catch_0
    move-exception p1

    goto :goto_0

    :catch_1
    move-exception p1

    goto :goto_0

    :catch_2
    move-exception p1

    goto :goto_0

    :catch_3
    move-exception p1

    :goto_0
    invoke-virtual {p1}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_1
    const/4 p1, 0x0

    return p1
.end method

.method declared-synchronized g(ILjava/lang/String;)Ljava/lang/String;
    .locals 2

    monitor-enter p0

    packed-switch p1, :pswitch_data_0

    :pswitch_0
    :try_start_0
    const-string p2, "DeviceInfoReaderBase"

    goto :goto_1

    :pswitch_1
    const-string p1, "NR"
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    monitor-exit p0

    return-object p1

    :pswitch_2
    :try_start_1
    const-string p1, "TD-SCDMA"
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit p0

    return-object p1

    :pswitch_3
    :try_start_2
    const-string p1, "LTE"
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    monitor-exit p0

    return-object p1

    :pswitch_4
    :try_start_3
    const-string p1, "CDMA"
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    monitor-exit p0

    return-object p1

    :pswitch_5
    :try_start_4
    const-string p1, "46000"

    invoke-virtual {p1, p2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-nez p1, :cond_1

    const-string p1, "46002"

    invoke-virtual {p1, p2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-nez p1, :cond_1

    const-string p1, "46007"

    invoke-virtual {p1, p2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_0

    goto :goto_0

    :cond_0
    const-string p1, "WCDMA"
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_0

    monitor-exit p0

    return-object p1

    :cond_1
    :goto_0
    :try_start_5
    const-string p1, "TD-SCDMA"
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_0

    monitor-exit p0

    return-object p1

    :pswitch_6
    :try_start_6
    const-string p1, "GSM"
    :try_end_6
    .catchall {:try_start_6 .. :try_end_6} :catchall_0

    monitor-exit p0

    return-object p1

    :catchall_0
    move-exception p1

    goto :goto_2

    :goto_1
    :try_start_7
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "unknown networktype "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {p2, p1}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_7
    .catchall {:try_start_7 .. :try_end_7} :catchall_0

    monitor-exit p0

    const/4 p1, 0x0

    return-object p1

    :goto_2
    monitor-exit p0

    throw p1

    nop

    :pswitch_data_0
    .packed-switch 0x1
        :pswitch_6
        :pswitch_6
        :pswitch_5
        :pswitch_4
        :pswitch_4
        :pswitch_4
        :pswitch_4
        :pswitch_5
        :pswitch_5
        :pswitch_5
        :pswitch_0
        :pswitch_4
        :pswitch_3
        :pswitch_4
        :pswitch_5
        :pswitch_6
        :pswitch_2
        :pswitch_0
        :pswitch_3
        :pswitch_1
    .end packed-switch
.end method

.method protected i(I)[I
    .locals 9

    const/4 v0, 0x1

    const/4 v1, 0x0

    const/4 v2, 0x0

    :try_start_0
    const-string v3, "android.telephony.SubscriptionManager"

    invoke-static {v3}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v3

    const-string v4, "getSubId"

    new-array v5, v0, [Ljava/lang/Class;

    sget-object v6, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    aput-object v6, v5, v1

    invoke-virtual {v3, v4, v5}, Ljava/lang/Class;->getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v3
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception v3

    invoke-virtual {v3}, Ljava/lang/Throwable;->printStackTrace()V

    move-object v3, v2

    :goto_0
    if-eqz v3, :cond_3

    :try_start_1
    new-array v4, v0, [Ljava/lang/Object;

    invoke-static {p1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v5

    aput-object v5, v4, v1

    invoke-virtual {v3, v2, v4}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, [J

    if-eqz v4, :cond_1

    array-length v5, v4

    if-lez v5, :cond_1

    array-length v5, v4

    new-array v5, v5, [I
    :try_end_1
    .catch Ljava/lang/ClassCastException; {:try_start_1 .. :try_end_1} :catch_1
    .catch Ljava/lang/IllegalAccessException; {:try_start_1 .. :try_end_1} :catch_1
    .catch Ljava/lang/IllegalArgumentException; {:try_start_1 .. :try_end_1} :catch_1
    .catch Ljava/lang/reflect/InvocationTargetException; {:try_start_1 .. :try_end_1} :catch_1

    move v6, v1

    :goto_1
    :try_start_2
    array-length v7, v4

    if-ge v6, v7, :cond_0

    aget-wide v7, v4, v6

    long-to-int v7, v7

    aput v7, v5, v6
    :try_end_2
    .catch Ljava/lang/ClassCastException; {:try_start_2 .. :try_end_2} :catch_2
    .catch Ljava/lang/IllegalAccessException; {:try_start_2 .. :try_end_2} :catch_2
    .catch Ljava/lang/IllegalArgumentException; {:try_start_2 .. :try_end_2} :catch_2
    .catch Ljava/lang/reflect/InvocationTargetException; {:try_start_2 .. :try_end_2} :catch_2

    add-int/lit8 v6, v6, 0x1

    goto :goto_1

    :catch_1
    move-object v5, v2

    :catch_2
    :try_start_3
    new-array v0, v0, [Ljava/lang/Object;

    invoke-static {p1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object p1

    aput-object p1, v0, v1

    invoke-virtual {v3, v2, v0}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, [I
    :try_end_3
    .catch Ljava/lang/IllegalAccessException; {:try_start_3 .. :try_end_3} :catch_5
    .catch Ljava/lang/IllegalArgumentException; {:try_start_3 .. :try_end_3} :catch_4
    .catch Ljava/lang/reflect/InvocationTargetException; {:try_start_3 .. :try_end_3} :catch_3

    move-object v2, p1

    goto :goto_3

    :catch_3
    move-exception p1

    goto :goto_2

    :catch_4
    move-exception p1

    goto :goto_2

    :catch_5
    move-exception p1

    :goto_2
    invoke-virtual {p1}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_0
    move-object v2, v5

    :cond_1
    :goto_3
    const-string p1, "DeviceInfoReaderBase"

    new-instance v0, Ljava/lang/StringBuilder;

    if-eqz v2, :cond_2

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "[getSubId] sub[0] is "

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    aget v1, v2, v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    goto :goto_4

    :cond_2
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "[getSubId] result is "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    :goto_4
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {p1, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    :cond_3
    return-object v2
.end method

.method protected declared-synchronized j(Landroid/content/Context;I)Z
    .locals 3

    monitor-enter p0

    const/4 v0, 0x0

    :try_start_0
    invoke-virtual {p1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object p1

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "preferred_network_mode"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    const/4 v1, -0x1

    invoke-static {p1, p2, v1}, Landroid/provider/Settings$Global;->getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I

    move-result p1
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    const/16 p2, 0x17

    if-lt p1, p2, :cond_0

    const/16 p2, 0x21

    if-gt p1, p2, :cond_0

    const/4 p1, 0x1

    move v0, p1

    goto :goto_0

    :catchall_0
    move-exception p1

    goto :goto_1

    :catch_0
    move-exception p1

    :try_start_1
    invoke-virtual {p1}, Ljava/lang/Throwable;->printStackTrace()V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    :cond_0
    :goto_0
    monitor-exit p0

    return v0

    :goto_1
    monitor-exit p0

    throw p1
.end method

.method l(Landroid/content/Context;Ld0/g;)V
    .locals 11

    const-string v0, "android.permission.READ_PHONE_NUMBERS"

    invoke-static {p1, v0}, Lj/a;->a(Landroid/content/Context;Ljava/lang/String;)I

    move-result v0

    if-eqz v0, :cond_0

    const-string v0, "android.permission.READ_PHONE_STATE"

    invoke-static {p1, v0}, Lj/a;->a(Landroid/content/Context;Ljava/lang/String;)I

    move-result v0

    if-eqz v0, :cond_0

    const-string p1, "DeviceInfoReaderBase"

    const-string p2, "readAndroidR no READ_PHONE_STATE PERMISSION_GRANTED"

    invoke-static {p1, p2}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V

    return-void

    :cond_0
    const-string v0, "phone"

    invoke-virtual {p1, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/telephony/TelephonyManager;

    const/4 v2, 0x0

    invoke-static {v1, v2}, Le0/i;->a(Landroid/telephony/TelephonyManager;I)Ljava/lang/String;

    move-result-object v3

    iput-object v3, p2, Ld0/g;->g:Ljava/lang/String;

    const/4 v3, 0x1

    invoke-static {v1, v3}, Le0/i;->a(Landroid/telephony/TelephonyManager;I)Ljava/lang/String;

    move-result-object v1

    iput-object v1, p2, Ld0/g;->r:Ljava/lang/String;

    invoke-static {v2}, Le0/r;->h(I)I

    move-result v1

    const-string v4, "46009"

    const-string v5, "46006"

    const-string v6, "46001"

    const-string v7, "1"

    const-string v8, "0"

    if-ltz v1, :cond_4

    invoke-virtual {p1, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v9

    check-cast v9, Landroid/telephony/TelephonyManager;

    invoke-virtual {v9, v1}, Landroid/telephony/TelephonyManager;->createForSubscriptionId(I)Landroid/telephony/TelephonyManager;

    move-result-object v9

    invoke-virtual {v9}, Landroid/telephony/TelephonyManager;->getSubscriberId()Ljava/lang/String;

    move-result-object v10

    iput-object v10, p2, Ld0/g;->i:Ljava/lang/String;

    if-eqz v10, :cond_4

    invoke-virtual {v10}, Ljava/lang/String;->length()I

    move-result v10

    if-lez v10, :cond_4

    invoke-virtual {v9}, Landroid/telephony/TelephonyManager;->getSimSerialNumber()Ljava/lang/String;

    move-result-object v10

    iput-object v10, p2, Ld0/g;->h:Ljava/lang/String;

    invoke-virtual {v9}, Landroid/telephony/TelephonyManager;->getLine1Number()Ljava/lang/String;

    move-result-object v10

    iput-object v10, p2, Ld0/g;->j:Ljava/lang/String;

    invoke-virtual {v9}, Landroid/telephony/TelephonyManager;->getNetworkOperator()Ljava/lang/String;

    move-result-object v9

    iput-object v9, p2, Ld0/g;->k:Ljava/lang/String;

    invoke-virtual {p0, p1, v1}, Le0/r;->j(Landroid/content/Context;I)Z

    move-result v1

    if-eqz v1, :cond_1

    iput-object v7, p2, Ld0/g;->p:Ljava/lang/String;

    goto :goto_0

    :cond_1
    iput-object v8, p2, Ld0/g;->p:Ljava/lang/String;

    :goto_0
    sget-object v1, Lf0/f;->n:Ljava/lang/String;

    iput-object v1, p2, Ld0/g;->l:Ljava/lang/String;

    invoke-virtual {p0, p1, v2, p2}, Le0/r;->r(Landroid/content/Context;ILd0/g;)V

    sget-boolean v1, Lf0/f;->c:Z

    if-eqz v1, :cond_3

    iget-object v1, p2, Ld0/g;->k:Ljava/lang/String;

    invoke-virtual {v1}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_2

    iget-object v1, p2, Ld0/g;->k:Ljava/lang/String;

    invoke-virtual {v1}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_2

    iget-object v1, p2, Ld0/g;->k:Ljava/lang/String;

    invoke-virtual {v1}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_4

    :cond_2
    invoke-virtual {p0, p1, v2}, Le0/r;->c(Landroid/content/Context;I)Ljava/lang/String;

    move-result-object v1

    goto :goto_1

    :cond_3
    invoke-virtual {p0, p1, v2}, Le0/r;->d(Landroid/content/Context;I)Ljava/lang/String;

    move-result-object v1

    :goto_1
    iput-object v1, p2, Ld0/g;->o:Ljava/lang/String;

    :cond_4
    invoke-static {v3}, Le0/r;->h(I)I

    move-result v1

    if-ltz v1, :cond_8

    invoke-virtual {p1, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/telephony/TelephonyManager;

    invoke-virtual {v0, v1}, Landroid/telephony/TelephonyManager;->createForSubscriptionId(I)Landroid/telephony/TelephonyManager;

    move-result-object v0

    invoke-virtual {v0}, Landroid/telephony/TelephonyManager;->getSubscriberId()Ljava/lang/String;

    move-result-object v2

    iput-object v2, p2, Ld0/g;->t:Ljava/lang/String;

    if-eqz v2, :cond_8

    invoke-virtual {v2}, Ljava/lang/String;->length()I

    move-result v2

    if-lez v2, :cond_8

    invoke-virtual {v0}, Landroid/telephony/TelephonyManager;->getSimSerialNumber()Ljava/lang/String;

    move-result-object v2

    iput-object v2, p2, Ld0/g;->s:Ljava/lang/String;

    invoke-virtual {v0}, Landroid/telephony/TelephonyManager;->getLine1Number()Ljava/lang/String;

    move-result-object v2

    iput-object v2, p2, Ld0/g;->u:Ljava/lang/String;

    invoke-virtual {v0}, Landroid/telephony/TelephonyManager;->getNetworkOperator()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->v:Ljava/lang/String;

    invoke-virtual {p0, p1, v1}, Le0/r;->j(Landroid/content/Context;I)Z

    move-result v0

    if-eqz v0, :cond_5

    iput-object v7, p2, Ld0/g;->A:Ljava/lang/String;

    goto :goto_2

    :cond_5
    iput-object v8, p2, Ld0/g;->A:Ljava/lang/String;

    :goto_2
    sget-object v0, Lf0/f;->o:Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->w:Ljava/lang/String;

    invoke-virtual {p0, p1, v3, p2}, Le0/r;->r(Landroid/content/Context;ILd0/g;)V

    sget-boolean v0, Lf0/f;->c:Z

    if-eqz v0, :cond_7

    iget-object v0, p2, Ld0/g;->v:Ljava/lang/String;

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_6

    iget-object v0, p2, Ld0/g;->v:Ljava/lang/String;

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_6

    iget-object v0, p2, Ld0/g;->v:Ljava/lang/String;

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_8

    :cond_6
    invoke-virtual {p0, p1, v3}, Le0/r;->c(Landroid/content/Context;I)Ljava/lang/String;

    move-result-object p1

    goto :goto_3

    :cond_7
    invoke-virtual {p0, p1, v3}, Le0/r;->d(Landroid/content/Context;I)Ljava/lang/String;

    move-result-object p1

    :goto_3
    iput-object p1, p2, Ld0/g;->z:Ljava/lang/String;

    :cond_8
    return-void
.end method

.method m(Landroid/content/Context;Ld0/g;)V
    .locals 2

    const-string v0, "phone"

    invoke-virtual {p1, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Landroid/telephony/TelephonyManager;

    invoke-virtual {p1}, Landroid/telephony/TelephonyManager;->getDeviceId()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->g:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/telephony/TelephonyManager;->getSubscriberId()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->i:Ljava/lang/String;

    if-eqz v0, :cond_0

    invoke-virtual {v0}, Ljava/lang/String;->length()I

    move-result v0

    if-lez v0, :cond_0

    invoke-virtual {p1}, Landroid/telephony/TelephonyManager;->getSimSerialNumber()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->h:Ljava/lang/String;

    const-string v0, ""

    iput-object v0, p2, Ld0/g;->j:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/telephony/TelephonyManager;->getSimOperator()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->k:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/telephony/TelephonyManager;->getNetworkType()I

    move-result v0

    iget-object v1, p2, Ld0/g;->k:Ljava/lang/String;

    invoke-virtual {p0, v0, v1}, Le0/r;->g(ILjava/lang/String;)Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->l:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/telephony/TelephonyManager;->getCellLocation()Landroid/telephony/CellLocation;

    move-result-object p1

    invoke-virtual {p2, p1}, Ld0/g;->f(Landroid/telephony/CellLocation;)V

    :cond_0
    return-void
.end method

.method n(Landroid/content/Context;Ld0/g;)V
    .locals 4

    const-string v0, "phone"

    invoke-virtual {p1, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/telephony/TelephonyManager;

    invoke-virtual {v0}, Landroid/telephony/TelephonyManager;->getDeviceId()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->g:Ljava/lang/String;

    const-string v0, "telephony_subscription_service"

    invoke-virtual {p1, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Landroid/telephony/SubscriptionManager;

    const/4 v0, 0x0

    invoke-virtual {p1, v0}, Landroid/telephony/SubscriptionManager;->getActiveSubscriptionInfoForSimSlotIndex(I)Landroid/telephony/SubscriptionInfo;

    move-result-object v0

    const/4 v1, 0x1

    invoke-virtual {p1, v1}, Landroid/telephony/SubscriptionManager;->getActiveSubscriptionInfoForSimSlotIndex(I)Landroid/telephony/SubscriptionInfo;

    move-result-object p1

    if-eqz v0, :cond_0

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v0}, Landroid/telephony/SubscriptionInfo;->getCarrierName()Ljava/lang/CharSequence;

    move-result-object v3

    check-cast v3, Ljava/lang/String;

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Landroid/telephony/SubscriptionInfo;->getNumber()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    iput-object v2, p2, Ld0/g;->j:Ljava/lang/String;

    invoke-virtual {v0}, Landroid/telephony/SubscriptionInfo;->getIccId()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->h:Ljava/lang/String;

    :cond_0
    if-eqz p1, :cond_1

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p1}, Landroid/telephony/SubscriptionInfo;->getCarrierName()Ljava/lang/CharSequence;

    move-result-object v2

    check-cast v2, Ljava/lang/String;

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Landroid/telephony/SubscriptionInfo;->getNumber()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->u:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/telephony/SubscriptionInfo;->getIccId()Ljava/lang/String;

    move-result-object p1

    iput-object p1, p2, Ld0/g;->s:Ljava/lang/String;

    :cond_1
    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v0, ""

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-static {}, Landroid/telephony/SubscriptionManager;->getDefaultDataSubscriptionId()I

    move-result v0

    add-int/2addr v0, v1

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    iput-object p1, p2, Ld0/g;->B:Ljava/lang/String;

    return-void
.end method

.method o(Landroid/content/Context;Ld0/g;)V
    .locals 4

    const-string v0, "DeviceInfoReaderBase"

    const-string v1, "read26"

    invoke-static {v0, v1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const-string v0, "phone"

    invoke-virtual {p1, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/telephony/TelephonyManager;

    const/4 v1, 0x0

    invoke-static {v0, v1}, Le0/i;->a(Landroid/telephony/TelephonyManager;I)Ljava/lang/String;

    move-result-object v2

    const/4 v3, 0x1

    invoke-static {v0, v3}, Le0/i;->a(Landroid/telephony/TelephonyManager;I)Ljava/lang/String;

    move-result-object v0

    if-eqz v2, :cond_0

    iput-object v2, p2, Ld0/g;->g:Ljava/lang/String;

    :cond_0
    if-eqz v0, :cond_1

    iput-object v0, p2, Ld0/g;->r:Ljava/lang/String;

    :cond_1
    const-string v0, "telephony_subscription_service"

    invoke-virtual {p1, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Landroid/telephony/SubscriptionManager;

    invoke-virtual {p1, v1}, Landroid/telephony/SubscriptionManager;->getActiveSubscriptionInfoForSimSlotIndex(I)Landroid/telephony/SubscriptionInfo;

    move-result-object v0

    invoke-virtual {p1, v3}, Landroid/telephony/SubscriptionManager;->getActiveSubscriptionInfoForSimSlotIndex(I)Landroid/telephony/SubscriptionInfo;

    move-result-object p1

    if-eqz v0, :cond_2

    invoke-virtual {v0}, Landroid/telephony/SubscriptionInfo;->getNumber()Ljava/lang/String;

    move-result-object v1

    iput-object v1, p2, Ld0/g;->j:Ljava/lang/String;

    invoke-virtual {v0}, Landroid/telephony/SubscriptionInfo;->getIccId()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->h:Ljava/lang/String;

    :cond_2
    if-eqz p1, :cond_3

    invoke-virtual {p1}, Landroid/telephony/SubscriptionInfo;->getNumber()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->u:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/telephony/SubscriptionInfo;->getIccId()Ljava/lang/String;

    move-result-object p1

    iput-object p1, p2, Ld0/g;->s:Ljava/lang/String;

    :cond_3
    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v0, ""

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-static {}, Landroid/telephony/SubscriptionManager;->getDefaultDataSubscriptionId()I

    move-result v0

    add-int/2addr v0, v3

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    iput-object p1, p2, Ld0/g;->B:Ljava/lang/String;

    return-void
.end method

.method public p(Landroid/content/Context;Ld0/g;)V
    .locals 12

    const-string v0, "getDeviceId"

    const-string v1, "phone"

    invoke-virtual {p1, v1}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/telephony/TelephonyManager;

    const/4 v2, 0x0

    :try_start_0
    invoke-static {v1, v0, v2}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/String;

    iput-object v3, p2, Ld0/g;->g:Ljava/lang/String;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception v3

    invoke-virtual {v3}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_0
    invoke-static {v2}, Le0/d2;->a(I)[I

    move-result-object v3

    const-string v4, "getNetworkType"

    const-string v5, "getNetworkOperator"

    const-string v6, "getNetworkOperatorForSubscription"

    const-string v7, "getLine1NumberForSubscriber"

    const-string v8, "getSimSerialNumber"

    const-string v9, "getSubscriberId"

    const/4 v10, 0x0

    if-eqz v3, :cond_0

    aget v11, v3, v2

    if-ltz v11, :cond_0

    :try_start_1
    invoke-static {v1, v9, v11}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v11

    check-cast v11, Ljava/lang/String;

    iput-object v11, p2, Ld0/g;->i:Ljava/lang/String;
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1

    goto :goto_1

    :catch_1
    move-exception v11

    invoke-virtual {v11}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_1
    iget-object v11, p2, Ld0/g;->i:Ljava/lang/String;

    if-eqz v11, :cond_0

    invoke-virtual {v11}, Ljava/lang/String;->length()I

    move-result v11

    if-lez v11, :cond_0

    :try_start_2
    aget v11, v3, v2

    invoke-static {v1, v8, v11}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v11

    check-cast v11, Ljava/lang/String;

    iput-object v11, p2, Ld0/g;->h:Ljava/lang/String;
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_2

    goto :goto_2

    :catch_2
    move-exception v11

    invoke-virtual {v11}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_2
    :try_start_3
    aget v11, v3, v2

    invoke-static {v1, v7, v11}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v11

    check-cast v11, Ljava/lang/String;

    iput-object v11, p2, Ld0/g;->j:Ljava/lang/String;
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_3

    goto :goto_3

    :catch_3
    move-exception v11

    invoke-virtual {v11}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_3
    :try_start_4
    aget v11, v3, v2

    invoke-static {v1, v6, v5, v11}, Le0/d2;->c(Landroid/telephony/TelephonyManager;Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v11

    check-cast v11, Ljava/lang/String;

    iput-object v11, p2, Ld0/g;->k:Ljava/lang/String;
    :try_end_4
    .catch Ljava/lang/Exception; {:try_start_4 .. :try_end_4} :catch_4

    goto :goto_4

    :catch_4
    move-exception v11

    invoke-virtual {v11}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_4
    :try_start_5
    aget v3, v3, v2

    invoke-static {v1, v4, v3}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/Integer;

    invoke-virtual {v3}, Ljava/lang/Integer;->intValue()I

    move-result v3

    iget-object v11, p2, Ld0/g;->k:Ljava/lang/String;

    invoke-virtual {p0, v3, v11}, Le0/r;->g(ILjava/lang/String;)Ljava/lang/String;

    move-result-object v3

    iput-object v3, p2, Ld0/g;->l:Ljava/lang/String;
    :try_end_5
    .catch Ljava/lang/Exception; {:try_start_5 .. :try_end_5} :catch_5

    goto :goto_5

    :catch_5
    move-exception v3

    invoke-virtual {v3}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_5
    :try_start_6
    const-string v3, "CellID0"

    invoke-static {p1, v3, v10}, Lf0/l;->h(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    iput-object v3, p2, Ld0/g;->m:Ljava/lang/String;

    const-string v3, "LAC0"

    invoke-static {p1, v3, v10}, Lf0/l;->h(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    iput-object v3, p2, Ld0/g;->n:Ljava/lang/String;
    :try_end_6
    .catch Ljava/lang/Exception; {:try_start_6 .. :try_end_6} :catch_6

    goto :goto_6

    :catch_6
    move-exception v3

    invoke-virtual {v3}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_0
    :goto_6
    const/4 v3, 0x1

    invoke-static {v3}, Le0/d2;->a(I)[I

    move-result-object v11

    :try_start_7
    invoke-static {v1, v0, v3}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->r:Ljava/lang/String;
    :try_end_7
    .catch Ljava/lang/Exception; {:try_start_7 .. :try_end_7} :catch_7

    goto :goto_7

    :catch_7
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_7
    if-eqz v11, :cond_1

    aget v0, v11, v2

    if-ltz v0, :cond_1

    :try_start_8
    invoke-static {v1, v9, v0}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->t:Ljava/lang/String;
    :try_end_8
    .catch Ljava/lang/Exception; {:try_start_8 .. :try_end_8} :catch_8

    goto :goto_8

    :catch_8
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_8
    iget-object v0, p2, Ld0/g;->t:Ljava/lang/String;

    if-eqz v0, :cond_1

    invoke-virtual {v0}, Ljava/lang/String;->length()I

    move-result v0

    if-lez v0, :cond_1

    :try_start_9
    aget v0, v11, v2

    invoke-static {v1, v8, v0}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->s:Ljava/lang/String;
    :try_end_9
    .catch Ljava/lang/Exception; {:try_start_9 .. :try_end_9} :catch_9

    goto :goto_9

    :catch_9
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_9
    :try_start_a
    aget v0, v11, v2

    invoke-static {v1, v7, v0}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->u:Ljava/lang/String;
    :try_end_a
    .catch Ljava/lang/Exception; {:try_start_a .. :try_end_a} :catch_a

    goto :goto_a

    :catch_a
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_a
    :try_start_b
    aget v0, v11, v2

    invoke-static {v1, v6, v5, v0}, Le0/d2;->c(Landroid/telephony/TelephonyManager;Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->v:Ljava/lang/String;
    :try_end_b
    .catch Ljava/lang/Exception; {:try_start_b .. :try_end_b} :catch_b

    goto :goto_b

    :catch_b
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_b
    :try_start_c
    aget v0, v11, v2

    invoke-static {v1, v4, v0}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/Integer;

    invoke-virtual {v0}, Ljava/lang/Integer;->intValue()I

    move-result v0

    iget-object v1, p2, Ld0/g;->v:Ljava/lang/String;

    invoke-virtual {p0, v0, v1}, Le0/r;->g(ILjava/lang/String;)Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->w:Ljava/lang/String;
    :try_end_c
    .catch Ljava/lang/Exception; {:try_start_c .. :try_end_c} :catch_c

    goto :goto_c

    :catch_c
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_c
    :try_start_d
    const-string v0, "CellID1"

    invoke-static {p1, v0, v10}, Lf0/l;->h(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->x:Ljava/lang/String;

    const-string v0, "LAC1"

    invoke-static {p1, v0, v10}, Lf0/l;->h(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, p2, Ld0/g;->y:Ljava/lang/String;
    :try_end_d
    .catch Ljava/lang/Exception; {:try_start_d .. :try_end_d} :catch_d

    goto :goto_d

    :catch_d
    move-exception p1

    invoke-virtual {p1}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_1
    :goto_d
    return-void
.end method

.method public q(Landroid/content/Context;Ld0/g;)V
    .locals 11

    const-string v0, "getDeviceId"

    const-string v1, "phone"

    invoke-virtual {p1, v1}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/telephony/TelephonyManager;

    const/4 v2, 0x0

    :try_start_0
    invoke-static {v1, v0, v2}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/String;

    iput-object v3, p2, Ld0/g;->g:Ljava/lang/String;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception v3

    invoke-virtual {v3}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_0
    invoke-static {v2}, Le0/d2;->a(I)[I

    move-result-object v3

    const-string v4, "getNetworkOperator"

    const-string v5, "getNetworkOperatorForSubscription"

    const-string v6, "getLine1NumberForSubscriber"

    const-string v7, "getSimSerialNumber"

    const-string v8, "getSubscriberId"

    const/4 v9, 0x0

    if-eqz v3, :cond_0

    aget v10, v3, v2

    if-ltz v10, :cond_0

    :try_start_1
    invoke-static {v1, v8, v10}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v10

    check-cast v10, Ljava/lang/String;

    iput-object v10, p2, Ld0/g;->i:Ljava/lang/String;
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1

    goto :goto_1

    :catch_1
    move-exception v10

    invoke-virtual {v10}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_1
    iget-object v10, p2, Ld0/g;->i:Ljava/lang/String;

    if-eqz v10, :cond_0

    invoke-virtual {v10}, Ljava/lang/String;->length()I

    move-result v10

    if-lez v10, :cond_0

    :try_start_2
    aget v10, v3, v2

    invoke-static {v1, v7, v10}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v10

    check-cast v10, Ljava/lang/String;

    iput-object v10, p2, Ld0/g;->h:Ljava/lang/String;
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_2

    goto :goto_2

    :catch_2
    move-exception v10

    invoke-virtual {v10}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_2
    :try_start_3
    aget v10, v3, v2

    invoke-static {v1, v6, v10}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v10

    check-cast v10, Ljava/lang/String;

    iput-object v10, p2, Ld0/g;->j:Ljava/lang/String;
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_3

    goto :goto_3

    :catch_3
    move-exception v10

    invoke-virtual {v10}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_3
    :try_start_4
    aget v10, v3, v2

    invoke-static {v1, v5, v4, v10}, Le0/d2;->c(Landroid/telephony/TelephonyManager;Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v10

    check-cast v10, Ljava/lang/String;

    iput-object v10, p2, Ld0/g;->k:Ljava/lang/String;
    :try_end_4
    .catch Ljava/lang/Exception; {:try_start_4 .. :try_end_4} :catch_4

    goto :goto_4

    :catch_4
    move-exception v10

    invoke-virtual {v10}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_4
    :try_start_5
    aget v3, v3, v2

    invoke-virtual {p0, v1, v3}, Le0/r;->f(Landroid/telephony/TelephonyManager;I)I

    move-result v3

    iget-object v10, p2, Ld0/g;->k:Ljava/lang/String;

    invoke-virtual {p0, v3, v10}, Le0/r;->g(ILjava/lang/String;)Ljava/lang/String;

    move-result-object v3

    iput-object v3, p2, Ld0/g;->l:Ljava/lang/String;
    :try_end_5
    .catch Ljava/lang/Exception; {:try_start_5 .. :try_end_5} :catch_5

    goto :goto_5

    :catch_5
    move-exception v3

    invoke-virtual {v3}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_5
    const-string v3, "CellID0"

    invoke-static {p1, v3, v9}, Lf0/l;->h(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    iput-object v3, p2, Ld0/g;->m:Ljava/lang/String;

    const-string v3, "LAC0"

    invoke-static {p1, v3, v9}, Lf0/l;->h(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    iput-object v3, p2, Ld0/g;->n:Ljava/lang/String;

    :cond_0
    const/4 v3, 0x1

    invoke-static {v3}, Le0/d2;->a(I)[I

    move-result-object v10

    :try_start_6
    invoke-static {v1, v0, v3}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->r:Ljava/lang/String;
    :try_end_6
    .catch Ljava/lang/Exception; {:try_start_6 .. :try_end_6} :catch_6

    goto :goto_6

    :catch_6
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_6
    if-eqz v10, :cond_1

    aget v0, v10, v2

    if-ltz v0, :cond_1

    :try_start_7
    invoke-static {v1, v8, v0}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->t:Ljava/lang/String;
    :try_end_7
    .catch Ljava/lang/Exception; {:try_start_7 .. :try_end_7} :catch_7

    goto :goto_7

    :catch_7
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_7
    iget-object v0, p2, Ld0/g;->t:Ljava/lang/String;

    if-eqz v0, :cond_1

    invoke-virtual {v0}, Ljava/lang/String;->length()I

    move-result v0

    if-lez v0, :cond_1

    :try_start_8
    aget v0, v10, v2

    invoke-static {v1, v7, v0}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->s:Ljava/lang/String;
    :try_end_8
    .catch Ljava/lang/Exception; {:try_start_8 .. :try_end_8} :catch_8

    goto :goto_8

    :catch_8
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_8
    :try_start_9
    aget v0, v10, v2

    invoke-static {v1, v6, v0}, Le0/d2;->b(Landroid/telephony/TelephonyManager;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->u:Ljava/lang/String;
    :try_end_9
    .catch Ljava/lang/Exception; {:try_start_9 .. :try_end_9} :catch_9

    goto :goto_9

    :catch_9
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_9
    :try_start_a
    aget v0, v10, v2

    invoke-static {v1, v5, v4, v0}, Le0/d2;->c(Landroid/telephony/TelephonyManager;Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->v:Ljava/lang/String;
    :try_end_a
    .catch Ljava/lang/Exception; {:try_start_a .. :try_end_a} :catch_a

    goto :goto_a

    :catch_a
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_a
    :try_start_b
    aget v0, v10, v2

    invoke-virtual {p0, v1, v0}, Le0/r;->f(Landroid/telephony/TelephonyManager;I)I

    move-result v0

    iget-object v1, p2, Ld0/g;->v:Ljava/lang/String;

    invoke-virtual {p0, v0, v1}, Le0/r;->g(ILjava/lang/String;)Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->w:Ljava/lang/String;
    :try_end_b
    .catch Ljava/lang/Exception; {:try_start_b .. :try_end_b} :catch_b

    goto :goto_b

    :catch_b
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_b
    const-string v0, "CellID1"

    invoke-static {p1, v0, v9}, Lf0/l;->h(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->x:Ljava/lang/String;

    const-string v0, "LAC1"

    invoke-static {p1, v0, v9}, Lf0/l;->h(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, p2, Ld0/g;->y:Ljava/lang/String;

    :cond_1
    return-void
.end method

.method protected declared-synchronized r(Landroid/content/Context;ILd0/g;)V
    .locals 8

    monitor-enter p0

    :try_start_0
    const-string v0, "android.permission.READ_PHONE_STATE"

    invoke-static {p1, v0}, Lj/a;->a(Landroid/content/Context;Ljava/lang/String;)I

    move-result v0

    if-eqz v0, :cond_0

    const-string p1, "DeviceInfoReaderBase"

    const-string p2, "setCellLoc no  READ_PHONE_STATE PERMISSION_GRANTED"

    invoke-static {p1, p2}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    monitor-exit p0

    return-void

    :cond_0
    :try_start_1
    invoke-static {p2}, Le0/r;->h(I)I

    move-result v0

    const-string v1, ""

    const-string v2, ""

    const-string v3, "phone"

    invoke-virtual {p1, v3}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Landroid/telephony/TelephonyManager;

    invoke-virtual {p1, v0}, Landroid/telephony/TelephonyManager;->createForSubscriptionId(I)Landroid/telephony/TelephonyManager;

    move-result-object p1

    invoke-static {p1}, Ld0/b;->a(Landroid/telephony/TelephonyManager;)Landroid/telephony/ServiceState;

    move-result-object p1

    if-eqz p1, :cond_a

    invoke-static {p1}, Le0/l;->a(Landroid/telephony/ServiceState;)Ljava/util/List;

    move-result-object p1

    invoke-interface {p1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p1

    :cond_1
    :goto_0
    invoke-interface {p1}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    const/4 v3, 0x1

    if-eqz v0, :cond_7

    invoke-interface {p1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/telephony/NetworkRegistrationInfo;

    invoke-static {v0}, Le0/m;->a(Landroid/telephony/NetworkRegistrationInfo;)I

    move-result v4

    const/4 v5, 0x2

    if-ne v4, v5, :cond_1

    invoke-static {v0}, Le0/n;->a(Landroid/telephony/NetworkRegistrationInfo;)I

    move-result v4

    if-ne v4, v3, :cond_1

    const-string v4, "DeviceInfoReaderBase"

    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    const-string v7, "cellIdentity:"

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-static {v0}, Le0/o;->a(Landroid/telephony/NetworkRegistrationInfo;)Landroid/telephony/CellIdentity;

    move-result-object v7

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v6

    invoke-static {v4, v6}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {v0}, Le0/o;->a(Landroid/telephony/NetworkRegistrationInfo;)Landroid/telephony/CellIdentity;

    move-result-object v0

    instance-of v4, v0, Landroid/telephony/CellIdentityGsm;

    if-eqz v4, :cond_2

    check-cast v0, Landroid/telephony/CellIdentityGsm;

    invoke-virtual {v0}, Landroid/telephony/CellIdentityGsm;->getCid()I

    move-result v1

    invoke-static {v1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0}, Landroid/telephony/CellIdentityGsm;->getLac()I

    move-result v0

    :goto_1
    invoke-static {v0}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v0

    move-object v2, v0

    goto :goto_0

    :cond_2
    instance-of v4, v0, Landroid/telephony/CellIdentityWcdma;

    if-eqz v4, :cond_3

    check-cast v0, Landroid/telephony/CellIdentityWcdma;

    invoke-virtual {v0}, Landroid/telephony/CellIdentityWcdma;->getCid()I

    move-result v1

    invoke-static {v1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0}, Landroid/telephony/CellIdentityWcdma;->getLac()I

    move-result v0

    goto :goto_1

    :cond_3
    instance-of v4, v0, Landroid/telephony/CellIdentityTdscdma;

    if-eqz v4, :cond_4

    check-cast v0, Landroid/telephony/CellIdentityTdscdma;

    invoke-static {v0}, Le0/p;->a(Landroid/telephony/CellIdentityTdscdma;)I

    move-result v1

    invoke-static {v1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v1

    invoke-static {v0}, Le0/q;->a(Landroid/telephony/CellIdentityTdscdma;)I

    move-result v0

    goto :goto_1

    :cond_4
    instance-of v4, v0, Landroid/telephony/CellIdentityCdma;

    if-eqz v4, :cond_5

    check-cast v0, Landroid/telephony/CellIdentityCdma;

    invoke-virtual {v0}, Landroid/telephony/CellIdentityCdma;->getBasestationId()I

    move-result v1

    invoke-static {v1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0}, Landroid/telephony/CellIdentityCdma;->getLongitude()I

    move-result v4

    invoke-virtual {v0}, Landroid/telephony/CellIdentityCdma;->getLatitude()I

    move-result v0

    const v6, 0x7fffffff

    if-eq v4, v6, :cond_1

    if-eq v0, v6, :cond_1

    const-string p1, "%d:%d"

    new-array v2, v5, [Ljava/lang/Object;

    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    const/4 v5, 0x0

    aput-object v4, v2, v5

    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v0

    aput-object v0, v2, v3

    invoke-static {p1, v2}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v2

    goto :goto_3

    :cond_5
    instance-of v4, v0, Landroid/telephony/CellIdentityLte;

    if-eqz v4, :cond_6

    check-cast v0, Landroid/telephony/CellIdentityLte;

    invoke-virtual {v0}, Landroid/telephony/CellIdentityLte;->getCi()I

    move-result p1

    invoke-static {p1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0}, Landroid/telephony/CellIdentityLte;->getTac()I

    move-result p1

    :goto_2
    invoke-static {p1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v2

    goto :goto_3

    :cond_6
    instance-of v4, v0, Landroid/telephony/CellIdentityNr;

    if-eqz v4, :cond_1

    check-cast v0, Landroid/telephony/CellIdentityNr;

    invoke-static {v0}, Le0/e;->a(Landroid/telephony/CellIdentityNr;)J

    move-result-wide v1

    invoke-static {v1, v2}, Ljava/lang/String;->valueOf(J)Ljava/lang/String;

    move-result-object v1

    invoke-static {v0}, Le0/f;->a(Landroid/telephony/CellIdentityNr;)I

    move-result p1

    goto :goto_2

    :cond_7
    :goto_3
    if-eqz p2, :cond_9

    if-eq p2, v3, :cond_8

    goto :goto_4

    :cond_8
    iput-object v1, p3, Ld0/g;->x:Ljava/lang/String;

    iput-object v2, p3, Ld0/g;->y:Ljava/lang/String;

    goto :goto_4

    :cond_9
    iput-object v1, p3, Ld0/g;->m:Ljava/lang/String;

    iput-object v2, p3, Ld0/g;->n:Ljava/lang/String;
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    :goto_4
    monitor-exit p0

    return-void

    :cond_a
    :try_start_2
    const-string p1, "DeviceInfoReaderBase"

    const-string p2, "setCellLoc failed!"

    invoke-static {p1, p2}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    monitor-exit p0

    return-void

    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method
