package com.hfstudio.guidenh.guide.internal.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.Test;

/**
 * The template section the guide editor appends below the navigation tree. It is driven by the caller rather
 * than by the tree, so it is projected on its own.
 */
class GuideNavProjectionTemplatesTest {

    private final GuideNavProjection projection = new GuideNavProjection();

    private static GuideNavProjection.DisplayRow template(String name) {
        return new GuideNavProjection.DisplayRow(
            GuideNavProjection.RowKind.TEMPLATE_PAGE,
            1,
            name,
            null,
            null,
            new ResourceLocation("guidenh", "guidenh/templates/" + name + ".md"),
            false,
            true);
    }

    @Test
    void appendsAGroupHoldingEveryTemplatePage() {
        var base = new GuideNavProjection.ProjectionResult(List.of());
        var result = projection.withTemplates(base, List.of(template("InfoBox"), template("Row")), true);

        assertEquals(
            3,
            result.rows()
                .size(),
            "one group row plus two templates");
        var group = result.rows()
            .get(0)
            .displayRow();
        assertEquals(GuideNavProjection.RowKind.TEMPLATE_GROUP, group.kind());
        assertEquals(0, group.depth());
        assertTrue(group.hasChildren());
        assertEquals(
            3,
            result.rows()
                .get(0)
                .subtreeEndRowIndexExclusive(),
            "the group must span its whole subtree");

        for (int index = 1; index < result.rows()
            .size(); index++) {
            var row = result.rows()
                .get(index);
            assertEquals(
                GuideNavProjection.RowKind.TEMPLATE_PAGE,
                row.displayRow()
                    .kind());
            assertEquals(
                1,
                row.displayRow()
                    .depth(),
                "a template is nested under the group");
            assertEquals(0, row.parentRowIndex(), "a template's parent is the group row");
            assertTrue(
                row.displayRow()
                    .hasPage(),
                "a template row must open its page");
        }
    }

    @Test
    void aCollapsedGroupKeepsOnlyItsOwnRow() {
        var base = new GuideNavProjection.ProjectionResult(List.of());
        var result = projection.withTemplates(base, List.of(template("InfoBox")), false);

        assertEquals(
            1,
            result.rows()
                .size(),
            "a collapsed group hides its templates");
        assertEquals(
            GuideNavProjection.RowKind.TEMPLATE_GROUP,
            result.rows()
                .get(0)
                .displayRow()
                .kind());
    }

    @Test
    void noTemplatesMeansNoSection() {
        var base = new GuideNavProjection.ProjectionResult(List.of());
        var result = projection.withTemplates(base, List.of(), true);

        assertTrue(
            result.rows()
                .isEmpty(),
            "an empty template list must not add an empty section");
    }
}
