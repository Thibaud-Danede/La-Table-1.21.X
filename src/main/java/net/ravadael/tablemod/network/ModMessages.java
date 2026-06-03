package net.ravadael.tablemod.network;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.resources.ResourceLocation;

public class ModMessages {
    private ModMessages() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                SelectAlchemyResultPayload.TYPE,
                SelectAlchemyResultPayload.STREAM_CODEC,
                SelectAlchemyResultPayload::handleOnServer
        );
    }

    public static void sendSelectResult(ResourceLocation recipeId, ItemStack result) {
        PacketDistributor.sendToServer(new SelectAlchemyResultPayload(recipeId, result));
    }
}
