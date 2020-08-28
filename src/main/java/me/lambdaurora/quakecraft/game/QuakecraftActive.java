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
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.event.*;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;
import xyz.nucleoid.plasmid.util.PlayerRef;

import java.util.Set;
import java.util.stream.Collectors;

public class QuakecraftActive
{
    private final QuakecraftConfig config;

    public final  GameWorld                                     world;
    private final Object2ObjectMap<PlayerRef, QuakecraftPlayer> participants;

    private QuakecraftActive(@NotNull QuakecraftConfig config, @NotNull GameWorld world, @NotNull Set<PlayerRef> participants)
    {
        this.config = config;
        this.world = world;
        this.participants = new Object2ObjectOpenHashMap<>();

        for (PlayerRef player : participants) {
            this.participants.put(player, new QuakecraftPlayer());
        }
    }

    public static void open(@NotNull QuakecraftConfig config, @NotNull GameWorld world, @NotNull QuakecraftMap map)
    {
        Set<PlayerRef> participants = world.getPlayers().stream().map(PlayerRef::of).collect(Collectors.toSet());
        QuakecraftActive active = new QuakecraftActive(config, world, participants);

        world.openGame(game -> {
            game.setRule(GameRule.CRAFTING, RuleResult.DENY);
            game.setRule(GameRule.PORTALS, RuleResult.DENY);
            game.setRule(GameRule.PVP, RuleResult.DENY);
            game.setRule(GameRule.HUNGER, RuleResult.DENY);
            game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
            game.setRule(GameRule.INTERACTION, RuleResult.DENY);
            game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
            game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
            game.setRule(GameRule.UNSTABLE_TNT, RuleResult.DENY);

            game.on(GameOpenListener.EVENT, active::onOpen);
            game.on(GameCloseListener.EVENT, active::onClose);

            game.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
            game.on(PlayerAddListener.EVENT, active::addPlayer);
            game.on(PlayerRemoveListener.EVENT, active::removePlayer);

            game.on(GameTickListener.EVENT, active::tick);

            game.on(PlayerDeathListener.EVENT, active::onPlayerDeath);

            game.on(UseItemListener.EVENT, active::onUseItem);
            game.on(HandSwingListener.EVENT, active::onSwingHand);
        });
    }

    private void onOpen()
    {
        for (ServerPlayerEntity player : this.world.getPlayers()) {
            this.spawnParticipant(player);
        }
    }

    private void onClose()
    {

    }

    private void tick()
    {

    }

    private void addPlayer(@NotNull ServerPlayerEntity player)
    {

    }

    private void removePlayer(@NotNull ServerPlayerEntity player)
    {

    }

    private @NotNull ActionResult onPlayerDeath(@NotNull ServerPlayerEntity player, @NotNull DamageSource source)
    {
        return ActionResult.FAIL;
    }

    private void onSwingHand(@NotNull ServerPlayerEntity player, @NotNull Hand hand)
    {

    }

    private @NotNull TypedActionResult<ItemStack> onUseItem(@NotNull ServerPlayerEntity player, @NotNull Hand hand)
    {
        ItemStack heldStack = player.getStackInHand(hand);

        if (heldStack.getItem().isIn(FabricToolTags.HOES)) {
            ItemCooldownManager cooldown = player.getItemCooldownManager();
            if (!cooldown.isCoolingDown(heldStack.getItem())) {
                QuakecraftPlayer participant = this.getParticipant(player);
                if (participant != null) {
                    participant.shoot();
                    Vec3d rotationVec = player.getRotationVec(1.0F);
                    player.setVelocity(rotationVec.multiply(1.2));
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

                    player.playSound(SoundEvents.ENTITY_HORSE_SADDLE, 1.0F, 1.0F);
                    cooldown.set(heldStack.getItem(), 20);
                }
            }
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }

    private void spawnParticipant(@NotNull ServerPlayerEntity player)
    {
        player.setGameMode(GameMode.ADVENTURE);
        player.inventory.clear();

        ItemStackBuilder hoeBuilder = ItemStackBuilder.of(Items.WOODEN_HOE)
                .setUnbreakable();

        player.inventory.insertStack(hoeBuilder.build());
    }

    public @Nullable QuakecraftPlayer getParticipant(@NotNull ServerPlayerEntity player)
    {
        return this.participants.get(PlayerRef.of(player));
    }
}
