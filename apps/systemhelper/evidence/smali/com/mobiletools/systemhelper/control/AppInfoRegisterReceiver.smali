.class public Lcom/mobiletools/systemhelper/control/AppInfoRegisterReceiver;
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
    .locals 3

    sput-object p1, Lcom/mobiletools/systemhelper/control/AppInfoRegisterReceiver;->a:Landroid/content/Context;

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "onReceive-->intent action: "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object p2

    invoke-virtual {v0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    const-string v0, "AppInfoRegisterReceiver"

    invoke-static {v0, p2}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    sget p2, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v0, 0x1e

    if-lt p2, v0, :cond_0

    invoke-static {p1}, Le0/r;->k(Landroid/content/Context;)V

    :cond_0
    sget-object v0, Lcom/mobiletools/systemhelper/control/AppInfoRegisterReceiver;->a:Landroid/content/Context;

    invoke-static {p1}, Lf0/l;->b(Landroid/content/Context;)J

    move-result-wide v1

    invoke-static {v0, v1, v2}, Lf0/a;->a(Landroid/content/Context;J)V

    invoke-static {}, Le0/c2;->i()V

    invoke-static {p1}, Le0/c2;->h(Landroid/content/Context;)Le0/c2$c;

    move-result-object v0

    if-eqz v0, :cond_3

    instance-of v1, v0, Le0/z0;

    const-string v2, "A"

    if-nez v1, :cond_2

    instance-of v1, v0, Le0/c0;

    if-nez v1, :cond_2

    instance-of v0, v0, Le0/e0;

    if-eqz v0, :cond_1

    goto :goto_0

    :cond_1
    sget-object v0, Lcom/mobiletools/systemhelper/control/AppInfoRegisterReceiver;->a:Landroid/content/Context;

    invoke-static {v0, v2}, Ld0/g;->b(Landroid/content/Context;Ljava/lang/String;)Ld0/g;

    const/16 v0, 0x1f

    if-lt p2, v0, :cond_3

    invoke-static {p1}, Le0/r;->s(Landroid/content/Context;)V

    goto :goto_1

    :cond_2
    :goto_0
    new-instance p2, Landroid/content/Intent;

    const-class v0, Lcom/mobiletools/systemhelper/service/RegisterIntentService;

    invoke-direct {p2, p1, v0}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V

    const-string p1, "RegType"

    invoke-virtual {p2, p1, v2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    sget-object p1, Lcom/mobiletools/systemhelper/control/AppInfoRegisterReceiver;->a:Landroid/content/Context;

    invoke-virtual {p1, p2}, Landroid/content/Context;->startService(Landroid/content/Intent;)Landroid/content/ComponentName;

    :cond_3
    :goto_1
    return-void
.end method
