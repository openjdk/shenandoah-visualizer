package org.openjdk.shenandoah;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

class RegionSelectionParser {
    StringBuilder sb = new StringBuilder(4);
    int rangeStart = -1;

    public List<Integer> parse(String expression) {
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
