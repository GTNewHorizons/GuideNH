package com.hfstudio.guidenh.bridge.protocol;

import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public class BridgeMessageCodec {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .create();

    private final BridgeProtocolLimits limits;

    public BridgeMessageCodec(BridgeProtocolLimits limits) {
        this.limits = limits;
    }

    public BridgeEnvelope decode(String message) {
        if (message == null) {
            throw new IllegalArgumentException("Message must not be null");
        }
        if (message.getBytes(StandardCharsets.UTF_8).length > limits.getMaxMessageBytes()) {
            throw new IllegalArgumentException("Message exceeds maximum size");
        }
        validateNesting(message);
        BridgeEnvelope envelope;
        try {
            envelope = GSON.fromJson(message, BridgeEnvelope.class);
        } catch (JsonParseException error) {
            throw new IllegalArgumentException("Invalid bridge JSON", error);
        }
        if (envelope == null || envelope.getMethod() == null
            || envelope.getType() == null
            || envelope.getProtocol() != 1) {
            throw new IllegalArgumentException("Invalid bridge envelope");
        }
        return envelope;
    }

    private static void validateNesting(String text) {
        boolean quoted = false;
        boolean escaped = false;
        int depth = 0;
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (escaped) escaped = false;
            else if (quoted && value == '\\') escaped = true;
            else if (value == '"') quoted = !quoted;
            else if (!quoted) {
                if (value == '{' || value == '[') {
                    if (++depth > 64) throw new IllegalArgumentException("Bridge JSON exceeds its nesting limit");
                } else if (value == '}' || value == ']') {
                    if (--depth < 0) throw new IllegalArgumentException("Unbalanced bridge JSON delimiters");
                }
            }
        }
        if (quoted || depth != 0) throw new IllegalArgumentException("Unbalanced bridge JSON delimiters");
    }

    public String encode(Object value) {
        String json = GSON.toJson(value);
        if (json.getBytes(StandardCharsets.UTF_8).length > limits.getMaxMessageBytes()) {
            throw new IllegalArgumentException("Encoded message exceeds maximum size");
        }
        return json;
    }
}
