package sqlancer.opengauss.ast;

import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussPostfixText implements OpenGaussExpression {

    private final OpenGaussExpression expr;
    private final String text;
    private final OpenGaussConstant expectedValue;
    private final OpenGaussDataType type;

    public OpenGaussPostfixText(OpenGaussExpression expr, String text, OpenGaussConstant expectedValue,
            OpenGaussDataType type) {
        this.expr = expr;
        this.text = text;
        this.expectedValue = expectedValue;
        this.type = type;
    }

    public OpenGaussExpression getExpr() {
        return expr;
    }

    public String getText() {
        return text;
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        return expectedValue;
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return type;
    }
}
