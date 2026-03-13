package io.github.qishr.cascara.lang.jsonpath.processor;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.NullReporter;
import io.github.qishr.cascara.common.lang.LanguageOptions;
import io.github.qishr.cascara.common.lang.processor.Parser;
import io.github.qishr.cascara.lang.jsonpath.JsonPathOptions;
import io.github.qishr.cascara.lang.jsonpath.JsonPathDocument;
import io.github.qishr.cascara.lang.jsonpath.JsonPathException;
import io.github.qishr.cascara.lang.jsonpath.ast.*;
import io.github.qishr.cascara.lang.jsonpath.token.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class JsonPathParser implements Parser<JsonPathDocument> {

    private Reporter reporter = new NullReporter();
    private JsonPathOptions options = new JsonPathOptions();

    private List<JsonPathToken> tokens;
    private int index;
    private URI uri;
    private int depth;

    @Override
    public JsonPathParser setReporter(Reporter reporter) {
        this.reporter = reporter;
        return this;
    }

    @Override
    public JsonPathParser setOptions(LanguageOptions<?> options) {
        if (options instanceof JsonPathOptions jp) {
            this.options = jp;
        }
        return this;
    }

    @Override
    public JsonPathDocument parse(String text) throws JsonPathException {
        return parse(text, null);
    }

    @Override
    public JsonPathDocument parse(String text, URI uri) throws JsonPathException {
        this.uri = uri;

        JsonPathTokenizer tokenizer = new JsonPathTokenizer()
                .setReporter(reporter)
                .setOptions(options);

        this.tokens = tokenizer.tokenize(text, uri);
        this.index = 0;
        this.depth = 0;

        JsonPathNode root = parsePath();
        return new JsonPathDocument(root);
    }

    //
    // --- Grammar Methods ---
    //

    private JsonPathNode parsePath() {
        depth++;
        trace("parsePath");
        try {
            if (match(JsonPathTokenType.ROOT)) {
                JsonPathRootNode root = new JsonPathRootNode();
                parsePathTail(root);
                return root;
            }

            if (match(JsonPathTokenType.CURRENT)) {
                JsonPathCurrentNode cur = new JsonPathCurrentNode();
                parsePathTail(cur);
                return cur;
            }

            error("Expected '$' or '@' at start of JsonPath");
            return new JsonPathRootNode();
        } finally {
            depth--;
        }
    }

    private void parsePathTail(JsonPathNode parent) {
        depth++;
        trace("parsePathTail");
        try {
            while (true) {
                if (match(JsonPathTokenType.DOT)) {
                    parent.addChild(parseDotSegment());
                    continue;
                }

                if (match(JsonPathTokenType.RECURSIVE_DESCENT)) {
                    parent.addChild(parseRecursiveSegment());
                    continue;
                }

                if (match(JsonPathTokenType.BRACKET_OPEN)) {
                    parent.addChild(parseBracketSegment());
                    expect(JsonPathTokenType.BRACKET_CLOSE, "Expected ']'");
                    continue;
                }

                break;
            }
        } finally {
            depth--;
        }
    }

    private JsonPathNode parseDotSegment() {
        depth++;
        trace("parseDotSegment");
        try {
            // Case: .*
            if (match(JsonPathTokenType.WILDCARD)) {
                return new JsonPathWildcardNode();
            }

            // Case: .identifier
            if (match(JsonPathTokenType.IDENTIFIER)) {
                JsonPathToken t = previous();
                return new JsonPathFieldNode(t.getLexeme());
            }

            error("Expected field name or '*' after '.'");
            return null;
        } finally {
            depth--;
        }
    }


    private JsonPathNode parseRecursiveSegment() {
        // Case: ..*
        if (match(JsonPathTokenType.WILDCARD)) {
            return new JsonPathRecursiveNode(new JsonPathWildcardNode());
        }

        // Case: ..identifier
        if (match(JsonPathTokenType.IDENTIFIER)) {
            JsonPathToken t = previous();
            return new JsonPathRecursiveNode(new JsonPathFieldNode(t.getLexeme()));
        }

        // Case: ..[ ... ]
        if (match(JsonPathTokenType.BRACKET_OPEN)) {
            JsonPathNode inner = parseBracketSegment();
            expect(JsonPathTokenType.BRACKET_CLOSE, "Expected ']'");
            return new JsonPathRecursiveNode(inner);
        }

        // Case: bare .. (rare but allowed)
        return new JsonPathRecursiveNode(null);
    }

    private JsonPathNode parseBracketSegment() {
        depth++;
        trace("parseBracketSegment");
        try {
            if (match(JsonPathTokenType.FILTER)) {
                return parseFilter();
            }

            // Numeric union vs slice/index
            if (check(JsonPathTokenType.NUMBER)) {
                boolean hasColon = false;
                boolean hasComma = false;

                for (int i = index; i < tokens.size(); i++) {
                    JsonPathTokenType t = tokens.get(i).getType();
                    if (t == JsonPathTokenType.BRACKET_CLOSE) break;
                    if (t == JsonPathTokenType.COLON) { hasColon = true; break; }
                    if (t == JsonPathTokenType.COMMA) { hasComma = true; }
                }

                if (hasComma && !hasColon) {
                    return parseUnion();      // [0,2] → union
                }

                return parseSliceOrIndex();   // [1], [1:3], [:3], etc.
            }

            if (check(JsonPathTokenType.COLON)) {
                return parseSliceOrIndex();
            }

            if (check(JsonPathTokenType.STRING) || check(JsonPathTokenType.IDENTIFIER)) {
                return parseUnion();
            }

            if (match(JsonPathTokenType.WILDCARD)) {
                return new JsonPathWildcardNode();
            }

            error("Unexpected token inside brackets");
            return new JsonPathLiteralNode(null);
        } finally {
            depth--;
        }
    }

    private JsonPathNode parseFilter() {
        depth++;
        trace("parseFilter");
        try {
            expect(JsonPathTokenType.PAREN_OPEN, "Expected '(' after '?'");
            JsonPathExpressionNode expr = parseExpression();
            expect(JsonPathTokenType.PAREN_CLOSE, "Expected ')'");
            return new JsonPathFilterNode(expr);
        } finally {
            depth--;
        }
    }

    private JsonPathNode parseSliceOrIndex() {
        depth++;
        trace("parseSliceOrIndex");
        try {
            // Look ahead: if NUMBER followed by COLON → slice, not index
            if (check(JsonPathTokenType.NUMBER)) {
                JsonPathToken num = peek();
                // If next token after NUMBER is NOT colon → index
                if (tokens.get(index + 1).getType() != JsonPathTokenType.COLON) {
                    match(JsonPathTokenType.NUMBER);
                    return new JsonPathIndexNode(((Number) num.getValue()).intValue());
                }
            }

            // Otherwise: parse slice [start:end:step]
            Integer start = null;
            Integer end   = null;
            Integer step  = null;

            // Optional start
            if (match(JsonPathTokenType.NUMBER)) {
                start = ((Number) previous().getValue()).intValue();
            }

            // Must have colon
            expect(JsonPathTokenType.COLON, "Expected ':' in slice expression");

            // Optional end
            if (match(JsonPathTokenType.NUMBER)) {
                end = ((Number) previous().getValue()).intValue();
            }

            // Optional step
            if (match(JsonPathTokenType.COLON)) {
                if (match(JsonPathTokenType.NUMBER)) {
                    step = ((Number) previous().getValue()).intValue();
                }
            }

            return new JsonPathSliceNode(start, end, step);
        } finally {
            depth--;
        }
    }

    private JsonPathNode parseUnion() {
        depth++;
        trace("parseUnion");
        try {
            List<JsonPathNode> elements = new ArrayList<>();

            // First element
            elements.add(parseUnionElement());

            // Additional elements
            while (match(JsonPathTokenType.COMMA)) {
                elements.add(parseUnionElement());
            }

            return new JsonPathUnionNode(elements);
        } finally {
            depth--;
        }
    }

    private JsonPathNode parseUnionElement() {
        depth++;
        trace("parseUnionElement");
        try {
            // STRING or IDENTIFIER → field name
            if (match(JsonPathTokenType.STRING) || match(JsonPathTokenType.IDENTIFIER)) {
                JsonPathToken t = previous();
                return new JsonPathFieldNode(t.getLexeme());
            }

            // NUMBER → index
            if (match(JsonPathTokenType.NUMBER)) {
                JsonPathToken t = previous();
                return new JsonPathIndexNode(((Number) t.getValue()).intValue());
            }

            error("Unexpected element in union");
            return new JsonPathLiteralNode(null);
        } finally {
            depth--;
        }
    }

    private JsonPathExpressionNode parseExpression() {
        depth++;
        trace("parseExpression");
        try {
            return parseOr();
        } finally {
            depth--;
        }
    }

    private JsonPathExpressionNode parseOr() {
        depth++;
        trace("parseOr");
        try {
            JsonPathExpressionNode expr = parseAnd();

            while (match(JsonPathTokenType.OP_OR)) {
                JsonPathToken op = previous();
                JsonPathExpressionNode right = parseAnd();
                expr = new JsonPathLogicalExpressionNode(expr, right, JsonPathLogicalOperator.OR);
            }

            return expr;
        } finally {
            depth--;
        }
    }

    private JsonPathExpressionNode parseAnd() {
        depth++;
        trace("parseAnd");
        try {
            JsonPathExpressionNode expr = parseComparison();

            while (match(JsonPathTokenType.OP_AND)) {
                JsonPathToken op = previous();
                JsonPathExpressionNode right = parseComparison();
                expr = new JsonPathLogicalExpressionNode(expr, right, JsonPathLogicalOperator.AND);
            }

            return expr;
        } finally {
            depth--;
        }
    }

    private JsonPathExpressionNode parseComparison() {
        depth++;
        trace("parseComparison");
        try {
            JsonPathExpressionNode left = parsePrimary();

            if (match(JsonPathTokenType.OP_EQ) ||
                match(JsonPathTokenType.OP_NE) ||
                match(JsonPathTokenType.OP_LT) ||
                match(JsonPathTokenType.OP_LE) ||
                match(JsonPathTokenType.OP_GT) ||
                match(JsonPathTokenType.OP_GE) ||
                match(JsonPathTokenType.OP_REGEX)) {

                JsonPathToken op = previous();
                JsonPathExpressionNode right = parsePrimary();

                return new JsonPathComparisonExpressionNode(
                        left,
                        right,
                        JsonPathComparisonOperator.fromToken(op.getType())
                );
            }

            return left;
        } finally {
            depth--;
        }
    }

    private JsonPathExpressionNode parsePrimary() {
        depth++;
        trace("parsePrimary");
        try {
            // Parenthesized expression
            if (match(JsonPathTokenType.PAREN_OPEN)) {
                JsonPathExpressionNode expr = parseExpression();
                expect(JsonPathTokenType.PAREN_CLOSE, "Expected ')'");
                return expr;
            }

            // Literals
            if (match(JsonPathTokenType.STRING)) {
                return new JsonPathLiteralNode(previous().getValue());
            }

            if (match(JsonPathTokenType.NUMBER)) {
                return new JsonPathLiteralNode(previous().getValue());
            }

            // Current node path: @.foo.bar
            if (match(JsonPathTokenType.CURRENT)) {
                JsonPathCurrentNode cur = new JsonPathCurrentNode();
                parsePathTail(cur);
                return cur;
            }

            // Root path: $.foo.bar
            if (match(JsonPathTokenType.ROOT)) {
                JsonPathRootNode root = new JsonPathRootNode();
                parsePathTail(root);
                return root;
            }

            // Identifier: could be a function call or a field reference
            if (match(JsonPathTokenType.IDENTIFIER)) {
                JsonPathToken id = previous();

                // Function call
                if (match(JsonPathTokenType.PAREN_OPEN)) {
                    List<JsonPathExpressionNode> args = new ArrayList<>();

                    // Optional argument list
                    if (!check(JsonPathTokenType.PAREN_CLOSE)) {
                        args.add(parseExpression());
                        while (match(JsonPathTokenType.COMMA)) {
                            args.add(parseExpression());
                        }
                    }

                    expect(JsonPathTokenType.PAREN_CLOSE, "Expected ')' after function arguments");

                    return new JsonPathFunctionCallNode(id.getLexeme(), args);
                }

                // Otherwise: identifier literal (field name)
                return new JsonPathFieldNode(id.getLexeme());
            }

            error("Unexpected token in expression: " + peek().getType());
            return new JsonPathLiteralNode(null);
        } finally {
            depth--;
        }
    }


    //
    // --- Token Helpers ---
    //

    private boolean match(JsonPathTokenType type) {
        if (check(type)) {
            index++;
            return true;
        }
        return false;
    }

    private boolean check(JsonPathTokenType type) {
        return !isAtEnd() && peek().getType() == type;
    }

    private JsonPathToken expect(JsonPathTokenType type, String message) {
        if (check(type)) return tokens.get(index++);
        error(message);
        return new JsonPathToken(type, "", null, 0, 0, 0);
    }

    private JsonPathToken peek() {
        return tokens.get(index);
    }

    private JsonPathToken previous() {
        return tokens.get(index - 1);
    }

    private boolean isAtEnd() {
        return peek().getType() == JsonPathTokenType.EOF;
    }

    private void error(String msg) {
        reporter.error(msg);
    }

    //
    // Diagnostics
    //

    /// Log the current method name and upcoming tokens
    private void trace(String methodName) {
        if (reporter == null) return;

        // Create indentation based on recursion depth
        String indent = "  ".repeat(Math.max(0, depth));

        reporter.trace("L%3d C%3d I%3d %s%s: %s",
                tokens.get(index).getStartLine(),
                tokens.get(index).getStartColumn(),
                index,
                indent,
                methodName,
                upcomingTokens());
    }

    // Get next 4 tokens as a string.
    private String upcomingTokens() {
        StringBuilder sb = new StringBuilder();
        int distance = Math.min(tokens.size() - index, 4);
        for (int i = 0; i < distance; i++) {
            JsonPathToken token = tokens.get(index + i);
            sb.append(token.getType());
            sb.append("(");
            sb.append(token.getLexeme().replace("\n", "\\n").replace("\r", "\\r"));
            sb.append(") ");
        }
        return sb.toString();
    }
}

