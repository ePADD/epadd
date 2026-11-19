#!/usr/bin/env bash
# Prints a one line description of the JDK in JAVA_HOME, for example
# "17.0.13+11-LTS (Zulu17.54+21-CA)".
#
# The installers bundle this JDK, so the line says which Java they ship with. It
# is read from the JDK's release file rather than from java -version, because
# that file has the same layout on every platform and needs nothing to run.
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=lib.sh
. "$script_dir/lib.sh"

[ -n "${JAVA_HOME-}" ] || die "JAVA_HOME must point at a JDK"
release=$JAVA_HOME/release
require_file "$release"

# Prints the value of one KEY="value" line of the release file, or nothing.
release_value() {
    sed -n "s/^$1=\"\\(.*\\)\"\\r*\$/\\1/p" "$release" | head -n 1
}

version=$(release_value JAVA_RUNTIME_VERSION)
[ -n "$version" ] || version=$(release_value JAVA_VERSION)
[ -n "$version" ] || die "no Java version in $release"

implementor_version=$(release_value IMPLEMENTOR_VERSION)
if [ -n "$implementor_version" ]; then
    printf '%s (%s)\n' "$version" "$implementor_version"
else
    printf '%s\n' "$version"
fi
