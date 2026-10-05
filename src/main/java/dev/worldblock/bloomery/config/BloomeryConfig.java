package dev.worldblock.bloomery.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class BloomeryConfig {
    public static final ModConfigSpec SPEC;
    public static final BloomeryConfig INSTANCE;

    public final ModConfigSpec.ConfigValue<List<? extends String>> disabledFurnaceOres;
    public final ModConfigSpec.BooleanValue disableInBlastFurnace;
    public final ModConfigSpec.ConfigValue<List<? extends String>> bloomeryOres;
    public final ModConfigSpec.IntValue packedMudCookingTime;
    public final ModConfigSpec.IntValue mudBrickCookingTime;
    public final ModConfigSpec.IntValue maxStackSize;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new BloomeryConfig(builder);
        SPEC = builder.build();
    }

    private BloomeryConfig(ModConfigSpec.Builder builder) {
        builder.push("furnace_disabling");
        disabledFurnaceOres = builder
                .comment("List of item IDs or tags to disable smelting in normal furnaces (e.g. 'minecraft:raw_iron', 'minecraft:iron_ore', '#c:ores/iron').")
                .defineList("disabledFurnaceOres", List.of(
                        "minecraft:raw_iron",
                        "minecraft:raw_copper",
                        "minecraft:iron_ore",
                        "minecraft:deepslate_iron_ore",
                        "minecraft:copper_ore",
                        "minecraft:deepslate_copper_ore"
                ), o -> o instanceof String);

        disableInBlastFurnace = builder
                .comment("Whether to also disable smelting the above ores in blast furnaces (default: false, blast furnace still allowed).")
                .define("disableInBlastFurnace", false);
        builder.pop();

        builder.push("bloomery");
        bloomeryOres = builder
                .comment("List of recipes/ores that can be smelted in the Bloomery. Format: 'input_item -> output_item' or just 'input_item' to auto-detect smelting output.")
                .defineList("bloomeryOres", List.of(
                        "minecraft:raw_iron -> minecraft:iron_ingot",
                        "minecraft:iron_ore -> minecraft:iron_ingot",
                        "minecraft:deepslate_iron_ore -> minecraft:iron_ingot",
                        "minecraft:raw_copper -> minecraft:copper_ingot",
                        "minecraft:copper_ore -> minecraft:copper_ingot",
                        "minecraft:deepslate_copper_ore -> minecraft:copper_ingot"
                ), o -> o instanceof String);

        packedMudCookingTime = builder
                .comment("Base cooking time in ticks for the Packed Mud Bloomery (20 ticks = 1 second, 1200 ticks = 1 minute).")
                .defineInRange("packedMudCookingTime", 1200, 20, 72000);

        mudBrickCookingTime = builder
                .comment("Base cooking time in ticks for the Mud Brick Bloomery (600 ticks = 30 seconds, 2x as fast).")
                .defineInRange("mudBrickCookingTime", 600, 10, 72000);

        maxStackSize = builder
                .comment("Maximum stack size allowed in the Bloomery input and output slots (default: 16).")
                .defineInRange("maxStackSize", 16, 1, 64);
        builder.pop();
    }
}
