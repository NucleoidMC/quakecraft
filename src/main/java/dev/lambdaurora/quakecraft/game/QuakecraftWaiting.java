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

package dev.lambdaurora.quakecraft.game;

import com.google.common.collect.Multimap;
import dev.lambdaurora.quakecraft.Quakecraft;
import dev.lambdaurora.quakecraft.game.map.MapBuilder;
import dev.lambdaurora.quakecraft.game.map.QuakecraftMap;
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
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.*;
import xyz.nucleoid.plasmid.game.event.*;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.game.player.TeamAllocator;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;

/**
 * Represents a Quakecraft wait-room.
 *
 * @author LambdAurora
 * @version 1.6.0
 * @since 1.0.0
 */
public class QuakecraftWaiting {
    private final GameLogic logic;
    private final QuakecraftMap map;
    private final QuakecraftConfig config;
    private final QuakecraftSpawnLogic spawnLogic;

    private QuakecraftWaiting(GameLogic logic, @NotNull QuakecraftMap map, @NotNull QuakecraftConfig config) {
        this.logic = logic;
        this.map = map;
        this.config = config;
        this.spawnLogic = new QuakecraftSpawnLogic(logic.getSpace(), map);
    }

    public static @NotNull GameOpenProcedure open(@NotNull GameOpenContext<QuakecraftConfig> context) {
        var config = context.getConfig();

        var map = new MapBuilder(config.map).create();
        var worldConfig = new BubbleWorldConfig()
                .setGenerator(map.asGenerator(context.getServer()))
                .setDefaultGameMode(GameMode.SPECTATOR)
                .setTimeOfDay(config.map.time);

        return context.createOpenProcedure(worldConfig, logic -> {
            map.init(logic.getSpace().getWorld());

            QuakecraftWaiting waiting = new QuakecraftWaiting(logic, map, config);

            GameWaitingLobby.applyTo(logic, config.players);

            logic.on(RequestStartListener.EVENT, waiting::requestStart);

            logic.on(PlayerAddListener.EVENT, waiting::addPlayer);
            logic.on(PlayerRemoveListener.EVENT, waiting::removePlayer);
            logic.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);

            logic.on(UseBlockListener.EVENT, waiting::onUseBlock);
            logic.on(UseItemListener.EVENT, waiting::onUseItem);
            logic.on(AttackEntityListener.EVENT, waiting::onAttackEntity);

            logic.setRule(GameRule.INTERACTION, RuleResult.ALLOW);
        });
    }

    private StartResult requestStart() {
        var players = this.allocatePlayers();
        QuakecraftGame.open(this.config, this.logic, this.map, this.spawnLogic, players);
        return StartResult.OK;
    }

    private void spawnPlayer(@NotNull ServerPlayerEntity player) {
        this.spawnLogic.resetWaitingPlayer(player);
        this.spawnLogic.spawnWaitingPlayer(player);
    }

    private void addPlayer(@NotNull ServerPlayerEntity player) {
        this.spawnPlayer(player);
    }

    private void removePlayer(@NotNull ServerPlayerEntity player) {
        Quakecraft.removeSpeed(player);
    }

    private @NotNull ActionResult onPlayerDeath(@NotNull ServerPlayerEntity player, @NotNull DamageSource source) {
        this.spawnPlayer(player);
        return ActionResult.FAIL;
    }

    private ActionResult onUseBlock(ServerPlayerEntity playerEntity, Hand hand, BlockHitResult blockHitResult) {
        return ActionResult.FAIL;
    }

    private @NotNull TypedActionResult<ItemStack> onUseItem(@NotNull ServerPlayerEntity player, @NotNull Hand hand) {
        var heldStack = player.getStackInHand(hand);

        if (heldStack.getItem().isIn(ItemTags.BEDS)) {
            this.logic.getSpace().removePlayer(player);
            return TypedActionResult.success(heldStack);
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }

    private @NotNull ActionResult onAttackEntity(ServerPlayerEntity player, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        if (player.interactionManager.getGameMode() == GameMode.SPECTATOR)
            return ActionResult.PASS;
        return ActionResult.FAIL;
    }

    private @Nullable Multimap<GameTeam, ServerPlayerEntity> allocatePlayers() {
        if (this.config.teams.size() == 0) {
            return null;
        }
        var allocator = new TeamAllocator<GameTeam, ServerPlayerEntity>(this.config.teams);
        this.logic.getSpace().getPlayers().forEach(player -> allocator.add(player, null));
        return allocator.build();
    }
}
