[参考资料 - Android 内存泄漏总结](https://yq.aliyun.com/articles/3009)


# Java 内存分配策略
Java 程序运行时的内存分配策略有三种,分别是静态分配,栈式分配,和堆式分配，对应的，三种存储策略使用的内存空间主要分别是静态存储区（也称方法区）、栈区和堆区。

- 静态存储区（方法区）：主要存放静态数据、全局 static 数据和常量。这块内存在程序编译时就已经分配好，并且在程序整个运行期间都存在。
- 栈区 ：当方法被执行时，方法体内的局部变量都在栈上创建，并在方法执行结束时这些局部变量所持有的内存将会自动被释放。因为栈内存分配运算内置于处理器的指令集中，效率很高，但是分配的内存容量有限。
- 堆区 ： 又称动态内存分配，通常就是指在程序运行时直接 new 出来的内存。这部分内存在不使用时将会由 Java 垃圾回收器来负责回收。

在方法体内定义的（局部变量）一些基本类型的变量和对象的引用变量都是在方法的栈内存中分配的。当在一段方法块中定义一个变量时，Java 就会在栈中为该变量分配内存空间，当超过该变量的作用域后，该变量也就无效了，分配给它的内存空间也将被释放掉，该内存空间可以被重新使用。

堆内存用来存放所有由 new 创建的对象（包括该对象其中的所有成员变量）和数组。在堆中分配的内存，将由 Java 垃圾回收器来自动管理。在堆中产生了一个数组或者对象后，还可以在栈中定义一个特殊的变量，这个变量的取值等于数组或者对象在堆内存中的首地址，这个特殊的变量就是我们上面说的引用变量。我们可以通过这个引用变量来访问堆中的对象或者数组。

局部变量的基本数据类型和引用存储于栈中，引用的对象实体存储于堆中。—— 因为它们属于方法中的变量，生命周期随方法而结束。

Java的内存管理就是对象的分配和释放问题。在 Java 中，程序员需要通过关键字 new 为每个对象申请内存空间 (基本类型除外)，所有的对象都在堆 (Heap)中分配空间。另外，对象的释放是由 GC 决定和执行的。在 Java 中，内存的分配是由程序完成的，而内存的释放是由 GC 完成的，这种收支两条线的方法确实简化了程序员的工作。但同时，它也加重了JVM的工作。这也是 Java 程序运行速度较慢的原因之一。因为，GC 为了能够正确释放对象，GC 必须监控每一个对象的运行状态，包括对象的申请、引用、被引用、赋值等，GC 都需要进行监控。

>在Java中，内存泄漏就是存在一些被分配的对象，这些对象有下面两个特点，首先，这些对象是可达的，即在有向图中，存在通路可以与其相连；其次，这些对象是无用的，即程序以后不会再使用这些对象。如果对象满足这两个条件，这些对象就可以判定为Java中的内存泄漏，这些对象不会被GC所回收，然而它却占用内存。


#内存泄漏各种情况分析

## 集合类泄漏

错误示范： 
  
```java
	Person wong = new Person();
	hashMap.put("dingo",wong);
    wong =null;
```
          
如果这个hashMap是全局的(生命周期和应用生命周期一样长)。那么便会出现内存泄漏，一直持有一个wong无用对象。

## 单例造成的内存泄漏

错误示范：

```java
public class XXXXManager {

    private Context context;
    private static XXXXManager instance;

    private XXXXManager(Context context) {
        this.context = context;
    }

    public static XXXXManager getInstance(Context context) {
        if (instance == null) {
            instance = new XXXXManager(context);
        }
        return instance;
    }

}
```

```java
 XXXXManager xxxxManager = XXXXManager.getInstance(TestActivity.this);
```

由于单例的静态特性使得其生命周期跟应用的生命周期一样长，所以如果持有的Context是属于Activity的话，Activity的Context对象无法释放，导致内存泄漏，解决的办法很简单：传入Application的Context对象或者，在Application 的onCreate中初始化各种Manager的实例。

## 非静态内部类
错误示范:

```java
public class Test3Activity extends AppCompatActivity {

    private static XXResources reasource = null;

   @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

  if (reasource == null)
            reasource = new XXResources();

 class XXResources {
.....
    }
}
```

第一次：

![1](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/leak_1.png?raw=true)

跳到其他页面并finish自身，第二次进入时：

![2](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/leak_2.png?raw=true)

resource的持有了Test3Activity@4943对象，让其无法正确释放。


## 异步线程与Activity生命周期不同步

错误示范：

```java
   final Runnable runnable1 = new MyRunnable();
        final Runnable runnable2 = new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < Integer.MAX_VALUE; i++) {
                        Log.i("!!!!2", this.getClass().getName());
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

```

```java
 private class MyRunnable implements Runnable {
        @Override
        public void run() {
            try {
                for (int i = 0; i < Integer.MAX_VALUE; i++) {
                    Log.i("!!!!1", this.getClass().getName());
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
```
```java
  Thread thread1 = new Thread(runnable1);
                thread1.setName("leak1");
                Thread thread2 = new Thread(runnable2);
                thread1.setName("leak2");
                thread1.start();
                thread2.start();
```

```java
!!!!1: com.example.dingo.demo.Test3Activity$MyRunnable
!!!!2: com.example.dingo.demo.Test3Activity$2
```

Test3Activity执行onDestory，两个线程依然持有该对象。导致无法回收

## 偷懒使用Handler

错误示范：

```java
private Handler handler;

   handler = new Handler() {
            @Override
            public void dispatchMessage(Message msg) {
                操作View
            }
        };
```

 测试代码：
 
 
```java
 handler.sendEmptyMessageDelayed(4, 1000 * 5);
 Intent intent = new Intent();
 intent.setClass(Test3Activity.this, TestActivity.class);
 startActivity(intent);
 finish();
```


## 用生命周期比activity更长的事物去持有acitvitiy


比如经常有人超傻逼的在Application中用列表保存Activity，自以为聪明地在Application onCreate中创建一个list，然后在每个activity onCreate的时候加入list，还经常在onDestory忘了remove。为什么这么写的，还告诉你说有需求token过期，退出所有activity跳转到登陆页面，或者什么当前获取activity弹dialog。要不然就是管理Acitvity。mmp，退出所有activity为什么不用FLAG_ACTIVITY_CLEAR_TASK。获取当前activity我为什么不用ActivityLifeCircle？

## 未正确关闭资源

资源使用完成后没有关闭，例如：BraodcastReceiver，ContentObserver，File，Cursor，Stream，Bitmap。无论如何，记得做完事擦屁股

## 错误的覆写了finalize()
错误的覆写了finalize()方法，finalize()方法执行执行不确定，可能会导致引用无法被释放。这一块不太明白，没重写过

# 针对于内存泄漏的修正：

在 Activity 中避免使用非静态内部类:
```java
    static class MyHandler extends Handler {

        private WeakReference<Activity> weakReference;

        MyHandler(IMainView view) {
            weakReference = new WeakReference<Activity>(view);
        }

        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            if (weakReference.get() != null)
                dosth
        }
    }

```

```java
 private static final Runnable sRunnable = new Runnable() {
      @Override
      public void run() { 

 }
  };
```

使用弱引用和软引用包装占用内存大而且生命周期较长的对象
![3](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/leak_3.jpg?raw=true)

尽量避免使用 static 成员变量