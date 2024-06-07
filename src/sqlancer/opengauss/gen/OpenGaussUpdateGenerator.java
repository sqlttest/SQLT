package sqlancer.opengauss.gen;

import java.util.Arrays;
import java.util.List;

import sqlancer.Randomly;
import sqlancer.common.gen.AbstractUpdateGenerator;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussColumn;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTable;
import sqlancer.opengauss.OpenGaussVisitor;
import sqlancer.opengauss.ast.OpenGaussExpression;

public final class OpenGaussUpdateGenerator extends AbstractUpdateGenerator<OpenGaussColumn> {

    private final OpenGaussGlobalState globalState;
    private OpenGaussTable randomTable;

    private OpenGaussUpdateGenerator(OpenGaussGlobalState globalState) {
        this.globalState = globalState;
        errors.addAll(Arrays.asList("conflicting key value violates exclusion constraint",
                "reached maximum value of sequence", "violates foreign key constraint", "violates not-null constraint",
                "violates unique constraint", "out of range", "cannot cast", "must be type boolean", "is not unique",
                " bit string too long", "can only be updated to DEFAULT", "division by zero",
                "You might need to add explicit type casts.", "invalid regular expression",
                "View columns that are not columns of their base relation are not updatable"));
    }

    public static SQLQueryAdapter create(OpenGaussGlobalState globalState) {
        return new OpenGaussUpdateGenerator(globalState).generate();
    }

    private SQLQueryAdapter generate() {
        randomTable = globalState.getSchema().getRandomTable(t -> t.isInsertable());
        List<OpenGaussColumn> columns = randomTable.getRandomNonEmptyColumnSubset();
        sb.append("UPDATE ");
        sb.append(randomTable.getName());
        sb.append(" SET ");
        errors.add("multiple assignments to same column"); // view whose columns refer to a column in the referenced
                                                           // table multiple times
        errors.add("new row violates check option for view");
        OpenGaussCommon.addCommonInsertUpdateErrors(errors);
        updateColumns(columns);
        errors.add("invalid input syntax for ");
        errors.add("operator does not exist: text = boolean");
        errors.add("violates check constraint");
        errors.add("could not determine which collation to use for string comparison");
        errors.add("but expression is of type");
        OpenGaussCommon.addCommonExpressionErrors(errors);
        if (!Randomly.getBooleanWithSmallProbability()) {
            sb.append(" WHERE ");
            OpenGaussExpression where = OpenGaussExpressionGenerator.generateExpression(globalState,
                    randomTable.getColumns(), OpenGaussDataType.BOOLEAN);
            sb.append(OpenGaussVisitor.asString(where));
        }

        return new SQLQueryAdapter(sb.toString(), errors, true);
    }

    @Override
    protected void updateValue(OpenGaussColumn column) {
        if (!Randomly.getBoolean()) {
            OpenGaussExpression constant = OpenGaussExpressionGenerator.generateConstant(globalState.getRandomly(),
                    column.getType());
            sb.append(OpenGaussVisitor.asString(constant));
        } else if (Randomly.getBoolean()) {
            sb.append("DEFAULT");
        } else {
            sb.append("(");
            OpenGaussExpression expr = OpenGaussExpressionGenerator.generateExpression(globalState,
                    randomTable.getColumns(), column.getType());
            // caused by casts
            sb.append(OpenGaussVisitor.asString(expr));
            sb.append(")");
        }
    }

}
