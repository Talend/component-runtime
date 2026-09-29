#!/usr/bin/env python3
"""Generates 09-integration-checklist.md (+ .json twin) from 02-feature-catalog/index.json.
Usage: python build_checklist.py   (run from anywhere). Deterministic; safe to re-run.
Item text = catalog contract (designer / runtime) with links stripped, first clauses only.
'How to verify' = hand-written override (VERIFY dict) if present, else generated from the contract."""
import json, os, re, glob, collections

HERE = os.path.dirname(os.path.abspath(__file__))
DOC = os.path.abspath(os.path.join(HERE, '..'))
CAT = os.path.join(DOC, '02-feature-catalog')
VERSION = '1.2611.0-SNAPSHOT'

CATNAMES = {
    'DSG': 'Design (component model and metadata)', 'CFG': 'Configuration model', 'UI': 'UI / forms',
    'VAL': 'Validation', 'ACT': 'Design-time actions', 'DAT': 'Data model', 'RUN': 'Runtime execution',
    'LCM': 'Lifecycle and evolution', 'INT': 'Interceptors and cross-cutting', 'SVC': 'Services (host-provided injection) and SPI',
    'HTTP': 'Declarative HTTP client service', 'SRV': 'Component server capabilities', 'TST': 'Testing support (informative)'}
ORDER = list(CATNAMES)
LEVELNAMES = {0: 'Level 0 - Mandatory / minimal', 1: 'Level 1 - Normal user experience (adds to level 0)',
              2: 'Level 2 - Best experience / full TCK (adds to levels 0 and 1)'}

