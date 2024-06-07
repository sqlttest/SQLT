package sqlancer.opengauss.gen;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTable;

/**
 * @see <a href="https://www.opengauss.org/docs/devel/sql-comment.html">COMMENT</a>
 */
public final class OpenGaussCommentGenerator {

    private OpenGaussCommentGenerator() {
    }

    private enum Action {
        INDEX, COLUMN, TABLE
    }

    public static SQLQueryAdapter generate(OpenGaussGlobalState globalState) {
        StringBuilder sb = new StringBuilder();
        sb.append("COMMENT ON ");
        Action type = Randomly.fromOptions(Action.values());
        OpenGaussTable randomTable = globalState.getSchema().getRandomTable();
        switch (type) {
        case INDEX:
            sb.append("INDEX ");
            if (randomTable.getIndexes().isEmpty()) {
                throw new IgnoreMeException();
            } else {
                sb.append(randomTable.getRandomIndex().getIndexName());
            }
            break;
        case COLUMN:
            sb.append("COLUMN ");
            sb.append(randomTable.getRandomColumn().getFullQualifiedName());
            break;
        case TABLE:
            sb.append("TABLE ");
            if (randomTable.isView()) {
                throw new IgnoreMeException();
            }
            sb.append(randomTable.getName());
            break;
        default:
            throw new AssertionError(type);
        }
        sb.append(" IS ");
        if (Randomly.getBoolean()) {
            sb.append("NULL");
        } else {
            sb.append("'");
            sb.append(globalState.getRandomly().getString().replace("'", "''"));
            sb.append("'");
        }
        return new SQLQueryAdapter(sb.toString());
    }

}
