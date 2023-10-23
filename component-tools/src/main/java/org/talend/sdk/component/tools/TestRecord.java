package org.talend.sdk.component.tools;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;

public class TestRecord implements Record {
    @Override
    public Schema getSchema() {
        return null;
    }

    @Override
    public <T> T get(Class<T> expectedType, String name) {
        return null;
    }

    @Override
    public Builder withNewSchema(Schema schema) {
        return null;
    }

    public Builder withNewSchema(Schema schema, Record record) {
        return null;
    }

    public Builder withTestSchema(Schema schema) {
        return null;
    }
}
