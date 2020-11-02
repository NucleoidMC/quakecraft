/*
 * Copyright (c) 2020 LambdAurora <aurora42lambda@gmail.com>
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

package me.lambdaurora.quakecraft.game;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.lambdaurora.quakecraft.PlayerAction;
import me.lambdaurora.quakecraft.Quakecraft;
import me.lambdaurora.quakecraft.entity.GrenadeEntity;
import me.lambdaurora.quakecraft.game.map.QuakecraftMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.event.*;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;

import java.util.*;

/**
 * Represents the Quakecraft running game.
 *
 * @author LambdAurora
 * @version 1.5.0
 * @since 1.0.0
 */
public class QuakecraftGame extends QuakecraftLogic
{
    private final QuakecraftSpawnLogic spawnLogic;
    private final QuakecraftScoreboard scoreboard;
    private final Object2ObjectMap<UUID, QuakecraftPlayer> participants;
    private boolean running = false;
    private boolean end = false;
    private int time;
    private int endTime = 10 * 20;

    private Set<QuakecraftPlayer> winners = new HashSet<>();

    private QuakecraftGame(@NotNull QuakecraftConfig config, @NotNull GameWorld world, @NotNull QuakecraftMap map, @NotNull QuakecraftSpawnLogic spawnLogic,
                           @NotNull Set<ServerPlayerEntity> participants)
    {
        super(world, config, map);
        this.spawnLogic = spawnLogic;
        this.scoreboard = new QuakecraftScoreboard(this);
        this.participants = new Object2ObjectOpenHashMap<>();

        for (ServerPlayerEntity player : participants) {
            this.participants.put(player.getUuid(), new QuakecraftPlayer(player));
        }

        this.time = this.getConfig().time;
    }

    /**
     * Opens the game.
     *
     * @param config The game configuration.
     * @param world The game world.
     * @param map The game map.
     * @param spawnLogic The game spawn logic.
     */
    public static void open(@NotNull QuakecraftConfig config, @NotNull GameWorld world, @NotNull QuakecraftMap map, @NotNull QuakecraftSpawnLogic spawnLogic)
    {
        QuakecraftGame active = new QuakecraftGame(config, world, map, spawnLogic, world.getPlayers());
        map.postInit(active);
        world.openGame(game -> {
            game.setRule(GameRule.CRAFTING, RuleResult.DENY);
            game.setRule(GameRule.PORTALS, RuleResult.DENY);
            game.setRule(GameRule.PVP, RuleResult.DENY);
            game.setRule(GameRule.HUNGER, RuleResult.DENY);
            game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
            game.setRule(GameRule.INTERACTION, RuleResult.ALLOW);
            game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
            game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
            game.setRule(GameRule.UNSTABLE_TNT, RuleResult.DENY);

            game.on(GameOpenListener.EVENT, active::onOpen);
            game.on(GameCloseListener.EVENT, active::onClose);

            game.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
            game.on(PlayerAddListener.EVENT, active::addPlayer);
            game.on(PlayerRemoveListener.EVENT, active::removePlayer);

            game.on(GameTickListener.EVENT, active::tick);

            game.on(PlayerDamageListener.EVENT, active::onDamage);
            game.on(PlayerDeathListener.EVENT, active::onPlayerDeath);

            game.on(UseBlockListener.EVENT, active::onUseBlock);
            game.on(UseItemListener.EVENT, active::onUseItem);
            game.on(HandSwingListener.EVENT, active::onSwingHand);
            game.on(AttackEntityListener.EVENT, active::onAttackEntity);
        });
    }

    private void onOpen()
    {
        for (ServerPlayerEntity player : this.getWorld().getPlayers()) {
            this.spawnParticipant(player);
            Quakecraft.get().addActivePlayer(player);
        }
        this.scoreboard.update();
        this.running = true;
    }

