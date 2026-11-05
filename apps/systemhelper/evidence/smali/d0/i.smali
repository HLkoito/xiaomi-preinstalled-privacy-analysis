.class public Ld0/i;
.super Ljava/lang/Thread;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Ld0/i$a;,
        Ld0/i$b;
    }
.end annotation


# instance fields
.field private b:J

.field private c:J

.field private d:Landroid/content/pm/PackageManager;

.field private e:Landroid/content/Context;

.field private f:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Lf0/b;",
            ">;"
        }
    .end annotation
.end field

.field private g:Ld0/i$a;

.field private h:Ld0/g;


# direct methods
.method public constructor <init>(Landroid/content/Context;Ld0/g;Ld0/i$a;)V
    .locals 2

    invoke-direct {p0}, Ljava/lang/Thread;-><init>()V

    const-wide/16 v0, 0x0

    iput-wide v0, p0, Ld0/i;->b:J

    iput-wide v0, p0, Ld0/i;->c:J

    const/4 v0, 0x0

    iput-object v0, p0, Ld0/i;->f:Ljava/util/List;

    iput-object p1, p0, Ld0/i;->e:Landroid/content/Context;

    iput-object p2, p0, Ld0/i;->h:Ld0/g;

    iput-object p3, p0, Ld0/i;->g:Ld0/i$a;

    return-void
.end method

