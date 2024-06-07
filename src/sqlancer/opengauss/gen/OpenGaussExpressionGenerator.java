package sqlancer.opengauss.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.Randomly.StringGenerationStrategy;
import sqlancer.common.gen.ExpressionGenerator;
import sqlancer.opengauss.OpenGaussCompoundDataType;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussProvider;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussColumn;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussRowValue;
import sqlancer.opengauss.ast.OpenGaussAggregate;
import sqlancer.opengauss.ast.OpenGaussAggregate.OpenGaussAggregateFunction;
import sqlancer.opengauss.ast.OpenGaussBetweenOperation;
import sqlancer.opengauss.ast.OpenGaussBinaryArithmeticOperation;
import sqlancer.opengauss.ast.OpenGaussBinaryArithmeticOperation.OpenGaussBinaryOperator;
import sqlancer.opengauss.ast.OpenGaussBinaryBitOperation;
import sqlancer.opengauss.ast.OpenGaussBinaryBitOperation.OpenGaussBinaryBitOperator;
import sqlancer.opengauss.ast.OpenGaussBinaryComparisonOperation;
import sqlancer.opengauss.ast.OpenGaussBinaryLogicalOperation;
import sqlancer.opengauss.ast.OpenGaussBinaryLogicalOperation.BinaryLogicalOperator;
import sqlancer.opengauss.ast.OpenGaussBinaryRangeOperation;
import sqlancer.opengauss.ast.OpenGaussBinaryRangeOperation.OpenGaussBinaryRangeComparisonOperator;
import sqlancer.opengauss.ast.OpenGaussBinaryRangeOperation.OpenGaussBinaryRangeOperator;
import sqlancer.opengauss.ast.OpenGaussCastOperation;
import sqlancer.opengauss.ast.OpenGaussCollate;
import sqlancer.opengauss.ast.OpenGaussColumnValue;
import sqlancer.opengauss.ast.OpenGaussConcatOperation;
import sqlancer.opengauss.ast.OpenGaussConstant;
import sqlancer.opengauss.ast.OpenGaussExpression;
import sqlancer.opengauss.ast.OpenGaussFunction;
import sqlancer.opengauss.ast.OpenGaussFunction.OpenGaussFunctionWithResult;
import sqlancer.opengauss.ast.OpenGaussFunctionWithUnknownResult;
import sqlancer.opengauss.ast.OpenGaussInOperation;
import sqlancer.opengauss.ast.OpenGaussLikeOperation;
import sqlancer.opengauss.ast.OpenGaussOrderByTerm;
import sqlancer.opengauss.ast.OpenGaussOrderByTerm.OpenGaussOrder;
import sqlancer.opengauss.ast.OpenGaussPOSIXRegularExpression;
import sqlancer.opengauss.ast.OpenGaussPOSIXRegularExpression.POSIXRegex;
import sqlancer.opengauss.ast.OpenGaussPostfixOperation;
import sqlancer.opengauss.ast.OpenGaussPostfixOperation.PostfixOperator;
import sqlancer.opengauss.ast.OpenGaussPrefixOperation;
import sqlancer.opengauss.ast.OpenGaussPrefixOperation.PrefixOperator;
import sqlancer.opengauss.ast.OpenGaussSimilarTo;

public class OpenGaussExpressionGenerator implements ExpressionGenerator<OpenGaussExpression> {

    private final int maxDepth;

    private final Randomly r;

    private List<OpenGaussColumn> columns;

    private OpenGaussRowValue rw;

    private boolean expectedResult;

    private OpenGaussGlobalState globalState;

    private boolean allowAggregateFunctions;

    private final Map<String, Character> functionsAndTypes;

    private final List<Character> allowedFunctionTypes;

    public OpenGaussExpressionGenerator(OpenGaussGlobalState globalState) {
        this.r = globalState.getRandomly();
        this.maxDepth = globalState.getOptions().getMaxExpressionDepth();
        this.globalState = globalState;
        this.functionsAndTypes = globalState.getFunctionsAndTypes();
        this.allowedFunctionTypes = globalState.getAllowedFunctionTypes();
    }

