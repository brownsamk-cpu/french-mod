package com.frenchmod.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import com.frenchmod.hideseek.HideSeekGame;
import com.frenchmod.hideseek.ModHideSeek;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;

/**
 * Dev-only: real in-game screenshots of the hide-and-seek timers (same setup as SteelScreenshots).
 * The dev player is the seeker. Dad is stood in by a pretend server player called "Papa" (only one real player on this box).
 */
public class HideSeekScreenshots implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		Path out = Path.of(System.getProperty("frenchmod.shots", "build/steel_shots"));
		context.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			var server = sp.getServer();
			sp.getConnection().waitForChunksRender();
			for (String cmd : List.of("gamerule send_command_feedback false", "gamemode survival @a", "time set 6000", "weather clear",
					"gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false",
					"fill -8 99 -8 8 99 8 minecraft:grass_block", "fill -8 100 -8 8 110 8 minecraft:air",
					"setblock 3 100 -4 minecraft:oak_log", "setblock 3 101 -4 minecraft:oak_log", "fill 2 102 -5 4 103 -3 minecraft:oak_leaves",
					"setblock -3 100 -3 minecraft:hay_block", "tp @a 0.5 100 0.5 180 10")) {
				server.runCommand(cmd);
			}
			context.waitTicks(220); // let the game-mode chat line fade
			UUID papa = UUID.nameUUIDFromBytes("Papa".getBytes());
			UUID me = context.computeOnClient(mc -> mc.player.getUUID());
			// 1) Hide phase: "Cache-toi!" and the 1:00 countdown, top-left. (The seeker's screen goes dark: blindness.)
			server.runOnServer(s -> ModHideSeek.run(s, new HideSeekGame(me, List.of(papa), HideSeekGame.DEFAULT_HIDE_TICKS)));
			context.waitTicks(30);
			save(context, "frenchmod_hideseek_hide_countdown", out);
			// Same countdown a bit later, with the blindness cleared just for the picture so you can see the world.
			context.waitTicks(200);
			server.runCommand("effect clear @a");
			context.waitTicks(3);
			save(context, "frenchmod_hideseek_hide_countdown_clear", out);
			// 2) Fast-forward the game's own timer to seeking, 2:14 in.
			server.runOnServer(s -> {
				HideSeekGame g = ModHideSeek.current;
				while (g.phase() == HideSeekGame.Phase.HIDE) g.tick(s);
				for (int i = 0; i < 20 * 134; i++) g.tick(s);
			});
			context.waitTicks(25);
			save(context, "frenchmod_hideseek_seek_timer", out);
			// 3) The seeker tags Papa: "Trouvé! Found in 2:15!"
			server.runOnServer(s -> {
				HideSeekGame g = ModHideSeek.current;
				g.found(s, s.getPlayerList().getPlayer(me), FakePlayer.get(s.overworld(), new GameProfile(papa, "Papa")));
			});
			context.waitTicks(15);
			save(context, "frenchmod_hideseek_found", out);
		}
	}

	private static void save(ClientGameTestContext context, String name, Path out) {
		Path shot = context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
		try {
			Files.createDirectories(out);
			Files.copy(shot, out.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}
}
