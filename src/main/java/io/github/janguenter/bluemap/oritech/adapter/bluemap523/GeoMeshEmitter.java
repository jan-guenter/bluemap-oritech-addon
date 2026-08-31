/* SPDX-License-Identifier: MIT */
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
import io.github.janguenter.bluemap.oritech.model.OritechGeoModel;
import io.github.janguenter.bluemap.oritech.model.OritechGeoModel.Quad;
import io.github.janguenter.bluemap.oritech.model.OritechGeoModel.Vec3;
import io.github.janguenter.bluemap.oritech.model.OritechGeoModel.Vertex;

/** Emits one installed Oritech GEO mesh with saved paint and block facing. */
final class GeoMeshEmitter {

    private static final Variant IDENTITY = new Variant(
            new ResourcePath<Model>("bluemap", "block/missing"), 0F, 0F, 0F
    );

    private final ResourcePack resourcePack;
    private final TextureGallery textures;
    private final RenderSettings settings;

    GeoMeshEmitter(ResourcePack resourcePack, TextureGallery textures, RenderSettings settings) {
        this.resourcePack = resourcePack;
        this.textures = textures;
        this.settings = settings;
    }

    boolean emit(
            OritechGeoModel model,
            int colorOrdinal,
            String facing,
            BlockNeighborhood block,
            TileModelView target,
            Color mapColor
    ) {
        Key textureKey = OritechCatalog.texture(model.name(), colorOrdinal);
        Texture texture = resourcePack.getTextures().get(textureKey);
        if (texture == null && colorOrdinal != 0) {
            textureKey = OritechCatalog.texture(model.name(), 0);
            texture = resourcePack.getTextures().get(textureKey);
        }
        if (texture == null) {
            return false;
        }
        int material = textures.get(textureKey);
        float topOpacity = 0F;
        for (Quad quad : model.quads()) {
            Vec3 normal = transformNormal(quad.normal(), facing);
            if (settings.isRenderTopOnly() && normal.y() <= 0D) {
                continue;
            }
            Direction direction = nearestDirection(normal);
            FaceLighting.Sample light = FaceLighting.sample(block, direction, IDENTITY);
            int visibleLight = settings.isCaveDetectionUsesBlockLight()
                    ? Math.max(light.sunlight(), light.blocklight()) : light.sunlight();
            if (block.isRemoveIfCave() && visibleLight == 0) {
                continue;
            }
            emitQuad(quad, facing, target, material, light);
            if (normal.y() > 0D) {
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
        if (mapColor.a > 0F) {
            mapColor.flatten().straight();
            mapColor.a = topOpacity;
        }
        return true;
    }

    private static void emitQuad(
            Quad quad,
            String facing,
            TileModelView target,
            int material,
            FaceLighting.Sample light
    ) {
        Vertex first = transform(quad.first(), facing);
        Vertex second = transform(quad.second(), facing);
        Vertex third = transform(quad.third(), facing);
        Vertex fourth = transform(quad.fourth(), facing);
        int start = target.add(2);
        TileModel mesh = target.getTileModel();
        positions(mesh, start, first, second, third);
        positions(mesh, start + 1, first, third, fourth);
        uvs(mesh, start, first, second, third);
        uvs(mesh, start + 1, first, third, fourth);
        for (int index = start; index < start + 2; index++) {
            mesh.setMaterialIndex(index, material);
            mesh.setColor(index, 1F, 1F, 1F);
            mesh.setAOs(index, 1F, 1F, 1F);
            mesh.setSunlight(index, light.sunlight());
            mesh.setBlocklight(index, light.blocklight());
        }
    }

    private static Vertex transform(Vertex vertex, String facing) {
        return new Vertex(transformPoint(vertex.position(), facing), vertex.u(), vertex.v());
    }

    private static Vec3 transformPoint(Vec3 point, String facing) {
        Vec3 transformed = switch (facing) {
            case "south" -> point.rotateY(180D);
            case "west" -> point.rotateY(90D);
            case "east" -> point.rotateY(-90D);
            case "up" -> point.rotateX(90D).add(new Vec3(0D, 0.5D, -0.5D));
            case "down" -> point.rotateX(-90D).add(new Vec3(0D, 0.5D, 0.5D));
            default -> point;
        };
        return transformed.add(new Vec3(0.5D, 0D, 0.5D));
    }

    private static Vec3 transformNormal(Vec3 normal, String facing) {
        return switch (facing) {
            case "south" -> normal.rotateY(180D);
            case "west" -> normal.rotateY(90D);
            case "east" -> normal.rotateY(-90D);
            case "up" -> normal.rotateX(90D);
            case "down" -> normal.rotateX(-90D);
            default -> normal;
        };
    }

    private static Direction nearestDirection(Vec3 normal) {
        double x = Math.abs(normal.x());
        double y = Math.abs(normal.y());
        double z = Math.abs(normal.z());
        if (y >= x && y >= z) {
            return normal.y() >= 0D ? Direction.UP : Direction.DOWN;
        }
        if (x >= z) {
            return normal.x() >= 0D ? Direction.EAST : Direction.WEST;
        }
        return normal.z() >= 0D ? Direction.SOUTH : Direction.NORTH;
    }

    private static void positions(
            TileModel mesh,
            int index,
            Vertex first,
            Vertex second,
            Vertex third
    ) {
        mesh.setPositions(
                index,
                (float) first.position().x(),
                (float) first.position().y(),
                (float) first.position().z(),
                (float) second.position().x(),
                (float) second.position().y(),
                (float) second.position().z(),
                (float) third.position().x(),
                (float) third.position().y(),
                (float) third.position().z()
        );
    }

    private static void uvs(
            TileModel mesh,
            int index,
            Vertex first,
            Vertex second,
            Vertex third
    ) {
        mesh.setUvs(
                index,
                first.u(), first.v(),
                second.u(), second.v(),
                third.u(), third.v()
        );
    }
}
