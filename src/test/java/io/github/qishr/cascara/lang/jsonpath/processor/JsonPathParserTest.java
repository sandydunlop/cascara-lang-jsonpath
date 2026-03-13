package io.github.qishr.cascara.lang.jsonpath.processor;

import io.github.qishr.cascara.lang.jsonpath.ast.*;
import io.github.qishr.cascara.lang.jsonpath.JsonPathOptions;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JsonPathParserTest {

    private JsonPathParser parser;

    @BeforeEach
    public void setup() {
        parser = new JsonPathParser()
                .setReporter(new SimpleReporter().setLevel(Level.TRACE))
                .setOptions(new JsonPathOptions());
    }

    private JsonPathNode parse(String input) {
        return parser.parse(input).getRoot();
    }

    //
    // Tests go here
    //

    @Test
    public void testRootPath() {
        JsonPathNode root = parse("$");
        assertInstanceOf(JsonPathRootNode.class, root);
        assertEquals(0, root.getChildren().size());
    }

    @Test
    public void testCurrentPath() {
        JsonPathNode root = parse("@");
        assertInstanceOf(JsonPathCurrentNode.class, root);
        assertEquals(0, root.getChildren().size());
    }

    @Test
    public void testDotField() {
        JsonPathNode root = parse("$.foo");
        assertInstanceOf(JsonPathRootNode.class, root);

        assertEquals(1, root.getChildren().size());
        assertTrue(root.getChildren().get(0) instanceof JsonPathFieldNode);

        JsonPathFieldNode field = (JsonPathFieldNode) root.getChildren().get(0);
        assertEquals("foo", field.getFieldName());
    }

    @Test
    public void testRecursiveDescent() {
        JsonPathNode root = parse("$..bar");

        assertEquals(1, root.getChildren().size());
        assertTrue(root.getChildren().get(0) instanceof JsonPathRecursiveNode);

        JsonPathRecursiveNode rec = (JsonPathRecursiveNode) root.getChildren().get(0);

        assertTrue(rec.getChild() instanceof JsonPathFieldNode);
        assertEquals("bar", ((JsonPathFieldNode) rec.getChild()).getName());
    }


    @Test
    public void testRecursiveWildcard() {
        JsonPathNode root = parse("$..*");

        assertEquals(1, root.getChildren().size());
        JsonPathRecursiveNode rec = (JsonPathRecursiveNode) root.getChildren().get(0);

        assertTrue(rec.getChild() instanceof JsonPathWildcardNode);
    }


    @Test
    public void testFilterDispatch() {
        JsonPathNode root = parse("$[?(@.foo < 10)]");

        assertEquals(1, root.getChildren().size());
        assertTrue(root.getChildren().get(0) instanceof JsonPathFilterNode);

        JsonPathFilterNode filter = (JsonPathFilterNode) root.getChildren().get(0);
        assertNotNull(filter.getExpression());
    }

    @Test
    public void testIndexDispatch() {
        JsonPathNode root = parse("$[3]");
        assertEquals(1, root.getChildren().size());
        assertNotNull(root.getChildren().get(0)); // until implemented
    }

    @Test
    public void testSliceDispatch() {
        JsonPathNode root = parse("$[1:5]");
        assertEquals(1, root.getChildren().size());
        assertNotNull(root.getChildren().get(0)); // until implemented
    }

    @Test
    public void testUnionDispatch() {
        JsonPathNode root = parse("$['a','b']");
        assertEquals(1, root.getChildren().size());
        assertNotNull(root.getChildren().get(0)); // until implemented
    }

    @Test
    public void testRootAsExpression() {
        JsonPathNode root = parse("$");
        assertInstanceOf(JsonPathRootNode.class, root);
    }

    @Test
    public void testCurrentAsExpression() {
        JsonPathNode root = parse("@");
        assertInstanceOf(JsonPathCurrentNode.class, root);
    }

    @Test
    public void testRootInsideFilter() {
        JsonPathNode root = parse("$[?($)]");

        assertEquals(1, root.getChildren().size());
        JsonPathFilterNode filter = (JsonPathFilterNode) root.getChildren().get(0);

        assertNotNull(filter.getExpression());
        assertInstanceOf(JsonPathRootNode.class, filter.getExpression());
    }

    @Test
    public void testCurrentInsideFilter() {
        JsonPathNode root = parse("$[?(@)]");

        assertEquals(1, root.getChildren().size());
        JsonPathFilterNode filter = (JsonPathFilterNode) root.getChildren().get(0);

        assertNotNull(filter.getExpression());
        assertInstanceOf(JsonPathCurrentNode.class, filter.getExpression());
    }

    @Test
    public void testCurrentPathInsideFilter() {
        JsonPathNode root = parse("$[?(@.foo)]");

        JsonPathFilterNode filter = (JsonPathFilterNode) root.getChildren().get(0);
        JsonPathExpressionNode expr = filter.getExpression();

        assertInstanceOf(JsonPathCurrentNode.class, expr);
        assertEquals(1, expr.getChildren().size());
        assertInstanceOf(JsonPathFieldNode.class, expr.getChildren().get(0));
    }

    @Test
    public void testRootPathInsideFilter() {
        JsonPathNode root = parse("$[?($.store.book)]");

        JsonPathFilterNode filter = (JsonPathFilterNode) root.getChildren().get(0);
        JsonPathExpressionNode expr = filter.getExpression();

        assertInstanceOf(JsonPathRootNode.class, expr);
        assertEquals(2, expr.getChildren().size());
        assertInstanceOf(JsonPathFieldNode.class, expr.getChildren().get(0));
        assertInstanceOf(JsonPathFieldNode.class, expr.getChildren().get(1));
    }

    @Test
    public void testComparisonWithCurrent() {
        JsonPathNode root = parse("$[?(@.price < 10)]");

        JsonPathFilterNode filter = (JsonPathFilterNode) root.getChildren().get(0);
        JsonPathComparisonExpressionNode cmp =
                (JsonPathComparisonExpressionNode) filter.getExpression();

        assertInstanceOf(JsonPathCurrentNode.class, cmp.getLeft());
        assertInstanceOf(JsonPathLiteralNode.class, cmp.getRight());
    }

    @Test
    public void testComparisonRootEqualsCurrent() {
        JsonPathNode root = parse("$[?($ == @)]");

        JsonPathFilterNode filter = (JsonPathFilterNode) root.getChildren().get(0);
        JsonPathComparisonExpressionNode cmp =
                (JsonPathComparisonExpressionNode) filter.getExpression();

        assertInstanceOf(JsonPathRootNode.class, cmp.getLeft());
        assertInstanceOf(JsonPathCurrentNode.class, cmp.getRight());
    }

    // Function calls

    @Test
    public void testZeroArgFunction() {
        JsonPathNode root = parse("$[?(now())]");

        JsonPathFilterNode filter = (JsonPathFilterNode) root.getChildren().get(0);
        JsonPathExpressionNode expr = filter.getExpression();

        assertInstanceOf(JsonPathFunctionCallNode.class, expr);

        JsonPathFunctionCallNode fn = (JsonPathFunctionCallNode) expr;
        assertEquals("now", fn.getName());
        assertEquals(0, fn.getArguments().size());
    }

    @Test
    public void testSingleArgFunction() {
        JsonPathNode root = parse("$[?(length(@.tags))]");

        JsonPathFilterNode filter = (JsonPathFilterNode) root.getChildren().get(0);
        JsonPathFunctionCallNode fn = (JsonPathFunctionCallNode) filter.getExpression();

        assertEquals("length", fn.getName());
        assertEquals(1, fn.getArguments().size());
        assertInstanceOf(JsonPathCurrentNode.class, fn.getArguments().get(0));
    }

    @Test
    public void testMultiArgFunction() {
        JsonPathNode root = parse("$[?(concat('a','b','c'))]");

        JsonPathFilterNode filter = (JsonPathFilterNode) root.getChildren().get(0);
        JsonPathFunctionCallNode fn = (JsonPathFunctionCallNode) filter.getExpression();

        assertEquals("concat", fn.getName());
        assertEquals(3, fn.getArguments().size());

        assertInstanceOf(JsonPathLiteralNode.class, fn.getArguments().get(0));
        assertInstanceOf(JsonPathLiteralNode.class, fn.getArguments().get(1));
        assertInstanceOf(JsonPathLiteralNode.class, fn.getArguments().get(2));
    }

    @Test
    public void testFunctionCallInComparison() {
        JsonPathNode root = parse("$[?(length(@.tags) > 2)]");

        JsonPathFilterNode filter = (JsonPathFilterNode) root.getChildren().get(0);
        JsonPathComparisonExpressionNode cmp =
                (JsonPathComparisonExpressionNode) filter.getExpression();

        assertInstanceOf(JsonPathFunctionCallNode.class, cmp.getLeft());
        assertInstanceOf(JsonPathLiteralNode.class, cmp.getRight());
    }

    @Test
    public void testFunctionCallWithPathArgument() {
        JsonPathNode root = parse("$[?(min($.prices))]");

        JsonPathFilterNode filter = (JsonPathFilterNode) root.getChildren().get(0);
        JsonPathFunctionCallNode fn = (JsonPathFunctionCallNode) filter.getExpression();

        assertEquals("min", fn.getName());
        assertEquals(1, fn.getArguments().size());
        assertInstanceOf(JsonPathRootNode.class, fn.getArguments().get(0));
    }
}
