#!/usr/bin/env bash
# Wraps the full edition's standalone jar into epadd.exe with launch4j.
# Run from the root of the epadd-launcher checkout.
#
# Unlike the jpackage installers this executable carries no Java runtime, so it
# needs Java 11 or later already installed. The launch4j configuration that
# decides the icon, the heap limits and the single instance behaviour lives in
# the launcher repository.
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=lib.sh
. "$script_dir/lib.sh"

if [ ! -f build.xml ]; then
    die "run this from the root of the epadd-launcher checkout"
fi
require_file epadd-standalone.jar

ant=$(resolve_tool ant)

log "wrapping the standalone jar with launch4j"
"$ant" exe

require_file epadd.exe
report_size epadd.exe
