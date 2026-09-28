package spl.parser;

import org.junit.jupiter.api.Test;
import spl.grammar.Grammar;
import spl.grammar.ParsingTable;
import spl.grammar.ParsingTableBuilder;
import spl.lexer.Lexer;
import spl.model.Token;
import spl.model.TreeNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    private ParsingTable buildTable() {
        return new ParsingTableBuilder(new Grammar()).build();
    }

    @Test
    void parsesValidProgramToAccept() {
        String source = "#x #y : " +
                "num #inc ( #n ) { : : #n = add ( #n 1 ) ; return ( #n ) } : " +
                "#x = 0 ; " +
                "#y = #inc ( #x ) ; " +
                "print ( #y ) ; " +
                "if eq ( #x 0 ) then { print \"isz\" ; } else { print \"notz\" ; } ; " +
                "while lesser ( #x 5 ) do { #x = add ( #x 1 ) ; print ( #x ) ; } ;";

        List<Token> tokens = new Lexer().tokenize(source);
        Parser parser = new Parser(buildTable(), new NodeIdGenerator());
        TreeNode root = parser.parse(tokens);

        assertEquals("SPL_PROG", root.contents);
        assertNull(root.parent);
        assertFalse(root.children.isEmpty());
    }

    @Test
    void rejectsMalformedCallAssignBoundary() {
        List<Token> tokens = new Lexer().tokenize("#x : : #x ( = ) ;");
        Parser parser = new Parser(buildTable(), new NodeIdGenerator());
        assertThrows(ParserException.class, () -> parser.parse(tokens));
    }

    @Test
    void rejectsTruncatedAssign() {
        List<Token> tokens = new Lexer().tokenize("#x : : #x =");
        Parser parser = new Parser(buildTable(), new NodeIdGenerator());
        assertThrows(ParserException.class, () -> parser.parse(tokens));
    }

    @Test
    void emptyProgramParsesViaAllEpsilonProductions() {
        // V_DECL, F_DECL, ALGO can all be empty — ": :" alone should parse.
        List<Token> tokens = new Lexer().tokenize(": :");
        Parser parser = new Parser(buildTable(), new NodeIdGenerator());
        TreeNode root = parser.parse(tokens);
        assertEquals("SPL_PROG", root.contents);
    }
}