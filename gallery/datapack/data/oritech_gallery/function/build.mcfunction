scoreboard objectives add oritech_gallery dummy
function oritech_gallery:clear
fill 163 99 163 249 99 234 minecraft:smooth_stone

# All nine saved machine-paint ordinals on a compact single-block machine.
setblock 168 100 168 oritech:pulverizer_block[facing=north]
data merge block 168 100 168 {color:0s}
setblock 173 100 168 oritech:pulverizer_block[facing=north]
data merge block 173 100 168 {color:1s}
setblock 178 100 168 oritech:pulverizer_block[facing=north]
data merge block 178 100 168 {color:2s}
setblock 183 100 168 oritech:pulverizer_block[facing=north]
data merge block 183 100 168 {color:3s}
setblock 188 100 168 oritech:pulverizer_block[facing=north]
data merge block 188 100 168 {color:4s}
setblock 193 100 168 oritech:pulverizer_block[facing=north]
data merge block 193 100 168 {color:5s}
setblock 198 100 168 oritech:pulverizer_block[facing=north]
data merge block 198 100 168 {color:6s}
setblock 203 100 168 oritech:pulverizer_block[facing=north]
data merge block 203 100 168 {color:7s}
setblock 208 100 168 oritech:pulverizer_block[facing=north]
data merge block 208 100 168 {color:8s}

# Representative formed multiblock exteriors, spaced for their large GEOs.
setblock 170 100 185 oritech:assembler_block[facing=north,machine_assembled=true]
data merge block 170 100 185 {color:2s}
setblock 181 100 185 oritech:atomic_forge_block[facing=north,machine_assembled=true]
data merge block 181 100 185 {color:3s}
setblock 192 100 185 oritech:refinery_block[facing=north,machine_assembled=true]
data merge block 192 100 185 {color:5s}
setblock 204 100 185 oritech:tainted_refinery_block[facing=north,machine_assembled=true]
data merge block 204 100 185 {color:8s}
setblock 218 100 185 oritech:deep_drill_block[facing=north,machine_assembled=true]
data merge block 218 100 185 {color:6s}
setblock 234 100 185 oritech:drone_port_block[facing=north,machine_assembled=true]
data merge block 234 100 185 {color:1s}

# Distinct static exteriors; activity, contents and readouts are intentionally idle.
setblock 168 100 205 oritech:augment_application_block[facing=north,machine_assembled=true]
setblock 179 100 205 oritech:basic_generator_block[facing=north]
data merge block 179 100 205 {color:4s}
setblock 188 100 205 oritech:enchanter_block[facing=north]
setblock 197 100 205 oritech:shrinker_block[facing=north,machine_assembled=true]
data merge block 197 100 205 {color:7s}
setblock 208 100 205 oritech:big_solar_panel_block[machine_assembled=true]
setblock 220 103 205 oritech:unstable_container[machine_assembled=true]
data merge block 220 103 205 {color:2s}
setblock 234 100 205 oritech:tech_door[facing=north,open=false]

# Seven exact Athena CTM routes as separate 3x3 panels.
fill 168 100 224 170 102 224 oritech:carbon_plating_block
fill 176 100 224 178 102 224 oritech:industrial_glass_block
fill 184 100 224 186 102 224 oritech:iron_plating_block
fill 192 100 224 194 102 224 oritech:machine_plating_block
fill 200 100 224 202 102 224 oritech:nickel_plating_block
fill 208 100 224 210 102 224 oritech:capacitor_addon_extender
fill 216 100 224 218 102 224 oritech:reactor_wall
setblock 217 101 224 oritech:reactor_controller

scoreboard players set #ready oritech_gallery 1
tellraw @a [{"text":"Oritech BlueMap gallery built inside x160..255, z160..255.","color":"aqua"}]
