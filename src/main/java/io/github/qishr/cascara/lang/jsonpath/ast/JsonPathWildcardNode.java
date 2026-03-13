package io.github.qishr.cascara.lang.jsonpath.ast;

public class JsonPathWildcardNode extends JsonPathNode {
    @Override
    public String getString() {
        return "*";
    }
}