.method private a(Ljava/util/List;Landroid/app/usage/UsageStats;)Lf0/b;
    .locals 4
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List<",
            "Landroid/content/pm/ApplicationInfo;",
            ">;",
            "Landroid/app/usage/UsageStats;",
            ")",
            "Lf0/b;"
        }
    .end annotation

    new-instance v0, Lf0/b;

    invoke-direct {v0}, Lf0/b;-><init>()V

    invoke-virtual {p2}, Landroid/app/usage/UsageStats;->getPackageName()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Lf0/b;->d(Ljava/lang/String;)V

    invoke-interface {p1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p1

    :cond_0
    invoke-interface {p1}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_1

    invoke-interface {p1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/content/pm/ApplicationInfo;

    iget-object v2, v1, Landroid/content/pm/ApplicationInfo;->packageName:Ljava/lang/String;

    invoke-virtual {p2}, Landroid/app/usage/UsageStats;->getPackageName()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v2

    if-eqz v2, :cond_0

    iget p1, v1, Landroid/content/pm/ApplicationInfo;->uid:I

    invoke-virtual {v0, p1}, Lf0/b;->o(I)V

    iget-object p1, p0, Ld0/i;->d:Landroid/content/pm/PackageManager;

    invoke-virtual {v1, p1}, Landroid/content/pm/PackageItemInfo;->loadLabel(Landroid/content/pm/PackageManager;)Ljava/lang/CharSequence;

    move-result-object p1

    check-cast p1, Ljava/lang/String;

    invoke-virtual {v0, p1}, Lf0/b;->c(Ljava/lang/String;)V

    :cond_1
    invoke-virtual {p2}, Landroid/app/usage/UsageStats;->getTotalTimeInForeground()J

    move-result-wide p1

    const-wide/32 v1, 0xea60

    div-long/2addr p1, v1

    invoke-virtual {v0, p1, p2}, Lf0/b;->e(J)V

    invoke-direct {p0, v0}, Ld0/i;->b(Lf0/b;)V

    return-object v0
.end method

.method private b(Lf0/b;)V
    .locals 14

    iget-object v0, p0, Ld0/i;->e:Landroid/content/Context;

    const-string v1, "netstats"

    invoke-virtual {v0, v1}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/app/usage/NetworkStatsManager;

    iget-object v1, p0, Ld0/i;->h:Ld0/g;

    iget-object v1, v1, Ld0/g;->i:Ljava/lang/String;

    const-wide/16 v8, 0x400

    const-wide/16 v10, 0x0

    const/4 v12, 0x0

    if-eqz v1, :cond_2

    invoke-virtual {v1}, Ljava/lang/String;->isEmpty()Z

    move-result v1

    if-nez v1, :cond_2

    new-instance v13, Landroid/app/usage/NetworkStats$Bucket;

    invoke-direct {v13}, Landroid/app/usage/NetworkStats$Bucket;-><init>()V

    const/4 v2, 0x0

    :try_start_0
    iget-object v1, p0, Ld0/i;->h:Ld0/g;

    iget-object v3, v1, Ld0/g;->i:Ljava/lang/String;

    iget-wide v4, p0, Ld0/i;->c:J

    iget-wide v6, p0, Ld0/i;->b:J

    move-object v1, v0

    invoke-virtual/range {v1 .. v7}, Landroid/app/usage/NetworkStatsManager;->querySummary(ILjava/lang/String;JJ)Landroid/app/usage/NetworkStats;

    move-result-object v12
    :try_end_0
    .catch Landroid/os/RemoteException; {:try_start_0 .. :try_end_0} :catch_1

    move-wide v1, v10

    move-wide v3, v1

    :cond_0
    :try_start_1
    invoke-virtual {v12, v13}, Landroid/app/usage/NetworkStats;->getNextBucket(Landroid/app/usage/NetworkStats$Bucket;)Z

    invoke-virtual {v13}, Landroid/app/usage/NetworkStats$Bucket;->getUid()I

    move-result v5

    invoke-virtual {p1}, Lf0/b;->b()I

    move-result v6

    if-ne v6, v5, :cond_1

    invoke-virtual {v13}, Landroid/app/usage/NetworkStats$Bucket;->getRxBytes()J

    move-result-wide v5

    add-long/2addr v1, v5

    invoke-virtual {v13}, Landroid/app/usage/NetworkStats$Bucket;->getTxBytes()J

    move-result-wide v5

    add-long/2addr v3, v5

    :cond_1
    invoke-virtual {v12}, Landroid/app/usage/NetworkStats;->hasNextBucket()Z

    move-result v5
    :try_end_1
    .catch Landroid/os/RemoteException; {:try_start_1 .. :try_end_1} :catch_0

    if-nez v5, :cond_0

    goto :goto_1

    :catch_0
    move-exception v5

    goto :goto_0

    :catch_1
    move-exception v5

    move-wide v1, v10

    move-wide v3, v1

    :goto_0
    invoke-virtual {v5}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_1
    invoke-virtual {v12}, Landroid/app/usage/NetworkStats;->close()V

    div-long v5, v3, v8

    invoke-virtual {p1, v5, v6}, Lf0/b;->l(J)V

    div-long v5, v1, v8

    invoke-virtual {p1, v5, v6}, Lf0/b;->f(J)V

    add-long/2addr v3, v1

    div-long/2addr v3, v8

    invoke-virtual {p1, v3, v4}, Lf0/b;->i(J)V

    :cond_2
    iget-object v1, p0, Ld0/i;->h:Ld0/g;

    iget-object v1, v1, Ld0/g;->t:Ljava/lang/String;

    if-eqz v1, :cond_5

    invoke-virtual {v1}, Ljava/lang/String;->isEmpty()Z

    move-result v1

    if-nez v1, :cond_5

    new-instance v13, Landroid/app/usage/NetworkStats$Bucket;

    invoke-direct {v13}, Landroid/app/usage/NetworkStats$Bucket;-><init>()V

    const/4 v2, 0x0

    :try_start_2
    iget-object v1, p0, Ld0/i;->h:Ld0/g;

    iget-object v3, v1, Ld0/g;->t:Ljava/lang/String;

    iget-wide v4, p0, Ld0/i;->c:J

    iget-wide v6, p0, Ld0/i;->b:J

    move-object v1, v0

    invoke-virtual/range {v1 .. v7}, Landroid/app/usage/NetworkStatsManager;->querySummary(ILjava/lang/String;JJ)Landroid/app/usage/NetworkStats;

    move-result-object v12
    :try_end_2
    .catch Landroid/os/RemoteException; {:try_start_2 .. :try_end_2} :catch_3

    move-wide v1, v10

    move-wide v3, v1

    :cond_3
    :try_start_3
    invoke-virtual {v12, v13}, Landroid/app/usage/NetworkStats;->getNextBucket(Landroid/app/usage/NetworkStats$Bucket;)Z

    invoke-virtual {v13}, Landroid/app/usage/NetworkStats$Bucket;->getUid()I

    move-result v5

    invoke-virtual {p1}, Lf0/b;->b()I

    move-result v6

    if-ne v6, v5, :cond_4

    invoke-virtual {v13}, Landroid/app/usage/NetworkStats$Bucket;->getRxBytes()J

    move-result-wide v5

    add-long/2addr v1, v5

    invoke-virtual {v13}, Landroid/app/usage/NetworkStats$Bucket;->getTxBytes()J

    move-result-wide v5

    add-long/2addr v3, v5

    :cond_4
    invoke-virtual {v12}, Landroid/app/usage/NetworkStats;->hasNextBucket()Z

    move-result v5
    :try_end_3
    .catch Landroid/os/RemoteException; {:try_start_3 .. :try_end_3} :catch_2

    if-nez v5, :cond_3

    goto :goto_3

    :catch_2
    move-exception v5

    goto :goto_2

    :catch_3
    move-exception v5

    move-wide v1, v10

    move-wide v3, v1

    :goto_2
    invoke-virtual {v5}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_3
    invoke-virtual {v12}, Landroid/app/usage/NetworkStats;->close()V

    div-long v5, v3, v8

    invoke-virtual {p1, v5, v6}, Lf0/b;->m(J)V

    div-long v5, v1, v8

    invoke-virtual {p1, v5, v6}, Lf0/b;->g(J)V

    add-long/2addr v3, v1

    div-long/2addr v3, v8

    invoke-virtual {p1, v3, v4}, Lf0/b;->j(J)V

    :cond_5
    new-instance v8, Landroid/app/usage/NetworkStats$Bucket;

    invoke-direct {v8}, Landroid/app/usage/NetworkStats$Bucket;-><init>()V

    const/4 v2, 0x1

    const/4 v3, 0x0

    :try_start_4
    iget-wide v4, p0, Ld0/i;->c:J

    iget-wide v6, p0, Ld0/i;->b:J

    move-object v1, v0

    invoke-virtual/range {v1 .. v7}, Landroid/app/usage/NetworkStatsManager;->querySummary(ILjava/lang/String;JJ)Landroid/app/usage/NetworkStats;

    move-result-object v12
    :try_end_4
    .catch Landroid/os/RemoteException; {:try_start_4 .. :try_end_4} :catch_5

    move-wide v0, v10

    :cond_6
    :try_start_5
    invoke-virtual {v12, v8}, Landroid/app/usage/NetworkStats;->getNextBucket(Landroid/app/usage/NetworkStats$Bucket;)Z

    invoke-virtual {v8}, Landroid/app/usage/NetworkStats$Bucket;->getUid()I

    move-result v2

    invoke-virtual {p1}, Lf0/b;->b()I

    move-result v3

    if-ne v3, v2, :cond_7

    invoke-virtual {v8}, Landroid/app/usage/NetworkStats$Bucket;->getRxBytes()J

    move-result-wide v2

    add-long/2addr v10, v2

    invoke-virtual {v8}, Landroid/app/usage/NetworkStats$Bucket;->getTxBytes()J

    move-result-wide v2

    add-long/2addr v0, v2

    :cond_7
    invoke-virtual {v12}, Landroid/app/usage/NetworkStats;->hasNextBucket()Z

    move-result v2
    :try_end_5
    .catch Landroid/os/RemoteException; {:try_start_5 .. :try_end_5} :catch_4

    if-nez v2, :cond_6

    goto :goto_5

    :catch_4
    move-exception v2

    goto :goto_4

    :catch_5
    move-exception v2

    move-wide v0, v10

    :goto_4
    invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_5
    invoke-virtual {v12}, Landroid/app/usage/NetworkStats;->close()V

    const-wide/32 v2, 0x100000

    div-long v4, v0, v2

    invoke-virtual {p1, v4, v5}, Lf0/b;->n(J)V

    div-long v4, v10, v2

    invoke-virtual {p1, v4, v5}, Lf0/b;->h(J)V

    add-long/2addr v0, v10

    div-long/2addr v0, v2

    invoke-virtual {p1, v0, v1}, Lf0/b;->k(J)V

    return-void
.end method

.method private c()Ljava/util/List;
    .locals 6
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/List<",
            "Landroid/app/usage/UsageStats;",
            ">;"
        }
    .end annotation

    iget-object v0, p0, Ld0/i;->e:Landroid/content/Context;

    const-string v1, "usagestats"

    invoke-virtual {v0, v1}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/app/usage/UsageStatsManager;

    new-instance v1, Ljava/util/ArrayList;

    invoke-direct {v1}, Ljava/util/ArrayList;-><init>()V

    iget-wide v2, p0, Ld0/i;->c:J

    iget-wide v4, p0, Ld0/i;->b:J

    invoke-virtual {v0, v2, v3, v4, v5}, Landroid/app/usage/UsageStatsManager;->queryAndAggregateUsageStats(JJ)Ljava/util/Map;

    move-result-object v0

    invoke-interface {v0}, Ljava/util/Map;->entrySet()Ljava/util/Set;

    move-result-object v0

    invoke-interface {v0}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :goto_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_0

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/util/Map$Entry;

    invoke-interface {v2}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Landroid/app/usage/UsageStats;

    invoke-interface {v1, v2}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    goto :goto_0

    :cond_0
    return-object v1
.end method

.method private d()Ljava/util/List;
    .locals 4
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/List<",
            "Lf0/b;",
            ">;"
        }
    .end annotation

    iget-object v0, p0, Ld0/i;->e:Landroid/content/Context;

    invoke-virtual {v0}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object v0

    iput-object v0, p0, Ld0/i;->d:Landroid/content/pm/PackageManager;

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Landroid/content/pm/PackageManager;->getInstalledApplications(I)Ljava/util/List;

    move-result-object v0

    invoke-direct {p0}, Ld0/i;->c()Ljava/util/List;

    move-result-object v1

    if-eqz v1, :cond_0

    new-instance v2, Ld0/i$b;

    invoke-direct {v2, p0}, Ld0/i$b;-><init>(Ld0/i;)V

    invoke-static {v1, v2}, Ljava/util/Collections;->sort(Ljava/util/List;Ljava/util/Comparator;)V

    :cond_0
    new-instance v2, Ljava/util/ArrayList;

    invoke-direct {v2}, Ljava/util/ArrayList;-><init>()V

    invoke-interface {v1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :goto_0
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v3

    if-eqz v3, :cond_1

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Landroid/app/usage/UsageStats;

    invoke-direct {p0, v0, v3}, Ld0/i;->a(Ljava/util/List;Landroid/app/usage/UsageStats;)Lf0/b;

    move-result-object v3

    invoke-interface {v2, v3}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    goto :goto_0

    :cond_1
    return-object v2
.end method


# virtual methods
.method public run()V
    .locals 4

    iget-wide v0, p0, Ld0/i;->c:J

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-nez v0, :cond_0

    iget-wide v0, p0, Ld0/i;->b:J

    cmp-long v0, v0, v2

    if-nez v0, :cond_0

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v0

    iput-wide v0, p0, Ld0/i;->b:J

    sget-wide v2, Lf0/f;->z:J

    sub-long/2addr v0, v2

    iput-wide v0, p0, Ld0/i;->c:J

    :cond_0
    invoke-direct {p0}, Ld0/i;->d()Ljava/util/List;

    move-result-object v0

    iput-object v0, p0, Ld0/i;->f:Ljava/util/List;

    iget-object v1, p0, Ld0/i;->g:Ld0/i$a;

    iget-object v2, p0, Ld0/i;->e:Landroid/content/Context;

    iget-object v3, p0, Ld0/i;->h:Ld0/g;

    invoke-interface {v1, v2, v3, v0}, Ld0/i$a;->a(Landroid/content/Context;Ld0/g;Ljava/util/List;)V

    return-void
.end method
