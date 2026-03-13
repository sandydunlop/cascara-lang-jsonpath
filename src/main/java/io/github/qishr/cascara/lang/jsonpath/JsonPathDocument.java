package io.github.qishr.cascara.lang.jsonpath;

import io.github.qishr.cascara.common.lang.StructuredDocument;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathNode;

public class JsonPathDocument extends JsonPathNode implements StructuredDocument {
    private JsonPathNode root;

    public JsonPathDocument(JsonPathNode root) {
        this.root = root;
        this.children.add(root);
    }

    @Override
    public JsonPathNode getRoot() {
        return root;
    }
}
