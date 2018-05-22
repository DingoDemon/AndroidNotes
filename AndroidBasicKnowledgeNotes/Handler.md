队列有了，薅消息的Looper也有了，现在还差什么？你去路边问路人，肯定回答你还“差个发送者和接受处理者啊”。这里发送接收的人，都是Handler。因为Handler这个人挺讲规矩，谁污染谁处理。一般来说，哪个Handler发的，就由哪个Handler来擦屁股。


我们先来看Handler持有的对象，稍后再看构造方法：

- final Looper mLooper;
- final MessageQueue mQueue;
- final Callback mCallback;
- final boolean mAsynchronous;
- IMessenger mMessenger;

Looper对象，MessageQueue对象，Callback回调，mAsynchronous是否异步Handler这些都比较简单。IMessenger是进程间通信里用到的，我们暂时忽略(这里主要讨论的是线程间通信)

这里为什么说稍后再看构造方法呢？因为Handler的构造方法，有7个之多：

- ① public Handler()
- ② public Handler(Callback callback)
- ③ public Handler(Looper looper)
- ④ public Handler(Looper looper, Callback callback)
- ⑤ public Handler(boolean async)
- ⑥ public Handler(Callback callback, boolean async)
- ⑦ public Handler(Looper looper, Callback callback, boolean async)

这7个构造方法，可以分为两类，一类是指定了Looper，一类是没有制定：


我们先来看没有制定的，分别是*1*，*2*，*5*，*6*。

*1*，*2*，*5*最终都调到了**6**:

```java
   public Handler(Callback callback, boolean async) {
      //忽略代码...

        mLooper = Looper.myLooper();
        if (mLooper == null) {
            throw new RuntimeException(
                "Can't create handler inside thread that has not called Looper.prepare()");
        }
        mQueue = mLooper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }
```    

而*3*，*4*则最终调用的**7**：

```java
    public Handler(Looper looper, Callback callback, boolean async) {
        mLooper = looper;
        mQueue = looper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }
```    

两者区别就是是否制定Looper，如果未指定的话，就是当前线程的Looper。


我们来看这个Callback，顾名思义,这个应该是个回调。这个Callback要和msg.callback区分开来。
我们知道looper找到一条消息后，会 msg.target.dispatchMessage(msg)。我们来看dispatchMessage方法：

```java
    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {//1
            handleCallback(msg);//2
        } else {
            if (mCallback != null) {//3
                if (mCallback.handleMessage(msg)) {//4
                    return;
                }
            }
            handleMessage(msg);//5
        }
    }
```

#### 1.

 msg可能会携带一个callback的Runnable，比如哈，我们使handler.post(Runnable):

```java
  public final boolean post(Runnable r)
    {
       return  sendMessageDelayed(getPostMessage(r), 0);
    }
```   

```java 
 private static Message getPostMessage(Runnable r) {
        Message m = Message.obtain();
        m.callback = r;
        return m;
    }
```

这里发现将post的Runnable封装到Message内，并且加入队列。

#### 2.
  执行msg中的runnable
private static void handleCallback(Message message) {
        message.callback.run();
    }

#### 3.
  检测Handler的mCallback是否为空，不为空的话就交由它处理
#### 4.
  判断mCallback.handleMessage(msg) ，true的话表示处理完毕。return
#### 5.
  交由Handler的handleMessage方法处理(就是我们重写的那个方法)


![1](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/Handler_1.png)





讲着讲着就把处理msg的事情讲的差不多了,举个例子：


![2](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/Handler_2.png)

接下来是发送消息的方法，我将其分为两类，一类是send，一类是post：



1. boolean sendMessage(Message msg)
2. boolean sendEmptyMessage(int what)
3. boolean sendEmptyMessageDelayed(int what, long delayMillis)
4. boolean sendEmptyMessageAtTime(int what, long uptimeMillis)
5. boolean sendMessageDelayed(Message msg, long delayMillis)
6. boolean sendMessageAtTime(Message msg, long uptimeMillis)
7. boolean sendMessageAtFrontOfQueue(Message msg)

---

1. boolean post(Runnable r)
2. boolean postAtTime(Runnable r, long uptimeMillis)
3. boolean postAtTime(Runnable r, Object token, long uptimeMillis)
4. boolean postDelayed(Runnable r, long delayMillis)
5. boolean postAtFrontOfQueue(Runnable r)
 
//为什么要预制这么多方法，就是希望开发者能正确控制Message的when


### send：
sendMessage调用的是sendMessageDelayed(msg, 0)，Delay都懂吧，就是延迟。然后继续调用的是
sendMessageAtTime(msg, SystemClock.uptimeMillis() + delayMillis)，AtTime都懂吧，不懂别搞开发了，先去初中学英语，然后内部继续调用的是
enqueueMessage(queue, msg, uptimeMillis)：

```java
  private boolean enqueueMessage(MessageQueue queue, Message msg, long uptimeMillis) {
        msg.target = this;
        if (mAsynchronous) {
            msg.setAsynchronous(true);
        }
        return queue.enqueueMessage(msg, uptimeMillis);
    }
```

将msg的target指向自身(谁污染谁治理)，然后调用Message的enqueueMessage(Message msg, long when)方法。有一个不同的就是sendMessageAtFrontOfQueue方法，顾名思义，直接插队到最前面。

一通百通，post一样的。这里就不具体分析了。上文也有讲过将post封装成Message的事。
入队的顺序是根据uptimeMillis来决定的，也就是说，消息队列是根据时间顺序来排列的，我们可以使用AtTime或者AtFrontOfQueue来插队：





![3](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/Handler_3.png)

![4](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/Handler_4.png)


