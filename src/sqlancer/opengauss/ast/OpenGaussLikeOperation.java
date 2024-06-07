package sqlancer.opengauss.ast;

import sqlancer.LikeImplementationHelper;
import sqlancer.common.ast.BinaryNode;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussLikeOperation extends BinaryNode<OpenGaussExpression> implements OpenGaussExpression {

    public OpenGaussLikeOperation(OpenGaussExpression left, OpenGaussExpression right) {
        super(left, right);
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return OpenGaussDataType.BOOLEAN;
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        OpenGaussConstant leftVal = getLeft().getExpectedValue();
        OpenGaussConstant rightVal = getRight().getExpectedValue();
        if (leftVal == null || rightVal == null) {
            return null;
        }
        if (leftVal.isNull() || rightVal.isNull()) {
            return OpenGaussConstant.createNullConstant();
        } else {
            boolean val = LikeImplementationHelper.match(leftVal.asString(), rightVal.asString(), 0, 0, true);
            return OpenGaussConstant.createBooleanConstant(val);
        }
    }

    @Override
    public String getOperatorRepresentation() {
        return "LIKE";
    }

}
