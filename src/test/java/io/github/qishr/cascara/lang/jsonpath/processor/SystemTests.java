package io.github.qishr.cascara.lang.jsonpath.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.common.lang.simple.SimpleMapNode;
import io.github.qishr.cascara.common.lang.simple.SimpleScalarNode;
import io.github.qishr.cascara.common.lang.simple.SimpleSequenceNode;
import io.github.qishr.cascara.lang.jsonpath.JsonPathOptions;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathFieldNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathIndexNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathRootNode;
import io.github.qishr.cascara.lang.jsonpath.ast.JsonPathUnionNode;

public class SystemTests {

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

    @Test
    public void testParserGeneratedStructureDuplication() {
        // 1. Data Setup - The "6-key" sidebar
        SimpleMapNode root = new SimpleMapNode();
        SimpleMapNode sidebar = new SimpleMapNode();
        sidebar.put("name", new SimpleScalarNode("N"));
        sidebar.put("entity", new SimpleScalarNode("E"));
        sidebar.put("list", new SimpleScalarNode("L"));
        sidebar.put("view", new SimpleScalarNode("V"));
        sidebar.put("constraints", new SimpleScalarNode("C"));

        SimpleSequenceNode items = new SimpleSequenceNode();
        items.add(new SimpleScalarNode("Target"));
        sidebar.put("items", items);

        root.put("sidebar", sidebar);

        // 2. Build the AST using the parser to capture any internal nesting logic
        // String path = "$['sidebar']['items'][0]";
        // Assuming 'parser' and 'astRoot' equivalent to your project setup:
        JsonPathNode pathNode = parser.parse("$['sidebar']['items'][0]").getRoot();

        // 3. Evaluation
        Object result = evaluator.evaluate(pathNode, root);

        // 4. Analysis
        if (result instanceof Collection<?> col) {
            assertEquals(1, col.size(), "Should have found 1 item, but found " + col.size() + ". Contents: " + col);
        } else {
            assertNotNull(result, "Result should not be null");
        }
    }

    @Test
    public void testPathWithInternalWildcard() {
        SimpleMapNode sidebar = new SimpleMapNode();
        sidebar.put("name", new SimpleScalarNode("N"));
        sidebar.put("entity", new SimpleScalarNode("E"));
        sidebar.put("list", new SimpleScalarNode("L"));
        sidebar.put("view", new SimpleScalarNode("V"));
        sidebar.put("constraints", new SimpleScalarNode("C"));

        SimpleSequenceNode items = new SimpleSequenceNode();
        items.add(new SimpleScalarNode("Target"));
        sidebar.put("items", items);

        SimpleMapNode root = new SimpleMapNode();
        root.put("sidebar", sidebar);

        // This mimics a structure where 'sidebar' might be followed by a wildcard
        // or where the parser thinks it needs to 'find' items inside all keys.
        // If the Organizer code uses a path like "$['sidebar'][*]", we'd get 6.
        JsonPathNode pathNode = parser.parse("$['sidebar'][*]").getRoot();

        Object result = evaluator.evaluate(pathNode, root);

        if (result instanceof Collection<?> col) {
            System.out.println("[DEBUG] Count: " + col.size());
            System.out.println("[DEBUG] Types: " + col.stream().map(Object::getClass).toList());
            // If this is 6, we've reproduced the "spreading" behavior.
        }
    }

    @Test
    public void testDeeplyNestedAstOverlapping() {
        // 1. Data: Sidebar with 6 keys
        SimpleMapNode sidebar = new SimpleMapNode();
        sidebar.put("name", new SimpleScalarNode("N"));
        sidebar.put("entity", new SimpleScalarNode("E"));
        sidebar.put("list", new SimpleScalarNode("L"));
        sidebar.put("view", new SimpleScalarNode("V"));
        sidebar.put("constraints", new SimpleScalarNode("C"));

        SimpleSequenceNode items = new SimpleSequenceNode();
        items.add(new SimpleScalarNode("Target"));
        sidebar.put("items", items);

        SimpleMapNode root = new SimpleMapNode();
        root.put("sidebar", sidebar);

        // 2. Build a nested AST that might be "leaking"
        // Root -> Field(sidebar) -> [Child: Field(items) -> [Child: Index(0)]]
        JsonPathFieldNode itemsNode = new JsonPathFieldNode("items");
        itemsNode.addChild(new JsonPathIndexNode(0));

        JsonPathFieldNode sidebarNode = new JsonPathFieldNode("sidebar");
        sidebarNode.addChild(itemsNode);

        JsonPathRootNode rootNode = new JsonPathRootNode();
        rootNode.addChild(sidebarNode);

        // 3. Evaluate
        Object result = evaluator.evaluate(rootNode, root);

        // 4. Assert
        if (result instanceof Collection<?> col) {
            assertEquals(1, col.size(), "Expected 1 result, but got " + col.size() + ". Contents: " + col);
        }
    }

