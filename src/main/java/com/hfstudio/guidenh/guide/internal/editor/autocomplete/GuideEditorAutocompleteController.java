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

/** Owns the guide editor's syntax completion session: it resolves what the cursor is inside, asks the guide's. */
public class GuideEditorAutocompleteController {

    public enum KeyResult {
        IGNORED,
        CONSUMED,
        COMMIT,
        FORWARD_TO_EDITOR
    }

    public record SelectionRange(int start, int end) {}

    /**
     * How long an edit waits before a query runs. A query parses the whole page, so it cannot run on every
     * tick of a fast typist, but the wait is also what the popup is hidden for - a long one reads as lag.
     * Two ticks is enough to coalesce a burst of keystrokes without being noticeable.
     */
    private static final long QUERY_DEBOUNCE_MILLIS = 40L;
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

    public void setModel(@Nullable GuideSyntaxModel model) {
        this.model = model != null ? model : GuideSyntaxModel.empty();
        this.markdownResolver.setModel(this.model);
        close();
    }

    public void markEdit() {
        queryRequestedByEdit = true;
        nextQueryAtMillis = System.currentTimeMillis() + QUERY_DEBOUNCE_MILLIS;
    }

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
            // The text moved while the query is debounced. The open popup describes older text, so it is
            // hidden rather than left showing candidates that no longer match, and the pending query is not
            // disarmed: the next update fills it for the new text. The selection survives, because hiding it
            // here would move the highlight back to the first entry on every keystroke.
            pendingContext = null;
            pendingCommit = null;
            popup.hideForPendingQuery();
            return;
        }

        lastText = text;
        lastCursor = cursorIndex;

        model.prepare(environment);
        List<AutocompleteCandidate> candidates;
        AutocompleteContext context;
        GuideSyntaxModel.SlotMatch slotMatch = model.matchSlot(text, cursorIndex);
        if (slotMatch != null) {
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

    private void dismissPopup() {
        pendingContext = null;
        pendingCommit = null;
        if (popup.isOpen()) {
            popup.close();
        }
    }

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

    public boolean handleMouseClick(String text, int mouseX, int mouseY, int button) {
        if (!popup.isOpen()) {
            return false;
        }
        if (!popup.contains(mouseX, mouseY)) {
            close();
            return false;
        }
        if (button != MOUSE_BUTTON_PRIMARY) {
            return true;
        }
        popup.mouseClicked(mouseX, mouseY);
        acceptSelected(text);
        return true;
    }

    public boolean handleWheel(int mouseX, int mouseY, int wheelDelta) {
        if (!popup.isOpen() || !popup.contains(mouseX, mouseY)) {
            return false;
        }
        popup.scrollWheel(wheelDelta);
        return true;
    }

    @Nullable
    public AutocompleteCommit takePendingCommit() {
        AutocompleteCommit commit = pendingCommit;
        pendingCommit = null;
        return commit;
    }

    @Nullable
    public SelectionRange resolveDoubleClickSelection(String text, int cursorIndex) {
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
            close();
            return;
        }
        AutocompleteCommit commit = AutocompleteCommitService.commit(sourceText, pendingContext, selected);
        if (commit == null) {
            close();
            return;
        }
        pendingCommit = commit;
    }

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
