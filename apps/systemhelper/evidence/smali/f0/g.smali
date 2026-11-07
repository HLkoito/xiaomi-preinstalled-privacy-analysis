.class public Lf0/g;
.super Ljava/lang/Object;
.source "SourceFile"


# instance fields
.field private a:Ljava/net/HttpURLConnection;

.field private b:I


# direct methods
.method public constructor <init>()V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const/4 v0, 0x0

    iput-object v0, p0, Lf0/g;->a:Ljava/net/HttpURLConnection;

    const/4 v0, 0x0

    iput v0, p0, Lf0/g;->b:I

    return-void
.end method


# virtual methods
.method public a()V
    .locals 1

    iget-object v0, p0, Lf0/g;->a:Ljava/net/HttpURLConnection;

    if-eqz v0, :cond_0

    invoke-virtual {v0}, Ljava/net/HttpURLConnection;->disconnect()V

    :cond_0
    return-void
.end method

.method public b()I
    .locals 1

    iget v0, p0, Lf0/g;->b:I

    return v0
.end method

.method public c(Ljava/lang/String;[B[B)Z
    .locals 3

    new-instance p3, Ljava/lang/StringBuilder;

    invoke-direct {p3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v0, "sendPOSTRequest() path ====== "

    invoke-virtual {p3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p3, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p3

    const-string v0, "HttpEncryptHelper"

    invoke-static {v0, p3}, Lf0/j;->a(Ljava/lang/String;Ljava/lang/String;)V

    :try_start_0
    new-instance p3, Ljava/net/URL;

    invoke-direct {p3, p1}, Ljava/net/URL;-><init>(Ljava/lang/String;)V

    invoke-virtual {p3}, Ljava/net/URL;->openConnection()Ljava/net/URLConnection;

    move-result-object p1

    check-cast p1, Ljava/net/HttpURLConnection;

    iput-object p1, p0, Lf0/g;->a:Ljava/net/HttpURLConnection;

    const/16 p3, 0x2710

    invoke-virtual {p1, p3}, Ljava/net/URLConnection;->setConnectTimeout(I)V

    iget-object p1, p0, Lf0/g;->a:Ljava/net/HttpURLConnection;

    const/16 p3, 0x3a98

    invoke-virtual {p1, p3}, Ljava/net/URLConnection;->setReadTimeout(I)V

    iget-object p1, p0, Lf0/g;->a:Ljava/net/HttpURLConnection;

    const-string p3, "POST"

    invoke-virtual {p1, p3}, Ljava/net/HttpURLConnection;->setRequestMethod(Ljava/lang/String;)V

    iget-object p1, p0, Lf0/g;->a:Ljava/net/HttpURLConnection;

    const/4 p3, 0x1

    invoke-virtual {p1, p3}, Ljava/net/URLConnection;->setDoOutput(Z)V

    iget-object p1, p0, Lf0/g;->a:Ljava/net/HttpURLConnection;

    invoke-virtual {p1, p3}, Ljava/net/URLConnection;->setDoInput(Z)V

    iget-object p1, p0, Lf0/g;->a:Ljava/net/HttpURLConnection;

    const-string v1, "Content-Type"

    const-string v2, "application/encrypted-json"

    invoke-virtual {p1, v1, v2}, Ljava/net/URLConnection;->setRequestProperty(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p1, p0, Lf0/g;->a:Ljava/net/HttpURLConnection;

    const-string v1, "Content-Length"

    array-length v2, p2

    invoke-static {v2}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {p1, v1, v2}, Ljava/net/URLConnection;->setRequestProperty(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p1, p0, Lf0/g;->a:Ljava/net/HttpURLConnection;

    invoke-virtual {p1}, Ljava/net/URLConnection;->getOutputStream()Ljava/io/OutputStream;

    move-result-object p1

    invoke-virtual {p1, p2}, Ljava/io/OutputStream;->write([B)V

    invoke-virtual {p1}, Ljava/io/OutputStream;->flush()V

    invoke-virtual {p1}, Ljava/io/OutputStream;->close()V

    iget-object p1, p0, Lf0/g;->a:Ljava/net/HttpURLConnection;

    invoke-virtual {p1}, Ljava/net/HttpURLConnection;->getResponseCode()I

    move-result p1

    iput p1, p0, Lf0/g;->b:I

    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

    const-string p2, "server responseCode-->"

    invoke-virtual {p1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget p2, p0, Lf0/g;->b:I

    invoke-static {p2}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object p2

    invoke-virtual {p1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v0, p1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    iget p1, p0, Lf0/g;->b:I

    const/16 p2, 0xc8

    if-ne p1, p2, :cond_0

    const-string p1, "register success!"

    invoke-static {v0, p1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_0

    :cond_0
    const-string p1, "register fail!"

    invoke-static {v0, p1}, Lf0/j;->b(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    const/4 p3, 0x0

    :goto_0
    invoke-virtual {p0}, Lf0/g;->a()V

    return p3

    :catchall_0
    move-exception p1

    invoke-virtual {p0}, Lf0/g;->a()V

    throw p1
.end method
