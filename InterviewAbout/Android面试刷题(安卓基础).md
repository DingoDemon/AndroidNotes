##  四大组件是什么
- Activity:展示性组件，向用户展示界面并获取用户输入信息
- Service：计算型组件，用于在后台执行计算任务
- BoardCastReceiver：消息型组件，在不同组件甚至不同应用间传递消息
- ContentProvider：数据共享型组件，用于向其他组件或者应用共享数据

##  四大组件的生命周期和简单用法
见下
##  Activity之间的通信方式
- EventBus
- startAvtivityForResult && onActivityResult
- 广播

##  Activity各种情况下的生命周期

![生命周期](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/interview_3_1.png?raw=true)

从A到B，再回到A:

- 「A onCreate」 > 「A onStart」 > 「A onResume」 Activity(A)运行中 

- 「A onPause」 > 「B onCreate」 > 「B onStart」 > 「B onResume」 > 「A onStop」 Activity(B)运行中 

- 「B onPause」 > 「A onRestart」 > 「A onStart」 -> 「A onResume」 > 「B onStop」返回到Activity(A)

切换横竖屏：

- onPause()
- onSaveInstanceState(Bundle outState)
- onStop()
- onDestroy()
- onCreate()
- onStart()
- onResume()

 
##  横竖屏切换的时候，Activity 各种情况下的生命周期
见上，可能有遗漏
##  Activity与Fragment之间生命周期比较
见上
##  Activity上有Dialog的时候按Home键时的生命周期
没发现有毛区别

##  两个Activity 之间跳转时必然会执行的是哪几个方法？
一般情况下比如说有两个activity,分别叫A,B。
当在A 里面激活B 组件的时候, A会调用onPause()方法,然后B调用onCreate() ,onStart(), onResume()。
这个时候B覆盖了A的窗体, A会调用onStop()方法。
如果B是个透明的窗口,或者是对话框的样式, 就不会调用A的onStop()方法。
如果B已经存在于Activity栈中，B就不会调用onCreate()方法。
##  Activity的四种启动模式对比
- (1)standard:标准模式，系统默认模式：每次启动一个Activity都会新建一个实例，不管这个实例是否存在，onCreate,onStart,onResume生命周期均会调用。
- (2)singleTop:栈顶复用模式：如果该Activity位于任务栈的栈顶，则该Activity不会被重复创建，同时他的onNewIntent方法会回调。onCreate,onStart不会被调用。如果该Activity已经存在但不位于栈顶，则仍会重复创建。
- (3)singleTask：栈内复用模式:单例模式，在该模式下，只要Activity的在一个栈中存在，那么多次启动此Activity都不会重复创建实例，并且和singleTop一样，系统会回调onNewIntent。举例，当一个singleTask的Activity启动后，系统会寻找是否存在该Activity想要的任务栈，如果存在，则判断是否存在该Activity是否存在实例，如果有，就调到栈顶并且回调onNewIntent方法，如果不存在就新创建一个实例并压入栈。如果不存在这个Activity所想要的任务栈则新建一个任务栈并入栈。
- (4)singleInstance:单实例模式，加强版singleTask,具有此模式的Activity，只能单独运行在一个任务栈中。

##  Activity状态保存于恢复
系统销毁一个Activity时，onSaveInstanceState() 会被调用。但是当用户主动去销毁一个Activity时，例如在应用中按返回键，onSaveInstanceState()就不会被调用。因为在这种情况下，用户的行为决定了不需要保存Activity的状态。通常onSaveInstanceState()只适合用于保存一些临时性的状态，而onPause()适合用于数据的持久化保存

##  fragment各种情况下的生命周期
- （1）onAttach：onAttach()回调将在Fragment与其Activity关联之后调用。需要使用Activity的引用或者使用Activity作为其他操作的上下文，将在此回调方法中实现。
需要注意的是：将Fragment附加到Activity以后，就无法再次调用setArguments()——除了在最开始，无法向初始化参数添加内容。
- （2）onCreate(Bundle savedInstanceState)：此时的Fragment的onCreat回调时，该fragmet还没有获得Activity的onCreate()已完成的通知，所以不能将依赖于Activity视图层次结构存在性的代码放入此回调方法中。在onCreate()回调方法中，我们应该尽量避免耗时操作。此时的bundle就可以获取到activity传来的参数
- （3）onCreateView(LayoutInflater inflater, ViewGroup container,
Bundle savedInstanceState)： 其中的Bundle为状态包与上面的bundle不一样。
注意的是：不要将视图层次结构附加到传入的ViewGroup父元素中，该关联会自动完成。如果在此回调中将碎片的视图层次结构附加到父元素，很可能会出现异常。
这句话什么意思呢？就是不要把初始化的view视图主动添加到container里面，以为这会系统自带，所以inflate函数的第三个参数必须填false，而且不能出现container.addView(v)的操作。
- （4）onActivityCreated：onActivityCreated()回调会在Activity完成其onCreate()回调之后调用。在调用onActivityCreated()之前，Activity的视图层次结构已经准备好了，这是在用户看到用户界面之前你可对用户界面执行的最后调整的地方。
强调的point：如果Activity和她的Fragment是从保存的状态重新创建的，此回调尤其重要，也可以在这里确保此Activity的其他所有Fragment已经附加到该Activity中了
- （5）Fragment与Activity相同生命周期调用：接下来的onStart()\onResume()\onPause()\onStop()回调方法将和Activity的回调方法进行绑定，也就是说与Activity中对应的生命周期相同，因此不做过多介绍。
- （6）onDestroyView:该回调方法在视图层次结构与Fragment分离之后调用。
- （7）onDestroy：不再使用Fragment时调用。（备注：Fragment仍然附加到Activity并任然可以找到，但是不能执行其他操作）
- （8）onDetach：Fragme生命周期最后回调函数，调用后，Fragment不再与Activity绑定，释放资源。
  ● 如何实现Fragment的滑动？
