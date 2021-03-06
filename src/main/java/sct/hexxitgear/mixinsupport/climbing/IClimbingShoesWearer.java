/*
 * HexxitGear
 * Copyright (C) 2013  Ryan Cohen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package sct.hexxitgear.mixinsupport.climbing;

import net.minecraftforge.common.util.ForgeDirection;

public interface IClimbingShoesWearer {
    void setClimbingShoesEquipped(boolean equipped);

    boolean isUpdating();

    void setUpdating(boolean updating);

    boolean areClimbingShoesEquipped();

    VectorTransformer getTransformer();

    void setFloor(ForgeDirection floor);

    void spendDistance(int distance);

    void resetDistance();

    void collideWithSide(ForgeDirection side);
}
