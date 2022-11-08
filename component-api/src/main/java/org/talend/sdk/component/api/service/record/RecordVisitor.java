/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.service.record;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;

/**
 * Visitor enabling to browse a record. All methods are adapters - implementing a no-op by default.
 * 
 * @param <T> the returned type by the visitor if it owns a state.
 */
public interface RecordVisitor<T> extends Supplier<T>, BinaryOperator<T> {

    /**
     * This is called to get the value extracted from this visitor.
     * It is also an exit callback for a record instance.
     *
     * @return the outcome value of this visitor.
     */
    @Override
    default T get() {
        return null;
    }

    /**
     * Enables to combine two visitors returned value ({@link RecordVisitor#get()}).
     *
     * @param t1 previous value, can be null.
     * @param t2 current value
     * @return the merged value of t1 and t2. By default it returns t1.
     */
    @Override
    default T apply(final T t1, final T t2) {
        return t1;
    }

    default void onInt(final Schema.Entry entry, final OptionalInt optionalInt) {
        // no-op
    }

    default void onLong(final Schema.Entry entry, final OptionalLong optionalLong) {
        // no-op
    }

    default void onFloat(final Schema.Entry entry, final OptionalDouble optionalFloat) {
        // no-op
    }

    default void onDouble(final Schema.Entry entry, final OptionalDouble optionalDouble) {
        // no-op
    }

    default void onBoolean(final Schema.Entry entry, final Optional<Boolean> optionalBoolean) {
        // no-op
    }

    default void onString(final Schema.Entry entry, final Optional<String> string) {
        // no-op
    }

    default void onDatetime(final Schema.Entry entry, final Optional<ZonedDateTime> dateTime) {
        // no-op
    }

    default void onBytes(final Schema.Entry entry, final Optional<byte[]> bytes) {
        // no-op
    }

    default RecordVisitor<T> onRecord(final Schema.Entry entry, final Optional<Record> record) {
        return this;
    }

    default void onIntArray(final Schema.Entry entry, final Optional<Collection<Integer>> array) {
        // no-op
    }

    default void onLongArray(final Schema.Entry entry, final Optional<Collection<Long>> array) {
        // no-op
    }

    default void onFloatArray(final Schema.Entry entry, final Optional<Collection<Float>> array) {
        // no-op
    }

    default void onDoubleArray(final Schema.Entry entry, final Optional<Collection<Double>> array) {
        // no-op
    }

    default void onBooleanArray(final Schema.Entry entry, final Optional<Collection<Boolean>> array) {
        // no-op
    }

    default void onStringArray(final Schema.Entry entry, final Optional<Collection<String>> array) {
        // no-op
    }

    default void onDatetimeArray(final Schema.Entry entry, final Optional<Collection<ZonedDateTime>> array) {
        // no-op
    }

    default void onBytesArray(final Schema.Entry entry, final Optional<Collection<byte[]>> array) {
        // no-op
    }

    default RecordVisitor<T> onRecordArray(final Schema.Entry entry, final Optional<Collection<Record>> array) {
        return this;
    }
}
