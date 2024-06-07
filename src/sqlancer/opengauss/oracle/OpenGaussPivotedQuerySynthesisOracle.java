package sqlancer.opengauss.oracle;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.SQLConnection;
import sqlancer.common.oracle.PivotedQuerySynthesisBase;
import sqlancer.common.query.Query;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussColumn;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussRowValue;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTables;
import sqlancer.opengauss.OpenGaussVisitor;
import sqlancer.opengauss.ast.OpenGaussColumnValue;
import sqlancer.opengauss.ast.OpenGaussConstant;
import sqlancer.opengauss.ast.OpenGaussExpression;
import sqlancer.opengauss.ast.OpenGaussPostfixOperation;
import sqlancer.opengauss.ast.OpenGaussPostfixOperation.PostfixOperator;
import sqlancer.opengauss.ast.OpenGaussSelect;
import sqlancer.opengauss.ast.OpenGaussSelect.OpenGaussFromTable;
import sqlancer.opengauss.gen.OpenGaussCommon;
import sqlancer.opengauss.gen.OpenGaussExpressionGenerator;

public class OpenGaussPivotedQuerySynthesisOracle
        extends PivotedQuerySynthesisBase<OpenGaussGlobalState, OpenGaussRowValue, OpenGaussExpression, SQLConnection> {

    private List<OpenGaussColumn> fetchColumns;

    public OpenGaussPivotedQuerySynthesisOracle(OpenGaussGlobalState globalState) throws SQLException {
        super(globalState);
        OpenGaussCommon.addCommonExpressionErrors(errors);
        OpenGaussCommon.addCommonFetchErrors(errors);
    }

    @Override
    public SQLQueryAdapter getRectifiedQuery() throws SQLException {
        OpenGaussTables randomFromTables = globalState.getSchema().getRandomTableNonEmptyTables();

        OpenGaussSelect selectStatement = new OpenGaussSelect();
        selectStatement.setSelectType(Randomly.fromOptions(OpenGaussSelect.SelectType.values()));
        List<OpenGaussColumn> columns = randomFromTables.getColumns();
        pivotRow = randomFromTables.getRandomRowValue(globalState.getConnection());

        fetchColumns = columns;
        selectStatement.setFromList(randomFromTables.getTables().stream().map(t -> new OpenGaussFromTable(t, false))
                .collect(Collectors.toList()));
        selectStatement.setFetchColumns(fetchColumns.stream()
                .map(c -> new OpenGaussColumnValue(getFetchValueAliasedColumn(c), pivotRow.getValues().get(c)))
                .collect(Collectors.toList()));
        OpenGaussExpression whereClause = generateRectifiedExpression(columns, pivotRow);
        selectStatement.setWhereClause(whereClause);
        List<OpenGaussExpression> groupByClause = generateGroupByClause(columns, pivotRow);
        selectStatement.setGroupByExpressions(groupByClause);
        OpenGaussExpression limitClause = generateLimit();
        selectStatement.setLimitClause(limitClause);
        if (limitClause != null) {
            OpenGaussExpression offsetClause = generateOffset();
            selectStatement.setOffsetClause(offsetClause);
        }
        List<OpenGaussExpression> orderBy = new OpenGaussExpressionGenerator(globalState).setColumns(columns)
                .generateOrderBy();
        selectStatement.setOrderByExpressions(orderBy);
        return new SQLQueryAdapter(OpenGaussVisitor.asString(selectStatement));
    }

    /*
     * Prevent name collisions by aliasing the column.
     */
    private OpenGaussColumn getFetchValueAliasedColumn(OpenGaussColumn c) {
        OpenGaussColumn aliasedColumn = new OpenGaussColumn(c.getName() + " AS " + c.getTable().getName() + c.getName(),
                c.getType());
        aliasedColumn.setTable(c.getTable());
        return aliasedColumn;
    }

    private List<OpenGaussExpression> generateGroupByClause(List<OpenGaussColumn> columns, OpenGaussRowValue rw) {
        if (Randomly.getBoolean()) {
            return columns.stream().map(c -> OpenGaussColumnValue.create(c, rw.getValues().get(c)))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private OpenGaussConstant generateLimit() {
        if (Randomly.getBoolean()) {
            return OpenGaussConstant.createIntConstant(Integer.MAX_VALUE);
        } else {
            return null;
        }
    }

    private OpenGaussExpression generateOffset() {
        if (Randomly.getBoolean()) {
            return OpenGaussConstant.createIntConstant(0);
        } else {
            return null;
        }
    }

    private OpenGaussExpression generateRectifiedExpression(List<OpenGaussColumn> columns, OpenGaussRowValue rw) {
        OpenGaussExpression expr = new OpenGaussExpressionGenerator(globalState).setColumns(columns).setRowValue(rw)
                .generateExpressionWithExpectedResult(OpenGaussDataType.BOOLEAN);
        OpenGaussExpression result;
        if (expr.getExpectedValue().isNull()) {
            result = OpenGaussPostfixOperation.create(expr, PostfixOperator.IS_NULL);
        } else {
            result = OpenGaussPostfixOperation.create(expr,
                    expr.getExpectedValue().cast(OpenGaussDataType.BOOLEAN).asBoolean() ? PostfixOperator.IS_TRUE
                            : PostfixOperator.IS_FALSE);
        }
        rectifiedPredicates.add(result);
        return result;
    }

    @Override
    protected Query<SQLConnection> getContainmentCheckQuery(Query<?> query) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ("); // ANOTHER SELECT TO USE ORDER BY without restrictions
        sb.append(query.getUnterminatedQueryString());
        sb.append(") as result WHERE ");
        int i = 0;
        for (OpenGaussColumn c : fetchColumns) {
            if (i++ != 0) {
                sb.append(" AND ");
            }
            sb.append("result.");
            sb.append(c.getTable().getName());
            sb.append(c.getName());
            if (pivotRow.getValues().get(c).isNull()) {
                sb.append(" IS NULL");
            } else {
                sb.append(" = ");
                sb.append(pivotRow.getValues().get(c).getTextRepresentation());
            }
        }
        String resultingQueryString = sb.toString();
        return new SQLQueryAdapter(resultingQueryString, errors);
    }

    @Override
    protected String getExpectedValues(OpenGaussExpression expr) {
        return OpenGaussVisitor.asExpectedValues(expr);
    }

}
