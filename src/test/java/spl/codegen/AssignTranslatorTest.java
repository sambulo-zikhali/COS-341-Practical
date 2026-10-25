package spl.codegen;

import org.junit.jupiter.api.Test;
import spl.grammar.Grammar;
import spl.grammar.ParsingTableBuilder;
import spl.lexer.Lexer;
import spl.model.TreeNode;
import spl.parser.NodeIdGenerator;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyzer;
import spl.semantic.SymbolTable;
import spl.types.NumericKind;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignTranslatorTest {
    @Test
    void translatesLiteralAssignmentWithDeclarationAndStore() {
        Parsed parsed = parse("#x : : #x = 5 ;");
        Emitter output = new Emitter();
        NameGen names = new NameGen();
        AssignTranslator translator = translator(parsed, output, names, NumericKind.INT);

        translator.transAssign(assignment(parsed), new RenameEnv());

        assertEquals("int t0 ;\r\nt0 = 5 ;\r\nv0 = t0 ;\r\n", output.toString());
    }

    @Test
    void translatesAssignmentExpression() {
        Parsed parsed = parse("#x #y : : #x = add ( #y 1 ) ;");
        Emitter output = new Emitter();
        AssignTranslator translator = translator(parsed, output, new NameGen(), NumericKind.INT);

        translator.transAssign(assignment(parsed), new RenameEnv());

        assertEquals(
                "int t0 ;\r\n"
                        + "int t1 ;\r\n"
                        + "int t2 ;\r\n"
                        + "t1 = v1 ;\r\n"
                        + "t2 = 1 ;\r\n"
                        + "t0 = t1 + t2 ;\r\n"
                        + "v0 = t0 ;\r\n",
                output.toString());
    }

    @Test
    void appliesRenameEnvironmentToTargetAndSourceNames() {
        Parsed parsed = parse("#x #y : : #x = add ( #y 1 ) ;");
        Emitter output = new Emitter();
        RenameEnv env = new RenameEnv();
        env.put("v0", "v0_c1");
        env.put("v1", "v1_p1");
        AssignTranslator translator = translator(parsed, output, new NameGen(), NumericKind.INT);

        translator.transAssign(assignment(parsed), env);

        assertEquals(
                "int t0 ;\r\n"
                        + "int t1 ;\r\n"
                        + "int t2 ;\r\n"
                        + "t1 = v1_p1 ;\r\n"
                        + "t2 = 1 ;\r\n"
                        + "t0 = t1 + t2 ;\r\n"
                        + "v0_c1 = t0 ;\r\n",
                output.toString());
    }

    @Test
    void usesFloatTypeForFloatNumericKind() {
        Parsed parsed = parse("#x : : #x = 5 ;");
        Emitter output = new Emitter();
        AssignTranslator translator = translator(parsed, output, new NameGen(), NumericKind.FLOAT);

        translator.transAssign(assignment(parsed), new RenameEnv());

        assertEquals("float t0 ;\r\nt0 = 5 ;\r\nv0 = t0 ;\r\n", output.toString());
    }

    @Test
    void assigningFromCallPropagatesUnsupportedOperation() {
        Parsed parsed = parse(
                "#x : num #f ( #a ) { : : return ( #a ) } : #x = #f ( 1 ) ;");
        Emitter output = new Emitter();
        AssignTranslator translator = translator(parsed, output, new NameGen(), NumericKind.INT);
        boolean rejected = false;

        try {
            translator.transAssign(assignment(parsed), new RenameEnv());
        } catch (UnsupportedOperationException exception) {
            rejected = true;
            assertEquals("calls not implemented yet", exception.getMessage());
        }

        assertTrue(rejected);
        assertEquals("int t0 ;\r\n", output.toString());
    }

    @Test
    void sharesTemporaryCounterWithTermTranslation() {
        Parsed parsed = parse("#x : : #x = add ( 1 2 ) ;");
        Emitter output = new Emitter();
        NameGen names = new NameGen();
        AssignTranslator translator = translator(parsed, output, names, NumericKind.INT);

        translator.transAssign(assignment(parsed), new RenameEnv());

        assertEquals(
                "int t0 ;\r\n"
                        + "int t1 ;\r\n"
                        + "int t2 ;\r\n"
                        + "t1 = 1 ;\r\n"
                        + "t2 = 2 ;\r\n"
                        + "t0 = t1 + t2 ;\r\n"
                        + "v0 = t0 ;\r\n",
                output.toString());
    }

    private Parsed parse(String source) {
        Grammar grammar = new Grammar();
        Parser parser = new Parser(
                new ParsingTableBuilder(grammar).build(),
                new NodeIdGenerator());
        TreeNode root = parser.parse(new Lexer().tokenize(source));
        Map<Integer, TreeNode> nodes = parser.getNodesById();
        SymbolTable symbols = new SemanticAnalyzer().analyze(root, nodes);
        return new Parsed(root, nodes, symbols);
    }

    private TreeNode assignment(Parsed parsed) {
        TreeNode program = child(parsed.root, 0, parsed.nodes);
        TreeNode algorithm = child(program, 4, parsed.nodes);
        TreeNode instruction = child(algorithm, 0, parsed.nodes);
        return child(instruction, 0, parsed.nodes);
    }

    private TreeNode child(TreeNode node, int index, Map<Integer, TreeNode> nodes) {
        return nodes.get(node.children.get(index));
    }

    private AssignTranslator translator(
            Parsed parsed,
            Emitter output,
            NameGen names,
            NumericKind kind) {
        TermTranslator terms = new TermTranslator(
                output,
                names,
                parsed.nodes,
                parsed.symbols,
                kind);
        return new AssignTranslator(
                output,
                names,
                parsed.nodes,
                parsed.symbols,
                kind,
                terms);
    }

    private static final class Parsed {
        private final TreeNode root;
        private final Map<Integer, TreeNode> nodes;
        private final SymbolTable symbols;

        private Parsed(TreeNode root, Map<Integer, TreeNode> nodes, SymbolTable symbols) {
            this.root = root;
            this.nodes = nodes;
            this.symbols = symbols;
        }
    }
}
