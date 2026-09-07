package spl.model;

// represents one lexical token produced by the lexer
public class Token {
    public final TokenType type;
    public final String text;
    public final int line;

    public Token(TokenType type, String text, int line) {
        this.type = type;
        this.text = text;
        this.line = line;
    }

    @Override
    public String toString() {
        return type + "(\"" + text + "\") @line " + line;
    }
}
