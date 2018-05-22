1. 客户端开始发送ClientHello报文开始SSL通信。报文中包含客户端支持的SSL的制定版本，加密组建列表。
2. 服务器返回serverhello报文，同理客户端。
3. 之后服务器发送certificate证书，其中包括公钥证书。
4. 服务器发送serverhellodone, 通知客户端最初阶段ssl握手结束。
5. 第一次握手结束后，客户端以clientKeyExchange报文作为回应。其中包含一串pre-master-secret随机密码串，并由步骤3中公钥加密。
6. 客户端继续发送ChangeCipher['saɪfɚ]Spec报文。表示此报文以后会用pre-master-secret密钥加密
7. 客户端发送finish报文。该报文包含连接至今全部报文的校验值。这次握手是否成功，要以服务端是否能够正确解密为准。
8. 服务端同样发送ChangeCipherSpec报文。
9. 服务端返回Finish报文。

然后开始http请求