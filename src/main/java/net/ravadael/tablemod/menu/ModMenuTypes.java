package net.ravadael.tablemod.menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.ravadael.tablemod.TableMod;
import net.minecraft.world.flag.FeatureFlags;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, TableMod.MOD_ID);

    public static final RegistryObject<MenuType<AlchemyTableMenu>> ALCHEMY_TABLE_MENU =
            MENUS.register("alchemy_table_menu", () ->
                    new MenuType<>(AlchemyTableMenu::new, FeatureFlags.VANILLA_SET));
}