viewpager
  ● fragment之间传递数据的方式？
通过Activity
通过tag暴露自己
通过接口
eventubs 
通知
##  Activity 怎么和Service 绑定？
bindService(Intent service, ServiceConnection conn,int flags)
##  怎么在Activity 中启动自己对应的Service？
bindService(Intent service, ServiceConnection conn,int flags)
startService(Intent service) 
##  service和activity怎么进行数据交互？
通过ServiceConnection中获取Ibinder实现实例
##  Service的开启方式
- bindService(Intent service, ServiceConnection conn,int flags)
「 onCreate ->onBind() ->onUnBind() -> onDestory()」
- startService(Intent service) 
「onCreate ->onStartCommand() ->onDestory()」
##  请描述一下Service 的生命周期
![service](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/interview_3_5.png?raw=true)

##  谈谈你对ContentProvider的理解

ContentProvider是一个抽象类，使用必须实现它的六个抽象方法：

- ● insert(Uri, ContentValues)：插入新数据；
- ● delete(Uri, String, String[])：删除已有数据；
- ● update(Uri, ContentValues, String, String[])：更新数据；
- ● query(Uri, String[], String, String[], String)：查询数据；
- ● onCreate()：执行初始化工作；
- ● getType(Uri)：获取数据MIME类型。

通过这六个方法实现数据统一共享
ContentProvider&&Sqlite:

