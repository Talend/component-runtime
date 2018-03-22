package org.talend.sdk.component.intellij.completion.properties;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SuggestionNode {

    public static final SuggestionNode DISPLAY_NAME = SuggestionNode.builder()
            .key("_displayName").isLeaf(true).build();

    public static final SuggestionNode PLACEHOLDER = SuggestionNode.builder()
            .key("_placeholder").isLeaf(true).build();

    private final String key;

    private final boolean isFamily;

    private final boolean isComponent;

    private final boolean isLeaf;

    private final boolean isConfigClass;
}
