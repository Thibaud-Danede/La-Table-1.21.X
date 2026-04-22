package net.ravadael.tablemod.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ravadael.tablemod.TableMod;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, TableMod.MOD_ID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, TableMod.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlchemyRecipe>> ALCHEMY_SERIALIZER =
            SERIALIZERS.register("alchemy", () -> new AlchemyRecipeSerializer());

    public static final DeferredHolder<RecipeType<?>, RecipeType<AlchemyRecipe>> ALCHEMY_RECIPE_TYPE =
            RECIPE_TYPES.register("alchemy", () -> {
                AlchemyRecipeType.INSTANCE = AlchemyRecipeType.INSTANCE;
                return AlchemyRecipeType.INSTANCE;
            });


    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        RECIPE_TYPES.register(eventBus);
    }
}
