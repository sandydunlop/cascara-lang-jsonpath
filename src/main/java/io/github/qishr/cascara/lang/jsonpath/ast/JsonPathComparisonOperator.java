package io.github.qishr.cascara.lang.jsonpath.ast;

import java.net.URI;
import java.util.List;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.CommentAstNode;
import io.github.qishr.cascara.lang.jsonpath.token.JsonPathTokenType;

public enum JsonPathComparisonOperator implements AstNode {
    EQ, NE, LT, LE, GT, GE, REGEX;

    public static JsonPathComparisonOperator fromToken(JsonPathTokenType type) {
        return switch (type) {
            case OP_EQ -> EQ;
            case OP_NE -> NE;
            case OP_LT -> LT;
            case OP_LE -> LE;
            case OP_GT -> GT;
            case OP_GE -> GE;
            case OP_REGEX -> REGEX;
            default -> throw new IllegalArgumentException("Not a comparison operator: " + type);
        };
    }

    @Override
    public int getStartLine() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStartLine'");
    }

    @Override
    public int getStartColumn() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStartColumn'");
    }

    @Override
    public int getEndLine() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEndLine'");
    }

    @Override
    public int getEndColumn() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEndColumn'");
    }

    @Override
    public URI getOriginUri() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUri'");
    }

    @Override
    public List<? extends AstNode> getChildren() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getChildren'");
    }

    @Override
    public List<CommentAstNode> getComments() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getComments'");
    }
}
