/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.tools.ComponentValidator.Configuration;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExceptionValidator implements Validator {

    private final Validators.ValidatorHelper helper;

    private final Configuration configuration;

    public ExceptionValidator(final ValidatorHelper helper, final Configuration configuration) {
        this.helper = helper;
        this.configuration = configuration;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final Stream<String> errorsException = helper
                .componentClassFiles()
                .flatMap(f -> streamClassesInDirectory(classLoader, null, f))
                .filter(ComponentException.class::isAssignableFrom)
                .map(e -> e.getName()
                        + " inherits from ComponentException, this will lead to ClassNotFound errors in some environments. Use instead ComponentException directly!");
        if (configuration.isFailOnValidateExceptions()) {
            return errorsException;
        } else {
            errorsException.forEach(e -> log.error(e));
        }

        return Stream.empty();
    }

    private Stream<Class> streamClassesInDirectory(final ClassLoader loader, final String pckg, final File classFile) {
        if (classFile.isDirectory()) {
            return Arrays
                    .stream(classFile.listFiles())
                    .flatMap(f -> streamClassesInDirectory(loader, nextPackage(pckg, classFile.getName()), f));
        }

        if (classFile.getName().endsWith(".class")) {
            final String className = classFile.getName().substring(0, classFile.getName().lastIndexOf("."));
            final String completeName = pckg + className;
            try {
                return Stream.of(loader.loadClass(completeName));
            } catch (final Exception e) {
                log.error("Could not load class : " + completeName + "=>" + e.getMessage());
            }
        }

        return Stream.empty();
    }

    private String nextPackage(final String current, final String next) {
        return current == null ? "" : current + next + ".";
    }
}
