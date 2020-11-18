/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.runtime.manager.component.AbstractMigrationHandler;

public class MigrationValidator implements Validator {

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        return finder.findAnnotatedClasses(Version.class).stream().map(c -> {
            Class<?> migration = c.getAnnotation(Version.class).migrationHandler();
            if (migration == MigrationHandler.class || AbstractMigrationHandler.class.isAssignableFrom(migration)) {
                return null;
            }
            return String
                    .format("Migration %s should inherit from %s.", c.getName(),
                            AbstractMigrationHandler.class.getCanonicalName());
        }).filter(Objects::nonNull).sorted();
    }
}
