package org.talend.sdk.component.api.processor;

import java.util.Iterator;

public interface MultiOutputIterator<T> {
    /**
     * <b>Split mode</b>: sets a single lazy iterator whose elements are tagged with
     * the target output connection name via {@link TaggedOutput}.
     * The runtime reads one record at a time and routes it to the matching connection.
     *
     * @param iterator the tagged iterator routing records to their named outputs
     */
    void setIterator(Iterator<TaggedOutput<T>> iterator);
}
