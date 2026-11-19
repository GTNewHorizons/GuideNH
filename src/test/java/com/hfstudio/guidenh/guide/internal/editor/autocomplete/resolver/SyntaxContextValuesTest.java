package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class SyntaxContextValuesTest {

    @Test
    public void trailingCommentsCannotOverrideTheBindingType() {
        String source = "---\nbindings:\n  - {type: \"thaumcraft\", id: \"INFUSION\"} # , type: \"wrong\"\n---\n";
        int cursor = source.indexOf("INFUSION") + 3;

        assertEquals(
            "thaumcraft",
            SyntaxContextValues.bindingEntry(source, cursor)
                .get("type"));
    }

    @Test
    public void commentsCannotOfferBindingValueCompletion() {
        String source = "---\nbindings:\n  - {type: \"thaumcraft\", id: \"INFUSION\"} # , id: \"comment\"\n---\n";

        assertNull(SyntaxContextValues.bindingField(source, source.indexOf("comment") + 3));
    }

    @Test
    public void anUnfinishedQuotedValueRetainsAnEmptyInsertionRange() {
        String source = "---\nbindings:\n  - {type: \"thaumcraft\", id: \"\n---\n";
        int cursor = source.indexOf("id: ") + 5;
        var field = SyntaxContextValues.bindingField(source, cursor);

        assertNotNull(field);
        assertEquals("id", field.name());
        assertEquals("", field.prefix());
        assertEquals(cursor, field.start());
        assertEquals(cursor, field.end());
        assertEquals(
            "thaumcraft",
            field.contextValues()
                .get("type"));
    }

    @Test
    public void quotedBlockValuesCannotIntroduceFlowFields() {
        String source = "---\nbindings:\n  - type: thaumcraft\n    id: 'chapter, type: \"wrong\"'\n---\n";
        int cursor = source.indexOf("wrong") + 2;

        assertEquals(
            "thaumcraft",
            SyntaxContextValues.bindingEntry(source, cursor)
                .get("type"));
        assertNull(SyntaxContextValues.bindingField(source, cursor));
    }
}
