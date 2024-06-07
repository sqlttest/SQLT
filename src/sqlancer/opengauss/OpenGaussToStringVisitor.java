package sqlancer.opengauss;

import java.util.Optional;

import sqlancer.Randomly;
import sqlancer.common.visitor.BinaryOperation;
import sqlancer.common.visitor.ToStringVisitor;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.ast.OpenGaussAggregate;
import sqlancer.opengauss.ast.OpenGaussBetweenOperation;
import sqlancer.opengauss.ast.OpenGaussBinaryLogicalOperation;
import sqlancer.opengauss.ast.OpenGaussCastOperation;
import sqlancer.opengauss.ast.OpenGaussCollate;
import sqlancer.opengauss.ast.OpenGaussColumnValue;
import sqlancer.opengauss.ast.OpenGaussConstant;
import sqlancer.opengauss.ast.OpenGaussExpression;
import sqlancer.opengauss.ast.OpenGaussFunction;
import sqlancer.opengauss.ast.OpenGaussInOperation;
import sqlancer.opengauss.ast.OpenGaussJoin;
import sqlancer.opengauss.ast.OpenGaussJoin.OpenGaussJoinType;
import sqlancer.opengauss.ast.OpenGaussLikeOperation;
import sqlancer.opengauss.ast.OpenGaussOrderByTerm;
import sqlancer.opengauss.ast.OpenGaussPOSIXRegularExpression;
import sqlancer.opengauss.ast.OpenGaussPostfixOperation;
import sqlancer.opengauss.ast.OpenGaussPostfixText;
import sqlancer.opengauss.ast.OpenGaussPrefixOperation;
import sqlancer.opengauss.ast.OpenGaussSelect;
import sqlancer.opengauss.ast.OpenGaussSelect.OpenGaussFromTable;
import sqlancer.opengauss.ast.OpenGaussSelect.OpenGaussSubquery;
import sqlancer.opengauss.ast.OpenGaussSimilarTo;

public final class OpenGaussToStringVisitor extends ToStringVisitor<OpenGaussExpression> implements OpenGaussVisitor {

    @Override
    public void visitSpecific(OpenGaussExpression expr) {
        OpenGaussVisitor.super.visit(expr);
    }

    @Override
    public void visit(OpenGaussConstant constant) {
        sb.append(constant.getTextRepresentation());
    }

    @Override
    public String get() {
        return sb.toString();
    }

    @Override
    public void visit(OpenGaussPostfixOperation op) {
        sb.append("(");
        visit(op.getExpression());
        sb.append(")");
        sb.append(" ");
        sb.append(op.getOperatorTextRepresentation());
    }

    @Override
    public void visit(OpenGaussColumnValue c) {
        sb.append(c.getColumn().getFullQualifiedName());
    }

    @Override
    public void visit(OpenGaussPrefixOperation op) {
        sb.append(op.getTextRepresentation());
        sb.append(" (");
        visit(op.getExpression());
        sb.append(")");
    }

    @Override
    public void visit(OpenGaussFromTable from) {
        // if (from.isOnly()) {
        //     sb.append("ONLY ");
        // }
        sb.append(from.getTable().getName());
        if (!from.isOnly() && Randomly.getBoolean()) {
            sb.append("*");
        }
    }

    @Override
    public void visit(OpenGaussSubquery subquery) {
        sb.append("(");
        visit(subquery.getSelect());
        sb.append(") AS ");
        sb.append(subquery.getName());
    }

    @Override
    public void visit(OpenGaussSelect s) {
        sb.append("SELECT ");
        switch (s.getSelectOption()) {
        case DISTINCT:
            sb.append("DISTINCT ");
            if (s.getDistinctOnClause() != null) {
                sb.append("ON (");
                visit(s.getDistinctOnClause());
                sb.append(") ");
            }
            break;
        case ALL:
            sb.append(Randomly.fromOptions("ALL ", ""));
            break;
        default:
            throw new AssertionError();
        }
        if (s.getFetchColumns() == null) {
            sb.append("*");
        } else {
            visit(s.getFetchColumns());
        }
        sb.append(" FROM ");
        visit(s.getFromList());

        for (OpenGaussJoin j : s.getJoinClauses()) {
            sb.append(" ");
            switch (j.getType()) {
            case INNER:
                if (Randomly.getBoolean()) {
                    sb.append("INNER ");
                }
                sb.append("JOIN");
                break;
            case LEFT:
                sb.append("LEFT OUTER JOIN");
                break;
            case RIGHT:
                sb.append("RIGHT OUTER JOIN");
                break;
            case FULL:
                sb.append("FULL OUTER JOIN");
                break;
            case CROSS:
                sb.append("CROSS JOIN");
                break;
            default:
                throw new AssertionError(j.getType());
            }
            sb.append(" ");
            visit(j.getTableReference());
            if (j.getType() != OpenGaussJoinType.CROSS) {
                sb.append(" ON ");
                visit(j.getOnClause());
            }
        }

        if (s.getWhereClause() != null) {
            sb.append(" WHERE ");
            visit(s.getWhereClause());
        }
        if (s.getGroupByExpressions().size() > 0) {
            sb.append(" GROUP BY ");
            visit(s.getGroupByExpressions());
        }
        if (s.getHavingClause() != null) {
            sb.append(" HAVING ");
            visit(s.getHavingClause());

        }
        if (!s.getOrderByExpressions().isEmpty()) {
            sb.append(" ORDER BY ");
            visit(s.getOrderByExpressions());
        }
        if (s.getLimitClause() != null) {
            sb.append(" LIMIT ");
            visit(s.getLimitClause());
        }

        if (s.getOffsetClause() != null) {
            sb.append(" OFFSET ");
            visit(s.getOffsetClause());
        }
    }

