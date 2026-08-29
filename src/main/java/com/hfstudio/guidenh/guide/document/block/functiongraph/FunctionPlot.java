package com.hfstudio.guidenh.guide.document.block.functiongraph;

import lombok.Getter;

/**
 * One curve definition inside a {@link LytFunctionGraph}. Each plot owns a parsed expression, an
 * optional inverse flag (so {@code x = f(y)} curves can be drawn by sweeping y), an optional domain
 * predicate, and the colour / label used for tooltips and legends.
 */
@Getter
public class FunctionPlot {

    private final String expressionText;
    private final FunctionExpr expression;
    private final boolean inverse;
    private final DomainPredicate domain;
    private final int color;
    private final String label;
    private final AutoPointSpec autoPointSpec;
    private final String tooltipText;
    private final boolean showFunction;
    private final boolean showValues;

    public FunctionPlot(String expressionText, FunctionExpr expression, boolean inverse, DomainPredicate domain,
        int color, String label) {
        this(expressionText, expression, inverse, domain, color, label, AutoPointSpec.NONE);
    }

    public FunctionPlot(String expressionText, FunctionExpr expression, boolean inverse, DomainPredicate domain,
        int color, String label, AutoPointSpec autoPointSpec) {
        this(expressionText, expression, inverse, domain, color, label, autoPointSpec, null, true, true);
    }

    public FunctionPlot(String expressionText, FunctionExpr expression, boolean inverse, DomainPredicate domain,
        int color, String label, AutoPointSpec autoPointSpec, String tooltipText, boolean showFunction,
        boolean showValues) {
        this.expressionText = expressionText != null ? expressionText : "";
        this.expression = expression != null ? expression : new FunctionExpr.Constant(Double.NaN);
        this.inverse = inverse;
        this.domain = domain != null ? domain : DomainPredicate.ALWAYS;
        this.color = color;
        this.label = label;
        this.autoPointSpec = autoPointSpec != null ? autoPointSpec : AutoPointSpec.NONE;
        this.tooltipText = tooltipText;
        this.showFunction = showFunction;
        this.showValues = showValues;
    }

    /**
     * Evaluate this plot's dependent value at the supplied independent value. For normal plots
     * {@code independent} is {@code x} and the returned value is {@code y}; for inverse plots they
     * are swapped.
     */
    public double evaluate(double independent) {
        if (inverse) {
            if (!domain.accepts(0d, independent)) {
                return Double.NaN;
            }
            return expression.evaluate(0d, independent);
        }
        if (!domain.accepts(independent, 0d)) {
            return Double.NaN;
        }
        return expression.evaluate(independent, 0d);
    }
}
