package com.hfstudio.guidenh.libs.mdast.gfm.model;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPhrasingContent;

public class GfmTableCell extends MdAstParent<MdAstPhrasingContent> implements MdAstAnyContent {

    public static final String TYPE = "tableCell";

    public int columnSpan = 1;

    public int rowSpan = 1;

    public int resolvedColumnIndex;

    public int resolvedRowIndex;

    public final Map<String, String> attributes = new LinkedHashMap<>();

    public GfmTableCell() {
        super(TYPE);
    }

    @Override
    protected Class<MdAstPhrasingContent> childClass() {
        return MdAstPhrasingContent.class;
    }

    @Override
    protected void writeJson(JsonWriter writer) throws IOException {
        if (columnSpan > 1) {
            writer.name("columnSpan")
                .value(columnSpan);
        }
        if (rowSpan > 1) {
            writer.name("rowSpan")
                .value(rowSpan);
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
        super.writeJson(writer);
    }

    @Override
    protected void readJson(JsonObject jsonObject) throws IOException {
        var columnSpan = jsonObject.getAsJsonPrimitive("columnSpan");
        this.columnSpan = columnSpan == null ? 1 : Math.max(1, columnSpan.getAsInt());
        var rowSpan = jsonObject.getAsJsonPrimitive("rowSpan");
        this.rowSpan = rowSpan == null ? 1 : Math.max(1, rowSpan.getAsInt());
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
        super.readJson(jsonObject);
    }
}
