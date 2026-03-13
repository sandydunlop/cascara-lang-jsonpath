package io.github.qishr.cascara.lang.jsonpath.ast;

import java.util.List;

public class JsonPathUnionNode extends JsonPathNode {
    private final List<JsonPathNode> elements;

    public JsonPathUnionNode(List<JsonPathNode> elements) {
        this.elements = elements;
    }

    public List<JsonPathNode> getElements() {
        return elements;
    }
}
