package com.hfstudio.guidenh.guide.syntax;

/**
 * Implemented by a {@link SyntaxValueSource} that needs live editor data, such as the page list or the current.
 */
public interface SyntaxEnvironmentAware {

    void prepare(SyntaxEnvironment environment);
}
