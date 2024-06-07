package sqlancer.opengauss.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.DBMSCommon;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.opengauss.OpenGaussGlobalState;
import sqlancer.opengauss.OpenGaussSchema;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussColumn;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTable;
import sqlancer.opengauss.OpenGaussVisitor;
import sqlancer.opengauss.ast.OpenGaussExpression;

public class OpenGaussTableGenerator {

    private final String tableName;
    private boolean columnCanHavePrimaryKey;
    private boolean columnHasPrimaryKey;
    private final StringBuilder sb = new StringBuilder();
    // private boolean isTemporaryTable;
    private boolean isPartitionedTable;
    private final OpenGaussSchema newSchema;
    private final List<OpenGaussColumn> columnsToBeAdded = new ArrayList<>();
    protected final ExpectedErrors errors = new ExpectedErrors();
    private final OpenGaussTable table;
    private final boolean generateOnlyKnown;
    private final OpenGaussGlobalState globalState;

    public OpenGaussTableGenerator(String tableName, OpenGaussSchema newSchema, boolean generateOnlyKnown,
            OpenGaussGlobalState globalState) {
        this.tableName = tableName;
        this.newSchema = newSchema;
        this.generateOnlyKnown = generateOnlyKnown;
        this.globalState = globalState;
        table = new OpenGaussTable(tableName, columnsToBeAdded, null,  null, false, false);
        errors.add("invalid input syntax for");
        errors.add("is not unique");
        errors.add("integer out of range");
        errors.add("division by zero");
        errors.add("cannot create partitioned table as inheritance child");
        errors.add("cannot cast");
        errors.add("ERROR: functions in index expression must be marked IMMUTABLE");
        errors.add("functions in partition key expression must be marked IMMUTABLE");
        errors.add("functions in index predicate must be marked IMMUTABLE");
        errors.add("has no default operator class for access method");
        errors.add("does not exist for access method");
        errors.add("does not accept data type");
        errors.add("but default expression is of type text");
        errors.add("has pseudo-type unknown");
        errors.add("no collation was derived for partition key column");
        errors.add("inherits from generated column but specifies identity");
        errors.add("inherits from generated column but specifies default");
        OpenGaussCommon.addCommonExpressionErrors(errors);
        OpenGaussCommon.addCommonTableErrors(errors);
    }

    public static SQLQueryAdapter generate(String tableName, OpenGaussSchema newSchema, boolean generateOnlyKnown,
            OpenGaussGlobalState globalState) {
        return new OpenGaussTableGenerator(tableName, newSchema, generateOnlyKnown, globalState).generate();
    }

    protected SQLQueryAdapter generate() {
        columnCanHavePrimaryKey = true;
        sb.append("CREATE");
        if (Randomly.getBoolean()) {
        //     sb.append(" ");
        //     isTemporaryTable = true;
        //     sb.append(Randomly.fromOptions("TEMPORARY", "TEMP"));
        } else if (Randomly.getBoolean()) {
            sb.append(" UNLOGGED");
        }
        sb.append(" TABLE");
        if (Randomly.getBoolean()) {
            sb.append(" IF NOT EXISTS");
        }
        sb.append(" ");
        sb.append(tableName);
        if (Randomly.getBoolean() && !newSchema.getDatabaseTables().isEmpty()) {
            createLike();
        } else {
            createStandard();
        }
        return new SQLQueryAdapter(sb.toString(), errors, true);
    }

