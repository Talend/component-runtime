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
package org.talend.sdk.component.proxy.cxf;

import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.apache.cxf.Bus;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduitFactory;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.talend.sdk.component.proxy.config.ProxyConfiguration;

public class ClientConfigurationCustomization implements Extension {

    void customizeBus(@Observes final AfterDeploymentValidation afterDeploymentValidation,
            final BeanManager beanManager) {
        final Bus bus = Bus.class.cast(beanManager.getReference(beanManager.resolve(beanManager.getBeans(Bus.class)),
                Bus.class, beanManager.createCreationalContext(null)));
        final ProxyConfiguration configuration = ProxyConfiguration.class
                .cast(beanManager.getReference(beanManager.resolve(beanManager.getBeans(ProxyConfiguration.class)),
                        ProxyConfiguration.class, beanManager.createCreationalContext(null)));
        final Config configSource = ConfigProvider.getConfig();
        Stream
                .of(AsyncHTTPConduitFactory.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
                .map(f -> {
                    try {
                        if (!f.isAccessible()) {
                            f.setAccessible(true);
                        }
                        return f.get(null);
                    } catch (final IllegalAccessException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(key -> key.startsWith("org.apache.cxf.transport.http.async"))
                .forEach(key -> configSource.getOptionalValue(configuration.getPrefix() + key, String.class).ifPresent(
                        val -> bus.setProperty(key, val)));
    }
}
