package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.TagCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.TagStartContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.ui.AutocompletePopup;

public class AutocompleteScrollbarTest {

    @Test
    public void clickingScrollbarDoesNotCommitSelectedCandidate() throws ReflectiveOperationException {
        var controller = controllerWithPopup();
        assertTrue(controller.handleMouseClick("<Item", 128, 25, 0));
        assertTrue(controller.isOpen());
        assertNull(controller.takePendingCommit());
    }

    @Test
    public void clickingCandidateStillCommits() throws ReflectiveOperationException {
        var controller = controllerWithPopup();
        assertTrue(controller.handleMouseClick("<Item", 20, 30, 0));
        assertNotNull(controller.takePendingCommit());
    }

    private static GuideEditorAutocompleteController controllerWithPopup() throws ReflectiveOperationException {
        var controller = new GuideEditorAutocompleteController();
        Field popupField = GuideEditorAutocompleteController.class.getDeclaredField("popup");
        popupField.setAccessible(true);
        var popup = (AutocompletePopup) popupField.get(controller);
        setField(popup, "open", true);
        setField(popup, "x", 10);
        setField(popup, "y", 20);
        setField(popup, "width", 120);
        setField(popup, "height", 78);
        setField(
            popup,
            "candidates",
            List.of("ItemImage", "ItemLink", "ItemGrid", "FluidImage", "FluidLink", "FluidIcon", "Recipe", "Structure")
                .stream()
                .map(name -> new TagCandidate(name, false))
                .toList());
        setField(controller, "pendingContext", new TagStartContext(1, 5, "Item"));
        return controller;
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
