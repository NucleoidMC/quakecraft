/*
 * Copyright (c) 2020-2022 LambdAurora <email@lambdaurora.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.quakecraft.game;

import com.google.common.collect.Multimap;
import dev.lambdaurora.quakecraft.Quakecraft;
import dev.lambdaurora.quakecraft.game.map.MapBuilder;
import dev.lambdaurora.quakecraft.game.map.QuakecraftMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.TeamAllocator;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

/**
 * Represents a Quakecraft wait-room.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.0.0
 */
public class QuakecraftWaiting {
	private final GameActivity game;
	private final ServerWorld world;
	private final QuakecraftMap map;
	private final QuakecraftConfig config;
	private final QuakecraftSpawnLogic spawnLogic;

	private QuakecraftWaiting(GameActivity game, ServerWorld world, QuakecraftMap map, QuakecraftConfig config) {
		this.game = game;
		this.world = world;
		this.map = map;
		this.config = config;
		this.spawnLogic = new QuakecraftSpawnLogic(game.getGameSpace(), world, map);
	}

	public static GameOpenProcedure open(GameOpenContext<QuakecraftConfig> context) {
		var config = context.config();

		var map = new MapBuilder(config.map()).create(context.server());
		var worldConfig = new RuntimeWorldConfig()
				.setGenerator(map.asGenerator(context.server()))
				.setTimeOfDay(config.map().time());

		return context.openWithWorld(worldConfig, (game, world) -> {
			map.init(world);

			QuakecraftWaiting waiting = new QuakecraftWaiting(game, world, map, config);

			GameWaitingLobby.addTo(game, config.players());

			game.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);

			game.listen(GamePlayerEvents.ACCEPT, waiting::addPlayer);
			game.listen(GamePlayerEvents.REMOVE, waiting::removePlayer);

			game.listen(PlayerDeathEvent.EVENT, waiting::onPlayerDeath);
			game.listen(ItemUseEvent.EVENT, waiting::onUseItem);

			game.deny(GameRuleType.USE_BLOCKS);
			game.deny(GameRuleType.PVP);
		});
	}

	private GameResult requestStart() {
		var players = this.allocatePlayers();
		QuakecraftGame.open(this.config, this.game.getGameSpace(), this.world, this.map, this.spawnLogic, players);
		return GameResult.ok();
	}

	private JoinAcceptorResult addPlayer(JoinAcceptor offer) {
		return offer.teleport(this.world, this.map.waitingSpawn.center()).thenRunForEach(player -> {
				this.spawnLogic.spawnWaitingPlayer(player);
				this.spawnLogic.resetWaitingPlayer(player);
		});
	}

	private void removePlayer(ServerPlayerEntity player) {
		Quakecraft.removeSpeed(player);
	}

	private EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		this.spawnLogic.resetWaitingPlayer(player);
		this.spawnLogic.spawnWaitingPlayer(player);
		return EventResult.DENY;
	}

	private ActionResult onUseItem(ServerPlayerEntity player, Hand hand) {
		var heldStack = player.getStackInHand(hand);

		if (heldStack.isIn(ItemTags.BEDS)) {
			this.game.getGameSpace().getPlayers().kick(player);
			return ActionResult.SUCCESS_SERVER;
		}

		return ActionResult.PASS;
	}

	private @Nullable Multimap<GameTeam, ServerPlayerEntity> allocatePlayers() {
		if (this.config.teams().size() == 0) {
			return null;
		}
		var allocator = new TeamAllocator<GameTeam, ServerPlayerEntity>(this.config.teams());
		this.game.getGameSpace().getPlayers().forEach(player -> allocator.add(player, null));
		return allocator.allocate();
	}
}