# ---------------------------------------------------------------- hand-written verifications (level 0 mostly)
# value: str (same for the single audience) or (designer, runtime) tuple; None = not applicable.
V = {
 'DSG-001': ('Load two components of one family and one of another: the palette shows two groups labelled `familyDisplayName`; two components with the same name in different families stay distinct.',
             'Register a plugin and call `findMapper/findProcessor` with plugin id + name: the right component is returned; a wrong family/name yields an empty Optional.'),
 'CFG-001': ('Render a component from `ComponentDetail.properties` only: every displayed field maps to a `path` from the server, none is invented, `$`-prefixed options are treated as technical.',
             'Instantiate a component whose `@Option` fields are set from a flat map: the fields hold the given values; a `final` `@Option` field is rejected by validation.'),
 'CFG-002': ('Fill a form with a nested object, a list of 2 items and a map: the produced map contains `configuration.a.b`, `configuration.list[0].x`, `configuration.list[1].x` keys and is accepted by `/action/execute` and by the runtime.',
             'Feed the same flat map to `ComponentManager`: the component object graph holds nested, list and map values; an unknown key is ignored.'),
 'CFG-003': ('Load a component exposing STRING, NUMBER, BOOLEAN, ENUM, OBJECT, ARRAY properties: each gets a matching input control; an unknown `type` value is rendered as STRING.',
             'Pass string values for int, boolean, enum and object options: the component receives correctly typed values without host-side conversion code.'),
 'CFG-014': ('Parse a `ComponentDetail` fixture: defaults from `defaultValue` pre-fill the form, constraints come from `validation` (not `metadata`), missing optional fields do not fail.',
             None),
 'VAL-001': ('Leave an active `@Required` field empty: the field is flagged as required and saving/running is refused; the same field hidden by an `@ActiveIf` is not enforced.',
             'Run a component with a null value for an active required option: start fails with a configuration validation error.'),
 'VAL-007': ('Feed a property with `validation.min/max/pattern/...`: the form enforces exactly those constraints; a property with an empty `validation` object is unconstrained.', None),
 'VAL-009': ('Render an ENUM property: only `enumValues` are selectable and the saved value is the constant name, not its label.',
             'Pass an unknown constant name for an enum option: instantiation fails with a validation error.'),
 'VAL-012': ('Enter an invalid value (e.g. below `@Min`): the error is displayed at design time before any run is possible.',
             'Start a job with an invalid configuration: it fails at start with a USER-origin configuration error message shown to the operator.'),
 'DAT-001': ('Receive a schema JSON from a discovery action: the host reads it without instantiating any `Record`.',
             'Run an input feeding a processor: the processor receives the same `Record` instance content unchanged; records are created through `RecordBuilderFactory` only.'),
 'DAT-002': 'Build a record with the builder and call `build()` once: a second `build()` is never issued by the host; two entries whose names collide are auto-renamed rather than lost.',
 'DAT-003': 'Read a record holding an INT, a nullable STRING and a DATETIME: typed accessors matching `Entry.getType()` return correct values; the nullable entry is read with `getOptional*` without NPE.',
 'DAT-004': ('Display a discovered schema JSON: all entries are listed with name, type, nullable.',
             'Create a schema through `RecordBuilderFactory`, use it for two records: it is immutable and shared without copy.'),
 'DAT-005': ('Map each of the 11 `Schema.Type` constants to a host column type: no constant is left unmapped.',
             'Round-trip one value of each of the 11 `Schema.Type` constants through the host record model: value equal after conversion.'),
 'DAT-006': ('Show a schema with a `rawName` differing from `name`: name, type, nullable and comment are displayed, rawName is used as label.',
             'Pass a record through two components: `name`/`rawName` pairs of entries are unchanged.'),
 'DAT-007': 'Build an entry without calling `nullable(...)` and one with `nullable(true)`: the first is not nullable, the second accepts null.',
 'DAT-008': 'Build a RECORD schema through the factory: the produced schema class is the implementation selected by the host (memory or Avro).',
 'DAT-013': ('Load a schema with a sanitized name and a `rawName`: the technical identifier is `name`, `rawName` is display-only.',
             'Send a record whose column had an illegal character: lookup by `Entry.getName()` succeeds, lookup by original name is not used.'),
 'DAT-016': 'Inject `RecordBuilderFactory` in a service of two plugins: both get a working factory and can build records.',
 'DAT-029': 'Run an input returning a POJO and one returning a `JsonObject`: the engine receives `Record` values in both cases.',
 'RUN-001': ('Load an `@Emitter` component: it shows as an input (no input connector, one `__default__` output).',
             'Run an emitter job to completion: `findMapper`, `create()`, `Input.start/next/stop` are driven and all produced records reach the sink.'),
 'RUN-002': ('Load a stoppable `@PartitionMapper`: `$maxRecords` and `$maxDurationMs` are rendered (default -1).',
             'Run a mapper: `assess()`, `split(n)` and `create()` are called; with `isStream()` true the job is handled as streaming.'),
 'RUN-003': 'Run a mapper: `assess()` is called once on a started mapper and its value is used to compute the split size.',
 'RUN-004': 'Split a mapper into 3: one `Input` per returned Mapper is created and the union of their records equals the sequential read.',
 'RUN-005': 'Split a mapper whose `@Split` takes an `int` and one taking a `long`: both receive the desired size without error.',
 'RUN-006': 'Run an input producing 5 records: `next()` is called until null, `start()` came first and `stop()` last; 5 records are emitted.',
 'RUN-008': ('Load a processor with 2 input and 2 output flows: 2 input and 2 output connectors are drawn.',
             'Run a processor over 3 records: call order is `start`, `beforeGroup`, `onNext` x3, `afterGroup`, `stop`.'),
 'RUN-009': 'Run a processor with one input: each incoming record triggers exactly one `onNext`, and `read(name)` returns the current record of that branch.',
 'RUN-010': 'Run 2 groups of records: `beforeGroup()` is called before the first record of each group.',
 'RUN-011': 'Run 2 groups: `afterGroup` runs at the end of each group and once more at end of data; records it emits reach the outputs.',
 'RUN-014': ('Load an output component with a `REJECT` flow: a reject connector is offered next to the default output.',
             'Emit to a branch that has no connection: no failure, record discarded; a connected branch receives its records.'),
 'RUN-015': 'Implement `OutputFactory.create(name)` for every name: writing a POJO to the emitter delivers a `Record` downstream.',
 'RUN-019': ('Load a processor with empty `outputFlows`: no outgoing connection can be drawn from it.',
             'Run a sink at the end of a job: it gets `beforeGroup`/`afterGroup` and a final flush.'),
 'RUN-021': ('Load a `@DriverRunner` component: it has no connectors and can be run as an independent step.',
             'Run it: `findDriverRunner`, `start()`, `runAtDriver()`, `stop()` are called on the coordinator; no records are handled.'),
 'RUN-022': 'Run a standalone component: `runAtDriver()` is invoked exactly once, with the plugin classloader as thread context classloader.',
 'RUN-023': 'Fail a component in `onNext`: `stop()` is still called (finally); starting a stopped instance is never attempted; every split Input is started and stopped.',
 'RUN-024': 'Compile the engine against `Mapper`, `Input`, `Processor`, `DriverRunner`, `Lifecycle` only (no implementation classes) and run an input->processor->output job.',
 'RUN-025': ('Save a component then reload the job: the stored `ComponentDetail.version` is persisted with the configuration.',
             'Call `findMapper(plugin, name, oldVersion, config)` for a component with a `MigrationHandler`: migration runs; an unknown component gives an empty Optional handled as "component missing".'),
 'RUN-039': 'Serialize a mapper and deserialize it on a second JVM/worker where the plugin is registered first: `create()` works; without the plugin registered the failure is explicit.',
 'RUN-041': ('Serialize a nested + list configuration: the produced keys equal the `path` values of `ComponentDetail` with `[i]` indexes.',
             'Pass the full map including defaults: the component instance is built; an invalid value surfaces as a user (configuration) error.'),
 'RUN-048': ('Draw connectors for a component from its `inputFlows`/`outputFlows` names.',
             'Wire branches by flow name: a record emitted to `reject` reaches the connection named `reject`, `__default__` reaches the default one.'),
 'RUN-049': 'Call `onNext` without `beforeGroup` on a test processor: the host never does this; the normal run performs `beforeGroup` first without NPE.',
 'LCM-001': ('Save a configuration for component v2, reopen it: the stored version (2) is kept and sent as the `configurationVersion` path parameter of migrate.',
             'Instantiate with the persisted version: it is the value passed to `find*`; the stored version is not rewritten silently.'),
 'LCM-002': 'Load a configuration saved with an older version: the migrated values come from the manager/server, not from host code.',
 'LCM-004': 'Register 2 plugins, remove one: its classloader is released (closed) and the other still runs; one `ComponentManager` per JVM.',
 'LCM-005': 'Load two plugins with conflicting versions of the same library: each works with its own version; the thread context classloader inside a component call is the plugin loader.',
 'LCM-006': 'Start the host with a plugin list, then restart with a newer plugin version: the plugin is replaced, not duplicated (stable id).',
 'LCM-007': 'Deploy a plugin whose jar has a `TALEND-INF/dependencies.txt`: all listed compile/runtime dependencies are resolved from the Maven-layout repository and the component runs.',
 'LCM-008': 'Start the host with `talend.component.manager.m2.repository` set to an empty directory: dependency resolution reads only from it (not from `~/.m2`).',
 'INT-005': ('Trigger a `ComponentException` with origin USER and one with BACKEND: the UI shows a user-fixable message for the first and a remote-system error for the second.',
             'Throw `ComponentException` in a component: the job/step fails and the host sees `ComponentException`, not the plugin-specific exception class.'),
 'INT-009': 'Throw a plugin-specific exception from a component: the engine catches a `ComponentException` (with `originalType` logged) and no plugin class leaks.',
 'SVC-001': ('None beyond ACT-002 (no direct check).',
             'Deploy a plugin with 2 `@Service` classes, one injecting the other: one instance per plugin exists and injection works in components and services.'),
 'SVC-005': ('Call an action that reads a local configuration value: the value arrives resolved; the host does not resolve `local_configuration:` itself.',
             'Set a key `<family>.<key>` and `<key>`: the family-prefixed key wins; the injected `LocalConfiguration` is scoped to the plugin id.'),
 'SVC-008': 'Inject `LocalCache` in two plugins, undeploy one: caches are independent and the removed plugin\'s cache is released.',
 'SVC-016': 'Inject `JsonBuilderFactory`, `JsonProvider`, `JsonReaderFactory`, `JsonWriterFactory`, `JsonGeneratorFactory`, `JsonParserFactory` by exact type and serialize an instance: Java serialization works.',
 'SVC-017': 'Inject `Jsonb` and (de)serialize a POJO: it works and shares the JSON-P buffers of the plugin.',
 'SVC-027': 'Deploy a service using an injected service inside `@PostConstruct`: it is non-null; on undeploy `@PreDestroy` runs; `@PostConstruct` runs once per container start.',
 'HTTP-001': 'Inject `HttpClientFactory` and a `HttpClient` sub-interface with `@Request` methods into a component, serialize the component: both work after deserialization.',
 'HTTP-002': 'Call `client.base(url)` in `@PostConstruct` then invoke a request: the URL starts with the base; the base survives copying of the proxy.',
 'HTTP-003': 'Create a client for an interface with a method lacking `@Request`: creation fails; a valid method is parsed once and reused.',
 'HTTP-004': 'Set `@Base` on a method: the request goes to that base + `/` + path with a single slash.',
 'HTTP-005': 'Use `@Url` on a parameter: the request goes to exactly that URL, ignoring base and path.',
 'HTTP-006': 'Use `@HttpMethod("PATCH")` (or another custom verb): the wire method is that verb.',
 'HTTP-007': 'Call a `@Path` method with a value needing encoding (space, `/`): the path segment is encoded as specified.',
 'HTTP-008': 'Call a method with 3 `@Query` parameters: the query string keeps declaration order.',
 'HTTP-009': 'Pass a `Map` to `@QueryParams`: each entry becomes a query parameter.',
 'HTTP-010': 'Use `QueryFormat.MULTI` and `QueryFormat.CSV` on a list argument: the list is arguments are formatted accordingly; another value gives `IllegalArgumentException`.',
 'HTTP-011': 'Send `@Header("X-A")`: the header reaches the server before the configurer runs.',
 'HTTP-012': 'Pass a `Map` to `@Headers`: all entries are sent as headers.',
 'HTTP-013': 'Declare `@Codec` with a custom encoder: it overrides the default for the same content type.',
 'HTTP-014': 'Send a null payload and a `byte[]` payload: `encode` is not called for either.',
 'HTTP-015': 'Return `Response<List<Foo>>`: the decoder receives the generic `List<Foo>` type.',
 'HTTP-016': 'Register two codecs with the same `@ContentType`: `IllegalArgumentException`.',
 'HTTP-017': 'Call with `application/json` and `text/plain` responses without custom codecs: the default JSON and text codecs decode them; content-type matching follows the documented order.',
 'HTTP-023': 'Call an endpoint returning 404 with a `Response<T>` return type: no exception, `status()==404` and `error(String.class)` returns the body.',
 'HTTP-024': ('Trigger an HTTP failure inside an action: the UI shows the error text (server answers 520 / ComponentException).',
              'Trigger an HTTP failure in a component: `HttpException` propagates as a component failure and fails the job.'),
 'HTTP-025': 'Execute a request with query, header and body: order of operations matches the documented execution semantics; large body returned as `InputStream` is streamed.',
 'HTTP-026': 'Declare a method with two payload parameters: creation fails; one payload is accepted.',
 'SRV-001': ('Call the server: every URL starts with `/api/v1`; `Environment.latestApiVersion` is read and a higher value is reported.', None),
 'SRV-002': ('Open the palette against a running server: components are listed from `/api/v1/component/index?language=<lang>`; a second open with unchanged `lastUpdated` is served from the client cache.', None),
 'SRV-003': ('Create a node: `/component/details?identifiers=<id>` is called before the form is rendered and `version` is stored with the configuration.', None),
 'SRV-024': ('Force a 400, a 456 and a 520 from `/action/execute`: each `ErrorPayload.description` is displayed and the three classes are distinguished (user / backend / unexpected).', None),
}

