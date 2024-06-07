package sqlancer.opengauss.ast;

import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussSimilarTo implements OpenGaussExpression {

    private final OpenGaussExpression string;
    private final OpenGaussExpression similarTo;
    private final OpenGaussExpression escapeCharacter;

    public OpenGaussSimilarTo(OpenGaussExpression string, OpenGaussExpression similarTo,
            OpenGaussExpression escapeCharacter) {
        this.string = string;
        this.similarTo = similarTo;
        this.escapeCharacter = escapeCharacter;
    }

    public OpenGaussExpression getString() {
        return string;
    }

    public OpenGaussExpression getSimilarTo() {
        return similarTo;
    }

    public OpenGaussExpression getEscapeCharacter() {
        return escapeCharacter;
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return OpenGaussDataType.BOOLEAN;
    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        return null;
    }

}
