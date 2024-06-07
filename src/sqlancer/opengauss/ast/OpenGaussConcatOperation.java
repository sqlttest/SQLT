package sqlancer.opengauss.ast;

import sqlancer.common.ast.BinaryNode;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussConcatOperation extends BinaryNode<OpenGaussExpression> implements OpenGaussExpression {

    public OpenGaussConcatOperation(OpenGaussExpression left, OpenGaussExpression right) {
        super(left, right);
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return OpenGaussDataType.TEXT;
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        OpenGaussConstant leftExpectedValue = getLeft().getExpectedValue();
        OpenGaussConstant rightExpectedValue = getRight().getExpectedValue();
        if (leftExpectedValue == null || rightExpectedValue == null) {
            return null;
        }
        if (leftExpectedValue.isNull() || rightExpectedValue.isNull()) {
            return OpenGaussConstant.createNullConstant();
        }
        String leftStr = leftExpectedValue.cast(OpenGaussDataType.TEXT).getUnquotedTextRepresentation();
        String rightStr = rightExpectedValue.cast(OpenGaussDataType.TEXT).getUnquotedTextRepresentation();
        return OpenGaussConstant.createTextConstant(leftStr + rightStr);
    }

    @Override
    public String getOperatorRepresentation() {
        return "||";
    }

}
