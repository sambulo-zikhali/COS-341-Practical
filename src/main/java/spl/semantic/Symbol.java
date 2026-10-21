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
    public final boolean isParameter;

    public Symbol(
            String sourceName,
            String internalName,
            Kind kind,
            int declarationNodeId,
            int scopeId,
            boolean isParameter) {
        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("sourceName must not be blank");
        }
        if (internalName == null || internalName.isBlank()) {
            throw new IllegalArgumentException("internalName must not be blank");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (isParameter && kind != Kind.VARIABLE) {
            throw new IllegalArgumentException("Only variable symbols can be parameters");
        }
        this.sourceName = sourceName;
        this.internalName = internalName;
        this.kind = kind;
        this.declarationNodeId = declarationNodeId;
        this.scopeId = scopeId;
        this.isParameter = isParameter;
    }

    private String type = "unknown";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        this.type = type;
    }
}
