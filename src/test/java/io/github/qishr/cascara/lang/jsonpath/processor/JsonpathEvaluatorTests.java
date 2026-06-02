package io.github.qishr.cascara.lang.jsonpath.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.ast.MapEntryAstNode;
import io.github.qishr.cascara.common.lang.simple.SimpleMapNode;
import io.github.qishr.cascara.common.lang.simple.SimpleScalarNode;
import io.github.qishr.cascara.common.lang.simple.SimpleSequenceNode;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.lang.jsonpath.JsonPathOptions;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathFieldNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathIndexNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathRootNode;

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

    @Test
    public void testDirectPathDuplication() {
        // Setup: A structure where a key exists once, but could be "seen"
        // multiple times if the evaluator is over-iterating.
        SimpleMapNode root = new SimpleMapNode();
        SimpleMapNode sidebar = new SimpleMapNode();
        SimpleSequenceNode items = new SimpleSequenceNode();

        SimpleMapNode item0 = new SimpleMapNode();
        item0.put("name", new SimpleScalarNode("Home"));

        items.add(item0);
        items.add(new SimpleScalarNode("Settings"));

        sidebar.put("items", items);
        root.put("sidebar", sidebar);

        // Path: $['sidebar']['items'][0]
        // The result should be the 'item0' MapNode (or its name if pathed further)
        JsonPathNode path = parse("$['sidebar']['items'][0]");
        Object result = evaluator.evaluate(path, root);

        // If the bug exists, this might return a List of 6 identical items
        // or fail the equality check because of unexpected nesting.
        assertFalse(result instanceof Collection && ((Collection<?>) result).size() > 1,
            "Expected 1 result, but got multiple: " + result);

        // Validate the specific content
        assertTrue(result instanceof MapAstNode, "Expected a MapAstNode");
    }

    @Test
    public void testDirectPathAgainstCollectionInput() {
        // Setup: A list containing two identical sidebar structures
        SimpleMapNode sidebar = new SimpleMapNode();
        SimpleSequenceNode items = new SimpleSequenceNode();
        items.add(new SimpleScalarNode("Item1"));
        sidebar.put("items", items);

        SimpleSequenceNode rootList = new SimpleSequenceNode();
        rootList.add(sidebar);
        rootList.add(sidebar); // Two entries

        // Path: $['items'][0] evaluated against the LIST
        // If our evaluatePath "smart mapping" is too aggressive,
        // it might be multiplying results here.
        JsonPathNode path = parse("$['items'][0]");
        Object result = evaluator.evaluate(path, rootList);

        System.out.println("[DEBUG] Result type: " + (result == null ? "null" : result.getClass().getSimpleName()));
        System.out.println("[DEBUG] Result value: " + result);

        if (result instanceof Collection<?> col) {
            assertEquals(1, col.size(), "Should not multiply results for direct access");
        }
    }

    @Test
    public void testMapEntryBranching() {
        SimpleMapNode sidebar = new SimpleMapNode();
        sidebar.put("name", new SimpleScalarNode("Side"));
        sidebar.put("entity", new SimpleScalarNode("Ent"));
        sidebar.put("list", new SimpleScalarNode("L"));
        sidebar.put("view", new SimpleScalarNode("V"));
        sidebar.put("constraints", new SimpleScalarNode("C"));

        SimpleSequenceNode items = new SimpleSequenceNode();
        items.add(new SimpleScalarNode("Target"));
        sidebar.put("items", items);

        SimpleMapNode root = new SimpleMapNode();
        root.put("sidebar", sidebar);

        // Path: $['sidebar']['items']
        // If the bug exists, evaluating ['items'] against the 'sidebar' Map
        // might see the Map as a collection of 6 entries and try to evaluate
        // against each, or return all 6 if the logic is scrambled.
        JsonPathNode path = parse("$['sidebar']['items']");
        Object result = evaluator.evaluate(path, root);

        if (result instanceof Collection<?> col) {
            assertEquals(1, col.size(), "Expected 1 result (the items list), but got: " + col.size());
        }
    }

    @Test
    public void testMapEntryLeakage() {
        SimpleMapNode sidebar = new SimpleMapNode();
        sidebar.put("name", new SimpleScalarNode("Side"));
        sidebar.put("entity", new SimpleScalarNode("Ent"));
        sidebar.put("list", new SimpleScalarNode("L"));
        sidebar.put("view", new SimpleScalarNode("V"));
        sidebar.put("constraints", new SimpleScalarNode("C"));
        sidebar.put("items", new SimpleSequenceNode());

        SimpleMapNode root = new SimpleMapNode();
        root.put("sidebar", sidebar);

        // Path: $['sidebar']
        // If this returns a List of 6 entries instead of 1 MapNode,
        // the evaluator is "spreading" the map into its entries.
        JsonPathNode path = parse("$['sidebar']");
        Object result = evaluator.evaluate(path, root);

        if (result instanceof Collection<?> col) {
            // If this is 6, evaluateField or the Root loop is spreading the map.
            assertEquals(1, col.size(), "Should return the Map itself, not its 6 entries");
            Object item = col.iterator().next();
            assertFalse(item instanceof MapEntryAstNode, "Result should not be a MapEntry");
        }
    }

    @Test
    public void testMapAsCollectionCollision() {
        SimpleMapNode sidebar = new SimpleMapNode();
        sidebar.put("name", new SimpleScalarNode("a"));
        sidebar.put("entity", new SimpleScalarNode("b"));
        sidebar.put("list", new SimpleScalarNode("c"));
        sidebar.put("view", new SimpleScalarNode("d"));
        sidebar.put("constraints", new SimpleScalarNode("e"));
        sidebar.put("items", new SimpleSequenceNode());

        SimpleMapNode root = new SimpleMapNode();
        root.put("sidebar", sidebar);

        // If MapAstNode implements Collection, this path will trigger the
        // "map over collection" logic and return 6 results (the entries).
        JsonPathNode path = parse("$['sidebar']");
        Object result = evaluator.evaluate(path, root);

        if (result instanceof List<?> list) {
            assertEquals(1, list.size(), "Should have 1 Map, but found " + list.size() + " items (likely MapEntries)");
        }
    }

    @Test
    public void testMapEntryTraversal() {
        SimpleMapNode sidebar = new SimpleMapNode();
        sidebar.put("name", new SimpleScalarNode("Side"));
        sidebar.put("entity", new SimpleScalarNode("Ent"));
        sidebar.put("list", new SimpleScalarNode("L"));
        sidebar.put("view", new SimpleScalarNode("V"));
        sidebar.put("constraints", new SimpleScalarNode("C"));
        sidebar.put("items", new SimpleSequenceNode());

        SimpleMapNode root = new SimpleMapNode();
        root.put("sidebar", sidebar);

        // If the walker treats MapEntryAstNode as a traversable child,
        // a recursive search for '*' might return the entries themselves.
        JsonPathNode path = parse("$..*");
        Object result = evaluator.evaluate(path, root);

        if (result instanceof Collection<?> col) {
            for (Object o : col) {
                // MapEntryAstNode should NEVER be a result; only their values should.
                assertFalse(o instanceof MapEntryAstNode, "Evaluator returned a MapEntryAstNode");
            }
        }
    }

    @Test
    public void testSiblingPathBranching() {
        // 1. Setup the data: A Map with 6 keys
        SimpleMapNode sidebar = new SimpleMapNode();
        sidebar.put("name", new SimpleScalarNode("N"));
        sidebar.put("entity", new SimpleScalarNode("E"));
        sidebar.put("list", new SimpleScalarNode("L"));
        sidebar.put("view", new SimpleScalarNode("V"));
        sidebar.put("constraints", new SimpleScalarNode("C"));

        SimpleSequenceNode itemsList = new SimpleSequenceNode();
        itemsList.add(new SimpleScalarNode("Target")); // This is the [0]
        sidebar.put("items", itemsList);

        SimpleMapNode root = new SimpleMapNode();
        root.put("sidebar", sidebar);

        // 2. Manually construct the AST siblings:
        // Root -> Field(sidebar) -> Field(items) -> Index(0)
        // We make them siblings under Root to match your "I already know they're siblings" point.
        JsonPathRootNode path = new JsonPathRootNode();
        JsonPathFieldNode f1 = new JsonPathFieldNode("sidebar");
        JsonPathFieldNode f2 = new JsonPathFieldNode("items");
        JsonPathIndexNode i3 = new JsonPathIndexNode(0);

        path.addChild(f1);
        path.addChild(f2);
        path.addChild(i3);

        // 3. Evaluate
        Object result = evaluator.evaluate(path, root);

        // 4. Assert
        if (result instanceof Collection<?> col) {
            assertEquals(1, col.size(), "Should have 1 result, but found: " + col.size() +
                ". Results are: " + col);
        } else {
            assertNotNull(result, "Result should not be null");
            assertEquals("Target", result);
        }
    }
}
