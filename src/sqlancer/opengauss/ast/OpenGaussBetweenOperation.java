package sqlancer.opengauss.ast;

import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.ast.OpenGaussBinaryComparisonOperation.OpenGaussBinaryComparisonOperator;
import sqlancer.opengauss.ast.OpenGaussBinaryLogicalOperation.BinaryLogicalOperator;

public final class OpenGaussBetweenOperation implements OpenGaussExpression {

    private final OpenGaussExpression expr;
    private final OpenGaussExpression left;
    private final OpenGaussExpression right;
    private final boolean isSymmetric;

    public OpenGaussBetweenOperation(OpenGaussExpression expr, OpenGaussExpression left, OpenGaussExpression right,
            boolean symmetric) {
        this.expr = expr;
        this.left = left;
        this.right = right;
        isSymmetric = symmetric;
    }

    public OpenGaussExpression getExpr() {
        return expr;
    }

    public OpenGaussExpression getLeft() {
        return left;
    }

    public OpenGaussExpression getRight() {
        return right;
    }

    public boolean isSymmetric() {
        return isSymmetric;
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        OpenGaussBinaryComparisonOperation leftComparison = new OpenGaussBinaryComparisonOperation(left, expr,
                OpenGaussBinaryComparisonOperator.LESS_EQUALS);
        OpenGaussBinaryComparisonOperation rightComparison = new OpenGaussBinaryComparisonOperation(expr, right,
                OpenGaussBinaryComparisonOperator.LESS_EQUALS);
        OpenGaussBinaryLogicalOperation andOperation = new OpenGaussBinaryLogicalOperation(leftComparison,
                rightComparison, OpenGaussBinaryLogicalOperation.BinaryLogicalOperator.AND);
        if (isSymmetric) {
            OpenGaussBinaryComparisonOperation leftComparison2 = new OpenGaussBinaryComparisonOperation(right, expr,
                    OpenGaussBinaryComparisonOperator.LESS_EQUALS);
            OpenGaussBinaryComparisonOperation rightComparison2 = new OpenGaussBinaryComparisonOperation(expr, left,
                    OpenGaussBinaryComparisonOperator.LESS_EQUALS);
            OpenGaussBinaryLogicalOperation andOperation2 = new OpenGaussBinaryLogicalOperation(leftComparison2,
                    rightComparison2, OpenGaussBinaryLogicalOperation.BinaryLogicalOperator.AND);
            OpenGaussBinaryLogicalOperation orOp = new OpenGaussBinaryLogicalOperation(andOperation, andOperation2,
                    BinaryLogicalOperator.OR);
            return orOp.getExpectedValue();
        } else {
            return andOperation.getExpectedValue();
        }
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return OpenGaussDataType.BOOLEAN;
    }

}
