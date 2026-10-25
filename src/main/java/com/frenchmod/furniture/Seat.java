package com.frenchmod.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** The invisible thing you ride when you sit on a Chair or Couch. It disappears when you stand up or the furniture is broken. */
public class Seat extends Entity {
	public Seat(EntityType<? extends Seat> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel && (!this.isVehicle() || !(this.level().getBlockState(this.seatPos()).getBlock() instanceof SeatBlock))) {
			this.ejectPassengers();
			this.discard();
		}
	}

	public BlockPos seatPos() {
		return BlockPos.containing(this.getX(), this.getY() - 0.05, this.getZ());
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		BlockPos pos = this.seatPos();
		if (this.level().getBlockState(pos).getBlock() instanceof SeatBlock) {
			// Stand up in front of the chair if there's room, otherwise on top of it.
			BlockPos front = pos.relative(this.level().getBlockState(pos).getValue(SeatBlock.FACING));
			if (this.level().getBlockState(front).getCollisionShape(this.level(), front).isEmpty()
					&& this.level().getBlockState(front.above()).getCollisionShape(this.level(), front.above()).isEmpty()) {
				return Vec3.atBottomCenterOf(front);
			}
			return Vec3.atBottomCenterOf(pos.above());
		}
		return super.getDismountLocationForPassenger(passenger);
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengers().isEmpty();
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}
}
