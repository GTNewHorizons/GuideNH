package com.hfstudio.guidenh.client.capture.geometry;

import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureResizeEdges;
import com.hfstudio.guidenh.client.capture.api.CaptureSnapOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;

public class CaptureSnapResolver {

    private AxisLock horizontal;
    private AxisLock vertical;

    public void reset() {
        horizontal = null;
        vertical = null;
    }

    public CaptureRect snap(CaptureRect rectangle, List<CaptureTarget> targets, CaptureSnapOptions options,
        CaptureResizeEdges edges, boolean moving) {
        if (!options.enabled()) {
            reset();
            return rectangle;
        }
        horizontal = resolve(
            rectangle,
            targets,
            options,
            horizontal,
            true,
            edges.left() || moving,
            edges.right() || moving);
        vertical = resolve(
            rectangle,
            targets,
            options,
            vertical,
            false,
            edges.top() || moving,
            edges.bottom() || moving);
        double dx = horizontal == null ? 0 : horizontal.coordinate() - edge(rectangle, true, horizontal.end());
        double dy = vertical == null ? 0 : vertical.coordinate() - edge(rectangle, false, vertical.end());
        if (moving) return rectangle.move(dx, dy);
        return CaptureRect.between(
            rectangle.x() + (edges.left() ? dx : 0),
            rectangle.y() + (edges.top() ? dy : 0),
            rectangle.right() + (edges.right() ? dx : 0),
            rectangle.bottom() + (edges.bottom() ? dy : 0));
    }

    private AxisLock resolve(CaptureRect rectangle, List<CaptureTarget> targets, CaptureSnapOptions options,
        AxisLock held, boolean xAxis, boolean startEnabled, boolean endEnabled) {
        if (!startEnabled && !endEnabled) return null;
        if (held != null && (held.end() ? endEnabled : startEnabled)
            && validLock(held, rectangle, targets, options, xAxis)
            && Math.abs(held.coordinate() - edge(rectangle, xAxis, held.end()))
                <= Math.max(options.acquireDistance(), options.releaseDistance()))
            return held;
        AxisLock nearest = null;
        double distance = options.acquireDistance();
        for (CaptureTarget target : targets) {
            if (!options.accepts(target)) continue;
            CaptureRect bounds = target.visibleBounds();
            if (!nearby(rectangle, bounds, xAxis, distance)) continue;
            double start = xAxis ? bounds.x() : bounds.y();
            double end = xAxis ? bounds.right() : bounds.bottom();
            for (int side = 0; side < 2; side++) {
                boolean useEnd = side == 1;
                if (useEnd ? !endEnabled : !startEnabled) continue;
                double ownEdge = edge(rectangle, xAxis, useEnd);
                double startDistance = Math.abs(start - ownEdge);
                double endDistance = Math.abs(end - ownEdge);
                if (startDistance <= distance) {
                    nearest = new AxisLock(start, useEnd, target.id());
                    distance = startDistance;
                }
                if (endDistance <= distance) {
                    nearest = new AxisLock(end, useEnd, target.id());
                    distance = endDistance;
                }
            }
        }
        return nearest;
    }

    private static double edge(CaptureRect rectangle, boolean xAxis, boolean end) {
        return xAxis ? end ? rectangle.right() : rectangle.x() : end ? rectangle.bottom() : rectangle.y();
    }

    private static boolean nearby(CaptureRect rectangle, CaptureRect bounds, boolean xAxis, double distance) {
        return xAxis ? rectangle.bottom() + distance >= bounds.y() && rectangle.y() - distance <= bounds.bottom()
            : rectangle.right() + distance >= bounds.x() && rectangle.x() - distance <= bounds.right();
    }

    private static boolean validLock(AxisLock held, CaptureRect rectangle, List<CaptureTarget> targets,
        CaptureSnapOptions options, boolean xAxis) {
        for (CaptureTarget target : targets) {
            if (!target.id()
                .equals(held.targetId()) || !options.accepts(target)) continue;
            CaptureRect bounds = target.visibleBounds();
            if (!nearby(rectangle, bounds, xAxis, Math.max(options.acquireDistance(), options.releaseDistance())))
                return false;
            return held.coordinate() == (xAxis ? bounds.x() : bounds.y())
                || held.coordinate() == (xAxis ? bounds.right() : bounds.bottom());
        }
        return false;
    }

    public record AxisLock(double coordinate, boolean end, String targetId) {

        public AxisLock(double coordinate, boolean end) {
            this(coordinate, end, "");
        }
    }
}
