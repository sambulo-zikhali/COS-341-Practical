package spl;

import org.junit.jupiter.api.Test;
import spl.grammar.Grammar;
import spl.grammar.ParsingTable;
import spl.grammar.ParsingTableBuilder;
import spl.lexer.Lexer;
import spl.lexer.LexerException;
import spl.model.Token;
import spl.model.TreeNode;
import spl.output.TreeValidator;
import spl.output.XmlTreeWriter;
import spl.parser.NodeIdGenerator;
import spl.parser.Parser;
import spl.parser.ParserException;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EndToEndTest {

    @Test
    void validProgramProducesValidTreeXml(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        String source = "#x #y : " +
                "num #inc ( #n ) { : : #n = add ( #n 1 ) ; return ( #n ) } : " +
                "#x = 0 ; " +
                "#y = #inc ( #x ) ; " +
                "print ( #y ) ; " +
                "if eq ( #x 0 ) then { print \"isz\" ; } else { print \"notz\" ; } ; " +
                "while lesser ( #x 5 ) do { #x = add ( #x 1 ) ; print ( #x ) ; } ;";

        Grammar grammar = new Grammar();
        ParsingTable table = new ParsingTableBuilder(grammar).build();

        List<Token> tokens = new Lexer().tokenize(source);
        Parser parser = new Parser(table, new NodeIdGenerator());
        TreeNode root = parser.parse(tokens);

        Path outPath = tempDir.resolve("tree.xml");
        new XmlTreeWriter(grammar).write(root, parser.getNodesById(), outPath);
        TreeValidator.validate(new File(outPath.toString()));
    }

    @Test
    void malformedProgramThrowsParserException() {
        Grammar grammar = new Grammar();
        ParsingTable table = new ParsingTableBuilder(grammar).build();
        List<Token> tokens = new Lexer().tokenize("#x : : #x ( = ) ;");
        Parser parser = new Parser(table, new NodeIdGenerator());
        assertThrows(ParserException.class, () -> parser.parse(tokens));
    }

    @Test
    void invalidLexicalTokenThrowsLexerException() {
        assertThrows(LexerException.class, () -> new Lexer().tokenize("#Invalid "));
    }
}