package org.talend.sdk.component.intellij.completion.properties;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupElementRenderer;
import com.intellij.icons.AllIcons;

import org.jetbrains.annotations.NotNull;
import org.talend.sdk.component.intellij.Icons;

public class Suggestion implements Comparable<Suggestion> {

    private final List<SuggestionNode> nodes = new ArrayList<>();

    public Suggestion append(SuggestionNode n) {
        if (n != null) {
            nodes.add(n);
        }
        return this;
    }

    public String getFullKey() {
        return nodes.stream().map(SuggestionNode::getKey).collect(joining("."));
    }

    public boolean isFamily() {
        return nodes.size() == 2 && nodes.get(0).isFamily()
                && nodes.get(1).isLeaf();
    }

    public boolean isComponent() {
        return nodes.stream().anyMatch(SuggestionNode::isComponent);
    }

    public boolean isConfigClass() {
        return nodes.stream().anyMatch(SuggestionNode::isConfigClass);
    }

    @Override
    public int compareTo(@NotNull final Suggestion o) {
        return this.getFullKey().compareTo(o.getFullKey());
    }

    public LookupElementBuilder newLookupElement() {
        return LookupElementBuilder.create(this, getFullKey())
                .withRenderer(new LookupElementRenderer<LookupElement>() {

                    @Override
                    public void renderElement(LookupElement element, LookupElementPresentation presentation) {
                        Suggestion suggestion = (Suggestion) element.getObject();
                        presentation.setItemText(suggestion.getFullKey());
                        if (suggestion.isFamily()) {
                            presentation.setIcon(AllIcons.Nodes.ModuleGroup);
                            presentation.appendTailText("  Family", true);
                        }
                        if (suggestion.isComponent()) {
                            presentation.setIcon(Icons.Tacokit);
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
