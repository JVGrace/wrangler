/*
 * Copyright © 2017-2019 Pradumn Patel
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

import io.cdap.wrangler.api.annotations.PublicEvolving;
import io.cdap.wrangler.api.parser.TimeDuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link TimeDuration} parsing and conversion functionality.
 */
@PublicEvolving
public class TimeDurationTest {

    @Test
    public void testValidUnits() {
        assertTimeDuration("5ms", 5);
        assertTimeDuration("2.5s", 2500);
        assertTimeDuration("3min", 3 * 60 * 1000);
        assertTimeDuration("1.5h", (long) (1.5 * 60 * 60 * 1000));
        assertTimeDuration("0.5d", (long) (0.5 * 24 * 60 * 60 * 1000));
    }

    @Test
    public void testCaseInsensitivity() {
        assertTimeDuration("5MS", 5);
        assertTimeDuration("2S", 2000);
        assertTimeDuration("3MIN", 3 * 60 * 1000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUnitsThrowsException() {
        new TimeDuration("10ns");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNumberFormatThrowsException() {
        new TimeDuration("fiveMs");
    }

    private void assertTimeDuration(String input, long expected) {
        TimeDuration duration = new TimeDuration(input);
        assertEquals(expected, duration.getMilliseconds());
    }
}
