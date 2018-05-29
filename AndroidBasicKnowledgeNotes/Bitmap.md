
Bitmap是Android系统中的用来对图像处理的类。用它可以获取图像文件信息，进行图像剪切、旋转、缩放等操作，并可以指定格式保存图像文件。

Bitmap的构造方法是package的，只能通过Bitmap.createBitmap()方法或者BitmapFactory类来构造Bitmap对象。

createBitmap方法:

![createBitmap](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/bitmap/bitmap_1.png?raw=true)

BitmapFactory这个类，就是用于我们构造Bitmap对象的：

![BitmapFactory](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/bitmap/bitmap_2.png?raw=true)

# BitmapFactory

BitmapFactory中的构造方法:

- public static Bitmap decodeFile(String pathName, Options opts) 　//从文件读取图片
- public static Bitmap decodeFile(String pathName)

- public static Bitmap decodeStream(InputStream is)　 //从输入流读取图片
- public static Bitmap decodeStream(InputStream is, Rect outPadding, Options opts)

- public static Bitmap decodeResource(Resources res, int id)　 //从资源文件读取图片
- public static Bitmap decodeResource(Resources res, int id, Options opts)

- public static Bitmap decodeByteArray(byte[] data, int offset, int length)　 //从数组读取图片
- public static Bitmap decodeByteArray(byte[] data, int offset, int length, Options opts)

public static Bitmap decodeFileDescriptor(FileDescriptor fd)　//从文件读取文件 与decodeFile不同的是这个直接调用JNI函数进行读取 效率比较高
public static Bitmap decodeFileDescriptor(FileDescriptor fd, Rect outPadding, Options opts)

decodeResource相关方法会根据res所放的文件夹(hdpi,xhdpi,xxhdpi等)，对BitMap进行缩放。

![BitmapFactory](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/bitmap/bitmap_3.png?raw=true)

```java
public static Bitmap decodeResourceStream(Resources res, TypedValue value,
            InputStream is, Rect pad, Options opts) {
        validate(opts);
        if (opts == null) {
            opts = new Options();//创建Options
        }

        if (opts.inDensity == 0 && value != null) {
            final int density = value.density;
            if (density == TypedValue.DENSITY_DEFAULT) {
                opts.inDensity = DisplayMetrics.DENSITY_DEFAULT;//设置默认值 160
            } else if (density != TypedValue.DENSITY_NONE) {
                opts.inDensity = density;//
            }
        }
        
        if (opts.inTargetDensity == 0 && res != null) {
            opts.inTargetDensity = res.getDisplayMetrics().densityDpi;//赋值为当前设备 densityDpi
        }
        
        return decodeStream(is, pad, opts);
    }
```

在加载图片的时候，我们可以通过BitmapFactory.Options来控制读入的图片属性：

![BitmapFactory.Options](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/bitmap/bitmap_4.png?raw=true)

## BitmapFactory.Options

我们来具体分析这些参数的作用：

### inMutable

设置是否可变，如果设置true的话，将返回一个可变位图

### inBitmap:

在Android 3.0 引进了BitmapFactory.Options.inBitmap. 如果这个值被设置了，decode方法会在加载内容的时候去重用已经存在的bitmap. 这意味着bitmap的内存是被重新利用的，这样可以提升性能, 并且减少了内存的分配与回收。然而，使用inBitmap有一些限制。特别是在Android 4.4 之前，只支持同等大小的位图。Android 4.4开始只要旧bitmap的尺寸大于等于新的bitmap就可以复用了。并且该Bitmap必须是mutable，且返回的Bitmap也是

### mutable

bitmap内存复用大致分两步：1、不用的bitmap用软引用保存起来，以备复用；2、使用前面保存的bitmap来创建新的bitmap。

### inJustDecodeBounds：

如果设置为true,将不返回bitmap, 但是Bitmap的outWidth,outHeight等属性将会赋值,允许调用查询Bitmap,而不需要为Bitmap分配内存.这个在加载大图的时候配合下文inSampleSize会用到

