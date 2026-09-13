# Changelog

## Unreleased

- Bootstrap the SI-01 Maven reactor with one reusable `event-timing-framework` library and one runnable `event-timing-app` consumer.
- Keep `domain`, `core`, `platform`, and `comm` as package/architecture responsibilities inside the framework artifact rather than speculative separate libraries.
- Adopt Java SE 8, Maven 3.9.16 and Maven Wrapper 3.3.4.
- Adopt `tool.git-project` for clean-checkout repository bootstrap/dependency restoration.
- Consume the reusable `tool.java-project` CI workflow as the first external product repository.
- Replace the wiring-only application smoke shell with an explicit minimal `NEW -> RUNNING -> STOPPED` lifecycle and automated lifecycle tests.
- Embed application/build identity in the executable artifact instead of hard-coding it in Java source:
  - Maven `${project.version}` remains the authoritative software version;
  - `git-commit-id-plugin` 4.9.10 captures the full source revision and UTC build timestamp on the Java 8 baseline;
  - runtime startup logging exposes the concrete snapshot provenance while the stable CI smoke line remains version-focused.
- Add intent-focused Javadoc/design references for the executable composition, lifecycle and build-identity boundary, and record the code-documentation convention in `AGENTS.md`.
- Adopt SLF4J as the logging facade while keeping the framework provider-neutral; the executable application selects `slf4j-jdk14` / `java.util.logging` for the initial runtime composition.
- Keep Step-2 execution deliberately short-lived and deterministic; HTTP/WebSocket, long-running service behaviour and timing-domain capability remain later work.
- Adopt generated build-output publication from the pinned `tool.java-project` revision:
  - PR builds publish both product JARs and evidence to `dev/pr-N/bld`;
  - `main` publishes the same canonical output to `prod/bld`;
  - the publication reuses the canonical Linux build rather than rebuilding solely for publication.
