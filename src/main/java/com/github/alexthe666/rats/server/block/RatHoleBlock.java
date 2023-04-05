package com.github.alexthe666.rats.server.block;

import com.github.alexthe666.rats.server.block.entity.RatHoleBlockEntity;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class RatHoleBlock extends BaseEntityBlock {

	public static final BooleanProperty NORTH = BooleanProperty.create("north");
	public static final BooleanProperty EAST = BooleanProperty.create("east");
	public static final BooleanProperty SOUTH = BooleanProperty.create("south");
	public static final BooleanProperty WEST = BooleanProperty.create("west");

	public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(Util.make(Maps.newEnumMap(Direction.class), map -> {
		map.put(Direction.NORTH, NORTH);
		map.put(Direction.EAST, EAST);
		map.put(Direction.SOUTH, SOUTH);
		map.put(Direction.WEST, WEST);
	}));

	private static final VoxelShape TOP_AABB = Block.box(0, 8, 0, 16, 16, 16);
	private static final VoxelShape NS_LEFT_AABB = Block.box(0, 0, 0, 4, 8, 16);
	private static final VoxelShape NS_RIGHT_AABB = Block.box(12, 0, 0, 16, 8, 16);
	private static final VoxelShape EW_LEFT_AABB = Block.box(0, 0, 0, 16, 8, 4);
	private static final VoxelShape EW_RIGHT_AABB = Block.box(0, 0, 12, 16, 8, 16);
	private static final VoxelShape NORTH_CORNER_AABB = Block.box(0, 0, 0, 4, 8, 4);
	private static final VoxelShape EAST_CORNER_AABB = Block.box(12, 0, 0, 16, 8, 4);
	private static final VoxelShape SOUTH_CORNER_AABB = Block.box(0, 0, 12, 4, 8, 16);
	private static final VoxelShape WEST_CORNER_AABB = Block.box(12, 0, 12, 16, 8, 16);
	private VoxelShape shape;

	public RatHoleBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.getStateDefinition().any()
				.setValue(NORTH, false)
				.setValue(EAST, false)
				.setValue(SOUTH, false)
				.setValue(WEST, false)
		);
		this.shape = Shapes.join(TOP_AABB, NORTH_CORNER_AABB, BooleanOp.OR).optimize();
		this.shape = Shapes.join(this.shape, SOUTH_CORNER_AABB, BooleanOp.OR).optimize();
		this.shape = Shapes.join(this.shape, EAST_CORNER_AABB, BooleanOp.OR).optimize();
		this.shape = Shapes.join(this.shape, WEST_CORNER_AABB, BooleanOp.OR).optimize();
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (level.getBlockEntity(pos) != null && level.getBlockEntity(pos) instanceof RatHoleBlockEntity hole) {
			NonNullList<ItemStack> ret = NonNullList.create();
			if (!level.isClientSide() && level instanceof ServerLevel) {
				getDrops(hole.getImitatedBlockState(), (ServerLevel) level, pos, null);
				for (ItemStack stack : ret) {
					ItemEntity ItemEntity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
					level.addFreshEntity(ItemEntity);
				}
			}
		}
		super.onRemove(state, level, pos, newState, moving);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos blockpos = context.getClickedPos();
		BlockPos blockpos1 = blockpos.north();
		BlockPos blockpos2 = blockpos.east();
		BlockPos blockpos3 = blockpos.south();
		BlockPos blockpos4 = blockpos.west();
		BlockState blockstate = level.getBlockState(blockpos1);
		BlockState blockstate1 = level.getBlockState(blockpos2);
		BlockState blockstate2 = level.getBlockState(blockpos3);
		BlockState blockstate3 = level.getBlockState(blockpos4);
		return Objects.requireNonNull(super.getStateForPlacement(context))
				.setValue(NORTH, this.canFenceConnectTo(blockstate, blockstate.isFaceSturdy(level, blockpos, Direction.SOUTH)))
				.setValue(EAST, this.canFenceConnectTo(blockstate1, blockstate.isFaceSturdy(level, blockpos, Direction.WEST)))
				.setValue(SOUTH, this.canFenceConnectTo(blockstate2, blockstate.isFaceSturdy(level, blockpos, Direction.NORTH)))
				.setValue(WEST, this.canFenceConnectTo(blockstate3, blockstate.isFaceSturdy(level, blockpos, Direction.EAST)));
	}

	public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
		VoxelShape shape1 = this.shape;
		if (state.getBlock() instanceof RatHoleBlock) {
			if (state.getValue(NORTH)) {
				shape1 = Shapes.join(shape1, EW_LEFT_AABB, BooleanOp.OR);
			}
			if (state.getValue(SOUTH)) {
				shape1 = Shapes.join(shape1, EW_RIGHT_AABB, BooleanOp.OR);
			}
			if (state.getValue(WEST)) {
				shape1 = Shapes.join(shape1, NS_LEFT_AABB, BooleanOp.OR);
			}
			if (state.getValue(EAST)) {
				shape1 = Shapes.join(shape1, NS_RIGHT_AABB, BooleanOp.OR);
			}
		}
		return shape1;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, WEST, SOUTH);
	}


	private boolean canFenceConnectTo(BlockState state, boolean connectToFace) {
		Block block = state.getBlock();
		return !connectToFace && block != this;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RatHoleBlockEntity(pos, state);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
		BooleanProperty connect = switch (facing) {
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case EAST -> EAST;
			case WEST -> WEST;
			default -> null;
		};
		if (connect == null) {
			return state;
		}
		return state.setValue(connect, this.canFenceConnectTo(facingState, facingState.isFaceSturdy(level, facingPos, facing.getOpposite())));
	}
}