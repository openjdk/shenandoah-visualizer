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
