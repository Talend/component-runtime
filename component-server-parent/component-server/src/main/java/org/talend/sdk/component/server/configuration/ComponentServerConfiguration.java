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

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ApplicationScoped
public class ComponentServerConfiguration {

    @Inject
    @Documentation("If set it will replace any message for exceptions. Set to `false` to use the actual exception message.")
    @ConfigProperty(name = "talend.component.server.jaxrs.exceptionhandler.defaultMessage", defaultValue = "false")
    private String defaultExceptionMessage;

    @Inject
    @Documentation("The local maven repository used to locate components and their dependencies")
    @ConfigProperty(name = "talend.component.server.maven.repository")
    private Optional<String> mavenRepository;

    // property to list plugins directly by gav. This is nice to set it on the cli but not as a maintenance solution.
    @Inject
    @Documentation("A comma separated list of gav to locate the components")
    @ConfigProperty(name = "talend.component.server.component.coordinates")
    private Optional<String> componentCoordinates;

    // property to list plugins like in a fatjar, ie value = gav. Nice for assemblies, less for demo/cli usage.
    @Inject
    @Documentation("A property file (or multiple comma separated) where the value is a gav of a component to register"
            + "(complementary with `coordinates`). Note that the path can end up with `*` or `*.properties` "
            + "to take into account all properties in a folder.")
    @ConfigProperty(name = "talend.component.server.component.registry")
    private Optional<List<String>> componentRegistry;

    @Inject
    @Documentation("Should the /documentation endpoint be activated. "
            + "Note that when called on localhost the doc is always available.")
    @ConfigProperty(name = "talend.component.server.documentation.active", defaultValue = "true")
    private Boolean supportsDocumentation;

    // sync with org.talend.sdk.component.server.service.security.SecurityExtension.addSecurityHandlers
    @Inject
    @Documentation("How to validate a connection. Accepted values: securityNoopHandler.")
    @ConfigProperty(name = "talend.component.server.security.connection.handler", defaultValue = "securityNoopHandler")
    private String securityConnectionHandler;

    // sync with org.talend.sdk.component.server.service.security.SecurityExtension.addSecurityHandlers
    @Inject
    @Documentation("How to validate a command/request. Accepted values: securityNoopHandler.")
    @ConfigProperty(name = "talend.component.server.security.command.handler", defaultValue = "securityNoopHandler")
    private String securityCommandHandler;

    @Inject
    @Documentation("Should the component extensions add required dependencies.")
    @ConfigProperty(name = "talend.component.server.component.extend.dependencies", defaultValue = "true")
    private Boolean addExtensionDependencies;

    @Inject
    @Documentation("A component translation repository. This is where you put your documentation translations. "
            + "Their name must follow the pattern `documentation_${container-id}_language.adoc` where `${container-id}` "
            + "is the component jar name (without the extension and version, generally the artifactId).")
    @ConfigProperty(name = "talend.component.server.component.documentation.translations",
            defaultValue = "${home}/documentations")
    private String documentationI18nTranslations;

    @Inject
    @Documentation("Should the /api/v1/environment endpoint be activated. "
            + "It shows some internal versions and git commit which are not always desirable over the wire.")
    @ConfigProperty(name = "talend.component.server.environment.active", defaultValue = "true")
    private Boolean supportsEnvironment;

    @Inject
    @Documentation("A folder available for the server - don't forget to mount it in docker if you are using the "
            + "image - which accepts subfolders named as component plugin id "
            + "(generally the artifactId or jar name without the version, ex: jdbc). Each family folder can contain:\n\n"
            + "- a `user-configuration.properties` file which will be merged with component configuration system "
            + "(see services). This properties file enables the function `userJar(xxxx)` to replace the jar named `xxxx` "
            + "by its virtual gav (`groupId:artifactId:version`),\n"
            + "- a list of jars which will be merged with component family classpath\n")
    @ConfigProperty(name = "talend.component.server.user.extensions.location")
    private Optional<String> userExtensions;

    @Inject
    @Documentation("Should the implicit artifacts be provisionned to a m2. If set to `auto` it tries to detect "
            + "if there is a m2 to provision - recommended, if set to `skip` it is ignored, else it uses the value as a "
            + "m2 path.")
    @ConfigProperty(name = "talend.component.server.user.extensions.provisioning.location", defaultValue = "auto")
    private String userExtensionsAutoM2Provisioning;

    @Inject
    @Documentation("Timeout for extension initialization at startup, since it ensures the startup wait extensions "
            + "are ready and loaded it allows to control the latency it implies.")
    @ConfigProperty(name = "talend.component.server.component.extension.startup.timeout", defaultValue = "180000")
    private Long extensionsStartupTimeout;

