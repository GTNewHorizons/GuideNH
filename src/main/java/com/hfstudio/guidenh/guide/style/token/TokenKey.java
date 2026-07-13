package com.hfstudio.guidenh.guide.style.token;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Each node declares static final TokenKey fields at class init time.
 * The id is assigned sequentially — stable within one JVM lifetime.
 */
public final class TokenKey<T extends ResolvedValue> {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);

    private final int id;
    private final String name;
    private final TokenType type;
    private final T defaultValue;

    private TokenKey(int id, String name, TokenType type, T defaultValue) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    /** Register a new token key. Called from node static initializers. */
    public static <T extends ResolvedValue> TokenKey<T> define(
        String name, TokenType type, T defaultValue
    ) {
        int id = NEXT_ID.getAndIncrement();
        TokenKey<T> key = new TokenKey<>(id, name, type, defaultValue);
        ThemeRegistry.register(key);
        return key;
    }

    public int id()              { return id; }
    public String name()         { return name; }
    public TokenType type()   { return type; }
    public T defaultValue()      { return defaultValue; }

    @Override public String toString() { return name + "[" + id + "]"; }
}
