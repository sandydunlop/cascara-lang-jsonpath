package io.github.qishr.cascara.lang.jsonpath.ast;

public class JsonPathIndexNode extends JsonPathNode {
    private final int index;

    public JsonPathIndexNode(int index) {
        this.index = index;
    }

    public int getIndex() { return index; }
}
