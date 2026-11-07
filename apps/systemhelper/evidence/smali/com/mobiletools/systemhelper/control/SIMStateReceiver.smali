.class public Lcom/mobiletools/systemhelper/control/SIMStateReceiver;
.super Landroid/content/BroadcastReceiver;
.source "SourceFile"


# static fields
.field private static a:Landroid/content/Context;

.field private static b:Lcom/mobiletools/systemhelper/control/SIMStateReceiver;


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

.method public static a()V
    .locals 2

    sget-object v0, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;->b:Lcom/mobiletools/systemhelper/control/SIMStateReceiver;

    if-eqz v0, :cond_0

    sget-object v1, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;->a:Landroid/content/Context;

    invoke-virtual {v1, v0}, Landroid/content/Context;->unregisterReceiver(Landroid/content/BroadcastReceiver;)V

    const-string v0, "SIMStateReceiver"

    const-string v1, "SimStateReceiver cancel watch-->"

    invoke-static {v0, v1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const/4 v0, 0x0

    sput-object v0, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;->b:Lcom/mobiletools/systemhelper/control/SIMStateReceiver;

    const/4 v0, 0x0

    sput-boolean v0, Lf0/f;->u:Z

    :cond_0
    return-void
.end method


# virtual methods
.method public b(Landroid/content/Context;)V
    .locals 1

    sput-object p1, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;->a:Landroid/content/Context;

    invoke-static {}, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;->a()V

    const/4 p1, 0x1

    sput-boolean p1, Lf0/f;->u:Z

    sput-object p0, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;->b:Lcom/mobiletools/systemhelper/control/SIMStateReceiver;

    const-string p1, "SIMStateReceiver"

    const-string v0, "SimStateReceiver startWatch-->"

    invoke-static {p1, v0}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p1, Landroid/content/IntentFilter;

    invoke-direct {p1}, Landroid/content/IntentFilter;-><init>()V

    const-string v0, "android.intent.action.SIM_STATE_CHANGED"

    invoke-virtual {p1, v0}, Landroid/content/IntentFilter;->addAction(Ljava/lang/String;)V

    sget-object v0, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;->a:Landroid/content/Context;

    invoke-virtual {v0, p0, p1}, Landroid/content/Context;->registerReceiver(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;

    return-void
.end method

.method public onReceive(Landroid/content/Context;Landroid/content/Intent;)V
    .locals 3

    sput-object p1, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;->a:Landroid/content/Context;

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "onReceive-->intent action: "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "SIMStateReceiver"

    invoke-static {v1, v0}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    sget-boolean v0, Lf0/f;->u:Z

    if-eqz v0, :cond_0

    const/4 p1, 0x0

    sput-boolean p1, Lf0/f;->u:Z

    return-void

    :cond_0
    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object p2

    if-eqz p2, :cond_4

    const-string v0, "android.intent.action.SIM_STATE_CHANGED"

    invoke-virtual {p2, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p2

    if-eqz p2, :cond_4

    const-string p2, "phone"

    invoke-virtual {p1, p2}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Landroid/telephony/TelephonyManager;

    invoke-virtual {p2}, Landroid/telephony/TelephonyManager;->getSimState()I

    move-result p2

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "SIM_STATE_CHANGED, state:"

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v1, v0}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const/4 v0, 0x1

    const/16 v2, 0x1e

    if-eq p2, v0, :cond_1

    const/4 v0, 0x5

    if-eq p2, v0, :cond_2

    goto :goto_0

    :cond_1
    sget p2, Landroid/os/Build$VERSION;->SDK_INT:I

    if-lt p2, v2, :cond_2

    invoke-static {p1}, Le0/r;->s(Landroid/content/Context;)V

    const-string p2, ""

    sput-object p2, Lf0/f;->n:Ljava/lang/String;

    sput-object p2, Lf0/f;->o:Ljava/lang/String;

    :cond_2
    sget p2, Landroid/os/Build$VERSION;->SDK_INT:I

    if-lt p2, v2, :cond_3

    invoke-static {p1}, Le0/r;->k(Landroid/content/Context;)V

    :cond_3
    const-string p1, "wait for sim ready for 6 minutes-->"

    invoke-static {v1, p1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    sget-object p1, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;->a:Landroid/content/Context;

    const-wide/32 v0, 0x57e40

    invoke-static {p1, v0, v1}, Lf0/a;->c(Landroid/content/Context;J)V

    return-void

    :cond_4
    :goto_0
    invoke-static {}, Le0/c2;->i()V

    invoke-static {p1}, Le0/c2;->h(Landroid/content/Context;)Le0/c2$c;

    move-result-object p2

    if-eqz p2, :cond_7

    instance-of v0, p2, Le0/z0;

    const-string v1, "T"

    if-nez v0, :cond_6

    instance-of v0, p2, Le0/c0;

    if-nez v0, :cond_6

    instance-of p2, p2, Le0/e0;

    if-eqz p2, :cond_5

    goto :goto_1

    :cond_5
    sget-object p1, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;->a:Landroid/content/Context;

    invoke-static {p1, v1}, Ld0/g;->b(Landroid/content/Context;Ljava/lang/String;)Ld0/g;

    goto :goto_2

    :cond_6
    :goto_1
    new-instance p2, Landroid/content/Intent;

    const-class v0, Lcom/mobiletools/systemhelper/service/RegisterIntentService;

    invoke-direct {p2, p1, v0}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V

    const-string p1, "RegType"

    invoke-virtual {p2, p1, v1}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    sget-object p1, Lcom/mobiletools/systemhelper/control/SIMStateReceiver;->a:Landroid/content/Context;

    invoke-virtual {p1, p2}, Landroid/content/Context;->startService(Landroid/content/Intent;)Landroid/content/ComponentName;

    :cond_7
    :goto_2
    return-void
.end method
