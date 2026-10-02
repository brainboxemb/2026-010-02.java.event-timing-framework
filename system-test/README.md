# System-test module

This module implements formal black-box verification cases from the SI-01 VTS.

## Structure

```text
VcSt1_001Test / VcSt1_002Test / ...
        |
        | formal VC procedure + assertions
        v
TimingApplicationFixture
        |
        | SI-01 launch configuration, readiness, evidence and supported shutdown
        v
systemtest.framework
        |
        +-- ProcessRun
        +-- HttpTestClient
        +-- EventStream
        +-- RemoteShellClient
        +-- TestPorts
```

The verification case is the important part. A `VcSt1_*Test` class should read
roughly like the corresponding VTS procedure.

## Boundary rules

System tests:

- start the packaged application as a separate process;
- use only supported external interfaces;
- do not import application/core product classes;
- retain run evidence under the formal `VC-...` identifier.

The reusable framework owns transport/process mechanics. It must not encode
TimingNode registration semantics or case-specific expected values.

`TimingApplicationFixture` is intentionally one layer above the generic
framework. It may know how to launch SI-01, create synthetic application
configuration, wait for IF-03 readiness and request supported shutdown. It must
not contain the pass/fail semantics of a verification case.

## Adding a verification case

1. Define or update the formal `VC-...` case in the meta-repository VTS.
2. Add a Java class named after that identifier, for example
   `VC-ST1-004 -> VcSt1_004Test`.
3. Keep the case procedure and assertions in that class.
4. Reuse `TimingApplicationFixture` and `systemtest.framework` for mechanics.
5. Add framework functionality only when it is reusable across verification
   cases; do not hide case semantics behind convenience helpers.

This separation is deliberate: the Java test should remain reviewable against
the formal VTS without reading socket/process implementation code first.
