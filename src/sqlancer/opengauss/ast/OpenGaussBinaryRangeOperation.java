package sqlancer.opengauss.ast;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryNode;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussBinaryRangeOperation extends BinaryNode<OpenGaussExpression> implements OpenGaussExpression {

    private final String op;

    public enum OpenGaussBinaryRangeOperator implements Operator {
        UNION("+"), INTERSECTION("*"), DIFFERENCE("-");

        private final String textRepresentation;

        OpenGaussBinaryRangeOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        @Override
        public String getTextRepresentation() {
            return textRepresentation;
        }

        public static OpenGaussBinaryRangeOperator getRandom() {
            return Randomly.fromOptions(values());
        }

    }

    public enum OpenGaussBinaryRangeComparisonOperator {
        CONTAINS_RANGE_OR_ELEMENT("@>"), RANGE_OR_ELEMENT_IS_CONTAINED("<@"), OVERLAP("&&"), STRICT_LEFT_OF("<<"),
        STRICT_RIGHT_OF(">>"), NOT_RIGHT_OF("&<"), NOT_LEFT_OF(">&"), ADJACENT("-|-");

        private final String textRepresentation;

        OpenGaussBinaryRangeComparisonOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public String getTextRepresentation() {
            return textRepresentation;
        }

        public static OpenGaussBinaryRangeComparisonOperator getRandom() {
            return Randomly.fromOptions(values());
        }
    }

    public OpenGaussBinaryRangeOperation(OpenGaussBinaryRangeComparisonOperator op, OpenGaussExpression left,
            OpenGaussExpression right) {
        super(left, right);
        this.op = op.getTextRepresentation();
    }

    public OpenGaussBinaryRangeOperation(OpenGaussBinaryRangeOperator op, OpenGaussExpression left,
            OpenGaussExpression right) {
        super(left, right);
        this.op = op.getTextRepresentation();
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return OpenGaussDataType.BOOLEAN;
    }

    @Override
    public String getOperatorRepresentation() {
        return op;
    }

}
