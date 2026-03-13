package io.github.qishr.cascara.lang.jsonpath.processor;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.NullReporter;
import io.github.qishr.cascara.common.lang.LanguageOptions;
import io.github.qishr.cascara.common.lang.processor.Tokenizer;
import io.github.qishr.cascara.lang.jsonpath.JsonPathOptions;
import io.github.qishr.cascara.lang.jsonpath.token.JsonPathToken;
import io.github.qishr.cascara.lang.jsonpath.token.JsonPathTokenType;

public class JsonPathTokenizer implements Tokenizer<JsonPathToken> {

    private Reporter reporter = new NullReporter();
    private JsonPathOptions options = new JsonPathOptions();
    private int offset = 0;
    private int line = 1;
    private int col = 1;
    private char currentChar = 0;
    private List<JsonPathToken> tokens = new ArrayList<>();

    @Override
    public JsonPathTokenizer setReporter(Reporter reporter) {
        this.reporter = reporter;
        return this;
    }

    @Override
    public JsonPathTokenizer setOptions(LanguageOptions<?> options) {
        if (options instanceof JsonPathOptions jp) {
            this.options = jp;
        }
        return this;
    }

    @Override
    public List<JsonPathToken> tokenize(String source) {
        return tokenize(source, null);
    }

    @Override
    public List<JsonPathToken> tokenize(String source, URI uri) {
        tokens = new ArrayList<>();
        offset = 0;
        line = 1;
        col = 1;

        while (offset < source.length()) {
            currentChar = source.charAt(offset);

            // Skip whitespace
            if (Character.isWhitespace(currentChar)) {
                offset++;
                col++;
                continue;
            }

            // Multi-character operators (must be checked first)
            if (source.startsWith("..", offset)) {
                addToken(tok(JsonPathTokenType.RECURSIVE_DESCENT, "..", offset, line, col));
                offset += 2;
                col += 2;
                continue;
            }
            if (source.startsWith("==", offset)) { addToken(tok(JsonPathTokenType.OP_EQ, "==", offset, line, col)); offset+=2; col+=2; continue; }
            if (source.startsWith("!=", offset)) { addToken(tok(JsonPathTokenType.OP_NE, "!=", offset, line, col)); offset+=2; col+=2; continue; }
            if (source.startsWith("<=", offset)) { addToken(tok(JsonPathTokenType.OP_LE, "<=", offset, line, col)); offset+=2; col+=2; continue; }
            if (source.startsWith(">=", offset)) { addToken(tok(JsonPathTokenType.OP_GE, ">=", offset, line, col)); offset+=2; col+=2; continue; }
            if (source.startsWith("&&", offset)) { addToken(tok(JsonPathTokenType.OP_AND, "&&", offset, line, col)); offset+=2; col+=2; continue; }
            if (source.startsWith("||", offset)) { addToken(tok(JsonPathTokenType.OP_OR, "||", offset, line, col)); offset+=2; col+=2; continue; }
            if (source.startsWith("=~", offset) && options.isAllowRegex()) {
                addToken(tok(JsonPathTokenType.OP_REGEX, "=~", offset, line, col));
                offset += 2;
                col += 2;
                continue;
            }

            // Single-character tokens
            switch (currentChar) {
                case '<':
                    addToken(tok(JsonPathTokenType.OP_LT, "<", offset, line, col));
                    offset++;
                    col++;
                    continue;

                case '>':
                    addToken(tok(JsonPathTokenType.OP_GT, ">", offset, line, col));
                    offset++;
                    col++;
                    continue;

                case '$': addToken(tok(JsonPathTokenType.ROOT, "$", offset, line, col)); offset++; col++; continue;
                case '@': addToken(tok(JsonPathTokenType.CURRENT, "@", offset, line, col)); offset++; col++; continue;
                case '.': addToken(tok(JsonPathTokenType.DOT, ".", offset, line, col)); offset++; col++; continue;
                case '*': addToken(tok(JsonPathTokenType.WILDCARD, "*", offset, line, col)); offset++; col++; continue;
                case '[': addToken(tok(JsonPathTokenType.BRACKET_OPEN, "[", offset, line, col)); offset++; col++; continue;
                case ']': addToken(tok(JsonPathTokenType.BRACKET_CLOSE, "]", offset, line, col)); offset++; col++; continue;
                case '(': addToken(tok(JsonPathTokenType.PAREN_OPEN, "(", offset, line, col)); offset++; col++; continue;
                case ')': addToken(tok(JsonPathTokenType.PAREN_CLOSE, ")", offset, line, col)); offset++; col++; continue;
                case ',': addToken(tok(JsonPathTokenType.COMMA, ",", offset, line, col)); offset++; col++; continue;
                case ':': addToken(tok(JsonPathTokenType.COLON, ":", offset, line, col)); offset++; col++; continue;
                case '?': addToken(tok(JsonPathTokenType.FILTER, "?", offset, line, col)); offset++; col++; continue;
            }

            // String literal
            if (currentChar == '"' || currentChar == '\'') {
                int startCol = col;
                char quote = currentChar;
                int startOffset = offset;
                offset++; col++;

                StringBuilder sb = new StringBuilder();
                boolean closed = false;

                while (offset < source.length()) {
                    char ch = source.charAt(offset);
                    if (ch == quote) {
                        closed = true;
                        offset++; col++;
                        break;
                    }
                    sb.append(ch);
                    offset++; col++;
                }

                if (!closed) {
                    reporter.errorAt(line, startCol, uri, "Unterminated string literal");
                }

                addToken(new JsonPathToken(
                        JsonPathTokenType.STRING,
                        sb.toString(),
                        sb.toString(),
                        startOffset,
                        line,
                        startCol
                ));
                continue;
            }

            // Number literal
            if (Character.isDigit(currentChar) || currentChar == '-') {
                int startOffset = offset;
                int startCol = col;
                StringBuilder sb = new StringBuilder();
                sb.append(currentChar);
                offset++; col++;

                while (offset < source.length()) {
                    char ch = source.charAt(offset);
                    if (Character.isDigit(ch) || ch == '.' || ch == 'e' || ch == 'E' || ch == '+' || ch == '-') {
                        sb.append(ch);
                        offset++; col++;
                    } else break;
                }

                addToken(new JsonPathToken(
                        JsonPathTokenType.NUMBER,
                        sb.toString(),
                        parseNumber(sb.toString()),
                        startOffset,
                        line,
                        startCol
                ));
                continue;
            }

            // Identifier
            if (Character.isJavaIdentifierStart(currentChar)) {
                int startOffset = offset;
                int startCol = col;
                StringBuilder sb = new StringBuilder();
                sb.append(currentChar);
                offset++; col++;

                while (offset < source.length()) {
                    char ch = source.charAt(offset);
                    if (Character.isJavaIdentifierPart(ch) || ch == '-') {
                        sb.append(ch);
                        offset++; col++;
                    } else break;
                }

                addToken(new JsonPathToken(
                        JsonPathTokenType.IDENTIFIER,
                        sb.toString(),
                        sb.toString(),
                        startOffset,
                        line,
                        startCol
                ));
                continue;
            }

            // Unknown character
            reporter.errorAt(line, col, uri, "Unexpected character: " + currentChar);
            offset++;
            col++;
        }

        addToken(tok(JsonPathTokenType.EOF, "", offset, line, col));
        return tokens;
    }

    private Number parseNumber(String s) {
        try {
            if (s.contains(".") || s.contains("e") || s.contains("E")) {
                return Double.parseDouble(s);
            }
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0; // fallback
        }
    }

    private JsonPathToken tok(JsonPathTokenType type, String lexeme, int offset, int line, int col) {
        return new JsonPathToken(type, lexeme, lexeme, offset, line, col);
    }

    private void addToken(JsonPathToken token) {
        tokens.add(token);
        trace("addToken");
    }

    //
    // Diagnostics
    //

    private void trace(String method) {
        if (reporter == null) return;
        reporter.trace("C=%03d '%s' %03d:%03d %s", offset, currentChar(currentChar), line, col, method);
    }

    private String currentChar(char c) {
        switch (c) {
            case '\t':
                return "⇥";
            case '\r':
                return "↵";
            case '\n':
                return "↩";
            default:
                return Character.toString(c);
        }
    }
}
