/*
 * SPDX-License-Identifier: MIT
 *
 * Small first-party adaptation of the accepted Athena CTM quadrant interpreter
 * used by the BlueMap Chisel add-on in this workspace.
 */
package io.github.janguenter.bluemap.oritech.adapter.bluemap523;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

import java.util.Set;

/** Renders the exact seven Oritech Athena CTM cubes from face-local neighbors. */
final class CtmEmitter {

    private static final Variant IDENTITY = new Variant(
            new ResourcePath<Model>("bluemap", "block/missing"), 0F, 0F, 0F
    );
    private static final Set<String> REACTOR_WALL_BLOCKS = Set.of(
            "oritech:reactor_wall",
            "oritech:reactor_absorber_port",
            "oritech:reactor_energy_port",
            "oritech:reactor_fuel_port",
            "oritech:reactor_redstone_port",
            "oritech:reactor_controller"
    );

    private final ResourcePack pack;
    private final TextureGallery textures;
    private final RenderSettings settings;

    CtmEmitter(ResourcePack pack, TextureGallery textures, RenderSettings settings) {
        this.pack = pack;
        this.textures = textures;
        this.settings = settings;
    }

    boolean emit(
            String blockId,
            String textureBase,
            BlockNeighborhood block,
            TileModelView target,
            Color mapColor
    ) {
        float topOpacity = 0F;
        for (Face face : Face.values()) {
            if (settings.isRenderTopOnly() && face != Face.UP) {
                continue;
            }
            if (connected(block, blockId, face.normal)) {
                continue;
            }
            FaceLighting.Sample light = FaceLighting.sample(
                    block, Direction.valueOf(face.name()), IDENTITY
            );
            int visibleLight = settings.isCaveDetectionUsesBlockLight()
                    ? Math.max(light.sunlight(), light.blocklight()) : light.sunlight();
            if (block.isRemoveIfCave() && visibleLight == 0) {
                continue;
            }
            Connections connections = connections(block, blockId, face);
            Quadrant[] quadrants = {
                    new Quadrant(0F, 0.5F, 0.5F, 1F,
                            role(connections.up, connections.left, connections.upLeft)),
                    new Quadrant(0.5F, 0.5F, 1F, 1F,
                            role(connections.up, connections.right, connections.upRight)),
                    new Quadrant(0F, 0F, 0.5F, 0.5F,
                            role(connections.down, connections.left, connections.downLeft)),
                    new Quadrant(0.5F, 0F, 1F, 0.5F,
                            role(connections.down, connections.right, connections.downRight))
            };
            for (Quadrant quadrant : quadrants) {
                Key textureKey = Key.parse(
                        "oritech:block/" + textureBase + '/' + quadrant.role
                );
                Texture texture = pack.getTextures().get(textureKey);
                if (texture == null) {
                    return false;
                }
                emitQuadrant(face, quadrant, textureKey, target, light);
                if (face == Face.UP) {
                    Color average = new Color().set(texture.getColorPremultiplied());
                    float lightFactor = Math.max(light.sunlight(), light.blocklight()) / 15F;
                    lightFactor = (1F - settings.getAmbientLight()) * lightFactor
                            + settings.getAmbientLight();
                    average.r *= lightFactor;
                    average.g *= lightFactor;
                    average.b *= lightFactor;
                    topOpacity = Math.max(topOpacity, average.a);
                    mapColor.add(average);
                }
            }
        }
        if (mapColor.a > 0F) {
            mapColor.flatten().straight();
            mapColor.a = topOpacity;
        }
        return true;
    }

    private static Connections connections(
            BlockNeighborhood block,
            String id,
            Face face
    ) {
        Vec up = face.up;
        Vec down = up.scale(-1);
        Vec right = face.right;
        Vec left = right.scale(-1);
        return new Connections(
                connected(block, id, up),
                connected(block, id, down),
                connected(block, id, left),
                connected(block, id, right),
                connected(block, id, up.add(left)),
                connected(block, id, up.add(right)),
                connected(block, id, down.add(left)),
                connected(block, id, down.add(right))
        );
    }

    private static boolean connected(BlockNeighborhood block, String id, Vec offset) {
        de.bluecolored.bluemap.core.world.block.ExtendedBlock neighborBlock =
                block.getNeighborBlock(offset.x, offset.y, offset.z);
        String neighbor = neighborBlock.getBlockState().getId().getFormatted();
        return "oritech:reactor_wall".equals(id)
                ? REACTOR_WALL_BLOCKS.contains(neighbor)
                : block.getBlockState().equals(neighborBlock.getBlockState());
    }

