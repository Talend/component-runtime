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
package org.talend.sdk.component.intellij.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.talend.sdk.component.intellij.completion.properties.Suggestion.DISPLAY_NAME;
import static org.talend.sdk.component.intellij.completion.properties.Suggestion.PLACEHOLDER;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

import org.jetbrains.annotations.NotNull;
import org.talend.sdk.component.intellij.completion.properties.Suggestion;

public class SuggestionServiceImpl implements SuggestionService {

    private static final String COMPONENTS = "org.talend.sdk.component.api.component.Components";

    private static final String OPTION = "org.talend.sdk.component.api.configuration.Option";

    private static final String PROCESSOR = "org.talend.sdk.component.api.processor.Processor";

    private static final String PARTITION_MAPPER = "org.talend.sdk.component.api.input.PartitionMapper";

    private static final String EMITTER = "org.talend.sdk.component.api.input.Emitter";

    private static final String DATA_STORE = "org.talend.sdk.component.api.configuration.type.DataStore";

    private static final String DATA_SET = "org.talend.sdk.component.api.configuration.type.DataSet";

    private static final String ACTION_TYPE = "org.talend.sdk.component.api.service.ActionType";

    private static final String ACTION_ACTION = "org.talend.sdk.component.api.service.Action";

    private static final String ACTION_HEALTHCHECK = "org.talend.sdk.component.api.service.healthcheck.HealthCheck";

    private static final String ACTION_ASYNCVALIDATION =
            "org.talend.sdk.component.api.service.asyncvalidation.AsyncValidation";

    private static final String ACTION_DYNAMICVALUES = "org.talend.sdk.component.api.service.completion.DynamicValues";

    private static final String ACTION_SUGGESTIONS = "org.talend.sdk.component.api.service.completion.Suggestions";

    private static final String ACTION_DISCOVERSCHEMA = "org.talend.sdk.component.api.service.schema.DiscoverSchema";

    private static final String ACTION_UPDATE = "org.talend.sdk.component.api.service.update.Update";

    private static final List<String> ACTIONS = List.of(ACTION_ACTION, ACTION_HEALTHCHECK, ACTION_ASYNCVALIDATION,
            ACTION_DYNAMICVALUES, ACTION_SUGGESTIONS, ACTION_DISCOVERSCHEMA, ACTION_UPDATE);

    private static final Predicate<PsiClass> IS_STATIC = clazz -> clazz.hasModifierProperty(PsiModifier.STATIC);

    @Override
    public boolean isSupported(final CompletionParameters completionParameters) {
        final String name = completionParameters.getOriginalFile().getName();
        return "Messages.properties".equals(name);
    }

    @Override
    public List<LookupElement> computeValueSuggestions(final String text) {
        final String[] segments = text.split("\\.");
        switch (segments.length) {
            case 2:
                return singletonList(new Suggestion(toHumanString(segments[0]), null)
                        .newLookupElement(withPriority(Suggestion.Type.Family)));
            case 3:
                return singletonList(new Suggestion(toHumanString(segments[1]), null)
                        .newLookupElement(withPriority(Suggestion.Type.Configuration)));
            case 5: // action format
                return singletonList(new Suggestion(toHumanString(segments[3]), null)
                        .newLookupElement(withPriority(Suggestion.Type.Action)));
            default:
                return emptyList();
        }
    }

