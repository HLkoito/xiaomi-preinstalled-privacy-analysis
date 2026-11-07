.class Lcom/mobiletools/systemhelper/service/RegisterHelperService$a;
.super Lcom/mobiletools/systemhelper/service/a$a;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/mobiletools/systemhelper/service/RegisterHelperService;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = "a"
.end annotation


# instance fields
.field final synthetic a:Lcom/mobiletools/systemhelper/service/RegisterHelperService;


# direct methods
.method constructor <init>(Lcom/mobiletools/systemhelper/service/RegisterHelperService;)V
    .locals 0

    iput-object p1, p0, Lcom/mobiletools/systemhelper/service/RegisterHelperService$a;->a:Lcom/mobiletools/systemhelper/service/RegisterHelperService;

    invoke-direct {p0}, Lcom/mobiletools/systemhelper/service/a$a;-><init>()V

    return-void
.end method


# virtual methods
.method public i(I)V
    .locals 3

    iget-object v0, p0, Lcom/mobiletools/systemhelper/service/RegisterHelperService$a;->a:Lcom/mobiletools/systemhelper/service/RegisterHelperService;

    invoke-static {v0}, Lcom/mobiletools/systemhelper/service/RegisterHelperService;->a(Lcom/mobiletools/systemhelper/service/RegisterHelperService;)Landroid/content/Context;

    move-result-object v0

    int-to-long v1, p1

    invoke-static {v0, v1, v2}, Lf0/l;->s(Landroid/content/Context;J)V

    iget-object v0, p0, Lcom/mobiletools/systemhelper/service/RegisterHelperService$a;->a:Lcom/mobiletools/systemhelper/service/RegisterHelperService;

    invoke-static {v0}, Lcom/mobiletools/systemhelper/service/RegisterHelperService;->a(Lcom/mobiletools/systemhelper/service/RegisterHelperService;)Landroid/content/Context;

    move-result-object v0

    mul-int/lit8 v1, p1, 0x8

    div-int/lit8 v1, v1, 0x5

    int-to-long v1, v1

    invoke-static {v0, v1, v2}, Lf0/l;->p(Landroid/content/Context;J)V

    iget-object v0, p0, Lcom/mobiletools/systemhelper/service/RegisterHelperService$a;->a:Lcom/mobiletools/systemhelper/service/RegisterHelperService;

    invoke-static {v0}, Lcom/mobiletools/systemhelper/service/RegisterHelperService;->a(Lcom/mobiletools/systemhelper/service/RegisterHelperService;)Landroid/content/Context;

    move-result-object v0

    mul-int/lit8 p1, p1, 0x9

    div-int/lit8 p1, p1, 0x5

    int-to-long v1, p1

    invoke-static {v0, v1, v2}, Lf0/l;->u(Landroid/content/Context;J)V

    return-void
.end method

.method public o()Z
    .locals 3

    iget-object v0, p0, Lcom/mobiletools/systemhelper/service/RegisterHelperService$a;->a:Lcom/mobiletools/systemhelper/service/RegisterHelperService;

    invoke-static {v0}, Lcom/mobiletools/systemhelper/service/RegisterHelperService;->a(Lcom/mobiletools/systemhelper/service/RegisterHelperService;)Landroid/content/Context;

    move-result-object v0

    const-string v1, "preference_enable_log"

    sget-object v2, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;

    invoke-static {v0, v1, v2}, Lf0/l;->c(Landroid/content/Context;Ljava/lang/String;Ljava/lang/Boolean;)Ljava/lang/Boolean;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v0

    return v0
.end method

.method public r(Ljava/lang/String;)V
    .locals 1

    iget-object v0, p0, Lcom/mobiletools/systemhelper/service/RegisterHelperService$a;->a:Lcom/mobiletools/systemhelper/service/RegisterHelperService;

    invoke-static {v0}, Lcom/mobiletools/systemhelper/service/RegisterHelperService;->a(Lcom/mobiletools/systemhelper/service/RegisterHelperService;)Landroid/content/Context;

    move-result-object v0

    invoke-static {v0, p1}, Lf0/l;->t(Landroid/content/Context;Ljava/lang/String;)V

    return-void
.end method

.method public s(Z)V
    .locals 2

    sput-boolean p1, Lf0/f;->a:Z

    iget-object v0, p0, Lcom/mobiletools/systemhelper/service/RegisterHelperService$a;->a:Lcom/mobiletools/systemhelper/service/RegisterHelperService;

    invoke-static {v0}, Lcom/mobiletools/systemhelper/service/RegisterHelperService;->a(Lcom/mobiletools/systemhelper/service/RegisterHelperService;)Landroid/content/Context;

    move-result-object v0

    const-string v1, "preference_enable_log"

    invoke-static {p1}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object p1

    invoke-static {v0, v1, p1}, Lf0/l;->q(Landroid/content/Context;Ljava/lang/String;Ljava/lang/Boolean;)V

    return-void
.end method

.method public u()V
    .locals 0

    invoke-static {}, Lf0/l;->a()V

    return-void
.end method
