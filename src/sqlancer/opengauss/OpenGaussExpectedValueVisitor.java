package sqlancer.opengauss;

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

public final class OpenGaussExpectedValueVisitor implements OpenGaussVisitor {

    private final StringBuilder sb = new StringBuilder();
    private static final int NR_TABS = 0;

    private void print(OpenGaussExpression expr) {
        OpenGaussToStringVisitor v = new OpenGaussToStringVisitor();
        v.visit(expr);
        for (int i = 0; i < NR_TABS; i++) {
            sb.append("\t");
        }
        sb.append(v.get());
        sb.append(" -- ");
        sb.append(expr.getExpectedValue());
        sb.append("\n");
    }

    // @Override
    // public void visit(OpenGaussExpression expr) {
    // nrTabs++;
    // try {
    // super.visit(expr);
    // } catch (IgnoreMeException e) {
    //
    // }
    // nrTabs--;
    // }

    @Override
    public void visit(OpenGaussConstant constant) {
        print(constant);
    }

    @Override
    public void visit(OpenGaussPostfixOperation op) {
        print(op);
        visit(op.getExpression());
    }

    public String get() {
        return sb.toString();
    }

    @Override
    public void visit(OpenGaussColumnValue c) {
        print(c);
    }

    @Override
    public void visit(OpenGaussPrefixOperation op) {
        print(op);
        visit(op.getExpression());
    }

    @Override
    public void visit(OpenGaussSelect op) {
        visit(op.getWhereClause());
    }

    @Override
    public void visit(OpenGaussOrderByTerm op) {

    }

    @Override
    public void visit(OpenGaussFunction f) {
        print(f);
        for (int i = 0; i < f.getArguments().length; i++) {
            visit(f.getArguments()[i]);
        }
    }

    @Override
    public void visit(OpenGaussCastOperation cast) {
        print(cast);
        visit(cast.getExpression());
    }

    @Override
    public void visit(OpenGaussBetweenOperation op) {
        print(op);
        visit(op.getExpr());
        visit(op.getLeft());
        visit(op.getRight());
    }

    @Override
    public void visit(OpenGaussInOperation op) {
        print(op);
        visit(op.getExpr());
        for (OpenGaussExpression right : op.getListElements()) {
            visit(right);
        }
    }

    @Override
    public void visit(OpenGaussPostfixText op) {
        print(op);
        visit(op.getExpr());
    }

    @Override
    public void visit(OpenGaussAggregate op) {
        print(op);
        for (OpenGaussExpression expr : op.getArgs()) {
            visit(expr);
        }
    }

    @Override
    public void visit(OpenGaussSimilarTo op) {
        print(op);
        visit(op.getString());
        visit(op.getSimilarTo());
        if (op.getEscapeCharacter() != null) {
            visit(op.getEscapeCharacter());
        }
    }

    @Override
    public void visit(OpenGaussPOSIXRegularExpression op) {
        print(op);
        visit(op.getString());
        visit(op.getRegex());
    }

    @Override
    public void visit(OpenGaussCollate op) {
        print(op);
        visit(op.getExpr());
    }

    @Override
    public void visit(OpenGaussFromTable from) {
        print(from);
    }

    @Override
    public void visit(OpenGaussSubquery subquery) {
        print(subquery);
    }

    @Override
    public void visit(OpenGaussBinaryLogicalOperation op) {
        print(op);
        visit(op.getLeft());
        visit(op.getRight());
    }

    @Override
    public void visit(OpenGaussLikeOperation op) {
        print(op);
        visit(op.getLeft());
        visit(op.getRight());
    }

}
