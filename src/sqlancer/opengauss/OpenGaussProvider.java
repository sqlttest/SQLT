package sqlancer.opengauss;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import com.google.auto.service.AutoService;

import sqlancer.AbstractAction;
import sqlancer.DatabaseProvider;
import sqlancer.IgnoreMeException;
import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.SQLConnection;
import sqlancer.SQLProviderAdapter;
import sqlancer.StatementExecutor;
import sqlancer.common.DBMSCommon;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.common.query.SQLQueryProvider;
import sqlancer.common.query.SQLancerResultSet;
import sqlancer.opengauss.OpenGaussOptions.OpenGaussOracleFactory;
import sqlancer.opengauss.gen.OpenGaussAlterTableGenerator;
import sqlancer.opengauss.gen.OpenGaussAnalyzeGenerator;
import sqlancer.opengauss.gen.OpenGaussClusterGenerator;
import sqlancer.opengauss.gen.OpenGaussCommentGenerator;
import sqlancer.opengauss.gen.OpenGaussDeleteGenerator;
// import sqlancer.opengauss.gen.OpenGaussDiscardGenerator;
import sqlancer.opengauss.gen.OpenGaussDropIndexGenerator;
import sqlancer.opengauss.gen.OpenGaussIndexGenerator;
import sqlancer.opengauss.gen.OpenGaussInsertGenerator;
import sqlancer.opengauss.gen.OpenGaussNotifyGenerator;
import sqlancer.opengauss.gen.OpenGaussReindexGenerator;
import sqlancer.opengauss.gen.OpenGaussSequenceGenerator;
import sqlancer.opengauss.gen.OpenGaussSetGenerator;
import sqlancer.opengauss.gen.OpenGaussTableGenerator;
import sqlancer.opengauss.gen.OpenGaussTransactionGenerator;
import sqlancer.opengauss.gen.OpenGaussTruncateGenerator;
import sqlancer.opengauss.gen.OpenGaussUpdateGenerator;
import sqlancer.opengauss.gen.OpenGaussVacuumGenerator;
import sqlancer.opengauss.gen.OpenGaussViewGenerator;

// EXISTS
// IN
@AutoService(DatabaseProvider.class)
public class OpenGaussProvider extends SQLProviderAdapter<OpenGaussGlobalState, OpenGaussOptions> {

    /**
     * Generate only data types and expressions that are understood by PQS.
     */
    public static boolean generateOnlyKnown;

    protected String entryURL;
    protected String username;
    protected String password;
    protected String entryPath;
    protected String host;
    protected int port;
    protected String testURL;
    protected String databaseName;
    protected String createDatabaseCommand;
    protected String extensionsList;

    public OpenGaussProvider() {
        super(OpenGaussGlobalState.class, OpenGaussOptions.class);
    }

    protected OpenGaussProvider(Class<OpenGaussGlobalState> globalClass, Class<OpenGaussOptions> optionClass) {
        super(globalClass, optionClass);
    }

    public enum Action implements AbstractAction<OpenGaussGlobalState> {
        ANALYZE(OpenGaussAnalyzeGenerator::create), //
        ALTER_TABLE(g -> OpenGaussAlterTableGenerator.create(g.getSchema().getRandomTable(t -> !t.isView()), g,
                generateOnlyKnown)), //
        CLUSTER(OpenGaussClusterGenerator::create), //
        COMMIT(g -> {
            SQLQueryAdapter query;
            if (Randomly.getBoolean()) {
                query = new SQLQueryAdapter("COMMIT", true);
            } else if (Randomly.getBoolean()) {
                query = OpenGaussTransactionGenerator.executeBegin();
            } else {
                query = new SQLQueryAdapter("ROLLBACK", true);
            }
            return query;
        }), //
        DELETE(OpenGaussDeleteGenerator::create), //
        // DISCARD(OpenGaussDiscardGenerator::create), //
        DROP_INDEX(OpenGaussDropIndexGenerator::create), //
        INSERT(OpenGaussInsertGenerator::insert), //
        UPDATE(OpenGaussUpdateGenerator::create), //
        // TRUNCATE(OpenGaussTruncateGenerator::create), //
        // VACUUM(OpenGaussVacuumGenerator::create), //
        REINDEX(OpenGaussReindexGenerator::create), //
        SET(OpenGaussSetGenerator::create), //
        CREATE_INDEX(OpenGaussIndexGenerator::generate), //
        SET_CONSTRAINTS((g) -> {
            StringBuilder sb = new StringBuilder();
            sb.append("SET CONSTRAINTS ALL ");
            sb.append(Randomly.fromOptions("DEFERRED", "IMMEDIATE"));
            return new SQLQueryAdapter(sb.toString());
        }), //
        RESET_ROLE((g) -> new SQLQueryAdapter("RESET ROLE")), //
        COMMENT_ON(OpenGaussCommentGenerator::generate), //
        RESET((g) -> new SQLQueryAdapter("RESET ALL") /*
                                                       * https://www.opengauss.org/docs/devel/sql-reset.html TODO: also
                                                       * configuration parameter
                                                       */), //
        // NOTIFY(OpenGaussNotifyGenerator::createNotify), //
        // LISTEN((g) -> OpenGaussNotifyGenerator.createListen()), //
        // UNLISTEN((g) -> OpenGaussNotifyGenerator.createUnlisten()), //
        // CREATE_SEQUENCE(OpenGaussSequenceGenerator::createSequence), //
        CREATE_VIEW(OpenGaussViewGenerator::create);

