.class public Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;
.super Landroid/content/BroadcastReceiver;
.source "SourceFile"


# static fields
.field private static a:Landroid/content/Context;

.field private static b:Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;


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

    sget-object v0, Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;->b:Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;

    if-eqz v0, :cond_0

    sget-object v1, Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;->a:Landroid/content/Context;

    invoke-virtual {v1, v0}, Landroid/content/Context;->unregisterReceiver(Landroid/content/BroadcastReceiver;)V

    const-string v0, "ConnectStateReceiver"

    const-string v1, "ConnectStateReceiver cancel watch-->"

    invoke-static {v0, v1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const/4 v0, 0x0

    sput-object v0, Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;->b:Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;

    const/4 v0, 0x0

    sput-boolean v0, Lf0/f;->v:Z

    :cond_0
    return-void
.end method


# virtual methods
.method public b(Landroid/content/Context;)V
    .locals 1

    sput-object p1, Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;->a:Landroid/content/Context;

    invoke-static {}, Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;->a()V

    const/4 p1, 0x1

    sput-boolean p1, Lf0/f;->v:Z

    sput-object p0, Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;->b:Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;

    const-string p1, "ConnectStateReceiver"

    const-string v0, "ConnectStateReceiver startWatch-->"

    invoke-static {p1, v0}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p1, Landroid/content/IntentFilter;

    invoke-direct {p1}, Landroid/content/IntentFilter;-><init>()V

    const-string v0, "android.net.conn.CONNECTIVITY_CHANGE"

    invoke-virtual {p1, v0}, Landroid/content/IntentFilter;->addAction(Ljava/lang/String;)V

    sget-object v0, Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;->a:Landroid/content/Context;

    invoke-virtual {v0, p0, p1}, Landroid/content/Context;->registerReceiver(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;

    return-void
.end method

.method public onReceive(Landroid/content/Context;Landroid/content/Intent;)V
    .locals 1

    sput-object p1, Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;->a:Landroid/content/Context;

    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v0, "onReceive-->intent action: "

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    const-string v0, "ConnectStateReceiver"

    invoke-static {v0, p1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    sget-boolean p1, Lf0/f;->v:Z

    if-eqz p1, :cond_0

    const/4 p1, 0x0

    sput-boolean p1, Lf0/f;->v:Z

    return-void

    :cond_0
    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object p1

    if-eqz p1, :cond_2

    const-string p2, "android.net.conn.CONNECTIVITY_CHANGE"

    invoke-virtual {p1, p2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_2

    sget-object p1, Lcom/mobiletools/systemhelper/control/ConnectStateReceiver;->a:Landroid/content/Context;

    invoke-static {p1}, Lf0/l;->l(Landroid/content/Context;)Ljava/lang/Boolean;

    move-result-object p1

    invoke-virtual {p1}, Ljava/lang/Boolean;->booleanValue()Z

    move-result p1

    if-nez p1, :cond_1

    const-string p1, "NetWork disconnected"

    goto :goto_0

    :cond_1
    const-string p1, "NetWork connected"

    :goto_0
    invoke-static {v0, p1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    :cond_2
    return-void
.end method
