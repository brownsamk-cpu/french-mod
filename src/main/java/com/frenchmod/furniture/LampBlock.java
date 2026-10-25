package com.frenchmod.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** A little lamp with a warm glow. It starts switched on; right-click to switch it off and on. */
public class LampBlock extends FurnitureBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public LampBlock(Properties properties) {
		super(Block.box(4, 0, 4, 12, 16, 12), properties);
		this.registerDefaultState(this.defaultBlockState().setValue(LIT, true));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			BlockState next = state.cycle(LIT);
			level.setBlock(pos, next, Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.CHERRY_WOOD_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4F, next.getValue(LIT) ? 1.2F : 0.9F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LIT);
	}
}
