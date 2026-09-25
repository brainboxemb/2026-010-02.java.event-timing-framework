# Event Timing Test Client

Small development/test UI for inspecting the public SI-01 IF-03 interface.

This is **not SI-02** and is not part of the Java-8/Pi runtime. It is a standalone
desktop Maven project that deliberately has no dependency on `event-timing-framework`
or `event-timing-app`.

## Baseline

- Java 17
- JavaFX 21.0.10
- Maven Wrapper from the repository root
- Jackson 2.21.2 for independent JSON parsing

The JavaFX Maven setup follows the normal OpenJFX Maven model: JavaFX modules and
platform-specific native libraries are resolved as Maven dependencies. The executable entry point is `TestClientApplication`, a plain Java class. The actual
JavaFX subclass is kept internal as `TestClientFxApplication`; this prevents the JVM
or an IDE from treating the selected main class as a special JavaFX launcher target.

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
```

Use:

- **Get Version** for `GET /api/v1/version`;
- **Get Status** for `GET /api/v1/status`;
- **Events → Connect** for `WS /api/v1/events`.

The window title includes the test-client software version. **Help → About** shows the test client's own build identity (version, revision, source ref, build origin and source state), independent of the SI-01 build information shown in the Status tab.

The **Status** tab shows selected parsed fields and the complete raw JSON response.

The **Events** tab uses Java 17's built-in WebSocket client. It shows connection state,
event type/time, the first TimingNode identity/lifecycle and every raw event. Connecting or
reconnecting should immediately produce a complete `STATUS_SNAPSHOT`. The current Step-3
application has no public status-changing command, so a normal manual session does not yet
produce `STATUS_CHANGED`.

The **Terminal** tab is a small built-in client for the A05 line-oriented remote shell.
It defaults to `127.0.0.1:8023`, has explicit Connect/Disconnect controls and uses a
black monospace terminal area. This is raw UTF-8 TCP for the project's development
shell; it is intentionally not an SSH/Telnet terminal emulator.

## Verify

```powershell
.\mvnw.cmd -f test-client\pom.xml verify
```

HTTP, WebSocket and remote-shell client logic remain outside the JavaFX event handlers so the
development UI does not become the owner of protocol semantics.
