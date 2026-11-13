# Build assets

`epadd-icon-128.png` is the application icon jpackage uses for the Linux
installer, which accepts PNG only. Windows uses `epadd.ico` and macOS uses
`epadd.icns`, and both of those already exist in the epadd-launcher repository.

It is the 128x128 entry of that repository's `launch4j-3.50-win32/epadd.ico`,
the largest size that icon holds, converted to PNG. It is committed rather than
converted during the build for two reasons: the GitHub runners carry no image
tooling, and the `.icns` stores its larger sizes as JPEG 2000, which jpackage
cannot read.

To regenerate it if the launcher's icon changes:

    magick 'launch4j-3.50-win32/epadd.ico[4]' png32:epadd-icon-128.png
