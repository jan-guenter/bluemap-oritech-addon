/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.oritech.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.MaxCapacityReachedException;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.map.hires.block.BlockRenderer;
import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import io.github.janguenter.bluemap.oritech.model.OritechGeoModel;

import java.util.IdentityHashMap;
import java.util.Map;

/** Replaces Oritech's entity-animated placeholder with its static installed GEO. */
final class OritechRenderer implements BlockRenderer {

    private static final ThreadLocal<Boolean> STOCK_FALLBACK =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final ResourcePack resourcePack;
    private final TextureGallery textures;
    private final RenderSettings settings;
    private final OritechRuntime runtime;
    private final OritechRuntime.PackData data;
    private final GeoMeshEmitter emitter;
    private final CtmEmitter ctmEmitter;
    private final Map<BlockRendererType, BlockRenderer> hosts = new IdentityHashMap<>();

    OritechRenderer(
            ResourcePack resourcePack,
            TextureGallery textures,
            RenderSettings settings,
            OritechRuntime runtime,
            OritechRuntime.PackData data
    ) {
        this.resourcePack = resourcePack;
        this.textures = textures;
        this.settings = settings;
        this.runtime = runtime;
        this.data = data;
        emitter = new GeoMeshEmitter(resourcePack, textures, settings);
        ctmEmitter = new CtmEmitter(resourcePack, textures, settings);
    }

    @Override
    public void render(
            BlockNeighborhood block,
            Variant variant,
            TileModelView target,
            Color mapColor
    ) {
        int start = target.getStart();
        try {
            if (!renderGeo(block, target, mapColor)) {
                stock(block, variant, target, mapColor);
            }
        } catch (MaxCapacityReachedException exception) {
            throw exception;
        } catch (Error error) {
            OritechRuntime.throwIfFatal(error);
            target.getTileModel().reset(start);
            target.initialize(start);
            runtime.inactive("renderer-" + error.getClass().getSimpleName());
            stockSafely(block, variant, target, mapColor, start);
        } catch (RuntimeException exception) {
            target.getTileModel().reset(start);
            target.initialize(start);
            runtime.report("renderer-" + exception.getClass().getSimpleName());
            stockSafely(block, variant, target, mapColor, start);
        }
    }

    private boolean renderGeo(
            BlockNeighborhood block,
            TileModelView target,
            Color mapColor
    ) {
        if (!runtime.active() || data == null) {
            return false;
        }
        String blockId = block.getBlockState().getId().getFormatted();
        Map<String, String> properties = block.getBlockState().getProperties();
        if (OritechCatalog.ALWAYS_INVISIBLE.contains(blockId)) {
            return true;
        }
        if (OritechCatalog.USED_MACHINE_CORES.contains(blockId)) {
            return "true".equals(properties.get("core_used"));
        }
        String ctmBase = OritechCatalog.ctmBase(blockId);
        if (ctmBase != null) {
            return ctmEmitter.emit(blockId, ctmBase, block, target, mapColor);
        }
        String modelName = OritechCatalog.model(blockId);
        OritechGeoModel model = data.models().get(modelName);
        if (model == null) {
            return false;
        }
        if (properties.containsKey("machine_assembled")
                && !"true".equals(properties.get("machine_assembled"))) {
            return false;
        }
        int color = block.getBlockEntity() instanceof OritechBlockEntityData entity
                ? entity.colorOrdinal() : 0;
        String facing = properties.getOrDefault("facing", "north");
        return emitter.emit(model, color, facing, block, target, mapColor);
    }

    private void stock(
            BlockNeighborhood block,
            Variant variant,
            TileModelView target,
            Color mapColor
    ) {
        if (STOCK_FALLBACK.get()) {
            return;
        }
        STOCK_FALLBACK.set(Boolean.TRUE);
        try {
            BlockRendererType type = data == null
                    ? BlockRendererType.DEFAULT : data.variants().original(variant);
            hosts.computeIfAbsent(
                    type, found -> found.create(resourcePack, textures, settings)
            ).render(block, variant, target, mapColor);
        } finally {
            STOCK_FALLBACK.set(Boolean.FALSE);
        }
    }

    private void stockSafely(
            BlockNeighborhood block,
            Variant variant,
            TileModelView target,
            Color mapColor,
            int start
    ) {
        try {
            stock(block, variant, target, mapColor);
        } catch (Error error) {
            OritechRuntime.throwIfFatal(error);
            target.getTileModel().reset(start);
            target.initialize(start);
            runtime.report("stock-fallback-" + error.getClass().getSimpleName());
        } catch (RuntimeException exception) {
            target.getTileModel().reset(start);
            target.initialize(start);
            runtime.report("stock-fallback-" + exception.getClass().getSimpleName());
        }
    }
}
