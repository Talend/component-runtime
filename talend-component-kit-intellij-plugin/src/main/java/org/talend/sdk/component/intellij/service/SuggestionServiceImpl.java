/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.intellij.service;

import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.talend.sdk.component.intellij.completion.properties.Suggestion.DISPLAY_NAME;
import static org.talend.sdk.component.intellij.completion.properties.Suggestion.PLACEHOLDER;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

import org.talend.sdk.component.intellij.completion.properties.Suggestion;

public class SuggestionServiceImpl implements SuggestionService {

    private static final String COMPONENTS = "org.talend.sdk.component.api.component.Components";

    private static final String OPTION = "org.talend.sdk.component.api.configuration.Option";

    private static final String PROCESSOR = "org.talend.sdk.component.api.processor.Processor";

    private static final String PARTITION_MAPPER = "org.talend.sdk.component.api.input.PartitionMapper";

    private static final String EMITTER = "org.talend.sdk.component.api.input.Emitter";

    private static final String DATA_STORE = "org.talend.sdk.component.api.configuration.type.DataStore";

    private static final String DATA_SET = "org.talend.sdk.component.api.configuration.type.DataSet";

    @Override
    public boolean isSupported(final CompletionParameters completionParameters) {
        final String name = completionParameters.getOriginalFile().getName();
        return "Messages.properties".equals(name);
    }

    @Override
    public List<LookupElement> computeSuggestions(final Project project, final Module module, final String packageName,
            final List<String> containerElements, final String query) {
        final JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        final PsiPackage pkg = javaPsiFacade.findPackage(packageName);
        if (pkg == null) {
            return Collections.emptyList();
        }

        final String defaultFamily = getFamilyFromPackageInfo(pkg, module);
        return Stream
                .concat(of(pkg.getClasses())
                        .flatMap(this::unwrapInnerClasses)
                        .filter(c -> AnnotationUtil.findAnnotation(c, PARTITION_MAPPER, PROCESSOR, EMITTER) != null)
                        .flatMap(clazz -> fromComponent(clazz, defaultFamily)),
                        of(pkg.getClasses())
                                .flatMap(this::unwrapInnerClasses)
                                .filter(c -> of(c.getAllFields())
                                        .anyMatch(f -> AnnotationUtil.findAnnotation(f, OPTION) != null))
                                .flatMap(c -> fromConfiguration(defaultFamily, c.getName(), c)))
                .filter(s -> containerElements.isEmpty() || !containerElements.contains(s.getKey()))
                .filter(s -> query == null || query.isEmpty() || s.getKey().startsWith(query))
                .map(s -> s.newLookupElement(withPriority(s)))
                .collect(toList());
    }

    private Stream<PsiClass> unwrapInnerClasses(final PsiClass c) {
        return Stream.concat(Stream.of(c),
                Stream
                        .of(c.getAllInnerClasses())
                        .filter(ic -> Stream.of(ic.getModifiers()).anyMatch(m -> JvmModifier.STATIC == m))
                        .flatMap(this::unwrapInnerClasses));
    }

    private int withPriority(final Suggestion suggestion) {
        if (Suggestion.Type.Family.equals(suggestion.getType())) {
            return 3;
        }
        if (Suggestion.Type.Component.equals(suggestion.getType())) {
            return 2;
        }
        return 1;
    }

    private Stream<Suggestion> fromComponent(final PsiClass clazz, final String defaultFamily) {
        final PsiAnnotation componentAnnotation =
                AnnotationUtil.findAnnotation(clazz, PARTITION_MAPPER, PROCESSOR, EMITTER);
        final PsiAnnotationMemberValue name = componentAnnotation.findAttributeValue("name");
        if (name == null || "\"\"".equals(name.getText())) {
            return Stream.empty();
        }

        final PsiAnnotationMemberValue familyValue = componentAnnotation.findAttributeValue("family");
        final String componentFamily = (familyValue == null || removeQuotes(familyValue.getText()).isEmpty()) ? null
                : removeQuotes(familyValue.getText());

        final String family = ofNullable(componentFamily).orElseGet(() -> ofNullable(defaultFamily).orElse(null));
        if (family == null) {
            return Stream.empty();
        }

        return Stream.of(new Suggestion(family + "." + DISPLAY_NAME, Suggestion.Type.Family), new Suggestion(
                family + "." + removeQuotes(name.getText()) + "." + DISPLAY_NAME, Suggestion.Type.Component));
    }

    private Stream<Suggestion> extractConfigTypes(final String family, final PsiClass configClass) {
        return Stream
                .of(DATA_STORE, DATA_SET)
                .map(a -> AnnotationUtil.findAnnotation(configClass, a))
                .filter(Objects::nonNull)
                .map(a -> new Suggestion(family + '.'
                        + a.getQualifiedName().substring(a.getQualifiedName().lastIndexOf('.') + 1).toLowerCase(ROOT)
                        + '.' + removeQuotes(a.findAttributeValue("value").getText()) + '.' + DISPLAY_NAME,
                        Suggestion.Type.Configuration));
    }

    private Stream<Suggestion> fromConfiguration(final String family, final String configurationName,
            final PsiClass configClazz) {
        return Stream.concat(of(configClazz.getAllFields())
                .filter(field -> AnnotationUtil.findAnnotation(field, OPTION) != null)
                .flatMap(field -> {
                    final PsiAnnotation fOption = AnnotationUtil.findAnnotation(field, OPTION);
                    final PsiAnnotationMemberValue fOptionValue = fOption.findAttributeValue("value");

                    String fieldName = removeQuotes(fOptionValue.getText());
                    if (fieldName.isEmpty()) {
                        fieldName = field.getName();
                    }

                    final Suggestion displayNameSuggestion = new Suggestion(
                            configurationName + "." + fieldName + "." + DISPLAY_NAME, Suggestion.Type.Configuration);
                    if ("java.lang.String".equals(field.getType().getCanonicalText())) {
                        return Stream.of(displayNameSuggestion,
                                new Suggestion(configurationName + "." + fieldName + "." + PLACEHOLDER,
                                        Suggestion.Type.Configuration));
                    }
                    return Stream.of(displayNameSuggestion);
                }), extractConfigTypes(family, configClazz));
    }

    private String getFamilyFromPackageInfo(final PsiPackage psiPackage, final Module module) {
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
                    return family[0];
                }).filter(Objects::nonNull).findFirst().orElseGet(() -> {
                    final PsiPackage parent = psiPackage.getParentPackage();
                    if (parent == null) {
                        return null;
                    }
                    return getFamilyFromPackageInfo(parent, module);
                });
    }

    private String getConfigurationName(final PsiAnnotationMemberValue optionValue, final String defaultName) {
        String configurationName = defaultName;
        if (optionValue != null && optionValue.getText() != null) {
            configurationName = removeQuotes(optionValue.getText());
            if (configurationName.isEmpty()) {
                configurationName = defaultName;
            }
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
