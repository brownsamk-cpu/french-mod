package com.frenchmod.tux;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Tux steals food (0.3.3): from a vanilla chest or barrel within 8 blocks (walks up, the lid opens, ONE food item vanishes with
 * a heart, then a 10 second cooldown), or from an item frame holding food (the food vanishes, the frame stays, empty).
 */
class StealFoodGoal extends Goal {
	static final int RANGE = 8;
	static final double REACH = 2.0;
	private final Tux tux;
	private @Nullable BlockPos chest;
	private @Nullable ItemFrame frame;
	private int searchDelay;
	private int giveUp;

	StealFoodGoal(Tux tux) {
		this.tux = tux;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	static boolean isFood(ItemStack stack) {
		return !stack.isEmpty() && stack.has(DataComponents.FOOD);
	}

	static boolean isStealable(BlockEntity be) {
		return be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity;
	}

	static int foodSlot(Container c) {
		for (int i = 0; i < c.getContainerSize(); i++) {
			if (isFood(c.getItem(i))) return i;
		}
		return -1;
	}

	@Override
	public boolean canUse() {
		if (this.tux.stealCooldown > 0 || --this.searchDelay > 0) return false;
		this.searchDelay = 10;
		this.chest = null;
		this.frame = null;
		double best = Double.MAX_VALUE;
		for (ItemFrame f : this.tux.level().getEntitiesOfClass(ItemFrame.class, this.tux.getBoundingBox().inflate(RANGE), f -> isFood(f.getItem()))) {
			double d = f.distanceToSqr(this.tux);
			if (d < best) {
				best = d;
				this.frame = f;
			}
		}
		BlockPos at = this.tux.blockPosition();
		for (BlockPos p : BlockPos.betweenClosed(at.offset(-RANGE, -3, -RANGE), at.offset(RANGE, 3, RANGE))) {
			BlockEntity be = this.tux.level().getBlockEntity(p);
			if (be != null && isStealable(be) && foodSlot((Container) be) >= 0) {
				double d = this.tux.distanceToSqr(Vec3.atCenterOf(p));
				if (d < best) {
					best = d;
					this.chest = p.immutable();
					this.frame = null;
				}
			}
		}
		return this.chest != null || this.frame != null;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.giveUp <= 0) return false;
		if (this.frame != null) return this.frame.isAlive() && isFood(this.frame.getItem());
		return this.chest != null && this.tux.level().getBlockEntity(this.chest) instanceof Container c && foodSlot(c) >= 0;
	}

	private BlockPos targetPos() {
		return this.frame != null ? this.frame.blockPosition() : this.chest;
	}

	private Vec3 targetCenter() {
		return this.frame != null ? this.frame.position() : Vec3.atCenterOf(this.chest);
	}

	@Override
	public void start() {
		this.giveUp = 400;
		walk();
	}

	private void walk() {
		this.tux.getNavigation().moveTo(this.tux.getNavigation().createPath(targetPos(), 1), 1.1);
	}

	@Override
	public void stop() {
		this.chest = null;
		this.frame = null;
		this.tux.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		this.giveUp--;
		if (this.chest == null && this.frame == null) return; // already stole this time; the goal stops next check
		Vec3 c = targetCenter();
		this.tux.getLookControl().setLookAt(c.x, c.y, c.z);
		double dx = c.x - this.tux.getX(), dz = c.z - this.tux.getZ(), dy = c.y - this.tux.getY();
		if (dx * dx + dz * dz <= REACH * REACH && Math.abs(dy) <= 2.0) {
			steal();
		} else if (this.tux.getNavigation().isDone() || this.tux.tickCount % 20 == 0) {
			walk();
		}
	}

	private void steal() {
		if (!(this.tux.level() instanceof ServerLevel level) || (this.chest == null && this.frame == null)) return;
		Vec3 c = targetCenter();
		if (this.frame != null) {
			if (!isFood(this.frame.getItem())) return;
			this.frame.setItem(ItemStack.EMPTY);
			this.frame = null;
		} else if (level.getBlockEntity(this.chest) instanceof Container box) {
			int slot = foodSlot(box);
			if (slot < 0) return;
			box.removeItem(slot, 1); // one item, not the stack
			box.setChanged();
			this.tux.openLid(level, this.chest);
			this.chest = null;
		}
		this.tux.stealCooldown = Tux.STEAL_COOLDOWN;
		level.sendParticles(ParticleTypes.HEART, c.x, c.y + 0.6, c.z, 2, 0.2, 0.1, 0.2, 0.0);
	}

	/** Opens (true) or closes (false) a chest or barrel lid, with the sound. */
	static void setLid(ServerLevel level, BlockPos pos, boolean open) {
		BlockState state = level.getBlockState(pos);
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof ChestBlockEntity) {
			level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
			level.playSound(null, pos, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1.0F);
		} else if (be instanceof BarrelBlockEntity && state.hasProperty(BarrelBlock.OPEN)) {
			level.setBlock(pos, state.setValue(BarrelBlock.OPEN, open), 3);
			level.playSound(null, pos, open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.5F, 1.0F);
		}
	}
}
