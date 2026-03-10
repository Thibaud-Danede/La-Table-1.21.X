package net.ravadael.tablemod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlchemyRecipeSerializer implements RecipeSerializer<AlchemyRecipe> {
    private static final MapCodec<AlchemyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(AlchemyRecipe::getInput),
            Ingredient.CODEC.optionalFieldOf("catalyst", Ingredient.EMPTY).forGetter(AlchemyRecipe::getCatalyst),
            Codec.BOOL.optionalFieldOf("catalyst_required", false).forGetter(AlchemyRecipe::isCatalystRequired),
            ItemStack.CODEC.listOf().optionalFieldOf("results", List.of()).forGetter(AlchemyRecipe::getRawResults),
            ResourceLocation.CODEC.optionalFieldOf("results_tag").forGetter(recipe -> Optional.ofNullable(recipe.getResultsTag()))
    ).apply(instance, AlchemyRecipeSerializer::fromCodec));

    private static final StreamCodec<RegistryFriendlyByteBuf, AlchemyRecipe> STREAM_CODEC = StreamCodec.of(
            AlchemyRecipeSerializer::toNetwork,
            AlchemyRecipeSerializer::fromNetwork
    );

    private static AlchemyRecipe fromCodec(Ingredient input, Ingredient catalyst, boolean catalystRequired,
                                           List<ItemStack> results, Optional<ResourceLocation> resultsTag) {
        return new AlchemyRecipe(input, catalyst, catalystRequired, results, resultsTag.orElse(null));
    }

    private static AlchemyRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
        Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
        Ingredient catalyst = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
        boolean catalystRequired = buf.readBoolean();

        int size = buf.readVarInt();
        List<ItemStack> results = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            results.add(ItemStack.STREAM_CODEC.decode(buf));
        }

        ResourceLocation resultsTag = buf.readOptional(ResourceLocation.STREAM_CODEC::decode).orElse(null);
        return new AlchemyRecipe(input, catalyst, catalystRequired, results, resultsTag);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buf, AlchemyRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getInput());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getCatalyst());
        buf.writeBoolean(recipe.isCatalystRequired());

        buf.writeVarInt(recipe.getRawResults().size());
        for (ItemStack result : recipe.getRawResults()) {
            ItemStack.STREAM_CODEC.encode(buf, result);
        }

        buf.writeOptional(Optional.ofNullable(recipe.getResultsTag()), ResourceLocation.STREAM_CODEC::encode);
    }

    @Override
    public MapCodec<AlchemyRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, AlchemyRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
