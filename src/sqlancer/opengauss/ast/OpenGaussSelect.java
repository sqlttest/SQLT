package sqlancer.opengauss.ast;

import java.util.Collections;
import java.util.List;

import sqlancer.Randomly;
import sqlancer.common.ast.SelectBase;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussTable;

public class OpenGaussSelect extends SelectBase<OpenGaussExpression> implements OpenGaussExpression {

    private SelectType selectOption = SelectType.ALL;
    private List<OpenGaussJoin> joinClauses = Collections.emptyList();
    private OpenGaussExpression distinctOnClause;
    private ForClause forClause;

    public enum ForClause {
        UPDATE("UPDATE"), NO_KEY_UPDATE("NO KEY UPDATE"), SHARE("SHARE"), KEY_SHARE("KEY SHARE");

        private final String textRepresentation;

        ForClause(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public String getTextRepresentation() {
            return textRepresentation;
        }

        public static ForClause getRandom() {
            return Randomly.fromOptions(values());
        }
    }

    public static class OpenGaussFromTable implements OpenGaussExpression {
        private final OpenGaussTable t;
        private final boolean only;

        public OpenGaussFromTable(OpenGaussTable t, boolean only) {
            this.t = t;
            this.only = only;
        }

        public OpenGaussTable getTable() {
            return t;
        }

        public boolean isOnly() {
            return only;
        }

        @Override
        public OpenGaussDataType getExpressionType() {
            return null;
        }
    }

    public static class OpenGaussSubquery implements OpenGaussExpression {
        private final OpenGaussSelect s;
        private final String name;

        public OpenGaussSubquery(OpenGaussSelect s, String name) {
            this.s = s;
            this.name = name;
        }

        public OpenGaussSelect getSelect() {
            return s;
        }

        public String getName() {
            return name;
        }

        @Override
        public OpenGaussDataType getExpressionType() {
            return null;
        }
    }

    public enum SelectType {
        DISTINCT, ALL;

        public static SelectType getRandom() {
            return Randomly.fromOptions(values());
        }
    }

    public void setSelectType(SelectType fromOptions) {
        this.setSelectOption(fromOptions);
    }

    public void setDistinctOnClause(OpenGaussExpression distinctOnClause) {
        if (selectOption != SelectType.DISTINCT) {
            throw new IllegalArgumentException();
        }
        this.distinctOnClause = distinctOnClause;
    }

    public SelectType getSelectOption() {
        return selectOption;
    }

    public void setSelectOption(SelectType fromOptions) {
        this.selectOption = fromOptions;
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return null;
    }

    public void setJoinClauses(List<OpenGaussJoin> joinStatements) {
        this.joinClauses = joinStatements;

    }

    public List<OpenGaussJoin> getJoinClauses() {
        return joinClauses;
    }

    public OpenGaussExpression getDistinctOnClause() {
        return distinctOnClause;
    }

    public void setForClause(ForClause forClause) {
        this.forClause = forClause;
    }

    public ForClause getForClause() {
        return forClause;
    }

}
