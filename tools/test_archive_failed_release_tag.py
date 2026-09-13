#!/usr/bin/env python3
"""Integration test for failed release-tag archival against a bare Git remote."""

import os
import subprocess
import sys
import tempfile
from pathlib import Path


def run(*args, cwd=None):
    return subprocess.run(args, cwd=cwd, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)


def remote_revision(work, remote, tag):
    result = run(
        "git",
        "ls-remote",
        str(remote),
        "refs/tags/{}".format(tag),
        "refs/tags/{}^{{}}".format(tag),
        cwd=str(work),
    )
    lines = [line.split("\t", 1) for line in result.stdout.splitlines() if line.strip()]
    peeled = [sha for sha, ref in lines if ref.endswith("^{}")]
    if peeled:
        return peeled[0]
    direct = [sha for sha, ref in lines if ref == "refs/tags/{}".format(tag)]
    return direct[0] if direct else None


def main():
    helper = Path(__file__).with_name("archive_failed_release_tag.py").resolve()
    with tempfile.TemporaryDirectory(prefix="failed-release-tag-") as tmp:
        root = Path(tmp)
        remote = root / "remote.git"
        work = root / "work"

        run("git", "init", "--bare", str(remote))
        run("git", "init", str(work))
        run("git", "config", "user.name", "release-test", cwd=str(work))
        run("git", "config", "user.email", "release-test@example.invalid", cwd=str(work))

        (work / "README.md").write_text("release candidate\n", encoding="utf-8")
        run("git", "add", "README.md", cwd=str(work))
        run("git", "commit", "-m", "release candidate", cwd=str(work))
        revision = run("git", "rev-parse", "HEAD", cwd=str(work)).stdout.strip()
        run("git", "remote", "add", "origin", str(remote), cwd=str(work))
        run("git", "push", "origin", "HEAD:refs/heads/main", cwd=str(work))

        run("git", "tag", "-a", "v0.1.0", "-m", "Release v0.1.0", cwd=str(work))
        run("git", "push", "origin", "refs/tags/v0.1.0", cwd=str(work))

        run(
            sys.executable,
            str(helper),
            "--remote",
            "origin",
            "--tag",
            "v0.1.0",
            "--expected-revision",
            revision,
            cwd=str(work),
        )

        if remote_revision(work, remote, "v0.1.0") is not None:
            raise AssertionError("normal release tag still exists after archival")
        if remote_revision(work, remote, "v0.1.0-failed") != revision:
            raise AssertionError("failed release tag does not point to the release candidate commit")

        invalid = subprocess.run(
            [
                sys.executable,
                str(helper),
                "--remote",
                "origin",
                "--tag",
                "v0.1.0-failed",
                "--expected-revision",
                revision,
            ],
            cwd=str(work),
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        if invalid.returncode == 0:
            raise AssertionError("helper accepted a -failed tag as a normal release tag")

    print("failed release tag archival integration test: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
