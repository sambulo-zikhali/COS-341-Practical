package spl.semantic;

import org.junit.jupiter.api.Test;
import spl.grammar.Grammar;
import spl.grammar.ParsingTable;
import spl.grammar.ParsingTableBuilder;
import spl.lexer.Lexer;
import spl.model.TreeNode;
import spl.parser.NodeIdGenerator;
import spl.parser.Parser;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SemanticAnalyzerDeclTest {
    @Test
    void validNestedFunctionsCreateScopesAtLevelsZeroOneAndTwo() {
        Analysis analysis = analyze(
                ": void #f ( ) { : void #g ( ) { : : return } : return } :");

        assertEquals(3, analysis.symbolTable.getScopesById().size());
        assertTrue(analysis.symbolTable.getScopesById().values().stream()
                .anyMatch(scope -> scope.level == 0 && scope.parent == null));
        assertEquals(1, analysis.symbolTable.getScopesById().values().stream()
                .filter(scope -> scope.level == 1).count());
        assertEquals(1, analysis.symbolTable.getScopesById().values().stream()
                .filter(scope -> scope.level == 2).count());
    }

    @Test
    void duplicateTopLevelVariableThrowsWithOffendingNameNode() {
        Analysis analysis = parse("#x #x : :");

        SemanticException exception = assertThrows(
                SemanticException.class,
                () -> new SemanticAnalyzer().analyze(analysis.root, analysis.nodes));
        assertEquals("#x", analysis.nodes.get(exception.nodeId).contents);
        assertTrue(exception.getMessage().contains("duplicate variable declaration #x"));
    }

    @Test
    void localVariableMaskingParameterThrowsWithOffendingNameNode() {
        Analysis analysis = parse(": void #f ( #x ) { #x : : return } :");

        SemanticException exception = assertThrows(
                SemanticException.class,
                () -> new SemanticAnalyzer().analyze(analysis.root, analysis.nodes));
        assertEquals("#x", analysis.nodes.get(exception.nodeId).contents);
        assertTrue(exception.getMessage().contains("local variable masks parameter #x"));
    }

    @Test
    void sameVariableNameAtDifferentLevelsIsAllowed() {
        Analysis analysis = analyze("#x : void #f ( ) { #x : : return } :");

        List<Symbol> variables = analysis.symbolTable.getSymbolsByDeclarationNodeId().values().stream()
                .filter(symbol -> symbol.kind == Symbol.Kind.VARIABLE && symbol.sourceName.equals("#x"))
                .toList();
        assertEquals(2, variables.size());
        assertNotEquals(variables.get(0).internalName, variables.get(1).internalName);
    }

    @Test
    void duplicateFunctionNamesInOneFDeclThrowWithOffendingNameNode() {
        Analysis analysis = parse(
                ": void #f ( ) { : : return } void #f ( ) { : : return } :");

        SemanticException exception = assertThrows(
                SemanticException.class,
                () -> new SemanticAnalyzer().analyze(analysis.root, analysis.nodes));
        assertEquals("#f", analysis.nodes.get(exception.nodeId).contents);
        assertTrue(exception.getMessage().contains("duplicate function #f"));
    }

    @Test
    void siblingFunctionsGetDistinctInternalNamesForSameVariableSpelling() {
        Analysis analysis = analyze(
                ": void #f ( ) { #x : : return } void #g ( ) { #x : : return } :");

        List<Symbol> variables = analysis.symbolTable.getSymbolsByDeclarationNodeId().values().stream()
                .filter(symbol -> symbol.kind == Symbol.Kind.VARIABLE && symbol.sourceName.equals("#x"))
                .toList();
        assertEquals(2, variables.size());
        assertNotEquals(variables.get(0).internalName, variables.get(1).internalName);
        assertNotEquals(variables.get(0).scopeId, variables.get(1).scopeId);
    }

    private Analysis analyze(String source) {
        Analysis analysis = parse(source);
        analysis.symbolTable = new SemanticAnalyzer().analyze(analysis.root, analysis.nodes);
        return analysis;
    }

    private Analysis parse(String source) {
        Grammar grammar = new Grammar();
        ParsingTable table = new ParsingTableBuilder(grammar).build();
        Parser parser = new Parser(table, new NodeIdGenerator());
        TreeNode root = parser.parse(new Lexer().tokenize(source));
        return new Analysis(root, parser.getNodesById());
    }

    private static final class Analysis {
        private final TreeNode root;
        private final Map<Integer, TreeNode> nodes;
        private SymbolTable symbolTable;

        private Analysis(TreeNode root, Map<Integer, TreeNode> nodes) {
            this.root = root;
            this.nodes = nodes;
        }
    }
}
