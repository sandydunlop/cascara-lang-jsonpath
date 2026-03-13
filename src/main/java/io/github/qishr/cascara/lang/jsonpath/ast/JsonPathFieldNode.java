package io.github.qishr.cascara.lang.jsonpath.ast;

public class JsonPathFieldNode  extends JsonPathExpressionNode {
    private final String fieldName;

    public JsonPathFieldNode(String fieldName) {
        this.fieldName = fieldName;
    }

    // TODO: Choose one of these
    public String getFieldName() { return fieldName; }
    public String getName() { return fieldName; }
}
