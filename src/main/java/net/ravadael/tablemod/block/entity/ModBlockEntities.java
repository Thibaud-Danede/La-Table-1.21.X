package net.ravadael.tablemod.block.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ravadael.tablemod.TableMod;
import net.ravadael.tablemod.block.ModBlocks;
import net.ravadael.tablemod.compat.CreateIntegration;

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
            ModList.get().isLoaded("create")
                    ? BLOCK_ENTITIES.register("automatic_alchemy_table", CreateIntegration::createBlockEntityType) : null;

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (ModList.get().isLoaded("create")) {
            CreateIntegration.registerCapabilities(event);
        }
    }
}
