package net.ravadael.tablemod.recipe;

import net.minecraft.world.item.crafting.RecipeType;

public class AlchemyRecipeType implements RecipeType<AlchemyRecipe> {
    public static AlchemyRecipeType INSTANCE = new AlchemyRecipeType();
    public static final String ID = "alchemy";

    private AlchemyRecipeType() {}

    @Override
    public String toString() {
        return ID;
    }
}


