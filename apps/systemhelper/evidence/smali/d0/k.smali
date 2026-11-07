.class public Ld0/k;
.super Ljava/lang/Thread;
.source "SourceFile"


# instance fields
.field private b:Landroid/content/Context;

.field private c:Ld0/g;


# direct methods
.method public constructor <init>(Landroid/content/Context;Ld0/g;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Thread;-><init>()V

    iput-object p1, p0, Ld0/k;->b:Landroid/content/Context;

    iput-object p2, p0, Ld0/k;->c:Ld0/g;

    return-void
.end method

.method private a(Ljava/lang/String;)Z
    .locals 5

    if-eqz p1, :cond_1

    invoke-virtual {p1}, Ljava/lang/String;->isEmpty()Z

    move-result v0

    if-nez v0, :cond_1

    invoke-virtual {p1}, Ljava/lang/String;->length()I

    move-result v0

    const/16 v1, 0xf

    const/4 v2, 0x0

    const-string v3, "imei: "

    const-string v4, "RegisterThread"

    if-eq v0, v1, :cond_0

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p1, " length is not 15!"

    :goto_0
    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v4, p1}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V

    return v2

    :cond_0
    const-string v0, "[0-9]+"

    invoke-virtual {p1, v0}, Ljava/lang/String;->matches(Ljava/lang/String;)Z

    move-result v0

    if-nez v0, :cond_1

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p1, " is not all number!"

    goto :goto_0

    :cond_1
    const/4 p1, 0x1

    return p1
.end method

