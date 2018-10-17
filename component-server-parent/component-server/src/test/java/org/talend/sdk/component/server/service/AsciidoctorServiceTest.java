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
        assertEquals("<h1>Test</h1>\n" + "<div class=\"paragraph\">\n" + "<p>Some Text</p>\n" + "</div>\n"
                + "<table class=\"tableblock frame-all grid-all stretch\">\n" + "<colgroup>\n"
                + "<col style=\"width: 50%;\">\n" + "<col style=\"width: 50%;\">\n" + "</colgroup>\n" + "<tbody>\n"
                + "<tr>\n" + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table</p></td>\n" + "</tr>\n"
                + "</tbody>\n" + "</table>", adoc.toHtml("= Test\n\nSome Text\n\n|===\n|Test|Table\n|===\n\n"));
        assertEquals("<h1>Test</h1>\n" + "<div class=\"sect1\">\n" + "<h2 id=\"_comp\">Comp</h2>\n"
                + "<div class=\"sectionbody\">\n" + "<div class=\"paragraph\">\n" + "<p>Test</p>\n" + "</div>\n"
                + "</div>\n" + "</div>", adoc.toHtml("= Test\n\n== Comp\n\nTest\n\n"));
        assertEquals("<table class=\"tableblock frame-all grid-all stretch\">\n" + "<colgroup>\n"
                + "<col style=\"width: 50%;\">\n" + "<col style=\"width: 50%;\">\n" + "</colgroup>\n" + "<tbody>\n"
                + "<tr>\n" + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table</p></td>\n" + "</tr>\n"
                + "<tr>\n" + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test 2</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table 2</p></td>\n"
                + "</tr>\n" + "<tr>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test 3</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table 3</p></td>\n"
                + "</tr>\n" + "</tbody>\n" + "</table>",
                adoc.toHtml("|===\n|Test|Table\n|Test 2|Table 2\n\n|Test 3|Table 3\n|===\n\n"));
        assertEquals("<table class=\"tableblock frame-all grid-all stretch\">\n" + "<colgroup>\n"
                + "<col style=\"width: 50%;\">\n" + "<col style=\"width: 50%;\">\n" + "</colgroup>\n" + "<tbody>\n"
                + "<tr>\n" + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table</p></td>\n" + "</tr>\n"
                + "<tr>\n" + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test 2</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table 2</p></td>\n"
                + "</tr>\n" + "<tr>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Test 3</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">Table <code>and</code> code</p></td>\n"
                + "</tr>\n" + "</tbody>\n" + "</table>",
                adoc.toHtml("|===\n|Test|Table\n|Test 2|Table 2\n|Test 3|Table `and` code\n|===\n\n"));
    }

    @Test
    void renderConfiguration() {
        assertEquals("<div class=\"sect1\">\n" + "<h2 id=\"_activeif\">activeif</h2>\n"
                + "<div class=\"sectionbody\">\n" + "<div class=\"sect2\">\n"
                + "<h3 id=\"_configuration\">Configuration</h3>\n"
                + "<table class=\"tableblock frame-all grid-all stretch\">\n" + "<colgroup>\n"
                + "<col style=\"width: 25%;\">\n" + "<col style=\"width: 25%;\">\n" + "<col style=\"width: 25%;\">\n"
                + "<col style=\"width: 25%;\">\n" + "</colgroup>\n" + "<thead>\n" + "<tr>\n"
                + "<th class=\"tableblock halign-left valign-top\">Path</th>\n"
                + "<th class=\"tableblock halign-left valign-top\">Description</th>\n"
                + "<th class=\"tableblock halign-left valign-top\">Default Value</th>\n"
                + "<th class=\"tableblock halign-left valign-top\">Enabled If</th>\n" + "</tr>\n" + "</thead>\n"
                + "<tbody>\n" + "<tr>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><em>configuration</em></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">configuration configuration</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><code>-</code></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><div class=\"content\"><div class=\"paragraph\">\n"
                + "<p>Always enabled</p>\n" + "</div></div></td>\n" + "</tr>\n" + "<tr>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><em>configuration.advanced</em></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">advanced configuration</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><code>false</code></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><div class=\"content\"><div class=\"paragraph\">\n"
                + "<p>Always enabled</p>\n" + "</div></div></td>\n" + "</tr>\n" + "<tr>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><em>configuration.advancedOption</em></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">advancedOption configuration</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><code>-</code></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><div class=\"content\"><div class=\"paragraph\">\n"
                + "<p>All these conditions are meet:</p>\n" + "</div>\n" + "<div class=\"ulist\">\n" + "<ul>\n"
                + "<li>\n" + "<p><code>advanced</code> is equals to <code>false</code></p>\n" + "</li>\n" + "<li>\n"
                + "<p>the length of <code>query</code> is <code>0</code></p>\n" + "</li>\n" + "</ul>\n"
                + "</div></div></td>\n" + "</tr>\n" + "<tr>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><em>configuration.query</em></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">query configuration</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><code>-</code></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><div class=\"content\"><div class=\"paragraph\">\n"
                + "<p>All these conditions are meet:</p>\n" + "</div>\n" + "<div class=\"ulist\">\n" + "<ul>\n"
                + "<li>\n" + "<p><code>toggle</code> is equals to <code>true</code></p>\n" + "</li>\n" + "<li>\n"
                + "<p><code>type</code> is equals to <code>mysql</code> or <code>oracle</code></p>\n" + "</li>\n"
                + "</ul>\n" + "</div></div></td>\n" + "</tr>\n" + "<tr>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><em>configuration.toggle</em></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">toggle configuration</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><code>false</code></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><div class=\"content\"><div class=\"paragraph\">\n"
                + "<p>Always enabled</p>\n" + "</div></div></td>\n" + "</tr>\n" + "<tr>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><em>configuration.token</em></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">token configuration</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><code>-</code></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><div class=\"content\"><div class=\"paragraph\">\n"
                + "<p><code>toggle</code> is equals to <code>true</code></p>\n" + "</div></div></td>\n" + "</tr>\n"
                + "<tr>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><em>configuration.type</em></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\">type configuration</p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><p class=\"tableblock\"><code>-</code></p></td>\n"
                + "<td class=\"tableblock halign-left valign-top\"><div class=\"content\"><div class=\"paragraph\">\n"
                + "<p>Always enabled</p>\n" + "</div></div></td>\n" + "</tr>\n" + "</tbody>\n" + "</table>\n"
                + "</div>\n" + "</div>\n" + "</div>",
                adoc
                        .toHtml("== activeif\n" + "\n" + "=== Configuration\n" + "\n"
                                + "[cols=\"e,d,m,a\",options=\"header\"]\n" + "|===\n"
                                + "|Path|Description|Default Value|Enabled If\n"
                                + "|configuration|configuration configuration|-|Always enabled\n"
                                + "|configuration.advanced|advanced configuration|false|Always enabled\n"
                                + "|configuration.advancedOption|advancedOption configuration|-|All these conditions are meet:\n"
                                + "\n" + "- `advanced` is equals to `false`\n" + "- the length of `query` is `0`\n"
                                + "\n" + "|configuration.query|query configuration|-|All these conditions are meet:\n"
                                + "\n" + "- `toggle` is equals to `true`\n"
                                + "- `type` is equals to `mysql` or `oracle`\n" + "\n"
                                + "|configuration.toggle|toggle configuration|false|Always enabled\n"
                                + "|configuration.token|token configuration|-|`toggle` is equals to `true`\n"
                                + "|configuration.type|type configuration|-|Always enabled\n" + "|===\n"));
    }
}
