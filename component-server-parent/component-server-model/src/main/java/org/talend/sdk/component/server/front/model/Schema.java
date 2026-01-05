package org.talend.sdk.component.server.front.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class Schema implements org.talend.sdk.component.api.record.Schema {
  private final Type type;
  private final org.talend.sdk.component.api.record.Schema elementSchema;
  private final List<Entry> entries;
  private final List<Entry> metadata;
  private final Stream<Entry> allEntries;
  private final Map<String, String> props;
  private final Map<String, String> properties;

  private Schema(
      Type type,
      org.talend.sdk.component.api.record.Schema elementSchema,
      List<Entry> entries,
      List<Entry> metadata,
      Stream<Entry> allEntries,
      Map<String, String> props,
      Map<String, String> properties) {
    this.type = type;
    this.elementSchema = elementSchema;
    this.entries = entries;
    this.metadata = metadata;
    this.allEntries = allEntries;
    this.props = props;
    this.properties = properties;
  }

  @Override
  public String getProp(String key){
      return this.getProperties().get(key);
  }

  /**
   * @return The value of the {@code type} attribute
   */
  @Override
  public Type getType() {
    return type;
  }

  /**
   * @return The value of the {@code elementSchema} attribute
   */
  @Override
  public org.talend.sdk.component.api.record.Schema getElementSchema() {
    return elementSchema;
  }

  /**
   * @return The value of the {@code entries} attribute
   */
  @Override
  public List<Entry> getEntries() {
    return entries;
  }

  /**
   * @return The value of the {@code metadata} attribute
   */
  @Override
  public List<Entry> getMetadata() {
    return metadata;
  }

  /**
   * @return The value of the {@code allEntries} attribute
   */
  @Override
  public Stream<Entry> getAllEntries() {
    return allEntries;
  }

  /**
   * @return The value of the {@code props} attribute
   */
  @Override
  public Map<String, String> getProps() {
    return props;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Schema#getType() type} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for type
   * @return A modified copy of the {@code this} object
   */
  public final Schema withType(Type value) {
    Type newValue = Objects.requireNonNull(value, "type");
    if (this.type == newValue) return this;
    return new Schema(
        newValue,
        this.elementSchema,
        this.entries,
        this.metadata,
        this.allEntries,
        this.props,
        this.properties);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Schema#getElementSchema() elementSchema} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for elementSchema
   * @return A modified copy of the {@code this} object
   */
  public final Schema withElementSchema(org.talend.sdk.component.api.record.Schema value) {
    if (this.elementSchema == value) return this;
    org.talend.sdk.component.api.record.Schema newValue = Objects.requireNonNull(value, "elementSchema");
    return new Schema(this.type, newValue, this.entries, this.metadata, this.allEntries, this.props, this.properties);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link Schema#getEntries() entries}.
   * @param elements The elements to set
   * @return A modified copy of {@code this} object
   */
  public final Schema withEntries(Entry... elements) {
    List<Entry> newValue = createUnmodifiableList(false, createSafeList(Arrays.asList(elements), true, false));
    return new Schema(
        this.type,
        this.elementSchema,
        newValue,
        this.metadata,
        this.allEntries,
        this.props,
        this.properties);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link Schema#getEntries() entries}.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param elements An iterable of entries elements to set
   * @return A modified copy of {@code this} object
   */
  public final Schema withEntries(Iterable<? extends Entry> elements) {
    if (this.entries == elements) return this;
    List<Entry> newValue = createUnmodifiableList(false, createSafeList(elements, true, false));
    return new Schema(
        this.type,
        this.elementSchema,
        newValue,
        this.metadata,
        this.allEntries,
        this.props,
        this.properties);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link Schema#getMetadata() metadata}.
   * @param elements The elements to set
   * @return A modified copy of {@code this} object
   */
  public final Schema withMetadata(Entry... elements) {
    List<Entry> newValue = createUnmodifiableList(false, createSafeList(Arrays.asList(elements), true, false));
    return new Schema(
        this.type,
        this.elementSchema,
        this.entries,
        newValue,
        this.allEntries,
        this.props,
        this.properties);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link Schema#getMetadata() metadata}.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param elements An iterable of metadata elements to set
   * @return A modified copy of {@code this} object
   */
  public final Schema withMetadata(Iterable<? extends Entry> elements) {
    if (this.metadata == elements) return this;
    List<Entry> newValue = createUnmodifiableList(false, createSafeList(elements, true, false));
    return new Schema(
        this.type,
        this.elementSchema,
        this.entries,
        newValue,
        this.allEntries,
        this.props,
        this.properties);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Schema#getAllEntries() allEntries} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for allEntries
   * @return A modified copy of the {@code this} object
   */
  public final Schema withAllEntries(Stream<Entry> value) {
    if (this.allEntries == value) return this;
    Stream<Entry> newValue = Objects.requireNonNull(value, "allEntries");
    return new Schema(
        this.type,
        this.elementSchema,
        this.entries,
        this.metadata,
        newValue,
        this.props,
        this.properties);
  }

  /**
   * Copy the current immutable object by replacing the {@link Schema#getProps() props} map with the specified map.
   * Nulls are not permitted as keys or values.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param entries The entries to be added to the props map
   * @return A modified copy of {@code this} object
   */
  public final Schema withProps(Map<String, ? extends String> entries) {
    if (this.props == entries) return this;
    Map<String, String> newValue = createUnmodifiableMap(true, false, entries);
    return new Schema(
        this.type,
        this.elementSchema,
        this.entries,
        this.metadata,
        this.allEntries,
        newValue,
        this.properties);
  }

  /**
   * Copy the current immutable object by replacing the {@link Schema#getProperties() properties} map with the specified map.
   * Nulls are not permitted as keys or values.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param entries The entries to be added to the properties map
   * @return A modified copy of {@code this} object
   */
  public final Schema withProperties(Map<String, ? extends String> entries) {
    if (this.properties == entries) return this;
    Map<String, String> newValue = createUnmodifiableMap(true, false, entries);
    return new Schema(
        this.type,
        this.elementSchema,
        this.entries,
        this.metadata,
        this.allEntries,
        this.props,
        newValue);
  }

  /**
   * This instance is equal to all instances of {@code SchemaDetail} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(Object another) {
    if (this == another) return true;
    return another instanceof Schema
        && equalTo(0, (Schema) another);
  }

  private boolean equalTo(int synthetic, Schema another) {
    return type.equals(another.type)
        && elementSchema.equals(another.elementSchema)
        && entries.equals(another.entries)
        && metadata.equals(another.metadata)
        && allEntries.equals(another.allEntries)
        && props.equals(another.props)
        && properties.equals(another.properties);
  }

  /**
   * Computes a hash code from attributes: {@code type}, {@code elementSchema}, {@code entries}, {@code metadata}, {@code allEntries}, {@code props}, {@code properties}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 5381;
    h += (h << 5) + type.hashCode();
    h += (h << 5) + elementSchema.hashCode();
    h += (h << 5) + entries.hashCode();
    h += (h << 5) + metadata.hashCode();
    h += (h << 5) + allEntries.hashCode();
    h += (h << 5) + props.hashCode();
    h += (h << 5) + properties.hashCode();
    return h;
  }

  /**
   * Prints the immutable value {@code Schema} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return "Schema{"
        + "type=" + type
        + ", elementSchema=" + elementSchema
        + ", entries=" + entries
        + ", metadata=" + metadata
        + ", allEntries=" + allEntries
        + ", props=" + props
        + ", properties=" + properties
        + "}";
  }

  /**
   * Creates an immutable copy of a {@link Schema} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable Schema instance
   */
  public static Schema copyOf(Schema instance) {
    if (instance instanceof Schema) {
      return (Schema) instance;
    }
    return Schema.builder()
        .from(instance)
        .build();
  }

  /**
   * Creates a builder for {@link Schema SchemaDetail}.
   * <pre>
   * SchemaDetail.builder()
   *    .type(org.talend.sdk.component.api.record.Schema.Type) // required {@link Schema#getType() type}
   *    .elementSchema(org.talend.sdk.component.api.record.Schema) // required {@link Schema#getElementSchema() elementSchema}
   *    .addEntries|addAllEntries(org.talend.sdk.component.api.record.Schema.Entry) // {@link Schema#getEntries() entries} elements
   *    .addMetadata|addAllMetadata(org.talend.sdk.component.api.record.Schema.Entry) // {@link Schema#getMetadata() metadata} elements
   *    .allEntries(stream.Stream&amp;lt;org.talend.sdk.component.api.record.Schema.Entry&amp;gt;) // required {@link Schema#getAllEntries() allEntries}
   *    .putProps|putAllProps(String =&gt; String) // {@link Schema#getProps() props} mappings
   *    .putProperties|putAllProperties(String =&gt; String) // {@link Schema#getProperties() properties} mappings
   *    .build();
   * </pre>
   * @return A new SchemaDetail builder
   */
  public static Schema.Builder builder() {
    return new Schema.Builder();
  }

  /**
   * Builds instances of type {@link Schema SchemaDetail}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  public static final class Builder {
    private static final long INIT_BIT_TYPE = 0x1L;
    private static final long INIT_BIT_ELEMENT_SCHEMA = 0x2L;
    private static final long INIT_BIT_ALL_ENTRIES = 0x4L;
    private long initBits = 0x7L;

    private Type type;
    private org.talend.sdk.component.api.record.Schema elementSchema;
    private List<Entry> entries = new ArrayList<Entry>();
    private List<Entry> metadata = new ArrayList<Entry>();
    private Stream<Entry> allEntries;
    private Map<String, String> props = new LinkedHashMap<String, String>();
    private Map<String, String> properties = new LinkedHashMap<String, String>();

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code org.talend.sdk.component.api.record.Schema} instance.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(org.talend.sdk.component.api.record.Schema instance) {
      Objects.requireNonNull(instance, "instance");
      from((short) 0, (Object) instance);
      return this;
    }

    /**
     * Fill a builder with attribute values from the provided {@code org.talend.sdk.component.server.front.model.Schema} instance.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(Schema instance) {
      Objects.requireNonNull(instance, "instance");
      from((short) 0, (Object) instance);
      return this;
    }

    private void from(short _unused, Object object) {
      long bits = 0;
      if (object instanceof org.talend.sdk.component.api.record.Schema) {
        org.talend.sdk.component.api.record.Schema instance = (org.talend.sdk.component.api.record.Schema) object;
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
        if ((bits & 0x4L) == 0) {
          this.allEntries(instance.getAllEntries());
          bits |= 0x4L;
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
        if ((bits & 0x4L) == 0) {
          this.allEntries(instance.getAllEntries());
          bits |= 0x4L;
        }
        if ((bits & 0x8L) == 0) {
          this.type(instance.getType());
          bits |= 0x8L;
        }
        putAllProperties(instance.getProperties());
        if ((bits & 0x10L) == 0) {
          putAllProps(instance.getProps());
          bits |= 0x10L;
        }
      }
    }

    /**
     * Initializes the value for the {@link Schema#getType() type} attribute.
     * @param type The value for type 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder type(Type type) {
      this.type = Objects.requireNonNull(type, "type");
      initBits &= ~INIT_BIT_TYPE;
      return this;
    }

    /**
     * Initializes the value for the {@link Schema#getElementSchema() elementSchema} attribute.
     * @param elementSchema The value for elementSchema 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder elementSchema(org.talend.sdk.component.api.record.Schema elementSchema) {
      this.elementSchema = Objects.requireNonNull(elementSchema, "elementSchema");
      initBits &= ~INIT_BIT_ELEMENT_SCHEMA;
      return this;
    }

    /**
     * Adds one element to {@link Schema#getEntries() entries} list.
     * @param element A entries element
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addEntries(Entry element) {
      this.entries.add(Objects.requireNonNull(element, "entries element"));
      return this;
    }

    /**
     * Adds elements to {@link Schema#getEntries() entries} list.
     * @param elements An array of entries elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addEntries(Entry... elements) {
      for (Entry element : elements) {
        this.entries.add(Objects.requireNonNull(element, "entries element"));
      }
      return this;
    }


    /**
     * Sets or replaces all elements for {@link Schema#getEntries() entries} list.
     * @param elements An iterable of entries elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder entries(Iterable<? extends Entry> elements) {
      this.entries.clear();
      return addAllEntries(elements);
    }

    /**
     * Adds elements to {@link Schema#getEntries() entries} list.
     * @param elements An iterable of entries elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addAllEntries(Iterable<? extends Entry> elements) {
      for (Entry element : elements) {
        this.entries.add(Objects.requireNonNull(element, "entries element"));
      }
      return this;
    }

    /**
     * Adds one element to {@link Schema#getMetadata() metadata} list.
     * @param element A metadata element
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addMetadata(Entry element) {
      this.metadata.add(Objects.requireNonNull(element, "metadata element"));
      return this;
    }

    /**
     * Adds elements to {@link Schema#getMetadata() metadata} list.
     * @param elements An array of metadata elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addMetadata(Entry... elements) {
      for (Entry element : elements) {
        this.metadata.add(Objects.requireNonNull(element, "metadata element"));
      }
      return this;
    }


    /**
     * Sets or replaces all elements for {@link Schema#getMetadata() metadata} list.
     * @param elements An iterable of metadata elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder metadata(Iterable<? extends Entry> elements) {
      this.metadata.clear();
      return addAllMetadata(elements);
    }

    /**
     * Adds elements to {@link Schema#getMetadata() metadata} list.
     * @param elements An iterable of metadata elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addAllMetadata(Iterable<? extends Entry> elements) {
      for (Entry element : elements) {
        this.metadata.add(Objects.requireNonNull(element, "metadata element"));
      }
      return this;
    }

    /**
     * Initializes the value for the {@link Schema#getAllEntries() allEntries} attribute.
     * @param allEntries The value for allEntries 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder allEntries(Stream<Entry> allEntries) {
      this.allEntries = Objects.requireNonNull(allEntries, "allEntries");
      initBits &= ~INIT_BIT_ALL_ENTRIES;
      return this;
    }

    /**
     * Put one entry to the {@link Schema#getProps() props} map.
     * @param key The key in the props map
     * @param value The associated value in the props map
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder putProps(String key, String value) {
      this.props.put(
          Objects.requireNonNull(key, "props key"),
          Objects.requireNonNull(value, value == null ? "props value for key: " + key : null));
      return this;
    }

    /**
     * Put one entry to the {@link Schema#getProps() props} map. Nulls are not permitted
     * @param entry The key and value entry
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder putProps(Map.Entry<String, ? extends String> entry) {
      String k = entry.getKey();
      String v = entry.getValue();
      this.props.put(
          Objects.requireNonNull(k, "props key"),
          Objects.requireNonNull(v, v == null ? "props value for key: " + k : null));
      return this;
    }

    /**
     * Sets or replaces all mappings from the specified map as entries for the {@link Schema#getProps() props} map. Nulls are not permitted
     * @param entries The entries that will be added to the props map
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder props(Map<String, ? extends String> entries) {
      this.props.clear();
      return putAllProps(entries);
    }

    /**
     * Put all mappings from the specified map as entries to {@link Schema#getProps() props} map. Nulls are not permitted
     * @param entries The entries that will be added to the props map
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder putAllProps(Map<String, ? extends String> entries) {
      for (Map.Entry<String, ? extends String> e : entries.entrySet()) {
        String k = e.getKey();
        String v = e.getValue();
        this.props.put(
            Objects.requireNonNull(k, "props key"),
            Objects.requireNonNull(v, v == null ? "props value for key: " + k : null));
      }
      return this;
    }

    /**
     * Put one entry to the {@link Schema#getProperties() properties} map.
     * @param key The key in the properties map
     * @param value The associated value in the properties map
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder putProperties(String key, String value) {
      this.properties.put(
          Objects.requireNonNull(key, "properties key"),
          Objects.requireNonNull(value, value == null ? "properties value for key: " + key : null));
      return this;
    }

    /**
     * Put one entry to the {@link Schema#getProperties() properties} map. Nulls are not permitted
     * @param entry The key and value entry
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder putProperties(Map.Entry<String, ? extends String> entry) {
      String k = entry.getKey();
      String v = entry.getValue();
      this.properties.put(
          Objects.requireNonNull(k, "properties key"),
          Objects.requireNonNull(v, v == null ? "properties value for key: " + k : null));
      return this;
    }

    /**
     * Sets or replaces all mappings from the specified map as entries for the {@link Schema#getProperties() properties} map. Nulls are not permitted
     * @param entries The entries that will be added to the properties map
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder properties(Map<String, ? extends String> entries) {
      this.properties.clear();
      return putAllProperties(entries);
    }

    /**
     * Put all mappings from the specified map as entries to {@link Schema#getProperties() properties} map. Nulls are not permitted
     * @param entries The entries that will be added to the properties map
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder putAllProperties(Map<String, ? extends String> entries) {
      for (Map.Entry<String, ? extends String> e : entries.entrySet()) {
        String k = e.getKey();
        String v = e.getValue();
        this.properties.put(
            Objects.requireNonNull(k, "properties key"),
            Objects.requireNonNull(v, v == null ? "properties value for key: " + k : null));
      }
      return this;
    }

    /**
     * Builds a new {@link Schema SchemaDetail}.
     * @return An immutable instance of Schema
     * @throws IllegalStateException if any required attributes are missing
     */
    public Schema build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new Schema(
          type,
          elementSchema,
          createUnmodifiableList(true, entries),
          createUnmodifiableList(true, metadata),
          allEntries,
          createUnmodifiableMap(false, false, props),
          createUnmodifiableMap(false, false, properties));
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = new ArrayList<>();
      if ((initBits & INIT_BIT_TYPE) != 0) attributes.add("type");
      if ((initBits & INIT_BIT_ELEMENT_SCHEMA) != 0) attributes.add("elementSchema");
      if ((initBits & INIT_BIT_ALL_ENTRIES) != 0) attributes.add("allEntries");
      return "Cannot build Schema, some of required attributes are not set " + attributes;
    }
  }

  private static <T> List<T> createSafeList(Iterable<? extends T> iterable, boolean checkNulls, boolean skipNulls) {
    ArrayList<T> list;
    if (iterable instanceof Collection<?>) {
      int size = ((Collection<?>) iterable).size();
      if (size == 0) return Collections.emptyList();
      list = new ArrayList<>(size);
    } else {
      list = new ArrayList<>();
    }
    for (T element : iterable) {
      if (skipNulls && element == null) continue;
      if (checkNulls) Objects.requireNonNull(element, "element");
      list.add(element);
    }
    return list;
  }

  private static <T> List<T> createUnmodifiableList(boolean clone, List<T> list) {
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

  private static <K, V> Map<K, V> createUnmodifiableMap(boolean checkNulls, boolean skipNulls, Map<? extends K, ? extends V> map) {
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
            if (k == null || v == null) continue;
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
}