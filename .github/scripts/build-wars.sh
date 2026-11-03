#!/usr/bin/env bash
# Builds epadd-discovery-0.0.1-SNAPSHOT.war and epadd-0.0.1-SNAPSHOT.war.
# Run from the root of the ePADD checkout.
#
# Both POMs write into the same target directory, so the order of the Maven
# invocations below is load bearing:
#
#   install parent -> clean -> validate -> ant version -> discovery WAR -> full WAR
#
# "ant version" regenerates two tracked files (src/java/edu/stanford/epadd/
# Version.java and WebContent/version.jsp), which leaves the checkout dirty.
# Running validate before it is what lets a release build carry real provenance:
# git-commit-id writes git.properties into target/classes while the tree is still
# pristine, so it records git.dirty=false, and the later packaging run is told to
# keep that file rather than regenerate it from the dirtied tree.
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=lib.sh
. "$script_dir/lib.sh"

version=""
provenance=false
skip_tests=false

while [ $# -gt 0 ]; do
    case $1 in
        --version)
            version=${2-}
            shift 2
            ;;
        --provenance)
            provenance=true
            shift
            ;;
        --skip-tests)
            skip_tests=true
            shift
            ;;
        *)
            die "unknown argument: $1"
            ;;
    esac
done

[ -n "$version" ] || die "--version is required"
if [ ! -f pom.xml ] || [ ! -f build.xml ]; then
    die "run this from the root of the ePADD checkout"
fi

mvn=$(resolve_tool mvn)
ant=$(resolve_tool ant)

# download-licenses fetches every dependency's licence URL on every build. Its
# output never reaches either WAR, so it only costs minutes and network
# flakiness here.
maven_args=(-B -ntp -Dlicense.skipDownloadLicenses=true)
if $skip_tests; then
    maven_args+=(-DskipTests)
fi

log "installing the parent POM"
"$mvn" "${maven_args[@]}" -f pom-common.xml install

log "cleaning previous output"
"$mvn" "${maven_args[@]}" clean

if $provenance; then
    # The enforcer rule refuses to build from a modified checkout, and the error
    # it raises arrives minutes later and does not say what was modified.
    # Stamping the version below modifies the checkout itself, so a second
    # provenance build in the same working copy needs those files restored first.
    modified=$(git status --porcelain --untracked-files=no)
    if [ -n "$modified" ]; then
        printf '%s\n' "$modified" >&2
        die "a provenance build needs a pristine checkout, but the files above are modified"
    fi

    log "recording git provenance from the pristine checkout"
    "$mvn" "${maven_args[@]}" validate -Dskip.generate.git.props=false
    require_file target/classes/git.properties
fi

log "stamping version $version"
printf '%s' "$version" > version
"$ant" version

# Both POMs compile the same sources and would therefore run the same tests
# twice, so only the full WAR below runs them.
log "building the discovery WAR"
"$mvn" "${maven_args[@]}" -f pom-discovery.xml package -DskipTests

log "building the full WAR"
if $provenance; then
    # The prod profile turns the enforcer rule on, which demands git.dirty=false.
    # A command line property beats the one the profile sets, so generation stays
    # off and the git.properties written earlier is what ends up in the WAR.
    #
    # Spelling both invocations out rather than collecting the extra arguments in
    # an array keeps this working on the bash 3.2 that macOS ships, where
    # expanding an empty array under set -u is an unbound variable error.
    "$mvn" "${maven_args[@]}" package -Pprod -Dskip.generate.git.props=true
else
    "$mvn" "${maven_args[@]}" package
fi

require_file target/epadd-0.0.1-SNAPSHOT.war
require_file target/epadd-discovery-0.0.1-SNAPSHOT.war

log "WARs built"
report_size target/epadd-0.0.1-SNAPSHOT.war
report_size target/epadd-discovery-0.0.1-SNAPSHOT.war
