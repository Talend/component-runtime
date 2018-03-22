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
package org.talend.sdk.component.intellij.service;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

import org.talend.sdk.component.intellij.completion.properties.Suggestion;
import org.talend.sdk.component.intellij.completion.properties.SuggestionNode;

public class SuggestionServiceImpl implements SuggestionService {

    private static final String COMPONENTS = "org.talend.sdk.component.api.component.Components";

    private static final String OPTION = "org.talend.sdk.component.api.configuration.Option";

    private static final String PROCESSOR = "org.talend.sdk.component.api.processor.Processor";

    private static final String PARTITION_MAPPER = "org.talend.sdk.component.api.input.PartitionMapper";

    private static final String DATA_STORE = "org.talend.sdk.component.api.configuration.type.DataStore";

    private static final String DATA_SET = "org.talend.sdk.component.api.configuration.type.DataSet";

    @Override
    public boolean isSupported(final CompletionParameters completionParameters) {
        final String name = completionParameters.getOriginalFile().getName();
        return "Messages.properties".equals(name);
    }

    @Override
    public List<LookupElementBuilder> computeSuggestions(final Project project, final Module module,
            final String packageName, final List<String> containerElements, final String query) {
        final JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        final PsiPackage pkg = javaPsiFacade.findPackage(packageName);
        if (pkg == null) {
            return Collections.emptyList();
        }
        // COMPONENTS (Service?)
        final List<PsiClass> components = of(pkg.getClasses())
                .filter(c -> AnnotationUtil.findAnnotation(c, PARTITION_MAPPER, PROCESSOR) != null)
                .collect(toList());

        final List<Suggestion> suggestions = new ArrayList<>();
        // get family from package-info
        final String defaultFamily = getFamilyFromPackageInfo(pkg, module, packageName, suggestions);

        components.forEach((PsiClass clazz) -> {
            final PsiAnnotation componentAnnotation = AnnotationUtil.findAnnotation(clazz, PARTITION_MAPPER, PROCESSOR);
            final PsiAnnotationMemberValue name = componentAnnotation.findAttributeValue("name");
            final PsiAnnotationMemberValue familyValue = componentAnnotation.findAttributeValue("family");
            final String family = (familyValue == null || removeQuotes(familyValue.getText()).isEmpty()) ? defaultFamily
                    : removeQuotes(familyValue.getText());

            // Configuration from construct
            final List<PsiParameter> configurations = of(clazz.getConstructors())
                    .flatMap(constructor -> of(constructor.getParameterList().getParameters())
                            .filter(p -> AnnotationUtil.findAnnotation(p, OPTION) != null))
                    .collect(toList());

            configurations.forEach(conf -> {
                final PsiClass configClazz =
                        javaPsiFacade.findClass(conf.getType().getCanonicalText(), conf.getResolveScope());
                if (family != null) { // family & Config Types (datastore | dataset)
                    suggestions.add(new Suggestion()
                            .append(SuggestionNode.builder().key(family).isFamily(true).build())
                            .append(SuggestionNode
                                    .builder()
                                    .key(removeQuotes(name.getText()))
                                    .isComponent(true)
                                    .build())
                            .append(SuggestionNode.DISPLAY_NAME));

                    final PsiAnnotation dataStore = AnnotationUtil.findAnnotation(configClazz, DATA_STORE);
                    if (dataStore != null) {
                        final PsiAnnotationMemberValue dataStoreName = dataStore.findAttributeValue("value");
                        suggestions.add(new Suggestion()
                                .append(SuggestionNode.builder().key(family).isFamily(true).build())
                                .append(SuggestionNode.builder().key("datastore").isConfigClass(true).build())
                                .append(SuggestionNode
                                        .builder()
                                        .key(removeQuotes(dataStoreName.getText()))
                                        .isLeaf(true)
                                        .build())
                                .append(SuggestionNode.DISPLAY_NAME));
                    }

                    final PsiAnnotation dataSet = AnnotationUtil.findAnnotation(configClazz, DATA_SET);
                    if (dataSet != null) {
                        final PsiAnnotationMemberValue dataSetName = dataSet.findAttributeValue("value");
                        suggestions.add(new Suggestion()
                                .append(SuggestionNode.builder().key(family).isFamily(true).build())
                                .append(SuggestionNode.builder().key("dataset").isConfigClass(true).build())
                                .append(SuggestionNode
                                        .builder()
                                        .key(removeQuotes(dataSetName.getText()))
                                        .isLeaf(true)
                                        .build())
                                .append(SuggestionNode.DISPLAY_NAME));
                    }
                }

                final PsiAnnotation option = AnnotationUtil.findAnnotation(conf, OPTION);
                final String configurationName = getConfigurationName(option.findAttributeValue("value"));
                suggestions.add(new Suggestion()
                        .append(SuggestionNode.builder().key(configurationName).isConfigClass(true).build())
                        .append(SuggestionNode.DISPLAY_NAME));

                // Configuration OPTION
                addConfigurationSuggestion(configurationName, configClazz, suggestions, javaPsiFacade, module);
            });
        });

        return suggestions
                .stream()
                .filter(s -> containerElements.isEmpty() || !containerElements.contains(s.getFullKey()))
                .filter(s -> query == null || query.isEmpty() || s.getFullKey().startsWith(query))
                .map(Suggestion::newLookupElement)
                .collect(toList());
    }

