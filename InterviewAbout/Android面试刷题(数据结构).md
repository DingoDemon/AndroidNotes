##  常用数据结构简介
- List 有序、可重复；索引查询速度快 (ArrayList)；插入、删除伴随数据移动，速度慢(LinkedList)
- Map 键值对储存元素
- Set不允许重复元素，最多只允许一个null值，排列无序
- Queue 先进先出
- Stack 先进后出

##  并发集合了解哪些？
ConcurrentHashMap：一个动作只会影响结构的一部分，则把整体拆分成若干部分，每个部分一个锁，部分A被锁不会影响部分B，从而提高并发程度。

##  列举java的集合以及集合之间的继承关系

![](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/interview_2_1.png?raw=true)

##  容器类介绍以及之间的区别（容器类估计很多人没听这个词，Java容器主要可以划分为4个部分：List列表、Set集合、Map映射、工具类（Iterator迭代器、Enumeration枚举类、Arrays和Collections））
还没看，《tij》17章有，看完来填

##  List,Set,Map的区别
见上
##  List和Map的实现方式以及存储方式
List：比如ArrayList，ArrayList 是一个数组队列，相当于动态数组。插入数据后后面的元素后移，删除元素后面的元素前移，index即为数组中的位置。
Map：比如HashMap：讲key和value组装成一个Entry，放入主干table数组中。
##  HashMap的实现原理
[HashMap-Android sdk 25](https://github.com/DingoDemon/AndroidNotes/blob/master/OtherBasicKnowledge/HashMap.md)
##  HashMap数据结构？
[HashMap-Android sdk 25](https://github.com/DingoDemon/AndroidNotes/blob/master/OtherBasicKnowledge/HashMap.md)
##  HashMap源码理解
[HashMap-Android sdk 25](https://github.com/DingoDemon/AndroidNotes/blob/master/OtherBasicKnowledge/HashMap.md)
##  HashMap如何put数据（从HashMap源码角度讲解）？
[HashMap-Android sdk 25](https://github.com/DingoDemon/AndroidNotes/blob/master/OtherBasicKnowledge/HashMap.md)
##  HashMap怎么手写实现？
呵呵。

[HashMap-Android sdk 25](https://github.com/DingoDemon/AndroidNotes/blob/master/OtherBasicKnowledge/HashMap.md)
##  ConcurrentHashMap的实现原理
我太菜了，看不懂
##  ArrayMap和HashMap的对比

ArrayMap有点类似于SparseArray
 int[] mHashes;
    Object[] mArray;
    int mSize;
HashMap见上
##  HashTable实现原理
略过，没人用HashTable了
##  TreeMap具体实现
红黑树+HashMap
##  HashMap和HashTable的区别
线程安全不安全
##  HashMap与HashSet的区别
HashMap见上
HashSet持有 private transient HashMap<E,Object> map;HashSet新增元素的时候，其实是将元素E作为Key放入HashMap，return map.put(e, PRESENT)==null;PRESENT是一个虚拟占位符
##  HashSet与HashMap怎么判断集合元素重复？
见上
##  ArrayList和LinkedList的区别，以及应用场景
可以理解成数组和链表的区别，ArrayList插入删除时间复杂度为On，查询时间复杂度为O1，LinkedList插入删除时间复杂度为O1，查找时间复杂度为On，前者用于频繁查找，后者用于频繁插入删除
##  数组和链表的区别
ArrayList和LinkedList，如上
##  二叉树的深度优先遍历和广度优先遍历的具体实现

[二叉搜索树实现 - JAVA](https://github.com/DingoDemon/AndroidNotes/blob/master/OtherBasicKnowledge/BinarySearchTree.md)

```java
private void printTree(BSTNode node) {//中序遍历,输出sorted
        if (node == null) return;
//        visitNode(node);//前序遍历
        printTree((BSTNode) node.getLeftChild());
        visitNode(node);//中序遍历,输出sorted
        printTree((BSTNode) node.getRightChild());
//        visitNode(node);//后序遍历

    }
```    
##  堆的结构
堆（heap）也被称为优先队列（priority queue）。队列中允许的操作是先进先出（FIFO），在队尾插入元素，在队头取出元素。而堆也是一样，在堆底插入元素，在堆顶取出元素，但是堆中元素的排列不是按照到来的先后顺序，而是按照一定的优先顺序排列的。这个优先顺序可以是元素的大小或者其他规则。如图一所示就是一个堆，堆优先顺序就是大的元素排在前面，小的元素排在后面，这样得到的堆称为最大堆。最大堆中堆顶的元素是整个堆中最大的，并且每一个分支也可以看成一个最大堆。同样的，我们可以定义最小堆
##  堆和树的区别
堆是一种特殊的树，特殊表现在是完全二叉树，且父子结点上的元素有大小关系限制。
##  手写Stack
[代码](https://github.com/DingoDemon/AndroidNotes/blob/master/ExampleCode/StackImp.java)