.method private declared-synchronized b(Ld0/g;)Z
    .locals 8

    monitor-enter p0

    :try_start_0
    iget-object v0, p0, Ld0/k;->b:Landroid/content/Context;

    sget-object v1, Lf0/d;->c:Ljava/lang/String;

    sget-object v2, Lf0/d;->b:Ljava/lang/Integer;

    invoke-virtual {v2}, Ljava/lang/Integer;->intValue()I

    move-result v2

    invoke-static {v0, v1, v2}, Lf0/d;->b(Landroid/content/Context;Ljava/lang/String;I)Lf0/d;

    move-result-object v0

    sget v1, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v2, 0x1c

    if-lt v1, v2, :cond_0

    invoke-virtual {v0}, Lf0/d;->e()Lorg/json/JSONArray;

    move-result-object v1

    :goto_0
    iput-object v1, p1, Ld0/g;->M:Lorg/json/JSONArray;

    goto :goto_1

    :cond_0
    invoke-virtual {v0}, Lf0/d;->d()Lorg/json/JSONArray;

    move-result-object v1

    goto :goto_0

    :goto_1
    invoke-virtual {v0}, Lf0/d;->close()V

    invoke-virtual {p1}, Ld0/g;->d()Ljava/lang/String;

    move-result-object v0

    const-string v1, "RegisterThread"

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "register postString\uff1a"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    const/4 v1, 0x0

    :try_start_1
    invoke-static {v0}, Lf0/c;->d(Ljava/lang/String;)[B

    move-result-object v2

    iget-object v3, p0, Ld0/k;->b:Landroid/content/Context;

    invoke-static {v3, v2}, Lf0/c;->c(Landroid/content/Context;[B)[B

    move-result-object v3

    invoke-static {v3}, Lf0/c;->b([B)Ljava/lang/String;

    move-result-object v3

    const-string v4, "utf-8"

    invoke-static {v3, v4}, Ljava/net/URLEncoder;->encode(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    iget-object v4, p1, Ld0/g;->b:Ljava/lang/String;

    const-string v5, "utf-8"

    invoke-static {v4, v5}, Ljava/net/URLEncoder;->encode(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v4

    iget-object v5, p1, Ld0/g;->a:Ljava/lang/String;

    const-string v6, "utf-8"

    invoke-static {v5, v6}, Ljava/net/URLEncoder;->encode(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    iget-object v7, p0, Ld0/k;->b:Landroid/content/Context;

    invoke-static {v7}, Lf0/l;->g(Landroid/content/Context;)Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v7, "?ver="

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v7, "3.0"

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v7, "&model="

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v4, "&manuf="

    invoke-virtual {v6, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v4, "&sign="

    invoke-virtual {v6, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v0, v2}, Lf0/c;->a(Ljava/lang/String;[B)[B

    move-result-object v0
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    :try_start_2
    new-instance v4, Lf0/g;

    invoke-direct {v4}, Lf0/g;-><init>()V

    invoke-virtual {v4, v3, v0, v2}, Lf0/g;->c(Ljava/lang/String;[B[B)Z

    move-result v0

    if-nez v0, :cond_1

    invoke-virtual {v4}, Lf0/g;->b()I

    move-result v1
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_0
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    const/16 v2, 0x1a0

    if-ne v1, v2, :cond_1

    const/4 v0, 0x1

    :cond_1
    move v1, v0

    goto :goto_2

    :catch_0
    move-exception v0

    :try_start_3
    const-string v2, "RegisterThread"

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "sendPOSTRequest exception--->"

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v2, v0}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V

    :goto_2
    if-eqz v1, :cond_2

    iget-object p1, p0, Ld0/k;->b:Landroid/content/Context;

    sget-object v0, Lf0/d;->c:Ljava/lang/String;

    sget-object v2, Lf0/d;->b:Ljava/lang/Integer;

    invoke-virtual {v2}, Ljava/lang/Integer;->intValue()I

    move-result v2

    invoke-static {p1, v0, v2}, Lf0/d;->b(Landroid/content/Context;Ljava/lang/String;I)Lf0/d;

    move-result-object p1

    invoke-virtual {p1}, Lf0/d;->a()V

    invoke-virtual {p1}, Lf0/d;->close()V

    goto :goto_3

    :cond_2
    const/4 v0, 0x0

    iput-object v0, p1, Ld0/g;->M:Lorg/json/JSONArray;

    iget-object v0, p0, Ld0/k;->b:Landroid/content/Context;

    sget-object v2, Lf0/d;->c:Ljava/lang/String;

    sget-object v3, Lf0/d;->b:Ljava/lang/Integer;

    invoke-virtual {v3}, Ljava/lang/Integer;->intValue()I

    move-result v3

    invoke-static {v0, v2, v3}, Lf0/d;->b(Landroid/content/Context;Ljava/lang/String;I)Lf0/d;

    move-result-object v0

    invoke-virtual {p1}, Ld0/g;->d()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v0, p1}, Lf0/d;->c(Ljava/lang/String;)V

    invoke-virtual {v0}, Lf0/d;->close()V
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    :goto_3
    monitor-exit p0

    return v1

    :catch_1
    move-exception p1

    :try_start_4
    const-string v0, "RegisterThread"

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "doEncrpytPost catch exception "

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v0, p1}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_0

    monitor-exit p0

    return v1

    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method

.method private c(Z)V
    .locals 0

    return-void
.end method

.method private declared-synchronized d(Ld0/g;)Z
    .locals 4

    monitor-enter p0

    :try_start_0
    iget-object v0, p1, Ld0/g;->g:Ljava/lang/String;

    invoke-direct {p0, v0}, Ld0/k;->a(Ljava/lang/String;)Z

    move-result v0

    const/4 v1, 0x0

    if-eqz v0, :cond_4

    iget-object v0, p1, Ld0/g;->r:Ljava/lang/String;

    invoke-direct {p0, v0}, Ld0/k;->a(Ljava/lang/String;)Z

    move-result v0

    if-nez v0, :cond_0

    goto/16 :goto_2

    :cond_0
    iget-object v0, p0, Ld0/k;->b:Landroid/content/Context;

    invoke-static {v0}, Lf0/l;->l(Landroid/content/Context;)Ljava/lang/Boolean;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v0

    if-eqz v0, :cond_3

    iget-object v0, p0, Ld0/k;->b:Landroid/content/Context;

    invoke-static {v0}, Lf0/l;->o(Landroid/content/Context;)Ljava/lang/Boolean;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v0

    if-eqz v0, :cond_1

    invoke-direct {p0, p1}, Ld0/k;->b(Ld0/g;)Z

    move-result p1
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    monitor-exit p0

    return p1

    :cond_1
    :try_start_1
    iget-object v0, p0, Ld0/k;->b:Landroid/content/Context;

    invoke-static {v0}, Le0/c2;->k(Landroid/content/Context;)Ljava/lang/Boolean;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v0

    if-nez v0, :cond_2

    invoke-static {p1}, Lf0/l;->m(Ld0/g;)Ljava/lang/Boolean;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v0

    if-eqz v0, :cond_2

    invoke-direct {p0, p1}, Ld0/k;->b(Ld0/g;)Z

    move-result p1
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit p0

    return p1

    :cond_2
    :try_start_2
    iget-object v0, p0, Ld0/k;->b:Landroid/content/Context;

    sget-object v2, Lf0/d;->c:Ljava/lang/String;

    sget-object v3, Lf0/d;->b:Ljava/lang/Integer;

    invoke-virtual {v3}, Ljava/lang/Integer;->intValue()I

    move-result v3

    invoke-static {v0, v2, v3}, Lf0/d;->b(Landroid/content/Context;Ljava/lang/String;I)Lf0/d;

    move-result-object v0

    invoke-virtual {p1}, Ld0/g;->d()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v0, p1}, Lf0/d;->c(Ljava/lang/String;)V

    invoke-virtual {v0}, Lf0/d;->close()V

    const-string p1, "RegisterThread"

    const-string v0, "isUnicomNetwork=false, not post!"

    :goto_0
    invoke-static {p1, v0}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_1

    :cond_3
    iget-object v0, p0, Ld0/k;->b:Landroid/content/Context;

    sget-object v2, Lf0/d;->c:Ljava/lang/String;

    sget-object v3, Lf0/d;->b:Ljava/lang/Integer;

    invoke-virtual {v3}, Ljava/lang/Integer;->intValue()I

    move-result v3

    invoke-static {v0, v2, v3}, Lf0/d;->b(Landroid/content/Context;Ljava/lang/String;I)Lf0/d;

    move-result-object v0

    invoke-virtual {p1}, Ld0/g;->d()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v0, p1}, Lf0/d;->c(Ljava/lang/String;)V

    invoke-virtual {v0}, Lf0/d;->close()V

    const-string p1, "RegisterThread"

    const-string v0, "No network connected!"
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    goto :goto_0

    :goto_1
    monitor-exit p0

    return v1

    :cond_4
    :goto_2
    monitor-exit p0

    return v1

    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method


# virtual methods
.method public run()V
    .locals 3

    sget-boolean v0, Lf0/f;->g:Z

    if-eqz v0, :cond_0

    iget-object v0, p0, Ld0/k;->c:Ld0/g;

    invoke-direct {p0, v0}, Ld0/k;->d(Ld0/g;)Z

    move-result v0

    invoke-direct {p0, v0}, Ld0/k;->c(Z)V

    goto :goto_0

    :cond_0
    iget-object v0, p0, Ld0/k;->b:Landroid/content/Context;

    sget-object v1, Lf0/d;->c:Ljava/lang/String;

    sget-object v2, Lf0/d;->b:Ljava/lang/Integer;

    invoke-virtual {v2}, Ljava/lang/Integer;->intValue()I

    move-result v2

    invoke-static {v0, v1, v2}, Lf0/d;->b(Landroid/content/Context;Ljava/lang/String;I)Lf0/d;

    move-result-object v0

    iget-object v1, p0, Ld0/k;->c:Ld0/g;

    invoke-virtual {v1}, Ld0/g;->d()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Lf0/d;->c(Ljava/lang/String;)V

    invoke-virtual {v0}, Lf0/d;->close()V

    const-string v0, "RegisterThread"

    const-string v1, "User Permission Closed!"

    invoke-static {v0, v1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    :goto_0
    return-void
.end method