# ---------------------------------------------------------------- helpers
MD_LINK = re.compile(r'\[([^\]]+)\]\([^)]*\)')

def clean(s):
    s = MD_LINK.sub(r'\1', s.strip())
    return re.sub(r'\s+', ' ', s)

def is_none(s):
    s = s.strip()
    return s.lower().rstrip('.') == 'none' or bool(re.match(r'(?i)^none\b[^.;]{0,120}$', s))

def clauses(text):
    text = clean(text)
    parts = re.split(r'(?<=[.;])\s+(?=[A-Z])|;\s+', text)
    return [p.strip().rstrip(';') for p in parts if p.strip()]

def what(text, limit=330):
    """Contract text kept as implement statement: MUST clauses first, then others, capped."""
    cl = clauses(text)
    must = [c for c in cl if re.search(r'\bMUST\b', c)]
    rest = [c for c in cl if c not in must]
    out = ''
    for c in must + rest:
        c = c if c.endswith('.') else c + '.'
        if out and len(out) + len(c) + 1 > limit:
            break
        out = (out + ' ' + c).strip()
    if len(out) > limit + 60:
        out = out[:limit].rsplit(' ', 1)[0] + ' ...'
    return out

def strip_modal(c):
    c = re.sub(r'^(MUST NOT|SHOULD NOT|MUST|SHOULD|MAY)\s+', lambda m: 'never ' if 'NOT' in m.group(1) else '', c.strip())
    c = re.sub(r'\b(MUST NOT|SHOULD NOT)\b', 'must not', c)
    c = re.sub(r'\b(MUST|SHOULD|MAY)\b\s*', '', c)
    return c.rstrip('.')

