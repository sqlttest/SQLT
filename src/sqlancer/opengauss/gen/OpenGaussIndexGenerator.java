package sqlancer.opengauss.gen;

import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.DBMSCommon;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussColumn;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussIndex;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTable;
import sqlancer.opengauss.OpenGaussVisitor;
import sqlancer.opengauss.ast.OpenGaussExpression;

public final class OpenGaussIndexGenerator {

    private OpenGaussIndexGenerator() {
    }

    public enum IndexType {
        BTREE, HASH, GIST, GIN
    }

    public static SQLQueryAdapter generate(OpenGaussGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE");
        if (Randomly.getBoolean()) {
            sb.append(" UNIQUE");
        }
        sb.append(" INDEX ");
        /*
         * Commented out as a workaround for https://www.opengauss.org/message-id/CA%2Bu7OA4XYhc-
         * qyCgJqwwgMGZDWAyeH821oa5oMzm_HEifZ4BeA%40mail.gmail.com
         */
        // if (Randomly.getBoolean()) {
        // sb.append("CONCURRENTLY ");
        // }
        OpenGaussTable randomTable = globalState.getSchema().getRandomTable(t -> !t.isView()); // TODO: materialized
                                                                                              // views
        String indexName = getNewIndexName(randomTable);
        sb.append(indexName);
        sb.append(" ON ");
        // if (Randomly.getBoolean()) {
        //     sb.append("ONLY ");
        // }
        sb.append(randomTable.getName());
        IndexType method;
        if (Randomly.getBoolean()) {
            sb.append(" USING ");
            method = Randomly.fromOptions(IndexType.values());
            sb.append(method);
        } else {
            method = IndexType.BTREE;
        }

        sb.append("(");
        if (method == IndexType.HASH) {
            sb.append(randomTable.getRandomColumn().getName());
        } else {
            for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                if (Randomly.getBoolean()) {
                    sb.append(randomTable.getRandomColumn().getName());
                } else {
                    sb.append("(");
                    OpenGaussExpression expression = OpenGaussExpressionGenerator.generateExpression(globalState,
                            randomTable.getColumns());
                    sb.append(OpenGaussVisitor.asString(expression));
                    sb.append(")");
                }

                // if (Randomly.getBoolean()) {
                // sb.append(" ");
                // sb.append("COLLATE ");
                // sb.append(Randomly.fromOptions("C", "POSIX"));
                // }
                if (Randomly.getBooleanWithRatherLowProbability()) {
                    sb.append(" ");
                    sb.append(globalState.getRandomOpclass());
                    errors.add("does not accept");
                    errors.add("does not exist for access method");
                }
                if (Randomly.getBoolean()) {
                    sb.append(" ");
                    sb.append(Randomly.fromOptions("ASC", "DESC"));
                }
                if (Randomly.getBooleanWithRatherLowProbability()) {
                    sb.append(" NULLS ");
                    sb.append(Randomly.fromOptions("FIRST", "LAST"));
                }
            }
        }

        sb.append(")");
        // if (Randomly.getBoolean() && method != IndexType.HASH) {
        //     sb.append(" INCLUDE(");
        //     List<OpenGaussColumn> columns = randomTable.getRandomNonEmptyColumnSubset();
        //     sb.append(columns.stream().map(c -> c.getName()).collect(Collectors.joining(", ")));
        //     sb.append(")");
        // }
        if (Randomly.getBoolean()) {
            sb.append(" WHERE ");
            OpenGaussExpression expr = new OpenGaussExpressionGenerator(globalState).setColumns(randomTable.getColumns())
                    .setGlobalState(globalState).generateExpression(OpenGaussDataType.BOOLEAN);
            sb.append(OpenGaussVisitor.asString(expr));
        }
        errors.add("already contains data"); // CONCURRENT INDEX failed
        errors.add("You might need to add explicit type casts");
        errors.add(" collations are not supported"); // TODO check
        errors.add("because it has pending trigger events");
        errors.add("could not determine which collation to use for index expression");
        errors.add("could not determine which collation to use for string comparison");
        errors.add("is duplicated");
        errors.add("access method \"gin\" does not support unique indexes");
        errors.add("access method \"hash\" does not support unique indexes");
        errors.add("already exists");
        errors.add("could not create unique index");
        errors.add("has no default operator class");
        errors.add("does not support");
        errors.add("cannot cast");
        errors.add("unsupported UNIQUE constraint with partition key definition");
        errors.add("insufficient columns in UNIQUE constraint definition");
        errors.add("invalid input syntax for");
        errors.add("must be type ");
        errors.add("integer out of range");
        errors.add("division by zero");
        errors.add("out of range");
        errors.add("functions in index predicate must be marked IMMUTABLE");
        errors.add("functions in index expression must be marked IMMUTABLE");
        errors.add("result of range difference would not be contiguous");
        errors.add("which is part of the partition key");
        OpenGaussCommon.addCommonExpressionErrors(errors);
        return new SQLQueryAdapter(sb.toString(), errors);
    }

    private static String getNewIndexName(OpenGaussTable randomTable) {
        List<OpenGaussIndex> indexes = randomTable.getIndexes();
        int indexI = 0;
        while (true) {
            String indexName = DBMSCommon.createIndexName(indexI++);
            if (indexes.stream().noneMatch(i -> i.getIndexName().equals(indexName))) {
                return indexName;
            }
        }
    }

}
