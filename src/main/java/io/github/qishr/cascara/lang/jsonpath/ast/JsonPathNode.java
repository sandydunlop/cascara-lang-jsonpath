package io.github.qishr.cascara.lang.jsonpath.ast;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.CommentAstNode;

public abstract class JsonPathNode implements AstNode {

    protected int startLine;
    protected int startColumn;
    protected int endLine;
    protected int endColumn;
    protected URI uri;
    protected URI schemaUri;

    protected final List<JsonPathNode> children = new ArrayList<>();
    protected final List<CommentAstNode> comments = new ArrayList<>();

    @Override
    public int getStartLine() { return startLine; }

    @Override
    public int getStartColumn() { return startColumn; }

    @Override
    public int getEndLine() { return endLine; }

    @Override
    public int getEndColumn() { return endColumn; }

    @Override
    public URI getUri() { return uri; }

    @Override
    public List<JsonPathNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public List<CommentAstNode> getComments() {
        return Collections.unmodifiableList(comments);
    }

    public void addChild(JsonPathNode node) {
        if (node != null) children.add(node);
    }

    public void setLocation(int startLine, int startColumn, int endLine, int endColumn, URI uri) {
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
        this.uri = uri;
    }

    @Override
    public String getString() {
        return this.toString();
    }

    // @Override
    public Optional<URI> getSchemaUri() {
        return schemaUri == null ? Optional.empty() : Optional.of(schemaUri);
    }

    public void setSchemaUri(URI schemaUri) {
        this.schemaUri = schemaUri;
    }

}
