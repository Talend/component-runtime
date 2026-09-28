# Module Knowledge: component-tools-webapp

## JAX-RS client gotchas

### CXF's JAX-RS client does not auto-discover a JSON-B provider — register it explicitly

[2026-09-23 | QTDI-3358] `jakarta.ws.rs.client.ClientBuilder.newClient().build()` on this repo's
CXF 4.1.8 stack does **not** auto-discover a JSON-B `MessageBodyReader`/`MessageBodyWriter` for
arbitrary `Map<String, Object>` (or POJO) payloads. Any client built this way and used to send/read
JSON bodies fails at runtime with:

```
jakarta.ws.rs.ProcessingException: No message body writer has been found for class java.util.HashMap
```

**Fix**: register johnzon-jsonb's bundled provider explicitly on the client:

```java
client.register(org.apache.johnzon.jaxrs.jsonb.jaxrs.JsonbJaxrsProvider.class);
```

`JsonbJaxrsProvider` ships **inside** the `org.apache.johnzon:johnzon-jsonb` jar — no separate
`johnzon-jaxrs` artifact is needed. If the class is imported directly (rather than relying on it
arriving transitively, e.g. via `component-server`), add an explicit `johnzon-jsonb` dependency to
this module's `pom.xml` rather than depending on a transitive path that could silently break.

This was found while adding test coverage for `JakartaJAXRSClient#action` (previously untested) —
the bug was real and pre-existing, not introduced by the test itself; reproduced first via the
`ProcessingException` failing without the fix, then confirmed green after registering the provider.

Applies to any future JAX-RS client built in this module (or elsewhere in the reactor) that
constructs its own `Client` rather than reusing an already-provider-registered one.
