package spl;

import org.junit.jupiter.api.Test;
import spl.codegen.AlgoCodeGenerator;
import spl.grammar.Grammar;
import spl.grammar.ParsingTableBuilder;
import spl.lexer.Lexer;
import spl.model.TreeNode;
import spl.output.TreeValidator;
import spl.output.XmlTreeWriter;
import spl.parser.NodeIdGenerator;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyzer;
import spl.semantic.SemanticException;
import spl.semantic.Symbol;
import spl.semantic.SymbolTable;
import spl.types.TypeAnalyser;
import spl.types.TypeException;

import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the hand-offs between parsing, scope analysis, typing, and Phase 3a translation. */
class PhaseIntegrationTest {
    private record Parsed(Grammar grammar, TreeNode root, Map<Integer, TreeNode> nodes) {
        TreeNode child(TreeNode node, int index) {
            return nodes.get(node.children.get(index));
        }

        TreeNode topAlgo() {
            return child(child(root, 0), 4);
        }
    }

    private Parsed parse(String source) {
        Grammar grammar = new Grammar();
        Parser parser = new Parser(new ParsingTableBuilder(grammar).build(), new NodeIdGenerator());
        TreeNode root = parser.parse(new Lexer().tokenize(source));
        return new Parsed(grammar, root, parser.getNodesById());
    }

    private TypeAnalyser typeCheck(Parsed parsed, SymbolTable symbols) {
        TypeAnalyser types = new TypeAnalyser(parsed.nodes, symbols, parsed.grammar);
        types.analyse(parsed.root);
        return types;
    }

    @Test
    void announcement21BindsDescendantsToTheirOwnAncestors() {
        Parsed parsed = parse(": void #f ( ) { : "
                + "void #g ( ) { #x : void #j ( ) { : : print ( #x ) ; return } : return } "
                + "void #h ( ) { #x : void #k ( ) { : : print ( #x ) ; return } : return } "
                + ": return } :");
        SymbolTable symbols = new SemanticAnalyzer().analyze(parsed.root, parsed.nodes);
        typeCheck(parsed, symbols);

        Map<String, Symbol> declarations = parsed.nodes.values().stream()
                .filter(n -> n.contents.equals("#x") && "V_DECL".equals(parsed.nodes.get(n.parent).contents))
                .collect(java.util.stream.Collectors.toMap(
                        n -> enclosingFunction(parsed, n),
                        n -> symbols.getSymbol(n.id)));
        assertEquals(Set.of("#g", "#h"), declarations.keySet());
        assertNotEquals(declarations.get("#g").internalName, declarations.get("#h").internalName);

        Map<String, Symbol> bindings = parsed.nodes.values().stream()
                .filter(n -> n.contents.equals("#x") && "TERM".equals(parsed.nodes.get(n.parent).contents))
                .collect(java.util.stream.Collectors.toMap(
                        n -> enclosingFunction(parsed, n),
                        n -> symbols.getBinding(n.id)));
        assertEquals(Set.of("#j", "#k"), bindings.keySet());
        assertEquals(declarations.get("#g").internalName, bindings.get("#j").internalName);
        assertEquals(declarations.get("#h").internalName, bindings.get("#k").internalName);
    }

    @Test
    void announcement21RejectsSidewaysVariableLookup() {
        Parsed parsed = parse(": void #f ( ) { : "
                + "void #g ( ) { #x : void #j ( ) { : : print ( #x ) ; return } : return } "
                + "void #h ( ) { : void #k ( ) { : : print ( #x ) ; return } : return } "
                + ": return } :");
        SemanticException error = assertThrows(SemanticException.class,
                () -> new SemanticAnalyzer().analyze(parsed.root, parsed.nodes));
        assertEquals("undeclared variable #x", error.getMessage());
    }

    @Test
    void typeAnalysisRejectsModAndDivTogether() {
        Parsed parsed = parse("#x : : #x = mod ( 5 2 ) ; print ( div ( 4 2 ) ) ;");
        SymbolTable symbols = new SemanticAnalyzer().analyze(parsed.root, parsed.nodes);
        TypeException error = assertThrows(TypeException.class, () -> typeCheck(parsed, symbols));
        assertEquals(TypeAnalyser.FLOAT_INT_CONFLICT, error.getMessage());
    }

    @Test
    void typeAnalysisRejectsDecimalWhenModIsUsed() {
        Parsed parsed = parse("#x : : #x = mod ( 5.1 2 ) ;");
        SymbolTable symbols = new SemanticAnalyzer().analyze(parsed.root, parsed.nodes);
        TypeException error = assertThrows(TypeException.class, () -> typeCheck(parsed, symbols));
        assertTrue(error.getMessage().contains("Decimal number"));
    }

    @Test
    void typeAnalysisDistinguishesProcedureAndNumericCalls() {
        Parsed procedureAsTerm = parse("#x : void #f ( ) { : : return } : #x = #f ( ) ;");
        SymbolTable procedureSymbols = new SemanticAnalyzer().analyze(procedureAsTerm.root, procedureAsTerm.nodes);
        assertThrows(TypeException.class, () -> typeCheck(procedureAsTerm, procedureSymbols));

        Parsed numericAsInstruction = parse(": num #f ( ) { : : return ( 1 ) } : #f ( ) ;");
        SymbolTable numericSymbols = new SemanticAnalyzer().analyze(numericAsInstruction.root, numericAsInstruction.nodes);
        assertThrows(TypeException.class, () -> typeCheck(numericAsInstruction, numericSymbols));
    }

