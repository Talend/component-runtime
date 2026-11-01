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
package org.talend.sdk.component.tcketl.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ApplicationResourcesTest {

    private static final String BASE = "/org/talend/sdk/component/tcketl/app/";

    @ParameterizedTest
    @CsvSource({ "MainView.fxml,BorderPane", "connection/ConnectionDialog.fxml,DialogPane" })
    void viewsArePackagedAndWellFormed(final String view, final String root) throws Exception {
        try (final InputStream stream = ApplicationResourcesTest.class.getResourceAsStream(BASE + view)) {
            assertNotNull(stream, view + " must be packaged with the application");
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            assertEquals(root, factory.newDocumentBuilder().parse(stream).getDocumentElement().getNodeName());
        }
    }

    @Test
    void stylesheetIsPackaged() {
        assertNotNull(ApplicationResourcesTest.class.getResource(BASE + "tcketl.css"));
    }
}
