/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.runtime.internationalization.ParameterBundle;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.Constructors;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ConditionParameterEnricher;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ConfigurationTypeParameterEnricher;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.DocumentationParameterEnricher;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.UiParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.runtime.manager.util.DefaultValueInspector;
import org.talend.sdk.component.runtime.manager.xbean.registry.EnrichedPropertyEditorRegistry;
import org.talend.sdk.component.tools.ComponentHelper.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public abstract class DocBaseGenerator extends BaseTask {

    private final DefaultValueInspector defaultValueInspector = new DefaultValueInspector();

    @Getter
    private final Locale locale;

    private final AbsolutePathResolver resolver = new AbsolutePathResolver();

    protected final Log log;

    protected final File output;

    private final ParameterModelService parameterModelService = new ParameterModelService(
            asList(new DocumentationParameterEnricher(), new ConditionParameterEnricher(),
                    new ConfigurationTypeParameterEnricher(), new UiParameterEnricher()),
            new EnrichedPropertyEditorRegistry()) {
    };

    DocBaseGenerator(final File[] classes, final Locale locale, final Object log, final File output) {
        super(classes);
        this.locale = locale;
        this.output = output;
        try {
            this.log = Log.class.isInstance(log) ? Log.class.cast(log) : new ReflectiveLog(log);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public final void run() {
        final Locale oldLocale = Locale.getDefault();
        final boolean shouldSwitchLocale =
                oldLocale != Locale.ROOT && !ofNullable(oldLocale.getLanguage()).orElse("").equals("en");
        if (shouldSwitchLocale) {
            Locale.setDefault(Locale.ROOT);
        }
        try {
            doRun();
        } finally {
            if (shouldSwitchLocale) {
                Locale.setDefault(oldLocale);
            }
        }
    }

    protected abstract void doRun();

    protected Stream<ComponentDescription> components() {
        final AnnotationFinder finder = newFinder();
        return findComponents(finder).map(component -> {
            final Collection<ParameterMeta> parameterMetas = parameterModelService
                    .buildParameterMetas(Constructors.findConstructor(component),
                            ofNullable(component.getPackage()).map(Package::getName).orElse(""),
                            new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "tools")));
            final Component componentMeta = ComponentHelper
                    .componentMarkers()
                    .filter(component::isAnnotationPresent)
                    .map(component::getAnnotation)
                    .map(ComponentHelper::asComponent)
                    .findFirst()
                    .orElseThrow(NoSuchElementException::new);
            String family = "";
            try {
                family = ComponentHelper.findFamily(componentMeta, component);
            } catch (final IllegalArgumentException iae) {
                // skip for doc
            }
            return new ComponentDescription(component, family, componentMeta.name(), getDoc(component),
                    sort(parameterMetas), resolver, defaultValueInspector, locale, emptyDefaultValue());
        });
    }

    protected Stream<Class<?>> findComponents(final AnnotationFinder finder) {
        return ComponentHelper.componentMarkers().flatMap(a -> finder.findAnnotatedClasses(a).stream());
    }

    private String getDoc(final Class<?> component) {
        final Collection<String> docKeys = Stream
                .of(getComponentPrefix(component), component.getSimpleName())
                .map(it -> it + "._documentation")
                .collect(toList());
        return ofNullable(findResourceBundle(component))
                .map(bundle -> docKeys
                        .stream()
                        .filter(bundle::containsKey)
                        .map(bundle::getString)
                        .findFirst()
                        .map(v -> v + "\n\n")
                        .orElse(null))
                .orElseGet(() -> ofNullable(component.getAnnotation(Documentation.class))
                        .map(Documentation::value)
                        .map(v -> v + "\n\n")
                        .orElse(""));
    }

    private String getComponentPrefix(final Class<?> component) {
        try {
            return ComponentHelper
                    .components(component)
                    .map(c -> ComponentHelper.findFamily(c, component) + "." + c.name())
                    .orElseGet(component::getSimpleName);
        } catch (final RuntimeException e) {
            return component.getSimpleName();
        }
    }

    private Collection<ParameterMeta> sort(final Collection<ParameterMeta> parameterMetas) {
        return parameterMetas.stream().sorted(comparing(ParameterMeta::getPath)).collect(toList());
    }

    protected void write(final File output, final String content) {
        ensureParentExists(output);
        try (final Writer writer = new BufferedWriter(new FileWriter(output))) {
            writer.write(content);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void ensureParentExists(final File output) {
        if (!output.getParentFile().isDirectory() && !output.getParentFile().mkdirs()) {
            throw new IllegalStateException("Can't create " + output.getParentFile());
        }
    }

    protected String emptyDefaultValue() {
        return "-";
    }

    private static class AbsolutePathResolver {

        // ensure it is aligned with org.talend.sdk.component.studio.model.parameter.SettingsCreator.computeTargetPath()
        // and org.talend.sdk.component.form.internal.converter.impl.widget.path.AbsolutePathResolver
        public String resolveProperty(final String propPath, final String paramRef) {
            return doResolveProperty(propPath, normalizeParamRef(paramRef));
        }

        private String normalizeParamRef(final String paramRef) {
            return (!paramRef.contains(".") ? "../" : "") + paramRef;
        }

        private String doResolveProperty(final String propPath, final String paramRef) {
            if (".".equals(paramRef)) {
                return propPath;
            }
            if (paramRef.startsWith("..")) {
                String current = propPath;
                String ref = paramRef;
                while (ref.startsWith("..")) {
                    int lastDot = current.lastIndexOf('.');
                    if (lastDot < 0) {
                        lastDot = 0;
                    }
                    current = current.substring(0, lastDot);
                    ref = ref.substring("..".length(), ref.length());
                    if (ref.startsWith("/")) {
                        ref = ref.substring(1);
                    }
                    if (current.isEmpty()) {
                        break;
                    }
                }
                return Stream.of(current, ref.replace('/', '.')).filter(it -> !it.isEmpty()).collect(joining("."));
            }
            if (paramRef.startsWith(".") || paramRef.startsWith("./")) {
                return propPath + '.' + paramRef.replaceFirst("\\./?", "").replace('/', '.');
            }
            return paramRef;
        }
    }

    @Data
    @AllArgsConstructor(access = PRIVATE)
    protected static class Condition {

        private final String target;

        private final String path;

        private final String value;

        private final boolean negate;

        private final String strategy;
    }

    @Data
    protected static class UIInfo {

        private final String section;

        private final Set<String> nestedLayout = new HashSet<>();

        public UIInfo(final String section, final Collection<String> layouts) {
            this.section = section;
            nestedLayout.addAll(layouts);
        }

        public void addNestedLayouts(final Collection<String> layouts) {
            nestedLayout.addAll(layouts);
        }
    }

    @Data
    @RequiredArgsConstructor(access = PRIVATE)
    protected static class Param implements Comparable<Param> {

        private final String displayName;

        private final String documentation;

        private final String defaultValue;

        private final String type;

        private final String fullPath;

        private final Conditions conditions;

        private final String section;

        private UIInfo uiInfo;

        private final SortedSet<Param> nested = new TreeSet<>();

        public void addNested(final Param p) {
            nested.add(p);
        }

        public int compareTo(final Param p) {
            return this.getDisplayName().compareTo(p.getDisplayName());
        }

        public boolean isComplex() {
            return this.nested.size() > 0;
        }

        public boolean isSection() {
            return isComplex() && section != null && !section.isEmpty();
        }

        public String getSectionName() {
            return "datastore".equals(section) ? "connection" : section;
        }

    }

    @Data
    @AllArgsConstructor(access = PRIVATE)
    public static class Conditions {

        private final String path;

        private final String operator;

        private final Collection<Condition> conditions;
    }

    @Data
    @AllArgsConstructor(access = PRIVATE)
    protected static class ComponentDescription {

        private final Class<?> type;

        private final String family;

        private final String name;

        private final String documentation;

        private final Collection<ParameterMeta> parameters;

        private final AbsolutePathResolver resolver;

        private final DefaultValueInspector defaultValueInspector;

        private final Locale locale;

        private final String emptyDefaultValue;

        private Map<String, UIInfo> getUIParamByPath(final Collection<ParameterMeta> parameters) {
            Map<String, UIInfo> uiparams = new HashMap<>();
            recurseUIParam(parameters, uiparams, "", null, null);

            return uiparams;
        }

        private Set<String> recurseUIParam(final Collection<ParameterMeta> params, final Map<String, UIInfo> uiparams,
                final String currentSectionName, final ParameterMeta parent, final Collection<String> parentLayouts) {
            Set<String> layouts = new HashSet<>();
            for (ParameterMeta param : params) {
                String path = param.getPath();
                String sectionType = param.getMetadata().get("tcomp::configurationtype::type");
                if (sectionType == null) {
                    sectionType = currentSectionName;
                }
                // String layout = getLayout(parent, layoutParent, param);
                Collection<String> paramLayouts = getPropertiesByLayout(parent, param.getName(), parentLayouts);
                layouts.addAll(paramLayouts);

                UIInfo uiInfo = new UIInfo(currentSectionName, paramLayouts);
                uiparams.put(path, uiInfo);
                if (param.getNestedParameters().size() > 0) {
                    Set<String> subLayouts =
                            recurseUIParam(param.getNestedParameters(), uiparams, sectionType, param, paramLayouts);
                    // uiInfo.addNestedLayouts(subLayouts);
                }
            }

            return layouts;
        }

        private Collection<String> getPropertiesByLayout(final ParameterMeta parent, final String param,
                final Collection<String> parentLayouts) {
            final Collection<String> layouts = new TreeSet<>();

            if (parent == null) {
                return Arrays.asList("tcomp::ui::gridlayout::Main::value", "tcomp::ui::gridlayout::Advanced::value");
            }

            Collection<String> definedLayouts = parent
                    .getMetadata()
                    .keySet()
                    .stream()
                    .filter(k -> k.startsWith("tcomp::ui::gridlayout::"))
                    .collect(Collectors.toList());
            if (definedLayouts.isEmpty()) {
                // If no layout defined, we take main if exists in parent
                if (parentLayouts.contains("tcomp::ui::gridlayout::Main::value")) {
                    return Arrays.asList("tcomp::ui::gridlayout::Main::value");
                }
            }

            definedLayouts
                    .stream()
                    .filter(l -> parentLayouts.contains(l)) // A property is visible in a layout only if its parent is
                                                            // also visible in this layout
                    .forEach(k -> {
                        String layoutConfig = parent.getMetadata().get(k);
                        if (layoutConfig != null) {
                            boolean isInThisLayout = Collections
                                    .list(new StringTokenizer(layoutConfig, "|"))
                                    .stream()
                                    .flatMap(p -> Collections.list(new StringTokenizer(p.toString(), ",")).stream())
                                    .collect(Collectors.toList())
                                    .contains(param);

                            if (isInThisLayout) {
                                layouts.add(k);
                            }
                        }
                    });

            return layouts;
        }

        Map<String, Map<String, List<Param>>> getParametersWithUInfo() {
            final Map<String, Map<String, List<Param>>> params = new HashMap<>();
            final Map<String, UIInfo> uiInfos = getUIParamByPath(parameters);

            for (Param p : toParams()) {
                setParametersWithUInfoRecurs(p, uiInfos, params);
            }

            return params;
        }

        void setParametersWithUInfoRecurs(final Param p, final Map<String, UIInfo> uiInfos,
                final Map<String, Map<String, List<Param>>> params) {
            UIInfo info = uiInfos.get(p.fullPath);
            p.setUiInfo(info);

            Map<String, List<Param>> section = params.get(info.getSection());
            if (section == null) {
                section = new HashMap<>();
                params.put(info.getSection(), section);
            }

            for (String l : info.getNestedLayout()) {
                List<Param> layout = section.get(l);
                if (layout == null) {
                    layout = new ArrayList<>();
                    section.put(l, layout);
                }

                layout.add(p);
            }

            for (Param n : p.getNested()) {
                setParametersWithUInfoRecurs(n, uiInfos, params);
            }
        }

        private Collection<Param> toParams() {
            Collection<Param> params = new ArrayList<>();
            for (ParameterMeta p : parameters) {
                Param tp = toParamWithNested(p, null, null, new HashMap<>());
                params.add(tp);
            }

            return params;
        }

        Stream<Param> parameters() {
            return mapParameters(parameters, null, null, new HashMap<>());
        }

        private Stream<Param> mapParameters(final Collection<ParameterMeta> parameterMetas,
                final DefaultValueInspector.Instance parentInstance, final ParameterBundle parentBundle,
                final Map<String, String> types) {
            return parameterMetas.stream().flatMap(p -> {
                final DefaultValueInspector.Instance instance = defaultValueInspector
                        .createDemoInstance(
                                ofNullable(parentInstance).map(DefaultValueInspector.Instance::getValue).orElse(null),
                                p);
                return Stream
                        .concat(Stream.of(toParam(p, instance, parentBundle, types)),
                                mapParameters(p.getNestedParameters(), instance, findBundle(p), types));
            });
        }

        private Param toParamWithNested(final ParameterMeta p, final DefaultValueInspector.Instance parentInstance,
                final ParameterBundle parent, final Map<String, String> types) {
            final ParameterBundle bundle = findBundle(p);
            final String type = findEnclosingConfigurationType(p, types);

            final DefaultValueInspector.Instance instance = defaultValueInspector
                    .createDemoInstance(
                            ofNullable(parentInstance).map(DefaultValueInspector.Instance::getValue).orElse(null), p);

            Param param = new Param(bundle.displayName(parent).orElse(p.getName()),
                    bundle.documentation(parent).orElseGet(() -> findDocumentation(p)),
                    ofNullable(findDefault(p, instance)).orElse(emptyDefaultValue), ofNullable(type).orElse("-"),
                    p.getPath(), createConditions(p.getPath(), p.getMetadata()),
                    p.getMetadata().get("tcomp::configurationtype::type"));

            for (ParameterMeta child : p.getNestedParameters()) {
                Param np = toParamWithNested(child, instance, bundle, types);
                param.addNested(np);
            }

            return param;
        }

        private Param toParam(final ParameterMeta p, final DefaultValueInspector.Instance instance,
                final ParameterBundle parent, final Map<String, String> types) {
            final ParameterBundle bundle = findBundle(p);
            final String type = findEnclosingConfigurationType(p, types);
            return new Param(bundle.displayName(parent).orElse(p.getName()),
                    bundle.documentation(parent).orElseGet(() -> findDocumentation(p)),
                    ofNullable(findDefault(p, instance)).orElse(emptyDefaultValue), ofNullable(type).orElse("-"),
                    p.getPath(), createConditions(p.getPath(), p.getMetadata()), "Not computed");
        }

        private ParameterBundle findBundle(final ParameterMeta p) {
            return p.findBundle(Thread.currentThread().getContextClassLoader(), locale);
        }

        private String findEnclosingConfigurationType(final ParameterMeta p, final Map<String, String> types) {
            String type = p.getMetadata().get("tcomp::configurationtype::type");
            if (type != null) {
                types.put(p.getPath(), type);
            } else { // try to find the closest parent
                String currentPath = p.getPath();
                while (type == null) {
                    final int sep = currentPath.lastIndexOf('.');
                    if (sep < 0) {
                        break;
                    }
                    currentPath = currentPath.substring(0, sep);
                    type = types.get(currentPath);
                }
            }
            return type;
        }

        private String findDefault(final ParameterMeta p, final DefaultValueInspector.Instance instance) {
            final String defVal = p.getMetadata().get("tcomp::ui::defaultvalue::value");
            if (defVal != null) { // todo: revise if we need to know if it is a ui default or an instance default
                return defVal;
            }

            if (instance == null || instance.getValue() == null || instance.isCreated()) {
                return null;
            }

            switch (p.getType()) {
            case NUMBER:
            case BOOLEAN:
            case STRING:
            case ENUM:
                return ofNullable(instance.getValue())
                        .map(String::valueOf)
                        .map(it -> it.isEmpty() ? "<empty>" : it)
                        .orElse(null);
            case ARRAY:
                return String
                        .valueOf(Collection.class.isInstance(instance.getValue())
                                ? Collection.class.cast(instance.getValue()).size()
                                : Array.getLength(instance.getValue()));
            case OBJECT:
            default:
                return null;
            }
        }

        private Conditions createConditions(final String path, final Map<String, String> metadata) {
            final String globalOperator = metadata.getOrDefault("tcomp::condition::ifs::operator", "AND");
            final Collection<Condition> conditionEntries =
                    metadata.keySet().stream().filter(it -> it.startsWith("tcomp::condition::if::target")).map(it -> {
                        final String target = metadata.get(it);
                        return new Condition(target, resolver.doResolveProperty(path, target),
                                metadata.get(it.replace("::target", "::value")),
                                Boolean.parseBoolean(metadata.get(it.replace("::target", "::negate"))),
                                metadata.get(it.replace("::target", "::evaluationStrategy")));
                    }).collect(toList());
            return new Conditions(path, globalOperator, conditionEntries);
        }

        private String findDocumentation(final ParameterMeta p) {
            final String inline = p.getMetadata().get("tcomp::documentation::value");
            if (inline != null) {
                if (inline.startsWith("resource:")) {
                    final InputStream stream = Thread
                            .currentThread()
                            .getContextClassLoader()
                            .getResourceAsStream(inline.substring("resource:".length()));
                    if (stream != null) {
                        try (final BufferedReader reader =
                                new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

                            return reader.lines().collect(joining("\n"));
                        } catch (final IOException e) {
                            throw new IllegalArgumentException("Bad resource: '" + inline + "'", e);
                        }
                    } else {
                        throw new IllegalArgumentException("No resource: '" + inline + "'");
                    }

                }
                return inline;
            }
            return p.getName() + " configuration";
        }
    }
}
