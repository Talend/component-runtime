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

import static java.util.Locale.ROOT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import lombok.extern.slf4j.Slf4j;

// avoids to embed jRuby (asciidoctorj) and Nashorn (asciidoctor.js) just doesn't work reliably - requires -noverify
//
// supported syntax:
// - titles (=* title)
// - paragraph
// - simple tables
//
@Slf4j
@ApplicationScoped
public class AsciidoctorService {

    private static final String[][] HTML_REPLACEMENTS = new String[][] { { "\"", "&quot;" }, { "&", "&amp;" },
            { "<", "&lt;" }, { ">", "&gt;" }, { "\u00A0", "&nbsp;" }, { "\u00A1", "&iexcl;" }, { "\u00A2", "&cent;" },
            { "\u00A3", "&pound;" }, { "\u00A4", "&curren;" }, { "\u00A5", "&yen;" }, { "\u00A6", "&brvbar;" },
            { "\u00A7", "&sect;" }, { "\u00A8", "&uml;" }, { "\u00A9", "&copy;" }, { "\u00AA", "&ordf;" },
            { "\u00AB", "&laquo;" }, { "\u00AC", "&not;" }, { "\u00AD", "&shy;" }, { "\u00AE", "&reg;" },
            { "\u00AF", "&macr;" }, { "\u00B0", "&deg;" }, { "\u00B1", "&plusmn;" }, { "\u00B2", "&sup2;" },
            { "\u00B3", "&sup3;" }, { "\u00B4", "&acute;" }, { "\u00B5", "&micro;" }, { "\u00B6", "&para;" },
            { "\u00B7", "&middot;" }, { "\u00B8", "&cedil;" }, { "\u00B9", "&sup1;" }, { "\u00BA", "&ordm;" },
            { "\u00BB", "&raquo;" }, { "\u00BC", "&frac14;" }, { "\u00BD", "&frac12;" }, { "\u00BE", "&frac34;" },
            { "\u00BF", "&iquest;" }, { "\u00C0", "&Agrave;" }, { "\u00C1", "&Aacute;" }, { "\u00C2", "&Acirc;" },
            { "\u00C3", "&Atilde;" }, { "\u00C4", "&Auml;" }, { "\u00C5", "&Aring;" }, { "\u00C6", "&AElig;" },
            { "\u00C7", "&Ccedil;" }, { "\u00C8", "&Egrave;" }, { "\u00C9", "&Eacute;" }, { "\u00CA", "&Ecirc;" },
            { "\u00CB", "&Euml;" }, { "\u00CC", "&Igrave;" }, { "\u00CD", "&Iacute;" }, { "\u00CE", "&Icirc;" },
            { "\u00CF", "&Iuml;" }, { "\u00D0", "&ETH;" }, { "\u00D1", "&Ntilde;" }, { "\u00D2", "&Ograve;" },
            { "\u00D3", "&Oacute;" }, { "\u00D4", "&Ocirc;" }, { "\u00D5", "&Otilde;" }, { "\u00D6", "&Ouml;" },
            { "\u00D7", "&times;" }, { "\u00D8", "&Oslash;" }, { "\u00D9", "&Ugrave;" }, { "\u00DA", "&Uacute;" },
            { "\u00DB", "&Ucirc;" }, { "\u00DC", "&Uuml;" }, { "\u00DD", "&Yacute;" }, { "\u00DE", "&THORN;" },
            { "\u00DF", "&szlig;" }, { "\u00E0", "&agrave;" }, { "\u00E1", "&aacute;" }, { "\u00E2", "&acirc;" },
            { "\u00E3", "&atilde;" }, { "\u00E4", "&auml;" }, { "\u00E5", "&aring;" }, { "\u00E6", "&aelig;" },
            { "\u00E7", "&ccedil;" }, { "\u00E8", "&egrave;" }, { "\u00E9", "&eacute;" }, { "\u00EA", "&ecirc;" },
            { "\u00EB", "&euml;" }, { "\u00EC", "&igrave;" }, { "\u00ED", "&iacute;" }, { "\u00EE", "&icirc;" },
            { "\u00EF", "&iuml;" }, { "\u00F0", "&eth;" }, { "\u00F1", "&ntilde;" }, { "\u00F2", "&ograve;" },
            { "\u00F3", "&oacute;" }, { "\u00F4", "&ocirc;" }, { "\u00F5", "&otilde;" }, { "\u00F6", "&ouml;" },
            { "\u00F7", "&divide;" }, { "\u00F8", "&oslash;" }, { "\u00F9", "&ugrave;" }, { "\u00FA", "&uacute;" },
            { "\u00FB", "&ucirc;" }, { "\u00FC", "&uuml;" }, { "\u00FD", "&yacute;" }, { "\u00FE", "&thorn;" },
            { "\u00FF", "&yuml;" }, { "\u0192", "&fnof;" }, { "\u0391", "&Alpha;" }, { "\u0392", "&Beta;" },
            { "\u0393", "&Gamma;" }, { "\u0394", "&Delta;" }, { "\u0395", "&Epsilon;" }, { "\u0396", "&Zeta;" },
            { "\u0397", "&Eta;" }, { "\u0398", "&Theta;" }, { "\u0399", "&Iota;" }, { "\u039A", "&Kappa;" },
            { "\u039B", "&Lambda;" }, { "\u039C", "&Mu;" }, { "\u039D", "&Nu;" }, { "\u039E", "&Xi;" },
            { "\u039F", "&Omicron;" }, { "\u03A0", "&Pi;" }, { "\u03A1", "&Rho;" }, { "\u03A3", "&Sigma;" },
            { "\u03A4", "&Tau;" }, { "\u03A5", "&Upsilon;" }, { "\u03A6", "&Phi;" }, { "\u03A7", "&Chi;" },
            { "\u03A8", "&Psi;" }, { "\u03A9", "&Omega;" }, { "\u03B1", "&alpha;" }, { "\u03B2", "&beta;" },
            { "\u03B3", "&gamma;" }, { "\u03B4", "&delta;" }, { "\u03B5", "&epsilon;" }, { "\u03B6", "&zeta;" },
            { "\u03B7", "&eta;" }, { "\u03B8", "&theta;" }, { "\u03B9", "&iota;" }, { "\u03BA", "&kappa;" },
            { "\u03BB", "&lambda;" }, { "\u03BC", "&mu;" }, { "\u03BD", "&nu;" }, { "\u03BE", "&xi;" },
            { "\u03BF", "&omicron;" }, { "\u03C0", "&pi;" }, { "\u03C1", "&rho;" }, { "\u03C2", "&sigmaf;" },
            { "\u03C3", "&sigma;" }, { "\u03C4", "&tau;" }, { "\u03C5", "&upsilon;" }, { "\u03C6", "&phi;" },
            { "\u03C7", "&chi;" }, { "\u03C8", "&psi;" }, { "\u03C9", "&omega;" }, { "\u03D1", "&thetasym;" },
            { "\u03D2", "&upsih;" }, { "\u03D6", "&piv;" }, { "\u2022", "&bull;" }, { "\u2026", "&hellip;" },
            { "\u2032", "&prime;" }, { "\u2033", "&Prime;" }, { "\u203E", "&oline;" }, { "\u2044", "&frasl;" },
            { "\u2118", "&weierp;" }, { "\u2111", "&image;" }, { "\u211C", "&real;" }, { "\u2122", "&trade;" },
            { "\u2135", "&alefsym;" }, { "\u2190", "&larr;" }, { "\u2191", "&uarr;" }, { "\u2192", "&rarr;" },
            { "\u2193", "&darr;" }, { "\u2194", "&harr;" }, { "\u21B5", "&crarr;" }, { "\u21D0", "&lArr;" },
            { "\u21D1", "&uArr;" }, { "\u21D2", "&rArr;" }, { "\u21D3", "&dArr;" }, { "\u21D4", "&hArr;" },
            { "\u2200", "&forall;" }, { "\u2202", "&part;" }, { "\u2203", "&exist;" }, { "\u2205", "&empty;" },
            { "\u2207", "&nabla;" }, { "\u2208", "&isin;" }, { "\u2209", "&notin;" }, { "\u220B", "&ni;" },
            { "\u220F", "&prod;" }, { "\u2211", "&sum;" }, { "\u2212", "&minus;" }, { "\u2217", "&lowast;" },
            { "\u221A", "&radic;" }, { "\u221D", "&prop;" }, { "\u221E", "&infin;" }, { "\u2220", "&ang;" },
            { "\u2227", "&and;" }, { "\u2228", "&or;" }, { "\u2229", "&cap;" }, { "\u222A", "&cup;" },
            { "\u222B", "&int;" }, { "\u2234", "&there4;" }, { "\u223C", "&sim;" }, { "\u2245", "&cong;" },
            { "\u2248", "&asymp;" }, { "\u2260", "&ne;" }, { "\u2261", "&equiv;" }, { "\u2264", "&le;" },
            { "\u2265", "&ge;" }, { "\u2282", "&sub;" }, { "\u2283", "&sup;" }, { "\u2284", "&nsub;" },
            { "\u2286", "&sube;" }, { "\u2287", "&supe;" }, { "\u2295", "&oplus;" }, { "\u2297", "&otimes;" },
            { "\u22A5", "&perp;" }, { "\u22C5", "&sdot;" }, { "\u2308", "&lceil;" }, { "\u2309", "&rceil;" },
            { "\u230A", "&lfloor;" }, { "\u230B", "&rfloor;" }, { "\u2329", "&lang;" }, { "\u232A", "&rang;" },
            { "\u25CA", "&loz;" }, { "\u2660", "&spades;" }, { "\u2663", "&clubs;" }, { "\u2665", "&hearts;" },
            { "\u2666", "&diams;" }, { "\u0152", "&OElig;" }, { "\u0153", "&oelig;" }, { "\u0160", "&Scaron;" },
            { "\u0161", "&scaron;" }, { "\u0178", "&Yuml;" }, { "\u02C6", "&circ;" }, { "\u02DC", "&tilde;" },
            { "\u2002", "&ensp;" }, { "\u2003", "&emsp;" }, { "\u2009", "&thinsp;" }, { "\u200C", "&zwnj;" },
            { "\u200D", "&zwj;" }, { "\u200E", "&lrm;" }, { "\u200F", "&rlm;" }, { "\u2013", "&ndash;" },
            { "\u2014", "&mdash;" }, { "\u2018", "&lsquo;" }, { "\u2019", "&rsquo;" }, { "\u201A", "&sbquo;" },
            { "\u201C", "&ldquo;" }, { "\u201D", "&rdquo;" }, { "\u201E", "&bdquo;" }, { "\u2020", "&dagger;" },
            { "\u2021", "&Dagger;" }, { "\u2030", "&permil;" }, { "\u2039", "&lsaquo;" }, { "\u203A", "&rsaquo;" },
            { "\u20AC", "&euro;" } };

