Java基础：

##  ● java中==和equals和hashCode的区别

\==比较内存地址，如果一样返回true。
equals是Object的方法， 用于比较两个Object是否相等。Object中是==，由子类重写来决定表示方式。
hashCode用于返回Hash码(散列码)，重写equals的时候应重写HashCode，两个相同的相等的对象的散列码应是相同的。

##  ● int、char、long各占多少字节数
int 二进制32位，首位表正负，之后最大值为2^31+2^30+......2^0,即2^32-1,4字节
char，16位，Java的char类型是UTF-16的code unit，也就是一定是16位（2字节）
long，64位，8字节

##  ● int与integer的区别
一个是对象(integer)，一个是基本数据类型。Java SE5开始就提供了自动装箱的特性，比如Integer integer = 10; 实际执行的是Integer.valueOf(10)。在通过valueOf方法创建Integer对象的时候，如果数值在[-128,127]之间，便返回指向IntegerCache.cache中已经存在的对象的引用；否则创建一个新的Integer对象


##  ● 谈谈对java多态的理解

面向对象设计语言使用后期绑定，当向对象发送消息的时候，被调用的代码直到运行的时候才能确定。比如向bird.fly(),子类鹅的话就是扑腾翅膀，子类鸽子就是起飞。扑腾翅膀和起飞这两种不同的fly方式，即展示了多态

##  ● String、StringBuffer、StringBuilder区别

String在Java中被设计成不可变的，  private final char value[]，除非通过反射去修改。StringBuffer，StringBuilder简单来说的话，Buffer是线程安全的，Builder不安全。Buffer内有个char[] toStringCache缓冲区，效率比Builder慢。

## ● 什么是内部类？内部类的作用

如果想创建一个辅助类，又不希望这个类是公用的，就可以在类的内部，定义一个类。比如HashMap.Entry之于HashMap。
分为静态内部类，成员内部类，局部内部类，匿名内部类：

1. 静态内部类构建的时候需要Out.in()。静态内部类可访问外部类的静态变量和静态方法。
2. 成员内部类构造的时候需要用外部类对象来构建内部类对象：
 A a = new A();
 A.C c = a.new C();
3. 定义在外部类方法中的类，只有在调用方法的时候才能触及。局部内部类反射构造方法创建对象的时候，构造方法会需要一个外部类的引用
4. new 实现接口() | 父类构造器 (实参列表)
 public void test(getInteger getInteger) {
        System.out.println(getInteger.getFive());
    }
    interface getInteger {
        int getFive();
    }

##  ● 抽象类和接口区别
抽象类有可以有具体实现的方法，接口只是定义行为。抽象类用作被继承，接口用于被实现。
##  ● 抽象类的意义
提取并实现子类的重复行为，为子类提供一个公共的类型，定义抽象方法
##  ● 抽象类与接口的应用场景
例如Adapter接口定义了Adapter中的共有行为,BaseAdapter实现了Adapter中数据绑定的重复方法，并交由子类继续重写getView，getItem，getCount等方法。
## ● 抽象类是否可以没有方法和属性？
可以
##  ● 接口的意义
定义共同行为
##  ● 泛型中extends和super的区别
xx extends T,表示xx是T的子类，xx superT，表示xx是T的父类
##  ● 父类的静态方法能否被子类重写
不能，静态方法从程序开始运行后就已经分配了内存，也就是说已经写死了。所有引用到该方法的对象（父类的对象也好子类的对象也好）所指向的都是同一块内存中的数据，也就是该静态方法。
##  ● 进程和线程的区别
线程是CPU可调度的最小单位，进程是资源分配的最小单位。线程是属于进程的，线程运行在进程空间内，同一进程所产生的线程共享同一内存空间，当进程退出时该进程所产生的线程都会被强制退出并清除。
##  ● final，finally，finalize的区别
final是关键字，被final修饰的类，不能被继承，被final修饰的方法，不能被重写，被final修饰的属性，只能被复制一次
finally是异常机制里必将执行的代码块
finalize()是Object的protected方法，子类可以覆盖该方法以实现资源清理工作，GC在回收对象之前调用该方法。
##  ● 序列化的方式
实现Serializable或Parcelable接口
##  ● Serializable 和Parcelable 的区别
Android的Parcelable的设计初衷是因为Serializable效率过慢，为了在程序内不同组件间以及不同Android程序间(AIDL)高效的传输数据而设计
##  ● 静态属性和静态方法是否可以被继承？是否可以被重写？以及原因？
可以，不可以，见上
##  ● 静态内部类的设计意图
见上
##  ● 成员内部类、静态内部类、局部内部类和匿名内部类的理解，以及项目中的应用
见上