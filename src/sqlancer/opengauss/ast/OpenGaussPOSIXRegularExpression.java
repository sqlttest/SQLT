package sqlancer.opengauss.ast;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussPOSIXRegularExpression implements OpenGaussExpression {

    private OpenGaussExpression string;
    private OpenGaussExpression regex;
    private POSIXRegex op;

    public enum POSIXRegex implements Operator {
        MATCH_CASE_SENSITIVE("~"), MATCH_CASE_INSENSITIVE("~*"), NOT_MATCH_CASE_SENSITIVE("!~"),
        NOT_MATCH_CASE_INSENSITIVE("!~*");

        private String repr;

        POSIXRegex(String repr) {
            this.repr = repr;
        }

        public String getStringRepresentation() {
            return repr;
        }

        public static POSIXRegex getRandom() {
            return Randomly.fromOptions(values());
        }

        @Override
        public String getTextRepresentation() {
            return toString();
        }
    }

    public OpenGaussPOSIXRegularExpression(OpenGaussExpression string, OpenGaussExpression regex, POSIXRegex op) {
        this.string = string;
        this.regex = regex;
        this.op = op;
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return OpenGaussDataType.BOOLEAN;
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        return null;
    }

    public OpenGaussExpression getRegex() {
        return regex;
    }

    public OpenGaussExpression getString() {
        return string;
    }

    public POSIXRegex getOp() {
        return op;
    }

}
