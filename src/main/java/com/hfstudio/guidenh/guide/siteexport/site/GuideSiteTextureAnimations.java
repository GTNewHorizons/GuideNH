package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.data.AnimationMetadataSection;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorTextureAtlasSprite;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorTextureMap;

import guideme.flatbuffers.scene.ExpAnimatedTexturePart;
import guideme.flatbuffers.scene.ExpAnimatedTexturePartFrame;

public class GuideSiteTextureAnimations implements AutoCloseable {

    private static final int GRID_SIZE = 64;
    private static final long MAX_COMBINED_PERIOD = 1_000_000L;
    private static final int MAX_RENDERED_FRAMES = 256;
    private static Capture active;
    private final GuideSiteAssetRegistry assets;
    private final Map<Integer, Map<Integer, List<Sprite>>> atlasCells = new HashMap<>();
    private final Map<TextureAtlasSprite, Map<Integer, String>> framePaths = new IdentityHashMap<>();
    private final AtomicLong exportCount = new AtomicLong();
    private final AtomicLong animatedExportCount = new AtomicLong();
    private final AtomicLong renderedFrameCount = new AtomicLong();
    private final AtomicLong fallbackCount = new AtomicLong();
    private final AtomicLong renderNanos = new AtomicLong();
    private final AtomicLong captureCallCount = new AtomicLong();
    private final AtomicLong capturedVertexCount = new AtomicLong();
    private final AtomicLong matchedSpriteCount = new AtomicLong();
    private final AtomicLong animationCacheHitCount = new AtomicLong();
    private final Map<AnimationCacheKey, String> renderedAnimationCache = new HashMap<>();
    private boolean hasAnimatedSprites;
    private boolean indexed;

    public GuideSiteTextureAnimations(GuideSiteAssetRegistry assets) {
        this.assets = assets;
    }

    public record Sprite(TextureAtlasSprite texture, int atlasId, int atlasWidth, int atlasHeight,
        List<Integer> indices, List<Integer> durations, long period) {

        public Sprite {
            if (texture == null || indices == null
                || durations == null
                || indices.isEmpty()
                || indices.size() != durations.size()
                || period <= 0) {
                throw new IllegalArgumentException("Animated sprite metadata is invalid");
            }
        }

        public int frameAt(long tick) {
            long phase = tick % period;
            for (int index = 0; index < durations.size(); index++) {
                if (phase < durations.get(index)) return indices.get(index);
                phase -= durations.get(index);
            }
            return indices.getFirst();
        }

        public int ticksUntilChange(long tick) {
            long phase = tick % period;
            for (int duration : durations) {
                if (phase < duration) return (int) (duration - phase);
                phase -= duration;
            }
            return durations.getFirst();
        }
    }

    public class Capture implements AutoCloseable {

        private final GuideSiteTextureAnimations owner = GuideSiteTextureAnimations.this;
        private final Capture previous;
        private final Map<Sprite, Boolean> seen = new IdentityHashMap<>();
        private final List<Sprite> used = new ArrayList<>();

        private Capture() {
            previous = active;
            active = this;
        }

        public List<Sprite> sprites() {
            return List.copyOf(used);
        }

        @Override
        public void close() {
            if (active != this) throw new IllegalStateException("Texture animation captures must close in order");
            active = previous;
        }

        private void collect(int[] vertices, int vertexCount) {
            captureCallCount.incrementAndGet();
            int unit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            if (unit != OpenGlHelper.defaultTexUnit) OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            try {
                Map<Integer, List<Sprite>> cells = atlasCells.get(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
                if (cells == null) return;
                int count = Math.min(vertexCount, vertices.length / 8);
                capturedVertexCount.addAndGet(count);
                for (int vertex = 0; vertex < count; vertex++) {
                    float u = Float.intBitsToFloat(vertices[vertex * 8 + 3]);
                    float v = Float.intBitsToFloat(vertices[vertex * 8 + 4]);
                    if (!Float.isFinite(u) || !Float.isFinite(v) || u < 0 || v < 0 || u >= 1 || v >= 1) continue;
                    List<Sprite> candidates = cells.get(cell(u, v));
                    if (candidates == null) continue;
                    for (Sprite sprite : candidates) {
                        TextureAtlasSprite texture = sprite.texture();
                        if (u >= texture.getMinU() - 0.000001f && u <= texture.getMaxU() + 0.000001f
                            && v >= texture.getMinV() - 0.000001f
                            && v <= texture.getMaxV() + 0.000001f) {
                            if (seen.put(sprite, Boolean.TRUE) == null) {
                                used.add(sprite);
                                matchedSpriteCount.incrementAndGet();
                            }
                        }
                    }
                }
            } finally {
                if (unit != OpenGlHelper.defaultTexUnit) OpenGlHelper.setActiveTexture(unit);
            }
        }
    }

