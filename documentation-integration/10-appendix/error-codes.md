# Appendix: error payloads, `ErrorDictionary` codes and HTTP status mapping

- **Framework version documented**: `1.2611.0-SNAPSHOT`.
- **Sources of truth**: `component-server-model/.../ErrorDictionary.java`, `.../error/ErrorPayload.java`, `component-server/.../front/*ResourceImpl.java`, `.../front/error/*`, `.../front/security/*`, `vault-client/.../VaultClient.java`, tests in `component-server/src/test/.../front/*Test.java`.
- Endpoint context: [03-component-server-api.md](../03-component-server-api.md).

## 1. `ErrorPayload`

`org.talend.sdk.component.server.front.model.error.ErrorPayload`:

| Field | Type | Nullable | Meaning |
|---|---|---|---|
| `code` | `ErrorDictionary` (JSON string) | no | Machine-readable category. |
| `description` | string | may be null | Human message; not localized; not stable. |

```json
{"code":"COMPONENT_MISSING","description":"No component 'dGhlLXRlc3QtY29tcG9uZW50I2NoYWluI2xpc3Q'"}
```

(Verbatim shape from `BulkReadResourceImplTest#valid`.) Property order is alphabetical (`code`, `description`) because the server JSON-B is built with `PropertyOrderStrategy.LEXICOGRAPHICAL` (`JsonbFactory`).

**Exception: bulk and 400 on `/component/details`.** `GET /component/details` returns, on HTTP 400, a JSON **object keyed by component identifier**, each value being an `ErrorPayload`: `{"<id1>": {ErrorPayload}, "<id2>": {ErrorPayload}}` (built from `Map<String, ErrorPayload>` in `ComponentResourceImpl.getDetail`; the OpenAPI class `SampleErrorForBulk` shows keys `error1`, `error2` as an illustration only). A client MUST NOT parse it as a single `ErrorPayload`.

`DefaultExceptionHandler` (JAX-RS `ExceptionMapper<Throwable>`): a `WebApplicationException` keeps its own response; another exception is mapped to HTTP 500 `{code: UNEXPECTED, description: <message>}` with content type `*/*`. When `talend.component.server.jaxrs.exceptionhandler.defaultMessage` is not `false`, the description is replaced by that configured text (`replaceException = !"false".equalsIgnoreCase(value)`). Note: the media type of the error response is `WILDCARD` in this handler, so a client SHOULD NOT rely on `Content-Type: application/json` for 500 errors (unverified at wire level).

## 2. `ErrorDictionary` (12 values, in declaration order)

| Code | Emitted by (server code) | Typical HTTP status | Meaning |
|---|---|---|---|
| `PLUGIN_MISSING` | `getDependency` (component found but plugin/container file absent, or Maven artifact absent: `onMissingJar`), `getDetail` (plugin container not found), family/component icon resolution, `DocumentationResource` (plugin missing) | 404 (in `getDetail`: value inside the 400 map) | The plugin (component jar/container) is not deployed or its file is missing. |
| `FAMILY_MISSING` | icon endpoints (`familyId` unknown), `ActionResource.execute` when `family` query param is null | 404 (icons) / 400 (action) | Unknown family identifier. |
| `TYPE_MISSING` | `ActionResource.execute` when `type` is null | 400 | Missing action type. |
| `COMPONENT_MISSING` | `getDependencies` (unknown id), `getDetail` (per id), `migrate` (component), `icon/{id}`, `DocumentationResource` (unknown component **and** no matching documentation content) | 404 (400 as a value inside the `details` error map) | Unknown component id. |
| `CONFIGURATION_MISSING` | `ConfigurationTypeResource.migrate` | 404 | Unknown configuration type id. |
| `ICON_MISSING` | icon endpoints, `icon/index` (no svg), virtual (extension) entities | 404 | No icon for the requested id/theme. |
| `ACTION_MISSING` | `ActionResource.execute` (action null -> 400; action not found -> 404) | 400 / 404 | Unknown or absent action. |
| `ACTION_ERROR` | `ActionResource.execute`, on any exception raised by the action | 400 (`ComponentException.ErrorOrigin.USER`), 456 (`BACKEND`), 520 (any other origin or non-`ComponentException`) | Action execution failed; description prefix: `Action execution failed with: `. Reason phrase: `Unexpected callback error`. |
| `BAD_FORMAT` | **not emitted by the component server**. Only `vault-client` (`VaultClient.decrypt`) uses it, for a missing `x-talend-tenant-id` header when the decrypt endpoint template needs it. | 404 (as coded in `VaultClient`) | Malformed request. |
| `DESIGN_MODEL_MISSING` | `getDetail`, when a component has no `DesignModel` (design extension absent) | inside the 400 map | Component design model not registered. |
| `UNEXPECTED` | `DefaultExceptionHandler`, `CompletionExceptionExceptionMapper`, `getIconIndex` failure, `ConfigurationTypeResource.migrate` failures, `ActionResource` wrapped `WebApplicationException`, `BulkReadResource` (invalid sub-request or servlet failure), `VaultClient` errors | 500 (default), 520 (config migration), or the upstream status (vault) | Anything not otherwise categorised. |
| `UNAUTHORIZED` | `ConnectionSecurityProvider` (`Invalid connection credentials`), `CommandSecurityProvider` (`Invalid command credentials`), `BulkReadResource` (constants) | 401 (filters); status 403 inside a bulk result | Rejected by security handler or forbidden in bulk mode. |

