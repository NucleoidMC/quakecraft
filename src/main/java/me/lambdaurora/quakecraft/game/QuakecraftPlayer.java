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
import me.lambdaurora.quakecraft.mixin.FireworkRocketEntityAccessor;
import me.lambdaurora.quakecraft.weapon.Weapon;
import me.lambdaurora.quakecraft.weapon.Weapons;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
 * @version 1.0.0
 * @since 1.0.0
 */
public class QuakecraftPlayer implements Comparable<QuakecraftPlayer>
{
    private final ServerWorld        world;
    public final  UUID               uuid;
    public final  String             name;
    public final  Weapon             primaryWeapon;
    private       ServerPlayerEntity player;
    private       long               respawnTime = -1;
    private       int                kills       = 0;

    public QuakecraftPlayer(@NotNull ServerPlayerEntity player)
    {
        this.world = player.getServerWorld();
        this.uuid = player.getUuid();
        this.name = player.getEntityName();
        this.primaryWeapon = Weapons.ADVANCED_SHOOTER;
        this.player = player;
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

    public boolean hasTeam()
    {
        // @TODO team management
        return false;
    }

    public void reset(@NotNull ServerPlayerEntity player)
    {
        this.player = player;

        this.player.setGameMode(GameMode.ADVENTURE);
        this.player.inventory.clear();
    }

    public void onDeath(@NotNull ServerPlayerEntity player)
    {
        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag tag = fireworkStack.getOrCreateSubTag("Fireworks");
        tag.putByte("Flight", (byte) 0);
        ListTag explosions = new ListTag();
        CompoundTag explosion = new CompoundTag();
        explosion.putByte("Type", (byte) 0);
        explosion.putIntArray("Colors", new int[]{15435844, 11743532});
        explosions.add(explosion);
        tag.put("Explosions", explosions);
        FireworkRocketEntity firework = new FireworkRocketEntity(this.world, player.getX(), player.getY() + 1.0, player.getZ(), fireworkStack);
        firework.setSilent(true);
        ((FireworkRocketEntityAccessor) firework).setLifeTime(0);
        this.world.spawnEntity(firework);
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
        return this.player;
    }

    @Override
    public int compareTo(@NotNull QuakecraftPlayer other)
    {
        return Integer.compare(this.getKills(), other.getKills());
    }
}
