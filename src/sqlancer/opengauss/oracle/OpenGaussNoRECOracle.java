package sqlancer.opengauss.oracle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.oracle.NoRECBase;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.common.query.SQLancerResultSet;
import sqlancer.opengauss.OpenGaussCompoundDataType;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussSchema;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussColumn;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTable;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTables;
import sqlancer.opengauss.OpenGaussVisitor;
import sqlancer.opengauss.ast.OpenGaussCastOperation;
import sqlancer.opengauss.ast.OpenGaussColumnValue;
import sqlancer.opengauss.ast.OpenGaussExpression;
import sqlancer.opengauss.ast.OpenGaussJoin;
import sqlancer.opengauss.ast.OpenGaussJoin.OpenGaussJoinType;
import sqlancer.opengauss.ast.OpenGaussPostfixText;
import sqlancer.opengauss.ast.OpenGaussSelect;
import sqlancer.opengauss.ast.OpenGaussSelect.OpenGaussFromTable;
import sqlancer.opengauss.ast.OpenGaussSelect.OpenGaussSubquery;
import sqlancer.opengauss.ast.OpenGaussSelect.SelectType;
import sqlancer.opengauss.gen.OpenGaussCommon;
import sqlancer.opengauss.gen.OpenGaussExpressionGenerator;
import sqlancer.opengauss.oracle.tlp.OpenGaussTLPBase;

public class OpenGaussNoRECOracle extends NoRECBase<OpenGaussGlobalState> implements TestOracle<OpenGaussGlobalState> {

    private final OpenGaussSchema s;

    public OpenGaussNoRECOracle(OpenGaussGlobalState globalState) {
        super(globalState);
        this.s = globalState.getSchema();
        OpenGaussCommon.addCommonExpressionErrors(errors);
        OpenGaussCommon.addCommonFetchErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        OpenGaussTables randomTables = s.getRandomTableNonEmptyTables();
        List<OpenGaussColumn> columns = randomTables.getColumns();
        OpenGaussExpression randomWhereCondition = getRandomWhereCondition(columns);
        List<OpenGaussTable> tables = randomTables.getTables();

        List<OpenGaussJoin> joinStatements = getJoinStatements(state, columns, tables);
        List<OpenGaussExpression> fromTables = tables.stream().map(t -> new OpenGaussFromTable(t, Randomly.getBoolean()))
                .collect(Collectors.toList());
        int secondCount = getUnoptimizedQueryCount(fromTables, randomWhereCondition, joinStatements);
        int firstCount = getOptimizedQueryCount(fromTables, columns, randomWhereCondition, joinStatements);
        if (firstCount == -1 || secondCount == -1) {
            throw new IgnoreMeException();
        }
        if (firstCount != secondCount) {
            String queryFormatString = "-- %s;\n-- count: %d";
            String firstQueryStringWithCount = String.format(queryFormatString, optimizedQueryString, firstCount);
            String secondQueryStringWithCount = String.format(queryFormatString, unoptimizedQueryString, secondCount);
            state.getState().getLocalState()
                    .log(String.format("%s\n%s", firstQueryStringWithCount, secondQueryStringWithCount));
            String assertionMessage = String.format("the counts mismatch (%d and %d)!\n%s\n%s", firstCount, secondCount,
                    firstQueryStringWithCount, secondQueryStringWithCount);
            throw new AssertionError(assertionMessage);
        }
    }

