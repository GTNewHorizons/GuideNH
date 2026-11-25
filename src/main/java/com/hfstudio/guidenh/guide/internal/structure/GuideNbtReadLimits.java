package com.hfstudio.guidenh.guide.internal.structure;

public record GuideNbtReadLimits(int maxBytes, long maxAllocationBytes, int maxTags, int maxDepth) {

    public static final GuideNbtReadLimits DEFAULT = new GuideNbtReadLimits(
        32 * 1024 * 1024,
        256L * 1024L * 1024L,
        4_000_000,
        128);

    public GuideNbtReadLimits {
        if (maxBytes <= 0 || maxAllocationBytes <= 0 || maxTags <= 0 || maxDepth <= 0) {
            throw new IllegalArgumentException("Positive NBT decoding limits are required");
        }
    }

    public void validateText(String text) {
        if (text.length() > maxBytes) throw new IllegalArgumentException("SNBT exceeds its text limit");
        int depth = 0;
        int tokens = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (quoted && value == '\\') {
                escaped = true;
            } else if (value == '"') {
                quoted = !quoted;
            } else if (!quoted) {
                if (value == '{' || value == '[') {
                    if (++depth > maxDepth) throw new IllegalArgumentException("SNBT exceeds its nesting limit");
                } else if (value == '}' || value == ']') {
                    if (--depth < 0) throw new IllegalArgumentException("Unbalanced SNBT delimiters");
                }
                if ((value == '{' || value == '[' || value == ',' || value == ':') && ++tokens > maxTags) {
                    throw new IllegalArgumentException("SNBT exceeds its element limit");
                }
            }
        }
        if (quoted || depth != 0) throw new IllegalArgumentException("Unbalanced SNBT delimiters");
    }
}
