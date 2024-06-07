package sqlancer.opengauss.gen;

import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTable;
import sqlancer.opengauss.OpenGaussVisitor;

public final class OpenGaussDeleteGenerator {

    private OpenGaussDeleteGenerator() {
    }

    public static SQLQueryAdapter create(OpenGaussGlobalState globalState) {
        OpenGaussTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
        ExpectedErrors errors = new ExpectedErrors();
        errors.add("violates foreign key constraint");
        errors.add("violates not-null constraint");
        errors.add("could not determine which collation to use for string comparison");
        StringBuilder sb = new StringBuilder("DELETE FROM");
        // if (Randomly.getBoolean()) {
        //     sb.append(" ONLY");
        // }
        sb.append(" ");
        sb.append(table.getName());
        if (Randomly.getBoolean()) {
            sb.append(" WHERE ");
            sb.append(OpenGaussVisitor.asString(OpenGaussExpressionGenerator.generateExpression(globalState,
                    table.getColumns(), OpenGaussDataType.BOOLEAN)));
        }
        if (Randomly.getBoolean()) {
            sb.append(" RETURNING ");
            sb.append(OpenGaussVisitor
                    .asString(OpenGaussExpressionGenerator.generateExpression(globalState, table.getColumns())));
        }
        OpenGaussCommon.addCommonExpressionErrors(errors);
        errors.add("out of range");
        errors.add("cannot cast");
        errors.add("invalid input syntax for");
        errors.add("division by zero");
        return new SQLQueryAdapter(sb.toString(), errors);
    }

}
