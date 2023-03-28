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

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class CircularBufferTest {

    private CircularBuffer<Character> buffer;

    @Before
    public void setUp() {
        buffer = new CircularBuffer<>();
    }

    @Test
    public void testCreateIsEmpty() {
        assertTrue(buffer.isEmpty());
    }

    @Test
    public void testAddItem() {
        buffer.add('A');
        assertFalse(buffer.isEmpty());
        assertEquals(Character.valueOf('A'), buffer.get(0));
    }

    @Test
    public void testWrapAround() {
        for (int i = 0; i < CircularBuffer.DEFAULT_SIZE + 1; i++) {
            buffer.add((char) ('A' + i));
            assertFalse(buffer.isEmpty());
        }
        assertEquals(Character.valueOf('B'), buffer.get(0));
        assertEquals(Character.valueOf('I'), buffer.get(CircularBuffer.DEFAULT_SIZE - 1));
    }

    @Test
    public void testSubList() {
        for (int i = 0; i < CircularBuffer.DEFAULT_SIZE + 1; i++) {
            buffer.add((char) ('A' + i));
        }

        // [I, B, C, D, E, F, G, H]
        assertEquals(Collections.emptyList(), buffer.subList(1, 1));
        assertEquals(Arrays.asList('B', 'C', 'D'), buffer.subList(0, 3));
        assertEquals(Arrays.asList('C', 'D', 'E'), buffer.subList(1, 4));
        assertEquals(Arrays.asList('G', 'H', 'I'), buffer.subList(5, 8));
    }

    @Test
    public void testGetEnd() {
        for (int i = 0; i < CircularBuffer.DEFAULT_SIZE + 1; i++) {
            char c = (char) ('A' + i);
            buffer.add(c);
            assertEquals(String.valueOf(c), String.valueOf(buffer.get(buffer.size() - 1)));
        }
    }
}
