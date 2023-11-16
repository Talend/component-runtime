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
package org.talend.sdk.component.server.configuration;

import java.util.EnumSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.talend.sdk.component.server.mdc.MdcRequestBinder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MdcInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(final Set<Class<?>> c, final ServletContext ctx) throws ServletException {
        try {
            ctx.getClassLoader().loadClass("org.apache.logging.log4j.web.ServletRequestThreadContext");
        } catch (final ClassNotFoundException e) {
            log.debug("log4j-web not available, skipping MDC setup");
            return;
        }
        final FilterRegistration.Dynamic filter = ctx.addFilter("mdc-request-binder", MdcRequestBinder.class);
        filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
        filter.setAsyncSupported(true);
    }
}
