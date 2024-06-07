package sqlancer.opengauss.oracle;

import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussVisitor;
import sqlancer.opengauss.gen.OpenGaussRandomQueryGenerator;

public class OpenGaussFuzzer implements TestOracle<OpenGaussGlobalState> {

    private final OpenGaussGlobalState globalState;

    public OpenGaussFuzzer(OpenGaussGlobalState globalState) {
        this.globalState = globalState;
    }

    @Override
    public void check() throws Exception {
        String s = OpenGaussVisitor.asString(
                OpenGaussRandomQueryGenerator.createRandomQuery(Randomly.smallNumber() + 1, globalState)) + ';';
        try {
            globalState.executeStatement(new SQLQueryAdapter(s));
            globalState.getManager().incrementSelectQueryCount();
        } catch (Error e) {

        }
    }

}
