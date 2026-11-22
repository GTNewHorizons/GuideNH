package com.hfstudio.guidenh.client.capture.ui;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record CaptureSetting(String key, Supplier<String> value, Consumer<String> update, List<String> choices,
    boolean recapture) {

    public CaptureSetting {
        choices = List.copyOf(choices);
    }

    public void cycle(int direction) {
        int current = choices.indexOf(value.get());
        update.accept(choices.get(Math.floorMod(current + direction, choices.size())));
    }
}
