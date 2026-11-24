package com.hfstudio.guidenh.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import lombok.Getter;

@Getter
public class GuideNhClientBridgeMessage implements IMessage {

    public static final byte ACTION_IMPORT_STRUCTURE = 0;

    private byte action;
    private int x;
    private int y;
    private int z;

    public GuideNhClientBridgeMessage() {
        this((byte) 0, 0, 0, 0);
    }

    private GuideNhClientBridgeMessage(byte action, int x, int y, int z) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static GuideNhClientBridgeMessage importStructure(int x, int y, int z) {
        return new GuideNhClientBridgeMessage(ACTION_IMPORT_STRUCTURE, x, y, z);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf.readableBytes() < 15) {
            throw new IllegalArgumentException("Truncated legacy client bridge packet");
        }
        action = buf.readByte();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        // Preserve the legacy wire slot without retaining a server-supplied file path.
        int unusedLength = buf.readUnsignedShort();
        if (unusedLength > buf.readableBytes()) {
            throw new IllegalArgumentException("Truncated legacy client bridge payload");
        }
        buf.skipBytes(unusedLength);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeShort(0);
    }
}
