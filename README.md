# 2026-010-02.java.event-timing-framework

Public Java framework for reusable event timing and time-registration applications.

Project-wide planning, requirements, architecture, interface design and verification coordination live in the companion meta repository: [**2026-010-01.meta.event-timing-software**](https://github.com/brainboxemb/2026-010-01.meta.event-timing-software).

## Current scope

This repository is the public implementation repository for **SI-01 — Timing Point Application**. `v0.2.2` is the accepted SIP Step-3 application/API foundation baseline; normal development continues on `0.2.3-SNAPSHOT` while Step 4 begins. The baseline provides external YAML configuration, long-running process lifecycle, shared local/remote terminal semantics, IF-03 HTTP/WebSocket version and status, runtime logging/live diagnostics, and automated separate-process system verification. Timing-domain behaviour plus RFID, CAN, display and backoffice integrations remain later-step work.

## Artifact and package model

Architectural responsibilities are not automatically Maven artifacts.

The default reactor contains three product artifacts. The small TimingData API is
an independently reusable IF-05 model/SPI artifact because both SI-01 and the
standalone Engineering Client are real consumers. The verification-only
`system-test` module is added only when the explicit Maven `system-test`
profile is selected:

```text
timing-data-api/  event-timing-data-api   shared Java-8 IF-05 model/SPI
framework/        event-timing-framework  reusable SI-01 library
app/              event-timing-app        runnable/default application
system-test/      event-timing-system-test black-box verification only (profile-only)
```

`system-test` has no Java dependency on the product artifacts. It starts the built app JAR as
a separate JVM process and verifies only external interfaces. It is not a release/publication
artifact. The root `event-timing-parent` POM is build/aggregation metadata rather than a deployed
product component.

The `io.github.brainboxemb.eventtiming` namespace denotes the software-system/product family; reusable SI-01 code is rooted under `io.github.brainboxemb.eventtiming.timingpoint` because SI-01 is the software running locally at a timing observation point. `TimingNode` remains a logical domain aggregate inside that application and is not the package root.

The reusable `event-timing-framework` JAR is organised by logical responsibility, but a
layer/package is not represented by a runtime marker object merely to make the source tree mirror
the architecture diagram.

Current real framework behaviour is deliberately small:

```text
io.github.brainboxemb.eventtiming.timingpoint.application.ApplicationStatus
io.github.brainboxemb.eventtiming.timingpoint.application.CommandHandler
io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode
io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeId
io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity
io.github.brainboxemb.eventtiming.timingpoint.infra.bootstrap.ApplicationBootstrap
io.github.brainboxemb.eventtiming.timingpoint.infra.bootstrap.config.ApplicationConfig
io.github.brainboxemb.eventtiming.timingpoint.runtime.TimingApplication
io.github.brainboxemb.eventtiming.timingpoint.runtime.TimingApplicationLifecycle
io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.console.LocalConsole
io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.shell.RemoteShellServer
io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.api.HttpEndpoint
io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.api.WebSocketEndpoint
io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.api.MessageWriter
io.github.brainboxemb.eventtiming.timingpoint.presentation.common.terminal.TerminalSession
```

`application` owns the shared client-facing request boundary. `infra` owns build/runtime
provenance. Further package responsibilities such as `domain`, `core`, `presentation`, `io`
and `platform` are introduced only when real classes require those boundaries.
Empty `*Layer` marker classes and pre-modelled future status objects are deliberately not kept as
architecture evidence.

The reusable framework owns the runtime and cross-cutting bootstrap model:

```text
io.github.brainboxemb.eventtiming/
  runtime/
    TimingApplication
    TimingApplicationLifecycle
  infra/
    bootstrap/
      ApplicationBootstrap
      config/
        ApplicationConfig
        PresentationConfig
        RemoteShellConfig
        ApiConfig
        ApiHttpConfig
        ApiWebSocketConfig
```

The executable artifact remains thin. Launcher/input adapters stay under `...eventtiming.timingpoint.app`;
reusable logging infrastructure lives in the framework artifact, while the executable selects the SLF4J provider:

```text
io.github.brainboxemb.eventtiming.timingpoint.app/
  TimingApplicationMain

io.github.brainboxemb.eventtiming.timingpoint.infra/
  BuildIdentity
  EmbeddedBuildIdentityLoader
  bootstrap/config/
    YamlApplicationConfigLoader

io.github.brainboxemb.eventtiming.timingpoint.infra.logging/
  Logging
  LoggingConfig
  LoggingLevel
  LoggingFileConfig
  LoggingControl
  TimestampedFileLogHandler
  CompactLogFormatter

io.github.brainboxemb.eventtiming.timingpoint.infra.loggingserver/
  LoggingServer
  LoggingServerConfig
  LiveLogHandler
```

`Logging` owns JUL/backend, retained file/console sink composition and runtime level control. `LoggingServer` is a separate infrastructure component/package that owns the optional client-facing live-log socket. The executable composes both; `Logging` does not construct or own `LoggingServer`. These reusable classes live in `event-timing-framework`; the
framework still selects no SLF4J provider. The default executable supplies `slf4j-jdk14` at
runtime and starts/stops the framework-provided logging component.

`ApplicationBootstrap` consumes the validated framework configuration model and
owns concrete composition plus presentation/startup wiring. The default IF-11 YAML parser/mapping and embedded build-identity interpretation are framework
infrastructure. The executable remains responsible only for supplying the configuration path and
its filtered `event-timing-build.properties` resource; SnakeYAML is therefore a framework
implementation dependency. The application has a deliberately minimal `NEW -> RUNNING -> STOPPED` executable lifecycle and
now composes the first shared client boundary, `CommandHandler`, for the authoritative version
query. Future timing-domain capability is added only when its use case is implemented.

### Artifact rule

A package or architecture layer is **not** a publication boundary by itself. Introduce another Maven library artifact only when a real reason exists, such as:

- another application needs to consume it independently;
- an optional integration brings a significant independent dependency/lifecycle boundary;
- deployment, ownership or release/versioning requires separation;
- public/private implementation boundaries require independent composition.

This keeps logical responsibilities inside the one framework artifact while real package boundaries emerge from implemented behaviour. A later capability such as RabbitMQ, a platform-specific implementation, or reusable test support can be split only when its consumer and boundary are concrete.

### Derived applications

The framework is intended to support more than one executable composition. Examples that may later become separate applications include:

```text
single-TimingNode application compose exactly one TimingNode
multi-TimingNode application  compose and coordinate 1..N TimingNode objects
```

Those applications should reuse the same framework library and inject/select their own concrete components. New Step-3 configuration/domain types use `TimingNode` / `TimingNodeId` directly; the repository does not introduce legacy `Waypoint` / `UniqueID` compatibility names.

The working design is coordinated in the meta repository, especially `docs/31-01-SDD-02-java-component-design.md`.

### Current Step-3 application

The executable uses one external YAML file for the single TimingNode currently composed by the
application. Step 3 now configures the implemented presentation listeners explicitly:

```yaml
timingNodeId: timing-node-01

presentation:
  remoteShell:
    bindAddress: 127.0.0.1
    port: 8023
  api:
    http:
      bindAddress: 127.0.0.1
      port: 8081
    webSocket:
      bindAddress: 127.0.0.1
      port: 8082
```

A08 also configures cross-cutting runtime logging independently from presentation/status:

```yaml
logging:
  level: INFO
  file:
    path: logs
    rotateBytes: 1048576
    retainedFiles: 5
  live:
    bindAddress: 127.0.0.1
    port: 8030
```

The framework still logs only through SLF4J. Executable infrastructure `Logging` maps the
semantic startup level to `slf4j-jdk14 -> java.util.logging`, applies the same compact formatter
to console output, and writes retained rotating file logs under timestamped names such as
`20250514-101657.txt`. Retained file records use
`HH:mm:ss.SSS - [LEVEL] - message - [sourceClass.sourceMethod]`, while the executable optionally
exposes a dedicated best-effort live-log TCP listener. The JavaFX
`LoggingServer` owns that live socket; the engineering client initiates the connection and its
**Logs** tab can inspect new records and
temporarily change the process-wide log level. A runtime level change is not persisted to YAML and
restart restores the configured level. The live diagnostics stream is separate from IF-03
`/api/v1/events`.

The remote shell is a small line-oriented TCP development/service endpoint. It is **not** an SSH
or Telnet protocol implementation. IF-03 is the general **API**; A06/A07 implement its first
HTTP version/status and WebSocket event slice:

```text
GET /api/v1/version                         http://127.0.0.1:8081
GET /api/v1/status                          http://127.0.0.1:8081
WS  /api/v1/events                          ws://127.0.0.1:8082
```

The WebSocket adapter sends a complete `STATUS_SNAPSHOT` immediately after connect/reconnect.
`STATUS_CHANGED` is reserved for real authoritative status changes; the current Step-3
TimingNode remains `CLOSED`, so no synthetic change is generated merely to exercise the
transport.

API HTTP and WebSocket are grouped under `presentation.api` because they are two
transports of the same functional interface. A future browser/iPad `presentation.web` interface
may have its own HTTP/WebSocket endpoints without sharing the API namespace.

The committed development example keeps all network presentation listeners loopback-only;
binding to another interface must be a deliberate configuration change.

A synthetic development example is kept at `config/application.yml`. After building, start the
configured application with:

```bash
java -jar app/target/event-timing-app-<version>.jar config/application.yml
```

The configured process stays running until the JVM receives a normal shutdown request. On a
development terminal, **Ctrl+C** remains a normal stop route on Windows and Linux; the JVM shutdown
hook closes the application through the same lifecycle path used by tests.

The local console and A05 remote terminal use the same command session:

```text
help      show available commands
version   show application/build version
status    show current TimingNode identity and lifecycle
quit      stop the application cleanly
exit      alias for quit
```

A remote client disconnect ends only that terminal session. A later connection can reconnect to
the same listener. `quit` / `exit` intentionally retain the same meaning as the local console and
request graceful application shutdown.

The temporary no-argument startup remains only for the existing artifact smoke check. Multiple
TimingNodes, further presentation endpoints, platform/profile overlays and I/O configuration are
added only when their SIP activities provide a real consumer.

### Engineering Client

`test-client/` is the standalone Java 17 / JavaFX **Engineering Client** used for manual integration, diagnostics and public-interface inspection. It remains engineering tooling rather than SI-02 and deliberately has no dependency on SI-01 implementation classes.
It is deliberately not part of the Java-8 SI-01 Maven reactor and has no dependency on
`event-timing-framework` or `event-timing-app`.

With JDK 17 selected:

```powershell
.\mvnw.cmd -f test-client\pom.xml javafx:run
```

The **Status** tab defaults to `http://127.0.0.1:8081` and provides **Get Version** and
**Get Status** with parsed fields plus raw JSON. The A07 **Events** tab defaults to
`ws://127.0.0.1:8082/api/v1/events` and displays the connection state, latest parsed event and
raw event stream. The **Terminal** tab connects directly to the A05 development shell on
`127.0.0.1:8023`, so manual shell verification does not require a separate PuTTY session.
See `test-client/README.md`.


## Local checkout and project tooling

The repository uses two reusable tooling layers:

```text
tools/tool.git-project   generic Git lifecycle/bootstrap and affected analysis
tools/tool.java-project  Java/Maven canonical build/test/evidence tooling
```

`tool.git-project` is pinned directly by its committed gitlink. `project.yml` declares `tool.java-project` as a managed tooling dependency and points to `project.java.yml` for Java-specific configuration. The reviewed Migration-006 baseline is documented in `docs/tooling-baseline.md`.

A normal clone does not require `--recurse-submodules`.

Windows:

```powershell
git clone https://github.com/brainboxemb/2026-010-02.java.event-timing-framework.git
cd 2026-010-02.java.event-timing-framework
.\bootstrap.ps1
.\mvnw.cmd verify
```

Linux/POSIX shell:

```bash
git clone https://github.com/brainboxemb/2026-010-02.java.event-timing-framework.git
cd 2026-010-02.java.event-timing-framework
./bootstrap.sh
./mvnw verify
```

The default root `verify` builds the product reactor and runs its normal tests, but does **not**
launch the separate application process. Deliberate full system verification is explicit:

```powershell
.\mvnw.cmd verify -Psystem-test
```

```bash
./mvnw verify -Psystem-test
```

That profile adds `system-test` after the application JAR has been packaged. The test launches
the JAR as a child JVM with temporary loopback ports, verifies IF-03 `/version`, `/status`,
WebSocket snapshot/reconnect behaviour, and then shuts the process down through the remote
terminal `quit` command. The verifier does not import framework/application classes, so this
remains a process-level black-box check rather than another in-process component test.

Use `update-repo.ps1` / `update-repo.sh` for a controlled dependency-alignment pass after changing refs in `project.yml`. The generic tool refuses to overwrite local changes inside a managed dependency.

### A04 Windows / NetBeans acceptance check

From a clean Windows checkout, run `.\bootstrap.ps1` and open the repository root in NetBeans as
the Maven project.

On first open, NetBeans may perform a **priming build** to resolve the reactor/dependencies. That
Maven preparation can compile and run tests; it is not the application Run action.

The repository contains `nbactions.xml` so **Build Project** uses `install` and
**Clean and Build Project** uses `clean install`, both with `maven.test.skip=true`. Those IDE
build actions are intentionally fast and do not compile or run tests.

**Run Project** on the root Maven project first installs the current product reactor sources with
tests skipped, then starts the executable `app/` module with `config/application.yml`. This
ensures the app uses the sibling framework from the same checkout rather than an older local
SNAPSHOT. The root POM remains build/aggregation metadata and is not made into an executable
application.

Use **Run Project** (or **Debug Project** when debugging), then enter:

```text
help
version
status
quit
```

`help` must list every supported local command, `version` and `status` must return the shared
application values, and `quit` must terminate the process through the normal graceful shutdown
path.

Run and Debug use the same configured application path; Debug only adds the NetBeans JPDA debugger.

The command-line split is:

```powershell
# Normal product verification: framework/app tests, no separate process launch
.\mvnw.cmd verify

# Deliberate VC-ST1-001 black-box verification
.\mvnw.cmd verify -Psystem-test

java -jar app\target\event-timing-app-0.2.3-SNAPSHOT.jar config\application.yml
```

## Toolchain baseline

```text
Java              Eclipse Temurin 8u504-b01 (`8.0.504+1` in CI)
Java source/API    Java SE 8
Maven              3.9.16
Maven Wrapper      3.3.4
Moon               2.5.4 through `tool.git-project`
```

The Java-specific baseline is recorded in `project.java.yml`. Maven remains the authoritative project-version and Java build/test source. Moon does not implement or cache the Maven lifecycle; it only declares which repository changes affect the canonical Java capability and which narrower changes require native full-Windows qualification.

## Minimal Step-2 application lifecycle

After a reactor build, run the executable application using the version from the root `pom.xml`:

```bash
java -jar app/target/event-timing-app-<version>.jar
```

The executable loads its application/build identity from a Maven-filtered resource, starts its minimal lifecycle, reaches `RUNNING`, and then shuts down to `STOPPED`. The embedded identity deliberately separates software identity from deterministic source/build provenance:

```text
application    event-timing-app
version        Maven ${project.version}
revision       exact Git commit
sourceRef      branch, tag or CI ref
buildOrigin    local or github-actions
dirty          true when uncommitted source changes were present
apiVersion     IF-03 major version
```

`pl.project13.maven:git-commit-id-plugin:4.9.10` supplies the Git revision, local source ref and dirty-state during Maven `initialize`; GitHub Actions supplies the CI source ref/origin through stable environment context. Normal resource filtering packages only those values the runtime needs. The executable bootstrap reads them into `BuildIdentity`; the framework value itself never knows about the resource file or a working Git checkout.

Wall-clock build time, CI run/build id and actor/user are deliberately **not** embedded. Repeating a build with the same version/revision/ref/origin/dirty inputs must not become a different artifact merely because it ran at another time or under another run id.

A Git tag does **not** silently determine or override the application version. If the POM still contains a `-SNAPSHOT` version, building a commit tagged as a release still reports that snapshot version. A valid release deliberately aligns Maven version, CHANGELOG release section and Git tag.

Lifecycle diagnostics use the selected logging composition:

```text
event-timing-framework  -> SLF4J API + reusable JUL logging infrastructure; no SLF4J provider selected
event-timing-app        -> selects slf4j-jdk14 -> java.util.logging
```

`java.util.logging` writes the lifecycle INFO records through the runtime logging backend. Startup logging includes the concrete Git revision, source ref, build origin and dirty-state. The stable stdout smoke line used by CI intentionally remains independent of build-specific provenance:

```text
event-timing-app lifecycle OK version=<version> state=STOPPED
```

This short-lived process is intentional for Step 2. Long-running service behaviour and public version/status transports belong to later SIP steps.

## Production CI and test evidence

This repository consumes released `tool.git-project v0.2.8` and `tool.java-project v0.3.2`. The exact Java owner commit is `c0ca2e1365a64bc626ca331a8170d13340ae0b36`; exact generic Git provenance is recorded in `docs/tooling-baseline.md`.

The consumer owns only product metadata/checks, trigger policy, impact declarations and artifact names. Shared Java execution is:

```text
product metadata + logging-boundary check
        ↓
exact base-to-head Java affected preflight
        ↓
        ├─ unrelated -> stop before JDK/Maven/Windows/publication
        │
        └─ affected -> one Linux canonical Maven Wrapper `verify`
                       + selected Windows qualification
        ↓
prepared canonical `bld` tree
        ↓
generated-output publication without rebuilding Maven output
```

`moon.yml` contains two impact-only capabilities:

```text
java.canonical      changes that require canonical Java execution
java.windows-full   narrower build/toolchain/workflow/platform-sensitive changes
```

The Java/Moon decision does not run the build. `tool.java-project` owns the one canonical Linux Maven producer, Maven dependency caching, Surefire evidence, toolchain/build provenance and generated-output finalization.

Windows qualification is selected by event and impact:

```text
pull request, ordinary Java impact       auto -> smoke
pull request, build/tooling impact       auto -> full
ordinary protected main publication     none
manual non-tag qualification             explicit, default full
exact release-tag qualification          full
```

`smoke` runs the exact Linux-produced application JAR on Windows without a second Maven build. `full` adds an independent native Windows Maven `verify`; that native Windows build can start in parallel with the Linux canonical producer after preflight, while exact-artifact smoke waits for Linux output. A normal protected-main publication deliberately does not allocate Windows again after the pull request has already qualified the change.

The canonical Linux producer stages both product JARs:

```text
artifacts/
  event-timing-framework-<version>.jar
  event-timing-app-<version>.jar
```

Producer evidence remains under:

```text
evidence/
  executions/java-canonical/
    execution.json
    execution.log
  tests/
  toolchain-build-provenance.txt
```

Current orchestration evidence is retained separately rather than rewriting producer evidence:

```text
orchestration/
  preflight/
    decision.json
    preflight.log
    affected/
  timing.json
  timing.md
```

`timing.md`/`timing.json` record actual GitHub job/step timings and Maven-reported time so Java work can be distinguished from runner, checkout, setup and artifact-transfer overhead.

The canonical Maven `verify` intentionally excludes the black-box `system-test` profile.
Pull-request CI therefore runs normal product tests without launching VC-ST1-001 on every commit.
After an affected change is integrated into protected `main`, a repository-owned Linux job runs
`verify -Psystem-test` explicitly. Exact release-tag qualification runs the same explicit profile
on both Linux and Windows; both Surefire result sets are copied into the final release evidence
bundle.

Generated output is published as:

```text
pull request #N -> dev/pr-N/bld
main            -> prod/bld
release tag     -> rel/vX.Y.Z/bld
```

Closing a pull request removes only its `dev/pr-N/bld` preview through the released generic cleanup workflow. `prod/bld` and release output are not affected.

## Release workflow

Development normally uses a Maven `-SNAPSHOT` version. A software release is prepared through a normal reviewed PR that:

- changes the complete Maven reactor to the intended non-SNAPSHOT Maven version;
- moves the relevant `CHANGELOG.md` content into a matching release section;
- passes the normal affected PR qualification.

After that release-preparation commit is merged to protected `main`, the normal main workflow performs the canonical Linux build/publication with Windows disabled. If release metadata is valid, it creates the immutable `v<version>` tag on that exact commit and explicitly dispatches the same workflow on the tag.

The exact tag is the release qualification boundary. Tag verification always performs:

```text
exact tagged Linux canonical Maven build
native Windows Maven verify       (parallel with Linux)
explicit VC-ST1-001 profile verify on Linux
explicit VC-ST1-001 profile verify on Windows
exact Linux-produced app JAR smoke on Windows
rel/vX.Y.Z/bld publication
product artifact/build-identity validation
GitHub Release asset publication
```

A tagged release build must satisfy all of the following:

```text
Maven version       X.Y.Z
CHANGELOG heading   ## X.Y.Z — <date>
Git tag             vX.Y.Z
BuildIdentity       version=X.Y.Z
BuildIdentity       revision=<tagged commit SHA>
```

Release assets contain:

- `event-timing-framework-X.Y.Z.jar`;
- `event-timing-app-X.Y.Z.jar`;
- SHA-256 checksums;
- a compressed evidence bundle containing build provenance, tests and orchestration evidence.

`prod/bld` remains the browsable output of `main`; tagged qualification publishes separately to `rel/vX.Y.Z/bld`. If exact tag qualification fails, the repository preserves the failed candidate through its existing `vX.Y.Z-failed` archival semantics rather than silently reusing the version.

After a successful release, a separate normal PR advances `main` to the next planned `-SNAPSHOT` version. Release CI never rewrites the development version behind the review workflow.

## Development workflow

Changes use issue → feature branch → draft PR → implementation/test/evidence → review → merge.

Keep real deployment identities, proprietary protocols, credentials, encryption keys and production mappings out of this public repository.

Docker is not required for the normal Java build/unit-test path. It may be introduced later for integration tests that need real external services. Long-term dependency preservation/offline rebuilding is coordinated separately in the meta-project rather than assuming the normal online Maven path will remain available forever.
