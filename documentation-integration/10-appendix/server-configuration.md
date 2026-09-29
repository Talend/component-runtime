# Appendix: component server configuration keys

- **Framework version documented**: `1.2611.0-SNAPSHOT`.
- **Sources of truth**: `component-server/.../configuration/ComponentServerConfiguration.java` (annotated with `@ConfigProperty` + `@Documentation`), `.../tomcat/GenerateCertificateAndActivateHttps.java`, `.../tomcat/TomcatSetup.java`, `.../configuration/PropertiesSetup.java`, `.../configuration/ComponentConfigurationLoader.java`, `.../service/IconResolver.java`, `.../service/jcache/FrontCacheResolver.java`, `vault-client/**` (`@ConfigProperty`), `component-server/src/main/resources/meecrowave.properties`. Prose: `documentation/.../pages/ref-server-configuration.adoc` and its generated partial `_partials/generated_server-configuration.adoc` (the partial matches `ComponentServerConfiguration` 1:1: 27 keys).
- Related: [03-component-server-api.md](../03-component-server-api.md), [error-codes.md](error-codes.md).

## 1. How configuration is read

- MicroProfile Config (Apache Geronimo implementation). A key can be given as **system property** (`-Dtalend.component.server.cache.maxSize=500`, typically through `_JAVA_OPTIONS` in Docker) or **environment variable** (dots -> underscores, upper case, e.g. `TALEND_COMPONENT_SERVER_CACHE_MAXSIZE`) (rule stated in `_partials/configuration-mecanism.adoc`; not re-verified in code).
- Extra source `ComponentConfigurationLoader` (ordinal 1000, name `component-configuration`): a properties file given by the CLI option `--component-configuration=<file-or-classpath-resource>`, merged with the Meecrowave builder properties.
- `PropertiesSetup` (Meecrowave customizer) sets defaults **only if the system property is unset**: `jdk.serialFilter` (from env `TALEND_JDK_SERIAL_FILTER`), `java.io.tmpdir` (env `JAVA_IO_TMPDIR`), `java.security.egd=file:/dev/./urandom`, `java.util.logging.manager=org.apache.logging.log4j.jul.LogManager`, `log4j.configurationFile` (env `LOG4J_CONFIGURATIONFILE`), `http` (env `BOUND_PORT`), `meecrowave-properties` (env `MEECROWAVE-PROPERTIES`), `meecrowave.home`/`meecrowave.base` (env `MEECROWAVE_HOME`/`MEECROWAVE_BASE`), `geronimo.metrics.sigar.refreshInterval=0`, `talend.component.exit-on-destroy=true`, `talend.component.manager.services.cache.eviction.defaultEvictionTimeout=30000`, `...defaultMaxSize=5000`, `...maxDeletionPerEvictionRun=-1`. The HTTP port precedence is: `-Dhttp=x` (or CLI `--http=x`) > env `TALEND_COMPONENT_SERVER_PORT`. If both env `LOGGING_LAYOUT` and `TALEND_APP_HOME` are set the log4j2 config `${TALEND_APP_HOME}/conf/log4j2-component-server-${LOGGING_LAYOUT}.xml` is loaded.
- `meecrowave.properties` (packaged): gzip on (`connector.attributes.compression = on`), scanning exclusions, `watcher-bouncing = -1`.

## 2. `talend.component.server.*` (ComponentServerConfiguration, exhaustive)

