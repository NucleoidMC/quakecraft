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

package dev.lambdaurora.quakecraft.game;

import dev.lambdaurora.quakecraft.PlayerAction;
import dev.lambdaurora.quakecraft.Quakecraft;
import dev.lambdaurora.quakecraft.QuakecraftConstants;
import dev.lambdaurora.quakecraft.weapon.Weapons;
import dev.lambdaurora.quakecraft.weapon.inventory.WeaponManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;

import java.util.UUID;

/**
 * Represents a Quakecraft player.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.0.0
 */
public class QuakecraftPlayer implements Comparable<QuakecraftPlayer> {
	private final ServerWorld world;
	public final UUID uuid;
	public final String name;
	private final WeaponManager weapons = new WeaponManager();
	private ServerPlayerEntity player;
	private long respawnTime = -1;
	private PlayerAction lastAction = PlayerAction.NONE;
	private int kills = 0;
	private int killsWithinATick = 0;
	private GameTeam team;

	private boolean left = false;

	public QuakecraftPlayer(ServerPlayerEntity player, GameTeam team) {
		this.world = player.getServerWorld();
		this.uuid = player.getUuid();
		this.name = player.getNameForScoreboard();
		this.weapons.add(Weapons.ADVANCED_SHOOTER);
		this.weapons.add(Weapons.ROCKET_LAUNCHER);
		this.weapons.add(Weapons.GRENADE_LAUNCHER);
		this.player = player;
		this.team = team;
	}

	public int getKills() {
		return this.kills;
	}

	public void incrementKills() {
		this.killsWithinATick++;
	}

	public boolean hasWon() {
		return this.kills >= 24;
	}

	public @Nullable GameTeam getTeam() {
		return this.team;
	}

	public boolean hasTeam() {
		return this.team != null;
	}

	public void setTeam(@Nullable GameTeam team) {
		this.team = team;
	}

	public boolean hasLeft() {
		return this.left;
	}

	public void leave() {
		this.left = true;

		Quakecraft.removeSpeed(this.player);
	}

	/**
	 * Resets the player.
	 *
	 * @param player the player instance
	 */
	public void reset(ServerPlayerEntity player) {
		this.player = player;

		if (this.left) {
			this.player.changeGameMode(GameMode.SPECTATOR);
			return;
		}

		this.player.changeGameMode(GameMode.ADVENTURE);
		this.player.getInventory().clear();

		this.weapons.insertStacks(this.player);
		//this.syncInventory();

		this.player.setVelocity(0, 0, 0);

		Quakecraft.applySpeed(this.player);

		this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 60 * 60 * 20, 0, false, false));

		this.lastAction = PlayerAction.NONE;
	}

	public void tick(GameSpace world) {
		this.kills += this.killsWithinATick;
		if (this.killsWithinATick >= 2) {
			if (this.killsWithinATick <= 5) {
				world.getPlayers().sendMessage(Text.translatable("quakecraft.game.special.kills." + this.killsWithinATick,
						this.getDisplayName())
						.formatted(Formatting.RED, Formatting.BOLD));
			} else {
				world.getPlayers().sendMessage(Text.translatable("quakecraft.game.special.kills.lot", this.getDisplayName())
						.formatted(Formatting.RED, Formatting.BOLD));
			}
		}

		this.killsWithinATick = 0;

		this.weapons.tick();
		var heldWeapon = this.weapons.get(this.player.getMainHandStack());
		if (heldWeapon != null) {
			int secondaryCooldown = this.weapons.getSecondaryCooldown(heldWeapon);
			if (secondaryCooldown > 0) {
				var bar = "▊▊▊▊▊▊▊▊▊▊";
				int progress = (int) (secondaryCooldown / (double) heldWeapon.secondaryCooldown * bar.length());
				this.player.sendMessage(Text.literal("[").formatted(Formatting.GRAY)
						.append(Text.literal(bar.substring(progress)).formatted(Formatting.GREEN))
						.append(Text.literal(bar.substring(0, progress)).formatted(Formatting.RED))
						.append("]"), true);
			}
		}

		//this.syncInventory();
	}

	/**
	 * Synchronizes the player inventory.
	 */
	public void syncInventory() {
		this.player.currentScreenHandler.sendContentUpdates();
	}

	/**
	 * Fired when the game ends.
	 */
	public void onEnd() {
		this.player.getInventory().clear();
	}

	public void onDeath() {
		this.player.playSound(SoundEvents.ENTITY_BLAZE_DEATH, 2.f, 1.f);
		Quakecraft.spawnFirework(this.world, this.player.getX(), this.player.getY(), this.player.getZ(), new int[]{15435844, 11743532}, true, 0);
	}

	public void startRespawn(long time) {
		this.respawnTime = time + QuakecraftConstants.RESPAWN_TICKS;
	}

	boolean tryRespawn(long time) {
		if (this.respawnTime != -1 && time >= this.respawnTime) {
			this.respawnTime = -1;
			return true;
		}
		return false;
	}

	public int onItemUse(ServerWorld world, ServerPlayerEntity player, Hand hand) {
		this.lastAction = PlayerAction.USE;

		return this.weapons.onPrimary(world, player, hand);
	}

	public void onSecondary(ServerWorld world) {
		this.weapons.onSecondary(world, this.player);
	}

	public void onSwingHand(ServerWorld world) {
		// Mmmhh yes attack prediction, really not fun to implement.
		if (this.lastAction.isUse()) {
			this.lastAction = PlayerAction.NONE;
			return;
		}

		this.onSecondary(world);
	}

	public PlayerAction getLastAction() {
		return this.lastAction;
	}

	void setLastAction(PlayerAction action) {
		this.lastAction = action;
	}

	/**
	 * Returns the display name of the player.
	 *
	 * @return the display name
	 */
	public Text getDisplayName() {
		if (this.player != null)
			return this.player.getDisplayName();

		return Text.literal(this.name);
	}

	public @Nullable ServerPlayerEntity getPlayer() {
		return this.player;
	}

	@Override
	public int compareTo(QuakecraftPlayer other) {
		return Integer.compare(this.getKills(), other.getKills());
	}
}
