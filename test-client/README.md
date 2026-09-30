# Event Timing Engineering Client

The `test-client/` Maven project is the project's standalone **Engineering Client** for
interactive development, integration and diagnostics against the public boundaries of the
**Timing Point Application** (SI-01).

The directory/module name remains `test-client` for now. The engineering role and UI
baseline are defined in the meta-repository SDE:

- [50-SDE-03 — Engineering Client development and UI baseline](https://github.com/brainboxemb/2026-010-01.meta.event-timing-software/blob/main/docs/50-SDE-03-engineering-client.md)

This application is **not SI-02** and is not part of the Java-8/Pi SI-01 runtime. It is a
standalone desktop Maven project with no dependency on `event-timing-framework` or
`event-timing-app`. Repository co-location is intentional while SI-01 public interfaces
and the Engineering Client evolve together.

## Baseline

- Java 17
- JavaFX 21.0.10
- Maven Wrapper from the repository root
- Jackson 2.21.2 for independent JSON parsing
- independent client services for IF-03, Remote Shell and live diagnostics

The JavaFX Maven setup follows the normal OpenJFX Maven model: JavaFX modules and
platform-specific native libraries are resolved as Maven dependencies. The executable
entry point is `TestClientApplication`, a plain Java class. The actual JavaFX subclass
is kept internal as `TestClientFxApplication`; this prevents the JVM or an IDE from
treating the selected main class as a special JavaFX launcher target.

## Engineering boundaries

The Engineering Client communicates with SI-01 only through supported external
interfaces.

Current client services are:

```text
TestClientFxApplication
  +-- ApiClient             IF-03 HTTP / JSON
  +-- ApiEventClient        IF-03 WebSocket
  +-- RemoteShellClient     line-oriented Remote Shell
  +-- LiveLogClient         LoggingServer diagnostics socket
```

JavaFX event handlers do not own HTTP/WebSocket, shell or live-log protocol semantics.
The client must not import SI-01 implementation classes or mutate SI-01 domain state
directly.

## Run on Windows

With `JAVA_HOME` pointing to a JDK 17 installation:

```powershell
.\mvnw.cmd -f test-client\pom.xml clean javafx:run
```

Start SI-01 separately from the normal Java-8 project/NetBeans run configuration.
The default development endpoints are:

```text
HTTP       http://127.0.0.1:8081
WebSocket  ws://127.0.0.1:8082/api/v1/events
Shell      127.0.0.1:8023
Live logs  127.0.0.1:8030
```

Use:

- **Get Version** for `GET /api/v1/version`;
- **Get Status** for `GET /api/v1/status`;
- **Events → Connect** for `WS /api/v1/events`;
- **Terminal → Connect** for the line-oriented Remote Shell;
- **Logs → Connect** for the live diagnostics socket.

The window title includes the Engineering Client software version. **Help → About** shows
the client's own build identity (version, revision, source ref, build origin and source
state), independent of the SI-01 build information shown in the Status tab.

## Current UI

### Status

The **Status** tab shows selected parsed fields from the SI-01 build/status responses plus
the complete raw JSON. Keeping the raw response visible is intentional: interface changes
can be inspected before every field has a dedicated UI control.

### Events

The **Events** tab uses Java 17's built-in WebSocket client. It shows connection state,
event type/time, the first TimingNode identity/lifecycle and every raw event. Connecting or
reconnecting should immediately produce a complete `STATUS_SNAPSHOT`.

The current Step-3 application has no public status-changing command, so a normal manual
session does not yet produce `STATUS_CHANGED`.

### Terminal

The **Terminal** tab is a small built-in client for the A05 line-oriented Remote Shell.
It defaults to `127.0.0.1:8023`, has explicit Connect/Disconnect controls and uses a
black monospace terminal area. This is raw UTF-8 TCP for the project's development shell;
it is intentionally not an SSH/Telnet terminal emulator.

### Logs

The **Logs** tab is an engineering-only live diagnostics client. SI-01 remains the
listener and this tool initiates the TCP connection. New runtime log records are shown
live; the selected global runtime level can be queried/changed temporarily.

That override is process state only and is not written back to `application.yml`. The
live stream is separate from IF-03 status/events and does not provide retained history.

## Step-4 direction

Step 4 uses this Engineering Client as the primary manual inspection tool. The optional
lightweight browser/web test client is **not** being introduced in this step.

The working UI direction adds enough inspection/control to exercise the Step-4 slice:

- TimingSystem/TimingNode selection when several are present;
- TimingNode lifecycle/status;
- StageStartTimes;
- NextUpTeams;
- RaceData/reference state;
- registration/LogBook history;
- TimingData identity/records;
- upstream engineering capabilities and DebugConnector test control.

These items are a UI direction, not a frozen API contract. Final IF-03 resources and
UpstreamProtocol message schemas are defined by the applicable Step-4 interface/protocol
documents after the use cases and existing web-application compatibility have been
reviewed.

### API-controlled DebugConnector

The Engineering Client is expected to use IF-03 as the **control plane** for upstream
simulation.

Conceptually:

```text
Engineering Client
       |
       | IF-03 capability / test control
       v
SI-01 DebugConnector
       |
       v
UpstreamGateway
       |
       v
UpstreamProtocol
       |
       v
TimingSystem / TimingNode
```

The client does not write directly into RaceData, StageStartTimes or other domain state.
A test message must enter through DebugConnector and the normal upstream semantic path.

DebugConnector may be the only upstream connector in a development composition or may
coexist with a real connector such as RabbitMqConnector. This makes targeted synthetic
input useful even while production-shaped messaging is active.

The Engineering Client should first query public capabilities and enable test controls
only when the running SI-01 advertises that the corresponding engineering capability is
supported and enabled.

## Documentation screenshots

The planned documentation workflow uses a deterministic Engineering Client
**documentation/demo mode** with public synthetic fixture data.

The intended CI flow is:

```text
GitHub Actions
  +-- JDK 17 / pinned JavaFX
  +-- virtual display when required
  +-- deterministic documentation fixture
  +-- render named JavaFX view
  +-- application-owned scene/window snapshot
  +-- retain PNG as generated documentation evidence
```

This is intentionally not generic desktop mouse/keyboard automation. A JavaFX-owned
snapshot can wait until the scene is rendered and does not depend on window-manager
coordinates.

The first screenshot proof should stay small; likely candidates are Status, a Step-4
Timing view and an Upstream/DebugConnector view. Screenshot generation is not implemented
by this documentation change.

## Verify

```powershell
.\mvnw.cmd -f test-client\pom.xml verify
```

API HTTP/WebSocket, live-log and remote-shell client logic remain outside the JavaFX event
handlers so the UI does not become the owner of protocol semantics. The API client code
remains independent of SI-01 implementation classes, matching the headless black-box
client boundary.
