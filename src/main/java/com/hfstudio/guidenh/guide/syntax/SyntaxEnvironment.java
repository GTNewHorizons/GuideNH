package com.hfstudio.guidenh.guide.syntax;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;

public interface SyntaxEnvironment {

    @Nullable
    Guide guide();

    List<String> pagePaths();

    String documentText();
}
