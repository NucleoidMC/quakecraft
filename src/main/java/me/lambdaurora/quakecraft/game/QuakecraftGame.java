/*
 *  Copyright (c) 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lambdaurora.quakecraft.game;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.lambdaurora.quakecraft.game.map.QuakecraftMap;
import me.lambdaurora.quakecraft.weapon.Weapons;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.event.*;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the Quakecraft running game.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class QuakecraftGame
{
    private final QuakecraftConfig                         config;
    private final QuakecraftMap                            map;
    public final  GameWorld                                world;
    private final QuakecraftSpawnLogic                     spawnLogic;
    private final QuakecraftScoreboard                     scoreboard;
    private final Object2ObjectMap<UUID, QuakecraftPlayer> participants;
    private       int                                      time;

    private QuakecraftGame(@NotNull QuakecraftConfig config, @NotNull GameWorld world, @NotNull QuakecraftMap map, @NotNull QuakecraftSpawnLogic spawnLogic,
                           @NotNull Set<ServerPlayerEntity> participants)
    {
        this.config = config;
        this.world = world;
        this.map = map;
        this.spawnLogic = spawnLogic;
        this.scoreboard = new QuakecraftScoreboard(this);
        this.participants = new Object2ObjectOpenHashMap<>();

        for (ServerPlayerEntity player : participants) {
            this.participants.put(player.getUuid(), new QuakecraftPlayer(player));
        }

        this.time = this.config.time;
    }

    /**
     * Opens the game.
     *
     * @param config     The game configuration.
     * @param world      The game world.
     * @param map        The game map.
     * @param spawnLogic The game spawn logic.
     */
    public static void open(@NotNull QuakecraftConfig config, @NotNull GameWorld world, @NotNull QuakecraftMap map, @NotNull QuakecraftSpawnLogic spawnLogic)
    {
        QuakecraftGame active = new QuakecraftGame(config, world, map, spawnLogic, world.getPlayers());

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
        });
    }

    private void onOpen()
    {
        for (ServerPlayerEntity player : this.world.getPlayers()) {
            this.spawnParticipant(player);
        }
        this.scoreboard.update();
    }

    private void onClose()
    {
        this.scoreboard.close();
    }

    private void tick()
    {
        this.participants.forEach((uuid, participant) -> {
            if (participant.hasWon()) {
                onWin(participant);
            }
        });
        this.time--;
        this.scoreboard.update();

        if (this.time <= 0) {
            this.world.getPlayerSet().sendMessage(new LiteralText("nobody has won.").formatted(Formatting.RED));
            this.world.close();
        }
    }

    private void onWin(@NotNull QuakecraftPlayer winner)
    {
        this.world.getPlayerSet().sendMessage(new LiteralText(winner.name + " has won \\o/"));
        this.world.close();
    }

    private void addPlayer(@NotNull ServerPlayerEntity player)
    {
        this.spawnLogic.spawnPlayer(player);
    }

    private void removePlayer(@NotNull ServerPlayerEntity player)
    {

    }

    private boolean onDamage(ServerPlayerEntity player, DamageSource source, float amount)
    {
        return source.isExplosive() && !(source.getAttacker() instanceof ServerPlayerEntity);
    }

    private @NotNull ActionResult onPlayerDeath(@NotNull ServerPlayerEntity player, @NotNull DamageSource source)
    {
        LivingEntity attacker = player.getAttacker();
        if (attacker != null) {
            QuakecraftPlayer other = this.participants.get(attacker.getUuid());
            if (other != null) {
                attacker.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 2.f, 5.f);
                other.incrementKills();
                this.world.getPlayerSet().sendMessage(new LiteralText("")
                        .append(attacker.getDisplayName())
                        .append(new LiteralText(" killed ").formatted(Formatting.GRAY))
                        .append(player.getDisplayName())
                        .append(new LiteralText(".").formatted(Formatting.GRAY)));

                this.getOptParticipant(player).ifPresent(participant -> participant.onDeath(player));
            }

            player.setAttacker(null);
            attacker.setAttacking(null);
        }

        this.spawnParticipant(player);

        return ActionResult.FAIL;
    }

    private void onSwingHand(@NotNull ServerPlayerEntity player, @NotNull Hand hand)
    {
    }

    private ActionResult onUseBlock(ServerPlayerEntity playerEntity, Hand hand, BlockHitResult blockHitResult)
    {
        return ActionResult.FAIL;
    }

    private @NotNull TypedActionResult<ItemStack> onUseItem(@NotNull ServerPlayerEntity player, @NotNull Hand hand)
    {
        ItemStack heldStack = player.getStackInHand(hand);

        ItemCooldownManager cooldown = player.getItemCooldownManager();
        if (!cooldown.isCoolingDown(heldStack.getItem())) {
            QuakecraftPlayer participant = this.getParticipant(player);
            if (participant != null) {
                int result = participant.onItemUse(this.world, player, hand);
                if (result != -1) {
                    player.playSound(SoundEvents.ENTITY_HORSE_SADDLE, 2.f, 1.f);
                    cooldown.set(heldStack.getItem(), result);
                    return TypedActionResult.success(ItemStack.EMPTY);
                }
            }
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }

    private void spawnParticipant(@NotNull ServerPlayerEntity player)
    {
        player.setGameMode(GameMode.ADVENTURE);
        player.inventory.clear();

        QuakecraftPlayer participant = this.getParticipant(player);
        if (participant != null) {
            participant.reset(player);
        } else {
            player.inventory.insertStack(Weapons.BASE_SHOOTER.build());
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
