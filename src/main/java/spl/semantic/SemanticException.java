package spl.semantic;

public class SemanticException extends RuntimeException {
    public final Integer nodeId;

    public SemanticException(String message) {
        this(message, null);
    }

    public SemanticException(String message, Integer nodeId) {
        super(message);
        this.nodeId = nodeId;
    }
}
