package com.frenchmod.furniture;

import java.util.List;
import java.util.function.Function;

import com.frenchmod.FrenchMod;
import com.frenchmod.ModSteel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Step 3, French-cottage furniture: Chair, Table, Fancy Bed, Couch and Lamp.
 * Cream painted wood, soft blue fabric and gold trim. Pictures and models come from tools/furniture.py.
 */
public final class ModFurniture {
	private static BlockBehaviour.Properties wood() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).instrument(NoteBlockInstrument.BASS).strength(1.5F).sound(SoundType.WOOD)
				.noOcclusion().ignitedByLava();
	}

	public static final Block CHAIR = block("chair", p -> new SeatBlock(Shapes.or(Block.box(2, 0, 2, 14, 10, 14), Block.box(2, 10, 12, 14, 16, 14)),
			0.6875, p), wood());
	public static final Block TABLE = block("table", p -> new FurnitureBlock(Shapes.or(Block.box(0, 13, 0, 16, 16, 16), Block.box(1.5, 0, 1.5, 14.5, 13, 14.5)), p), wood());
	/** The Golden Trim Table (0.3.1): table-shaped, white with sugar pink, a golden trim. */
	public static final Block GOLDEN_TRIM_TABLE = block("golden_trim_table", p -> new FurnitureBlock(Shapes.or(Block.box(0, 13, 0, 16, 16, 16),
			Block.box(1.5, 0, 1.5, 14.5, 13, 14.5)), p), wood().mapColor(MapColor.COLOR_PINK));
	public static final Block FANCY_BED = block("fancy_bed", FancyBedBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).sound(SoundType.WOOD)
			.strength(0.2F).bounceRestitution(0.75F).fallDistanceReduction(0.5F).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
	public static final Block COUCH = block("couch", p -> new SeatBlock(Shapes.or(Block.box(0, 0, 1, 16, 10, 15), Block.box(0, 10, 12, 16, 16, 15)),
			0.625, p), wood());
	public static final Block LAMP = block("lamp", LampBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.5F).sound(SoundType.LANTERN)
			.noOcclusion().lightLevel(state -> state.getValue(LampBlock.LIT) ? 15 : 0));

	public static final Item CHAIR_ITEM = item(CHAIR, new Item.Properties());
	public static final Item TABLE_ITEM = item(TABLE, new Item.Properties());
	public static final Item FANCY_BED_ITEM = item(FANCY_BED, new Item.Properties().stacksTo(1));
	public static final Item COUCH_ITEM = item(COUCH, new Item.Properties());
	public static final Item LAMP_ITEM = item(LAMP, new Item.Properties());
	public static final Item GOLDEN_TRIM_TABLE_ITEM = item(GOLDEN_TRIM_TABLE, new Item.Properties());
	public static final List<Block> BLOCKS = List.of(CHAIR, TABLE, FANCY_BED, COUCH, LAMP, GOLDEN_TRIM_TABLE);

	public static final ResourceKey<EntityType<?>> SEAT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, FrenchMod.id("seat"));
	public static final EntityType<Seat> SEAT = Registry.register(BuiltInRegistries.ENTITY_TYPE, SEAT_KEY,
			EntityType.Builder.<Seat>of(Seat::new, MobCategory.MISC).sized(0.25F, 0.05F).noSummon().clientTrackingRange(10).updateInterval(20)
					.build(SEAT_KEY));

	private ModFurniture() {
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, FrenchMod.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	private static Item item(Block block, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, properties.useBlockDescriptionPrefix().setId(key)));
		Item.BY_BLOCK.put(block, item);
		ModSteel.ITEMS.add(item); // shows up in the French Mod creative tab
		return item;
	}

	public static void init() {
		// Everything above registers when this class loads.
	}
}
