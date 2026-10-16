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

class SemanticAnalyzerUseTest {
    @Test
    void variableUseBindsToItsDeclaration() {
        Analysis analysis = analyze("#x : : print ( #x ) ;");
        TreeNode declaration = findNameByParent(analysis, "#x", "V_DECL");
        TreeNode use = findNameByParent(analysis, "#x", "TERM");

        assertEquals(
                analysis.symbolTable.getSymbol(declaration.id).internalName,
                analysis.symbolTable.getBinding(use.id).internalName);
    }

    @Test
    void undeclaredVariableThrowsWithUseNodeId() {
        Analysis analysis = parse(": : print ( #missing ) ;");
        TreeNode use = findNameByParent(analysis, "#missing", "TERM");

        SemanticException exception = assertThrows(
                SemanticException.class,
                () -> new SemanticAnalyzer().analyze(analysis.root, analysis.nodes));

        assertEquals(use.id, exception.nodeId);
        assertEquals("undeclared variable #missing", exception.getMessage());
    }

    @Test
    void nearestVariableDeclarationWins() {
        Analysis analysis = analyze(
                "#x : void #f ( ) { #x : : print ( #x ) ; return } :");
        TreeNode use = findNameByParent(analysis, "#x", "TERM");
        List<Symbol> declarations = analysis.symbolTable.getSymbolsByDeclarationNodeId().values().stream()
                .filter(symbol -> symbol.kind == Symbol.Kind.VARIABLE && symbol.sourceName.equals("#x"))
                .toList();
        Symbol topLevel = declarations.stream()
                .filter(symbol -> analysis.symbolTable.getScope(symbol.scopeId).level == 0)
                .findFirst()
                .orElseThrow();
        Symbol local = declarations.stream()
                .filter(symbol -> analysis.symbolTable.getScope(symbol.scopeId).level == 1)
                .findFirst()
                .orElseThrow();

        assertNotEquals(topLevel.internalName, local.internalName);
        assertEquals(local.internalName, analysis.symbolTable.getBinding(use.id).internalName);
    }

    @Test
    void outerVariableIsVisibleInNestedFunctionAlgo() {
        Analysis analysis = analyze(
                "#x : void #f ( ) { : : print ( #x ) ; return } :");
        TreeNode declaration = findNameByParent(analysis, "#x", "V_DECL");
        TreeNode use = findNameByParent(analysis, "#x", "TERM");

        assertEquals(
                analysis.symbolTable.getSymbol(declaration.id).internalName,
                analysis.symbolTable.getBinding(use.id).internalName);
    }

    @Test
    void declarationInFunctionIsVisibleToItsDescendantButNotSibling() {
        Analysis analysis = parse(
                ": void #f ( ) { : "
                        + "void #g ( ) { #x : void #j ( ) { : : print ( #x ) ; return } : return } "
                        + "void #h ( ) { : : print ( #x ) ; return } "
                        + ": return } :");
        TreeNode hUse = findNameInFunctionAlgo(analysis, "#h", "#x");

        SemanticException exception = assertThrows(
                SemanticException.class,
                () -> new SemanticAnalyzer().analyze(analysis.root, analysis.nodes));

        assertEquals(hUse.id, exception.nodeId);
        assertEquals("undeclared variable #x", exception.getMessage());
    }

    @Test
    void callToFunctionDeclaredInSameProgramBindsToFunctionSymbol() {
        Analysis analysis = analyze(
                ": void #f ( ) { : : return } : #f ( ) ;");
        TreeNode declaration = findNameByParent(analysis, "#f", "F_TYPE");
        TreeNode use = findNameByParent(analysis, "#f", "CALL");

        assertEquals(
                analysis.symbolTable.getSymbol(declaration.id).internalName,
                analysis.symbolTable.getBinding(use.id).internalName);
    }

    @Test
    void directRecursionIsReportedAsUndeclaredFunction() {
        Analysis analysis = parse(
                ": void #f ( ) { : : #f ( ) ; return } :");

        SemanticException exception = assertThrows(
                SemanticException.class,
                () -> new SemanticAnalyzer().analyze(analysis.root, analysis.nodes));

        assertEquals("undeclared function #f", exception.getMessage());
        assertEquals(findNameByParent(analysis, "#f", "CALL").id, exception.nodeId);
    }

