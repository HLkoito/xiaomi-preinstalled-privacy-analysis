.class public Le0/c2;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Le0/c2$c;
    }
.end annotation


# static fields
.field public static a:Ljava/util/Map;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/Map<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field

.field private static b:Le0/c2$c;

.field private static c:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Lf0/b;",
            ">;"
        }
    .end annotation
.end field

.field private static d:Ld0/i$a;

.field private static e:Ld0/h$a;


# direct methods
.method static constructor <clinit>()V
    .locals 1

    new-instance v0, Le0/c2$a;

    invoke-direct {v0}, Le0/c2$a;-><init>()V

    sput-object v0, Le0/c2;->d:Ld0/i$a;

    new-instance v0, Le0/c2$b;

    invoke-direct {v0}, Le0/c2$b;-><init>()V

    sput-object v0, Le0/c2;->e:Ld0/h$a;

    return-void
.end method

.method static synthetic a(Landroid/content/Context;)Z
    .locals 0

    invoke-static {p0}, Le0/c2;->e(Landroid/content/Context;)Z

    move-result p0

    return p0
.end method

.method static synthetic b()Ljava/util/List;
    .locals 1

    sget-object v0, Le0/c2;->c:Ljava/util/List;

    return-object v0
.end method

.method static synthetic c(Ljava/util/List;)Ljava/util/List;
    .locals 0

    sput-object p0, Le0/c2;->c:Ljava/util/List;

    return-object p0
.end method

.method static synthetic d()Ld0/h$a;
    .locals 1

    sget-object v0, Le0/c2;->e:Ld0/h$a;

    return-object v0
.end method

