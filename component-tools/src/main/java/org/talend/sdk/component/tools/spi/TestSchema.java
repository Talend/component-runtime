package org.talend.sdk.component.tools.spi;

import org.talend.sdk.component.api.record.Schema;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class TestSchema implements Schema {
    @Override
    public Type getType() {
        return null;
    }

    public Builder toBuilder(String prop) {
        return Schema.super.toBuilder();
    }

    @Override
    public Schema getElementSchema() {
        return null;
    }

    @Override
    public List<Entry> getEntries() {
        return null;
    }

    @Override
    public List<Entry> getMetadata() {
        return null;
    }

    @Override
    public Stream<Entry> getAllEntries() {
        return null;
    }

    @Override
    public Map<String, String> getProps() {
        return null;
    }

    @Override
    public String getProp(String property) {
        return null;
    }

    public String getProp(String property, String property2) {
        return null;
    }
}
