/*
 * Copyright (c) 2023, Amazon.com, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
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
