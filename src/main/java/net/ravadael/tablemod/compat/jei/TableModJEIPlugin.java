package net.ravadael.tablemod.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.ravadael.tablemod.TableMod;
import net.ravadael.tablemod.block.ModBlocks;
import net.ravadael.tablemod.recipe.AlchemyRecipe;
import net.ravadael.tablemod.recipe.ModRecipes;

@JeiPlugin
public class TableModJEIPlugin implements IModPlugin {
    public static final RecipeType<AlchemyRecipe> ALCHEMY_TYPE =
            RecipeType.create(TableMod.MOD_ID, "alchemy", AlchemyRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(TableMod.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new AlchemyRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registry) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            TableMod.LOGGER.info("JEI alchemy recipes: level is null during registration");
            return;
        }

        var recipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.ALCHEMY_RECIPE_TYPE.get())
                .stream()
                .map(holder -> holder.value())
                .toList();
        TableMod.LOGGER.info("JEI alchemy recipes registered: {}", recipes.size());
        registry.addRecipes(ALCHEMY_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
        registry.addRecipeCatalyst(new ItemStack(ModBlocks.ALCHEMY_TABLE.get()), ALCHEMY_TYPE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registry) {
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registry) {
        registry.addRecipeTransferHandler(new AlchemyTransferHandler(), ALCHEMY_TYPE);
    }
}
