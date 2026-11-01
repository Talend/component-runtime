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
package org.talend.sdk.component.tcketl.app.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.testing.FxHarness;
import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;
import org.talend.sdk.component.tcketl.domain.server.ServerUnavailableException;

/**
 * Renders the connection dialog off screen, in the states the user meets, to look at it and to check what it shows.
 * It needs the JavaFX toolkit, so it only runs when asked to, with {@code -Dtcketl.ui.snapshots=<directory>}.
 */
@Tag("ui")
@EnabledIfSystemProperty(named = FxHarness.SNAPSHOTS, matches = ".+")
class ConnectionDialogSnapshotTest {

    private final Path directory = FxHarness.snapshotDirectory();

    private ConnectionViewModel model;

    private DialogPane pane;

    private Scene scene;

    @BeforeAll
    static void startToolkit() throws InterruptedException {
        FxHarness.start();
    }

    @Test
    void settingsTheDialogStartsWith() throws Exception {
        show(settings -> new ServerStatus("1.2611.0-SNAPSHOT", "9f2c1ab", "stamp"));

        final int colors = FxHarness.snapshot(scene, directory.resolve("connection-dialog.png"));

        assertTrue(colors > 20, "The dialog is blank: " + colors + " color(s)");
        assertEquals("http://localhost:8080/api/v1", text("#url"));
        assertEquals("", text("#validation"));
        assertFalse(applyIsDisabled());
    }

    @Test
    void serverThatAnswers() throws Exception {
        show(settings -> new ServerStatus("1.2611.0-SNAPSHOT", "9f2c1ab", "stamp"));

        test();

        FxHarness.snapshot(scene, directory.resolve("connection-dialog-success.png"));
        assertEquals("Connected to component server 1.2611.0-SNAPSHOT (commit 9f2c1ab)", text("#testResult"));
        assertTrue(styledAs("#testResult", "success"));
    }

    @Test
    void serverThatDoesNotAnswer() throws Exception {
        show(settings -> {
            throw new ServerUnavailableException("Cannot reach http://localhost:8080/api/v1: Connection refused"
                    + " (is the component server started on that host, with that port?)", null);
        });

        test();

        FxHarness.snapshot(scene, directory.resolve("connection-dialog-failure.png"));
        assertTrue(text("#testResult").contains("Connection refused"), text("#testResult"));
        assertTrue(styledAs("#testResult", "failure"));
        assertTrue(fitsItsRoom("#testResult"), "The message is clipped");
    }

    @Test
    void settingsThatAreNotValid() throws Exception {
        show(settings -> new ServerStatus("1.2611.0-SNAPSHOT", "9f2c1ab", "stamp"));

        FxHarness.call(() -> {
            model.port().set("80x");
            return null;
        });

        FxHarness.snapshot(scene, directory.resolve("connection-dialog-invalid.png"));
        assertEquals("The port must be a number between 1 and 65535.", text("#validation"));
        assertTrue(applyIsDisabled());
        assertTrue(isDisabled("#testButton"));
    }

    private void show(final ConnectionTester tester) throws Exception {
        scene = FxHarness.call(() -> {
            model = new ConnectionViewModel(ApplicationConfig.defaults(), tester, AsyncRunner.direct());
            pane = ConnectionDialog.create(model);
            return new Scene(pane);
        });
    }

    // the runner is direct, so the outcome is there when the call returns
    private void test() throws Exception {
        FxHarness.call(() -> {
            model.test();
            return null;
        });
    }

    private String text(final String selector) throws Exception {
        return FxHarness.call(() -> ((Label) pane.lookup(selector)).getText());
    }

    private boolean styledAs(final String selector, final String styleClass) throws Exception {
        return FxHarness.call(() -> pane.lookup(selector).getStyleClass().contains(styleClass));
    }

    // a wrapped label that is shorter than its text keeps one line and ends it with an ellipsis
    private boolean fitsItsRoom(final String selector) throws Exception {
        return FxHarness.call(() -> {
            final Label label = (Label) pane.lookup(selector);
            return label.getHeight() >= label.prefHeight(label.getWidth());
        });
    }

    private boolean isDisabled(final String selector) throws Exception {
        return FxHarness.call(() -> pane.lookup(selector).isDisabled());
    }

    private boolean applyIsDisabled() throws Exception {
        return FxHarness.call(() -> pane.lookupButton(ConnectionDialog.APPLY).isDisabled());
    }
}
