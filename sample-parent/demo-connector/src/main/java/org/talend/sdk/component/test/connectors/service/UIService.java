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
package org.talend.sdk.component.test.connectors.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.completion.SuggestionValues;
import org.talend.sdk.component.api.service.completion.SuggestionValues.Item;
import org.talend.sdk.component.api.service.completion.Suggestions;
import org.talend.sdk.component.api.service.update.Update;
import org.talend.sdk.component.test.connectors.config.NestedConfig;

@Service
public class UIService {

    // TODO add translation for the services

    public final static String LIST_ENTITIES = "action_LIST_ENTITIES";

    public final static String UPDATE_NESTED_CONFIG = "action_UPDATE_NESTED_CONFIG";

    @Service
    private I18n i18n;

    /**
     * Suggestions action without any configuration.
     * 
     * @return
     */
    @Suggestions(LIST_ENTITIES)
    public SuggestionValues getListEntities() {

        List<Item> entities = Arrays.asList(1, 2, 3, 4)
                .stream()
                .map(i -> String.valueOf(i))
                .map(i -> new Item(i, i18n.entityName(i)))
                .collect(Collectors.toList());

        return new SuggestionValues(true, entities);
    }

    @Update(UPDATE_NESTED_CONFIG)
    public NestedConfig retrieveFeedback(final NestedConfig source) throws Exception {
        NestedConfig dest = new NestedConfig();
        dest.setStringOption1("set by service : " + source.getStringOption1());
        dest.setStringOption2("set by service : " + source.getStringOption2());
        return dest;
    }

}