.method private static e(Landroid/content/Context;)Z
    .locals 2

    invoke-virtual {p0}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object p0

    const-string v0, "device_provisioned"

    const/4 v1, 0x0

    invoke-static {p0, v0, v1}, Landroid/provider/Settings$Global;->getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I

    move-result p0

    const/4 v0, 0x1

    if-ne p0, v0, :cond_0

    move v1, v0

    :cond_0
    new-instance p0, Ljava/lang/StringBuilder;

    invoke-direct {p0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v0, "checkUserPermission:"

    invoke-virtual {p0, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0, v1}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    const-string v0, "DeviceManager"

    invoke-static {v0, p0}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    return v1
.end method

.method public static declared-synchronized f(Landroid/content/Context;)Ld0/g;
    .locals 4

    const-class v0, Le0/c2;

    monitor-enter v0

    :try_start_0
    const-string v1, "DeviceManager"

    const-string v2, "enter getCurDeviceInfo"

    invoke-static {v1, v2}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {}, Le0/c2;->i()V

    new-instance v1, Ld0/g;

    invoke-direct {v1}, Ld0/g;-><init>()V

    invoke-static {p0, v1}, Le0/c2;->n(Landroid/content/Context;Ld0/g;)V

    invoke-static {p0, v1}, Le0/c2;->l(Landroid/content/Context;Ld0/g;)Z

    invoke-static {p0, v1}, Le0/c2;->m(Landroid/content/Context;Ld0/g;)Z

    invoke-static {v1}, Le0/c2;->o(Ld0/g;)V

    new-instance v2, Ld0/i;

    sget-object v3, Le0/c2;->d:Ld0/i$a;

    invoke-direct {v2, p0, v1, v3}, Ld0/i;-><init>(Landroid/content/Context;Ld0/g;Ld0/i$a;)V

    invoke-virtual {v2}, Ljava/lang/Thread;->start()V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    monitor-exit v0

    return-object v1

    :catchall_0
    move-exception p0

    monitor-exit v0

    throw p0
.end method

.method public static declared-synchronized g(Landroid/content/Context;Ljava/lang/String;)Ld0/g;
    .locals 4

    const-class v0, Le0/c2;

    monitor-enter v0

    :try_start_0
    const-string v1, "DeviceManager"

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "enter getCurDeviceInfo--RegType:"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    new-instance v1, Ld0/g;

    invoke-direct {v1}, Ld0/g;-><init>()V

    iput-object p1, v1, Ld0/g;->H:Ljava/lang/String;

    invoke-static {p0, v1}, Le0/c2;->n(Landroid/content/Context;Ld0/g;)V

    invoke-virtual {p1}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v2

    const-string v3, "T"

    invoke-virtual {v2, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_0

    invoke-static {p0, v1}, Le0/c2;->l(Landroid/content/Context;Ld0/g;)Z

    move-result v2

    if-nez v2, :cond_1

    invoke-static {p0, v1}, Le0/c2;->m(Landroid/content/Context;Ld0/g;)Z

    move-result v2

    if-nez v2, :cond_1

    const-string p0, "DeviceManager"

    const-string p1, "SIM not changed\uff0cnot post\uff01"

    invoke-static {p0, p1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    monitor-exit v0

    return-object v1

    :cond_0
    :try_start_1
    invoke-static {p0, v1}, Le0/c2;->l(Landroid/content/Context;Ld0/g;)Z

    invoke-static {p0, v1}, Le0/c2;->m(Landroid/content/Context;Ld0/g;)Z

    :cond_1
    invoke-static {v1}, Le0/c2;->o(Ld0/g;)V

    invoke-static {p0}, Le0/c2;->e(Landroid/content/Context;)Z

    move-result v2

    sput-boolean v2, Lf0/f;->g:Z

    invoke-virtual {p1}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v2

    const-string v3, "A"

    invoke-virtual {v2, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_2

    sget-boolean v2, Lf0/f;->g:Z

    if-eqz v2, :cond_2

    new-instance p1, Ld0/i;

    sget-object v2, Le0/c2;->d:Ld0/i$a;

    invoke-direct {p1, p0, v1, v2}, Ld0/i;-><init>(Landroid/content/Context;Ld0/g;Ld0/i$a;)V

    :goto_0
    invoke-virtual {p1}, Ljava/lang/Thread;->start()V

    goto :goto_1

    :cond_2
    invoke-virtual {p1}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object p1

    const-string v2, "R"

    invoke-virtual {p1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_3

    new-instance p1, Ld0/j;

    invoke-direct {p1, p0, v1}, Ld0/j;-><init>(Landroid/content/Context;Ld0/g;)V

    invoke-virtual {p1}, Ljava/lang/Thread;->start()V

    const-string p0, "DeviceManager"

    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "deviceinfo.signalinfo:"

    invoke-virtual {p1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v2, v1, Ld0/g;->N:Ljava/lang/String;

    invoke-virtual {p1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {p0, p1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_1

    :cond_3
    new-instance p1, Ld0/k;

    invoke-direct {p1, p0, v1}, Ld0/k;-><init>(Landroid/content/Context;Ld0/g;)V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    goto :goto_0

    :goto_1
    monitor-exit v0

    return-object v1

    :catchall_0
    move-exception p0

    monitor-exit v0

    throw p0
.end method

.method public static h(Landroid/content/Context;)Le0/c2$c;
    .locals 3

    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v1, 0x1e

    if-lt v0, v1, :cond_0

    invoke-static {p0}, Le0/r;->k(Landroid/content/Context;)V

    :cond_0
    sget-object p0, Le0/c2;->b:Le0/c2$c;

    if-eqz p0, :cond_1

    return-object p0

    :cond_1
    sget-object p0, Le0/c2;->a:Ljava/util/Map;

    sget-object v0, Landroid/os/Build;->MODEL:Ljava/lang/String;

    invoke-interface {p0, v0}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Ljava/lang/String;

    const-string v1, "DeviceManager"

    if-eqz p0, :cond_40

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "manuf:"

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v1, v0}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const-string v0, "LETV"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_2

    new-instance p0, Le0/r0;

    invoke-direct {p0}, Le0/r0;-><init>()V

    goto/16 :goto_2

    :cond_2
    const-string v0, "HUAWEI"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_3

    new-instance p0, Le0/h0;

    invoke-direct {p0}, Le0/h0;-><init>()V

    goto/16 :goto_2

    :cond_3
    const-string v0, "BROR"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_4

    new-instance p0, Le0/a;

    invoke-direct {p0}, Le0/a;-><init>()V

    goto/16 :goto_2

    :cond_4
    const-string v0, "Xiaomi"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_5

    new-instance p0, Le0/y1;

    invoke-direct {p0}, Le0/y1;-><init>()V

    goto/16 :goto_2

    :cond_5
    const-string v0, "Onefive"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_6

    new-instance p0, Le0/b1;

    invoke-direct {p0}, Le0/b1;-><init>()V

    goto/16 :goto_2

    :cond_6
    const-string v0, "OYSIN"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_7

    new-instance p0, Le0/e1;

    invoke-direct {p0}, Le0/e1;-><init>()V

    goto/16 :goto_2

    :cond_7
    const-string v0, "meizu"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_8

    new-instance p0, Le0/u0;

    invoke-direct {p0}, Le0/u0;-><init>()V

    goto/16 :goto_2

    :cond_8
    const-string v0, "BenWee"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_9

    new-instance p0, Le0/t;

    invoke-direct {p0}, Le0/t;-><init>()V

    goto/16 :goto_2

    :cond_9
    const-string v0, "INTKI"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_a

    new-instance p0, Le0/k0;

    invoke-direct {p0}, Le0/k0;-><init>()V

    goto/16 :goto_2

    :cond_a
    const-string v0, "QIKU"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_b

    new-instance p0, Le0/l1;

    invoke-direct {p0}, Le0/l1;-><init>()V

    goto/16 :goto_2

    :cond_b
    const-string v0, "EAMEY"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_c

    new-instance p0, Le0/b0;

    invoke-direct {p0}, Le0/b0;-><init>()V

    goto/16 :goto_2

    :cond_c
    const-string v0, "ORANGE"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_d

    new-instance p0, Le0/d1;

    invoke-direct {p0}, Le0/d1;-><init>()V

    goto/16 :goto_2

    :cond_d
    const-string v0, "K-Touch"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_e

    new-instance p0, Le0/p0;

    invoke-direct {p0}, Le0/p0;-><init>()V

    goto/16 :goto_2

    :cond_e
    const-string v0, "Shown"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_f

    new-instance p0, Le0/n1;

    invoke-direct {p0}, Le0/n1;-><init>()V

    goto/16 :goto_2

    :cond_f
    const-string v0, "YuLong"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_10

    new-instance p0, Le0/a2;

    invoke-direct {p0}, Le0/a2;-><init>()V

    goto/16 :goto_2

    :cond_10
    const-string v0, "Hisense"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_11

    new-instance p0, Le0/g0;

    invoke-direct {p0}, Le0/g0;-><init>()V

    goto/16 :goto_2

    :cond_11
    const-string v0, "ZTE"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_12

    new-instance p0, Le0/b2;

    invoke-direct {p0}, Le0/b2;-><init>()V

    goto/16 :goto_2

    :cond_12
    const-string v0, "kingsun"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_13

    new-instance p0, Le0/n0;

    invoke-direct {p0}, Le0/n0;-><init>()V

    goto/16 :goto_2

    :cond_13
    const-string v0, "KONKA"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_14

    new-instance p0, Le0/o0;

    invoke-direct {p0}, Le0/o0;-><init>()V

    goto/16 :goto_2

    :cond_14
    const-string v1, "lephone"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_15

    new-instance p0, Le0/q0;

    invoke-direct {p0}, Le0/q0;-><init>()V

    goto/16 :goto_2

    :cond_15
    const-string v1, "SINTAVE"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_16

    new-instance p0, Le0/o1;

    invoke-direct {p0}, Le0/o1;-><init>()V

    goto/16 :goto_2

    :cond_16
    const-string v1, "TCL"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_17

    new-instance p0, Le0/s1;

    invoke-direct {p0}, Le0/s1;-><init>()V

    goto/16 :goto_2

    :cond_17
    const-string v1, "PPTV"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_18

    new-instance p0, Le0/i1;

    invoke-direct {p0}, Le0/i1;-><init>()V

    goto/16 :goto_2

    :cond_18
    const-string v1, "BestSonny"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_19

    new-instance p0, Le0/u;

    invoke-direct {p0}, Le0/u;-><init>()V

    goto/16 :goto_2

    :cond_19
    const-string v1, "SOP"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_1a

    new-instance p0, Le0/p1;

    invoke-direct {p0}, Le0/p1;-><init>()V

    goto/16 :goto_2

    :cond_1a
    const-string v1, "OPPO"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_1b

    new-instance p0, Le0/c1;

    invoke-direct {p0}, Le0/c1;-><init>()V

    goto/16 :goto_2

    :cond_1b
    const-string v1, "PHICOMM"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_1c

    new-instance p0, Le0/g1;

    invoke-direct {p0}, Le0/g1;-><init>()V

    goto/16 :goto_2

    :cond_1c
    const-string v1, "Spreadtrum"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_1d

    new-instance p0, Le0/r1;

    invoke-direct {p0}, Le0/r1;-><init>()V

    goto/16 :goto_2

    :cond_1d
    const-string v1, "BBK"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_3f

    const-string v1, "vivo"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_1e

    goto/16 :goto_1

    :cond_1e
    const-string v1, "CHANGHONG"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_1f

    new-instance p0, Le0/v;

    invoke-direct {p0}, Le0/v;-><init>()V

    goto/16 :goto_2

    :cond_1f
    const-string v1, "PHILIPS"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_20

    new-instance p0, Le0/h1;

    invoke-direct {p0}, Le0/h1;-><init>()V

    goto/16 :goto_2

    :cond_20
    const-string v1, "Bee"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_21

    new-instance p0, Le0/s;

    invoke-direct {p0}, Le0/s;-><init>()V

    goto/16 :goto_2

    :cond_21
    const-string v1, "ChuiZi"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_22

    new-instance p0, Le0/w;

    invoke-direct {p0}, Le0/w;-><init>()V

    goto/16 :goto_2

    :cond_22
    const-string v1, "PROTRULY"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_23

    new-instance p0, Le0/j1;

    invoke-direct {p0}, Le0/j1;-><init>()V

    goto/16 :goto_2

    :cond_23
    const-string v1, "Haipainoble"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_24

    new-instance p0, Le0/f0;

    invoke-direct {p0}, Le0/f0;-><init>()V

    goto/16 :goto_2

    :cond_24
    const-string v1, "NOAIN"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_25

    new-instance p0, Le0/y0;

    invoke-direct {p0}, Le0/y0;-><init>()V

    goto/16 :goto_2

    :cond_25
    const-string v1, "mfox"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_26

    new-instance p0, Le0/x0;

    invoke-direct {p0}, Le0/x0;-><init>()V

    goto/16 :goto_2

    :cond_26
    const-string v1, "gionee"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_27

    new-instance p0, Le0/d0;

    invoke-direct {p0}, Le0/d0;-><init>()V

    goto/16 :goto_2

    :cond_27
    const-string v1, "Xiaolajiao"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_28

    new-instance p0, Le0/x1;

    invoke-direct {p0}, Le0/x1;-><init>()V

    goto/16 :goto_2

    :cond_28
    const-string v1, "motorola"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_29

    new-instance p0, Le0/w0;

    invoke-direct {p0}, Le0/w0;-><init>()V

    goto/16 :goto_2

    :cond_29
    const-string v1, "nubia"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_2a

    new-instance p0, Le0/z0;

    invoke-direct {p0}, Le0/z0;-><init>()V

    goto/16 :goto_2

    :cond_2a
    const-string v1, "sprd"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_2b

    new-instance p0, Le0/c;

    invoke-direct {p0}, Le0/c;-><init>()V

    goto/16 :goto_2

    :cond_2b
    const-string v1, "Lenovo"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_2c

    new-instance p0, Le0/s0;

    invoke-direct {p0}, Le0/s0;-><init>()V

    goto/16 :goto_2

    :cond_2c
    const-string v1, "EWPE"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_2d

    new-instance p0, Le0/c0;

    invoke-direct {p0}, Le0/c0;-><init>()V

    goto/16 :goto_2

    :cond_2d
    const-string v1, "samsung"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_2e

    new-instance p0, Le0/q1;

    invoke-direct {p0}, Le0/q1;-><init>()V

    goto/16 :goto_2

    :cond_2e
    const-string v1, "realme"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_2f

    new-instance p0, Le0/m1;

    invoke-direct {p0}, Le0/m1;-><init>()V

    goto/16 :goto_2

    :cond_2f
    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_30

    new-instance p0, Le0/o0;

    invoke-direct {p0}, Le0/o0;-><init>()V

    goto/16 :goto_2

    :cond_30
    const-string v0, "OnePlus"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_31

    new-instance p0, Le0/f1;

    invoke-direct {p0}, Le0/f1;-><init>()V

    goto/16 :goto_2

    :cond_31
    const-string v0, "5G HYUN"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_32

    new-instance p0, Le0/a1;

    invoke-direct {p0}, Le0/a1;-><init>()V

    goto/16 :goto_2

    :cond_32
    const-string v0, "Liantong"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-nez v0, :cond_3e

    const-string v0, "H10"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_33

    goto/16 :goto_0

    :cond_33
    const-string v0, "Coolpad"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_34

    new-instance p0, Le0/y;

    invoke-direct {p0}, Le0/y;-><init>()V

    goto/16 :goto_2

    :cond_34
    const-string v0, "YIK"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_35

    new-instance p0, Le0/z1;

    invoke-direct {p0}, Le0/z1;-><init>()V

    goto/16 :goto_2

    :cond_35
    const-string v0, "TOSOT"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_36

    new-instance p0, Le0/e0;

    invoke-direct {p0}, Le0/e0;-><init>()V

    goto/16 :goto_2

    :cond_36
    const-string v0, "JingLing"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_37

    new-instance p0, Le0/m0;

    invoke-direct {p0}, Le0/m0;-><init>()V

    goto/16 :goto_2

    :cond_37
    const-string v0, "TDTECH"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_38

    new-instance p0, Le0/t1;

    invoke-direct {p0}, Le0/t1;-><init>()V

    goto/16 :goto_2

    :cond_38
    const-string v0, "PTAC"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_39

    new-instance p0, Le0/k1;

    invoke-direct {p0}, Le0/k1;-><init>()V

    goto :goto_2

    :cond_39
    const-string v0, "HZTL"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_3a

    new-instance p0, Le0/j0;

    invoke-direct {p0}, Le0/j0;-><init>()V

    goto :goto_2

    :cond_3a
    const-string v0, "WIKO"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_3b

    new-instance p0, Le0/v1;

    invoke-direct {p0}, Le0/v1;-><init>()V

    goto :goto_2

    :cond_3b
    const-string v0, "xiaodu"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_3c

    new-instance p0, Le0/w1;

    invoke-direct {p0}, Le0/w1;-><init>()V

    goto :goto_2

    :cond_3c
    const-string v0, "DOUQI"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result p0

    if-eqz p0, :cond_3d

    new-instance p0, Le0/a0;

    invoke-direct {p0}, Le0/a0;-><init>()V

    goto :goto_2

    :cond_3d
    new-instance p0, Le0/r;

    invoke-direct {p0}, Le0/r;-><init>()V

    goto :goto_2

    :cond_3e
    :goto_0
    new-instance p0, Le0/t0;

    invoke-direct {p0}, Le0/t0;-><init>()V

    goto :goto_2

    :cond_3f
    :goto_1
    new-instance p0, Le0/u1;

    invoke-direct {p0}, Le0/u1;-><init>()V

    goto :goto_2

    :cond_40
    new-instance p0, Ljava/lang/StringBuilder;

    invoke-direct {p0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "getReader error!! manufacture==null! model="

    invoke-virtual {p0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v1, p0}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p0, Le0/r;

    invoke-direct {p0}, Le0/r;-><init>()V

    :goto_2
    sput-object p0, Le0/c2;->b:Le0/c2$c;

    sget-object p0, Le0/c2;->b:Le0/c2$c;

    return-object p0
.end method

.method public static i()V
    .locals 7

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    if-nez v0, :cond_3

    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    sput-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v0, "ro.product.manufacturer"

    invoke-static {v0}, Lf0/l;->i(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "manuf:"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    const-string v2, "DeviceManager"

    invoke-static {v2, v1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const-string v1, "samsung"

    const-string v3, "realme"

    const-string v4, "vivo"

    if-eqz v0, :cond_1

    invoke-virtual {v0}, Ljava/lang/String;->isEmpty()Z

    move-result v5

    if-nez v5, :cond_1

    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    const-string v6, "manufacturer="

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-static {v2, v5}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v2, v4}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v2

    if-nez v2, :cond_0

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v2, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v2

    if-nez v2, :cond_0

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v2, v3}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v2

    if-nez v2, :cond_0

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v2

    const-string v5, "xiaodu"

    invoke-virtual {v2, v5}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v2

    if-nez v2, :cond_0

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v2

    const-string v5, "DOUQI"

    invoke-virtual {v2, v5}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v2

    if-eqz v2, :cond_1

    :cond_0
    sget-object v2, Le0/c2;->a:Ljava/util/Map;

    sget-object v5, Landroid/os/Build;->MODEL:Ljava/lang/String;

    invoke-interface {v2, v5, v0}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    :cond_1
    sget-object v2, Le0/c2;->a:Ljava/util/Map;

    const-string v5, "SM-G9750"

    invoke-interface {v2, v5, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v2, Le0/c2;->a:Ljava/util/Map;

    const-string v5, "SM-N9760"

    invoke-interface {v2, v5, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v2, Le0/c2;->a:Ljava/util/Map;

    const-string v5, "SM-N9700"

    invoke-interface {v2, v5, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v2, Le0/c2;->a:Ljava/util/Map;

    const-string v5, "SM-A5070"

    invoke-interface {v2, v5, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v2, Le0/c2;->a:Ljava/util/Map;

    const-string v5, "SM-A9080"

    invoke-interface {v2, v5, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v2, Le0/c2;->a:Ljava/util/Map;

    const-string v5, "SM-T735C"

    invoke-interface {v2, v5, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "EVR-AN00"

    const-string v5, "HUAWEI"

    invoke-interface {v1, v2, v5}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1911A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1916A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1922A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1923A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1924A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1930A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1901A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1921A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1914A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1936A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1913A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1934A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1932A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1941A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1945A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1938A"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1936AL"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1938CA"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1938T"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    const-string v2, "V1938CT"

    invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget v1, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v2, 0x1e

    const-string v4, "Xiaomi"

    if-lt v1, v2, :cond_2

    sget-boolean v1, Lf0/f;->d:Z

    if-eqz v1, :cond_2

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1, v4}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_2

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    sget-object v2, Landroid/os/Build;->MODEL:Ljava/lang/String;

    invoke-interface {v1, v2, v0}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    :cond_2
    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1908F1XE"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Mi9 Pro 5G"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1906G7T"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Redmi Note 8 Pro"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1908C3IC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Redmi 8"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1908C3KE"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Redmi 8A"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1908C3JC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Redmi Note 8"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1910F4E"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "MI CC9 Pro"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "MI CC9 Pro Premium Edition"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1901F7BE"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Redmi Note 7 Pro"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1906F9SC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "MI CC 9e"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1904F3BT"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "MI CC 9 Meitu Edition"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1912G7BC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Redmi K30"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2001J2C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Mi 10"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2001J1C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Mi 10 Pro"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2001G7AE"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Redmi K30 5G"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2001J11C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2001J11E"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2001J11"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Redmi K30 Pro Zoom Edition"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2003J15SC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2002J9E"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2004J7BC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2004J7AC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2004J19C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1901F7C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Redmi Note 7"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1903C3EC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Redmi 7A"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1810F6LT"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Redmi 7"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1904F3BC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "MI CC 9"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1902F1C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "MI 9"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M1808D2TC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "MI 8 Lite"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2006C3LC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2006J10C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2007J1SC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2007J22C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2007J17C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2011K2C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2012K11AC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2012K11C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2101K9C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2102K1AC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2102K1C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2102J2SC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2011J18C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2103K19C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2012K10C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2104K10AC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M2104K11TC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2106118C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2107119DC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2109119BC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "21091116C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "21091116UC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "21091116AC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2112123AC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2201122C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2201123C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "21121210C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2201116SC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22011211C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22041211AC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22021211RC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22041219C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22041216C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22041216UC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2206123SC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2203121C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2206122SC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22061218C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22081212C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22095RA98C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2209129SC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2210132C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2211133C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22101316C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22101316UC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22101316UCP"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22101317C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22101320C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22122RK93C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "22127RK46C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "23013RK75C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "23049RAD8C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2304FPN6DC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "23046PNC9C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2308CPXD0C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "23076RA4BC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "23054RA19C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "23078RKD5C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2312DRAABC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2312DRA50C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2312CRAD3C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "23090RA98C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "23116PN5BC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "23127PN0CC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "23113RKC6C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2311DRK48C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "23117RK66C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "23124RN87C"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "2311FRAFDC"

    invoke-interface {v0, v1, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "ZTE BV0870"

    const-string v2, "ZTE"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "ZTE A2020N2 Pro"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "ZTE 9000N"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "ZTE 7532N"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCKM00"

    const-string v2, "OPPO"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCHM30"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCHM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCHM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCKM80"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCNM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCLM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCGM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PBBM30"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCPM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PDBM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCAM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCRM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCLM50"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PDCM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCCM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCAM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCEM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PCDM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PBDM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PBEM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PDEM30"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PDEM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PAFM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PAHM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PDKM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PDAM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PAAM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PACM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PDPM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PDNM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PDYM20"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PDYM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEAM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PDVM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PECM30"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEGM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PDSM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PDRM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PELM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PENM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEPM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEQM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEHM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEMM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEDM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEEM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEGM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEMM20"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEFM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEXM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PFGM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEYM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PECM20"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PERM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PFVM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PFJM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PFCM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PFDM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PFJM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PFUM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PFEM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PFFM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PFTM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PFTM20"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PEUM00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PFZM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PGAM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PGBM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PGDM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PGNM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PHA120"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PHA110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PHJ110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PGT110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PGFM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PGU110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PHM110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PGX110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PGW110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PGEM10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PHQ110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PHS110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PHW110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PHV110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PHU110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PHN110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PHT110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PJG110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PJB110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PJS110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "PJU110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "VP003"

    const-string v2, "Liantong"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "VP001"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "VP004"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "VP005"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "S10"

    const-string v2, "H10"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "CP03"

    const-string v2, "Coolpad"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "M926Q"

    const-string v2, "meizu"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "meizu 16Xs"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "20190619G"

    const-string v2, "gionee"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "HLTE217T"

    const-string v2, "Hisense"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "HLTE226T"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "HLTE227T"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "HNR550T"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "HLTE260T"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "HNR560T"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "HNR553T"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "NX659J"

    const-string v2, "nubia"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "NX651J"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "angelcare N9"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "XDM-01"

    const-string v2, "sprd"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Lenovo TB-X505N"

    const-string v2, "Lenovo"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Lenovo TB-X605M"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Lenovo TB-X306NC_PRC"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Lenovo TB-8505N"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "G0515D"

    const-string v2, "EWPE"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RMX2201"

    invoke-interface {v0, v1, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RMX3093"

    invoke-interface {v0, v1, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RMX3122"

    invoke-interface {v0, v1, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RMX3043"

    invoke-interface {v0, v1, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RMX3143"

    invoke-interface {v0, v1, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RMX3334"

    invoke-interface {v0, v1, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RMX3362"

    invoke-interface {v0, v1, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RMX3125"

    invoke-interface {v0, v1, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RMX3461"

    invoke-interface {v0, v1, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RMX3300"

    invoke-interface {v0, v1, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RMX3611"

    invoke-interface {v0, v1, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RMX3781"

    invoke-interface {v0, v1, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RMX3706"

    invoke-interface {v0, v1, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "KONKA X10"

    const-string v2, "KONKA"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "LE2120"

    const-string v2, "OnePlus"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "LE2110"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "LE2100"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "5GP50"

    const-string v2, "5G HYUN"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "F810"

    const-string v2, "TCL"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "YK6701"

    const-string v2, "YIK"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "G0615D"

    const-string v2, "TOSOT"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "JingPad C1"

    const-string v2, "JingLing"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "RAI-AN01"

    const-string v2, "TDTECH"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "AVA-PA00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "TDT-MA01"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "FIO-BD00"

    const-string v2, "PTAC"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Hebe-BD00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "Hera-BD00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "MGZ-BD00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "ALH-BD00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "MIZ-BD00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "MRK-BD00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "TAI-M01"

    const-string v2, "HZTL"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "GAR-AN00"

    const-string v2, "WIKO"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "LGN-AN00"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "GAR-AN60"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-object v0, Le0/c2;->a:Ljava/util/Map;

    const-string v1, "XT2335-3"

    const-string v2, "motorola"

    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    :cond_3
    return-void
.end method

.method public static j(Landroid/content/Context;)Z
    .locals 2

    const/4 v0, 0x0

    if-nez p0, :cond_0

    return v0

    :cond_0
    invoke-static {p0}, Le0/c2;->h(Landroid/content/Context;)Le0/c2$c;

    move-result-object v1

    if-nez v1, :cond_1

    return v0

    :cond_1
    invoke-interface {v1, p0}, Le0/c2$c;->a(Landroid/content/Context;)Z

    move-result p0

    return p0
.end method

.method public static k(Landroid/content/Context;)Ljava/lang/Boolean;
    .locals 0

    if-nez p0, :cond_0

    sget-object p0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;

    return-object p0

    :cond_0
    invoke-static {}, Le0/c2;->i()V

    invoke-static {p0}, Le0/c2;->j(Landroid/content/Context;)Z

    move-result p0

    invoke-static {p0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object p0

    return-object p0
.end method

.method private static declared-synchronized l(Landroid/content/Context;Ld0/g;)Z
    .locals 6

    const-class v0, Le0/c2;

    monitor-enter v0

    :try_start_0
    const-string v1, "iccid1"

    const/4 v2, 0x0

    invoke-static {p0, v1, v2}, Lf0/l;->h(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    const/4 v2, 0x1

    if-eqz v1, :cond_0

    invoke-virtual {v1}, Ljava/lang/String;->isEmpty()Z

    move-result v3

    if-nez v3, :cond_0

    iget-object v3, p1, Ld0/g;->h:Ljava/lang/String;

    invoke-virtual {v1, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_1

    const-string v3, "DeviceManager"

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "SIM1 Changed,old iccid:"

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, ", new iccid:"

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p1, Ld0/g;->h:Ljava/lang/String;

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v3, v1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const-string v1, "iccid1"

    iget-object p1, p1, Ld0/g;->h:Ljava/lang/String;

    invoke-static {p0, v1, p1}, Lf0/l;->v(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    monitor-exit v0

    return v2

    :cond_0
    :try_start_1
    iget-object v3, p1, Ld0/g;->h:Ljava/lang/String;

    if-eqz v3, :cond_1

    invoke-virtual {v3}, Ljava/lang/String;->isEmpty()Z

    move-result v3

    if-nez v3, :cond_1

    const-string v3, "DeviceManager"

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "SIM1 Changed,old iccid:"

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, ", new iccid:"

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p1, Ld0/g;->h:Ljava/lang/String;

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v3, v1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const-string v1, "iccid1"

    iget-object p1, p1, Ld0/g;->h:Ljava/lang/String;

    invoke-static {p0, v1, p1}, Lf0/l;->v(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit v0

    return v2

    :cond_1
    monitor-exit v0

    const/4 p0, 0x0

    return p0

    :catchall_0
    move-exception p0

    monitor-exit v0

    throw p0
.end method

.method private static declared-synchronized m(Landroid/content/Context;Ld0/g;)Z
    .locals 6

    const-class v0, Le0/c2;

    monitor-enter v0

    :try_start_0
    const-string v1, "iccid2"

    const/4 v2, 0x0

    invoke-static {p0, v1, v2}, Lf0/l;->h(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    const/4 v2, 0x1

    if-eqz v1, :cond_0

    invoke-virtual {v1}, Ljava/lang/String;->isEmpty()Z

    move-result v3

    if-nez v3, :cond_0

    iget-object v3, p1, Ld0/g;->s:Ljava/lang/String;

    invoke-virtual {v1, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_1

    const-string v3, "DeviceManager"

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "SIM2 Changed,old iccid:"

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, ", new iccid:"

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p1, Ld0/g;->s:Ljava/lang/String;

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v3, v1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const-string v1, "iccid2"

    iget-object p1, p1, Ld0/g;->s:Ljava/lang/String;

    invoke-static {p0, v1, p1}, Lf0/l;->v(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    monitor-exit v0

    return v2

    :cond_0
    :try_start_1
    iget-object v3, p1, Ld0/g;->s:Ljava/lang/String;

    if-eqz v3, :cond_1

    invoke-virtual {v3}, Ljava/lang/String;->isEmpty()Z

    move-result v3

    if-nez v3, :cond_1

    const-string v3, "DeviceManager"

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "SIM2 Changed,old iccid:"

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, ", new iccid:"

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p1, Ld0/g;->s:Ljava/lang/String;

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v3, v1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const-string v1, "iccid2"

    iget-object p1, p1, Ld0/g;->s:Ljava/lang/String;

    invoke-static {p0, v1, p1}, Lf0/l;->v(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit v0

    return v2

    :cond_1
    monitor-exit v0

    const/4 p0, 0x0

    return p0

    :catchall_0
    move-exception p0

    monitor-exit v0

    throw p0
.end method

.method private static declared-synchronized n(Landroid/content/Context;Ld0/g;)V
    .locals 4

    const-class v0, Le0/c2;

    monitor-enter v0

    :try_start_0
    invoke-virtual {p0}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object v1

    invoke-virtual {p0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object v2

    const/4 v3, 0x0

    invoke-virtual {v1, v2, v3}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object v1

    iget-object v1, v1, Landroid/content/pm/PackageInfo;->versionName:Ljava/lang/String;

    iput-object v1, p1, Ld0/g;->J:Ljava/lang/String;
    :try_end_0
    .catch Landroid/content/pm/PackageManager$NameNotFoundException; {:try_start_0 .. :try_end_0} :catch_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    goto :goto_0

    :catchall_0
    move-exception p0

    goto/16 :goto_3

    :catch_0
    move-exception v1

    :try_start_1
    invoke-virtual {v1}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_0
    invoke-static {}, Le0/c2;->i()V

    invoke-static {p0}, Le0/c2;->h(Landroid/content/Context;)Le0/c2$c;

    move-result-object v1

    if-eqz v1, :cond_7

    invoke-interface {v1, p0, p1}, Le0/c2$c;->b(Landroid/content/Context;Ld0/g;)V

    iget-object v1, p1, Ld0/g;->i:Ljava/lang/String;

    const/4 v2, 0x0

    if-eqz v1, :cond_0

    if-eqz v1, :cond_1

    invoke-virtual {v1}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/String;->length()I

    move-result v1

    if-nez v1, :cond_1

    :cond_0
    iput-object v2, p1, Ld0/g;->i:Ljava/lang/String;

    :cond_1
    iget-object v1, p1, Ld0/g;->t:Ljava/lang/String;

    if-eqz v1, :cond_2

    if-eqz v1, :cond_3

    invoke-virtual {v1}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/String;->length()I

    move-result v1

    if-nez v1, :cond_3

    :cond_2
    iput-object v2, p1, Ld0/g;->t:Ljava/lang/String;

    :cond_3
    const-string v1, "android.permission.ACCESS_NETWORK_STATE"

    invoke-static {p0, v1}, Lj/a;->a(Landroid/content/Context;Ljava/lang/String;)I

    move-result v1

    if-nez v1, :cond_6

    invoke-static {p0}, Lf0/l;->l(Landroid/content/Context;)Ljava/lang/Boolean;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v1

    if-eqz v1, :cond_5

    invoke-static {p0}, Lf0/l;->o(Landroid/content/Context;)Ljava/lang/Boolean;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/Boolean;->booleanValue()Z

    move-result p0

    if-nez p0, :cond_4

    const-string p0, "Mobile"

    :goto_1
    iput-object p0, p1, Ld0/g;->C:Ljava/lang/String;

    goto :goto_2

    :cond_4
    const-string p0, "WiFi"

    goto :goto_1

    :cond_5
    const-string p0, "Disconnected"

    goto :goto_1

    :cond_6
    :goto_2
    invoke-static {}, Lf0/l;->d()Ljava/lang/String;

    move-result-object p0

    iput-object p0, p1, Ld0/g;->K:Ljava/lang/String;

    const-string p0, "DeviceManager"

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "Clear Text mDeivceInfo:"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ld0/g;->d()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {p0, p1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit v0

    return-void

    :cond_7
    :try_start_2
    const-string p0, "DeviceManager"

    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "manuf==null,model="

    invoke-virtual {p1, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    sget-object v1, Landroid/os/Build;->MODEL:Ljava/lang/String;

    invoke-virtual {p1, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {p0, p1}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    monitor-exit v0

    return-void

    :goto_3
    monitor-exit v0

    throw p0
.end method

.method private static declared-synchronized o(Ld0/g;)V
    .locals 12

    const-class v0, Le0/c2;

    monitor-enter v0

    :try_start_0
    iget-object v1, p0, Ld0/g;->g:Ljava/lang/String;

    if-eqz v1, :cond_1

    iget-object v2, p0, Ld0/g;->r:Ljava/lang/String;

    if-eqz v2, :cond_1

    invoke-virtual {v1}, Ljava/lang/String;->isEmpty()Z

    move-result v1

    if-nez v1, :cond_1

    iget-object v1, p0, Ld0/g;->r:Ljava/lang/String;

    invoke-virtual {v1}, Ljava/lang/String;->isEmpty()Z

    move-result v1

    if-nez v1, :cond_1

    iget-object v1, p0, Ld0/g;->g:Ljava/lang/String;

    iget-object v2, p0, Ld0/g;->r:Ljava/lang/String;

    invoke-virtual {v1, v2}, Ljava/lang/String;->compareTo(Ljava/lang/String;)I

    move-result v1

    if-lez v1, :cond_1

    new-instance v1, Ld0/g;

    invoke-direct {v1}, Ld0/g;-><init>()V

    iget-object v2, p0, Ld0/g;->g:Ljava/lang/String;

    iput-object v2, v1, Ld0/g;->g:Ljava/lang/String;

    iget-object v2, p0, Ld0/g;->h:Ljava/lang/String;

    iput-object v2, v1, Ld0/g;->h:Ljava/lang/String;

    iget-object v2, p0, Ld0/g;->i:Ljava/lang/String;

    iput-object v2, v1, Ld0/g;->i:Ljava/lang/String;

    iget-object v2, p0, Ld0/g;->j:Ljava/lang/String;

    iput-object v2, v1, Ld0/g;->j:Ljava/lang/String;

    iget-object v2, p0, Ld0/g;->k:Ljava/lang/String;

    iput-object v2, v1, Ld0/g;->k:Ljava/lang/String;

    iget-object v2, p0, Ld0/g;->l:Ljava/lang/String;

    iput-object v2, v1, Ld0/g;->l:Ljava/lang/String;

    iget-object v2, p0, Ld0/g;->m:Ljava/lang/String;

    iput-object v2, v1, Ld0/g;->m:Ljava/lang/String;

    iget-object v2, p0, Ld0/g;->n:Ljava/lang/String;

    iput-object v2, v1, Ld0/g;->n:Ljava/lang/String;

    iget-object v2, p0, Ld0/g;->o:Ljava/lang/String;

    iput-object v2, v1, Ld0/g;->o:Ljava/lang/String;

    iget-object v2, p0, Ld0/g;->p:Ljava/lang/String;

    iput-object v2, v1, Ld0/g;->p:Ljava/lang/String;

    iget-object v2, p0, Ld0/g;->r:Ljava/lang/String;

    iput-object v2, v1, Ld0/g;->r:Ljava/lang/String;

    iget-object v3, p0, Ld0/g;->s:Ljava/lang/String;

    iput-object v3, v1, Ld0/g;->s:Ljava/lang/String;

    iget-object v4, p0, Ld0/g;->t:Ljava/lang/String;

    iput-object v4, v1, Ld0/g;->t:Ljava/lang/String;

    iget-object v5, p0, Ld0/g;->u:Ljava/lang/String;

    iput-object v5, v1, Ld0/g;->u:Ljava/lang/String;

    iget-object v6, p0, Ld0/g;->v:Ljava/lang/String;

    iput-object v6, v1, Ld0/g;->v:Ljava/lang/String;

    iget-object v7, p0, Ld0/g;->w:Ljava/lang/String;

    iput-object v7, v1, Ld0/g;->w:Ljava/lang/String;

    iget-object v8, p0, Ld0/g;->x:Ljava/lang/String;

    iput-object v8, v1, Ld0/g;->x:Ljava/lang/String;

    iget-object v9, p0, Ld0/g;->y:Ljava/lang/String;

    iput-object v9, v1, Ld0/g;->y:Ljava/lang/String;

    iget-object v10, p0, Ld0/g;->z:Ljava/lang/String;

    iput-object v10, v1, Ld0/g;->z:Ljava/lang/String;

    iget-object v11, p0, Ld0/g;->A:Ljava/lang/String;

    iput-object v11, v1, Ld0/g;->A:Ljava/lang/String;

    iput-object v2, p0, Ld0/g;->g:Ljava/lang/String;

    iput-object v3, p0, Ld0/g;->h:Ljava/lang/String;

    iput-object v4, p0, Ld0/g;->i:Ljava/lang/String;

    iput-object v5, p0, Ld0/g;->j:Ljava/lang/String;

    iput-object v6, p0, Ld0/g;->k:Ljava/lang/String;

    iput-object v7, p0, Ld0/g;->l:Ljava/lang/String;

    iput-object v8, p0, Ld0/g;->m:Ljava/lang/String;

    iput-object v9, p0, Ld0/g;->n:Ljava/lang/String;

    iput-object v10, p0, Ld0/g;->o:Ljava/lang/String;

    iput-object v11, p0, Ld0/g;->p:Ljava/lang/String;

    iget-object v2, v1, Ld0/g;->g:Ljava/lang/String;

    iput-object v2, p0, Ld0/g;->r:Ljava/lang/String;

    iget-object v2, v1, Ld0/g;->h:Ljava/lang/String;

    iput-object v2, p0, Ld0/g;->s:Ljava/lang/String;

    iget-object v2, v1, Ld0/g;->i:Ljava/lang/String;

    iput-object v2, p0, Ld0/g;->t:Ljava/lang/String;

    iget-object v2, v1, Ld0/g;->j:Ljava/lang/String;

    iput-object v2, p0, Ld0/g;->u:Ljava/lang/String;

    iget-object v2, v1, Ld0/g;->k:Ljava/lang/String;

    iput-object v2, p0, Ld0/g;->v:Ljava/lang/String;

    iget-object v2, v1, Ld0/g;->l:Ljava/lang/String;

    iput-object v2, p0, Ld0/g;->w:Ljava/lang/String;

    iget-object v2, v1, Ld0/g;->m:Ljava/lang/String;

    iput-object v2, p0, Ld0/g;->x:Ljava/lang/String;

    iget-object v2, v1, Ld0/g;->n:Ljava/lang/String;

    iput-object v2, p0, Ld0/g;->y:Ljava/lang/String;

    iget-object v2, v1, Ld0/g;->o:Ljava/lang/String;

    iput-object v2, p0, Ld0/g;->z:Ljava/lang/String;

    iget-object v1, v1, Ld0/g;->p:Ljava/lang/String;

    iput-object v1, p0, Ld0/g;->A:Ljava/lang/String;

    iget-object v1, p0, Ld0/g;->B:Ljava/lang/String;

    if-eqz v1, :cond_1

    const-string v2, "1"

    invoke-virtual {v1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_0

    const-string v1, "2"

    :goto_0
    iput-object v1, p0, Ld0/g;->B:Ljava/lang/String;

    goto :goto_1

    :cond_0
    iget-object v1, p0, Ld0/g;->B:Ljava/lang/String;

    const-string v2, "2"

    invoke-virtual {v1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_1

    const-string v1, "1"

    goto :goto_0

    :cond_1
    :goto_1
    iget-object v1, p0, Ld0/g;->k:Ljava/lang/String;

    if-eqz v1, :cond_2

    const-string v2, ""

    invoke-virtual {v1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_2

    iget-object v1, p0, Ld0/g;->k:Ljava/lang/String;

    invoke-static {v1}, Lf0/l;->n(Ljava/lang/String;)Ljava/lang/Boolean;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v1

    if-nez v1, :cond_2

    iget-object v1, p0, Ld0/g;->h:Ljava/lang/String;

    invoke-static {v1}, Lf0/h;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Ld0/g;->h:Ljava/lang/String;

    iget-object v1, p0, Ld0/g;->j:Ljava/lang/String;

    invoke-static {v1}, Lf0/h;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Ld0/g;->j:Ljava/lang/String;

    iget-object v1, p0, Ld0/g;->i:Ljava/lang/String;

    invoke-static {v1}, Lf0/h;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Ld0/g;->i:Ljava/lang/String;

    iget-object v1, p0, Ld0/g;->m:Ljava/lang/String;

    invoke-static {v1}, Lf0/h;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Ld0/g;->m:Ljava/lang/String;

    iget-object v1, p0, Ld0/g;->n:Ljava/lang/String;

    invoke-static {v1}, Lf0/h;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Ld0/g;->n:Ljava/lang/String;

    :cond_2
    iget-object v1, p0, Ld0/g;->v:Ljava/lang/String;

    if-eqz v1, :cond_3

    const-string v2, ""

    invoke-virtual {v1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_3

    iget-object v1, p0, Ld0/g;->v:Ljava/lang/String;

    invoke-static {v1}, Lf0/l;->n(Ljava/lang/String;)Ljava/lang/Boolean;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v1

    if-nez v1, :cond_3

    iget-object v1, p0, Ld0/g;->s:Ljava/lang/String;

    invoke-static {v1}, Lf0/h;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Ld0/g;->s:Ljava/lang/String;

    iget-object v1, p0, Ld0/g;->u:Ljava/lang/String;

    invoke-static {v1}, Lf0/h;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Ld0/g;->u:Ljava/lang/String;

    iget-object v1, p0, Ld0/g;->t:Ljava/lang/String;

    invoke-static {v1}, Lf0/h;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Ld0/g;->t:Ljava/lang/String;

    iget-object v1, p0, Ld0/g;->x:Ljava/lang/String;

    invoke-static {v1}, Lf0/h;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Ld0/g;->x:Ljava/lang/String;

    iget-object v1, p0, Ld0/g;->y:Ljava/lang/String;

    invoke-static {v1}, Lf0/h;->a(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Ld0/g;->y:Ljava/lang/String;
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    :cond_3
    monitor-exit v0

    return-void

    :catchall_0
    move-exception p0

    monitor-exit v0

    throw p0
.end method
