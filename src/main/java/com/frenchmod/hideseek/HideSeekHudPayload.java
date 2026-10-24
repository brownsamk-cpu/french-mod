package com.frenchmod.hideseek;

import com.frenchmod.FrenchMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: what the top-left hide-and-seek timer should say.
 * phase: 0 = no game (hide the timer), 1 = hiding (ticks = time LEFT), 2 = seeking (ticks = time so far).
 */
public record HideSeekHudPayload(int phase, int ticks, int found, int total) implements CustomPacketPayload {
	public static final Type<HideSeekHudPayload> TYPE = new Type<>(FrenchMod.id("hideseek_hud"));
	public static final StreamCodec<RegistryFriendlyByteBuf, HideSeekHudPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, HideSeekHudPayload::phase,
			ByteBufCodecs.VAR_INT, HideSeekHudPayload::ticks,
			ByteBufCodecs.VAR_INT, HideSeekHudPayload::found,
			ByteBufCodecs.VAR_INT, HideSeekHudPayload::total,
			HideSeekHudPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
