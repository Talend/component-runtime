# Feature Catalog - INT: Interceptors and cross-cutting concerns

> Framework version documented: **1.2611.0-SNAPSHOT** (root `pom.xml`). Category prefix: `INT-`. Generated from the same data as [`index.INT.json`](index.INT.json); both agree exactly.

Scope: `@Intercepts`/`InterceptorHandler` mechanism, error handling (`ComponentException`, `DiscoverSchemaException`, exception unwrapping, configuration validation failure), and Studio-oriented connection/context helpers (`@CreateConnection`, `@CloseConnection`, `@Connection`, `@RuntimeContext`). Cross-references: [`RUN-runtime.md`](RUN-runtime.md), [`LCM-lifecycle.md`](LCM-lifecycle.md).

Levels (see `../09-integration-checklist.md`): 0 = mandatory, 1 = normal user experience, 2 = full TCK.

## Index

| ID | Name | Level | Designer | Runtime |
|---|---|---|---|---|
| [INT-001](#int-001-intercepts) | `@Intercepts` | 2 | none | MUST apply the chain in INT-003 when hosting services; the ComponentManager does it automatically. |
| [INT-002](#int-002-interceptorhandler) | `InterceptorHandler` | 2 | none | MUST honor the constructor injection rules if it re-implements the facade. |
| [INT-003](#int-003-interceptor-chain-resolution-and-service-proxies) | `Interceptor chain resolution and service proxies` | 2 | none | MUST call service methods through the generated proxy so interceptors run; MUST NOT call the underlying instance directly. |
| [INT-005](#int-005-componentexception) | `ComponentException` | 0 | MUST map it to a user-facing error: origin USER = fixable by the user (configuration), BACKEND = remote system problem. | MUST let it propagate as failure of the job/step and MUST NOT depend on the original exception classes (classloader isolation). |
| [INT-006](#int-006-componentexceptionerrororigin) | `ComponentException.ErrorOrigin` | 1 | SHOULD present USER errors as validation feedback and BACKEND errors as service unavailability. | SHOULD classify job failure causes with it. |
| [INT-007](#int-007-discoverschemaexception) | `DiscoverSchemaException` | 2 | MAY implement the recovery strategies (INT-008) for schema-guess failures. | none |
| [INT-008](#int-008-discoverschemaexceptionhandleerrorwith) | `DiscoverSchemaException.HandleErrorWith` | 2 | MAY react to EXECUTE_LIFECYCLE/EXECUTE_MOCK_JOB by running the component (Studio-like hosts). | none |
| [INT-009](#int-009-runtime-exception-unwrapping-invocationexceptionwrapper) | `Runtime exception unwrapping (InvocationExceptionWrapper)` | 0 | none | MUST expect `ComponentException` (never plugin-specific exception classes) from component calls; SHOULD log `originalType`. |

## Entries

### INT-001 `@Intercepts`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/interceptor/Intercepts.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/services-interceptors.adoc`)
- **Category / Sub-area**: INT / Interceptors
- **Kind**: annotation (meta)
- **Applies to**: service (marker annotations placed on service methods)
- **Description**: Marks an annotation type as an interceptor marker and binds it to an `InterceptorHandler`. The handler class is mandatory (`value`; the sentinel `InterceptorHandler.class` selects a framework built-in, currently only for `@Cached`, SVC-009). Interceptors on a method are ordered by `priority`.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | value | Class<? extends InterceptorHandler> | required | handler implementation |
  | priority | int | 0 | ascending sort; the first one is the outermost wrapper |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST apply the chain in INT-003 when hosting services; the ComponentManager does it automatically.
- **Server exposure**: none
- **Maturity level**: 2 - cross-cutting extension; components work without custom interceptors (checklist: interceptors at level 2).
- **Example**:

```java
@Intercepts(TimingHandler.class) @Retention(RUNTIME) @Target(METHOD) public @interface Timed { }
```

### INT-002 `InterceptorHandler`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/interceptor/InterceptorHandler.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/services-interceptors.adoc`)
- **Category / Sub-area**: INT / Interceptors
- **Kind**: interface
- **Applies to**: service
- **Description**: `Object invoke(Method method, Object[] args)` replaces the delegate method call. Default `findAnnotation(Method, Class<T>)` looks on the method then on its declaring class. Constructor selection by `InterceptorHandlerFacade` (first match wins): a constructor whose FIRST parameter is `BiFunction<Method,Object[],Object>` (the invoker to continue the chain - required to chain several interceptors); else one whose first parameter is `Object` (the delegate service); else any no-arg constructor. Any remaining parameter is resolved by type from the container services (invoker parameter = `BiFunction`, delegate = `Object`). A handler without invoker parameter breaks the chain and is only accepted when it is the only interceptor of the method (`Interceptors not compatible for ...`).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | invoke(Method, Object[]) | Object | - | intercepted call |
  | findAnnotation(Method, Class<T>) | Optional<T> | default | annotation lookup on method then class |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST honor the constructor injection rules if it re-implements the facade.
- **Server exposure**: none
- **Maturity level**: 2 - cross-cutting extension.
- **Example**:

```java
public class TimingHandler implements InterceptorHandler {
    private final BiFunction<Method, Object[], Object> invoker;
    public TimingHandler(final BiFunction<Method, Object[], Object> invoker) { this.invoker = invoker; }
    public Object invoke(final Method m, final Object[] args) { long t = System.nanoTime(); try { return invoker.apply(m, args); } finally { log(t); } }
}
```

### INT-003 `Interceptor chain resolution and service proxies`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/interceptor/InterceptorHandlerFacade.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/services-interceptors.adoc`)
- **Category / Sub-area**: INT / Interceptors
- **Kind**: convention
- **Applies to**: service
- **Description**: `ServiceHelper.createServiceInstance` requires a public no-arg constructor. If the service class (or any public method) carries an annotation meta-annotated with `@Intercepts`, or the class is not `Serializable`/has no `writeReplace`, an ASM subclass `<Service>$$TalendServiceProxy` (Serializable) is generated in the plugin loader; when interceptors exist the proxy delegates every call to `InterceptorHandlerFacade` wrapping a fresh service instance. The facade caches per `Method` a composed invoker: it collects the annotations on the METHOD (`method.getAnnotations()` only), keeps those whose type has `@Intercepts`, sorts by `priority`, and links handler i to handler i+1 (the last one calls the real method). Failure modes: `No available constructor for <handler>`, `No handler for <annotation>` (built-in placeholder other than `@Cached`), `Some handlers don't take an invoker as parameter`. Exceptions of the delegate: RuntimeException rethrown as is, others wrapped in IllegalStateException(message). (Inferred: a class-level marker annotation triggers proxy generation but is not read by the facade, so it has no effect unless repeated on the method.)
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST call service methods through the generated proxy so interceptors run; MUST NOT call the underlying instance directly.
- **Server exposure**: none
- **Maturity level**: 2 - cross-cutting extension.
- **Example**: none

### INT-004 (moved) -> see SVC-009

### INT-005 `ComponentException`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/exception/ComponentException.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/services-actions.adoc`)
- **Category / Sub-area**: INT / Error handling
- **Kind**: class
- **Applies to**: input, processor, output, standalone, service
- **Description**: The framework exception for component errors: carries `errorOrigin` (USER | BACKEND | UNKNOWN), `originalType` (class name of the original exception) and `originalMessage`; message is `(type) message`. The cause chain is converted to plain `Throwable` copies (`toGenericThrowable`, message `(fully.qualified.Type) : msg`) so it deserializes anywhere without the component classes. Constructors accept (origin,message[,cause]), (type,message,stackTrace,cause) etc.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | errorOrigin | ErrorOrigin | UNKNOWN | who is responsible |
  | originalType | String | cause class name or null | type of the wrapped exception |
  | originalMessage | String | - | message without the type prefix |

- **Contract for the Designer**: MUST map it to a user-facing error: origin USER = fixable by the user (configuration), BACKEND = remote system problem.
- **Contract for the Runtime**: MUST let it propagate as failure of the job/step and MUST NOT depend on the original exception classes (classloader isolation).
- **Server exposure**: HTTP mapping used by server endpoints: USER -> 400, BACKEND -> 456, other -> 520, body `ErrorPayload` with code `UNEXPECTED` (action execution and migrate endpoints).
- **Maturity level**: 0 - checklist: error propagation is Level 0.
- **Example**:

```java
throw new ComponentException(ComponentException.ErrorOrigin.USER, "Invalid URL: " + url);
```

### INT-006 `ComponentException.ErrorOrigin`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/exception/ComponentException.java`
- **Category / Sub-area**: INT / Error handling
- **Kind**: enum
- **Applies to**: input, processor, output, standalone, service
- **Description**: `USER` (misconfiguration), `BACKEND` (backend/system error), `UNKNOWN` (default).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | USER | enum | - | user error |
  | BACKEND | enum | - | backend error |
  | UNKNOWN | enum | default | anything else |

- **Contract for the Designer**: SHOULD present USER errors as validation feedback and BACKEND errors as service unavailability.
- **Contract for the Runtime**: SHOULD classify job failure causes with it.
- **Server exposure**: HTTP 400/456/520 (INT-005).
- **Maturity level**: 1 - distinguishing error classes is normal-UX, not required for correctness.
- **Example**: none

### INT-007 `DiscoverSchemaException`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/exception/DiscoverSchemaException.java`
- **Category / Sub-area**: INT / Error handling
- **Kind**: class
- **Applies to**: service (@DiscoverSchema actions), Studio guess-schema
- **Description**: RuntimeException for Studio's guess-schema feature adding `possibleHandleErrorWith` (default `EXCEPTION`). JSON-B property order: `localizedMessage, message, stackTrace, suppressed, possibleHandleErrorWith`; has a `@JsonbCreator(message, stackTrace, handling)`. Constructors from a `ComponentException` (uses `originalMessage` and cause) and from message + handling. `InvocationExceptionWrapper` keeps it unwrapped. Only the Studio DI integration (`TaCoKitGuessSchema`) interprets `possibleHandleErrorWith`; `component-server-parent` and `component-form` do not reference it.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | possibleHandleErrorWith | HandleErrorWith | EXCEPTION | recommended recovery |

- **Contract for the Designer**: MAY implement the recovery strategies (INT-008) for schema-guess failures.
- **Contract for the Runtime**: none
- **Server exposure**: Would surface as a generic action error (unverified for this type).
- **Maturity level**: 2 - Studio-only.
- **Example**: none

### INT-008 `DiscoverSchemaException.HandleErrorWith`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/exception/DiscoverSchemaException.java`
- **Category / Sub-area**: INT / Error handling
- **Kind**: enum
- **Applies to**: service (@DiscoverSchema), Studio
- **Description**: `EXCEPTION` (default, show error), `SILENT` (unhandled), `RETRY` (unhandled), `EXECUTE_MOCK_JOB` (run a mock job after user approval; connector MUST have no side effect), `EXECUTE_LIFECYCLE` (run the component lifecycle from configuration only, no user input; MUST have no side effect).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | EXCEPTION | enum | default | raise |
  | SILENT | enum | - | unhandled |
  | RETRY | enum | - | unhandled |
  | EXECUTE_MOCK_JOB | enum | - | mock job with confirmation |
  | EXECUTE_LIFECYCLE | enum | - | lifecycle execution |

- **Contract for the Designer**: MAY react to EXECUTE_LIFECYCLE/EXECUTE_MOCK_JOB by running the component (Studio-like hosts).
- **Contract for the Runtime**: none
- **Server exposure**: none
- **Maturity level**: 2 - Studio-only.
- **Example**: none

### INT-009 `Runtime exception unwrapping (InvocationExceptionWrapper)`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/base/lang/exception/InvocationExceptionWrapper.java`
- **Category / Sub-area**: INT / Error handling
- **Kind**: convention
- **Applies to**: all components and actions
- **Description**: Every reflective call (`LifecycleImpl.doInvoke`, action invokers, constructors) unwraps `InvocationTargetException` through `toRuntimeException`: `ComponentException` and `DiscoverSchemaException` pass through; a `java.*` RuntimeException whose cause is null or also `java.*` passes through; anything else is converted to `ComponentException(originalTypeName, message, stackTrace, mappedCause)` with suppressed exceptions mapped too (cycles cut via a visited set). Purpose: no plugin-classloader exception type ever crosses the isolation boundary.
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST expect `ComponentException` (never plugin-specific exception classes) from component calls; SHOULD log `originalType`.
- **Server exposure**: none
- **Maturity level**: 0 - error propagation across classloaders.
- **Example**: none

### INT-010 (moved) -> see ACT-011

### INT-011 (moved) -> see ACT-012

### INT-012 (moved) -> see ACT-012

### INT-013 (moved) -> see ACT-012

### INT-014 (moved) -> see SVC-019

### INT-015 (moved) -> see SVC-020

### INT-016 (moved) -> see VAL-012

