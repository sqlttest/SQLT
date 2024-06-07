package sqlancer.opengauss.ast;

import java.util.function.BinaryOperator;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryOperatorNode;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.ast.OpenGaussBinaryArithmeticOperation.OpenGaussBinaryOperator;

public class OpenGaussBinaryArithmeticOperation extends BinaryOperatorNode<OpenGaussExpression, OpenGaussBinaryOperator>
        implements OpenGaussExpression {

    public enum OpenGaussBinaryOperator implements Operator {

        ADDITION("+") {
            @Override
            public OpenGaussConstant apply(OpenGaussConstant left, OpenGaussConstant right) {
                return applyBitOperation(left, right, (l, r) -> l + r);
            }

        },
        SUBTRACTION("-") {
            @Override
            public OpenGaussConstant apply(OpenGaussConstant left, OpenGaussConstant right) {
                return applyBitOperation(left, right, (l, r) -> l - r);
            }
        },
        MULTIPLICATION("*") {
            @Override
            public OpenGaussConstant apply(OpenGaussConstant left, OpenGaussConstant right) {
                return applyBitOperation(left, right, (l, r) -> l * r);
            }
        },
        DIVISION("/") {

            @Override
            public OpenGaussConstant apply(OpenGaussConstant left, OpenGaussConstant right) {
                return applyBitOperation(left, right, (l, r) -> r == 0 ? -1 : l / r);

            }

        },
        MODULO("%") {
            @Override
            public OpenGaussConstant apply(OpenGaussConstant left, OpenGaussConstant right) {
                return applyBitOperation(left, right, (l, r) -> r == 0 ? -1 : l % r);

            }
        },
        EXPONENTIATION("^") {
            @Override
            public OpenGaussConstant apply(OpenGaussConstant left, OpenGaussConstant right) {
                return null;
            }
        };

        private String textRepresentation;

        private static OpenGaussConstant applyBitOperation(OpenGaussConstant left, OpenGaussConstant right,
                BinaryOperator<Long> op) {
            if (left.isNull() || right.isNull()) {
                return OpenGaussConstant.createNullConstant();
            } else {
                long leftVal = left.cast(OpenGaussDataType.INT).asInt();
                long rightVal = right.cast(OpenGaussDataType.INT).asInt();
                long value = op.apply(leftVal, rightVal);
                return OpenGaussConstant.createIntConstant(value);
            }
        }

        OpenGaussBinaryOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        @Override
        public String getTextRepresentation() {
            return textRepresentation;
        }

        public abstract OpenGaussConstant apply(OpenGaussConstant left, OpenGaussConstant right);

        public static OpenGaussBinaryOperator getRandom() {
            return Randomly.fromOptions(values());
        }

    }

    public OpenGaussBinaryArithmeticOperation(OpenGaussExpression left, OpenGaussExpression right,
            OpenGaussBinaryOperator op) {
        super(left, right, op);
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        OpenGaussConstant leftExpected = getLeft().getExpectedValue();
        OpenGaussConstant rightExpected = getRight().getExpectedValue();
        if (leftExpected == null || rightExpected == null) {
            return null;
        }
        return getOp().apply(leftExpected, rightExpected);
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return OpenGaussDataType.INT;
    }

}
