package com.hfstudio.guidenh.guide.internal.syntax;

import com.hfstudio.guidenh.guide.compiler.tags.PreCompiler;
import com.hfstudio.guidenh.guide.document.block.AlignItems;
import com.hfstudio.guidenh.guide.document.block.chart.ChartLabelPosition;
import com.hfstudio.guidenh.guide.document.block.chart.ChartLegendPosition;
import com.hfstudio.guidenh.guide.document.block.chart.CornerLegendPosition;
import com.hfstudio.guidenh.guide.document.block.functiongraph.AutoPointLabelMode;
import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockLanguageRegistry;
import com.hfstudio.guidenh.guide.internal.syntax.values.AnchorValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.AttributePresetValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.BlockIdValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.BooleanValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.ColorValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.CommandValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.DomainValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.EntityNameValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.ExpressionValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.FilePathValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.FormatPatternValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.ItemIdValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.KeyBindValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.ModIdValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.NumericPresetValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.OreDictValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.PagePathValueSource;
import com.hfstudio.guidenh.guide.internal.syntax.values.QuestIdValueSource;
import com.hfstudio.guidenh.guide.syntax.AttributeSyntax;
import com.hfstudio.guidenh.guide.syntax.MarkdownSnippet;
import com.hfstudio.guidenh.guide.syntax.SyntaxContributor;
import com.hfstudio.guidenh.guide.syntax.SyntaxSink;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;

/**
 * The guide syntax that ships with the mod.
 *
 * <p>
 * It is an ordinary {@link SyntaxContributor}: tag names arrive from the registered tag compilers, and
 * this class only adds the facts a compiler does not express. Attribute kinds drive which
 * {@code SyntaxValueSource} answers a completion request, so marking an attribute as an item id is all
 * it takes to get item completion for it.
 */
public class BuiltinSyntaxContributor implements SyntaxContributor {

    @Override
    public String namespace() {
        return "guidenh";
    }

    private static final AttributeSyntax RECIPE_INDEX = AttributeSyntax.of("recipeIndex", SyntaxValueKind.INT);
    private static final AttributeSyntax RECIPE_INPUT = AttributeSyntax.of("input", SyntaxValueKind.ITEM_ID);
    private static final AttributeSyntax RECIPE_OUTPUT = AttributeSyntax.of("output", SyntaxValueKind.ITEM_ID);
    private static final AttributeSyntax FORMED = AttributeSyntax.of("formed", SyntaxValueKind.BOOLEAN);
    private static final AttributeSyntax SHOW_WHEN_STRUCTURE = AttributeSyntax
        .of("showWhenStructure", SyntaxValueKind.STRING);
    private static final AttributeSyntax SHOW_WHEN_TIER = AttributeSyntax.of("showWhenTier", SyntaxValueKind.STRING);
    private static final AttributeSyntax SHOW_WHEN_CHANNELS = AttributeSyntax
        .of("showWhenChannels", SyntaxValueKind.STRING);
    private static final AttributeSyntax LAYOUT_WRAP = AttributeSyntax.of("wrap", SyntaxValueKind.STRING);
    private static final AttributeSyntax LAYOUT_ALIGN = AttributeSyntax.of("align", SyntaxValueKind.STRING);
    private static final AttributeSyntax LAYOUT_FLOAT = AttributeSyntax.of("float", SyntaxValueKind.STRING);
    private static final AttributeSyntax TEXT_VALUE = AttributeSyntax.of("value", SyntaxValueKind.STRING);
    private static final AttributeSyntax TEXT_EXPR = AttributeSyntax.of("expr", SyntaxValueKind.STRING);
    private static final AttributeSyntax TEXT_NAME = AttributeSyntax.of("name", SyntaxValueKind.STRING);
    private static final AttributeSyntax SPECIAL_PAGE = AttributeSyntax.of("page", SyntaxValueKind.STRING);
    private static final AttributeSyntax SPECIAL_PREFIX = AttributeSyntax.of("prefix", SyntaxValueKind.STRING);
    private static final AttributeSyntax SPECIAL_LANGUAGE = AttributeSyntax.of("language", SyntaxValueKind.STRING);
    private static final AttributeSyntax SPECIAL_QUERY = AttributeSyntax.of("query", SyntaxValueKind.STRING);
    private static final AttributeSyntax QUEST_SHOW_TOOLTIP = AttributeSyntax
        .of("showTooltip", SyntaxValueKind.BOOLEAN);
    private static final AttributeSyntax QUEST_SHOW_TOOLTIP_SNAKE = AttributeSyntax
        .of("show_tooltip", SyntaxValueKind.BOOLEAN);

