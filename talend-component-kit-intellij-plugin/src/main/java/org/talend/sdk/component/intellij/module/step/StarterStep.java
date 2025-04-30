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
package org.talend.sdk.component.intellij.module.step;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static java.util.Optional.ofNullable;
import static org.talend.sdk.component.intellij.Configuration.getMessage;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBLoadingPanel;

import org.talend.sdk.component.intellij.Configuration;
import org.talend.sdk.component.intellij.module.ProjectCreationRequest;
import org.talend.sdk.component.intellij.module.TalendModuleBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class StarterStep extends ModuleWizardStep implements Disposable {

    private final TalendModuleBuilder builder;

    private StarterPanel starterPanel;

    private final JBLoadingPanel loadingPanel = new JBLoadingPanel(new BorderLayout(), this, 100);

    private ProjectCreationRequest request;

    public StarterStep(final TalendModuleBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void _init() {
        super._init();
        loadingPanel.getContentPanel().removeAll();
        loadingPanel.startLoading();
        getApplication().executeOnPooledThread(() -> loadingPanel.setLoadingText(getMessage("wizard.starter.loading")));
        if (starterPanel == null || starterPanel.panelLoaded.isCompletedExceptionally()) {
            starterPanel = new StarterPanel();
        }
        starterPanel.getPanelLoaded().handle((panel, ex) -> {
            getApplication().executeOnPooledThread(() -> {
                if (ex != null) {
                    showErrorDialog(ex.getMessage(), getMessage("wizard.starter.loading.error.title"));
                } else {
                    loadingPanel.add(panel);
                    loadingPanel.stopLoading();
                    loadingPanel.revalidate();
                }
            });
            return panel;
        });
    }

    private CompletableFuture<ProjectCreationRequest> getProjectInformation() {
        final CompletableFuture<ProjectCreationRequest> result = new CompletableFuture<>();
        final WebEngine webEngine = starterPanel.getWebEngine();
        if (webEngine == null) {
            result.completeExceptionally(new NullPointerException("webengine is null"));
            return result;
        }
        starterPanel.document.handle((document, ex) -> {
            if (ex != null) {
                result.completeExceptionally(ex);
                return document;
            }
            Platform.runLater(() -> {
                if (document == null) {
                    result.completeExceptionally(new IllegalArgumentException("No document available"));
                    return;
                }

                final Element el = document.getElementById("go-to-finish-button");
                if (el == null) {
                    result.completeExceptionally(new IllegalArgumentException("No finish step found"));
                    return;
                }
                webEngine.executeScript("document.getElementById(\"go-to-finish-button\").click()");

                final Element form = document.getElementById("download-zip-form");
                if (form == null) {
                    result.completeExceptionally(new IllegalArgumentException("No download form in the page"));
                    return;
                }

                final Node input = form.getElementsByTagName("input").item(0); // project data
                final String action = form.getAttribute("action");
                final String method = form.getAttribute("method");
                final String project = input.getAttributes().getNamedItem("value").getTextContent();
                result.complete(new ProjectCreationRequest(action, method, project));
            });
            return document;
        });
        return result;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        if (!starterPanel.getPanelLoaded().isDone()) {
            throw new ConfigurationException(getMessage("wizard.starter.loading"), getMessage("wait.message"));
        }
        if (starterPanel.getPanelLoaded().isCompletedExceptionally()) {
            final AtomicReference<Throwable> error = new AtomicReference<>();
            starterPanel.getPanelLoaded().handle((r, t) -> {
                if (t != null) {
                    error.set(t);
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    throw new IllegalStateException(t);
                }
                return r;
            });
            throw new ConfigurationException(
                    ofNullable(error.get()).map(Throwable::getMessage).orElseGet(() -> getMessage("fail.to.load")),
                    getMessage("fail.to.load"));
        }

        try {
            request = getProjectInformation().get();
        } catch (final InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ConfigurationException(e.getMessage());
        }

        return true;
    }

    @Override
    public JComponent getComponent() {
        return loadingPanel;
    }

    @Override
    public void updateDataModel() {
        builder.updateQuery(request);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return starterPanel == null ? null : starterPanel.jfxPanel;
    }

    @Override
    public void dispose() {
        // no-op
    }

    public static final class StarterPanel extends JPanel {

        private final JFXPanel jfxPanel;

        private WebEngine webEngine;

        private WebView browser;

        private final CompletableFuture<Document> document = new CompletableFuture<>();

        private final CompletableFuture<StarterPanel> panelLoaded = new CompletableFuture<>();

        StarterPanel() {
            super(new BorderLayout());
            Platform.setImplicitExit(false);
            jfxPanel = new JFXPanel();
            add(jfxPanel, BorderLayout.CENTER);
            final String css = Base64
                    .getMimeEncoder()
                    .encodeToString(("div[class^=\"Finish__bigButton\"]{display:none !important;}\n"
                            + "#go-to-finish-button{color:white !important;}\n"
                            + "#go-to-finish-button:before{display:none !important;}\n"
                            + "div[class^=\"Drawer__tc-drawer-container\"] > div:nth-child(2) {display:block !important;}\n"
                            + "div[class^=\"Drawer__tc-drawer-container\"] > div:nth-child(2) {display:block !important;}\n"
                            + "input {height:25px !important;}\n" + "#step-finish{display:none !important;}\n"
                            + "#step-add-component{border-left: none;}\n").getBytes(StandardCharsets.UTF_8));
            Platform.runLater(() -> {
                browser = new WebView();
                webEngine = browser.getEngine();
                webEngine.getLoadWorker().exceptionProperty().addListener((observable, oldValue, newValue) -> {
                    panelLoaded.completeExceptionally(newValue);
                    document.completeExceptionally(newValue);
                });
                webEngine
                        .getLoadWorker()
                        .stateProperty()
                        .addListener((ObservableValue<? extends Worker.State> observable, Worker.State oldValue,
                                      Worker.State newValue) -> {
                            switch (newValue) {
                                case FAILED:
                                case CANCELLED:
                                    panelLoaded
                                            .completeExceptionally(
                                                    new IllegalStateException("failed loading panel: " + newValue));
                                    document
                                            .completeExceptionally(
                                                    new IllegalStateException("Didn't load the starter properly"));
                                    break;
                                case SUCCEEDED:
                                    webEngine.setUserStyleSheetLocation("data:text/css;charset=utf-8;base64," + css);
                                    panelLoaded.complete(StarterPanel.this);
                                    break;
                                default:
                            }
                        });
                webEngine
                        .documentProperty()
                        .addListener((observable, oldValue, newValue) -> document.complete(newValue));
                // todo timeouts, see com.sun.webkit.network.URLLoader.prepareConnection
                final String starterHost = Configuration.getStarterHost();
                try {
                    new URL(starterHost).openStream().close();
                } catch (final IllegalArgumentException | IOException e) {
                    // no-op, just here to preload the JVM and not wait so much with next line
                }
                webEngine.load(starterHost);
                jfxPanel.setScene(new Scene(browser));
            });
        }

        WebEngine getWebEngine() {
            return webEngine;
        }

        CompletableFuture<StarterPanel> getPanelLoaded() {
            return panelLoaded;
        }
    }

}
