安卓的消息机制主要涉及Handler，Looper，MessageQueue，Message；四个类。下面会分篇幅详细讲解。

*参考资料：https://www.jianshu.com/p/e440d3ebc470*

在ActivityThread的main()方法中，调用了 Looper.prepareMainLooper(); Looper.loop();两个静态方法来创建主线程的Looper(循环)和MessageQueue(消息队列)。我们先来分析MessageQueue。



# MessageQueue：

 __Low-level class holding the list of messages to be dispatched by a
  Looper. Messages are not added directly to a MessageQueue,
  but rather through Handler objects associated with the Looper. 
 You can retrieve the MessageQueue for the current thread with
 Looper.myQueue()__

用我腊鸡英语翻译翻译过来就是一个低级别的持有消息列表的类，消息不会直接插入到MessageQueue中，而是通过Looper和Handler联系起来，可以通过当前线程的Looper.myQueue()方法来获取。

Handler将Message发送到消息队列中，消息队列会按照一定的规则取出要执行的Message，我们来看下他的构造方法和主要变量：



- ① private final boolean mQuitAllowed;

- ② private long mPtr; // used by native code

- ③ Message mMessages;

- ④ private final ArrayList<IdleHandler> mIdleHandlers = new ArrayList<IdleHandler>();

- ⑤ private SparseArray<FileDescriptorRecord> mFileDescriptorRecords;
  
- ⑥ private IdleHandler[] mPendingIdleHandlers;

- ⑦ private boolean mQuitting;

    // Indicates whether next() is blocked waiting in pollOnce() with a non-zero timeout.
- ⑧ private boolean mBlocked;

    // The next barrier token.
    // Barriers are indicated by messages with a null target whose arg1 field carries the token.
- ⑨ private int mNextBarrierToken;


---

1. 是否允许退出，主线程的消息队列不允许退出。
2. 该变量用于保存native代码中的MessageQueue的指针，可暂时忽略
3. 在MessageQueue中，所有的Message是以链表的形式组织在一起的，该变量保存了链表的第一个元素，也可以说它就是链表的本身
4. 当Handler线程处于空闲状态的时候(MessageQueue没有其他Message时)，可以利用它来处理一些事物，该变量就是用于保存这些空闲时候要处理的事务
5. 暂时忽略
6. 用于保存将要被执行的IdleHandler
7. 标示MessageQueue是否正在关闭。
8. 标示 MessageQueue是否阻塞
9. 在MessageQueue里面有一个概念叫做障栅，它用于拦截同步的Message，阻止这些消息被执行只有异步Message才会放行。障栅本身也是一个Message，只是它的target为null并且arg1用于区分不同的障栅，所以该变量就是用于不断累加生成不同的障栅。

构造方法：

```java
 MessageQueue(boolean quitAllowed) {
        mQuitAllowed = quitAllowed;
        mPtr = nativeInit();
    }
```    

就一个是否允许退出，并且在native层执行init();mPtr为NativeMessageQueue的指针。

我们来看一下ArrayList<IdleHandler> mIdleHandlers里的IdleHandler:

```java
    public static interface IdleHandler {
        boolean queueIdle();
    }
```

我们来看IdleHandler，这个东西是干什么的呢，原文注释为:*Callback interface for discovering when a thread is going to block waiting for more messages*,同样用我的腊鸡英语翻译的话，就是“当线程阻塞等待更多消息时的回调接口”。这个queueIdle()方法是干什么的呢。*“Called when the message queue has run out of messages and will now wait for more.  Return true to keep your idle handler active, false to have it removed.  This may be called if there are still messages pending in the queue, but they are all scheduled to be dispatched after the current time.”* 长求总by my LaJi English，就是消息队列消耗完消息时调用，False的话会执行完后将其remove。

与之对应的是：

```java
   public void addIdleHandler(@NonNull IdleHandler handler) {
        if (handler == null) {
            throw new NullPointerException("Can't add a null IdleHandler");
        }
        synchronized (this) {
            mIdleHandlers.add(handler);
        }
    }
```

```java
    public void removeIdleHandler(@NonNull IdleHandler handler) {
        synchronized (this) {
            mIdleHandlers.remove(handler);
        }
    }
```

增添删除这两个方法。

![1](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/message_queue_1.png)

![2](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/message_queue_2.png)



接下来，我们来看前面提到的栅栏这个东西，分析这个东西之后，我们才能分析查找下一个的next()方法。

