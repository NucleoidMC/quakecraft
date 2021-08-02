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

package dev.lambdaurora.quakecraft.weapon;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

/**
 * Represents a weapon.
 *
 * @author LambdAurora
 * @version 1.6.0
 * @since 1.0.0
 */
public class Weapon {
    public final Identifier identifier;
    public final Item item;
    public final int primaryCooldown;
    public final int secondaryCooldown;
    public final int reloadCooldown;
    public final int clipSize;
    public final int ammoSize;

    public Weapon(@NotNull Identifier id, @NotNull Item item, @NotNull Settings settings) {
        this.identifier = id;
        this.item = item;
        this.primaryCooldown = settings.primaryCooldown;
        this.secondaryCooldown = settings.secondaryCooldown;
        this.reloadCooldown = settings.reloadCooldown;
        this.clipSize = settings.clipSize;
        this.ammoSize = settings.ammoSize;
    }

    /**
     * Returns whether the weapon has a secondary action.
     *
     * @return {@code true} if the weapon has a secondary action, else {@code false}
     * @since 1.1.0
     */
    public boolean hasSecondaryAction() {
        return this.secondaryCooldown >= 0;
    }

    /**
     * Returns whether this weapon requires ammo or not.
     *
     * @return {@code true} if the weapon requires ammo, else {@code false}
     * @since 1.4.0
     */
    public boolean doesRequireAmmo() {
        return this.ammoSize > 0;
    }

    /**
     * Returns whether the specified item stack matches this weapon or not.
     *
     * @param stack the item stack
     * @return {@code true} if the item stack matches this weapon, else {@code false}
     */
    public boolean matchesStack(@NotNull ItemStack stack) {
        return !stack.isEmpty()
                && this.item == stack.getItem();
    }

    /**
     * Fired each tick.
     *
     * @param stack the weapon stack
     * @since 1.1.0
     */
    public void tick(@NotNull ItemStack stack) {
    }

    public @NotNull ActionResult onPrimary(@NotNull GameSpace world, @NotNull ServerPlayerEntity player, @NotNull Hand hand) {
        return ActionResult.PASS;
    }

    public @NotNull ActionResult onSecondary(@NotNull GameSpace world, @NotNull ServerPlayerEntity player, @NotNull ItemStack stack) {
        return ActionResult.PASS;
    }

    public @NotNull ItemStackBuilder stackBuilder() {
        return ItemStackBuilder.of(this.item)
                .setUnbreakable();
    }

    /**
     * Builds the item stack.
     *
     * @return the item stack
     */
    public final @NotNull ItemStack build(@NotNull ServerPlayerEntity player) {
        return this.stackBuilder()
                .setName(new TranslatableText("weapon." + this.identifier.getNamespace() + "." + this.identifier.getPath())
                        .styled(style -> style.withItalic(false)))
                .build();
    }

    public static class Settings {
        private final int primaryCooldown;
        private int secondaryCooldown = -1;
        private int reloadCooldown = -1;
        private int clipSize = -1;
        private int ammoSize = -1;

        public Settings(int primaryCooldown) {
            this.primaryCooldown = primaryCooldown;
        }

        public @NotNull Settings secondaryCooldown(int secondaryCooldown) {
            this.secondaryCooldown = secondaryCooldown;
            return this;
        }

        public @NotNull Settings reloadCooldown(int reloadCooldown) {
            this.reloadCooldown = reloadCooldown;
            return this;
        }

        public @NotNull Settings clipSize(int clipSize) {
            this.clipSize = clipSize;
            return this;
        }

        public @NotNull Settings ammoSize(int ammoSize) {
            this.ammoSize = ammoSize;
            return this;
        }
    }
}
