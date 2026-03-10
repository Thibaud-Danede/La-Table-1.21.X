package net.ravadael.tablemod.menu;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ravadael.tablemod.block.entity.AlchemyTableBlockEntity;
import net.ravadael.tablemod.recipe.AlchemyRecipe;
import net.ravadael.tablemod.recipe.AlchemyRecipeInput;
import net.ravadael.tablemod.recipe.ModRecipes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AlchemyTableMenu extends AbstractContainerMenu {
    private final SimpleContainer input = new SimpleContainer(2);
    private final SimpleContainer result = new SimpleContainer(1);
    private final ContainerLevelAccess access;
    private final Level level;

    private List<AlchemyRecipe> recipes = List.of();
    private ItemStack selectedOutput = ItemStack.EMPTY;
    private ItemStack lastInputItem = ItemStack.EMPTY;
    private ItemStack lastCatalystItem = ItemStack.EMPTY;

    public AlchemyTableMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level(), buf.readBlockPos());
    }

    public AlchemyTableMenu(int id, Inventory inv) {
        this(id, inv, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(BlockPos.ZERO));
    }

    public AlchemyTableMenu(int id, Inventory inv, Level level, BlockPos pos) {
        super(ModMenuTypes.ALCHEMY_TABLE_MENU.get(), id);
        this.level = level;
        this.access = ContainerLevelAccess.create(level, pos);

        this.addSlot(new Slot(input, 0, 20, 23) {
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(input);
            }
        });

        this.addSlot(new Slot(input, 1, 20, 42) {
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(input);
            }
        });

        this.addSlot(new Slot(result, 0, 143, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                stack.onCraftedBy(player.level(), player, stack.getCount());

                ItemStack in = input.getItem(0);
                in.shrink(1);
                if (in.isEmpty()) {
                    input.setItem(0, ItemStack.EMPTY);
                }

                AlchemyRecipe recipe = recipes.isEmpty() ? null : recipes.get(0);
                if (recipe != null && recipe.isCatalystRequired()) {
                    ItemStack catalyst = input.getItem(1);
                    catalyst.shrink(1);
                    if (catalyst.isEmpty()) {
                        input.setItem(1, ItemStack.EMPTY);
                    }
                }

                updateRecipes();
                assembleSelectedOutput();
                player.playSound(SoundEvents.BREWING_STAND_BREW, 0.3F, 1.0F);
                super.onTake(player, stack);
            }
        });

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot) {
            this.addSlot(new Slot(inv, hotbarSlot, 8 + hotbarSlot * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        ItemStack inputItem = input.getItem(0);
        ItemStack catalystItem = input.getItem(1);

        boolean changed = !ItemStack.isSameItemSameComponents(inputItem, lastInputItem)
                || !ItemStack.isSameItemSameComponents(catalystItem, lastCatalystItem);

        lastInputItem = inputItem.copy();
        lastCatalystItem = catalystItem.copy();

        if (changed) {
            selectedOutput = ItemStack.EMPTY;
        }

        updateRecipes();
    }

    private void updateRecipes() {
        ItemStack inputItem = input.getItem(0);
        if (inputItem.isEmpty()) {
            recipes = List.of();
            selectedOutput = ItemStack.EMPTY;
            result.setItem(0, ItemStack.EMPTY);
            broadcastChanges();
            return;
        }

        AlchemyRecipeInput recipeInput = new AlchemyRecipeInput(inputItem, input.getItem(1));
        List<AlchemyRecipe> valid = new ArrayList<>();
        for (var holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.ALCHEMY_RECIPE_TYPE.get())) {
            AlchemyRecipe recipe = holder.value();
            if (recipe.matchesInputOnly(recipeInput)) {
                valid.add(recipe);
            }
        }

        valid.sort(Comparator.comparing(recipe -> recipe.getResultItem(level.registryAccess()).getDisplayName().getString()));
        recipes = valid;
        assembleSelectedOutput();
    }

    public void assembleSelectedOutput() {
        if (selectedOutput.isEmpty()) {
            result.setItem(0, ItemStack.EMPTY);
            broadcastChanges();
            return;
        }

        ItemStack inputItem = input.getItem(0);
        if (inputItem.isEmpty()) {
            selectedOutput = ItemStack.EMPTY;
            result.setItem(0, ItemStack.EMPTY);
            broadcastChanges();
            return;
        }

        AlchemyRecipeInput recipeInput = new AlchemyRecipeInput(inputItem, input.getItem(1));
        for (AlchemyRecipe recipe : recipes) {
            for (ItemStack output : recipe.getFilteredResults(inputItem)) {
                if (ItemStack.isSameItemSameComponents(output, selectedOutput)) {
                    result.setItem(0, recipe.matches(recipeInput, level) ? output.copy() : ItemStack.EMPTY);
                    broadcastChanges();
                    return;
                }
            }
        }

        selectedOutput = ItemStack.EMPTY;
        result.setItem(0, ItemStack.EMPTY);
        broadcastChanges();
    }

    public void setSelectedOutput(ItemStack output) {
        this.selectedOutput = output.copy();
        assembleSelectedOutput();
    }

    public ItemStack getInputItem() {
        return input.getItem(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack original = stackInSlot.copy();

        if (index == 2) {
            if (selectedOutput.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack output = selectedOutput.copy();
            ItemStack inputItem = input.getItem(0);
            ItemStack catalyst = input.getItem(1);
            int maxCrafts = inputItem.getCount();

            if (!recipes.isEmpty() && recipes.get(0).isCatalystRequired()) {
                maxCrafts = Math.min(maxCrafts, catalyst.getCount());
            }

            maxCrafts = Math.min(maxCrafts, output.getMaxStackSize());
            boolean crafted = false;

            for (int i = 0; i < maxCrafts; i++) {
                if (!this.moveItemStackTo(output.copy(), 3, 39, true)) {
                    break;
                }

                inputItem.shrink(1);
                if (inputItem.isEmpty()) {
                    input.setItem(0, ItemStack.EMPTY);
                }

                if (!recipes.isEmpty() && recipes.get(0).isCatalystRequired()) {
                    catalyst.shrink(1);
                    if (catalyst.isEmpty()) {
                        input.setItem(1, ItemStack.EMPTY);
                    }
                }

                crafted = true;
            }

            if (crafted) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREWING_STAND_BREW,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.3F, 1.0F);
            }

            updateRecipes();
            assembleSelectedOutput();
            return original;
        }

        if (index < 2) {
            if (!this.moveItemStackTo(stackInSlot, 3, 39, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stackInSlot, 0, 1, false) && !this.moveItemStackTo(stackInSlot, 1, 2, false)) {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, stackInSlot);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D) <= 64, true);
    }

    public List<AlchemyRecipe> getCurrentRecipes() {
        return recipes;
    }

    public AlchemyRecipe getRecipeForResult(ItemStack resultStack) {
        ItemStack inputItem = input.getItem(0);
        if (inputItem.isEmpty()) {
            return null;
        }

        for (AlchemyRecipe recipe : recipes) {
            for (ItemStack output : recipe.getFilteredResults(inputItem)) {
                if (ItemStack.isSameItemSameComponents(output, resultStack)) {
                    return recipe;
                }
            }
        }
        return null;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            if (level.getBlockEntity(pos) instanceof AlchemyTableBlockEntity be) {
                be.removeUser(player);
            }
            this.clearContainer(player, this.input);
        });
    }
}


