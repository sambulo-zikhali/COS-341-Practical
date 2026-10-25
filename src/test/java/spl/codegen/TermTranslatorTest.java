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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TermTranslatorTest {
    @Test
    void translatesVariableWithAndWithoutRenaming() {
        Parsed parsed = parse("#x : : #x = #x ;");
        TreeNode term = assignmentTerm(parsed);
        Emitter original = new Emitter("int");
        TermTranslator originalTranslator = translator(parsed, original, new NameGen(), NumericKind.INT);
        originalTranslator.transTerm(term, "dest", new RenameEnv());

        assertEquals("dest = v0 ;\r\n", original.toString());

        Emitter renamed = new Emitter("int");
        RenameEnv env = new RenameEnv();
        env.put("v0", "v0_c1");
        TermTranslator renamedTranslator = translator(parsed, renamed, new NameGen(), NumericKind.INT);
        renamedTranslator.transTerm(term, "dest", env);

        assertEquals("dest = v0_c1 ;\r\n", renamed.toString());
    }

    @Test
    void translatesNumbersAndNormalizesEnDash() {
        Parsed number = parse("#x : : #x = 5 ;");
        Emitter numberOutput = new Emitter("int");
        translator(number, numberOutput, new NameGen(), NumericKind.INT)
                .transTerm(assignmentTerm(number), "dest", new RenameEnv());
        assertEquals("dest = 5 ;\r\n", numberOutput.toString());

        Parsed negative = parse("#x : : #x = \u20135 ;");
        Emitter negativeOutput = new Emitter("int");
        translator(negative, negativeOutput, new NameGen(), NumericKind.INT)
                .transTerm(assignmentTerm(negative), "dest", new RenameEnv());
        assertEquals("dest = -5 ;\r\n", negativeOutput.toString());
    }

    @Test
    void translatesAdditionAndNestedAdditionWithMultiplication() {
        Parsed addition = parse("#x : : #x = add ( 1 2 ) ;");
        Emitter additionOutput = new Emitter("int");
        translator(addition, additionOutput, new NameGen(), NumericKind.INT)
                .transTerm(assignmentTerm(addition), "dest", new RenameEnv());
        assertEquals(
                "int t0 ;\r\n"
                        + "int t1 ;\r\n"
                        + "t0 = 1 ;\r\n"
                        + "t1 = 2 ;\r\n"
                        + "dest = t0 + t1 ;\r\n",
                additionOutput.toString());

        Parsed nested = parse("#x : : #x = add ( 1 mul ( 2 3 ) ) ;");
        Emitter nestedOutput = new Emitter("int");
        translator(nested, nestedOutput, new NameGen(), NumericKind.INT)
                .transTerm(assignmentTerm(nested), "dest", new RenameEnv());
        assertEquals(
                "int t0 ;\r\n"
                        + "int t1 ;\r\n"
                        + "t0 = 1 ;\r\n"
                        + "int t2 ;\r\n"
                        + "int t3 ;\r\n"
                        + "t2 = 2 ;\r\n"
                        + "t3 = 3 ;\r\n"
                        + "t1 = t2 * t3 ;\r\n"
                        + "dest = t0 + t1 ;\r\n",
                nestedOutput.toString());
    }

    @Test
    void translatesNegationDivisionAndModulo() {
        Parsed negation = parse("#x : : #x = neg ( 5 ) ;");
        Emitter negationOutput = new Emitter("int");
        translator(negation, negationOutput, new NameGen(), NumericKind.INT)
                .transTerm(assignmentTerm(negation), "dest", new RenameEnv());
        assertEquals("int t0 ;\r\nt0 = 5 ;\r\ndest = - t0 ;\r\n", negationOutput.toString());

        Parsed division = parse("#x : : #x = div ( 4 2 ) ;");
        Emitter divisionOutput = new Emitter("float");
        translator(division, divisionOutput, new NameGen(), NumericKind.FLOAT)
                .transTerm(assignmentTerm(division), "dest", new RenameEnv());
        assertEquals(
                "float t0 ;\r\n"
                        + "float t1 ;\r\n"
                        + "t0 = 4 ;\r\n"
                        + "t1 = 2 ;\r\n"
                        + "dest = t0 / t1 ;\r\n",
                divisionOutput.toString());

        Parsed modulo = parse("#x : : #x = mod ( 5 2 ) ;");
        Emitter moduloOutput = new Emitter("int");
        translator(modulo, moduloOutput, new NameGen(), NumericKind.INT)
                .transTerm(assignmentTerm(modulo), "dest", new RenameEnv());
        assertEquals(
                "int t0 ;\r\n"
                        + "int t1 ;\r\n"
                        + "t0 = 5 ;\r\n"
                        + "t1 = 2 ;\r\n"
                        + "dest = t0 % t1 ;\r\n",
                moduloOutput.toString());
    }

    @Test
    void callTermThrowsUnsupportedOperationException() {
        Parsed parsed = parse(
                "#x : num #f ( #a ) { : : return ( #a ) } : #x = #f ( 1 ) ;");
        Emitter output = new Emitter("int");
        TermTranslator translator = translator(parsed, output, new NameGen(), NumericKind.INT);
        boolean rejected = false;

        try {
            translator.transTerm(assignmentTerm(parsed), "dest", new RenameEnv());
        } catch (UnsupportedOperationException exception) {
            rejected = true;
            assertEquals("calls not implemented yet", exception.getMessage());
        }

        assertTrue(rejected);
        assertEquals("", output.toString());
    }

    @Test
    void mineModeEmitsNoBlockCommentTerminator() {
        Parsed parsed = parse("#x : : #x = add ( 1 2 ) ;");
        Emitter output = new Emitter("int");
        output.beginMine();
        translator(parsed, output, new NameGen(), NumericKind.INT)
                .transTerm(assignmentTerm(parsed), "dest", new RenameEnv());

        assertFalse(output.toString().contains("*/"));
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

    private TreeNode assignmentTerm(Parsed parsed) {
        TreeNode program = child(parsed.root, 0, parsed.nodes);
        TreeNode algorithm = child(program, 4, parsed.nodes);
        TreeNode instruction = child(algorithm, 0, parsed.nodes);
        TreeNode assignment = child(instruction, 0, parsed.nodes);
        return child(assignment, 2, parsed.nodes);
    }

    private TreeNode child(TreeNode node, int index, Map<Integer, TreeNode> nodes) {
        return nodes.get(node.children.get(index));
    }

    private TermTranslator translator(
            Parsed parsed,
            Emitter output,
            NameGen names,
            NumericKind kind) {
        return new TermTranslator(output, names, parsed.nodes, parsed.symbols, kind);
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
