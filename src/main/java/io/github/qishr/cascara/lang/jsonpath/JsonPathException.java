package io.github.qishr.cascara.lang.jsonpath;

import java.net.URI;

import io.github.qishr.cascara.common.lang.exception.ParserException;

public class JsonPathException extends ParserException {

    public JsonPathException(String message, Throwable cause) {
        super(message, cause, UNKNOWN_COORD, UNKNOWN_COORD, null);
    }

    public JsonPathException(String message, int line, int column, URI uri) {
        super(message, line, column, uri);
    }

    public JsonPathException(String message, Throwable cause, int line, int column, URI uri) {
        super(message, cause, line, column, uri);
    }
}
