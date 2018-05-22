package BinarySearchTree;

import java.util.ArrayDeque;
import java.util.Iterator;

public class BST implements BSTInterface {

    private BSTNodeInterface root;//根节点


    @Override
    public void insertNode(Comparable comparable) {
        root = insertNode(comparable, (BSTNode) root);
    }

    private BSTNode insertNode(Comparable comparable, BSTNode node) {
        if (node == null) {
            return new BSTNode(null, null, comparable, null);
        }
        int compareResult = comparable.compareTo(node.getData());
        if (compareResult > 0) {
            node.setRightChild(insertNode(comparable, (BSTNode) node.getRightChild()));
            ((BSTNode) node.getRightChild()).setParent(node);

        } else if (compareResult < 0) {
            node.setLeftChild(insertNode(comparable, (BSTNode) node.getLeftChild()));
            ((BSTNode) node.getLeftChild()).setParent(node);
        } else {
            //do nothing
        }
        return node;
    }

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
            node.setRightChild(deleteNode(comparable, (BSTNode) node.getRightChild()));
        else if (result < 0)
            node.setLeftChild(deleteNode(comparable, (BSTNode) node.getLeftChild()));
        else if (node.hasLeftChild() && node.hasRightChild()) {
            node.setData(findMin(node.getRightChild()).getData());
            node.setRightChild(deleteNode(node.getData(), (BSTNode) node.getRightChild()));
        } else
            node = (BSTNode) ((node.hasLeftChild()) ? node.getLeftChild() : node.getRightChild());
        return node;

    }


    @Override
    public boolean contains(Comparable comparable) {
        return contains(comparable, root);
    }


    private boolean contains(Comparable comparable, BSTNodeInterface node) {
        if (node == null)
            return false;

        int compareResult = comparable.compareTo(node.getData());
        if (compareResult < 0) {
            return contains(comparable, node.getLeftChild());
        } else if (compareResult > 0) {
            return contains(comparable, node.getRightChild());
        } else return true;

    }

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

    @Override
    public Iterator<BSTNodeInterface> getPreorderIterator() {
        return new PreorderTraversalIterator();
    }

    @Override
    public Iterator<BSTNodeInterface> getPostorderIterator() {
        return null;
    }

    @Override
    public Iterator<BSTNodeInterface> getInorderIterator() {
        return null;
    }

    @Override
    public Iterator<BSTNodeInterface> getLevelOrderIterator() {
        return new LevelOrderIterator();
    }


    public PreorderTraversalIterator getpreIterator() {
        return new PreorderTraversalIterator();
    }


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


    public void clear() {
        root = null;
    }

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

    public class LevelOrderIterator implements Iterator<BSTNodeInterface> {
        private ArrayDeque<BSTNode> arrayDeque;

        public LevelOrderIterator() {
            arrayDeque = new ArrayDeque<>();
            if (root != null)
                arrayDeque.add((BSTNode) root);
        }

        @Override
        public boolean hasNext() {
            return !arrayDeque.isEmpty();
        }

        @Override
        public BSTNode next() {
            BSTNode node = arrayDeque.poll();
            if (node.hasLeftChild())
                arrayDeque.addLast((BSTNode) node.getLeftChild());
            if (node.hasRightChild())
                arrayDeque.addLast((BSTNode) node.getRightChild());
            return node;
        }
    }

    public class PreorderTraversalIterator implements Iterator<BSTNodeInterface> {
        private ArrayDeque<BSTNode> arrayDeque;
        private BSTNode currentNode;

        public PreorderTraversalIterator() {
            arrayDeque = new ArrayDeque<>();
            if (root != null)
                arrayDeque.add((BSTNode) root);
            currentNode = (BSTNode) root;
        }

        @Override
        public boolean hasNext() {
            return !arrayDeque.isEmpty() && currentNode != null;
        }

        @Override
        public BSTNode next() {
            BSTNode node = arrayDeque.pop();
            if (node.hasRightChild()) arrayDeque.push((BSTNode) node.getRightChild());
            if (node.hasLeftChild()) arrayDeque.push((BSTNode) node.getLeftChild());
            return node;

        }
    }

    public class InorderTraversalIterator implements Iterator<BSTNode> {
        private ArrayDeque<BSTNode> arrayDeque;
        private BSTNode currentNode;

        public InorderTraversalIterator() {
            arrayDeque = new ArrayDeque<>();
            if (root != null)
                arrayDeque.add((BSTNode) root);
            currentNode = (BSTNode) root;
        }

        @Override
        public boolean hasNext() {
            return !arrayDeque.isEmpty() && currentNode != null;
        }

        @Override
        public BSTNode next() {
            BSTNode node = arrayDeque.pop();
            if (node.hasRightChild()) arrayDeque.push((BSTNode) node.getRightChild());
            if (node.hasLeftChild()) arrayDeque.push((BSTNode) node.getLeftChild());
            return node;
        }
    }
}
