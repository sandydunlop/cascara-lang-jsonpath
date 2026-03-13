package io.github.qishr.cascara.lang.jsonpath;

import io.github.qishr.cascara.common.lang.LanguageOptions;

public class JsonPathOptions extends LanguageOptions<JsonPathOptions> {
    private boolean allowScriptExpressions;
    private boolean allowRegex;
    private boolean treatMissingFieldsAsNull;
    private boolean strictMode;
    private int maxFilterDepth;

    public boolean isAllowScriptExpressions() { return allowScriptExpressions; }
    public JsonPathOptions setAllowScriptExpressions(boolean allowScriptExpressions) { this.allowScriptExpressions = allowScriptExpressions; return this;}
    public boolean isAllowRegex() { return allowRegex; }
    public JsonPathOptions setAllowRegex(boolean allowRegex) { this.allowRegex = allowRegex; return this; }
    public boolean isTreatMissingFieldsAsNull() { return treatMissingFieldsAsNull; }
    public JsonPathOptions setTreatMissingFieldsAsNull(boolean treatMissingFieldsAsNull) { this.treatMissingFieldsAsNull = treatMissingFieldsAsNull; return this; }
    public boolean isStrictMode() { return strictMode; }
    public JsonPathOptions setStrictMode(boolean strictMode) { this.strictMode = strictMode; return this; }
    public int getMaxFilterDepth() { return maxFilterDepth; }
    public JsonPathOptions setMaxFilterDepth(int maxFilterDepth) { this.maxFilterDepth = maxFilterDepth; return this; }
}