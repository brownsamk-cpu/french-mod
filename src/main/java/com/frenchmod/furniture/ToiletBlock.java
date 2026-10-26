package com.frenchmod.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;

/** The Toilet (0.4.0): white with a gold handle. Right-click the seat to sit; right-click the tank at the back to flush (whoosh + splash). */
public class ToiletBlock extends SeatBlock {
	public ToiletBlock(Properties properties) {
		super(Shapes.or(Block.box(3, 0, 2, 13, 9, 13), Block.box(2, 0, 12, 14, 16, 16)), 0.5625, properties);
	}

	/** True when the click landed on the tank (the back quarter of the block). */
	public static boolean hitsTank(BlockState state, BlockPos pos, BlockHitResult hit) {
		Direction back = state.getValue(FACING).getOpposite();
		double along = (hit.getLocation().x - pos.getX() - 0.5) * back.getStepX() + (hit.getLocation().z - pos.getZ() - 0.5) * back.getStepZ();
		return along > 0.2 || hit.getLocation().y - pos.getY() > 0.6;
	}

	public void flush(Level level, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.PLAYER_SPLASH, SoundSource.BLOCKS, 0.7F, 1.3F);
		level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8F, 0.8F);
		if (level instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 25, 0.2, 0.05, 0.2, 0.1);
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (hitsTank(state, pos, hitResult)) {
			if (!level.isClientSide()) this.flush(level, pos);
			return InteractionResult.SUCCESS;
		}
		return super.useWithoutItem(state, level, pos, player, hitResult);
	}
}