def gen_verify(e, aud):
    text = e['designer'] if aud == 'Designer' else e['runtime']
    cl = clauses(text)
    pick = next((c for c in cl if re.search(r'\bMUST\b', c)), cl[0] if cl else '')
    pick = strip_modal(pick)
    if len(pick) > 200:
        pick = pick[:200].rsplit(' ', 1)[0] + ' ...'
    who = 'Designer' if aud == 'Designer' else 'Runtime'
    return f'run a fixture component/payload that uses `{e["name"]}` and check that the {who} host does: {pick}.'

def verify(e, aud, split):
    v = V.get(e['id'])
    if v is not None:
        if isinstance(v, tuple):
            x = v[0] if aud == 'Designer' else v[1]
            if x:
                return x
        elif aud in ('Designer', 'Runtime') and (e['tag'] == aud or not split):
            return v
        elif isinstance(v, str):
            return v
    return gen_verify(e, aud)

# ---------------------------------------------------------------- load
idx = json.load(open(os.path.join(CAT, 'index.json'), encoding='utf-8'))

catfile = {p: os.path.basename(glob.glob(os.path.join(CAT, p + '-*.md'))[0]) for p in ORDER}
def slug(h):
    h = h.strip().lower().replace('`', '')
    h = re.sub(r'[^\w\- ]', '', h)
    return h.replace(' ', '-')
