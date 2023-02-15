/*
 * Copyright (c) 2022, Amazon.com, Inc. All rights reserved.
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

import org.HdrHistogram.Histogram;
import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;


import java.io.IOException;

public class ProcessLoggingTagTest {
    DataLogProvider dataLogProvider;

    @Before
    public void setUp() throws IOException {
        dataLogProvider = new DataLogProvider("src/test/resources/regions-6425.log");
    }

    @Test
    public void test_process_tagged_meta_data() {
        String taggedMetaDataLine = "[1.666s][info][gc,region] 1665172926 0 3859 1024 2";
        Assert.assertEquals("1665172926 0 3859 1024 2", dataLogProvider.processLoggingTag(taggedMetaDataLine));
    }
    @Test
    public void test_process_tagged_region_data() {
        String taggedRegionDataLine = "[1.667s][info][gc,region] 648518346342989924 648518346342989924 648518346342989924";
        Assert.assertEquals("648518346342989924 648518346342989924 648518346342989924", dataLogProvider.processLoggingTag(taggedRegionDataLine));
    }
    @Test
    public void test_process_not_tagged_meta_data() {
        String notTaggedMetaDataLine = "1665172926 0 3859 1024 2";
        Assert.assertEquals("1665172926 0 3859 1024 2", dataLogProvider.processLoggingTag(notTaggedMetaDataLine));
    }
    @Test
    public void test_process_not_tagged_region_data() {
        String notTaggedRegionDataLine = "648518346342989924 648518346342989924 648518346342989924";
        Assert.assertEquals("648518346342989924 648518346342989924 648518346342989924", dataLogProvider.processLoggingTag(notTaggedRegionDataLine));
    }
}