    @Inject
    @Documentation("If you deploy some extension, where they can create their dependencies if needed.")
    @ConfigProperty(name = "talend.component.server.component.extension.maven.repository")
    private Optional<String> extensionMavenRepository;

    @Inject
    @Documentation("Should the components using a `@GridLayout` support tab translation. "
            + "Studio does not suppot that feature yet so this is not enabled by default.")
    @ConfigProperty(name = "talend.component.server.gridlayout.translation.support", defaultValue = "false")
    private Boolean translateGridLayoutTabNames;

    @Inject
    @Documentation("Should the all requests/responses be logged (debug purposes - only work when running with CXF).")
    @ConfigProperty(name = "talend.component.server.request.log", defaultValue = "false")
    private Boolean logRequests;

    @Inject
    @Documentation("Maximum items a cache can store, used for index endpoints.")
    @ConfigProperty(name = "talend.component.server.cache.maxSize", defaultValue = "1000")
    private Integer maxCacheSize;

    @Inject
    @Documentation("Should the lastUpdated timestamp value of `/environment` "
            + "endpoint be updated with server start time.")
    @ConfigProperty(name = "talend.component.server.lastUpdated.useStartTime", defaultValue = "false")
    private Boolean changeLastUpdatedAtStartup;

    @Inject
    @Documentation("These patterns are used to find the icons in the classpath(s).")
    @ConfigProperty(name = "talend.component.server.icon.paths",
            defaultValue = "icons/%s.svg,icons/svg/%s.svg,icons/%s_icon32.png,icons/png/%s_icon32.png")
    private List<String> iconExtensions;

    @Inject
    @Documentation("For caching reasons the goal is to reduce the locales to the minimum required numbers. "
            + "For instance we avoid `fr` and `fr_FR` which would lead to the same entries but x2 in terms of memory. "
            + "This mapping enables that by whitelisting allowed locales, default being `en`. "
            + "If the key ends with `*` it means all string starting with the prefix will match. "
            + "For instance `fr*` will match `fr_FR` but also `fr_CA`.")
    @ConfigProperty(name = "talend.component.server.locale.mapping",
            defaultValue = "en*=en\nfr*=fr\nzh*=zh_CN\nja*=ja\nde*=de")
    private String localeMapping;

    @Inject
    @Documentation("Should the plugins be un-deployed and re-deployed.")
    @ConfigProperty(name = "talend.component.server.plugins.reloading.active", defaultValue = "false")
    private Boolean pluginsReloadActive;

    @Inject
    @Documentation("Re-deploy method on a `timestamp` or `connectors` version change. By default, the timestamp is"
            + " checked on the file pointed by `talend.component.server.component.registry` or "
            + "`talend.component.server.plugins.reloading.marker` variable, otherwise we inspect the content of the "
            + "`CONNECTORS_VERSION` file. Accepted values: `timestamp`, anything else defaults to `connectors`.")
    @ConfigProperty(name = "talend.component.server.plugins.reloading.method", defaultValue = "timestamp")
    private Boolean pluginsReloadMethod;

    @Inject
    @Documentation("Interval in seconds between each check if plugins re-loading is enabled.")
    @ConfigProperty(name = "talend.component.server.plugins.reloading.interval", defaultValue = "600")
    private Long pluginsReloadInterval;

    @Inject
    @Documentation("Specify a file to check its timestamp on the filesystem. This file will take precedence of the default "
            + "ones provided by the `talend.component.server.component.registry` property (used for timestamp method).")
    @ConfigProperty(name = "talend.component.server.plugins.reloading.marker")
    private Optional<String> pluginsReloadFileMarker;

    @PostConstruct
    private void init() {
        if (logRequests != null && logRequests) {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try {
                doActivateDebugMode(loader, loader.loadClass("org.apache.cxf.ext.logging.LoggingFeature"));
            } catch (final Exception | NoClassDefFoundError e) {
                try {
                    doActivateDebugMode(loader, loader.loadClass("org.apache.cxf.feature.LoggingFeature"));
                } catch (final Exception | NoClassDefFoundError ex) {
                    log.warn("Can't honor log request configuration, skipping ({})", e.getMessage());
                }
            }
        }
    }

    private void doActivateDebugMode(final ClassLoader loader, final Class<?> feature)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
            java.lang.reflect.InvocationTargetException, NoSuchMethodException {
        final Class<?> bus = loader.loadClass("org.apache.cxf.Bus");
        final Object instance = feature.getConstructor().newInstance();
        final Object busInstance = CDI.current().select(bus).get();
        feature.getMethod("initialize", bus).invoke(instance, busInstance);
        log.info("Activated debug mode - will log requests/responses");
    }
}
