package sqlancer.opengauss.oracle.tlp;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.OpenGaussVisitor;
import sqlancer.opengauss.ast.OpenGaussExpression;
import sqlancer.opengauss.gen.OpenGaussCommon;

public class OpenGaussTLPHavingOracle extends OpenGaussTLPBase {

    public OpenGaussTLPHavingOracle(OpenGaussGlobalState state) {
        super(state);
        OpenGaussCommon.addGroupingErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        havingCheck();
    }

    protected void havingCheck() throws SQLException {
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression(OpenGaussDataType.BOOLEAN));
        }
        select.setGroupByExpressions(gen.generateExpressions(Randomly.smallNumber() + 1));
        select.setHavingClause(null);
        String originalQueryString = OpenGaussVisitor.asString(select);
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        boolean orderBy = Randomly.getBoolean();
        if (orderBy) {
            select.setOrderByExpressions(gen.generateOrderBy());
        }
        select.setHavingClause(predicate);
        String firstQueryString = OpenGaussVisitor.asString(select);
        select.setHavingClause(negatedPredicate);
        String secondQueryString = OpenGaussVisitor.asString(select);
        select.setHavingClause(isNullPredicate);
        String thirdQueryString = OpenGaussVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        List<String> secondResultSet = ComparatorHelper.getCombinedResultSet(firstQueryString, secondQueryString,
                thirdQueryString, combinedString, !orderBy, state, errors);
        ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                state);
    }

    @Override
    protected OpenGaussExpression generatePredicate() {
        return gen.generateHavingClause();
    }

    @Override
    List<OpenGaussExpression> generateFetchColumns() {
        List<OpenGaussExpression> expressions = gen.allowAggregates(true)
                .generateExpressions(Randomly.smallNumber() + 1);
        gen.allowAggregates(false);
        return expressions;
    }

}
