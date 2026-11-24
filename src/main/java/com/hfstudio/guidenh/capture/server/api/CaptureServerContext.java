package com.hfstudio.guidenh.capture.server.api;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

public record CaptureServerContext(EntityPlayerMP player, WorldServer world, CaptureServerQuery query,
    boolean integrated) {}
