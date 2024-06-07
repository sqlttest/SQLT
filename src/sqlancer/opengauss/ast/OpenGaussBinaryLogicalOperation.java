package sqlancer.opengauss.ast;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryOperatorNode;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.ast.OpenGaussBinaryLogicalOperation.BinaryLogicalOperator;

public class OpenGaussBinaryLogicalOperation extends BinaryOperatorNode<OpenGaussExpression, BinaryLogicalOperator>
        implements OpenGaussExpression {

    public enum BinaryLogicalOperator implements Operator {
        AND {
            @Override
            public OpenGaussConstant apply(OpenGaussConstant left, OpenGaussConstant right) {
                OpenGaussConstant leftBool = left.cast(OpenGaussDataType.BOOLEAN);
                OpenGaussConstant rightBool = right.cast(OpenGaussDataType.BOOLEAN);
                if (leftBool.isNull()) {
                    if (rightBool.isNull()) {
                        return OpenGaussConstant.createNullConstant();
                    } else {
                        if (rightBool.asBoolean()) {
                            return OpenGaussConstant.createNullConstant();
                        } else {
                            return OpenGaussConstant.createFalse();
                        }
                    }
                } else if (!leftBool.asBoolean()) {
                    return OpenGaussConstant.createFalse();
                }
                assert leftBool.asBoolean();
                if (rightBool.isNull()) {
                    return OpenGaussConstant.createNullConstant();
                } else {
                    return OpenGaussConstant.createBooleanConstant(rightBool.isBoolean() && rightBool.asBoolean());
                }
            }
        },
        OR {
            @Override
            public OpenGaussConstant apply(OpenGaussConstant left, OpenGaussConstant right) {
                OpenGaussConstant leftBool = left.cast(OpenGaussDataType.BOOLEAN);
                OpenGaussConstant rightBool = right.cast(OpenGaussDataType.BOOLEAN);
                if (leftBool.isBoolean() && leftBool.asBoolean()) {
                    return OpenGaussConstant.createTrue();
                }
                if (rightBool.isBoolean() && rightBool.asBoolean()) {
                    return OpenGaussConstant.createTrue();
                }
                if (leftBool.isNull() || rightBool.isNull()) {
                    return OpenGaussConstant.createNullConstant();
                }
                return OpenGaussConstant.createFalse();
            }
        };

        public abstract OpenGaussConstant apply(OpenGaussConstant left, OpenGaussConstant right);

        public static BinaryLogicalOperator getRandom() {
            return Randomly.fromOptions(values());
        }

        @Override
        public String getTextRepresentation() {
            return toString();
        }
    }

    public OpenGaussBinaryLogicalOperation(OpenGaussExpression left, OpenGaussExpression right, BinaryLogicalOperator op) {
        super(left, right, op);
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return OpenGaussDataType.BOOLEAN;
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        OpenGaussConstant leftExpectedValue = getLeft().getExpectedValue();
        OpenGaussConstant rightExpectedValue = getRight().getExpectedValue();
        if (leftExpectedValue == null || rightExpectedValue == null) {
            return null;
        }
        return getOp().apply(leftExpectedValue, rightExpectedValue);
    }

}
