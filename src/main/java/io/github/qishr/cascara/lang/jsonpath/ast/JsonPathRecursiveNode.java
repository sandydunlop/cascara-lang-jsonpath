package io.github.qishr.cascara.lang.jsonpath.ast;

public class JsonPathRecursiveNode extends JsonPathNode {
    private final JsonPathNode child;

    public JsonPathRecursiveNode(JsonPathNode child) {
        this.child = child;
        if (child != null) addChild(child);
    }

    public JsonPathNode getChild() {
        return child;
    }

    @Override
    public String getString() {
        return ".." + (child != null ? child.getString() : "");
    }
}

