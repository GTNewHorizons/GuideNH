package com.hfstudio.guidenh.bridge;

import java.util.Set;

import lombok.Getter;

@Getter
public class GuideNhRuntimeBridgeSettings {

    private final boolean enabled;
    private final String host;
    private final int port;
    private final String token;
    private final int maxMessageBytes;
    private final int maxPageSize;
    private final int maxSubscriptions;
    private final int maxConnections;
    private final int maxDeltaEntries;
    private final Set<String> allowedOrigins;

    public GuideNhRuntimeBridgeSettings(boolean enabled, String host, int port, String token, int maxMessageBytes,
        int maxPageSize, int maxSubscriptions, int maxConnections, int maxDeltaEntries) {
        this(
            enabled,
            host,
            port,
            token,
            maxMessageBytes,
            maxPageSize,
            maxSubscriptions,
            maxConnections,
            maxDeltaEntries,
            Set.of());
    }

    public GuideNhRuntimeBridgeSettings(boolean enabled, String host, int port, String token, int maxMessageBytes,
        int maxPageSize, int maxSubscriptions, int maxConnections, int maxDeltaEntries, Set<String> allowedOrigins) {
        this.enabled = enabled;
        this.host = host == null ? "" : host.trim();
        this.port = port;
        this.token = token == null ? "" : token;
        this.maxMessageBytes = maxMessageBytes;
        this.maxPageSize = maxPageSize;
        this.maxSubscriptions = maxSubscriptions;
        this.maxConnections = maxConnections;
        this.maxDeltaEntries = maxDeltaEntries;
        this.allowedOrigins = Set.copyOf(allowedOrigins);
    }

    public boolean canStart() {
        return enabled && !host.isEmpty() && port > 0 && port <= 65535 && !token.isEmpty();
    }

}
