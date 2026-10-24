package com.hfstudio.guidenh.libs.mdast.gfm.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonWriter;
import com.hfstudio.guidenh.libs.mdast.model.MdAstFlowContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.micromark.extensions.gfm.Align;

public class GfmTable extends MdAstParent<GfmTableRow> implements MdAstFlowContent {

    public static final String TYPE = "table";

    @Nullable
    public List<Align> align = null;

    public final Map<String, String> attributes = new LinkedHashMap<>();

    public boolean headerless;

    public GfmTable() {
        super(TYPE);
    }

    @Override
    protected Class<GfmTableRow> childClass() {
        return GfmTableRow.class;
    }

    @Override
    protected void writeJson(JsonWriter writer) throws IOException {
        if (headerless) {
            writer.name("headerless")
                .value(true);
        }
        if (!attributes.isEmpty()) {
            writer.name("attributes")
                .beginObject();
            for (var entry : attributes.entrySet()) {
                writer.name(entry.getKey())
                    .value(entry.getValue());
            }
            writer.endObject();
        }
        if (align != null) {
            writer.name("align")
                .beginArray();
            for (var value : align) {
                switch (value) {
                    case LEFT -> writer.value("left");
                    case CENTER -> writer.value("center");
                    case RIGHT -> writer.value("right");
                    case NONE -> writer.nullValue();
                }
            }
            writer.endArray();
        }

        super.writeJson(writer);
    }

    @Override
    protected void readJson(JsonObject jsonObject) throws IOException {
        var headerless = jsonObject.getAsJsonPrimitive("headerless");
        this.headerless = headerless != null && headerless.getAsBoolean();
        var attributes = jsonObject.getAsJsonObject("attributes");
        if (attributes != null) {
            this.attributes.clear();
            for (var entry : attributes.entrySet()) {
                if (entry.getValue()
                    .isJsonPrimitive()) {
                    this.attributes.put(
                        entry.getKey(),
                        entry.getValue()
                            .getAsString());
                }
            }
        }
        var align = jsonObject.getAsJsonArray("align");
        if (align != null) {
            this.align = new ArrayList<>();
            for (var alignEl : align) {
                if (alignEl.isJsonNull()) {
                    this.align.add(Align.NONE);
                } else switch (alignEl.getAsString()) {
                    case "left" -> this.align.add(Align.LEFT);
                    case "center" -> this.align.add(Align.CENTER);
                    case "right" -> this.align.add(Align.RIGHT);
                    default -> throw new JsonSyntaxException("Unknown table align: " + alignEl);
                }
            }
        }

        super.readJson(jsonObject);
    }
}
