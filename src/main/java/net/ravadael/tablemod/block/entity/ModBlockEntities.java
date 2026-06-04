package net.ravadael.tablemod.block.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ravadael.tablemod.TableMod;
import net.ravadael.tablemod.block.ModBlocks;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TableMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlchemyTableBlockEntity>> ALCHEMY_TABLE_BE =
            BLOCK_ENTITIES.register("alchemy_table",
                    () -> BlockEntityType.Builder.of(
                            AlchemyTableBlockEntity::new,
                            ModBlocks.ALCHEMY_TABLE.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutomaticAlchemyTableBlockEntity>> AUTOMATIC_ALCHEMY_TABLE_BE =
            BLOCK_ENTITIES.register("automatic_alchemy_table",
                    () -> BlockEntityType.Builder.of(
                            AutomaticAlchemyTableBlockEntity::new,
                            ModBlocks.AUTOMATIC_ALCHEMY_TABLE.get()
                    ).build(null));

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                AUTOMATIC_ALCHEMY_TABLE_BE.get(),
                (be, side) -> be.getItemHandler(side)
        );
    }
}
