package io.github.qishr.cascara.lang.jsonpath.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.ast.MapEntryAstNode;
import io.github.qishr.cascara.common.lang.ast.ScalarAstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.lang.jsonpath.ast.*;

public class JsonPathEvaluator {

    private Reporter reporter = new SimpleReporter()
        .setLevel(Level.TRACE)
        .setDisableFlush(false);

    private final Map<String, JsonPathFunction> functions = new HashMap<>();

    public JsonPathEvaluator() {
        registerBuiltInFunctions();
    }

    public JsonPathEvaluator setReporter(Reporter reporter) {
        this.reporter = reporter;
        return this;
    }

    public Object evaluate(JsonPathNode path, AstNode json) {
        reporter.trace("=== Begin JsonPathEvaluator ===");
        printStructure(path, "");
        EvaluationContext ctx = new EvaluationContext(json, json, null, 0, 0);
        Object object = evaluatePath(path, ctx);
        reporter.trace("=== End JsonPathEvaluator ===");
        return object;
    }

    private Object evaluatePath(JsonPathNode node, EvaluationContext ctx) {
        // DIAGNOSTIC: Trace the evaluation of the chain
        if (node instanceof JsonPathFieldNode || node instanceof JsonPathIndexNode) {
            reporter.trace("%sPathNode: %s | Input: %s",
                "  ".repeat(ctx.depth), node.getClass().getSimpleName(),
                ctx.current == null ? "null" : ctx.current.getClass().getSimpleName());
        }


        // // 1. Handle Collection Mapping for Siblings
        // if (ctx.current instanceof Collection<?> col &&
        //     (node instanceof JsonPathFieldNode || node instanceof JsonPathIndexNode || node instanceof JsonPathSliceNode)) {

        //     List<Object> results = new ArrayList<>();
        //     for (Object item : col) {
        //         Object res = evaluatePath(node, ctx.withCurrent(item));
        //         if (res != null) {
        //             if (res instanceof Collection<?> sub) results.addAll(sub);
        //             else results.add(res);
        //         }
        //     }
        //     return results;
        // }

        // Handle Collection Mapping for Siblings/Recursive results
        if (ctx.current instanceof Collection<?> col &&
            !(node instanceof JsonPathIndexNode || node instanceof JsonPathSliceNode)) {

            List<Object> results = new ArrayList<>();
            for (Object item : col) {
                Object res = evaluatePath(node, ctx.withCurrent(item));
                if (res != null) {
                    if (res instanceof Collection<?> sub) results.addAll(sub);
                    else results.add(res);
                }
            }
            return results;
        }


        Object result = null;
        if (node instanceof JsonPathRootNode root) {
            result = evaluateRoot(root, ctx);
        }
        else if (node instanceof JsonPathCurrentNode cur) {
            result = evaluateCurrent(cur, ctx);
        }
        else if (node instanceof JsonPathFieldNode field) {
            result = evaluateField(field, ctx);
        }
        else if (node instanceof JsonPathIndexNode idx) {
            result = evaluateIndex(idx, ctx);
        }
        else if (node instanceof JsonPathSliceNode slice) {
            result = evaluateSlice(slice, ctx);
        }
        else if (node instanceof JsonPathUnionNode union) {
            result = evaluateUnion(union, ctx);
        }
        else if (node instanceof JsonPathFilterNode filter) {
            result = evaluateFilter(filter, ctx);
        }
        else if (node instanceof JsonPathWildcardNode wc) {
            result = evaluateWildcard(wc, ctx);
        }
        else if (node instanceof JsonPathRecursiveNode rec) {
            result = evaluateRecursive(rec, ctx);
        } else {
            throw new IllegalStateException("Unhandled node: " + node.getClass());
        }

        if (node.getChildren().isEmpty()) {
            if (result instanceof List<?> list && list.size() == 1) {
                result = list.get(0);
            }
            if (result instanceof ScalarAstNode s) {
                result = s.getPrimitiveValue();
            }
        }


        // // DIAGNOSTIC: Trace the evaluation of the chain
        // if (node instanceof JsonPathFieldNode || node instanceof JsonPathIndexNode) {
        //     reporter.trace("%sPathNode: %s | Result: %s",
        //         "  ".repeat(ctx.depth), node.getClass().getSimpleName(),
        //         (result == null ? "NULL" : result.getClass().getSimpleName()));
        // }

        // DIAGNOSTIC: Replace your current tracing block with this
        if (node instanceof JsonPathFieldNode || node instanceof JsonPathIndexNode) {
            String detail = "null";
            if (ctx.current instanceof SequenceAstNode seq) detail = "Sequence(size=" + seq.size() + ")";
            else if (ctx.current instanceof MapAstNode<?,?> map) detail = "Map(keys=" + map.getEntries().size() + ")";
            else if (ctx.current instanceof Collection<?> col) detail = "Collection(size=" + col.size() + ")";
            else if (ctx.current != null) detail = ctx.current.getClass().getSimpleName();

            reporter.trace("%sPathNode: %s | Input: %s",
                "  ".repeat(ctx.depth), node.getClass().getSimpleName(), detail);
        }

        return result;
    }

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
        if (cur instanceof MapAstNode<?,?> map) {
            return map.get(node.getName());
        }
        return null;
    }

    // private Object evaluateIndex(JsonPathIndexNode node, EvaluationContext ctx) {
    //     Object cur = ctx.current;
    //     if (cur instanceof SequenceAstNode seq) {
    //         int idx = node.getIndex();
    //         if (idx >= 0 && idx < seq.size()) {
    //             return seq.get(idx);
    //         }
    //     }
    //     return null;
    // }
    private Object evaluateIndex(JsonPathIndexNode node, EvaluationContext ctx) {
        Object cur = ctx.current;
        int idx = node.getIndex();

        // Handle standard AST Sequences
        if (cur instanceof SequenceAstNode<?> seq) {
            if (idx >= 0 && idx < seq.size()) return seq.get(idx);
        }
        // Handle Java Lists (from Recursive/Union nodes)
        else if (cur instanceof List<?> list) {
            if (idx >= 0 && idx < list.size()) return list.get(idx);
        }

        return null;
    }

    private Object evaluateSlice(JsonPathSliceNode slice, EvaluationContext ctx) {
        Object cur = ctx.current;
        if (!(cur instanceof SequenceAstNode seq)) return null;

        int size = seq.size();
        int start = slice.getStart() != null ? slice.getStart() : 0;
        int end   = slice.getEnd()   != null ? slice.getEnd()   : size;
        int step  = slice.getStep()  != null ? slice.getStep()  : 1;

        List<Object> result = new ArrayList<>();
        for (int i = start; i < end; i += step) {
            if (i >= 0 && i < size) {
                result.add(seq.get(i));
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
        if (!(cur instanceof SequenceAstNode seq)) return null;

        List<Object> result = new ArrayList<>();
        for (int i = 0; i < seq.size(); i++) {
            AstNode item = seq.get(i);
            EvaluationContext childCtx = new EvaluationContext(ctx.root, item, cur, i, ctx.depth + 1);
            Object exprValue = evaluateExpression(node.getExpression(), childCtx);
            if (truthy(exprValue)) {
                result.add(item);
            }
        }
        return result;
    }

    private Object evaluateRecursive(JsonPathRecursiveNode node, EvaluationContext ctx) {
        List<Object> results = new ArrayList<>();
        walkRecursiveInternal(ctx.current, node.getChild(), ctx, results);
        return results;
    }

    private void walkRecursiveInternal(Object current, JsonPathNode child, EvaluationContext ctx, List<Object> out) {
        if (child != null) {
            boolean apply = true;

            if ((child instanceof JsonPathIndexNode || child instanceof JsonPathSliceNode)
                    && current instanceof SequenceAstNode seq) {
                apply = isLeafList(seq);
            }

            if (child instanceof JsonPathUnionNode) {
                if (current instanceof SequenceAstNode seq) {
                    apply = isLeafList(seq);
                } else {
                    apply = false;
                }
            }

            if (apply) {
                Object value = evaluatePath(child, ctx.withCurrent(current));

                if (value != null) {
                    // If the child (book) has siblings ([0], title), they need to be evaluated
                    // AGAINST the value we just found before we add it to 'out'.
                    Object finalValue = value;

                    // Find the recursive node in the parent's children to get its siblings
                    // Or, more simply, if evaluatePath didn't already consume the siblings:
                    if (child.getChildren() != null) {
                        for (JsonPathNode sibling : child.getChildren()) {
                            finalValue = evaluatePath(sibling, ctx.withCurrent(finalValue));
                        }
                    }

                    if (finalValue != null) {
                        out.add(finalValue instanceof ScalarAstNode s ? s.getPrimitiveValue() : finalValue);
                    }
                }
            }
        }

        // Original structural traversal logic
        if (current instanceof MapAstNode<?,?> map) {
            for (MapEntryAstNode<?> entry : map.getEntries()) {
                walkRecursiveInternal(entry.getValue(), child, ctx, out);
            }
        } else if (current instanceof SequenceAstNode seq) {
            for (AstNode node : seq.getChildren()) {
                walkRecursiveInternal(node, child, ctx, out);
            }
        }
    }

    private boolean isLeafList(SequenceAstNode seq) {
        for (AstNode v : seq.getChildren()) {
            if (v instanceof SequenceAstNode) {
                return false;
            }
        }
        return true;
    }

    public void registerFunction(String name, JsonPathFunction fn) {
        functions.put(name, fn);
    }

    private void registerBuiltInFunctions() {
        registerFunction("length", (args, ctx) -> {
            Object arg = args.get(0);
            if (arg instanceof SequenceAstNode seq) return seq.size();
            if (arg instanceof MapAstNode<?,?> map) return map.getEntries().size();
            if (arg instanceof String s) return s.length();
            return null;
        });
    }

    private Object evaluateExpression(JsonPathExpressionNode expr, EvaluationContext ctx) {
        if (expr instanceof JsonPathLiteralNode lit) return lit.getValue();
        if (expr instanceof JsonPathRootNode root) return evaluateRoot(root, ctx);
        if (expr instanceof JsonPathCurrentNode cur) return evaluateCurrent(cur, ctx);
        if (expr instanceof JsonPathFieldNode field) return evaluateField(field, ctx);
        if (expr instanceof JsonPathComparisonExpressionNode cmp) return evaluateComparison(cmp, ctx);
        if (expr instanceof JsonPathLogicalExpressionNode log) return evaluateLogical(log, ctx);
        if (expr instanceof JsonPathFunctionCallNode fn) return evaluateFunctionCall(fn, ctx);
        throw new IllegalStateException("Unhandled expression: " + expr.getClass());
    }

    private Object evaluateComparison(JsonPathComparisonExpressionNode cmp, EvaluationContext ctx) {
        Object left = evaluateExpression(cmp.getLeft(), ctx);
        Object right = evaluateExpression(cmp.getRight(), ctx);

        // Required unwrap for comparison logic to function against AST types
        if (left instanceof ScalarAstNode s) left = s.getPrimitiveValue();
        if (right instanceof ScalarAstNode s) right = s.getPrimitiveValue();

        return switch (cmp.getOperator()) {
            case EQ -> Objects.equals(left, right);
            case NE -> !Objects.equals(left, right);
            case LT -> compareNumbers(left, right) < 0;
            case LE -> compareNumbers(left, right) <= 0;
            case GT -> compareNumbers(left, right) > 0;
            case GE -> compareNumbers(left, right) >= 0;
            case REGEX -> regexMatch(left, right);
        };
    }

    private int compareNumbers(Object a, Object b) {
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        return 0;
    }

    private boolean regexMatch(Object left, Object right) {
        if (left instanceof String s && right instanceof String pattern) {
            return s.matches(pattern);
        }
        return false;
    }

    private Object evaluateLogical(JsonPathLogicalExpressionNode log, EvaluationContext ctx) {
        boolean leftBool = truthy(evaluateExpression(log.getLeft(), ctx));
        if (log.getOperator() == JsonPathLogicalOperator.AND) {
            return leftBool && truthy(evaluateExpression(log.getRight(), ctx));
        }
        return leftBool || truthy(evaluateExpression(log.getRight(), ctx));
    }

    private boolean truthy(Object v) {
        if (v == null) return false;
        if (v instanceof ScalarAstNode s) v = s.getPrimitiveValue();
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        if (v instanceof String s) return !s.isEmpty();
        if (v instanceof SequenceAstNode s) return s.size() > 0;
        if (v instanceof Collection<?> c) return !c.isEmpty();
        return true;
    }

    private Object evaluateFunctionCall(JsonPathFunctionCallNode fn, EvaluationContext ctx) {
        JsonPathFunction impl = functions.get(fn.getName());
        if (impl == null) throw new IllegalStateException("Unknown function: " + fn.getName());
        List<Object> args = fn.getArguments().stream()
                .map(arg -> evaluateExpression(arg, ctx))
                .collect(Collectors.toList());
        return impl.apply(args, ctx);
    }

    private Object evaluateWildcard(JsonPathWildcardNode node, EvaluationContext ctx) {
        Object cur = ctx.current;
        if (cur instanceof MapAstNode<?,?> map) {
            return map.getEntries().stream().map(MapEntryAstNode::getValue).collect(Collectors.toList());
        }
        if (cur instanceof SequenceAstNode seq) {
            return seq.getChildren();
        }
        return null;
    }

    private void printStructure(JsonPathNode node, String indent) {
        reporter.trace(indent + node.getClass().getSimpleName());
        for (JsonPathNode child : node.getChildren()) {
            printStructure(child, indent + "  ");
        }
    }
}