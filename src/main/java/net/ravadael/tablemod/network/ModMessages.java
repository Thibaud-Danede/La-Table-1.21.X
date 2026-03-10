package net.ravadael.tablemod.network;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import net.ravadael.tablemod.TableMod;

public class ModMessages {
    public static final SimpleChannel INSTANCE = ChannelBuilder
            .named(TableMod.MOD_ID + ":messages")
            .networkProtocolVersion(1)
            .simpleChannel();

    private ModMessages() {
    }

    public static void register() {
        INSTANCE.messageBuilder(SelectAlchemyResultPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectAlchemyResultPacket::toBytes)
                .decoder(SelectAlchemyResultPacket::new)
                .consumerMainThread(SelectAlchemyResultPacket::handle)
                .add();
    }

    public static void sendSelectResult(ItemStack result) {
        INSTANCE.send(new SelectAlchemyResultPacket(result), PacketDistributor.SERVER.noArg());
    }
}
