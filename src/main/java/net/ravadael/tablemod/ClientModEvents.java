package net.ravadael.tablemod;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.ravadael.tablemod.block.ModBlocks;
import net.ravadael.tablemod.menu.ModMenuTypes;
import net.ravadael.tablemod.screen.AlchemyTableScreen;

@EventBusSubscriber(modid = TableMod.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ALCHEMY_TABLE_MENU.get(), AlchemyTableScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                ItemBlockRenderTypes.setRenderLayer(
                        ModBlocks.ALCHEMY_TABLE.get(),
                        RenderType.cutout()
                ));
    }
}
