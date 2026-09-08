package spl.lexer;

import spl.model.Token;
import spl.model.TokenType;
import java.util.ArrayList;
import java.util.List;

public class Lexer {

    public List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        int line = 1;
        int i = 0;
        int length = source.length();

        while (i < length) {
            char current = source.charAt(i);
            if (current == ' ') {
                i++;
                continue;
            }
            if (current == '\r' || current == '\n') {
                line++;
                i++;
                continue;
            }

            if (current == '"') {
                int start = i;
                i++; // move past opening quote
                while (i < length && source.charAt(i) != '"') {
                    if (source.charAt(i) == '\n') {
                        line++;
                    }
                    i++;
                }
                if (i >= length) {
                    throw new LexerException("Unterminated string starting at line " + line);
                }
                i++; // move past closing quote
                String text = source.substring(start, i);
                tokens.add(new Token(TokenType.STRING, text, line));
                continue;
            }

            int start = i;
            while (i < length && source.charAt(i) != ' '
                    && source.charAt(i) != '\r' && source.charAt(i) != '\n') {
                i++;
            }
            String text = source.substring(start, i);

            Token token = classify(text, line);
            tokens.add(token);
        }

        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    private Token classify(String text, int line) {
        if (text.startsWith("#")) {
            return new Token(TokenType.NAME, text, line);
        }

        if (isNumber(text)) {
            return new Token(TokenType.NUM, text, line);
        }

        TokenType keyword = matchKeyword(text);
        if (keyword != null) {
            return new Token(keyword, text, line);
        }

        TokenType symbol = matchSymbol(text);
        if (symbol != null) {
            return new Token(symbol, text, line);
        }

        throw new LexerException("Unrecognized token '" + text + "' at line " + line);
    }

    private boolean isNumber(String text) {
        int i = 0;
        int length = text.length();

        if (length == 0) {
            return false;
        }

        if (text.charAt(0) == '-') {
            i++;
        }

        if (i == length) {
            return false;
        }

        boolean sawDigitBeforeDecimal = false;
        while (i < length && Character.isDigit(text.charAt(i))) {
            sawDigitBeforeDecimal = true;
            i++;
        }

        if (!sawDigitBeforeDecimal) {
            return false;
        }

        if (i < length && text.charAt(i) == '.') {
            i++;
            boolean sawDigitAfterDecimal = false;
            while (i < length && Character.isDigit(text.charAt(i))) {
                sawDigitAfterDecimal = true;
                i++;
            }
            if (!sawDigitAfterDecimal) {
                return false;
            }
        }

        return i == length;
    }

    private TokenType matchKeyword(String text) {
        return switch (text) {
            case "void" -> TokenType.VOID;
            case "num" -> TokenType.NUM_KW;
            case "return" -> TokenType.RETURN;
            case "print" -> TokenType.PRINT;
            case "nop" -> TokenType.NOP;
            case "comment" -> TokenType.COMMENT;
            case "if" -> TokenType.IF;
            case "then" -> TokenType.THEN;
            case "else" -> TokenType.ELSE;
            case "while" -> TokenType.WHILE;
            case "until" -> TokenType.UNTIL;
            case "do" -> TokenType.DO;
            case "not" -> TokenType.NOT;
            case "and" -> TokenType.AND;
            case "or" -> TokenType.OR;
            case "eq" -> TokenType.EQ;
            case "larger" -> TokenType.LARGER;
            case "lesser" -> TokenType.LESSER;
            case "mod" -> TokenType.MOD;
            case "add" -> TokenType.ADD;
            case "sub" -> TokenType.SUB;
            case "mul" -> TokenType.MUL;
            case "div" -> TokenType.DIV;
            case "neg" -> TokenType.NEG;
            default -> null;
        };
    }

    private TokenType matchSymbol(String text) {
        if (text.length() != 1) {
            return null;
        }
        return switch (text.charAt(0)) {
            case '(' -> TokenType.LPAREN;
            case ')' -> TokenType.RPAREN;
            case '{' -> TokenType.LBRACE;
            case '}' -> TokenType.RBRACE;
            case ';' -> TokenType.SEMICOLON;
            case ':' -> TokenType.COLON;
            case '=' -> TokenType.EQUALS;
            default -> null;
        };
    }
}