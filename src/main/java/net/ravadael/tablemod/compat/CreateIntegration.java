package net.ravadael.tablemod.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.ravadael.tablemod.block.ModBlocks;
import net.ravadael.tablemod.block.custom.AutomaticAlchemyTableBlock;
import net.ravadael.tablemod.block.entity.AutomaticAlchemyTableBlockEntity;
import net.ravadael.tablemod.block.entity.ModBlockEntities;
import net.ravadael.tablemod.menu.AutomaticAlchemyTableMenu;

/** Loaded only when Create is present, keeping its classes out of the manual table's startup. */
public final class CreateIntegration {
    private CreateIntegration() {
    }

    public static Block createAutomaticTable() {
        return new AutomaticAlchemyTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_PLANKS)
                .sound(SoundType.WOOD).noOcclusion());
    }

    public static BlockEntityType<AutomaticAlchemyTableBlockEntity> createBlockEntityType() {
        return BlockEntityType.Builder.of(AutomaticAlchemyTableBlockEntity::new,
                ModBlocks.AUTOMATIC_ALCHEMY_TABLE.get()).build(null);
    }

    public static MenuType<AutomaticAlchemyTableMenu> createMenuType() {
        return IMenuTypeExtension.create(AutomaticAlchemyTableMenu::new);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.AUTOMATIC_ALCHEMY_TABLE_BE.get(),
                AutomaticAlchemyTableBlockEntity::getItemHandler);
    }

    public static void selectOutput(AbstractContainerMenu menu, ResourceLocation recipeId, ItemStack output) {
        if (menu instanceof AutomaticAlchemyTableMenu automaticMenu) {
            automaticMenu.setSelectedOutput(recipeId, output);
        }
    }
}
