package spl.codegen;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RenameEnv {
    private final Map<String, String> entries = new HashMap<>();
    private final RenameEnv parent;

    public RenameEnv() {
        this(null);
    }

    private RenameEnv(RenameEnv parent) {
        this.parent = parent;
    }

    public void put(String from, String to) {
        entries.put(Objects.requireNonNull(from), Objects.requireNonNull(to));
    }

    public boolean has(String name) {
        return entries.containsKey(name) || (parent != null && parent.has(name));
    }

    public String lookup(String name) {
        if (entries.containsKey(name)) {
            return entries.get(name);
        }
        return parent == null ? name : parent.lookup(name);
    }

    public RenameEnv child() {
        return new RenameEnv(this);
    }
}