Message 在MessageQueue中，被分为3类，同步消息，异步消息，和栅栏。

### 同步消息：

>正常情况下我们通过Handler发送的Message都属于同步消息，除非我们在发送的时候执行该消
>息是一个异步消息。同步消息会按顺序排列在队列中，除非指定Message的执行时间，否咋
>Message会按顺序执行。


### 异步消息：

>想要往消息队列中发送异步消息，我们必须在初始化Handler的时候通过构造函数public 
>Handler(boolean async)中指定Handler是异步的，这样Handler在讲Message加入消息队
>列的时候就会将Message设置为异步的。

### 栅栏：

>障栅(Barrier) 是一种特殊的Message，它的target为null(只有障栅的target可以为
>null，如果我们自己设置Message的target为null的话会报异常)，并且arg1属性被用作障栅
>的标识符来区别不同的障栅。障栅的作用是用于拦截队列中同步消息，放行异步消息。就好像交警
>一样，在道路拥挤的时候会决定哪些车辆可以先通过，这些可以通过的车辆就是异步消息。


![3](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/message_queue_3.png)

我们来看插入栅栏的方法：

```java
public int postSyncBarrier() {
        return postSyncBarrier(SystemClock.uptimeMillis());
    }
```

```java
  private int postSyncBarrier(long when) {
        // Enqueue a new sync barrier token.
        // We don't need to wake the queue because the purpose of a barrier is to stall it.
        synchronized (this) {
            final int token = mNextBarrierToken++;//1
            final Message msg = Message.obtain();//2
            msg.markInUse();
            msg.when = when;
            msg.arg1 = token;

            Message prev = null;//3
            Message p = mMessages;

            if (when != 0) {
                while (p != null && p.when <= when) {//4
                    prev = p;
                    p = p.next;
                }
            }
            if (prev != null) { // 5
                msg.next = p;
                prev.next = msg;
            } else {
                msg.next = p;
                mMessages = msg;
            }
            return token;//6
        }
    }
```

1. 栅栏唯一标示，从0开始自增。
2. 从Message消息对象池中获取一个message对象，表示改成InUse，when改成when，arg1改成栅栏唯一标示。
3. 创建变量pre和p，为之后的操作做准备
4. 对队列中的第一个Message的when和障栅的when作比较，决定障栅在整个消息队列中的位置，比如是放在队列的头部，还是队列中第二个位置，如果障栅在头部，则拦截后面所有的同步消息，如果在第二的位置，则会放过第一个，然后拦截剩下的消息，以此类推。
5. 将msg(栅栏)插入队列中
6. 返回token

分析完了栅栏的插入，我们来看移除栅栏的方法：

```java
  public void removeSyncBarrier(int token) {
        synchronized (this) {
            Message prev = null;
            Message p = mMessages;
            while (p != null && (p.target != null || p.arg1 != token)) {
                prev = p;
                p = p.next;
            }
 //忽略代码....
            final boolean needWake;
            if (prev != null) {
                prev.next = p.next;
                needWake = false;
            } else {
                mMessages = p.next;
                needWake = mMessages == null || mMessages.target != null;
            }
            p.recycleUnchecked();
//忽略代码
        }
    }
```

移除的方法相对简单，遍历链表找到栅栏，然后使其脱链。

关于栅栏的使用，请看demo：

![4](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/message_queue_4.png)

![5](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/message_queue_5.png)

![6](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/message_queue_6.png)

![7](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/message_queue_7.png)





可以看出，在postSyncBarrier之后，剩余的消息被拦截起来，只有removeSyncBarrier除去栅栏后，handler才能处理剩余的message(此时处于阻塞状态)，由于removeSyncBarrier和postSyncBarrier均为hide方法。所以我们只有通过反射来处理。

接下来，我们讲解入列方法enqueueMessage：

