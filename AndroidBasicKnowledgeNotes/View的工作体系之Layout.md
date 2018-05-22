**layout的作用是ViewGroup来确定子元素的位置，当ViewGroup的位置固定后，他会在onLayout中遍历所有子元素并调用它的layout方法。在layout方法中又会调用onLayout方法。layout方法确认View自身的位置，onLayout则会确认所有子元素的位置。我们先来看layout的源码：**

```java
 public void layout(int l, int t, int r, int b) {
      //忽略代码....

        int oldL = mLeft;
        int oldT = mTop;
        int oldB = mBottom;
        int oldR = mRight;

        boolean changed = isLayoutModeOptical(mParent) ?
                setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);

        if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
            onLayout(changed, l, t, r, b);
            mPrivateFlags &= ~PFLAG_LAYOUT_REQUIRED;

     //忽略代码....
      
    }

```

isLayoutModeOptical这个方法是判断是否有光学边界，我们暂且不用去关心

```
    protected boolean setFrame(int left, int top, int right, int bottom) {
        boolean changed = false;
 //忽略代码...
       if (mLeft != left || mRight != right || mTop != top || mBottom != bottom) {
            changed = true;
            // Remember our drawn bit
            int drawn = mPrivateFlags & PFLAG_DRAWN;

            int oldWidth = mRight - mLeft;
            int oldHeight = mBottom - mTop;
            int newWidth = right - left;
            int newHeight = bottom - top;
            boolean sizeChanged = (newWidth != oldWidth) || (newHeight != oldHeight);

            // Invalidate our old position
            invalidate(sizeChanged);

            mLeft = left;
            mTop = top;
            mRight = right;
            mBottom = bottom;
            mRenderNode.setLeftTopRightBottom(mLeft, mTop, mRight, mBottom);

            mPrivateFlags |= PFLAG_HAS_BOUNDS;


            if (sizeChanged) {
                sizeChange(newWidth, newHeight, oldWidth, oldHeight);
            }

  //忽略代码...
    }
```

layout方法首先通过setFram方法来设置4个顶点的位置。即初始化mLeft，mTop，mRight，mBottom。这四个点确认了，在父布局中的位置也确认了。接着会调用 onLayout(changed, l, t, r, b)，onLayout是父容器确认子View位置的方法，实现细节和具体需求有关，所以View和ViewGroup比如我们来看下LinearLayout的onLayout(竖直排列)：


```java
 void layoutVertical(int left, int top, int right, int bottom) {
 //忽略代码...
        
 
        for (int i = 0; i < count; i++) {
            final View child = getVirtualChildAt(i);
            if (child == null) {
                childTop += measureNullChild(i);
            } else if (child.getVisibility() != GONE) {
                final int childWidth = child.getMeasuredWidth();
                final int childHeight = child.getMeasuredHeight();
                
                final LinearLayout.LayoutParams lp =
                        (LinearLayout.LayoutParams) child.getLayoutParams();
                
                int gravity = lp.gravity;
                if (gravity < 0) {
                    gravity = minorGravity;
                }
                final int layoutDirection = getLayoutDirection();
                final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = paddingLeft + ((childSpace - childWidth) / 2)
                                + lp.leftMargin - lp.rightMargin;
                        break;

                    case Gravity.RIGHT:
                        childLeft = childRight - childWidth - lp.rightMargin;
                        break;

                    case Gravity.LEFT:
                    default:
                        childLeft = paddingLeft + lp.leftMargin;
                        break;
                }

                if (hasDividerBeforeChildAt(i)) {
                    childTop += mDividerHeight;
                }

                childTop += lp.topMargin;
                setChildFrame(child, childLeft, childTop + getLocationOffset(child),
                        childWidth, childHeight);
                childTop += childHeight + lp.bottomMargin + getNextLocationOffset(child);

                i += getChildrenSkipCount(child, i);
            }
        }
    }
```

代码逻辑大概就是遍历所有子view来确定位置，其中childTop会不停增大，后面的view放在之前view的下边。setChildFrame也只是调用了view的layout方法：

```java
 private void setChildFrame(View child, int left, int top, int width, int height) {        
        child.layout(left, top, left + width, top + height);
    }
```
我们来看setChildFrame的是：final int childWidth = child.getMeasuredWidth();
                final int childHeight = child.getMeasuredHeight();

我们来关注为什么view的最终大小是在layout的时候才决定的：

>
>```java
> public final int getWidth() {
>        return mRight - mLeft;
>   }
>```
>
>
>```java
>public final int getMeasuredWidth() {
>        return mMeasuredWidth & MEASURED_SIZE_MASK;
>    }
>```

如果在重写onLayout的时候：

super.layout(l+100,top+100,right+100,bottom+100）
那么最终大小比测量大小的长宽，都会多(100+100)px。还有就是view可能会measure多次，测量结果也会随之修改多次

