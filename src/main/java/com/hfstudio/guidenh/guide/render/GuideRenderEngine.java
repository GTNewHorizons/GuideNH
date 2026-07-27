package com.hfstudio.guidenh.guide.render;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Central render engine that accepts GuideRenderPrimitives and batches them
 * into optimized Tessellator draw calls.
 * <p>
 * Lifecycle: beginFrame() -> execute() -> endFrame() (once per document render).
 * <p>
 * Coordinate contract: all primitives use absolute document coordinates.
 * The engine converts to screen GUI coordinates via its transform stack using
 * {@code screen = doc * scale + (tx, ty)} — the same math as
 * {@link VanillaRenderContext#toScreenRect}. GL pixels are obtained by
 * multiplying with the display scale factor where needed (glScissor).
 */
public class GuideRenderEngine {

    private static final RenderItem ITEM_RENDERER = new RenderItem();
    /** Synthetic-italic slant factor for sheared glyph runs (MC §o parity). */
    private static final float GLYPH_SHEAR_K = 0.25f;

    private final GuideGlyphAtlas glyphAtlas;
    private final GuidebookSceneRenderer sceneRenderer;

    private final Deque<Transform> transformStack = new ArrayDeque<>();
    /** Scissor rects in screen GUI coordinates (not GL pixels). */
    private final Deque<LytRect> scissorStack = new ArrayDeque<>();

    private LytRect viewport;
    private int guiScale = 1;

    /** Current batch state for Tessellator session sharing. */
    private BatchState batchState = BatchState.IDLE;
    /** Texture ID for the current textured batch (−1 when not textured). */
    private int batchTexId = -1;

    private enum BatchState {
        IDLE,
        /** Accumulating untextured quads (FillRect, GradientFill, DrawBorder, DrawLine). */
        SHAPE_QUADS,
        /** Accumulating textured quads (BlitTexture, DrawGlyphRun). */
        TEXTURED_QUADS
    }

    public GuideRenderEngine(GuideGlyphAtlas glyphAtlas, GuidebookSceneRenderer sceneRenderer) {
        this.glyphAtlas = glyphAtlas;
        this.sceneRenderer = sceneRenderer;
    }

    /** Begin a new frame. Clears all state stacks and establishes the GL baseline. */
    public void beginFrame(LytRect viewport, float displayScale) {
        this.viewport = viewport;
        this.guiScale = Math.max(1, Math.round(displayScale));
        resetStacks();
        applyFrameBaselineGlState();
    }

    /**
     * The frame's baseline GL state — the single state contract the engine
     * owns. Established at frame start, re-established after every escape
     * (HostDraw, item/3D rendering) and at frame end, so no primitive and no
     * escape hatch can leak enables into the stream. Keep in sync with
     * beginShape/beginTextured scopes.
     */
    private void applyFrameBaselineGlState() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private void resetStacks() {
        transformStack.clear();
        transformStack.push(new Transform(0f, 0f, 1.0f));
        scissorStack.clear();
        batchState = BatchState.IDLE;
        batchTexId = -1;
    }

    /** Execute a batch of primitives. Flushes the Tessellator before returning so
     *  subsequent drawing (e.g. legacy scrollbar rendering) does not hit "Already
     *  tesselating!". */
    public void execute(List<GuideRenderPrimitive> primitives) {
        // Safety flush: close any Tessellator batch left open from a prior frame.
        try { Tessellator.instance.draw(); } catch (IllegalStateException e) { /* not drawing */ }
        try {
            for (GuideRenderPrimitive p : primitives) {
                switch (p) {
                    case GuideRenderPrimitive.PushTransform t -> pushTransform(t);
                    case GuideRenderPrimitive.PopTransform __ -> popTransform();
                    case GuideRenderPrimitive.PushScissor s -> pushScissor(s);
                    case GuideRenderPrimitive.PopScissor __ -> popScissor();
                    case GuideRenderPrimitive.PushScreenScissor ss -> pushScreenScissor(ss);
                    case GuideRenderPrimitive.PopScreenScissor __ -> popScissor();
                    case GuideRenderPrimitive.FillRect f -> drawFillRect(f);
                    case GuideRenderPrimitive.GradientFill g -> drawGradientFill(g);
                    case GuideRenderPrimitive.DrawBorder db -> drawBorder(db);
                    case GuideRenderPrimitive.BlitTexture bt -> drawBlitTexture(bt);
                    case GuideRenderPrimitive.DrawGlyphRun dg -> drawGlyphRun(dg);
                    case GuideRenderPrimitive.DrawLine dl -> drawLine(dl);
                    case GuideRenderPrimitive.DrawTriangle dt -> drawTriangle(dt);
                    case GuideRenderPrimitive.DrawCircle dc -> drawCircle(dc);
                    case GuideRenderPrimitive.DrawCircleOutline dco -> drawCircleOutline(dco);
                    case GuideRenderPrimitive.DrawPolygon dp -> drawPolygon(dp);
                    case GuideRenderPrimitive.RenderItem ri -> drawRenderItem(ri);
                    case GuideRenderPrimitive.DrawText dtxt -> drawText(dtxt);
                    case GuideRenderPrimitive.RenderScene3D s3 -> drawScene3D(s3);
                    case GuideRenderPrimitive.HostDraw hd -> drawHostDraw(hd);
                }
            }
        } finally {
            // Close the current batch even when a drawXxx method throws mid-batch,
            // so the Tessellator never leaks in DRAWING state after execute() returns.
            flush();
            // Reset stacks to frame-initial state. A prior frame that threw mid-execute
            // can leave the stacks unbalanced; resetting here guarantees every execute()
            // starts with a clean slate regardless of what happened last frame.
            resetStacks();
            // Do not leak GL state to subsequent legacy drawing.
            restoreGlState();
        }
    }

    /** End the frame. Flush any remaining batch. */
    public void endFrame() {
        flush();
    }

    /** Restore the GL state expected by vanilla GUI drawing after execute(). */
    private void restoreGlState() {
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        // Mirror the frame baseline exactly (A-7): nothing the execute pass
        // touched may leak into subsequent legacy drawing.
        applyFrameBaselineGlState();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    // ---- transform stack ---------------------------------------------------

    private Transform currentTransform() {
        Transform t = transformStack.peek();
        if (t == null) {
            // Safety fallback: identity transform when no beginFrame() was called.
            t = new Transform(0f, 0f, 1.0f);
        }
        return t;
    }

    private void pushTransform(GuideRenderPrimitive.PushTransform t) {
        flush();
        Transform parent = currentTransform();
        // screen = doc * (parent.scale * t.scale) + (parent.t + t.t * parent.scale)
        transformStack.push(
            new Transform(
                parent.tx + t.tx() * parent.scale,
                parent.ty + t.ty() * parent.scale,
                parent.scale * t.scale()));
    }

    private void popTransform() {
        flush();
        if (transformStack.size() > 1) {
            transformStack.pop();
        }
    }

    // ---- scissor stack (screen GUI coordinates) ----------------------------

    /** Intersect {@code screenRect} with the current scissor, push, and apply. */
    private void pushScissorRect(LytRect screenRect) {
        flush();
        LytRect sr = screenRect;
        if (!scissorStack.isEmpty()) {
            LytRect parent = scissorStack.peek();
            int x1 = Math.max(sr.x(), parent.x());
            int y1 = Math.max(sr.y(), parent.y());
            int x2 = Math.min(sr.right(), parent.right());
            int y2 = Math.min(sr.bottom(), parent.bottom());
            sr = new LytRect(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
        }
        scissorStack.push(sr);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        glScissor(sr);
    }

    private void pushScissor(GuideRenderPrimitive.PushScissor s) {
        pushScissorRect(toScreen(s.x(), s.y(), s.w(), s.h()));
    }

    /**
     * Screen-space scissor: coordinates are already in screen GUI space, no
     * transform-stack conversion. Used for the fixed viewport clip.
     */
    private void pushScreenScissor(GuideRenderPrimitive.PushScreenScissor ss) {
        pushScissorRect(new LytRect(ss.x(), ss.y(), ss.w(), ss.h()));
    }

    private void popScissor() {
        flush();
        if (scissorStack.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            return;
        }
        scissorStack.pop();
        if (!scissorStack.isEmpty()) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            glScissor(scissorStack.peek());
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    /**
     * Re-apply the current scissor state after external code (HostDraw legacy
     * rendering, 3D scene) may have changed it.
     */
    private void reapplyScissor() {
        if (!scissorStack.isEmpty()) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            glScissor(scissorStack.peek());
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private void glScissor(LytRect r) {
        int sx = r.x() * guiScale;
        int sy = Minecraft.getMinecraft().displayHeight - r.bottom() * guiScale;
        // Zero-area scissors must stay zero (a min-1 clamp would leak a 1px
        // strip of content that should be fully clipped — A-6).
        int sw = Math.max(0, r.width() * guiScale);
        int sh = Math.max(0, r.height() * guiScale);
        GL11.glScissor(sx, Math.max(0, sy), sw, sh);
    }

    // ---- coordinate conversion ----------------------------------------------

    /** Document -> screen GUI coordinates, integer rect. */
    private LytRect toScreen(int x, int y, int w, int h) {
        Transform t = currentTransform();
        return new LytRect(
            Math.round(x * t.scale + t.tx),
            Math.round(y * t.scale + t.ty),
            Math.max(0, Math.round(w * t.scale)),
            Math.max(0, Math.round(h * t.scale)));
    }

    private float sx(float docX) {
        Transform t = currentTransform();
        return docX * t.scale + t.tx;
    }

    private float sy(float docY) {
        Transform t = currentTransform();
        return docY * t.scale + t.ty;
    }

    // ---- draw helpers --------------------------------------------------------

    /**
     * Shape-primitive state scope. The ENGINE owns GL state — every shape draw
     * (fills, lines, circles, polygons) runs inside this scope, which fully
     * controls the state legacy rendering proved necessary
     * (VanillaRenderContext.beginShapeDraw): TEXTURE_2D off, BLEND on,
     * ALPHA_TEST / CULL_FACE / DEPTH_TEST off. CULL_FACE matters: shared-state
     * leakage (item rendering, HostDraw legacy) leaves it enabled, and without
     * this scope all CW-wound primitives (lines, circles) are culled away.
     * Individual draw methods must NOT touch GL state themselves.
     */
    private void beginShape() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private void endShape() {
        GL11.glPopAttrib();
    }

    /**
     * Submit the current batched primitives to GL and reset batch state.
     * Safe to call when no batch is open (no-op in that case).
     */
    private void flush() {
        if (batchState == BatchState.IDLE) return;
        Tessellator tess = Tessellator.instance;
        tess.draw();
        if (batchState == BatchState.SHAPE_QUADS) {
            endShape();
        } else if (batchState == BatchState.TEXTURED_QUADS) {
            endTextured();
        }
        batchState = BatchState.IDLE;
        batchTexId = -1;
    }

    /**
     * Start or join a shape-quads batch (FillRect, GradientFill, DrawBorder, DrawLine).
     * Flushes any incompatible batch first.
     */
    private void beginShapeQuads() {
        if (batchState == BatchState.SHAPE_QUADS) return;
        flush();
        beginShape();
        Tessellator.instance.startDrawingQuads();
        batchState = BatchState.SHAPE_QUADS;
    }

    /**
     * Start or join a textured-quads batch (BlitTexture, DrawGlyphRun).
     * Flushes any incompatible batch first (different type or different texture).
     */
    private void beginTexturedQuads(int texId) {
        if (batchState == BatchState.TEXTURED_QUADS && batchTexId == texId) return;
        flush();
        beginTextured();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        Tessellator.instance.startDrawingQuads();
        batchState = BatchState.TEXTURED_QUADS;
        batchTexId = texId;
    }

    private void color(int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        GL11.glColor4f(r, g, b, a);
    }

    /**
     * Apply color to the Tessellator's per-vertex color channel. Required in
     * 1.7.10: once anything has put the shared Tessellator into hasColor state
     * (vanilla Gui.drawRect, item rendering, ...), it ignores the global GL
     * color and uses the last per-vertex color instead — shape primitives drawn
     * without this come out with a stale (usually dark or transparent) color.
     * Mirrors VanillaRenderContext.tessColor.
     */
    private static void tessColor(Tessellator tess, int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        if (a == 0) {
            a = 0xFF;
        }
        tess.setColorRGBA_I((r << 16) | (g << 8) | b, a);
    }

    // ---- draw primitives ------------------------------------------------------

    private void drawFillRect(GuideRenderPrimitive.FillRect f) {
        LytRect r = toScreen(f.x(), f.y(), f.w(), f.h());
        beginShapeQuads();
        color(f.argb());
        Tessellator tess = Tessellator.instance;
        tessColor(tess, f.argb());
        tess.addVertex(r.x(), r.y() + r.height(), 0);
        tess.addVertex(r.x() + r.width(), r.y() + r.height(), 0);
        tess.addVertex(r.x() + r.width(), r.y(), 0);
        tess.addVertex(r.x(), r.y(), 0);
    }

    private void drawGradientFill(GuideRenderPrimitive.GradientFill g) {
        LytRect r = toScreen(g.x(), g.y(), g.w(), g.h());
        beginShapeQuads();
        Tessellator tess = Tessellator.instance;
        color(g.argbBottom());
        tessColor(tess, g.argbBottom());
        tess.addVertex(r.x(), r.y() + r.height(), 0);
        tess.addVertex(r.x() + r.width(), r.y() + r.height(), 0);
        color(g.argbTop());
        tessColor(tess, g.argbTop());
        tess.addVertex(r.x() + r.width(), r.y(), 0);
        tess.addVertex(r.x(), r.y(), 0);
    }

    private void drawBorder(GuideRenderPrimitive.DrawBorder db) {
        int x = db.x(), y = db.y(), w = db.w(), h = db.h();
        int argb = db.argb();
        beginShapeQuads();
        Tessellator tess = Tessellator.instance;
        color(argb);
        tessColor(tess, argb);
        // Aggregate all four sides into a single begin/end pair. Each side that
        // is visible adds exactly one quad (4 vertices); at most 4 quads / 16
        // vertices total.
        if (db.top() > 0) {
            LytRect r = toScreen(x, y, w, db.top());
            tess.addVertex(r.x(), r.y() + r.height(), 0);
            tess.addVertex(r.x() + r.width(), r.y() + r.height(), 0);
            tess.addVertex(r.x() + r.width(), r.y(), 0);
            tess.addVertex(r.x(), r.y(), 0);
        }
        if (db.bottom() > 0) {
            LytRect r = toScreen(x, y + h - db.bottom(), w, db.bottom());
            tess.addVertex(r.x(), r.y() + r.height(), 0);
            tess.addVertex(r.x() + r.width(), r.y() + r.height(), 0);
            tess.addVertex(r.x() + r.width(), r.y(), 0);
            tess.addVertex(r.x(), r.y(), 0);
        }
        if (db.left() > 0) {
            LytRect r = toScreen(x, y + db.top(), db.left(), h - db.top() - db.bottom());
            tess.addVertex(r.x(), r.y() + r.height(), 0);
            tess.addVertex(r.x() + r.width(), r.y() + r.height(), 0);
            tess.addVertex(r.x() + r.width(), r.y(), 0);
            tess.addVertex(r.x(), r.y(), 0);
        }
        if (db.right() > 0) {
            LytRect r = toScreen(x + w - db.right(), y + db.top(), db.right(), h - db.top() - db.bottom());
            tess.addVertex(r.x(), r.y() + r.height(), 0);
            tess.addVertex(r.x() + r.width(), r.y() + r.height(), 0);
            tess.addVertex(r.x() + r.width(), r.y(), 0);
            tess.addVertex(r.x(), r.y(), 0);
        }
    }

    /**
     * Textured-primitive state scope (Blit/GlyphRun). The ENGINE owns GL state:
     * texture on, blend on, ALPHA_TEST / CULL_FACE / DEPTH_TEST off — item
     * rendering and HostDraw legacy can leak any of these into the stream, and
     * without this scope textured quads get culled or their AA edges cut (A-3).
     */
    private void beginTextured() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private void endTextured() {
        GL11.glPopAttrib();
    }

    private void drawBlitTexture(GuideRenderPrimitive.BlitTexture bt) {
        LytRect r = toScreen(bt.x(), bt.y(), bt.w(), bt.h());
        beginTexturedQuads(bt.texId());
        color(bt.argb());
        Tessellator tess = Tessellator.instance;
        tessColor(tess, bt.argb());
        tess.addVertexWithUV(r.x(), r.y() + r.height(), 0, bt.u(), bt.v2());
        tess.addVertexWithUV(r.x() + r.width(), r.y() + r.height(), 0, bt.u2(), bt.v2());
        tess.addVertexWithUV(r.x() + r.width(), r.y(), 0, bt.u2(), bt.v());
        tess.addVertexWithUV(r.x(), r.y(), 0, bt.u(), bt.v());
    }

    private void drawGlyphRun(GuideRenderPrimitive.DrawGlyphRun dg) {
        List<GuideRenderPrimitive.PlacedGlyph> glyphs = dg.glyphs();
        if (glyphs == null || glyphs.isEmpty()) return;
        int atlasTex = dg.atlasId();
        if (atlasTex <= 0) return;
        Transform t = currentTransform();
        // Synthetic-italic slant (MC §o parity): x shifts right proportional to
        // the distance above the run's bottom edge.
        boolean shear = dg.shear();
        float shearBaseY = 0f;
        if (shear) {
            for (GuideRenderPrimitive.PlacedGlyph g : glyphs) {
                shearBaseY = Math.max(shearBaseY, g.y() + g.h());
            }
            shearBaseY = shearBaseY * t.scale + t.ty;
        }
        beginTexturedQuads(atlasTex);
        color(dg.argb());
        Tessellator tess = Tessellator.instance;
        tessColor(tess, dg.argb());
        int missingAtlas = 0;
        for (GuideRenderPrimitive.PlacedGlyph g : glyphs) {
            // UV coordinates from glyph atlas
            GuideGlyphAtlas.GlyphUV uv = glyphAtlas.lookup(g.atlasKey());
            if (uv == null) {
                missingAtlas++;
                continue;
            }
            // Keep subpixel precision: vertices are float, only the atlas UV is fixed.
            float x = g.x() * t.scale + t.tx;
            float y = g.y() * t.scale + t.ty;
            float w = g.w() * t.scale;
            float h = g.h() * t.scale;
            float xTop = x;
            float xBottom = x;
            if (shear) {
                xTop = x + GLYPH_SHEAR_K * (shearBaseY - y);
                xBottom = x + GLYPH_SHEAR_K * (shearBaseY - y - h);
            }
            // Atlas rows are written top-to-bottom: uv.v = glyph top row,
            // uv.v2 = glyph bottom row. Screen Y increases downward, so screen
            // top pairs with uv.v and screen bottom with uv.v2 — same convention
            // as VanillaRenderContext.blitTexture.
            tess.addVertexWithUV(xBottom, y + h, 0, uv.u(), uv.v2()); // bottom-left → glyph bottom row
            tess.addVertexWithUV(xBottom + w, y + h, 0, uv.u2(), uv.v2()); // bottom-right → glyph bottom row
            tess.addVertexWithUV(xTop + w, y, 0, uv.u2(), uv.v()); // top-right → glyph top row
            tess.addVertexWithUV(xTop, y, 0, uv.u(), uv.v()); // top-left → glyph top row
        }
        if (missingAtlas > 0 && GuideDebugLog.isLayoutOverlayEnabled()) {
            GuideDebugLog.warnAlways(
                "[TRC] DrawGlyphRun: {} glyphs missing from atlas (run size={})",
                missingAtlas,
                glyphs.size());
        }
    }

    private void drawLine(GuideRenderPrimitive.DrawLine dl) {
        float x1 = sx(dl.x1()), y1 = sy(dl.y1());
        float x2 = sx(dl.x2()), y2 = sy(dl.y2());
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;

        float half = Math.max(0.5f, dl.thickness() * currentTransform().scale * 0.5f);
        float nx = -dy / len * half;
        float ny = dx / len * half;

        beginShapeQuads();
        color(dl.argb());
        Tessellator tess = Tessellator.instance;
        tessColor(tess, dl.argb());
        tess.addVertex(x1 - nx, y1 - ny, 0);
        tess.addVertex(x2 - nx, y2 - ny, 0);
        tess.addVertex(x2 + nx, y2 + ny, 0);
        tess.addVertex(x1 + nx, y1 + ny, 0);
    }

    private void drawTriangle(GuideRenderPrimitive.DrawTriangle dt) {
        flush();
        beginShape();
        color(dt.argb());
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLES);
        tessColor(tess, dt.argb());
        tess.addVertex(sx(dt.x1()), sy(dt.y1()), 0);
        tess.addVertex(sx(dt.x2()), sy(dt.y2()), 0);
        tess.addVertex(sx(dt.x3()), sy(dt.y3()), 0);
        tess.draw();
        endShape();
    }

    private static final int CIRCLE_SEGMENTS = 32;

    private void drawCircle(GuideRenderPrimitive.DrawCircle dc) {
        flush();
        float cx = sx(dc.cx()), cy = sy(dc.cy());
        float radius = dc.radius() * currentTransform().scale;
        beginShape();
        color(dc.argb());
        Tessellator tess = Tessellator.instance;
        if (dc.filled()) {
            tess.startDrawing(GL11.GL_TRIANGLE_FAN);
            tessColor(tess, dc.argb());
            tess.addVertex(cx, cy, 0);
            for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                double a = Math.PI * 2.0 * i / CIRCLE_SEGMENTS;
                tess.addVertex(cx + (float) (Math.cos(a) * radius), cy + (float) (Math.sin(a) * radius), 0);
            }
            tess.draw();
        } else {
            tess.startDrawing(GL11.GL_TRIANGLE_STRIP);
            tessColor(tess, dc.argb());
            for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                double a = Math.PI * 2.0 * i / CIRCLE_SEGMENTS;
                float cos = (float) Math.cos(a);
                float sin = (float) Math.sin(a);
                tess.addVertex(cx + cos * (radius - 1), cy + sin * (radius - 1), 0);
                tess.addVertex(cx + cos * (radius + 1), cy + sin * (radius + 1), 0);
            }
            tess.draw();
        }
        endShape();
    }

    private void drawCircleOutline(GuideRenderPrimitive.DrawCircleOutline dco) {
        flush();
        float cx = sx(dco.cx()), cy = sy(dco.cy());
        float scale = currentTransform().scale;
        float half = Math.max(0.5f, dco.thickness() * scale * 0.5f);
        float radius = dco.radius() * scale;
        float inner = Math.max(0f, radius - half);
        float outer = radius + half;
        beginShape();
        color(dco.argb());
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_STRIP);
        tessColor(tess, dco.argb());
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double a = Math.PI * 2.0 * i / CIRCLE_SEGMENTS;
            float cos = (float) Math.cos(a);
            float sin = (float) Math.sin(a);
            tess.addVertex(cx + cos * inner, cy + sin * inner, 0);
            tess.addVertex(cx + cos * outer, cy + sin * outer, 0);
        }
        tess.draw();
        endShape();
    }

    private void drawPolygon(GuideRenderPrimitive.DrawPolygon dp) {
        flush();
        float[] xs = dp.xs();
        float[] ys = dp.ys();
        if (xs == null || ys == null || xs.length < 3) return;
        int n = Math.min(xs.length, ys.length);
        beginShape();
        color(dp.argb());
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_FAN);
        tessColor(tess, dp.argb());
        tess.addVertex(sx(xs[0]), sy(ys[0]), 0);
        for (int i = 1; i < n; i++) {
            tess.addVertex(sx(xs[i]), sy(ys[i]), 0);
        }
        tess.draw();
        endShape();
    }

    private void drawRenderItem(GuideRenderPrimitive.RenderItem ri) {
        flush();
        if (ri.stack() == null) return;
        var mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRenderer;
        float scale = currentTransform().scale;
        // Match VanillaRenderContext.renderItemInternal state handling; the item is
        // drawn at the transformed position with zoom applied via the modelview.
        GL11.glPushMatrix();
        GL11.glTranslatef(sx(ri.x()), sy(ri.y()), 0f);
        GL11.glScalef(scale, scale, 1f);
        try {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1f, 1f, 1f, 1f);

            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_ALPHA_TEST);

            ITEM_RENDERER.zLevel = 100f;
            ITEM_RENDERER.renderItemAndEffectIntoGUI(fr, mc.getTextureManager(), ri.stack(), 0, 0);
            ITEM_RENDERER.renderItemOverlayIntoGUI(fr, mc.getTextureManager(), ri.stack(), 0, 0);
            RenderHelper.disableStandardItemLighting();
        } finally {
            ITEM_RENDERER.zLevel = 0f;
            GL11.glPopMatrix();
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            // Restore the frame baseline (A-3): item rendering — especially
            // modded IItemRenderer — may leave CULL_FACE/DEPTH_TEST/ALPHA_TEST
            // enabled, which would cull or cut every later primitive this frame.
            RenderHelper.disableStandardItemLighting();
            applyFrameBaselineGlState();
        }
    }

    private void drawText(GuideRenderPrimitive.DrawText dt) {
        flush();
        Minecraft mc = Minecraft.getMinecraft();
        String text = dt.text();
        if (text == null || text.isEmpty()) return;
        ResolvedTextStyle style = dt.style();
        int x = Math.round(sx(dt.x()));
        int y = Math.round(sy(dt.y()));

        int color = resolveTextColor(style);
        String drawn = GuideFontCompat.prepareRenderedText(text, style);

        // Combine the style's own font scale with the current zoom so text
        // scales exactly like it did under the legacy GL-matrix pipeline.
        float scale = style.fontScale() * currentTransform().scale;
        boolean scaled = Math.abs(scale - 1f) > 1e-4f;
        if (scaled) {
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0f);
            GL11.glScalef(scale, scale, 1f);
            if (style.dropShadow()) {
                mc.fontRenderer.drawStringWithShadow(drawn, 0, 0, color);
            } else {
                mc.fontRenderer.drawString(drawn, 0, 0, color);
            }
            GL11.glPopMatrix();
        } else if (style.dropShadow()) {
            mc.fontRenderer.drawStringWithShadow(drawn, x, y, color);
        } else {
            mc.fontRenderer.drawString(drawn, x, y, color);
        }

        drawTextDecorations(drawn, x, y, color, style, scale);
    }

    private int resolveTextColor(ResolvedTextStyle style) {
        int color = style.color() != null ? style.color()
            .resolve(LightDarkMode.current()) : 0xFFFFFFFF;
        if ((color >>> 24) == 0) {
            color |= 0xFF000000;
        }
        return color;
    }

    private void drawTextDecorations(String text, int x, int y, int color, ResolvedTextStyle style, float scale) {
        boolean hasUnderline = style.underlined();
        boolean hasWavyUnderline = style.wavyUnderline();
        boolean hasDottedUnderline = style.dottedUnderline();
        if (!hasUnderline && !hasWavyUnderline && !hasDottedUnderline) return;

        Minecraft mc = Minecraft.getMinecraft();
        int scaledFontHeight = Math.round(mc.fontRenderer.FONT_HEIGHT * scale);
        int decorationY = y + scaledFontHeight - 1;
        int decoratedWidth = GuideFontCompat.getPreparedStringWidth(mc.fontRenderer, text, style);

        if (hasUnderline) {
            Gui.drawRect(x, decorationY, x + decoratedWidth, decorationY + 1, color);
        }
        if (hasWavyUnderline) {
            for (int i = 0; i < decoratedWidth; i++) {
                int phase = i & 3;
                int dy = (phase == 0 || phase == 2) ? 0 : (phase == 1 ? -1 : 1);
                Gui.drawRect(x + i, decorationY + dy, x + i + 1, decorationY + dy + 1, color);
            }
        }
        if (hasDottedUnderline) {
            int cursor = 0;
            boolean bold = style.bold();
            for (int i = 0; i < text.length() && cursor < decoratedWidth; i++) {
                char c = text.charAt(i);
                int charWidth = mc.fontRenderer.getCharWidth(c);
                if (bold) charWidth++;
                int dotX = x + cursor + (charWidth - 2) / 2;
                Gui.drawRect(dotX, decorationY - 1, dotX + 2, decorationY + 1, color);
                cursor += charWidth;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void drawScene3D(GuideRenderPrimitive.RenderScene3D s3) {
        flush();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();

        LytRect clip = toScreen(s3.clipX(), s3.clipY(), s3.clipW(), s3.clipH());
        if (!scissorStack.isEmpty()) {
            LytRect parent = scissorStack.peek();
            int x1 = Math.max(clip.x(), parent.x());
            int y1 = Math.max(clip.y(), parent.y());
            int x2 = Math.min(clip.right(), parent.right());
            int y2 = Math.min(clip.bottom(), parent.bottom());
            clip = new LytRect(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
        }

        sceneRenderer.render(
            s3.level(),
            s3.camera(),
            s3.particles(),
            s3.weatherEffects(),
            s3.weatherAnimationTick(),
            s3.lightDarkMode(),
            clip.x(),
            clip.y(),
            clip.width(),
            clip.height(),
            viewport.width(),
            viewport.height());

        GL11.glPopMatrix();
        GL11.glPopAttrib();

        // Restore scissor after 3D rendering
        reapplyScissor();
    }

    /**
     * Legacy subtree rendering. The GL modelview is set from the current
     * transform stack so the callback can render in document coordinates
     * ({@code screen = doc * scale + t}). The callback's RenderContext scissor
     * stack is seeded with the engine's current scissor so nested legacy
     * scissors stay inside the viewport, and GL/scissor state is restored
     * afterwards.
     */
    private void drawHostDraw(GuideRenderPrimitive.HostDraw hd) {
        flush();
        Transform t = currentTransform();
        RenderContext ctx = hd.context();
        if (GuideDebugLog.isLayoutOverlayEnabled()) {
            GuideDebugLog.warnAlways(
                "[TRC] HostDraw execute transform=({},{},{}) scissorTop={}",
                t.tx,
                t.ty,
                t.scale,
                scissorStack.peek());
        }
        // Escape-hatch contract: the legacy callback runs with full GL state
        // saved/restored, so it cannot leak enables (cull face, depth test, ...)
        // into the primitive stream.
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glTranslatef(t.tx, t.ty, 0f);
        GL11.glScalef(t.scale, t.scale, 1f);
        boolean seededScissor = false;
        if (!scissorStack.isEmpty()) {
            ctx.pushScissor(scissorStack.peek());
            seededScissor = true;
        }
        try {
            hd.draw()
                .run();
        } finally {
            if (seededScissor) {
                ctx.popScissor();
            }
            // The legacy path may leave the Tessellator mid-batch.
            try {
                Tessellator.instance.draw();
            } catch (IllegalStateException ignored) {}
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            ctx.restoreExternalRenderState();
            // restoreExternalRenderState restores the LEGACY context's expected
            // state (it re-enables ALPHA_TEST among others); the engine must
            // re-establish its own baseline afterwards (A-2), then re-apply the
            // engine scissor over the context's (now empty) scissor stack.
            applyFrameBaselineGlState();
            reapplyScissor();
        }
    }

    private record Transform(float tx, float ty, float scale) {}
}
