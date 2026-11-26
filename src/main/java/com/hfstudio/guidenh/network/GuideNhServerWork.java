package com.hfstudio.guidenh.network;

import net.minecraft.entity.player.EntityPlayerMP;

public interface GuideNhServerWork {

    boolean step(EntityPlayerMP player, long deadlineNanos);

    default void close() {}
}
