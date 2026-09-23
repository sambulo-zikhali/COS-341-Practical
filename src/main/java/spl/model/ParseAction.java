package spl.model;

// one cell of the LR ACTION/GOTO table
public final class ParseAction {
    // the kind of action the cell represents
    public enum Kind {
        SHIFT,
        REDUCE,
        GOTO,
        ACCEPT,
        ERROR
    }


    public final Kind kind;
    public final int value; // state number for SHIFT/GOTO, rule number for REDUCE, unused for ACCEPT/ERROR

    public ParseAction(Kind kind, int value) {
        this.kind = kind;
        this.value = value;
    }

    public static ParseAction shift(int state) {
        return new ParseAction(Kind.SHIFT, state);
    }

    public static ParseAction reduce(int ruleNumber) {
        return new ParseAction(Kind.REDUCE, ruleNumber);
    }

    public static ParseAction gotoState(int state) {
        return new ParseAction(Kind.GOTO, state);
    }

    public static ParseAction accept() {
        return new ParseAction(Kind.ACCEPT, -1);
    }

    public static ParseAction error() {
        return new ParseAction(Kind.ERROR, -1);
    }

    public Kind getKind() {
        return kind;
    }

    public int getTargetState() {
        if(kind !=Kind.SHIFT) throw new IllegalStateException("Not a SHIFT action: ");
        return value;
    }

    public int getRuleId() {
        if (kind != Kind.REDUCE) throw new IllegalStateException("Not a REDUCE action: ");
        return value;
    }

    @Override 
    public String toString() {
        switch(kind) {
            case SHIFT: return "shift "+value;
            case REDUCE: return "reduce "+value;
            case ACCEPT: return "accept";
            default: return "error";
        }
    }
}
