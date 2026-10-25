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
 * Dev-only: real in-game screenshots of Tux (close-up, Tux next to dropped food, and after the food is gone; since 0.3.3 Tux at
 * an open chest he is stealing from, and Tux by the item frame he emptied).
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

			// 0.3.3: Tux steals one bread from a chest; the shot is taken while the lid is open.
			server.runCommand("kill @e[type=frenchmod:tux]");
			server.runCommand("kill @e[type=item]");
			server.runCommand("setblock 0 100 -3 minecraft:chest[facing=south]{Items:[{Slot:0b,id:\"minecraft:bread\",count:3},{Slot:1b,id:\"minecraft:stick\",count:1}]}");
			server.runCommand("tp @a 2.5 101 1.5 153 30");
			context.waitTicks(30);
			server.runCommand("summon frenchmod:tux -3.5 100 -1.5");
			server.waitFor(s -> s.overworld().getEntities(ModTux.TUX, t -> t.isLidOpen()).size() > 0, 600);
			context.waitTicks(6);
			save(context, "frenchmod_tux_chest", out);

			// 0.3.3: Tux takes the apple out of an item frame; the empty frame stays on the wall.
			server.runCommand("kill @e[type=frenchmod:tux]");
			server.runCommand("setblock 0 100 -3 minecraft:air");
			server.runCommand("fill -2 100 -4 2 101 -4 minecraft:stone_bricks");
			server.runCommand("summon minecraft:item_frame 0 100 -3 {Facing:3b,Item:{id:\"minecraft:apple\",count:1}}");
			server.runCommand("tp @a 0.5 101 1.5 180 25");
			context.waitTicks(30);
			server.runCommand("summon frenchmod:tux -3.5 100 0.5");
			server.waitFor(s -> s.overworld().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(net.minecraft.world.entity.decoration.ItemFrame.class), f -> f.getItem().isEmpty()).size() > 0, 600);
			context.waitTicks(15);
			save(context, "frenchmod_tux_item_frame", out);
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