    public OpenGaussExpressionGenerator setColumns(List<OpenGaussColumn> columns) {
        this.columns = columns;
        return this;
    }

    public OpenGaussExpressionGenerator setRowValue(OpenGaussRowValue rw) {
        this.rw = rw;
        return this;
    }

    public OpenGaussExpression generateExpression(int depth) {
        return generateExpression(depth, OpenGaussDataType.getRandomType());
    }

    public List<OpenGaussExpression> generateOrderBy() {
        List<OpenGaussExpression> orderBys = new ArrayList<>();
        for (int i = 0; i < Randomly.smallNumber(); i++) {
            orderBys.add(new OpenGaussOrderByTerm(OpenGaussColumnValue.create(Randomly.fromList(columns), null),
                    OpenGaussOrder.getRandomOrder()));
        }
        return orderBys;
    }

    private enum BooleanExpression {
        POSTFIX_OPERATOR, NOT, BINARY_LOGICAL_OPERATOR, BINARY_COMPARISON, FUNCTION, 
        CAST, 
        LIKE, BETWEEN, IN_OPERATION,
        SIMILAR_TO, POSIX_REGEX, BINARY_RANGE_COMPARISON;
    }

    private OpenGaussExpression generateFunctionWithUnknownResult(int depth, OpenGaussDataType type) {
        List<OpenGaussFunctionWithUnknownResult> supportedFunctions = OpenGaussFunctionWithUnknownResult
                .getSupportedFunctions(type);
        // filters functions by allowed type (STABLE 's', IMMUTABLE 'i', VOLATILE 'v')
        supportedFunctions = supportedFunctions.stream()
                .filter(f -> allowedFunctionTypes.contains(functionsAndTypes.get(f.getName())))
                .collect(Collectors.toList());
        if (supportedFunctions.isEmpty()) {
            throw new IgnoreMeException();
        }
        OpenGaussFunctionWithUnknownResult randomFunction = Randomly.fromList(supportedFunctions);
        return new OpenGaussFunction(randomFunction, type, randomFunction.getArguments(type, this, depth + 1));
    }

    private OpenGaussExpression generateFunctionWithKnownResult(int depth, OpenGaussDataType type) {
        List<OpenGaussFunctionWithResult> functions = Stream.of(OpenGaussFunction.OpenGaussFunctionWithResult.values())
                .filter(f -> f.supportsReturnType(type)).collect(Collectors.toList());
        // filters functions by allowed type (STABLE 's', IMMUTABLE 'i', VOLATILE 'v')
        functions = functions.stream().filter(f -> allowedFunctionTypes.contains(functionsAndTypes.get(f.getName())))
                .collect(Collectors.toList());
        if (functions.isEmpty()) {
            throw new IgnoreMeException();
        }
        OpenGaussFunctionWithResult randomFunction = Randomly.fromList(functions);
        int nrArgs = randomFunction.getNrArgs();
        if (randomFunction.isVariadic()) {
            nrArgs += Randomly.smallNumber();
        }
        OpenGaussDataType[] argTypes = randomFunction.getInputTypesForReturnType(type, nrArgs);
        OpenGaussExpression[] args = new OpenGaussExpression[nrArgs];
        do {
            for (int i = 0; i < args.length; i++) {
                args[i] = generateExpression(depth + 1, argTypes[i]);
            }
        } while (!randomFunction.checkArguments(args));
        return new OpenGaussFunction(randomFunction, type, args);
    }