### inSampleSize：

这个平时经常用到：
> If set to a value > 1, requests the decoder to subsample the original image, returning a smaller image to save memory

inSampleSize的默认值和最小值为1（当小于1时，解码器将该值当做1来处理），且在大于1时，该值只能为2的幂（当不为2的幂时，解码器会取与该值最接近的2的幂）。例如，当inSampleSize为2时，长 = 长/2，宽 = 宽/2

###  inPreferredConfig

解码时配置参数
inPreferredConfig有四个值可以选：
ALPHA_8，RGB_565，ARGB_4444(Deprecated)，ARGB_8888
#### ALPHA_8模式
ALPHA_8模式表示的图片信息中只包含Alpha透明度信息，不包含任何颜色信息，所以ALPHA_8模式只能用在一些特殊场景。
#### RGB_565模式
显然RGB_565模式不能表示所有的RGB颜色，它能表示的颜色数只有32 × 64 × 32 = 65536种，远远小于24位真彩色所能表示的颜色数（256 × 257 × 256 = 16677216）。当图片中某个像素的颜色不在RGB_565模式表示的颜色范围内时，会使用相近的颜色来表示。
#### ARGB_8888模式
ARGB_8888模式用8位来表示透明度，有256个透明度等级，用24位来表示R，G，B三个颜色通道，能够完全表示32位真彩色，但同时这种模式占用的内存空间也最大，是RGB_565模式的两倍，是ALPHA_8模式的4倍。

这个选项是可选的，系统会根据原图来处理，并非一定按代码制定的模式去处理。

### inPremultiplied：

如果设置了true(默认是true)，那么返回的图片RGB都会预乘透明通道A后的颜色，如果设为false在绘制时可能会抛出异常(没试出来。。。)

### inDensity，inTargetDensity，inScreenDensity，inScale
- inDensity——Bitmap的像素密度
- inTargetDensity——Bitmap最终的像素密度
- inScreenDensity——当前屏幕的像素密度
- inScale ——默认为true，是否支持缩放，设置为true时，Bitmap将以inTargetDensity 的值进行缩放；

这四个的值在setDensityFromOptions方法中得到处理：

```java
 private static void setDensityFromOptions(Bitmap outputBitmap, Options opts) {
        if (outputBitmap == null || opts == null) return;
        final int density = opts.inDensity;
        if (density != 0) {
            outputBitmap.setDensity(density);
            final int targetDensity = opts.inTargetDensity;
            if (targetDensity == 0 || density == targetDensity || density == opts.inScreenDensity) {
                return;
            }

            byte[] np = outputBitmap.getNinePatchChunk();
            final boolean isNinePatch = np != null && NinePatch.isNinePatchChunk(np);
            if (opts.inScaled || isNinePatch) {
                outputBitmap.setDensity(targetDensity);
            }
        } else if (opts.inBitmap != null) {
            // bitmap was reused, ensure density is reset
            outputBitmap.setDensity(Bitmap.getDefaultDensity());
        }
    }
```

drawable文件夹后缀对应dpi：


|    后缀   | dpi     |
|--------   |------- |
| (mdpi)    | 160dpi |
| (hdpi)    | 240dpi |
| (xhdpi)   | 320dpi |
| (xxhdpi)  | 490dpi |
| (xxxhdpi) | 640dpi |


当使用decodeResuore()解码drawable目录下的图片时, 会根据手机的屏幕密度,到对应的文件夹中查找图片,如果图片存在于其他目录,则会对该图片进行放缩处理再显示,放缩处理的规则:
(scale= 设备屏幕密度/drawable目录设定的屏幕密度)  -> (scale= inTargetDensity/inDesity;)

屏幕是420dpi
我们尝试将图片放在xhdpi 和xxxhdpi中，然后不做代码修改，加载图片：
![240](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/bitmap/bitmap_5.png?raw=true)
![640](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/bitmap/bitmap_6.png?raw=true)

