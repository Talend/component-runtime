# Repository Knowledge: component-runtime

---

## Overview

`component-runtime` is the **Talend Component Kit (TCK) framework** itself — the
foundation on which all connector repos are built. It defines the component
lifecycle, annotations, testing utilities, Studio integration, and the Remote
Engine Gen 2 runtime. It is the only repository in the ecosystem that is **open
source**.

All connector repos (`connectors-se`, `connectors-ee`, `cloud-components`) consume
`component-runtime` as a **released Maven artifact** via the `<component-runtime.version>`
property in their parent POM — it does not need to be built locally for connector work.

- **GitHub**: `Talend/component-runtime` (open source)
- **Primary language**: Java / Maven
- **Reference docs**: https://codewiki.google/github.com/talend/component-runtime
- **Related repos**: see [repos-overview.md](https://github.com/Talend/di-ai-commons/blob/main/knowledge/repos/repos-overview.md)
- **Team**: TDI (QTDI in Jira) — sub-team **TCK** (Jira field `Components = TCK`)
- **Release cadence**: 3rd week of each month (independently of the connectors release)

---

## Module map

| Module path                        | Responsibility |
|--------------------------------------|----------------|
| component-api                      | Provides the Talend Component Kit API annotations and interfaces for component development. |
| component-form                     | Umbrella parent for Web UI form generation to represent component properties. |
| component-runtime-impl             | Internal implementation module that interprets the component API model into executable runtime code. |
| component-runtime-beam             | Provides Apache Beam runtime bridge and integration for component execution. |
| component-runtime-design-extension | Supplies design-time extensions and features for the Component Runtime Manager. |
| component-runtime-testing          | Umbrella parent containing testing utilities and fixtures for Spark, JUnit, HTTP, and Beam. |
| component-spi                      | Runtime SPI enabling components to use external programming models and custom implementations. |
| component-runtime-manager          | Core framework manager enabling component access, discovery, and dynamic instantiation. |
| component-server-parent            | Parent module aggregating component server model, API, implementation, and extensions. |
| component-starter-server           | Web application generating Maven project skeletons to accelerate component development. |
| component-studio                   | Parent module integrating component runtime with design studio environments. |
| component-tools-webapp             | Lightweight HTTP server web application for local component testing and validation. |
| container                          | Parent module for classloader management and Maven repository utilities. |
| documentation                      | Framework documentation generated as an Antora website. |
| sample-parent                      | Parent aggregating sample components demonstrating the Component Kit API. |
| singer-parent                      | Parent module for Singer-based component implementations and adapters. |
| talend-component-maven-plugin      | Maven plugin wrapping Talend Component Kit tools as reusable Mojos. |

---

## Architecture overview

- **Entry points**: `@PartitionMapper`, `@Processor`, `@Emitter` — defined in `component-api`
- **Annotation processing**: `talend-component-maven-plugin` validates TCK annotations at build time
- **Testing**: provides `component-runtime-testing` utilities consumed by connector repos for integration tests
- **Studio integration**: `tdi-studio-se` depends on `component-runtime` for Studio-side TCK support
- **Remote Engine Gen 2**: uses the connectors and TCK Docker image produced during release for Pipeline Designer

---

## Key patterns

### Changing or adding a TCK annotation
> TODO: SME to fill

### Adding a new attribute to an enriched annotation (e.g. `@Suggestable`)

`ActionParameterEnricher` (and other parameter enrichers) serialise **every annotation
method generically via reflection**. When you add a new attribute to an enriched
annotation:

- **No changes to the enricher are needed.** The new attribute is picked up automatically.
- The new key appears in the component metadata map as `tcomp::action::<type>::<methodName>`
  (e.g. `tcomp::action::suggestions::labelDisplayMode`).
- `SimplePropertyDefinition.metadata` (in `component-server-model`) is an open
  `Map<String, String>` — **no DTO changes needed** for a new enricher-produced key;
  it lands there automatically.

Test coverage: update the existing enricher test to assert the default value, and
add a new test for each non-default value.

### Bumping the TCK version in connector repos

When a new `component-runtime` version is released, update `<component-runtime.version>`
in the parent POM of each connector repo (`connectors-se`, `connectors-ee`,
`cloud-components`). This is typically done as part of the monthly connectors
release, picking up the TCK version from the **previous month's** component-runtime
release.

---

## Branch & PR conventions

- **Default branch**: `master` (not `main`). Always use `--base master` in any `gh pr create` call.
- Branch naming follows the same `username/QTDI-###_short_description` pattern as connector repos.

---

## Build & test commands

```bash
# Full build (this is a large repo — may take significant time)
mvn clean install -DskipTests

# With tests
mvn clean install

# Run a specific module's tests
mvn test -pl <module-path>

# Formatting
mvn spotless:check
mvn spotless:apply
```

---

## Testing gotchas

### Mockito / JUnit 5 incompatibility

`mockito-junit-jupiter:4.8.1` (pinned in the root POM) is **incompatible** with JUnit
5.10.0 on the test classpath — causes `TestInstantiationAwareExtension$ExtensionContextScope`
not found at runtime.

**Workaround**: use `MockitoAnnotations.openMocks(this)` in a `@BeforeEach` method
instead of `@ExtendWith(MockitoExtension.class)`.

### JUnit testing modules — prefer JUnit 5

The repo has **two** JUnit testing modules under `component-runtime-testing/`:
- `component-runtime-junit` — JUnit 5 (preferred for all new tests)
- `component-runtime-junit4` — JUnit 4 (legacy, do not use for new tests)

Always depend on `component-runtime-junit` for new test code.

---

## CI / Jenkinsfile patterns

### Dual Maven deploy — Sonatype + internal Nexus

When a stage must publish SNAPSHOTs to **both** `central.sonatype.com` (Sonatype OSS) and `artifacts-zl.talend.com` (internal Nexus), **two separate `mvn deploy` invocations are required**:

- The first call uses `-Possrh -Psnapshot` (existing Sonatype deploy, unchanged).
- The second call uses `-Pprivate_repository` — but this profile **overrides `snapshotRepository`**, so combining both profiles in a single call silently suppresses the Sonatype publish and only deploys to Nexus.

```groovy
// Sonatype deploy (existing — unchanged)
mvn ... -Possrh -Psnapshot deploy

// Internal Nexus deploy (new — runs after Sonatype)
withCredentials([usernamePassword(credentialsId: 'nexusCredentials', ...)]) {
    mvn ... --activate-profiles private_repository deploy
}
```

If the first deploy fails, the second is never triggered — correct behavior.

_Discovered: QTDI-3123, 2026-07-10_

### `stdBranch_buildOnly` — master/maintenance build guard

`stdBranch_buildOnly` is a boolean set by the CI framework that is `true` only for master/maintenance builds (not dev/PR builds). Always use this flag — not a branch name comparison — when adding deploy steps that must not run on PR builds:

```groovy
if (stdBranch_buildOnly) {
    // runs on master/maintenance only
}
```

_Discovered: QTDI-3123, 2026-07-10_

### Nexus credentials — `nexusCredentials`

`.jenkins/settings.xml` already has a `talend.snapshots` server entry. The credentials binding ID is `nexusCredentials`, which injects `NEXUS_USERNAME` and `NEXUS_PASSWORD`. **No `settings.xml` edits are needed** when adding a new deploy step targeting the internal Nexus — only add the `withCredentials` block in `Jenkinsfile`.

_Discovered: QTDI-3123, 2026-07-10_

---

## Coding rules delta

No known repo-specific exceptions to the shared [coding-rules.md](https://github.com/Talend/di-ai-commons/blob/main/knowledge/rules/coding-rules.md).
