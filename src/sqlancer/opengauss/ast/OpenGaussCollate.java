package sqlancer.opengauss.ast;

import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussCollate implements OpenGaussExpression {

    private final OpenGaussExpression expr;
    private final String collate;

    public OpenGaussCollate(OpenGaussExpression expr, String collate) {
        this.expr = expr;
        this.collate = collate;
    }

    public String getCollate() {
        return collate;
    }

    public OpenGaussExpression getExpr() {
        return expr;
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return expr.getExpressionType();
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        return null;
    }

}
