package com.frenchmod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

/** French Mod: a cubey French city, one step at a time. Step 1 is steel, step 2 is hide-and-seek. */
public class FrenchMod implements ModInitializer {
	public static final String MOD_ID = "frenchmod";

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModSteel.init();
		com.frenchmod.hideseek.ModHideSeek.init();
	}
}