    @Test
    public void testActualYamlListStructure() {
        // 1. DATA: sidebar -> items (List) -> 6 separate Map elements
        SimpleMapNode sidebar = new SimpleMapNode();
        SimpleSequenceNode items = new SimpleSequenceNode();

        // Item 1 has 'name', Item 2 has 'entity', etc.
        String[] properties = {"name", "entity", "list", "view", "constraints", "items"};
        for (String prop : properties) {
            SimpleMapNode element = new SimpleMapNode();
            element.put(prop, new SimpleScalarNode("value-of-" + prop));
            items.add(element);
        }

        sidebar.put("items", items);

        SimpleMapNode root = new SimpleMapNode();
        root.put("sidebar", sidebar);

        // 2. PATH: $['sidebar']['items'][0]
        JsonPathNode pathNode = parser.parse("$['sidebar']['items'][0]").getRoot();

        // 3. EVALUATE
        Object result = evaluator.evaluate(pathNode, root);

        // 4. THE TEST
        if (result instanceof Collection<?> col) {
            // If this returns 6, the evaluator is visiting every element in the 'items' list
            // and returning a result for each, ignoring that [0] should have stopped it.
            assertEquals(1, col.size(), "Found " + col.size() + " results instead of 1.");
        }
    }

    @Test
    public void testExactOrganizerYamlStructure() {
        SimpleMapNode root = new SimpleMapNode();

        SimpleScalarNode initialItem = new SimpleScalarNode("$['sidebar']['items'][0]");
        root.put("initialItem", initialItem);

        root.put("name", new SimpleScalarNode("Project Cascara"));

        SimpleMapNode kanban = new SimpleMapNode();
        kanban.put("entityType", new SimpleScalarNode("task"));
        kanban.put("entityStatusIdColumn", new SimpleScalarNode("status"));
        root.put("kanban", kanban);

        // 1. DATA: Replicating the YAML exactly
        SimpleMapNode sidebar = new SimpleMapNode();
        SimpleSequenceNode items = new SimpleSequenceNode();

        // Map 2: { entity: ... }
        SimpleMapNode m1 = new SimpleMapNode();
        m1.put("name", new SimpleScalarNode("Tasks"));
        m1.put("list", new SimpleScalarNode("Page"));
        items.add(m1);

        SimpleMapNode m2 = new SimpleMapNode();
        m2.put("name", new SimpleScalarNode("Bugs"));
        m2.put("list", new SimpleScalarNode("Page"));
        items.add(m2);

        // Map 3: { list: ... }
        SimpleMapNode m3 = new SimpleMapNode();
        m3.put("name", new SimpleScalarNode("Gantt"));
        m3.put("view", new SimpleScalarNode("all"));
        items.add(m3);

        // Map 4: { view: ... }

        SimpleSequenceNode nestedArray3 = new SimpleSequenceNode();
        nestedArray3.add(new SimpleScalarNode("sprint = $PARENT"));

        SimpleMapNode nestedMap1 = new SimpleMapNode();
        nestedMap1.put("name", new SimpleScalarNode("Current Sprint"));
        nestedMap1.put("entity", new SimpleScalarNode("sprint"));
        nestedMap1.put("constraints", nestedArray3);

        SimpleSequenceNode nestedArray1 = new SimpleSequenceNode();
        nestedArray1.add(new SimpleScalarNode("one"));
        nestedArray1.add(new SimpleScalarNode("two"));

        SimpleSequenceNode nestedArray2 = new SimpleSequenceNode();
        nestedArray2.add(nestedMap1);

        SimpleMapNode m4 = new SimpleMapNode();
        m4.put("name", new SimpleScalarNode("nav"));
        m4.put("entity", new SimpleScalarNode("default"));
        m4.put("constraints", nestedArray1);
        m4.put("items", nestedArray2);
        items.add(m4);


        SimpleMapNode m5 = new SimpleMapNode();
        m5.put("name", new SimpleScalarNode("nav"));
        m5.put("list", new SimpleScalarNode("Page"));
        items.add(m5);

        SimpleMapNode m6 = new SimpleMapNode();
        m6.put("name", new SimpleScalarNode("nav"));
        m6.put("list", new SimpleScalarNode("Page"));
        items.add(m6);

        SimpleMapNode m7 = new SimpleMapNode();
        m7.put("name", new SimpleScalarNode("nav"));
        m7.put("list", new SimpleScalarNode("Page"));
        items.add(m7);

        sidebar.put("items", items);

        root.put("sidebar", sidebar);

        // 2. PATH: $['sidebar']['items'][0]
        JsonPathNode pathNode = parser.parse("$['sidebar']['items'][0]").getRoot();

        // 3. EVALUATE
        Object result = evaluator.evaluate(pathNode, root);

        // 4. THE TEST
        if (result instanceof Collection<?> col) {
            // If the evaluator is branching, it will return all 6 Maps
            // from the 'items' list instead of just the one at index 0.
            assertEquals(1, col.size(), "Found " + col.size() + " results. Results: " + col);
        }
    }

