/*
 * Copyright (c) 2020-2022 LambdAurora <email@lambdaurora.dev>
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

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;

/**
 * Represents a weapon.
 *
 * @author LambdAurora
 * @version 1.7.0
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

	public Weapon(Identifier id, Item item, Settings settings) {
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
	public boolean matchesStack(ItemStack stack) {
		return !stack.isEmpty()
				&& this.item == stack.getItem();
	}

	/**
	 * Fired each tick.
	 *
	 * @param stack the weapon stack
	 * @since 1.1.0
	 */
	public void tick(ItemStack stack) {
	}

	public InteractionResult onPrimary(ServerLevel world, ServerPlayer player, InteractionHand hand) {
		return InteractionResult.PASS;
	}

	public InteractionResult onSecondary(ServerLevel world, ServerPlayer player, ItemStack stack) {
		return InteractionResult.PASS;
	}

	public ItemStackBuilder stackBuilder() {
		return ItemStackBuilder.of(this.item)
				.setUnbreakable();
	}

	/**
	 * Builds the item stack.
	 *
	 * @return the item stack
	 */
	public final ItemStack build(ServerPlayer player) {
		return this.stackBuilder()
				.setName(Component.translatable("weapon." + this.identifier.getNamespace() + "." + this.identifier.getPath())
						.withStyle(style -> style.withItalic(false)))
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

		public Settings secondaryCooldown(int secondaryCooldown) {
			this.secondaryCooldown = secondaryCooldown;
			return this;
		}

		public Settings reloadCooldown(int reloadCooldown) {
			this.reloadCooldown = reloadCooldown;
			return this;
		}

		public Settings clipSize(int clipSize) {
			this.clipSize = clipSize;
			return this;
		}

		public Settings ammoSize(int ammoSize) {
			this.ammoSize = ammoSize;
			return this;
		}
	}
}
