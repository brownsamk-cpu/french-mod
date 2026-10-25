package com.frenchmod.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.frenchmod.furniture.ModFurniture;
import com.frenchmod.furniture.SeatBlock;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;

/**
 * Dev-only: real in-game screenshots of the furniture (a little cottage room, sitting on the couch, the inventory).
 * Run with ./gradlew runClientGameTest -PclientShots (see SteelScreenshots for the headless settings).
 */
public class FurnitureScreenshots implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		Path out = Path.of(System.getProperty("frenchmod.shots", "build/steel_shots"));
		context.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			var server = sp.getServer();
			sp.getConnection().waitForChunksRender();
			for (String cmd : List.of("gamerule send_command_feedback false", "gamemode survival @a", "time set 18000", "weather clear",
					"gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false",
					// Night time, so the lamps light up the room. A little cottage room: oak floor, white walls, a roof, and a window behind the couch.
					"fill -6 99 -8 6 99 6 minecraft:oak_planks", "fill -6 100 -8 6 106 6 minecraft:air",
					"fill -5 100 -6 5 104 -6 minecraft:white_concrete", "fill -5 100 -6 -5 104 6 minecraft:white_concrete",
					"fill 5 100 -6 5 104 6 minecraft:white_concrete", "fill -5 105 -6 5 105 6 minecraft:spruce_planks",
					"fill -1 101 -6 1 102 -6 minecraft:glass_pane",
					// The furniture.
					"setblock 0 100 -5 frenchmod:couch[facing=south]",
					"setblock 1 100 -5 frenchmod:couch[facing=south]",
					"setblock -1 100 -5 frenchmod:lamp[facing=south,lit=true]",
					"setblock 2 100 -5 frenchmod:lamp[facing=south,lit=true]",
					"setblock 0 100 -3 frenchmod:table[facing=south]",
					"setblock -1 100 -3 frenchmod:chair[facing=east]",
					"setblock 1 100 -3 frenchmod:chair[facing=west]",
					"setblock -4 100 -4 frenchmod:fancy_bed[facing=north,part=head]",
					"setblock -4 100 -3 frenchmod:fancy_bed[facing=north,part=foot]",
					"setblock 4 100 -5 frenchmod:lamp[facing=south,lit=true]",
					"kill @e[type=item]")) {
				server.runCommand(cmd);
			}
			// Carpet on the floor (the fill above only works on air, so place it on top of the planks instead).
			server.runCommand("fill -1 100 -4 1 100 -2 minecraft:light_blue_carpet replace minecraft:air");
			server.runCommand("item replace entity @a hotbar.0 with frenchmod:chair");
			server.runCommand("item replace entity @a hotbar.1 with frenchmod:table");
			server.runCommand("item replace entity @a hotbar.2 with frenchmod:fancy_bed");
			server.runCommand("item replace entity @a hotbar.3 with frenchmod:couch");
			server.runCommand("item replace entity @a hotbar.4 with frenchmod:lamp");
			server.runCommand("tp @a 0.5 100 2.5 180 15");
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(4));
			sp.getConnection().waitForChunksRender();
			context.waitTicks(220);

			// 1) The room, first person.
			save(context, "frenchmod_furniture_room", out);

			// 2) The inventory with the five pieces.
			context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(context.computeOnClient(mc -> mc.player)));
			context.getInput().setCursorPos(40, 40);
			context.waitTicks(10);
			save(context, "frenchmod_furniture_inventory", out);
			context.setScreen(() -> null);

			// 3) Sitting on the couch, seen from the front (third person).
			server.runOnServer(s -> {
				var player = s.getPlayerList().getPlayers().get(0);
				((SeatBlock) ModFurniture.COUCH).sit(player.level(), new BlockPos(0, 100, -5), player);
			});
			context.waitTicks(20);
			context.runOnClient(mc -> { mc.player.setYRot(0); mc.player.setYHeadRot(0); mc.player.setYBodyRot(0); mc.player.setXRot(10); }); // look out of the couch, into the room
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(30);
			save(context, "frenchmod_furniture_sitting", out);

			// 4) Close-up of the chair and table, and the lamp switched off.
			server.runCommand("setblock 4 100 -5 frenchmod:lamp[facing=south,lit=false]");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			server.runOnServer(s -> s.getPlayerList().getPlayers().get(0).stopRiding());
			context.waitTicks(10);
			server.runCommand("tp @a 2.5 100 0.5 150 25");
			context.waitTicks(30);
			save(context, "frenchmod_furniture_closeup", out);
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