    private void onClose()
    {
        this.scoreboard.close();
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.running) {
            int[] activePlayer = new int[]{0};
            this.participants.forEach((uuid, participant) -> {
                if (participant.hasLeft())
                    return;

                participant.tick(this.getWorld());
                activePlayer[0]++;

                if (participant.hasWon()) {
                    this.onWin(participant);
                }
            });
            this.time--;

            if (activePlayer[0] <= 1) {
                this.getWorld().getPlayerSet().sendMessage(new TranslatableText("quakecraft.game.end.not_enough_players").formatted(Formatting.RED));
                this.getWorld().close();
            }

            if (this.time <= 0) {
                this.getWorld().getPlayerSet().sendMessage(new TranslatableText("quakecraft.game.end.nobody_won").formatted(Formatting.RED));
                this.getWorld().close();
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

                        Quakecraft.spawnFirework(this.getWorld().getWorld(), mcPlayer.getX(), mcPlayer.getY(), mcPlayer.getZ(), new int[]{15435844, 11743532}, false, -1);
                    }
                });
            }

            if (this.endTime == 0)
                this.getWorld().close();
        }

        this.scoreboard.update();
    }

    private void onWin(@NotNull QuakecraftPlayer winner)
    {
        this.getWorld().getPlayerSet().sendMessage(new TranslatableText("quakecraft.game.end.win", winner.getDisplayName()).formatted(Formatting.GREEN));
        this.end = true;
        this.running = false;
        this.winners.add(winner);
    }

    private void addPlayer(@NotNull ServerPlayerEntity player)
    {
        this.spawnParticipant(player);
    }

    private void removePlayer(@NotNull ServerPlayerEntity player)
    {
        QuakecraftPlayer participant = this.participants.get(player.getUuid());
        if (participant != null) {
            participant.leave();
        }
        Quakecraft.get().removeActivePlayer(player);
    }

    private boolean onDamage(ServerPlayerEntity player, DamageSource source, float amount)
    {
        if (source.isExplosive()) {
            Entity attacker = null;
            if (source.getSource() instanceof GrenadeEntity) {
                GrenadeEntity grenade = (GrenadeEntity) source.getSource();
                attacker = grenade.getOwner();
            } else if (source.getSource() instanceof ServerPlayerEntity) {
                attacker = source.getSource();
            }

            if (attacker != null) {
                if (attacker instanceof ServerPlayerEntity && attacker != player) {
                    player.setAttacker((LivingEntity) attacker);
                    ((ServerPlayerEntity) attacker).setAttacking(player);
                    player.kill();
                }
            }
        }
        return source.isExplosive() && !(source.getAttacker() instanceof ServerPlayerEntity);
    }

    private @NotNull ActionResult onPlayerDeath(@NotNull ServerPlayerEntity player, @NotNull DamageSource source)
    {
        LivingEntity attacker = player.getAttacker();
        if (attacker != null) {
            QuakecraftPlayer other = this.participants.get(attacker.getUuid());
            if (other != null) {
                ((ServerPlayerEntity) attacker).playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 2.f, 5.f);
                other.incrementKills();
                this.getWorld().getPlayerSet().sendMessage(
                        new TranslatableText("quakecraft.game.kill", attacker.getDisplayName(), player.getDisplayName()).formatted(Formatting.GRAY)
                );

                this.getOptParticipant(player).ifPresent(QuakecraftPlayer::onDeath);
            }

            player.setAttacker(null);
            attacker.setAttacking(null);
        }

        this.spawnParticipant(player);

        return ActionResult.FAIL;
    }

    private void onSwingHand(@NotNull ServerPlayerEntity player, @NotNull Hand hand)
    {
        if (Thread.currentThread() != player.getServer().getThread())
            return;

        if (hand == Hand.OFF_HAND) {
            // Attack cannot be in OFF_HAND
            return;
        }
        QuakecraftPlayer participant = this.getParticipant(player);
        if (participant == null)
            return;
        participant.onSwingHand(this.getWorld());
    }

    private ActionResult onUseBlock(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult)
    {
        QuakecraftPlayer participant = this.getParticipant(player);
        if (participant != null) {
            if (participant.getLastAction() == PlayerAction.USE_BLOCK_AND_ITEM)
                participant.setLastAction(PlayerAction.NONE);
            else
                participant.setLastAction(PlayerAction.USE_BLOCK);
        }
        return ActionResult.FAIL;
    }

    private @NotNull TypedActionResult<ItemStack> onUseItem(@NotNull ServerPlayerEntity player, @NotNull Hand hand)
    {
        if (hand == Hand.OFF_HAND) {
            return TypedActionResult.fail(ItemStack.EMPTY);
        }

        ItemStack heldStack = player.getStackInHand(hand);

        QuakecraftPlayer participant = this.getParticipant(player);
        if (participant != null) {
            ItemCooldownManager cooldown = player.getItemCooldownManager();
            if (!cooldown.isCoolingDown(heldStack.getItem())) {
                int result = participant.onItemUse(this.getWorld(), player, hand);
                if (result != -1) {
                    this.getWorld().getPlayerSet().forEach(other -> {
                        if (player.squaredDistanceTo(other) <= 16.f) {
                            other.networkHandler.sendPacket(new PlaySoundS2CPacket(SoundEvents.ENTITY_HORSE_SADDLE, SoundCategory.MASTER, player.getX(), player.getY(), player.getZ(), 2.f, 1.f));
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

    private @NotNull ActionResult onAttackEntity(ServerPlayerEntity player, Hand hand, Entity entity, EntityHitResult entityHitResult)
    {
        if (player.interactionManager.getGameMode() == GameMode.SPECTATOR)
            return ActionResult.PASS;
        return ActionResult.FAIL;
    }

    private void spawnParticipant(@NotNull ServerPlayerEntity player)
    {
        QuakecraftPlayer participant = this.getParticipant(player);
        if (participant != null) {
            participant.reset(player);
            this.spawnLogic.spawnPlayer(player);
        } else if (this.running) {
            player.setGameMode(GameMode.SPECTATOR);
            player.inventory.clear();
            this.spawnLogic.spawnWaitingPlayer(player);
        }
    }

    public int getTime()
    {
        return this.time;
    }

    public @Nullable QuakecraftPlayer getParticipant(@NotNull ServerPlayerEntity player)
    {
        return this.participants.get(player.getUuid());
    }

    public @NotNull Optional<QuakecraftPlayer> getOptParticipant(@NotNull ServerPlayerEntity player)
    {
        return Optional.ofNullable(this.getParticipant(player));
    }

    public @NotNull Collection<QuakecraftPlayer> getParticipants()
    {
        return this.participants.values();
    }
}