    public void contribute(SyntaxSink sink) {
        // Inline/Flow tags
        sink.attributes(
            "ItemImage",
            AttributeSyntax.of("id", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("ore", SyntaxValueKind.ORE_DICT),
            AttributeSyntax.of("scale", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yOffset", SyntaxValueKind.INT),
            AttributeSyntax.of("labelYOffset", SyntaxValueKind.INT),
            AttributeSyntax.of("noTooltip", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showTooltip", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showIcon", SyntaxValueKind.STRING),
            AttributeSyntax.of("label", SyntaxValueKind.STRING),
            AttributeSyntax.of("format", SyntaxValueKind.FORMAT_PATTERN));
        sink.attributes(
            "ItemLink",
            AttributeSyntax.of("id", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("ore", SyntaxValueKind.ORE_DICT),
            AttributeSyntax.of("noTooltip", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showTooltip", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showText", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showIcon", SyntaxValueKind.STRING),
            AttributeSyntax.of("linksTo", SyntaxValueKind.PAGE_PATH));
        sink.attributes(
            "BlockImage",
            AttributeSyntax.of("id", SyntaxValueKind.BLOCK_ID),
            AttributeSyntax.of("ore", SyntaxValueKind.ORE_DICT),
            AttributeSyntax.of("scale", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("wrap", SyntaxValueKind.STRING),
            AttributeSyntax.of("align", SyntaxValueKind.STRING),
            AttributeSyntax.of("float", SyntaxValueKind.STRING));
        sink.attributes(
            "FloatingImage",
            AttributeSyntax.of("src", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("align", SyntaxValueKind.STRING),
            AttributeSyntax.of("wrap", SyntaxValueKind.STRING),
            AttributeSyntax.of("title", SyntaxValueKind.STRING),
            AttributeSyntax.of("alt", SyntaxValueKind.STRING),
            AttributeSyntax.of("x", SyntaxValueKind.INT),
            AttributeSyntax.of("y", SyntaxValueKind.INT),
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT),
            AttributeSyntax.of("w", SyntaxValueKind.INT),
            AttributeSyntax.of("h", SyntaxValueKind.INT),
            AttributeSyntax.of("scaleX", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("scaleY", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("displayWidth", SyntaxValueKind.INT),
            AttributeSyntax.of("displayHeight", SyntaxValueKind.INT),
            AttributeSyntax.of("sound", SyntaxValueKind.STRING),
            AttributeSyntax.of("soundSrc", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("trigger", SyntaxValueKind.STRING));
        sink.attributes(
            "Color",
            AttributeSyntax.of("id", SyntaxValueKind.COLOR),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR));
        sink.attributes(
            "KeyBind",
            AttributeSyntax.of("id", SyntaxValueKind.KEY_BIND),
            AttributeSyntax.of("action", SyntaxValueKind.STRING));
        sink.attributes(
            "CommandLink",
            AttributeSyntax.of("command", SyntaxValueKind.COMMAND),
            AttributeSyntax.of("close", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("title", SyntaxValueKind.STRING));
        sink.attributes(
            "Recipe",
            AttributeSyntax.of("id", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("fallbackText", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerName", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerId", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerOrder", SyntaxValueKind.INT),
            AttributeSyntax.of("input", SyntaxValueKind.STRING),
            AttributeSyntax.of("output", SyntaxValueKind.STRING),
            AttributeSyntax.of("limit", SyntaxValueKind.INT));
        sink.attributes(
            "RecipeFor",
            AttributeSyntax.of("id", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("fallbackText", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerName", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerId", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerOrder", SyntaxValueKind.INT),
            AttributeSyntax.of("input", SyntaxValueKind.STRING),
            AttributeSyntax.of("output", SyntaxValueKind.STRING),
            AttributeSyntax.of("limit", SyntaxValueKind.INT));
        sink.attributes(
            "RecipesFor",
            AttributeSyntax.of("id", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("fallbackText", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerName", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerId", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerOrder", SyntaxValueKind.INT),
            AttributeSyntax.of("input", SyntaxValueKind.STRING),
            AttributeSyntax.of("output", SyntaxValueKind.STRING),
            AttributeSyntax.of("limit", SyntaxValueKind.INT));
        sink.attributes(
            "SubPages",
            AttributeSyntax.of("id", SyntaxValueKind.PAGE_PATH),
            AttributeSyntax.of("alphabetical", SyntaxValueKind.BOOLEAN));
        sink.attributes(
            "Category",
            AttributeSyntax.of("name", SyntaxValueKind.STRING),
            AttributeSyntax.of("rows", SyntaxValueKind.INT));
        sink.attributes(
            "Special",
            AttributeSyntax.of("name", SyntaxValueKind.STRING),
            AttributeSyntax.of("rows", SyntaxValueKind.INT));
        sink.attributes(
            "Structure",
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT));
        sink.attributes(
            "Mermaid",
            AttributeSyntax.of("src", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT));
        sink.attributes(
            "CsvTable",
            AttributeSyntax.of("src", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("header", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("widths", SyntaxValueKind.STRING));
        sink.attributes("details", AttributeSyntax.of("open", SyntaxValueKind.BOOLEAN));
        sink.attributes(
            "ContentTabs",
            AttributeSyntax.of("title", SyntaxValueKind.STRING),
            AttributeSyntax.of("default", SyntaxValueKind.STRING),
            AttributeSyntax.of("defaultIndex", SyntaxValueKind.INT),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("icon", SyntaxValueKind.STRING),
            AttributeSyntax.of("iconPng", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("icon_png", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("iconItem", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("icon_item", SyntaxValueKind.ITEM_ID));
        sink.attributes("Tab", AttributeSyntax.of("title", SyntaxValueKind.STRING));
        sink.attributes(
            "Row",
            AttributeSyntax.of("gap", SyntaxValueKind.INT),
            AttributeSyntax.of("alignItems", SyntaxValueKind.ENUM, AlignItems.class),
            AttributeSyntax.of("fullWidth", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("width", SyntaxValueKind.INT));
        sink.attributes(
            "Column",
            AttributeSyntax.of("gap", SyntaxValueKind.INT),
            AttributeSyntax.of("alignItems", SyntaxValueKind.ENUM, AlignItems.class),
            AttributeSyntax.of("fullWidth", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("width", SyntaxValueKind.INT));
        sink.attributes(
            "a",
            AttributeSyntax.of("name", SyntaxValueKind.STRING),
            AttributeSyntax.of("href", SyntaxValueKind.PAGE_PATH),
            AttributeSyntax.of("title", SyntaxValueKind.STRING));
        sink.attributes("br", AttributeSyntax.of("clear", SyntaxValueKind.STRING));
        sink.attributes("ImportPonder", AttributeSyntax.of("src", SyntaxValueKind.FILE_PATH));
        sink.attributes("RemoveBlocks", AttributeSyntax.of("id", SyntaxValueKind.BLOCK_ID));
        sink.attributes(
            "IsometricCamera",
            AttributeSyntax.of("yaw", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("pitch", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("roll", SyntaxValueKind.FLOAT));
        sink.attributes("Tooltip", AttributeSyntax.of("label", SyntaxValueKind.STRING));
        sink.attributes("mark", AttributeSyntax.of("color", SyntaxValueKind.COLOR));
        sink.attributes(
            "FileTree",
            AttributeSyntax.of("indent", SyntaxValueKind.INT),
            AttributeSyntax.of("gap", SyntaxValueKind.INT));
        sink.attributes("ItemGrid"); // no attributes - uses child elements
        sink.attributes("FootnoteList", AttributeSyntax.of("width", SyntaxValueKind.INT));

        // Charts share CommonChartAttrs.
        sink.attributes(
            "BarChart",
            AttributeSyntax.of("title", SyntaxValueKind.STRING),
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT),
            AttributeSyntax.of("background", SyntaxValueKind.COLOR),
            AttributeSyntax.of("border", SyntaxValueKind.COLOR),
            AttributeSyntax.of("titleColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("labelColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("legend", SyntaxValueKind.ENUM, ChartLegendPosition.class),
            AttributeSyntax.of("labelPosition", SyntaxValueKind.ENUM, ChartLabelPosition.class),
            AttributeSyntax.of("cornerLegend", SyntaxValueKind.ENUM, CornerLegendPosition.class),
            AttributeSyntax.of("cornerLegendWidth", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendHeight", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendBackground", SyntaxValueKind.COLOR),
            AttributeSyntax.of("categories", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("showXGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showYGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("xGridColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("yGridColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("barWidthRatio", SyntaxValueKind.FLOAT));
        sink.attributes(
            "ColumnChart",
            AttributeSyntax.of("title", SyntaxValueKind.STRING),
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT),
            AttributeSyntax.of("background", SyntaxValueKind.COLOR),
            AttributeSyntax.of("border", SyntaxValueKind.COLOR),
            AttributeSyntax.of("titleColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("labelColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("legend", SyntaxValueKind.ENUM, ChartLegendPosition.class),
            AttributeSyntax.of("labelPosition", SyntaxValueKind.ENUM, ChartLabelPosition.class),
            AttributeSyntax.of("cornerLegend", SyntaxValueKind.ENUM, CornerLegendPosition.class),
            AttributeSyntax.of("cornerLegendWidth", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendHeight", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendBackground", SyntaxValueKind.COLOR),
            AttributeSyntax.of("categories", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("showXGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showYGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("xGridColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("yGridColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("barWidthRatio", SyntaxValueKind.FLOAT));
        sink.attributes(
            "LineChart",
            AttributeSyntax.of("title", SyntaxValueKind.STRING),
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT),
            AttributeSyntax.of("background", SyntaxValueKind.COLOR),
            AttributeSyntax.of("border", SyntaxValueKind.COLOR),
            AttributeSyntax.of("titleColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("labelColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("legend", SyntaxValueKind.ENUM, ChartLegendPosition.class),
            AttributeSyntax.of("labelPosition", SyntaxValueKind.ENUM, ChartLabelPosition.class),
            AttributeSyntax.of("cornerLegend", SyntaxValueKind.ENUM, CornerLegendPosition.class),
            AttributeSyntax.of("cornerLegendWidth", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendHeight", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendBackground", SyntaxValueKind.COLOR),
            AttributeSyntax.of("categories", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("showXGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showYGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("xGridColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("yGridColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("numericX", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showPoints", SyntaxValueKind.BOOLEAN));
        sink.attributes(
            "ScatterChart",
            AttributeSyntax.of("title", SyntaxValueKind.STRING),
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT),
            AttributeSyntax.of("background", SyntaxValueKind.COLOR),
            AttributeSyntax.of("border", SyntaxValueKind.COLOR),
            AttributeSyntax.of("titleColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("labelColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("legend", SyntaxValueKind.ENUM, ChartLegendPosition.class),
            AttributeSyntax.of("labelPosition", SyntaxValueKind.ENUM, ChartLabelPosition.class),
            AttributeSyntax.of("cornerLegend", SyntaxValueKind.ENUM, CornerLegendPosition.class),
            AttributeSyntax.of("cornerLegendWidth", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendHeight", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendBackground", SyntaxValueKind.COLOR),
            AttributeSyntax.of("xAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("showXGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showYGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("xGridColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("yGridColor", SyntaxValueKind.COLOR));
        sink.attributes(
            "PieChart",
            AttributeSyntax.of("title", SyntaxValueKind.STRING),
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT),
            AttributeSyntax.of("background", SyntaxValueKind.COLOR),
            AttributeSyntax.of("border", SyntaxValueKind.COLOR),
            AttributeSyntax.of("titleColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("labelColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("legend", SyntaxValueKind.ENUM, ChartLegendPosition.class),
            AttributeSyntax.of("labelPosition", SyntaxValueKind.ENUM, ChartLabelPosition.class),
            AttributeSyntax.of("cornerLegend", SyntaxValueKind.ENUM, CornerLegendPosition.class),
            AttributeSyntax.of("cornerLegendWidth", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendHeight", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendBackground", SyntaxValueKind.COLOR),
            AttributeSyntax.of("startAngle", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("clockwise", SyntaxValueKind.BOOLEAN));

        // Chart child tags.
        sink.attributes(
            "Series",
            AttributeSyntax.of("name", SyntaxValueKind.STRING),
            AttributeSyntax.of("data", SyntaxValueKind.STRING),
            AttributeSyntax.of("points", SyntaxValueKind.STRING),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("icon", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("iconImage", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("tooltip", SyntaxValueKind.STRING));
        sink.attributes(
            "LineSeries",
            AttributeSyntax.of("name", SyntaxValueKind.STRING),
            AttributeSyntax.of("data", SyntaxValueKind.STRING),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("icon", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("iconImage", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("tooltip", SyntaxValueKind.STRING));
        sink.attributes(
            "Slice",
            AttributeSyntax.of("name", SyntaxValueKind.STRING),
            AttributeSyntax.of("value", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("icon", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("iconImage", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("tooltip", SyntaxValueKind.STRING));
        sink.attributes(
            "PieInset",
            AttributeSyntax.of("size", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("position", SyntaxValueKind.STRING),
            AttributeSyntax.of("startAngleDeg", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("direction", SyntaxValueKind.STRING),
            AttributeSyntax.of("title", SyntaxValueKind.STRING),
            AttributeSyntax.of("titleColor", SyntaxValueKind.COLOR));

        // FunctionGraph child tags.
        sink.attributes(
            "Plot",
            AttributeSyntax.of("expr", SyntaxValueKind.EXPRESSION),
            AttributeSyntax.of("inverse", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("domain", SyntaxValueKind.DOMAIN),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("label", SyntaxValueKind.STRING),
            AttributeSyntax.of("tooltip", SyntaxValueKind.STRING),
            AttributeSyntax.of("showFunction", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showValues", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("pointEveryX", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("pointEveryY", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("autoPointLabel", SyntaxValueKind.ENUM, AutoPointLabelMode.class),
            AttributeSyntax.of("autoPointColor", SyntaxValueKind.COLOR));
        sink.attributes(
            "Point",
            AttributeSyntax.of("x", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("y", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("name", SyntaxValueKind.STRING),
            AttributeSyntax.of("plot", SyntaxValueKind.INT),
            AttributeSyntax.of("atX", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("atY", SyntaxValueKind.FLOAT));

        // Existing registrations with extended attributes.

        // GameScene: add camera attributes
        sink.attributes(
            "Scene",
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT),
            AttributeSyntax.of("zoom", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("perspective", SyntaxValueKind.STRING),
            AttributeSyntax.of("rotateX", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("rotateY", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("rotateZ", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("offsetX", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("offsetY", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("centerX", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("centerY", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("centerZ", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("interactive", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("allowLayerSlider", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("gridButtonEnabled", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showGrid", SyntaxValueKind.BOOLEAN));

        // GameScene: also register as "GameScene" with same attrs (SceneTagCompiler handles both)
        sink.attributes(
            "GameScene",
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT),
            AttributeSyntax.of("zoom", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("perspective", SyntaxValueKind.STRING),
            AttributeSyntax.of("rotateX", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("rotateY", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("rotateZ", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("offsetX", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("offsetY", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("centerX", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("centerY", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("centerZ", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("interactive", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("allowLayerSlider", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("gridButtonEnabled", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showGrid", SyntaxValueKind.BOOLEAN));

        // Function: add missing container + plot attrs
        sink.attributes(
            "Function",
            AttributeSyntax.of("title", SyntaxValueKind.STRING),
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT),
            AttributeSyntax.of("background", SyntaxValueKind.COLOR),
            AttributeSyntax.of("border", SyntaxValueKind.COLOR),
            AttributeSyntax.of("axisColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("gridColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("showGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showAxes", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("xMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xRange", SyntaxValueKind.STRING),
            AttributeSyntax.of("yRange", SyntaxValueKind.STRING),
            AttributeSyntax.of("xLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("yLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("quadrants", SyntaxValueKind.STRING),
            AttributeSyntax.of("cornerLegend", SyntaxValueKind.ENUM, CornerLegendPosition.class),
            AttributeSyntax.of("cornerLegendWidth", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendHeight", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendBackground", SyntaxValueKind.COLOR),
            AttributeSyntax.of("expr", SyntaxValueKind.EXPRESSION),
            AttributeSyntax.of("inverse", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("domain", SyntaxValueKind.DOMAIN),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("label", SyntaxValueKind.STRING),
            AttributeSyntax.of("tooltip", SyntaxValueKind.STRING),
            AttributeSyntax.of("showFunction", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showValues", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("pointEveryX", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("pointEveryY", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("autoPointLabel", SyntaxValueKind.ENUM, AutoPointLabelMode.class),
            AttributeSyntax.of("autoPointColor", SyntaxValueKind.COLOR));

        // FunctionGraph: add missing container attrs
        sink.attributes(
            "FunctionGraph",
            AttributeSyntax.of("title", SyntaxValueKind.STRING),
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT),
            AttributeSyntax.of("background", SyntaxValueKind.COLOR),
            AttributeSyntax.of("border", SyntaxValueKind.COLOR),
            AttributeSyntax.of("axisColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("gridColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("showGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showAxes", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("xMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xRange", SyntaxValueKind.STRING),
            AttributeSyntax.of("yRange", SyntaxValueKind.STRING),
            AttributeSyntax.of("domain", SyntaxValueKind.DOMAIN),
            AttributeSyntax.of("xLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("yLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("quadrants", SyntaxValueKind.STRING),
            AttributeSyntax.of("cornerLegend", SyntaxValueKind.ENUM, CornerLegendPosition.class),
            AttributeSyntax.of("cornerLegendWidth", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendHeight", SyntaxValueKind.INT),
            AttributeSyntax.of("cornerLegendBackground", SyntaxValueKind.COLOR));

        // Entity: add missing rotation attrs
        sink.attributes(
            "Entity",
            AttributeSyntax.of("id", SyntaxValueKind.ENTITY_ID),
            AttributeSyntax.of("data", SyntaxValueKind.SNBT),
            AttributeSyntax.of("name", SyntaxValueKind.STRING),
            AttributeSyntax.of("uuid", SyntaxValueKind.STRING),
            AttributeSyntax.of("showName", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showCape", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("baby", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("x", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("y", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("z", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("rotationY", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("rotationX", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("headRotation", SyntaxValueKind.STRING),
            AttributeSyntax.of("leftArmRotation", SyntaxValueKind.STRING),
            AttributeSyntax.of("rightArmRotation", SyntaxValueKind.STRING),
            AttributeSyntax.of("leftLegRotation", SyntaxValueKind.STRING),
            AttributeSyntax.of("rightLegRotation", SyntaxValueKind.STRING),
            AttributeSyntax.of("capeRotation", SyntaxValueKind.STRING));

        // Latex: add missing attrs
        sink.attributes(
            "Latex",
            AttributeSyntax.of("formula", SyntaxValueKind.STRING),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("scale", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("sourceScale", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("showTooltip", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("valign", SyntaxValueKind.STRING),
            AttributeSyntax.of("offsetX", SyntaxValueKind.INT),
            AttributeSyntax.of("offsetY", SyntaxValueKind.INT));

        // ImportStructureLib: add offset attrs
        sink.attributes(
            "ImportStructureLib",
            AttributeSyntax.of("controller", SyntaxValueKind.STRING),
            AttributeSyntax.of("piece", SyntaxValueKind.STRING),
            AttributeSyntax.of("channel", SyntaxValueKind.STRING),
            AttributeSyntax.of("facing", SyntaxValueKind.STRING),
            AttributeSyntax.of("rotation", SyntaxValueKind.STRING),
            AttributeSyntax.of("flip", SyntaxValueKind.STRING),
            AttributeSyntax.of("offsetX", SyntaxValueKind.INT),
            AttributeSyntax.of("offsetY", SyntaxValueKind.INT),
            AttributeSyntax.of("offsetZ", SyntaxValueKind.INT));
        sink.attributes(
            "Tier",
            AttributeSyntax.of("value", SyntaxValueKind.INT),
            AttributeSyntax.of("expr", SyntaxValueKind.INT));
        sink.attributes(
            "Channel",
            AttributeSyntax.of("name", SyntaxValueKind.STRING),
            AttributeSyntax.of("id", SyntaxValueKind.STRING),
            AttributeSyntax.of("value", SyntaxValueKind.INT),
            AttributeSyntax.of("expr", SyntaxValueKind.INT));
        sink.attributes("Facing", AttributeSyntax.of("value", SyntaxValueKind.STRING));
        sink.attributes("Rotation", AttributeSyntax.of("value", SyntaxValueKind.STRING));
        sink.attributes("Flip", AttributeSyntax.of("value", SyntaxValueKind.STRING));
        sink.attributes("Orientation", AttributeSyntax.of("value", SyntaxValueKind.STRING));
        sink.attributes("GregTechActiveController");
        sink.attributes("GtActiveController");
        sink.attributes("GregTechPlaceHatches");
        sink.attributes("GtPlaceHatches");

        // PlaceBlock: add dx/dy/dz
        sink.attributes(
            "PlaceBlock",
            AttributeSyntax.of("id", SyntaxValueKind.BLOCK_ID),
            AttributeSyntax.of("nbt", SyntaxValueKind.SNBT),
            AttributeSyntax.of("x", SyntaxValueKind.INT),
            AttributeSyntax.of("y", SyntaxValueKind.INT),
            AttributeSyntax.of("z", SyntaxValueKind.INT),
            AttributeSyntax.of("dx", SyntaxValueKind.INT),
            AttributeSyntax.of("dy", SyntaxValueKind.INT),
            AttributeSyntax.of("dz", SyntaxValueKind.INT));

        // ReplaceBlock: add bounds attrs
        sink.attributes(
            "ReplaceBlock",
            AttributeSyntax.of("from", SyntaxValueKind.BLOCK_ID),
            AttributeSyntax.of("to", SyntaxValueKind.BLOCK_ID),
            AttributeSyntax.of("from_nbt", SyntaxValueKind.SNBT),
            AttributeSyntax.of("to_nbt", SyntaxValueKind.SNBT),
            AttributeSyntax.of("x", SyntaxValueKind.INT),
            AttributeSyntax.of("y", SyntaxValueKind.INT),
            AttributeSyntax.of("z", SyntaxValueKind.INT),
            AttributeSyntax.of("dx", SyntaxValueKind.INT),
            AttributeSyntax.of("dy", SyntaxValueKind.INT),
            AttributeSyntax.of("dz", SyntaxValueKind.INT));

        // ImportStructure: fix types + add x/y/z
        sink.attributes(
            "ImportStructure",
            AttributeSyntax.of("src", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("x", SyntaxValueKind.INT),
            AttributeSyntax.of("y", SyntaxValueKind.INT),
            AttributeSyntax.of("z", SyntaxValueKind.INT),
            AttributeSyntax.of("offsetX", SyntaxValueKind.INT),
            AttributeSyntax.of("offsetY", SyntaxValueKind.INT),
            AttributeSyntax.of("offsetZ", SyntaxValueKind.INT));

        // Block scene element
        sink.attributes(
            "Block",
            AttributeSyntax.of("id", SyntaxValueKind.BLOCK_ID),
            AttributeSyntax.of("ore", SyntaxValueKind.ORE_DICT),
            AttributeSyntax.of("x", SyntaxValueKind.INT),
            AttributeSyntax.of("y", SyntaxValueKind.INT),
            AttributeSyntax.of("z", SyntaxValueKind.INT),
            AttributeSyntax.of("meta", SyntaxValueKind.INT),
            AttributeSyntax.of("facing", SyntaxValueKind.STRING),
            AttributeSyntax.of("nbt", SyntaxValueKind.SNBT));

        // Annotation tags
        sink.attributes(
            "BlockAnnotation",
            AttributeSyntax.of("pos", SyntaxValueKind.VECTOR3),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("thickness", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("alwaysOnTop", SyntaxValueKind.BOOLEAN));

        sink.attributes(
            "BoxAnnotation",
            AttributeSyntax.of("min", SyntaxValueKind.VECTOR3),
            AttributeSyntax.of("max", SyntaxValueKind.VECTOR3),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("thickness", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("alwaysOnTop", SyntaxValueKind.BOOLEAN));

        sink.attributes(
            "LineAnnotation",
            AttributeSyntax.of("points", SyntaxValueKind.STRING),
            AttributeSyntax.of("from", SyntaxValueKind.VECTOR3),
            AttributeSyntax.of("to", SyntaxValueKind.VECTOR3),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("thickness", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("alwaysOnTop", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("showPoints", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("pointColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("pointSize", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("arrow", SyntaxValueKind.STRING));

        sink.attributes(
            "DiamondAnnotation",
            AttributeSyntax.of("pos", SyntaxValueKind.VECTOR3),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("alwaysOnTop", SyntaxValueKind.BOOLEAN));

        sink.attributes(
            "TextAnnotation",
            AttributeSyntax.of("text", SyntaxValueKind.STRING),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("maxWidth", SyntaxValueKind.INT),
            AttributeSyntax.of("backgroundAlpha", SyntaxValueKind.INT),
            AttributeSyntax.of("independent", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("yOffset", SyntaxValueKind.INT),
            AttributeSyntax.of("pos", SyntaxValueKind.VECTOR3));

        sink.attributes("BlockAnnotationTemplate", AttributeSyntax.of("id", SyntaxValueKind.STRING));

        // Sound tags (share GuideSoundParsers.parseAttributes)
        sink.attributes(
            "PlaySound",
            AttributeSyntax.of("sound", SyntaxValueKind.STRING),
            AttributeSyntax.of("src", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("volume", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("pitch", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("cooldown", SyntaxValueKind.INT),
            AttributeSyntax.of("radius", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("minVolume", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("x", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("y", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("z", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("trigger", SyntaxValueKind.STRING));

        sink.attributes(
            "SoundLink",
            AttributeSyntax.of("sound", SyntaxValueKind.STRING),
            AttributeSyntax.of("src", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("volume", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("pitch", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("cooldown", SyntaxValueKind.INT),
            AttributeSyntax.of("radius", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("minVolume", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("x", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("y", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("z", SyntaxValueKind.FLOAT));

        // Quest integration tags
        sink.attributes(
            "QuestLink",
            AttributeSyntax.of("id", SyntaxValueKind.STRING),
            AttributeSyntax.of("text", SyntaxValueKind.STRING));

        sink.attributes(
            "QuestCard",
            AttributeSyntax.of("id", SyntaxValueKind.STRING),
            AttributeSyntax.of("show_desc", SyntaxValueKind.STRING));

        // Scene block stats
        sink.attributes(
            "BlockStats",
            AttributeSyntax.of("visible", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("buttonEnabled", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("mode", SyntaxValueKind.STRING),
            AttributeSyntax.of("corner", SyntaxValueKind.STRING),
            AttributeSyntax.of("dock", SyntaxValueKind.STRING),
            AttributeSyntax.of("showNames", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("filterMode", SyntaxValueKind.STRING),
            AttributeSyntax.of("filter", SyntaxValueKind.STRING),
            AttributeSyntax.of("maxWidth", SyntaxValueKind.INT),
            AttributeSyntax.of("maxHeight", SyntaxValueKind.INT));

        sink.attributes(
            "BlockStat",
            AttributeSyntax.of("id", SyntaxValueKind.BLOCK_ID),
            AttributeSyntax.of("item", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("count", SyntaxValueKind.FLOAT));

        registerCrossCuttingAttributes(sink);
    }

    /**
     * Attributes that are read by shared parsers or written back by the scene editor rather than by a
     * single tag compiler, plus the tags that only exist as children of another tag. Keeping them in
     * one place mirrors where their readers live.
     */
    private static void registerCrossCuttingAttributes(SyntaxSink sink) {
        // Recipe family: <Usage>, <RecipeUsage> and <RecipesUsage> are the ingredient-directed twins.
        sink.attributes("Recipe", RECIPE_INDEX);
        sink.attributes("RecipeFor", RECIPE_INDEX);
        sink.attributes("RecipesFor", RECIPE_INDEX);
        sink.attributes(
            "Usage",
            AttributeSyntax.of("id", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("fallbackText", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerName", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerId", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerOrder", SyntaxValueKind.INT),
            AttributeSyntax.of("input", SyntaxValueKind.STRING),
            AttributeSyntax.of("output", SyntaxValueKind.STRING),
            AttributeSyntax.of("limit", SyntaxValueKind.INT),
            RECIPE_INDEX);
        sink.attributes(
            "RecipeUsage",
            AttributeSyntax.of("id", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("fallbackText", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerName", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerId", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerOrder", SyntaxValueKind.INT),
            AttributeSyntax.of("input", SyntaxValueKind.STRING),
            AttributeSyntax.of("output", SyntaxValueKind.STRING),
            AttributeSyntax.of("limit", SyntaxValueKind.INT),
            RECIPE_INDEX);
        sink.attributes(
            "RecipesUsage",
            AttributeSyntax.of("id", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("fallbackText", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerName", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerId", SyntaxValueKind.STRING),
            AttributeSyntax.of("handlerOrder", SyntaxValueKind.INT),
            AttributeSyntax.of("input", SyntaxValueKind.STRING),
            AttributeSyntax.of("output", SyntaxValueKind.STRING),
            AttributeSyntax.of("limit", SyntaxValueKind.INT),
            RECIPE_INDEX);

        // Multi-part structures: every structural element shares the "formed" flag.
        sink.attributes("Block", FORMED);
        sink.attributes("ImportStructure", FORMED);
        sink.attributes("PlaceBlock", FORMED);
        sink.attributes("ReplaceBlock", FORMED);

        // StructureLib driven visibility, honoured by every in-world annotation and by sounds.
        sink.attributes("BlockAnnotation", SHOW_WHEN_STRUCTURE, SHOW_WHEN_TIER, SHOW_WHEN_CHANNELS);
        sink.attributes("BoxAnnotation", SHOW_WHEN_STRUCTURE, SHOW_WHEN_TIER, SHOW_WHEN_CHANNELS);
        sink.attributes("LineAnnotation", SHOW_WHEN_STRUCTURE, SHOW_WHEN_TIER, SHOW_WHEN_CHANNELS);
        sink.attributes("DiamondAnnotation", SHOW_WHEN_STRUCTURE, SHOW_WHEN_TIER, SHOW_WHEN_CHANNELS);
        sink.attributes("TextAnnotation", SHOW_WHEN_STRUCTURE, SHOW_WHEN_TIER, SHOW_WHEN_CHANNELS);
        sink.attributes("BlockAnnotationTemplate", SHOW_WHEN_STRUCTURE, SHOW_WHEN_TIER, SHOW_WHEN_CHANNELS);
        sink.attributes("PlaySound", SHOW_WHEN_STRUCTURE, SHOW_WHEN_TIER, SHOW_WHEN_CHANNELS);
        sink.attributes("InputAnnotation", SHOW_WHEN_STRUCTURE, SHOW_WHEN_TIER, SHOW_WHEN_CHANNELS);

        sink.attributes("Scene", AttributeSyntax.of("showBackground", SyntaxValueKind.BOOLEAN));
        sink.attributes("GameScene", AttributeSyntax.of("showBackground", SyntaxValueKind.BOOLEAN));

        sink.attributes(
            "ImportStructureLib",
            AttributeSyntax.of("name", SyntaxValueKind.STRING),
            AttributeSyntax.of("formed", SyntaxValueKind.BOOLEAN));
        sink.attributes(
            "TextAnnotation",
            AttributeSyntax.of("x", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("y", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("z", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("textKey", SyntaxValueKind.STRING),
            AttributeSyntax.of("connectorSide", SyntaxValueKind.STRING),
            AttributeSyntax.of("connectorOffset", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("connectorLength", SyntaxValueKind.FLOAT));
        sink.attributes(
            "BlockImage",
            AttributeSyntax.of("meta", SyntaxValueKind.INT),
            AttributeSyntax.of("nbt", SyntaxValueKind.SNBT),
            AttributeSyntax.of("perspective", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT));
        sink.attributes("Latex", AttributeSyntax.of("tooltip", SyntaxValueKind.STRING));
        sink.attributes(
            "details",
            AttributeSyntax.of("width", SyntaxValueKind.INT),
            AttributeSyntax.of("height", SyntaxValueKind.INT));

        // The mouse-input hint: a world position, which button, an optional modifier and item.
        sink.attributes(
            "InputAnnotation",
            AttributeSyntax.of("pos", SyntaxValueKind.VECTOR3),
            AttributeSyntax.of("x", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("y", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("z", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("inputType", SyntaxValueKind.ENUM, "lmb", "rmb", "scroll"),
            AttributeSyntax.of("modifier", SyntaxValueKind.ENUM, "none", "sneak", "sprint", "use"),
            AttributeSyntax.of("item", SyntaxValueKind.ITEM_ID));

        // Chart axis options. ChartAttrParser reads these by prefix, so they were invisible to
        // the editor even though the tag reference documents them.
        sink.attributes(
            "BarChart",
            AttributeSyntax.of("xAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("showXGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("xGridColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("showYGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("yGridColor", SyntaxValueKind.COLOR));

        sink.attributes(
            "ColumnChart",
            AttributeSyntax.of("xAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("showXGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("xGridColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("showYGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("yGridColor", SyntaxValueKind.COLOR));

        sink.attributes(
            "LineChart",
            AttributeSyntax.of("xAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("showXGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("xGridColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("showYGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("yGridColor", SyntaxValueKind.COLOR));

        sink.attributes(
            "ScatterChart",
            AttributeSyntax.of("xAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("xAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("xAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisLabel", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisMin", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisMax", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisStep", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("yAxisUnit", SyntaxValueKind.STRING),
            AttributeSyntax.of("yAxisTickFormat", SyntaxValueKind.STRING),
            AttributeSyntax.of("showXGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("xGridColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("showYGrid", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("yGridColor", SyntaxValueKind.COLOR));

        // Child-only tags.
        sink.attributes(
            "ItemIcon",
            AttributeSyntax.of("id", SyntaxValueKind.ITEM_ID),
            AttributeSyntax.of("ore", SyntaxValueKind.ORE_DICT));
        sink.attributes(
            "LinePoint",
            AttributeSyntax.of("index", SyntaxValueKind.INT),
            AttributeSyntax.of("show", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("color", SyntaxValueKind.COLOR),
            AttributeSyntax.of("size", SyntaxValueKind.FLOAT));
        sink.attributes(
            "ImageAnnotation",
            AttributeSyntax.of("x", SyntaxValueKind.INT),
            AttributeSyntax.of("y", SyntaxValueKind.INT),
            AttributeSyntax.of("w", SyntaxValueKind.INT),
            AttributeSyntax.of("h", SyntaxValueKind.INT),
            AttributeSyntax.of("border", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("borderThickness", SyntaxValueKind.INT),
            AttributeSyntax.of("borderColor", SyntaxValueKind.COLOR),
            AttributeSyntax.of("sound", SyntaxValueKind.STRING),
            AttributeSyntax.of("soundSrc", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("volume", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("pitch", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("cooldown", SyntaxValueKind.INT),
            AttributeSyntax.of("trigger", SyntaxValueKind.STRING));
        sink.attributes(
            "SoundArea",
            AttributeSyntax.of("x", SyntaxValueKind.INT),
            AttributeSyntax.of("y", SyntaxValueKind.INT),
            AttributeSyntax.of("w", SyntaxValueKind.INT),
            AttributeSyntax.of("h", SyntaxValueKind.INT),
            AttributeSyntax.of("sound", SyntaxValueKind.STRING),
            AttributeSyntax.of("soundSrc", SyntaxValueKind.FILE_PATH),
            AttributeSyntax.of("volume", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("pitch", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("cooldown", SyntaxValueKind.INT),
            AttributeSyntax.of("trigger", SyntaxValueKind.STRING));

        registerBlockLayoutAttributes(sink);
        registerStructureLibAttributes(sink);
        registerAttributeCorrections(sink);
        registerMediaWikiSpecialAttributes(sink);
        registerQuestVisibilityAttributes(sink);
        contributeTagShape(sink);
        contributeValueKinds(sink);
        contributeMarkdown(sink);
        contributeFences(sink);
        contributeFrontmatter(sink);
        contributeValueSources(sink);
    }

    /**
     * {@code BlockTagCompiler} applies {@code wrap}, {@code align} and {@code float} to every block tag
     * it compiles, so all of its subclasses accept them.
     */
    private static void registerBlockLayoutAttributes(SyntaxSink sink) {
        // BlockTagCompiler applies wrap, align and float to every block tag it compiles.
        sink.attributes("details", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("ContentTabs", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("FileTree", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("Row", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("Column", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("Recipe", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("RecipeFor", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("RecipesFor", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("Usage", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("RecipeUsage", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("RecipesUsage", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("FootnoteList", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("Structure", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("Mermaid", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("CsvTable", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("ColumnChart", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("BarChart", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("LineChart", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("PieChart", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("ScatterChart", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("FunctionGraph", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("Function", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("Scene", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("GameScene", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("ItemGrid", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("SubPages", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("Category", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
        sink.attributes("Special", LAYOUT_WRAP, LAYOUT_ALIGN, LAYOUT_FLOAT);
    }

    /**
     * StructureLib scene options are only read from {@code <ImportStructureLib>}, and its child tags
     * describe tiers, channels and orientations.
     */
    private static void registerStructureLibAttributes(SyntaxSink sink) {
        sink.attributes(
            "ImportStructureLib",
            AttributeSyntax.of("tier", SyntaxValueKind.INT),
            AttributeSyntax.of("facing", SyntaxValueKind.STRING),
            AttributeSyntax.of("rotation", SyntaxValueKind.STRING),
            AttributeSyntax.of("flip", SyntaxValueKind.STRING),
            AttributeSyntax.of("gtActiveController", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("gtPlaceHatches", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("channelName", SyntaxValueKind.STRING),
            AttributeSyntax.of("channel", SyntaxValueKind.INT),
            AttributeSyntax.of("value", SyntaxValueKind.INT));

        sink.attributes("Facing", TEXT_VALUE, TEXT_EXPR, TEXT_NAME);
        sink.attributes("Rotation", TEXT_VALUE, TEXT_EXPR, TEXT_NAME);
        sink.attributes("Flip", TEXT_VALUE, TEXT_EXPR, TEXT_NAME);
        sink.attributes("Orientation", TEXT_VALUE, TEXT_EXPR, TEXT_NAME);
        sink.attributes(
            "Tier",
            AttributeSyntax.of("value", SyntaxValueKind.STRING),
            AttributeSyntax.of("expr", SyntaxValueKind.STRING),
            AttributeSyntax.of("tier", SyntaxValueKind.INT));
        sink.attributes(
            "Channel",
            AttributeSyntax.of("value", SyntaxValueKind.STRING),
            AttributeSyntax.of("expr", SyntaxValueKind.STRING),
            AttributeSyntax.of("name", SyntaxValueKind.STRING));
    }

    /** Facts that correct or complete an existing tag registration. */
    private static void registerAttributeCorrections(SyntaxSink sink) {
        sink.attributes(
            "Entity",
            AttributeSyntax.of("sceneEntityId", SyntaxValueKind.STRING),
            AttributeSyntax.of("mount", SyntaxValueKind.STRING),
            AttributeSyntax.of("unmount", SyntaxValueKind.BOOLEAN));
        sink.attributes("ItemImage", AttributeSyntax.of("nbt", SyntaxValueKind.SNBT));
        sink.attributes("ItemLink", AttributeSyntax.of("scale", SyntaxValueKind.FLOAT));
        sink.attributes(
            "TextAnnotation",
            AttributeSyntax.of("hlMinX", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("hlMinY", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("hlMinZ", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("hlMaxX", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("hlMaxY", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("hlMaxZ", SyntaxValueKind.FLOAT),
            AttributeSyntax.of("highlightColor", SyntaxValueKind.COLOR));
        // Block statistics: the values the scene parsers accept for each option.
        sink.attributes(
            "BlockStats",
            AttributeSyntax.of("mode", SyntaxValueKind.ENUM, "auto", "manual"),
            AttributeSyntax.of("corner", SyntaxValueKind.ENUM, "topRight", "topLeft", "bottomRight", "bottomLeft"),
            AttributeSyntax.of("dock", SyntaxValueKind.ENUM, "inside", "left", "top", "right", "bottom"),
            AttributeSyntax.of("filterMode", SyntaxValueKind.ENUM, "blacklist", "whitelist")); // Read as an integer
                                                                                               // channel index, not as
                                                                                               // free text.
        sink.attributes("ImportStructureLib", AttributeSyntax.of("channel", SyntaxValueKind.INT));
        // Read as a string, so the bare value form is the right completion.
        sink.attributes("BlockImage", AttributeSyntax.of("perspective", SyntaxValueKind.STRING));
    }

    /** MediaWiki list tags can select the generated page, its label prefix, and its query. */
    private static void registerMediaWikiSpecialAttributes(SyntaxSink sink) {
        sink.attributes("Special", SPECIAL_PAGE, SPECIAL_PREFIX, SPECIAL_LANGUAGE, SPECIAL_QUERY);
        sink.attributes("Category", SPECIAL_PAGE, SPECIAL_PREFIX, SPECIAL_LANGUAGE, SPECIAL_QUERY);
    }

    private static void registerQuestVisibilityAttributes(SyntaxSink sink) {
        sink.attributes("QuestLink", QUEST_SHOW_TOOLTIP, QUEST_SHOW_TOOLTIP_SNAKE);
        sink.attributes("QuestCard", QUEST_SHOW_TOOLTIP, QUEST_SHOW_TOOLTIP_SNAKE);
    }

    /**
     * Which tags wrap content, which tags may appear inside them, and the tags a compiler accepts but
     * authors never write.
     */
    private static void contributeTagShape(SyntaxSink sink) {
        sink.containerTags(
            "a",
            "Tooltip",
            "Color",
            "CommandLink",
            "SoundLink",
            "kbd",
            "Latex",
            "mark",
            "sub",
            "sup",
            "Spoiler",
            "Comment",
            "span",
            "Row",
            "Column",
            "div",
            "details",
            "summary",
            "ContentTabs",
            "Tab",
            "FileTree",
            "FootnoteList",
            "ItemGrid",
            "Mermaid",
            "NodeContent",
            "Structure",
            "BlockStats",
            "ColumnChart",
            "BarChart",
            "LineChart",
            "PieChart",
            "ScatterChart",
            "PieInset",
            "FunctionGraph",
            "Function",
            "Plot",
            "GameScene",
            "Scene",
            "FloatingImage",
            "ImageAnnotation",
            "BlockAnnotation",
            "BoxAnnotation",
            "LineAnnotation",
            "DiamondAnnotation",
            "TextAnnotation",
            "BlockAnnotationTemplate");

        sink.children(
            "GameScene",
            "ImportStructure",
            "ImportStructureLib",
            "ImportPonder",
            "IsometricCamera",
            "Block",
            "Entity",
            "PlaceBlock",
            "ReplaceBlock",
            "RemoveBlocks",
            "RemoveEntity",
            "Particle",
            "Weather",
            "PlaySound",
            "Tier",
            "Channel",
            "Facing",
            "Rotation",
            "Flip",
            "Orientation",
            "GregTechActiveController",
            "GtActiveController",
            "GregTechPlaceHatches",
            "GtPlaceHatches",
            "BlockStats",
            "BlockAnnotation",
            "BoxAnnotation",
            "LineAnnotation",
            "DiamondAnnotation",
            "TextAnnotation",
            "BlockAnnotationTemplate");
        sink.children(
            "Scene",
            "ImportStructure",
            "ImportStructureLib",
            "ImportPonder",
            "IsometricCamera",
            "Block",
            "Entity",
            "PlaceBlock",
            "ReplaceBlock",
            "RemoveBlocks",
            "RemoveEntity",
            "Particle",
            "Weather",
            "PlaySound",
            "Tier",
            "Channel",
            "Facing",
            "Rotation",
            "Flip",
            "Orientation",
            "GregTechActiveController",
            "GtActiveController",
            "GregTechPlaceHatches",
            "GtPlaceHatches",
            "BlockStats",
            "BlockAnnotation",
            "BoxAnnotation",
            "LineAnnotation",
            "DiamondAnnotation",
            "TextAnnotation",
            "BlockAnnotationTemplate");
        sink.children("BarChart", "Series", "LineSeries", "PieInset");
        sink.children("ColumnChart", "Series", "LineSeries", "PieInset");
        sink.children("LineChart", "Series", "LineSeries", "PieInset");
        sink.children("ScatterChart", "Series", "LineSeries", "PieInset");
        sink.children("PieChart", "Slice", "PieInset");
        sink.children("PieInset", "Slice");
        sink.children("FunctionGraph", "Plot", "Point", "Function");
        sink.children("ContentTabs", "Tab");
        sink.children(
            "ImportStructureLib",
            "Tier",
            "Channel",
            "Facing",
            "Rotation",
            "Flip",
            "Orientation",
            "GregTechActiveController",
            "GtActiveController",
            "GregTechPlaceHatches",
            "GtPlaceHatches");
        sink.children("BlockStats", "BlockStat");
        sink.children("ItemGrid", "ItemIcon");
        sink.children("Mermaid", "NodeContent");
        sink.children("details", "summary");
        sink.children("FloatingImage", "ImageAnnotation", "SoundArea");
        // Annotations nest inside annotations so a template can describe a whole structure.
        sink.children(
            "BlockAnnotation",
            "BlockAnnotation",
            "BoxAnnotation",
            "LineAnnotation",
            "DiamondAnnotation",
            "TextAnnotation",
            "BlockAnnotationTemplate");
        sink.children(
            "BoxAnnotation",
            "BlockAnnotation",
            "BoxAnnotation",
            "LineAnnotation",
            "DiamondAnnotation",
            "TextAnnotation",
            "BlockAnnotationTemplate");
        sink.children(
            "LineAnnotation",
            "BlockAnnotation",
            "BoxAnnotation",
            "LineAnnotation",
            "DiamondAnnotation",
            "TextAnnotation",
            "BlockAnnotationTemplate");
        sink.children(
            "DiamondAnnotation",
            "BlockAnnotation",
            "BoxAnnotation",
            "LineAnnotation",
            "DiamondAnnotation",
            "TextAnnotation",
            "BlockAnnotationTemplate");
        sink.children(
            "TextAnnotation",
            "BlockAnnotation",
            "BoxAnnotation",
            "LineAnnotation",
            "DiamondAnnotation",
            "TextAnnotation",
            "BlockAnnotationTemplate");
        sink.children(
            "BlockAnnotationTemplate",
            "BlockAnnotation",
            "BoxAnnotation",
            "LineAnnotation",
            "DiamondAnnotation",
            "TextAnnotation",
            "BlockAnnotationTemplate");
        sink.children("LineAnnotation", "LinePoint");
        sink.children("GameScene", "InputAnnotation");
        sink.children("Scene", "InputAnnotation");

        // The markdown parser models paragraphs, headings, lists and inline emphasis as tags. Authors
        // write markdown for those, so they stay out of tag completion.
        sink.hiddenTags(
            "p",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "ul",
            "ol",
            "li",
            "pre",
            "blockquote",
            "table",
            "hr",
            "strong",
            "em",
            "del",
            "u",
            "wavy",
            "dotted",
            "code",
            "img",
            "link",
            "image");
    }

    /**
     * Attributes whose value kind the editor must know about but which the compiler reads loosely.
     * {@code link} and {@code image} are not tags: they are the markdown link and image targets the
     * resolver reports so page and file completion work inside {@code [](...)}.
     */
    private static void contributeValueKinds(SyntaxSink sink) {
        sink.attributes("Recipe", RECIPE_INPUT, RECIPE_OUTPUT);
        sink.attributes("RecipeFor", RECIPE_INPUT, RECIPE_OUTPUT);
        sink.attributes("RecipesFor", RECIPE_INPUT, RECIPE_OUTPUT);
        sink.attributes("Usage", RECIPE_INPUT, RECIPE_OUTPUT);
        sink.attributes("RecipeUsage", RECIPE_INPUT, RECIPE_OUTPUT);
        sink.attributes("RecipesUsage", RECIPE_INPUT, RECIPE_OUTPUT);
        sink.attributes("BlockAnnotationTemplate", AttributeSyntax.of("id", SyntaxValueKind.BLOCK_ID));
        sink.tags("link", "image");
        sink.attributes("link", AttributeSyntax.of("url", SyntaxValueKind.PAGE_PATH));
        sink.attributes("image", AttributeSyntax.of("url", SyntaxValueKind.FILE_PATH));
    }

    private static void contributeFences(SyntaxSink sink) {
        sink.fenceLanguages(
            CodeBlockLanguageRegistry.getLanguageIds()
                .toArray(new String[0]));
        sink.fenceLanguages(PreCompiler.FILE_TREE_FENCES.toArray(new String[0]));
        sink.fenceLanguages(PreCompiler.FUNCTION_GRAPH_FENCES.toArray(new String[0]));
    }

    private static void contributeFrontmatter(SyntaxSink sink) {
        sink.frontmatterKeys(
            "navigation",
            "title",
            "parent",
            "position",
            "priority",
            "icon",
            "icons",
            "icon_texture",
            "icon_textures",
            "keyword",
            "keywords",
            "required_mod",
            "required_mods",
            "excluded_mod",
            "excluded_mods",
            "item_id",
            "item_ids",
            "ore_ids",
            "quest_ids",
            "categories",
            "authors",
            "author",
            "date",
            "updated",
            "zoom");
        sink.frontmatterValues(
            "navigation",
            "\n  title:",
            "\n  parent:",
            "\n  position:",
            "\n  icon: minecraft:book:0:{display:{Name:\"Custom Icon\"}}",
            "\n  icons:\n    - minecraft:book:0:{display:{Name:\"Cycling Icon\"}}",
            "\n  icon_texture:");
        sink.frontmatterValues("quest_ids", "\n  - 00000000-0000-0000-0000-000000000000");
        sink.frontmatterKind("item_id", SyntaxValueKind.ITEM_ID);
        sink.frontmatterKind("item_ids", SyntaxValueKind.ITEM_ID);
        sink.frontmatterKind("icon", SyntaxValueKind.ITEM_ID);
        sink.frontmatterKind("icons", SyntaxValueKind.ITEM_ID);
        sink.frontmatterKind("ore_ids", SyntaxValueKind.ORE_DICT);
        sink.frontmatterKind("quest_ids", SyntaxValueKind.QUEST_UUID);
        sink.frontmatterKind("parent", SyntaxValueKind.PAGE_PATH);
        sink.frontmatterKind("icon_texture", SyntaxValueKind.FILE_PATH);
        sink.frontmatterKind("icon_textures", SyntaxValueKind.FILE_PATH);
        sink.frontmatterKind("required_mod", SyntaxValueKind.MOD_ID);
        sink.frontmatterKind("required_mods", SyntaxValueKind.MOD_ID);
        sink.frontmatterKind("excluded_mod", SyntaxValueKind.MOD_ID);
        sink.frontmatterKind("excluded_mods", SyntaxValueKind.MOD_ID);
        sink.frontmatterKind("position", SyntaxValueKind.INT);
        sink.frontmatterKind("priority", SyntaxValueKind.INT);
        sink.frontmatterKind("zoom", SyntaxValueKind.FLOAT);
    }

    private static void contributeMarkdown(SyntaxSink sink) {
        sink.markdown(
            MarkdownSnippet.block("#", "Heading 1", "# ", 2),
            MarkdownSnippet.block("##", "Heading 2", "## ", 3),
            MarkdownSnippet.block("###", "Heading 3", "### ", 4),
            MarkdownSnippet.block("####", "Heading 4", "#### ", 5),
            MarkdownSnippet.block("#####", "Heading 5", "##### ", 6),
            MarkdownSnippet.block("######", "Heading 6", "###### ", 7),
            MarkdownSnippet.block("-", "Bullet list", "- ", 2),
            MarkdownSnippet.block("*", "Bullet list", "* ", 2),
            MarkdownSnippet.block("+", "Bullet list", "+ ", 2),
            MarkdownSnippet.block("1.", "Numbered list", "1. ", 3),
            MarkdownSnippet.block("- [ ]", "Task list", "- [ ] ", 6),
            MarkdownSnippet.block(">", "Quote", "> ", 2),
            MarkdownSnippet.block("> [!NOTE]", "Note alert", "> [!NOTE]\n> ", 12),
            MarkdownSnippet.block("> [!TIP]", "Tip alert", "> [!TIP]\n> ", 11),
            MarkdownSnippet.block("> [!IMPORTANT]", "Important alert", "> [!IMPORTANT]\n> ", 17),
            MarkdownSnippet.block("> [!WARNING]", "Warning alert", "> [!WARNING]\n> ", 15),
            MarkdownSnippet.block("> [!CAUTION]", "Caution alert", "> [!CAUTION]\n> ", 15),
            MarkdownSnippet.block("|", "Table", "| Header | Value |\n| --- | --- |\n|  |  |\n", 2),
            MarkdownSnippet.block("```", "Code block", "```\n\n```", 4),
            MarkdownSnippet.block("---", "Thematic break", "---", 3),
            MarkdownSnippet.block("$$", "Math block", "$$\n\n$$", 3),
            MarkdownSnippet.inline("**", "Bold", "****", 2),
            MarkdownSnippet.inline("*", "Italic", "**", 1),
            MarkdownSnippet.inline("~~", "Strikethrough", "~~~~", 2),
            MarkdownSnippet.inline("++", "Underline", "++++", 2),
            MarkdownSnippet.inline("`", "Inline code", "``", 1),
            MarkdownSnippet.inline("[", "Link", "[]()", 1),
            MarkdownSnippet.inline("![", "Image", "![]()", 2));
    }

    private static void contributeValueSources(SyntaxSink sink) {
        sink.valueSource(new ItemIdValueSource());
        sink.valueSource(new BlockIdValueSource());
        sink.valueSource(new ModIdValueSource());
        sink.valueSource(new DomainValueSource());
        sink.valueSource(new OreDictValueSource());
        sink.valueSource(new EntityNameValueSource());
        sink.valueSource(new KeyBindValueSource());
        sink.valueSource(new CommandValueSource());
        sink.valueSource(new PagePathValueSource());
        sink.valueSource(new AnchorValueSource());
        sink.valueSource(new FilePathValueSource());
        sink.valueSource(new ColorValueSource());
        sink.valueSource(new ExpressionValueSource());
        sink.valueSource(new FormatPatternValueSource());
        sink.valueSource(new NumericPresetValueSource());
        sink.valueSource(new AttributePresetValueSource());
        sink.valueSource(new BooleanValueSource());
        sink.valueSource(new QuestIdValueSource());
    }
}