        private final SQLQueryProvider<OpenGaussGlobalState> sqlQueryProvider;

        Action(SQLQueryProvider<OpenGaussGlobalState> sqlQueryProvider) {
            this.sqlQueryProvider = sqlQueryProvider;
        }

        @Override
        public SQLQueryAdapter getQuery(OpenGaussGlobalState state) throws Exception {
            return sqlQueryProvider.getQuery(state);
        }
    }

    protected static int mapActions(OpenGaussGlobalState globalState, Action a) {
        Randomly r = globalState.getRandomly();
        int nrPerformed;
        switch (a) {
        case CREATE_INDEX:
        case CLUSTER:
            nrPerformed = r.getInteger(0, 3);
            break;
        // case DISCARD:
        case DROP_INDEX:
            nrPerformed = r.getInteger(0, 5);
            break;
        case COMMIT:
            nrPerformed = r.getInteger(0, 0);
            break;
        case ALTER_TABLE:
            nrPerformed = r.getInteger(0, 5);
            break;
        case REINDEX:
        case RESET:
            nrPerformed = r.getInteger(0, 3);
            break;
        case DELETE:
        case RESET_ROLE:
        case SET:
            nrPerformed = r.getInteger(0, 5);
            break;
        case ANALYZE:
            nrPerformed = r.getInteger(0, 3);
            break;
        // case VACUUM:
        case SET_CONSTRAINTS:
        case COMMENT_ON:
        // case NOTIFY:
        // case LISTEN:
        // case UNLISTEN:
        // case CREATE_SEQUENCE:
        // case TRUNCATE:
        //     nrPerformed = r.getInteger(0, 2);
        //     break;
        case CREATE_VIEW:
            nrPerformed = r.getInteger(0, 2);
            break;
        case UPDATE:
            nrPerformed = r.getInteger(0, 10);
            break;
        case INSERT:
            nrPerformed = r.getInteger(0, globalState.getOptions().getMaxNumberInserts());
            break;
        default:
            throw new AssertionError(a);
        }
        return nrPerformed;

    }

    @Override
    public void generateDatabase(OpenGaussGlobalState globalState) throws Exception {
        readFunctions(globalState);
        createTables(globalState, Randomly.fromOptions(4, 5, 6));
        prepareTables(globalState);

        extensionsList = globalState.getDbmsSpecificOptions().extensions;
        if (!extensionsList.isEmpty()) {
            String[] extensionNames = extensionsList.split(",");

            /*
             * To avoid of a test interference with an extension objects, create them in a separate schema. Of course,
             * they must be truly relocatable.
             */
            globalState.executeStatement(new SQLQueryAdapter("CREATE SCHEMA extensions;", true));
            for (int i = 0; i < extensionNames.length; i++) {
                globalState.executeStatement(new SQLQueryAdapter(
                        "CREATE EXTENSION " + extensionNames[i] + " WITH SCHEMA extensions;", true));
            }
        }
    }

