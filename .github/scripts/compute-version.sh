#!/usr/bin/env bash
# Derives the version strings a build needs from the git ref it was triggered by,
# and reports whether a pushed tag is a semantic version.
#
# Two different strings are needed because they answer to different rules:
#   display   what the application shows and what asset filenames carry. Free form.
#   installer what jpackage accepts for --app-version. Numeric only, and macOS
#             additionally requires a major component greater than zero, so
#             builds without a release tag fall back to a placeholder.
#
# Results are appended to $GITHUB_OUTPUT when running under Actions and always
# echoed, so the script is testable from a shell.
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=lib.sh
. "$script_dir/lib.sh"

# Placeholder --app-version for builds that are not releases. jpackage rejects a
# zero major version when building a .dmg, so this cannot be 0.0.0.
readonly DEV_INSTALLER_VERSION=1.0.0

# The semantic versioning 2.0.0 regex from semver.org, allowing a leading "v".
readonly SEMVER_RE='^v?(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-((0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*)(\.(0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*))*))?(\+([0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*))?$'

tag=""
ref_name=""
sha=""

while [ $# -gt 0 ]; do
    case $1 in
        --tag)
            tag=${2-}
            shift 2
            ;;
        --ref-name)
            ref_name=${2-}
            shift 2
            ;;
        --sha)
            sha=${2-}
            shift 2
            ;;
        *)
            die "unknown argument: $1"
            ;;
    esac
done

[ -n "$sha" ] || die "--sha is required"

is_semver=false
is_prerelease=false

if [ -n "$tag" ] && [[ $tag =~ $SEMVER_RE ]]; then
    is_semver=true
    display_version=${tag#v}
    # BASH_REMATCH[1..3] are the major, minor and patch captures; everything
    # after them is a prerelease or build identifier jpackage will not accept.
    installer_version="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.${BASH_REMATCH[3]}"
    if [ -n "${BASH_REMATCH[5]-}" ]; then
        is_prerelease=true
    fi
else
    # Slashes appear in pull request refs such as 42/merge and cannot go into a
    # filename, so they become dashes.
    display_version="${ref_name//\//-}-${sha:0:7}"
    installer_version=$DEV_INSTALLER_VERSION
fi

printf 'display-version=%s\n' "$display_version"
printf 'installer-version=%s\n' "$installer_version"
printf 'is-semver=%s\n' "$is_semver"
printf 'is-prerelease=%s\n' "$is_prerelease"

if [ -n "${GITHUB_OUTPUT-}" ]; then
    {
        printf 'display-version=%s\n' "$display_version"
        printf 'installer-version=%s\n' "$installer_version"
        printf 'is-semver=%s\n' "$is_semver"
        printf 'is-prerelease=%s\n' "$is_prerelease"
    } >> "$GITHUB_OUTPUT"
fi
