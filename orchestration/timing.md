# Java workflow timing

- Workflow run: `35211641563`
- Capture point: `before-generated-output-push`
- Wall clock to capture: `81 s`
- Hosted runner time to capture: `107 s`
- Started runners: `5`
- Maven reported total time: `9.303 s`

## Jobs

| Job | Runner | Result | Duration |
| --- | --- | --- | ---: |
| release-metadata | ubuntu-24.04 | success | 3 s |
| release-linux / Linux canonical build | ubuntu-24.04 | success | 23 s |
| release-windows-full / Windows compatibility build | windows-2025 | success | 66 s |
| release-windows-smoke / Windows canonical-artifact smoke | windows-2025 | success | 10 s |
| release-publish-build / Publish generated output | ubuntu-24.04 | running | 5 s |

## Steps

### release-metadata

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 0 s |
| Checkout exact source revision | success | 1 s |
| Validate Maven version, CHANGELOG and tag semantics | success | 0 s |
| Verify failed-tag archival semantics | success | 0 s |
| Verify logging dependency boundary | success | 0 s |
| Post Checkout exact source revision | success | 0 s |
| Complete job | success | 0 s |

### release-linux / Linux canonical build

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 1 s |
| Validate execution inputs | success | 0 s |
| Checkout exact consumer source | success | 1 s |
| Set up exact Java 8 baseline | success | 1 s |
| Checkout exact tool.java-project implementation | success | 0 s |
| Run canonical Java action | success | 16 s |
| Upload canonical Java artifact | success | 0 s |
| Upload prepared build publication | success | 1 s |
| Upload Linux evidence | success | 1 s |
| Post Checkout exact tool.java-project implementation | success | 0 s |
| Post Set up exact Java 8 baseline | success | 0 s |
| Post Checkout exact consumer source | success | 0 s |
| Complete job | success | 0 s |

### release-windows-full / Windows compatibility build

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 3 s |
| Validate exact source when supplied | success | 1 s |
| Checkout exact consumer source | success | 13 s |
| Set up exact Java 8 baseline | success | 2 s |
| Validate Maven Wrapper baseline | success | 6 s |
| Verify | success | 31 s |
| Upload Windows test evidence | success | 1 s |
| Post Set up exact Java 8 baseline | success | 3 s |
| Post Checkout exact consumer source | success | 3 s |
| Complete job | success | 0 s |

### release-windows-smoke / Windows canonical-artifact smoke

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 2 s |
| Set up exact Java 8 baseline | success | 0 s |
| Download canonical Linux artifact | success | 2 s |
| Run canonical artifact on Windows | success | 2 s |
| Post Set up exact Java 8 baseline | success | 0 s |
| Complete job | success | 0 s |

### release-publish-build / Publish generated output

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 1 s |
| Checkout exact Java publication finalizer | success | 1 s |
| Checkout exact generic publisher implementation | success | 1 s |
| Download prepared output | running | 1 s |