    public String toHtml(final String input) {
        final StringBuilder builder = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new StringReader(input))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.startsWith("////")) {
                    do {
                        line = reader.readLine();
                        lineNumber++;
                    } while (!line.startsWith("////"));
                }
                if (line.startsWith("//")) {
                    continue;
                }
                if (line.startsWith(":")) { // ignore attributes
                    continue;
                }
                if (line.startsWith("[")) {
                    if (line.startsWith("[source")) {
                        line = reader.readLine();
                        lineNumber++;
                        if (line != null && line.startsWith("----")) {
                            builder.append("<pre><code>\n");
                            do {
                                line = reader.readLine();
                                if (line == null) {
                                    break;
                                }
                                lineNumber++;
                                if (line.startsWith("----")) {
                                    break;
                                }
                                builder.append(escapeHtml4(line, false)).append("\n");
                            } while (true);
                            builder.append("</pre></code>\n");
                        }
                    } // else ignore customizations for now
                    continue;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }

                if (line.startsWith("=")) {
                    final int space = line.indexOf(' ');
                    if (space < 0) {
                        throw new IllegalArgumentException("line " + lineNumber + " missing space after equal(s)");
                    }
                    final String title = line.substring(space).trim();
                    builder
                            .append("<h")
                            .append(space)
                            .append(" id=\"")
                            .append("_")
                            .append(title.replace(" ", "_").toLowerCase(ROOT))
                            .append("\">")
                            .append(escapeHtml4(title, true))
                            .append("</h")
                            .append(space)
                            .append(">\n");
                } else if (line.startsWith("|===")) {
                    builder.append("<table class=\"tableblock frame-all grid-all spread\">\n");
                    boolean headersDone = false;
                    do {
                        line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        lineNumber++;
                        if (line.startsWith("|===")) {
                            break;
                        }
                        if (line.trim().isEmpty()) {
                            continue;
                        }

                        if (!headersDone) {
                            builder.append("<thead>\n");
                        }

                        final String[] columns = (line.startsWith("|") ? line.substring(1) : line).split("\\|");
                        builder.append("<tr>\n");
                        if (columns.length > 1) {
                            final boolean data = headersDone;
                            Stream.of(columns).forEach(c -> td(data, builder, c));
                        } else {
                            td(headersDone, builder, line);
                            do {
                                line = reader.readLine();
                                if (line == null) {
                                    break;
                                }
                                lineNumber++;
                                if (line.isEmpty()) {
                                    break;
                                }
                                line = line.startsWith("|") ? line.substring(1) : line;
                                td(headersDone, builder, line);
                            } while (true);
                        }
                        builder.append("</tr>\n");
                        if (!headersDone) {
                            builder.append("</thead>\n<tbody>\n");
                            headersDone = true;
                        }
                    } while (true);
                    builder.append("</tbody>\n</table>\n");
                } else { // paragraph
                    builder.append("<div class=\"paragraph\">\n<p>\n").append(escapeHtml4(line, true)).append("\n");
                    do {
                        line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        lineNumber++;
                        if (line.isEmpty()) {
                            break;
                        }
                        builder.append(escapeHtml4(line, true)).append("\n");
                    } while (true);
                    builder.append("</p>\n</div>\n");
                }
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        return builder.toString();
    }

    private void td(final boolean data, final StringBuilder builder, final String line) {
        final char marker = data ? 'd' : 'h';
        builder
                .append("<t")
                .append(marker)
                .append(" class=\"tableblock halign-left valign-top\">")
                .append("<p class=\"tableblock\">")
                .append(escapeHtml4(line, true))
                .append("</p></t")
                .append(marker)
                .append(">\n");
    }

    private String escapeHtml4(final String value, final boolean parse) {
        String htmlisable = value;
        for (final String[] replacement : HTML_REPLACEMENTS) {
            htmlisable = htmlisable.replace(replacement[0], replacement[1]);
        }
        if (parse) { // handle code for now
            while (true) {
                final int codeIdx = htmlisable.indexOf("`");
                if (codeIdx >= 0) {
                    final int endCode = htmlisable.indexOf("`", codeIdx + 1);
                    if (endCode > 0) {
                        htmlisable =
                                htmlisable.substring(0, codeIdx) + "<pre>" + htmlisable.substring(codeIdx + 1, endCode)
                                        + "</pre>" + htmlisable.substring(endCode + 1);
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return htmlisable;
    }
}
