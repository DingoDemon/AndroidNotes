Google对这个类的描述为:

A cache that holds strong references to a limited number of values. Each time a value is accessed, it is moved to the head of a queue. When a value is added to a full cache, the value at the end of that queue is evicted and may become eligible for garbage collection.

If your cached values hold resources that need to be explicitly released, override entryRemoved(boolean, K, V, V).

If a cache miss should be computed on demand for the corresponding keys, override create(K). This simplifies the calling code, allowing it to assume a value will always be returned, even when there's a cache miss.

By default, the cache size is measured in the number of entries. Override sizeOf(K, V)

also translate by my 腊鸡 English：

这个缓存，对它大小限制内的缓存的值持有强引用。每次这个缓存中的值被使用的时候，便将其移动到队列顶端，当缓存要满的时候，将队列尾部缓存的值出队并有可能进行垃圾回收。

如果缓存的值需要明确地被释放，重写entryRemoved(boolean, K, V, V).

如果需要的话，重写create(K)方法来返回假定的缓存值，应对key相对应的cache没有找到这种情况。

默认情况下，得重写 sizeOf(K, V)来计算缓存大小

有点绕，反正就是一个健值对的缓存集合，容量将要达到临界值的时候，将使用频率最低的缓存remove出去。

接下来，还是持有变量和构造方法：

- ① private final LinkedHashMap<K, V> map;
- ② private int size;
- ③ private int maxSize;

- ④ private int putCount;
-    private int createCount;
-    private int evictionCount; 
-    private int hitCount;
-    private int missCount;


1. 实际存放缓存的linkedHashMap
2. 实际缓存的总大小
3. 缓存限制大小
4. 从上到下分别是，put的总次数，create的总次数，回收的总次数，命中的总次数，未命中的总次数。在后文中会讲解。

```java
    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<K, V>(0, 0.75f, true);
    }
```

这里我们看到**this.map = new LinkedHashMap<K, V>(0, 0.75f, true)**，如果不熟悉LinkedHashMap的话，可能对最后个参数不太明白。

| 参数         |      意义          |
| -------------- | -------------------- |
| initialCapacity | the initial capacity
| loadFactor      | the load factor
| accessOrder  |  the ordering mode - true for access-order, false for insertion-order

```java
  public LinkedHashMap(int initialCapacity,float loadFactor,boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }
```

测试代码：

```java
  LinkedHashMap<Integer, Integer> linkedHashMap = new LinkedHashMap<Integer, Integer>(0, 0.75f, true);

        linkedHashMap.put(0, 0);

        linkedHashMap.put(1, 1);

        linkedHashMap.put(2, 2);
        linkedHashMap.put(3, 3);
        linkedHashMap.put(4, 4);
        linkedHashMap.put(5, 5);

        linkedHashMap.get(5);
        linkedHashMap.get(4);
        linkedHashMap.get(3);
        linkedHashMap.get(2);
        linkedHashMap.get(1);
        linkedHashMap.get(0);


        for (Map.Entry entry : linkedHashMap.entrySet()) {
            System.out.println("key: " + entry.getKey()+"  value :"+ entry.getValue());
        }
```

输出：

- key: 5  value :5
- key: 4  value :4
- key: 3  value :3
- key: 2  value :2
- key: 1  value :1
- key: 0  value :0


可以看到，最后使用的元素，最后被输出，将其想象成一个队列，最先入队的5就是最后入队，放在尾部的元素。合理利用这一特性正好能满足LRU缓存算法的思想

我们来看put方法:

```java
   public final V put(K key, V value) {

        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        V previous;//先前缓存
        synchronized (this) {
            putCount++;//put计数加一
            size += safeSizeOf(key, value);//实际容量加上sizeOf(K,V)的容量
            previous = map.put(key, value);//获取先前缓存
            if (previous != null) {
                size -= safeSizeOf(key, previous);//如果先前缓存存在，减去先前缓存的size。这里是实际上完成了对相同key，不同value的size的计算。
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, value);//上文中对需要被明确被释放的value的值
        }

        trimToSize(maxSize);//调整大小，见下
        return previous;
    }
```


