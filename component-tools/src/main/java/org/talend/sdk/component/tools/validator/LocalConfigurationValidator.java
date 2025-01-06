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
package org.talend.sdk.component.tools.validator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.configuration.ui.DefaultValue;
import org.talend.sdk.component.tools.ComponentValidator.Configuration;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

public class LocalConfigurationValidator implements Validator {

    private final Validators.ValidatorHelper helper;

    private final Configuration configuration;

    public LocalConfigurationValidator(final ValidatorHelper helper, final Configuration configuration) {
        this.helper = helper;
        this.configuration = configuration;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final String pluginId = this.pluginId();
        Stream<String> errStart = helper
                .componentClassFiles() //
                .map(root -> new File(root, "TALEND-INF/local-configuration.properties")) //
                .filter(File::exists) //
                .flatMap(props -> {
                    final Properties properties = new Properties();
                    try (final InputStream stream = new BufferedInputStream(new FileInputStream(props))) {
                        properties.load(stream);
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                    return properties
                            .stringPropertyNames()
                            .stream()
                            .filter(it -> !it.toLowerCase(Locale.ROOT).startsWith(pluginId + "."))
                            .map(it -> "'" + it + "' does not start with '" + pluginId + "', "
                                    + "it is recommended to prefix all keys by the family");
                });

        // then check the @DefaultValue annotation
        Stream<String> familyName = Stream
                .concat(finder.findAnnotatedFields(DefaultValue.class).stream(),
                        finder.findAnnotatedConstructorParameters(DefaultValue.class).stream())
                .map(d -> {
                    final DefaultValue annotation = d.getAnnotation(DefaultValue.class);
                    if (annotation.value().startsWith("local_configuration:") && !annotation
                            .value()
                            .toLowerCase(Locale.ROOT)
                            .startsWith("local_configuration:" + pluginId + ".")) {
                        return d + " does not start with family name (followed by a dot): '" + pluginId + "'";
                    }
                    return null;
                })
                .filter(Objects::nonNull);

        return Stream.concat(errStart, familyName);
    }

    private String pluginId() {
        return Optional.of(configuration).map(Configuration::getPluginId).orElseGet(this::guessPluginId);
    }

    private String guessPluginId() { // assume folder name == module id
        return this.helper.componentClassFiles().map(f -> {
            if (!f.isDirectory()) {
                return null;
            }
            File current = f;
            int iteration = 5;
            while (iteration-- > 0 && current != null) {
                final File currentRef = current;
                if (Stream
                        .of("classes", "target", "main", "java", "build")
                        .anyMatch(it -> it.equals(currentRef.getName()))) {
                    current = current.getParentFile();
                } else {
                    return current.getName();
                }
            }
            return null;
        }) //
                .filter(Objects::nonNull)
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException("No pluginId set"));
    }
}
