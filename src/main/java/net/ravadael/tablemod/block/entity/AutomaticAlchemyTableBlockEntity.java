package net.ravadael.tablemod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ravadael.tablemod.block.custom.AutomaticAlchemyTableBlock;
import net.ravadael.tablemod.menu.AutomaticAlchemyTableMenu;
import net.ravadael.tablemod.recipe.AlchemyRecipe;
import net.ravadael.tablemod.recipe.AlchemyRecipeInput;
import net.ravadael.tablemod.recipe.ModRecipes;

import javax.annotation.Nullable;

public class AutomaticAlchemyTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_CATALYST = 1;
    public static final int SLOT_OUTPUT = 2;

    /** Un item par opération de transfert, comme un hopper vanilla. */
    private static final int ITEMS_PER_TRANSFER = 1;
    /** Délai entre deux transferts réussis (8 ticks = hopper vanilla). */
    private static final int TICKS_PER_TRANSFER = HopperBlockEntity.MOVE_ITEM_SPEED;
    /** Temps de craft automatique (20 ticks = 1 seconde). */
    private static final int TICKS_PER_CRAFT = 10;

    private int transferCooldown = 0;
    private int craftCooldown = 0;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return AutomaticAlchemyTableBlockEntity.this.isItemValidForInventorySlot(slot, stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_OUTPUT ? 1 : super.getSlotLimit(slot);
        }
    };

    @Nullable
    private ResourceLocation selectedRecipeId;
    private ItemStack selectedOutput = ItemStack.EMPTY;
    private int playersUsing = 0;
    private int ambientSoundTimer = 0;

    public AutomaticAlchemyTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOMATIC_ALCHEMY_TABLE_BE.get(), pos, state);
    }

    public void addUser(Player player) {
        playersUsing++;
        ambientSoundTimer = 0;
    }

    public void removeUser(Player player) {
        if (playersUsing > 0) {
            playersUsing--;
        }
    }

    public boolean hasUsers() {
        return playersUsing > 0;
    }

    public boolean hasTargetSelection() {
        return selectedRecipeId != null && !selectedOutput.isEmpty();
    }

    @Nullable
    public ResourceLocation getSelectedRecipeId() {
        return selectedRecipeId;
    }

    public ItemStack getSelectedOutput() {
        return selectedOutput.copy();
    }

    public int getCraftCooldown() {
        return craftCooldown;
    }

    public int getTicksPerOperation() {
        return TICKS_PER_CRAFT;
    }

    public void rememberSelection(@Nullable ResourceLocation recipeId, ItemStack output) {
        if (recipeId == null || output.isEmpty()) {
            selectedRecipeId = null;
            selectedOutput = ItemStack.EMPTY;
        } else {
            selectedRecipeId = recipeId;
            selectedOutput = output.copy();
            if (selectedOutput.getCount() < 1) {
                selectedOutput.setCount(1);
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.invalidateCapabilities(worldPosition);
        }
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (level == null) {
            return inventory;
        }
        if (side == null) {
            return inventory;
        }

        Direction facing = getBlockState().getValue(AutomaticAlchemyTableBlock.FACING);
        // Gauche / droite vus depuis la face avant du bloc (FACING)
        Direction inputSide = facing.getClockWise();
        Direction outputSide = facing.getCounterClockWise();

        if (side == Direction.UP) {
            return new SingleSlotHandler(inventory, SLOT_CATALYST, true, true, this::canInsertCatalyst);
        }
        if (side == inputSide) {
            // Insert filtré (funnels Create push) ; extract désactivé pour éviter les boucles
            return new SingleSlotHandler(inventory, SLOT_INPUT, true, false, this::canAcceptInputFromHopper);
        }
        if (side == outputSide) {
            return new SingleSlotHandler(inventory, SLOT_OUTPUT, false, true, stack -> false);
        }
        return null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AutomaticAlchemyTableBlockEntity be) {
        boolean active = be.hasTargetSelection()
                && (!be.inventory.getStackInSlot(SLOT_INPUT).isEmpty() || be.craftCooldown > 0);
        boolean lit = be.hasUsers() || active;

        if (state.getValue(AutomaticAlchemyTableBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(AutomaticAlchemyTableBlock.LIT, lit), 3);
        }

        if (be.hasUsers()) {
            be.ambientSoundTimer++;
            if (be.ambientSoundTimer > 60) {
                be.ambientSoundTimer = 0;
                level.playSound(null, pos, SoundEvents.CANDLE_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.0F);
            }
        }

        if (!level.isClientSide) {
            be.resolveSelectionForCurrentInput(level);

            if (be.transferCooldown > 0) {
                be.transferCooldown--;
            }

            if (be.transferCooldown == 0 && be.tryTransferWithAdjacentBlocks(level)) {
                be.transferCooldown = TICKS_PER_TRANSFER;
            }

            if (be.craftCooldown > 0) {
                if (be.canCraftOne(level)) {
                    be.craftCooldown--;
                    if (be.craftCooldown == 0) {
                        be.tryCraftOne(level);
                    }
                } else {
                    be.craftCooldown = 0;
                }
            } else if (be.canCraftOne(level)) {
                be.craftCooldown = TICKS_PER_CRAFT;
            }
        }
    }

    private Direction getInputSide() {
        return getBlockState().getValue(AutomaticAlchemyTableBlock.FACING).getClockWise();
    }

    private Direction getOutputSide() {
        return getBlockState().getValue(AutomaticAlchemyTableBlock.FACING).getCounterClockWise();
    }

    private boolean tryTransferWithAdjacentBlocks(Level level) {
        return tryPullFromAdjacent(level, getInputSide(), SLOT_INPUT)
                | tryPullFromAdjacent(level, Direction.UP, SLOT_CATALYST)
                | tryPushToAdjacent(level, getOutputSide(), SLOT_OUTPUT);
    }

    /** Recette réellement utilisable pour l'entrée + la cible choisie (pas seulement l'id mémorisé). */
    private void resolveSelectionForCurrentInput(Level level) {
        if (!hasTargetSelection()) {
            return;
        }

        ItemStack inputStack = inventory.getStackInSlot(SLOT_INPUT);
        if (inputStack.isEmpty()) {
            return;
        }

        RecipeHolder<AlchemyRecipe> holder = findCraftHolder(level, inputStack,
                inventory.getStackInSlot(SLOT_CATALYST), selectedOutput);
        if (holder != null && !holder.id().equals(selectedRecipeId)) {
            rememberSelection(holder.id(), selectedOutput);
        }
    }

    @Nullable
    private IItemHandler getAdjacentItemHandler(BlockPos pos, Direction fromTableTowardNeighbor) {
        if (level == null) {
            return null;
        }
        Direction accessSide = fromTableTowardNeighbor.getOpposite();
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, accessSide);
        if (handler != null) {
            return handler;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }

    private boolean tryPullFromAdjacent(Level level, Direction fromSide, int inventorySlot) {
        BlockPos sourcePos = worldPosition.relative(fromSide);
        IItemHandler source = getAdjacentItemHandler(sourcePos, fromSide);
        if (source == null) {
            return false;
        }

        int space = inventory.getStackInSlot(inventorySlot).getMaxStackSize() - inventory.getStackInSlot(inventorySlot).getCount();
        if (space <= 0) {
            return false;
        }

        int amount = Math.min(ITEMS_PER_TRANSFER, space);

        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack probe = source.extractItem(i, amount, true);
            if (probe.isEmpty() || !isItemValidForInventorySlot(inventorySlot, probe)) {
                continue;
            }

            ItemStack extracted = source.extractItem(i, amount, false);
            ItemStack remainder = inventory.insertItem(inventorySlot, extracted, false);
            if (!remainder.isEmpty()) {
                source.insertItem(i, remainder, false);
            }
            return true;
        }
        return false;
    }

    private boolean tryPushToAdjacent(Level level, Direction toSide, int inventorySlot) {
        ItemStack stack = inventory.getStackInSlot(inventorySlot);
        if (stack.isEmpty()) {
            return false;
        }

        BlockPos destPos = worldPosition.relative(toSide);
        IItemHandler dest = getAdjacentItemHandler(destPos, toSide);
        if (dest == null) {
            return false;
        }

        ItemStack toInsert = stack.copy();
        toInsert.setCount(1);
        ItemStack remainder = ItemHandlerHelper.insertItem(dest, toInsert, false);
        if (remainder.isEmpty()) {
            stack.shrink(1);
            inventory.setStackInSlot(inventorySlot, stack.isEmpty() ? ItemStack.EMPTY : stack);
            return true;
        }
        return false;
    }

    private boolean isItemValidForInventorySlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> canAcceptInputFromHopper(stack);
            case SLOT_CATALYST -> canInsertCatalyst(stack);
            default -> false;
        };
    }

    /**
     * Entrée hopper : remplit le slot si l'item appartient à une famille alchimie (sans tenir compte de la cible).
     * Le craft, lui, exige une cible compatible dans {@link #tryCraftOne}.
     */
    private boolean canAcceptInputFromHopper(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }

        ItemStack existing = inventory.getStackInSlot(SLOT_INPUT);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
            return false;
        }

        return matchesAnyAlchemyInput(stack);
    }

    private boolean matchesAnyAlchemyInput(ItemStack stack) {
        for (RecipeHolder<AlchemyRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.ALCHEMY_RECIPE_TYPE.get())) {
            if (holder.value().matchesInputOnly(new AlchemyRecipeInput(stack, ItemStack.EMPTY))) {
                return true;
            }
        }
        return false;
    }

    private boolean tryCraftOne(Level level) {
        if (!canCraftOne(level)) {
            return false;
        }

        ItemStack inputStack = inventory.getStackInSlot(SLOT_INPUT);
        ItemStack catalystStack = inventory.getStackInSlot(SLOT_CATALYST);
        RecipeHolder<AlchemyRecipe> holder = findCraftHolder(level, inputStack, catalystStack, selectedOutput);
        if (holder == null) {
            return false;
        }

        AlchemyRecipe recipe = holder.value();

        ItemStack result = selectedOutput.copy();
        if (result.getCount() < 1) {
            result.setCount(1);
        }

        inputStack.shrink(1);
        inventory.setStackInSlot(SLOT_INPUT, inputStack.isEmpty() ? ItemStack.EMPTY : inputStack);

        if (recipe.isCatalystRequired()) {
            catalystStack.shrink(1);
            inventory.setStackInSlot(SLOT_CATALYST, catalystStack.isEmpty() ? ItemStack.EMPTY : catalystStack);
        }

        inventory.setStackInSlot(SLOT_OUTPUT, result);

        if (!holder.id().equals(selectedRecipeId)) {
            rememberSelection(holder.id(), selectedOutput);
        }

        setChanged();
        level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.3F, 1.0F);
        return true;
    }

    private boolean canCraftOne(Level level) {
        if (!hasTargetSelection()) {
            return false;
        }

        ItemStack inputStack = inventory.getStackInSlot(SLOT_INPUT);
        ItemStack catalystStack = inventory.getStackInSlot(SLOT_CATALYST);
        RecipeHolder<AlchemyRecipe> holder = findCraftHolder(level, inputStack, catalystStack, selectedOutput);
        if (holder == null) {
            return false;
        }

        AlchemyRecipe recipe = holder.value();
        AlchemyRecipeInput recipeInput = new AlchemyRecipeInput(inputStack, catalystStack);

        if (!recipe.matches(recipeInput, level)) {
            return false;
        }

        if (!inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            return false;
        }

        if (!holder.id().equals(selectedRecipeId)) {
            rememberSelection(holder.id(), selectedOutput);
        }

        return true;
    }

    private boolean canInsertCatalyst(ItemStack stack) {
        if (!hasTargetSelection() || level == null) {
            return false;
        }

        ItemStack inputStack = inventory.getStackInSlot(SLOT_INPUT);
        RecipeHolder<AlchemyRecipe> holder = findCraftHolder(level, inputStack,
                inventory.getStackInSlot(SLOT_CATALYST), selectedOutput);
        if (holder == null) {
            return false;
        }

        AlchemyRecipe recipe = holder.value();
        return recipe.isCatalystRequired() && recipe.getCatalyst().test(stack);
    }

    @Nullable
    private RecipeHolder<AlchemyRecipe> findCraftHolder(Level level, ItemStack input, ItemStack catalyst,
                                                        ItemStack target) {
        if (input.isEmpty() || target.isEmpty()) {
            return null;
        }

        AlchemyRecipeInput recipeInput = new AlchemyRecipeInput(input, catalyst);
        for (RecipeHolder<AlchemyRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.ALCHEMY_RECIPE_TYPE.get())) {
            AlchemyRecipe recipe = holder.value();
            if (!recipe.matchesInputOnly(recipeInput)) {
                continue;
            }
            if (matchesSelectedOutput(recipe, input, target, level.registryAccess())) {
                return holder;
            }
        }
        return null;
    }

    private static boolean matchesSelectedOutput(AlchemyRecipe recipe, ItemStack input, ItemStack target,
                                                 HolderLookup.Provider registries) {
        for (ItemStack output : recipe.getFilteredResults(input, registries)) {
            if (ItemStack.isSameItemSameComponents(output, target) || ItemStack.isSameItem(output, target)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Items"));
        loadSelection(tag, registries);
        playersUsing = tag.getInt("Users");
        transferCooldown = tag.getInt("TransferCooldown");
        craftCooldown = tag.getInt("CraftCooldown");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", inventory.serializeNBT(registries));
        saveSelection(tag, registries);
        tag.putInt("Users", playersUsing);
        tag.putInt("TransferCooldown", transferCooldown);
        tag.putInt("CraftCooldown", craftCooldown);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveSelection(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        loadSelection(tag, registries);
    }

    private void loadSelection(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("SelectedRecipeId")) {
            selectedRecipeId = ResourceLocation.tryParse(tag.getString("SelectedRecipeId"));
            selectedOutput = ItemStack.parseOptional(registries, tag.getCompound("SelectedOutput"));
            if (selectedOutput.isEmpty()) {
                selectedRecipeId = null;
            }
        } else {
            selectedRecipeId = null;
            selectedOutput = ItemStack.EMPTY;
        }
    }

    private void saveSelection(CompoundTag tag, HolderLookup.Provider registries) {
        if (selectedRecipeId != null && !selectedOutput.isEmpty()) {
            tag.putString("SelectedRecipeId", selectedRecipeId.toString());
            tag.put("SelectedOutput", selectedOutput.saveOptional(registries));
        } else {
            tag.remove("SelectedRecipeId");
            tag.remove("SelectedOutput");
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AutomaticAlchemyTableMenu(id, inv, level, worldPosition);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.tablemod.automatic_alchemy");
    }

    private static final class SingleSlotHandler implements IItemHandler {
        private final ItemStackHandler backing;
        private final int slot;
        private final boolean allowInsert;
        private final boolean allowExtract;
        private final InsertValidator insertValidator;

        private SingleSlotHandler(ItemStackHandler backing, int slot, boolean allowInsert, boolean allowExtract,
                                  InsertValidator insertValidator) {
            this.backing = backing;
            this.slot = slot;
            this.allowInsert = allowInsert;
            this.allowExtract = allowExtract;
            this.insertValidator = insertValidator;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return index == 0 ? backing.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int index) {
            return backing.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int index, ItemStack stack) {
            return index == 0 && insertValidator.test(stack);
        }

        @Override
        public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
            if (!allowInsert || index != 0 || stack.isEmpty() || !insertValidator.test(stack)) {
                return stack;
            }
            return backing.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int index, int amount, boolean simulate) {
            if (!allowExtract || index != 0) {
                return ItemStack.EMPTY;
            }
            return backing.extractItem(slot, amount, simulate);
        }
    }

    @FunctionalInterface
    private interface InsertValidator {
        boolean test(ItemStack stack);
    }
}
