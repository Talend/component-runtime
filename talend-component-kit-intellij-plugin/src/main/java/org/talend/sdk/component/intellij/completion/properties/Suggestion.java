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
package org.talend.sdk.component.intellij.completion.properties;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupElementRenderer;
import com.intellij.icons.AllIcons;

import org.talend.sdk.component.intellij.Icons;

public class Suggestion implements Comparable<Suggestion> {

    private final List<SuggestionNode> nodes = new ArrayList<>();

    public Suggestion append(final SuggestionNode n) {
        if (n != null) {
            nodes.add(n);
        }
        return this;
    }

    public String getFullKey() {
        return nodes.stream().map(SuggestionNode::getKey).collect(joining("."));
    }

    public boolean isFamily() {
        return nodes.size() == 2 && nodes.get(0).isFamily() && nodes.get(1).isLeaf();
    }

    public boolean isComponent() {
        return nodes.stream().anyMatch(SuggestionNode::isComponent);
    }

    public boolean isConfigClass() {
        return nodes.stream().anyMatch(SuggestionNode::isConfigClass);
    }

    @Override
    public int compareTo(final Suggestion o) {
        return this.getFullKey().compareTo(o.getFullKey());
    }

    public LookupElementBuilder newLookupElement() {
        return LookupElementBuilder.create(this, getFullKey()).withRenderer(new LookupElementRenderer<LookupElement>() {

            @Override
            public void renderElement(final LookupElement element, final LookupElementPresentation presentation) {
                final Suggestion suggestion = Suggestion.class.cast(element.getObject());
                presentation.setItemText(suggestion.getFullKey());
                if (suggestion.isFamily()) {
                    presentation.setIcon(AllIcons.Nodes.ModuleGroup);
                    presentation.appendTailText("  Family", true);
                }
                if (suggestion.isComponent()) {
                    presentation.setIcon(Icons.TACOKIT);
                    presentation.appendTailText("  Component", true);
                }
                if (suggestion.isConfigClass()) {
                    presentation.setIcon(AllIcons.Hierarchy.Class);
                    presentation.appendTailText("  Configuration", true);
                }
            }
        });
    }
}
