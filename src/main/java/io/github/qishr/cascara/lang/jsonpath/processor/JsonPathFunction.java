package io.github.qishr.cascara.lang.jsonpath.processor;

import java.util.List;

@FunctionalInterface
public interface JsonPathFunction {
    Object apply(List<Object> args, EvaluationContext ctx);
}
