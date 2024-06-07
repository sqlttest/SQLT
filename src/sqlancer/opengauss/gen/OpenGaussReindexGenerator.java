package sqlancer.opengauss.gen;

import java.util.List;
import java.util.stream.Collectors;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussIndex;

public final class OpenGaussReindexGenerator {

    private OpenGaussReindexGenerator() {
    }

    private enum Scope {
        INDEX, TABLE, DATABASE;
    }

    public static SQLQueryAdapter create(OpenGaussGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        errors.add("could not create unique index"); // CONCURRENT INDEX
        StringBuilder sb = new StringBuilder();
        sb.append("REINDEX");
        // if (Randomly.getBoolean()) {
        // sb.append(" VERBOSE");
        // }
        sb.append(" ");
        Scope scope = Randomly.fromOptions(Scope.values());
        switch (scope) {
        case INDEX:
            sb.append("INDEX ");
            if (Randomly.getBoolean()) {
                sb.append("CONCURRENTLY ");
            }
            List<OpenGaussIndex> indexes = globalState.getSchema().getRandomTable().getIndexes();
            if (indexes.isEmpty()) {
                throw new IgnoreMeException();
            }
            sb.append(indexes.stream().map(i -> i.getIndexName()).collect(Collectors.joining()));
            break;
        case TABLE:
            sb.append("TABLE ");
            if (Randomly.getBoolean()) {
                sb.append("CONCURRENTLY ");
            }
            sb.append(globalState.getSchema().getRandomTable(t -> !t.isView()).getName());
            break;
        case DATABASE:
            sb.append("DATABASE ");
            if (Randomly.getBoolean()) {
                sb.append("CONCURRENTLY ");
            }
            sb.append(globalState.getSchema().getDatabaseName());
            break;
        default:
            throw new AssertionError(scope);
        }
        errors.add("already contains data"); // FIXME bug report
        errors.add("does not exist"); // internal index
        errors.add("REINDEX is not yet implemented for partitioned indexes");
        return new SQLQueryAdapter(sb.toString(), errors);
    }

}
