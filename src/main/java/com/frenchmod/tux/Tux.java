package com.frenchmod.tux;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.feline.CatSoundVariant;
import net.minecraft.world.entity.animal.feline.CatSoundVariants;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Tux, the family's real cat: a fluffy long-haired black cat with a big white chest. A normal peaceful mob that wanders around.
 * When food is dropped within 8 blocks, Tux walks over, and when Tux gets to it (about 1 block) the food just disappears
 * (one stack) with a little heart.
 */
public class Tux extends Animal {
	public static final double FOOD_SEARCH_RANGE = 8.0;
	public static final double EAT_RANGE = 1.0;

	public Tux(EntityType<? extends Tux> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.3F);
	}

	public static boolean isDroppedFood(ItemEntity item) {
		return item.isAlive() && item.getItem().has(DataComponents.FOOD);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
		this.goalSelector.addGoal(2, new GoToDroppedFoodGoal(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level() instanceof ServerLevel level && this.isAlive()) {
			List<ItemEntity> close = level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(EAT_RANGE), Tux::isDroppedFood);
			if (!close.isEmpty()) {
				ItemEntity food = close.get(0);
				food.discard(); // the whole dropped stack disappears, no chewing
				level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.7, this.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
			}
		}
	}

	private static CatSoundVariant.CatSoundSet sounds() {
		return SoundEvents.CAT_SOUNDS.get(CatSoundVariants.SoundSet.CLASSIC).adultSounds();
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return this.random.nextInt(3) == 0 ? sounds().purrSound().value() : sounds().ambientSound().value();
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return sounds().hurtSound().value();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return sounds().deathSound().value();
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return false; // no breeding
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	/** Walk to the nearest dropped food within 8 blocks. */
	static class GoToDroppedFoodGoal extends Goal {
		private final Tux tux;
		private @Nullable ItemEntity target;

		GoToDroppedFoodGoal(Tux tux) {
			this.tux = tux;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		private @Nullable ItemEntity find() {
			ItemEntity best = null;
			double bestDist = Double.MAX_VALUE;
			for (ItemEntity item : this.tux.level().getEntitiesOfClass(ItemEntity.class, this.tux.getBoundingBox().inflate(FOOD_SEARCH_RANGE),
					Tux::isDroppedFood)) {
				double d = item.distanceToSqr(this.tux);
				if (d < bestDist && d <= FOOD_SEARCH_RANGE * FOOD_SEARCH_RANGE) {
					best = item;
					bestDist = d;
				}
			}
			return best;
		}

		@Override
		public boolean canUse() {
			this.target = find();
			return this.target != null;
		}

		@Override
		public boolean canContinueToUse() {
			return this.target != null && this.target.isAlive() && this.target.distanceToSqr(this.tux) <= (FOOD_SEARCH_RANGE + 2) * (FOOD_SEARCH_RANGE + 2);
		}

		@Override
		public void start() {
			this.tux.getNavigation().moveTo(this.target, 1.1);
		}

		@Override
		public void stop() {
			this.target = null;
			this.tux.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (this.target == null) return;
			this.tux.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
			if (this.tux.getNavigation().isDone() || this.tux.tickCount % 10 == 0) {
				this.tux.getNavigation().moveTo(this.target, 1.1);
			}
		}
	}
}
