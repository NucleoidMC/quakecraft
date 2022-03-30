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

package dev.lambdaurora.quakecraft.weapon;

import dev.lambdaurora.quakecraft.Quakecraft;
import net.minecraft.item.Items;

public final class Weapons {
	private Weapons() {
		throw new UnsupportedOperationException("Weapons only contains static definitions.");
	}

	public static final ShooterWeapon BASE_SHOOTER = new ShooterWeapon(
			Quakecraft.id("base_railgun"),
			Items.STONE_HOE,
			new Weapon.Settings(2 * 20 + 10).secondaryCooldown(4 * 20)
	);
	public static final ShooterWeapon ADVANCED_SHOOTER = new ShooterWeapon(
			Quakecraft.id("advanced_railgun"),
			Items.IRON_HOE,
			new Weapon.Settings(25).secondaryCooldown(4 * 20)
	);

	public static final RocketLauncherWeapon ROCKET_LAUNCHER = new RocketLauncherWeapon(
			Quakecraft.id("rocket_launcher"),
			Items.IRON_AXE,
			new Weapon.Settings(35)
					.ammoSize(20)
					.clipSize(4)
	);

	public static final GrenadeWeapon GRENADE_LAUNCHER = new GrenadeWeapon(
			Quakecraft.id("grenade_launcher"),
			Items.BLAZE_ROD,
			new Weapon.Settings(45)
					.ammoSize(16)
					.clipSize(4)
	);
}
