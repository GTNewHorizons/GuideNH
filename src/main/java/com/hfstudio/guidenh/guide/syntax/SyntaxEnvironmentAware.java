package com.hfstudio.guidenh.guide.syntax;

/**
 * Implemented by a {@link SyntaxValueSource} that needs live editor data, such as the page list or the
 * current document text. {@link GuideSyntaxModel#prepare(SyntaxEnvironment)} is called once per
 * completion tick before any request is answered.
 */
public interface SyntaxEnvironmentAware {

    void prepare(SyntaxEnvironment environment);
}
