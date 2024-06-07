package sqlancer.opengauss.ast;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.ast.FunctionNode;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.ast.OpenGaussAggregate.OpenGaussAggregateFunction;

/**
 * @see <a href="https://www.sqlite.org/lang_aggfunc.html">Built-in Aggregate Functions</a>
 */
public class OpenGaussAggregate extends FunctionNode<OpenGaussAggregateFunction, OpenGaussExpression>
        implements OpenGaussExpression {

    public enum OpenGaussAggregateFunction {
        AVG(OpenGaussDataType.INT, OpenGaussDataType.FLOAT, OpenGaussDataType.REAL, OpenGaussDataType.DECIMAL),
        BIT_AND(OpenGaussDataType.INT), BIT_OR(OpenGaussDataType.INT), BOOL_AND(OpenGaussDataType.BOOLEAN),
        BOOL_OR(OpenGaussDataType.BOOLEAN), COUNT(OpenGaussDataType.INT), EVERY(OpenGaussDataType.BOOLEAN), MAX, MIN,
        // STRING_AGG
        SUM(OpenGaussDataType.INT, OpenGaussDataType.FLOAT, OpenGaussDataType.REAL, OpenGaussDataType.DECIMAL);

        private OpenGaussDataType[] supportedReturnTypes;

        OpenGaussAggregateFunction(OpenGaussDataType... supportedReturnTypes) {
            this.supportedReturnTypes = supportedReturnTypes.clone();
        }

        public List<OpenGaussDataType> getTypes(OpenGaussDataType returnType) {
            return Arrays.asList(returnType);
        }

        public boolean supportsReturnType(OpenGaussDataType returnType) {
            return Arrays.asList(supportedReturnTypes).stream().anyMatch(t -> t == returnType)
                    || supportedReturnTypes.length == 0;
        }

        public static List<OpenGaussAggregateFunction> getAggregates(OpenGaussDataType type) {
            return Arrays.asList(values()).stream().filter(p -> p.supportsReturnType(type))
                    .collect(Collectors.toList());
        }

        public OpenGaussDataType getRandomReturnType() {
            if (supportedReturnTypes.length == 0) {
                return Randomly.fromOptions(OpenGaussDataType.getRandomType());
            } else {
                return Randomly.fromOptions(supportedReturnTypes);
            }
        }

    }

    public OpenGaussAggregate(List<OpenGaussExpression> args, OpenGaussAggregateFunction func) {
        super(func, args);
    }

}
