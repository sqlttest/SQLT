package sqlancer.opengauss.ast;

import sqlancer.IgnoreMeException;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussPrefixOperation implements OpenGaussExpression {

    public enum PrefixOperator implements Operator {
        NOT("NOT", OpenGaussDataType.BOOLEAN) {

            @Override
            public OpenGaussDataType getExpressionType() {
                return OpenGaussDataType.BOOLEAN;
            }

            @Override
            protected OpenGaussConstant getExpectedValue(OpenGaussConstant expectedValue) {
                if (expectedValue.isNull()) {
                    return OpenGaussConstant.createNullConstant();
                } else {
                    return OpenGaussConstant
                            .createBooleanConstant(!expectedValue.cast(OpenGaussDataType.BOOLEAN).asBoolean());
                }
            }
        },
        UNARY_PLUS("+", OpenGaussDataType.INT) {

            @Override
            public OpenGaussDataType getExpressionType() {
                return OpenGaussDataType.INT;
            }

            @Override
            protected OpenGaussConstant getExpectedValue(OpenGaussConstant expectedValue) {
                // TODO: actual converts to double precision
                return expectedValue;
            }

        },
        UNARY_MINUS("-", OpenGaussDataType.INT) {

            @Override
            public OpenGaussDataType getExpressionType() {
                return OpenGaussDataType.INT;
            }

            @Override
            protected OpenGaussConstant getExpectedValue(OpenGaussConstant expectedValue) {
                if (expectedValue.isNull()) {
                    // TODO
                    throw new IgnoreMeException();
                }
                if (expectedValue.isInt() && expectedValue.asInt() == Long.MIN_VALUE) {
                    throw new IgnoreMeException();
                }
                try {
                    return OpenGaussConstant.createIntConstant(-expectedValue.asInt());
                } catch (UnsupportedOperationException e) {
                    return null;
                }
            }

        };

        private String textRepresentation;
        private OpenGaussDataType[] dataTypes;

        PrefixOperator(String textRepresentation, OpenGaussDataType... dataTypes) {
            this.textRepresentation = textRepresentation;
            this.dataTypes = dataTypes.clone();
        }

        public abstract OpenGaussDataType getExpressionType();

        protected abstract OpenGaussConstant getExpectedValue(OpenGaussConstant expectedValue);

        @Override
        public String getTextRepresentation() {
            return toString();
        }

    }

    private final OpenGaussExpression expr;
    private final PrefixOperator op;

    public OpenGaussPrefixOperation(OpenGaussExpression expr, PrefixOperator op) {
        this.expr = expr;
        this.op = op;
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return op.getExpressionType();
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        OpenGaussConstant expectedValue = expr.getExpectedValue();
        if (expectedValue == null) {
            return null;
        }
        return op.getExpectedValue(expectedValue);
    }

    public OpenGaussDataType[] getInputDataTypes() {
        return op.dataTypes;
    }

    public String getTextRepresentation() {
        return op.textRepresentation;
    }

    public OpenGaussExpression getExpression() {
        return expr;
    }

}
