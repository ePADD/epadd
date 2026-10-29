# Automated builds and releases

Every pull request is built and tested on Linux, Windows and both macOS
architectures. Pushing a semantic version tag builds the same thing again and
publishes it as a GitHub release.

## How the pieces fit together

ePADD is assembled from three repositories, which is why the workflows do more
than call Maven:

| Repository | Produces |
| --- | --- |
| `epadd` (this one) | `epadd-0.0.1-SNAPSHOT.war` and `epadd-discovery-0.0.1-SNAPSHOT.war` |
| `epadd-launcher` | wraps each WAR with an embedded Tomcat into a runnable jar, and wraps the full jar into `epadd.exe` |
| `epadd-settings` | the NLP models that are packed inside those jars |

Only this repository is forked, so the other two are checked out at a commit
pinned in `LAUNCHER_REF` and `SETTINGS_REF` at the top of
`.github/workflows/build.yml`. Moving either pin is a deliberate change that
goes through review like any other.

The three checkouts are placed next to each other in the workspace, which is the
layout `epadd-launcher/build.xml` already expects.

## Workflows

| File | Trigger | What it does |
| --- | --- | --- |
| `ci.yml` | pull request, push to `main`, manual | Lints the build scripts, then builds and tests on all five platforms. Uploads nothing unless asked. |
| `release.yml` | push of a `v*` tag | Releases the tag if it is a semantic version, otherwise does nothing. |
| `build.yml` | called by the other two | Does the actual work. Not triggered directly. |

`ci.yml` keeps no artifacts by default, because one run produces several
gigabytes of installers and the purpose of a pull request build is to prove that
every platform still packages. Run it from the Actions tab with
**Keep the installers as workflow artifacts** when the installers from a branch
are needed.

## Releasing

Push an annotated tag that is a full semantic version:

```
git tag -a v11.1.4 -m 'ePADD 11.1.4'
git push origin v11.1.4
```

A tag with a prerelease identifier, for example `v11.1.4-beta.1`, is published as
a GitHub prerelease.

Tags that are not a full semantic version are ignored. The repository already
carries a number of those, such as `v11.1`, `v11-alpha` and `v8`, and pushing
one leaves the workflow reporting that it built nothing rather than failing.

Of the 74 tags in the repository, 23 pass that check. All the recent ones do;
the rest predate the current convention.

### Two version strings

A release carries two versions because they answer to different rules.

The **display version** is the tag without its leading `v`. It is what
`ant version` bakes into `Version.java` and `version.jsp`, so it is what the
application reports, and it is what the asset filenames carry.

The **installer version** is the numeric part of the tag only. jpackage accepts
nothing else for `--app-version`, and on macOS it additionally insists on a major
component greater than zero, so `v11.1.4-beta.1` becomes `11.1.4`. A tag like
`v0.1.0` is therefore refused outright, in the version job rather than minutes
later in the two macOS jobs. Builds that are
not releases have no version to work from and fall back to a placeholder of
`1.0.0`; the real identity of such a build is in the display version, which is
the branch or pull request plus the short commit hash.

### Provenance

Release builds record the commit they came from in
`WEB-INF/classes/git.properties` inside the WAR, and the enforcer rule that
demands a clean checkout is active.

This needs the Maven invocations to run in a particular order, which
`build-wars.sh` documents and implements. `ant version` rewrites two tracked
files, `src/java/edu/stanford/epadd/Version.java` and
`WebContent/version.jsp`, so running it leaves the checkout dirty. The
provenance is therefore captured by a separate `mvn validate` while the checkout
is still pristine, and the packaging run is told to keep that file rather than
regenerate it from the dirtied tree.

Untracking those two generated files would remove the need for that dance, but
it would also change how the project builds in an IDE, so it has been left
alone.

## Artifacts

| Asset | Platforms | Bundles a Java runtime |
| --- | --- | --- |
| `epadd-<version>-<platform>.deb` | Linux x86_64 | yes |
| `epadd-<version>-<platform>.msi` | Windows x86_64, Windows arm64 | yes |
| `epadd-<version>-<platform>.dmg` | macOS arm64, macOS x86_64 | yes |
| `epadd-<version>.exe` | Windows, either architecture | no |
| `epadd-<version>-standalone.jar` | any | no |
| `epadd-<version>.war` | any servlet container | no |

Each asset also exists in an `epadd-discovery-` form, except `epadd.exe`, which
the launch4j configuration in `epadd-launcher` only defines for the full
edition.

Two caveats are worth knowing about:

* The installers bundle the runner's JDK, so each one only runs on the
  architecture it was built on. That is why Windows and macOS are each built
  twice. `epadd.exe` is the exception: launch4j emits a 32 bit x86 binary
  whatever it runs on, so it is built once, on x86_64.
