package com.hfstudio.guidenh.client.capture.api;

import java.util.List;

public interface CaptureFormatCatalog {

    public List<String> formats();

    public boolean animationFormat(String format);
}
