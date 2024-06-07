package sqlancer.opengauss.ast;

import java.util.ArrayList;
import java.util.List;

import sqlancer.Randomly;
import sqlancer.opengauss.OpenGaussSchema.OpenGaussDataType;
import sqlancer.opengauss.gen.OpenGaussExpressionGenerator;

public enum OpenGaussFunctionWithUnknownResult {

    ABBREV("abbrev", OpenGaussDataType.TEXT, OpenGaussDataType.INET),
    BROADCAST("broadcast", OpenGaussDataType.INET, OpenGaussDataType.INET),
    FAMILY("family", OpenGaussDataType.INT, OpenGaussDataType.INET),
    HOSTMASK("hostmask", OpenGaussDataType.INET, OpenGaussDataType.INET),
    MASKLEN("masklen", OpenGaussDataType.INT, OpenGaussDataType.INET),
    NETMASK("netmask", OpenGaussDataType.INET, OpenGaussDataType.INET),
    SET_MASKLEN("set_masklen", OpenGaussDataType.INET, OpenGaussDataType.INET, OpenGaussDataType.INT),
    TEXT("text", OpenGaussDataType.TEXT, OpenGaussDataType.INET),
    INET_SAME_FAMILY("inet_same_family", OpenGaussDataType.BOOLEAN, OpenGaussDataType.INET, OpenGaussDataType.INET),

    // https://www.opengauss.org/docs/devel/functions-admin.html#FUNCTIONS-ADMIN-SIGNAL-TABLE
    // PG_RELOAD_CONF("pg_reload_conf", OpenGaussDataType.BOOLEAN), // too much output
    // PG_ROTATE_LOGFILE("pg_rotate_logfile", OpenGaussDataType.BOOLEAN), prints warning

    // https://www.opengauss.org/docs/devel/functions-info.html#FUNCTIONS-INFO-SESSION-TABLE
    CURRENT_DATABASE("current_database", OpenGaussDataType.TEXT), // name
    // CURRENT_QUERY("current_query", OpenGaussDataType.TEXT), // can generate false positives
    CURRENT_SCHEMA("current_schema", OpenGaussDataType.TEXT), // name
    // CURRENT_SCHEMAS("current_schemas", OpenGaussDataType.TEXT, OpenGaussDataType.BOOLEAN),
    INET_CLIENT_PORT("inet_client_port", OpenGaussDataType.INT),
    // INET_SERVER_PORT("inet_server_port", OpenGaussDataType.INT),
    PG_BACKEND_PID("pg_backend_pid", OpenGaussDataType.INT),
    PG_CURRENT_LOGFILE("pg_current_logfile", OpenGaussDataType.TEXT),
    // PG_IS_OTHER_TEMP_SCHEMA("pg_is_other_temp_schema", OpenGaussDataType.BOOLEAN),
    PG_JIT_AVAILABLE("pg_jit_available", OpenGaussDataType.BOOLEAN),
    PG_NOTIFICATION_QUEUE_USAGE("pg_notification_queue_usage", OpenGaussDataType.REAL),
    PG_TRIGGER_DEPTH("pg_trigger_depth", OpenGaussDataType.INT), VERSION("version", OpenGaussDataType.TEXT),

    //
    TO_CHAR("to_char", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT, OpenGaussDataType.TEXT) {
        @Override
        public OpenGaussExpression[] getArguments(OpenGaussDataType returnType, OpenGaussExpressionGenerator gen,
                int depth) {
            OpenGaussExpression[] args = super.getArguments(returnType, gen, depth);
            args[0] = gen.generateExpression(OpenGaussDataType.getRandomType());
            return args;
        }
    },

