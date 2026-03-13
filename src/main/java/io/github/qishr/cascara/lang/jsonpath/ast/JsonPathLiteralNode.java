package io.github.qishr.cascara.lang.jsonpath.ast;

public class JsonPathLiteralNode extends JsonPathExpressionNode {

    private final Object value;

    public JsonPathLiteralNode(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String getString() {
        return value == null ? "null" : value.toString();
    }
}

