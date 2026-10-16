package spl.semantic;

public final class Symbol {
    public enum Kind {
        VARIABLE,
        FUNCTION
    }

    public final String sourceName;
    public final String internalName;
    public final Kind kind;
    public final int declarationNodeId;
    public final int scopeId;

    public Symbol(String sourceName, String internalName, Kind kind, int declarationNodeId, int scopeId) {
        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("sourceName must not be blank");
        }
        if (internalName == null || internalName.isBlank()) {
            throw new IllegalArgumentException("internalName must not be blank");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        this.sourceName = sourceName;
        this.internalName = internalName;
        this.kind = kind;
        this.declarationNodeId = declarationNodeId;
        this.scopeId = scopeId;
    }
}
