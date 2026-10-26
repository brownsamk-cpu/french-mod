package com.frenchmod.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Bathtub (0.4.0): two blocks long, white with gold feet and a gold tap.
 * part=head is the end you placed (sit there), part=foot is the tap end behind it.
 * Right-click the tap end with an empty hand, or anywhere with a water bucket, to fill it; tap again to let the water out.
 */
public class BathtubBlock extends SeatBlock {
	public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
	public static final BooleanProperty FILLED = BooleanProperty.create("filled");

	public BathtubBlock(Properties properties) {
		super(Block.box(0, 0, 0, 16, 10, 16), 0.3125, properties);
		this.registerDefaultState(this.defaultBlockState().setValue(PART, BedPart.HEAD).setValue(FILLED, false));
	}

	/** Where the other half is: the foot (tap end) sits behind the head, away from the player. */
	public static BlockPos otherHalf(BlockState state, BlockPos pos) {
		Direction back = state.getValue(FACING).getOpposite();
		return state.getValue(PART) == BedPart.HEAD ? pos.relative(back) : pos.relative(back.getOpposite());
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		BlockPos foot = context.getClickedPos().relative(state.getValue(FACING).getOpposite());
		Level level = context.getLevel();
		return level.getBlockState(foot).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(foot) ? state : null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide()) {
			level.setBlock(otherHalf(state, pos), state.setValue(PART, BedPart.FOOT), Block.UPDATE_ALL);
		}
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos neighborPos,
			BlockState neighbor, RandomSource random) {
		if (neighborPos.equals(otherHalf(state, pos))) {
			if (!neighbor.is(this) || neighbor.getValue(PART) == state.getValue(PART)) return Blocks.AIR.defaultBlockState();
			return state.setValue(FILLED, neighbor.getValue(FILLED)); // both halves fill together
		}
		return super.updateShape(state, level, ticks, pos, dir, neighborPos, neighbor, random);
	}

	/** Fills or empties both halves, with a splash. */
	public void setFilled(Level level, BlockPos pos, boolean filled) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(this) || state.getValue(FILLED) == filled) return;
		level.setBlock(pos, state.setValue(FILLED, filled), Block.UPDATE_ALL);
		BlockPos other = otherHalf(state, pos);
		if (level.getBlockState(other).is(this)) level.setBlock(other, level.getBlockState(other).setValue(FILLED, filled), Block.UPDATE_ALL);
		level.playSound(null, pos, filled ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);
		if (level instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 20, 0.4, 0.1, 0.4, 0.1);
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (stack.is(Items.WATER_BUCKET) && !state.getValue(FILLED)) {
			if (!level.isClientSide()) {
				this.setFilled(level, pos, true);
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
			}
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (state.getValue(PART) == BedPart.FOOT) { // the tap end: turn the water on or off
			if (!level.isClientSide()) this.setFilled(level, pos, !state.getValue(FILLED));
			return InteractionResult.SUCCESS;
		}
		return super.useWithoutItem(state, level, pos, player, hitResult); // the other end: sit in the bath
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(FILLED) && random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.6, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(PART, FILLED);
	}
}
