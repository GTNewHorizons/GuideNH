package com.hfstudio.guidenh.libs.mdx;

import java.util.List;

import com.hfstudio.guidenh.libs.micromark.Assert;
import com.hfstudio.guidenh.libs.micromark.CharUtil;
import com.hfstudio.guidenh.libs.micromark.Construct;
import com.hfstudio.guidenh.libs.micromark.ContentType;
import com.hfstudio.guidenh.libs.micromark.ListUtils;
import com.hfstudio.guidenh.libs.micromark.State;
import com.hfstudio.guidenh.libs.micromark.Token;
import com.hfstudio.guidenh.libs.micromark.TokenizeContext;
import com.hfstudio.guidenh.libs.micromark.Tokenizer;
import com.hfstudio.guidenh.libs.micromark.Types;
import com.hfstudio.guidenh.libs.micromark.factory.FactorySpace;
import com.hfstudio.guidenh.libs.micromark.symbol.Codes;

public class JsxFlow {

    public static final Construct INSTANCE = new Construct();

    static {
        INSTANCE.tokenize = JsxFlow::tokenize;
        INSTANCE.resolve = JsxFlow::resolve;
        INSTANCE.concrete = true;
    }

    private static List<Tokenizer.Event> resolve(List<Tokenizer.Event> events, TokenizeContext context) {
        if (events.isEmpty() || !Types.chunkText.equals(
            events.getLast()
                .token().type)) {
            return events;
        }
        // The summary label is inline content inside its flow container. Let the normal inline
        // tokenizer identify its closing tag, so a literal </summary> in code stays ordinary text.
        var text = events.getLast()
            .token();
        var paragraph = new Token();
        paragraph.type = Types.paragraph;
        paragraph.start = text.start;
        paragraph.end = text.end;
        ListUtils.splice(
            events,
            events.size() - 2,
            2,
            List.of(
                Tokenizer.Event.enter(paragraph, context),
                Tokenizer.Event.enter(text, context),
                Tokenizer.Event.exit(text, context),
                Tokenizer.Event.exit(paragraph, context)));
        return events;
    }

    private static boolean isOpeningSummary(String tag) {
        return tag.startsWith("<summary") && tag.length() > 8
            && (tag.charAt(8) == '>' || CharUtil.markdownLineEndingOrSpace(tag.charAt(8)))
            && !tag.endsWith("/>");
    }

    public static State tokenize(TokenizeContext context, Tokenizer.Effects effects, State ok, State nok) {
        class StateMachine {

            State start(int code) {
                Assert.check(code == Codes.lessThan, "expected `<`");
                return FactoryTag
                    .create(
                        context,
                        effects,
                        this::afterTag,
                        nok,
                        false,
                        "mdxJsxFlowTag",
                        "mdxJsxFlowTagMarker",
                        "mdxJsxFlowTagClosingMarker",
                        "mdxJsxFlowTagSelfClosingMarker",
                        "mdxJsxFlowTagName",
                        "mdxJsxFlowTagNamePrimary",
                        "mdxJsxFlowTagNameMemberMarker",
                        "mdxJsxFlowTagNameMember",
                        "mdxJsxFlowTagNamePrefixMarker",
                        "mdxJsxFlowTagNameLocal",
                        "mdxJsxFlowTagExpressionAttribute",
                        "mdxJsxFlowTagExpressionAttributeMarker",
                        "mdxJsxFlowTagExpressionAttributeValue",
                        "mdxJsxFlowTagAttribute",
                        "mdxJsxFlowTagAttributeName",
                        "mdxJsxFlowTagAttributeNamePrimary",
                        "mdxJsxFlowTagAttributeNamePrefixMarker",
                        "mdxJsxFlowTagAttributeNameLocal",
                        "mdxJsxFlowTagAttributeInitializerMarker",
                        "mdxJsxFlowTagAttributeValueLiteral",
                        "mdxJsxFlowTagAttributeValueLiteralMarker",
                        "mdxJsxFlowTagAttributeValueLiteralValue",
                        "mdxJsxFlowTagAttributeValueExpression",
                        "mdxJsxFlowTagAttributeValueExpressionMarker",
                        "mdxJsxFlowTagAttributeValueExpressionValue")
                    .step(code);
            }

            State afterTag(int code) {
                if (code == Codes.eof || CharUtil.markdownLineEnding(code)) {
                    return ok.step(code);
                }
                String tag = context.sliceSerialize(
                    context.getLastEvent()
                        .token());
                if (isOpeningSummary(tag)) {
                    // A summary usually has its label on the same line. Falling back to HTML flow here
                    // makes that line opaque until a blank line, swallowing the enclosing </details>.
                    // Tokenize the remainder as inline Markdown, including its closing summary tag.
                    return FactorySpace.create(effects, this::summaryStart, Types.whitespace)
                        .step(code);
                }
                return FactorySpace.create(effects, this::after, Types.whitespace)
                    .step(code);
            }

            State summaryStart(int code) {
                if (code == Codes.eof || CharUtil.markdownLineEnding(code)) {
                    return ok.step(code);
                }
                effects.enter(Types.chunkText).contentType = ContentType.TEXT;
                return summaryContent(code);
            }

            State summaryContent(int code) {
                if (code == Codes.eof || CharUtil.markdownLineEnding(code)) {
                    effects.exit(Types.chunkText);
                    return ok.step(code);
                }
                effects.consume(code);
                return this::summaryContent;
            }

            State after(int code) {
                // Another tag.
                return code == Codes.lessThan ? start(code)
                    : code == Codes.eof || CharUtil.markdownLineEnding(code) ? ok.step(code) : nok.step(code);
            }
        }

        return new StateMachine()::start;
    }

}
