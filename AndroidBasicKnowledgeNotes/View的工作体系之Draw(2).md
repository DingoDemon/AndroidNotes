*特别感谢[HenCoder Android 开发进阶: 自定义 View 1-2 Paint 详解](http://hencoder.com/ui-1-2/),安卓draw方面的技术知识都是跟着HenCoder学习的，这篇只能说是阅读总结笔记，大部分非原创。*


# Shader

它的中文叫做「着色器」，也是用于设置绘制颜色的。「着色器」不是 Android 独有的，它是图形领域里一个通用的概念，它和直接设置颜色的区别是，着色器设置的是一个颜色方案，或者说是一套着色规则。当设置了 Shader 之后，Paint 在绘制图形和文字时就不使用 setColor/ARGB() 设置的颜色了，而是使用 Shader 的方案中的颜色。

## LinearGradient 线性渐变：

从(x1,y1)到(x2,y2)从color0到color1进行线性渐变
![1](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_1.png)

设置两个点和两种颜色，以这两个点作为端点，使用两种颜色的渐变来绘制颜色：

- x0 y0 x1 y1：渐变的两个端点的位置 
- color0 color1 是端点的颜色 
- tile：端点范围之外的着色规则，类型是 TileMode。TileMode 一共有 3 个值可选： CLAMP, MIRROR和 REPEAT。
![2](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_2.png)
![3](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_3.png)
![4](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_4.png)


## RadialGradient 辐射渐变

从(x,y)向外辐射渐变

![5](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_5.png)


- centerX centerY：辐射中心的坐标 
- radius：辐射半径 
- centerColor：辐射中心的颜色 
- edgeColor：辐射边缘的颜色 
- tileMode：辐射范围之外的着色模式。

## SweepGradient 拖地渐变

像拖把一样拖出渐变。。

![6](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_6.png)



- cx cy ：拖把的中心 
- color0：拖把的起始颜色 
- color1：拖把的终止颜色

![7](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_7.png)
![8](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_8.png)
![9](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_9.png)




这三个图分别对性线性渐变，辐射渐变，sweep渐变。

## BitmapShader

以bitmap作为填充元素

![10](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_10.png)

 
for example：

```java
Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.batman);
        paint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        canvas.drawCircle(200, 200, 200, paint);
```

![11](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_11.png)


## ComposeShader

着色混合器，将两个shader组合起来展示

```java
        Bitmap bitmap1= BitmapFactory.decodeResource(getResources(), R.drawable.batman);

        Bitmap bitmap2= BitmapFactory.decodeResource(getResources(), R.drawable.batman_logo);

        Shader shader1 = new BitmapShader(bitmap1, Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);

        Shader shader2 = new BitmapShader(bitmap2, Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);

        Shader shader = new ComposeShader(shader1,shader2, PorterDuff.Mode.DST_ATOP);

        paint.setShader(shader);

```

![12](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_12.png)


关于PorterDuff.Mode有十七种之多，详情请见[谷歌官方文档](https://developer.android.com/reference/android/graphics/PorterDuff.Mode)

![13](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_13.png)

# Xfermode

```java
Xfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
```


还是PorterDuff.Mode，PorterDuff.Mode在paint一共有3个API：

|类| 作用 |
| ---------- | -------------------- |
| ComposeShader | 将两个Shader混合 |
| PorterDuffColorFilter | 的作用是使用一个指定的颜色和一种指定的 PorterDuff.Mode 来与绘制对象进行合成| 
| Xfermode | 设置绘制内容和View中已有内容的混合计算方式。 |


```java
setLayerType(View.LAYER_TYPE_SOFTWARE, null);//关闭硬件加速
  canvas.drawBitmap(bitmap1, 0, bitmap1.getHeight() + 20, paint);//绘制第一个Bitmap
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));//设置Mode.DST_OUT
        canvas.drawBitmap(bitmap2, 0, bitmap1.getHeight() + 20, paint);绘制第二个Bitmap
        paint.setXfermode(null);//置空Mode
```


Xfermode 注意事项
1. 使用离屏缓冲（Off-screen Buffer）

```java
int saved = canvas.saveLayer(null, null, Canvas.ALL_SAVE_FLAG);


canvas.drawBitmap(rectBitmap, 0, 0, paint); // 画方
paint.setXfermode(xfermode); // 设置 Xfermode
canvas.drawBitmap(circleBitmap, 0, 0, paint); // 画圆
paint.setXfermode(null); // 用完及时清除 Xfermode


canvas.restoreToCount(saved);
```

![14](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_14.jpg)


2. 控制好透明区域
使用 Xfermode 来绘制的内容，除了注意使用离屏缓冲，还应该注意控制它的透明区域不要太小，要让它足够覆盖到要和它结合绘制的内容，否则得到的结果很可能不是你想要的。

![15](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_15.jpg)



# 线条形状

## setStrokeWidth(float width)

```java
paint.setStyle(Paint.Style.STROKE);  
paint.setStrokeWidth(1);  
canvas.drawCircle(150, 125, 100, paint);  
paint.setStrokeWidth(5);  
canvas.drawCircle(400, 125, 100, paint);  
paint.setStrokeWidth(40);  
canvas.drawCircle(650, 125, 100, paint); 
```

![16](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_16.png)


## setStrokeCap(Paint.Cap cap)

线头形状有三种：BUTT 平头、ROUND 圆头、SQUARE 方头。默认为 BUTT。

![17](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_17.jpg)


## setStrokeJoin(Paint.Join join)

MITER 尖角、 BEVEL 平角和 ROUND 圆角。默认为 MITER

![18](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_18.jpg)

# setPathEffect(PathEffect effect)

## DashPathEffect(float intervals[], float phase)

画虚线：

```java
 PathEffect pathEffect = new DashPathEffect(new float[]{10, 5}, 10);
        paint.setPathEffect(pathEffect);
        canvas.drawPath(path, paint);
```        

## CornerPathEffect(float radius)
将拐角变为圆角:

```java
PathEffect pathEffect = new CornerPathEffect(20);  
paint.setPathEffect(pathEffect);
canvas.drawPath(path, paint);  
```

## DiscretePathEffect(float segmentLength, float deviation) 

使线段随机偏移:

```java
  DiscretePathEffect discretePathEffect = new DiscretePathEffect(20,20);
        paint.setPathEffect(discretePathEffect);
        canvas.drawPath(path, paint);
```

## PathDashPathEffect(Path shape, float advance, float phase,tyle style)

使用path来绘制一段“虚线”
shape 参数是用来绘制的 Path ； advance 是两个相邻的 shape 段之间的间隔，不过注意，这个间隔是两个 shape 段的起点的间隔，而不是前一个的终点和后一个的起点的距离； phase 和 DashPathEffect 中一样，是虚线的偏移；最后一个参数 style，是用来指定拐弯改变的时候 shape 的转换方式。style 的类型为 PathDashPathEffect.Style ，是一个 enum ：

```java
  public enum Style {
        TRANSLATE(0),   //!< translate the shape to each position
        ROTATE(1),      //!< rotate the shape about its center
        MORPH(2);       //!< transform each point, and turn lines into curves
        
        Style(int value) {
            native_style = value;
        }
        int native_style;
    }
```
  
-  TRANSLATE：位移
-  ROTATE：旋转
-  MORPH：变体


![19](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_19.jpg)

## SumPathEffect(PathEffect first, PathEffect second)
两种PathEffect先后混合

## ComposePathEffect(PathEffect outerpe, PathEffect innerpe)
这也是一个组合效果类的 PathEffect 。不过它是先对目标 Path 使用一个 PathEffect，然后再对这个改变后的 Path 使用另一个 PathEffect

## setShadowLayer(float radius, float dx, float dy, int shadowColor)
在之后的绘制内容下面加一层阴影。

radius 是阴影的模糊范围； dx dy 是阴影的偏移量； shadowColor 是阴影的颜色。

- 在硬件加速开启的情况下， setShadowLayer() 只支持文字的绘制，文字之外的绘制必须关闭硬件加速才能正常绘制阴影。
- 如果 shadowColor 是半透明的，阴影的透明度就使用 shadowColor 自己的透明度；而如果 shadowColor 是不透明的，阴影的透明度就使用 paint 的透明度。

# setMaskFilter(MaskFilter maskfilter)
为之后的绘制设置 MaskFilter。上一个方法 setShadowLayer() 是设置的在绘制层下方的附加效果；而这个 MaskFilter 和它相反，设置的是在绘制层上方的附加效果。

 radius 参数是模糊的范围， style 是模糊的类型。一共有四种：
  ● NORMAL: 内外都模糊绘制
  ● SOLID: 内部正常绘制，外部模糊
  ● INNER: 内部模糊，外部不绘制
  ● OUTER: 内部不绘制，外部模糊

![19](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/draw_pics_2/draw_20.jpg)