    @Test
    void phase3aTranslatesTypedAlgoUsingResolvedNamesAndGotos(@org.junit.jupiter.api.io.TempDir Path tempDir)
            throws Exception {
        Parsed parsed = parse("#x : : #x = add ( 1 2 ) ; "
                + "if eq ( #x 3 ) then { print \"yes\" ; } else { print \"no\" ; } ; "
                + "while lesser ( #x 5 ) do { #x = add ( #x 1 ) ; } ;");
        SymbolTable symbols = new SemanticAnalyzer().analyze(parsed.root, parsed.nodes);
        TypeAnalyser types = typeCheck(parsed, symbols);
        Path tree = tempDir.resolve("tree.xml");
        new XmlTreeWriter(parsed.grammar).write(parsed.root, parsed.nodes, tree);
        TreeValidator.validate(tree.toFile());
        AlgoCodeGenerator generator = new AlgoCodeGenerator(symbols, types, parsed.nodes);
        generator.transAlgo(parsed.topAlgo());
        String code = generator.getGeneratedCode();

        String x = symbols.getSymbolsByDeclarationNodeId().values().stream()
                .filter(s -> s.kind == Symbol.Kind.VARIABLE && s.sourceName.equals("#x"))
                .findFirst().orElseThrow().internalName;
        assertTrue(code.contains(x + " = "), code);
        assertTrue(code.contains("std::cout << \"yes\";"), code);
        assertTrue(code.contains("std::cout << \"no\";"), code);
        assertTrue(code.contains("if ("), code);
        assertTrue(code.contains("goto L"), code);
        assertFalse(code.contains("#x"), code);
        assertFalse(code.contains("TODO"), code);
        assertValidJumpTargets(code);
    }

    @Test
    void phase3aHandlesFourLoopFormsAndBooleanComposition() {
        Parsed parsed = parse("#x : : "
                + "while not ( eq ( #x 0 ) ) do { #x = sub ( #x 1 ) ; } ; "
                + "until or ( eq ( #x 1 ) eq ( #x 2 ) ) do { nop ; } ; "
                + "do { #x = add ( #x 1 ) ; } while lesser ( #x 5 ) ; "
                + "do { #x = mul ( #x 2 ) ; } until and ( larger ( #x 10 ) lesser ( #x 20 ) ) ;");
        SymbolTable symbols = new SemanticAnalyzer().analyze(parsed.root, parsed.nodes);
        TypeAnalyser types = typeCheck(parsed, symbols);
        AlgoCodeGenerator generator = new AlgoCodeGenerator(symbols, types, parsed.nodes);
        generator.transAlgo(parsed.topAlgo());
        String code = generator.getGeneratedCode();
        assertTrue(code.contains("/* condition */"), code);
        assertTrue(code.contains("/* do */"), code);
        assertFalse(code.contains("&&"), code);
        assertFalse(code.contains("||"), code);
        assertValidJumpTargets(code);
    }

    @Test
    void phase3aUsesFloatTemporariesForRealNumbers() {
        Parsed parsed = parse("#x : : #x = 1.5 ; print ( div ( #x 2 ) ) ;");
        SymbolTable symbols = new SemanticAnalyzer().analyze(parsed.root, parsed.nodes);
        TypeAnalyser types = typeCheck(parsed, symbols);
        AlgoCodeGenerator generator = new AlgoCodeGenerator(symbols, types, parsed.nodes);
        generator.transAlgo(parsed.topAlgo());
        String code = generator.getGeneratedCode();
        assertTrue(code.contains("float temp"), code);
        assertTrue(code.contains(" / "), code);
    }

    private void assertValidJumpTargets(String code) {
        Pattern labels = Pattern.compile("(?m)^(L[0-9]+):");
        Pattern jumps = Pattern.compile("\\bgoto (L[0-9]+);");
        Set<String> declared = new HashSet<>();
        Matcher labelMatcher = labels.matcher(code);
        while (labelMatcher.find()) {
            assertTrue(declared.add(labelMatcher.group(1)), "Duplicate label in:\n" + code);
        }
        Matcher jumpMatcher = jumps.matcher(code);
        int jumpCount = 0;
        while (jumpMatcher.find()) {
            assertTrue(declared.contains(jumpMatcher.group(1)), "Missing jump target in:\n" + code);
            jumpCount++;
        }
        assertTrue(jumpCount > 0, code);
    }

    private String enclosingFunction(Parsed parsed, TreeNode node) {
        for (TreeNode current = node; current.parent != null; current = parsed.nodes.get(current.parent)) {
            TreeNode parent = parsed.nodes.get(current.parent);
            if ("F_TYPE".equals(parent.contents)) return parsed.child(parent, 1).contents;
        }
        throw new AssertionError("No enclosing function for node " + node.id);
    }
}
