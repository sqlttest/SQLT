package sqlancer.opengauss.ast;

import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public interface OpenGaussExpression {

    default OpenGaussDataType getExpressionType() {
        return null;
    }

    default OpenGaussConstant getExpectedValue() {
        return null;
    }
}
