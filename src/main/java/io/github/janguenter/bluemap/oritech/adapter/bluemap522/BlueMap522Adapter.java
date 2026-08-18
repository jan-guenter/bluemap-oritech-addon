/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.oritech.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.block.BlockRenderer;
import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import de.bluecolored.bluemap.core.world.mca.blockentity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;

/** BlueMap 5.22 internal ABI registration boundary. */
public final class BlueMap522Adapter {

    private static final OritechRuntime RUNTIME = OritechRuntime.INSTANCE;
    private static final BlockRendererType RENDERER = new BlockRendererType.Impl(
            Key.parse("bluemap_oritech:static_geo"), BlueMap522Adapter::createRenderer
    );
    private static final ResourcePack.Extension<OritechResourceExtension> EXTENSION =
            new OritechResourceExtensionType(RENDERER, RUNTIME);
    private static final List<BlockEntityType> BLOCK_ENTITIES = createBlockEntityTypes();

    private BlueMap522Adapter() {
    }

    public static synchronized boolean install() {
        if (!canRegister(BlockRendererType.REGISTRY, RENDERER)
                || !canRegister(ResourcePack.Extension.REGISTRY, EXTENSION)) {
            RUNTIME.inactive("registry-collision");
            return false;
        }
        for (BlockEntityType type : BLOCK_ENTITIES) {
            if (!canRegister(BlockEntityType.REGISTRY, type)) {
                RUNTIME.inactive("block-entity-registry-collision");
                return false;
            }
        }
        if (!register(BlockRendererType.REGISTRY, RENDERER)
                || !register(ResourcePack.Extension.REGISTRY, EXTENSION)) {
            return false;
        }
        for (BlockEntityType type : BLOCK_ENTITIES) {
            if (!register(BlockEntityType.REGISTRY, type)) {
                return false;
            }
        }
        return true;
    }

    private static BlockRenderer createRenderer(
            ResourcePack pack,
            TextureGallery gallery,
            RenderSettings settings
    ) {
        try {
            return new OritechRenderer(pack, gallery, settings, RUNTIME, RUNTIME.data(pack));
        } catch (Error error) {
            OritechRuntime.throwIfFatal(error);
            RUNTIME.inactive("renderer-construction-" + error.getClass().getSimpleName());
            return BlockRendererType.DEFAULT.create(pack, gallery, settings);
        } catch (RuntimeException exception) {
            RUNTIME.inactive("renderer-construction-" + exception.getClass().getSimpleName());
            return BlockRendererType.DEFAULT.create(pack, gallery, settings);
        }
    }

    private static List<BlockEntityType> createBlockEntityTypes() {
        List<BlockEntityType> result = new ArrayList<>();
        for (String id : OritechCatalog.BLOCK_ENTITIES.keySet()) {
            result.add(new BlockEntityType.Impl(Key.parse(id), OritechBlockEntityData.class));
        }
        return List.copyOf(result);
    }

    private static <T extends Keyed> boolean canRegister(Registry<T> registry, T candidate) {
        T existing = registry.get(candidate.getKey());
        return existing == null || existing == candidate;
    }

    private static <T extends Keyed> boolean register(Registry<T> registry, T candidate) {
        T existing = registry.get(candidate.getKey());
        if (existing == null) {
            registry.register(candidate);
            existing = registry.get(candidate.getKey());
        }
        return existing == candidate;
    }
}