    @Override
    public SQLConnection createDatabase(OpenGaussGlobalState globalState) throws SQLException {
        if (globalState.getDbmsSpecificOptions().getTestOracleFactory().stream()
                .anyMatch((o) -> o == OpenGaussOracleFactory.PQS)) {
            generateOnlyKnown = true;
        }

        username = globalState.getOptions().getUserName();
        password = globalState.getOptions().getPassword();
        host = globalState.getOptions().getHost();
        port = globalState.getOptions().getPort();
        entryPath = "/test";
        entryURL = globalState.getDbmsSpecificOptions().connectionURL;
        // trim URL to exclude "jdbc:"
        if (entryURL.startsWith("jdbc:")) {
            entryURL = entryURL.substring(5);
        }
        String entryDatabaseName = entryPath.substring(1);
        databaseName = globalState.getDatabaseName();

        try {
            URI uri = new URI(entryURL);
            String userInfoURI = uri.getUserInfo();
            String pathURI = uri.getPath();
            if (userInfoURI != null) {
                // username and password specified in URL take precedence
                if (userInfoURI.contains(":")) {
                    String[] userInfo = userInfoURI.split(":", 2);
                    username = userInfo[0];
                    password = userInfo[1];
                } else {
                    username = userInfoURI;
                    password = null;
                }
                int userInfoIndex = entryURL.indexOf(userInfoURI);
                String preUserInfo = entryURL.substring(0, userInfoIndex);
                String postUserInfo = entryURL.substring(userInfoIndex + userInfoURI.length() + 1);
                entryURL = preUserInfo + postUserInfo;
            }
            if (pathURI != null) {
                entryPath = pathURI;
            }
            if (host == null) {
                host = uri.getHost();
            }
            if (port == MainOptions.NO_SET_PORT) {
                port = uri.getPort();
            }
            entryURL = String.format("%s://%s:%d/%s", "postgresql", host, port, entryDatabaseName);

//            System.out.println(uri.getScheme());
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }

        Connection con = DriverManager.getConnection("jdbc:" + entryURL, username, password);
        globalState.getState().logStatement(String.format("\\c %s;", entryDatabaseName));
        globalState.getState().logStatement("DROP DATABASE IF EXISTS " + databaseName);
        createDatabaseCommand = getCreateDatabaseCommand(globalState);
        globalState.getState().logStatement(createDatabaseCommand);
        try (Statement s = con.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + databaseName);
        }
        try (Statement s = con.createStatement()) {
            s.execute(createDatabaseCommand);
        }
        con.close();
        int databaseIndex = entryURL.indexOf(entryDatabaseName);
        String preDatabaseName = entryURL.substring(0, databaseIndex);
        String postDatabaseName = entryURL.substring(databaseIndex + entryDatabaseName.length());
        testURL = preDatabaseName + databaseName + postDatabaseName;

        globalState.getState().logStatement(String.format("\\c %s;", databaseName));

        con = DriverManager.getConnection("jdbc:" + testURL, username, password);
        return new SQLConnection(con);
    }

    protected void readFunctions(OpenGaussGlobalState globalState) throws SQLException {
        SQLQueryAdapter query = new SQLQueryAdapter("SELECT proname, provolatile FROM pg_proc;");
        SQLancerResultSet rs = query.executeAndGet(globalState);
        while (rs.next()) {
            String functionName = rs.getString(1);
            Character functionType = rs.getString(2).charAt(0);
            globalState.addFunctionAndType(functionName, functionType);
        }
    }

    protected void createTables(OpenGaussGlobalState globalState, int numTables) throws Exception {
        while (globalState.getSchema().getDatabaseTables().size() < numTables) {
            try {
                String tableName = DBMSCommon.createTableName(globalState.getSchema().getDatabaseTables().size());
                SQLQueryAdapter createTable = OpenGaussTableGenerator.generate(tableName, globalState.getSchema(),
                        generateOnlyKnown, globalState);
                globalState.executeStatement(createTable);
            } catch (IgnoreMeException e) {

            }
        }
    }

    protected void prepareTables(OpenGaussGlobalState globalState) throws Exception {
        StatementExecutor<OpenGaussGlobalState, Action> se = new StatementExecutor<>(globalState, Action.values(),
                OpenGaussProvider::mapActions, (q) -> {
                    if (globalState.getSchema().getDatabaseTables().isEmpty()) {
                        throw new IgnoreMeException();
                    }
                });
        se.executeStatements();
        globalState.executeStatement(new SQLQueryAdapter("COMMIT", true));
        globalState.executeStatement(new SQLQueryAdapter("SET SESSION statement_timeout = 5000;\n"));
    }

    private String getCreateDatabaseCommand(OpenGaussGlobalState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE DATABASE " + databaseName+ " ");
        if (Randomly.getBoolean() && ((OpenGaussOptions) state.getDbmsSpecificOptions()).testCollations) {
            if (Randomly.getBoolean()) {
                sb.append("WITH ENCODING '");
                sb.append(Randomly.fromOptions("utf8"));
                sb.append("' ");
            }
            for (String lc : Arrays.asList("LC_COLLATE", "LC_CTYPE")) {
                if (!state.getCollates().isEmpty() && Randomly.getBoolean()) {
                    sb.append(String.format(" %s = '%s'", lc, Randomly.fromList(state.getCollates())));
                }
            }
            // sb.append(" TEMPLATE template0");
        }
        sb.append(" dbcompatibility 'PG' ");
        return sb.toString();
    }

    @Override
    public String getDBMSName() {
        return "opengauss";
    }

}
