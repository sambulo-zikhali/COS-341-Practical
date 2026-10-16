package spl.semantic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SymbolTable {
    private final Map<Integer, Symbol> symbolsByDeclarationNodeId = new LinkedHashMap<>();
    private final Map<Integer, Symbol> bindingsByNodeId = new LinkedHashMap<>();
    private final Map<Integer, Scope> scopesById = new LinkedHashMap<>();

    public void addScope(Scope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
        if (scopesById.putIfAbsent(scope.id, scope) != null) {
            throw new IllegalArgumentException("A scope with id " + scope.id + " already exists");
        }
    }

    public void addSymbol(Symbol symbol) {
        if (symbol == null) {
            throw new IllegalArgumentException("symbol must not be null");
        }
        if (symbolsByDeclarationNodeId.putIfAbsent(symbol.declarationNodeId, symbol) != null) {
            throw new IllegalArgumentException(
                    "A symbol for declaration node " + symbol.declarationNodeId + " already exists");
        }
    }

    public void bind(int nodeId, Symbol symbol) {
        if (symbol == null) {
            throw new IllegalArgumentException("symbol must not be null");
        }
        if (bindingsByNodeId.putIfAbsent(nodeId, symbol) != null) {
            throw new IllegalArgumentException("Node " + nodeId + " already has a symbol binding");
        }
    }

    public Symbol getSymbol(int declarationNodeId) {
        return symbolsByDeclarationNodeId.get(declarationNodeId);
    }

    public Symbol getBinding(int nodeId) {
        return bindingsByNodeId.get(nodeId);
    }

    public Scope getScope(int scopeId) {
        return scopesById.get(scopeId);
    }

    public Map<Integer, Symbol> getSymbolsByDeclarationNodeId() {
        return Collections.unmodifiableMap(symbolsByDeclarationNodeId);
    }

    public Map<Integer, Symbol> getBindingsByNodeId() {
        return Collections.unmodifiableMap(bindingsByNodeId);
    }

    public Map<Integer, Scope> getScopesById() {
        return Collections.unmodifiableMap(scopesById);
    }
}
