package io.github.qishr.cascara.lang.jsonpath.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.lang.simple.SimpleMapNode;
import io.github.qishr.cascara.common.lang.simple.SimpleScalarNode;
import io.github.qishr.cascara.common.lang.simple.SimpleSequenceNode;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.lang.jsonpath.JsonPathOptions;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathNode;

class JsonpathEvaluatorTests {
    private JsonPathParser parser;
    private JsonPathEvaluator evaluator;

    @BeforeEach
    public void setup() {
        parser = new JsonPathParser()
                .setReporter(new SimpleReporter().setLevel(Level.TRACE))
                .setOptions(new JsonPathOptions());
        evaluator = new JsonPathEvaluator();
    }

    private JsonPathNode parse(String input) {
        return parser.parse(input).getRoot();
    }

    //
    // Tests
    //

    @Test
    public void testWildcardOnMap() {
        // Was: Map<String,Object> json = new TreeMap<>(Map.of( "a", 1, "b", 2, "c", 3 ));

        SimpleMapNode json = new SimpleMapNode();
        SimpleScalarNode scalar1 = new SimpleScalarNode(1);
        SimpleScalarNode scalar2 = new SimpleScalarNode(2);
        SimpleScalarNode scalar3 = new SimpleScalarNode(3);
        json.put("a", scalar1);
        json.put("b", scalar2);
        json.put("c", scalar3);


        System.out.println(Arrays.toString("$. *".toCharArray()));

        JsonPathNode path = parse("$. *");
        Object result = evaluator.evaluate(path, json);

        assertInstanceOf(List.class, result);
        List<?> list = (List<?>) result;
        assertEquals(scalar1, list.get(0));
        assertEquals(scalar2, list.get(1));
        assertEquals(scalar3, list.get(2));
    }

    @Test
    public void testWildcardOnList() {
        // Was: List<Object> json = List.of(1,2,3);

        SimpleSequenceNode json = new SimpleSequenceNode();
        SimpleScalarNode scalar1 = new SimpleScalarNode(1);
        SimpleScalarNode scalar2 = new SimpleScalarNode(2);
        SimpleScalarNode scalar3 = new SimpleScalarNode(3);
        json.add(scalar1);
        json.add(scalar2);
        json.add(scalar3);

        JsonPathNode path = parse("$[*]");
        Object result = evaluator.evaluate(path, json);

        assertInstanceOf(List.class, result);
        List<?> list = (List<?>) result;
        assertEquals(scalar1, list.get(0));
        assertEquals(scalar2, list.get(1));
        assertEquals(scalar3, list.get(2));
    }

    @Test
    public void testWildcardInsideFilter() {
        // $.items[?(@.*)]
        SimpleMapNode json = new SimpleMapNode();
        SimpleSequenceNode items = new SimpleSequenceNode();

        SimpleMapNode item1 = new SimpleMapNode();
        item1.put("x", new SimpleScalarNode(1));
        SimpleMapNode item2 = new SimpleMapNode();
        item2.put("x", new SimpleScalarNode(2));

        items.add(item1);
        items.add(item2);
        json.put("items", items);

        JsonPathNode path = parse("$.items[?(@.*)]");
        Object result = evaluator.evaluate(path, json);

        assertInstanceOf(List.class, result);
        List<?> resList = (List<?>) result;
        assertEquals(2, resList.size());
        assertEquals(item1, resList.get(0));
    }

    @Test
    public void testRecursiveField() {
        // $..author
        SimpleMapNode json = new SimpleMapNode();
        SimpleMapNode store = new SimpleMapNode();
        SimpleSequenceNode books = new SimpleSequenceNode();

        SimpleMapNode book1 = new SimpleMapNode();
        book1.put("author", new SimpleScalarNode("A"));
        SimpleMapNode book2 = new SimpleMapNode();
        book2.put("author", new SimpleScalarNode("B"));

        books.add(book1);
        books.add(book2);
        store.put("book", books);
        json.put("store", store);

        JsonPathNode path = parse("$..author");
        Object result = evaluator.evaluate(path, json);

        assertInstanceOf(List.class, result);
        assertTrue(((List<?>) result).contains("A"));
        assertTrue(((List<?>) result).contains("B"));
    }

