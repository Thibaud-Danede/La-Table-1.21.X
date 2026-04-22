package net.ravadael.tablemod.network;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

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

    public static void sendSelectResult(ItemStack result) {
        PacketDistributor.sendToServer(new SelectAlchemyResultPayload(result));
    }
}
