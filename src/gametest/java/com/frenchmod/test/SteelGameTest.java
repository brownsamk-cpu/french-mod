package com.frenchmod.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.frenchmod.ModSteel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;

public class SteelGameTest {
	private static final Item I = ModSteel.STEEL_INGOT, N = ModSteel.STEEL_NUGGET, B = ModSteel.STEEL_BLOCK_ITEM, S = Items.STICK, O = null;

	private static ItemStack s(Item item) {
		return item == null ? ItemStack.EMPTY : new ItemStack(item);
	}

	private static List<RecipeHolder<?>> matching(GameTestHelper helper, CraftingInput input) {
		List<RecipeHolder<?>> out = new ArrayList<>();
		for (RecipeHolder<?> holder : helper.getLevel().recipeAccess().getRecipes()) {
			if (holder.value() instanceof CraftingRecipe recipe && recipe.matches(input, helper.getLevel())) {
				out.add(holder);
			}
		}
		return out;
	}

	/** Exactly one crafting recipe matches this grid, and it makes count x result. */
	private static void expect(GameTestHelper helper, int w, int h, List<Item> grid, Item result, int count) {
		CraftingInput input = CraftingInput.of(w, h, grid.stream().map(SteelGameTest::s).toList());
		List<RecipeHolder<?>> found = matching(helper, input);
		helper.assertTrue(found.size() == 1, result + ": expected exactly one recipe, found " + found.stream().map(r -> r.id().identifier()).toList());
		ItemStack out = ((CraftingRecipe) found.get(0).value()).assemble(input);
		helper.assertTrue(out.is(result) && out.getCount() == count, "recipe for " + result + " gave " + out);
	}

	private static void expectCooked(GameTestHelper helper, RecipeType<?> type, Item input, Item result) {
		SingleRecipeInput in = new SingleRecipeInput(new ItemStack(input));
		List<ItemStack> outs = new ArrayList<>();
		for (RecipeHolder<?> holder : helper.getLevel().recipeAccess().getRecipes()) {
			if (holder.value() instanceof AbstractCookingRecipe recipe && recipe.getType() == type && recipe.matches(in, helper.getLevel())) {
				outs.add(recipe.assemble(in));
			}
		}
		helper.assertTrue(outs.size() == 1 && outs.get(0).is(result), type + " " + input + " should make " + result + ", got " + outs);
	}

	private static List<Item> g(Item... items) {
		return Arrays.asList(items);
	}

	/** Iron ingot + coal (or charcoal) = Steel Blend; smelt or blast it into a Steel Ingot; 9 ingots <-> block; 9 nuggets <-> ingot; the full set. */
	@GameTest
	public void steelIsMadeAndCrafted(GameTestHelper helper) {
		expect(helper, 2, 1, g(Items.IRON_INGOT, Items.COAL), ModSteel.STEEL_BLEND, 1);
		expect(helper, 2, 1, g(Items.COAL, Items.IRON_INGOT), ModSteel.STEEL_BLEND, 1);
		expect(helper, 2, 1, g(Items.IRON_INGOT, Items.CHARCOAL), ModSteel.STEEL_BLEND, 1);
		expectCooked(helper, RecipeType.SMELTING, ModSteel.STEEL_BLEND, I);
		expectCooked(helper, RecipeType.BLASTING, ModSteel.STEEL_BLEND, I);
		expect(helper, 3, 3, g(I, I, I, I, I, I, I, I, I), B, 1);
		expect(helper, 1, 1, g(B), I, 9);
		expect(helper, 1, 1, g(I), N, 9);
		expect(helper, 3, 3, g(N, N, N, N, N, N, N, N, N), I, 1);
		expect(helper, 1, 3, g(I, I, S), ModSteel.STEEL_SWORD, 1);
		expect(helper, 1, 3, g(I, S, S), ModSteel.STEEL_SHOVEL, 1);
		expect(helper, 3, 3, g(I, I, I, O, S, O, O, S, O), ModSteel.STEEL_PICKAXE, 1);
		expect(helper, 2, 3, g(I, I, I, S, O, S), ModSteel.STEEL_AXE, 1);
		expect(helper, 2, 3, g(I, I, S, I, S, O), ModSteel.STEEL_AXE, 1); // mirrored
		expect(helper, 2, 3, g(I, I, O, S, O, S), ModSteel.STEEL_HOE, 1);
		expect(helper, 3, 2, g(I, I, I, I, O, I), ModSteel.STEEL_HELMET, 1);
		expect(helper, 3, 3, g(I, O, I, I, I, I, I, I, I), ModSteel.STEEL_CHESTPLATE, 1);
		expect(helper, 3, 3, g(I, I, I, I, O, I, I, O, I), ModSteel.STEEL_LEGGINGS, 1);
		expect(helper, 3, 2, g(I, O, I, I, O, I), ModSteel.STEEL_BOOTS, 1);
		expectCooked(helper, RecipeType.SMELTING, ModSteel.STEEL_SWORD, N);
		expectCooked(helper, RecipeType.BLASTING, ModSteel.STEEL_CHESTPLATE, N);
		// Iron still makes iron things (no clashes).
		Item fe = Items.IRON_INGOT;
		expect(helper, 1, 3, g(fe, fe, S), Items.IRON_SWORD, 1);
		expect(helper, 3, 3, g(fe, fe, fe, fe, fe, fe, fe, fe, fe), Items.IRON_BLOCK, 1);
		expect(helper, 2, 1, g(fe, Items.FLINT), Items.FLINT_AND_STEEL, 1);
		helper.succeed();
	}