```java
 public void trimToSize(int maxSize) {
        while (true) {
            K key;
            V value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(getClass().getName()
                            + ".sizeOf() is reporting inconsistent results!");
                }
 //如果缓存大小size小于最大缓存，不需要再删除缓存对象，跳出循环
                if (size <= maxSize) {
                    break;
                }

  //我的sdk里面是这样的。eldest报红，但是很好理解，在网上查到的是 :Map.Entry<K, V> toEvict = map.entrySet().iterator().next();也是相同的原理，取最后使用元素
                Map.Entry<K, V> toEvict = map.eldest();
                if (toEvict == null) {
                    break;
                }
//在map中移除并且减少size
                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= safeSizeOf(key, value);
                evictionCount++;
            }

            entryRemoved(true, key, value, null);
        }
    }
```

当调用LruCache的get()方法获取集合中的缓存对象时，就代表访问了一次该元素，将会更新队列，保持整个队列是按照访问顺序排序。这个更新过程就是在LinkedHashMap中的get()方法中完成的。


```java
   public final V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                hitCount++;
                return mapValue;
            }
            missCount++;
        }

    
------------------------------------------------------------------------

        V createdValue = create(key);
        if (createdValue == null) {
            return null;
        }

        synchronized (this) {
            createCount++;
            mapValue = map.put(key, createdValue);

            if (mapValue != null) {
                // There was a conflict so undo that last put
                map.put(key, mapValue);
            } else {
                size += safeSizeOf(key, createdValue);
            }
        }

        if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue);
            return mapValue;
        } else {
            trimToSize(maxSize);
            return createdValue;
        }
    }
```


这个方法分上下两部分来分析：

上半部分：


```java
      V mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                hitCount++;
                return mapValue;
            }
            missCount++;
        }

```

map.get(key) in LinkedHashMap:


```java
    public V get(Object key) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) == null)
            return null;
        if (accessOrder)
            afterNodeAccess(e);
        return e.value;
    }
```
插入Node后LinkedHashMap自身排序：

```java
 void afterNodeAccess(Node<K,V> e) { // move node to last
        LinkedHashMapEntry<K,V> last;
        if (accessOrder && (last = tail) != e) {
            LinkedHashMapEntry<K,V> p =
                (LinkedHashMapEntry<K,V>)e, b = p.before, a = p.after;
            p.after = null;
            if (b == null)
                head = a;
            else
                b.after = a;
            if (a != null)
                a.before = b;
            else
                last = b;
            if (last == null)
                head = p;
            else {
                p.before = last;
                last.after = p;
            }
            tail = p;
            ++modCount;
        }
    }
```

下半部分：

```java
       V createdValue = create(key);
        if (createdValue == null) {
            return null;//如未重写create(K key)方法，这里就直接返回空了
        }

        synchronized (this) {
            createCount++;
            mapValue = map.put(key, createdValue);//将createdValue放入map，并取得原来的值


            if (mapValue != null) {//mapValue不为空，向下执行
                // There was a conflict so undo that last put
                map.put(key, mapValue);//如果mapValue不为空，则撤销上一步的put操作。额这里不是很明白为什么需要进行这一步操作
            } else {
                size += safeSizeOf(key, createdValue);//直接加
            }
        }

        if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue);
            return mapValue;
        } else {
            trimToSize(maxSize);//每次新加入对象都需要调用trimToSize方法看是否需要回收
            return createdValue;
        }
```

remove就很简单了，在map中移除了再减去size

```java
   public final V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V previous;
        synchronized (this) {
            previous = map.remove(key);
            if (previous != null) {
                size -= safeSizeOf(key, previous);
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, null);
        }

        return previous;
    }
```    