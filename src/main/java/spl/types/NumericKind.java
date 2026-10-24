package spl.types;

public enum NumericKind {
    INT ("int"),
    FLOAT("float");

    public final String cType;

    NumericKind(String cType) {
        this.cType=cType;
    }
}
