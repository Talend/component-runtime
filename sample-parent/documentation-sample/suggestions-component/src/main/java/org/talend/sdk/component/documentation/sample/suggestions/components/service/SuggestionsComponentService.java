/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.documentation.sample.suggestions.components.service;

import org.talend.sdk.component.documentation.sample.suggestions.components.processor.SuggestionsProcessorProcessorConfiguration.MyConfig;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.completion.SuggestionValues;
import org.talend.sdk.component.api.service.completion.Suggestions;

import java.util.Arrays;

@Service
public class SuggestionsComponentService {

    // you can put logic here you can reuse in components
    @Suggestions("loadModules")
    public SuggestionValues loadModules(@Option final MyConfig myconfig) {
        return new SuggestionValues(false,
                Arrays
                        .asList(new SuggestionValues.Item("1", "Delete"), new SuggestionValues.Item("2", "Insert"),
                                new SuggestionValues.Item("3", "Update")));
    }
}