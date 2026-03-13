module cascara.lang.jsonpath {
    requires transitive cascara.common;

    exports io.github.qishr.cascara.lang.jsonpath;
    exports io.github.qishr.cascara.lang.jsonpath.ast;
    exports io.github.qishr.cascara.lang.jsonpath.token;
    exports io.github.qishr.cascara.lang.jsonpath.processor;

    opens io.github.qishr.cascara.lang.jsonpath;
    opens io.github.qishr.cascara.lang.jsonpath.ast;
    opens io.github.qishr.cascara.lang.jsonpath.token;
    opens io.github.qishr.cascara.lang.jsonpath.processor;
}
