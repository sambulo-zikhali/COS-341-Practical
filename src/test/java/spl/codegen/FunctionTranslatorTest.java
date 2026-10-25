package spl.codegen;

import org.junit.jupiter.api.Test;
import spl.grammar.Grammar;
import spl.grammar.ParsingTableBuilder;
import spl.lexer.Lexer;
import spl.model.TreeNode;
import spl.parser.NodeIdGenerator;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyzer;
import spl.semantic.Symbol;
import spl.semantic.SymbolTable;
import spl.types.NumericKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionTranslatorTest {
    @Test
    void translatesVoidProcedureWithParameters() {
        Parsed parsed = parse(": void #f ( #a #b ) { : : return } :");
        Emitter output = new Emitter("int");
        RecordingBody body = new RecordingBody(output);
        FunctionTranslator translator = translator(parsed, output, new NameGen(), NumericKind.INT, body);
        TreeNode fDecl = child(child(parsed.root, 0, parsed), 2, parsed);
        TreeNode functionType = child(fDecl, 0, parsed);
        String functionName = symbol(functionType, 1, parsed).internalName;
        List<String> parameters = parameterNames(functionType, parsed);
        TreeNode program = child(functionType, 6, parsed);

        translator.transFDecl(fDecl);

        assertEquals(
                "begin of code-mine for void-procedure " + functionName + "\r\n"
                        + "Parameters for inlining:\r\n"
                        + "int " + parameters.get(0) + " ;\r\n"
                        + "int " + parameters.get(1) + " ;\r\n"
                        + "Body of the procedure:\r\n"
                        + "BODY\r\n"
                        + "end of code-mine for void-procedure " + functionName + "\r\n"
                        + "\r\n",
                output.toString());
        assertEquals(List.of(program), body.programs);
        assertEquals(List.of(false), body.withMineValues);
    }

    @Test
    void translatesNumericFunctionAndReturnTerm() {
        Parsed parsed = parse(": num #f ( #a ) { : : return ( #a ) } :");
        Emitter output = new Emitter("int");
        RecordingBody body = new RecordingBody(output);
        NameGen names = new NameGen();
        TermTranslator terms = new TermTranslator(output, names, parsed.nodes, parsed.symbols, NumericKind.INT);
        FunctionTranslator translator = new FunctionTranslator(
                output, names, parsed.nodes, parsed.symbols, NumericKind.INT, terms, body);
        TreeNode fDecl = child(child(parsed.root, 0, parsed), 2, parsed);
        TreeNode functionType = child(fDecl, 0, parsed);
        String functionName = symbol(functionType, 1, parsed).internalName;
        String parameter = parameterNames(functionType, parsed).get(0);
        String returnName = functionName + "r0";

        translator.transFDecl(fDecl);

        assertEquals(
                "begin of code-mine for Function " + functionName + "\r\n"
                        + "Parameters for inlining:\r\n"
                        + "int " + parameter + " ;\r\n"
                        + "Body of the procedure:\r\n"
                        + "BODY\r\n"
                        + ";\r\n"
                        + "int " + returnName + " ;\r\n"
                        + returnName + " = " + parameter + " ;\r\n"
                        + "end of code-mine for Function " + functionName + "\r\n"
                        + "The result is found in the return value " + returnName + "\r\n"
                        + "\r\n",
                output.toString());
        assertFalse(body.withMineValues.get(0));
    }

    @Test
    void translatesFunctionsInSourceOrder() {
        Parsed parsed = parse(
                ": void #first ( ) { : : return } void #second ( ) { : : return } :");
        Emitter output = new Emitter("int");
        RecordingBody body = new RecordingBody(output);
        TreeNode fDecl = child(child(parsed.root, 0, parsed), 2, parsed);
        TreeNode firstType = child(fDecl, 0, parsed);
        TreeNode secondType = child(child(fDecl, 1, parsed), 0, parsed);
        String firstName = symbol(firstType, 1, parsed).internalName;
        String secondName = symbol(secondType, 1, parsed).internalName;

        translator(parsed, output, new NameGen(), NumericKind.INT, body).transFDecl(fDecl);

        assertTrue(output.toString().indexOf(firstName) < output.toString().indexOf(secondName));
        assertEquals(List.of(child(firstType, 6, parsed), child(secondType, 6, parsed)), body.programs);
        assertEquals(List.of(false, false), body.withMineValues);
    }

    @Test
    void emptyFunctionListEmitsNothing() {
        Parsed parsed = parse(": :");
        Emitter output = new Emitter("int");
        TreeNode fDecl = child(child(parsed.root, 0, parsed), 2, parsed);

        translator(parsed, output, new NameGen(), NumericKind.INT, new RecordingBody(output)).transFDecl(fDecl);

        assertEquals("", output.toString());
    }

    @Test
    void emitsMineSafeOutputAndUsesFloatType() {
        Parsed parsed = parse(": num #f ( #a ) { : : return ( #a ) } :");
        Emitter output = new Emitter("float");
        output.beginMine();
        RecordingBody body = new RecordingBody(output);
        NameGen names = new NameGen();
        TermTranslator terms = new TermTranslator(output, names, parsed.nodes, parsed.symbols, NumericKind.FLOAT);
        FunctionTranslator translator = new FunctionTranslator(
                output, names, parsed.nodes, parsed.symbols, NumericKind.FLOAT, terms, body);
        TreeNode fDecl = child(child(parsed.root, 0, parsed), 2, parsed);
        TreeNode functionType = child(fDecl, 0, parsed);
        String functionName = symbol(functionType, 1, parsed).internalName;
        String parameter = parameterNames(functionType, parsed).get(0);
        String returnName = functionName + "r0";

        translator.transFDecl(fDecl);

        assertEquals(
                "begin of code-mine for Function " + functionName + "\r\n"
                        + "Parameters for inlining:\r\n"
                        + "float " + parameter + " ;\r\n"
                        + "Body of the procedure:\r\n"
                        + "BODY\r\n"
                        + ";\r\n"
                        + "float " + returnName + " ;\r\n"
                        + returnName + " = " + parameter + " ;\r\n"
                        + "end of code-mine for Function " + functionName + "\r\n"
                        + "The result is found in the return value " + returnName + "\r\n"
                        + "\r\n",
                output.toString());
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

    private FunctionTranslator translator(
            Parsed parsed,
            Emitter output,
            NameGen names,
            NumericKind kind,
            BodyTranslator body) {
        TermTranslator terms = new TermTranslator(output, names, parsed.nodes, parsed.symbols, kind);
        return new FunctionTranslator(output, names, parsed.nodes, parsed.symbols, kind, terms, body);
    }

    private Symbol symbol(TreeNode node, int index, Parsed parsed) {
        TreeNode leaf = child(node, index, parsed);
        return parsed.symbols.getSymbol(leaf.id);
    }

    private List<String> parameterNames(TreeNode functionType, Parsed parsed) {
        List<String> parameters = new ArrayList<>();
        TreeNode current = child(functionType, 3, parsed);
        while (!current.children.isEmpty()) {
            TreeNode parameterLeaf = child(current, 0, parsed);
            parameters.add(parsed.symbols.getSymbol(parameterLeaf.id).internalName);
            current = child(current, 1, parsed);
        }
        return parameters;
    }

    private TreeNode child(TreeNode node, int index, Parsed parsed) {
        return parsed.nodes.get(node.children.get(index));
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

    private static final class RecordingBody implements BodyTranslator {
        private final Emitter out;
        private final List<TreeNode> programs = new ArrayList<>();
        private final List<Boolean> withMineValues = new ArrayList<>();

        private RecordingBody(Emitter out) {
            this.out = out;
        }

        @Override
        public void transBody(TreeNode p, RenameEnv env, boolean withMine) {
            programs.add(p);
            withMineValues.add(withMine);
            out.line("BODY");
        }
    }
}
