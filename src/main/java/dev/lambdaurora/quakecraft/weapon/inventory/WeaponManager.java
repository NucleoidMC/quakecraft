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

package dev.lambdaurora.quakecraft.weapon.inventory;

import dev.lambdaurora.quakecraft.weapon.Weapon;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameSpace;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a weapon manager.
 *
 * @author LambdAurora
 * @version 1.6.0
 * @since 1.2.0
 */
public final class WeaponManager {
    private final List<Weapon> weapons = new ArrayList<>();
    private final Object2IntMap<Weapon> secondaryCooldowns = new Object2IntOpenHashMap<>();

    public WeaponManager() {
        this.secondaryCooldowns.defaultReturnValue(0);
    }

    public void add(@NotNull Weapon weapon) {
        this.weapons.add(weapon);
    }

    public @Nullable Weapon get(@NotNull ItemStack stack) {
        if (stack.isEmpty())
            return null;

        for (Weapon weapon : this.weapons) {
            if (weapon.matchesStack(stack))
                return weapon;
        }
        return null;
    }

    public void tick() {
        for (var weapon : this.weapons) {
            if (weapon.hasSecondaryAction()) {
                int currentCooldown = this.secondaryCooldowns.getInt(weapon);
                if (currentCooldown > 0) {
                    currentCooldown--;
                    this.secondaryCooldowns.put(weapon, currentCooldown);
                }
            }
        }
    }

    public void insertStacks(@NotNull ServerPlayerEntity player) {
        for (Weapon weapon : this.weapons) {
            player.inventory.insertStack(weapon.build(player));
        }
    }

    public int onPrimary(@NotNull GameSpace world, @NotNull ServerPlayerEntity player, @NotNull Hand hand) {
        ItemStack heldStack = player.getStackInHand(hand);

        for (Weapon weapon : this.weapons) {
            if (weapon.matchesStack(heldStack)) {
                weapon.onPrimary(world, player, hand);
                return weapon.primaryCooldown;
            }
        }

        return -1;
    }

    public void onSecondary(@NotNull GameSpace world, @NotNull ServerPlayerEntity player) {
        ItemStack heldStack = player.getStackInHand(Hand.MAIN_HAND);

        for (Weapon weapon : this.weapons) {
            if (weapon.matchesStack(heldStack) && weapon.hasSecondaryAction()) {
                if (this.canUseSecondary(weapon)) {
                    weapon.onSecondary(world, player, heldStack);
                    this.secondaryCooldowns.put(weapon, weapon.secondaryCooldown);
                }
                return;
            }
        }
    }

    public int getSecondaryCooldown(@NotNull Weapon weapon) {
        return this.secondaryCooldowns.getInt(weapon);
    }

    /**
     * Returns whether the secondary fire of the specified weapon is currently available or not.
     *
     * @param weapon the weapon
     * @return {@code true} if the secondary fire is available, else {@code false}
     */
    public boolean canUseSecondary(@NotNull Weapon weapon) {
        return this.getSecondaryCooldown(weapon) == 0;
    }
}
