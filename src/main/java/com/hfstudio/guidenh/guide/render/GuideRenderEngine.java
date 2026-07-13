package com.hfstudio.guidenh.guide.render;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.document.LytRect;

/**
 * Central render engine that accepts GuideRenderPrimitives and batches them
 * into optimized Tessellator draw calls.
 * <p>
 * Lifecycle: beginFrame() -> execute() -> endFrame() (once per frame).
 * Coordinates: all primitives use absolute document coordinates.
 * Engine applies the current transform stack internally.
 */
public class GuideRenderEngine {

    private final GuideGlyphAtlas glyphAtlas;
    private final GuidebookSceneRenderer sceneRenderer;

    private final Deque<Transform> transformStack = new ArrayDeque<>();
    private final Deque<LytRect> scissorStack = new ArrayDeque<>();
    private int currentColor = 0xFFFFFFFF;
    private int currentBlend = GL11.GL_SRC_ALPHA;
    private int currentTexture = 0;

    private LytRect viewport;
    private float displayScale;

    public GuideRenderEngine(GuideGlyphAtlas glyphAtlas, GuidebookSceneRenderer sceneRenderer) {
        this.glyphAtlas = glyphAtlas;
        this.sceneRenderer = sceneRenderer;
    }

    /** Begin a new frame. Clears all state stacks. */
    public void beginFrame(LytRect viewport, float displayScale) {
        this.viewport = viewport;
        this.displayScale = displayScale;
        this.transformStack.clear();
        this.transformStack.push(new Transform(0, 0, 1.0f));
        this.scissorStack.clear();
        this.currentColor = 0xFFFFFFFF;
        this.currentBlend = GL11.GL_SRC_ALPHA;
        this.currentTexture = 0;
    }

