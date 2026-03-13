package io.github.qishr.cascara.lang.jsonpath.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathComparisonExpressionNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathCurrentNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathExpressionNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathFieldNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathFilterNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathFunctionCallNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathIndexNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathLiteralNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathLogicalExpressionNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathRecursiveNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathRootNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathSliceNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathUnionNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathWildcardNode;

public class JsonPathEvaluator {

    private final Map<String, JsonPathFunction> functions = new HashMap<>();

    public JsonPathEvaluator() {
        registerBuiltInFunctions();
    }

    public Object evaluate(JsonPathNode path, Object json) {
        EvaluationContext ctx = new EvaluationContext(json, json, null, null);
        return evaluatePath(path, ctx);
    }

    private Object evaluatePath(JsonPathNode node, EvaluationContext ctx) {
        if (node instanceof JsonPathRootNode root) {
            return evaluateRoot(root, ctx);
        }
        if (node instanceof JsonPathCurrentNode cur) {
            return evaluateCurrent(cur, ctx);
        }
        if (node instanceof JsonPathFieldNode field) {
            return evaluateField(field, ctx);
        }
        if (node instanceof JsonPathIndexNode idx) {
            return evaluateIndex(idx, ctx);
        }
        if (node instanceof JsonPathSliceNode slice) {
            return evaluateSlice(slice, ctx);
        }
        if (node instanceof JsonPathUnionNode union) {
            return evaluateUnion(union, ctx);
        }
        if (node instanceof JsonPathFilterNode filter) {
            return evaluateFilter(filter, ctx);
        }
        if (node instanceof JsonPathWildcardNode wc) {
            return evaluateWildcard(wc, ctx);
        }
        if (node instanceof JsonPathRecursiveNode rec) {
            return evaluateRecursive(rec, ctx);
        }

        throw new IllegalStateException("Unhandled node: " + node.getClass());
    }

    // private Object evaluateRoot(JsonPathRootNode node, EvaluationContext ctx) {
    //     return ctx.root;
    // }

    private Object evaluateRoot(JsonPathRootNode node, EvaluationContext ctx) {
        Object value = ctx.root;

        for (JsonPathNode child : node.getChildren()) {
            value = evaluatePath(child, ctx.withCurrent(value));
        }

        return value;
    }


    private Object evaluateCurrent(JsonPathCurrentNode node, EvaluationContext ctx) {
        Object value = ctx.current;

        for (JsonPathNode child : node.getChildren()) {
            value = evaluatePath(child, ctx.withCurrent(value));
        }

        return value;
    }

    private Object evaluateField(JsonPathFieldNode node, EvaluationContext ctx) {
        Object cur = ctx.current;

        if (cur instanceof Map<?,?> map) {
            return map.get(node.getName());
        }

        return null;
    }

    private Object evaluateIndex(JsonPathIndexNode node, EvaluationContext ctx) {
        Object cur = ctx.current;

        if (cur instanceof List<?> list) {
            int idx = node.getIndex();
            if (idx >= 0 && idx < list.size()) {
                return list.get(idx);
            }
        }

        return null;
    }

    private Object evaluateSlice(JsonPathSliceNode slice, EvaluationContext ctx) {
        Object cur = ctx.current;

        if (!(cur instanceof List<?> list)) return null;

        int size = list.size();

        int start = slice.getStart() != null ? slice.getStart() : 0;
        int end   = slice.getEnd()   != null ? slice.getEnd()   : size;
        int step  = slice.getStep()  != null ? slice.getStep()  : 1;

        List<Object> result = new ArrayList<>();

        for (int i = start; i < end; i += step) {
            if (i >= 0 && i < size) {
                result.add(list.get(i));
            }
        }

        return result;
    }

    private Object evaluateUnion(JsonPathUnionNode node, EvaluationContext ctx) {
        List<Object> result = new ArrayList<>();

        for (JsonPathNode element : node.getElements()) {
            Object value = evaluatePath(element, ctx);
            result.add(value);
        }

        return result;
    }

    private Object evaluateFilter(JsonPathFilterNode node, EvaluationContext ctx) {
        Object cur = ctx.current;

        if (!(cur instanceof List<?> list)) return null;

        List<Object> result = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            EvaluationContext childCtx = new EvaluationContext(ctx.root, item, cur, i);

            Object exprValue = evaluateExpression(node.getExpression(), childCtx);

            if (truthy(exprValue)) {
                result.add(item);
            }
        }

