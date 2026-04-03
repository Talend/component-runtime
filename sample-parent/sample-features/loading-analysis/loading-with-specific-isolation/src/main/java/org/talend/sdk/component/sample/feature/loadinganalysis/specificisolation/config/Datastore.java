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
package org.talend.sdk.component.sample.feature.loadinganalysis.specificisolation.config;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.widget.TextArea;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@DataStore("dyndepsdso")
@GridLayout({
        @GridLayout.Row("noDynamicDependency"),
        @GridLayout.Row("group"),
        @GridLayout.Row("artifact"),
        @GridLayout.Row("version"),
        @GridLayout.Row("clazz"),
        @GridLayout.Row("resource")
})
@GridLayout(names = GridLayout.FormType.ADVANCED, value = {
        @GridLayout.Row("classFilterAcceptAll"),
        @GridLayout.Row("classFilterOption"),
        @GridLayout.Row("parentClassesFilterAcceptAll"),
        @GridLayout.Row("parentClassesFilterOption"),
        @GridLayout.Row("jarAsResource"),
        @GridLayout.Row("parentResourceFilterAcceptAll"),
        @GridLayout.Row("parentResourceFilterOption")
})
public class Datastore implements Serializable {

    @Option
    @Documentation("Don't add dynamic dependency.")
    private boolean noDynamicDependency;

    @Option
    @Documentation("The dependency group.")
    @ActiveIf(target = "noDynamicDependency", value = "false")
    private String group;

    @Option
    @Documentation("The dependency artifact.")
    @ActiveIf(target = "noDynamicDependency", value = "false")
    private String artifact;

    @Option
    @Documentation("The dependency version.")
    @ActiveIf(target = "noDynamicDependency", value = "false")
    private String version;

    @Option
    @Documentation("A class to load.")
    private String clazz;

    @Option
    @Documentation("A resource to load.")
    private String resource;

    @Option
    @Documentation("Child classloader classfilter predicate always true.")
    private boolean classesFilterAcceptAll;

    @Option
    @Documentation("Child classloader class filter. One filter by line. Can be empty/")
    @TextArea
    @ActiveIf(target = "classFilterAcceptAll", value = "false")
    private String classesFilterOption;

    @Option
    @Documentation("Child classloader parentClassesFilter predicate always true.")
    private boolean parentClassesFilterAcceptAll;

    @Option
    @Documentation("Child classloader parent class filter. One filter by line. Can be empty/")
    @TextArea
    @ActiveIf(target = "parentClassesFilterAcceptAll", value = "false")
    private String parentClassesFilterOption;

    @Option
    @Documentation("Jar in jar compliant.")
    private boolean jarAsResource;

    @Option
    @Documentation("Child classloader parent resource filter predicate always true.")
    private boolean parentResourcesFilterAcceptAll;

    @Option
    @Documentation("Child classloader parent resource filter. One filter by line. Can be empty/")
    @TextArea
    @ActiveIf(target = "parentResourceFilterAcceptAll", value = "false")
    private String parentResourcesFilterOption;

}