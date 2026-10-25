package spl.codegen;

public final class NameGen {
    private int next;

    public String fresh(String prefix) {
        return prefix + (next++);
    }
}