anchor = {}
for p in ORDER:
    for line in open(os.path.join(CAT, catfile[p]), encoding='utf-8'):
        m = re.match(r'###\s+((?:[A-Z]+)-\d{3})\s+(.*)', line)
        if m and '(moved)' not in line:
            anchor[m.group(1)] = slug(line[4:])

def link(e):
    a = anchor.get(e['id'])
    f = f'02-feature-catalog/{catfile[e["category"]]}'
    return f'[{e["id"]}]({f}#{a})' if a else f'[{e["id"]}]({f})'

items = []   # sub-checks
for e in idx:
    d_ok, r_ok = not is_none(e['designer']), not is_none(e['runtime'])
    if e['tag'] == 'Both' and d_ok and r_ok:
        subs = [('Designer', True), ('Runtime', True)]
    elif e['tag'] == 'Both':          # category default, one/both contracts empty
        subs = [('Both', False)]
    else:
        subs = [(e['tag'], False)]
    for aud, split in subs:
        if aud == 'Designer':
            w = what(e['designer'])
        elif aud == 'Runtime':
            w = what(e['runtime'])
        else:   # Both without a split
            parts = [('Designer', e['designer']), ('Runtime', e['runtime'])]
            w = ' '.join(f'{n}: {what(t)}' for n, t in parts if not is_none(t)) or \
                'Informative: no host obligation stated in the contract; support it if the host exposes this feature.'
        if aud == 'Both':
            v = V.get(e['id'])
            if isinstance(v, str):
                vv = v
            elif isinstance(v, tuple):
                vv = ' / '.join(x for x in v if x)
            else:
                t = e['designer'] if not is_none(e['designer']) else e['runtime']
                pick = strip_modal(clauses(t)[0]) if clauses(t) and not is_none(t) else 'the feature is exercised without error'
                vv = f'run a fixture using `{e["name"]}` and check: {pick[:200]}.'
        else:
            vv = verify(e, aud, split)
        items.append(dict(id=e['id'], name=e['name'], category=e['category'], level=e['level'], tag=e['tag'],
                          audience=aud, split=split, what=w, verify=vv, link=link(e)))

# ---------------------------------------------------------------- counts
def cnt(f):
    return sum(1 for e in idx if f(e))
levels = (0, 1, 2)
feat = {(l, t): cnt(lambda e, l=l, t=t: e['level'] == l and e['tag'] == t) for l in levels for t in ('Designer', 'Runtime', 'Both')}
def checks(l, aud):
    """sub-checks applicable for certification of aud at level l (not cumulative)"""
    return sum(1 for i in items if i['level'] == l and (i['audience'] == aud or (i['audience'] == 'Both' and not i['split'])))
dchk = {l: sum(1 for i in items if i['level'] == l and i['audience'] in ('Designer',)) +
           sum(1 for i in items if i['level'] == l and i['audience'] == 'Both') for l in levels}
rchk = {l: sum(1 for i in items if i['level'] == l and i['audience'] in ('Runtime',)) +
           sum(1 for i in items if i['level'] == l and i['audience'] == 'Both') for l in levels}
