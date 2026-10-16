package spl.semantic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Scope {
    public final int id;
    public final int level;
    public final Scope parent;

    private final Map<String, Symbol> variables = new LinkedHashMap<>();
    private final Map<String, Symbol> functions = new LinkedHashMap<>();

    public Scope(int id, int level, Scope parent) {
        this.id = id;
        this.level = level;
        this.parent = parent;
    }

    public boolean addVariable(Symbol symbol) {
        requireKind(symbol, Symbol.Kind.VARIABLE);
        return variables.putIfAbsent(symbol.sourceName, symbol) == null;
    }

    public boolean addFunction(Symbol symbol) {
        requireKind(symbol, Symbol.Kind.FUNCTION);
        return functions.putIfAbsent(symbol.sourceName, symbol) == null;
    }

    public Symbol getVariable(String sourceName) {
        return variables.get(sourceName);
    }

    public Symbol lookupVariable(String sourceName) {
        for (Scope scope = this; scope != null; scope = scope.parent) {
            Symbol symbol = scope.getVariable(sourceName);
            if (symbol != null) {
                return symbol;
            }
        }
        return null;
    }

    public Symbol getFunction(String sourceName) {
        return functions.get(sourceName);
    }

    public Map<String, Symbol> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public Map<String, Symbol> getFunctions() {
        return Collections.unmodifiableMap(functions);
    }

    private void requireKind(Symbol symbol, Symbol.Kind expectedKind) {
        if (symbol == null) {
            throw new IllegalArgumentException("symbol must not be null");
        }
        if (symbol.kind != expectedKind) {
            throw new IllegalArgumentException("Expected a " + expectedKind + " symbol");
        }
        if (symbol.scopeId != id) {
            throw new IllegalArgumentException("Symbol belongs to a different scope");
        }
    }
}
