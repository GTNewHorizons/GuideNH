package com.hfstudio.guidenh.guide.editor;

import com.hfstudio.guidenh.guide.color.ColorUtils;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Reusable horizontal slider implementation for a registered Scene Editor dropdown item.
 *
 * <p>The value supplier is read during rendering and the consumer is called with a clamped value
 * whenever the track is clicked or dragged. Slider coordinates are expressed in the row supplied by
 * the menu; subclasses may override the sizing and hit-test methods to provide a different layout.</p>
 */
public class SceneEditorSliderWidget implements SceneEditorMenuWidget {

    protected DoubleSupplier value;
    protected DoubleConsumer valueConsumer;
    protected double minimum;
    protected double maximum;
    protected String label;

    /** Creates a slider whose emitted values are clamped to the inclusive minimum/maximum range. */
    public SceneEditorSliderWidget(DoubleSupplier value, DoubleConsumer valueConsumer, double minimum, double maximum,
        String label) {
        if (minimum > maximum) throw new IllegalArgumentException("minimum must not exceed maximum");
        this.value = value;
        this.valueConsumer = valueConsumer;
        this.minimum = minimum;
        this.maximum = maximum;
        this.label = label == null ? "" : label;
    }

    /** Returns the pixel height reserved for this slider row. */
    @Override
    public int height() {
        return 26;
    }

    /** Returns the preferred pixel width for this slider row. */
    @Override
    public int preferredWidth() {
        return 150;
    }

    @Override
    public void render(SceneEditorMenuWidgetContext context, int x, int y, int width, int height, boolean hovered,
        boolean enabled) {
        int color = enabled ? ColorUtils.PANEL_MUTED_TEXT.getColor() : ColorUtils.ARGB_FF737A82.getColor();
        context.drawString(label, x + 6, y + 3, color);
        int trackY = y + 16;
        int trackLeft = x + 6;
        int trackRight = x + width - 6;
        context.drawRect(trackLeft, trackY, trackRight, trackY + 3, ColorUtils.INPUT_BORDER.getColor());
        double current = value.getAsDouble();
        double fraction = maximum <= minimum ? 0d : (current - minimum) / (maximum - minimum);
        fraction = Math.max(0d, Math.min(1d, fraction));
        int thumbX = trackLeft + (int) Math.round((trackRight - trackLeft) * fraction);
        context.drawRect(thumbX - 2, trackY - 2, thumbX + 3, trackY + 5, enabled ? ColorUtils.ACCENT.getColor() : ColorUtils.ARGB_FF737A82.getColor());
    }

    @Override
    public boolean mouseClicked(SceneEditorActionContext actionContext, int x, int y, int width, int height, int mouseX,
        int mouseY, int button) {
        if (button != 0 || !insideTrack(x, y, width, height, mouseX, mouseY)) return false;
        applyAt(x, width, mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(SceneEditorActionContext actionContext, int x, int y, int width, int height, int mouseX,
        int mouseY, int button) {
        if (button != 0) return false;
        applyAt(x, width, mouseX);
        return true;
    }

    protected boolean insideTrack(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x + 2 && mouseX < x + width - 2 && mouseY >= y + 12 && mouseY < y + height;
    }

    protected void applyAt(int x, int width, int mouseX) {
        double fraction = (mouseX - (x + 6d)) / Math.max(1d, width - 12d);
        fraction = Math.max(0d, Math.min(1d, fraction));
        valueConsumer.accept(minimum + (maximum - minimum) * fraction);
    }
}
