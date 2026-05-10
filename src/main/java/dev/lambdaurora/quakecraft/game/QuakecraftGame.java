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
import dev.lambdaurora.quakecraft.PlayerAction;
import dev.lambdaurora.quakecraft.Quakecraft;
import dev.lambdaurora.quakecraft.entity.GrenadeEntity;
import dev.lambdaurora.quakecraft.entity.RocketEntity;
import dev.lambdaurora.quakecraft.game.map.QuakecraftMap;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.util.PlayerUtil;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.player.PlayerSwingHandEvent;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Represents the Quakecraft running game.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.0.0
 */
public class QuakecraftGame extends QuakecraftLogic {
	private final QuakecraftSpawnLogic spawnLogic;
	private final QuakecraftScoreboard scoreboard;
	private boolean running = false;
	private boolean end = false;
	private int time;
	private int endTime = 10 * 20;

	private Set<QuakecraftPlayer> winners = new HashSet<>();

	private QuakecraftGame(QuakecraftConfig config, GameActivity game, ServerLevel world, QuakecraftMap map, QuakecraftSpawnLogic spawnLogic) {
		super(game.getGameSpace(), world, config, map);
		this.spawnLogic = spawnLogic;
		GlobalWidgets widgets = GlobalWidgets.addTo(game);
		this.scoreboard = new QuakecraftScoreboard(this, widgets);

		this.time = this.getConfig().time();
	}

	/**
	 * Opens the game.
	 *
	 * @param config the game configuration
	 * @param gameSpace the game logic
	 * @param map the game map
	 * @param spawnLogic the game spawn logic
	 * @param players the players affected to teams
	 */
	public static void open(QuakecraftConfig config, GameSpace gameSpace, ServerLevel world, QuakecraftMap map,
	                        QuakecraftSpawnLogic spawnLogic, @Nullable Multimap<GameTeam, ServerPlayer> players) {
		gameSpace.setActivity(game -> {
			QuakecraftGame active = new QuakecraftGame(config, game, world, map, spawnLogic);
			if (players != null)
				active.assignTeams(players);
			map.postInit(active);

			game.deny(GameRuleType.CRAFTING);
			game.deny(GameRuleType.PORTALS);
			game.deny(GameRuleType.PVP);
			game.deny(GameRuleType.HUNGER);
			game.deny(GameRuleType.FALL_DAMAGE);
			game.deny(GameRuleType.BLOCK_DROPS);
			game.deny(GameRuleType.THROW_ITEMS);
			game.deny(GameRuleType.UNSTABLE_TNT);
			game.allow(GameRuleType.INTERACTION);

			game.listen(GameActivityEvents.ENABLE, active::onOpen);
			game.listen(GameActivityEvents.DISABLE, active::onClose);

			game.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
			game.listen(GamePlayerEvents.ACCEPT, offer -> offer.teleport(active.world(), active.map().waitingSpawn.center()));
			game.listen(GamePlayerEvents.ADD, active::addPlayer);
			game.listen(GamePlayerEvents.REMOVE, active::removePlayer);

			game.listen(GameActivityEvents.TICK, active::tick);

			game.listen(PlayerDamageEvent.EVENT, active::onDamage);
			game.listen(PlayerDeathEvent.EVENT, active::onPlayerDeath);

			game.listen(BlockUseEvent.EVENT, active::onUseBlock);
			game.listen(ItemUseEvent.EVENT, active::onUseItem);
			game.listen(PlayerSwingHandEvent.EVENT, active::onSwingHand);
			game.listen(PlayerAttackEntityEvent.EVENT, active::onAttackEntity);
		});
	}

	@Override
	protected void onOpen() {
		super.onOpen();
		for (ServerPlayer player : this.getSpace().getPlayers()) {
			this.spawnParticipant(player);
			Quakecraft.get().addActivePlayer(player);
		}
		this.running = true;
		this.scoreboard.update();
	}

	@Override
	protected void onClose() {
		super.onClose();
	}

	@Override
	public void tick() {
		super.tick();
		if (this.running) {
			int[] activePlayer = new int[]{0};
			this.participants.forEach((uuid, participant) -> {
				if (participant.hasLeft())
					return;

				participant.tick(this.getSpace());
				activePlayer[0]++;

				if (participant.hasWon()) {
					this.onWin(participant);
				}
			});
			this.time--;

			if (activePlayer[0] <= 1) {
				this.getSpace().getPlayers().sendMessage(Component.translatable("quakecraft.game.end.not_enough_players").withStyle(ChatFormatting.RED));
				this.getSpace().close(GameCloseReason.CANCELED);
			}

			if (this.time <= 0) {
				this.getSpace().getPlayers().sendMessage(Component.translatable("quakecraft.game.end.nobody_won").withStyle(ChatFormatting.RED));
				this.getSpace().close(GameCloseReason.FINISHED);
			}

			if (this.end) {
				this.participants.forEach((uuid, participant) -> participant.onEnd());
			}
		} else if (this.end) {
			this.endTime--;

			if (this.endTime % 20 == 0) {
				this.winners.forEach(player -> {
					if (!player.hasLeft()) {
						ServerPlayer mcPlayer = player.getPlayer();
						if (mcPlayer == null)
							return;

						Quakecraft.spawnFirework(this.world(), mcPlayer.getX(), mcPlayer.getY(), mcPlayer.getZ(),
								new int[]{15435844, 11743532}, false, -1);
					}
				});
			}

			if (this.endTime == 0)
				this.getSpace().close(GameCloseReason.FINISHED);
		}

		this.scoreboard.update();
	}

