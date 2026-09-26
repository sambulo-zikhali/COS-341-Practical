package spl;

import spl.grammar.Grammar;
import spl.grammar.ParsingTable;
import spl.grammar.ParsingTableBuilder;
import spl.lexer.Lexer;
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

public class EndToEndManualTest {

    public static void main(String[] args) {
        Grammar grammar = new Grammar();
        ParsingTable table = new ParsingTableBuilder(grammar).build();

        System.out.println("=== TEST 1: valid.spl ===");
        String valid =
            "#x #y : " +
            "num #inc ( #n ) { : : #n = add ( #n 1 ) ; return ( #n ) } : " +
            "#x = 0 ; " +
            "#y = #inc ( #x ) ; " +
            "print ( #y ) ; " +
            "if eq ( #x 0 ) then { print \"isz\" ; } else { print \"notz\" ; } ; " +
            "while lesser ( #x 5 ) do { #x = add ( #x 1 ) ; print ( #x ) ; } ;";
        runTest(grammar, table, valid, true);

        System.out.println("\n=== TEST 2: malformed CALL/ASSIGN ===");
        runTest(grammar, table, "#x : : #x ( = ) ;", false);

        System.out.println("\n=== TEST 3: truncated input ===");
        runTest(grammar, table, "#x : : #x =", false);
    }

    private static void runTest(Grammar grammar, ParsingTable table, String source, boolean writeXml) {
        try {
            Lexer lexer = new Lexer();
            List<Token> tokens = lexer.tokenize(source);

            Parser parser = new Parser(table, new NodeIdGenerator());
            TreeNode root = parser.parse(tokens);

            System.out.println("PARSED OK. Root node id=" + root.id
                + ", contents=" + root.contents
                + ", children=" + root.children.size());

            if (writeXml) {
                Path outPath = Path.of("tree_test.xml");
                new XmlTreeWriter(grammar).write(root, parser.getNodesById(), outPath);
                System.out.println("Wrote " + outPath.toAbsolutePath());

                TreeValidator.validate(new File(outPath.toString()));
            }
        } catch (ParserException e) {
            System.out.println("PARSER ERROR: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("OTHER ERROR (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        } catch (Exception e) {
            System.out.println("EXCEPTION (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }
}