#!/usr/bin/env bash
# Writes the body of a GitHub release to stdout.
#
# Run from a checkout of the tag being released, with the finished assets in the
# directory given by --dist.
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=lib.sh
. "$script_dir/lib.sh"

tag=""
dist=""
repository=""
macos_signed=false
windows_signed=false

while [ $# -gt 0 ]; do
    case $1 in
        --tag) tag=${2-}; shift 2 ;;
        --dist) dist=${2-}; shift 2 ;;
        --repository) repository=${2-}; shift 2 ;;
        --macos-signed) macos_signed=${2-}; shift 2 ;;
        --windows-signed) windows_signed=${2-}; shift 2 ;;
        *) die "unknown argument: $1" ;;
    esac
done

for required in tag dist repository; do
    [ -n "${!required}" ] || die "--${required//_/-} is required"
done
[ -d "$dist" ] || die "no such directory: $dist"

commit=$(git rev-parse HEAD)
previous_tag=$(git describe --tags --abbrev=0 "$tag^" 2>/dev/null || true)

printf 'ePADD %s, built from `%s`.\n\n' "${tag#v}" "${commit:0:7}"

printf '## Downloads\n\n'
printf '| Asset | Size |\n| --- | --: |\n'
for asset in "$dist"/*; do
    printf '| `%s` | %s MiB |\n' "$(basename "$asset")" "$(( $(wc -c < "$asset") / 1048576 ))"
done
printf '\n'

printf '## Installing\n\n'
printf '* The `.deb`, `.msi` and `.dmg` installers each bundle their own Java runtime, so nothing needs to be installed first.\n'
printf '* `epadd.exe` and the `-standalone.jar` files do not bundle a runtime and need Java 11 or later on the machine.\n'
printf '* The installers are per architecture, because each bundles the runtime it was built with: `macos-arm64` for Apple silicon, `macos-x86_64` for Intel, `windows-arm64` for Arm machines such as Snapdragon laptops, `windows-x86_64` otherwise.\n'
printf '* `epadd.exe` is a 32 bit x86 binary and runs on either Windows architecture, under emulation on Arm.\n'
printf '* The `.deb` was built on Ubuntu 24.04 and its dependencies are resolved against that release. On older distributions use the standalone jar.\n'

if [ "$windows_signed" != true ] || [ "$macos_signed" != true ]; then
    if [ "$windows_signed" != true ]; then
        printf '* The Windows artifacts are not code signed, so SmartScreen will warn about an unrecognised publisher.\n'
    fi
    if [ "$macos_signed" != true ]; then
        printf '* The macOS artifacts are neither signed nor notarized, so Gatekeeper will refuse to open them until the application is allowed under System Settings, Privacy and Security.\n'
    fi
fi

if [ -n "$previous_tag" ]; then
    printf '\n## Changes since %s\n\n' "$previous_tag"
    git log --no-merges --pretty='* %s' "$previous_tag..$tag"
    printf '\n**Full diff**: https://github.com/%s/compare/%s...%s\n' "$repository" "$previous_tag" "$tag"
fi
