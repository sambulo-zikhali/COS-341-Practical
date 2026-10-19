package spl.types;

public enum Type {
    UNKNOWN, OK, NUMERIC, PROCEDURE;

    @Override 
    public String toString() {
        return name().toLowerCase();
    }

    public static Type fromString(String s) {
        return valueOf(s.toUpperCase());
    }
}
