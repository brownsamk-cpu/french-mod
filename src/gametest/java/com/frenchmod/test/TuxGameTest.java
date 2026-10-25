package com.frenchmod.test;

import com.frenchmod.ModSteel;
import com.frenchmod.tux.ModTux;
import com.frenchmod.tux.Tux;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
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
}
