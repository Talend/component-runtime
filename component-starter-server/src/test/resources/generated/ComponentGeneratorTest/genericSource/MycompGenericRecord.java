package com.foo.source;

import static java.util.Collections.emptySet;

import org.talend.sdk.component.api.processor.data.ObjectMap;
// this is the pojo which will be used to represent your data
public class MycompGenericRecord implements ObjectMap {
    @Override
    public Object get(final String location) {
        return null;
    }

    @Override
    public ObjectMap getMap(final String location) {
        return null;
    }

    @Override
    public Collection<ObjectMap> getCollection(final String location) {
        return null;
    }

    @Override
    public synchronized Set<String> keys() {
        return emptySet();
    }
    
}