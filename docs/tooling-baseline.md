# Java tooling baseline

This repository consumes `brainboxemb/tool.java-project` as a released project-tooling dependency.

Current baseline:

- semantic release: `v0.1.0`;
- exact release commit: `3dd4b176956513948c601ec9cf95f09f6f21712a`.

`project.yml` uses the semantic release because it is the human-readable dependency contract. The committed `tools/tool.java-project` gitlink and reusable GitHub workflow `uses:` entries remain pinned to the exact immutable commit behind that release so build provenance is unambiguous.

The released tool baseline is independently exercised by `brainboxemb/template.java-project` before it is adopted here. This repository then provides the real-product consumer evidence.
