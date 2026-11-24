package com.hfstudio.guidenh.capture.server.api;

import java.util.List;

public interface CaptureServerObjectProvider {

    String id();

    List<CaptureServerObjectTarget> enumerate(CaptureServerContext context);
}
