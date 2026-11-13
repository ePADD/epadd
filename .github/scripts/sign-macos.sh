#!/usr/bin/env bash
# macOS code signing and notarization.
#
# Signing happens inside jpackage, which needs the Developer ID certificate in a
# keychain before it runs, so this script is split into two steps:
#
#   import-certificate   load the certificate into a throwaway keychain
#   notarize <file>      submit the finished .dmg to Apple and staple the ticket
#
# Expects the signing material in the environment, supplied from repository
# secrets by the workflow:
#   MACOS_CERTIFICATE           base64 encoded Developer ID Application .p12
#   MACOS_CERTIFICATE_PASSWORD  its passphrase
#   MACOS_NOTARY_APPLE_ID       Apple ID of the notarization account
#   MACOS_NOTARY_TEAM_ID        Apple Developer team identifier
#   MACOS_NOTARY_PASSWORD       app specific password for that Apple ID
#
# Notarization uploads the installer to Apple and sends the Apple ID with it.
# Without it macOS refuses to open the application unless the user overrides
# Gatekeeper by hand.
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=lib.sh
. "$script_dir/lib.sh"

keychain_path() {
    printf '%s/epadd-signing.keychain-db\n' "${RUNNER_TEMP:-${TMPDIR:-/tmp}}"
}

import_certificate() {
    [ -n "${MACOS_CERTIFICATE-}" ] || die "MACOS_CERTIFICATE is not set"
    [ -n "${MACOS_CERTIFICATE_PASSWORD-}" ] || die "MACOS_CERTIFICATE_PASSWORD is not set"

    local keychain password certificate_dir certificate
    keychain=$(keychain_path)
    password=$(openssl rand -base64 24)
    certificate_dir=$(mktemp -d)
    certificate=$certificate_dir/certificate.p12

    printf '%s' "$MACOS_CERTIFICATE" | base64 -d > "$certificate"

    log "creating a keychain for the signing identity"
    rm -f "$keychain"
    security create-keychain -p "$password" "$keychain"
    # The default keychain relocks on a timer, which would break a long build.
    security set-keychain-settings -lut 21600 "$keychain"
    security unlock-keychain -p "$password" "$keychain"
    security import "$certificate" -k "$keychain" -P "$MACOS_CERTIFICATE_PASSWORD" \
        -T /usr/bin/codesign -T /usr/bin/security
    # Without this, codesign prompts for permission to use the key and the build
    # hangs instead of failing.
    security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k "$password" "$keychain" >/dev/null
    # Prepend rather than replace, so the system roots stay searchable.
    # shellcheck disable=SC2046
    security list-keychains -d user -s "$keychain" $(security list-keychains -d user | tr -d '"')

    rm -rf "$certificate_dir"

    log "signing identities available:"
    security find-identity -v -p codesigning "$keychain" >&2
}

notarize() {
    local file=$1
    require_file "$file"
    [ -n "${MACOS_NOTARY_APPLE_ID-}" ] || die "MACOS_NOTARY_APPLE_ID is not set"
    [ -n "${MACOS_NOTARY_TEAM_ID-}" ] || die "MACOS_NOTARY_TEAM_ID is not set"
    [ -n "${MACOS_NOTARY_PASSWORD-}" ] || die "MACOS_NOTARY_PASSWORD is not set"

    log "submitting $(basename "$file") to the Apple notary service"
    xcrun notarytool submit "$file" \
        --apple-id "$MACOS_NOTARY_APPLE_ID" \
        --team-id "$MACOS_NOTARY_TEAM_ID" \
        --password "$MACOS_NOTARY_PASSWORD" \
        --wait

    log "stapling the notarization ticket"
    xcrun stapler staple "$file"
    xcrun stapler validate "$file"
}

command=${1-}
case $command in
    import-certificate)
        import_certificate
        ;;
    notarize)
        shift
        [ $# -eq 1 ] || die "usage: sign-macos.sh notarize <file>"
        notarize "$1"
        ;;
    *)
        die "usage: sign-macos.sh import-certificate | notarize <file>"
        ;;
esac
