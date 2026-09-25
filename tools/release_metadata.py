#!/usr/bin/env python3
"""Release metadata checks shared by the repository release workflow.

The script deliberately keeps Maven's project version authoritative. Git tags,
CHANGELOG sections and embedded runtime build identity are checked against that
version; they never rewrite it implicitly.

Release governance is coordinated by the meta-project SDP/SIP. This helper owns
only repository-local mechanical validation.
"""

import argparse
import re
import sys
import zipfile
from pathlib import Path
import xml.etree.ElementTree as ET

MAVEN_NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def _text(element, path):
    node = element.find(path, MAVEN_NS)
    return node.text.strip() if node is not None and node.text else None


def _root_version(repo_root):
    root = ET.parse(str(repo_root / "pom.xml")).getroot()
    version = _text(root, "m:version")
    if not version:
        raise ValueError("root pom.xml must declare an explicit project version")
    return root, version


def _validate_reactor_versions(repo_root, root, version):
    modules = [node.text.strip() for node in root.findall("m:modules/m:module", MAVEN_NS) if node.text]
    for module in modules:
        pom = repo_root / module / "pom.xml"
        if not pom.is_file():
            raise ValueError("reactor module POM is missing: {}".format(pom))
        module_root = ET.parse(str(pom)).getroot()
        parent_version = _text(module_root, "m:parent/m:version")
        explicit_version = _text(module_root, "m:version")
        if parent_version and parent_version != version:
            raise ValueError(
                "{} parent version {} does not match root version {}".format(pom, parent_version, version)
            )
        if explicit_version and explicit_version != version:
            raise ValueError(
                "{} project version {} does not match root version {}".format(pom, explicit_version, version)
            )


def _release_section(changelog_text, version):
    pattern = re.compile(r"^##\s+{}(?:\s|$).*?$".format(re.escape(version)), re.MULTILINE)
    match = pattern.search(changelog_text)
    if not match:
        return None
    start = match.end()
    next_heading = re.search(r"^##\s+", changelog_text[start:], re.MULTILINE)
    end = start + next_heading.start() if next_heading else len(changelog_text)
    return changelog_text[start:end].strip()


def inspect(repo_root, git_ref, github_output=None):
    root, version = _root_version(repo_root)
    _validate_reactor_versions(repo_root, root, version)

    is_release = not version.endswith("-SNAPSHOT")
    expected_tag = "v{}".format(version)
    changelog = (repo_root / "CHANGELOG.md").read_text(encoding="utf-8")

    if is_release and _release_section(changelog, version) is None:
        raise ValueError("CHANGELOG.md has no release section for {}".format(version))

    if git_ref.startswith("refs/tags/"):
        actual_tag = git_ref[len("refs/tags/") :]
        if not is_release:
            raise ValueError("release tag {} points to SNAPSHOT version {}".format(actual_tag, version))
        if actual_tag != expected_tag:
            raise ValueError("tag {} does not match Maven version {}; expected {}".format(actual_tag, version, expected_tag))

    values = {
        "version": version,
        "is_release": "true" if is_release else "false",
        "release_tag": expected_tag,
        "app_jar": "event-timing-app-{}.jar".format(version),
        "framework_jar": "event-timing-framework-{}.jar".format(version),
    }

    for key, value in values.items():
        print("{}={}".format(key, value))

    if github_output:
        with github_output.open("a", encoding="utf-8") as handle:
            for key, value in values.items():
                handle.write("{}={}\n".format(key, value))


def _load_properties(raw):
    values = {}
    for line in raw.decode("ISO-8859-1").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or line.startswith("!"):
            continue
        separator = "=" if "=" in line else ":" if ":" in line else None
        if separator:
            key, value = line.split(separator, 1)
            values[key.strip()] = value.strip()
    return values


def verify_artifact(jar_path, expected_version, expected_revision):
    with zipfile.ZipFile(str(jar_path), "r") as archive:
        try:
            raw = archive.read("event-timing-build.properties")
        except KeyError:
            raise ValueError("{} contains no event-timing-build.properties".format(jar_path))

    properties = _load_properties(raw)
    version = properties.get("application.version")
    revision = properties.get("build.revision")
    source_ref = properties.get("build.sourceRef")
    build_origin = properties.get("build.origin")
    dirty = properties.get("build.dirty")

    if version != expected_version:
        raise ValueError("artifact version {} does not match expected {}".format(version, expected_version))
    if revision != expected_revision:
        raise ValueError("artifact revision {} does not match expected {}".format(revision, expected_revision))
    if not source_ref:
        raise ValueError("artifact source ref is missing")
    if not build_origin:
        raise ValueError("artifact build origin is missing")
    if dirty not in ("true", "false"):
        raise ValueError("artifact dirty state is invalid: {}".format(dirty))

    print("version={}".format(version))
    print("revision={}".format(revision))
    print("source_ref={}".format(source_ref))
    print("build_origin={}".format(build_origin))
    print("dirty={}".format(dirty))


def write_release_notes(repo_root, version, output):
    changelog = (repo_root / "CHANGELOG.md").read_text(encoding="utf-8")
    notes = _release_section(changelog, version)
    if notes is None:
        raise ValueError("CHANGELOG.md has no release section for {}".format(version))
    output.write_text(notes + "\n", encoding="utf-8")
    print("wrote release notes to {}".format(output))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", type=Path, default=Path("."))
    subparsers = parser.add_subparsers(dest="command", required=True)

    inspect_parser = subparsers.add_parser("inspect", help="validate Maven/CHANGELOG/tag metadata")
    inspect_parser.add_argument("--git-ref", required=True)
    inspect_parser.add_argument("--github-output", type=Path)

    verify_parser = subparsers.add_parser("verify-artifact", help="verify embedded application build identity")
    verify_parser.add_argument("--jar", type=Path, required=True)
    verify_parser.add_argument("--version", required=True)
    verify_parser.add_argument("--revision", required=True)

    notes_parser = subparsers.add_parser("release-notes", help="extract one CHANGELOG release section")
    notes_parser.add_argument("--version", required=True)
    notes_parser.add_argument("--output", type=Path, required=True)

    args = parser.parse_args()
    repo_root = args.repo_root.resolve()

    try:
        if args.command == "inspect":
            inspect(repo_root, args.git_ref, args.github_output)
        elif args.command == "verify-artifact":
            verify_artifact(args.jar, args.version, args.revision)
        elif args.command == "release-notes":
            write_release_notes(repo_root, args.version, args.output)
    except (OSError, ValueError, ET.ParseError, zipfile.BadZipFile) as exc:
        print("ERROR: {}".format(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
