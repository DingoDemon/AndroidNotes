## AndroidManifest的作用与理解
Manifest告知系统在运行app之前系统必须了解的信息，包名，描述应用的各个组件 Activity、Service、BoardCastReceiver和ContentProvider,和组件的所在进程。声明应用必须具备的权限，列出 Instrumentation 类，声明应用所需的最低 Android API 级别，列出应用必须链接到的库

## requestLayout()、invalidate()与postInvalidate()有什么区别？

- 子View调用requestLayout方法，会标记当前View及父容器，同时逐层向上提交，直到ViewRootImpl处理该事件，ViewRootImpl会调用三大流程，从measure开始，对于每一个含有标记位的view及其子View都会进行测量、布局、绘制。

- 当子View调用了invalidate方法后，会为该View添加一个标记位，同时不断向父容器请求刷新，父容器通过计算得出自身需要重绘的区域，直到传递到ViewRootImpl中，最终触发performTraversals方法，进行开始View树重绘流程(只绘制需要重绘的视图)。

- postInvalidate方法与invalidate方法的作用是一样的，都是使View树重绘，但两者的使用条件不同，postInvalidate是在非UI线程中调用，invalidate则是在UI线程中调用。 

## 描述一次网络请求的流程

### 域名解析：

拿到到host之后，从本地寻找该host的IP地址，如果没有的话，请求上层dns服务器，上层DNS服务器的本地缓存中如果没有该记录，则再向上层询问，一直到DNS根服务器直至找到。

###  TCP连接建立: 

然后进行三次握手进行TCP连接:

- (1) A发送报文至B(首部SYN = 1，A端初始序号seq =x)，SYN报文消耗一个序号，并且不能携带数据。A进入同步已发送状态
- (2) B如果同意打开连接, 则回传(首部SYN =1，首部ACK = 1，B端初始序号y，确认号ack = x+1)。同样消耗一个序号，并且不能携带数据，B进入同步收到状态。
- (3) A发送报文至B(首部ACK = 1，序号seq = x+1，确认号seq = y+1)，可以携带数据，如果携带数据则消耗一个序号，不携带则不消耗。

###  客户端写入HTTP请求：

- 请求行：标示HTTP协议版本和方法。
- 请求头：headers
- 空行(CR+LF)
- 主体：body

### 服务端返回响应内容:

解析报文

### 根须实际情况，选择是否关闭连接：

- (1)A向B发送断开请求报文，将首部FIN置1(FIN = 1, seq = u)
- (2)B向A回执报文，(ACK = 1,seq = v,ack = u+1)
- (3)B向A传递报文,将首部FIN置1(ACK = 1, FIN =1,seq = w,ack = u+1)
- (4)A向B确认(ACK = 1,seq = u+1,ack = w+1)

## Bitmap对象的理解


