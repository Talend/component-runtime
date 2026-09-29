# Appendix - Configuration types

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`). Sources: `component-api/.../configuration/type/*`, `documentation/src/main/antora/modules/ROOT/pages/_partials/generated_configuration-types.adoc`, `ConfigurationTypeParameterEnricher`, `RepositoryModelBuilder`. Catalog: `CFG-007`..`CFG-012`, `CFG-017` in [CFG](../02-feature-catalog/CFG-configuration.md).

## 1. Types

A configuration type classifies a configuration class by an annotation meta-annotated with `@ConfigurationType(<type id>)`. `ConfigurationTypeParameterEnricher` emits two property metadata keys on the annotated object: `configurationtype::type` and `configurationtype::name` (= annotation `value()`, default `"default"`).

| Annotation (package) | `configurationtype::type` | Meaning | Rules | i18n display-name key |
|---|---|---|---|---|
| `@DataStore` (`configuration.type`) | `datastore` | connection to a backend | testable by `@HealthCheck` via `@Checkable` | `${family}.datastore.${name}._displayName` |
| `@DataSet` (`configuration.type`) | `dataset` | datastore + data selection | contains a datastore; every input/output has one; unique names; a source must be instantiable filling only the dataset | `${family}.dataset.${name}._displayName` |
| `@DatasetDiscovery` (`configuration.type`) | `datasetDiscovery` | dataset discovery configuration | extends `DatasetDiscoveryConfiguration` (`boolean retrieveDataset`, default `false`); contains a datastore | `${family}.datasetdiscovery.${name}._displayName` (validator key) |
| `@DynamicDependenciesConfiguration` (`configuration.type`) | `dynamicDependenciesConfiguration` | input of `@DynamicDependencies` | - | - |
| `@Checkpoint` (`org.talend.sdk.component.api.input.checkpoint`, RUN catalog) | `checkpoint` | checkpoint configuration and state of a streaming input | `CheckpointValidator` requires it on checkpointing inputs | - |

Metadata samples (from `generated_configuration-types.adoc`):

```json
{ "tcomp::configurationtype::name": "test", "tcomp::configurationtype::type": "dataset" }
```

On the server (prefix stripped): `{ "configurationtype::type": "dataset", "configurationtype::name": "test" }`.

## 2. Configuration type index (server)

Nodes (`ConfigTypeNode`): `id`, `version` (`@Version` of the class or `-1`), `parentId`, `configurationType` (type id), `name`, `displayName`, `edges` (child ids), `properties` (`SimplePropertyDefinition[]`, root path renamed `configuration`), `actions` (`ActionReference[]`). Nesting: a type whose model contains another type is its parent (datastore -> dataset). The family of a node is the family of the components that use it. See [03](../03-component-server-api.md).

```json
{ "nodes": { "idForDstore": { "datastore": "...", "edges": [ { "id": "idForDset" } ] }, "idForDset": { "dataset": "..." } } }
```

(the illustration is from `creating-dataset-datastore.adoc`; the real node fields are listed above.)

## 3. Related helpers

`DatasetDiscoveryConfiguration` (base class), `ConnectorRef`/`ConnectorReference` (`dependencies::connector` metadata, `CFG-013`), `ComponentManager.findDatasetParameterMeta|findDatastoreParameterMeta|findCheckpointParameterMeta` (runtime lookup by type).
