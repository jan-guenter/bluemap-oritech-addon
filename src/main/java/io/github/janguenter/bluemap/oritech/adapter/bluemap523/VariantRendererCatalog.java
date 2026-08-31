/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.oritech.adapter.bluemap523;

import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.Key;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/** Original renderer identity retained for fail-soft stock fallback. */
final class VariantRendererCatalog {

    private final Map<Variant, BlockRendererType> originals;

    private VariantRendererCatalog(Map<Variant, BlockRendererType> originals) {
        this.originals = Collections.unmodifiableMap(originals);
    }

    static VariantRendererCatalog wrap(ResourcePack pack, BlockRendererType wrapper) {
        IdentityHashMap<Variant, BlockRendererType> originals = new IdentityHashMap<>();
        for (String id : OritechCatalog.BLOCKS) {
            BlockState state = pack.getBlockStates().get(Key.parse(id));
            if (state == null) {
                continue;
            }
            state.forEach(variant -> {
                if (variant.getRenderer() != wrapper) {
                    originals.put(variant, variant.getRenderer());
                    variant.setRenderer(wrapper);
                }
            });
        }
        return new VariantRendererCatalog(originals);
    }

    BlockRendererType original(Variant variant) {
        return originals.getOrDefault(variant, BlockRendererType.DEFAULT);
    }

    int size() {
        return originals.size();
    }
}
