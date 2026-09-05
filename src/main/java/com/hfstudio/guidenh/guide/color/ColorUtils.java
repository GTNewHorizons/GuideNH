package com.hfstudio.guidenh.guide.color;

import java.util.Locale;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.color.ColorResource;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Central registry for colors used by GuideNH.
 *
 * <p>
 * Color resources can be overridden by resource packs and are refreshed by GTNHLib when resources reload. Callers
 * should resolve a resource with {@link ColorResource#getColor()} at the point where the color is consumed.
 * </p>
 */
public class ColorUtils {

    private static final ColorResource.Factory COLORS = new ColorResource.Factory("guidenh");

    public static final ColorResource TRANSPARENT = COLORS.argb("transparent", "0x00000000");
    public static final ColorResource BLACK = COLORS.argb("black", "0xFF000000");
    public static final ColorResource WHITE = COLORS.argb("white", "0xFFFFFFFF");
    public static final ColorResource RGB_WHITE = COLORS.rgb("rgbWhite", "0xFFFFFF");
    public static final ColorResource WHITE_70 = COLORS.argb("white70", "0xB3FFFFFF");
    public static final ColorResource REGION_X_AXIS = COLORS.argb("regionXAxis", "0xF2FF3333");
    public static final ColorResource REGION_Y_AXIS = COLORS.argb("regionYAxis", "0xF240FF40");
    public static final ColorResource REGION_Z_AXIS = COLORS.argb("regionZAxis", "0xF24073FF");

    public static final ColorResource PANEL = COLORS.argb("panel", "0xB418181C");
    public static final ColorResource PANEL_INNER = COLORS.argb("panelInner", "0x70121216");
    public static final ColorResource PANEL_BORDER = COLORS.argb("panelBorder", "0xFF5A5A5A");
    public static final ColorResource PANEL_HEADER = COLORS.argb("panelHeader", "0xFFDEE6F0");
    public static final ColorResource PANEL_MUTED_TEXT = COLORS.argb("panelMutedText", "0xFFB9C2CE");
    public static final ColorResource PANEL_SUBTLE_TEXT = COLORS.argb("panelSubtleText", "0xFF8F98A3");

    public static final ColorResource INPUT_BORDER = COLORS.argb("inputBorder", "0xFF3E434A");
    public static final ColorResource INPUT_FOCUSED_BORDER = COLORS.argb("inputFocusedBorder", "0xFF7FC8FF");
    public static final ColorResource INPUT_ERROR_BORDER = COLORS.argb("inputErrorBorder", "0xFFFF6767");
    public static final ColorResource INPUT_BACKGROUND = COLORS.argb("inputBackground", "0x80101012");
    public static final ColorResource CHECKBOX_BACKGROUND = COLORS.argb("checkboxBackground", "0xA0141418");
    public static final ColorResource CHECKBOX_CHECK = COLORS.argb("checkboxCheck", "0xFF00CAF2");

    public static final ColorResource TAB_ACTIVE = COLORS.argb("tabActive", "0xD6202C36");
    public static final ColorResource TAB_INACTIVE = COLORS.argb("tabInactive", "0x6612181C");
    public static final ColorResource TAB_HOVER = COLORS.argb("tabHover", "0xA61C252E");
    public static final ColorResource ELEMENT_ROW = COLORS.argb("elementRow", "0x6A121418");
    public static final ColorResource ELEMENT_ROW_SELECTED = COLORS.argb("elementRowSelected", "0x9A1C222A");
    public static final ColorResource ELEMENT_ROW_EXPANDED = COLORS.argb("elementRowExpanded", "0x7A101216");
    public static final ColorResource ELEMENT_MENU = COLORS.argb("elementMenu", "0xEE121418");
    public static final ColorResource ELEMENT_MENU_HOVER = COLORS.argb("elementMenuHover", "0xCC1A222A");

    public static final ColorResource DIALOG_OVERLAY = COLORS.argb("dialogOverlay", "0x8A050608");
    public static final ColorResource DIALOG = COLORS.argb("dialog", "0xF0181C22");
    public static final ColorResource DIALOG_HOVER = COLORS.argb("dialogHover", "0xCC24303A");
    public static final ColorResource TEXT = COLORS.argb("text", "0xFFF0F0F0");
    public static final ColorResource TEXT_MUTED = COLORS.argb("textMuted", "0xFFD0D8E0");
    public static final ColorResource TEXT_DISABLED = COLORS.argb("textDisabled", "0xFF8F98A3");
    public static final ColorResource ACCENT = COLORS.argb("accent", "0xFF00CAF2");
    public static final ColorResource ERROR = COLORS.argb("error", "0xFFFF6767");
    public static final ColorResource DEBUG_TEXT = COLORS.argb("debugText", "0xFFC47BA1");
    public static final ColorResource DEBUG_CURSOR = COLORS.argb("debugCursor", "0xCC00FF00");
    public static final ColorResource DEBUG_OUTLINE = COLORS.argb("debugOutline", "0xFFC47BA1");

    public static final ConstantColor LINK = new ConstantColor(rgb(0, 213, 255));
    public static final ConstantColor BODY_TEXT = new ConstantColor(rgb(210, 210, 210));
    public static final ConstantColor ERROR_TEXT = new ConstantColor(rgb(255, 0, 0));
    public static final ConstantColor CRAFTING_RECIPE_TYPE = new ConstantColor(rgb(64, 64, 64));
    public static final ConstantColor THEMATIC_BREAK = new ConstantColor(rgb(155, 155, 155));
    public static final ConstantColor HEADER1_SEPARATOR = new ConstantColor(argb(127, 255, 255, 255));
    public static final ConstantColor HEADER2_SEPARATOR = new ConstantColor(argb(127, 210, 210, 210));
    public static final ConstantColor NAVBAR_BG_TOP = new ConstantColor(BLACK.getColor());
    public static final ConstantColor NAVBAR_BG_BOTTOM = new ConstantColor(argb(127, 0, 0, 0));
    public static final ConstantColor NAVBAR_ROW_HOVER = new ConstantColor(rgb(33, 33, 33));
    public static final ConstantColor NAVBAR_EXPAND_ARROW = new ConstantColor(rgb(238, 238, 238));
    public static final ConstantColor TABLE_BORDER = new ConstantColor(rgb(124, 124, 124));
    public static final ConstantColor ICON_BUTTON_NORMAL = new ConstantColor(mono(200));
    public static final ConstantColor ICON_BUTTON_DISABLED = new ConstantColor(mono(64));
    public static final ConstantColor ICON_BUTTON_HOVER = new ConstantColor(rgb(0, 213, 255));
    public static final ConstantColor IN_WORLD_BLOCK_HIGHLIGHT = new ConstantColor(argb(0xCC, 0x99, 0x99, 0x99));
    public static final ConstantColor SYMBOLIC_SCENE_BACKGROUND = new ConstantColor(argb(20, 0, 0, 0));
    public static final ConstantColor GUIDE_SCREEN_BACKGROUND = new ConstantColor(argb(229, 63, 63, 63));
    public static final ConstantColor BLOCKQUOTE_BACKGROUND = new ConstantColor(argb(64, 255, 255, 255));

    public static final ConstantColor MC_BLACK = new ConstantColor(hexToRgb("#000"));
    public static final ConstantColor MC_DARK_BLUE = new ConstantColor(hexToRgb("#00A"));
    public static final ConstantColor MC_DARK_GREEN = new ConstantColor(hexToRgb("#0A0"));
    public static final ConstantColor MC_DARK_AQUA = new ConstantColor(hexToRgb("#0AA"));
    public static final ConstantColor MC_DARK_RED = new ConstantColor(hexToRgb("#A00"));
    public static final ConstantColor MC_DARK_PURPLE = new ConstantColor(hexToRgb("#A0A"));
    public static final ConstantColor MC_GOLD = new ConstantColor(hexToRgb("#AA0"));
    public static final ConstantColor MC_GRAY = new ConstantColor(hexToRgb("#AAA"));
    public static final ConstantColor MC_DARK_GRAY = new ConstantColor(hexToRgb("#555"));
    public static final ConstantColor MC_BLUE = new ConstantColor(hexToRgb("#55F"));
    public static final ConstantColor MC_GREEN = new ConstantColor(hexToRgb("#5F5"));
    public static final ConstantColor MC_AQUA = new ConstantColor(hexToRgb("#5FF"));
    public static final ConstantColor MC_RED = new ConstantColor(hexToRgb("#F55"));
    public static final ConstantColor MC_LIGHT_PURPLE = new ConstantColor(hexToRgb("#F5F"));
    public static final ConstantColor MC_YELLOW = new ConstantColor(hexToRgb("#FF5"));
    public static final ConstantColor MC_WHITE = new ConstantColor(hexToRgb("#FFF"));

    public static ColorValue symbolic(String id) {
        if (id == null) {
            return null;
        }
        return switch (id.toUpperCase(Locale.ROOT)) {
            case "LINK" -> LINK;
            case "BODY_TEXT" -> BODY_TEXT;
            case "ERROR_TEXT" -> ERROR_TEXT;
            case "CRAFTING_RECIPE_TYPE" -> CRAFTING_RECIPE_TYPE;
            case "THEMATIC_BREAK" -> THEMATIC_BREAK;
            case "HEADER1_SEPARATOR" -> HEADER1_SEPARATOR;
            case "HEADER2_SEPARATOR" -> HEADER2_SEPARATOR;
            case "NAVBAR_BG_TOP" -> NAVBAR_BG_TOP;
            case "NAVBAR_BG_BOTTOM" -> NAVBAR_BG_BOTTOM;
            case "NAVBAR_ROW_HOVER" -> NAVBAR_ROW_HOVER;
            case "NAVBAR_EXPAND_ARROW" -> NAVBAR_EXPAND_ARROW;
            case "TABLE_BORDER" -> TABLE_BORDER;
            case "ICON_BUTTON_NORMAL" -> ICON_BUTTON_NORMAL;
            case "ICON_BUTTON_DISABLED" -> ICON_BUTTON_DISABLED;
            case "ICON_BUTTON_HOVER" -> ICON_BUTTON_HOVER;
            case "IN_WORLD_BLOCK_HIGHLIGHT" -> IN_WORLD_BLOCK_HIGHLIGHT;
            case "SCENE_BACKGROUND" -> SYMBOLIC_SCENE_BACKGROUND;
            case "GUIDE_SCREEN_BACKGROUND" -> GUIDE_SCREEN_BACKGROUND;
            case "BLOCKQUOTE_BACKGROUND" -> BLOCKQUOTE_BACKGROUND;
            case "BLACK" -> MC_BLACK;
            case "DARK_BLUE" -> MC_DARK_BLUE;
            case "DARK_GREEN" -> MC_DARK_GREEN;
            case "DARK_AQUA" -> MC_DARK_AQUA;
            case "DARK_RED" -> MC_DARK_RED;
            case "DARK_PURPLE" -> MC_DARK_PURPLE;
            case "GOLD" -> MC_GOLD;
            case "GRAY" -> MC_GRAY;
            case "DARK_GRAY" -> MC_DARK_GRAY;
            case "BLUE" -> MC_BLUE;
            case "GREEN" -> MC_GREEN;
            case "AQUA" -> MC_AQUA;
            case "RED" -> MC_RED;
            case "LIGHT_PURPLE" -> MC_LIGHT_PURPLE;
            case "YELLOW" -> MC_YELLOW;
            case "WHITE" -> MC_WHITE;
            default -> null;
        };
    }

    public static final ColorResource SCENE_BACKGROUND = COLORS.argb("sceneBackground", "0xFF0A0A10");
    public static final ColorResource SCENE_BORDER = COLORS.argb("sceneBorder", "0xFF303040");
    public static final ColorResource X_AXIS = COLORS.argb("xAxis", "0xFFFF5A5A");
    public static final ColorResource Y_AXIS = COLORS.argb("yAxis", "0xFF67E26C");
    public static final ColorResource Z_AXIS = COLORS.argb("zAxis", "0xFF64A8FF");
    public static final ColorResource XY_PLANE = COLORS.argb("xyPlane", "0xD8FFD45A");
    public static final ColorResource YZ_PLANE = COLORS.argb("yzPlane", "0xD85AE9FF");
    public static final ColorResource ZX_PLANE = COLORS.argb("zxPlane", "0xD8F16BFF");
    public static final ColorResource HIGHLIGHT = COLORS.argb("highlight", "0x8000FFAA");

    public static final ColorResource CHART_BACKGROUND = COLORS.argb("chartBackground", "0xFF1B1F23");
    public static final ColorResource CHART_BORDER = COLORS.argb("chartBorder", "0xFF3A4047");
    public static final ColorResource CHART_AXIS = COLORS.argb("chartAxis", "0xFFB8C2CF");
    public static final ColorResource CHART_GRID = COLORS.argb("chartGrid", "0x33B8C2CF");
    public static final ColorResource CHART_TITLE = COLORS.argb("chartTitle", "0xFFE0E0E0");
    public static final ColorResource CHART_LABEL = COLORS.argb("chartLabel", "0xFFB8C2CF");

    public static final ColorResource SCROLLBAR_TRACK = COLORS.argb("scrollbarTrack", "0x35101010");
    public static final ColorResource SCROLLBAR_THUMB = COLORS.argb("scrollbarThumb", "0xA0D8D8D8");
    public static final ColorResource SCROLLBAR_HOVER = COLORS.argb("scrollbarHover", "0x889AA3B2");

    public static final ColorResource[] CHART_PALETTE = { COLORS.argb("chartPalette01", "0xFFE15759"),
        COLORS.argb("chartPalette02", "0xFF4E79A7"), COLORS.argb("chartPalette03", "0xFF59A14F"),
        COLORS.argb("chartPalette04", "0xFFF28E2B"), COLORS.argb("chartPalette05", "0xFF76B7B2"),
        COLORS.argb("chartPalette06", "0xFFEDC948"), COLORS.argb("chartPalette07", "0xFFB07AA1"),
        COLORS.argb("chartPalette08", "0xFFFF9DA7"), COLORS.argb("chartPalette09", "0xFF9C755F"),
        COLORS.argb("chartPalette10", "0xFFBAB0AC"), COLORS.argb("chartPalette11", "0xFF1F77B4"),
        COLORS.argb("chartPalette12", "0xFFFF7F0E"), COLORS.argb("chartPalette13", "0xFF2CA02C"),
        COLORS.argb("chartPalette14", "0xFFD62728"), COLORS.argb("chartPalette15", "0xFF9467BD"),
        COLORS.argb("chartPalette16", "0xFF8C564B") };

    public static final ColorResource[] FUNCTION_GRAPH_PALETTE = { COLORS.argb("functionGraphPalette01", "0xFFE15759"),
        COLORS.argb("functionGraphPalette02", "0xFF4E79A7"), COLORS.argb("functionGraphPalette03", "0xFF59A14F"),
        COLORS.argb("functionGraphPalette04", "0xFFF28E2B"), COLORS.argb("functionGraphPalette05", "0xFF76B7B2"),
        COLORS.argb("functionGraphPalette06", "0xFFB07AA1"), COLORS.argb("functionGraphPalette07", "0xFFEDC948"),
        COLORS.argb("functionGraphPalette08", "0xFF9C755F"), COLORS.argb("functionGraphPalette09", "0xFFFF9DA7"),
        COLORS.argb("functionGraphPalette10", "0xFF1F77B4"), COLORS.argb("functionGraphPalette11", "0xFFFF7F0E"),
        COLORS.argb("functionGraphPalette12", "0xFF2CA02C"), COLORS.argb("functionGraphPalette13", "0xFFD62728"),
        COLORS.argb("functionGraphPalette14", "0xFF9467BD") };

    public static final ColorResource ARGB_0E0E20 = COLORS.rgb("color0E0E20", "0x0E0E20");
    public static final ColorResource ARGB_10000000 = COLORS.argb("color10000000", "0x10000000");
    public static final ColorResource ARGB_121216 = COLORS.rgb("color121216", "0x121216");
    public static final ColorResource ARGB_1A0C1117 = COLORS.argb("color1A0C1117", "0x1A0C1117");
    public static final ColorResource ARGB_1A6FB6FF = COLORS.argb("color1A6FB6FF", "0x1A6FB6FF");
    public static final ColorResource ARGB_1AF0F6FF = COLORS.argb("color1AF0F6FF", "0x1AF0F6FF");
    public static final ColorResource ARGB_20FFFFFF = COLORS.argb("color20FFFFFF", "0x20FFFFFF");
    public static final ColorResource ARGB_22262D38 = COLORS.argb("color22262D38", "0x22262D38");
    public static final ColorResource ARGB_22FFFFFF = COLORS.argb("color22FFFFFF", "0x22FFFFFF");
    public static final ColorResource ARGB_262A3340 = COLORS.argb("color262A3340", "0x262A3340");
    public static final ColorResource MERMAID_SUBGRAPH_BACKGROUND_DARK = COLORS
        .argb("mermaidSubgraphBackgroundDark", "0x301E2A2A");
    public static final ColorResource MERMAID_SUBGRAPH_BACKGROUND_PURPLE = COLORS
        .argb("mermaidSubgraphBackgroundPurple", "0x301E2A45");
    public static final ColorResource ARGB_30242B33 = COLORS.argb("color30242B33", "0x30242B33");
    public static final ColorResource MERMAID_SUBGRAPH_BACKGROUND_VIOLET = COLORS
        .argb("mermaidSubgraphBackgroundViolet", "0x302A1E45");
    public static final ColorResource MERMAID_SUBGRAPH_BACKGROUND_GOLD = COLORS
        .argb("mermaidSubgraphBackgroundGold", "0x302A2A1E");
    public static final ColorResource ARGB_33101012 = COLORS.argb("color33101012", "0x33101012");
    public static final ColorResource ARGB_33262D38 = COLORS.argb("color33262D38", "0x33262D38");
    public static final ColorResource ARGB_33FFFFFF = COLORS.argb("color33FFFFFF", "0x33FFFFFF");
    public static final ColorResource ARGB_34101018 = COLORS.argb("color34101018", "0x34101018");
    public static final ColorResource ARGB_40000000 = COLORS.argb("color40000000", "0x40000000");
    public static final ColorResource ARGB_40FFFFFF = COLORS.argb("color40FFFFFF", "0x40FFFFFF");
    public static final ColorResource ARGB_4438BDF8 = COLORS.argb("color4438BDF8", "0x4438BDF8");
    public static final ColorResource ARGB_44FFFFFF = COLORS.argb("color44FFFFFF", "0x44FFFFFF");
    public static final ColorResource ARGB_4CFFFFFF = COLORS.argb("color4CFFFFFF", "0x4CFFFFFF");
    public static final ColorResource ARGB_4D000000 = COLORS.argb("color4D000000", "0x4D000000");
    public static final ColorResource ARGB_4D6E7681 = COLORS.argb("color4D6E7681", "0x4D6E7681");
    public static final ColorResource ARGB_5028007F = COLORS.argb("color5028007F", "0x5028007F");
    public static final ColorResource ARGB_505000FF = COLORS.argb("color505000FF", "0x505000FF");
    public static final ColorResource ARGB_5512181C = COLORS.argb("color5512181C", "0x5512181C");
    public static final ColorResource ARGB_5522262C = COLORS.argb("color5522262C", "0x5522262C");
    public static final ColorResource ARGB_55FFFFFF = COLORS.argb("color55FFFFFF", "0x55FFFFFF");
    public static final ColorResource ARGB_60FFFFFF = COLORS.argb("color60FFFFFF", "0x60FFFFFF");
    public static final ColorResource ARGB_6600F5FF = COLORS.argb("color6600F5FF", "0x6600F5FF");
    public static final ColorResource ARGB_661E232B = COLORS.argb("color661E232B", "0x661E232B");
    public static final ColorResource ARGB_6622262C = COLORS.argb("color6622262C", "0x6622262C");
    public static final ColorResource ARGB_663D89C9 = COLORS.argb("color663D89C9", "0x663D89C9");
    public static final ColorResource ARGB_66434C57 = COLORS.argb("color66434C57", "0x66434C57");
    public static final ColorResource ARGB_66464A50 = COLORS.argb("color66464A50", "0x66464A50");
    public static final ColorResource ARGB_6656C8FF = COLORS.argb("color6656C8FF", "0x6656C8FF");
    public static final ColorResource ARGB_66586275 = COLORS.argb("color66586275", "0x66586275");
    public static final ColorResource ARGB_665A5A5A = COLORS.argb("color665A5A5A", "0x665A5A5A");
    public static final ColorResource ARGB_66AA2222 = COLORS.argb("color66AA2222", "0x66AA2222");
    public static final ColorResource ARGB_66FFFFFF = COLORS.argb("color66FFFFFF", "0x66FFFFFF");
    public static final ColorResource ARGB_70000000 = COLORS.argb("color70000000", "0x70000000");
    public static final ColorResource ARGB_7A1C252E = COLORS.argb("color7A1C252E", "0x7A1C252E");
    public static final ColorResource ARGB_80000000 = COLORS.argb("color80000000", "0x80000000");
    public static final ColorResource ARGB_802A2A2A = COLORS.argb("color802A2A2A", "0x802A2A2A");
    public static final ColorResource ARGB_80768496 = COLORS.argb("color80768496", "0x80768496");
    public static final ColorResource ARGB_80AAAADD = COLORS.argb("color80AAAADD", "0x80AAAADD");
    public static final ColorResource ARGB_80FFFFFF = COLORS.argb("color80FFFFFF", "0x80FFFFFF");
    public static final ColorResource ARGB_88000000 = COLORS.argb("color88000000", "0x88000000");
    public static final ColorResource ARGB_88303946 = COLORS.argb("color88303946", "0x88303946");
    public static final ColorResource ARGB_88FFFFFF = COLORS.argb("color88FFFFFF", "0x88FFFFFF");
    public static final ColorResource ARGB_8A00CAF2 = COLORS.argb("color8A00CAF2", "0x8A00CAF2");
    public static final ColorResource ARGB_94D049BB = COLORS.argb("color94D049BB", "0x94D049BB");
    public static final ColorResource ARGB_96D9B44A = COLORS.argb("color96D9B44A", "0x96D9B44A");
    public static final ColorResource MERMAID_SUBGRAPH_BORDER_BLUE = COLORS
        .argb("mermaidSubgraphBorderBlue", "0x99434C57");
    public static final ColorResource MERMAID_SUBGRAPH_BORDER_TEAL = COLORS
        .argb("mermaidSubgraphBorderTeal", "0x9943574C");
    public static final ColorResource MERMAID_SUBGRAPH_BORDER_GREEN = COLORS
        .argb("mermaidSubgraphBorderGreen", "0x994C5743");
    public static final ColorResource MERMAID_SUBGRAPH_BORDER_OLIVE = COLORS
        .argb("mermaidSubgraphBorderOlive", "0x99575743");
    public static final ColorResource ARGB_99B8C0CC = COLORS.argb("color99B8C0CC", "0x99B8C0CC");
    public static final ColorResource ARGB_9E3779B9 = COLORS.argb("color9E3779B9", "0x9E3779B9");
    public static final ColorResource ARGB_A0121216 = COLORS.argb("colorA0121216", "0xA0121216");
    public static final ColorResource ARGB_A014161A = COLORS.argb("colorA014161A", "0xA014161A");
    public static final ColorResource ARGB_A0AAB5C2 = COLORS.argb("colorA0AAB5C2", "0xA0AAB5C2");
    public static final ColorResource ARGB_A6181A20 = COLORS.argb("colorA6181A20", "0xA6181A20");
    public static final ColorResource ARGB_AA111922 = COLORS.argb("colorAA111922", "0xAA111922");
    public static final ColorResource ARGB_AA1CB4E9 = COLORS.argb("colorAA1CB4E9", "0xAA1CB4E9");
    public static final ColorResource ARGB_AAFFC107 = COLORS.argb("colorAAFFC107", "0xAAFFC107");
    public static final ColorResource ARGB_BF58476D = COLORS.argb("colorBF58476D", "0xBF58476D");
    public static final ColorResource ARGB_C0AAAADD = COLORS.argb("colorC0AAAADD", "0xC0AAAADD");
    public static final ColorResource ARGB_C0FFFFFF = COLORS.argb("colorC0FFFFFF", "0xC0FFFFFF");
    public static final ColorResource ARGB_C824303A = COLORS.argb("colorC824303A", "0xC824303A");
    public static final ColorResource ARGB_CBF29CE4 = COLORS.argb("colorCBF29CE4", "0xCBF29CE4");
    public static final ColorResource ARGB_CC00FF00 = COLORS.argb("colorCC00FF00", "0xCC00FF00");
    public static final ColorResource ARGB_CC0C1117 = COLORS.argb("colorCC0C1117", "0xCC0C1117");
    public static final ColorResource ARGB_CC0E0E20 = COLORS.argb("colorCC0E0E20", "0xCC0E0E20");
    public static final ColorResource ARGB_CC0F0F12 = COLORS.argb("colorCC0F0F12", "0xCC0F0F12");
    public static final ColorResource ARGB_CC2A3A46 = COLORS.argb("colorCC2A3A46", "0xCC2A3A46");
    public static final ColorResource ARGB_CC768496 = COLORS.argb("colorCC768496", "0xCC768496");
    public static final ColorResource ARGB_CCEAF6FF = COLORS.argb("colorCCEAF6FF", "0xCCEAF6FF");
    public static final ColorResource ARGB_D0000000 = COLORS.argb("colorD0000000", "0xD0000000");
    public static final ColorResource ARGB_D0202020 = COLORS.argb("colorD0202020", "0xD0202020");
    public static final ColorResource ARGB_D8FFFFFF = COLORS.argb("colorD8FFFFFF", "0xD8FFFFFF");
    public static final ColorResource ARGB_E0101010 = COLORS.argb("colorE0101010", "0xE0101010");
    public static final ColorResource ARGB_E0151515 = COLORS.argb("colorE0151515", "0xE0151515");
    public static final ColorResource ARGB_F00C1117 = COLORS.argb("colorF00C1117", "0xF00C1117");
    public static final ColorResource ARGB_F0100010 = COLORS.argb("colorF0100010", "0xF0100010");
    public static final ColorResource ARGB_F0181818 = COLORS.argb("colorF0181818", "0xF0181818");
    public static final ColorResource ARGB_F0F0F0 = COLORS.rgb("colorF0F0F0", "0xF0F0F0");
    public static final ColorResource ARGB_F8FFFFFF = COLORS.argb("colorF8FFFFFF", "0xF8FFFFFF");
    public static final ColorResource ARGB_FF00008B = COLORS.argb("colorFF00008B", "0xFF00008B");
    public static final ColorResource ARGB_FF0000FF = COLORS.argb("colorFF0000FF", "0xFF0000FF");
    public static final ColorResource ARGB_FF006400 = COLORS.argb("colorFF006400", "0xFF006400");
    public static final ColorResource ARGB_FF008B8B = COLORS.argb("colorFF008B8B", "0xFF008B8B");
    public static final ColorResource ARGB_FF00D2FC = COLORS.argb("colorFF00D2FC", "0xFF00D2FC");
    public static final ColorResource ARGB_FF00E000 = COLORS.argb("colorFF00E000", "0xFF00E000");
    public static final ColorResource ARGB_FF00FF00 = COLORS.argb("colorFF00FF00", "0xFF00FF00");
    public static final ColorResource ARGB_FF00FFFF = COLORS.argb("colorFF00FFFF", "0xFF00FFFF");
    public static final ColorResource ARGB_FF0D1117 = COLORS.argb("colorFF0D1117", "0xFF0D1117");
    public static final ColorResource ARGB_FF111922 = COLORS.argb("colorFF111922", "0xFF111922");
    public static final ColorResource ARGB_FF121216 = COLORS.argb("colorFF121216", "0xFF121216");
    public static final ColorResource ARGB_FF161B22 = COLORS.argb("colorFF161B22", "0xFF161B22");
    public static final ColorResource ARGB_FF1E1E1E = COLORS.argb("colorFF1E1E1E", "0xFF1E1E1E");
    public static final ColorResource ARGB_FF1F2A38 = COLORS.argb("colorFF1F2A38", "0xFF1F2A38");
    public static final ColorResource ARGB_FF202020 = COLORS.argb("colorFF202020", "0xFF202020");
    public static final ColorResource ARGB_FF262A33 = COLORS.argb("colorFF262A33", "0xFF262A33");
    public static final ColorResource ARGB_FF2A2A2A = COLORS.argb("colorFF2A2A2A", "0xFF2A2A2A");
    public static final ColorResource ARGB_FF2D3137 = COLORS.argb("colorFF2D3137", "0xFF2D3137");
    public static final ColorResource ARGB_FF30363D = COLORS.argb("colorFF30363D", "0xFF30363D");
    public static final ColorResource ARGB_FF333333 = COLORS.argb("colorFF333333", "0xFF333333");
    public static final ColorResource ARGB_FF33404C = COLORS.argb("colorFF33404C", "0xFF33404C");
    public static final ColorResource ARGB_FF373737 = COLORS.argb("colorFF373737", "0xFF373737");
    public static final ColorResource ARGB_FF3A3A3A = COLORS.argb("colorFF3A3A3A", "0xFF3A3A3A");
    public static final ColorResource ARGB_FF464A50 = COLORS.argb("colorFF464A50", "0xFF464A50");
    public static final ColorResource ARGB_FF46505A = COLORS.argb("colorFF46505A", "0xFF46505A");
    public static final ColorResource ARGB_FF4A4A4A = COLORS.argb("colorFF4A4A4A", "0xFF4A4A4A");
    public static final ColorResource ARGB_FF4D5661 = COLORS.argb("colorFF4D5661", "0xFF4D5661");
    public static final ColorResource ARGB_FF4FA3FF = COLORS.argb("colorFF4FA3FF", "0xFF4FA3FF");
    public static final ColorResource ARGB_FF53565C = COLORS.argb("colorFF53565C", "0xFF53565C");
    public static final ColorResource ARGB_FF555555 = COLORS.argb("colorFF555555", "0xFF555555");
    public static final ColorResource ARGB_FF586170 = COLORS.argb("colorFF586170", "0xFF586170");
    public static final ColorResource ARGB_FF5D6C7C = COLORS.argb("colorFF5D6C7C", "0xFF5D6C7C");
    public static final ColorResource ARGB_FF5EA8FF = COLORS.argb("colorFF5EA8FF", "0xFF5EA8FF");
    public static final ColorResource ARGB_FF61B75D = COLORS.argb("colorFF61B75D", "0xFF61B75D");
    public static final ColorResource ARGB_FF638EF1 = COLORS.argb("colorFF638EF1", "0xFF638EF1");
    public static final ColorResource ARGB_FF666666 = COLORS.argb("colorFF666666", "0xFF666666");
    public static final ColorResource ARGB_FF737A82 = COLORS.argb("colorFF737A82", "0xFF737A82");
    public static final ColorResource ARGB_FF73DACA = COLORS.argb("colorFF73DACA", "0xFF73DACA");
    public static final ColorResource ARGB_FF79C0FF = COLORS.argb("colorFF79C0FF", "0xFF79C0FF");
    public static final ColorResource ARGB_FF7A7A7A = COLORS.argb("colorFF7A7A7A", "0xFF7A7A7A");
    public static final ColorResource ARGB_FF7AA2F7 = COLORS.argb("colorFF7AA2F7", "0xFF7AA2F7");
    public static final ColorResource ARGB_FF7C8795 = COLORS.argb("colorFF7C8795", "0xFF7C8795");
    public static final ColorResource ARGB_FF7DCFFF = COLORS.argb("colorFF7DCFFF", "0xFF7DCFFF");
    public static final ColorResource ARGB_FF7EE787 = COLORS.argb("colorFF7EE787", "0xFF7EE787");
    public static final ColorResource ARGB_FF800080 = COLORS.argb("colorFF800080", "0xFF800080");
    public static final ColorResource ARGB_FF808080 = COLORS.argb("colorFF808080", "0xFF808080");
    public static final ColorResource ARGB_FF8755DD = COLORS.argb("colorFF8755DD", "0xFF8755DD");
    public static final ColorResource ARGB_FF888888 = COLORS.argb("colorFF888888", "0xFF888888");
    public static final ColorResource ARGB_FF88BBFF = COLORS.argb("colorFF88BBFF", "0xFF88BBFF");
    public static final ColorResource ARGB_FF8A6A00 = COLORS.argb("colorFF8A6A00", "0xFF8A6A00");
    public static final ColorResource ARGB_FF8B0000 = COLORS.argb("colorFF8B0000", "0xFF8B0000");
    public static final ColorResource ARGB_FF8B8B8B = COLORS.argb("colorFF8B8B8B", "0xFF8B8B8B");
    public static final ColorResource ARGB_FF8B949E = COLORS.argb("colorFF8B949E", "0xFF8B949E");
    public static final ColorResource ARGB_FF8FC7FF = COLORS.argb("colorFF8FC7FF", "0xFF8FC7FF");
    public static final ColorResource ARGB_FF9AA3B2 = COLORS.argb("colorFF9AA3B2", "0xFF9AA3B2");
    public static final ColorResource ARGB_FF9ECE6A = COLORS.argb("colorFF9ECE6A", "0xFF9ECE6A");
    public static final ColorResource ARGB_FF9FC6FF = COLORS.argb("colorFF9FC6FF", "0xFF9FC6FF");
    public static final ColorResource ARGB_FF9FFFB0 = COLORS.argb("colorFF9FFFB0", "0xFF9FFFB0");
    public static final ColorResource ARGB_FFA0A0A0 = COLORS.argb("colorFFA0A0A0", "0xFFA0A0A0");
    public static final ColorResource ARGB_FFA5D6FF = COLORS.argb("colorFFA5D6FF", "0xFFA5D6FF");
    public static final ColorResource ARGB_FFAAAAAA = COLORS.argb("colorFFAAAAAA", "0xFFAAAAAA");
    public static final ColorResource ARGB_FFB8C0CC = COLORS.argb("colorFFB8C0CC", "0xFFB8C0CC");
    public static final ColorResource ARGB_FFBBBBBB = COLORS.argb("colorFFBBBBBB", "0xFFBBBBBB");
    public static final ColorResource ARGB_FFC0C0FF = COLORS.argb("colorFFC0C0FF", "0xFFC0C0FF");
    public static final ColorResource ARGB_FFC47BA1 = COLORS.argb("colorFFC47BA1", "0xFFC47BA1");
    public static final ColorResource ARGB_FFC79D3E = COLORS.argb("colorFFC79D3E", "0xFFC79D3E");
    public static final ColorResource ARGB_FFCCCCCC = COLORS.argb("colorFFCCCCCC", "0xFFCCCCCC");
    public static final ColorResource ARGB_FFCDD6E1 = COLORS.argb("colorFFCDD6E1", "0xFFCDD6E1");
    public static final ColorResource ARGB_FFD2A8FF = COLORS.argb("colorFFD2A8FF", "0xFFD2A8FF");
    public static final ColorResource ARGB_FFD5DCE7 = COLORS.argb("colorFFD5DCE7", "0xFFD5DCE7");
    public static final ColorResource ARGB_FFD7DEE7 = COLORS.argb("colorFFD7DEE7", "0xFFD7DEE7");
    public static final ColorResource ARGB_FFD8E9FF = COLORS.argb("colorFFD8E9FF", "0xFFD8E9FF");
    public static final ColorResource ARGB_FFE0AF68 = COLORS.argb("colorFFE0AF68", "0xFFE0AF68");
    public static final ColorResource ARGB_FFE2E6ED = COLORS.argb("colorFFE2E6ED", "0xFFE2E6ED");
    public static final ColorResource ARGB_FFE46150 = COLORS.argb("colorFFE46150", "0xFFE46150");
    public static final ColorResource ARGB_FFE5E9F0 = COLORS.argb("colorFFE5E9F0", "0xFFE5E9F0");
    public static final ColorResource ARGB_FFE6E6E6 = COLORS.argb("colorFFE6E6E6", "0xFFE6E6E6");
    public static final ColorResource ARGB_FFE6EDF3 = COLORS.argb("colorFFE6EDF3", "0xFFE6EDF3");
    public static final ColorResource ARGB_FFE8A317 = COLORS.argb("colorFFE8A317", "0xFFE8A317");
    public static final ColorResource ARGB_FFE8E8E8 = COLORS.argb("colorFFE8E8E8", "0xFFE8E8E8");
    public static final ColorResource ARGB_FFE8EDF5 = COLORS.argb("colorFFE8EDF5", "0xFFE8EDF5");
    public static final ColorResource ARGB_FFEAF6FF = COLORS.argb("colorFFEAF6FF", "0xFFEAF6FF");
    public static final ColorResource ARGB_FFEEEEEE = COLORS.argb("colorFFEEEEEE", "0xFFEEEEEE");
    public static final ColorResource ARGB_FFF1F6FB = COLORS.argb("colorFFF1F6FB", "0xFFF1F6FB");
    public static final ColorResource ARGB_FFF4F7FB = COLORS.argb("colorFFF4F7FB", "0xFFF4F7FB");
    public static final ColorResource ARGB_FFF4FBFF = COLORS.argb("colorFFF4FBFF", "0xFFF4FBFF");
    public static final ColorResource ARGB_FFF7768E = COLORS.argb("colorFFF7768E", "0xFFF7768E");
    public static final ColorResource ARGB_FFFF0000 = COLORS.argb("colorFFFF0000", "0xFFFF0000");
    public static final ColorResource ARGB_FFFF00FF = COLORS.argb("colorFFFF00FF", "0xFFFF00FF");
    public static final ColorResource ARGB_FFFF5555 = COLORS.argb("colorFFFF5555", "0xFFFF5555");
    public static final ColorResource ARGB_FFFF7777 = COLORS.argb("colorFFFF7777", "0xFFFF7777");
    public static final ColorResource ARGB_FFFF7B72 = COLORS.argb("colorFFFF7B72", "0xFFFF7B72");
    public static final ColorResource ARGB_FFFF8484 = COLORS.argb("colorFFFF8484", "0xFFFF8484");
    public static final ColorResource ARGB_FFFF9999 = COLORS.argb("colorFFFF9999", "0xFFFF9999");
    public static final ColorResource ARGB_FFFFA500 = COLORS.argb("colorFFFFA500", "0xFFFFA500");
    public static final ColorResource ARGB_FFFFA657 = COLORS.argb("colorFFFFA657", "0xFFFFA657");
    public static final ColorResource ARGB_FFFFC07A = COLORS.argb("colorFFFFC07A", "0xFFFFC07A");
    public static final ColorResource ARGB_FFFFCC55 = COLORS.argb("colorFFFFCC55", "0xFFFFCC55");
    public static final ColorResource ARGB_FFFFD254 = COLORS.argb("colorFFFFD254", "0xFFFFD254");
    public static final ColorResource ARGB_FFFFE16A = COLORS.argb("colorFFFFE16A", "0xFFFFE16A");
    public static final ColorResource ARGB_FFFFF1A8 = COLORS.argb("colorFFFFF1A8", "0xFFFFF1A8");
    public static final ColorResource ARGB_FFFFFF00 = COLORS.argb("colorFFFFFF00", "0xFFFFFF00");

    public static int getColor(ColorResource resource) {
        return resource.getColor();
    }

    public static void applyGlColor(int argb) {
        GL11.glColor4f(red(argb) / 255.0F, green(argb) / 255.0F, blue(argb) / 255.0F, alpha(argb) / 255.0F);
    }

    public static void applyGlColor(float red, float green, float blue, float alpha) {
        GL11.glColor4f(red, green, blue, alpha);
    }

    public static void applyWhite(float alpha) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
    }

    public static ConstantColor constant(ColorResource resource) {
        return new ConstantColor(resource.getColor());
    }

    public static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    public static int red(int argb) {
        return (argb >>> 16) & 0xFF;
    }

    public static int green(int argb) {
        return (argb >>> 8) & 0xFF;
    }

    public static int blue(int argb) {
        return argb & 0xFF;
    }

    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    public static int rgb(int red, int green, int blue) {
        return argb(0xFF, red, green, blue);
    }

    public static int mono(int w) {
        return rgb(w, w, w);
    }

    public static int lerp(float factor, int from, int to) {
        return argb(
            (int) (alpha(from) + factor * (alpha(to) - alpha(from))),
            (int) (red(from) + factor * (red(to) - red(from))),
            (int) (green(from) + factor * (green(to) - green(from))),
            (int) (blue(from) + factor * (blue(to) - blue(from))));
    }

    /**
     * Converts a hexadecimal color string to a packed RGB or ARGB integer. If no alpha is given, assumes alpha 255. The
     * order of colors in the hex string follows CSS notations (#RRGGBBAA or #RGBA).
     *
     * @param hexColor The color string in hex format (with or without #)
     * @return The packed RGB value as an integer
     * @throws IllegalArgumentException if the input format is invalid
     */
    public static int hexToRgb(String hexColor) {
        if (!hexColor.isEmpty()) {
            int start = 0;
            if (hexColor.charAt(0) == '#') {
                start++; // Skip leading #
            }

            int remainingChars = hexColor.length() - start;
            // #rgb
            if (remainingChars == 3 || remainingChars == 4) {
                int r = fromHexChar(hexColor.charAt(start));
                int g = fromHexChar(hexColor.charAt(start + 1));
                int b = fromHexChar(hexColor.charAt(start + 2));
                int a = 15;
                if (remainingChars == 4) {
                    a = fromHexChar(hexColor.charAt(start + 3));
                }
                if (r != -1 && g != -1 && b != -1 && a != -1) {
                    return argb(a << 4 | a, r << 4 | r, g << 4 | g, b << 4 | b);
                }
            } else if (remainingChars == 6 || remainingChars == 8) {
                int rHi = fromHexChar(hexColor.charAt(start));
                int rLo = fromHexChar(hexColor.charAt(start + 1));
                int gHi = fromHexChar(hexColor.charAt(start + 2));
                int gLo = fromHexChar(hexColor.charAt(start + 3));
                int bHi = fromHexChar(hexColor.charAt(start + 4));
                int bLo = fromHexChar(hexColor.charAt(start + 5));
                int aHi = 15, aLo = 15;
                if (remainingChars == 8) {
                    aHi = fromHexChar(hexColor.charAt(start + 6));
                    aLo = fromHexChar(hexColor.charAt(start + 7));
                }
                if (rHi != -1 && rLo != -1
                    && gHi != -1
                    && gLo != -1
                    && bHi != -1
                    && bLo != -1
                    && aHi != -1
                    && aLo != -1) {
                    return argb(aHi << 4 | aLo, rHi << 4 | rLo, gHi << 4 | gLo, bHi << 4 | bLo);
                }
            }
        }

        GuideDebugLog.error("[GuideNH] [Colors] Tried to parse an invalid hexadecimal color string: '{}'", hexColor);
        return 0;
    }

    public static int fromHexChar(int ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        } else if (ch >= 'a' && ch <= 'f') {
            return 0xa + (ch - 'a');
        } else if (ch >= 'A' && ch <= 'F') {
            return 0xa + (ch - 'A');
        } else {
            return -1;
        }
    }
}
