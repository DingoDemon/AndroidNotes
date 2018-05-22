**参考资料：
http://hencoder.com/ui-1-1/
https://cloud.tencent.com/developer/article/1038842
http://www.gcssloop.com/customview/Path_Over**

draw的方法很多，这一篇主要介绍canvas.draw()。下一篇再写Paint。
这一篇直接举例简单应用：

## drawColor
直接画一片Color

```
drawColor(@ColorInt int color)

public void drawRGB(int r, int g, int b) 

public void drawARGB(int a, int r, int g, int b)
```


## drawCircle

画正圆

```java
drawCircle(float cx, float cy, float radius, @NonNull Paint paint) 
```
![1](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_1.png)



如果想画空心圆（或者叫环形），也可以使用 paint.setStyle(Paint.Style.STROKE) 来把绘制模式改为画线模式。

```java
 Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(100,100,88,paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawCircle(350,100,88,paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.colorPrimary));
        canvas.drawCircle(100,350,88,paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(20);
        canvas.drawCircle(350,350,88,paint);
```
        

在绘制的时候，往往需要开启抗锯齿来让图形和文字的边缘更加平滑。开启抗锯齿很简单，只要在 new Paint() 的时候加上一个 ANTI_ALIAS_FLAG 参数就行：
Paint paint = new Paint(ANTI_ALIAS_FLAG);


![2](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_2.png)


## Rect

画矩形

```java
 public void drawRect(float left, float top, float right, float bottom, @NonNull Paint paint) 
```

类似圆，也能通过paint.setStyle来设置空心实心

## Point

画点

```java
 public void drawPoint(float x, float y, @NonNull Paint paint)
```

圆点和方点的切换使用 paint.setStrokeCap(cap)：`ROUND` 是圆点，`BUTT` 或 `SQUARE` 是方点

```java
 Paint paint = new Paint();
        paint.setStrokeWidth(50);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawPoint(100,100,paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
        canvas.drawPoint(200,100,paint);
```

![3](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_3.png)


## Oval

画椭圆

```java
 public void drawOval(float left, float top, float right, float bottom, @NonNull Paint paint) 
```

## line

画直线

```java
 drawLine(float startX, float startY, float stopX, float stopY,
            @NonNull Paint paint)
```

## RoundRect

圆角矩形

```java
 drawRoundRect(float left, float top, float right, float bottom, float rx, float ry,@NonNull Paint paint)
```

## Arc

画扇形和圆弧

```java
public void drawArc(float left, float top, float right, float bottom, float startAngle,
            float sweepAngle, boolean useCenter, @NonNull Paint paint)
```

- startAngel 起始角度（x 轴的正向，即正右的方向，是 0 度的位置；顺时针为正角度，逆时针为负角度）
- sweepAngle 经过角度
- useCenter 是否连接到圆心

```java
  canvas.drawArc(200,200,400,400,0,80,true,paint);
  canvas.drawArc(200,200,400,400,90,160,false,paint);
  paint.setStyle(Paint.Style.STROKE);
  canvas.drawArc(200,200,400,400,-100,80,true,paint);
```

![4](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_4.png)



## path 

*重头戏来了，Path*

### 先讲path的第一组方法：
__moveTo、 setLastPoint、 lineTo 和 close__


public void lineTo (float x, float y)我们惊奇的发现只有一个点，一个点怎么画线？
实际上，lineTo是指的某一个点到(float x, float y)所做的line。


```java
        Paint mPaint = new Paint();             // 创建画笔
        mPaint.setColor(Color.BLACK);           // 画笔颜色 - 黑色
        mPaint.setStyle(Paint.Style.STROKE);    // 填充模式 - 描边
        mPaint.setStrokeWidth(10);              // 边框宽度 - 10
        Path path = new Path();                     // 创建Path
        path.lineTo(200, 200);                      // lineToOne
        path.lineTo(200,0);                  //lineToTwo
        canvas.drawPath(path, mPaint);
```

![5](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_5.png)


可以看到lineToOne这里的起点就是(0,0),lineToTwo这里的起点为上一次的落点(200,0)



 +    public void moveTo (float x, float y)
 +    public void setLastPoint (float dx, float dy)


| 方法名 | 简介 |是否影响之前的操作| 是否影响之后操作| 
| ----  | --- |---------------|------------ | 
| moveTo| 移动下一次操作的起点位置 |否| 是 | 
| setLastPoint| 设置之前操作的最后一个点位置| 是| 是 |


```java
        path.lineTo(200, 200);// lineTo
        path.moveTo(200,100);
        path.lineTo(200,0);
```

![6](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_6.png)

我们将path.moveTo(200,100)修改为path.setLastPoint(200,100);

![7](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_7.png)


根据这两个图就可以很明显的区分出区别了。


close方法用于连接当前最后一个点和最初的一个点(如果两个点不重合的话)，最终形成一个封闭的图形。

### 第2组: 

__addXxx__


```java
    // 圆形
    public void addCircle (float x, float y, float radius, Path.Direction dir)
    // 椭圆
    public void addOval (RectF oval, Path.Direction dir)
    // 矩形
    public void addRect (float left, float top, float right, float bottom, Path.Direction dir)
    public void addRect (RectF rect, Path.Direction dir)
    // 圆角矩形
    public void addRoundRect (RectF rect, float[] radii, Path.Direction dir)
    public void addRoundRect (RectF rect, float rx, float ry, Path.Direction dir)
   
```
与canvas.draw并无太大区别，注意最后的Path.Direction：


