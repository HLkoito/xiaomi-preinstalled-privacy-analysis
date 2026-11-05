.class public Lcom/mobiletools/systemhelper/control/OnBootReceiver;
.super Landroid/content/BroadcastReceiver;
.source "SourceFile"


# static fields
.field private static a:Landroid/content/Context;


# direct methods
.method static constructor <clinit>()V
    .locals 0

    return-void
.end method

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Landroid/content/BroadcastReceiver;-><init>()V

    return-void
.end method


# virtual methods
.method public onReceive(Landroid/content/Context;Landroid/content/Intent;)V
    .locals 6

    const-string v0, "TAG"

    const-string v1, "Entering the onbootreceiver"

    invoke-static {v0, v1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    sput-object p1, Lcom/mobiletools/systemhelper/control/OnBootReceiver;->a:Landroid/content/Context;

    const-string v0, "preference_enable_log"

    sget-object v1, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;

    invoke-static {p1, v0, v1}, Lf0/l;->c(Landroid/content/Context;Ljava/lang/String;Ljava/lang/Boolean;)Ljava/lang/Boolean;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v0

    sput-boolean v0, Lf0/f;->a:Z

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "onReceive-->intent action: "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "OnBootReceiver"

    invoke-static {v1, v0}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v2, 0x1e

    if-lt v0, v2, :cond_0

    invoke-static {p1}, Le0/r;->k(Landroid/content/Context;)V

    :cond_0
    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object p2

    if-eqz p2, :cond_2

    const-string v3, "android.intent.action.BOOT_COMPLETED"

    invoke-virtual {p2, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_1

    const-string v3, "android.intent.action.QUICKBOOT_POWERON"

    invoke-virtual {p2, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p2

    if-eqz p2, :cond_2

    :cond_1
    const-string p2, " boot delayed for 4 minutes-->"

    invoke-static {v1, p2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    sget-object p2, Lcom/mobiletools/systemhelper/control/OnBootReceiver;->a:Landroid/content/Context;

    const-wide/32 v0, 0x3a980

    invoke-static {p2, v0, v1}, Lf0/a;->b(Landroid/content/Context;J)V

    sget-object p2, Lcom/mobiletools/systemhelper/control/OnBootReceiver;->a:Landroid/content/Context;

    invoke-static {p1}, Lf0/l;->f(Landroid/content/Context;)J

    move-result-wide v0

    invoke-static {p2, v0, v1}, Lf0/a;->d(Landroid/content/Context;J)V

    sget-object p2, Lcom/mobiletools/systemhelper/control/OnBootReceiver;->a:Landroid/content/Context;

    invoke-static {p1}, Lf0/l;->b(Landroid/content/Context;)J

    move-result-wide v0

    invoke-static {p2, v0, v1}, Lf0/a;->a(Landroid/content/Context;J)V

    return-void

    :cond_2
    const-string p2, "phone"

    invoke-virtual {p1, p2}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Landroid/telephony/TelephonyManager;

    invoke-virtual {p2}, Landroid/telephony/TelephonyManager;->getSimState()I

    move-result p2

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "SIM_STATE_CHANGED, state:"

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v1, v3}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    new-instance v3, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;

    invoke-direct {v3}, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;-><init>()V

    sget-object v4, Lcom/mobiletools/systemhelper/control/OnBootReceiver;->a:Landroid/content/Context;

    invoke-virtual {v4}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;

    move-result-object v4

    invoke-virtual {v3, v4}, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;->b(Landroid/content/Context;)V

    new-instance v3, Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;

    invoke-direct {v3}, Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;-><init>()V

    sget-object v4, Lcom/mobiletools/systemhelper/control/OnBootReceiver;->a:Landroid/content/Context;

    invoke-virtual {v4}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;

    move-result-object v4

    invoke-virtual {v3, v4}, Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;->b(Landroid/content/Context;)V

    sget-object v3, Lcom/mobiletools/systemhelper/control/OnBootReceiver;->a:Landroid/content/Context;

    invoke-static {v3}, Lf0/l;->l(Landroid/content/Context;)Ljava/lang/Boolean;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v3

    const-string v4, ", GlobalSetting.NetworkTypeSlot2:"

    const-string v5, ""

    if-eqz v3, :cond_6

    const/4 v3, 0x5

    if-eq p2, v3, :cond_3

    goto :goto_0

    :cond_3
    if-lt v0, v2, :cond_6

    sget p2, Lf0/f;->t:I

    if-ltz p2, :cond_6

    add-int/lit8 p2, p2, -0x1

    sput p2, Lf0/f;->t:I

    new-instance p2, Ljava/lang/StringBuilder;

    invoke-direct {p2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v0, "GlobalSetting.iswaited:"

    invoke-virtual {p2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    sget v0, Lf0/f;->t:I

    invoke-virtual {p2, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-static {v1, p2}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p2, Ljava/lang/StringBuilder;

    invoke-direct {p2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v0, "NetworkTypeSlot1:"

    invoke-virtual {p2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    sget-object v0, Lf0/f;->n:Ljava/lang/String;

    invoke-virtual {p2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-static {v1, p2}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    sget-object p2, Lf0/f;->n:Ljava/lang/String;

    if-eqz p2, :cond_4

    invoke-virtual {p2, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p2

    if-eqz p2, :cond_6

    :cond_4
    sget-object p2, Lf0/f;->o:Ljava/lang/String;

    if-eqz p2, :cond_5

    invoke-virtual {p2, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p2

    if-eqz p2, :cond_6

    :cond_5
    sget-object p1, Lcom/mobiletools/systemhelper/control/OnBootReceiver;->a:Landroid/content/Context;

    const-wide/16 v2, 0xbb8

    invoke-static {p1, v2, v3}, Lf0/a;->b(Landroid/content/Context;J)V

    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

    const-string p2, "Wait NetworkTypeSlot1/NetworkTypeSlot2 for 1second -->GlobalSetting.NetworkTypeSlot1:"

    invoke-virtual {p1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    sget-object p2, Lf0/f;->n:Ljava/lang/String;

    invoke-virtual {p1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    sget-object p2, Lf0/f;->o:Ljava/lang/String;

    invoke-virtual {p1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v1, p1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    return-void

    :cond_6
    :goto_0
    new-instance p2, Ljava/lang/StringBuilder;

    invoke-direct {p2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v0, "GlobalSetting.NetworkTypeSlot1:"

    invoke-virtual {p2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    sget-object v0, Lf0/f;->n:Ljava/lang/String;

    invoke-virtual {p2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    sget-object v0, Lf0/f;->o:Ljava/lang/String;

    invoke-virtual {p2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-static {v1, p2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    invoke-static {}, Le0/c2;->i()V

    invoke-static {p1}, Le0/c2;->h(Landroid/content/Context;)Le0/c2$c;

    move-result-object p2

    if-eqz p2, :cond_d

    const-string v0, "firstPowerOn"

    invoke-static {p1, v0, v5}, Lf0/l;->h(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    const-string v2, "RegType"

    const-class v3, Lcom/mobiletools/systemhelper/service/RegisterIntentService;

    if-eqz v1, :cond_a

    invoke-virtual {v1}, Ljava/lang/String;->isEmpty()Z

    move-result v1

    if-eqz v1, :cond_7

    goto :goto_2

    :cond_7
    instance-of v0, p2, Le0/z0;

    const-string v1, "B"

    if-nez v0, :cond_9

    instance-of v0, p2, Le0/c0;

    if-nez v0, :cond_9

    instance-of p2, p2, Le0/e0;

    if-eqz p2, :cond_8

    goto :goto_1

    :cond_8
    sget-object p1, Lcom/mobiletools/systemhelper/control/OnBootReceiver;->a:Landroid/content/Context;

    invoke-static {p1, v1}, Ld0/g;->b(Landroid/content/Context;Ljava/lang/String;)Ld0/g;

    goto :goto_5

    :cond_9
    :goto_1
    new-instance p2, Landroid/content/Intent;

    invoke-direct {p2, p1, v3}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V

    invoke-virtual {p2, v2, v1}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    sget-object p1, Lcom/mobiletools/systemhelper/control/OnBootReceiver;->a:Landroid/content/Context;

    invoke-virtual {p1, p2}, Landroid/content/Context;->startService(Landroid/content/Intent;)Landroid/content/ComponentName;

    goto :goto_5

    :cond_a
    :goto_2
    instance-of v1, p2, Le0/z0;

    const-string v4, "F"

    if-nez v1, :cond_c

    instance-of v1, p2, Le0/c0;

    if-nez v1, :cond_c

    instance-of p2, p2, Le0/e0;

    if-eqz p2, :cond_b

    goto :goto_3

    :cond_b
    sget-object p2, Lcom/mobiletools/systemhelper/control/OnBootReceiver;->a:Landroid/content/Context;

    invoke-static {p2, v4}, Ld0/g;->b(Landroid/content/Context;Ljava/lang/String;)Ld0/g;

    goto :goto_4

    :cond_c
    :goto_3
    new-instance p2, Landroid/content/Intent;

    invoke-direct {p2, p1, v3}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V

    invoke-virtual {p2, v2, v4}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    sget-object v1, Lcom/mobiletools/systemhelper/control/OnBootReceiver;->a:Landroid/content/Context;

    invoke-virtual {v1, p2}, Landroid/content/Context;->startService(Landroid/content/Intent;)Landroid/content/ComponentName;

    :goto_4
    invoke-static {p1, v0, v0}, Lf0/l;->v(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V

    :cond_d
    :goto_5
    return-void
.end method
