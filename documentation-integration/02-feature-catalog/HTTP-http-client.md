# HTTP - Declarative HTTP client service

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`; last release tag `1.2610.0`).
> Category prefix: `HTTP`. Machine-readable twin: [index.HTTP.json](index.HTTP.json) (generated from this file, both MUST agree).
> Related: [SVC-services.md](SVC-services.md), [built-in services appendix](../10-appendix/built-in-services.md), [01-overview-and-architecture.md](../01-overview-and-architecture.md).

The HTTP client is a **run-time and design-time service** implemented by `component-runtime-manager` (`service/http/*`) on top of `java.net.HttpURLConnection`. A component author declares a Java interface extending `HttpClient` whose methods carry `@Request`; the host generates a `java.lang.reflect.Proxy`. Designers never call it; a Runtime host that reuses `component-runtime-manager` gets it for free, a host that re-implements the runtime MUST implement every level-0 entry below with the exact semantics described.

Conventions: API base path `API/` = `component-api/src/main/java/org/talend/sdk/component/api/`; package `service/http`; implementation base `IMPL/` = `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/service/http/`; Antora page `documentation/src/main/antora/modules/ROOT/pages/services-built-in.adoc` (section "Using HttpClient").

Minimal end-to-end sample (from the reference doc):

```java
public interface APIClient extends HttpClient {
    @Request(path = "api/records/{id}", method = "GET")
    @Codec(decoder = RecordDecoder.class)
    Record getRecord(@Header("Authorization") String basicAuth, @Path("id") int id);
}
```

---

### HTTP-001 HttpClientFactory
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/HttpClientFactory.java` (+ `IMPL/HttpClientFactoryImpl.java`)
- **Category / Sub-area**: HTTP / Factory
- **Kind**: interface (`<T> T create(Class<T> api, String base)`); injectable built-in service
- **Applies to**: component, service
- **Description**: Creates a proxy client for an `api` interface. `create` throws `IllegalArgumentException` if `api` is not an interface, or (message "Invalid Http Proxy specification") if it does not extend `HttpClient` or has a non-default method without `@Request`. The proxy implements `api`, `HttpClient`, `Serializable` and the internal `Copiable`; `base(String)` is applied immediately (null allowed). `toString()` returns `"@Request " + base`.
- **Attributes**: `api` (`Class<T>`), `base` (`String`, base URL).
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST inject a serializable `HttpClientFactory` scoped to the plugin (serialization via `SerializableService`); MUST also auto-create and register a proxy for every `HttpClient` sub-interface that declares `@Request` methods so it can be injected by type (HTTP-002).
- **Server exposure**: none.
- **Maturity level**: 0 - components that use declarative clients fail at load without it.
- **Example**:

```java
final APIClient client = httpClientFactory.create(APIClient.class, "https://api.example.com");
```

### HTTP-002 HttpClient
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/HttpClient.java`
- **Category / Sub-area**: HTTP / Core
- **Kind**: interface (`@Deprecated void base(String base)`)
- **Applies to**: service, component
- **Description**: Marker super-interface every declarative client MUST extend. `base(String)` sets the base URI (deprecated in favour of `@Base` on a parameter, HTTP-004). The proxy handles `base` itself and throws `UnsupportedOperationException("HttpClient.<method>")` for other methods declared on `HttpClient`. Default methods of the interface are executed as Java defaults; methods without `@Request` that are not defaults are rejected at creation.
- **Attributes**: `base(String base)`.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST let components call `client.base(url)` (typically in `@PostConstruct`) and store it in the proxy handler; the base MUST be preserved when the proxy is copied.
- **Server exposure**: none.
- **Maturity level**: 0 - required for any declarative client.
- **Example**:

```java
client.base("http://localhost:8080");
```

### HTTP-003 @Request
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/Request.java`
- **Category / Sub-area**: HTTP / Request definition
- **Kind**: annotation (`@Target(METHOD)`)
- **Applies to**: methods of an `HttpClient` interface
- **Description**: Declares an HTTP request method. Leading and trailing `/` of `path` are stripped; `{name}` placeholders are replaced by `@Path` parameters. Return type handling: `Response<T>` (status, headers, body, error), `InputStream` (streamed, not buffered), `byte[]` (raw), `String`/`Void`, or any type handled by a matching decoder (HTTP-017).
- **Attributes**:

| Name | Type | Default | Meaning |
|---|---|---|---|
| `method` | `String` | `"GET"` | HTTP verb; overridden by an `@HttpMethod` parameter when non-null |
| `path` | `String` | `""` | Request path template appended to the base |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST parse each method once (annotations + parameters) and cache the execution plan; MUST fail creation on missing `@Request`.
- **Server exposure**: none.
- **Maturity level**: 0 - core of the feature.
- **Example**:

```java
@Request(path = "api/records", method = "POST")
Record createRecord(@Header("Authorization") String auth, Record body);
```

### HTTP-004 @Base
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/Base.java`
- **Category / Sub-area**: HTTP / Request definition
- **Kind**: annotation (`@Target(PARAMETER)`)
- **Applies to**: `String` parameter of a `@Request` method
- **Description**: The argument (converted with `String.valueOf`) is used as the base URL for this call, replacing the proxy base. Two `@Base` parameters on one method raise `IllegalStateException`.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST use it in preference to the proxy base; URL = base + `/` + path (single slash join, empty parts tolerated).
- **Server exposure**: none.
- **Maturity level**: 0 - recommended replacement of `HttpClient.base`.
- **Example**:

```java
Record get(@Base String base, @Path("id") int id);
```

### HTTP-005 @Url
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/Url.java`
- **Category / Sub-area**: HTTP / Request definition
- **Kind**: annotation (`@Target(PARAMETER)`)
- **Applies to**: parameter of a `@Request` method
- **Description**: The argument is the complete URL; it overrides `@Request.path` and the base. `@Path` placeholders are still substituted in it. Two `@Url` parameters raise `IllegalStateException`.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST ignore base and request path when present.
- **Server exposure**: none.
- **Maturity level**: 0 - part of the annotation contract.
- **Example**:

```java
@Request Record get(@Url String fullUrl);
```

### HTTP-006 @HttpMethod
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/HttpMethod.java`
- **Category / Sub-area**: HTTP / Request definition
- **Kind**: annotation (`@Target(PARAMETER)`)
- **Applies to**: `String` parameter
- **Description**: The argument is the HTTP verb for this invocation and overrides `Request.method()` (falls back to it when the argument is null). Two such parameters raise `IllegalStateException`.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST honor the override.
- **Server exposure**: none.
- **Maturity level**: 0 - part of the annotation contract.
- **Example**:

```java
@Request(path = "x") String call(@HttpMethod String verb);
```

### HTTP-007 @Path
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/Path.java`
- **Category / Sub-area**: HTTP / Parameters
- **Kind**: annotation (`@Target(PARAMETER)`)
- **Applies to**: parameter
- **Description**: Replaces every `{value}` placeholder in the path template with the argument (`String.valueOf`). Encoding keeps the characters `=@/:!$&'(),;~` unescaped, URL-encodes the rest, and maps space to `%20` while preserving `+`.
- **Attributes**:

| Name | Type | Default | Meaning |
|---|---|---|---|
| `value` | `String` | required | Placeholder name |
| `encode` | `boolean` | `true` | Encode the value |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST apply exactly the encoding above.
- **Server exposure**: none.
- **Maturity level**: 0 - part of the annotation contract.
- **Example**:

```java
@Request(path = "api/records/{id}") Record get(@Path("id") int id);
```

### HTTP-008 @Query
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/Query.java`
- **Category / Sub-area**: HTTP / Parameters
- **Kind**: annotation (`@Target(PARAMETER)`)
- **Applies to**: parameter
- **Description**: Adds `name=value` to the query string. Null arguments are skipped. A `Collection` argument is emitted according to `format`. With `encode=true` the value is URL-encoded except `?`, `/`, `,`.
- **Attributes**:

| Name | Type | Default | Meaning |
|---|---|---|---|
| `value` | `String` | required | Query parameter name |
| `format` | `QueryFormat` | `CSV` | How to serialize multi-valued arguments |
| `encode` | `boolean` | `true` | Encode the value |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST preserve declaration order of query parameters.
- **Server exposure**: none.
- **Maturity level**: 0 - part of the annotation contract.
- **Example**:

```java
@Request(path = "s") String search(@Query("q") String q, @Query(value = "tag", format = QueryFormat.MULTI) List<String> tags);
```

### HTTP-009 @QueryParams
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/QueryParams.java`
- **Category / Sub-area**: HTTP / Parameters
- **Kind**: annotation (`@Target(PARAMETER)`)
- **Applies to**: `Map<String, ?>` parameter
- **Description**: The whole map is expanded to query parameters (null values dropped when `encode=true`). A null map is treated as empty.
- **Attributes**:

| Name | Type | Default | Meaning |
|---|---|---|---|
| `encode` | `boolean` | `true` | Encode values |
| `format` | `QueryFormat` | `CSV` | Multi-value serialization |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST support Map arguments.
- **Server exposure**: none.
- **Maturity level**: 0 - part of the annotation contract.
- **Example**:

```java
@Request(path = "s") String search(@QueryParams Map<String, String> params);
```

### HTTP-010 QueryFormat
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/QueryFormat.java`
- **Category / Sub-area**: HTTP / Parameters
- **Kind**: enum
- **Applies to**: `@Query`, `@QueryParams`
- **Description**: How several values of one query parameter are serialized: `CSV` (one parameter, comma-separated values: `a=1,2`), `MULTI` (one parameter per value: `a=1&a=2`).
- **Attributes**: constants `CSV`, `MULTI`.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST implement both; any other value is an `IllegalArgumentException("Unsupported formatting")`.
- **Server exposure**: none.
- **Maturity level**: 0 - part of the annotation contract.
- **Example**:

```java
@Query(value = "id", format = QueryFormat.MULTI) List<Integer> ids
```

### HTTP-011 @Header
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/Header.java`
- **Category / Sub-area**: HTTP / Parameters
- **Kind**: annotation (`@Target(PARAMETER)`)
- **Applies to**: parameter
- **Description**: Sets one request header from the argument (`String.valueOf`). Null arguments are skipped. Duplicate header names across `@Header`/`@Headers` raise `IllegalArgumentException("conflictings keys")`.
- **Attributes**:

| Name | Type | Default | Meaning |
|---|---|---|---|
| `value` | `String` | required | Header name |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST set headers with `setRequestProperty` semantics before the configurer runs.
- **Server exposure**: none.
- **Maturity level**: 0 - part of the annotation contract.
- **Example**:

```java
Record get(@Header("Authorization") String auth);
```

### HTTP-012 @Headers
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/Headers.java`
- **Category / Sub-area**: HTTP / Parameters
- **Kind**: annotation (`@Target(PARAMETER)`)
- **Applies to**: `Map<String,String>` parameter
- **Description**: The whole map is added as request headers (entries with null value ignored).
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST support Map arguments.
- **Server exposure**: none.
- **Maturity level**: 0 - part of the annotation contract.
- **Example**:

```java
Record get(@Headers Map<String, String> headers);
```

### HTTP-013 @Codec
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/Codec.java`
- **Category / Sub-area**: HTTP / Serialization
- **Kind**: annotation (`@Target({TYPE, METHOD})`)
- **Applies to**: interface or method (method wins over interface)
- **Description**: Declares custom encoder(s) for the request payload and decoder(s) for the response. Implementations are instantiated with constructor injection of services (the constructor found by `Constructors.findConstructor`, parameters resolved from the plugin services, so `@Service`/`@Internationalized` types can be injected). Each is registered under its `@ContentType` value (default `*/*`).
- **Attributes**:

| Name | Type | Default | Meaning |
|---|---|---|---|
| `encoder` | `Class<? extends Encoder>[]` | `{}` | Payload encoders |
| `decoder` | `Class<? extends Decoder>[]` | `{}` | Response decoders |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST instantiate codecs with service injection and merge them with the defaults of HTTP-017 (user codecs win on identical content type).
- **Server exposure**: none.
- **Maturity level**: 0 - required to decode non-String payloads to `Record`/POJOs beyond JSON.
- **Example**:

```java
@Codec(encoder = RecordEncoder.class, decoder = RecordDecoder.class)
```

### HTTP-014 Encoder
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/Encoder.java`
- **Category / Sub-area**: HTTP / Serialization
- **Kind**: interface (`byte[] encode(Object value)`)
- **Applies to**: payload parameter
- **Description**: Converts the payload argument to bytes. A `byte[]` payload is sent as is without encoder. With a single registered encoder it is used directly; otherwise the encoder is picked by the request `Content-Type` header (HTTP-017).
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST call `encode` only for non-null, non-`byte[]` payloads.
- **Server exposure**: none.
- **Maturity level**: 0 - part of the codec contract.
- **Example**:

