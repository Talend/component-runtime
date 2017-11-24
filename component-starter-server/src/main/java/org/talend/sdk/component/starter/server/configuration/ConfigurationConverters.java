/*
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *   <p>
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   <p>
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   <p>
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.talend.sdk.component.starter.server.configuration;

import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;

import org.apache.deltaspike.core.api.config.ConfigResolver;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ConfigurationConverters {

    @ApplicationScoped
    public static class SetConverter implements ConfigResolver.Converter<Set<String>> {

        @Override
        public Set<String> convert(final String value) {
            if (value == null) {
                return emptySet();
            }
            return Stream.of(value.split(";")).map(String::trim).filter(s -> !s.isEmpty()).collect(toSet());
        }
    }

}
