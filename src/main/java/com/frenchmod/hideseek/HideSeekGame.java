package com.frenchmod.hideseek;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * One game of hide-and-seek. Kid-safe: nobody in the game can be hurt while it runs (see ModHideSeek),
 * the seeker is only blinded and slowed while the others hide, and nothing ever kills anyone, so it is fine in hardcore worlds.
 */
public class HideSeekGame {
	public enum Phase { HIDE, SEEK, DONE }

	public static final int DEFAULT_HIDE_TICKS = 60 * 20; // 1 minute to hide
	public static final double TOUCH_DISTANCE = 1.5;

	public final UUID seeker;
	public final Set<UUID> hiders;
	public final Map<UUID, Integer> foundAt = new LinkedHashMap<>(); // hider -> seek ticks when found
	private final Map<UUID, String> foundNames = new LinkedHashMap<>();
	public final int hideTicks;
	private Phase phase = Phase.HIDE;
	private int hideLeft;
	private int seekTicks;

	public HideSeekGame(UUID seeker, Iterable<UUID> hiders, int hideTicks) {
		this.seeker = seeker;
		this.hiders = new LinkedHashSet<>();
		for (UUID h : hiders) if (!h.equals(seeker)) this.hiders.add(h);
		this.hideTicks = hideTicks;
		this.hideLeft = hideTicks;
	}

	public Phase phase() { return phase; }
	public int hideTicksLeft() { return hideLeft; }
	public int seekTicks() { return seekTicks; }
	public boolean isPlayer(UUID id) { return seeker.equals(id) || hiders.contains(id); }
	public boolean isFound(UUID id) { return foundAt.containsKey(id); }

	/** "2:15" */
	public static String clock(int ticks) {
		int s = Math.max(0, ticks) / 20;
		return (s / 60) + ":" + String.format("%02d", s % 60);
	}

	/** For the countdown: round up so it shows 1:00 ... 0:01, never 0:00 while still hiding. */
	public static String countdown(int ticksLeft) {
		return clock(ticksLeft + 19);
	}

	private List<ServerPlayer> online(MinecraftServer server) {
		List<ServerPlayer> out = new ArrayList<>();
		ServerPlayer s = server.getPlayerList().getPlayer(seeker);
		if (s != null) out.add(s);
		for (UUID h : hiders) {
			ServerPlayer p = server.getPlayerList().getPlayer(h);
			if (p != null) out.add(p);
		}
		return out;
	}

	public void begin(MinecraftServer server) {
		ServerPlayer s = server.getPlayerList().getPlayer(seeker);
		String name = s == null ? "The seeker" : s.getName().getString();
		title(server, Component.literal("Cache-toi!").withStyle(ChatFormatting.GOLD),
				Component.literal("Hide! " + name + " is counting to " + clock(hideTicks) + "!"));
		freezeSeeker(server);
		sync(server);
	}

	/** One game tick. Returns true while the game is still going. */
	public boolean tick(MinecraftServer server) {
		if (phase == Phase.HIDE) {
			hideLeft--;
			if (hideLeft % 20 == 0) freezeSeeker(server);
			if (hideLeft <= 0) startSeeking(server);
		} else if (phase == Phase.SEEK) {
			seekTicks++;
			ServerPlayer s = server.getPlayerList().getPlayer(seeker);
			if (s != null) {
				for (UUID h : List.copyOf(hiders)) {
					if (isFound(h)) continue;
					ServerPlayer p = server.getPlayerList().getPlayer(h);
					if (p != null && p.level() == s.level() && s.distanceTo(p) <= TOUCH_DISTANCE + 0.6 && s.hasLineOfSight(p)) {
						found(server, s, p);
					}
				}
			}
		}
		if (phase != Phase.DONE && (seekTicks % 5 == 0 || phase == Phase.HIDE)) sync(server);
		return phase != Phase.DONE;
	}

