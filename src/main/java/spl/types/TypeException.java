package spl.types;

public class TypeException extends RuntimeException {
    private final int nodeId;
    
    public TypeException(int nodeId,String message) {
        super(message + "(syntax tree node" + nodeId + ")");
        this.nodeId = nodeId;
    }
    
    //errors that like arent from one specific node
    public TypeException(String message) {
        super(message);
        this.nodeId = -1;
    }

    public int getNodeId() {
        return nodeId;
    }
}
