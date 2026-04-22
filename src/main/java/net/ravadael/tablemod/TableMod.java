package net.ravadael.tablemod;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.ravadael.tablemod.block.ModBlocks;
import net.ravadael.tablemod.block.entity.ModBlockEntities;
import net.ravadael.tablemod.item.ModCreativeModTabs;
import net.ravadael.tablemod.item.ModItems;
import net.ravadael.tablemod.menu.ModMenuTypes;
import net.ravadael.tablemod.network.ModMessages;
import net.ravadael.tablemod.recipe.ModRecipes;
import org.slf4j.Logger;

@Mod(TableMod.MOD_ID)
public class TableMod {
    public static final String MOD_ID = "tablemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TableMod(IEventBus modEventBus) {
        ModCreativeModTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        modEventBus.addListener(ModMessages::register);
        modEventBus.addListener(this::addCreative);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        ModMenuTypes.MENUS.register(modEventBus);
        ModRecipes.register(modEventBus);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        int recipeCount = event.getServer().getRecipeManager().getAllRecipesFor(ModRecipes.ALCHEMY_RECIPE_TYPE.get()).size();
        LOGGER.info("Server alchemy recipes loaded: {}", recipeCount);
    }
}
