package sqlancer.opengauss.ast;

import java.util.List;

import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussInOperation implements OpenGaussExpression {

    private final OpenGaussExpression expr;
    private final List<OpenGaussExpression> listElements;
    private final boolean isTrue;

    public OpenGaussInOperation(OpenGaussExpression expr, List<OpenGaussExpression> listElements, boolean isTrue) {
        this.expr = expr;
        this.listElements = listElements;
        this.isTrue = isTrue;
    }

    public OpenGaussExpression getExpr() {
        return expr;
    }

    public List<OpenGaussExpression> getListElements() {
        return listElements;
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        OpenGaussConstant leftValue = expr.getExpectedValue();
        if (leftValue == null) {
            return null;
        }
        if (leftValue.isNull()) {
            return OpenGaussConstant.createNullConstant();
        }
        boolean isNull = false;
        for (OpenGaussExpression expr : getListElements()) {
            OpenGaussConstant rightExpectedValue = expr.getExpectedValue();
            if (rightExpectedValue == null) {
                return null;
            }
            if (rightExpectedValue.isNull()) {
                isNull = true;
            } else if (rightExpectedValue.isEquals(this.expr.getExpectedValue()).isBoolean()
                    && rightExpectedValue.isEquals(this.expr.getExpectedValue()).asBoolean()) {
                return OpenGaussConstant.createBooleanConstant(isTrue);
            }
        }

        if (isNull) {
            return OpenGaussConstant.createNullConstant();
        } else {
            return OpenGaussConstant.createBooleanConstant(!isTrue);
        }
    }

    public boolean isTrue() {
        return isTrue;
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return OpenGaussDataType.BOOLEAN;
    }
}
