package sqlancer.opengauss;

import java.util.List;

import sqlancer.opengauss.OpenGaussSchema.OpenGaussColumn;
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
import sqlancer.opengauss.gen.OpenGaussExpressionGenerator;

public interface OpenGaussVisitor {

    void visit(OpenGaussConstant constant);

    void visit(OpenGaussPostfixOperation op);

    void visit(OpenGaussColumnValue c);

    void visit(OpenGaussPrefixOperation op);

    void visit(OpenGaussSelect op);

    void visit(OpenGaussOrderByTerm op);

    void visit(OpenGaussFunction f);

    void visit(OpenGaussCastOperation cast);

    void visit(OpenGaussBetweenOperation op);

    void visit(OpenGaussInOperation op);

    void visit(OpenGaussPostfixText op);

    void visit(OpenGaussAggregate op);

    void visit(OpenGaussSimilarTo op);

    void visit(OpenGaussCollate op);

    void visit(OpenGaussPOSIXRegularExpression op);

    void visit(OpenGaussFromTable from);

    void visit(OpenGaussSubquery subquery);

    void visit(OpenGaussBinaryLogicalOperation op);

    void visit(OpenGaussLikeOperation op);

    default void visit(OpenGaussExpression expression) {
        if (expression instanceof OpenGaussConstant) {
            visit((OpenGaussConstant) expression);
        } else if (expression instanceof OpenGaussPostfixOperation) {
            visit((OpenGaussPostfixOperation) expression);
        } else if (expression instanceof OpenGaussColumnValue) {
            visit((OpenGaussColumnValue) expression);
        } else if (expression instanceof OpenGaussPrefixOperation) {
            visit((OpenGaussPrefixOperation) expression);
        } else if (expression instanceof OpenGaussSelect) {
            visit((OpenGaussSelect) expression);
        } else if (expression instanceof OpenGaussOrderByTerm) {
            visit((OpenGaussOrderByTerm) expression);
        } else if (expression instanceof OpenGaussFunction) {
            visit((OpenGaussFunction) expression);
        }
        else if (expression instanceof OpenGaussCastOperation) {
            visit((OpenGaussCastOperation) expression);
        } 
        else if (expression instanceof OpenGaussBetweenOperation) {
            visit((OpenGaussBetweenOperation) expression);
        } else if (expression instanceof OpenGaussInOperation) {
            visit((OpenGaussInOperation) expression);
        } else if (expression instanceof OpenGaussAggregate) {
            visit((OpenGaussAggregate) expression);
        } else if (expression instanceof OpenGaussPostfixText) {
            visit((OpenGaussPostfixText) expression);
        } else if (expression instanceof OpenGaussSimilarTo) {
            visit((OpenGaussSimilarTo) expression);
        } else if (expression instanceof OpenGaussPOSIXRegularExpression) {
            visit((OpenGaussPOSIXRegularExpression) expression);
        } else if (expression instanceof OpenGaussCollate) {
            visit((OpenGaussCollate) expression);
        } else if (expression instanceof OpenGaussFromTable) {
            visit((OpenGaussFromTable) expression);
        } else if (expression instanceof OpenGaussSubquery) {
            visit((OpenGaussSubquery) expression);
        } else if (expression instanceof OpenGaussLikeOperation) {
            visit((OpenGaussLikeOperation) expression);
        } else {
            throw new AssertionError(expression);
        }
    }

    static String asString(OpenGaussExpression expr) {
        OpenGaussToStringVisitor visitor = new OpenGaussToStringVisitor();
        visitor.visit(expr);
        return visitor.get();
    }

    static String asExpectedValues(OpenGaussExpression expr) {
        OpenGaussExpectedValueVisitor v = new OpenGaussExpectedValueVisitor();
        v.visit(expr);
        return v.get();
    }

    static String getExpressionAsString(OpenGaussGlobalState globalState, OpenGaussDataType type,
            List<OpenGaussColumn> columns) {
        OpenGaussExpression expression = OpenGaussExpressionGenerator.generateExpression(globalState, columns, type);
        OpenGaussToStringVisitor visitor = new OpenGaussToStringVisitor();
        visitor.visit(expression);
        return visitor.get();
    }

}
