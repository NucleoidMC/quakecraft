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

package dev.lambdaurora.quakecraft.game;

import dev.lambdaurora.quakecraft.Quakecraft;
import dev.lambdaurora.quakecraft.game.map.MapSpawn;
import dev.lambdaurora.quakecraft.game.map.QuakecraftMap;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;

import java.util.Random;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

/**
 * Represents the Quakecraft spawn logic.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.0.0
 */
public class QuakecraftSpawnLogic {
	private static final Random RANDOM = new Random();
	private final GameSpace space;
	private final ServerLevel world;
	private final QuakecraftMap map;
	private final SpawnCache spawnCache;

	public QuakecraftSpawnLogic(GameSpace space, ServerLevel world, QuakecraftMap map) {
		this.space = space;
		this.world = world;
		this.map = map;
		this.spawnCache = new SpawnCache(map.getSpawnCount() / 2);
	}

	public void spawnPlayer(ServerPlayer player) {
		MapSpawn spawn = null;
		int spawnIndex = -1;
		int lowestPlayers = this.space.getPlayers().size();
		for (int i = 0; i < this.map.getSpawnCount(); i++) {
			if (this.spawnCache.contains(i))
				continue;
			var currentSpawn = this.map.getSpawn(i);

			var box = AABB.encapsulatingFullBlocks(currentSpawn.pos().offset(-16, -5, -16), currentSpawn.pos().offset(16, 5, 16));
			int playersNearSpawn = (int) this.space.getPlayers().stream().filter(p -> box.contains(p.position())).count();
			if (playersNearSpawn < lowestPlayers) {
				lowestPlayers = playersNearSpawn;
				spawn = currentSpawn;
				spawnIndex = i;
			}
		}

		if (spawnIndex == -1) {
			spawnIndex = this.spawnCache.rollNextSpawn();
			spawn = this.map.getSpawn(spawnIndex);
		} else {
			this.spawnCache.push(spawnIndex);
		}

		player.teleportTo(this.world, spawn.pos().getX(), spawn.pos().getY(), spawn.pos().getZ(), Set.of(), spawn.direction(), 0.f, false);
	}

	public void resetWaitingPlayer(ServerPlayer player) {
		player.setGameMode(GameType.ADVENTURE);
		player.getInventory().clearContent();

		var leaveGame = ItemStackBuilder.of(Items.BED.red())
				.setName(Component.literal("Leave Lobby").withStyle(style -> style.withItalic(false).withColor(ChatFormatting.YELLOW)))
				.build();
		player.getInventory().add(8, leaveGame);

		Quakecraft.applySpeed(player);
	}

	/**
	 * Spawns a player in the waiting room.
	 *
	 * @param player the player to spawn
	 */
	public final void spawnWaitingPlayer(ServerPlayer player) {
		var bounds = this.map.waitingSpawn;
		var min = bounds.min();
		var max = bounds.max();

		double x = Mth.nextDouble(player.getRandom(), min.getX(), max.getX());
		double z = Mth.nextDouble(player.getRandom(), min.getZ(), max.getZ());
		double y = min.getY() + 0.5;

		player.teleportTo(this.world, x, y, z, Set.of(), 0.f, 0.f, false);
	}

	/**
	 * Represents a spawn cache.
	 *
	 * @version 1.0.1
	 * @since 1.0.1
	 */
	public class SpawnCache {
		private final int size;
		private int[] lastSpawns;

		public SpawnCache(int size) {
			this.size = size;
			this.lastSpawns = new int[this.size];

			for (int index = 0; index < this.size; index++) {
				this.lastSpawns[index] = -1;
			}
		}

		/**
		 * Returns whether the spawn index is in the last spawn cache or not.
		 *
		 * @param spawn the spawn index to check
		 * @return {@code true} if the spawn index is in the cache, else {@code false}
		 */
		public boolean contains(int spawn) {
			for (int cached : this.lastSpawns)
				if (spawn == cached)
					return true;
			return false;
		}

		/**
		 * Rolls the next spawn.
		 *
		 * @return the next spawn index
		 */
		public int rollNextSpawn() {
			int index = 0;
			if (map.getSpawnCount() > 1) {
				index = RANDOM.nextInt(map.getSpawnCount() - 1);

				int tries = 0;
				while (this.contains(index) && tries <= this.size) {
					index++;

					if (index >= map.getSpawnCount())
						index = 0;

					tries++;
				}
			}

			this.push(index);

			return index;
		}

		/**
		 * Pushes a new last spawn index.
		 *
		 * @param spawn the spawn index
		 */
		public void push(int spawn) {
			if (this.size - 1 >= 0) System.arraycopy(this.lastSpawns, 0, this.lastSpawns, 1, this.size - 1);

			this.lastSpawns[0] = spawn;
		}
	}
}
