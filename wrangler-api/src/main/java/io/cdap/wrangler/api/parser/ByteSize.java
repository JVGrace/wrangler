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

import io.cdap.wrangler.api.annotations.PublicEvolving;

/**
 * Class description here.
 */
@PublicEvolving
public class ByteSize implements Token {
  /**
   *  
   */
private static final long serialVersionUID = 1L;
private final String original;
  private final long bytes;

  public ByteSize(String value) {
    this.original = value;
    this.bytes = parse(value);
  }

  private long parse(String input) {
    input = input.trim().toUpperCase();
    double number;
    try {
      if (input.endsWith("PB")) {
        number = Double.parseDouble(input.replace("PB", ""));
        return (long) (number * 1024L * 1024 * 1024 * 1024 * 1024);
      } else if (input.endsWith("TB")) {
        number = Double.parseDouble(input.replace("TB", ""));
        return (long) (number * 1024L * 1024 * 1024 * 1024);
      } else if (input.endsWith("GB")) {
        number = Double.parseDouble(input.replace("GB", ""));
        return (long) (number * 1024L * 1024 * 1024);
      } else if (input.endsWith("MB")) {
        number = Double.parseDouble(input.replace("MB", ""));
        return (long) (number * 1024L * 1024);
      } else if (input.endsWith("KB")) {
        number = Double.parseDouble(input.replace("KB", ""));
        return (long) (number * 1024L);
      } else if (input.endsWith("B")) {
        number = Double.parseDouble(input.replace("B", ""));
        return (long) number;
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid byte size format: " + input);
    }
    throw new IllegalArgumentException("Invalid byte size unit in: " + input);
  }

  public long getBytes() {
    return bytes;
  }

  @Override
  public Object value() {
    return bytes;
  }

  @Override
  public TokenType type() {
    return TokenType.BYTE_SIZE;
  }

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(bytes);
  }

  @Override
  public String toString() {
    return original;
  }
}
