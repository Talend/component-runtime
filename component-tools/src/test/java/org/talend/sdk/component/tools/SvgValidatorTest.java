/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools;

import static java.util.stream.Collectors.toList;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(PER_CLASS)
class SvgValidatorTest {

    @Test
    void validSvg() {
        doValidate("valid", 0, Boolean.TRUE);
    }

    @Test
    void validSvg40() {
        doValidate("valid40", 0, Boolean.FALSE);
    }

    @Test
    void invalidSvg() {
        final String error = doValidate("invalid", 1, Boolean.TRUE).iterator().next();
        assertTrue(error.startsWith("[invalid.svg] Invalid SVG: org.apache.batik.dom.util.SAXIOException: "), error);
    }

    @Test
    void viewboxSize() {
        final String error = doValidate("viewboxsize", 1, Boolean.TRUE).iterator().next();
        assertTrue(error.startsWith(
                "[viewboxsize.svg] viewBox must be '0 0 16 16' (family) or '0 0 40 40' (connector) found '0 0 16 17'"),
                error);
    }

    @Test
    void viewboxSize40() {
        final String error = doValidate("viewboxsize40", 1, Boolean.FALSE).iterator().next();
        assertTrue(error.startsWith(
                "[viewboxsize40.svg] viewBox must be '0 0 16 16' (family) or '0 0 40 40' (connector) found '0 0 16 17'"),
                error);
    }

    @Test
    void pathsAreClosed() {
        final String error = doValidate("closedpath", 1, Boolean.TRUE).iterator().next();
        assertTrue(error.startsWith("[closedpath.svg] All path must be closed so end with 'z', found value:"), error);
    }

    @Test
    void noEmbedStyle() {
        final String error = doValidate("embeddedstyle", 1, Boolean.TRUE).iterator().next();
        assertTrue(error.startsWith("[embeddedstyle.svg] Forbidden <style> in icon"), error);
    }

    @Test
    void noDisplayNone() {
        final String error = doValidate("displaynone", 1, Boolean.TRUE).iterator().next();
        assertTrue(error.startsWith("[displaynone.svg] 'display:none' is forbidden in SVG icons"), error);
    }

    private List<String> doValidate(final String name, final int count, final Boolean legacyMode) {
        final List<String> errors = new SvgValidator(legacyMode).validate(icon(name)).collect(toList());
        assertEquals(count, errors.size());
        return errors;
    }

    private Path icon(final String name) {
        return jarLocation(SvgValidatorTest.class).toPath().resolve("SvgValidatorTest").resolve(name + ".svg");
    }
}
