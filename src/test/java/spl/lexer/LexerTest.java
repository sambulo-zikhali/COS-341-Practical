package spl.lexer;

import org.junit.jupiter.api.Test;
import spl.model.Token;
import spl.model.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    @Test
    void tokenizesUserDefinedNames() {
        List<Token> tokens = new Lexer().tokenize("#x #y ");
        assertEquals(TokenType.NAME, tokens.get(0).type);
        assertEquals("#x", tokens.get(0).text);
        assertEquals(TokenType.NAME, tokens.get(1).type);
        assertEquals("#y", tokens.get(1).text);
    }

    @Test
    void tokenizesValidNumbers() {
        List<Token> tokens = new Lexer().tokenize("0 42 -7 1.5 ");
        assertEquals(TokenType.NUM, tokens.get(0).type);
        assertEquals(TokenType.NUM, tokens.get(1).type);
        assertEquals(TokenType.NUM, tokens.get(2).type);
        assertEquals(TokenType.NUM, tokens.get(3).type);
    }

    @Test
    void rejectsNegativeZero() {
        assertThrows(LexerException.class, () -> new Lexer().tokenize("-0 "));
    }

    @Test
    void rejectsLeadingZeros() {
        assertThrows(LexerException.class, () -> new Lexer().tokenize("007 "));
        assertThrows(LexerException.class, () -> new Lexer().tokenize("01 "));
    }

    @Test
    void rejectsTrailingZeroInDecimal() {
        assertThrows(LexerException.class, () -> new Lexer().tokenize("1.20 "));
    }

    @Test
    void rejectsInvalidUserDefinedNameCharacters() {
        assertThrows(LexerException.class, () -> new Lexer().tokenize("#Foo "));
    }

    @Test
    void tokenizesValidString() {
        List<Token> tokens = new Lexer().tokenize("\"hello,world\" ");
        assertEquals(TokenType.STRING, tokens.get(0).type);
    }

    @Test
    void rejectsInvalidStringCharacters() {
        assertThrows(LexerException.class, () -> new Lexer().tokenize("\"Hello\" "));
    }

    @Test
    void appendsEofToken() {
        List<Token> tokens = new Lexer().tokenize("#x ");
        assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).type);
    }

    @Test
    void tokenizesKeywordsAndSymbols() {
        List<Token> tokens = new Lexer().tokenize("if then else ( ) { } ; : = ");
        assertEquals(TokenType.IF, tokens.get(0).type);
        assertEquals(TokenType.THEN, tokens.get(1).type);
        assertEquals(TokenType.ELSE, tokens.get(2).type);
        assertEquals(TokenType.LPAREN, tokens.get(3).type);
        assertEquals(TokenType.RPAREN, tokens.get(4).type);
        assertEquals(TokenType.LBRACE, tokens.get(5).type);
        assertEquals(TokenType.RBRACE, tokens.get(6).type);
        assertEquals(TokenType.SEMICOLON, tokens.get(7).type);
        assertEquals(TokenType.COLON, tokens.get(8).type);
        assertEquals(TokenType.EQUALS, tokens.get(9).type);
    }
}