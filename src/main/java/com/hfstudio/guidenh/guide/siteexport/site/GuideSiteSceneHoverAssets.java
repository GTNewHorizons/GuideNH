package com.hfstudio.guidenh.guide.siteexport.site;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class GuideSiteSceneHoverAssets {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .serializeNulls()
        .create();
    private static final String BINDINGS_BUCKET = "scene-hover-targets";
    private static final int FORMAT_VERSION = 2;

    private GuideSiteSceneHoverAssets() {}

    public static String export(String hoverTargetsJson, GuideSiteAssetRegistry assets) throws Exception {
        JsonArray targets = new JsonParser().parse(hoverTargetsJson != null ? hoverTargetsJson : "[]")
            .getAsJsonArray();
        JsonArray styles = new JsonArray();
        JsonArray rows = new JsonArray();
        JsonArray templates = new JsonArray();
        JsonArray templateSlots = new JsonArray();
        Map<JsonObject, Integer> styleIds = new LinkedHashMap<>();
        Map<String, Integer> templateIds = new LinkedHashMap<>();
        for (JsonElement element : targets) {
            JsonObject style = element.getAsJsonObject();
            JsonElement minCorner = style.remove("minCorner");
            JsonElement maxCorner = style.remove("maxCorner");
            JsonElement blockPos = style.remove("blockPos");
            JsonElement template = style.remove("contentTemplateId");
            Integer styleId = styleIds.get(style);
            if (styleId == null) {
                styleId = styles.size();
                styleIds.put(style, styleId);
                styles.add(style);
            }
            int templateId = -1;
            if (template != null && !template.isJsonNull()) {
                String name = template.getAsString();
                Integer existing = templateIds.get(name);
                if (existing == null) {
                    existing = templates.size();
                    templateIds.put(name, existing);
                    templates.add(template);
                }
                templateId = existing;
            }
            templateSlots.add(new JsonPrimitive(templateId));
            // Unit cubes need only a style and block position; other targets retain exact bounds.
            JsonArray row = new JsonArray();
            row.add(new JsonPrimitive(styleId));
            row.add(blockPos != null ? blockPos : JsonNull.INSTANCE);
            if (!isUnitCube(blockPos, minCorner, maxCorner)) {
                row.add(minCorner);
                row.add(maxCorner);
            }
            rows.add(row);
        }
        JsonObject geometry = new JsonObject();
        geometry.addProperty("version", FORMAT_VERSION);
        geometry.add("styles", styles);
        geometry.add("targets", rows);
        String geometryPath = assets.writeSharedCompressed(
            "scene-hover-geometry",
            ".json",
            GSON.toJson(geometry)
                .getBytes(StandardCharsets.UTF_8));
        JsonObject bindings = new JsonObject();
        bindings.addProperty("version", FORMAT_VERSION);
        bindings.addProperty(
            "geometrySrc",
            Path.of("_res", BINDINGS_BUCKET)
                .relativize(Path.of(geometryPath))
                .toString()
                .replace('\\', '/'));
        bindings.add("templates", templates);
        bindings.add("templateSlots", templateSlots);
        return assets.writeSharedCompressed(
            BINDINGS_BUCKET,
            ".json",
            GSON.toJson(bindings)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isUnitCube(JsonElement blockPos, JsonElement minCorner, JsonElement maxCorner) {
        if (blockPos == null || !blockPos.isJsonArray()
            || minCorner == null
            || !minCorner.isJsonArray()
            || maxCorner == null
            || !maxCorner.isJsonArray()) {
            return false;
        }
        JsonArray position = blockPos.getAsJsonArray();
        JsonArray minimum = minCorner.getAsJsonArray();
        JsonArray maximum = maxCorner.getAsJsonArray();
        if (position.size() != 3 || minimum.size() != 3 || maximum.size() != 3) {
            return false;
        }
        for (int axis = 0; axis < 3; axis++) {
            double coordinate = position.get(axis)
                .getAsDouble();
            if (minimum.get(axis)
                .getAsDouble() != coordinate
                || maximum.get(axis)
                    .getAsDouble() != coordinate + 1d) {
                return false;
            }
        }
        return true;
    }
}
