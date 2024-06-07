package sqlancer.opengauss;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.postgresql.util.PSQLException;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.SQLConnection;
import sqlancer.common.DBMSCommon;
import sqlancer.common.schema.AbstractRelationalTable;
import sqlancer.common.schema.AbstractRowValue;
import sqlancer.common.schema.AbstractSchema;
import sqlancer.common.schema.AbstractTableColumn;
import sqlancer.common.schema.AbstractTables;
import sqlancer.common.schema.TableIndex;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTable;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTable.TableType;
import sqlancer.opengauss.ast.OpenGaussConstant;

public class OpenGaussSchema extends AbstractSchema<OpenGaussGlobalState, OpenGaussTable> {

    private final String databaseName;

    public enum OpenGaussDataType {
        INT, BOOLEAN, TEXT, DECIMAL, FLOAT, REAL, RANGE, MONEY, BIT, INET;

        public static OpenGaussDataType getRandomType() {
            List<OpenGaussDataType> dataTypes = new ArrayList<>(Arrays.asList(values()));
            if (OpenGaussProvider.generateOnlyKnown) {
                dataTypes.remove(OpenGaussDataType.DECIMAL);
                dataTypes.remove(OpenGaussDataType.FLOAT);
                dataTypes.remove(OpenGaussDataType.REAL);
                dataTypes.remove(OpenGaussDataType.INET);
                dataTypes.remove(OpenGaussDataType.RANGE);
                dataTypes.remove(OpenGaussDataType.MONEY);
                dataTypes.remove(OpenGaussDataType.BIT);
            }
            return Randomly.fromList(dataTypes);
        }
    }

    public static class OpenGaussColumn extends AbstractTableColumn<OpenGaussTable, OpenGaussDataType> {

        public OpenGaussColumn(String name, OpenGaussDataType columnType) {
            super(name, null, columnType);
        }

        public static OpenGaussColumn createDummy(String name) {
            return new OpenGaussColumn(name, OpenGaussDataType.INT);
        }

    }

    public static class OpenGaussTables extends AbstractTables<OpenGaussTable, OpenGaussColumn> {

        public OpenGaussTables(List<OpenGaussTable> tables) {
            super(tables);
        }

        public OpenGaussRowValue getRandomRowValue(SQLConnection con) throws SQLException {
            String randomRow = String.format("SELECT %s FROM %s ORDER BY RANDOM() LIMIT 1", columnNamesAsString(
                    c -> c.getTable().getName() + "." + c.getName() + " AS " + c.getTable().getName() + c.getName()),
                    // columnNamesAsString(c -> "typeof(" + c.getTable().getName() + "." +
                    // c.getName() + ")")
                    tableNamesAsString());
            Map<OpenGaussColumn, OpenGaussConstant> values = new HashMap<>();
            try (Statement s = con.createStatement()) {
                ResultSet randomRowValues = s.executeQuery(randomRow);
                if (!randomRowValues.next()) {
                    throw new AssertionError("could not find random row! " + randomRow + "\n");
                }
                for (int i = 0; i < getColumns().size(); i++) {
                    OpenGaussColumn column = getColumns().get(i);
                    int columnIndex = randomRowValues.findColumn(column.getTable().getName() + column.getName());
                    assert columnIndex == i + 1;
                    OpenGaussConstant constant;
                    if (randomRowValues.getString(columnIndex) == null) {
                        constant = OpenGaussConstant.createNullConstant();
                    } else {
                        switch (column.getType()) {
                        case INT:
                            constant = OpenGaussConstant.createIntConstant(randomRowValues.getLong(columnIndex));
                            break;
                        case BOOLEAN:
                            constant = OpenGaussConstant.createBooleanConstant(randomRowValues.getBoolean(columnIndex));
                            break;
                        case TEXT:
                            constant = OpenGaussConstant.createTextConstant(randomRowValues.getString(columnIndex));
                            break;
                        default:
                            throw new IgnoreMeException();
                        }
                    }
                    values.put(column, constant);
                }
                assert !randomRowValues.next();
                return new OpenGaussRowValue(this, values);
            } catch (PSQLException e) {
                throw new IgnoreMeException();
            }

        }

    }

    public static OpenGaussDataType getColumnType(String typeString) {
        switch (typeString) {
        case "smallint":
        case "integer":
        case "bigint":
            return OpenGaussDataType.INT;
        case "boolean":
            return OpenGaussDataType.BOOLEAN;
        case "text":
        case "character":
        case "character varying":
        case "name":
        case "regclass":
            return OpenGaussDataType.TEXT;
        case "numeric":
            return OpenGaussDataType.DECIMAL;
        case "double precision":
            return OpenGaussDataType.FLOAT;
        case "real":
            return OpenGaussDataType.REAL;
        case "int4range":
            return OpenGaussDataType.RANGE;
        case "money":
            return OpenGaussDataType.MONEY;
        case "bit":
        case "bit varying":
            return OpenGaussDataType.BIT;
        case "inet":
            return OpenGaussDataType.INET;
        default:
            throw new AssertionError(typeString);
        }
    }

