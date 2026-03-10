package net.ravadael.tablemod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ravadael.tablemod.block.custom.AlchemyTableBlock;
import net.ravadael.tablemod.menu.AlchemyTableMenu;

public class AlchemyTableBlockEntity extends BlockEntity implements MenuProvider {
    private final SimpleContainer inventory = new SimpleContainer(3);
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
        playersUsing = tag.getInt("Users");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", inventory.createTag(registries));
        tag.putInt("Users", playersUsing);
    }

    public SimpleContainer getInventory() {
        return inventory;
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