# 'Both' unsplit (category default) counts for both certifications.

# ---------------------------------------------------------------- render
L = []
w = L.append
w('# 09 - Integration checklist (maturity levels 0 / 1 / 2)')
w('')
w(f'> Framework version documented: `{VERSION}` (root `pom.xml`). GENERATED FILE: do not edit by hand; regenerate with `python documentation-integration/.work/build_checklist.py` from [02-feature-catalog/index.json](02-feature-catalog/index.json) ({len(idx)} features).')
w('> Conventions: MUST / SHOULD / MAY are RFC 2119. Every item links to its catalog entry (contract, attributes, example). Related: [README](README.md), [07 Designer blueprint](07-designer-blueprint.md), [08 Runtime blueprint](08-runtime-blueprint.md), [known discrepancies](10-appendix/known-discrepancies.md).')
w('')
w('## 1. How to use')
w('')
w('1. Decide what you build: a **Designer** (design-time host, client of the Component Server), a **Runtime** (executes components with `ComponentManager`), or both. Certification is **separate** for the two (section 3).')
w('2. Work through the levels in order. Each line is `- [ ] **ID Name** [Designer|Runtime|Both] - what to implement - How to verify: ...`. Tick it only when the verification observably passes.')
w('3. A `[Both]` feature whose Designer and Runtime contracts differ is listed **twice** (one `Designer` line, one `Runtime` line). Tick each independently; a Designer-only host skips the `Runtime` lines and vice versa.')
w('4. "What to implement" is the catalog contract shortened to its MUST clauses first (then SHOULD/MAY). The full text is behind the ID link. A MUST clause is required to pass the item; SHOULD/MAY clauses are recommended and do not block a pass.')
w('5. The "How to verify" text is either hand-written (level 0 and key features) or generated from the first MUST clause of the contract (run a fixture that uses the feature and check that clause). Refine generated ones in a test plan before relying on them.')
w('6. Read the [server-side prerequisites](#2-server-side-prerequisites-known-limitations-of-the-component-server) first: several assumptions of older documentation (ETag, authentication) are wrong.')
w('')
w('### Level definitions (cumulative)')
w('')
w('| Level | Name | Meaning | Omitting an item causes |')
w('|---|---|---|---|')
w('| 0 | Mandatory / minimal | Discover components (server index/details), load and run them correctly: configuration serialization and defaults, plugin loading and classloader isolation, mapper/producer/processor/output/standalone lifecycle, Record/Schema handling, required validations, `version` passed through, mandatory services injection, error propagation. Only what is needed for correctness. | wrong behaviour |')
w('| 1 | Normal user experience | Usable product: forms with layouts and common widgets, display names/i18n, icons, documentation, `@ActiveIf` conditions, standard validations, health check, suggestions/dynamic values, schema discovery, dataset/datastore reuse, configuration migration, batch groups, multi-output/reject flows, client-side caching, connection handling. | degraded but working UX |')
w('| 2 | Best experience / full TCK | Everything else: update actions, async validation, discover-dataset, dynamic dependencies, checkpoint/streaming, record pointers, advanced widgets, interceptors, bulk, environment/feature negotiation, `.car` deployment, Studio/Singer bridges, full i18n, testing helpers. | lost optional capability |')
w('')
w('## 2. Server-side prerequisites (known limitations of the Component Server)')
w('')
w('These are facts about the provided server (verified in code, see [known-discrepancies.md](10-appendix/known-discrepancies.md) C1, C4, C5, D01, D04, D05). They are not host checklist items; they constrain how items are implemented and verified. No item below or above claims `ETag` support.')
w('')
w('- [ ] **No `ETag` / `If-None-Match` / `Last-Modified` / `Cache-Control` support.** The server never returns `304`. A host MUST NOT send conditional headers. Caching is **client-side**: key by URL + `language`, invalidate on `Environment.lastUpdated` / `connectors.pluginsHash` (SRV-014, SRV-017). Read every "caching" item of this checklist that way.')
w('- [ ] **No authentication by default.** Both security handlers default to `securityNoopHandler`. A production host MUST put authentication/authorization in front of the server (gateway, filter, custom handler); the Designer MUST NOT assume roles or scopes (SRV-019).')
w('- [ ] **`GET /api/v1/cache/clear` mutates state** (redeploys plugins, clears caches) although it is a `GET`. Never call it from prefetchers, health probes or crawlers; do not treat it as idempotent; protect it (SRV-016).')
w('- [ ] **Version is an argument, not a configuration key.** `POST /component/migrate/{id}/{configurationVersion}`; there is no `tcomp::component::version` key (LCM-001, SRV-004).')
w('- [ ] **Technical option is `$maxDurationMs`** (milliseconds); `$maxDurationSeconds` does not exist (CFG-016, RUN-028). Checkpoint switch is the JVM property `talend.checkpoint.enabled` (RUN-033).')
w('- [ ] **Documentation UI toggle.** `talend.component.server.documentation.active` guards only the static `/documentation` UI, not `/api/v1/documentation/...` (D21).')
w('')
w('## 3. Scoring')
w('')
w('Counts are computed from `index.json` by the generator. **Features** = catalog entries by `tag`. **Checks** = tickable lines (a `Both` feature with two different contracts yields one Designer and one Runtime line; a `Both` feature with a category-default tag and a single contract yields one line counted for both hosts).')
w('')
w('### 3.1 Features per level and tag')
w('')
w('| Level | Designer | Runtime | Both | Total features | Cumulative (<= level) |')
w('|---|---|---|---|---|---|')
cum = 0
for l in levels:
    t = sum(feat[(l, x)] for x in ('Designer', 'Runtime', 'Both'))
    cum += t
    w(f'| {l} | {feat[(l, "Designer")]} | {feat[(l, "Runtime")]} | {feat[(l, "Both")]} | {t} | {cum} |')
