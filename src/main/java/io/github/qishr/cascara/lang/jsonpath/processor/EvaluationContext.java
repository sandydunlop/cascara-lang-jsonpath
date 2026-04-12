package io.github.qishr.cascara.lang.jsonpath.processor;

public class EvaluationContext {
    public final Object root;     // $
    public final Object current;  // @
    public final Object parent;   // optional
    public final Integer index;   // optional
    public final int depth;

    public EvaluationContext(Object root, Object current, Object parent, Integer index, int depth) {
        this.root = root;
        this.current = current;
        this.parent = parent;
        this.index = index;
        this.depth = depth;
    }

    public EvaluationContext withCurrent(Object newCurrent) {
        // When moving to a child, we naturally increase the depth
        return new EvaluationContext(root, newCurrent, current, index, depth + 1);
    }

    public EvaluationContext withIndex(int idx) {
        return new EvaluationContext(root, current, parent, idx, depth);
    }

    public int getDepth() { return depth; }
}
