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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

class RegionSelectionParser {
    StringBuilder sb = new StringBuilder(4);
    int rangeStart = -1;

    List<Integer> parse(String expression) {
        try {
            Collection<Integer> ints = new HashSet<>();
            for (int i = 0, n = expression.length(); i < n; ++i) {
                char c = expression.charAt(i);
                if (Character.isDigit(c)) {
                    sb.append(c);
                } else if (c == ',') {
                    consumeExpression(ints);
                } else if (c == '-') {
                    rangeStart = consumeBuffer();
                }
            }
            if (!sb.isEmpty()) {
                consumeExpression(ints);
            }
            List<Integer> sorted = new ArrayList<>(ints);
            sorted.sort(Integer::compareTo);
            return sorted;
        } finally {
            rangeStart = -1;
            sb.setLength(0);
        }
    }

    private void consumeExpression(Collection<Integer> ints) {
        if (rangeStart == -1) {
            ints.add(consumeBuffer());
        } else {
            int rangeFinish = consumeBuffer();
            expandRange(ints, rangeFinish);
        }
    }

    private void expandRange(Collection<Integer> ints, int rangeFinish) {
        if (rangeStart > rangeFinish) {
            throw new IllegalArgumentException("Invalid range: " + rangeStart + " - " + rangeFinish);
        }

        for (int i = rangeStart; i <= rangeFinish; ++i) {
            ints.add(i);
        }
        rangeStart = -1;
    }

    private int consumeBuffer() {
        int result = Integer.parseInt(sb.toString());
        sb.setLength(0);
        return result;
    }
}
