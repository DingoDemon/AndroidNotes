**HashMap作为我们每天都在用的数据结构，我们没有理解对其没有清晰的理解认知，在Java1.8上，对HashMap进行了优化，(碰撞导致链表过长(大于等于TREEIFY_THRESHOLD)，将链表转换成红黑树)。此文中的版本为/Android/sdk/platforms/android-25/android.jar!/java/util/HashMap.class 相对来说会简单很多**


分析HashMap之前，我设想几个问题，这几个问题是不去看源码不太明了的：

1. HashMap的初始容量以及如何扩容的？
2. Entry在HashMap中是如何存放的。
3. 新增Entry如果发生hash冲突的话，这个新增Entry是存放在那里的？
4. HashMap能否存放Key or Value为null的Entry。
5. 扩容标准是根据什么来决定的。



我们先来看HashMap的构造方法：


```java
  public HashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

  public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        } else if (initialCapacity < DEFAULT_INITIAL_CAPACITY) {
            initialCapacity = DEFAULT_INITIAL_CAPACITY;
        }

        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
threshold = initialCapacity;
        init();
    }
```
//这里threshold被赋值成initialCapacity（此版本为4）

这里注意两个参数：
initialCapacity(DEFAULT_INITIAL_CAPACITY ~ MAXIMUM_CAPACITY )和loadFactor(DEFAULT_LOAD_FACTOR)初始容量和负载因子，正好引出HashMap的几个重要字段:

- transient HashMapEntry<K,V>[] table = (HashMapEntry<K,V>[]) EMPTY_TABLE;//初始为空的存放HashMapEntry的数组。

- transient int size;//HashMap实际储存健值对的个数。

- int threshold;//阈值，初识等于initialCapacity。threshold一般为 capacity*loadFactory。HashMap在进行扩容时需要参考threshold，后面会详细谈到

- final float loadFactor;负载因子，代表了table的填充度有多少，默认是0.75

//用于快速失败，由于HashMap非线程安全，在对HashMap进行迭代时，如果期间其他线程的参与导致HashMap的结构发生变化了（比如put，remove等操作），需要抛出异常ConcurrentModificationException

- transient int modCount;

![1](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/xiuluo_juju_one.png)
![2](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/xiuluo_juju_two.png)
![3](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/xiuluo_juju_three.png)



HashMap的主干，是一个存放HashMapEntry的数组。在下文中我们用table(变量名)来称呼它。在常规构造HashMap时，未给table分配空间。所以，我们不难推测出HashMap在put的时候才构建真正的table数组：

```java
  public V put(K key, V value) {
        if (table == EMPTY_TABLE) {
            inflateTable(threshold);① 
        }
        if (key == null)
            return putForNullKey(value);②
        int hash = hash(key); ③
        int i = indexFor(hash, table.length); ④
        for (HashMapEntry<K,V> e = table[i]; e != null; e = e.next) {⑤
            Object k;
            if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
                V oldValue = e.value;
                e.value = value;
                e.recordAccess(this);
                return oldValue;
            }
        }
        modCount++;
        addEntry(hash, key, value, i); ⑥
        return null;
    }
```


## point one：
如果table为空的话，为table分配储存空间。inflateTable这个方法值得一看：

```java
    private void inflateTable(int toSize) {
      
        int capacity = roundUpToPowerOf2(toSize);//1

        float thresholdFloat = capacity * loadFactor;
        if (thresholdFloat > MAXIMUM_CAPACITY + 1) {
            thresholdFloat = MAXIMUM_CAPACITY + 1;
        }

        threshold = (int) thresholdFloat;
        table = new HashMapEntry[capacity];
    }
```

roundUpToPowerOf2这个方法很有意思：

```java
 private static int roundUpToPowerOf2(int number) {
        // assert number >= 0 : "number must be non-negative";
        int rounded = number >= MAXIMUM_CAPACITY
                ? MAXIMUM_CAPACITY
                : (rounded = Integer.highestOneBit(number)) != 0
                    ? (Integer.bitCount(number) > 1) ? rounded << 1 : rounded
                    : 1;

        return rounded;
    }
```

 Integer.highestOneBit方法设计得很精髓，先将数字右移或等5次，使之最高位以及之后的每一位，都是1。然后再减去无符号右移一次之后的值。得到最高位为1，之后所有位均为0的数字。即它最靠近的比它小的2的N次方的数字。

> 简而言之
> 
> - 如果一个数是0, 则返回0；
> - 如果是负数, 则返回 -2147483648：
> - 如果是正数, 返回的则是跟它最靠近的比它小的2的N次方

Integer.bitCount这个方法返回二进制中1的个数。如果大于1的话，则证明不为2的N次幂。

这段代码简而言之就是：

*number如果比MAXIMUM_CAPACITY还大，滚，就MAXIMUM_CAPACITY。*

*然后寻找最靠近number的比它小的2的N次方的数字。如果为0的话，返回1，不为0的话：*

*看number是不是2的幂，是的话，返回他，不是的话，将最靠近number的比它小的2的N次方的数字乘2，然后返回这个数字。*

*阈值threshold 赋值为(int) capacity 乘以 loadFactor 。即最大容量乘以负载因子。
至此，为空table分配空间完成。*

## point two:

```java
  private V putForNullKey(V value) {
        for (HashMapEntry<K,V> e = table[0]; e != null; e = e.next) {
            if (e.key == null) {
                V oldValue = e.value;
                e.value = value;
                e.recordAccess(this);
                return oldValue;
            }
        }
        modCount++;
        addEntry(0, null, value, 0);
        return null;
    }
```

