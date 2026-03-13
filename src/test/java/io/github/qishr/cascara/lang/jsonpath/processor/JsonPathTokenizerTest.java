package io.github.qishr.cascara.lang.jsonpath.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.qishr.cascara.lang.jsonpath.token.JsonPathToken;
import io.github.qishr.cascara.lang.jsonpath.token.JsonPathTokenType;
import io.github.qishr.cascara.lang.jsonpath.JsonPathOptions;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.NullReporter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class JsonPathTokenizerTest {

    private JsonPathTokenizer tokenizer;
    private Reporter reporter;

    @BeforeEach
    public void setup() {
        reporter = new SimpleReporter().setLevel(Level.TRACE);
        tokenizer = new JsonPathTokenizer()
                .setReporter(reporter)
                .setOptions(new JsonPathOptions());
    }

    private List<JsonPathToken> tok(String input) {
        return tokenizer.tokenize(input);
    }

    private void assertTypes(List<JsonPathToken> tokens, JsonPathTokenType... expected) {
        assertEquals(expected.length, tokens.size(), "Token count mismatch");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tokens.get(i).getType(), "Token mismatch at index " + i);
        }
    }

    //
    //
    //

    @Test
    public void testRootAndField() {
        var t = tok("$.store.book");
        assertTypes(t,
            JsonPathTokenType.ROOT,
            JsonPathTokenType.DOT,
            JsonPathTokenType.IDENTIFIER,
            JsonPathTokenType.DOT,
            JsonPathTokenType.IDENTIFIER,
            JsonPathTokenType.EOF
        );
    }


    @Test
    public void testRecursiveDescent() {
        var t = tok("$..author");
        assertTypes(t,
            JsonPathTokenType.ROOT,
            JsonPathTokenType.RECURSIVE_DESCENT,
            JsonPathTokenType.IDENTIFIER,
            JsonPathTokenType.EOF
        );
    }


    @Test
    public void testWildcard() {
        var t = tok("$.store.*");
        assertTypes(t,
            JsonPathTokenType.ROOT,
            JsonPathTokenType.DOT,
            JsonPathTokenType.IDENTIFIER,
            JsonPathTokenType.DOT,
            JsonPathTokenType.WILDCARD,
            JsonPathTokenType.EOF
        );
    }

    @Test
    public void testArrayIndex() {
        var t = tok("$.books[3]");
        assertTypes(t,
            JsonPathTokenType.ROOT,
            JsonPathTokenType.DOT,
            JsonPathTokenType.IDENTIFIER,
            JsonPathTokenType.BRACKET_OPEN,
            JsonPathTokenType.NUMBER,
            JsonPathTokenType.BRACKET_CLOSE,
            JsonPathTokenType.EOF
        );
    }

    @Test
    public void testSlice() {
        var t = tok("$.books[1:5:2]");
        assertTypes(t,
            JsonPathTokenType.ROOT,
            JsonPathTokenType.DOT,
            JsonPathTokenType.IDENTIFIER,
            JsonPathTokenType.BRACKET_OPEN,
            JsonPathTokenType.NUMBER,
            JsonPathTokenType.COLON,
            JsonPathTokenType.NUMBER,
            JsonPathTokenType.COLON,
            JsonPathTokenType.NUMBER,
            JsonPathTokenType.BRACKET_CLOSE,
            JsonPathTokenType.EOF
        );
    }

    @Test
    public void testUnion() {
        var t = tok("$.books[1,2,'foo']");
        assertTypes(t,
            JsonPathTokenType.ROOT,
            JsonPathTokenType.DOT,
            JsonPathTokenType.IDENTIFIER,
            JsonPathTokenType.BRACKET_OPEN,
            JsonPathTokenType.NUMBER,
            JsonPathTokenType.COMMA,
            JsonPathTokenType.NUMBER,
            JsonPathTokenType.COMMA,
            JsonPathTokenType.STRING,
            JsonPathTokenType.BRACKET_CLOSE,
            JsonPathTokenType.EOF
        );
    }

    @Test
    public void testFilter() {
        var t = tok("$.books[?(@.price < 10)]");
        assertTypes(t,
            JsonPathTokenType.ROOT,
            JsonPathTokenType.DOT,
            JsonPathTokenType.IDENTIFIER,
            JsonPathTokenType.BRACKET_OPEN,
            JsonPathTokenType.FILTER,
            JsonPathTokenType.PAREN_OPEN,
            JsonPathTokenType.CURRENT,
            JsonPathTokenType.DOT,
            JsonPathTokenType.IDENTIFIER,
            JsonPathTokenType.OP_LT,
            JsonPathTokenType.NUMBER,
            JsonPathTokenType.PAREN_CLOSE,
            JsonPathTokenType.BRACKET_CLOSE,
            JsonPathTokenType.EOF
        );
    }

    @Test
    public void testRegex() {
        tokenizer.setOptions(new JsonPathOptions().setAllowRegex(true));
        var t = tok("$.books[?(@.title =~ '.*Java.*')]");
        assertTypes(t,
            JsonPathTokenType.ROOT,
            JsonPathTokenType.DOT,
            JsonPathTokenType.IDENTIFIER,
            JsonPathTokenType.BRACKET_OPEN,
            JsonPathTokenType.FILTER,
            JsonPathTokenType.PAREN_OPEN,
            JsonPathTokenType.CURRENT,
            JsonPathTokenType.DOT,
            JsonPathTokenType.IDENTIFIER,
            JsonPathTokenType.OP_REGEX,
            JsonPathTokenType.STRING,
            JsonPathTokenType.PAREN_CLOSE,
            JsonPathTokenType.BRACKET_CLOSE,
            JsonPathTokenType.EOF
        );
    }

    @Test
    public void testStringLiteral() {
        var t = tok("$['weird-key']");
        assertTypes(t,
            JsonPathTokenType.ROOT,
            JsonPathTokenType.BRACKET_OPEN,
            JsonPathTokenType.STRING,
            JsonPathTokenType.BRACKET_CLOSE,
            JsonPathTokenType.EOF
        );
    }

    @Test
    public void testNumberLiteral() {
        var t = tok("$.nums[3.14]");
        assertTypes(t,
            JsonPathTokenType.ROOT,
            JsonPathTokenType.DOT,
            JsonPathTokenType.IDENTIFIER,
            JsonPathTokenType.BRACKET_OPEN,
            JsonPathTokenType.NUMBER,
            JsonPathTokenType.BRACKET_CLOSE,
            JsonPathTokenType.EOF
        );
    }

}
