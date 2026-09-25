# Java workflow timing

- Workflow run: `36159995157`
- Capture point: `before-generated-output-push`
- Wall clock to capture: `80 s`
- Hosted runner time to capture: `115 s`
- Started runners: `6`
- Selected Windows mode: `full`
- Java/Moon decision action: `3 s`
- Maven reported total time: `11.790 s`

## Jobs

| Job | Runner | Result | Duration |
| --- | --- | --- | ---: |
| release-metadata | ubuntu-24.04 | success | 3 s |
| java-production / Java affected preflight | ubuntu-24.04 | success | 11 s |
| java-production / Full Windows qualification / Windows compatibility build | windows-2025 | success | 46 s |
| java-production / Java execution / Linux canonical build | ubuntu-24.04 | success | 34 s |
| java-production / Canonical artifact Windows smoke / Windows canonical-artifact smoke | windows-2025 | success | 13 s |
| publish-build / Publish generated output | ubuntu-24.04 | running | 8 s |

## Steps

### release-metadata

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 1 s |
| Checkout exact source revision | success | 1 s |
| Validate Maven version, CHANGELOG and tag semantics | success | 0 s |
| Verify failed-tag archival semantics | success | 0 s |
| Verify logging dependency boundary | success | 0 s |
| Post Checkout exact source revision | success | 0 s |
| Complete job | success | 0 s |

### java-production / Java affected preflight

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 0 s |
| Validate policy inputs | success | 0 s |
| Checkout exact consumer source | success | 2 s |
| Fetch exact affected-analysis base | success | 0 s |
| Checkout exact generic affected owner | success | 1 s |
| Checkout exact Java preflight owner | success | 1 s |
| Resolve Java and Windows impact once | success | 3 s |
| Upload preflight evidence | success | 1 s |
| Post Checkout exact Java preflight owner | success | 0 s |
| Post Checkout exact generic affected owner | success | 0 s |
| Post Checkout exact consumer source | success | 0 s |
| Complete job | success | 0 s |

### java-production / Full Windows qualification / Windows compatibility build

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 1 s |
| Validate exact source when supplied | success | 1 s |
| Checkout exact consumer source | success | 7 s |
| Set up exact Java 8 baseline | success | 3 s |
| Validate Maven Wrapper baseline | success | 14 s |
| Verify | success | 14 s |
| Upload Windows test evidence | success | 1 s |
| Post Set up exact Java 8 baseline | success | 0 s |
| Post Checkout exact consumer source | success | 2 s |
| Complete job | success | 0 s |

### java-production / Java execution / Linux canonical build

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 1 s |
| Validate execution inputs | success | 1 s |
| Checkout exact consumer source | success | 0 s |
| Set up exact Java 8 baseline | success | 3 s |
| Checkout exact tool.java-project implementation | success | 1 s |
| Run canonical Java action | success | 19 s |
| Upload canonical Java artifact | success | 1 s |
| Upload prepared build publication | success | 2 s |
| Upload Linux evidence | success | 2 s |
| Post Checkout exact tool.java-project implementation | success | 0 s |
| Post Set up exact Java 8 baseline | success | 0 s |
| Post Checkout exact consumer source | success | 0 s |
| Complete job | success | 0 s |

### java-production / Canonical artifact Windows smoke / Windows canonical-artifact smoke

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 2 s |
| Set up exact Java 8 baseline | success | 0 s |
| Download canonical Linux artifact | success | 2 s |
| Run canonical artifact on Windows | success | 6 s |
| Post Set up exact Java 8 baseline | success | 0 s |
| Complete job | success | 0 s |

### publish-build / Publish generated output

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 2 s |
| Checkout exact Java publication finalizer | success | 1 s |
| Checkout exact generic publisher implementation | success | 1 s |
| Download prepared output | success | 1 s |
| Download Java preflight evidence | running | 2 s |
