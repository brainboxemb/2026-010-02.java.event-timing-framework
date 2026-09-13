# 2026-010-02.java.event-timing-framework

Public Java framework for reusable event timing and time-registration applications.

Project-wide planning, requirements, architecture, interface design and verification coordination live in the companion meta repository: [**2026-010-01.meta.event-timing-software**](https://github.com/brainboxemb/2026-010-01.meta.event-timing-software).

## Current scope

This repository is the public implementation repository for **SI-01 — Headless Timing Application**. The current Step-2 baseline establishes a reusable framework-library boundary, a first executable consumer, deterministic repository/build/test tooling, and a minimal explicit application lifecycle. It intentionally does not implement HTTP/WebSocket, RFID, CAN, display, backoffice or timing-domain behaviour yet.

## Artifact and package model

Architectural responsibilities are not automatically Maven artifacts.

The initial reactor deliberately contains only two product deliverables:

```text
framework/   event-timing-framework   reusable library
app/         event-timing-app         runnable/default application
```

The root `event-timing-parent` POM is build/aggregation metadata rather than a deployed product component.

The reusable `event-timing-framework` JAR contains the initial responsibility-oriented Java package structure:

```text
io.github.brainboxemb.eventtiming.domain     application/domain model, rules and services
io.github.brainboxemb.eventtiming.core       reusable runtime engine and orchestration
io.github.brainboxemb.eventtiming.platform   execution-platform/environment abstractions
io.github.brainboxemb.eventtiming.comm       communication contracts and reusable communication concerns
```

The executable lives separately under:

```text
io.github.brainboxemb.eventtiming.app
```

The framework still contains small marker classes used to prove the initial package structure. The application now has a real but deliberately minimal `NEW -> RUNNING -> STOPPED` lifecycle; neither defines future timing-domain capability merely to make the skeleton look complete.

### Artifact rule

A package or architecture layer is **not** a publication boundary by itself. Introduce another Maven library artifact only when a real reason exists, such as:

- another application needs to consume it independently;
- an optional integration brings a significant independent dependency/lifecycle boundary;
- deployment, ownership or release/versioning requires separation;
- public/private implementation boundaries require independent composition.

This keeps `domain`, `core`, `platform` and `comm` together while the structure is being proven. A later capability such as RabbitMQ, a platform-specific implementation, or reusable test support can be split only when its consumer and boundary are concrete.

### Derived applications

The framework is intended to support more than one executable composition. Examples that may later become separate applications include:

```text
single-system application   compose exactly one TimingSystemInstance
multi-system application    compose and route 1..X TimingSystemInstance objects
```

Those applications should reuse the same framework library and inject/select their own concrete components. They are not created during Step 2 merely to predict future structure.

The working design is coordinated in the meta repository, especially `docs/31-01-SDD-02-java-component-design.md`.

## Local checkout and project tooling

The repository uses two reusable tooling layers:

```text
tools/tool.git-project   generic Git externals/bootstrap/update handling
tools/tool.java-project  Java/Maven build/test/CI tooling
```

`tool.git-project` is pinned directly by its committed gitlink. `project.yml` then declares `tool.java-project` as a managed tooling dependency and points to `project.java.yml` for Java-specific configuration.

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

## Toolchain baseline

```text
Java              Eclipse Temurin 8u504-b01 (`8.0.504+1` in CI)
Java source/API    Java SE 8
Maven              3.9.16
Maven Wrapper      3.3.4
```

The Java-specific baseline is recorded in `project.java.yml`. The current reusable workflow still receives these values explicitly; the profile provides the local/project source for the Java-specific settings and can later become a validated workflow input source.

## Minimal Step-2 application lifecycle

After a reactor build, run the executable application with:

```bash
java -jar app/target/event-timing-app-0.1.0-SNAPSHOT.jar
```

The executable loads its application/build identity from a Maven-filtered resource, starts its minimal lifecycle, reaches `RUNNING`, and then shuts down to `STOPPED`. The embedded identity deliberately separates the software version from the concrete build provenance:

```text
application version   Maven ${project.version}, for example 0.1.0-SNAPSHOT
source revision       full Git commit captured at build time
build timestamp       UTC/ISO-8601 wall-clock build time
```

`pl.project13.maven:git-commit-id-plugin:4.9.10` supplies the Git revision and build time during the Maven `initialize` phase; normal resource filtering then packages only the values the runtime needs. The plugin version is intentionally pinned because it remains compatible with the Java 8 build baseline. `BuildIdentity` reads those packaged values and never consults a working Git checkout at runtime.

A Git tag does **not** silently determine or override the application version. If the POM still contains `0.1.0-SNAPSHOT`, building a commit tagged `v0.1.0` still reports `0.1.0-SNAPSHOT`. A valid release deliberately aligns Maven version, CHANGELOG release section and Git tag.

The wall-clock build timestamp is intentionally useful for distinguishing different snapshot binaries built from the same version line. If bit-for-bit reproducible release artifacts later become a requirement, the release process can instead adopt a fixed/commit-derived Maven `project.build.outputTimestamp` policy.

Lifecycle diagnostics use the selected logging composition:

```text
event-timing-framework  -> SLF4J API only; no logging provider selected
event-timing-app        -> SLF4J API + slf4j-jdk14 -> java.util.logging
```

`java.util.logging` writes the lifecycle INFO records through the runtime logging backend. Startup logging includes the concrete Git revision and build timestamp. The stable stdout smoke line used by CI intentionally remains independent of build-specific provenance:

```text
event-timing-app lifecycle OK version=0.1.0-SNAPSHOT state=STOPPED
```

This short-lived process is intentional for Step 2. Long-running service behaviour and public version/status transports belong to later SIP steps.

## Reusable CI and test evidence

This repository is the first real product consumer of both `brainboxemb/tool.git-project` and `brainboxemb/tool.java-project`.

CI first proves a clean checkout and root project-tool bootstrap on Linux and Windows, including exact tooling SHAs. It then calls the reusable Java workflow pinned to the same immutable `tool.java-project` commit declared in `project.yml`.

The CI proof includes:

- clean checkout before local tooling restoration on Linux and Windows;
- deterministic restoration of both tooling layers;
- Linux canonical full-reactor build/test and application artifact production;
- Windows full-reactor compatibility build/test;
- download and execution on Windows of the exact `event-timing-app` JAR produced by Linux;
- exact lifecycle/build-identity stdout verification for that canonical artifact;
- build/test provenance generated by the reusable Java toolchain.

The canonical Linux Surefire XML is also rendered into a readable aggregate report without running the tests a second time. The report contains overall, per-module and per-suite totals and links back to the retained raw Surefire XML/TXT evidence.

The same canonical Linux build prepares a browsable build-output tree containing both product JARs plus provenance/test evidence. Publication is handled by the separate reusable publisher from the same pinned `tool.java-project` revision:

```text
pull request #N -> dev/pr-N/bld
main            -> prod/bld
```

The generated branch contains build output/evidence only, not a source checkout or tooling repositories. Temporary Actions artifacts remain available for CI transfer and short-lived downloads.

## Release workflow

Development normally uses a Maven `-SNAPSHOT` version. A software release is prepared through a normal reviewed PR that:

- changes the reactor to the intended non-SNAPSHOT Maven version;
- moves the relevant `CHANGELOG.md` content into a matching release section;
- passes the same Java 8 Linux/Windows build, test and canonical-artifact smoke checks as normal development.

After that release-preparation commit is merged to `main`, the green main workflow creates the immutable `v<version>` tag on that exact commit. GitHub does not recursively start a workflow for a tag pushed with `GITHUB_TOKEN`, so the release job explicitly dispatches this same verification workflow at the new tag. The tagged revision is therefore checked independently rather than treating the pre-tag main build as sufficient release evidence.

A tagged release build must satisfy all of the following:

```text
Maven version       X.Y.Z
CHANGELOG heading   ## X.Y.Z — <date>
Git tag             vX.Y.Z
BuildIdentity       version=X.Y.Z
BuildIdentity       revision=<tagged commit SHA>
```

Only after the tag build has passed Linux/Windows verification and canonical-artifact smoke does CI create/update the GitHub Release. Release assets contain:

- `event-timing-framework-X.Y.Z.jar`;
- `event-timing-app-X.Y.Z.jar`;
- SHA-256 checksums;
- a compressed evidence bundle containing build provenance and the readable/raw unit-test evidence.

`prod/bld` remains the browsable output of `main`; tag verification does not overwrite it. After a successful release, a separate normal PR advances `main` to the next planned `-SNAPSHOT` version. Release CI never rewrites the development version behind the review workflow.

The first exercise of this process is intentionally a `0.0.1` trial release. The Step-2 maturity baseline will only become `0.1.0` after that trial has demonstrated the complete release path.

## Development workflow

Changes use issue → feature branch → draft PR → implementation/test/evidence → review → merge.

Keep real deployment identities, proprietary protocols, credentials, encryption keys and production mappings out of this public repository.

Docker is not required for the normal Java build/unit-test path. It may be introduced later for integration tests that need real external services. Long-term dependency preservation/offline rebuilding is coordinated separately in the meta-project rather than assuming the normal online Maven path will remain available forever.
