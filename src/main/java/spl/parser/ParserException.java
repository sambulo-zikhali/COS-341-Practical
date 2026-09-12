package spl.parser;

public class ParserException extends RuntimeException {
    public final int line;
    public final String expected;
    public final String found;

    public ParserException(int line, String expected, String found) {
        super("Error at line " + line + ": unexpected token. Expected '" + expected + "', but found '" + found + "'.");
        this.line = line;
        this.expected = expected;
        this.found = found;
    }
}
