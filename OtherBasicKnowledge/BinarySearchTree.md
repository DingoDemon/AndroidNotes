参考资料[《数据结构与算法分析_Java语言描述(第2版)》](https://item.jd.com/11886254.html)

二叉搜索树是指一棵空树或者具有下列性质的二叉树：

1. 若任意节点的左子树不空，则左子树上所有节点的值均小于它的根节点的值；
 
2. 若任意节点的右子树不空，则右子树上所有节点的值均大于它的根节点的值；

3. 任意节点的左、右子树也分别为二叉查找树；
 
4. 没有键值相等的节点。

二叉查找树相比于其他数据结构的优势在于查找、插入的时间复杂度较低。当先后插入的关键字有序时，构成的二叉查找树蜕变为单支树，树的深度为n，其平均查找长度为(n+1)/2(和顺序查找相同)。最好的情况是二叉查找树的形态和折半查找的判定树相同，其平均查找长度和log2(n)成正比(O(log2(n))

for example:

![example](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/bst.png)

接下来，我们将用java去实现一个BinarySearchTree:

------

## BinarySearchTreeNode


首先是节点node，将节点Node的行为抽象成[BinarySearchTreeNode](https://github.com/DingoDemon/AndroidNotes/blob/master/ExampleCode/BSTNodeInterface.java)如下：

-  Comparable getData();//返回结点的数据部分

-  void setData(Comparable newData);//设置结点的数据域的值

-  BSTNodeInterface getLeftChild();//获取结点的左孩子

-  BSTNodeInterface getRightChild();//获取结点的右孩子

-  void setLeftChild(BSTNodeInterface leftChild);//设置结点的左孩子为指定结点

-  void setRightChild(BSTNodeInterface rightChild);//设置结点的右孩子为指定结点

-  boolean hasLeftChild();//判断结点是否有左孩子

-  boolean hasRightChild();//判断结点是否有右孩子

-  boolean isLeaf();//检查结点是否是叶子结点

-  int getNumberOfNodes();//计算以该结点为根的子树的结点数目

-  int getHeight();//计算以该结点为根的子树的高度

-  BSTNodeInterface copy();//复制以该结点为根的子树

-  BSTNodeInterface getParent();//获取结点的父节点

-  void setParent(BSTNodeInterface nodeInterface);//设置节点的父节点

---

## BSTNode
接下来，我们用一个[BSTNode](https://github.com/DingoDemon/AndroidNotes/blob/master/ExampleCode/BSTNode.java)类去实现它：


```java

public class BSTNode implements BSTNodeInterface {

    private BSTNode left;//左节点
    private BSTNode right;//右节点
    private Comparable element;//携带元素
    private BSTNode parent;//父节点

   /**
     * 构造根节点
     * @param element
     */
    public BSTNode(Comparable element) {
        this(null, null, element, null);
    }

    /**
     * 构造子节点
     * @param left
     * @param right
     * @param element
     * @param parent
     */
    public BSTNode(BSTNode left, BSTNode right, Comparable element, BSTNode parent) {
        this.left = left;
        this.right = right;
        this.element = element;
        this.parent = parent;
    }
//实现接口code....
}
```

接下来，我们来看抽象行为的具体实现：
先是比较简单的get/set方法：

```java
@Override
public void setParent(BSTNodeInterface nodeInterface) {
    this.parent = (BSTNode) nodeInterface;
}
```

```java
public void setParent(BSTNode parent) {
    this.parent = parent;
}
```

```java
@Override
public Comparable getData() {
    return element;
}
```

```java
@Override
public void setData(Comparable newData) {
    this.element = newData;
}
```

```java
@Override
public BSTNodeInterface getLeftChild() {
    return left;
}
```

```java
@Override
public BSTNodeInterface getRightChild() {
    return right;
}
```

```java
@Override
public void setLeftChild(BSTNodeInterface leftChild) {
    this.left = (BSTNode) leftChild;
}
```

```java
@Override
public void setRightChild(BSTNodeInterface rightChild) {
    this.right = (BSTNode) rightChild;
}
```

```java
@Override
public boolean hasLeftChild() {
    return left != null;
}
```

```java
@Override
public boolean hasRightChild() {
    return right != null;
}
```

```java
@Override
public boolean isLeaf() {
    return (!hasLeftChild()) && (!hasRightChild());
}
```

接下来统计的方法：


```java
   @Override
    public int getNumberOfNodes() {
        return getNumberOfNodes(this);
    }

    private int getNumberOfNodes(BSTNode node) {
        if (node != null) {
            int leftNumber = 0;
            int rightNumber = 0;
            //利用递归的思想将左右两棵树的节点数加起来再加上自身。
            if (hasLeftChild()) {
                leftNumber = left.getNumberOfNodes();
            }
            if (hasRightChild()) {
                rightNumber = right.getNumberOfNodes();
            }
            return 1 + leftNumber + rightNumber;
        }else{
            return 0;
        }
    }
```


```java
  @Override
    public int getHeight() {
        return getHeight(this);
    }


    private int getHeight(BSTNode node) {
        int height = 0;
        if (node != null)
        //利用递归的思想返回左右两个子节点最大高度+1
            height = 1 + Math.max(getHeight(node.left), getHeight(node.right));
        return height;
    }
```


最后是复制自身的方法：

```java
 @Override
    public BSTNodeInterface copy() {
        BSTNode newRoot = new BSTNode(element);
        if (left != null)
            newRoot.left = (BSTNode) left.copy();
        if (right != null)
            newRoot.right = (BSTNode) right.copy();
        return newRoot;
    }
```

------

## BinarySearchTree

接下来就是bst的抽象行为：

先是[树](https://github.com/DingoDemon/AndroidNotes/blob/master/ExampleCode/TreeInterface.java)通用抽象行为：


```java
public interface TreeInterface<T> {

    public T getRootData();//获取根节点数据

    public int getHeight();//获取树的高度

    public int getNumberOfNodes();//获取节点总数

    public boolean isEmpty();//是否为空

    public void clear();//清空该树

}
```

[二叉树](https://github.com/DingoDemon/AndroidNotes/blob/master/ExampleCode/BSTInterface.java)的抽象行为：

```java
public interface BSTInterface extends TreeInterface {

    void insertNode(Comparable comparable);


    void deleteNode(Comparable comparable);

    boolean contains(Comparable comparable);


    Comparable findMax();


    Comparable findMin();



    public Iterator<BSTNodeInterface> getPreorderIterator();//前序遍历

    public Iterator<BSTNodeInterface> getPostorderIterator();//后序遍历

    public Iterator<BSTNodeInterface> getInorderIterator();//中续遍历

    public Iterator<BSTNodeInterface> getLevelOrderIterator();//广度优先遍历

}
```

接下来就是实现这些接口的类([BST](https://github.com/DingoDemon/AndroidNotes/blob/master/ExampleCode/BST.java)):

```java
public class BST implements BSTInterface {

    private BSTNodeInterface root;//根节点


//............. 实现code
}
```

然后是实现Tree的方法：

```java
 @Override
    public Comparable getRootData() {
        return root.getData();
    }

   @Override
    public int getHeight() {
        return root == null ? 0 : root.getHeight();
    }


 @Override
    public int getNumberOfNodes() {
        return root == null ? 0 : root.getNumberOfNodes();
    }

 @Override
    public boolean isEmpty() {
        return root == null;
    }


  @Override
    public void clear() {
        root = null;
    }
    
```
 
 
*//这里存疑，但是查到说是正确的？(子树的left(子子树)/right(子子树)和right(子子树)/left(子子树).parent(子树)互相持有引用，是否会造成内存泄漏？还是说应该从叶子开始递归删除节点？)[stackoverflow](https://stackoverflow.com/questions/22271147/how-to-properly-clear-an-entire-binary-tree-in-java?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa)*

**后来查证得：
在Java中采取了 可达性分析法。该方法的基本思想是通过一系列的“GC Roots”对象作为起点进行搜索，如果在“GC Roots”和一个对象之间没有可达路径，则称该对象是不可达的，不过要注意的是被判定为不可达的对象不一定就会成为可回收对象。被判定为不可达的对象要成为可回收对象必须至少经历两次标记过程，如果在这两次标记过程中仍然没有逃脱成为可回收对象的可能性，则基本上就真的成为可回收对象了。**

> 证明不会内存溢出

再是BSTInterface的实现方法：


插入:
插入的思想是不断比较找到合适的位置，将Comparable包装成node插入

```java
   @Override
    public void insertNode(Comparable comparable) {
        root = insertNode(comparable, (BSTNode) root);
    }

    private BSTNode insertNode(Comparable comparable, BSTNode node) {
        if (node == null) {
            return new BSTNode(null, null, comparable, null);//如果为空的话，则new一个。
        }
        int compareResult = comparable.compareTo(node.getData());
        if (compareResult > 0) {//如果大的话，向右边插入
            node.setRightChild(insertNode(comparable, (BSTNode) node.getRightChild()));//递归执行
            ((BSTNode) node.getRightChild()).setParent(node);//将父节点指向

        } else if (compareResult < 0) {//小的话就往左边插入
            node.setLeftChild(insertNode(comparable, (BSTNode) node.getLeftChild()));
            ((BSTNode) node.getLeftChild()).setParent(node);
        } else {
         //do nothing
        }
        return node;
    }
```

查找：
查找的思想是大？就往右走，小？就往左走

```java
    @Override
    public boolean contains(Comparable comparable) {
        return contains(comparable, root);
    }


    private boolean contains(Comparable comparable, BSTNodeInterface node) {
        if (node == null)
            return false;

        int compareResult = comparable.compareTo(node.getData());
        if (compareResult < 0) {
            return contains(comparable, node.getLeftChild());//小了往左走
        } else if (compareResult > 0) {
            return contains(comparable, node.getRightChild());//大了往右走
        } else return true;//命中

    }
```

同理很容里写出findMax和findMin：

```java
 @Override
    public Comparable findMax() {
        return findMax(root).getData();
    }


    private BSTNodeInterface findMax(BSTNodeInterface node) {
        if (node == null)
            return null;
        else if (node.hasRightChild()) {
            return findMax(node.getRightChild());
        }
        return node;
    }
```

```java
   @Override
    public Comparable findMin() {
        return findMin(root).getData();
    }

    private BSTNodeInterface findMin(BSTNodeInterface node) {
        if (node == null)
            return null;
        while (node.hasLeftChild()) {
            node = node.getLeftChild();
        }
        return node;
    }
```

删除：
删除的逻辑相对于增和查来说要麻烦一些。分为三种情况：

1. 叶子节点，直接删除
2. 只有一个子节点，删除自身并将子节点和父节点连接起来
3. 如果自身有2个子节点，则选择右子树中最小的节点，将其赋值给自身，并删除那个节点，如果右子树中最小的节点依然有2个子节点，则递归重复这一过程

```java
 @Override
    public void deleteNode(Comparable comparable) {
        deleteNode(comparable, (BSTNode) root);
    }

 private BSTNode deleteNode(Comparable comparable, BSTNode node) {
        if (node == null) {
            return null;
        }

        int result = comparable.compareTo(node.getData());
        if (result > 0)
            node.setRightChild(deleteNode(comparable, (BSTNode) node.getRightChild()));//向左走
        else if (result < 0)
            node.setLeftChild(deleteNode(comparable, (BSTNode) node.getLeftChild()));//向右走
        else if (node.hasLeftChild() && node.hasRightChild()) {
            node.setData(findMin(node.getRightChild()).getData());//找到右子树中最小的节点
            node.setRightChild(deleteNode(node.getData(), (BSTNode) node.getRightChild()));//删除它
        } else
            node = (BSTNode) ((node.hasLeftChild()) ? node.getLeftChild() : node.getRightChild());//将自己的根节点和父节点连起来
        return node;

    }

```


常规方法就介绍完毕了接下来是三种深度优先的遍历方式：

```java
   public void printTree() {
        printTree((BSTNode) root);
    }

    private void printTree(BSTNode node) {//中序遍历,输出sorted
        if (node == null) return;
//        visitNode(node);//前序遍历
        printTree((BSTNode) node.getLeftChild());
        visitNode(node);//中序遍历,输出sorted
        printTree((BSTNode) node.getRightChild());
//        visitNode(node);//后序遍历

    }

    private void visitNode(BSTNodeInterface node) {
        System.out.println(node.getData());
    }

```

根据*(根左右)*,*(左根右)*,*(左右根)*这三个先后顺序来处理访问到的节点便是先，中，后序遍历的结果。

而广度遍历的遍历方式，我写在LevelOrderIterator内的：

```
   class LevelOrderIterator implements Iterator<BSTNodeInterface> {
        private ArrayDeque<BSTNode> arrayDeque;//双端队列

        public LevelOrderIterator() {
            arrayDeque = new ArrayDeque<>();
            if (root != null)
                arrayDeque.add((BSTNode) root);将根节点入队
        }

        @Override
        public boolean hasNext() {
            return !arrayDeque.isEmpty();
        }

        @Override
        public BSTNode next() {
            BSTNode node = arrayDeque.poll();//弹出一个node
//然后将node的子节点放入队列尾部。
            if (node.hasLeftChild())
                arrayDeque.addLast((BSTNode) node.getLeftChild());
            if (node.hasRightChild())
                arrayDeque.addLast((BSTNode) node.getRightChild());
            return node;
        }
    }
```

BST的基本实现和4种遍历就介绍完毕了(中序遍历和后续遍历的Iterator没写。。暂时没想出怎么写)。




