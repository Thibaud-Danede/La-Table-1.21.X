package net.ravadael.tablemod.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.ravadael.tablemod.block.entity.AutomaticAlchemyTableBlockEntity;
import net.ravadael.tablemod.recipe.AlchemyRecipe;
import net.ravadael.tablemod.recipe.ModRecipes;

import net.ravadael.tablemod.recipe.AlchemyRecipeInput;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutomaticAlchemyTableMenu extends AbstractContainerMenu {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_SELECTION_PREVIEW = 1;
    private static final int SLOT_CATALYST = 2;
    private static final int SLOT_OUTPUT = 3;
    private static final int SLOT_PLAYER_START = 4;
    private static final int SLOT_PLAYER_END = 40;

    private final SimpleContainer selectionPreview = new SimpleContainer(1);
    private final ContainerLevelAccess access;
    private final Level level;
    @Nullable
    private final AutomaticAlchemyTableBlockEntity blockEntity;

    @Nullable
    private ResourceLocation selectedRecipeId;
    private ItemStack selectedOutput = ItemStack.EMPTY;

    public AutomaticAlchemyTableMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level(), buf != null ? buf.readBlockPos() : BlockPos.ZERO);
    }

    public AutomaticAlchemyTableMenu(int id, Inventory inv, Level level, BlockPos pos) {
        super(ModMenuTypes.AUTOMATIC_ALCHEMY_TABLE_MENU.get(), id);
        this.level = level;
        this.access = ContainerLevelAccess.create(level, pos);
        this.blockEntity = level.getBlockEntity(pos) instanceof AutomaticAlchemyTableBlockEntity be ? be : null;

        if (blockEntity != null) {
            this.addSlot(new SlotItemHandler(blockEntity.getInventory(), AutomaticAlchemyTableBlockEntity.SLOT_INPUT, 20, 54) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        } else {
            SimpleContainer emptyInput = new SimpleContainer(1);
            this.addSlot(new Slot(emptyInput, 0, 20, 54) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }

        this.addSlot(new Slot(selectionPreview, 0, 143, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        if (blockEntity != null) {
            this.addSlot(new SlotItemHandler(blockEntity.getInventory(), AutomaticAlchemyTableBlockEntity.SLOT_CATALYST, 20, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
            this.addSlot(new SlotItemHandler(blockEntity.getInventory(), AutomaticAlchemyTableBlockEntity.SLOT_OUTPUT, 143, 63) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        } else {
            SimpleContainer emptyCatalyst = new SimpleContainer(1);
            this.addSlot(new Slot(emptyCatalyst, 0, 20, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) { return false; }
                @Override
                public boolean mayPickup(Player player) { return false; }
            });
            SimpleContainer emptyOutput = new SimpleContainer(1);
            this.addSlot(new Slot(emptyOutput, 0, 143, 63) {
                @Override
                public boolean mayPlace(ItemStack stack) { return false; }
                @Override
                public boolean mayPickup(Player player) { return false; }
            });
        }

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 96 + row * 18));
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot) {
            this.addSlot(new Slot(inv, hotbarSlot, 8 + hotbarSlot * 18, 154));
        }

        loadSelectionFromBlockEntity();
    }

    private void loadSelectionFromBlockEntity() {
        if (blockEntity == null || !blockEntity.hasTargetSelection()) {
            return;
        }

        selectedRecipeId = blockEntity.getSelectedRecipeId();
        selectedOutput = blockEntity.getSelectedOutput();
        selectionPreview.setItem(0, selectedOutput.copy());
    }

    public void setSelectedOutput(ResourceLocation recipeId, ItemStack output) {
        this.selectedRecipeId = recipeId;
        this.selectedOutput = output.copy();
        if (!this.selectedOutput.isEmpty() && this.selectedOutput.getCount() < 1) {
            this.selectedOutput.setCount(1);
        }
        selectionPreview.setItem(0, selectedOutput.copy());

        if (blockEntity != null && !level.isClientSide) {
            blockEntity.rememberSelection(recipeId, selectedOutput);
        }

        broadcastChanges();
    }

    public List<RecipeHolder<AlchemyRecipe>> getAllRecipeHolders() {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.ALCHEMY_RECIPE_TYPE.get());
    }

    public ItemStack getInputItem() {
        return slots.get(SLOT_INPUT).getItem();
    }

    /** Recettes compatibles avec l'item en entrée (comme la table manuelle). */
    public List<RecipeHolder<AlchemyRecipe>> getRecipeHoldersForInput(ItemStack input) {
        if (input.isEmpty()) {
            return getAllRecipeHolders();
        }

        AlchemyRecipeInput recipeInput = new AlchemyRecipeInput(input, ItemStack.EMPTY);
        List<RecipeHolder<AlchemyRecipe>> valid = new ArrayList<>();
        for (RecipeHolder<AlchemyRecipe> holder : getAllRecipeHolders()) {
            if (holder.value().matchesInputOnly(recipeInput)) {
                valid.add(holder);
            }
        }

        valid.sort(Comparator.comparing(holder -> holder.value()
                .getResultItem(level.registryAccess())
                .getDisplayName()
                .getString()));
        return valid;
    }

    @Nullable
    public ResourceLocation getSelectedRecipeId() {
        return selectedRecipeId;
    }

    public ItemStack getSelectedOutput() {
        return selectionPreview.getItem(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == SLOT_SELECTION_PREVIEW) {
            return ItemStack.EMPTY;
        }

        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack original = stackInSlot.copy();

        if (index == SLOT_INPUT || index == SLOT_CATALYST || index == SLOT_OUTPUT) {
            if (!this.moveItemStackTo(stackInSlot, SLOT_PLAYER_START, SLOT_PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= SLOT_PLAYER_START && index < SLOT_PLAYER_END - 9) {
            if (!this.moveItemStackTo(stackInSlot, SLOT_PLAYER_END - 9, SLOT_PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= SLOT_PLAYER_END - 9) {
            if (!this.moveItemStackTo(stackInSlot, SLOT_PLAYER_START, SLOT_PLAYER_END - 9, false)) {
                return ItemStack.EMPTY;
            }
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            if (level.getBlockEntity(pos) instanceof AutomaticAlchemyTableBlockEntity be) {
                be.removeUser(player);
            }
        });
    }
}
