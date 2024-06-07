package sqlancer.opengauss.ast;

import sqlancer.opengauss.OpenGaussSchema.OpenGaussColumn;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussColumnValue implements OpenGaussExpression {

    private final OpenGaussColumn c;
    private final OpenGaussConstant expectedValue;

    public OpenGaussColumnValue(OpenGaussColumn c, OpenGaussConstant expectedValue) {
        this.c = c;
        this.expectedValue = expectedValue;
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return c.getType();
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        return expectedValue;
    }

    public static OpenGaussColumnValue create(OpenGaussColumn c, OpenGaussConstant expected) {
        return new OpenGaussColumnValue(c, expected);
    }

    public OpenGaussColumn getColumn() {
        return c;
    }

}
