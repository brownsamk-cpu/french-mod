package com.frenchmod.hideseek;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

/**
 * French Mod step 2: hide-and-seek. /hideseek start <seeker>  and  /hideseek stop.
 * Everyone else online hides. While a game runs nobody playing can be hurt by anything.
 */
public final class ModHideSeek {
	public static HideSeekGame current;
	private static MinecraftServer currentServer;

	private ModHideSeek() {}

	public static HideSeekGame start(MinecraftServer server, ServerPlayer seeker, int hideTicks) {
		if (current != null) current.cancel(server);
		List<UUID> hiders = new ArrayList<>();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) hiders.add(p.getUUID());
		return run(server, new HideSeekGame(seeker.getUUID(), hiders, hideTicks));
	}

	/** Make this the running game (ticked every server tick) and announce it. */
	public static HideSeekGame run(MinecraftServer server, HideSeekGame game) {
		if (current != null && current != game) current.cancel(server);
		current = game;
		currentServer = server;
		game.begin(server);
		return game;
	}

	public static void stop(MinecraftServer server) {
		if (current != null) current.cancel(server);
		current = null;
	}

	/** True when this entity is playing in a running game, so it must not take any damage. */
	public static boolean protectedPlayer(UUID id) {
		return current != null && current.phase() != HideSeekGame.Phase.DONE && current.isPlayer(id);
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(HideSeekHudPayload.TYPE, HideSeekHudPayload.CODEC);

		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> dispatcher.register(Commands.literal("hideseek")
				.then(Commands.literal("start").then(Commands.argument("seeker", EntityArgument.player()).executes(ctx -> {
					ServerPlayer seeker = EntityArgument.getPlayer(ctx, "seeker");
					HideSeekGame g = start(ctx.getSource().getServer(), seeker, HideSeekGame.DEFAULT_HIDE_TICKS);
					ctx.getSource().sendSuccess(() -> Component.literal("Hide-and-seek! " + seeker.getName().getString() + " seeks, "
							+ (g.hiders.isEmpty() ? "nobody else is here, so it's practice." : g.hiders.size() + " hiding.")), true);
					return 1;
				})))
				.then(Commands.literal("stop").executes(ctx -> {
					stop(ctx.getSource().getServer());
					return 1;
				}))));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (current != null && currentServer == server && !current.tick(server)) current = null;
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> current = null);

		// Tagging a hider = found, and it never hurts. Hiders hitting the seeker doesn't hurt either.
		AttackEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
			if (current == null || level.isClientSide() || !(player instanceof ServerPlayer sp) || !(target instanceof ServerPlayer tp)) return InteractionResult.PASS;
			if (!current.isPlayer(sp.getUUID()) || !current.isPlayer(tp.getUUID())) return InteractionResult.PASS;
			current.found(sp.level().getServer(), sp, tp);
			return InteractionResult.FAIL;
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !protectedPlayer(entity.getUUID()));
	}
}
