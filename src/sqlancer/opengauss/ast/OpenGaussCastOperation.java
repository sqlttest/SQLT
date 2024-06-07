package sqlancer.opengauss.ast;

import sqlancer.opengauss.OpenGaussCompoundDataType;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussCastOperation implements OpenGaussExpression {

    private final OpenGaussExpression expression;
    private final OpenGaussCompoundDataType type;

    public OpenGaussCastOperation(OpenGaussExpression expression, OpenGaussCompoundDataType type) {
        if (expression == null) {
            throw new AssertionError();
        }
        this.expression = expression;
        this.type = type;
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return type.getDataType();
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        OpenGaussConstant expectedValue = expression.getExpectedValue();
        if (expectedValue == null) {
            return null;
        }
        return expectedValue.cast(type.getDataType());
    }

    public OpenGaussExpression getExpression() {
        return expression;
    }

    public OpenGaussDataType getType() {
        return type.getDataType();
    }

    public OpenGaussCompoundDataType getCompoundType() {
        return type;
    }

}
