package io.github.qishr.cascara.lang.jsonpath.ast;

public class JsonPathLogicalExpressionNode extends JsonPathExpressionNode {
    private final JsonPathExpressionNode left;
    private final JsonPathExpressionNode right;
    private final JsonPathLogicalOperator op;

    public JsonPathLogicalExpressionNode(JsonPathExpressionNode left, JsonPathExpressionNode right, JsonPathLogicalOperator op) {
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

    // // TODO: Choose one
    // public JsonPathComparisonOperator getOp() {
    //     return op;
    // }
    public JsonPathLogicalOperator getOperator() {
        return op;
    }


}
