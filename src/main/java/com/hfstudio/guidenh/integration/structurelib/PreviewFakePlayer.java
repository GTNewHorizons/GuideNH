package com.hfstudio.guidenh.integration.structurelib;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.mojang.authlib.GameProfile;

class PreviewFakePlayer extends EntityPlayer {

    static final int CONTROLLER_X = 0;
    static final int CONTROLLER_Y = 64;
    static final int CONTROLLER_Z = 0;

    PreviewFakePlayer(World world) {
        super(world, new GameProfile(UUID.fromString("9c7ef542-6ab6-4524-b7d7-8caaf8df467c"), "GuideNHPreview"));
        capabilities.isCreativeMode = true;
        noClip = true;
        configureForControllerFacing(ForgeDirection.SOUTH);
    }

    void configureForControllerFacing(ForgeDirection controllerFacing) {
        ForgeDirection facing = controllerFacing != null && controllerFacing != ForgeDirection.UNKNOWN
            ? controllerFacing
            : ForgeDirection.SOUTH;
        double x = CONTROLLER_X + 0.5D + facing.offsetX * 4.0D;
        double y = CONTROLLER_Y + 1.62D;
        double z = CONTROLLER_Z + 0.5D + facing.offsetZ * 4.0D;
        float yaw = yawForFacing(facing);
        prevPosX = lastTickPosX = posX = x;
        prevPosY = lastTickPosY = posY = y;
        prevPosZ = lastTickPosZ = posZ = z;
        prevRotationYaw = rotationYaw = yaw;
        prevRotationPitch = rotationPitch = 0.0F;
        setPositionAndRotation(x, y, z, yaw, 0.0F);
    }

    private static float yawForFacing(ForgeDirection facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    @Override
    public void addChatMessage(IChatComponent message) {}

    @Override
    public boolean canCommandSenderUseCommand(int i, String s) {
        return false;
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates() {
        return new ChunkCoordinates(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z);
    }

    @Override
    public void openGui(Object mod, int modGuiId, World world, int x, int y, int z) {}

    @Override
    public boolean isEntityInvulnerable() {
        return true;
    }

    @Override
    public boolean canAttackPlayer(EntityPlayer player) {
        return false;
    }

    @Override
    public void onDeath(DamageSource source) {}

    @Override
    public void onUpdate() {}

    @Override
    public void travelToDimension(int dim) {}
}
