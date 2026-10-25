package spl.codegen;

public final class Emitter {
    public static final String NL = "\r\n";

    private final StringBuilder out = new StringBuilder();
    private final String typ;
    private int mineDepth;

    public Emitter(String typ) {
        this.typ=typ;
    }

    public boolean inMine() {return mineDepth>0;}
    public void beginMine() {mineDepth++;}
    public void endMine() {mineDepth--;}

    public void line(String s) {
        if(mineDepth > 0 && (s.contains("/*") || s.contains("*/"))) {
            throw new IllegalStateException("Comment delimiter inside code-mine: " +s);
        }
        out.append(s).append(NL);
    }

    // A statement line: text efollowed by ";". */
    public void stmt(String s) {
        line(s+ " ;");
    }

    //declares glogral variable
    public void decl(String name) {
        stmt(typ + " " + name);
    }

    //Annotation text for live mode amd mine mode
    public String note(String text) {
        return mineDepth >0 ? "["+text+ "]": "/*" + text +" */";
    }

    //label line but ends in empty space so the label is never at the bottom off a block I think
    public void label(String label, String noteText) {
        line(label + " : "+(noteText == null ? "": note(noteText) + " ") + ";");
    }

    //negative numbers user an en dash, C++ needs ASCVII minus idk if theres a differnce
    public static String number(String token) {
        return token.replace('\u2023', '-');
    }

    //String token
    public static String stringLiteral(String token) {
        return token.replace("?", "\\?");
    }

    @Override 
    public String toString() {
        return out.toString();
    }
}