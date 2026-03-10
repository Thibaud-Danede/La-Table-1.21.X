package net.ravadael.tablemod;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.ravadael.tablemod.block.ModBlocks;
import net.ravadael.tablemod.menu.ModMenuTypes;
import net.ravadael.tablemod.screen.AlchemyTableScreen;

@Mod.EventBusSubscriber(modid = TableMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {

            // --- Enregistre la screen de ton bloc ---
            MenuScreens.register(ModMenuTypes.ALCHEMY_TABLE_MENU.get(), AlchemyTableScreen::new);

            // --- Indique que le bloc utilise une render layer CUTOUT ---
            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.ALCHEMY_TABLE.get(),
                    RenderType.cutout()
            );
        });
    }
}