是在talbe[0]的链表中查找key为null的元素，找到了就将新的value赋值给这个元素旧value，并返回原来的value。
如果上面for循环没找到则将这个元素添加到talbe[0]。记住addEntry这个方法，我们接下来会分析。


## point three:

计算key的hash，hash是一个神奇的方法，每个版本实现不同，对key的hashcode进一步进行计算以及二进制位的调整等来保证最终获取的存储位置尽量分布均匀。

## point four: 

根据hash和主干长度来计算储存位置：
static int indexFor(int h, int length) {
        return h & (length-1);
    }

我们接下来会得到table长度一定为2的N次幂的结论，我们先借用这个结论来分析：
length -1 的话，则得到所有位全为1的二进制数。如果h比length-1小的话，会得到h，如果h比length -1大的话：通过&计算，一定会得到一个比length -1 小的数字。通过这个方法来确定Entry在主干中的位置

![图片1](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/hash_one.png)





## point five：

key是无限的，计算出来的h(hash)是有限的。是势必会出现hash冲突。所以，如果在主干上的每一个位置就是一个链表，如果位置上有Entry了，遍历链表(HashMapEntry<K,V> e = table[i]; e != null; e = e.next)，如果(e.hash == hash && ((k = e.key) == key || key.equals(k))),将旧值替换成新值，并返回。


## ponit six：

主干上位置为空，太好了。直接addEntry：

```java
   void addEntry(int hash, K key, V value, int bucketIndex) {
        if ((size >= threshold) && (null != table[bucketIndex])) {
            resize(2 * table.length);
            hash = (null != key) ? sun.misc.Hashing.singleWordWangJenkinsHash(key) : 0;
            bucketIndex = indexFor(hash, table.length);
        }
        createEntry(hash, key, value, bucketIndex);
    }
```

put方法，就已经分析完毕了。我们可以看到扩容是在addEntry的时候进行的resize();我们接着往下面走：

```java
   void addEntry(int hash, K key, V value, int bucketIndex) {
        if ((size >= threshold) && (null != table[bucketIndex])) {①
            resize(2 * table.length);② 
            hash = (null != key) ? sun.misc.Hashing.singleWordWangJenkinsHash(key) : 0;
            bucketIndex = indexFor(hash, table.length);
        }
        createEntry(hash, key, value, bucketIndex);③
    }
```

#### little point one:

如果主干上这个位置不为空的话并且，size已经大于阈值了。就得扩容了。

#### little point two:

```java
  void resize(int newCapacity) {
        HashMapEntry[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }
        HashMapEntry[] newTable = new HashMapEntry[newCapacity];
        transfer(newTable);
        table = newTable;
        threshold = (int)Math.min(newCapacity * loadFactor, MAXIMUM_CAPACITY + 1);
    }
```

-  如果旧的容量已经是MAXIMUM_CAPACITY，阈值只能给你Integer.MAX_VALUE，然后滚。
-  新建一个newTable，然后将老的数据拷贝过去

```java
   void transfer(HashMapEntry[] newTable) {
        int newCapacity = newTable.length;
        for (HashMapEntry<K,V> e : table) {
            while(null != e) {
                HashMapEntry<K,V> next = e.next;
                int i = indexFor(e.hash, newCapacity);
                e.next = newTable[i];
                newTable[i] = e;
                e = next;
            }
        }
    }
```
注意    int i = indexFor(e.hash, newCapacity);
                e.next = newTable[i]; 这两句话。
              
 ![图片2](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/hash_two.png)


如图所示我们在resize的时候，不需要重新定位，只需要看看原来的hash值新增的那个bit是1还是0就好了，是0的话位置没变，是1的话位置变成“原位置+oldCap”。




#### little point three:
```java
 void createEntry(int hash, K key, V value, int bucketIndex) {
        HashMapEntry<K,V> e = table[bucketIndex];
        table[bucketIndex] = new HashMapEntry<>(hash, key, value, e);
        size++;
    }
```

很简单了。不分析了。


至此开头提出的5个问题，已经可以全部回答了。
 
---------

存看完了，我们来看取：

```java
  public V get(Object key) {
        if (key == null)
            return getForNullKey();
        Entry<K,V> entry = getEntry(key);

        return null == entry ? null : entry.getValue();
    }
```

key为空的话，去取table[0]：

```java
for (HashMapEntry<K,V> e = table[0]; e != null; e = e.next) {
            if (e.key == null)
                return e.value;
        }
```

否则的话：

```java
  final Entry<K,V> getEntry(Object key) {
        if (size == 0) {
            return null;
        }

        int hash = (key == null) ? 0 : hash(key);
        for (HashMapEntry<K,V> e = table[indexFor(hash, table.length)];
             e != null;
             e = e.next) {
            Object k;
            if (e.hash == hash &&
                ((k = e.key) == key || (key != null && key.equals(k))))
                return e;
        }
        return null;
    }
```

可以看出取很简单，先根据hash()确定table中的index，如果没有找到的话就去e.next。


-----
Btw：
关于HashMap的遍历：


```java 
for (Entry<String, String> entry : map.entrySet()) {

entry.getKey();	entry.getValue();}
```

```java
Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
while (iterator.hasNext()) {
	Map.Entry<String, String> entry = iterator.next();
	entry.getKey();
	entry.getValue();
}
```

```java
for (String key : map.keySet()) {
	map.get(key);
}
```

```java
for (Entry<String, String> entry : entrySet) {
	entry.getKey();
	entry.getValue();
}

```

