
**measure过程决定了View的宽高，Measure完成之后，可以通过getMeasureWidth和getMeasureHeight方法来获取View测量之后的宽高，在几乎所有情况下，它都等同于view的最终宽高，但是特殊情况除外。layout过程决定了view四个顶点的坐标和view实际的宽高。完成后可通过getTop,getBottom,getLeft,getRight来拿到四个顶点的位置，并且可以通过getWeight和getWidth方法来拿到View的最终宽高。Draw过程决定了View的最终显示。只有Draw方法完成后，view才展示在屏幕上。**



DecorView作为顶级View，一般情况下上面是title，下面是content，我们所设置的setContentView就是加入到content中的。所以，获取content可以通过
*ViewGroup content=findViewById(R.android.id.content)* 拿到，而我们所设置的View，可以通过content.getChildAt(0)获取。

![content](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/measure_one.png)

MeasureSpec:
MeasureSpec代表一个32位的int值，高两位代表SpecMode，低30位表示SpecSize。

```java
  private static final int MODE_SHIFT = 30;
        private static final int MODE_MASK  = 0x3 << MODE_SHIFT;
        /**
         * Measure specification mode: The parent has not imposed any constraint
         * on the child. It can be whatever size it wants.
         */
        public static final int UNSPECIFIED = 0 << MODE_SHIFT;

        /**
         * Measure specification mode: The parent has determined an exact size
         * for the child. The child is going to be given those bounds regardless
         * of how big it wants to be.
         */
        public static final int EXACTLY     = 1 << MODE_SHIFT;

        /**
         * Measure specification mode: The child can be as large as it wants up
         * to the specified size.
         */
        public static final int AT_MOST     = 2 << MODE_SHIFT;
```

三种测量规格：

- UNSPECIFIED：
父view不对子view做任何限制，要多大给多大。

- EXACTLY：
父容器已经测出子View所需的精确大小，这个时候View的最终大小就是SpecSize。对应LayoutParams中的match_parent和具体数值这两种模式。

- AT_MOST：
父容器给了一个大小范围，View的大小不能大于这个值，具体多少，得看不同View的表现形式。对应LayoutParams中的wrap_content。


----


>系统内部是通过MeasureSpec来进行View的测量。我们也可以给View设置LayoutParams，在View测量的时候，系统会将LayoutParams转换成MeasureSpec，然后再根据MeasureSpec来确定View测量后的宽高。LayoutParams需要和父容器一起，才能决定View的MeasureSpec。对于DecorView和普通View来说，转换的策略略有不同。对于DecorView，MeasureSpec是由窗口尺寸和自身LayoutParams来共同决定，对于普通View，MeasureSpec由父容器的MeasureSpec和自身LayoutParams来共同决定，MeasureSpec确定了。就可以在onMeasure中确定View的测量后宽高。

```java
   private boolean measureHierarchy(final View host, final WindowManager.LayoutParams lp,
            final Resources res, final int desiredWindowWidth, final int desiredWindowHeight) {
//忽略代码....
            childWidthMeasureSpec = getRootMeasureSpec(desiredWindowWidth, lp.width);
            childHeightMeasureSpec = getRootMeasureSpec(desiredWindowHeight, lp.height);
            performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
//忽略代码....
    }
```

```java
  private static int getRootMeasureSpec(int windowSize, int rootDimension) {
        int measureSpec;
        switch (rootDimension) {
        case ViewGroup.LayoutParams.MATCH_PARENT:
            // Window can't resize. Force root view to be windowSize.
            measureSpec = MeasureSpec.makeMeasureSpec(windowSize, MeasureSpec.EXACTLY);
            break;
        case ViewGroup.LayoutParams.WRAP_CONTENT:
            // Window can resize. Set max size for root view.
            measureSpec = MeasureSpec.makeMeasureSpec(windowSize, MeasureSpec.AT_MOST);
            break;
        default:
            // Window wants to be an exact size. Force root view to be that size.
            measureSpec = MeasureSpec.makeMeasureSpec(rootDimension, MeasureSpec.EXACTLY);
            break;
        }
        return measureSpec;
    }
    
```
根据如上代码，DecorView的measureSpec生产过程总结如下：

1. 如果是ViewGroup.LayoutParams.MATCH_PARENT：则为MeasureSpec.EXACTLY，大小为窗口大小。
2. 如果是ViewGroup.LayoutParams.WRAP_CONTENT：则为MeasureSpec.AT_MOST，大小不能超过窗口大小。
3. 如果是exact size，则为MeasureSpec.EXACTLY，大小为LayoutParams中指定的大小。

对于布局中的View，View的measure过程由ViewGroup传递而来：

```java
   protected void measureChildWithMargins(View child,
            int parentWidthMeasureSpec, int widthUsed,
            int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin
                        + heightUsed, lp.height);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
```

在调用子元素measure之前会通过getChildMeasureSpec来确定子元素的MeasureSpec，子元素的MeasureSpec的创建与父容器的MeasureSpec和自身LayoutParams有关，并且还和自身margin，pandding有关：

