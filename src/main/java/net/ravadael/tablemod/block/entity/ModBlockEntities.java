package net.ravadael.tablemod.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.ravadael.tablemod.TableMod;
import net.ravadael.tablemod.block.ModBlocks;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TableMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<AlchemyTableBlockEntity>> ALCHEMY_TABLE_BE =
        BLOCK_ENTITIES.register("alchemy_table",
            () -> BlockEntityType.Builder.of(
                AlchemyTableBlockEntity::new,
                ModBlocks.ALCHEMY_TABLE.get()
            ).build(null));
}