package BinarySearchTree;

import java.util.Iterator;

public interface BSTInterface extends TreeInterface {

    void insertNode(Comparable comparable);


    void deleteNode(Comparable comparable);

    boolean contains(Comparable comparable);


    Comparable findMax();


    Comparable findMin();



    public Iterator<BSTNodeInterface> getPreorderIterator();//前序遍历

    public Iterator<BSTNodeInterface> getPostorderIterator();//中序遍历

    public Iterator<BSTNodeInterface> getInorderIterator();//后续遍历

    public Iterator<BSTNodeInterface> getLevelOrderIterator();//广度遍历


}
