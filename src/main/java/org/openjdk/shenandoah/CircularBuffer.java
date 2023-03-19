package org.openjdk.shenandoah;

import java.util.*;

public class CircularBuffer<T> {

    public static final int DEFAULT_SIZE = 8;

    private final Object[] elements;
    private int tail;
    private int count;

    public CircularBuffer(int size) {
        elements = new Object[size];
        count = tail = 0;
    }

    public CircularBuffer(Collection<T> elements) {
        this.elements = elements.toArray();
        tail = 0;
        count = this.elements.length;
    }

    public CircularBuffer() {
        this(DEFAULT_SIZE);
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public void add(T i) {
        elements[tail] = i;
        tail = (tail  + 1) % elements.length;
        ++count;
    }

    public List<T> subList(int include, int exclude) {
        if (include == exclude) {
            return Collections.emptyList();
        }

        List<T> list = new ArrayList<>(exclude - include);
        for (; include < exclude; ++include) {
            list.add(get(include));
        }
        return list;
    }

    public T get(int elementAt) {
        return (T) elements[index(elementAt)];
    }

    public int size() {
        return Math.min(count, elements.length);
    }

    private int index(int offset) {
        if (count <= elements.length) {
            return offset;
        }
        return ((tail + offset) % elements.length);
    }
}
