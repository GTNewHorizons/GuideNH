package com.hfstudio.guidenh.client.capture.export;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureResolution;
import com.hfstudio.guidenh.client.capture.api.CaptureScrollGeometry;

public record CaptureScrollSlice(String group, CaptureRect crop, CaptureRect destination, double contentWidth,
    double contentHeight, double pixelScaleX, double pixelScaleY, long regionId) {

    public static final String GROUP = "capture.scroll.group";

    public static CaptureScrollSlice from(CaptureExportRequest request, CaptureScrollGeometry geometry,
        boolean separate) {
        CaptureComposition composition = CaptureRowComposer.compose(request);
        var candidates = separate ? composition.regions() : composition.placements();
        CapturePlacement placement = null;
        double area = 0;
        int region = 0;
        int selectedRegion = 0;
        CaptureRect wanted = request.viewport()
            .pixels(geometry.viewport());
        for (CapturePlacement candidate : candidates) {
            CaptureRect intersection = wanted.intersect(
                new CaptureRect(candidate.sourceX(), candidate.sourceY(), candidate.width(), candidate.height()));
            double visibleArea = intersection.width() * intersection.height();
            if (visibleArea > area) {
                area = visibleArea;
                placement = candidate;
                selectedRegion = region;
            }
            region++;
        }
        if (placement == null) return null;
        CaptureRect bounds = new CaptureRect(
            placement.sourceX() / request.viewport()
                .scaleX(),
            placement.sourceY() / request.viewport()
                .scaleY(),
            placement.width() / request.viewport()
                .scaleX(),
            placement.height() / request.viewport()
                .scaleY());
        CaptureRect visible = geometry.viewport()
            .intersect(bounds)
            .intersect(
                request.viewport()
                    .bounds());
        if (visible.isEmpty()) return null;
        CaptureRect pixels = request.viewport()
            .pixels(visible);
        CaptureResolution resolution = request.options()
            .resolution();
        CaptureOutputSize output = resolution.resolve(
            composition.size()
                .width(),
            composition.size()
                .height());
        double actualX = (double) output.width() / composition.size()
            .width();
        double actualY = (double) output.height() / composition.size()
            .height();
        int left = (int) Math.ceil((pixels.x() - placement.sourceX() + placement.destinationX()) * actualX);
        int top = (int) Math.ceil((pixels.y() - placement.sourceY() + placement.destinationY()) * actualY);
        int right = (int) Math.ceil((pixels.right() - placement.sourceX() + placement.destinationX()) * actualX);
        int bottom = (int) Math.ceil((pixels.bottom() - placement.sourceY() + placement.destinationY()) * actualY);
        if (right <= left || bottom <= top) return null;
        if (separate) {
            int originX = (int) Math.ceil(placement.destinationX() * actualX);
            int originY = (int) Math.ceil(placement.destinationY() * actualY);
            left -= originX;
            right -= originX;
            top -= originY;
            bottom -= originY;
        }
        long regionId = separate ? request.regions()
            .stream()
            .filter(
                value -> !value.erased() && !request.viewport()
                    .pixels(value.bounds())
                    .isEmpty())
            .toList()
            .get(selectedRegion)
            .id() : 0;
        boolean custom = resolution.requestedWidth() > 0 || resolution.requestedHeight() > 0;
        return new CaptureScrollSlice(
            geometry.groupId(),
            new CaptureRect(left, top, right - left, bottom - top),
            new CaptureRect(
                geometry.offsetX() + visible.x()
                    - geometry.viewport()
                        .x(),
                geometry.offsetY() + visible.y()
                    - geometry.viewport()
                        .y(),
                visible.width(),
                visible.height()),
            geometry.contentWidth(),
            geometry.contentHeight(),
            request.viewport()
                .scaleX() * (custom ? actualX : resolution.scaleX()),
            request.viewport()
                .scaleY() * (custom ? actualY : resolution.scaleY()),
            regionId);
    }

    public Map<String, String> metadata() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(GROUP, group);
        data.put("capture.scroll.region", Long.toString(regionId));
        rectangle(data, "crop", crop);
        rectangle(data, "destination", destination);
        data.put("capture.scroll.content.width", Double.toString(contentWidth));
        data.put("capture.scroll.content.height", Double.toString(contentHeight));
        data.put("capture.scroll.scale.x", Double.toString(pixelScaleX));
        data.put("capture.scroll.scale.y", Double.toString(pixelScaleY));
        return Map.copyOf(data);
    }

    private static void rectangle(Map<String, String> data, String name, CaptureRect rectangle) {
        data.put("capture.scroll." + name + ".x", Double.toString(rectangle.x()));
        data.put("capture.scroll." + name + ".y", Double.toString(rectangle.y()));
        data.put("capture.scroll." + name + ".width", Double.toString(rectangle.width()));
        data.put("capture.scroll." + name + ".height", Double.toString(rectangle.height()));
    }

    public static CaptureScrollSlice parse(Map<String, String> data) {
        if (!data.containsKey(GROUP)) return null;
        return new CaptureScrollSlice(
            data.get(GROUP),
            rectangle(data, "crop"),
            rectangle(data, "destination"),
            number(data, "content.width"),
            number(data, "content.height"),
            number(data, "scale.x"),
            number(data, "scale.y"),
            Long.parseLong(data.get("capture.scroll.region")));
    }

    private static CaptureRect rectangle(Map<String, String> data, String name) {
        return new CaptureRect(
            number(data, name + ".x"),
            number(data, name + ".y"),
            number(data, name + ".width"),
            number(data, name + ".height"));
    }

    private static double number(Map<String, String> data, String name) {
        return Double.parseDouble(data.get("capture.scroll." + name));
    }
}
