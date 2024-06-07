package sqlancer.opengauss.oracle.tlp;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussVisitor;

public class OpenGaussTLPWhereOracle extends OpenGaussTLPBase {

    public OpenGaussTLPWhereOracle(OpenGaussGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        whereCheck();
    }

    protected void whereCheck() throws SQLException {
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setOrderByExpressions(gen.generateOrderBy());
        }
        String originalQueryString = OpenGaussVisitor.asString(select);
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        select.setOrderByExpressions(Collections.emptyList());
        select.setWhereClause(predicate);
        String firstQueryString = OpenGaussVisitor.asString(select);
        select.setWhereClause(negatedPredicate);
        String secondQueryString = OpenGaussVisitor.asString(select);
        select.setWhereClause(isNullPredicate);
        String thirdQueryString = OpenGaussVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        List<String> secondResultSet = ComparatorHelper.getCombinedResultSet(firstQueryString, secondQueryString,
                thirdQueryString, combinedString, Randomly.getBoolean(), state, errors);
        ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                state);
    }
}
