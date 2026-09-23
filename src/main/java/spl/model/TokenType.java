package spl.model;
import java.util.HashMap;
import java.util.Map;

// every category of token the lexer can produce
public enum TokenType {
    // literals
    NUM("NUM"),
    NAME("USER-DEFINED-NAME"),
    STRING("STRING"),

    // keywords
    VOID("void"),
    NUM_KW("num"),
    RETURN("return"),
    PRINT("print"),
    NOP("nop"),
    COMMENT("comment"),
    IF("if"),
    THEN("then"),
    ELSE("else"),
    WHILE("while"),
    UNTIL("until"),
    DO("do"),
    NOT("not"),
    AND("and"),
    OR("or"),
    EQ("eq"),
    LARGER("larger"),
    LESSER("lesser"),
    MOD("mod"),
    ADD("add"),
    SUB("sub"),
    MUL("mul"),
    DIV("div"),
    NEG("neg"),

    // symbols
    LPAREN("("),
    RPAREN(")"),
    LBRACE("{"),
    RBRACE("}"),
    SEMICOLON(";"),
    COLON(":"),
    EQUALS("="),

    // end of input
    EOF("$");

    private final String grammarSymbol;

    TokenType(String grammarSymbol) {
        this.grammarSymbol=grammarSymbol;
    }
    public String getGrammarType() {
        return grammarSymbol;
    }

    private static final Map<String, TokenType> BY_SYMBOL = new HashMap<>();
    static {
        for(TokenType t: values()) {
            BY_SYMBOL.put(t.grammarSymbol,t);
        }
    }

    public static TokenType fromGrammarSymbol(String symbol) {
        TokenType t = BY_SYMBOL.get(symbol);
        if(t == null) {
            throw new IllegalArgumentException("Not a terminal symbol of SPL"+symbol);
        }
        return t;
    }
    public static boolean isTerminalSymbol(String symbol) {
        return BY_SYMBOL.containsKey(symbol);
    }
}
