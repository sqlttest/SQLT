package sqlancer.opengauss.ast;

import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;

public class OpenGaussFunction implements OpenGaussExpression {

    private final String func;
    private final OpenGaussExpression[] args;
    private final OpenGaussDataType returnType;
    private OpenGaussFunctionWithResult functionWithKnownResult;

    public OpenGaussFunction(OpenGaussFunctionWithResult func, OpenGaussDataType returnType, OpenGaussExpression... args) {
        functionWithKnownResult = func;
        this.func = func.getName();
        this.returnType = returnType;
        this.args = args.clone();
    }

    public OpenGaussFunction(OpenGaussFunctionWithUnknownResult f, OpenGaussDataType returnType,
            OpenGaussExpression... args) {
        this.func = f.getName();
        this.returnType = returnType;
        this.args = args.clone();
    }

    public String getFunctionName() {
        return func;
    }

    public OpenGaussExpression[] getArguments() {
        return args.clone();
    }

    public enum OpenGaussFunctionWithResult {
        ABS(1, "abs") {

            @Override
            public OpenGaussConstant apply(OpenGaussConstant[] evaluatedArgs, OpenGaussExpression... args) {
                if (evaluatedArgs[0].isNull()) {
                    return OpenGaussConstant.createNullConstant();
                } else {
                    return OpenGaussConstant
                            .createIntConstant(Math.abs(evaluatedArgs[0].cast(OpenGaussDataType.INT).asInt()));
                }
            }

            @Override
            public boolean supportsReturnType(OpenGaussDataType type) {
                return type == OpenGaussDataType.INT;
            }

            @Override
            public OpenGaussDataType[] getInputTypesForReturnType(OpenGaussDataType returnType, int nrArguments) {
                return new OpenGaussDataType[] { returnType };
            }

        },
        LOWER(1, "lower") {

            @Override
            public OpenGaussConstant apply(OpenGaussConstant[] evaluatedArgs, OpenGaussExpression... args) {
                if (evaluatedArgs[0].isNull()) {
                    return OpenGaussConstant.createNullConstant();
                } else {
                    String text = evaluatedArgs[0].asString();
                    return OpenGaussConstant.createTextConstant(text.toLowerCase());
                }
            }

            @Override
            public boolean supportsReturnType(OpenGaussDataType type) {
                return type == OpenGaussDataType.TEXT;
            }

            @Override
            public OpenGaussDataType[] getInputTypesForReturnType(OpenGaussDataType returnType, int nrArguments) {
                return new OpenGaussDataType[] { OpenGaussDataType.TEXT };
            }

        },
        LENGTH(1, "length") {
            @Override
            public OpenGaussConstant apply(OpenGaussConstant[] evaluatedArgs, OpenGaussExpression... args) {
                if (evaluatedArgs[0].isNull()) {
                    return OpenGaussConstant.createNullConstant();
                }
                String text = evaluatedArgs[0].asString();
                return OpenGaussConstant.createIntConstant(text.length());
            }

            @Override
            public boolean supportsReturnType(OpenGaussDataType type) {
                return type == OpenGaussDataType.INT;
            }

            @Override
            public OpenGaussDataType[] getInputTypesForReturnType(OpenGaussDataType returnType, int nrArguments) {
                return new OpenGaussDataType[] { OpenGaussDataType.TEXT };
            }
        },
        UPPER(1, "upper") {

            @Override
            public OpenGaussConstant apply(OpenGaussConstant[] evaluatedArgs, OpenGaussExpression... args) {
                if (evaluatedArgs[0].isNull()) {
                    return OpenGaussConstant.createNullConstant();
                } else {
                    String text = evaluatedArgs[0].asString();
                    return OpenGaussConstant.createTextConstant(text.toUpperCase());
                }
            }

            @Override
            public boolean supportsReturnType(OpenGaussDataType type) {
                return type == OpenGaussDataType.TEXT;
            }

            @Override
            public OpenGaussDataType[] getInputTypesForReturnType(OpenGaussDataType returnType, int nrArguments) {
                return new OpenGaussDataType[] { OpenGaussDataType.TEXT };
            }

        },
        // NULL_IF(2, "nullif") {
        //
        // @Override
        // public OpenGaussConstant apply(OpenGaussConstant[] evaluatedArgs, OpenGaussExpression[] args) {
        // OpenGaussConstant equals = evaluatedArgs[0].isEquals(evaluatedArgs[1]);
        // if (equals.isBoolean() && equals.asBoolean()) {
        // return OpenGaussConstant.createNullConstant();
        // } else {
        // // TODO: SELECT (nullif('1', FALSE)); yields '1', but should yield TRUE
        // return evaluatedArgs[0];
        // }
        // }
        //
        // @Override
        // public boolean supportsReturnType(OpenGaussDataType type) {
        // return true;
        // }
        //
        // @Override
        // public OpenGaussDataType[] getInputTypesForReturnType(OpenGaussDataType returnType, int nrArguments) {
        // return getType(nrArguments, returnType);
        // }
        //
        // @Override
        // public boolean checkArguments(OpenGaussExpression[] constants) {
        // for (OpenGaussExpression e : constants) {
        // if (!(e instanceof OpenGaussNullConstant)) {
        // return true;
        // }
        // }
        // return false;
        // }
        //
        // },
        NUM_NONNULLS(1, "num_nonnulls") {
            @Override
            public OpenGaussConstant apply(OpenGaussConstant[] args, OpenGaussExpression... origArgs) {
                int nr = 0;
                for (OpenGaussConstant c : args) {
                    if (!c.isNull()) {
                        nr++;
                    }
                }
                return OpenGaussConstant.createIntConstant(nr);
            }

            @Override
            public OpenGaussDataType[] getInputTypesForReturnType(OpenGaussDataType returnType, int nrArguments) {
                return getRandomTypes(nrArguments);
            }

            @Override
            public boolean supportsReturnType(OpenGaussDataType type) {
                return type == OpenGaussDataType.INT;
            }

            @Override
            public boolean isVariadic() {
                return true;
            }

        },
        NUM_NULLS(1, "num_nulls") {
            @Override
            public OpenGaussConstant apply(OpenGaussConstant[] args, OpenGaussExpression... origArgs) {
                int nr = 0;
                for (OpenGaussConstant c : args) {
                    if (c.isNull()) {
                        nr++;
                    }
                }
                return OpenGaussConstant.createIntConstant(nr);
            }

            @Override
            public OpenGaussDataType[] getInputTypesForReturnType(OpenGaussDataType returnType, int nrArguments) {
                return getRandomTypes(nrArguments);
            }

            @Override
            public boolean supportsReturnType(OpenGaussDataType type) {
                return type == OpenGaussDataType.INT;
            }

            @Override
            public boolean isVariadic() {
                return true;
            }

        };

