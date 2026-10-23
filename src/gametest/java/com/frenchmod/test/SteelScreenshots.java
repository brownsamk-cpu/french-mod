package com.frenchmod.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.frenchmod.ModSteel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Dev-only: real in-game screenshots of the steel set. Not part of the mod jar and not run by ./gradlew build.
 * Run with: ./gradlew runClientGameTest -PclientShots   (offline dev player, singleplayer test world)
 * Headless boxes: Minecraft asks for an sRGB window, which plain GLX on Xvfb can't give, so use EGL:
 *   DISPLAY=:77 LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe SDL_VIDEO_FORCE_EGL=1 ./gradlew runClientGameTest -PclientShots
 * Pictures are copied to the folder in the system property frenchmod.shots (default build/steel_shots).
 */
public class SteelScreenshots implements FabricClientGameTest {
	private static String id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		Path out = Path.of(System.getProperty("frenchmod.shots", "build/steel_shots"));
		context.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			var server = sp.getServer();
			sp.getConnection().waitForChunksRender();
			for (String cmd : List.of("gamerule send_command_feedback false", "gamemode survival @a", "time set 6000", "weather clear", "gamerule advance_time false",
					"gamerule advance_weather false", "gamerule spawn_mobs false",
					"fill -8 99 -6 8 99 12 minecraft:oak_planks", "fill -8 100 -6 8 110 12 minecraft:air",
					"fill -8 100 -6 8 104 -6 minecraft:white_concrete",
					// Block of Iron (left as you look at it) and Block of Steel (right), each with a sign.
					"setblock 2 100 -2 minecraft:iron_block", "setblock -2 100 -2 frenchmod:steel_block",
					"setblock 2 101 -2 minecraft:oak_sign[rotation=0]{front_text:{messages:[\"\",\"Iron\",\"\",\"\"]}}",
					"setblock -2 101 -2 minecraft:oak_sign[rotation=0]{front_text:{messages:[\"\",\"Steel\",\"\",\"\"]}}",
					// And a pair touching each other for the close-up: steel (left) next to iron (right).
					"setblock -1 100 7 frenchmod:steel_block", "setblock 0 100 7 minecraft:iron_block",
					"setblock -1 101 7 minecraft:oak_sign[rotation=0]{front_text:{messages:[\"\",\"Steel\",\"\",\"\"]}}",
					"setblock 0 101 7 minecraft:oak_sign[rotation=0]{front_text:{messages:[\"\",\"Iron\",\"\",\"\"]}}",
					// An armor stand in iron armor, to compare with the player in steel.
					"summon minecraft:armor_stand 3.5 100 0.5 {Rotation:[0f],NoBasePlate:1b,ShowArms:1b,equipment:{head:{id:\"minecraft:iron_helmet\"},"
							+ "chest:{id:\"minecraft:iron_chestplate\"},legs:{id:\"minecraft:iron_leggings\"},feet:{id:\"minecraft:iron_boots\"},"
							+ "mainhand:{id:\"minecraft:iron_sword\"}}}")) {
				server.runCommand(cmd);
			}
			// The player wears the full steel set and holds the steel sword.
			for (String[] slot : new String[][] {{"armor.head", id(ModSteel.STEEL_HELMET)}, {"armor.chest", id(ModSteel.STEEL_CHESTPLATE)},
					{"armor.legs", id(ModSteel.STEEL_LEGGINGS)}, {"armor.feet", id(ModSteel.STEEL_BOOTS)}, {"hotbar.0", id(ModSteel.STEEL_SWORD)}}) {
				server.runCommand("item replace entity @a " + slot[0] + " with " + slot[1]);
			}

			// 1) The inventory: iron on top, steel right underneath (the doll is wearing steel armor).
			List<Item> iron = List.of(Items.IRON_INGOT, Items.IRON_NUGGET, Items.IRON_BLOCK, Items.IRON_SWORD, Items.IRON_SHOVEL, Items.IRON_PICKAXE,
					Items.IRON_AXE, Items.IRON_HOE, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
			List<Item> steel = List.of(ModSteel.STEEL_INGOT, ModSteel.STEEL_NUGGET, ModSteel.STEEL_BLOCK_ITEM, ModSteel.STEEL_SWORD, ModSteel.STEEL_SHOVEL,
					ModSteel.STEEL_PICKAXE, ModSteel.STEEL_AXE, ModSteel.STEEL_HOE, ModSteel.STEEL_HELMET, ModSteel.STEEL_CHESTPLATE, ModSteel.STEEL_LEGGINGS,
					ModSteel.STEEL_BOOTS);
			for (int i = 0; i < 9; i++) { // rows 1-2: ingot ... helmet
				server.runCommand("item replace entity @a inventory." + i + " with " + id(iron.get(i)) + (i < 3 ? " 16" : ""));
				server.runCommand("item replace entity @a inventory." + (9 + i) + " with " + id(steel.get(i)) + (i < 3 ? " 16" : ""));
			}
			for (int i = 9; i < 12; i++) { // row 3 iron chest/legs/boots, hotbar steel chest/legs/boots
				server.runCommand("item replace entity @a inventory." + (18 + i - 9) + " with " + id(iron.get(i)));
				server.runCommand("item replace entity @a hotbar." + (i - 8) + " with " + id(steel.get(i)));
			}
			// How steel is made, at the end: iron ingot + coal -> steel blend.
			server.runCommand("item replace entity @a inventory.24 with minecraft:iron_ingot 8");
			server.runCommand("item replace entity @a inventory.25 with minecraft:coal 8");
			server.runCommand("item replace entity @a inventory.26 with " + id(ModSteel.STEEL_BLEND) + " 8");
			server.runCommand("item replace entity @a hotbar.8 with " + id(ModSteel.STEEL_BLEND) + " 8");
			server.runCommand("tp @a 0.5 100 2.5 180 0");
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
			sp.getConnection().waitForChunksRender();
			context.waitTicks(220); // let the game-mode chat line and toasts fade
			context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(context.computeOnClient(mc -> mc.player)));
			context.getInput().setCursorPos(40, 40); // park the mouse off the panel so no tooltip hides an item
			context.waitTicks(10);
			save(context, "frenchmod_steel_inventory", out);
			context.setScreen(() -> null);

			// 2) Third person, from the front: the player in full steel armor next to an armor stand in iron armor,
			//    with the Block of Steel and Block of Iron behind.
			server.runCommand("tp @a 0.5 100 0.5 0 0");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(30);
			save(context, "frenchmod_steel_armor", out);

			// 3) Close-up of a Block of Steel right next to a Block of Iron, first person.
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand("tp @a 0.0 100 10.3 180 20");
			context.waitTicks(30);
			save(context, "frenchmod_steel_blocks", out);
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