```java
public byte[] encode(Object v) { return jsonb.toJson(v).getBytes(UTF_8); }
```

### HTTP-015 Decoder
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/Decoder.java`
- **Category / Sub-area**: HTTP / Serialization
- **Kind**: interface (`Object decode(byte[] value, Type expectedType)`)
- **Applies to**: response body and error body
- **Description**: Converts response bytes to the declared return type (or the type requested in `Response.error(Class)`). Selected by response `Content-Type` (HTTP-017). `byte[]` return types bypass decoders.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST pass the generic `Type` (the `Response<T>` argument when wrapped).
- **Server exposure**: none.
- **Maturity level**: 0 - part of the codec contract.
- **Example**:

```java
public Object decode(byte[] v, Type t) { return jsonb.fromJson(new String(v, UTF_8), t); }
```

### HTTP-016 @ContentType
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/ContentType.java`
- **Category / Sub-area**: HTTP / Serialization
- **Kind**: annotation (`@Target(TYPE)`)
- **Applies to**: `Encoder` / `Decoder` implementation classes
- **Description**: Declares the media type a codec handles (wildcards `*` allowed, e.g. `application/*+json`).
- **Attributes**:

| Name | Type | Default | Meaning |
|---|---|---|---|
| `value` | `String` | `"*/*"` | Media type pattern |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST register codecs under this value; two codecs with the same key are an error (`IllegalArgumentException`).
- **Server exposure**: none.
- **Maturity level**: 0 - part of the codec contract.
- **Example**:

