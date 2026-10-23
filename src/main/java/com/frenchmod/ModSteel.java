package com.frenchmod;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Step 1, Steel: "a silver thing in blocks, like iron, the same pixels of iron, just a little bit darker."
 * Iron ingot + coal (or charcoal) = Steel Blend; smelt or blast the blend = Steel Ingot.
 * Steel lasts longer than iron (between iron and diamond), and works like iron in the main iron recipes (see data/frenchmod/recipe/*_from_steel.json).
 */
public final class ModSteel {
	public static final TagKey<Item> STEEL_TOOL_MATERIALS = TagKey.create(Registries.ITEM, FrenchMod.id("steel_tool_materials"));
	public static final TagKey<Item> REPAIRS_STEEL_ARMOR = TagKey.create(Registries.ITEM, FrenchMod.id("repairs_steel_armor"));
	public static final ResourceKey<EquipmentAsset> STEEL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, FrenchMod.id("steel"));

	/**
	 * Iron is 250 uses / mining speed 6, diamond 1561 / 8. Steel: 500 uses, mining speed 7, mines what iron mines (not obsidian).
	 * Damage (her rule): the same +2 bonus as iron, so every steel tool hits exactly like its iron version.
	 */
	public static final ToolMaterial STEEL_TOOLS = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 500, 7.0F, 2.0F, 12, STEEL_TOOL_MATERIALS);

	/** Armor points: iron 2/6/5/2 = 15, steel 2/7/6/2 = 17, diamond 3/8/6/3 = 20. Durability x24 (iron x15, diamond x33), toughness 1 (diamond 2). */
	public static final ArmorMaterial STEEL_ARMOR = new ArmorMaterial(24, defense(2, 6, 7, 2, 7), 12, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F,
			REPAIRS_STEEL_ARMOR, STEEL_ASSET);

	public static final List<Item> ITEMS = new ArrayList<>();

	public static final Item STEEL_BLEND = item("steel_blend", new Item.Properties());
	public static final Item STEEL_INGOT = item("steel_ingot", new Item.Properties());
	public static final Item STEEL_NUGGET = item("steel_nugget", new Item.Properties());
	public static final Block STEEL_BLOCK = block("steel_block", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
	public static final Item STEEL_BLOCK_ITEM = blockItem(STEEL_BLOCK);

	// Same damage numbers and attack speeds as the iron versions (Items.IRON_SWORD etc.).
	public static final Item STEEL_SWORD = item("steel_sword", new Item.Properties().sword(STEEL_TOOLS, 3.0F, -2.4F));   // 6 damage, 1.6 speed (= iron)
	public static final Item STEEL_SHOVEL = item("steel_shovel", new Item.Properties().shovel(STEEL_TOOLS, 1.5F, -3.0F)); // 4.5 (= iron)
	public static final Item STEEL_PICKAXE = item("steel_pickaxe", new Item.Properties().pickaxe(STEEL_TOOLS, 1.0F, -2.8F)); // 4 (= iron)
	public static final Item STEEL_AXE = item("steel_axe", new Item.Properties().axe(STEEL_TOOLS, 6.0F, -3.1F));          // 9 damage, 0.9 speed (= iron)
	public static final Item STEEL_HOE = item("steel_hoe", new Item.Properties().hoe(STEEL_TOOLS, -2.0F, -1.0F));      // 1 (= iron)

	public static final Item STEEL_HELMET = item("steel_helmet", new Item.Properties().humanoidArmor(STEEL_ARMOR, ArmorType.HELMET));
	public static final Item STEEL_CHESTPLATE = item("steel_chestplate", new Item.Properties().humanoidArmor(STEEL_ARMOR, ArmorType.CHESTPLATE));
	public static final Item STEEL_LEGGINGS = item("steel_leggings", new Item.Properties().humanoidArmor(STEEL_ARMOR, ArmorType.LEGGINGS));
	public static final Item STEEL_BOOTS = item("steel_boots", new Item.Properties().humanoidArmor(STEEL_ARMOR, ArmorType.BOOTS));

	public static final List<Item> TOOLS = List.of(STEEL_SWORD, STEEL_SHOVEL, STEEL_PICKAXE, STEEL_AXE, STEEL_HOE);
	public static final List<Item> ARMOR = List.of(STEEL_HELMET, STEEL_CHESTPLATE, STEEL_LEGGINGS, STEEL_BOOTS);

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, FrenchMod.id("french_mod"));
	public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.frenchmod.french_mod"))
			.icon(() -> new ItemStack(STEEL_INGOT))
			.displayItems((params, output) -> ITEMS.forEach(output::accept))
			.build());

	private ModSteel() {
	}

	private static Map<ArmorType, Integer> defense(int boots, int leggings, int chestplate, int helmet, int body) {
		Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
		map.put(ArmorType.BOOTS, boots);
		map.put(ArmorType.LEGGINGS, leggings);
		map.put(ArmorType.CHESTPLATE, chestplate);
		map.put(ArmorType.HELMET, helmet);
		map.put(ArmorType.BODY, body);
		return map;
	}

	private static Item item(String name, Item.Properties properties) {
		return register(name, Item::new, properties);
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, FrenchMod.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ITEMS.add(item);
		return item;
	}

	private static Block block(String name, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, FrenchMod.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, new Block(properties.setId(key)));
	}

	private static Item blockItem(Block block) {
		Item item = register(BuiltInRegistries.BLOCK.getKey(block).getPath(), props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix());
		Item.BY_BLOCK.put(block, item);
		return item;
	}

	public static void init() {
		// Everything above registers when this class loads.
	}
}