    public static List<OpenGaussJoin> getJoinStatements(OpenGaussGlobalState globalState, List<OpenGaussColumn> columns,
            List<OpenGaussTable> tables) {
        List<OpenGaussJoin> joinStatements = new ArrayList<>();
        OpenGaussExpressionGenerator gen = new OpenGaussExpressionGenerator(globalState).setColumns(columns);
        for (int i = 1; i < tables.size(); i++) {
            OpenGaussExpression joinClause = gen.generateExpression(OpenGaussDataType.BOOLEAN);
            OpenGaussTable table = Randomly.fromList(tables);
            tables.remove(table);
            OpenGaussJoinType options = OpenGaussJoinType.getRandom();
            OpenGaussJoin j = new OpenGaussJoin(new OpenGaussFromTable(table, Randomly.getBoolean()), joinClause, options);
            joinStatements.add(j);
        }
        // JOIN subqueries
        for (int i = 0; i < Randomly.smallNumber(); i++) {
            OpenGaussTables subqueryTables = globalState.getSchema().getRandomTableNonEmptyTables();
            OpenGaussSubquery subquery = OpenGaussTLPBase.createSubquery(globalState, String.format("sub%d", i),
                    subqueryTables);
            OpenGaussExpression joinClause = gen.generateExpression(OpenGaussDataType.BOOLEAN);
            OpenGaussJoinType options = OpenGaussJoinType.getRandom();
            OpenGaussJoin j = new OpenGaussJoin(subquery, joinClause, options);
            joinStatements.add(j);
        }
        return joinStatements;
    }

    private OpenGaussExpression getRandomWhereCondition(List<OpenGaussColumn> columns) {
        return new OpenGaussExpressionGenerator(state).setColumns(columns).generateExpression(OpenGaussDataType.BOOLEAN);
    }

    private int getUnoptimizedQueryCount(List<OpenGaussExpression> fromTables, OpenGaussExpression randomWhereCondition,
            List<OpenGaussJoin> joinStatements) throws SQLException {
        OpenGaussSelect select = new OpenGaussSelect();
        OpenGaussCastOperation isTrue = new OpenGaussCastOperation(randomWhereCondition,
                OpenGaussCompoundDataType.create(OpenGaussDataType.INT));
        OpenGaussPostfixText asText = new OpenGaussPostfixText(isTrue, " as count", null, OpenGaussDataType.INT);
        select.setFetchColumns(Arrays.asList(asText));
        select.setFromList(fromTables);
        select.setSelectType(SelectType.ALL);
        select.setJoinClauses(joinStatements);
        int secondCount = 0;
        unoptimizedQueryString = "SELECT SUM(count) FROM (" + OpenGaussVisitor.asString(select) + ") as res";
        if (options.logEachSelect()) {
            logger.writeCurrent(unoptimizedQueryString);
        }
        errors.add("canceling statement due to statement timeout");
        SQLQueryAdapter q = new SQLQueryAdapter(unoptimizedQueryString, errors);
        SQLancerResultSet rs;
        try {
            rs = q.executeAndGet(state);
        } catch (Exception e) {
            throw new AssertionError(unoptimizedQueryString, e);
        }
        if (rs == null) {
            return -1;
        }
        if (rs.next()) {
            secondCount += rs.getLong(1);
        }
        rs.close();
        return secondCount;
    }

    private int getOptimizedQueryCount(List<OpenGaussExpression> randomTables, List<OpenGaussColumn> columns,
            OpenGaussExpression randomWhereCondition, List<OpenGaussJoin> joinStatements) throws SQLException {
        OpenGaussSelect select = new OpenGaussSelect();
        OpenGaussColumnValue allColumns = new OpenGaussColumnValue(Randomly.fromList(columns), null);
        select.setFetchColumns(Arrays.asList(allColumns));
        select.setFromList(randomTables);
        select.setWhereClause(randomWhereCondition);
        if (Randomly.getBooleanWithSmallProbability()) {
            select.setOrderByExpressions(new OpenGaussExpressionGenerator(state).setColumns(columns).generateOrderBy());
        }
        select.setSelectType(SelectType.ALL);
        select.setJoinClauses(joinStatements);
        int firstCount = 0;
        try (Statement stat = con.createStatement()) {
            optimizedQueryString = OpenGaussVisitor.asString(select);
            if (options.logEachSelect()) {
                logger.writeCurrent(optimizedQueryString);
            }
            try (ResultSet rs = stat.executeQuery(optimizedQueryString)) {
                while (rs.next()) {
                    firstCount++;
                }
            }
        } catch (SQLException e) {
            throw new IgnoreMeException();
        }
        return firstCount;
    }

}
