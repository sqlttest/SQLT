package sqlancer.opengauss.ast;

import sqlancer.Randomly;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussJoin implements OpenGaussExpression {

    public enum OpenGaussJoinType {
        INNER, LEFT, RIGHT, FULL, CROSS;

        public static OpenGaussJoinType getRandom() {
            return Randomly.fromOptions(values());
        }

    }

    private final OpenGaussExpression tableReference;
    private final OpenGaussExpression onClause;
    private final OpenGaussJoinType type;

    public OpenGaussJoin(OpenGaussExpression tableReference, OpenGaussExpression onClause, OpenGaussJoinType type) {
        this.tableReference = tableReference;
        this.onClause = onClause;
        this.type = type;
    }

    public OpenGaussExpression getTableReference() {
        return tableReference;
    }

    public OpenGaussExpression getOnClause() {
        return onClause;
    }

    public OpenGaussJoinType getType() {
        return type;
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        throw new AssertionError();
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        throw new AssertionError();
    }

}