    // String functions
    ASCII("ascii", OpenGaussDataType.INT, OpenGaussDataType.TEXT),
    BTRIM("btrim", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    CHR("chr", OpenGaussDataType.TEXT, OpenGaussDataType.INT),
    CONVERT_FROM("convert_from", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT, OpenGaussDataType.TEXT) {
        @Override
        public OpenGaussExpression[] getArguments(OpenGaussDataType returnType, OpenGaussExpressionGenerator gen,
                int depth) {
            OpenGaussExpression[] args = super.getArguments(returnType, gen, depth);
            args[1] = OpenGaussConstant.createTextConstant(Randomly.fromOptions("UTF8", "LATIN1"));
            return args;
        }
    },
    // concat
    // segfault
    // BIT_LENGTH("bit_length", OpenGaussDataType.INT, OpenGaussDataType.TEXT),
    INITCAP("initcap", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    LEFT("left", OpenGaussDataType.TEXT, OpenGaussDataType.INT, OpenGaussDataType.TEXT),
    LOWER("lower", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    MD5("md5", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    UPPER("upper", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    // PG_CLIENT_ENCODING("pg_client_encoding", OpenGaussDataType.TEXT),
    QUOTE_LITERAL("quote_literal", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    QUOTE_IDENT("quote_ident", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    REGEX_REPLACE("regex_replace", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    // REPEAT("repeat", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT,
    // OpenGaussDataType.INT),
    REPLACE("replace", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    REVERSE("reverse", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    RIGHT("right", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT, OpenGaussDataType.INT),
    RPAD("rpad", OpenGaussDataType.TEXT, OpenGaussDataType.INT, OpenGaussDataType.INT),
    RTRIM("rtrim", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    SPLIT_PART("split_part", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT, OpenGaussDataType.INT),
    STRPOS("strpos", OpenGaussDataType.INT, OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    SUBSTR("substr", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT, OpenGaussDataType.INT, OpenGaussDataType.INT),
    TO_ASCII("to_ascii", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    TO_HEX("to_hex", OpenGaussDataType.INT, OpenGaussDataType.TEXT),
    TRANSLATE("translate", OpenGaussDataType.TEXT, OpenGaussDataType.TEXT, OpenGaussDataType.TEXT, OpenGaussDataType.TEXT),
    // mathematical functions
    // https://www.opengauss.org/docs/9.5/functions-math.html
    ABS("abs", OpenGaussDataType.REAL, OpenGaussDataType.REAL),
    CBRT("cbrt", OpenGaussDataType.REAL, OpenGaussDataType.REAL), CEILING("ceiling", OpenGaussDataType.REAL), //
    DEGREES("degrees", OpenGaussDataType.REAL), EXP("exp", OpenGaussDataType.REAL), LN("ln", OpenGaussDataType.REAL),
    LOG("log", OpenGaussDataType.REAL), LOG2("log", OpenGaussDataType.REAL, OpenGaussDataType.REAL),
    PI("pi", OpenGaussDataType.REAL), POWER("power", OpenGaussDataType.REAL, OpenGaussDataType.REAL),
    TRUNC("trunc", OpenGaussDataType.REAL, OpenGaussDataType.INT),
    TRUNC2("trunc", OpenGaussDataType.REAL, OpenGaussDataType.INT, OpenGaussDataType.REAL),
    FLOOR("floor", OpenGaussDataType.REAL),

    // trigonometric functions - complete
    // https://www.opengauss.org/docs/12/functions-math.html#FUNCTIONS-MATH-TRIG-TABLE
    ACOS("acos", OpenGaussDataType.REAL), //
    ACOSD("acosd", OpenGaussDataType.REAL), //
    ASIN("asin", OpenGaussDataType.REAL), //
    ASIND("asind", OpenGaussDataType.REAL), //
    ATAN("atan", OpenGaussDataType.REAL), //
    ATAND("atand", OpenGaussDataType.REAL), //
    ATAN2("atan2", OpenGaussDataType.REAL, OpenGaussDataType.REAL), //
    ATAN2D("atan2d", OpenGaussDataType.REAL, OpenGaussDataType.REAL), //
    COS("cos", OpenGaussDataType.REAL), //
    COSD("cosd", OpenGaussDataType.REAL), //
    COT("cot", OpenGaussDataType.REAL), //
    COTD("cotd", OpenGaussDataType.REAL), //
    SIN("sin", OpenGaussDataType.REAL), //
    SIND("sind", OpenGaussDataType.REAL), //
    TAN("tan", OpenGaussDataType.REAL), //
    TAND("tand", OpenGaussDataType.REAL), //

    // hyperbolic functions - complete
    // https://www.opengauss.org/docs/12/functions-math.html#FUNCTIONS-MATH-HYP-TABLE
    SINH("sinh", OpenGaussDataType.REAL), //
    COSH("cosh", OpenGaussDataType.REAL), //
    TANH("tanh", OpenGaussDataType.REAL), //
    ASINH("asinh", OpenGaussDataType.REAL), //
    ACOSH("acosh", OpenGaussDataType.REAL), //
    ATANH("atanh", OpenGaussDataType.REAL), //

    // https://www.opengauss.org/docs/devel/functions-binarystring.html
    GET_BIT("get_bit", OpenGaussDataType.INT, OpenGaussDataType.TEXT, OpenGaussDataType.INT),
    GET_BYTE("get_byte", OpenGaussDataType.INT, OpenGaussDataType.TEXT, OpenGaussDataType.INT),

    // range functions
    // https://www.opengauss.org/docs/devel/functions-range.html#RANGE-FUNCTIONS-TABLE
    RANGE_LOWER("lower", OpenGaussDataType.INT, OpenGaussDataType.RANGE), //
    RANGE_UPPER("upper", OpenGaussDataType.INT, OpenGaussDataType.RANGE), //
    RANGE_ISEMPTY("isempty", OpenGaussDataType.BOOLEAN, OpenGaussDataType.RANGE), //
    RANGE_LOWER_INC("lower_inc", OpenGaussDataType.BOOLEAN, OpenGaussDataType.RANGE), //
    RANGE_UPPER_INC("upper_inc", OpenGaussDataType.BOOLEAN, OpenGaussDataType.RANGE), //
    RANGE_LOWER_INF("lower_inf", OpenGaussDataType.BOOLEAN, OpenGaussDataType.RANGE), //
    RANGE_UPPER_INF("upper_inf", OpenGaussDataType.BOOLEAN, OpenGaussDataType.RANGE), //
    RANGE_MERGE("range_merge", OpenGaussDataType.RANGE, OpenGaussDataType.RANGE, OpenGaussDataType.RANGE), //

    // https://www.opengauss.org/docs/devel/functions-admin.html#FUNCTIONS-ADMIN-DBSIZE
    GET_COLUMN_SIZE("get_column_size", OpenGaussDataType.INT, OpenGaussDataType.TEXT);
    // PG_DATABASE_SIZE("pg_database_size", OpenGaussDataType.INT, OpenGaussDataType.INT);
    // PG_SIZE_BYTES("pg_size_bytes", OpenGaussDataType.INT, OpenGaussDataType.TEXT);

    private String functionName;
    private OpenGaussDataType returnType;
    private OpenGaussDataType[] argTypes;

    OpenGaussFunctionWithUnknownResult(String functionName, OpenGaussDataType returnType, OpenGaussDataType... indexType) {
        this.functionName = functionName;
        this.returnType = returnType;
        this.argTypes = indexType.clone();

    }

    public boolean isCompatibleWithReturnType(OpenGaussDataType t) {
        return t == returnType;
    }

    public OpenGaussExpression[] getArguments(OpenGaussDataType returnType, OpenGaussExpressionGenerator gen, int depth) {
        OpenGaussExpression[] args = new OpenGaussExpression[argTypes.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = gen.generateExpression(depth, argTypes[i]);
        }
        return args;

    }

    public String getName() {
        return functionName;
    }
    //根据类型返回适合的函数
    public static List<OpenGaussFunctionWithUnknownResult> getSupportedFunctions(OpenGaussDataType type) {
        List<OpenGaussFunctionWithUnknownResult> functions = new ArrayList<>();
        for (OpenGaussFunctionWithUnknownResult func : values()) {
            if (func.isCompatibleWithReturnType(type)) {
                functions.add(func);
            }
        }
        return functions;
    }

}
