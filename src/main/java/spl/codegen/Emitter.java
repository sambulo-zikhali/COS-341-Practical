package spl.codegen;

public class Emitter {
    private final boolean mineMode;
    private final StringBuilder output = new StringBuilder();

    public Emitter() {
        this(false);
    }

    public Emitter(boolean mineMode) {
        this.mineMode = mineMode;
    }

    public void emit(String line) {
        if (mineMode && line.contains("*/")) {
            throw new IllegalStateException("Mine output cannot contain a block-comment terminator");
        }
        output.append(line).append("\r\n");
    }

    @Override
    public String toString() {
        return output.toString();
    }
}
