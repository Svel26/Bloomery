package dev.worldblock.bloomery.recipe;

import dev.worldblock.bloomery.Bloomery;
import dev.worldblock.bloomery.config.BloomeryConfig;
import dev.worldblock.bloomery.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class BloomeryRecipeManager implements PreparableReloadListener {
    public record BloomingResult(ItemStack result, int cookTime, float experience) {}

    private static final Map<Item, ItemStack> CAPTURED_SMELTING_RESULTS = new ConcurrentHashMap<>();

    public static boolean isPrimityLoaded() {
        ResourceLocation castIronLoc = ResourceLocation.fromNamespaceAndPath("primity", "cast_iron_ingot");
        return BuiltInRegistries.ITEM.containsKey(castIronLoc) &&
                BuiltInRegistries.ITEM.get(castIronLoc) != BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air"));
    }

    public static ItemStack getIronBloomingOutput() {
        if (isPrimityLoaded()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("primity", "cast_iron_ingot"));
            return new ItemStack(item);
        }
        return new ItemStack(Items.IRON_INGOT);
    }

    public static ItemStack getGoldBloomingOutput() {
        if (isPrimityLoaded()) {
            return new ItemStack(Items.GOLD_NUGGET);
        }
        return new ItemStack(Items.GOLD_INGOT);
    }

    public static void reinitializeDefaults() {
        ItemStack ironOut = getIronBloomingOutput();
        ItemStack goldOut = getGoldBloomingOutput();

        CAPTURED_SMELTING_RESULTS.put(Items.RAW_IRON, ironOut.copy());
        CAPTURED_SMELTING_RESULTS.put(Items.IRON_ORE, ironOut.copy());
        CAPTURED_SMELTING_RESULTS.put(Items.DEEPSLATE_IRON_ORE, ironOut.copy());
        CAPTURED_SMELTING_RESULTS.put(Items.RAW_COPPER, new ItemStack(Items.COPPER_INGOT));
        CAPTURED_SMELTING_RESULTS.put(Items.COPPER_ORE, new ItemStack(Items.COPPER_INGOT));
        CAPTURED_SMELTING_RESULTS.put(Items.DEEPSLATE_COPPER_ORE, new ItemStack(Items.COPPER_INGOT));
        CAPTURED_SMELTING_RESULTS.put(Items.RAW_GOLD, goldOut.copy());
        CAPTURED_SMELTING_RESULTS.put(Items.GOLD_ORE, goldOut.copy());
        CAPTURED_SMELTING_RESULTS.put(Items.DEEPSLATE_GOLD_ORE, goldOut.copy());

        if (isPrimityLoaded()) {
            Item castIronBlock = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("primity", "cast_iron_block"));
            CAPTURED_SMELTING_RESULTS.put(castIronBlock, new ItemStack(Items.IRON_INGOT));
            Item castIronIngot = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("primity", "cast_iron_ingot"));
            CAPTURED_SMELTING_RESULTS.put(castIronIngot, new ItemStack(Items.IRON_NUGGET));
        }
    }

    static {
        // Guaranteed static defaults available immediately upon mod initialization
        reinitializeDefaults();
    }

    private final ReloadableServerResources serverResources;
    private final RegistryAccess registryAccess;

    public BloomeryRecipeManager(ReloadableServerResources serverResources, RegistryAccess registryAccess) {
        this.serverResources = serverResources;
        this.registryAccess = registryAccess;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
                                          ProfilerFiller prepProfiler, ProfilerFiller reloadProfiler,
                                          Executor bgExecutor, Executor gameExecutor) {
        return stage.wait(null).thenRunAsync(() -> {
            RecipeManager recipeManager = this.serverResources.getRecipeManager();
            filterAndInjectRecipes(recipeManager, this.registryAccess);
        }, gameExecutor);
    }

    public static void filterAndInjectRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        reinitializeDefaults();

        List<String> disabledOres = (List<String>) BloomeryConfig.INSTANCE.disabledFurnaceOres.get();
        boolean disableBlast = BloomeryConfig.INSTANCE.disableInBlastFurnace.get();
        List<String> bloomeryOreConfigs = (List<String>) BloomeryConfig.INSTANCE.bloomeryOres.get();

        List<RecipeHolder<?>> kept = new ArrayList<>();
        int disabledCount = 0;

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            RecipeType<?> type = recipe.getType();

            boolean isSmelting = (type == RecipeType.SMELTING);
            boolean isBlasting = (type == RecipeType.BLASTING);

            if (isSmelting || (isBlasting && disableBlast)) {
                if (matchesAnyDisabled(recipe, disabledOres, registryAccess)) {
                    disabledCount++;
                    NonNullList<Ingredient> ingredients = recipe.getIngredients();
                    if (!ingredients.isEmpty()) {
                        for (ItemStack stack : ingredients.get(0).getItems()) {
                            ItemStack out = recipe.getResultItem(registryAccess);
                            if (!out.isEmpty()) {
                                CAPTURED_SMELTING_RESULTS.put(stack.getItem(), out.copy());
                            }
                        }
                    }
                    continue;
                }
            }

            kept.add(holder);
        }

        // Dynamically synchronize any existing BLOOMING_TYPE recipes with captured outputs
        // (so that if Primity changes raw iron smelting output to cast iron, blooming recipes match!)
        List<RecipeHolder<?>> synchronizedKept = new ArrayList<>();
        for (RecipeHolder<?> holder : kept) {
            if (holder.value().getType() == ModRecipes.BLOOMING_TYPE.get()) {
                BloomeryRecipe bloomeryRecipe = (BloomeryRecipe) holder.value();
                NonNullList<Ingredient> ingredients = bloomeryRecipe.getIngredients();
                if (!ingredients.isEmpty()) {
                    ItemStack captured = null;
                    for (ItemStack stack : ingredients.get(0).getItems()) {
                        if (CAPTURED_SMELTING_RESULTS.containsKey(stack.getItem())) {
                            captured = CAPTURED_SMELTING_RESULTS.get(stack.getItem());
                            break;
                        }
                    }
                    if (captured != null && !ItemStack.isSameItemSameComponents(bloomeryRecipe.getResultItem(registryAccess), captured)) {
                        BloomeryRecipe updated = new BloomeryRecipe(
                                bloomeryRecipe.getGroup(),
                                CookingBookCategory.MISC,
                                ingredients.get(0),
                                captured.copy(),
                                bloomeryRecipe.getExperience(),
                                bloomeryRecipe.getCookingTime()
                        );
                        synchronizedKept.add(new RecipeHolder<>(holder.id(), updated));
                        continue;
                    }
                }
            }
            synchronizedKept.add(holder);
        }
        kept = synchronizedKept;

        Set<Item> existingBloomingInputs = new HashSet<>();
        for (RecipeHolder<?> holder : kept) {
            if (holder.value().getType() == ModRecipes.BLOOMING_TYPE.get()) {
                NonNullList<Ingredient> ingredients = holder.value().getIngredients();
                if (!ingredients.isEmpty()) {
                    for (ItemStack stack : ingredients.get(0).getItems()) {
                        existingBloomingInputs.add(stack.getItem());
                    }
                }
            }
        }

        int injectedCount = 0;
        int defaultCookingTime = BloomeryConfig.INSTANCE.packedMudCookingTime.get();

        // Inject primity:cast_iron_block -> minecraft:iron_ingot if Primity is loaded
        if (isPrimityLoaded()) {
            Item castIronBlock = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("primity", "cast_iron_block"));
            if (castIronBlock != BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air")) && !existingBloomingInputs.contains(castIronBlock)) {
                BloomeryRecipe blockRecipe = new BloomeryRecipe(
                        "iron_ingot",
                        CookingBookCategory.MISC,
                        Ingredient.of(castIronBlock),
                        new ItemStack(Items.IRON_INGOT),
                        0.7F,
                        defaultCookingTime
                );
                kept.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(Bloomery.MOD_ID, "injected_primity_cast_iron_block"), blockRecipe));
                existingBloomingInputs.add(castIronBlock);
                injectedCount++;
            }

            Item castIronIngot = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("primity", "cast_iron_ingot"));
            if (castIronIngot != BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air")) && !existingBloomingInputs.contains(castIronIngot)) {
                BloomeryRecipe ingotRecipe = new BloomeryRecipe(
                        "iron_nugget",
                        CookingBookCategory.MISC,
                        Ingredient.of(castIronIngot),
                        new ItemStack(Items.IRON_NUGGET),
                        0.2F,
                        defaultCookingTime
                );
                kept.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(Bloomery.MOD_ID, "injected_primity_cast_iron_ingot"), ingotRecipe));
                existingBloomingInputs.add(castIronIngot);
                injectedCount++;
            }
        }

        for (String configEntry : bloomeryOreConfigs) {
            configEntry = configEntry.trim();
            if (configEntry.isEmpty()) continue;

            String inputStr;
            String outputStr = null;

            if (configEntry.contains("->")) {
                String[] parts = configEntry.split("->");
                inputStr = parts[0].trim();
                outputStr = parts[1].trim();
            } else {
                inputStr = configEntry;
            }

            ResourceLocation inputLoc = ResourceLocation.tryParse(inputStr);
            if (inputLoc == null) continue;

            Item inputItem = BuiltInRegistries.ITEM.get(inputLoc);
            if (inputItem == BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air"))) {
                continue;
            }

            if (existingBloomingInputs.contains(inputItem)) {
                continue;
            }

            ItemStack outputStack = ItemStack.EMPTY;
            if (outputStr != null) {
                // If Primity is active and this is raw_iron, prefer Primity's cast iron unless explicitly customized
                if (inputItem == Items.RAW_IRON && isPrimityLoaded() && outputStr.equals("minecraft:iron_ingot")) {
                    outputStack = getIronBloomingOutput();
                } else {
                    ResourceLocation outLoc = ResourceLocation.tryParse(outputStr);
                    if (outLoc != null) {
                        Item outItem = BuiltInRegistries.ITEM.get(outLoc);
                        if (outItem != BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air"))) {
                            outputStack = new ItemStack(outItem);
                        }
                    }
                }
            } else if (CAPTURED_SMELTING_RESULTS.containsKey(inputItem)) {
                outputStack = CAPTURED_SMELTING_RESULTS.get(inputItem).copy();
            }

            if (!outputStack.isEmpty()) {
                Ingredient ingredient = Ingredient.of(inputItem);
                BloomeryRecipe bloomeryRecipe = new BloomeryRecipe(
                        "",
                        CookingBookCategory.MISC,
                        ingredient,
                        outputStack,
                        0.7F,
                        defaultCookingTime
                );

                ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                        Bloomery.MOD_ID, "injected_" + inputLoc.getNamespace() + "_" + inputLoc.getPath());

                kept.add(new RecipeHolder<>(recipeId, bloomeryRecipe));
                existingBloomingInputs.add(inputItem);
                injectedCount++;
            }
        }

        recipeManager.replaceRecipes(kept);
        Bloomery.LOGGER.info("Bloomery: Disabled {} furnace recipe(s), injected {} dynamic bloomery recipe(s). Total recipes: {}",
                disabledCount, injectedCount, kept.size());
    }

    public static @Nullable BloomingResult findRecipe(ItemStack input, Level level) {
        if (input.isEmpty()) return null;
        Item item = input.getItem();
        int cookTime = BloomeryConfig.INSTANCE.packedMudCookingTime.get();

        // 1. Direct captured output mapping
        if (CAPTURED_SMELTING_RESULTS.containsKey(item)) {
            return new BloomingResult(CAPTURED_SMELTING_RESULTS.get(item).copy(), cookTime, 0.7F);
        }

        // 2. Common tags
        TagKey<Item> rawIronTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "raw_materials/iron"));
        TagKey<Item> ironOresTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/iron"));
        if (input.is(rawIronTag) || input.is(ironOresTag)) {
            return new BloomingResult(getIronBloomingOutput(), cookTime, 0.7F);
        }

        TagKey<Item> rawCopperTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "raw_materials/copper"));
        TagKey<Item> copperOresTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/copper"));
        if (input.is(rawCopperTag) || input.is(copperOresTag)) {
            return new BloomingResult(new ItemStack(Items.COPPER_INGOT), cookTime, 0.7F);
        }

        TagKey<Item> rawGoldTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "raw_materials/gold"));
        TagKey<Item> goldOresTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/gold"));
        if (input.is(rawGoldTag) || input.is(goldOresTag)) {
            return new BloomingResult(getGoldBloomingOutput(), cookTime, 0.7F);
        }

        // 3. Name heuristic for modded ores/raw materials
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        if (path.contains("raw_iron") || path.contains("iron_ore")) {
            return new BloomingResult(getIronBloomingOutput(), cookTime, 0.7F);
        }
        if (path.contains("raw_copper") || path.contains("copper_ore")) {
            return new BloomingResult(new ItemStack(Items.COPPER_INGOT), cookTime, 0.7F);
        }
        if (path.contains("raw_gold") || path.contains("gold_ore")) {
            return new BloomingResult(getGoldBloomingOutput(), cookTime, 0.7F);
        }
        if (path.equals("cast_iron_block")) {
            return new BloomingResult(new ItemStack(Items.IRON_INGOT), cookTime, 0.7F);
        }
        if (path.equals("cast_iron_ingot")) {
            return new BloomingResult(new ItemStack(Items.IRON_NUGGET), cookTime, 0.2F);
        }

        // 4. Fallback: Smelting recipes in RecipeManager
        if (level != null) {
            for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
                if (holder.value().getType() == RecipeType.SMELTING) {
                    NonNullList<Ingredient> ingredients = holder.value().getIngredients();
                    if (!ingredients.isEmpty() && ingredients.get(0).test(input)) {
                        ItemStack out = holder.value().getResultItem(level.registryAccess());
                        if (!out.isEmpty()) {
                            return new BloomingResult(out.copy(), cookTime, 0.7F);
                        }
                    }
                }
            }
        }

        return null;
    }

    private static boolean matchesAnyDisabled(Recipe<?> recipe, List<String> disabledOres, RegistryAccess registryAccess) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) return false;

        Ingredient input = ingredients.get(0);
        for (ItemStack stack : input.getItems()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            for (String entry : disabledOres) {
                entry = entry.trim();
                if (entry.startsWith("#")) {
                    ResourceLocation tagLoc = ResourceLocation.tryParse(entry.substring(1));
                    if (tagLoc != null) {
                        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLoc);
                        if (stack.is(tagKey)) return true;
                    }
                } else {
                    ResourceLocation itemLoc = ResourceLocation.tryParse(entry);
                    if (itemLoc != null && id.equals(itemLoc)) {
                        return true;
                    }
                    if (id.getPath().equalsIgnoreCase(entry)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