* The `.deb` has its dependencies resolved against the distribution it was built
  on, currently Ubuntu 24.04. On an older distribution the standalone jar is the
  better option.

## Signing

Signing is skipped when the certificates are not configured, so the pipeline
works before the secrets exist. Unsigned macOS artifacts are refused by
Gatekeeper until the user allows them by hand, and unsigned Windows artifacts
raise a SmartScreen warning.

Set these repository secrets to turn it on:

| Secret | Purpose |
| --- | --- |
| `MACOS_CERTIFICATE` | base64 encoded Developer ID Application `.p12` |
| `MACOS_CERTIFICATE_PASSWORD` | its passphrase |
| `MACOS_SIGNING_IDENTITY` | the identity name, for example `Developer ID Application: Example (ABCDE12345)` |
| `MACOS_NOTARY_APPLE_ID` | Apple ID of the notarization account |
| `MACOS_NOTARY_TEAM_ID` | Apple Developer team identifier |
| `MACOS_NOTARY_PASSWORD` | app specific password for that Apple ID |
| `WINDOWS_CERTIFICATE` | base64 encoded code signing `.p12` or `.pfx` |
| `WINDOWS_CERTIFICATE_PASSWORD` | its passphrase |

The macOS secrets and the Windows secrets are independent: configuring one
platform does not require the other.

Notarization uploads the disk image to Apple and sends the Apple ID with it,
which is why it is a secret rather than a variable. Windows signing sends the
hash of each file, and nothing else, to a timestamp authority, so that the
signature stays valid after the certificate expires. The authority defaults to
DigiCert's and can be changed with the `WINDOWS_TIMESTAMP_URL` repository
variable.

Pull requests are built from the `pull_request` event and never request signing,
so a build triggered by a fork has no access to any of this.

## Scripts

The workflows stay thin and the logic lives in `.github/scripts`, so it can be
read and run outside Actions:

| Script | Job |
| --- | --- |
| `lib.sh` | helpers shared by the rest, sourced rather than executed |
| `compute-version.sh` | derives the two version strings from the git ref |
| `build-wars.sh` | builds both WARs, in the order provenance requires |
| `build-standalone.sh` | builds the launcher and assembles the standalone jars |
| `build-windows-exe.sh` | wraps the full standalone jar into `epadd.exe` |
| `build-installers.sh` | builds both editions' installers for the current platform |
| `package-installer.sh` | one jpackage invocation, with the per-format arguments |
| `sign-windows.sh` | signs an executable or installer with signtool |
| `sign-macos.sh` | imports the signing identity, and notarizes a disk image |
| `release-notes.sh` | writes the body of the GitHub release |

Four details in there exist because of the environment rather than by choice.
Maven and Ant are installed as `.cmd` wrappers on the Windows runners, which
bash does not resolve from the bare name, so every call goes through
`resolve_tool`. The JDK cannot read MSYS paths such as `/d/a/epadd/epadd`, so
absolute paths handed to a Java tool go through `native_path` first. The
workflows invoke each script as `bash <script>` rather than directly, because a
Windows checkout does not carry the executable bit.

And the scripts have to run on the bash 3.2 that macOS still ships, which is
stricter than it looks: expanding an empty array under `set -u` is an unbound
variable error there, though it is harmless on bash 4.4 and later. Optional
Maven arguments are therefore spelled out at each call site instead of being
collected into an array that might end up empty. Associative arrays, `mapfile`
and `printf -v` are all unavailable for the same reason.

## Running it locally

The scripts need Maven, Ant, a JDK 17 and the three checkouts side by side:

```
mkdir epadd-build && cd epadd-build
git clone https://github.com/darrendignam/epadd.git
git clone https://github.com/ePADD/epadd-launcher.git
git clone https://github.com/ePADD/epadd-settings.git

cd epadd
.github/scripts/build-wars.sh --version 11.1.4-local
cd ../epadd-launcher
../epadd/.github/scripts/build-standalone.sh --workspace "$(cd .. && pwd)"
cd ..
epadd/.github/scripts/build-installers.sh \
    --type deb --platform linux-x86_64 \
    --version 11.1.4-local --app-version 11.1.4 \
    --launcher-dir "$PWD/epadd-launcher" \
    --icon <a png, icns or ico> --dest "$PWD/dist"
```

This differs from `README.build.txt` in one way: that document has
`epadd-settings` checked out in the home directory, because
`epadd-launcher/build.xml` reads the models from `${user.home}`.
`build-standalone.sh` overrides that property instead, so the checkout can live
in the workspace next to the others.