        return result;
    }

    private Object evaluateRecursive(JsonPathRecursiveNode node, EvaluationContext ctx) {
        JsonPathNode child = node.getChild();

        List<Object> results = new ArrayList<>();
        walkRecursive(ctx.current, child, ctx, results);
        return results;
    }

    private void walkRecursive(Object current, JsonPathNode child, EvaluationContext ctx, List<Object> out) {
        walkRecursiveInternal(current, child, ctx, out);
    }

    private void walkRecursiveInternal(Object current, JsonPathNode child,
        EvaluationContext ctx, List<Object> out) {

        if (child != null) {
            boolean apply = true;

            // Index / slice: only on leaf lists
            if ((child instanceof JsonPathIndexNode || child instanceof JsonPathSliceNode)
                    && current instanceof List<?> list) {
                apply = isLeafList(list);
            }

            // Union: only on leaf lists, never on maps / scalars
            if (child instanceof JsonPathUnionNode) {
                if (current instanceof List<?> list) {
                    apply = isLeafList(list);
                } else {
                    apply = false;
                }
            }

            if (apply) {
                Object value = evaluatePath(child, ctx.withCurrent(current));

                if (value instanceof Collection<?> col) {
                    out.addAll(col);
                } else if (value != null) {
                    out.add(value);
                }
            }
        }

        if (current instanceof Map<?,?> map) {
            for (Object v : map.values()) {
                walkRecursiveInternal(v, child, ctx, out);
            }
        } else if (current instanceof List<?> list) {
            for (Object v : list) {
                walkRecursiveInternal(v, child, ctx, out);
            }
        }
    }

    private boolean isLeafList(List<?> list) {
        for (Object v : list) {
            if (v instanceof List<?>) {
                return false;
            }
        }
        return true;
    }

    public void registerFunction(String name, JsonPathFunction fn) {
        functions.put(name, fn);
    }

    private JsonPathFunction getFunction(String name) {
        JsonPathFunction fn = functions.get(name);
        if (fn == null) {
            throw new IllegalStateException("Unknown function: " + name);
        }
        return fn;
    }

    private void registerBuiltInFunctions() {
        registerFunction("length", (args, ctx) -> {
            Object arg = args.get(0);
            if (arg instanceof Collection<?> col) return col.size();
            if (arg instanceof Map<?,?> map) return map.size();
            if (arg instanceof String s) return s.length();
            return null;
        });

        registerFunction("min", (args, ctx) -> {
            Object arg = args.get(0);
            if (arg instanceof Collection<?> col) {
                return col.stream()
                        .filter(Number.class::isInstance)
                        .map(Number.class::cast)
                        .min(Comparator.comparingDouble(Number::doubleValue))
                        .orElse(null);
            }
            return null;
        });

        registerFunction("max", (args, ctx) -> {
            Object arg = args.get(0);
            if (arg instanceof Collection<?> col) {
                return col.stream()
                        .filter(Number.class::isInstance)
                        .map(Number.class::cast)
                        .max(Comparator.comparingDouble(Number::doubleValue))
                        .orElse(null);
            }
            return null;
        });
    }

    // private Object evaluateFunctionCall(JsonPathFunctionCallNode fn, EvaluationContext ctx) {
    //     JsonPathFunction impl = getFunction(fn.getName());

    //     List<Object> args = new ArrayList<>();
    //     for (JsonPathExpressionNode argExpr : fn.getArguments()) {
    //         args.add(evaluateExpression(argExpr, ctx));
    //     }

    //     return impl.apply(args, ctx);
    // }

    private Object evaluateExpression(JsonPathExpressionNode expr, EvaluationContext ctx) {

        if (expr instanceof JsonPathLiteralNode lit) {
            return lit.getValue();
        }

        if (expr instanceof JsonPathRootNode root) {
            return evaluateRoot(root, ctx);
        }

        if (expr instanceof JsonPathCurrentNode cur) {
            return evaluateCurrent(cur, ctx);
        }

        if (expr instanceof JsonPathFieldNode field) {
            return evaluateField(field, ctx);
        }

        if (expr instanceof JsonPathComparisonExpressionNode cmp) {
            return evaluateComparison(cmp, ctx);
        }

        if (expr instanceof JsonPathLogicalExpressionNode log) {
            return evaluateLogical(log, ctx);
        }

        if (expr instanceof JsonPathFunctionCallNode fn) {
            return evaluateFunctionCall(fn, ctx);
        }

        throw new IllegalStateException("Unhandled expression: " + expr.getClass());
    }

    private Object evaluateComparison(JsonPathComparisonExpressionNode cmp, EvaluationContext ctx) {
        Object left  = evaluateExpression(cmp.getLeft(), ctx);
        Object right = evaluateExpression(cmp.getRight(), ctx);

        return switch (cmp.getOperator()) {
            case EQ    -> Objects.equals(left, right);
            case NE    -> !Objects.equals(left, right);
            case LT    -> compareNumbers(left, right) < 0;
            case LE    -> compareNumbers(left, right) <= 0;
            case GT    -> compareNumbers(left, right) > 0;
            case GE    -> compareNumbers(left, right) >= 0;
            case REGEX -> regexMatch(left, right);
        };
    }

    private int compareNumbers(Object a, Object b) {
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        return 0; // or throw
    }

    private boolean regexMatch(Object left, Object right) {
        if (left instanceof String s && right instanceof String pattern) {
            return s.matches(pattern);
        }
        return false;
    }

    private Object evaluateLogical(JsonPathLogicalExpressionNode log, EvaluationContext ctx) {
        Object left = evaluateExpression(log.getLeft(), ctx);

        boolean leftBool = truthy(left);

        return switch (log.getOperator()) {
            case AND -> leftBool && truthy(evaluateExpression(log.getRight(), ctx));
            case OR  -> leftBool || truthy(evaluateExpression(log.getRight(), ctx));
        };
    }

    private boolean truthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        if (v instanceof String s) return !s.isEmpty();
        if (v instanceof Collection<?> c) return !c.isEmpty();
        return true;
    }

    private Object evaluateFunctionCall(JsonPathFunctionCallNode fn, EvaluationContext ctx) {
        JsonPathFunction impl = functions.get(fn.getName());
        if (impl == null) {
            throw new IllegalStateException("Unknown function: " + fn.getName());
        }

        List<Object> args = new ArrayList<>();
        for (JsonPathExpressionNode argExpr : fn.getArguments()) {
            args.add(evaluateExpression(argExpr, ctx));
        }

        return impl.apply(args, ctx);
    }

    private Object evaluateWildcard(JsonPathWildcardNode node, EvaluationContext ctx) {
        Object cur = ctx.current;

        if (cur instanceof Map<?,?> map) {
            return new ArrayList<>(map.values());
        }

        if (cur instanceof List<?> list) {
            return new ArrayList<>(list);
        }

        return null;
    }

}
