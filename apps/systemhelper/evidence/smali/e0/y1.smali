.class public Le0/y1;
.super Le0/r;
.source "SourceFile"


# direct methods
.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Le0/r;-><init>()V

    return-void
.end method

.method private A(Landroid/content/Context;Ld0/g;)V
    .locals 21

    move-object/from16 v0, p0

    move-object/from16 v1, p2

    const-string v2, "android.telephony.TelephonyManager"

    invoke-static {v2}, Lf0/o;->a(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v2

    const-string v3, "phone"

    move-object/from16 v4, p1

    invoke-virtual {v4, v3}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Landroid/telephony/TelephonyManager;

    const-string v4, "miui.telephony.TelephonyManagerEx"

    invoke-static {v4}, Lf0/o;->a(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v4

    const/4 v5, 0x0

    invoke-static {v5}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v6

    new-array v7, v5, [Ljava/lang/Class;

    const-string v8, "getDefault"

    invoke-static {v4, v8, v7}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v7

    const/4 v8, 0x0

    new-array v9, v5, [Ljava/lang/Object;

    invoke-static {v7, v8, v9}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v7

    const/4 v8, 0x1

    invoke-static {v8}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v9

    new-array v10, v8, [Ljava/lang/Class;

    sget-object v11, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    aput-object v11, v10, v5

    const-string v12, "getImei"

    invoke-static {v2, v12, v10}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v10

    new-array v13, v8, [Ljava/lang/Object;

    aput-object v6, v13, v5

    invoke-static {v10, v3, v13}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v10

    check-cast v10, Ljava/lang/String;

    iput-object v10, v1, Ld0/g;->g:Ljava/lang/String;

    invoke-static {v5}, Le0/r;->h(I)I

    move-result v10

    invoke-static {v10}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v10

    invoke-virtual {v10}, Ljava/lang/Integer;->intValue()I

    move-result v13

    const-string v14, "getVoiceNetworkType"

    const-string v15, "getCellLocationForSlot"

    const-string v5, "getNetworkType"

    const-string v8, "getSimOperator"

    move-object/from16 v17, v9

    const-string v9, "getLine1Number"

    move-object/from16 v18, v12

    const-string v12, "getSimSerialNumber"

    move-object/from16 v19, v7

    const-string v7, "getSubscriberId"

    if-ltz v13, :cond_2

    move-object/from16 v16, v6

    const/4 v13, 0x1

    new-array v6, v13, [Ljava/lang/Class;

    const/16 v20, 0x0

    aput-object v11, v6, v20

    invoke-static {v2, v7, v6}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v6

    move-object/from16 p1, v7

    new-array v7, v13, [Ljava/lang/Object;

    aput-object v10, v7, v20

    invoke-static {v6, v3, v7}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Ljava/lang/String;

    iput-object v6, v1, Ld0/g;->i:Ljava/lang/String;

    if-eqz v6, :cond_1

    invoke-virtual {v6}, Ljava/lang/String;->length()I

    move-result v6

    if-lez v6, :cond_1

    new-array v6, v13, [Ljava/lang/Class;

    aput-object v11, v6, v20

    invoke-static {v2, v12, v6}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v6

    new-array v7, v13, [Ljava/lang/Object;

    aput-object v10, v7, v20

    invoke-static {v6, v3, v7}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Ljava/lang/String;

    iput-object v6, v1, Ld0/g;->h:Ljava/lang/String;

    new-array v6, v13, [Ljava/lang/Class;

    aput-object v11, v6, v20

    invoke-static {v2, v9, v6}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v6

    new-array v7, v13, [Ljava/lang/Object;

    aput-object v10, v7, v20

    invoke-static {v6, v3, v7}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Ljava/lang/String;

    iput-object v6, v1, Ld0/g;->j:Ljava/lang/String;

    new-array v6, v13, [Ljava/lang/Class;

    aput-object v11, v6, v20

    invoke-static {v2, v8, v6}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v6

    new-array v7, v13, [Ljava/lang/Object;

    aput-object v10, v7, v20

    invoke-static {v6, v3, v7}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Ljava/lang/String;

    iput-object v6, v1, Ld0/g;->k:Ljava/lang/String;

    new-array v6, v13, [Ljava/lang/Class;

    aput-object v11, v6, v20

    invoke-static {v2, v5, v6}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v6

    new-array v7, v13, [Ljava/lang/Object;

    aput-object v10, v7, v20

    invoke-static {v6, v3, v7}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Ljava/lang/Integer;

    invoke-virtual {v6}, Ljava/lang/Integer;->intValue()I

    move-result v6

    if-nez v6, :cond_0

    new-array v6, v13, [Ljava/lang/Class;

    aput-object v11, v6, v20

    invoke-static {v2, v14, v6}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v6

    new-array v7, v13, [Ljava/lang/Object;

    aput-object v10, v7, v20

    invoke-static {v6, v3, v7}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Ljava/lang/Integer;

    invoke-virtual {v6}, Ljava/lang/Integer;->intValue()I

    move-result v6

    :cond_0
    iget-object v7, v1, Ld0/g;->k:Ljava/lang/String;

    invoke-virtual {v0, v6, v7}, Le0/r;->g(ILjava/lang/String;)Ljava/lang/String;

    move-result-object v6

    iput-object v6, v1, Ld0/g;->l:Ljava/lang/String;

    new-array v6, v13, [Ljava/lang/Class;

    aput-object v11, v6, v20

    invoke-static {v4, v15, v6}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v6

    new-array v7, v13, [Ljava/lang/Object;

    aput-object v16, v7, v20

    move-object/from16 v10, v19

    invoke-static {v6, v10, v7}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Landroid/telephony/CellLocation;

    invoke-virtual {v1, v6}, Ld0/g;->f(Landroid/telephony/CellLocation;)V

    goto :goto_0

    :cond_1
    move-object/from16 v10, v19

    goto :goto_0

    :cond_2
    move-object/from16 p1, v7

    move-object/from16 v10, v19

    const/4 v13, 0x1

    const/16 v20, 0x0

    :goto_0
    new-array v6, v13, [Ljava/lang/Class;

    aput-object v11, v6, v20

    move-object/from16 v7, v18

    invoke-static {v2, v7, v6}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v6

    new-array v7, v13, [Ljava/lang/Object;

    aput-object v17, v7, v20

    invoke-static {v6, v3, v7}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Ljava/lang/String;

    iput-object v6, v1, Ld0/g;->r:Ljava/lang/String;

    invoke-static {v13}, Le0/r;->h(I)I

    move-result v6

    if-ltz v6, :cond_4

    new-array v7, v13, [Ljava/lang/Class;

    aput-object v11, v7, v20

    move-object/from16 v19, v10

    move-object/from16 v10, p1

    invoke-static {v2, v10, v7}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v7

    new-array v10, v13, [Ljava/lang/Object;

    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v16

    aput-object v16, v10, v20

    invoke-static {v7, v3, v10}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v7

    check-cast v7, Ljava/lang/String;

    iput-object v7, v1, Ld0/g;->t:Ljava/lang/String;

    if-eqz v7, :cond_4

    invoke-virtual {v7}, Ljava/lang/String;->length()I

    move-result v7

    if-lez v7, :cond_4

    new-array v7, v13, [Ljava/lang/Class;

    aput-object v11, v7, v20

    invoke-static {v2, v12, v7}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v7

    new-array v10, v13, [Ljava/lang/Object;

    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v12

    aput-object v12, v10, v20

    invoke-static {v7, v3, v10}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v7

    check-cast v7, Ljava/lang/String;

    iput-object v7, v1, Ld0/g;->s:Ljava/lang/String;

    new-array v7, v13, [Ljava/lang/Class;

    aput-object v11, v7, v20

    invoke-static {v2, v9, v7}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v7

    new-array v9, v13, [Ljava/lang/Object;

    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v10

    aput-object v10, v9, v20

    invoke-static {v7, v3, v9}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v7

    check-cast v7, Ljava/lang/String;

    iput-object v7, v1, Ld0/g;->u:Ljava/lang/String;

    new-array v7, v13, [Ljava/lang/Class;

    aput-object v11, v7, v20

    invoke-static {v2, v8, v7}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v7

    new-array v8, v13, [Ljava/lang/Object;

    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v9

    aput-object v9, v8, v20

    invoke-static {v7, v3, v8}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v7

    check-cast v7, Ljava/lang/String;

    iput-object v7, v1, Ld0/g;->v:Ljava/lang/String;

    new-array v7, v13, [Ljava/lang/Class;

    aput-object v11, v7, v20

    invoke-static {v2, v5, v7}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v5

    new-array v7, v13, [Ljava/lang/Object;

    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v8

    aput-object v8, v7, v20

    invoke-static {v5, v3, v7}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Ljava/lang/Integer;

    invoke-virtual {v5}, Ljava/lang/Integer;->intValue()I

    move-result v5

    if-nez v5, :cond_3

    new-array v5, v13, [Ljava/lang/Class;

    aput-object v11, v5, v20

    invoke-static {v2, v14, v5}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    new-array v5, v13, [Ljava/lang/Object;

    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v6

    aput-object v6, v5, v20

    invoke-static {v2, v3, v5}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/Integer;

    invoke-virtual {v2}, Ljava/lang/Integer;->intValue()I

    move-result v5

    :cond_3
    iget-object v2, v1, Ld0/g;->v:Ljava/lang/String;

    invoke-virtual {v0, v5, v2}, Le0/r;->g(ILjava/lang/String;)Ljava/lang/String;

    move-result-object v2

    iput-object v2, v1, Ld0/g;->w:Ljava/lang/String;

    new-array v2, v13, [Ljava/lang/Class;

    aput-object v11, v2, v20

    invoke-static {v4, v15, v2}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    new-array v3, v13, [Ljava/lang/Object;

    aput-object v17, v3, v20

    move-object/from16 v4, v19

    invoke-static {v2, v4, v3}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Landroid/telephony/CellLocation;

    invoke-virtual {v1, v2}, Ld0/g;->g(Landroid/telephony/CellLocation;)V

    :cond_4
    return-void
.end method

.method private B(Landroid/content/Context;Ld0/g;)V
    .locals 25

    move-object/from16 v0, p0

    move-object/from16 v1, p1

    move-object/from16 v2, p2

    const-string v3, "android.permission.READ_PHONE_STATE"

    invoke-static {v1, v3}, Lj/a;->a(Landroid/content/Context;Ljava/lang/String;)I

    move-result v3

    const-string v4, "DeviceInfoReaderXiaomi"

    if-eqz v3, :cond_0

    const-string v1, "readQualcomm_QR no  READ_PHONE_STATE PERMISSION_GRANTED"

    invoke-static {v4, v1}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V

    return-void

    :cond_0
    const-string v3, "android.telephony.TelephonyManager"

    invoke-static {v3}, Lf0/o;->a(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v3

    const-string v5, "phone"

    invoke-virtual {v1, v5}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Landroid/telephony/TelephonyManager;

    const-string v6, "miui.telephony.TelephonyManager"

    invoke-static {v6}, Lf0/o;->a(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v6

    const-string v7, "getDefault"

    const/4 v8, 0x0

    new-array v9, v8, [Ljava/lang/Class;

    invoke-static {v6, v7, v9}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v7

    const/4 v9, 0x0

    new-array v10, v8, [Ljava/lang/Object;

    invoke-static {v7, v9, v10}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v7

    sget-boolean v9, Lf0/f;->g:Z

    const-string v10, "getImei"

    const-string v11, ""

    const/4 v12, 0x1

    if-eqz v9, :cond_1

    new-array v9, v12, [Ljava/lang/Class;

    sget-object v13, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    aput-object v13, v9, v8

    invoke-static {v3, v10, v9}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v9

    new-array v13, v12, [Ljava/lang/Object;

    invoke-static {v8}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v14

    aput-object v14, v13, v8

    invoke-static {v9, v5, v13}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v9

    check-cast v9, Ljava/lang/String;

    iput-object v9, v2, Ld0/g;->g:Ljava/lang/String;

    goto :goto_0

    :cond_1
    iput-object v11, v2, Ld0/g;->g:Ljava/lang/String;

    :goto_0
    invoke-static {v8}, Le0/r;->h(I)I

    move-result v9

    invoke-static {v9}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v9

    new-instance v13, Ljava/lang/StringBuilder;

    invoke-direct {v13}, Ljava/lang/StringBuilder;-><init>()V

    const-string v14, "subId0:"

    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v13, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v13}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v13

    invoke-static {v4, v13}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v9}, Ljava/lang/Integer;->intValue()I

    move-result v13

    const-string v14, "1"

    const-string v15, "0"

    const-string v8, "getCellLocationForSlot"

    const-string v12, "getNetworkTypeForSubscription"

    move-object/from16 v18, v4

    const-string v4, "getSimOperatorForSubscription"

    move-object/from16 v19, v5

    const-string v5, "getSimSerialNumberForSubscription"

    move-object/from16 v20, v3

    const-string v3, "getSubscriberIdForSubscription"

    if-ltz v13, :cond_5

    move-object/from16 v21, v10

    const/4 v13, 0x1

    new-array v10, v13, [Ljava/lang/Class;

    sget-object v22, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    const/16 v16, 0x0

    aput-object v22, v10, v16

    invoke-static {v6, v3, v10}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v10

    move-object/from16 v23, v3

    new-array v3, v13, [Ljava/lang/Object;

    aput-object v9, v3, v16

    invoke-static {v10, v7, v3}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/String;

    iput-object v3, v2, Ld0/g;->i:Ljava/lang/String;

    if-eqz v3, :cond_4

    invoke-virtual {v3}, Ljava/lang/String;->length()I

    move-result v3

    if-lez v3, :cond_4

    new-array v3, v13, [Ljava/lang/Class;

    aput-object v22, v3, v16

    invoke-static {v6, v5, v3}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v3

    new-array v10, v13, [Ljava/lang/Object;

    aput-object v9, v10, v16

    invoke-static {v3, v7, v10}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/String;

    iput-object v3, v2, Ld0/g;->h:Ljava/lang/String;

    iput-object v11, v2, Ld0/g;->j:Ljava/lang/String;

    new-array v3, v13, [Ljava/lang/Class;

    aput-object v22, v3, v16

    invoke-static {v6, v4, v3}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v3

    new-array v10, v13, [Ljava/lang/Object;

    aput-object v9, v10, v16

    invoke-static {v3, v7, v10}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/String;

    iput-object v3, v2, Ld0/g;->k:Ljava/lang/String;

    sget v3, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v10, 0x1e

    if-lt v3, v10, :cond_3

    invoke-virtual {v9}, Ljava/lang/Integer;->intValue()I

    move-result v3

    invoke-virtual {v0, v1, v3}, Le0/r;->j(Landroid/content/Context;I)Z

    move-result v3

    if-eqz v3, :cond_2

    iput-object v14, v2, Ld0/g;->p:Ljava/lang/String;

    goto :goto_1

    :cond_2
    iput-object v15, v2, Ld0/g;->p:Ljava/lang/String;

    :goto_1
    sget-object v3, Lf0/f;->n:Ljava/lang/String;

    iput-object v3, v2, Ld0/g;->l:Ljava/lang/String;

    const/4 v3, 0x0

    invoke-virtual {v0, v1, v3, v2}, Le0/r;->r(Landroid/content/Context;ILd0/g;)V

    invoke-virtual {v0, v1, v3}, Le0/r;->d(Landroid/content/Context;I)Ljava/lang/String;

    move-result-object v9

    iput-object v9, v2, Ld0/g;->o:Ljava/lang/String;

    move-object/from16 v24, v12

    goto :goto_2

    :cond_3
    const/4 v3, 0x0

    const/4 v10, 0x1

    new-array v13, v10, [Ljava/lang/Class;

    aput-object v22, v13, v3

    invoke-static {v6, v12, v13}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v13

    move-object/from16 v24, v12

    new-array v12, v10, [Ljava/lang/Object;

    aput-object v9, v12, v3

    invoke-static {v13, v7, v12}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v9

    check-cast v9, Ljava/lang/Integer;

    invoke-virtual {v9}, Ljava/lang/Integer;->intValue()I

    move-result v9

    iget-object v12, v2, Ld0/g;->k:Ljava/lang/String;

    invoke-virtual {v0, v9, v12}, Le0/r;->g(ILjava/lang/String;)Ljava/lang/String;

    move-result-object v9

    iput-object v9, v2, Ld0/g;->l:Ljava/lang/String;

    new-array v9, v10, [Ljava/lang/Class;

    aput-object v22, v9, v3

    invoke-static {v6, v8, v9}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v9

    new-array v12, v10, [Ljava/lang/Object;

    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v10

    aput-object v10, v12, v3

    invoke-static {v9, v7, v12}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v9

    check-cast v9, Landroid/telephony/CellLocation;

    invoke-virtual {v2, v9}, Ld0/g;->f(Landroid/telephony/CellLocation;)V

    invoke-virtual {v0, v1, v3}, Le0/y1;->u(Landroid/content/Context;I)Ljava/lang/String;

    move-result-object v9

    iput-object v9, v2, Ld0/g;->o:Ljava/lang/String;

    goto :goto_2

    :cond_4
    move-object/from16 v24, v12

    move/from16 v3, v16

    goto :goto_2

    :cond_5
    move-object/from16 v23, v3

    move-object/from16 v21, v10

    move-object/from16 v24, v12

    const/4 v3, 0x0

    :goto_2
    sget-boolean v9, Lf0/f;->g:Z

    if-eqz v9, :cond_6

    const/4 v9, 0x1

    new-array v10, v9, [Ljava/lang/Class;

    sget-object v12, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    aput-object v12, v10, v3

    move-object/from16 v12, v20

    move-object/from16 v13, v21

    invoke-static {v12, v13, v10}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v10

    new-array v12, v9, [Ljava/lang/Object;

    invoke-static {v9}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v13

    aput-object v13, v12, v3

    move-object/from16 v3, v19

    invoke-static {v10, v3, v12}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/String;

    iput-object v3, v2, Ld0/g;->r:Ljava/lang/String;

    goto :goto_3

    :cond_6
    const/4 v9, 0x1

    iput-object v11, v2, Ld0/g;->r:Ljava/lang/String;

    :goto_3
    invoke-static {v9}, Le0/r;->h(I)I

    move-result v3

    new-instance v10, Ljava/lang/StringBuilder;

    invoke-direct {v10}, Ljava/lang/StringBuilder;-><init>()V

    const-string v12, "subId1:"

    invoke-virtual {v10, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v10, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v10

    move-object/from16 v12, v18

    invoke-static {v12, v10}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    if-ltz v3, :cond_9

    new-array v10, v9, [Ljava/lang/Class;

    sget-object v13, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    const/16 v16, 0x0

    aput-object v13, v10, v16

    move-object/from16 v18, v12

    move-object/from16 v12, v23

    invoke-static {v6, v12, v10}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v10

    new-array v12, v9, [Ljava/lang/Object;

    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v17

    aput-object v17, v12, v16

    invoke-static {v10, v7, v12}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v10

    check-cast v10, Ljava/lang/String;

    iput-object v10, v2, Ld0/g;->t:Ljava/lang/String;

    if-eqz v10, :cond_a

    invoke-virtual {v10}, Ljava/lang/String;->length()I

    move-result v10

    if-lez v10, :cond_a

    new-array v10, v9, [Ljava/lang/Class;

    aput-object v13, v10, v16

    invoke-static {v6, v5, v10}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v5

    new-array v10, v9, [Ljava/lang/Object;

    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v12

    aput-object v12, v10, v16

    invoke-static {v5, v7, v10}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Ljava/lang/String;

    iput-object v5, v2, Ld0/g;->s:Ljava/lang/String;

    iput-object v11, v2, Ld0/g;->j:Ljava/lang/String;

    new-array v5, v9, [Ljava/lang/Class;

    aput-object v13, v5, v16

    invoke-static {v6, v4, v5}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v4

    new-array v5, v9, [Ljava/lang/Object;

    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v9

    aput-object v9, v5, v16

    invoke-static {v4, v7, v5}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/lang/String;

    iput-object v4, v2, Ld0/g;->v:Ljava/lang/String;

    sget v4, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v5, 0x1e

    if-lt v4, v5, :cond_8

    invoke-virtual {v0, v1, v3}, Le0/r;->j(Landroid/content/Context;I)Z

    move-result v3

    if-eqz v3, :cond_7

    iput-object v14, v2, Ld0/g;->A:Ljava/lang/String;

    goto :goto_4

    :cond_7
    iput-object v15, v2, Ld0/g;->A:Ljava/lang/String;

    :goto_4
    sget-object v3, Lf0/f;->o:Ljava/lang/String;

    iput-object v3, v2, Ld0/g;->w:Ljava/lang/String;

    const/4 v4, 0x1

    invoke-virtual {v0, v1, v4, v2}, Le0/r;->r(Landroid/content/Context;ILd0/g;)V

    invoke-virtual {v0, v1, v4}, Le0/r;->d(Landroid/content/Context;I)Ljava/lang/String;

    move-result-object v1

    goto :goto_5

    :cond_8
    const/4 v4, 0x1

    new-array v5, v4, [Ljava/lang/Class;

    const/4 v9, 0x0

    aput-object v13, v5, v9

    move-object/from16 v10, v24

    invoke-static {v6, v10, v5}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v5

    new-array v10, v4, [Ljava/lang/Object;

    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v3

    aput-object v3, v10, v9

    invoke-static {v5, v7, v10}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/Integer;

    invoke-virtual {v3}, Ljava/lang/Integer;->intValue()I

    move-result v3

    iget-object v5, v2, Ld0/g;->v:Ljava/lang/String;

    invoke-virtual {v0, v3, v5}, Le0/r;->g(ILjava/lang/String;)Ljava/lang/String;

    move-result-object v3

    iput-object v3, v2, Ld0/g;->w:Ljava/lang/String;

    new-array v3, v4, [Ljava/lang/Class;

    aput-object v13, v3, v9

    invoke-static {v6, v8, v3}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v3

    new-array v5, v4, [Ljava/lang/Object;

    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v6

    aput-object v6, v5, v9

    invoke-static {v3, v7, v5}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Landroid/telephony/CellLocation;

    invoke-virtual {v2, v3}, Ld0/g;->g(Landroid/telephony/CellLocation;)V

    invoke-virtual {v0, v1, v4}, Le0/y1;->u(Landroid/content/Context;I)Ljava/lang/String;

    move-result-object v1

    :goto_5
    iput-object v1, v2, Ld0/g;->z:Ljava/lang/String;

    goto :goto_6

    :cond_9
    move-object/from16 v18, v12

    :cond_a
    :goto_6
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "readQualcomm_QR--NetworkTypeSlot1:"

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    sget-object v3, Lf0/f;->n:Ljava/lang/String;

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v3, ", NCLS1\uff1a"

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v3, v2, Ld0/g;->l:Ljava/lang/String;

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v3, ", NetworkTypeSlot2:"

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    sget-object v3, Lf0/f;->o:Ljava/lang/String;

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v3, ", NCLS2\uff1a"

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v2, v2, Ld0/g;->w:Ljava/lang/String;

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    move-object/from16 v2, v18

    invoke-static {v2, v1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    return-void
.end method

.method private t(Landroid/content/Context;)Z
    .locals 2

    invoke-virtual {p1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object p1

    const-string v0, "device_provisioned"

    const/4 v1, 0x0

    invoke-static {p1, v0, v1}, Landroid/provider/Settings$Global;->getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I

    move-result p1

    const/4 v0, 0x1

    if-ne p1, v0, :cond_0

    move v1, v0

    :cond_0
    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v0, "checkUserPermission:"

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1, v1}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    const-string v0, "DeviceInfoReaderXiaomi"

    invoke-static {v0, p1}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    return v1
.end method

.method private v(Ld0/g;)Ljava/lang/String;
    .locals 1

    iget-object p1, p1, Ld0/g;->b:Ljava/lang/String;

    const-string v0, "2106118C"

    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_0

    const-string p1, "ro.build.hardware.version"

    :goto_0
    invoke-static {p1}, Lf0/l;->i(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1

    :cond_0
    const-string p1, "ro.miui.cust_hardware"

    goto :goto_0
.end method

.method private w()Ljava/lang/String;
    .locals 1

    const-string v0, "ro.build.version.incremental"

    invoke-static {v0}, Lf0/l;->i(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public static x()Lorg/json/JSONObject;
    .locals 5

    invoke-static {}, Le0/y1;->y()Ljava/lang/String;

    move-result-object v0

    invoke-static {}, Lf0/i;->e()Ljava/lang/String;

    move-result-object v1

    invoke-static {}, Lf0/i;->a()Ljava/lang/String;

    move-result-object v2

    const-string v3, "Unknown"

    if-nez v0, :cond_0

    move-object v0, v3

    :cond_0
    if-nez v1, :cond_1

    move-object v1, v3

    :cond_1
    if-nez v2, :cond_2

    move-object v2, v3

    :cond_2
    new-instance v3, Lorg/json/JSONObject;

    invoke-direct {v3}, Lorg/json/JSONObject;-><init>()V

    :try_start_0
    const-string v4, "RAM"

    invoke-virtual {v3, v4, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string v0, "ROM"

    invoke-virtual {v3, v0, v1}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string v0, "FREE"

    invoke-virtual {v3, v0, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;
    :try_end_0
    .catch Lorg/json/JSONException; {:try_start_0 .. :try_end_0} :catch_0

    return-object v3

    :catch_0
    move-exception v0

    const-string v1, "DeviceInfoReaderXiaomi"

    invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v0

    invoke-static {v1, v0}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V

    const/4 v0, 0x0

    return-object v0
.end method

.method public static y()Ljava/lang/String;
    .locals 7

    const-string v0, "DeviceInfoReaderXiaomi"

    const/4 v1, -0x1

    :try_start_0
    new-instance v2, Ljava/io/BufferedReader;

    new-instance v3, Ljava/io/FileReader;

    const-string v4, "/proc/meminfo"

    invoke-direct {v3, v4}, Ljava/io/FileReader;-><init>(Ljava/lang/String;)V

    invoke-direct {v2, v3}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V

    invoke-virtual {v2}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;

    move-result-object v3

    const-string v4, "MemTotal:"

    invoke-virtual {v3, v4}, Ljava/lang/String;->indexOf(Ljava/lang/String;)I

    move-result v4

    add-int/lit8 v4, v4, 0x9

    const-string v5, "kB"

    invoke-virtual {v3, v5}, Ljava/lang/String;->indexOf(Ljava/lang/String;)I

    move-result v5

    invoke-virtual {v3, v4, v5}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v3

    invoke-static {v3}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v3

    int-to-double v3, v3

    const-wide/high16 v5, 0x4090000000000000L    # 1024.0

    div-double/2addr v3, v5

    div-double/2addr v3, v5

    invoke-static {v3, v4}, Ljava/lang/Math;->ceil(D)D

    move-result-wide v3

    double-to-int v1, v3

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "ddr_size_in_GB:"

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v0, v3}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const/16 v3, 0x10

    if-le v1, v3, :cond_0

    const/16 v4, 0x12

    if-gt v1, v4, :cond_0

    :goto_0
    move v1, v4

    goto :goto_2

    :cond_0
    const/16 v4, 0xc

    if-le v1, v4, :cond_1

    :goto_1
    move v1, v3

    goto :goto_2

    :cond_1
    const/16 v3, 0xa

    if-le v1, v3, :cond_2

    goto :goto_0

    :cond_2
    const/16 v4, 0x8

    if-le v1, v4, :cond_3

    goto :goto_1

    :cond_3
    const/4 v3, 0x6

    if-le v1, v3, :cond_4

    goto :goto_0

    :cond_4
    const/4 v4, 0x4

    if-le v1, v4, :cond_5

    goto :goto_1

    :cond_5
    const/4 v3, 0x3

    if-le v1, v3, :cond_6

    goto :goto_0

    :cond_6
    const/4 v4, 0x2

    if-le v1, v4, :cond_7

    goto :goto_1

    :cond_7
    const/4 v3, 0x1

    if-le v1, v3, :cond_8

    goto :goto_0

    :cond_8
    const/4 v1, 0x0

    :goto_2
    invoke-virtual {v2}, Ljava/io/BufferedReader;->close()V
    :try_end_0
    .catch Ljava/io/FileNotFoundException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_4

    :catch_0
    move-exception v2

    goto :goto_3

    :catch_1
    move-exception v2

    :goto_3
    invoke-virtual {v2}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v2}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V

    :goto_4
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v1, "G"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method private z()Z
    .locals 5

    const-string v0, "miui.telephony.TelephonyManagerEx"

    invoke-static {v0}, Lf0/o;->a(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v0

    const/4 v1, 0x0

    new-array v2, v1, [Ljava/lang/Class;

    const-string v3, "getDefault"

    invoke-static {v0, v3, v2}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    const/4 v3, 0x0

    new-array v4, v1, [Ljava/lang/Object;

    invoke-static {v2, v3, v4}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    const-string v3, "isFiveGCapable"

    new-array v4, v1, [Ljava/lang/Class;

    invoke-static {v0, v3, v4}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v0

    new-array v1, v1, [Ljava/lang/Object;

    invoke-static {v0, v2, v1}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/Boolean;

    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v0

    return v0
.end method


# virtual methods
.method public b(Landroid/content/Context;Ld0/g;)V
    .locals 10

    invoke-direct {p0, p1}, Le0/y1;->t(Landroid/content/Context;)Z

    move-result v0

    sput-boolean v0, Lf0/f;->g:Z

    sget-object v0, Landroid/os/Build;->MODEL:Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    sget-object v1, Le0/c2;->a:Ljava/util/Map;

    invoke-interface {v1, v0}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->a:Ljava/lang/String;

    const-string v0, "Android"

    iput-object v0, p2, Ld0/g;->e:Ljava/lang/String;

    sget-object v0, Landroid/os/Build$VERSION;->RELEASE:Ljava/lang/String;

    iput-object v0, p2, Ld0/g;->f:Ljava/lang/String;

    invoke-static {}, Le0/y1;->x()Lorg/json/JSONObject;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->F:Lorg/json/JSONObject;

    const-string v0, "telephony_subscription_service"

    invoke-virtual {p1, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/telephony/SubscriptionManager;

    invoke-static {}, Landroid/telephony/SubscriptionManager;->getDefaultDataSubscriptionId()I

    move-result v1

    sget v2, Landroid/os/Build$VERSION;->SDK_INT:I

    const-string v3, ""

    const/4 v4, 0x0

    const/4 v5, 0x1

    const/16 v6, 0x1d

    if-lt v2, v6, :cond_0

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-static {v1}, Le0/x;->a(I)I

    move-result v1

    add-int/2addr v1, v5

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    goto :goto_0

    :cond_0
    const-string v7, "android.telephony.SubscriptionManager"

    invoke-static {v7}, Lf0/o;->a(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v7

    new-array v8, v5, [Ljava/lang/Class;

    sget-object v9, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    aput-object v9, v8, v4

    const-string v9, "getPhoneId"

    invoke-static {v7, v9, v8}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v7

    new-array v8, v5, [Ljava/lang/Object;

    invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v1

    aput-object v1, v8, v4

    invoke-static {v7, v0, v8}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/Integer;

    invoke-virtual {v0}, Ljava/lang/Integer;->intValue()I

    move-result v0

    add-int/2addr v0, v5

    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v0

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    :goto_0
    iput-object v0, p2, Ld0/g;->B:Ljava/lang/String;

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "DataSlot:"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p2, Ld0/g;->B:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "DeviceInfoReaderXiaomi"

    invoke-static {v1, v0}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    const/16 v0, 0x1e

    const-string v7, "000005"

    const-string v8, "000004"

    if-lt v2, v0, :cond_2

    sget-boolean v0, Lf0/f;->d:Z

    if-eqz v0, :cond_2

    invoke-direct {p0}, Le0/y1;->z()Z

    move-result v0

    if-eqz v0, :cond_1

    iput-object v7, p2, Ld0/g;->G:Ljava/lang/String;

    goto :goto_1

    :cond_1
    iput-object v8, p2, Ld0/g;->G:Ljava/lang/String;

    :cond_2
    :goto_1
    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M1908F1XE"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "Mi9 Pro 5G"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2001J2C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "Mi 10"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2001J1C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "Mi 10 Pro"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2001G7AE"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "Redmi K30 5G"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2001J11C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2001J11E"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2001J11"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "Redmi K30 Pro Zoom Edition"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2002J9E"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2004J7BC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2004J7AC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2006J10C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2007J1SC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2007J22C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2007J17C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2011K2C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2012K11AC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2012K11C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2101K9C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2102K1AC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2102K1C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2102J2SC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2011J18C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2103K19C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2012K10C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2104K10AC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "M2104K11TC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2106118C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2107119DC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2109119BC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "21091116C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "21091116UC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "21091116AC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2112123AC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2201122C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2201123C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "21121210C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2201116SC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22011211C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22041211AC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22021211RC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22041219C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22041216C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22041216UC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2206123SC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2203121C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2206122SC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22061218C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22081212C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22095RA98C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2209129SC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2210132C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2211133C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22101316C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22101316UC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22101316UCP"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22101317C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22101320C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22122RK93C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "23013RK75C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "23049RAD8C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2304FPN6DC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "23046PNC9C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2308CPXD0C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "22127RK46C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "23076RA4BC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "23054RA19C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "23078RKD5C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2312DRAABC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2312DRA50C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "2312CRAD3C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "23090RA98C"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "23116PN5BC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_7

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v9, "23127PN0CC"

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_3

    goto/16 :goto_4

    :cond_3
    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1906G7T"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "Redmi Note 8 Pro"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1908C3IC"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "Redmi 8"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1908C3KE"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "Redmi 8A"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1908C3JC"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "Redmi Note 8"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1910F4E"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "MI CC9 Pro"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "MI CC9 Pro Premium Edition"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1901F7BE"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "Redmi Note 7 Pro"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1906F9SC"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "MI CC 9e"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1904F3BT"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "MI CC 9 Meitu Edition"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1912G7BC"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "Redmi K30"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M2003J15SC"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M2004J19C"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1901F7C"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "Redmi Note 7"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1903C3EC"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "Redmi 7A"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1810F6LT"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "Redmi 7"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1904F3BC"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "MI CC 9"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1902F1C"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "MI 9"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M1808D2TC"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "MI 8 Lite"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_5

    iget-object v0, p2, Ld0/g;->b:Ljava/lang/String;

    const-string v3, "M2006C3LC"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_4

    goto :goto_2

    :cond_4
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "Model:"

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v3, p2, Ld0/g;->b:Ljava/lang/String;

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v1, v0}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    if-lt v2, v6, :cond_6

    goto :goto_3

    :cond_5
    :goto_2
    invoke-direct {p0, p2}, Le0/y1;->v(Ld0/g;)Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->c:Ljava/lang/String;

    invoke-direct {p0}, Le0/y1;->w()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->d:Ljava/lang/String;

    iput-object v8, p2, Ld0/g;->G:Ljava/lang/String;

    if-lt v2, v6, :cond_6

    :goto_3
    invoke-direct {p0, p1, p2}, Le0/y1;->B(Landroid/content/Context;Ld0/g;)V

    goto/16 :goto_6

    :cond_6
    invoke-direct {p0, p1, p2}, Le0/y1;->A(Landroid/content/Context;Ld0/g;)V

    goto/16 :goto_6

    :cond_7
    :goto_4
    invoke-direct {p0, p2}, Le0/y1;->v(Ld0/g;)Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Ld0/g;->c:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v0

    const-string v1, "miui_version_name"

    invoke-static {v0, v1}, Landroid/provider/Settings$Global;->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v1

    if-nez v1, :cond_8

    invoke-virtual {v0}, Ljava/lang/String;->length()I

    move-result v1

    if-le v1, v5, :cond_8

    const-string v1, "\r|\n"

    invoke-virtual {v0, v1, v3}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    goto :goto_5

    :cond_8
    invoke-direct {p0}, Le0/y1;->w()Ljava/lang/String;

    move-result-object v0

    if-eqz v0, :cond_9

    invoke-direct {p0}, Le0/y1;->w()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/String;->length()I

    move-result v0

    if-eqz v0, :cond_9

    invoke-direct {p0}, Le0/y1;->w()Ljava/lang/String;

    move-result-object v0

    const-string v1, "."

    invoke-virtual {v0, v1}, Ljava/lang/String;->lastIndexOf(Ljava/lang/String;)I

    move-result v3

    invoke-virtual {v0, v5, v3}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v8

    invoke-virtual {v8, v1}, Ljava/lang/String;->indexOf(Ljava/lang/String;)I

    move-result v9

    add-int/2addr v9, v5

    invoke-virtual {v8, v1, v9}, Ljava/lang/String;->indexOf(Ljava/lang/String;I)I

    move-result v9

    add-int/2addr v9, v5

    invoke-virtual {v8, v1, v9}, Ljava/lang/String;->indexOf(Ljava/lang/String;I)I

    move-result v1

    invoke-virtual {v8, v4, v1}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v1

    add-int/2addr v3, v5

    invoke-virtual {v0, v3}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v0

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "MIUI "

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "("

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v0, ")"

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    goto :goto_5

    :cond_9
    invoke-direct {p0}, Le0/y1;->w()Ljava/lang/String;

    move-result-object v0

    :goto_5
    iput-object v0, p2, Ld0/g;->d:Ljava/lang/String;

    iput-object v7, p2, Ld0/g;->G:Ljava/lang/String;

    if-lt v2, v6, :cond_6

    goto/16 :goto_3

    :goto_6
    return-void
.end method

.method protected u(Landroid/content/Context;I)Ljava/lang/String;
    .locals 5

    const-string p1, "miui.telephony.TelephonyManager"

    invoke-static {p1}, Lf0/o;->a(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object p1

    const/4 v0, 0x0

    new-array v1, v0, [Ljava/lang/Class;

    const-string v2, "getDefault"

    invoke-static {p1, v2, v1}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v1

    const/4 v2, 0x0

    new-array v3, v0, [Ljava/lang/Object;

    invoke-static {v1, v2, v3}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    const/4 v2, 0x1

    new-array v3, v2, [Ljava/lang/Class;

    sget-object v4, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    aput-object v4, v3, v0

    const-string v4, "isVolteEnabledByUser"

    invoke-static {p1, v4, v3}, Lf0/o;->b(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object p1

    new-array v2, v2, [Ljava/lang/Object;

    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v3

    aput-object v3, v2, v0

    invoke-static {p1, v1, v2}, Lf0/o;->c(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Ljava/lang/Boolean;

    invoke-virtual {p1}, Ljava/lang/Boolean;->booleanValue()Z

    move-result p1

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "checkVolteState--slotId:"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string p2, ", result:"

    invoke-virtual {v0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    const-string v0, "DeviceRegister"

    invoke-static {v0, p2}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    if-eqz p1, :cond_0

    const-string p1, "1"

    goto :goto_0

    :cond_0
    const-string p1, "0"

    :goto_0
    return-object p1
.end method
