package io.github.qishr.cascara.lang.jsonpath.ast;

public class JsonPathComparisonExpressionNode extends JsonPathExpressionNode {
    private final JsonPathExpressionNode left;
    private final JsonPathExpressionNode right;
    private final JsonPathComparisonOperator op;

    public JsonPathComparisonExpressionNode(JsonPathExpressionNode left, JsonPathExpressionNode right, JsonPathComparisonOperator op) {
        this.left = left;
        this.right = right;
        this.op = op;
    }

    public JsonPathExpressionNode getLeft() {
        return left;
    }

    public JsonPathExpressionNode getRight() {
        return right;
    }

    public JsonPathComparisonOperator getOperator() {
        return op;
    }


}
