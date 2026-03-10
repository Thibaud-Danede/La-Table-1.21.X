package net.ravadael.tablemod.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.ravadael.tablemod.TableMod;
import net.ravadael.tablemod.block.ModBlocks;
import net.ravadael.tablemod.recipe.AlchemyRecipe;

public class AlchemyRecipeCategory implements IRecipeCategory<AlchemyRecipe> {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TableMod.MOD_ID, "textures/gui/alchemy_jei.png");

    private final IDrawable background;
    private final IDrawable icon;

    public AlchemyRecipeCategory(IGuiHelper gui) {
        this.background = gui.createDrawable(TEXTURE, 0, 0, 98, 54);
        this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ALCHEMY_TABLE.get()));
    }

    @Override
    public RecipeType<AlchemyRecipe> getRecipeType() {
        return TableModJEIPlugin.ALCHEMY_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.tablemod.alchemy");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AlchemyRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 11, 10).addIngredients(recipe.getInput());

        if (recipe.isCatalystRequired() && !recipe.getCatalyst().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 11, 29).addIngredients(recipe.getCatalyst());
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 72, 19).addItemStacks(recipe.getResults());
    }
}
