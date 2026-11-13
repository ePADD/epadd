#!/usr/bin/env bash
# Shared helpers for the ePADD build and release scripts. Sourced, not executed.

log() {
    printf '==> %s\n' "$*" >&2
}

die() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

require_file() {
    [ -f "$1" ] || die "expected file is missing: $1"
}

# Maven and Ant are installed as .cmd wrappers on the Windows runners and bash
# does not resolve those from the bare name, so every call site goes through here.
resolve_tool() {
    local name=$1 candidate
    for candidate in "$name" "$name.cmd" "$name.bat"; do
        if command -v "$candidate" >/dev/null 2>&1; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done
    die "$name was not found on PATH"
}

# The JDK and Ant cannot read MSYS paths such as /d/a/epadd/epadd, so absolute
# paths handed to a Java tool have to be converted to native form first.
native_path() {
    if command -v cygpath >/dev/null 2>&1; then
        cygpath -m "$1"
    else
        printf '%s\n' "$1"
    fi
}

report_size() {
    local path=$1
    printf '%s\t%s MiB\n' "$(basename "$path")" "$(( $(wc -c < "$path") / 1048576 ))"
}
