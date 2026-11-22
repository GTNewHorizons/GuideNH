package com.hfstudio.guidenh.client.capture.ui;

import java.util.function.BooleanSupplier;

public record CaptureOverlayActions(Runnable save, Runnable record, Runnable pause, Runnable stop, Runnable schedule,
    Runnable close, BooleanSupplier scheduled) {}
