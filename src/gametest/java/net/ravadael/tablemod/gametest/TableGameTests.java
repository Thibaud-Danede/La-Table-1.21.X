package net.ravadael.tablemod.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ravadael.tablemod.block.ModBlocks;
import net.ravadael.tablemod.block.entity.AlchemyTableBlockEntity;

@GameTestHolder("tablemod")
@PrefixGameTestTemplate(false)
public class TableGameTests {
    static {
        StructureUtils.testStructuresDir = System.getProperty("tablemod.testStructures");
    }

    @GameTest(template = "empty")
    public static void manualTableAndOptionalRegistration(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ALCHEMY_TABLE.get());
        helper.assertTrue(helper.getBlockEntity(pos) instanceof AlchemyTableBlockEntity,
                "Manual table must load with or without Create");
        boolean create = ModList.get().isLoaded("create");
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(
                ResourceLocation.parse("tablemod:automatic_alchemy_table")) == create,
                "Automatic table must only be registered with Create");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(
                ResourceLocation.parse("tablemod:automatic_alchemy_table")).isPresent() == create,
                "Automatic crafting recipe must only exist with Create");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beltWithoutSelection(GameTestHelper helper) {
        if (ModList.get().isLoaded("create")) CreateBeltTests.withoutSelection(helper);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void incompatibleBeltItem(GameTestHelper helper) {
        if (ModList.get().isLoaded("create")) CreateBeltTests.incompatibleItem(helper);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beltResumesAfterSelection(GameTestHelper helper) {
        if (ModList.get().isLoaded("create")) CreateBeltTests.resumesAfterSelection(helper);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fullOutputBlocksBelt(GameTestHelper helper) {
        if (ModList.get().isLoaded("create")) CreateBeltTests.fullOutput(helper);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void simulatedInsertionAndSave(GameTestHelper helper) {
        if (ModList.get().isLoaded("create")) CreateBeltTests.simulationAndSave(helper);
        helper.succeed();
    }
}
