# 2026-010-02.java.event-timing-framework

Public Java framework for reusable event timing and time-registration applications.

Project-wide planning, requirements, architecture, interface design and verification coordination live in the companion meta repository: [**2026-010-01.meta.event-timing-software**](https://github.com/brainboxemb/2026-010-01.meta.event-timing-software).

## Current scope

This repository is the public implementation repository for **SI-01 — Headless Timing Application**. SIP Step 2 is complete; Step 3 is active on the `0.2.2-SNAPSHOT` development line. The current implementation still contains only the small reusable framework/application baseline while Step 3 adds the first external configuration and TimingNode behaviour incrementally. HTTP/WebSocket, RFID, CAN, display and backoffice capability are introduced only when their Step-3/later slices require them.

## Artifact and package model

Architectural responsibilities are not automatically Maven artifacts.

The initial reactor deliberately contains only two product deliverables:

```text
framework/   event-timing-framework   reusable library
app/         event-timing-app         runnable/default application
```

The root `event-timing-parent` POM is build/aggregation metadata rather than a deployed product component.

The reusable `event-timing-framework` JAR is organised by logical responsibility, but a
layer/package is not represented by a runtime marker object merely to make the source tree mirror
the architecture diagram.

Current real framework behaviour is deliberately small:

```text
io.github.brainboxemb.eventtiming.application.ApplicationStatus
io.github.brainboxemb.eventtiming.application.CommandHandler
io.github.brainboxemb.eventtiming.domain.timing.TimingNode
io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId
io.github.brainboxemb.eventtiming.infra.BuildIdentity
io.github.brainboxemb.eventtiming.presentation.console.LocalConsole
```

`application` owns the shared client-facing request boundary. `infra` owns build/runtime
provenance. Further package responsibilities such as `domain`, `core`, `presentation`, `io`
and `platform` are introduced only when real classes require those boundaries.
Empty `*Layer` marker classes and pre-modelled future status objects are deliberately not kept as
architecture evidence.

The executable lives separately under:

```text
io.github.brainboxemb.eventtiming.app
```

The application has a deliberately minimal `NEW -> RUNNING -> STOPPED` executable lifecycle and
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
application. Only the identity has a real configuration consumer yet:

```yaml
timingNodeId: timing-node-01
```

A synthetic development example is kept at `config/application.yml`. After building, start the
configured application with:

```bash
java -jar app/target/event-timing-app-<version>.jar config/application.yml
```

The configured process stays running until the JVM receives a normal shutdown request. On a
development terminal, **Ctrl+C** remains a normal stop route on Windows and Linux; the JVM shutdown
hook closes the application through the same lifecycle path used by tests.

The local console is available while the configured application is running:

```text
help      show available commands
version   show application/build version
status    show current application state and TimingNodeId
quit      stop the application cleanly
exit      alias for quit
```

The temporary no-argument startup remains only for the existing artifact smoke check.
Presentation settings, multiple TimingNodes, platform/profile overlays and I/O configuration are
added only when their SIP activities provide a real consumer.

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

Use `update-repo.ps1` / `update-repo.sh` for a controlled dependency-alignment pass after changing refs in `project.yml`. The generic tool refuses to overwrite local changes inside a managed dependency.

### A04 Windows / NetBeans acceptance check

From a clean Windows checkout, run `.\bootstrap.ps1` and open the repository root in NetBeans as
the Maven project.

On first open, NetBeans may perform a **priming build** to resolve the reactor/dependencies. That
Maven preparation can compile and run tests; it is not the application Run action.

The repository contains `nbactions.xml` so **Run Project** on the root Maven project first
installs the current reactor sources with tests skipped, then starts the executable `app/`
module with `config/application.yml`. This ensures the app uses the sibling framework from the
same checkout rather than an older local SNAPSHOT. The root POM remains build/aggregation metadata
and is not made into an executable application.

Use **Run Project**, then enter:

```text
help
version
status
quit
```

`help` must list every supported local command, `version` and `status` must return the shared
application values, and `quit` must terminate the process through the normal graceful shutdown
path.

The command-line equivalent remains:

```powershell
.\mvnw.cmd verify
java -jar app\target\event-timing-app-0.2.2-SNAPSHOT.jar config\application.yml
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

The executable loads its application/build identity from a Maven-filtered resource, starts its minimal lifecycle, reaches `RUNNING`, and then shuts down to `STOPPED`. The embedded identity deliberately separates the software version from the concrete build provenance:

```text
application version   Maven ${project.version}, for example 0.2.0-SNAPSHOT during development or 0.1.0 for a release
source revision       full Git commit captured at build time
build timestamp       UTC/ISO-8601 wall-clock build time
```

`pl.project13.maven:git-commit-id-plugin:4.9.10` supplies the Git revision and build time during the Maven `initialize` phase; normal resource filtering then packages only the values the runtime needs. The plugin version is intentionally pinned because it remains compatible with the Java 8 build baseline. The executable bootstrap reads those packaged values into its `BuildIdentity`; the framework value itself never knows about the resource file or a working Git checkout.

A Git tag does **not** silently determine or override the application version. If the POM still contains a `-SNAPSHOT` version, building a commit tagged as a release still reports that snapshot version. A valid release deliberately aligns Maven version, CHANGELOG release section and Git tag.

The wall-clock build timestamp is intentionally useful for distinguishing different snapshot binaries built from the same version line. If bit-for-bit reproducible release artifacts later become a requirement, the release process can instead adopt a fixed/commit-derived Maven `project.build.outputTimestamp` policy.

Lifecycle diagnostics use the selected logging composition:

```text
event-timing-framework  -> SLF4J API only; no logging provider selected
event-timing-app        -> SLF4J API + slf4j-jdk14 -> java.util.logging
```

`java.util.logging` writes the lifecycle INFO records through the runtime logging backend. Startup logging includes the concrete Git revision and build timestamp. The stable stdout smoke line used by CI intentionally remains independent of build-specific provenance:

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
