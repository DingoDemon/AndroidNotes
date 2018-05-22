MessageQueue维护了消息队列，但是那个队列摆在那里就摆在那里了，动也不动。我们需要什么？我们需要一个掏粪boy，去把Message掏出来丢给Handler处理。这个boy就是Looper。(这个类不关注native层的话，挺简单的)


# Looper

同样，我们来看下主要持有变量和构造方法。

   - ① static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();
   - ② private static Looper sMainLooper;  // guarded by Looper.class
   - ③ final MessageQueue mQueue;
   - ④ final Thread mThread;




1. ThreadLocal为每个线程保存了Looper，这个sThreadLocal在单进程下，是全局唯一的。
2. 主线程looper
3. Looper内消息队列 
4. Looper对应线程

然后我们发现构造方法是private的，并将内部线程和消息队列赋值：

```java
  private Looper(boolean quitAllowed) {
        mQueue = new MessageQueue(quitAllowed);
        mThread = Thread.currentThread();
    }
```

而什么时候构造的Looper对象呢？

```java
  public static void prepare() {
        prepare(true);
    }

    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }
```    

如果sThreadLocal.get有值的话，抛异常，一个线程只能初始化一次Looper。然后为这个线程保存这个Looper对象：

```
public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }
```

与之类似的是在ActivityThread 的main方法中调用的： 

```java  
public static void prepareMainLooper() {
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }
```

可以看到MainThread中的MessageQueue是不允许退出的。

接下来我们看Looper是如何掏粪的loop方法：

```java
    public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");//1
        }
        final MessageQueue queue = me.mQueue;

        // 忽略代码......

        for (;;) {
            Message msg = queue.next(); // might block//2
            if (msg == null) {//3
                return;
            }

        //忽略代码...

            try {
                msg.target.dispatchMessage(msg);//4
            } 
//忽略代码....
            msg.recycleUnchecked();//5
        }
    }
```

1. 用Handler 之前调用Looper.prepare(),之后调用loop，这是常识，这里解释了如果没有调用prepare()抛出异常的原因。
2. 无限循环找到msg；前文[《MessageQueue》](https://github.com/DingoDemon/AndroidNotes/blob/master/AndroidBasicKnowledgeNotes/MessageQueue.md)中讲到，真正的阻塞是发生在MessageQueue中的next方法中的nativePollOnce()处所以这里如果没有消息的话，会阻塞住
3. 这里为什么return了呢？没有消息就退出了么？并不是，( No message indicates that the message queue is quitting.)源码中注释告诉了我们没有消息表明消息队列正在退出。
4. msg(<-Message).target(<-Handler).dispatchMessage();msg持有的handler对象执行dispatchMessage(Message msg)方法。msg和handler互相持有对方引用
5. msg自身回收，将其加入message对象池。

所以一次正确的调用，应当是Looper类注释中的：

```java
    class LooperThread extends Thread {
        public Handler mHandler;
  
        public void run() {
            Looper.prepare();
  
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    // process incoming messages here
                }
            };
  
            Looper.loop();
        }
    }
```

退出也是调用的MessageQueue中的退出方法。

```java
 public void quitSafely() {
        mQueue.quit(true);
    }
```

```java
  public void quit() {
        mQueue.quit(false);
    }
```

quite会直接退出，quitSafely安全退出会设置一个退出标记，把消息队列中已有消息处理完毕后才会安全退出。Looper退出后，通过Handler发送消息的send方法会返回false