    public static class OpenGaussRowValue extends AbstractRowValue<OpenGaussTables, OpenGaussColumn, OpenGaussConstant> {

        protected OpenGaussRowValue(OpenGaussTables tables, Map<OpenGaussColumn, OpenGaussConstant> values) {
            super(tables, values);
        }

    }

    public static class OpenGaussTable
            extends AbstractRelationalTable<OpenGaussColumn, OpenGaussIndex, OpenGaussGlobalState> {

        public enum TableType {
            STANDARD
        }

        private final TableType tableType;
        private final boolean isInsertable;

        public OpenGaussTable(String tableName, List<OpenGaussColumn> columns, List<OpenGaussIndex> indexes,
                TableType tableType, boolean isView, boolean isInsertable) {
            super(tableName, columns, indexes, isView);
            this.isInsertable = isInsertable;
            this.tableType = tableType;
        }


        public TableType getTableType() {
            return tableType;
        }

        public boolean isInsertable() {
            return isInsertable;
        }

    }



    public static final class OpenGaussIndex extends TableIndex {

        private OpenGaussIndex(String indexName) {
            super(indexName);
        }

        public static OpenGaussIndex create(String indexName) {
            return new OpenGaussIndex(indexName);
        }

        @Override
        public String getIndexName() {
            if (super.getIndexName().contentEquals("PRIMARY")) {
                return "`PRIMARY`";
            } else {
                return super.getIndexName();
            }
        }

    }

    public static OpenGaussSchema fromConnection(SQLConnection con, String databaseName) throws SQLException {
        try {
            List<OpenGaussTable> databaseTables = new ArrayList<>();
            try (Statement s = con.createStatement()) {
                try (ResultSet rs = s.executeQuery(
                        "SELECT table_name, table_schema, table_type, is_insertable_into FROM information_schema.tables WHERE table_schema='public' OR table_schema LIKE 'pg_temp_%' ORDER BY table_name;")) {
                    while (rs.next()) {
                        String tableName = rs.getString("table_name");
                        String tableTypeSchema = rs.getString("table_schema");
                        boolean isInsertable = rs.getBoolean("is_insertable_into");
                        // TODO: also check insertable
                        // TODO: insert into view?
                        boolean isView = tableName.startsWith("v"); // tableTypeStr.contains("VIEW") ||
                                                                    // tableTypeStr.contains("LOCAL TEMPORARY") &&
                                                                    // !isInsertable;
                        OpenGaussTable.TableType tableType = getTableType(tableTypeSchema);
                        List<OpenGaussColumn> databaseColumns = getTableColumns(con, tableName);
                        List<OpenGaussIndex> indexes = getIndexes(con, tableName);
                        OpenGaussTable t = new OpenGaussTable(tableName, databaseColumns, indexes, tableType, 
                                isView, isInsertable);
                        for (OpenGaussColumn c : databaseColumns) {
                            c.setTable(t);
                        }
                        databaseTables.add(t);
                    }
                }
            }
            return new OpenGaussSchema(databaseTables, databaseName);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new AssertionError(e);
        }
    }

    

    protected static OpenGaussTable.TableType getTableType(String tableTypeStr) throws AssertionError {
        OpenGaussTable.TableType tableType;
        if (tableTypeStr.contentEquals("public")) {
            tableType = TableType.STANDARD;

//        } else if (tableTypeStr.startsWith("pg_temp")) {
//            tableType = TableType.TEMPORARY;
        } else {
            throw new AssertionError(tableTypeStr);
        }
        return tableType;
    }

    protected static List<OpenGaussIndex> getIndexes(SQLConnection con, String tableName) throws SQLException {
        List<OpenGaussIndex> indexes = new ArrayList<>();
        try (Statement s = con.createStatement()) {
            try (ResultSet rs = s.executeQuery(String
                    .format("SELECT indexname FROM pg_indexes WHERE tablename='%s' ORDER BY indexname;", tableName))) {
                while (rs.next()) {
                    String indexName = rs.getString("indexname");
                    if (DBMSCommon.matchesIndexName(indexName)) {
                        indexes.add(OpenGaussIndex.create(indexName));
                    }
                }
            }
        }
        return indexes;
    }

    protected static List<OpenGaussColumn> getTableColumns(SQLConnection con, String tableName) throws SQLException {
        List<OpenGaussColumn> columns = new ArrayList<>();
        try (Statement s = con.createStatement()) {
            try (ResultSet rs = s
                    .executeQuery("select column_name, data_type from INFORMATION_SCHEMA.COLUMNS where table_name = '"
                            + tableName + "' ORDER BY column_name")) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name");
                    String dataType = rs.getString("data_type");
                    OpenGaussColumn c = new OpenGaussColumn(columnName, getColumnType(dataType));
                    columns.add(c);
                }
            }
        }
        return columns;
    }

    public OpenGaussSchema(List<OpenGaussTable> databaseTables, String databaseName) {
        super(databaseTables);
        this.databaseName = databaseName;
    }

    public OpenGaussTables getRandomTableNonEmptyTables() {
        return new OpenGaussTables(Randomly.nonEmptySubset(getDatabaseTables()));
    }

    public String getDatabaseName() {
        return databaseName;
    }

}