    private OpenGaussExpression generateBooleanExpression(int depth) {
        List<BooleanExpression> validOptions = new ArrayList<>(Arrays.asList(BooleanExpression.values()));
        if (OpenGaussProvider.generateOnlyKnown) {
            validOptions.remove(BooleanExpression.SIMILAR_TO);
            validOptions.remove(BooleanExpression.POSIX_REGEX);
            validOptions.remove(BooleanExpression.BINARY_RANGE_COMPARISON);
        }
        BooleanExpression option = Randomly.fromList(validOptions);
        switch (option) {
        case POSTFIX_OPERATOR:
            PostfixOperator random = PostfixOperator.getRandom();
            return OpenGaussPostfixOperation
                    .create(generateExpression(depth + 1, Randomly.fromOptions(random.getInputDataTypes())), random);
        case IN_OPERATION:
            return inOperation(depth + 1);
        case NOT:
            return new OpenGaussPrefixOperation(generateExpression(depth + 1, OpenGaussDataType.BOOLEAN),
                    PrefixOperator.NOT);
        case BINARY_LOGICAL_OPERATOR:
            OpenGaussExpression first = generateExpression(depth + 1, OpenGaussDataType.BOOLEAN);
            int nr = Randomly.smallNumber() + 1;
            for (int i = 0; i < nr; i++) {
                first = new OpenGaussBinaryLogicalOperation(first,
                        generateExpression(depth + 1, OpenGaussDataType.BOOLEAN), BinaryLogicalOperator.getRandom());
            }
            return first;
        case BINARY_COMPARISON:
            OpenGaussDataType dataType = getMeaningfulType();
            return generateComparison(depth, dataType);
        case CAST:
            return new OpenGaussCastOperation(generateExpression(depth + 1),
                    getCompoundDataType(OpenGaussDataType.BOOLEAN));
        case FUNCTION:
            return generateFunction(depth + 1, OpenGaussDataType.BOOLEAN);
        case LIKE:
            return new OpenGaussLikeOperation(generateExpression(depth + 1, OpenGaussDataType.TEXT),
                    generateExpression(depth + 1, OpenGaussDataType.TEXT));
        case BETWEEN:
            OpenGaussDataType type = getMeaningfulType();
            return new OpenGaussBetweenOperation(generateExpression(depth + 1, type),
                    generateExpression(depth + 1, type), generateExpression(depth + 1, type), Randomly.getBoolean());
        case SIMILAR_TO:
            assert !expectedResult;
            // TODO also generate the escape character
            return new OpenGaussSimilarTo(generateExpression(depth + 1, OpenGaussDataType.TEXT),
                    generateExpression(depth + 1, OpenGaussDataType.TEXT), null);
        case POSIX_REGEX:
            assert !expectedResult;
            return new OpenGaussPOSIXRegularExpression(generateExpression(depth + 1, OpenGaussDataType.TEXT),
                    generateExpression(depth + 1, OpenGaussDataType.TEXT), POSIXRegex.getRandom());
        case BINARY_RANGE_COMPARISON:
            // TODO element check
            return new OpenGaussBinaryRangeOperation(OpenGaussBinaryRangeComparisonOperator.getRandom(),
                    generateExpression(depth + 1, OpenGaussDataType.RANGE),
                    generateExpression(depth + 1, OpenGaussDataType.RANGE));
        default:
            throw new AssertionError();
        }
    }

    private OpenGaussDataType getMeaningfulType() {
        // make it more likely that the expression does not only consist of constant
        // expressions
        if (Randomly.getBooleanWithSmallProbability() || columns == null || columns.isEmpty()) {
            return OpenGaussDataType.getRandomType();
        } else {
            return Randomly.fromList(columns).getType();
        }
    }

    private OpenGaussExpression generateFunction(int depth, OpenGaussDataType type) {
        if (OpenGaussProvider.generateOnlyKnown || Randomly.getBoolean()) {
            return generateFunctionWithKnownResult(depth, type);
        } else {
            return generateFunctionWithUnknownResult(depth, type);
        }
    }

    private OpenGaussExpression generateComparison(int depth, OpenGaussDataType dataType) {
        OpenGaussExpression leftExpr = generateExpression(depth + 1, dataType);
        OpenGaussExpression rightExpr = generateExpression(depth + 1, dataType);
        return getComparison(leftExpr, rightExpr);
    }