上面一张图是xhdpi(420/240)，下面一张图是xxxhdpi(420/640)。
*后者options属性：*


![options](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/bitmap/bitmap_7.png?raw=true)


```java
BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = true;
        options.inDensity = 240;
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cat_360, options);
```
如果我们将起inDensity手动修改成240的话，就和上图一样了 。

### outWidth，outHeight：
宽高

### outMimeType：
If known, this string is set to the mimetype of the decoded image.If not known, or there is an error, it is set to null.

![mimetype](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/bitmap/bitmap_8.png?raw=true)

### outConfig：
见上问config。

# BitMap内存优化：
比如，如何加载一张大图:
**(本来做了一张500多mb的图，结果编译都过不了，所以准备了1张银河系的图)**
![big image](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/bitmap/bitmap_9.png?raw=true)


```java
   button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.the_milky_way);
                imageView.setImageBitmap(bitmap);
            }
        });
```
        
按下按钮后：

![oom](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/bitmap/bitmap_10.png?raw=true)

当图片转成Bitmap后，它的大小= mBitmapHeight * mBitmapWidth * 4byte(ARGB_8888)。「差不多300mb」这里迅速抛出了OOM。

但是呢，展示图片的imageView就300dp * 300dp。所以我们完全没有必要加载这么大一张图。在加载图片之前，将bitmap压缩成300*300或者更小即可。
优化代码：

```java
     button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                //这一步decodeResource返回为空，但是options中outHeight，outWidth会被赋值
                BitmapFactory.decodeResource(getResources(), R.drawable.the_milky_way, options);
                //不要担心getWidth()/getHeight()为0，这里重点讨论的不是这个。
                options.inSampleSize = caculateInSampleSize(options, imageView.getWidth(), imageView.getHeight());
                options.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.the_milky_way, options);
                imageView.setImageBitmap(bitmap);
            }
        });
    }
```

```java
    private int caculateInSampleSize(BitmapFactory.Options options, int rWidth, int rHeight) {
//这里可以放子线程去做，计算操作会阻塞主线程
        int inSampleSize = 1;
        int originWidth = options.outWidth;
        int originHeight = options.outHeight;

        while (originWidth > rWidth || originHeight > rHeight) {
            originWidth = originWidth >> 1;
            originHeight = originHeight >> 1;
            inSampleSize = inSampleSize << 1;
        }
        return inSampleSize;
    }
```
![!](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/bitmap/bitmap_11.png?raw=true)


# 关于Bitmap的压缩:
压缩，从目的上来区分，分为两种，一种是读进内存，进行压缩，例如上文中的加载大图，另外一种是，将bitmap导出时进行压缩，常见于上传图片之前。

### 关于后者：
系统提供了很简便的方法：

```java
public boolean compress(CompressFormat format, int quality, OutputStream stream)
```

- format: JPEG,PNG,WEBP
- quality: 质量百分比(0~100)
- stream: 输出流

质量压缩不会减少图片的像素，它是在保持像素的前提下改变图片的位深及透明度，来达到压缩图片的目的，图片的长，宽，像素都不会改变，那么bitmap所占内存大小是不会变的。质量压缩对png没有作用，因为png是无损压缩。

### 关于前者:
上文说过，Bitmap加载进内存后，大小由 长(像素) * 宽(像素) * 每个像素的字节数决定总大小。压缩就得从大小，和携带字节数这两方面着手:

#### 1.字节数：

|    Config  | 明细     |
|--------  |----------------------   |
| ARGB_8888 | 每个像素存储在4个字节。|
| RGB_565   | 每个像素存储在2个字节中，只有RGB通道被编码：红色以5位精度存储（32个可能值），绿色以6位精度存储（64个可能值），蓝色存储为5位精确。 |


将options.inPreferredConfig 赋值为Bitmap.Config.RGB_565


#### 2.长宽：
方法很多了，这里不再赘述，什么matrix，上文中的sampleSize均可