    @Override
    public List<LookupElement> computeKeySuggestions(final Project project, final Module module,
                                                     final String packageName, final List<String> containerElements, final String query) {
        final JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        final PsiPackage pkg = javaPsiFacade.findPackage(packageName);
        if (pkg == null) {
            return Collections.emptyList();
        }

        final String defaultFamily = getFamilyFromPackageInfo(pkg, module);
        return Stream
                .concat(Stream
                                .concat(of(pkg.getClasses())
                                                .flatMap(this::unwrapInnerClasses)
                                                .filter(c -> AnnotationUtil
                                                        .findAnnotation(c, PARTITION_MAPPER, PROCESSOR, EMITTER) != null)
                                                .flatMap(clazz -> fromComponent(clazz, defaultFamily)),
                                        of(pkg.getClasses())
                                                .flatMap(this::unwrapInnerClasses)
                                                .filter(c -> of(c.getAllFields())
                                                        .anyMatch(f -> AnnotationUtil.findAnnotation(f, OPTION) != null))
                                                .flatMap(c -> fromConfiguration(defaultFamily, c.getName(), c))),
                        of(pkg.getClasses())
                                .flatMap(this::unwrapInnerClasses)
                                .flatMap(c -> of(c.getMethods())
                                        .filter(m -> ACTIONS
                                                .stream()
                                                .anyMatch(action -> AnnotationUtil.findAnnotation(m, action) != null)))
                                .map(m -> ACTIONS
                                        .stream()
                                        .map(action -> AnnotationUtil.findAnnotation(m, action))
                                        .filter(Objects::nonNull)
                                        .findFirst()
                                        .get())
                                .map(action -> fromAction(action, defaultFamily))
                                .filter(Optional::isPresent)
                                .map(Optional::get))
                .filter(s -> containerElements.isEmpty() || !containerElements.contains(s.getKey()))
                .filter(s -> query == null || query.isEmpty() || s.getKey().startsWith(query))
                .map(s -> s.newLookupElement(withPriority(s.getType())))
                .collect(toList());
    }

    private Optional<Suggestion> fromAction(final PsiAnnotation action, final String defaultFamily) {

        final String actionType = ofNullable(action.getNameReferenceElement())
                .map(PsiReference::resolve)
                .filter(PsiClass.class::isInstance)
                .map(e -> (PsiClass) e)
                .map(e -> AnnotationUtil.findAnnotation(e, true, ACTION_TYPE))
                .map(e -> e.findAttributeValue("value"))
                .map(v -> removeQuotes(v.getText()))
                .orElse("");

        return ofNullable(action.findAttributeValue("value"))
                .map(t -> removeQuotes(t.getText()))
                .map(actionId -> new Suggestion(
                        defaultFamily + ".actions." + actionType + "." + actionId + "." + DISPLAY_NAME,
                        Suggestion.Type.Action));
    }

