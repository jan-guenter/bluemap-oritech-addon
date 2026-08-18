/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.oritech.adapter.bluemap522;

import de.bluecolored.bluemap.core.util.Key;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact Oritech 1.2.10 block, block-entity, GEO and paint routing. */
final class OritechCatalog {

    static final Map<String, String> BLOCK_MODELS = blockModels();
    static final Map<String, String> BLOCK_ENTITIES = blockEntities();
    static final Map<String, String> CTM_BLOCKS = ctmBlocks();
    static final Set<String> ALWAYS_INVISIBLE = Set.of(
            "oritech:hangar_door_helper", "oritech:tech_door_hinge"
    );
    static final Set<String> USED_MACHINE_CORES = Set.of(
            "oritech:machine_core_1", "oritech:machine_core_2",
            "oritech:machine_core_3", "oritech:machine_core_4",
            "oritech:machine_core_5", "oritech:machine_core_6",
            "oritech:machine_core_7"
    );
    static final Set<String> BLOCKS = blocks();
    static final List<String> PAINTS = List.of(
            "", "diamond", "camo", "fluxite", "white",
            "industrial", "netherite", "redstone", "sculk"
    );
    static final Set<String> NON_COLORABLE = Set.of(
            "augment_application_block",
            "big_solar_panel_block",
            "enchanter_block",
            "enchantment_catalyst_block",
            "pipe_booster_block"
    );
    static final Set<Key> TEXTURES = textures();

    private OritechCatalog() {
    }

    static String model(String blockId) {
        return BLOCK_MODELS.get(blockId);
    }

    static String ctmBase(String blockId) {
        return CTM_BLOCKS.get(blockId);
    }

    static Key texture(String model, int colorOrdinal) {
        String suffix = NON_COLORABLE.contains(model)
                || colorOrdinal <= 0 || colorOrdinal >= PAINTS.size()
                ? "" : "colored/" + model + '_' + PAINTS.get(colorOrdinal);
        return Key.parse("oritech:block/models/" + (suffix.isEmpty() ? model : suffix));
    }

    private static Map<String, String> blockModels() {
        Map<String, String> result = new LinkedHashMap<>();
        add(result, "pulverizer_block");
        add(result, "fragment_forge_block");
        add(result, "assembler_block");
        add(result, "foundry_block");
        add(result, "cooler_block");
        add(result, "centrifuge_block");
        add(result, "atomic_forge_block");
        add(result, "powered_furnace_block");
        add(result, "refinery_block");
        add(result, "tainted_refinery_block");
        add(result, "refinery_module_block");
        result.put("oritech:augment_application_block", "augment_application_block");
        add(result, "bio_generator_block");
        add(result, "basic_generator_block");
        add(result, "fuel_generator_block");
        add(result, "lava_generator_block");
        add(result, "steam_engine_block");
        result.put("oritech:big_solar_panel_block", "big_solar_panel_block");
        add(result, "deep_drill_block");
        add(result, "drone_port_block");
        add(result, "treefeller_block");
        add(result, "enchanter_block");
        add(result, "pipe_booster_block");
        add(result, "enchantment_catalyst_block");
        add(result, "pump_block");
        add(result, "shrinker_block");
        result.put("oritech:unstable_container", "unstable_container");
        result.put("oritech:tech_door", "tech_door");
        return Map.copyOf(result);
    }

    private static Map<String, String> blockEntities() {
        Map<String, String> result = new LinkedHashMap<>();
        route(result, "pulverizer_entity", "pulverizer_block");
        route(result, "fragment_forge_entity", "fragment_forge_block");
        route(result, "assembler_entity", "assembler_block");
        route(result, "foundry_entity", "foundry_block");
        route(result, "cooler_entity", "cooler_block");
        route(result, "centrifuge_entity", "centrifuge_block");
        route(result, "atomic_forge_entity", "atomic_forge_block");
        route(result, "powered_furnace_entity", "powered_furnace_block");
        route(result, "refinery_entity", "refinery_block");
        route(result, "tainted_refinery_entity", "tainted_refinery_block");
        route(result, "refinery_module_entity", "refinery_module_block");
        route(result, "player_modifier_block_entity", "augment_application_block");
        route(result, "bio_generator_entity", "bio_generator_block");
        route(result, "basic_generator_entity", "basic_generator_block");
        route(result, "fuel_generator_entity", "fuel_generator_block");
        route(result, "lava_generator_entity", "lava_generator_block");
        route(result, "steam_engine_entity", "steam_engine_block");
        route(result, "big_solar_entity", "big_solar_panel_block");
        route(result, "deep_drill_entity", "deep_drill_block");
        route(result, "drone_port_entity", "drone_port_block");
        route(result, "treefeller_block_entity", "treefeller_block");
        route(result, "enchanter_block_entity", "enchanter_block");
        route(result, "pipe_booster_block_entity", "pipe_booster_block");
        route(result, "enchantment_catalyst_block_entity", "enchantment_catalyst_block");
        route(result, "pump_block", "pump_block");
        route(result, "shrinker_block_entity", "shrinker_block");
        route(result, "unstable_container_block_entity", "unstable_container");
        route(result, "tech_door_entity", "tech_door");
        return Map.copyOf(result);
    }

    private static Map<String, String> ctmBlocks() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("oritech:capacitor_addon_extender", "capacitor_ctm");
        result.put("oritech:carbon_plating_block", "carbon_plating_block");
        result.put("oritech:industrial_glass_block", "industrial_glass_block");
        result.put("oritech:iron_plating_block", "iron_plating_block");
        result.put("oritech:machine_plating_block", "machine_plating_block");
        result.put("oritech:nickel_plating_block", "nickel_plating_block");
        result.put("oritech:reactor_wall", "reactor_wall");
        return Map.copyOf(result);
    }

    private static void add(Map<String, String> target, String name) {
        target.put("oritech:" + name, name);
    }

    private static void route(Map<String, String> target, String entity, String block) {
        target.put("oritech:" + entity, "oritech:" + block);
    }

    private static Set<Key> textures() {
        Set<Key> result = new LinkedHashSet<>();
        for (String model : BLOCK_MODELS.values()) {
            int colorCount = NON_COLORABLE.contains(model) ? 1 : PAINTS.size();
            for (int color = 0; color < colorCount; color++) {
                result.add(texture(model, color));
            }
        }
        for (String base : CTM_BLOCKS.values()) {
            for (String role : List.of(
                    "particle", "empty", "center", "vertical", "horizontal"
            )) {
                result.add(Key.parse("oritech:block/" + base + '/' + role));
            }
        }
        return Set.copyOf(result);
    }

    private static Set<String> blocks() {
        Set<String> result = new LinkedHashSet<>(BLOCK_MODELS.keySet());
        result.addAll(CTM_BLOCKS.keySet());
        result.addAll(ALWAYS_INVISIBLE);
        result.addAll(USED_MACHINE_CORES);
        return Set.copyOf(result);
    }
}
