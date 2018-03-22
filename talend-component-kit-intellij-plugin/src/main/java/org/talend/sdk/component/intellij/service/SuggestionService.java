package org.talend.sdk.component.intellij.service;

import java.util.List;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

public interface SuggestionService {

    boolean isSupported(CompletionParameters completionParameters);

    List<LookupElementBuilder> computeSuggestions(Project project, Module module, String packageName,
            List<String> containerElements, String query);

}
