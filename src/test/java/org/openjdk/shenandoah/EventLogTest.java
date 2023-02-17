package org.openjdk.shenandoah;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EventLogTest {

    private record Event(long time) implements Timed { }

    @Test
    public void testThatEmptyLogHasEmptyRange() {
        Assert.assertEquals(Collections.emptyList(), new EventLog<Event>().inRange());
    }

    @Test
    public void testFetchingLatest() {
        EventLog<Event> log = createEventLog(1, 2, 3);
        log.stepBy(1);
        Assert.assertEquals(new Event(1), log.latest());
    }
    @Test
    public void testThatSteppingForwardIncreasesRange() {
        EventLog<Event> log = createEventLog(1, 2, 3);
        log.stepBy(1);
        Assert.assertEquals(createEvents(1), log.inRange());
        log.stepBy(10);
        Assert.assertEquals(createEvents(1, 2, 3), log.inRange());
    }

    @Test
    public void testThatSteppingBackwardDecreasesRange() {
        EventLog<Event> log = createEventLog(1, 2, 3);
        log.stepBy(10);
        Assert.assertEquals(createEvents(1, 2, 3), log.inRange());
        log.stepBy(-1);
        Assert.assertEquals(createEvents(1, 2), log.inRange());
    }

    @Test
    public void testAdvancingTimeIncreasesRange() {
        EventLog<Event> log = createEventLog(100, 200, 300);
        log.advanceTo(50, TimeUnit.NANOSECONDS);
        Assert.assertEquals(Collections.emptyList(), log.inRange());
        log.advanceTo(150, TimeUnit.NANOSECONDS);
        Assert.assertEquals(createEvents(100), log.inRange());
        log.advanceTo(500, TimeUnit.NANOSECONDS);
        Assert.assertEquals(createEvents(100, 200, 300), log.inRange());
    }

    @Test
    public void testIncrementalAdvanceIncreasesRange() {
        // reference time is the time of the first event
        EventLog<Event> log = createEventLog(100, 200, 300);
        log.advanceBy(50, TimeUnit.NANOSECONDS);
        Assert.assertEquals(createEvents(100), log.inRange()); // ref time is 150
        log.advanceBy(25, TimeUnit.NANOSECONDS);             // ref time is 175
        Assert.assertEquals(createEvents(100), log.inRange());
        log.advanceBy(25, TimeUnit.NANOSECONDS);             // ref time is 200
        Assert.assertEquals(createEvents(100, 200), log.inRange());
    }

    @Test
    public void testRangeIncludesEventsInsertedBeforeReferenceTime() {
        EventLog<Event> log = createEventLog(100, 200, 300);
        log.advanceTo(500, TimeUnit.MILLISECONDS);
        Assert.assertEquals(createEvents(100, 200, 300), log.inRange());
        log.add(new Event(400));
        log.add(new Event(600));
        Assert.assertEquals(createEvents(100, 200, 300, 400), log.inRange());
    }

    private static List<Event> createEvents(int... args) {
        var list = new ArrayList<Event>(args.length);
        for (var t : args) {
            list.add(new Event(t));
        }
        return list;
    }

    private static EventLog<Event> createEventLog(int... args) {
        var log = new EventLog<Event>();
        for (var t : args) {
            log.add(new Event(t));
        }
        return log;
    }
}