    /** Execute a batch of primitives. */
    public void execute(List<GuideRenderPrimitive> primitives) {
        for (GuideRenderPrimitive p : primitives) {
            switch (p) {
                case GuideRenderPrimitive.PushTransform t -> pushTransform(t);
                case GuideRenderPrimitive.PopTransform __ -> popTransform();
                case GuideRenderPrimitive.PushScissor s -> pushScissor(s);
                case GuideRenderPrimitive.PopScissor __ -> popScissor();
                case GuideRenderPrimitive.SetColor c -> currentColor = c.argb();
                case GuideRenderPrimitive.SetBlendMode b -> currentBlend = b.mode();
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
    }

    /** End the frame. Flush any remaining batch. */
    public void endFrame() {
        flush();
    }

    private void pushTransform(GuideRenderPrimitive.PushTransform t) {
        Transform parent = transformStack.peek();
        transformStack.push(new Transform(parent.tx + t.tx(), parent.ty + t.ty(), parent.scale * t.scale()));
    }

    private void popTransform() {
        if (transformStack.size() > 1) {
            transformStack.pop();
        }
    }

    private void pushScissor(GuideRenderPrimitive.PushScissor s) {
        LytRect screen = toScreen(s.x(), s.y(), s.w(), s.h());
        if (!scissorStack.isEmpty()) {
            LytRect parent = scissorStack.peek();
            int x1 = Math.max(screen.x(), parent.x());
            int y1 = Math.max(screen.y(), parent.y());
            int x2 = Math.min(screen.right(), parent.right());
            int y2 = Math.min(screen.bottom(), parent.bottom());
            screen = new LytRect(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
        }
        scissorStack.push(screen);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        glScissor(screen);
    }

    private void popScissor() {
        scissorStack.pop();
        if (!scissorStack.isEmpty()) {
            glScissor(scissorStack.peek());
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private LytRect toScreen(int x, int y, int w, int h) {
        Transform t = transformStack.peek();
        return new LytRect(
            Math.round((x + t.tx) * t.scale),
            Math.round((y + t.ty) * t.scale),
            Math.max(1, Math.round(w * t.scale)),
            Math.max(1, Math.round(h * t.scale)));
    }

    private void glScissor(LytRect r) {
        int s = Math.round(displayScale);
        int sx = r.x() * s;
        int sy = Minecraft.getMinecraft().displayHeight - r.bottom() * s;
        int sw = Math.max(1, r.width() * s);
        int sh = Math.max(1, r.height() * s);
        GL11.glScissor(sx, Math.max(0, sy), sw, Math.max(0, sh));
    }

    private void setupSolid() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void teardownSolid() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void flush() {
        // In Phase 1, each draw call is immediate (no batching yet).
        // Future phase: batch similar primitives to reduce Tessellator.draw() calls.
    }

    private void color(int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        GL11.glColor4f(r, g, b, a);
    }

    private void drawFillRect(GuideRenderPrimitive.FillRect f) {
        LytRect r = toScreen(f.x(), f.y(), f.w(), f.h());
        setupSolid();
        color(f.argb());
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertex(r.x(), r.y() + r.height(), 0);
        tess.addVertex(r.x() + r.width(), r.y() + r.height(), 0);
        tess.addVertex(r.x() + r.width(), r.y(), 0);
        tess.addVertex(r.x(), r.y(), 0);
        tess.draw();
        teardownSolid();
    }

    private void drawGradientFill(GuideRenderPrimitive.GradientFill g) {
        LytRect r = toScreen(g.x(), g.y(), g.w(), g.h());
        setupSolid();
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        color(g.argbBottom());
        tess.addVertex(r.x(), r.y() + r.height(), 0);
        tess.addVertex(r.x() + r.width(), r.y() + r.height(), 0);
        color(g.argbTop());
        tess.addVertex(r.x() + r.width(), r.y(), 0);
        tess.addVertex(r.x(), r.y(), 0);
        tess.draw();
        teardownSolid();
    }

    private void drawBorder(GuideRenderPrimitive.DrawBorder db) {
        int x = db.x(), y = db.y(), w = db.w(), h = db.h();
        int argb = db.argb();
        if (db.top() > 0) drawFillRect(new GuideRenderPrimitive.FillRect(x, y, w, db.top(), argb));
        if (db.bottom() > 0)
            drawFillRect(new GuideRenderPrimitive.FillRect(x, y + h - db.bottom(), w, db.bottom(), argb));
        if (db.left() > 0) drawFillRect(
            new GuideRenderPrimitive.FillRect(x, y + db.top(), db.left(), h - db.top() - db.bottom(), argb));
        if (db.right() > 0) drawFillRect(
            new GuideRenderPrimitive.FillRect(
                x + w - db.right(),
                y + db.top(),
                db.right(),
                h - db.top() - db.bottom(),
                argb));
    }

    private void drawBlitTexture(GuideRenderPrimitive.BlitTexture bt) {
        LytRect r = toScreen(bt.x(), bt.y(), bt.w(), bt.h());
        flush();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, bt.texId());
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(r.x(), r.y() + r.height(), 0, bt.u(), bt.v2());
        tess.addVertexWithUV(r.x() + r.width(), r.y() + r.height(), 0, bt.u2(), bt.v2());
        tess.addVertexWithUV(r.x() + r.width(), r.y(), 0, bt.u2(), bt.v());
        tess.addVertexWithUV(r.x(), r.y(), 0, bt.u(), bt.v());
        tess.draw();
    }

    private void drawGlyphRun(GuideRenderPrimitive.DrawGlyphRun dg) {
        List<GuideRenderPrimitive.PlacedGlyph> glyphs = dg.glyphs();
        if (glyphs == null || glyphs.isEmpty()) return;
        flush();
        int atlasTex = dg.atlasId();
        if (atlasTex <= 0) return; // atlas not uploaded yet
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlasTex);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        for (GuideRenderPrimitive.PlacedGlyph g : glyphs) {
            LytRect sr = toScreen(Math.round(g.x()), Math.round(g.y()), Math.round(g.w()), Math.round(g.h()));
            // UV coordinates from glyph atlas
            GuideGlyphAtlas.GlyphUV uv = glyphAtlas.lookup(g.glyphId());
            if (uv == null) continue;
            tess.addVertexWithUV(sr.x(), sr.y() + sr.height(), 0, uv.u(), uv.v2());
            tess.addVertexWithUV(sr.x() + sr.width(), sr.y() + sr.height(), 0, uv.u2(), uv.v2());
            tess.addVertexWithUV(sr.x() + sr.width(), sr.y(), 0, uv.u2(), uv.v());
            tess.addVertexWithUV(sr.x(), sr.y(), 0, uv.u(), uv.v());
        }
        tess.draw();
    }

    private void drawLine(GuideRenderPrimitive.DrawLine dl) {
        float dx = dl.x2() - dl.x1();
        float dy = dl.y2() - dl.y1();
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;

        float half = Math.max(0.5f, dl.thickness() * 0.5f);
        float nx = -dy / len * half;
        float ny = dx / len * half;

        setupSolid();
        color(dl.argb());
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertex(dl.x1() - nx, dl.y1() - ny, 0);
        tess.addVertex(dl.x2() - nx, dl.y2() - ny, 0);
        tess.addVertex(dl.x2() + nx, dl.y2() + ny, 0);
        tess.addVertex(dl.x1() + nx, dl.y1() + ny, 0);
        tess.draw();
        teardownSolid();
    }

    private void drawTriangle(GuideRenderPrimitive.DrawTriangle dt) {
        setupSolid();
        color(dt.argb());
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLES);
        tess.addVertex(dt.x1(), dt.y1(), 0);
        tess.addVertex(dt.x2(), dt.y2(), 0);
        tess.addVertex(dt.x3(), dt.y3(), 0);
        tess.draw();
        teardownSolid();
    }

    private static final int CIRCLE_SEGMENTS = 32;

    private void drawCircle(GuideRenderPrimitive.DrawCircle dc) {
        setupSolid();
        color(dc.argb());
        Tessellator tess = Tessellator.instance;
        if (dc.filled()) {
            tess.startDrawing(GL11.GL_TRIANGLE_FAN);
            tess.addVertex(dc.cx(), dc.cy(), 0);
            for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                double a = Math.PI * 2.0 * i / CIRCLE_SEGMENTS;
                tess.addVertex(
                    dc.cx() + (float) (Math.cos(a) * dc.radius()),
                    dc.cy() + (float) (Math.sin(a) * dc.radius()),
                    0);
            }
            tess.draw();
        } else {
            tess.startDrawing(GL11.GL_TRIANGLE_STRIP);
            for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                double a = Math.PI * 2.0 * i / CIRCLE_SEGMENTS;
                float cos = (float) Math.cos(a);
                float sin = (float) Math.sin(a);
                tess.addVertex(dc.cx() + cos * (dc.radius() - 1), dc.cy() + sin * (dc.radius() - 1), 0);
                tess.addVertex(dc.cx() + cos * (dc.radius() + 1), dc.cy() + sin * (dc.radius() + 1), 0);
            }
            tess.draw();
        }
        teardownSolid();
    }

