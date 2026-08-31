/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.oritech.adapter.bluemap523;

import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePackExtension;
import de.bluecolored.bluemap.core.util.Key;
import io.github.janguenter.bluemap.oritech.model.GeoModelCompiler;
import io.github.janguenter.bluemap.oritech.model.OritechGeoModel;
import io.github.janguenter.bluemap.oritech.profile.ExactOritechArtifactDetector;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Exact-artifact GEO compilation and target-only renderer wrapping. */
final class OritechResourceExtension implements ResourcePackExtension {

    private final ResourcePack resourcePack;
    private final BlockRendererType renderer;
    private final OritechRuntime runtime;
    private Map<String, OritechGeoModel> models = Map.of();

    OritechResourceExtension(
            ResourcePack resourcePack,
            BlockRendererType renderer,
            OritechRuntime runtime
    ) {
        this.resourcePack = resourcePack;
        this.renderer = renderer;
        this.runtime = runtime;
    }

    @Override
    public void loadResources(Iterable<Path> roots) {
        if (Boolean.getBoolean("bluemap.oritech.disabled")) {
            runtime.inactive("operator-disabled");
            return;
        }
        Path artifact = ExactOritechArtifactDetector.find(roots).orElse(null);
        if (artifact == null) {
            runtime.inactive("exact-oritech-artifact-not-found");
            return;
        }
        try {
            models = loadModels(artifact);
            if (models.size() != OritechCatalog.BLOCK_MODELS.size()) {
                throw new IOException("Oritech GEO roster changed");
            }
            runtime.activate();
        } catch (IOException | RuntimeException exception) {
            models = Map.of();
            runtime.inactive("geo-compile-" + exception.getClass().getSimpleName());
        }
    }

    @Override
    public Set<Key> collectUsedTextureKeys() {
        return OritechCatalog.TEXTURES;
    }

    @Override
    public void bake() {
        if (!runtime.active() || models.isEmpty()) {
            return;
        }
        VariantRendererCatalog variants = VariantRendererCatalog.wrap(resourcePack, renderer);
        runtime.install(resourcePack, models, variants);
        System.out.println("BlueMap Oritech add-on active: compiled " + models.size()
                + " static GEO models and wrapped " + variants.size() + " variants.");
    }

    private static Map<String, OritechGeoModel> loadModels(Path artifact) throws IOException {
        Map<String, OritechGeoModel> result = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(artifact.toFile())) {
            for (String model : OritechCatalog.BLOCK_MODELS.values()) {
                String path = "assets/oritech/geo/block/models/" + model + ".geo.json";
                ZipEntry entry = zip.getEntry(path);
                if (entry == null || entry.isDirectory()) {
                    throw new IOException("missing installed Oritech GEO " + model);
                }
                byte[] raw;
                try (java.io.InputStream input = zip.getInputStream(entry)) {
                    raw = input.readAllBytes();
                }
                result.put(model, GeoModelCompiler.compile(model, raw));
            }
        }
        return Map.copyOf(result);
    }
}
