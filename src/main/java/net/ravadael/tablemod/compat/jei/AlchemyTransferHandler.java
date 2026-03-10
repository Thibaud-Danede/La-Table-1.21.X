package net.ravadael.tablemod.compat.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.ravadael.tablemod.menu.AlchemyTableMenu;
import net.ravadael.tablemod.menu.ModMenuTypes;
import net.ravadael.tablemod.recipe.AlchemyRecipe;

import java.util.Optional;

public class AlchemyTransferHandler implements IRecipeTransferHandler<AlchemyTableMenu, AlchemyRecipe> {

    @Override
    public Optional<MenuType<AlchemyTableMenu>> getMenuType() {
        return Optional.of(ModMenuTypes.ALCHEMY_TABLE_MENU.get());
    }

    @Override
    public Class<? extends AlchemyTableMenu> getContainerClass() {
        return AlchemyTableMenu.class;
    }

    @Override
    public RecipeType<AlchemyRecipe> getRecipeType() {
        return TableModJEIPlugin.ALCHEMY_TYPE;
    }

    @Override
    public IRecipeTransferError transferRecipe(
            AlchemyTableMenu menu,
            AlchemyRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            Player player,
            boolean maxTransfer,
            boolean doTransfer
    ) {
        // Pas de transfert auto pour l’instant
        return null;
    }
}
