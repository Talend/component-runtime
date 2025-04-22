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
package org.talend.sdk.component.intellij.completion.properties;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.talend.sdk.component.intellij.util.PsiUtil.findModule;
import static org.talend.sdk.component.intellij.util.PsiUtil.truncateIdeaDummyIdentifier;

import java.util.List;
import java.util.Objects;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.ProcessingContext;

import org.talend.sdk.component.intellij.service.SuggestionService;

public class PropertiesCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(final CompletionParameters completionParameters,
                                  final ProcessingContext processingContext, final CompletionResultSet resultSet) {

        final PsiElement element = completionParameters.getPosition();
        if (!(element instanceof LeafPsiElement)) {
            return; // ignore comment
        }

        final Project project = element.getProject();
        final Module module = findModule(element);
        final SuggestionService service = project.getService(SuggestionService.class);
        if ((module == null || !service.isSupported(completionParameters))) { // limit suggestion to Messages
            return;
        }

        if (element instanceof PropertyValueImpl) {
            ofNullable(element.getPrevSibling())
                    .map(PsiElement::getPrevSibling)
                    .map(PsiElement::getText)
                    .ifPresent(text -> resultSet.addAllElements(service.computeValueSuggestions(text)));
        } else if (element instanceof PropertyKeyImpl) {
            final List<String> containerElements = ((PropertiesFile) element.getContainingFile())
                    .getProperties()
                    .stream()
                    .map(IProperty::getKey)
                    .filter(key -> !Objects.equals(key, element.getText()))
                    .collect(toList());
            final String packageName = getPropertiesPackage(module, completionParameters);
            if (packageName != null) {
                resultSet
                        .addAllElements(service
                                .computeKeySuggestions(project, module, packageName,
                                        containerElements, truncateIdeaDummyIdentifier(element)));
            }
        }
    }

    private String getPropertiesPackage(final Module module, final CompletionParameters completionParameters) {
        final PsiDirectory parentDirectory = completionParameters.getOriginalFile().getParent();
        if (parentDirectory == null) {
            return null;
        }

        final String propPath = parentDirectory.getVirtualFile().getPath();
        return extractPackageName(module, propPath);
    }

    private String extractPackageName(final Module module, final String currentPath) {
        for (final VirtualFile sourceRoot : ModuleRootManager.getInstance(module).getSourceRoots()) {
            final String sourceRootPath = sourceRoot.getPath();
            if (currentPath.startsWith(sourceRootPath)) {
                return currentPath.substring(sourceRootPath.length() + 1)
                        .replace("/", ".")
                        .replace("\\", ".");
            }
        }

        return null;
    }
}
