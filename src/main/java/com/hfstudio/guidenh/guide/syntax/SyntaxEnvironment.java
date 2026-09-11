package com.hfstudio.guidenh.guide.syntax;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;

/**
 * The live data a {@link SyntaxValueSource} may consult while answering a request.
 *
 * <p>
 * Values are pulled on demand, so a source that needs nothing costs nothing. Implementations are
 * created by the editor once per completion tick.
 */
public interface SyntaxEnvironment {

    /** The guide currently open in the editor, or null. */
    @Nullable
    Guide guide();

    /** Page ids of the loaded guide, built once per guide change. */
    List<String> pagePaths();

    /** The text currently in the editor. */
    String documentText();
}
