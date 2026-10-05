package dev.worldblock.bloomery.registry;

import dev.worldblock.bloomery.Bloomery;
import dev.worldblock.bloomery.recipe.BloomeryRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCookingSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Bloomery.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Bloomery.MOD_ID);

    public static final Supplier<RecipeType<BloomeryRecipe>> BLOOMING_TYPE =
            RECIPE_TYPES.register("blooming", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Bloomery.MOD_ID, "blooming")));

    public static final Supplier<RecipeSerializer<BloomeryRecipe>> BLOOMING_SERIALIZER =
            RECIPE_SERIALIZERS.register("blooming", () -> new SimpleCookingSerializer<>(BloomeryRecipe::new, 1200));
}