	private void freezeSeeker(MinecraftServer server) {
		ServerPlayer s = server.getPlayerList().getPlayer(seeker);
		if (s == null) return;
		// Short effects, refreshed every second, so they fade by themselves even if something goes wrong.
		s.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
		s.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 6, false, false, true));
	}

	private void unfreezeSeeker(MinecraftServer server) {
		ServerPlayer s = server.getPlayerList().getPlayer(seeker);
		if (s == null) return;
		s.removeEffect(MobEffects.BLINDNESS);
		s.removeEffect(MobEffects.SLOWNESS);
	}

	public void startSeeking(MinecraftServer server) {
		if (phase != Phase.HIDE) return;
		phase = Phase.SEEK;
		hideLeft = 0;
		unfreezeSeeker(server);
		title(server, Component.literal("Prêt? Cherche!").withStyle(ChatFormatting.AQUA),
				Component.literal(hiders.isEmpty() ? "Ready? Practice seeking!" : "Ready or not, here I come!"));
		sync(server);
	}

	/** The seeker found this hider (by tagging or touching). Returns true if it counted. */
	public boolean found(MinecraftServer server, ServerPlayer by, ServerPlayer hider) {
		if (phase != Phase.SEEK || !by.getUUID().equals(seeker) || !hiders.contains(hider.getUUID()) || isFound(hider.getUUID())) return false;
		foundAt.put(hider.getUUID(), seekTicks);
		foundNames.put(hider.getUUID(), hider.getName().getString());
		title(server, Component.literal("Found in " + clock(seekTicks) + "!").withStyle(ChatFormatting.GREEN),
				Component.literal("Trouvé! " + by.getName().getString() + " found " + hider.getName().getString() + "!"));
		hider.sendSystemMessage(Component.literal("Trouvé! You were found in " + clock(seekTicks) + ".").withStyle(ChatFormatting.GREEN));
		if (foundAt.size() == hiders.size()) finish(server);
		else sync(server);
		return true;
	}

	private void finish(MinecraftServer server) {
		phase = Phase.DONE;
		StringBuilder sb = new StringBuilder("Bravo! Everyone was found:");
		for (Map.Entry<UUID, Integer> e : foundAt.entrySet()) {
			sb.append("\n  ").append(foundNames.getOrDefault(e.getKey(), "?")).append(" - found in ").append(clock(e.getValue()));
		}
		for (ServerPlayer p : online(server)) p.sendSystemMessage(Component.literal(sb.toString()).withStyle(ChatFormatting.GOLD));
		sendHud(server, new HideSeekHudPayload(0, 0, 0, 0));
	}

	public void cancel(MinecraftServer server) {
		if (phase == Phase.DONE) return;
		phase = Phase.DONE;
		unfreezeSeeker(server);
		for (ServerPlayer p : online(server)) p.sendSystemMessage(Component.literal("Hide-and-seek stopped. À bientôt!").withStyle(ChatFormatting.YELLOW));
		sendHud(server, new HideSeekHudPayload(0, 0, 0, 0));
	}

	public HideSeekHudPayload hud() {
		return phase == Phase.HIDE ? new HideSeekHudPayload(1, hideLeft, 0, hiders.size())
				: phase == Phase.SEEK ? new HideSeekHudPayload(2, seekTicks, foundAt.size(), hiders.size())
				: new HideSeekHudPayload(0, 0, 0, 0);
	}

	private void sync(MinecraftServer server) {
		sendHud(server, hud());
	}

	private void sendHud(MinecraftServer server, HideSeekHudPayload payload) {
		for (ServerPlayer p : online(server)) {
			if (ServerPlayNetworking.canSend(p, HideSeekHudPayload.TYPE)) ServerPlayNetworking.send(p, payload);
		}
	}

	private void title(MinecraftServer server, Component title, Component subtitle) {
		for (ServerPlayer p : online(server)) {
			p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
			p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
			p.connection.send(new ClientboundSetTitleTextPacket(title));
		}
	}
}
