package net.ravadael.tablemod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.ravadael.tablemod.menu.AlchemyTableMenu;

public class SelectAlchemyResultPacket {
    private final ItemStack selectedOutput;

    public SelectAlchemyResultPacket(ItemStack selectedOutput) {
        this.selectedOutput = selectedOutput;
    }

    public SelectAlchemyResultPacket(RegistryFriendlyByteBuf buf) {
        this.selectedOutput = ItemStack.STREAM_CODEC.decode(buf);
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        ItemStack.STREAM_CODEC.encode(buf, selectedOutput);
    }

    public static void handle(SelectAlchemyResultPacket message, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            if (context.getSender() != null && context.getSender().containerMenu instanceof AlchemyTableMenu menu) {
                menu.setSelectedOutput(message.selectedOutput);
            }
        });
        context.setPacketHandled(true);
    }
}
