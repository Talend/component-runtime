/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class RecordAsserts implements Function<Iterable<Map<String, List<Serializable>>>, Void>, Serializable {

    private final Map<String, SerializableConsumer<List<? extends Serializable>>> validators = new HashMap<>();

    public <R extends Serializable> RecordAsserts withAsserts(final String outputName,
            final SerializableConsumer<List<R>> validator) {
        validators.put(outputName, SerializableConsumer.class.cast(validator));
        return this;
    }

    @Override
    public Void apply(final Iterable<Map<String, List<Serializable>>> input) {
        final Map<String, List<Serializable>> outputs = StreamSupport
                .stream(input.spliterator(), false)
                .flatMap(m -> m.entrySet().stream())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (u1, u2) -> Stream
                                .of(u1, u2)
                                .filter(Objects::nonNull)
                                .flatMap(Collection::stream)
                                .collect(toList())));

        // if we want to validate some outputs which are not here it means the
        // validation fails
        // note: if we don't validate an output which is here it can means we don't care
        // for current test so ignore the opposite
        // validation
        final Collection<String> missing = new HashSet<>(validators.keySet());
        missing.removeAll(outputs.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing outputs: " + missing);
        }

        validators.forEach((k, v) -> v.accept(outputs.get(k)));

        return null;
    }

    public interface SerializableConsumer<A> extends Consumer<A>, Serializable {
    }

}
