package com.hfstudio.guidenh.guide.syntax;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;

/** The live data a {@link SyntaxValueSource} may consult while answering a request. */
public interface SyntaxEnvironment {

    @Nullable
    Guide guide();

    List<String> pagePaths();

    String documentText();
}
