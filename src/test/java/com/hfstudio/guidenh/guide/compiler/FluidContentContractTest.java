package com.hfstudio.guidenh.guide.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.hfstudio.guidenh.bridge.preview.PreviewQueryFactory;
import com.hfstudio.guidenh.bridge.protocol.BridgeProtocolLimits;
import com.hfstudio.guidenh.guide.GuidePageChange;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.tags.ItemGridCompiler.FluidEntry;
import com.hfstudio.guidenh.guide.compiler.tags.ItemGridCompiler.ItemEntry;
import com.hfstudio.guidenh.guide.compiler.tags.ItemGridCompiler.ItemGridPlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.indices.FluidIndex;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.guide.internal.recipe.FluidFilterExpression;
import com.hfstudio.guidenh.guide.internal.recipe.FluidRecipeSlots;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateTestHarness;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteExportedItem;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteItemHtml;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteLabelFormatter;
import com.hfstudio.guidenh.integration.api.RecipeSlot;

public class FluidContentContractTest {

    @Test
    public void exportedItemQuantitiesUseTextOverlays() {
        var html = new StringBuilder();
        GuideSiteItemHtml
            .appendIcon(html, new GuideSiteExportedItem("minecraft:stone", "Stone", "stone.png", 64, null), null);
        assertTrue(
            html.toString()
                .contains("class=\"item-amount\">64</span>"));
        assertTrue(
            html.toString()
                .contains("src=\"stone.png\""));
        html.setLength(0);
        GuideSiteItemHtml.appendIcon(html, new GuideSiteExportedItem("minecraft:stone", "Stone", "stone.png"), null);
        assertFalse(
            html.toString()
                .contains("item-amount"));
    }

    @Test
    public void labelTemplatesAcceptLiteralPercentsAndEscapeNames() {
        assertEquals(
            "<strong>100% water / water</strong>",
            GuideSiteLabelFormatter.render("water", "**100% %s / %s**"));
        assertEquals("&lt;tag&gt;&amp; / &lt;tag&gt;&amp;", GuideSiteLabelFormatter.render("<tag>&", "%s / %s"));
    }

    @Test
    public void fluidRecipeMatchesAuxiliaryOutputsAndRejectsMalformedFilters() {
        var input = new RecipeSlot(0, 0, List.of(), null, List.of(new FluidStackSpec("water")), null);
        var output = new RecipeSlot(18, 0, List.of(), null, List.of(new FluidStackSpec("steam")), null);
        var primary = new RecipeSlot(36, 0, List.of());
        assertTrue(
            FluidRecipeSlots.matches(
                List.of(input),
                List.of(output),
                primary,
                "steam",
                false,
                false,
                FluidFilterExpression.parse("water&!lava"),
                FluidFilterExpression.parse("steam")));
        assertFalse(
            FluidRecipeSlots.matches(
                List.of(input),
                List.of(output),
                primary,
                "Steam",
                false,
                false,
                FluidFilterExpression.EMPTY,
                FluidFilterExpression.EMPTY));
        for (String malformed : List.of("water&", ",steam", "!", "water,!steam&")) {
            assertThrows(IllegalArgumentException.class, () -> FluidFilterExpression.parse(malformed));
        }
    }

    @Test
    public void duplicateBindingsPreserveDistinctAnchors() {
        var page = page("fluid_id: water\nfluid_ids:\n  - ' water# '\n  - water#uses\n  - water#uses\n");
        var index = new FluidIndex();
        index.rebuild(List.of(page));
        assertEquals(List.of(PageAnchor.page(page.getId()), new PageAnchor(page.getId(), "uses")), index.get("water"));
    }

    @Test
    public void updatingBindingsRemovesOldFluidDestinations() {
        var before = page("fluid_id: water\n");
        var after = page("fluid_id: lava#uses\n");
        var index = new FluidIndex();
        index.rebuild(List.of(before));
        index.update(List.of(after), List.of(new GuidePageChange(before.getId(), before, after)));
        assertEquals(List.of(), index.get("water"));
        assertEquals(List.of(new PageAnchor(after.getId(), "uses")), index.get("lava"));
    }

    @Test
    public void mixedGridPreservesSourceOrderWithoutRegistryResolution() {
        var harness = new MediaWikiTemplateTestHarness();
        var compiled = harness.compile("""
            <ItemGrid>
              <ItemIcon id="minecraft:stone" />
              <FluidIcon id="unregistered.fluid" amount={0} showTooltip={false} />
              <ItemIcon id="minecraft:dirt" />
            </ItemGrid>
            """);
        var placeholders = new ArrayList<ItemGridPlaceholder>();
        compiled.document()
            .visit(new LytVisitor() {

                @Override
                public Result beforeNode(LytNode node) {
                    if (node instanceof ItemGridPlaceholder grid) placeholders.add(grid);
                    return Result.CONTINUE;
                }
            });
        assertEquals(1, placeholders.size());
        var entries = placeholders.getFirst()
            .getEntries();
        assertEquals(3, entries.size());
        assertEquals("minecraft:stone", assertInstanceOf(ItemEntry.class, entries.get(0)).id());
        var fluid = assertInstanceOf(FluidEntry.class, entries.get(1));
        assertEquals(
            "unregistered.fluid",
            fluid.fluid()
                .id());
        assertEquals(
            0,
            fluid.fluid()
                .amount());
        assertFalse(fluid.showTooltip());
        assertEquals("minecraft:dirt", assertInstanceOf(ItemEntry.class, entries.get(2)).id());
    }

    private static ParsedGuidePage page(String yaml) {
        ResourceLocation id = new ResourceLocation("guidenh:fluid-contract.md");
        return new ParsedGuidePage("guidenh", id, "", null, Frontmatter.parse(id, yaml), "en_us", null, null, null);
    }

    @Test
    public void fluidPreviewRejectsTruncatedQuantities() {
        var factory = new PreviewQueryFactory(new BridgeProtocolLimits(131072, 80, 4, 4, 100));
        for (String amount : List.of("-1", "0.5", "2147483648", "null", "true")) {
            var payload = new JsonParser()
                .parse("{\"capability\":\"fluids\",\"id\":\"water\",\"amount\":" + amount + "}")
                .getAsJsonObject();
            assertThrows(IllegalArgumentException.class, () -> factory.createResolveQuery(payload));
        }
        var payload = new JsonParser().parse("{\"capability\":\"fluids\",\"id\":\"water\",\"amount\":0}")
            .getAsJsonObject();
        var query = factory.createResolveQuery(payload);
        assertEquals(0, query.getAmount());
        assertEquals(1, query.getCount());
    }
}
