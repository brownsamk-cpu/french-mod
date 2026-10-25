package com.frenchmod.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A real bed (sleep through the night, sets your spawn), just fancier: cream wood, white sheets, a blue blanket and gold trim. */
public class FancyBedBlock extends BedBlock {
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 9, 16);

	public FancyBedBlock(Properties properties) {
		super(DyeColor.WHITE, properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
