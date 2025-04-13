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

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.concurrent.TimeUnit;

/**
 * Token implementation for time duration strings like '10s', '5min', '3h', '2d', etc.
 * Converts to milliseconds.
 */
public class TimeDuration implements Token {
  /**
   *  
   */
private static final long serialVersionUID = 1L;
private final String original;
  private final long milliseconds;

  public TimeDuration(String value) {
    this.original = value;
    this.milliseconds = parse(value);
  }

  private long parse(String input) {
    input = input.trim().toLowerCase();
    double number;
    try {
      if (input.endsWith("ms")) {
        number = Double.parseDouble(input.replace("ms", ""));
        return (long) number;
      } else if (input.endsWith("s")) {
        number = Double.parseDouble(input.replace("s", ""));
        return (long) (number * 1000);
      } else if (input.endsWith("min")) {
        number = Double.parseDouble(input.replace("min", ""));
        return (long) (number * 60 * 1000);
      } else if (input.endsWith("h")) {
        number = Double.parseDouble(input.replace("h", ""));
        return (long) (number * 60 * 60 * 1000);
      } else if (input.endsWith("d")) {
        number = Double.parseDouble(input.replace("d", ""));
        return (long) (number * 24 * 60 * 60 * 1000);
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid time duration format: " + input);
    }
    throw new IllegalArgumentException("Invalid time duration unit in: " + input);
  }

  public long getMilliseconds() {
    return milliseconds;
  }

  @Override
  public Object value() {
    return milliseconds;
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(milliseconds);
  }

  @Override
  public String toString() {
    return original;
  }
}