tD = sum(feat[(l, 'Designer')] for l in levels); tR = sum(feat[(l, 'Runtime')] for l in levels); tB = sum(feat[(l, 'Both')] for l in levels)
w(f'| **All** | **{tD}** | **{tR}** | **{tB}** | **{tD + tR + tB}** | **{tD + tR + tB}** |')
w('')
w('### 3.2 Applicable checks per certification')
w('')
w('| Level | Designer checks | Runtime checks | Designer cumulative | Runtime cumulative |')
w('|---|---|---|---|---|')
cd = cr = 0
for l in levels:
    cd += dchk[l]; cr += rchk[l]
    w(f'| {l} | {dchk[l]} | {rchk[l]} | {cd} | {cr} |')
w(f'| **All** | **{sum(dchk.values())}** | **{sum(rchk.values())}** | | |')
w('')
w('Lines in this file: ' + str(len(items)) + f' (= {len(idx)} features + {sum(1 for i in items if i["split"]) // 2} split `Both` features counted twice).')
w('')
w('### 3.3 Pass rule')
w('')
w('- A host is **Designer Level N** when 100% of its applicable Designer checks (lines tagged `Designer`, the `Designer` line of split `Both` features, and unsplit `Both` lines) for levels <= N pass. Ignore lines that only concern the Runtime.')
w('- A host is **Runtime Level N** when 100% of its applicable Runtime checks (lines tagged `Runtime`, the `Runtime` line of split `Both` features, and unsplit `Both` lines) for levels <= N pass.')
w('- Designer and Runtime are certified **separately**; a product providing both reports two levels. Level N requires all lower levels (cumulative): no level skipping.')
w('- Only MUST clauses gate a pass. Record any not-applicable item (feature not offered by the host) with a written justification in the report; it is excluded from the denominator only when the whole feature is out of the host\'s declared scope (for example a Runtime that does not run streaming components), never for a Level 0 item.')
w('- `TST-*` items are informative helpers for verification; they follow the same rule but are typically ticked "n/a" if you do not reuse the JUnit tooling.')
w('')
w('## 4. Summary')
w('')
w('### Minimal viable Designer (Designer Level 0)')
w('')
w('A Designer at Level 0 talks to the Component Server under `/api/v1`, reads the component index and details, and builds a palette grouped by family and category (component identity is family + name). From `ComponentDetail.properties` it builds a form that honours types (STRING, NUMBER, BOOLEAN, ENUM, OBJECT, ARRAY), defaults and the `path` of every property, enforces required, min/max/pattern-style constraints read from the `validation` object and restricts enums, and serializes the values into the flat `configuration.<path>` map (with `[i]` list indexes). It stores the component `version` next to each saved configuration and can display and classify server errors (`ErrorPayload`, 4xx, 456, 520) and `ComponentException` origins. It needs no layouts, widgets, actions, i18n or icons. It must not assume ETag or authentication.')
w('')
w('### Minimal viable Runtime (Runtime Level 0)')
w('')
w('A Runtime at Level 0 embeds `ComponentManager`, registers plugins from a Maven-layout repository with one isolated classloader per plugin (thread context classloader set around every call), and injects the mandatory services (`@Service` lifecycle order, `LocalConfiguration`, `LocalCache`, JSON-P/`Jsonb`, `RecordBuilderFactory`, `HttpClientFactory`). It resolves components by plugin, name, persisted version and flat configuration, and drives the lifecycle: `Mapper.assess/split/create`, `Input.start/next/stop`, `Processor.start`, `beforeGroup`, `onNext` per record, `afterGroup` (plus end-of-data flush), `stop` in a finally block, and `DriverRunner.runAtDriver` for standalone components. It routes named outputs (`__default__`, `REJECT`), never fails on unconnected branches, creates records only through `RecordBuilderFactory`, converts non-Record values, ships serialized mappers only to workers where the plugin is registered, and propagates `ComponentException` (with USER / BACKEND origin) as job failure. Streaming, checkpoint, splitting optimizations beyond one input per split, and engine adapters beyond a simple sequential runner are Level 1-2.')
w('')

