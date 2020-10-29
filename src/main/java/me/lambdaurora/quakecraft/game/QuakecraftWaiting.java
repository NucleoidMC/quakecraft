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

import me.lambdaurora.quakecraft.Quakecraft;
import me.lambdaurora.quakecraft.game.map.MapGenerator;
import me.lambdaurora.quakecraft.game.map.QuakecraftMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.event.*;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.world.bubble.BubbleWorldConfig;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a Quakecraft wait-room.
 *
 * @author LambdAurora
 * @version 1.4.6
 * @since 1.0.0
 */
public class QuakecraftWaiting
{
    private final GameWorld world;
    private final QuakecraftMap map;
    private final QuakecraftConfig config;
    private final QuakecraftSpawnLogic spawnLogic;

    private QuakecraftWaiting(@NotNull GameWorld world, @NotNull QuakecraftMap map, @NotNull QuakecraftConfig config)
    {
        this.world = world;
        this.map = map;
        this.config = config;
        this.spawnLogic = new QuakecraftSpawnLogic(world, map);
    }

    public static @NotNull CompletableFuture<GameWorld> open(@NotNull GameOpenContext<QuakecraftConfig> context)
    {
        QuakecraftConfig config = context.getConfig();
        MapGenerator generator = new MapGenerator(config.map);

        return generator.create().thenCompose(map -> {
            BubbleWorldConfig worldConfig = new BubbleWorldConfig()
                    .setGenerator(map.asGenerator(context.getServer()))
                    .setDefaultGameMode(GameMode.SPECTATOR)
                    .setTimeOfDay(config.map.time);

            return context.openWorld(worldConfig).thenApply(gameWorld -> {
                QuakecraftWaiting waiting = new QuakecraftWaiting(gameWorld, map, config);

                return GameWaitingLobby.open(gameWorld, config.players, game -> {
                    game.setRule(GameRule.CRAFTING, RuleResult.DENY);
                    game.setRule(GameRule.PORTALS, RuleResult.DENY);
                    game.setRule(GameRule.PVP, RuleResult.DENY);
                    game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
                    game.setRule(GameRule.HUNGER, RuleResult.DENY);
                    game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
                    game.setRule(GameRule.INTERACTION, RuleResult.ALLOW);

                    game.on(RequestStartListener.EVENT, waiting::requestStart);

                    game.on(PlayerAddListener.EVENT, waiting::addPlayer);
                    game.on(PlayerRemoveListener.EVENT, waiting::removePlayer);
                    game.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);

                    game.on(UseBlockListener.EVENT, waiting::onUseBlock);
                    game.on(UseItemListener.EVENT, waiting::onUseItem);
                    game.on(AttackEntityListener.EVENT, waiting::onAttackEntity);
                });
            });
        });
    }

    private StartResult requestStart()
    {
        QuakecraftGame.open(this.config, this.world, this.map, this.spawnLogic);
        return StartResult.OK;
    }


    private void spawnPlayer(@NotNull ServerPlayerEntity player)
    {
        this.spawnLogic.resetWaitingPlayer(player);
        this.spawnLogic.spawnWaitingPlayer(player);
    }

    private void addPlayer(@NotNull ServerPlayerEntity player)
    {
        this.spawnPlayer(player);
    }

    private void removePlayer(@NotNull ServerPlayerEntity player)
    {
        Quakecraft.removeSpeed(player);
    }

    private @NotNull ActionResult onPlayerDeath(@NotNull ServerPlayerEntity player, @NotNull DamageSource source)
    {
        this.spawnPlayer(player);
        return ActionResult.FAIL;
    }

    private ActionResult onUseBlock(ServerPlayerEntity playerEntity, Hand hand, BlockHitResult blockHitResult)
    {
        return ActionResult.FAIL;
    }

    private @NotNull TypedActionResult<ItemStack> onUseItem(@NotNull ServerPlayerEntity player, @NotNull Hand hand)
    {
        ItemStack heldStack = player.getStackInHand(hand);

        if (heldStack.getItem().isIn(ItemTags.BEDS)) {
            this.world.removePlayer(player);
            return TypedActionResult.success(heldStack);
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }

    private @NotNull ActionResult onAttackEntity(ServerPlayerEntity player, Hand hand, Entity entity, EntityHitResult entityHitResult)
    {
        if (player.interactionManager.getGameMode() == GameMode.SPECTATOR)
            return ActionResult.PASS;
        return ActionResult.FAIL;
    }
}
