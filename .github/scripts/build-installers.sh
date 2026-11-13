#!/usr/bin/env bash
# Builds the native installers for both ePADD editions on the current platform
# and leaves them in one directory under their release filenames.
#
# ePADD ships two editions from the same sources: the full application used for
# appraisal, processing and delivery, and the discovery module. Each needs its
# own package identity, so those identities live here as constants rather than
# as workflow configuration. The macOS bundle identifier and the Windows upgrade
# UUID must never change: Windows Installer uses the UUID to recognise an
# installed ePADD as the same product and replace it, and changing it makes a new
# version install alongside the old one instead of upgrading it.
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=lib.sh
. "$script_dir/lib.sh"

readonly EDITIONS=(full discovery)

edition_jar() {
    case $1 in
        full) printf 'epadd-standalone.jar\n' ;;
        discovery) printf 'epadd-discovery-standalone.jar\n' ;;
    esac
}

edition_name() {
    case $1 in
        full) printf 'epadd\n' ;;
        discovery) printf 'epadd-discovery\n' ;;
    esac
}

edition_mac_identifier() {
    case $1 in
        full) printf 'components.epadd\n' ;;
        discovery) printf 'components.epadd.discovery\n' ;;
    esac
}

edition_msi_upgrade_uuid() {
    case $1 in
        full) printf '1d3ea5d7-ade5-42a7-ade9-dea17a06d620\n' ;;
        discovery) printf '7bcfa560-1a38-4c39-b470-7d2b0be9c8df\n' ;;
    esac
}

# The Maintainer field of a .deb has to be a real address. This is the public
# ePADD project contact, taken from WebContent/contactus.jsp.
readonly DEB_MAINTAINER=epadd_project@stanford.edu

type=""
platform=""
version=""
app_version=""
launcher_dir=""
icon=""
dest=""
signing_identity=""

while [ $# -gt 0 ]; do
    case $1 in
        --type) type=${2-}; shift 2 ;;
        --platform) platform=${2-}; shift 2 ;;
        --version) version=${2-}; shift 2 ;;
        --app-version) app_version=${2-}; shift 2 ;;
        --launcher-dir) launcher_dir=${2-}; shift 2 ;;
        --icon) icon=${2-}; shift 2 ;;
        --dest) dest=${2-}; shift 2 ;;
        --signing-identity) signing_identity=${2-}; shift 2 ;;
        *) die "unknown argument: $1" ;;
    esac
done

for required in type platform version app_version launcher_dir icon dest; do
    [ -n "${!required}" ] || die "--${required//_/-} is required"
done

splash=$launcher_dir/lib/splash-image.png
mkdir -p "$dest"

for edition in "${EDITIONS[@]}"; do
    name=$(edition_name "$edition")
    args=(
        --type "$type"
        --jar "$launcher_dir/$(edition_jar "$edition")"
        --name "$name"
        --app-version "$app_version"
        --icon "$icon"
        --splash "$splash"
        --dest "$dest"
        --output-name "$name-$version-$platform.$type"
    )

    case $type in
        dmg)
            args+=(--package-identifier "$(edition_mac_identifier "$edition")")
            if [ -n "$signing_identity" ]; then
                args+=(--signing-identity "$signing_identity")
            fi
            ;;
        msi)
            args+=(--upgrade-uuid "$(edition_msi_upgrade_uuid "$edition")")
            ;;
        deb)
            args+=(--deb-maintainer "$DEB_MAINTAINER")
            ;;
        *)
            die "unsupported installer type: $type"
            ;;
    esac

    bash "$script_dir/package-installer.sh" "${args[@]}"
done
