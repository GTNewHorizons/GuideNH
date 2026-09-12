package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.FontRenderer;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.AutocompleteCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.CompositeResolver;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MarkdownSyntaxResolver;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxSyntaxResolver;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.SelectionStrategies;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.WordBoundaryResolver;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.ui.AutocompletePopup;
import com.hfstudio.guidenh.guide.syntax.GuideSyntaxModel;
import com.hfstudio.guidenh.guide.syntax.SyntaxEnvironment;
import com.hfstudio.guidenh.guide.syntax.SyntaxSelection;

/**
 * Owns the guide editor's syntax completion session: it resolves what the cursor is inside, asks the
 * guide's {@link GuideSyntaxModel} for candidates, and drives the completion popup.
 */
public class GuideEditorAutocompleteController {

    /** How the host must react to a key that arrived while completion may be active. */
    public enum KeyResult {
        /**
         * The popup did not take the key, so the host keeps its normal handling for it. The
         * controller may have dismissed the popup first.
         */
        IGNORED,
        /** The key only changed popup state. */
        CONSUMED,
        /** A candidate was accepted into {@link #takePendingCommit()}. */
        COMMIT,
        /** The popup closed and the key still has to reach the text area. */
        FORWARD_TO_EDITOR
    }

    public record SelectionRange(int start, int end) {}

    private static final long QUERY_DEBOUNCE_MILLIS = 100L;
    private static final int QUERY_LIMIT = 20;
    private static final int MOUSE_BUTTON_PRIMARY = 0;

    private final AutocompletePopup popup = new AutocompletePopup();
    private final MdxSyntaxResolver mdxResolver = new MdxSyntaxResolver();
    private final MarkdownSyntaxResolver markdownResolver = new MarkdownSyntaxResolver();
    private final SyntaxContextResolver resolver;
    private final Map<SyntaxElementType, SelectionStrategy> selectionStrategies;

    private GuideSyntaxModel model = GuideSyntaxModel.empty();
    @Nullable
    private AutocompleteContext pendingContext;
    @Nullable
    private AutocompleteCommit pendingCommit;
    @Nullable
    private String lastText;
    private int lastCursor = -1;
    private long nextQueryAtMillis;
    private boolean queryRequestedByEdit;
    private int anchorX;
    private int anchorY;
    private int viewportWidth;
    private int viewportHeight;

    public GuideEditorAutocompleteController() {
        this.resolver = new CompositeResolver(mdxResolver, markdownResolver, new WordBoundaryResolver());
        this.selectionStrategies = SelectionStrategies.defaults();
    }

    /** Points the session at the syntax of the guide currently open in the editor. */
    public void setModel(@Nullable GuideSyntaxModel model) {
        this.model = model != null ? model : GuideSyntaxModel.empty();
        this.markdownResolver.setModel(this.model);
        close();
    }

    /** Records that the text area changed, arming a debounced query for the next tick. */
    public void markEdit() {
        queryRequestedByEdit = true;
        nextQueryAtMillis = System.currentTimeMillis() + QUERY_DEBOUNCE_MILLIS;
    }

    /**
     * Resolves the syntax under the caret and refreshes the popup. Queries only run after an edit so
     * merely moving the caret never reopens the popup.
     */
    public void update(@Nullable String text, int cursorIndex, int anchorX, int anchorY, int viewportWidth,
        int viewportHeight, FontRenderer fontRenderer, SyntaxEnvironment environment) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        if (text == null) {
            close();
            return;
        }

        boolean firstRun = lastText == null;
        boolean textChanged = firstRun || !text.equals(lastText);
        boolean cursorMoved = firstRun || cursorIndex != lastCursor;
        if (!textChanged && !cursorMoved) {
            return;
        }

        if (!textChanged && cursorMoved) {
            lastCursor = cursorIndex;
            close();
            return;
        }

        if (!queryRequestedByEdit) {
            lastText = text;
            lastCursor = cursorIndex;
            close();
            return;
        }

        if (textChanged && !firstRun && System.currentTimeMillis() < nextQueryAtMillis) {
            // The text moved while the query is debounced, so the open popup describes older text.
            // Dismiss it without disarming the pending query: the next update fills it for the new text.
            dismissPopup();
            return;
        }

        lastText = text;
        lastCursor = cursorIndex;

