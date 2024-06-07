package sqlancer.opengauss.oracle.tlp;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.postgresql.util.PSQLException;

import sqlancer.ComparatorHelper;
import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.common.query.SQLancerResultSet;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.OpenGaussVisitor;
import sqlancer.opengauss.ast.OpenGaussAggregate;
import sqlancer.opengauss.ast.OpenGaussAggregate.OpenGaussAggregateFunction;
import sqlancer.opengauss.ast.OpenGaussAlias;
import sqlancer.opengauss.ast.OpenGaussExpression;
import sqlancer.opengauss.ast.OpenGaussJoin;
import sqlancer.opengauss.ast.OpenGaussPostfixOperation;
import sqlancer.opengauss.ast.OpenGaussPostfixOperation.PostfixOperator;
import sqlancer.opengauss.ast.OpenGaussPrefixOperation;
import sqlancer.opengauss.ast.OpenGaussPrefixOperation.PrefixOperator;
import sqlancer.opengauss.ast.OpenGaussSelect;
import sqlancer.opengauss.gen.OpenGaussCommon;

public class OpenGaussTLPAggregateOracle extends OpenGaussTLPBase implements TestOracle<OpenGaussGlobalState> {

    private String firstResult;
    private String secondResult;
    private String originalQuery;
    private String metamorphicQuery;

    public OpenGaussTLPAggregateOracle(OpenGaussGlobalState state) {
        super(state);
        OpenGaussCommon.addGroupingErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        aggregateCheck();
    }

    protected void aggregateCheck() throws SQLException {
        OpenGaussAggregateFunction aggregateFunction = Randomly.fromOptions(OpenGaussAggregateFunction.MAX,
                OpenGaussAggregateFunction.MIN, OpenGaussAggregateFunction.SUM, OpenGaussAggregateFunction.BIT_AND,
                OpenGaussAggregateFunction.BIT_OR, OpenGaussAggregateFunction.BOOL_AND, OpenGaussAggregateFunction.BOOL_OR,
                OpenGaussAggregateFunction.COUNT);
        OpenGaussAggregate aggregate = gen.generateArgsForAggregate(aggregateFunction.getRandomReturnType(),
                aggregateFunction);
        List<OpenGaussExpression> fetchColumns = new ArrayList<>();
        fetchColumns.add(aggregate);
        while (Randomly.getBooleanWithRatherLowProbability()) {
            fetchColumns.add(gen.generateAggregate());
        }
        select.setFetchColumns(Arrays.asList(aggregate));
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setOrderByExpressions(gen.generateOrderBy());
        }
        originalQuery = OpenGaussVisitor.asString(select);
        firstResult = getAggregateResult(originalQuery);
        metamorphicQuery = createMetamorphicUnionQuery(select, aggregate, select.getFromList());
        secondResult = getAggregateResult(metamorphicQuery);