    private void createStandard() throws AssertionError {
        sb.append("(");
        for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            String name = DBMSCommon.createColumnName(i);
            createColumn(name);
        }
        if (Randomly.getBoolean()) {
            errors.add("constraints on temporary tables may reference only temporary tables");
            errors.add("constraints on unlogged tables may reference only permanent or unlogged tables");
            errors.add("constraints on permanent tables may reference only permanent tables");
            errors.add("cannot be implemented");
            errors.add("there is no unique constraint matching given keys for referenced table");
            errors.add("cannot reference partitioned table");
            errors.add("unsupported ON COMMIT and foreign key combination");
            errors.add("ERROR: invalid ON DELETE action for foreign key constraint containing generated column");
            errors.add("exclusion constraints are not supported on partitioned tables");
            OpenGaussCommon.addTableConstraints(columnHasPrimaryKey, sb, table, globalState, errors);
        }
        sb.append(")");
        // generateInherits();
        // generateUsing();
        OpenGaussCommon.generateWith(sb, globalState, errors);
        // if (Randomly.getBoolean() && isTemporaryTable) {
        //     sb.append(" ON COMMIT ");
        //     sb.append(Randomly.fromOptions("PRESERVE ROWS", "DELETE ROWS", "DROP"));
        //     sb.append(" ");
        // }
    }

    private void createLike() {
        sb.append("(");
        sb.append("LIKE ");
        sb.append(newSchema.getRandomTable().getName());
        // if (Randomly.getBoolean()) {
        //     for (int i = 0; i < Randomly.smallNumber(); i++) {
        //         String option = Randomly.fromOptions("DEFAULTS", "CONSTRAINTS", "INDEXES", "STORAGE", "COMMENTS",
        //                 "GENERATED", "STORAGE", "ALL");
        //         sb.append(" ");
        //         sb.append(Randomly.fromOptions("INCLUDING", "EXCLUDING"));
        //         sb.append(" ");
        //         sb.append(option);
        //     }
        // }
        sb.append(")");
    }

    private void createColumn(String name) throws AssertionError {
        sb.append(name);
        sb.append(" ");
        OpenGaussDataType type = OpenGaussDataType.getRandomType();
        boolean serial = OpenGaussCommon.appendDataType(type, sb, true, generateOnlyKnown, globalState.getCollates());
        OpenGaussColumn c = new OpenGaussColumn(name, type);
        c.setTable(table);
        columnsToBeAdded.add(c);
        sb.append(" ");
        if (Randomly.getBoolean()) {
            createColumnConstraint(type, serial);
        }
    }

    

    private void generateUsing() {
        /*
         * OpenGauss does not allow specifying USING clause for partitioned tables since they don't have any storage
         * associated with them
         */
        if (isPartitionedTable) {
            return;
        }
        if (Randomly.getBoolean()) {
            return;
        }
        sb.append(" USING ");
        sb.append(globalState.getRandomTableAccessMethod());
    }

    private void generateInherits() {
        if (Randomly.getBoolean() && !newSchema.getDatabaseTables().isEmpty()) {
            sb.append(" INHERITS(");
            sb.append(newSchema.getDatabaseTablesRandomSubsetNotEmpty().stream().map(t -> t.getName())
                    .collect(Collectors.joining(", ")));
            sb.append(")");
            errors.add("has a type conflict");
            errors.add("has a generation conflict");
            errors.add("cannot create partitioned table as inheritance child");
            errors.add("cannot inherit from temporary relation");
            errors.add("cannot inherit from partitioned table");
            errors.add("has a collation conflict");
            errors.add("inherits conflicting default values");
            errors.add("specifies generation expression");
        }
    }

    private enum ColumnConstraint {
        NULL_OR_NOT_NULL, UNIQUE, PRIMARY_KEY, DEFAULT, CHECK, GENERATED
    };

    private void createColumnConstraint(OpenGaussDataType type, boolean serial) {
        List<ColumnConstraint> constraintSubset = Randomly.nonEmptySubset(ColumnConstraint.values());
        if (Randomly.getBoolean()) {
            // make checks constraints less likely
            constraintSubset.remove(ColumnConstraint.CHECK);
        }
        if (!columnCanHavePrimaryKey || columnHasPrimaryKey) {
            constraintSubset.remove(ColumnConstraint.PRIMARY_KEY);
        }
        if (constraintSubset.contains(ColumnConstraint.GENERATED)
                && constraintSubset.contains(ColumnConstraint.DEFAULT)) {
            // otherwise: ERROR: both default and identity specified for column
            constraintSubset.remove(Randomly.fromOptions(ColumnConstraint.GENERATED, ColumnConstraint.DEFAULT));
        }
        if (constraintSubset.contains(ColumnConstraint.GENERATED) && type != OpenGaussDataType.INT) {
            // otherwise: ERROR: identity column type must be smallint, integer, or bigint
            constraintSubset.remove(ColumnConstraint.GENERATED);
        }
        if (serial) {
            constraintSubset.remove(ColumnConstraint.GENERATED);
            constraintSubset.remove(ColumnConstraint.DEFAULT);
            constraintSubset.remove(ColumnConstraint.NULL_OR_NOT_NULL);

        }
        for (ColumnConstraint c : constraintSubset) {
            sb.append(" ");
            switch (c) {
            case NULL_OR_NOT_NULL:
                sb.append(Randomly.fromOptions("NOT NULL", "NULL"));
                errors.add("conflicting NULL/NOT NULL declarations");
                break;
            case UNIQUE:
                sb.append("UNIQUE");
                break;
            case PRIMARY_KEY:
                sb.append("PRIMARY KEY");
                columnHasPrimaryKey = true;
                break;
            case DEFAULT:
                sb.append("DEFAULT");
                sb.append(" (");
                sb.append(OpenGaussVisitor.asString(OpenGaussExpressionGenerator.generateExpression(globalState, type)));
                sb.append(")");
                // CREATE TEMPORARY TABLE t1(c0 smallint DEFAULT ('566963878'));
                errors.add("out of range");
                errors.add("is a generated column");
                break;
            case CHECK:
                sb.append("CHECK (");
                sb.append(OpenGaussVisitor.asString(OpenGaussExpressionGenerator.generateExpression(globalState,
                        columnsToBeAdded, OpenGaussDataType.BOOLEAN)));
                sb.append(")");
                if (Randomly.getBoolean()) {
                    sb.append(" NO INHERIT");
                }
                errors.add("out of range");
                break;
            case GENERATED:
                sb.append("GENERATED ");
                sb.append(" ALWAYS AS (");
                sb.append(OpenGaussVisitor.asString(
                        OpenGaussExpressionGenerator.generateExpression(globalState, columnsToBeAdded, type)));
                sb.append(") STORED");
                errors.add("A generated column cannot reference another generated column.");
                errors.add("cannot use generated column in partition key");
                errors.add("generation expression is not immutable");
                errors.add("cannot use column reference in DEFAULT expression");

                break;
            default:
                throw new AssertionError(sb);
            }
        }
    }

}
