# Java workflow timing

- Workflow run: `37146163298`
- Capture point: `before-generated-output-push`
- Wall clock to capture: `68 s`
- Hosted runner time to capture: `67 s`
- Started runners: `5`
- Selected Windows mode: `none`
- Java/Moon decision action: `3 s`
- Maven reported total time: `14.748 s`

## Jobs

| Job | Runner | Result | Duration |
| --- | --- | --- | ---: |
| release-metadata | ubuntu-24.04 | success | 6 s |
| java-production / Java affected preflight | ubuntu-24.04 | success | 14 s |
| java-production / Java execution / Linux canonical build | ubuntu-24.04 | success | 32 s |
| Main black-box system verification | ubuntu-24.04 | running | 8 s |
| publish-build / Publish generated output | ubuntu-24.04 | running | 7 s |

## Steps

### release-metadata

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 1 s |
| Checkout exact source revision | success | 1 s |
| Validate Maven version, CHANGELOG and tag semantics | success | 0 s |
| Verify failed-tag archival semantics | success | 0 s |
| Verify logging dependency boundary | success | 0 s |
| Verify black-box system-test dependency boundary | success | 0 s |
| Post Checkout exact source revision | success | 1 s |
| Complete job | success | 0 s |

### java-production / Java affected preflight

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 0 s |
| Validate policy inputs | success | 1 s |
| Checkout exact consumer source | success | 1 s |
| Fetch exact affected-analysis base | success | 0 s |
| Checkout exact generic affected owner | success | 1 s |
| Checkout exact Java preflight owner | success | 1 s |
| Resolve Java and Windows impact once | success | 3 s |
| Upload preflight evidence | success | 1 s |
| Post Checkout exact Java preflight owner | success | 0 s |
| Post Checkout exact generic affected owner | success | 1 s |
| Post Checkout exact consumer source | success | 0 s |
| Complete job | success | 0 s |

### java-production / Java execution / Linux canonical build

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 2 s |
| Validate execution inputs | success | 0 s |
| Checkout exact consumer source | success | 1 s |
| Set up exact Java 8 baseline | success | 1 s |
| Checkout exact tool.java-project implementation | success | 1 s |
| Run canonical Java action | success | 20 s |
| Upload canonical Java artifact | success | 1 s |
| Upload prepared build publication | success | 2 s |
| Upload Linux evidence | success | 1 s |
| Post Checkout exact tool.java-project implementation | success | 0 s |
| Post Set up exact Java 8 baseline | success | 0 s |
| Post Checkout exact consumer source | success | 0 s |
| Complete job | success | 0 s |

### Main black-box system verification

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 3 s |
| Checkout exact integrated revision | success | 0 s |
| Set up exact Java 8 baseline | success | 1 s |
| Run VC-ST1 black-box system verification | running | 4 s |

### publish-build / Publish generated output

| Step | Result | Duration |
| --- | --- | ---: |
| Set up job | success | 1 s |
| Checkout exact Java publication finalizer | success | 1 s |
| Checkout exact generic publisher implementation | success | 1 s |
| Download prepared output | success | 1 s |
| Download Java preflight evidence | success | 1 s |
| Collect current workflow timing | running | 1 s |
