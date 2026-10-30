package com.hfstudio.guidenh.guide.scene.outline;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;

import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Glowing-effect style outlines: targets are rasterized into a style/visibility mask, a separable
 * distance search grows the contour outside the silhouette, and the result is alpha-blended over the
 * scene. Every pass is clipped to the screen rectangle the targets can reach, so the cost follows the
 * outlined area instead of the scene size.
 */
final class ScreenSpaceOutlineBackend implements OutlineRenderBackend {

    private static final int STYLE_SLOTS = 256;
    private static final int STYLE_COLUMNS = 4;
    private static final int COLUMN_VISIBLE = 0;
    private static final int COLUMN_OCCLUDED = 1;
    private static final int COLUMN_THICKNESS = 2;
    private static final float THICKNESS_SCALE = 8f;
    private static final int MAX_RADIUS = 32;
    private static final int SIZE_STEP = 64;
    private static final int AUX_TEXTURE_UNIT = 3;
    private static final int MAX_DRAINED_ERRORS = 16;
    private static final float MIN_CLIP_W = 1.0e-4f;

    private final IntBuffer intScratch = BufferUtils.createIntBuffer(16);
    private final ByteBuffer styleData = BufferUtils.createByteBuffer(STYLE_SLOTS * STYLE_COLUMNS * 4);
    private final Vector4f corner = new Vector4f();
    private final Runnable maskState = this::applyMaskState;

    private OutlineShaderProgram maskProgram;
    private OutlineShaderProgram spanProgram;
    private OutlineShaderProgram compositeProgram;
    private int styleTexture;
    private int maskTexture;
    private int spanTexture;
    private int depthTexture;
    private int maskFramebuffer;
    private int spanFramebuffer;
    private int width;
    private int height;
    private boolean depthCopyProbed;
    private boolean depthCopySupported = true;

    static boolean isSupported() {
        ContextCapabilities capabilities = GLContext.getCapabilities();
        return OpenGlHelper.framebufferSupported && capabilities.OpenGL20 && capabilities.OpenGL14;
    }

