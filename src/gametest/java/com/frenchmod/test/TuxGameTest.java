package com.frenchmod.test;

import com.frenchmod.ModSteel;
import com.frenchmod.tux.ModTux;
import com.frenchmod.tux.Tux;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class TuxGameTest {
	private static void floor(GameTestHelper h) {
		for (int x = -1; x <= 6; x++) {
			for (int z = -2; z <= 2; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
			}
		}
	}

	private static ItemEntity drop(GameTestHelper h, net.minecraft.world.item.Item item, double x) {
		ItemEntity e = h.spawnItem(item, new Vec3(x, 1.1, 0.5));
		e.setDeltaMovement(Vec3.ZERO);
		return e;
	}

	/** Bread dropped 4 blocks away: Tux walks over and the bread disappears. */
	@GameTest(maxTicks = 300)
	public void tuxWalksToDroppedBreadAndEatsIt(GameTestHelper h) {
		floor(h);
		h.assertTrue(ModSteel.ITEMS.contains(ModTux.TUX_SPAWN_EGG), "Tux Spawn Egg in the French Mod tab");
		Tux tux = h.spawn(ModTux.TUX, new Vec3(0.5, 1.0, 0.5));
		ItemEntity bread = drop(h, Items.BREAD, 4.5);
		h.assertTrue(bread.distanceTo(tux) > 3.0, "bread starts away from Tux");
		h.succeedWhen(() -> h.assertTrue(bread.isRemoved() && tux.isAlive(), "Tux has not eaten the bread yet"));
	}

	/** A stick is not food: Tux leaves it alone. */
	@GameTest(maxTicks = 200)
	public void tuxIgnoresAStick(GameTestHelper h) {
		floor(h);
		Tux tux = h.spawn(ModTux.TUX, new Vec3(0.5, 1.0, 0.5));
		ItemEntity stick = drop(h, Items.STICK, 2.5);
		h.runAfterDelay(120, () -> {
			h.assertTrue(!stick.isRemoved() && stick.getItem().is(Items.STICK), "the stick should still be there");
			h.assertTrue(tux.isAlive(), "Tux is fine");
			h.succeed();
		});
	}

	/** A chest with 2 bread and a stick: Tux takes ONE bread, the other bread and the stick stay. */
	@GameTest(maxTicks = 300)
	public void tuxStealsOneBreadFromAChest(GameTestHelper h) {
		floor(h);
		BlockPos chestPos = new BlockPos(4, 1, 0);
		h.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
		ChestBlockEntity chest = (ChestBlockEntity) h.getBlockEntity(chestPos, ChestBlockEntity.class);
		chest.setItem(0, new ItemStack(Items.BREAD, 2));
		chest.setItem(1, new ItemStack(Items.STICK));
		Tux tux = h.spawn(ModTux.TUX, new Vec3(0.5, 1.0, 0.5));
		h.succeedWhen(() -> {
			h.assertTrue(chest.getItem(0).is(Items.BREAD) && chest.getItem(0).getCount() == 1, "Tux should take one bread, slot 0 is " + chest.getItem(0));
			h.assertTrue(chest.getItem(1).is(Items.STICK) && chest.getItem(1).getCount() == 1, "the stick stays");
			h.assertTrue(tux.isAlive(), "Tux is fine");
		});
	}

	/** An item frame holding an apple: Tux takes the apple, the empty frame stays. */
	@GameTest(maxTicks = 300)
	public void tuxStealsTheAppleFromAnItemFrame(GameTestHelper h) {
		floor(h);
		h.setBlock(new BlockPos(3, 1, 0), Blocks.STONE.defaultBlockState());
		ItemFrame frame = new ItemFrame(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 1)), Direction.SOUTH);
		h.getLevel().addFreshEntity(frame);
		frame.setItem(new ItemStack(Items.APPLE));
		h.assertTrue(frame.getItem().is(Items.APPLE), "frame holds the apple");
		h.spawn(ModTux.TUX, new Vec3(0.5, 1.0, 2.5));
		h.succeedWhen(() -> h.assertTrue(frame.isAlive() && frame.getItem().isEmpty(), "the frame should be empty, has " + frame.getItem()));
	}
}
