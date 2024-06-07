package sqlancer.opengauss;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import sqlancer.DBMSSpecificOptions;
import sqlancer.OracleFactory;
import sqlancer.common.oracle.CompositeTestOracle;
import sqlancer.common.oracle.TestOracle;
import sqlancer.opengauss.OpenGaussOptions.OpenGaussOracleFactory;
import sqlancer.opengauss.oracle.OpenGaussFuzzer;
import sqlancer.opengauss.oracle.OpenGaussNoRECOracle;
import sqlancer.opengauss.oracle.OpenGaussPivotedQuerySynthesisOracle;
import sqlancer.opengauss.oracle.tlp.OpenGaussTLPAggregateOracle;
import sqlancer.opengauss.oracle.tlp.OpenGaussTLPHavingOracle;
import sqlancer.opengauss.oracle.tlp.OpenGaussTLPWhereOracle;

@Parameters(separators = "=", commandDescription = "PostgreSQL (default port: " + OpenGaussOptions.DEFAULT_PORT
        + ", default host: " + OpenGaussOptions.DEFAULT_HOST + ")")
public class OpenGaussOptions implements DBMSSpecificOptions<OpenGaussOracleFactory> {
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 5432;

    @Parameter(names = "--bulk-insert", description = "Specifies whether INSERT statements should be issued in bulk", arity = 1)
    public boolean allowBulkInsert;

    @Parameter(names = "--oracle", description = "Specifies which test oracle should be used for PostgreSQL")
    public List<OpenGaussOracleFactory> oracle = Arrays.asList(OpenGaussOracleFactory.QUERY_PARTITIONING);

    @Parameter(names = "--test-collations", description = "Specifies whether to test different collations", arity = 1)
    public boolean testCollations = true;

    @Parameter(names = "--connection-url", description = "Specifies the URL for connecting to the PostgreSQL server", arity = 1)
    public String connectionURL = String.format("opengauss://%s:%d/test", OpenGaussOptions.DEFAULT_HOST,
            OpenGaussOptions.DEFAULT_PORT);

    @Parameter(names = "--extensions", description = "Specifies a comma-separated list of extension names to be created in each test database", arity = 1)
    public String extensions = "";

    public enum OpenGaussOracleFactory implements OracleFactory<OpenGaussGlobalState> {
        NOREC {
            @Override
            public TestOracle<OpenGaussGlobalState> create(OpenGaussGlobalState globalState) throws SQLException {
                return new OpenGaussNoRECOracle(globalState);
            }
        },
        PQS {
            @Override
            public TestOracle<OpenGaussGlobalState> create(OpenGaussGlobalState globalState) throws SQLException {
                return new OpenGaussPivotedQuerySynthesisOracle(globalState);
            }

            @Override
            public boolean requiresAllTablesToContainRows() {
                return true;
            }
        },
        HAVING {

            @Override
            public TestOracle<OpenGaussGlobalState> create(OpenGaussGlobalState globalState) throws SQLException {
                return new OpenGaussTLPHavingOracle(globalState);
            }

        },
        QUERY_PARTITIONING {
            @Override
            public TestOracle<OpenGaussGlobalState> create(OpenGaussGlobalState globalState) throws SQLException {
                List<TestOracle<OpenGaussGlobalState>> oracles = new ArrayList<>();
                oracles.add(new OpenGaussTLPWhereOracle(globalState));
                oracles.add(new OpenGaussTLPHavingOracle(globalState));
                oracles.add(new OpenGaussTLPAggregateOracle(globalState));
                return new CompositeTestOracle<OpenGaussGlobalState>(oracles, globalState);
            }
        },
        FUZZER {
            @Override
            public TestOracle<OpenGaussGlobalState> create(OpenGaussGlobalState globalState) throws Exception {
                return new OpenGaussFuzzer(globalState);
            }

        };

    }

    @Override
    public List<OpenGaussOracleFactory> getTestOracleFactory() {
        return oracle;
    }

}
