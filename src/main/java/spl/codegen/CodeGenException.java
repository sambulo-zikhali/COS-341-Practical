package spl.codegen;

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
