package com.hfstudio.guidenh.network.capture;

import com.hfstudio.guidenh.capture.server.api.CaptureServerQuery;
import com.hfstudio.guidenh.capture.server.api.CaptureServerRequest;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class CaptureRequestMessage implements IMessage {

    private CaptureServerRequest request;

    public CaptureRequestMessage() {}

    public CaptureRequestMessage(CaptureServerRequest request) {
        this.request = request;
    }

    public CaptureServerRequest request() {
        return request;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        request = new CaptureServerRequest(
            CaptureWireCodec.uuid(buffer),
            CaptureWireCodec.uuid(buffer),
            ByteBufUtils.readUTF8String(buffer),
            ByteBufUtils.readUTF8String(buffer),
            CaptureWireCodec.query(buffer));
        if (request.action()
            .equals("catalog") && buffer.isReadable()) {
            CaptureServerQuery query = request.query();
            request = new CaptureServerRequest(
                request.id(),
                request.catalogId(),
                request.action(),
                request.targetId(),
                new CaptureServerQuery(
                    query.radius(),
                    query.maxTargets(),
                    query.temporaryTravel(),
                    CaptureWireCodec.area(buffer)));
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        CaptureWireCodec.uuid(buffer, request.id());
        CaptureWireCodec.uuid(buffer, request.catalogId());
        ByteBufUtils.writeUTF8String(buffer, request.action());
        ByteBufUtils.writeUTF8String(buffer, request.targetId());
        CaptureWireCodec.query(buffer, request.query());
        if (request.action()
            .equals("catalog"))
            CaptureWireCodec.area(
                buffer,
                request.query()
                    .area());
    }
}
