/*
 * Copyright © 2021 Cask Data, Inc.
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
package io.cdap.directives.aggregates;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Optional;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates byte size and time duration fields, outputs total or average in custom units.
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate-stats")
@Categories(categories = {"aggregation", "stats"})
@Description("Aggregates byte size and time duration fields, outputs total or average in custom units.")
public class AggregateStats implements Directive {
    public static final String NAME = "aggregate-stats";
    private static final String SIZE_COL = "size_col";
    private static final String TIME_COL = "time_col";
    private static final String OUTPUT_SIZE = "output_size";
    private static final String OUTPUT_TIME = "output_time";
    private static final String SIZE_UNIT = "size_unit";
    private static final String TIME_UNIT = "time_unit";
    private static final String AGG_TYPE = "agg_type";

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
        UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
        builder.define(SIZE_COL, TokenType.COLUMN_NAME);
        builder.define(TIME_COL, TokenType.COLUMN_NAME);
        builder.define(OUTPUT_SIZE, TokenType.COLUMN_NAME);
        builder.define(OUTPUT_TIME, TokenType.COLUMN_NAME);
        builder.define(SIZE_UNIT, TokenType.TEXT, Optional.TRUE);
        builder.define(TIME_UNIT, TokenType.TEXT, Optional.TRUE);
        builder.define(AGG_TYPE, TokenType.TEXT, Optional.TRUE);
        builder.define("timeout", TokenType.TEXT, Optional.TRUE);  // Add timeout parameter
        return builder.build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        this.sizeColumn = ((ColumnName) args.value(SIZE_COL)).value();
        this.timeColumn = ((ColumnName) args.value(TIME_COL)).value();
        this.outputSizeColumn = ((ColumnName) args.value(OUTPUT_SIZE)).value();
        this.outputTimeColumn = ((ColumnName) args.value(OUTPUT_TIME)).value();

        if (args.contains(SIZE_UNIT)) {
            this.sizeUnit = args.value(SIZE_UNIT).value().toString().toLowerCase();
        }
        if (args.contains(TIME_UNIT)) {
            this.timeUnit = args.value(TIME_UNIT).value().toString().toLowerCase();
        }
        if (args.contains(AGG_TYPE)) {
            this.aggType = args.value(AGG_TYPE).value().toString().toLowerCase();
            if (!"total".equals(aggType) && !"average".equals(aggType)) {
                throw new DirectiveParseException(NAME, "agg_type must be either 'total' or 'average'");
            }
        }
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
        if (emitted) { 
            return new ArrayList<>(); 
        }
        
        for (Row row : rows) {
            Object sizeVal = row.getValue(sizeColumn);
            Object timeVal = row.getValue(timeColumn);
            
            try {
                if (sizeVal instanceof String) { 
                    totalSize += parseSize((String) sizeVal);
                } else if (sizeVal instanceof Number) {
                    totalSize += ((Number) sizeVal).longValue();
                }
                
                if (timeVal instanceof String) {
                    totalTime += parseTime((String) timeVal);
                } else if (timeVal instanceof Number) {
                    totalTime += ((Number) timeVal).longValue();
                }
                rowCount++;
            } catch (NumberFormatException e) {
                throw new DirectiveExecutionException(
                    String.format("Invalid value in row %d: %s", rowCount + 1, e.getMessage()), e);
            }
        }

        if (rowCount == 0) {
            return Collections.emptyList();
        }

        long finalSize = "average".equals(aggType) ? totalSize / rowCount : totalSize;
        long finalTime = "average".equals(aggType) ? totalTime / rowCount : totalTime;

        double convertedSize = convertSize(finalSize, sizeUnit);
        double convertedTime = convertTime(finalTime, timeUnit);

        Row output = new Row();
        output.add(outputSizeColumn, convertedSize);
        output.add(outputTimeColumn, convertedTime);

        emitted = true;
        return Collections.singletonList(output);
    }

    private long parseSize(String val) throws NumberFormatException {
        val = val.trim().toLowerCase();
        try {
            if (val.endsWith("kb")) { 
                return (long) (Double.parseDouble(val.replace("kb", "")) * 1024);
            }
            if (val.endsWith("mb")) {
                return (long) (Double.parseDouble(val.replace("mb", "")) * 1024 * 1024);
            }
            if (val.endsWith("gb")) {
                return (long) (Double.parseDouble(val.replace("gb", "")) * 1024 * 1024 * 1024);
            }
            if (val.endsWith("b")) {
                return (long) (Double.parseDouble(val.replace("b", "")));
            }
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid size format: " + val);
        }
    }

    private long parseTime(String val) throws NumberFormatException {
        val = val.trim().toLowerCase();
        try {
            if (val.endsWith("ms")) { 
                return (long) (Double.parseDouble(val.replace("ms", "")) * 1_000_000);
            }
            if (val.endsWith("s")) { 
                return (long) (Double.parseDouble(val.replace("s", "")) * 1_000_000_000);
            }
            if (val.endsWith("min")) {
                return (long) (Double.parseDouble(val.replace("min", "")) * 60L * 1_000_000_000);
            }
            if (val.endsWith("ns")) {
                return (long) (Double.parseDouble(val.replace("ns", "")));
            }
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid time format: " + val);
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
        // Clean up if needed
    }
}