grp = collections.OrderedDict()
for i in items:
    grp.setdefault(i['level'], collections.OrderedDict()).setdefault(i['category'], []).append(i)
sec = 5
for l in levels:
    n = sum(len(v) for v in grp[l].values())
    w(f'## {sec}. {LEVELNAMES[l]}')
    w('')
    w(f'{n} checks. Justification: level 0 = omitting causes wrong behaviour; level 1 = degraded but working UX; level 2 = lost optional capability (per-feature justification is in the catalog entry, "Maturity level").')
    w('')
    for p in ORDER:
        lst = grp[l].get(p)
        if not lst:
            continue
        w(f'### {sec}.{ORDER.index(p) + 1} {p} - {CATNAMES[p]}')
        w('')
        for i in lst:
            aud = i['audience']
            tagtxt = i['tag'] if not i['split'] else 'Both'
            suffix = f' {aud}' if i['split'] else ''
            w(f'- [ ] **{i["id"]} {i["name"]}** [{tagtxt}]{suffix} ({i["link"]}) - {i["what"]} - How to verify: {i["verify"]}')
        w('')
    sec += 1

w(f'## {sec}. Integration report template')
w('')
w('Copy, fill, and attach to the integration review. One report per host (Designer or Runtime).')
w('')
w('```markdown')
w('# TCK integration report')
w('')
w('| Field | Value |')
w('|---|---|')
w('| Host name / version | |')
w('| Host type | Designer / Runtime |')
w(f'| TCK framework version | {VERSION} |')
w('| Component Server used (Designer) / plugin source (Runtime) | |')
w('| Date / tester | |')
w('| Level claimed | 0 / 1 / 2 |')
w('')
w('| Level | Applicable checks | Passed | Failed | N/A (justified) | Pass % | Level reached |')
w('|---|---|---|---|---|---|---|')
w('| 0 | | | | | | yes / no |')
w('| 1 | | | | | | yes / no |')
w('| 2 | | | | | | yes / no |')
w('')
w('| ID | Audience | Level | Result (pass/fail/n.a.) | Evidence (test name, log, screenshot) | Comment / deviation |')
w('|---|---|---|---|---|---|')
w('| DSG-001 | Designer | 0 | | | |')
w('| RUN-001 | Runtime | 0 | | | |')
w('')
w('Known limitations accepted: (ETag not supported by server: client-side caching used; authentication provided by ...)')
w('```')
w('')

open(os.path.join(DOC, '09-integration-checklist.md'), 'w', encoding='utf-8', newline='\n').write('\n'.join(L))

twin = dict(frameworkVersion=VERSION, features=len(idx),
            featuresPerLevel={str(l): {t: feat[(l, t)] for t in ('Designer', 'Runtime', 'Both')} for l in levels},
            checksPerLevel={str(l): {'Designer': dchk[l], 'Runtime': rchk[l]} for l in levels},
            items=[{k: i[k] for k in ('id', 'name', 'category', 'level', 'tag', 'audience', 'what', 'verify')} for i in items])
with open(os.path.join(DOC, '09-integration-checklist.json'), 'w', encoding='utf-8', newline='\n') as f:
    json.dump(twin, f, indent=2, ensure_ascii=False); f.write('\n')
print(len(idx), 'features ->', len(items), 'checks; designer', dchk, 'runtime', rchk)
