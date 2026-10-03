package com.hfstudio.guidenh.guide.internal.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.internal.markdown.MarkdownLatexShorthand.MaskResult;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownLatexShorthand.Segment;

class MarkdownLatexShorthandTest {

    /** Placeholder prefix used for formula entries (both $$ and single $). */
    private static final String FORMULA_PREFIX = "\uE000GUIDENH_LATEX_";

    /** Placeholder prefix used for \$ escape entries. */
    private static final String ESCAPE_PREFIX = "\uE000GUIDENH_LATEXESC_";

    /** Extract non-escape formula values from a MaskResult. */
    private static Set<String> maskFormulas(MaskResult mr) {
        return mr.formulas()
            .entrySet()
            .stream()
            .filter(
                e -> e.getKey()
                    .startsWith(FORMULA_PREFIX))
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet());
    }

    /** Extract formula values from a split result. */
    private static Set<String> splitFormulas(String text) {
        return MarkdownLatexShorthand.split(text)
            .stream()
            .filter(Segment::isFormula)
            .map(Segment::getValue)
            .collect(Collectors.toSet());
    }

    /** Simulate restore by replacing placeholders with their values, then split. */
    private static List<Segment> maskRestoreSplit(String input) {
        MaskResult mr = MarkdownLatexShorthand.mask(input);
        String restored = mr.source();
        for (Map.Entry<String, String> e : mr.formulas()
            .entrySet()) {
            restored = restored.replace(e.getKey(), e.getValue());
        }
        return MarkdownLatexShorthand.split(restored);
    }

    /** Verify mask formulas (non-escape) == split formulas for the same input. */
    private static void assertMaskSplitConsistent(String input) {
        MaskResult mr = MarkdownLatexShorthand.mask(input);
        Set<String> mf = maskFormulas(mr);
        Set<String> sf = splitFormulas(input);
        assertEquals(mf, sf, "mask/split formula mismatch for: " + input);
    }

    /** Assert that split produces exactly the given formula values in order. */
    private static void assertSplitFormulas(String input, String... expectedFormulas) {
        List<Segment> segs = MarkdownLatexShorthand.split(input);
        List<String> actual = segs.stream()
            .filter(Segment::isFormula)
            .map(Segment::getValue)
            .toList();
        assertEquals(List.of(expectedFormulas), actual, "split formulas mismatch for: " + input);
    }

    /** Assert that mask/restore/split produces the same formulas as direct split. */
    private static void assertMaskRestoreSplitFormulas(String input, String... expectedFormulas) {
        List<Segment> segs = maskRestoreSplit(input);
        List<String> actual = segs.stream()
            .filter(Segment::isFormula)
            .map(Segment::getValue)
            .toList();
        assertEquals(List.of(expectedFormulas), actual, "mask/restore/split formulas mismatch for: " + input);
    }

    /** a. $E=mc^2$ → single formula segment. */
    @Test
    void testSingleInlineFormula() {
        String input = "$E=mc^2$";
        assertSplitFormulas(input, "E=mc^2");
        assertMaskSplitConsistent(input);
    }

    /** b. Two inline formulas with text between. */
    @Test
    void testTwoInlineFormulas() {
        String input = "$\\frac{1}{2}$ and $\\frac{a+b}{c-d}$";
        assertSplitFormulas(input, "\\frac{1}{2}", "\\frac{a+b}{c-d}");
        assertMaskSplitConsistent(input);
    }

    /** c. Currency $5 should NOT be detected as formula. */
    @Test
    void testCurrencyNotFormula() {
        String input = "Price is $5 and $10 today";
        assertSplitFormulas(input);
        assertMaskSplitConsistent(input);
    }

    /** d. $5-$10 → closing $ followed by digit → no formula. */
    @Test
    void testDollarRangeNotFormula() {
        String input = "$5-$10";
        assertSplitFormulas(input);
        assertMaskSplitConsistent(input);
    }

    /** e. Escaped \$: only $y$ should become a formula. */
    @Test
    void testEscapedDollar() {
        String input = "Use \\$x for $y$";
        // Direct split: $y$ is formula, \$x is just text
        assertSplitFormulas(input, "y");
        assertMaskSplitConsistent(input);

        // After mask/restore, \$x → $x (literal), $y$ → formula y
        assertMaskRestoreSplitFormulas(input, "y");
    }

    /** f. $$...$$ and $...$ mixed: $$ takes priority. */
    @Test
    void testMixedDoubleAndSingleDollar() {
        String input = "$$d$$ and $i$";
        // $$d$$ → formula "d", $i$ → formula "i"
        assertSplitFormulas(input, "d", "i");
        assertMaskSplitConsistent(input);

        // Also verify sequence has correct ordering
        List<Segment> segs = MarkdownLatexShorthand.split(input);
        assertEquals(3, segs.size(), "expected 3 segments: formula + text + formula");
        assertTrue(
            segs.get(0)
                .isFormula());
        assertEquals(
            "d",
            segs.get(0)
                .getValue());
        assertFalse(
            segs.get(1)
                .isFormula());
        assertEquals(
            " and ",
            segs.get(1)
                .getValue());
        assertTrue(
            segs.get(2)
                .isFormula());
        assertEquals(
            "i",
            segs.get(2)
                .getValue());
    }

    /** g. Space-adjacent: $ x$ and $x $ → no formula. */
    @Test
    void testSpaceAdjacentNoFormula() {
        assertSplitFormulas("$ x$");
        assertMaskSplitConsistent("$ x$");

        assertSplitFormulas("$x $");
        assertMaskSplitConsistent("$x $");
    }

    /** h. Multi-line $a\nb$ → no formula (single $ must be single-line). */
    @Test
    void testMultilineNoFormula() {
        String input = "$a\nb$";
        assertSplitFormulas(input);
        assertMaskSplitConsistent(input);
    }

    /** i. extractSoleDisplayFormula and mayContain for single $. */
    @Test
    void testExtractSoleDisplayWithSingleDollar() {
        // Single $ never produces display mode
        assertNull(
            MarkdownLatexShorthand.extractSoleDisplayFormula("$x$"),
            "extractSoleDisplayFormula must return null for single $");
        // mayContain is a superset check: true for any $
        assertTrue(MarkdownLatexShorthand.mayContain("$x$"), "mayContain must return true for text containing $");
        // $$ still works for display mode
        assertEquals("x", MarkdownLatexShorthand.extractSoleDisplayFormula("$$x$$"));
    }

    /** Mask/split consistency for all test inputs a-h. */
    @Test
    void testMaskSplitConsistencyAll() {
        String[] cases = { "$E=mc^2$", "$\\frac{1}{2}$ and $\\frac{a+b}{c-d}$", "Price is $5 and $10 today", "$5-$10",
            "Use \\$x for $y$", "$$d$$ and $i$", "$ x$", "$x $", "$a\nb$", };
        for (String tc : cases) {
            assertMaskSplitConsistent(tc);
        }
    }

}
