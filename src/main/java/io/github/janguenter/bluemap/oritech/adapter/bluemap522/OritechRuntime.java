/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.oritech.adapter.bluemap522;

import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import io.github.janguenter.bluemap.oritech.model.OritechGeoModel;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Shared activation, compiled-model and bounded-diagnostic state. */
final class OritechRuntime {

    static final OritechRuntime INSTANCE = new OritechRuntime();

    private final AtomicBoolean active = new AtomicBoolean();
    private final AtomicInteger diagnostics = new AtomicInteger();
    private final Map<ResourcePack, PackData> packs = new WeakHashMap<>();

    private OritechRuntime() {
    }

    boolean active() {
        return active.get();
    }

    void activate() {
        active.set(true);
    }

    void inactive(String reason) {
        active.set(false);
        report("inactive-" + reason);
    }

    synchronized void install(
            ResourcePack pack,
            Map<String, OritechGeoModel> models,
            VariantRendererCatalog variants
    ) {
        packs.put(pack, new PackData(Map.copyOf(models), variants));
    }

    synchronized PackData data(ResourcePack pack) {
        return packs.get(pack);
    }

    void report(String reason) {
        if (diagnostics.incrementAndGet() <= 12) {
            System.err.println("BlueMap Oritech add-on: " + reason + '.');
        }
    }

    @SuppressWarnings("removal")
    static void throwIfFatal(Error error) {
        if (error instanceof OutOfMemoryError outOfMemory) {
            throw outOfMemory;
        }
        if (error instanceof ThreadDeath threadDeath) {
            throw threadDeath;
        }
    }

    record PackData(Map<String, OritechGeoModel> models, VariantRendererCatalog variants) {
    }
}
