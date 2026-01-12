/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.server.front.model;

import java.beans.ConstructorProperties;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public final class Schema {

  private final Type type;

  private final Schema elementSchema;

  private final List<Entry> entries;

  private final List<Entry> metadata;

  private final Map<String, String> props;

  @ConstructorProperties({"type", "elementSchema", "entries", "metadata", "props"})
  public Schema(
          final Type type,
          final Schema elementSchema,
          final List<Entry> entries,
          final List<Entry> metadata,
          final Map<String, String> props) {
    this.type = type;
    this.elementSchema = elementSchema;
    this.entries = entries;
    this.metadata = metadata;
    this.props = props;
  }

  public String getProp(final String key){
      return this.props.get(key);
  }

  public enum Type {

    RECORD(new Class<?>[] { Record.class }),
    ARRAY(new Class<?>[] { Collection.class }),
    STRING(new Class<?>[] { String.class, Object.class }),
    BYTES(new Class<?>[] { byte[].class, Byte[].class }),
    INT(new Class<?>[] { Integer.class }),
    LONG(new Class<?>[] { Long.class }),
    FLOAT(new Class<?>[] { Float.class }),
    DOUBLE(new Class<?>[] { Double.class }),
    BOOLEAN(new Class<?>[] { Boolean.class }),
    DATETIME(new Class<?>[] { Long.class, Date.class, Temporal.class }),
    DECIMAL(new Class<?>[] { BigDecimal.class });

    /**
     * All compatibles Java classes
     */
    private final Class<?>[] classes;

    Type(final Class<?>[] classes) {
      this.classes = classes;
    }

    /**
     * Check if input can be affected to an entry of this type.
     *
     * @param input : object.
     *
     * @return true if input is null or ok.
     */
    public boolean isCompatible(final Object input) {
      if (input == null) {
        return true;
      }
      for (final Class<?> clazz : classes) {
        if (clazz.isInstance(input)) {
          return true;
        }
      }
      return false;
    }
  }
}