| Key | Type | Default | Meaning |
|---|---|---|---|
| `talend.component.server.jaxrs.exceptionhandler.defaultMessage` | String | `false` | If not `false`, replaces the description of every unexpected exception in `ErrorPayload`. |
| `talend.component.server.maven.repository` | String (optional) | none (machine default m2) | Local Maven repository used to locate components and dependencies; also the root for `GET /component/dependency/{gav}` resolution. |
| `talend.component.server.component.coordinates` | String (optional) | none | Comma separated GAVs of components to register at startup. |
| `talend.component.server.component.registry` | List<String> (optional) | none | Properties file(s) (comma separated) whose values are component GAVs; path may end with `*` or `*.properties` to take a whole folder. Also the file whose timestamp drives plugin reloading. |
| `talend.component.server.documentation.active` | Boolean | `true` | Activates the static `/documentation` web resource (OpenAPI UI, `META-INF/resources/documentation/openapi.html`). Filter `DocumentationToggle` answers 404 when false. Does **not** affect `GET /api/v1/documentation/component/{id}` (inferred from `@WebFilter(urlPatterns = {"/documentation", "/documentation/*"})`); the property text says localhost is always allowed but the filter code does not implement that (discrepancy). |
| `talend.component.server.security.connection.handler` | String | `securityNoopHandler` | Name (`@Named`) of the CDI bean whose `@Observes OnConnection` method validates a connection (pre-matching filter). Startup fails with `No handler '<name>'` if unknown. |
| `talend.component.server.security.command.handler` | String | `securityNoopHandler` | Same for `OnCommand` (per resource method invocation). |
| `talend.component.server.component.extend.dependencies` | Boolean | `true` | Whether `GET /component/dependencies` appends the extra dependencies of the owning `ComponentExtension`. |
| `talend.component.server.component.documentation.translations` | String | `${home}/documentations` | Folder of translated docs named `documentation_${container-id}_${language}.adoc` (`${home}` = system property `meecrowave.home`). |
| `talend.component.server.environment.active` | Boolean | `true` | If false `GET /api/v1/environment` returns 404 (`EnvironmentFilter`). |
| `talend.component.server.user.extensions.location` | String (optional) | none | Folder with one sub-folder per plugin id containing `user-configuration.properties` (enables `userJar(xxxx)`) and extra jars merged into the family classpath. |
| `talend.component.server.user.extensions.provisioning.location` | String | `auto` | `auto` (detect m2), `skip`, or an m2 path where implicit artifacts are provisioned. |
| `talend.component.server.component.extension.startup.timeout` | Long (ms) | `180000` | Max wait for server extensions initialization at startup. |
| `talend.component.server.component.extension.maven.repository` | String (optional) | none | Where extensions create their dependencies. |
| `talend.component.server.gridlayout.translation.support` | Boolean | `false` | Translate `@GridLayout` tab names in `ui::gridlayout::<Tab>::value` keys (off by default because Studio does not support it). |
| `talend.component.server.request.log` | Boolean | `false` | Log requests/responses (CXF only). |
| `talend.component.server.cache.maxSize` | Integer | `1000` | Max entries of the in-memory index caches (`ComponentResourceImpl`, `ConfigurationTypeResourceImpl`; eviction when size reaches `max - 1`). |
| `talend.component.server.lastUpdated.useStartTime` | Boolean | `false` | If true `Environment.lastUpdated` is `max(server start, last plugin update)`. |
| `talend.component.server.icon.paths` | List<String> | `icons/%s.svg,icons/svg/%s.svg,icons/%s_icon32.png,icons/png/%s_icon32.png` | Classpath patterns for icons (`%s` = icon key, prefixed by the theme folder when themes are on). SVG patterns are dropped if SVG is not supported. |
| `talend.component.server.icon.theme.support` | Boolean | `true` | Enable themed icons (`icons/<theme>/...`). |
| `talend.component.server.icon.theme.legacy` | Boolean | `true` | Fallback to non-themed icons if the themed one is missing. |
| `talend.component.server.icon.theme.default` | String | `light` | Theme used when `theme` query param is absent. |
| `talend.component.server.locale.mapping` | String (properties syntax, newline separated) | `en*=en`, `fr*=fr`, `zh*=zh_CN`, `ja*=ja`, `de*=de` | Whitelist mapping of requested languages to served locales (trailing `*` = prefix match). Unmatched -> `en`. |
| `talend.component.server.plugins.reloading.active` | Boolean | `false` | Periodically check and redeploy plugins. |
| `talend.component.server.plugins.reloading.method` | String | `timestamp` | `timestamp` (file timestamp of registry/marker) or anything else = `connectors` (content of `CONNECTORS_VERSION` in the m2 root). |
| `talend.component.server.plugins.reloading.interval` | Long (s) | `600` | Check period. |
| `talend.component.server.plugins.reloading.marker` | String (optional) | none | File whose timestamp takes precedence over the registry file. |

## 3. Other keys read by the server code

