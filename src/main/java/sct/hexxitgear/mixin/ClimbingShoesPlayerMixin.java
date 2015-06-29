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

package sct.hexxitgear.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sct.hexxitgear.HexxitGear;
import sct.hexxitgear.mixinsupport.climbing.ClimbingHelper;
import sct.hexxitgear.mixinsupport.climbing.IClimbingShoesWearer;
import sct.hexxitgear.mixinsupport.climbing.ShoesHelper;
import sct.hexxitgear.mixinsupport.climbing.VectorTransformer;
import sct.hexxitgear.net.HexxitGearNetwork;
import sct.hexxitgear.net.packets.PolarityPacket;

import java.util.LinkedList;

@Mixin(EntityPlayer.class)
public abstract class ClimbingShoesPlayerMixin extends EntityLivingBase implements IClimbingShoesWearer {

    public ClimbingShoesPlayerMixin(World world) {
        super(world);
    }

    private ForgeDirection climbingShoesDirection;
    private ForgeDirection newClimbingShoesDirection;
    private boolean willClimbingShoesBeEquipped;
    private boolean areClimbingShoesEquipped;
    private VectorTransformer transformer;
    private boolean updateInProgress = false;
    private int parkourDistance;
    private static final int MAX_PARKOUR_DISTANCE = 600;

    private LinkedList<ForgeDirection> collidedSides = new LinkedList<ForgeDirection>();

    @Shadow
    public PlayerCapabilities capabilities;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructed(World world, GameProfile profile, CallbackInfo info) {
        this.climbingShoesDirection = ForgeDirection.DOWN;
        this.newClimbingShoesDirection = ForgeDirection.DOWN;
        this.areClimbingShoesEquipped = false;
        this.willClimbingShoesBeEquipped = false;
        this.transformer = new VectorTransformer(climbingShoesDirection);
        this.parkourDistance = MAX_PARKOUR_DISTANCE;
    }

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void transformPlayerPositioning(CallbackInfo info) {
        setUpdating(true);
        if (!areClimbingShoesEquipped())
            return;

        ClimbingHelper.transformEntity(this, getTransformer());
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void untransformPlayerPositioning(CallbackInfo info) {
        Vec3 lookDir = this.getLook(1.0f);
        if (areClimbingShoesEquipped()) {
            ClimbingHelper.untransformEntity(this, getTransformer());

            if (this.worldObj.isRemote)
                ShoesHelper.processShoes((EntityPlayer) (Object) this, collidedSides, lookDir);
        }

        setUpdating(false);
        collidedSides.clear();

        this.areClimbingShoesEquipped = willClimbingShoesBeEquipped && !this.capabilities.isFlying;
        updateDirection();
    }

    @Override
    public void moveEntity(double motionX, double motionY, double motionZ) {
        double savedMotionX = motionX;
        double savedMotionZ = motionZ;
        double savedPosX = this.posX;
        double savedPosZ = this.posZ;

        super.moveEntity(motionX, motionY, motionZ);

        if (areClimbingShoesEquipped()) {
            if (this.isCollidedHorizontally && this.onGround && this.worldObj.isRemote) {
                double remainingMotionX = savedMotionX - (this.posX - savedPosX);
                double remainingMotionZ = savedMotionZ - (this.posZ - savedPosZ);

                if (ShoesHelper.processStep((EntityPlayer) (Object) this, remainingMotionX, remainingMotionZ))
                    collidedSides.clear();
            }
        }
    }

    @Override
    public VectorTransformer getTransformer() {
        return transformer;
    }

    @Override
    public boolean areClimbingShoesEquipped() {
        return areClimbingShoesEquipped && updateInProgress;
    }

    @Override
    public void setClimbingShoesEquipped(boolean equipped) {
        if (willClimbingShoesBeEquipped != equipped) {
            this.willClimbingShoesBeEquipped = equipped;
            this.newClimbingShoesDirection = ForgeDirection.DOWN;
        }
    }

    @Override
    public void setUpdating(boolean updating) {
        this.updateInProgress = updating;
    }

    @Override
    public boolean isUpdating() {
        return this.updateInProgress;
    }

    @Override
    public void setFloor(ForgeDirection direction) {
        if (newClimbingShoesDirection != direction) {

            this.newClimbingShoesDirection = direction;
            if (!updateInProgress)
                updateDirection();

            EntityPlayer player = (EntityPlayer) (Object) this;
            if (!this.worldObj.isRemote) {
                HexxitGearNetwork.sendToNearbyPlayers(new PolarityPacket(direction), player.worldObj.provider.dimensionId, player.posX, player.posY, player.posZ, 64.0D);
            } else {
                if (HexxitGear.proxy.isClientPlayer(player))
                    HexxitGearNetwork.sendToServer(new PolarityPacket(direction));
            }
        }
    }

    private void updateDirection() {
        ForgeDirection newDirection = areClimbingShoesEquipped ? newClimbingShoesDirection : ForgeDirection.DOWN;
        transformer = new VectorTransformer(newDirection);
        if (climbingShoesDirection != newDirection) {
            ClimbingHelper.rotateEntityBBFromTo(this, climbingShoesDirection, newClimbingShoesDirection);
        }
        this.climbingShoesDirection = newDirection;
        this.newClimbingShoesDirection = newDirection;
    }

    @Override
    public void spendDistance(int distance) {
        parkourDistance -= distance;
        if (parkourDistance <= 0) {
            setFloor(ForgeDirection.DOWN);
        }
    }

    @Override
    public void resetDistance() {
        parkourDistance = MAX_PARKOUR_DISTANCE;
    }

    @Override
    public void collideWithSide(ForgeDirection direction) {
        collidedSides.add(direction);
    }
}
