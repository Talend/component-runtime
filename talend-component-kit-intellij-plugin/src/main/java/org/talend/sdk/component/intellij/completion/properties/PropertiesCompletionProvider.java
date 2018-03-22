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

import static java.util.stream.Collectors.toList;
import static org.talend.sdk.component.intellij.util.PsiUtil.findModule;
import static org.talend.sdk.component.intellij.util.PsiUtil.truncateIdeaDummyIdentifier;

import java.util.List;
import java.util.Objects;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;

import org.talend.sdk.component.intellij.service.SuggestionService;

public class PropertiesCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(final CompletionParameters completionParameters,
            final ProcessingContext processingContext, final CompletionResultSet resultSet) {

        PsiElement element = completionParameters.getPosition();
        if (element instanceof PsiComment) {
            return; // ignore comment
        }

        Project project = element.getProject();
        Module module = findModule(element);
        SuggestionService service = ServiceManager.getService(project, SuggestionService.class);
        if ((module == null || !service.isSupported(completionParameters))) { // limit suggestion to Messages
            return;
        }

        final List<String> containerElements = PropertiesFileImpl.class
                .cast(element.getContainingFile())
                .getProperties()
                .stream()
                .filter(p -> !Objects.equals(p.getKey(), element.getText()))
                .map(IProperty::getKey)
                .collect(toList());

        service
                .computeSuggestions(project, module, getPropertiesPackage(module, completionParameters),
                        containerElements, truncateIdeaDummyIdentifier(element))
                .forEach(resultSet::addElement);
    }

    private String getPropertiesPackage(final Module module, final CompletionParameters completionParameters) {
        final String moduleFilePath = module.getModuleFilePath();
        final String moduleName = module.getName();
        final String moduleDirPath = moduleFilePath.replace(moduleName + ".iml", "");
        final String propPath = completionParameters.getOriginalFile().getVirtualFile().getPath();
        return propPath
                .replace(moduleDirPath, "")
                .replace("/", ".")
                .replace("\\", ".")
                .replace("src.main.resources.", "")
                .replace("." + completionParameters.getOriginalFile().getName(), "");
    }
}