```java
  public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
        int specMode = MeasureSpec.getMode(spec);
        int specSize = MeasureSpec.getSize(spec);
        int size = Math.max(0, specSize - padding);
        int resultSize = 0;
        int resultMode = 0;
        switch (specMode) {
        // Parent has imposed an exact size on us
        case MeasureSpec.EXACTLY:
            if (childDimension >= 0) {
                resultSize = childDimension;
                resultMode = MeasureSpec.EXACTLY;
            } else if (childDimension == LayoutParams.MATCH_PARENT) {
                // Child wants to be our size. So be it.
                resultSize = size;
                resultMode = MeasureSpec.EXACTLY;
            } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                // Child wants to determine its own size. It can't be
                // bigger than us.
                resultSize = size;
                resultMode = MeasureSpec.AT_MOST;
            }
            break;
        // Parent has imposed a maximum size on us
        case MeasureSpec.AT_MOST:
            if (childDimension >= 0) {
                // Child wants a specific size... so be it
                resultSize = childDimension;
                resultMode = MeasureSpec.EXACTLY;
            } else if (childDimension == LayoutParams.MATCH_PARENT) {
                // Child wants to be our size, but our size is not fixed.
                // Constrain child to not be bigger than us.
                resultSize = size;
                resultMode = MeasureSpec.AT_MOST;
            } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                // Child wants to determine its own size. It can't be
                // bigger than us.
                resultSize = size;
                resultMode = MeasureSpec.AT_MOST;
            }
            break;
        // Parent asked to see how big we want to be
        case MeasureSpec.UNSPECIFIED:
            if (childDimension >= 0) {
                // Child wants a specific size... let him have it
                resultSize = childDimension;
                resultMode = MeasureSpec.EXACTLY;
            } else if (childDimension == LayoutParams.MATCH_PARENT) {
                // Child wants to be our size... find out how big it should
                // be
                resultSize = View.sUseZeroUnspecifiedMeasureSpec ? 0 : size;
                resultMode = MeasureSpec.UNSPECIFIED;
            } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                // Child wants to determine its own size.... find out how
                // big it should be
                resultSize = View.sUseZeroUnspecifiedMeasureSpec ? 0 : size;
                resultMode = MeasureSpec.UNSPECIFIED;
            }
            break;
        }
        //noinspection ResourceType
        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }
    
```

简而言之就是根据父容器的MeasureSpec同时结合View自身的LayoutParams来确定子元素的MeasureSpec。子元素的可用大小为父容器尺寸减去padding：
int size = Math.max(0, specSize - padding);

![汇总图](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/measure_two.png)






## view对于自身的测量过程


如果一个View那么通过measure方法就可以完成其测量过程。如果是一个ViewGroup的话，除了完成自身测量，也要遍历自身子view的measure方法。各个子元素再递归完成这个流程。

### View：

view的measure方法是一个final方法，子类不能重写，在measure方法中，又会调用
     onMeasure(widthMeasureSpec, heightMeasureSpec);我们来看：

```java  
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }
    
```
setMeasuredDimension方法会设置View的宽高测量值，所以我们只需看

```java
    public static int getDefaultSize(int size, int measureSpec) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
        case MeasureSpec.UNSPECIFIED:
            result = size;
            break;
        case MeasureSpec.AT_MOST:
        case MeasureSpec.EXACTLY:
            result = specSize;
            break;
        }
        return result;
    }
```

很简单，如果是MeasureSpec.UNSPECIFIED的话return上文中的，getSuggestedMinimumWidth()。而AT_MOST和EXACTLY的话return测量后的specSize。specSize最终是在layout的时候决定的。绝大部分情况下，specSize和最终大小是相等的。而getSuggestedMinimumWidth()是什么呢，我们接着看：

```java
 protected int getSuggestedMinimumWidth() {
        return (mBackground == null) ? mMinWidth : max(mMinWidth, mBackground.getMinimumWidth());
    }
```    

很好理解了，如果没有背景的话，就是mMinWidth：对应android：minWidth。如果有背景的话取mBackground.getMinimumWidth()和mMinWidth中的较大者。前提是这个drawable有原始宽度。什么时候有原始宽度，可以简单的先记成：ShapeDrawable没有，BitmapDrawable有。

### ViewGroup:

ViewGroup除了自身measure，还要对子元素(们)的measure方法，各个子元素再去循环递归执行这个过程。ViewGroup是一个抽象类，且没有重写onMeasure。但是提供了两个方法:measureChildren和measureChild。

```java
    protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
        final int size = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < size; ++i) {
            final View child = children[i];
            if ((child.mViewFlags & VISIBILITY_MASK) != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
            }
        }
    }
```
```java 
   protected void measureChild(View child, int parentWidthMeasureSpec,
            int parentHeightMeasureSpec) {
        final LayoutParams lp = child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                mPaddingLeft + mPaddingRight, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                mPaddingTop + mPaddingBottom, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
```

measureChild就是取出child的layoutparams，然后通过getCHildMeasureSpec去创建子元素的MeasureSpec，然后将MeasureSpec和layoutparams传给view的measure方法进行测量。因为各个ViewGroup的测量方式，细节不同。所以ViewGroup的onMeasure方法由子类去实现。