    private static String role(boolean vertical, boolean horizontal, boolean diagonal) {
        if (!vertical && !horizontal) {
            return "particle";
        }
        if (vertical && !horizontal) {
            return "vertical";
        }
        if (!vertical) {
            return "horizontal";
        }
        return diagonal ? "empty" : "center";
    }

    private void emitQuadrant(
            Face face,
            Quadrant quadrant,
            Key texture,
            TileModelView target,
            FaceLighting.Sample light
    ) {
        Point bottomLeft = point(face, quadrant.left, quadrant.bottom);
        Point bottomRight = point(face, quadrant.right, quadrant.bottom);
        Point topRight = point(face, quadrant.right, quadrant.top);
        Point topLeft = point(face, quadrant.left, quadrant.top);
        int start = target.add(2);
        TileModel mesh = target.getTileModel();
        mesh.setPositions(start,
                bottomLeft.x, bottomLeft.y, bottomLeft.z,
                bottomRight.x, bottomRight.y, bottomRight.z,
                topRight.x, topRight.y, topRight.z);
        mesh.setPositions(start + 1,
                bottomLeft.x, bottomLeft.y, bottomLeft.z,
                topRight.x, topRight.y, topRight.z,
                topLeft.x, topLeft.y, topLeft.z);
        mesh.setUvs(start,
                quadrant.left, 1F - quadrant.bottom,
                quadrant.right, 1F - quadrant.bottom,
                quadrant.right, 1F - quadrant.top);
        mesh.setUvs(start + 1,
                quadrant.left, 1F - quadrant.bottom,
                quadrant.right, 1F - quadrant.top,
                quadrant.left, 1F - quadrant.top);
        int material = textures.get(texture);
        for (int index = start; index < start + 2; index++) {
            mesh.setMaterialIndex(index, material);
            mesh.setColor(index, 1F, 1F, 1F);
            mesh.setAOs(index, 1F, 1F, 1F);
            mesh.setSunlight(index, light.sunlight());
            mesh.setBlocklight(index, light.blocklight());
        }
    }

    private static Point point(Face face, float horizontal, float vertical) {
        float horizontalScale = horizontal - 0.5F;
        float verticalScale = vertical - 0.5F;
        return new Point(
                0.5F + face.normal.x * 0.5F
                        + face.right.x * horizontalScale + face.up.x * verticalScale,
                0.5F + face.normal.y * 0.5F
                        + face.right.y * horizontalScale + face.up.y * verticalScale,
                0.5F + face.normal.z * 0.5F
                        + face.right.z * horizontalScale + face.up.z * verticalScale
        );
    }

    private enum Face {
        DOWN(new Vec(0, -1, 0), new Vec(1, 0, 0), new Vec(0, 0, 1)),
        UP(new Vec(0, 1, 0), new Vec(1, 0, 0), new Vec(0, 0, -1)),
        NORTH(new Vec(0, 0, -1), new Vec(-1, 0, 0), new Vec(0, 1, 0)),
        SOUTH(new Vec(0, 0, 1), new Vec(1, 0, 0), new Vec(0, 1, 0)),
        WEST(new Vec(-1, 0, 0), new Vec(0, 0, 1), new Vec(0, 1, 0)),
        EAST(new Vec(1, 0, 0), new Vec(0, 0, -1), new Vec(0, 1, 0));

        private final Vec normal;
        private final Vec right;
        private final Vec up;

        Face(Vec normal, Vec right, Vec up) {
            this.normal = normal;
            this.right = right;
            this.up = up;
        }
    }

    private record Vec(int x, int y, int z) {
        Vec scale(int factor) {
            return new Vec(x * factor, y * factor, z * factor);
        }

        Vec add(Vec other) {
            return new Vec(x + other.x, y + other.y, z + other.z);
        }
    }

    private record Point(float x, float y, float z) {
    }

    private record Quadrant(float left, float bottom, float right, float top, String role) {
    }

    private record Connections(
            boolean up,
            boolean down,
            boolean left,
            boolean right,
            boolean upLeft,
            boolean upRight,
            boolean downLeft,
            boolean downRight
    ) {
    }
}
