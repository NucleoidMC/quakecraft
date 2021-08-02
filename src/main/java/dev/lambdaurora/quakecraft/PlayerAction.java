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

package dev.lambdaurora.quakecraft;

/**
 * Represents a player action.
 * <p>
 * This is used to predict if a hand swing is caused by the attack action or by the use action.
 *
 * @author LambdAurora
 * @version 1.2.2
 * @since 1.2.2
 */
public enum PlayerAction
{
    NONE,
    ATTACK,
    USE,
    USE_BLOCK,
    USE_BLOCK_AND_ITEM;

    public boolean isUse()
    {
        return this == USE || this == USE_BLOCK || this == USE_BLOCK_AND_ITEM;
    }
}
