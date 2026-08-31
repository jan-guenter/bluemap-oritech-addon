# BlueMap Oritech Add-on

[![CI](https://github.com/jan-guenter/bluemap-oritech-addon/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/jan-guenter/bluemap-oritech-addon/actions/workflows/ci.yml)

A small exact-profile BlueMap 5.23 feature-backport add-on for stable block appearance
missing from Oritech 1.2.10 in All the Mons 1.2.0.

## Status and compatibility

Version `0.1.0-alpha.3` is the owner-accepted migration release candidate for
this exact environment. It preserves the accepted `0.1.0-alpha.2` renderer
payload; `v0.1.0-alpha.2` is an unpublished failed release tag whose metadata
was not sealed. Compatibility outside these inputs is not asserted.

## Visual scope

The add-on targets only:

- All the Mons `1.2.0`, Minecraft `1.21.1`, NeoForge `21.1.248`, Java 21;
- BlueMap feature backport
  `5.22-feature.backport-5.23-stateless-java-web-server-46` at commit
  `7e07f4e74ec1e92a6ead9aa1e66054af3e133aac` and API commit
  `285c9a60eff3ac2b0cab308ce1058d1565be0971`;
- Oritech `1.2.10`, exact 10,990,540-byte JAR with SHA-256
  `7c17c78ac55d9cbb71a9108a2bec7e2659192e08c5a1b49026088f875dbde821`.

It interprets 28 installed Bedrock/GeckoLib machine GEOs in their default
static pose, applies their persisted nine-way paint, renders the seven
Oritech Athena-style connected-texture families, and suppresses multiblock
helper blocks. Animation, glow, displays, inventories, fill levels, and other
fast-changing state are intentionally outside the static map view. The
bespoke hangar-door and laser-arm poses are deferred from this prerelease.

Missing Oritech, a different artifact, or unsupported data leaves stock
BlueMap rendering unchanged. The add-on writes nothing to the world.

## Build and verification

Clone with `--recurse-submodules`, or initialize the two pinned support
modules in an existing checkout:

```bash
git submodule update --init --recursive -- \
  tooling/bluemap-addon-toolkit modules/bluemap-addon-adapter-api
gradle --no-daemon \
  -PbluemapSourcePath=/path/to/exact/bluemap \
  -PoritechJar=/path/to/oritech-neoforge-1.21.1-1.2.10.jar \
  clean prototypeCheck build \
  generatePomFileForAddonPublication \
  generateMetadataFileForAddonPublication
```

The settings preflight accepts only the committed support gitlinks and exact
BlueMap source identities. `prototypeCheck` verifies the exact installed
Oritech artifact, adapter boundary, archive boundaries, tests, and gallery.

Tagged releases publish production/source JARs, POM, Gradle module metadata,
and checksums on GitHub Releases and Maven coordinates
`io.github.jan-guenter:bluemap-oritech-addon:<version>` on GitHub Packages.

## Installation

Place `bluemap-oritech-addon-0.1.0-alpha.3.jar` in `config/bluemap/packs`,
make the exact Oritech JAR available to BlueMap's resource scan, restart, and
rerender the affected area. Do not place this add-on in `mods`.

The gallery datapack in the source tree is only a disposable visual-review
fixture and is deliberately excluded from release assets.

## License and provenance

This project is released under the [MIT License](LICENSE). It bundles no
Oritech, GeckoLib, or Athena resources or binaries. See [NOTICE.md](NOTICE.md),
[THIRD_PARTY.md](THIRD_PARTY.md), and
[provenance/upstreams.json](provenance/upstreams.json).
