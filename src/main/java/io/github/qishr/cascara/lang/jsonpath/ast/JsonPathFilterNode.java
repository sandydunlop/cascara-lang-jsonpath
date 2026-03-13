package io.github.qishr.cascara.lang.jsonpath.ast;

public class JsonPathFilterNode  extends JsonPathNode {
    private final JsonPathExpressionNode expression;

    public JsonPathFilterNode(JsonPathExpressionNode expression) {
        this.expression = expression;
    }

    public JsonPathExpressionNode getExpression() { return expression; }
}
