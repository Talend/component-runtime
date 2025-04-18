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
package org.talend.sdk.component.form.internal.converter.impl.widget;

import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.talend.sdk.component.form.internal.lang.CompletionStages;
import org.talend.sdk.component.form.model.uischema.UiSchema;

import lombok.Data;

@Data
class ListItem {

    private final int index;

    private final String[] items;

    private final List<UiSchema> uiSchemas = new ArrayList<>();

    public static void merge(final Collection<CompletionStage<ListItem>> futures, final UiSchema uiSchema) {
        futures
                .stream()
                .map(CompletionStage::toCompletableFuture)
                .map(CompletionStages::get)
                .sorted(comparing(ListItem::getIndex))
                .forEach(it -> uiSchema.getItems().addAll(it.getUiSchemas()));
    }
}
