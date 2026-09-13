#!/usr/bin/env bash
set -euo pipefail

root="$(git rev-parse --show-toplevel)"
cd "$root"

# Artifact filenames are Maven-versioned. Read the reactor version directly from
# the root POM so snapshot/release preparation does not require editing Moon config.
version="$(python3 - <<'PY'
import xml.etree.ElementTree as ET

root = ET.parse('pom.xml').getroot()
namespace = {'m': 'http://maven.apache.org/POM/4.0.0'}
version = root.find('m:version', namespace)
if version is None or not version.text or not version.text.strip():
    raise SystemExit('root Maven project version is missing')
print(version.text.strip())
PY
)"

bash tools/tool.java-project/java-project.sh canonical \
  --working-directory . \
  --java-version '8.0.504+1' \
  --maven-version '3.9.16' \
  --maven-wrapper-version '3.3.4' \
  --test-report-path '**/target/surefire-reports/**' \
  --publication-root bld \
  --publication-artifact "framework/target/event-timing-framework-${version}.jar" \
  --publication-artifact "app/target/event-timing-app-${version}.jar"