| Key | Read in | Default | Meaning |
|---|---|---|---|
| `talend.component.server.ssl.active` | `GenerateCertificateAndActivateHttps` | `false` | Auto HTTPS: generate a keystore (via `keytool -genkey`) and enable TLS. |
| `talend.component.server.ssl.password` | same | `changeit` | Keystore/key password. |
| `talend.component.server.ssl.keystore.location` | same | `${meecrowave.base:.}/conf/ssl.p12` | Keystore file. |
| `talend.component.server.ssl.keystore.alias` | same | `talend` | Key alias. |
| `talend.component.server.ssl.keystore.type` | same | `PKCS12` | Keystore type. |
| `talend.component.server.ssl.keystore.generation.force` | same | `false` | Regenerate even if the file exists. |
| `talend.component.server.ssl.keystore.generation.command` | same | none | Custom generation command instead of `keytool`. |
| `talend.component.server.ssl.keypair.algorithm` | same | `RSA` | |
| `talend.component.server.ssl.certificate.dname` | same | `CN=Talend,OU=www.talend.com,O=component-server,C=<hostname>` | |
| `talend.component.server.ssl.keypair.size` | same | `2048` | |
| `talend.component.server.ssl.port` | same | HTTP port | HTTPS port. When active, the HTTP connector is skipped (`setSkipHttp(true)`). |
| `talend.component.server.tomcat.valve.error.debug` (system property) | `TomcatSetup` | `false` | When false Tomcat's error report valve shows neither report nor server info. |
| `talend.component.exit-on-destroy` (system property) | `TomcatSetup` | `true` (set by `PropertiesSetup`) | `System.exit(0)` after server destroy. |
| `talend.component.server.icon.svg.support` (system property) | `IconResolver` | see note | Only consulted if `talend.studio.version` is set (Studio embedding): then SVG icons are enabled iff this property is true; otherwise SVG is always enabled. |
| `talend.studio.version`, `osgi.bundles` (system properties) | `IconResolver`, `VirtualDependenciesService` | none | Presence switches the server into Studio-embedded behaviours. |
| `git.build.version`, `git.commit.id`, `git.build.time` | `EnvironmentResourceImpl` | build-generated (`git.properties`) | Values of `Environment.version/commit/time`. |
| `talend.vault.cache.jcache.refresh.period` | `FrontCacheResolver` | `30000` (ms) | Period of the thread that clears the front caches when `Environment.lastUpdated` is newer than the last clean-up. |
| virtual group id `virtual.talend.component.server.generated.` (constant, not a key) | `VirtualDependenciesService` | | Prefix of synthetic GAVs for user-configuration jars. |

## 4. Vault client / cache keys (`vault-client`, used by the server to decipher `vault:` values)

| Key | Default | Meaning |
|---|---|---|
| `talend.vault.cache.vault.url` | `no-vault` | Base URL of Vault. |
| `talend.vault.cache.vault.auth.endpoint` | `v1/auth/engines/login` | Token retrieval path. |
| `talend.vault.cache.vault.decrypt.endpoint` | `v1/tenants-keyrings/decrypt/{x-talend-tenant-id}` | Decrypt path; `{x-talend-tenant-id}` is replaced by the request header `x-talend-tenant-id`. |
| `talend.vault.cache.vault.auth.token` | `-` (ignored) | Static token (makes role/secret ignored). |
| `talend.vault.cache.vault.auth.roleId` / `.secretId` | `-` | AppRole login. |
| `talend.vault.cache.service.auth.refreshDelayMargin` | `600000` | Token refresh margin (ms). |
| `talend.vault.cache.service.auth.refreshDelayOnFailure` | `1000` | Retry delay (ms). |
| `talend.vault.cache.service.auth.numberOfRetryOnFailure` | `3` | Retries on recoverable failures. |
| `talend.vault.cache.service.auth.cantDecipherStatusCode` | `422` | HTTP status when values cannot be deciphered. |
| `talend.vault.cache.service.decipher.skip.regex` | `vault\:v[0-9]+\:.*` | Whitelist regex of ciphered values; others pass through. |
| `talend.vault.cache.jcache.manager.uri` / `.manager.properties` | `geronimo://simple-jcache.properties` / empty | JCache manager. |
| `talend.vault.cache.jcache.cache.management` / `.statistics` | `true` / `true` | JCache MBeans / statistics. |
| `talend.vault.cache.jcache.cache.expiry` | `3600` | Expiry of decrypted values (documented as ms). |
| `talend.vault.cache.jcache.maxCacheSize` | `100000` | Max size per cache. |
| `talend.vault.cache.client.timeout.connect` / `.read` | `30000` / `30000` | HTTP timeouts to Vault (ms). |
| `talend.vault.cache.client.providers` | none | JAX-RS providers FQNs for the clients. |
| `talend.vault.cache.client.certificate.acceptAny` | `false` | Accept any certificate (dev only). |
| `talend.vault.cache.client.vault.certificate.keystore.location` / `.type` / `.password` | none / none / `changeit` | Client keystore for Vault. |
| `talend.vault.cache.client.vault.certificate.truststore.type` | none | Truststore type. |
| `talend.vault.cache.client.vault.hostname.accepted` | `localhost,127.0.0.1,0:0:0:0:0:0:0:1` | Accepted hostnames. |
| `talend.vault.cache.client.executor.vault.max` / `.core` / `.keepAlive` | `256` / `64` / `60000` | Thread pool of the Vault client. |

