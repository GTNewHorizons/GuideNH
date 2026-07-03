package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.mermaid.MermaidEdgeStyle;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidParser;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidParser.NodeShapePattern;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidParser.NodeShapeResult;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidSourceExtractor;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;

public class FlowchartParser {

    private static final Pattern DIRECTION_PATTERN = Pattern
        .compile("^(?:flowchart|graph)\\s+(TB|BT|LR|RL)\\s*$", Pattern.CASE_INSENSITIVE);

    private static final Pattern SUBGRAPH_DIRECTION_PATTERN = Pattern
        .compile("^direction\\s+(TB|BT|LR|RL)\\s*$", Pattern.CASE_INSENSITIVE);

    private static final Pattern STYLE_DIRECTIVE = Pattern
        .compile("^style\\s+(\\S+)\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern CLASS_DEF = Pattern.compile("^classDef\\s+(\\S+)\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern CLASS_APPLY = Pattern.compile("^class\\s+(\\S+)\\s+(\\S+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern SUBGRAPH_START = Pattern.compile("^subgraph\\s*(.*)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern SUBGRAPH_END = Pattern.compile("^end\\s*$", Pattern.CASE_INSENSITIVE);

    private static final Pattern LINK_STYLE = Pattern
        .compile("^linkStyle\\s+(\\d+)\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    protected FlowchartParser() {}

    private static int findArrowSyntaxLength(String line) {
        if (line == null) return -1;
        int bestLen = -1;
        int bestIdx = Integer.MAX_VALUE;
        for (ArrowPattern ap : ArrowPattern.values()) {
            int idx = line.indexOf(ap.syntax);
            if (idx >= 0 && (idx < bestIdx || (idx == bestIdx && ap.syntax.length() > bestLen))) {
                bestIdx = idx;
                bestLen = ap.syntax.length();
            }
        }
        return bestLen > 0 ? bestLen : -1;
    }

    public static FlowchartDocument parse(String source) {
        String normalized = MermaidSourceExtractor.normalize(source);
        List<String> lines = GuideStringLines.splitLines(normalized);
        return parseLines(lines);
    }

    private static FlowchartDocument parseLines(List<String> lines) {
        FlowchartDirection direction = FlowchartDirection.TB;
        FlowchartLayoutMode layoutMode = FlowchartLayoutMode.BUILTIN;

        int index = 0;
        if (!lines.isEmpty() && MermaidSourceExtractor.isFrontmatterDelimiter(lines.getFirst())) {
            int end = MermaidSourceExtractor.findFrontmatterEnd(lines);
            if (end > 0) {
                List<String> frontmatter = lines.subList(1, end);
                direction = parseFrontmatterDirection(frontmatter, direction);
                layoutMode = parseFrontmatterLayout(frontmatter);
                index = end + 1;
            }
        }

        Map<String, FlowchartNode> nodes = new LinkedHashMap<>();
        List<FlowchartEdge> edges = new ArrayList<>();
        List<FlowchartSubgraph> subgraphs = new ArrayList<>();
        Map<String, String> styleOverrides = new LinkedHashMap<>();
        Map<String, String> classDefs = new LinkedHashMap<>();
        List<String> unresolvedNodeIds = new ArrayList<>();
        List<String> unresolvedNodeSources = new ArrayList<>();

        Deque<SubgraphContext> subgraphStack = new ArrayDeque<>();
        int subgraphCounter = 0;

        for (; index < lines.size(); index++) {
            String raw = lines.get(index);
            String trimmed = raw.trim();

            if (trimmed.isEmpty() || MermaidSourceExtractor.isCommentLine(trimmed)) {
                continue;
            }

            Matcher dirMatcher = DIRECTION_PATTERN.matcher(trimmed);
            if (dirMatcher.matches()) {
                direction = FlowchartDirection.fromString(dirMatcher.group(1));
                continue;
            }

            if (SUBGRAPH_END.matcher(trimmed)
                .matches()) {
                if (!subgraphStack.isEmpty()) {
                    SubgraphContext ctx = subgraphStack.pop();
                    FlowchartSubgraph sg = new FlowchartSubgraph(
                        ctx.id,
                        ctx.label,
                        new ArrayList<>(ctx.nodeIds),
                        new ArrayList<>(ctx.edges),
                        new ArrayList<>(ctx.children),
                        ctx.direction);
                    if (!subgraphStack.isEmpty()) {
                        subgraphStack.peek().children.add(sg);
                        subgraphStack.peek().nodeIds.addAll(ctx.nodeIds);
                    } else {
                        subgraphs.add(sg);
                    }
                }
                continue;
            }

            // Subgraph direction statement
            if (!subgraphStack.isEmpty()) {
                Matcher subDirMatcher = SUBGRAPH_DIRECTION_PATTERN.matcher(trimmed);
                if (subDirMatcher.matches()) {
                    subgraphStack.peek().direction = FlowchartDirection.fromString(subDirMatcher.group(1));
                    continue;
                }
            }

            Matcher subMatcher = SUBGRAPH_START.matcher(trimmed);
            if (subMatcher.matches()) {
                String subRaw = subMatcher.group(1)
                    .trim();
                String sgId = "subgraph_" + (subgraphCounter++);
                String sgLabel = null;

                int bracketIdx = findLabelStart(subRaw);
                if (bracketIdx >= 0) {
                    String maybeId = subRaw.substring(0, bracketIdx)
                        .trim();
                    if (!maybeId.isEmpty()) {
                        sgId = maybeId;
                    }
                    String rest = subRaw.substring(bracketIdx);
                    char open = rest.charAt(0);
                    char close = switch (open) {
                        case '[' -> ']';
                        case '(' -> ')';
                        case '"' -> '"';
                        case '\'' -> '\'';
                        default -> open;
                    };
                    int closeIdx = findMatchingClose(rest, open, close);
                    if (closeIdx > 0) {
                        sgLabel = rest.substring(1, closeIdx)
                            .trim();
                        sgLabel = MermaidParser.stripWrappingQuotes(sgLabel);
                    }
                } else if (!subRaw.isEmpty()) {
                    sgId = subRaw;
                }
                if (sgLabel == null || sgLabel.isEmpty()) {
                    sgLabel = sgId;
                }

                subgraphStack.push(new SubgraphContext(sgId, sgLabel));
                continue;
            }

            boolean insideSubgraph = !subgraphStack.isEmpty();
            if (!insideSubgraph || !isSubgraphLocalDirective(trimmed)) {
                Matcher styleMatcher = STYLE_DIRECTIVE.matcher(trimmed);
                if (styleMatcher.matches()) {
                    styleOverrides.put(styleMatcher.group(1), styleMatcher.group(2));
                    continue;
                }

                Matcher classDefMatcher = CLASS_DEF.matcher(trimmed);
                if (classDefMatcher.matches()) {
                    classDefs.put(classDefMatcher.group(1), classDefMatcher.group(2));
                    continue;
                }

                Matcher classApplyMatcher = CLASS_APPLY.matcher(trimmed);
                if (classApplyMatcher.matches()) {
                    String nodeId = classApplyMatcher.group(1);
                    String className = classApplyMatcher.group(2);
                    FlowchartNode existing = nodes.get(nodeId);
                    if (existing != null) {
                        List<String> updatedClasses = new ArrayList<>(existing.getClasses());
                        updatedClasses.add(className);
                        nodes.put(
                            nodeId,
                            new FlowchartNode(
                                existing.getId(),
                                existing.getLabel(),
                                existing.getShape(),
                                updatedClasses,
                                existing.getStyleOverride()));
                    }
                    continue;
                }
            }

            FlowchartEdge edge = tryParseEdge(trimmed);
            if (edge != null) {
                String processLine = trimmed;
                while (true) {
                    unresolvedNodeIds.add(edge.getFrom());
                    unresolvedNodeIds.add(edge.getTo());
                    edges.add(edge);
                    if (!subgraphStack.isEmpty()) {
                        SubgraphContext ctx = subgraphStack.peek();
                        ctx.edges.add(edge);
                        ctx.nodeIds.add(edge.getFrom());
                        ctx.nodeIds.add(edge.getTo());
                    }
                    int arrowLen = findArrowSyntaxLength(processLine);
                    if (arrowLen > 0) {
                        int arrowIdx = -1;
                        for (ArrowPattern ap : ArrowPattern.values()) {
                            int idx = processLine.indexOf(ap.syntax);
                            if (idx >= 0 && ap.syntax.length() == arrowLen) {
                                arrowIdx = idx;
                                break;
                            }
                        }
                        if (arrowIdx >= 0) {
                            String leftRaw = processLine.substring(0, arrowIdx)
                                .trim();
                            unresolvedNodeSources.add(leftRaw);
                            String rightRaw = processLine.substring(arrowIdx + arrowLen)
                                .trim();
                            if (rightRaw.startsWith("|")) {
                                int pipeEnd = rightRaw.indexOf('|', 1);
                                if (pipeEnd > 0) {
                                    rightRaw = rightRaw.substring(pipeEnd + 1)
                                        .trim();
                                }
                            }
                            unresolvedNodeSources.add(rightRaw);
                        }
                    }
                    int chainEnd = MermaidParser.findIdEnd(processLine);
                    if (chainEnd <= 0) break;
                    int aLen = findArrowSyntaxLength(processLine);
                    if (aLen <= 0) break;
                    int aStart = -1;
                    for (ArrowPattern ap : ArrowPattern.values()) {
                        int idx = processLine.indexOf(ap.syntax);
                        if (idx >= 0 && ap.syntax.length() == aLen) {
                            aStart = idx;
                            break;
                        }
                    }
                    if (aStart < 0) break;
                    String rPart = processLine.substring(aStart + aLen)
                        .trim();
                    if (rPart.startsWith("|")) {
                        int pipeEnd = rPart.indexOf('|', 1);
                        if (pipeEnd > 0) {
                            rPart = rPart.substring(pipeEnd + 1)
                                .trim();
                        }
                    }
                    int idEnd = MermaidParser.findIdEnd(rPart);
                    if (idEnd <= 0) break;
                    String rest = rPart.substring(idEnd)
                        .trim();
                    if (rest.isEmpty()) break;
                    int restArrowLen = findArrowSyntaxLength(rest);
                    if (restArrowLen <= 0) break;
                    String fromId = edge.getTo();
                    int restAStart = -1;
                    for (ArrowPattern ap : ArrowPattern.values()) {
                        int idx = rest.indexOf(ap.syntax);
                        if (idx >= 0 && ap.syntax.length() == restArrowLen) {
                            restAStart = idx;
                            break;
                        }
                    }
                    if (restAStart < 0) break;
                    String restRight = rest.substring(restAStart + restArrowLen)
                        .trim();
                    String restLabel = null;
                    if (restRight.startsWith("|")) {
                        int pipeEnd = restRight.indexOf('|', 1);
                        if (pipeEnd > 0) {
                            restLabel = restRight.substring(1, pipeEnd)
                                .trim();
                            restRight = restRight.substring(pipeEnd + 1)
                                .trim();
                        }
                    }
                    String toId = MermaidParser.extractLeadingId(restRight);
                    if (toId == null) break;
                    edge = new FlowchartEdge(
                        fromId,
                        toId,
                        restLabel != null ? MermaidParser.normalizeLabel(restLabel) : null,
                        edge.getStyle());
                    processLine = rest;
                }
                continue;
            }

            FlowchartNode node = tryParseNode(trimmed);
            if (node != null) {
                nodes.put(node.getId(), node);
                if (!subgraphStack.isEmpty()) {
                    subgraphStack.peek().nodeIds.add(node.getId());
                }
            }
        }

        for (String rawSource : unresolvedNodeSources) {
            FlowchartNode parsed = tryParseNode(rawSource);
            if (parsed != null) {
                nodes.putIfAbsent(parsed.getId(), parsed);
            }
        }
        for (String nodeId : unresolvedNodeIds) {
            nodes.putIfAbsent(nodeId, new FlowchartNode(nodeId, nodeId, MermaidNodeShape.DEFAULT, List.of(), null));
        }

        applyStyleOverrides(nodes, styleOverrides);
        applyClassDefs(nodes, classDefs);

        if (layoutMode == FlowchartLayoutMode.ELK) {
            System.err.println(
                "[FlowchartParser] Warning: 'layout: elk' is not supported yet. "
                    + "Using built-in orthogonal layout.");
        }

        return new FlowchartDocument(direction, nodes, edges, subgraphs, layoutMode);
    }

    private static FlowchartDirection parseFrontmatterDirection(List<String> lines, FlowchartDirection fallback) {
        for (String line : lines) {
            String trimmed = line.trim();
            int colon = trimmed.indexOf(':');
            if (colon <= 0) continue;
            String key = trimmed.substring(0, colon)
                .trim();
            if ("direction".equalsIgnoreCase(key)) {
                return FlowchartDirection.fromString(trimmed.substring(colon + 1));
            }
        }
        return fallback;
    }

    @Nullable
    private static FlowchartLayoutMode parseFrontmatterLayout(List<String> lines) {
        for (String line : lines) {
            String trimmed = line.trim();
            int colon = trimmed.indexOf(':');
            if (colon <= 0) continue;
            String key = trimmed.substring(0, colon)
                .trim();
            if ("layout".equalsIgnoreCase(key)) {
                return FlowchartLayoutMode.fromConfigValue(trimmed.substring(colon + 1));
            }
        }
        return FlowchartLayoutMode.BUILTIN;
    }

    private static boolean isSubgraphLocalDirective(String trimmed) {
        return STYLE_DIRECTIVE.matcher(trimmed)
            .matches()
            || CLASS_DEF.matcher(trimmed)
                .matches()
            || CLASS_APPLY.matcher(trimmed)
                .matches();
    }

    private static int findLabelStart(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '[' || c == '(' || c == '"' || c == '\'') {
                return i;
            }
        }
        return -1;
    }

    private static int findMatchingClose(String text, char open, char close) {
        if (open == '"' || open == '\'') {
            int end = text.indexOf(close, 1);
            return end > 0 ? end : -1;
        }
        int depth = 0;
        for (int i = 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == open) depth++;
            else if (c == close) {
                if (depth == 0) return i;
                depth--;
            }
        }
        return -1;
    }

