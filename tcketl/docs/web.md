# tcketl-web - the designer in a browser

`tcketl-web` is the Vaadin version of the designer: the same pipeline model, form engine and component-server
client, with a browser UI. This page records what is shared, how it is built, what the browser changes, and the limits.

## Run

```text
mvn -f tcketl/pom.xml -o install -DskipTests          # once, or after a change in another module
mvn -f tcketl/pom.xml -pl tcketl-web spring-boot:run \
    -Dspring-boot.run.arguments=--tcketl.web.default-server=http://localhost:8765/api/v1
```

Then open <http://localhost:8080>. The first start builds the frontend with node and npm (several minutes); the next
ones take seconds. This is the Vaadin development mode: a production bundle (`vaadin:build-frontend`,
`spring-boot:repackage`) and a container image are **not** set up (unverified, not produced).

## Modules

```text
tcketl-web ──┬──► tcketl-forms-vaadin ──► tcketl-forms-core ──► tcketl-domain
             └──► tcketl-app-core ───────► tcketl-component-client, tcketl-persistence, tcketl-config
tcketl-app ──┬──► tcketl-forms (JavaFX renderer) ──► tcketl-forms-core
             └──► tcketl-app-core
```

| Module | Responsibility |
|---|---|
| `tcketl-app-core` | UI-free view models and services: `CompositionRoot`, session, palette, canvas, project, icons, `AsyncRunner`. Depends on `javafx-base` (observable properties) only, no scene graph. |
| `tcketl-forms-core` | Neutral form engine: `FormViewModel`, `FormBindings`, `ActionService`, and `FormAssembler<N>` which decides what a form contains (tabs, conditions, buttons, lists) through a `FormToolkit<N>`. |
| `tcketl-forms` | `FxFormToolkit`: the JavaFX widgets. |
| `tcketl-forms-vaadin` | `VaadinFormToolkit` and `VaadinFormView`: the Vaadin widgets, same behaviour. |
| `tcketl-web` | Spring Boot + Vaadin app: `DesignerView` (page), `canvas/` (SVG canvas custom element `tcketl-canvas.js` and its JSON protocol), `InspectorPanel`, `ProjectBar`, `WebSettings`. |

## How the browser differs

- **One `CompositionRoot` per browser tab.** Pipeline and connection are never shared between tabs; everything is
  closed on detach so no call outlives the tab. Server answers reach the UI through `ui.access`.
- **The component-server is called by the web server, not by the browser.** The browser only talks to `tcketl-web`.
  This avoids CORS and keeps the server model owner, but it means the web server must reach the component-server.
- **Canvas.** A dependency-free SVG custom element. The browser sends `select`, `move`, `drop`, `link` and `delete`
  events; the server validates them against `CanvasViewModel` and pushes the whole graph back (`CanvasProtocol`).
- **Forms.** Layout tabs are built lazily, typed groups (dataset, datastore, ...) are collapsible `Details`, other
  complex objects add no panel, `@ActiveIf` hides widgets (no room), documentation is a tooltip after 0.5 s, services
  (healthcheck, suggestions, dynamic values, update, schema guess) go through the same `ActionService`.
- **Pipelines.** A pipeline lives in the tab; `Download` produces a `.tcketl.json`, `Open...` uploads one (5 MB max) and
  reads it with the validation and migration of the desktop application. **Nothing is stored on the server**, so users
  never see each other's work. Server-side project storage needs authentication and is out of scope of v1.
- **Path fields** are plain text: a browser cannot browse the file system of the machine running the pipeline.

## Settings (`application.properties` or command line)

| Key | Default | Meaning |
|---|---|---|
| `tcketl.web.default-server` | `http://localhost:8080/api/v1` | What a new tab connects to; always allowed. |
| `tcketl.web.allowed-hosts` | empty | Comma separated hosts a user may point the application to; when set, only these (and the default). |
| `tcketl.web.block-private-hosts` | `false` | Refuse hosts that resolve to loopback, link-local, site-local or wildcard addresses. Set it to `true` when the application is shared. |

## Limits and risks

- **No authentication** in v1: anybody who reaches the application can make it call the allowed hosts. Use
  `allowed-hosts` and `block-private-hosts` (SSRF); the check resolves the name once at connect time, so DNS
  rebinding is not covered.
- **Vaadin version**: `vaadin.version` must stay at 24.10.7 or older; 24.10.8+ needs a commercial licence. Only the
  open source Flow components (`vaadin-core`) are used.
- **Not done**: shared server-side catalog cache between tabs (each tab fetches its own), environment stamp push,
  production bundle and image, node validity badge, undo/redo.
- `javafx-base` is a dependency of the neutral modules for observable properties; it resolves to the classifier of the
  build platform, so build on the platform that runs the application.
