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
package org.talend.sdk.component.tcketl.domain.form;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Same results as the path resolver of the reference form renderer. */
class PathResolverTest {

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({ "dummy.foo.bar,sibling,dummy.foo.sibling", "dummy.foo.bar,../sibling,dummy.foo.sibling",
            "dummy.foo.bar,../sibling/child,dummy.foo.sibling.child", "dummy.foo[].bar,sibling,dummy.foo[].sibling",
            "dummy,.,dummy", "dummy.foo,..,dummy", "dummy.foo,../../foo/bar,foo.bar",
            "dummy.foo,./child,dummy.foo.child",
            "dummy.foo,./child/grandchild,dummy.foo.child.grandchild", "p[1].x,y,p[1].y",
            "a.b.c,other.path,other.path" })
    void resolvesAReferenceAgainstAProperty(final String property, final String reference, final String expected) {
        assertEquals(expected, PathResolver.resolve(property, reference));
    }
}
