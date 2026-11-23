package com.hfstudio.guidenh.network.capture;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class CaptureCapabilityMessage implements IMessage {

    public static final int VERSION = 1;
    public static final int MAX_RADIUS = 256;
    public static final int MAX_TARGETS = 5000;
    private int version;
    private boolean integrated;

    public CaptureCapabilityMessage() {}

    public CaptureCapabilityMessage(boolean integrated) {
        version = VERSION;
        this.integrated = integrated;
    }

    public int version() {
        return version;
    }

    public boolean integrated() {
        return integrated;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        version = buffer.readInt();
        integrated = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(version);
        buffer.writeBoolean(integrated);
    }
}