The OpenAPI annotations in `component-server-api` promise some codes that differ from the implementation; see "Discrepancies" below.

## 3. HTTP status matrix per endpoint (as implemented)

| Endpoint | Status | Body / `code` |
|---|---|---|
| any (security) | 401 | `UNAUTHORIZED` "Invalid connection credentials" (pre-matching filter) or "Invalid command credentials" |
| `GET /component/index` | 200 | `ComponentIndices`. Invalid `q` syntax throws `IllegalArgumentException` in `SimpleQueryLanguageCompiler`, mapped by `DefaultExceptionHandler` to 500 `UNEXPECTED` (inferred: no dedicated 400 mapping in code). |
| `GET /component/details` | 200; 400 with map id -> `ErrorPayload` | `COMPONENT_MISSING`, `PLUGIN_MISSING`, `DESIGN_MODEL_MISSING` per id. No identifiers -> 200 `{"details":[]}`. |
| `GET /component/dependencies` | 200; 404 | `COMPONENT_MISSING` "No component matching the id: `<id>`". No identifier -> 200 `{"dependencies":{}}`. |
| `GET /component/dependency/{id}` | 200 octet-stream; 404 | `PLUGIN_MISSING` ("No plugin matching the id", "No dependency matching the id", "No file found for: id"). |
| `GET /component/icon/family/{id}` | 200; 404 | `ICON_MISSING` / `FAMILY_MISSING` / `PLUGIN_MISSING` |
| `GET /component/icon/{id}` | 200; 404 | `ICON_MISSING` / `COMPONENT_MISSING` / `PLUGIN_MISSING` |
| `GET /component/icon/custom/{familyId}/{iconKey}` | 200; 404 | `ICON_MISSING` / `FAMILY_MISSING` / `PLUGIN_MISSING` |
| `GET /component/icon/index` | 200 `image/svg+xml`; 404 (`ICON_MISSING` "No svg icon available"); 406 if `Accept` excludes svg/json (asserted by test); 500 `UNEXPECTED` | |
| `POST /component/migrate/{id}/{v}` | 200; 404 | `COMPONENT_MISSING` "Didn't find component `<id>`". Exceptions thrown by a user `MigrationHandler` are **not** caught here: they surface through `DefaultExceptionHandler` as 500 `UNEXPECTED` (inferred from code; no try/catch in `ComponentResourceImpl.migrate`). |
| `GET /configurationtype/index`, `/details` | 200 | no error payload defined. |
| `POST /configurationtype/migrate/{id}/{v}` | 200; 404 `CONFIGURATION_MISSING`; 400 / 456 / 520 `UNEXPECTED` | `ComponentException` origin USER -> 400, BACKEND -> 456, other -> 520; non-`ComponentException` -> 520. Reason phrase `Unexpected migration error`; description prefix `Migration execution failed with: `. |
| `POST /action/execute` | 200; 400 `ACTION_MISSING`/`TYPE_MISSING`/`FAMILY_MISSING`; 404 `ACTION_MISSING`; 400/456/520 `ACTION_ERROR`; vault errors (see section 5) | |
| `GET /action/index` | 200 | |
| `GET /documentation/component/{id}` | 200; 404 | `COMPONENT_MISSING` (unknown id, or no documentation file for it), `PLUGIN_MISSING`; 500 `UNEXPECTED` if the adoc file cannot be read. Invalid `segment` value is rejected by JAX-RS (404/400 by the container; unverified). |
| `GET /environment` | 200; 404 (empty body) when `talend.component.server.environment.active=false` (`EnvironmentFilter` sets status 404). | |
| `POST /bulk` | 200 always for a well-formed body; each entry has its own `status` | see section 4 |
| `GET /cache/clear` | 200 | `CacheClear`. |

## 4. Bulk-mode error results

Per-request results inside `BulkResponses.responses[]` (constants in `BulkReadResourceImpl`):

