# AGENTS.md

## Repository purpose

This repository implements the public Java framework for SI-01. Keep product implementation details here and coordination/system-level planning in the meta repository.

## Working rules

- Use issue → feature branch → draft PR → evidence/review → merge.
- Keep Java 8 compatibility until the meta-project explicitly changes the baseline.
- Use the repository Maven Wrapper; do not require a globally installed Maven.
- Keep the Maven reactor aligned with the documented module responsibilities.
- Dependencies point inward. Domain/core code must not depend on concrete adapters.
- Prefer composition and public ports/contracts over subclass-based extension.
- Keep the public API surface deliberately small.
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

The reusable Java verification workflow is owned by `brainboxemb/tool.java-project`. Consumers must pin a reviewed immutable commit SHA while that toolchain is pre-v1.

Docker is not a prerequisite for normal Java compile/unit-test work. Add Docker/Compose only where an external service makes it useful for integration testing.
