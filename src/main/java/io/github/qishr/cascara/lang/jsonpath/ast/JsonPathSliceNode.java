package io.github.qishr.cascara.lang.jsonpath.ast;

public class JsonPathSliceNode extends JsonPathNode {
    private final Integer start;
    private final Integer end;
    private final Integer step;

    public JsonPathSliceNode(Integer start, Integer end, Integer step) {
        this.start = start;
        this.end = end;
        this.step = step;
    }

    public Integer getStart() {
        return start;
    }

    public Integer getEnd() {
        return end;
    }

    public Integer getStep() {
        return step;
    }
}
