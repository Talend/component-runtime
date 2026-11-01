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

import java.io.IOException;
import java.lang.System.Logger;

import javafx.application.Application;
import javafx.stage.Stage;

public class TcketlApp extends Application {

    private static final Logger LOGGER = System.getLogger(TcketlApp.class.getName());

    private CompositionRoot root;

    @Override
    public void start(final Stage stage) throws IOException {
        root = CompositionRoot.create(getParameters().getRaw());
        stage.setScene(MainWindow.createScene(root, stage));
        stage.show();
        root.start();
        LOGGER.log(Logger.Level.INFO, "tcketl started");
    }

    @Override
    public void stop() {
        if (root != null) {
            root.close();
        }
    }
}
