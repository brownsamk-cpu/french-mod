package com.frenchmod.test;

import java.util.List;

import com.frenchmod.hideseek.HideSeekGame;
import com.frenchmod.hideseek.ModHideSeek;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public class HideSeekGameTest {
	private static void at(GameTestHelper h, ServerPlayer p, double x, double z) {
		Vec3 v = h.absoluteVec(new Vec3(x, 1, z));
		p.snapTo(v.x, v.y, v.z);
	}

	/** Hide phase counts down from 1:00 with the seeker blinded+slowed, then the seek timer counts up. */
	@GameTest
	public void phasesAndTimers(GameTestHelper h) {
		MinecraftServer server = h.getLevel().getServer();
		ServerPlayer seeker = h.makeMockServerPlayerInLevel(), hider = h.makeMockServerPlayerInLevel();
		at(h, seeker, 0.5, 0.5); at(h, hider, 6.5, 6.5);
		HideSeekGame g = new HideSeekGame(seeker.getUUID(), List.of(seeker.getUUID(), hider.getUUID()), HideSeekGame.DEFAULT_HIDE_TICKS);
		h.assertTrue(g.hiders.size() == 1 && !g.hiders.contains(seeker.getUUID()), "seeker must not be a hider");
		g.begin(server);
		h.assertTrue(g.phase() == HideSeekGame.Phase.HIDE, "starts hiding");
		h.assertTrue(HideSeekGame.countdown(g.hideTicksLeft()).equals("1:00"), "countdown shows 1:00");
		h.assertTrue(seeker.hasEffect(MobEffects.BLINDNESS) && seeker.hasEffect(MobEffects.SLOWNESS), "seeker blinded and slowed");
		h.assertTrue(!hider.hasEffect(MobEffects.BLINDNESS), "hider not blinded");
		for (int i = 0; i < 20 * 59; i++) g.tick(server);
		h.assertTrue(g.phase() == HideSeekGame.Phase.HIDE && HideSeekGame.countdown(g.hideTicksLeft()).equals("0:01"), "1 second left: " + g.hideTicksLeft());
		for (int i = 0; i < 20; i++) g.tick(server);
		h.assertTrue(g.phase() == HideSeekGame.Phase.SEEK, "seeking after one minute");
		h.assertTrue(!seeker.hasEffect(MobEffects.BLINDNESS) && !seeker.hasEffect(MobEffects.SLOWNESS), "seeker can see again");
		for (int i = 0; i < 20 * 135; i++) g.tick(server);
		h.assertTrue(HideSeekGame.clock(g.seekTicks()).equals("2:15"), "seek timer counts up: " + HideSeekGame.clock(g.seekTicks()));
		h.assertTrue(g.hud().phase() == 2 && g.hud().ticks() == g.seekTicks(), "HUD payload matches");
		g.cancel(server);
		h.succeed();
	}

	/** Touching (within 1.5 blocks, in sight) finds a hider and stops their time; tagging is ignored during hiding. */
	@GameTest
	public void foundByTouchAndTag(GameTestHelper h) {
		MinecraftServer server = h.getLevel().getServer();
		ServerPlayer seeker = h.makeMockServerPlayerInLevel(), a = h.makeMockServerPlayerInLevel(), b = h.makeMockServerPlayerInLevel();
		at(h, seeker, 0.5, 0.5); at(h, a, 8.5, 0.5); at(h, b, 0.5, 8.5);
		HideSeekGame g = new HideSeekGame(seeker.getUUID(), List.of(a.getUUID(), b.getUUID()), 40);
		g.begin(server);
		h.assertTrue(!g.found(server, seeker, a), "can't find anyone while they hide");
		for (int i = 0; i < 40; i++) g.tick(server);
		h.assertTrue(g.phase() == HideSeekGame.Phase.SEEK, "seeking");
		for (int i = 0; i < 100; i++) g.tick(server);
		h.assertTrue(g.foundAt.isEmpty(), "far away hiders are not found");
		at(h, a, 1.5, 0.5); // walk up and touch
		g.tick(server);
		h.assertTrue(g.foundAt.get(a.getUUID()) == 101, "a found by touch at 101 ticks, got " + g.foundAt);
		h.assertTrue(!g.found(server, a, b), "a hider can't 'find' someone");
		for (int i = 0; i < 50; i++) g.tick(server);
		h.assertTrue(g.foundAt.get(a.getUUID()) == 101, "a's time stopped");
		h.assertTrue(g.found(server, seeker, b), "tagging b finds b");
		h.assertTrue(g.phase() == HideSeekGame.Phase.DONE, "game ends when everyone is found");
		h.succeed();
	}

	/** A wall between them blocks touch-finding (needs line of sight). */
	@GameTest
	public void noFindThroughWalls(GameTestHelper h) {
		MinecraftServer server = h.getLevel().getServer();
		ServerPlayer seeker = h.makeMockServerPlayerInLevel(), a = h.makeMockServerPlayerInLevel();
		for (int y = 1; y <= 3; y++) for (int z = 0; z <= 2; z++) h.setBlock(1, y, z, net.minecraft.world.level.block.Blocks.STONE);
		at(h, seeker, 0.3, 1.5); at(h, a, 2.3, 1.5);
		HideSeekGame g = new HideSeekGame(seeker.getUUID(), List.of(a.getUUID()), 1);
		g.begin(server);
		for (int i = 0; i < 10; i++) g.tick(server);
		h.assertTrue(g.foundAt.isEmpty(), "found through a wall");
		g.cancel(server);
		h.succeed();
	}

	/** During a game nobody playing takes damage (hardcore-safe); after it ends damage works again. */
	@GameTest(maxTicks = 200)
	public void noDamageDuringGame(GameTestHelper h) {
		MinecraftServer server = h.getLevel().getServer();
		ServerPlayer seeker = h.makeMockServerPlayerInLevel(), a = h.makeMockServerPlayerInLevel();
		at(h, seeker, 0.5, 0.5); at(h, a, 4.5, 4.5);
		seeker.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		a.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		h.assertTrue(!a.isInvulnerable() && !a.getAbilities().invulnerable, "test players must be hurtable");
		// New players can't be hurt for their first 3 seconds anyway, so wait until that is over.
		h.runAfterDelay(80, () -> {
			HideSeekGame g = ModHideSeek.start(server, seeker, 20);
			try {
				h.assertTrue(g.isPlayer(a.getUUID()), "a is a hider");
				float before = a.getHealth();
				boolean hurt = a.hurtServer(h.getLevel(), a.damageSources().playerAttack(seeker), 5f);
				boolean hurt2 = seeker.hurtServer(h.getLevel(), seeker.damageSources().generic(), 50f);
				h.assertTrue(!hurt && !hurt2 && a.getHealth() == before && seeker.getHealth() == seeker.getMaxHealth(), "no damage in game");
			} finally {
				ModHideSeek.stop(server);
			}
			h.assertTrue(a.hurtServer(h.getLevel(), a.damageSources().generic(), 1f), "damage normal again after the game");
			h.succeed();
		});
	}
}
