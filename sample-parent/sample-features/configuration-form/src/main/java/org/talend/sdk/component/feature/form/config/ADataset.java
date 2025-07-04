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
package org.talend.sdk.component.feature.form.config;

import java.io.Serializable;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout.FormType;
import org.talend.sdk.component.api.configuration.ui.widget.Structure;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.feature.form.service.UIService;

import lombok.Data;

@Data
@DataSet
@GridLayout(value = {
        @GridLayout.Row({ "datastore" }),
        @GridLayout.Row({ "entity" }),
        @GridLayout.Row({ "schema" }),
})
@GridLayout(names = FormType.ADVANCED, value = { @GridLayout.Row({ "datastore" }) })
public class ADataset implements Serializable {

    @Option
    @Documentation("The connection part.")
    private ADatastore datastore;

    @Option
    @Documentation("A login.")
    private Entity entity;

    @Option
    @Structure(type = Structure.Type.OUT, discoverSchema = UIService.DISCOVERSCHEMA)
    @Documentation("The schema information.")
    private List<StudioSchema> schema;

}