/*
 * Copyright © 2021 Pradumn Patel
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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.Row;

/**
 * Tests for {@link io.cdap.directives.aggregation.AggregateStats} directive.
 */
public class AggregateStatsTest {
	
	@Test
    public void testAggregateStats() throws Exception {
        List<Row> rows = Arrays.asList();

        String[] recipe = new String[] {
            "aggregate-stats :size :time :total_size :total_time"
        };

        List<Row> results = TestingRig.execute(recipe, rows);
        assertEquals(0, results.size());
    }

	@Test(expected = Exception.class)
	public void testInvalidInputFormat() throws Exception {
		List<Row> rows = Arrays.asList(new Row("size", "invalid").add("time", "500ms"),
				new Row("size", "2MB").add("time", "invalid"));

		String[] recipe = new String[] { "aggregate-stats :size :time total_size total_time" };

		TestingRig.execute(recipe, rows);
	}

	@Test
    public void testBasicAggregation() throws Exception {
        List<Row> rows = Arrays.asList(
            new Row("data_transfer_size", "1MB").add("response_time", "500ms"),
            new Row("data_transfer_size", "2.5MB").add("response_time", "1.2s"),
            new Row("data_transfer_size", "500KB").add("response_time", "300ms")
        );

        // Proper directive syntax with all required parameters
        String[] recipe = new String[] {
        		"aggregate-stats :data_transfer_size :response_time :total_size_mb :total_time_sec size_unit:mb time_unit:s"
            };

        List<Row> results = TestingRig.execute(recipe, rows);
        
        assertEquals(1, results.size());
        Row result = results.get(0);
        
        // Expected: (1MB + 2.5MB + 500KB) in MB
        assertEquals(3.98828125, (double) result.getValue("total_size_mb"), 0.0001);
        
        // Expected: (500ms + 1200ms + 300ms) in seconds
        assertEquals(2.0, (double) result.getValue("total_time_sec"), 0.0001);
    }

	@Test
	public void testAverageAggregation() throws Exception {
	    List<Row> rows = Arrays.asList(
	        new Row("file_size", "10 MB").add("processing_time", "100 ms"),
	        new Row("file_size", "5 MB").add("processing_time", "50 ms"),
	        new Row("file_size", "15 MB").add("processing_time", "150 ms")
	    );

	    String[] recipe = new String[] {
	        "aggregate-stats :file_size :processing_time :avg_size_mb :avg_time_ms agg_type:average"
	    };

        List<Row> results = TestingRig.execute(recipe, rows);

        assertEquals(1, results.size());
        Row result = results.get(0);
        
        assertEquals(10.0, (double) result.getValue("avg_size_mb"), 0.0001);
        assertEquals(100.0, (double) result.getValue("avg_time_ms"), 0.0001);
    }

	@Test
	public void testDifferentUnits() throws Exception {
	    List<Row> rows = Arrays.asList(
	        new Row("bytes", "1KB").add("duration", "1s"),
	        new Row("bytes", "1MB").add("duration", "1000ms"),
	        new Row("bytes", "1GB").add("duration", "1min")
	    );

	    // Fixed: Added colons before output column names
	    String[] recipe = new String[] {
	        "aggregate-stats :bytes :duration :total_bytes_gb :total_duration_min"
	    };

	    List<Row> results = TestingRig.execute(recipe, rows);
	    assertEquals(1, results.size());
	    Row result = results.get(0);
	    
	    assertEquals(1.001, (double) result.getValue("total_bytes_gb"), 0.001);
	    assertEquals(1.0333, (double) result.getValue("total_duration_min"), 0.0001);
	}
    
}