    @Override
    public void visit(OpenGaussOrderByTerm op) {
        visit(op.getExpr());
        sb.append(" ");
        sb.append(op.getOrder());
    }

    @Override
    public void visit(OpenGaussFunction f) {
        sb.append(f.getFunctionName());
        sb.append("(");
        int i = 0;
        for (OpenGaussExpression arg : f.getArguments()) {
            if (i++ != 0) {
                sb.append(", ");
            }
            visit(arg);
        }
        sb.append(")");
    }

    @Override
    public void visit(OpenGaussCastOperation cast) {
        if (Randomly.getBoolean()) {
            sb.append("CAST(");
            visit(cast.getExpression());
            sb.append(" AS ");
            appendType(cast);
            sb.append(")");
        } else {
            sb.append("(");
            visit(cast.getExpression());
            sb.append(")::");
            appendType(cast);
        }
    }

    private void appendType(OpenGaussCastOperation cast) {
        OpenGaussCompoundDataType compoundType = cast.getCompoundType();
        switch (compoundType.getDataType()) {
        case BOOLEAN:
            sb.append("BOOLEAN");
            break;
        case INT: // TODO support also other int types
            sb.append("INT");
            break;
        case TEXT:
            // TODO: append TEXT, CHAR
            sb.append(Randomly.fromOptions("VARCHAR"));
            break;
        case REAL:
            sb.append("FLOAT");
            break;
        case DECIMAL:
            sb.append("DECIMAL");
            break;
        case FLOAT:
            sb.append("REAL");
            break;
        case RANGE:
            sb.append("int4range");
            break;
        case MONEY:
            sb.append("MONEY");
            break;
        case INET:
            sb.append("INET");
            break;
        case BIT:
            sb.append("BIT");
            // if (Randomly.getBoolean()) {
            // sb.append("(");
            // sb.append(Randomly.getNotCachedInteger(1, 100));
            // sb.append(")");
            // }
            break;
        default:
            throw new AssertionError(cast.getType());
        }
        Optional<Integer> size = compoundType.getSize();
        if (size.isPresent()) {
            sb.append("(");
            sb.append(size.get());
            sb.append(")");
        }
    }

    @Override
    public void visit(OpenGaussBetweenOperation op) {
        sb.append("(");
        visit(op.getExpr());
        if (OpenGaussProvider.generateOnlyKnown && op.getExpr().getExpressionType() == OpenGaussDataType.TEXT
                && op.getLeft().getExpressionType() == OpenGaussDataType.TEXT) {
            sb.append(" COLLATE \"C\"");
        }
        sb.append(") BETWEEN ");
        if (op.isSymmetric()) {
            sb.append("SYMMETRIC ");
        }
        sb.append("(");
        visit(op.getLeft());
        sb.append(") AND (");
        visit(op.getRight());
        if (OpenGaussProvider.generateOnlyKnown && op.getExpr().getExpressionType() == OpenGaussDataType.TEXT
                && op.getRight().getExpressionType() == OpenGaussDataType.TEXT) {
            sb.append(" COLLATE \"C\"");
        }
        sb.append(")");
    }

    @Override
    public void visit(OpenGaussInOperation op) {
        sb.append("(");
        visit(op.getExpr());
        sb.append(")");
        if (!op.isTrue()) {
            sb.append(" NOT");
        }
        sb.append(" IN (");
        visit(op.getListElements());
        sb.append(")");
    }

    @Override
    public void visit(OpenGaussPostfixText op) {
        visit(op.getExpr());
        sb.append(op.getText());
    }

    @Override
    public void visit(OpenGaussAggregate op) {
        sb.append(op.getFunction());
        sb.append("(");
        visit(op.getArgs());
        sb.append(")");
    }

    @Override
    public void visit(OpenGaussSimilarTo op) {
        sb.append("(");
        visit(op.getString());
        sb.append(" SIMILAR TO ");
        visit(op.getSimilarTo());
        if (op.getEscapeCharacter() != null) {
            visit(op.getEscapeCharacter());
        }
        sb.append(")");
    }

    @Override
    public void visit(OpenGaussPOSIXRegularExpression op) {
        visit(op.getString());
        sb.append(op.getOp().getStringRepresentation());
        visit(op.getRegex());
    }

    @Override
    public void visit(OpenGaussCollate op) {
        sb.append("(");
        visit(op.getExpr());
        sb.append(" COLLATE ");
        sb.append('"');
        sb.append(op.getCollate());
        sb.append('"');
        sb.append(")");
    }

    @Override
    public void visit(OpenGaussBinaryLogicalOperation op) {
        super.visit((BinaryOperation<OpenGaussExpression>) op);
    }

    @Override
    public void visit(OpenGaussLikeOperation op) {
        super.visit((BinaryOperation<OpenGaussExpression>) op);
    }

}