    private OpenGaussExpression getComparison(OpenGaussExpression leftExpr, OpenGaussExpression rightExpr) {
        OpenGaussBinaryComparisonOperation op = new OpenGaussBinaryComparisonOperation(leftExpr, rightExpr,
                OpenGaussBinaryComparisonOperation.OpenGaussBinaryComparisonOperator.getRandom());
        if (OpenGaussProvider.generateOnlyKnown && op.getLeft().getExpressionType() == OpenGaussDataType.TEXT
                && op.getRight().getExpressionType() == OpenGaussDataType.TEXT) {
            return new OpenGaussCollate(op, "C");
        }
        return op;
    }

    private OpenGaussExpression inOperation(int depth) {
        OpenGaussDataType type = OpenGaussDataType.getRandomType();
        OpenGaussExpression leftExpr = generateExpression(depth + 1, type);
        List<OpenGaussExpression> rightExpr = new ArrayList<>();
        for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
            rightExpr.add(generateExpression(depth + 1, type));
        }
        return new OpenGaussInOperation(leftExpr, rightExpr, Randomly.getBoolean());
    }

    public static OpenGaussExpression generateExpression(OpenGaussGlobalState globalState, OpenGaussDataType type) {
        return new OpenGaussExpressionGenerator(globalState).generateExpression(0, type);
    }

    public OpenGaussExpression generateExpression(int depth, OpenGaussDataType originalType) {
        OpenGaussDataType dataType = originalType;
        if (dataType == OpenGaussDataType.REAL && Randomly.getBoolean()) {
            dataType = Randomly.fromOptions(OpenGaussDataType.INT, OpenGaussDataType.FLOAT);
        }
        if (dataType == OpenGaussDataType.FLOAT && Randomly.getBoolean()) {
            dataType = OpenGaussDataType.INT;
        }
        if (!filterColumns(dataType).isEmpty() && Randomly.getBoolean()) {
            return potentiallyWrapInCollate(dataType, createColumnOfType(dataType));
        }
        OpenGaussExpression exprInternal = generateExpressionInternal(depth, dataType);
        return potentiallyWrapInCollate(dataType, exprInternal);
    }

    private OpenGaussExpression potentiallyWrapInCollate(OpenGaussDataType dataType, OpenGaussExpression exprInternal) {
        if (dataType == OpenGaussDataType.TEXT && OpenGaussProvider.generateOnlyKnown) {
            return new OpenGaussCollate(exprInternal, "C");
        } else {
            return exprInternal;
        }
    }

    private OpenGaussExpression generateExpressionInternal(int depth, OpenGaussDataType dataType) throws AssertionError {
        if (allowAggregateFunctions && Randomly.getBoolean()) {
            allowAggregateFunctions = false; // aggregate function calls cannot be nested
            return getAggregate(dataType);
        }
        if (Randomly.getBooleanWithRatherLowProbability() || depth > maxDepth) {
            // generic expression
            if (Randomly.getBoolean() || depth > maxDepth) {
                if (Randomly.getBooleanWithRatherLowProbability()) {
                    return generateConstant(r, dataType);
                } else {
                    if (filterColumns(dataType).isEmpty()) {
                        return generateConstant(r, dataType);
                    } else {
                        return createColumnOfType(dataType);
                    }
                }
            } else {
                if (Randomly.getBoolean()) {
//                    return new OpenGaussCastOperation(generateExpression(depth + 1), getCompoundDataType(dataType));
                    if(dataType == OpenGaussDataType.INT){
                        if(Randomly.getBoolean()){
                            StringGenerationStrategy strategy = Randomly.StringGenerationStrategy.NUMERIC;
                            //生成随机字符串
                            String s = strategy.getString(r);
//                            System.out.println(s);
                            //生成字符串的表达式
                            return new OpenGaussCastOperation(generateConstant(s), getCompoundDataType(dataType));
                        }
                        return new OpenGaussCastOperation(generateConstant(r,dataType), getCompoundDataType(OpenGaussDataType.INT));
                    }
                    else{
                        return new OpenGaussCastOperation(generateExpression(depth + 1), getCompoundDataType(dataType));
                    }
                } else {
                    return generateFunctionWithUnknownResult(depth, dataType);
                }
            }
        } else {
            switch (dataType) {
            case BOOLEAN:
                return generateBooleanExpression(depth);
            case INT:
                return generateIntExpression(depth);
            case TEXT:
                return generateTextExpression(depth);
            case DECIMAL:
            case REAL:
            case FLOAT:
            case MONEY:
            case INET:
                return generateConstant(r, dataType);
            case BIT:
                return generateBitExpression(depth);
            case RANGE:
                return generateRangeExpression(depth);
            default:
                throw new AssertionError(dataType);
            }
        }
    }

    private static OpenGaussCompoundDataType getCompoundDataType(OpenGaussDataType type) {
        switch (type) {
        case BOOLEAN:
        case DECIMAL: // TODO
        case FLOAT:
        case INT:
        case MONEY:
        case RANGE:
        case REAL:
        case INET:
            return OpenGaussCompoundDataType.create(type);
        case TEXT: // TODO
        case BIT:
            if (Randomly.getBoolean() || OpenGaussProvider.generateOnlyKnown /*
                                                                             * The PQS implementation does not check for
                                                                             * size specifications
                                                                             */) {
                return OpenGaussCompoundDataType.create(type);
            } else {
                return OpenGaussCompoundDataType.create(type, (int) Randomly.getNotCachedInteger(1, 1000));
            }
        default:
            throw new AssertionError(type);
        }

    }

    private enum RangeExpression {
        BINARY_OP;
    }

    private OpenGaussExpression generateRangeExpression(int depth) {
        RangeExpression option;
        List<RangeExpression> validOptions = new ArrayList<>(Arrays.asList(RangeExpression.values()));
        option = Randomly.fromList(validOptions);
        switch (option) {
        case BINARY_OP:
            return new OpenGaussBinaryRangeOperation(OpenGaussBinaryRangeOperator.getRandom(),
                    generateExpression(depth + 1, OpenGaussDataType.RANGE),
                    generateExpression(depth + 1, OpenGaussDataType.RANGE));
        default:
            throw new AssertionError(option);
        }
    }

    private enum TextExpression {
        CAST, FUNCTION, CONCAT, COLLATE
    }

    private OpenGaussExpression generateTextExpression(int depth) {
        TextExpression option;
        List<TextExpression> validOptions = new ArrayList<>(Arrays.asList(TextExpression.values()));
        if (expectedResult) {
            validOptions.remove(TextExpression.COLLATE);
        }
        if (!globalState.getDbmsSpecificOptions().testCollations) {
            validOptions.remove(TextExpression.COLLATE);
        }
        option = Randomly.fromList(validOptions);

        switch (option) {
        case CAST:
            return new OpenGaussCastOperation(generateExpression(depth + 1), getCompoundDataType(OpenGaussDataType.TEXT));
        case FUNCTION:
            return generateFunction(depth + 1, OpenGaussDataType.TEXT);
        case CONCAT:
            return generateConcat(depth);
        case COLLATE:
            assert !expectedResult;
            return new OpenGaussCollate(generateExpression(depth + 1, OpenGaussDataType.TEXT), globalState == null
                    ? Randomly.fromOptions("C", "POSIX", "de_CH.utf8", "es_CR.utf8") : globalState.getRandomCollate());
        default:
            throw new AssertionError();
        }
    }

    private OpenGaussExpression generateConcat(int depth) {
        OpenGaussExpression left = generateExpression(depth + 1, OpenGaussDataType.TEXT);
        OpenGaussExpression right = generateExpression(depth + 1);
        return new OpenGaussConcatOperation(left, right);
    }

    private enum BitExpression {
        BINARY_OPERATION
    };

    private OpenGaussExpression generateBitExpression(int depth) {
        BitExpression option;
        option = Randomly.fromOptions(BitExpression.values());
        switch (option) {
        case BINARY_OPERATION:
            return new OpenGaussBinaryBitOperation(OpenGaussBinaryBitOperator.getRandom(),
                    generateExpression(depth + 1, OpenGaussDataType.BIT),
                    generateExpression(depth + 1, OpenGaussDataType.BIT));
        default:
            throw new AssertionError();
        }
    }

    private enum IntExpression {
        UNARY_OPERATION, FUNCTION, 
        CAST, 
        BINARY_ARITHMETIC_EXPRESSION
    }

    private OpenGaussExpression generateIntExpression(int depth) {
        IntExpression option;
        option = Randomly.fromOptions(IntExpression.values());
        switch (option) {
        case CAST:
            return new OpenGaussCastOperation(generateExpression(depth + 1), getCompoundDataType(OpenGaussDataType.INT));
        case UNARY_OPERATION:
            OpenGaussExpression intExpression = generateExpression(depth + 1, OpenGaussDataType.INT);
            return new OpenGaussPrefixOperation(intExpression,
                    Randomly.getBoolean() ? PrefixOperator.UNARY_PLUS : PrefixOperator.UNARY_MINUS);
        case FUNCTION:
            return generateFunction(depth + 1, OpenGaussDataType.INT);
        case BINARY_ARITHMETIC_EXPRESSION:
            return new OpenGaussBinaryArithmeticOperation(generateExpression(depth + 1, OpenGaussDataType.INT),
                    generateExpression(depth + 1, OpenGaussDataType.INT), OpenGaussBinaryOperator.getRandom());
        default:
            throw new AssertionError();
        }
    }

    private OpenGaussExpression createColumnOfType(OpenGaussDataType type) {
        List<OpenGaussColumn> columns = filterColumns(type);
        OpenGaussColumn fromList = Randomly.fromList(columns);
        OpenGaussConstant value = rw == null ? null : rw.getValues().get(fromList);
        return OpenGaussColumnValue.create(fromList, value);
    }

    final List<OpenGaussColumn> filterColumns(OpenGaussDataType type) {
        if (columns == null) {
            return Collections.emptyList();
        } else {
            return columns.stream().filter(c -> c.getType() == type).collect(Collectors.toList());
        }
    }

    public OpenGaussExpression generateExpressionWithExpectedResult(OpenGaussDataType type) {
        this.expectedResult = true;
        OpenGaussExpressionGenerator gen = new OpenGaussExpressionGenerator(globalState).setColumns(columns)
                .setRowValue(rw);
        OpenGaussExpression expr;
        do {
            expr = gen.generateExpression(type);
        } while (expr.getExpectedValue() == null);
        return expr;
    }

    public static OpenGaussExpression generateConstant(Randomly r, OpenGaussDataType type) {
        if (Randomly.getBooleanWithSmallProbability()) {
            return OpenGaussConstant.createNullConstant();
        }
        // if (Randomly.getBooleanWithSmallProbability()) {
        // return OpenGaussConstant.createTextConstant(r.getString());
        // }
        switch (type) {
        case INT:
            if (Randomly.getBooleanWithSmallProbability()) {
                return OpenGaussConstant.createTextConstant(String.valueOf(r.getInteger()));
            } else {
                return OpenGaussConstant.createIntConstant(r.getInteger());
            }
        case BOOLEAN:
            if (Randomly.getBooleanWithSmallProbability() && !OpenGaussProvider.generateOnlyKnown) {
                return OpenGaussConstant
                        .createTextConstant(Randomly.fromOptions("TR", "TRUE", "FA", "FALSE", "0", "1", "ON", "off"));
            } else {
                return OpenGaussConstant.createBooleanConstant(Randomly.getBoolean());
            }
        case TEXT:
            return OpenGaussConstant.createTextConstant(r.getString());
        case DECIMAL:
            return OpenGaussConstant.createDecimalConstant(r.getRandomBigDecimal());
        case FLOAT:
            return OpenGaussConstant.createFloatConstant((float) r.getDouble());
        case REAL:
            return OpenGaussConstant.createDoubleConstant(r.getDouble());
        case RANGE:
            return OpenGaussConstant.createRange(r.getInteger(), Randomly.getBoolean(), r.getInteger(),
                    Randomly.getBoolean());
        case MONEY:
            return new OpenGaussCastOperation(generateConstant(r, OpenGaussDataType.FLOAT),
                    getCompoundDataType(OpenGaussDataType.MONEY));
        case INET:
            return OpenGaussConstant.createInetConstant(getRandomInet(r));
        case BIT:
            return OpenGaussConstant.createBitConstant(r.getInteger());
        default:
            throw new AssertionError(type);
        }
    }
    public static OpenGaussExpression generateConstant(String s){
        return OpenGaussConstant.createTextConstant(s);
    }
    private static String getRandomInet(Randomly r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i != 0) {
                sb.append('.');
            }
            sb.append(r.getInteger() & 255);
        }
        return sb.toString();
    }

    public static OpenGaussExpression generateExpression(OpenGaussGlobalState globalState, List<OpenGaussColumn> columns,
            OpenGaussDataType type) {
        return new OpenGaussExpressionGenerator(globalState).setColumns(columns).generateExpression(0, type);
    }

    public static OpenGaussExpression generateExpression(OpenGaussGlobalState globalState, List<OpenGaussColumn> columns) {
        return new OpenGaussExpressionGenerator(globalState).setColumns(columns).generateExpression(0);

    }

    public List<OpenGaussExpression> generateExpressions(int nr) {
        List<OpenGaussExpression> expressions = new ArrayList<>();
        for (int i = 0; i < nr; i++) {
            expressions.add(generateExpression(0));
        }
        return expressions;
    }

    public OpenGaussExpression generateExpression(OpenGaussDataType dataType) {
        return generateExpression(0, dataType);
    }

    public OpenGaussExpressionGenerator setGlobalState(OpenGaussGlobalState globalState) {
        this.globalState = globalState;
        return this;
    }

    public OpenGaussExpression generateHavingClause() {
        this.allowAggregateFunctions = true;
        OpenGaussExpression expression = generateExpression(OpenGaussDataType.BOOLEAN);
        this.allowAggregateFunctions = false;
        return expression;
    }

    public OpenGaussExpression generateAggregate() {
        return getAggregate(OpenGaussDataType.getRandomType());
    }

    private OpenGaussExpression getAggregate(OpenGaussDataType dataType) {
        List<OpenGaussAggregateFunction> aggregates = OpenGaussAggregateFunction.getAggregates(dataType);
        OpenGaussAggregateFunction agg = Randomly.fromList(aggregates);
        return generateArgsForAggregate(dataType, agg);
    }

    public OpenGaussAggregate generateArgsForAggregate(OpenGaussDataType dataType, OpenGaussAggregateFunction agg) {
        List<OpenGaussDataType> types = agg.getTypes(dataType);
        List<OpenGaussExpression> args = new ArrayList<>();
        for (OpenGaussDataType argType : types) {
            args.add(generateExpression(argType));
        }
        return new OpenGaussAggregate(args, agg);
    }

    public OpenGaussExpressionGenerator allowAggregates(boolean value) {
        allowAggregateFunctions = value;
        return this;
    }

    @Override
    public OpenGaussExpression generatePredicate() {
        return generateExpression(OpenGaussDataType.BOOLEAN);
    }

    @Override
    public OpenGaussExpression negatePredicate(OpenGaussExpression predicate) {
        return new OpenGaussPrefixOperation(predicate, OpenGaussPrefixOperation.PrefixOperator.NOT);
    }

    @Override
    public OpenGaussExpression isNull(OpenGaussExpression expr) {
        return new OpenGaussPostfixOperation(expr, PostfixOperator.IS_NULL);
    }

}
