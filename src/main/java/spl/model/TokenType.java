package spl.model;

// every category of token the lexer can produce
public enum TokenType {
    // literals
    NUM,
    NAME,
    STRING,

    // keywords
    VOID,
    NUM_KW,
    RETURN,
    PRINT,
    NOP,
    COMMENT,
    IF,
    THEN,
    ELSE,
    WHILE,
    UNTIL,
    DO,
    NOT,
    AND,
    OR,
    EQ,
    LARGER,
    LESSER,
    MOD,
    ADD,
    SUB,
    MUL,
    DIV,
    NEG,

    // symbols
    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    SEMICOLON,
    COLON,
    EQUALS,

    // end of input
    EOF
}
