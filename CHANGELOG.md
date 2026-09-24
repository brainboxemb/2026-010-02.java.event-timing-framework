# Changelog

## Unreleased

- Inline embedded build-metadata reading into `TimingApplication`, make the executable composition directly own its `BuildIdentity`, and remove the standalone `BuildIdentityLoader` class; deployment configuration remains a separate Step-3 concern.

- Simplify the first-executable implementation around real behaviour: keep the minimal shared `CommandHandler.version()` boundary and compact `TimingApplication.Builder`, remove bootstrap-only `*Layer` markers and premature status-model objects, and move `BuildIdentity` to `infra` because build provenance is infrastructure rather than application/domain state.

- Route repository agent guidance through `brainboxemb.meta/AGENTS.md`, make dependency-owner AGENTS explicitly non-inherited, and keep implementation-specific architecture/boundary guidance local.

## 0.2.1 — 2026-09-17

- Publish the intended 0.2.x product baseline after the first tagged `0.2.0` candidate was archived as failed during final GitHub Release evidence packaging.
- Fix GitHub Release packaging so it consumes the finalized immutable `rel/vX.Y.Z/bld` tree, including durable `orchestration/**` evidence, rather than the pre-finalization Actions artifact.
- Introduce the shared `application` responsibility as the authoritative semantic home for application build identity and current status.
- Move executable build-metadata loading behind composition and map it into the reusable framework `BuildIdentity`, including IF-03 API major version `1`.
- Add immutable first-executable status snapshots with application state, minimal timing-system status and observable problem values, plus one application-owned current-status authority for later presentation adapters.
- Keep process hosting/shutdown, external settings and concrete console/remote/HTTP/WebSocket adapters deferred to their later SIP Step-3 activities.
- Adopt released `tool.java-project v0.3.2` and `tool.git-project v0.2.8` as the Migration-006 execution baseline.
- Replace repository-owned Linux/Windows/Moon build orchestration with shared Java production workflows while keeping Maven authoritative for the multi-module reactor.
- Keep Moon as `java.canonical` / `java.windows-full` impact declarations only and retain exact preflight/timing evidence in generated `bld` output.
- Use event-sensitive Windows qualification: pull requests select `auto`, ordinary protected-main publication uses `none`, and exact release-tag qualification uses `full`.
- Run native full-Windows Maven release qualification in parallel with the Linux canonical producer and run exact Linux-artifact smoke after the canonical artifact is available.
- Preserve repository-owned release metadata, embedded build-identity validation, logging dependency boundaries and both product release JARs.

## 0.2.0-failed — 2026-09-17

- Candidate `v0.2.0` at exact source `e8066c9e33cf2ed77bb7f37ac8a68707933e28e8` passed Linux canonical Maven, independent native Windows Maven and exact Linux-produced application-JAR smoke on Windows.
- Finalized `rel/v0.2.0/bld` publication also succeeded, but GitHub Release packaging incorrectly read the pre-finalization Actions artifact and failed because `orchestration/**` was not present there yet.
- The release fail-safe removed normal `v0.2.0` and preserved the consumed candidate as `v0.2.0-failed`; version `0.2.0` is not reused.

## 0.1.0 — 2026-09-13

- Establish `0.1.0` as the SIP Step-2 software baseline for the reusable framework and minimal runnable application.
- Consume the released `tool.java-project v0.1.0` toolchain baseline through `project.yml`, while keeping the committed gitlink and reusable workflow callers pinned to the exact immutable release commit.
- Record the released Java-tooling baseline and its external `template.java-project` conformance role in the framework documentation.
- Archive failed release candidates as `vX.Y.Z-failed`, remove the corresponding normal release tag, and never reuse a consumed release version; the next attempt advances the patch version and records the failed attempt in this changelog.

## 0.0.1 — 2026-09-13

- Bootstrap the SI-01 Maven reactor with one reusable `event-timing-framework` library and one runnable `event-timing-app` consumer.
- Keep `domain`, `core`, `platform`, and `comm` as package/architecture responsibilities inside the framework artifact rather than speculative separate libraries.
- Adopt Java SE 8, Maven 3.9.16 and Maven Wrapper 3.3.4.
- Adopt `tool.git-project` for clean-checkout repository bootstrap/dependency restoration.
- Consume the reusable `tool.java-project` CI workflow as the first external product repository.
- Replace the wiring-only application smoke shell with an explicit minimal `NEW -> RUNNING -> STOPPED` lifecycle and automated lifecycle tests.
- Embed application/build identity in the executable artifact instead of hard-coding it in Java source:
  - Maven `${project.version}` remains the authoritative software version;
  - `git-commit-id-plugin` 4.9.10 captures the full source revision and UTC build timestamp on the Java 8 baseline;
  - runtime startup logging exposes the concrete build provenance while the stable CI smoke line remains version-focused.
- Add intent-focused Javadoc/design references for the executable composition, lifecycle and build-identity boundary, and record the code-documentation convention in `AGENTS.md`.
- Adopt SLF4J as the logging facade while keeping the framework provider-neutral; the executable application selects `slf4j-jdk14` / `java.util.logging` for the initial runtime composition.
- Keep Step-2 execution deliberately short-lived and deterministic; HTTP/WebSocket, long-running service behaviour and timing-domain capability remain later work.
- Adopt generated build-output publication from the pinned `tool.java-project` revision:
  - PR builds publish both product JARs and evidence to `dev/pr-N/bld`;
  - `main` publishes the same canonical output to `prod/bld`;
  - the publication reuses the canonical Linux build rather than rebuilding solely for publication.
- Publish a readable aggregate unit-test report from the canonical Surefire results while retaining the raw XML/TXT evidence.
- Add version-aware release verification so release versions, CHANGELOG sections, Git tags, embedded build identity and retained GitHub Release artifacts are checked as one release baseline.
