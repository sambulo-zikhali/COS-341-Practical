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

            // Skip spaces (ASCII 32)
            if (current == ' ') {
                i++;
                continue;
            }

            // Line breaks — handle \r\n as a single break
            if (current == '\r' || current == '\n') {
                if (current == '\r' && i + 1 < length && source.charAt(i + 1) == '\n') {
                    i++; // skip the \n in \r\n
                }
                line++;
                i++;
                continue;
            }

            // ── String literal ──────────────────────────────────────────
            if (current == '"') {
                int start = i;
                i++; // move past opening quote
                while (i < length && source.charAt(i) != '"') {
                    char sc = source.charAt(i);
                    if (sc == '\r' || sc == '\n') {
                        throw new LexerException(
                                "String at line " + line + " cannot span multiple lines.");
                    }
                    if (!isValidStringChar(sc)) {
                        throw new LexerException(
                                "Invalid character '" + sc + "' in string at line " + line
                                + ". Only lowercase letters, digits, and the symbols , . : \u2013 ? ! are allowed.");
                    }
                    i++;
                }
                if (i >= length) {
                    throw new LexerException("Unterminated string starting at line " + line);
                }
                i++; // move past closing quote
                String text = source.substring(start, i);

                // Spec: every token must end with a blank_space (ASCII 32 or 13)
                validateTokenTerminator(source, i, length, text, line);

                tokens.add(new Token(TokenType.STRING, text, line));
                continue;
            }

            // ── General token: read until whitespace ────────────────────
            int start = i;
            while (i < length && source.charAt(i) != ' '
                    && source.charAt(i) != '\r' && source.charAt(i) != '\n') {
                i++;
            }
            String text = source.substring(start, i);

            // Spec: every token must end with a blank_space (ASCII 32 or 13)
            validateTokenTerminator(source, i, length, text, line);

            Token token = classify(text, line);
            tokens.add(token);
        }

        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    /**
     * Validates that the character immediately after a token is a blank space
     * (ASCII 32), carriage return (ASCII 13), or newline (ASCII 10).
     * Per spec: "every token must end with a blank_space".
     * We allow end-of-file as a valid terminator for the very last token.
     */
    private void validateTokenTerminator(String source, int pos, int length,
                                         String tokenText, int line) {
        if (pos >= length) {
            // End of file — allow as terminator for the last token
            return;
        }
        char terminator = source.charAt(pos);
        if (terminator != ' ' && terminator != '\r' && terminator != '\n') {
            throw new LexerException("Token '" + tokenText + "' at line " + line
                    + " is not followed by a blank space as required by the SPL spec.");
        }
    }

    /**
     * Checks whether a character is valid inside a STRING literal.
     * Per spec regex: only  , . : \u2013 (en-dash) ? ! 0-9 a-z  are allowed.
     */
    private boolean isValidStringChar(char c) {
        if (c >= 'a' && c <= 'z') return true;
        if (c >= '0' && c <= '9') return true;
        if (c == ',' || c == '.' || c == ':' || c == '?' || c == '!') return true;
        if (c == '\u2013') return true; // en-dash U+2013
        return false;
    }

    // ── Token classification ────────────────────────────────────────────

    private Token classify(String text, int line) {
        // USER-DEFINED-NAME: must match #[0-9a-z]*
        if (text.startsWith("#")) {
            validateUserDefinedName(text, line);
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

    /**
     * Validates that a USER-DEFINED-NAME matches the spec regex: #[0-9a-z]*
     * (a '#' followed by zero or more lowercase letters or digits).
     */
    private void validateUserDefinedName(String text, int line) {
        for (int i = 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))) {
                throw new LexerException("Invalid character '" + c
                        + "' in user-defined name '" + text + "' at line " + line
                        + ". Only lowercase letters and digits are allowed after '#'.");
            }
        }
    }

    /**
     * Checks whether {@code text} matches the spec's NUM regex exactly:
     * <pre>
     *   0
     *   [-\u2013]?0.[0-9]*[1-9]
     *   [-\u2013]?[1-9][0-9]*.[0-9]*[1-9]
     *   [-\u2013]?[1-9][0-9]*
     * </pre>
     * Rejects: {@code -0}, leading zeros ({@code 01}, {@code 007}),
     *          trailing zeros in decimals ({@code 1.20}).
     * Supports en-dash (\u2013, U+2013) alongside hyphen-minus (-).
     */
    private boolean isNumber(String text) {
        int i = 0;
        int len = text.length();
        if (len == 0) return false;

        // Case 1: exactly "0"
        if (text.equals("0")) return true;

        // Optional sign: - or \u2013 (en-dash)
        if (text.charAt(0) == '-' || text.charAt(0) == '\u2013') {
            i++;
        }
        if (i >= len) return false;

        char firstDigit = text.charAt(i);
        if (!Character.isDigit(firstDigit)) return false;

        if (firstDigit == '0') {
            // After optional sign, '0' must be followed by '.' for a decimal
            // (e.g.  0.5  or  -0.5).   Bare "-0" or "00..." is invalid.
            i++;
            if (i >= len || text.charAt(i) != '.') {
                return false;   // "-0", "00", "01" etc.
            }
            i++; // skip the '.'
            return parseDecimalPart(text, i, len);
        }

        // firstDigit is 1-9: read remaining integer digits
        i++;
        while (i < len && Character.isDigit(text.charAt(i))) {
            i++;
        }

        // Consumed everything → valid integer  (e.g. "42", "-7")
        if (i == len) return true;

        // Next char is '.' → parse fractional part
        if (text.charAt(i) == '.') {
            i++;
            return parseDecimalPart(text, i, len);
        }

        return false; // unexpected character
    }

    /**
     * Validates the fractional digits after the decimal point.
     * Must have at least one digit, and the last digit must be 1-9
     * (no trailing zeros, e.g. 1.20 is invalid).
     */
    private boolean parseDecimalPart(String text, int start, int len) {
        if (start >= len) return false; // nothing after the dot
        for (int i = start; i < len; i++) {
            if (!Character.isDigit(text.charAt(i))) return false;
        }
        // Last digit must be 1-9
        char lastDigit = text.charAt(len - 1);
        return lastDigit >= '1' && lastDigit <= '9';
    }

    // ── Keyword & symbol matching (unchanged) ───────────────────────────

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