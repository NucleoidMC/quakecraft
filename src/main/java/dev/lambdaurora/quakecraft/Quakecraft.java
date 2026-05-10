/*
 * Copyright (c) 2022 LambdAurora <email@lambdaurora.dev>
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

package dev.lambdaurora.quakecraft;

import dev.lambdaurora.quakecraft.game.QuakecraftConfig;
import dev.lambdaurora.quakecraft.game.QuakecraftLogic;
import dev.lambdaurora.quakecraft.game.QuakecraftWaiting;
import dev.lambdaurora.quakecraft.mixin.FireworkRocketEntityAccessor;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Represents the Quakecraft minigame mod.
 *
 * @author LambdAurora
 * @version 1.7.3
 * @since 1.0.0
 */
public class Quakecraft implements ModInitializer {
	public static final String NAMESPACE = "quakecraft";
	private static Quakecraft INSTANCE;
	public final Logger logger = LogManager.getLogger(NAMESPACE);
	private final List<QuakecraftLogic> activeGames = new ArrayList<>();
	private final List<ServerPlayer> activePlayers = new ArrayList<>();

	@Override
	public void onInitialize() {
		INSTANCE = this;

		QuakecraftRegistry.init();

		GameTypes.register(Identifier.fromNamespaceAndPath(NAMESPACE, "quakecraft"),
				QuakecraftConfig.CODEC, QuakecraftWaiting::open);
	}

	/**
	 * Prints a message to the terminal.
	 *
	 * @param info the message to print
	 */
	public void log(String info) {
		this.logger.info("[" + NAMESPACE + "] " + info);
	}

	public void addActivePlayer(@NotNull ServerPlayer player) {
		this.activePlayers.add(player);
	}

	public void removeActivePlayer(@NotNull ServerPlayer player) {
		this.activePlayers.remove(player);
	}

	public boolean isPlayerActive(@NotNull ServerPlayer player) {
		return this.activePlayers.contains(player);
	}

	public void addActiveGame(@NotNull QuakecraftLogic game) {
		this.activeGames.add(game);
	}

	public void removeActiveGame(@NotNull QuakecraftLogic game) {
		this.activeGames.remove(game);
	}

	public List<QuakecraftLogic> getActiveGames() {
		return this.activeGames;
	}

	public static Quakecraft get() {
		return INSTANCE;
	}

	public static Identifier id(@NotNull String name) {
		return Identifier.fromNamespaceAndPath(NAMESPACE, name);
	}

	/**
	 * Applies the speed modifier to the specified player.
	 *
	 * @param player the player
	 * @since 1.1.0
	 */
	public static void applySpeed(ServerPlayer player) {
		var movementSpeedAttribute = player.getAttributes().getInstance(Attributes.MOVEMENT_SPEED);
		if (movementSpeedAttribute != null) {
			movementSpeedAttribute.removeModifier(QuakecraftConstants.PLAYER_MOVEMENT_SPEED_MODIFIER.id());
			movementSpeedAttribute.addTransientModifier(QuakecraftConstants.PLAYER_MOVEMENT_SPEED_MODIFIER);
		}
	}

	/**
	 * Removes the speed modifier to the specified player.
	 *
	 * @param player the player
	 * @since 1.1.0
	 */
	public static void removeSpeed(ServerPlayer player) {
		var movementSpeedAttribute = player.getAttributes().getInstance(Attributes.MOVEMENT_SPEED);
		if (movementSpeedAttribute != null) {
			movementSpeedAttribute.removeModifier(QuakecraftConstants.PLAYER_MOVEMENT_SPEED_MODIFIER.id());
		}
	}

	public static Direction getDirectionByName(@Nullable String name) {
		return name == null ? null : Direction.byName(name.toLowerCase(Locale.ROOT));
	}

	public static void spawnFirework(ServerLevel world, double x, double y, double z, int[] colors, boolean silent, int lifetime) {
		var fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);

		fireworkStack.set(DataComponents.FIREWORKS, new Fireworks(0, List.of(new FireworkExplosion(FireworkExplosion.Shape.SMALL_BALL,
				IntList.of(colors), IntList.of(), false, false))));
		var firework = new FireworkRocketEntity(world, x, y, z, fireworkStack);
		firework.setSilent(silent);
		if (lifetime >= 0)
			((FireworkRocketEntityAccessor) firework).setLifeTime(lifetime);
		world.addFreshEntity(firework);
	}
}