    @Test
    void siblingFunctionCallIsReportedAsUndeclaredFunction() {
        Analysis analysis = parse(
                ": void #f ( ) { : : #g ( ) ; return } "
                        + "void #g ( ) { : : return } :");

        SemanticException exception = assertThrows(
                SemanticException.class,
                () -> new SemanticAnalyzer().analyze(analysis.root, analysis.nodes));

        assertEquals("undeclared function #g", exception.getMessage());
        assertEquals(findNameByParent(analysis, "#g", "CALL").id, exception.nodeId);
    }

    @Test
    void functionNameUsedAsVariableIsReportedUndeclared() {
        Analysis analysis = parse(
                ": void #f ( ) { : : return } : print ( #f ) ;");

        SemanticException exception = assertThrows(
                SemanticException.class,
                () -> new SemanticAnalyzer().analyze(analysis.root, analysis.nodes));

        assertEquals("undeclared variable #f", exception.getMessage());
        assertEquals(findNameByParent(analysis, "#f", "TERM").id, exception.nodeId);
    }

    @Test
    void numFunctionParameterResolvesInReturnTerm() {
        Analysis analysis = analyze(
                ": num #f ( #parameter ) { : : return ( #parameter ) } :");
        TreeNode parameter = findNameByParent(analysis, "#parameter", "V_DECL");
        TreeNode returnUse = findNameByParent(analysis, "#parameter", "TERM");

        assertTrue(analysis.symbolTable.getSymbol(parameter.id).isParameter);
        assertEquals(
                analysis.symbolTable.getSymbol(parameter.id).internalName,
                analysis.symbolTable.getBinding(returnUse.id).internalName);
    }

    @Test
    void numFunctionParameterAndLocalFlagsAreRecordedAndEmptyProgramHasOneScope() {
        Analysis analysis = analyze(
                ": num #f ( #parameter ) { #local : : return ( 1 ) } :");
        TreeNode parameter = findNameByParent(analysis, "#parameter", "V_DECL");
        TreeNode local = findNameByParent(analysis, "#local", "V_DECL");

        assertTrue(analysis.symbolTable.getSymbol(parameter.id).isParameter);
        assertFalse(analysis.symbolTable.getSymbol(local.id).isParameter);

        Analysis emptyProgram = analyze(": :");
        assertEquals(1, emptyProgram.symbolTable.getScopesById().size());
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

    private TreeNode findNameByParent(Analysis analysis, String name, String parentContents) {
        return analysis.nodes.values().stream()
                .filter(node -> node.children.isEmpty() && name.equals(node.contents))
                .filter(node -> node.parent != null)
                .filter(node -> {
                    TreeNode parent = analysis.nodes.get(node.parent);
                    return parent != null && parentContents.equals(parent.contents);
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Could not find " + name + " with parent " + parentContents));
    }

    private TreeNode findNameInFunctionAlgo(Analysis analysis, String functionName, String name) {
        TreeNode functionType = analysis.nodes.values().stream()
                .filter(node -> "F_TYPE".equals(node.contents))
                .filter(node -> childAt(analysis, node, 1).contents.equals(functionName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Could not find function " + functionName));
        TreeNode body = childAt(analysis, functionType, 6);
        TreeNode algo = childAt(analysis, body, 4);
        return findNameInSubtree(analysis, algo, name);
    }

    private TreeNode findNameInSubtree(Analysis analysis, TreeNode root, String name) {
        if (root.children.isEmpty()) {
            return name.equals(root.contents) ? root : null;
        }
        for (int childId : root.children) {
            TreeNode match = findNameInSubtree(analysis, analysis.nodes.get(childId), name);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private TreeNode childAt(Analysis analysis, TreeNode node, int index) {
        List<TreeNode> children = node.children.stream()
                .map(analysis.nodes::get)
                .toList();
        return children.get(index);
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