[DBHelper](https://github.com/DingoDemon/AndroidNotes/blob/master/ExampleCode/PersonDBHelper.java)
[MyContentProvider](https://github.com/DingoDemon/AndroidNotes/blob/master/ExampleCode/MyContentProvider.java)

##  说说ContentProvider、ContentResolver、ContentObserver 之间的关系

- ContentProvider 内容提供者,用于对外提供统一数据交互方式

- ontentResolver 内容解析者, 用于获取内容提供者提供的数据

- ContentObserver 内容监听器, 可以监听数据的改变状态
resolver.registerContentObserver(uri, true, new MyObserver(myHandler));
 notifyForDescendents  表示精确匹配，即只匹配该Uri

##  请描述一下广播BroadcastReceiver的理解
BroadcastReceiver是一种消息型组件，用于在组件甚至不用应用之间传递消息
##  广播的分类
- 从注册的角度来说，分为动态注册和静态注册。
- 从作用域的角度来说，分为本地广播和全局广播。
- 从接收的角度来讲，分为标准广播和有序广播是一种完全异步执行的广播，在广播发出后所有的广播接收器会在同一时间接收到这条广播，之间没有先后顺序，效率比较高，且无法被截断。有序广播是一种同步执行的广播，在广播发出后同一时刻只有一个广播接收器能够接收到， 优先级高的广播接收器会优先接收，当优先级高的广播接收器的 onReceiver() 方法运行结束后，广播才会继续传递，且前面的广播接收器可以选择截断广播，这样后面的广播接收器就无法接收到这条广播了
##  广播使用的方式和场景
见下
##  在manifest 和代码中如何注册和使用BroadcastReceiver?
静态注册即在清单文件中为 BroadcastReceiver 进行注册，使用< receiver >标签声明，并在标签内用 < intent-filter > 标签设置过滤器。这种形式的 BroadcastReceiver 的生命周期伴随着整个应用，如果这种方式处理的是系统广播，那么不管应用是否在运行，该广播接收器都能接收到该广播
##  本地广播和全局广播有什么差别？
sendBroadcast发送和BroadcastReceiver接收到的广播全都是属于系统全局广播，即发出的广播可以被其他应用接收到，而且也可以接收到其他应用发送出的广播。本地广播是无法通过静态注册的方式来接收，通过LocalBroadcastManager来注册Receiver ：

```java
localBroadcastManager = LocalBroadcastManager.getInstance(this); localReceiver = new LocalReceiver(); IntentFilter filter = new IntentFilter(LOCAL_ACTION); localBroadcastManager.registerReceiver(localReceiver, filter);
```

##  BroadcastReceiver，LocalBroadcastReceiver 区别
见上
##  AlertDialog,popupWindow,Activity区别
AlertDialog是非阻塞式对话框：AlertDialog弹出时，后台还可以做事情；而PopupWindow是阻塞式对话框：PopupWindow弹出时，程序会等待，在PopupWindow退出前，程序一直等待，只有当我们调用了dismiss方法的后，PopupWindow退出，程序才会向下执行。
##  Application 和 Activity 的 Context 对象的区别
![context](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/interview_3_2.png?raw=true)
![context](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/interview_3_3.png?raw=true)

##  LinearLayout、RelativeLayout、FrameLayout的特性及对比，并介绍使用场景。
。。。。。略过
##  写一个回调demo
[Example Code](https://github.com/DingoDemon/AndroidNotes/blob/master/ExampleCode/Worker.java)
##  介绍下安卓动画

安卓动画分为三类：

1. Animation(view动画)
2. AnimationDrawable(帧动画)
3. PropertyAnimation(属性动画)

#### 1.ViewAnimation包括四组基本动画，平移(TranslateAnimation)，旋转(RotateAnimation)，透明度(AlphaAnimation)和缩放(ScaleAnimation)。
可以先写好动画的xml文件，然后通过AnimationUtils获取Animation实例；或者通过代码创建Animation实例。然后调用View.startAnimation(Animation animation)方法来播放动画。
此外，还有：

- (1)LayoutAnimation为ViewGroup指定一个动画，每个子View出场时均会携带此效果
- (2)Activity切换效果：enterAnim和exitAnim，需放在finish或stattActivity之后

#### 2.AnimationDrawable
顺序播放实现预定好的一组图片，通过AnimationDrawable调用

#### 3.属性动画：
- (1)ViewPropertyAnimator
通过View的ViewPropertyAnimator animate(）方法，获取ViewPropertyAnimator对象，然后执行如下方法：
![context](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/interview_3_4.png?raw=true)

- (2)ObjectAnimator
ObjectAnimator可以用来实现对View任何属性的修改，比如
ObjectAnimator ofInt(Object target, String propertyName, int... values)
target为作用对象，propertyName为作用属性，第三个属性如果为1个值的话，便是最终结果，2个的话就是起始值和最终值，多个的话就是起始值，中间值。。。。。。，最终值。
并且对propertyName提供get/set方法，让ObjectAnimator可以获取/修改属性。并在合适的时候调用 invalidate()方法进行重绘。
如果是对自定义属性作出修改的话，继承TypeEvaluator<T>，重写  public T evaluate(float fraction, T startValue, T endValue) { }方法执行变换规则。调用ObjectAnimator.setEvaluator(TypeEvaluator value)。
最后调用ObjectAnimator.start来播放动画
##  查值器
Interpolator，用于设定变换速率
##  估值器
TypeEvaluator，见上
##  介绍下SurfaceView
系统通过发出VSSYNC信号来进行屏幕的重绘，刷新的时间间隔是16ms,如果我们可以在16ms以内将绘制工作完成，则没有任何问题，如果我们绘制过程逻辑很复杂，并且我们的界面更新还非常频繁，这时候就会造成界面的卡顿，影响用户体验，为此Android提供了SurfaceView来解决这一问题。
View适用于主动更新的情况，而SurfaceView则适用于被动更新的情况，比如频繁刷新界面。
View在主线程中对页面进行刷新，而SurfaceView则开启一个或者多个子线程来对页面进行刷新。
View在绘图时没有实现双缓冲机制，SurfaceView在底层机制中就实现了双缓冲机制。
(双缓冲技术是游戏开发中的一个重要的技术。当一个动画争先显示时，程序又在改变它，前面还没有显示完，程序又请求重新绘制，这样屏幕就会不停地闪烁。而双缓冲技术是把要处理的图片在内存中处理好之后，再将其显示在屏幕上。双缓冲主要是为了解决 反复局部刷屏带来的闪烁。把要画的东西先画到一个内存区域里，然后整体的一次性画出来。)
「只知道改变，没用过，暂时也没太大兴趣去学这个」
##  序列化的作用，以及Android两种序列化的区别
Serializable（Java自带）：
Serializable是序列化的意思，表示将一个对象转换成可存储或可传输的状态。序列化后的对象可以在网络上进行传输，也可以存储到本地。
Parcelable（android 专用）：
除了Serializable之外，使用Parcelable也可以实现相同的效果，
不过不同于将对象进行序列化，Parcelable方式的实现原理是将一个完整的对象进行分解，
而分解后的每一部分都是Intent所支持的数据类型，这样也就实现传递对象的功能了。
##  Android中数据存储方式
sp,sqlite,文件