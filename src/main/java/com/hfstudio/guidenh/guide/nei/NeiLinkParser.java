package com.hfstudio.guidenh.guide.nei;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IdUtils;

public class NeiLinkParser {

    private NeiLinkParser() {}

    public static boolean isNeiLink(String href) {
        return href != null && href.regionMatches(true, 0, "nei:", 0, 4);
    }

    public static NeiLinkTarget parseUri(String href, String defaultNamespace) {
        if (!isNeiLink(href) || href.indexOf('#') >= 0) {
            throw new IllegalArgumentException("Expected a NEI action without a fragment.");
        }
        int queryStart = href.indexOf('?', 4);
        String action = href.substring(4, queryStart < 0 ? href.length() : queryStart);
        Map<String, String> parameters = new LinkedHashMap<>();
        if (queryStart >= 0) {
            for (String pair : href.substring(queryStart + 1)
                .split("&", -1)) {
                int separator = pair.indexOf('=');
                if (separator <= 0) {
                    throw new IllegalArgumentException("NEI parameters must have a name and value.");
                }
                String name = decode(pair.substring(0, separator));
                if (!"id".equals(name) && !"handlerId".equals(name)) {
                    throw new IllegalArgumentException("Unknown NEI parameter: " + name);
                }
                String value = decode(pair.substring(separator + 1));
                if (value.isBlank() || parameters.putIfAbsent(name, value) != null) {
                    throw new IllegalArgumentException("Blank or duplicate NEI parameter: " + name);
                }
            }
        }
        return parseAttributes(action, parameters.get("id"), parameters.get("handlerId"), defaultNamespace);
    }

    public static NeiLinkTarget parseAttributes(@Nullable String mode, @Nullable String itemId,
        @Nullable String handlerId, String defaultNamespace) {
        String item = itemId == null || itemId.isBlank() ? null : itemId.trim();
        String action = mode == null ? (item == null ? "handler" : "recipes")
            : mode.trim()
                .toLowerCase(Locale.ROOT);
        if ((itemId != null && itemId.chars()
            .anyMatch(Character::isISOControl)) || (handlerId != null
                && handlerId.chars()
                    .anyMatch(Character::isISOControl))) {
            throw new IllegalArgumentException("NEI targets must not contain control characters.");
        }
        if (item != null) {
            int nbtStart = item.indexOf('{');
            String head = nbtStart < 0 ? item : item.substring(0, nbtStart);
            if (nbtStart >= 0 && head.endsWith(":")) head = head.substring(0, head.length() - 1);
            var reference = IdUtils.parseItemRef(head, defaultNamespace);
            if (reference == null || reference.id()
                .getResourcePath()
                .isEmpty()) {
                throw new IllegalArgumentException("NEI item id must not be empty.");
            }
            if (nbtStart >= 0) {
                try {
                    if (!(JsonToNBT.func_150315_a(item.substring(nbtStart)) instanceof NBTTagCompound)) {
                        throw new IllegalArgumentException("NEI item SNBT must be a compound.");
                    }
                } catch (NBTException exception) {
                    throw new IllegalArgumentException("Invalid NEI item SNBT.", exception);
                }
            }
            if (head.indexOf(':') < 0) {
                item = defaultNamespace + ":" + item;
            }
        }
        return new NeiLinkTarget(action, item, handlerId);
    }

    private static String decode(String value) {
        StringBuilder decoded = new StringBuilder(value.length());
        for (int index = 0; index < value.length();) {
            if (value.charAt(index) != '%') {
                decoded.append(value.charAt(index++));
                continue;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            while (index < value.length() && value.charAt(index) == '%') {
                if (index + 2 >= value.length()) {
                    throw new IllegalArgumentException("Incomplete NEI percent escape.");
                }
                int high = Character.digit(value.charAt(index + 1), 16);
                int low = Character.digit(value.charAt(index + 2), 16);
                if (high < 0 || low < 0) {
                    throw new IllegalArgumentException("Invalid NEI percent escape.");
                }
                bytes.write((high << 4) | low);
                index += 3;
            }
            try {
                decoded.append(
                    StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes.toByteArray())));
            } catch (CharacterCodingException exception) {
                throw new IllegalArgumentException("NEI parameters must use valid UTF-8.", exception);
            }
        }
        return decoded.toString();
    }
}
