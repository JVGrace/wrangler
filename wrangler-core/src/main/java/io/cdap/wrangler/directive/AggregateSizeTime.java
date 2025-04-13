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

package io.cdap.wrangler.directive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

/**
 * Class description here.
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate-size-time")
@Description("Aggregates byte size and time duration fields, outputs total or average in custom units.")
public class AggregateSizeTime implements Directive {
  private String sizeColumn;
  private String timeColumn;
  private String outputSizeColumn;
  private String outputTimeColumn;
  private String sizeUnit = "b";
  private String timeUnit = "ns";
  private String aggType = "total";
  private long totalSize = 0;
  private long totalTime = 0;
  private int rowCount = 0;
  private boolean emitted = false;
  
  @Override
  public UsageDefinition define() {
	  UsageDefinition.Builder builder = UsageDefinition.builder("aggregate-size-time");
	  builder.define("size_col", TokenType.COLUMN_NAME);
	  builder.define("time_col", TokenType.COLUMN_NAME);
	  builder.define("output_size", TokenType.COLUMN_NAME);
	  builder.define("output_time", TokenType.COLUMN_NAME);
	  builder.define("size_unit", TokenType.TEXT, true);
	  builder.define("time_unit", TokenType.TEXT, true);
	  builder.define("agg_type", TokenType.TEXT, true);
	  return builder.build();
    
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    sizeColumn = args.value("size_col");
    timeColumn = args.value("time_col");
    outputSizeColumn = args.value("output_size");
    outputTimeColumn = args.value("output_time");

    if (args.contains("size_unit")) {
    	  sizeUnit = args.value("size_unit").toString().toLowerCase();
    	}
    	if (args.contains("time_unit")) {
    	  timeUnit = args.value("time_unit").toString().toLowerCase();
    	}
    	if (args.contains("agg_type")) {
    	  aggType = args.value("agg_type").toString().toLowerCase();
    	}
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    if (emitted) { return new ArrayList<>();} // Prevent duplicate results on re-execution
    for (Row row : rows) {
      Object sizeVal = row.getValue(sizeColumn);
      Object timeVal = row.getValue(timeColumn);
      if (sizeVal instanceof String) { totalSize += parseSize((String) sizeVal);}
      if (timeVal instanceof String) {totalTime += parseTime((String) timeVal);}
      rowCount++;
    }

    long finalSize = aggType.equals("average") ? totalSize / rowCount : totalSize;
    long finalTime = aggType.equals("average") ? totalTime / rowCount : totalTime;

    double convertedSize = convertSize(finalSize, sizeUnit);
    double convertedTime = convertTime(finalTime, timeUnit);

    Row output = new Row();
    output.add(outputSizeColumn, convertedSize);
    output.add(outputTimeColumn, convertedTime);

    emitted = true;
    return Collections.singletonList(output);
  }

  private long parseSize(String val) {
    val = val.trim().toLowerCase();
    try {
      if (val.endsWith("kb")) { return (long)(Double.parseDouble(val.replace("kb", "")) * 1024);}
      if (val.endsWith("mb")) {return (long)(Double.parseDouble(val.replace("mb", "")) * 1024 * 1024);}
      if (val.endsWith("gb")) {return (long)(Double.parseDouble(val.replace("gb", "")) * 1024 * 1024 * 1024);}
      if (val.endsWith("b")) {return (long)(Double.parseDouble(val.replace("b", "")));}
      return Long.parseLong(val);
    } catch (Exception e) {
      return 0;
    }
  }

  private long parseTime(String val) {
    val = val.trim().toLowerCase();
    try {
      if (val.endsWith("ms")) { return (long)(Double.parseDouble(val.replace("ms", "")) * 1_000_000);}
      if (val.endsWith("s")) { return (long)(Double.parseDouble(val.replace("s", "")) * 1_000_000_000);}
      if (val.endsWith("ns")) {return (long)(Double.parseDouble(val.replace("ns", "")));}
      return Long.parseLong(val);
    } catch (Exception e) {
      return 0;
    }
  }

  private double convertSize(long size, String unit) {
    switch (unit) {
      case "kb": return size / 1024.0;
      case "mb": return size / (1024.0 * 1024);
      case "gb": return size / (1024.0 * 1024 * 1024);
      default: return size;
    }
  }

  private double convertTime(long time, String unit) {
    switch (unit) {
      case "ms": return time / 1_000_000.0;
      case "s": return time / 1_000_000_000.0;
      case "min": return time / (60.0 * 1_000_000_000);
      default: return time;
    }
  }

@Override
public void destroy() {
	// TODO Auto-generated method stub
	
}
}
