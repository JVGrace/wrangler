/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package io.cdap.wrangler.utils;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.Row;

public class AggregateStatsTest {

    @Test
    public void testBasicAggregation() throws Exception {
        // Create sample input data
        List<Row> rows = Arrays.asList(
            new Row("data_transfer_size", "1MB").add("response_time", "500ms"),
            new Row("data_transfer_size", "2.5MB").add("response_time", "1.2s"),
            new Row("data_transfer_size", "500KB").add("response_time", "300ms")
        );

        String[] recipe = new String[] {
            "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
        };

        List<Row> results = TestingRig.execute(recipe, rows);

        // Verify output
        Assert.assertEquals(1, results.size());
        Row result = results.get(0);
        
        // Expected calculations:
        // Total size: (1MB + 2.5MB + 500KB) = (1048576 + 2621440 + 512000) bytes = 4182016 bytes = 3.98828125 MB
        Assert.assertEquals(3.98828125, (double) result.getValue("total_size_mb"), 0.0001);
        
        // Total time: (500ms + 1200ms + 300ms) = 2000ms = 2.0 seconds
        Assert.assertEquals(2.0, (double) result.getValue("total_time_sec"), 0.0001);
    }

    @Test
    public void testMultipleAggregations() throws Exception {
        // Create sample input data
        List<Row> rows = Arrays.asList(
            new Row("file_size", "10MB").add("processing_time", "100ms"),
            new Row("file_size", "5MB").add("processing_time", "50ms"),
            new Row("file_size", "15MB").add("processing_time", "150ms")
        );

        String[] recipe = new String[] {
            "aggregate-stats :file_size :processing_time " +
            "total_size_mb avg_size_mb median_size_mb p95_size_mb " +
            "total_time_sec avg_time_sec max_time_sec min_time_sec"
        };

        List<Row> results = TestingRig.execute(recipe, rows);

        // Verify output
        Assert.assertEquals(1, results.size());
        Row result = results.get(0);
        
        // Size calculations (all values in MB)
        Assert.assertEquals(30.0, (double) result.getValue("total_size_mb"), 0.0001); // 10 + 5 + 15
        Assert.assertEquals(10.0, (double) result.getValue("avg_size_mb"), 0.0001);   // 30 / 3
        Assert.assertEquals(10.0, (double) result.getValue("median_size_mb"), 0.0001); // middle value
        Assert.assertEquals(15.0, (double) result.getValue("p95_size_mb"), 0.0001);   // 95th percentile
        
        // Time calculations (all values in seconds)
        Assert.assertEquals(0.3, (double) result.getValue("total_time_sec"), 0.0001);  // 100 + 50 + 150 = 300ms
        Assert.assertEquals(0.1, (double) result.getValue("avg_time_sec"), 0.0001);    // 300 / 3 = 100ms
        Assert.assertEquals(0.15, (double) result.getValue("max_time_sec"), 0.0001);   // 150ms
        Assert.assertEquals(0.05, (double) result.getValue("min_time_sec"), 0.0001);  // 50ms
    }

    @Test
    public void testDifferentUnits() throws Exception {
        // Mixed units in input data
        List<Row> rows = Arrays.asList(
            new Row("bytes", "1KB").add("duration", "1s"),
            new Row("bytes", "1MB").add("duration", "1000ms"),
            new Row("bytes", "1GB").add("duration", "1min")
        );

        String[] recipe = new String[] {
            "aggregate-stats :bytes :duration total_bytes_gb total_duration_min"
        };

        List<Row> results = TestingRig.execute(recipe, rows);

        // Verify output
        Assert.assertEquals(1, results.size());
        Row result = results.get(0);
        
        // Total bytes: (1024 + 1048576 + 1073741824) = 1074791424 bytes ≈ 1.001GB
        Assert.assertEquals(1.001, (double) result.getValue("total_bytes_gb"), 0.001);
        
        // Total duration: (1 + 1 + 60) = 62 seconds ≈ 1.0333 minutes
        Assert.assertEquals(1.0333, (double) result.getValue("total_duration_min"), 0.0001);
    }

    @Test(expected = Exception.class)
    public void testInvalidInputFormat() throws Exception {
        List<Row> rows = Arrays.asList(
            new Row("size", "invalid").add("time", "500ms"),
            new Row("size", "2MB").add("time", "invalid")
        );

        String[] recipe = new String[] {
            "aggregate-stats :size :time total_size total_time"
        };

        TestingRig.execute(recipe, rows);
    }

    @Test
    public void testEmptyInput() throws Exception {
        List<Row> rows = Arrays.asList();

        String[] recipe = new String[] {
            "aggregate-stats :size :time total_size total_time"
        };

        List<Row> results = TestingRig.execute(recipe, rows);
        Assert.assertEquals(0, results.size());
    }
}