```java
 boolean enqueueMessage(Message msg, long when) {
        if (msg.target == null) {
            throw new IllegalArgumentException("Message must have a target.");//1
        }
        if (msg.isInUse()) {
            throw new IllegalStateException(msg + " This message is already in use.");//2
        }
       synchronized (this) {
            if (mQuitting) {//3
                IllegalStateException e = new IllegalStateException(
                        msg.target + " sending message to a Handler on a dead thread");
                Log.w(TAG, e.getMessage(), e);
                msg.recycle();
                return false;
            }
            msg.markInUse();//4
            msg.when = when;//5
            Message p = mMessages;//6
            boolean needWake;
            if (p == null || when == 0 || when < p.when) {//7
                // New head, wake up the event queue if blocked.
                msg.next = p;
                mMessages = msg;
                needWake = mBlocked;
            } else {
                needWake = mBlocked && p.target == null && msg.isAsynchronous();
                Message prev;
                for (;;) {//8
                    prev = p;
                    p = p.next;
                    if (p == null || when < p.when) {
                        break;
                    }
                    if (needWake && p.isAsynchronous()) {
                        needWake = false;
                    }
                }
                msg.next = p; // 9
                prev.next = msg;
            }
            if (needWake) {
                nativeWake(mPtr);
            }
        }
        return true;
    }
```

1. msg没有持有handler 对象，抛异常
2. msg已经在使用，抛异常
3. 如果正在关闭的话，抛异常，回收msg，return false
4. 标记为正在使用
5. msg记录时间when
6. 初始一个指针p指向当前message
7. 判断是否为第一个message，或者msg的触发时间是队列中最早的
8. 开始循环遍历直至尾端或者时间早于when(p == null || when < p.when)
9. 插入该msg

然后是返回Message的next方法()

```java
  Message next() {
     //忽略代码...
        int pendingIdleHandlerCount = -1; // -1 only during first iteration
        int nextPollTimeoutMillis = 0;
//忽略代码...
            synchronized (this) {
                final long now = SystemClock.uptimeMillis();
                Message prevMsg = null;
                Message msg = mMessages;
                if (msg != null && msg.target == null) {//一进来就发现是第一个就是栅栏
                  do {
                        prevMsg = msg;
                        msg = msg.next;
                    } while (msg != null && !msg.isAsynchronous());//遍历直到找到一个异步消息
                }
                if (msg != null) {
                    if (now < msg.when) {//判断该Mesage是否到了被执行的时间。
                        nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);//当Message还没有到被执行时间的时候，记录下一次要执行的Message的时间点
                    } else {
                     // Message的被执行时间已到 
 // 从队列中取出该Message，并重新构建原来队列的链接 
// 此刻说明说有消息，所以不能阻塞
                        mBlocked = false;
                        if (prevMsg != null) {
                            prevMsg.next = msg.next;
                        } else {
                            mMessages = msg.next;
                        }
                        msg.next = null;
                        if (DEBUG) Log.v(TAG, "Returning message: " + msg);
                        msg.markInUse();
                        return msg;//取出message
                    }
                } else {
                     // 没有任何可执行的Message，重置时间
                    nextPollTimeoutMillis = -1;
                }

               // 关闭消息队列，返回null，通知Looper停止循环
                if (mQuitting) {
                    dispose();
                    return null;
                }

                // 当第一次循环的时候才会在空闲的时候去执行IdleHandler，从代码可以看出所谓的空闲状态
 // 指的就是当队列中没有任何可执行的Message，这里的可执行有两要求，
 // 即该Message不会被障栅拦截，且Message.when到达了执行时间点

                if (pendingIdleHandlerCount < 0
                        && (mMessages == null || now < mMessages.when)) {
                    pendingIdleHandlerCount = mIdleHandlers.size();
                }
消息队列在阻塞的标示是消息队列中没有任何消息，并且所有的 IdleHandler 都已经执行过一次了
                if (pendingIdleHandlerCount <= 0) {
                    // No idle handlers to run.  Loop and wait some more.
                    mBlocked = true;
                    continue;
                }
// 初始化要被执行的IdleHandler，最少4个
                if (mPendingIdleHandlers == null) {
                    mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
                }
                mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);
            }

            // 开始循环执行所有的IdleHandler，并且根据返回值判断是否保留IdleHandler
            for (int i = 0; i < pendingIdleHandlerCount; i++) {
                final IdleHandler idler = mPendingIdleHandlers[i];
                mPendingIdleHandlers[i] = null; // release the reference to the handler

                boolean keep = false;
                try {
                    keep = idler.queueIdle();
                } catch (Throwable t) {
                    Log.wtf(TAG, "IdleHandler threw exception", t);
                }

                if (!keep) {
                    synchronized (this) {
                        mIdleHandlers.remove(idler);
                    }
                }
            }

     //IdleHandler只会在消息队列阻塞之前执行一次，执行之后改标示设置为0，
            // 之后就不会再执行，一直到下一次调用MessageQueue.next() 方法。
            pendingIdleHandlerCount = 0;

 当执行了IdleHandler 的 处理之后，会消耗一段时间，这时候消息队列里的可能有消息已经到达 
             // 可执行时间，所以重置该变量回去重新检查消息队列。
            nextPollTimeoutMillis = 0;
        }
    }
```

