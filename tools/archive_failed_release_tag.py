#!/usr/bin/env python3
"""Archive a failed release candidate tag as <tag>-failed.

The normal release tag is removed only after the failed tag has been pushed and
verified on the remote. Release versions are therefore never silently reused.
"""

import argparse
import re
import subprocess
import sys

RELEASE_TAG = re.compile(r"^v\d+\.\d+\.\d+$")


def run(*args, cwd=None, check=True):
    result = subprocess.run(args, cwd=cwd, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if check and result.returncode != 0:
        raise RuntimeError("command failed: {}\n{}".format(" ".join(args), result.stderr.strip()))
    return result


def remote_tag_revision(remote, tag, cwd=None):
    result = run("git", "ls-remote", remote, "refs/tags/{}".format(tag), "refs/tags/{}^{{}}".format(tag), cwd=cwd)
    lines = [line.strip().split("\t", 1) for line in result.stdout.splitlines() if line.strip()]
    peeled = [sha for sha, ref in lines if ref.endswith("^{}")]
    if peeled:
        return peeled[0]
    direct = [sha for sha, ref in lines if ref == "refs/tags/{}".format(tag)]
    return direct[0] if direct else None


def archive(remote, tag, expected_revision, cwd=None):
    if not RELEASE_TAG.match(tag):
        raise ValueError("not a normal release tag: {}".format(tag))

    failed_tag = tag + "-failed"
    run("git", "fetch", "--force", "--tags", remote, cwd=cwd)

    actual_revision = run("git", "rev-list", "-n", "1", tag, cwd=cwd).stdout.strip()
    if actual_revision != expected_revision:
        raise ValueError(
            "{} resolves to {}, expected {}".format(tag, actual_revision, expected_revision)
        )

    existing_failed_revision = remote_tag_revision(remote, failed_tag, cwd=cwd)
    if existing_failed_revision and existing_failed_revision != expected_revision:
        raise ValueError(
            "{} already exists at {}, expected {}".format(
                failed_tag, existing_failed_revision, expected_revision
            )
        )

    if not existing_failed_revision:
        run(
            "git",
            "tag",
            "-a",
            failed_tag,
            expected_revision,
            "-m",
            "Failed release candidate {}".format(tag),
            cwd=cwd,
        )
        run("git", "push", remote, "refs/tags/{}".format(failed_tag), cwd=cwd)

    verified_failed_revision = remote_tag_revision(remote, failed_tag, cwd=cwd)
    if verified_failed_revision != expected_revision:
        raise ValueError(
            "remote {} resolves to {}, expected {}".format(
                failed_tag, verified_failed_revision, expected_revision
            )
        )

    run("git", "push", remote, ":refs/tags/{}".format(tag), cwd=cwd)

    if remote_tag_revision(remote, tag, cwd=cwd) is not None:
        raise ValueError("normal release tag still exists remotely: {}".format(tag))

    print("archived_failed_release_tag={}".format(failed_tag))
    print("revision={}".format(expected_revision))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--remote", default="origin")
    parser.add_argument("--tag", required=True)
    parser.add_argument("--expected-revision", required=True)
    args = parser.parse_args()

    try:
        archive(args.remote, args.tag, args.expected_revision)
    except (RuntimeError, ValueError) as exc:
        print("ERROR: {}".format(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
