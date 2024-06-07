package sqlancer.opengauss.ast;

import java.math.BigDecimal;

import sqlancer.IgnoreMeException;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public abstract class OpenGaussConstant implements OpenGaussExpression {

    public abstract String getTextRepresentation();

    public abstract String getUnquotedTextRepresentation();

    public static class BooleanConstant extends OpenGaussConstant {

        private final boolean value;

        public BooleanConstant(boolean value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            return value ? "TRUE" : "FALSE";
        }

        @Override
        public OpenGaussDataType getExpressionType() {
            return OpenGaussDataType.BOOLEAN;
        }

        @Override
        public boolean asBoolean() {
            return value;
        }

        @Override
        public boolean isBoolean() {
            return true;
        }

        @Override
        public OpenGaussConstant isEquals(OpenGaussConstant rightVal) {
            if (rightVal.isNull()) {
                return OpenGaussConstant.createNullConstant();
            } else if (rightVal.isBoolean()) {
                return OpenGaussConstant.createBooleanConstant(value == rightVal.asBoolean());
            } else if (rightVal.isString()) {
                return OpenGaussConstant
                        .createBooleanConstant(value == rightVal.cast(OpenGaussDataType.BOOLEAN).asBoolean());
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected OpenGaussConstant isLessThan(OpenGaussConstant rightVal) {
            if (rightVal.isNull()) {
                return OpenGaussConstant.createNullConstant();
            } else if (rightVal.isString()) {
                return isLessThan(rightVal.cast(OpenGaussDataType.BOOLEAN));
            } else {
                assert rightVal.isBoolean();
                return OpenGaussConstant.createBooleanConstant((value ? 1 : 0) < (rightVal.asBoolean() ? 1 : 0));
            }
        }

        @Override
        public OpenGaussConstant cast(OpenGaussDataType type) {
            switch (type) {
            case BOOLEAN:
                return this;
            case INT:
                return OpenGaussConstant.createIntConstant(value ? 1 : 0);
            case TEXT:
                return OpenGaussConstant.createTextConstant(value ? "true" : "false");
            default:
                return null;
            }
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return getTextRepresentation();
        }

    }

    public static class OpenGaussNullConstant extends OpenGaussConstant {

        @Override
        public String getTextRepresentation() {
            return "NULL";
        }

        @Override
        public OpenGaussDataType getExpressionType() {
            return null;
        }

        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public OpenGaussConstant isEquals(OpenGaussConstant rightVal) {
            return OpenGaussConstant.createNullConstant();
        }

        @Override
        protected OpenGaussConstant isLessThan(OpenGaussConstant rightVal) {
            return OpenGaussConstant.createNullConstant();
        }

        @Override
        public OpenGaussConstant cast(OpenGaussDataType type) {
            return OpenGaussConstant.createNullConstant();
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return getTextRepresentation();
        }

    }

    public static class StringConstant extends OpenGaussConstant {

        private final String value;

        public StringConstant(String value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            return String.format("'%s'", value.replace("'", "''"));
        }

        @Override
        public OpenGaussConstant isEquals(OpenGaussConstant rightVal) {
            if (rightVal.isNull()) {
                return OpenGaussConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                return cast(OpenGaussDataType.INT).isEquals(rightVal.cast(OpenGaussDataType.INT));
            } else if (rightVal.isBoolean()) {
                return cast(OpenGaussDataType.BOOLEAN).isEquals(rightVal.cast(OpenGaussDataType.BOOLEAN));
            } else if (rightVal.isString()) {
                return OpenGaussConstant.createBooleanConstant(value.contentEquals(rightVal.asString()));
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected OpenGaussConstant isLessThan(OpenGaussConstant rightVal) {
            if (rightVal.isNull()) {
                return OpenGaussConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                return cast(OpenGaussDataType.INT).isLessThan(rightVal.cast(OpenGaussDataType.INT));
            } else if (rightVal.isBoolean()) {
                return cast(OpenGaussDataType.BOOLEAN).isLessThan(rightVal.cast(OpenGaussDataType.BOOLEAN));
            } else if (rightVal.isString()) {
                return OpenGaussConstant.createBooleanConstant(value.compareTo(rightVal.asString()) < 0);
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        public OpenGaussConstant cast(OpenGaussDataType type) {
            if (type == OpenGaussDataType.TEXT) {
                return this;
            }
            String s = value.trim();
            switch (type) {
            case BOOLEAN:
                try {
                    return OpenGaussConstant.createBooleanConstant(Long.parseLong(s) != 0);
                } catch (NumberFormatException e) {
                }
                switch (s.toUpperCase()) {
                case "T":
                case "TR":
                case "TRU":
                case "TRUE":
                case "1":
                case "YES":
                case "YE":
                case "Y":
                case "ON":
                    return OpenGaussConstant.createTrue();
                case "F":
                case "FA":
                case "FAL":
                case "FALS":
                case "FALSE":
                case "N":
                case "NO":
                case "OF":
                case "OFF":
                default:
                    return OpenGaussConstant.createFalse();
                }
            case INT:
                try {
                    return OpenGaussConstant.createIntConstant(Long.parseLong(s));
                } catch (NumberFormatException e) {
                    return OpenGaussConstant.createIntConstant(-1);
                }
            case TEXT:
                return this;
            default:
                return null;
            }
        }

        @Override
        public OpenGaussDataType getExpressionType() {
            return OpenGaussDataType.TEXT;
        }

        @Override
        public boolean isString() {
            return true;
        }

        @Override
        public String asString() {
            return value;
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return value;
        }

    }

    public static class IntConstant extends OpenGaussConstant {

        private final long val;

        public IntConstant(long val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(val);
        }

        @Override
        public OpenGaussDataType getExpressionType() {
            return OpenGaussDataType.INT;
        }

        @Override
        public long asInt() {
            return val;
        }

        @Override
        public boolean isInt() {
            return true;
        }

        @Override
        public OpenGaussConstant isEquals(OpenGaussConstant rightVal) {
            if (rightVal.isNull()) {
                return OpenGaussConstant.createNullConstant();
            } else if (rightVal.isBoolean()) {
                return cast(OpenGaussDataType.BOOLEAN).isEquals(rightVal);
            } else if (rightVal.isInt()) {
                return OpenGaussConstant.createBooleanConstant(val == rightVal.asInt());
            } else if (rightVal.isString()) {
                return OpenGaussConstant.createBooleanConstant(val == rightVal.cast(OpenGaussDataType.INT).asInt());
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected OpenGaussConstant isLessThan(OpenGaussConstant rightVal) {
            if (rightVal.isNull()) {
                return OpenGaussConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                return OpenGaussConstant.createBooleanConstant(val < rightVal.asInt());
            } else if (rightVal.isBoolean()) {
                throw new AssertionError(rightVal);
            } else if (rightVal.isString()) {
                return OpenGaussConstant.createBooleanConstant(val < rightVal.cast(OpenGaussDataType.INT).asInt());
            } else {
                throw new IgnoreMeException();
            }

        }

        @Override
        public OpenGaussConstant cast(OpenGaussDataType type) {
            switch (type) {
            case BOOLEAN:
                return OpenGaussConstant.createBooleanConstant(val != 0);
            case INT:
                return this;
            case TEXT:
                return OpenGaussConstant.createTextConstant(String.valueOf(val));
            default:
                return null;
            }
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return getTextRepresentation();
        }

    }

    public static OpenGaussConstant createNullConstant() {
        return new OpenGaussNullConstant();
    }

    public String asString() {
        throw new UnsupportedOperationException(this.toString());
    }

    public boolean isString() {
        return false;
    }

    public static OpenGaussConstant createIntConstant(long val) {
        return new IntConstant(val);
    }

    public static OpenGaussConstant createBooleanConstant(boolean val) {
        return new BooleanConstant(val);
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        return this;
    }

    public boolean isNull() {
        return false;
    }

    public boolean asBoolean() {
        throw new UnsupportedOperationException(this.toString());
    }

    public static OpenGaussConstant createFalse() {
        return createBooleanConstant(false);
    }

    public static OpenGaussConstant createTrue() {
        return createBooleanConstant(true);
    }

    public long asInt() {
        throw new UnsupportedOperationException(this.toString());
    }

    public boolean isBoolean() {
        return false;
    }

    public abstract OpenGaussConstant isEquals(OpenGaussConstant rightVal);

    public boolean isInt() {
        return false;
    }

    protected abstract OpenGaussConstant isLessThan(OpenGaussConstant rightVal);

    @Override
    public String toString() {
        return getTextRepresentation();
    }

    public abstract OpenGaussConstant cast(OpenGaussDataType type);

    public static OpenGaussConstant createTextConstant(String string) {
        return new StringConstant(string);
    }

    public abstract static class OpenGaussConstantBase extends OpenGaussConstant {

        @Override
        public String getUnquotedTextRepresentation() {
            return null;
        }

        @Override
        public OpenGaussConstant isEquals(OpenGaussConstant rightVal) {
            return null;
        }

        @Override
        protected OpenGaussConstant isLessThan(OpenGaussConstant rightVal) {
            return null;
        }

        @Override
        public OpenGaussConstant cast(OpenGaussDataType type) {
            return null;
        }
    }

    public static class DecimalConstant extends OpenGaussConstantBase {

        private final BigDecimal val;

        public DecimalConstant(BigDecimal val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(val);
        }

        @Override
        public OpenGaussDataType getExpressionType() {
            return OpenGaussDataType.DECIMAL;
        }

    }

    public static class InetConstant extends OpenGaussConstantBase {

        private final String val;

        public InetConstant(String val) {
            this.val = "'" + val + "'";
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(val);
        }

        @Override
        public OpenGaussDataType getExpressionType() {
            return OpenGaussDataType.INET;
        }

    }

    public static class FloatConstant extends OpenGaussConstantBase {

        private final float val;

        public FloatConstant(float val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            if (Double.isFinite(val)) {
                return String.valueOf(val);
            } else {
                return "'" + val + "'";
            }
        }

        @Override
        public OpenGaussDataType getExpressionType() {
            return OpenGaussDataType.FLOAT;
        }

    }

    public static class DoubleConstant extends OpenGaussConstantBase {

        private final double val;

        public DoubleConstant(double val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            if (Double.isFinite(val)) {
                return String.valueOf(val);
            } else {
                return "'" + val + "'";
            }
        }

        @Override
        public OpenGaussDataType getExpressionType() {
            return OpenGaussDataType.FLOAT;
        }

    }

    public static class BitConstant extends OpenGaussConstantBase {

        private final long val;

        public BitConstant(long val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return String.format("B'%s'", Long.toBinaryString(val));
        }

        @Override
        public OpenGaussDataType getExpressionType() {
            return OpenGaussDataType.BIT;
        }

    }


    public static class RangeConstant extends OpenGaussConstantBase {

        private final long left;
        private final boolean leftIsInclusive;
        private final long right;
        private final boolean rightIsInclusive;

        public RangeConstant(long left, boolean leftIsInclusive, long right, boolean rightIsInclusive) {
            this.left = left;
            this.leftIsInclusive = leftIsInclusive;
            this.right = right;
            this.rightIsInclusive = rightIsInclusive;
        }

        @Override
        public String getTextRepresentation() {
            StringBuilder sb = new StringBuilder();
            sb.append("'");
            if (leftIsInclusive) {
                sb.append("[");
            } else {
                sb.append("(");
            }
            sb.append(left);
            sb.append(",");
            sb.append(right);
            if (rightIsInclusive) {
                sb.append("]");
            } else {
                sb.append(")");
            }
            sb.append("'");
            sb.append("::int4range");
            return sb.toString();
        }

        @Override
        public OpenGaussDataType getExpressionType() {
            return OpenGaussDataType.RANGE;
        }

    }

    public static OpenGaussConstant createDecimalConstant(BigDecimal bigDecimal) {
        return new DecimalConstant(bigDecimal);
    }

    public static OpenGaussConstant createFloatConstant(float val) {
        return new FloatConstant(val);
    }

    public static OpenGaussConstant createDoubleConstant(double val) {
        return new DoubleConstant(val);
    }

    public static OpenGaussConstant createRange(long left, boolean leftIsInclusive, long right,
            boolean rightIsInclusive) {
        long realLeft;
        long realRight;
        if (left > right) {
            realRight = left;
            realLeft = right;
        } else {
            realLeft = left;
            realRight = right;
        }
        return new RangeConstant(realLeft, leftIsInclusive, realRight, rightIsInclusive);
    }

    public static OpenGaussExpression createBitConstant(long integer) {
        return new BitConstant(integer);
    }

    public static OpenGaussExpression createInetConstant(String val) {
        return new InetConstant(val);
    }

}
