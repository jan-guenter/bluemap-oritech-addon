/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.oritech.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Key;

/** Resource-pack extension factory registered before resource loading. */
final class OritechResourceExtensionType
        implements ResourcePack.Extension<OritechResourceExtension> {

    private static final Key KEY = Key.parse("bluemap_oritech:prototype");

    private final BlockRendererType renderer;
    private final OritechRuntime runtime;

    OritechResourceExtensionType(BlockRendererType renderer, OritechRuntime runtime) {
        this.renderer = renderer;
        this.runtime = runtime;
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public OritechResourceExtension create(ResourcePack pack) {
        return new OritechResourceExtension(pack, renderer, runtime);
    }
}
