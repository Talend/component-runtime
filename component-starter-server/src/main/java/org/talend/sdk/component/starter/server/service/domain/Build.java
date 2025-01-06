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
package org.talend.sdk.component.starter.server.service.domain;

import java.util.List;

import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Build {

    private final String artifact;

    private final String group;

    private final String version;

    private final String mainJavaDirectory;

    private final String testJavaDirectory;

    private final String mainResourcesDirectory;

    private final String testResourcesDirectory;

    private final String mainWebResourcesDirectory;

    private final String buildFileName;

    private final String buildFileContent;

    private final String buildDir;

    private final List<FacetGenerator.InMemoryFile> wrapperFiles;
}
