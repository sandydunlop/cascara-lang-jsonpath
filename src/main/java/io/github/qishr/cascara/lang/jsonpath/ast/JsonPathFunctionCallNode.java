package io.github.qishr.cascara.lang.jsonpath.ast;

import java.util.List;

public class JsonPathFunctionCallNode extends JsonPathExpressionNode {
    private final List<JsonPathExpressionNode> arguments;
    private final String name;

    public JsonPathFunctionCallNode(String name, List<JsonPathExpressionNode> args) {
        this.name = name;
        this.arguments = args;
    }

    public List<JsonPathExpressionNode> getArguments() {
        return arguments;
    }

    public String getName() {
        return name;
    }
}
