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
package org.talend.sdk.component.tcketl.app.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.beans.property.ReadOnlyObjectProperty;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.app.testing.TestAsync;

class LazyResourcesTest {

    private final TestAsync async = new TestAsync();

    private final List<String> loads = new ArrayList<>();

    private final LazyResources<String, String> resources =
            new LazyResources<>(async.runner(), "placeholder", key -> {
                loads.add(key);
                if ("broken".equals(key)) {
                    throw new IOException("cannot read " + key);
                }
                return "missing".equals(key) ? Optional.empty() : Optional.of(key.toUpperCase(Locale.ROOT));
            });

    @Test
    void answersWithThePlaceholderUntilTheResourceIsLoaded() {
        final ReadOnlyObjectProperty<String> resource = resources.get("a");
        assertEquals("placeholder", resource.get());

        async.drain();
        assertEquals("A", resource.get());
    }

    @Test
    void loadsEachKeyOnce() {
        final ReadOnlyObjectProperty<String> first = resources.get("a");
        async.drain();
        assertSame(first, resources.get("a"));
        async.drain();
        assertEquals(List.of("a"), loads);
    }

    @Test
    void keepsThePlaceholderOfAResourceThatIsMissingOrBrokenAndDoesNotAskAgain() {
        final ReadOnlyObjectProperty<String> missing = resources.get("missing");
        final ReadOnlyObjectProperty<String> broken = resources.get("broken");
        async.drain();
        resources.get("missing");
        resources.get("broken");
        async.drain();

        assertEquals("placeholder", missing.get());
        assertEquals("placeholder", broken.get());
        assertEquals(List.of("missing", "broken"), loads);
    }

    @Test
    void clearAbandonsTheLoadsInFlightAndForgetsWhatWasLoaded() {
        final ReadOnlyObjectProperty<String> abandoned = resources.get("a");
        resources.clear();
        async.drain();
        assertEquals("placeholder", abandoned.get());
        assertEquals(List.of(), loads);

        final ReadOnlyObjectProperty<String> again = resources.get("a");
        async.drain();
        assertNotSame(abandoned, again);
        assertEquals("A", again.get());
    }
}