        String queryFormatString = "-- %s;\n-- result: %s";
        String firstQueryString = String.format(queryFormatString, originalQuery, firstResult);
        String secondQueryString = String.format(queryFormatString, metamorphicQuery, secondResult);
        state.getState().getLocalState().log(String.format("%s\n%s", firstQueryString, secondQueryString));
        if (firstResult == null && secondResult != null || firstResult != null && secondResult == null
                || firstResult != null && !firstResult.contentEquals(secondResult)
                        && !ComparatorHelper.isEqualDouble(firstResult, secondResult)) {
            if (secondResult != null && secondResult.contains("Inf")) {
                throw new IgnoreMeException(); // FIXME: average computation
            }
            String assertionMessage = String.format("the results mismatch!\n%s\n%s", firstQueryString,
                    secondQueryString);
            throw new AssertionError(assertionMessage);
        }
    }

    private String createMetamorphicUnionQuery(OpenGaussSelect select, OpenGaussAggregate aggregate,
            List<OpenGaussExpression> from) {
        String metamorphicQuery;
        OpenGaussExpression whereClause = gen.generateExpression(OpenGaussDataType.BOOLEAN);
        OpenGaussExpression negatedClause = new OpenGaussPrefixOperation(whereClause, PrefixOperator.NOT);
        OpenGaussExpression notNullClause = new OpenGaussPostfixOperation(whereClause, PostfixOperator.IS_NULL);
        List<OpenGaussExpression> mappedAggregate = mapped(aggregate);
        OpenGaussSelect leftSelect = getSelect(mappedAggregate, from, whereClause, select.getJoinClauses());
        OpenGaussSelect middleSelect = getSelect(mappedAggregate, from, negatedClause, select.getJoinClauses());
        OpenGaussSelect rightSelect = getSelect(mappedAggregate, from, notNullClause, select.getJoinClauses());
        metamorphicQuery = "SELECT " + getOuterAggregateFunction(aggregate) + " FROM (";
        metamorphicQuery += OpenGaussVisitor.asString(leftSelect) + " UNION ALL "
                + OpenGaussVisitor.asString(middleSelect) + " UNION ALL " + OpenGaussVisitor.asString(rightSelect);
        metamorphicQuery += ") as asdf";
        return metamorphicQuery;
    }

    private String getAggregateResult(String queryString) throws SQLException {
        // log TLP Aggregate SELECT queries on the current log file
        if (state.getOptions().logEachSelect()) {
            // TODO: refactor me
            state.getLogger().writeCurrent(queryString);
            try {
                state.getLogger().getCurrentFileWriter().flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        String resultString;
        SQLQueryAdapter q = new SQLQueryAdapter(queryString, errors);
        try (SQLancerResultSet result = q.executeAndGet(state)) {
            if (result == null) {
                throw new IgnoreMeException();
            }
            if (!result.next()) {
                resultString = null;
            } else {
                resultString = result.getString(1);
            }
        } catch (PSQLException e) {
            throw new AssertionError(queryString, e);
        }
        return resultString;
    }

    private List<OpenGaussExpression> mapped(OpenGaussAggregate aggregate) {
        switch (aggregate.getFunction()) {
        case SUM:
        case COUNT:
        case BIT_AND:
        case BIT_OR:
        case BOOL_AND:
        case BOOL_OR:
        case MAX:
        case MIN:
            return aliasArgs(Arrays.asList(aggregate));
        // case AVG:
        //// List<OpenGaussExpression> arg = Arrays.asList(new
        // OpenGaussCast(aggregate.getExpr().get(0),
        // OpenGaussDataType.DECIMAL.get()));
        // OpenGaussAggregate sum = new OpenGaussAggregate(OpenGaussAggregateFunction.SUM,
        // aggregate.getExpr());
        // OpenGaussCast count = new OpenGaussCast(
        // new OpenGaussAggregate(OpenGaussAggregateFunction.COUNT, aggregate.getExpr()),
        // OpenGaussDataType.DECIMAL.get());
        //// OpenGaussBinaryArithmeticOperation avg = new
        // OpenGaussBinaryArithmeticOperation(sum, count,
        // OpenGaussBinaryArithmeticOperator.DIV);
        // return aliasArgs(Arrays.asList(sum, count));
        default:
            throw new AssertionError(aggregate.getFunction());
        }
    }

    private List<OpenGaussExpression> aliasArgs(List<OpenGaussExpression> originalAggregateArgs) {
        List<OpenGaussExpression> args = new ArrayList<>();
        int i = 0;
        for (OpenGaussExpression expr : originalAggregateArgs) {
            args.add(new OpenGaussAlias(expr, "agg" + i++));
        }
        return args;
    }

    private String getOuterAggregateFunction(OpenGaussAggregate aggregate) {
        switch (aggregate.getFunction()) {
        // case AVG:
        // return "SUM(agg0::DECIMAL)/SUM(agg1)::DECIMAL";
        case COUNT:
            return OpenGaussAggregateFunction.SUM.toString() + "(agg0)";
        default:
            return aggregate.getFunction().toString() + "(agg0)";
        }
    }

    private OpenGaussSelect getSelect(List<OpenGaussExpression> aggregates, List<OpenGaussExpression> from,
            OpenGaussExpression whereClause, List<OpenGaussJoin> joinList) {
        OpenGaussSelect leftSelect = new OpenGaussSelect();
        leftSelect.setFetchColumns(aggregates);
        leftSelect.setFromList(from);
        leftSelect.setWhereClause(whereClause);
        leftSelect.setJoinClauses(joinList);
        if (Randomly.getBooleanWithSmallProbability()) {
            leftSelect.setGroupByExpressions(gen.generateExpressions(Randomly.smallNumber() + 1));
        }
        return leftSelect;
    }

}
