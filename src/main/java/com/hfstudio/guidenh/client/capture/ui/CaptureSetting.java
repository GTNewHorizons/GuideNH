package com.hfstudio.guidenh.client.capture.ui;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.StatCollector;

public record CaptureSetting(String key, Supplier<String> value, Consumer<String> update,
    Supplier<List<String>> choiceSource, boolean recapture, CaptureSliderRange slider, CaptureSettingSection section) {

    public static final String PROVIDER_PREFIX = "snap_provider:";

    public CaptureSetting(String key, Supplier<String> value, Consumer<String> update,
        Supplier<List<String>> choiceSource, boolean recapture, CaptureSliderRange slider) {
        this(key, value, update, choiceSource, recapture, slider, CaptureSettingSection.forKey(key));
    }

    public CaptureSetting {
        if (section == null) throw new IllegalArgumentException("A setting section is required");
    }

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
            new CaptureSliderRange(minimum, maximum, step),
            section);
    }

    private static Supplier<List<String>> immutableChoices(List<String> choices) {
        List<String> values = List.copyOf(choices);
        return () -> values;
    }

    public List<String> choices() {
        return List.copyOf(choiceSource.get());
    }

    public String label() {
        if (!key.startsWith(PROVIDER_PREFIX)) return I18n.format("guidenh.capture.setting." + key);
        String id = key.substring(PROVIDER_PREFIX.length());
        String translationKey = "guidenh.capture.value." + id;
        String name = StatCollector.canTranslate(translationKey) ? I18n.format(translationKey) : id;
        return I18n.format("guidenh.capture.setting.snap_provider", name);
    }

    public CaptureSetting withSection(CaptureSettingSection section) {
        return new CaptureSetting(key, value, update, choiceSource, recapture, slider, section);
    }

    public void cycle(int direction) {
        List<String> choices = choices();
        int current = choices.indexOf(value.get());
        update.accept(choices.get(Math.floorMod(current + direction, choices.size())));
    }
}
