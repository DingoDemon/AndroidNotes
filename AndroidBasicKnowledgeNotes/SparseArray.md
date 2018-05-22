参考资料：http://extremej.itscoder.com/sparsearray_source_analyse/

别再用HashMap\<Integer,Object\> 了。Sparse[spɑrs]Arrays了解一下：

![warning](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/SparseArray_1.jpg)

SparseArrays map integers to Objects。它里面并没有储存一个一个健值对的Entry，而是维护了key和value的两个数组：int[] mKeys，Object[] mValues。相对于HashMap来说更为节省空间。并且由于key指定为int类型，也可以节省int-Integer的装箱拆箱操作带来的性能消耗。在扩容的时候，hashmap要对链表上每一个entry执行indexFor()方法。而SparseArrays仅仅是copy数组。另外，SparseArray为了提升性能，在删除操作时做了一些优化：在删除的时候，并不是直接remove元素，而是将mValues[i]标记为DELETE，如果将来重用该空间的时候，可以再将该空间从DELETE复制为需要的value。在gc()的时候再去删除所有标记为DELETE的value。


我们现在看下所持有的变量和构造方法：


- private static final Object DELETED = new Object();
- private boolean mGarbage = false;
- private int[] mKeys;
- private Object[] mValues;
- private int mSize;


```java
  public SparseArray() {
        this(10);
    }
    public SparseArray(int initialCapacity) {
        if (initialCapacity == 0) {
            mKeys = EmptyArray.INT;
            mValues = EmptyArray.OBJECT;
        } else {
            mValues = ArrayUtils.newUnpaddedObjectArray(initialCapacity);
            mKeys = new int[mValues.length];
        }
        mSize = 0;
    }
```    
  
很简单，默认初始为10为的的int[]和Object[]两个存放key和value的两个数组。mGarbage表示是否需要gc，DELETED标记需要被gc的value。



我们来看put方法：

```java
  public void put(int key, E value) {
        int i = ContainerHelpers.binarySearch(mKeys, mSize, key);①  
        if (i >= 0) {
            mValues[i] = value;②
        } else {
            i = ~i;③ 
            if (i < mSize && mValues[i] == DELETED) {④ 
                mKeys[i] = key;
                mValues[i] = value;
                return;
            }
            if (mGarbage && mSize >= mKeys.length) {⑤ 
                gc();
                // Search again because indices may have changed.
                i = ~ContainerHelpers.binarySearch(mKeys, mSize, key);
            }

⑥
         mKeys = GrowingArrayUtils.insert(mKeys, mSize, i, key);
         mValues = GrowingArrayUtils.insert(mValues, mSize, i,value);
         mSize++;
        }
    }
```

#### 1.先通过二分查找法去寻找index：

```java
 static int binarySearch(int[] array, int size, int value) {
        int lo = 0;
        int hi = size - 1;
        while (lo <= hi) {
            final int mid = (lo + hi) >>> 1;//这个无符号右移有点皮
            final int midVal = array[mid];

            if (midVal < value) {
                lo = mid + 1;
            } else if (midVal > value) {
                hi = mid - 1;
            } else {
                return mid;  // value found
            }
        }
        return ~lo;  // value not present
    }
```

二分查找去 key 数组中查找要插入的 key，返回索引，如果没找到的话，就返回反位(负数)。注意最后一行代码，当找不到这个值的时候return ~lo，实际上到这一步的时候，理论上lo\==mid\==hi。所以这个位置是最适合插入数据的地方。但是为了让能让调用者既知道没有查到值，又知道索引位置，做了一个取反操作，返回一个负数。这样调用处可以首先通过正负来判断命中，之后又可以通过取反获取索引位置。

#### 2.如果为正数，说明查到了，直接将value替换成新的。

#### 3.再取反，得到应该插入的位置i。

#### 4.如果当前位置被标记成了DELETE且不需要扩容的话，则复用当前空间：

![2](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/SparseArray_2.jpg)


#### 5.如果需要GC，且需要扩容的话。先执行gc。gc后下标i可能发生变化，所以再次用二分查找找到应该插入的位置i



![3](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/SparseArray_3.jpg)

![4](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/SparseArray_4.jpg)



#### 6.开始插入

//插入key（可能需要扩容） 

- mKeys = GrowingArrayUtils.insert(mKeys, mSize, i, key); 

//插入value（可能需要扩容）

-  mValues = GrowingArrayUtils.insert(mValues, mSize, i, value); 

//集合大小递增 

- mSize++;


```java
  public static int[] insert(int[] array, int currentSize, int index, int element) {
        assert currentSize <= array.length;
        if (currentSize + 1 <= array.length) {
            System.arraycopy(array, index, array, index + 1, currentSize - index);
            array[index] = element;
            return array;
        }
        int[] newArray = ArrayUtils.newUnpaddedIntArray(growSize(currentSize));
        System.arraycopy(array, 0, newArray, 0, index);
        newArray[index] = element;
        System.arraycopy(array, index, newArray, index + 1, array.length - index);
        return newArray;
    }
```
    


存看完了，我们来看取：

    public E get(int key) {
        return get(key, null);
    }

    public E get(int key, E valueIfKeyNotFound) {
        int i = ContainerHelpers.binarySearch(mKeys, mSize, key);

        if (i < 0 || mValues[i] == DELETED) {
            return valueIfKeyNotFound;
        } else {
            return (E) mValues[i];
        }
    }

取的时候按照key查询，如果key不存在，返回valueIfKeyNotFound

接下来，我们来看删除：

```java
  public void remove(int key) {
        delete(key);
    }

public void delete(int key) {
        int i = ContainerHelpers.binarySearch(mKeys, mSize, key);

        if (i >= 0) {
            if (mValues[i] != DELETED) {
                mValues[i] = DELETED;
                mGarbage = true;
            }
        }
    }
    
```
可以看出我们在开头所讲到的，并不是直接remove元素，而是将mValues[i]标记为DELETE，如果将来重用该空间的时候，可以再将该空间从DELETE复制为需要的value。

我们一直提到的gc()方法内部是如何执行的呢：


 private void gc() {
        int n = mSize;
        int o = 0;
        int[] keys = mKeys;
        Object[] values = mValues;

        for (int i = 0; i < n; i++) {
            Object val = values[i];

            if (val != DELETED) {//1
                if (i != o) {//2
                    keys[o] = keys[i];//3
                    values[o] = val;//4
                    values[i] = null;//5
                }
                o++;
            }
        }

        mGarbage = false;
        mSize = o;
    }

1. 当前这个 value 不等于 DELETED
2. 如果i != o (i为旧，o为新)
3. 将索引 i 处的 key 赋值给 o 处的key
4. 同时将值也赋值给 o 处
5. 最后将 i 处的值置为空防止内存泄漏

![5](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/SparseArray_5.jpg)




什么时候会出发gc呢，通过源码我们可以总结：
> 1.put (int key, E value)
> 
> 2.size()
> 
> 3.keyAt(int index)
> 
> 4.E valueAt(int index)
> 
> 5.setValueAt(int index, E value)
> 
> 6.indexOfKey(int key)
> 
> 7.indexOfValue(E value)
> 
> 8.append(int key, E value)

可以看出只要跟index有关的方法，都会触发gc

除了SparseArrays以外还有三个思想类似的类可供大家使用：

- SparseIntArray — int:int
- SparseBooleanArray— int:boolean
- SparseLongArray— int:long