## 5. Component-manager (runtime library) properties visible in a server deployment

Read by `ComponentManager` and helpers (`component-runtime-manager`); they apply to the server because it embeds a `ComponentManager`, and to any runtime host that embeds one. Semantics below are read from the property names and surrounding code only where stated; otherwise **(unverified)**.

| Key | Meaning |
|---|---|
| `talend.checkpoint.enabled` | Default `false`. When `true`, `ComponentManager.mergeCheckpointConfiguration` rewrites `$checkpoint*` keys of a *mapper* configuration to the path of the `@Checkpoint` option (see 03 file, flat-properties section). |
| `talend.component.configuration.validation.skip` | Skips option validation in `ReflectionService` (unverified default). |
| `talend.component.configuration.` (prefix) | `DefaultServiceProvider`: component-scoped `LocalConfiguration` prefix (unverified). |
| `talend.component.manager.classloader.container.classesAndPackages`, `...container.parentResources` | Classloader isolation whitelists (unverified). |
| `talend.component.manager.plugins.parallel` | Parallel plugin deployment (unverified). |
| `talend.component.manager.classpathcontributor.skip`, `talend.component.manager.jmx.skip`, `talend.component.manager.log.info`, `talend.component.manager.localconfiguration.skip` | Feature toggles (unverified). |
| `component.manager.callers.skip`, `component.manager.classpath.skip` | Skip auto-discovery of caller / classpath plugins (`autoDiscoverPlugins0`, verified in code). |
| `talend.component.manager.services.cache.eviction.*` | `LocalCache` eviction (`defaultEvictionTimeout`, `defaultMaxSize`, `maxDeletionPerEvictionRun`; see defaults in section 1). |
| `talend.component.manager.m2.repository`, `talend.component.manager.m2.settings`, `talend.component.manager.user.m2.fallback` | Maven repository resolution (unverified). |

## 6. Ports, TLS, Docker and logging (from `documentation-rest.adoc`)

- Default listener is the Meecrowave HTTP connector; port via `--http=<port>` / `-Dhttp=<port>` / env `TALEND_COMPONENT_SERVER_PORT` / env `BOUND_PORT`. Manual HTTPS example from the documentation: `-Dskip-http=true -Dssl=true -Dhttps=8443 -Dkeystore-type=PKCS12 -Dkeystore-alias=talend -Dkeystore-password=talend -Dkeystore-file=/opt/certificates/component.p12` (Meecrowave options, not re-verified in code).
- Docker image (`tacokit/component-server`): options through `_JAVA_OPTIONS`; components mounted through `-Dtalend.component.server.component.registry=...` and `-Dtalend.component.server.maven.repository=...`; `LOGGING_LAYOUT` = `TEXT` (default) / `JSON` / `KAFKA` (`LOG_KAFKA_TOPIC`, `LOG_KAFKA_URL`); `CONSOLE_LOG_LEVEL`. Custom extension jars in `/opt/talend/component-kit/custom/`. Health check used in the compose examples: `curl --fail http://localhost:<port>/api/v1/environment` (this requires `environment.active=true`).
- WebSocket transport: base `/websocket/v1` instead of `/api/v1`; multiplexed endpoint `/websocket/v1/bus` with header `destinationMethod` (documentation; implementation in `WebSocketBroadcastSetup`, not analysed in depth).

## 7. Discrepancies

| Item | Detail |
|---|---|
| `documentation.active` "on localhost the doc is always available" | Not implemented in `DocumentationToggle` (unconditional 404 when off). |
| `talend.vault.cache.jcache.cache.expiry` | Documented "(ms)" but default `3600` looks like seconds (unverified). |
| `ref-server-configuration.adoc` | Only includes the generated partial and `configuration-mecanism.adoc`; SSL, vault and runtime-manager keys above are absent from the published docs. |
