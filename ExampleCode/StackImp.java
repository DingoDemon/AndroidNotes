
public class StackImp<E> implements Stack<E> {

    public static final int DEFAULT_SIZE = 4;

    private int capacity = 0;

    private Object[] items;

    private int top;


    public StackImp() {
        items = new Object[DEFAULT_SIZE];
        top = -1;
    }

    @Override
    public void push(E e) {
        if (getSize() + 1 > items.length)
            resize();
        items[++top] = e;
        capacity++;

    }

    private void resize() {
        Object[] newOne = new Object[capacity <<1];
        System.arraycopy(items, 0, newOne, 0, items.length);
        items = newOne;
    }

    @Override
    public E pop() {
        if (!isEmpty()) {
            Object o = items[top];
            items[top--] = null;
            return (E) o;
        } else
            return null;


    }

    @Override
    public boolean isEmpty() {
        return capacity == 0;
    }

    @Override
    public int getSize() {
        return top + 1;
    }

    @Override
    public E top() {
        return (E) items[top];
    }
}
