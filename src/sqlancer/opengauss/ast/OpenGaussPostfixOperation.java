package sqlancer.opengauss.ast;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussPostfixOperation implements OpenGaussExpression {

    private final OpenGaussExpression expr;
    private final PostfixOperator op;
    private final String operatorTextRepresentation;

    public enum PostfixOperator implements Operator {
        IS_NULL("IS NULL", "ISNULL") {
            @Override
            public OpenGaussConstant apply(OpenGaussConstant expectedValue) {
                return OpenGaussConstant.createBooleanConstant(expectedValue.isNull());
            }

            @Override
            public OpenGaussDataType[] getInputDataTypes() {
                return OpenGaussDataType.values();
            }

        },
        IS_UNKNOWN("IS UNKNOWN") {
            @Override
            public OpenGaussConstant apply(OpenGaussConstant expectedValue) {
                return OpenGaussConstant.createBooleanConstant(expectedValue.isNull());
            }

            @Override
            public OpenGaussDataType[] getInputDataTypes() {
                return new OpenGaussDataType[] { OpenGaussDataType.BOOLEAN };
            }
        },

        IS_NOT_NULL("IS NOT NULL", "NOTNULL") {

            @Override
            public OpenGaussConstant apply(OpenGaussConstant expectedValue) {
                return OpenGaussConstant.createBooleanConstant(!expectedValue.isNull());
            }

            @Override
            public OpenGaussDataType[] getInputDataTypes() {
                return OpenGaussDataType.values();
            }

        },
        IS_NOT_UNKNOWN("IS NOT UNKNOWN") {
            @Override
            public OpenGaussConstant apply(OpenGaussConstant expectedValue) {
                return OpenGaussConstant.createBooleanConstant(!expectedValue.isNull());
            }

            @Override
            public OpenGaussDataType[] getInputDataTypes() {
                return new OpenGaussDataType[] { OpenGaussDataType.BOOLEAN };
            }
        },
        IS_TRUE("IS TRUE") {

            @Override
            public OpenGaussConstant apply(OpenGaussConstant expectedValue) {
                if (expectedValue.isNull()) {
                    return OpenGaussConstant.createFalse();
                } else {
                    return OpenGaussConstant
                            .createBooleanConstant(expectedValue.cast(OpenGaussDataType.BOOLEAN).asBoolean());
                }
            }

            @Override
            public OpenGaussDataType[] getInputDataTypes() {
                return new OpenGaussDataType[] { OpenGaussDataType.BOOLEAN };
            }

        },
        IS_FALSE("IS FALSE") {

            @Override
            public OpenGaussConstant apply(OpenGaussConstant expectedValue) {
                if (expectedValue.isNull()) {
                    return OpenGaussConstant.createFalse();
                } else {
                    return OpenGaussConstant
                            .createBooleanConstant(!expectedValue.cast(OpenGaussDataType.BOOLEAN).asBoolean());
                }
            }

            @Override
            public OpenGaussDataType[] getInputDataTypes() {
                return new OpenGaussDataType[] { OpenGaussDataType.BOOLEAN };
            }

        };

        private String[] textRepresentations;

        PostfixOperator(String... textRepresentations) {
            this.textRepresentations = textRepresentations.clone();
        }

        public abstract OpenGaussConstant apply(OpenGaussConstant expectedValue);

        public abstract OpenGaussDataType[] getInputDataTypes();

        public static PostfixOperator getRandom() {
            return Randomly.fromOptions(values());
        }

        @Override
        public String getTextRepresentation() {
            return toString();
        }
    }

    public OpenGaussPostfixOperation(OpenGaussExpression expr, PostfixOperator op) {
        this.expr = expr;
        this.operatorTextRepresentation = Randomly.fromOptions(op.textRepresentations);
        this.op = op;
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return OpenGaussDataType.BOOLEAN;
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        OpenGaussConstant expectedValue = expr.getExpectedValue();
        if (expectedValue == null) {
            return null;
        }
        return op.apply(expectedValue);
    }

    public String getOperatorTextRepresentation() {
        return operatorTextRepresentation;
    }

    public static OpenGaussExpression create(OpenGaussExpression expr, PostfixOperator op) {
        return new OpenGaussPostfixOperation(expr, op);
    }

    public OpenGaussExpression getExpression() {
        return expr;
    }

}
