#!/usr/bin/env bash
# Turns a standalone jar into a native installer with jpackage.
#
# The argument set mirrors the macapp target in epadd-launcher/build.xml, which
# only knows how to build a .dmg. Driving jpackage from here instead means the
# Linux, Windows and macOS installers are produced the same way and that signing
# has one place to live.
#
# The bundled runtime is whatever JDK $JAVA_HOME points at, so the installer is
# only valid for the architecture of the machine that built it.
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=lib.sh
. "$script_dir/lib.sh"

readonly MAIN_CLASS=edu.stanford.epadd.launcher.ePADD
readonly VENDOR="Stanford University Libraries"
readonly DESCRIPTION="ePADD email archiving and discovery software"

type=""
jar=""
name=""
app_version=""
icon=""
splash=""
dest=""
output_name=""
package_identifier=""
upgrade_uuid=""
deb_maintainer=""
signing_identity=""

while [ $# -gt 0 ]; do
    case $1 in
        --type) type=${2-}; shift 2 ;;
        --jar) jar=${2-}; shift 2 ;;
        --name) name=${2-}; shift 2 ;;
        --app-version) app_version=${2-}; shift 2 ;;
        --icon) icon=${2-}; shift 2 ;;
        --splash) splash=${2-}; shift 2 ;;
        --dest) dest=${2-}; shift 2 ;;
        --output-name) output_name=${2-}; shift 2 ;;
        --package-identifier) package_identifier=${2-}; shift 2 ;;
        --upgrade-uuid) upgrade_uuid=${2-}; shift 2 ;;
        --deb-maintainer) deb_maintainer=${2-}; shift 2 ;;
        --signing-identity) signing_identity=${2-}; shift 2 ;;
        *) die "unknown argument: $1" ;;
    esac
done

for required in type jar name app_version icon splash dest output_name; do
    [ -n "${!required}" ] || die "--${required//_/-} is required"
done
require_file "$jar"
require_file "$icon"
require_file "$splash"

[ -n "${JAVA_HOME-}" ] || die "JAVA_HOME must point at a JDK 14 or later"
# jpackage copies the runtime image by walking it, and fails part way through if
# it is reached through a symlink, so the real location is resolved up front.
java_home=$(cd "$JAVA_HOME" && pwd -P)
jpackage=$java_home/bin/jpackage
[ -x "$jpackage" ] || die "no jpackage in $java_home/bin; JDK 14 or later is required"

# jpackage copies the whole input directory into the application, so it gets a
# directory holding nothing but the jar and the splash image.
stage=$(mktemp -d)
# jpackage names its output itself, so it also writes into an empty directory of
# its own. Letting it write straight into $dest would make the one file it
# produced impossible to tell apart from the installers already collected there.
output_dir=$(mktemp -d)
trap 'rm -rf "$stage" "$output_dir"' EXIT
cp "$jar" "$splash" "$stage/"

mkdir -p "$dest"

args=(
    --type "$type"
    --name "$name"
    --app-version "$app_version"
    --vendor "$VENDOR"
    --description "$DESCRIPTION"
    --icon "$(native_path "$icon")"
    --input "$(native_path "$stage")"
    --main-jar "$(basename "$jar")"
    --main-class "$MAIN_CLASS"
    --runtime-image "$(native_path "$java_home")"
    --java-options -XX:MaxRAMPercentage=75
    # The jar's SplashScreen-Image manifest entry is ignored because the
    # installed application is not started with java -jar. $APPDIR is expanded
    # by the launcher at runtime, so it must reach jpackage unexpanded.
    --java-options '-splash:$APPDIR/splash-image.png'
    --dest "$(native_path "$output_dir")"
)

case $type in
    dmg)
        [ -n "$package_identifier" ] || die "--package-identifier is required for a dmg"
        args+=(--mac-package-identifier "$package_identifier")
        if [ -n "$signing_identity" ]; then
            log "signing the application bundle as $signing_identity"
            args+=(--mac-sign --mac-signing-key-user-name "$signing_identity")
        fi
        ;;
    msi)
        [ -n "$upgrade_uuid" ] || die "--upgrade-uuid is required for an msi"
        # The upgrade UUID identifies the product line to Windows Installer. It
        # has to stay the same forever, or upgrades install alongside the old
        # version instead of replacing it.
        #
        # A per-user install keeps the installer from demanding administrator
        # rights, which users on managed library machines frequently do not have.
        args+=(
            --win-upgrade-uuid "$upgrade_uuid"
            --win-per-user-install
            --win-dir-chooser
            --win-menu
            --win-menu-group ePADD
            --win-shortcut
        )
        ;;
    deb)
        [ -n "$deb_maintainer" ] || die "--deb-maintainer is required for a deb"
        args+=(
            --linux-package-name "$name"
            --linux-deb-maintainer "$deb_maintainer"
            --linux-app-category utils
            --linux-menu-group Office
            --linux-shortcut
        )
        ;;
    *)
        die "unsupported installer type: $type"
        ;;
esac

log "building $output_name with jpackage"
"$jpackage" "${args[@]}"

# jpackage decides the filename itself and the convention differs per platform,
# for example epadd_11.1.4_amd64.deb against epadd-11.1.4.dmg, so whatever landed
# in its output directory is renamed to the one name the rest of the pipeline
# expects.
produced=$(find "$output_dir" -maxdepth 1 -type f -name "*.$type")
# grep exits non-zero when it counts nothing, which under set -e would abort
# here rather than reach the message below.
case $(printf '%s\n' "$produced" | grep -c . || true) in
    1) ;;
    0) die "jpackage produced no .$type" ;;
    *) die "jpackage produced more than one .$type:
$produced" ;;
esac
mv "$produced" "$dest/$output_name"

log "built $output_name"
report_size "$dest/$output_name"
