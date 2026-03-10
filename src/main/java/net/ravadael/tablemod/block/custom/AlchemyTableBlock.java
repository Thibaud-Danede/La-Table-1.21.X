package net.ravadael.tablemod.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ravadael.tablemod.block.entity.AlchemyTableBlockEntity;
import net.ravadael.tablemod.block.entity.ModBlockEntities;
import net.ravadael.tablemod.menu.AlchemyTableMenu;

public class AlchemyTableBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<AlchemyTableBlock> CODEC = simpleCodec(AlchemyTableBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    private static final Component CONTAINER_TITLE = Component.translatable("container.tablemod.alchemy");
    private static final VoxelShape SHAPE_NORTH = Shapes.box(0.0, 0.0, 1.0 / 16.0, 1.0, 15.0 / 16.0, 15.0 / 16.0);
    private static final VoxelShape SHAPE_EAST = rotate(SHAPE_NORTH);
    private static final VoxelShape SHAPE_SOUTH = rotate(SHAPE_EAST);
    private static final VoxelShape SHAPE_WEST = rotate(SHAPE_SOUTH);

    public AlchemyTableBlock(BlockBehaviour.Properties props) {
        super(props.lightLevel(state -> state.getValue(LIT) ? 7 : 0));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlchemyTableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.ALCHEMY_TABLE_BE.get()
                ? (lvl, p, st, be) -> AlchemyTableBlockEntity.serverTick(lvl, p, st, (AlchemyTableBlockEntity) be)
                : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    private static VoxelShape rotate(VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double nMinX = 1 - maxZ;
            double nMinZ = minX;
            double nMaxX = 1 - minZ;
            double nMaxZ = maxX;
            buffer[1] = Shapes.or(buffer[1], Shapes.box(nMinX, minY, nMinZ, nMaxX, maxY, nMaxZ));
        });
        return buffer[1];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AlchemyTableBlockEntity table) {
                table.addUser(player);
            }

            if (!state.getValue(LIT)) {
                level.setBlock(pos, state.setValue(LIT, true), 3);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.4F);
            }

            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            player.openMenu(state.getMenuProvider(level, pos));
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }

        if (random.nextInt(8) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, SoundEvents.CANDLE_AMBIENT,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.2F, 1.0F, false);
        }

        double baseX = pos.getX() + 0.5;
        double baseY = pos.getY();
        double baseZ = pos.getZ() + 0.5;
        double[][] offsets = {
                {0.29, 0.90, -0.05},
                {0.30, 0.95, 0.00},
                {0.29, 0.90, 0.07}
        };

        Direction dir = state.getValue(FACING);
        for (double[] offset : offsets) {
            double ox = -offset[2];
            double oz = offset[0];
            double[] rotated = rotateOffset(ox, oz, dir);
            double x = baseX + rotated[0];
            double y = baseY + offset[1];
            double z = baseZ + rotated[1];

            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, x, y, z, 0, 0, 0);
            if (random.nextInt(6) == 0) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, x, y + 0.03, z, 0, 0, 0);
            }
        }
    }

    private double[] rotateOffset(double x, double z, Direction dir) {
        return switch (dir) {
            case NORTH -> new double[]{x, z};
            case EAST -> new double[]{-z, x};
            case SOUTH -> new double[]{-x, -z};
            case WEST -> new double[]{z, -x};
            default -> new double[]{x, z};
        };
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, player) -> new AlchemyTableMenu(id, inv, level, pos), CONTAINER_TITLE);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
}
