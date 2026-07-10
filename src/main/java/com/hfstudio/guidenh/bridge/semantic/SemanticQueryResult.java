package com.hfstudio.guidenh.bridge.semantic;

import java.util.List;
import java.util.Map;

import lombok.Getter;

@Getter
public class SemanticQueryResult {

    private final String capability;
    private final int version;
    private final List<Map<String, String>> entries;
    private final String nextCursor;

    public SemanticQueryResult(String capability, int version, List<Map<String, String>> entries, String nextCursor) {
        this.capability = capability;
        this.version = version;
        this.entries = entries;
        this.nextCursor = nextCursor;
    }

}