        model.prepare(environment);
        List<AutocompleteCandidate> candidates;
        AutocompleteContext context;
        GuideSyntaxModel.SlotMatch slotMatch = model.matchSlot(text, cursorIndex);
        if (slotMatch != null) {
            // A slot of another mod claims the caret, so the editor's own resolvers stay out of it even
            // when the slot has no values to offer right now.
            candidates = GuideSyntaxCompletion.slotQuery(slotMatch, QUERY_LIMIT);
            context = new SlotContext(slotMatch);
        } else {
            TextSyntaxContext syntax = resolver.resolve(text, cursorIndex);
            if (syntax == null || !syntax.shouldAutocomplete()) {
                close();
                return;
            }
            candidates = GuideSyntaxCompletion.query(model, syntax, QUERY_LIMIT);
            context = syntax.getAutocomplete();
        }
        // The query has been answered, so the next edit arms a new one. Disarming before the answer would
        // leave the popup shut until the next keystroke if anything above failed.
        queryRequestedByEdit = false;
        if (candidates.isEmpty() || context == null) {
            close();
            return;
        }
        pendingContext = context;
        popup.show(candidates, anchorX, anchorY, viewportWidth, viewportHeight, fontRenderer);
    }

    public boolean isOpen() {
        return popup.isOpen();
    }

    public void close() {
        dismissPopup();
        queryRequestedByEdit = false;
    }

    /** Drops the popup and its resolved slot, leaving any armed query intact. */
    private void dismissPopup() {
        pendingContext = null;
        pendingCommit = null;
        if (popup.isOpen()) {
            popup.close();
        }
    }

    /** Draws the popup at the anchor recorded by the last {@link #update}. */
    public void draw(int mouseX, int mouseY, FontRenderer fontRenderer) {
        if (!popup.isOpen()) {
            return;
        }
        popup.reposition(anchorX, anchorY, viewportWidth, viewportHeight, fontRenderer);
        popup.draw(fontRenderer, mouseX, mouseY);
    }

    public KeyResult handleKey(String text, char typedChar, int keyCode) {
        if (!popup.isOpen()) {
            return KeyResult.IGNORED;
        }
        switch (keyCode) {
            case Keyboard.KEY_ESCAPE:
                close();
                return KeyResult.CONSUMED;
            case Keyboard.KEY_UP:
                popup.moveSelection(-1);
                return KeyResult.CONSUMED;
            case Keyboard.KEY_DOWN:
                popup.moveSelection(1);
                return KeyResult.CONSUMED;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
            case Keyboard.KEY_TAB:
                acceptSelected(text);
                return pendingCommit != null ? KeyResult.COMMIT : KeyResult.CONSUMED;
            default:
                if (pendingContext != null && pendingContext.isCommitCharacter(typedChar)) {
                    acceptSelected(text);
                    if (pendingCommit != null) {
                        return KeyResult.COMMIT;
                    }
                }
                if (AutocompleteKeyPolicy.isControlChord(typedChar, keyCode)) {
                    // Control chords belong to the host - save, undo, copy, paste - so the popup steps
                    // aside instead of swallowing the key.
                    close();
                    return KeyResult.IGNORED;
                }
                if (!AutocompleteKeyPolicy.shouldCloseForKey(typedChar, keyCode)) {
                    return KeyResult.CONSUMED;
                }
                close();
                return KeyResult.FORWARD_TO_EDITOR;
        }
    }

    /** @return true when the click belonged to the popup. */
    public boolean handleMouseClick(String text, int mouseX, int mouseY, int button) {
        if (!popup.isOpen()) {
            return false;
        }
        if (!popup.contains(mouseX, mouseY)) {
            // Any click elsewhere dismisses the popup before the screen handles it.
            close();
            return false;
        }
        if (button != MOUSE_BUTTON_PRIMARY) {
            // Keep a secondary click from accepting a candidate by accident.
            return true;
        }
        popup.mouseClicked(mouseX, mouseY);
        acceptSelected(text);
        return true;
    }

    /** @return true when the wheel moved the popup list. */
    public boolean handleWheel(int mouseX, int mouseY, int wheelDelta) {
        if (!popup.isOpen() || !popup.contains(mouseX, mouseY)) {
            return false;
        }
        popup.scrollWheel(wheelDelta);
        return true;
    }

    /**
     * Consumes the accepted candidate. The caller applies it and then calls {@link #close()} so the
     * freshly inserted text is not queried again.
     */
    @Nullable
    public AutocompleteCommit takePendingCommit() {
        AutocompleteCommit commit = pendingCommit;
        pendingCommit = null;
        return commit;
    }

    /** Resolves the syntax element under a double click so the host can extend the selection. */
    @Nullable
    public SelectionRange resolveDoubleClickSelection(String text, int cursorIndex) {
        // A slot of another mod knows its own boundaries, so it answers before the editor's strategies.
        SyntaxSelection slotSelection = model.matchSlotSelection(text, cursorIndex);
        if (slotSelection != null) {
            return new SelectionRange(slotSelection.start(), slotSelection.end());
        }
        TextSyntaxContext syntax = resolver.resolve(text, cursorIndex);
        if (syntax == null) {
            return null;
        }
        SelectionStrategy strategy = selectionStrategies.get(syntax.getElementType());
        if (strategy == null) {
            return null;
        }
        int start = strategy.getSelectionStart(syntax, text, cursorIndex);
        int end = strategy.getSelectionEnd(syntax, text, cursorIndex);
        return start != end ? new SelectionRange(start, end) : null;
    }

    private void acceptSelected(String sourceText) {
        AutocompleteCandidate selected = popup.getSelected();
        if (selected == null || pendingContext == null) {
            close();
            return;
        }
        if (!stillDescribes(sourceText, pendingContext)) {
            // An out-of-band edit moved the text, so the recorded range no longer describes the slot.
            // Commit nothing rather than overwrite whatever now sits there.
            close();
            return;
        }
        AutocompleteCommit commit = AutocompleteCommitService.commit(sourceText, pendingContext, selected);
        if (commit == null) {
            // The candidate could not produce an edit - a slot writer failed, for instance - so nothing is
            // written and the popup closes instead of leaving a session whose slot no longer holds.
            close();
            return;
        }
        pendingCommit = commit;
    }

    /** True when {@code text} still starts the slot's typed text at the slot's recorded position. */
    private static boolean stillDescribes(String text, AutocompleteContext context) {
        int start = context.replaceStart();
        int end = context.replaceEnd();
        String partial = context.getPartialText();
        if (start < 0 || start > end || end > text.length() || partial == null) {
            return false;
        }
        return text.startsWith(partial, start);
    }
}
