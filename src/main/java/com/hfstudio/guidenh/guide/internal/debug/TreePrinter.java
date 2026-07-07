package com.hfstudio.guidenh.guide.internal.debug;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;

/**
 * Utility for printing document and element trees to console for debugging.
 * Optimized for performance and minimal memory allocation.
 */
public class TreePrinter {

    private static final Logger LOGGER = LogManager.getLogger("GuideNH-Debug");

    private static final String BRANCH_CONTINUE = "├── ";
    private static final String BRANCH_END = "└── ";
    private static final String BRANCH_VERTICAL = "│   ";
    private static final String BRANCH_SPACE = "    ";

    /**
     * Print document tree to log output.
     * More efficient than System.out.println as it uses logger infrastructure.
     */
    public static void printDocumentTree(LytDocument document) {
        if (document == null) {
            LOGGER.info("Document Tree: null");
            return;
        }

        StringBuilder builder = new StringBuilder(2048);
        builder.append("Document Tree:\n");
        builder.append("Document: ")
            .append(
                document.getClass()
                    .getSimpleName())
            .append('\n');

        var children = document.getChildren();
        for (int i = 0; i < children.size(); i++) {
            boolean isLast = i == children.size() - 1;
            printNode(children.get(i), builder, "", isLast);
        }

        LOGGER.info("\n{}", builder);
    }

    /**
     * Convert document tree to string without logging.
     * Useful for programmatic access or custom output.
     */
    public static String toString(LytDocument document) {
        if (document == null) {
            return "Document Tree: null";
        }

        StringBuilder builder = new StringBuilder(2048);
        builder.append("Document: ")
            .append(
                document.getClass()
                    .getSimpleName())
            .append('\n');

        var children = document.getChildren();
        for (int i = 0; i < children.size(); i++) {
            boolean isLast = i == children.size() - 1;
            printNode(children.get(i), builder, "", isLast);
        }

        return builder.toString();
    }

    private static void printNode(LytNode node, StringBuilder builder, String indent, boolean isLast) {
        builder.append(indent);
        builder.append(isLast ? BRANCH_END : BRANCH_CONTINUE);

        String nodeInfo = getNodeInfo(node);
        builder.append(nodeInfo);
        builder.append('\n');

        var children = node.getChildren();
        if (children.isEmpty()) {
            return;
        }

        String childIndent = indent + (isLast ? BRANCH_SPACE : BRANCH_VERTICAL);
        for (int i = 0; i < children.size(); i++) {
            boolean isChildLast = i == children.size() - 1;
            printNode(children.get(i), builder, childIndent, isChildLast);
        }
    }

    private static String getNodeInfo(LytNode node) {
        StringBuilder sb = new StringBuilder(128);
        String className = node.getClass()
            .getSimpleName();
        sb.append(className);

        if (node.getId() != null) {
            sb.append(" [id=")
                .append(node.getId())
                .append(']');
        }

        if (node.getNodeUid() != null) {
            sb.append(" [uid=")
                .append(node.getNodeUid())
                .append(']');
        }

        if (node.getStyleClass() != null) {
            sb.append(" [class=")
                .append(node.getStyleClass())
                .append(']');
        }

        var bounds = node.getBounds();
        if (bounds != null) {
            sb.append(" {x=")
                .append(bounds.x())
                .append(", y=")
                .append(bounds.y())
                .append(", w=")
                .append(bounds.width())
                .append(", h=")
                .append(bounds.height())
                .append('}');
        }

        int childCount = node.getChildren()
            .size();
        if (childCount > 0) {
            sb.append(" (")
                .append(childCount)
                .append(" children)");
        }

        return sb.toString();
    }
}
