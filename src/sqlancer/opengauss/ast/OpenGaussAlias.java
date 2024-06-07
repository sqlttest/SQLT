package sqlancer.opengauss.ast;

import sqlancer.common.visitor.UnaryOperation;

public class OpenGaussAlias implements UnaryOperation<OpenGaussExpression>, OpenGaussExpression {

    private final OpenGaussExpression expr;
    private final String alias;

    public OpenGaussAlias(OpenGaussExpression expr, String alias) {
        this.expr = expr;
        this.alias = alias;
    }

    @Override
    public OpenGaussExpression getExpression() {
        return expr;
    }

    @Override
    public String getOperatorRepresentation() {
        return " as " + alias;
    }

    @Override
    public OperatorKind getOperatorKind() {
        return OperatorKind.POSTFIX;
    }

    @Override
    public boolean omitBracketsWhenPrinting() {
        return true;
    }

}
