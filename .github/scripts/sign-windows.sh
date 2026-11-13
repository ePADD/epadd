#!/usr/bin/env bash
# Code signs a Windows executable or installer.
#
# Expects the signing material in the environment, supplied from repository
# secrets by the workflow:
#   WINDOWS_CERTIFICATE           base64 encoded PKCS#12 bundle
#   WINDOWS_CERTIFICATE_PASSWORD  its passphrase
#   WINDOWS_TIMESTAMP_URL         optional RFC 3161 timestamp authority
#
# Timestamping sends the file's hash, and nothing else, to the timestamp
# authority so that the signature stays valid after the certificate expires.
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=lib.sh
. "$script_dir/lib.sh"

readonly DEFAULT_TIMESTAMP_URL=http://timestamp.digicert.com

[ $# -gt 0 ] || die "usage: sign-windows.sh <file> [file...]"
[ -n "${WINDOWS_CERTIFICATE-}" ] || die "WINDOWS_CERTIFICATE is not set"
[ -n "${WINDOWS_CERTIFICATE_PASSWORD-}" ] || die "WINDOWS_CERTIFICATE_PASSWORD is not set"

# signtool ships with the Windows SDK, which installs one directory per SDK
# version, so the newest one present is picked.
signtool=$(find "/c/Program Files (x86)/Windows Kits/10/bin" -type f -name signtool.exe -path '*/x64/*' 2>/dev/null | sort -V | tail -1)
[ -n "$signtool" ] || die "signtool.exe was not found; the Windows SDK is missing"
log "using $signtool"

certificate=$(mktemp -d)/certificate.pfx
trap 'rm -rf "$(dirname "$certificate")"' EXIT
printf '%s' "$WINDOWS_CERTIFICATE" | base64 -d > "$certificate"

for file in "$@"; do
    require_file "$file"
    log "signing $(basename "$file")"
    "$signtool" sign \
        /f "$(native_path "$certificate")" \
        /p "$WINDOWS_CERTIFICATE_PASSWORD" \
        /fd SHA256 \
        /tr "${WINDOWS_TIMESTAMP_URL:-$DEFAULT_TIMESTAMP_URL}" \
        /td SHA256 \
        /d ePADD \
        "$(native_path "$file")"
    "$signtool" verify /pa /v "$(native_path "$file")"
done
