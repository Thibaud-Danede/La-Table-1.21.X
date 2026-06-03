package net.ravadael.tablemod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ravadael.tablemod.TableMod;
import net.ravadael.tablemod.menu.AlchemyTableMenu;

public record SelectAlchemyResultPayload(ResourceLocation recipeId, ItemStack selectedOutput) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SelectAlchemyResultPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TableMod.MOD_ID, "select_alchemy_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectAlchemyResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    SelectAlchemyResultPayload::recipeId,
                    ItemStack.STREAM_CODEC,
                    SelectAlchemyResultPayload::selectedOutput,
                    SelectAlchemyResultPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(SelectAlchemyResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() != null && context.player().containerMenu instanceof AlchemyTableMenu menu) {
                menu.setSelectedOutput(payload.recipeId(), payload.selectedOutput());
            }
        });
    }
}