    public Capture beginCapture() {
        if (!indexed) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null) throw new IllegalStateException("Minecraft client is not ready for texture export");
            index(
                minecraft.getTextureManager()
                    .getTexture(TextureMap.locationBlocksTexture));
            index(
                minecraft.getTextureManager()
                    .getTexture(TextureMap.locationItemsTexture));
            indexed = true;
        }
        return new Capture();
    }

    public static void captureVertices(int[] vertices, int vertexCount, boolean textured) {
        if (active != null && active.owner.hasAnimatedSprites && textured && vertices != null && vertexCount > 0) {
            active.collect(vertices, vertexCount);
        }
    }

    private static int cell(float u, float v) {
        return Math.min(GRID_SIZE - 1, (int) (u * GRID_SIZE))
            + Math.min(GRID_SIZE - 1, (int) (v * GRID_SIZE)) * GRID_SIZE;
    }

    private void index(ITextureObject object) {
        if (!(object instanceof TextureMap atlas) || !(atlas instanceof AccessorTextureMap accessor)) return;
        int unit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlas.getGlTextureId());
            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (width <= 0 || height <= 0) return;
            Map<Integer, List<Sprite>> cells = new HashMap<>();
            for (TextureAtlasSprite texture : accessor.guidenh$getAnimatedSprites()) {
                if (texture.getFrameCount() < 2 || !(texture instanceof AccessorTextureAtlasSprite spriteAccessor))
                    continue;
                AnimationMetadataSection metadata = spriteAccessor.guidenh$getAnimationMetadata();
                int frameCount = metadata != null && metadata.getFrameCount() > 0 ? metadata.getFrameCount()
                    : texture.getFrameCount();
                List<Integer> indices = new ArrayList<>(frameCount);
                List<Integer> durations = new ArrayList<>(frameCount);
                long period = 0;
                for (int frame = 0; frame < frameCount; frame++) {
                    int index = metadata != null && metadata.getFrameCount() > 0 ? metadata.getFrameIndex(frame)
                        : frame;
                    int duration = metadata == null ? 1
                        : Math.max(
                            1,
                            metadata.getFrameCount() > 0 ? metadata.getFrameTimeSingle(frame)
                                : metadata.getFrameTime());
                    if (index < 0 || index >= texture.getFrameCount() || texture.getFrameTextureData(index) == null)
                        continue;
                    indices.add(index);
                    durations.add(duration);
                    period += duration;
                }
                if (indices.size() < 2) continue;
                Sprite sprite = new Sprite(
                    texture,
                    atlas.getGlTextureId(),
                    width,
                    height,
                    List.copyOf(indices),
                    List.copyOf(durations),
                    period);
                int minX = (int) (texture.getMinU() * GRID_SIZE);
                int maxX = Math.min(GRID_SIZE - 1, (int) (texture.getMaxU() * GRID_SIZE));
                int minY = (int) (texture.getMinV() * GRID_SIZE);
                int maxY = Math.min(GRID_SIZE - 1, (int) (texture.getMaxV() * GRID_SIZE));
                for (int y = minY; y <= maxY; y++) {
                    for (int x = minX; x <= maxX; x++)
                        cells.computeIfAbsent(x + y * GRID_SIZE, key -> new ArrayList<>())
                            .add(sprite);
                }
            }
            atlasCells.put(atlas.getGlTextureId(), cells);
            if (!cells.isEmpty()) {
                hasAnimatedSprites = true;
            }
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, binding);
            OpenGlHelper.setActiveTexture(unit);
        }
    }

    public String exportRendered(String bucket, Supplier<BufferedImage> renderer) throws Exception {
        exportCount.incrementAndGet();
        BufferedImage first;
        List<Sprite> sprites;
        try (Capture capture = beginCapture()) {
            first = renderer.get();
            sprites = capture.sprites();
        }
        if (sprites.isEmpty()) return assets.writePngAsync(bucket, first);
        AnimationCacheKey cacheKey = new AnimationCacheKey(bucket, assets.imageHash(first), List.copyOf(sprites));
        String cachedPath = renderedAnimationCache.get(cacheKey);
        if (cachedPath != null) {
            animationCacheHitCount.incrementAndGet();
            return cachedPath;
        }
        animatedExportCount.incrementAndGet();
        long period = 1;
        for (Sprite sprite : sprites) {
            long divisor = gcd(period, sprite.period());
            long multiplier = sprite.period() / divisor;
            if (period > MAX_COMBINED_PERIOD / multiplier) {
                GuideDebugLog.warnAlways(
                    "[GuideNH] [TextureExport] Combined animation cycle exceeds {} ticks; exporting the current frame for {} sprites",
                    MAX_COMBINED_PERIOD,
                    sprites.size());
                return assets.writePngAsync(bucket, first);
            }
            period *= multiplier;
        }
        int estimatedFrames = estimateFrameCount(period, sprites);
        if (period > MAX_COMBINED_PERIOD || estimatedFrames > MAX_RENDERED_FRAMES) {
            fallbackCount.incrementAndGet();
            GuideDebugLog.warnAlways(
                "[GuideNH] [TextureExport] Animation sampling is bounded: period={}, estimatedFrames={}, limit={}, sprites={}; exporting the current frame",
                period,
                estimatedFrames,
                MAX_RENDERED_FRAMES,
                sprites.size());
            return assets.writePngAsync(bucket, first);
        }
        if (period > 1200) GuideDebugLog.warnAlways(
            "[GuideNH] [TextureExport] Raster animation has a long combined cycle: {} ticks, {} sprites",
            period,
            sprites.size());
        List<GuideSiteAnimatedPng.Frame> frames = new ArrayList<>();
        Map<Sprite, FrameState> savedFrames = new LinkedHashMap<>();
        Map<Sprite, Integer> uploadedFrames = new IdentityHashMap<>();
        for (Sprite sprite : sprites) {
            AccessorTextureAtlasSprite accessor = (AccessorTextureAtlasSprite) sprite.texture();
            int counter = accessor.guidenh$getFrameCounter();
            savedFrames.put(
                sprite,
                new FrameState(
                    sprite.indices()
                        .get(
                            Math.floorMod(
                                counter,
                                sprite.indices()
                                    .size())),
                    counter,
                    accessor.guidenh$getTickCounter()));
        }
        try {
            for (long tick = 0; tick < period;) {
                long duration = Math.min(65535, period - tick);
                for (Sprite sprite : sprites) {
                    int frame = sprite.frameAt(tick);
                    if (!Integer.valueOf(frame)
                        .equals(uploadedFrames.put(sprite, frame))) {
                        upload(sprite, frame);
                    }
                    duration = Math.min(duration, sprite.ticksUntilChange(tick));
                }
                long renderStartedAt = System.nanoTime();
                frames.add(new GuideSiteAnimatedPng.Frame(renderer.get(), (int) duration));
                renderNanos.addAndGet(System.nanoTime() - renderStartedAt);
                renderedFrameCount.incrementAndGet();
                tick += duration;
            }
        } finally {
            for (var saved : savedFrames.entrySet()) {
                upload(
                    saved.getKey(),
                    saved.getValue()
                        .textureFrame());
                AccessorTextureAtlasSprite accessor = (AccessorTextureAtlasSprite) saved.getKey()
                    .texture();
                accessor.guidenh$setFrameCounter(
                    saved.getValue()
                        .frameCounter());
                accessor.guidenh$setTickCounter(
                    saved.getValue()
                        .tickCounter());
            }
        }
        String exportedPath = assets.writeAnimatedPngAsync(bucket, frames);
        renderedAnimationCache.put(cacheKey, exportedPath);
        return exportedPath;
    }

    private int estimateFrameCount(long period, List<Sprite> sprites) {
        int frames = 0;
        for (long tick = 0; tick < period && frames <= MAX_RENDERED_FRAMES;) {
            long duration = Math.min(65535, period - tick);
            for (Sprite sprite : sprites) {
                duration = Math.min(duration, sprite.ticksUntilChange(tick));
            }
            tick += Math.max(1, duration);
            frames++;
        }
        return frames;
    }

    @Override
    public void close() {
        GuideDebugLog.infoAlways(
            "[GuideNH] [GuideSiteTextureAnimations] exports={}, animated={}, renderedFrames={}, fallbacks={}, renderTimeMs={}, captureCalls={}, capturedVertices={}, matchedSprites={}, cacheHits={}",
            exportCount.get(),
            animatedExportCount.get(),
            renderedFrameCount.get(),
            fallbackCount.get(),
            TimeUnit.NANOSECONDS.toMillis(renderNanos.get()),
            captureCallCount.get(),
            capturedVertexCount.get(),
            matchedSpriteCount.get(),
            animationCacheHitCount.get());
    }

    private record FrameState(int textureFrame, int frameCounter, int tickCounter) {}

    private record AnimationCacheKey(String bucket, String firstImageHash, List<Sprite> sprites) {}

    private static long gcd(long a, long b) {
        while (b != 0) {
            long remainder = a % b;
            a = b;
            b = remainder;
        }
        return a;
    }

    private void upload(Sprite sprite, int frame) {
        int unit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.atlasId());
            int minFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
            int magFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
            TextureAtlasSprite texture = sprite.texture();
            try {
                TextureUtil.uploadTextureMipmap(
                    texture.getFrameTextureData(frame),
                    texture.getIconWidth(),
                    texture.getIconHeight(),
                    texture.getOriginX(),
                    texture.getOriginY(),
                    false,
                    false);
            } finally {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter);
            }
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, binding);
            OpenGlHelper.setActiveTexture(unit);
        }
    }

    public List<Integer> exportSceneParts(FlatBufferBuilder builder,
        List<GuideSiteSceneTessellatorCapture.ExportedTexture> textures, List<Sprite> sprites) throws Exception {
        List<Integer> parts = new ArrayList<>();
        for (Sprite sprite : sprites) {
            for (var texture : textures) {
                if (!texture.textureId.equals("gltex-" + sprite.atlasId())) continue;
                parts.add(writeSpritePart(builder, texture.textureId, sprite, texture.mipLevel));
            }
        }
        return parts;
    }

    private int writeSpritePart(FlatBufferBuilder builder, String textureId, Sprite sprite, int mip) throws Exception {
        List<Integer> uniqueFrames = sprite.indices()
            .stream()
            .distinct()
            .toList();
        int width = Math.max(
            1,
            sprite.texture()
                .getIconWidth() >> mip);
        int height = Math.max(
            1,
            sprite.texture()
                .getIconHeight() >> mip);
        Map<Integer, String> paths = framePaths.computeIfAbsent(sprite.texture(), key -> new HashMap<>());
        String path = paths.get(mip);
        if (path == null) {
            BufferedImage sheet = new BufferedImage(
                width,
                Math.multiplyExact(height, uniqueFrames.size()),
                BufferedImage.TYPE_INT_ARGB);
            for (int frame = 0; frame < uniqueFrames.size(); frame++) {
                int[][] levels = sprite.texture()
                    .getFrameTextureData(uniqueFrames.get(frame));
                int[] pixels = levels.length > mip ? levels[mip] : null;
                if (pixels == null) throw new IllegalStateException(
                    "Animated sprite is missing mip level " + mip
                        + ": "
                        + sprite.texture()
                            .getIconName());
                sheet.setRGB(0, frame * height, width, height, pixels, 0, width);
            }
            path = assets.writePngAsync("texture-frames", sheet);
            paths.put(mip, path);
        }
        int idOffset = builder.createString(textureId);
        int pathOffset = builder.createString(path);
        ExpAnimatedTexturePart.startFramesVector(
            builder,
            sprite.indices()
                .size());
        for (int frame = sprite.indices()
            .size() - 1; frame >= 0; frame--) {
            ExpAnimatedTexturePartFrame.createExpAnimatedTexturePartFrame(
                builder,
                uniqueFrames.indexOf(
                    sprite.indices()
                        .get(frame)),
                sprite.durations()
                    .get(frame));
        }
        int framesOffset = builder.endVector();
        return ExpAnimatedTexturePart.createExpAnimatedTexturePart(
            builder,
            idOffset,
            sprite.texture()
                .getOriginX() >> mip,
            sprite.texture()
                .getOriginY() >> mip,
            width,
            height,
            pathOffset,
            uniqueFrames.size(),
            1,
            framesOffset);
    }
}
