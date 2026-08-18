# BlueMap Oritech Add-on

[![CI](https://github.com/jan-guenter/bluemap-oritech-addon/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/jan-guenter/bluemap-oritech-addon/actions/workflows/ci.yml)

A small exact-profile BlueMap 5.22 add-on for the stable block appearance
missing from Oritech 1.2.10 in All the Mons 1.2.0.

## Status and compatibility

Version `0.1.0-alpha.1` is the owner-accepted prerelease for this exact
environment. Its production JAR is 64,472 bytes with SHA-256
`958ae6fa2ae5a17893cccc348d3a6ce90498ff16ded21a97b0575774b7698a8e`.
Compatibility outside these inputs is not asserted.

## Visual scope

The add-on targets only:

- All the Mons `1.2.0`, Minecraft `1.21.1`, NeoForge `21.1.248`, Java 21;
- BlueMap backport `5.22-agent.backport-5.22-mc1.21.1-2` at commit
  `9be321df995a1103808621d529eb72773e719d4d`;
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

```bash
gradle --no-daemon clean check build \
  generatePomFileForAddonPublication \
  generateMetadataFileForAddonPublication
```

`check` rejects any production JAR that differs from the owner-accepted size
or SHA-256. Tagged releases publish production/source JARs, POM, Gradle module
metadata, and checksums on GitHub Releases and Maven coordinates
`io.github.jan-guenter:bluemap-oritech-addon:<version>` on GitHub Packages.

## Installation

Place `bluemap-oritech-addon-0.1.0-alpha.1.jar` in `config/bluemap/packs`,
make the exact Oritech JAR available to BlueMap's resource scan, restart, and
rerender the affected area. Do not place this add-on in `mods`.

The gallery datapack in the source tree is only a disposable visual-review
fixture and is deliberately excluded from release assets.

## License and provenance

This project is released under the [MIT License](LICENSE). It bundles no
Oritech, GeckoLib, or Athena resources or binaries. See [NOTICE.md](NOTICE.md),
[THIRD_PARTY.md](THIRD_PARTY.md), and
[provenance/upstreams.json](provenance/upstreams.json).
