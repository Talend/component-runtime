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

import org.apache.meecrowave.junit5.MeecrowaveConfig;
import org.junit.jupiter.api.Test;

@MeecrowaveConfig
class AsciidoctorServiceTest {

    @Inject
    private AsciidoctorService adoc;

    @Test
    void renderHtml() {
        assertEquals("<div class=\"paragraph\">\n" + "<p>Some Text</p>\n" + "</div>\n"
                + "<table class=\"tableblock frame-all grid-all spread\">\n" + "<colgroup>\n"
                + "<col style=\"width: 50%;\">\n" + "<col style=\"width: 50%;\">\n" + "</colgroup>\n" + "<tbody>\n"
                + "<tr>\n" + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table</p></td>\n" + "</tr>\n"
                + "</tbody>\n" + "</table>", adoc.toHtml("= Test\n\nSome Text\n\n|===\n|Test|Table\n|===\n\n"));
        assertEquals(
                "<div class=\"sect1\">\n" + "<h2 id=\"_comp\">Comp</h2>\n" + "<div class=\"sectionbody\">\n"
                        + "<div class=\"paragraph\">\n" + "<p>Test</p>\n" + "</div>\n" + "</div>\n" + "</div>",
                adoc.toHtml("= Test\n\n== Comp\n\nTest\n\n"));
    }
}
