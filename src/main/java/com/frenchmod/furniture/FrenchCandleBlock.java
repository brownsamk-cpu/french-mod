package com.frenchmod.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** The French Candle (0.4.0): a cream candle in a gold holder. Right-click to light it or blow it out; it glows when lit. */
public class FrenchCandleBlock extends FurnitureBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public FrenchCandleBlock(Properties properties) {
		super(Block.box(4, 0, 4, 12, 12, 12), properties);
		this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			boolean lit = !state.getValue(LIT);
			level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
			level.playSound(null, pos, lit ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIT)) {
			double x = pos.getX() + 0.5, y = pos.getY() + 0.85, z = pos.getZ() + 0.5;
			level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0, 0);
			if (random.nextInt(5) == 0) level.addParticle(ParticleTypes.SMOKE, x, y + 0.05, z, 0, 0, 0);
			if (random.nextInt(100) == 0) level.playLocalSound(pos, SoundEvents.CANDLE_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LIT);
	}
}