    @Test
    public void testRecursiveIndex() {
        // $..[1]
        SimpleMapNode json = new SimpleMapNode();
        SimpleSequenceNode root = new SimpleSequenceNode();

        SimpleSequenceNode sub1 = new SimpleSequenceNode();
        sub1.add(new SimpleScalarNode(1));
        SimpleScalarNode target1 = new SimpleScalarNode(2);
        sub1.add(target1);

        SimpleSequenceNode sub2 = new SimpleSequenceNode();
        sub2.add(new SimpleScalarNode(3));
        SimpleScalarNode target2 = new SimpleScalarNode(4);
        sub2.add(target2);

        root.add(sub1);
        root.add(sub2);
        json.put("root", root);

        JsonPathNode path = parse("$..[1]");
        Object result = evaluator.evaluate(path, json);

        assertInstanceOf(List.class, result);
        List<?> resList = (List<?>) result;

        // Change: Check for the primitive values, not the Node objects
        assertTrue(resList.contains(2), "Result should contain the unwrapped value 2");
        assertTrue(resList.contains(4), "Result should contain the unwrapped value 4");
    }

    @Test
    public void testRecursiveBookFirstTitle() {
        // $..book[0].title
        SimpleMapNode json = new SimpleMapNode();
        SimpleMapNode store = new SimpleMapNode();
        SimpleSequenceNode books = new SimpleSequenceNode();

        SimpleMapNode book1 = new SimpleMapNode();
        book1.put("title", new SimpleScalarNode("T1"));
        books.add(book1);
        store.put("book", books);
        json.put("store", store);

        JsonPathNode path = parse("$..book[0].title");
        Object result = evaluator.evaluate(path, json);

        // Should return the primitive value if evaluateExpression unwraps it
        // assertEquals("T1", result);
        assertEquals(List.of("T1"), result);
    }

    @Test
    public void testRecursiveFilter() {
        // $..[?(@.price < 10)]
        SimpleMapNode json = new SimpleMapNode();
        SimpleSequenceNode books = new SimpleSequenceNode();

        SimpleMapNode b1 = new SimpleMapNode();
        b1.put("price", new SimpleScalarNode(5));
        SimpleMapNode b2 = new SimpleMapNode();
        b2.put("price", new SimpleScalarNode(15));

        books.add(b1);
        books.add(b2);
        json.put("books", books);

        JsonPathNode path = parse("$..[?(@.price < 10)]");
        Object result = evaluator.evaluate(path, json);

        assertInstanceOf(List.class, result);
        List<?> res = (List<?>) result;
        assertEquals(1, res.size());
        assertEquals(b1, res.get(0));
    }

    @Test
    public void testRecursiveNulls() {
        SimpleMapNode json = new SimpleMapNode();
        SimpleSequenceNode a = new SimpleSequenceNode();
        a.add(null); // Explicit null test

        SimpleMapNode wrap = new SimpleMapNode();
        wrap.put("x", new SimpleScalarNode(1));
        a.add(wrap);

        json.put("a", a);
        json.put("b", null);

        JsonPathNode path = parse("$..x");
        Object result = evaluator.evaluate(path, json);

        assertEquals(List.of(1), result);
    }

    @Test
    public void testRecursiveDeepNesting() {
        // a.b.c.d = 42
        SimpleMapNode a = new SimpleMapNode();
        SimpleMapNode b = new SimpleMapNode();
        SimpleMapNode c = new SimpleMapNode();
        c.put("d", new SimpleScalarNode(42));
        b.put("c", c);
        a.put("b", b);

        SimpleMapNode root = new SimpleMapNode();
        root.put("a", a);

        JsonPathNode path = parse("$..d");
        Object result = evaluator.evaluate(path, root);

        assertEquals(List.of(42), result);
    }
}
