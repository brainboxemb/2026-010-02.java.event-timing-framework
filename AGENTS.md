# AGENTS.md

## Repository purpose

This repository implements the public Java framework for SI-01 plus executable applications that consume it. Keep product implementation details here and coordination/system-level planning in the meta repository.

## Working rules

- Use one work-item number end to end: create issue `#N`, create `feature/pr-N-<short-slug>`, make the smallest initial commit, then convert that exact issue directly into draft PR `#N`; do not create a separate PR number for the same work item when issue conversion is available.
- Continue implementation, evidence/review and merge in that same PR.
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
- Add tests with implementation changes and keep the released Linux/Windows contract green, but do not allocate Windows on every build: pull requests use affected `auto` selection, ordinary protected-main publication uses `none`, and exact release qualification uses `full`.
- Keep Moon limited to consumer-owned impact declarations. Maven remains Java build/test authority and released `tool.java-project` owns shared execution, selective Windows qualification, evidence and generated build publication.

## Code documentation

- Add intent-focused Javadoc to public or non-trivial classes and methods when ownership, lifecycle, invariants, build provenance or architectural purpose are not obvious from the Java syntax alone.
- Explain **why** a boundary or mechanism exists rather than restating what a line of Java already says.
- Reference the owning meta-repository SAD/SDD/requirement by stable document path when that materially helps a future maintainer understand the decision.
- Keep implementation detail in this repository; do not duplicate large design sections from the meta repository into source comments.
- Document non-obvious pinned build plugins/dependencies in the POM/README, including compatibility constraints that explain why a specific version is intentional.
- Keep comments current when behaviour changes; stale design references are worse than no reference.

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

Generic repository bootstrap/dependency handling is owned by `brainboxemb/tool.git-project`. Java build/test/CI tooling is owned by `brainboxemb/tool.java-project`. Keep the semantic release in `project.yml` and exact immutable workflow/gitlink provenance aligned to the reviewed release.

Docker is not a prerequisite for normal Java compile/unit-test work. Add Docker/Compose where it solves a concrete integration, reproducibility or long-term/offline build requirement rather than by default.