        private String functionName;
        final int nrArgs;
        private final boolean variadic;

        public OpenGaussDataType[] getRandomTypes(int nr) {
            OpenGaussDataType[] types = new OpenGaussDataType[nr];
            for (int i = 0; i < types.length; i++) {
                types[i] = OpenGaussDataType.getRandomType();
            }
            return types;
        }

        OpenGaussFunctionWithResult(int nrArgs, String functionName) {
            this.nrArgs = nrArgs;
            this.functionName = functionName;
            this.variadic = false;
        }

        /**
         * Gets the number of arguments if the function is non-variadic. If the function is variadic, the minimum number
         * of arguments is returned.
         *
         * @return the number of arguments
         */
        public int getNrArgs() {
            return nrArgs;
        }

        public abstract OpenGaussConstant apply(OpenGaussConstant[] evaluatedArgs, OpenGaussExpression... args);

        @Override
        public String toString() {
            return functionName;
        }

        public boolean isVariadic() {
            return variadic;
        }

        public String getName() {
            return functionName;
        }

        public abstract boolean supportsReturnType(OpenGaussDataType type);

        public abstract OpenGaussDataType[] getInputTypesForReturnType(OpenGaussDataType returnType, int nrArguments);

        public boolean checkArguments(OpenGaussExpression... constants) {
            return true;
        }

    }

    @Override
    public OpenGaussConstant getExpectedValue() {
        if (functionWithKnownResult == null) {
            return null;
        }
        OpenGaussConstant[] constants = new OpenGaussConstant[args.length];
        for (int i = 0; i < constants.length; i++) {
            constants[i] = args[i].getExpectedValue();
            if (constants[i] == null) {
                return null;
            }
        }
        return functionWithKnownResult.apply(constants, args);
    }

    @Override
    public OpenGaussDataType getExpressionType() {
        return returnType;
    }

}
