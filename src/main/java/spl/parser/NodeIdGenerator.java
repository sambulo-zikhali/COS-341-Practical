package spl.parser;

public class NodeIdGenerator {
    private int counter = 0;

    public int next() {
        return counter++;
    }
}
