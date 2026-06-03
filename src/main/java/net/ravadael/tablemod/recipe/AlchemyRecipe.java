package net.ravadael.tablemod.recipe;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlchemyRecipe implements Recipe<AlchemyRecipeInput> {
    private final Ingredient input;
    private final Ingredient catalyst;
    private final boolean catalystRequired;
    private final List<ItemStack> results;
    @Nullable
    private final ResourceLocation resultsTag;
    private volatile List<ItemStack> resolvedResults;

    public AlchemyRecipe(Ingredient input, Ingredient catalyst, boolean catalystRequired, List<ItemStack> results,
                         @Nullable ResourceLocation resultsTag) {
        this.input = input;
        this.catalyst = catalyst;
        this.catalystRequired = catalystRequired;
        this.results = List.copyOf(results);
        this.resultsTag = resultsTag;
    }

    public Ingredient getInput() {
        return input;
    }

    public Ingredient getCatalyst() {
        return catalyst;
    }

    public boolean isCatalystRequired() {
        return catalystRequired;
    }

    public List<ItemStack> getRawResults() {
        return results;
    }

    @Nullable
    public ResourceLocation getResultsTag() {
        return resultsTag;
    }

    public List<ItemStack> getResults() {
        return getResults(getRegistryAccess());
    }

    public List<ItemStack> getResults(@Nullable HolderLookup.Provider registries) {
        if (resultsTag != null) {
            if (registries == null) {
                return resolvedResults != null ? resolvedResults : List.of();
            }

            if (resolvedResults == null || resolvedResults.isEmpty()) {
                resolvedResults = resolveResultsFromTag(registries);
            }

            return resolvedResults;
        }
        return results;
    }

    private List<ItemStack> resolveResultsFromTag(HolderLookup.Provider registries) {
        var itemRegistry = registries.lookupOrThrow(Registries.ITEM);
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, resultsTag);
        List<ItemStack> resolved = new ArrayList<>();
        for (var holder : itemRegistry.getOrThrow(tagKey)) {
            resolved.add(new ItemStack(holder));
        }
        return Collections.unmodifiableList(resolved);
    }

    @Nullable
    private static HolderLookup.Provider getRegistryAccess() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.registryAccess();
        }

        if (FMLEnvironment.dist == Dist.CLIENT) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                return minecraft.level.registryAccess();
            }
        }

        return null;
    }

    public boolean matchesInputOnly(AlchemyRecipeInput input) {
        return this.input.test(input.getItem(0));
    }

    @Override
    public boolean matches(AlchemyRecipeInput input, Level level) {
        if (!this.input.test(input.getItem(0))) {
            return false;
        }

        return !catalystRequired || this.catalyst.test(input.getItem(1));
    }

    @Override
    public ItemStack assemble(AlchemyRecipeInput input, HolderLookup.Provider registries) {
        List<ItemStack> list = getResults(registries);
        return list.isEmpty() ? ItemStack.EMPTY : list.get(0).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        List<ItemStack> list = getResults(registries);
        return list.isEmpty() ? ItemStack.EMPTY : list.get(0);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ALCHEMY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ALCHEMY_RECIPE_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public List<ItemStack> getFilteredResults(ItemStack input) {
        return getFilteredResults(input, getRegistryAccess());
    }

    public List<ItemStack> getFilteredResults(ItemStack input, @Nullable HolderLookup.Provider registries) {
        List<ItemStack> list = getResults(registries);
        if (input.isEmpty()) {
            return list;
        }

        boolean containsInput = false;
        for (ItemStack result : list) {
            if (ItemStack.isSameItemSameComponents(result, input)) {
                containsInput = true;
                break;
            }
        }

        if (!containsInput) {
            return list;
        }

        return list.stream()
                .filter(result -> !ItemStack.isSameItemSameComponents(result, input))
                .toList();
    }
}