    @Override
    public void render(GuidebookLevel level, OutlineFrame frame, OutlineTargetDrawer drawer, Matrix4fc viewProjection,
        float partialTicks) {
        IntBuffer ints = intScratch;
        ints.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, ints);
        int viewportX = ints.get(0);
        int viewportY = ints.get(1);
        int viewportWidth = ints.get(2);
        int viewportHeight = ints.get(3);
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return;
        }
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        ints.clear();
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, ints);

        Minecraft minecraft = Minecraft.getMinecraft();
        PixelRect visible = new PixelRect(
            Math.max(viewportX, 0),
            Math.max(viewportY, 0),
            Math.min(viewportX + viewportWidth, minecraft.displayWidth),
            Math.min(viewportY + viewportHeight, minecraft.displayHeight));
        if (scissorEnabled) {
            visible = visible.intersect(
                new PixelRect(ints.get(0), ints.get(1), ints.get(0) + ints.get(2), ints.get(1) + ints.get(3)));
        }
        // From here on rectangles are viewport-relative, the pixel space of the offscreen targets.
        visible = visible.offset(-viewportX, -viewportY);
        int radius = Math.min(MAX_RADIUS, (int) Math.ceil(frame.maxThickness() * DisplayScale.scaleFactor()) + 1);
        PixelRect mask = projectBounds(frame, viewProjection, viewportWidth, viewportHeight)
            .intersect(visible.expand(radius, radius));
        if (mask.isEmpty()) {
            return;
        }
        PixelRect composite = mask.expand(radius, radius)
            .intersect(visible);
        if (composite.isEmpty()) {
            return;
        }
        PixelRect depth = mask.intersect(visible);
        PixelRect spans = mask.expand(radius, 0)
            .intersect(new PixelRect(0, 0, viewportWidth, viewportHeight));

        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_VIEWPORT_BIT
                | GL11.GL_SCISSOR_BIT
                | GL11.GL_CURRENT_BIT
                | GL11.GL_POLYGON_BIT
                | GL11.GL_LIGHTING_BIT);
        try {
            ensureResources(viewportWidth, viewportHeight);
            uploadStyles(frame);
            if (!depth.isEmpty()) {
                copySceneDepth(depth, viewportX, viewportY);
            }

            OpenGlHelper.func_153171_g(GL30.GL_FRAMEBUFFER, maskFramebuffer);
            GL11.glViewport(0, 0, viewportWidth, viewportHeight);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            scissor(mask, 0, 0);
            drawMask(level, frame, drawer, partialTicks, depthCopySupported ? depth : PixelRect.EMPTY);

            OpenGlHelper.func_153171_g(GL30.GL_FRAMEBUFFER, spanFramebuffer);
            scissor(spans, 0, 0);
            drawSpans(viewportWidth, viewportHeight, radius, mask);

            OpenGlHelper.func_153171_g(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
            scissor(composite, viewportX, viewportY);
            drawComposite(viewportWidth, viewportHeight, radius, mask);
        } finally {
            OpenGlHelper.func_153171_g(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL20.glUseProgram(previousProgram);
            bindAuxTexture(0);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);
            GL11.glPopAttrib();
        }
    }

    @Override
    public void release() {
        deleteTargets();
        if (styleTexture != 0) {
            GL11.glDeleteTextures(styleTexture);
            styleTexture = 0;
        }
        for (OutlineShaderProgram program : new OutlineShaderProgram[] { maskProgram, spanProgram, compositeProgram }) {
            if (program != null) {
                program.delete();
            }
        }
        maskProgram = null;
        spanProgram = null;
        compositeProgram = null;
    }

    private PixelRect projectBounds(OutlineFrame frame, Matrix4fc viewProjection, int viewportWidth,
        int viewportHeight) {
        PixelRect whole = new PixelRect(0, 0, viewportWidth, viewportHeight);
        float[] bounds = frame.bounds();
        if (bounds == null) {
            return whole;
        }
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < 8; i++) {
            corner.set(bounds[(i & 1) == 0 ? 0 : 3], bounds[(i & 2) == 0 ? 1 : 4], bounds[(i & 4) == 0 ? 2 : 5], 1f);
            viewProjection.transform(corner);
            if (corner.w < MIN_CLIP_W) {
                // A corner behind the camera projects nowhere useful; fall back to the full viewport.
                return whole;
            }
            float x = (corner.x / corner.w * 0.5f + 0.5f) * viewportWidth;
            float y = (corner.y / corner.w * 0.5f + 0.5f) * viewportHeight;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        return new PixelRect(
            (int) Math.floor(minX) - 1,
            (int) Math.floor(minY) - 1,
            (int) Math.ceil(maxX) + 1,
            (int) Math.ceil(maxY) + 1).intersect(whole);
    }

    private void drawMask(GuidebookLevel level, OutlineFrame frame, OutlineTargetDrawer drawer, float partialTicks,
        PixelRect depth) {
        GL11.glColorMask(true, true, true, true);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        maskProgram.use();
        maskProgram.setVec2("uTexel", 1f / width, 1f / height);
        maskProgram.setVec4("uDepthRect", depth.x0(), depth.y0(), depth.x1(), depth.y1());
        for (int i = 0; i < frame.batchCount(); i++) {
            OutlineBatch batch = frame.batch(i);
            maskProgram.setFloat("uSlot", slotOf(i) / 255f);
            if (!batch.getSolid()
                .isEmpty()) {
                maskProgram.setFloat("uCutout", 0f);
                drawer.draw(level, batch.getSolid(), partialTicks, maskState);
            }
            if (!batch.getCutout()
                .isEmpty()) {
                maskProgram.use();
                maskProgram.setFloat("uCutout", 1f);
                drawer.draw(level, batch.getCutout(), partialTicks, maskState);
            }
        }
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
    }

    private void applyMaskState() {
        maskProgram.use();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL14.glBlendEquation(GL14.GL_MAX);
    }

    private void drawSpans(int viewportWidth, int viewportHeight, int radius, PixelRect mask) {
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        spanProgram.use();
        spanProgram.setVec2("uTexel", 1f / width, 1f / height);
        spanProgram.setVec2("uMaskColumns", (float) mask.x0() / width, (float) mask.x1() / width);
        spanProgram.setInt("uRadius", radius);
        bindAuxTexture(styleTexture);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, maskTexture);
        drawFullscreenQuad((float) viewportWidth / width, (float) viewportHeight / height);
    }

    private void drawComposite(int viewportWidth, int viewportHeight, int radius, PixelRect mask) {
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        OpenGlHelper
            .glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        compositeProgram.use();
        compositeProgram.setVec2("uTexel", 1f / width, 1f / height);
        compositeProgram.setVec2("uSpanRows", (float) mask.y0() / height, (float) mask.y1() / height);
        compositeProgram.setInt("uRadius", radius);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, spanTexture);
        drawFullscreenQuad((float) viewportWidth / width, (float) viewportHeight / height);
    }

    private static void drawFullscreenQuad(float maxU, float maxV) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(-1f, -1f);
        GL11.glTexCoord2f(maxU, 0f);
        GL11.glVertex2f(1f, -1f);
        GL11.glTexCoord2f(maxU, maxV);
        GL11.glVertex2f(1f, 1f);
        GL11.glTexCoord2f(0f, maxV);
        GL11.glVertex2f(-1f, 1f);
        GL11.glEnd();
    }

    private static void scissor(PixelRect rect, int offsetX, int offsetY) {
        GL11.glScissor(rect.x0() + offsetX, rect.y0() + offsetY, rect.width(), rect.height());
    }

    private void copySceneDepth(PixelRect rect, int viewportX, int viewportY) {
        if (!depthCopySupported) {
            return;
        }
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit + AUX_TEXTURE_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        boolean probe = !depthCopyProbed;
        if (probe) {
            drainGlErrors();
        }
        GL11.glCopyTexSubImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            rect.x0(),
            rect.y0(),
            rect.x0() + viewportX,
            rect.y0() + viewportY,
            rect.width(),
            rect.height());
        if (probe) {
            // GLES (Angelica's ES profile) and some drivers refuse depth copies; occlusion is then skipped.
            depthCopyProbed = true;
            depthCopySupported = GL11.glGetError() == GL11.GL_NO_ERROR;
            if (!depthCopySupported) {
                GuideDebugLog.warnAlways("[GuideNH] Scene depth cannot be copied, outlines ignore occlusion");
            }
        }
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private static void drainGlErrors() {
        int drained = 0;
        while (drained < MAX_DRAINED_ERRORS && GL11.glGetError() != GL11.GL_NO_ERROR) {
            drained++;
        }
    }

    private static void bindAuxTexture(int texture) {
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit + AUX_TEXTURE_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private void uploadStyles(OutlineFrame frame) {
        float scale = DisplayScale.scaleFactor();
        ByteBuffer data = styleData;
        int usedSlots = 1;
        for (int i = 0; i < frame.batchCount(); i++) {
            OutlineBatch batch = frame.batch(i);
            int slot = slotOf(i);
            usedSlots = Math.max(usedSlots, slot + 1);
            putArgb(data, slot, COLUMN_VISIBLE, batch.getVisibleArgb());
            putArgb(data, slot, COLUMN_OCCLUDED, batch.getOccludedArgb());
            int thickness = Math.clamp(Math.round(batch.getThickness() * scale * THICKNESS_SCALE), 1, 255);
            putArgb(data, slot, COLUMN_THICKNESS, 0xFF000000 | thickness << 16);
        }
        data.clear();
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, styleTexture);
        GL11.glTexSubImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            0,
            0,
            STYLE_COLUMNS,
            usedSlots,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            data);
    }

    private static void putArgb(ByteBuffer data, int slot, int column, int argb) {
        int offset = (slot * STYLE_COLUMNS + column) * 4;
        data.put(offset, (byte) (argb >>> 16));
        data.put(offset + 1, (byte) (argb >>> 8));
        data.put(offset + 2, (byte) argb);
        data.put(offset + 3, (byte) (argb >>> 24));
    }

    private static int slotOf(int batchIndex) {
        return Math.min(batchIndex + 1, STYLE_SLOTS - 1);
    }

    private void ensureResources(int viewportWidth, int viewportHeight) {
        if (maskProgram == null) {
            maskProgram = OutlineShaderProgram.load("mask.vsh", "mask.fsh")
                .sampler("uTexture", 0)
                .sampler("uSceneDepth", AUX_TEXTURE_UNIT);
            spanProgram = OutlineShaderProgram.load("fullscreen.vsh", "spans.fsh")
                .sampler("uMask", 0)
                .sampler("uStyles", AUX_TEXTURE_UNIT);
            compositeProgram = OutlineShaderProgram.load("fullscreen.vsh", "composite.fsh")
                .sampler("uSpans", 0)
                .sampler("uStyles", AUX_TEXTURE_UNIT);
            styleTexture = createTexture(
                GL11.GL_RGBA8,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                STYLE_COLUMNS,
                STYLE_SLOTS);
        }
        if (viewportWidth <= width && viewportHeight <= height) {
            return;
        }
        int nextWidth = roundUp(Math.max(viewportWidth, width));
        int nextHeight = roundUp(Math.max(viewportHeight, height));
        deleteTargets();
        width = nextWidth;
        height = nextHeight;
        maskTexture = createTexture(GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, width, height);
        spanTexture = createTexture(GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, width, height);
        depthTexture = createTexture(
            GL14.GL_DEPTH_COMPONENT24,
            GL11.GL_DEPTH_COMPONENT,
            GL11.GL_UNSIGNED_INT,
            width,
            height);
        // Compare mode off: the mask shader reads raw depth from the red channel, which works in core profiles too.
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE);
        maskFramebuffer = createFramebuffer(maskTexture);
        spanFramebuffer = createFramebuffer(spanTexture);
    }

    private void deleteTargets() {
        if (maskFramebuffer != 0) {
            OpenGlHelper.func_153174_h(maskFramebuffer);
            maskFramebuffer = 0;
        }
        if (spanFramebuffer != 0) {
            OpenGlHelper.func_153174_h(spanFramebuffer);
            spanFramebuffer = 0;
        }
        for (int texture : new int[] { maskTexture, spanTexture, depthTexture }) {
            if (texture != 0) {
                GL11.glDeleteTextures(texture);
            }
        }
        maskTexture = 0;
        spanTexture = 0;
        depthTexture = 0;
        width = 0;
        height = 0;
    }

    private static int createTexture(int internalFormat, int format, int type, int textureWidth, int textureHeight) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            internalFormat,
            textureWidth,
            textureHeight,
            0,
            format,
            type,
            (ByteBuffer) null);
        return texture;
    }

    private static int createFramebuffer(int colorTexture) {
        int framebuffer = OpenGlHelper.func_153165_e();
        OpenGlHelper.func_153171_g(GL30.GL_FRAMEBUFFER, framebuffer);
        OpenGlHelper.func_153188_a(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colorTexture, 0);
        int status = OpenGlHelper.func_153167_i(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Outline framebuffer is incomplete: 0x" + Integer.toHexString(status));
        }
        return framebuffer;
    }

    private static int roundUp(int size) {
        return (size + SIZE_STEP - 1) / SIZE_STEP * SIZE_STEP;
    }

    private record PixelRect(int x0, int y0, int x1, int y1) {

        static final PixelRect EMPTY = new PixelRect(0, 0, 0, 0);

        boolean isEmpty() {
            return x1 <= x0 || y1 <= y0;
        }

        int width() {
            return Math.max(0, x1 - x0);
        }

        int height() {
            return Math.max(0, y1 - y0);
        }

        PixelRect intersect(PixelRect other) {
            return new PixelRect(
                Math.max(x0, other.x0),
                Math.max(y0, other.y0),
                Math.min(x1, other.x1),
                Math.min(y1, other.y1));
        }

        PixelRect expand(int dx, int dy) {
            return new PixelRect(x0 - dx, y0 - dy, x1 + dx, y1 + dy);
        }

        PixelRect offset(int dx, int dy) {
            return new PixelRect(x0 + dx, y0 + dy, x1 + dx, y1 + dy);
        }
    }
}
