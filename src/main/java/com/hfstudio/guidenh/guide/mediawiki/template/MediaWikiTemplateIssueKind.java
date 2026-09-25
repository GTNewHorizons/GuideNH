package com.hfstudio.guidenh.guide.mediawiki.template;

public enum MediaWikiTemplateIssueKind {
    UNKNOWN_TEMPLATE,
    MALFORMED_INVOCATION,
    UNBALANCED_BRACES,
    RECURSION_LIMIT,
    EXPANSION_LIMIT,
    UNKNOWN_FUNCTION,
    INVALID_FUNCTION_ARGS,
    MISSING_PARAMETER
}
