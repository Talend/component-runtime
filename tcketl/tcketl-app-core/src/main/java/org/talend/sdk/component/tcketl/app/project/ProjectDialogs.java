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
package org.talend.sdk.component.tcketl.app.project;

import java.nio.file.Path;
import java.util.Optional;

/** What the project commands ask of the user, so that they can be tested without a window. */
public interface ProjectDialogs {

    enum Choice {
        SAVE,
        DISCARD,
        CANCEL
    }

    /** Asks what to do with the unsaved changes of the pipeline called {@code name}. */
    Choice confirmDiscard(String name);

    /** Lets the user pick the file to open, empty when the user gives up. */
    Optional<Path> chooseFileToOpen();

    /**
     * Lets the user pick where to save, empty when the user gives up.
     *
     * @param suggestedName a file name (no folder) to propose
     */
    Optional<Path> chooseFileToSave(String suggestedName);

    void showError(String message);
}
