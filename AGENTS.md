# AGENTS.md

## Repository purpose

This repository implements the public Java framework for SI-01 plus executable applications that consume it. Keep product implementation details here and coordination/system-level planning in the meta repository.

## Working rules

- Use issue → feature branch → draft PR → evidence/review → merge.
- Keep Java 8 compatibility until the meta-project explicitly changes the baseline.
- Use the repository Maven Wrapper; do not require a globally installed Maven.
- Keep the initial product artifact boundary deliberately small: one reusable `event-timing-framework` library and one runnable `event-timing-app` consumer.
- Do not equate architecture packages/layers with Maven artifacts. Add a separate library only when a real reuse, dependency, deployment, ownership, public/private or release boundary justifies it.
- Inside the framework library, keep event-timing rules/services in `domain`, reusable runtime/orchestration concerns in `core`, execution-environment abstractions in `platform`, and communication contracts/reusable communication concerns in `comm`.
- Keep application composition/startup/shutdown in the executable application rather than turning the framework into one fixed product composition.
- Place contracts with their semantic owner instead of collecting them automatically in a generic `api` module.
- Prefer composition and explicit ports/contracts over subclass-based extension.
- Design the framework so later single-system, multi-system or product-specific applications can consume it and inject/select their own concrete components.
- Do not create those future applications or optional libraries until a current implementation/test need justifies them.
- Keep public surfaces deliberately small.
- Do not introduce substantial future capability merely to make the skeleton look complete.
- Add tests with implementation changes and keep CI green on Linux and Windows.

## Public/private boundary

Never commit real/proprietary deployment information, including:

- real registration asset/system names;
- concrete external source IDs or mappings;
- reserve/virtual source assignments;
- production queue/exchange/routing names;
- proprietary protocol values or field mappings;
- credentials, encryption keys or other secrets.

Use generic synthetic identifiers and configuration in public tests/examples.

Do not name a specific real-world event in public repository documentation.

## Toolchain

Generic repository bootstrap/dependency handling is owned by `brainboxemb/tool.git-project`. Java build/test/CI tooling is owned by `brainboxemb/tool.java-project`. Consumers pin reviewed immutable commit SHAs while these tools are pre-v1.

Docker is not a prerequisite for normal Java compile/unit-test work. Add Docker/Compose only where an external service makes it useful for integration testing.