    static FlowchartNode tryParseNode(String line) {
        if (line == null || line.isEmpty()) return null;
        String trimmed = line.trim();

        int colon = trimmed.indexOf(':');
        if (colon > 0) {
            String afterColon = trimmed.substring(colon + 1)
                .trim();
            if (!afterColon.isEmpty() && (afterColon.charAt(0) == '@' || MermaidParser.isShapeStart(afterColon.charAt(0)))) {
                trimmed = afterColon;
            } else {
                return null;
            }
        }

        int idEnd = MermaidParser.findIdEnd(trimmed);
        if (idEnd <= 0) return null;

        String id = trimmed.substring(0, idEnd);
        String rest = trimmed.substring(idEnd)
            .trim();

        if (rest.isEmpty()) return null;

        // ::: inline class operator
        List<String> inlineClasses = new ArrayList<>();
        if (rest.startsWith(":::")) {
            int spaceAfter = rest.indexOf(' ', 3);
            String classPart = spaceAfter > 0 ? rest.substring(3, spaceAfter).trim() : rest.substring(3).trim();
            if (!classPart.isEmpty()) {
                for (String cls : classPart.split("\\s+")) {
                    if (!cls.isEmpty()) inlineClasses.add(cls);
                }
            }
            rest = spaceAfter > 0 ? rest.substring(spaceAfter + 1).trim() : "";
            if (rest.isEmpty()) {
                return new FlowchartNode(id, id, MermaidNodeShape.DEFAULT, inlineClasses, null);
            }
        }

        // @{ shape: ..., icon: ..., img: ... } extended syntax
        if (rest.startsWith("@{")) {
            return tryParseExtendedNode(id, rest, inlineClasses);
        }

        NodeShapeResult result = NodeShapePattern.match(rest);
        if (result == null) return null;

        String rawLabel = result.label();
        String icon = extractIconPrefix(rawLabel);
        if (icon != null) {
            rawLabel = rawLabel.substring(rawLabel.indexOf(':') + 1).trim();
        }

        boolean markdownLabel = rawLabel.startsWith("`") && rawLabel.endsWith("`");
        if (markdownLabel) {
            rawLabel = rawLabel.substring(1, rawLabel.length() - 1).trim();
        }

        String label = MermaidParser.normalizeLabel(rawLabel);
        if (label.isEmpty()) {
            label = id;
        }

        return new FlowchartNode(id, label, result.shape(), inlineClasses, null, icon, markdownLabel, null);
    }

