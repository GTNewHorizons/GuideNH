package com.hfstudio.guidenh.guide.mediawiki.template;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/**
 * Evaluates the branch tags at compile time. The chosen branch's nodes are returned so the caller can put
 * them where the conditional stood; the branch not taken is dropped and never compiled.
 *
 * <p>
 * A conditional's children up to an {@code <Else>} are its first branch and the children after it are the
 * fallback. An {@code <Else>} may instead carry its fallback inline, as {@code <Else>text</Else>}.
 */
public class TemplateConditionals {

    private TemplateConditionals() {}

    public static List<MdAstAnyContent> evaluate(MdxJsxElementFields element, MediaWikiTemplateArguments arguments,
        MediaWikiTemplateContext context, PageCompiler compiler) {
        String tag = element.name();
        boolean takeThen;
        if (TemplateTags.IF.equals(tag)) {
            String value = TemplateValues.readParameterName(element, TemplateTags.TEST_ATTRIBUTE, arguments, context);
            takeThen = value != null && !value.trim()
                .isEmpty();
        } else if (TemplateTags.IF_EQ.equals(tag)) {
            takeThen = TemplateValues.equalTo(
                TemplateValues.readComparable(element, TemplateTags.A_ATTRIBUTE, arguments, context),
                TemplateValues.readComparable(element, TemplateTags.B_ATTRIBUTE, arguments, context));
        } else if (TemplateTags.IF_EXIST.equals(tag)) {
            String page = TemplateValues.read(element, TemplateTags.PAGE_ATTRIBUTE, arguments, context);
            takeThen = page != null && MediaWikiPageExistence.pageExists(
                page.trim(),
                context,
                compiler == null ? null
                    : pageId -> compiler.getPageCollection()
                        .pageExists(pageId));
        } else if (TemplateTags.SWITCH.equals(tag)) {
            return selectSwitch(element, arguments, context);
        } else {
            return List.of();
        }
        return takeThen ? branch(element, true) : branch(element, false);
    }

    private static List<MdAstAnyContent> branch(MdxJsxElementFields element, boolean then) {
        List<MdAstAnyContent> selected = new ArrayList<>();
        boolean passedElse = false;
        for (MdAstAnyContent child : significantChildren(element)) {
            boolean isElse = child instanceof MdxJsxElementFields nested && TemplateTags.ELSE.equals(nested.name());
            if (isElse) {
                if (then) {
                    break;
                }
                passedElse = true;
                List<MdAstAnyContent> inline = MediaWikiTemplateValue
                    .copyNodes(((MdxJsxElementFields) child).children());
                if (!inline.isEmpty()) {
                    return inline;
                }
                continue;
            }
            if (then || passedElse) {
                selected.add(MediaWikiTemplateAst.copy(child));
            }
        }
        return selected;
    }

    private static List<MdAstAnyContent> significantChildren(MdxJsxElementFields element) {
        List<MdAstAnyContent> flattened = new ArrayList<>();
        for (MdAstAnyContent child : element.children()) {
            if (child instanceof MdxJsxElementFields nested && "p".equals(nested.name())) {
                flattened.addAll(nested.children());
            } else {
                flattened.add(child);
            }
        }
        return flattened;
    }

    private static List<MdAstAnyContent> selectSwitch(MdxJsxElementFields element, MediaWikiTemplateArguments arguments,
        MediaWikiTemplateContext context) {
        String subject = TemplateValues.readComparable(element, TemplateTags.TEST_ATTRIBUTE, arguments, context);
        List<MdAstAnyContent> fallback = List.of();
        for (MdAstAnyContent child : significantChildren(element)) {
            if (!(child instanceof MdxJsxElementFields caseElement)) {
                continue;
            }
            String tag = caseElement.name();
            if (TemplateTags.DEFAULT.equals(tag)) {
                fallback = MediaWikiTemplateValue.copyNodes(caseElement.children());
                continue;
            }
            if (!TemplateTags.CASE.equals(tag)) {
                continue;
            }
            String candidate = TemplateValues
                .readComparable(caseElement, TemplateTags.VALUE_ATTRIBUTE, arguments, context);
            if (TemplateValues.equalTo(candidate, subject)) {
                return MediaWikiTemplateValue.copyNodes(caseElement.children());
            }
        }
        return fallback;
    }
}