	private void assignTeams(Multimap<GameTeam, ServerPlayer> players) {
		players.forEach((team, player) -> this.getOptParticipant(player).ifPresent(p -> p.setTeam(team)));
	}

	private void onWin(QuakecraftPlayer winner) {
		this.getSpace().getPlayers().sendMessage(Component.translatable("quakecraft.game.end.win", winner.getDisplayName()).withStyle(ChatFormatting.GREEN));
		this.end = true;
		this.running = false;
		this.winners.add(winner);
	}

	private void addPlayer(ServerPlayer player) {
		this.spawnParticipant(player);
	}

	private void removePlayer(ServerPlayer player) {
		QuakecraftPlayer participant = this.participants.get(player.getUUID());
		if (participant != null) {
			participant.leave();
		}
		Quakecraft.get().removeActivePlayer(player);
	}

	private EventResult onDamage(ServerPlayer player, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.IS_EXPLOSION)) {
			Entity attacker = null;
			if (source.getDirectEntity() instanceof GrenadeEntity grenade) {
				attacker = grenade.getOwner();
			} else if (source.getDirectEntity() instanceof RocketEntity rocket) {
				attacker = rocket.getOwner();
			} else if (source.getDirectEntity() instanceof FireworkRocketEntity fireworkRocket) {
				return EventResult.DENY;
			} else if (source.getDirectEntity() instanceof ServerPlayer) {
				attacker = source.getDirectEntity();
			}

			if (attacker != null) {
				if (attacker instanceof ServerPlayer playerAttacker && attacker != player) {
					player.setLastHurtByMob(playerAttacker);
					playerAttacker.setLastHurtByPlayer(player, 200);
					player.kill(player.level());
				}
				return EventResult.DENY;
			}
		}
		return EventResult.PASS;
	}

	private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
		LivingEntity attacker = player.getLastHurtByMob();
		if (attacker != null) {
			QuakecraftPlayer other = this.participants.get(attacker.getUUID());
			if (other != null) {
				PlayerUtil.playSoundToPlayer((ServerPlayer) attacker, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 2.f, 5.f);
				other.incrementKills();
				this.getSpace().getPlayers().sendMessage(
						Component.translatable("quakecraft.game.kill", attacker.getDisplayName(), player.getDisplayName()).withStyle(ChatFormatting.GRAY)
				);

				this.getOptParticipant(player).ifPresent(QuakecraftPlayer::onDeath);
			}

			player.setLastHurtByMob(null);
			attacker.setLastHurtByPlayer(player, 1);
		}

		this.spawnParticipant(player);

		return EventResult.DENY;
	}

	private void onSwingHand(ServerPlayer player, InteractionHand hand) {
		if (Thread.currentThread() != player.level().getServer().getRunningThread())
			return;

		if (hand == InteractionHand.OFF_HAND) {
			// Attack cannot be in OFF_HAND
			return;
		}
		QuakecraftPlayer participant = this.getParticipant(player);
		if (participant == null)
			return;
		participant.onSwingHand(this.world());
	}

	private InteractionResult onUseBlock(ServerPlayer player, InteractionHand hand, BlockHitResult hitResult) {
		QuakecraftPlayer participant = this.getParticipant(player);
		if (participant != null) {
			if (participant.getLastAction() == PlayerAction.USE_BLOCK_AND_ITEM)
				participant.setLastAction(PlayerAction.NONE);
			else
				participant.setLastAction(PlayerAction.USE_BLOCK);
		}
		return InteractionResult.FAIL;
	}

	private InteractionResult onUseItem(ServerPlayer player, InteractionHand hand) {
		if (hand == InteractionHand.OFF_HAND) {
			return InteractionResult.FAIL;
		}

		ItemStack heldStack = player.getItemInHand(hand);

		QuakecraftPlayer participant = this.getParticipant(player);
		if (participant != null) {
			ItemCooldowns cooldown = player.getCooldowns();
			if (!cooldown.isOnCooldown(heldStack)) {
				int result = participant.onItemUse(this.world(), player, hand);
				if (result != -1) {
					this.getSpace().getPlayers().forEach(other -> {
						if (player.distanceToSqr(other) <= 16.f) {
							other.connection.send(new ClientboundSoundPacket(SoundEvents.HORSE_SADDLE, SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 2.f, 1.f, 0));
						}
					});
					cooldown.addCooldown(heldStack, result);

					return InteractionResult.SUCCESS_SERVER;
				}
			} else {
				// No swing
				if (participant.getLastAction() == PlayerAction.USE_BLOCK)
					participant.setLastAction(PlayerAction.USE_BLOCK_AND_ITEM);
			}
		}
		return InteractionResult.PASS;
	}

	private EventResult onAttackEntity(ServerPlayer player, InteractionHand hand, Entity entity, EntityHitResult entityHitResult) {
		if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR)
			return EventResult.PASS;
		return EventResult.PASS;
	}

	private void spawnParticipant(ServerPlayer player) {
		QuakecraftPlayer participant = this.getParticipant(player);
		if (participant != null) {
			participant.reset(player);
			this.spawnLogic.spawnPlayer(player);
		} else if (this.running) {
			player.setGameMode(GameType.SPECTATOR);
			player.getInventory().clearContent();
			this.spawnLogic.spawnWaitingPlayer(player);
		}
	}

	public int getTime() {
		return this.time;
	}
}
