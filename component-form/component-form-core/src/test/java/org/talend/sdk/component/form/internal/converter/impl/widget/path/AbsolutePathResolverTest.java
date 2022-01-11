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
package org.talend.sdk.component.form.internal.converter.impl.widget.path;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AbsolutePathResolverTest {

    private final AbsolutePathResolver resolver = new AbsolutePathResolver();

    @Test
    void resolveSibling() {
        assertEquals("dummy.foo.sibling", resolver.resolveProperty("dummy.foo.bar", "sibling"));
    }

    @Test
    void resolveSiblingFromParent() {
        assertEquals("dummy.foo.sibling", resolver.resolveProperty("dummy.foo.bar", "../sibling"));
    }

    @Test
    void resolveSiblingChild() {
        assertEquals("dummy.foo.sibling.child", resolver.resolveProperty("dummy.foo.bar", "../sibling/child"));
    }

    @Test
    void resolveSiblingArray() {
        assertEquals("dummy.foo[].sibling", resolver.resolveProperty("dummy.foo[].bar", "sibling"));
    }

    @Test
    void resolveMe() {
        assertEquals("dummy", resolver.resolveProperty("dummy", "."));
    }

    @Test
    void resolveParent() {
        assertEquals("dummy", resolver.resolveProperty("dummy.foo", ".."));
    }

    @Test
    void resolveGrandParent() {
        assertEquals("foo.bar", resolver.resolveProperty("dummy.foo", "../../foo/bar"));
    }

    @Test
    void resolveChild() {
        assertEquals("dummy.foo.child", resolver.resolveProperty("dummy.foo", "./child"));
    }

    @Test
    void resolveGrandChild() {
        assertEquals("dummy.foo.child.grandchild", resolver.resolveProperty("dummy.foo", "./child/grandchild"));
    }
}
