package sqlancer.opengauss;

import java.util.Optional;

import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
// 复合类型
public final class OpenGaussCompoundDataType {

    private final OpenGaussDataType dataType;
    private final OpenGaussCompoundDataType elemType;
    private final Integer size;

    private OpenGaussCompoundDataType(OpenGaussDataType dataType, OpenGaussCompoundDataType elemType, Integer size) {
        this.dataType = dataType;
        this.elemType = elemType;
        this.size = size;
    }

    public OpenGaussDataType getDataType() {
        return dataType;
    }

    public OpenGaussCompoundDataType getElemType() {
        if (elemType == null) {
            throw new AssertionError();
        }
        return elemType;
    }

    public Optional<Integer> getSize() {
        if (size == null) {
            return Optional.empty();
        } else {
            return Optional.of(size);
        }
    }

    public static OpenGaussCompoundDataType create(OpenGaussDataType type, int size) {
        return new OpenGaussCompoundDataType(type, null, size);
    }

    public static OpenGaussCompoundDataType create(OpenGaussDataType type) {
        return new OpenGaussCompoundDataType(type, null, null);
    }
}
