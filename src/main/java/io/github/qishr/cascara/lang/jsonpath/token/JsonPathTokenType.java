package io.github.qishr.cascara.lang.jsonpath.token;

import io.github.qishr.cascara.common.lang.token.TokenCategory;
import io.github.qishr.cascara.common.lang.token.TokenType;

public enum JsonPathTokenType implements TokenType {
    ROOT("$", TokenCategory.SYMBOL),
    CURRENT("@", TokenCategory.SYMBOL),

    DOT(".", TokenCategory.SYMBOL),
    RECURSIVE_DESCENT("..", TokenCategory.SYMBOL),

    WILDCARD("*", TokenCategory.SYMBOL),

    BRACKET_OPEN("[", TokenCategory.DELIMITER),
    BRACKET_CLOSE("]", TokenCategory.DELIMITER),
    PAREN_OPEN("(", TokenCategory.DELIMITER),
    PAREN_CLOSE(")", TokenCategory.DELIMITER),

    COMMA(",", TokenCategory.DELIMITER),
    COLON(":", TokenCategory.DELIMITER),

    IDENTIFIER("identifier", TokenCategory.IDENTIFIER),
    STRING("string", TokenCategory.STRING),
    NUMBER("number", TokenCategory.NUMBER),

    FILTER("?", TokenCategory.OPERATOR),

    OP_EQ("==", TokenCategory.OPERATOR),
    OP_NE("!=", TokenCategory.OPERATOR),
    OP_LT("<", TokenCategory.OPERATOR),
    OP_GT(">", TokenCategory.OPERATOR),
    OP_LE("<=", TokenCategory.OPERATOR),
    OP_GE(">=", TokenCategory.OPERATOR),
    OP_REGEX("=~", TokenCategory.OPERATOR),

    OP_AND("&&", TokenCategory.OPERATOR),
    OP_OR("||", TokenCategory.OPERATOR),
    OP_NOT("!", TokenCategory.OPERATOR),

    EOF("eof", TokenCategory.INTERNAL);

    private final String id;
    private final TokenCategory category;

    JsonPathTokenType(String id, TokenCategory category) {
        this.id = id;
        this.category = category;
    }

    @Override public String getId() { return id; }
    @Override public TokenCategory getCategory() { return category; }
}
