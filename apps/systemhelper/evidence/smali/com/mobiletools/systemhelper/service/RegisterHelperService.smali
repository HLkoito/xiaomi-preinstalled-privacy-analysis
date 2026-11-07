.class public Lcom/mobiletools/systemhelper/service/RegisterHelperService;
.super Landroid/app/Service;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/mobiletools/systemhelper/service/RegisterHelperService$a;
    }
.end annotation


# instance fields
.field private b:Landroid/content/Context;


# direct methods
.method public constructor <init>()V
    .locals 1

    invoke-direct {p0}, Landroid/app/Service;-><init>()V

    const/4 v0, 0x0

    iput-object v0, p0, Lcom/mobiletools/systemhelper/service/RegisterHelperService;->b:Landroid/content/Context;

    return-void
.end method

.method static synthetic a(Lcom/mobiletools/systemhelper/service/RegisterHelperService;)Landroid/content/Context;
    .locals 0

    iget-object p0, p0, Lcom/mobiletools/systemhelper/service/RegisterHelperService;->b:Landroid/content/Context;

    return-object p0
.end method


# virtual methods
.method public onBind(Landroid/content/Intent;)Landroid/os/IBinder;
    .locals 1

    const-string p1, "RegisterHelperService"

    const-string v0, "registerhelper Service onBind sucessed!"

    invoke-static {p1, v0}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p1, Lcom/mobiletools/systemhelper/service/RegisterHelperService$a;

    invoke-direct {p1, p0}, Lcom/mobiletools/systemhelper/service/RegisterHelperService$a;-><init>(Lcom/mobiletools/systemhelper/service/RegisterHelperService;)V

    return-object p1
.end method

.method public onCreate()V
    .locals 3

    const-string v0, "RegisterHelperService"

    const-string v1, "registerhelper Service onCreate-->"

    invoke-static {v0, v1}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {p0}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;

    move-result-object v0

    iput-object v0, p0, Lcom/mobiletools/systemhelper/service/RegisterHelperService;->b:Landroid/content/Context;

    sget v1, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v2, 0x1e

    if-lt v1, v2, :cond_0

    invoke-static {v0}, Le0/r;->k(Landroid/content/Context;)V

    :cond_0
    return-void
.end method

.method public onDestroy()V
    .locals 2

    const-string v0, "RegisterHelperService"

    const-string v1, "registerhelper Service onDestroy--->"

    invoke-static {v0, v1}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V

    invoke-super {p0}, Landroid/app/Service;->onDestroy()V

    return-void
.end method

.method public onUnbind(Landroid/content/Intent;)Z
    .locals 1

    invoke-super {p0, p1}, Landroid/app/Service;->onUnbind(Landroid/content/Intent;)Z

    const-string p1, "RegisterHelperService"

    const-string v0, "registerhelper Service onUnbind sucessed!"

    invoke-static {p1, v0}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V

    const/4 p1, 0x1

    return p1
.end method