    private String toHumanString(final String segment) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segment.length(); i++) {
            final char ch = segment.charAt(i);
            if (i == 0) {
                builder.append(Character.toUpperCase(ch));
            } else {
                if (Character.isUpperCase(ch)) {
                    builder.append(" ");
                }
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private Stream<PsiClass> unwrapInnerClasses(final PsiClass c) {
        return Stream
                .concat(Stream.of(c),
                        Stream.of(c.getAllInnerClasses()).filter(IS_STATIC).flatMap(this::unwrapInnerClasses));
    }

    private int withPriority(final Suggestion.Type type) {
        if (Suggestion.Type.Action.equals(type)) {
            return 5;
        }
        if (Suggestion.Type.Family.equals(type)) {
            return 3;
        }
        if (Suggestion.Type.Component.equals(type)) {
            return 2;
        }
        return 1;
    }

    private Stream<Suggestion> fromComponent(final PsiClass clazz, final String defaultFamily) {
        final PsiAnnotation componentAnnotation =
                AnnotationUtil.findAnnotation(clazz, PARTITION_MAPPER, PROCESSOR, EMITTER);
        if (componentAnnotation == null) {
            return Stream.empty();
        }

        final PsiAnnotationMemberValue name = componentAnnotation.findAttributeValue("name");
        if (name == null || "\"\"".equals(name.getText())) {
            return Stream.empty();
        }

        final PsiAnnotationMemberValue familyValue = componentAnnotation.findAttributeValue("family");
        final String componentFamily = (familyValue == null || removeQuotes(familyValue.getText()).isEmpty()) ? null
                : removeQuotes(familyValue.getText());

        final String family = ofNullable(componentFamily).orElse(defaultFamily);
        if (family == null) {
            return Stream.empty();
        }

        return Stream
                .of(new Suggestion(family + "." + DISPLAY_NAME, Suggestion.Type.Family), new Suggestion(
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
        return Stream
                .concat(of(configClazz.getAllFields())
                        .filter(field -> AnnotationUtil.findAnnotation(field, OPTION) != null)
                        .flatMap(field -> {
                            final PsiAnnotation fOption = AnnotationUtil.findAnnotation(field, OPTION);
                            final PsiAnnotationMemberValue fOptionValue = fOption.findAttributeValue("value");

                            String fieldName = removeQuotes(fOptionValue.getText());
                            if (fieldName.isEmpty()) {
                                fieldName = field.getName();
                            }

                            final Suggestion displayNameSuggestion =
                                    new Suggestion(configurationName + "." + fieldName + "." + DISPLAY_NAME,
                                            Suggestion.Type.Configuration);
                            final PsiType type = field.getType();
                            if ("java.lang.String".equals(type.getCanonicalText())) {
                                return Stream
                                        .of(displayNameSuggestion,
                                                new Suggestion(configurationName + "." + fieldName + "." + PLACEHOLDER,
                                                        Suggestion.Type.Configuration));
                            }
                            final PsiClass clazz = findClass(type);
                            if (clazz != null && clazz.isEnum()) {
                                return Stream
                                        .concat(Stream.of(displayNameSuggestion),
                                                Stream
                                                        .of(clazz.getFields())
                                                        .filter(PsiEnumConstant.class::isInstance)
                                                        .map(f -> clazz
                                                                .getName()
                                                                .substring(clazz.getName().lastIndexOf('.') + 1) + '.'
                                                                + f.getName() + "._displayName")
                                                        .map(v -> new Suggestion(v, Suggestion.Type.Configuration)));
                            }
                            return Stream.of(displayNameSuggestion);
                        }), extractConfigTypes(family, configClazz));
    }

    private PsiClass findClass(final PsiType type) {
        if (type instanceof PsiClass) {
            return (PsiClass) type;
        }
        if (type instanceof PsiClassReferenceType) {
            return ((PsiClassReferenceType) type).resolve();
        }
        return null;
    }

    private String getFamilyFromPackageInfo(final PsiPackage psiPackage, final Module module) {
        final PsiManager psiManager = PsiManager.getInstance(psiPackage.getProject());
        return FilenameIndex
                .getVirtualFilesByName("package-info.java", GlobalSearchScope.moduleScope(module))
                .stream()
                .map(psiManager::findFile)
                .filter(PsiJavaFile.class::isInstance)
                .map(PsiJavaFile.class::cast)
                .filter(psiFile -> psiFile.getPackageName().equals(psiPackage.getQualifiedName()))
                .map(psiFile -> {
                    final String[] family = {null};
                    psiFile.accept(new JavaRecursiveElementWalkingVisitor() {

                        @Override
                        public void visitAnnotation(@NotNull final PsiAnnotation annotation) {
                            super.visitAnnotation(annotation);
                            if (!COMPONENTS.equals(annotation.getQualifiedName())) {
                                return;
                            }
                            final PsiAnnotationMemberValue familyAttribute =
                                    annotation.findAttributeValue("family");
                            if (familyAttribute == null) {
                                return;
                            }
                            family[0] = removeQuotes(familyAttribute.getText());
                        }
                    });
                    return family[0];
                })
                .findFirst()
                .orElseGet(() -> {
                    final PsiPackage parent = psiPackage.getParentPackage();
                    if (parent == null) {
                        return null;
                    }
                    return getFamilyFromPackageInfo(parent, module);
                });
    }

    private String removeQuotes(final String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.replaceAll("^\"|\"$", "");
    }
}
