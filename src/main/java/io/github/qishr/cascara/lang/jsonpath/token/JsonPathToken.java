package io.github.qishr.cascara.lang.jsonpath.token;

import io.github.qishr.cascara.common.lang.token.Token;

public class JsonPathToken implements Token {
    private final JsonPathTokenType type;
    private final String lexeme;
    private final Object value;
    private final int offset;
    private final int startLine;
    private final int startColumn;

    public JsonPathToken(JsonPathTokenType type, String lexeme, Object value,
                         int offset, int startLine, int startColumn) {
        this.type = type;
        this.lexeme = lexeme;
        this.value = value;
        this.offset = offset;
        this.startLine = startLine;
        this.startColumn = startColumn;
    }

    @Override public JsonPathTokenType getType() { return type; }
    @Override public String getLexeme() { return lexeme; }
    @Override public Object getValue() { return value; }
    @Override public int getOffset() { return offset; }
    @Override public int getStartLine() { return startLine; }
    @Override public int getStartColumn() { return startColumn; }

    @Override public String toString() {
        return type + "('" + lexeme + "')";
    }
}
