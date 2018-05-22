package BinarySearchTree;

public interface BSTNodeInterface {

    Comparable getData();//返回结点的数据部分

    void setData(Comparable newData);//设置结点的数据域的值

    BSTNodeInterface getLeftChild();//获取结点的左孩子

    BSTNodeInterface getRightChild();//获取结点的右孩子

    void setLeftChild(BSTNodeInterface leftChild);//设置结点的左孩子为指定结点

    void setRightChild(BSTNodeInterface rightChild);//设置结点的右孩子为指定结点

    boolean hasLeftChild();//判断结点是否有左孩子

    boolean hasRightChild();//判断结点是否有右孩子

    boolean isLeaf();//检查结点是否是叶子结点

    int getNumberOfNodes();//计算以该结点为根的子树的结点数目

    int getHeight();//计算以该结点为根的子树的高度

    BSTNodeInterface copy();//复制以该结点为根的子树

    BSTNodeInterface getParent();//获取结点的父节点

    void setParent(BSTNodeInterface nodeInterface);//设置节点的父节点


}
