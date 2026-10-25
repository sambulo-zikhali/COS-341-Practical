package spl.codegen;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import spl.semantic.Symbol;

public final class RenameEnv {
    private final Map<Symbol, String> names = new HashMap<>();
    private final Map<String,String> entries = new HashMap<>();
    private final RenameEnv parent;

    public RenameEnv() {
        this(null);
    }

    private RenameEnv(RenameEnv parent) {
        this.parent = parent;
    }

    public static RenameEnv identity() {
        return new RenameEnv(null);
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

    public void bind (Symbol s,String name) {
        names.put(Objects.requireNonNull(s), Objects.requireNonNull(name));
    }

    public String nameOf(Symbol s) {
        for (RenameEnv e = this; e!=null;e=e.parent) {
            String n = e.names.get(s);
            if(n !=  null) return n;
        }
        return s.internalName;
    }
}
