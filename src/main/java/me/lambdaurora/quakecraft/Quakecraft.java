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

package me.lambdaurora.quakecraft;

import me.lambdaurora.quakecraft.game.QuakecraftConfig;
import me.lambdaurora.quakecraft.game.QuakecraftWaiting;
import me.lambdaurora.quakecraft.mixin.FireworkRocketEntityAccessor;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.GameType;

/**
 * Represents the Quakecraft minigame mod.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class Quakecraft implements ModInitializer
{
    public static final String     NAMESPACE = "quakecraft";
    private static      Quakecraft INSTANCE;
    public final        Logger     logger    = LogManager.getLogger(NAMESPACE);

    @Override
    public void onInitialize()
    {
        INSTANCE = this;

        GameType.register(new Identifier(NAMESPACE, "quakecraft"),
                QuakecraftWaiting::open,
                QuakecraftConfig.CODEC);
    }

    /**
     * Prints a message to the terminal.
     *
     * @param info The message to print.
     */
    public void log(String info)
    {
        this.logger.info("[" + NAMESPACE + "] " + info);
    }

    public static final Quakecraft get()
    {
        return INSTANCE;
    }

    public static void spawnFirework(@NotNull ServerWorld world, double x, double y, double z, int[] colors, boolean silent, int lifetime)
    {
        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);

        CompoundTag tag = fireworkStack.getOrCreateSubTag("Fireworks");
        tag.putByte("Flight", (byte) 0);

        ListTag explosions = new ListTag();
        CompoundTag explosion = new CompoundTag();
        explosion.putByte("Type", (byte) 0);
        explosion.putIntArray("Colors", colors);
        explosions.add(explosion);
        tag.put("Explosions", explosions);

        FireworkRocketEntity firework = new FireworkRocketEntity(world, x, y, z, fireworkStack);
        firework.setSilent(silent);
        if (lifetime >= 0)
            ((FireworkRocketEntityAccessor) firework).setLifeTime(lifetime);
        world.spawnEntity(firework);
    }
}