```java
@ContentType("text/csv") public class CsvDecoder implements Decoder { /* ... */ }
```

### HTTP-017 Default codecs and content-type matching
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/service/http/RequestParser.java`, `codec/CodecMatcher.java`, `service/MediaTypeComparator.java`, `codec/JsonpEncoder.java`, `JsonpDecoder.java`, `JAXBEncoder.java`, `JAXBDecoder.java`
- **Category / Sub-area**: HTTP / Serialization
- **Kind**: convention
- **Applies to**: every proxy
- **Description**: Registered defaults, added only if the user did not register the same key: encoders/decoders `*/json` and `*/*+json` (JSON-B backed, JSON-P types supported); `*/xml` and `*/*+xml` (JAXB, only when a JAXB context is found, i.e. the model has `@XmlRootElement`); fallback `*/*`: encoder writes `String.valueOf(value)` as UTF-8 (empty array for null), decoder returns `new String(bytes)`. Codec maps are sorted by `MediaTypeComparator` (most specific first). Matching (`CodecMatcher.select`): media type = `Content-Type` before `;`, lower-cased, empty means `*/*`; exact match first, then the first key whose pattern (with `*` as `.+` and `+` literal) matches; no match raises `IllegalStateException("No codec found for content-type: '<ct>'")`; results are memoized.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST implement exactly these defaults and this matching order.
- **Server exposure**: none.
- **Maturity level**: 0 - default JSON behaviour is what most connectors rely on.
- **Example**:

```java
// response "application/vnd.api+json; charset=utf-8" -> matches "*/*+json" decoder
```

### HTTP-018 @UseConfigurer
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/UseConfigurer.java`
- **Category / Sub-area**: HTTP / Configurer
- **Kind**: annotation (`@Target({TYPE, METHOD})`)
- **Applies to**: interface or method (method wins)
- **Description**: Registers a `Configurer` invoked on each call after headers and before the body is written. The configurer class MUST have a public no-arg constructor (it is instantiated reflectively with `getConstructor().newInstance()`).
- **Attributes**:

| Name | Type | Default | Meaning |
|---|---|---|---|
| `value` | `Class<? extends Configurer>` | `Configurer.class` | Configurer implementation |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST call `configure(connection, configuration)` once per invocation.
- **Server exposure**: none.
- **Maturity level**: 1 - needed for timeouts, custom auth or redirects; basic calls work without it.
- **Example**:

```java
@Request(path = "x") @UseConfigurer(BasicConfigurer.class) Record find(@ConfigurerOption("username") String u);
```

### HTTP-019 Configurer
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/Configurer.java`
- **Category / Sub-area**: HTTP / Configurer
- **Kind**: interface + nested `Configurer.Connection`, `Configurer.ConfigurerConfiguration`
- **Applies to**: HTTP connection
- **Description**: `void configure(Connection connection, ConfigurerConfiguration configuration)`. `Connection`: `getMethod()`, `getUrl()`, `getHeaders()` (`Map<String,List<String>>`), `getPayload()` (`byte[]` or null), `withHeader(name, value)` (adds a request property), `withReadTimeout(int ms)`, `withConnectionTimeout(int ms)`, `withoutFollowRedirects()` (redirects are followed by default). `ConfigurerConfiguration`: `Object[] configuration()`, `<T> T get(String name, Class<T> type)` (null if missing).
- **Attributes**: see methods.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST expose the current `HttpURLConnection` state through `Connection` and apply `postConfigure` (follow-redirect flag) before connecting.
- **Server exposure**: none.
- **Maturity level**: 1 - optional customization point.
- **Example**:

```java
connection.withReadTimeout(30_000).withConnectionTimeout(5_000);
```

### HTTP-020 @ConfigurerOption
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/ConfigurerOption.java`
- **Category / Sub-area**: HTTP / Configurer
- **Kind**: annotation (`@Target(PARAMETER)`)
- **Applies to**: parameter
- **Description**: The argument is passed to the configurer under `value` and retrievable with `ConfigurerConfiguration.get(name, type)`.
- **Attributes**:

| Name | Type | Default | Meaning |
|---|---|---|---|
| `value` | `String` | required | Option name |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST pass the raw (unconverted) argument object.
- **Server exposure**: none.
- **Maturity level**: 1 - goes with `@UseConfigurer`.
- **Example**:

```java
Record find(@ConfigurerOption("username") String user, @ConfigurerOption("password") String pwd);
```

### HTTP-021 OAuth1 configurer
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/configurer/oauth1/OAuth1.java` (+ `IMPL/configurer/OAuth1ProviderImpl.java`)
- **Category / Sub-area**: HTTP / Configurer
- **Kind**: final class with nested `OAuth1.Configurer` (a `Configurer`), `OAuth1.Configuration` (`@Data @Builder`), `OAuth1.OAuth1Provider` (SPI), and `buildParameters(method, url, payload, config)`. Annotated `@Partial("This doesn't fully implement oauth1 yet but is a good example of configurer entry point")`.
- **Applies to**: `@UseConfigurer(OAuth1.Configurer.class)` with `@ConfigurerOption("oauth1") OAuth1.Configuration`
- **Description**: Signs a request with OAuth 1.0 and sets the `Authorization` header. Not a complete OAuth1 implementation. It delegates to the first `OAuth1Provider` found by `ServiceLoader` (`IllegalStateException("No registered implementation of OAuth1Provider")` if none).
- **Attributes** (`OAuth1.Configuration`):

| Name | Type | Default | Meaning |
|---|---|---|---|
| `header` | `String` | `Authorization` | Header name |
| `headerPrefix` | `String` | `OAuth` | Prefix of the header value |
| `payloadHashAlgorithm` | `String` | `null` (ignored) | Payload hashing algorithm |
| `algorithm` | `String` | `HmacSHA1` | Signing algorithm |
| `signingHmacKey` | `byte[]` | derived from consumer secret and token secret | HMAC key |
| `signingSignatureKey` | `java.security.PrivateKey` | - | Private key for signature algorithms |
| `oauthParameters` | `Map<String,String>` | - | Extra (unencoded) OAuth parameters |
| `token` | `String` | ignored if unset | `oauth_token` |
| `tokenSecret` | `String` | - | Token secret |
| `consumerKey` | `String` | - | Consumer key |
| `consumerSecret` | `String` | - | Consumer secret |
| `nonce` | `String` | generated | Nonce |
| `timestamp` | `Long` | generated | Timestamp |

- **Contract for the Designer**: none
- **Contract for the Runtime**: SHOULD ship an `OAuth1Provider` if it wants to run connectors using it (the manager registers `OAuth1ProviderImpl` through `META-INF/services/org.talend.sdk.component.api.service.http.configurer.oauth1.OAuth1$OAuth1Provider`).
- **Server exposure**: none.
- **Maturity level**: 2 - niche authentication scheme.
- **Example**:

```java
@Request(path = "/oauth1") @UseConfigurer(OAuth1.Configurer.class)
String get(@ConfigurerOption("oauth1") OAuth1.Configuration configuration);
```

### HTTP-022 OAuth1Provider (SPI)
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/configurer/oauth1/OAuth1.java` (nested `@Internal interface OAuth1Provider`)
- **Category / Sub-area**: HTTP / Configurer
- **Kind**: interface, `ServiceLoader` SPI (internal API)
- **Applies to**: host
- **Description**: `Map<String,String> buildParameters(String method, String url, byte[] payload, OAuth1.Configuration config)` and `Configurer newConfigurer()`. Implementation provided by `component-runtime-manager`.
- **Attributes**: see methods.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY provide an alternative implementation via `ServiceLoader`.
- **Server exposure**: none.
- **Maturity level**: 2 - internal SPI.
- **Example**:

```java
// META-INF/services/org.talend.sdk.component.api.service.http.configurer.oauth1.OAuth1$OAuth1Provider
```

### HTTP-023 Response
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/Response.java` (+ `IMPL/ExecutionContext.java`)
- **Category / Sub-area**: HTTP / Result
- **Kind**: interface `Response<T>`
- **Applies to**: return type of a `@Request` method
- **Description**: Wraps the HTTP result. `int status()`, `Map<String,List<String>> headers()` (case-insensitive keys), `T body()` (decoded lazily, cached), `<E> E error(Class<E> type)` (decodes the error body; `String.class` returns raw text; null when no error). When the declared return type is `Response<...>`, error statuses are returned as a `Response` and do not throw; otherwise an error status throws `HttpException`.
- **Attributes**: see methods.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST NOT throw for error statuses when the method returns `Response`; MUST expose the error body via `error()`.
- **Server exposure**: none.
- **Maturity level**: 0 - required to consume APIs whose errors carry meaning.
- **Example**:

```java
Response<Record> r = client.getRecord(auth, 1);
if (r.status() == 200) { r.body(); }
```

### HTTP-024 HttpException
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/http/HttpException.java`
- **Category / Sub-area**: HTTP / Result
- **Kind**: class (`extends RuntimeException`)
- **Applies to**: `@Request` methods not returning `Response`
- **Description**: Thrown when the HTTP call fails with an `IOException` on reading the response (i.e. non-2xx). Message: `"code: <status>, message: <error as String>"`. `getResponse()` returns the `Response`. Network-level `IOException`s before a response are wrapped in `IllegalStateException`.
- **Attributes**: `response` (`Response`).
- **Contract for the Designer**: MAY show `getResponse().error(String.class)` for action failures (actions surface as `ComponentException`/HTTP 520 in the server).
- **Contract for the Runtime**: MUST propagate as a component failure (error propagation, level 0).
- **Server exposure**: none directly (server maps action exceptions, see [SRV-024](SRV-server.md#srv-024-error-payload-errorpayload-and-errordictionary)).
- **Maturity level**: 0 - error propagation.
- **Example**:

```java
try { client.getRecord(auth, 1); } catch (HttpException e) { int code = e.getResponse().status(); }
```

### HTTP-025 Request execution semantics
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/service/http/ExecutionContext.java`, `HttpRequestCreator.java`, `RequestParser.java`
- **Category / Sub-area**: HTTP / Execution
- **Kind**: convention
- **Applies to**: every call on a generated client
- **Description**: Order of operations: build URL (`@Url` or base + path; append `?` + joined query parameters), open `HttpURLConnection`, set the method, set headers, run `Configurer.configure`, apply `postConfigure` (follow redirects), write the payload if present (`setDoOutput(true)`), read status, then read the body. Return `InputStream` when declared (streamed, or wrapped in `Response<InputStream>`); return raw `byte[]` when declared; otherwise slurp then decode. On `IOException` while reading: read the error stream, build the error `Response`, return it if the declared type is `Response`, else throw `HttpException`. Response header names are case-insensitive; duplicated header keys are merged arbitrarily with a warning. The only per-call connection controls are those of `Configurer.Connection` (HTTP-019).
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST reproduce this order; SHOULD prefer `InputStream` return types for large payloads.
- **Server exposure**: none.
- **Maturity level**: 0 - defines observable behaviour.
- **Example**:

```java
@Request(path = "/big/http/data") InputStream getData();
```

### HTTP-026 Payload parameter
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/service/http/RequestParser.java`
- **Category / Sub-area**: HTTP / Request definition
- **Kind**: convention
- **Applies to**: exactly one parameter of a `@Request` method
- **Description**: The single parameter without any HTTP annotation is the request body. A second such parameter raises `IllegalArgumentException("has two payload parameters")`. A null payload sends no body. It is encoded per HTTP-014.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST enforce at most one payload parameter.
- **Server exposure**: none.
- **Maturity level**: 0 - required for POST/PUT.
- **Example**:

```java
@Request(path = "api/records", method = "POST") Record create(Record payload);
```
