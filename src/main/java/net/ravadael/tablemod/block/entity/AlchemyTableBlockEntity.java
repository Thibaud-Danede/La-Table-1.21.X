package net.ravadael.tablemod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ravadael.tablemod.block.custom.AlchemyTableBlock;
import net.ravadael.tablemod.menu.AlchemyTableMenu;

import java.util.HashMap;
import java.util.Map;

public class AlchemyTableBlockEntity extends BlockEntity implements MenuProvider {
    private final SimpleContainer inventory = new SimpleContainer(3);
    private final Map<ResourceLocation, ItemStack> rememberedSelections = new HashMap<>();
    private int playersUsing = 0;
    private int ambientSoundTimer = 0;

    public AlchemyTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMY_TABLE_BE.get(), pos, state);
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemyTableBlockEntity be) {
        if (!be.hasUsers()) {
            if (state.getValue(AlchemyTableBlock.LIT)) {
                level.setBlock(pos, state.setValue(AlchemyTableBlock.LIT, false), 3);
            }
            return;
        }

        if (!state.getValue(AlchemyTableBlock.LIT)) {
            level.setBlock(pos, state.setValue(AlchemyTableBlock.LIT, true), 3);
        }

        be.ambientSoundTimer++;
        if (be.ambientSoundTimer > 60) {
            be.ambientSoundTimer = 0;
            level.playSound(null, pos, SoundEvents.CANDLE_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.0F);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.fromTag(tag.getList("Items", 10), registries);
        rememberedSelections.clear();
        ListTag rememberedList = tag.getList("RememberedSelections", 10);
        for (int i = 0; i < rememberedList.size(); i++) {
            CompoundTag entry = rememberedList.getCompound(i);
            ResourceLocation recipeId = ResourceLocation.tryParse(entry.getString("RecipeId"));
            if (recipeId == null) {
                continue;
            }

            ItemStack output = ItemStack.parseOptional(registries, entry.getCompound("Output"));
            if (!output.isEmpty()) {
                rememberedSelections.put(recipeId, output);
            }
        }
        playersUsing = tag.getInt("Users");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", inventory.createTag(registries));
        ListTag rememberedList = new ListTag();
        for (Map.Entry<ResourceLocation, ItemStack> entry : rememberedSelections.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            CompoundTag memoryTag = new CompoundTag();
            memoryTag.putString("RecipeId", entry.getKey().toString());
            memoryTag.put("Output", entry.getValue().saveOptional(registries));
            rememberedList.add(memoryTag);
        }
        tag.put("RememberedSelections", rememberedList);
        tag.putInt("Users", playersUsing);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public void rememberSelection(ResourceLocation recipeId, ItemStack output) {
        if (output.isEmpty()) {
            rememberedSelections.remove(recipeId);
        } else {
            rememberedSelections.put(recipeId, output.copy());
        }
        setChanged();
    }

    public ItemStack getRememberedSelection(ResourceLocation recipeId) {
        ItemStack remembered = rememberedSelections.get(recipeId);
        return remembered != null ? remembered.copy() : ItemStack.EMPTY;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AlchemyTableMenu(id, inv, level, worldPosition);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.tablemod.alchemy");
    }
}
