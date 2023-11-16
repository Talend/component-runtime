/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.service;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ValidationParameterEnricher;
import org.talend.sdk.component.server.front.model.PropertyValidation;

@ApplicationScoped
public class PropertyValidationService {

    private Function<Map<String, String>, PropertyValidation> propertyValidationCreator;

    @PostConstruct
    private void initMapper() {
        // precompute the mapping of validations to centralize the convention - note: can be moved to impl for setters
        // part
        final Collection<BiFunction<Object, Map<String, String>, Boolean>> validationSetters =
                Stream.of(PropertyValidation.class.getDeclaredFields()).map(f -> {
                    // we need boolean, int, string, collection<string>
                    final Function<String, Object> valueConverter;
                    if (Integer.class == f.getType()) {
                        valueConverter = v -> Double.valueOf(v).intValue();
                    } else if (Boolean.class == f.getType()) {
                        valueConverter = Boolean::parseBoolean;
                    } else if (Collection.class == f.getType()) {
                        valueConverter = s -> Stream.of(s.split(",")).collect(toList());
                    } else {
                        valueConverter = s -> s;
                    }
                    if (!f.isAccessible()) {
                        f.setAccessible(true);
                    }
                    return (BiFunction<Object, Map<String, String>, Boolean>) (instance,
                            meta) -> ofNullable(meta.get(ValidationParameterEnricher.META_PREFIX + f.getName()))
                                    .map(valueConverter)
                                    .map(val -> {
                                        try {
                                            f.set(instance, val);
                                        } catch (IllegalAccessException e) {
                                            throw new IllegalStateException(e);
                                        }
                                        return true;
                                    })
                                    .orElse(false);
                }).collect(toList());
        propertyValidationCreator = config -> {
            final PropertyValidation validation = new PropertyValidation();
            if (validationSetters.stream().filter(s -> s.apply(validation, config)).count() == 0) {
                return null;
            }
            return validation;
        };
    }

    public PropertyValidation map(final Map<String, String> meta) {
        return propertyValidationCreator.apply(meta);
    }
}