	/** "You could also use it as iron": the main iron recipes also work with steel. */
	@GameTest
	public void steelWorksLikeIron(GameTestHelper helper) {
		Item c = Items.COBBLESTONE, r = Items.REDSTONE, st = Items.STONE;
		expect(helper, 3, 2, g(I, O, I, O, I, O), Items.BUCKET, 1);
		expect(helper, 2, 2, g(O, I, I, O), Items.SHEARS, 1);
		expect(helper, 3, 2, g(I, I, I, I, I, I), Items.IRON_BARS, 16);
		expect(helper, 3, 3, g(I, O, I, I, S, I, I, O, I), Items.RAIL, 16);
		expect(helper, 3, 2, g(I, O, I, I, I, I), Items.MINECART, 1);
		expect(helper, 3, 3, g(I, O, I, I, O, I, I, I, I), Items.CAULDRON, 1);
		expect(helper, 3, 3, g(I, O, I, I, Items.CHEST, I, O, I, O), Items.HOPPER, 1);
		expect(helper, 3, 3, g(B, B, B, O, I, O, I, I, I), Items.ANVIL, 1);
		expect(helper, 1, 3, g(N, I, N), Items.IRON_CHAIN, 1);
		expect(helper, 3, 3, g(N, N, N, N, Items.TORCH, N, N, N, N), Items.LANTERN, 1);
		expect(helper, 2, 3, g(I, I, I, I, I, I), Items.IRON_DOOR, 3);
		expect(helper, 2, 1, g(I, Items.FLINT), Items.FLINT_AND_STEEL, 1);
		expect(helper, 3, 3, g(O, I, O, I, r, I, O, I, O), Items.COMPASS, 1);
		expect(helper, 3, 3, g(Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, c, I, c, c, r, c), Items.PISTON, 1);
		expect(helper, 3, 2, g(O, I, O, st, st, st), Items.STONECUTTER, 1);
		int fromSteel = 0;
		for (RecipeHolder<?> holder : helper.getLevel().recipeAccess().getRecipes()) {
			Identifier id = holder.id().identifier();
			if (id.getNamespace().equals("frenchmod") && id.getPath().endsWith("_from_steel")) {
				fromSteel++;
			}
		}
		helper.assertTrue(fromSteel == 28, "28 iron recipes should have a steel version, found " + fromSteel);
		helper.succeed();
	}

