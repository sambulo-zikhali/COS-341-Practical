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
import spl.types.TypeAnalyser;
import spl.types.TypeException;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallInliningTest {
    @Test
    void inlinesVoidProcedureAndAssignsCapturedArgumentToParameter() {
        Parsed parsed = parse(": void #f ( #a ) { : : print ( #a ) ; return } : #f ( 1 ) ;");
        String output = generate(parsed);
        Symbol parameter = parameter(parsed, "#a");

        Matcher capture = Pattern.compile("p\\d+").matcher(output);
        assertTrue(capture.find(), output);
        assertTrue(output.contains(parameter.internalName + "k"), output);
        assertTrue(Pattern.compile(parameter.internalName + "k\\d+ = p\\d+ ;").matcher(output).find(), output);
        assertTrue(Pattern.compile("t\\d+ = " + parameter.internalName + "k\\d+ ;")
                .matcher(output).find(), output);
        assertTrue(Pattern.compile("std::cout << t\\d+ ;")
                .matcher(output).find(), output);
    }

    @Test
    void inlinesNumericFunctionAndAssignsItsReturnValue() {
        String output = generate(
                "#y : num #f ( #a ) { : : return ( add ( #a 1 ) ) } : #y = #f ( 3 ) ;");

        Matcher returnVariable = Pattern.compile("r\\d+").matcher(output);
        assertTrue(returnVariable.find(), output);
        assertTrue(Pattern.compile("= r\\d+ ;").matcher(output).find(), output);
    }

    @Test
    void repeatedCallsHaveUniqueLabelsAndDeclarations() {
        String output = generate(
                ": void #f ( #a ) { #x : : "
                        + "while lesser ( #x 2 ) do { #x = add ( #x 1 ) ; } ; "
                        + "return } : #f ( 1 ) ; #f ( 2 ) ;");

        assertUniqueMatches(output, Pattern.compile("(?m)(L\\d+) :"));
        assertUniqueMatches(output, Pattern.compile("(?m)int ([A-Za-z][A-Za-z0-9_]*) ;"));
    }

    @Test
    void outerVariableRemainsUnrenamedWhileFunctionVariablesAreRenamed() {
        Parsed parsed = parse(
                "#g : void #f ( #a ) { #x : : #g = #a ; return } : #f ( 1 ) ;");
        String output = generate(parsed);
        Symbol outer = variable(parsed, "#g", false);
        Symbol parameter = parameter(parsed, "#a");
        Symbol local = variable(parsed, "#x", false);

        assertTrue(Pattern.compile("t\\d+ = " + parameter.internalName + "k\\d+ ;")
                .matcher(output).find(), output);
        assertTrue(Pattern.compile("(?m)" + outer.internalName + " = t\\d+ ;").matcher(output).find(), output);
        assertTrue(output.contains("int " + parameter.internalName + "k"), output);
        assertTrue(output.contains("int " + local.internalName + "k"), output);
        assertFalse(Pattern.compile(Pattern.quote(outer.internalName) + "k\\d+").matcher(output).find(), output);
    }

    @Test
    void nestedSubfunctionIsInlinedWithoutDuplicateLabelsOrDeclarations() {
        String output = generate(
                ": void #f ( ) { : "
                        + "void #g ( ) { : : while lesser ( 1 2 ) do { nop ; } ; return } "
                        + ": #g ( ) ; return } : #f ( ) ;");

        assertTrue(output.contains("begin of code-mine for void-procedure"), output);
        assertUniqueMatches(output, Pattern.compile("(?m)(L\\d+) :"));
        assertUniqueMatches(output, Pattern.compile("(?m)int ([A-Za-z][A-Za-z0-9_]*) ;"));
    }

    @Test
    void translatesCallUsedAsArgument() {
        String output = generate(
                "#y : "
                        + "num #g ( #b ) { : : return ( #b ) } "
                        + "num #f ( #a ) { : : return ( #a ) } "
                        + ": #y = #f ( #g ( 1 ) ) ;");

        assertTrue(Pattern.compile("p\\d+ = 1 ;").matcher(output).find(), output);
        assertTrue(Pattern.compile("v\\d+k\\d+ = p\\d+ ;").matcher(output).find(), output);
        assertTrue(Pattern.compile("= r\\d+ ;").matcher(output).find(), output);
    }

    @Test
    void typeAnalysisRejectsWrongArgumentCountBeforeInlining() {
        Parsed parsed = parse("#y : num #f ( #a ) { : : return ( #a ) } : #y = #f ( ) ;");
        boolean rejected = false;

        try {
            parsed.types.analyse(parsed.root);
        } catch (TypeException exception) {
            rejected = true;
        }
        assertTrue(rejected);
    }

    @Test
    void mineOutputContainsNoPrematureCommentTerminator() {
        String output = generate(
                ": void #f ( ) { : void #g ( ) { : : comment \"safe\" ; return } "
                        + ": #g ( ) ; return } : #f ( ) ;");
        String start = "/* begin of the non-executable code-mine";
        String end = "end of the non-executable code-mine */";
        int mineStart = output.indexOf(start);
        int mineEnd = output.indexOf(end);
        int contentsStart = output.indexOf("\r\n", mineStart) + 2;

        assertTrue(mineStart >= 0, output);
        assertTrue(mineEnd > mineStart, output);
        assertFalse(output.substring(contentsStart, mineEnd).contains("*/"), output);
    }

    private String generate(String source) {
        return generate(parse(source));
    }

    private String generate(Parsed parsed) {
        parsed.types.analyse(parsed.root);
        return CodeGenerator.generate(
                parsed.root,
                parsed.nodes,
                parsed.symbols,
                parsed.types.getNumericKind());
    }

    private Parsed parse(String source) {
        Grammar grammar = new Grammar();
        Parser parser = new Parser(
                new ParsingTableBuilder(grammar).build(),
                new NodeIdGenerator());
        TreeNode root = parser.parse(new Lexer().tokenize(source));
        Map<Integer, TreeNode> nodes = parser.getNodesById();
        SymbolTable symbols = new SemanticAnalyzer().analyze(root, nodes);
        TypeAnalyser types = new TypeAnalyser(nodes, symbols, grammar);
        return new Parsed(root, nodes, symbols, types);
    }

    private Symbol parameter(Parsed parsed, String sourceName) {
        for (Symbol symbol : parsed.symbols.getSymbolsByDeclarationNodeId().values()) {
            if (symbol.isParameter && symbol.sourceName.equals(sourceName)) {
                return symbol;
            }
        }
        throw new AssertionError("Parameter not found: " + sourceName);
    }

    private Symbol variable(Parsed parsed, String sourceName, boolean parameter) {
        for (Symbol symbol : parsed.symbols.getSymbolsByDeclarationNodeId().values()) {
            if (symbol.kind == Symbol.Kind.VARIABLE
                    && symbol.isParameter == parameter
                    && symbol.sourceName.equals(sourceName)) {
                return symbol;
            }
        }
        throw new AssertionError("Variable not found: " + sourceName);
    }

    private void assertUniqueMatches(String output, Pattern pattern) {
        Matcher matcher = pattern.matcher(output);
        java.util.Set<String> names = new java.util.HashSet<>();
        while (matcher.find()) {
            assertTrue(names.add(matcher.group(1)), "Duplicate generated name: " + matcher.group(1) + "\n" + output);
        }
    }

    private static final class Parsed {
        private final TreeNode root;
        private final Map<Integer, TreeNode> nodes;
        private final SymbolTable symbols;
        private final TypeAnalyser types;

        private Parsed(TreeNode root, Map<Integer, TreeNode> nodes, SymbolTable symbols, TypeAnalyser types) {
            this.root = root;
            this.nodes = nodes;
            this.symbols = symbols;
            this.types = types;
        }
    }
}
