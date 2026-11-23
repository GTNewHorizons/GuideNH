package com.hfstudio.guidenh.client.capture.ui;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record CaptureSetting(String key, Supplier<String> value, Consumer<String> update,
    Supplier<List<String>> choiceSource, boolean recapture, CaptureSliderRange slider) {

    public CaptureSetting(String key, Supplier<String> value, Consumer<String> update,
        Supplier<List<String>> choiceSource, boolean recapture) {
        this(key, value, update, choiceSource, recapture, null);
    }

    public CaptureSetting(String key, Supplier<String> value, Consumer<String> update, List<String> choices,
        boolean recapture) {
        this(key, value, update, immutableChoices(choices), recapture, null);
    }

    public CaptureSetting withSlider(double minimum, double maximum, double step) {
        return new CaptureSetting(
            key,
            value,
            update,
            choiceSource,
            recapture,
            new CaptureSliderRange(minimum, maximum, step));
    }

    private static Supplier<List<String>> immutableChoices(List<String> choices) {
        List<String> values = List.copyOf(choices);
        return () -> values;
    }

    public List<String> choices() {
        return List.copyOf(choiceSource.get());
    }

    public void cycle(int direction) {
        List<String> choices = choices();
        int current = choices.indexOf(value.get());
        update.accept(choices.get(Math.floorMod(current + direction, choices.size())));
    }
}