    //
    //
    //

    @Test
    public void testCascaraIdentityReproduction() {
        // 1. DATA: Building the 12-item list exactly like the YAML
        SimpleMapNode root = new SimpleMapNode();
        SimpleMapNode sidebar = new SimpleMapNode();
        SimpleSequenceNode items = new SimpleSequenceNode();

        // Item 0: Tasks
        SimpleMapNode i0 = new SimpleMapNode();
        i0.put("name", new SimpleScalarNode("Tasks"));
        i0.put("list", new SimpleScalarNode("task"));
        items.add(i0);

        // Item 1: Bugs
        SimpleMapNode i1 = new SimpleMapNode();
        i1.put("name", new SimpleScalarNode("Bugs"));
        i1.put("list", new SimpleScalarNode("bug"));
        items.add(i1);

        // Item 2: Gantt
        SimpleMapNode i2 = new SimpleMapNode();
        i2.put("name", new SimpleScalarNode("Gantt"));
        i2.put("view", new SimpleScalarNode("gantt"));
        items.add(i2);

        // Item 3: Current Sprint (The one with 4+ properties)
        SimpleMapNode i3 = new SimpleMapNode();
        i3.put("name", new SimpleScalarNode("Current Sprint"));
        i3.put("entity", new SimpleScalarNode("sprint"));
        i3.put("constraints", new SimpleSequenceNode());
        i3.put("items", new SimpleSequenceNode());
        items.add(i3);

        // Items 4-11: (People, Tags, etc.) - Adding 8 more to reach 12
        for(int i=0; i<8; i++) items.add(new SimpleMapNode());

        sidebar.put("items", items);
        root.put("sidebar", sidebar);

        // 2. AST: Root -> Union(sidebar) -> Union(items) -> Index(0)
        // This replicates the exact sibling structure from your trace
        JsonPathRootNode path = new JsonPathRootNode();
        path.addChild(new JsonPathUnionNode(List.of(new JsonPathFieldNode("sidebar"))));
        path.addChild(new JsonPathUnionNode(List.of(new JsonPathFieldNode("items"))));
        path.addChild(new JsonPathIndexNode(0));

        // 3. EVALUATE
        Object result = evaluator.evaluate(path, root);

        // 4. THE TEST
        if (result instanceof Collection<?> col) {
            // BUG: If evaluatePath sees the List from UnionNode and 'spreads' [0]
            // across the 12 items, this will return size 12.
            assertEquals(1, col.size(), "Expected 1 item, but found " + col.size());
        }
    }
}
