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

import me.lambdaurora.quakecraft.QuakecraftConstants;
import me.lambdaurora.quakecraft.weapon.Weapon;
import me.lambdaurora.quakecraft.weapon.Weapons;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameWorld;

import java.util.UUID;

/**
 * Represents a Quakecraft player.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class QuakecraftPlayer implements Comparable<QuakecraftPlayer>
{
    private final ServerWorld world;
    public final  UUID        uuid;
    public final  String      name;
    public final  Weapon      primaryWeapon;
    private       long        respawnTime = -1;
    private       int         kills       = 0;

    public QuakecraftPlayer(@NotNull ServerPlayerEntity player)
    {
        this.world = player.getServerWorld();
        this.uuid = player.getUuid();
        this.name = player.getEntityName();
        this.primaryWeapon = Weapons.ADVANCED_SHOOTER;
    }

    public int getKills()
    {
        return this.kills;
    }

    public void incrementKills()
    {
        this.kills++;
    }

    public boolean hasWon()
    {
        return this.kills >= 24;
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
        ItemStack heldStack = player.getStackInHand(hand);

        if (this.primaryWeapon.matchesStack(heldStack)) {
            this.primaryWeapon.onUse(world, player, hand);
            return this.primaryWeapon.cooldown;
        }

        return -1;
    }

    public @Nullable ServerPlayerEntity getPlayer()
    {
        return this.world.getServer().getPlayerManager().getPlayer(this.uuid);
    }

    @Override
    public int compareTo(@NotNull QuakecraftPlayer other)
    {
        return Integer.compare(this.getKills(), other.getKills());
    }
}
