package dev.worldblock.bloomery.recipe;

import dev.worldblock.bloomery.registry.ModRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class BloomeryRecipe extends AbstractCookingRecipe {
    public BloomeryRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
        super(ModRecipes.BLOOMING_TYPE.get(), group, category, ingredient, result, experience, cookingTime);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BLOOMING_SERIALIZER.get();
    }
}
