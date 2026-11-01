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
package org.talend.sdk.component.tcketl.persistence;

import java.io.IOException;
import java.nio.file.Path;

import org.talend.sdk.component.tcketl.domain.pipeline.Pipeline;

/** Stores pipelines as documents. The calls are blocking and MUST NOT be made from the JavaFX application thread. */
public interface PipelineRepository {

    /** Extension of the pipeline files, for file choosers. */
    String FILE_EXTENSION = ".tcketl.json";

    /**
     * @throws FileFormatException when the file is not a valid pipeline document (syntax, structure, version, values).
     * @throws IOException when the file cannot be read.
     */
    Pipeline load(Path file) throws IOException;

    /** Replaces the file atomically, the previous content is kept when the save fails. */
    void save(Path file, Pipeline pipeline) throws IOException;
}
