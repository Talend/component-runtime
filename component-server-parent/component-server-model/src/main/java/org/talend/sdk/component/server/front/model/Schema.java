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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;

public final class Schema {

  @Getter
  private final Type type;

  @Getter
  private final Schema elementSchema;

  @Getter
  private final List<Entry> entries;

  @Getter
  private final List<Entry> metadata;

  @Getter
  private final Map<String, String> props;

  @ConstructorProperties({"type", "elementSchema", "entries", "metadata", "props"})
  private Schema(
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

  public final Schema withType(final Type value) {
    Type newValue = Objects.requireNonNull(value, "type");
    if (this.type == newValue) {
      return this;
    }
    return new Schema(
        newValue,
        this.elementSchema,
        this.entries,
        this.metadata,
        this.props);
  }

  public final Schema withElementSchema(final Schema value) {
    if (this.elementSchema == value) {
      return this;
    }
    Schema newValue = Objects.requireNonNull(value, "elementSchema");
    return new Schema(this.type, newValue, this.entries, this.metadata, this.props);
  }

  public final Schema withEntries(final Entry... elements) {
    List<Entry> newValue = createUnmodifiableList(false, createSafeList(Arrays.asList(elements), true, false));
    return new Schema(
        this.type,
        this.elementSchema,
        newValue,
        this.metadata,
        this.props);
  }

  public final Schema withEntries(final Iterable<? extends Entry> elements) {
    if (this.entries == elements) {
      return this;
    }
    List<Entry> newValue = createUnmodifiableList(false, createSafeList(elements, true, false));
    return new Schema(
        this.type,
        this.elementSchema,
        newValue,
        this.metadata,
        this.props);
  }

  public final Schema withMetadata(final Entry... elements) {
    List<Entry> newValue = createUnmodifiableList(false, createSafeList(Arrays.asList(elements), true, false));
    return new Schema(
        this.type,
        this.elementSchema,
        this.entries,
        newValue,
        this.props);
  }

  public final Schema withMetadata(final Iterable<? extends Entry> elements) {
    if (this.metadata == elements) {
      return this;
    }
    List<Entry> newValue = createUnmodifiableList(false, createSafeList(elements, true, false));
    return new Schema(
        this.type,
        this.elementSchema,
        this.entries,
        newValue,
        this.props);
  }

  public final Schema withProps(final Map<String, ? extends String> entries) {
    if (this.props == entries) {
      return this;
    }
    Map<String, String> newValue = createUnmodifiableMap(true, false, entries);
    return new Schema(
        this.type,
        this.elementSchema,
        this.entries,
        this.metadata,
        newValue);
  }

  @Override
  public boolean equals(final Object another) {
    if (this == another) {
      return true;
    }
    return another instanceof Schema
        && equalTo(0, (Schema) another);
  }

  private boolean equalTo(final int synthetic, final Schema another) {
    return type.equals(another.type)
        && elementSchema.equals(another.elementSchema)
        && entries.equals(another.entries)
        && metadata.equals(another.metadata)
        && props.equals(another.props);
  }

  public int hashCode() {
    int h = 5381;
    h += (h << 5) + type.hashCode();
    h += (h << 5) + elementSchema.hashCode();
    h += (h << 5) + entries.hashCode();
    h += (h << 5) + metadata.hashCode();
    h += (h << 5) + props.hashCode();
    return h;
  }

  @Override
  public String toString() {
    return "Schema{"
        + "type=" + type
        + ", elementSchema=" + elementSchema
        + ", entries=" + entries
        + ", metadata=" + metadata
        + ", props=" + props
        + "}";
  }

  public static Schema copyOf(final Schema instance) {
    if (instance != null && instance instanceof Schema) {
      return (Schema) instance;
    }
    return Schema.builder()
        .from(instance)
        .build();
  }

  public static Schema.Builder builder() {
    return new Schema.Builder();
  }

  public static final class Builder {
    private static final long INIT_BIT_TYPE = 0x1L;
    private static final long INIT_BIT_ELEMENT_SCHEMA = 0x2L;
    private static final long INIT_BIT_ALL_ENTRIES = 0x4L;
    private long initBits = 0x7L;

    private Type type;
    private Schema elementSchema;
    private List<Entry> entries = new ArrayList<Entry>();
    private List<Entry> metadata = new ArrayList<Entry>();
    private Map<String, String> props = new LinkedHashMap<String, String>();

    private Builder() {
    }

    public final Builder from(final Schema instance) {
      Objects.requireNonNull(instance, "instance");
      from((Object) instance);
      return this;
    }

    private void from(final Object object) {
      long bits = 0;
      if (object instanceof Schema) {
        Schema instance = (Schema) object;
        if ((bits & 0x1L) == 0) {
          this.elementSchema(instance.getElementSchema());
          bits |= 0x1L;
        }
        if ((bits & 0x2L) == 0) {
          addAllMetadata(instance.getMetadata());
          bits |= 0x2L;
        }
        if ((bits & 0x20L) == 0) {
          addAllEntries(instance.getEntries());
          bits |= 0x20L;
        }
        if ((bits & 0x8L) == 0) {
          this.type(instance.getType());
          bits |= 0x8L;
        }
        if ((bits & 0x10L) == 0) {
          putAllProps(instance.getProps());
          bits |= 0x10L;
        }
      }
      if (object instanceof Schema) {
        Schema instance = (Schema) object;
        if ((bits & 0x1L) == 0) {
          this.elementSchema(instance.getElementSchema());
          bits |= 0x1L;
        }
        if ((bits & 0x2L) == 0) {
          addAllMetadata(instance.getMetadata());
          bits |= 0x2L;
        }
        if ((bits & 0x20L) == 0) {
          addAllEntries(instance.getEntries());
          bits |= 0x20L;
        }
        if ((bits & 0x8L) == 0) {
          this.type(instance.getType());
          bits |= 0x8L;
        }
      }
    }

    public final Builder type(final Type type) {
      this.type = Objects.requireNonNull(type, "type");
      initBits &= ~INIT_BIT_TYPE;
      return this;
    }

    public final Builder elementSchema(final Schema elementSchema) {
      this.elementSchema = Objects.requireNonNull(elementSchema, "elementSchema");
      initBits &= ~INIT_BIT_ELEMENT_SCHEMA;
      return this;
    }

    public final Builder addEntries(final Entry element) {
      this.entries.add(Objects.requireNonNull(element, "entries element"));
      return this;
    }

    public final Builder addEntries(final Entry... elements) {
      for (Entry element : elements) {
        this.entries.add(Objects.requireNonNull(element, "entries element"));
      }
      return this;
    }

    public final Builder entries(final Iterable<? extends Entry> elements) {
      this.entries.clear();
      return addAllEntries(elements);
    }

    public final Builder addAllEntries(final Iterable<? extends Entry> elements) {
      for (Entry element : elements) {
        this.entries.add(Objects.requireNonNull(element, "entries element"));
      }
      return this;
    }

    public final Builder addMetadata(final Entry element) {
      this.metadata.add(Objects.requireNonNull(element, "metadata element"));
      return this;
    }

    public final Builder addMetadata(final Entry... elements) {
      for (Entry element : elements) {
        this.metadata.add(Objects.requireNonNull(element, "metadata element"));
      }
      return this;
    }

    public final Builder metadata(final Iterable<? extends Entry> elements) {
      this.metadata.clear();
      return addAllMetadata(elements);
    }

    public final Builder addAllMetadata(final Iterable<? extends Entry> elements) {
      for (Entry element : elements) {
        this.metadata.add(Objects.requireNonNull(element, "metadata element"));
      }
      return this;
    }

    public final Builder putProps(final String key, final String value) {
      this.props.put(
          Objects.requireNonNull(key, "props key"),
          Objects.requireNonNull(value, value == null ? "props value for key: " + key : null));
      return this;
    }

    public final Builder putProps(final Map.Entry<String, ? extends String> entry) {
      String k = entry.getKey();
      String v = entry.getValue();
      this.props.put(
          Objects.requireNonNull(k, "props key"),
          Objects.requireNonNull(v, v == null ? "props value for key: " + k : null));
      return this;
    }

    public final Builder props(final Map<String, ? extends String> entries) {
      this.props.clear();
      return putAllProps(entries);
    }

    public final Builder putAllProps(final Map<String, ? extends String> entries) {
      for (Map.Entry<String, ? extends String> e : entries.entrySet()) {
        String k = e.getKey();
        String v = e.getValue();
        this.props.put(
            Objects.requireNonNull(k, "props key"),
            Objects.requireNonNull(v, v == null ? "props value for key: " + k : null));
      }
      return this;
    }

    public Schema build() {
      return new Schema(
          type,
          elementSchema,
          createUnmodifiableList(true, entries),
          createUnmodifiableList(true, metadata),
          createUnmodifiableMap(false, false, props));
    }

  }

  private static <T> List<T> createSafeList(final Iterable<? extends T> iterable, final boolean checkNulls, final boolean skipNulls) {
    ArrayList<T> list;
    if (iterable instanceof Collection<?>) {
      int size = ((Collection<?>) iterable).size();
      if (size == 0) {
        return Collections.emptyList();
      }
      list = new ArrayList<>(size);
    } else {
      list = new ArrayList<>();
    }
    for (T element : iterable) {
      if (skipNulls && element == null) {
        continue;
      }
      if (checkNulls) {
        Objects.requireNonNull(element, "element");
      }
      list.add(element);
    }
    return list;
  }

  private static <T> List<T> createUnmodifiableList(final boolean clone, final List<T> list) {
    switch(list.size()) {
    case 0: return Collections.emptyList();
    case 1: return Collections.singletonList(list.get(0));
    default:
      if (clone) {
        return Collections.unmodifiableList(new ArrayList<>(list));
      } else {
        if (list instanceof ArrayList<?>) {
          ((ArrayList<?>) list).trimToSize();
        }
        return Collections.unmodifiableList(list);
      }
    }
  }

  private static <K, V> Map<K, V> createUnmodifiableMap(final boolean checkNulls, final boolean skipNulls,
                                                        final Map<? extends K, ? extends V> map) {
    switch (map.size()) {
    case 0: return Collections.emptyMap();
    case 1: {
      Map.Entry<? extends K, ? extends V> e = map.entrySet().iterator().next();
      K k = e.getKey();
      V v = e.getValue();
      if (checkNulls) {
        Objects.requireNonNull(k, "key");
        Objects.requireNonNull(v, v == null ? "value for key: " + k : null);
      }
      if (skipNulls && (k == null || v == null)) {
        return Collections.emptyMap();
      }
      return Collections.singletonMap(k, v);
    }
    default: {
      Map<K, V> linkedMap = new LinkedHashMap<>(map.size() * 4 / 3 + 1);
      if (skipNulls || checkNulls) {
        for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
          K k = e.getKey();
          V v = e.getValue();
          if (skipNulls) {
            if (k == null || v == null) {
              continue;
            }
          } else if (checkNulls) {
            Objects.requireNonNull(k, "key");
            Objects.requireNonNull(v, v == null ? "value for key: " + k : null);
          }
          linkedMap.put(k, v);
        }
      } else {
        linkedMap.putAll(map);
      }
      return Collections.unmodifiableMap(linkedMap);
    }
    }
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