    private void drawCircleOutline(GuideRenderPrimitive.DrawCircleOutline dco) {
        float half = Math.max(0.5f, dco.thickness() * 0.5f);
        float inner = Math.max(0f, dco.radius() - half);
        float outer = dco.radius() + half;
        setupSolid();
        color(dco.argb());
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double a = Math.PI * 2.0 * i / CIRCLE_SEGMENTS;
            float cos = (float) Math.cos(a);
            float sin = (float) Math.sin(a);
            tess.addVertex(dco.cx() + cos * inner, dco.cy() + sin * inner, 0);
            tess.addVertex(dco.cx() + cos * outer, dco.cy() + sin * outer, 0);
        }
        tess.draw();
        teardownSolid();
    }

    private void drawPolygon(GuideRenderPrimitive.DrawPolygon dp) {
        float[] xs = dp.xs();
        float[] ys = dp.ys();
        if (xs == null || ys == null || xs.length < 3) return;
        int n = Math.min(xs.length, ys.length);
        setupSolid();
        color(dp.argb());
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_FAN);
        tess.addVertex(xs[0], ys[0], 0);
        for (int i = 1; i < n; i++) {
            tess.addVertex(xs[i], ys[i], 0);
        }
        tess.draw();
        teardownSolid();
    }

    private void drawRenderItem(GuideRenderPrimitive.RenderItem ri) {
        flush();
        // TODO: Delegate to Minecraft's RenderItem when MC API is confirmed.
        // MC 1.7.10: RenderItem.renderItemAndEffectIntoGUI(fr, tm, stack, x, y)
    }

    private void drawText(GuideRenderPrimitive.DrawText dt) {
        flush();
        Minecraft mc = Minecraft.getMinecraft();
        String text = dt.text();
        if (text == null || text.isEmpty()) return;
        int color = dt.argb() | 0xFF000000; // ensure opaque
        if (dt.shadow()) {
            mc.fontRenderer.drawStringWithShadow(text, dt.x(), dt.y(), color);
        } else {
            mc.fontRenderer.drawString(text, dt.x(), dt.y(), color);
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
        if (!scissorStack.isEmpty()) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            glScissor(scissorStack.peek());
        }
    }

    private void drawHostDraw(GuideRenderPrimitive.HostDraw hd) {
        flush();
        // Phase 1: HostDraw calls use a simple callback registry.
        // In future phases, route via GuideRenderEngine's callback manager.
        HostDrawCallback callback = hostCallbackRegistry.remove(hd.callbackId());
        if (callback != null) {
            callback.draw(hd.x(), hd.y(), hd.w(), hd.h());
        }
    }

    private java.util.Map<Integer, HostDrawCallback> hostCallbackRegistry = new java.util.HashMap<>();
    private int nextCallbackId = 1;

    public int registerHostDrawCallback(HostDrawCallback callback) {
        int id = nextCallbackId++;
        hostCallbackRegistry.put(id, callback);
        return id;
    }

    @FunctionalInterface
    public interface HostDrawCallback {

        void draw(int x, int y, int w, int h);
    }

    private record Transform(int tx, int ty, float scale) {}
}
