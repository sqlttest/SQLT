package sqlancer.opengauss.ast;

import sqlancer.Randomly;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussOrderByTerm implements OpenGaussExpression {

    private final OpenGaussOrder order;
    private final OpenGaussExpression expr;

    public enum OpenGaussOrder {
        ASC, DESC;

        public static OpenGaussOrder getRandomOrder() {
            return Randomly.fromOptions(OpenGaussOrder.values());
        }
    }

    public OpenGaussOrderByTerm(OpenGaussExpression expr, OpenGaussOrder order) {
        this.expr = expr;
        this.order = order;
    }

    public OpenGaussOrder getOrder() {
        return order;
    }

    public OpenGaussExpression getExpr() {
        return expr;
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        throw new AssertionError(this);
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return null;
    }

}
