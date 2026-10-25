package com.frenchmod.tux;

import com.frenchmod.FrenchMod;
import com.frenchmod.ModSteel;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/** Tux the cat (0.3.2) and the Tux Spawn Egg. No natural spawns: Tux comes from the spawn egg. Pictures come from tools/tux.py. */
public final class ModTux {
	public static final ResourceKey<EntityType<?>> TUX_KEY = ResourceKey.create(Registries.ENTITY_TYPE, FrenchMod.id("tux"));
	public static final EntityType<Tux> TUX = Registry.register(BuiltInRegistries.ENTITY_TYPE, TUX_KEY,
			EntityType.Builder.<Tux>of(Tux::new, MobCategory.CREATURE).sized(0.6F, 0.7F).eyeHeight(0.35F).clientTrackingRange(8).build(TUX_KEY));

	public static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, FrenchMod.id("tux_spawn_egg"));
	public static final Item TUX_SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
			new SpawnEggItem(new Item.Properties().spawnEgg(TUX).setId(EGG_KEY)));

	private ModTux() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(TUX, Tux.createAttributes());
		ModSteel.ITEMS.add(TUX_SPAWN_EGG); // French Mod creative tab
	}
}
