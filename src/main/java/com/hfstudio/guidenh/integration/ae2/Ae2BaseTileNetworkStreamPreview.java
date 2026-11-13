package com.hfstudio.guidenh.integration.ae2;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementNbt;

import appeng.tile.AEBaseTile;
import appeng.tile.networking.TileCableBus;
import cpw.mods.fml.common.Optional;

/**
 * Server AEBaseTile description stream using the native metadata-64 packet and NBT {@code X} payload.
 * Subclass NETWORK_WRITE handlers define its contents. TileCableBus uses the separate multipart supplement
 * instead.
 */
public class Ae2BaseTileNetworkStreamPreview {

    /**
     * Stored under {@link ServerPreviewSupplementNbt#TAG_ROOT}.
     */
    public static final String SUPPLEMENT_ID = "guidenh.ae2.ae_base_tile_network";

    /**
     * Max {@code X} bytes per tile in multiplayer batch payloads ( ushort length on wire ).
     */
    public static final int MAX_X_PAYLOAD_BYTES = 16384;

    private static final int AE_TILE_UPDATE_META = 64;

    private Ae2BaseTileNetworkStreamPreview() {}

    public static String supplementId() {
        return SUPPLEMENT_ID;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static boolean eligible(TileEntity te) {
        return te instanceof AEBaseTile && !(te instanceof TileCableBus);
    }

    /**
     * Extracts AE2 {@code X} bytes from {@link AEBaseTile#getDescriptionPacket()} (authority context: resolved server
     * TE
     * or handler-side world TE).
     */
    @Optional.Method(modid = "appliedenergistics2")
    public static byte @Nullable [] captureAuthoritativeXPayload(@Nullable AEBaseTile tile) {
        if (tile == null) {
            return null;
        }
        try {
            Packet packet = tile.getDescriptionPacket();
            if (!(packet instanceof S35PacketUpdateTileEntity pkt)) {
                return null;
            }
            if (pkt.func_148853_f() != AE_TILE_UPDATE_META) {
                return null;
            }
            NBTTagCompound compound = pkt.func_148857_g();
            if (compound == null || !compound.hasKey("X")) {
                return null;
            }
            byte[] xBytes = compound.getByteArray("X");
            byte[] out = xBytes != null && xBytes.length > 0 ? xBytes.clone() : null;
            return out;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Applies authoritative {@code X} to a preview-place {@link AEBaseTile}, or {@code false} when no-op / failure.
     */
    @Optional.Method(modid = "appliedenergistics2")
    public static boolean applyAuthorityToPreviewTile(AEBaseTile previewTile, byte @Nullable [] xPayload) {
        if (xPayload == null || xPayload.length == 0) {
            return false;
        }
        try {
            NBTTagCompound data = new NBTTagCompound();
            data.setByteArray("X", xPayload);
            S35PacketUpdateTileEntity pkt = new S35PacketUpdateTileEntity(
                previewTile.xCoord,
                previewTile.yCoord,
                previewTile.zCoord,
                AE_TILE_UPDATE_META,
                data);
            previewTile.onDataPacket(null, pkt);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
