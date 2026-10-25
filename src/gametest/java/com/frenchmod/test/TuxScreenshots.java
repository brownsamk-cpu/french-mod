package com.frenchmod.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.frenchmod.tux.ModTux;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Dev-only: real in-game screenshots of Tux (close-up, Tux next to dropped food, and after the food is gone).
 * Run with ./gradlew runClientGameTest -PclientShots (see SteelScreenshots for the headless settings).
 */
public class TuxScreenshots implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		Path out = Path.of(System.getProperty("frenchmod.shots", "build/steel_shots"));
		context.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			var server = sp.getServer();
			sp.getConnection().waitForChunksRender();
			for (String cmd : List.of("gamerule send_command_feedback false", "gamemode creative @a", "time set 6000", "weather clear",
					"gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false",
					"fill -6 99 -8 6 99 6 minecraft:grass_block", "fill -6 100 -8 6 106 6 minecraft:air", "kill @e[type=!player]")) {
				server.runCommand(cmd);
			}
			// Tux holds still for the close-up (no AI).
			server.runCommand("summon frenchmod:tux 0.5 100 -1.5 {NoAI:1b,Rotation:[-45f,0f]}");
			// In front of Tux, looking at the face, white chin and white chest.
			server.runCommand("tp @e[type=frenchmod:tux] 0.5 100 -1.5 -45 0");
			server.runCommand("tp @a 2.3 100 0.3 135 24");
			sp.getConnection().waitForChunksRender();
			context.waitTicks(80);
			save(context, "frenchmod_tux_closeup", out);

			// Tux next to some dropped food: AI on, the bread is a few blocks away, shot taken right away.
			server.runCommand("tp @e[type=frenchmod:tux] -1.0 100 -2.5 -90 0");
			server.runCommand("tp @a 0.5 100 1.0 180 30");
			server.runOnServer(s -> {
				var level = s.overworld();
				ItemEntity bread = new ItemEntity(level, 1.5, 100.05, -2.5, new ItemStack(Items.BREAD), 0, 0, 0);
				bread.setPickUpDelay(32767);
				level.addFreshEntity(bread);
			});
			context.waitTicks(30);
			save(context, "frenchmod_tux_food", out);

			// Let Tux go: Tux walks to the bread and it disappears.
			server.runOnServer(s -> s.overworld().getEntities(ModTux.TUX, t -> true).forEach(t -> t.setNoAi(false)));
			context.waitTicks(160);
			save(context, "frenchmod_tux_food_gone", out);
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