1. CW clockwise 顺时针
2. CCW counter-clockwise 逆时针

在普通情况下，这两者并没有区别，两个path相互重叠的时候，就有区别了，这里引入另外一个问题，两个path相交，重叠了。他们之间的填充规则是怎样的？


![8](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_8.png)


这里得引入一个叫做fillType的东西：

_*Path.setFillType(fillType)*_

FillType 的取值有四个：

- EVEN_ODD
- WINDING （默认值）
- INVERSE_EVEN_ODD
- INVERSE_WINDING


#### EVEN_ODD(奇偶规则)

![9](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_9.png)

P1: 从P1发出一条射线，发现图形与该射线相交边数为0，偶数，故P1点在图形外部。
P2: 从P2发出一条射线，发现图形与该射线相交边数为1，奇数，故P2点在图形内部。
P3: 从P3发出一条射线，发现图形与该射线相交边数为2，偶数，故P3点在图形外部。



我们来看个例子：

```java
 path.setFillType(Path.FillType.EVEN_ODD);  
 path.addCircle(200,200,150, Path.Direction.CW);
 path.addCircle(300,200,150, Path.Direction.CW);
       canvas.drawPath(path,mDeafultPaint);
```       

![10](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_10.png)


奇内偶外，所以填充为这个样子。不难猜出INVERSE_EVEN_ODD的样子：

![11](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_11.jpg)

是不是很好理解。

#### WINDING(非零环绕数规则)

![12](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_12.png)


P1: 从P1点发出一条射线，沿射线方向移动，并没有与边相交点部分，环绕数为0，故P1在图形外边。
P2: 从P2点发出一条射线，沿射线方向移动，与图形点左侧边相交，该边从左到右穿过射线，环绕数－1，最终环绕数为－1，故P2在图形内部。
P3: 从P3点发出一条射线，沿射线方向移动，在第一个交点处，底边从右到左穿过射线，环绕数＋1，在第二个交点处，右侧边从左到右穿过射线，环绕数－1，最终环绕数为0，故P3在图形外部。



引入一张总截图：

![13](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_13.png)



### 第三组

__addArc与arcTo__

```java
 public void addArc (RectF oval, float startAngle, float sweepAngle)
    // arcTo
    public void arcTo (RectF oval, float startAngle, float sweepAngle)
    public void arcTo (RectF oval, float startAngle, float sweepAngle, boolean forceMoveTo)
```

| 方法 |  功效  |  描述  | 
| ----  |  --------------- | ------------------------------------ | 
| addArc | 添加一个圆弧到path | 直接添加一个圆弧到path中 | 
| arcTo | 添加一个圆弧到path | 添加一个圆弧到path，如果圆弧的起点和上次最后一个坐标 点不相同，连接两个点 | 

| forceMoveTo的值 | 效果 |
| ----  | --------------- |
| true | 将最后一个点移动到圆弧起点，即不连接最后一个点与圆弧起点 |
| false | 不移动，而是连接最后一个点与圆弧起点 |

简而言之，就是抬不抬笔，true的话就是抬


我们来尝试画个桃心：

```java
  path.addArc(200, 100, 400, 300, -225, 225);
  path.arcTo(400, 100, 600, 300, -180, 225,false);
  path.lineTo(400,444);
```

![14](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_14.png)


![heart](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/heart.png)

很简单吧，三行代码的事情。如果是画贝塞尔曲线的话，可以运用：

-  画二次贝塞尔曲线

```java
quadTo(float x1, float y1, float x2, float y2) / rQuadTo(float dx1, float dy1, float dx2, float dy2)
```

-  画三次贝塞尔曲线

```java
cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) / rCubicTo(float x1, float y1, float x2, float y2, float x3, float y3) 
```




这里我们遇到个问题，我们想画下图时，太多点位需要计算：

![15](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_15.png)



要绘制这种阴阳图，去计算各种点，太麻烦，有没有个好的解决方法呢？

肯定是有的：

```java
  Paint paint = new Paint();
        Path pathBig = new Path();
        Path up = new Path();
        Path down = new Path();
        Path rect = new Path();
        pathBig.addCircle(300, 300, 200, Path.Direction.CW);
        up.addCircle(300, 200, 100, Path.Direction.CW);
        down.addCircle(300, 400, 100, Path.Direction.CW);
        rect.addRect(300, 100, 500, 500, Path.Direction.CW);
```

一共新增了4个图案

![16](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_16.jpg)


我们根据Path的布尔运算有五种逻辑：

![17](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_17.png)

我们分3步来处理：

第一步：

pathBig.op(rect, Path.Op.DIFFERENCE);

![18](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_18.jpg)

第二步：

pathBig.op(up, Path.Op.UNION);

![19](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_19.jpg)

第三步：

pathBig.op(down, Path.Op.DIFFERENCE);

![20](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics/draw_20.png)




最后得到我们想要的图案。

## BitMap
画bitmap

```java
drawBitmap(Bitmap bitmap, float left, float top, Paint paint) 
```


## Text

画文字

```java
drawText(String text, float x, float y, Paint paint) 
```