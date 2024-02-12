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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SoundPlayS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.player.PlayerSwingHandEvent;

import java.util.HashSet;
import java.util.Set;

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

	private QuakecraftGame(QuakecraftConfig config, GameActivity game, ServerWorld world, QuakecraftMap map, QuakecraftSpawnLogic spawnLogic) {
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
	public static void open(QuakecraftConfig config, GameSpace gameSpace, ServerWorld world, QuakecraftMap map,
	                        QuakecraftSpawnLogic spawnLogic, @Nullable Multimap<GameTeam, ServerPlayerEntity> players) {
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

			game.listen(GamePlayerEvents.OFFER, offer -> offer.accept(active.world(), active.map().waitingSpawn.center()));
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
		for (ServerPlayerEntity player : this.getSpace().getPlayers()) {
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
				this.getSpace().getPlayers().sendMessage(Text.translatable("quakecraft.game.end.not_enough_players").formatted(Formatting.RED));
				this.getSpace().close(GameCloseReason.CANCELED);
			}

			if (this.time <= 0) {
				this.getSpace().getPlayers().sendMessage(Text.translatable("quakecraft.game.end.nobody_won").formatted(Formatting.RED));
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
						ServerPlayerEntity mcPlayer = player.getPlayer();
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

	private void assignTeams(Multimap<GameTeam, ServerPlayerEntity> players) {
		players.forEach((team, player) -> this.getOptParticipant(player).ifPresent(p -> p.setTeam(team)));
	}

	private void onWin(QuakecraftPlayer winner) {
		this.getSpace().getPlayers().sendMessage(Text.translatable("quakecraft.game.end.win", winner.getDisplayName()).formatted(Formatting.GREEN));
		this.end = true;
		this.running = false;
		this.winners.add(winner);
	}

	private void addPlayer(ServerPlayerEntity player) {
		this.spawnParticipant(player);
	}

	private void removePlayer(ServerPlayerEntity player) {
		QuakecraftPlayer participant = this.participants.get(player.getUuid());
		if (participant != null) {
			participant.leave();
		}
		Quakecraft.get().removeActivePlayer(player);
	}

	private ActionResult onDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		if (source.isTypeIn(DamageTypeTags.IS_EXPLOSION)) {
			Entity attacker = null;
			if (source.getSource() instanceof GrenadeEntity grenade) {
				attacker = grenade.getOwner();
			} else if (source.getSource() instanceof RocketEntity rocket) {
				attacker = rocket.getOwner();
			} else if (source.getSource() instanceof FireworkRocketEntity fireworkRocket) {
				return ActionResult.FAIL;
			} else if (source.getSource() instanceof ServerPlayerEntity) {
				attacker = source.getSource();
			}

			if (attacker != null) {
				if (attacker instanceof ServerPlayerEntity playerAttacker && attacker != player) {
					player.setAttacker(playerAttacker);
					playerAttacker.setAttacking(player);
					player.kill();
				}
				return ActionResult.FAIL;
			}
		}
		return ActionResult.PASS;
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		LivingEntity attacker = player.getAttacker();
		if (attacker != null) {
			QuakecraftPlayer other = this.participants.get(attacker.getUuid());
			if (other != null) {
				((ServerPlayerEntity) attacker).playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.MASTER, 2.f, 5.f);
				other.incrementKills();
				this.getSpace().getPlayers().sendMessage(
						Text.translatable("quakecraft.game.kill", attacker.getDisplayName(), player.getDisplayName()).formatted(Formatting.GRAY)
				);

				this.getOptParticipant(player).ifPresent(QuakecraftPlayer::onDeath);
			}

			player.setAttacker(null);
			attacker.setAttacking(null);
		}

		this.spawnParticipant(player);

		return ActionResult.FAIL;
	}

	private void onSwingHand(ServerPlayerEntity player, Hand hand) {
		if (Thread.currentThread() != player.getServer().getThread())
			return;

		if (hand == Hand.OFF_HAND) {
			// Attack cannot be in OFF_HAND
			return;
		}
		QuakecraftPlayer participant = this.getParticipant(player);
		if (participant == null)
			return;
		participant.onSwingHand(this.world());
	}

	private ActionResult onUseBlock(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
		QuakecraftPlayer participant = this.getParticipant(player);
		if (participant != null) {
			if (participant.getLastAction() == PlayerAction.USE_BLOCK_AND_ITEM)
				participant.setLastAction(PlayerAction.NONE);
			else
				participant.setLastAction(PlayerAction.USE_BLOCK);
		}
		return ActionResult.FAIL;
	}

	private TypedActionResult<ItemStack> onUseItem(ServerPlayerEntity player, Hand hand) {
		if (hand == Hand.OFF_HAND) {
			return TypedActionResult.fail(ItemStack.EMPTY);
		}

		ItemStack heldStack = player.getStackInHand(hand);

		QuakecraftPlayer participant = this.getParticipant(player);
		if (participant != null) {
			ItemCooldownManager cooldown = player.getItemCooldownManager();
			if (!cooldown.isCoolingDown(heldStack.getItem())) {
				int result = participant.onItemUse(this.world(), player, hand);
				if (result != -1) {
					this.getSpace().getPlayers().forEach(other -> {
						if (player.squaredDistanceTo(other) <= 16.f) {
							other.networkHandler.send(new SoundPlayS2CPacket(Registries.SOUND_EVENT.wrapAsHolder(SoundEvents.ENTITY_HORSE_SADDLE), SoundCategory.MASTER, player.getX(), player.getY(), player.getZ(), 2.f, 1.f, 0));
						}
					});
					cooldown.set(heldStack.getItem(), result);

					return TypedActionResult.success(ItemStack.EMPTY);
				}
			} else {
				// No swing
				if (participant.getLastAction() == PlayerAction.USE_BLOCK)
					participant.setLastAction(PlayerAction.USE_BLOCK_AND_ITEM);
			}
		}
		return TypedActionResult.pass(ItemStack.EMPTY);
	}

	private ActionResult onAttackEntity(ServerPlayerEntity player, Hand hand, Entity entity, EntityHitResult entityHitResult) {
		if (player.interactionManager.getGameMode() == GameMode.SPECTATOR)
			return ActionResult.PASS;
		return ActionResult.FAIL;
	}

	private void spawnParticipant(ServerPlayerEntity player) {
		QuakecraftPlayer participant = this.getParticipant(player);
		if (participant != null) {
			participant.reset(player);
			this.spawnLogic.spawnPlayer(player);
		} else if (this.running) {
			player.changeGameMode(GameMode.SPECTATOR);
			player.getInventory().clear();
			this.spawnLogic.spawnWaitingPlayer(player);
		}
	}

	public int getTime() {
		return this.time;
	}
}
