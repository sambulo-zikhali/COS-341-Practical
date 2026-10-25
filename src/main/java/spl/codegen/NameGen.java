package spl.codegen;

public class NameGen {
    private int counter;

    public String next(String prefix) {
        if (prefix == null || prefix.startsWith("v")) {
            throw new IllegalArgumentException("Generated-name prefixes must not start with 'v'");
        }
        return prefix + counter++;
    }

    public String temp() {
        return next("t");
    }

    public String label() {
        return next("L");
    }

    public String capture() {
        return next("p");
    }

    public String ret() {
        return next("r");
    }

    public String cont() {
        return next("c");
    }
}
