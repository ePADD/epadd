#!/usr/bin/env bash
# Builds epadd-standalone.jar and epadd-discovery-standalone.jar.
# Run from the root of the epadd-launcher checkout.
#
# The launcher wraps each WAR together with an embedded Tomcat, the launcher's
# own icons and the ePADD settings models into one runnable jar. Its build.xml
# reads the WARs from ../epadd/target and the models from ${user.home}, so the
# three repositories have to sit next to each other in the workspace.
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=lib.sh
. "$script_dir/lib.sh"

workspace=""

while [ $# -gt 0 ]; do
    case $1 in
        --workspace)
            workspace=${2-}
            shift 2
            ;;
        *)
            die "unknown argument: $1"
            ;;
    esac
done

[ -n "$workspace" ] || die "--workspace is required"
if [ ! -f pom.xml ] || [ ! -f build.xml ]; then
    die "run this from the root of the epadd-launcher checkout"
fi
[ -d "$workspace/epadd-settings" ] || die "no epadd-settings checkout in $workspace"
require_file "$workspace/epadd/target/epadd-0.0.1-SNAPSHOT.war"
require_file "$workspace/epadd/target/epadd-discovery-0.0.1-SNAPSHOT.war"

mvn=$(resolve_tool mvn)
ant=$(resolve_tool ant)

log "building the launcher jar"
# The launcher POM declares clean compile assembly:single as its default goal.
"$mvn" -B -ntp
require_file target/epadd-standalone-jar-with-dependencies.jar

log "assembling the standalone jars"
# build.xml hardcodes ${user.home} as the base directory it reads the
# epadd-settings models from. Pointing user.home at the workspace lets the
# checkout be used directly instead of having to be copied into the real home
# directory of whoever or whatever is running the build.
"$ant" standalone-jar -Duser.home="$(native_path "$workspace")"

require_file epadd-standalone.jar
require_file epadd-discovery-standalone.jar

log "standalone jars built"
report_size epadd-standalone.jar
report_size epadd-discovery-standalone.jar
