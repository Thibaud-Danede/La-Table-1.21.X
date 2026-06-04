package net.ravadael.tablemod.menu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ravadael.tablemod.TableMod;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, TableMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AlchemyTableMenu>> ALCHEMY_TABLE_MENU =
            MENUS.register("alchemy_table_menu", () ->
                    new MenuType<>(AlchemyTableMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<AutomaticAlchemyTableMenu>> AUTOMATIC_ALCHEMY_TABLE_MENU =
            MENUS.register("automatic_alchemy_table_menu", () ->
                    IMenuTypeExtension.create(AutomaticAlchemyTableMenu::new));
}
