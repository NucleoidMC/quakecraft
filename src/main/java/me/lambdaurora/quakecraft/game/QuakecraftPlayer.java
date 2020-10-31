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

import me.lambdaurora.quakecraft.PlayerAction;
import me.lambdaurora.quakecraft.Quakecraft;
import me.lambdaurora.quakecraft.QuakecraftConstants;
import me.lambdaurora.quakecraft.weapon.Weapon;
import me.lambdaurora.quakecraft.weapon.Weapons;
import me.lambdaurora.quakecraft.weapon.inventory.WeaponManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameWorld;

import java.util.UUID;

/**
 * Represents a Quakecraft player.
 *
 * @author LambdAurora
 * @version 1.3.0
 * @since 1.0.0
 */
public class QuakecraftPlayer implements Comparable<QuakecraftPlayer>
{
    private final ServerWorld        world;
    public final  UUID               uuid;
    public final  String             name;
    private final WeaponManager      weapons          = new WeaponManager();
    private       ServerPlayerEntity player;
    private       long               respawnTime      = -1;
    private       PlayerAction       lastAction       = PlayerAction.NONE;
    private       int                kills            = 0;
    private       int                killsWithinATick = 0;

    private boolean left = false;

    public QuakecraftPlayer(@NotNull ServerPlayerEntity player)
    {
        this.world = player.getServerWorld();
        this.uuid = player.getUuid();
        this.name = player.getEntityName();
        this.weapons.add(Weapons.ADVANCED_SHOOTER);
        this.weapons.add(Weapons.ROCKET_LAUNCHER);
        this.weapons.add(Weapons.GRENADE_LAUNCHER);
        this.player = player;
    }

    public int getKills()
    {
        return this.kills;
    }

    public void incrementKills()
    {
        this.killsWithinATick++;
    }

    public boolean hasWon()
    {
        return this.kills >= 24;
    }

    public boolean hasTeam()
    {
        // @TODO team management
        return false;
    }

    public boolean hasLeft()
    {
        return this.left;
    }

    public void leave()
    {
        this.left = true;

        Quakecraft.removeSpeed(this.player);
    }

    /**
     * Resets the player.
     *
     * @param player The player instance.
     */
    public void reset(@NotNull ServerPlayerEntity player)
    {
        this.player = player;

        if (this.left) {
            this.player.setGameMode(GameMode.SPECTATOR);
            return;
        }

        this.player.setGameMode(GameMode.ADVENTURE);
        this.player.inventory.clear();

        this.weapons.insertStacks(this.player);
        this.syncInventory();

        this.player.setVelocity(0, 0, 0);

        Quakecraft.applySpeed(this.player);

        this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 60 * 60 * 20, 0, false, false));

        this.lastAction = PlayerAction.NONE;
    }

    public void tick(@NotNull GameWorld world)
    {
        this.kills += this.killsWithinATick;
        if (this.killsWithinATick >= 2) {
            if (this.killsWithinATick <= 5) {
                world.getPlayerSet().sendMessage(new TranslatableText("quakecraft.game.special.kills." + this.killsWithinATick, this.getDisplayName())
                        .formatted(Formatting.RED, Formatting.BOLD));
            } else {
                world.getPlayerSet().sendMessage(new TranslatableText("quakecraft.game.special.kills.lot", this.getDisplayName())
                        .formatted(Formatting.RED, Formatting.BOLD));
            }
        }

        this.killsWithinATick = 0;

        this.weapons.tick();
        Weapon heldWeapon = this.weapons.get(this.player.getMainHandStack());
        if (heldWeapon != null) {
            int secondaryCooldown = this.weapons.getSecondaryCooldown(heldWeapon);
            if (secondaryCooldown > 0) {
                String bar = "▊▊▊▊▊▊▊▊▊▊";
                int progress = (int) (secondaryCooldown / (double) heldWeapon.secondaryCooldown * bar.length());
                this.player.sendMessage(new LiteralText("[").formatted(Formatting.GRAY)
                        .append(new LiteralText(bar.substring(progress)).formatted(Formatting.GREEN))
                        .append(new LiteralText(bar.substring(0, progress)).formatted(Formatting.RED))
                        .append("]"), true);
            }
        }

        this.syncInventory();
    }

    /**
     * Synchronizes the player inventory.
     */
    public void syncInventory()
    {
        this.player.currentScreenHandler.sendContentUpdates();
        this.player.playerScreenHandler.onContentChanged(this.player.inventory);
        this.player.updateCursorStack();
    }

    /**
     * Fired when the game ends.
     */
    public void onEnd()
    {
        this.player.inventory.clear();
    }

    public void onDeath()
    {
        this.player.playSound(SoundEvents.ENTITY_BLAZE_DEATH, 2.f, 1.f);
        Quakecraft.spawnFirework(this.world, this.player.getX(), this.player.getY(), this.player.getZ(), new int[]{15435844, 11743532}, true, 0);
    }

    public void startRespawn(long time)
    {
        this.respawnTime = time + QuakecraftConstants.RESPAWN_TICKS;
    }

    boolean tryRespawn(long time)
    {
        if (this.respawnTime != -1 && time >= this.respawnTime) {
            this.respawnTime = -1;
            return true;
        }
        return false;
    }

    public int onItemUse(@NotNull GameWorld world, @NotNull ServerPlayerEntity player, @NotNull Hand hand)
    {
        this.lastAction = PlayerAction.USE;

        return this.weapons.onPrimary(world, player, hand);
    }

    public void onSecondary(@NotNull GameWorld world)
    {
        this.weapons.onSecondary(world, this.player);
    }

    public void onSwingHand(@NotNull GameWorld world)
    {
        // Mmmhh yes attack prediction, really not fun to implement.
        if (this.lastAction.isUse()) {
            this.lastAction = PlayerAction.NONE;
            return;
        }

        this.onSecondary(world);
    }

    public @NotNull PlayerAction getLastAction()
    {
        return this.lastAction;
    }

    void setLastAction(@NotNull PlayerAction action)
    {
        this.lastAction = action;
    }

    /**
     * Returns the display name of the player.
     *
     * @return The display name.
     */
    public @NotNull Text getDisplayName()
    {
        if (this.player != null)
            return this.player.getDisplayName();

        return new LiteralText(this.name);
    }

    public @Nullable ServerPlayerEntity getPlayer()
    {
        return this.player;
    }

    @Override
    public int compareTo(@NotNull QuakecraftPlayer other)
    {
        return Integer.compare(this.getKills(), other.getKills());
    }
}
