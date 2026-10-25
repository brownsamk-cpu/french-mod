package com.frenchmod.test;

import java.util.List;

import com.frenchmod.ModSteel;
import com.frenchmod.furniture.FancyBedBlock;
import com.frenchmod.furniture.LampBlock;
import com.frenchmod.furniture.ModFurniture;
import com.frenchmod.furniture.Seat;
import com.frenchmod.furniture.SeatBlock;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class FurnitureGameTest {
	private static BlockHitResult hit(GameTestHelper h, BlockPos rel) {
		BlockPos abs = h.absolutePos(rel);
		return new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
	}

	/** All five can be placed facing every way, are in the French Mod tab, and drop themselves when broken. */
	@GameTest
	public void everyPiecePlacesAndDropsItself(GameTestHelper h) {
		List<Block> pieces = List.of(ModFurniture.CHAIR, ModFurniture.TABLE, ModFurniture.COUCH, ModFurniture.LAMP);
		int x = 0;
		for (Block b : pieces) {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos rel = new BlockPos(x, 1, 0);
				h.setBlock(rel, b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d));
				h.assertBlockPresent(b, rel);
				List<ItemStack> drops = Block.getDrops(h.getBlockState(rel), h.getLevel(), h.absolutePos(rel), null);
				h.assertTrue(drops.size() == 1 && drops.get(0).is(b.asItem()), b + " should drop itself, got " + drops);
			}
			h.assertTrue(ModSteel.ITEMS.contains(b.asItem()), b + " in the creative tab");
			x++;
		}
		h.assertTrue(ModSteel.ITEMS.contains(ModFurniture.FANCY_BED_ITEM), "bed in the creative tab");
		h.succeed();
	}

	/** The Fancy Bed is a real bed: two halves, sleepable (an AbstractBedBlock with a sleep height), and only the head drops one bed. */
	@GameTest
	public void fancyBedIsARealBed(GameTestHelper h) {
		BlockPos foot = new BlockPos(1, 1, 1), head = foot.north();
		BlockState footState = ModFurniture.FANCY_BED.defaultBlockState().setValue(AbstractBedBlock.FACING, Direction.NORTH);
		h.setBlock(foot, footState);
		h.setBlock(head, footState.setValue(AbstractBedBlock.PART, BedPart.HEAD));
		h.assertTrue(ModFurniture.FANCY_BED instanceof AbstractBedBlock && ModFurniture.FANCY_BED instanceof FancyBedBlock, "is a bed");
		h.assertTrue(((AbstractBedBlock) ModFurniture.FANCY_BED).getSleepHeight(h.getBlockState(head), h.getLevel(), h.absolutePos(head)).isPresent(),
				"can lie in it");
		h.assertTrue(((AbstractBedBlock) ModFurniture.FANCY_BED).getBedRule(h.getLevel(), h.absolutePos(head)).canSetSpawn(h.getLevel()), "sets spawn");
		List<ItemStack> headDrops = Block.getDrops(h.getBlockState(head), h.getLevel(), h.absolutePos(head), null);
		List<ItemStack> footDrops = Block.getDrops(h.getBlockState(foot), h.getLevel(), h.absolutePos(foot), null);
		h.assertTrue(headDrops.size() == 1 && headDrops.get(0).is(ModFurniture.FANCY_BED_ITEM) && footDrops.isEmpty(), "one bed back: " + headDrops + footDrops);
		h.setBlock(head, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
		h.succeedWhen(() -> h.assertBlockNotPresent(ModFurniture.FANCY_BED, foot));
	}

	private static void sitTest(GameTestHelper h, Block seatBlock) {
		BlockPos rel = new BlockPos(1, 1, 1);
		h.setBlock(rel, seatBlock.defaultBlockState());
		ServerPlayer p = h.makeMockServerPlayerInLevel();
		Vec3 v = h.absoluteVec(new Vec3(1.5, 1, 3.5));
		p.snapTo(v.x, v.y, v.z);
		h.getBlockState(rel).useWithoutItem(h.getLevel(), p, hit(h, rel));
		h.assertTrue(p.getVehicle() instanceof Seat, seatBlock + ": right-click sits you down");
		Seat seat = (Seat) p.getVehicle();
		double seatY = h.absolutePos(rel).getY() + ((SeatBlock) seatBlock).seatHeight;
		h.assertTrue(Math.abs(seat.getY() - seatY) < 0.01, "seat height " + seat.getY());
		ServerPlayer other = h.makeMockServerPlayerInLevel();
		h.getBlockState(rel).useWithoutItem(h.getLevel(), other, hit(h, rel));
		h.assertTrue(!other.isPassenger(), "only one sitter at a time");
		p.stopRiding(); // what sneaking does
		h.assertTrue(!p.isPassenger(), "stood up");
		seat.tick();
		h.assertTrue(seat.isRemoved(), "empty seat goes away");
		// Breaking the furniture under you also stands you up.
		h.getBlockState(rel).useWithoutItem(h.getLevel(), p, hit(h, rel));
		Seat seat2 = (Seat) p.getVehicle();
		h.assertTrue(seat2 != null, "sat again");
		h.destroyBlock(rel);
		seat2.tick();
		h.assertTrue(seat2.isRemoved() && !p.isPassenger(), "broken chair stands you up");
		h.succeed();
	}

	@GameTest
	public void chairCanBeSatOn(GameTestHelper h) {
		sitTest(h, ModFurniture.CHAIR);
	}

	@GameTest
	public void couchCanBeSatOn(GameTestHelper h) {
		sitTest(h, ModFurniture.COUCH);
	}

	/** The lamp glows (light 15), right-click switches it off (no light) and back on. */
	@GameTest
	public void lampGlowsAndToggles(GameTestHelper h) {
		BlockPos rel = new BlockPos(1, 1, 1);
		h.setBlock(rel, ModFurniture.LAMP.defaultBlockState());
		h.assertTrue(h.getBlockState(rel).getValue(LampBlock.LIT) && h.getBlockState(rel).getLightEmission() == 15, "starts on, light 15");
		ServerPlayer p = h.makeMockServerPlayerInLevel();
		h.getBlockState(rel).useWithoutItem(h.getLevel(), p, hit(h, rel));
		h.assertTrue(!h.getBlockState(rel).getValue(LampBlock.LIT) && h.getBlockState(rel).getLightEmission() == 0, "switched off");
		h.getBlockState(rel).useWithoutItem(h.getLevel(), p, hit(h, rel));
		h.assertTrue(h.getBlockState(rel).getValue(LampBlock.LIT), "switched back on");
		h.succeedWhen(() -> {
			int light = h.getLevel().getBrightness(LightLayer.BLOCK, h.absolutePos(rel.east()));
			h.assertTrue(light >= 14, "lights up next to it: " + light);
		});
	}
}
