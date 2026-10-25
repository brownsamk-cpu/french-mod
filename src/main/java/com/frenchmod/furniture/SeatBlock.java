package com.frenchmod.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Chair and Couch: right-click to sit down (on an invisible Seat), sneak to stand up. */
public class SeatBlock extends FurnitureBlock {
	/** How high you sit, in blocks above the bottom of the block. */
	public final double seatHeight;

	public SeatBlock(VoxelShape northShape, double seatHeight, Properties properties) {
		super(northShape, properties);
		this.seatHeight = seatHeight;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		return sit(level, pos, player) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
	}

	/** Sits the player down, unless someone is already sitting here or the player is riding something. */
	public boolean sit(Level level, BlockPos pos, Player player) {
		if (player.isPassenger() || player.isShiftKeyDown()) return false;
		if (!level.getEntitiesOfClass(Seat.class, new AABB(pos)).isEmpty()) return false;
		Seat seat = new Seat(ModFurniture.SEAT, level);
		seat.setPos(pos.getX() + 0.5, pos.getY() + this.seatHeight, pos.getZ() + 0.5);
		seat.setYRot(level.getBlockState(pos).getValue(FACING).toYRot());
		if (!level.addFreshEntity(seat)) return false;
		if (!player.startRiding(seat)) {
			seat.discard();
			return false;
		}
		return true;
	}
}
