package spl.output;

import org.junit.jupiter.api.Test;
import spl.grammar.Grammar;
import spl.grammar.ParsingTable;
import spl.grammar.ParsingTableBuilder;
import spl.lexer.Lexer;
import spl.model.Token;
import spl.model.TreeNode;
import spl.parser.NodeIdGenerator;
import spl.parser.Parser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XmlTreeWriterTest {

    @Test
    void writesValidTreeXml(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        Grammar grammar = new Grammar();
        ParsingTable table = new ParsingTableBuilder(grammar).build();

        List<Token> tokens = new Lexer().tokenize("#x : : #x = 5 ;");
        Parser parser = new Parser(table, new NodeIdGenerator());
        TreeNode root = parser.parse(tokens);

        Path outPath = tempDir.resolve("tree.xml");
        new XmlTreeWriter(grammar).write(root, parser.getNodesById(), outPath);

        assertTrue(Files.exists(outPath));
        TreeValidator.validate(new File(outPath.toString())); // throws on any structural violation
    }
}