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
package org.talend.sdk.component.intellij.module.step;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static org.talend.sdk.component.intellij.Configuration.getMessage;

import java.awt.BorderLayout;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
        starterPanel = new StarterPanel();
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
        Platform.runLater(() -> {
            final Element el = starterPanel.getWebEngine().getDocument().getElementById("go-to-finish-button");
            if (el != null) {// not on finish page
                starterPanel.getWebEngine().executeScript("document.getElementById(\"go-to-finish-button\").click()");
            }

            final Element form = starterPanel.getWebEngine().getDocument().getElementById("download-zip-form");
            final Node input = form.getElementsByTagName("input").item(0);// project data
            final String action = form.getAttribute("action");
            final String method = form.getAttribute("method");
            final String project = input.getAttributes().getNamedItem("value").getTextContent();
            result.complete(new ProjectCreationRequest(action, method, project));
        });

        return result;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        if (!starterPanel.getPanelLoaded().isDone()) {
            throw new ConfigurationException(getMessage("wizard.starter.loading"), getMessage("wait.message"));
        }

        try {
            request = getProjectInformation().get();
        } catch (InterruptedException | ExecutionException e) {
            if (InterruptedException.class.isInstance(e)) {
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
    public void dispose() {

    }

    public static final class StarterPanel extends JPanel {

        private final JFXPanel jfxPanel;

        private WebEngine webEngine;

        private WebView browser;

        private final CompletableFuture<JPanel> panelLoaded = new CompletableFuture<>();

        StarterPanel() {
            super(new BorderLayout());
            Platform.setImplicitExit(false);
            jfxPanel = new JFXPanel();
            add(jfxPanel, BorderLayout.CENTER);
            final String css = Base64
                    .getMimeEncoder()
                    .encodeToString(
                            ("div[class^=\"Finish__bigButton\"], #go-to-finish-button{display:none  !important;} "
                                    + "div[class^=\"Drawer__tc-drawer-container\"] > div:nth-child(2) {display:block !important;} "
                                    + "input {height:25px !important;}").getBytes(StandardCharsets.UTF_8));
            Platform.runLater(() -> {
                browser = new WebView();
                webEngine = browser.getEngine();
                webEngine
                        .getLoadWorker()
                        .stateProperty()
                        .addListener((ObservableValue<? extends Worker.State> observable, Worker.State oldValue,
                                Worker.State newValue) -> {
                            if (newValue != Worker.State.SUCCEEDED) {
                                if (newValue == Worker.State.CANCELLED || newValue == Worker.State.FAILED) {
                                    panelLoaded
                                            .completeExceptionally(
                                                    new IllegalStateException("failed loading panel: " + newValue));
                                }
                                return;
                            }
                            webEngine.setUserStyleSheetLocation("data:text/css;charset=utf-8;base64," + css);
                            panelLoaded.complete(StarterPanel.this);
                        });
                // todo timeouts, see com.sun.webkit.network.URLLoader.prepareConnection
                webEngine.load(Configuration.getStarterHost());
                jfxPanel.setScene(new Scene(browser));
            });
        }

        WebEngine getWebEngine() {
            return webEngine;
        }

        CompletableFuture<JPanel> getPanelLoaded() {
            return panelLoaded;
        }
    }

}
