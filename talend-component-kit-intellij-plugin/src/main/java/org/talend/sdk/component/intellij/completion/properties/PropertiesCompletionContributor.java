package org.talend.sdk.component.intellij.completion.properties;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.patterns.PlatformPatterns;

public class PropertiesCompletionContributor extends CompletionContributor {

    public PropertiesCompletionContributor() {

        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(PropertiesLanguage.INSTANCE),
                new PropertiesCompletionProvider());
    }
}
