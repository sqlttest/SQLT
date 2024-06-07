package sqlancer.opengauss.ast;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryOperatorNode;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.ast.OpenGaussBinaryBitOperation.OpenGaussBinaryBitOperator;

public class OpenGaussBinaryBitOperation extends BinaryOperatorNode<OpenGaussExpression, OpenGaussBinaryBitOperator>
        implements OpenGaussExpression {

    public enum OpenGaussBinaryBitOperator implements Operator {
        CONCATENATION("||"), //
        BITWISE_AND("&"), //
        BITWISE_OR("|"), //
        BITWISE_XOR("#"), //
        BITWISE_SHIFT_LEFT("<<"), //
        BITWISE_SHIFT_RIGHT(">>");

        private String text;

        OpenGaussBinaryBitOperator(String text) {
            this.text = text;
        }

        public static OpenGaussBinaryBitOperator getRandom() {
            return Randomly.fromOptions(OpenGaussBinaryBitOperator.values());
        }

        @Override
        public String getTextRepresentation() {
            return text;
        }

    }

    public OpenGaussBinaryBitOperation(OpenGaussBinaryBitOperator op, OpenGaussExpression left, OpenGaussExpression right) {
        super(left, right, op);
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return OpenGaussDataType.BIT;
    }

}
