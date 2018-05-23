/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;

@MonoMeecrowaveConfig
class AsciidoctorServiceTest {

    @Inject
    private AsciidoctorService adoc;

    @Test
    void renderHtml() {
        assertEquals("<h1 id=\"_test\">Test</h1>\n" + "<div class=\"paragraph\">\n" + "<p>\n" + "Some Text\n" + "</p>\n"
                + "</div>\n" + "<table class=\"tableblock frame-all grid-all spread\">\n" + "<thead>\n" + "<tr>\n"
                + "<th class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test</p></th>\n"
                + "<th class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table</p></th>\n" + "</tr>\n"
                + "</thead>\n" + "<tbody>\n" + "</tbody>\n" + "</table>\n",
                adoc.toHtml("= Test\n\nSome Text\n\n|===\n|Test|Table\n|===\n\n"));
        assertEquals("<h1 id=\"_test\">Test</h1>\n" + "<h2 id=\"_comp\">Comp</h2>\n" + "<div class=\"paragraph\">\n"
                + "<p>\n" + "Test\n" + "</p>\n" + "</div>\n", adoc.toHtml("= Test\n\n== Comp\n\nTest\n\n"));
        assertEquals(
                "<table class=\"tableblock frame-all grid-all spread\">\n" + "<thead>\n" + "<tr>\n"
                        + "<th class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test</p></th>\n"
                        + "<th class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table</p></th>\n"
                        + "</tr>\n" + "</thead>\n" + "<tbody>\n" + "<tr>\n"
                        + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test 2</p></td>\n"
                        + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table 2</p></td>\n"
                        + "</tr>\n" + "<tr>\n"
                        + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test 3</p></td>\n"
                        + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table 3</p></td>\n"
                        + "</tr>\n" + "</tbody>\n" + "</table>\n",
                adoc.toHtml("|===\n|Test|Table\n|Test 2|Table 2\n\n|Test 3|Table 3\n|===\n\n"));
        assertEquals("<table class=\"tableblock frame-all grid-all spread\">\n" + "<thead>\n" + "<tr>\n"
                + "<th class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test</p></th>\n"
                + "<th class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table</p></th>\n" + "</tr>\n"
                + "</thead>\n" + "<tbody>\n" + "<tr>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test 2</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table 2</p></td>\n"
                + "</tr>\n" + "<tr>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test 3</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table <pre>and</pre> code</p></td>\n"
                + "</tr>\n" + "</tbody>\n" + "</table>\n",
                adoc.toHtml("|===\n|Test|Table\n|Test 2|Table 2\n|Test 3|Table `and` code\n|===\n\n"));
    }
}
