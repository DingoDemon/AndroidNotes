> _篇幅较长：涉及到的计算机网络的知识也不少。比起前面的内容。个人感觉难度陡增(主要是我比较菜)，理解编写花费了不少时间，其中https，路由，代理部分写的不多，今后有机会再补充，这里主要还是关心主要流程_

通常，由HTTP客户端发起一个请求，创建一个到服务器指定端口（默认是80端口）的TCP连接。TCP建立连接需要3次握手，然后才开始传输数据，然后又要三次挥手，断开连接。
如果客户端频繁请求的话，会耗费大量资源在握手和挥手上。无限创建连接会导致性能地下。

在http 1.1 可以使用字段connection 设置成keep-alive来保持长连接。在OKhttp中，采用复用连接池来节省资源，复用连接。



- StreamAllocation(流の分配器，协调Connections，Streams，Calls这三者之间的关系)
- ConnectionPool(连接池)
- RealConnection(一个抽象的连接)

---

# ConnectionPool
我们先来分析ConnectionPool看下他所持有的变量：

1. private static final Executor executor = new ThreadPoolExecutor(0 /* corePoolSize */,
 Integer.MAX_VALUE /* maximumPoolSize */, 60L /* keepAliveTime */, TimeUnit.SECONDS,
 new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp ConnectionPool", true));//1

2. private final int maxIdleConnections;
3. private final long keepAliveDurationNs;

4. private final Runnable cleanupRunnable {....}

5. private final Deque<RealConnection> connections = new ArrayDeque<>();
6. final RouteDatabase routeDatabase = new RouteDatabase();
7. boolean cleanupRunning;

>- 1.用于清理过期connection的executor
>- 2.每个address的最大空闲连接数
>- 3.最大存活时间
>- 4.清理线程池任务
>- 5.连接存放双端队列
>- 6.失败路由黑名单
>- 7.是否执行清理任务标示符。
>
> 值得一提的是，我们可以在构造OkHttpClient的时候传入自定义连接池。
public ConnectionPool(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit)
> 来控制最大连接个数和保活时间。

作为存放connection的容器，最重要的肯定是存和取两个方法。我们来具体分析：
### get：

```java
 @Nullable RealConnection get(Address address, StreamAllocation streamAllocation, Route route) {
    assert (Thread.holdsLock(this));//1
    for (RealConnection connection : connections) {
      if (connection.isEligible(address, route)) {//2
        streamAllocation.acquire(connection, true);//3
        return connection;
      }
    }
    return null;
  }
```

*请注意这里的的取，取出来的是可重用的RealConnection:*

1. 断言，确保线程未被锁住。
然后开始遍历队列
2. 根据地址和路由，来判断connection是否可重用
3. 复用连接返回(connection.allocations+1)，并将StreamAllocation中的connection赋值成此connection(请记住这段！很重要！后面会用到！)

### put:

```java
 void put(RealConnection connection) {
    assert (Thread.holdsLock(this));
    if (!cleanupRunning) {
      cleanupRunning = true;
      executor.execute(cleanupRunnable);
    }
    connections.add(connection);
  }
```
异步触发清理任务，然后将连接加入队列中。

这里我们来关注清理任务:

```java
 private final Runnable cleanupRunnable = new Runnable() {
    @Override public void run() {
      while (true) {
        long waitNanos = cleanup(System.nanoTime());
        if (waitNanos == -1) return;
        if (waitNanos > 0) {
          long waitMillis = waitNanos / 1000000L;
          waitNanos -= (waitMillis * 1000000L);
          synchronized (ConnectionPool.this) {
            try {
              ConnectionPool.this.wait(waitMillis, (int) waitNanos);
            } catch (InterruptedException ignored) {
            }
          }
        }
      }
    }
  };
```
由cleanup方法返回等待之间，为-1的时候return，否则等待waitNanos时间后，再次清理。cleanup方法就很关键了：

```java
  long cleanup(long now) {
    int inUseConnectionCount = 0;//在用连接
    int idleConnectionCount = 0;//空闲连接
    RealConnection longestIdleConnection = null;
    long longestIdleDurationNs = Long.MIN_VALUE;
    // Find either a connection to evict, or the time that the next eviction is due.
    synchronized (this) {
      for (Iterator<RealConnection> i = connections.iterator(); i.hasNext(); ) {
        RealConnection connection = i.next();
        // If the connection is in use, keep searching.
        if (pruneAndGetAllocationCount(connection, now) > 0) {//1
          inUseConnectionCount++;
          continue;
        }
        idleConnectionCount++;
        // If the connection is ready to be evicted, we're done.
        long idleDurationNs = now - connection.idleAtNanos;
        if (idleDurationNs > longestIdleDurationNs) {
//找出空闲时间最长的连接以及对应的空闲时间
          longestIdleDurationNs = idleDurationNs;
          longestIdleConnection = connection;
        }
      }
      if (longestIdleDurationNs >= this.keepAliveDurationNs
          || idleConnectionCount > this.maxIdleConnections) {
        // We've found a connection to evict. Remove it from the list, then close it below (outside
        // of the synchronized block).
 //在符合清理条件下，清理空闲时间最长的连接
        connections.remove(longestIdleConnection);
      } else if (idleConnectionCount > 0) {
        // A connection will be ready to evict soon.
        //不符合清理条件，则返回下次需要执行清理的等待时间，也就是此连接即将到期的时间
        return keepAliveDurationNs - longestIdleDurationNs;
      } else if (inUseConnectionCount > 0) {
       //没有空闲的连接，则隔keepAliveDuration之后再次执行
        // All connections are in use. It'll be at least the keep alive duration 'til we run again.
       //清理结束
        return keepAliveDurationNs;
      } else {
        // No connections, idle or in use.
        cleanupRunning = false;
        return -1;
      }
    }
//关闭socket
    closeQuietly(longestIdleConnection.socket());
    // Cleanup again immediately.
    return 0;
  }
```



我们接下来分析pruneAndGetAllocationCount方法：

```java 
 private int pruneAndGetAllocationCount(RealConnection connection, long now) {
    List<Reference<StreamAllocation>> references = connection.allocations;
    for (int i = 0; i < references.size(); ) {
      Reference<StreamAllocation> reference = references.get(i);
      if (reference.get() != null) {
//若StreamAllocation被使用则接着循环
        i++;
        continue;
      }
//若StreamAllocation为空则移除引用，这边注释为泄露
      // We've discovered a leaked allocation. This is an application bug.
      StreamAllocation.StreamAllocationReference streamAllocRef =
          (StreamAllocation.StreamAllocationReference) reference;
      String message = "A connection to " + connection.route().address().url()
          + " was leaked. Did you forget to close a response body?";
      Platform.get().logCloseableLeak(message, streamAllocRef.callStackTrace);

      references.remove(i);
      connection.noNewStreams = true;
//如果列表为空则说明此连接没有被引用了，则返回0，表示此连接是空闲连接
      // If this was the last allocation, the connection is eligible for immediate eviction.
      if (references.isEmpty()) {
        connection.idleAtNanos = now - keepAliveDurationNs;
        return 0;
      }
    }
    return references.size();
  }
```
** 这个方法内返回的connection.allocations,不为0的话证明connection正在使用。
下文会有[说明](####关于allocations)**


还有一些其他方法:

*deduplicate(Address address, StreamAllocation streamAllocation):该方法主要是针对HTTP/2场景下多个多路复用连接清除的场景*

*connectionBecameIdle:(RealConnection connection)标示一个连接处于空闲状态，即没有流任务，那么久需要调用该方法，由ConnectionPool来决定是否需要清理该连接。*

*evictAll() 关闭所有空闲连接。*

这里不做具体分析了。

--------

# RealConnection：

同样，我们先来分析ConnectionPool看下他所持有的变量：


1. private static final String NPE_THROW_WITH_NULL = "throw with null exception";
2. private static final int MAX_TUNNEL_ATTEMPTS = 21; //最大隧道个数
3. private final ConnectionPool connectionPool;//连接池
4. private final Route route;//路由
5. private Socket rawSocket;//底层socket
6. private Socket socket;//应用层socket，SSLSocket或者没用SSL的连接的socket(rawsocket本身)
7. private Handshake handshake;//TLS握手集合
8. private Protocol protocol;//协议，HTTP_1_0，HTTP_1_1，HTTP_2等
9. private Http2Connection http2Connection;//http2的connection
10. private BufferedSource source;//服务器交互输入输出流
11. private BufferedSink sink;
12. public boolean noNewStreams;//标记是否不在有新流
13. public int successCount;//成功次数
14. public int allocationLimit = 1;一个连接中可承载的流的最大个数，
15. <span id = "jump">public final List<Reference<StreamAllocation>> allocations = new ArrayList<>();//StreamAllocation的弱引用list</span>
16. public long idleAtNanos = Long.MAX_VALUE;记录allocations.size()=0时的时间戳

#### 关于allocations
记数[增加](#####allocations增加)和[减少](#####allocations减少)是在StreamAllocation类中处理事件的，下文会降到。
> 

```java
public RealConnection(ConnectionPool connectionPool, Route route) {
    this.connectionPool = connectionPool;
    this.route = route;
  }
 
```

结合下面我们马上分析connect方法，我们可以得到以下结论：

- *发现RealConnection的绝大部分字段都是在connect中赋值的，并且不会再次赋值*
- *在非Http2的情况下，一个connection中可存在的最大流数为1*


```java
  public void connect(int connectTimeout, int readTimeout, int writeTimeout,
      int pingIntervalMillis, boolean connectionRetryEnabled, Call call,
      EventListener eventListener) {
    if (protocol != null) throw new IllegalStateException("already connected");//1
    RouteException routeException = null;
    List<ConnectionSpec> connectionSpecs = route.address().connectionSpecs();//2
    ConnectionSpecSelector connectionSpecSelector = new ConnectionSpecSelector(connectionSpecs);//3
    if (route.address().sslSocketFactory() == null) {//4
      if (!connectionSpecs.contains(ConnectionSpec.CLEARTEXT)) {
        throw new RouteException(new UnknownServiceException(
            "CLEARTEXT communication not enabled for client"));
      }
      String host = route.address().url().host();
      if (!Platform.get().isCleartextTrafficPermitted(host)) { //5
        throw new RouteException(new UnknownServiceException(
            "CLEARTEXT communication to " + host + " not permitted by network security policy"));
      }
    }
    while (true) {
      try {
        if (route.requiresTunnel()) {//6
          connectTunnel(connectTimeout, readTimeout, writeTimeout, call, eventListener);//建立隧道
          if (rawSocket == null) {
            // We were unable to connect the tunnel but properly closed down our resources.
            break;
          }
        } else {
          connectSocket(connectTimeout, readTimeout, call, eventListener);//建立连接
        }
        establishProtocol(connectionSpecSelector, pingIntervalMillis, call, eventListener);//建立协议
        eventListener.connectEnd(call, route.socketAddress(), route.proxy(), protocol);
        break;
      } catch (IOException e) {
      //忽略代码...
    }
    if (route.requiresTunnel() && rawSocket == null) {
      ProtocolException exception = new ProtocolException("Too many tunnel connections attempted: "
          + MAX_TUNNEL_ATTEMPTS);
      throw new RouteException(exception);
    }
    if (http2Connection != null) {
      synchronized (connectionPool) {
        allocationLimit = http2Connection.maxConcurrentStreams();
      }
    }
  }

```  

1. 通过protocol来判断连接是否建立，已建立就抛异常
2. 连接策略，内含MODERN_TLS(是连接到最新的HTTPS服务器的安全配置),COMPATIBEL_TLS(是连接到过时的HTTPS服务器的安全配置),CLEARTEXT(是用于http://开头的URL的非安全配置)三种策略。
3. ConnectionSpecSelector,用以从ConnectionSpec集合中选择与SSLSocket匹配的ConnectionSpec，并对SSLSocket做配置的操作
4. 如果连接是https但连接策略中不包含ConnectionSpec.CLEARTEXT则会抛异常
5. 判断平台本身的安全策略允向相应的主机发送明文请求。对于Android平台而言，这种安全策略主要由系统的组件android.security.NetworkSecurityPolicy执行。平台的这种安全策略不是每个Android版本都有的。Android6.0之后存在这种控制。
6. address.sslSocketFactory != null && proxy.type() == Proxy.Type.HTTP;代理为http且请求为https的话，建立https ssl隧道。



我们接下来分析中间最重要的三个动作，建立隧道，建立连接，建立协议：

### 建立隧道

> “为了使https与代理配合工作，要进行几处修改后以告知代理连接到何处。一种常用的技术就是HTTPS SSL隧道技术。使用HTTPS隧道协议，客户端首先要告知代理，它想要连接的安全主机和端口，这是开始加密之前，以明文形式告知的，所以代理可以理解这条信息” -《HTTP权威指南》
> 

![http_tunnel](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_tunnel.png)   
                                              

```java
  private void connectTunnel(int connectTimeout, int readTimeout, int writeTimeout, Call call,
      EventListener eventListener) throws IOException {
    Request tunnelRequest = createTunnelRequest();//1
    HttpUrl url = tunnelRequest.url();
    for (int i = 0; i < MAX_TUNNEL_ATTEMPTS; i++) {
      connectSocket(connectTimeout, readTimeout, call, eventListener);//2
      tunnelRequest = createTunnel(readTimeout, writeTimeout, tunnelRequest, url);//3
      if (tunnelRequest == null) break; // Tunnel successfully created.//4
      // The proxy decided to close the connection after an auth challenge. We need to create a new
      // connection, but this time with the auth credentials.
      closeQuietly(rawSocket);
      rawSocket = null;
      sink = null;
      source = null;
      eventListener.connectEnd(call, route.socketAddress(), route.proxy(), null);
    }
```

1. new Request.Builder()
        .url(route.address().url())
        .header("Host", Util.hostHeader(route.address().url(), true))
        .header("Proxy-Connection", "Keep-Alive") // For HTTP/1.0 proxies like Squid.
        .header("User-Agent", Version.userAgent())
        .build();
2. 连接socket
3. 建立tunnel
4. 成功建立

其中第三步建立隧道代码如下：


```java
 private Request createTunnel(int readTimeout, int writeTimeout, Request tunnelRequest,
      HttpUrl url) throws IOException {
    // 拼接CONNECT命令
    String requestLine = "CONNECT " + Util.hostHeader(url, true) + " HTTP/1.1";
    while (true) {
//对应http/1.1 编码HTTP请求并解码HTTP响应
      Http1Codec tunnelConnection = new Http1Codec(null, null, source, sink);
      
//忽略代码.......

//发送CONNECT，请求打开隧道连接
      tunnelConnection.writeRequest(tunnelRequest.headers(), requestLine);
       //完成连接
      tunnelConnection.finishRequest();
      Response response = tunnelConnection.readResponseHeaders(false)
          .request(tunnelRequest)
          .build();
          
//忽略代码.......

      switch (response.code()) {
        case HTTP_OK://成功
     //忽略代码.......
          return null;
        case HTTP_PROXY_AUTH://表示服务器要求对客户端提供访问证书，进行代理认证
//进行代理认证
          tunnelRequest = route.address().proxyAuthenticator().authenticate(route, response);
//代理认证不通过
          if (tunnelRequest == null) throw new IOException("Failed to authenticate with proxy");
//代理认证通过，但是响应要求close，则关闭TCP连接此时客户端无法再此连接上发送数据
          if ("close".equalsIgnoreCase(response.header("Connection"))) {
            return tunnelRequest;
          }
          break;
        default:
          throw new IOException(
              "Unexpected response code for CONNECT: " + response.code());
      }
    }
```

### 建立连接：


```java
private void connectSocket(int connectTimeout, int readTimeout, Call call,EventListener eventListener) throws IOException {
    Proxy proxy = route.proxy();
    Address address = route.address();
    rawSocket = proxy.type() == Proxy.Type.DIRECT || proxy.type() == Proxy.Type.HTTP
        ? address.socketFactory().createSocket()
        : new Socket(proxy);//1

    eventListener.connectStart(call, route.socketAddress(), proxy);
    rawSocket.setSoTimeout(readTimeout);
    try {
      Platform.get().connectSocket(rawSocket, route.socketAddress(), connectTimeout);//2
    } catch (ConnectException e) {
      ConnectException ce = new ConnectException("Failed to connect to " + route.socketAddress());
      ce.initCause(e);
      throw ce;
    }
    try {
      source = Okio.buffer(Okio.source(rawSocket));//3
      sink = Okio.buffer(Okio.sink(rawSocket));
    } catch (NullPointerException npe) {
      if (NPE_THROW_WITH_NULL.equals(npe.getMessage())) {
        throw new IOException(npe);
      }
    }
  }
```

1. 根据代理类型来选择socket类型，是代理还是直连,如果是http或者直连的话，则通过Address的socketFactory对象来创建一个Socket。如果是SOCKS的话，则通过代理来new一个socket
2. 打开socket连接
3. 对输入和输出做处理 

### 建立协议

```java
private void establishProtocol(ConnectionSpecSelector connectionSpecSelector,int pingIntervalMillis, Call call, EventListener eventListener) throws IOException {
    if (route.address().sslSocketFactory() == null) {//1
      protocol = Protocol.HTTP_1_1;
      socket = rawSocket;
      return;
    }
    eventListener.secureConnectStart(call);
    connectTls(connectionSpecSelector);//2
    eventListener.secureConnectEnd(call, handshake);
    if (protocol == Protocol.HTTP_2) {//3
      socket.setSoTimeout(0); // HTTP/2 connection timeouts are set per-stream.
      http2Connection = new Http2Connection.Builder(true)
          .socket(socket, route.address().url().host(), source, sink)
          .listener(this)
          .pingIntervalMillis(pingIntervalMillis)
          .build();
      http2Connection.start();
    }
  }
```

1. 没有设置代理的情况下，直接与HTTP服务器建立TCP连接，然后进行HTTP请求/响应的交互。
2. 如果是ssl，创建tls连接(具体不做详细分析。以后针对于https详细写一篇)
3. 如果是http2的话，构造一个Http2Connection，执行start()和服务器建立协议。

其他还有两个重要方法：


*一个是isEligible(Address address, @Nullable Route route)；
这个方法主要是判断面对给出的addres和route，这个realConnetion是否可以重用。*


*一个是isHealthy(boolean doExtensiveChecks)
doExtensiveChecks表示是否需要额外的检查。这里主要是检查，判断这个连接是否是健康的连接//Returns true if this connection is ready to host new streams*

以及
public HttpCodec newCodec(OkHttpClient client, Interceptor.Chain chain,
      StreamAllocation streamAllocation) 
里面主要是判断是否是HTTP/2,如果是HTTP/2则new一个Http2Codec。如果不是HTTP/2则new一个Http1Codec。

----------
# StreamAllocation


Http请求需要在连接上建立一个新的流，我将StreamAllocation称之为流の分配器，它负责为一次"请求"寻找"连接"并建立"流"，从而完成远程通信。所以说StreamAllocation与"请求"、"连接"、"流"都有关。
Connection是建立在socket上的物流通信信道，stream代表逻辑的流，而call是对请求的封装。
作为协调这三者关系的类，它的主要作用是给请求(call)寻找合适的连接(connection)，并且为他开辟一个流(HttpCodec)。

![大概流程](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/stram_allocation.png)

同样，我们先看它所持有的变量和构造方法：

1. public final Address address;//地址
2. private RouteSelector.Selection routeSelection;
3. private Route route;//路由
4. private final ConnectionPool connectionPool;//连接池
5. public final Call call;//请求
6. public final EventListener eventListener;
7. private final Object callStackTrace;
8. private final RouteSelector routeSelector;//路由选择器
9. private int refusedStreamCount;//拒绝次数
10. private RealConnection connection;//连接
11. private boolean reportedAcquired;
12. private boolean released;//释放标识
13. private boolean canceled;//取消标识
14. private HttpCodec codec;//流


>

```java
public StreamAllocation(ConnectionPool connectionPool, Address address, Call call,
      EventListener eventListener, Object callStackTrace) {
    this.connectionPool = connectionPool;
    this.address = address;
    this.call = call;
    this.eventListener = eventListener;
    this.routeSelector = new RouteSelector(address, routeDatabase(), call, eventListener);
    this.callStackTrace = callStackTrace;
  }
```

我们来关注它的重要方法：

```java
 public HttpCodec newStream(
      OkHttpClient client, Interceptor.Chain chain, boolean doExtensiveHealthChecks) {
 //忽略代码 ....
      RealConnection resultConnection = findHealthyConnection(connectTimeout, readTimeout,
          writeTimeout, pingIntervalMillis, connectionRetryEnabled, doExtensiveHealthChecks);//1
      HttpCodec resultCodec = resultConnection.newCodec(client, chain, this);//2
//忽略代码 ....
  }
```

很明显，1获取连接2打开流，接下来我们来看下到底是如何获取连接的：

```java
  private RealConnection findHealthyConnection(int connectTimeout, int readTimeout,
      int writeTimeout, int pingIntervalMillis, boolean connectionRetryEnabled,
      boolean doExtensiveHealthChecks) throws IOException {
    while (true) {
      RealConnection candidate = findConnection(connectTimeout, readTimeout, writeTimeout,
          pingIntervalMillis, connectionRetryEnabled);
      // If this is a brand new connection, we can skip the extensive health checks.
      synchronized (connectionPool) {
        if (candidate.successCount == 0) {
          return candidate;
        }
      }
      // Do a (potentially slow) check to confirm that the pooled connection is still good. If it
      // isn't, take it out of the pool and start again.
      if (!candidate.isHealthy(doExtensiveHealthChecks)) {
        noNewStreams();
        continue;
      }
      return candidate;
    }
  }
```  

在死循环中寻找connection，如果他successCount为0的话，就跳过健康检查。检查出来不健康则跳过寻找下一个connection。我们来看findConnection方法：


```java
private RealConnection findConnection(int connectTimeout, int readTimeout, int writeTimeout,
      boolean connectionRetryEnabled) throws IOException {
    RealConnection result = null;
    Route selectedRoute = null;
    Connection releasedConnection;
    Socket toClose;

      //忽略异常代码....


-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*

      //如果有已知连接且可用，则直接返回
  releasedConnection = this.connection;
      toClose = releaseIfNoNewStreams();
      if (this.connection != null) {
        // We had an already-allocated connection and it's good.
        result = this.connection;
        releasedConnection = null;
      }
      if (!reportedAcquired) {
        // If the connection was never reported acquired, don't report it as released!
        releasedConnection = null;
      }
-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*

      //如果在连接池有对应address的连接，则返回
  if (result == null) {
        // Attempt to get a connection from the pool.
        Internal.instance.get(connectionPool, address, this, null);
        if (connection != null) {
          foundPooledConnection = true;
          result = connection;
        } else {
          selectedRoute = route;
        }
      }
    }
    closeQuietly(toClose);
    if (releasedConnection != null) {
      eventListener.connectionReleased(call, releasedConnection);
    }
    if (foundPooledConnection) {
      eventListener.connectionAcquired(call, result);
    }
    if (result != null) {
      // If we found an already-allocated or pooled connection, we're done.
      return result;
    }
    
-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*    
    
    // 切换路由再在连接池里面找下，如果有则返回
   // If we need a route selection, make one. This is a blocking operation.
    boolean newRouteSelection = false;
    if (selectedRoute == null && (routeSelection == null || !routeSelection.hasNext())) {
      newRouteSelection = true;
      routeSelection = routeSelector.next();
    }
    synchronized (connectionPool) {
      if (canceled) throw new IOException("Canceled");
      if (newRouteSelection) {
        // Now that we have a set of IP addresses, make another attempt at getting a connection from
        // the pool. This could match due to connection coalescing.
        List<Route> routes = routeSelection.getAll();
        for (int i = 0, size = routes.size(); i < size; i++) {
          Route route = routes.get(i);
          Internal.instance.get(connectionPool, address, this, route);
          if (connection != null) {
            foundPooledConnection = true;
            result = connection;
            this.route = route;
            break;
          }
        }
      }
      if (!foundPooledConnection) {
        if (selectedRoute == null) {
          selectedRoute = routeSelection.next();
        }
        // Create a connection and assign it to this allocation immediately. This makes it possible
        // for an asynchronous cancel() to interrupt the handshake we're about to do.
        route = selectedRoute;
        refusedStreamCount = 0;
        result = new RealConnection(connectionPool, selectedRoute);
        acquire(result, false);
      }
    }
    // If we found a pooled connection on the 2nd time around, we're done.
    if (foundPooledConnection) {
      eventListener.connectionAcquired(call, result);
      return result;
    }
    
-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*    
    
    // 开始连接
 // Do TCP + TLS handshakes. This is a blocking operation.
    result.connect(connectTimeout, readTimeout, writeTimeout, pingIntervalMillis,
        connectionRetryEnabled, call, eventListener);
    routeDatabase().connected(result.route());
    Socket socket = null;
    synchronized (connectionPool) {
      reportedAcquired = true;
      // Pool the connection.
      Internal.instance.put(connectionPool, result);
      // If another multiplexed connection to the same address was created concurrently, then
      // release this connection and acquire that one.
      if (result.isMultiplexed()) {
        socket = Internal.instance.deduplicate(connectionPool, address, this);
        result = connection;
      }
    }
    closeQuietly(socket);
    eventListener.connectionAcquired(call, result);
    return result;
  }
```


1. 先找是否有已经存在的连接，如果有已经存在的连接，并且可以使用(!noNewStreams)则直接返回。
2. 根据已知的address在connectionPool里面找，如果有连接，则返回
3. 更换路由，更换线路，在connectionPool里面再次查找，如果有则返回。
4. 如果以上条件都不满足则直接new一个RealConnection出来
5. new出来的RealConnection通过acquire关联到connection.allocations上
6. 做去重判断，如果有重复的socket则关闭


#### allocations增加

```java
  public void acquire(RealConnection connection, boolean reportedAcquired) {
    assert (Thread.holdsLock(connectionPool));
    if (this.connection != null) throw new IllegalStateException();

    this.connection = connection;
    this.reportedAcquired = reportedAcquired;
    connection.allocations.add(new StreamAllocationReference(this, callStackTrace));
  }

```

这里相当于给connection的引用计数器加1,会在ConnectionPool.get和StreamAllocation.findConnection的时候调用

有打开流，就有关闭流，我们来看看：

```java
public void streamFinished(boolean noNewStreams, HttpCodec codec, long bytesRead, IOException e) {
    eventListener.responseBodyEnd(call, bytesRead);
    Socket socket;
    Connection releasedConnection;
    boolean callEnd;
    synchronized (connectionPool) {
      if (codec == null || codec != this.codec) {
        throw new IllegalStateException("expected " + this.codec + " but was " + codec);
      }
      if (!noNewStreams) {
        connection.successCount++;//成功次数+1
      }
      releasedConnection = connection;
      socket = deallocate(noNewStreams, false, true);
      if (connection != null) releasedConnection = null;
      callEnd = this.released;
    }
    closeQuietly(socket);
//忽略代码.....
  }
  
```

```java
 private Socket deallocate(boolean noNewStreams, boolean released, boolean streamFinished) {
    assert (Thread.holdsLock(connectionPool));
    if (streamFinished) {
      this.codec = null;
    }
    if (released) {
      this.released = true;
    }
    Socket socket = null;
    if (connection != null) {
      if (noNewStreams) {
        connection.noNewStreams = true;
      }
      if (this.codec == null && (this.released || connection.noNewStreams)) {
        release(connection);
        if (connection.allocations.isEmpty()) {
          connection.idleAtNanos = System.nanoTime();
          if (Internal.instance.connectionBecameIdle(connectionPool, connection)) {
            socket = connection.socket();
          }
        }
        connection = null;
      }
    }
    return socket;
  }
```

#### allocations减少

```java
  private void release(RealConnection connection) {
    for (int i = 0, size = connection.allocations.size(); i < size; i++) {
      Reference<StreamAllocation> reference = connection.allocations.get(i);
      if (reference.get() == this) {
        connection.allocations.remove(i);
        return;
      }
    }
    throw new IllegalStateException();
  }
```


- deallocate(boolean, boolean, boolean)方法根据传入的三个布尔类型的值进行操作，如果streamFinished为true则代表关闭流，this.codec == null && (this.released || connection.noNewStreams)则release(RealConnection)。并且如果connection.allocations.isEmpty()则通知连接池connectionPool把这个connection设置空闲连接。如果可以设为空闲连接则返回这个socket。不能则返回null。

- release(RealConnection)方法比较简单，主要是把RealConnection对应的allocations清除掉，把计数器归零。

- noNewStreams()方法，主要是设置防止别人在这个连接上开新的流

```java
public void noNewStreams() { 
Socket socket; 
synchronized (connectionPool) { socket = deallocate(true, false, false); } 
closeQuietly(socket); }

```









