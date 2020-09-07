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

package me.lambdaurora.quakecraft.weapon;

import net.minecraft.item.Items;

public final class Weapons
{
    private Weapons()
    {
        throw new UnsupportedOperationException("Weapons only contains static definitions.");
    }

    public static final ShooterWeapon BASE_SHOOTER     = new ShooterWeapon(Items.STONE_HOE, 2 * 20 + 10, 4 * 20);
    public static final ShooterWeapon ADVANCED_SHOOTER = new ShooterWeapon(Items.IRON_HOE, 25, 4 * 20);

    public static final GrenadeWeapon BASE_GRENADE = new GrenadeWeapon(Items.SNOWBALL, 45);
}
