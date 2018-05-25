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
package org.talend.sdk.component.proxy.config;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import lombok.Getter;

@Getter
@ApplicationScoped
public class ProxyConfiguration {

    private static final String PREFIX = "talend.component.proxy.";

    public static final String ORIGIN = "tacokit";

    @Inject
    @ConfigProperty(name = PREFIX + "targetServerBase")
    private String targetServerBase;

    @Inject
    @ConfigProperty(name = PREFIX + "client.providers")
    private Collection<Class> clientProviders;

}
