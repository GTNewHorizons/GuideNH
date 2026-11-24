package com.hfstudio.guidenh.network.capture;

import java.util.UUID;

import com.hfstudio.guidenh.capture.server.api.CaptureServerArea;
import com.hfstudio.guidenh.capture.server.api.CaptureServerQuery;
import com.hfstudio.guidenh.capture.server.api.CaptureServerTarget;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public class CaptureWireCodec {

    public static void area(ByteBuf buffer, CaptureServerArea area) {
        buffer.writeBoolean(area != null);
        if (area == null) return;
        buffer.writeInt(area.dimension());
        buffer.writeInt(area.minX());
        buffer.writeInt(area.minY());
        buffer.writeInt(area.minZ());
        buffer.writeInt(area.maxX());
        buffer.writeInt(area.maxY());
        buffer.writeInt(area.maxZ());
    }

    public static CaptureServerArea area(ByteBuf buffer) {
        if (!buffer.readBoolean()) return null;
        return new CaptureServerArea(
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt());
    }

    private CaptureWireCodec() {}

    public static void uuid(ByteBuf buffer, UUID id) {
        buffer.writeLong(id.getMostSignificantBits());
        buffer.writeLong(id.getLeastSignificantBits());
    }

    public static UUID uuid(ByteBuf buffer) {
        return new UUID(buffer.readLong(), buffer.readLong());
    }

    public static void query(ByteBuf buffer, CaptureServerQuery query) {
        buffer.writeInt(query.radius());
        buffer.writeInt(query.maxTargets());
        buffer.writeBoolean(query.temporaryTravel());
    }

    public static CaptureServerQuery query(ByteBuf buffer) {
        return new CaptureServerQuery(buffer.readInt(), buffer.readInt(), buffer.readBoolean());
    }

    public static void target(ByteBuf buffer, CaptureServerTarget target) {
        ByteBufUtils.writeUTF8String(buffer, target.id());
        ByteBufUtils.writeUTF8String(buffer, target.providerId());
        buffer.writeInt(target.dimension());
        buffer.writeInt(target.x());
        buffer.writeInt(target.y());
        buffer.writeInt(target.z());
        ByteBufUtils.writeUTF8String(buffer, target.variant());
        ByteBufUtils.writeUTF8String(buffer, target.label());
        ByteBufUtils.writeUTF8String(buffer, target.availability());
        ByteBufUtils.writeUTF8String(buffer, target.reason());
    }

    public static CaptureServerTarget target(ByteBuf buffer) {
        return new CaptureServerTarget(
            ByteBufUtils.readUTF8String(buffer),
            ByteBufUtils.readUTF8String(buffer),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            ByteBufUtils.readUTF8String(buffer),
            ByteBufUtils.readUTF8String(buffer),
            ByteBufUtils.readUTF8String(buffer),
            ByteBufUtils.readUTF8String(buffer));
    }
}
