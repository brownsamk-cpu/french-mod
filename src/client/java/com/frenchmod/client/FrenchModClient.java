package com.frenchmod.client;

import com.frenchmod.FrenchMod;
import com.frenchmod.hideseek.HideSeekGame;
import com.frenchmod.hideseek.HideSeekHudPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;

/** Steel needs nothing client-only (plain resource files). Hide-and-seek draws its timer in the top-left corner. */
public class FrenchModClient implements ClientModInitializer {
	private static volatile HideSeekHudPayload hud = new HideSeekHudPayload(0, 0, 0, 0);

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(HideSeekHudPayload.TYPE, (payload, context) -> hud = payload);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> hud = new HideSeekHudPayload(0, 0, 0, 0));

		HudElementRegistry.addLast(FrenchMod.id("hideseek_timer"), (g, delta) -> {
			HideSeekHudPayload h = hud;
			if (h.phase() == 0) return;
			var font = Minecraft.getInstance().font;
			String line1, line2;
			int color;
			if (h.phase() == 1) {
				line1 = "Cache-toi! Hide!";
				line2 = HideSeekGame.countdown(h.ticks());
				color = 0xFFFFD24A;
			} else {
				line1 = "Cherche! Seeking:";
				line2 = HideSeekGame.clock(h.ticks()) + (h.total() > 0 ? "   Trouvé: " + h.found() + "/" + h.total() : "");
				color = 0xFF7FE0FF;
			}
			int w = Math.max(font.width(line1), font.width(line2) * 2) + 8;
			g.fill(2, 2, 2 + w, 2 + 32, 0x90000000);
			g.text(font, line1, 6, 6, 0xFFFFFFFF, true);
			g.pose().pushMatrix();
			g.pose().translate(6, 16);
			g.pose().scale(2f, 2f);
			g.text(font, line2, 0, 0, color, true);
			g.pose().popMatrix();
		});
	}
}
