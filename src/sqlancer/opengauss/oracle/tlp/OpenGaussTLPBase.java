package sqlancer.opengauss.oracle.tlp;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.gen.ExpressionGenerator;
import sqlancer.common.oracle.TernaryLogicPartitioningOracleBase;
import sqlancer.common.oracle.TestOracle;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussSchema;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussColumn;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTable;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTables;
import sqlancer.opengauss.ast.OpenGaussColumnValue;
import sqlancer.opengauss.ast.OpenGaussConstant;
import sqlancer.opengauss.ast.OpenGaussExpression;
import sqlancer.opengauss.ast.OpenGaussJoin;
import sqlancer.opengauss.ast.OpenGaussSelect;
import sqlancer.opengauss.ast.OpenGaussSelect.ForClause;
import sqlancer.opengauss.ast.OpenGaussSelect.OpenGaussFromTable;
import sqlancer.opengauss.ast.OpenGaussSelect.OpenGaussSubquery;
import sqlancer.opengauss.gen.OpenGaussCommon;
import sqlancer.opengauss.gen.OpenGaussExpressionGenerator;
import sqlancer.opengauss.oracle.OpenGaussNoRECOracle;

public class OpenGaussTLPBase extends TernaryLogicPartitioningOracleBase<OpenGaussExpression, OpenGaussGlobalState>
        implements TestOracle<OpenGaussGlobalState> {

    protected OpenGaussSchema s;
    protected OpenGaussTables targetTables;
    protected OpenGaussExpressionGenerator gen;
    protected OpenGaussSelect select;

    public OpenGaussTLPBase(OpenGaussGlobalState state) {
        super(state);
        OpenGaussCommon.addCommonExpressionErrors(errors);
        OpenGaussCommon.addCommonFetchErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        s = state.getSchema();
        targetTables = s.getRandomTableNonEmptyTables();
        List<OpenGaussTable> tables = targetTables.getTables();
        List<OpenGaussJoin> joins = getJoinStatements(state, targetTables.getColumns(), tables);
        generateSelectBase(tables, joins);
    }

    protected List<OpenGaussJoin> getJoinStatements(OpenGaussGlobalState globalState, List<OpenGaussColumn> columns,
            List<OpenGaussTable> tables) {
        return OpenGaussNoRECOracle.getJoinStatements(state, columns, tables);
        // TODO joins
    }

    protected void generateSelectBase(List<OpenGaussTable> tables, List<OpenGaussJoin> joins) {
        List<OpenGaussExpression> tableList = tables.stream().map(t -> new OpenGaussFromTable(t, Randomly.getBoolean()))
                .collect(Collectors.toList());
        gen = new OpenGaussExpressionGenerator(state).setColumns(targetTables.getColumns());
        initializeTernaryPredicateVariants();
        select = new OpenGaussSelect();
        select.setFetchColumns(generateFetchColumns());
        select.setFromList(tableList);
        select.setWhereClause(null);
        select.setJoinClauses(joins);
        if (Randomly.getBoolean()) {
            select.setForClause(ForClause.getRandom());
        }
    }

    List<OpenGaussExpression> generateFetchColumns() {
        if (Randomly.getBooleanWithRatherLowProbability()) {
            return Arrays.asList(new OpenGaussColumnValue(OpenGaussColumn.createDummy("*"), null));
        }
        List<OpenGaussExpression> fetchColumns = new ArrayList<>();
        List<OpenGaussColumn> targetColumns = Randomly.nonEmptySubset(targetTables.getColumns());
        for (OpenGaussColumn c : targetColumns) {
            fetchColumns.add(new OpenGaussColumnValue(c, null));
        }
        return fetchColumns;
    }

    @Override
    protected ExpressionGenerator<OpenGaussExpression> getGen() {
        return gen;
    }

    public static OpenGaussSubquery createSubquery(OpenGaussGlobalState globalState, String name, OpenGaussTables tables) {
        List<OpenGaussExpression> columns = new ArrayList<>();
        OpenGaussExpressionGenerator gen = new OpenGaussExpressionGenerator(globalState).setColumns(tables.getColumns());
        for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
            columns.add(gen.generateExpression(0));
        }
        OpenGaussSelect select = new OpenGaussSelect();
        select.setFromList(tables.getTables().stream().map(t -> new OpenGaussFromTable(t, Randomly.getBoolean()))
                .collect(Collectors.toList()));
        select.setFetchColumns(columns);
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression(0, OpenGaussDataType.BOOLEAN));
        }
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setOrderByExpressions(gen.generateOrderBy());
        }
        if (Randomly.getBoolean()) {
            select.setLimitClause(OpenGaussConstant.createIntConstant(Randomly.getPositiveOrZeroNonCachedInteger()));
            if (Randomly.getBoolean()) {
                select.setOffsetClause(
                        OpenGaussConstant.createIntConstant(Randomly.getPositiveOrZeroNonCachedInteger()));
            }
        }
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setForClause(ForClause.getRandom());
        }
        return new OpenGaussSubquery(select, name);
    }

}
