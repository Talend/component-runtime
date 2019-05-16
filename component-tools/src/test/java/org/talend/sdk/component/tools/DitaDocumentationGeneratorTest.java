/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.ziplock.IO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class DitaDocumentationGeneratorTest extends GeneratorBase {

    @Test
    void generateDita(@TempDir final File temporaryFolder, final TestInfo info) throws IOException {
        final File output = new File(temporaryFolder, info.getTestMethod().get().getName() + ".zip");
        new DitaDocumentationGenerator(
                new File[] {
                        copyBinaries("org.talend.test.valid", temporaryFolder, info.getTestMethod().get().getName()) },
                Locale.ROOT, log, output, true, true).run();
        assertTrue(output.exists());
        final Map<String, String> files = new HashMap<>();
        try (final ZipInputStream zip = new ZipInputStream(new FileInputStream(output))) {
            ZipEntry nextEntry;
            while ((nextEntry = zip.getNextEntry()) != null) {
                files.put(nextEntry.getName(), IO.slurp(zip));
            }
        }
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<!DOCTYPE topic PUBLIC \"-//OASIS//DTD DITA Topic//EN\" \"topic.dtd\">\n"
                + "<topic id=\"connector-test-my\" xml:lang=\"en\">\n" + "  <title>my parameters</title>\n"
                + "  <shortdesc>super my component</shortdesc>\n" + "  <prolog>\n" + "    <metadata>\n"
                + "      <othermeta content=\"test\" name=\"pageid\"/>\n" + "    </metadata>\n" + "  </prolog>\n"
                + "  <body outputclass=\"subscription\">\n"
                + "    <section id=\"section_connector-test-my\" outputclass=\"subscription\">\n"
                + "      <title>Parameters for test my component.</title>\n"
                + "      <table colsep=\"1\" frame=\"all\" rowsep=\"1\">\n" + "        <tgroup cols=\"4\">\n"
                + "          <colspec colname=\"c1\" colnum=\"1\" colwidth=\"1*\"/>\n"
                + "          <colspec colname=\"c2\" colnum=\"2\" colwidth=\"1*\"/>\n"
                + "          <colspec colname=\"c3\" colnum=\"3\" colwidth=\"1*\"/>\n"
                + "          <colspec colname=\"c4\" colnum=\"4\" colwidth=\"1*\"/>\n" + "          <thead>\n"
                + "            <row>\n" + "              <entry>Display Name</entry>\n"
                + "              <entry>Description</entry>\n" + "              <entry>Default Value</entry>\n"
                + "              <entry>Enabled If</entry>\n" + "            </row>\n" + "          </thead>\n"
                + "          <tbody>\n" + "            <row>\n" + "              <entry>configuration</entry>\n"
                + "              <entry>configuration configuration</entry>\n" + "              <entry>-</entry>\n"
                + "              <entry>Always enabled</entry>\n" + "            </row>\n" + "            <row>\n"
                + "              <entry>input</entry>\n" + "              <entry>the input value</entry>\n"
                + "              <entry>-</entry>\n" + "              <entry>Always enabled</entry>\n"
                + "            </row>\n" + "            <row>\n" + "              <entry>nested</entry>\n"
                + "              <entry>it is nested</entry>\n" + "              <entry>-</entry>\n"
                + "              <entry>Always enabled</entry>\n" + "            </row>\n" + "            <row>\n"
                + "              <entry>datastore</entry>\n" + "              <entry>the datastore</entry>\n"
                + "              <entry>-</entry>\n" + "              <entry>Always enabled</entry>\n"
                + "            </row>\n" + "            <row>\n" + "              <entry>user</entry>\n"
                + "              <entry>the user to log in</entry>\n" + "              <entry>unknown</entry>\n"
                + "              <entry>Always enabled</entry>\n" + "            </row>\n" + "          </tbody>\n"
                + "        </tgroup>\n" + "      </table>\n" + "    </section>\n" + "  </body>\n" + "</topic>\n",
                files.get("generateDita/test/my.dita"));
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<!DOCTYPE topic PUBLIC \"-//OASIS//DTD DITA Topic//EN\" \"topic.dtd\">\n"
                + "<topic id=\"connector-test-my2\" xml:lang=\"en\">\n" + "  <title>my2 parameters</title>\n"
                + "  <shortdesc>super my component2</shortdesc>\n" + "  <prolog>\n" + "    <metadata>\n"
                + "      <othermeta content=\"test\" name=\"pageid\"/>\n" + "    </metadata>\n" + "  </prolog>\n"
                + "  <body outputclass=\"subscription\">\n"
                + "    <section id=\"section_connector-test-my2\" outputclass=\"subscription\">\n"
                + "      <title>Parameters for test my2 component.</title>\n"
                + "      <table colsep=\"1\" frame=\"all\" rowsep=\"1\">\n" + "        <tgroup cols=\"4\">\n"
                + "          <colspec colname=\"c1\" colnum=\"1\" colwidth=\"1*\"/>\n"
                + "          <colspec colname=\"c2\" colnum=\"2\" colwidth=\"1*\"/>\n"
                + "          <colspec colname=\"c3\" colnum=\"3\" colwidth=\"1*\"/>\n"
                + "          <colspec colname=\"c4\" colnum=\"4\" colwidth=\"1*\"/>\n" + "          <thead>\n"
                + "            <row>\n" + "              <entry>Display Name</entry>\n"
                + "              <entry>Description</entry>\n" + "              <entry>Default Value</entry>\n"
                + "              <entry>Enabled If</entry>\n" + "            </row>\n" + "          </thead>\n"
                + "          <tbody>\n" + "            <row>\n" + "              <entry>ds</entry>\n"
                + "              <entry>ds configuration</entry>\n" + "              <entry>-</entry>\n"
                + "              <entry>Always enabled</entry>\n" + "            </row>\n" + "            <row>\n"
                + "              <entry>datastore</entry>\n" + "              <entry>the datastore</entry>\n"
                + "              <entry>-</entry>\n" + "              <entry>Always enabled</entry>\n"
                + "            </row>\n" + "          </tbody>\n" + "        </tgroup>\n" + "      </table>\n"
                + "    </section>\n" + "  </body>\n" + "</topic>\n", files.get("generateDita/test/my2.dita"));
        assertEquals(4, files.size());
        // folders
        assertEquals("", files.get("generateDita/test/"));
        assertEquals("", files.get("generateDita/"));
    }
}
