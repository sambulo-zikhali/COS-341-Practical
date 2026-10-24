package spl;

import spl.grammar.Grammar;
import spl.grammar.ParsingTable;
import spl.grammar.ParsingTableBuilder;
import spl.lexer.Lexer;
import spl.lexer.LexerException;
import spl.model.Token;
import spl.model.TreeNode;
import spl.output.XmlTreeWriter;
import spl.parser.NodeIdGenerator;
import spl.parser.Parser;
import spl.parser.ParserException;
import spl.semantic.SemanticAnalyzer;
import spl.semantic.SemanticException;
import spl.semantic.SymbolTable;
import spl.types.TypeAnalyser;
import spl.types.TypeException;
import spl.codegen.CodeGenerator;
import spl.types.NumericKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Entry point: reads an SPL source file, parses it, and either writes
 * tree.xml on success or prints a meaningful syntax error on failure.
 *
 * Usage: java -jar group-23.jar SPL.txt
 *        (or with no args, looks for SPL.txt in the current directory)
 */
public class Main {

    public static void main(String[] args) {
        Path inputPath = resolveInputPath(args);

        String source;
        try {
            source = Files.readString(inputPath);
        } catch (IOException e) {
            System.err.println("Could not read input file: " + inputPath.toAbsolutePath());
            System.err.println("Reason: " + e.getMessage());
            System.exit(1);
            return;
        }

        Grammar grammar = new Grammar();
        ParsingTable table = new ParsingTableBuilder(grammar).build();

        List<Token> tokens;
        try {
            tokens = new Lexer().tokenize(source);
        } catch (LexerException e) {
            System.err.println("Lexical error in " + inputPath.getFileName() + ":");
            System.err.println("  " + e.getMessage());
            System.exit(1);
            return;
        }

        TreeNode root;
        Parser parser = new Parser(table, new NodeIdGenerator());
        try {
            root = parser.parse(tokens);
        } catch (ParserException e) {
            System.err.println("Syntax error in " + inputPath.getFileName() + ":");
            System.err.println("  " + e.getMessage());
            System.exit(1);
            return;
        }

        Path outputPath = Path.of("tree.xml");
        try {
            new XmlTreeWriter(grammar).write(root, parser.getNodesById(), outputPath);
        } catch (IOException e) {
            System.err.println("Parsed successfully, but failed to write tree.xml:");
            System.err.println("  " + e.getMessage());
            System.exit(1);
            return;
        }

        try {
            new SemanticAnalyzer().analyze(root, parser.getNodesById());
        } catch (SemanticException e) {
            System.err.println("Semantic error in " + inputPath.getFileName() + ":");
            String nodeSuffix = e.nodeId == null ? "" : " (node " + e.nodeId + ")";
            System.err.println("  " + e.getMessage() + nodeSuffix);
            System.exit(1);
            return;
        }

        // System.out.println("Semantic analysis passed.");
        // System.out.println("Parsed successfully. Wrote " + outputPath.toAbsolutePath());

        SymbolTable symbols;
        try {
            symbols = new SemanticAnalyzer().analyze(root, parser.getNodesById());
        } catch (TypeException e) {
            System.err.println("Type error in " + inputPath.getFileName() + ":");
            String nodeSuffix = e.getNodeId() == -1 ? "" : " (node " + e.getNodeId() + ")";
            System.err.println("  " + e.getMessage() + nodeSuffix);
            System.exit(1);
            return;
        }
        catch (SemanticException e) 
        {
            System.err.println("Semantic error in " + inputPath.getFileName() + ":");
            String nodeSuffix = e.nodeId == null ? "" : " (node " + e.nodeId + ")";
            System.err.println("  " + e.getMessage() + nodeSuffix);
            System.exit(1);
            return;
        }
        System.out.println("Semantic analysis passed.");

        TypeAnalyser analyser = new TypeAnalyser(parser.getNodesById(), symbols, grammar);
        try {

        }catch (TypeException e) {
            System.out.println("Type error in "+ inputPath.getFileName() + ":");
        }
    }

    /**
     * Resolves the input SPL file path: the first CLI argument if given,
     * otherwise SPL.txt in the current working directory.
     */
    private static Path resolveInputPath(String[] args) {
        if (args.length > 0) {
            return Path.of(args[0]);
        }
        return Path.of("SPL.txt");
    }

}