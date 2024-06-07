package sqlancer.opengauss.ast;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryOperatorNode;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.ast.OpenGaussBinaryComparisonOperation.OpenGaussBinaryComparisonOperator;

public class OpenGaussBinaryComparisonOperation
        extends BinaryOperatorNode<OpenGaussExpression, OpenGaussBinaryComparisonOperator> implements OpenGaussExpression {

    public enum OpenGaussBinaryComparisonOperator implements Operator {
        EQUALS("=") {
            @Override
            public OpenGaussConstant getExpectedValue(OpenGaussConstant leftVal, OpenGaussConstant rightVal) {
                return leftVal.isEquals(rightVal);
            }
        },
        IS_DISTINCT("IS DISTINCT FROM") {
            @Override
            public OpenGaussConstant getExpectedValue(OpenGaussConstant leftVal, OpenGaussConstant rightVal) {
                return OpenGaussConstant
                        .createBooleanConstant(!IS_NOT_DISTINCT.getExpectedValue(leftVal, rightVal).asBoolean());
            }
        },
        IS_NOT_DISTINCT("IS NOT DISTINCT FROM") {
            @Override
            public OpenGaussConstant getExpectedValue(OpenGaussConstant leftVal, OpenGaussConstant rightVal) {
                if (leftVal.isNull()) {
                    return OpenGaussConstant.createBooleanConstant(rightVal.isNull());
                } else if (rightVal.isNull()) {
                    return OpenGaussConstant.createFalse();
                } else {
                    return leftVal.isEquals(rightVal);
                }
            }
        },
        NOT_EQUALS("!=") {
            @Override
            public OpenGaussConstant getExpectedValue(OpenGaussConstant leftVal, OpenGaussConstant rightVal) {
                OpenGaussConstant isEquals = leftVal.isEquals(rightVal);
                if (isEquals.isBoolean()) {
                    return OpenGaussConstant.createBooleanConstant(!isEquals.asBoolean());
                }
                return isEquals;
            }
        },
        LESS("<") {

            @Override
            public OpenGaussConstant getExpectedValue(OpenGaussConstant leftVal, OpenGaussConstant rightVal) {
                return leftVal.isLessThan(rightVal);
            }
        },
        LESS_EQUALS("<=") {

            @Override
            public OpenGaussConstant getExpectedValue(OpenGaussConstant leftVal, OpenGaussConstant rightVal) {
                OpenGaussConstant lessThan = leftVal.isLessThan(rightVal);
                if (lessThan.isBoolean() && !lessThan.asBoolean()) {
                    return leftVal.isEquals(rightVal);
                } else {
                    return lessThan;
                }
            }
        },
        GREATER(">") {
            @Override
            public OpenGaussConstant getExpectedValue(OpenGaussConstant leftVal, OpenGaussConstant rightVal) {
                OpenGaussConstant equals = leftVal.isEquals(rightVal);
                if (equals.isBoolean() && equals.asBoolean()) {
                    return OpenGaussConstant.createFalse();
                } else {
                    OpenGaussConstant applyLess = leftVal.isLessThan(rightVal);
                    if (applyLess.isNull()) {
                        return OpenGaussConstant.createNullConstant();
                    }
                    return OpenGaussPrefixOperation.PrefixOperator.NOT.getExpectedValue(applyLess);
                }
            }
        },
        GREATER_EQUALS(">=") {

            @Override
            public OpenGaussConstant getExpectedValue(OpenGaussConstant leftVal, OpenGaussConstant rightVal) {
                OpenGaussConstant equals = leftVal.isEquals(rightVal);
                if (equals.isBoolean() && equals.asBoolean()) {
                    return OpenGaussConstant.createTrue();
                } else {
                    OpenGaussConstant applyLess = leftVal.isLessThan(rightVal);
                    if (applyLess.isNull()) {
                        return OpenGaussConstant.createNullConstant();
                    }
                    return OpenGaussPrefixOperation.PrefixOperator.NOT.getExpectedValue(applyLess);
                }
            }

        };

        private final String textRepresentation;

        @Override
        public String getTextRepresentation() {
            return textRepresentation;
        }

        OpenGaussBinaryComparisonOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public abstract OpenGaussConstant getExpectedValue(OpenGaussConstant leftVal, OpenGaussConstant rightVal);

        public static OpenGaussBinaryComparisonOperator getRandom() {
            return Randomly.fromOptions(OpenGaussBinaryComparisonOperator.values());
        }

    }

    public OpenGaussBinaryComparisonOperation(OpenGaussExpression left, OpenGaussExpression right,
            OpenGaussBinaryComparisonOperator op) {
        super(left, right, op);
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        OpenGaussConstant leftExpectedValue = getLeft().getExpectedValue();
        OpenGaussConstant rightExpectedValue = getRight().getExpectedValue();
        if (leftExpectedValue == null || rightExpectedValue == null) {
            return null;
        }
        return getOp().getExpectedValue(leftExpectedValue, rightExpectedValue);
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return OpenGaussDataType.BOOLEAN;
    }

}
