package org.talend.sdk.component.runtime.tdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.talend.sdk.component.api.record.Schema;

public class TdpSchema implements Schema {
    private Type type;
    private Map<String, Entry> entries = new HashMap<>();
    private Map<String, Entry> metadata = new HashMap<>();
    private Map<String, String> props = new HashMap<>();

    private TdpSchema() {
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Schema getElementSchema() {
        throw new UnsupportedOperationException("#getElementSchema is not supported");
    }

    @Override
    public List<Entry> getEntries() {
        return new ArrayList<>(entries.values());
    }

    @Override
    public List<Entry> getMetadata() {
        return new ArrayList<>(metadata.values());
    }

    @Override
    public Stream<Entry> getAllEntries() {
        return entries.values().stream();
    }

    @Override
    public Map<String, String> getProps() {
        return props;
    }

    @Override
    public String getProp(final String property) {
        return props.get(property);
    }

    boolean hasEntry(String entryName) {
        return entries.containsKey(entryName);
    }

    public TdpSchema setType(final Type type) {
        this.type = type;
        return this;
    }

    public TdpSchema setEntries(final List<Entry> entries) {
        this.entries = entries.stream()
                .collect(Collectors.toMap(
                        Entry::getName,
                        Function.identity()
                ));
        return this;
    }

    public TdpSchema setMetadata(final List<Entry> metadata) {
        this.metadata = metadata.stream()
                .collect(Collectors.toMap(
                        Entry::getName,
                        Function.identity()
                ));
        return this;
    }

    public TdpSchema setProps(final Map<String, String> props) {
        this.props = props;
        return this;
    }

    public TdpSchema setProp(String propKey, String propValue) {
        this.props.put(propKey, propValue);
        return this;
    }

    public TdpSchema addEntry(Entry entry) {
        this.entries.put(entry.getName(), entry);
        return this;
    }

    public TdpEntry createEntry(final String name, final Schema.Type type, final boolean nullable) {
        final TdpEntry tdpEntry = (TdpEntry) TdpEntry.builder()
                .withName(name)
                .withType(type)
                .withNullable(nullable)
                .build();

        addEntry(tdpEntry);

        return tdpEntry;
    }

    public TdpSchema putMetadata(Entry entry) {
        this.metadata.put(entry.getName(), entry);
        return this;
    }

    /**
     * Warning, will mutate existing instance
     */
    @Override
    public Schema.Builder toBuilder() {
        return new TdpSchema.Builder(this);
    }

    public static TdpSchema.Builder builder() {
        return new TdpSchema.Builder();
    }

    public static TdpSchema.Builder builder(Schema schema) {
        return new TdpSchema.Builder(schema);
    }

    static TdpSchema fromExistingSchema(Schema schema) {
        final TdpSchema tdpSchema = new TdpSchema();
        tdpSchema.setEntries(schema.getEntries());
        tdpSchema.setMetadata(schema.getMetadata());
        tdpSchema.setProps(schema.getProps());
        tdpSchema.setType(schema.getType());
        return tdpSchema;
    }

    /**
     * An awful builder which can either build a fresh {@link org.talend.sdk.component.api.record.Schema.Entry}
     * instance, or mutate an existing one.
     */
    public static class Builder implements Schema.Builder {
        private final TdpSchema tdpSchema;

        private Builder(final TdpSchema tdpSchema) {
            this.tdpSchema = tdpSchema;
        }

        private Builder(Schema schema) {
            this.tdpSchema = new TdpSchema();
            tdpSchema.setEntries(schema.getEntries());
            tdpSchema.setMetadata(schema.getMetadata());
            tdpSchema.setProps(schema.getProps());
            tdpSchema.setType(schema.getType());
        }

        private Builder() {
            this.tdpSchema = new TdpSchema();
        }

        @Override
        public Schema.Builder withType(final Type type) {
            tdpSchema.setType(type);
            return this;
        }

        @Override
        public Schema.Builder withEntry(final Entry entry) {
            tdpSchema.addEntry(entry);
            return this;
        }

        @Override
        public Schema.Builder withElementSchema(final Schema schema) {
            throw new UnsupportedOperationException("#withElementSchema is not supported");
        }

        @Override
        public Schema.Builder withProps(final Map<String, String> props) {
            tdpSchema.setProps(props);
            return this;
        }

        @Override
        public Schema.Builder withProp(final String key, final String value) {
            tdpSchema.setProp(key, value);
            return this;
        }

        @Override
        public Schema build() {
            return tdpSchema;
        }
    }

}
