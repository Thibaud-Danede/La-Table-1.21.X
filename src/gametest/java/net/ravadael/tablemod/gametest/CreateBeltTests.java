package net.ravadael.tablemod.gametest;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ravadael.tablemod.block.ModBlocks;
import net.ravadael.tablemod.block.custom.AutomaticAlchemyTableBlock;
import net.ravadael.tablemod.block.entity.AutomaticAlchemyTableBlockEntity;

final class CreateBeltTests {
    private record Setup(GameTestHelper helper, BeltBlockEntity belt, AutomaticAlchemyTableBlockEntity table) {
        void tick(int count) {
            for (int i = 0; i < count; i++) {
                belt.getInventory().tick();
                AutomaticAlchemyTableBlockEntity.serverTick(helper.getLevel(), table.getBlockPos(),
                        table.getBlockState(), table);
            }
        }

        void assertRetained(Item expected) {
            var items = belt.getInventory().getTransportedItems();
            helper.assertTrue(items.size() == 1 && items.getFirst().stack.is(expected)
                    && items.getFirst().stack.getCount() == 1, "Item must remain intact on the belt");
            float position = items.getFirst().beltPosition;
            tick(20);
            helper.assertTrue(items.getFirst().beltPosition == position, "Item must stop at the belt endpoint");
            helper.assertTrue(table.getInventory().getStackInSlot(0).isEmpty(), "Rejected input must stay outside the table");
        }

        void select(String recipe, Item output) {
            table.rememberSelection(ResourceLocation.parse("tablemod:alchemy/" + recipe), new ItemStack(output));
        }
    }

    private static Setup setup(GameTestHelper helper, Item input) {
        BlockPos start = new BlockPos(1, 1, 2);
        BlockPos end = start.east();
        BlockPos tablePos = end.east();
        helper.setBlock(tablePos, ModBlocks.AUTOMATIC_ALCHEMY_TABLE.get().defaultBlockState()
                .setValue(AutomaticAlchemyTableBlock.FACING, Direction.SOUTH));
        var beltState = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:belt")).defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BeltBlock.SLOPE, BeltSlope.HORIZONTAL);
        helper.setBlock(start, beltState.setValue(BeltBlock.PART, BeltPart.START));
        helper.setBlock(end, beltState.setValue(BeltBlock.PART, BeltPart.END));
        BeltBlockEntity belt = (BeltBlockEntity) helper.getBlockEntity(start);
        BeltBlockEntity last = (BeltBlockEntity) helper.getBlockEntity(end);
        belt.setController(helper.absolutePos(start));
        last.setController(helper.absolutePos(start));
        belt.beltLength = last.beltLength = 2;
        belt.index = 0;
        last.index = 1;
        belt.setSpeed(64);
        if (belt.getMovementFacing() != Direction.EAST) belt.setSpeed(-64);
        helper.assertTrue(belt.getMovementFacing() == Direction.EAST, "Test belt must move toward the table");
        TransportedItemStack transported = new TransportedItemStack(new ItemStack(input));
        transported.beltPosition = transported.prevBeltPosition = 0.5F;
        belt.getInventory().addItem(transported);
        return new Setup(helper, belt, (AutomaticAlchemyTableBlockEntity) helper.getBlockEntity(tablePos));
    }

    static void withoutSelection(GameTestHelper helper) {
        Setup setup = setup(helper, Items.OAK_LOG);
        setup.tick(80);
        setup.assertRetained(Items.OAK_LOG);
    }

    static void incompatibleItem(GameTestHelper helper) {
        Setup setup = setup(helper, Items.OAK_LOG);
        setup.select("stone/stone", Items.GRANITE);
        setup.tick(80);
        setup.assertRetained(Items.OAK_LOG);
    }

    static void resumesAfterSelection(GameTestHelper helper) {
        Setup setup = setup(helper, Items.OAK_LOG);
        setup.tick(80);
        setup.assertRetained(Items.OAK_LOG);
        setup.select("wood/logs", Items.BIRCH_LOG);
        setup.tick(80);
        helper.assertTrue(setup.belt.getInventory().getTransportedItems().isEmpty(), "Compatible item should leave the belt");
        helper.assertTrue(setup.table.getInventory().getStackInSlot(2).is(Items.BIRCH_LOG), "Table must craft the selected output");
    }

    static void fullOutput(GameTestHelper helper) {
        Setup setup = setup(helper, Items.STONE);
        setup.select("stone/stone", Items.GRANITE);
        setup.table.getInventory().setStackInSlot(2, new ItemStack(Items.GRANITE));
        setup.tick(80);
        setup.assertRetained(Items.STONE);
        setup.table.getInventory().setStackInSlot(2, ItemStack.EMPTY);
        setup.tick(80);
        helper.assertTrue(setup.belt.getInventory().getTransportedItems().isEmpty(), "Belt should resume after output is cleared");
        helper.assertTrue(setup.table.getInventory().getStackInSlot(2).is(Items.GRANITE), "Output must be produced without loss");
    }

    static void simulationAndSave(GameTestHelper helper) {
        Setup setup = setup(helper, Items.STONE);
        setup.select("stone/stone", Items.GRANITE);
        var behaviour = BlockEntityBehaviour.get(helper.getLevel(), setup.table.getBlockPos(), DirectBeltInputBehaviour.TYPE);
        helper.assertTrue(behaviour != null, "Create must discover the table's direct belt input");
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            helper.getLevel().setBlock(setup.table.getBlockPos(), setup.table.getBlockState()
                    .setValue(AutomaticAlchemyTableBlock.FACING, facing), 3);
            Direction travel = facing.getClockWise().getOpposite();
            ItemStack stack = new ItemStack(Items.STONE, 4);
            ItemStack remainder = behaviour.handleInsertion(stack, travel, true);
            helper.assertTrue(remainder.getCount() == 3 && stack.getCount() == 4,
                    "Simulation must only accept one item and preserve the source stack");
            helper.assertTrue(setup.table.getInventory().getStackInSlot(0).isEmpty(), "Simulation must not mutate inventory");
            helper.assertTrue(behaviour.handleInsertion(stack, travel.getOpposite(), true).getCount() == 4,
                    "Wrong face must reject input");
        }
        Direction travel = setup.table.getBlockState().getValue(AutomaticAlchemyTableBlock.FACING).getClockWise().getOpposite();
        behaviour.handleInsertion(new ItemStack(Items.STONE), travel, false);
        var saved = setup.table.saveWithoutMetadata(helper.getLevel().registryAccess());
        var restored = new AutomaticAlchemyTableBlockEntity(setup.table.getBlockPos(), setup.table.getBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.getSelectedOutput().is(Items.GRANITE)
                && restored.getInventory().getStackInSlot(0).is(Items.STONE), "Inventory and selection must survive reload");
    }
}