代码量看起来多，但是逻辑并不复杂。前面处理了栅栏，最后处理了IdleHandler。中间那个else里返回msg。


经查阅资料得知，真正阻塞的地方是nativePollOnce()方法。
nativePollOnce(ptr, nextPollTimeoutMillis)是一个native方法，是一个阻塞操作。其中nextPollTimeoutMillis代表下一个消息到来前，还需要等待的时长；当nextPollTimeoutMillis = -1时，表示消息队列中无消息，会一直等待下去。空闲后，往往会执行IdleHandler中的方法。当nativePollOnce()返回后，next()从mMessages中提取一个消息。nativePollOnce()在native做了大量的工作，想深入研究可查看资料： [Android消息机制2-Handler(Native层)](http://gityuan.com/2015/12/27/handler-message-native/)。


接下来就是removeMessage的操作了，remove的操作有5个：

 - void removeMessages(Handler h, Runnable r, Object object) 
 - void removeMessages(Handler h, int what, Object object)
 - void removeCallbacksAndMessages(Handler, Object)
 - void removeAllMessagesLocked()
 - void removeAllFutureMessagesLocked()

我们选一个来看：

```java
    void removeMessages(Handler h, int what, Object object) {
        if (h == null) {//1
            return;
        }

        synchronized (this) {
            Message p = mMessages;//2

            // Remove all messages at front.
            while (p != null && p.target == h && p.what == what
                   && (object == null || p.obj == object)) { //从消息队列的头部开始
                Message n = p.next;
                mMessages = n;
                p.recycleUnchecked();
                p = n;
            }
           //移除剩余的符合要求的消息
            while (p != null) { //4
                Message n = p.next;
                if (n != null) {
                    if (n.target == h && n.what == what
                        && (object == null || n.obj == object)) {
                        Message nn = n.next;
                        n.recycleUnchecked();
                        p.next = nn;
                        continue;
                    }
                }
                p = n;
            }
        }
    }
```

- 第1步 对传递进来的Handler做非空判断，如果传递进来的Handler为空，则直接返回
- 第2步 加同步锁，指针指向mMessages
- 第3步 移除第一个Message，并回收
- 第4步 删除剩下的符合删除条件的Message。



最后是quite方法：

```java

  void quit(boolean safe) {
        if (!mQuitAllowed) {
            throw new IllegalStateException("Main thread not allowed to quit.");
        }

        synchronized (this) {
            if (mQuitting) {
                return;
            }
            mQuitting = true;

            if (safe) {
                removeAllFutureMessagesLocked();
            } else {
                removeAllMessagesLocked();
            }

            // We can assume mPtr != 0 because mQuitting was previously false.
            nativeWake(mPtr);
        }
    }
```

我们仅需注意其中的 **removeAllFutureMessagesLocked()** 方法和 **removeAllMessagesLocked()** 方法

```java
  private void removeAllMessagesLocked() {
        Message p = mMessages;
        while (p != null) {
            Message n = p.next;
            p.recycleUnchecked();
            p = n;
        }
        mMessages = null;
    }
```
```java
 private void removeAllFutureMessagesLocked() {
        final long now = SystemClock.uptimeMillis();
        Message p = mMessages;
        if (p != null) {
            if (p.when > now) {//1
                removeAllMessagesLocked();
            } else {
                Message n;
                for (;;) {
                    n = p.next;
                    if (n == null) {
                        return;
                    }
                    if (n.when > now) {
                        break;
                    }
                    p = n;
                }
                p.next = null;
                do {
                    p = n;
                    n = p.next;
                    p.recycleUnchecked();
                } while (n != null);
            }
        }
    }
```

removeAllFutureMessagesLocked相对于removeAllMessagesLocked其实多做了一层时间上的判断，我们知道详细是根据时间从前往后排列的，如果第一个消息都是之后的时间，那直接移除。否则的话，如果消息队列中的头元素小于或等于当前时间，则说明要从消息队列中截取，从中间的某个未知的位置截取到消息队列链表的队尾。这个时候就需要找到这个具体的位置，这个步骤主要就是做这个事情。通过对比时间，找到合适的位置。找到合适的位置后，就开始删除这个位置到消息队列队尾的所有元素

