/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.utils;

import io.cdap.wrangler.api.parser.ByteSize;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ByteSize} parsing and conversion functionality.
 */
public class ByteSizeTest {

    @Test
    public void testValidUnits() {
        assertByteSize("10B", 10);
        assertByteSize("2KB", 2 * 1024L);
        assertByteSize("1.5MB", (long) (1.5 * 1024 * 1024));
        assertByteSize("3GB", 3L * 1024 * 1024 * 1024);
        assertByteSize("0.5TB", (long) (0.5 * 1024L * 1024 * 1024 * 1024));
        assertByteSize("4PB", 4L * 1024 * 1024 * 1024 * 1024 * 1024);
    }

    @Test
    public void testCaseInsensitivity() {
        assertByteSize("10kb", 10 * 1024);
        assertByteSize("2mb", 2 * 1024 * 1024);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUnitsThrowsException() {
        new ByteSize("10XB");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNumberFormatThrowsException() {
        new ByteSize("abcMB");
    }

    private void assertByteSize(String input, long expected) {
        ByteSize byteSize = new ByteSize(input);
        assertEquals(expected, byteSize.getBytes());
    }
}