| Condition | `status` | `response` |
|---|---|---|
| Path is null, does not start with `/api/v1`, or contains `?` | 400 | `{"code":"UNEXPECTED","description":"unknownEndpoint."}` |
| Path starts with `/api/v1/component/icon/` or `/api/v1/component/dependency/` (or path is null) | 403 | `{"code":"UNAUTHORIZED","description":"Forbidden endpoint in bulk mode."}` |
| Underlying endpoint error | the endpoint's status (e.g. 404, 400, 520) | the endpoint's JSON body (must be a JSON **object**; see 03 file) |
| Servlet-level failure | 500 | `ErrorPayload` `UNEXPECTED` with the exception message, then the outer request fails with `IllegalStateException` |
| A future completes exceptionally | outer HTTP 500 | `UNEXPECTED` |

The constant "Secured endpoint, ensure to pass the right token." (`forbiddenResponse`) is declared but unused in the current code.

## 5. Vault / credential errors (action execution)

Applicable to `POST /action/execute` when a credential parameter value starts with `vault:` (`SecurityUtils.hasCipheredKeys`). Implemented by `vault-client` and surfaced as `WebApplicationException` carrying an `ErrorPayload`:

| Case | HTTP status | Payload |
|---|---|---|
| Decrypt endpoint requires the tenant and header `x-talend-tenant-id` is absent | 404 | `BAD_FORMAT` "No header x-talend-tenant-id" |
| Vault answers with per-value errors / empty results | `talend.vault.cache.service.auth.cantDecipherStatusCode` (default 422; test fixtures use 400) | `UNEXPECTED` "Can't decipher properties: [...]" or "Decrypted values are empty" |
| Vault HTTP error | the upstream status | `UNEXPECTED` "Decryption failed: ..." (404 and empty body -> "Decryption failed: Endpoint not found, check your setup.") |
| Authentication to vault failed | 403 ("getOrRequestAuth failed") or the auth error status | `UNEXPECTED` |

`ActionResourceImplTest#executeFailWithBadEncrypted` asserts HTTP 400 with description `{"errors":["wrong vault_encrypt"]}` (mock vault). `POST /component/migrate` does **not** decipher credentials (asserted by `migrateWithEncrypted`).

## 6. Java exception -> status mapping (for hosts that embed the server logic)

| Java exception | Where | Result |
|---|---|---|
| `ComponentException` (`org.talend.sdk.component.api.exception`) with `getErrorOrigin() == USER` | action execute, config migrate | HTTP 400 |
| `ComponentException` with `BACKEND` | same | HTTP 456 |
| `ComponentException` with any other origin (`UNKNOWN`) | same | HTTP 520 |
| Any other `RuntimeException` in an action | action execute | HTTP 520 `ACTION_ERROR`; null message -> "unexpected null" (NPE) or "no error message" |
| `WebApplicationException` thrown inside an action (e.g. from an HTTP client service) | action execute | if its entity is an `ErrorPayload` it is rethrown as is; otherwise re-wrapped as `UNEXPECTED` with the original status and message |
| `CompletionException` | any async resource | unwrapped and delegated to the mapper of the cause (`CompletionExceptionExceptionMapper`) |

The user-facing errors of the runtime (component execution) are Java exceptions, not HTTP: see [../06-runtime-execution.md](../06-runtime-execution.md).

## 7. Discrepancies between OpenAPI annotations and code

| Item | Annotation says | Code does |
|---|---|---|
| `ActionResource.execute` 400 | "If the action is not set ... ACTION_MISSING" | Also 400 for `TYPE_MISSING`/`FAMILY_MISSING`, and 400 for `ComponentException` with USER origin (`ACTION_ERROR`). 456 for BACKEND is not documented at all. |
| `ConfigurationTypeResource.migrate` 520 | "UNEXPECTED" | Also 400 / 456 for `ComponentException` origins. |
| `ComponentResource.getIndex` `@APIResponse` media type | `application/octet-stream` | Class-level `@Produces(application/json)`; the payload is JSON. Documentation error only. |
| `ComponentResource.getIndex` `includeIconContent` schema | `@Schema(type = STRING, defaultValue = "en")` | Parameter is boolean, default `false`. Copy/paste error in annotation. |
| `ComponentResource.migrate` 404 | `ErrorPayload` | Response built without an explicit media type (JSON is negotiated by the provider). |
| `DocumentationResource` 200 description | "list of available and storable configurations" | Returns a `DocumentationContent`. Copy/paste error. |
| Server-side ETag / `If-None-Match` | (required by the integration task) | **Not implemented**; see 03 file, section "Caching". |
