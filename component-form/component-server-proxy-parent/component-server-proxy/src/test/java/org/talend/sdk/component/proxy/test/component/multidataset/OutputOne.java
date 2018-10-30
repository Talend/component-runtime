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
package org.talend.sdk.component.proxy.test.component.multidataset;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.processor.*;
import org.talend.sdk.component.api.record.Record;

@Processor(name = "Output", family = "MultiDatasetFamily")
@Icon(value = Icon.IconType.CUSTOM, custom = "myicon")
public class OutputOne {

    private DataSetOne dataSetOne;

    public OutputOne(@Option DataSetOne dataSetOne) {
        this.dataSetOne = dataSetOne;
    }

    @BeforeGroup
    public void beforeGroup() {

    }

    @ElementListener
    public void elementListener(@Input final Record record) {

    }

    @AfterGroup
    public void afterGroup() {

    }

}
