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
package org.talend.sdk.component.intellij.completion.properties;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import javax.swing.Icon;

import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupElementRenderer;
import com.intellij.icons.AllIcons;

import org.talend.sdk.component.intellij.Icons;

import lombok.Data;

@Data
public class Suggestion {

    public static final String DISPLAY_NAME = "_displayName";

    public static final String PLACEHOLDER = "_placeholder";

    private final String key;

    private final Type type;

    public enum Type {
        Family,
        Component,
        Configuration,
        Action
    }

    public LookupElement newLookupElement(final int priority) {
        return PrioritizedLookupElement
                .withPriority(LookupElementBuilder
                        .create(this, getKey())
                        .withRenderer(new LookupElementRenderer<LookupElement>() {

                            @Override
                            public void renderElement(final LookupElement element,
                                    final LookupElementPresentation presentation) {
                                final Suggestion suggestion = Suggestion.class.cast(element.getObject());
                                presentation.setItemText(suggestion.getKey());
                                if (Type.Family.equals(suggestion.getType())) {
                                    presentation.setIcon(AllIcons.Nodes.ModuleGroup);
                                    presentation.appendTailText("  Family", true);
                                } else if (Type.Component.equals(suggestion.getType())) {
                                    presentation.setIcon(Icons.TACOKIT.getIcon());
                                    presentation.appendTailText("  Component", true);
                                } else if (Type.Configuration.equals(suggestion.getType())) {
                                    presentation.setIcon(AllIcons.Hierarchy.Class);
                                    presentation.appendTailText("  Configuration", true);
                                } else if (Type.Action.equals(suggestion.getType())) {
                                    presentation.setIcon(findSubmit());
                                    presentation.appendTailText("  Action", true);
                                }
                            }
                        }), priority);
    }

    private Icon findSubmit() {
        return Stream.of("SetDefault", "Submit1").flatMap(it -> {
            try {
                final Field declaredField = AllIcons.Actions.class.getField(it);
                return Stream.of(Icon.class.cast(declaredField.get(null)));
            } catch (final Exception e) {
                return Stream.empty();
            }
        }).findFirst().orElse(AllIcons.Actions.Forward);
    }
}