[《Bitmap》](https://dingowong.top/2018/05/29/Bitmap/)

## LaunchMode应用场景
  
  
- (1)standard:标准模式，系统默认模式：每次启动一个Activity都会新建一个实例，不管这个实例是否存在，onCreate,onStart,onResume生命周期均会调用。

- (2)singleTop:栈顶复用模式：如果该Activity位于任务栈的栈顶，则该Activity不会被重复创建，同时他的onNewIntent方法会回调。onCreate,onStart不会被调用。如果该Activity已经存在但不位于栈顶，则仍会重复创建。

- (3)singleTask：栈内复用模式:单例模式，在该模式下，只要Activity的在一个栈中存在，那么多次启动此Activity都不会重复创建实例，并且和singleTop一样，系统会回调onNewIntent。举例，当一个singleTask的Activity启动后，系统会寻找是否存在该Activity想要的任务栈，如果存在，则判断是否存在该Activity是否存在实例，如果有，就调到栈顶并且回调onNewIntent方法，如果不存在就新创建一个实例并压入栈。如果不存在这个Activity所想要的任务栈则新建一个任务栈并入栈。

- (4)singleInstance:单实例模式，加强版singleTask,具有此模式的Activity，只能单独运行在一个任务栈中。

## SpareArray原理

[《SpareArray》](https://dingowong.top/2018/05/22/SparseArray/)

## Handler机制和底层实现

[《Handler》](https://dingowong.top/2018/05/22/Handler/)

## Handler、Thread和HandlerThread的差别

Handler：见上文
Thread：线程，不赘述
HandlerThread继承自Thread，封装了一些looper的操作方便给开发者调用。

## handler发消息给子线程，looper怎么启动？

Looper.prepare();

## 关于Handler，在任何地方new Handler 都是什么线程下?
当前线程

## ThreadLocal原理，实现及如何保证Local属性？

ThreadLocal内部使用了一个Map存储thread和变量。为每个线程单独维护了一份变量，互不影响

## 请解释下在单线程模型中Message、Handler、Message Queue、Looper之间的关系

[《MessageQueue》](https://dingowong.top/2018/05/22/MessageQueue/)
[《Handler》](https://dingowong.top/2018/05/22/Handler/)
[《Looper》](https://dingowong.top/2018/05/22/Looper/)

## 请描述一下View事件传递分发机制

[《Touch事件分发》](https://dingowong.top/2018/05/22/%E5%AE%89%E5%8D%93Touch%E4%BA%8B%E4%BB%B6%E5%88%86%E5%8F%91%E6%9C%BA%E5%88%B6%E5%92%8C%E5%86%B2%E7%AA%81%E8%A7%A3%E5%86%B3/)

## Touch事件传递流程
[《Touch事件分发》](https://dingowong.top/2018/05/22/%E5%AE%89%E5%8D%93Touch%E4%BA%8B%E4%BB%B6%E5%88%86%E5%8F%91%E6%9C%BA%E5%88%B6%E5%92%8C%E5%86%B2%E7%AA%81%E8%A7%A3%E5%86%B3/)

## 事件分发中的onTouch 和onTouchEvent 有什么区别，又该如何使用？

如果设置了onTouchlistener的话，返回false才会调用onTouchEvent,否则不调用onTouchEvent。所以touchListener的优先级比onTouchEvent高。onClickListener的优先级比onTouchEvent还低，处于最底层。
## View和ViewGroup分别有哪些事件分发相关的回调方法
[《Touch事件分发》](https://dingowong.top/2018/05/22/%E5%AE%89%E5%8D%93Touch%E4%BA%8B%E4%BB%B6%E5%88%86%E5%8F%91%E6%9C%BA%E5%88%B6%E5%92%8C%E5%86%B2%E7%AA%81%E8%A7%A3%E5%86%B3/)

## View刷新机制

AttachInfo中保存的信息告诉父View刷新自己

## View绘制流程

- 绘制背景：drawBackground()
- 绘制主体：onDraw();
- 绘制子View: dispatchDraw();
- 绘制滑动条和前景:onDrawForeground();


## 自定义控件原理

[《measure》](https://dingowong.top/2018/05/22/View%E7%9A%84%E5%B7%A5%E4%BD%9C%E4%BD%93%E7%B3%BB%E4%B9%8BMeasure/)
[《layout》](https://dingowong.top/2018/05/22/View%E7%9A%84%E5%B7%A5%E4%BD%9C%E4%BD%93%E7%B3%BB%E4%B9%8BLayout/)
[《draw》](https://dingowong.top/2018/05/22/View%E7%9A%84%E5%B7%A5%E4%BD%9C%E4%BD%93%E7%B3%BB%E4%B9%8BDraw(1)/)

## 自定义View如何提供获取View属性的接口？

For Example:

- 第一步：

define declare-styleable in style:

```html
   <declare-styleable name="MyView1">
        <attr name="photo" format="reference" />
    </declare-styleable>
```

- 第二步：add attrs in xml


```html
 <wong.dingo.com.viewdemo.Views.MyView1
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:photo="@drawable/cat2"
        />
```    
 
    
- 第三步：set Value In Constructor


```java
  private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MyView1);
        photoResource = typedArray.getResourceId(R.styleable.MyView1_photo, R.drawable.cat2);
        if(photoResource != 0)
        bitmap = BitmapFactory.decodeResource(getResources(), photoResource);
        typedArray.recycle();
    }
```
    
- 第四步： Use this attribute in code

```java
  paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shader = new BitmapShader(bitmap, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR);
        paint.setShader(shader);
```
        
Android View的构造函数，下来还得继续深入学习下

## AsyncTask机制

AsyncTask这个没用，异步任务我选RxJava2。
大概是内部维护了一个线程池，将任务加入线程池中。但是根据版本变化，线程池中任务是串行还是并行有所变化

##  为什么不能在子线程更新UI？

哪个线程创建了View，就只有哪个线程能触及它
##  ANR产生的原因是什么？


1. View的点击事件或者触摸事件在特定的时间5s内无法得到响应。
2. BroadcastTimeout，广播在10s内无法完成处理
3. Service的各个生命周期函数在20s内无法完成处理。

##  ANR定位和修正
当发生ANR时，可以通过结合Logcat日志和生成的位于手机内部存储的/data/anr/traces.tex文件进行分析和定位。

## oom是什么？
out of memory 内存溢出
##  什么情况导致oom？
android系统为每一个应用程序都设置了一个硬性的条件：DalvikHeapSize最大阀值64M/48M/24M.如果你的应用程序内存占用接近这个阀值，此时如果再尝试内存分配的时候就会造成OOM
##  有什么解决方法可以避免OOM？

避免在内存中装载big object。可使用SoftReference, WeakReference, 硬盘缓存来缓解

##  Oom 是否可以try catch？为什么？
在try语句中声明了很大的对象，导致OOM，并且可以确认OOM是由try语句中的对象声明导致的，那么在catch语句中，可以释放掉这些对象，解决OOM的问题，继续执行剩余语句。
但是这通常不是合适的做法。
Java中管理内存除了显式地catch OOM之外还有更多有效的方法：比如SoftReference, WeakReference, 硬盘缓存等。
在JVM用光内存之前，会多次触发GC，这些GC会降低程序运行的效率。
如果OOM的原因不是try语句中的对象（比如内存泄漏），那么在catch语句中会继续抛出OOM
作者：知乎用户
链接：https://www.zhihu.com/question/54630917/answer/140320945
来源：知乎
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。

## 内存泄漏是什么？

[《Android 内存泄漏总结》](https://dingowong.top/2018/05/28/Android%E5%86%85%E5%AD%98%E6%B3%84%E6%BC%8F%E6%80%BB%E7%BB%93/)

##  什么情况导致内存泄漏？

[《Android 内存泄漏总结》](https://dingowong.top/2018/05/28/Android%E5%86%85%E5%AD%98%E6%B3%84%E6%BC%8F%E6%80%BB%E7%BB%93/)

## 如何防止线程的内存泄漏？

[《Android 内存泄漏总结》](https://dingowong.top/2018/05/28/Android%E5%86%85%E5%AD%98%E6%B3%84%E6%BC%8F%E6%80%BB%E7%BB%93/)

## 内存泄露场的解决方法

[《Android 内存泄漏总结》](https://dingowong.top/2018/05/28/Android%E5%86%85%E5%AD%98%E6%B3%84%E6%BC%8F%E6%80%BB%E7%BB%93/)

## 内存泄漏和内存溢出区别？

见上文


## LruCache默认缓存大小

这不是用户自定义的么。。。。

[《LruCache》](https://dingowong.top/2018/05/31/LruCache/)

## ContentProvider的权限管理(解答：读写分离，权限控制-精确到表级，URL控制)

额，我查查再来写，另外，是URI吧

## 如何通过广播拦截和abort一条短信？

1. 申请android.provider.Telephony.SMS_RECEIVED权限
2. 静态注册receiver，并且将它的优先级设为最高(即有序广播)
3. 拦截广播 abortBroadcast()

```html
       <receiver android:name=".MySMSReceiver">
            <intent-filter android:priority="1000">
                <action android:name="android.provider.Telephony.SMS_RECEIVED" />
            </intent-filter>
        </receiver>
```

```java        
 abortBroadcast();
``` 

##  广播是否可以请求网络？

不能,主线程

## 广播引起anr的时间限制是多少？

10s

## 计算一个view的嵌套层级

.....好吧。

```java
 public int getHierarchy(View view) {
        return getHierarchy(view, 1);
    }

    private int getHierarchy(View view, int i) {
        if (view.getParent() == null) {
            return i;
        } else {
            i++;
            return getHierarchy((View) view.getParent(), i);
        }

    }
```
    
## Activity栈

taskAffinity。Activity都有taskAffinity属性，这个属性指出了它所需的栈的名字，如果没有特别制定，则为包名。可以给每一个Activity都指定taskAffinity属性，但如果和包名一致，则相当于没有指定。

## ListView重用的是什么？

ViewHolder


## Android为什么引入Parcelable？

性能，Serializable效率太低不适合安卓平台

## ActivityThread，AMS，WMS的工作原理

再等半年我就能告诉你了


