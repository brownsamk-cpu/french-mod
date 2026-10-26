package com.frenchmod.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/**
 * Dev-only (0.4.0): a little French bathroom with the Bathtub (full of water), the Toilet and the French Candle, plus a close-up of the lit candle.
 * Run with ./gradlew runClientGameTest -PclientShots.
 */
public class BathroomScreenshots implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		Path out = Path.of(System.getProperty("frenchmod.shots", "build/steel_shots"));
		context.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			var server = sp.getServer();
			sp.getConnection().waitForChunksRender();
			for (String cmd : List.of("gamerule send_command_feedback false", "gamemode survival @a", "time set 18000", "weather clear",
					"gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false",
					// Evening, so the candles glow. A little bathroom: light blue tiles on the floor, white walls, a roof, a window.
					"fill -6 99 -8 6 99 6 minecraft:smooth_quartz", "fill -6 100 -8 6 106 6 minecraft:air",
					"fill -3 99 -4 3 99 -1 minecraft:light_blue_concrete",
					"fill -4 100 -5 4 104 -5 minecraft:white_concrete", "fill -4 100 -5 -4 104 4 minecraft:white_concrete",
					"fill 4 100 -5 4 104 4 minecraft:white_concrete", "fill -4 105 -5 4 105 4 minecraft:spruce_planks",
					"fill -1 102 -5 0 103 -5 minecraft:glass_pane",
					// Bathtub along the back wall: the sitting end on the right (x=-1), the gold tap end on the left (x=-2).
					"setblock -1 100 -4 frenchmod:bathtub[facing=east,part=head,filled=true]",
					"setblock -2 100 -4 frenchmod:bathtub[facing=east,part=foot,filled=true]",
					"setblock 2 100 -4 frenchmod:toilet[facing=south]",
					"setblock 0 100 -4 frenchmod:golden_trim_table[facing=south]",
					"setblock 3 100 -2 frenchmod:golden_trim_table[facing=west]",
					"kill @e[type=item]")) {
				server.runCommand(cmd);
			}
			server.runCommand("setblock 0 101 -4 frenchmod:french_candle[facing=south,lit=true]");
			server.runCommand("setblock 3 101 -2 frenchmod:french_candle[facing=west,lit=true]");
			server.runCommand("item replace entity @a hotbar.0 with frenchmod:bathtub");
			server.runCommand("item replace entity @a hotbar.1 with frenchmod:toilet");
			server.runCommand("item replace entity @a hotbar.2 with frenchmod:french_candle");
			server.runCommand("tp @a 0.5 100 1.2 180 28");
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(2));
			sp.getConnection().waitForChunksRender();
			context.waitTicks(220);
			save(context, "frenchmod_bathroom", out);

			// Close-up of the lit candle on the table.
			server.runCommand("tp @a 0.5 100 -1.6 180 22");
			context.waitTicks(40);
			save(context, "frenchmod_candle_lit", out);
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
