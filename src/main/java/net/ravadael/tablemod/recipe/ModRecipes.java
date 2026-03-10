package net.ravadael.tablemod.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.ravadael.tablemod.TableMod;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TableMod.MOD_ID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, TableMod.MOD_ID);

    public static final RegistryObject<RecipeSerializer<AlchemyRecipe>> ALCHEMY_SERIALIZER =
            SERIALIZERS.register("alchemy", () -> new AlchemyRecipeSerializer());

    public static final RegistryObject<RecipeType<AlchemyRecipe>> ALCHEMY_RECIPE_TYPE =
            RECIPE_TYPES.register("alchemy", () -> {
                AlchemyRecipeType.INSTANCE = AlchemyRecipeType.INSTANCE;
                return AlchemyRecipeType.INSTANCE;
            });


    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        RECIPE_TYPES.register(eventBus);
    }
}
