package com.hfstudio.guidenh.network.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hfstudio.guidenh.capture.server.api.CaptureServerReply;
import com.hfstudio.guidenh.capture.server.api.CaptureServerTarget;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class CaptureReplyMessage implements IMessage {

    private CaptureServerReply reply;

    public CaptureReplyMessage() {}

    public CaptureReplyMessage(CaptureServerReply reply) {
        this.reply = reply;
    }

    public CaptureServerReply reply() {
        return reply;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        UUID id = CaptureWireCodec.uuid(buffer);
        String status = ByteBufUtils.readUTF8String(buffer);
        String reason = ByteBufUtils.readUTF8String(buffer);
        int window = buffer.readInt();
        boolean complete = buffer.readBoolean();
        int count = buffer.readUnsignedShort();
        if (count > 128) throw new IllegalArgumentException("Capture reply exceeds the catalog page size");
        List<CaptureServerTarget> targets = new ArrayList<>(count);
        for (int index = 0; index < count; index++) targets.add(CaptureWireCodec.target(buffer));
        reply = new CaptureServerReply(id, status, reason, window, targets, complete);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        CaptureWireCodec.uuid(buffer, reply.id());
        ByteBufUtils.writeUTF8String(buffer, reply.status());
        ByteBufUtils.writeUTF8String(buffer, reply.reason());
        buffer.writeInt(reply.windowId());
        buffer.writeBoolean(reply.complete());
        buffer.writeShort(
            reply.targets()
                .size());
        for (CaptureServerTarget target : reply.targets()) CaptureWireCodec.target(buffer, target);
    }
}
