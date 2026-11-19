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

version=${tag#v}

printf 'ePADD %s, built from `%s`.\n\n' "$version" "${commit:0:7}"

# A short guide to the one installer most people need. A row is left out when
# its installer is not among the assets, for example after a platform failed.
printf '## Which file do I need?\n\n'
printf '| Your computer | Download |\n| --- | --- |\n'
which_file() {
    local computer=$1 asset=$2
    if [ -e "$dist/$asset" ]; then
        printf '| %s | `%s` |\n' "$computer" "$asset"
    fi
}
which_file 'Windows' "epadd-$version-windows-x86_64.msi"
which_file 'Windows on Arm, for example Snapdragon laptops' "epadd-$version-windows-arm64.msi"
which_file 'Mac with Apple silicon (M1 or later)' "epadd-$version-macos-arm64.dmg"
which_file 'Mac with an Intel processor' "epadd-$version-macos-x86_64.dmg"
which_file 'Ubuntu 24.04 or later' "epadd-$version-linux-x86_64.deb"
which_file 'Any system with Java 11 or later, no installation' "epadd-$version-standalone.jar"
printf '\n'
printf 'Not sure which Mac you have? Open the Apple menu and choose About This Mac. If it shows "Chip" followed by Apple M1, M2 or later, you have Apple silicon. If it shows "Processor" with Intel, you have an Intel Mac.\n\n'
printf 'The `epadd-discovery` files are the Discovery edition, which is used to publish collections for searching. The `.war` and `-standalone.jar` files are for server installations and for running ePADD with your own Java. The other files are described under Installing below.\n\n'

printf '## All downloads\n\n'
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