    private void addConfigurationSuggestion(final String configurationName, final PsiClass configClazz,
            final List<Suggestion> suggestions, final JavaPsiFacade javaPsiFacade, final Module module) {

        of(configClazz.getAllFields()).forEach(field -> {
            final PsiAnnotation fOption = AnnotationUtil.findAnnotation(field, OPTION);
            if (fOption != null) {
                final PsiAnnotationMemberValue fOptionValue = fOption.findAttributeValue("value");
                String fieldName = field.getName();
                if (!removeQuotes(fOptionValue.getText()).isEmpty()) {
                    fieldName = removeQuotes(fOptionValue.getText());
                }

                if (PsiPrimitiveType.class.isInstance(field.getType())
                        || "java.lang.String".equals(field.getType().getCanonicalText())) {// primitives

                    suggestions.add(new Suggestion()
                            .append(SuggestionNode.builder().key(configurationName).isConfigClass(true).build())
                            .append(SuggestionNode.builder().key(fieldName).isLeaf(true).build())
                            .append(SuggestionNode.DISPLAY_NAME));

                    if ("java.lang.String".equals(field.getType().getCanonicalText())) {// string
                        suggestions.add(new Suggestion()
                                .append(SuggestionNode.builder().key(configurationName).isConfigClass(true).build())
                                .append(SuggestionNode.builder().key(fieldName).isLeaf(true).build())
                                .append(SuggestionNode.PLACEHOLDER));
                    }
                } else { // object
                    final PsiType type = field.getType();
                    final PsiClass nestedClazz =
                            javaPsiFacade.findClass(type.getCanonicalText(), GlobalSearchScope.moduleScope(module));
                    if (nestedClazz == null) {
                        return;
                    }

                    addConfigurationSuggestion(configurationName + "." + fieldName, nestedClazz, suggestions,
                            javaPsiFacade, module);
                }
            }
        });

    }

    private String getFamilyFromPackageInfo(final PsiPackage psiPackage, final Module module, final String packageName,
            final List<Suggestion> suggestions) {
        return of(FilenameIndex.getFilesByName(psiPackage.getProject(), "package-info.java",
                GlobalSearchScope.moduleScope(module))).map(psiFile -> {
                    if (!PsiJavaFile.class.cast(psiFile).getPackageName().equals(psiPackage.getQualifiedName())) {
                        return null;
                    }
                    final String[] family = { null };
                    PsiJavaFile.class.cast(psiFile).accept(new JavaRecursiveElementWalkingVisitor() {

                        @Override
                        public void visitAnnotation(final PsiAnnotation annotation) {
                            super.visitAnnotation(annotation);
                            if (!COMPONENTS.equals(annotation.getQualifiedName())) {
                                return;
                            }
                            final PsiAnnotationMemberValue familyAttribute = annotation.findAttributeValue("family");
                            if (familyAttribute == null) {
                                return;
                            }
                            family[0] = removeQuotes(familyAttribute.getText());
                        }
                    });

                    // if package-info is in the same package as the Message.properties // we add the family name
                    if (psiPackage.getQualifiedName().equals(packageName) && family[0] != null) {
                        suggestions.add(new Suggestion()
                                .append(SuggestionNode.builder().key(family[0]).isFamily(true).build())
                                .append(SuggestionNode.DISPLAY_NAME));
                    }

                    return family[0];
                }).filter(Objects::nonNull).findFirst().orElseGet(() -> {
                    final PsiPackage parent = psiPackage.getParentPackage();
                    if (parent == null) {
                        return null;
                    }

                    return getFamilyFromPackageInfo(parent, module, packageName, suggestions);
                });
    }

    private String getConfigurationName(final PsiAnnotationMemberValue optionValue) {
        String configurationName = "configuration";// default name is configuration form the api
        if (optionValue != null || !removeQuotes(optionValue.getText()).isEmpty()) {
            configurationName = removeQuotes(optionValue.getText());
        }

        return configurationName;
    }

    private String removeQuotes(final String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.replaceAll("^\"|\"$", "");
    }
}
