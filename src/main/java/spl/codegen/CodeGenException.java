package spl.codegen;

/**
 * Thrown when the code generator encounters an invalid tree structure,
 * missing symbol, or an unhandled grammar rule.
 */
public class CodeGenException extends RuntimeException {
    private final int nodeId;

    public CodeGenException(int nodeId, String message) {
        super("Code generation error at node " + nodeId + ": " + message);
        this.nodeId = nodeId;
    }

    public int getNodeId() {
        return nodeId;
    }
}