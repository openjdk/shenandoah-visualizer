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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

class EventLog<T extends Timed> {
    private final List<T> events;
    private final TimeUnit eventTimeUnit;
    private int cursor;
    private long referenceTime;

    EventLog() {
        this(TimeUnit.NANOSECONDS);
    }

    EventLog(TimeUnit eventTimeUnit) {
        // TODO: Use a circular buffer to prevent live sessions from growing without bound.
        // (And to avoid having to shift all the remaining elements if we remove the oldest).
        // Linked list could also work, but the sublist construction would need to change.
        this.events = new ArrayList<>();
        this.eventTimeUnit = eventTimeUnit;
    }

    public synchronized void add(T t) {
        if (!events.isEmpty() && t.time() < events.get(events.size() - 1).time()) {
            throw new IllegalArgumentException("Events must be added in chronological order.");
        }

        events.add(t);
        System.out.println("Added snapshot: " + t.time() + ", referenceTime: " + referenceTime + ", cursor: " + cursor);

        if (referenceTime == 0) {
            referenceTime = t.time();
        } else if (t.time() < referenceTime) {
            cursor++;
        }
    }

    public synchronized List<T> inRange() {
        if (events.isEmpty() || cursor == 0) {
            return Collections.emptyList();
        }

        assert 1 <= cursor && cursor <= events.size();
        return events.subList(0, cursor);
    }

    public synchronized void stepBy(int amount) {
        stepTo(cursor + amount);
    }

    public void stepTo(int value) {
        cursor = clamp(value, events.size());
        referenceTime = latest().time();
    }

    public synchronized void stepToEnd() {
        stepTo(Integer.MAX_VALUE);
    }

    public synchronized  void advanceTo(long pointInTime, TimeUnit timeUnit) {
        long eventTime = eventTimeUnit.convert(pointInTime, timeUnit);
        advanceTo(eventTime);
    }

    public void advanceBy(long duration, TimeUnit timeUnit) {
        long eventTime = eventTimeUnit.convert(duration, timeUnit);
        long pointInTime = referenceTime + eventTime;
        advanceTo(pointInTime);
    }

    private void advanceTo(long pointInTime) {
        for (var iter = events.listIterator(cursor); iter.hasNext(); ) {
            var event = iter.next();
            if (pointInTime < event.time()) {
                break;
            }
            cursor++;
        }
        referenceTime = pointInTime;
    }

    public synchronized T latest() {
        if (cursor == 0) {
            return null;
        }
        return events.get(cursor - 1);
    }

    public int size() {
        return events.size();
    }

    public int cursor() {
        return cursor;
    }

    private static int clamp(int val, int max) {
        return Math.min(Math.max(1, val), max);
    }
}