	private static double attack(Item item) {
		return item.components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).compute(Attributes.ATTACK_DAMAGE, 1.0, EquipmentSlot.MAINHAND);
	}

	private static double speed(Item item) {
		return item.components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).compute(Attributes.ATTACK_SPEED, 4.0, EquipmentSlot.MAINHAND);
	}

	private static int armor(GameTestHelper helper, Item... pieces) {
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		double total = 0;
		for (int i = 0; i < 4; i++) {
			total += pieces[i].components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).compute(Attributes.ARMOR, 0.0, slots[i]);
		}
		return (int) Math.round(total);
	}

	/** Steel lasts 500 uses (iron 250, diamond 1561), mines a bit faster than iron, hits exactly like iron, armor 17 (iron 15, diamond 20); repaired with steel ingots. */
	@GameTest
	public void steelStatsAreBetweenIronAndDiamond(GameTestHelper helper) {
		Item[][] tools = {{Items.IRON_SWORD, ModSteel.STEEL_SWORD, Items.DIAMOND_SWORD}, {Items.IRON_PICKAXE, ModSteel.STEEL_PICKAXE, Items.DIAMOND_PICKAXE},
				{Items.IRON_AXE, ModSteel.STEEL_AXE, Items.DIAMOND_AXE}, {Items.IRON_SHOVEL, ModSteel.STEEL_SHOVEL, Items.DIAMOND_SHOVEL},
				{Items.IRON_HOE, ModSteel.STEEL_HOE, Items.DIAMOND_HOE}};
		for (Item[] t : tools) {
			ItemStack steel = new ItemStack(t[1]);
			helper.assertTrue(steel.getMaxDamage() == 500, t[1] + " should have 500 uses, has " + steel.getMaxDamage());
			helper.assertTrue(attack(t[1]) == attack(t[0]), t[1] + " damage " + attack(t[1]) + " should equal iron's " + attack(t[0]));
			helper.assertTrue(speed(t[1]) == speed(t[0]), t[1] + " attack speed " + speed(t[1]) + " should equal iron's " + speed(t[0]));
			helper.assertTrue(steel.isValidRepairItem(new ItemStack(I)) && !steel.isValidRepairItem(new ItemStack(Items.IRON_INGOT)), t[1] + " repairs with steel");
		}
		// Her rule: steel tools hit exactly like iron (sword 6 at 1.6 speed, axe 9 at 0.9 speed); steel just lasts longer.
		helper.assertTrue(attack(ModSteel.STEEL_SWORD) == 6.0 && attack(Items.IRON_SWORD) == 6.0, "swords: iron " + attack(Items.IRON_SWORD) + ", steel " + attack(ModSteel.STEEL_SWORD));
		helper.assertTrue(Math.abs(speed(ModSteel.STEEL_SWORD) - 1.6) < 1e-6 && speed(ModSteel.STEEL_SWORD) == speed(Items.IRON_SWORD), "sword speed " + speed(ModSteel.STEEL_SWORD));
		helper.assertTrue(attack(Items.IRON_AXE) == 9.0 && attack(ModSteel.STEEL_AXE) == 9.0, "axes: iron " + attack(Items.IRON_AXE) + ", steel " + attack(ModSteel.STEEL_AXE));
		helper.assertTrue(Math.abs(speed(ModSteel.STEEL_AXE) - 0.9) < 1e-6 && speed(ModSteel.STEEL_AXE) == speed(Items.IRON_AXE), "axe speed " + speed(ModSteel.STEEL_AXE));
		ItemStack pick = new ItemStack(ModSteel.STEEL_PICKAXE);
		helper.assertTrue(pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 7.0F, "steel pickaxe speed 7 (iron 6, diamond 8)");
		helper.assertTrue(pick.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "steel mines diamond ore");
		helper.assertTrue(!pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "steel does not mine obsidian (that's diamond's job)");
		int iron = armor(helper, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
		int steel = armor(helper, ModSteel.STEEL_HELMET, ModSteel.STEEL_CHESTPLATE, ModSteel.STEEL_LEGGINGS, ModSteel.STEEL_BOOTS);
		int diamond = armor(helper, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
		helper.assertTrue(iron == 15 && steel == 17 && diamond == 20, "armor iron/steel/diamond " + iron + "/" + steel + "/" + diamond);
		Item[][] armor = {{Items.IRON_HELMET, ModSteel.STEEL_HELMET, Items.DIAMOND_HELMET}, {Items.IRON_CHESTPLATE, ModSteel.STEEL_CHESTPLATE, Items.DIAMOND_CHESTPLATE},
				{Items.IRON_LEGGINGS, ModSteel.STEEL_LEGGINGS, Items.DIAMOND_LEGGINGS}, {Items.IRON_BOOTS, ModSteel.STEEL_BOOTS, Items.DIAMOND_BOOTS}};
		for (Item[] a : armor) {
			int i = new ItemStack(a[0]).getMaxDamage(), s = new ItemStack(a[1]).getMaxDamage(), d = new ItemStack(a[2]).getMaxDamage();
			helper.assertTrue(i < s && s < d, a[1] + " durability " + s + " should be between " + i + " and " + d);
			helper.assertTrue(new ItemStack(a[1]).isValidRepairItem(new ItemStack(I)), a[1] + " repairs with steel");
			helper.assertTrue(new ItemStack(a[0]).isValidRepairItem(new ItemStack(I)), a[0] + " (iron) can be patched with steel too");
			helper.assertTrue(a[1].components().has(DataComponents.EQUIPPABLE), a[1] + " can be worn");
		}
		helper.succeed();
	}

	private static boolean in(Item item, String tag) {
		return new ItemStack(item).is(TagKey.create(Registries.ITEM, Identifier.parse(tag)));
	}

	/** Steel is in the common c: tags, the vanilla tool/armor tags (so it can be enchanted), and the Block of Steel acts like a Block of Iron. */
	@GameTest
	public void steelTagsAndBlock(GameTestHelper helper) {
		helper.assertTrue(in(I, "c:ingots/steel") && in(I, "c:ingots") && in(N, "c:nuggets/steel") && in(B, "c:storage_blocks/steel"), "c: tags");
		helper.assertTrue(in(I, "minecraft:beacon_payment_items"), "steel pays for a beacon");
		helper.assertTrue(!in(I, "minecraft:iron_tool_materials"), "steel is NOT an iron tool material (it would clash with steel tools)");
		helper.assertTrue(in(ModSteel.STEEL_SWORD, "minecraft:swords") && in(ModSteel.STEEL_PICKAXE, "minecraft:pickaxes") && in(ModSteel.STEEL_AXE, "minecraft:axes")
				&& in(ModSteel.STEEL_SHOVEL, "minecraft:shovels") && in(ModSteel.STEEL_HOE, "minecraft:hoes"), "tool tags");
		helper.assertTrue(in(ModSteel.STEEL_HELMET, "minecraft:head_armor") && in(ModSteel.STEEL_CHESTPLATE, "minecraft:chest_armor")
				&& in(ModSteel.STEEL_LEGGINGS, "minecraft:leg_armor") && in(ModSteel.STEEL_BOOTS, "minecraft:foot_armor"), "armor tags");
		helper.assertTrue(in(ModSteel.STEEL_SWORD, "minecraft:enchantable/sharp_weapon") && in(ModSteel.STEEL_HELMET, "minecraft:enchantable/head_armor"), "steel sword and armor can be enchanted");
		var block = ModSteel.STEEL_BLOCK.defaultBlockState();
		helper.assertTrue(block.is(BlockTags.BEACON_BASE_BLOCKS) && block.is(BlockTags.MINEABLE_WITH_PICKAXE) && block.is(BlockTags.NEEDS_STONE_TOOL), "block tags");
		helper.assertTrue(!new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(block) && new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(block),
				"Block of Steel needs a stone pickaxe or better, like iron");
		helper.assertTrue(ModSteel.ITEMS.size() == 19, "13 steel items + 6 furniture in the French Mod tab, got " + ModSteel.ITEMS.size());
		helper.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(ModSteel.TAB_KEY) == ModSteel.TAB, "French Mod tab registered");
		helper.succeed();
	}
}