    @Nullable
    private static FlowchartNode tryParseExtendedNode(String id, String rest, List<String> inlineClasses) {
        int closeBrace = findMatchingBrace(rest);
        if (closeBrace < 0) return null;

        String content = rest.substring(2, closeBrace).trim();
        Map<String, String> props = new LinkedHashMap<>();
        parseKeyValues(content, props);

        String shapeName = props.get("shape");
        MermaidNodeShape shape = shapeName != null ? parseShapeName(shapeName) : MermaidNodeShape.DEFAULT;

        String label = props.get("label");
        if (label != null) {
            label = MermaidParser.stripWrappingQuotes(MermaidParser.normalizeLabel(label));
        }
        if (label == null || label.isEmpty()) {
            label = id;
        }

        String icon = null;
        if (props.containsKey("icon")) {
            icon = MermaidParser.stripWrappingQuotes(props.get("icon"));
        }
        if (icon == null && props.containsKey("img")) {
            icon = props.get("img");
        }

        return new FlowchartNode(id, label, shape, inlineClasses, null, icon, false, props);
    }

    private static int findMatchingBrace(String text) {
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static void parseKeyValues(String content, Map<String, String> props) {
        boolean inQuotes = false;
        int start = 0;
        for (int i = 0; i <= content.length(); i++) {
            if (i < content.length()) {
                char c = content.charAt(i);
                if (c == '"') {
                    inQuotes = !inQuotes;
                }
                if (c == ',' && !inQuotes) {
                    addKeyValue(content.substring(start, i).trim(), props);
                    start = i + 1;
                }
            } else {
                addKeyValue(content.substring(start).trim(), props);
            }
        }
    }

    private static void addKeyValue(String kv, Map<String, String> props) {
        int eq = kv.indexOf(':');
        if (eq <= 0) return;
        String key = kv.substring(0, eq).trim().toLowerCase(java.util.Locale.ROOT);
        String value = kv.substring(eq + 1).trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        if (!key.isEmpty() && !value.isEmpty()) {
            props.put(key, value);
        }
    }

    @Nullable
    private static String extractIconPrefix(String label) {
        if (label == null || label.isEmpty()) return null;
        int colon = label.indexOf(':');
        if (colon <= 0) return null;
        String prefix = label.substring(0, colon);
        if (prefix.equals("fa") || prefix.equals("fab") || prefix.equals("fas")
            || prefix.equals("far") || prefix.equals("fal") || prefix.equals("fad")
            || prefix.equals("fak")) {
            return label;
        }
        return null;
    }

    private static MermaidNodeShape parseShapeName(String name) {
        if (name == null) return MermaidNodeShape.DEFAULT;
        return switch (name.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "rect", "rectangle", "proc", "process" -> MermaidNodeShape.DEFAULT;
            case "rounded", "event" -> MermaidNodeShape.ROUNDED;
            case "stadium", "pill", "terminal" -> MermaidNodeShape.STADIUM;
            case "subprocess", "subproc", "subroutine", "framed-rectangle", "fr-rect" -> MermaidNodeShape.SUBPROCESS;
            case "diamond", "diam", "decision", "question" -> MermaidNodeShape.DIAMOND;
            case "cylinder", "cyl", "database", "db" -> MermaidNodeShape.CYLINDER;
            case "hexagon", "hex", "prepare" -> MermaidNodeShape.HEXAGON;
            case "circle", "circ", "start" -> MermaidNodeShape.CIRCLE;
            case "double-circle", "dbl-circ", "doublecircle" -> MermaidNodeShape.DOUBLE_CIRCLE;
            case "lean-r", "lean-right", "in-out" -> MermaidNodeShape.ASYMMETRIC;
            case "trapezoid", "trap-b", "priority", "trapezoid-bottom" -> MermaidNodeShape.TRAPEZOID;
            case "square" -> MermaidNodeShape.SQUARE;
            case "bang" -> MermaidNodeShape.BANG;
            case "cloud" -> MermaidNodeShape.CLOUD;
            default -> MermaidNodeShape.DEFAULT;
        };
    }

    private enum ArrowPattern {

        THICK_BOTH("<=>", MermaidEdgeStyle.THICK, true, true, '=', 2),
        THICK_FWD("==>", MermaidEdgeStyle.THICK, true, false, '=', 2),
        THICK_REV("<===", MermaidEdgeStyle.THICK, false, true, '=', 2),
        DOUBLE_FWD("--o", MermaidEdgeStyle.SOLID, true, false, '\0', 0),
        DOUBLE_REV("o--", MermaidEdgeStyle.SOLID, false, true, '\0', 0),
        DOUBLE_BOTH("o--o", MermaidEdgeStyle.SOLID, true, true, '\0', 0),
        CROSS_FWD("--x", MermaidEdgeStyle.SOLID, true, false, '\0', 0),
        CROSS_REV("x--", MermaidEdgeStyle.SOLID, false, true, '\0', 0),
        CROSS_BOTH("x--x", MermaidEdgeStyle.SOLID, true, true, '\0', 0),
        DASHED_DOT_FWD("-.->", MermaidEdgeStyle.DASHED, true, false, '.', 1),
        DASHED_DOT_REV("<-.--", MermaidEdgeStyle.DASHED, false, true, '.', 1),
        DASHED_DOT_BOTH("<-.->", MermaidEdgeStyle.DASHED, true, true, '.', 1),
        DOTTED_FWD("~~>", MermaidEdgeStyle.DOTTED, true, false, '~', 2),
        DOTTED_REV("<~~~", MermaidEdgeStyle.DOTTED, false, true, '~', 2),
        DOTTED_BOTH("<~~>", MermaidEdgeStyle.DOTTED, true, true, '~', 2),
        SOLID_FWD("-->", MermaidEdgeStyle.SOLID, true, false, '-', 2),
        SOLID_REV("<---", MermaidEdgeStyle.SOLID, false, true, '-', 2),
        SOLID_BOTH("<-->", MermaidEdgeStyle.SOLID, true, true, '-', 2),
        THICK_LINK("===", MermaidEdgeStyle.THICK, false, false, '=', 3),
        DASHED_LINK("-.-", MermaidEdgeStyle.DASHED, false, false, '.', 1),
        INVISIBLE_LINK("~~~", MermaidEdgeStyle.INVISIBLE, false, false, '~', 3),
        SOLID_LINK("---", MermaidEdgeStyle.SOLID, false, false, '-', 3);

        final String syntax;
        final MermaidEdgeStyle style;
        final boolean arrowFwd;
        final boolean arrowRev;
        final char repeatChar;
        final int minRepeat;

        ArrowPattern(String syntax, MermaidEdgeStyle style, boolean arrowFwd, boolean arrowRev,
            char repeatChar, int minRepeat) {
            this.syntax = syntax;
            this.style = style;
            this.arrowFwd = arrowFwd;
            this.arrowRev = arrowRev;
            this.repeatChar = repeatChar;
            this.minRepeat = minRepeat;
        }
    }

    static FlowchartEdge tryParseEdge(String line) {
        if (line == null || line.isEmpty()) return null;
        String trimmed = line.trim();

        // Edge ID prefix: e1@-->, myEdge@--- etc.
        String edgeId = null;
        String searchSpace = trimmed;
        int atIdx = trimmed.indexOf('@');
        if (atIdx > 0) {
            String candidateId = trimmed.substring(0, atIdx).trim();
            if (!candidateId.isEmpty() && MermaidParser.findIdEnd(candidateId) == candidateId.length()) {
                edgeId = candidateId;
                searchSpace = trimmed.substring(atIdx + 1).trim();
            }
        }

        ArrowPattern matched = null;
        int arrowStart = -1;

        for (ArrowPattern ap : ArrowPattern.values()) {
            int idx = searchSpace.indexOf(ap.syntax);
            if (idx >= 0) {
                if (matched == null || ap.syntax.length() > matched.syntax.length()) {
                    matched = ap;
                    arrowStart = idx;
                }
            }
        }

        if (matched == null) return null;

        // Compute length for variable-length arrows (solid, thick, tilde)
        int length = 0;
        if (matched.repeatChar != '\0' && matched.minRepeat > 0 && matched.repeatChar != '.') {
            int count = 0;
            for (int i = arrowStart; i < searchSpace.length(); i++) {
                if (searchSpace.charAt(i) == matched.repeatChar) count++;
                else break;
            }
            length = Math.max(0, count - matched.minRepeat);
        }

        // Recompute full syntax length for variable-length arrows
        int syntaxLen = matched.syntax.length();
        if (matched.repeatChar != '\0' && matched.minRepeat > 0 && matched.repeatChar != '.') {
            syntaxLen = 0;
            for (int i = arrowStart; i < searchSpace.length(); i++) {
                char c = searchSpace.charAt(i);
                if (c == matched.repeatChar) {
                    syntaxLen = i - arrowStart + 1;
                } else if (c == '>' || c == '<') {
                    syntaxLen = i - arrowStart + 1;
                    break;
                } else {
                    break;
                }
            }
        }
        if (syntaxLen <= 0) syntaxLen = matched.syntax.length();

        int arrowEnd = arrowStart + syntaxLen;

        String leftPart = searchSpace.substring(0, arrowStart)
            .trim();
        String fromId = MermaidParser.extractLeadingId(leftPart);
        if (fromId == null) return null;

        // Handle edge ID on the left side (before the @)
        if (edgeId == null && leftPart.length() > fromId.length()) {
            String afterFrom = leftPart.substring(fromId.length()).trim();
            if (afterFrom.startsWith("@")) {
                String restAfter = afterFrom.substring(1).trim();
                edgeId = fromId;
                fromId = MermaidParser.extractLeadingId(restAfter);
                if (fromId == null) return null;
            }
        }

        String rightPart = searchSpace.substring(arrowEnd)
            .trim();

        String label = null;
        String afterArrow = rightPart;
        if (afterArrow.startsWith("|")) {
            int pipeEnd = afterArrow.indexOf('|', 1);
            if (pipeEnd > 0) {
                label = afterArrow.substring(1, pipeEnd)
                    .trim();
                afterArrow = afterArrow.substring(pipeEnd + 1)
                    .trim();
            }
        }

        String toId = MermaidParser.extractLeadingId(afterArrow);
        if (toId == null) return null;

        String edgeLabel = label != null ? MermaidParser.normalizeLabel(label) : null;
        FlowchartEdge edge = new FlowchartEdge(fromId, toId, edgeLabel, matched.style, edgeId, length);

        if (matched.arrowRev && !matched.arrowFwd) {
            edge = new FlowchartEdge(toId, fromId, edgeLabel, matched.style, edgeId, length);
        }

        return edge;
    }

    private static void applyStyleOverrides(Map<String, FlowchartNode> nodes, Map<String, String> styleOverrides) {
        for (var entry : styleOverrides.entrySet()) {
            String nodeId = entry.getKey();
            FlowchartNode existing = nodes.get(nodeId);
            if (existing != null) {
                nodes.put(
                    nodeId,
                    new FlowchartNode(
                        existing.getId(),
                        existing.getLabel(),
                        existing.getShape(),
                        existing.getClasses(),
                        entry.getValue()));
            }
        }
    }

    private static void applyClassDefs(Map<String, FlowchartNode> nodes, Map<String, String> classDefs) {
        for (var entry : classDefs.entrySet()) {
            String className = entry.getKey();
            String style = entry.getValue();
            for (var nodeEntry : nodes.entrySet()) {
                FlowchartNode node = nodeEntry.getValue();
                if (node.getClasses()
                    .contains(className)) {
                    if (node.getStyleOverride() == null) {
                        nodeEntry.setValue(
                            new FlowchartNode(
                                node.getId(),
                                node.getLabel(),
                                node.getShape(),
                                node.getClasses(),
                                style));
                    }
                }
            }
        }
    }

    private static class SubgraphContext {

        final String id;
        final String label;
        final LinkedHashSet<String> nodeIds = new LinkedHashSet<>();
        final List<FlowchartEdge> edges = new ArrayList<>();
        final List<FlowchartSubgraph> children = new ArrayList<>();
        @Nullable FlowchartDirection direction;

        SubgraphContext(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }
}
