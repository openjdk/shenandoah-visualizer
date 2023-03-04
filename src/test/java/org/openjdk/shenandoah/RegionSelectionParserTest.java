package org.openjdk.shenandoah;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RegionSelectionParserTest {

    @Test
    public void testParseSingleNumber() {
        RegionSelectionParser parser = new RegionSelectionParser();
        Collection<Integer> regions = parser.parse("42");
        assertEquals(List.of(42), regions);
    }

    @Test
    public void testParseCommaDelimited() {
        RegionSelectionParser parser = new RegionSelectionParser();
        Collection<Integer> regions = parser.parse("42, 23");
        assertEquals(Arrays.asList(23, 42), regions);
    }

    @Test
    public void testParseRange() {
        RegionSelectionParser parser = new RegionSelectionParser();
        Collection<Integer> regions = parser.parse("23 - 29");
        assertEquals(Arrays.asList(23, 24, 25, 26, 27, 28, 29), regions);
    }

    @Test
    public void testNumbersAndRanges() {
        RegionSelectionParser parser = new RegionSelectionParser();
        Collection<Integer> regions = parser.parse("13, 23 - 29, 25, 42");
        assertEquals(Arrays.asList(13, 23, 24, 25, 26, 27, 28, 29, 42), regions);
    }

    @Test
    public void testInvalidRangeThrows() {
        try {
            RegionSelectionParser parser = new RegionSelectionParser();
            parser.parse("29 - 23");
            fail("Should not allow reversed ranges");
        } catch (IllegalArgumentException ignore) {}
    }
}
