package io.github.qishr.cascara.lang.jsonpath.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
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
        // Map<String,Object> json = Map.of(
        //     "a", 1,
        //     "b", 2,
        //     "c", 3
        // );
        Map<String,Object> json = new TreeMap<>(Map.of( "a", 1, "b", 2, "c", 3 ));


        System.out.println(Arrays.toString("$. *".toCharArray()));

        JsonPathNode path = parse("$. *");
        Object result = evaluator.evaluate(path, json);

        assertEquals(List.of(1,2,3), result);
    }

    @Test
    public void testWildcardOnList() {
        List<Object> json = List.of(1,2,3);

        JsonPathNode path = parse("$[*]");
        Object result = evaluator.evaluate(path, json);

        assertEquals(List.of(1,2,3), result);
    }

    @Test
    public void testWildcardInsideFilter() {
        Map<String,Object> json = Map.of(
            "items", List.of(
                Map.of("x", 1),
                Map.of("x", 2),
                Map.of("x", 3)
            )
        );

        JsonPathNode path = parse("$.items[?(@.*)]");
        Object result = evaluator.evaluate(path, json);

        assertEquals(List.of(
            Map.of("x", 1),
            Map.of("x", 2),
            Map.of("x", 3)
        ), result);
    }

    @Test
    public void testRecursiveField() {
        Map<String,Object> json = Map.of(
            "store", Map.of(
                "book", List.of(
                    Map.of("author", "A"),
                    Map.of("author", "B")
                ),
                "bicycle", Map.of("color", "red")
            )
        );

        JsonPathNode path = parse("$..author");
        Object result = evaluator.evaluate(path, json);

        assertEquals(List.of("A", "B"), result);
    }

    @Test
    public void testRecursiveWildcard() {
        Map<String,Object> json = Map.of(
            "a", Map.of("x", 1),
            "b", List.of(2, 3)
        );

        JsonPathNode path = parse("$..*");
        Object result = evaluator.evaluate(path, json);

        // Should contain all nested values
        assertTrue(((List<?>) result).containsAll(List.of(1,2,3)));
    }

    @Test
    public void testRecursiveIndex() {
        Map<String,Object> json = Map.of(
            "root", List.of(
                List.of(1,2),
                List.of(3,4)
            )
        );

        JsonPathNode path = parse("$..[1]");
        Object result = evaluator.evaluate(path, json);

        assertEquals(List.of(2,4), result);
    }

    //

    @Test
    public void testRecursiveFieldSimple() {
        Map<String,Object> json = Map.of(
            "a", Map.of("b", 1),
            "c", Map.of("b", 2)
        );

        JsonPathNode path = parse("$..b");
        Object result = evaluator.evaluate(path, json);

        // assertEquals(List.of(1,2), result);
        assertTrue(new java.util.HashSet<>((List<?>) result).containsAll(List.of(1,2)));
    }

    @Test
    public void testRecursiveWildcardSimple() {
        Map<String,Object> json = Map.of(
            "a", Map.of("x", 1),
            "b", List.of(2, 3)
        );

        JsonPathNode path = parse("$..*");
        Object result = evaluator.evaluate(path, json);

        assertTrue(((List<?>) result).containsAll(List.of(1,2,3)));
    }

    @Test
    public void testRecursiveIndexSimple() {
        Map<String,Object> json = Map.of(
            "root", List.of(
                List.of(1,2),
                List.of(3,4)
            )
        );

        JsonPathNode path = parse("$..[1]");
        Object result = evaluator.evaluate(path, json);

        assertEquals(List.of(2,4), result);
    }

    @Test
    public void testRecursiveBookAuthors() {
        Map<String,Object> json = Map.of(
            "store", Map.of(
                "book", List.of(
                    Map.of("author", "A"),
                    Map.of("author", "B")
                )
            )
        );

        // JsonPathNode path = parse("$..book[*].author");
        // Object result = evaluator.evaluate(path, json);
        // assertEquals(List.of("A","B"), result);

        JsonPathNode path = parse("$..author");
        Object result = evaluator.evaluate(path, json);
        assertEquals(List.of("A","B"), result);
    }

    @Test
    public void testRecursiveBookFirstTitle() {
        Map<String,Object> json = Map.of(
            "store", Map.of(
                "book", List.of(
                    Map.of("title", "T1"),
                    Map.of("title", "T2")
                )
            )
        );

        JsonPathNode path = parse("$..book[0].title");
        Object result = evaluator.evaluate(path, json);

        assertEquals("T1", result);
    }

    @Test
    public void testRecursiveFilter() {
        Map<String,Object> json = Map.of(
            "store", Map.of(
                "book", List.of(
                    Map.of("price", 5),
                    Map.of("price", 15),
                    Map.of("price", 7)
                )
            )
        );

        JsonPathNode path = parse("$..[?(@.price < 10)]");
        Object result = evaluator.evaluate(path, json);

        assertEquals(
            List.of(
                Map.of("price", 5),
                Map.of("price", 7)
            ),
            result
        );
    }

    @Test
    public void testRecursiveUnion() {
        Map<String,Object> json = Map.of(
            "root", List.of(
                List.of(1,2,3),
                List.of(4,5,6)
            )
        );

        JsonPathNode path = parse("$..[0,2]");
        Object result = evaluator.evaluate(path, json);

        assertEquals(List.of(1,3,4,6), result);
    }

    @Test
    public void testRecursiveSlice() {
        Map<String,Object> json = Map.of(
            "root", List.of(
                List.of(1,2,3,4),
                List.of(5,6,7,8)
            )
        );

        JsonPathNode path = parse("$..[1:3]");
        Object result = evaluator.evaluate(path, json);

        assertEquals(List.of(2,3,6,7), result);
    }

    @Test
    public void testRecursiveNoMatches() {
        Map<String,Object> json = Map.of("a", 1);

        JsonPathNode path = parse("$..missing");
        Object result = evaluator.evaluate(path, json);

        assertEquals(List.of(), result);
    }

    @Test
    public void testRecursiveNulls() {
        // Map<String,Object> json = Map.of(
        //     "a", List.of(null, Map.of("x", 1)),
        //     "b", null
        // );

        Map<String,Object> json = new HashMap<>();
        json.put("a", Arrays.asList(null, Map.of("x", 1)));
        json.put("b", null);


        JsonPathNode path = parse("$..x");
        Object result = evaluator.evaluate(path, json);

        assertEquals(List.of(1), result);
    }

    @Test
    public void testRecursiveDeepNesting() {
        Map<String,Object> json = Map.of(
            "a", Map.of(
                "b", Map.of(
                    "c", Map.of(
                        "d", 42
                    )
                )
            )
        );

        JsonPathNode path = parse("$..d");
        Object result = evaluator.evaluate(path, json);

        assertEquals(List.of(42), result);
    }

}
