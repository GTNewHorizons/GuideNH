package com.hfstudio.guidenh.client.capture.api;

import java.util.List;

@FunctionalInterface
public interface CaptureGeometryCatalog {

    String DRAW_CALLS = "rendered_draws";
    String DRAW_BUTTONS = "rendered_buttons";
    String PIXELS = "pixel_coverage";
    String TOOLTIPS = "captured_tooltips";
    String VANILLA_HUD = "vanilla_hud";
    List<String> DERIVED_IDS = List.of(DRAW_CALLS, DRAW_BUTTONS, PIXELS, TOOLTIPS, VANILLA_HUD);
    CaptureGeometryCatalog EMPTY = List::of;

    List<String> geometryProviderIds();
}
