# SRV - Component Server capabilities

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`; last release tag `1.2610.0`).
> Category prefix: `SRV`. Machine-readable twin: [index.SRV.json](index.SRV.json) (generated from this file, both MUST agree).
> Authoritative payload tables, JSON examples and per-endpoint parameters: [../03-component-server-api.md](../03-component-server-api.md). This file only summarizes each server capability and its integration contract; it deliberately does not duplicate payload field tables.
> Related: [01-overview-and-architecture.md](../01-overview-and-architecture.md), [SVC-services.md](SVC-services.md).

Sources of truth: JAX-RS interfaces in `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/`, payload POJOs in `component-server-parent/component-server-model/src/main/java/org/talend/sdk/component/server/front/model/`, behaviour in `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/`. Antora pages: `documentation-rest.adoc`, `ref-rest-resources.adoc`, `ref-server-configuration.adoc`, `rest-openapi.adoc`, `studio.adoc`.

Role of the server: the **Designer talks to the Component Server over HTTP** (design time). The **Runtime normally does not** call the server; it loads plugins directly with `ComponentManager` (see [01 overview](../01-overview-and-architecture.md)). The server is a Meecrowave/Tomcat + CXF (JAX-RS, JCache, CDI) application; it embeds its own `ComponentManager` and executes actions inside plugin classloaders. All entries below except SRV-017..SRV-023, SRV-025 concern the HTTP contract.

## EP label to SRV ID mapping

The payload document [../03-component-server-api.md](../03-component-server-api.md) labels each endpoint `EP-*`. This table is the authoritative mapping from those local labels to the catalog entries of this file (every `EP-*` label maps to exactly one `SRV-*`; one `SRV-*` may cover several labels).

| SRV ID | EP label(s) in 03 | Operation / topic |
|---|---|---|
| [SRV-001](#srv-001-base-path-and-jax-rs-application) | (base path) | `/api/v1` application root |
| [SRV-002](#srv-002-get-apiv1componentindex) | EP-COMPONENT-INDEX | `GET /component/index` |
| [SRV-003](#srv-003-get-apiv1componentdetails) | EP-COMPONENT-DETAILS | `GET /component/details` |
| [SRV-004](#srv-004-post-apiv1componentmigrateidconfigurationversion) | EP-COMPONENT-MIGRATE | `POST /component/migrate/{id}/{configurationVersion}` |
| [SRV-005](#srv-005-icon-endpoints) | EP-ICON-FAMILY, EP-ICON-COMPONENT, EP-ICON-CUSTOM, EP-ICON-INDEX | `GET /component/icon/family/{id}`, `GET /component/icon/{id}`, `GET /component/icon/custom/{familyId}/{iconKey}`, `GET /component/icon/index` |
| [SRV-006](#srv-006-get-apiv1componentdependencies) | EP-COMPONENT-DEPS | `GET /component/dependencies` |
| [SRV-007](#srv-007-get-apiv1componentdependencyid) | EP-COMPONENT-DEP | `GET /component/dependency/{id}` |
| [SRV-008](#srv-008-get-apiv1documentationcomponentid) | EP-DOC | `GET /documentation/component/{id}` |
| [SRV-009](#srv-009-get-apiv1configurationtypeindex) | EP-CONFIG-INDEX | `GET /configurationtype/index` |
| [SRV-010](#srv-010-get-apiv1configurationtypedetails) | EP-CONFIG-DETAILS | `GET /configurationtype/details` |
| [SRV-011](#srv-011-post-apiv1configurationtypemigrateidconfigurationversion) | EP-CONFIG-MIGRATE | `POST /configurationtype/migrate/{id}/{configurationVersion}` |
| [SRV-012](#srv-012-get-apiv1actionindex) | EP-ACTION-INDEX | `GET /action/index` |
| [SRV-013](#srv-013-post-apiv1actionexecute) | EP-ACTION-EXECUTE | `POST /action/execute` |
| [SRV-014](#srv-014-get-apiv1environment) | EP-ENV | `GET /environment` |
| [SRV-015](#srv-015-post-apiv1bulk) | EP-BULK | `POST /bulk` |
| [SRV-016](#srv-016-get-apiv1cacheclear) | EP-CACHE-CLEAR | `GET /cache/clear` |
| [SRV-017](#srv-017-response-caching-and-invalidation) | (cross-cutting, 03 section 3.1) | response caching and invalidation |
| [SRV-018](#srv-018-internationalization-language-parameter-and-locale-mapping) | (cross-cutting) | `language` parameter / locale mapping |
| [SRV-019](#srv-019-security-handlers-connection-and-command) | (cross-cutting, 03 section 3.2) | security handlers |
| [SRV-020](#srv-020-credential-decryption-vault) | (cross-cutting) | credential decryption |
| [SRV-021](#srv-021-documentation-ui-and-toggle) | (static UI) | documentation UI and toggle |
| [SRV-022](#srv-022-simple-query-language-q-parameter) | (query param `q` of EP-COMPONENT-INDEX) | simple query language |
| [SRV-023](#srv-023-server-configuration-keys-summary) | (server configuration) | configuration keys summary |
| [SRV-024](#srv-024-error-payload-errorpayload-and-errordictionary) | (error model, 03 section 5) | `ErrorPayload` / `ErrorDictionary` |
| [SRV-025](#srv-025-plugin-deployment-registry-and-reloading) | (deployment side effects of EP-CACHE-CLEAR) | plugin deployment, registry, reloading |

---

### SRV-001 Base path and JAX-RS application
- **Source**: `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/front/TalendComponentApplication.java` (+ doc `ref-rest-resources.adoc`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint (application root)
- **Applies to**: all endpoints
- **Description**: All REST resources are served under `@ApplicationPath("api/v1")`, i.e. `/api/v1/...` plus any servlet-context prefix configured by the deployment. Media type is `application/json` except binary endpoints (icons, dependency jars) and `documentation`. Resources: `component`, `configurationtype`, `action`, `documentation`, `environment`, `bulk`, `cache`. Gzip is on (`connector.attributes.compression = on` in `meecrowave.properties`).
- **Attributes**: none.
- **Contract for the Designer**: MUST prefix every call with `/api/v1`; SHOULD read `Environment.latestApiVersion` (SRV-014) to detect newer API versions.
- **Contract for the Runtime**: none
- **Server exposure**: path prefix `api/v1`.
- **Maturity level**: 0 - every call depends on the base path.
- **Example**:

```
GET http://localhost:8080/api/v1/component/index
```

### SRV-002 GET /api/v1/component/index
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/ComponentResource.java` (`getIndex`; impl `front/ComponentResourceImpl.java`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint
- **Applies to**: palette / component discovery
- **Description**: Returns `ComponentIndices` (list of `ComponentIndex`). Query parameters: `language` (default `en`, mapped by SRV-018), `includeIconContent` (default `false`), `q` (filter, SRV-022), `theme` (`light`/`dark`, default `light`). Results are cached (SRV-017). Payload: see [03](../03-component-server-api.md).
- **Attributes**: `language`, `includeIconContent`, `q`, `theme` (all optional query parameters).
- **Contract for the Designer**: MUST call it to build the palette; SHOULD pass the user language and cache the answer until `Environment.lastUpdated` changes.
- **Contract for the Runtime**: none (runtime loads plugins locally).
- **Server exposure**: `ComponentIndices{components[]}`.
- **Maturity level**: 0 - component discovery.
- **Example**:

```
GET /api/v1/component/index?language=en&includeIconContent=false
```

### SRV-003 GET /api/v1/component/details
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/ComponentResource.java` (`getDetail`; impl `front/ComponentResourceImpl.java`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint
- **Applies to**: form rendering, configuration defaults
- **Description**: Returns `ComponentDetailList` for the component ids passed in the repeatable query parameter `identifiers` (required), with `language` (default `en`). Each `ComponentDetail` carries the properties tree (`SimplePropertyDefinition` list with `path`, `metadata` map), actions, inputs/outputs, version, etc. Unknown ids yield 404 with `ErrorPayload` code `COMPONENT_MISSING`. Payload and every metadata key: see [03](../03-component-server-api.md).
- **Attributes**: `identifiers` (repeatable), `language`.
- **Contract for the Designer**: MUST fetch details before rendering a form or creating a job node; MUST persist `ComponentDetail.version` with the configuration (see SRV-004).
- **Contract for the Runtime**: none; the runtime obtains the same information from `ComponentManager` metadata.
- **Server exposure**: `ComponentDetailList{details[]}`.
- **Maturity level**: 0 - form model and defaults.
- **Example**:

```
GET /api/v1/component/details?identifiers=<componentId>&language=en
```

### SRV-004 POST /api/v1/component/migrate/{id}/{configurationVersion}
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/ComponentResource.java` (`migrate`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint
- **Applies to**: saved component configuration
- **Description**: Body: a flat `Map<String,String>` configuration saved at `configurationVersion`; response: the migrated `Map<String,String>` for the current component version (runs the component `MigrationHandler`).
- **Attributes**: path `id` (component id), path `configurationVersion` (int); JSON body map.
- **Contract for the Designer**: MUST call it on load when the stored version is lower than `ComponentDetail.version`, then persist the returned map with the new version.
- **Contract for the Runtime**: MUST perform the same migration through `ComponentManager` when it receives an older version (see RUN/LCM categories).
- **Server exposure**: request/response are flat property maps (`configuration.*` keys).
- **Maturity level**: 1 - without it, old saved jobs break after component upgrades.
- **Example**:

```
POST /api/v1/component/migrate/<id>/1   {"configuration.url":"..."}
```

### SRV-005 Icon endpoints
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/ComponentResource.java` (`familyIcon`, `icon`, custom `icon`, `getIconIndex`; `service/IconResolver.java`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint group
- **Applies to**: palette and canvas rendering
- **Description**: `GET /component/icon/family/{id}`, `GET /component/icon/{id}`, `GET /component/icon/custom/{familyId}/{iconKey}` return icon bytes (`application/octet-stream`, or JSON error), all with optional `theme`; `GET /component/icon/index` returns the list of available SVG icons (`application/json` or `image/svg+xml`). Icons are located on the plugin classpath by patterns `talend.component.server.icon.paths` (default `icons/%s.svg,icons/svg/%s.svg,icons/%s_icon32.png,icons/png/%s_icon32.png`); themed lookup (`talend.component.server.icon.theme.support`, default `true`), legacy fallback (`...theme.legacy`, default `true`), default theme `light`. Missing icons return 404 with `ICON_MISSING`.
- **Attributes**: path `id` / `familyId` / `iconKey`; query `theme`.
- **Contract for the Designer**: SHOULD render icons from these endpoints or from `includeIconContent=true`; MUST handle 404 with a default icon.
- **Contract for the Runtime**: none
- **Server exposure**: raw bytes; `Icon` in index payloads.
- **Maturity level**: 1 - UX only.
- **Example**:

```
GET /api/v1/component/icon/family/<familyId>?theme=dark
```

### SRV-006 GET /api/v1/component/dependencies
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/ComponentResource.java` (`getDependencies`; impl uses `service/VirtualDependenciesService.java`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint
- **Applies to**: remote deployment of a plugin to an engine
- **Description**: Returns `Dependencies` (map of id to `DependencyDefinition`, i.e. the Maven artifacts) for one or more ids given as repeatable query parameter `identifier` (component or configuration type id). Includes extension-provided dependencies when `talend.component.server.component.extend.dependencies` is `true` (default) and user extension jars (`talend.component.server.user.extensions.location`). Unknown plugin: `PLUGIN_MISSING`.
- **Attributes**: `identifier` (repeatable).
- **Contract for the Designer**: MAY use it to package a job.
- **Contract for the Runtime**: A runtime that does not have plugins locally MAY use it with SRV-007 to download them; otherwise none.
- **Server exposure**: `Dependencies{dependencies: {id: DependencyDefinition}}`.
- **Maturity level**: 2 - remote/`.car` style deployment, optional.
- **Example**:

```
GET /api/v1/component/dependencies?identifier=<componentId>
```

### SRV-007 GET /api/v1/component/dependency/{id}
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/ComponentResource.java` (`getDependency`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint (`application/octet-stream`)
- **Applies to**: remote deployment
- **Description**: Streams the binary (jar) of the artifact `id` (Maven coordinate or component id per the operation description). 404 with `PLUGIN_MISSING` if unknown.
- **Attributes**: path `id`.
- **Contract for the Designer**: MAY proxy it.
- **Contract for the Runtime**: MAY download artifacts with it after SRV-006.
- **Server exposure**: binary stream.
- **Maturity level**: 2 - optional deployment path.
- **Example**:

```
GET /api/v1/component/dependency/org.talend.demo:my-lib:1.0.0
```

### SRV-008 GET /api/v1/documentation/component/{id}
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/DocumentationResource.java` (`getDocumentation`; impl `front/DocumentationResourceImpl.java`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint
- **Applies to**: component help display
- **Description**: Returns `DocumentationContent{type (deprecated), source}` where `source` is the component documentation in asciidoc format (only asciidoctor is supported). Query: `language` (default `en`), `segment` (`ALL` default, `DESCRIPTION`, `CONFIGURATION`). Translations are read from `talend.component.server.component.documentation.translations` (default `${home}/documentations`, files named `documentation_${container-id}_language.adoc`).
- **Attributes**: path `id`; query `language`, `segment`.
- **Contract for the Designer**: SHOULD display it in a help panel and render asciidoc (or use the returned rendering).
- **Contract for the Runtime**: none
- **Server exposure**: `DocumentationContent`.
- **Maturity level**: 1 - documentation display.
- **Example**:

```
GET /api/v1/documentation/component/<id>?language=en&segment=DESCRIPTION
```

### SRV-009 GET /api/v1/configurationtype/index
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/ConfigurationTypeResource.java` (`getRepositoryModel`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint
- **Applies to**: dataset/datastore (storable configuration) reuse
- **Description**: Returns `ConfigTypeNodes` describing the tree of storable configuration types (datastores, datasets, ...). Query: `language` (default `en`), `lightPayload` (default `true`, omits the properties), `q` (filter, SRV-022).
- **Attributes**: `language`, `lightPayload`, `q`.
- **Contract for the Designer**: MUST use it to offer reusable datastores/datasets and to link them to components.
- **Contract for the Runtime**: none
- **Server exposure**: `ConfigTypeNodes{nodes: {id: ConfigTypeNode}}`.
- **Maturity level**: 1 - dataset/datastore reuse.
- **Example**:

```
GET /api/v1/configurationtype/index?lightPayload=true
```

### SRV-010 GET /api/v1/configurationtype/details
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/ConfigurationTypeResource.java` (`getDetail`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint
- **Applies to**: dataset/datastore form
- **Description**: Returns `ConfigTypeNodes` with full properties for the ids in repeatable `identifiers` (with `language`).
- **Attributes**: `identifiers`, `language`.
- **Contract for the Designer**: MUST use it to render dataset/datastore forms.
- **Contract for the Runtime**: none
- **Server exposure**: `ConfigTypeNodes`.
- **Maturity level**: 1 - same as SRV-009.
- **Example**:

```
GET /api/v1/configurationtype/details?identifiers=<id>
```

### SRV-011 POST /api/v1/configurationtype/migrate/{id}/{configurationVersion}
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/ConfigurationTypeResource.java` (`migrate`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint
- **Applies to**: stored dataset/datastore configuration
- **Description**: Same contract as SRV-004 for a configuration type node.
- **Attributes**: path `id`, `configurationVersion`; JSON body map.
- **Contract for the Designer**: MUST migrate stored datasets/datastores whose version is older.
- **Contract for the Runtime**: SHOULD migrate through `ComponentManager` for nested dataset versions.
- **Server exposure**: flat property maps.
- **Maturity level**: 1 - configuration evolution.
- **Example**:

```
POST /api/v1/configurationtype/migrate/<id>/1
```

### SRV-012 GET /api/v1/action/index
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/ActionResource.java` (`getIndex`; impl `front/ActionResourceImpl.java`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint
- **Applies to**: design-time actions
- **Description**: Returns `ActionList` of `ActionItem{component (family), type, name, properties}`. Query filters: repeatable `type`, repeatable `family`, `language` (default en). Includes actions from deployed plugins and virtual (extension) actions. Merged from ACT-023: `ActionItem.component` holds the *family* and `properties` are the action parameter definitions (`SimplePropertyDefinition`). Per component the same references also appear in `ComponentDetail.actions` / `ConfigTypeNode.actions` as `ActionReference{family, name, type, displayName, properties}` (`displayName` from `${family}.actions.${type}.${name}._displayName`).
- **Attributes**: `type`, `family`, `language`.
- **Contract for the Designer**: MAY use it for discovery; component details already carry action references. MUST use the `ActionReference` entries of the component/config detail to resolve triggers; MAY use the index for custom (`user`, ACT-001) actions.
- **Contract for the Runtime**: none
- **Server exposure**: `ActionList`.
- **Maturity level**: 1 - action discovery.
- **Example**:

```
GET /api/v1/action/index?type=healthcheck&family=myfamily
```

### SRV-013 POST /api/v1/action/execute
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/ActionResource.java` (`execute`; impl `front/ActionResourceImpl.java`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint (asynchronous `CompletionStage<Response>`)
- **Applies to**: healthcheck, suggestions, dynamic values, update, schema discovery, validation, dynamic dependencies, user actions
- **Description**: Query parameters `family`, `type`, `action` (all required) and `lang` (default `en`). Body: a flat `Map<String,String>` of `configuration.*` values. The server adds `$lang` (mapped language), decrypts vault values (SRV-020) and invokes the action synchronously inside the plugin. Result is JSON whose shape depends on the action type (see [03](../03-component-server-api.md) and category ACT). Errors: missing `action`/`type`/`family` -> 400 with `ACTION_MISSING`/`TYPE_MISSING`/`FAMILY_MISSING`; unknown action -> 404 `ACTION_MISSING`; `ComponentException` -> 400 (origin USER), 456 (BACKEND), 520 otherwise, with `ACTION_ERROR`.
- **Result shapes by action type** (merged from ACT-022): the server invokes the method synchronously and returns its result as JSON (`200`); `lang` defaults to `en`.

  | type | result |
  |---|---|
  | `healthcheck` | `HealthCheckStatus{status, comment}` |
  | `suggestions` | `SuggestionValues{cacheable, items[{id,label}]}` |
  | `dynamic_values` | `Values{items[{id,label}]}` |
  | `validation` | `ValidationResult{status, comment}` |
  | `update` | JSON of the returned object |
  | `schema`, `schema_extended` | record `Schema` JSON |
  | `discoverdataset` | `DiscoverDatasetResult{datasetDescriptionList[]}` |
  | `dynamic_dependencies` | `List<String>` |
  | `available_output` | `Collection<String>` |
  | `schema_mapping` | `String` |
  | `create_connection` / `close_connection` | Studio in-process only |
  | `user` | any JSON |

  Errors: `400` `ErrorPayload` when `family`/`type`/`action` is missing (`FAMILY_MISSING`, `TYPE_MISSING`, `ACTION_MISSING`); `404` `ACTION_MISSING` when no action matches; when the action throws `ComponentException` the status depends on `ErrorOrigin`: `USER` -> `400`, `BACKEND` -> `456`, `UNKNOWN`/other -> `520`, with `ErrorPayload{code: ACTION_ERROR, description: "Action execution failed with: ..."}`. Studio guess-schema failures use `DiscoverSchemaException{possibleHandleErrorWith: EXCEPTION|SILENT|RETRY|EXECUTE_MOCK_JOB|EXECUTE_LIFECYCLE}` (`SILENT`, `RETRY` not implemented).
- **Attributes**: `family`, `type`, `action`, `lang`; body map.
- **Contract for the Designer**: MUST send only the properties the action declares (see the action's `properties` from `ComponentDetail`), with the exact `configuration.<path>` keys; MUST interpret errors by status and `ErrorPayload.code`. MUST send only the parameters listed in the trigger `parameters` (key = action parameter path, value = form value at `path`); MUST map HTTP `400`/`456`/`520` to user-visible errors using `ErrorPayload.description` (MAY distinguish user vs backend errors); SHOULD debounce change-triggered calls and drop stale responses (inferred).
- **Contract for the Runtime**: none
- **Server exposure**: action result body; header `x-talend-tenant-id` (optional) selects the vault tenant.
- **Maturity level**: 1 - health check, suggestions, schema discovery.
- **Example**:

```
POST /api/v1/action/execute?family=myfamily&type=healthcheck&action=mycheck&lang=en
{"datastore.url":"jdbc:..."}
```

### SRV-014 GET /api/v1/environment
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/EnvironmentResource.java` (`get`; impl `front/EnvironmentResourceImpl.java`, `front/security/web/EnvironmentFilter.java`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint
- **Applies to**: feature/version negotiation and cache invalidation
- **Description**: Returns `Environment{latestApiVersion (int, highest @ApplicationPath api/vN), version, commit, time, lastUpdated (Date), connectors{version, pluginsHash, pluginsList}}`. `lastUpdated` changes when plugins are (re)deployed (or is the server start time when `talend.component.server.lastUpdated.useStartTime=true`). Disabled with `talend.component.server.environment.active=false`, in which case the filter returns HTTP 404.
- **Attributes**: none.
- **Contract for the Designer**: SHOULD poll it (or call on reconnect) to invalidate its component cache when `lastUpdated`/`connectors.pluginsHash` change; MUST tolerate 404.
- **Contract for the Runtime**: none
- **Server exposure**: `Environment`.
- **Maturity level**: 2 - negotiation and cache-hygiene features.
- **Example**:

```
GET /api/v1/environment
```

### SRV-015 POST /api/v1/bulk
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/BulkReadResource.java` (`bulk`; impl `front/BulkReadResourceImpl.java`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint
- **Applies to**: batching several GET calls
- **Description**: Body `BulkRequests{requests:[{path, verb, queryParameters, headers}]}`; response `BulkResponses` with each sub-response normalized (status, headers, body). Multi-valued query parameters are arrays of strings. Payload: see [03](../03-component-server-api.md).
- **Attributes**: request body only.
- **Contract for the Designer**: MAY use it to reduce round trips (index + details).
- **Contract for the Runtime**: none
- **Server exposure**: `BulkResponses`.
- **Maturity level**: 2 - optimization.
- **Example**:

```json
{"requests":[{"path":"/api/v1/component/index","verb":"GET","queryParameters":{},"headers":{}}]}
```

### SRV-016 GET /api/v1/cache/clear
- **Source**: `component-server-parent/component-server-api/src/main/java/org/talend/sdk/component/server/api/CacheResource.java` (`clearCaches`; impl `service/jcache/FrontCacheResolver.java`)
- **Category / Sub-area**: SRV / Endpoints
- **Kind**: REST endpoint (declared `@GET`)
- **Applies to**: operations / dev mode
- **Description**: Clears all server caches and redeploys plugins (`service.redeployPlugins()`); returns `CacheClear` (count of active caches cleared). Note the verb is GET although it mutates state.
- **Attributes**: none.
- **Contract for the Designer**: MAY expose a "reload components" action (Studio dev mode does).
- **Contract for the Runtime**: none
- **Server exposure**: `CacheClear`.
- **Maturity level**: 2 - dev/ops convenience.
- **Example**:

```
GET /api/v1/cache/clear
```

### SRV-017 Response caching and invalidation
- **Source**: `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/service/jcache/FrontCacheResolver.java`, `FrontCacheKeyGenerator.java`, `configuration/ComponentServerConfiguration.java`
- **Category / Sub-area**: SRV / Caching
- **Kind**: convention
- **Applies to**: index, details, action index, documentation, configuration type endpoints
- **Description**: Server-side response caching uses JCache `@CacheResult` on the resource implementations (`component`, `action`, `documentation`, `configurationtype`), keyed by request parameters (incl. mapped locale), max entries `talend.component.server.cache.maxSize` (default 1000). A refresher thread compares `Environment.lastUpdated` every `talend.vault.cache.jcache.refresh.period` ms (default 30000) and clears the caches when plugins changed. **HTTP validators are not implemented**: no `ETag`, `If-None-Match` or `Cache-Control` handling exists in the server code (searched `component-server-parent`; the task brief mentions ETag, this is a documented discrepancy). Clients cache using `Environment.lastUpdated` instead.
- **Attributes**: config keys above.
- **Contract for the Designer**: MUST NOT rely on `ETag`/`304`; SHOULD cache responses client-side keyed by URL + language and invalidate when `Environment.lastUpdated`/`connectors.pluginsHash` changes.
- **Contract for the Runtime**: none
- **Server exposure**: none (implicit).
- **Maturity level**: 1 - caching improves UX; correctness does not depend on it.
- **Example**:

```
GET /api/v1/environment  ->  compare lastUpdated with the stored value before reusing cached component details
```

### SRV-018 Internationalization (language parameter and locale mapping)
- **Source**: `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/service/LocaleMapper.java`, `configuration/ComponentServerConfiguration.java`
- **Category / Sub-area**: SRV / i18n
- **Kind**: convention
- **Applies to**: every endpoint accepting `language`/`lang`
- **Description**: The requested language is normalized to a small set to bound cache size: `talend.component.server.locale.mapping` (default `en*=en`, `fr*=fr`, `zh*=zh_CN`, `ja*=ja`, `de*=de`; a key ending with `*` matches any prefix; unmatched or null means `en`). Display names, descriptions, action labels and documentation are returned translated. Actions receive the mapped language as `$lang`. `talend.component.server.gridlayout.translation.support` (default `false`) toggles translation of `@GridLayout` tab names (Studio does not support it).
- **Attributes**: config keys above.
- **Contract for the Designer**: MUST send the UI language in `language`/`lang`; SHOULD accept that the returned language may differ (fallback `en`).
- **Contract for the Runtime**: none (component messages use `@Internationalized`, category INT/DSG).
- **Server exposure**: `language`/`lang` query parameters; translated fields (`displayName`, ...).
- **Maturity level**: 1 - display names in the user language.
- **Example**:

```
GET /api/v1/component/index?language=fr_CA   (mapped to fr)
```

### SRV-019 Security handlers (connection and command)
- **Source**: `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/service/security/SecurityExtension.java`, `NoopHandler.java`, `front/security/ConnectionSecurityProvider.java`, `CommandSecurityProvider.java`
- **Category / Sub-area**: SRV / Security
- **Kind**: convention (CDI extension points)
- **Applies to**: server deployment
- **Description**: The server fires CDI events `OnConnection` (websocket/connection validation) and `OnCommand` (per request validation); the handler observers are selected by name via `talend.component.server.security.connection.handler` and `talend.component.server.security.command.handler` (both default `securityNoopHandler`, the only accepted value shipped; a custom `@Named` observer can be added). The default deployment performs **no authentication**: the integrator MUST secure the server at the network/gateway level or provide a handler. There are no `@RolesAllowed` annotations on the resources.
- **Attributes**: the two config keys above.
- **Contract for the Designer**: MUST add authentication in front of the server (gateway/handler) in multi-tenant deployments; MAY forward `x-talend-tenant-id` (SRV-020).
- **Contract for the Runtime**: none
- **Server exposure**: none (`UNAUTHORIZED` error code exists in `ErrorDictionary`).
- **Maturity level**: 2 - hardening; not needed for a functional integration.
- **Example**:

```
-Dtalend.component.server.security.command.handler=myHandler
```

### SRV-020 Credential decryption (vault)
- **Source**: `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/front/security/SecurityUtils.java`, `front/ActionResourceImpl.java` (+ module `vault-client`)
- **Category / Sub-area**: SRV / Security
- **Kind**: convention
- **Applies to**: `POST /action/execute`
- **Description**: If any value of the submitted configuration starts with `vault:`, the server decrypts the values of properties whose metadata `tcomp::ui::credential` is `true` through the `VaultClient`, using the tenant from HTTP header `x-talend-tenant-id` (absent header means null tenant). Other properties pass through unchanged.
- **Attributes**: header `x-talend-tenant-id`.
- **Contract for the Designer**: MAY send encrypted (`vault:`-prefixed) credentials to action calls instead of clear text; MUST send the tenant header when the deployment is multi-tenant.
- **Contract for the Runtime**: SHOULD decrypt `vault:` values before instantiating components if it stores them encrypted (unverified: runtime-side handling is host-specific).
- **Server exposure**: header `x-talend-tenant-id`; metadata `tcomp::ui::credential`.
- **Maturity level**: 2 - optional secret handling.
- **Example**:

```
POST /api/v1/action/execute?...   x-talend-tenant-id: tenant1   {"datastore.password":"vault:v1:abc"}
```

### SRV-021 Documentation UI and toggle
- **Source**: `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/front/security/web/DocumentationToggle.java`, resource `component-server-parent/component-server/src/main/resources/META-INF/resources/documentation/openapi.html`
- **Category / Sub-area**: SRV / Documentation
- **Kind**: convention
- **Applies to**: `/documentation`, `/documentation/*`
- **Description**: The server serves an OpenAPI browsing page under `/documentation` (see `rest-openapi.adoc`). Controlled by `talend.component.server.documentation.active` (default `true`); when `false` the filter answers HTTP 404 (the configuration text says localhost calls are always allowed). This is distinct from the component documentation endpoint SRV-008.
- **Attributes**: config key above.
- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: static resources under `/documentation`.
- **Maturity level**: 2 - developer tooling.
- **Example**:

```
GET /documentation/openapi.html
```

### SRV-022 Simple query language (q parameter)
- **Source**: `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/service/SimpleQueryLanguageCompiler.java`
- **Category / Sub-area**: SRV / Filtering
- **Kind**: convention
- **Applies to**: `q` on `/component/index` and `/configurationtype/index`
- **Description**: Grammar: `field OP value` clauses combined with `AND` / `OR`, grouped with parentheses; `OP` is `=` or `!=`; map fields use `field[key]` (e.g. `metadata[mydata] = xxxx`). Comparison is on `String.valueOf(field)`; the literal `null` matches a null field. Unknown field or malformed expression raises `IllegalArgumentException` (surfaced as an error to the caller). Available field names are those registered by each resource (see [03](../03-component-server-api.md)).
- **Attributes**: `q`.
- **Contract for the Designer**: MAY use it for server-side palette filtering; MUST escape spaces properly in URLs.
- **Contract for the Runtime**: none
- **Server exposure**: `q` query parameter.
- **Maturity level**: 1 - filtering by family/metadata in palettes.
- **Example**:

```
GET /api/v1/component/index?q=(id = abc) OR (metadata[mode] = batch)
```

### SRV-023 Server configuration keys (summary)
- **Source**: `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/configuration/ComponentServerConfiguration.java` (+ doc `ref-server-configuration.adoc`, `studio.adoc`; extension SPI module `component-server-parent/extensions/component-server-extension-api`)
- **Category / Sub-area**: SRV / Configuration
- **Kind**: convention (MicroProfile Config `@ConfigProperty`)
- **Applies to**: server deployment
- **Description**: Keys (all read through MicroProfile Config: system properties, environment, config files). Full descriptions in the server-configuration appendix.

| Key | Default | Purpose |
|---|---|---|
| `talend.component.server.jaxrs.exceptionhandler.defaultMessage` | `false` | Replace exception messages; `false` shows real ones |
| `talend.component.server.maven.repository` | - | Local Maven repository |
| `talend.component.server.component.coordinates` | - | Comma-separated component GAVs |
| `talend.component.server.component.registry` | - | Property file(s) of GAVs; path may end with `*`/`*.properties` |
| `talend.component.server.documentation.active` | `true` | Enable `/documentation` |
| `talend.component.server.security.connection.handler` | `securityNoopHandler` | Connection validation handler |
| `talend.component.server.security.command.handler` | `securityNoopHandler` | Command validation handler |
| `talend.component.server.component.extend.dependencies` | `true` | Extensions add dependencies |
| `talend.component.server.component.documentation.translations` | `${home}/documentations` | Documentation translations folder |
| `talend.component.server.environment.active` | `true` | Enable `/api/v1/environment` |
| `talend.component.server.user.extensions.location` | - | Folder of per-plugin user extension jars and `user-configuration.properties` |
| `talend.component.server.user.extensions.provisioning.location` | `auto` | m2 provisioning of implicit artifacts (`auto`, `skip`, or a path) |
| `talend.component.server.component.extension.startup.timeout` | `180000` | Extension init timeout (ms) |
| `talend.component.server.component.extension.maven.repository` | - | Where extensions create dependencies |
| `talend.component.server.gridlayout.translation.support` | `false` | Translate grid layout tab names |
| `talend.component.server.request.log` | `false` | Log requests/responses (CXF only) |
| `talend.component.server.cache.maxSize` | `1000` | Max cache entries |
| `talend.component.server.lastUpdated.useStartTime` | `false` | `lastUpdated` = start time |
| `talend.component.server.icon.paths` | `icons/%s.svg,icons/svg/%s.svg,icons/%s_icon32.png,icons/png/%s_icon32.png` | Icon lookup patterns |
| `talend.component.server.icon.theme.support` | `true` | Themed icons |
| `talend.component.server.icon.theme.legacy` | `true` | Fallback to non-themed icons |
| `talend.component.server.icon.theme.default` | `light` | Default theme |
| `talend.component.server.locale.mapping` | `en*=en` newline `fr*=fr` newline `zh*=zh_CN` newline `ja*=ja` newline `de*=de` | Locale whitelist |
| `talend.component.server.plugins.reloading.active` | `false` | Undeploy and redeploy plugins on change |
| `talend.component.server.plugins.reloading.method` | `timestamp` | `timestamp` or anything else (`connectors`) |
| `talend.component.server.plugins.reloading.interval` | `600` | Check interval (s) |
| `talend.component.server.plugins.reloading.marker` | - | File whose timestamp is watched |
| `talend.vault.cache.jcache.refresh.period` | `30000` | Cache-invalidation poll period (ms), declared in `FrontCacheResolver` |

- **Attributes**: see table.
- **Contract for the Designer**: none beyond knowing which endpoints may be disabled (`environment`, `documentation`).
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - deployment tuning.
- **Example**:

```
java -Dtalend.component.server.component.coordinates=org.talend.demo:my-components:1.0.0 -jar component-server.jar
```

### SRV-024 Error payload (ErrorPayload and ErrorDictionary)
- **Source**: `component-server-parent/component-server-model/src/main/java/org/talend/sdk/component/server/front/model/ErrorDictionary.java`, `.../model/error/ErrorPayload.java` (+ `front/error/DefaultExceptionHandler.java`)
- **Category / Sub-area**: SRV / Errors
- **Kind**: payload field
- **Applies to**: all endpoints
- **Description**: Error responses carry `ErrorPayload{code (ErrorDictionary), description}`. `ErrorDictionary` values: `PLUGIN_MISSING`, `FAMILY_MISSING`, `TYPE_MISSING`, `COMPONENT_MISSING`, `CONFIGURATION_MISSING`, `ICON_MISSING`, `ACTION_MISSING`, `ACTION_ERROR`, `BAD_FORMAT`, `DESIGN_MODEL_MISSING`, `UNEXPECTED`, `UNAUTHORIZED`. Status mapping: missing entities 404, bad parameters 400, `ACTION_ERROR` 400/456/520 by `ComponentException.ErrorOrigin` (`USER`/`BACKEND`/other), unexpected 500 (or 520 for action callbacks). Unhandled exceptions become 500 `UNEXPECTED` with the message replaced by `talend.component.server.jaxrs.exceptionhandler.defaultMessage` unless that key is `false`. Field-level detail: see [03](../03-component-server-api.md).
- **Attributes**: `code`, `description`.
- **Contract for the Designer**: MUST parse `ErrorPayload` on non-2xx and surface `description`; MUST distinguish 4xx user errors, 456 backend errors and 520 unexpected errors from actions.
- **Contract for the Runtime**: none
- **Server exposure**: JSON error body.
- **Maturity level**: 0 - error propagation.
- **Example**:

```json
{"code":"ACTION_MISSING","description":"No action with id 'foo'"}
```

### SRV-025 Plugin deployment, registry and reloading
- **Source**: `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/service/ComponentManagerService.java`, `configuration/ComponentServerConfiguration.java` (+ doc `studio.adoc`)
- **Category / Sub-area**: SRV / Deployment
- **Kind**: convention
- **Applies to**: server startup and hot reload
- **Description**: The server deploys plugins from `talend.component.server.component.coordinates` (GAV list), `talend.component.server.component.registry` (property files, value = GAV) and the Maven repository `talend.component.server.maven.repository`; extensions may add virtual components/actions (`ExtensionComponentMetadataManager`). With `talend.component.server.plugins.reloading.active=true` it re-deploys on marker-file timestamp (`method=timestamp`) or on `CONNECTORS_VERSION` change; each (re)deployment updates `Environment.lastUpdated`. Studio wraps this with `component.java.*` `config.ini` keys (`component.java.coordinates`, `component.java.registry`, `component.java.m2`, `component.java.port`, `component.environment=dev` shows a reload button that reloads the components and invalidates the cache, unverified mapping to SRV-016) - see `studio.adoc`.
- **Attributes**: config keys in SRV-023.
- **Contract for the Designer**: MUST refresh its palette after `lastUpdated` changes (SRV-014).
- **Contract for the Runtime**: none (runtime deploys plugins itself with `ComponentManager`).
- **Server exposure**: `Environment.connectors`.
- **Maturity level**: 1 - a usable designer needs a way to deploy/reload components.
- **Example**:

```
-Dtalend.component.server.plugins.reloading.active=true -Dtalend.component.server.plugins.reloading.interval=60
```
