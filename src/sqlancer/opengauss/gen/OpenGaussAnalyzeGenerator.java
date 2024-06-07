package sqlancer.opengauss.gen;

import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTable;

public final class OpenGaussAnalyzeGenerator {

    private OpenGaussAnalyzeGenerator() {
    }

    public static SQLQueryAdapter create(OpenGaussGlobalState globalState) {
        OpenGaussTable table = globalState.getSchema().getRandomTable();
        StringBuilder sb = new StringBuilder("ANALYZE");
        // if (Randomly.getBoolean()) {
        //     sb.append("(");
        //     if (Randomly.getBoolean()) {
        //         sb.append(" VERBOSE");
        //     } else {
        //         sb.append(" SKIP_LOCKED");
        //     }
        //     sb.append(")");
        // }
        if (Randomly.getBoolean()) {
            sb.append(" ");
            sb.append(table.getName());
            if (Randomly.getBoolean()) {
                sb.append("(");
                sb.append(table.getRandomNonEmptyColumnSubset().stream().map(c -> c.getName())
                        .collect(Collectors.joining(", ")));
                sb.append(")");
            }
        }
        // FIXME: bug in opengauss?
        return new SQLQueryAdapter(sb.toString(), ExpectedErrors.from("deadlock"));
    }

}
